/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.shark;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

/**
 *
 * @author pungin
 */
public class SharkReader {

    private List<SharkPacket> stored = new ArrayList<>();
    private final static int SEVENBITS = 0x0000007f;
    private final static int SIGNBIT = 0x00000080;
    private int MapleSharkVersion = 0;
    public int mLocalPort = 0;
    public int mRemotePort = 0;
    public int mLocale = 0;
    public int mBuild = 0;
    public String mLocalEndpoint = "";
    public String mRemoteEndpoint = "";
    public String mPatchLocation = "";
    private final File file;
    private long pos = 0;

    public SharkReader(File file) {
        this.file = file;
        init();
    }

    public static byte[] getBytes(File file) {
        byte[] buffer = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
            byte[] b = new byte[(int) file.length()];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (Exception e) {
        }
        return buffer;
    }

    private int read7BitInt(final LittleEndianAccessor mplew) {
        int intValue = 0;

        int byteValue = mplew.readByte();
        intValue += (byteValue & SEVENBITS);
        if ((byteValue & SIGNBIT) == 0) {
            return intValue;
        }
        byteValue = mplew.readByte();
        intValue += ((byteValue & SEVENBITS) << 7);
        if ((byteValue & SIGNBIT) == 0) {
            return intValue;
        }
        byteValue = mplew.readByte();
        intValue += ((byteValue & SEVENBITS) << 14);
        if ((byteValue & SIGNBIT) == 0) {
            return intValue;
        }
        byteValue = mplew.readByte();
        intValue += ((byteValue & SEVENBITS) << 21);
        if ((byteValue & SIGNBIT) == 0) {
            return intValue;
        }
        byteValue = mplew.readByte();
        intValue += ((byteValue & SEVENBITS) << 28);
        return intValue;
    }

    private void init() {
        if (!file.exists()) {
            System.out.println("文件不存在");
        }
        final LittleEndianAccessor mplew = new LittleEndianAccessor(new ByteArrayByteStream(getBytes(file)));

        MapleSharkVersion = mplew.readUShort();
        if (MapleSharkVersion < 0x2000) {
            mLocalPort = mplew.readUShort();
        } else {
            byte v1 = (byte) ((MapleSharkVersion >> 12) & 0xF);
            byte v2 = (byte) ((MapleSharkVersion >> 8) & 0xF);
            byte v3 = (byte) ((MapleSharkVersion >> 4) & 0xF);
            byte v4 = (byte) (MapleSharkVersion & 0xF);
            System.out.println("Loading MSB file, saved by MapleShark V" + v1 + "." + v2 + "." + v3 + "." + v4);
            if (MapleSharkVersion == 0x2012) {
                mLocale = mplew.readUShort();
                mBuild = mplew.readUShort();
                mLocalPort = mplew.readUShort();
            } else if (MapleSharkVersion == 0x2014) {
                mLocalEndpoint = mplew.readAsciiString(read7BitInt(mplew));
                mLocalPort = mplew.readUShort();
                mRemoteEndpoint = mplew.readAsciiString(read7BitInt(mplew));
                mRemotePort = mplew.readUShort();

                mLocale = mplew.readUShort();
                mBuild = mplew.readUShort();
            } else if (MapleSharkVersion == 0x2015 || MapleSharkVersion >= 0x2020) {
                mLocalEndpoint = mplew.readAsciiString(read7BitInt(mplew));
                mLocalPort = mplew.readUShort();
                mRemoteEndpoint = mplew.readAsciiString(read7BitInt(mplew));
                mRemotePort = mplew.readUShort();

                mLocale = mplew.readByte();
                mBuild = mplew.readUShort();

                if (MapleSharkVersion >= 0x2021) {
                    mPatchLocation = mplew.readAsciiString(read7BitInt(mplew));
                }
            } else {
                System.out.println("I have no idea how to open this MSB file. It looks to me as a version " + v1 + "."
                        + v2 + "." + v3 + "." + v4 + " MapleShark MSB file... O.o?!");
                return;
            }
        }
        while (mplew.available() >= (MapleSharkVersion >= 0x2020 ? 13 : 12)) {
            long timestamp = mplew.readLong();
            long size = MapleSharkVersion < 0x2027 ? mplew.readUShort() : mplew.readUInt();
            int opcode = mplew.readUShort();
            boolean outbound;
            if (MapleSharkVersion >= 0x2020) {
                outbound = mplew.readByte() == 1;
            } else {
                outbound = (size & 0x8000) != 0;
                size = (size & 0x7FFF);
            }

            if (mplew.available() < size + (MapleSharkVersion >= 0x2025 ? 8 : 0)) {
                System.out.println("剩餘數據長度不正確");
                break;
            }

            byte[] info = mplew.read((int) size);

            int preDecodeIV = 0, postDecodeIV = 0;
            if (MapleSharkVersion >= 0x2025) {
                preDecodeIV = mplew.readInt();
                postDecodeIV = mplew.readInt();
            }
            stored.add(new SharkPacket(info, opcode, outbound, timestamp));
        }
    }

    public SharkPacket read() {
        if (available() <= 0) {
            System.out.println("剩餘數據包不足");
            return null;
        }
        return stored.get((int) (pos++));
    }

    public long available() {
        return stored.size() - pos;
    }
}
