package handling.cashshop;

import constants.ServerConstants;
import constants.ServerConstants.ServerType;
import handling.MapleServerHandler;
import handling.netty.ServerConnection;
import handling.channel.PlayerStorage;
import server.ServerProperties;

public class CashShopServer {

    private static String ip;
    public static int PORT = 8600;
    private static ServerConnection acceptor;
    private static PlayerStorage players;
    private static boolean finishedShutdown = false;

    public static void run_startup_configurations() {
        System.out.print("正在加載商城...");
        ip = ServerConstants.getHostAddress() + ":" + PORT;

        players = new PlayerStorage(MapleServerHandler.CASH_SHOP_SERVER);
        acceptor = new ServerConnection(ServerType.購物商城, PORT, 0, MapleServerHandler.CASH_SHOP_SERVER);
        acceptor.run();

        System.out.println("完成!");
        System.out.println("商城伺服器正在監聽" + PORT + "端口\r\n");
    }

    public static String getIP() {
        return ip;
    }

    public static PlayerStorage getPlayerStorage() {
        return players;
    }

    public static void shutdown() {
        if (finishedShutdown) {
            return;
        }
        System.out.println("儲存所有連接的用戶端(商城)...");
        players.disconnectAll();
        System.out.println("正在關閉商城...");
        acceptor.close();
        finishedShutdown = true;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }

    public static void loadSetting() {
        PORT = ServerProperties.getProperty("CASHSHOP_PORT", PORT);
    }

    static {
        loadSetting();
    }
}
