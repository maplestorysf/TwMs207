/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import server.MapleItemInformationProvider;
import tools.HexTool;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;

/**
 *
 * @author pungin
 */
public class RoyaCoupon {

    private final static MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

    public static void main(String file) {
        Properties data = new Properties();
        InputStreamReader is;
        try {
            is = new FileReader(file);
            data.load(is);
            is.close();
        } catch (IOException ex) {
            System.out.println("Failed to load " + file);
        }
        dumpPackages(data);
    }

    public static void dumpPackages(Properties data) {
        byte[] hexdata = HexTool.getByteArrayFromHexString(data.getProperty("packages"));
        final LittleEndianAccessor slea = new LittleEndianAccessor(new ByteArrayByteStream(hexdata));
        StringBuilder sb = new StringBuilder();
        byte size = slea.readByte();
        for (int i = 0; i < size; i++) {
            if (slea.readByte() == 0) {
                continue;
            }
            int coupId = slea.readInt();
            sb.append("if (!unkCoupon.containsKey(").append(coupId).append(")) { // ").append(ii.getName(coupId))
                    .append("\r\n");
            sb.append("    unkCoupon.put(").append(coupId).append(", Arrays.asList(new Pair[] {\r\n");
            slea.readByte();
            short couSize = slea.readShort();
            for (int j = 0; j < couSize; j++) {
                int left = slea.readInt();
                int right = slea.readInt();
                sb.append("        new Pair<>(").append(left).append(", ").append(right).append("), // [")
                        .append(ii.getName(left)).append(", ").append(ii.getName(right)).append("]\r\n");
            }
            sb.append("    }));\r\n");
            sb.append("}\r\n");
        }
        size = slea.readByte();
        for (int i = 0; i < size; i++) {
            if (slea.readByte() == 0) {
                continue;
            }
            int coupId = slea.readInt();
            sb.append("if (!unkCoupon2.containsKey(").append(coupId).append(")) { // ").append(ii.getName(coupId))
                    .append("\r\n");
            sb.append("    unkCoupon2.put(").append(coupId).append(", Arrays.asList(new List[] {\r\n");
            short couSize = slea.readShort();
            for (int j = 0; j < couSize; j++) {
                if (slea.readByte() == 0) {
                    continue;
                }
                sb.append("        Arrays.asList(new Pair[] {\r\n");
                short kSize = slea.readShort();
                for (int k = 0; k < kSize; k++) {
                    int left = slea.readInt();
                    int right = slea.readInt();
                    sb.append("            new Pair<>(").append(left).append(", ").append(right).append("), // [")
                            .append(ii.getName(left)).append(", ").append(ii.getName(right)).append("]\r\n");
                }
                sb.append("        }),\r\n");
            }
            sb.append("    }));\r\n");
            sb.append("}\r\n");
        }
        int royaSize = slea.readInt();
        for (int i = 0; i < royaSize; i++) {
            int coupId = slea.readInt();
            sb.append("if (!royaCoupon.containsKey(").append(coupId).append(")) { // ").append(ii.getName(coupId))
                    .append("\r\n");
            sb.append("    royaCoupon.put(").append(coupId).append(", new Pair<>(\r\n");
            sb.append("        Arrays.asList(new Integer[] { // 男\r\n");
            int jSize = slea.readInt();
            for (int j = 0; j < jSize; j++) {
                int id = slea.readInt();
                sb.append("            ").append(id).append(", // ").append(ii.getName(id)).append("\r\n");
            }
            sb.append("        }),\r\n");
            sb.append("        Arrays.asList(new Integer[] { // 女\r\n");
            jSize = slea.readInt();
            for (int j = 0; j < jSize; j++) {
                int id = slea.readInt();
                sb.append("            ").append(id).append(", // ").append(ii.getName(id)).append("\r\n");
            }
            sb.append("        })\r\n");
            sb.append("    ));\r\n");
            sb.append("}\r\n");
        }
        try {
            File outfile = new File("RoyaCoupon");
            outfile.mkdir();
            FileOutputStream out = new FileOutputStream(outfile + "/packages.txt", false);
            out.write(sb.toString().getBytes());
        } catch (IOException ex) {
            System.out.println("Failed to save data into a file");
        }
    }

    static {
        ii.runItems(false);
    }
}
