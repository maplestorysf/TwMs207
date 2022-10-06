package tools.data;

import constants.ServerConstants;
import handling.SendPacketOpcode;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import tools.FileoutputUtil;
import tools.HexTool;

/**
 * Writes a maplestory-packet little-endian stream of bytes.
 *
 * @author Frz
 */
public class MaplePacketLittleEndianWriter {

    private final ByteArrayOutputStream baos;
    private static final Charset CHARSET = ServerConstants.MAPLE_TYPE.getCharset();

    /**
     * Constructor - initializes this stream with a default size.
     */
    public MaplePacketLittleEndianWriter() {
        this(32);
    }

    /**
     * Constructor - initializes this stream with size <code>size</code>.
     *
     * @param size
     *            The size of the underlying stream.
     */
    public MaplePacketLittleEndianWriter(final int size) {
        this.baos = new ByteArrayOutputStream(size);
    }

    private void log(byte str) {
        log(HexTool.toString(str) + " ");
    }

    private void log(String str) {
        if (SendPacketOpcode.record) {
            FileoutputUtil.logToFile(FileoutputUtil.Packet_Record, str);
        }
    }

    private void baosWrite(byte b) {
        baos.write(b);
        log(b);
    }

    /**
     * Gets a <code>MaplePacket</code> instance representing this sequence of bytes.
     *
     * @return A <code>MaplePacket</code> with the bytes in this stream.
     */
    public final byte[] getPacket() {
        return baos.toByteArray();
    }

    /**
     * Changes this packet into a human-readable hexadecimal stream of bytes.
     *
     * @return This packet as hex digits.
     */
    @Override
    public final String toString() {
        return HexTool.toString(baos.toByteArray());
    }

    /**
     * Write the number of zero bytes
     *
     * @param i
     */
    public final void writeZeroBytes(final int i) {
        for (int x = 0; x < i; x++) {
            baosWrite((byte) 0);
        }
        log("(長度:" + i + ")");
        log("\r\n");
    }

    /**
     * Write an array of bytes to the stream.
     *
     * @param b
     *            The bytes to write.
     */
    public final void write(final byte[] b) {
        for (int x = 0; x < b.length; x++) {
            baosWrite(b[x]);
        }
        log("(長度:" + b.length + ")");
        log("\r\n");
    }

    /**
     * Write a byte to the stream.
     *
     * @param b
     *            The byte to write.
     */
    public final void write(final byte b) {
        baosWrite(b);
        log("\r\n");
    }

    public final void write(final int b) {
        baosWrite((byte) b);
        log("\r\n");
    }

    public final void write(final boolean b) {
        baosWrite((byte) (b ? 1 : 0));
        log("\r\n");
    }

    /**
     * Write a short integer to the stream.
     *
     * @param i
     *            The short integer to write.
     */
    public final void writeShort(final int i) {
        baosWrite((byte) (i & 0xFF));
        baosWrite((byte) ((i >>> 8) & 0xFF));
        log("\r\n");
    }

    /**
     * Writes an integer to the stream.
     *
     * @param i
     *            The integer to write.
     */
    public final void writeInt(final int i) {
        baosWrite((byte) (i & 0xFF));
        baosWrite((byte) ((i >>> 8) & 0xFF));
        baosWrite((byte) ((i >>> 16) & 0xFF));
        baosWrite((byte) ((i >>> 24) & 0xFF));
        log("\r\n");
    }

    public void writeReversedInt(long l) {
        baosWrite((byte) (int) (l >>> 32 & 0xFF));
        baosWrite((byte) (int) (l >>> 40 & 0xFF));
        baosWrite((byte) (int) (l >>> 48 & 0xFF));
        baosWrite((byte) (int) (l >>> 56 & 0xFF));
        log("\r\n");
    }

    /**
     * Writes an ASCII string the the stream.
     *
     * @param s
     *            The ASCII string to write.
     */
    public final void writeAsciiString(String s) {
        write(s.getBytes(CHARSET));
    }

    public final void writeAsciiString(String s, final int max) {
        String string = getLimitedString(s, max);
        writeAsciiString(string);
        writeZeroBytes(max - string.getBytes(CHARSET).length);
    }

    /**
     * Writes a maple-convention ASCII string to the stream.
     *
     * @param s
     *            The ASCII string to use maple-convention to write.
     */
    public final void writeMapleAsciiString(String s) {
        String string = getLimitedString(s, Short.MAX_VALUE);
        writeShort(Math.min(string.getBytes(CHARSET).length, Short.MAX_VALUE));
        writeAsciiString(string);
    }

    private String getLimitedString(String s, final int max) {
        String string = "";
        int stringLength = 0;
        String tempString;
        for (char c : s.toCharArray()) {
            tempString = String.valueOf(c);
            stringLength += tempString.getBytes(CHARSET).length;
            if (stringLength <= max) {
                string += tempString;
            } else {
                break;
            }
        }
        return string;
    }

    public final void writeBoolean(boolean b) {
        baosWrite((byte) (b ? 1 : 0));
        log("\r\n");
    }

    /**
     * Writes a 2D 4 byte position information
     *
     * @param s
     *            The Point position to write.
     */
    public final void writePos(final Point s) {
        writeShort(s.x);
        writeShort(s.y);
        log("\r\n");
    }

    public final void writeRect(final Rectangle s) {
        writeInt(s.x);
        writeInt(s.y);
        writeInt(s.x + s.width);
        writeInt(s.y + s.height);
        log("\r\n");
    }

    /**
     * Write a long integer to the stream.
     *
     * @param l
     *            The long integer to write.
     */
    public final void writeLong(final long l) {
        baosWrite((byte) (l & 0xFF));
        baosWrite((byte) ((l >>> 8) & 0xFF));
        baosWrite((byte) ((l >>> 16) & 0xFF));
        baosWrite((byte) ((l >>> 24) & 0xFF));
        baosWrite((byte) ((l >>> 32) & 0xFF));
        baosWrite((byte) ((l >>> 40) & 0xFF));
        baosWrite((byte) ((l >>> 48) & 0xFF));
        baosWrite((byte) ((l >>> 56) & 0xFF));
        log("\r\n");
    }

    public final void writeReversedLong(final long l) {
        baosWrite((byte) ((l >>> 32) & 0xFF));
        baosWrite((byte) ((l >>> 40) & 0xFF));
        baosWrite((byte) ((l >>> 48) & 0xFF));
        baosWrite((byte) ((l >>> 56) & 0xFF));
        baosWrite((byte) (l & 0xFF));
        baosWrite((byte) ((l >>> 8) & 0xFF));
        baosWrite((byte) ((l >>> 16) & 0xFF));
        baosWrite((byte) ((l >>> 24) & 0xFF));
        log("\r\n");
    }

    public final void writeFile(final File file) {
        byte[] bytes = new byte[0];
        if (file != null && file.exists()) {
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                System.err.println("檔案太大");
            } else {
                bytes = new byte[(int) length];
                int offset = 0;
                int numRead = 0;
                try (InputStream is = new FileInputStream(file)) {
                    while ((offset < bytes.length)
                            && ((numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)) {
                        offset += numRead;
                    }
                } catch (IOException e) {
                    System.err.println("讀取檔案失敗:" + e);
                    bytes = new byte[0];
                }
                if (offset < bytes.length) {
                    System.err.println("無法完整讀取檔案:" + file.getName());
                    bytes = new byte[0];
                }
            }
        }
        writeInt(bytes.length);
        write(bytes);
    }

}
