package client.inventory;

import constants.GameConstants;

public class ModifyInventory {

    public static class Types {

        public static final int ADD = 0;
        public static final int UPDATE = 1;
        public static final int MOVE = 2;
        public static final int REMOVE = 3;
        public static final int MOVE_TO_BAG = 5;
        public static final int UPDATE_IN_BAG = 6;
        public static final int REMOVE_IN_BAG = 7;
        public static final int MOVE_IN_BAG = 8;
        public static final int ADD_IN_BAG = 9;
    }

    private final int mode;
    private Item item;
    private final byte inventoryType;
    private final short pos;
    private final Short newPos;
    private final short quantity;

    public ModifyInventory(final int mode, final Item item) {
        this.mode = mode;
        this.item = item.copy();
        newPos = null;
        pos = this.item.getPosition();
        quantity = this.item.getQuantity();
        inventoryType = GameConstants.getInventoryType(item.getItemId()).getType();
    }

    public ModifyInventory(final int mode, final Item item, final short oldPos) {
        this.mode = mode;
        this.item = item.copy();
        newPos = this.item.getPosition();
        this.item.setPosition(oldPos);
        if (item.getPet() != null) {
            item.getPet().setInventoryPosition(newPos);
        }
        pos = this.item.getPosition();
        quantity = this.item.getQuantity();
        inventoryType = GameConstants.getInventoryType(item.getItemId()).getType();
    }

    public ModifyInventory(final int mode, final Item item, final short pos, final Short newPos, final short quantity,
            final byte inventoryType) {
        this.mode = mode;
        this.item = item == null ? null : item.copy();
        if (this.item != null) {
            this.item.setPosition(pos);
        }
        this.pos = pos;
        this.newPos = newPos;
        this.quantity = quantity;
        this.inventoryType = inventoryType;
    }

    public final int getMode() {
        return mode;
    }

    public final int getInventoryType() {
        return inventoryType == -1 ? 1 : inventoryType;
    }

    public final short getPosition() {
        return pos;
    }

    public final short getNewPosition() {
        return newPos == null ? 0 : newPos;
    }

    public final short getQuantity() {
        return quantity;
    }

    public final Item getItem() {
        return item;
    }

    public final void clear() {
        item = null;
    }
}
