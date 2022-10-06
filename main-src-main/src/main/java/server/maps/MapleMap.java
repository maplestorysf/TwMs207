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
package server.maps;

import scripting.MapScriptMethods;
import client.character.stat.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.familiar.MonsterFamiliar;
import client.MonsterStatus;
import client.MonsterStatusEffect;
import client.skill.Skill;
import client.skill.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import constants.QuickMove;
import constants.QuickMove.QuickMoveNPC;
import constants.FeaturesConfig;
import database.ManagerDatabasePool;
import extensions.temporary.FieldEffectOpcode;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.exped.ExpeditionType;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scripting.EventManager;
import scripting.NPCConversationManager;
import server.MapleCarnivalFactory;
import server.MapleCarnivalFactory.MCSkill;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleSquad;
import server.MapleSquad.MapleSquadType;
import server.MapleStatEffect;
import server.Randomizer;
import server.SpeedRunner;
import server.Timer.EtcTimer;
import server.Timer.MapTimer;
import server.events.MapleEvent;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MapleNPC;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import server.life.SpawnPoint;
import server.life.SpawnPointAreaBoss;
import server.life.Spawns;
import server.maps.MapleNodes.DirectionInfo;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import server.maps.MapleNodes.MonsterPoint;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.MobPacket;
import tools.packet.PetPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.PartyPacket;
import tools.packet.JobPacket.PhantomPacket;
import tools.packet.SkillPacket;
import extensions.temporary.UserEffectOpcode;
import scripting.EventInstanceManager;
import server.life.ChangeableStats;
import server.life.EliteMonsterInfo;
import tools.StringTool;
import tools.packet.FamiliarPacket;
import tools.packet.JobPacket;

public final class MapleMap {

    /*
     * Holds mappings of OID -> MapleMapObject separated by MapleMapObjectType.
     * Please acquire the appropriate lock when reading and writing to the
     * LinkedHashMaps. The MapObjectType Maps themselves do not need to synchronized
     * in any way since they should never be modified.
     */
    private final Map<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>> mapobjects;
    private final Map<MapleMapObjectType, ReentrantReadWriteLock> mapobjectlocks;
    private final List<MapleCharacter> characters = new ArrayList<>();
    private final ReentrantReadWriteLock charactersLock = new ReentrantReadWriteLock();
    private int runningOid = 500000;
    private final Lock runningOidLock = new ReentrantLock();
    private final List<Spawns> monsterSpawn = new ArrayList<>();
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private final Map<Integer, MaplePortal> portals = new HashMap<>();
    private MapleFootholdTree footholds = null;
    private float monsterRate, recoveryRate;
    private MapleMapEffect mapEffect;
    private final byte channel;
    private short decHP = 0, createMobInterval = 10000, top = 0, bottom = 0, left = 0, right = 0;
    private int consumeItemCoolTime = 0, protectItem = 0, decHPInterval = 10000, mapid, returnMapId, timeLimit,
            fieldLimit, maxRegularSpawn = 0, fixedMob, forcedReturnMap = 999999999, instanceid = -1, lvForceMove = 0,
            lvLimit = 0, permanentWeather = 0, partyBonusRate = 0;
    private boolean town, clock, personalShop, everlast = false, dropsDisabled = false, gDropsDisabled = false,
            soaring = false, squadTimer = false, isSpawns = true, checkStates = true;
    private String mapName, streetName, onUserEnter, onFirstUserEnter, speedRunLeader = "";
    private final List<Integer> dced = new ArrayList<>();
    private ScheduledFuture<?> squadSchedule;
    private long speedRunStart = 0, lastSpawnTime = 0, lastHurtTime = 0;
    private MapleNodes nodes;
    private MapleSquadType squad;
    private final Map<String, Integer> environment = new LinkedHashMap<>();
    private WeakReference<MapleCharacter> changeMobOrigin = null;
    private long burningFieldTime;
    private long burningFieldLastTime;
    private long lastSpawnRune = 0;
    private long lastSpawnEliteMob = 0;
    private int killEliteMobTimes = 0;
    private MapleMonster eliteMob = null;
    private List<MapleMonster> eliteBossMobs = new LinkedList<>();

