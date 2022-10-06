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
package server.life;

import client.character.stat.MapleBuffStat;
import client.inventory.Equip;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import constants.GameConstants;
import client.skill.Skill;
import client.inventory.Item;
import client.MapleCharacter;
import client.inventory.MapleInventoryType;
import client.MapleClient;
import handling.channel.ChannelServer;
import client.skill.SkillFactory;
import client.MonsterStatus;
import client.MonsterStatusEffect;
import constants.ServerConstants;
import constants.FeaturesConfig;
import extensions.temporary.FieldEffectOpcode;

import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scripting.EventInstanceManager;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.Randomizer;
import server.Timer;
import server.Timer.BuffTimer;
import server.Timer.EtcTimer;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.ConcurrentEnumMap;
import tools.Pair;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.MobPacket;
import tools.packet.SkillPacket;

public class MapleMonster extends AbstractLoadedMapleLife {

    private MapleMonsterStats stats;
    private ChangeableStats ostats = null;
    private long hp, nextKill = 0, lastDropTime = 0;
    private int mp;
    private byte carnivalTeam = -1;
    private MapleMap map;
    private WeakReference<MapleMonster> sponge = new WeakReference<>(null);
    private int linkoid = 0, lastNode = -1, highestDamageChar = 0, linkCID = 0; // Just a reference for monster EXP
    // distribution after dead
    private WeakReference<MapleCharacter> controller = new WeakReference<>(null);
    private boolean fake = false, dropsDisabled = false, controllerHasAggro = false, controllerKnowsAboutAggro;
    private final Collection<AttackerEntry> attackers = new LinkedList<>();
    private EventInstanceManager eventInstance;
    private MonsterListener listener = null;
    private byte[] reflectpack = null, nodepack = null;
    private final ConcurrentEnumMap<MonsterStatus, MonsterStatusEffect> stati = new ConcurrentEnumMap<>(
            MonsterStatus.class);
    private final LinkedList<MonsterStatusEffect> poisons = new LinkedList<>();
    private final ReentrantReadWriteLock poisonsLock = new ReentrantReadWriteLock();
    private Map<Integer, Long> usedSkills;
    private int stolen = -1; // monster can only be stolen ONCE
    private boolean shouldDropItem = false, killed = false;
    private int triangulation = 0;
    private boolean mark = false;
    private int mobType = -1;
    private EliteMonsterInfo eliteInfo = null;
    private int mobSize = 100;

    public MapleMonster(final int id, final MapleMonsterStats stats) {
        super(id);
        initWithStats(stats);
    }

    public MapleMonster(final MapleMonster monster) {
        super(monster);
        initWithStats(monster.stats);
    }

    private void initWithStats(final MapleMonsterStats stats) {
        setStance(5);
        this.stats = stats;
        hp = stats.getFinalMaxHP() > 0 ? stats.getFinalMaxHP()
                : stats.getFinalMaxHP() < 0 ? Long.MAX_VALUE : stats.getHp();
        mp = stats.getMp();

        if (stats.getNoSkills() > 0) {
            usedSkills = new HashMap<>();
        }
    }

    public final ArrayList<AttackerEntry> getAttackers() {
        if (attackers == null || attackers.size() <= 0) {
            return new ArrayList<>();
        }
        ArrayList<AttackerEntry> ret = new ArrayList<>();
        for (AttackerEntry e : attackers) {
            if (e != null) {
                ret.add(e);
            }
        }
        return ret;
    }

    public final MapleMonsterStats getStats() {
        return stats;
    }

    public final void disableDrops() {
        this.dropsDisabled = true;
    }

    public final boolean dropsDisabled() {
        return dropsDisabled;
    }

    public boolean isSpecial() {
        switch (getId()) {
            case 8910000: // 混沌班班
            case 8910100: // 斑斑
            case 9990033:
                return true;
            default:
                return false;
        }
    }

    public Rectangle getBanRange() {
        switch (getId()) {
            case 9300498:
            case 9300507:
                return new Rectangle(-2350, -1750, 150 - (-2350), 100 - (-1750));
        }
        return null;
    }

    public int getMobType() {
        return mobType;
    }

    public void setMobType(int mobType) {
        this.mobType = mobType;
    }

    public boolean isEliteMob() {
        return getEliteType() >= 0;
    }

    public int getEliteType() {
        if (getEliteInfo() == null) {
            return -1;
        }
        return getEliteInfo().getEliteType();
    }

    public EliteMonsterInfo getEliteInfo() {
        return eliteInfo;
    }

    public void setEliteInfo(EliteMonsterInfo emi) {
        eliteInfo = emi;
    }

    public int getMobSize() {
        return mobSize;
    }

    public void setMobSize(int mobSize) {
        this.mobSize = mobSize;
    }

    public void addMobSize(int size) {
        mobSize += size;
    }

    public final void setSponge(final MapleMonster mob) {
        sponge = new WeakReference<>(mob);
        if (linkoid <= 0) {
            linkoid = mob.getObjectId();
        }
    }

    public final void setMap(final MapleMap map) {
        this.map = map;
        startDropItemSchedule();
    }

    public final long getHp() {
        return hp;
    }

    public final void setHp(long hp) {
        this.hp = hp;
    }

    public final ChangeableStats getChangedStats() {
        return ostats;
    }

    public final int getMaxHp() {
        if (ostats != null) {
            return ostats.hp;
        }
        return stats.getHp();
    }

    public final long getMobMaxHp() {
        if (ostats != null) {
            return ostats.finalmaxHP > 0 ? ostats.finalmaxHP : ostats.hp;
        }
        return stats.getFinalMaxHP() > 0 ? stats.getFinalMaxHP() : stats.getHp();
    }

    public final int getMp() {
        return mp;
    }

    public final void setMp(int mp) {
        if (mp < 0) {
            mp = 0;
        }
        this.mp = mp;
    }

    public final int getMobMaxMp() {
        if (ostats != null) {
            return ostats.mp;
        }
        return stats.getMp();
    }

    public final int getMobExp() {
        if (ostats != null) {
            return ostats.exp;
        }
        return stats.getExp();
    }

    public final void setOverrideStats(final OverrideMonsterStats ostats) {
        this.ostats = new ChangeableStats(stats, ostats);
        this.hp = ostats.getFinalMaxHP() > 0 ? ostats.getFinalMaxHP()
                : ostats.getFinalMaxHP() < 0 ? Long.MAX_VALUE : ostats.getHp();
        this.mp = ostats.getMp();
    }

    public final void setOverrideStats(final ChangeableStats ostats) {
        this.ostats = ostats;
        this.hp = ostats.getFinalMaxHP() > 0 ? ostats.getFinalMaxHP()
                : ostats.getFinalMaxHP() < 0 ? Long.MAX_VALUE : ostats.getHp();
        this.mp = ostats.getMp();
    }

    public final void changeLevel(final int newLevel) {
        changeLevel(newLevel, true);
    }

    public final void changeLevel(final int newLevel, boolean pqMob) {
        if (!stats.isChangeable()) {
            return;
        }
        this.ostats = new ChangeableStats(stats, newLevel, pqMob);
        this.hp = ostats.getFinalMaxHP() > 0 ? ostats.getFinalMaxHP()
                : ostats.getFinalMaxHP() < 0 ? Long.MAX_VALUE : ostats.getHp();
        this.mp = ostats.getMp();
    }

    public final MapleMonster getSponge() {
        return sponge.get();
    }

    public final void damage(final MapleCharacter from, long damage, final boolean updateAttackTime) {
        damage(from, damage, updateAttackTime, 0);
    }

