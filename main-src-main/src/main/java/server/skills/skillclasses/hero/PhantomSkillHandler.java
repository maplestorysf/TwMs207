/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.hero;

import client.MapleCharacter;
import client.MapleJob;
import client.skill.Skill;
import client.skill.SkillFactory;
import client.forceatoms.ForceAtom;
import client.forceatoms.ForceAtomType;
import handling.channel.handler.AttackInfo;
import java.util.ArrayList;
import java.util.List;
import tools.AttackPair;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.MapleMonster;
import server.skills.AbstractSkillHandler;
import tools.packet.CField;
import tools.packet.JobPacket;
import tools.packet.JobPacket.PhantomPacket;

/**
 *
 * @author Weber
 */
public class PhantomSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {

        handleCardAttack(player, attackPairs);
        if (skill != null) {

        }
    }

    private void handleCardAttack(MapleCharacter player, List<AttackPair> attackPairs) {
        List<Integer> noirObjectIds = new ArrayList<>();
        List<ForceAtom> noirForce = new ArrayList<>();
        List<Integer> blancObjectIds = new ArrayList<>();
        List<ForceAtom> blancForce = new ArrayList<>();
        Skill noir = SkillFactory.getSkill(24120002);
        Skill blanc = SkillFactory.getSkill(24100003);
        // 只有爆擊出現
        attackPairs.forEach(attackPair -> {
            boolean hasCritical = Randomizer.isSuccess(player.getStat().getCritRate());
            // attackPair.attack.stream().filter(damage -> damage.right).count() > 0;
            if (hasCritical) {
                if (player.getSkillLevel(noir) > 0) {
                    MapleStatEffect effect = noir.getEffect(player.getSkillLevel(noir));
                    if (effect.makeChanceResult()) {
                        noirObjectIds.add(attackPair.objectid);
                        noirForce.add(player.getNewAtom(noir.getId(), false));
                        incCardStack(player);
                    }
                }
                if (player.getSkillLevel(blanc) > 0) {
                    MapleStatEffect effect = noir.getEffect(player.getSkillLevel(blanc));
                    if (effect.makeChanceResult()) {
                        blancObjectIds.add(attackPair.objectid);
                        blancForce.add(player.getNewAtom(blanc.getId(), false));
                        incCardStack(player);
                    }
                }
            }
        });
        if (!noirObjectIds.isEmpty()) {
            player.getClient().announce(CField.OnCreateForceAtom(false, player, noirObjectIds,
                    ForceAtomType.PhantomCard.getValue(), noir.getId(), noirForce, null, null, 0, 0, 0));
        }
        if (!blancObjectIds.isEmpty()) {
            player.getClient().announce(CField.OnCreateForceAtom(false, player, blancObjectIds,
                    ForceAtomType.PhantomCard.getValue(), blanc.getId(), blancForce, null, null, 0, 0, 0));
        }
    }

    private void incCardStack(MapleCharacter player) {
        if (player.getCardStack() < player.getCardStackMax()) {
            player.setCardStack((byte) (player.getCardStack() + 1));
            player.getClient().announce(PhantomPacket.updateCardStack(player.getCardStack()));
        }
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
        return MapleJob.is幻影俠盜(player.getJob());
    }

    @Override
    public void onDamage(MapleCharacter player, MapleMonster monster) {

    }
}
