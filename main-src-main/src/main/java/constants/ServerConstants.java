package constants;

import server.ServerProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Calendar;

public class ServerConstants {

    public static enum WzMapleVersion {
        GMS, EMS, BMS, CLASSIC, GENERATE, GETFROMZLZ
    }

    public static byte Class_Bonus_EXP(final int job) {
        switch (job) {
            case 501:
            case 530:
            case 531:
            case 532:
            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
            case 11212:
            case 800:
            case 900:
            case 910:
                return 10;
        }
        return 0;
    }

    public static boolean getEventTime() {
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        switch (Calendar.DAY_OF_WEEK) {
            case 1:
                return time >= 1 && time <= 5;
            case 2:
                return time >= 4 && time <= 9;
            case 3:
                return time >= 7 && time <= 12;
            case 4:
                return time >= 10 && time <= 15;
            case 5:
                return time >= 13 && time <= 18;
            case 6:
                return time >= 16 && time <= 21;
        }
        return time >= 19 && time <= 24;
    }

    public static String SYSTEM;

    // GMS stuff
    public static boolean TESPIA = false;
    public static short MAPLE_VERSION = (short) 207;
    public static String MAPLE_PATCH = "1";
    public static String MAPLE_LOGIN_PATCH = "1:0";
    public static MapleType MAPLE_TYPE = MapleType.台灣;
    public static WzMapleVersion wzMapleVersion = WzMapleVersion.CLASSIC;

    // Server stuff
    public static boolean USE_LOCALHOST = false;
    public static boolean REDIRECTOR = true; // 退出遊戲返回選角界面開關
    public static int SHARK_VERSION = 0x2021;
    public static boolean AntiKS = false;
    public static int MIRACLE_RATE = 1;
    public static byte SHOP_DISCOUNT = 0;
    public static boolean RELEASE_VERSION = false;
    public static boolean IS_BETA_FOR_ADMINS = false;// 是否Beta版,若是創建的角色都是伺服器管理員
    public static boolean FEVER_TIME = false; // Fever Time!! 咒語的痕跡用的
    public static String WELCOME_MSG = "歡迎來到「#b# 0##k」\\n\\n祝您遊戲愉快！";

    private static InetAddress getInetAddress() {
        try {
            return InetAddress.getByName(ServerConfig.IP);
        } catch (UnknownHostException ex) {
            System.err.println("獲取IP錯誤_未知的IP地址,錯誤內容:" + ex);
            return null;
        }
    }

    public static final byte[] getGateway_IP() {
        final InetAddress inetAddr = getInetAddress();
        if (inetAddr == null) {
            return new byte[]{(byte) 127, (byte) 0, (byte) 0, (byte) 1};
        }
        return inetAddr.getAddress();
    }

    public static final String getHostAddress() {
        final InetAddress inetAddr = getInetAddress();
        if (inetAddr == null) {
            return "127.0.0.1";
        }
        return inetAddr.getHostAddress();
    }

    public static enum ServerType {
        登入伺服器(1),
        頻道伺服器(2),
        購物商城(3),
        拍賣場(4),
        FARM_SERVER(5),
        ;
        private int type = 0;

        ServerType(int type) {
            this.type = type;
        }
    }

    public static enum MapleType {

        UNKNOWN(-1, 949),
        한국(1, 949),
        한국_TEST(2, 949),
        日本(3, 932),
        中国(4, 936),
        TESPIA(5, 949),
        台灣(6, 950),
        SEA(7, 949),
        GLOBAL(8, 949),
        BRAZIL(9, 949);

        byte type;
        int codepage;
        Charset charset;

        private MapleType(int type, int codepage) {
            this.type = (byte) type;
            this.codepage = codepage;
            try {
                charset = Charset.forName(String.format("MS%d", codepage));
            } catch (Exception e) {
                this.codepage = 949;
                charset = Charset.forName("MS949");
                System.err.println("設置Charset出錯(" + name() + "):" + e);
            }
        }

        public byte getType() {
            if (!ServerConstants.TESPIA) {
                return type;
            }
            switch (this) {
                case 한국:
                    return 한국_TEST.getType(); // KMS測試機
                case 한국_TEST:
                case TESPIA:
                    return type;
                default:
                    return TESPIA.getType(); // 測試機
            }
        }

        public int getCodePage() {
            return codepage;
        }

        public Charset getCharset() {
            return charset;
        }

        public void setType(int type) {
            this.type = (byte) type;
        }

        public static MapleType getByType(byte type) {
            for (MapleType l : MapleType.values()) {
                if (l.getType() == type) {
                    return l;
                }
            }
            return UNKNOWN;
        }
    }

    public static void loadSetting() {
        TESPIA = ServerProperties.getProperty("TESPIA", TESPIA);

        IS_BETA_FOR_ADMINS = GameConstants.getRegType() == 2 ? false : IS_BETA_FOR_ADMINS;
        USE_LOCALHOST = ServerProperties.getProperty("USE_LOCALHOST", USE_LOCALHOST);
        REDIRECTOR = ServerProperties.getProperty("REDIRECTOR", REDIRECTOR);
        SHARK_VERSION = ServerProperties.getProperty("SHARK_VERSION", SHARK_VERSION);
        MIRACLE_RATE = ServerProperties.getProperty("MIRACLE_RATE", MIRACLE_RATE);
        SHOP_DISCOUNT = ServerProperties.getProperty("SHOP_DISCOUNT", SHOP_DISCOUNT);
        FEVER_TIME = ServerProperties.getProperty("FEVER_TIME", FEVER_TIME);
        WELCOME_MSG = ServerProperties.getProperty("WELCOME_MSG", WELCOME_MSG);
    }

    static {
        if (System.getProperties().getProperty("os.name").toLowerCase().contains("windows")) {
            SYSTEM = "windows";
        } else if (System.getProperties().getProperty("os.name").toLowerCase().contains("linux")) {
            SYSTEM = "linux";
        } else {
            SYSTEM = "others";
        }
        loadSetting();
    }
}
