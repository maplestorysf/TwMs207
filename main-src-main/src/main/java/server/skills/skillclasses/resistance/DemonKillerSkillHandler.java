/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.resistance;

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
import server.Randomizer;
import server.life.MapleMonster;
import server.skills.AbstractSkillHandler;
import tools.AttackPair;
import tools.packet.CField;

/**
 *
 * @author Weber
 */
public class DemonKillerSkillHandler extends AbstractSkillHandler {

    private final List<Integer> dranPowerSkills = Arrays.asList(31000004, 31001006, 31001007, 31001008, 30010166,
            30011167, 30011168, 30011169, 30011170);

    public DemonKillerSkillHandler() {
    }

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {
        // 處理吸取力量
        if (canUseDranPower(skill)) {
            List<Integer> objectIds = new ArrayList<>();
            List<ForceAtom> forceInfo = new ArrayList<>();
            attackPairs.forEach(attackPair -> {
                objectIds.add(attackPair.objectid);
                int maxForceGain = attackPair.attack.size();
                if (Randomizer.isSuccess(player.getStat().mpRecoverProp)) {
                    maxForceGain += 2;
                    for (int i = 0; i < maxForceGain; i++) {
                        if (Randomizer.isSuccess(30)) { // 惡魔殺手汲取能量的機率
                            forceInfo.add(player.getNewAtom(skill.getId(), true));
                        }
                    }
                }
            });
            if (!forceInfo.isEmpty()) {
                player.getClient().announce(CField.OnCreateForceAtom(true, player, objectIds,
                        ForceAtomType.DemonKillerDran.getValue(), skill.getId(), forceInfo, null, null, 0, 0, 0));
            }
        }

    }

    private boolean canUseDranPower(Skill skill) {
        return skill != null ? dranPowerSkills.contains(skill.getId()) : false;
    }

    @Override
    public void onForceAtomCollision(MapleCharacter player, Skill skill, List<ForceAtom> forceAtoms,
            List<Integer> objectIds) {
        player.addMP(forceAtoms.size());
        forceAtoms.forEach(forceAtom -> player.removeForceAtom(forceAtom));
    }

    @Override
    public boolean checkJob(MapleCharacter player) {
        return MapleJob.is惡魔殺手(player.getJob());
    }

    @Override
    public void onSpecialSkill(MapleCharacter player, List<Integer> listObjIds, Skill skill) {
    }

    @Override
    public void onDamage(MapleCharacter player, MapleMonster monster) {
        Skill powerDefense = SkillFactory.getSkill(31110008);
    }
}
