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

import java.util.Arrays;

import client.inventory.Item;
import client.inventory.ItemFlag;
import client.MapleClient;
import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import constants.FeaturesConfig;
import constants.ItemConstants;
import handling.InteractionOpcode;
import handling.world.World;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleTrade;
import server.commands.CommandProcessor;
import server.commands.CommandType;
import server.maps.FieldLimitType;
import server.stores.HiredMerchant;
import server.stores.IMaplePlayerShop;
import server.stores.MaplePlayerShop;
import server.stores.MaplePlayerShopItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.stores.MapleMiniGame;
import tools.Pair;
import tools.packet.PlayerShopPacket;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;

public class PlayerInteractionHandler {

    public static final void PlayerInteraction(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) throws Exception {
        byte mode = slea.readByte();
        InteractionOpcode.精靈商人_求購道具.setValue((short) 39);
        final InteractionOpcode action = InteractionOpcode.getByAction(mode);
        if (chr == null || action == null) {
            if (chr != null && chr.isShowErr()) {
                chr.showInfo("玩家互動", true, "未知的操作類型[" + mode + "]:" + slea.toString());
            }
            return;
        }
        c.getPlayer().setScrolledPosition((short) 0);
        if (chr.isShowInfo()) {
            chr.showInfo("玩家互動", false, "操作類型:" + action.name() + "(" + action.getValue() + ")");
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        switch (action) {
            case 設置物品:
            case 設置物品_2:
            case 設置物品_3:
            case 設置物品_4: {
                final MapleInventoryType ivType = MapleInventoryType.getByType(slea.readByte());
                final Item item = chr.getInventory(ivType).getItem((byte) slea.readShort());
                final short quantity = slea.readShort();
                final byte targetSlot = slea.readByte();

                if (chr.getTrade() == null || item == null) {
                    break;
                }
                if ((quantity <= item.getQuantity() && quantity >= 0) || ItemConstants.類型.可充值道具(item.getItemId())) {
                    chr.getTrade().setItems(c, item, targetSlot, quantity);
                }
                break;
            }
            case 設定楓幣:
            case 設定楓幣_2:
            case 設定楓幣_3:
            case 設定楓幣_4: {
                final MapleTrade trade = chr.getTrade();
                if (trade != null) {
                    trade.setMeso(slea.readLong());
                }
                break;
            }
            case 確認交易:
            case 確認交易_2:
            case 確認交易楓幣:
            case 確認交易楓幣_2: {
                if (chr.getTrade() == null) {
                    break;
                }
                MapleTrade.completeTrade(chr);
                break;
            }
            case 創建: {
                if (chr.getPlayerShop() != null || c.getChannelServer().isShutdown() || chr.hasBlockedInventory()) {
                    chr.dropMessage(1, "現在還不能進行.");
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                // 1 - 五子棋
                // 2 - 找碴
                // 3 - ??
                // 4 - 玩家交易
                // 5 - 營業執照
                // 6 - 精靈商人
                final byte createType = slea.readByte();
                if (createType == 4) {
                    MapleTrade.startTrade(chr);
                } else if (createType == 1 || createType == 2/* || createType == 3 */ || createType == 5
                        || createType == 6) {
                    if (!chr.getMap()
                            .getMapObjectsInRange(chr.getTruePosition(), 20000,
                                    Arrays.asList(MapleMapObjectType.SHOP, MapleMapObjectType.HIRED_MERCHANT))
                            .isEmpty() || !chr.getMap().getPortalsInRange(chr.getTruePosition(), 20000).isEmpty()) {
                        chr.dropMessage(1, "無法在這個地方使用.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    } else if (createType == 1 || createType == 2) {
                        if (FieldLimitType.Minigames.check(chr.getMap().getFieldLimit())
                                || chr.getMap().allowPersonalShop()) {
                            chr.dropMessage(1, "無法在這個地方使用.");
                            c.getSession().writeAndFlush(CWvsContext.enableActions());
                            return;
                        }
                    }

                    final String desc = slea.readMapleAsciiString();
                    String pass = "";
                    if (slea.readByte() > 0) {
                        pass = slea.readMapleAsciiString();
                    }
                    if (createType == 1 || createType == 2) {
                        final int piece = slea.readByte();
                        final int itemId = createType == 1 ? (4080000 + piece) : 4080100;
                        if (!chr.haveItem(itemId)
                                || (c.getPlayer().getMapId() >= 910000001 && c.getPlayer().getMapId() <= 910000022)) {
                            return;
                        }
                        MapleMiniGame game = new MapleMiniGame(chr, itemId, desc, pass, createType); // itemid
                        game.setPieceType(piece);
                        chr.setPlayerShop(game);
                        game.setAvailable(true);
                        game.setOpen(true);
                        game.send(c);
                        chr.getMap().addMapObject(game);
                        game.update();
                    } else if (chr.getMap().allowPersonalShop()) {
                        Item shop = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) slea.readShort());
                        if (shop == null || shop.getQuantity() <= 0 || shop.getItemId() != slea.readInt()
                                || c.getPlayer().getMapId() < 910000001 || c.getPlayer().getMapId() > 910000022) {
                            return;
                        }
                        if (createType == 5) {
                            if (shop.getItemId() / 10000 != 514) {
                                return;
                            }
                            chr.dropMessage(1, "伺服器暫時不支援此功能.");
                            // if (chr.getPlayerShop() != null) {
                            // return;
                            // }
                            // MaplePlayerShop mps = new MaplePlayerShop(chr, shop.getItemId(), desc);
                            // chr.setPlayerShop(mps);
                            // chr.getMap().addMapObject(mps);
                            // c.getSession().writeAndFlush(PlayerShopPacket.getPlayerStore(chr, true));
                        } else if (HiredMerchantHandler.UseHiredMerchant(chr.getClient(), false)) {
                            if (shop.getItemId() / 10000 != 503) {
                                return;
                            }
                            final HiredMerchant merch = new HiredMerchant(chr, shop.getItemId(), desc);
                            chr.setPlayerShop(merch);
                            chr.getMap().addMapObject(merch);
                            c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merch, true));
                            chr.dropMessage(-2, "系統提示 : 使用指令 '/幫助' 可以看到交易可用的指令說明.");
                        }
                    }
                } else {
                    chr.dropMessage(1, "暫時不支持這個操作,請把錯誤反饋給管理員Error Code - " + createType);
                }
                break;
            }
            case 訪問: {
                if (c.getChannelServer().isShutdown()) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                if (chr.getTrade() != null && chr.getTrade().getPartner() != null && !chr.getTrade().inTrade()) {
                    MapleTrade.visitTrade(chr, chr.getTrade().getPartner().getChr());
                } else if (chr.getMap() != null && chr.getTrade() == null) {
                    final int obid = slea.readInt();
                    MapleMapObject ob = chr.getMap().getMapObject(obid, MapleMapObjectType.HIRED_MERCHANT);
                    if (ob == null) {
                        ob = chr.getMap().getMapObject(obid, MapleMapObjectType.SHOP);
                    }

                    if (ob instanceof IMaplePlayerShop && chr.getPlayerShop() == null) {
                        final IMaplePlayerShop ips = (IMaplePlayerShop) ob;

                        if (ob instanceof HiredMerchant) {
                            final HiredMerchant merchant = (HiredMerchant) ips;
                            if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
                                merchant.setOpen(false);
                                merchant.removeAllVisitors(18, 1);
                                chr.setPlayerShop(ips);
                                c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                                chr.dropMessage(-2, "系統提示 : 使用指令 '/幫助' 可以看到交易可用的指令說明.");
                            } else if (!merchant.isOpen() || !merchant.isAvailable()) {
                                chr.dropMessage(1, "主人正在整理商店物品\r\n請稍後再度光臨!");
                            } else if (ips.getFreeSlot() == -1) {
                                chr.dropMessage(1, "店鋪已達到最大人數\r\n請稍後再度光臨!");
                            } else if (merchant.isInBlackList(chr.getName())) {
                                chr.dropMessage(1, "你被禁止進入該店鋪.");
                            } else {
                                chr.setPlayerShop(ips);
                                merchant.addVisitor(chr);
                                c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                            }
                        } else if (ips instanceof MaplePlayerShop && ((MaplePlayerShop) ips).isBanned(chr.getName())) {
                            chr.dropMessage(1, "你被禁止進入該店鋪.");
                        } else {
                            if (ips.getFreeSlot() < 0 || ips.getVisitorSlot(chr) > -1 || !ips.isOpen()
                                    || !ips.isAvailable()) {
                                c.getSession().writeAndFlush(PlayerShopPacket.getMiniGameFull());
                            } else {
                                if (slea.available() > 0 && slea.readByte() > 0) { // a password has been entered
                                    String pass = slea.readMapleAsciiString();
                                    if (!pass.equals(ips.getPassword())) {
                                        c.getPlayer().dropMessage(1, "你輸入的密碼不正確.");
                                        return;
                                    }
                                } else if (ips.getPassword().length() > 0) {
                                    c.getPlayer().dropMessage(1, "你輸入的密碼不正確.");
                                    return;
                                }
                                chr.setPlayerShop(ips);
                                ips.addVisitor(chr);
                                if (ips instanceof MapleMiniGame) {
                                    ((MapleMiniGame) ips).send(c);
                                } else {
                                    c.getSession().writeAndFlush(PlayerShopPacket.getPlayerStore(chr, false));
                                }
                            }
                        }
                    }
                }
                break;
            }
            case 交易邀請: {
                if (chr.getMap() == null) {
                    return;
                }
                MapleCharacter chrr = chr.getMap().getCharacterById(slea.readInt());
                if (chrr == null || c.getChannelServer().isShutdown() || chrr.hasBlockedInventory()) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                MapleTrade.inviteTrade(chr, chrr);
                break;
            }
            case 拒絕邀請: {
                MapleTrade.declineTrade(chr);
                break;
            }
            case 聊天: {
                chr.updateTick(slea.readInt());
                final String message = slea.readMapleAsciiString();
                if (chr.getTrade() != null) {
                    chr.getTrade().chat(message);
                } else if (chr.getPlayerShop() != null) {
                    final IMaplePlayerShop ips = chr.getPlayerShop();
                    if (ips.isOwner(chr)) {
                        if (CommandProcessor.processCommand(chr.getClient(), message, CommandType.TRADE)) {
                            break;
                        }
                    }
                    ips.broadcastToVisitors(
                            PlayerShopPacket.shopChat(chr.getName() + " : " + message, ips.getVisitorSlot(chr)));
                    if (ips.getShopType() == 1) {
                        ips.getMessages().add(new Pair<>(chr.getName() + " : " + message, ips.getVisitorSlot(chr)));
                    }
                    if (chr.getClient().isMonitored()) {
                        World.Broadcast.broadcastGMMessage(CWvsContext.broadcastMsg(6,
                                chr.getName() + " said in " + ips.getOwnerName() + " shop : " + message));
                    }
                }
                break;
            }
            // case 精靈商人_維護: {
            // if (c.getChannelServer().isShutdown() || chr.getMap() == null ||
            // chr.getTrade() != null) {
            // c.getSession().writeAndFlush(CWvsContext.enableActions());
            // return;
            // }
            // slea.skip(1);
            // byte type = slea.readByte();
            // slea.skip(3);
            // if (type != 6) {
            // c.getSession().writeAndFlush(CWvsContext.enableActions());
            // return;
            // }
            // final String password = slea.readMapleAsciiString();
            // if (!c.CheckSecondPassword(password)) {
            // chr.dropMessage(5, "第2組密碼錯誤。");
            // c.getSession().writeAndFlush(CWvsContext.enableActions());
            // return;
            // }
            // final int obid = slea.readInt();
            // MapleMapObject ob = chr.getMap().getMapObject(obid,
            // MapleMapObjectType.HIRED_MERCHANT);
            // if (ob == null || chr.getPlayerShop() != null) {
            // c.getSession().writeAndFlush(CWvsContext.enableActions());
            // return;
            // }
            // if (ob instanceof IMaplePlayerShop && ob instanceof HiredMerchant) {
            // final IMaplePlayerShop ips = (IMaplePlayerShop) ob;
            // final HiredMerchant merchant = (HiredMerchant) ips;
            // if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
            // merchant.setOpen(false);
            // merchant.removeAllVisitors(18, 1);
            // chr.setPlayerShop(ips);
            // c.getSession().writeAndFlush(PlayerShopPacket.getHiredMerch(chr, merchant,
            // false));
            // } else if (!merchant.isOpen() || !merchant.isAvailable()) {
            // chr.dropMessage(1, "主人正在整理商店物品\r\n請稍後再度光臨!");
            // } else if (ips.getFreeSlot() == -1) {
            // chr.dropMessage(1, "店鋪已達到最大人數\r\n請稍後再度光臨!");
            // } else if (merchant.isInBlackList(chr.getName())) {
            // chr.dropMessage(1, "你被禁止進入該店鋪.");
            // } else {
            // c.getSession().writeAndFlush(CWvsContext.enableActions());
            // }
            // }
            // break;
            // }
            case 開啟商店:
            case 開啟商店_密碼: {
                final IMaplePlayerShop shop = chr.getPlayerShop();
                if (action == InteractionOpcode.開啟商店_密碼) {
                    slea.skip(1);
                    byte type = slea.readByte();
                    if (type != 6) {
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    }
                    final String password = slea.readMapleAsciiString();
                    if (!c.CheckSecondPassword(password)) {
                        chr.dropMessage(5, "第2組密碼錯誤。");
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    }
                } else if (action == InteractionOpcode.開啟商店) {
                    if (shop != null) {
                        for (MaplePlayerShopItem item : shop.getItems()) {
                            if (ii.isCash(item.item.getItemId())) {
                                return;
                            }
                        }
                    }
                }
                if (shop != null && shop.isOwner(chr) && shop.getShopType() <= IMaplePlayerShop.PLAYER_SHOP
                        && !shop.isAvailable()) {
                    if (chr.getMap().allowPersonalShop()) {
                        if (c.getChannelServer().isShutdown()) {
                            chr.dropMessage(1, "現在還不能進行.");
                            c.getSession().writeAndFlush(CWvsContext.enableActions());
                            shop.closeShop(shop.getShopType() == IMaplePlayerShop.HIRED_MERCHANT, false);
                            return;
                        }

                        if (shop.getItems().size() < 1) {
                            c.getSession().writeAndFlush(CWvsContext.enableActions());
                            return;
                        }

                        if (shop.getShopType() == IMaplePlayerShop.HIRED_MERCHANT
                                && HiredMerchantHandler.UseHiredMerchant(chr.getClient(), false)) {
                            final HiredMerchant merchant = (HiredMerchant) shop;
                            merchant.setStoreid(c.getChannelServer().addMerchant(merchant));
                            merchant.setOpen(true);
                            merchant.setAvailable(true);
                            chr.getMap().broadcastMessage(PlayerShopPacket.spawnHiredMerchant(merchant));
                            chr.setPlayerShop(null);
                            if (action == InteractionOpcode.開啟商店_密碼) {
                                c.getSession().write(PlayerShopPacket.hiredMerchantOwnerLeave());
                            }
                        } else if (shop.getShopType() == IMaplePlayerShop.PLAYER_SHOP) {
                            shop.setOpen(true);
                            shop.setAvailable(true);
                            shop.update();
                        }
                    } else {
                        c.getSession().close();
                        System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                    }
                }

                break;
            }
            case 退出: {
                if (chr.getTrade() != null) {
                    MapleTrade.cancelTrade(chr.getTrade(), chr.getClient(), chr);
                } else {
                    final IMaplePlayerShop ips = chr.getPlayerShop();
                    if (ips == null) {
                        return;
                    }
                    if (ips.isOwner(chr) && ips.getShopType() != 1) {
                        ips.closeShop(false, ips.isAvailable());
                    } else {
                        ips.removeVisitor(chr);
                        if (ips.isOwner(chr) && !ips.isOpen() && ips.isAvailable()) {
                            ips.setOpen(true);
                        }
                    }
                    chr.setPlayerShop(null);
                }
                break;
            }
            case 添加道具:
            case 添加道具_2:
            case 添加道具_3:
            case 添加道具_4: {
                final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                final byte slot = (byte) slea.readShort();
                final short bundles = slea.readShort();
                final short perBundle = slea.readShort();
                final int price = slea.readInt();

                if (price <= 0 || bundles <= 0 || perBundle <= 0) {
                    return;
                }
                final IMaplePlayerShop shop = chr.getPlayerShop();

                if (shop == null || !shop.isOwner(chr) || shop instanceof MapleMiniGame) {
                    return;
                }
                final Item ivItem = chr.getInventory(type).getItem(slot);
                if (ivItem == null) {
                    break;
                }
                long check = bundles * perBundle;
                if (check > 32767 || check <= 0) { // This is the better way to check.
                    return;
                }
                final short bundles_perbundle = (short) (bundles * perBundle);
                if (ivItem.getQuantity() >= bundles_perbundle) {
                    final int flag = ivItem.getFlag();
                    if (ItemFlag.LOCK.check(flag) || ii.isAccountShared(ivItem.getItemId())
                            || ii.isShareTagEnabled(ivItem.getItemId()) || ii.isSharableOnce(ivItem.getItemId())) {
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    }
                    if ((ii.isCash(ivItem.getItemId()) || ii.isDropRestricted(ivItem.getItemId())
                            || ItemFlag.UNTRADABLE.check(flag)) && !ItemFlag.KARMA.check(flag)) {
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    }
                    if (ItemConstants.類型.可充值道具(ivItem.getItemId())) {
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, ivItem.getQuantity(), true);

                        final Item sellItem = ivItem.copy();
                        shop.addItem(new MaplePlayerShopItem(sellItem, (short) 1, price));
                    } else {
                        MapleInventoryManipulator.removeFromSlot(c, type, slot, bundles_perbundle, true);

                        final Item sellItem = ivItem.copy();
                        sellItem.setQuantity(perBundle);
                        shop.addItem(new MaplePlayerShopItem(sellItem, bundles, price));
                    }
                    c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(shop));
                }
                break;
            }
            case 精靈商人_購買道具:
            case 精靈商人_購買道具2:
            case 精靈商人_購買道具3:
            case 精靈商人_購買道具4: {
                final int item = slea.readByte();
                final short quantity = slea.readShort();

                final IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop == null || shop.isOwner(chr) || shop instanceof MapleMiniGame || item >= shop.getItems().size()) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                final MaplePlayerShopItem tobuy = shop.getItems().get(item);
                if (tobuy == null) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                long check = tobuy.bundles * quantity;
                long check2 = tobuy.price * quantity;
                long check3 = tobuy.item.getQuantity() * quantity;
                if (check <= 0 || check2 > 9999999999L || check2 <= 0 || check3 > 32767 || check3 < 0) { // This is the
                    // better way to
                    // check.
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                if (chr.getMeso() - check2 < 0) {
                    c.getSession().write(PlayerShopPacket.Merchant_Buy_Error((byte) 2));
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                if (tobuy.bundles < quantity
                        || (tobuy.bundles % quantity != 0 && ItemConstants.類型.裝備(tobuy.item.getItemId()))
                        || chr.getMeso() - check2 > FeaturesConfig.MESO_MAX || shop.getMeso() + check2 < 0
                        || shop.getMeso() + check2 > FeaturesConfig.MESO_MAX) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                if (quantity >= 50 && tobuy.item.getItemId() == 2340000) {
                    c.setMonitored(true);
                }
                shop.buy(c, item, quantity);
                shop.broadcastToVisitors(PlayerShopPacket.shopItemUpdate(shop));
                break;
            }
            case 精靈商人_求購道具: {
                chr.dropMessage(1, "伺服器暫時不支援此功能.");
                c.getSession().writeAndFlush(CWvsContext.enableActions());
                break;
            }
            case 移除道具: {
                slea.skip(1);
                int slot = slea.readShort();
                final IMaplePlayerShop shop = chr.getPlayerShop();
                if (chr.isShowInfo() && shop != null) {
                    chr.showInfo("移除商店道具", false, "道具數量 " + shop.getItems().size() + " slot " + slot);
                }
                if (shop == null || !shop.isOwner(chr) || shop instanceof MapleMiniGame || shop.getItems().size() <= 0
                        || shop.getItems().size() <= slot || slot < 0) {
                    return;
                }
                final MaplePlayerShopItem item = shop.getItems().get(slot);

                if (item != null) {
                    if (item.bundles > 0) {
                        Item item_get = item.item.copy();
                        long check = item.bundles * item.item.getQuantity();
                        if (check < 0 || check > 32767) {
                            if (chr.isShowErr()) {
                                chr.showInfo("移除商店道具", true, "移除出錯: check " + check);
                            }
                            return;
                        }
                        item_get.setQuantity((short) check);
                        if (item_get.getQuantity() >= 50 && item.item.getItemId() == 2340000) {
                            c.setMonitored(true);
                        }
                        if (MapleInventoryManipulator.checkSpace(c, item_get.getItemId(), item_get.getQuantity(),
                                item_get.getOwner())) {
                            MapleInventoryManipulator.addFromDrop(c, item_get);
                            item.bundles = 0;
                            shop.removeFromSlot(slot);
                        }
                    }
                }
                c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(shop));
                break;
            }
            case 精靈商人_離開商店: {
                final IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null && shop instanceof HiredMerchant && shop.isOwner(chr) && shop.isAvailable()) {
                    shop.setOpen(true);
                    shop.saveItems();
                    shop.getMessages().clear();
                    shop.removeAllVisitors(-1, -1);
                    chr.setPlayerShop(null);
                }
                break;
            }
            case 精靈商人_物品整理: {
                final IMaplePlayerShop imps = chr.getPlayerShop();
                if (imps != null && imps.isOwner(chr) && !(imps instanceof MapleMiniGame)) {
                    for (int i = 0; i < imps.getItems().size(); i++) {
                        if (imps.getItems().get(i).bundles == 0) {
                            imps.getItems().remove(i);
                        }
                    }
                    if (chr.getMeso() + imps.getMeso() > 0) {
                        chr.gainMeso(imps.getMeso(), false);
                        imps.setMeso(0);
                    }
                    c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(imps));
                }
                break;
            }
            case 精靈商人_關閉商店: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == IMaplePlayerShop.HIRED_MERCHANT
                        && merchant.isOwner(chr)/* && merchant.isAvailable() */) {
                    c.getSession().write(PlayerShopPacket.hiredMerchantOwnerLeave());
                    merchant.removeVisitor(chr);
                    merchant.removeAllVisitors(-1, -1);
                    chr.setPlayerShop(null);
                    merchant.closeShop(true, true);
                }
                break;
            }
            case 精靈商人_查看訪問者: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).sendVisitor(c);
                }
                break;
            }
            case 精靈商人_鎖定清單: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).sendBlackList(c);
                }
                break;
            }
            case 精靈商人_更變名稱: {
                IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant == null || merchant.getShopType() != IMaplePlayerShop.HIRED_MERCHANT
                        || !merchant.isOwner(chr)) {
                    break;
                }
                String desc = slea.readMapleAsciiString();
                if (((HiredMerchant) merchant).canChangeName()) {
                    merchant.setDescription(desc);
                } else {
                    c.getPlayer().dropMessage(1,
                            "還不能更變名稱，還需要等待" + ((HiredMerchant) merchant).getChangeNameTimeLeft() + "秒。");
                }
                break;
            }
            case 精靈商人_添加黑名單: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).addBlackList(slea.readMapleAsciiString());
                }
                break;
            }
            case 精靈商人_移除黑名單: {
                final IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == 1 && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).removeBlackList(slea.readMapleAsciiString());
                }
                break;
            }
            case 精靈商人_維護開啟: {
                break;
            }
            case GIVE_UP: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 0, game.getVisitorSlot(chr)));
                    game.nextLoser();
                    game.setOpen(true);
                    game.update();
                    game.checkExitAfterGame();
                }
                break;
            }
            // case EXPEL: {
            // final IMaplePlayerShop ips = chr.getPlayerShop();
            // if (ips != null && ips instanceof MapleMiniGame) {
            // if (!((MapleMiniGame) ips).isOpen()) {
            // break;
            // }
            // ips.removeAllVisitors(3, 1); //no msg
            // }
            // break;
            // }
            // case READY:
            // case UN_READY: {
            // final IMaplePlayerShop ips = chr.getPlayerShop();
            // if (ips != null && ips instanceof MapleMiniGame) {
            // MapleMiniGame game = (MapleMiniGame) ips;
            // if (!game.isOwner(chr) && game.isOpen()) {
            // game.setReady(game.getVisitorSlot(chr));
            // game.broadcastToVisitors(PlayerShopPacket.getMiniGameReady(game.isReady(game.getVisitorSlot(chr))));
            // }
            // }
            // break;
            // }
            case START: {
                final IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOwner(chr) && game.isOpen()) {
                        for (int i = 1; i < ips.getSize(); i++) {
                            if (!game.isReady(i)) {
                                return;
                            }
                        }
                        game.setGameType();
                        game.shuffleList();
                        if (game.getGameType() == 1) {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameStart(game.getLoser()));
                        } else {
                            game.broadcastToVisitors(PlayerShopPacket.getMatchCardStart(game, game.getLoser()));
                        }
                        game.setOpen(false);
                        game.update();
                    }
                }
                break;
            }
            // case REQUEST_TIE: {
            // final IMaplePlayerShop ips = chr.getPlayerShop();
            // if (ips != null && ips instanceof MapleMiniGame) {
            // MapleMiniGame game = (MapleMiniGame) ips;
            // if (game.isOpen()) {
            // break;
            // }
            // if (game.isOwner(chr)) {
            // game.broadcastToVisitors(PlayerShopPacket.getMiniGameRequestTie(), false);
            // } else {
            // game.getMCOwner().getClient().getSession().writeAndFlush(PlayerShopPacket.getMiniGameRequestTie());
            // }
            // game.setRequestedTie(game.getVisitorSlot(chr));
            // }
            // break;
            // }
            // case ANSWER_TIE: {
            // final IMaplePlayerShop ips = chr.getPlayerShop();
            // if (ips != null && ips instanceof MapleMiniGame) {
            // MapleMiniGame game = (MapleMiniGame) ips;
            // if (game.isOpen()) {
            // break;
            // }
            // if (game.getRequestedTie() > -1 && game.getRequestedTie() !=
            // game.getVisitorSlot(chr)) {
            // if (slea.readByte() > 0) {
            // game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 1,
            // game.getRequestedTie()));
            // game.nextLoser();
            // game.setOpen(true);
            // game.update();
            // game.checkExitAfterGame();
            // } else {
            // game.broadcastToVisitors(PlayerShopPacket.getMiniGameDenyTie());
            // }
            // game.setRequestedTie(-1);
            // }
            // }
            // break;
            // }
            // case SKIP: {
            // final IMaplePlayerShop ips = chr.getPlayerShop();
            // if (ips != null && ips instanceof MapleMiniGame) {
            // MapleMiniGame game = (MapleMiniGame) ips;
            // if (game.isOpen()) {
            // break;
            // }
            // if (game.getLoser() != ips.getVisitorSlot(chr)) {
            // ips.broadcastToVisitors(PlayerShopPacket.shopChat("Turn could not be skipped
            // by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " +
            // ips.getVisitorSlot(chr), ips.getVisitorSlot(chr)));
            // return;
            // }
            // ips.broadcastToVisitors(PlayerShopPacket.getMiniGameSkip(ips.getVisitorSlot(chr)));
            // game.nextLoser();
            // }
            // break;
            // }
            // case MOVE_OMOK: {
            // final IMaplePlayerShop ips = chr.getPlayerShop();
            // if (ips != null && ips instanceof MapleMiniGame) {
            // MapleMiniGame game = (MapleMiniGame) ips;
            // if (game.isOpen()) {
            // break;
            // }
            // if (game.getLoser() != game.getVisitorSlot(chr)) {
            // game.broadcastToVisitors(PlayerShopPacket.shopChat("Omok could not be placed
            // by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " +
            // game.getVisitorSlot(chr), game.getVisitorSlot(chr)));
            // return;
            // }
            // game.setPiece(slea.readInt(), slea.readInt(), slea.readByte(), chr);
            // }
            // break;
            // }
            // case SELECT_CARD: {
            // final IMaplePlayerShop ips = chr.getPlayerShop();
            // if (ips != null && ips instanceof MapleMiniGame) {
            // MapleMiniGame game = (MapleMiniGame) ips;
            // if (game.isOpen()) {
            // break;
            // }
            // if (game.getLoser() != game.getVisitorSlot(chr)) {
            // game.broadcastToVisitors(PlayerShopPacket.shopChat("Card could not be placed
            // by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " +
            // game.getVisitorSlot(chr), game.getVisitorSlot(chr)));
            // return;
            // }
            // if (slea.readByte() != game.getTurn()) {
            // game.broadcastToVisitors(PlayerShopPacket.shopChat("Omok could not be placed
            // by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " +
            // game.getVisitorSlot(chr) + " Turn: " + game.getTurn(),
            // game.getVisitorSlot(chr)));
            // return;
            // }
            // final int slot = slea.readByte();
            // final int turn = game.getTurn();
            // final int fs = game.getFirstSlot();
            // if (turn == 1) {
            // game.setFirstSlot(slot);
            // if (game.isOwner(chr)) {
            // game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs,
            // turn), false);
            // } else {
            // game.getMCOwner().getClient().getSession().writeAndFlush(PlayerShopPacket.getMatchCardSelect(turn,
            // slot, fs, turn));
            // }
            // game.setTurn(0); //2nd turn nao
            // return;
            // } else if (fs > 0 && game.getCardId(fs + 1) == game.getCardId(slot + 1)) {
            // game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs,
            // game.isOwner(chr) ? 2 : 3));
            // game.setPoints(game.getVisitorSlot(chr)); //correct.. so still same loser.
            // diff turn tho
            // } else {
            // game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs,
            // game.isOwner(chr) ? 0 : 1));
            // game.nextLoser();//wrong haha
            //
            // }
            // game.setTurn(1);
            // game.setFirstSlot(0);
            //
            // }
            // break;
            // }
            // case EXIT_AFTER_GAME:
            // case CANCEL_EXIT: {
            // final IMaplePlayerShop ips = chr.getPlayerShop();
            // if (ips != null && ips instanceof MapleMiniGame) {
            // MapleMiniGame game = (MapleMiniGame) ips;
            // if (game.isOpen()) {
            // break;
            // }
            // game.setExitAfter(chr);
            // game.broadcastToVisitors(PlayerShopPacket.getMiniGameExitAfter(game.isExitAfter(chr)));
            // }
            // break;
            // }
            default: {
                if (chr.isShowErr()) {
                    chr.showInfo("玩家互動", true,
                            "未處理的操作類型" + action.name() + "(" + action.getValue() + "):" + slea.toString());
                }
                break;
            }
        }
    }
}
