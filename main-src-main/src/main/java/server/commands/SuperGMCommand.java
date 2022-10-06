/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.commands;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.anticheat.CheatingOffense;
import client.inventory.*;
import client.skill.Skill;
import client.skill.SkillFactory;
import constants.FeaturesConfig;
import constants.GameConstants;
import constants.ItemConstants;
import database.ManagerDatabasePool;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.world.World;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.MapleGachapon;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Timer;
import server.Timer.*;
import server.commands.InternCommand.封鎖帳號;
import server.life.*;
import server.maps.*;
import server.quest.MapleQuest;
import server.shops.MapleShopFactory;
import tools.*;
import tools.packet.CField;
import tools.packet.CField.NPCPacket;
import tools.packet.CWvsContext;
import tools.packet.MobPacket;

import java.awt.*;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 *
 * @author Emilyx3
 */
public class SuperGMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERGM;
    }

    public static class serverMSG extends 伺服器公告 {
    }

    public static class 伺服器公告 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <類型> <頻道> <內容>");
                return 0;
            }
            for (MapleCharacter all : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                all.getClient().getChannelServer()
                        .broadcastMessage(CWvsContext.broadcastMsg(Integer.parseInt(splitted[1]),
                                Integer.parseInt(splitted[2]), StringUtil.joinStringFrom(splitted, 3)));
            }
            return 1;
        }
    }

    public static class SpecialMessage extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter all : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                all.getClient().getChannelServer().broadcastMessage(CWvsContext
                        .getSpecialMsg(StringUtil.joinStringFrom(splitted, 2), Integer.parseInt(splitted[1]), true));
            }
            return 1;
        }
    }

    public static class HideSpecialMessage extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter all : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                all.getClient().getChannelServer().broadcastMessage(CWvsContext.getSpecialMsg("", 0, false));
            }
            return 1;
        }
    }

    public static class clockwarpmap extends 定時更變地圖 {

    }

    public static class 定時更變地圖 extends CommandExecute {

        @Override
        public int execute(final MapleClient c, String splitted[]) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " (初始地圖ID:默認當前地圖) <更變后的地圖ID> <時間:秒>");
                return 0;
            }
            final int map;
            final int nextmap;
            final int time;
            if (splitted.length == 4) {
                map = Integer.parseInt(splitted[1]);
                nextmap = Integer.parseInt(splitted[2]);
                time = Integer.parseInt(splitted[3]);
            } else {
                map = c.getPlayer().getMapId();
                nextmap = Integer.parseInt(splitted[1]);
                time = Integer.parseInt(splitted[2]);
            }
            c.getChannelServer().getMapFactory().getMap(map).broadcastMessage(CField.getClock(time));
            c.getChannelServer().getMapFactory().getMap(map).startMapEffect("計時結束後將被傳送離開此地圖", 5120041);
            EventTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    for (MapleCharacter mch : c.getChannelServer().getMapFactory().getMap(map).getCharacters()) {
                        if (mch == null) {
                            return;
                        } else {
                            mch.changeMap(nextmap, 0);
                        }
                    }
                }
            }, time * 1000); // seconds
            return 1;
        }
    }

    public static class setPlayerName extends 設置名稱 {
    }

    public static class 設置名稱 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <玩家新名稱>");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                c.getPlayer().dropMessage(-11, "找不到玩家");
                return 0;
            }
            if (c.getPlayer().getGmLevel() < 6 && !victim.isGM()) {
                c.getPlayer().dropMessage(-11, "沒有權限更改比自己高等級的管理員的名稱");
                return 0;
            }
            victim.getClient().getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            victim.getClient().disconnect(true, false);
            victim.setName(splitted[2]);
            return 1;
        }
    }

    public static class Popup extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (splitted.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(StringUtil.joinStringFrom(splitted, 1));
                    mch.dropMessage(1, sb.toString());
                } else {
                    c.getPlayer().dropMessage(-11, splitted[0] + " <內容>");
                    return 0;
                }
            }
            return 1;
        }
    }

    public static class saveCh extends 存檔 {

    }

    public static class 存檔 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                mch.saveToDB(false, false);
            }
            c.getPlayer().dropMessage(-11, "存檔成功");
            return 1;
        }
    }

    public static class saveAndroid extends 機器人存檔 {
    }

    public static class 機器人存檔 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                mch.getAndroid().saveToDb();
                mch.dropMessage(0, "機器人存檔成功");
            }
            return 1;
        }
    }

    public static class 完全鎖帳 extends 封鎖帳號 {

        public 完全鎖帳() {
            hellban = true;
        }
    }

    public static class unbanFully extends 解鎖完全鎖帳 {

    }

    public static class 解鎖完全鎖帳 extends 解鎖帳號 {

        public 解鎖完全鎖帳() {
            hellban = true;
        }
    }

    public static class unban extends 解鎖帳號 {

    }

    public static class 解鎖帳號 extends CommandExecute {

        protected boolean hellban = false;

        private String getCommand() {
            if (hellban) {
                return "解鎖完整鎖帳";
            } else {
                return "解鎖帳號";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱>");
                return 0;
            }
            byte ret;
            if (hellban) {
                ret = MapleClient.unban(splitted[1], false, true);
            } else {
                ret = MapleClient.unban(splitted[1], false, false);
            }
            if (ret == -2) {
                c.getPlayer().dropMessage(-11, "[" + getCommand() + "] 數據庫出錯");
                return 0;
            } else if (ret == -1) {
                c.getPlayer().dropMessage(-11, "[" + getCommand() + "] 玩家不存在");
                return 0;
            } else {
                c.getPlayer().dropMessage(-11, "[" + getCommand() + "] 解鎖成功");
            }
            byte ret_ = MapleClient.unbanIPMacs(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] 數據庫出錯");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] 玩家不存在");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] 此玩家IP或Mac不存在");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] IP或Mac已解鎖");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] IP和Macs已解鎖");
            }
            return ret_ > 0 ? 1 : 0;
        }
    }

    public static class unbanIPMac extends 解鎖IPMAC {
    }

    public static class 解鎖IPMAC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱>");
                return 0;
            }
            byte ret = MapleClient.unbanIPMacs(splitted[1]);
            if (ret == -2) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] 數據庫出錯");
            } else if (ret == -1) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] 玩家不存在");
            } else if (ret == 0) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] 此玩家IP或Mac不存在");
            } else if (ret == 1) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] IP或Mac已解鎖");
            } else if (ret == 2) {
                c.getPlayer().dropMessage(-11, "[解鎖IP/Mac] IP和Macs已解鎖");
            }
            if (ret > 0) {
                return 1;
            }
            return 0;
        }
    }

    public static class giveSkill extends 給予技能 {
    }

    public static class 給予技能 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <角色名稱> <技能ID> (技能等級:默認1) (技能最高等級:默認1)");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);

            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            victim.changeSingleSkillLevel(skill, level, masterlevel);
            return 1;
        }
    }

    public static class unlockitem extends 解封印道具 {

    }

    public static class 解封印道具 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<Item> eqs = new ArrayList<>();
            boolean add = false;
            if (splitted.length < 2 || splitted[1].equals("全部")) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (Item item : c.getPlayer().getInventory(type)) {
                        if (ItemFlag.LOCK.check(item.getFlag())) {
                            item.setFlag(item.getFlag() - ItemFlag.LOCK.getValue());
                            add = true;
                            // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                            // type.getType()));
                        }
                        if (ItemFlag.UNTRADABLE.check(item.getFlag())) {
                            item.setFlag(item.getFlag() - ItemFlag.UNTRADABLE.getValue());
                            add = true;
                            // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                            // type.getType()));
                        }
                        if (add) {
                            eqs.add(item);
                        }
                        add = false;
                    }
                }
            } else if (splitted[1].equals("身上裝備")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).newList()) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.LOCK.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (ItemFlag.UNTRADABLE.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.UNTRADABLE.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (add) {
                        eqs.add(item);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("裝備")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.LOCK.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (ItemFlag.UNTRADABLE.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.UNTRADABLE.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (add) {
                        eqs.add(item);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("消耗")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.LOCK.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (ItemFlag.UNTRADABLE.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.UNTRADABLE.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (add) {
                        eqs.add(item);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("裝飾")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.LOCK.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (ItemFlag.UNTRADABLE.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.UNTRADABLE.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (add) {
                        eqs.add(item);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("其他")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.LOCK.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (ItemFlag.UNTRADABLE.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.UNTRADABLE.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (add) {
                        eqs.add(item);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("特殊")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.LOCK.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (ItemFlag.UNTRADABLE.check(item.getFlag())) {
                        item.setFlag(item.getFlag() - ItemFlag.UNTRADABLE.getValue());
                        add = true;
                        // c.getSession().writeAndFlush(CField.updateSpecialItemUse(item,
                        // type.getType()));
                    }
                    if (add) {
                        eqs.add(item);
                    }
                    add = false;
                }
            } else {
                c.getPlayer().dropMessage(-11, splitted[0] + " (道具類型:全部(空)/身上裝備/裝備/消耗/其他/裝飾/特殊)");
            }

            for (Item eq : eqs) {
                c.getPlayer().forceReAddItem_NoUpdate(eq.copy());
            }
            return 1;
        }
    }

    public static class drop extends 扔 {
    }

    public static class 扔 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <道具ID> (數量:默認)");
                return 0;
            }
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, "此道具不存在");
            } else {
                Item toDrop;
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    toDrop = new client.inventory.Item(itemId, (byte) 0, quantity, 0);
                }
                MaplePet pet = null;
                if (ItemConstants.類型.寵物(itemId)) {
                    pet = MaplePet.createPet(itemId);
                    toDrop.setExpiration(System.currentTimeMillis() + (ii.getLife(itemId) * 24 * 60 * 60 * 1000));
                    if (pet != null) {
                        toDrop.setPet(pet);
                    }
                }
                toDrop.setUniqueId(MapleInventoryManipulator.getUniqueId(toDrop.getItemId(), pet));
                toDrop.setGMLog(c.getPlayer().getName() + " 使用 " + splitted[0] + " 指令製作, 時間:"
                        + FileoutputUtil.CurrentReadable_Time());
                if (!c.getPlayer().isAdmin()) {
                    toDrop.setOwner(c.getPlayer().getName());
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(),
                        true, true);
            }
            return 1;
        }
    }

    public static class dropName extends 扔道具 {
    }

    public static class 扔道具 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <數量> <道具名稱>");
                return 0;
            }
            final String itemName = StringUtil.joinStringFrom(splitted, 2);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1);
            int itemId = 0;
            for (Pair<Integer, String> item : MapleItemInformationProvider.getInstance().getAllItems2()) {
                if (item.getRight().toLowerCase().equals(itemName.toLowerCase())) {
                    itemId = item.getLeft();
                    break;
                }
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, "此道具不存在");
            } else {
                Item toDrop;
                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = ii.getEquipById(itemId);
                } else {
                    toDrop = new client.inventory.Item(itemId, (byte) 0, quantity, 0);
                }
                MaplePet pet = null;
                if (ItemConstants.類型.寵物(itemId)) {
                    pet = MaplePet.createPet(itemId);
                    toDrop.setExpiration(System.currentTimeMillis() + (ii.getLife(itemId) * 24 * 60 * 60 * 1000));
                    if (pet != null) {
                        toDrop.setPet(pet);
                    }
                }
                toDrop.setUniqueId(MapleInventoryManipulator.getUniqueId(toDrop.getItemId(), pet));
                toDrop.setGMLog(c.getPlayer().getName() + " 使用 " + splitted[0] + " 指令製作, 時間:"
                        + FileoutputUtil.CurrentReadable_Time());
                if (!c.getPlayer().isAdmin()) {
                    toDrop.setOwner(c.getPlayer().getName());
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(),
                        true, true);
            }
            return 1;
        }
    }

    public static class Marry extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, "Need <name> <itemid>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[2]);
            if (!ItemConstants.類型.特效戒指(itemId)) {
                c.getPlayer().dropMessage(-11, "Invalid itemID.");
            } else {
                MapleCharacter fff = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (fff == null) {
                    c.getPlayer().dropMessage(-11, "Player must be online");
                } else {
                    int[] ringID = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
                    try {
                        MapleCharacter[] chrz = {fff, c.getPlayer()};
                        for (int i = 0; i < chrz.length; i++) {
                            Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId,
                                    ringID[i]);
                            if (eq == null) {
                                c.getPlayer().dropMessage(-11, "Invalid itemID.");
                                return 0;
                            }
                            MapleInventoryManipulator.addbyItem(chrz[i].getClient(), eq.copy());
                            chrz[i].dropMessage(6, "Successfully married with " + chrz[i == 0 ? 1 : 0].getName());
                        }
                        MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                    } catch (SQLException e) {
                    }
                }
            }
            return 1;
        }
    }

    public static class mobVac extends 吸怪 {

    }

    public static class 吸怪 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (final MapleMapObject mmo : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
                final MapleMonster monster = (MapleMonster) mmo;
                c.getPlayer().getMap().broadcastMessage(MobPacket.moveMonster(false, -1, 0, monster.getObjectId(), 0,
                        monster.getTruePosition(), new Point(0, 0), c.getPlayer().getLastRes()));
                monster.setPosition(c.getPlayer().getPosition());
            }
            return 1;
        }
    }

    public static class speakM extends 全圖說話 {

    }

    public static class 全圖說話 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(CField.getChatText(victim.getId(),
                            StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return 1;
        }
    }

    public static class speakC extends 全頻說話 {

    }

    public static class 全頻說話 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter victim : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(CField.getChatText(victim.getId(),
                            StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return 1;
        }
    }

    public static class speakW extends 全世界說話 {

    }

    public static class 全世界說話 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter victim : cserv.getPlayerStorage().getAllCharacters()) {
                    if (victim.getId() != c.getPlayer().getId()) {
                        victim.getMap().broadcastMessage(CField.getChatText(victim.getId(),
                                StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                    }
                }
            }
            return 1;
        }
    }

    public static class monitor extends 監視 {

    }

    public static class 監視 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱>");
                return 0;
            }
            MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (target != null) {
                if (target.getClient().isMonitored()) {
                    target.getClient().setMonitored(false);
                    c.getPlayer().dropMessage(5, "停止了對 " + target.getName() + " 的監視");
                } else {
                    target.getClient().setMonitored(true);
                    c.getPlayer().dropMessage(5, "正在對 " + target.getName() + " 進行監視");
                }
            } else {
                c.getPlayer().dropMessage(5, "在當前頻道中找不到此玩家");
                return 0;
            }
            return 1;
        }
    }

    public static class 重置玩家任務 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <任務ID>");
                return 0;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[2]))
                    .forfeit(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]));
            return 1;
        }
    }

    public static class FStartOther extends 開始玩家任務 {

    }

    public static class 開始玩家任務 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <任務ID> <NPCID> (customData:默認空)");
                return 0;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceStart(
                    c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]),
                    Integer.parseInt(splitted[3]), splitted.length > 4 ? splitted[4] : null);
            return 1;
        }
    }

    public static class FCompleteOther extends 完成玩家任務 {

    }

    public static class 完成玩家任務 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <任務ID> <NPCID>");
                return 0;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceComplete(
                    c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]),
                    Integer.parseInt(splitted[3]));
            return 1;
        }
    }

    public static class Threads extends 線程 {

    }

    public static class 線程 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().contains(filter.toLowerCase())) {
                    c.getPlayer().dropMessage(-11, i + ": " + tstring);
                }
            }
            return 1;
        }
    }

    public static class ShowTrace extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                throw new IllegalArgumentException();
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            c.getPlayer().dropMessage(-11, t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                c.getPlayer().dropMessage(-11, elem.toString());
            }
            return 1;
        }
    }

    public static class DetectCheat extends 檢測作弊行為 {

    }

    public static class 檢測作弊行為 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                StringBuilder sb = new StringBuilder();
                for (CheatingOffense co : CheatingOffense.values()) {
                    sb.append(co.name()).append("/");
                }
                c.getPlayer().dropMessage(-11, splitted[0] + " <行為>");
                c.getPlayer().dropMessage(-11, "行為: " + sb.toString());
                return 0;
            }
            try {
                CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
                co.setEnabled(!co.isEnabled());
            } catch (IllegalArgumentException iae) {
                c.getPlayer().dropMessage(-11, "作弊行為 " + splitted[1] + " 沒找到");
            }
            return 1;
        }
    }

    public static class TMegaphone extends 開關喇叭 {

    }

    public static class 開關喇叭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            World.toggleMegaphoneMuteState();
            c.getPlayer().dropMessage(-11, "喇叭狀態 : " + (c.getChannelServer().getMegaphoneMuteState() ? "可用" : "不可用"));
            return 1;
        }
    }

    public static class SSetReactor extends 放置反應堆 {

    }

    public static class 放置反應堆 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <反應堆ID>");
                return 0;
            }
            MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(splitted[1])),
                    Integer.parseInt(splitted[1]));
            reactor.setDelay(-1);
            c.getPlayer().getMap().spawnReactorOnGroundBelow(reactor,
                    new Point(c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y - 20));
            return 1;
        }
    }

    /*
     * public static class ClearSquads extends CommandExecute {
     * 
     * @Override public int execute(MapleClient c, String[] splitted) { final
     * Collection<MapleSquad> squadz = new
     * ArrayList<>(c.getChannelServer().getAllSquads().values()); for (MapleSquad
     * squads : squadz) { squads.clear(); } return 1; } }
     */
    public static class damageMobByOID extends 傷OID怪物 {

    }

    public static class 傷OID怪物 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <怪物OID> <傷害值>");
                return 0;
            }
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            int damage = Integer.parseInt(splitted[2]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.broadcastMessage(MobPacket.damageMonster(targetId, damage));
                monster.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

    public static class damageMapMob extends 傷全圖怪 {

    }

    public static class 傷全圖怪 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            int damage = 0;
            if (splitted.length > 2) {
                int irange = Integer.parseInt(splitted[1]);
                if (irange != 0) {
                    range = irange * irange;
                }
                if (splitted.length <= 2) {
                    damage = Integer.parseInt(splitted[2]);
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    damage = Integer.parseInt(splitted[3]);
                }
            } else if (splitted.length == 2) {
                damage = Integer.parseInt(splitted[1]);
            } else {
                c.getPlayer().dropMessage(-11, splitted[0] + " (<範圍:默認0全圖> (地圖ID:默認當前地圖)) <傷害值>");
                return 0;
            }
            if (map == null) {
                c.getPlayer().dropMessage(-11, "地圖不存在");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range,
                    Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                mob.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }

    public static class damageMob extends 傷怪 {

    }

    public static class 傷怪 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <傷害> <怪物ID>");
                return 0;
            }
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            int damage = Integer.parseInt(splitted[1]);
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range,
                    Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[2])) {
                    map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                    mob.damage(c.getPlayer(), damage, false);
                }
            }
            return 1;
        }
    }

    public static class killall extends 殺怪 {

    }

    public static class 殺怪 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <怪物ID>");
                return 0;
            }
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range,
                    Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[1])) {
                    mob.damage(c.getPlayer(), (int) mob.getHp(), false);
                }
            }
            c.getPlayer().monsterMultiKill();
            return 1;
        }
    }

    public static class killallexp extends 清怪獲得經驗 {

    }

    public static class 清怪獲得經驗 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " (<範圍:默認0全圖> (地圖:默認當前地圖))");
            }
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                // && !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster")
                // && !splitted[0].equals("!hitmonsterbyoid") &&
                // !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(-11, "地圖不存在");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range,
                    Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                mob.damage(c.getPlayer(), (int) mob.getHp(), false);
            }
            c.getPlayer().monsterMultiKill();
            return 1;
        }
    }

    public static class cleardrops extends 清怪掉寶 {

    }

    public static class removedrops extends 清怪掉寶 {

    }

    public static class 清怪掉寶 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " (<範圍:默認0全圖> (地圖:默認當前地圖))");
            }
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(-11, "地圖不存在");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range,
                    Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (!mob.getStats().isBoss() || mob.getStats().isPartyBonus() || c.getPlayer().isGM()) {
                    map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
                }
            }
            c.getPlayer().monsterMultiKill();
            return 1;
        }
    }

    public static class npc extends 添加臨時NPC {

    }

    public static class 添加臨時NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <NPCID>");
                return 0;
            }
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition(), false).getId());
                npc.setCustom(true);
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(NPCPacket.spawnNPC(npc, true));
            } else {
                c.getPlayer().dropMessage(-11, "NPCID無效");
                return 0;
            }
            return 1;
        }
    }

    public static class pnpc extends 添加NPC {

    }

    public static class 添加NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <NPCID>");
                return 0;
            }
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                final int xpos = c.getPlayer().getPosition().x;
                final int ypos = c.getPlayer().getPosition().y;
                final int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition(), false)
                        .getId();
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos);
                npc.setRx1(xpos);
                npc.setFh(fh);
                npc.setCustom(true);
                try {
                    Connection con = ManagerDatabasePool.getConnection();

                    try (PreparedStatement ps = (PreparedStatement) con.prepareStatement(
                            "INSERT INTO wz_customlife (dataid, f, hide, fh, cy, rx0, rx1, type, x, y, mid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, npcId);
                        ps.setInt(2, 0); // 1 = right , 0 = left
                        ps.setInt(3, 0); // 1 = hide, 0 = show
                        ps.setInt(4, fh);
                        ps.setInt(5, ypos);
                        ps.setInt(6, xpos);
                        ps.setInt(7, xpos);
                        ps.setString(8, "n");
                        ps.setInt(9, xpos);
                        ps.setInt(10, ypos);
                        ps.setInt(11, c.getPlayer().getMapId());
                        ps.executeUpdate();
                    }
                    ManagerDatabasePool.closeConnection(con);
                } catch (SQLException e) {
                    c.getPlayer().dropMessage(-11, "將NPC添加到數據庫失敗");
                }
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(NPCPacket.spawnNPC(npc, true));
                c.getPlayer().dropMessage(-11, "請不要重載此地圖, 否則伺服器重啟後NPC會消失");
            } else {
                c.getPlayer().dropMessage(-11, "NPCID無效");
                return 0;
            }
            return 1;
        }
    }

    public static class Pmob extends 添加怪物 {
    }

    public static class 添加怪物 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <怪物ID> <數量>");
                return 0;
            }
            int mobid = Integer.parseInt(splitted[1]);
            int mobTime = Integer.parseInt(splitted[2]);
            MapleMonster npc;
            try {
                npc = MapleLifeFactory.getMonster(mobid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "錯誤: " + e.getMessage());
                return 0;
            }
            if (npc != null) {
                final int xpos = c.getPlayer().getPosition().x;
                final int ypos = c.getPlayer().getPosition().y;
                final int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition(), false)
                        .getId();
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos);
                npc.setRx1(xpos);
                npc.setFh(fh);
                try {
                    Connection con = ManagerDatabasePool.getConnection();
                    try (PreparedStatement ps = (PreparedStatement) con.prepareStatement(
                            "INSERT INTO wz_customlife (dataid, f, hide, fh, cy, rx0, rx1, type, x, y, mid, mobtime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        ps.setInt(1, mobid);
                        ps.setInt(2, 0); // 1 = right , 0 = left
                        ps.setInt(3, 0); // 1 = hide, 0 = show
                        ps.setInt(4, fh);
                        ps.setInt(5, ypos);
                        ps.setInt(6, xpos);
                        ps.setInt(7, xpos);
                        ps.setString(8, "m");
                        ps.setInt(9, xpos);
                        ps.setInt(10, ypos);
                        ps.setInt(11, c.getPlayer().getMapId());
                        ps.setInt(12, mobTime);
                        ps.executeUpdate();
                    }
                    ManagerDatabasePool.closeConnection(con);
                } catch (SQLException e) {
                    c.getPlayer().dropMessage(-11, "將怪物添加到數據庫失敗");
                }
                c.getPlayer().getMap().addMonsterSpawn(npc, mobTime, (byte) -1, null);
                c.getPlayer().dropMessage(-11, "請不要重載此地圖, 否則伺服器重啟後怪物會消失");
            } else {
                c.getPlayer().dropMessage(-11, "怪物ID無效");
                return 0;
            }
            return 1;
        }
    }

    public static class playerNPC extends 玩家NPC {

    }

    public static class 玩家NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <NPCID>");
                return 0;
            }
            try {
                c.getPlayer().dropMessage(-11, "正在製作玩家NPC...");
                MapleClient cs = new MapleClient(null, null, new MockIOSession());
                MapleCharacter chhr = MapleCharacter.loadCharFromDB(MapleCharacterUtil.getIdByName(splitted[1]), cs,
                        false);
                if (chhr == null) {
                    c.getPlayer().dropMessage(-11, "玩家不存在");
                    return 0;
                }
                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(),
                        c.getPlayer());
                npc.addToServer();
                c.getPlayer().dropMessage(-11, "完成");
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage(-11, "製作NPC失敗... : " + e.getMessage());
            }
            return 1;
        }
    }

    public static class removePlayerNPC extends 移除玩家NPC {

    }

    public static class 移除玩家NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <NPCoid>");
                return 0;
            }
            try {
                c.getPlayer().dropMessage(-11, "正在移除玩家NPC...");
                final MapleNPC npc = c.getPlayer().getMap().getNPCByOid(Integer.parseInt(splitted[1]));
                if (npc instanceof PlayerNPC) {
                    ((PlayerNPC) npc).destroy(true);
                    c.getPlayer().dropMessage(-11, "完成");
                } else {
                    c.getPlayer().dropMessage(-11, splitted[0] + " <NPCoid>");
                }
            } catch (NumberFormatException e) {
                c.getPlayer().dropMessage(-11, "移除NPC失敗... : " + e.getMessage());
            }
            return 1;
        }
    }

    public static class ServerMessage extends 伺服器訊息 {

    }

    public static class 伺服器訊息 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.setServerMessage(c.getWorld(), outputMessage);
            }
            return 1;
        }
    }

    public static class SendPacket extends 發送數據包 {

    }

    public static class 發送數據包 extends CommandExecute {

        protected static StringBuilder builder = new StringBuilder();

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (builder.length() > 1) {
                c.getSession().writeAndFlush(CField.getPacketFromHexString(builder.toString()));
                builder = new StringBuilder();
            } else {
                c.getPlayer().dropMessage(-11, "請輸入數據包數據");
            }
            return 1;
        }
    }

    public static class addPacket extends 添加數據 {

    }

    public static class 添加數據 extends 發送數據包 {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                builder.append(StringUtil.joinStringFrom(splitted, 1));
                c.getPlayer().dropMessage(-11, "當前數據包數據: " + builder.toString());
            } else {
                c.getPlayer().dropMessage(-11, "請輸入數據包數據");
            }
            return 1;
        }
    }

    public static class CreatePacket extends 創建數據包 {

    }

    public static class 創建數據包 extends 發送數據包 {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            builder = new StringBuilder();
            return 1;
        }
    }

    public static class Packet extends 數據包 {

    }

    public static class 數據包 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                c.getSession().writeAndFlush(CField.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
            } else {
                c.getPlayer().dropMessage(-11, "請輸入數據包數據");
            }
            return 1;
        }
    }

    public static class P extends 數據包 {
    }

    public static class PacketToServer extends 數據包給服務端 {

    }

    public static class 數據包給服務端 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                try {
                    c.getSession().pipeline()
                            .fireChannelRead(CField.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
                } catch (Exception e) {
                    c.getPlayer().dropMessage(-11, "錯誤: " + e);
                }
            } else {
                c.getPlayer().dropMessage(-11, "請輸入數據包數據");
            }
            return 1;
        }
    }

    public static class PTS extends 數據包給服務端 {
    }

    public static class reloadMap extends 重載地圖 {
    }

    public static class 重載地圖 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <地圖ID>");
                return 0;
            }
            final int mapId = Integer.parseInt(splitted[1]);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId)
                        && cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0) {
                    c.getPlayer().dropMessage(5, "目標地圖的" + cserv.getChannel() + "頻道有角色在,無法重載");
                    return 0;
                }
            }
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId)) {
                    cserv.getMapFactory().removeMap(mapId);
                }
            }
            return 1;
        }
    }

    public static class respawn extends 生怪 {
    }

    public static class 生怪 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().respawn(true);
            return 1;
        }
    }

    public abstract static class TestTimer extends CommandExecute {

        protected Timer toTest = null;

        @Override
        public int execute(final MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <時間:秒>");
                return 0;
            }
            final int sec = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(5, "Message will pop up in " + sec + " seconds.");
            c.getPlayer().dropMessage(5, "Active: " + toTest.getSES().getActiveCount() + " Core: "
                    + toTest.getSES().getCorePoolSize() + " Largest: " + toTest.getSES().getLargestPoolSize() + " Max: "
                    + toTest.getSES().getMaximumPoolSize() + " Current: " + toTest.getSES().getPoolSize() + " Status: "
                    + toTest.getSES().isShutdown() + toTest.getSES().isTerminated() + toTest.getSES().isTerminating());
            final long oldMillis = System.currentTimeMillis();
            toTest.schedule(new Runnable() {
                @Override
                public void run() {
                    c.getPlayer().dropMessage(5,
                            "Message has popped up in " + ((System.currentTimeMillis() - oldMillis) / 1000)
                            + " seconds, expected was " + sec + " seconds");
                    c.getPlayer().dropMessage(5, "Active: " + toTest.getSES().getActiveCount() + " Core: "
                            + toTest.getSES().getCorePoolSize() + " Largest: " + toTest.getSES().getLargestPoolSize()
                            + " Max: " + toTest.getSES().getMaximumPoolSize() + " Current: "
                            + toTest.getSES().getPoolSize() + " Status: " + toTest.getSES().isShutdown()
                            + toTest.getSES().isTerminated() + toTest.getSES().isTerminating());
                }
            }, sec * 1000);
            return 1;
        }
    }

    public static class TestEventTimer extends TestTimer {

        public TestEventTimer() {
            toTest = EventTimer.getInstance();
        }
    }

    public static class TestCloneTimer extends TestTimer {

        public TestCloneTimer() {
            toTest = CloneTimer.getInstance();
        }
    }

    public static class TestEtcTimer extends TestTimer {

        public TestEtcTimer() {
            toTest = EtcTimer.getInstance();
        }
    }

    public static class TestMapTimer extends TestTimer {

        public TestMapTimer() {
            toTest = MapTimer.getInstance();
        }
    }

    public static class TestWorldTimer extends TestTimer {

        public TestWorldTimer() {
            toTest = WorldTimer.getInstance();
        }
    }

    public static class TestBuffTimer extends TestTimer {

        public TestBuffTimer() {
            toTest = BuffTimer.getInstance();
        }
    }

    public static class Crash extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null && c.getPlayer().getGmLevel() >= victim.getGmLevel()) {
                victim.getClient().getSession().writeAndFlush(HexTool.getByteArrayFromHexString("1A 00")); // give_buff
                // with no
                // data :D
                return 1;
            } else {
                c.getPlayer().dropMessage(-11, "受害者不存在");
                return 0;
            }
        }
    }

    /*
     * public static class ReloadIPMonitor extends CommandExecute {
     * 
     * @Override public int execute(MapleClient c, String[] splitted) {
     * MapleServerHandler.reloadLoggedIPs(); return 1; } }
     * 
     * public static class AddIPMonitor extends CommandExecute {
     * 
     * @Override public int execute(MapleClient c, String[] splitted) {
     * MapleServerHandler.addIP(splitted[1]); return 1; } }
     */
    public static class FillBook extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (int e : MapleItemInformationProvider.getInstance().getMonsterBook().keySet()) {
                c.getPlayer().getMonsterBook().getCards().put(e, 2);
            }
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "Done.");
            return 1;
        }
    }

    public static class ListBook extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final List<Entry<Integer, Integer>> mbList = new ArrayList<>(
                    MapleItemInformationProvider.getInstance().getMonsterBook().entrySet());
            Collections.sort(mbList, new BookComparator());
            final int page = Integer.parseInt(splitted[1]);
            for (int e = (page * 8); e < Math.min(mbList.size(), (page + 1) * 8); e++) {
                c.getPlayer().dropMessage(-11, e + ": " + mbList.get(e).getKey() + " - " + mbList.get(e).getValue());
            }

            return 0;
        }

        public static class BookComparator implements Comparator<Entry<Integer, Integer>>, Serializable {

            @Override
            public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
                if (o1.getValue() > o2.getValue()) {
                    return 1;
                } else if (Objects.equals(o1.getValue(), o2.getValue())) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    }

    public static class Subcategory extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSubcategory(Byte.parseByte(splitted[1]));
            return 1;
        }
    }

    public static class gainMaxMesos extends 最大楓幣 {
    }

    public static class 最大楓幣 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱>");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            victim.gainMeso(FeaturesConfig.MESO_MAX - c.getPlayer().getMeso(), true);
            return 1;
        }
    }

    public static class giveMeso extends 給予楓幣 {
    }

    public static class 給予楓幣 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <金額>");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            victim.gainMeso(Long.parseLong(splitted[2]), true);
            return 1;
        }
    }

    public static class giveBeanFun extends 給予樂豆點 {
    }

    public static class 給予樂豆點 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <點數>");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            victim.modifyCSPoints(1, Integer.parseInt(splitted[2]), true);
            return 1;
        }
    }

    public static class SSO extends 設置伺服端包頭 {

    }

    public static class SetServerOpcode extends 設置伺服端包頭 {

    }

    public static class 設置伺服端包頭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <包名> <包頭值>");
                return 0;
            }
            SendPacketOpcode.valueOf(splitted[1]).setValue(Short.parseShort(splitted[2]));
            return 1;
        }
    }

    public static class SRO extends 設置用戶端包頭 {

    }

    public static class SetRecvOpcode extends 設置用戶端包頭 {

    }

    public static class 設置用戶端包頭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <包名> <包頭值>");
                return 0;
            }
            RecvPacketOpcode.valueOf(splitted[1]).setValue(Short.parseShort(splitted[2]));
            return 1;
        }
    }

    public static class reloadskills extends 重載技能 {
    }

    public static class 重載技能 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(-11, "開始重載技能...");
            SkillFactory.load(true);
            c.getPlayer().dropMessage(-11, "重載技能完成!!");
            return 1;
        }
    }

    public static class reloaddrops extends 重載掉寶 {
    }

    public static class 重載掉寶 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            return 1;
        }
    }

    public static class reloadportals extends 重載傳送點 {
    }

    public static class 重載傳送點 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            PortalScriptManager.getInstance().clearScripts();
            return 1;
        }
    }

    public static class reloadshops extends 重載商店 {
    }

    public static class 重載商店 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory.getInstance().clear();
            return 1;
        }
    }

    public static class reloadevents extends 重載事件 {
    }

    public static class 重載事件 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
            return 1;
        }
    }

    public static class resetMap extends 重置地圖 {
    }

    public static class 重置地圖 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetFully();
            return 1;
        }
    }

    public static class ResetQuest extends 重置任務 {

    }

    public static class 重置任務 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <任務ID>");
                return 0;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
            return 1;
        }
    }

    public static class StartQuest extends 開始任務 {

    }

    public static class 開始任務 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <任務ID> <NPCID>");
                return 0;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class CompleteQuest extends 完成任務 {

    }

    public static class 完成任務 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <任務ID> <NPCID> <selection>");
                return 0;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]),
                    Integer.parseInt(splitted[3]));
            return 1;
        }
    }

    public static class StartPacketQuest extends 開始數據任務 {

    }

    public static class 開始數據任務 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <任務ID> <NPCID> (customData:默認空)");
                return 0;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(),
                    Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
            return 1;
        }
    }

    public static class CompletePacketQuest extends 完成數據任務 {

    }

    public static class 完成數據任務 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <任務ID> <NPCID>");
                return 0;
            }
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }

    public static class hitReactor extends 攻擊反應堆 {

    }

    public static class 攻擊反應堆 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <反應堆OID>");
                return 0;
            }
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
            return 1;
        }
    }

    public static class HitScriptReactor extends 攻擊腳本反應堆 {

    }

    public static class 攻擊腳本反應堆 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <反應堆OID>");
                return 0;
            }
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).forceHitReactor(c.getPlayer(),
                    Byte.parseByte(splitted[2]));
            return 1;
        }
    }

    public static class crashReactor extends 破壞反應堆 {
    }

    public static class 破壞反應堆 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <全部/反應堆名稱>");
                return 0;
            }
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(),
                    Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.REACTOR));
            if (splitted[1].equals("全部")) {
                for (MapleMapObject reactorL : reactors) {
                    MapleReactor reactor2l = (MapleReactor) reactorL;
                    c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
                }
            } else {
                c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
            }
            return 1;
        }
    }

    public static class setReactor extends 設置反應堆 {
    }

    public static class 設置反應堆 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <ReactorID>");
                return 0;
            }
            c.getPlayer().getMap().setReactorState(Byte.parseByte(splitted[1]));
            return 1;
        }
    }

    public static class resetReactor extends 重置反應堆 {
    }

    public static class 重置反應堆 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetReactors();
            return 1;
        }
    }

    public static class note extends 發送留言 {
    }

    public static class 發送留言 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (splitted.length >= 2) {
                String text = StringUtil.joinStringFrom(splitted, 1);
                c.getPlayer().sendNote(victim.getName(), text);
            } else {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <內容>");
                return 0;
            }
            return 1;
        }
    }

    public static class noteall extends 給所有人發送留言 {
    }

    public static class 給所有人發送留言 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (splitted.length >= 1) {
                String text = StringUtil.joinStringFrom(splitted, 1);
                for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    c.getPlayer().sendNote(mch.getName(), text);
                }
            } else {
                c.getPlayer().dropMessage(-11, splitted[0] + " <內容>");
                return 0;
            }
            return 1;
        }
    }

    public static class SkillEffect extends 技能增益 {

    }

    public static class 技能增益 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <技能ID> <技能等級>");
                return 0;
            }
            SkillFactory.getSkill(Integer.parseInt(splitted[1])).getEffect(Integer.parseInt(splitted[2]))
                    .applyTo(c.getPlayer());
            return 0;
        }
    }

    public static class ItemEffect extends 道具增益 {

    }

    public static class 道具增益 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <道具ID>");
                return 0;
            }
            MapleItemInformationProvider.getInstance().getItemEffect(Integer.parseInt(splitted[1]))
                    .applyTo(c.getPlayer());
            return 0;
        }
    }

    public static class ItemEffectEx extends 道具增益EX {

    }

    public static class 道具增益EX extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <道具ID>");
                return 0;
            }
            MapleItemInformationProvider.getInstance().getItemEffectEX(Integer.parseInt(splitted[1]))
                    .applyTo(c.getPlayer());
            return 0;
        }
    }

    public static class CancelSkillEffect extends 取消技能增益 {

    }

    public static class 取消技能增益 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dispelBuff(Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class MapSkillEffect extends 全圖技能增益 {

    }

    public static class 全圖技能增益 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <技能ID> <技能等級>");
                return 0;
            }
            for (MapleCharacter mch : c.getPlayer().getMap().getCharacters()) {
                SkillFactory.getSkill(Integer.parseInt(splitted[1])).getEffect(Integer.parseInt(splitted[2]))
                        .applyTo(mch);
            }
            return 0;
        }
    }

    public static class MapItemEffect extends 全圖道具增益 {

    }

    public static class 全圖道具增益 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <道具ID>");
                return 0;
            }
            for (MapleCharacter mch : c.getPlayer().getMap().getCharacters()) {
                MapleItemInformationProvider.getInstance().getItemEffect(Integer.parseInt(splitted[1])).applyTo(mch);
            }
            return 0;
        }
    }

    public static class MapItemEffectEX extends 全圖道具增益EX {

    }

    public static class 全圖道具增益EX extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <道具ID>");
                return 0;
            }
            for (MapleCharacter mch : c.getPlayer().getMap().getCharacters()) {
                MapleItemInformationProvider.getInstance().getItemEffectEX(Integer.parseInt(splitted[1])).applyTo(mch);
            }
            return 0;
        }
    }

    public static class MapItemSize extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(-11,
                    "Number of items: " + MapleItemInformationProvider.getInstance().getAllItems().size());
            return 0;
        }
    }

    public static class openUIOption extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getSession().writeAndFlush(CField.UIPacket.openUIOption(Integer.parseInt(splitted[1]), 9010000));
            return 1;
        }
    }

    public static class openUIWindow extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getSession().writeAndFlush(CField.UIPacket.openUI(Integer.parseInt(splitted[1])));
            return 1;
        }
    }

    public static class reloadGach extends 重載轉蛋機數據 {
    }

    public static class 重載轉蛋機數據 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleGachapon.reloadItems();
            c.getPlayer().dropMessage(5, "轉蛋機資料重載完成。");
            return 1;
        }
    }
}
