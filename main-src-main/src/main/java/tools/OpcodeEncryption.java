/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import handling.RecvPacketOpcode;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;
import server.Randomizer;

/**
 *
 * @author Weber
 */
public class OpcodeEncryption {

    public byte[] aKey = new byte[24];
    public Key pKey;
    private Map<Integer, Integer> mOpcodeMappingTable;

    public OpcodeEncryption(byte[] key) {
        System.arraycopy(key, 0, this.aKey, 0, aKey.length);
        System.out.println("Key = " + HexTool.toString(key));
        this.pKey = new SecretKeySpec(aKey, "DESede");
        this.mOpcodeMappingTable = new HashMap<>();
    }

    public byte[] initMap() {
        String sOpcodeMap = "";
        List<Integer> used = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            int opcode;
            while (true) {
                opcode = Randomizer.rand(RecvPacketOpcode.CP_BEGIN_USER.getValue(), 9999);
                if (!used.contains(opcode)) {
                    used.add(opcode);
                    break;
                }
            }
            String sOpcode = String.format("%04d", opcode);
            this.mOpcodeMappingTable.put(opcode, i + RecvPacketOpcode.CP_BEGIN_USER.getValue());
            sOpcodeMap += sOpcode;
        }
        return encrypt(sOpcodeMap.getBytes(Charset.forName("UTF-8")));
    }

    private byte[] encrypt(byte[] data) {
        try {
            Cipher pCipher = Cipher.getInstance("DESede");
            pCipher.init(Cipher.ENCRYPT_MODE, this.pKey);
            byte[] aEncrypted = pCipher.doFinal(data);
            byte[] aBuffer = new byte[Short.MAX_VALUE + 1];
            System.arraycopy(aEncrypted, 0, aBuffer, 0, aEncrypted.length);
            for (int i = aEncrypted.length; i < aBuffer.length; i++) {
                aBuffer[i] = (byte) Randomizer.nextInt();
            }
            return aBuffer;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.out.println("MapleDES Encrypt Error");
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Logger.getLogger(OpcodeEncryption.class.getName()).log(Level.SEVERE, null, ex);
        }
        return data;
    }

    public int mapOpcode(int op) {
        if (mOpcodeMappingTable.containsKey(op)) {
            return mOpcodeMappingTable.get(op);
        }
        return op;
    }
}
