package client.character.stat;

import java.io.Serializable;
import server.Randomizer;
import handling.BuffStat;

public enum MapleDisease implements Serializable, BuffStat {

    封印(MapleBuffStat.Seal, 120), 黑暗(MapleBuffStat.Darkness, 121), 虛弱(MapleBuffStat.Weakness, 122), 昏迷(
            MapleBuffStat.Stun,
            123), 詛咒(MapleBuffStat.Curse, 124), 中毒(MapleBuffStat.Poison, 125), 緩慢(MapleBuffStat.HiddenPieceOn,
            126), 誘惑(MapleBuffStat.Attract, 128), 混亂(MapleBuffStat.ReverseInput, 132), 不死化(MapleBuffStat.BanMap,
            133), 無法使用藥水(MapleBuffStat.StopPortion, 134), 影子(MapleBuffStat.StopMotion, 135), // receiving
    // damage/moving
    致盲(MapleBuffStat.Blind, 136), 冰凍(MapleBuffStat.Frozen, 137), 裝備潛能無效化(MapleBuffStat.DispelItemOption,
            138), 變身(MapleBuffStat.Morph, 172), 龍捲風(MapleBuffStat.DarkTornado, 173), 旗幟(MapleBuffStat.IncMaxMP, 799); // PVP
    // -
    // Capture
    // the
    // Flag
    // 127 = 1 snow?
    // 129 = turn?
    // 131 = poison also, without msg
    // 133, become undead?..50% recovery?
    // 0x100 is disable skill except buff
    private static final long serialVersionUID = 0L;
    private final int buffstat;
    private final int first;
    private final int disease;

    private MapleDisease(MapleBuffStat buffstat, int disease) {
        this.buffstat = buffstat.getValue();
        this.first = buffstat.getPosition();
        this.disease = disease;
    }

    @Override
    public int getPosition() {
        return first;
    }

    @Override
    public int getValue() {
        return buffstat;
    }

    public int getDisease() {
        return disease;
    }

    public MapleBuffStat getStat() {
        return MapleBuffStat.getMapleBuffStat(buffstat);
    }

    public static MapleDisease getRandom() {
        while (true) {
            for (MapleDisease dis : MapleDisease.values()) {
                if (Randomizer.nextInt(MapleDisease.values().length) == 0) {
                    return dis;
                }
            }
        }
    }

    public static MapleDisease getBySkill(final int skill) {
        for (MapleDisease d : MapleDisease.values()) {
            if (d.getDisease() == skill) {
                return d;
            }
        }
        return null;
    }
}
