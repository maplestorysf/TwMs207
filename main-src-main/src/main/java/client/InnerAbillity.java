package client;

import client.skill.Skill;
import client.skill.SkillFactory;
import constants.GameConstants;
import server.Randomizer;

public class InnerAbillity {

    private static InnerAbillity instance = null;

    public static InnerAbillity getInstance() {
        if (instance == null) {
            instance = new InnerAbillity();
        }
        return instance;
    }

    public InnerSkillValueHolder renewSkill(int rank, int position) {
        int randomSkill = GameConstants.getInnerSkillbyRank(
                rank)[(int) Math.floor(Math.random() * GameConstants.getInnerSkillbyRank(rank).length)];
        Skill skill = SkillFactory.getSkill(randomSkill);
        if (skill == null) {
            return null;
        }

        int random = Randomizer.nextInt(100);
        int skillLevel;
        if (random < 10) {
            skillLevel = Randomizer.rand(skill.getMaxLevel() / 2, skill.getMaxLevel());
        } else {
            if (random < 20) {
                skillLevel = Randomizer.rand(skill.getMaxLevel() / 3, skill.getMaxLevel() / 2);
            } else {
                skillLevel = Randomizer.rand(skill.getMaxLevel() / 4, skill.getMaxLevel() / 3);
            }
        }
        if (skillLevel > skill.getMaxLevel()) {
            skillLevel = skill.getMaxLevel();
        }
        return new InnerSkillValueHolder(randomSkill, skillLevel, (byte) position, (byte) rank);
    }

    public int getCirculatorRank(int circulator) {
        return ((circulator % 1000) / 100) + 1;
    }
}
