package handling.login.handler;

import client.ClientRedirector;
import client.LoginCrypto;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleJob;
import client.PartTimeJob;
import client.skill.Skill;
import client.skill.SkillEntry;
import client.skill.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import constants.ServerConfig;
import constants.ServerConstants;
import constants.WorldConstants;
import database.ManagerDatabasePool;
import server.swing.WvsCenter;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginInformationProvider.JobInfoFlag;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.login.LoginWorker;
import handling.world.World;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.DateUtil;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.StringUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.LoginPacket;

public class CharLoginHandler {

    private static boolean loginFailCount(final MapleClient c) {
        if (c.isAdmin()) {
            return false;
        }
        c.loginAttempt++;
        return c.loginAttempt > 4;
    }

    public static void handleAuthRequest(final LittleEndianAccessor slea, final MapleClient c) {
        // System.out.println("Sending response to client.");
        int request = slea.readInt();
        int response;

        response = ((request >> 5) << 5) + (((((request & 0x1F) >> 3) ^ 2) << 3) + (7 - (request & 7)));
        response |= ((request >> 7) << 7);
        response -= 1; // -1 again on v143

        c.announce(LoginPacket.sendAuthResponse(response));
    }

    public static final void login(final LittleEndianAccessor slea, final MapleClient c) {
        int[] bytes = new int[6];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = slea.readByteAsInt();
        }
        StringBuilder sps = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sps.append(StringUtil.getLeftPaddedStr(Integer.toHexString(bytes[i]).toUpperCase(), '0', 2));
            sps.append("-");
        }
        String macData = sps.toString();
        macData = macData.substring(0, macData.length() - 1);
        c.setMac(macData);

        slea.skip(16);

        String login = slea.readMapleAsciiString()/* .replace("NP12:auth06:5:0:","") */;
        String pwd = slea.readMapleAsciiString();

        final boolean ipBan = c.hasBannedIP();
        final boolean macBan = c.hasBannedMac();
        final boolean ban = ipBan || macBan;

        int loginok = c.login(login, pwd, ban);
        String errorInfo = null;

        if (loginok == 0 && ban && !c.isGM()) {
            // 被封鎖IP或MAC的非GM帳號成功登入處理
            loginok = 3;
            if (macBan) {
                // this is only an ipban o.O" - maybe we should refactor this a bit so it's more
                // readable
                MapleClient.banIP(c.getSession().remoteAddress().toString().split(":")[0]);
            }
        } else if (loginok == 0 && (c.getGender() == 10 || c.getSecondPassword() == null)) {
            // 選擇性别並設置第二組密碼
            // c.updateLoginState(MapleClient.CHOOSE_GENDER, c.getSessionIPAddress());
            c.announce(LoginPacket.genderNeeded());
            return;
        } else if (loginok == 5) {
            // 帳號不存在
            if (ServerConfig.isAutoRegister()) {
                // 避免帳號長度過長發生異常
                if (login.length() >= 12) {
                    errorInfo = "您的帳號長度過長！\r\n請重新輸入帳號。";
                } else if (AutoRegister.createAccount(login, pwd, c.getSession().remoteAddress().toString(), macData)) {
                    errorInfo = "註冊帳號成功。\r\n請重新輸入賬號密碼進入遊戲。";
                } else {
                    errorInfo = "註冊帳號失敗，可能原因如：\r\n1.伺服器錯誤，請回報給管理員。\r\n2.你所使用的電腦超過註冊次數。";
                }
            } else {
                errorInfo = "帳號註冊失敗，未開啟自動註冊功能，請到網站註冊帳號。";
            }
            loginok = 1;
        }

        if (loginok == 0) {
            c.loginAttempt = 0;
            c.updateMacs();
            c.clearTempPassword();
            LoginWorker.registerClient(c);
        } else {
            if (loginFailCount(c)) {
                c.clearInformation();
                c.getSession().close();
                System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                return;
            }
            if (loginok == 2) {
                long time = c.getTempBanCalendar().getTimeInMillis();
                c.announce(LoginPacket.getTempBan(time == 0 ? -1 : DateUtil.getFileTimestamp(time), c.getBanReason()));
            } else {
                c.announce(LoginPacket.getLoginFailed(loginok));
                if (errorInfo != null) {
                    c.announce(CWvsContext.broadcastMsg(1, errorInfo));
                }
            }
            c.clearInformation();
        }
    }

    public static void ServerListRequest(final MapleClient c) {
        c.announce(CField.onChnageBackground(null));
        for (WorldConstants.Option servers : WorldConstants.values()) {
            if (servers.show()) {
                c.announce(LoginPacket.onWorldInformation(servers));
                if (ServerConstants.MAPLE_TYPE == ServerConstants.MapleType.GLOBAL) {
                    c.announce(LoginPacket.getWorldSelected(c));
                }
            }
        }
        c.announce(LoginPacket.onWorldInformation());
        boolean hasCharacters = false;
        for (int world = 0; world < WorldConstants.values().length; world++) {
            final List<MapleCharacter> chars = c.loadCharacters(world);
            if (chars != null) {
                hasCharacters = true;
                break;
            }
        }
        if (!hasCharacters) {
            c.announce(LoginPacket.enableRecommended(WorldConstants.recommended));
        }
        if (WorldConstants.recommended >= 0) {
            c.announce(LoginPacket.sendRecommended(WorldConstants.recommended, WorldConstants.recommendedmsg));
        }
    }

    public static void ServerStatusRequest(final MapleClient c) {
        // 0 = Select world normally
        // 1 = "Since there are many users, you may encounter some..."
        // 2 = "The concurrent users in this world have reached the max"
        final int numPlayer = LoginServer.getUsersOn();
        final int userLimit = LoginServer.getUserLimit();
        if (numPlayer >= userLimit) {
            c.announce(LoginPacket.getServerStatus(2));
        } else if (numPlayer * 2 >= userLimit) {
            c.announce(LoginPacket.getServerStatus(1));
        } else {
            c.announce(LoginPacket.getServerStatus(0));
        }
    }

    public static void CharlistRequest(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            return;
        }
        final int mode = slea.readByte(); // 2?
        final int server;
        final int channel;
        if (mode == 0) {
            server = slea.readByte();
            channel = slea.readByte() + 1;
        } else {
            slea.skip(1);
            String code = slea.readMapleAsciiString();
            Map<String, ClientRedirector> redirectors = World.Redirector.getRedirectors();
            ClientRedirector redirector;
            if (!redirectors.containsKey(code) || !redirectors.get(code).isLogined()) {
                if (!redirectors.get(code).isLogined()) {
                    redirectors.remove(code);
                }
                c.getSession().close();
                System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                return;
            } else {
                redirector = redirectors.remove(code);
            }
            server = redirector.getWorld();
            channel = redirector.getChannel();
        }
        if (!World.isChannelAvailable(channel, server) || !WorldConstants.isExists(server)) {
            c.announce(LoginPacket.getLoginFailed(10)); // cannot process so many
            return;
        }

        if (!WorldConstants.getById(server).isAvailable() && !(c.isGM() && server == WorldConstants.gmserver)) {
            c.announce(CWvsContext.broadcastMsg(1, "這個伺服器暫時無法連接. \r\n請嘗試連接其他伺服器."));
            c.announce(LoginPacket.getLoginFailed(1)); // Shows no message, but it is used to unstuck
            return;
        }

        // if (c.getMVPLevel() <= 0 && ChannelServer.getInstance(channel) != null &&
        // ChannelServer.getInstance(channel).getChannelType() ==
        // ChannelServer.ChannelType.MVP) {
        // c.announce(CWvsContext.broadcastMsg(1, "您沒有進入MVP頻道的權限."));
        // c.announce(LoginPacket.getLoginFailed(1));
        // return;
        // }
        System.out.println("用戶端地址: " + c.getSession().remoteAddress().toString().split(":")[0] + " 連接到伺服器: " + server
                + " 頻道: " + channel + "");
        final List<MapleCharacter> chars = c.loadCharacters(server);
        if (chars != null && ChannelServer.getInstance(channel) != null) {
            c.setWorld(server);
            c.setChannel(channel);
            if (ServerConstants.MAPLE_TYPE == ServerConstants.MapleType.GLOBAL || mode == 1) {
                c.announce(LoginPacket.getSecondAuthSuccess(c));
                c.announce(LoginPacket.getChannelSelected());
            }
            String worldType = "normal";
            WorldConstants.Option w = WorldConstants.getById(server);
            if (w == WorldConstants.WorldOption.REBOOT || w == WorldConstants.TespiaWorldOption.REBOOT) {
                worldType = "reboot";
            }
            c.announce(LoginPacket.OnSelectWorldResult(worldType, c.getSecondPassword(), c.getCharacterPos(), chars, c.getCharacterSlots()));
        } else {
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
        }
    }

    public static void changeCharPosition(final LittleEndianAccessor slea, final MapleClient c) {
        slea.readInt();
        slea.readByte();
        int count = slea.readInt();

        if (count != c.getCharacterPos().size()) {
            System.out.println("角色位置更變出錯: 更變個數與實際不符");
        }

        final ArrayList<Integer> newCharPos = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int pos = slea.readInt();
            if (c.getCharacterPos().contains(pos)) {
                newCharPos.add(pos);
            } else {
                System.out.println("角色位置更變出錯: 非本賬號的角色ID");
                return;
            }
        }
        c.updateCharacterPos(newCharPos);
    }

    public static void updateCCards(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.available() != 36 || !c.isLoggedIn()) {
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            return;
        }
        final Map<Integer, Integer> cids = new LinkedHashMap<>();
        for (int i = 1; i <= 9; i++) {
            final int charId = slea.readInt();
            if ((!c.login_Auth(charId) && charId != 0) || ChannelServer.getInstance(c.getChannel()) == null
                    || !WorldConstants.isExists(c.getWorld())) {
                c.getSession().close();
                System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                return;
            }
            cids.put(i, charId);
        }
        c.updateCharacterCards(cids);
    }

    public static void CheckCharName(final String name, final MapleClient c) {
        LoginInformationProvider li = LoginInformationProvider.getInstance();
        boolean nameUsed = true;
        if (MapleCharacterUtil.canCreateChar(name, c.isGM())) {
            nameUsed = false;
        }
        if (li.isForbiddenName(name) && !c.isGM()) {
            nameUsed = false;
        }
        c.announce(LoginPacket.charNameResponse(name, nameUsed));
    }

    public static void CreateChar2Pw(final LittleEndianAccessor slea, final MapleClient c) {
        final String Secondpw_Client = slea.readMapleAsciiString();

        if (!c.isLoggedIn() || loginFailCount(c) || c.getSecondPassword() == null) {
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            return;
        }
        byte state = 0;

        if (!c.CheckSecondPassword(Secondpw_Client)) { // Wrong Password
            state = 20;
        }

        c.announce(LoginPacket.createCharResponse(state));
        // 驗證碼
        // c.announce(LoginPacket.createCharCheckCode(CheckCodeImageCreator.createCheckCode().getRight(),
        // (byte) 0, (byte) 1, (byte) 1, (byte) 0));
    }

    public static void CreateCharClick(final LittleEndianAccessor slea, final MapleClient c) {
        c.announce(LoginPacket.secondPasswordWindows(3, 0));
    }

    public static void CreateChar(final LittleEndianAccessor slea, final MapleClient c) {
        byte gender, skin, unk;
        short subcategory;
        Map<JobInfoFlag, Integer> infos = new LinkedHashMap<>();

        String name = slea.readMapleAsciiString();
        LoginInformationProvider li = LoginInformationProvider.getInstance();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (!MapleCharacterUtil.canCreateChar(name, false) || (li.isForbiddenName(name) && !c.isGM())) {
            System.out.println("非法創建角色名: " + name);
            return;
        }
        int keymapType = slea.readInt(); // 按鍵模式: 0-基本模式; 1-進階模式
        slea.readInt(); // 還不知道是什麼

        int job_type = slea.readInt();
        JobType job = JobType.getByType(job_type);
        if (job == null) {
            System.out.println("發現新職業類型: " + job_type);
            return;
        }
        for (JobConstants.LoginJob j : JobConstants.LoginJob.values()) {
            if (j.getJobType() == job_type) {
                if (!j.enableCreate()) {
                    System.err.println("未開放的職業被嘗試創建");
                    return;
                }
            }
        }

        subcategory = slea.readShort();
        if (subcategory == 1) {
            if (job != JobType.影武者) {

            }
        }
        if ((subcategory == 0 && (job == JobType.影武者 || job == JobType.重砲指揮官))
                || (subcategory == 1 && job != JobType.影武者) || (subcategory == 2 && job != JobType.重砲指揮官)) {
            System.err.println("創建職業子類別異常:" + subcategory + " 職業:" + job.name()
                    + (subcategory == 0 && (job == JobType.影武者 || job == JobType.重砲指揮官))
                    + (subcategory == 1 && job != JobType.影武者) + (subcategory == 2 && job != JobType.重砲指揮官));
            return;
        }
        gender = slea.readByte();
        skin = slea.readByte();
        boolean skinOk = skin == 0;
        switch (job) {
            case 皇家騎士團:
            case 米哈逸:
                skin = 10;
                skinOk = true;
                break;
            case 伊利恩:
            case 狂狼勇士:
                skin = 11;
                skinOk = true;
                break;
            case 精靈遊俠:
                skin = 12;
                skinOk = true;
                break;
            case 惡魔:
                skinOk = skinOk || skin == 13;
                break;
        }
        if (!skinOk) {
            System.err.println("創建職業皮膚顏色錯誤, 職業:" + job.name() + " 皮膚:" + skin);
            return;
        }
        unk = slea.readByte(); // 6/7/8/9
        // 驗證創建角色的可選項是否正確
        int index = 0;
        for (JobInfoFlag jf : JobInfoFlag.values()) {
            if (jf.check(job.flag)) {
                int value = slea.readInt();
                if (!li.isEligibleItem(gender, index,
                        job == JobType.影武者 ? 1 : job == JobType.蒼龍俠客 ? MapleJob.蒼龍俠客1轉.getId() : job.id, value)) {
                    System.err.println(
                            "創建角色確認道具出錯 - 性別:" + gender + " 職業:" + job.name() + " 類型:" + jf.name() + " 值:" + value);
                    return;
                }
                if (jf == JobInfoFlag.尾巴 || jf == JobInfoFlag.耳朵) {
                    value = ItemConstants.getEffectItemID(value);
                }
                infos.put(jf, value);
                index++;
            } else {
                infos.put(jf, 0);
            }
        }

        if (slea.available() != 0) {
            System.err.println("創建角色讀取訊息出錯, 有未讀取訊息: " + HexTool.toString(slea.read((int) slea.available())));
            return;
        }

        if (job == JobType.凱內西斯) {
            infos.put(JobInfoFlag.副手, 1353200); // 西洋棋─士兵
        }
        // 讀取創建角色默認配置
        MapleCharacter newchar = MapleCharacter.getDefault(c, job);
        newchar.setWorld((byte) c.getWorld());
        newchar.setFace(infos.get(JobInfoFlag.臉型));
        newchar.setSecondFace(infos.get(JobInfoFlag.臉型));
        int hair = infos.get(JobInfoFlag.髮型);
        if (job == JobType.傑諾) {
            if (hair == 30000) { // 黑色基本冒險造型
                hair = 36137; // 棕色史烏造型
            } else if (hair == 31002) { // 橘色基本冒險造型
                hair = 37467; // 棕色殺人鯨造型
            }
        }
        newchar.setHair(hair);
        newchar.setSecondHair(infos.get(JobInfoFlag.髮型));
        if (job == JobType.天使破壞者) {
            newchar.setSecondFace(21173);
            newchar.setSecondHair(37141);
        } else if (job == JobType.神之子) {
            newchar.setSecondFace(21290);
            newchar.setSecondHair(37623);
        }
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor(skin);
        newchar.setFaceMarking(infos.get(JobInfoFlag.臉飾));
        newchar.setEars(infos.get(JobInfoFlag.耳朵));
        newchar.setTail(infos.get(JobInfoFlag.尾巴));
        final MapleInventory equip = newchar.getInventory(MapleInventoryType.EQUIPPED);
        Item item;
        // -1 Hat | -2 Face | -3 Eye acc | -4 Ear acc | -5 Topwear
        // -6 Bottom | -7 Shoes | -8 glove | -9 Cape | -10 Shield | -11 Weapon
        int[] equips = new int[]{infos.get(JobInfoFlag.帽子), infos.get(JobInfoFlag.衣服), infos.get(JobInfoFlag.褲裙),
            infos.get(JobInfoFlag.披風), infos.get(JobInfoFlag.鞋子), infos.get(JobInfoFlag.手套),
            infos.get(JobInfoFlag.武器), infos.get(JobInfoFlag.副手),};
        for (int i : equips) {
            if (i > 0) {
                short[] equipSlot = ItemConstants.getEquipedSlot(i);
                if (equipSlot == null || equipSlot.length < 1) {
                    System.err.println("創建角色新增裝備出錯, 裝備欄位未知, 道具ID" + i);
                    continue;
                }
                item = ii.getEquipById(i);
                item.setPosition(equipSlot[0]);
                item.setGMLog("創建角色獲得, 時間 " + FileoutputUtil.CurrentReadable_Time());
                equip.addFromDB(item);
            }
        }

        String info = "\r\n\r\n名字: " + name + "\r\n職業: " + job.name() + "(序號" + job_type + ")" + "\r\n子類別: "
                + subcategory + "\r\n性別: " + gender + "\r\n皮膚: " + skin + "\r\n未知值: " + unk;
        for (Map.Entry<JobInfoFlag, Integer> i : infos.entrySet()) {
            info += "\r\n" + i.getKey().name() + ": " + i.getValue();
        }
        info += "\r\n\r\n";
        FileoutputUtil.log(FileoutputUtil.Create_Character, info);

        // 修正進階按鍵不完全
        if (keymapType == 1) {
            newchar.getQuestNAdd(MapleQuest.getInstance(GameConstants.QUICK_SLOT))
                    .setCustomData("16,17,18,19,30,31,32,33,2,3,4,5,29,56,44,45,6,7,8,9,46,22,23,36,10,11,37,49");
        }

        if (MapleCharacterUtil.canCreateChar(name, c.isGM())
                && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGM())
                && (c.isGM() || c.canMakeCharacter(c.getWorld()))) {
            MapleCharacter.saveNewCharToDB(newchar, job, subcategory, keymapType);
            c.announce(LoginPacket.OnCreateNewCharacterResult(newchar, true));
            c.createdChar(newchar.getId());
            WvsCenter.getInstance().addCharTable(newchar);
            // newchar.newCharRewards();
        } else {
            c.announce(LoginPacket.OnCreateNewCharacterResult(newchar, false));
        }
    }

    public static void CreateUltimate(final LittleEndianAccessor slea, final MapleClient c) {
        if (!c.isLoggedIn() || c.getPlayer() == null || c.getPlayer().getLevel() < 120
                || c.getPlayer().getMapId() != 130000000 || c.getPlayer().getQuestStatus(20734) != 0
                || c.getPlayer().getQuestStatus(20616) != 2 || !MapleJob.is皇家騎士團(c.getPlayer().getJob())
                || !c.canMakeCharacter(c.getPlayer().getWorld())) {
            c.announce(CField.createUltimate(2));
            // Character slots are full. Please purchase another slot from the Cash Shop.
            return;
        }
        // System.out.println(slea.toString());
        final String name = slea.readMapleAsciiString();
        final int job = slea.readInt(); // job ID

        final int face = slea.readInt();
        final int hair = slea.readInt();

        // No idea what are these used for:
        final int hat = slea.readInt();
        final int top = slea.readInt();
        final int glove = slea.readInt();
        final int shoes = slea.readInt();
        final int weapon = slea.readInt();

        final byte gender = c.getPlayer().getGender();

        JobType errorCheck = JobType.冒險家;
        if (!LoginInformationProvider.getInstance().isEligibleItem(gender, 0, errorCheck.type, face)) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        JobType jobType = JobType.終極冒險家;

        MapleCharacter newchar = MapleCharacter.getDefault(c, jobType);
        newchar.setJob(job);
        newchar.setWorld(c.getPlayer().getWorld());
        newchar.setFace(face);
        newchar.setHair(hair);
        newchar.setGender(gender);
        newchar.setName(name);
        newchar.setSkinColor((byte) 3); // troll
        newchar.setLevel((short) 50);
        newchar.getStat().力量 = (short) 4;
        newchar.getStat().敏捷 = (short) 4;
        newchar.getStat().智力 = (short) 4;
        newchar.getStat().幸運 = (short) 4;
        newchar.setRemainingAp((short) 254); // 49*5 + 25 - 16
        newchar.setRemainingSp(job / 100 == 2 ? 128 : 122); // 2 from job advancements. 120 from leveling. (mages get
        // +6)
        newchar.getStat().maxhp += 150; // Beginner 10 levels
        newchar.getStat().maxmp += 125;
        switch (job) {
            case 110:
            case 120:
            case 130:
                newchar.getStat().maxhp += 600; // Job Advancement
                newchar.getStat().maxhp += 2000; // Levelup 40 times
                newchar.getStat().maxmp += 200;
                break;
            case 210:
            case 220:
            case 230:
                newchar.getStat().maxmp += 600;
                newchar.getStat().maxhp += 500; // Levelup 40 times
                newchar.getStat().maxmp += 2000;
                break;
            case 310:
            case 320:
            case 410:
            case 420:
            case 520:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 900; // Levelup 40 times
                newchar.getStat().maxmp += 600;
                break;
            case 510:
                newchar.getStat().maxhp += 500;
                newchar.getStat().maxmp += 250;
                newchar.getStat().maxhp += 450; // Levelup 20 times
                newchar.getStat().maxmp += 300;
                newchar.getStat().maxhp += 800; // Levelup 20 times
                newchar.getStat().maxmp += 400;
                break;
            default:
                return;
        }

        final Map<Skill, SkillEntry> ss = new HashMap<>();
        ss.put(SkillFactory.getSkill(1074 + (job / 100)), new SkillEntry((byte) 5, (byte) 5, -1));
        ss.put(SkillFactory.getSkill(80), new SkillEntry((byte) 1, (byte) 1, -1));
        newchar.changeSkillLevel_Skip(ss, false);
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();

        int[] items = new int[]{1142257, hat, top, shoes, glove, weapon, hat + 1, top + 1, shoes + 1, glove + 1,
            weapon + 1}; // brilliant = fine+1
        for (byte i = 0; i < items.length; i++) {
            Item item = li.getEquipById(items[i]);
            item.setPosition((byte) (i + 1));
            newchar.getInventory(MapleInventoryType.EQUIP).addFromDB(item);
        }

        newchar.getInventory(MapleInventoryType.USE).addItem(new Item(2000004, (byte) 0, (short) 200, 0));
        if (MapleCharacterUtil.canCreateChar(name, c.isGM())
                && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGM())) {
            MapleCharacter.saveNewCharToDB(newchar, jobType, (short) 0);
            MapleQuest.getInstance(20734).forceComplete(c.getPlayer(), 1101000);
            c.announce(CField.createUltimate(0));
        } else if (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGM()) {
            c.announce(CField.createUltimate(3)); // "You cannot use this name."
        } else {
            c.announce(CField.createUltimate(1));
        }
    }

    public static void DeleteChar(final LittleEndianAccessor slea, final MapleClient c) {
        final String Secondpw_Client = slea.readMapleAsciiString();
        final int Character_ID = slea.readInt();

        if (!c.login_Auth(Character_ID) || !c.isLoggedIn() || loginFailCount(c) || c.getSecondPassword() == null) {
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            return; // Attempting to delete other character
        }
        byte state = 0;

        if (!c.CheckSecondPassword(Secondpw_Client)) { // Wrong Password
            state = 20;
        }

        if (state == 0) {
            state = (byte) c.deleteCharacter(Character_ID);
        }
        WvsCenter.getInstance().removeCharTable(Character_ID);
        c.announce(LoginPacket.deleteCharResponse(Character_ID, state));
    }

    public static void CharacterSelected(final LittleEndianAccessor slea, final MapleClient c, final boolean view,
            final boolean check2PW) {
        String password = null;
        if (check2PW) {
            password = slea.readMapleAsciiString();
        }
        final int charId = slea.readInt();
        if (view) {
            c.setChannel(1);
            c.setWorld(slea.readInt());
        }
        if (!c.isLoggedIn() || loginFailCount(c) || c.getSecondPassword() == null || !c.login_Auth(charId)
                || ChannelServer.getInstance(c.getChannel()) == null || !WorldConstants.isExists(c.getWorld())) {
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            return;
        }
        if (password == null || c.CheckSecondPassword(password)) {
            if (c.getIdleTask() != null) {
                c.getIdleTask().cancel(true);
            }
            final String s = c.getSessionIPAddress();
            LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP(),
                    c.getChannel());
            c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
            c.announce(CField.getServerIP(c,
                    Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
        } else {
            // c.announce(LoginPacket.createCharResponse(20));
            c.announce(CField.getServerIP(c,
                    Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId, 0x4));
            // c.announce(LoginPacket.secondPwError((byte) 0x14));
        }
    }

    public static void partTimeJob(final LittleEndianAccessor slea, final MapleClient c) {
        System.out.println("[Part Time Job] data: " + slea);
        byte mode = slea.readByte(); // 1 = start 2 = end
        int cid = slea.readInt(); // character id
        byte job = slea.readByte(); // part time job
        if (mode == 0) {
            LoginPacket.partTimeJob(cid, (byte) 0, System.currentTimeMillis());
        } else if (mode == 1) {
            LoginPacket.partTimeJob(cid, job, System.currentTimeMillis());
        }
    }

    public static void PartJob(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer() != null || !c.isLoggedIn()) {
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            return;
        }
        final byte mode = slea.readByte();
        final int cid = slea.readInt();
        if (mode == 1) { // 開始打工
            final PartTimeJob partTime = MapleCharacter.getPartTime(cid);
            final byte job = slea.readByte();
            if (/* chr.getLevel() < 30 || */job < 0 || job > 5 || partTime.getReward() > 0
                    || (partTime.getJob() > 0 && partTime.getJob() <= 5)) {
                c.getSession().close();
                System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                return;
            }
            partTime.setTime(System.currentTimeMillis());
            partTime.setJob(job);
            c.announce(LoginPacket.updatePartTimeJob(partTime));
            MapleCharacter.removePartTime(cid);
            MapleCharacter.addPartTime(partTime);
        } else if (mode == 2) { // 結束打工
            final PartTimeJob partTime = MapleCharacter.getPartTime(cid);
            if (/* chr.getLevel() < 30 || */partTime.getReward() > 0 || partTime.getJob() < 0
                    || partTime.getJob() > 5) {
                c.getSession().close();
                System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                return;
            }
            final long distance = (System.currentTimeMillis() - partTime.getTime()) / (60 * 60 * 1000L);
            if (distance > 1) {
                partTime.setReward((int) (((partTime.getJob() + 1) * 1000L) + distance));
            } else {
                partTime.setJob((byte) 0);
                partTime.setReward(0);
            }
            partTime.setTime(System.currentTimeMillis());
            MapleCharacter.removePartTime(cid);
            MapleCharacter.addPartTime(partTime);
            c.announce(LoginPacket.updatePartTimeJob(partTime));
        }
    }

    public static void SetGender(LittleEndianAccessor slea, MapleClient c) {
        String name = slea.readMapleAsciiString();
        String secondPassword = slea.readMapleAsciiString();
        byte gender = slea.readByte();
        if (!c.getAccountName().equals(name) || c.getSecondPassword() != null || gender < 0 || gender > 1) {
            c.announce(LoginPacket.genderChanged(false));
            return;
        }
        c.clearInformation();
        if (secondPassword.length() >= 5) {

            try {
                Connection con = ManagerDatabasePool.getConnection();
                try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET gender = ?, 2ndpassword = ? WHERE name = ?")) {
                    ps.setInt(1, gender);
                    ps.setString(2, LoginCrypto.hexSha1(secondPassword));
                    ps.setString(3, name);
                    ps.execute();
                }
                ManagerDatabasePool.closeConnection(con);
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
            }
            c.announce(LoginPacket.genderChanged(true));
        } else {
            c.announce(LoginPacket.genderChanged(false));
        }
    }
}
