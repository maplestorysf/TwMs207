/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.cygnus;

import client.character.stat.MapleBuffStat;
import client.MapleCharacter;
import client.MapleJob;
import client.skill.Skill;
import client.skill.SkillFactory;
import client.forceatoms.ForceAtom;
import client.forceatoms.ForceAtomType;
import handling.channel.handler.AttackInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.skills.AbstractSkillHandler;
import tools.AttackPair;
import tools.packet.CField;

/**
 *
 * @author Weber
 */
public class WindArcherSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {
        List<Integer> objectIds = new ArrayList<>();
        attackPairs.forEach(attackPair -> {
            objectIds.add(attackPair.objectid);
        });
        List<ForceAtom> forceAtoms = new ArrayList<>();

        attackPairs.forEach(attackPair -> {
            objectIds.add(attackPair.objectid);
        });

        if (player.getBuffedValue(MapleBuffStat.TriflingWhimOnOff) != null) {
            List<Integer> trifingSkills = Arrays.asList(13100022, 13110022, 13120003);
            List<Integer> advTrifingSkills = Arrays.asList(13100027, 13110027, 13120010);
            int index = 0;
            int usedSkillLevel = 0;
            int usedSkill = 0;
            for (int i = 0; i < trifingSkills.size(); i++) {
                if (player.getTotalSkillLevel(trifingSkills.get(i)) > 0) {
                    index = i;
                    usedSkill = trifingSkills.get(i);
                    usedSkillLevel = player.getTotalSkillLevel(trifingSkills.get(i));
                }
            }
            if (skill != null) {
                if (trifingSkills.contains(skill.getId())) {
                    return;
                }
                if (advTrifingSkills.contains(skill.getId())) {
                    return;
                }
            }
            if (usedSkillLevel > 0) {
                MapleStatEffect effect = SkillFactory.getSkill(usedSkill).getEffect(usedSkillLevel);
                if (effect != null) {
                    int count = effect.getX();
                    for (int i = 0; i < count; i++) {
                        if (effect.makeChanceResult()) {
                            if (effect.makeSubChanceResult()) {
                                usedSkill = advTrifingSkills.get(i);
                            }
                            forceAtoms.add(player.getNewAtom(usedSkill, false));
                        }
                    }
                }
            }
            if (!forceAtoms.isEmpty()) {
                player.getClient().announce(CField.OnCreateForceAtom(false, player, objectIds,
                        ForceAtomType.WindFairyArrow.getValue(), usedSkill, forceAtoms, null, null, 0, 0, 0));
            }
        }
        if (player.getBuffedValue(MapleBuffStat.StormBringer) != null) {
            int usedSkill = 13121054;
            int usedSkillLevel = player.getTotalSkillLevel(usedSkill);
            List<ForceAtom> atoms = new ArrayList<>();
            if (usedSkillLevel > 0) {
                MapleStatEffect effect = SkillFactory.getSkill(usedSkill).getEffect(usedSkillLevel);
                if (effect != null && effect.makeChanceResult()) {
                    atoms.clear();
                    atoms.add(player.getNewAtom(usedSkill, false));
                    player.getClient().announce(CField.OnCreateForceAtom(false, player, objectIds,
                            ForceAtomType.WindStormBringer.getValue(), usedSkill, atoms, null, null, 0, 0, 0));
                }
            }
        }

        // 400031022 風轉奇想
    }

    @Override
    public void onSpecialSkill(MapleCharacter player, List<Integer> listObjIds, Skill skill) {
    }

    @Override
    public void onForceAtomCollision(MapleCharacter player, Skill skill, List<ForceAtom> forceAtoms,
            List<Integer> objectIds) {
        forceAtoms.forEach(forceAtom -> player.removeForceAtom(forceAtom));
    }

    @Override
    public boolean checkJob(MapleCharacter player) {
        return MapleJob.is破風使者(player.getJob());
    }

    @Override
    public void onDamage(MapleCharacter player, MapleMonster monster) {

    }
}
