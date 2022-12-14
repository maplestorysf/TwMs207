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
package handling.channel.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.BuddyList;
import client.BuddylistEntry;
import client.CharacterNameAndId;
import client.MapleCharacter;
import client.MapleClient;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import database.ManagerDatabasePool;
import handling.channel.ChannelServer;
import handling.world.World;
import tools.FileoutputUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext.BuddyListPacket;

public class BuddyListHandler {

    private static final class CharacterIdNameBuddyCapacity extends CharacterNameAndId {

        private final int buddyCapacity;
        private final int gm;

        public CharacterIdNameBuddyCapacity(int id, String name, String group, int buddyCapacity, int gm) {
            super(id, name, group);
            this.buddyCapacity = buddyCapacity;
            this.gm = gm;
        }

        public int getBuddyCapacity() {
            return buddyCapacity;
        }

        public int getGMLevel() {
            return gm;
        }
    }

    private static CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(final String name, final String group)
            throws SQLException {
        CharacterIdNameBuddyCapacity ret;
        Connection con = ManagerDatabasePool.getConnection();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE name LIKE ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                ret = null;
                if (rs.next()) {
                    ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), group,
                            rs.getInt("buddyCapacity"), rs.getInt("gm"));
                }
            }
        }
        ManagerDatabasePool.closeConnection(con);
        return ret;
    }

    public static final void BuddyOperation(final LittleEndianAccessor slea, final MapleClient c) {
        final int mode = slea.readByte();
        final BuddyList buddylist = c.getPlayer().getBuddylist();
        if (mode == 1) { // ????????????
            final String addName = slea.readMapleAsciiString();
            final String groupName = slea.readMapleAsciiString();
            String remark = slea.readMapleAsciiString();
            String alias = null;
            if (slea.readByte() == 1) {
                alias = slea.readMapleAsciiString();
            }
            if (slea.available() > 0) {
                if (c.getPlayer().isShowErr()) {
                    c.getPlayer().showInfo("????????????", true, "????????????????????? " + slea.toString());
                }
            }
            if (alias != null) {
                c.getPlayer().dropMessage(1, "?????????????????????????????????????????????");
                return;
            }

            final BuddylistEntry ble = buddylist.get(addName);

            if (addName.getBytes().length > 15 || groupName.getBytes().length > 16
                    || (alias != null && alias.getBytes().length > 15) || remark.getBytes().length > 133) {
                return;
            }
            // ?????????????????????????????????????????????
            if (ble != null && (ble.getGroup().equals(groupName) || !ble.isVisible())) { // ?????????????????????
                c.getSession().writeAndFlush(BuddyListPacket
                        .buddylistMessage(alias != null ? BuddyOperation.????????????????????? : BuddyOperation.????????????????????????));
            } else if (ble != null && ble.isVisible()) { // ???????????????, ????????????
                ble.setGroup(groupName);
                c.getSession().writeAndFlush(BuddyListPacket.getBuddylist(buddylist.getBuddies()));
            } else if (buddylist.isFull()) { // ?????????????????????
                c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.?????????));
            } else { // ????????????????????????
                try {
                    // ????????????????????????
                    CharacterIdNameBuddyCapacity charWithId = null;
                    int channel = World.Find.findChannel(addName);
                    MapleCharacter otherChar = null;
                    if (channel > 0) {
                        otherChar = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(addName);
                        if (otherChar == null) {
                            charWithId = getCharacterIdAndNameFromDatabase(addName, groupName);
                        } else {
                            charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getName(),
                                    groupName, otherChar.getBuddylist().getCapacity(), otherChar.getGmLevel());
                        }
                    } else {
                        charWithId = getCharacterIdAndNameFromDatabase(addName, groupName);
                    }

                    // ?????????????????????????????????
                    if (charWithId == null) {
                        c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.???????????????));
                        return;
                    } else if (charWithId.getGMLevel() > 0 && !c.getPlayer().isIntern()) {
                        c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.??????????????????));
                        return;
                    } else if (charWithId.getId() == c.getPlayer().getId()) {
                        c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.???????????????));
                        return;
                    }

                    // ??????????????????
                    BuddyAddResult buddyAddResult = null;
                    if (channel > 0) {
                        // ??????????????????

                        // ???????????????????????????????????????
                        buddyAddResult = World.Buddy.requestBuddyAdd(addName, -1, c.getPlayer().getId(),
                                c.getPlayer().getName(), c.getPlayer().getLevel(), c.getPlayer().getJob());
                    } else {
                        // ??????????????????
                        Connection con = ManagerDatabasePool.getConnection();

                        PreparedStatement ps;
                        ResultSet rs;

                        // ?????????????????????????????????
                        ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
                        ps.setInt(1, charWithId.getId());
                        ps.setInt(2, c.getPlayer().getId());
                        rs = ps.executeQuery();
                        if (rs.next()) {
                            buddyAddResult = BuddyAddResult.ALREADY_ON_LIST;
                        }
                        rs.close();
                        ps.close();

                        // ???????????????????????????????????????
                        if (buddyAddResult == null) {
                            // ??????????????????????????????????????????
                            ps = con.prepareStatement(
                                    "SELECT COUNT(*) as buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
                            ps.setInt(1, charWithId.getId());
                            rs = ps.executeQuery();

                            if (rs.next()) {
                                int count = rs.getInt("buddyCount");
                                if (count >= charWithId.getBuddyCapacity()) {
                                    buddyAddResult = BuddyAddResult.BUDDYLIST_FULL;
                                } else {
                                    buddyAddResult = BuddyAddResult.OK;
                                }
                            } else {
                                buddyAddResult = BuddyAddResult.NO;
                            }
                            rs.close();
                            ps.close();

                            // ?????????????????????????????????
                            if (buddyAddResult == BuddyAddResult.OK) {
                                ps = con.prepareStatement(
                                        "INSERT INTO buddies (`characterid`, `buddyid`, `pending`) VALUES (?, ?, 0)");
                                ps.setInt(1, charWithId.getId());
                                ps.setInt(2, c.getPlayer().getId());
                                ps.executeUpdate();
                            }
                        }
                    }

                    // ??????????????????
                    // ?????????????????????????????????
                    if (buddyAddResult == BuddyAddResult.BUDDYLIST_FULL) {
                        c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.???????????????));
                        // ????????????????????????
                    } else if (buddyAddResult == BuddyAddResult.NO) {
                        c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.???????????????));
                    } else {
                        BuddylistEntry buddy;
                        if (buddyAddResult == BuddyAddResult.OK) {
                            buddy = new BuddylistEntry(charWithId.getName(), charWithId.getId(), groupName, -1, true);
                        } else {
                            buddy = new BuddylistEntry(charWithId.getName(), charWithId.getId(), groupName, channel,
                                    true);
                            if (channel > 0) {
                                notifyRemoteChannel(c, charWithId.getId(), BuddyOperation.????????????);
                            }
                        }
                        buddylist.put(buddy);
                        c.getSession().writeAndFlush(BuddyListPacket.addBuddy(buddy));
                        // ???charWithId.getName()????????????????????????
                        c.getSession().writeAndFlush(
                                BuddyListPacket.buddylistPrompt(BuddyOperation.????????????, charWithId.getName()));
                    }
                    ManagerDatabasePool.closeConnection(null);
                } catch (SQLException e) {
                    System.err.println("SQL THROW" + e);
                }
            }
        } else if (mode == 2) { // ??????????????????
            int otherCid = slea.readInt();
            if (slea.available() > 0) {
                if (c.getPlayer().isShowErr()) {
                    c.getPlayer().showInfo("????????????", true, "????????????????????? " + slea.toString());
                }
            }

            // ??????????????????????????????????????????
            final BuddylistEntry ble = buddylist.get(otherCid);

            // ??????!??????ID??????????????????????????????
            if (ble == null) {
                c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.????????????));
            } else if (ble.isVisible()) { // ?????????????????????
                c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.????????????));
            } else {
                final int channel = World.Find.findChannel(otherCid);
                if (channel > 0) {
                    notifyRemoteChannel(c, otherCid, BuddyOperation.????????????);
                } else {
                    try {
                        Connection con = ManagerDatabasePool.getConnection();
                        try (PreparedStatement ps = con.prepareStatement(
                                "UPDATE buddies SET pending = ? WHERE characterid = ? AND buddyid = ?")) {
                            ps.setInt(1, 0);
                            ps.setInt(2, otherCid);
                            ps.setInt(3, c.getPlayer().getId());
                            ps.execute();
                        }
                        ManagerDatabasePool.closeConnection(con);
                    } catch (SQLException ex) {
                        FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
                    }
                }
                ble.setVisible(true);
                ble.setChannel(channel);
                buddylist.put(ble);
                c.getSession().writeAndFlush(BuddyListPacket.addBuddy(ble));
            }
        } else if (mode == 4) { // ????????????
            final int otherCid = slea.readInt();
            if (slea.available() > 0) {
                if (c.getPlayer().isShowErr()) {
                    c.getPlayer().showInfo("????????????", true, "????????????????????? " + slea.toString());
                }
            }

            // ??????????????????????????????????????????
            final BuddylistEntry blz = buddylist.get(otherCid);

            // ??????!??????????????????????????????????????????
            if (blz == null) {
                c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.????????????));
            } else if (!blz.isVisible()) { // ????????????????????????????????????
                c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.???????????????));
            } else {
                notifyRemoteChannel(c, otherCid, BuddyOperation.????????????);
                buddylist.remove(otherCid);
                c.getSession().writeAndFlush(BuddyListPacket.deleteBuddy(otherCid));
            }
        } else if (mode == 6) { // ??????????????????
            int otherCid = slea.readInt();
            if (slea.available() > 0) {
                if (c.getPlayer().isShowErr()) {
                    c.getPlayer().showInfo("??????????????????", true, "????????????????????? " + slea.toString());
                }
            }

            // ??????????????????????????????????????????
            final BuddylistEntry blz = buddylist.get(otherCid);

            // ????????????????????????????????????????????? || ?????????????????????
            if (blz == null || blz.isVisible()) {
                c.getSession().writeAndFlush(BuddyListPacket.buddylistMessage(BuddyOperation.????????????));
            } else {
                // ????????????????????????????????????????????????
                buddylist.remove(otherCid);
                c.getSession().writeAndFlush(BuddyListPacket.deleteBuddy(otherCid));

                // ????????????????????????????????????
                MapleCharacter other = MapleCharacter.getOnlineCharacterById(otherCid);
                if (other != null) {
                    other.getBuddylist().remove(c.getPlayer().getId());
                    other.getClient().getSession().writeAndFlush(BuddyListPacket.deleteBuddy(c.getPlayer().getId()));
                    // c.getPlayer().getName()????????????????????????
                    other.getClient().getSession().writeAndFlush(
                            BuddyListPacket.buddylistPrompt(BuddyOperation.??????????????????, c.getPlayer().getName()));
                } else {
                    try {
                        Connection con = ManagerDatabasePool.getConnection();
                        try (PreparedStatement ps = con
                                .prepareStatement("DELETE FROM buddies WHERE characterid = ? AND buddyid = ?")) {
                            ps.setInt(1, otherCid);
                            ps.setInt(1, c.getPlayer().getId());
                            ps.executeUpdate();
                        }
                        ManagerDatabasePool.closeConnection(con);
                    } catch (SQLException ex) {
                        FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
                    }
                }
            }
        } else if (mode == 0xA) { // ??????????????????
            if (slea.available() > 0) {
                if (c.getPlayer().isShowErr()) {
                    c.getPlayer().showInfo("??????????????????", true, "????????????????????? " + slea.toString());
                }
            }

            int capacity = c.getPlayer().getBuddyCapacity();
            if (capacity >= 100 || c.getPlayer().getMeso() < 50000) {
                c.getPlayer().dropMessage(1, "???????????????????????????????????????????????????????????????????????????????????????????????????100???????????????????????????????????????: " + capacity);
            } else {
                int newcapacity = capacity + 5;
                c.getPlayer().gainMeso(-50000, true, true);
                c.getPlayer().setBuddyCapacity((byte) newcapacity);
            }
        } else {
            System.err.println("???????????????????????????" + mode);
        }
    }

    private static void notifyRemoteChannel(final MapleClient c, final int otherCid, final BuddyOperation operation) {
        if (c == null) {
            return;
        }
        final MapleCharacter player = c.getPlayer();
        if (player == null) {
            return;
        }

        int ch = World.Find.findChannel(otherCid);
        if (ch > 0) {
            final MapleCharacter addChar = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(otherCid);
            if (addChar != null) {
                final BuddyList buddylist = addChar.getBuddylist();
                BuddylistEntry buddy = null;
                if (buddylist.contains(player.getId())) {
                    buddy = buddylist.get(player.getId());
                }
                switch (operation) {
                    case ????????????:
                        if (buddy != null) {
                            buddy.setVisible(true);
                            buddy.setChannel(c.getChannel());
                            buddylist.put(buddy);
                            addChar.getClient().getSession().writeAndFlush(BuddyListPacket.updateBuddyChannel(
                                    player.getId(), c.getChannel() - 1, addChar.getClient().getAccID()));
                        }
                        break;
                    case ????????????:
                        if (buddy != null) {
                            buddy.setChannel(-1);
                            buddylist.put(buddy);
                            addChar.getClient().getSession().writeAndFlush(
                                    BuddyListPacket.updateBuddyChannel(player.getId(), -1, addChar.getClient().getAccID()));
                        }
                        break;
                }
            }
        }
    }
}
