/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.stores;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ItemConstants;
import database.ManagerDatabasePool;
import handling.channel.ChannelServer;
import handling.world.World;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.FileoutputUtil;
import tools.Pair;
import tools.packet.PlayerShopPacket;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractPlayerStore extends MapleMapObject implements IMaplePlayerShop {

    protected boolean open = false, available = false;
    protected String ownerName, des, pass;
    protected int ownerId, owneraccount, itemId, channel, map, fh;
    protected AtomicLong meso = new AtomicLong(0);
    protected List<WeakReference<MapleCharacter>> chrs;
    protected Map<String, VisitorInfo> visitorsList = new HashMap<>();
    protected List<BoughtItem> bought = new LinkedList<>();
    protected List<MaplePlayerShopItem> items = new LinkedList<>();
    protected List<Pair<String, Byte>> messages = new LinkedList<>();

    public AbstractPlayerStore(MapleCharacter owner, int itemId, String desc, String pass, int slots) {
        this.setPosition(owner.getTruePosition());
        this.ownerName = owner.getName();
        this.ownerId = owner.getId();
        this.owneraccount = owner.getAccountID();
        this.itemId = itemId;
        this.des = desc;
        this.pass = pass;
        this.map = owner.getMapId();
        this.channel = owner.getClient().getChannel();
        this.fh = owner.getFh();
        chrs = new LinkedList<>();
        for (int i = 0; i < slots; i++) {
            chrs.add(new WeakReference<>(null));
        }
    }

    @Override
    public int getMaxSize() {
        return chrs.size() + 1;
    }

    @Override
    public int getSize() {
        return getFreeSlot() == -1 ? getMaxSize() : getFreeSlot();
    }

    @Override
    public void broadcastToVisitors(byte[] packet) {
        broadcastToVisitors(packet, true);
    }

    public void broadcastToVisitors(byte[] packet, boolean owner) {
        for (WeakReference<MapleCharacter> chr : chrs) {
            if (chr != null && chr.get() != null) {
                chr.get().getClient().getSession().writeAndFlush(packet);
            }
        }
        if (getShopType() != IMaplePlayerShop.HIRED_MERCHANT && owner && getMCOwner() != null) {
            getMCOwner().getClient().getSession().writeAndFlush(packet);
        }
    }

    public void broadcastToVisitors(byte[] packet, int exception) {
        for (WeakReference<MapleCharacter> chr : chrs) {
            if (chr != null && chr.get() != null && getVisitorSlot(chr.get()) != exception) {
                chr.get().getClient().getSession().writeAndFlush(packet);
            }
        }
        if (getShopType() != IMaplePlayerShop.HIRED_MERCHANT && getMCOwner() != null && exception != ownerId) {
            getMCOwner().getClient().getSession().writeAndFlush(packet);
        }
    }

    @Override
    public long getMeso() {
        return meso.get();
    }

    @Override
    public void setMeso(long meso) {
        this.meso.set(meso);
    }

    @Override
    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean saveItems() {
        if (getShopType() != IMaplePlayerShop.HIRED_MERCHANT) { // hired merch only
            FileoutputUtil.log(FileoutputUtil.Hired_Merchant_Error, "儲存精靈商人資料時異常: 儲存的商店被判斷為非精靈商人,類型是" + getShopType());
            return false;
        }

        try {
            Connection con = ManagerDatabasePool.getConnection();
            PreparedStatement ps = con
                    .prepareStatement("DELETE FROM hiredmerch WHERE accountid = ? OR characterid = ?");
            ps.setInt(1, owneraccount);
            ps.setInt(2, ownerId);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement(
                    "INSERT INTO hiredmerch (characterid, accountid, Mesos, time) VALUES (?, ?, ?, ?)",
                    ManagerDatabasePool.RETURN_GENERATED_KEYS);
            ps.setInt(1, ownerId);
            ps.setInt(2, owneraccount);
            ps.setLong(3, meso.get());
            ps.setLong(4, System.currentTimeMillis());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Error, adding merchant to DB");
            }
            final int packageid = rs.getInt(1);
            rs.close();
            ps.close();
            List<Pair<Item, MapleInventoryType>> iters = new ArrayList<>();
            Item item;
            for (MaplePlayerShopItem pItems : items) {
                if (pItems.item == null || pItems.bundles <= 0) {
                    continue;
                }
                if (pItems.item.getQuantity() <= 0 && !ItemConstants.類型.可充值道具(pItems.item.getItemId())) {
                    continue;
                }
                item = pItems.item.copy();
                item.setQuantity((short) (item.getQuantity() * pItems.bundles));
                boolean add = iters.add(new Pair<>(item, GameConstants.getInventoryType(item.getItemId())));
            }
            ItemLoader.HIRED_MERCHANT.saveItems(iters, packageid, con);
            ManagerDatabasePool.closeConnection(con);
            return true;
        } catch (SQLException | RuntimeException ex) {
            FileoutputUtil.log(FileoutputUtil.Hired_Merchant_Error, "儲存精靈商人資料時異常: " + ex);
        }
        return false;
    }

    public MapleCharacter getVisitor(int num) {
        return chrs.get(num).get();
    }

    @Override
    public void update() {
        if (isAvailable()) {
            if (getShopType() == IMaplePlayerShop.HIRED_MERCHANT) {
                getMap().broadcastMessage(PlayerShopPacket.updateHiredMerchant((HiredMerchant) this));
            } else if (getMCOwner() != null) {
                getMap().broadcastMessage(PlayerShopPacket.sendPlayerShopBox(getMCOwner()));
            }
        }
    }

    @Override
    public void addVisitor(MapleCharacter visitor) {
        int i = getFreeSlot();
        if (i > 0) {
            if (getShopType() >= 3) {
                broadcastToVisitors(PlayerShopPacket.getMiniGameNewVisitor(visitor, i, (MapleMiniGame) this));
            } else {
                broadcastToVisitors(PlayerShopPacket.shopVisitorAdd(visitor, i));
            }
            chrs.remove(i - 1);
            chrs.add(i - 1, new WeakReference<>(visitor));
            updateVisitorsList(visitor, false);
            if (i == 6) {
                update();
            }
        }
    }

    public boolean isInVisitorsList(String visitorName) {
        return visitorsList.containsKey(visitorName);
    }

    public void updateVisitorsList(MapleCharacter visitor, boolean leave) {
        if (visitor != null && !isOwner(visitor) && !visitor.isGM()) {
            if (visitorsList.containsKey(visitor.getName())) {
                if (leave) {
                    visitorsList.get(visitor.getName()).updateInTime();
                } else {
                    visitorsList.get(visitor.getName()).updateStartTime();
                }
            } else {
                visitorsList.put(visitor.getName(), new VisitorInfo());
            }
        }
    }

    public void removeVisitorsList(String visitorName) {
        if (visitorsList.containsKey(visitorName)) {
            visitorsList.remove(visitorName);
        }
    }

    @Override
    public void removeVisitor(MapleCharacter visitor) {
        final byte slot = getVisitorSlot(visitor);
        boolean shouldUpdate = getFreeSlot() == -1;
        if (slot > 0) {
            broadcastToVisitors(PlayerShopPacket.shopVisitorLeave(slot), slot);
            chrs.remove(slot - 1);
            chrs.add(slot - 1, new WeakReference<>(null));
            if (shouldUpdate) {
                update();
            }
        }
    }

    @Override
    public byte getVisitorSlot(MapleCharacter visitor) {
        for (byte i = 0; i < chrs.size(); i++) {
            if (chrs.get(i) != null && chrs.get(i).get() != null && chrs.get(i).get().getId() == visitor.getId()) {
                return (byte) (i + 1);
            }
        }
        if (visitor.getId() == ownerId) { // can visit own store in merch, otherwise not.
            return 0;
        }
        return -1;
    }

    @Override
    public void removeAllVisitors(int error, int type) {
        for (int i = 0; i < chrs.size(); i++) {
            MapleCharacter visitor = getVisitor(i);
            if (visitor != null) {
                if (type != -1) {
                    visitor.getClient().getSession().writeAndFlush(PlayerShopPacket.shopErrorMessage(error, type));
                }
                broadcastToVisitors(PlayerShopPacket.shopVisitorLeave(getVisitorSlot(visitor)),
                        getVisitorSlot(visitor));
                visitor.setPlayerShop(null);
                chrs.remove(i);
                chrs.add(i, new WeakReference<>(null));
                updateVisitorsList(visitor, true);
            }
        }
        update();
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public int getOwnerAccId() {
        return owneraccount;
    }

    @Override
    public String getDescription() {
        if (des == null) {
            return "";
        }
        return des;
    }

    @Override
    public void setDescription(String desc) {
        if (des.equalsIgnoreCase(desc)) {
            return;
        }
        this.des = desc;
        if (isAvailable() && getShopType() == IMaplePlayerShop.HIRED_MERCHANT) {
            getMap().broadcastMessage(PlayerShopPacket.updateHiredMerchant((HiredMerchant) this, false));
        }
    }

    @Override
    public List<Pair<Byte, MapleCharacter>> getVisitors() {
        List<Pair<Byte, MapleCharacter>> chrz = new LinkedList<>();
        for (byte i = 0; i < chrs.size(); i++) { // include owner or no
            if (chrs.get(i) != null && chrs.get(i).get() != null) {
                boolean add = chrz.add(new Pair<>((byte) (i + 1), chrs.get(i).get()));
            }
        }
        return chrz;
    }

    @Override
    public List<MaplePlayerShopItem> getItems() {
        return items;
    }

    @Override
    public void addItem(MaplePlayerShopItem item) {
        // System.out.println("Adding item ... 2");
        items.add(item);
    }

    @Override
    public boolean removeItem(int item) {
        return false;
    }

    @Override
    public void removeFromSlot(int slot) {
        items.remove(slot);
    }

    @Override
    public byte getFreeSlot() {
        for (byte i = 0; i < chrs.size(); i++) {
            if (chrs.get(i) == null || chrs.get(i).get() == null) {
                return (byte) (i + 1);
            }
        }
        return -1;
    }

    @Override
    public int getItemId() {
        return itemId;
    }

    @Override
    public boolean isOwner(MapleCharacter chr) {
        return chr.getId() == ownerId && chr.getName().equals(ownerName);
    }

    @Override
    public String getPassword() {
        if (pass == null) {
            return "";
        }
        return pass;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
    }

    @Override
    public void sendSpawnData(MapleClient client) {
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SHOP;
    }

    public MapleCharacter getMCOwnerWorld() {
        int ourChannel = World.Find.findChannel(ownerId);
        if (ourChannel <= 0) {
            return null;
        }
        return ChannelServer.getInstance(ourChannel).getPlayerStorage().getCharacterById(ownerId);
    }

    public MapleCharacter getMCOwnerChannel() {
        return ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(ownerId);
    }

    public MapleCharacter getMCOwner() {
        return getMap().getCharacterById(ownerId);
    }

    public MapleMap getMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(map);
    }

    @Override
    public int getGameType() {
        if (getShopType() == IMaplePlayerShop.HIRED_MERCHANT) { // hiredmerch
            return 6;
        } else if (getShopType() == IMaplePlayerShop.PLAYER_SHOP) { // shop lol
            return 5;
        } else if (getShopType() == IMaplePlayerShop.OMOK) { // omok
            return 1;
        } else if (getShopType() == IMaplePlayerShop.MATCH_CARD) { // matchcard
            return 2;
        }
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void setAvailable(boolean b) {
        this.available = b;
    }

    @Override
    public List<BoughtItem> getBoughtItems() {
        return bought;
    }

    @Override
    public List<Pair<String, Byte>> getMessages() {
        return messages;
    }

    @Override
    public int getMapId() {
        return map;
    }

    @Override
    public int getChannel() {
        return channel;
    }

    public static final class VisitorInfo {

        public int inTime;
        public long startTime;

        public VisitorInfo() {
            this.inTime = 0;
            this.startTime = System.currentTimeMillis();
        }

        public void updateInTime() {
            int time = (int) (System.currentTimeMillis() - this.startTime);
            if (time > 0) {
                this.inTime += time;
            }
        }

        public int getInTime() {
            return this.inTime;
        }

        public void updateStartTime() {
            this.startTime = System.currentTimeMillis();
        }
    }

    public static final class BoughtItem {

        public int id;
        public int quantity;
        public long totalPrice;
        public String buyer;

        public BoughtItem(final int id, final int quantity, final long totalPrice, final String buyer) {
            this.id = id;
            this.quantity = quantity;
            this.totalPrice = totalPrice;
            this.buyer = buyer;
        }
    }

    public int getFh() {
        return fh;
    }
}
