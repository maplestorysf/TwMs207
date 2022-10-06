/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.familiar;

/**
 *
 * @author Weber
 */
public class FamiliarCard {

    private byte grade;
    private short skill;
    private int option1;
    private int option2;
    private int option3;

    private byte level;

    public FamiliarCard(byte grade) {
        this.grade = grade;
        this.level = 1;
    }

    public FamiliarCard() {

    }

    public FamiliarCard(short skill, byte level, byte grade, int option1, int option2, int option3) {
        super();
        this.skill = skill;
        this.level = (byte) Math.max(1, (int) level);
        this.grade = grade;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
    }

    public void copy(FamiliarCard card) {
        this.skill = card.getSkill();
        this.level = card.getLevel();
        this.grade = card.getGrade();
        this.option1 = card.getOption1();
        this.option2 = card.getOption2();
        this.option3 = card.getOption3();
    }

    public byte getGrade() {
        return this.grade;
    }

    public short getSkill() {
        return this.skill;
    }

    public byte getLevel() {
        return this.level;
    }

    public int getOption(int index) {
        switch (index) {
            case 2:
                return this.option3;
            case 1:
                return this.option2;
            case 0:
                return this.option1;
            default:
                return 0;
        }
    }

    public int getOption1() {
        return this.option1;
    }

    public int getOption2() {
        return this.option2;
    }

    public int getOption3() {
        return this.option3;
    }
}
