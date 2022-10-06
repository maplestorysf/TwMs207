/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills;

import server.skills.skillclasses.resistance.DemonAvangerSkillHandler;
import server.skills.skillclasses.nova.AngelBusterSkillHandler;
import server.skills.skillclasses.resistance.DemonKillerSkillHandler;
import server.skills.skillclasses.cygnus.NightWalkerSkillHandler;
import server.skills.skillclasses.cygnus.WindArcherSkillHandler;
import server.skills.skillclasses.hero.PhantomSkillHandler;
import client.MapleCharacter;
import client.skill.Skill;
import client.forceatoms.ForceAtom;
import handling.channel.handler.AttackInfo;
import java.util.List;
import server.life.MapleMonster;
import server.skills.skillclasses.adventurer.ThiefSkillHandler;
import tools.AttackPair;

/**
 *
 * @author Weber
 */
public class SkillHandleFetcher {

    public static final ISkillHandler[] HANDLERS = {new DemonAvangerSkillHandler(), new PhantomSkillHandler(),
        new WindArcherSkillHandler(), new DemonKillerSkillHandler(), new AngelBusterSkillHandler(),
        new NightWalkerSkillHandler(), new ThiefSkillHandler(),};

    public static void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs,
            Skill skill) {
        for (ISkillHandler handler : HANDLERS) {
            if (handler.checkJob(player)) {
                handler.onAttack(player, attackInfo, attackPairs, skill);
            }
        }
    }

    public static void onSpecialSkill(MapleCharacter player, List<Integer> listObjIds, Skill skill) {
        for (ISkillHandler handler : HANDLERS) {
            if (handler.checkJob(player)) {
                handler.onSpecialSkill(player, listObjIds, skill);
            }
        }
    }

    public static void onCollision(MapleCharacter player, Skill skill, List<ForceAtom> forceAtoms,
            List<Integer> objectIds) {
        for (ISkillHandler handler : HANDLERS) {
            if (handler.checkJob(player)) {
                handler.onForceAtomCollision(player, skill, forceAtoms, objectIds);
            }
        }
    }

    public static void onDamage(MapleCharacter player, MapleMonster attacker) {
        for (ISkillHandler handler : HANDLERS) {
            if (handler.checkJob(player)) {
                handler.onDamage(player, attacker);
            }
        }
    }

}
