/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import handling.SendPacketOpcode;
import server.maps.MapleMap;
import tools.DateUtil;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author LEL
 */
public class EvolvingPacket {

    public static byte[] showEvolvingMessage(int action) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        // 24 00 1B 01 00
        pw.writeShort(SendPacketOpcode.LP_StatChanged.getValue());
        pw.writeShort(284);
        pw.write(action);
        return pw.getPacket();
    }

    public static byte[] partyCoreInfo(int[] core) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        // AF 00 /00 /48 EF 36 00 /D3 FB 36 00 /00 00 00 00 /00 00 00 00/ 00 00 00 00/
        // 00 00 00 00 /00 00 00 00 /32 F3 36 00 /00 00 00 00 /00 00 00 00
        pw.writeShort(SendPacketOpcode.LP_EvolvingResult.getValue());//
        pw.write(0);
        for (int i = 0; i < 10; i++) {
            pw.writeInt(core[i]);
        }
        return pw.getPacket();
    }

    public static byte[] showPartyConnect(MapleCharacter chr) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        pw.writeShort(SendPacketOpcode.LP_EvolvingResult.getValue());//
        pw.write(1);
        pw.write(1);
        pw.write(chr.getParty().getLeader().getId() == chr.getId() ? 1 : 0);
        return pw.getPacket();
    }

    public static byte[] connectCancel() {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        pw.writeShort(SendPacketOpcode.LP_EvolvingResult.getValue());//
        pw.writeShort(1);
        return pw.getPacket();
    }

    public static byte[] rewardCore(int itemid, int position) {
        // AF 00 02 01 00 00 00 00 00 D0 F2 36 00 01 00 00 00
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        pw.writeShort(SendPacketOpcode.LP_EvolvingResult.getValue());//
        pw.write(2); // 슬롯?
        pw.write(1);
        pw.writeInt(0);
        pw.write(position);
        pw.writeInt(itemid);
        pw.writeInt(1);
        return pw.getPacket();
    }

    public static byte[] showRewardCore(int itemid) {
        // 24 00 1D 16 D0 F2 36 00 01 00 00 00
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        pw.writeShort(SendPacketOpcode.LP_StatChanged.getValue());
        pw.writeShort(5662);
        pw.writeInt(itemid);
        pw.writeInt(1);
        return pw.getPacket();
    }

    public static byte[] moveCore(byte equip, byte slot, byte move, byte to) {
        // AF 00 03 00 01 02 01 03
        // AF 00 03 00 01 03 01 04
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        pw.writeShort(SendPacketOpcode.LP_EvolvingResult.getValue());//
        pw.write(3);
        pw.write(0);
        pw.write(equip);
        pw.write(slot);
        pw.write(move);
        pw.write(to);
        return pw.getPacket();
    }

    public static byte[] dropCore(byte position, short quantity) {
        // AF 00 04 01 /00 /01 00 /00 00
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        pw.writeShort(SendPacketOpcode.LP_EvolvingResult.getValue());//
        pw.write(4);
        pw.write(1);
        pw.write(position);
        pw.writeShort(quantity);// 1
        pw.writeShort(0);
        return pw.getPacket();
    }

    public static byte[] EvolvingWarpToMap(MapleMap to, int spawnPoint, MapleCharacter chr) {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();
        pw.writeShort(SendPacketOpcode.LP_SetField.getValue());
        EvolvingWarpToMapInfo(pw, chr, to, spawnPoint);
        pw.writeInt(100);
        pw.write(1);
        pw.writeInt(0);
        pw.writeMapleAsciiString("bgm");
        pw.writeInt(0);
        pw.writeShort(0);
        return pw.getPacket();
    }

    public static void EvolvingWarpToMapInfo(MaplePacketLittleEndianWriter pw, MapleCharacter player, MapleMap map,
            int sp) {
        pw.writeLong(player.getClient().getChannel());
        pw.write(0);
        pw.write(2);
        pw.writeInt(0);
        pw.writeInt(0);
        pw.writeInt(map.getId()); // 957010001
        pw.write(sp);
        pw.writeInt(player.getStat().getHp());
        pw.writeShort(0);
        pw.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
    }

    public static byte[] spawnEvolvingMonster() {
        MaplePacketLittleEndianWriter pw = new MaplePacketLittleEndianWriter();

        return pw.getPacket();
    }
}
