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
package scripting;

import client.skill.Skill;
import client.skill.SkillFactory;
import client.*;
import client.MapleTrait.MapleTraitType;
import client.inventory.*;
import constants.GameConstants;
import constants.ItemConstants;
import extensions.temporary.FieldEffectOpcode;
import extensions.temporary.ScriptMessageType;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.exped.MapleExpedition;
import handling.world.guild.MapleGuild;
import java.awt.Point;
import java.util.LinkedList;
import java.util.List;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.Timer.EventTimer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.*;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.PetPacket;
import extensions.temporary.UserEffectOpcode;
import handling.channel.handler.InventoryHandler;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import server.StructSetItem;
import tools.StringUtil;
import tools.packet.CField.CScriptMan;

public abstract class AbstractPlayerInteraction {

    protected MapleClient c;
    protected int id;
    protected int id2;
    protected String script;

    public AbstractPlayerInteraction(MapleClient c) {
        this.c = c;
        id = 0;
        id2 = 0;
        script = null;
    }

    public AbstractPlayerInteraction(final MapleClient c, final int id, final int id2, final String script) {
        this.c = c;
        this.id = id;
        this.id2 = id2;
        this.script = script;
    }

    public final MapleClient getClient() {
        return c;
    }

    public final MapleClient getC() {
        return c;
    }

    public MapleCharacter getChar() {
        return c.getPlayer();
    }

    public final ChannelServer getChannelServer() {
        return c.getChannelServer();
    }

    public final MapleCharacter getPlayer() {
        return c.getPlayer();
    }

    public final EventManager getEventManager(final String event) {
        return c.getChannelServer().getEventSM().getEventManager(event);
    }

    public final EventInstanceManager getEventInstance() {
        return c.getPlayer().getEventInstance();
    }

    public void unequipEverything() {
        MapleInventory equipped = getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<Short> ids = new LinkedList<>();
        for (Item item : equipped.newList()) {
            ids.add(item.getPosition());
        }
        for (short itemid : ids) {
            MapleInventoryManipulator.unequip(getC(), itemid, equip.getNextFreeSlot());
        }
    }

    public void equip(int itemId) {
        c.getPlayer().equip(itemId);
    }

    public void equip(int itemId, boolean replace) {
        c.getPlayer().equip(itemId, replace);
    }

    public void equip(int itemId, boolean replace, boolean add) {
        c.getPlayer().equip(itemId, replace, add);
    }

    public void unequip(int itemId) {
        c.getPlayer().unequip(itemId);
    }

    public void unequip(int itemId, boolean remove) {
        c.getPlayer().unequip(itemId, remove);
    }

