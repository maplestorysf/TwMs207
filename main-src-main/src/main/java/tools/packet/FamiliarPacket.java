/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import client.familiar.MonsterFamiliar;
import handling.SendPacketOpcode;
import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import server.movement.LifeMovementFragment;
import tools.HexTool;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Weber
 */
public class FamiliarPacket {

    public static byte[] showFamiliarCard(MapleCharacter player, HashMap<Integer, Integer> data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(player.getId());
        writeFamiliarData(mplew, null, player, 2, 0x94000001, data, false);
        writeFamiliarData(mplew, null, player, 4, 0x94000001, data, false);
        return mplew.getPacket();
    }

    public static byte[] addFamiliarCard(MapleCharacter player, MonsterFamiliar mf) {
        tools.data.MaplePacketLittleEndianWriter mplew = new tools.data.MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        TreeMap<Integer, Integer> data = new TreeMap<>();
        mplew.writeInt(player.getId());
        data.put(2, 1);
        writeFamiliarData(mplew, mf, player, 3, 0x94000001, data, true);
        return mplew.getPacket();
    }

    public static void writeFamiliarData(MaplePacketLittleEndianWriter mplew, MonsterFamiliar mf, MapleCharacter player,
            int mode, int unkMask, Map<Integer, Integer> cards, boolean b) {
        mplew.write(mode);
        switch (mode) {
        case 4:
            mplew.write(cards.size() * 2);
            for (Map.Entry<Integer, Integer> entry : cards.entrySet()) {
                mplew.writeInt(entry.getKey());
                mplew.write(entry.getValue());
                mplew.writeInt(0);
            }
            break;
        case 3: {
            mplew.writeInt(unkMask);
            mplew.writeShort(cards.size());
            for (Map.Entry<Integer, Integer> entry : cards.entrySet()) {
                writeFamiliarCard(mplew, entry.getKey(), entry.getValue(), player, mf);
            }
            break;
        }
        case 2: {
            mplew.writeInt(unkMask);
            if ((unkMask & 1) == 1) {
                return;
            }
            mplew.write(0);
        }
        case 1: {
            mplew.writeInt(unkMask);
            break;
        }
        case 0: {
            mplew.write(HexTool.getByteArrayFromHexString("24 31 7b 25"));
            mplew.writeInt(unkMask);
            mf.writeRegisterPacket(mplew, b);
            mplew.write(0);
            mplew.write(0);
            mplew.writeInt(2000);
            mplew.writeInt(2000);
        }
        }
    }

    public static void writeFamiliarCard(MaplePacketLittleEndianWriter mplew, int type, int unk, MapleCharacter player,
            MonsterFamiliar mf) {
        mplew.write(type);
        switch (type) {
        case 6: {
            mplew.writeInt(3);
            mplew.writeInt(unk);
            if (unk != 3) {
                if (unk != 1) {
                    return;
                }
                mplew.write(0);
                mplew.writeShort(0);
                break;
            } else {
                int i1 = 0;
                int i2 = 0;
                while (i1 < 3) {
                    i1 = i2 + 1;
                    mplew.write(i2);
                    mplew.writeShort(0);
                    i2 = i1;
                }
                break;
            }
        }
        case 4: {
            mplew.writeShort(unk);
            break;
        }
        case 2: {
            mplew.writeInt(player.getFamiliars().size());
            if (player.getFamiliars().size() <= 0) {
                break;
            }
            mplew.writeInt(unk);
            if (unk == 1 && player.getFamiliars().size() != unk) {
                mplew.write(mf.getIndex() * 2);
                mf.writeRegisterPacket(mplew, true);
                break;
            }
            int index = 0;
            for (Map.Entry<Integer, MonsterFamiliar> sub : player.getFamiliars().entrySet()) {
                sub.getValue().setIndex(index);
                mplew.write(index * 2);
                sub.getValue().writeRegisterPacket(mplew, true);
                index += 1;
            }
            break;
        }

        case 0: {
            if ((unk & 1) != 0) {
                unk = unk + -1;
            }
            mplew.writeInt(unk | 3);
        }
        }
    }

    public static byte[] showAllFamiliar(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(chr.getId());
        Map<Integer, Integer> data = new HashMap<>();
        data.put(2, chr.getFamiliars().size());
        writeFamiliarData(mplew, null, chr, 3, 0x94000001, data, false);
        return mplew.getPacket();
    }

