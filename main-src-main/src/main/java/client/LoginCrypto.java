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
package client;

import constants.ServerConstants;
import java.util.Random;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import tools.HexTool;

public class LoginCrypto {

    protected final static int extralength = 6;
    private final static String[] Alphabet = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
        "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private final static String[] Number = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private final static Random rand = new Random();
    private static KeyFactory RSAKeyFactory;

    public static final String Generate_13DigitAsiasoftPassport() {
        StringBuilder sb = new StringBuilder();
        sb.append(Alphabet[rand.nextInt(Alphabet.length)]); // First Letter

        for (int i = 0; i < 11; i++) {
            sb.append(Number[rand.nextInt(Number.length)]); // 11 Numbers
        }
        sb.append(Alphabet[rand.nextInt(Alphabet.length)]); // Last Letter

        return sb.toString();
    }

    private static final String toSimpleHexString(final byte[] bytes) {
        return HexTool.toString(bytes).replace(" ", "").toLowerCase();
    }

    private static final String hashWithDigest(final String in, final String digest) {
        try {
            MessageDigest Digester = MessageDigest.getInstance(digest);
            Digester.update(in.getBytes("UTF-8"), 0, in.length());
            byte[] sha1Hash = Digester.digest();
            return toSimpleHexString(sha1Hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Hashing the password failed", ex);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
    }

    public static String hexMd5(String in) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException("Encoding the string failed", e);
        }

        try {
            byte[] byteArray = in.getBytes("UTF-8");
            byte[] md5Bytes = md5.digest(byteArray);
            StringBuilder hexValue = new StringBuilder();
            for (int i = 0; i < md5Bytes.length; i++) {
                int val = ((int) md5Bytes[i]) & 0xff;
                if (val < 16) {
                    hexValue.append("0");
                }
                hexValue.append(Integer.toHexString(val));
            }
            return hexValue.toString().toUpperCase();
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Encoding the string failed", ex);
        }
    }

    public static final boolean checkMd5Hash(final String hash, final String password) {
        return hash.equals(hexMd5(password));
    }

    public static final boolean checkRegCodeHash(final String hash, final String code) {
        String salt = String.valueOf(ServerConstants.MAPLE_TYPE.getType())
                + String.valueOf(ServerConstants.MAPLE_VERSION);
        try {
            return checkMd5Hash(code, (toSimpleHexString(salt.getBytes("UTF-8")) + hash));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Encoding the string failed", ex);
        }
    }

    public static final String hexSha1(final String in) {
        return hashWithDigest(in, "SHA-1");
    }

    private static final String hexSha512(final String in) {
        return hashWithDigest(in, "SHA-512");
    }

    public static final boolean checkSha1Hash(final String hash, final String password) {
        return hash.equals(hexSha1(password));
    }

    public static final boolean checkSaltedSha512Hash(final String hash, final String password, final String salt) {
        return hash.equals(makeSaltedSha512Hash(password, salt));
    }

    public static final String makeSaltedSha512Hash(final String password, final String salt) {
        return hexSha512(password + salt);
    }

    public static final String makeSalt() {
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        return toSimpleHexString(salt);
    }

    public final static String rand_s(final String in) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extralength; i++) {
            sb.append(
                    rand.nextBoolean() ? Alphabet[rand.nextInt(Alphabet.length)] : Number[rand.nextInt(Number.length)]);
        }
        return sb.toString() + in;
    }

    public final static String rand_r(final String in) {
        return in.substring(extralength, extralength + 128);
    }
}
