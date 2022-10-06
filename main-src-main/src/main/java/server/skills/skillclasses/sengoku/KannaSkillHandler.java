/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills.skillclasses.sengoku;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.forceatoms.ForceAtom;
import client.inventory.Item;
import client.skill.Skill;
import client.skill.SkillFactory;
import extensions.temporary.UserEffectOpcode;
import handling.channel.handler.AttackInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.life.MapleMonster;
import server.maps.MapleAffectedArea;
import server.maps.MapleMapItem;
import server.skills.AbstractSkillHandler;
import tools.AttackPair;
import tools.packet.CField;

/**
 *
 * @author Weber
 */
public class KannaSkillHandler extends AbstractSkillHandler {

    @Override
    public void onAttack(MapleCharacter player, AttackInfo attackInfo, List<AttackPair> attackPairs, Skill skill) {
        List<Point> imperialBodyDestoryList = new ArrayList<>();
        attackPairs.forEach(attackPair -> {
            MapleMonster monster = player.getMap().getMonsterByOid(attackPair.objectid);
            if (monster == null) {
                return;
            }
            MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(skill));
            // 妖雲召喚
            if (attackInfo.skill == 42101005) {
                if (effect != null) {
                    final Rectangle bounds = MapleStatEffect.calculateBoundingBox(monster.getPosition(),
                            monster.isFacingLeft(), effect.lt2, effect.rb2, effect.info.get(MapleStatInfo.range));
                    final MapleAffectedArea mist = new MapleAffectedArea(bounds, player, effect);
                    mist.setPosition(monster.getPosition());
                    player.getMap().spawnAffectedArea(mist, effect.getDuration(), false);
                }
            }
            // 紅玉咒印
            if (player.getTotalSkillLevel(42111002) > 0) {
                Skill _skill = SkillFactory.getSkill(42111002);
                MapleStatEffect _effect = _skill.getEffect(player.getTotalSkillLevel(_skill));
                if (_effect.makeChanceResult()) {
                    Item soulShearItem = new Item(4033270, (byte) 0, (short) 1);
                    final MapleMapItem mdrop = new MapleMapItem(soulShearItem, monster.getPosition(), monster, player,
                            (byte) 0, true);
                    mdrop.registerExpire(50000);
                    player.getMap().spawnAndAddRangedMapObject(mdrop, (MapleClient c) -> {
                        if (monster.getPosition() != null) {
                            c.announce(CField.dropItemFromMapObject(mdrop, monster.getTruePosition(),
                                    monster.getPosition(), (byte) 1));
                        }
                    });
                }
            }
            // 吸生纏氣回血處理
            if (player.getTotalSkillLevel(42110008) > 0) {
                Skill _skill = SkillFactory.getSkill(42110008);
                if (_skill != null) {
                    MapleStatEffect _effect = _skill.getEffect(player.getTotalSkillLevel(42110008));
                    if (_effect != null) {
                        int healHp = 0;
                        int maxHp = player.getStat().getCurrentMaxHp();
                        if (!monster.isAlive()) {
                            healHp = (int) Math.ceil(maxHp * _effect.getX() / 100.0);
                        } else if (monster.getStats().isBoss()) {
                            healHp = (int) Math.ceil(maxHp * 1 / 100.0);
                        }
                        if (healHp > 0) {
                            player.healHP(healHp);
                            player.getClient().announce(CField.EffectPacket.showEffect(true, player,
                                    UserEffectOpcode.UserEffect_SkillAffected,
                                    new int[]{42110008, player.getTotalSkillLevel(42110008)}, null, null, null));
                            player.getMap().broadcastMessage(player, CField.EffectPacket.showEffect(false, player,
                                    UserEffectOpcode.UserEffect_SkillAffected,
                                    new int[]{42110008, player.getTotalSkillLevel(42110008)}, null, null, null),
                                    false);
                        }
                    }
                }
            }
            // 御身消滅
            if (player.getTotalSkillLevel(42100007) > 0 && !monster.isAlive()) {
                imperialBodyDestoryList.add(monster.getPosition());
            }

        });
        if (!imperialBodyDestoryList.isEmpty()) {
            player.getClient().announce(CField.sendImperialBodyDestory(imperialBodyDestoryList));
        }
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
        return MapleJob.is陰陽師(player.getJob());
    }

}
