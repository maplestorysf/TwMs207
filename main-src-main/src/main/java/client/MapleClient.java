package client;

import constants.FeaturesConfig;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseException;
import database.ManagerDatabasePool;
import handling.MapleServerHandler;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.guild.MapleGuildCharacter;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import server.CharacterCardFactory;
import server.Timer.PingTimer;
import server.commands.PlayerGMRank;
import server.farm.MapleFarm;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import server.shark.SharkLogger;
import server.stores.IMaplePlayerShop;
import tools.*;
import tools.packet.CWvsContext;
import tools.packet.LoginPacket;

import javax.script.ScriptEngine;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapleClient implements Serializable {

    public static enum BanReason {

        帳號不正常(0),
        騷擾他人(1),
        廣告(2),
        現金交易(3),
        竄改資料(4),
        詐騙(5),
        盜用帳號(6),
        從作弊玩家獲益(7),
        正在調查帳號(9),
        點數詐騙(10),
        違反規章(11),
        外掛(13),
        系統鎖定(14),
        暴露其他人資料(15),
        賭博(16),
        使用BUG(18),
        ;

        private final int reason;

        private BanReason(int reason) {
            this.reason = reason;
        }

        public int getReason() {
            return reason;
        }

        public static BanReason getByReason(int reason) {
            for (BanReason r : values()) {
                if (r.getReason() == reason) {
                    return r;
                }
            }
            return null;
        }
    }

    private static final long serialVersionUID = 9179541993413738569L;
    public static final byte LOGIN_NOTLOGGEDIN = 0, LOGIN_SERVER_TRANSITION = 1, LOGIN_LOGGEDIN = 2, CHANGE_CHANNEL = 3;
    public static final AttributeKey<MapleClient> CLIENT_KEY = AttributeKey.valueOf("Client");
    private final transient MapleAESOFB send, receive;
    private final transient Channel session;
    private long sessionId;
    private MapleCharacter player;
    private int channel = 1, accId = -1, world, birthday;
    private int charslots = GameConstants.DEFAULT_CHARSLOT;
    private boolean loggedIn = false, serverTransition = false;
    private transient Calendar tempban = null;
    private String accountName;
    private transient long lastPong = 0, lastPing = 0;
    private boolean monitored = false, receiving = true;
    private int gmLevel;
    private byte greason = 1, gender = -1;
    public transient short loginAttempt = 0;
    public transient short couponAttempt = 0;
    private final transient List<Integer> allowedChar = new LinkedList<>();
    private transient String mac = "00-00-00-00-00-00";
    private final transient Map<String, ScriptEngine> engines = new HashMap<>();
    private transient ScheduledFuture<?> idleTask = null;
    private transient String secondPassword, salt2, tempIP = ""; // To be used only on login
    private final transient Lock mutex = new ReentrantLock(true);
    private final transient Lock npc_mutex = new ReentrantLock();
    private long lastNpcClick = 0;
    private final static Lock login_mutex = new ReentrantLock(true);
    private final Map<Integer, Pair<Short, Short>> charInfo = new LinkedHashMap<>();
    private ArrayList<Integer> charPos = new ArrayList<>();
    private int client_increnement = 1;
    private MapleFarm farm;
    private String redirectorUsername;
    private Map<String, String> CustomValues = null;
    private final Map<String, String> TempValues = new HashMap<>();
    public SharkLogger sl = new SharkLogger(ServerConstants.SHARK_VERSION, this); // no boilerplate because i'm lazy
    private boolean isSaving = false;
    public OpcodeEncryption opecodeCrypto = null;

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, Channel session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }

    public final MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public final MapleAESOFB getSendCrypto() {
        return send;
    }

    public synchronized Channel getSession() {
        return session;
    }

    public OpcodeEncryption getOpecodeCrypto() {
        return opecodeCrypto;
    }

    public void setOpecodeCrypto(OpcodeEncryption opecodeCrypto) {
        this.opecodeCrypto = opecodeCrypto;
    }

    public void announce(byte[] data) {
        this.getSession().writeAndFlush(data);
    }

    public long getSessionId() {
        return this.sessionId;

    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public final Lock getLock() {
        return mutex;
    }

    public final Lock getNPCLock() {
        return npc_mutex;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void createdChar(final int id) {
        allowedChar.add(id);
    }

    public final boolean login_Auth(final int id) {
        return allowedChar.contains(id);
    }

    public final List<MapleCharacter> loadCharacters(final int serverId) {
        final List<MapleCharacter> chars = new LinkedList<>();

        final Map<Integer, CardData> cardss = CharacterCardFactory.getInstance().loadCharacterCards(accId, serverId);
        for (final CharNameAndId cni : loadCharactersInternal(serverId)) {
            final MapleCharacter chr = MapleCharacter.loadCharFromDB(cni.id, this, false, cardss);
            chars.add(chr);
            charInfo.put(chr.getId(), new Pair<>(chr.getLevel(), chr.getJob())); // to be used to update charCards
            if (!login_Auth(chr.getId())) {
                allowedChar.add(chr.getId());
            }
        }
        return chars;
    }

    public final ArrayList<Integer> getCharacterPos() {
        if (charPos.isEmpty() && !charInfo.isEmpty()) {
            loadCharacterPos();
        }
        return charPos;
    }

    public final boolean addCharacterPos(int id) {
        if (charPos.contains(id)) {
            return false;
        } else {
            charPos.add(id);
            updateCharacterPos(charPos);
            return true;
        }
    }

    public final void loadCharacterPos() {
        charPos.clear();
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id, name, gm FROM characters WHERE accountid = ? AND world = ? ORDER BY position ASC")) {
                ps.setInt(1, accId);
                ps.setInt(2, world);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        charPos.add(rs.getInt("id"));
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            Logger.getLogger(MapleClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final void updateCharacterPos(final ArrayList<Integer> newPos) {
        System.out.println("updateCharacterPos: " + newPos.toString());
        if (charPos.isEmpty()) { // no characters
            return;
        }
        charPos = newPos;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            newPos.forEach((id) -> {
                try (PreparedStatement ps = con
                        .prepareStatement("UPDATE `characters` SET `position` = ? WHERE id = ?")) {
                    ps.setInt(1, newPos.indexOf(id));
                    ps.setInt(2, id);
                    ps.executeUpdate();
                } catch (SQLException sqlE) {
                    System.err.println("Failed to update character position. Reason: " + sqlE.toString());
                }
            });

            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            Logger.getLogger(MapleClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final void updateCharacterCards(final Map<Integer, Integer> cids) {
        System.out.println("updateCharacterCards: " + cids.toString());
        if (charInfo.isEmpty()) { // no characters
            return;
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM `character_cards` WHERE `accid` = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            }
            try (PreparedStatement psu = con.prepareStatement(
                    "INSERT INTO `character_cards` (accid, worldid, characterid, position) VALUES (?, ?, ?, ?)")) {
                for (final Entry<Integer, Integer> ii : cids.entrySet()) {
                    final Pair<Short, Short> info = charInfo.get(ii.getValue()); // charinfo we can use here as
                    // characters are already loaded
                    if (info == null || ii.getValue() == 0
                            || !CharacterCardFactory.getInstance().canHaveCard(info.getLeft(), info.getRight())) {
                        continue;
                    }
                    psu.setInt(1, accId);
                    psu.setInt(2, world);
                    psu.setInt(3, ii.getValue());
                    psu.setInt(4, ii.getKey()); // position shouldn't matter much, will reset upon login
                    psu.executeUpdate();
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException sqlE) {
            System.err.println("Failed to update character cards. Reason: " + sqlE.toString());
        }
    }

    public boolean canMakeCharacter(int serverId) {
        return loadCharactersSize(serverId) < getCharacterSlots();
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new LinkedList<>();
        loadCharactersInternal(serverId).forEach((cni) -> {
            chars.add(cni.name);
        });
        return chars;
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        List<CharNameAndId> chars = new LinkedList<>();
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id, name, gm FROM characters WHERE accountid = ? AND world = ? ORDER BY position ASC")) {
                ps.setInt(1, accId);
                ps.setInt(2, serverId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
                        LoginServer.getLoginAuth(rs.getInt("id"));
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("error loading characters internal");
        }
        return chars;
    }

    private int loadCharactersSize(int serverId) {
        int chars = 0;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT count(*) FROM characters WHERE accountid = ? AND world = ?")) {
                ps.setInt(1, accId);
                ps.setInt(2, serverId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        chars = rs.getInt(1);
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("error loading characters internal");
        }
        return chars;
    }

    public boolean isLoggedIn() {
        return loggedIn && accId >= 0;
    }

    private Calendar getTempBanCalendar(ResultSet rs) throws SQLException {
        Calendar lTempban = Calendar.getInstance();
        if (rs.getLong("tempban") == 0) { // basically if timestamp in db is 0000-00-00
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }

        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return tempban;
    }

    public byte getBanReason() {
        return greason;
    }

    public String getTrueBanReason(String name) {
        String ret = null;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT banreason FROM accounts WHERE name = ?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ret = rs.getString(1);
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
            return ret;
        } catch (SQLException ex) {
            System.err.println("Error getting ban reason: " + ex);
        }
        return ret;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
                ps.setString(1, getSessionIPAddress());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        ret = true;
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            System.err.println("Error checking ip bans" + ex);
        }
        return ret;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String macData) {
        if (macData.equalsIgnoreCase("00-00-00-00-00-00") || macData.length() != 17) {
            return;
        }
        mac = macData;
    }

    public void updateMacs() {
        updateMacs(mac);
    }

    public void updateMacs(String macData) {
        if (macData.equalsIgnoreCase("00-00-00-00-00-00") || macData.length() != 17) {
            return;
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?")) {
                ps.setString(1, macData);
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("Error saving MACs" + e);
        }
    }

    public void banMacs() {
        banMacs(mac);
    }

    public boolean hasBannedMac() {
        if (mac.equalsIgnoreCase("00-00-00-00-00-00") || mac.length() != 17) {
            return false;
        }
        boolean ret = false;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM macbans WHERE mac = ?")) {
                ps.setString(1, mac);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        ret = true;
                    }
                }
            }
            ManagerDatabasePool.closeConnection(null);
        } catch (SQLException ex) {
            System.err.println("Error checking mac bans" + ex);
        }
        return ret;
    }

    public static void banMacs(String macData) {
        if (macData.equalsIgnoreCase("00-00-00-00-00-00") || macData.length() != 17) {
            return;
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)")) {
                ps.setString(1, macData);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(null);
        } catch (SQLException ex) {
            System.err.println("封鎖MAC時異常:" + ex);
        }
    }

    public static boolean banIP(String ip) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            if (ip.matches("/[0-9]{1,3}\\..*")) {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)")) {
                    ps.setString(1, ip);
                    ps.execute();
                }
            } else {
                return false;
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            System.err.println("封鎖IP時異常:" + ex);
            return false;
        }
        return true;
    }

    public static boolean ban(String id, String reason, Calendar tempBan, BanReason greason, boolean autoban,
            boolean isAccountId, boolean ip, boolean mac, boolean hellban) {
        try {
            boolean ret;
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps;
            if (isAccountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            ret = false;
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int z = rs.getInt(1);
                    int i = 1;
                    Timestamp TS;
                    try (PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ?, "
                            + (tempBan == null ? "" : "tempban = ?, ") + "greason = ? WHERE id = ?")) {
                        psb.setInt(i++, autoban ? 2 : 1);
                        psb.setString(i++, reason);
                        if (tempBan != null) {
                            TS = new Timestamp(tempBan.getTimeInMillis());
                            psb.setTimestamp(i++, TS);
                        }
                        psb.setInt(i++, greason.getReason());
                        psb.setInt(i++, z);
                        psb.execute();
                    }

                    if (ip || mac || hellban) { // admin ban
                        try (PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
                            psa.setInt(1, z);
                            try (ResultSet rsa = psa.executeQuery()) {
                                if (rsa.next()) {
                                    String sessionIP = rsa.getString("sessionIP");

                                    // 封鎖IP
                                    if (ip) {
                                        if (sessionIP != null && sessionIP.matches("/[0-9]{1,3}\\..*")) {
                                            banIP(sessionIP);
                                        }
                                    }

                                    // 封鎖MAC
                                    if (mac) {
                                        String macData = rsa.getString("macs");
                                        if (macData != null) {
                                            banMacs(macData);
                                        }
                                    }

                                    // 封鎖相同郵箱或者IP的帳戶
                                    if (hellban) {
                                        try (PreparedStatement pss = con
                                                .prepareStatement("UPDATE accounts SET banned = ?, banreason = ?, "
                                                        + (tempBan == null ? "" : "tempban = ?, ")
                                                        + "greason = ? WHERE email = ?"
                                                        + (sessionIP == null ? "" : " OR SessionIP = ?"))) {
                                            i = 1;
                                            pss.setInt(i++, autoban ? 2 : 1);
                                            pss.setString(i++, reason);
                                            if (tempBan != null) {
                                                TS = new Timestamp(tempBan.getTimeInMillis());
                                                pss.setTimestamp(i++, TS);
                                            }
                                            pss.setInt(i++, greason.getReason());
                                            pss.setString(i++, rsa.getString("email"));
                                            if (sessionIP != null) {
                                                pss.setString(i++, sessionIP);
                                            }
                                            pss.execute();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ret = true;
                }
            }
            ps.close();
            ManagerDatabasePool.closeConnection(con);
            return ret;
        } catch (SQLException ex) {
            System.err.println("封鎖帳號異常:" + ex);
        }
        return false;
    }

    private byte unban() {
        return unban(accountName, true, false);
    }

    public static byte unbanIPMacs(String charname) {
        try {
            byte ret;
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT accountid from characters where name = ?");
            ps.setString(1, charname);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final int accid = rs.getInt(1);
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final String sessionIP = rs.getString("sessionIP");
            final String macs = rs.getString("macs");
            rs.close();
            ps.close();
            ret = 0;
            if (sessionIP != null) {
                try (PreparedStatement psa = con.prepareStatement("DELETE FROM ipbans WHERE ip like ?")) {
                    psa.setString(1, sessionIP);
                    psa.execute();
                }
                ret++;
            }
            if (macs != null) {
                String[] macz;
                macz = macs.split(", ");
                for (String mac : macz) {
                    if (!mac.equals("")) {
                        try (PreparedStatement psa = con.prepareStatement("DELETE FROM macbans WHERE mac = ?")) {
                            psa.setString(1, mac);
                            psa.execute();
                        }
                    }
                }
                ret++;
            }
            ManagerDatabasePool.closeConnection(con);
            return ret;
        } catch (SQLException e) {
            System.err.println("Error while unbanning" + e);
            return -2;
        }
    }

    public static byte unban(String id, boolean isAccountId, boolean hellban) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps;
            if (isAccountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            ps.setString(1, id);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final int accid = rs.getInt(1);
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            final String sessionIP = rs.getString("sessionIP");
            final String email = rs.getString("email");
            rs.close();
            ps.close();

            ps = con.prepareStatement(
                    "UPDATE accounts SET banned = 0, banreason = NULL, tempban = DEFAULT, greason = NULL WHERE "
                    + (!hellban ? "id = ?" : ("email = ?" + (sessionIP == null ? "" : " OR sessionIP = ?"))));
            if (hellban) {
                ps.setString(1, email);
                if (sessionIP != null) {
                    ps.setString(2, sessionIP);
                }
            } else {
                ps.setInt(1, accid);
            }
            ps.execute();
            ps.close();
            ManagerDatabasePool.closeConnection(con);
            return 0;
        } catch (SQLException e) {
            System.err.println("Error while unbanning" + e);
            return -2;
        }
    }

    public int finishLogin() {
        login_mutex.lock();
        try {
            final byte state = getLoginState();
            if (state > MapleClient.LOGIN_NOTLOGGEDIN) { // already loggedin
                loggedIn = false;
                return 7;
            }
            updateLoginState(MapleClient.LOGIN_LOGGEDIN, getSessionIPAddress());
        } finally {
            login_mutex.unlock();
        }
        return 0;
    }

    public void clearInformation() {
        accountName = null;
        accId = -1;
        secondPassword = null;
        salt2 = null;
        gmLevel = 0;
        loggedIn = false;
        greason = (byte) 1;
        tempban = null;
        gender = (byte) -1;
        charInfo.clear();
    }

    public void loginData(String login) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?")) {
                ps.setString(1, login);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        accountName = login;
                        accId = rs.getInt("id");
                        secondPassword = rs.getString("2ndpassword");
                        salt2 = rs.getString("salt2");
                        gmLevel = rs.getInt("gm");
                        greason = rs.getByte("greason");
                        tempban = getTempBanCalendar(rs);
                        gender = rs.getByte("gender");
                    }

                    if (secondPassword != null && salt2 != null) {
                        secondPassword = LoginCrypto.rand_r(secondPassword);
                    }
                }
            }
            loggedIn = true;
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("ERROR" + e);
        }
    }

    public int login(String login, String pwd, boolean ipMacBanned) {
        int loginok = 5;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int banned = rs.getInt("banned");
                final String passhash = rs.getString("password");
                // System.out.println("密碼哈希值 - " + passhash);
                final String salt = rs.getString("salt");
                final String tempPassword = rs.getString("temppassword");
                final long lasttemp = rs.getTimestamp("lasttemp") == null ? 0 : rs.getTimestamp("lasttemp").getTime();
                final String oldSession = rs.getString("SessionIP");
                accountName = login;
                accId = rs.getInt("id");
                secondPassword = rs.getString("2ndpassword");
                salt2 = rs.getString("salt2");
                gmLevel = rs.getInt("gm");
                greason = rs.getByte("greason");
                tempban = getTempBanCalendar(rs);
                gender = rs.getByte("gender");

                if (secondPassword != null && salt2 != null) {
                    secondPassword = LoginCrypto.rand_r(secondPassword);
                }

                if (banned > 0 && rs.getLong("tempban") != 0
                        && tempban.getTimeInMillis() != rs.getTimestamp("tempban").getTime()) {
                    banned = -1;
                }

                ps.close();

                if (banned > 0) {
                    loginok = 2;
                } else {
                    if (banned == -1) {
                        unban();
                    }
                    boolean updatePasswordHash = false;
                    // Check if the passwords are correct here. :B
                    if (tempPassword != null) {
                        if (pwd.equals(tempPassword) && lasttemp != 0
                                && lasttemp + (10 * 60 * 1000) >= System.currentTimeMillis()) {
                            loginok = 0;
                        } else {
                            loginok = 4;
                            loggedIn = false;
                        }
                    } else if (passhash == null || passhash.isEmpty()) {
                        // match by sessionIP
                        if (oldSession != null && !oldSession.isEmpty()) {
                            loggedIn = getSessionIPAddress().equals(oldSession);
                            loginok = loggedIn ? 0 : 4;
                        } else {
                            loginok = 4;
                            loggedIn = false;
                        }
                    } else if (LoginCryptoLegacy.isLegacyPassword(passhash)
                            && LoginCryptoLegacy.checkPassword(pwd, passhash)) {
                        // Check if a password upgrade is needed.
                        loginok = 0;
                        updatePasswordHash = true;
                    } else if (salt == null && LoginCrypto.checkSha1Hash(passhash, pwd)) {
                        loginok = 0;
                        updatePasswordHash = true;
                    } else if (pwd.equals(passhash)) {
                        // 檢查密碼是否未做任何加密
                        loginok = 0;
                        updatePasswordHash = true;
                    } else if (LoginCrypto.checkSaltedSha512Hash(passhash, pwd, salt)) {
                        loginok = 0;
                    } else {
                        loggedIn = false;
                        loginok = 4;
                    }
                    if (updatePasswordHash) {
                        try (PreparedStatement pss = con
                                .prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE id = ?")) {
                            final String newSalt = LoginCrypto.makeSalt();
                            pss.setString(1, LoginCrypto.makeSaltedSha512Hash(pwd, newSalt));
                            pss.setString(2, newSalt);
                            pss.setInt(3, accId);
                            pss.executeUpdate();
                        }
                    }

                    if (getLoginState() > MapleClient.LOGIN_NOTLOGGEDIN) { // already loggedin
                        if (loginok != 0) {
                            loggedIn = false;
                            loginok = 7;
                        } else {// 卡號解卡处理
                            boolean unLocked = false;
                            boolean saving = false;
                            for (final MapleClient c : World.Client.getClients()) {
                                if (c == this) {
                                    continue;
                                }
                                if (c.getAccID() == accId) {
                                    if (c.isSaving()) {
                                        saving = true;
                                        break;
                                    } else if (!c.getSession().isActive()) {
                                        c.disconnect();
                                        List<String> charName = c.loadCharacterNames(c.getWorld());
                                        for (final String cha : charName) {
                                            MapleCharacter chr = CashShopServer.getPlayerStorage()
                                                    .getCharacterByName(cha);
                                            if (chr != null) {
                                                CashShopServer.getPlayerStorage().deregisterPlayer(chr);
                                                break;
                                            }
                                        }
                                        for (ChannelServer cs : ChannelServer.getAllInstances()) {
                                            for (final String cha : charName) {
                                                MapleCharacter chr = cs.getPlayerStorage().getCharacterByName(cha);
                                                if (chr != null) {
                                                    cs.removePlayer(chr);
                                                    break;
                                                }
                                            }
                                        }
                                        break;
                                    }
                                    c.unLockDisconnect();
                                    unLocked = true;
                                }
                            }
                            if (saving) {
                                loggedIn = false;
                                loginok = 7;
                            } else if (!unLocked) {
                                try {
                                    ps = con.prepareStatement("UPDATE accounts SET loggedin = 0 WHERE name = ?");
                                    ps.setString(1, accountName);
                                    ps.executeUpdate();
                                    ps.close();
                                } catch (SQLException se) {
                                }
                            }
                        }
                    }
                }
            }
            rs.close();
            ps.close();
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("ERROR" + e);
        }
        return loginok;
    }

    public boolean CheckSecondPassword(String in) {
        boolean allow = false;
        boolean updatePasswordHash = false;

        if (secondPassword == null || secondPassword.isEmpty()) {
            loginData(accountName);
        }

        // Check if the passwords are correct here. :B
        if (LoginCryptoLegacy.isLegacyPassword(secondPassword) && LoginCryptoLegacy.checkPassword(in, secondPassword)) {
            // Check if a password upgrade is needed.
            allow = true;
            updatePasswordHash = true;
        } else if (salt2 == null && LoginCrypto.checkSha1Hash(secondPassword, in)) {
            allow = true;
            updatePasswordHash = true;
        } else if (in.equals(secondPassword)) {
            // 檢查密碼是否未做任何加密
            allow = true;
            updatePasswordHash = true;
        } else if (LoginCrypto.checkSaltedSha512Hash(secondPassword, in, salt2)) {
            allow = true;
        }
        if (updatePasswordHash) {

            try {
                Connection con = ManagerDatabasePool.getConnection();
                try (PreparedStatement ps = con
                        .prepareStatement("UPDATE `accounts` SET `2ndpassword` = ?, `salt2` = ? WHERE id = ?")) {
                    final String newSalt = LoginCrypto.makeSalt();
                    ps.setString(1, LoginCrypto.rand_s(LoginCrypto.makeSaltedSha512Hash(in, newSalt)));
                    ps.setString(2, newSalt);
                    ps.setInt(3, accId);
                    ps.executeUpdate();
                }
                ManagerDatabasePool.closeConnection(con);
            } catch (SQLException e) {
                return false;
            }
        }
        return allow;
    }

    public void setAccID(int id) {
        this.accId = id;
    }

    public int getAccID() {
        return this.accId;
    }

    public final void clearTempPassword() {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET lasttemp = ? WHERE id = ?")) {
                ps.setNull(1, Types.TIMESTAMP);
                ps.setInt(2, getAccID());
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("清除動態密碼錯誤:" + e);
        }
    }

    public final void updateLoginState(final int newstate, final String SessionID) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE accounts SET loggedin = ?, SessionIP = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?")) {
                ps.setInt(1, newstate);
                ps.setString(2, SessionID);
                ps.setInt(3, getAccID());
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("error updating login state " + e);
        }
        if (newstate == MapleClient.LOGIN_NOTLOGGEDIN) {
            loggedIn = false;
            serverTransition = false;
        } else {
            serverTransition = (newstate == MapleClient.LOGIN_SERVER_TRANSITION
                    || newstate == MapleClient.CHANGE_CHANNEL);
            loggedIn = !serverTransition;
        }
    }

    public byte getAccountWorld(String accountname) {
        byte ret = 0;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT lastWorld FROM accounts WHERE name = ?")) {
                ps.setString(1, accountname);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    ret = rs.getByte(1);
                }
                ret = rs.getByte(1);
                rs.close();
                ps.close();
            }
            ManagerDatabasePool.closeConnection(con);
            return ret;
        } catch (SQLException ex) {
            System.err.println("Failed to fetch data: " + ex.toString());
        }
        return ret;
    }

    public final void updateSecondPassword() {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("UPDATE `accounts` SET `2ndpassword` = ?, `salt2` = ? WHERE id = ?")) {
                final String newSalt = LoginCrypto.makeSalt();
                ps.setString(1, LoginCrypto.rand_s(LoginCrypto.makeSaltedSha512Hash(secondPassword, newSalt)));
                ps.setString(2, newSalt);
                ps.setInt(3, accId);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
            System.err.println("error updating login state " + e);
        }
    }

    public final byte getLoginState() {
        try {
            byte state;
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement(
                    "SELECT loggedin, lastlogin, banned, `birthday` + 0 AS `bday` FROM accounts WHERE id = ?");
            ps.setInt(1, getAccID());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getInt("banned") > 0) {
                    ps.close();
                    rs.close();
                    session.close();
                    throw new DatabaseException("Account doesn't exist or is banned");
                }
                birthday = rs.getInt("bday");
                state = rs.getByte("loggedin");
                if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
                    if (rs.getTimestamp("lastlogin").getTime() + 20000 < System.currentTimeMillis()) { // connecting to
                        // chanserver
                        // timeout
                        state = MapleClient.LOGIN_NOTLOGGEDIN;
                        updateLoginState(state, getSessionIPAddress());
                    }
                }
            }
            ps.close();
            ManagerDatabasePool.closeConnection(con);
            loggedIn = state == MapleClient.LOGIN_LOGGEDIN;
            return state;
        } catch (SQLException e) {
            loggedIn = false;
            throw new DatabaseException("error getting login state", e);
        }
    }

    public final boolean checkBirthDate(final int date) {
        return birthday == date;
    }

    public final void removalTask(boolean shutdown) {
        try {
            player.cancelAllBuffs_();
            player.cancelAllDebuffs();
            if (player.getMarriageId() > 0) {
                final MapleQuestStatus stat1 = player.getQuestNoAdd(MapleQuest.getInstance(160001));
                final MapleQuestStatus stat2 = player.getQuestNoAdd(MapleQuest.getInstance(160002));
                if (stat1 != null && stat1.getCustomData() != null
                        && (stat1.getCustomData().equals("2_") || stat1.getCustomData().equals("2"))) {
                    // dc in process of marriage
                    if (stat2 != null && stat2.getCustomData() != null) {
                        stat2.setCustomData("0");
                    }
                    stat1.setCustomData("3");
                }
            }
            if (player.getMapId() == GameConstants.JAIL) {
                final MapleQuestStatus stat1 = player.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME));
                final MapleQuestStatus stat2 = player.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST));
                if (stat1.getCustomData() == null) {
                    stat1.setCustomData(String.valueOf(System.currentTimeMillis()));
                } else if (stat2.getCustomData() == null) {
                    stat2.setCustomData("0"); // seconds of jail
                } else { // previous seconds - elapsed seconds
                    int seconds = Integer.parseInt(stat2.getCustomData())
                            - (int) ((System.currentTimeMillis() - Long.parseLong(stat1.getCustomData())) / 1000);
                    if (seconds < 0) {
                        seconds = 0;
                    }
                    stat2.setCustomData(String.valueOf(seconds));
                }
            }
            player.changeRemoval(true);
            if (player.getEventInstance() != null) {
                player.getEventInstance().playerDisconnected(player, player.getId());
            }
            final IMaplePlayerShop shop = player.getPlayerShop();
            if (shop != null) {
                shop.removeVisitor(player);
                if (shop.isOwner(player)) {
                    if (shop.getShopType() == 1 && shop.isAvailable() && !shutdown) {
                        shop.setOpen(true);
                    } else {
                        shop.closeShop(true, !shutdown);
                    }
                }
            }
            player.setMessenger(null);
            if (player.getMap() != null) {
                if (shutdown || (getChannelServer() != null && getChannelServer().isShutdown())) {
                    int questID = -1;
                    switch (player.getMapId()) {
                        case 240060200: // HT
                            questID = 160100;
                            break;
                        case 240060201: // ChaosHT
                            questID = 160103;
                            break;
                        case 280030000: // Zakum
                            questID = 160101;
                            break;
                        case 280030001: // ChaosZakum
                            questID = 160102;
                            break;
                        case 270050100: // PB
                            questID = 160101;
                            break;
                        case 105100300: // Balrog
                        case 105100400: // Balrog
                            questID = 160106;
                            break;
                        case 211070000: // VonLeon
                        case 211070100: // VonLeon
                        case 211070101: // VonLeon
                        case 211070110: // VonLeon
                            questID = 160107;
                            break;
                        case 551030200: // scartar
                            questID = 160108;
                            break;
                        case 271040100: // cygnus
                            questID = 160109;
                            break;
                        case 262030000:
                        case 262031300: // hilla
                            questID = 160110;
                            break;
                        case 272030400:
                            questID = 160111;
                            break;
                    }
                    if (questID > 0) {
                        player.getQuestNAdd(MapleQuest.getInstance(questID)).setCustomData("0"); // reset the time.
                    }
                } else if (player.isAlive()) {
                    switch (player.getMapId()) {
                        case 541010100: // latanica
                        case 541020800: // krexel
                        case 220080001: // pap
                            player.getMap().addDisconnected(player.getId());
                            break;
                    }
                }
                player.getMap().removePlayer(player);
            }
        } catch (final NumberFormatException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.Acc_Stuck, e);
        }
    }

    public final void unLockDisconnect() {
        getSession().writeAndFlush(CWvsContext.broadcastMsg(1, "當前賬號在別處登入\r\n若不是你本人操作請及時更改密碼。"));
        disconnect(serverTransition, getChannel() == MapleServerHandler.CASH_SHOP_SERVER);
        final MapleClient client = this;
        Thread closeSession = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(3000);
                } catch (InterruptedException ex) {
                }
                client.getSession().close();
                System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
        };
        try {
            closeSession.start();
        } catch (Exception ex) {
        }
    }

    public final void disconnect() {
        disconnect(true);
    }

    public final void disconnect(final boolean removeInChannelServer) {
        disconnect(removeInChannelServer, getChannel() == MapleServerHandler.CASH_SHOP_SERVER);
    }

    public final void disconnect(final boolean removeInChannelServer, final boolean fromCS) {
        disconnect(removeInChannelServer, fromCS, false);
    }

    public final void disconnect(final boolean removeInChannelServer, final boolean fromCS, final boolean shutdown) {
        if (player != null) {
            System.out.println(getSessionIPAddress() + "下線處理(已上角色)");
            MapleMap map = player.getMap();
            final MapleParty party = player.getParty();
            final boolean clone = player.isClone();
            final String namez = player.getName();
            final int idz = player.getId(),
                    messengerid = player.getMessenger() == null ? 0 : player.getMessenger().getId(),
                    gid = player.getGuildId();
            final BuddyList bl = player.getBuddylist();
            final MaplePartyCharacter chrp = new MaplePartyCharacter(player);
            final MapleMessengerCharacter chrm = new MapleMessengerCharacter(player);
            final MapleGuildCharacter chrg = player.getMGC();

            removalTask(shutdown);
            LoginServer.getLoginAuth(player.getId());
            player.saveToDB(true, fromCS);
            if (shutdown) {
                player = null;
                receiving = false;
                charPos.clear();
                return;
            }

            if (!fromCS) {
                charPos.clear();
                final ChannelServer ch = ChannelServer.getInstance(map == null ? channel : map.getChannel());
                final int chz = World.Find.findChannel(idz);
                if (chz < -1) {
                    disconnect(removeInChannelServer, true);// u lie
                    return;
                }
                try {
                    if (chz == -1 || ch == null || clone || ch.isShutdown()) {
                        player = null;
                        return;// no idea
                    }
                    if (messengerid > 0) {
                        World.Messenger.leaveMessenger(messengerid, chrm);
                    }
                    if (party != null) {
                        chrp.setOnline(false);
                        World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, chrp);
                        if (map != null && party.getLeader().getId() == idz) {
                            MaplePartyCharacter lchr = null;
                            for (MaplePartyCharacter pchr : party.getMembers()) {
                                if (pchr != null && map.getCharacterById(pchr.getId()) != null
                                        && (lchr == null || lchr.getLevel() < pchr.getLevel())) {
                                    lchr = pchr;
                                }
                            }
                            if (lchr != null) {
                                World.Party.updateParty(party.getId(), PartyOperation.PartyRes_ChangePartyBoss_Done_DC,
                                        lchr);
                            }
                        }
                    }
                    if (bl != null) {
                        if (!serverTransition) {
                            World.Buddy.loggedOff(namez, idz, channel, bl.getBuddyIds());
                        } else { // Change channel
                            World.Buddy.loggedOn(namez, idz, channel, bl.getBuddyIds());
                        }
                    }
                    if (gid > 0 && chrg != null) {
                        World.Guild.setGuildMemberOnline(chrg, false, -1);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    FileoutputUtil.outputFileError(FileoutputUtil.Acc_Stuck, e);
                    System.err.println(getLogMessage(this, "ERROR") + e);
                } finally {
                    if (removeInChannelServer && ch != null) {
                        ch.removePlayer(idz, namez);
                    }
                    player = null;
                }
            } else {
                final int ch = World.Find.findChannel(idz);
                if (ch > 0) {
                    disconnect(removeInChannelServer, false);// u lie
                    return;
                }
                try {
                    if (party != null) {
                        chrp.setOnline(false);
                        World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, chrp);
                    }
                    if (!serverTransition) {
                        World.Buddy.loggedOff(namez, idz, channel, bl.getBuddyIds());
                    } else { // Change channel
                        World.Buddy.loggedOn(namez, idz, channel, bl.getBuddyIds());
                    }
                    if (gid > 0 && chrg != null) {
                        World.Guild.setGuildMemberOnline(chrg, false, -1);
                    }
                    if (player != null) {
                        player.setMessenger(null);
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    FileoutputUtil.outputFileError(FileoutputUtil.Acc_Stuck, e);
                    System.err.println(getLogMessage(this, "ERROR") + e);
                } finally {
                    if (removeInChannelServer && ch > 0) {
                        CashShopServer.getPlayerStorage().deregisterPlayer(idz, namez);
                    }
                    player = null;
                }
            }
        } else {
            System.out.println(getSessionIPAddress() + "下線處理(未上角色)");
        }
        if (!serverTransition && isLoggedIn()) {
            updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN, getSessionIPAddress());
        }
        engines.clear();
    }

    public final String getSessionIPAddress() {
        if (session != null && session.remoteAddress() != null) {
            return session.remoteAddress().toString().split(":")[0];
        } else {
            return getLastIPAddress();
        }
    }

    public final String getLastIPAddress() {
        String sessionIP = null;
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT SessionIP FROM accounts WHERE id = ?")) {
                ps.setInt(1, this.accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        sessionIP = rs.getString("SessionIP");
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (final SQLException e) {
            System.err.println("Failed in checking IP address for client.");
        }
        return sessionIP == null ? "" : sessionIP;
    }

    public final boolean CheckIPAddress() {
        if (this.accId < 0) {
            return false;
        }
        try {
            boolean canlogin = false;
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT SessionIP, banned FROM accounts WHERE id = ?");) {
                ps.setInt(1, this.accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        final String sessionIP = rs.getString("SessionIP");
                        if (sessionIP != null) { // Probably a login proced skipper?
                            canlogin = getSessionIPAddress().equals(sessionIP.split(":")[0]);
                        }
                        if (rs.getInt("banned") > 0) {
                            canlogin = false; // canlogin false = close client
                        }
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
            return canlogin;
        } catch (final SQLException e) {
            System.out.println("Failed in checking IP address for client.");
        }
        return true;
    }

    public final void DebugMessage(final StringBuilder sb) {
        sb.append("IP:");
        sb.append(getSession().remoteAddress());
        sb.append(" 連接狀態:");
        sb.append(getSession().isActive());
        sb.append(" ClientKeySet:");
        sb.append(getSession().attr(MapleClient.CLIENT_KEY).get() != null);
        sb.append(" 是否已登入:");
        sb.append(isLoggedIn());
        sb.append(" 角色上線:");
        sb.append(getPlayer() != null);
    }

    public final int getChannel() {
        return channel;
    }

    public final ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }

    public final int deleteCharacter(final int cid) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT guildid, guildrank, name FROM characters WHERE id = ? AND accountid = ?")) {
                ps.setInt(1, cid);
                ps.setInt(2, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return 9;
                    }
                    if (rs.getInt("guildid") > 0) { // is in a guild when deleted
                        if (rs.getInt("guildrank") == 1) { // cant delete when leader
                            rs.close();
                            ps.close();
                            return 22;
                        }
                        World.Guild.deleteGuildCharacter(rs.getInt("guildid"), cid);
                    }
                }
            }

            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM characters WHERE id = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM hiredmerch WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM cheatlog WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mountdata WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM inventoryitems WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM famelog WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM famelog WHERE characterid_to = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM dueypackages WHERE RecieverId = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM buddies WHERE buddyid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM keymap WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM regrocklocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM hyperrocklocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM familiars WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mountdata WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM extendedSlots WHERE characterid = ?", cid);
            MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM charactercustomdata WHERE characterid = ?", cid);
            loadCharacterPos();
            ManagerDatabasePool.closeConnection(con);
            return 0;
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
        }
        return 10;
    }

    public final byte getGender() {
        return gender;
    }

    public final void setGender(final byte gender) {
        this.gender = gender;
    }

    public final String getSecondPassword() {
        return secondPassword;
    }

    public final void setSecondPassword(final String secondPassword) {
        this.secondPassword = secondPassword;
    }

    public final String getAccountName() {
        return accountName;
    }

    public final boolean checkSecuredAccountName(String accountName) {
        if (getAccountName().length() != accountName.length()) {
            return false;
        }
        for (int i = 0; i < accountName.length(); i++) {
            if (accountName.charAt(i) == '*') {
                continue;
            }
            if (accountName.charAt(i) != this.accountName.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public final String getSecurityAccountName() {
        StringBuilder sb = new StringBuilder(accountName);
        if (sb.length() >= 4) {
            sb.replace(1, 3, "**");
        } else if (sb.length() >= 3) {
            sb.replace(1, 2, "*");
        }
        if (sb.length() > 4) {
            sb.replace(sb.length() - 1, sb.length(), "*");
        }
        return sb.toString();
    }

    public final void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public final void setChannel(final int channel) {
        this.channel = channel;
    }

    public final int getWorld() {
        return world;
    }

    public final void setWorld(final int world) {
        this.world = world;
    }

    public final int getLatency() {
        return (int) (lastPong - lastPing);
    }

    public final long getLastPong() {
        return lastPong;
    }

    public final long getLastPing() {
        return lastPing;
    }

    public final void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public final void sendPing() {
        lastPing = System.currentTimeMillis();
        getSession().writeAndFlush(LoginPacket.getPing());

        PingTimer.getInstance().schedule(() -> {
            try {
                if (getLatency() < 0) {
                    disconnect(true, false);
                    if (getSession().isActive()) {
                        getSession().close();
                        System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                    }
                }
            } catch (final NullPointerException e) {
                // Client Is Gone :O
            }
        }, 180000); // 3 Mins
    }

    public static String getLogMessage(final MapleClient cfor, final String message) {
        return getLogMessage(cfor, message, new Object[0]);
    }

    public static String getLogMessage(final MapleCharacter cfor, final String message) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message);
    }

    public static String getLogMessage(final MapleCharacter cfor, final String message, final Object... parms) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message, parms);
    }

    public static String getLogMessage(final MapleClient cfor, final String message, final Object... parms) {
        final StringBuilder builder = new StringBuilder();
        if (cfor != null) {
            if (cfor.getPlayer() != null) {
                builder.append(MapleCharacterUtil.makeMapleReadable(cfor.getPlayer().getName()));
                builder.append("(ID:");
                builder.append(cfor.getPlayer().getId());
                builder.append(")");
            }
            if (cfor.getAccountName() != null) {
                builder.append("[帳號:");
                builder.append(cfor.getAccountName());
                builder.append("]");
            }
        }
        builder.append(message);
        int start;
        for (final Object parm : parms) {
            start = builder.indexOf("{}");
            builder.replace(start, start + 2, parm.toString());
        }
        return builder.toString();
    }

    public static int findAccIdForCharacterName(final String charName) {
        try {
            int ret;
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                ps.setString(1, charName);
                try (ResultSet rs = ps.executeQuery()) {
                    ret = -1;
                    if (rs.next()) {
                        ret = rs.getInt("accountid");
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
            return ret;
        } catch (final SQLException e) {
            System.err.println("findAccIdForCharacterName SQL error");
        }
        return -1;
    }

    public boolean isIntern() {
        return gmLevel >= PlayerGMRank.INTERN.getLevel();
    }

    public boolean isGM() {
        return gmLevel >= PlayerGMRank.GM.getLevel();
    }

    public boolean isSuperGM() {
        return gmLevel >= PlayerGMRank.SUPERGM.getLevel();
    }

    public boolean isAdmin() {
        return gmLevel >= PlayerGMRank.ADMIN.getLevel();
    }

    public int getGmLevel() {
        return gmLevel;
    }

    public final void setGmLevel(PlayerGMRank rank) {
        this.gmLevel = rank.getLevel();
    }

    public final void setScriptEngine(final String name, final ScriptEngine e) {
        engines.put(name, e);
    }

    public final ScriptEngine getScriptEngine(final String name) {
        return engines.get(name);
    }

    public final void removeScriptEngine(final String name) {
        engines.remove(name);
    }

    public final ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public final void setIdleTask(final ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    protected static final class CharNameAndId {

        public final String name;
        public final int id;

        public CharNameAndId(final String name, final int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }

    public int getCharacterSlots() {
        if (FeaturesConfig.defaultFullCharslot) {
            return GameConstants.CHARSLOT_MAX;
        } else if (charslots < GameConstants.DEFAULT_CHARSLOT) {
            return GameConstants.DEFAULT_CHARSLOT;
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT * FROM character_slots WHERE accid = ? AND worldid = ?")) {
                ps.setInt(1, accId);
                ps.setInt(2, world);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        charslots = Math.min(rs.getInt("charslots"), GameConstants.CHARSLOT_MAX);
                    } else {
                        try (PreparedStatement psu = con.prepareStatement(
                                "INSERT INTO character_slots (accid, worldid, charslots) VALUES (?, ?, ?)")) {
                            psu.setInt(1, accId);
                            psu.setInt(2, world);
                            psu.setInt(3, charslots);
                            psu.executeUpdate();
                        }
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException sqlE) {
        }

        return charslots;
    }

    public boolean gainCharacterSlot() {
        if (getCharacterSlots() >= GameConstants.CHARSLOT_MAX) {
            return false;
        }
        charslots = Math.min(charslots + 1, GameConstants.CHARSLOT_MAX);
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("UPDATE character_slots SET charslots = ? WHERE worldid = ? AND accid = ?")) {
                ps.setInt(1, charslots);
                ps.setInt(2, world);
                ps.setInt(3, accId);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException sqlE) {
            return false;
        }
        return true;
    }

    public boolean isMonitored() {
        return monitored;
    }

    public void setMonitored(boolean m) {
        this.monitored = m;
    }

    public boolean isReceiving() {
        return receiving;
    }

    public void setReceiving(boolean m) {
        this.receiving = m;
    }

    public boolean canClickNPC() {
        return lastNpcClick + 500 < System.currentTimeMillis();
    }

    public void setClickedNPC() {
        lastNpcClick = System.currentTimeMillis();
    }

    public void removeClickedNPC() {
        lastNpcClick = 0;
    }

    public final Timestamp getCreated() {

        try {
            Timestamp ret;
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT createdat FROM accounts WHERE id = ?")) {
                ps.setInt(1, getAccID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return null;
                    }
                    ret = rs.getTimestamp("createdat");
                }
            }
            ManagerDatabasePool.closeConnection(con);
            return ret;
        } catch (SQLException e) {
            throw new DatabaseException("error getting create", e);
        }
    }

    public String getTempIP() {
        return tempIP;
    }

    public void setTempIP(String s) {
        this.tempIP = s;
    }

    public int getNextClientIncrenement() {
        int result = client_increnement;
        client_increnement++;
        return result;
    }

    public void setFarm(MapleFarm farm) {
        this.farm = farm;
    }

    public MapleFarm getFarm() {
        if (farm == null) {
            return MapleFarm.getDefault(35549721, this, "Creating...");
        }
        return farm;
    }

    public List<String> getRanking(int limit) {
        List<String> ret = new LinkedList<>();

        try {
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps = con
                    .prepareStatement("SELECT `name`, `gm` FROM `characters` ORDER BY `level` DESC LIMIT " + limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("gm") <= PlayerGMRank.NORMAL.getLevel()) {
                    ret.add(rs.getString("name"));
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    private void loadValue() {
        CustomValues = new HashMap<>();
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accountcustomdata WHERE accountid = ?")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CustomValues.put(rs.getString("key"), rs.getString("data"));
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
        }
    }

    private void updateValue() {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM accountcustomdata WHERE accountid = ?");
            ps.setInt(1, accId);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("INSERT INTO accountcustomdata (accountid, `key`, `data`) VALUES (?, ?, ?)");
            ps.setInt(1, accId);
            for (Map.Entry<String, String> entry : CustomValues.entrySet()) {
                ps.setString(2, entry.getKey());
                ps.setString(3, entry.getValue());
                ps.execute();
            }
            ps.close();
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
        }
    }

    public void setValue(String arg, String values) {
        if (CustomValues == null) {
            loadValue();
        }
        if (CustomValues.containsKey(arg)) {
            CustomValues.remove(arg);
        }
        if (values == null) {
            return;
        }
        CustomValues.put(arg, values);
        updateValue();
    }

    public String getValue(String arg) {
        if (CustomValues == null) {
            loadValue();
        }
        if (CustomValues.containsKey(arg)) {
            return CustomValues.get(arg);
        }
        return null;
    }

    public String getOneValue(String arg, String key) {
        if (CustomValues == null) {
            loadValue();
        }
        if (!CustomValues.containsKey(arg)) {
            return null;
        }
        return StringTool.getOneValue(CustomValues.get(arg), key);
    }

    public void updateOneValue(final String arg, final String key, final String value) {
        if (key == null) {
            return;
        }
        String info = StringTool.updateOneValue(getValue(arg), key, value);
        if (info == null) {
            return;
        }
        if (info.isEmpty()) {
            setValue(arg, null);
        } else {
            setValue(arg, info);
        }
    }

    public void setTempValue(String arg, String values) {
        if (TempValues.containsKey(arg)) {
            TempValues.remove(arg);
        }
        if (values == null) {
            return;
        }
        TempValues.put(arg, values);
    }

    public String getTempValue(String arg) {
        if (TempValues.containsKey(arg)) {
            return TempValues.get(arg);
        }
        return null;
    }

    public String getOneTempValue(String arg, String key) {
        if (!TempValues.containsKey(arg)) {
            return null;
        }
        return StringTool.getOneValue(TempValues.get(arg), key);
    }

    public void updateOneTempValue(final String arg, final String key, final String value) {
        if (key == null) {
            return;
        }
        String info = StringTool.updateOneValue(getTempValue(arg), key, value);
        if (info == null) {
            return;
        }
        if (info.isEmpty()) {
            setTempValue(arg, null);
        } else {
            setTempValue(arg, info);
        }
    }

    public void setSaving(boolean save) {
        isSaving = save;
    }

    public boolean isSaving() {
        return isSaving;
    }
}
