/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.adventurer;

import client.MapleCharacter;
import client.MapleJob;
import client.forceatoms.ForceAtom;
import client.skill.Skill;
import client.skill.SkillFactory;
import handling.channel.handler.AttackInfo;
import java.util.List;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.skills.AbstractSkillHandler;
import tools.AttackPair;

/**
 *
 * @author Weber
 */
public class MagicianSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {

        attackPairs.forEach(attackPair -> {
            MapleMonster monster = player.getMap().getMonsterByOid(attackPair.objectid);
            if (monster == null) {
                return;
            }
            // 神秘狙擊
            int[] venomskills = {2120010, 2220010, 2320011};
            for (int skillId : venomskills) {
                final Skill venomskill = SkillFactory.getSkill(skillId);
                if (player.getTotalSkillLevel(venomskill) > 0) {
                    int hit = attackInfo.hits;
                    if (player.getAllLinkMid().isEmpty() || player.getAllLinkMid().size() < 3) {
                        for (int h = 0; h < hit; h++) {
                            hit--;
                            for (int n = 0; n < 3; n++) {
                                if (!player.getAllLinkMid().containsKey(n)) {
                                    player.getAllLinkMid().put(n, 0);
                                    break;
                                }
                            }
                            if (player.getAllLinkMid().size() == 3) {
                                break;
                            }
                        }
                    }
                    boolean apply = false;
                    final MapleStatEffect venomEffect = venomskill.getEffect(player.getTotalSkillLevel(venomskill));
                    for (int h = 0; h < hit; h++) {
                        if (venomEffect.makeChanceResult()) {
                            int value = 0;
                            value = player.getAllLinkMid().values().stream().map((val) -> val).reduce(value,
                                    Integer::sum);
                            if (value / venomEffect.getX() < venomEffect.getY()) {
                                player.getAllLinkMid().put(monster.getObjectId(),
                                        (player.getAllLinkMid().containsKey(monster.getObjectId())
                                        ? player.getAllLinkMid().get(monster.getObjectId())
                                        : 0) + venomEffect.getX());
                            }
                            apply = true;
                        }
                    }
                    if (apply) {
                        venomEffect.applyTo(player);
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
        return MapleJob.is法師(player.getJob());
    }

}
