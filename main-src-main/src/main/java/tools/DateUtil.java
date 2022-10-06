package tools;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public class DateUtil {

    private static final int ITEM_YEAR2000 = -1085019342;
    private static final long REAL_YEAR2000 = 946681229830L;
    private static final int QUEST_UNIXAGE = 27111908;
    private static final long FT_UT_OFFSET = 116444520000000000L;

    public static final long MAX_TIME = 150842304000000000L;
    public static final long ZERO_TIME = 94354848000000000L;
    public static final long PERMANENT = 150841440000000000L;

    public static int getItemTimestamp(long realTimestamp) {
        int time = (int) ((realTimestamp - REAL_YEAR2000) / 1000L / 60L);
        return (int) (time * 35.762787000000003D) + ITEM_YEAR2000;
    }

    public static int getQuestTimestamp(long realTimestamp) {
        int time = (int) (realTimestamp / 1000L / 60L);
        return (int) (time * 0.1396987D) + QUEST_UNIXAGE;
    }

    public static String getBingoTimeString(long realTimestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        return sdf.format(realTimestamp);
    }

    public static boolean isDST() {
        return SimpleTimeZone.getDefault().inDaylightTime(new Date());
    }

    public static long getFileTimestamp(long timeStampinMillis) {
        return getFileTimestamp(timeStampinMillis, false);
    }

    public static long getFileTimestamp(long timeStampinMillis, boolean roundToMinutes) {
        if (timeStampinMillis == -1L) { // 00 80 05 BB 46 E6 17 02, 1/1/2079
            return MAX_TIME;
        }
        if (timeStampinMillis == -2L) { // 00 40 E0 FD 3B 37 4F 01, 1/1/1900
            return ZERO_TIME;
        }
        if (timeStampinMillis == -3L) {
            return PERMANENT;
        }
        if (isDST()) {
            timeStampinMillis -= 3600000L;
        }
        timeStampinMillis += 50400000L;
        long time;
        if (roundToMinutes) {
            time = timeStampinMillis / 1000L / 60L * 600000000L;
        } else {
            time = timeStampinMillis * 10000L;
        }
        return time + FT_UT_OFFSET;
    }

    public static long decodeFileTimestamp(long fakeTimestamp) {
        if (fakeTimestamp == MAX_TIME) {
            return -1L;
        }
        if (fakeTimestamp == ZERO_TIME) {
            return -2L;
        }
        if (fakeTimestamp == PERMANENT) {
            return -3L;
        }
        return (fakeTimestamp - FT_UT_OFFSET) / 10000L;
    }

    public static int getTime(long realTimestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmm");
        return Integer.valueOf(sdf.format(realTimestamp));
    }

    public static int getTime() {
        String time = new SimpleDateFormat("yyyy-MM-dd-HH").format(new Date()).replace("-", "");
        return Integer.valueOf(time);
    }

    public static long getKoreanTimestamp(long realTimestamp) {
        return realTimestamp * 10000L + 116444592000000000L;
    }
}
