/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import constants.GameConstants;
import javax.script.ScriptException;
import server.MapleItemInformationProvider;
import tools.HexTool;
import tools.OpcodeEncryption;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

/**
 *
 * @author Weber
 */
public class TestEval {

    public static void main(String[] args) throws ScriptException {

        byte[] key = HexTool.getByteArrayFromHexString("38 33 33 30 38 37 33 20 89 84 1b 5d 5f 38 d9 87 38 33 33 30 38 37 33 20");
        OpcodeEncryption enc = new OpcodeEncryption(key);
        
        String g = HexTool.toString(enc.initMap());
        
        System.exit(0);
        
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        ii.runItems(false);
        String packet = "6D 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 00 1B 00 00 00 40 08 00 00 8B 00 00 00 0C 26 00 00 01 00 00 00 08 00 A7 42 C0 EF B7 AC BF 7D 01 F6 97 65 09 28 63 2A FE CF FF EC 5B FE 67 48 E5 F0 63 47 5D 34 E4 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C 1C 0E 00 00 88 00 00 00 D6 25 00 00 02 00 00 00 0C 00 42 65 6C 69 65 76 65 A9 D9 AF F9 6F 01 20 17 28 00 50 9C 77 9E 63 3C 6E 4E FE 47 53 63 33 37 27 60 10 21 07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C 0C 19 00 00 89 00 00 00 69 25 00 00 03 00 00 00 0C 00 4F 6F BF FB C1 E5 B3 4E A4 68 6F 4F 01 01 39 4F 8B BC 61 5E FE CF 05 2E 53 FE 07 78 B3 74 46 7F 69 24 60 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C 6C 3B 00 00 89 00 00 00 B1 24 00 00 04 00 00 00 09 00 47 52 45 54 45 4C 4D 41 4E 00 28 0C 00 00 88 00 00 00 33 20 00 00 05 00 00 00 08 00 B0 7B B9 70 66 6A 73 64 00 E0 17 00 00 87 00 00 00 D3 1F 00 00 06 00 00 00 08 00 AD AB A6 5E A6 5E BE D0 00 E0 17 00 00 82 00 00 00 2B 1C 00 00 07 00 00 00 0B 00 4B 77 4D 69 72 61 63 6C 65 C9 40 00 84 37 00 00 85 00 00 00 37 1A 00 00 08 00 00 00 08 00 B4 4E B7 52 B3 4A B3 4A 00 1C 0E 00 00 7F 00 00 00 CB 19 00 00 09 00 00 00 08 00 AD B7 B2 4D B5 CE AA B5 00 10 10 00 00 8B 00 00 00 AD 19 00 00 0A 00 00 00 0C 00 58 A4 BB B9 44 A4 51 A6 72 B8 F4 58 00 6C 3B 00 00 6C 00 00 00 67 18 00 00 0B 00 00 00 0B 00 72 33 33 37 34 34 36 35 34 31 30 00 98 0A 00 00 76 00 00 00 38 18 00 00 0C 00 00 00 08 00 49 61 6D 61 6C 67 61 65 00 6C 3B 00 00 78 00 00 00 23 16 00 00 0D 00 00 00 0A 00 AF 53 A8 BD B4 B5 A7 51 AE A6 00 9C 01 00 00 82 00 00 00 13 16 00 00 0E 00 00 00 0C 00 AA 77 AD D3 A7 AC A7 4F A8 D3 B6 C3 00 CC 2B 00 00 83 00 00 00 FE 14 00 00 0F 00 00 00 0C 00 41 6F 78 A4 70 A6 CF A6 CF 78 6F 41 00 B2 01 00 00 70 00 00 00 9B 14 00 00 10 00 00 00 08 00 A8 D3 A7 E2 A5 A8 BC 43 00 74 10 00 00 70 00 00 00 3C 14 00 00 11 00 00 00 09 00 AC 50 A5 69 A5 69 4F 75 4F 00 6C 3B 00 00 6F 00 00 00 35 14 00 00 12 00 00 00 06 00 AA AE A7 41 A9 66 00 84 37 00 00 8B 00 00 00 79 12 00 00 13 00 00 00 08 00 BE 79 C9 40 C3 52 C9 40 00 B2 01 00 00 7A 00 00 00 9C 11 00 00 14 00 00 00 09 00 54 75 6D 62 6C 65 72 BD E5 00 70 19 00 00 79 00 00 00 06 0D 00 00 15 00 00 00 0C 00 A7 DA A8 E4 B9 EA AB DC B7 C5 AC 58 00 70 19 00 00 6A 00 00 00 47 0C 00 00 16 00 00 00 0A 00 76 69 76 69 61 6E A5 AD A4 5A 00 74 10 00 00 85 00 00 00 1F 0B 00 00 17 00 00 00 06 00 50 6F 72 6F 6E 67 00 0C 19 00 00 7E 00 00 00 0B 0B 00 00 18 00 00 00 0A 00 A7 DA AA BA A6 D1 BB 51 C3 5A 00 0C 19 00 00 72 00 00 00 88 0A 00 00 19 00 00 00 07 00 51 70 6F 6F 6F 6E 6E 00 32 0C 00 00 8C 00 00 00 C8 07 00 00 1A 00 00 00 08 00 A6 42 C1 F7 B4 48 A6 E5 00 6C 3B 00 00 88 00 00 00 84 07 00 00 1B 00 00 00 08 00 4D 4D 57 57 4D 57 57 4D 00 6D 00 00 00 01 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 01 4B 00 00 00 9C 01 00 00 B0 00 00 00 03 6A 00 00 01 00 00 00 08 00 BF 57 CA D6 AC 50 B7 AC 01 61 3F 32 12 D7 15 34 C2 20 06 AA 44 6A B0 40 CF F1 12 27 49 8E A1 03 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C 84 37 00 00 A4 00 00 00 1A 66 00 00 02 00 00 00 0A 00 BC E9 AC 79 AA FC BD D7 A5 4A 01 40 89 01 BE B6 A8 6D 4E E3 37 FA 47 4C 11 84 FB 54 63 5F 66 44 E0 07 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C F0 0C 00 00 B2 00 00 00 6B 4D 00 00 03 00 00 00 06 00 AF CD BB 48 30 30 01 21 99 C8 14 C3 91 77 26 23 06 8E 21 4E 31 13 3B B4 22 17 5E E2 66 03 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C B2 01 00 00 AF 00 00 00 25 4D 00 00 04 00 00 00 06 00 6F BC FE C1 6C 6F 00 6C 3B 00 00 A7 00 00 00 15 4D 00 00 05 00 00 00 0C 00 A8 A7 BC DF AA BA A5 EC A7 51 AE A6 00 10 10 00 00 B1 00 00 00 E3 4C 00 00 06 00 00 00 06 00 4B 41 4E 45 AB 69 00 70 19 00 00 9A 00 00 00 D7 4C 00 00 07 00 00 00 0C 00 AF 65 AD B7 AC 4F AF 7D C3 61 AA CC 00 B2 01 00 00 B3 00 00 00 D0 4C 00 00 08 00 00 00 0A 00 50 61 74 61 79 61 61 61 61 61 00 E8 00 00 00 AA 00 00 00 5D 4C 00 00 09 00 00 00 0A 00 B7 D9 AE F0 B3 DC A8 A7 BC DF 00 DE 00 00 00 AA 00 00 00 22 4C 00 00 0A 00 00 00 0A 00 4D 65 72 63 75 72 79 35 32 30 00 6C 3B 00 00 AE 00 00 00 DD 4B 00 00 0B 00 00 00 0A 00 BB E3 A5 69 B6 52 A6 61 B4 DF 00 B2 01 00 00 9D 00 00 00 AA 4B 00 00 0C 00 00 00 09 00 C0 E3 C0 E3 AB 4F B0 AE 7A 00 70 19 00 00 B3 00 00 00 99 4B 00 00 0D 00 00 00 0B 00 A4 F2 A4 F2 A5 58 A5 F4 B0 C8 31 00 D0 09 00 00 B1 00 00 00 38 4B 00 00 0E 00 00 00 04 00 DE B7 CF 4B 00 32 0C 00 00 B3 00 00 00 E9 3C 00 00 0F 00 00 00 0A 00 B3 7B A5 D2 C3 B9 A7 D3 B2 BB 00 08 09 00 00 AD 00 00 00 58 37 00 00 10 00 00 00 08 00 A6 CC A9 5F B2 A2 B5 A9 00 6C 09 00 00 B2 00 00 00 14 32 00 00 11 00 00 00 0B 00 B7 7C AD B8 AA BA B7 51 A9 C0 63 00 A9 08 00 00 B4 00 00 00 C2 30 00 00 12 00 00 00 08 00 76 B8 74 A4 E5 B4 CB 76 00 40 08 00 00 A7 00 00 00 83 2F 00 00 13 00 00 00 08 00 A9 B5 D0 D2 41 72 61 6E 00 28 0C 00 00 B1 00 00 00 7C 29 00 00 14 00 00 00 0A 00 B3 7B A5 D2 BC EF DE B3 AC 66 00 E0 17 00 00 B4 00 00 00 BE 26 00 00 15 00 00 00 0A 00 73 54 79 4C 65 A9 D9 AF F9 7A 00 0A 02 00 00 A7 00 00 00 97 26 00 00 16 00 00 00 04 00 59 AB ED 43 00 9C 01 00 00 AD 00 00 00 96 26 00 00 17 00 00 00 06 00 C2 C5 C4 D1 A5 5D 00 32 0C 00 00 99 00 00 00 93 26 00 00 18 00 00 00 08 00 C1 A8 AD B7 BB 65 A6 D0 00 6C 3B 00 00 A7 00 00 00 91 26 00 00 19 00 00 00 09 00 74 61 6E 67 72 6F 79 61 6C 00 E8 00 00 00 A1 00 00 00 8E 26 00 00 1A 00 00 00 07 00 43 68 69 B2 F6 6F 6F 00 A6 01 00 00 A6 00 00 00 85 26 00 00 1B 00 00 00 0B 00 A4 6A B8 7A C4 D1 BD 75 6F C6 67 00 E8 00 00 00 AD 00 00 00 85 26 00 00 1C 00 00 00 0A 00 7A A8 42 A4 74 A5 EC AA DC 7A 00 84 37 00 00 AD 00 00 00 81 26 00 00 1D 00 00 00 0B 00 B3 A5 BD DE C3 4D A4 68 7A A8 EC 00 E8 00 00 00 9E 00 00 00 7E 26 00 00 1E 00 00 00 0A 00 BB B2 A7 55 B5 DE B5 DE AA BA 00 84 37 00 00 A8 00 00 00 7D 26 00 00 1F 00 00 00 07 00 58 75 6E AC A5 AC A5 00 6C 3B 00 00 AD 00 00 00 7B 26 00 00 20 00 00 00 0A 00 A6 5E C1 E7 B8 D5 B8 D5 AC DD 00 6C 3B 00 00 9D 00 00 00 6F 26 00 00 21 00 00 00 0A 00 77 AA 6B A4 4F B5 4C C3 E4 77 00 0C 19 00 00 B1 00 00 00 6E 26 00 00 22 00 00 00 0A 00 A8 73 B7 A5 53 4D B9 46 A4 48 00 08 09 00 00 9E 00 00 00 60 26 00 00 23 00 00 00 04 00 50 41 57 47 00 08 09 00 00 AD 00 00 00 37 26 00 00 24 00 00 00 0A 00 AB DC AB D3 A4 46 A7 4F AC DD 00 70 19 00 00 9B 00 00 00 34 26 00 00 25 00 00 00 0C 00 78 BC 5A B8 A8 AA BA AF CD BB 48 78 00 DE 00 00 00 9B 00 00 00 1A 26 00 00 26 00 00 00 07 00 D3 7D D3 7D 6F 6D 6F 00 10 10 00 00 A7 00 00 00 18 26 00 00 27 00 00 00 08 00 A8 60 BC F6 C2 41 A6 E5 00 08 09 00 00 98 00 00 00 16 26 00 00 28 00 00 00 0B 00 50 65 67 67 79 50 69 67 34 35 36 00 80 27 00 00 B4 00 00 00 15 26 00 00 29 00 00 00 08 00 AA AF A4 7E BA 4E B3 BD 00 6C 3B 00 00 94 00 00 00 0D 26 00 00 2A 00 00 00 07 00 4E 61 7A B8 AF AA 4C 00 E8 05 00 00 9B 00 00 00 02 26 00 00 2B 00 00 00 09 00 42 65 6E 31 30 62 61 6E 67 00 10 10 00 00 A6 00 00 00 FC 25 00 00 2C 00 00 00 0A 00 AF 45 B5 BE B6 57 AF C5 AB D3 00 00 02 00 00 96 00 00 00 D5 25 00 00 2D 00 00 00 06 00 5A 65 74 72 6F 4C 00 1C 0E 00 00 9B 00 00 00 D4 25 00 00 2E 00 00 00 04 00 BF D5 B1 78 00 42 01 00 00 B4 00 00 00 CF 25 00 00 2F 00 00 00 06 00 A7 F5 AE F5 AC 75 00 9C 01 00 00 97 00 00 00 BC 25 00 00 30 00 00 00 0B 00 6F A5 D5 A5 D5 78 AD BB DB A3 6F 00 74 10 00 00 9F 00 00 00 8E 25 00 00 31 00 00 00 08 00 A4 4B A7 BD C6 5B AC 50 00 84 37 00 00 9F 00 00 00 46 25 00 00 32 00 00 00 07 00 73 74 72 73 64 65 74 00 40 08 00 00 8E 00 00 00 17 25 00 00 33 00 00 00 08 00 A4 70 A5 D5 B6 D9 B6 D9 00 1C 0E 00 00 A9 00 00 00 79 22 00 00 34 00 00 00 0A 00 BC D6 BC D6 A9 C0 AD 5E A4 E5 00 84 37 00 00 A2 00 00 00 CF 21 00 00 35 00 00 00 0A 00 AC C1 BC FE B7 73 AA B1 AE 61 00 84 37 00 00 AD 00 00 00 96 21 00 00 36 00 00 00 08 00 A6 42 AD B7 D4 D0 C2 C5 00 A9 08 00 00 9C 00 00 00 52 21 00 00 37 00 00 00 08 00 AA 67 AA 67 B7 7C BB 44 00 9C 01 00 00 A1 00 00 00 C3 16 00 00 38 00 00 00 05 00 B6 4C B2 DB 7A 00 CC 2B 00 00 93 00 00 00 BD 0E 00 00 39 00 00 00 0A 00 AA 59 AA 59 A4 6A AA 59 AA 59 00 10 10 00 00 98 00 00 00 75 0B 00 00 3A 00 00 00 0A 00 48 61 73 61 6B 65 79 79 79 79 00 0C 19 00 00 A5 00 00 00 C9 07 00 00 3B 00 00 00 0A 00 AD EC BD CC A5 4C 37 37 A6 B8 00 20 05 00 00 A7 00 00 00 C8 07 00 00 3C 00 00 00 08 00 B5 4C B7 AC C3 4D A4 68 00 08 09 00 00 9F 00 00 00 C6 07 00 00 3D 00 00 00 08 00 B5 4C BB EE BA EB C6 46 00 E0 17 00 00 AC 00 00 00 C5 07 00 00 3E 00 00 00 0B 00 32 AD D3 38 37 A8 AB A4 40 B0 5F 00 70 00 00 00 9D 00 00 00 C3 07 00 00 3F 00 00 00 0A 00 4F 6F AD 5E AA 4E B6 AF 6F 4F 00 8C 0C 00 00 B0 00 00 00 C0 07 00 00 40 00 00 00 07 00 B7 C5 A9 67 A6 6E 51 00 08 09 00 00 97 00 00 00 BF 07 00 00 41 00 00 00 08 00 B7 D3 BC CB B3 79 A5 79 00 32 0C 00 00 9B 00 00 00 BD 07 00 00 42 00 00 00 0C 00 B5 4C BF 4E B5 4A AA BA B5 4A BF 7D 00 84 37 00 00 97 00 00 00 BC 07 00 00 43 00 00 00 06 00 30 73 78 77 65 30 00 B8 0D 00 00 A1 00 00 00 B8 07 00 00 44 00 00 00 0A 00 A5 D5 A6 E2 AA BA B6 C2 AC 7D 00 74 10 00 00 9A 00 00 00 B8 07 00 00 45 00 00 00 0B 00 A6 D1 B9 AB A6 59 70 69 6E 6B 79 00 CC 2B 00 00 98 00 00 00 A3 07 00 00 46 00 00 00 0C 00 A7 4F A6 62 B7 52 BB A1 AF BA A4 46 00 28 0C 00 00 97 00 00 00 A0 07 00 00 47 00 00 00 09 00 42 B6 CB AD 6E A6 6E B0 AA 00 6C 09 00 00 97 00 00 00 9C 07 00 00 48 00 00 00 0C 00 A9 CE B3 5C AA BE B9 44 AC 4F BD D6 00 A6 01 00 00 9D 00 00 00 9A 07 00 00 49 00 00 00 0A 00 BC 75 BC 75 AA BA A9 AF BA D6 00 1C 0E 00 00 97 00 00 00 92 07 00 00 4A 00 00 00 06 00 BF D5 BF D5 A4 42 00 10 10 00 00 91 00 00 00 6E 07 00 00 4B 00 00 00 0C 00 A5 A4 AF F9 A9 40 B0 D8 A5 69 A5 69 00 6D 00 00 00 02 00 00 00 00 00 00 00 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF 02 64 00 00 00 D0 09 00 00 FA 00 00 00 D1 EA 00 00 01 00 00 00 04 00 DA 54 AD B8 01 06 82 9E 6D E6 66 0E C2 20 2E BA 44 C0 60 90 8F F4 04 3F 63 62 E7 03 80 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C E8 00 00 00 FA 00 00 00 17 DF 00 00 02 00 00 00 07 00 B7 AC A4 DF 6F A8 47 01 D5 91 0C 90 74 11 34 C2 20 00 1F 63 FE 27 13 CD F0 04 F9 1F AA A9 01 79 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C 9C 01 00 00 FA 00 00 00 E5 DB 00 00 03 00 00 00 08 00 AA F8 AB EB BB B5 BA 71 01 A7 20 77 30 22 27 34 C2 20 00 FF 51 FE 67 3F CB F1 1B 27 49 AA A9 83 A8 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0C 6C 09 00 00 FA 00 00 00 70 DB 00 00 04 00 00 00 09 00 AC 50 A9 5D A4 EB 6F B3 C7 00 7A 00 00 00 FA 00 00 00 63 DB 00 00 05 00 00 00 08 00 AB CA A4 A7 A6 42 AA CC 00 40 08 00 00 EE 00 00 00 CF CF 00 00 06 00 00 00 05 00 B8 B6 76 A6 D0 00 F0 0C 00 00 F7 00 00 00 EB CB 00 00 07 00 00 00 04 00 B3 DE BA B5 00 00 02 00 00 EB 00 00 00 DC CB 00 00 08 00 00 00 0B 00 69 6F 72 69 A4 4B AF AB 30 31 33 00 6C 09 00 00 FA 00 00 00 8D CB 00 00 09 00 00 00 08 00 B4 A3 A9 D4 6D 69 73 75 00 32 0C 00 00 F1 00 00 00 2C C8 00 00 0A 00 00 00 06 00 41 68 72 75 73 61 00 84 00 00 00 E7 00 00 00 30 C4 00 00 0B 00 00 00 04 00 A9 74 C0 AD 00 38 01 00 00 F0 00 00 00 7D C0 00 00 0C 00 00 00 05 00 C9 44 C9 44 78 00 9C 01 00 00 F4 00 00 00 76 C0 00 00 0D 00 00 00 0B 00 43 61 72 61 6D 65 6C A6 E3 B5 59 00 F0 0C 00 00 FA 00 00 00 6B C0 00 00 0E 00 00 00 04 00 D2 7B CD A3 00 00 02 00 00 F1 00 00 00 67 C0 00 00 0F 00 00 00 09 00 C2 49 C2 49 AB 42 BA 77 63 00 8C 0C 00 00 EA 00 00 00 3A C0 00 00 10 00 00 00 06 00 B4 A3 A8 E0 B8 A6 00 B2 01 00 00 E6 00 00 00 26 C0 00 00 11 00 00 00 0C 00 A6 B4 A4 EC A4 A3 A5 69 C0 4A B7 A6 00 E8 00 00 00 F1 00 00 00 23 C0 00 00 12 00 00 00 0B 00 C3 4D A4 FB A5 68 A5 B4 C2 79 6F 00 10 10 00 00 FA 00 00 00 0C C0 00 00 13 00 00 00 06 00 A4 EB AA B5 6D 6D 00 E8 00 00 00 EC 00 00 00 0A C0 00 00 14 00 00 00 08 00 C0 B0 A6 D5 BE F7 51 51 00 84 00 00 00 F1 00 00 00 FB BF 00 00 15 00 00 00 08 00 70 B2 C4 A4 40 A6 57 71 00 00 02 00 00 EA 00 00 00 DE BF 00 00 16 00 00 00 07 00 51 43 43 43 43 51 51 00 74 10 00 00 FA 00 00 00 CD BF 00 00 17 00 00 00 06 00 E3 6E A6 D0 B6 C6 00 B2 01 00 00 FA 00 00 00 47 BC 00 00 18 00 00 00 08 00 B3 B7 A4 48 B4 CB B4 CB 00 A6 01 00 00 FA 00 00 00 46 BC 00 00 19 00 00 00 0C 00 A4 70 BD 74 C2 E6 B3 CC A5 69 B7 52 00 9C 01 00 00 F1 00 00 00 43 BC 00 00 1A 00 00 00 07 00 B6 CC 6F AA E4 AA 59 00 F0 0C 00 00 F2 00 00 00 0F BC 00 00 1B 00 00 00 0A 00 78 B9 E0 C4 5F AC B5 BC 75 78 00 D4 00 00 00 E6 00 00 00 AC B8 00 00 1C 00 00 00 0A 00 45 75 73 74 69 61 AF 55 C3 FA 00 E8 00 00 00 FA 00 00 00 84 B8 00 00 1D 00 00 00 0C 00 C9 C3 B7 AC A4 A7 A5 5D A4 6C C9 C3 00 40 08 00 00 E7 00 00 00 82 B8 00 00 1E 00 00 00 08 00 A4 D1 AD B7 B7 52 BF DF 00 40 08 00 00 F0 00 00 00 52 B8 00 00 1F 00 00 00 0A 00 A5 A9 A7 4A A4 4F C1 F7 B6 ED 00 D4 00 00 00 E1 00 00 00 4C B8 00 00 20 00 00 00 08 00 52 6F 63 6B 65 72 A4 74 00 DE 00 00 00 FA 00 00 00 EB B4 00 00 21 00 00 00 0A 00 78 53 74 61 72 42 75 72 73 74 00 DE 00 00 00 FA 00 00 00 DC B4 00 00 22 00 00 00 08 00 4D 50 AA 51 AE DB AE E7 00 00 02 00 00 E6 00 00 00 BE B4 00 00 23 00 00 00 08 00 53 77 45 65 54 AC 50 78 00 B2 01 00 00 F1 00 00 00 B7 B4 00 00 24 00 00 00 09 00 52 65 7A 61 78 A9 5D CB 62 00 10 10 00 00 E6 00 00 00 8C B4 00 00 25 00 00 00 07 00 4D 65 69 AA DE A6 E7 00 6C 3B 00 00 E3 00 00 00 75 B4 00 00 26 00 00 00 07 00 B5 DE A9 5D C2 E6 6F 00 80 27 00 00 F0 00 00 00 73 B4 00 00 27 00 00 00 0A 00 4B 61 67 61 6D 69 6E 65 73 73 00 38 01 00 00 EC 00 00 00 35 B4 00 00 28 00 00 00 07 00 6C 75 66 66 79 A5 E0 00 9C 01 00 00 E2 00 00 00 CC B0 00 00 29 00 00 00 09 00 78 B8 58 AE F0 61 B3 F3 78 00 1C 0E 00 00 E6 00 00 00 A7 B0 00 00 2A 00 00 00 08 00 53 74 79 6C 65 B1 E3 63 00 98 0A 00 00 EC 00 00 00 91 B0 00 00 2B 00 00 00 0A 00 AE FC C2 41 B5 4B A4 F5 C1 E7 00 B2 01 00 00 E1 00 00 00 88 B0 00 00 2C 00 00 00 06 00 A4 70 C3 4D A6 F8 00 98 0A 00 00 E0 00 00 00 67 B0 00 00 2D 00 00 00 0A 00 4C 75 73 74 72 65 BB FE AA B5 00 F0 0C 00 00 E7 00 00 00 39 B0 00 00 2E 00 00 00 08 00 BB F4 B6 E3 AB A9 B0 DA 00 9C 01 00 00 EA 00 00 00 02 AD 00 00 2F 00 00 00 08 00 A6 BA AF AB A4 66 A5 CA 00 8C 0C 00 00 ED 00 00 00 D7 AC 00 00 30 00 00 00 0A 00 4F 78 B7 A5 A5 FA B3 BD 78 4F 00 9C 01 00 00 DD 00 00 00 CB AC 00 00 31 00 00 00 05 00 A9 C7 CA D6 63 00 9C 01 00 00 E1 00 00 00 C4 AC 00 00 32 00 00 00 04 00 AA D1 A6 EB 00 84 00 00 00 E7 00 00 00 AA AC 00 00 33 00 00 00 09 00 53 74 79 6C 65 C1 A2 A8 C8 00 9C 01 00 00 D9 00 00 00 86 AC 00 00 34 00 00 00 0C 00 C9 49 BA C6 A8 67 BC D0 B8 E9 C9 49 00 D4 00 00 00 E8 00 00 00 74 AC 00 00 35 00 00 00 0A 00 B3 6E BA F8 BA F8 B3 B7 A8 DF 00 B2 01 00 00 E1 00 00 00 6F AC 00 00 36 00 00 00 07 00 B8 76 A4 70 CB EC 71 00 8C 0C 00 00 ED 00 00 00 69 AC 00 00 37 00 00 00 05 00 AB F5 44 AA A8 00 DE 00 00 00 DE 00 00 00 DF A8 00 00 38 00 00 00 0A 00 C5 A5 BB A1 A7 41 B4 4E AC 4F 00 B2 01 00 00 DB 00 00 00 D8 A8 00 00 39 00 00 00 06 00 41 73 69 73 6B 65 00 84 37 00 00 E8 00 00 00 BC A8 00 00 3A 00 00 00 08 00 A5 54 AD 5C BA CD A6 A5 00 E8 00 00 00 E6 00 00 00 B5 A8 00 00 3B 00 00 00 0A 00 4E 61 76 79 BF 57 A8 A4 C4 48 00 B2 01 00 00 DE 00 00 00 97 A8 00 00 3C 00 00 00 0A 00 73 54 79 4C 65 A9 D9 AF F9 78 00 DE 00 00 00 E4 00 00 00 77 A8 00 00 3D 00 00 00 0B 00 4B 61 6E 61 64 65 4C 69 68 75 61 00 B2 01 00 00 D4 00 00 00 70 A8 00 00 3E 00 00 00 0B 00 6F 54 68 65 61 51 75 65 65 4E 6F 00 B2 01 00 00 DE 00 00 00 65 A8 00 00 3F 00 00 00 0A 00 73 6D 69 6C 65 C2 43 4F 75 4F 00 84 37 00 00 DF 00 00 00 5F A8 00 00 40 00 00 00 08 00 A6 B9 A4 EC AC B0 A7 F7 00 9C 01 00 00 FA 00 00 00 0B A6 00 00 41 00 00 00 07 00 58 AF 65 AD B7 59 5A 00 7A 00 00 00 E1 00 00 00 45 A5 00 00 42 00 00 00 08 00 50 65 64 6F 63 63 68 69 00 E8 05 00 00 DB 00 00 00 40 A5 00 00 43 00 00 00 0A 00 AA C4 A4 BC B7 CF AB 42 B1 A1 00 DE 00 00 00 EB 00 00 00 22 A5 00 00 44 00 00 00 06 00 BF A6 BF A6 A9 60 00 70 00 00 00 DC 00 00 00 16 A5 00 00 45 00 00 00 0A 00 A6 DC B0 AA A4 D1 A4 A7 A5 44 00 80 27 00 00 E0 00 00 00 01 A5 00 00 46 00 00 00 0C 00 A6 6E A6 59 AA BA BF 40 B4 F6 B4 CE 00 F0 0C 00 00 E3 00 00 00 F5 A4 00 00 47 00 00 00 0C 00 53 69 6E 47 6C 65 C9 40 BF DF C9 40 00 9C 01 00 00 DE 00 00 00 EB A4 00 00 48 00 00 00 07 00 A4 70 AD AF A5 CD 6F 00 14 02 00 00 E1 00 00 00 EA A4 00 00 49 00 00 00 0C 00 B6 57 A4 6A AA 4D BC F6 A5 69 A5 69 00 3C 02 00 00 EF 00 00 00 E8 A4 00 00 4A 00 00 00 0A 00 47 6F 74 68 69 63 C5 DA B2 FA 00 B2 01 00 00 D2 00 00 00 DA A4 00 00 4B 00 00 00 09 00 61 6C 62 65 72 74 79 65 65 00 B2 01 00 00 DD 00 00 00 C0 A4 00 00 4C 00 00 00 08 00 BC 76 AA 5A B6 F8 A6 4E 00 9C 01 00 00 DF 00 00 00 9C A4 00 00 4D 00 00 00 04 00 CF F4 B4 51 00 9C 01 00 00 DC 00 00 00 95 A4 00 00 4E 00 00 00 0A 00 A4 73 AA 46 A9 B1 A4 70 A4 47 00 BC 04 00 00 E4 00 00 00 DC A2 00 00 4F 00 00 00 07 00 C4 AB AA 47 AA E4 63 00 DE 00 00 00 DD 00 00 00 11 A1 00 00 50 00 00 00 0C 00 B4 A3 BC AF B7 52 A6 59 A4 FB B1 C6 00 08 09 00 00 DC 00 00 00 FD A0 00 00 51 00 00 00 0B 00 45 74 65 72 6E 61 6C C4 E5 A6 D0 00 0C 19 00 00 DE 00 00 00 EC A0 00 00 52 00 00 00 08 00 BF 4B C3 EC B1 FA B6 AF 00 70 19 00 00 DE 00 00 00 DE A0 00 00 53 00 00 00 0C 00 6E 61 72 75 68 69 6E 61 31 32 32 37 00 B2 01 00 00 D8 00 00 00 D0 A0 00 00 54 00 00 00 0B 00 AC DD A4 40 A6 B8 31 30 30 B6 F4 00 E0 17 00 00 D3 00 00 00 CA A0 00 00 55 00 00 00 0A 00 A8 A9 BE 7C A5 AC AE D4 A5 A7 00 08 09 00 00 E6 00 00 00 C5 A0 00 00 56 00 00 00 08 00 AC 4C AE DA B5 B2 A5 66 00 40 08 00 00 D6 00 00 00 BC A0 00 00 57 00 00 00 0B 00 6F B8 55 A8 BD A9 BA B3 7E C9 40 00 84 05 00 00 E0 00 00 00 9E A0 00 00 58 00 00 00 05 00 B8 C1 BB 52 63 00 28 0C 00 00 E6 00 00 00 2C 9D 00 00 59 00 00 00 0A 00 B9 DA C6 4C 78 50 69 61 6E 6F 00 38 01 00 00 D3 00 00 00 06 9D 00 00 5A 00 00 00 0A 00 54 65 61 70 61 72 74 79 59 61 00 00 02 00 00 D9 00 00 00 EE 9C 00 00 5B 00 00 00 09 00 BF D5 A6 CC 6F A6 CC A5 69 00 10 10 00 00 D3 00 00 00 E8 9C 00 00 5C 00 00 00 08 00 C1 AD BE 6C BF 4E 63 63 00 B2 01 00 00 D3 00 00 00 CF 9C 00 00 5D 00 00 00 07 00 CC C9 78 BF 55 BF 4E 00 B2 01 00 00 D7 00 00 00 BB 9C 00 00 5E 00 00 00 0A 00 AC FC A6 6E AA BA BC D0 BC D0 00 3C 02 00 00 E8 00 00 00 24 99 00 00 5F 00 00 00 08 00 A8 E2 A5 FA BB 61 C0 73 00 6C 09 00 00 D8 00 00 00 17 99 00 00 60 00 00 00 06 00 49 6D B1 6D A6 E2 00 98 0A 00 00 E3 00 00 00 0A 99 00 00 61 00 00 00 0A 00 4C 69 62 72 61 B5 EA B5 4C 6F 00 6C 3B 00 00 DC 00 00 00 07 99 00 00 62 00 00 00 08 00 AD B7 AD B7 59 4F 4F 4F 00 08 09 00 00 E0 00 00 00 01 99 00 00 63 00 00 00 08 00 A7 4E A9 5D A7 4E A7 4E 00 0C 19 00 00 DD 00 00 00 D5 98 00 00 64 00 00 00 08 00 6B 6B A5 64 B8 A6 AE 52 00 00";

        final LittleEndianAccessor slea = new LittleEndianAccessor(
                new ByteArrayByteStream((byte[]) HexTool.getByteArrayFromHexString(packet)));
        decodeDojankRank(slea);
        decodeDojankRank(slea);
        decodeDojankRank(slea);
    }