    public MapleMap(final int mapid, final int channel, final int returnMapId, final float monsterRate) {
        this.mapid = mapid;
        this.channel = (byte) channel;
        this.returnMapId = returnMapId;
        burningFieldTime = 0;
        burningFieldLastTime = 0;
        if (this.returnMapId == 999999999) {
            this.returnMapId = mapid;
        }
        if (GameConstants.getPartyPlay(mapid) > 0) {
            this.monsterRate = (monsterRate - 1.0f) * 2.5f + 1.0f;
        } else {
            this.monsterRate = monsterRate;
        }
        EnumMap<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>> objsMap = new EnumMap<>(
                MapleMapObjectType.class);
        EnumMap<MapleMapObjectType, ReentrantReadWriteLock> objlockmap = new EnumMap<>(MapleMapObjectType.class);
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            objsMap.put(type, new LinkedHashMap<>());
            objlockmap.put(type, new ReentrantReadWriteLock());
        }
        mapobjects = Collections.unmodifiableMap(objsMap);
        mapobjectlocks = Collections.unmodifiableMap(objlockmap);
    }

    public final void setSpawns(final boolean fm) {
        this.isSpawns = fm;
    }

    public final boolean getSpawns() {
        return isSpawns;
    }

    public final void setFixedMob(int fm) {
        this.fixedMob = fm;
    }

    public final void setForceMove(int fm) {
        this.lvForceMove = fm;
    }

    public final int getForceMove() {
        return lvForceMove;
    }

    public final void setLevelLimit(int fm) {
        this.lvLimit = fm;
    }

    public final int getLevelLimit() {
        return lvLimit;
    }

    public final void setReturnMapId(int rmi) {
        this.returnMapId = rmi;
    }

    public final void setSoaring(boolean b) {
        this.soaring = b;
    }

    public final boolean canSoar() {
        return soaring;
    }

    public final void toggleDrops() {
        this.dropsDisabled = !dropsDisabled;
    }

    public final void setDrops(final boolean b) {
        this.dropsDisabled = b;
    }

    public final void toggleGDrops() {
        this.gDropsDisabled = !gDropsDisabled;
    }

    public final int getId() {
        return mapid;
    }

    public final MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public final int getReturnMapId() {
        return returnMapId;
    }

    public final int getForcedReturnId() {
        return forcedReturnMap;
    }

    public final MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public final void setForcedReturnMap(final int map) {
        if (GameConstants.isCustomReturnMap(mapid)) {
            this.forcedReturnMap = GameConstants.getCustomReturnMap(mapid);
        } else {
            this.forcedReturnMap = map;
        }
    }

    public final float getRecoveryRate() {
        return recoveryRate;
    }

    public final void setRecoveryRate(final float recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public final int getFieldLimit() {
        return fieldLimit;
    }

    public final void setFieldLimit(final int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public final void setCreateMobInterval(final short createMobInterval) {
        this.createMobInterval = createMobInterval;
    }

    public final void setTimeLimit(final int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public final void setMapName(final String mapName) {
        this.mapName = mapName;
    }

    public final String getMapName() {
        return mapName;
    }

    public final String getStreetName() {
        return streetName;
    }

    public final void setFirstUserEnter(final String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public final void setUserEnter(final String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public final String getFirstUserEnter() {
        return onFirstUserEnter;
    }

    public final String getUserEnter() {
        return onUserEnter;
    }

    public final boolean hasClock() {
        return clock;
    }

    public final void setClock(final boolean hasClock) {
        this.clock = hasClock;
    }

    public final boolean isTown() {
        return town;
    }

    public final void setTown(final boolean town) {
        this.town = town;
    }

    public final boolean allowPersonalShop() {
        return personalShop;
    }

    public final void setPersonalShop(final boolean personalShop) {
        this.personalShop = personalShop;
    }

    public final void setStreetName(final String streetName) {
        this.streetName = streetName;
    }

    public final void setEverlast(final boolean everlast) {
        this.everlast = everlast;
    }

    public final boolean getEverlast() {
        return everlast;
    }

    public final int getHPDec() {
        return decHP;
    }

    public final void setHPDec(final int delta) {
        if (delta > 0 || mapid == 749040100) { // pmd
            lastHurtTime = System.currentTimeMillis(); // start it up
        }
        decHP = (short) delta;
    }

    public final int getHPDecInterval() {
        return decHPInterval;
    }

    public final void setHPDecInterval(final int delta) {
        decHPInterval = delta;
    }

    public final int getHPDecProtect() {
        return protectItem;
    }

    public final void setHPDecProtect(final int delta) {
        this.protectItem = delta;
    }

    public final int getCurrentPartyId() {
        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (chr.getParty() != null) {
                    return chr.getParty().getId();
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return -1;
    }

    public final void addMapObject(final MapleMapObject mapobject) {
        runningOidLock.lock();
        int newOid;
        try {
            newOid = ++runningOid;
        } finally {
            runningOidLock.unlock();
        }

        mapobject.setObjectId(newOid);

        mapobjectlocks.get(mapobject.getType()).writeLock().lock();
        try {
            mapobjects.get(mapobject.getType()).put(newOid, mapobject);
        } finally {
            mapobjectlocks.get(mapobject.getType()).writeLock().unlock();
        }
    }

    public void spawnAndAddRangedMapObject(final MapleMapObject mapobject, final DelayedPacketCreation packetbakery) {
        addMapObject(mapobject);

        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> itr = characters.iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                if (!chr.isClone() && (mapobject.getType() == MapleMapObjectType.AFFECTED_AREA || chr.getTruePosition()
                        .distanceSq(mapobject.getTruePosition()) <= GameConstants.maxViewRangeSq())) {
                    packetbakery.sendPackets(chr.getClient());
                    chr.addVisibleMapObject(mapobject);
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    public final void removeMapObject(final MapleMapObject obj) {
        mapobjectlocks.get(obj.getType()).writeLock().lock();
        try {
            mapobjects.get(obj.getType()).remove(obj.getObjectId());
        } finally {
            mapobjectlocks.get(obj.getType()).writeLock().unlock();
        }
    }

    public final Point calcPointBelow(final Point initial) {
        final MapleFoothold fh = footholds.findBelow(initial, false);
        if (fh == null) {
            return initial;
        }
        int dropX = initial.x;
        if (dropX < footholds.getMinDropX()) {
            dropX = footholds.getMinDropX();
        } else if (dropX > footholds.getMaxDropX()) {
            dropX = footholds.getMaxDropX();
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            final double s1 = Math.abs(fh.getY2() - fh.getY1());
            final double s2 = Math.abs(fh.getX2() - fh.getX1());
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) (Math.cos(Math.atan(s2 / s1))
                        * (Math.abs(dropX - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            } else {
                dropY = fh.getY1() + (int) (Math.cos(Math.atan(s2 / s1))
                        * (Math.abs(dropX - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            }
        }
        return new Point(dropX, dropY);
    }

    public final Point calcDropPos(final Point initial, final Point fallback) {
        final Point ret = calcPointBelow(new Point(initial.x, initial.y - 50));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(final MapleCharacter chr, final MapleMonster mob, final boolean instanced) {
        if (mob == null || chr == null || ChannelServer.getInstance(channel) == null || dropsDisabled
                || mob.dropsDisabled() || chr.getPyramidSubway() != null) { // no drops in pyramid ok? no cash either
            return;
        }

        // We choose not to readLock for this.
        // This will not affect the internal state, and we don't want to
        // introduce unneccessary locking, especially since this function
        // is probably used quite often.
        if (!instanced) {
            LinkedHashMap<Integer, MapleMapObject> drops = mapobjects.get(MapleMapObjectType.ITEM);
            int normalSize = 0;
            int 紅玉咒印Size = 0;
            for (MapleMapObject moj : drops.values()) {
                switch (((MapleMapItem) moj).getItemId()) {
                    case 4033270:
                        紅玉咒印Size++;
                        break;
                    default:
                        normalSize++;
                }
            }
            if (normalSize >= 255 || 紅玉咒印Size >= 255) {
                removeDrops();
                broadcastMessage(CField.getGameMessage(7, "[楓之谷幫助]地上掉寶多於255個，已被清理。"));
            }
        }

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3
                : mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1 : 0);
        final int mobpos = mob.getTruePosition().x;
        final int cmServerrate = ChannelServer.getInstance(channel).getMesoRate(chr.getWorld());
        final int chServerrate = ChannelServer.getInstance(channel).getDropRate(chr.getWorld());
        Item idrop;
        byte d = 1;
        Point pos = new Point(0, mob.getTruePosition().y);
        double showdown = 100.0;
        final MonsterStatusEffect mse = mob.getBuff(MonsterStatus.M_Ambush);
        if (mse != null) {
            showdown += mse.getX();
        }

        final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        final List<MonsterDropEntry> drops = mi.retrieveDrop(mob.getId());
        if (drops == null) { // if no drops, no global drops either
            return;
        }
        final List<MonsterDropEntry> dropEntry = new ArrayList<>(drops);
        Collections.shuffle(dropEntry);

        boolean mesoDropped = false;
        for (final MonsterDropEntry de : dropEntry) {
            if (de.itemId == mob.getStolen()) {
                continue;
            }
            if (de.itemId != 0 && !ii.itemExists(de.itemId)) {
                continue;
            }

            ChannelServer ch = ChannelServer.getInstance(channel);
            if (!ch.getChannelType().check(de.channelType) && de.channelType != 0) {
                continue;
            }

            if (Randomizer.nextInt(999999) < (int) (de.chance * chServerrate * chr.getDropMod() * chr.getStat().dropBuff
                    / 100.0 * (showdown / 100.0))) {
                if (mesoDropped && droptype != 3 && de.itemId == 0) { // not more than 1 sack of meso
                    continue;
                }
                if (de.questid > 0 && chr.getQuestStatus(de.questid) != 1) {
                    continue;
                }
                if (de.itemId / 10000 == 238 && !mob.getStats().isBoss()
                        && chr.getMonsterBook().getLevelByCard(ii.getCardMobId(de.itemId)) >= 2) {
                    continue;
                }
                if (droptype == 3) {
                    pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                } else {
                    pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
                }
                if (de.itemId == 0) { // meso
                    int mesos = Randomizer.nextInt(1 + Math.abs(de.Maximum - de.Minimum)) + de.Minimum;
                    if (mesos > 0) {
                        spawnMobMesoDrop(
                                (int) (mesos * (chr.getStat().mesoBuff / 100.0) * chr.getDropMod() * cmServerrate),
                                calcDropPos(pos, mob.getTruePosition()), mob, chr, false, droptype);
                        mesoDropped = true;
                    }
                } else {
                    if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                        idrop = MapleInventoryManipulator.checkEnhanced(idrop, chr);// 潛能掉寶
                    } else {
                        final int range = Math.abs(de.Maximum - de.Minimum);
                        idrop = new Item(de.itemId, (byte) 0,
                                (short) (de.Maximum != 1 ? Randomizer.nextInt(range <= 0 ? 1 : range) + de.Minimum : 1),
                                0);
                    }
                    idrop.setGMLog("怪物噴寶: " + mob.getId() + " 地圖: " + toString() + " 時間: "
                            + FileoutputUtil.CurrentReadable_Date());
                    spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, de.questid);
                }
                d++;
            }
        }
        final List<MonsterGlobalDropEntry> globalEntry = new ArrayList<>(mi.getGlobalDrop());
        Collections.shuffle(globalEntry);
        // 全域掉寶
        for (final MonsterGlobalDropEntry de : globalEntry) {
            if (Randomizer.nextInt(999999) < de.chance
                    && (de.continent < 0 || (de.continent < 10 && mapid / 100000000 == de.continent)
                    || (de.continent < 100 && mapid / 10000000 == de.continent)
                    || (de.continent < 1000 && mapid / 1000000 == de.continent))) {
                if (de.questid > 0 && chr.getQuestStatus(de.questid) != 1) {
                    continue;
                }

                if ((de.elite == -2 && !mob.isEliteMob()) || de.elite != mob.getEliteType()) {
                    continue;
                }

                ChannelServer ch = ChannelServer.getInstance(channel);
                if (!ch.getChannelType().check(de.channelType) && de.channelType != 0) {
                    continue;
                }

                if (de.itemId > 0 && de.itemId < 4) {
                    chr.modifyCSPoints(de.itemId,
                            (int) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1),
                            true);
                } else if (!gDropsDisabled) {
                    if (droptype == 3) {
                        pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                    } else {
                        pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
                    }
                    if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                        idrop = MapleInventoryManipulator.checkEnhanced(idrop, chr);// 潛能掉寶
                    } else {
                        idrop = new Item(de.itemId, (byte) 0,
                                (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum
                                        : 1),
                                0);
                    }
                    idrop.setGMLog("怪物噴寶: " + mob.getId() + " 地圖: " + toString() + " (全域) 時間: "
                            + FileoutputUtil.CurrentReadable_Date());
                    spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, de.onlySelf ? 0 : droptype,
                            de.questid);
                    d++;
                }
            }
        }
    }

    public void removeMonster(final MapleMonster monster) {
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        if (GameConstants.isAzwanMap(mapid)) {
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0, true));
        } else {
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0, false));
        }
        removeMapObject(monster);
        monster.killed();
    }

    public void killMonster(final MapleMonster monster) { // For mobs with removeAfter
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        if (monster.getLinkCID() <= 0) {
            monster.spawnRevives(this);
        }
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(),
                monster.getStats().getSelfD() < 0 ? 1 : monster.getStats().getSelfD(), false));
        removeMapObject(monster);
        monster.killed();
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops,
            final boolean second, byte animation) {
        killMonster(monster, chr, withDrops, second, animation, 0);
    }

    public final void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops,
            final boolean second, byte animation, final int lastSkill) {
        if (MapleJob.is天使破壞者(chr.getJob())) {
            if (!chr.getKillMob_Temp_using()) {
                chr.addKillMob_Temp();
            }
        }
        // 黑騎士轉生
        if (chr.getBuffedValue(MapleBuffStat.Reincarnation) != null && !monster.getStats().isBoss()) {
            chr.setKillCount(Math.max(chr.getKillCount() - 1, 0));
            final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
            stat.put(MapleBuffStat.Reincarnation, 1);
            chr.getClient().announce(CWvsContext.BuffPacket.giveBuff(1320019, -1, stat, null, chr));
        }

        // 病毒綠水靈
        if (monster.getStatusSourceID(MonsterStatus.M_Poison) == 2111010) {
            Skill sumskill = SkillFactory.getSkill(2111010);
            if (sumskill != null) {
                int skilllevel = chr.getTotalSkillLevel(sumskill);
                MapleStatEffect infoEffect = sumskill.getEffect(skilllevel);
                for (int i = 0; i < 2; i++) {
                    if (chr.getAllLinksummon().size() < infoEffect.getX()) {
                        infoEffect.applySummonEffect(chr, false, monster.getPosition(), infoEffect.getDuration(), 0);
                    }
                }
            }
        }

        // TODO 任務擊殺怪物後激活動作
        if (monster.getId() == 2700302 && chr.getMapId() == 331001120) {
            if (StringTool.parseInt(chr.getOneInfo(22700, "kinetuto")) != 1) {
                chr.updateOneInfo(22700, "kinetuto", "1");
                chr.getClient().announce(CField.playSound("Party1/Clear"));
                MapleQuest.getInstance(25965).forceStart(chr, 0, "1");
            }
        }
        if (monster.getId() == 2700300 && chr.getMapId() == 331001130) {
            int qInfo = StringTool.parseInt(chr.getQuest(MapleQuest.getInstance(25965)).getCustomData());
            if (qInfo < 2) {
                qInfo = 2;
            }
            qInfo++;
            if (StringTool.parseInt(chr.getOneInfo(22700, "kinetuto2")) != 1 && qInfo > 8) {
                chr.updateOneInfo(22700, "kinetuto2", "1");

                chr.getClient().announce(CField.playSound("Party1/Clear"));
                chr.getClient().announce(CField.showScreenAutoLetterBox("monsterPark/clear"));
            }
            if (qInfo > 15) {
                chr.getClient().announce(CField.playSound("Party1/Clear"));
                chr.getClient().announce(CField.showScreenAutoLetterBox("monsterPark/clear"));
            }
            MapleQuest.getInstance(25965).forceStart(chr, 0, String.valueOf(Math.min(qInfo, 16)));
        }
        if (monster.getId() == 9300471) {
            spawnNpcForPlayer(chr.getClient(), 9073000, new Point(-595, 215));
        }
        if (monster.getId() == 9001045) {
            chr.setQuestAdd(MapleQuest.getInstance(25103), (byte) 1, "1");
        }
        if (monster.getId() == 9001050 && chr.getMapId() == 913070050) {
            if (chr.getMap().getMobsSize() < 2) { // should be 1 left
                MapleQuest.getInstance(20035).forceComplete(chr, 1106000);
            }
        }
        if (chr.getMapId() == 940001010) {
            if (chr.getMap().getMobsSize() < 2) {
                Map<Pair<Integer, MapleClient>, MapleNPC> npcs = NPCConversationManager.npcRequestController;
                Integer oid = 0;
                for (Map.Entry<Pair<Integer, MapleClient>, MapleNPC> npc : npcs.entrySet()) {
                    if (npc.getValue().getId() == 3000107 && npc.getKey().right == chr.getClient()) {
                        oid = npc.getKey().left;
                        break;
                    }
                }
                NPCConversationManager.npcRequestController.remove(new Pair<>(oid, chr.getClient()));
                chr.changeMap(940001050, 0);
            }
        }
        if (chr.getMapId() == 940011110) {
            int size = chr.getMap().getMobsSize();
            if (chr.getMap().getMobsSize() < 2) {
                chr.changeMap(940011150, 0);
            }
        }
        if (monster.getId() == 9400902) {
            chr.getMap().startMapEffect(
                    "So what do you think? Just as though as the original, right?! Move by going to Resercher H.",
                    5120039);
        }
        if ((monster.getId() == 8810122 || monster.getId() == 8810018) && !second) {
            MapTimer.getInstance().schedule(() -> {
                killMonster(monster, chr, true, true, (byte) 1);
                killAllMonsters(true);
            }, 3000);
            return;
        }
        if (monster.getId() == 8820014) { // pb sponge, kills pb(w) first before dying
            killMonster(8820000);
        } else if (monster.getId() == 9300166) { // ariant pq bomb
            animation = 2;
        }
        spawnedMonstersOnMap.decrementAndGet();
        removeMapObject(monster);
        monster.killed();
        final MapleSquad sqd = getSquadByMap();
        final boolean instanced = sqd != null || monster.getEventInstance() != null || getEMByMap() != null;
        int dropOwner = monster.killBy(chr, lastSkill);
        if (animation >= 0) {
            if (GameConstants.isAzwanMap(getId())) {
                broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation, true));
            } else {
                broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation, false));
            }
        }
        if (monster.getId() == 9390935 && getId() == 866103000) {
            if (chr.getQuestStatus(59003) != 0) {
                chr.changeMap(866104000, 0);
            } else {
                spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(monster.getId()), monster.getPosition());
            }
        } else if (monster.getId() == 9390936 && getId() == 866105000) {
            spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(monster.getId()), monster.getPosition());
        }
        if (dropOwner != -1) {
            monster.killGainExp(chr, lastSkill);
        }

        if (monster.getBuffToGive() > -1) {
            final int buffid = monster.getBuffToGive();
            final MapleStatEffect buff = MapleItemInformationProvider.getInstance().getItemEffect(buffid);

            charactersLock.readLock().lock();
            try {
                for (final MapleCharacter mc : characters) {
                    if (mc.isAlive()) {
                        buff.applyTo(mc);

                        switch (monster.getId()) {
                            case 8810018:
                            case 8810122:
                            case 8820001:
                                mc.getClient().announce(EffectPacket.showBuffEffect(true, mc, buffid,
                                        UserEffectOpcode.UserEffect_PlayPortalSE, mc.getLevel(), 1)); // HT nine spirit
                                broadcastMessage(mc, EffectPacket.showBuffEffect(false, mc, buffid,
                                        UserEffectOpcode.UserEffect_PlayPortalSE, mc.getLevel(), 1), false); // HT nine
                                // spirit
                                break;
                        }
                    }
                }
            } finally {
                charactersLock.readLock().unlock();
            }
        }
        final int mobid = monster.getId();
        ExpeditionType type = null;
        if (mobid == 8810018 && mapid == 240060200) { // 闇黑龍王
            if (speedRunStart > 0) {
                type = ExpeditionType.Horntail;
            }
            doShrine(true);
        } else if (mobid == 8810122 && mapid == 240060201) { // 混沌闇黑龍王
            broadcastMessage(CWvsContext.broadcastMsg(6, "經過無數次的挑戰，終於擊破了闇黑龍王的遠征隊！你們才是龍之林的真正英雄~"));
            if (speedRunStart > 0) {
                type = ExpeditionType.ChaosHT;
            }
            doShrine(true);
        } else if (mobid == 9400266 && mapid == 802000111) { // 無名魔獸
            doShrine(true);
        } else if (mobid == 9400265 && mapid == 802000211) { // 貝魯加墨特
            doShrine(true);
        } else if (mobid == 9400270 && mapid == 802000411) { // 杜那斯
            doShrine(true);
        } else if (mobid == 9400273 && mapid == 802000611) { // 尼貝龍根
            doShrine(true);
        } else if (mobid == 9400294 && mapid == 802000711) { // 杜那斯
            doShrine(true);
        } else if (mobid == 9400296 && mapid == 802000803) { // 普雷茲首腦
            doShrine(true);
        } else if (mobid == 9400289 && mapid == 802000821) { // 奧芙赫班
            doShrine(true);
        } else if (mobid == 8830000 && mapid == 105100300) { // 巴洛古
            if (speedRunStart > 0) {
                type = ExpeditionType.Balrog;
            }
        } else if ((mobid == 9420544 || mobid == 9420549) && mapid == 551030200 && monster.getEventInstance() != null
                && monster.getEventInstance().getName().contains(getEMByMap().getName())) {
            // 狂暴的泰勒熊
            doShrine(getAllReactor().isEmpty());
        } else if (mobid == 8820001 && mapid == 270050100) { // 皮卡啾
            if (speedRunStart > 0) {
                type = ExpeditionType.Pink_Bean;
            }
            doShrine(true);
        } else if (mobid == 8820212 && mapid == 270051100) { // 混沌皮卡啾
            broadcastMessage(CWvsContext.broadcastMsg(6, "憑藉永遠不疲倦的熱情打敗皮卡啾的遠征隊啊！你們是真正的時間的勝者！"));
            if (speedRunStart > 0) {
                type = ExpeditionType.Chaos_Pink_Bean;
            }
            doShrine(true);
        } else if (mobid == 8850011 && mapid == 274040200) { // 西格諾斯
            broadcastMessage(CWvsContext.broadcastMsg(6, "被黑魔法師黑化的西格諾斯女皇終於被永不言敗的遠征隊打倒! 混沌世界得以淨化!"));
            if (speedRunStart > 0) {
                type = ExpeditionType.Cygnus;
            }
            doShrine(true);
        } else if (mobid == 8870000 && mapid == 262030300) { // 希拉
            if (speedRunStart > 0) {
                type = ExpeditionType.Hilla;
            }
            doShrine(true);
        } else if (mobid == 8870100 && mapid == 262031300) { // 希拉
            if (speedRunStart > 0) {
                type = ExpeditionType.Hilla;
            }
            doShrine(true);
        } else if (mobid == 8860000 && mapid == 272030400) { // 阿卡伊農
            if (speedRunStart > 0) {
                type = ExpeditionType.Akyrum;
            }
            doShrine(true);
        } else if (mobid == 8840000 && mapid == 211070100) { // 凡雷恩
            if (speedRunStart > 0) {
                type = ExpeditionType.Von_Leon;
            }
            doShrine(true);
        } else if (mobid == 8800002 && (mapid == 280030000 || mapid == 280030100)) { // 殘暴炎魔
            if (speedRunStart > 0) {
                type = ExpeditionType.Zakum;
            }
            doShrine(true);
        } else if (mobid == 8800102 && mapid == 280030001) { // 混沌殘暴炎魔
            if (speedRunStart > 0) {
                type = ExpeditionType.Chaos_Zakum;
            }
            doShrine(true);
        } else if (mobid >= 8800003 && mobid <= 8800010) { // 殘暴炎魔
            boolean makeZakReal = true;
            final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMapObject object : monsters) {
                    final MapleMonster mons = ((MapleMonster) object);
                    if (mons.getId() == 8800000) {
                        final Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), pos);
                        break;
                    }
                }
            }
        } else if (mobid >= 8800103 && mobid <= 8800110) { // 混沌殘暴炎魔
            boolean makeZakReal = true;
            final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800103 && mons.getId() <= 8800110) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMonster mons : monsters) {
                    if (mons.getId() == 8800100) {
                        final Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800100), pos);
                        break;
                    }
                }
            }
        } else if (mobid >= 8800023 && mobid <= 8800030) { // 弱化的殘暴炎魔
            boolean makeZakReal = true;
            final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800023 && mons.getId() <= 8800030) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMonster mons : monsters) {
                    if (mons.getId() == 8800020) {
                        final Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800020), pos);
                        break;
                    }
                }
            }
        } else if (mobid >= 9400903 && mobid <= 9400910) { // 粉紅殘暴炎魔
            boolean makeZakReal = true;
            final Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 9400903 && mons.getId() <= 9400910) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMapObject object : monsters) {
                    final MapleMonster mons = ((MapleMonster) object);
                    if (mons.getId() == 9400900) {
                        final Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9400900), pos);
                        break;
                    }
                }
            }
        } else if (mobid == 8820008) { // 皮卡啾
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid >= 8820010 && mobid <= 8820014) { // 皮卡啾
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getId() != 8820000 && mons.getId() != 8820001 && mons.getObjectId() != monster.getObjectId()
                        && mons.isAlive() && mons.getLinkOid() == monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid == 8820108) { // 混沌皮卡啾
            for (MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid >= 8820300 && mobid <= 8820304) { // 混沌皮卡啾
            for (MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if ((mons.getId() != 8820100) && (mons.getId() != 8820212)
                        && (mons.getObjectId() != monster.getObjectId()) && (mons.isAlive())
                        && (mons.getLinkOid() == monster.getObjectId())) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid >= 9390600 && mobid < 9390602) { // 培羅德的頭
            EventInstanceManager eim = chr.getEventInstance();
            if (eim != null) {
                MapleMonster mob = MapleLifeFactory.getMonster(mobid + 1);
                if (mob != null) {
                    int rank = Math.max(Math.min(StringTool.parseInt(eim.getProperty("rank")), 4), 1);
                    ChangeableStats cStats = monster.getChangedStats();
                    if (cStats != null) {
                        long hp = 0;
                        switch (cStats.level) {
                            case 170:
                                switch (mob.getId() % 10) {
                                    case 1:
                                        hp = 100000000L;
                                        break;
                                    case 2:
                                        hp = 50000000L;
                                        break;
                                }
                                break;
                            case 190:
                                switch (mob.getId() % 10) {
                                    case 1:
                                        hp = 1500000000L;
                                        break;
                                    case 2:
                                        hp = 300000000L;
                                        break;
                                }
                                break;
                            case 210:
                                switch (mob.getId() % 10) {
                                    case 1:
                                        hp = 30000000000L;
                                        break;
                                    case 2:
                                        hp = 30000000000L;
                                        break;
                                }
                                break;
                        }
                        if (hp > 0) {
                            ChangeableStats changeStats = new ChangeableStats(mob.getStats());
                            changeStats.level = cStats.level;
                            changeStats.PDRate = cStats.PDRate;
                            changeStats.MDRate = cStats.MDRate;
                            changeStats.finalmaxHP = hp;
                            mob.setOverrideStats(changeStats);
                        }
                    }
                    spawnMonsterOnGroundBelow(mob, monster.getPosition());
                }
            }
        } else if (mobid / 100000 == 98 && chr.getMapId() / 10000000 == 95 && getAllMonstersThreadsafe().isEmpty()) {
            switch ((chr.getMapId() % 1000) / 100) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    chr.getClient().announce(CField.showMapEffect("monsterPark/clear"));
                    break;
                case 5:
                    if (chr.getMapId() / 1000000 == 952) {
                        chr.getClient().announce(CField.showMapEffect("monsterPark/clearF"));
                    } else {
                        chr.getClient().announce(CField.showMapEffect("monsterPark/clear"));
                    }
                    break;
                case 6:
                    chr.getClient().announce(CField.showMapEffect("monsterPark/clearF"));
                    break;
            }
        }
        if (type != null) {
            if (speedRunStart > 0 && speedRunLeader.length() > 0) {
                long endTime = System.currentTimeMillis();
                String time = StringUtil.getReadableMillis(speedRunStart, endTime);
                broadcastMessage(CWvsContext.broadcastMsg(5,
                        speedRunLeader + "'s squad has taken " + time + " to defeat " + type.name() + "!"));
                getRankAndAdd(speedRunLeader, time, type, (endTime - speedRunStart),
                        (sqd == null ? null : sqd.getMembers()));
                endSpeedRun();
            }

        }

        if (chr.isEquippedSoulWeapon()) {
            int num = 1 + Randomizer.nextInt(4);
            // 4001536 - 靈魂結晶
            for (int i = 0; i < num; i++) {
                Item 靈魂結晶 = new Item(4001536, (byte) 0, (short) 1);
                MapleMapItem drop = new MapleMapItem(靈魂結晶, monster.getPosition(), monster, chr, (byte) 0, false);
                drop.registerExpire(500000);
                addMapObject(drop);
                chr.getClient().announce(
                        CField.dropItemFromMapObject(drop, monster.getPosition(), monster.getPosition(), (byte) 1));
                drop.setPickedUp(true);
                chr.getClient().announce(CField.removeItemFromMap(drop.getObjectId(), 2, chr.getId()));
                removeMapObject(drop);
            }
            chr.getClient().announce(
                    CWvsContext.BuffPacket.giveSoulGauge(chr.addgetSoulCount(num), chr.getEquippedSoulSkill()));
            chr.checkSoulState(false);
        }

        if (MapleJob.is凱內西斯(chr.getJob())) {
            chr.givePPoint(0);
        }

        if (MapleJob.is煉獄巫師(chr.getJob())) {
            int count = chr.getBuffedValue(MapleBuffStat.BMageDeath) != null
                    ? chr.getBuffedValue(MapleBuffStat.BMageDeath)
                    : -1;
            int maxAttack = 6;
            if (chr.getBuffSource(MapleBuffStat.AttackCountX) == 32121056) {
                maxAttack = 3;
            }

            if (count > -1) {
                count++;
                if (count > maxAttack) {
                    count = 0;
                    chr.getClient().announce(JobPacket.BattleMagicPacket.GrimContractAttack(chr));
                    Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
                    statups.put(MapleBuffStat.BMageDeath, count);
                    chr.setBuffedValue(MapleBuffStat.BMageDeath, count);
                    chr.getClient().announce(CWvsContext.BuffPacket
                            .giveBuff(chr.getBuffSource(MapleBuffStat.BMageDeath), 0, statups, null, null));
                    chr.getClient().announce(CWvsContext.enableActions());
                }
                Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
                statups.put(MapleBuffStat.BMageDeath, count);
                chr.setBuffedValue(MapleBuffStat.BMageDeath, count);
                chr.getClient().announce(CWvsContext.BuffPacket.giveBuff(chr.getBuffSource(MapleBuffStat.BMageDeath), 0,
                        statups, null, null));
                chr.getClient().announce(CWvsContext.enableActions());
            }
        }

        if (withDrops && dropOwner != -1) {
            MapleCharacter drop;
            if (dropOwner <= 0) {
                drop = chr;
            } else {
                drop = getCharacterById(dropOwner);
                if (drop == null) {
                    drop = chr;
                }
            }
            dropFromMonster(drop, monster, instanced);
        }
        // 處理怪物死亡點數掉落
        FeaturesConfig.gainMobBFGash(monster, chr);

        // 符文輪處理
        Pair<Integer, Integer> mapLevel = getMapMonsterLvRange();
        long lastRune = System.currentTimeMillis() - lastSpawnRune;
        if (!isTown() && lastRune >= 10 * 60 * 1000 && mapLevel.getLeft() >= 30
                && (!mapobjects.containsKey(MapleMapObjectType.RUNE)
                || mapobjects.get(MapleMapObjectType.RUNE).isEmpty())) {
            Spawns spawn = (Spawns) monsterSpawn.get(Randomizer.rand(0, monsterSpawn.size() - 1));
            MapleRune rune = new MapleRune(Randomizer.rand(0, 3), spawn.getPosition(), this);
            spawnRune(rune);
        }

        // 菁英怪物處理
        if (monster.isEliteMob()) {
            boolean spawnBoss = false;
            if (((eliteBossMobs.isEmpty() && (monster.getStats().getLevel() <= chr.getLevel() + 30
                    && monster.getStats().getLevel() >= chr.getLevel() - 30)) || monster.getStats().getLevel() >= 170)
                    && monster == eliteMob) {
                killEliteMobTimes += 1;
                if (killEliteMobTimes >= 10 && Randomizer.isSuccess(5 * killEliteMobTimes)) {
                    spawnEliteBoss(monster);
                    spawnBoss = true;
                }
            }
            if (monster == eliteMob) {
                eliteMob = null;
            }
            if (eliteBossMobs.contains(monster)) {
                eliteBossMobs.remove(monster);
            } else if (!spawnBoss) {
                broadcastMessage(CWvsContext
                        .getTopMsg(killEliteMobTimes < 10 ? "黑暗氣息尚未消失,這個地方變的更加陰森." : "這個地方充滿了黑暗氣息,好像有什麼事情即將發生的樣子."));
            }
        } else {
            long lastElite = System.currentTimeMillis() - lastSpawnEliteMob;
            if (!monster.getStats().isBoss() && lastElite >= 10 * 60 * 1000 && Randomizer.isSuccess(10)
                    && eliteMob == null) {
                if ((monster.getStats().getLevel() <= chr.getLevel() + 30
                        && monster.getStats().getLevel() >= chr.getLevel() - 30)
                        || monster.getStats().getLevel() >= 170) {
                    MapleMonster mob = spawnEliteMob(monster, false, true);
                    if (mob != null) {
                        lastSpawnEliteMob = System.currentTimeMillis();
                        eliteMob = mob;
                    }
                }
            }
        }
    }

    public void spawnEliteBoss(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        for (MapleMapObject monstermo : getMonstersInRange(monster.getPosition(), Double.POSITIVE_INFINITY)) {
            MapleMonster mob = (MapleMonster) monstermo;
            removeMonster(mob);
        }
        MapleMonster mob = spawnEliteMob(monster, true, false);
        if (mob != null) {
            killEliteMobTimes = 0;
            eliteBossMobs.add(mob);
        }
        for (int i = 0; i < 2; i++) {
            MapleMonster m = spawnEliteMob(monster, false, false);
            if (m != null) {
                eliteBossMobs.add(m);
            }
        }
        lastSpawnEliteMob = System.currentTimeMillis();
    }

    public MapleMonster spawnEliteMob(MapleMonster monster, boolean isBoss, boolean showInfo) {
        MapleMonster mob = MapleLifeFactory
                .getMonster(isBoss ? GameConstants.EliteBossIds[Randomizer.nextInt(GameConstants.EliteBossIds.length)]
                        : monster.getId());

        if (mob != null) {
            mob.setEliteInfo(new EliteMonsterInfo());
            Map<EliteMonsterInfo.EliteAttribute, Integer> attMap = mob.getEliteInfo().getEliteAttribute();
            if (isBoss) {
                for (int i = 0; i < 10; i++) {
                    EliteMonsterInfo.EliteAttribute att = EliteMonsterInfo.EliteAttribute.getRandomAttribute();
                    if (!attMap.containsKey(att)) {
                        attMap.put(att, 100);
                        break;
                    }
                }
            }
            ChangeableStats changeStats = mob.getChangedStats();
            if (changeStats == null) {
                changeStats = new ChangeableStats(mob.getStats(), monster.getStats().getLevel(), true);
            }
            long hp;
            switch (mob.getEliteType()) {
                case 0:
                    hp = changeStats.hp * 30;
                    changeStats.hp = (int) Math.min(Integer.MAX_VALUE, hp < 0 ? Long.MAX_VALUE : hp);
                    if (changeStats.finalmaxHP > 0) {
                        changeStats.finalmaxHP *= 30;
                    } else if (hp > Integer.MAX_VALUE) {
                        changeStats.finalmaxHP = hp;
                    }
                    changeStats.exp *= 15;
                    break;
                case 1:
                    hp = changeStats.hp * 45;
                    changeStats.hp = (int) Math.min(Integer.MAX_VALUE, hp < 0 ? Long.MAX_VALUE : hp);
                    if (changeStats.finalmaxHP > 0) {
                        changeStats.finalmaxHP *= 45;
                    } else if (hp > Integer.MAX_VALUE) {
                        changeStats.finalmaxHP = hp;
                    }
                    changeStats.exp *= 22.5;
                    break;
                case 2:
                    hp = changeStats.hp * 60;
                    changeStats.hp = (int) Math.min(Integer.MAX_VALUE, hp < 0 ? Long.MAX_VALUE : hp);
                    if (changeStats.finalmaxHP > 0) {
                        changeStats.finalmaxHP *= 60;
                    } else if (hp > Integer.MAX_VALUE) {
                        changeStats.finalmaxHP = hp;
                    }
                    changeStats.exp *= 30;
                    break;
            }
            if (changeStats.finalmaxHP < 0) {
                changeStats.finalmaxHP = Long.MAX_VALUE;
            }
            mob.setOverrideStats(changeStats);
            mob.setPosition(monster.getPosition());
            mob.setFh(monster.getFh());
            mob.setMobSize(200);
            spawnMonster(mob, -2);
            if (showInfo) {
                broadcastMessage(CWvsContext.getTopMsg("強大怪物伴隨著黑暗氣息一同出現."));
            }
        }

        return mob;
    }

    public List<MapleReactor> getAllReactor() {
        return getAllReactorsThreadsafe();
    }

    public List<MapleReactor> getAllReactorsThreadsafe() {
        ArrayList<MapleReactor> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ret.add((MapleReactor) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        return ret;
    }

    public List<MapleRune> getAllRune() {
        return getAllRuneThreadsafe();
    }

    public List<MapleRune> getAllRuneThreadsafe() {
        ArrayList<MapleRune> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.RUNE).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.RUNE).values()) {
                ret.add((MapleRune) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.RUNE).readLock().unlock();
        }
        return ret;
    }

    public List<MapleSummon> getAllSummonsThreadsafe() {
        ArrayList<MapleSummon> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.SUMMON).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.SUMMON).values()) {
                if (mmo instanceof MapleSummon) {
                    ret.add((MapleSummon) mmo);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.SUMMON).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getAllDoor() {
        return getAllDoorsThreadsafe();
    }

    public List<MapleMapObject> getAllDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
                if (mmo instanceof MapleDoor) {
                    ret.add(mmo);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getAllMechDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
                if (mmo instanceof MapleOpenGate) {
                    ret.add(mmo);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getAllMerchant() {
        return getAllHiredMerchantsThreadsafe();
    }

    public List<MapleMapObject> getAllHiredMerchantsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.HIRED_MERCHANT).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.HIRED_MERCHANT).values()) {
                ret.add(mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.HIRED_MERCHANT).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMonster> getAllMonster() {
        return getAllMonstersThreadsafe();
    }

    public List<MapleMonster> getAllMonstersThreadsafe() {
        ArrayList<MapleMonster> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
                ret.add((MapleMonster) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
        return ret;
    }

    public List<Integer> getAllUniqueMonsters() {
        ArrayList<Integer> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
                final int theId = ((MapleMonster) mmo).getId();
                if (!ret.contains(theId)) {
                    ret.add(theId);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
        return ret;
    }

    public final void killAllMonsters(final boolean animate) {
        for (final MapleMapObject monstermo : getAllMonstersThreadsafe()) {
            final MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            if (GameConstants.isAzwanMap(mapid)) {
                broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0, true));
            } else {
                broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0, false));
            }
            removeMapObject(monster);
            monster.killed();
        }
    }

    public final void killMonster(final int monsId) {
        for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
            if (((MapleMonster) mmo).getId() == monsId) {
                spawnedMonstersOnMap.decrementAndGet();
                removeMapObject(mmo);
                if (GameConstants.isAzwanMap(mapid)) {
                    broadcastMessage(MobPacket.killMonster(mmo.getObjectId(), 1, true));
                } else {
                    broadcastMessage(MobPacket.killMonster(mmo.getObjectId(), 1, false));
                }
                ((MapleMonster) mmo).killed();
                break;
            }
        }
    }

    private String MapDebug_Log() {
        final StringBuilder sb = new StringBuilder("Defeat time : ");
        sb.append(FileoutputUtil.CurrentReadable_Time());

        sb.append(" | Mapid : ").append(this.mapid);

        charactersLock.readLock().lock();
        try {
            sb.append(" Users [").append(characters.size()).append("] | ");
            for (MapleCharacter mc : characters) {
                sb.append(mc.getName()).append(", ");
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return sb.toString();
    }

    public final void limitReactor(final int rid, final int num) {
        List<MapleReactor> toDestroy = new ArrayList<>();
        Map<Integer, Integer> contained = new LinkedHashMap<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (contained.containsKey(mr.getReactorId())) {
                    if (contained.get(mr.getReactorId()) >= num) {
                        toDestroy.add(mr);
                    } else {
                        contained.put(mr.getReactorId(), contained.get(mr.getReactorId()) + 1);
                    }
                } else {
                    contained.put(mr.getReactorId(), 1);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public final void destroyReactors(final int first, final int last) {
        List<MapleReactor> toDestroy = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    toDestroy.add(mr);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public final void destroyReactor(final int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        if (reactor == null) {
            return;
        }
        broadcastMessage(CField.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);

        if (reactor.getDelay() > 0) {
            MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public final void run() {
                    respawnReactor(reactor);
                }
            }, reactor.getDelay());
        }
    }

    public final void reloadReactors() {
        List<MapleReactor> toSpawn = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                final MapleReactor reactor = (MapleReactor) obj;
                broadcastMessage(CField.destroyReactor(reactor));
                reactor.setAlive(false);
                reactor.setTimerActive(false);
                toSpawn.add(reactor);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor r : toSpawn) {
            removeMapObject(r);
            if (!r.isCustom()) { // guardians cpq
                respawnReactor(r);
            }
        }
    }

    /*
     * command to reset all item-reactors in a map to state 0 for GM/NPC use - not
     * tested (broken reactors get removed from mapobjects when destroyed) Should
     * create instances for multiple copies of non-respawning reactors...
     */
    public final void resetReactors() {
        setReactorState((byte) 0);
    }

    public final void setReactorState() {
        setReactorState((byte) 1);
    }

    public final void setReactorState(final byte state) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ((MapleReactor) obj).forceHitReactor(null, state);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public final void setReactorDelay(final int state) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ((MapleReactor) obj).setDelay(state);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /*
     * command to shuffle the positions of all reactors in a map for PQ purposes
     * (such as ZPQ/LMPQ)
     */
    public final void shuffleReactors() {
        shuffleReactors(0, 9999999); // all
    }

    public final void shuffleReactors(int first, int last) {
        List<Point> points = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    points.add(mr.getPosition());
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        Collections.shuffle(points);
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    mr.setPosition(points.remove(points.size() - 1));
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /**
     * Automagically finds a new controller for the given monster from the chars
     * on the map...
     *
     * @param monster
     */
    public final void updateMonsterController(final MapleMonster monster) {
        if (!monster.isAlive() || monster.getLinkCID() > 0 || monster.getStats().isEscort()) {
            return;
        }
        if (monster.getController() != null) {
            if (monster.getController().getMap() != this || monster.getController().getTruePosition()
                    .distanceSq(monster.getTruePosition()) > monster.getRange()) {
                monster.getController().stopControllingMonster(monster);
            } else { // Everything is fine :)
                return;
            }
        }
        int mincontrolled = -1;
        MapleCharacter newController = null;

        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (!chr.isHidden() && !chr.isClone()
                        && (chr.getControlledSize() < mincontrolled || mincontrolled == -1)
                        && chr.getTruePosition().distanceSq(monster.getTruePosition()) <= monster.getRange()) {
                    mincontrolled = chr.getControlledSize();
                    newController = chr;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        if (newController != null) {
            if (monster.isFirstAttack()) {
                newController.controlMonster(monster, true);
                monster.setControllerHasAggro(true);
                monster.setControllerKnowsAboutAggro(true);
            } else {
                newController.controlMonster(monster, false);
            }
        }
    }

    public final MapleMapObject getMapObject(int oid, MapleMapObjectType type) {
        mapobjectlocks.get(type).readLock().lock();
        try {
            return mapobjects.get(type).get(oid);
        } finally {
            mapobjectlocks.get(type).readLock().unlock();
        }
    }

    public final boolean containsNPC(int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC n = (MapleNPC) itr.next();
                if (n.getId() == npcid) {
                    return true;
                }
            }
            return false;

        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public MapleNPC getNPCById(int id) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC n = (MapleNPC) itr.next();
                if (n.getId() == id) {
                    return n;
                }
            }
            return null;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public MapleMonster getMonsterById(int id) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            MapleMonster ret = null;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
            while (itr.hasNext()) {
                MapleMonster n = (MapleMonster) itr.next();
                if (n.getId() == id) {
                    ret = n;
                    break;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public int countMonsterById(int id) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            int ret = 0;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
            while (itr.hasNext()) {
                MapleMonster n = (MapleMonster) itr.next();
                if (n.getId() == id) {
                    ret++;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public MapleReactor getReactorById(int id) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            MapleReactor ret = null;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.REACTOR).values().iterator();
            while (itr.hasNext()) {
                MapleReactor n = (MapleReactor) itr.next();
                if (n.getReactorId() == id) {
                    ret = n;
                    break;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns
     * null
     *
     * @param oid
     * @return
     */
    public final MapleMonster getMonsterByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.MONSTER);
        if (mmo == null) {
            return null;
        }
        return (MapleMonster) mmo;
    }

    public final MapleNPC getNPCByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.NPC);
        if (mmo == null) {
            return null;
        }
        return (MapleNPC) mmo;
    }

    public final MapleReactor getReactorByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.REACTOR);
        if (mmo == null) {
            return null;
        }
        return (MapleReactor) mmo;
    }

    public final MonsterFamiliar getFamiliarByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.FAMILIAR);
        if (mmo == null) {
            return null;
        }
        return (MonsterFamiliar) mmo;
    }

    public MapleAffectedArea getMistByChr(int cid, int skillid) {
        for (MapleAffectedArea mist : getAllMistsThreadsafe()) {
            if (mist.getOwnerId() == cid && mist.getSource().getSourceId() == skillid) {
                return mist;
            }
        }
        return null;
    }

    public void removeMist(final MapleAffectedArea Mist) {
        this.removeMapObject(Mist);
        this.broadcastMessage(CField.removeAffectedArea(Mist.getObjectId(), false));
    }

    public void removeSummon(final int summonid) {
        MapleSummon summon = (MapleSummon) getMapObject(summonid, MapleMapObjectType.SUMMON);
        this.removeMapObject(summon);
        this.broadcastMessage(SummonPacket.removeSummon(summon, false));
    }

    public MapleAffectedArea getMistByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.AFFECTED_AREA);
        if (mmo == null) {
            return null;
        }
        return (MapleAffectedArea) mmo;
    }

    public final MapleReactor getReactorByName(final String name) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = ((MapleReactor) obj);
                if (mr.getName().equalsIgnoreCase(name)) {
                    return mr;
                }
            }
            return null;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public final void spawnNpc(final int id, final Point pos) {
        final MapleNPC npc = MapleLifeFactory.getNPC(id);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(getFootholds().findBelow(pos, false).getId());
        npc.setCustom(true);
        addMapObject(npc);
        broadcastMessage(NPCPacket.spawnNPC(npc, true));
    }

    public final void spawnNpcForPlayer(MapleClient c, final int id, final Point pos) {
        final MapleNPC npc = MapleLifeFactory.getNPC(id);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(getFootholds().findBelow(pos, false).getId());
        npc.setCustom(true);
        addMapObject(npc);
        c.announce(NPCPacket.spawnNPC(npc, true));
    }

    public final void removeNpc(final int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).writeLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC npc = (MapleNPC) itr.next();
                if (npc.isCustom() && (npcid == -1 || npc.getId() == npcid)) {
                    broadcastMessage(NPCPacket.removeNPCController(npc.getObjectId()));
                    broadcastMessage(NPCPacket.removeNPC(npc.getObjectId()));
                    itr.remove();
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).writeLock().unlock();
        }
    }

    public final void hideNpc(final int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC npc = (MapleNPC) itr.next();
                if (npcid == -1 || npc.getId() == npcid) {
                    broadcastMessage(NPCPacket.removeNPCController(npc.getObjectId()));
                    broadcastMessage(NPCPacket.removeNPC(npc.getObjectId()));
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public final void hideNpc(MapleClient c, final int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC npc = (MapleNPC) itr.next();
                if (npcid == -1 || npc.getId() == npcid) {
                    c.announce(NPCPacket.removeNPCController(npc.getObjectId()));
                    c.announce(NPCPacket.removeNPC(npc.getObjectId()));
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public final void spawnReactorOnGroundBelow(final MapleReactor mob, final Point pos) {
        mob.setPosition(pos); // reactors dont need FH lol
        mob.setCustom(true);
        spawnReactor(mob);
    }

    public final void spawnMonster_sSack(final MapleMonster mob, final Point pos, final int spawnType) {
        mob.setPosition(calcPointBelow(new Point(pos.x, pos.y - 1)));
        spawnMonster(mob, spawnType);
    }

    public final void spawnObtacleAtom() {
        List<ObtacleAtom> loa = new ArrayList<>();
        ObtacleAtom oa = new ObtacleAtom(new Point(900, -1347));
        loa.add(oa);
        CField.spawnObtacleAtomBomb(loa);
    }

    public final void spawnMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        spawnMonster_sSack(mob, pos, -2);
    }

    public final int spawnMonsterWithEffectBelow(final MapleMonster mob, final Point pos, final int effect) {
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        return spawnMonsterWithEffect(mob, effect, spos);
    }

    public final void spawnZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800000);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800003, 8800004, 8800005, 8800006, 8800007, 8800008, 8800009, 8800010};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnChaosZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800100);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800103, 8800104, 8800105, 8800106, 8800107, 8800108, 8800109, 8800110};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnSimpleZakum(final int x, final int y) {
        final Point pos = new Point(x, y);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800020);
        final Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        final int[] zakpart = {8800023, 8800024, 8800025, 8800026, 8800027, 8800028, 8800029, 8800030};

        for (final int i : zakpart) {
            final MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnFakeMonsterOnGroundBelow(final MapleMonster mob, final Point pos) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        spos.y -= 1;
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    private void checkRemoveAfter(final MapleMonster monster) {
        final int ra = monster.getStats().getRemoveAfter();

        if (ra > 0 && monster.getLinkCID() <= 0) {
            monster.registerKill(ra * 1000);
        }
    }

    public final void spawnRevives(final MapleMonster monster, final int oid) {
        monster.setMap(this);
        checkRemoveAfter(monster);
        monster.setLinkOid(oid);
        spawnAndAddRangedMapObject(monster, (MapleClient c) -> {
            if (GameConstants.isAzwanMap(c.getPlayer().getMapId())) {
                c.announce(MobPacket.spawnMonster(monster,
                        monster.getStats().getSummonType() <= 1 ? -3 : monster.getStats().getSummonType(), oid, true));
            } else {
                c.announce(MobPacket.spawnMonster(monster,
                        monster.getStats().getSummonType() <= 1 ? -3 : monster.getStats().getSummonType(), oid, false));
            }
        });
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    /*
     * public final void spawnMonster(final MapleMonster monster, final int
     * spawnType) { spawnMonster(monster, spawnType, false); }
     * 
     * public final void spawnMonster(final MapleMonster monster, final int
     * spawnType, final boolean overwrite) { monster.setMap(this);
     * checkRemoveAfter(monster);
     * 
     * spawnAndAddRangedMapObject(monster, new DelayedPacketCreation() {
     * 
     * public final void sendPackets(MapleClient c) {
     * c.announce(MobPacket.spawnMonster(monster, monster.getStats().getSummonType()
     * <= 1 || monster.getStats().getSummonType() == 27 || overwrite ? spawnType :
     * monster.getStats().getSummonType(), 0)); } });
     * updateMonsterController(monster);
     * 
     * spawnedMonstersOnMap.incrementAndGet(); }
     */
    public final void spawnMonster(MapleMonster monster, int spawnType) {
        spawnMonster(monster, spawnType, false);
    }

    public final void spawnMonster(final MapleMonster monster, final int spawnType, final boolean overwrite) {
        if (this.getId() == 109010100 && monster.getId() != 9300166) {
            return;
        }
        monster.setMap(this);
        checkRemoveAfter(monster);

        if (monster.getId() == 9300166) {
            MapTimer.getInstance().schedule(() -> {
                broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 2, false));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    System.out.println("Error spawning monster: " + ex);
                }
                removeMapObject(monster);
            }, this.getId() == 109010100 ? 1500
                    /* Bomberman */ : this.getId() == 910025200 ? 200 /* The Dragon's Shout */ : 3000);
        }

        // 怪物調整屬性
        ChannelServer ch = ChannelServer.getInstance(channel);
        if (ch.getChannelType() != ChannelServer.ChannelType.普通 && !GameConstants.isTutorialMap(mapid)) {
            ChangeableStats changeStats = monster.getChangedStats();
            if (changeStats == null) {
                changeStats = new ChangeableStats(monster.getStats());
            }
            changeStats.level += Math.min(changeStats.level >= 100 ? 20 : changeStats.level >= 30 ? 10 : 0, 255);
            long hp = changeStats.hp * (changeStats.level < 100 ? 4
                    : changeStats.level < 200 ? changeStats.level >= 150 ? 16 : 6 : changeStats.level < 240 ? 27 : 37);
            changeStats.hp = (int) Math.min(Integer.MAX_VALUE, hp < 0 ? Long.MAX_VALUE : hp);
            if (changeStats.finalmaxHP > 0) {
                changeStats.finalmaxHP *= (changeStats.level < 100 ? 4 : changeStats.level < 200 ? 6 : 7);
            } else if (hp > Integer.MAX_VALUE) {
                changeStats.finalmaxHP = hp;
            }
            if (changeStats.finalmaxHP < 0) {
                changeStats.finalmaxHP = Long.MAX_VALUE;
            }
            changeStats.exp *= 4.5 + (changeStats.level >= 200 ? 0.1 : 0)
                    + (ch.getChannelType() == ChannelServer.ChannelType.MVP ? 1 : 0);
            changeStats.watk *= 3;
            changeStats.matk *= 3;
            if (ch.getChannelType() == ChannelServer.ChannelType.混沌) {
                changeStats.PDRate += 30;
                changeStats.MDRate += 30;
            }
            int defRate = 0;
            if (changeStats.level >= 240) {
                defRate = 70;
            } else if (changeStats.level >= 200) {
                defRate = 60;
            } else if (changeStats.level >= 150) {
                defRate = 50;
            }
            changeStats.PDRate += defRate;
            changeStats.MDRate += defRate;
            monster.addMobSize(30);
            monster.setOverrideStats(changeStats);
        }

        spawnAndAddRangedMapObject(monster, (MapleClient c) -> {
            if (GameConstants.isAzwanMap(c.getPlayer().getMapId())) {
                c.announce(MobPacket.spawnMonster(monster,
                        monster.getStats().getSummonType() <= 1 || monster.getStats().getSummonType() == 27 || overwrite
                        ? spawnType
                        : monster.getStats().getSummonType(),
                        0, true));
            } else {
                c.announce(MobPacket.spawnMonster(monster,
                        monster.getStats().getSummonType() <= 1 || monster.getStats().getSummonType() == 27 || overwrite
                        ? spawnType
                        : monster.getStats().getSummonType(),
                        0, false));
            }
        });
        updateMonsterController(monster);

        this.spawnedMonstersOnMap.incrementAndGet();
        String spawnMsg = null;
        switch (monster.getId()) {
            case 9300815:
                spawnMsg = "伴隨著一陣陰森的感覺,紅寶王出現了.";
                break;
        }
        if (spawnMsg != null) {
            broadcastMessage(CWvsContext.broadcastMsg(6, spawnMsg));
        }
    }

    public final int spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        try {
            monster.setMap(this);
            monster.setPosition(pos);

            spawnAndAddRangedMapObject(monster, (MapleClient c) -> {
                if (GameConstants.isAzwanMap(c.getPlayer().getMapId())) {
                    c.announce(MobPacket.spawnMonster(monster, effect, 0, true));
                } else {
                    c.announce(MobPacket.spawnMonster(monster, effect, 0, false));
                }
            });
            updateMonsterController(monster);

            spawnedMonstersOnMap.incrementAndGet();
            return monster.getObjectId();
        } catch (Exception e) {
            return -1;
        }
    }

    public final void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);

        spawnAndAddRangedMapObject(monster, (MapleClient c) -> {
            if (GameConstants.isAzwanMap(c.getPlayer().getMapId())) {
                c.announce(MobPacket.spawnMonster(monster, -4, 0, true));
            } else {
                c.announce(MobPacket.spawnMonster(monster, -4, 0, false));
            }
        });
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public final void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);

        spawnAndAddRangedMapObject(reactor, (MapleClient c) -> {
            c.announce(CField.spawnReactor(reactor));
        });
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public final void spawnRune(final MapleRune rune) {
        rune.setMap(this);

        spawnAndAddRangedMapObject(rune, (MapleClient c) -> {
            rune.sendSpawnData(c);
            c.announce(CWvsContext.enableActions());
        });
    }

    public final void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, (MapleClient c) -> {
            door.sendSpawnData(c);
            c.announce(CWvsContext.enableActions());
        });
    }

    public final void spawnMechDoor(final MapleOpenGate door) {
        spawnAndAddRangedMapObject(door, (MapleClient c) -> {
            c.announce(SkillPacket.spawnOpenGate(door, true));
            c.announce(CWvsContext.enableActions());
        });
    }

    public final void spawnSummon(final MapleSummon summon) {
        summon.updateMap(this);
        spawnAndAddRangedMapObject(summon, (MapleClient c) -> {
            // if (summon != null && c.getPlayer() != null && (!summon.isChangedMap() ||
            // summon.getOwnerId() == c.getPlayer().getId())) {
            c.announce(SummonPacket.spawnSummon(summon, true));
            // }
        });
    }

    public final void spawnFamiliar(final MonsterFamiliar familiar, final boolean respawn) {
        spawnAndAddRangedMapObject(familiar, (MapleClient c) -> {
            if (familiar != null && c.getPlayer() != null) {
                c.announce(FamiliarPacket.spawnFamiliar(familiar, true, respawn));
            }
        });
    }

    public final void spawnExtractor(final MapleExtractor ex) {
        spawnAndAddRangedMapObject(ex, (MapleClient c) -> {
            ex.sendSpawnData(c);
        });
    }

    public final void spawnAffectedArea(final MapleAffectedArea mist, final int duration, boolean fake) {// TODO
        // MIST::添加處理
        spawnAndAddRangedMapObject(mist, (MapleClient c) -> {
            mist.sendSpawnData(c);
        });

        final MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture<?> poisonSchedule;
        if (mist.isPoisonMist() && !mist.isMobMist()) {
            final MapleCharacter owner = getCharacterById(mist.getOwnerId());
            final boolean pvp = owner != null ? owner.inPVP() : false;
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : MapleMap.this.getMapObjectsInRect(mist.getBox(),
                        Collections.singletonList(pvp ? MapleMapObjectType.PLAYER : MapleMapObjectType.MONSTER))) {
                    if (pvp && mist.makeChanceResult() && !((MapleCharacter) mo).hasDOT()
                            && ((MapleCharacter) mo).getId() != mist.getOwnerId()) {
                        ((MapleCharacter) mo).setDOT(mist.getSource().getDOT(), mist.getSourceSkill().getId(),
                                mist.getSkillLevel());
                    } else if (!pvp && mist.makeChanceResult() && !((MapleMonster) mo).isBuffed(MonsterStatus.M_Poison)
                            && owner != null) {
                        ((MapleMonster) mo).applyStatus(owner, new MonsterStatusEffect(MonsterStatus.M_Poison, 1,
                                mist.getSourceSkill().getId(), null, false), true, duration, true, mist.getSource());
                    }
                }
            }, 2000, 2500);
        } else if (mist.isRecovery()) {
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : MapleMap.this.getMapObjectsInRect(mist.getBox(),
                        Collections.singletonList(MapleMapObjectType.PLAYER))) {
                    MapleCharacter chr = (MapleCharacter) mo;
                    if (mist.makeChanceResult() && chr.isAlive()) {
                        chr.addMPHP((int) ((mist.getSource().getX() * (chr.getStat().getMaxHp() / 100.0D))),
                                (int) (mist.getSource().getX() * (chr.getStat().getMaxMp() / 100.0D)));
                    }
                }
            }, 2000, 2500);
        } else if (!mist.isMobMist() && mist.getSourceSkill().getId() == 4121015) {
            final MapleCharacter owner = getCharacterById(mist.getOwnerId());
            final boolean pvp = owner != null ? owner.inPVP() : false;
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : MapleMap.this.getMapObjectsInRect(mist.getBox(),
                        Collections.singletonList(pvp ? MapleMapObjectType.PLAYER : MapleMapObjectType.MONSTER))) {
                    if (pvp && mist.makeChanceResult() && !((MapleCharacter) mo).hasDOT()
                            && ((MapleCharacter) mo).getId() != mist.getOwnerId()) {
                        ((MapleCharacter) mo).setDOT(mist.getSource().getDOT(), mist.getSourceSkill().getId(),
                                mist.getSkillLevel());
                    } else if (!pvp && mist.makeChanceResult() && owner != null) {
                        ((MapleMonster) mo).applyStatus(owner,
                                new MonsterStatusEffect(MonsterStatus.M_PAD, -mist.getSource().getW(),
                                        mist.getSourceSkill().getId(), null, false),
                                false, duration, true, mist.getSource());
                        ((MapleMonster) mo).applyStatus(owner,
                                new MonsterStatusEffect(MonsterStatus.M_MAD, -mist.getSource().getW(),
                                        mist.getSourceSkill().getId(), null, false),
                                false, duration, true, mist.getSource());
                        ((MapleMonster) mo).applyStatus(owner,
                                new MonsterStatusEffect(MonsterStatus.M_Speed, mist.getSource().getY(),
                                        mist.getSourceSkill().getId(), null, false),
                                false, duration, true, mist.getSource());
                    }
                }
            }, 2000, 2500);
        } else if (!mist.isMobMist() && mist.getSourceSkill().getId() == 36121007) {
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : MapleMap.this.getMapObjectsInRect(mist.getBox(),
                        Collections.singletonList(MapleMapObjectType.PLAYER))) {
                    if (((MapleCharacter) mo).getParty() != null
                            && ((MapleCharacter) mo).getParty().getMemberById(mist.getOwnerId()) != null
                            && !((MapleCharacter) mo).isBuffed(mist.getSource())
                            || ((MapleCharacter) mo).getId() == mist.getOwnerId()
                            && !((MapleCharacter) mo).isBuffed(mist.getSource())) {
                        ((MapleCharacter) mo).clearAllCooldowns();
                    }
                }
            }, 2000, 2500);
        } else if (!mist.isMobMist()
                && (mist.getSourceSkill().getId() == 25111206 || mist.getSourceSkill().getId() == 2201009)) {
            final MapleCharacter owner = getCharacterById(mist.getOwnerId());
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : MapleMap.this.getMapObjectsInRect(mist.getBox(),
                        Collections.singletonList(MapleMapObjectType.MONSTER))) {
                    ((MapleMonster) mo).applyStatus(owner, new MonsterStatusEffect(MonsterStatus.M_Freeze, 1,
                            mist.getSourceSkill().getId(), null, false), false, duration, true, mist.getSource());
                }
            }, 2000, 2500);
        } else if (!mist.isMobMist() && mist.getSourceSkill().getId() == 131001307) {
            final MapleCharacter owner = getCharacterById(mist.getOwnerId());
            final boolean pvp = owner != null ? owner.inPVP() : false;
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : MapleMap.this.getMapObjectsInRect(mist.getBox(),
                        Collections.singletonList(pvp ? MapleMapObjectType.PLAYER : MapleMapObjectType.MONSTER))) {
                    if (pvp && mist.makeChanceResult() && !((MapleCharacter) mo).hasDOT()
                            && ((MapleCharacter) mo).getId() != mist.getOwnerId()) {
                        ((MapleCharacter) mo).setDOT(mist.getSource().getDOT(), mist.getSourceSkill().getId(),
                                mist.getSkillLevel());
                    } else if (!pvp && mist.makeChanceResult() && owner != null) {
                        ((MapleMonster) mo).applyStatus(owner,
                                new MonsterStatusEffect(MonsterStatus.M_PDR, mist.getSource().getX(),
                                        mist.getSourceSkill().getId(), null, false),
                                false, duration, true, mist.getSource());
                        ((MapleMonster) mo).applyStatus(owner,
                                new MonsterStatusEffect(MonsterStatus.M_Speed, mist.getSource().getY(),
                                        mist.getSourceSkill().getId(), null, false),
                                false, duration, true, mist.getSource());
                        ((MapleMonster) mo).applyStatus(owner,
                                new MonsterStatusEffect(MonsterStatus.M_PinkbeanFlowerPot, 151707,
                                        mist.getSourceSkill().getId(), null, false),
                                false, duration, true, mist.getSource());
                    }
                }
            }, 2000, 2500);
        } else if (mist.isGiveBuff() && !mist.isMobMist()) {
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : getMapObjectsInRect(mist.getBox(),
                        Collections.singletonList(MapleMapObjectType.PLAYER))) {
                    MapleCharacter chr = (MapleCharacter) mo;
                    if (((chr.getParty() != null && chr.getParty().getMemberById(mist.getOwnerId()) != null)
                            || // 給隊友加Buff
                            chr.getId() == mist.getOwnerId()) // 給自身加Buff
                            && !chr.isBuffed(mist.getSource())) { // 自身沒有這個Buff
                        mist.getSource().applyBuffEffect(chr, mist.getSource().getDuration());
                    }
                }
                // 離開範圍取消Buff
                MapleCharacter ower = getCharacterById_InMap(mist.getOwnerId());
                if (ower == null) {
                } else if (ower.getParty() == null) { // 無隊伍取消範圍外自身Buff
                    if (!getCharactersIntersect(mist.getBox()).contains(ower) && ower.isBuffed(mist.getSource())) {
                        boolean cancel = true;
                        for (MapleMapObject mo : getMapObjectsInRange(ower.getPosition(),
                                GameConstants.maxViewRangeSq(),
                                Collections.singletonList(MapleMapObjectType.AFFECTED_AREA))) {
                            MapleAffectedArea mi = (MapleAffectedArea) mo;
                            if (mi.getSourceSkill().getId() == mist.getSourceSkill().getId()
                                    && getCharactersIntersect(mi.getBox()).contains(ower)) {
                                cancel = false;
                                break;
                            }
                        }
                        if (cancel) {
                            ower.cancelEffect(mist.getSource(), false, -1);
                        }
                    }
                } else { // 取消範圍外隊伍Buff
                    for (MaplePartyCharacter mo : ower.getParty().getMembers()) {
                        MapleCharacter chr = MapleCharacter.getOnlineCharacterById(mo.getId());
                        if (chr != null && !getCharactersIntersect(mist.getBox()).contains(chr)
                                && chr.isBuffed(mist.getSource())) {
                            boolean cancel = true;
                            for (MapleMapObject m : getMapObjectsInRange(chr.getPosition(),
                                    GameConstants.maxViewRangeSq(),
                                    Collections.singletonList(MapleMapObjectType.AFFECTED_AREA))) {
                                MapleAffectedArea mi = (MapleAffectedArea) m;
                                MapleCharacter miOwer = MapleCharacter.getCharacterById(mi.getOwnerId());
                                if (miOwer.getParty() != null
                                        && mi.getSourceSkill().getId() == mist.getSourceSkill().getId()
                                        && getCharactersIntersect(mi.getBox()).contains(chr)) {
                                    for (MaplePartyCharacter mpc : miOwer.getParty().getMembers()) {
                                        if (mpc.getId() == chr.getId()) {
                                            cancel = false;
                                            break;
                                        }
                                    }
                                    if (!cancel) {
                                        break;
                                    }
                                }
                            }
                            if (cancel) {
                                chr.cancelEffect(mist.getSource(), false, -1);
                            }
                        }
                    }
                }
            }, 2000, 2500);
        } else {
            int id = mist.isMobMist() ? mist.getMobSkill().getSkillId() : mist.getSourceSkill().getId();
            String from = mist.isMobMist() ? "怪物" : "玩家：" + mist.getOwnerId();
            System.out.println("[未處理AffectedArea] 來自 " + from + " 技能ID：" + id + "");
            poisonSchedule = null;
        }
        mist.setPoisonSchedule(poisonSchedule);
        mist.setSchedule(tMan.schedule(() -> {
            broadcastMessage(CField.removeAffectedArea(mist.getObjectId(), false));
            removeMapObject(mist);
            if (poisonSchedule != null) {
                poisonSchedule.cancel(false);
            }
        }, duration));
    }

    public final void disappearingItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item,
            final Point pos) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(CField.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 3),
                drop.getTruePosition());
    }

    public final void spawnMesoDrop(final int meso, final Point position, final MapleMapObject dropper,
            final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, (MapleClient c) -> {
            c.announce(CField.dropItemFromMapObject(mdrop, dropper.getTruePosition(), droppos, (byte) 1));
        });
        if (!everlast) {
            mdrop.registerExpire(120000);
            if (droptype == 0 || droptype == 1) {
                mdrop.registerFFA(30000);
            }
        }
    }

    public final void spawnMobMesoDrop(final int meso, final Point position, final MapleMapObject dropper,
            final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final MapleMapItem mdrop = new MapleMapItem(meso, position, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, (MapleClient c) -> {
            c.announce(CField.dropItemFromMapObject(mdrop, dropper.getTruePosition(), position, (byte) 1));
        });

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
    }

    public final void spawnMobDrop(final Item idrop, final Point dropPos, final MapleMonster mob,
            final MapleCharacter chr, final byte droptype, final int questid) {
        final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr, droptype, false, questid);

        spawnAndAddRangedMapObject(mdrop, (MapleClient c) -> {
            if (c != null && c.getPlayer() != null && (questid <= 0 || c.getPlayer().getQuestStatus(questid) == 1)
                    && (idrop.getItemId() / 10000 != 238
                    || c.getPlayer().getMonsterBook().getLevelByCard(idrop.getItemId()) >= 2)
                    && mob != null && dropPos != null) {
                c.announce(CField.dropItemFromMapObject(mdrop, mob.getTruePosition(), dropPos, (byte) 1));
            }
        });
        // broadcastMessage(CField.dropItemFromMapObject(mdrop, mob.getTruePosition(),
        // dropPos, (byte) 0));

        if (mob.getStats().getWP() > 0 && MapleJob.is神之子(chr.getJob())) {
            chr.addWeaponPoint(mob.getStats().getWP());
        }

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
        activateItemReactors(mdrop, chr.getClient());
    }

    public final void spawnRandDrop() {
        if (mapid != 910000000 || channel != 1) {
            return; // fm, ch1
        }

        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject o : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                if (((MapleMapItem) o).isRandDrop()) {
                    return;
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                final Point pos = new Point(Randomizer.nextInt(800) + 531, -806);
                final int theItem = Randomizer.nextInt(1000);
                int itemid = 0;
                if (theItem < 950) { // 0-949 = normal, 950-989 = rare, 990-999 = super
                    itemid = GameConstants.normalDrops[Randomizer.nextInt(GameConstants.normalDrops.length)];
                } else if (theItem < 990) {
                    itemid = GameConstants.rareDrops[Randomizer.nextInt(GameConstants.rareDrops.length)];
                } else {
                    itemid = GameConstants.superDrops[Randomizer.nextInt(GameConstants.superDrops.length)];
                }
                spawnAutoDrop(itemid, pos);
            }
        }, 20000);
    }

    public final void spawnAutoDrop(final int itemid, final Point pos) {
        Item idrop = null;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
            idrop = ii.randomizeStats((Equip) ii.getEquipById(itemid));
        } else {
            idrop = new Item(itemid, (byte) 0, (short) 1, 0);
        }
        idrop.setGMLog("從自動掉寶中獲得, 地圖:" + this + ", 時間:" + FileoutputUtil.CurrentReadable_Time());
        final MapleMapItem mdrop = new MapleMapItem(pos, idrop);
        spawnAndAddRangedMapObject(mdrop, (MapleClient c) -> {
            c.announce(CField.dropItemFromMapObject(mdrop, pos, pos, (byte) 1));
        });
        broadcastMessage(CField.dropItemFromMapObject(mdrop, pos, pos, (byte) 0));
        if (itemid / 10000 != 291) {
            mdrop.registerExpire(120000);
        }
    }

    public final void spawnItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item,
            Point pos, final boolean ffaDrop, final boolean playerDrop) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 2, playerDrop);

        spawnAndAddRangedMapObject(drop, (MapleClient c) -> {
            c.announce(CField.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 1));
        });
        broadcastMessage(CField.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 0));

        if (!everlast) {
            drop.registerExpire(120000);
            activateItemReactors(drop, owner.getClient());
        }
    }

    private void activateItemReactors(final MapleMapItem drop, final MapleClient c) {
        final Item item = drop.getItem();

        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (final MapleMapObject o : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                final MapleReactor react = (MapleReactor) o;

                if (react.getReactorType() == 100) {
                    if (item.getItemId() == GameConstants.getCustomReactItem(react.getReactorId(),
                            react.getReactItem().getLeft()) && react.getReactItem().getRight() == item.getQuantity()) {
                        if (react.getArea().contains(drop.getTruePosition())) {
                            if (!react.isTimerActive()) {
                                MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
                                react.setTimerActive(true);
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public int getItemsSize() {
        return mapobjects.get(MapleMapObjectType.ITEM).size();
    }

    public int getExtractorSize() {
        return mapobjects.get(MapleMapObjectType.EXTRACTOR).size();
    }

    public int getMobsSize() {
        return mapobjects.get(MapleMapObjectType.MONSTER).size();
    }

    public List<MapleMapItem> getAllItems() {
        return getAllItemsThreadsafe();
    }

    public List<MapleMapItem> getAllItemsThreadsafe() {
        ArrayList<MapleMapItem> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                ret.add((MapleMapItem) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return ret;
    }

    public Point getPointOfItem(int itemid) {
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                MapleMapItem mm = ((MapleMapItem) mmo);
                if (mm.getItem() != null && mm.getItem().getItemId() == itemid) {
                    return mm.getPosition();
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return null;
    }

    public List<MapleAffectedArea> getAllMistsThreadsafe() {
        ArrayList<MapleAffectedArea> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.AFFECTED_AREA).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.AFFECTED_AREA).values()) {
                ret.add((MapleAffectedArea) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.AFFECTED_AREA).readLock().unlock();
        }
        return ret;
    }

    public final void returnEverLastItem(final MapleCharacter chr) {
        for (final MapleMapObject o : getAllItemsThreadsafe()) {
            final MapleMapItem item = ((MapleMapItem) o);
            if (item.getOwner() == chr.getId()) {
                item.setPickedUp(true);
                broadcastMessage(CField.removeItemFromMap(item.getObjectId(), 2, chr.getId()), item.getTruePosition());
                if (item.getMeso() > 0) {
                    chr.gainMeso(item.getMeso(), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(chr.getClient(), item.getItem(), false,
                            item.getDropper() instanceof MapleMonster);
                }
                removeMapObject(item);
            }
        }
        spawnRandDrop();
    }

    public final void talkMonster(final String msg, final int itemId, final int objectid) {
        if (itemId > 0) {
            startMapEffect(msg, itemId, false);
        }
        broadcastMessage(MobPacket.talkMonster(objectid, itemId, msg)); // 5120035
        broadcastMessage(MobPacket.removeTalkMonster(objectid));
    }

    public final void startMapEffect(final String msg, final int itemId) {
        startMapEffect(msg, itemId, false);
    }

    public final void startMapEffect(final String msg, final int itemId, final boolean jukebox) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        mapEffect.setJukebox(jukebox);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (mapEffect != null) {
                    broadcastMessage(mapEffect.makeDestroyData());
                    mapEffect = null;
                }
            }
        }, jukebox ? 300000 : 30000);
    }

    public final void startExtendedMapEffect(final String msg, final int itemId) {
        broadcastMessage(CField.startMapEffect(msg, itemId, true));
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                broadcastMessage(CField.removeMapEffect());
                broadcastMessage(CField.startMapEffect(msg, itemId, false));
                // dont remove mapeffect.
            }
        }, 60000);
    }

    public final void startSimpleMapEffect(final String msg, final int itemId) {
        broadcastMessage(CField.startMapEffect(msg, itemId, true));
    }

    public final void startJukebox(final String msg, final int itemId) {
        startMapEffect(msg, itemId, true);
    }

    public final void addPlayer(final MapleCharacter chr) {
        mapobjectlocks.get(MapleMapObjectType.PLAYER).writeLock().lock();
        try {
            mapobjects.get(MapleMapObjectType.PLAYER).put(chr.getObjectId(), chr);
        } finally {
            mapobjectlocks.get(MapleMapObjectType.PLAYER).writeLock().unlock();
        }

        charactersLock.writeLock().lock();
        try {
            characters.add(chr);
        } finally {
            charactersLock.writeLock().unlock();
        }
        chr.setChangeTime();
        if (GameConstants.isTeamMap(mapid) && !chr.inPVP()) {
            chr.setTeam(getAndSwitchTeam() ? 0 : 1);
        }
        final byte[] packet = CField.spawnPlayerMapobject(chr);
        if (!chr.isHidden()) {
            broadcastMessage(chr, packet, false);
            if (chr.isIntern() && speedRunStart > 0) {
                endSpeedRun();
                broadcastMessage(CWvsContext.broadcastMsg(5, "The speed run has ended."));
            }
            broadcastMessage(chr, CField.getEffectSwitch(chr.getId(), chr.getEffectSwitch()), true);
        } else {
            broadcastGMMessage(chr, packet, false);
            broadcastGMMessage(chr, CField.getEffectSwitch(chr.getId(), chr.getEffectSwitch()), true);
        }
        int burningFieldStep = getBurningFieldStep();
        if (burningFieldStep > 0) {
            chr.getClient().announce(CField.EffectPacket.showEffect(true, chr, UserEffectOpcode.UserEffect_TextEffect,
                    new int[]{50, 1500, 4, 0, -200, 1, 4, 2}, new String[]{"#fn哥德 ExtraBold##fs26#          燃燒"
                        + burningFieldStep + "階段 : 經驗值追加贈送 " + burningFieldStep + "0%！！   "},
                    null, null));
        }

        if (!chr.isClone()) {
            if (!onFirstUserEnter.equals("")) {
                if (getCharactersSize() == 1) {
                    MapScriptMethods.startScript_FirstUser(chr.getClient(), onFirstUserEnter);
                }
            }

            if (!onUserEnter.equals("")) {
                MapScriptMethods.startScript_User(chr.getClient(), onUserEnter);
            }

            sendObjectPlacement(chr);

            GameConstants.achievementRatio(chr.getClient());
            chr.getClient().announce(packet);
            // chr.getClient().announce(CField.spawnFlags(nodes.getFlags()));
            if (GameConstants.isTeamMap(mapid) && !chr.inPVP()) {
                chr.getClient().announce(CField.showEquipEffect(chr.getTeam()));
            }
            switch (mapid) {
                case 809000101:
                case 809000201:
                    chr.getClient().announce(CField.showEquipEffect());
                    break;
                case 689000000:
                case 689000010:
                    chr.getClient().announce(CField.getCaptureFlags(this));
                    break;
            }
        }
        for (final MaplePet pet : chr.getSummonedPets()) {
            pet.setPos(chr.getTruePosition());
            chr.getClient().announce(PetPacket.updatePet(
                    chr.getInventory(MapleInventoryType.CASH).getItem((short) (byte) pet.getInventoryPosition())));
            chr.getClient().announce(PetPacket.showPet(chr, pet, false, false, true));
            chr.getClient()
                    .announce(PetPacket.showPetUpdate(chr, pet.getUniqueId(), (byte) (pet.getSummonedValue() - 1)));
        }
        if (chr.getSummonedFamiliar() != null) {
            chr.spawnFamiliar(chr.getSummonedFamiliar(), true);
        }
        if (chr.getAndroid() != null) {
            chr.getAndroid().setPos(chr.getPosition());
            broadcastMessage(CField.spawnAndroid(chr, chr.getAndroid()));
        }
        if (chr.getParty() != null && !chr.isClone()) {
            chr.silentPartyUpdate();
            chr.getClient().announce(PartyPacket.updateParty(chr.getClient().getChannel(), chr.getParty(),
                    PartyOperation.PartyRes_LoadParty_Done, null));
        }

        List<QuickMoveNPC> qmn = new LinkedList<>();
        if (!chr.isInBlockedMap()) {
            for (QuickMove qm : QuickMove.values()) {
                if (qm.getMap() == chr.getMapId()) {
                    long npcs = qm.getNPCFlag();
                    for (QuickMoveNPC npc : QuickMoveNPC.values()) {
                        if (npc.check(npcs) && npc.show(mapid) && !npc.check(QuickMove.GLOBAL_NPC)) {
                            qmn.add(npc);
                        }
                    }
                    break;
                }
            }
            if (QuickMove.GLOBAL_NPC != 0 && !GameConstants.isBossMap(chr.getMapId())
                    && !GameConstants.isTutorialMap(chr.getMapId())
                    && (chr.getMapId() / 100 != 9100000 || chr.getMapId() == 910000000)) {
                for (QuickMoveNPC npc : QuickMoveNPC.values()) {
                    if (npc.check(QuickMove.GLOBAL_NPC) && npc.show(mapid)) {
                        qmn.add(npc);
                    }
                }
            }
        }
        chr.getClient().announce(CField.getQuickMoveInfo(qmn));

        if (getNPCById(9073000) != null && getId() == 931050410) {
            chr.getClient().announce(CField.NPCPacket.toggleNPCShow(getNPCById(9073000).getObjectId(), true));
        }
        if (MapleJob.is幻影俠盜(chr.getJob())) {
            chr.getClient().announce(PhantomPacket.updateCardStack(chr.getCardStack()));
        }
        if (!chr.isClone()) {
            final List<MapleSummon> ss = chr.getSummonsReadLock();
            try {
                for (MapleSummon summon : ss) {
                    summon.setPosition(chr.getTruePosition());
                    chr.addVisibleMapObject(summon);
                    this.spawnSummon(summon);
                }
            } finally {
                chr.unlockSummonsReadLock();
            }
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (timeLimit > 0 && getReturnMap() != null && !chr.isClone()) {
            chr.startMapTimeLimitTask(timeLimit, this.getReturnMap());
        } else if (timeLimit > 0 && getForcedReturnMap() != null && !chr.isClone()) {
            chr.startMapTimeLimitTask(timeLimit, this.getForcedReturnMap());
        }
        if (chr.getBuffedValue(MapleBuffStat.RideVehicle) != null && !MapleJob.is末日反抗軍(chr.getJob())) {
            if (FieldLimitType.Mount.check(fieldLimit)) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
            }
        }
        if (!chr.isClone()) {
            if (hasClock()) {
                final Calendar cal = Calendar.getInstance();
                chr.getClient().announce((CField.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
                        cal.get(Calendar.SECOND))));
            }

            EventInstanceManager eim = chr.getEventInstance();
            if (eim != null) {
                // 顯示計時器
                if (eim.isTimerStarted()) {
                    if (chr.inPVP()) {
                        chr.getClient().announce(CField.getPVPClock(Integer.parseInt(eim.getProperty("type")),
                                (int) (eim.getTimeLeft() / 1000)));
                    } else {
                        chr.getClient().announce(CField.getClock((int) (eim.getTimeLeft() / 1000)));
                    }
                }

                // 顯示剩餘死亡次數
                if (eim.getDeathCount(chr.getId()) != -1 && eim.isShowDeathCountMap(mapid)) {
                    chr.getClient().announce(CField.getDeathCountInfo(false, eim.getDeathCount(chr.getId())));
                }

                // 仇恨排名
                List<String> rank = eim.getAggroRank(getId());
                if (rank != null && !rank.isEmpty()) {
                    chr.getClient().announce(CField.getAggroRankInfo(rank));
                }

                // 有怪物嘉年華隊伍
                if (chr.getCarnivalParty() != null) {
                    eim.onMapLoad(chr);
                }
            }

            MapleEvent.mapLoad(chr, channel);
            if (getSquadBegin() != null && getSquadBegin().getTimeLeft() > 0 && getSquadBegin().getStatus() == 1) {
                chr.getClient().announce(CField.getClock((int) (getSquadBegin().getTimeLeft() / 1000)));
            }
            if (mapid / 1000 != 105100 && mapid / 100 != 8020003 && mapid / 100 != 8020008 && mapid != 271040100) { // no
                // boss_balrog/2095/coreblaze/auf/cygnus.
                // but
                // coreblaze/auf/cygnus
                // does
                // AFTER
                final MapleSquad sqd = getSquadByMap(); // for all squads
                final EventManager em = getEMByMap();
                if (!squadTimer && sqd != null && chr.getName().equals(sqd.getLeaderName()) && em != null
                        && em.getProperty("leader") != null && em.getProperty("leader").equals("true") && checkStates) {
                    // leader? display
                    doShrine(false);
                    squadTimer = true;
                }
            }
            if (getNumMonsters() > 0 && (mapid == 280030001 || mapid == 240060201 || mapid == 280030000
                    || mapid == 240060200 || mapid == 220080001 || mapid == 541020800 || mapid == 541010100)) {
                String music = "Bgm09/TimeAttack";
                switch (mapid) {
                    case 240060200:
                    case 240060201:
                        music = "Bgm14/HonTale";
                        break;
                    case 280030000:
                    case 280030001:
                        music = "Bgm06/FinalFight";
                        break;
                }
                chr.getClient().announce(CField.musicChange(music));
                // maybe timer too for zak/ht
            }
            for (final WeakReference<MapleCharacter> chrz : chr.getClones()) {
                if (chrz.get() != null) {
                    chrz.get().setPosition(chr.getTruePosition());
                    chrz.get().setMap(this);
                    addPlayer(chrz.get());
                }
            }
            switch (mapid) {
                case 940001010:// 凱薩
                case 931050930:// 傑諾
                case 927020010:// 夜光
                case 927000000:// 惡魔殺手 惡魔復仇者
                case 914000000:// 狂郎勇士
                case 910150003:// 精靈
                case 807100102:// 陰陽師
                case 807100001:// 劍豪
                    chr.getClient().announce(CWvsContext.temporaryStats_Aran());// 暫存能力值設定
                    break;
                case 931050040:// 惡魔殺手 惡魔復仇者
                case 927020071:// 夜光
                case 910150002:// 精靈
                case 807000000:// 陰陽師
                case 807100002:// 劍豪
                case 400000000:// 凱薩
                case 310010000:// 傑諾
                case 140090000:// 狂郎勇士

                case 105100401:// 巴洛谷
                case 105100301:// 巴洛谷
                case 105100100:// 巴洛谷
                    chr.getClient().announce(CWvsContext.onForcedStatReset());// 暫存能力值重製
                    break;
                case 105100300:// 巴洛谷 限制
                    chr.getClient().announce(CWvsContext.temporaryStats_Balrog(chr));
                    break;
            }
        }
        if (MapleJob.is龍魔導士(chr.getJob()) && chr.getJob() >= 2200) {
            if (chr.getDragon() == null) {
                chr.makeDragon();
            } else {
                chr.getDragon().setPosition(chr.getPosition());
            }
            if (chr.getDragon() != null) {
                broadcastMessage(CField.spawnDragon(chr.getDragon()));
            }
        }
        if (chr.getTotalSkillLevel(40020109) > 0 && MapleJob.is陰陽師(chr.getJob())) { // 花狐
            if (chr.getHaku() == null) {
                chr.makeHaku();
            } else {
                chr.getHaku().setPosition(chr.getPosition());
            }
            if (chr.getHaku() != null) {
                broadcastMessage(chr, CField.spawnHaku(chr.getHaku()), false);
                if (chr.getBuffSource(MapleBuffStat.ChangeFoxMan) > 0) {
                    chr.getHaku().setFigureHaku(true);
                    broadcastMessage(chr, CField.spawnFigureHaku(chr.getHaku()), true);
                    broadcastMessage(chr, CField.hakuChangeEffect(chr.getId()), true);
                    broadcastMessage(chr, CField.hakuChange(chr.getHaku()), true);
                }
            }
        }
        if (permanentWeather > 0) {
            chr.getClient().announce(CField.startMapEffect("", permanentWeather, false)); // snow, no msg
        }
        if (getPlatforms().size() > 0) {
            chr.getClient().announce(CField.getMovingPlatforms(this));
        }
        if (environment.size() > 0) {
            chr.getClient().announce(CField.getUpdateEnvironment(this));
        }
        // if (partyBonusRate > 0) {
        // chr.dropMessage(-1, partyBonusRate + "% additional EXP will be applied per
        // each party member here.");
        // chr.dropMessage(-1, "You've entered the party play zone.");
        // }
        if (isTown()) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.Dance);
        }
        if (!canSoar()) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.Flying);
        }
        if (chr.getJob() < 3200 || chr.getJob() > 3212) {
            // chr.cancelEffectFromBuffStat(MapleBuffStat.AURA);
        }
        chr.getClient().announce(CWvsContext.showChronosphere(chr));
        if (chr.getCustomBGState() == 1) {
            chr.removeBGLayers();
        }
    }

    public int getNumItems() {
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            return mapobjects.get(MapleMapObjectType.ITEM).size();
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
    }

    public int getNumMonsters() {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            return mapobjects.get(MapleMapObjectType.MONSTER).size();
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public void doShrine(final boolean spawned) { // false = entering map, true = defeated
        if (squadSchedule != null) {
            cancelSquadSchedule(true);
        }
        final MapleSquad sqd = getSquadByMap();
        if (sqd == null) {
            return;
        }
        final int mode = (mapid == 280030000 ? 1
                : (mapid == 280030001 ? 2 : (mapid == 240060200 || mapid == 240060201 ? 3 : 0)));
        // chaos_horntail message for horntail too because it looks nicer
        final EventManager em = getEMByMap();
        if (sqd != null && em != null && getCharactersSize() > 0) {
            final String leaderName = sqd.getLeaderName();
            final String state = em.getProperty("state");
            final Runnable run;
            MapleMap returnMapa = getForcedReturnMap();
            if (returnMapa == null || returnMapa.getId() == mapid) {
                returnMapa = getReturnMap();
            }
            switch (mode) {
                case 1:
                case 2:
                    // chaoszakum
                    broadcastMessage(CField.showChaosZakumShrine(spawned, 5));
                    break;
                case 3:
                    // ht/chaosht
                    broadcastMessage(CField.showChaosHorntailShrine(spawned, 5));
                    break;
                default:
                    broadcastMessage(CField.showHorntailShrine(spawned, 5));
                    break;
            }
            if (spawned) { // both of these together dont go well
                broadcastMessage(CField.getClock(300)); // 5 min
            }
            final MapleMap returnMapz = returnMapa;
            if (!spawned) { // no monsters yet; inforce timer to spawn it quickly
                final List<MapleMonster> monsterz = getAllMonstersThreadsafe();
                final List<Integer> monsteridz = new ArrayList<>();
                for (MapleMapObject m : monsterz) {
                    monsteridz.add(m.getObjectId());
                }
                run = new Runnable() {
                    @Override
                    public void run() {
                        final MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        if (MapleMap.this.getCharactersSize() > 0 && MapleMap.this.getNumMonsters() == monsterz.size()
                                && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName)
                                && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            boolean passed = monsterz.isEmpty();
                            for (MapleMapObject m : MapleMap.this.getAllMonstersThreadsafe()) {
                                for (int i : monsteridz) {
                                    if (m.getObjectId() == i) {
                                        passed = true;
                                        break;
                                    }
                                }
                                if (passed) {
                                    break;
                                } // even one of the monsters is the same
                            }
                            if (passed) {
                                // are we still the same squad? are monsters still == 0?
                                byte[] packet;
                                if (mode == 1 || mode == 2) { // chaoszakum
                                    packet = CField.showChaosZakumShrine(spawned, 0);
                                } else {
                                    packet = CField.showHorntailShrine(spawned, 0); // chaoshorntail message is weird
                                }
                                for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { // warp all in map
                                    chr.getClient().announce(packet);
                                    chr.changeMap(returnMapz, returnMapz.getPortal(0)); // hopefully event will still
                                    // take care of everything once
                                    // warp out
                                }
                                checkStates("");
                                resetFully();
                            }
                        }

                    }
                };
            } else { // inforce timer to gtfo
                run = new Runnable() {
                    @Override
                    public void run() {
                        MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        // we dont need to stop clock here because they're getting warped out anyway
                        if (MapleMap.this.getCharactersSize() > 0 && sqnow != null && sqnow.getStatus() == 2
                                && sqnow.getLeaderName().equals(leaderName)
                                && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            // are we still the same squad? monsters however don't count
                            byte[] packet;
                            if (mode == 1 || mode == 2) { // chaoszakum
                                packet = CField.showChaosZakumShrine(spawned, 0);
                            } else {
                                packet = CField.showHorntailShrine(spawned, 0); // chaoshorntail message is weird
                            }
                            for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { // warp all in map
                                chr.getClient().announce(packet);
                                chr.changeMap(returnMapz, returnMapz.getPortal(0)); // hopefully event will still take
                                // care of everything once warp out
                            }
                            checkStates("");
                            resetFully();
                        }
                    }
                };
            }
            squadSchedule = MapTimer.getInstance().schedule(run, 300000); // 5 mins
        }
    }

    public final MapleSquad getSquadByMap() {
        MapleSquadType zz = null;
        switch (mapid) {
            case 105100400:
            case 105100300:
                zz = MapleSquadType.bossbalrog;
                break;
            case 280030000:
                zz = MapleSquadType.zak;
                break;
            case 280030001:
                zz = MapleSquadType.chaoszak;
                break;
            case 240060200:
                zz = MapleSquadType.horntail;
                break;
            case 240060201:
                zz = MapleSquadType.chaosht;
                break;
            case 270050100:
                zz = MapleSquadType.pinkbean;
                break;
            case 802000111:
                zz = MapleSquadType.nmm_squad;
                break;
            case 802000211:
                zz = MapleSquadType.vergamot;
                break;
            case 802000311:
                zz = MapleSquadType.tokyo_2095;
                break;
            case 802000411:
                zz = MapleSquadType.dunas;
                break;
            case 802000611:
                zz = MapleSquadType.nibergen_squad;
                break;
            case 802000711:
                zz = MapleSquadType.dunas2;
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                zz = MapleSquadType.core_blaze;
                break;
            case 802000821:
            case 802000823:
                zz = MapleSquadType.aufheben;
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                zz = MapleSquadType.vonleon;
                break;
            case 551030200:
                zz = MapleSquadType.scartar;
                break;
            case 271040100:
                zz = MapleSquadType.cygnus;
                break;
            case 262030300:
                zz = MapleSquadType.hilla;
                break;
            case 262031300:
                zz = MapleSquadType.darkhilla;
                break;
            case 272030400:
                zz = MapleSquadType.arkarium;
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getMapleSquad(zz);
    }

    public final MapleSquad getSquadBegin() {
        if (squad != null) {
            return ChannelServer.getInstance(channel).getMapleSquad(squad);
        }
        return null;
    }

    public final EventManager getEMByMap() {
        String em;
        switch (mapid) {
            case 105100400:
                em = "BossBalrog_EASY";
                break;
            case 105100300:
                em = "BossBalrog_NORMAL";
                break;
            case 280030000:
                em = "ZakumBattle";
                break;
            case 240060200:
                em = "HorntailBattle";
                break;
            case 280030001:
                em = "ChaosZakum";
                break;
            case 240060201:
                em = "ChaosHorntail";
                break;
            case 270050100:
                em = "PinkBeanBattle";
                break;
            case 802000111:
                em = "NamelessMagicMonster";
                break;
            case 802000211:
                em = "Vergamot";
                break;
            case 802000311:
                em = "2095_tokyo";
                break;
            case 802000411:
                em = "Dunas";
                break;
            case 802000611:
                em = "Nibergen";
                break;
            case 802000711:
                em = "Dunas2";
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                em = "CoreBlaze";
                break;
            case 802000821:
            case 802000823:
                em = "Aufhaven";
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                em = "VonLeonBattle";
                break;
            case 551030200:
                em = "ScarTarBattle";
                break;
            case 271040100:
                em = "CygnusBattle";
                break;
            case 262030300:
                em = "HillaBattle";
                break;
            case 262031300:
                em = "DarkHillaBattle";
                break;
            case 272020110:
            case 272030400:
                em = "ArkariumBattle";
                break;
            case 955000100:
            case 955000200:
            case 955000300:
                em = "AswanOffSeason";
                break;
            // case 689010000:
            // em = "PinkZakumEntrance";
            // break;
            // case 689013000:
            // em = "PinkZakumFight";
            // break;
            default:
                if (mapid >= 262020000 && mapid < 262023000) {
                    em = "Azwan";
                    break;
                }
                return null;
        }
        return ChannelServer.getInstance(channel).getEventSM().getEventManager(em);
    }

    public final void removePlayer(final MapleCharacter chr) {
        // log.warn("[dc] [level2] Player {} leaves map {}", new Object[] {
        // chr.getName(), mapid });

        if (everlast) {
            returnEverLastItem(chr);
        }

        charactersLock.writeLock().lock();
        try {
            characters.remove(chr);
        } finally {
            charactersLock.writeLock().unlock();
        }
        removeMapObject(chr);
        broadcastMessage(CField.removePlayerFromMap(chr.getId()));
        for (MapleMonster monster : chr.getControlledMonsters()) {
            monster.setController(null);
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
            updateMonsterController(monster);
        }
        chr.checkFollow();
        chr.removeExtractor();

        if (chr.getSummonedFamiliar() != null) {
            chr.removeVisibleFamiliar();
        }
        List<MapleSummon> toCancel = new ArrayList<>();
        final List<MapleSummon> ss = chr.getSummonsReadLock();
        try {
            for (final MapleSummon summon : ss) {
                broadcastMessage(SummonPacket.removeSummon(summon, true));
                removeMapObject(summon);
                if (summon.getMovementType() == SummonMovementType.不會移動
                        || summon.getMovementType() == SummonMovementType.盤旋攻擊怪死後跟隨
                        || summon.getMovementType() == SummonMovementType.自由移動) {
                    toCancel.add(summon);
                } else {
                    summon.setChangedMap(true);
                }
            }
        } finally {
            chr.unlockSummonsReadLock();
        }
        for (MapleSummon summon : toCancel) {
            chr.removeSummon(summon);
            chr.dispelSkill(summon.getSkill()); // remove the buff
        }
        if (!chr.isClone()) {
            checkStates(chr.getName());
            if (mapid == 109020001) {
                chr.canTalk(true);
            }
            for (final WeakReference<MapleCharacter> chrz : chr.getClones()) {
                if (chrz.get() != null) {
                    removePlayer(chrz.get());
                }
            }
            chr.leaveMap(this);
        }
    }

    public void broadcastGMMessage(MapleCharacter source, byte[] packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, null,
                new Pair<>(source == null ? 1 : source.getGmLevel(), Integer.MAX_VALUE));
    }

    public void broadcasGMtMessage(MapleCharacter source, byte[] packet) {
        broadcastMessage(source, packet, Double.POSITIVE_INFINITY, null,
                new Pair<>(source == null ? 1 : source.getGmLevel(), Integer.MAX_VALUE));
    }

    public void broadcastNONGMMessage(MapleCharacter source, byte[] packet, boolean repeatToSource) {
        broadcastNONGMMessage(repeatToSource ? null : source, packet);
    }

    private void broadcastNONGMMessage(MapleCharacter source, byte[] packet) {
        broadcastMessage(source, packet, Double.POSITIVE_INFINITY, null, new Pair<>(0, 0));
    }

    public final void broadcastMessage(final byte[] packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public final void broadcastMessage(final MapleCharacter source, final byte[] packet, final boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getTruePosition());
    }

    public final void broadcastMessage(final byte[] packet, final Point rangedFrom) {
        broadcastMessage(null, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public final void broadcastMessage(final MapleCharacter source, final byte[] packet, final Point rangedFrom) {
        broadcastMessage(source, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public void broadcastMessage(final MapleCharacter source, final byte[] packet, final double rangeSq,
            final Point rangedFrom) {
        broadcastMessage(source, packet, rangeSq, rangedFrom, new Pair<>(0, Integer.MAX_VALUE));
    }

    public void broadcastMessage(final MapleCharacter source, final byte[] packet, final double rangeSq,
            final Point rangedFrom, Pair<Integer, Integer> allowGmLevel) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr != source) {
                    if ((rangeSq < Double.POSITIVE_INFINITY && rangedFrom.distanceSq(chr.getTruePosition()) <= rangeSq)
                            || rangeSq >= Double.POSITIVE_INFINITY) {
                        if (chr.getGmLevel() >= allowGmLevel.getLeft() && chr.getGmLevel() <= allowGmLevel.getRight()) {
                            chr.getClient().announce(packet);
                        }
                    }
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    private void sendObjectPlacement(final MapleCharacter c) {
        if (c == null || c.isClone()) {
            return;
        }
        for (final MapleMapObject o : getMapObjectsInRange(c.getTruePosition(), c.getRange(),
                GameConstants.rangedMapobjectTypes)) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (!((MapleReactor) o).isAlive()) {
                    continue;
                }
            }
            if (o.getType() == MapleMapObjectType.NPC) {
                int npcId = ((MapleNPC) o).getId();
                if (getId() == 807040000) {
                    if (npcId == 9130031 && !MapleJob.is劍豪(c.getJob())) {
                        continue;
                    }
                    if (npcId == 9130082 && !MapleJob.is陰陽師(c.getJob())) {
                        continue;
                    }
                } else if (getId() == 807040100) {
                    if (npcId == 9130024 && !MapleJob.is劍豪(c.getJob())) {
                        continue;
                    }
                    if (npcId == 9130083 && !MapleJob.is陰陽師(c.getJob())) {
                        continue;
                    }
                }
            }
            o.sendSpawnData(c.getClient());
            c.addVisibleMapObject(o);
        }
    }

    public final List<MaplePortal> getPortalsInRange(final Point from, final double rangeSq) {
        final List<MaplePortal> ret = new ArrayList<>();
        for (MaplePortal type : portals.values()) {
            if (from.distanceSq(type.getPosition()) <= rangeSq && type.getTargetMapId() != mapid
                    && type.getTargetMapId() != 999999999) {
                ret.add(type);
            }
        }
        return ret;
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq) {
        final List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (from.distanceSq(mmo.getTruePosition()) <= rangeSq) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public List<MapleMapObject> getItemsInRange(Point from, double rangeSq) {
        return getMapObjectsInRange(from, rangeSq, Arrays.asList(MapleMapObjectType.ITEM));
    }

    public List<MapleMapObject> getMonstersInRange(Point from, double rangeSq) {
        return getMapObjectsInRange(from, rangeSq, Arrays.asList(MapleMapObjectType.MONSTER));
    }

    public final List<MapleMapObject> getMapObjectsInRange(final Point from, final double rangeSq,
            final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (from.distanceSq(mmo.getTruePosition()) <= rangeSq) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public final List<MapleMapObject> getMapObjectsInRect(final Rectangle box,
            final List<MapleMapObjectType> MapObject_types) {
        final List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (box.contains(mmo.getTruePosition())) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public final List<MapleCharacter> getCharactersIntersect(final Rectangle box) {
        final List<MapleCharacter> ret = new ArrayList<>();
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr.getBounds().intersects(box)) {
                    ret.add(chr);
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public final List<MapleCharacter> getPlayersInRectAndInList(final Rectangle box,
            final List<MapleCharacter> chrList) {
        final List<MapleCharacter> character = new LinkedList<>();

        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter a;
            while (ltr.hasNext()) {
                a = ltr.next();
                if (chrList.contains(a) && box.contains(a.getTruePosition())) {
                    character.add(a);
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return character;
    }

    public final void addPortal(final MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public final MaplePortal getPortal(final String portalname) {
        for (final MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public final MaplePortal getPortal(final int portalid) {
        return portals.get(portalid);
    }

    public final void resetPortals() {
        for (final MaplePortal port : portals.values()) {
            port.setPortalState(true);
        }
    }

    public final void setFootholds(final MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public final MapleFootholdTree getFootholds() {
        return footholds;
    }

    public final int getNumSpawnPoints() {
        return monsterSpawn.size();
    }

    public final void loadMonsterRate(final boolean first) {
        final int spawnSize = monsterSpawn.size();
        if (spawnSize >= 20 || partyBonusRate > 0) {
            maxRegularSpawn = Math.round(spawnSize / monsterRate);
        } else {
            maxRegularSpawn = (int) Math.ceil(spawnSize * monsterRate);
        }
        if (fixedMob > 0) {
            maxRegularSpawn = fixedMob;
        } else if (maxRegularSpawn <= 2) {
            maxRegularSpawn = 2;
        } else if (maxRegularSpawn > spawnSize) {
            maxRegularSpawn = Math.max(10, spawnSize);
        }

        Collection<Spawns> newSpawn = new LinkedList<>();
        Collection<Spawns> newBossSpawn = new LinkedList<>();
        for (final Spawns s : monsterSpawn) {
            if (s.getCarnivalTeam() >= 2) {
                continue; // Remove carnival spawned mobs
            }
            if (s.getMonster().isBoss()) {
                newBossSpawn.add(s);
            } else {
                newSpawn.add(s);
            }
        }
        monsterSpawn.clear();
        monsterSpawn.addAll(newBossSpawn);
        monsterSpawn.addAll(newSpawn);

        if (first && spawnSize > 0) {
            lastSpawnTime = System.currentTimeMillis();
            if (GameConstants.isForceRespawn(mapid)) {
                createMobInterval = 15000;
            }
            respawn(false); // this should do the trick, we don't need to wait upon entering map
        }
    }

    public final SpawnPoint addMonsterSpawn(final MapleMonster monster, final int mobTime, final byte carnivalTeam,
            final String msg) {
        final Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        final SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime, carnivalTeam, msg);
        if (carnivalTeam > -1) {
            monsterSpawn.add(0, sp); // at the beginning
        } else {
            monsterSpawn.add(sp);
        }
        return sp;
    }

    public final void addAreaMonsterSpawn(final MapleMonster monster, Point pos1, Point pos2, Point pos3,
            final int mobTime, final String msg, final boolean shouldSpawn) {
        if (pos1 != null) {
            pos1 = calcPointBelow(pos1);
        }
        if (pos2 != null) {
            pos2 = calcPointBelow(pos2);
        }
        if (pos3 != null) {
            pos3 = calcPointBelow(pos3);
        }
        if (pos1 != null) {
            pos1.y -= 1;
        }
        if (pos2 != null) {
            pos2.y -= 1;
        }
        if (pos3 != null) {
            pos3.y -= 1;
        }
        if (pos1 == null && pos2 == null && pos3 == null) {
            System.out.println("WARNING: mapid " + mapid + ", monster " + monster.getId() + " could not be spawned.");

            return;
        } else if (pos1 != null) {
            if (pos2 == null) {
                pos2 = new Point(pos1);
            }
            if (pos3 == null) {
                pos3 = new Point(pos1);
            }
        } else if (pos2 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos2);
            }
            if (pos3 == null) {
                pos3 = new Point(pos2);
            }
        } else if (pos3 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos3);
            }
            if (pos2 == null) {
                pos2 = new Point(pos3);
            }
        }
        monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg, shouldSpawn));
    }

    public final List<MapleCharacter> getCharacters() {
        return getCharactersThreadsafe();
    }

    public final List<MapleCharacter> getCharactersThreadsafe() {
        final List<MapleCharacter> chars = new ArrayList<>();

        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                chars.add(mc);
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return chars;
    }

    public final MapleCharacter getCharacterByName(final String id) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                if (mc.getName().equalsIgnoreCase(id)) {
                    return mc;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return null;
    }

    public final MapleCharacter getCharacterById_InMap(final int id) {
        return getCharacterById(id);
    }

    public final MapleCharacter getCharacterById(final int id) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                if (mc.getId() == id) {
                    return mc;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return null;
    }

    public final void updateMapObjectVisibility(final MapleCharacter chr, final MapleMapObject mo) {
        if (chr == null || chr.isClone()) {
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.AFFECTED_AREA || mo.getType() == MapleMapObjectType.EXTRACTOR
                    || mo.getType() == MapleMapObjectType.SUMMON || mo.getType() == MapleMapObjectType.FAMILIAR
                    || mo instanceof MapleOpenGate
                    || mo.getTruePosition().distanceSq(chr.getTruePosition()) <= mo.getRange()) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else // monster left view range
        {
            if (!(mo instanceof MapleOpenGate) && mo.getType() != MapleMapObjectType.AFFECTED_AREA
                    && mo.getType() != MapleMapObjectType.EXTRACTOR && mo.getType() != MapleMapObjectType.SUMMON
                    && mo.getType() != MapleMapObjectType.FAMILIAR
                    && mo.getTruePosition().distanceSq(chr.getTruePosition()) > mo.getRange()) {
                chr.removeVisibleMapObject(mo);
                mo.sendDestroyData(chr.getClient());
            } else if (mo.getType() == MapleMapObjectType.MONSTER) { // monster didn't leave view range, and is visible
                if (chr.getTruePosition().distanceSq(mo.getTruePosition()) <= GameConstants.maxViewRangeSq_Half()) {
                    updateMonsterController((MapleMonster) mo);
                }
            }
        }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);

        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                updateMapObjectVisibility(mc, monster);
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    public void movePlayer(final MapleCharacter player, final Point newPosition) {
        player.setPosition(newPosition);
        if (!player.isClone()) {
            try {
                Collection<MapleMapObject> visibleObjects = player.getAndWriteLockVisibleMapObjects();
                ArrayList<MapleMapObject> copy = new ArrayList<>(visibleObjects);
                Iterator<MapleMapObject> itr = copy.iterator();
                while (itr.hasNext()) {
                    MapleMapObject mo = itr.next();
                    if (mo != null && getMapObject(mo.getObjectId(), mo.getType()) == mo) {
                        updateMapObjectVisibility(player, mo);
                    } else if (mo != null) {
                        visibleObjects.remove(mo);
                    }
                }
                for (MapleMapObject mo : getMapObjectsInRange(player.getTruePosition(), player.getRange())) {
                    if (mo != null && !visibleObjects.contains(mo)) {
                        if (mo.getType() == MapleMapObjectType.NPC) {
                            int npcId = ((MapleNPC) mo).getId();
                            if (getId() == 807040000) {
                                if (npcId == 9130031 && !MapleJob.is劍豪(player.getJob())) {
                                    continue;
                                }
                                if (npcId == 9130082 && !MapleJob.is陰陽師(player.getJob())) {
                                    continue;
                                }
                            } else if (getId() == 807040100) {
                                if (npcId == 9130024 && !MapleJob.is劍豪(player.getJob())) {
                                    continue;
                                }
                                if (npcId == 9130083 && !MapleJob.is陰陽師(player.getJob())) {
                                    continue;
                                }
                            }
                        }
                        mo.sendSpawnData(player.getClient());
                        visibleObjects.add(mo);
                    }
                }
            } finally {
                player.unlockWriteVisibleMapObjects();
            }
        }
    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance
                    && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal findClosestPortal(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public String spawnDebug() {
        StringBuilder sb = new StringBuilder("Mobs in map : ");
        sb.append(this.getMobsSize());
        sb.append(" spawnedMonstersOnMap: ");
        sb.append(spawnedMonstersOnMap);
        sb.append(" spawnpoints: ");
        sb.append(monsterSpawn.size());
        sb.append(" maxRegularSpawn: ");
        sb.append(maxRegularSpawn);
        sb.append(" actual monsters: ");
        sb.append(getNumMonsters());
        sb.append(" monster rate: ");
        sb.append(monsterRate);
        sb.append(" fixed: ");
        sb.append(fixedMob);

        return sb.toString();
    }

    public int characterSize() {
        return characters.size();
    }

    public final int getMapObjectSize() {
        return mapobjects.size() + getCharactersSize() - characters.size();
    }

    public final int getCharactersSize() {
        int ret = 0;
        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (!chr.isClone()) {
                    ret++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    public void spawnMonsterOnGroudBelow(MapleMonster mob, Point pos) {
        spawnMonsterOnGroundBelow(mob, pos);
    }

    private class ActivateItemReactor implements Runnable {

        private final MapleMapItem mapitem;
        private final MapleReactor reactor;
        private final MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId(), mapitem.getType())
                    && !mapitem.isPickedUp()) {
                mapitem.expire(MapleMap.this);
                reactor.hitReactor(c);
                reactor.setTimerActive(false);

                if (reactor.getDelay() > 0) {
                    MapTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            reactor.forceHitReactor(c.getPlayer(), (byte) 0);
                        }
                    }, reactor.getDelay());
                }
            } else {
                reactor.setTimerActive(false);
            }
        }
    }

    public void respawn(final boolean force) {
        respawn(force, System.currentTimeMillis());
    }

    public void respawn(final boolean force, final long now) {
        lastSpawnTime = now;
        if (eliteMob != null && System.currentTimeMillis() - lastSpawnEliteMob >= 5 * 60 * 1000) {
            removeMonster(eliteMob);
            eliteMob = null;
        }
        if (!eliteBossMobs.isEmpty()) {
            if (System.currentTimeMillis() - lastSpawnEliteMob >= 30 * 60 * 1000) {
                for (MapleMonster mob : eliteBossMobs) {
                    removeMonster(mob);
                }
                eliteBossMobs.clear();
            } else {
                return;
            }
        }
        // 技能造成額外召喚數量
        int extraSpawnNum = 0;
        double rate = 0.0;
        ChannelServer ch = ChannelServer.getInstance(channel);
        if (ch != null) {
            switch (ch.getChannelType()) {
                case MVP:
                    rate = 0.1;
                    break;
            }
            extraSpawnNum += monsterSpawn.size() * rate;
            rate = 0.0;
        }
        List<Integer> addedSummon = new ArrayList<>();
        List<MapleSummon> summons = getAllSummonsThreadsafe();
        for (MapleSummon summon : summons) {
            if (addedSummon.contains(summon.getSkill())) {
                continue;
            }
            addedSummon.add(summon.getSkill());
            switch (summon.getSkill()) {
                case 80011261: // 輪迴
                    rate = 2.0;
                    break;
                case 42111003: // 鬼神召喚
                    rate = 0.3;
                    break;
            }
            extraSpawnNum += monsterSpawn.size() * rate;
        }
        if (force) { // cpq quick hack
            final int numShouldSpawn = monsterSpawn.size() * FeaturesConfig.monsterSpawn + extraSpawnNum
                    - spawnedMonstersOnMap.get();

            if (numShouldSpawn > 0) {
                int spawned = 0;

                for (Spawns spawnPoint : monsterSpawn) {
                    spawnPoint.spawnMonster(this);
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        } else {
            final int numShouldSpawn = (GameConstants.isForceRespawn(mapid)
                    ? monsterSpawn.size() * FeaturesConfig.monsterSpawn
                    : maxRegularSpawn * FeaturesConfig.monsterSpawn) + extraSpawnNum - spawnedMonstersOnMap.get();
            if (numShouldSpawn > 0) {
                int spawned = 0;

                final List<Spawns> randomSpawn = new ArrayList<>(monsterSpawn);
                Collections.shuffle(randomSpawn);

                for (Spawns spawnPoint : randomSpawn) {
                    if (!isSpawns && spawnPoint.getMobTime() > 0) {
                        continue;
                    }
                    if (spawnPoint.shouldSpawn(lastSpawnTime) || GameConstants.isForceRespawn(mapid)
                            || (monsterSpawn.size() * FeaturesConfig.monsterSpawn < 10 && maxRegularSpawn
                            * FeaturesConfig.monsterSpawn > monsterSpawn.size() * FeaturesConfig.monsterSpawn
                            && partyBonusRate > 0)) {
                        spawnPoint.spawnMonster(this);
                        spawned++;
                    }
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    public static interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    public String getSnowballPortal() {
        int[] teamss = new int[2];
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr.getTruePosition().y > -80) {
                    teamss[0]++;
                } else {
                    teamss[1]++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        if (teamss[0] > teamss[1]) {
            return "st01";
        } else {
            return "st00";
        }
    }

    public boolean isDisconnected(int id) {
        return dced.contains(id);
    }

    public void addDisconnected(int id) {
        dced.add(id);
    }

    public void resetDisconnected() {
        dced.clear();
    }

    public void startSpeedRun() {
        final MapleSquad squad = getSquadByMap();
        if (squad != null) {
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter chr : characters) {
                    if (chr.getName().equals(squad.getLeaderName()) && !chr.isIntern()) {
                        startSpeedRun(chr.getName());
                        return;
                    }
                }
            } finally {
                charactersLock.readLock().unlock();
            }
        }
    }

    public void startSpeedRun(String leader) {
        speedRunStart = System.currentTimeMillis();
        speedRunLeader = leader;
    }

    public void endSpeedRun() {
        speedRunStart = 0;
        speedRunLeader = "";
    }

    public void getRankAndAdd(String leader, String time, ExpeditionType type, long timz, Collection<String> squad) {
        try {
            long lastTime = SpeedRunner.getSpeedRunData(type) == null ? 0 : SpeedRunner.getSpeedRunData(type).right;
            // if(timz > lastTime && lastTime > 0) {
            // return;
            // }
            // Pair<String, Map<Integer, String>>
            StringBuilder rett = new StringBuilder();
            if (squad != null) {
                for (String chr : squad) {
                    rett.append(chr);
                    rett.append(",");
                }
            }
            String z = rett.toString();
            if (squad != null) {
                z = z.substring(0, z.length() - 1);
            }
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO speedruns(`type`, `leader`, `timestring`, `time`, `members`) VALUES (?,?,?,?,?)")) {
                ps.setString(1, type.name());
                ps.setString(2, leader);
                ps.setString(3, time);
                ps.setLong(4, timz);
                ps.setString(5, z);
                ps.executeUpdate();
            }

            if (lastTime == 0) { // great, we just add it
                SpeedRunner.addSpeedRunData(type,
                        SpeedRunner.addSpeedRunData(new StringBuilder(SpeedRunner.getPreamble(type)),
                                new HashMap<Integer, String>(), z, leader, 1, time),
                        timz);
            } else {
                // i wish we had a way to get the rank
                SpeedRunner.removeSpeedRunData(type);
                SpeedRunner.loadSpeedRunData(type);
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
        }
    }

    public long getSpeedRunStart() {
        return speedRunStart;
    }

    public final void disconnectAll() {
        for (MapleCharacter chr : getCharactersThreadsafe()) {
            if (!chr.isIntern()) {
                chr.getClient().disconnect(true, false);
                chr.getClient().getSession().close();
                System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
        }
    }

    public List<MapleNPC> getAllNPCs() {
        return getAllNPCsThreadsafe();
    }

    public List<MapleNPC> getAllNPCsThreadsafe() {
        ArrayList<MapleNPC> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.NPC).values()) {
                ret.add((MapleNPC) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
        return ret;
    }

    public final void resetNPCs() {
        removeNpc(-1);
    }

    public final void resetPQ(int level) {
        resetFully();
        for (MapleMonster mons : getAllMonstersThreadsafe()) {
            mons.changeLevel(level, true);
        }
        resetSpawnLevel(level);
    }

    public final void resetSpawnLevel(int level) {
        for (Spawns spawn : monsterSpawn) {
            if (spawn instanceof SpawnPoint) {
                ((SpawnPoint) spawn).setLevel(level);
            }
        }
    }

    public final void resetFully() {
        resetFully(true);
    }

    public final void resetFully(final boolean respawn) {
        killAllMonsters(false);
        reloadReactors();
        removeDrops();
        resetNPCs();
        resetSpawns();
        resetDisconnected();
        endSpeedRun();
        cancelSquadSchedule(true);
        resetPortals();
        environment.clear();
        if (respawn) {
            respawn(true);
        }
    }

    public final void cancelSquadSchedule(boolean interrupt) {
        squadTimer = false;
        checkStates = true;
        if (squadSchedule != null) {
            squadSchedule.cancel(interrupt);
            squadSchedule = null;
        }
    }

    public final void removeDrops() {
        List<MapleMapItem> items = this.getAllItemsThreadsafe();
        for (MapleMapItem i : items) {
            i.expire(this);
        }
    }

    public final void resetAllSpawnPoint(int mobid, int mobTime) {
        Collection<Spawns> sss = new LinkedList<>(monsterSpawn);
        resetFully();
        monsterSpawn.clear();
        for (Spawns s : sss) {
            MapleMonster newMons = MapleLifeFactory.getMonster(mobid);
            newMons.setF(s.getF());
            newMons.setFh(s.getFh());
            newMons.setPosition(s.getPosition());
            addMonsterSpawn(newMons, mobTime, (byte) -1, null);
        }
        loadMonsterRate(true);
    }

    public final void resetSpawns() {
        boolean changed = false;
        Iterator<Spawns> sss = monsterSpawn.iterator();
        while (sss.hasNext()) {
            if (sss.next().getCarnivalId() > -1) {
                sss.remove();
                changed = true;
            }
        }
        setSpawns(true);
        if (changed) {
            loadMonsterRate(true);
        }
    }

    public final boolean makeCarnivalSpawn(final int team, final MapleMonster newMons, final int num) {
        MonsterPoint ret = null;
        for (MonsterPoint mp : nodes.getMonsterPoints()) {
            if (mp.team == team || mp.team == -1) {
                final Point newpos = calcPointBelow(new Point(mp.x, mp.y));
                newpos.y -= 1;
                boolean found = false;
                for (Spawns s : monsterSpawn) {
                    if (s.getCarnivalId() > -1 && (mp.team == -1 || s.getCarnivalTeam() == mp.team)
                            && s.getPosition().x == newpos.x && s.getPosition().y == newpos.y) {
                        found = true;
                        break; // this point has already been used.
                    }
                }
                if (!found) {
                    ret = mp; // this point is safe for use.
                    break;
                }
            }
        }
        if (ret != null) {
            newMons.setCy(ret.cy);
            newMons.setF(0); // always.
            newMons.setFh(ret.fh);
            newMons.setRx0(ret.x + 50);
            newMons.setRx1(ret.x - 50); // does this matter
            newMons.setPosition(new Point(ret.x, ret.y));
            newMons.setHide(false);
            final SpawnPoint sp = addMonsterSpawn(newMons, 1, (byte) team, null);
            sp.setCarnival(num);
        }
        return ret != null;
    }

    public final boolean makeCarnivalReactor(final int team, final int num) {
        final MapleReactor old = getReactorByName(team + "" + num);
        if (old != null && old.getState() < 5) { // already exists
            return false;
        }
        Point guardz = null;
        final List<MapleReactor> react = getAllReactorsThreadsafe();
        for (Pair<Point, Integer> guard : nodes.getGuardians()) {
            if (guard.right == team || guard.right == -1) {
                boolean found = false;
                for (MapleReactor r : react) {
                    if (r.getTruePosition().x == guard.left.x && r.getTruePosition().y == guard.left.y
                            && r.getState() < 5) {
                        found = true;
                        break; // already used
                    }
                }
                if (!found) {
                    guardz = guard.left; // this point is safe for use.
                    break;
                }
            }
        }
        if (guardz != null) {
            final MapleReactor my = new MapleReactor(MapleReactorFactory.getReactor(9980000 + team), 9980000 + team);
            my.setState((byte) 1);
            my.setName(team + "" + num); // lol
            // with num. -> guardians in factory
            spawnReactorOnGroundBelow(my, guardz);
            final MCSkill skil = MapleCarnivalFactory.getInstance().getGuardian(num);
            for (MapleMonster mons : getAllMonstersThreadsafe()) {
                if (mons.getCarnivalTeam() == team) {
                    skil.getSkill().applyEffect(null, mons, false);
                }
            }
        }
        return guardz != null;
    }

    public final void blockAllPortal() {
        for (MaplePortal p : portals.values()) {
            p.setPortalState(false);
        }
    }

    public boolean getAndSwitchTeam() {
        return getCharactersSize() % 2 != 0;
    }

    public void setSquad(MapleSquadType s) {
        this.squad = s;

    }

    public int getChannel() {
        return channel;
    }

    public int getConsumeItemCoolTime() {
        return consumeItemCoolTime;
    }

    public void setConsumeItemCoolTime(int ciit) {
        this.consumeItemCoolTime = ciit;
    }

    public void setPermanentWeather(int pw) {
        this.permanentWeather = pw;
    }

    public int getPermanentWeather() {
        return permanentWeather;
    }

    public void checkStates(final String chr) {
        if (!checkStates) {
            return;
        }
        final MapleSquad sqd = getSquadByMap();
        final EventManager em = getEMByMap();
        final int size = getCharactersSize();
        if (sqd != null && sqd.getStatus() == 2) {
            sqd.removeMember(chr);
            if (em != null) {
                if (sqd.getLeaderName().equalsIgnoreCase(chr)) {
                    em.setProperty("leader", "false");
                }
                if (chr.equals("") || size == 0) {
                    em.setProperty("state", "0");
                    em.setProperty("leader", "true");
                    cancelSquadSchedule(!chr.equals(""));
                    sqd.clear();
                    sqd.copy();
                }
            }
        }
        if (em != null && em.getProperty("state") != null && (sqd == null || sqd.getStatus() == 2) && size == 0) {
            em.setProperty("state", "0");
            if (em.getProperty("leader") != null) {
                em.setProperty("leader", "true");
            }
        }
        if (speedRunStart > 0 && size == 0) {
            endSpeedRun();
        }
    }

    public void setCheckStates(boolean b) {
        this.checkStates = b;
    }

    public void setNodes(final MapleNodes mn) {
        this.nodes = mn;
    }

    public final List<MaplePlatform> getPlatforms() {
        return nodes.getPlatforms();
    }

    public Collection<MapleNodeInfo> getNodes() {
        return nodes.getNodes();
    }

    public MapleNodeInfo getNode(final int index) {
        return nodes.getNode(index);
    }

    public boolean isLastNode(final int index) {
        return nodes.isLastNode(index);
    }

    public final List<Rectangle> getAreas() {
        return nodes.getAreas();
    }

    public final Rectangle getArea(final int index) {
        return nodes.getArea(index);
    }

    public final void changeEnvironment(final String ms, final int type) {
        broadcastMessage(CField.OnFieldEffect(new String[]{ms}, FieldEffectOpcode.getType(type)));
    }

    public final void toggleEnvironment(final String ms) {
        if (environment.containsKey(ms)) {
            moveEnvironment(ms, environment.get(ms) == 1 ? 2 : 1);
        } else {
            moveEnvironment(ms, 1);
        }
    }

    public final void moveEnvironment(final String ms, final int type) {
        broadcastMessage(CField.environmentMove(ms, type));
        environment.put(ms, type);
    }

    public final Map<String, Integer> getEnvironment() {
        return environment;
    }

    public final int getNumPlayersInArea(final int index) {
        return getNumPlayersInRect(getArea(index));
    }

    public final int getNumPlayersInRect(final Rectangle rect) {
        int ret = 0;

        charactersLock.readLock().lock();
        try {
            final Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter a;
            while (ltr.hasNext()) {
                if (rect.contains(ltr.next().getTruePosition())) {
                    ret++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public final int getNumPlayersItemsInArea(final int index) {
        return getNumPlayersItemsInRect(getArea(index));
    }

    public final int getNumPlayersItemsInRect(final Rectangle rect) {
        int ret = getNumPlayersInRect(rect);

        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                if (rect.contains(mmo.getTruePosition())) {
                    ret++;
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return ret;
    }

    public final List<Pair<Integer, Integer>> getMobsToSpawn() {
        return nodes.getMobsToSpawn();
    }

    public final List<Integer> getSkillIds() {
        return nodes.getSkillIds();
    }

    public final boolean canSpawn(long now) {
        double spawnDelay = 1.0;
        ChannelServer ch = ChannelServer.getInstance(channel);
        if (ch != null) {
            switch (ch.getChannelType()) {
                case MVP:
                    spawnDelay -= 0.1;
                case 混沌:
                    spawnDelay -= 0.5;
                    break;
            }
        }
        List<Integer> addedSummon = new LinkedList<>();
        List<MapleSummon> summons = getAllSummonsThreadsafe();
        for (MapleSummon summon : summons) {
            if (addedSummon.contains(summon.getSkill())) {
                continue;
            }
            addedSummon.add(summon.getSkill());
            switch (summon.getSkill()) {
                case 80011261: // 輪迴
                    spawnDelay -= 1.0;
                    break;
                case 42111003: // 鬼神召喚
                    spawnDelay -= 0.5;
                    break;
            }
            if (spawnDelay < 0.0) {
                break;
            }
        }
        if (spawnDelay < 0.0) {
            spawnDelay = 0.0;
        }
        return lastSpawnTime > 0 && lastSpawnTime + Math.max(2000, createMobInterval * spawnDelay) < now;
    }

    public final boolean canHurt(long now) {
        if (lastHurtTime > 0 && lastHurtTime + decHPInterval < now) {
            lastHurtTime = now;
            return true;
        }
        return false;
    }

    public final void resetShammos(final MapleClient c) {
        killAllMonsters(true);
        broadcastMessage(CWvsContext.broadcastMsg(5,
                "A player has moved too far from Shammos. Shammos is going back to the start."));
        EtcTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (c.getPlayer() != null) {
                    c.getPlayer().changeMap(MapleMap.this, getPortal(0));
                }
            }
        }, 500); // avoid dl
    }

    public int getInstanceId() {
        return instanceid;
    }

    public void setInstanceId(int ii) {
        this.instanceid = ii;
    }

    public int getPartyBonusRate() {
        return partyBonusRate;
    }

    public void setPartyBonusRate(int ii) {
        this.partyBonusRate = ii;
    }

    public short getTop() {
        return top;
    }

    public short getBottom() {
        return bottom;
    }

    public short getLeft() {
        return left;
    }

    public short getRight() {
        return right;
    }

    public void setTop(int ii) {
        this.top = (short) ii;
    }

    public void setBottom(int ii) {
        this.bottom = (short) ii;
    }

    public void setLeft(int ii) {
        this.left = (short) ii;
    }

    public void setRight(int ii) {
        this.right = (short) ii;
    }

    public final void setChangeableMobOrigin(MapleCharacter d) {
        this.changeMobOrigin = new WeakReference<>(d);
    }

    public final MapleCharacter getChangeableMobOrigin() {
        if (changeMobOrigin == null) {
            return null;
        }
        return changeMobOrigin.get();
    }

    public List<Pair<Point, Integer>> getGuardians() {
        return nodes.getGuardians();
    }

    public DirectionInfo getDirectionInfo(int i) {
        return nodes.getDirection(i);
    }

    public final MapleMapObject getClosestMapObjectInRange(final Point from, final double rangeSq,
            final List<MapleMapObjectType> MapObject_types) {
        MapleMapObject ret = null;
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (from.distanceSq(mmo.getTruePosition()) <= rangeSq && (ret == null
                            || from.distanceSq(ret.getTruePosition()) > from.distanceSq(mmo.getTruePosition()))) {
                        ret = mmo;
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public final void mapMessage(final int type, final String message) {
        broadcastMessage(CWvsContext.broadcastMsg(type, message));
    }

    @Override
    public String toString() {
        return "'" + getStreetName() + " : " + getMapName() + "'(" + getId() + ")";
    }

    public boolean isBossMap() {
        return GameConstants.isBossMap(mapid);
    }

    public boolean isMarketMap() {
        return (this.mapid >= 910000000) && (this.mapid <= 910000017);
    }

    public void setLastSpawnRune() {
        lastSpawnRune = System.currentTimeMillis();
    }

    public Pair<Integer, Integer> getMapMonsterLvRange() {
        Integer min = null;
        Integer max = null;
        for (Spawns mob : monsterSpawn) {
            if (mob == null || mob.getMonster() == null) {
                continue;
            }
            if (min == null || mob.getMonster().getLevel() < min) {
                min = (int) mob.getMonster().getLevel();
            }
            if (max == null || mob.getMonster().getLevel() > max) {
                max = (int) mob.getMonster().getLevel();
            }
        }
        if (min == null) {
            min = 0;
        }
        if (max == null) {
            max = 0;
        }
        return new Pair<>(min, max);
    }

    public void updateBurningField() {
        updateBurningField(true);
    }

    public int getBurningFieldStep() {
        return updateBurningField(false);
    }

    public int updateBurningField(boolean show) {
        Pair<Integer, Integer> lv = getMapMonsterLvRange();
        if (isTown() || GameConstants.isTutorialMap(mapid) || lv.getRight() == null || lv.getRight() < 100) {
            return 0;
        }

        long eachTime = 30 * 60 * 1000; // 30分鐘
        int MaxStep = 10; // 最高階段

        long fieldTime = burningFieldTime;
        int lastStep = (int) Math.min(MaxStep, fieldTime / eachTime);
        long now = System.currentTimeMillis();
        long time = now - burningFieldLastTime;
        int nowStep;
        if (time >= 0) {
            if (time >= eachTime) {
                fieldTime += time;
                long step = fieldTime / eachTime;
                if ((step == MaxStep + 1 && fieldTime % eachTime != 0) || step > MaxStep + 1) {
                    fieldTime = (MaxStep + 1) * eachTime;
                }
            } else {
                fieldTime -= time;
                fieldTime = Math.max(0, fieldTime);
            }
            nowStep = (int) Math.min(MaxStep, fieldTime / eachTime);
        } else {
            nowStep = lastStep;
            System.err.println("Error: 燃燒場地時間更新出錯");
        }
        if (show) {
            if (burningFieldTime == 0 && burningFieldLastTime == 0) {
                show = false;
            }
            burningFieldTime = fieldTime;
            burningFieldLastTime = now;
        }

        if (nowStep > 0 && lastStep != nowStep && show) {
            broadcastMessage(CField.EffectPacket.showEffect(true, null, UserEffectOpcode.UserEffect_TextEffect,
                    new int[]{50, 1500, 4, 0, -200, 1, 4, 2},
                    new String[]{
                        "#fn哥德 ExtraBold##fs26#          燃燒" + nowStep + "階段 : 經驗值追加贈送 " + nowStep + "0%！！   "},
                    null, null));
        }
        return nowStep;
    }

    public void spawnKite(final MapleClient c, final MapleKite kite) {
        addMapObject(kite);
        broadcastMessage(kite.getSpawnKitePacket());
        EtcTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                removeMapObject(kite);
                broadcastMessage(kite.getDestroyKitePacket());
            }
        }, 1000 * 60 * 60);
    }
}