    public final void warp(final int map) {
        final MapleMap mapz = getWarpMap(map);
        try {
            c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp_Instanced(final int map) {
        final MapleMap mapz = getMap_Instanced(map);
        try {
            c.getPlayer().changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
        } catch (Exception e) {
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public final void warp(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        if (portal != 0 && map == c.getPlayer().getMapId()) { // test
            final Point portalPos = new Point(c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { // estimation
                c.announce(CField.instantMapWarp((byte) portal)); // until we get packet for far movement, this will do
                c.getPlayer().checkFollow();
                c.getPlayer().getMap().movePlayer(c.getPlayer(), portalPos);
            } else {
                c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public final void warpS(final int map, final int portal) {
        final MapleMap mapz = getWarpMap(map);
        c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warp(final int map, String portal) {
        final MapleMap mapz = getWarpMap(map);
        if (map == 109060000 || map == 109060002 || map == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        if (map == c.getPlayer().getMapId()) { // test
            final Point portalPos = new Point(c.getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { // estimation
                c.getPlayer().checkFollow();
                c.announce(CField.instantMapWarp((byte) c.getPlayer().getMap().getPortal(portal).getId()));
                c.getPlayer().getMap().movePlayer(c.getPlayer(),
                        new Point(c.getPlayer().getMap().getPortal(portal).getPosition()));
            } else {
                c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public final void warpS(final int map, String portal) {
        final MapleMap mapz = getWarpMap(map);
        if (map == 109060000 || map == 109060002 || map == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        c.getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public final void warpMap(final int mapid, final int portal) {
        final MapleMap map = getMap(mapid);
        for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
            chr.changeMap(map, map.getPortal(portal));
        }
    }

    public final void warpByName(final int mapid, final String chrname) {
        MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(chrname);
        if (chr == null) {
            c.getPlayer().dropMessage(1, "Could not find the character.");
            c.announce(CWvsContext.enableActions());
            return;
        }
        final MapleMap mapz = getWarpMap(mapid);
        try {
            chr.changeMap(mapz, mapz.getPortal(Randomizer.nextInt(mapz.getPortals().size())));
            chr.getClient().removeClickedNPC();
            NPCScriptManager.getInstance().dispose(chr.getClient());
            chr.getClient().announce(CWvsContext.enableActions());
        } catch (Exception e) {
            chr.changeMap(mapz, mapz.getPortal(0));
            chr.getClient().removeClickedNPC();
            NPCScriptManager.getInstance().dispose(chr.getClient());
            chr.getClient().announce(CWvsContext.enableActions());
        }
    }

    public final void mapChangeTimer(final int map, final int nextmap, final int time, final boolean notice) {
        final List<MapleCharacter> current = c.getChannelServer().getMapFactory().getMap(map).getCharacters();
        c.getChannelServer().getMapFactory().getMap(map).broadcastMessage(CField.getClock(time));
        if (notice) {
            c.getChannelServer().getMapFactory().getMap(map)
                    .startMapEffect("You will be moved out of the map when the timer ends.", 5120041);
        }
        EventTimer.getInstance().schedule(() -> {
            if (current != null) {
                for (MapleCharacter chrs : current) {
                    chrs.changeMap(nextmap, 0);
                }
            }
        }, time * 1000); // seconds
    }

    public final void playPortalSE() {
        c.announce(CField.EffectPacket.showEffect(true, c.getPlayer(), UserEffectOpcode.UserEffect_PlayPortalSE, null,
                null, null, null));
    }

    private MapleMap getWarpMap(final int map) {
        return ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(map);
    }

    public final MapleMap getMap() {
        return c.getPlayer().getMap();
    }

    public final MapleMap getMap(final int map) {
        return getWarpMap(map);
    }

    public final MapleMap getMap_Instanced(final int map) {
        return c.getPlayer().getEventInstance() == null ? getMap(map)
                : c.getPlayer().getEventInstance().getMapInstance(map);
    }

    public void spawnMonster(final int id, final int qty) {
        spawnMob(id, qty, c.getPlayer().getTruePosition());
    }

    public final void spawnMobOnMap(final int id, final int qty, final int x, final int y, final int map) {
        for (int i = 0; i < qty; i++) {
            getMap(map).spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), new Point(x, y));
        }
    }

    public final void spawnMob(final int id, final int qty, final int x, final int y) {
        spawnMob(id, qty, new Point(x, y));
    }

    public final void spawnMob(final int id, final int x, final int y) {
        spawnMob(id, 1, new Point(x, y));
    }

    private void spawnMob(final int id, final int qty, final Point pos) {
        for (int i = 0; i < qty; i++) {
            c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public final void killMob(int ids) {
        c.getPlayer().getMap().killMonster(ids);
    }

    public final void killAllMob() {
        c.getPlayer().getMap().killAllMonsters(true);
    }

    public final void addHP(final int delta) {
        c.getPlayer().addHP(delta);
    }

    public final int getPlayerStat(final String type) {
        switch (type) {
            case "LVL":
                return c.getPlayer().getLevel();
            case "STR":
                return c.getPlayer().getStat().getStr();
            case "DEX":
                return c.getPlayer().getStat().getDex();
            case "INT":
                return c.getPlayer().getStat().getInt();
            case "LUK":
                return c.getPlayer().getStat().getLuk();
            case "HP":
                return c.getPlayer().getStat().getHp();
            case "MP":
                return c.getPlayer().getStat().getMp();
            case "MAXHP":
                return c.getPlayer().getStat().getMaxHp();
            case "MAXMP":
                return c.getPlayer().getStat().getMaxMp();
            case "RAP":
                return c.getPlayer().getRemainingAp();
            case "RSP":
                return c.getPlayer().getRemainingSp();
            case "GID":
                return c.getPlayer().getGuildId();
            case "GRANK":
                return c.getPlayer().getGuildRank();
            case "ARANK":
                return c.getPlayer().getAllianceRank();
            case "GM":
                return c.getPlayer().isGM() ? 1 : 0;
            case "ADMIN":
                return c.getPlayer().isAdmin() ? 1 : 0;
            case "GENDER":
                return c.getPlayer().getGender();
            case "FACE":
                return c.getPlayer().getFace();
            case "HAIR":
                return c.getPlayer().getHair();
        }
        return -1;
    }

    public final void setGender(byte gender) {
        c.getPlayer().setGender(gender);
        c.getPlayer().updateSingleStat(MapleStat.GENDER, gender);
        c.getPlayer().equipChanged();
    }

    public final String getName() {
        return c.getPlayer().getName();
    }

    public final boolean haveItem(final int itemid) {
        return haveItem(itemid, 1);
    }

    public final boolean haveItem(final int itemid, final int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public final boolean haveItem(final int itemid, final int quantity, final boolean checkEquipped,
            final boolean greaterOrEquals) {
        return c.getPlayer().haveItem(itemid, quantity, checkEquipped, greaterOrEquals);
    }

    public final boolean canHold() {
        for (int i = 1; i <= 5; i++) {
            if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).getNextFreeSlot() <= -1) {
                return false;
            }
        }
        return true;
    }

    public final boolean canHoldSlots(final int slot) {
        for (int i = 1; i <= 5; i++) {
            if (c.getPlayer().getInventory(MapleInventoryType.getByType((byte) i)).isFull(slot)) {
                return false;
            }
        }
        return true;
    }

    public final boolean canHold(final int itemid) {
        return c.getPlayer().getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public final boolean canHold(final int itemid, final int quantity) {
        return MapleInventoryManipulator.checkSpace(c, itemid, quantity, "");
    }

    public final MapleQuestStatus getQuestRecord(final int id) {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(id));
    }

    public final MapleQuestStatus getQuestNoRecord(final int id) {
        return c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(id));
    }

    public final byte getQuestStatus(final int id) {
        return c.getPlayer().getQuestStatus(id);
    }

    public final boolean isQuestActive(final int id) {
        return getQuestStatus(id) == 1;
    }

    public final boolean isQuestFinished(final int id) {
        return getQuestStatus(id) == 2;
    }

    public final void showQuestMsg(final String msg) {
        c.announce(CWvsContext.showQuestMsg(msg));
    }

    public void startQuest(int questId, int npcid) {
        MapleQuest.getInstance(questId).start(getPlayer(), npcid);
    }

    public final void removeQuestStatus(int questId) {
        c.getPlayer().getQuestRemove(MapleQuest.getInstance(questId));
    }

    public final void forceStartQuest(final int id, final String data) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, data);
    }

    public final void forceStartQuest(final int id, final int data, final boolean filler) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, filler ? String.valueOf(data) : null);
    }

    public void forceStartQuest(final int id) {
        MapleQuest.getInstance(id).forceStart(c.getPlayer(), 0, null);
    }

    public void forceCompleteQuest(final int id) {
        MapleQuest.getInstance(id).forceComplete(getPlayer(), 0);
    }

    public void spawnNpc(final int npcId) {
        c.getPlayer().getMap().spawnNpc(npcId, c.getPlayer().getPosition());
    }

    public final void spawnNpc(final int npcId, final int x, final int y) {
        c.getPlayer().getMap().spawnNpc(npcId, new Point(x, y));
    }

    public final void spawnNpc(final int npcId, final Point pos) {
        c.getPlayer().getMap().spawnNpc(npcId, pos);
    }

    public final void spawnNpcForPlayer(final int npcId, final int x, final int y) {
        c.getPlayer().getMap().spawnNpcForPlayer(c, npcId, new Point(x, y));
    }

    public final void removeNpc(final int mapid, final int npcId) {
        c.getChannelServer().getMapFactory().getMap(mapid).removeNpc(npcId);
    }

    public final void removeNpc(final int npcId) {
        c.getPlayer().getMap().removeNpc(npcId);
    }

    public final void hideNpc(final int npcId) {
        c.getPlayer().getMap().hideNpc(npcId);
    }

    public final void respawn(final boolean force) {
        c.getPlayer().getMap().respawn(force);
    }

    public final void forceStartReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.forceStartReactor(c);
                break;
            }
        }
    }

    public final void destroyReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(c);
                break;
            }
        }
    }

    public final void hitReactor(final int mapid, final int id) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        MapleReactor react;

        for (final MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == id) {
                react.hitReactor(c);
                break;
            }
        }
    }

    public final int getJob() {
        return c.getPlayer().getJob();
    }

    public final void gainNX(final int amount) {
        c.getPlayer().modifyCSPoints(1, amount, true);
    }

    public final void gainItemPeriod(final int id, final short quantity, final int period) { // period is in days
        gainItem(id, quantity, false, period, false, -1, "");
    }

    public final void gainItemPeriod(final int id, final short quantity, final long period, final String owner) { // period
        // is
        // in
        // days
        gainItem(id, quantity, false, period, false, -1, owner);
    }

    public final void gainItemPeriod(final int id, final short quantity, final int period, boolean hours) { // period is
        // in days
        gainItem(id, quantity, false, period, hours, -1, "");
    }

    public final void gainItemPeriod(final int id, final short quantity, final long period, boolean hours,
            final String owner) { // period is in days
        gainItem(id, quantity, false, period, hours, -1, owner);
    }

    public final void gainItem(final int id, final short quantity) {
        gainItem(id, quantity, false, 0, false, -1, "");
    }

    public final void gainItemSilent(final int id, final short quantity) {
        gainItem(id, quantity, false, 0, false, -1, "", c, false);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats) {
        gainItem(id, quantity, randomStats, 0, false, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final int slots) {
        gainItem(id, quantity, randomStats, 0, false, slots, "");
    }

    public final void gainItem(final int id, final short quantity, final long period) {
        gainItem(id, quantity, false, period, false, -1, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period,
            final int slots) {
        gainItem(id, quantity, randomStats, period, false, slots, "");
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period,
            boolean hours, final int slots, final String owner) {
        gainItem(id, quantity, randomStats, period, hours, slots, owner, c);
    }

    public final void gainItem(final int id, final short quantity, final boolean randomStats, final long period,
            boolean hours, final int slots, final String owner, final MapleClient cg) {
        gainItem(id, quantity, randomStats, period, hours, slots, owner, cg, true);
    }

    // public final void gainItem(final int id, final short quantity, final boolean
    // randomStats, final long period, boolean hours, final int slots, boolean
    // potential, final String owner) {
    // gainItem(id, quantity, randomStats, period, hours, slots, potential, owner,
    // c);
    // }
    public final void gainItem(final int id, final short quantity, final boolean randomStats, long period,
            boolean hours, final int slots, final String owner, final MapleClient cg, final boolean show) {
        if (quantity >= 0) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final MapleInventoryType type = GameConstants.getInventoryType(id);

            if (!MapleInventoryManipulator.checkSpace(cg, id, quantity, "")) {
                return;
            }
            if (type.equals(MapleInventoryType.EQUIP) && !ItemConstants.類型.可充值道具(id)) {
                Equip item = (Equip) ii.getEquipById(id);
                if (randomStats) {
                    item = ii.randomizeStats(item);
                }
                if (period > 0) {
                    item.setExpiration(System.currentTimeMillis() + (period * (hours ? 1 : 24) * 60 * 60 * 1000));
                }
                if (slots > 0) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() + slots));
                }
                if (owner != null) {
                    item.setOwner(owner);
                }
                item.setGMLog("從腳本 " + this.id + "(" + id2 + ")[" + script + "] 中獲得, 時間 "
                        + FileoutputUtil.CurrentReadable_Time());
                final String name = ii.getName(id);
                if (id / 10000 == 114 && name != null && name.length() > 0) { // medal
                    final String msg = "<" + name + ">獲得稱號。";
                    cg.getPlayer().dropMessage(-1, msg);
                    cg.getPlayer().dropMessage(5, msg);
                }
                item.setUniqueId(MapleInventoryManipulator.getUniqueId(item.getItemId(), null));
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                final MaplePet pet;
                if (ItemConstants.類型.寵物(id)) {
                    pet = MaplePet.createPet(id);
                    if (period == 0) {
                        period = ii.getLife(id) * (hours ? 24 : 1);
                    }
                } else {
                    pet = null;
                }
                MapleInventoryManipulator.addById(cg, id, quantity, owner == null ? "" : owner, pet, period,
                        hours ? MapleInventoryManipulator.HOUR : MapleInventoryManipulator.DAY, "從腳本 " + this.id + "("
                        + id2 + ")[" + script + "] 中獲得, 時間 " + FileoutputUtil.CurrentReadable_Time());
            }
        } else {
            MapleInventoryManipulator.removeById(cg, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        if (show) {
            cg.announce(InfoPacket.getShowItemGain(id, quantity, true));
        }
    }

    public final boolean removeItem(final int id) { // quantity 1
        if (MapleInventoryManipulator.removeById_Lock(c, GameConstants.getInventoryType(id), id)) {
            c.announce(InfoPacket.getShowItemGain(id, (short) -1, true));
            return true;
        }
        return false;
    }

    public final void worldMessage(final int type, final String message) {
        World.Broadcast.broadcastMessage(CWvsContext.broadcastMsg(type, message));
    }

    // default playerMessage and mapMessage to use type 5
    public final void playerMessage(final String message) {
        playerMessage(5, message);
    }

    public final void mapMessage(final String message) {
        mapMessage(5, message);
    }

    public final void guildMessage(final String message) {
        guildMessage(5, message);
    }

    public final void playerMessage(final int type, final String message) {
        c.getPlayer().dropMessage(type, message);
    }

    public final void mapMessage(final int type, final String message) {
        c.getPlayer().getMap().broadcastMessage(CWvsContext.broadcastMsg(type, message));
    }

    public final void guildMessage(final int type, final String message) {
        if (getPlayer().getGuildId() > 0) {
            World.Guild.guildPacket(getPlayer().getGuildId(), CWvsContext.broadcastMsg(type, message));
        }
    }

    public final MapleGuild getGuild() {
        return getGuild(getPlayer().getGuildId());
    }

    public final MapleGuild getGuild(int guildid) {
        return World.Guild.getGuild(guildid);
    }

    public final MapleParty getParty() {
        return c.getPlayer().getParty();
    }

    public final int getCurrentPartyId(int mapid) {
        return getMap(mapid).getCurrentPartyId();
    }

    public final boolean isLeader() {
        if (getPlayer().getParty() == null) {
            return false;
        }
        return getParty().getLeader().getId() == c.getPlayer().getId();
    }

    public void partyMessage(int type, String msg) {
        for (MaplePartyCharacter mem : this.c.getPlayer().getParty().getMembers()) {
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterById(mem.getId());
            chr.dropMessage(type, msg);
        }
    }

    public final boolean isAllPartyMembersAllowedJob(final int job) {
        return getNotAllowedJobMembers(job).isEmpty();
    }

    public final String getNotAllowedJobMembers(final int job) {
        if (c.getPlayer().getParty() == null) {
            return c.getPlayer().getJob() / 100 != job ? c.getPlayer().getName() : "";
        }
        String string = "";
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            if (mem.getJobId() / 100 != job) {
                string += mem.getName() + ",";
            }
        }
        if (!string.isEmpty()) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public final String getNotAllowedLevelMembers(final int min, final int max) {
        if (c.getPlayer().getParty() == null) {
            return c.getPlayer().getLevel() < min || c.getPlayer().getLevel() > max ? c.getPlayer().getName() : "";
        }
        String string = "";
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            if (mem.getLevel() < min || mem.getLevel() > max) {
                string += mem.getName() + ",";
            }
        }
        if (!string.isEmpty()) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public final String getNotAllowedPQMembers(final String pqName, int times, boolean week) {
        if (c.getPlayer().getParty() == null) {
            return c.getPlayer().getPQLog(pqName) >= times ? c.getPlayer().getName() : "";
        }
        String string = "";
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterById(mem.getId());
            if (chr == null || chr.getWeekPQLog(pqName, week ? Calendar.THURSDAY : 1, week) >= times) {
                string += mem.getName() + ",";
            }
        }
        if (!string.isEmpty()) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public final String getNotFinishQuestMembers(final int quest) {
        if (c.getPlayer().getParty() == null || quest == 0) {
            return quest != 0 && c.getPlayer().getQuestStatus(quest) != 2 ? c.getPlayer().getName() : "";
        }
        String string = "";
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterById(mem.getId());
            if (chr == null || chr.getQuestStatus(quest) != 2) {
                string += mem.getName() + ",";
            }
        }
        if (!string.isEmpty()) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public final String getNotHaveItemMembers(final int itemId) {
        if (c.getPlayer().getParty() == null || itemId == 0) {
            return itemId != 0 && !c.getPlayer().haveItem(itemId) ? c.getPlayer().getName() : "";
        }
        String string = "";
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterById(mem.getId());
            if (chr == null || !chr.haveItem(itemId)) {
                string += mem.getName() + ",";
            }
        }
        if (!string.isEmpty()) {
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }

    public final void gainMembersPQ(final String pqName, int num) {
        if (c.getPlayer().getParty() == null) {
            c.getPlayer().setPQLog(pqName, 0, num);
            return;
        }
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterById(mem.getId());
            if (chr != null) {
                chr.setPQLog(pqName, 0, num);
            }
        }
    }

