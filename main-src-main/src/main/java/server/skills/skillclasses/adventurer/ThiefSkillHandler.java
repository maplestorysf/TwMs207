package server.skills.skillclasses.adventurer;

import client.MapleCharacter;
import client.MapleJob;
import client.MonsterStatus;
import client.MonsterStatusEffect;
import client.character.stat.MapleBuffStat;
import client.forceatoms.ForceAtom;
import client.skill.Skill;
import client.skill.SkillFactory;
import handling.channel.handler.AttackInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.skills.AbstractSkillHandler;
import tools.AttackPair;
import tools.Pair;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Weber
 */
public class ThiefSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {
        List<Integer> objectIds = new ArrayList<>();
        attackPairs.forEach(attackPair -> {
            objectIds.add(attackPair.objectid);
            MapleMap map = player.getMap();
            MapleMonster monster = player.getMap().getMonsterByOid(attackPair.objectid);
            if (monster == null) {
                return;
            }
            MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(skill));

            // 妙手術
            if (attackInfo.skill == 4201004) {
                monster.handleSteal(player);
            }

            // 飛毒殺
            switch (attackInfo.skill) {
                case 4001002:
                case 4001334:
                case 4001344:
                case 4111005:
                case 4121007:
                case 4201005:
                case 4211002:
                case 4221001:
                case 4221007:
                case 4301001:
                case 4311002:
                case 4311003:
                case 4331000:
                case 4331004:
                case 4331005:
                case 4331006:
                case 4341002:
                case 4341004:
                case 4341005:
                case 4341009:
                case 14001004:
                case 14111002:
                case 14111005:
                    int[] venomSkillIds = {4120005, 4220005, 4340001, 14110004};
                    for (int venomSkillId : venomSkillIds) {
                        Skill venomSkill = SkillFactory.getSkill(venomSkillId);
                        if (player.getTotalSkillLevel(venomSkill) > 0) {
                            MapleStatEffect venomEffect = venomSkill.getEffect(player.getTotalSkillLevel(venomSkill));
                            if (!venomEffect.makeChanceResult()) {
                                break;
                            }
                            monster.applyStatus(player,
                                    new MonsterStatusEffect(MonsterStatus.M_Poison, 1, venomSkillId, null, false), true,
                                    venomEffect.getDuration(), true, venomEffect);
                            break;
                        }
                    }
                    break;
            }

            // 勇者掠奪術
            if (player.getBuffedValue(MapleBuffStat.PickPocket) != null) {
                switch (skill.getId()) {
                    case 0:// 普通攻擊
                    case 4001334:// 劈空斬
                    case 4201012:// 迴旋斬
                    case 4211002:// 順影殺
                    case 4211011:// 高速鋒刃
                    case 4221007:// 順步連擊
                    case 4221010:// 穢土轉生。改
                    case 4221014:// 致命暗殺
                    case 4221052:// 暗影霧殺
                        int maxMeso = player.getBuffedValue(MapleBuffStat.PickPocket);
                        for (Pair<Long, Boolean> eachde : attackPair.attack) {
                            long eachd = eachde.left;
                            if (Randomizer.isSuccess(player.getStat().pickRate)) {
                                player.getMap().spawnMesoDrop(
                                        Math.min((int) Math.max(eachd / 20000.0D * maxMeso, 1.0D), maxMeso),
                                        new Point(
                                                (int) (monster.getTruePosition().getX() + Randomizer.nextInt(100) - 50.0D),
                                                (int) monster.getTruePosition().getY()),
                                        monster, player, false, (byte) 0);
                            }
                        }
                        break;
                }
            }
        });
    }

    @Override
    public void onSpecialSkill(MapleCharacter player, List<Integer> listObjIds, Skill skill) {
    }

    @Override
    public void onForceAtomCollision(MapleCharacter player, Skill skill, List<ForceAtom> forceAtoms,
            List<Integer> objectIds) {
    }

    @Override
    public void onDamage(MapleCharacter player, MapleMonster monster) {
    }

    @Override
    public boolean checkJob(MapleCharacter player) {
        return MapleJob.is盜賊(player.getJob());
    }

}