    public static int b2i(byte[] b, int offset) {
        final int byte1 = b[offset];
        final int byte2 = b[offset + 1];
        final int byte3 = b[offset + 2];
        final int byte4 = b[offset + 3];
        return (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    public static short b2s(byte[] b, int offset) {
        final int byte1 = b[offset];
        final int byte2 = b[offset + 1];
        return (short) ((byte2 << 8) + byte1);
    }

    private static void decodeDojankRank(LittleEndianAccessor slea) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        System.out.println("=======================================");
        slea.readInt();
        int nType = slea.readByte();
        System.out.println("RankType: " + nType);
        slea.readInt();
        slea.readInt();
        slea.readInt();
        slea.readInt();
        slea.readInt();
        slea.readInt();
        slea.readInt();
        slea.readInt();

        int nType2 = slea.readByte();
        System.out.println("Second RankType: " + nType2);
        int characterSize = slea.readInt();

        for (int i = 0; i < characterSize; i++) {
            int job = slea.readInt();
            int level = slea.readInt();
            int point = slea.readInt();
            int rank = slea.readInt();
            String name = slea.readMapleAsciiString();
            System.out.printf("職業 %d 等級 %d 積分 %d 排名 %d 角色 %s\n", job, level, point, rank, name);
            boolean hasAvatar = slea.readByte() > 0;
            if (hasAvatar) {
                int[] anHairEquip = new int[32];
                byte[] data = slea.read(120);

                int ver = data[119];
                if (ver == 0 || true) {

                    int v218 = data[0] & 1;
                    int v219 = (b2i(data, 0) >> 5) & 0x3FF;
                    int v220 = (b2i(data, 0) >> 1) & 0xF;
                    int v221;
                    int nGender = data[0] & 1;
                    if (nGender == 0xFF) {
                        nGender = -1;
                    }
                    int nFace = v219;
                    if (v219 == 1023) {
                        nFace = 0;
                    } else {
                        if (data[1] >= 0) {
                            v221 = 1000 * v218;
                        } else {
                            v221 = 2000;
                        }
                        nFace = v219 + v221 + 20000;
                    }
                    ////////
                    int v222 = b2i(data, 3);
                    int v223 = b2i(data, 2) & 0x3FF;
                    int v224;
                    anHairEquip[0] = v223;
                    if (v223 == 1023) {
                        anHairEquip[0] = 0;
                    } else {
                        if ((v222 & 4) > 0) {
                            v224 = 2000;
                        } else {
                            v224 = 1000 * nGender;
                        }
                        anHairEquip[0] = v223 + v224 + 30000;
                    }
                    ////////
                    short v225 = b2s(data, 4);
                    int v226 = (v222 >> 3) & 0x3FF;
                    int v227;
                    anHairEquip[1] = v226;
                    if (v226 == 1023) {
                        anHairEquip[1] = 0;
                    } else {
                        if ((v225 & 0x20) > 0) {
                            v227 = 2000;
                        } else {
                            v227 = 1000 * nGender;
                        }
                        anHairEquip[1] = v226 + v227 + 1000000; // 帽子
                    }
                    ////
                    int v228 = b2i(data, 6);
                    int v229 = data[6] & 1;
                    int v230 = v225 >> 6;
                    int v231;
                    anHairEquip[2] = v230;
                    if (v230 == 1023) {
                        anHairEquip[2] = 0;
                    } else {
                        if (v229 > 0) {
                            v231 = 2000;
                        } else {
                            v231 = 1000 * nGender;
                        }
                        anHairEquip[2] = v231 + v230 + 1010000;
                    }
                    ////
                    int v232 = b2i(data, 7);
                    int v233 = (v228 >> 1) & 0x3FF;
                    anHairEquip[3] = v233;
                    int v234;
                    if (v233 == 1023) {
                        anHairEquip[3] = 0;
                    } else {
                        if ((v232 & 8) > 0) {
                            v234 = 2000;
                        } else {
                            v234 = 1000 * nGender;
                        }
                        anHairEquip[3] = v233 + v234 + 1020000;
                    }
                    ///
                    int v235 = data[8] & 0x000000FF;
                    int earId = (v232 >> 4) & 0x3FF;
                    int earGender;
                    anHairEquip[4] = earId;
                    if (earId == 1023) {
                        anHairEquip[4] = 0;
                    } else {
                        if ((v235 & 0x40) > 0) {
                            earGender = 2000;
                        } else {
                            earGender = 1000 * nGender;
                        }
                        anHairEquip[4] = earId + earGender + 1030000; // 耳環
                    }
                    ///
                    int v238 = b2i(data, 10);
                    short v239 = (short) (b2s(data, 9) & 0x3FF);
                    int v240;
                    anHairEquip[5] = v239;
                    if (v239 == 1023) {
                        anHairEquip[5] = 0;
                    } else {
                        if ((v238 & 4) > 0) {
                            v240 = 2000;
                        } else {
                            v240 = 1000 * nGender;
                        }
                        anHairEquip[5] = v240 + v239 + 10000 * (((v235 & 0x80) | 13312) >> 7);
                    }
                    short v241 = b2s(data, 11);
                    int v242 = (v238 >> 3) & 0x3FF;
                    int v243;
                    anHairEquip[6] = v242;
                    if (v242 == 1023) {
                        anHairEquip[6] = 0;
                    } else {
                        if ((v241 & 0x20) > 0) {
                            v243 = 2000;
                        } else {
                            v243 = 1000 * nGender;
                        }
                        anHairEquip[6] = v242 + v243 + 1060000;
                    }
                    v241 = b2s(data, 11);
                    int v244 = b2i(data, 13);
                    int v245 = data[13] & 1;
                    int v246 = v241 >> 6;
                    int v247;
                    anHairEquip[7] = v246;
                    if (v246 == 1023) {
                        anHairEquip[7] = 0;
                    } else {
                        if (v245 > 0) {
                            v247 = 2000;
                        } else {
                            v247 = 1000 * nGender;
                        }
                        anHairEquip[7] = v247 + v246 + 1070000;
                    }
                    int v248 = b2i(data, 14);
                    int v249 = (v244 >> 1) & 0x3FF;
                    int v250;
                    anHairEquip[8] = v249;
                    if (v249 == 1023) {
                        anHairEquip[8] = 0;
                    } else {
                        if ((v248 & 8) > 0) {
                            v250 = 2000;
                        } else {
                            v250 = 1000 * nGender;
                        }
                        anHairEquip[8] = v249 + v250 + 1080000;
                    }
                    int v251 = b2i(data, 15);
                    int v252 = (v248 >> 4) & 0x3FF;
                    int v253;
                    anHairEquip[9] = v252;
                    if (v252 == 1023) {
                        anHairEquip[9] = 0;
                    } else {
                        if ((v251 & 0x40) > 0) {
                            v253 = 2000;
                        } else {
                            v253 = 1000 * nGender;
                        }
                        anHairEquip[9] = v252 + v253 + 1100000;
                    }
                    int v254 = b2i(data, 17);
                    int v255 = (v251 >> 7) & 0x3FF;
                    int v256;
                    anHairEquip[10] = v255;
                    if (v255 == 1023) {
                        anHairEquip[10] = 0;
                    } else {
                        if ((v254 & 2) > 0) {
                            v256 = 2000;
                        } else {
                            v256 = 1000 * nGender;
                        }
                        anHairEquip[10] = v255 + v256 + 1090000;
                    }
                    /////
                    int v257 = b2i(data, 18);
                    int v258;
                    int v4 = 0;
                    int nWeaponStickerID;
                    int v259;
                    int v260;
                    int v261;
                    if ((v254 & 4) > 0) {
                        v258 = (v254 >> 3) & 0x3FF;
                        if (v258 == 1023) {
                            anHairEquip[11] = 1002000;
                            nWeaponStickerID = 0;
                        } else {
                            if ((data[18] & 0x20) > 0) {
                                v259 = 2000;
                            } else {
                                v259 = 1000 * nGender;
                            }
                            anHairEquip[11] = 1002000;
                            v4 = v258 + v259 + 1700000;
                            nWeaponStickerID = v4;
                        }
                    } else {
                        v260 = (v254 >> 3) & 0x3FF;
                        if (v260 == 1023) {
                            v4 = 0;
                        } else {
                            if ((data[18] & 0x20) > 0) {
                                v261 = 2000;
                            } else {
                                v261 = 1000 * nGender;
                            }
                            v4 = v260 + v261 + 1000000;
                        }
                        anHairEquip[11] = v4;
                        nWeaponStickerID = 0;
                    }
                    int v211 = (v257 >> 6) & 0x1F;
                    int v262 = anHairEquip[11];
                    if (v211 > 0 && v262 > 0) {
                        v4 = v262 + 10000 * GameConstants.gWeaponType[v211];
                        anHairEquip[11] = v4;
                    } else {
                        anHairEquip[11] = 0;
                        nWeaponStickerID = 0;
                    }
                    System.out.printf("ver %d nGender %d nFace %d nWeaponStickerID %d  \n", ver, nGender, nFace,
                            nWeaponStickerID);
                    for (int j = 0; j < anHairEquip.length; j++) {
                        if (anHairEquip[j] > 0) {
                            System.out.printf("anHairEquip[%d] = %s (%d)\n", j, ii.getName(anHairEquip[j]),
                                    anHairEquip[j]);
                        }
                    }
                    continue;
                }
                int v4 = ver - 1;
                if (v4 > 10) {

                }
            }
        }
    }
}
