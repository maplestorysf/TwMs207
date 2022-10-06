/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

/**
 *
 * @author o黯淡o
 */
public enum MapleZeroStat {
    bIsBeta(0x1), nSubHP(0x2), nSubMP(0x4), nSubSkin(0x8), nSubHair(0x10), nSubFace(0x20), nSubMHP(0x40), nSubMMP(
            0x80), dbcharZeroLinkCashPart(0x100), nMixBaseHair(0x200);
    private final int i;

    private MapleZeroStat(int i) {
        this.i = i;
    }

    public int getValue() {
        return i;
    }

    public static MapleZeroStat getByValue(final int value) {
        for (final MapleZeroStat stat : MapleZeroStat.values()) {
            if (stat.i == value) {
                return stat;
            }
        }
        return null;
    }
}
