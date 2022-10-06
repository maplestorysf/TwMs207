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
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import server.Randomizer;
import server.maps.MapleSummon;
import server.maps.SummonMovementType;
import tools.AttackPair;
import tools.Rect;
import tools.packet.CField;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.skills.AbstractSkillHandler;

/**
 *
 * @author Weber
 */
public class NightWalkerSkillHandler extends AbstractSkillHandler {

    private List<Integer> shadowBatSkills = Arrays.asList(14001020, 14101020, 14111020, 14111022);

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {

        // 處理暗黑蝙蝠
        if (skill != null) {
            handleShadowBat(player, attackPairs, skill);
        }

    }

    private boolean canUseShadowBat(Skill skill) {
        return skill != null ? shadowBatSkills.contains(skill.getId()) : true;
    }

    private void handleShadowBat(MapleCharacter player, List<AttackPair> attackPairs, Skill skill) {
        final List<Integer> batTargets = new ArrayList<>();
        Skill batSkill = SkillFactory.getSkill(14001027);
        Skill batSummonSkill = SkillFactory.getSkill(14000027);
        Skill batAtomSkill = SkillFactory.getSkill(14001028);
        if (hasLearnedSkill(player, batSkill) && canUseShadowBat(skill)) {
            attackPairs.forEach(attackPair -> {
                boolean hasAttack = attackPair.attack.stream().filter(damage -> damage.left > 0).count() > 0;
                if (hasAttack) {
                    player.setAttackCombo((short) (player.getAttackCombo() + 1));
                    batTargets.add(attackPair.objectid);
                }
            });
            MapleStatEffect effect = batSkill.getEffect(player.getSkillLevel(batSkill));
            if (effect.makeChanceResult()) {
                MapleSummon bat = player.getSummonBySkill(batSkill.getId());
                if (bat != null) {
                    Rect rc = new Rect(bat.getPosition().x + 10, bat.getPosition().y + 10, bat.getPosition().x - 10,
                            bat.getPosition().y - 10);
                    player.removeSummon(bat);
                    player.getMap().removeSummon(bat.getObjectId());
                    final List<ForceAtom> forceinfo = Collections
                            .singletonList(player.getNewAtom(skill.getId(), false));
                    player.getClient().announce(CField.OnCreateForceAtom(false, player, batTargets,
                            ForceAtomType.ShadowBat.getValue(), 14000029, forceinfo, null, rc, 0, 0, 0)); // 自己看的
                    player.getMap().broadcastMessage(CField.OnCreateForceAtom(true, player, batTargets, 16,
                            batAtomSkill.getId(), forceinfo, null, rc, 0, 0, 0)); // 其他玩家看的(要再重新寫過，這個應該要寫在Recv後)
                }
            }
            if (player.getAttackCombo() % effect.getZ() == 0) {
                player.setAttackCombo((short) (player.getAttackCombo() - 3));
            }
            if (player.getBuffedValue(MapleBuffStat.NightWalkerBat) != null) {
                final MapleSummon tosummon = new MapleSummon(player, batSummonSkill.getId(), 1,
                        new Point(player.getTruePosition()), SummonMovementType.跟隨飛行隨機移動攻擊);
                player.getMap().spawnSummon(tosummon);
                player.addSummon(tosummon);
            }
        }
    }

    @Override
    public void onSpecialSkill(MapleCharacter player, List<Integer> listObjIds, Skill skill) {
    }

    @Override
    public void onForceAtomCollision(MapleCharacter player, Skill skill, List<ForceAtom> forceAtoms,
            List<Integer> objectIds) {
        List<ForceAtom> newForceAtoms = new ArrayList<>();
        Skill nightWalkerBat = SkillFactory.getSkill(14001027);
        Skill advSkill1 = SkillFactory.getSkill(14100027);
        Skill advSkill2 = SkillFactory.getSkill(14110029);
        if (skill.getId() == 14001027 && player.getSkillLevel(skill) > 0) {
            MapleStatEffect effect = skill.getEffect(player.getSkillLevel(nightWalkerBat));
            int maxNum = effect.getMobCount();
            if (player.getSkillLevel(advSkill1) > 0) {
                maxNum += advSkill1.getEffect(player.getSkillLevel(advSkill1)).getMobCount();
            }
            if (player.getSkillLevel(advSkill2) > 0) {
                maxNum += advSkill2.getEffect(player.getSkillLevel(advSkill2)).getMobCount();
            }
            final int maxCount = maxNum;
            if (effect.makeChanceResult()) {
                forceAtoms.forEach((ForceAtom forceAtom) -> {
                    int nextNum = forceAtom.getNum() + 1;
                    if (nextNum < maxCount) {
                        ForceAtom nAtom = player.getNewAtom(skill.getId(), true);
                        nAtom.setNum(nextNum);
                        newForceAtoms.add(nAtom);
                    }
                    player.removeForceAtom(forceAtom);
                });
            }
        }
        if (!newForceAtoms.isEmpty()) {
            player.getMap()
                    .broadcastGMMessage(player, CField.OnCreateForceAtom(true, player, objectIds,
                            ForceAtomType.DrainSoul.getValue(), skill.getId(), newForceAtoms, null, null, 0, 0, 0),
                            true);
        }
    }

    @Override
    public boolean checkJob(MapleCharacter player) {
        return MapleJob.is暗夜行者(player.getJob());
    }

    @Override
    public void onDamage(MapleCharacter player, MapleMonster monster) {

    }
}
