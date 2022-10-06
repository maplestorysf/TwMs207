package handling.cashshop.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleQuestStatus;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import constants.GameConstants;
import constants.ItemConstants;
import database.ManagerDatabasePool;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import server.CashItemFactory;
import server.CashItem;
import server.CashModItem;
import server.CashShop;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CCashShop;
import tools.packet.CWvsContext;

public class CashShopOperation {

    public static void LeaveCS(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || c == null) {
            return;
        }
        World.ChannelChange_Data(c, chr);
        ChannelServer toch = ChannelServer.getInstance(c.getChannel());
        if (toch == null) {
            FileoutputUtil.log("日誌/離開商城.txt", new StringBuilder().append("玩家: ").append(chr.getName())
                    .append(" 從商城離開發生錯誤.找不到頻道[").append(c.getChannel()).append("]的訊息.").toString());
            c.getSession().close();
            return;
        }
        CashShopServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        String s = c.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        c.getSession().writeAndFlush(CField.getChannelChange(Integer.parseInt(toch.getIP().split(":")[1])));
        chr.saveToDB(false, true);
        c.setPlayer(null);
        c.setReceiving(false);
    }

    public static void EnterCS(final MapleCharacter chr, final MapleClient c) {
        if (chr == null || !c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            return;
        }

        final int state = c.getLoginState();
        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
            World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        CashShopServer.getPlayerStorage().registerPlayer(chr);
        c.getSession().writeAndFlush(CWvsContext.getRoyaCouponInfo());
        c.getSession().writeAndFlush(CCashShop.setCashShop(c));
        c.getSession().writeAndFlush(CCashShop.CCashShop());
        c.getSession().writeAndFlush(CCashShop.disableCS());
        // 0x159
        c.getSession().writeAndFlush(CCashShop.CashUse(10500002, 0x32, 0xD2, 20130320, 20130326));
        c.getSession().writeAndFlush(CCashShop.CashUse2(0x3));
        c.getSession().writeAndFlush(CCashShop.getCSInventory(c));
        c.getSession().writeAndFlush(CCashShop.CashUse3());
        c.getSession().writeAndFlush(CCashShop.doCSMagic());
        c.getSession().writeAndFlush(CCashShop.getCSGifts(c));
        c.getSession().writeAndFlush(CCashShop.showCSAccount(c));
        c.getSession().writeAndFlush(CCashShop.sendWishList(c.getPlayer(), false));
        c.getSession().writeAndFlush(CCashShop.CashUse4());
        c.getSession().writeAndFlush(CCashShop.showNXMapleTokens(c.getPlayer()));
    }

    public static void CSUpdate(final MapleClient c) {
        doCSPackets(c);
    }

    private static boolean CouponCodeAttempt(final MapleClient c) {
        c.couponAttempt++;
        return c.couponAttempt > 5;
    }

    public static void CouponCode(final String code, final MapleClient c) {
        if (code.length() <= 0) {
            return;
        }
        Triple<Boolean, Integer, Integer> info = null;
        try {
            info = MapleCharacterUtil.getNXCodeInfo(code);
        } catch (SQLException e) {
        }
        if (info != null && info.left) {
            if (!CouponCodeAttempt(c)) {
                int type = info.mid, item = info.right;
                try {
                    MapleCharacterUtil.setNXCodeUsed(c.getPlayer().getName(), code);
                } catch (SQLException e) {
                }
                /*
                 * Explanation of type! Basically, this makes coupon codes do different things!
                 *
                 * Type 1: 樂豆點數 Type 2: 楓葉點數 Type 3: 普通物品(SN) Type 4: 楓幣
                 */
                Map<Integer, Item> itemz = new HashMap<>();
                int maplePoints = 0, mesos = 0;
                switch (type) {
                    case 1:
                    case 2:
                        c.getPlayer().modifyCSPoints(type, item, false);
                        maplePoints = item;
                        break;
                    case 3:
                        CashModItem itez = CashItemFactory.getInstance().getItem(item);
                        if (itez == null) {
                            c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                            return;
                        }
                        byte slot = MapleInventoryManipulator.addId(c, itez.getItemId(), (short) 1, "",
                                "[商城]從兌換券兌換, 時間:" + FileoutputUtil.CurrentReadable_Date());
                        if (slot < 0) {
                            c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                            return;
                        } else {
                            itemz.put(item, c.getPlayer().getInventory(GameConstants.getInventoryType(item)).getItem(slot));
                        }
                        break;
                    case 4:
                        c.getPlayer().gainMeso(item, false);
                        mesos = item;
                        break;
                }
                c.getSession().writeAndFlush(CCashShop.showCouponRedeemedItem(itemz, mesos, maplePoints, c));
                doCSPackets(c);
            }
        } else if (CouponCodeAttempt(c) == true) {
            c.getSession().writeAndFlush(CCashShop.sendCSFail(48)); // A1, 9F
        } else {
            c.getSession().writeAndFlush(CCashShop.sendCSFail(info == null ? 14 : 17)); // A1, 9F
        }
    }

    public static void BuyCashItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int action = slea.readByte();
        if (chr.isShowInfo()) {
            chr.dropMessage(-1, "[商城購買操作]操作碼::" + action);
        }

        CashItemFactory cif = CashItemFactory.getInstance();
        switch (action) {
            case 0: // 兌換券
                slea.skip(2);
                CouponCode(slea.readMapleAsciiString(), c);
                break;
            case 2: {
                slea.skip(1);
                int type = slea.readInt();
                int sn = slea.readInt();
                final CashModItem item = cif.getItem(sn);
                final int toCharge = slea.readInt();
                if (item == null) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                    break;
                }
                chr.modifyCSPoints(type, -toCharge, true);
                Item itemz = chr.getCashInventory().toItem(item);
                if (itemz != null) {
                    chr.getCashInventory().addToInventory(itemz);
                    c.getSession().writeAndFlush(CCashShop.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                } else {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                }
                break;
            }
            case 3: { // 購買道具
                final int toCharge = slea.readByte() + 1;
                slea.skip(1);
                slea.skip(1);
                int snCS = slea.readInt();
                final CashModItem item = cif.getItem(snCS);
                if (chr.isShowInfo()) {
                    chr.dropMessage(-1, "商城 => 購買 - 道具 " + snCS + " 是否為空 " + (item == null));
                }
                if (item != null && chr.getCSPoints(toCharge) >= item.getPrice() && toCharge != 3) {
                    if (!item.genderEquals(c.getPlayer().getGender())/* && c.getPlayer().getAndroid() == null */) {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA7));
                        doCSPackets(c);
                        return;
                    } else if (item.getItemId() == 5211046 || item.getItemId() == 5211047 || item.getItemId() == 5211048
                            || item.getItemId() == 5050100/* AP點數初始化券 */ /* || item.getId() == 5051001技能點數初始化卷軸 */) {
                        c.getSession().writeAndFlush(CWvsContext.broadcastMsg(1, "目前無法購買本道具。"));
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        doCSPackets(c);
                        return;
                    } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB2));
                        doCSPackets(c);
                        return;
                    }
                    for (int id : GameConstants.cashBlock) {
                        if (item.getItemId() == id) {
                            c.getSession().writeAndFlush(CWvsContext.broadcastMsg(1, "目前無法購買本道具。"));
                            c.getSession().writeAndFlush(CWvsContext.enableActions());
                            doCSPackets(c);
                            return;
                        }
                    }
                    chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                    Item itemz = chr.getCashInventory().toItem(item);
                    if (itemz != null && itemz.getUniqueId() > 0 && itemz.getItemId() == item.getItemId()
                            && itemz.getQuantity() == item.getCount()) {
                        chr.getCashInventory().addToInventory(itemz);
                        // c.getSession().writeAndFlush(CSPacket.confirmToCSInventory(itemz,
                        // c.getAccID(), item.getSN()));
                        c.getSession().writeAndFlush(CCashShop.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                    } else {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                    }
                } else {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                }
                break;
            }
            case 5: // 購物車
                chr.clearWishlist();
                if (slea.available() < 48) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                    doCSPackets(c);
                    return;
                }
                int[] wishlist = new int[12];
                for (int i = 0; i < 12; i++) {
                    wishlist[i] = slea.readInt();
                }
                chr.setWishlist(wishlist);
                c.getSession().writeAndFlush(CCashShop.sendWishList(chr, true));
                break;
            case 6: { // 擴充道具欄位
                final int toCharge = slea.readByte() + 1;
                final boolean coupon = slea.readByte() > 0;
                if (coupon) {
                    final MapleInventoryType type = getInventoryType(slea.readInt());
                    if ((type == MapleInventoryType.SETUP ? chr.getCSPoints(toCharge) >= 150
                            : chr.getCSPoints(toCharge) >= 180) && chr.getInventory(type).getSlotLimit() < 89) {
                        chr.modifyCSPoints(toCharge, type == MapleInventoryType.SETUP ? -150 : -180, false);
                        chr.getInventory(type).addSlot((short) 8);
                        chr.dropMessage(1, "道具欄位擴充至 " + chr.getInventory(type).getSlotLimit() + " 格。");
                    } else {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA4));
                    }
                } else {
                    final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                    if (chr.getCSPoints(toCharge) >= 100 && chr.getInventory(type).getSlotLimit() < 93) {
                        chr.modifyCSPoints(toCharge, -100, false);
                        chr.getInventory(type).addSlot((short) 4);
                        chr.dropMessage(1, "道具欄位擴充至 " + chr.getInventory(type).getSlotLimit() + " 格。");
                    } else {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA4));
                    }
                }
                break;
            }
            case 7: { // 擴充倉庫欄位
                final int toCharge = slea.readByte() + 1;
                final int coupon = slea.readByte() > 0 ? 2 : 1;
                if ((coupon == 1 ? chr.getCSPoints(toCharge) >= 100 : chr.getCSPoints(toCharge) >= 180)
                        && chr.getStorage().getSlots() < (49 - (4 * coupon))) {
                    chr.modifyCSPoints(toCharge, coupon == 1 ? -100 : -180, false);
                    chr.getStorage().increaseSlots((byte) (4 * coupon));
                    try {
                        Connection con = ManagerDatabasePool.getConnection();
                        chr.getStorage().saveToDB(con);
                        ManagerDatabasePool.closeConnection(con);
                    } catch (SQLException ex) {
                        System.err.println("Error saving storage" + ex);
                        FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
                    }
                    chr.dropMessage(1, "倉庫欄位擴充至 " + chr.getStorage().getSlots() + " 格。");
                } else {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA4));
                }
                break;
            }
            case 8: { // 擴充角色欄位
                final int toCharge = slea.readByte() + 1;
                CashModItem item = cif.getItem(slea.readInt());
                int slots = c.getCharacterSlots();
                if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice()
                        || slots >= GameConstants.CHARSLOT_MAX || item.getItemId() != 5430000) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                    doCSPackets(c);
                    return;
                }
                if (c.gainCharacterSlot()) {
                    c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                    chr.dropMessage(1, "角色欄位擴充至 " + (slots + 1) + " 格。");
                } else {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                }
                break;
            }
            case 10: { // 擴充墜飾欄位
                final int toCharge = slea.readByte() + 1;
                final int sn = slea.readInt();
                CashModItem item = cif.getItem(sn);
                if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice()
                        || item.getItemId() / 10000 != 555) {
                    chr.dropMessage(1, "墜飾欄位擴充失敗，點數餘額不足或出現其他錯誤。");
                    c.getSession().writeAndFlush(CCashShop.showNXMapleTokens(chr));
                    doCSPackets(c);
                    return;
                }
                MapleQuestStatus marr = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.墜飾欄));
                if (marr != null && marr.getCustomData() != null && (Long.parseLong(marr.getCustomData()) == 0
                        || Long.parseLong(marr.getCustomData()) >= System.currentTimeMillis())) {
                    chr.dropMessage(1, "墜飾欄位擴充失敗，您已經進行過墜飾欄位擴充。");
                    c.getSession().writeAndFlush(CCashShop.showNXMapleTokens(chr));
                } else {
                    long days = 0;
                    if (item.getItemId() == 5550000) {
                        days = 30;
                    } else if (item.getItemId() == 5550001) {
                        days = 7;
                    }
                    String customData = String.valueOf(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L);
                    c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.墜飾欄)).setCustomData(customData);
                    c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                    chr.dropMessage(1, "墜飾欄位擴充成功。");
                }
                break;
            }
            case 14: { // 購物商城→道具欄位
                Item item = c.getPlayer().getCashInventory().findByCashId((int) slea.readLong());
                if (item != null && item.getQuantity() > 0
                        && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                    Item item_ = item.copy();
                    short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                    if (pos >= 0) {
                        if (item_.getPet() != null) {
                            item_.getPet().setInventoryPosition(pos);
                            c.getPlayer().addPet(item_.getPet());
                        }
                        c.getPlayer().getCashInventory().removeFromInventory(item);
                        c.getSession().writeAndFlush(CCashShop.confirmFromCSInventory(item_, pos));
                    } else {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB1));
                    }
                } else {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB1));
                }
                break;
            }
            case 15: { // 道具欄位→購物商城
                Item item1;
                int sn;
                CashShop cs = chr.getCashInventory();
                int cashId = (int) slea.readLong();
                byte type = slea.readByte();
                MapleInventory mi = chr.getInventory(MapleInventoryType.getByType(type));
                item1 = mi.findByUniqueId(cashId);
                if (item1 == null) {
                    c.getSession().writeAndFlush(CCashShop.showNXMapleTokens(chr));
                    return;
                }
                sn = cif.getItemSN(item1.getItemId());
                if (cs.getItemsSize() < 100 && (cif.getSimpleItem(sn) != null || cif.getOriginSimpleItem(sn) == null)) {
                    cs.addToInventory(item1);
                    mi.removeSlot(item1.getPosition());
                    c.getSession().writeAndFlush(CCashShop.confirmToCSInventory(item1, c.getAccID(), sn));
                } else {
                    chr.dropMessage(1, "商城欄位已滿或這件道具無法存入商城。");
                }
                break;
            }
            case 31: // 賣回
            case 57: { // 丟棄物品
                Item real = null;
                if (action == 31) {
                    slea.skip(2);// 00 00
                }
                String secondPassword = slea.readMapleAsciiString().toLowerCase();
                if (action == 57) {
                    slea.skip(1);
                }
                long uniqueid = slea.readLong();
                for (int i = 0; i < chr.getCashInventory().getInventory().size(); i++) {
                    Item temp = chr.getCashInventory().getInventory().get(i);
                    if (temp.getUniqueId() == uniqueid) {
                        real = temp.copy();
                        break;
                    }
                }
                if (real == null || !c.CheckSecondPassword(secondPassword)) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                    doCSPackets(c);
                    return;
                }
                if (action == 31) {
                    CashItem citem = null;
                    int sn = 0;
                    int prize = 0;
                    if (ItemConstants.類型.裝備(real.getItemId())) {
                        sn = cif.getItemSN(real.getItemId());
                        citem = cif.getSimpleItem(sn);
                        if (citem == null) {
                            citem = cif.getOriginSimpleItem(sn);
                        }
                        if (citem != null) {
                            prize = (int) Math.round(citem.getPrice() * 0.3);
                        }
                    }
                    if (citem == null) {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                        doCSPackets(c);
                        return;
                    }
                    if (prize >= 0) {
                        chr.modifyCSPoints(2, prize, true);
                    }
                }
                c.getPlayer().getCashInventory().removeFromInventory(real);
                break;
            }
            case 34:
            case 40: { // 好友戒指
                slea.readMapleAsciiString();
                final int toCharge = slea.readByte() + 1;
                final CashModItem item = cif.getItem(slea.readInt());
                slea.readInt();
                final String partnerName = slea.readMapleAsciiString();
                final String msg = slea.readMapleAsciiString();
                if (item == null || !ItemConstants.類型.特效戒指(item.getItemId())
                        || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || msg.getBytes().length > 73
                        || msg.getBytes().length < 1) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                    doCSPackets(c);
                    return;
                } else if (!item.genderEquals(c.getPlayer().getGender())) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA6));
                    doCSPackets(c);
                    return;
                } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB1));
                    doCSPackets(c);
                    return;
                }
                Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName,
                        c.getPlayer().getWorld());
                if (info == null || info.getLeft() <= 0 || info.getLeft() == c.getPlayer().getId()) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB4));
                    doCSPackets(c);
                    return;
                } else if (info.getMid() == c.getAccID()) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA3));
                    doCSPackets(c);
                    return;
                } else {
                    if (info.getRight() == c.getPlayer().getGender() && action == 30) {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA1));
                        doCSPackets(c);
                        return;
                    }
                    int err = MapleRing.createRing(item.getItemId(), c.getPlayer(), partnerName, msg,
                            info.getLeft().intValue(), item.getSN());
                    if (err != 1) {
                        c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                        doCSPackets(c);
                        return;
                    }
                    c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                }
                break;
            }
            case 35: { // 購買套組
                final int toCharge = slea.readByte() + 1;
                final CashModItem item = cif.getItem(slea.readInt());
                List<Integer> ccc = null;
                if (item != null) {
                    ccc = cif.getPackageItems(item.getItemId());
                }
                if (item == null || ccc == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice()) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                    doCSPackets(c);
                    return;
                } else if (!item.genderEquals(c.getPlayer().getGender())) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA6));
                    doCSPackets(c);
                    return;
                } else if (c.getPlayer().getCashInventory().getItemsSize() >= (100 - ccc.size())) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB1));
                    doCSPackets(c);
                    return;
                }
                Map<Integer, Item> ccz = new HashMap<>();
                for (int i : ccc) {
                    final CashItem cii = cif.getOriginSimpleItem(i);
                    if (cii == null) {
                        continue;
                    }
                    Item itemz = c.getPlayer().getCashInventory().toItem(cii);
                    if (itemz == null || itemz.getUniqueId() <= 0) {
                        continue;
                    }
                    for (int iz : GameConstants.cashBlock) {
                        if (itemz.getItemId() == iz) {
                        }
                    }
                    ccz.put(i, itemz);
                    c.getPlayer().getCashInventory().addToInventory(itemz);
                }
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                c.getSession().writeAndFlush(CCashShop.showBoughtCSPackage(ccz, c.getAccID()));
                break;
            }
            case 37:
            case 99: { // 購買任務道具
                final CashModItem item = cif.getItem(slea.readInt());
                if (item == null || !MapleItemInformationProvider.getInstance().isQuestItem(item.getItemId())) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                    doCSPackets(c);
                    return;
                } else if (c.getPlayer().getMeso() < item.getPrice()) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB8));
                    doCSPackets(c);
                    return;
                } else if (c.getPlayer().getInventory(GameConstants.getInventoryType(item.getItemId()))
                        .getNextFreeSlot() < 0) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB1));
                    doCSPackets(c);
                    return;
                }
                byte pos = MapleInventoryManipulator.addId(c, item.getItemId(), (short) item.getCount(), null,
                        "Cash shop: quest item" + " on " + FileoutputUtil.CurrentReadable_Date());
                if (pos < 0) {
                    c.getSession().writeAndFlush(CCashShop.sendCSFail(0xB1));
                    doCSPackets(c);
                    return;
                }
                chr.gainMeso(-item.getPrice(), false);
                c.getSession().writeAndFlush(
                        CCashShop.showBoughtCSQuestItem(item.getPrice(), (short) item.getCount(), pos, item.getItemId()));
                break;
            }
            default:
                if (chr.isShowErr()) {
                    chr.dropMessage(-1, "[商城購買操作]未知操作碼::" + action);
                }
                c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
                break;
        }
        doCSPackets(c);
    }

    public static void sendCSgift(final LittleEndianAccessor slea, final MapleClient c) {
        String secondPassword = slea.readMapleAsciiString();
        final CashModItem item = CashItemFactory.getInstance().getItem(slea.readInt());
        String partnerName = slea.readMapleAsciiString();
        String msg = slea.readMapleAsciiString();
        // if (!secondPassword.equals(c.getSecondPassword())) {
        // c.getPlayer().dropMessage(1, "第二組密碼錯誤，請重新輸入。");
        // doCSPackets(c);
        // return;
        // }
        if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice() || msg.getBytes().length > 73
                || msg.getBytes().length < 1) { // dont want packet editors gifting random stuff =P
            c.getSession().writeAndFlush(CCashShop.sendCSFail(0));
            doCSPackets(c);
            return;
        }
        Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName,
                c.getPlayer().getWorld());
        if (info == null || info.getLeft() <= 0 || info.getLeft() == c.getPlayer().getId()
                || info.getMid() == c.getAccID()) {
            c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA2));
            doCSPackets(c);
            return;
        } else if (!item.genderEquals(info.getRight())) {
            c.getSession().writeAndFlush(CCashShop.sendCSFail(0xA3));
            doCSPackets(c);
            return;
        } else {
            c.getPlayer().getCashInventory().gift(info.getLeft(), c.getPlayer().getName(), msg, item.getSN(),
                    MapleInventoryIdentifier.getInstance());
            c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
            c.getSession().writeAndFlush(
                    CCashShop.sendGift(item.getPrice(), item.getItemId(), item.getCount(), partnerName, true));
        }
        doCSPackets(c);
    }

    public static void SwitchCategory(final LittleEndianAccessor slea, final MapleClient c) {
        int Scategory = slea.readByte();
        // System.out.println("Scategory " + Scategory);
        switch (Scategory) {
            case 103:
                slea.skip(1);
                int itemSn = slea.readInt();
                try {
                    Connection con = ManagerDatabasePool.getConnection();
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO `wishlist` VALUES (?, ?)")) {
                        ps.setInt(1, c.getPlayer().getId());
                        ps.setInt(2, itemSn);
                        ps.executeUpdate();
                    }
                    ManagerDatabasePool.closeConnection(con);
                } catch (SQLException ex) {
                    System.out.println("error");
                    FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
                }
                c.getSession().writeAndFlush(CCashShop.addFavorite(itemSn));
                break;
            case 105:
                int item = slea.readInt();
                try {
                    Connection con = ManagerDatabasePool.getConnection();
                    try (PreparedStatement ps = con
                            .prepareStatement("UPDATE cashshop_items SET likes = likes+" + 1 + " WHERE sn = ?")) {
                        ps.setInt(1, item);
                        ps.executeUpdate();
                    }
                    ManagerDatabasePool.closeConnection(con);
                } catch (SQLException ex) {
                    FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
                }
                c.getSession().writeAndFlush(CCashShop.Like(item));
                break;
            // c.getSession().writeAndFlush(CSPacket.Favorite(c.getPlayer()));
            case 109:
                break;
            // click on special item
            // int C8 - C9 - CA
            case 112:
                break;
            // buy from cart inventory
            // byte buy = 1 or gift = 0
            // byte amount
            // for each SN
            case 113:
                break;
            default:
                int category = slea.readInt();
                if (category == 1060100) {
                    c.getSession().writeAndFlush(CCashShop.showNXChar(category));
                    // c.getSession().writeAndFlush(CSPacket.changeCategory(category));
                } else {
                    // System.err.println(category);
                    // c.getSession().writeAndFlush(CSPacket.changeCategory(category));
                }
                break;
        }
    }

    private static MapleInventoryType getInventoryType(final int id) {
        switch (id) {
            case 140500002:
                return MapleInventoryType.EQUIP;
            case 140500003:
                return MapleInventoryType.USE;
            case 140500005:
                return MapleInventoryType.SETUP;
            case 140500004:
                return MapleInventoryType.ETC;
            default:
                return MapleInventoryType.UNDEFINED;
        }
    }

    public static void doCSPackets(MapleClient c) {
        // c.getSession().writeAndFlush(CSPacket.getCSInventory(c));
        // c.getSession().writeAndFlush(CSPacket.doCSMagic());
        // c.getSession().writeAndFlush(CSPacket.getCSGifts(c));
        c.getSession().writeAndFlush(CCashShop.showNXMapleTokens(c.getPlayer()));
        // c.getSession().writeAndFlush(CSPacket.sendWishList(c.getPlayer(), false));
        // c.getSession().writeAndFlush(CSPacket.showNXMapleTokens(c.getPlayer()));
        c.getSession().writeAndFlush(CCashShop.getCSInventory(c));
        c.getSession().writeAndFlush(CCashShop.disableCS());
        // c.getSession().writeAndFlush(CSPacket.enableCSUse());
        // c.getPlayer().getCashInventory().checkExpire(c);
    }
}