    public final boolean allMembersHere() {
        if (c.getPlayer().getParty() == null) {
            return false;
        }
        for (final MaplePartyCharacter mem : c.getPlayer().getParty().getMembers()) {
            final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(mem.getId());
            if (chr == null) {
                return false;
            }
        }
        return true;
    }

    public final void warpParty(final int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp(mapId, 0);
            return;
        }
        final MapleMap target = getMap(mapId);
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public final void warpParty(final int mapId, final int portal) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            if (portal < 0) {
                warp(mapId);
            } else {
                warp(mapId, portal);
            }
            return;
        }
        final boolean rand = portal < 0;
        final MapleMap target = getMap(mapId);
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                if (rand) {
                    try {
                        curChar.changeMap(target, target.getPortal(Randomizer.nextInt(target.getPortals().size())));
                    } catch (Exception e) {
                        curChar.changeMap(target, target.getPortal(0));
                    }
                } else {
                    curChar.changeMap(target, target.getPortal(portal));
                }
            }
        }
    }

    public final void warpParty_Instanced(final int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp_Instanced(mapId);
            return;
        }
        final MapleMap target = getMap_Instanced(mapId);

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    public void gainMeso(int gain) {
        c.getPlayer().gainMeso(gain, true, true);
    }

    public void gainExp(int gain) {
        c.getPlayer().gainExp(gain, true, true, true);
    }

    public void gainExpR(int gain) {
        c.getPlayer().gainExp(
                gain * c.getChannelServer().getExpRate(c.getPlayer() != null ? c.getPlayer().getWorld() : 0), true,
                true, true);
    }

    public void gainSp(final int amount) {
        c.getPlayer().gainSP(amount);
    }

    public void gainSp(final int amount, final int skillbook) {
        c.getPlayer().gainSP(amount, skillbook);
    }

    public final void givePartyItems(final int id, final short quantity, final List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(chr.getClient(), id, quantity,
                        "Received from party interaction " + id + " (" + id2 + ")");
            } else {
                MapleInventoryManipulator.removeById(chr.getClient(), GameConstants.getInventoryType(id), id, -quantity,
                        true, false);
            }
            chr.getClient().announce(InfoPacket.getShowItemGain(id, quantity, true));
        }
    }

    public void addPartyTrait(String t, int e, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.getTrait(MapleTraitType.valueOf(t)).addExp(e, chr);
        }
    }

    public void addPartyTrait(String t, int e) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            addTrait(t, e);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.getTrait(MapleTraitType.valueOf(t)).addExp(e, curChar);
            }
        }
    }

    public void addTrait(String t, int e) {
        getPlayer().getTrait(MapleTraitType.valueOf(t)).addExp(e, getPlayer());
    }

    public final void givePartyItems(final int id, final short quantity) {
        givePartyItems(id, quantity, false);
    }

    public final void givePartyItems(final int id, final short quantity, final boolean removeAll) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainItem(id, (short) (removeAll ? -getPlayer().itemQuantity(id) : quantity));
            return;
        }

        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                gainItem(id, (short) (removeAll ? -curChar.itemQuantity(id) : quantity), false, 0, false, 0, "",
                        curChar.getClient());
            }
        }
    }

    public final void givePartyExp_PQ(final int maxLevel, final double mod, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(
                    chr.getLevel() > maxLevel ? (maxLevel + ((maxLevel - chr.getLevel()) / 10)) : chr.getLevel())
                    / (Math.min(chr.getLevel(), maxLevel) / 5.0) / (mod * 2.0));
            chr.gainExp(amount * c.getChannelServer().getExpRate(chr.getWorld()), true, true, true);
        }
    }

    public final void gainExp_PQ(final int maxLevel, final double mod) {
        final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(
                getPlayer().getLevel() > maxLevel ? (maxLevel + (getPlayer().getLevel() / 10)) : getPlayer().getLevel())
                / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0) / mod);
        gainExp(amount * c.getChannelServer().getExpRate(c.getPlayer() != null ? c.getPlayer().getWorld() : 0));
    }

    public final void givePartyExp_PQ(final int maxLevel, final double mod) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            final int amount = (int) Math.round(GameConstants
                    .getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? (maxLevel + (getPlayer().getLevel() / 10))
                            : getPlayer().getLevel())
                    / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0) / mod);
            gainExp(amount * c.getChannelServer().getExpRate(c.getPlayer() != null ? c.getPlayer().getWorld() : 0));
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                final int amount = (int) Math.round(GameConstants.getExpNeededForLevel(
                        curChar.getLevel() > maxLevel ? (maxLevel + (curChar.getLevel() / 10)) : curChar.getLevel())
                        / (Math.min(curChar.getLevel(), maxLevel) / 10.0) / mod);
                curChar.gainExp(amount * c.getChannelServer().getExpRate(curChar.getWorld()), true, true, true);
            }
        }
    }

    public final void givePartyExp(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.gainExp(amount * c.getChannelServer().getExpRate(chr.getWorld()), true, true, true);
        }
    }

    public final void givePartyExp(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainExp(amount * c.getChannelServer().getExpRate(c.getPlayer() != null ? c.getPlayer().getWorld() : 0));
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.gainExp(amount * c.getChannelServer().getExpRate(curChar.getWorld()), true, true, true);
            }
        }
    }

    public void userSetFieldFloating(int i, int i0, int i1, int i2) {
        this.c.announce(CField.onUserSetFieldFloating(i, i0, i1, i2));
    }

    public final void endPartyQuest(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.endPartyQuest(amount);
        }
    }

    public final void endPartyQuest(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            getPlayer().endPartyQuest(amount);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.endPartyQuest(amount);
            }
        }
    }

    public final void removeFromParty(final int id, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            final int possesed = chr.getInventory(GameConstants.getInventoryType(id)).countById(id);
            if (possesed > 0) {
                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(id), id, possesed, true, false);
                chr.getClient().announce(InfoPacket.getShowItemGain(id, (short) -possesed, true));
            }
        }
    }

    public final void removeFromParty(final int id) {
        givePartyItems(id, (short) 0, true);
    }

    public final void useSkill(final int skill, final int level) {
        if (level <= 0) {
            return;
        }
        SkillFactory.getSkill(skill).getEffect(level).applyTo(c.getPlayer());
    }

    public final void dispelBuff(final int skillId) {
        c.getPlayer().dispelBuff(skillId);
    }

    public final void useItem(final int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(c.getPlayer());
        c.announce(InfoPacket.getStatusMsg(id));
    }

    public final void cancelItem(final int id) {
        c.getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(id), false, -1);
    }

    public final int getMorphState() {
        return c.getPlayer().getMorphState();
    }

    public final void removeAll(final int id) {
        c.getPlayer().removeAll(id);
    }

    public final void gainCloseness(final int closeness, final int index) {
        final MaplePet pet = getPlayer().getSummonedPet(index);
        if (pet != null) {
            pet.setCloseness(pet.getCloseness() + (closeness * getChannelServer().getTraitRate()));
            getClient().announce(PetPacket.updatePet(
                    getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
        }
    }

    public final void gainClosenessAll(final int closeness) {
        for (final MaplePet pet : getPlayer().getSummonedPets()) {
            pet.setCloseness(pet.getCloseness() + closeness);
            getClient().announce(PetPacket.updatePet(
                    getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
        }
    }

    public final void givePartyNX(final int amount, final List<MapleCharacter> party) {
        for (final MapleCharacter chr : party) {
            chr.modifyCSPoints(1, amount, true);
        }
    }

    public final void givePartyNX(final int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainNX(amount);
            return;
        }
        final int cMap = getPlayer().getMapId();
        for (final MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            final MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null
                    && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.modifyCSPoints(1, amount, true);
            }
        }
    }

    public final void resetMap(final int mapid) {
        getMap(mapid).resetFully();
    }

    public final void openNpc(final int npc) {
        openNpc(c, npc);
    }

    public final void openNpc(final String filename) {
        openNpc(c, filename);
    }

    public final void openNpc(final int npc, final String filename) {
        openNpc(c, npc, filename);
    }

    public final void openNpc(final MapleClient client, final int npc) {
        openNpc(client, npc, null);
    }

    public final void openNpc(final MapleClient client, final String filename) {
        openNpc(client, 0, filename);
    }

    public final void openNpc(final MapleClient client, final int npc, final String filename) {
        client.removeClickedNPC();
        NPCScriptManager.getInstance().start(client, npc, filename);
    }

    public final int getMapId() {
        return c.getPlayer().getMap().getId();
    }

    public final boolean haveMonster(final int mobid) {
        for (MapleMapObject obj : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
            final MapleMonster mob = (MapleMonster) obj;
            if (mob.getId() == mobid) {
                return true;
            }
        }
        return false;
    }

    public final int getChannelNumber() {
        return c.getChannel();
    }

    public final int getMonsterCount(final int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getNumMonsters();
    }

    public final void teachSkill(final int id, final int level, final byte masterlevel) {
        getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(id), level, masterlevel);
    }

    public final void teachSkill(final int id, int level) {
        final Skill skil = SkillFactory.getSkill(id);
        if (getPlayer().getSkillLevel(skil) > level) {
            level = getPlayer().getSkillLevel(skil);
        }
        getPlayer().changeSingleSkillLevel(skil, level, (byte) skil.getMaxLevel());
    }

    public final int getPlayerCount(final int mapid) {
        return c.getChannelServer().getMapFactory().getMap(mapid).getCharactersSize();
    }

    public final void dojo_getUp() {
        // int sec = 12;//getCurrentTime
        // long curtime = getCurrentTime();
        // System.err.println(curtime);
        c.getPlayer().updateInfoQuest(7215, "stage=6;type=1;token=3");
        c.getPlayer().updateInfoQuest(7218, "1");
        for (int i = 0; i < 3; i++) {
            c.getPlayer().updateInfoQuest(7281, "item=0;chk=0;cNum=0;sec=2;stage=0;lBonus=0"); // last stage
        }
        for (int i = 0; i < 2; i++) {
            c.getPlayer().updateInfoQuest(7281, "item=0;chk=0;cNum=0;sec=2;stage=0;lBonus=0");
        }
        c.getPlayer().updateInfoQuest(7216, "3");
        c.getPlayer().updateInfoQuest(7214, "5");
        c.getPlayer().updateInfoQuest(7215, "0");
        // c.announce(InfoPacket.updateInfoQuest(1207, "min=1;tuto=1")); //old - 1207,
        // "pt=1;min=4;belt=1;tuto=1"));
        // c.announce(InfoPacket.updateInfoQuest(7281, "item=0;chk=0;cNum=0;sec=" + sec
        // + ";stage=0;lBonus=0"));
        c.announce(EffectPacket.Mulung_DojoUp());
        c.announce(CField.instantMapWarp((byte) 6));
    }

    public final boolean dojoAgent_NextMap(final boolean dojo, final boolean fromresting) {
        if (dojo) {
            return MulungSystem.warpNextMap(c.getPlayer(), fromresting, c.getPlayer().getMap());
        }
        return MulungSystem.warpNextMap_Agent(c.getPlayer(), fromresting);
    }

    public final boolean dojoAgent_NextMap(final boolean dojo, final boolean fromresting, final int mapid) {
        if (dojo) {
            return MulungSystem.warpNextMap(c.getPlayer(), fromresting, getMap(mapid));
        }
        return MulungSystem.warpNextMap_Agent(c.getPlayer(), fromresting);
    }

    public final int dojo_getPts() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO);
    }

    public final MapleEvent getEvent(final String loc) {
        return c.getChannelServer().getEvent(MapleEventType.valueOf(loc));
    }

    public final int getSavedLocation(final String loc) {
        final Integer ret = c.getPlayer().getSavedLocation(SavedLocationType.fromString(loc));
        if (ret == -1) {
            return 950000100;
        }
        return ret;
    }

    public final void saveLocation(final String loc) {
        c.getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public final void saveReturnLocation(final String loc) {
        c.getPlayer().saveLocation(SavedLocationType.fromString(loc), c.getPlayer().getMap().getReturnMap().getId());
    }

    public final void clearSavedLocation(final String loc) {
        c.getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }

    public final void summonMsg(final int type) {
        if (!c.getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        c.announce(UIPacket.summonMessage(true, type, ""));
    }

    public final void summonMsg(final String msg) {
        if (!c.getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        c.announce(UIPacket.summonMessage(false, 0, msg));
    }

    public final void showInstruction(final String msg, final int width, final int height) {
        c.announce(CField.sendHint(msg, width, height));
    }

    public final void playerSummonHint(final boolean summon) {
        c.getPlayer().setHasSummon(summon);
        c.announce(UIPacket.summonHelper(summon));
    }

    public final String getInfoQuest(final int id) {
        return c.getPlayer().getInfoQuest(id);
    }

    public final void updateInfoQuest(final int id, final String data) {
        c.getPlayer().updateInfoQuest(id, data);
    }

    public String getOneInfo(final int questid, final String key) {
        return c.getPlayer().getOneInfo(questid, key);
    }

    public void updateOneInfo(final int questid, final String key, final String value) {
        c.getPlayer().updateOneInfo(questid, key, value);
    }

    public final boolean getEvanIntroState(final String data) {
        return getInfoQuest(22013).equals(data);
    }

    public final void updateEvanIntroState(final String data) {
        updateInfoQuest(22013, data);
    }

    public final void Aran_Start() {
        c.announce(CField.Aran_Start());
    }

    public final void evanTutorial(final String data, final int v1) {
        c.announce(CScriptMan.getEvanTutorial(data));
    }

    public final void AranTutInstructionalBubble(final String data) {
        c.announce(EffectPacket.TutInstructionalBalloon(data));
    }

    public final void ShowWZEffect(final String data) {
        c.announce(EffectPacket.TutInstructionalBalloon(data));
    }

    public void showWZEffect(final String data) {
        c.announce(EffectPacket.showWZEffect(data));
    }

    public void showWZEffectNew(final String data) {
        c.announce(EffectPacket.showWZEffectNew(data));
    }

    public void playSoundEffect(final String data) {
        c.announce(EffectPacket.playSoundEffect(data));
    }

    public void playVoiceEffect(final String data) {
        c.announce(EffectPacket.playVoiceEffect(data));
    }

    public void showScreenAutoLetterBox(String effect) {
        showScreenAutoLetterBox(false, effect);
    }

    public void showScreenAutoLetterBox(boolean broadcast, String effect) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(CField.showScreenAutoLetterBox(effect));
        } else {
            c.announce(CField.showScreenAutoLetterBox(effect));
        }
    }

    public void playSound(String sound) {
        playSound(false, sound);
    }

    public void playSound(boolean broadcast, String sound) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(CField.playSound(sound));
        } else {
            c.announce(CField.playSound(sound));
        }
    }

    public void showTextEffect(String data, int[] values) {
        c.announce(CField.EffectPacket.showEffect(true, null, UserEffectOpcode.UserEffect_TextEffect, values,
                new String[]{data}, null, null));
    }

    public void playAmbientSound(String data) {
        c.announce(CField.playAmbientSound(data));
    }

    public void stopAmbientSound(String data) {
        c.announce(CField.stopAmbientSound(data));
    }

    public void setSkillMap(int skillId, int key) {
        c.announce(CField.setSkillMap(skillId, key));
    }

    public void showBlackBGEffect(int value, int value2, int value3, byte value4) {
        c.announce(EffectPacket.showBlackBGEffect(value, value2, value3, value4));
    }

    public void showDarkEffect(final boolean dark) {
        c.announce(EffectPacket.showDarkEffect(dark));
    }

    public void showWZEffect3(final String data, int[] value) {
        c.announce(EffectPacket.showWZEffect3(data, value));
    }

    public void startMapEffect(String msg, int itemid, boolean active) {
        c.announce(CField.startMapEffect(msg, itemid, active));
    }

    public final void EarnTitleMsg(final String data) {
        c.announce(CWvsContext.getTopMsg(data));
    }

    public MapleInventoryType getInvType(int i) {
        return MapleInventoryType.getByType((byte) i);
    }

    public String getItemName(final int id) {
        return MapleItemInformationProvider.getInstance().getName(id);
    }

    public void gainPet(int id, String name, int level, int closeness, int fullness, long period) {
        if (id >= 5001000 || id < 5000000) {
            id = 5000000;
        }
        if (level > 30) {
            level = 30;
        }
        if (closeness > 30000) {
            closeness = 30000;
        }
        if (fullness > 100) {
            fullness = 100;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        try {
            MapleInventoryManipulator.addById(c, id, (short) 1, "",
                    MaplePet.createPet(id, name, level, closeness, fullness, MapleInventoryIdentifier.getInstance(),
                            ii.getLimitedLife(id), ii.getPetFlagInfo(id), 0),
                    ii.getLife(id), MapleInventoryManipulator.DAY,
                    "從腳本 " + id + "(" + id2 + ")[" + script + "] 中獲得, 時間:" + FileoutputUtil.CurrentReadable_Date());
        } catch (NullPointerException ex) {
        }
    }

    public void removeFromSlot(int invType, byte slot, short quantity) {
        MapleInventoryManipulator.removeFromSlot(c, getInvType(invType), slot, quantity, true);
    }

    public void gainGP(final int gp) {
        if (getPlayer().getGuildId() <= 0) {
            return;
        }
        World.Guild.gainGP(getPlayer().getGuildId(), gp); // 1 for
    }

    public int getGP() {
        if (getPlayer().getGuildId() <= 0) {
            return 0;
        }
        return World.Guild.getGP(getPlayer().getGuildId()); // 1 for
    }

    public void showMapEffect(String path) {
        getClient().announce(CField.showMapEffect(path));
    }

    public int itemQuantity(int itemid) {
        return getPlayer().itemQuantity(itemid);
    }

    public EventInstanceManager getDisconnected(String event) {
        EventManager em = getEventManager(event);
        if (em == null) {
            return null;
        }
        for (EventInstanceManager eim : em.getInstances()) {
            if (eim.isDisconnected(c.getPlayer()) && eim.getPlayerCount() > 0) {
                return eim;
            }
        }
        return null;
    }

    public boolean isAllReactorState(final int reactorId, final int state) {
        boolean ret = false;
        for (MapleReactor r : getMap().getAllReactorsThreadsafe()) {
            if (r.getReactorId() == reactorId) {
                ret = r.getState() == state;
            }
        }
        return ret;
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPlayer().getTruePosition());
    }

    // summon one monster, remote location
    public void spawnMonster(int id, int x, int y) {
        spawnMonster(id, 1, new Point(x, y));
    }

    // multiple monsters, remote location
    public void spawnMonster(int id, int qty, int x, int y) {
        spawnMonster(id, qty, new Point(x, y));
    }

    // handler for all spawnMonster
    public void spawnMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }

    public void sendNPCText(final String text, final int npc) {
        getMap().broadcastMessage(CScriptMan.getNPCTalk(npc, ScriptMessageType.SM_SAY, text, "00 00", (byte) 0));
    }

    public boolean getTempFlag(final int flag) {
        return (c.getChannelServer().getTempFlag(c.getWorld()) & flag) == flag;
    }

    public void sendUIWindow(final int type, final int npc) {
        c.announce(CField.UIPacket.openUIOption(type, npc));
    }

    public void openUI(final int type) {
        c.announce(UIPacket.openUI(type));
    }

    public void logPQ(String text) {
        // FileoutputUtil.log(FileoutputUtil.PQ_Log, text);
    }

    public void outputFileError(Throwable t) {
        FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, t);
    }

    public void trembleEffect(int type, int delay) {
        c.announce(CField.trembleEffect(type, delay));
    }

    public int nextInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public MapleQuest getQuest(int arg0) {
        return MapleQuest.getInstance(arg0);
    }

    public void achievement(int a) {
        c.getPlayer().getMap().broadcastMessage(CField.achievementRatio(a));
    }

    public final MapleInventory getInventory(int type) {
        return c.getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
    }

    public final void prepareAswanMob(int mapid, EventManager eim) {
        MapleMap map = eim.getMapFactory().getMap(mapid);
        if (c.getPlayer().getParty() != null) {
            map.setChangeableMobOrigin(ChannelServer.getInstance(c.getChannel()).getPlayerStorage()
                    .getCharacterById(c.getPlayer().getParty().getLeader().getId()));
        } else {
            map.setChangeableMobOrigin(c.getPlayer());
        }
        // map.setChangeableMobUsing(true);
        map.killAllMonsters(false);
        map.respawn(true);
    }

    public final void startAswanOffSeason(final MapleCharacter leader) {
        final List<MapleCharacter> check1 = c.getChannelServer().getMapFactory().getMap(955000100).getCharacters();
        final List<MapleCharacter> check2 = c.getChannelServer().getMapFactory().getMap(955000200).getCharacters();
        final List<MapleCharacter> check3 = c.getChannelServer().getMapFactory().getMap(955000300).getCharacters();
        c.getChannelServer().getMapFactory().getMap(955000100).broadcastMessage(CField.getClock(20 * 60));
        c.getChannelServer().getMapFactory().getMap(955000200).broadcastMessage(CField.getClock(20 * 60));
        c.getChannelServer().getMapFactory().getMap(955000300).broadcastMessage(CField.getClock(20 * 60));
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (check1 != null && check2 != null && check3 != null && (leader.getMapId() == 955000100
                        || leader.getMapId() == 955000200 || leader.getMapId() == 955000300)) {
                    for (MapleCharacter chrs : check1) {
                        chrs.changeMap(262010000, 0);
                    }
                    for (MapleCharacter chrs : check2) {
                        chrs.changeMap(262010000, 0);
                    }
                    for (MapleCharacter chrs : check3) {
                        chrs.changeMap(262010000, 0);
                    }
                } else {
                    EventTimer.getInstance().stop();
                }
            }
        }, 20 * 60 * 1000);
    }

    public int randInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public void sendRemoveNPC(int oid) {
        c.announce(NPCPacket.removeNPC(oid));
    }

    public void sendDirectionFacialExpression(int expression, int duration) {
        c.announce(UIPacket.getDirectionFacialExpression(expression, duration));
    }

    public final void lockKey(final boolean enabled) {
        c.announce(UIPacket.lockKey(enabled));
    }

    public void lockUI(boolean enable) {
        c.announce(CField.UIPacket.lockUI(enable));
    }

    public void lockUI(boolean enable, int enable2) {
        lockUI(enable ? 1 : 0, enable2);
    }

    public void lockUI(int enable, int enable2) {
        c.announce(CField.UIPacket.lockUI(enable, enable2));
    }

    public final void disableOthers(final boolean enabled) {
        c.announce(UIPacket.disableOthers(enabled));
    }

    public final void disableOthers(final boolean enabled, final int enable2) {
        c.announce(UIPacket.disableOthers(enabled, enable2));
    }

    public void getDirectionStatus(boolean enable) {
        c.announce(CField.UIPacket.getDirectionStatus(enable));
    }

    public void playMovie(String data, boolean show) {
        c.announce(UIPacket.playMovie(data, show));
    }

    public void playMovieURL(String data) {
        c.announce(UIPacket.playMovieURL(data));
    }

    public void spawnReactorOnGroundBelow(int id, int x, int y) {
        c.getPlayer().getMap().spawnReactorOnGroundBelow(new MapleReactor(MapleReactorFactory.getReactor(id), id),
                new Point(x, y));
    }

    public final void topMsg(final String message) {
        getTopMsg(message);
    }

    public void getTopMsg(String message) {
        c.announce(CWvsContext.getTopMsg(message));
    }

    public void getTopMsg2(String message) {
        c.announce(CWvsContext.getTopMsg2(message));
    }

    public void enableActions() {
        c.announce(CWvsContext.enableActions());
    }

    public String getCharacterName(int characterid) {
        return c.getChannelServer().getPlayerStorage().getCharacterById(characterid).getName();
    }

    public final MapleExpedition getExpedition() {
        return World.Party.getExped(c.getPlayer().getParty().getId());
    }

    public int getExpeditionMembers(int id) {
        return World.Party.getExped(c.getPlayer().getParty().getId()).getAllMembers();
    }

    public void warpExpedition(int mapid, int portal) {
        for (MapleCharacter chr : World.Party.getExped(c.getPlayer().getParty().getId()).getExpeditionMembers(c)) {
            chr.changeMap(mapid, portal);
        }
    }

    public String getMasteryBooksByJob(String job) {
        StringBuilder sb = new StringBuilder();
        for (Pair<Integer, String> book : MapleItemInformationProvider.getInstance().getAllItems2()) {
            if (book.getLeft() >= 2280000 && book.getLeft() < 2300000) {
                String skilldesc = MapleItemInformationProvider.getInstance().getDesc(book.getLeft());
                if (skilldesc.contains(job)) {
                    sb.append("~").append(book.getLeft());
                }
            }
        }
        return sb.toString();
    }

    public void test(String test) {
        System.out.println(test);
    }

    public String format(String format, Object... toFormat) {
        return String.format(format, toFormat);
    }

    public void addReward(int type, int item, int mp, int meso, int exp, String desc) {
        addReward(false, type, item, mp, meso, exp, desc);
    }

    public void addReward(boolean forAccount, int type, int item, int mp, int meso, int exp, String desc) {
        getPlayer().addReward(forAccount, type, item, mp, meso, exp, desc);
    }

    public void addReward(long start, long end, int type, int item, int mp, int meso, int exp, String desc) {
        addReward(false, start, end, type, item, mp, meso, exp, desc);
    }

    public void addReward(boolean forAccount, long start, long end, int type, int item, int mp, int meso, int exp,
            String desc) {
        getPlayer().addReward(forAccount, start, end, type, item, mp, meso, exp, desc);
    }

    public int getPQLog(String pqid) {
        return getPlayer().getPQLog(pqid);
    }

    public void setPQLog(String pqid) {
        getPlayer().setPQLog(pqid);
    }

    public void changeDamageSkinByItem(int itemId) {
        if (!ItemConstants.傷害字型.isDamageSkin(itemId)) {
            return;
        }
        changeDamageSkin(ItemConstants.傷害字型.getDamageSkinNumberByItem(itemId), true);
    }

    public void changeDamageSkin(int skinid) {
        changeDamageSkin(skinid, false);
    }

    public void changeDamageSkin(int skinid, boolean show) {
        if (MapleJob.is神之子(c.getPlayer().getJob())) {
            c.getPlayer().dropMessage(-9, "神之子無法套用傷害字型。");
            return;
        }
        if (skinid == -1) {
            c.getPlayer().dropMessage(-9, "出現未知錯誤");
            return;
        }
        c.getPlayer().setDamageSkin(skinid);
        if (show) {
            c.getPlayer().dropMessage(-9, "傷害字型已更變。");
        }
        byte[] packet = CField.updateDamageSkin(c.getPlayer().getId(), skinid);
        if (c.getPlayer().isHidden()) {
            c.getPlayer().getMap().broadcastGMMessage(c.getPlayer(), packet, false);
        } else {
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), packet, false);
        }
        List<WeakReference<MapleCharacter>> clones = c.getPlayer().getClones();
        for (WeakReference<MapleCharacter> cln : clones) {
            if (cln.get() != null) {
                final MapleCharacter clone = cln.get();
                byte[] packetClone = CField.updateDamageSkin(clone.getId(), skinid);
                if (!clone.isHidden()) {
                    clone.getMap().broadcastMessage(clone, packetClone, true);
                } else {
                    clone.getMap().broadcastGMMessage(clone, packetClone, true);
                }
            }
        }
    }

    public void environmentChange(String env) {
        environmentChange(env, FieldEffectOpcode.FieldEffect_Object.getValue());
    }

    public void environmentChange(String env, int info) {
        environmentChange(false, env, FieldEffectOpcode.getType(info));
    }

    public void environmentChange(boolean broadcast, String env) {
        environmentChange(broadcast, env, FieldEffectOpcode.FieldEffect_Object);
    }

    public void environmentChange(boolean broadcast, String env, FieldEffectOpcode info) {
        if (broadcast) {
            c.getPlayer().getMap().broadcastMessage(CField.OnFieldEffect(new String[]{env}, info));
        } else {
            c.announce(CField.OnFieldEffect(new String[]{env}, info));
        }
    }

    public void darkEnv(boolean on, int brightness, int unk) {
        c.announce(CField.OnFieldEffect(FieldEffectOpcode.FieldEffect_Blind, null,
                new int[]{on ? 1 : 0, brightness, 0, 0, 0, unk}));
    }

    public void showOnOffLayer(String[] env, int[] info) {
        c.announce(CField.OnFieldEffect(FieldEffectOpcode.FieldEffect_OnOffLayer, env, info));
    }

    public void showStageClear(int value) {
        c.announce(CField.stageClear(value));
    }

    public void showOverlapDetail(int[] values) {
        c.announce(CField.OnFieldEffect(FieldEffectOpcode.FieldEffect_Overlap_Detail, null, values));
    }

    public void removeOverlapDetail(int value) {
        c.announce(
                CField.OnFieldEffect(FieldEffectOpcode.FieldEffect_Remove_Overlap_Detail, null, new int[]{value}));
    }

    public final void changeMusic(final String songName) {
        getPlayer().getMap().broadcastMessage(CField.musicChange(songName));
    }

    public int[] getSetItems(int setId) {
        StructSetItem setItem = MapleItemInformationProvider.getInstance().getSetItem(setId);
        int[] items = new int[setItem.itemIDs.size()];
        for (int i = 0; i < items.length; i++) {
            items[i] = setItem.itemIDs.get(i);
        }
        return items;
    }

    public boolean useCube(int equipSlot, int cubeId) {
        Equip eq = (Equip) getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) equipSlot);
        Item toUse = getPlayer().getInventory(MapleInventoryType.getByType((byte) (cubeId / 1000000))).findById(cubeId);
        if (eq == null || toUse == null) {
            return false;
        }
        boolean used = getPlayer().useCube(cubeId, eq);
        if (used) {
            c.announce(CWvsContext.InventoryPacket.scrolledItem(toUse, eq, false));
            MapleInventoryManipulator.removeById(c, MapleInventoryType.getByType((byte) (cubeId / 1000000)), cubeId,
                    (short) 1, false, true);
        }
        return used;
    }

    public long getMagnifyPrice(int itemId) {
        return GameConstants.getMagnifyPrice(itemId);
    }

    public boolean magnifyEquip(int equipSlot, int cubeId) {
        Equip eq = (Equip) getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) equipSlot);
        if (eq == null) {
            return false;
        }
        return InventoryHandler.magnifyEquip(c, null, eq, (byte) equipSlot, true, true, cubeId);
    }

    public String getEquipPotential(int equipSlot, int potentialPos, boolean isBonus) {
        Equip eq = (Equip) getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) equipSlot);
        if (eq == null) {
            return "";
        }
        return eq.getPotentialStr(potentialPos, isBonus);
    }

    public int getEquipPotentialStats(int equipSlot, boolean isBonus) {
        Equip eq = (Equip) getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((short) equipSlot);
        if (eq == null) {
            return 0;
        }
        return eq.getState(isBonus);
    }

    public int[] getEquipByChr() {
        List<Item> itemList = getPlayer().getInventory(MapleInventoryType.EQUIP).newList();
        int[] itemId = new int[itemList.get(itemList.size() - 1).getPosition() + 1];
        for (int i = 0; i < itemId.length; i++) {
            itemId[i] = 0;
        }
        for (Item item : itemList) {
            itemId[item.getPosition()] = item.getItemId();
        }
        return itemId;
    }

    public int[] getUseByChr() {
        List<Item> itemList = getPlayer().getInventory(MapleInventoryType.USE).newList();
        int[] itemId = new int[itemList.get(itemList.size() - 1).getPosition() + 1];
        for (int i = 0; i < itemId.length; i++) {
            itemId[i] = 0;
        }
        for (Item item : itemList) {
            itemId[item.getPosition()] = item.getItemId();
        }
        return itemId;
    }

    public int[] getSetupByChr() {
        List<Item> itemList = getPlayer().getInventory(MapleInventoryType.SETUP).newList();
        int[] itemId = new int[itemList.get(itemList.size() - 1).getPosition() + 1];
        for (int i = 0; i < itemId.length; i++) {
            itemId[i] = 0;
        }
        for (Item item : itemList) {
            itemId[item.getPosition()] = item.getItemId();
        }
        return itemId;
    }

    public int[] getEtcByChr() {
        List<Item> itemList = getPlayer().getInventory(MapleInventoryType.ETC).newList();
        int[] itemId = new int[itemList.get(itemList.size() - 1).getPosition() + 1];
        for (int i = 0; i < itemId.length; i++) {
            itemId[i] = 0;
        }
        for (Item item : itemList) {
            itemId[item.getPosition()] = item.getItemId();
        }
        return itemId;
    }

    public int[] getCashByChr() {
        List<Item> itemList = getPlayer().getInventory(MapleInventoryType.CASH).newList();
        int[] itemId = new int[itemList.get(itemList.size() - 1).getPosition() + 1];
        for (int i = 0; i < itemId.length; i++) {
            itemId[i] = 0;
        }
        for (Item item : itemList) {
            itemId[item.getPosition()] = item.getItemId();
        }
        return itemId;
    }

    public void cameraSwitch(String sTargetName, int tTime) {
        c.announce(CField.cameraSwitch(sTargetName, tTime));
    }

    public void cameraSwitchPos(int nX, int nY, int tTime) {
        c.announce(CField.cameraSwitchPos(nX, nY, tTime));
    }

    public void cameraSwitchBack() {
        c.announce(CField.cameraSwitchBack());
    }

    public void progressMessageFont(String sMsg, int nFontNameType, int nFontSize, int nFontColorType,
            int nFadeOutDelay) {
        c.announce(CWvsContext.getProgressMessageFont(sMsg, nFontNameType, nFontSize, nFontColorType, nFadeOutDelay));
    }

    // public int getMVPLevel() {
    // return getClient().getMVPLevel();
    // }
}
