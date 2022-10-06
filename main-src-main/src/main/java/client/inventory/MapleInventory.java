package client.inventory;

import constants.GameConstants;
import constants.ItemConstants;
import java.io.Serializable;
import java.util.*;

public class MapleInventory implements Iterable<Item>, Serializable {

    private final Map<Short, Item> inventory;
    private short slotLimit = 0;
    private final MapleInventoryType type;

    public MapleInventory(MapleInventoryType type) {
        this.inventory = new TreeMap<>(Comparator.naturalOrder());
        this.type = type;
    }

    public void addSlot(short slot) {
        setSlotLimit((short) (slotLimit + slot));
    }

    public short getSlotLimit() {
        if (type == MapleInventoryType.CASH) {
            slotLimit = GameConstants.InventorySlotMax;
        } else {
            if (slotLimit > GameConstants.InventorySlotMax) {
                slotLimit = GameConstants.InventorySlotMax;
            }
            if (slotLimit < 0) {
                slotLimit = 0;
            }
        }
        return slotLimit;
    }

    public void setSlotLimit(short slot) {
        if (type == MapleInventoryType.CASH) {
            slotLimit = GameConstants.InventorySlotMax;
        } else {
            slotLimit = (short) Math.min(slot, GameConstants.InventorySlotMax);
        }
    }

    public Item findById(int itemId, int position) {
        for (Item item : this.inventory.values()) {
            if (item.getItemId() == itemId && item.getPosition() == position) {
                return item;
            }
        }
        return null;
    }

    public Item findById(int itemId) {
        for (Item item : this.inventory.values()) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public Item findByUniqueId(int uniqueId) {
        for (Item item : this.inventory.values()) {
            if (item.getUniqueId() == uniqueId) {
                return item;
            }
        }
        return null;
    }

    public Item findByInventoryId(long itemId, int itemI) {
        for (Item item : this.inventory.values()) {
            if ((item.getInventoryId() == itemId) && (item.getItemId() == itemI)) {
                return item;
            }
        }
        return findById(itemI);
    }

    public Item findByInventoryIdOnly(long itemId, int itemI) {
        for (Item item : this.inventory.values()) {
            if ((item.getInventoryId() == itemId) && (item.getItemId() == itemI)) {
                return item;
            }
        }
        return null;
    }

    public int countById(int itemId) {
        int possesed = 0;
        for (Item item : this.inventory.values()) {
            if (item.getItemId() == itemId) {
                possesed += item.getQuantity();
            }
        }
        return possesed;
    }

    public List<Item> listById(int itemId) {
        List<Item> ret = new ArrayList<>();
        for (Item item : this.inventory.values()) {
            if (item.getItemId() == itemId) {
                ret.add(item);
            }

        }

        if (ret.size() > 1) {
            Collections.sort(ret);
        }
        return ret;
    }

    public Collection<Item> list() {
        return this.inventory.values();
    }

    public List<Item> newList() {
        if (this.inventory.size() <= 0) {
            return Collections.emptyList();
        }
        return new LinkedList<>(this.inventory.values());
    }

    public List<Integer> listIds() {
        List<Integer> ret = new ArrayList<>();
        for (Item item : this.inventory.values()) {
            if (!ret.contains(item.getItemId())) {
                ret.add(item.getItemId());
            }
        }
        if (ret.size() > 1) {
            Collections.sort(ret);
        }
        return ret;
    }

    public short addItem(Item item) {
        short slotId = getNextFreeSlot();
        if (slotId < 0) {
            return -1;
        }
        this.inventory.put(slotId, item);
        item.setPosition(slotId);
        return slotId;
    }

    public short addItem_shied(Item item) {
        short slotId = 97;
        if (this.inventory.containsKey(97)) {
            return -1;
        }

        this.inventory.put(slotId, item);
        item.setPosition(slotId);
        return slotId;
    }

    public void addFromDB(Item item) {
        if ((item.getPosition() < 0) && (!this.type.equals(MapleInventoryType.EQUIPPED))) {
            return;
        }
        if ((item.getPosition() > 0) && (this.type.equals(MapleInventoryType.EQUIPPED))) {
            return;
        }
        this.inventory.put(item.getPosition(), item);
    }

    public void move(short sSlot, short dSlot, short slotMax) {
        Item source = (Item) this.inventory.get(Short.valueOf(sSlot));
        Item target = (Item) this.inventory.get(Short.valueOf(dSlot));
        if (source == null) {
            throw new InventoryException("Trying to move empty slot");
        }
        if (target == null) {
            if ((dSlot < 0) && (!this.type.equals(MapleInventoryType.EQUIPPED))) {
                return;
            }
            if ((dSlot > 0) && (this.type.equals(MapleInventoryType.EQUIPPED))) {
                return;
            }
            source.setPosition(dSlot);
            this.inventory.put(dSlot, source);
            this.inventory.remove(sSlot);
        } else if ((target.getItemId() == source.getItemId()) && (!ItemConstants.類型.可充值道具(source.getItemId()))
                && (target.getOwner().equals(source.getOwner()))
                && (target.getExpiration() == source.getExpiration())) {
            if ((this.type.getType() == MapleInventoryType.EQUIP.getType())
                    || (this.type.getType() == MapleInventoryType.CASH.getType())) {
                swap(target, source);
            } else if (source.getQuantity() + target.getQuantity() > slotMax) {
                source.setQuantity((short) (source.getQuantity() + target.getQuantity() - slotMax));
                target.setQuantity(slotMax);
            } else {
                target.setQuantity((short) (source.getQuantity() + target.getQuantity()));
                this.inventory.remove(sSlot);
            }
        } else {
            swap(target, source);
        }
    }

    private void swap(Item source, Item target) {
        this.inventory.remove(source.getPosition());
        this.inventory.remove(target.getPosition());
        short swapPos = source.getPosition();
        source.setPosition(target.getPosition());
        target.setPosition(swapPos);
        this.inventory.put(source.getPosition(), source);
        this.inventory.put(target.getPosition(), target);
    }

    public Item getItem(short slot) {
        return (Item) this.inventory.get(slot);
    }

    public void removeItem(short slot) {
        removeItem(slot, (short) 1, false);
    }

    public void removeItem(short slot, short quantity, boolean allowZero) {
        Item item = (Item) this.inventory.get(slot);
        if (item == null) {
            return;
        }
        item.setQuantity((short) (item.getQuantity() - quantity));
        if (item.getQuantity() < 0) {
            item.setQuantity((short) 0);
        }
        if ((item.getQuantity() == 0) && (!allowZero)) {
            removeSlot(slot);
        }
    }

    public void removeSlot(short slot) {
        this.inventory.remove(slot);
    }

    public boolean isFull() {
        return this.inventory.size() >= getSlotLimit();
    }

    public boolean isFull(int margin) {
        return this.inventory.size() + margin >= getSlotLimit();
    }

    public short getNextFreeSlot() {
        if (isFull()) {
            return -1;
        }
        for (short i = 1; i <= getSlotLimit(); i = (short) (i + 1)) {
            if (!this.inventory.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    public short getNumFreeSlot() {
        if (isFull()) {
            return 0;
        }
        short free = 0;
        for (short i = 1; i <= getSlotLimit(); i = (short) (i + 1)) {
            if (!this.inventory.containsKey(i)) {
                free = (short) (free + 1);
            }
        }
        return free;
    }

    public MapleInventoryType getType() {
        return this.type;
    }

    @Override
    public Iterator<Item> iterator() {
        return Collections.unmodifiableCollection(this.inventory.values()).iterator();
    }
}
