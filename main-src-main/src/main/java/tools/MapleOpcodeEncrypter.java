/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import server.Randomizer;

/**
 *
 * @author Weber
 */
public class MapleOpcodeEncrypter {

    private static final short baseOffset = 0x00B6;

    private byte[] key;

    private Map<Short, Short> mapping;

    public MapleOpcodeEncrypter(byte[] key) {
        this.key = key;
        this.mapping = new LinkedHashMap<>();
        this.generateRandomOpcode();
    }

    private void generateRandomOpcode() {
        short base = baseOffset;
        String nOpStr = "";
        for (short i = base; i < 1100; i++) {
            short nOp = (short) (Randomizer.nextInt() & 0xFFFF);
            while (this.mapping.containsKey(nOp)) {
                nOp = (short) (Randomizer.nextInt() & 0xFFFF);
            }
            this.mapping.put(nOp, i);
            i++;
            nOpStr += StringUtil.getLeftPaddedStr(String.valueOf(nOp), '0', 4);
        }

    }

    public byte[] getMappingPacket() {
        return null;
    }

    public short getMapOpcode(short originOpcode) {
        if (this.mapping.containsKey(originOpcode)) {
            return this.mapping.get(originOpcode);
        } else {
            return originOpcode;
        }
    }

}
