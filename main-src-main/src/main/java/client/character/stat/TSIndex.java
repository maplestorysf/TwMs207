/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.character.stat;

import handling.BuffStat;

public enum TSIndex {
    EnergyCharged(0),
    DashSpeed(1),
    DashJump(2),
    RideVehicle(3),
    PartyBooster(4),
    GuidedBullet(5),
    Undead(6),
    Undead2(7),
    RideVehicleExpire(8);
    private final int index;

    private TSIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static MapleBuffStat get_CTS_from_TSIndex(int nIdx) {
        switch (nIdx) {
            case 0:
                return MapleBuffStat.EnergyCharged;
            case 1:
                return MapleBuffStat.DashSpeed;
            case 2:
                return MapleBuffStat.DashJump;
            case 3:
                return MapleBuffStat.RideVehicle;
            case 4:
                return MapleBuffStat.PartyBooster;
            case 5:
                return MapleBuffStat.GuidedBullet;
            case 6:
                return MapleBuffStat.Undead;
            case 7:
                return MapleBuffStat.Undead;
            case 8:
                return MapleBuffStat.RideVehicleExpire;
            default: {
                return null;
            }
        }
    }

    public static int get_TSIndex_from_CTS(BuffStat uFlag) {
        for (int i = 0; i < TSIndex.values().length; i++) {
            if (get_CTS_from_TSIndex(i) == uFlag) {
                return i;
            }
        }
        return -1;
    }

    public static boolean is_valid_TSIndex(BuffStat uFlag) {
        for (int i = 0; i < TSIndex.values().length; i++) {
            if (get_CTS_from_TSIndex(i) == uFlag) {
                return true;
            }
        }
        return false;
    }
}
