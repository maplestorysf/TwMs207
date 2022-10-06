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
public class DemonAvangerSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPair, Skill skill) {
        final Skill drainLife = SkillFactory.getSkill(31010002);
        final Skill advDrainLife = SkillFactory.getSkill(31210006);
        final Skill drainLifeExplosion = SkillFactory.getSkill(31211001);
        final Skill exceedPain = SkillFactory.getSkill(31210005);

        double recoverValue = 0;
        if (player.getTotalSkillLevel(drainLife) > 0) {// 生命吸收
            MapleStatEffect eff = drainLife.getEffect(player.getTotalSkillLevel(drainLife));
            if (eff.makeChanceResult()) {
                if (player.getTotalSkillLevel(advDrainLife) > 0) {// 進階生命吸收
                    MapleStatEffect eff_ = advDrainLife.getEffect(player.getTotalSkillLevel(31210006));
                    recoverValue = eff_.getX();
                } else {
                    recoverValue = eff.getX();
                }
            }
        }
        if (skill != null && player.getSkillLevel(skill) > 0) {
            MapleStatEffect eff = skill.getEffect(player.getTotalSkillLevel(skill));
            // 噬魂爆發
            if (skill == drainLifeExplosion) {
                recoverValue += eff.getY();
            }

        }
        if (recoverValue > 0) {
            double heal = player.getStat().getCurrentMaxHp() * recoverValue / 100;
            heal *= (100 - player.getExceedOverload() / 2
                    * (1 + (player.getTotalSkillLevel(exceedPain)/* 超越苦痛 */ > 0 ? 2 : 1))) / 100.0;
            if (heal > 0) {
                player.addHP((int) Math.round(heal));
            }
        }
    }

    @Override
    public void onSpecialSkill(MapleCharacter player, List<Integer> listObjIds, Skill skill) {
        List<ForceAtom> forceAtoms = new ArrayList<>();
        final Skill shieldAttack = SkillFactory.getSkill(31221001);
        if (skill != null && player.getSkillLevel(skill) > 0) {
            if (skill.getId() == shieldAttack.getId() && player.getSkillLevel(skill) > 0) {
                MapleStatEffect effect = skill.getEffect(player.getSkillLevel(skill));
                int count = effect.getMobCount();
                for (int i = 0; i < count; i++) {
                    forceAtoms.add(player.getNewAtom(skill.getId(), false));
                }
            }
        }

        if (!forceAtoms.isEmpty()) {
            player.getMap().broadcastMessage(player, CField.OnCreateForceAtom(false, player, listObjIds,
                    ForceAtomType.DrainSoul.getValue(), skill.getId(), forceAtoms, null, null, 0, 0, 0), true);
        }
    }

    @Override
    public void onForceAtomCollision(MapleCharacter player, Skill skill, List<ForceAtom> forceAtoms,
            List<Integer> objectIds) {
        List<ForceAtom> newForceAtoms = new ArrayList<>();
        if (skill.getId() == 31221001 && player.getSkillLevel(skill) > 0) {
            MapleStatEffect effect = skill.getEffect(player.getSkillLevel(skill));
            if (effect.makeChanceResult()) {
                forceAtoms.forEach(forceAtom -> {
                    int nextNum = forceAtom.getNum() + 1;
                    if (nextNum < effect.getZ()) {
                        ForceAtom nAtom = player.getNewAtom(skill.getId(), true);
                        nAtom.setNum(nextNum);
                        newForceAtoms.add(nAtom);
                    }
                });
                forceAtoms.forEach(forceAtom -> player.removeForceAtom(forceAtom));
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
        return MapleJob.is惡魔復仇者(player.getJob());
    }

    @Override
    public void onDamage(MapleCharacter player, MapleMonster monster) {

    }

}