    public final void damage(final MapleCharacter from, long damage, final boolean updateAttackTime,
            final int lastSkill) {
        if (from == null) {
            System.err.println("無法對怪物造成傷害, 角色為空");
            return;
        }
        if (damage <= 0 || !isAlive()) {
            if (from.isShowErr()) {
                from.showInfo("傷害", true, "無法對怪物造成傷害, 傷害<=0 - " + (damage <= 0) + ", 怪物已死亡 - " + (!isAlive()));
            }
            if (!isAlive() && map != null) {
                map.killMonster(this, from, false, false, (byte) 1, lastSkill);
            }
            return;
        }
        // 黑騎士轉生
        if (from.getBuffedValue(MapleBuffStat.Reincarnation) != null && getStats().isBoss()) {
            from.setKillCount(Math.max(from.getKillCount() - 1, 0));
            final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
            stat.put(MapleBuffStat.Reincarnation, 1);
            from.getClient().getSession().writeAndFlush(CWvsContext.BuffPacket.giveBuff(1320019, -1, stat, null, from));
        }
        from.updateLastAttackTime();
        AttackerEntry attacker;
        if (this.getId() == 9400551 && from.getMapId() == 109010104) {
            // this.getStats().setHp(damage + 1);
            OverrideMonsterStats newstat = new OverrideMonsterStats((int) (damage + 1), this.getMp(), this.getMobExp(),
                    (damage + 1) > Integer.MAX_VALUE ? (damage + 1) : 0);
            this.setOverrideStats(newstat);
        }
        if (from.getParty() != null) {
            attacker = new PartyAttackerEntry(from.getParty().getId());
        } else {
            attacker = new SingleAttackerEntry(from);
        }
        boolean replaced = false;
        for (final AttackerEntry aentry : getAttackers()) {
            if (aentry != null && aentry.equals(attacker)) {
                attacker = aentry;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            attackers.add(attacker);
        }
        final long rDamage = Math.max(0, Math.min(damage, hp));
        attacker.addDamage(from, rDamage, updateAttackTime);

        if (stats.getSelfD() != -1) {
            hp -= rDamage;
            if (hp > 0) {
                if (hp < stats.getSelfDHp()) { // HP is below the selfd level
                    map.killMonster(this, from, false, false, stats.getSelfD(), lastSkill);
                } else { // Show HP
                    for (final AttackerEntry mattacker : getAttackers()) {
                        for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                            if (cattacker.getAttacker().getMap() == from.getMap()) { // current attacker is on the map
                                // of the monster
                                if (cattacker.getLastAttackTime() >= System.currentTimeMillis() - 4000) {
                                    cattacker.getAttacker().getClient().getSession()
                                            .writeAndFlush(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                                }
                            }
                        }
                    }
                }
            } else { // Character killed it without explosing :(
                // map.broadcastMessage(MobPacket.showMonsterHP(getObjectId(), 0));
                map.killMonster(this, from, true, false, (byte) 1, lastSkill);
            }
        } else {
            if (sponge.get() != null) {
                if (sponge.get().hp > 0) { // If it's still alive, dont want double/triple rewards
                    // Sponge are always in the same map, so we can use this.map
                    // The only mob that uses sponge are PB/HT
                    sponge.get().hp -= rDamage;

                    if (sponge.get().hp <= 0) {
                        map.broadcastMessage(MobPacket.showBossHP(sponge.get().getId(), -1, sponge.get().getMaxHp(),
                                sponge.get().getMobMaxHp()));
                        map.killMonster(sponge.get(), from, true, false, (byte) 1, lastSkill);
                    } else {
                        map.broadcastMessage(MobPacket.showBossHP(sponge.get()));
                    }
                }
            }
            if (hp > 0) {
                hp -= rDamage;
                if (eventInstance != null) {
                    eventInstance.monsterDamaged(from, this, (int) rDamage);
                } else {
                    final EventInstanceManager em = from.getEventInstance();
                    if (em != null) {
                        em.monsterDamaged(from, this, (int) rDamage);
                    }
                }
                if (sponge.get() == null && hp > 0) {
                    switch (stats.getHPDisplayType()) {
                        case 0:
                            map.broadcastMessage(MobPacket.showBossHP(this), this.getTruePosition());
                            break;
                        case 1:
                            map.broadcastMessage(from, MobPacket.damageFriendlyMob(this, damage, true), false);
                            break;
                        case 2:
                            map.broadcastMessage(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                            from.modifyMulungEnergy(true);
                            break;
                        case 3:
                            for (final AttackerEntry mattacker : getAttackers()) {
                                if (mattacker != null) {
                                    for (final AttackingMapleCharacter cattacker : mattacker.getAttackers()) {
                                        if (cattacker != null && cattacker.getAttacker().getMap() == from.getMap()) { // current
                                            // attacker
                                            // is
                                            // on
                                            // the
                                            // map
                                            // of
                                            // the
                                            // monster
                                            cattacker.getAttacker().getClient().getSession()
                                                    .writeAndFlush(MobPacket.showMonsterHP(getObjectId(), getHPPercent()));
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }

                if (hp <= 0) {
                    if (stats.getHPDisplayType() == 0) {
                        map.broadcastMessage(MobPacket.showBossHP(getId(), -1, getMaxHp(), getMobMaxHp()),
                                this.getTruePosition());
                    }
                    map.killMonster(this, from, true, false, (byte) 1, lastSkill);
                }
            }
        }
        startDropItemSchedule();
    }

    public int getHPPercent() {
        return (int) Math.ceil((hp * 100.0) / getMobMaxHp());
    }

    public final void heal(int hp, int mp, final boolean broadcast) {
        final long TotalHP = getHp() + hp;
        final int TotalMP = getMp() + mp;

        if (TotalHP >= getMobMaxHp()) {
            setHp(getMobMaxHp());
        } else {
            setHp(TotalHP);
        }
        if (TotalMP >= getMp()) {
            setMp(getMp());
        } else {
            setMp(TotalMP);
        }
        if (broadcast) {
            map.broadcastMessage(MobPacket.healMonster(getObjectId(), hp));
        } else if (sponge.get() != null) { // else if, since only sponge doesn't broadcast
            sponge.get().hp += hp;
        }
    }

    public final void killed() {
        if (listener != null) {
            listener.monsterKilled();
        }
        listener = null;
    }

    private void giveExpToCharacter(final MapleCharacter attacker, int exp, final boolean highestDamage,
            final int numExpSharers, byte pty, final byte Class_Bonus_EXP_PERCENT, final byte Premium_Bonus_EXP_PERCENT,
            final int lastskillID) {
        if (highestDamage) {
            if (eventInstance != null) {
                eventInstance.monsterKilled(attacker, this);
            } else {
                final EventInstanceManager em = attacker.getEventInstance();
                if (em != null) {
                    em.monsterKilled(attacker, this);
                }
            }
            highestDamageChar = attacker.getId();
        }
        if (exp > 0) {
            attacker.gainExpMonster(exp, true, highestDamage, pty, Class_Bonus_EXP_PERCENT, Premium_Bonus_EXP_PERCENT,
                    this);
        }
        attacker.mobKilled(getId(), lastskillID);
    }

    public void killGainExp(MapleCharacter killer, int lastSkill) {
        int totalBaseExp = getMobExp();
        AttackerEntry highest = null;
        long highdamage = 0;
        final List<AttackerEntry> list = getAttackers();
        for (final AttackerEntry attackEntry : list) {
            if (attackEntry != null && attackEntry.getDamage() > highdamage) {
                highest = attackEntry;
                highdamage = attackEntry.getDamage();
            }
        }
        int baseExp;
        for (final AttackerEntry attackEntry : list) {
            if (attackEntry != null) {
                baseExp = (int) Math.ceil(totalBaseExp * (0.2 * (attackEntry.contains(killer) ? 1 : 0)
                        + 0.8 * ((double) attackEntry.getDamage() / getMobMaxHp())));
                attackEntry.killedMob(getMap(), baseExp, attackEntry == highest, lastSkill);
            }
        }
    }

    public final int killBy(final MapleCharacter killer, final int lastSkill) {
        if (killed) {
            return -1;
        }
        killed = true;
        final MapleCharacter controll = controller.get();
        if (controll != null) { // this can/should only happen when a hidden gm attacks the monster
            if (GameConstants.isAzwanMap(killer.getMapId())) {
                controll.getClient().getSession().writeAndFlush(MobPacket.stopControllingMonster(this, true));
            } else {
                controll.getClient().getSession().writeAndFlush(MobPacket.stopControllingMonster(this, false));
            }
            controll.stopControllingMonster(this);
        }
        spawnRevives(getMap());
        if (eventInstance != null) {
            eventInstance.unregisterMonster(this);
            eventInstance = null;
        }
        if (killer != null && killer.getPyramidSubway() != null) {
            killer.getPyramidSubway().onKill(killer);
        }
        hp = 0;
        MapleMonster oldSponge = getSponge();
        sponge = new WeakReference<>(null);
        if (oldSponge != null && oldSponge.isAlive()) {
            boolean set = true;
            for (MapleMapObject mon : map.getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mon;
                if (mons.isAlive() && mons.getObjectId() != oldSponge.getObjectId() && mons.getStats().getLevel() > 1
                        && mons.getObjectId() != this.getObjectId()
                        && (mons.getSponge() == oldSponge || mons.getLinkOid() == oldSponge.getObjectId())) { // sponge
                    // was
                    // this,
                    // please
                    // update
                    set = false;
                    break;
                }
            }
            if (set) { // all sponge monsters are dead, please kill off the sponge
                map.killMonster(oldSponge, killer, true, false, (byte) 1);
            }
        }

        reflectpack = null;
        nodepack = null;
        if (stati.size() > 0) {
            List<MonsterStatus> statuses = new LinkedList<>(stati.keySet());
            for (MonsterStatus ms : statuses) {
                cancelStatus(ms);
            }
            statuses.clear();
        }
        if (poisons.size() > 0) {
            List<MonsterStatusEffect> ps = new LinkedList<>();
            poisonsLock.readLock().lock();
            try {
                ps.addAll(poisons);
            } finally {
                poisonsLock.readLock().unlock();
            }
            for (MonsterStatusEffect p : ps) {
                cancelSingleStatus(p);
            }
            ps.clear();
        }
        // attackers.clear();
        cancelDropItem();
        int v1 = highestDamageChar;
        this.highestDamageChar = 0; // reset so we dont kill twice
        return v1;
    }

    public final void spawnRevives(final MapleMap map) {
        List<Integer> toSpawn = stats.getRevives();

        if (toSpawn == null || this.getLinkCID() > 0) {
            return;
        }
        MapleMonster spongy = null;
        long spongyHp = 0L;
        switch (getId()) {
            case 6160003: // 薛西斯
            case 8820002: // 艾莉爾
            case 8820003: // 賢者所羅門
            case 8820004: // 賢者雷克斯
            case 8820005: // 輝沁
            case 8820006: // 穆寧
            case 8840000: // 凡雷恩
            case 8850011: // 西格諾斯
                break;
            case 8810118: // 混沌闇黑龍王
            case 8810119: // 混沌闇黑龍王
            case 8810120: // 混沌闇黑龍王
            case 8810121: // 混沌闇黑龍王
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    mob.setPosition(getTruePosition());
                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    switch (mob.getId()) {
                        case 8810119: // 混沌闇黑龍王
                        case 8810120: // 混沌闇黑龍王
                        case 8810121: // 混沌闇黑龍王
                        case 8810122: // 混沌闇黑龍王
                            spongy = mob;
                            break;
                    }
                }
                if (spongy != null && map.getMonsterById(spongy.getId()) == null) {
                    map.spawnMonster(spongy, -2);
                    for (MapleMapObject mon : map.getAllMonstersThreadsafe()) {
                        MapleMonster mons = (MapleMonster) mon;
                        if (mons.getObjectId() != spongy.getObjectId()
                                && (mons.getSponge() == this || mons.getLinkOid() == this.getObjectId())) { // sponge was
                            // this, please
                            // update
                            mons.setSponge(spongy);
                        }
                    }
                }
                break;
            case 8820300: // 混沌皮卡啾1階段
            case 8820301: // 混沌皮卡啾2階段
            case 8820302: // 混沌皮卡啾3階段
            case 8820303: // 混沌皮卡啾4階段
            case 8820102: // 混沌艾莉爾
            case 8820103: // 混沌賢者所羅門
            case 8820104: // 混沌之贤者莱克斯
            case 8820105: // 混沌賢者雷克斯
            case 8820106: // 混沌穆寧
            case 8820112: // 混度女神雕像
            case 8820113: // 混沌皮卡啾
            case 8820114: // 混沌皮卡啾
            case 8820115: // 混沌賢者所羅門
            case 8820116: // 混沌賢者雷克斯
            case 8820117: // 混沌輝沁
            case 8820118: // 混沌穆寧
            case 8820213: // 混沌賢者所羅門
            case 8820214: // 混沌賢者所羅門
            case 8820215: // 混沌賢者雷克斯
            case 8820216: // 混沌賢者雷克斯
            case 8820217: // 混沌輝沁
            case 8820218: // 混沌輝沁
            case 8820219: // 混沌輝沁
            case 8820220: // 混沌輝沁
            case 8820221: // 混沌穆寧
            case 8820222: // 混沌穆寧
            case 8820223: // 混沌穆寧
            case 8820224: // 混沌穆寧
            case 8820225: // 混沌艾莉爾
            case 8820226: // 混沌艾莉爾
            case 8820227: // 混沌艾莉爾
                MapleMonster linkMob = MapleLifeFactory.getMonster(getId() - 190);
                if (linkMob == null) {
                    break;
                }
                toSpawn = linkMob.getStats().getRevives();
            case 8820108: // 皮卡啾
            case 8820109: // 透明怪物
                List<MapleMonster> cs_mobs = new ArrayList<>();
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);
                    mob.setPosition(getTruePosition());
                    if (this.eventInstance != null) {
                        this.eventInstance.registerMonster(mob);
                    }
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    switch (mob.getId()) {
                        case 8820109: // 透明怪物
                        case 8820300: // 混沌皮卡啾1階段
                        case 8820301: // 混沌皮卡啾2階段
                        case 8820302: // 混沌皮卡啾3階段
                        case 8820303: // 混沌皮卡啾4階段
                        case 8820304: // 混沌皮卡啾5階段
                            spongy = mob;

                            break;
                        default:
                            if (mob.isFirstAttack()) {
                                spongyHp += mob.getMobMaxHp();
                            }
                            cs_mobs.add(mob);
                    }

                }

                if ((spongy == null) || (map.getMonsterById(spongy.getId()) != null)) {
                    return;
                }
                if (spongyHp > 0L) {
                    spongy.setHp(spongyHp);
                    spongy.getStats().setFinalMaxHP(spongyHp);
                }

                map.spawnMonster(spongy, -2);
                for (MapleMonster i : cs_mobs) {
                    map.spawnMonster(i, -2);
                    i.setSponge(spongy);
                }
                break;
            case 8810026: // 召喚闇黑龍王
            case 8810130: // 混沌闇黑龍王召喚
            case 8820008: // 皮卡啾
            case 8820009: // 透明怪物
            case 8820010: // 皮卡啾
            case 8820011: // 皮卡啾
            case 8820012: // 皮卡啾
            case 8820013: { // 皮卡啾
                final List<MapleMonster> mobs = new ArrayList<>();

                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    mob.setPosition(getTruePosition());
                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    switch (mob.getId()) {
                        case 8810018: // 闇黑龍王Horntail Sponge
                        case 8810118: // 混沌闇黑龍王
                        case 8820009: // 透明怪物PinkBeanSponge0
                        case 8820010: // 皮卡啾PinkBeanSponge1
                        case 8820011: // 皮卡啾PinkBeanSponge2
                        case 8820012: // 皮卡啾PinkBeanSponge3
                        case 8820013: // 皮卡啾PinkBeanSponge4
                        case 8820014: // 皮卡啾PinkBeanSponge5
                            spongy = mob;
                            break;
                        default:
                            mobs.add(mob);
                            break;
                    }
                }
                if (spongy != null && map.getMonsterById(spongy.getId()) == null) {
                    map.spawnMonster(spongy, -2);

                    for (final MapleMonster i : mobs) {
                        map.spawnMonster(i, -2);
                        i.setSponge(spongy);
                    }
                }
                break;
            }
            case 8820304: // 混沌皮卡啾5階段
                MapleMonster linkMob_1 = MapleLifeFactory.getMonster(getId() - 190);
                if (linkMob_1 != null) {
                    toSpawn = linkMob_1.getStats().getRevives();
                }
            case 8820014: // 皮卡啾
            case 8820101: // 混沌皮卡啾
            case 8820200: // 混沌皮卡啾
            case 8820201: // 混沌皮卡啾
            case 8820202: // 混沌皮卡啾
            case 8820203: // 混沌皮卡啾
            case 8820204: // 混沌皮卡啾
            case 8820205: // 混沌皮卡啾
            case 8820206: // 混沌皮卡啾
            case 8820207: // 混沌皮卡啾
            case 8820208: // 混沌皮卡啾
            case 8820209: // 混沌皮卡啾
            case 8820210: // 混沌皮卡啾
            case 8820211: { // 混沌皮卡啾
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    mob.setPosition(getTruePosition());
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    map.spawnMonster(mob, -2);
                }
                break;
            }
            default: {
                for (final int i : toSpawn) {
                    final MapleMonster mob = MapleLifeFactory.getMonster(i);

                    if (mob == null) {
                        break;
                    }
                    if (eventInstance != null) {
                        eventInstance.registerMonster(mob);
                    }
                    mob.setPosition(getTruePosition());
                    if (dropsDisabled()) {
                        mob.disableDrops();
                    }
                    map.spawnRevives(mob, this.getObjectId());

                    if (mob.getId() == 9300216) {
                        map.broadcastMessage(
                                CField.OnFieldEffect(new String[]{"Dojang/clear"}, FieldEffectOpcode.FieldEffect_Sound));// was4
                        map.broadcastMessage(CField.OnFieldEffect(new String[]{"dojang/end/clear"},
                                FieldEffectOpcode.FieldEffect_Screen_AutoLetterBox));// was3
                    }
                }
                break;
            }
        }
    }

    public final boolean isAlive() {
        return hp > 0;
    }

    public final void setCarnivalTeam(final byte team) {
        carnivalTeam = team;
    }

    public final byte getCarnivalTeam() {
        return carnivalTeam;
    }

    public final MapleCharacter getController() {
        return controller.get();
    }

    public final void setController(final MapleCharacter controller) {
        this.controller = new WeakReference<>(controller);
    }

    public final void switchController(final MapleCharacter newController, final boolean immediateAggro) {
        final MapleCharacter controllers = getController();
        if (controllers == newController) {
            return;
        } else if (controllers != null) {
            if (GameConstants.isAzwanMap(newController.getMapId())) {
                controllers.getClient().getSession().writeAndFlush(MobPacket.stopControllingMonster(this, true));
            } else {
                controllers.getClient().getSession().writeAndFlush(MobPacket.stopControllingMonster(this, false));
            }
            controllers.stopControllingMonster(this);
            sendStatus(controllers.getClient());
        }
        setController(newController);
        newController.controlMonster(this, immediateAggro);
        if (immediateAggro) {
            setControllerHasAggro(true);
        }
        setControllerKnowsAboutAggro(false);
    }

    public final void addListener(final MonsterListener listener) {
        this.listener = listener;
    }

    public final boolean isControllerHasAggro() {
        return controllerHasAggro;
    }

    public final void setControllerHasAggro(final boolean controllerHasAggro) {
        this.controllerHasAggro = controllerHasAggro;
    }

    public final void sendStatus(final MapleClient client) {
        if (reflectpack != null) {
            client.getSession().writeAndFlush(reflectpack);
        }
        if (poisons.size() > 0) {
            poisonsLock.readLock().lock();
            try {
                client.getSession().writeAndFlush(MobPacket.applyMonsterStatus(this, poisons));
            } finally {
                poisonsLock.readLock().unlock();
            }
        }
    }

    @Override
    public final void sendSpawnData(final MapleClient client) {
        if (!isAlive()) {
            return;
        }
        if (GameConstants.isAzwanMap(client.getPlayer().getMapId())) {
            client.getSession().writeAndFlush(MobPacket.spawnMonster(this, fake && linkCID <= 0 ? -4 : -1, 0, true));
        } else {
            client.getSession().writeAndFlush(MobPacket.spawnMonster(this, fake && linkCID <= 0 ? -4 : -1, 0, false));
        }
        sendStatus(client);
        if (map != null && !stats.isEscort() && client.getPlayer() != null && client.getPlayer().getTruePosition()
                .distanceSq(getTruePosition()) <= GameConstants.maxViewRangeSq_Half()) {
            map.updateMonsterController(this);
        }
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
        if (stats.isEscort() && getEventInstance() != null && lastNode >= 0) { // shammos
            map.resetShammos(client);
        } else {
            if (GameConstants.isAzwanMap(client.getPlayer().getMapId())) {
                client.getSession().writeAndFlush(MobPacket.killMonster(getObjectId(), 0, true));
            } else {
                client.getSession().writeAndFlush(MobPacket.killMonster(getObjectId(), 0, false));
            }
            if (getController() != null && client.getPlayer() != null
                    && client.getPlayer().getId() == getController().getId()) {
                client.getPlayer().stopControllingMonster(this);
            }
        }
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(stats.getName());
        sb.append("(");
        sb.append(getId());
        sb.append(")[");
        sb.append(getObjectId());
        sb.append("]LV.");
        sb.append(stats.getLevel());
        if (ostats != null) {
            sb.append("→");
            sb.append(ostats.level);
        }
        sb.append("(");
        sb.append(getTruePosition().x);
        sb.append(", ");
        sb.append(getTruePosition().y);
        sb.append(")HP:");
        sb.append(getHp());
        sb.append("/");
        sb.append(getMobMaxHp());
        sb.append(",MP:");
        sb.append(getMp());
        sb.append("/");
        sb.append(getMobMaxMp());
        sb.append(",仇恨:");
        final MapleCharacter chr = controller.get();
        sb.append(chr != null ? chr.getName() : "無");

        return sb.toString();
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.MONSTER;
    }

    public final EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public final void setEventInstance(final EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public final int getStatusSourceID(final MonsterStatus status) {
        if (status == MonsterStatus.M_Poison || status == MonsterStatus.M_Dazzle) {
            poisonsLock.readLock().lock();
            try {
                for (MonsterStatusEffect ps : poisons) {
                    if (ps != null) {
                        return ps.getSkill();
                    }
                }
                return -1;
            } finally {
                poisonsLock.readLock().unlock();
            }
        }
        final MonsterStatusEffect effect = stati.get(status);
        if (effect != null) {
            return effect.getSkill();
        }
        return -1;
    }

    public final ElementalEffectiveness getEffectiveness(final Element e) {
        return stats.getEffectiveness(e);
    }

    public void applyStatus(final MapleCharacter from, final MonsterStatusEffect statuss, final boolean poison,
            final long duration, final boolean checkboss, MapleStatEffect eff) {
        List<MonsterStatusEffect> ss = new ArrayList<>();
        ss.add(statuss);
        applyStatus(from, ss, poison, duration, checkboss, eff);
    }

    public final void applyStatus(final MapleCharacter from, final List<MonsterStatusEffect> statuss,
            final boolean poison, long duration, final boolean checkboss, final MapleStatEffect eff) {
        if (!isAlive() || getLinkCID() > 0) {
            return;
        }
        MapleCharacter con = getController();
        int statusSkill;
        MonsterStatus stat;
        long aniTime;
        for (MonsterStatusEffect status : statuss) {
            Skill skilz = SkillFactory.getSkill(status.getSkill());
            if (skilz != null) {
                switch (stats.getEffectiveness(skilz.getElement())) {
                    case IMMUNE:
                    case STRONG:
                        return;
                    case NORMAL:
                    case WEAK:
                        break;
                    default:
                        return;
                }
            }
            // compos don't have an elemental (they have 2 - so we have to hack here...)
            statusSkill = status.getSkill();
            switch (statusSkill) {
                case 2111006: { // FP compo
                    switch (stats.getEffectiveness(Element.POISON)) {
                        case IMMUNE:
                        case STRONG:
                            return;
                    }
                    break;
                }
                case 2211006: { // IL compo
                    switch (stats.getEffectiveness(Element.ICE)) {
                        case IMMUNE:
                        case STRONG:
                            return;
                    }
                    break;
                }
                case 4110011:
                case 4120011:
                case 4210010:
                case 4220011:
                case 4120005:
                case 4320005:
                case 4340012:
                case 14110004: {
                    switch (stats.getEffectiveness(Element.POISON)) {
                        case IMMUNE:
                        case STRONG:
                            return;
                    }
                    break;
                }
            }
            if (duration >= 2000000000) {
                duration = 5000; // teleport master
            }
            stat = status.getStati();
            if (stats.isNoDoom() && stat == MonsterStatus.M_Venom) {
                return;
            }

            if (stats.isBoss()) {
                if (stat == MonsterStatus.M_Stun) {
                    return;
                }
                if (checkboss && stat != (MonsterStatus.M_Speed) && stat != (MonsterStatus.M_Blind)
                        && stat != (MonsterStatus.M_PAD) && stat != (MonsterStatus.M_Poison)
                        && stat != MonsterStatus.M_Dazzle && stat != (MonsterStatus.M_Darkness)
                        && stat != (MonsterStatus.M_HitCriDamR)) {
                    return;
                }
                // hack: don't magic crash cygnus boss
                if (getId() == 8850011 && stat == MonsterStatus.M_HitCriDamR) {
                    return;
                }
            }
            if (stats.isFriendly() || isFake()) {
                if (stat == MonsterStatus.M_Stun || stat == MonsterStatus.M_Speed || stat == MonsterStatus.M_Poison
                        || stat == MonsterStatus.M_Dazzle) {
                    return;
                }
            }
            if ((stat == MonsterStatus.M_Dazzle || stat == MonsterStatus.M_Poison) && eff == null || eff == null) {
                return;
            }
            if (stati.containsKey(stat)) {
                cancelStatus(stat);
            }
            if (stat == MonsterStatus.M_Poison || stat == MonsterStatus.M_Dazzle) {
                int count = 0;
                poisonsLock.readLock().lock();
                try {
                    for (MonsterStatusEffect mse : poisons) {
                        if (mse != null && (mse.getSkill() == eff.getSourceId()
                                || mse.getSkill() == GameConstants.getLinkedAttackSkill(eff.getSourceId())
                                || GameConstants.getLinkedAttackSkill(mse.getSkill()) == eff.getSourceId())) {
                            count++;
                        }
                    }
                } finally {
                    poisonsLock.readLock().unlock();
                }
                if (count >= eff.getDOTStack()) {
                    return;
                }
            }
            if (poison && getHp() > 1) {
                duration = Math.max(duration, eff.getDOTTime() * 1000);
            }
            duration += from.getStat().dotTime * 1000;

            if (duration >= 60000L) {
                duration = 10000L;
            }

            aniTime = duration;
            if (skilz != null) {
                aniTime += skilz.getAnimationTime();
            }
            status.setCancelTask(aniTime);
            if (poison && getHp() > 1) { // 中毒[POISON]
                if (status.getchr() != null) {
                    return;
                }
                status.setDotTime(duration);
                int poisonDot = from.getStat().dot;
                int damageIncrease = from.getStat().getDamageIncrease(eff.getSourceId());
                if (damageIncrease > eff.getDOT()) {
                    poisonDot += damageIncrease;
                } else {
                    poisonDot += eff.getDOT();
                }
                if (from.isShowInfo()) {
                    from.showInfo("持續傷害", false, "開始處理效果 - 技能ID:" + eff.getSourceId());
                    from.showInfo("持續傷害", false, "加成 - 技能ID:" + eff.getDOT() + " 被動： " + from.getStat().dot + " 被動加成： "
                            + damageIncrease + " 最終加成：" + poisonDot);
                }
                status.setValue(status.getStati(),
                        (int) (poisonDot * from.getStat().getCurrentMaxBaseDamage() / 100.0));
                int dam = (int) (aniTime / 1000 * status.getX() / 2);
                if (from.isShowInfo()) {
                    from.showInfo("持續傷害", false, "持續傷害:" + dam + " 持續時間:" + aniTime + "持續掉血:" + status.getX());
                }
                status.setPoisonSchedule(status.getX(), from);
            } else if (statusSkill == 4111003 || statusSkill == 14111001) { // shadow web
                status.setValue(status.getStati(), (int) (getMobMaxHp() / 50.0 + 0.999));
                status.setPoisonSchedule(status.getX(), from);
            } else if (statusSkill == 4341003) { // monsterbomb
                status.setPoisonSchedule((int) (eff.getDamage() * from.getStat().getCurrentMaxBaseDamage() / 100.0),
                        from);
            } else if (statusSkill == 4121004 || statusSkill == 4221004) { // NINJA_AMBUSH
                status.setValue(status.getStati(), Math.min(Short.MAX_VALUE,
                        (int) (eff.getDamage() * from.getStat().getCurrentMaxBaseDamage() / 100.0)));
                int dam = (int) (aniTime / 1000 * status.getX() / 2);
                status.setPoisonSchedule(dam, from);
                if (dam > 0) {
                    if (dam >= hp) {
                        dam = (int) (hp - 1);
                    }
                    // damage(from, dam, false);
                }
            }

            if (stat == MonsterStatus.M_Poison || stat == MonsterStatus.M_Dazzle) {
                poisonsLock.writeLock().lock();
                try {
                    poisons.add(status);
                    status.scheduledoPoison(this);
                } finally {
                    poisonsLock.writeLock().unlock();
                }
            } else {
                stati.put(stat, status);
            }
        }

        if (con != null) {
            map.broadcastMessage(con, MobPacket.applyMonsterStatus(this, statuss), getTruePosition());
            con.getClient().getSession().writeAndFlush(MobPacket.applyMonsterStatus(this, statuss));
        } else {
            map.broadcastMessage(MobPacket.applyMonsterStatus(this, statuss), getTruePosition());
        }
    }

    public final void dispelSkill(final MobSkill skillId) {
        List<MonsterStatus> toCancel = new ArrayList<>();
        for (Entry<MonsterStatus, MonsterStatusEffect> effects : stati.entrySet()) {
            MonsterStatusEffect mse = effects.getValue();
            if (mse.getMobSkill() != null && mse.getMobSkill().getSkillId() == skillId.getSkillId()) { // not checking
                // for level.
                toCancel.add(effects.getKey());
            }
        }
        for (MonsterStatus stat : toCancel) {
            cancelStatus(stat);
        }
    }

    public void applyMonsterBuff(final Map<MonsterStatus, Integer> effect, final int x, final int skillId,
            final long duration, final MobSkill skill, final List<Integer> reflection) {
        final MapleCharacter con = getController();
        BuffTimer BuffTimer = Timer.BuffTimer.getInstance();
        final Runnable cancelTask = new Runnable() {
            @Override
            public void run() {
                if (isAlive()) {
                    List<MonsterStatusEffect> mse = new ArrayList<>();
                    for (Entry<MonsterStatus, Integer> z : effect.entrySet()) {
                        mse.add(new MonsterStatusEffect(z.getKey(), z.getValue(), skillId, skill, true,
                                reflection.size() > 0));
                    }
                    map.broadcastMessage(con, MobPacket.cancelMonsterStatus(MapleMonster.this, mse), getPosition());
                    if (getController() != null && !getController().isMapObjectVisible(MapleMonster.this)) {
                        getController().getClient().getSession()
                                .writeAndFlush(MobPacket.cancelMonsterStatus(MapleMonster.this, mse));
                    }
                    for (final MonsterStatus stat : effect.keySet()) {
                        stati.remove(stat);
                    }
                    reflection.clear();
                }
                if (con != null && con.isShowInfo()) {
                    con.showMessage(10, "結束 => 持續傷害: 結束時間[" + System.currentTimeMillis() + "]");
                }
            }
        };
        for (Entry<MonsterStatus, Integer> z : effect.entrySet()) {
            if (stati.containsKey(z.getKey())) {
                cancelStatus(z.getKey());
            }
            final MonsterStatusEffect effectz = new MonsterStatusEffect(z.getKey(), z.getValue(), 0, skill, true,
                    reflection.size() > 0);
            effectz.setCancelTask(duration);
            stati.put(z.getKey(), effectz);
        }
        if (reflection.size() > 0) {
            List<MonsterStatusEffect> mse = new ArrayList<>();
            for (Entry<MonsterStatus, Integer> z : effect.entrySet()) {
                mse.add(new MonsterStatusEffect(z.getKey(), z.getValue(), 0, skill, true, reflection.size() > 0));
            }
            this.reflectpack = MobPacket.applyMonsterStatus(this, mse);
            if (con != null) {
                map.broadcastMessage(con, reflectpack, getTruePosition());
                con.getClient().getSession().writeAndFlush(this.reflectpack);
            } else {
                map.broadcastMessage(reflectpack, getTruePosition());
            }
        } else {
            for (Entry<MonsterStatus, Integer> z : effect.entrySet()) {
                final MonsterStatusEffect effectz = new MonsterStatusEffect(z.getKey(), z.getValue(), 0, skill, true,
                        reflection.size() > 0);
                if (con != null) {
                    map.broadcastMessage(con, MobPacket.applyMonsterStatus(this, effectz), getTruePosition());
                    con.getClient().getSession().writeAndFlush(MobPacket.applyMonsterStatus(this, effectz));
                } else {
                    map.broadcastMessage(MobPacket.applyMonsterStatus(this, effectz), getTruePosition());
                }
            }
        }
        BuffTimer.schedule(cancelTask, duration);
        if (con != null && con.isShowInfo()) {
            String bfn = "";
            for (Entry<MonsterStatus, Integer> z : effect.entrySet()) {
                bfn += "[" + z.getKey().name() + "] ";
            }
            con.showMessage(10,
                    "開始 => 怪物施放狀態: 持續時間[" + duration + "] 開始時間[" + System.currentTimeMillis() + "] 狀態效果:" + bfn);
        }
    }

    public final void setTempEffectiveness(final Element e, final long milli) {
        stats.setEffectiveness(e, ElementalEffectiveness.WEAK);
        EtcTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                stats.removeEffectiveness(e);
            }
        }, milli);
    }

