/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.nova;

import client.MapleCharacter;
import client.MapleJob;
import client.character.stat.MapleBuffStat;
import client.forceatoms.ForceAtom;
import client.skill.Skill;
import handling.channel.handler.AttackInfo;
import java.util.List;
import server.life.MapleMonster;
import server.skills.AbstractSkillHandler;
import tools.AttackPair;

/**
 *
 * @author Weber
 */
public class KaiserSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {
        attackPairs.forEach(attackPair -> {
            MapleMonster monster = player.getMap().getMonsterByOid(attackPair.objectid);
            if (monster == null) {
                return;
            }
            player.handleKaiserCombo();
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
        return MapleJob.is凱撒(player.getJob());
    }

}
