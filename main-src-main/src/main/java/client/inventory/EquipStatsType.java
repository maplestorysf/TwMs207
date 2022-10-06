/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.inventory;

/**
 *
 * @author Pungin
 */
public enum EquipStatsType {

    STAR_FORCE(0x1), FLAME(0x2),;
    private final int value;

    private EquipStatsType(int value) {
        this.value = value;
    }

    public final int getValue() {
        return value;
    }

    public final boolean check(int flag) {
        return (flag & value) != 0;
    }
}
