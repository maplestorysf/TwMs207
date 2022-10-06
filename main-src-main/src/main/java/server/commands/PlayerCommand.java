package server.commands;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ItemConstants;
import server.swing.WvsCenter;
import handling.channel.ChannelServer;
import scripting.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.stores.IMaplePlayerShop;
import server.stores.MaplePlayerShopItem;
import tools.StringUtil;
import tools.packet.CWvsContext;
import tools.packet.PlayerShopPacket;

/**
 *
 * @author Emilyx3
 */
public class PlayerCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }

    public static class ea extends 解卡 {
    }

    public static class 解卡 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            c.getPlayer().dropMessage(-11, "解卡成功");
            return 1;
        }
    }

    public static class 查看 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            c.getPlayer().showMessage(10, "如下是你在伺服器上的訊息，如不不正確請與管理員聯繫");
            c.getPlayer().showPlayerStats(false);
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return 1;
        }
    }

    public static class GM extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (final ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.broadcastGMMessage(tools.packet.CField.multiChat("[管理幫幫忙] " + c.getPlayer().getName(),
                        StringUtil.joinStringFrom(splitted, 1), 4));
            }
            WvsCenter.addChatLog(
                    "[管理幫幫忙] " + c.getPlayer().getName() + ": " + StringUtil.joinStringFrom(splitted, 1) + "\r\n");
            c.getPlayer().dropMessage(5, "訊息發送成功");
            return 1;
        }
    }

    /* 遊戲活動相關 */
    public static class Event extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().isInBlockedMap() || c.getPlayer().hasBlockedInventory()) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            NPCScriptManager.getInstance().start(c, 9000000, null);
            return 1;
        }
    }

    public static class JoinEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getChannelServer().warpToEvent(c.getPlayer());
            return 1;
        }
    }

    /*
     * public static class SpawnBomb extends CommandExecute {
     * 
     * @Override public int execute(MapleClient c, String[] splitted) { if
     * (c.getPlayer().getMapId() != 109010100) { c.getPlayer().dropMessage(5,
     * "You may only spawn bomb in the event map."); return 0; } if
     * (!c.getChannelServer().bombermanActive()) { c.getPlayer().dropMessage(5,
     * "You may not spawn bombs yet."); return 0; }
     * c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(
     * 9300166), c.getPlayer().getPosition()); return 1; } }
     * 
     * //大概是賽跑活動的指令 public static class JoinRace extends CommandExecute {
     * 
     * @Override public int execute(MapleClient c, String[] splitted) { if
     * (c.getPlayer().getEntryNumber() < 1) { if (c.getPlayer().getMapId() ==
     * 100000000) { if (c.getChannelServer().getWaiting() || c.getPlayer().isGM()) {
     * //TOD: test
     * c.getPlayer().setEntryNumber(c.getChannelServer().getCompetitors() + 1);
     * c.getChannelServer().setCompetitors(c.getChannelServer().getCompetitors() +
     * 1); SkillFactory.getSkill(c.getPlayer().getGender() == 1 ? 80001006 :
     * 80001005).getEffect(1).applyTo(c.getPlayer()); c.getPlayer().dropMessage(-11,
     * "You have successfully joined the race! Your entry number is " +
     * c.getPlayer().getEntryNumber() + "."); c.getPlayer().dropMessage(1,
     * "If you cancel the mount buff, you will automatically leave the race."); }
     * else { c.getPlayer().dropMessage(-11,
     * "There is no event currently taking place."); return 0; } } else {
     * c.getPlayer().dropMessage(-11, "You are not at Henesys."); return 0; } } else
     * { c.getPlayer().dropMessage(-11, "You have already joined this race.");
     * return 0; } return 1; } }
     * 
     * public static class Rules extends CommandExecute {
     * 
     * @Override public int execute(MapleClient c, String[] splitted) { if
     * (c.getChannelServer().getWaiting() || c.getChannelServer().getRace()) {
     * c.getPlayer().dropMessage(-11,
     * "The Official Rules and Regulations of the Great Victoria Island Race:");
     * c.getPlayer().dropMessage(-11,
     * "-------------------------------------------------------------------------------------------"
     * ); c.getPlayer().dropMessage(-11,
     * "To win you must race from Henesys all the way to Henesys going Eastward.");
     * c.getPlayer().dropMessage(-11,
     * "Rule #1: No cheating. You can't use any warping commands, or you'll be disqualified."
     * ); c.getPlayer().dropMessage(-11,
     * "Rule #2: You may use any form of transportation. This includes Teleport, Flash Jump and Mounts."
     * ); c.getPlayer().dropMessage(-11,
     * "Rule #3: You are NOT allowed to kill any monsters in your way. They are obstacles."
     * ); c.getPlayer().dropMessage(-11,
     * "Rule #4: You may start from anywhere in Henesys, but moving on to the next map before the start won't work."
     * ); } else { c.getPlayer().dropMessage(-11,
     * "There is no event currently taking place."); return 0; } return 1; } }
     */
    public static class 卡圖 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().卡圖 == c.getPlayer().getMapId()
                    && !GameConstants.isTutorialMap(c.getPlayer().getMapId())) {
                c.getPlayer().changeMap(100000000, 0);
            } else {
                c.getPlayer().dropMessage(1, "你並沒有卡圖啊。");
            }
            c.getPlayer().卡圖 = 0;
            return 1;
        }
    }

    public static class 交易道具 extends CommandExecute.TradeExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-2, "系統提示 : <欄位:裝備/消耗/裝飾/其他/特殊> <道具名稱> [道具數量(可選,默認1)]");
            } else if (c.getPlayer().getLevel() < 70) {
                c.getPlayer().dropMessage(-2, "系統提示 : 只有等級達到70或以上才能使用這個指令.");
            } else {
                String type = splitted[1];
                MapleInventoryType iType;
                switch (type) {
                    case "裝備":
                        iType = MapleInventoryType.EQUIP;
                        break;
                    case "消耗":
                        iType = MapleInventoryType.USE;
                        break;
                    case "裝飾":
                        iType = MapleInventoryType.SETUP;
                        break;
                    case "其他":
                        iType = MapleInventoryType.ETC;
                        break;
                    case "特殊":
                        iType = MapleInventoryType.CASH;
                        break;
                    default:
                        c.getPlayer().dropMessage(-2, "系統提示 : 請輸入正確的欄位名稱(裝備/消耗/裝飾/其他/特殊)");
                        return 0;
                }

                String search = splitted[2].toLowerCase();
                int quantity = 1;
                try {
                    quantity = Integer.parseInt(splitted[3]);
                } catch (Exception e) {
                }
                Item found = null;
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                for (Item inv : c.getPlayer().getInventory(iType)) {
                    if (ii.getName(inv.getItemId()) != null
                            && ii.getName(inv.getItemId()).toLowerCase().contains(search)) {
                        found = inv;
                        break;
                    }
                }
                final IMaplePlayerShop shop = c.getPlayer().getPlayerShop();
                if (c.getPlayer().getTrade() == null && (shop == null || !shop.isOwner(c.getPlayer()))) {
                    c.getPlayer().dropMessage(-2, "系統提示 : 當前狀態無法使用");
                    return 0;
                } else if (found == null) {
                    c.getPlayer().dropMessage(-2, "系統提示 : 沒有找到道具 (" + search + ")");
                    return 0;
                } else if (ItemConstants.類型.寵物(found.getItemId()) || ItemConstants.類型.可充值道具(found.getItemId())) {
                    c.getPlayer().dropMessage(-2, "系統提示 : 你不應該用指令來交易此道具");
                    return 0;
                } else if (quantity > found.getQuantity() || quantity <= 0) {
                    c.getPlayer().dropMessage(-2, "系統提示 : 錯誤的數量");
                    return 0;
                } else {
                    if (c.getPlayer().getTrade() != null) {
                        if (quantity > ii.getSlotMax(found.getItemId())) {
                            c.getPlayer().dropMessage(-2, "系統提示 : 錯誤的數量");
                            return 0;
                        } else if (!c.getPlayer().getTrade().setItems(c, found, (byte) -1, quantity)) {
                            c.getPlayer().dropMessage(-2, "系統提示 : 這個道具無法交易");
                            return 0;
                        } else {
                            c.getPlayer().getTrade().chatAuto("系統提示 : " + c.getPlayer().getName() + " 添加道具 "
                                    + ii.getName(found.getItemId()) + " x " + quantity);
                        }
                    } else if (shop != null && shop.isOwner(c.getPlayer())) {
                        boolean f = false;
                        for (MaplePlayerShopItem item : shop.getItems()) {
                            if (ii.getSlotMax(found.getItemId()) - item.bundles < quantity
                                    || item.item.getItemId() != found.getItemId() || item.item.getQuantity() != 1
                                    || item.bundles <= 0) {
                                continue;
                            }
                            f = true;
                            MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(found.getItemId()),
                                    found.getItemId(), quantity, true, false);
                            item.bundles = (short) (item.bundles + quantity);
                            c.getSession().writeAndFlush(PlayerShopPacket.shopItemUpdate(shop));
                            break;
                        }
                        if (!f) {
                            c.getPlayer().dropMessage(-2, "系統提示 : 添加道具失敗,請確認商店里放上至少1個或道具未達到存儲的最大上限個數並且確認道具非以組販賣");
                        } else {
                            c.getPlayer().dropMessage(-2,
                                    "系統提示 : 添加道具 " + ii.getName(found.getItemId()) + " x " + quantity);
                        }
                        return 0;
                    } else {
                        c.getPlayer().dropMessage(-2, "系統提示 : 發生未知錯誤");
                    }
                }
            }
            return 1;
        }
    }
}
