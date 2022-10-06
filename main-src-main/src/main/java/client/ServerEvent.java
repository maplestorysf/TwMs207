package client;

import handling.channel.ChannelServer;
import server.Timer.WorldTimer;

public class ServerEvent {

    public static void start() {
        // 自動請求進程
        WorldTimer.getInstance().register(() -> {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter player : cserv.getPlayerStorage().getAllCharacters()) {
                    if (player.getLastCheckProcess() <= 0) {
                        player.iNeedSystemProcess();
                        // System.out.println("請求進程");
                        continue;
                    }
                    if (System.currentTimeMillis() - player.getLastCheckProcess() >= 100 * 1000) { // 距上次發送進程包經過 2 分鐘
                        player.iNeedSystemProcess();
                    }
                }
            }
        }, 30 * 1000);

        // 泡點系統
        /*
         * WorldTimer.getInstance().register(() -> { for (ChannelServer cserv :
         * ChannelServer.getAllInstances()) { if (cserv.getChannelType() !=
         * ChannelServer.ChannelType.NORMAL) { continue; } for (MapleCharacter player :
         * cserv.getPlayerStorage().getAllCharacters()) { if (player.getMapId() / 100 ==
         * 9100000) { player.dropMessage(5, "你在自由市場充分的休息，因此獲得楓葉點數獎勵。");
         * player.modifyCSPoints(2, 2, true); } } } }, 5 * 60 * 1000);
         */
    }
}
