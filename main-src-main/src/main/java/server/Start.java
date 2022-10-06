package server;

import client.ServerEvent;
import client.inventory.MapleAndroid;
import client.inventory.MaplePet;
import client.skill.SkillFactory;
import client.skill.vcore.VCoreFactory;
import constants.GameConstants;
import constants.ServerConfig;
import constants.ServerConstants;
import constants.WorldConstants;
import database.ManagerDatabasePool;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.DojangRankingFactory;
import handling.channel.MapleDojoRanking;
import handling.channel.MapleGuildRanking;
import handling.farm.FarmServer;
import handling.login.LoginServer;
import handling.world.World;
import handling.world.guild.MapleGuild;
import server.Timer.*;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.PlayerNPC;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import server.swing.Progressbar;
import tools.MapleAESOFB;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class Start {

    public static long startTime = System.currentTimeMillis();
    public static final Start instance = new Start();
    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0);

    public void run() throws InterruptedException, IOException {
        String regCode = getRegCode();
        if (regCode == null || !GameConstants.regCodeCheck(regCode)) {
            RegCode regWin = new RegCode();
            regWin.setVisible(true);
            return;
        }
        Progressbar.visible(true);
        Progressbar.updateTitle("啟動伺服器");
        long start = System.currentTimeMillis();

        if (ServerConfig.ADMIN_ONLY || ServerConstants.USE_LOCALHOST) {
            System.out.println("Admin Only mode is active.");
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0")) {
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            throw new RuntimeException("運行時錯誤: 無法連接到MySQL數據庫服務器 - " + ex);
        }

        System.out.println("正在加載" + ServerConfig.SERVER_NAME + "伺服器");
        Progressbar.setText("初始化伺服器...");
        SendPacketOpcode.loadValues();
        RecvPacketOpcode.loadValues();
        World.init();
        Progressbar.addValue();
        System.out.println("\r\n主機: " + ServerConfig.IP + ":" + LoginServer.PORT);
        System.out.println("支援遊戲版本: " + ServerConstants.MAPLE_TYPE + "的" + ServerConstants.MAPLE_VERSION + "."
                + ServerConstants.MAPLE_PATCH + "版本" + (ServerConstants.TESPIA ? "測試機" : "") + "用戶端");
        System.out.println("主伺服器名稱: " + WorldConstants.getMainWorld().name());
        System.out.println("");

        if (ServerConstants.MAPLE_TYPE == ServerConstants.MapleType.GLOBAL) {
            boolean encryptionfound = false;
            for (MapleAESOFB.EncryptionKey encryptkey : MapleAESOFB.EncryptionKey.values()) {
                if (("V" + ServerConstants.MAPLE_VERSION).equals(encryptkey.name())) {
                    System.out.println("Packet Encryption: Up-To-Date!");
                    encryptionfound = true;
                    break;
                }
            }
            if (!encryptionfound) {
                System.out.println(
                        "Cannot find the packet encryption for the Maple Version you entered. Using the previous packet encryption instead.");
            }
        }
        runThread();
        loadData(false);
        Progressbar.setText("加載\"登入\"伺服器...");
        Progressbar.addValue();
        LoginServer.run_startup_configurations();
        Progressbar.setText("正在加載頻道...");
        Progressbar.addValue();
        ChannelServer.startChannel_Main();
        Progressbar.setText("正在加載商城...");
        Progressbar.addValue();

        CashShopServer.run_startup_configurations();
        if (ServerConstants.MAPLE_TYPE == ServerConstants.MapleType.GLOBAL) {
            FarmServer.run_startup_configurations();
        }
        Progressbar.setText("怪物刷新設定...");
        Progressbar.addValue();
        World.registerRespawn();
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        Progressbar.setText("加載地圖元素...");
        Progressbar.addValue();
        System.out.print("加載地圖元素");
        // 加載自訂地圖元素
        MapleMapFactory.loadCustomLife(false);
        // 加載玩家NPC
        PlayerNPC.loadAll();
        LoginServer.setOn();
        System.out.println("完成!\r\n");
        // System.out.println("Event Script List: " + ServerConfig.getEventList());
        if (ServerConfig.LOG_PACKETS) {
            System.out.println("數據包日誌模式已啟用");
        }
        long now = System.currentTimeMillis() - start;
        long seconds = now / 1000;
        long ms = now % 1000;
        System.out.println("\r\n加載完成, 耗時: " + seconds + "秒" + ms + "毫秒");
        Progressbar.setValue(100);
        JOptionPane.showMessageDialog(null, "伺服端啟動完成。");
        Progressbar.visible(false);
    }

    public static void runThread() {
        Progressbar.setText("加載線程...");
        System.out.print("正在加載線程");
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        CloneTimer.getInstance().start();
        System.out.print(/* "\u25CF" */".");
        Progressbar.addValue();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();
        ServerEvent.start();
        Progressbar.addValue();
        System.out.println("完成!\r\n");
    }

    public static void loadData(boolean reload) {
        try {
            System.out.println("載入數據");
            // 加載等級經驗
            System.out.println("加載等級經驗數據");
            Progressbar.setText("加載等級經驗數據...");
            Progressbar.addValue();
            GameConstants.LoadEXP();

            Progressbar.setText("加載排名訊息數據...");
            Progressbar.addValue();
            System.out.println("加載排名訊息數據");
            // 加載道場拍排名
            MapleDojoRanking.getInstance().load(reload);
            // 加載公會排名
            MapleGuildRanking.getInstance().load(reload);
            // 加載排名
            RankingWorker.run();

            Progressbar.setText("加載公會數據并清理不存在公會/寵物/機器人...");
            Progressbar.addValue();
            System.out.println("加載公會數據并清理不存在公會/寵物/機器人");
            // 清理已經刪除的寵物
            MaplePet.clearPet();
            // 清理已經刪除的機器人
            MapleAndroid.clearAndroid();
            // 加載公會並且清理無人公會
            MapleGuild.loadAll();
            // 加載家族(家族功能已去掉)
            // MapleFamily.loadAll();

            System.out.println("加載任務數據");
            Progressbar.setText("加載任務數據...");
            Progressbar.addValue();
            // 加載任務訊息
            MapleLifeFactory.loadQuestCounts(reload);
            // 加載轉存到數據庫的任務訊息
            MapleQuest.initQuests(reload);

            System.out.println("加載道具數據");
            Progressbar.setText("加載道具數據...");
            Progressbar.addValue();
            // 加載道具訊息(從WZ)
            MapleItemInformationProvider.getInstance().runEtc(reload);
            // 加載道具訊息(從SQL)
            MapleItemInformationProvider.getInstance().runItems(reload);

            System.out.println("加載技能數據");
            Progressbar.setText("加載技能數據...");
            Progressbar.addValue();
            // 加載技能
            SkillFactory.load(reload);
            VCoreFactory.Enforcement.load();
            VCoreFactory.CoreData.load();

            System.out.println("加載角色卡數據");
            Progressbar.setText("加載角色卡數據...");
            Progressbar.addValue();
            // 加載角色卡訊息
            CharacterCardFactory.getInstance().initialize(reload);

            System.out.println("加載商城道具數據");
            Progressbar.setText("加載商城道具數據...");
            Progressbar.addValue();
            // 加載商城道具訊息
            CashItemFactory.getInstance().initialize(reload);

            Progressbar.setText("加載掉寶數據...");
            Progressbar.addValue();
            System.out.println("加載掉寶數據");
            // 加載掉寶和全域掉寶數據
            MapleMonsterInformationProvider.getInstance().load();
            // 加載額外的掉寶訊息
            MapleMonsterInformationProvider.getInstance().addExtra();
            System.out.println("加載武陵排名");
            DojangRankingFactory.getInstance().load();
            Progressbar.setText("loadSpeedRuns...");
            Progressbar.addValue();
            System.out.println("loadSpeedRuns");
            System.out.println("加載成長導覽");
            GrowHelpFactory.getInstance().load();
            // ?
            SpeedRunner.loadSpeedRuns(reload);
            System.out.println("數據載入完成!\r\n");
            if (Progressbar.getValue() < 80) {
                Progressbar.setValue(80);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("開啟伺服器讀取訊息失敗:" + e);
        }
    }

    public static class Shutdown implements Runnable {

        @Override
        public void run() {
            ShutdownServer.getInstance().run();
        }
    }

    public static void setRegCode(String code) {
        ServerProperties.setProperty("REG_CODE", code);
        ServerProperties.saveProperties();
    }

    public static String getRegCode() {
        return ServerProperties.getProperty("REG_CODE", "");
    }
}
