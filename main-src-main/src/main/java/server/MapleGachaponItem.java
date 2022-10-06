/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author pungin
 */
public class MapleGachaponItem {

    private final int itemId;
    private final int quantity;
    private int remainingQuantity;
    private final int minQuantity;
    private final int maxQuantity;
    private final int chance;
    private final int smegaType;
    private final int gachaponType;

    public MapleGachaponItem(int itemId, int quantity, int remainingQuantity, int minQuantity, int maxQuantity,
            int chance, int smegaType, int gachaponType) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.chance = chance;
        this.smegaType = smegaType;
        this.gachaponType = gachaponType;
    }

    public int getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(int remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public int getMinQuantity() {
        return minQuantity;
    }

    public int getMaxQuantity() {
        return maxQuantity;
    }

    public int getChance() {
        return chance;
    }

    public int getSmegaType() {
        return smegaType;
    }

    public int getGachaponType() {
        return gachaponType;
    }
}