    public static byte[] H(MapleCharacter chr, int i) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(chr.getId());
        Map<Integer, Integer> data = new HashMap<>();
        data.put(4, i);
        writeFamiliarData(mplew, null, chr, 3, 0x94000001, data, false);
        return mplew.getPacket();
    }

    public static byte[] getWarpToMap(MapleCharacter chr, boolean b) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeInt(3);
        mplew.writeInt(0x5A267D83);
        mplew.writeInt(0x54000002);
        mplew.writeInt(0x66);
        mplew.writeInt(0x00030000);
        mplew.writeInt(0x00030000);
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 E0 FD 3B 37 4F 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"));
        mplew.writeInt(0x52FB1682);
        mplew.writeInt(0x54000001);

        if (chr.getFamiliars().size() > 0) {
            mplew.writeInt(chr.getFamiliars().size() * 56);
            mplew.writeInt(0);
            mplew.write(chr.getFamiliars().size() * 2);
            chr.getFamiliars().values().forEach((mf) -> {
                mf.writeRegisterPacket(mplew, false);
            });
            mplew.writeShort(0);
            mplew.write(6);
        } else {
            mplew.writeInt(0x0C);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.writeShort(0);
            mplew.write(0);
        }
        mplew.writeInt(0x000000C8);
        mplew.writeInt(0x761124F7);
        mplew.writeInt(0x54000000);
        mplew.writeInt(0x0C);
        mplew.writeInt(0x54000001);
        mplew.writeInt(0x54000002);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] updateFamiliar(MapleCharacter chr) {
        int i = 0;
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        Map<Integer, Integer> data = new TreeMap<>();
        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(chr.getId());
        if (chr.getSummonedFamiliar() == null) {
            i = 0x95000001;
        } else {
            data.put(0, 0x93000001);
            writeFamiliarData(mplew, chr.getSummonedFamiliar(), chr, 0, 0x93000001, null, true);
            i = 0x93000001;
        }
        if ((i & 2) != 0) {
            i = i + -2;
        }
        data.put(2, chr.getFamiliars().size());
        data.put(4, 0);
        data.put(6, 3);
        tools.packet.FamiliarPacket.writeFamiliarData(mplew, null, chr, 3, i | 1, data, false);
        return mplew.getPacket();
    }

    public static byte[] removeFamiliar(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(cid);
        Map<Integer, Integer> data = new HashMap<>();
        data.put(0, 0x0);
        writeFamiliarData(mplew, null, null, 1, 0x94000003, data, false);
        writeFamiliarData(mplew, null, null, 3, 0x94000001, data, false);
        return mplew.getPacket();
    }

    public static byte[] spawnFamiliar(MonsterFamiliar mf, boolean spawn, boolean respawn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(mf.getCharacterId());
        Map<Integer, Integer> data = new HashMap<>();
        data.put(0, 0x94000001);
        writeFamiliarData(mplew, mf, null, 0, 0x94000003, data, false);
        writeFamiliarData(mplew, mf, null, 3, 0x94000001, data, false);
        return mplew.getPacket();
    }

    public static byte[] moveFamiliar(int cid, int duration, Point mPos, Point oPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(cid);
        mplew.write(2);
        mplew.writeInt(0x14000003);
        mplew.writeZeroBytes(3);
        PacketHelper.serializeMovementList(mplew, duration, mPos, oPos, moves);
        return mplew.getPacket();
    }

    public static byte[] touchFamiliar(int cid, byte unk, int objectid, int type, int delay, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(cid);
        mplew.write(0);
        mplew.write(unk);
        mplew.writeInt(objectid);
        mplew.writeInt(type);
        mplew.writeInt(delay);
        mplew.writeInt(damage);

        return mplew.getPacket();
    }

    public static byte[] familiarAttack(int cid, byte unk, List<Triple<Integer, Integer, List<Integer>>> attackPair) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(cid);
        mplew.write(0);// familiar id?
        mplew.write(unk);
        mplew.write(attackPair.size());
        for (Triple<Integer, Integer, List<Integer>> s : attackPair) {
            mplew.writeInt(s.left);
            mplew.write(s.mid);
            mplew.write(s.right.size());
            for (int damage : s.right) {
                mplew.writeInt(damage);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] renameFamiliar(MonsterFamiliar mf) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(mf.getCharacterId());
        mplew.write(0);
        mplew.writeInt(mf.getFamiliar());
        mplew.writeMapleAsciiString(mf.getName());

        return mplew.getPacket();
    }

    public static byte[] attackFamiliar(int charid, int unk, Map<Integer, Integer> attackInfo) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.writeInt(charid);
        mplew.write(2);
        mplew.writeInt(unk);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.write(attackInfo.size() * 2);
        for (Map.Entry<Integer, Integer> entry : attackInfo.entrySet()) {
            mplew.writeInt(entry.getKey());
            mplew.write(2);
            mplew.writeInt(entry.getValue());
        }
        return mplew.getPacket();
    }

    public static byte[] bl(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_FamiliarOperation.getValue());
        mplew.write(2);
        mplew.writeInt(0x14000003);
        mplew.write(3);
        return mplew.getPacket();
    }

}
