package tools;

import constants.ServerConstants;
import constants.ServerConstants.MapleType;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class MapleAESOFB {

    private byte iv[];
    private Cipher cipher;
    private final short mapleVersion;

    public static enum EncryptionKey {

        GMS148((short) 148, MapleType.GLOBAL, new short[]{0x7E, 0x00, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00, 0xBD, 0x00, 0x00, 0x00, 0x79, 0x00, 0x00, 0x00, 0xA1, 0x00, 0x00, 0x00, 0xC6, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0xF3, 0x00, 0x00, 0x00}),
        GMS147((short) 147, MapleType.GLOBAL, new short[]{0x66, 0x00, 0x00, 0x00, 0x62, 0x00, 0x00, 0x00, 0x21, 0x00, 0x00, 0x00, 0x0E, 0x00, 0x00, 0x00, 0x2B, 0x00, 0x00, 0x00, 0xBC, 0x00, 0x00, 0x00, 0x39, 0x00, 0x00, 0x00, 0x7B, 0x00, 0x00, 0x00}),
        GMS146((short) 146, MapleType.GLOBAL, new short[]{0x8B, 0x00, 0x00, 0x00, 0x24, 0x00, 0x00, 0x00, 0x8B, 0x00, 0x00, 0x00, 0x6D, 0x00, 0x00, 0x00, 0xB5, 0x00, 0x00, 0x00, 0xC6, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0xB0, 0x00, 0x00, 0x00}),
        GMS145((short) 145, MapleType.GLOBAL, new short[]{0xF9, 0x00, 0x00, 0x00, 0x12, 0x00, 0x00, 0x00, 0x2B, 0x00, 0x00, 0x00, 0x9D, 0x00, 0x00, 0x00, 0x46, 0x00, 0x00, 0x00, 0x63, 0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0xAB, 0x00, 0x00, 0x00}),
        GMS144((short) 144, MapleType.GLOBAL, new short[]{0x46, 0x00, 0x00, 0x00, 0x3C, 0x00, 0x00, 0x00, 0xA3, 0x00, 0x00, 0x00, 0xB6, 0x00, 0x00, 0x00, 0x2F, 0x00, 0x00, 0x00, 0xAE, 0x00, 0x00, 0x00, 0x57, 0x00, 0x00, 0x00, 0xB7, 0x00, 0x00, 0x00}),
        GMS143((short) 143, MapleType.GLOBAL, new short[]{0xFA, 0x00, 0x00, 0x00, 0xE0, 0x00, 0x00, 0x00, 0x43, 0x00, 0x00, 0x00, 0xE8, 0x00, 0x00, 0x00, 0xC9, 0x00, 0x00, 0x00, 0x3F, 0x00, 0x00, 0x00, 0x72, 0x00, 0x00, 0x00, 0x92, 0x00, 0x00, 0x00}),
        GMS142((short) 142, MapleType.GLOBAL, new short[]{0x6D, 0x00, 0x00, 0x00, 0x23, 0x00, 0x00, 0x00, 0x13, 0x00, 0x00, 0x00, 0xE9, 0x00, 0x00, 0x00, 0xEE, 0x00, 0x00, 0x00, 0x27, 0x00, 0x00, 0x00, 0xA8, 0x00, 0x00, 0x00, 0xCF, 0x00, 0x00, 0x00}),
        GMS141((short) 141, MapleType.GLOBAL, new short[]{0x5C, 0x00, 0x00, 0x00, 0xC0, 0x00, 0x00, 0x00, 0xC0, 0x00, 0x00, 0x00, 0x86, 0x00, 0x00, 0x00, 0xEA, 0x00, 0x00, 0x00, 0x85, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x37, 0x00, 0x00, 0x00}),
        GMS140((short) 140, MapleType.GLOBAL, new short[]{0xCD, 0x00, 0x00, 0x00, 0x5C, 0x00, 0x00, 0x00, 0xDC, 0x00, 0x00, 0x00, 0x98, 0x00, 0x00, 0x00, 0xD8, 0x00, 0x00, 0x00, 0x1C, 0x00, 0x00, 0x00, 0x9A, 0x00, 0x00, 0x00, 0x47, 0x00, 0x00, 0x00}),
        CMS130((short) 130, MapleType.中国, new short[]{0x76, 0x00, 0x00, 0x00, 0xC9, 0x00, 0x00, 0x00, 0x49, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0xB1, 0x00, 0x00, 0x00, 0x11, 0x00, 0x00, 0x00, 0xD6, 0x00, 0x00, 0x00, 0x97, 0x00, 0x00, 0x00}),
        CMS129((short) 129, MapleType.中国, new short[]{0x5B, 0x00, 0x00, 0x00, 0x8F, 0x00, 0x00, 0x00, 0xE5, 0x00, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, 0x84, 0x00, 0x00, 0x00, 0xA7, 0x00, 0x00, 0x00, 0xEE, 0x00, 0x00, 0x00, 0x2F, 0x00, 0x00, 0x00}),
        CMS128((short) 128, MapleType.中国, new short[]{0x18, 0x00, 0x00, 0x00, 0x64, 0x00, 0x00, 0x00, 0x85, 0x00, 0x00, 0x00, 0xF8, 0x00, 0x00, 0x00, 0x96, 0x00, 0x00, 0x00, 0x16, 0x00, 0x00, 0x00, 0xD4, 0x00, 0x00, 0x00, 0xD0, 0x00, 0x00, 0x00}),
        CMS124((short) 124, MapleType.中国, new short[]{0xF6, 0x00, 0x00, 0x00, 0xE1, 0x00, 0x00, 0x00, 0xB0, 0x00, 0x00, 0x00, 0xB9, 0x00, 0x00, 0x00, 0x2C, 0x00, 0x00, 0x00, 0x99, 0x00, 0x00, 0x00, 0xB2, 0x00, 0x00, 0x00, 0x53, 0x00, 0x00, 0x00}),
        ;

        private final SecretKeySpec skey;
        private final short version;
        private final MapleType mapleType;
        private boolean tespia;

        EncryptionKey(short version, MapleType mapleType, short[] skeys) {
            byte[] skeyBytes = new byte[skeys.length];
            for (int i = 0; i < skeys.length; i++) {
                skeyBytes[i] = (byte) skeys[i];
            }
            this.skey = new SecretKeySpec(skeyBytes, "AES");
            this.version = version;
            this.mapleType = mapleType;
        }

        EncryptionKey(short version, MapleType mapleType, String keyBuffer) {
            byte[] keyBufferBytes = HexTool.getByteArrayFromHexString(keyBuffer);
            byte[] skeyBytes = new byte[keyBufferBytes.length];
            for (int i = 0; i < keyBufferBytes.length; i = i + 4) {
                skeyBytes[i] = keyBufferBytes[i];
                skeyBytes[i + 1] = 0;
                skeyBytes[i + 2] = 0;
                skeyBytes[i + 3] = 0;
            }
            this.skey = new SecretKeySpec(skeyBytes, "AES");
            this.version = version;
            this.mapleType = mapleType;
        }

        EncryptionKey(short version, MapleType mapleType, boolean tespia, short[] skeys) {
            byte[] skeyBytes = new byte[skeys.length];
            for (int i = 0; i < skeys.length; i++) {
                skeyBytes[i] = (byte) skeys[i];
            }
            this.skey = new SecretKeySpec(skeyBytes, "AES");
            this.version = version;
            this.mapleType = mapleType;
            this.tespia = tespia;
        }

        EncryptionKey(short version, MapleType mapleType, boolean tespia, String keyBuffer) {
            byte[] keyBufferBytes = HexTool.getByteArrayFromHexString(keyBuffer);
            byte[] skeyBytes = new byte[keyBufferBytes.length];
            for (int i = 0; i < keyBufferBytes.length; i = i + 4) {
                skeyBytes[i] = (byte) keyBufferBytes[i];
                skeyBytes[i + 1] = 0;
                skeyBytes[i + 2] = 0;
                skeyBytes[i + 3] = 0;
            }
            this.skey = new SecretKeySpec(skeyBytes, "AES");
            this.version = version;
            this.mapleType = mapleType;
            this.tespia = tespia;
        }

        public SecretKeySpec getEncryptionKey() {
            return skey;
        }

        public boolean isTespia() {
            return tespia;
        }

        public short getVersion() {
            return version;
        }

        public MapleType getMapleType() {
            if (!tespia) {
                return mapleType;
            }
            if (mapleType == MapleType.한국 || mapleType == MapleType.한국_TEST) {
                return MapleType.한국_TEST;
            } else {
                return MapleType.TESPIA;
            }
        }
    }

    private static String[] skeys = new String[] {
            // 0
            "2923BE84E16CD6AE529049F1F1BBE9EBB3A6DB3C870C3E99245E0D1C06B747DE",
            // 1
            "B3124DC843BB8BA61F035A7D0938251F5DD4CBFC96F5453B130D890A1CDBAE32",
            // 2
            "888138616B681262F954D0E7711748780D92291D86299972DB741CFA4F37B8B5",
            // 3
            "209A50EE407836FD124932F69E7D49DCAD4F14F2444066D06BC430B7323BA122",
            // 4
            "F622919DE18B1FDAB0CA9902B9729D492C807EC599D5E980B2EAC9CC53BF67D6",
            // 5
            "BF14D67E2DDC8E6683EF574961FF698F61CDD11E9D9C167272E61DF0844F4A77",
            // 6
            "02D7E8392C53CBC9121E33749E0CF4D5D49FD4A4597E35CF3222F4CCCFD3902D",
            // 7
            "48D38F75E6D91D2AE5C0F72B788187440E5F5000D4618DBE7B0515073B33821F",
            // 8
            "187092DA6454CEB1853E6915F8466A0496730ED9162F6768D4F74A4AD0576876",
            // 9
            "5B628A8A8F275CF7E5874A3B329B614084C6C3B1A7304A10EE756F032F9E6AEF",
            // 10
            "762DD0C2C9CD68D4496A792508614014B13B6AA51128C18CD6A90B87978C2FF1",
            // 11
            "10509BC8814329288AF6E99E47A18148316CCDA49EDE81A38C9810FF9A43CDCF",
            // 12
            "5E4EE1309CFED9719FE2A5E20C9BB44765382A4689A982797A7678C263B126DF",
            // 13
            "DA296D3E62E0961234BF39A63F895EF16D0EE36C28A11E201DCBC2033F410784",
            // 14
            "0F1405651B2861C9C5E72C8E463608DCF3A88DFEBEF2EB71FFA0D03B75068C7E",
            // 15
            "8778734DD0BE82BEDBC246412B8CFA307F70F0A754863295AA5B68130BE6FCF5",
            // 16
            "CABE7D9F898A411BFDB84F68F6727B1499CDD30DF0443AB4A66653330BCBA110",
            // 17
            "5E4CEC034C73E605B4310EAAADCFD5B0CA27FFD89D144DF4792759427C9CC1F8",
            // 18
            "CD8C87202364B8A687954CB05A8D4E2D99E73DB160DEB180AD0841E96741A5D5",
            // 19
            "9FE4189F15420026FE4CD12104932FB38F735340438AAF7ECA6FD5CFD3A195CE" };
    private static SecretKeySpec skey = new SecretKeySpec(new byte[] { (byte) 0x88, 0x00, 0x00, 0x00, (byte) 0x6B, 0x00,
            0x00, 0x00, (byte) 0xF9, 0x00, 0x00, 0x00, (byte) 0x71, 0x00, 0x00, 0x00, (byte) 0x0D, 0x00, 0x00, 0x00,
            (byte) 0x86, 0x00, 0x00, 0x00, (byte) 0xDB, 0x00, 0x00, 0x00, (byte) 0x4F, 0x00, 0x00, 0x00 }, "AES");
    // KMS
    // {0x13, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00, 0x2A, 0x00, 0x00, 0x00,
    // 0x5B, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x10,
    // 0x00, 0x00, 0x00, 0x60, 0x00, 0x00, 0x00}
    /* VANA */
    private static final byte[] funnyBytes = new byte[] { (byte) 0xEC, (byte) 0x3F, (byte) 0x77, (byte) 0xA4,
            (byte) 0x45, (byte) 0xD0, (byte) 0x71, (byte) 0xBF, (byte) 0xB7, (byte) 0x98, (byte) 0x20, (byte) 0xFC,
            (byte) 0x4B, (byte) 0xE9, (byte) 0xB3, (byte) 0xE1, (byte) 0x5C, (byte) 0x22, (byte) 0xF7, (byte) 0x0C,
            (byte) 0x44, (byte) 0x1B, (byte) 0x81, (byte) 0xBD, (byte) 0x63, (byte) 0x8D, (byte) 0xD4, (byte) 0xC3,
            (byte) 0xF2, (byte) 0x10, (byte) 0x19, (byte) 0xE0, (byte) 0xFB, (byte) 0xA1, (byte) 0x6E, (byte) 0x66,
            (byte) 0xEA, (byte) 0xAE, (byte) 0xD6, (byte) 0xCE, (byte) 0x06, (byte) 0x18, (byte) 0x4E, (byte) 0xEB,
            (byte) 0x78, (byte) 0x95, (byte) 0xDB, (byte) 0xBA, (byte) 0xB6, (byte) 0x42, (byte) 0x7A, (byte) 0x2A,
            (byte) 0x83, (byte) 0x0B, (byte) 0x54, (byte) 0x67, (byte) 0x6D, (byte) 0xE8, (byte) 0x65, (byte) 0xE7,
            (byte) 0x2F, (byte) 0x07, (byte) 0xF3, (byte) 0xAA, (byte) 0x27, (byte) 0x7B, (byte) 0x85, (byte) 0xB0,
            (byte) 0x26, (byte) 0xFD, (byte) 0x8B, (byte) 0xA9, (byte) 0xFA, (byte) 0xBE, (byte) 0xA8, (byte) 0xD7,
            (byte) 0xCB, (byte) 0xCC, (byte) 0x92, (byte) 0xDA, (byte) 0xF9, (byte) 0x93, (byte) 0x60, (byte) 0x2D,
            (byte) 0xDD, (byte) 0xD2, (byte) 0xA2, (byte) 0x9B, (byte) 0x39, (byte) 0x5F, (byte) 0x82, (byte) 0x21,
            (byte) 0x4C, (byte) 0x69, (byte) 0xF8, (byte) 0x31, (byte) 0x87, (byte) 0xEE, (byte) 0x8E, (byte) 0xAD,
            (byte) 0x8C, (byte) 0x6A, (byte) 0xBC, (byte) 0xB5, (byte) 0x6B, (byte) 0x59, (byte) 0x13, (byte) 0xF1,
            (byte) 0x04, (byte) 0x00, (byte) 0xF6, (byte) 0x5A, (byte) 0x35, (byte) 0x79, (byte) 0x48, (byte) 0x8F,
            (byte) 0x15, (byte) 0xCD, (byte) 0x97, (byte) 0x57, (byte) 0x12, (byte) 0x3E, (byte) 0x37, (byte) 0xFF,
            (byte) 0x9D, (byte) 0x4F, (byte) 0x51, (byte) 0xF5, (byte) 0xA3, (byte) 0x70, (byte) 0xBB, (byte) 0x14,
            (byte) 0x75, (byte) 0xC2, (byte) 0xB8, (byte) 0x72, (byte) 0xC0, (byte) 0xED, (byte) 0x7D, (byte) 0x68,
            (byte) 0xC9, (byte) 0x2E, (byte) 0x0D, (byte) 0x62, (byte) 0x46, (byte) 0x17, (byte) 0x11, (byte) 0x4D,
            (byte) 0x6C, (byte) 0xC4, (byte) 0x7E, (byte) 0x53, (byte) 0xC1, (byte) 0x25, (byte) 0xC7, (byte) 0x9A,
            (byte) 0x1C, (byte) 0x88, (byte) 0x58, (byte) 0x2C, (byte) 0x89, (byte) 0xDC, (byte) 0x02, (byte) 0x64,
            (byte) 0x40, (byte) 0x01, (byte) 0x5D, (byte) 0x38, (byte) 0xA5, (byte) 0xE2, (byte) 0xAF, (byte) 0x55,
            (byte) 0xD5, (byte) 0xEF, (byte) 0x1A, (byte) 0x7C, (byte) 0xA7, (byte) 0x5B, (byte) 0xA6, (byte) 0x6F,
            (byte) 0x86, (byte) 0x9F, (byte) 0x73, (byte) 0xE6, (byte) 0x0A, (byte) 0xDE, (byte) 0x2B, (byte) 0x99,
            (byte) 0x4A, (byte) 0x47, (byte) 0x9C, (byte) 0xDF, (byte) 0x09, (byte) 0x76, (byte) 0x9E, (byte) 0x30,
            (byte) 0x0E, (byte) 0xE4, (byte) 0xB2, (byte) 0x94, (byte) 0xA0, (byte) 0x3B, (byte) 0x34, (byte) 0x1D,
            (byte) 0x28, (byte) 0x0F, (byte) 0x36, (byte) 0xE3, (byte) 0x23, (byte) 0xB4, (byte) 0x03, (byte) 0xD8,
            (byte) 0x90, (byte) 0xC8, (byte) 0x3C, (byte) 0xFE, (byte) 0x5E, (byte) 0x32, (byte) 0x24, (byte) 0x50,
            (byte) 0x1F, (byte) 0x3A, (byte) 0x43, (byte) 0x8A, (byte) 0x96, (byte) 0x41, (byte) 0x74, (byte) 0xAC,
            (byte) 0x52, (byte) 0x33, (byte) 0xF0, (byte) 0xD9, (byte) 0x29, (byte) 0x80, (byte) 0xB1, (byte) 0x16,
            (byte) 0xD3, (byte) 0xAB, (byte) 0x91, (byte) 0xB9, (byte) 0x84, (byte) 0x7F, (byte) 0x61, (byte) 0x1E,
            (byte) 0xCF, (byte) 0xC5, (byte) 0xD1, (byte) 0x56, (byte) 0x3D, (byte) 0xCA, (byte) 0xF4, (byte) 0x05,
            (byte) 0xC6, (byte) 0xE5, (byte) 0x08, (byte) 0x49 };

    /**
     * Class constructor - Creates an instance of the MapleStory encryption cipher.
     *
     * @param iv
     *            The 4-byte IV to use.
     * @param mapleVersion
     */
    public MapleAESOFB(byte iv[], short mapleVersion) {
        boolean hasKeyChanger = false;
        for (EncryptionKey encryptkey : EncryptionKey.values()) {
            if (encryptkey.getMapleType() != ServerConstants.MAPLE_TYPE
                    || encryptkey.isTespia() != ServerConstants.TESPIA) {
                continue;
            }
            if (ServerConstants.MAPLE_VERSION >= encryptkey.getVersion()) {
                skey = encryptkey.getEncryptionKey();
                hasKeyChanger = true;
                break;
            }
        }
        // TwMS規律金鑰處理
        if (!hasKeyChanger && ServerConstants.MAPLE_VERSION >= 176 && ServerConstants.MAPLE_TYPE == MapleType.台灣) {
            int keyChangerIndex = ServerConstants.MAPLE_VERSION - (int) (ServerConstants.MAPLE_VERSION / 20) * 20;
            byte[] keyBufferBytes = HexTool.getByteArrayFromHexString(skeys[keyChangerIndex]);
            byte[] skeyBytes = new byte[keyBufferBytes.length];
            for (int i = 0; i < keyBufferBytes.length; i = i + 4) {
                skeyBytes[i] = (byte) keyBufferBytes[i];
                skeyBytes[i + 1] = 0;
                skeyBytes[i + 2] = 0;
                skeyBytes[i + 3] = 0;
            }
            // System.out.println(HexTool.toString(skeyBytes));
            skey = new SecretKeySpec(skeyBytes, "AES");
        }

        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            System.err.println("啟動加密計算工具錯誤:" + e);
        }

        this.setIv(iv);
        this.mapleVersion = (short) (((mapleVersion >> 8) & 0xFF) | ((mapleVersion << 8) & 0xFF00));
    }

    /**
     * Sets the IV of this instance.
     *
     * @param iv
     *            The new IV.
     */
    private void setIv(byte[] iv) {
        this.iv = iv;
    }

    /**
     * For debugging/testing purposes only.
     *
     * @return The IV.
     */
    public byte[] getIv() {
        return this.iv;
    }

    /**
     * Encrypts <code>data</code> and generates a new IV.
     *
     * @param data
     *            The bytes to encrypt.
     * @return The encrypted bytes.
     */
    public byte[] crypt(byte[] data) {
        int remaining = data.length;
        int llength = 0x5B0;
        int start = 0;

        try {
            while (remaining > 0) {
                byte[] myIv = BitTools.multiplyBytes(this.iv, 4, 4);
                if (remaining < llength) {
                    llength = remaining;
                }
                for (int x = start; x < (start + llength); x++) {
                    if ((x - start) % myIv.length == 0) {
                        byte[] newIv = cipher.doFinal(myIv);
                        System.arraycopy(newIv, 0, myIv, 0, myIv.length);
                    }
                    data[x] ^= myIv[(x - start) % myIv.length];
                }
                start += llength;
                remaining -= llength;
                llength = 0x5B4;
            }
            updateIv();
        } catch (IllegalBlockSizeException | BadPaddingException e) {
        }
        return data;
    }

    public byte[] encryptEx(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (data[i] + this.iv[0]);
        }
        this.updateIv();
        return data;
    }

    /**
     * Generates a new IV.
     */
    private void updateIv() {
        byte[] pOldIV = this.iv;
        byte[] newIV = { (byte) 0xF2, 0x53, (byte) 0x50, (byte) 0xC6 };
        for (int i = 0; i < 4; i++)
            funnyShit(pOldIV[i], newIV);

        System.arraycopy(newIV, 0, iv, 0, iv.length);
    }

    /**
     * Generates a packet header for a packet that is <code>length</code> long.
     *
     * @param length
     *            How long the packet that this header is for is.
     * @return The header.
     */
    public byte[] getPacketHeader(int length) {
        int iiv = (((iv[3]) & 0xFF) | ((iv[2] << 8) & 0xFF00)) ^ mapleVersion;
        int mlength = (((length << 8) & 0xFF00) | (length >>> 8)) ^ iiv;

        return new byte[] { (byte) ((iiv >>> 8) & 0xFF), (byte) (iiv & 0xFF), (byte) ((mlength >>> 8) & 0xFF), (byte) (mlength & 0xFF) };
    }

    /**
     * Gets the packet length from a header.
     *
     * @param packetHeader
     *            The header as an integer.
     * @return The length of the packet.
     */
    public static int getPacketLength(int packetHeader) {
        int packetLength = ((packetHeader >>> 16) ^ (packetHeader & 0xFFFF));
        packetLength = ((packetLength << 8) & 0xFF00) | ((packetLength >>> 8) & 0xFF); // fix endianness
        return packetLength;
    }

    /**
     * Check the packet to make sure it has a header.
     *
     * @param packet
     *            The packet to check.
     * @return <code>True</code> if the packet has a correct header,
     *         <code>false</code> otherwise.
     */
    public boolean checkPacket(byte[] packet) {
        return ((((packet[0] ^ iv[2]) & 0xFF) == ((mapleVersion >> 8) & 0xFF)) && (((packet[1] ^ iv[3]) & 0xFF) == (mapleVersion & 0xFF)));
    }

    /**
     * Check the header for validity.
     *
     * @param packetHeader
     *            The packet header to check.
     * @return <code>True</code> if the header is correct, <code>false</code>
     *         otherwise.
     */
    public boolean checkPacket(int packetHeader) {
        return checkPacket(new byte[] { (byte) ((packetHeader >> 24) & 0xFF), (byte) ((packetHeader >> 16) & 0xFF) });
    }

    /**
     * Returns the IV of this instance as a string.
     *
     * @return
     */
    @Override
    public String toString() {
        return "IV: " + HexTool.toString(this.iv);
    }

    /**
     * Does funny stuff. <code>this.OldIV</code> must not equal <code>in</code>
     * Modifies <code>in</code> and returns it for convenience.
     *
     * @param inputByte
     *            The byte to apply the funny stuff to.
     * @param in
     *            Something needed for all this to occur.
     */
    public static final void funnyShit(byte inputByte, byte[] in) {
        byte a = in[1];
        byte b = inputByte;
        byte c = funnyBytes[(int) a & 0xFF];
        c -= inputByte;
        in[0] += c;
        c = in[2];
        c ^= funnyBytes[(int) b & 0xFF];
        a -= (int) c & 0xFF;
        in[1] = a;
        a = in[3];
        c = a;
        a -= (int) in[0] & 0xFF;
        c = funnyBytes[(int) c & 0xFF];
        c += inputByte;
        c ^= in[2];
        in[2] = c;
        a += (int) funnyBytes[(int) b & 0xFF] & 0xFF;
        in[3] = a;

        int d = ((int) in[0]) & 0xFF;
        d |= (in[1] << 8) & 0xFF00;
        d |= (in[2] << 16) & 0xFF0000;
        d |= (in[3] << 24) & 0xFF000000;
        int ret_value = d >>> 0x1d;
        d <<= 3;
        ret_value |= d;

        in[0] = (byte) (ret_value & 0xFF);
        in[1] = (byte) ((ret_value >> 8) & 0xFF);
        in[2] = (byte) ((ret_value >> 16) & 0xFF);
        in[3] = (byte) ((ret_value >> 24) & 0xFF);
    }
}
