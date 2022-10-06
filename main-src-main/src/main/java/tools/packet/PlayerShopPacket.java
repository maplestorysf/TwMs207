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
package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import handling.InteractionOpcode;
import handling.SendPacketOpcode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import server.MapleItemInformationProvider;
import server.MerchItemPackage;
import server.stores.AbstractPlayerStore;
import server.stores.AbstractPlayerStore.BoughtItem;
import server.stores.HiredMerchant;
import server.stores.IMaplePlayerShop;
import server.stores.MapleMiniGame;
import server.stores.MaplePlayerShop;
import server.stores.MaplePlayerShopItem;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

public class PlayerShopPacket {

    public static byte[] sendAdminShop(final int npcid, boolean notshop) {
        List<Pair<Integer, Integer>> ShopList = new ArrayList<>();
        if (notshop) {
            ShopList.add(new Pair<>(2000032, 1));
        }

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AdminShopCommodity.getValue());
        mplew.writeInt(npcid);
        mplew.writeShort(ShopList.size());
        if (ShopList.size() > 0) {
            for (Pair<Integer, Integer> ItemD : ShopList) {
                mplew.writeInt(0);
                mplew.writeInt(ItemD.left); // 物品ID
                mplew.writeInt(ItemD.right); // 價格
                mplew.write(0);
                mplew.writeShort(0);
            }
            mplew.write(0);
        }

