/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.farm.handler;

import client.MapleCharacter;
import client.MapleClient;
import constants.WorldConstants;
import handling.channel.ChannelServer;
import handling.farm.FarmServer;
import handling.login.LoginServer;
import handling.world.World;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import server.farm.MapleFarm;
import tools.FileoutputUtil;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.FarmPacket;

/**
 *
 * @author Itzik
 */
public class FarmOperation {

    public static void EnterFarm(final MapleCharacter chr, final MapleClient c) {
        if (chr == null || !c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            return;
        }

        final int state = c.getLoginState();
        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
            World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        FarmServer.getPlayerStorage().registerPlayer(chr);
        c.getSession().writeAndFlush(FarmPacket.updateMonster(new LinkedList<>()));
        c.getSession().writeAndFlush(FarmPacket.enterFarm(c));
        c.getSession().writeAndFlush(FarmPacket.farmQuestData(new LinkedList<>(), new LinkedList<>()));
        c.getSession().writeAndFlush(FarmPacket.updateMonsterInfo(new LinkedList<>()));
        c.getSession().writeAndFlush(FarmPacket.updateAesthetic(c.getFarm().getAestheticPoints()));
        c.getSession().writeAndFlush(FarmPacket.spawnFarmMonster1());
        c.getSession().writeAndFlush(FarmPacket.farmPacket1());
        c.getSession().writeAndFlush(FarmPacket.updateFarmFriends(new LinkedList<>()));
        c.getSession().writeAndFlush(FarmPacket.updateFarmInfo(c));
        // c.getSession().writeAndFlush(CField.getPacketFromHexString("19 72 1E 02 00 00
        // 00 00 00 00 00 00 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00
        // 01 00 00 00 00 0B 00 43 72 65 61 74 69 6E 67 2E 2E 2E 00 00 00 00 00 00 00 00
        // 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 00 00 00 00 00 01 00 00 00 00
        // 00 00 00 00 FF FF FF FF 00"));
        c.getSession().writeAndFlush(FarmPacket.updateQuestInfo(21002, (byte) 1, ""));
        SimpleDateFormat sdfGMT = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
        sdfGMT.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));
        String timeStr = sdfGMT.format(Calendar.getInstance().getTime()).replaceAll("-", "");
        c.getSession().writeAndFlush(FarmPacket.updateQuestInfo(21001, (byte) 1, timeStr));
        c.getSession().writeAndFlush(FarmPacket.updateQuestInfo(21003, (byte) 1, "30"));
        c.getSession().writeAndFlush(FarmPacket.updateUserFarmInfo(chr, false));
        List<Pair<MapleFarm, Integer>> ranking = new LinkedList<>();
        ranking.add(new Pair<>(MapleFarm.getDefault(1, c, "Pyrous"), 999999));
        ranking.add(new Pair<>(MapleFarm.getDefault(1, c, "Sango"), 1));
        ranking.add(new Pair<>(MapleFarm.getDefault(1, c, "Hemmi"), -1));
        c.getSession().writeAndFlush(FarmPacket.sendFarmRanking(chr, ranking));
        c.getSession()
                .writeAndFlush(FarmPacket.updateAvatar(new Pair<>(WorldConstants.getMainWorld(), chr), null, false));
        if (c.getFarm().getName().equals("Creating...")) {
            c.getSession().writeAndFlush(FarmPacket.updateQuestInfo(1111, (byte) 0, "A1/"));
            c.getSession().writeAndFlush(FarmPacket.updateQuestInfo(2001, (byte) 0, "A1/"));
        }
    }

    public static void LeaveFarm(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || c == null) {
            return;
        }
        World.ChannelChange_Data(c, chr);
        ChannelServer toch = ChannelServer.getInstance(c.getChannel());
        if (toch == null) {
            FileoutputUtil.log("日誌/離開農場.txt", new StringBuilder().append("玩家: ").append(chr.getName())
                    .append(" 从農場離開發生錯誤.找不到頻道[").append(c.getChannel()).append("]的訊息.").toString());
            c.getSession().close();
            return;
        }
        FarmServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());
        String s = c.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        c.getSession().writeAndFlush(CField.getChannelChange(Integer.parseInt(toch.getIP().split(":")[1])));
        chr.saveToDB(false, true);
        c.setPlayer(null);
        c.setReceiving(false);
    }
}
