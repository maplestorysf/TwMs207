/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.life;

import java.util.HashMap;
import java.util.Map;
import server.Randomizer;

/**
 *
 * @author pungin
 */
public class EliteMonsterInfo {

    private int eliteType;
    private final Map<EliteAttribute, Integer> eliteAttrs = new HashMap<>();

    public EliteMonsterInfo() {
        this.eliteType = Randomizer.nextInt(3);
        this.addEliteAttribute(EliteAttribute.getRandomAttribute(), 100);
    }

    public EliteMonsterInfo(int eliteType) {
        this.eliteType = eliteType;
        this.addEliteAttribute(EliteAttribute.getRandomAttribute(), 100);
    }

    public EliteMonsterInfo(int eliteType, Map<EliteAttribute, Integer> eliteAttrs) {
        this.eliteType = eliteType;
        this.eliteAttrs.putAll(eliteAttrs);
    }

    public int getEliteType() {
        return eliteType;
    }

    public void setEliteType(int eliteType) {
        this.eliteType = eliteType;
    }

    public Map<EliteAttribute, Integer> getEliteAttribute() {
        return eliteAttrs;
    }

    public final void addEliteAttribute(EliteAttribute att, int unk) {
        eliteAttrs.put(att, unk);
    }

    public void removeEliteAttribute(EliteAttribute att) {
        eliteAttrs.remove(att);
    }

    public static enum EliteAttribute {

        堅固的(0x0), 抗魔的(0x1), 再生的(0x2), 快速的(0x3), 封印的(0x8), 迴避的(0x9), 虛弱的(0xA), 昏倒的(0xB), 詛咒的(0xC), 猛毒的(0xD), 黏黏的(
                0xE), 噬魔的(0xF), 魅惑的(0x10), 毒霧的(
                0x13), 混亂的(0x14), 不死的(0x15), 厭藥的(0x16), 不停的(0x17), 黑暗的(0x18), 堅固的2(0x1E), 反射的(0x21), 無敵的(0x22),;

        private int value;

        private EliteAttribute(int value) {
            this.value = value;
        }

        public int getValue() {
            return 0x70 + value;
        }

        public static EliteAttribute getRandomAttribute() {
            return EliteAttribute.values()[Randomizer.nextInt(EliteAttribute.values().length)];
        }
    }
}
