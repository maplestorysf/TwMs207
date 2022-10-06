/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.nova;

import client.MapleCharacter;
import client.MapleJob;
import client.skill.Skill;
import client.skill.SkillFactory;
import client.forceatoms.ForceAtom;
import client.forceatoms.ForceAtomType;
import handling.channel.handler.AttackInfo;
import java.util.ArrayList;
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
public class AngelBusterSkillHandler extends AbstractSkillHandler {

    public AngelBusterSkillHandler() {
    }

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {
    }

    @Override
    public void onForceAtomCollision(MapleCharacter player, Skill atomSkill, List<ForceAtom> forceAtoms,
            List<Integer> objectIds) {

        List<ForceAtom> newForceAtoms = new ArrayList<>();
        Skill drainSoul = SkillFactory.getSkill(65111100);
        if (atomSkill != null) {
            if (player.getSkillLevel(atomSkill) > 0) {
                // 靈魂探求者
                if (atomSkill.getId() == drainSoul.getId()) {
                    MapleStatEffect effect = atomSkill.getEffect(player.getSkillLevel(atomSkill));
                    if (effect.makeChanceResult()) {
                        forceAtoms.forEach(forceAtom -> {
                            int nextNum = forceAtom.getNum() + 1;
                            if (nextNum < effect.getZ()) {
                                ForceAtom nAtom = player.getNewAtom(atomSkill.getId(), true);
                                nAtom.setNum(nextNum);
                                newForceAtoms.add(nAtom);
                                player.removeForceAtom(forceAtom);
                            }
                        });
                    }
                }
            }
        }
        if (!newForceAtoms.isEmpty()) {
            player.getMap()
                    .broadcastGMMessage(player, CField.OnCreateForceAtom(true, player, objectIds,
                            ForceAtomType.DrainSoul.getValue(), atomSkill.getId(), newForceAtoms, null, null, 0, 0, 0),
                            true);
        }
    }

    @Override
    public boolean checkJob(MapleCharacter player) {
        return MapleJob.is天使破壞者(player.getJob());
    }

    @Override
    public void onSpecialSkill(MapleCharacter player, List<Integer> listObjIds, Skill skill) {
        List<ForceAtom> forceAtoms = new ArrayList<>();
        if (skill.getId() == 65111100 && player.getSkillLevel(skill) > 0) {
            MapleStatEffect effect = skill.getEffect(player.getSkillLevel(skill));
            int count = effect.getMobCount();
            for (int i = 0; i < count; i++) {
                forceAtoms.add(player.getNewAtom(skill.getId(), false));
            }

        }
        if (!forceAtoms.isEmpty()) {
            player.getMap().broadcastMessage(player, CField.OnCreateForceAtom(false, player, listObjIds,
                    ForceAtomType.DrainSoul.getValue(), skill.getId(), forceAtoms, null, null, 0, 0, 0), true);
        }
    }

    @Override
    public void onDamage(MapleCharacter player, MapleMonster monster) {

    }
}