    public final boolean isBuffed(final MonsterStatus status) {
        if (status == MonsterStatus.M_Poison || status == MonsterStatus.M_Dazzle) {
            return poisons.size() > 0 || stati.containsKey(status);
        }
        return stati.containsKey(status);
    }

    public final MonsterStatusEffect getBuff(final MonsterStatus status) {
        return stati.get(status);
    }

    public final int getStatiSize() {
        return stati.size() + (poisons.size() > 0 ? 1 : 0);
    }

    public final ArrayList<MonsterStatusEffect> getAllBuffs() {
        ArrayList<MonsterStatusEffect> ret = new ArrayList<>();
        for (MonsterStatusEffect e : stati.values()) {
            ret.add(e);
        }
        poisonsLock.readLock().lock();
        try {
            for (MonsterStatusEffect e : poisons) {
                ret.add(e);
            }
        } finally {
            poisonsLock.readLock().unlock();
        }
        return ret;
    }

    public final void setFake(final boolean fake) {
        this.fake = fake;
    }

    public final boolean isFake() {
        return fake;
    }

    public final MapleMap getMap() {
        return map;
    }

    public final List<Pair<Integer, Integer>> getSkills() {
        return stats.getSkills();
    }

    public final boolean hasSkill(final int skillId, final int level) {
        return stats.hasSkill(skillId, level);
    }

