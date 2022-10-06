/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

/**
 *
 * @author Fate
 */
public class FileTime {
    public int dwLowDateTime;
    public int dwHighDateTime;
    private static final long FT_UT_OFFSET = 116444592000000000L;
    private static final long MAX_TIME = 150842304000000000L;
    private static final long ZERO_TIME = 94354848000000000L;
    private static final long PERMANENT = 150841440000000000L;

    public static long getTime(long realTimestamp) {
        if (realTimestamp == -1L) { // 00 80 05 BB 46 E6 17 02, 1/1/2079
            return MAX_TIME;
        }
        if (realTimestamp == -2L) { // 00 40 E0 FD 3B 37 4F 01, 1/1/1900
            return ZERO_TIME;
        }
        if (realTimestamp == -3L) {
            return PERMANENT;
        }
        return realTimestamp * 10000L + FT_UT_OFFSET;
    }

    public FileTime(long hFT1) {
        this.dwLowDateTime = (int) (hFT1 & 0xFFFFFFFF);
        this.dwHighDateTime = (int) (hFT1 >> 32);
    }

    public static FileTime GetMaxTime() {
        return new FileTime(getTime(-1));
    }

    public static FileTime GetZeroTime() {
        return new FileTime(getTime(-2));
    }

    public static FileTime GetPermanentTime() {
        return new FileTime(getTime(-3));
    }

    public static FileTime GetSystemTime() {
        return new FileTime(getTime(System.currentTimeMillis()));
    }

    // DB_DATE_19000101_45 <0x0FDE04000, 0x14F373B>
    // DB_DATE_20790101_46 <0x0BB058000, 0x217E646>

    // dwLowDateTime
    // dwHighDateTime
}
