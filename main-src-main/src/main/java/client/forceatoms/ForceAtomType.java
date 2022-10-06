/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.forceatoms;

/**
 *
 * @author Weber
 */
public enum ForceAtomType {
    DemonKillerDran(0), PhantomCard(1), DrainSoul(3), WindFairyArrow(7), WindStormBringer(8), ShadowBat(
            15), ShadowBatFromMob(16),;
    private int value;

    private ForceAtomType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
