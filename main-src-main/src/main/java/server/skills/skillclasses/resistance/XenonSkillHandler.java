/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.resistance;

import client.MapleCharacter;
import client.MapleJob;
import client.MonsterStatus;
import client.MonsterStatusEffect;
import client.skill.Skill;
import client.skill.SkillFactory;
import client.forceatoms.ForceAtom;
import handling.channel.handler.AttackInfo;
import java.util.List;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.skills.AbstractSkillHandler;
import tools.AttackPair;
import tools.packet.SkillPacket;

/**
 *
 * @author Weber
 */
public class XenonSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {

        attackPairs.forEach(attackPair -> {
            MapleMonster monster = player.getMap().getMonsterByOid(attackPair.objectid);
            if (monster == null) {
                return;
            }
            // 三角列陣
            if (player.getTotalSkillLevel(36110005) > 0) {
                Skill _skill = SkillFactory.getSkill(36110005);
                MapleStatEffect _effect = _skill.getEffect(player.getTotalSkillLevel(_skill));
                if (player.getLastCombo() + 5000 < System.currentTimeMillis()) {
                    monster.setTriangulation(0);
                }
                if (_effect.makeChanceResult()) {
                    player.setLastCombo(System.currentTimeMillis());
                    if (monster.getTriangulation() < 3) {
                        monster.setTriangulation(monster.getTriangulation() + 1);
                    }
                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.M_Darkness, _effect.getX(),
                            _effect.getSourceId(), null, false), false, _effect.getY() * 1000, true, _effect);
                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.M_TempMoveAbility,
                            monster.getTriangulation(), _effect.getSourceId(), null, false), false,
                            _effect.getY() * 1000, true, _effect);
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
        handleAegisSystem(player, monster);
    }

    private void handleAegisSystem(MapleCharacter player, MapleMonster monster) {
        // 神盾系統
        Skill aegisSystem = SkillFactory.getSkill(36111004);
        if (monster != null) {
            long currentTime = System.currentTimeMillis();
            if (player.getSkillLevel(aegisSystem) > 0 && currentTime - player.getStiffTime() > 1500) {
                player.setStiffTime(currentTime);
                MapleStatEffect effect = aegisSystem.getEffect(player.getTotalSkillLevel(aegisSystem));
                if (effect != null && effect.makeChanceResult()) {
                    player.getMap().broadcastMessage(player, SkillPacket.AccurateRocket(player, aegisSystem.getId(),
                            null, monster.getObjectId(), 3, 3, true), true);
                }
            }
        }
    }

    @Override
    public boolean checkJob(MapleCharacter player) {
        return MapleJob.is傑諾(player.getJob());
    }

}
