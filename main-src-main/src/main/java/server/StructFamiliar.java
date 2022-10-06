package server;

public class StructFamiliar {

    private int grade;
    private int skillId;
    private int effectAfter;
    private int mobId;
    private int monsterCardId;

    public StructFamiliar(int grade, int skillId, int effectAfter, int mobId, int monsterCardId) {
        this.grade = grade;
        this.skillId = skillId;
        this.effectAfter = effectAfter;
        this.mobId = mobId;
        this.monsterCardId = monsterCardId;
    }

    public int getGrade() {
        return grade;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getEffectAfter() {
        return effectAfter;
    }

    public int getMobId() {
        return mobId;
    }

    public int getMonsterCardId() {
        return monsterCardId;
    }

}
