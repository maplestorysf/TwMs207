package server;

import client.character.stat.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.MapleQuestStatus;
import client.MapleTrait.MapleTraitType;
import client.PlayerStats;
import client.skill.Skill;
import client.skill.SkillEntry;
import client.skill.SkillFactory;
import client.familiar.FamiliarCard;
import client.inventory.*;
import constants.GameConstants;
import constants.ItemConstants;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.quest.MapleQuest;
import tools.Pair;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.CCashShop;

public class MapleInventoryManipulator {

    public static int DAY = 3;
    public static int HOUR = 2;
    public static int MINUTE = 1;
    public static int SECOND = 0;

    public static void addRing(MapleCharacter chr, int itemId, int ringId, int sn, String partner) {
        CashModItem csi = CashItemFactory.getInstance().getItem(sn);
        if (csi == null) {
            return;
        }
        Item ring = chr.getCashInventory().toItem(csi, ringId);
        if (ring == null || ring.getUniqueId() != ringId || ring.getUniqueId() <= 0 || ring.getItemId() != itemId) {
            return;
        }
        chr.getCashInventory().addToInventory(ring);
        chr.getClient().getSession().writeAndFlush(CCashShop.sendBoughtRings(ItemConstants.類型.戀人戒指(itemId), ring, sn,
                chr.getClient().getAccID(), partner));
    }

    public static boolean addbyItem(final MapleClient c, final Item item) {
        return addbyItem(c, item, false) >= 0;
    }

    public static short addbyItem(final MapleClient c, final Item item, final boolean fromcs) {
        final short newSlot = c.getPlayer().getInventory(GameConstants.getInventoryType(item.getItemId()))
                .addItem(item);
        if (newSlot == -1) {
            if (!fromcs) {
                c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
            }
            return newSlot;
        }
        if (GameConstants.isHarvesting(item.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }
        c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(item));
        c.getPlayer().havePartyQuest(item.getItemId());
        return newSlot;
    }

    public static int getUniqueId(int itemId, MaplePet pet) {
        int uniqueid = -1;
        if (ItemConstants.類型.寵物(itemId)) {
            if (pet != null) {
                uniqueid = pet.getUniqueId();
            } else {
                uniqueid = MapleInventoryIdentifier.getInstance();
            }
        } else if (GameConstants.getInventoryType(itemId) == MapleInventoryType.CASH
                || MapleItemInformationProvider.getInstance().isCash(itemId)) { // less work to do
            uniqueid = MapleInventoryIdentifier.getInstance(); // shouldnt be generated yet, so put it here
        }
        return uniqueid;
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String gmLog) {
        return addById(c, itemId, quantity, null, null, 0, MapleInventoryManipulator.DAY, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addById(c, itemId, quantity, owner, null, 0, MapleInventoryManipulator.DAY, gmLog);
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addId(c, itemId, quantity, owner, null, 0, MapleInventoryManipulator.DAY, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, String gmLog) {
        return addById(c, itemId, quantity, owner, pet, 0, MapleInventoryManipulator.DAY, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period,
            int periodMod, String gmLog) {
        return addId(c, itemId, quantity, owner, pet, period, periodMod, gmLog) >= 0;
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period,
            int periodMod, String gmLog) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false))
                || (!ii.itemExists(itemId))) {
            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
            c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
            return -1;
        }
        if (itemId >= 4031332 && itemId <= 4031341) {
            c.getSession().writeAndFlush(
                    CField.getGameMessage(8, "Hint: Use @event to exchange a certificate of straight wins."));
        }

        if (ItemConstants.類型.寵物(itemId) && pet == null) {
            if (c.getPlayer().isShowErr()) {
                c.getPlayer().showInfo("增加道具", true, "出錯, 道具是寵物, 可是沒有傳遞寵物實例。");
            }
            return -1;
        }

