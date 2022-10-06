/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extensions.temporary;

/**
 *
 * @author o黯淡o
 */
public class BossLists {

    public enum BossListType {
        FindPart(2), Waiting(5), Join(7), Exit(8),;

        private final int value;

        private BossListType(int value) {
            this.value = value;
        }

        public static BossListType getType(int type) {
            for (BossListType bl : values()) {
                if (bl.getValue() == type) {
                    return bl;
                }
            }
            return null;
        }

        public int getValue() {
            return value;
        }
    }

    public enum BossList {
        濃姬_N(103, 811000999, 800011126, 140, 255, 58913), // 還有任務58955
        巴洛古_N(1, 105100100, 80001535, 65, 100, 0), 炎魔_E(2, 211042300, 80001536, 50, 255, 0), 炎魔_N(3, 211042300,
                80001536, 90, 255,
                0), 炎魔_C(4, 211042300, 80001536, 90, 255, 0), 闇黑龍王_E(5, 240040700, 80001537, 130, 255, 0), 闇黑龍王_N(6,
                240040700, 80001537, 130, 255, 0), 闇黑龍王_C(27, 240040700, 80001537, 135, 255, 0), 希拉_N(7,
                262030000, 80001538, 120, 255, 0), 希拉_H(8, 262030000, 80001538, 170, 255, 0), 比艾樂_N(9,
                105200000, 80001544, 125, 255,
                30007), 比艾樂_C(10, 105200000, 80001544, 180, 255, 30007), 斑斑_N(11, 105200000,
                80001545, 125, 255,
                30007), 斑斑_C(12, 105200000, 80001545, 180, 255, 30007), 血腥皇后_N(13,
                105200000, 80001546, 125, 255,
                30007), 血腥皇后_C(14, 105200000, 80001546, 180, 255, 30007), 貝倫_N(
                15, 105200000, 80001547, 125, 255, 30007), 貝倫_C(16,
                105200000, 80001547, 180, 255, 30007), 凡雷恩_E(17,
                211070000, 80001539, 125, 255,
                3170), 凡雷恩_N(18, 211070000, 80001539,
                125, 255, 3170), 阿卡伊農_E(19,
                272000000, 80001540,
                140, 255, 0), 阿卡伊農_N(20,
                272000000,
                80001540, 140,
                255,
                0), 梅格耐斯_E(21,
                401060399,
                80001541,
                115,
                255,
                31851), 梅格耐斯_N(
                22,
                401060000,
                80001541,
                155,
                255,
                31833), 梅格耐斯_H(
                23,
                401060000,
                80001541,
                175,
                255,
                31833), 皮卡啾_N(
                24,
                270040000,
                80001542,
                160,
                255,
                3521), 皮卡啾_C(
                25,
                270040000,
                80001542,
                170,
                255,
                3521), 西格諾斯_N(
                26,
                271030600,
                80001543,
                170,
                255,
                31152), 史烏_N(
                28,
                350060300,
                0,
                190,
                255,
                33294), 史烏_H(
                29,
                350060300,
                0,
                190,
                255,
                33294), 烏勒斯_N(
                30,
                970072200,
                0,
                100,
                255,
                0), 森蘭丸_N(
                101,
                807300100,
                800011125,
                120,
                255,
                0), 森蘭丸_C(
                102,
                807300200,
                800011125,
                180,
                255,
                0), 庫洛斯_E(
                110,
                806300000,
                0,
                160,
                255,
                58636), 庫洛斯_N(
                111,
                806300000,
                0,
                180,
                255,
                58636),;
        private final int value, mapid, skillId, minLevel, maxLevel, questId;

        private BossList(int value, int mapid, int skillId, int minLevel, int maxLevel, int questId) {
            this.value = value;
            this.mapid = mapid;
            this.skillId = skillId;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.questId = questId;
        }

        public static BossList getType(int type) {
            for (BossList bl : values()) {
                if (bl.getValue() == type) {
                    return bl;
                }
            }
            return null;
        }

        public int getValue() {
            return value;
        }

        public int getMapId() {
            return mapid;
        }

        public int getSkillId() {
            return skillId;
        }

        public int getMinLevel() {
            return minLevel;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public int getQuestId() {
            return questId;
        }
    }
}
