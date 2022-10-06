package handling.channel.handler;

import client.*;
import client.anticheat.CheatTracker;
import client.anticheat.CheatingOffense;
import client.character.stat.MapleBuffStat;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.skill.Skill;
import client.skill.SkillFactory;
import constants.GameConstants;
import constants.SkillConstants;
import handling.RecvPacketOpcode;
import java.awt.Point;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.*;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.skills.SkillHandleFetcher;
import tools.AttackPair;
import tools.FileoutputUtil;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.JobPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DamageParse {

    public static void applyAttack(AttackInfo attack, Skill usedSkill, MapleCharacter player, int attackCount,
            double maxDamagePerMonster, MapleStatEffect effect, boolean mirror) {
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        if ((attack.real) && (GameConstants.getAttackDelay(attack.skill, usedSkill) >= 100)) {
            player.getCheatTracker().checkAttack(attack.skill, attack.lastAttackTickCount);
        }
        String attackType;
        if (attack.isMeleeAttack) {
            attackType = "近戰攻擊";
        } else if (attack.isShootAttack) {
            attackType = "射擊攻擊";
        } else if (attack.isMagicAttack) {
            attackType = "魔法攻擊";
        } else if (attack.isBodyAttack) {
            attackType = "身體攻擊";
        } else {
            attackType = "技能攻擊";
        }
        if (mirror) {
            attackType += "(分身)";
        }
        if (attack.skill != 0) {
            if (player.isShowInfo()) {
                int display = attack.display & 0x7FFF;
                String skillName = String.valueOf(attack.skill);
                Skill sk = SkillFactory.getSkill(attack.skill);
                if (sk != null || usedSkill != null) {
                    if (usedSkill != null) {
                        skillName = usedSkill.getName() + "(" + attack.skill + ")";
                    } else {
                        skillName = sk.getName() + "(" + attack.skill + ")";
                    }
                }
                player.showMessage(6, "[" + attackType + "]使用技能[" + skillName + "]進行攻擊，攻擊動作:0x"
                        + Integer.toHexString(display).toUpperCase() + "(" + display + ")");
            }
            if (effect == null) {
                player.getClient().announce(CWvsContext.enableActions());
                return;
            }
            if (GameConstants.isMulungSkill(attack.skill)) {
                if (player.getMapId() / 10000 != 92502) {
                    return;
                }
                if (player.getMulungEnergy() < 10000) {
                    return;
                }
                player.modifyMulungEnergy(false);
            } else if (GameConstants.isPyramidSkill(attack.skill)) {
                if (player.getMapId() / 1000000 != 926) {
                    return;
                }
                if ((player.getPyramidSubway() == null) || (!player.getPyramidSubway().onSkillUse(player))) {
                    return;
                }
            } else if (GameConstants.isInflationSkill(attack.skill)) {
                if (player.getBuffedValue(MapleBuffStat.Inflation) == null) {
                    return;
                }
            }
            int maxTargets = effect.getMobCount() + SkillConstants.SkillIncreaseMobCount(attack.skill)
                    + player.getStat().getMobCount(attack.skill, effect.getMobCount());
            if ((attack.targets > maxTargets)) {
                player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT);
                if (player.isShowErr()) {
                    player.dropMessage(-5,
                            "[" + attackType + "]怪物數量檢測 => 封包解析次數: " + attack.targets + " 伺服器設置次數: " + maxTargets);
                }
                FileoutputUtil.logToFile("日誌/打怪數量異常.txt", "\r\n 玩家: " + player.getName() + " 技能代碼: " + attack.skill
                        + " 封包怪物數量 : " + attack.targets + " 伺服器怪物數量 :" + maxTargets);
                return;
            }

            // 處理靈魂寶珠
            for (int skil : SkillConstants.getSoulSkills()) {
                if (skil == attack.skill) {
                    if (attack.skill == player.getEquippedSoulSkill()) {
                        player.checkSoulState(true);
                        break;
                    } else {
                        return;
                    }
                }
            }
        }

        boolean useAttackCount = !GameConstants.is不檢測次數(attack.skill);

        if (attack.hits > 0 && attack.targets > 0) {
            if (!player.getStat().checkEquipDurabilitys(player, -1)) {
                player.dropMessage(5, "裝備耐久度不足。");
                return;
            }
        }

        int totDamage = 0;
        MapleMap map = player.getMap();

        if (attack.skill == 4211006 || attack.skill == 42111002) { // 楓幣炸彈 || 紅玉咒印
            for (AttackPair oned : attack.allDamage) {
                if (oned.attack == null) {
                    MapleMapObject mapobject = map.getMapObject(oned.objectid, MapleMapObjectType.ITEM);
                    if (mapobject != null) {
                        MapleMapItem mapitem = (MapleMapItem) mapobject;
                        mapitem.getLock().lock();
                        try {
                            if (mapitem.isPickedUp() || mapitem.getOwner() != player.getId()) {
                                return;
                            }
                            if (attack.skill == 4211006 && mapitem.getMeso() > 0) { // 楓幣炸彈
                                map.removeMapObject(mapitem);
                                map.broadcastMessage(CField.explodeDrop(mapitem.getObjectId()));
                                mapitem.setPickedUp(true);
                            } else if (attack.skill == 42111002 && mapitem.getItemId() == 4033270) { // 紅玉咒印
                                map.removeMapObject(mapitem);
                                map.broadcastMessage(CField.removeItemFromMap(mapitem.getObjectId(), 1, 0));
                                mapitem.setPickedUp(true);
                            } else {
                                player.getCheatTracker().registerOffense(CheatingOffense.ETC_EXPLOSION);
                                return;
                            }
                        } finally {
                            mapitem.getLock().unlock();
                        }
                    } else {
                        player.getCheatTracker().registerOffense(CheatingOffense.EXPLODING_NONEXISTANT);
                        return;
                    }
                }
            }
        }

        // 狂狼連擊處理
        // 燃燒場地更新
        if (attack.targets > 0) {
            player.getMap().updateBurningField();
        }

        int totDamageToOneMonster = 0;
        long hpMob = 0L;
        PlayerStats stats = player.getStat();

        int criticalDamage = stats.getMaxCritDamage();
        int shadowPartnetBuff = 0;
        if (mirror) {
            MapleStatEffect shadowPartnerEffect = player.getStatForBuff(MapleBuffStat.ShadowPartner);
            if (shadowPartnerEffect != null) {
                shadowPartnetBuff += shadowPartnerEffect.getX();
            }
            attackCount /= 2;
        }
        shadowPartnetBuff *= (criticalDamage + 100) / 100;
        if (attack.skill == 4221014 || attack.skill == 4221016) {
            shadowPartnetBuff *= 30;
        }

        // 魔幻箭筒
        if (attack.skill == 3120017) {
            effect.getMonsterStati().clear();
            if (player.getmod() == 3) {
                effect.getMonsterStati().put(MonsterStatus.M_Poison, 1);
            }
        }

        double maxDamagePerHit = 0.0D;

        List<Integer> attackedMobs = new ArrayList<>();
        StringBuilder logDamage = null;

        for (AttackPair oned : attack.allDamage) {
            MapleMonster monster = map.getMonsterByOid(oned.objectid);
            if (player.isShowInfo()) {
                player.showMessage(6, "[攻擊]怪物:" + monster);
            }
            if (monster != null && monster.getLinkCID() <= 0) {
                attackedMobs.add(monster.getObjectId());

                totDamageToOneMonster = 0;
                hpMob = monster.getMobMaxHp();
                MapleMonsterStats monsterstats = monster.getStats();
                long fixeddmg = monsterstats.getFixedDamage();
                boolean Tempest = (monster.getStatusSourceID(MonsterStatus.M_Freeze) == 21120006)
                        || (attack.skill == 21120006) || (attack.skill == 1221011);

                if (!Tempest) {
                    maxDamagePerHit = CalculateMaxWeaponDamagePerHit(player, monster, attack, usedSkill, effect,
                            maxDamagePerMonster, criticalDamage);
                }
                byte overallAttackCount = 0;

                int criticals = 0;
                logDamage = new StringBuilder();
                logDamage.append("傷害(預計):");
                for (Pair<Long, Boolean> eachde : oned.attack) {
                    Long eachd = eachde.left;
                    overallAttackCount = (byte) (overallAttackCount + 1);
                    if (eachde.right) {
                        criticals++;
                    }
                    if ((useAttackCount) && (overallAttackCount - 1 == attackCount)) {
                        double min = maxDamagePerHit;
                        double shadow = (shadowPartnetBuff == 0D ? 1D : shadowPartnetBuff);
                        if (shadowPartnetBuff != 0) {
                            min = maxDamagePerHit / 100.0D;
                        }
                        double dam = (monsterstats.isBoss() ? stats.bossdam_r : stats.dam_r);
                        double last = min * (shadow * dam / 100.0D);
                        maxDamagePerHit = last;
                    }
                    logDamage.append(eachd).append("(").append((int) maxDamagePerHit).append("),");
                    if (fixeddmg != -1) {
                        if (monsterstats.getOnlyNoramlAttack()) {
                            eachd = attack.skill != 0 ? 0L : fixeddmg;
                        } else {
                            eachd = fixeddmg;
                        }
                    } else if (monsterstats.getOnlyNoramlAttack()) {
                        eachd = attack.skill != 0 ? 0 : Math.min(eachd, (int) maxDamagePerHit);
                    } else if (Tempest) {
                        if (eachd > monster.getMobMaxHp()) {
                            eachd = Math.min(monster.getMobMaxHp(), Long.MAX_VALUE);
                            player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE);
                        }
                    } else if (((player.getJob() >= 3200) && (player.getJob() <= 3212)
                            && (!monster.isBuffed(MonsterStatus.M_Venom))
                            && (!monster.isBuffed(MonsterStatus.M_HardSkin))
                            && (!monster.isBuffed(MonsterStatus.M_Showdown)))
                            || (attack.skill == 23121003)
                            || (((player.getJob() < 3200) || (player.getJob() > 3212))
                            && (!monster.isBuffed(MonsterStatus.M_Venom))
                            && (!monster.isBuffed(MonsterStatus.M_Web))
                            && (!monster.isBuffed(MonsterStatus.M_Weakness)))) {
                        if (eachd > maxDamagePerHit) {
                            player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE,
                                    new StringBuilder().append("[傷害: ").append(eachd).append(", 預計傷害: ")
                                    .append((int) maxDamagePerHit).append(", 怪物: ").append(monster.getId())
                                    .append("] [職業: ").append(player.getJob()).append(", 等級: ")
                                    .append(player.getLevel()).append(", 技能: ").append(attack.skill).append("]")
                                    .toString());
                            if (attack.real) {
                                player.getCheatTracker().checkSameDamage(eachd, maxDamagePerHit);
                            }
                            if (eachd > maxDamagePerHit * 2.0D) {
                                player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_2,
                                        new StringBuilder().append("[傷害: ").append(eachd).append(", 預計傷害: ")
                                        .append((int) maxDamagePerHit).append(", 怪物: ").append(monster.getId())
                                        .append("] [職業: ").append(player.getJob()).append(", 等級: ")
                                        .append(player.getLevel()).append(", 技能: ").append(attack.skill)
                                        .append("]").toString());
                                eachd = (long) (maxDamagePerHit * 2.0D);
                            }
                        }

                    } else if (eachd > maxDamagePerHit) {
                        eachd = (long) maxDamagePerHit;
                    }

                    totDamageToOneMonster += eachd;

                    if (((eachd == 0) || (monster.getId() == 9700021)) && (player.getPyramidSubway() != null)) {
                        player.getPyramidSubway().onMiss(player);
                    }
                }
                if (player.isShowInfo()) {
                    player.showInfo(attackType, false, logDamage.toString());
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);

                if ((GameConstants.getAttackDelay(attack.skill, usedSkill) >= 100)
                        && (!GameConstants.isNoDelaySkill(attack.skill)) && (attack.skill != 3101005)
                        && (!monster.getStats().isBoss())
                        && (player.getTruePosition().distanceSq(monster.getTruePosition()) > GameConstants
                        .getAttackRange(effect, player.getStat().defRange))) {
                    player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, new StringBuilder()
                            .append("[範圍: ").append(player.getTruePosition().distanceSq(monster.getTruePosition()))
                            .append(", 預計範圍: ").append(GameConstants.getAttackRange(effect, player.getStat().defRange))
                            .append(" 職業: ").append(player.getJob()).append("]").toString());
                }

                if ((attack.skill == 2301002) && (!monsterstats.getUndead())) {
                    player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
                    if (player.isShowErr()) {
                        player.showInfo("魔法攻擊", true, "群體治愈無法對非不死怪物造成傷害");
                    }
                    return;
                }

                if ((totDamageToOneMonster > 0)) {

                    if (MapleJob.is暗影神偷(player.getJob())) {
                        player.getClient().announce(JobPacket.ThiefPacket.OnOffFlipTheCoin(true));
                    }

                    monster.damage(player, totDamageToOneMonster, true, attack.skill);

                    player.onAttack(monster.getMobMaxHp(), monster.getMobMaxMp(), attack.skill, monster, totDamage, 0,
                            attack.hits);

                    if (totDamageToOneMonster > 0) {
                        Item weapon_ = player.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                        if (weapon_ != null) {
                            MonsterStatus stat = GameConstants.getStatFromWeapon(weapon_.getItemId());
                            if ((stat != null) && (Randomizer.isSuccess(GameConstants.getStatChance()))) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(stat,
                                        GameConstants.getXForStat(stat),
                                        GameConstants.getSkillForStat(stat), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, 10000L, false, null);
                            }
                        }
                        if (player.getBuffedValue(MapleBuffStat.Blind) != null) {
                            MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.Blind);
                            if ((eff != null) && (eff.makeChanceResult())) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.M_ACC,
                                        eff.getX(), eff.getSourceId(), null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 1000, true, eff);
                            }
                        }
                        if (player.getBuffedValue(MapleBuffStat.IllusionStep) != null) {
                            MapleStatEffect _effect = player.getStatForBuff(MapleBuffStat.IllusionStep);
                            if ((_effect != null) && (_effect.makeChanceResult())) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.M_Speed,
                                        _effect.getX(), 3121007, null, false);
                                monster.applyStatus(player, monsterStatusEffect, false, _effect.getY() * 1000, true,
                                        _effect);
                            }
                        }
                    }
                    if (effect != null && effect.getMonsterStati().size() > 0 && effect.makeChanceResult()) {
                        for (Map.Entry z : effect.getMonsterStati().entrySet()) {
                            monster.applyStatus(player,
                                    new MonsterStatusEffect((MonsterStatus) z.getKey(), (Integer) z.getValue(),
                                            usedSkill.getId(), null, false),
                                    effect.isPoison(), effect.getDuration(), true, effect);
                        }
                    }
                }
            }
        }

        SkillHandleFetcher.onAttack(player, attack, attack.allDamage, usedSkill);

        // TODO: 處理攻擊效果
        if (!attack.allDamage.isEmpty()) {
            player.handle元素衝擊(attack.skill);
        }

        if ((hpMob > 0L) && (totDamageToOneMonster > 0)) {
            player.afterAttack(attack.targets, attack.hits, attack.skill);
        }

        boolean isApplySpecialEffect;
        switch (attack.skill) {
            case 4341002:
            case 4331003:
            case 131001001:// 皮卡啾攻擊
            case 131001002:// 皮卡啾攻擊
            case 131001003:// 皮卡啾攻擊
            case 131001101:// 皮卡啾攻擊
            case 131001102:// 皮卡啾攻擊
            case 131001103:// 皮卡啾攻擊
            case 131002000:// 皮卡啾攻擊
            case 131001000:// 皮卡啾攻擊
            case 131001113:// 電吉他
                // 必須攻擊命中才增加BUFF
                isApplySpecialEffect = attack.targets > 0;
                break;
            default:
                // 非無延遲技能
                isApplySpecialEffect = !GameConstants.isNoDelaySkill(attack.skill);
                break;
        }
        if (attack.skill != 0 && isApplySpecialEffect && (GameConstants.getLinkedAttackSkill(attack.skill) != 61120007)
                && GameConstants.getLinkedAttackSkill(attack.skill) != 61101002) {
            boolean applyTo = effect.applyTo(player, attack.position);
            if (applyTo) {
                int linkBuff = 0;
                int duration = 0;
                switch (attack.skill) {
                    case 2321008: // 天怒
                        linkBuff = 2321001; // 核爆術
                        duration = effect.getCooldown(player) * 1000;
                        break;
                }
                if (linkBuff > 0) {
                    Skill buffSkill = SkillFactory.getSkill(linkBuff);
                    if (buffSkill != null) {
                        int buffSkillLevel = player.getTotalSkillLevel(GameConstants.getLinkedAttackSkill(linkBuff));
                        MapleStatEffect buffEffect = player.inPVP() ? buffSkill.getPVPEffect(buffSkillLevel)
                                : buffSkill.getEffect(buffSkillLevel);
                        if (buffEffect != null) {
                            if (player.isShowInfo()) {
                                player.dropSpouseMessage(10,
                                        "發送攻擊技能連結的BUFF技能 - 技能ID：" + linkBuff + " 持續時間：" + duration / 1000 + "秒");
                            }
                            buffEffect.applyBuffEffect(player, duration);
                        }
                    }
                }
            }
        }
        if ((totDamage > 1) && (GameConstants.getAttackDelay(attack.skill, usedSkill) >= 100)) {
            CheatTracker tracker = player.getCheatTracker();
            tracker.setAttacksWithoutHit(true);
            if (tracker.getAttacksWithoutHit() > 1000) {
                tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT,
                        Integer.toString(tracker.getAttacksWithoutHit()));
            }
        }
        player.setTouchedRune(-1);
    }

    private static double CalculateMaxMagicDamagePerHit(MapleCharacter chr, Skill skill, MapleMonster monster,
            MapleMonsterStats mobstats, PlayerStats stats, Element elem, Integer sharpEye, double maxDamagePerMonster,
            MapleStatEffect attackEffect) {
        int dLevel = Math.max(mobstats.getLevel() - chr.getLevel(), 0) * 2;
        int hitRate = dLevel;
        hitRate -= dLevel;
        if ((hitRate <= 0) && ((!MapleJob.isBeginner(skill.getId() / 10000)) || (skill.getId() % 10000 != 1000))) {
            return 0.0D;
        }

        int CritPercent = sharpEye;
        ElementalEffectiveness ee = monster.getEffectiveness(elem);
        double elemMaxDamagePerMob;
        switch (ee) {
            case IMMUNE:
                elemMaxDamagePerMob = 1.0D;
                break;
            default:
                elemMaxDamagePerMob = ElementalStaffAttackBonus(elem, maxDamagePerMonster * ee.getValue(), stats);
        }

        int MDRate = monster.getStats().getMDRate();
        MonsterStatusEffect pdr = monster.getBuff(MonsterStatus.M_MDR);
        if (pdr != null) {
            MDRate += pdr.getX();
        }
        elemMaxDamagePerMob -= elemMaxDamagePerMob
                * (Math.max(MDRate - stats.ignoreTargetDEF - attackEffect.getIgnoreMob(), 0) / 100.0D);

        elemMaxDamagePerMob += elemMaxDamagePerMob / 100.0D * CritPercent;

        elemMaxDamagePerMob *= (monster.getStats().isBoss() ? chr.getStat().bossdam_r : 100.0) / 100.0D;
        MonsterStatusEffect imprint = monster.getBuff(MonsterStatus.M_Mystery);
        if (imprint != null) {
            elemMaxDamagePerMob += elemMaxDamagePerMob * imprint.getX() / 100.0D;
        }
        elemMaxDamagePerMob += elemMaxDamagePerMob * chr.getDamageIncrease(monster.getObjectId()) / 100.0D;
        if (MapleJob.isBeginner(skill.getId() / 10000)) {
            switch (skill.getId() % 10000) {
                case 1000:
                    elemMaxDamagePerMob = 40.0D;
                    break;
                case 1020:
                    elemMaxDamagePerMob = 1.0D;
                    break;
                case 1009:
                    elemMaxDamagePerMob = monster.getStats().isBoss() ? monster.getMobMaxHp() / 30L * 100L
                            : monster.getMobMaxHp();
            }
        }

        switch (skill.getId()) {
            case 32001000:
            case 32101000:
            case 32111002:
            case 32121002:
                elemMaxDamagePerMob *= 1.5D;
        }

        if (elemMaxDamagePerMob > 999999.0D) {
            elemMaxDamagePerMob = 999999.0D;
        } else if (elemMaxDamagePerMob <= 0.0D) {
            elemMaxDamagePerMob = 1.0D;
        }

        return elemMaxDamagePerMob;
    }

    private static double ElementalStaffAttackBonus(Element elem, double elemMaxDamagePerMob, PlayerStats stats) {
        switch (elem) {
            case FIRE:
                return elemMaxDamagePerMob / 100.0D * (stats.element_fire + stats.getElementBoost(elem));
            case ICE:
                return elemMaxDamagePerMob / 100.0D * (stats.element_ice + stats.getElementBoost(elem));
            case LIGHTING:
                return elemMaxDamagePerMob / 100.0D * (stats.element_light + stats.getElementBoost(elem));
            case POISON:
                return elemMaxDamagePerMob / 100.0D * (stats.element_psn + stats.getElementBoost(elem));
        }
        return elemMaxDamagePerMob / 100.0D * (stats.def + stats.getElementBoost(elem));
    }

    private static double CalculateMaxWeaponDamagePerHit(MapleCharacter player, MapleMonster monster, AttackInfo attack,
            Skill theSkill, MapleStatEffect attackEffect, double maximumDamageToMonster,
            Integer CriticalDamagePercent) {
        int dLevel = Math.max(monster.getStats().getLevel() - player.getLevel(), 0) * 2;
        int HitRate = 30;
        HitRate -= dLevel;
        if ((HitRate <= 0) && ((!MapleJob.isBeginner(attack.skill / 10000)) || (attack.skill % 10000 != 1000))
                && (!GameConstants.isPyramidSkill(attack.skill)) && (!GameConstants.isMulungSkill(attack.skill))
                && (!GameConstants.isInflationSkill(attack.skill))) {
            return 0.0D;
        }
        if ((player.getMapId() / 1000000 == 914) || (player.getMapId() / 1000000 == 927)) {
            return 999999.0D;
        }

        List<Element> elements = new ArrayList<>();
        boolean defined = false;
        int CritPercent = CriticalDamagePercent;
        int PDRate = monster.getStats().getPDRate();
        MonsterStatusEffect pdr = monster.getBuff(MonsterStatus.M_PDR);
        if (pdr != null) {
            PDRate += pdr.getX();
        }
        if (theSkill != null) {
            elements.add(theSkill.getElement());
            if (MapleJob.isBeginner(theSkill.getId() / 10000)) {
                switch (theSkill.getId() % 10000) {
                    case 1000:
                        maximumDamageToMonster = 40.0D;
                        defined = true;
                        break;
                    case 1020:
                        maximumDamageToMonster = 1.0D;
                        defined = true;
                        break;
                    case 1009:
                        maximumDamageToMonster = monster.getStats().isBoss() ? monster.getMobMaxHp() / 30L * 100L
                                : monster.getMobMaxHp();
                        defined = true;
                }
            }

            switch (theSkill.getId()) {
                case 1311005:
                    PDRate = monster.getStats().isBoss() ? PDRate : 0;
                    break;
                case 3221001:
                case 33101001:
                    maximumDamageToMonster *= attackEffect.getMobCount();
                    defined = true;
                    break;
                case 3101005:
                    defined = true;
                    break;
                case 32001000:
                case 32101000:
                case 32111002:
                case 32121002:
                    maximumDamageToMonster *= 1.5D;
                    break;
                case 1221009:
                case 3221007:
                case 4331003:
                    // case 23121003:
                    if (!monster.getStats().isBoss()) {
                        maximumDamageToMonster = monster.getMobMaxHp();
                        defined = true;
                    }
                    break;
                case 1221011:
                case 21120006:
                    maximumDamageToMonster = monster.getStats().isBoss() ? 500000.0D : monster.getHp() - 1L;
                    defined = true;
                    break;
                case 3211006:
                    if (monster.getStatusSourceID(MonsterStatus.M_Freeze) == 3211003) {
                        defined = true;
                        maximumDamageToMonster = 999999.0D;
                    }
                    break;
                case 33101215:
                    maximumDamageToMonster *= 20000;
                    break;
                case 4311002:
                case 4210014:
                    maximumDamageToMonster *= 60;
                    break;
                case 4341009:
                case 4341004:
                case 4331011:
                case 4321006:
                    maximumDamageToMonster *= 30;
                    break;
                case 4331006:
                case 4331000:
                case 4321002:
                case 4311003:
                case 4221052:
                case 4211011:
                case 4201012:
                case 4001334:
                    maximumDamageToMonster *= 25;
                    break;

                case 4321004:
                case 4301004:
                case 4001013:
                    maximumDamageToMonster *= 15;
                    break;
                case 80001770:
                case 61001004:
                case 36121000:
                case 4211002:
                    maximumDamageToMonster *= 10;
                    break;
                case 4341011:
                case 4221016:
                case 4221007:
                    maximumDamageToMonster *= 5;
                    break;
                case 14121001:
                case 4341002:
                case 4221010:
                    maximumDamageToMonster *= 3;
                    break;
                case 80011133:
                case 33001205:
                case 31201010:
                case 31201009:
                case 31201008:
                case 31201007:
                case 31201000:
                    maximumDamageToMonster *= 2;
                    break;
            }
        }
        double elementalMaxDamagePerMonster = maximumDamageToMonster;
        if ((player.getJob() == 311) || (player.getJob() == 312) || (player.getJob() == 321)
                || (player.getJob() == 322)) {
            Skill mortal = SkillFactory
                    .getSkill((player.getJob() == 311) || (player.getJob() == 312) ? 3110001 : 3210001);
            if (player.getTotalSkillLevel(mortal) > 0) {
                MapleStatEffect mort = mortal.getEffect(player.getTotalSkillLevel(mortal));
                if ((mort != null) && (monster.getHPPercent() < mort.getX())) {
                    elementalMaxDamagePerMonster = 999999.0D;
                    defined = true;
                    if (mort.getZ() > 0) {
                        player.addHP(player.getStat().getMaxHp() * mort.getZ() / 100);
                    }
                }
            }
        } else if ((player.getJob() == 221) || (player.getJob() == 222)) {
            Skill mortal = SkillFactory.getSkill(2210000);
            if (player.getTotalSkillLevel(mortal) > 0) {
                MapleStatEffect mort = mortal.getEffect(player.getTotalSkillLevel(mortal));
                if ((mort != null) && (monster.getHPPercent() < mort.getX())) {
                    elementalMaxDamagePerMonster = 999999.0D;
                    defined = true;
                }
            }
        }
        if ((!defined) || ((theSkill != null) && ((theSkill.getId() == 33101001) || (theSkill.getId() == 3221001)))) {
            if (player.getBuffedValue(MapleBuffStat.WeaponCharge) != null) {
                int chargeSkillId = player.getBuffSource(MapleBuffStat.WeaponCharge);

                switch (chargeSkillId) {
                    case 1201011: // 烈焰之劍Flame Charge
                        elements.add(Element.FIRE);
                        break;
                    case 1201012: // 寒冰之劍Blizzard Charge
                        elements.add(Element.ICE);
                        break;
                    case 1211008: // 雷鳴之劍Lightning Charge
                    case 15101006: // 雷鳴
                        elements.add(Element.LIGHTING);
                        break;
                    case 1221004: // 聖靈之劍Holy Charge
                    case 11111007: // 閃耀激發
                        elements.add(Element.HOLY);
                        break;
                    case 12101005:
                }

            }

            if (player.getBuffedValue(MapleBuffStat.AssistCharge) != null) {
                elements.add(Element.LIGHTING);
            }
            if (player.getBuffedValue(MapleBuffStat.ElementalReset) != null) {
                elements.clear();
            }
            double elementalEffect;
            if (elements.size() > 0) {
                switch (attack.skill) {
                    case 3111003:
                    case 3211003:
                        elementalEffect = attackEffect.getX() / 100.0D;
                        break;
                    default:
                        elementalEffect = 0.5D / elements.size();
                }

                for (Element element : elements) {
                    switch (monster.getEffectiveness(element)) {
                        case IMMUNE:
                            elementalMaxDamagePerMonster = 1.0D;
                            break;
                        case WEAK:
                            elementalMaxDamagePerMonster *= (1.0D + elementalEffect
                                    + player.getStat().getElementBoost(element));
                            break;
                        case STRONG:
                            elementalMaxDamagePerMonster *= (1.0D - elementalEffect
                                    - player.getStat().getElementBoost(element));
                    }

                }

            }

            elementalMaxDamagePerMonster -= elementalMaxDamagePerMonster
                    * (Math.max(PDRate - Math.max(player.getStat().ignoreTargetDEF, 0)
                            - Math.max(attackEffect == null ? 0 : attackEffect.getIgnoreMob(), 0), 0) / 100.0D);

            elementalMaxDamagePerMonster += elementalMaxDamagePerMonster / 100.0D * CritPercent;

            MonsterStatusEffect imprint = monster.getBuff(MonsterStatus.M_Mystery);
            if (imprint != null) {
                elementalMaxDamagePerMonster += elementalMaxDamagePerMonster * imprint.getX() / 100.0D;
            }

            double skillDamage = 100.0D;
            if (attackEffect != null) {
                skillDamage = attackEffect.getDamage() + player.getStat().getDamageIncrease(theSkill.getId());
                switch (attackEffect.getSourceId()) {
                    case 131001000:
                        skillDamage += (double) player.getLevel() * attackEffect.getY();
                        break;
                    case 131001001:
                    case 131001101:
                        skillDamage = 100.0D + 3 * player.getLevel();
                        break;
                    case 131001002:
                    case 131001102:
                    case 131001003:
                    case 131001103:
                        skillDamage = 200.0D + 3 * player.getLevel();
                        break;
                    case 131001004:
                        skillDamage = 150.0D + 2 * player.getLevel();
                        break;
                    case 131001104:
                        skillDamage = 20.0D + player.getLevel() / 3;
                        break;
                }
            }
            if (player.isShowInfo()) {
                player.showMessage(6,
                        "[傷害計算]屬性傷害：" + (int) Math.ceil(elementalMaxDamagePerMonster) + " 技能傷害："
                        + (int) Math.ceil(skillDamage) + "% BOSS傷害："
                        + (int) Math.ceil(((monster.getStats().isBoss()) && (attackEffect != null)
                                ? player.getStat().bossdam_r + attackEffect.getBossDamage()
                                : 100.0) - 100)
                        + "%(" + ((monster.getStats().isBoss()) && (attackEffect != null)) + ")");
            }
            elementalMaxDamagePerMonster += elementalMaxDamagePerMonster
                    * player.getDamageIncrease(monster.getObjectId()) / 100.0D;
            elementalMaxDamagePerMonster *= ((monster.getStats().isBoss()) && (attackEffect != null)
                    ? player.getStat().bossdam_r + attackEffect.getBossDamage()
                    : 100.0) / 100.0D;
            elementalMaxDamagePerMonster *= skillDamage / 100.0D;
        }
        if (elementalMaxDamagePerMonster > 100000000.0D) {
            if (!defined) {
                elementalMaxDamagePerMonster = 100000000.0D;
            }
        } else if (elementalMaxDamagePerMonster <= 0.0D) {
            elementalMaxDamagePerMonster = 1.0D;
        }
        return elementalMaxDamagePerMonster;
    }

    public static final AttackInfo DivideAttack(final AttackInfo attack, final int rate) {
        attack.real = false;
        if (rate <= 1) {
            return attack; // lol
        }
        for (AttackPair p : attack.allDamage) {
            if (p.attack != null) {
                for (Pair<Long, Boolean> eachd : p.attack) {
                    eachd.left /= rate; // too ex.
                }
            }
        }
        return attack;
    }

    public static final AttackInfo Modify_AttackCrit(AttackInfo attack, MapleCharacter chr, MapleStatEffect effect) {
        int CriticalRate;
        boolean shadow;
        List<Long> damages;
        List<Long> damage;
        if ((attack.skill != 4211006) && (attack.skill != 3211003) && (attack.skill != 4111004)
                && (attack.skill != 42111002)) {
            CriticalRate = chr.getStat().getCritRate() + (effect == null ? 0 : effect.getCr());
            shadow = (chr.getBuffedValue(MapleBuffStat.ShadowPartner) != null) && !MapleJob.is法師(attack.skill / 10000);
            damages = new ArrayList<>();
            damage = new ArrayList<>();

            boolean isCritical = false;

            for (AttackPair p : attack.allDamage) {
                if (p.attack != null) {
                    int hit = 0;
                    int mid_att = shadow ? p.attack.size() / 2 : p.attack.size();

                    int toCrit = (attack.skill == 4221001) || (attack.skill == 3221007) || (attack.skill == 23121003)
                            || (attack.skill == 4341005) || (attack.skill == 4331006) || (attack.skill == 21120005)
                                    ? mid_att
                                    : 0;
                    if (toCrit == 0) {
                        for (Pair<Long, Boolean> eachd : p.attack) {
                            if (!eachd.right && hit < mid_att) {
                                if (eachd.left > 999999 || Randomizer.isSuccess(CriticalRate)) {
                                    toCrit++;
                                    isCritical = true;
                                }
                                damage.add(eachd.left);
                            }
                            hit++;
                        }
                        if (toCrit == 0) {
                            damage.clear();
                        } else {
                            Collections.sort(damage);
                            for (int i = damage.size(); i > damage.size() - toCrit; i--) {
                                damages.add(damage.get(i - 1));
                            }
                            damage.clear();
                        }
                    } else {
                        hit = 0;
                        for (Pair<Long, Boolean> eachd : p.attack) {
                            if (!eachd.right) {
                                if (attack.skill == 4221001) {
                                    eachd.right = hit == 3;
                                } else if ((attack.skill == 3221007) || (attack.skill == 23121003)
                                        || (attack.skill == 21120005) || (attack.skill == 4341005)
                                        || (attack.skill == 4331006) || (eachd.left > 999999)) {
                                    eachd.right = true;
                                } else if (hit >= mid_att) {
                                    eachd.right = p.attack.get(hit - mid_att).right;
                                } else {
                                    eachd.right = damages.contains(eachd.left);
                                }
                                if (eachd.right) {
                                    isCritical = true;
                                }
                            }
                            hit++;
                        }
                        damages.clear();
                    }
                }
            }
            if (isCritical) {
                if (chr.getJob() == 422 && chr.dualBrid == 0 && chr.acaneAim < 5) {
                    chr.getClient().announce(JobPacket.ThiefPacket.OnOffFlipTheCoin(true));
                    chr.acaneAim++;
                    chr.dualBrid = 1;
                }
            }
        }
        return attack;
    }

    public static final AttackInfo parseDamage(final LittleEndianAccessor lea, final MapleCharacter chr, RecvPacketOpcode header) {
        final AttackInfo ret = new AttackInfo();
        switch (header) {
            case CP_UserMeleeAttack:
                ret.isMeleeAttack = true;
                break;
            case CP_UserShootAttack:
                ret.isShootAttack = true;
                break;
            case CP_UserMagicAttack:
                ret.isMagicAttack = true;
                break;
            case CP_UserBodyAttack:
                ret.isBodyAttack = true;
                break;
            case CP_UserAreaDotAttack:
                ret.isDotAttack = true;
                break;
            default:
                if (chr.isShowErr()) {
                    chr.showInfo("分析攻擊", true, "類型[" + header.name() + "]未處理或這個攻擊類型不適用這個分析函數");
                }
                return null;
        }

        if (ret.isBodyAttack && chr.getBuffedValue(MapleBuffStat.EnergyCharged) == null
                && // 能量获得
                chr.getBuffedValue(MapleBuffStat.BodyPressure) == null
                && // 強化連擊
                chr.getBuffedValue(MapleBuffStat.DARK_AURA_OLD) == null
                && // 黑色繩索
                chr.getBuffedValue(MapleBuffStat.HowlingMaxMP) == null
                && // 煉獄颶風
                chr.getBuffedValue(MapleBuffStat.SUMMON) == null
                && // 召唤兽
                chr.getBuffedValue(MapleBuffStat.Dance) == null
                && // 地雷
                chr.getBuffedValue(MapleBuffStat.TeleportMasteryOn) == null
                && // 瞬間移動精通
                chr.getBuffedValue(MapleBuffStat.Asura) == null
                && // 修羅
                chr.getTotalSkillLevel(SkillFactory.getSkill(131000016)) <= 0 // 皮卡啾的品格
                ) {
            return null;
        }

        if (ret.isShootAttack) {
            lea.skip(1);
        }
        lea.skip(1);

        // 攻擊怪物個數 & 傷害段數數據
        ret.tbyte = lea.readByte();
        ret.targets = ((byte) (ret.tbyte >>> 4 & 0xF));
        ret.hits = ((byte) (ret.tbyte & 0xF));

        // 技能ID
        ret.skill = lea.readInt();
        Skill skill = SkillFactory.getSkill(ret.skill);
        if (skill == null && ret.skill != 0) {
            if (chr.isShowErr()) {
                chr.showInfo("分析攻擊", true, "類型[" + header.name() + "]技能不存在, 技能ID:" + ret.skill);
            }
            return null;
        }

        // 技能等級
        ret.level = lea.readByte();
        if (chr.getTotalSkillLevel(skill) != ret.level || (ret.level <= 0 && ret.skill != 0)) {
            if (chr.isShowErr()) {
                chr.showInfo("分析攻擊", true, "類型[" + header.name() + "]技能等級不正確, 技能: " + skill.getName() + "(" + ret.skill + ") 等級:" + ret.level + " 實際:" + chr.getTotalSkillLevel(skill));
            }
            return null;
        }

        if (!ret.isMagicAttack && !GameConstants.isEnergyBuff(ret.skill)) {
            lea.skip(1);
        }
        int nSkillCRC = lea.readInt(); // nSkillCRC

        boolean charge = false;
        switch (ret.skill) {
            case 2221012: // 冰鋒刃
            case 4221052: // 暗影霧殺
            case 11121055:// 黃泉十字架
            case 27121201:// 晨星殞落
            case 27120211:// 晨星殞落
            case 61111100:// 龍劍風
            case 61111218:// 龍劍風
            case 65121052:// 超級超新星
                charge = true;
                break;
        }
        if ((skill != null && skill.isChargeSkill() && ret.skill != 4341052/* 修羅 */ && ret.skill != 101110104/* 進階旋風 */) || charge) {
            ret.charge = lea.readInt();
            if (ret.skill == 3111013) { // 箭座
                lea.skip(8);
            }
        } else {
            ret.charge = 0;
        }

        lea.readByte();
        if (MapleJob.is神之子(ret.skill / 10000)) {
            ret.zeroUnk = lea.readByte();
        }
        if (MapleJob.is精靈遊俠(ret.skill / 10000)) {
            switch (ret.skill) {
                case 20021166: // 光速雙擊
                case 23120011: // 旋風月光翻轉
                case 23121052: // 憤怒天使
                    break;
                default:
                    lea.readInt();
            }
        }
        if (ret.isShootAttack) {
            ret.starSlot = ((byte) lea.readShort());
            ret.cashSlot = ((byte) lea.readShort());
            ret.AOE = lea.readByte();
        }

        lea.skip(4);
        lea.skip(14);
        ret.direction = lea.readByte();
        ret.display = lea.readShort();
        // 猩猩火酒
        if (ret.skill == 42120003) {
            lea.skip(4);
            // 元素火焰 IV || 元素火焰 III || 元素火焰 II || 元素火焰
        } else if (ret.skill == 12120010 || ret.skill == 12110028 || ret.skill == 12100028 || ret.skill == 12000026) {
            lea.skip(6);
            // 皮卡啾的品格
        } else if (ret.skill != 131000016) {
            lea.skip(4);
        }

        // 火藥桶破壞 || 颶風飛擊 || 颶風飛擊 || 蝙蝠群
        if (ret.skill == 5300007 || ret.skill == 5101012 || ret.skill == 15101010 || ret.skill == 31201001) {
            lea.readInt();
        }

        if (ret.isBodyAttack) {
            lea.skip(3);
        }

        switch (ret.skill) {
            case 14111022:// 星塵
            case 14111023:// 星塵
                lea.skip(4);
                break;
            case 3121013: // 延伸彈匣
            case 5220023:
            case 5221022: // 海盜砲擊艇
            case 5310011:
            case 95001000: // 箭座
                lea.skip(8);
                break;
            case 23111001: // 落葉旋風射擊
            case 36111010: // 戰鬥轉換：分裂
                lea.skip(12);
                break;
        }
        if (ret.skill == 5311010) { // 狂暴猴子
            if (lea.available() > 18) {
                lea.skip(8);
            }
        }
        ret.speed = lea.readByte();
        lea.readByte();
        ret.lastAttackTickCount = lea.readInt();
        lea.skip(4);
        if (!GameConstants.isEnergyBuff(ret.skill)) {
            if ((!ret.isMagicAttack && !ret.isShootAttack && !ret.isBodyAttack)
                    || ret.skill == 14000028 // 暗影蝙蝠
                    || ret.skill == 12120010 // 元素火焰 IV
                    || ret.skill == 12110028 // 元素火焰 III
                    || ret.skill == 12100028 // 元素火焰 II
                    || ret.skill == 12000026 // 元素火焰
                    || ret.skill == 101120104 // 進階碎地猛擊
                    ) {
                int linkskill = lea.readInt();
                if (linkskill > 0) {
                    lea.skip(2);
                }
            }
        }

        if (ret.isDotAttack) {
            lea.readInt();
        }
        if (ret.isShootAttack) {
            lea.skip(3);
            lea.readInt();
            lea.readInt();
        }

        switch (ret.skill) {
            case 14111022:
            case 14111023:
            case 14121004:
                lea.skip(2);
                break;
            case 14121052:
            case 14000028: // // 暗影蝙蝠
                lea.skip(6);
                break;
            case 5111009: // 雙重螺旋
                lea.skip(1);
            case 131001000:
            case 131001001:
            case 131001002:
            case 131001101:
            case 131001102:
                // case 80001770:
                lea.skip(4);
                break;
        }

        if (chr.isShowInfo()) {
            chr.dropMessage(-5, "分析攻擊類型[" + header.name() + "]技能[" + (skill != null ? skill.getName() : "普通攻擊") + "(" + ret.skill + ")] - 攻擊數量: " + ret.targets + " - 攻擊時間" + ret.lastAttackTickCount);
        }

        String msg;
        int oid;
        short unktype;
        boolean crit;
        int monsterId;
        byte bullets;
        long damage;
        MapleMonster mob;
        ret.allDamage = new ArrayList<>();
        for (int i = 0; i < ret.targets; i++) {
            oid = lea.readInt();
            unktype = lea.readShort();
            lea.readByte();
            lea.readShort();
            monsterId = lea.readInt();
            mob = MapleLifeFactory.getMonster(monsterId);
            if (mob == null) {
                if (chr.isShowErr()) {
                    chr.showInfo("分析攻擊", true, "類型[" + header.name() + "]找不到怪物:" + monsterId);
                }
                return null;
            }
            lea.readByte();
            lea.readInt();
            lea.readInt();
            if (ret.isMagicAttack) {
                lea.read(1);
            }
            switch (ret.skill) {
                // 楓幣炸彈
                case 4211006:
                    lea.skip(2);
                    bullets = lea.readByte();
                    break;
                // 紅玉咒印
                case 42111002:
                    lea.skip(1);
                    bullets = lea.readByte();
                    break;
                default:
                    lea.skip(10);
                    bullets = ret.hits;
                    break;
            }
            msg = "怪物[" + (i + 1) + ")" + mob + "]OID[" + oid + "]段數[" + bullets + "]傷害:";
            List<Pair<Long, Boolean>> allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < bullets; j++) {
                damage = lea.readLong();
                allDamageNumbers.add(new Pair<>(damage, false));
                msg += damage;
                if (j < bullets - 1) {
                    msg += ",";
                }
            }
            if (chr.isShowInfo()) {
                chr.dropMessage(-5, msg);
            }
            lea.skip(4);
            lea.skip(4);
            // 2D骨骼怪物特殊內容
            if (mob.getStats().getSkeleton() == 1) {
                lea.read(1);
                lea.readMapleAsciiString();
                lea.skip(2 + 4);
                int num = lea.readInt();
                for (int j = 0; j < num; j++) {
                    lea.readMapleAsciiString();
                }
                lea.skip(3);
            } else {
                lea.skip(4);
            }

            switch (ret.skill) {
                case 142120001: // 猛烈心靈2
                    lea.skip(8);
                    break;
            }
            lea.skip(4);
            lea.skip(4);
            lea.skip(2);
            Point pos = lea.readPos();
            // if (ret.isMagicAttack) {
            // lea.skip(1);
            // }
            ret.allDamage.add(new AttackPair(oid, allDamageNumbers, unktype));
        }

        if (ret.skill != 5220023 && (ret.skill != 4341052 || ret.skill != 4341052 && lea.available() == 4L)) {
            ret.position = lea.readPos();
        }

        if (ret.isMagicAttack) {
            lea.skip(1); // 00
        }

        int dropOid;
        short targets;
        switch (ret.skill) {
            // 致命毒霧
            case 2111003:
            // 地獄爆發
            case 2121003:
                lea.skip(1);
                break;
            // 楓幣炸彈
            case 4211006:
            // 紅玉咒印
            case 42111002:
                // lea.skip(16);
                bullets = lea.readByte();
                for (int j = 0; j < bullets; j++) {
                    dropOid = lea.readInt();
                    targets = lea.readShort(); // 當前掉寶攻擊怪物個數
                    if (chr.isShowInfo()) {
                        chr.dropMessage(-5,
                                "楓幣炸彈/紅玉咒印攻擊怪物: 掉寶OID: " + dropOid + "(" + (j + 1) + "/" + bullets + ") 掉寶攻擊次數 " + targets);
                    }
                    ret.allDamage.add(new AttackPair(dropOid, null, targets));
                }
                lea.readShort(); // 未知
                break;
        }

        switch (ret.skill) {
            case 2111003: // 致命毒霧
            case 2201009: // 寒冰瞬移
                lea.readInt();
                break;
            case 2121003: // 地獄爆發
                while (lea.available() >= 4) {
                    lea.readInt(); // MistOid
                }
                break;
        }

        if ((ret.isShootAttack && lea.available() == 4L) || (ret.isMagicAttack && MapleJob.is龍魔導士(ret.skill / 10000))) {
            ret.skillposition = lea.readPos();
        }

        switch (ret.skill) {
            case 2221012: // 冰鋒刃
                lea.skip(4);
                break;
            case 23121002: // 傳說之槍
                lea.skip(1);
                break;
            case 42100007: // 御身消滅
                lea.skip(2);
                break;
            case 4341052: // 修羅
                lea.skip(4);
                break;
            case 61121222: // 惡魔之嘆
            case 61121105: // 龍烈焰
                lea.skip(18);
                break;
            case 24121052: // 玫瑰四重曲
                lea.skip(22);
                break;
        }
        switch (ret.skill) {
            case 21001010:
            case 21000007:
            case 21000006:
                lea.skip(1);
                break;
            default:
                break;
        }
        if (lea.available() > 0) {
            if (chr.isShowErr()) {
                chr.showInfo("分析攻擊", true, "類型[" + header.name() + "]分析錯誤,有剩餘封包:" + lea.toString());
            }
        }
        return ret;
    }
}
