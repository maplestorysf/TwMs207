package server;

import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.MapleRing;
import constants.GameConstants;
import constants.ItemConstants;
import database.ManagerDatabasePool;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tools.FileoutputUtil;
import tools.Pair;
import tools.packet.CCashShop;

public class CashShop implements Serializable {

    private static final long serialVersionUID = 231541893513373579L;
    private final int accountId, characterId;
    private final ItemLoader factory = ItemLoader.CASHSHOP;
    private final List<Item> inventory = new ArrayList<>();
    private final List<Integer> uniqueids = new ArrayList<>();

    public CashShop(int accountId, int characterId, int jobType) throws SQLException {
        this.accountId = accountId;
        this.characterId = characterId;
        for (Pair<Item, MapleInventoryType> item : factory.loadItems(false, accountId).values()) {
            inventory.add(item.getLeft());
        }
    }

    public int getItemsSize() {
        return inventory.size();
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public Item findByCashId(int cashId) {
        for (Item item : inventory) {
            if (item.getUniqueId() == cashId) {
                return item;
            }
        }

        return null;
    }

    public void checkExpire(MapleClient c) {
        List<Item> toberemove = new ArrayList<>();
        for (Item item : inventory) {
            if (item != null && !ItemConstants.類型.寵物(item.getItemId()) && item.getExpiration() > 0
                    && item.getExpiration() < System.currentTimeMillis()) {
                toberemove.add(item);
            } else if (ItemConstants.類型.寵物(item.getItemId())
                    && MapleItemInformationProvider.getInstance().getLimitedLife(item.getItemId()) > 0
                    && item.getPet() != null && item.getPet().getLimitedLife() <= 0) {
                toberemove.add(item);
            }
        }
        if (toberemove.size() > 0) {
            for (Item item : toberemove) {
                removeFromInventory(item);
                c.getSession().writeAndFlush(CCashShop.cashItemExpired(item.getUniqueId()));
            }
            toberemove.clear();
        }
    }

    public Item toItem(CashItem cItem) {
        return toItem(cItem, MapleInventoryManipulator.getUniqueId(cItem.getItemId(), null), "");
    }

    public Item toItem(CashItem cItem, String gift) {
        return toItem(cItem, MapleInventoryManipulator.getUniqueId(cItem.getItemId(), null), gift);
    }

    public Item toItem(CashItem cItem, int uniqueid) {
        return toItem(cItem, uniqueid, "");
    }

    public Item toItem(CashItem cItem, int uniqueid, String gift) {
        if (uniqueid <= 0) {
            uniqueid = MapleInventoryIdentifier.getInstance();
        }
        long period = cItem.getPeriod();
        Item ret;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.getInventoryType(cItem.getItemId()) == MapleInventoryType.EQUIP) {
            Equip eq = (Equip) ii.getEquipById(cItem.getItemId(), uniqueid);
            if (period > 0) {
                eq.setExpiration(System.currentTimeMillis() + period * 24 * 60 * 60 * 1000);
            }
            eq.setGMLog("商場購買 - SN:" + cItem.getSN() + " 時間: " + FileoutputUtil.CurrentReadable_Date());

            eq.setGiftFrom(gift);
            if (ItemConstants.類型.特效戒指(cItem.getItemId()) && uniqueid > 0) {
                MapleRing ring = MapleRing.loadFromDb(uniqueid);
                if (ring != null) {
                    eq.setRing(ring);
                }
            }
            ret = eq.copy();
        } else {
            Item item = new Item(cItem.getItemId(), (byte) 0, (short) cItem.getCount(), 0, uniqueid, (short) 0);
            if (ItemConstants.類型.寵物(cItem.getItemId())) {
                period = ii.getLife(cItem.getItemId());
                final MaplePet pet = MaplePet.createPet(cItem.getItemId(), uniqueid);
                if (pet != null) {
                    item.setPet(pet);
                }
            }
            if (period > 0) {
                item.setExpiration(System.currentTimeMillis() + period * 24 * 60 * 60 * 1000);
            }
            item.setGMLog("商場購買 - SN:" + cItem.getSN() + " 時間: " + FileoutputUtil.CurrentReadable_Date());
            item.setGiftFrom(gift);
            ret = item.copy();
        }
        return ret;
    }

    public void addToInventory(Item item) {
        inventory.add(item);
    }

    public void removeFromInventory(Item item) {
        inventory.remove(item);
    }

    public void gift(int recipient, String from, String message, int sn) {
        gift(recipient, from, message, sn, 0);
    }

    public void gift(int recipient, String from, String message, int sn, int uniqueid) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO `gifts` VALUES (DEFAULT, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, recipient);
                ps.setString(2, from);
                ps.setString(3, message);
                ps.setInt(4, sn);
                ps.setInt(5, uniqueid);
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException sqle) {
        }
    }

    public List<Pair<Item, String>> loadGifts() {
        List<Pair<Item, String>> gifts = new ArrayList<>();
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM `gifts` WHERE `recipient` = ?")) {
                ps.setInt(1, characterId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        CashModItem cmItem = CashItemFactory.getInstance().getItem(rs.getInt("sn"));
                        if (cmItem == null) {
                            continue;
                        }
                        Item item = toItem(cmItem, rs.getInt("uniqueid"), rs.getString("from"));
                        gifts.add(new Pair<>(item, rs.getString("message")));
                        uniqueids.add(item.getUniqueId());
                        List<Integer> packages = CashItemFactory.getInstance().getPackageItems(cmItem.getItemId());
                        if (packages != null && packages.size() > 0) {
                            for (int packageItem : packages) {
                                CashItem pack = CashItemFactory.getInstance().getOriginSimpleItem(packageItem);
                                if (pack != null) {
                                    addToInventory(toItem(pack, rs.getString("from")));
                                }
                            }
                        } else {
                            addToInventory(item);
                        }
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException sqle) {
        }
        return gifts;
    }

    public boolean canSendNote(int uniqueid) {
        return uniqueids.contains(uniqueid);
    }

    public void sendedNote(int uniqueid) {
        for (int i = 0; i < uniqueids.size(); i++) {
            if (uniqueids.get(i) == uniqueid) {
                uniqueids.remove(i);
            }
        }
    }

    public void save(Connection con) throws SQLException {
        List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

        for (Item item : inventory) {
            itemsWithType.add(new Pair<>(item, GameConstants.getInventoryType(item.getItemId())));
        }

        factory.saveItems(itemsWithType, accountId, con);
    }
}
