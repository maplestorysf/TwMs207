package client.inventory;

import client.familiar.FamiliarCard;
import constants.ItemConstants;
import database.ManagerDatabasePool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import server.MapleItemInformationProvider;
import tools.FileoutputUtil;
import tools.Pair;

public enum ItemLoader {

    INVENTORY(0), STORAGE(1, true), CASHSHOP(2, true), HIRED_MERCHANT(5), PACKAGE(6), MTS(8), MTS_TRANSFER(9);

    private final int value;
    private final boolean account;

    private ItemLoader(int value) {
        this.value = value;
        this.account = false;
    }

    private ItemLoader(int value, boolean account) {
        this.value = value;
        this.account = account;
    }

    public static boolean isExistsByUniqueid(int uniqueid) {
        for (ItemLoader il : ItemLoader.values()) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM `inventoryitems` WHERE `type` = ? AND uniqueid = ?");
            try {
                Connection con = ManagerDatabasePool.getConnection();
                try (PreparedStatement ps = con.prepareStatement(query.toString())) {
                    ps.setInt(1, il.value);
                    ps.setInt(2, uniqueid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.first()) {
                            ps.close();
                            rs.close();
                            return true;
                        }
                    }
                }
                ManagerDatabasePool.closeConnection(con);
            } catch (SQLException ex) {
                Logger.getLogger(ItemLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    public int getValue() {
        return value;
    }

    // does not need connection con to be auto commit
    public Map<Long, Pair<Item, MapleInventoryType>> loadItems(boolean login, int id) throws SQLException {
        Map<Long, Pair<Item, MapleInventoryType>> items = new LinkedHashMap<>();
        StringBuilder query = new StringBuilder();
        query.append(
                "SELECT * FROM `inventoryitems` LEFT JOIN `inventoryequipment` USING (`inventoryitemid`) LEFT JOIN `familiarcard` USING(`inventoryitemid`) WHERE `type` = ? AND `");
        query.append(account ? "accountid" : "characterid");
        query.append("` = ?");

        if (login) {
            query.append(" AND `inventorytype` = ");
            query.append(MapleInventoryType.EQUIPPED.getType());
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(query.toString())) {
                ps.setInt(1, value);
                ps.setInt(2, id);
                try (ResultSet rs = ps.executeQuery()) {
                    final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    while (rs.next()) {
                        try {
                            if (!ii.itemExists(rs.getInt("itemid"))) { // EXPENSIVE
                                StringBuilder sb = new StringBuilder();
                                sb.append("讀取").append(name()).append("道具時錯誤, 道具在WZ裏不存在, 道具[")
                                        .append(rs.getInt("itemid")).append("]可能會丟失.\r\n");
                                FileoutputUtil.log(FileoutputUtil.ItemSave_Error, sb.toString());
                                continue;
                            }
                            MapleInventoryType mit = MapleInventoryType.getByType(rs.getByte("inventorytype"));

                            if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                                Equip equip = new Equip(rs.getInt("itemid"), rs.getShort("position"),
                                        rs.getInt("uniqueid"), rs.getInt("flag"));
                                // 鐵砧影像
                                equip.setFusionAnvil(rs.getInt("fusionAnvil"));
                                if (!login && equip.getPosition() != -55) { // monsterbook
                                    equip.setQuantity((short) 1);
                                    equip.setInventoryId(rs.getLong("inventoryitemid"));
                                    equip.setOwner(rs.getString("owner"));
                                    equip.setExpiration(rs.getLong("expiredate"));
                                    equip.setPlatinumHammer(rs.getByte("PlatinumHammer"));
                                    equip.setUpgradeSlots(rs.getByte("upgradeslots"));
                                    equip.setLevel(rs.getByte("level"));
                                    equip.setStr(rs.getShort("str"));
                                    equip.setDex(rs.getShort("dex"));
                                    equip.setInt(rs.getShort("int"));
                                    equip.setLuk(rs.getShort("luk"));
                                    equip.setHp(rs.getShort("hp"));
                                    equip.setMp(rs.getShort("mp"));
                                    equip.setWatk(rs.getShort("watk"));
                                    equip.setMatk(rs.getShort("matk"));
                                    equip.setWdef(rs.getShort("wdef"));
                                    equip.setMdef(rs.getShort("mdef"));
                                    equip.setAcc(rs.getShort("acc"));
                                    equip.setAvoid(rs.getShort("avoid"));
                                    equip.setHands(rs.getShort("hands"));
                                    equip.setSpeed(rs.getShort("speed"));
                                    equip.setJump(rs.getShort("jump"));
                                    equip.setViciousHammer(rs.getByte("ViciousHammer"));
                                    equip.setItemEXP(rs.getLong("itemEXP"));
                                    equip.setGMLog(rs.getString("GM_Log"));
                                    equip.setDurability(rs.getInt("durability"));
                                    equip.setEnhance(rs.getByte("enhance"));
                                    equip.setState(rs.getByte("state"), false);
                                    equip.setPotential(rs.getInt("potential1"), 1, false);
                                    equip.setPotential(rs.getInt("potential2"), 2, false);
                                    equip.setPotential(rs.getInt("potential3"), 3, false);
                                    equip.setState(rs.getByte("bonusState"), true);
                                    equip.setPotential(rs.getInt("potential4"), 1, true);
                                    equip.setPotential(rs.getInt("potential5"), 2, true);
                                    equip.setPotential(rs.getInt("potential6"), 3, true);
                                    equip.setSocket(rs.getInt("socket1"), 1);
                                    equip.setSocket(rs.getInt("socket2"), 2);
                                    equip.setSocket(rs.getInt("socket3"), 3);
                                    equip.setGiftFrom(rs.getString("sender"));
                                    equip.setIncSkill(rs.getInt("incSkill"));
                                    equip.setPVPDamage(rs.getShort("pvpDamage"));
                                    equip.setCharmEXP(rs.getShort("charmEXP"));
                                    equip.setEnhanctBuff(rs.getShort("enhanctBuff"));
                                    equip.setReqLevel(rs.getByte("reqLevel"));
                                    equip.setYggdrasilWisdom(rs.getByte("yggdrasilWisdom"));
                                    equip.setFinalStrike(rs.getByte("finalStrike") > 0);
                                    equip.setBossDamage(rs.getByte("bossDamage"));
                                    equip.setIgnorePDR(rs.getByte("ignorePDR"));
                                    equip.setTotalDamage(rs.getByte("totalDamage"));
                                    equip.setAllStat(rs.getByte("allStat"));
                                    equip.setKarmaCount(rs.getByte("karmaCount"));
                                    equip.setStarForce(rs.getByte("starforce"));
                                    equip.setSoulName(rs.getShort("soulname"));
                                    equip.setSoulEnchanter(rs.getShort("soulenchanter"));
                                    equip.setSoulPotential(rs.getShort("soulpotential"));
                                    equip.setSoulSkill(rs.getInt("soulskill"));
                                    if (equip.getCharmEXP() < 0) { // has not been initialized yet
                                        equip.setCharmEXP(((Equip) ii.getEquipById(equip.getItemId())).getCharmEXP());
                                    }
                                    if (equip.getUniqueId() > -1) {
                                        if (ItemConstants.類型.特效戒指(rs.getInt("itemid"))) {
                                            MapleRing ring = MapleRing.loadFromDb(equip.getUniqueId(),
                                                    mit.equals(MapleInventoryType.EQUIPPED));
                                            if (ring != null) {
                                                equip.setRing(ring);
                                            }
                                        } else if (equip.getItemId() / 10000 == 166) {
                                            MapleAndroid ad = MapleAndroid.loadFromDb(equip.getItemId(),
                                                    equip.getUniqueId());
                                            if (ad != null) {
                                                equip.setAndroid(ad);
                                            }
                                        }
                                    }
                                }
                                items.put(rs.getLong("inventoryitemid"), new Pair<>(equip.copy(), mit));
                            } else {
                                Item item = new Item(rs.getInt("itemid"), rs.getShort("position"),
                                        rs.getShort("quantity"), rs.getInt("flag"), rs.getInt("uniqueid"),
                                        rs.getShort("espos"));
                                item.setOwner(rs.getString("owner"));
                                item.setInventoryId(rs.getLong("inventoryitemid"));
                                item.setExpiration(rs.getLong("expiredate"));
                                item.setGMLog(rs.getString("GM_Log"));
                                item.setGiftFrom(rs.getString("sender"));
                                if (constants.ItemConstants.getFamiliarByItemID(item.getItemId()) > 0) {
                                    int skill = rs.getShort("skillid");
                                    int level = rs.getByte("level");
                                    int grade = rs.getByte("grade");
                                    item.setFamiliarCard(new FamiliarCard((short) skill, (byte) level, (byte) grade,
                                            rs.getInt("option1"), rs.getInt("option2"), rs.getInt("option3")));
                                }
                                // item.setExp(rs.getInt("exp"));
                                if (ItemConstants.類型.寵物(item.getItemId())) {
                                    if (item.getUniqueId() > -1) {
                                        MaplePet pet = MaplePet.loadFromDb(item.getItemId(), item.getUniqueId(),
                                                item.getPosition());
                                        if (pet != null) {
                                            item.setPet(pet);
                                        }
                                    } else {
                                        // O_O hackish fix
                                        item.setPet(MaplePet.createPet(item.getItemId()));
                                    }
                                }
                                items.put(rs.getLong("inventoryitemid"), new Pair<>(item.copy(), mit));
                            }
                        } catch (SQLException ex) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("讀取").append(name()).append("道具時異常, 道具[").append(rs.getInt("itemid"))
                                    .append("]可能會丟失.\r\n");
                            sb.append("錯誤訊息:").append(ex);
                            FileoutputUtil.log(FileoutputUtil.ItemSave_Error, sb.toString());
                        }
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException se) {
            FileoutputUtil.log(FileoutputUtil.ItemSave_Error, "讀取" + name() + "道具時異常: " + se);
        }
        return items;
    }

    public void saveItems(List<Pair<Item, MapleInventoryType>> items, int id, Connection con) {
        try {
            if (con == null) {
                con = ManagerDatabasePool.getConnection();
            }
            StringBuilder query = new StringBuilder();
            query.append("DELETE FROM `inventoryitems` WHERE `type` = ? AND `");
            query.append(account ? "accountid" : "characterid");
            query.append("` = ?");

            PreparedStatement ps = con.prepareStatement(query.toString());
            PreparedStatement psf = con
                    .prepareStatement("INSERT INTO `familiarcard` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, value);
            ps.setInt(2, id);
            ps.executeUpdate();
            ps.close();
            if (items == null) {
                return;
            }
            StringBuilder query_2 = new StringBuilder("INSERT INTO `inventoryitems` (");
            query_2.append(account ? "accountid" : "characterid");
            query_2.append(
                    ", itemid, inventorytype, position, quantity, owner, GM_Log, uniqueid, expiredate, flag, `type`, sender) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps = con.prepareStatement(query_2.toString(), Statement.RETURN_GENERATED_KEYS);

            String valueStr = "";
            int values = 52;
            for (int i = 0; i < values; i++) {
                if (i == (values - 1)) {
                    valueStr += "?";
                } else {
                    valueStr += "?, ";
                }
            }
            PreparedStatement pse = con
                    .prepareStatement("INSERT INTO `inventoryequipment` VALUES (DEFAULT, " + valueStr + ")");
            final Iterator<Pair<Item, MapleInventoryType>> iter = items.iterator();
            Pair<Item, MapleInventoryType> pair;
            while (iter.hasNext()) {
                pair = iter.next();
                Item item = pair.getLeft();
                if (item == null) {
                    continue;
                }
                MapleInventoryType mit = pair.getRight();
                if (item.getPosition() == -55) {
                    continue;
                }
                try {
                    int i = 1;
                    ps.setInt(i++, id);
                    ps.setInt(i++, item.getItemId());
                    ps.setInt(i++, mit.getType());
                    ps.setInt(i++, item.getPosition());
                    ps.setInt(i++, item.getQuantity());
                    ps.setString(i++, item.getOwner());
                    ps.setString(i++, item.getGMLog());
                    if (item.getPet() != null) { // expensif?
                        // item.getPet().saveToDb();
                        ps.setInt(i++, Math.max(item.getUniqueId(), item.getPet().getUniqueId()));
                    } else {
                        ps.setInt(i++, item.getUniqueId());
                    }
                    ps.setLong(i++, item.getExpiration());
                    ps.setInt(i++, item.getFlag());
                    ps.setByte(i++, (byte) value);
                    ps.setString(i++, item.getGiftFrom());

                    ps.executeUpdate();
                    final long iid;
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            rs.close();
                            continue;
                        }
                        iid = rs.getLong(1);
                    }

                    item.setInventoryId(iid);

                    if (item.getFamiliarCard() != null && ItemConstants.getFamiliarByItemID(item.getItemId()) > 0) {
                        i = 1;
                        psf.setLong(i++, iid);
                        FamiliarCard card = item.getFamiliarCard();
                        psf.setByte(i++, card.getLevel());
                        psf.setByte(i++, card.getGrade());
                        psf.setShort(i++, card.getSkill());
                        psf.setInt(i++, card.getOption1());
                        psf.setInt(i++, card.getOption2());
                        psf.setInt(i++, card.getOption3());
                        psf.executeUpdate();
                    }

                    if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                        Equip equip = (Equip) item;
                        i = 1;
                        pse.setLong(i++, iid);
                        pse.setInt(i++, equip.getPlatinumHammer());
                        pse.setInt(i++, equip.getUpgradeSlots());
                        pse.setInt(i++, equip.getLevel());
                        pse.setInt(i++, equip.getStr(0));
                        pse.setInt(i++, equip.getDex(0));
                        pse.setInt(i++, equip.getInt(0));
                        pse.setInt(i++, equip.getLuk(0));
                        pse.setInt(i++, equip.getHp(0));
                        pse.setInt(i++, equip.getMp(0));
                        pse.setInt(i++, equip.getWatk(0));
                        pse.setInt(i++, equip.getMatk(0));
                        pse.setInt(i++, equip.getWdef(0));
                        pse.setInt(i++, 0);
                        pse.setInt(i++, 0);
                        pse.setInt(i++, 0);
                        pse.setInt(i++, equip.getHands(0));
                        pse.setInt(i++, equip.getSpeed(0));
                        pse.setInt(i++, equip.getJump(0));
                        pse.setInt(i++, equip.getViciousHammer());
                        pse.setLong(i++, equip.getItemEXP());
                        pse.setInt(i++, equip.getDurability());
                        pse.setByte(i++, equip.getEnhance());
                        pse.setByte(i++, equip.getState(false));
                        pse.setInt(i++, equip.getPotential(1, false));
                        pse.setInt(i++, equip.getPotential(2, false));
                        pse.setInt(i++, equip.getPotential(3, false));
                        pse.setByte(i++, equip.getState(true));
                        pse.setInt(i++, equip.getPotential(1, true));
                        pse.setInt(i++, equip.getPotential(2, true));
                        pse.setInt(i++, equip.getPotential(3, true));
                        pse.setInt(i++, equip.getFusionAnvil());
                        pse.setInt(i++, equip.getSocket(1));
                        pse.setInt(i++, equip.getSocket(2));
                        pse.setInt(i++, equip.getSocket(3));
                        pse.setInt(i++, equip.getIncSkill());
                        pse.setShort(i++, equip.getCharmEXP());
                        pse.setShort(i++, equip.getPVPDamage());
                        pse.setShort(i++, equip.getEnhanctBuff());
                        pse.setByte(i++, equip.getReqLevel());
                        pse.setByte(i++, equip.getYggdrasilWisdom());
                        pse.setByte(i++, (byte) (equip.getFinalStrike() ? 1 : 0));
                        pse.setByte(i++, equip.getBossDamage(0));
                        pse.setByte(i++, equip.getIgnorePDR(0));
                        pse.setByte(i++, equip.getTotalDamage(0));
                        pse.setByte(i++, equip.getAllStat(0));
                        pse.setByte(i++, equip.getKarmaCount());
                        pse.setByte(i++, equip.getStarForce());
                        pse.setShort(i++, equip.getSoulName());
                        pse.setShort(i++, equip.getSoulEnchanter());
                        pse.setShort(i++, equip.getSoulPotential());
                        pse.setInt(i++, equip.getSoulSkill());
                        pse.executeUpdate();
                    }
                } catch (SQLException ex) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("儲存").append(name()).append("道具時異常, 道具[").append(item).append("]已丟失: \r\n");
                    if (mit != null) {
                        sb.append("道具訊息:\r\n");
                        sb.append("類型:").append(mit.name()).append("\r\n");
                        sb.append("Position:").append(item.getPosition()).append("\r\n");
                        sb.append("Owner:").append(item.getOwner()).append("\r\n");
                        sb.append("GMLog:").append(item.getGMLog()).append("\r\n");
                        if (item.getPet() != null) {
                            sb.append("UniqueId:").append(Math.max(item.getUniqueId(), item.getPet().getUniqueId()))
                                    .append("\r\n");
                        } else {
                            sb.append("UniqueId:").append(item.getUniqueId()).append("\r\n");
                        }
                        sb.append("Expiration:").append(item.getExpiration()).append("\r\n");
                        sb.append("Flag:").append(item.getFlag()).append("\r\n");
                        sb.append("GiftFrom:").append(item.getGiftFrom()).append("\r\n");
                        if (mit.equals(MapleInventoryType.EQUIP) || mit.equals(MapleInventoryType.EQUIPPED)) {
                            Equip equip = (Equip) item;
                            sb.append("PlatinumHammer:").append(equip.getPlatinumHammer()).append("\r\n");
                            sb.append("UpgradeSlots:").append(equip.getUpgradeSlots()).append("\r\n");
                            sb.append("Level:").append(equip.getLevel()).append("\r\n");
                            sb.append("Str:").append(equip.getStr(0)).append("\r\n");
                            sb.append("Str:").append(equip.getStr(0xFFFF)).append("(").append(equip.getStr(0))
                                    .append("+").append(equip.getStr(0xFFFF) - equip.getStr(0)).append(")\r\n");
                            sb.append("Dex:").append(equip.getDex(0xFFFF)).append("(").append(equip.getDex(0))
                                    .append("+").append(equip.getDex(0xFFFF) - equip.getDex(0)).append(")\r\n");
                            sb.append("Int:").append(equip.getInt(0xFFFF)).append("(").append(equip.getInt(0))
                                    .append("+").append(equip.getInt(0xFFFF) - equip.getInt(0)).append(")\r\n");
                            sb.append("Luk:").append(equip.getLuk(0xFFFF)).append("(").append(equip.getLuk(0))
                                    .append("+").append(equip.getLuk(0xFFFF) - equip.getLuk(0)).append(")\r\n");
                            sb.append("Hp:").append(equip.getHp(0xFFFF)).append("(").append(equip.getHp(0)).append("+")
                                    .append(equip.getHp(0xFFFF) - equip.getHp(0)).append(")\r\n");
                            sb.append("Mp:").append(equip.getMp(0xFFFF)).append("(").append(equip.getMp(0)).append("+")
                                    .append(equip.getMp(0xFFFF) - equip.getMp(0)).append(")\r\n");
                            sb.append("Watk:").append(equip.getWatk(0xFFFF)).append("(").append(equip.getWatk(0))
                                    .append("+").append(equip.getWatk(0xFFFF) - equip.getWatk(0)).append(")\r\n");
                            sb.append("Matk:").append(equip.getMatk(0xFFFF)).append("(").append(equip.getMatk(0))
                                    .append("+").append(equip.getMatk(0xFFFF) - equip.getMatk(0)).append(")\r\n");
                            sb.append("Wdef:").append(equip.getWdef(0xFFFF)).append("(").append(equip.getWdef(0))
                                    .append("+").append(equip.getWdef(0xFFFF) - equip.getWdef(0)).append(")\r\n");
                            sb.append("Hands:").append(equip.getHands(0xFFFF)).append("(").append(equip.getHands(0))
                                    .append("+").append(equip.getHands(0xFFFF) - equip.getHands(0)).append(")\r\n");
                            sb.append("Speed:").append(equip.getSpeed(0xFFFF)).append("(").append(equip.getSpeed(0))
                                    .append("+").append(equip.getSpeed(0xFFFF) - equip.getSpeed(0)).append(")\r\n");
                            sb.append("Jump:").append(equip.getJump(0xFFFF)).append("(").append(equip.getJump(0))
                                    .append("+").append(equip.getJump(0xFFFF) - equip.getJump(0)).append(")\r\n");
                            sb.append("ViciousHammer:").append(equip.getViciousHammer()).append("\r\n");
                            sb.append("ItemEXP:").append(equip.getItemEXP()).append("\r\n");
                            sb.append("Durability:").append(equip.getDurability()).append("\r\n");
                            sb.append("Enhance:").append(equip.getEnhance()).append("\r\n");
                            sb.append("潛能等級:").append(equip.getState(false)).append("\r\n");
                            sb.append("潛能1:").append(equip.getPotential(1, false)).append("\r\n");
                            sb.append("潛能2:").append(equip.getPotential(2, false)).append("\r\n");
                            sb.append("潛能3:").append(equip.getPotential(3, false)).append("\r\n");
                            sb.append("附加潛能等級:").append(equip.getState(true)).append("\r\n");
                            sb.append("附加潛能1:").append(equip.getPotential(1, true)).append("\r\n");
                            sb.append("附加潛能2:").append(equip.getPotential(2, true)).append("\r\n");
                            sb.append("附加潛能3:").append(equip.getPotential(3, true)).append("\r\n");
                            sb.append("FusionAnvil:").append(equip.getFusionAnvil()).append("\r\n");
                            sb.append("Socket1:").append(equip.getSocket(1)).append("\r\n");
                            sb.append("Socket2:").append(equip.getSocket(2)).append("\r\n");
                            sb.append("Socket3:").append(equip.getSocket(3)).append("\r\n");
                            sb.append("IncSkill:").append(equip.getIncSkill()).append("\r\n");
                            sb.append("CharmEXP:").append(equip.getCharmEXP()).append("\r\n");
                            sb.append("PVPDamage:").append(equip.getPVPDamage()).append("\r\n");
                            sb.append("EnhanctBuff:").append(equip.getEnhanctBuff()).append("\r\n");
                            sb.append("ReqLevel:").append(equip.getReqLevel()).append("\r\n");
                            sb.append("YggdrasilWisdom:").append(equip.getYggdrasilWisdom()).append("\r\n");
                            sb.append("FinalStrike:").append(equip.getFinalStrike()).append("\r\n");
                            sb.append("BossDamage:").append(equip.getBossDamage(0xFFFF)).append("(")
                                    .append(equip.getBossDamage(0)).append("+")
                                    .append(equip.getBossDamage(0xFFFF) - equip.getBossDamage(0)).append(")\r\n");
                            sb.append("IgnorePDR:").append(equip.getIgnorePDR(0xFFFF)).append("(")
                                    .append(equip.getIgnorePDR(0)).append("+")
                                    .append(equip.getIgnorePDR(0xFFFF) - equip.getIgnorePDR(0)).append(")\r\n");
                            sb.append("TotalDamage:").append(equip.getTotalDamage(0xFFFF)).append("(")
                                    .append(equip.getTotalDamage(0)).append("+")
                                    .append(equip.getTotalDamage(0xFFFF) - equip.getTotalDamage(0)).append(")\r\n");
                            sb.append("AllStat:").append(equip.getAllStat(0xFFFF)).append("(")
                                    .append(equip.getAllStat(0)).append("+")
                                    .append(equip.getAllStat(0xFFFF) - equip.getAllStat(0)).append(")\r\n");
                            sb.append("KarmaCount:").append(equip.getKarmaCount()).append("\r\n");
                            sb.append("StarForce:").append(equip.getStarForce()).append("\r\n");
                            sb.append("SoulName:").append(equip.getSoulName()).append("\r\n");
                            sb.append("SoulEnchanter:").append(equip.getSoulEnchanter()).append("\r\n");
                            sb.append("SoulPotential:").append(equip.getSoulPotential()).append("\r\n");
                            sb.append("SoulSkill:").append(equip.getSoulSkill()).append("\r\n");
                        }
                    }
                    sb.append("錯誤訊息:").append(ex);
                    FileoutputUtil.log(FileoutputUtil.ItemSave_Error, sb.toString());
                }
            }
            pse.close();
            ps.close();
        } catch (SQLException ex) {
            FileoutputUtil.log(FileoutputUtil.ItemSave_Error, "儲存" + name() + "道具時異常: " + ex);
        }
    }
}
