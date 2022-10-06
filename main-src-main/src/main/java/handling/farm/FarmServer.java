/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.farm;

import constants.ServerConstants;
import constants.ServerConstants.ServerType;
import handling.MapleServerHandler;
import handling.netty.ServerConnection;
import handling.channel.PlayerStorage;
import server.ServerProperties;

/**
 *
 * @author Itzik
 */
public class FarmServer {

    private static String ip;
    public static int PORT = 8601;
    private static ServerConnection acceptor;
    private static PlayerStorage players;
    private static boolean finishedShutdown = false;

    public static void run_startup_configurations() {
        ip = ServerConstants.getHostAddress() + ":" + PORT;

        players = new PlayerStorage(MapleServerHandler.FARM_SERVER);
        acceptor = new ServerConnection(ServerType.FARM_SERVER, PORT, 0, MapleServerHandler.FARM_SERVER);
        acceptor.run();
        System.out.println("Farm Server is listening on port 8601.");
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
        System.out.println("Saving all connected clients (Farm)...");
        players.disconnectAll();
        System.out.println("Shutting down Farm...");

        finishedShutdown = true;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }

    public static void loadSetting() {
        PORT = ServerProperties.getProperty("FARM_PORT", PORT);
    }

    static {
        loadSetting();
    }
}
