/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills;

import client.MapleCharacter;
import client.skill.Skill;
import client.forceatoms.ForceAtom;
import handling.channel.handler.AttackInfo;
import java.util.List;
import server.life.MapleMonster;
import tools.AttackPair;

/**
 *
 * @author Weber
 */
public interface ISkillHandler {

    void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill);

    void onSpecialSkill(MapleCharacter player, List<Integer> listObjIds, Skill skill);

    void onForceAtomCollision(MapleCharacter player, Skill skill, List<ForceAtom> forceAtoms, List<Integer> objectIds);

    void onDamage(MapleCharacter player, MapleMonster monster);

    boolean checkJob(MapleCharacter player);
}