        return mplew.getPacket();
    }

    public static byte[] sendTitleBox() {
        return sendTitleBox(7); // SendOpenShopRequest
    }

    public static byte[] sendTitleBox(int mode) {
        return sendTitleBox(mode, 0, 0, "");
    }

    public static byte[] sendTitleBox(int mode, int val1, int val2, String str) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(8);

        mplew.writeShort(SendPacketOpcode.LP_EntrustedShopCheckResult.getValue());
        mplew.write(mode);
        if (mode == 8 || mode == 16) {
            mplew.writeInt(val1);
            mplew.write(val2);
        } else if (mode == 13) {
            mplew.writeInt(val1);
        } else if (mode == 14) {
            mplew.write(val1);
        } else if (mode == 18) {
            mplew.write(1);
            mplew.writeMapleAsciiString(str);
        }

        return mplew.getPacket();
    }

    public static byte[] requestShopPic(final int oid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);

        mplew.writeShort(SendPacketOpcode.LP_EntrustedShopCheckResult.getValue());
        mplew.write(17);
        mplew.writeInt(oid);
        mplew.writeShort(0);
        mplew.writeLong(0L);

        return mplew.getPacket();
    }

    public static final byte[] addCharBox(final MapleCharacter c, final int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserMiniRoomBalloon.getValue());
        mplew.writeInt(c.getId());
        PacketHelper.addAnnounceBox(mplew, c);

        return mplew.getPacket();
    }

    public static final byte[] removeCharBox(final MapleCharacter c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserMiniRoomBalloon.getValue());
        mplew.writeInt(c.getId());
        mplew.write(0);

        return mplew.getPacket();
    }

    public static final byte[] sendPlayerShopBox(final MapleCharacter c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserMiniRoomBalloon.getValue());
        mplew.writeInt(c.getId());
        PacketHelper.addAnnounceBox(mplew, c);

        return mplew.getPacket();
    }

    public static byte[] getHiredMerch(MapleCharacter chr, HiredMerchant merch, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.房間.getValue());
        mplew.write(6);
        mplew.write(merch.getMaxSize());
        mplew.writeShort(merch.getVisitorSlot(chr));
        mplew.writeInt(merch.getItemId());
        mplew.writeMapleAsciiString(MapleItemInformationProvider.getInstance().getName(merch.getItemId()));
        for (Pair<Byte, MapleCharacter> storechr : merch.getVisitors()) {
            mplew.write(storechr.left);
            PacketHelper.AvatarLook__Decode(mplew, storechr.right, false, false);
            mplew.writeMapleAsciiString(storechr.right.getName());
            mplew.writeShort(storechr.right.getJob());
        }
        mplew.write(-1);
        mplew.writeShort(merch.isOwner(chr) ? merch.getMessages().size() : 0);
        if (merch.isOwner(chr)) {
            for (Pair<String, Byte> msg : merch.getMessages()) {
                mplew.writeMapleAsciiString(msg.getLeft());
                mplew.write(msg.getRight());
            }
        }
        mplew.writeMapleAsciiString(merch.getOwnerName());
        if (merch.isOwner(chr)) {
            mplew.writeInt(merch.getTimeLeft());
            mplew.write(firstTime ? 1 : 0);
            mplew.write(merch.getBoughtItems().size());
            for (final BoughtItem SoldItem : merch.getBoughtItems()) {
                mplew.writeInt(SoldItem.id);
                mplew.writeShort(SoldItem.quantity);
                mplew.writeLong(SoldItem.totalPrice);
                mplew.writeMapleAsciiString(SoldItem.buyer);
            }
            mplew.writeLong(merch.getMeso());
        }
        mplew.writeInt(merch.getObjectId());
        mplew.writeMapleAsciiString(merch.getDescription());
        mplew.write(16);
        mplew.writeLong(merch.getMeso());
        mplew.write(merch.getItems().size());
        for (MaplePlayerShopItem item : merch.getItems()) {
            mplew.writeShort(item.bundles);
            mplew.writeShort(item.item.getQuantity());
            mplew.writeLong(item.price);
            PacketHelper.GW_ItemSlotBase_Decode(mplew, item.item);
        }
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static final byte[] getPlayerStore(final MapleCharacter chr, final boolean firstTime) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        IMaplePlayerShop ips = chr.getPlayerShop();
        mplew.write(11);
        switch (ips.getShopType()) {
        case 2:
            mplew.write(4);
            mplew.write(4);
            break;
        case 3:
            mplew.write(2);
            mplew.write(2);
            break;
        case 4:
            mplew.write(1);
            mplew.write(2);
            break;
        }
        mplew.writeShort(ips.getVisitorSlot(chr));
        PacketHelper.AvatarLook__Decode(mplew, ((MaplePlayerShop) ips).getMCOwner(), false, false);
        mplew.writeMapleAsciiString(ips.getOwnerName());
        mplew.writeShort(((MaplePlayerShop) ips).getMCOwner().getJob());
        for (final Pair<Byte, MapleCharacter> storechr : ips.getVisitors()) {
            mplew.write(storechr.left);
            PacketHelper.AvatarLook__Decode(mplew, storechr.right, false, false);
            mplew.writeMapleAsciiString(storechr.right.getName());
            mplew.writeShort(storechr.right.getJob());
        }
        mplew.write(255);
        mplew.writeMapleAsciiString(ips.getDescription());
        mplew.write(10);
        mplew.write(ips.getItems().size());

        for (final MaplePlayerShopItem item : ips.getItems()) {
            mplew.writeShort(item.bundles);
            mplew.writeShort(item.item.getQuantity());
            mplew.writeInt(item.price);
            PacketHelper.GW_ItemSlotBase_Decode(mplew, item.item);
        }
        return mplew.getPacket();
    }

    public static final byte[] shopChat(final String message, final int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.聊天.getValue());
        mplew.write(InteractionOpcode.聊天事件.getValue());
        mplew.write(slot);
        mplew.writeMapleAsciiString(message);

        return mplew.getPacket();
    }

    public static byte[] shopErrorMessage(final int error, final int type) {
        return shopErrorMessage(false, error, type);
    }

    public static final byte[] shopErrorMessage(boolean room, final int error, final int type) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(room ? InteractionOpcode.房間.getValue() : InteractionOpcode.退出.getValue());
        mplew.write(type);
        mplew.write(error);

        return mplew.getPacket();
    }

    public static final byte[] spawnHiredMerchant(final HiredMerchant hm) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_EmployeeEnterField.getValue());
        mplew.writeInt(hm.getOwnerId());
        mplew.writeInt(hm.getItemId());
        mplew.writePos(hm.getTruePosition());
        mplew.writeShort(hm.getFh());
        mplew.writeMapleAsciiString(hm.getOwnerName());
        PacketHelper.addInteraction(mplew, hm);

        return mplew.getPacket();
    }

    public static final byte[] destroyHiredMerchant(final int id) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_EmployeeLeaveField.getValue());
        mplew.writeInt(id);

        return mplew.getPacket();
    }

    public static final byte[] shopItemUpdate(final IMaplePlayerShop shop) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.精靈商人_更新訊息.getValue());// was50
        if (shop.getShopType() == 1) {
            mplew.writeLong(shop.getMeso());
        }
        mplew.write(shop.getItems().size());
        for (final MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.bundles);
            mplew.writeShort(item.item.getQuantity());
            mplew.writeLong(item.price);
            PacketHelper.GW_ItemSlotBase_Decode(mplew, item.item);
        }
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static final byte[] shopVisitorAdd(final MapleCharacter chr, final int slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.訪問.getValue());
        mplew.write(slot);
        PacketHelper.AvatarLook__Decode(mplew, chr, false, false);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeShort(chr.getJob());

        return mplew.getPacket();
    }

    public static final byte[] shopVisitorLeave(final byte slot) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.退出.getValue());
        mplew.write(slot);

        return mplew.getPacket();
    }

    public static final byte[] Merchant_Buy_Error(final byte message) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 2 = You have not enough meso
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.精靈商人_錯誤提示.getValue());
        mplew.write(message);

        return mplew.getPacket();
    }

    public static final byte[] updateHiredMerchant(final HiredMerchant shop) {
        return updateHiredMerchant(shop, true);
    }

    public static final byte[] updateHiredMerchant(final HiredMerchant shop, final boolean update) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(update ? SendPacketOpcode.UPDATE_HIRED_MERCHANT.getValue()
                : SendPacketOpcode.CHANGE_HIRED_MERCHANT_NAME.getValue());
        mplew.writeInt(shop.getOwnerId());
        PacketHelper.addInteraction(mplew, shop);

        return mplew.getPacket();
    }

    public static final byte[] hiredMerchantOwnerLeave() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.精靈商人_關閉完成.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    public static final byte[] merchItem_Message(final int op) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_StoreBankGetAllResult.getValue());
        mplew.write(op);

        return mplew.getPacket();
    }

    public static final byte[] merchItemStore(final byte op, final int days, final int fees) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 40: This is currently unavailable.\r\nPlease try again later
        mplew.writeShort(SendPacketOpcode.LP_StoreBankResult.getValue());
        mplew.write(op);
        switch (op) {
        case 39:
            mplew.writeInt(999999999); // ?
            mplew.writeInt(999999999); // mapid
            mplew.write(0); // >= -2 channel
            // if cc -1 or map = 999,999,999 : I don't think you have any items or money to
            // retrieve here. This is where you retrieve the items and mesos that you
            // couldn't get from your Hired Merchant. You'll also need to see me as the
            // character that opened the Personal Store.
            // Your Personal Store is open #bin Channel %s, Free Market %d#k.\r\nIf you need
            // me, then please close your personal store first before seeing me.
            break;
        case 38:
            mplew.writeInt(days); // % tax or days, 1 day = 1%
            mplew.writeInt(fees); // feees
            break;
        }

        return mplew.getPacket();
    }

    public static final byte[] merchItemStore_ItemData(final MerchItemPackage pack) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_StoreBankResult.getValue());
        mplew.write(38);
        mplew.writeInt(9030000); // Fredrick
        mplew.write(16); // max items?
        mplew.writeLong(126L); // ?
        mplew.writeLong(pack.getMesos());
        mplew.write(0);
        mplew.write(pack.getItems().size());
        for (final Item item : pack.getItems()) {
            PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
        }
        mplew.writeZeroBytes(3);

        return mplew.getPacket();
    }

    public static byte[] getMiniGame(MapleClient c, MapleMiniGame minigame) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(10);
        mplew.write(minigame.getGameType());
        mplew.write(minigame.getMaxSize());
        mplew.writeShort(minigame.getVisitorSlot(c.getPlayer()));
        PacketHelper.AvatarLook__Decode(mplew, minigame.getMCOwner(), false, false);
        mplew.writeMapleAsciiString(minigame.getOwnerName());
        mplew.writeShort(minigame.getMCOwner().getJob());
        for (Pair<Byte, MapleCharacter> visitorz : minigame.getVisitors()) {
            mplew.write(visitorz.getLeft());
            PacketHelper.AvatarLook__Decode(mplew, visitorz.getRight(), false, false);
            mplew.writeMapleAsciiString(visitorz.getRight().getName());
            mplew.writeShort(visitorz.getRight().getJob());
        }
        mplew.write(-1);
        mplew.write(0);
        addGameInfo(mplew, minigame.getMCOwner(), minigame);
        for (Pair<Byte, MapleCharacter> visitorz : minigame.getVisitors()) {
            mplew.write(visitorz.getLeft());
            addGameInfo(mplew, visitorz.getRight(), minigame);
        }
        mplew.write(-1);
        mplew.writeMapleAsciiString(minigame.getDescription());
        mplew.writeShort(minigame.getPieceType());
        return mplew.getPacket();
    }

    public static byte[] getMiniGameReady(boolean ready) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(ready ? 56 : 60);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameExitAfter(boolean ready) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(ready ? 54 : 58);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameStart(int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(62);
        mplew.write(loser == 1 ? 0 : 1);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameSkip(int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(64);

        mplew.write(slot);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameRequestTie() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(51);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameDenyTie() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(50);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.writeShort(10);
        mplew.write(2);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameMoveOmok(int move1, int move2, int move3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(65);
        mplew.writeInt(move1);
        mplew.writeInt(move2);
        mplew.write(move3);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameNewVisitor(MapleCharacter c, int slot, MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(9);
        mplew.write(slot);
        PacketHelper.AvatarLook__Decode(mplew, c, false, false);
        mplew.writeMapleAsciiString(c.getName());
        mplew.writeShort(c.getJob());
        addGameInfo(mplew, c, game);
        return mplew.getPacket();
    }

    public static void addGameInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, MapleMiniGame game) {
        mplew.writeInt(game.getGameType());
        mplew.writeInt(game.getWins(chr));
        mplew.writeInt(game.getTies(chr));
        mplew.writeInt(game.getLosses(chr));
        mplew.writeInt(game.getScore(chr));
    }

    public static byte[] getMiniGameClose(byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(18);
        mplew.write(1);
        mplew.write(number);
        return mplew.getPacket();
    }

    public static byte[] getMatchCardStart(MapleMiniGame game, int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(62);
        mplew.write(loser == 1 ? 0 : 1);
        int times = game.getPieceType() == 2 ? 30 : game.getPieceType() == 1 ? 20 : 12;
        mplew.write(times);
        for (int i = 1; i <= times; i++) {
            mplew.writeInt(game.getCardId(i));
        }
        return mplew.getPacket();
    }

    public static byte[] getMatchCardSelect(int turn, int slot, int firstslot, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(69);
        mplew.write(turn);
        mplew.write(slot);
        if (turn == 0) {
            mplew.write(firstslot);
            mplew.write(type);
        }
        return mplew.getPacket();
    }

    public static byte[] getMiniGameResult(MapleMiniGame game, int type, int x) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(63);
        mplew.write(type);
        game.setPoints(x, type);
        if (type != 0) {
            game.setPoints(x == 1 ? 0 : 1, type == 2 ? 0 : 1);
        }
        if (type != 1) {
            if (type == 0) {
                mplew.write(x == 1 ? 0 : 1);
            } else {
                mplew.write(x);
            }
        }
        addGameInfo(mplew, game.getMCOwner(), game);
        for (Pair<Byte, MapleCharacter> visitorz : game.getVisitors()) {
            addGameInfo(mplew, visitorz.right, game);
        }

        return mplew.getPacket();

    }

    public static final byte[] MerchantVisitorView(Map<String, AbstractPlayerStore.VisitorInfo> visitor) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.精靈商人_查看訪問者.getValue());
        mplew.writeShort(visitor.size());
        for (Map.Entry<String, AbstractPlayerStore.VisitorInfo> ret : visitor.entrySet()) {
            mplew.writeMapleAsciiString(ret.getKey());
            mplew.writeInt(ret.getValue().getInTime());
        }

        return mplew.getPacket();
    }

    public static final byte[] MerchantBlackListView(final List<String> blackList) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
        mplew.write(InteractionOpcode.精靈商人_鎖定清單.getValue());
        mplew.writeShort(blackList.size());
        for (String visit : blackList) {
            mplew.writeMapleAsciiString(visit);
        }
        return mplew.getPacket();
    }
}
