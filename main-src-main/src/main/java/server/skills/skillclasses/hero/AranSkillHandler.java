/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.hero;

import client.MapleCharacter;
import client.MapleJob;
import client.MonsterStatus;
import client.MonsterStatusEffect;
import client.character.stat.MapleBuffStat;
import client.forceatoms.ForceAtom;
import client.skill.Skill;
import handling.channel.handler.AttackInfo;
import handling.channel.handler.PlayerHandler;
import java.util.List;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.skills.AbstractSkillHandler;
import tools.AttackPair;

/**
 *
 * @author Weber
 */
public class AranSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {
        // 狂狼連擊處理
        PlayerHandler.AranCombo(player, attackInfo.hits * attackInfo.targets);

        attackPairs.forEach(attackPair -> {
            MapleMonster monster = player.getMap().getMonsterByOid(attackPair.objectid);
            if (monster == null) {
                return;
            }

            switch (attackInfo.skill) {
                case 21000002:
                case 21100001:
                case 21100002:
                case 21100004:
                case 21110002:
                case 21110003:
                case 21110004:
                case 21110006:
                case 21110007:
                case 21110008:
                case 21120002:
                case 21120005:
                case 21120006:
                case 21120009:
                case 21120010:
                    if ((player.getBuffedValue(MapleBuffStat.WeaponCharge) != null) && (!monster.getStats().isBoss())) {
                        MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.WeaponCharge);
                        if (eff != null) {
                            monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.M_Speed, eff.getX(),
                                    eff.getSourceId(), null, false), false, eff.getY() * 1000, true, eff);
                        }
                    }
                    if ((player.getBuffedValue(MapleBuffStat.BodyPressure) != null) && (!monster.getStats().isBoss())) {
                        MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.BodyPressure);

                        if ((eff != null) && (eff.makeChanceResult()) && (!monster.isBuffed(MonsterStatus.M_Dark))) {
                            monster.applyStatus(player,
                                    new MonsterStatusEffect(MonsterStatus.M_Dark, 1, eff.getSourceId(), null, false), false,
                                    eff.getX() * 1000, true, eff);
                        }
                    }
                    break;
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
        return MapleJob.is狂狼勇士(player.getJob());
    }

}