        if (period > 0) {
            if (periodMod >= MapleInventoryManipulator.SECOND) {
                period *= 1000;
            }
            if (periodMod >= MapleInventoryManipulator.MINUTE) {
                period *= 60;
            }
            if (periodMod >= MapleInventoryManipulator.HOUR) {
                period *= 60;
            }
            if (periodMod >= MapleInventoryManipulator.DAY) {
                period *= 24;
            }
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);
        int uniqueid = getUniqueId(itemId, pet);
        short newSlot = -1;
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!ItemConstants.類型.可充值道具(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null)
                                    && eItem.getExpiration() == -1) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                newSlot = eItem.getPosition();
                                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(eItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                Item nItem;
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, 0, uniqueid, (short) 0);
                        newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                            c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                            return -1;
                        }
                        if (gmLog != null) {
                            nItem.setGMLog(gmLog);
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        if (period > 0) {
                            nItem.setExpiration(System.currentTimeMillis() + period);
                        }
                        if (pet != null) {
                            nItem.setPet(pet);
                            pet.setInventoryPosition(newSlot);
                            c.getPlayer().addPet(pet);
                        }
                        c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(nItem));
                        if (ItemConstants.類型.可充值道具(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.getPlayer().havePartyQuest(itemId);
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return (byte) newSlot;
                    }
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, 0, uniqueid, (short) 0);
                newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                if (period > 0) {
                    nItem.setExpiration(System.currentTimeMillis() + period);
                }
                if (gmLog != null) {
                    nItem.setGMLog(gmLog);
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(nItem));
                c.getSession().writeAndFlush(CWvsContext.enableActions());
            }
        } else {
            if (quantity == 1) {
                final Item nEquip = ii.getEquipById(itemId, uniqueid);
                if (owner != null) {
                    nEquip.setOwner(owner);
                }
                if (gmLog != null) {
                    nEquip.setGMLog(gmLog);
                }
                if (period > 0) {
                    nEquip.setExpiration(System.currentTimeMillis() + period);
                }
                newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(nEquip));
                if (GameConstants.isHarvesting(itemId)) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        c.getPlayer().havePartyQuest(itemId);
        return (byte) newSlot;
    }

    public static Item addbyId_Gachapon(final MapleClient c, final int itemId, short quantity) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() == -1
                || c.getPlayer().getInventory(MapleInventoryType.USE).getNextFreeSlot() == -1
                || c.getPlayer().getInventory(MapleInventoryType.ETC).getNextFreeSlot() == -1
                || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNextFreeSlot() == -1) {
            return null;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false))
                || (!ii.itemExists(itemId))) {
            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
            c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
            return null;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemId);

        int uniqueid = getUniqueId(itemId, null);
        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(itemId);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);

            if (!ItemConstants.類型.可充值道具(itemId)) {
                Item nItem = null;
                boolean recieved = false;

                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            nItem = i.next();
                            short oldQ = nItem.getQuantity();

                            if (oldQ < slotMax) {
                                recieved = true;

                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                nItem.setQuantity(newQ);
                                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(nItem, false));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, 0, uniqueid, (short) 0);
                        final MaplePet pet;
                        if (ItemConstants.類型.寵物(itemId)) {
                            pet = MaplePet.createPet(itemId, uniqueid);
                            nItem.setExpiration(
                                    System.currentTimeMillis() + (ii.getLife(itemId) * 24 * 60 * 60 * 1000));
                        } else {
                            pet = null;
                        }
                        final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1 && recieved) {
                            return nItem;
                        } else if (newSlot == -1) {
                            return null;
                        }
                        recieved = true;
                        c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(nItem));
                        if (ItemConstants.類型.可充值道具(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (recieved && nItem != null) {
                    c.getPlayer().havePartyQuest(nItem.getItemId());
                    return nItem;
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(itemId, (byte) 0, quantity, 0);
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);

                if (newSlot == -1) {
                    return null;
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(nItem));
                c.getPlayer().havePartyQuest(nItem.getItemId());
                return nItem;
            }
        } else {
            if (quantity == 1) {
                final Item item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                if (uniqueid > -1) {
                    item.setUniqueId(uniqueid);
                }
                final short newSlot = c.getPlayer().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    return null;
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(item, true));
                c.getPlayer().havePartyQuest(item.getItemId());
                return item;
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return null;
    }

    public static boolean addFromDrop(final MapleClient c, Item item) {
        return addFromDrop(c, item, false, false);
    }

    public static boolean addFromDrop(final MapleClient c, Item item, final boolean show, final boolean isMonsterDrop) {
        return addFromDrop(c, item, show, isMonsterDrop, false);
    }

    public static boolean addFromDrop(final MapleClient c, Item item, final boolean show, final boolean isMonsterDrop, final boolean isPetPickup) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        if (c.getPlayer() == null || (ii.isPickupRestricted(item.getItemId()) && c.getPlayer().haveItem(item.getItemId(), 1, true, false)) || (!ii.itemExists(item.getItemId()))) {
            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
            c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
            return false;
        }
        final int before = c.getPlayer().itemQuantity(item.getItemId());
        short quantity = item.getQuantity();
        final MapleInventoryType type = GameConstants.getInventoryType(item.getItemId());

        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(item.getItemId());
            final List<Item> existing = c.getPlayer().getInventory(type).listById(item.getItemId());
            if (!ItemConstants.類型.可充值道具(item.getItemId())) {
                if (quantity <= 0) { // wth
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.showItemUnavailable());
                    return false;
                }
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            final Item eItem = i.next();
                            final short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner()) && item.getExpiration() == eItem.getExpiration()) {
                                final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(eItem, !isPetPickup));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    final short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    final Item nItem = new Item(item.getItemId(), (byte) 0, newQ, item.getFlag());
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());
                    nItem.setPet(item.getPet());
                    nItem.setGMLog(item.getGMLog());
                    if (item.getFamiliarCard() == null) {
                        item.setFamiliarCard(new FamiliarCard((byte) 0));
                    }
                    nItem.setFamiliarCard(item.getFamiliarCard());
                    short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                    if (newSlot == -1) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(nItem, !isPetPickup));
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                final Item nItem = new Item(item.getItemId(), (byte) 0, quantity, item.getFlag());
                nItem.setExpiration(item.getExpiration());
                nItem.setOwner(item.getOwner());
                nItem.setPet(item.getPet());
                nItem.setGMLog(item.getGMLog());
                nItem.setFamiliarCard(item.getFamiliarCard());
                final short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(nItem, !isPetPickup));
                c.getSession().writeAndFlush(CWvsContext.enableActions());
            }
        } else {
            if (quantity == 1) {
                final short newSlot = c.getPlayer().getInventory(type).addItem(item);

                if (newSlot == -1) {
                    c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                Equip eq = (Equip) item;
                if (isMonsterDrop && (eq.getState(false) > 0 || eq.getState(true) > 0)) {
                    c.getSession().writeAndFlush(CField.EffectPacket.showItemTopMsgEffect(item));
                }
                c.getSession().writeAndFlush(InventoryPacket.addInventorySlot(item, !isPetPickup));
                if (GameConstants.isHarvesting(item.getItemId())) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
            } else {
                throw new RuntimeException("Trying to create equip with non-one quantity");
            }
        }
        if (item.getQuantity() >= 50 && item.getItemId() == 2340000) {
            c.setMonitored(true);
        }
        // if (before == 0) {
        // switch (item.getItemId()) {
        // case AramiaFireWorks.KEG_ID:
        // c.getPlayer().dropMessage(5, "You have gained a Powder Keg.");
        // break;
        // case AramiaFireWorks.SUN_ID:
        // c.getPlayer().dropMessage(5, "You have gained a Warm Sun.");
        // break;
        // case AramiaFireWorks.DEC_ID:
        // c.getPlayer().dropMessage(5, "You have gained a Tree Decoration.");
        // break;
        // }
        // }
        c.getPlayer().havePartyQuest(item.getItemId());
        if (show) {
            c.getSession().writeAndFlush(InfoPacket.getShowItemGain(item.getItemId(), item.getQuantity()));
        }
        return true;
    }

    public static Item checkEnhanced(final Item before, final MapleCharacter chr) {
        if (before instanceof Equip) {
            final Equip eq = (Equip) before;
            if (eq.getState(false) == 0 && (eq.getUpgradeSlots() >= 1 || eq.getLevel() >= 1)
                    && ItemConstants.卷軸.canScroll(eq.getItemId()) && Randomizer.nextInt(100) >= 80) { // 20%概率潛能掉寶
                eq.resetPotential(false);
            }
        }
        return before;
    }

    public static boolean checkSpace(final MapleClient c, final int itemid, int quantity, final String owner) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (c.getPlayer() == null || (ii.isPickupRestricted(itemid) && c.getPlayer().haveItem(itemid, 1, true, false))
                || (!ii.itemExists(itemid))) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        }
        if (quantity <= 0 && !ItemConstants.類型.可充值道具(itemid)) {
            return false;
        }
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        if (c == null || c.getPlayer() == null || c.getPlayer().getInventory(type) == null) { // wtf is causing this?
            return false;
        }
        if (!type.equals(MapleInventoryType.EQUIP)) {
            final short slotMax = ii.getSlotMax(itemid);
            final List<Item> existing = c.getPlayer().getInventory(type).listById(itemid);
            if (!ItemConstants.類型.可充值道具(itemid)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    for (Item eItem : existing) {
                        final short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner != null && owner.equals(eItem.getOwner())) {
                            final short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            // add new slots if there is still something left
            final int numSlotsNeeded;
            if (slotMax > 0 && !ItemConstants.類型.可充值道具(itemid)) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else {
                numSlotsNeeded = 1;
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot,
            final short quantity, final boolean fromDrop) {
        return removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static boolean removeFromSlot(final MapleClient c, final MapleInventoryType type, final short slot,
            short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            final boolean allowZero = consume && ItemConstants.類型.可充值道具(item.getItemId());
            c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
            if (GameConstants.isHarvesting(item.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }

            if (item.getQuantity() == 0 && !allowZero) {
                c.getSession().writeAndFlush(InventoryPacket.clearInventoryItem(type, item.getPosition(), fromDrop));
            } else {
                c.getSession().writeAndFlush(InventoryPacket.updateInventorySlot(item, fromDrop));
            }
            return true;
        }
        return false;
    }

    public static boolean removeById(final MapleClient c, final MapleInventoryType type, final int itemId,
            final int quantity, final boolean fromDrop, final boolean consume) {
        int remremove = quantity;
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            int theQ = item.getQuantity();
            if (remremove <= theQ
                    && removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume)) {
                remremove = 0;
                break;
            } else if (remremove > theQ
                    && removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume)) {
                remremove -= theQ;
            }
        }
        return remremove <= 0;
    }

    public static boolean removeFromSlot_Lock(final MapleClient c, final MapleInventoryType type, final short slot,
            short quantity, final boolean fromDrop, final boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        final Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            if (ItemFlag.LOCK.check(item.getFlag()) || ItemFlag.UNTRADABLE.check(item.getFlag())) {
                return false;
            }
            return removeFromSlot(c, type, slot, quantity, fromDrop, consume);
        }
        return false;
    }

    public static boolean removeById_Lock(final MapleClient c, final MapleInventoryType type, final int itemId) {
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            if (removeFromSlot_Lock(c, type, item.getPosition(), (short) 1, false, false)) {
                return true;
            }
        }
        return false;
    }

    public static void move(final MapleClient c, final MapleInventoryType type, final short src, final short dst) {
        if (src < 0 || dst < 0 || src == dst || type == MapleInventoryType.EQUIPPED) {
            return;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Item source = c.getPlayer().getInventory(type).getItem(src);
        final Item initialTarget = c.getPlayer().getInventory(type).getItem(dst);
        if (source == null) {
            return;
        }
        boolean bag = false, switchSrcDst = false, bothBag = false;
        if (dst > c.getPlayer().getInventory(type).getSlotLimit()) {
            if (type == MapleInventoryType.ETC && dst > 100 && dst % 100 != 0) {
                final int eSlot = c.getPlayer().getExtendedSlot((dst / 100) - 1);
                if (eSlot > 0) {
                    final MapleStatEffect ee = ii.getItemEffect(eSlot);
                    if (dst % 100 > ee.getSlotCount() || ee.getType() != ii.getBagType(source.getItemId())
                            || ee.getType() <= 0) {
                        c.getPlayer().dropMessage(1, "無法移動道具到背包.");
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    } else {
                        bag = true;
                    }
                } else {
                    c.getPlayer().dropMessage(1, "背包已滿, 無法移動.");
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
            } else {
                c.getPlayer().dropMessage(1, "無法移動到那裡.");
                c.getSession().writeAndFlush(CWvsContext.enableActions());
                return;
            }
        }
        if (src > c.getPlayer().getInventory(type).getSlotLimit() && type == MapleInventoryType.ETC && src > 100
                && src % 100 != 0) {
            // source should be not null so not much checks are needed
            if (!bag) {
                switchSrcDst = true;
                bag = true;
            } else {
                bothBag = true;
            }
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        final short oldsrcQ = source.getQuantity();
        final short slotMax = ii.getSlotMax(source.getItemId());
        c.getPlayer().getInventory(type).move(src, dst, slotMax);
        if (GameConstants.isHarvesting(source.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }
        final List<ModifyInventory> mods = new ArrayList<>();
        if (!type.equals(MapleInventoryType.EQUIP) && initialTarget != null
                && initialTarget.getItemId() == source.getItemId() && initialTarget.getOwner().equals(source.getOwner())
                && initialTarget.getExpiration() == source.getExpiration()
                && !ItemConstants.類型.可充值道具(source.getItemId()) && !type.equals(MapleInventoryType.CASH)) {
            if (GameConstants.isHarvesting(initialTarget.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            if ((olddstQ + oldsrcQ) > slotMax) {
                mods.add(new ModifyInventory(bag && (switchSrcDst || bothBag) ? ModifyInventory.Types.UPDATE_IN_BAG
                        : ModifyInventory.Types.UPDATE, source));
            } else {
                mods.add(new ModifyInventory(bag && (switchSrcDst || bothBag) ? ModifyInventory.Types.REMOVE_IN_BAG
                        : ModifyInventory.Types.REMOVE, source));
            }
            mods.add(new ModifyInventory(bag && (!switchSrcDst || bothBag) ? ModifyInventory.Types.UPDATE_IN_BAG
                    : ModifyInventory.Types.UPDATE, initialTarget));
        } else {
            mods.add(new ModifyInventory(
                    bag ? ModifyInventory.Types.MOVE_TO_BAG
                            : bothBag ? ModifyInventory.Types.MOVE_IN_BAG : ModifyInventory.Types.MOVE,
                    source, switchSrcDst ? dst : src));
        }
        c.getSession().writeAndFlush(InventoryPacket.modifyInventory(true, mods));
    }

    public static void equip(final MapleClient c, final short src, short dst) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleCharacter chr = c.getPlayer();
        if (chr == null || dst == -55) {
            return;
        }
        final PlayerStats statst = c.getPlayer().getStat();
        statst.recalcLocalStats(c.getPlayer());
        Equip source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src); // Equip
        Equip target = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst); // Currently equipping

        if (source == null || source.getDurability() == 0 || GameConstants.isHarvesting(source.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }

        if (ItemConstants.類型.管理員裝備(source.getItemId()) && !c.getPlayer().isStaff()) {
            c.getPlayer().dropMessage(1, "只有管理員能裝備這件道具。");
            c.getPlayer().removeAll(source.getItemId(), false);
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }

        if (ItemConstants.isMadeByGM(c, source.getItemId(), src) && !c.getPlayer().isStaff()) {
            c.getPlayer().dropMessage(1, "You are not allowed to use GM-Made equips.");
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }

        // if (ItemConstants.isOverPoweredEquip(c, source.getItemId(), src) &&
        // !c.getPlayer().isStaff()) {
        // c.getPlayer().dropMessage(1, "這件裝備的能量看起來太過於強大，如果你覺得是系統錯誤請報告給管理員。");
        // //c.getPlayer().removeAll(source.getItemId(), false); //System might be wrong
        // c.getSession().writeAndFlush(CWvsContext.enableActions());
        // return;
        // }
        final Map<String, Integer> stats = ii.getEquipStats(source.getItemId());

        if (stats == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (dst > -1200 && dst < -999 && !ItemConstants.類型.龍裝備(source.getItemId())
                && !ItemConstants.類型.機械(source.getItemId())) {
            if (chr.isShowErr()) {
                chr.dropMessage(5, new StringBuilder().append("穿戴裝備 - 1 ").append(source.getItemId()).toString());
            }
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        } else if ((dst < -5200 || (dst >= -999 && dst < -99)) && !stats.containsKey("cash")) {
            if (chr.isShowErr()) {
                chr.dropMessage(5,
                        new StringBuilder().append("穿戴裝備 - 2 ").append(source.getItemId()).append(" dst: ").append(dst)
                                .append(" 檢測1: ").append(dst <= -1200).append(" 檢測2: ")
                                .append((dst >= -5003) && (dst < -99)).append(" 檢測3: ")
                                .append(!stats.containsKey("cash")).toString());
            }
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        } else if ((dst <= -1300) && (dst > -1306) && (!MapleJob.is天使破壞者(chr.getJob()))) {
            if (chr.isShowErr()) {
                chr.dropMessage(5,
                        new StringBuilder().append("穿戴裝備 - 3 ").append(source.getItemId()).append(" dst: ").append(dst)
                                .append(" 檢測1: ").append((dst <= -1300) && (dst > -1306)).append(" 檢測2: ")
                                .append(!MapleJob.is天使破壞者(chr.getJob())).toString());
            }
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (!ii.canEquip(stats, source.getItemId(), chr.getLevel(), chr.getJob(), chr.getFame(), statst.getTotalStr(),
                statst.getTotalDex(), statst.getTotalLuk(), statst.getTotalInt(), statst.levelBonus,
                source.getReqLevel(), chr.getTotalSkillLevel(30010242) > 0 || chr.getTotalSkillLevel(30020240) > 0)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if ((ItemConstants.is透明短刀(source.getItemId()) && dst != -110) & ItemConstants.類型.副手(source.getItemId())
                && dst != -10) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (ItemConstants.類型.武器(source.getItemId()) && dst != -11 && dst != -5200) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (dst == -5200 && !ItemConstants.類型.扇子(source.getItemId())
                && c.getPlayer().getTotalSkillLevel(40020109) == 0) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (dst == -18 && !GameConstants.isMountItemAvailable(source.getItemId(), c.getPlayer().getJob())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (dst == -118 && source.getItemId() / 10000 != 190) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (dst == -119 && source.getItemId() / 10000 != 191) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (dst > -5003 && dst <= -5000 && source.getItemId() / 10000 != 120) {
            chr.dropMessage(1, "無法將此裝備佩戴這個地方，該位置只能裝備圖騰道具");
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if ((dst == -31) && (source.getItemId() / 10000 != 116)) {
            chr.dropMessage(1, "無法將此裝備佩戴這個地方，該位置只能裝備口袋道具");
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if ((dst == -34) && (source.getItemId() / 10000 != 118)) {
            chr.dropMessage(1, "無法將此裝備佩戴這個地方，該位置只能裝備胸章道具");
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if ((dst == -35) && (!ItemConstants.類型.能源(source.getItemId()))) {
            chr.dropMessage(1, "無法將此裝備佩戴這個地方，該位置只能裝備能源道具");
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (dst == -36) { // pendant
            MapleQuestStatus stat = c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.墜飾欄));
            if (stat == null || stat.getCustomData() == null || (Long.parseLong(stat.getCustomData()) != 0
                    && Long.parseLong(stat.getCustomData()) < System.currentTimeMillis())) {
                c.getSession().writeAndFlush(CWvsContext.enableActions());
                return;
            }
        }
        if (!ItemConstants.is透明短刀(source.getItemId())
                && (ItemConstants.類型.雙刀(source.getItemId()) || source.getItemId() / 10000 == 135)) {
            dst = (byte) -10; // 盾牌欄位
        }
        if (ItemConstants.類型.龍裝備(source.getItemId()) && (chr.getJob() < 2200 || chr.getJob() > 2218)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }

        if (ItemConstants.類型.機械(source.getItemId()) && (chr.getJob() < 3500 || chr.getJob() > 3512)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }

        if (source.getItemId() / 1000 == 1112) { // ring
            for (RingSet s : RingSet.values()) {
                if (s.id.contains(source.getItemId())) {
                    List<Integer> theList = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).listIds();
                    for (Integer i : s.id) {
                        if (theList.contains(i)) {
                            c.getPlayer().dropMessage(1, "You may not equip this item because you already have a "
                                    + (StringUtil.makeEnumHumanReadable(s.name())) + " equipped.");
                            c.getSession().writeAndFlush(CWvsContext.enableActions());
                            return;
                        }
                    }
                }
            }
        }

        final List<ModifyInventory> mods = new ArrayList<>();

        Equip target2 = null;
        switch (dst) {
            case -6: { // 褲裙Bottom
                target2 = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                if (target2 != null && ItemConstants.類型.套服(target2.getItemId())) {
                    if (target != null && chr.getInventory(MapleInventoryType.EQUIP).isFull(1)) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                } else {
                    target2 = null;
                }
                break;
            }
            case -5: { // 衣服Top
                target2 = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -6);
                if (target2 != null && ItemConstants.類型.套服(source.getItemId())) {
                    if (target != null && chr.getInventory(MapleInventoryType.EQUIP).isFull(1)) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                } else {
                    target2 = null;
                }
                break;
            }
            case -10: { // 副手Shield
                target2 = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (ItemConstants.類型.雙刀(source.getItemId())) {
                    if ((chr.getJob() != 900 && !MapleJob.is影武者(chr.getJob())) || target2 == null
                            || !ItemConstants.類型.短劍(target2.getItemId())) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    target2 = null;
                } else if (target2 != null && ItemConstants.類型.雙手武器(target2.getItemId())
                        && !ItemConstants.類型.特殊副手(source.getItemId())) {
                    if (target != null && chr.getInventory(MapleInventoryType.EQUIP).isFull(1)) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                } else {
                    target2 = null;
                }
                break;
            }
            case -11: { // 武器Weapon
                target2 = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
                if (target2 != null && ItemConstants.類型.雙手武器(source.getItemId())
                        && !ItemConstants.類型.特殊副手(target2.getItemId())) {
                    if (target != null && chr.getInventory(MapleInventoryType.EQUIP).isFull(1)) {
                        c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                        c.getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                } else {
                    target2 = null;
                }
                break;
            }
        }
        int flag = source.getFlag();
        if (stats.get("equipTradeBlock") != null || source.getItemId() / 10000 == 167) { // Block trade when equipped.
            if (!ItemFlag.UNTRADABLE.check(flag)) {
                flag |= ItemFlag.UNTRADABLE.getValue();
                source.setFlag(flag);
                c.getSession().writeAndFlush(InventoryPacket.updateSpecialItemUse_(source));
            }
        }
        if (source.getItemId() / 10000 == 166) {
            if (source.getAndroid() == null) {
                int uid = MapleInventoryIdentifier.getInstance();
                source.setUniqueId(uid);
                source.setAndroid(MapleAndroid.create(source.getItemId(), uid));
                flag = flag | ItemFlag.UNTRADABLE.getValue();
                flag = flag | ItemFlag.ANDROID_ACTIVATED.getValue();
                source.setFlag(flag);
                c.getSession().writeAndFlush(CWvsContext.InventoryPacket.updateSpecialItemUse_(source));
            }
            final Equip heart = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -33);
            if (heart != null) {
                chr.removeAndroid();
                chr.setAndroid(source.getAndroid());
            }
        }
        if (source.getCharmEXP() > 0 && !ItemFlag.CHARM_EQUIPPED.check(flag)) {
            chr.getTrait(MapleTraitType.charm).addExp(source.getCharmEXP(), chr);
            source.setCharmEXP((short) 0);
            flag |= ItemFlag.CHARM_EQUIPPED.getValue();
            source.setFlag(flag);
            c.getSession().writeAndFlush(InventoryPacket.updateSpecialItemUse_(source));
        }

        chr.getInventory(MapleInventoryType.EQUIP).removeSlot(src);
        source.setPosition(dst);
        mods.add(new ModifyInventory(ModifyInventory.Types.MOVE, source, src));

        if (target != null) {
            chr.getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
            target.setPosition(src);
            chr.getInventory(MapleInventoryType.EQUIP).addFromDB(target);
        }

        if (target2 != null) {
            short slot = target2.getPosition();
            chr.getInventory(MapleInventoryType.EQUIPPED).removeSlot(slot);
            target2.setPosition(target == null ? src : chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            chr.getInventory(MapleInventoryType.EQUIP).addFromDB(target2);
            mods.add(new ModifyInventory(ModifyInventory.Types.MOVE, target2, slot));
        }

        chr.getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);

        c.getSession().writeAndFlush(InventoryPacket.modifyInventory(true, mods));

        if (ItemConstants.類型.武器(source.getItemId())) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.Booster);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.NoBulletConsume);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SoulArrow);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.WeaponCharge);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.AssistCharge);
        }
        if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.Mechanic);
        }
        if (source.getState(false) >= 17) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] potentials = {source.getPotential(1, false), source.getPotential(2, false),
                source.getPotential(3, false), source.getPotential(1, true), source.getPotential(2, true),
                source.getPotential(3, true)};
            for (int i : potentials) {
                if (i > 0) {
                    int reqLevel = Math.min(ii.getReqLevel(source.getItemId()) / 10, 19);
                    StructItemOption pot = ii.getPotentialInfo(i).get(reqLevel);
                    if (pot != null && pot.get("skillID") > 0) {
                        ss.put(SkillFactory
                                .getSkill(PlayerStats.getSkillByJob(pot.get("skillID"), c.getPlayer().getJob())),
                                new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(ss, true);
        }
        if (source.getSocketState() > 15) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] sockets = {source.getSocket(1), source.getSocket(2), source.getSocket(3)};
            for (int i : sockets) {
                if (i > 0) {
                    StructItemOption soc = ii.getSocketInfo(i);
                    if (soc != null && soc.get("skillID") > 0) {
                        ss.put(SkillFactory
                                .getSkill(PlayerStats.getSkillByJob(soc.get("skillID"), c.getPlayer().getJob())),
                                new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(ss, true);
        }

        if (dst > -1300 && dst <= -1200 && chr.getAndroid() != null) {
            chr.updateAndroid(dst);
        }
        chr.equipChanged();
        if (GameConstants.Equipment_Bonus_EXP(source.getItemId()) > 0) {
            chr.startFairySchedule(true, true);
        }
        if (target != null && chr.isSoulWeapon(target)) {
            chr.unequipSoulWeapon(target);
        }
        if (chr.isSoulWeapon(source)) {
            chr.equipSoulWeapon(source);
        }
    }

    public static void unequip(final MapleClient c, final short src, final short dst) {
        Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);

        if (dst < 0 || source == null || src == -55) {
            return;
        }
        if (target != null && src <= 0) { // do not allow switching with equip
            c.getSession().writeAndFlush(InventoryPacket.getInventoryFull());
            return;
        }
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }

        if (ItemConstants.類型.武器(source.getItemId())) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.Booster);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.NoBulletConsume);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.SoulArrow);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.WeaponCharge);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.AssistCharge);
        } else if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.Mechanic);
        } else if (source.getItemId() / 10000 == 166) {
            c.getPlayer().removeAndroid();
        } else if ((source.getItemId() / 10000 == 167) && (c.getPlayer().getAndroid() != null)) {
            c.getPlayer().removeAndroid();
            c.getSession().writeAndFlush(CField.removeAndroidHeart());
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (source.getState(false) >= 17) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] potentials = {source.getPotential(1, false), source.getPotential(2, false),
                source.getPotential(3, false), source.getPotential(1, true), source.getPotential(2, true),
                source.getPotential(3, true)};
            for (int i : potentials) {
                if (i > 0) {
                    int reqLevel = Math.min(ii.getReqLevel(source.getItemId()) / 10, 19);
                    StructItemOption pot = ii.getPotentialInfo(i).get(reqLevel);
                    if (pot != null && pot.get("skillID") > 0) {
                        ss.put(SkillFactory
                                .getSkill(PlayerStats.getSkillByJob(pot.get("skillID"), c.getPlayer().getJob())),
                                new SkillEntry((byte) -1, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(ss, true);
        }
        if (source.getSocketState() > 15) {
            final Map<Skill, SkillEntry> ss = new HashMap<>();
            int[] sockets = {source.getSocket(1), source.getSocket(2), source.getSocket(3)};
            for (int i : sockets) {
                if (i > 0) {
                    StructItemOption soc = ii.getSocketInfo(i);
                    if (soc != null && soc.get("skillID") > 0) {
                        ss.put(SkillFactory
                                .getSkill(PlayerStats.getSkillByJob(soc.get("skillID"), c.getPlayer().getJob())),
                                new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(ss, true);
        }
        c.getSession().writeAndFlush(
                InventoryPacket.modifyInventory(true, new ModifyInventory(ModifyInventory.Types.MOVE, source, src)));
        if (src > -1300 && src <= -1200 && c.getPlayer().getAndroid() != null) {
            c.getPlayer().updateAndroid(src);
        }
        c.getPlayer().equipChanged();
        if (GameConstants.Equipment_Bonus_EXP(source.getItemId()) > 0) {
            c.getPlayer().cancelFairySchedule(true);
        }
        if (c.getPlayer().isSoulWeapon(source)) {
            c.getPlayer().unequipSoulWeapon(source);
        }
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, final short quantity) {
        return drop(c, type, src, quantity, false);
    }

    public static boolean drop(final MapleClient c, MapleInventoryType type, final short src, short quantity,
            final boolean npcInduced) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (src < 0) {
            type = MapleInventoryType.EQUIPPED;
        }
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return false;
        }
        final Item source = c.getPlayer().getInventory(type).getItem(src);
        if (quantity < 0 || source == null || src == -55 || (!npcInduced && ItemConstants.類型.寵物(source.getItemId()))
                || (quantity == 0 && !ItemConstants.類型.可充值道具(source.getItemId())) || c.getPlayer().inPVP()) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        }

        final int flag = source.getFlag();
        if (quantity > source.getQuantity() && !ItemConstants.類型.可充值道具(source.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        }
        if (ItemFlag.LOCK.check(flag) || (quantity != 1 && type == MapleInventoryType.EQUIP)) { // hack
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        }
        if (ii.isCash(source.getItemId())) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        }
        final Point dropPos = new Point(c.getPlayer().getPosition());
        c.getPlayer().getCheatTracker().checkDrop();
        if (quantity < source.getQuantity() && !ItemConstants.類型.可充值道具(source.getItemId())) {
            final Item target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.getSession().writeAndFlush(InventoryPacket.dropInventoryItemUpdate(source));

            if (ItemConstants.類型.寵物(source.getItemId()) || ItemFlag.UNTRADABLE.check(flag)
                    || ii.isDropRestricted(source.getItemId()) || ii.isAccountShared(source.getItemId())
                    || ii.isShareTagEnabled(source.getItemId()) || ii.isSharableOnce(source.getItemId())) {
                if (GameConstants.isAnyDropMap(c.getPlayer().getMapId())) {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else if (ii.isAccountShared(source.getItemId()) || ii.isShareTagEnabled(source.getItemId())
                        || ii.isSharableOnce(source.getItemId())) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                } else if (ItemFlag.KARMA.check(flag)) {
                    target.setFlag(flag - ItemFlag.KARMA.getValue());
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                }
            } else {
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
            }
        } else {
            c.getPlayer().getInventory(type).removeSlot(src);
            if (GameConstants.isHarvesting(source.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            c.getSession()
                    .writeAndFlush(InventoryPacket.dropInventoryItem((src < 0 ? MapleInventoryType.EQUIP : type), src));
            if (src < 0) {
                c.getPlayer().equipChanged();
            }
            if (ItemConstants.類型.寵物(source.getItemId()) || ItemFlag.UNTRADABLE.check(flag)
                    || ii.isDropRestricted(source.getItemId()) || ii.isAccountShared(source.getItemId())
                    || ii.isShareTagEnabled(source.getItemId()) || ii.isSharableOnce(source.getItemId())) {
                if (GameConstants.isAnyDropMap(c.getPlayer().getMapId())) {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else if (ii.isAccountShared(source.getItemId()) || ii.isShareTagEnabled(source.getItemId())
                        || ii.isSharableOnce(source.getItemId())) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                } else if (ItemFlag.KARMA.check(flag)) {
                    source.setFlag(flag - ItemFlag.KARMA.getValue());
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                }
            } else {
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
            }
        }
        return true;
    }

    public static String searchId(int type, String search) {
        String result = "";
        MapleData data = null;
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider("String");
        // result += "<<Type: " + type + " | Search: " + search + ">>";
        switch (type) {
            case 1:
                List<String> retNpcs = new ArrayList<>();
                data = dataProvider.getData("Npc.img");
                List<Pair<Integer, String>> npcPairList = new LinkedList<>();
                for (MapleData npcIdData : data.getChildren()) {
                    npcPairList.add(new Pair<>(Integer.parseInt(npcIdData.getName()),
                            MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
                }
                for (Pair<Integer, String> npcPair : npcPairList) {
                    if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                        retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                    }
                }
                if (retNpcs != null && retNpcs.size() > 0) {
                    for (String singleRetNpc : retNpcs) {
                        result += singleRetNpc;
                    }
                } else {
                    result += "No NPC's Found";
                }
                break;
            case 2:
                List<String> retMaps = new ArrayList<>();
                data = dataProvider.getData("Map.img");
                List<Pair<Integer, String>> mapPairList = new LinkedList<>();
                for (MapleData mapAreaData : data.getChildren()) {
                    for (MapleData mapIdData : mapAreaData.getChildren()) {
                        mapPairList.add(new Pair<>(Integer.parseInt(mapIdData.getName()),
                                MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - "
                                + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
                    }
                }
                for (Pair<Integer, String> mapPair : mapPairList) {
                    if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                        retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                    }
                }
                if (retMaps != null && retMaps.size() > 0) {
                    for (String singleRetMap : retMaps) {
                        result += singleRetMap;
                    }
                } else {
                    result += "No Maps Found";
                }
                break;
            case 3:
                List<String> retMobs = new ArrayList<>();
                data = dataProvider.getData("Mob.img");
                List<Pair<Integer, String>> mobPairList = new LinkedList<>();
                for (MapleData mobIdData : data.getChildren()) {
                    mobPairList.add(new Pair<>(Integer.parseInt(mobIdData.getName()),
                            MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
                }
                for (Pair<Integer, String> mobPair : mobPairList) {
                    if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                        retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                    }
                }
                if (retMobs != null && retMobs.size() > 0) {
                    for (String singleRetMob : retMobs) {
                        result += singleRetMob;
                    }
                } else {
                    result += "No Mobs Found";
                }
                break;
            case 4:
                List<String> retItems = new ArrayList<>();
                for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                    if (itemPair != null && itemPair.name != null
                            && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                        retItems.add("\r\n#b" + itemPair.itemId + " " + " #k- " + " #r#z" + itemPair.itemId + "##k");
                    }
                }
                if (retItems != null && retItems.size() > 0) {
                    for (String singleRetItem : retItems) {
                        if (result.length() < 10000) {
                            result += singleRetItem;
                        } else {
                            result += "\r\n#bCouldn't load all items, there are too many results.#k";
                            return result;
                        }
                    }
                } else {
                    result += "No Items Found";
                }
                break;
            case 5:
                List<String> retQuests = new ArrayList<>();
                for (MapleQuest itemPair : MapleQuest.getAllInstances()) {
                    if (itemPair.getName().length() > 0
                            && itemPair.getName().toLowerCase().contains(search.toLowerCase())) {
                        retQuests.add(itemPair.getId() + " - " + itemPair.getName());
                    }
                }
                if (retQuests != null && retQuests.size() > 0) {
                    for (String singleRetQuest : retQuests) {
                        result += singleRetQuest;
                    }
                } else {
                    result += "No Quests Found";
                }
                break;
            case 6:
                List<String> retSkills = new ArrayList<>();
                for (Skill skil : SkillFactory.getAllSkills()) {
                    if (skil.getName() != null && skil.getName().toLowerCase().contains(search.toLowerCase())) {
                        retSkills.add(skil.getId() + " - " + skil.getName());
                    }
                }
                if (retSkills != null && retSkills.size() > 0) {
                    for (String singleRetSkill : retSkills) {
                        result += singleRetSkill;
                    }
                } else {
                    result += "No Skills Found";
                }
                break;
            default:
                result += "Invalid Type";
        }
        return result;
    }
}