    public final long getLastSkillUsed(final int skillId) {
        if (usedSkills.containsKey(skillId)) {
            return usedSkills.get(skillId);
        }
        return 0;
    }

    public final void setLastSkillUsed(final int skillId, final long now, final long cooltime) {
        switch (skillId) {
            case 140:
                usedSkills.put(skillId, now + (cooltime * 2));
                usedSkills.put(141, now);
                break;
            case 141:
                usedSkills.put(skillId, now + (cooltime * 2));
                usedSkills.put(140, now + cooltime);
                break;
            default:
                usedSkills.put(skillId, now + cooltime);
                break;
        }
    }

    public final byte getNoSkills() {
        return stats.getNoSkills();
    }

    public final boolean isFirstAttack() {
        return stats.isFirstAttack();
    }

    public final int getBuffToGive() {
        return stats.getBuffToGive();
    }

    public final void doPoison(final MonsterStatusEffect status, final WeakReference<MapleCharacter> weakChr) {
        if ((status.getStati() == MonsterStatus.M_Dazzle || status.getStati() == MonsterStatus.M_Poison)
                && poisons.size() <= 0) {
            return;
        }
        if (status.getStati() != MonsterStatus.M_Dazzle && status.getStati() != MonsterStatus.M_Poison
                && !stati.containsKey(status.getStati())) {
            return;
        }
        if (weakChr == null) {
            return;
        }
        int damage = status.getPoisonSchedule();
        final boolean shadowWeb = status.getSkill() == 4111003 || status.getSkill() == 14111001;
        final MapleCharacter chr = weakChr.get();
        boolean cancel = damage <= 0 || chr == null || chr.getMapId() != map.getId();
        if (damage >= hp) {
            damage = (int) hp--;
            // cancel = !shadowWeb && cancel;
        }
        if (!cancel) {
            damage(chr, damage, false);
            if (shadowWeb) {
                map.broadcastMessage(MobPacket.damageMonster(getObjectId(), damage), getTruePosition());
            }
        }
    }

