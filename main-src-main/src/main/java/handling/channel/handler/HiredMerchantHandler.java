/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import database.ManagerDatabasePool;
import extensions.temporary.ScriptMessageType;
import handling.world.World;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MerchItemPackage;
import server.stores.HiredMerchant;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CField.CScriptMan;
import tools.packet.CWvsContext;
import tools.packet.PlayerShopPacket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HiredMerchantHandler {

    public static final boolean UseHiredMerchant(final MapleClient c, final boolean packet) {
        if (c.getChannelServer().isShutdown()) {
            // 目前無法開設商店！
            c.getSession().writeAndFlush(PlayerShopPacket.sendTitleBox(11));
            return false;
        }
        if (c.getPlayer().getMap() != null && c.getPlayer().getMap().allowPersonalShop()) {
            HiredMerchant merchant = World.getMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
            if (merchant != null) {
                // 您已在第%s頻道自由市場%s內開設商店，請先將該商店關閉後再重新使用。
                c.getSession()
                        .write(PlayerShopPacket.sendTitleBox(8, merchant.getMapId(), merchant.getChannel() - 1, ""));
                // // 目前有其他角色正在使用中。\r\n請以該角色登入後關閉商店，或將商店倉庫清空。
                // c.getSession().writeAndFlush(PlayerShopPacket.sendTitleBox(10));
            } else {
                final byte state = checkExistance(c.getPlayer().getAccountID(), c.getPlayer().getId());

                switch (state) {
                    case 1:
                        // 請向自由市場入口處的富蘭德里領取物品後，重新再試。
                        c.getSession().write(PlayerShopPacket.sendTitleBox(9));
                        return false;
                    case 0:
                        if (packet) {
                            // 管理帳號無法進行。
                            c.getSession().writeAndFlush(PlayerShopPacket.sendTitleBox());
                        }
                        return true;
                    default:
                        c.getPlayer().dropMessage(1, "An unknown error occured.");
                        return true;
                }
                // 請透過富蘭德里領取物品。
                // c.getSession().writeAndFlush(PlayerShopPacket.sendTitleBox(15));
            }
        } else {
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
        }
        return false;
    }

    private static byte checkExistance(final int accid, final int cid) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con
                    .prepareStatement("SELECT * from hiredmerch where accountid = ? OR characterid = ?")) {
                ps.setInt(1, accid);
                ps.setInt(2, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ps.close();
                        rs.close();
                        return 1;
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException se) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, se);
            return -1;
        }
        return 0;
    }

    public static void displayMerch(MapleClient c) {
        final int conv = c.getPlayer().getConversation();
        boolean merch = World.hasMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
        if (merch) {
            c.getPlayer().dropMessage(1, "請先關閉精靈商人後再試一次.");
            c.getPlayer().setConversation(0);
        } else if (c.getChannelServer().isShutdown()) {
            c.getPlayer().dropMessage(1, "現在還不能進行.");
            c.getPlayer().setConversation(0);
        } else if (conv == 3) { // Hired Merch
            final MerchItemPackage pack = loadItemFrom_Database(c.getPlayer().getAccountID());

            if (pack == null) {
                c.getSession().writeAndFlush(CScriptMan.getNPCTalk(9030000, ScriptMessageType.SM_SAY,
                        "我這裡沒有你的任何物品或金錢，這裡只會有精靈商人的東西，不會有個人商店的東西唷", "00 00", (byte) 0));
                c.getPlayer().setConversation(0);
            } else if (pack.getItems().size() <= 0) { // error fix for complainers.
                if (!check(c.getPlayer(), pack)) {
                    c.getSession().writeAndFlush(PlayerShopPacket.merchItem_Message((byte) 0x21));
                    return;
                }
                if (deletePackage(c.getPlayer().getAccountID(), pack.getPackageid(), c.getPlayer().getId())) {
                    // c.getPlayer().fakeRelog();
                    // c.getPlayer().gainMeso(pack.getMesos(), false);
                    c.getSession().writeAndFlush(PlayerShopPacket.merchItem_Message((byte) 0x1d));
                    c.getPlayer().setConversation(0);
                } else {
                    c.getPlayer().dropMessage(1, "發生未知的錯誤，請稍後再試");
                }
                c.getPlayer().setConversation(0);
            } else {
                c.getSession().writeAndFlush(PlayerShopPacket.merchItemStore_ItemData(pack));
                MapleInventoryManipulator.checkSpace(c, conv, conv, null);
                for (final Item item : pack.getItems()) {
                    if (c.getPlayer().getInventory(GameConstants.getInventoryType(item.getItemId())).isFull()) {
                        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(9030000, ScriptMessageType.SM_SAY,
                                "請檢查你的背包欄位是否足夠後再試一次.", "00 00", (byte) 0));
                        c.getPlayer().setConversation(0);
                        break;
                    }
                    MapleInventoryManipulator.addFromDrop(c, item, true, false);
                    deletePackage(c.getPlayer().getAccountID(), pack.getPackageid(), c.getPlayer().getId());
                    // c.getPlayer().fakeRelog();
                    c.getSession().writeAndFlush(CScriptMan.getNPCTalk(9030000, ScriptMessageType.SM_SAY,
                            "你領取了保管的道具和楓幣.", "00 00", (byte) 0));
                    c.getPlayer().setConversation(0);
                }

            }
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions());
    }

    public static final void MerchantItemStore(final LittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer() == null) {
            return;
        }
        final byte operation = slea.readByte();
        if (operation == 27 || operation == 28) { // Request, Take out
            requestItems(c, operation == 27);
        } else if (operation == 30) { // Exit
            c.getPlayer().setConversation(0);
        }
    }

    private static void requestItems(final MapleClient c, final boolean request) {
        if (c.getPlayer().getConversation() != 3) {
            return;
        }
        boolean merch = World.hasMerchant(c.getPlayer().getAccountID(), c.getPlayer().getId());
        if (merch) {
            c.getPlayer().dropMessage(1, "請先關閉精靈商人後再試一次.");
            c.getPlayer().setConversation(0);
            return;
        }
        final MerchItemPackage pack = loadItemFrom_Database(c.getPlayer().getAccountID());
        if (pack == null) {
            c.getPlayer().dropMessage(1, "發生未知的錯誤，請稍後再試");
            return;
        } else if (c.getChannelServer().isShutdown()) {
            c.getPlayer().dropMessage(1, "現在還不能進行.");
            c.getPlayer().setConversation(0);
            return;
        }
        final int days = StringUtil.getDaysAmount(pack.getSavedTime(), System.currentTimeMillis()); // max 100%
        final double percentage = days / 100.0;
        final int fee = (int) Math.ceil(percentage * pack.getMesos()); // if no mesos = no tax
        if (request && days > 0 && percentage > 0 && pack.getMesos() > 0 && fee > 0) {
            c.getSession().writeAndFlush(PlayerShopPacket.merchItemStore((byte) 38, days, fee));
            return;
        }
        if (fee < 0) { // impossible
            c.getSession().writeAndFlush(PlayerShopPacket.merchItem_Message(33));
            return;
        }
        if (c.getPlayer().getMeso() < fee) {
            c.getSession().writeAndFlush(PlayerShopPacket.merchItem_Message(35));
            return;
        }
        if (!check(c.getPlayer(), pack)) {
            c.getSession().writeAndFlush(PlayerShopPacket.merchItem_Message(36));
            return;
        }
        if (deletePackage(c.getPlayer().getAccountID(), pack.getPackageid(), c.getPlayer().getId())) {
            if (fee > 0) {
                c.getPlayer().gainMeso(-fee, true);
            }
            c.getPlayer().gainMeso(pack.getMesos(), false);
            for (Item item : pack.getItems()) {
                MapleInventoryManipulator.addFromDrop(c, item);
            }
            c.getSession().writeAndFlush(PlayerShopPacket.merchItem_Message(32));
        } else {
            c.getPlayer().dropMessage(1, "發生未知的錯誤，請稍後再試");
        }
    }

    private static boolean check(final MapleCharacter chr, final MerchItemPackage pack) {
        if (chr.getMeso() + pack.getMesos() < 0) {
            return false;
        }
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        for (Item item : pack.getItems()) {
            final MapleInventoryType invtype = GameConstants.getInventoryType(item.getItemId());
            if (null != invtype) {
                switch (invtype) {
                    case EQUIP:
                        eq++;
                        break;
                    case USE:
                        use++;
                        break;
                    case SETUP:
                        setup++;
                        break;
                    case ETC:
                        etc++;
                        break;
                    case CASH:
                        cash++;
                        break;
                    default:
                        break;
                }
            }
            if (MapleItemInformationProvider.getInstance().isPickupRestricted(item.getItemId())
                    && chr.haveItem(item.getItemId(), 1)) {
                return false;
            }
        }
        return chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() >= eq
                && chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() >= use
                && chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= setup
                && chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= etc
                && chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() >= cash;
    }

    private static boolean deletePackage(final int accid, final int packageid, final int chrId) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "DELETE from hiredmerch where accountid = ? OR packageid = ? OR characterid = ?")) {
                ps.setInt(1, accid);
                ps.setInt(2, packageid);
                ps.setInt(3, chrId);
                ps.executeUpdate();
            }
            ItemLoader.HIRED_MERCHANT.saveItems(null, packageid, con);
            ManagerDatabasePool.closeConnection(con);
            return true;
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
            return false;
        }
    }

    public static final void showFredrick(MapleClient c) {
        final MerchItemPackage pack = HiredMerchantHandler.loadItemFrom_Database(c.getPlayer().getAccountID());
        c.getSession().writeAndFlush(PlayerShopPacket.merchItemStore_ItemData(pack));
    }

    private static MerchItemPackage loadItemFrom_Database(final int accountid) {

        try {
            final MerchItemPackage pack;
            Connection con = ManagerDatabasePool.getConnection();
            final int packageid;
            try (PreparedStatement ps = con.prepareStatement("SELECT * from hiredmerch where accountid = ?")) {
                ps.setInt(1, accountid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        ps.close();
                        rs.close();
                        return null;
                    }
                    packageid = rs.getInt("PackageId");
                    pack = new MerchItemPackage();
                    pack.setPackageid(packageid);
                    pack.setMesos(rs.getLong("Mesos"));
                    pack.setSavedTime(rs.getLong("time"));
                }
            }

            Map<Long, Pair<Item, MapleInventoryType>> items = ItemLoader.HIRED_MERCHANT.loadItems(false, packageid);
            if (items != null) {
                List<Item> iters = new ArrayList<>();
                items.values().forEach((z) -> {
                    iters.add(z.left);
                });
                pack.setItems(iters);
            }
            ManagerDatabasePool.closeConnection(con);
            return pack;
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, e);
            return null;
        }
    }
}
