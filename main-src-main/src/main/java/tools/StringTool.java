/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pungin
 */
public class StringTool {

    public static boolean parseBoolean(final String info) {
        return info != null && info.equalsIgnoreCase("true") || parseLong(info) > 0;
    }

    public static byte parseSByte(final String info) {
        return (byte) parseLong(info);
    }

    public static short parseByte(final String info) {
        return (short) (parseLong(info) & 0xFF);
    }

    public static short parseShort(final String info) {
        return (short) parseLong(info);
    }

    public static int parseUShort(final String info) {
        return (short) (parseLong(info) & 0xFFFF);
    }

    public static int parseInt(final String info) {
        return (int) parseLong(info);
    }

    public static long parseUInt(final String info) {
        return (parseLong(info) & 0xFFFFFFFF);
    }

    public static long parseLong(final String info) {
        return (long) parseDouble(info);
    }

    public static float parseFloat(final String info) {
        return (float) Double.parseDouble(info);
    }

    public static double parseDouble(final String info) {
        if (info == null || info.isEmpty() || !info.matches("-?[0-9]*.?[0-9]*")) {
            return 0.0D;
        }
        return Double.parseDouble(info);
    }

    public static List<String> splitList(final String info, final String value) {
        if (info == null || value == null) {
            return null;
        }
        List<String> list = new LinkedList<>();
        list.addAll(Arrays.asList(split(info, value)));
        return list;
    }

    public static String[] split(final String info, final String value) {
        if (info == null || value == null) {
            return null;
        }
        return info.split(value);
    }

    public static String unite(String[] infos, final String value) {
        return unite(Arrays.asList(infos), value);
    }

    public static String unite(final List<String> infos, final String value) {
        if (infos == null || infos.isEmpty() || value == null) {
            return null;
        }
        String info = "";
        for (String str : infos) {
            info += str + value;
        }
        info = info.isEmpty() ? info : info.substring(0, info.length() - value.length());
        return info;
    }

    public static String getOneValue(final String info, final String key) {
        if (info == null || key == null) {
            return null;
        }
        final String[] split = info.split(";");
        for (String x : split) {
            final String[] split2 = x.split("=");
            if (split2.length == 2 && split2[0].equals(key)) {
                return split2[1];
            }
        }
        return null;
    }

    public static String updateOneValue(String info, final String key, final String value) {
        if (key == null) {
            return null;
        }
        if (info == null) {
            if (value != null) {
                info = key + "=" + value;
            }
            return info;
        }

        final String[] split = info.split(";");
        boolean changed = false;
        boolean match = false;
        final StringBuilder newValue = new StringBuilder();
        for (String x : split) {
            final String[] split2 = x.split("=");
            if (split2.length != 2) {
                continue;
            }
            if (split2[0].equals(key)) {
                if (value != null) {
                    newValue.append(key).append("=").append(value);
                }
                match = true;
            } else {
                newValue.append(x);
            }
            newValue.append(";");
            changed = true;
        }
        if (!match && value != null) {
            newValue.append(key).append("=").append(value).append(";");
        }

        info = changed ? newValue.toString().substring(0, newValue.toString().length() - 1) : newValue.toString();
        if (info.isEmpty()) {
            return null;
        }
        return info;
    }
}
