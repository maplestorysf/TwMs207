/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skills;

import client.MapleCharacter;
import client.skill.Skill;

/**
 *
 * @author Weber
 */
public abstract class AbstractSkillHandler implements ISkillHandler {

    public AbstractSkillHandler() {

    }

    public boolean isBufferdFromSkill(MapleCharacter player, Integer skillId) {
        return player.isBuffed(skillId);
    }

    public boolean hasLearnedSkill(MapleCharacter player, Integer skillId) {
        return player.getTotalSkillLevel(skillId) > 0;
    }

    public boolean hasLearnedSkill(MapleCharacter player, Skill skill) {
        return player.getTotalSkillLevel(skill) > 0;
    }
}