    public String getName() {
        return stats.getName();
    }

    private static class AttackingMapleCharacter {

        private final MapleCharacter attacker;
        private long lastAttackTime;

        public AttackingMapleCharacter(final MapleCharacter attacker, final long lastAttackTime) {
            super();
            this.attacker = attacker;
            this.lastAttackTime = lastAttackTime;
        }

        public final long getLastAttackTime() {
            return lastAttackTime;
        }

        public final void setLastAttackTime(final long lastAttackTime) {
            this.lastAttackTime = lastAttackTime;
        }

        public final MapleCharacter getAttacker() {
            return attacker;
        }
    }

    private interface AttackerEntry {

        List<AttackingMapleCharacter> getAttackers();

        public void addDamage(MapleCharacter from, long damage, boolean updateAttackTime);

        public long getDamage();

        public boolean contains(MapleCharacter chr);

        public void killedMob(MapleMap map, int baseExp, boolean mostDamage, int lastSkill);
    }

    private final class SingleAttackerEntry implements AttackerEntry {

        private long damage = 0;
        private final int chrid;
        private long lastAttackTime;

        public SingleAttackerEntry(final MapleCharacter from) {
            this.chrid = from.getId();
        }

        @Override
        public void addDamage(final MapleCharacter from, final long damage, final boolean updateAttackTime) {
            if (chrid == from.getId()) {
                this.damage += damage;
                if (updateAttackTime) {
                    lastAttackTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public final List<AttackingMapleCharacter> getAttackers() {
            final MapleCharacter chr = map.getCharacterById(chrid);
            if (chr != null) {
                return Collections.singletonList(new AttackingMapleCharacter(chr, lastAttackTime));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean contains(final MapleCharacter chr) {
            return chrid == chr.getId();
        }

        @Override
        public long getDamage() {
            return damage;
        }

        @Override
        public void killedMob(final MapleMap map, final int baseExp, final boolean mostDamage, final int lastSkill) {
            final MapleCharacter chr = map.getCharacterById(chrid);
            if (chr != null && chr.isAlive()) {
                giveExpToCharacter(chr, baseExp, mostDamage, 1, (byte) 0, (byte) 0, (byte) 0, lastSkill);
            }
        }

        @Override
        public int hashCode() {
            return chrid;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SingleAttackerEntry other = (SingleAttackerEntry) obj;
            return chrid == other.chrid;
        }
    }

    private static final class ExpMap {

        public final int exp;
        public final byte ptysize;
        public final byte Class_Bonus_EXP;
        public final byte Premium_Bonus_EXP;

        public ExpMap(final int exp, final byte ptysize, final byte Class_Bonus_EXP, final byte Premium_Bonus_EXP) {
            super();
            this.exp = exp;
            this.ptysize = ptysize;
            this.Class_Bonus_EXP = Class_Bonus_EXP;
            this.Premium_Bonus_EXP = Premium_Bonus_EXP;
        }
    }

    private static final class OnePartyAttacker {

        public MapleParty lastKnownParty;
        public long damage;
        public long lastAttackTime;

        public OnePartyAttacker(final MapleParty lastKnownParty, final long damage) {
            super();
            this.lastKnownParty = lastKnownParty;
            this.damage = damage;
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    private class PartyAttackerEntry implements AttackerEntry {

        private long totDamage = 0;
        private final Map<Integer, OnePartyAttacker> attackers = new HashMap<>(6);
        private final int partyid;

        public PartyAttackerEntry(final int partyid) {
            this.partyid = partyid;
        }

        @Override
        public List<AttackingMapleCharacter> getAttackers() {
            final List<AttackingMapleCharacter> ret = new ArrayList<>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> entry : attackers.entrySet()) {
                final MapleCharacter chr = map.getCharacterById(entry.getKey());
                if (chr != null) {
                    ret.add(new AttackingMapleCharacter(chr, entry.getValue().lastAttackTime));
                }
            }
            return ret;
        }

        private Map<MapleCharacter, OnePartyAttacker> resolveAttackers() {
            final Map<MapleCharacter, OnePartyAttacker> ret = new HashMap<>(attackers.size());
            for (final Entry<Integer, OnePartyAttacker> aentry : attackers.entrySet()) {
                final MapleCharacter chr = map.getCharacterById(aentry.getKey());
                if (chr != null) {
                    ret.put(chr, aentry.getValue());
                }
            }
            return ret;
        }

        @Override
        public final boolean contains(final MapleCharacter chr) {
            return attackers.containsKey(chr.getId());
        }

        @Override
        public final long getDamage() {
            return totDamage;
        }

        @Override
        public void addDamage(final MapleCharacter from, final long damage, final boolean updateAttackTime) {
            final OnePartyAttacker oldPartyAttacker = attackers.get(from.getId());
            if (oldPartyAttacker != null) {
                oldPartyAttacker.damage += damage;
                oldPartyAttacker.lastKnownParty = from.getParty();
                if (updateAttackTime) {
                    oldPartyAttacker.lastAttackTime = System.currentTimeMillis();
                }
            } else {
                // only the last setup will get exp - but otherwise we'd have to store the full
                // party
                // constellation for every attack/everytime it changes, might be wanted/needed
                // in the
                // future but not now
                final OnePartyAttacker onePartyAttacker = new OnePartyAttacker(from.getParty(), damage);
                attackers.put(from.getId(), onePartyAttacker);
                if (!updateAttackTime) {
                    onePartyAttacker.lastAttackTime = 0;
                }
            }
            totDamage += damage;
        }

        @Override
        public final void killedMob(final MapleMap map, final int baseExp, final boolean mostDamage,
                final int lastSkill) {
            MapleCharacter pchr, highest = null;
            long iDamage, highestDamage = 0;
            int iexp;
            MapleParty party;
            double addedPartyLevel, levelMod;
            List<MapleCharacter> expApplicable;
            final Map<MapleCharacter, ExpMap> expMap = new HashMap<>(6);
            byte Class_Bonus_EXP;
            byte Premium_Bonus_EXP;

            for (final Entry<MapleCharacter, OnePartyAttacker> attacker : resolveAttackers().entrySet()) {
                party = attacker.getValue().lastKnownParty;
                addedPartyLevel = 0;
                Class_Bonus_EXP = 0;
                Premium_Bonus_EXP = 0;
                expApplicable = new ArrayList<>();
                byte pty = 0;
                for (final MaplePartyCharacter partychar : party.getMembers()) {
                    pchr = map.getCharacterById(partychar.getId());
                    if (pchr != null && pchr.isAlive()) {
                        pty++;
                        int i = attacker.getKey().getLevel() - partychar.getLevel();
                        int j = stats.getLevel() - partychar.getLevel();
                        if ((i <= 5 && i >= -40) || (j <= 5 && j >= -40)) {
                            expApplicable.add(pchr);
                            addedPartyLevel += pchr.getLevel();

                            Class_Bonus_EXP += ServerConstants.Class_Bonus_EXP(pchr.getJob());
                            if (pchr.getStat().equippedWelcomeBackRing && Premium_Bonus_EXP == 0) {
                                Premium_Bonus_EXP = 80;
                            }
                        }
                    }
                }
                iDamage = attacker.getValue().damage;
                if (iDamage > highestDamage) {
                    highest = attacker.getKey();
                    highestDamage = iDamage;
                }
                if (expApplicable.size() <= 1 || !FeaturesConfig.classBonusEXP) {
                    Class_Bonus_EXP = 0;
                }

                for (final MapleCharacter expReceiver : expApplicable) {
                    levelMod = expReceiver.getLevel() / addedPartyLevel * (0.8);
                    iexp = (int) Math.round(
                            ((attacker.getKey().getId() == expReceiver.getId() ? (0.2) : 0.0) + levelMod) * baseExp);
                    expMap.put(expReceiver, new ExpMap(iexp, (byte) pty, Class_Bonus_EXP, Premium_Bonus_EXP));
                }
            }
            ExpMap expmap;
            for (final Entry<MapleCharacter, ExpMap> expReceiver : expMap.entrySet()) {
                expmap = expReceiver.getValue();
                giveExpToCharacter(expReceiver.getKey(), expmap.exp,
                        mostDamage ? expReceiver.getKey() == highest : false, expMap.size(), expmap.ptysize,
                        expmap.Class_Bonus_EXP, expmap.Premium_Bonus_EXP, lastSkill);
            }
        }

        @Override
        public final int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + partyid;
            return result;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PartyAttackerEntry other = (PartyAttackerEntry) obj;
            return partyid == other.partyid;
        }
    }

    public int getLinkOid() {
        return linkoid;
    }

    public void setLinkOid(int lo) {
        this.linkoid = lo;
    }

    public final ConcurrentEnumMap<MonsterStatus, MonsterStatusEffect> getStati() {
        return stati;
    }

    public void addEmpty() {
        for (MonsterStatus stat : MonsterStatus.values()) {
            if (stat.isEmpty()) {
                stati.put(stat, new MonsterStatusEffect(stat, 0, 0, null, false));
            }
        }
    }

    public final int getStolen() {
        return stolen;
    }

    public final void setStolen(final int s) {
        this.stolen = s;
    }

    public final void handleSteal(MapleCharacter chr) {
        double showdown = 100.0;
        final MonsterStatusEffect mse = getBuff(MonsterStatus.M_Ambush);
        if (mse != null) {
            showdown += mse.getX();
        }

        Skill steal = SkillFactory.getSkill(4201004);
        final int level = chr.getTotalSkillLevel(steal),
                chServerrate = ChannelServer.getInstance(chr.getClient().getChannel()).getDropRate(chr.getWorld());
        if (level > 0 && !getStats().isBoss() && stolen == -1 && steal.getEffect(level).makeChanceResult()) {
            final MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
            final List<MonsterDropEntry> de = mi.retrieveDrop(getId());
            if (de == null) {
                stolen = 0;
                return;
            }
            final List<MonsterDropEntry> dropEntry = new ArrayList<>(de);
            Collections.shuffle(dropEntry);
            Item idrop;
            for (MonsterDropEntry d : dropEntry) { // set to 4x rate atm, 40% chance + 10x
                if (d.itemId > 0 && d.questid == 0 && d.itemId / 10000 != 238
                        && Randomizer.nextInt(999999) < (int) (10 * d.chance * chServerrate * chr.getDropMod()
                        * chr.getStat().dropBuff / 100.0 * (showdown / 100.0))) { // kinda op
                    if (GameConstants.getInventoryType(d.itemId) == MapleInventoryType.EQUIP) {
                        Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(d.itemId);
                        idrop = MapleItemInformationProvider.getInstance().randomizeStats(eq);
                    } else {
                        idrop = new Item(d.itemId, (byte) 0,
                                (short) (d.Maximum != 1 ? Randomizer.nextInt(d.Maximum - d.Minimum) + d.Minimum : 1),
                                0);
                    }
                    stolen = d.itemId;
                    map.spawnMobDrop(idrop, map.calcDropPos(getPosition(), getTruePosition()), this, chr, (byte) 0,
                            (short) 0);
                    break;
                }
            }
        } else {
            stolen = 0; // failed once, may not go again
        }
    }

    public final void setLastNode(final int lastNode) {
        this.lastNode = lastNode;
    }

    public final int getLastNode() {
        return lastNode;
    }

    public final void cancelStatus(final MonsterStatus stat) {
        if (stat == MonsterStatus.M_Burned || stat == MonsterStatus.M_SeperateSoulC) {
            return;
        }
        final MonsterStatusEffect mse = stati.get(stat);
        if (mse == null || !isAlive()) {
            return;
        }
        if (mse.isReflect()) {
            reflectpack = null;
        }
        mse.cancelPoisonSchedule(this);
        final MapleCharacter con = getController();
        if (con != null) {
            map.broadcastMessage(con, MobPacket.cancelMonsterStatus(this, mse), getTruePosition());
            con.getClient().getSession().writeAndFlush(MobPacket.cancelMonsterStatus(this, mse));
        } else {
            map.broadcastMessage(MobPacket.cancelMonsterStatus(this, mse), getTruePosition());
        }
        stati.remove(stat);
    }

    public final void cancelSingleStatus(final MonsterStatusEffect stat) {
        if (stat == null || stat.getStati() == MonsterStatus.M_Burned
                || stat.getStati() == MonsterStatus.M_SeperateSoulC || !isAlive()) {
            return;
        }
        if (stat.getStati() != MonsterStatus.M_Poison && stat.getStati() != MonsterStatus.M_Dazzle) {
            cancelStatus(stat.getStati());
            return;
        }
        poisonsLock.writeLock().lock();
        try {
            if (!poisons.contains(stat)) {
                return;
            }
            poisons.remove(stat);
            if (stat.isReflect()) {
                reflectpack = null;
            }
            stat.cancelPoisonSchedule(this);
            final MapleCharacter con = getController();
            if (con != null) {
                map.broadcastMessage(con, MobPacket.cancelMonsterStatus(this, stat), getTruePosition());
                con.getClient().getSession().writeAndFlush(MobPacket.cancelMonsterStatus(this, stat));
            } else {
                map.broadcastMessage(MobPacket.cancelMonsterStatus(this, stat), getTruePosition());
            }
        } finally {
            poisonsLock.writeLock().unlock();
        }
    }

    public final void cancelDropItem() {
        lastDropTime = 0;
    }

    public final void startDropItemSchedule() {
        cancelDropItem();
        if (stats.getDropItemPeriod() <= 0 || !isAlive()) {
            return;
        }
        shouldDropItem = false;
        lastDropTime = System.currentTimeMillis();
    }

    public boolean shouldDrop(long now) {
        return lastDropTime > 0 && lastDropTime + (stats.getDropItemPeriod() * 1000) < now;
    }

    public void doDropItem(long now) {
        final int itemId;
        switch (getId()) {
            case 9300061:
                itemId = 4001101;
                break;
            default: // until we find out ... what other mobs use this and how to get the ITEMID
                cancelDropItem();
                return;
        }
        if (isAlive() && map != null) {
            if (shouldDropItem) {
                map.spawnAutoDrop(itemId, getTruePosition());
            } else {
                shouldDropItem = true;
            }
        }
        lastDropTime = now;
    }

    public byte[] getNodePacket() {
        return nodepack;
    }

    public void setNodePacket(final byte[] np) {
        this.nodepack = np;
    }

    public void registerKill(final long next) {
        this.nextKill = System.currentTimeMillis() + next;
    }

    public boolean shouldKill(long now) {
        return nextKill > 0 && now > nextKill;
    }

    public int getLinkCID() {
        return linkCID;
    }

    public void setLinkCID(int lc) {
        this.linkCID = lc;
        if (lc > 0) {
            stati.put(MonsterStatus.M_BodyPressure,
                    new MonsterStatusEffect(MonsterStatus.M_BodyPressure, 60000, 30001062, null, false));
        }
    }

    public void setTriangulation(int triangulation) {
        this.triangulation = triangulation;
    }

    public int getTriangulation() {
        return triangulation;
    }

    /* Anti KS */
    private int belongsTo = -1;
    private long endBelong;

    public boolean getBelongsToSomeone() {
        return belongsTo != -1 && endBelong > System.currentTimeMillis();
    }

    public int getBelongsTo() {
        return belongsTo;
    }

    public long getBelongTimeLeft() {
        if (getBelongsToSomeone()) {
            return endBelong - System.currentTimeMillis();
        } else {
            return 0;
        }
    }

    public boolean getCanBelong() {
        return belongsTo == -1;
    }

    public void expireAntiKS() {
        belongsTo = -2;
        // endBelong = System.currentTimeMillis();
    }

    public void setBelongsTo(MapleCharacter chr) { // Only let it be controlled once.
        if (belongsTo != -1) {
            return;
        }
        if (stats.isBoss()) {
            belongsTo = -2;
            return;
        }
        belongsTo = chr.getId();
        endBelong = System.currentTimeMillis() + (stats.isBoss() ? 300000 : 30000); // 30 seconds for the person to kill
        // it.
    }

    /* Anti KS */
    public boolean isAttackedBy(MapleCharacter chr) {
        for (AttackerEntry aentry : attackers) {
            if (aentry.contains(chr)) {
                return true;
            }
        }
        return false;
    }

    public boolean isControllerKnowsAboutAggro() {
        return controllerKnowsAboutAggro;
    }

    public void setControllerKnowsAboutAggro(boolean controllerKnowsAboutAggro) {
        this.controllerKnowsAboutAggro = controllerKnowsAboutAggro;
    }

    public void setmark(boolean x) {
        if (!x) {
            this.getMap().broadcastMessage(SkillPacket.cancel标飞标记(this.getObjectId(), this.getController()));
        }
        this.mark = x;
    }

    public boolean getmark() {
        return mark;
    }
}
