/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.swing;

import client.MapleCharacter;
import client.skill.SkillFactory;
import constants.*;
import database.ManagerDatabasePool;
import extensions.temporary.ScriptMessageType;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.farm.FarmServer;
import handling.login.LoginServer;
import handling.world.World;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.*;
import server.Timer;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.quest.MapleQuest;
import server.swing.tools.*;
import tools.*;
import tools.export.BuffInformation;
import tools.packet.CField.CScriptMan;
import tools.packet.CWvsContext;
import tools.wztosql.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Pungin
 */
public class WvsCenter extends javax.swing.JFrame {

    /**
     * Creates new form WvsCenter
     */
    private Thread server = null;
    private boolean searchServer = false;
    private List<Tools> tools = new ArrayList<>();
    private boolean writeChatLog = true;
    private static final WvsCenter instance = new WvsCenter();
    private Map<Windows, javax.swing.JFrame> windows = new HashMap<>();
    private List<Object[]> cashShopItems = new LinkedList<>();
    private int cashShopItemsPage = 0;
    private boolean charInitFinished = false;
    private static boolean MYSQL = false;
    public static boolean CAN_START = false;
    private final TableRowSorter<TableModel> charTableSorter;
    private int charTablePage;
    private int charTableFilterMinLevel = 1;
    private int charTableFilterMaxLevel = 255;
    private int charTableFilterGender = 0;
    private String charTableFilterName = "";
    private int charTableFilterStatus = 0;
    private final String[] charTableStatus = new String[]{"線上", "離線"};

    public static WvsCenter getInstance() {
        return instance;
    }

    public WvsCenter() {
        /* Set the Windows look and feel */
        // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
        // (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default
         * look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
 /*
         * 界面風格 Metal Nimbus CDE/Motif Windows Windows Classic
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | javax.swing.UnsupportedLookAndFeelException | IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(WvsCenter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        // </editor-fold>

        initComponents();
        super.setResizable(false);
        charTableSorter = new TableRowSorter<>(this.charTable.getModel());
        worldList.setSelectedItem(WorldConstants.getMainWorld().name());
        resetWorldPanel();
        resetSetting(false);
        this.charTablePage = 1;
        refreshCharTable();
        charTable.setRowSorter(this.charTableSorter);
    }

    private void refreshCharTable() {
        charTableSorter.setRowFilter(makeRowFilter(12, this.charTablePage - 1));
    }

    private RowFilter<TableModel, Integer> makeRowFilter(final int itemsPerPage, final int target) {
        return new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                int ei = entry.getIdentifier();
                String name = entry.getValue(4).toString();
                short level = (short) entry.getValue(5);

                boolean nameCom = (name.contains(charTableFilterName));
                if (jCheckBox1.isSelected()) {
                    nameCom = (name.equals(charTableFilterName));
                }

                return (target * itemsPerPage <= ei && ei < target * itemsPerPage + itemsPerPage) && nameCom
                        && (level >= charTableFilterMinLevel && level <= charTableFilterMaxLevel)
                        && (charTableFilterGender - 1 == -1 || (charTableFilterGender - 1 == (byte) entry.getValue(18)))
                        && (charTableFilterStatus - 1 == -1
                        || entry.getValue(0).toString().equals(charTableStatus[charTableFilterStatus - 1]));

            }
        };
    }

    public enum Tools {

        UpdateSQLWZ, FixCharSets, FixShopItemsPrice, BuffInformation;
    }

    public enum Windows {

        BuffStatusCalculator, ConvertOpcodes, SearchGenerator, ScriptExport, CashShopItemEditor, CashShopItemAdder, FixJavaScript, Donate,;
    }

    private javax.swing.ComboBoxModel getWorldModel() {
        List<String> worldModel = new ArrayList<>();
        for (WorldConstants.Option e : WorldConstants.values()) {
            worldModel.add(0, e.name());
        }
        return new DefaultComboBoxModel<>(worldModel.toArray());
    }

    private javax.swing.ComboBoxModel getMapleTypeModel() {
        List<String> mapleTypeModel = new ArrayList<>();
        mapleTypeModel.add(ServerConstants.MAPLE_TYPE.name());
        // for (ServerConstants.MapleType e : ServerConstants.MapleType.values()) {
        // if (e == ServerConstants.MapleType.UNKNOWN) {
        // continue;
        // }
        // mapleTypeModel.add(e.name());
        // }
        return new DefaultComboBoxModel<>(mapleTypeModel.toArray());
    }

    private javax.swing.ComboBoxModel getJobConstantModel() {
        List<String> jobModel = new ArrayList<>();
        for (JobConstants.LoginJob e : JobConstants.LoginJob.values()) {
            jobModel.add(e.name());
        }
        return new DefaultComboBoxModel<>(jobModel.toArray());
    }

    private void resetWorldPanel() {
        WorldConstants.Option world = WorldConstants.valueOf((String) worldList.getSelectedItem());
        expRate.setText(String.valueOf(world.getExp()));
        mesoRate.setText(String.valueOf(world.getMeso()));
        dropRate.setText(String.valueOf(world.getDrop()));
        flag.setText(String.valueOf(world.getFlag()));
        show.setSelected(world.show());
        show.setText(show.isSelected() ? "顯示" : "不顯");
        available.setSelected(world.isAvailable());
        available.setText(available.isSelected() ? "啟用" : "關閉");
        channelCount.setText(String.valueOf(world.getChannelCount()));
        worldTip.setText(String.valueOf(world.getWorldTip()));
        scrollingMessage.setText(String.valueOf(world.getScrollMessage()));
    }

    public static void addChatLog(String msg) {
        getInstance().chatLog.setText(getInstance().chatLog.getText() + msg);
    }

    public static boolean runExe(String processName) {
        return runExe(processName, null);
    }

    private static boolean runExe(String processName, String cmd) {
        if (!(new File(processName)).exists()) {
            return false;
        }
        if (findProcess(processName)) {
            killProcess(processName);
        }
        try {
            Runtime.getRuntime().exec(processName + (cmd == null || cmd.isEmpty() ? "" : (" " + cmd)));
        } catch (IOException ex) {
            Logger.getLogger(WvsCenter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private static boolean killProcess(String processName) {
        if (processName.split("/").length > 1) {
            processName = processName.split("/")[processName.split("/").length - 1];
        }
        if (findProcess(processName)) {
            try {
                Runtime.getRuntime().exec("taskkill /F /IM " + processName);
            } catch (IOException ex) {
                Logger.getLogger(WvsCenter.class.getName()).log(Level.SEVERE, null, ex);
            }
            return true;
        }
        return false;
    }

    private static boolean findProcess(String processName) {
        if (processName.split("/").length > 1) {
            processName = processName.split("/")[processName.split("/").length - 1];
        }
        BufferedReader bufferedReader = null;
        try {
            Process proc = Runtime.getRuntime().exec("cmd /c tasklist");
            bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(processName)) {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            Logger.getLogger(WvsCenter.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private void initCashShopItem() {
        CashItemFactory.getInstance().initialize(false);
        for (CashModItem modItem : CashItemFactory.getInstance().getAllModInfo()) {
            cashShopItems.add(new Object[]{modItem.getSN(), modItem.getNote(), modItem.getItemId(),
                modItem.getCount(), modItem.getPrice(), modItem.getPeriod(), modItem.getGender(),
                modItem.isOnSale() ? 1 : 0, modItem.getClass_(), modItem.isMainItem() ? 1 : 0});
        }
        initCashShopItemPannel();
    }

    private void initCashShopItemPannel() {
        DefaultTableModel model = (DefaultTableModel) cashshopItemsTable.getModel();
        int count = model.getRowCount();
        for (int i = 0; i < count; i++) {
            model.removeRow(0);
        }
        for (int i = 0; i < 100; i++) {
            int num = i + (100 * cashShopItemsPage);
            if (cashShopItems.size() < (num + 1)) {
                return;
            }
            model.insertRow(cashshopItemsTable.getRowCount(), cashShopItems.get(num));
        }
    }

    private static void updateSQLWZ() {
        Progressbar.setTitle("更新數據庫WZ[*請勿結束程序以免造成異常*]", 5);
        Progressbar.setText("正在轉存...");
        Progressbar.visible(true);
        DumpMobSkills.start(new String[0]);
        Progressbar.nextStep();
        DumpOxQuizData.start(new String[0]);
        Progressbar.nextStep();
        DumpNpcNames.start(new String[0]);
        Progressbar.nextStep();
        DumpQuests.start(new String[0]);
        Progressbar.nextStep();
        DumpItems.start(new String[0]);
        Progressbar.setValue(100);
        JOptionPane.showMessageDialog(null, "更新完成。");
        Progressbar.visible(false);
    }

    void addCashShopItem(List<Pair<Integer, String>> list) {
        for (Pair<Integer, String> pair : list) {
            if (pair.getLeft() != null) {
                addCashShopItem(pair.getLeft(), pair.getRight());
            }
        }
    }

    private void addCashShopItem(int sn, String note) {
        if (CashItemFactory.getInstance().getItem(sn) == null) {
            CashItem ci = CashItemFactory.getInstance().getOriginSimpleItem(sn);
            CashModItem cModItem = new CashModItem(ci.getSN(), note, ci.getItemId(), ci.getCount(), ci.getPrice(),
                    ci.getPeriod(), ci.getGender(), ci.getClass_(), ci.isOnSale(), false);
            CashItemFactory.getInstance().addModItem(cModItem);
            cashShopItems.add(new Object[]{cModItem.getSN(), cModItem.getNote(), cModItem.getItemId(),
                cModItem.getCount(), cModItem.getPrice(), cModItem.getPeriod(), cModItem.getGender(),
                cModItem.isOnSale() ? 1 : 0, cModItem.getClass_(), cModItem.isMainItem() ? 1 : 0});
        }
        initCashShopItemPannel();
    }

    void updateCashShopItem(Object[] values) {
        if (values != null && values.length > 9) {
            if (values[0] instanceof Integer) {
                CashModItem cModItem = CashItemFactory.getInstance().getItem((int) values[0]);
                if (cModItem != null) {
                    cModItem.setSN((int) values[0]);
                    cModItem.setNote((String) values[1]);
                    cModItem.setItemId((int) values[2]);
                    cModItem.setCount((int) values[3]);
                    cModItem.setPrice((int) values[4]);
                    cModItem.setPeriod((int) values[5]);
                    cModItem.setGender((int) values[6]);
                    cModItem.setOnSale(((int) values[7]) == 1);
                    cModItem.setClass_((int) values[8]);
                    cModItem.setMainItem(((int) values[9]) == 1);
                    CashItemFactory.getInstance().updateModItem(cModItem);
                }
            }
            for (int i = 0; i < cashShopItems.size(); i++) {
                if ((int) cashShopItems.get(i)[0] == (int) values[0]) {
                    cashShopItems.remove(i);
                    cashShopItems.add(i, values);
                    break;
                }
            }
        }
        initCashShopItemPannel();
    }

    private void deleteCashShopItem(int sn) {
        for (Object[] v : cashShopItems) {
            if ((int) v[0] == sn) {
                cashShopItems.remove(v);
                break;
            }
        }
        CashItemFactory.getInstance().deleteModItem(sn);
        initCashShopItemPannel();
    }

    private void initCharacterPannel() {
        if (charInitFinished) {
            return;
        }
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM characters");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ((DefaultTableModel) charTable.getModel()).insertRow(charTable.getRowCount(),
                            new Object[]{this.charTableStatus[1], rs.getInt("id"), rs.getInt("accountid"),
                                rs.getInt("world"), rs.getString("name"), rs.getShort("level"), rs.getLong("exp"),
                                rs.getInt("str"), rs.getInt("dex"), rs.getInt("int"), rs.getInt("luk"),
                                rs.getLong("hp"), rs.getLong("mp"), rs.getLong("maxhp"), rs.getLong("maxmp"),
                                rs.getLong("meso"), rs.getShort("job"), rs.getShort("skincolor"),
                                rs.getByte("gender"), rs.getInt("fame"), rs.getInt("hair"), rs.getInt("face"),
                                rs.getInt("ap"), rs.getInt("map"), rs.getByte("gm"), rs.getString("sp")});
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            System.out.println("初始化角色訊息錯誤:" + ex);
            JOptionPane.showMessageDialog(null, "初始化角色訊息錯誤, 請確認MySQL是否正常啟動");
        }
        charInitFinished = true;
    }

    public void addCharTable(MapleCharacter chr) {
        String sp = "";
        for (int s = 0; s < chr.getRemainingSps().length; s++) {
            sp += chr.getRemainingSps()[s];
            if (s < chr.getRemainingSps().length - 1) {
                sp += ",";
            }
        }
        ((DefaultTableModel) charTable.getModel()).insertRow(charTable.getRowCount(),
                new Object[]{this.charTableStatus[1], chr.getId(), chr.getAccountID(), chr.getWorld(), chr.getName(),
                    chr.getLevel(), chr.getExp(), chr.getStr(), chr.getDex(), chr.getInt(), chr.getLuk(),
                    chr.getStat().getHp(), chr.getStat().getMp(), chr.getStat().getMaxHp(),
                    chr.getStat().getMaxMp(), chr.getMeso(), chr.getJob(), chr.getSkinColor(), chr.getGender(),
                    chr.getFame(), chr.getHair(), chr.getFace(), chr.getRemainingAp(), chr.getMapId(),
                    chr.getGmLevel(), sp});
    }

    public void removeCharTable(int cid) {
        for (int i = 0; i < charTable.getRowCount(); i++) {
            int id = (Integer) charTable.getValueAt(i, 1);
            if (id == cid) {
                ((DefaultTableModel) charTable.getModel()).removeRow(i);
                break;
            }
        }
    }

    public void updateCharTable(boolean login, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        for (int i = 0; i < charTable.getRowCount(); i++) {
            int id = (Integer) charTable.getValueAt(i, 1);
            if (id == chr.getId()) {
                int j = 0;
                charTable.setValueAt(login ? charTableStatus[0] : charTableStatus[1], i, j++);
                charTable.setValueAt(chr.getId(), i, j++);
                charTable.setValueAt(chr.getAccountID(), i, j++);
                charTable.setValueAt(chr.getWorld(), i, j++);
                charTable.setValueAt(chr.getName(), i, j++);
                charTable.setValueAt(chr.getLevel(), i, j++);
                charTable.setValueAt(chr.getExp(), i, j++);
                charTable.setValueAt(chr.getStr(), i, j++);
                charTable.setValueAt(chr.getDex(), i, j++);
                charTable.setValueAt(chr.getInt(), i, j++);
                charTable.setValueAt(chr.getLuk(), i, j++);
                charTable.setValueAt(chr.getStat().getHp(), i, j++);
                charTable.setValueAt(chr.getStat().getMp(), i, j++);
                charTable.setValueAt(chr.getStat().getMaxHp(), i, j++);
                charTable.setValueAt(chr.getStat().getMaxMp(), i, j++);
                charTable.setValueAt(chr.getMeso(), i, j++);
                charTable.setValueAt(chr.getJob(), i, j++);
                charTable.setValueAt(chr.getSkinColor(), i, j++);
                charTable.setValueAt(chr.getGender(), i, j++);
                charTable.setValueAt(chr.getFame(), i, j++);
                charTable.setValueAt(chr.getHair(), i, j++);
                charTable.setValueAt(chr.getFace(), i, j++);
                charTable.setValueAt(chr.getRemainingAp(), i, j++);
                charTable.setValueAt(chr.getMapId(), i, j++);
                charTable.setValueAt(chr.getGmLevel(), i, j++);
                String sp = "";
                for (int s = 0; s < chr.getRemainingSps().length; s++) {
                    sp += chr.getRemainingSps()[s];
                    if (s < chr.getRemainingSps().length - 1) {
                        sp += ",";
                    }
                }
                charTable.setValueAt(sp, i, j++);
                break;
            }
        }
    }

    private MapleCharacter getSelectCharacter() {
        int val_targ;
        if (charTable.getSelectedRow() == -1) {
            return null;
        } else if (charTable.getValueAt(charTable.getSelectedRow(), 0) == "離線") {
            return null;
        } else {
            val_targ = (Integer) charTable.getValueAt(charTable.getSelectedRow(), 1);
        }

        return MapleCharacter.getOnlineCharacterById(val_targ);
    }

    private void startServer() {
        if (LoginServer.isShutdown() && server == null) {
            server = new Thread(() -> {
                try {
                    Start.instance.run();
                } catch (InterruptedException | IOException ex) {
                    Logger.getLogger(WvsCenter.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            server.start();
        } else {
            JOptionPane.showMessageDialog(null, "伺服端已在運行中。");
        }
    }

    private void reStartServer() {
        if (LoginServer.isShutdown() || server == null) {
            JOptionPane.showMessageDialog(null, "伺服端未運行。");
        } else {
            JOptionPane.showMessageDialog(null, "正在重新啟動伺服端,請點選確定繼續。");
            ShutdownServer.getInstance().shutdown();
            server = null;
            startServer();
        }
    }

    protected static Thread t = null;
    private static ScheduledFuture<?> ts = null;
    private int minutesLeft = 0;

    private void shutdownServer() {
        if (LoginServer.isShutdown() || server == null) {
            JOptionPane.showMessageDialog(null, "伺服端未運行。");
            return;
        }
        minutesLeft = 2;
        if (ts == null && (t == null || !t.isAlive())) {
            t = new Thread(ShutdownServer.getInstance());
            ts = Timer.EventTimer.getInstance().register(() -> {
                if (minutesLeft == 0) {
                    ShutdownServer.getInstance().shutdown();
                    t.start();
                    ts.cancel(false);
                    server = null;
                    return;
                }
                World.Broadcast.broadcastMessage(
                        CWvsContext.broadcastMsg(0, "伺服器將在" + minutesLeft + " 分鐘后進行停機維護, 請及時安全的下線, 以免造成不必要的損失。"));
                minutesLeft--;
            }, 60000);
            JOptionPane.showMessageDialog(null, "伺服器將在" + minutesLeft + " 分鐘后關閉");
        } else {
            JOptionPane.showMessageDialog(null, "關閉進程正在進行或者關閉已完成，請稍候。");
        }
    }

    private enum ServerModifyType {

        EXP, MESO, DROP, FLAG, SHOW, AVAILABLE, CHANNELS, WORLD_TIP, SCROLL_MSG;
    }

    private void modifyServer(ServerModifyType type) {
        try {
            WorldConstants.Option world = WorldConstants.valueOf((String) worldList.getSelectedItem());
            switch (type) {
                case EXP:
                    world.setExp(0);
                    break;
                case MESO:
                    world.setMeso(0);
                    break;
                case DROP:
                    world.setDrop(0);
                    break;
                case FLAG:
                    world.setFlag((byte) -1);
                    break;
                case SHOW:
                    world.setShow(false);
                    break;
                case AVAILABLE:
                    world.setAvailable(false);
                    break;
                case CHANNELS:
                    world.setChannelCount(0);
                    break;
                case WORLD_TIP:
                    world.setWorldTip(null);
                    break;
                case SCROLL_MSG:
                    world.setScrollMessage(null);
                    break;
            }
            resetWorldPanel();
            JOptionPane.showMessageDialog(null, "更變成功。");
        } catch (NumberFormatException | HeadlessException e) {
            JOptionPane.showMessageDialog(null, "錯誤!\r\n" + e);
        }
    }

    private void modifyServer(ServerModifyType type, String str) {
        try {
            WorldConstants.Option world = WorldConstants.valueOf((String) worldList.getSelectedItem());
            switch (type) {
                case EXP:
                    world.setExp(Integer.valueOf(str));
                    break;
                case MESO:
                    world.setMeso(Integer.valueOf(str));
                    break;
                case DROP:
                    world.setDrop(Integer.valueOf(str));
                    break;
                case FLAG:
                    world.setFlag(Byte.valueOf(str));
                    break;
                case SHOW:
                    world.setShow(Boolean.valueOf(str));
                    break;
                case AVAILABLE:
                    world.setAvailable(Boolean.valueOf(str));
                    break;
                case CHANNELS:
                    world.setChannelCount(Integer.valueOf(str));
                    break;
                case WORLD_TIP:
                    world.setWorldTip(str);
                    break;
                case SCROLL_MSG:
                    world.setScrollMessage(str);
                    break;
            }
            resetWorldPanel();
            JOptionPane.showMessageDialog(null, "更變成功。");
        } catch (NumberFormatException | HeadlessException e) {
            JOptionPane.showMessageDialog(null, "錯誤!\r\n" + e);
        }
    }

    private void sendNotice(int type) {
        try {
            String str = noticeText.getText();
            byte[] p = null;
            switch (type) {
                case 0:
                    p = CWvsContext.broadcastMsg(6, "[公告事項] " + str);
                    break;
                case 1:
                    p = CWvsContext.broadcastMsg(1, str);
                    break;
                case 2:
                    p = CWvsContext.broadcastMsg(5, str);
                    break;
                case 3:
                    p = CScriptMan.getNPCTalk(2007, ScriptMessageType.SM_SAY, str, "00 00", (byte) 0);
            }
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.broadcastPacket(p);
            }
            if (type == 0) {
                printChatLog("[公告事項] " + str);
            }

            noticeText.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "錯誤!\r\n" + e);
        }
    }

    private void printChatLog(String str) {
        if (writeChatLog) {
            chatLog.setText(chatLog.getText() + str + "\r\n");
        }
    }

    private void runTool(final Tools tool) {
        if (tools.contains(tool)) {
            JOptionPane.showMessageDialog(null, "工具已在運行。");
        } else {
            tools.add(tool);
            Thread t = new Thread(() -> {
                switch (tool) {
                    case UpdateSQLWZ:
                        updateSQLWZ();
                        break;
                    case FixShopItemsPrice:
                        FixShopItemsPrice.main();
                        break;
                    case FixCharSets:
                        FixCharSets.start(new String[0]);
                        break;
                    case BuffInformation:
                        BuffInformation.start(new String[0]);
                        break;
                }
                tools.remove(tool);
            });
            t.start();
        }
    }

    private void openWindow(final Windows w) {
        if (!windows.containsKey(w)) {
            switch (w) {
                case BuffStatusCalculator:
                    windows.put(w, new BuffStatusCalculator());
                    break;
                case ConvertOpcodes:
                    windows.put(w, new ConvertOpcodes());
                    break;
                case SearchGenerator:
                    windows.put(w, new SearchGeneratorFrame());
                    break;
                case ScriptExport:
                    windows.put(w, new ScriptExport());
                    break;
                case CashShopItemEditor:
                    windows.put(w, new CashShopItemEditor());
                    break;
                case CashShopItemAdder:
                    windows.put(w, new CashShopItemAdder());
                    break;
                case FixJavaScript:
                    windows.put(w, new FixJavaScript());
                    break;
                case Donate:
                    windows.put(w, new Donate());
                    break;
                default:
                    return;
            }
            windows.get(w).setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        }
        windows.get(w).setVisible(true);
    }

    private void resetSetting(boolean read) {
        if (read) {
            ServerProperties.loadProperties();
            WorldConstants.loadSetting();
            ServerConfig.loadSetting();
            ServerConstants.loadSetting();
        }
        jTextField9.setText(WorldConstants.WORLD_TIP);
        jTextField10.setText(WorldConstants.SCROLL_MESSAGE);
        jTextField11.setText(String.valueOf(WorldConstants.CHANNEL_COUNT));

        jTextField12.setText(ServerConfig.SERVER_NAME);
        jTextField13.setText(ServerConfig.IP);
        jTextField14.setText(String.valueOf(ServerConfig.USER_LIMIT));
        jTextField21.setText(String.valueOf(ServerConfig.CHANNEL_MAX_CHAR_VIEW));

        jTextField15.setText(String.valueOf(WorldConstants.FLAG));
        jTextField3.setText(String.valueOf(WorldConstants.EXP_RATE));
        jTextField4.setText(String.valueOf(WorldConstants.MESO_RATE));
        jTextField5.setText(String.valueOf(WorldConstants.DROP_RATE));

        jCheckBox5.setSelected(ServerConfig.ADMIN_ONLY);
        jCheckBox10.setSelected(ServerConfig.LOG_PACKETS);
        jCheckBox11.setSelected(ServerConfig.AUTO_REGISTER);
        jCheckBox8.setSelected(ServerConfig.LOG_SHARK);

        jTextField2.setText(ServerConfig.SQL_IP);
        jTextField6.setText(ServerConfig.SQL_USER);
        jTextField7.setText(ServerConfig.SQL_PASSWORD);
        jTextField8.setText(ServerConfig.SQL_PORT);
        jTextField1.setText(ServerConfig.SQL_DATABASE);

        jCheckBox3.setSelected(ServerConstants.TESPIA);
        jCheckBox4.setSelected(ServerConstants.REDIRECTOR);
        jCheckBox9.setSelected(ServerConstants.FEVER_TIME);
        jCheckBox6.setSelected(ServerConstants.USE_LOCALHOST);

        jTextField26.setText(String.valueOf(ServerConstants.SHOP_DISCOUNT));
        jTextField24.setText(String.valueOf(ServerConstants.MIRACLE_RATE));
        jTextField25.setText(String.valueOf(ServerConstants.SHARK_VERSION));

        jToggleButton1.setSelected(JobConstants.LoginJob.valueOf((String) jComboBox4.getSelectedItem()).enableCreate());
        jToggleButton1.setText(jToggleButton1.isSelected() ? "開啟" : "關閉");

        jTextField23.setText(String.valueOf(LoginServer.PORT));
        jTextField27.setText(String.valueOf(ChannelServer.DEFAULT_PORT));
        jTextField28.setText(String.valueOf(CashShopServer.PORT));
        jTextField29.setText(String.valueOf(FarmServer.PORT));

        resetWorldPanel();
    }

    private void updateSetting(boolean save) {
        ServerProperties.setProperty("WORLD_TIP", jTextField9.getText());
        ServerProperties.setProperty("SCROLL_MESSAGE", jTextField10.getText());
        ServerProperties.setProperty("CHANNEL_COUNT", jTextField11.getText());

        ServerProperties.setProperty("SERVER_NAME", jTextField12.getText());
        ServerProperties.setProperty("IP", jTextField13.getText());
        ServerProperties.setProperty("USER_LIMIT", jTextField14.getText());
        ServerProperties.setProperty("CHANNEL_MAX_CHAR_VIEW", jTextField21.getText());

        ServerProperties.setProperty("FLAG", jTextField15.getText());
        ServerProperties.setProperty("EXP_RATE", jTextField3.getText());
        ServerProperties.setProperty("MESO_RATE", jTextField4.getText());
        ServerProperties.setProperty("DROP_RATE", jTextField5.getText());

        ServerProperties.setProperty("ADMIN_ONLY", jCheckBox5.isSelected());
        ServerProperties.setProperty("LOG_PACKETS", jCheckBox10.isSelected());
        ServerProperties.setProperty("AUTO_REGISTER", jCheckBox11.isSelected());
        ServerProperties.setProperty("LOG_SHARK", jCheckBox8.isSelected());

        ServerProperties.setProperty("SQL_IP", jTextField2.getText());
        ServerProperties.setProperty("SQL_USER", jTextField6.getText());
        ServerProperties.setProperty("SQL_PASSWORD", jTextField7.getText());
        ServerProperties.setProperty("SQL_PORT", jTextField8.getText());
        ServerProperties.setProperty("SQL_DATABASE", jTextField1.getText());

        ServerProperties.setProperty("TESPIA", jCheckBox3.isSelected());
        ServerProperties.setProperty("REDIRECTOR", jCheckBox4.isSelected());
        ServerProperties.setProperty("FEVER_TIME", jCheckBox9.isSelected());
        ServerProperties.setProperty("USE_LOCALHOST", jCheckBox6.isSelected());

        ServerProperties.setProperty("SHOP_DISCOUNT", jTextField26.getText());
        ServerProperties.setProperty("MIRACLE_RATE", jTextField24.getText());
        ServerProperties.setProperty("SHARK_VERSION", jTextField25.getText());

        ServerProperties.setProperty("LOGIN_PORT", jTextField23.getText());
        ServerProperties.setProperty("CHANNEL_START_PORT", jTextField27.getText());
        ServerProperties.setProperty("CASHSHOP_PORT", jTextField28.getText());
        ServerProperties.setProperty("FARM_PORT", jTextField29.getText());

        WorldConstants.loadSetting();
        ServerConfig.loadSetting();
        ServerConstants.loadSetting();
        LoginServer.loadSetting();
        ChannelServer.loadSetting();
        CashShopServer.loadSetting();
        FarmServer.loadSetting();

        resetWorldPanel();
        if (save) {
            ServerProperties.saveProperties();
        }
    }

    private void loadShops() {

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        AppIcon = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        worldList = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        expRate = new javax.swing.JTextField();
        changeExpRate = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        dropRate = new javax.swing.JTextField();
        changeDropRate = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        mesoRate = new javax.swing.JTextField();
        changeMesoRate = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        worldTip = new javax.swing.JTextField();
        jButton8 = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        noticeText = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        sendNotice = new javax.swing.JButton();
        sendWinNotice = new javax.swing.JButton();
        sendMsgNotice = new javax.swing.JButton();
        sendNpcTalkNotice = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        jButton13 = new javax.swing.JButton();
        jButton14 = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        chatLog = new javax.swing.JTextArea();
        jButton10 = new javax.swing.JButton();
        jLabel26 = new javax.swing.JLabel();
        scrollingMessage = new javax.swing.JTextField();
        jButton11 = new javax.swing.JButton();
        jButton12 = new javax.swing.JButton();
        changeExpRate1 = new javax.swing.JButton();
        changeDropRate1 = new javax.swing.JButton();
        changeMesoRate1 = new javax.swing.JButton();
        jLabel44 = new javax.swing.JLabel();
        jLabel46 = new javax.swing.JLabel();
        flag = new javax.swing.JTextField();
        jButton27 = new javax.swing.JButton();
        jButton38 = new javax.swing.JButton();
        jLabel47 = new javax.swing.JLabel();
        channelCount = new javax.swing.JTextField();
        jButton39 = new javax.swing.JButton();
        jButton40 = new javax.swing.JButton();
        show = new javax.swing.JToggleButton();
        available = new javax.swing.JToggleButton();
        jButton41 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton42 = new javax.swing.JButton();
        jButton43 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel26 = new javax.swing.JPanel();
        jLabel39 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel22 = new javax.swing.JPanel();
        jLabel35 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jPanel23 = new javax.swing.JPanel();
        jLabel36 = new javax.swing.JLabel();
        jTextField16 = new javax.swing.JTextField();
        jLabel37 = new javax.swing.JLabel();
        jTextField17 = new javax.swing.JTextField();
        jPanel24 = new javax.swing.JPanel();
        jLabel38 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jPanel25 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jTextField18 = new javax.swing.JTextField();
        jButton32 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        charTable = new javax.swing.JTable();
        jCheckBox2 = new javax.swing.JCheckBox();
        jButton46 = new javax.swing.JButton();
        jButton47 = new javax.swing.JButton();
        jPanel27 = new javax.swing.JPanel();
        jPanel33 = new javax.swing.JPanel();
        jLabel40 = new javax.swing.JLabel();
        jTextField19 = new javax.swing.JTextField();
        jTextField20 = new javax.swing.JTextField();
        jButton34 = new javax.swing.JButton();
        jButton35 = new javax.swing.JButton();
        jPanel34 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextArea3 = new javax.swing.JTextArea();
        jButton36 = new javax.swing.JButton();
        jButton37 = new javax.swing.JButton();
        jLabel42 = new javax.swing.JLabel();
        jButton33 = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jLabel43 = new javax.swing.JLabel();
        jScrollPane12 = new javax.swing.JScrollPane();
        jButton58 = new javax.swing.JButton();
        jButton59 = new javax.swing.JButton();
        jButton60 = new javax.swing.JButton();
        jButton61 = new javax.swing.JButton();
        jButton62 = new javax.swing.JButton();
        jLabel55 = new javax.swing.JLabel();
        jScrollPane13 = new javax.swing.JScrollPane();
        jButton63 = new javax.swing.JButton();
        jButton64 = new javax.swing.JButton();
        jButton65 = new javax.swing.JButton();
        jButton66 = new javax.swing.JButton();
        jButton67 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jLabel29 = new javax.swing.JLabel();
        jScrollPane10 = new javax.swing.JScrollPane();
        shopsTable = new javax.swing.JTable();
        jButton48 = new javax.swing.JButton();
        jButton49 = new javax.swing.JButton();
        jButton50 = new javax.swing.JButton();
        jLabel34 = new javax.swing.JLabel();
        jScrollPanel11 = new javax.swing.JScrollPane();
        shopItemsTable = new javax.swing.JTable();
        jButton51 = new javax.swing.JButton();
        jButton52 = new javax.swing.JButton();
        jButton53 = new javax.swing.JButton();
        jButton54 = new javax.swing.JButton();
        jButton55 = new javax.swing.JButton();
        jButton56 = new javax.swing.JButton();
        jButton57 = new javax.swing.JButton();
        jPanel17 = new javax.swing.JPanel();
        jScrollPane9 = new javax.swing.JScrollPane();
        cashshopItemsTable = new javax.swing.JTable();
        jButton5 = new javax.swing.JButton();
        jButton9 = new javax.swing.JButton();
        jButton21 = new javax.swing.JButton();
        jButton44 = new javax.swing.JButton();
        jButton45 = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        jLabel30 = new javax.swing.JLabel();
        jButton68 = new javax.swing.JButton();
        jPanel19 = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        jButton25 = new javax.swing.JButton();
        jButton26 = new javax.swing.JButton();
        jButton19 = new javax.swing.JButton();
        jPanel20 = new javax.swing.JPanel();
        jLabel32 = new javax.swing.JLabel();
        jButton28 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jPanel21 = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        jButton29 = new javax.swing.JButton();
        jButton30 = new javax.swing.JButton();
        jButton31 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jTextField10 = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jTextField11 = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel48 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jLabel49 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jLabel50 = new javax.swing.JLabel();
        jTextField15 = new javax.swing.JTextField();
        jLabel53 = new javax.swing.JLabel();
        jTextField23 = new javax.swing.JTextField();
        jLabel57 = new javax.swing.JLabel();
        jTextField27 = new javax.swing.JTextField();
        jLabel61 = new javax.swing.JLabel();
        jTextField28 = new javax.swing.JTextField();
        jLabel64 = new javax.swing.JLabel();
        jTextField29 = new javax.swing.JTextField();
        jPanel13 = new javax.swing.JPanel();
        jLabel23 = new javax.swing.JLabel();
        jTextField12 = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        jTextField13 = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        jTextField14 = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jTextField21 = new javax.swing.JTextField();
        jLabel52 = new javax.swing.JLabel();
        jTextField22 = new javax.swing.JTextField();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel54 = new javax.swing.JLabel();
        jLabel58 = new javax.swing.JLabel();
        jTextField24 = new javax.swing.JTextField();
        jLabel59 = new javax.swing.JLabel();
        jTextField25 = new javax.swing.JTextField();
        jLabel60 = new javax.swing.JLabel();
        jTextField26 = new javax.swing.JTextField();
        jCheckBox3 = new javax.swing.JCheckBox();
        jCheckBox4 = new javax.swing.JCheckBox();
        jCheckBox5 = new javax.swing.JCheckBox();
        jCheckBox6 = new javax.swing.JCheckBox();
        jCheckBox9 = new javax.swing.JCheckBox();
        jCheckBox10 = new javax.swing.JCheckBox();
        jCheckBox8 = new javax.swing.JCheckBox();
        jCheckBox11 = new javax.swing.JCheckBox();
        jComboBox4 = new javax.swing.JComboBox();
        jLabel27 = new javax.swing.JLabel();
        jToggleButton1 = new javax.swing.JToggleButton();
        jPanel16 = new javax.swing.JPanel();
        jButton17 = new javax.swing.JButton();
        jButton16 = new javax.swing.JButton();
        jButton15 = new javax.swing.JButton();
        jButton18 = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jTextField8 = new javax.swing.JTextField();
        jLabel41 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jPanel28 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        logo = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jLabel56 = new javax.swing.JLabel();
        jLabel62 = new javax.swing.JLabel();
        jLabel63 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();

        AppIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/Icon.png"))); // NOI18N
        AppIcon.setText("jLabel56");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("真正楓之谷伺服端");
        setMaximumSize(new java.awt.Dimension(765, 631));
        setMinimumSize(new java.awt.Dimension(765, 631));

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(
                org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, AppIcon,
                org.jdesktop.beansbinding.ELProperty.create("${icon.image}"), this,
                org.jdesktop.beansbinding.BeanProperty.create("iconImage"));
        bindingGroup.addBinding(binding);

        jPanel9.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        jLabel3.setText("伺服器");

        worldList.setModel(getWorldModel());
        worldList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                worldListActionPerformed(evt);
            }
        });

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel5.setText("更變倍率");

        jLabel7.setText("經驗倍率");

        changeExpRate.setText("更變");
        changeExpRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeExpRateActionPerformed(evt);
            }
        });

        jLabel8.setText("掉寶倍率");

        changeDropRate.setText("更變");
        changeDropRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeDropRateActionPerformed(evt);
            }
        });

        jLabel9.setText("楓幣倍率");

        changeMesoRate.setText("更變");
        changeMesoRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeMesoRateActionPerformed(evt);
            }
        });

        jLabel10.setFont(jLabel10.getFont().deriveFont(jLabel10.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel10.setText("伺服器公告");

        jButton8.setText("更變");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        jLabel11.setText("發送遊戲公告");

        jLabel12.setText("頂部公告");

        sendNotice.setText("公告事項");
        sendNotice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendNoticeActionPerformed(evt);
            }
        });

        sendWinNotice.setText("視窗公告");
        sendWinNotice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendWinNoticeActionPerformed(evt);
            }
        });

        sendMsgNotice.setText("訊息");
        sendMsgNotice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendMsgNoticeActionPerformed(evt);
            }
        });

        sendNpcTalkNotice.setText("NPC對話");
        sendNpcTalkNotice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendNpcTalkNoticeActionPerformed(evt);
            }
        });

        jLabel13.setFont(jLabel13.getFont().deriveFont(jLabel13.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel13.setText("訊息輸出");

        jButton13.setText("清空訊息輸出");
        jButton13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton13ActionPerformed(evt);
            }
        });

        jButton14.setText("關閉訊息輸出");
        jButton14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton14ActionPerformed(evt);
            }
        });

        chatLog.setColumns(20);
        chatLog.setRows(5);
        jScrollPane4.setViewportView(chatLog);

        jButton10.setText("通用");
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jLabel26.setText("事件訊息");

        jButton11.setText("更變");
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        jButton12.setText("通用");
        jButton12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton12ActionPerformed(evt);
            }
        });

        changeExpRate1.setText("通用");
        changeExpRate1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeExpRate1ActionPerformed(evt);
            }
        });

        changeDropRate1.setText("通用");
        changeDropRate1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeDropRate1ActionPerformed(evt);
            }
        });

        changeMesoRate1.setText("通用");
        changeMesoRate1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeMesoRate1ActionPerformed(evt);
            }
        });

        jLabel44.setText("發送方式");

        jLabel46.setText("狀態");

        jButton27.setText("更變");
        jButton27.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton27ActionPerformed(evt);
            }
        });

        jButton38.setText("通用");
        jButton38.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton38ActionPerformed(evt);
            }
        });

        jLabel47.setText("頻道總數");

        jButton39.setText("更變");
        jButton39.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton39ActionPerformed(evt);
            }
        });

        jButton40.setText("通用");
        jButton40.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton40ActionPerformed(evt);
            }
        });

        show.setText("不顯");
        show.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showActionPerformed(evt);
            }
        });

        available.setText("關閉");
        available.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                availableActionPerformed(evt);
            }
        });

        jButton41.setText("儲存選中伺服器配置");
        jButton41.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton41ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel9Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel9Layout.createSequentialGroup()
                                        .addComponent(jButton13, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                                jButton14, javax.swing.GroupLayout.PREFERRED_SIZE, 364,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel9Layout.createSequentialGroup().addGroup(jPanel9Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel9Layout.createSequentialGroup().addComponent(jLabel3)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(worldList, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(show)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(available)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jLabel47)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(channelCount, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton39)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton40)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jLabel46)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(flag, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton27)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton38))
                                        .addGroup(jPanel9Layout.createSequentialGroup().addGroup(jPanel9Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel9Layout.createSequentialGroup().addGroup(jPanel9Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                false)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                jPanel9Layout.createSequentialGroup()
                                                                        .addComponent(jLabel9)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(mesoRate,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                76,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                jPanel9Layout.createSequentialGroup()
                                                                        .addComponent(jLabel8)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(dropRate,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                76,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel9Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addGroup(jPanel9Layout.createSequentialGroup()
                                                                        .addComponent(changeMesoRate)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(changeMesoRate1))
                                                                .addGroup(jPanel9Layout.createSequentialGroup()
                                                                        .addComponent(changeDropRate)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(changeDropRate1))))
                                                .addComponent(jLabel5)
                                                .addGroup(jPanel9Layout.createSequentialGroup().addComponent(jLabel7)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(expRate, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                76, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(changeExpRate)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(changeExpRate1))
                                                .addComponent(jLabel13))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(jPanel9Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel9Layout.createSequentialGroup()
                                                                .addComponent(jLabel44)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(sendNotice)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(sendWinNotice)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(sendMsgNotice)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(sendNpcTalkNotice))
                                                        .addGroup(jPanel9Layout.createSequentialGroup()
                                                                .addComponent(jLabel11)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(
                                                                        noticeText,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 352,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(jPanel9Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                        false)
                                                                .addGroup(jPanel9Layout.createSequentialGroup()
                                                                        .addComponent(jLabel10)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                        .addComponent(jButton41))
                                                                .addGroup(jPanel9Layout.createSequentialGroup()
                                                                        .addGroup(jPanel9Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(
                                                                                        jLabel26,
                                                                                        javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                .addComponent(jLabel12))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(jPanel9Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(scrollingMessage,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                        250,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(
                                                                                        worldTip,
                                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                        250,
                                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addGroup(jPanel9Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(jPanel9Layout
                                                                                        .createSequentialGroup()
                                                                                        .addComponent(
                                                                                                jButton11)
                                                                                        .addPreferredGap(
                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                        .addComponent(jButton12))
                                                                                .addGroup(
                                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                        jPanel9Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jButton8)
                                                                                                .addPreferredGap(
                                                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(
                                                                                                        jButton10)))))))
                                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 692,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap()));
        jPanel9Layout
                .setVerticalGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel9Layout.createSequentialGroup().addContainerGap().addGroup(jPanel9Layout
                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel3)
                                .addComponent(worldList, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel46)
                                .addComponent(flag, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton27).addComponent(jButton38).addComponent(jLabel47)
                                .addComponent(channelCount, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton39).addComponent(jButton40).addComponent(show)
                                .addComponent(available))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel5).addComponent(jLabel10).addComponent(jButton41))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(
                                        jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel9Layout.createSequentialGroup().addGroup(jPanel9Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel7)
                                                        .addComponent(expRate, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(changeExpRate).addComponent(changeExpRate1))
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel9Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.BASELINE)
                                                                .addComponent(jLabel8)
                                                                .addComponent(dropRate,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(changeDropRate)
                                                                .addComponent(changeDropRate1)))
                                                .addGroup(jPanel9Layout.createSequentialGroup().addGroup(jPanel9Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(scrollingMessage,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel12))
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel9Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.BASELINE)
                                                                .addComponent(
                                                                        worldTip, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(jLabel26)))
                                                .addGroup(jPanel9Layout.createSequentialGroup().addGroup(jPanel9Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jButton11).addComponent(jButton12))
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel9Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.BASELINE)
                                                                .addComponent(jButton8).addComponent(jButton10))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel9)
                                        .addComponent(mesoRate, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(changeMesoRate).addComponent(changeMesoRate1)
                                        .addComponent(jLabel11)
                                        .addComponent(noticeText, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel44).addComponent(sendNotice).addComponent(sendWinNotice)
                                        .addComponent(sendMsgNotice).addComponent(sendNpcTalkNotice)
                                        .addComponent(jLabel13))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton13).addComponent(jButton14))
                                .addContainerGap()));

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel2.setText("伺服器操作");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel14Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2))
                        .addContainerGap(67, Short.MAX_VALUE)));
        jPanel14Layout
                .setVerticalGroup(
                        jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel14Layout.createSequentialGroup().addContainerGap().addComponent(jLabel2)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addContainerGap()));

        jPanel15.setToolTipText("");
        jPanel15.setName(""); // NOI18N

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel1.setText("伺服端操作");

        jButton1.setText("啟動伺服端");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton3.setText("關閉伺服端");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("重載腳本");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton2.setText("重新啟動伺服端");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton42.setText("加載包頭檔案");
        jButton42.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton42ActionPerformed(evt);
            }
        });

        jButton43.setText("重新載入數據");
        jButton43.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton43ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout
                .setHorizontalGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel15Layout.createSequentialGroup().addContainerGap().addGroup(jPanel15Layout
                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel1)
                                .addGroup(jPanel15Layout.createSequentialGroup().addGroup(jPanel15Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel15Layout.createSequentialGroup()
                                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 117,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton2))
                                        .addGroup(jPanel15Layout.createSequentialGroup()
                                                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 117,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton43, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel15Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 117,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jButton42, javax.swing.GroupLayout.PREFERRED_SIZE, 117,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addContainerGap(379, Short.MAX_VALUE)));
        jPanel15Layout.setVerticalGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel15Layout.createSequentialGroup().addContainerGap().addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton1).addComponent(jButton3).addComponent(jButton2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton43).addComponent(jButton4).addComponent(jButton42))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup().addContainerGap()
                        .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(0, 0, 0)));

        jPanel15.getAccessibleContext().setAccessibleName("");

        jTabbedPane1.addTab("伺服器", jPanel1);

        jLabel39.setFont(jLabel39.getFont().deriveFont(jLabel39.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel39.setText("角色選擇");

        jLabel35.setText("狀態");

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "全部", "線上", "離線" }));
        jComboBox3.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox3ItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel22Layout.createSequentialGroup().addContainerGap().addComponent(jLabel35)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel22Layout.setVerticalGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel22Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel35).addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jLabel36.setText("等級");

        jTextField16.setText("0");
        jTextField16.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField16FocusLost(evt);
            }
        });

        jLabel37.setText("-");

        jTextField17.setText("255");
        jTextField17.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField17FocusLost(evt);
            }
        });
        jTextField17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField17ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel23Layout.createSequentialGroup().addContainerGap().addComponent(jLabel36)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField16, javax.swing.GroupLayout.PREFERRED_SIZE, 32,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel37)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField17, javax.swing.GroupLayout.PREFERRED_SIZE, 32,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel23Layout.setVerticalGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel23Layout.createSequentialGroup().addContainerGap().addGroup(jPanel23Layout
                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel36)
                        .addComponent(jTextField16, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel37).addComponent(jTextField17, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jLabel38.setText("性別");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "全部", "男", "女", "其他" }));
        jComboBox2.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBox2ItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel24Layout.createSequentialGroup().addContainerGap().addComponent(jLabel38)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel24Layout.setVerticalGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel24Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel38).addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jCheckBox1.setText("只搜尋正確一致的單字");

        jTextField18.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField18FocusLost(evt);
            }
        });
        jTextField18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField18ActionPerformed(evt);
            }
        });

        jButton32.setText("搜尋");
        jButton32.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton32ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel25Layout.createSequentialGroup().addContainerGap().addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField18, javax.swing.GroupLayout.PREFERRED_SIZE, 150,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton32)
                        .addContainerGap()));
        jPanel25Layout.setVerticalGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel25Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jCheckBox1)
                                .addComponent(jTextField18, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton32))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel5Layout.createSequentialGroup().addGap(2, 2, 2)
                        .addComponent(jPanel22, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel23, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel24, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel25, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(1, 1, 1)));
        jPanel5Layout.setVerticalGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPanel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jPanel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jPanel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jPanel25, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE));

        charTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {

        }, new String[] { "狀態", "角色ID", "賬號ID", "伺服器", "名稱", "等級", "經驗", "力量", "敏捷", "智力", "運氣", "HP", "MP", "最大HP",
                "最大MP", "楓幣", "職業", "皮膚", "性別", "人氣", "髮型", "臉型", "AP", "地圖", "管理員等級", "技能點數" }) {
            Class[] types = new Class[] { java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.String.class, java.lang.Short.class, java.lang.Long.class,
                    java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class,
                    java.lang.Long.class, java.lang.Short.class, java.lang.Short.class, java.lang.Byte.class,
                    java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Byte.class, java.lang.String.class };
            boolean[] canEdit = new boolean[] { false, false, false, false, false, false, false, false, false, false,
                    false, false, false, false, false, false, false, false, false, false, false, false, false, false,
                    false, false };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        charTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jScrollPane1.setViewportView(charTable);

        jCheckBox2.setText("對全體線上角色操作(無視下面列表的選擇)");

        jButton46.setText("<< 上一頁");
        jButton46.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton46ActionPerformed(evt);
            }
        });

        jButton47.setText("下一頁 >>");
        jButton47.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton47ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel26Layout = new javax.swing.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel26Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel26Layout.createSequentialGroup().addComponent(jLabel39)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jCheckBox2).addGap(338, 338, 338)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(jPanel26Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 715,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                        jPanel26Layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton46, javax.swing.GroupLayout.PREFERRED_SIZE, 344,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton47, javax.swing.GroupLayout.PREFERRED_SIZE, 369,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));
        jPanel26Layout.setVerticalGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel26Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel39).addComponent(jCheckBox2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton46).addComponent(jButton47))
                        .addContainerGap()));

        jLabel40.setText("發送道具");

        jTextField19.setText("道具代碼");

        jTextField20.setText("數量");

        jButton34.setText("發送");
        jButton34.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton34ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel33Layout = new javax.swing.GroupLayout(jPanel33);
        jPanel33.setLayout(jPanel33Layout);
        jPanel33Layout.setHorizontalGroup(jPanel33Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel33Layout.createSequentialGroup().addContainerGap().addComponent(jLabel40)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField19, javax.swing.GroupLayout.PREFERRED_SIZE, 69,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jTextField20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton34)
                        .addContainerGap()));
        jPanel33Layout
                .setVerticalGroup(jPanel33Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel33Layout.createSequentialGroup().addContainerGap().addGroup(jPanel33Layout
                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel40)
                                .addComponent(jTextField19, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField20, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton34))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jButton35.setText("踢下線");
        jButton35.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton35ActionPerformed(evt);
            }
        });

        jTextArea3.setColumns(20);
        jTextArea3.setLineWrap(true);
        jTextArea3.setRows(5);
        jScrollPane3.setViewportView(jTextArea3);

        jButton36.setText("發送數據包");
        jButton36.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton36ActionPerformed(evt);
            }
        });

        jButton37.setText("發送檔案數據包");
        jButton37.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton37ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel34Layout = new javax.swing.GroupLayout(jPanel34);
        jPanel34.setLayout(jPanel34Layout);
        jPanel34Layout.setHorizontalGroup(jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel34Layout.createSequentialGroup().addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(jPanel34Layout.createSequentialGroup()
                                        .addComponent(jButton36, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton37, javax.swing.GroupLayout.PREFERRED_SIZE, 228,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 466,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 10, Short.MAX_VALUE)));
        jPanel34Layout.setVerticalGroup(jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel34Layout.createSequentialGroup().addContainerGap()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel34Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton36).addComponent(jButton37))));

        jLabel42.setFont(jLabel42.getFont().deriveFont(jLabel42.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel42.setText("操作");

        jButton33.setText("更改屬性");
        jButton33.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton33ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel27Layout.createSequentialGroup().addContainerGap().addGroup(jPanel27Layout
                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel27Layout.createSequentialGroup().addComponent(jLabel42).addGap(226, 226,
                                        226))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                        jPanel27Layout.createSequentialGroup()
                                                .addComponent(jPanel33, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(jPanel27Layout.createSequentialGroup().addComponent(jButton35)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton33)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addComponent(jPanel34, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap()));
        jPanel27Layout.setVerticalGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel27Layout.createSequentialGroup().addContainerGap().addGroup(jPanel27Layout
                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel34, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel27Layout.createSequentialGroup().addComponent(jLabel42)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton35).addComponent(jButton33))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20,
                                        Short.MAX_VALUE)
                                .addComponent(jPanel33, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)))));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel27, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jPanel26, javax.swing.GroupLayout.PREFERRED_SIZE, 728,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup().addContainerGap()
                        .addComponent(jPanel26, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel27, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap()));

        jTabbedPane1.addTab("角色操作", jPanel2);

        jLabel43.setFont(new java.awt.Font("PMingLiU", 1, 12)); // NOI18N
        jLabel43.setText("怪物掉寶");

        jButton58.setText("<< 上一頁");

        jButton59.setText("下一頁 >>");
        jButton59.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton59ActionPerformed(evt);
            }
        });

        jButton60.setText("新增");

        jButton61.setText("更變");

        jButton62.setText("移除");

        jLabel55.setFont(new java.awt.Font("PMingLiU", 1, 12)); // NOI18N
        jLabel55.setText("全域怪物掉寶");

        jButton63.setText("新增");

        jButton64.setText("更變");

        jButton65.setText("移除");

        jButton66.setText("<< 上一頁");

        jButton67.setText("下一頁 >>");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel7Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jScrollPane13).addComponent(jScrollPane12)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                        jPanel7Layout.createSequentialGroup().addComponent(jButton60)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton61)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton62)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jButton58)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton59))
                                .addGroup(jPanel7Layout.createSequentialGroup()
                                        .addGroup(jPanel7Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel43).addComponent(jLabel55))
                                        .addGap(0, 0, Short.MAX_VALUE))
                                .addGroup(jPanel7Layout.createSequentialGroup().addComponent(jButton63)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton64)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton65)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jButton66)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton67)))
                        .addContainerGap()));
        jPanel7Layout.setVerticalGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel7Layout.createSequentialGroup().addContainerGap().addComponent(jLabel43)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane12, javax.swing.GroupLayout.PREFERRED_SIZE, 280,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton58).addComponent(jButton59).addComponent(jButton60)
                                .addComponent(jButton61).addComponent(jButton62))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jLabel55)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane13, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton63).addComponent(jButton64).addComponent(jButton65)
                                .addComponent(jButton66).addComponent(jButton67))
                        .addContainerGap()));

        jTabbedPane1.addTab("怪物掉寶", jPanel7);

        jLabel29.setFont(new java.awt.Font("PMingLiU", 1, 12)); // NOI18N
        jLabel29.setText("商店");

        shopsTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {

        }, new String[] { "商店ID", "NPCID" }) {
            Class[] types = new Class[] { java.lang.Integer.class, java.lang.Integer.class };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        jScrollPane10.setViewportView(shopsTable);

        jButton48.setText("新增");

        jButton49.setText("編輯");

        jButton50.setText("移除");

        jLabel34.setFont(new java.awt.Font("PMingLiU", 1, 12)); // NOI18N
        jLabel34.setText("商店商品");

        shopItemsTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {

        }, new String[] { "商店ID", "道具ID", "道具名稱", "價格", "位置", "物品兌換ID", "物品兌換數量", "期限" }) {
            Class[] types = new Class[] { java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class,
                    java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }
        });
        shopItemsTable.setToolTipText("");
        jScrollPanel11.setViewportView(shopItemsTable);

        jButton51.setText("新增");

        jButton52.setText("編輯");

        jButton53.setText("移除");

        jButton54.setText("<< 上一頁");

        jButton55.setText("下一頁 >>");

        jButton56.setText("<< 上一頁");

        jButton57.setText("下一頁 >>");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel8Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel8Layout.createSequentialGroup().addGroup(jPanel8Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel29)
                                        .addGroup(jPanel8Layout.createSequentialGroup().addComponent(jButton48)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton49)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton50))
                                        .addComponent(
                                                jScrollPane10, javax.swing.GroupLayout.PREFERRED_SIZE, 0,
                                                Short.MAX_VALUE))
                                        .addGroup(jPanel8Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel8Layout
                                                        .createSequentialGroup().addGap(9, 9, 9).addComponent(jLabel34)
                                                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE))
                                                .addGroup(jPanel8Layout.createSequentialGroup()
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel8Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jScrollPanel11,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 0,
                                                                        Short.MAX_VALUE)
                                                                .addGroup(jPanel8Layout.createSequentialGroup()
                                                                        .addComponent(jButton51)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(jButton52)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(jButton53)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                        .addComponent(jButton56)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(jButton57)))
                                                        .addContainerGap())))
                                .addGroup(jPanel8Layout.createSequentialGroup()
                                        .addComponent(jButton54, javax.swing.GroupLayout.PREFERRED_SIZE, 91,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton55, javax.swing.GroupLayout.PREFERRED_SIZE, 87,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))));
        jPanel8Layout.setVerticalGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel8Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel29).addComponent(jLabel34))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel8Layout.createSequentialGroup()
                                        .addComponent(jScrollPane10, javax.swing.GroupLayout.DEFAULT_SIZE, 467,
                                                Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel8Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jButton54).addComponent(jButton55)))
                                .addComponent(jScrollPanel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton48).addComponent(jButton49).addComponent(jButton50)
                                .addComponent(jButton51).addComponent(jButton52).addComponent(jButton53)
                                .addComponent(jButton56).addComponent(jButton57))
                        .addContainerGap()));

        jTabbedPane1.addTab("商店", jPanel8);

        cashshopItemsTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] {

        }, new String[] { "SN", "備註", "道具ID", "數量", "折後價格", "Period", "性別", "在售", "Class", "主頁推薦" }) {
            Class[] types = new Class[] { java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class,
                    java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class };
            boolean[] canEdit = new boolean[] { false, false, false, false, false, false, false, false, false, false };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane9.setViewportView(cashshopItemsTable);

        jButton5.setText("增加");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton9.setText("刪除");
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jButton21.setText("編輯");
        jButton21.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton21ActionPerformed(evt);
            }
        });

        jButton44.setText("<< 上一頁");
        jButton44.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton44ActionPerformed(evt);
            }
        });

        jButton45.setText("下一頁 >>");
        jButton45.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton45ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel17Layout.createSequentialGroup().addContainerGap().addGroup(jPanel17Layout
                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane9, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addGroup(jPanel17Layout.createSequentialGroup().addComponent(jButton5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton21)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton44)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton45)))
                        .addContainerGap()));
        jPanel17Layout.setVerticalGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel17Layout.createSequentialGroup().addContainerGap()
                        .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 517, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton5).addComponent(jButton21).addComponent(jButton9)
                                .addComponent(jButton44).addComponent(jButton45))
                        .addContainerGap()));

        jTabbedPane1.addTab("商城", jPanel17);

        jLabel30.setFont(jLabel30.getFont().deriveFont(jLabel30.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel30.setText("轉存數據");

        jButton68.setText("更新數據庫WZ");
        jButton68.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton68ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel18Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel30).addComponent(jButton68))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel18Layout.setVerticalGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel18Layout.createSequentialGroup().addContainerGap().addComponent(jLabel30)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton68)
                        .addContainerGap(39, Short.MAX_VALUE)));

        jLabel31.setFont(jLabel31.getFont().deriveFont(jLabel31.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel31.setText("修正");

        jButton25.setText("商店商品價格過低");
        jButton25.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton25ActionPerformed(evt);
            }
        });

        jButton26.setText("數據庫編碼");
        jButton26.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton26ActionPerformed(evt);
            }
        });

        jButton19.setText("修正JavaScript");
        jButton19.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton19ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel19Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel31)
                                .addGroup(jPanel19Layout.createSequentialGroup().addComponent(jButton25)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton26)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton19)))
                        .addContainerGap(15, Short.MAX_VALUE)));
        jPanel19Layout.setVerticalGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel19Layout.createSequentialGroup().addContainerGap().addComponent(jLabel31)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton25).addComponent(jButton26).addComponent(jButton19))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jLabel32.setFont(jLabel32.getFont().deriveFont(jLabel32.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel32.setText("解數據包");

        jButton28.setText("輔助解包");
        jButton28.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton28ActionPerformed(evt);
            }
        });

        jButton7.setText("腳本解包");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel20Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel32)
                                .addGroup(jPanel20Layout.createSequentialGroup().addComponent(jButton28)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton7)))
                        .addContainerGap(164, Short.MAX_VALUE)));
        jPanel20Layout.setVerticalGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel20Layout.createSequentialGroup().addContainerGap().addComponent(jLabel32)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton28).addComponent(jButton7))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jLabel33.setFont(jLabel33.getFont().deriveFont(jLabel33.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel33.setText("其他");

        jButton29.setText("BUFF訊息導出");
        jButton29.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton29ActionPerformed(evt);
            }
        });

        jButton30.setText("包頭轉換");
        jButton30.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton30ActionPerformed(evt);
            }
        });

        jButton31.setText("代碼檢索器");
        jButton31.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton31ActionPerformed(evt);
            }
        });

        jButton6.setText("基址計算器");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel21Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel33)
                                .addGroup(jPanel21Layout.createSequentialGroup().addComponent(jButton29)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton30)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton31))
                                .addComponent(jButton6))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel21Layout.setVerticalGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel21Layout.createSequentialGroup().addContainerGap().addComponent(jLabel33)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton29).addComponent(jButton30).addComponent(jButton31))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton6)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel18, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel21, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel21, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(355, Short.MAX_VALUE)));

        jTabbedPane1.addTab("工具", jPanel3);

        jLabel20.setText("事件訊息");

        jLabel21.setText("頂部公告");

        jLabel24.setText("頻道總數");

        jLabel18.setFont(jLabel18.getFont().deriveFont(jLabel18.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel18.setText("通用伺服器");

        jLabel4.setText("經驗倍率");

        jLabel48.setText("楓幣倍率");

        jLabel49.setText("掉寶倍率");

        jLabel50.setText("狀態");

        jLabel53.setText("登入埠");

        jLabel57.setText("CH起始埠");

        jLabel61.setText("商城埠");

        jLabel64.setText("農場埠");

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout
                .setHorizontalGroup(
                        jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                                jPanel12Layout.createSequentialGroup().addContainerGap().addGroup(jPanel12Layout
                                        .createParallelGroup(
                                                javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel12Layout.createSequentialGroup().addComponent(jLabel20)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, 256,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(
                                                jPanel12Layout.createSequentialGroup().addComponent(
                                                        jLabel21)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jTextField10,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 256,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jLabel18)
                                        .addGroup(jPanel12Layout.createSequentialGroup().addGroup(jPanel12Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addGroup(jPanel12Layout
                                                        .createSequentialGroup().addComponent(jLabel4)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(
                                                                jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 50,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGap(4, 4, 4).addComponent(jLabel48))
                                                .addGroup(jPanel12Layout.createSequentialGroup().addGroup(
                                                        jPanel12Layout.createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jLabel24).addComponent(jLabel53))
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel12Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addGroup(jPanel12Layout.createSequentialGroup()
                                                                        .addComponent(jTextField23,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(jLabel57)
                                                                        .addGap(0, 0, Short.MAX_VALUE))
                                                                .addGroup(jPanel12Layout
                                                                        .createSequentialGroup()
                                                                        .addComponent(jTextField11,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(
                                                                                jLabel50,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)))))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel12Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel12Layout.createSequentialGroup()
                                                                .addComponent(jTextField4,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 50,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel49))
                                                        .addGroup(jPanel12Layout.createSequentialGroup()
                                                                .addGroup(jPanel12Layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING,
                                                                        false)
                                                                        .addComponent(jTextField15,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                50, Short.MAX_VALUE)
                                                                        .addComponent(jTextField27))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(jPanel12Layout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel61)
                                                                        .addComponent(jLabel64))))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel12Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel12Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jTextField5,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(jTextField28,
                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addComponent(jTextField29,
                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))
                                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel12Layout.setVerticalGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel12Layout.createSequentialGroup().addContainerGap().addComponent(jLabel18)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel20))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel21)
                                .addComponent(jTextField10, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(10, 10, 10)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel4)
                                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel48)
                                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel49).addComponent(jTextField5,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel50)
                                        .addComponent(jTextField15, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel61).addComponent(jTextField28,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(jLabel24))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField23, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel53)
                                .addComponent(jTextField29, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel64).addComponent(jLabel57).addComponent(jTextField27,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jLabel23.setText("IP地址");

        jLabel19.setText("伺服名稱");

        jTextField13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField13ActionPerformed(evt);
            }
        });

        jLabel25.setText("最大登入角色數限制");

        jLabel22.setFont(jLabel22.getFont().deriveFont(jLabel22.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel22.setText("伺服端");

        jLabel51.setText("頻道顯示最大角色數");

        jLabel52.setText("楓之谷版本");

        jTextField22.setEditable(false);
        jTextField22.setText(String.valueOf(ServerConstants.MAPLE_VERSION) + "." + ServerConstants.MAPLE_PATCH);

        jComboBox1.setModel(getMapleTypeModel());
        jComboBox1.setEnabled(false);

        jLabel54.setText("所在國家/地區");

        jLabel58.setText("楓鯊檔案版本");

        jLabel59.setText("方塊跳框倍率");

        jLabel60.setText("商店折扣");

        jCheckBox3.setText("測試機");

        jCheckBox4.setText("下線回到選角色");

        jCheckBox5.setText("僅管理員模式");

        jCheckBox6.setText("本地模式");

        jCheckBox9.setText("咒文痕跡FEVER TIME");

        jCheckBox10.setText("日誌模式");

        jCheckBox8.setText("自動輸出楓鯊記錄");

        jCheckBox11.setText("自動註冊");

        jComboBox4.setModel(getJobConstantModel());
        jComboBox4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox4ActionPerformed(evt);
            }
        });

        jLabel27.setText("職業開關");

        jToggleButton1.setText("關閉");
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel13Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel22)
                                .addGroup(jPanel13Layout.createSequentialGroup()
                                        .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 60,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel25)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextField14, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel13Layout.createSequentialGroup().addComponent(jLabel52)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextField22, javax.swing.GroupLayout.PREFERRED_SIZE, 60,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jLabel54)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jCheckBox3))
                                .addGroup(jPanel13Layout.createSequentialGroup().addGroup(jPanel13Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel13Layout
                                                .createSequentialGroup().addComponent(jLabel58)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jTextField25, javax.swing.GroupLayout.PREFERRED_SIZE, 88,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel13Layout
                                                .createSequentialGroup()
                                                .addGroup(jPanel13Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel60, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                60, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel13Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                false)
                                                        .addComponent(jTextField13,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jTextField26,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))))
                                        .addGap(4, 4, 4)
                                        .addGroup(jPanel13Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addGroup(jPanel13Layout.createSequentialGroup().addGroup(jPanel13Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel51).addComponent(jLabel59))
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(jPanel13Layout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jTextField21,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(
                                                                        jTextField24,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGroup(jPanel13Layout.createSequentialGroup().addComponent(jLabel27)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jComboBox4,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 97,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jToggleButton1))))
                                .addGroup(jPanel13Layout.createSequentialGroup()
                                        .addGroup(jPanel13Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jCheckBox9).addComponent(jCheckBox8))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel13Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel13Layout.createSequentialGroup()
                                                        .addComponent(jCheckBox11)
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jCheckBox10))
                                                .addComponent(jCheckBox4))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel13Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jCheckBox5).addComponent(jCheckBox6))))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel13Layout.setVerticalGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel13Layout.createSequentialGroup().addContainerGap().addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel52)
                                .addComponent(jTextField22, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel54)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jCheckBox3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel19)
                                .addComponent(jTextField12, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel25).addComponent(jTextField14,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel23)
                                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jTextField13, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel51).addComponent(jTextField21,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jTextField24, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel59).addComponent(jLabel60).addComponent(jTextField26,
                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel58)
                                .addComponent(jTextField25, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel27).addComponent(jToggleButton1).addComponent(jComboBox4,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jCheckBox8).addComponent(jCheckBox11).addComponent(jCheckBox10)
                                .addComponent(jCheckBox6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jCheckBox9).addComponent(jCheckBox4).addComponent(jCheckBox5))
                        .addContainerGap(202, Short.MAX_VALUE)));

        jButton17.setText("應用更變");
        jButton17.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton17ActionPerformed(evt);
            }
        });

        jButton16.setText("放棄更變");
        jButton16.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton16ActionPerformed(evt);
            }
        });

        jButton15.setText("儲存並應用");
        jButton15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton15ActionPerformed(evt);
            }
        });

        jButton18.setText("讀取配置檔案");
        jButton18.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton18ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel16Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jButton17, javax.swing.GroupLayout.PREFERRED_SIZE, 191,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton18, javax.swing.GroupLayout.PREFERRED_SIZE, 191,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jButton15, javax.swing.GroupLayout.PREFERRED_SIZE, 191,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton16, javax.swing.GroupLayout.PREFERRED_SIZE, 191,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))));
        jPanel16Layout.setVerticalGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel16Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton17, javax.swing.GroupLayout.PREFERRED_SIZE, 37,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton16, javax.swing.GroupLayout.PREFERRED_SIZE, 37,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton15, javax.swing.GroupLayout.PREFERRED_SIZE, 37,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jButton18, javax.swing.GroupLayout.PREFERRED_SIZE, 37,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel15.setText("用戶名");

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel16.setText("密碼");

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel17.setText("連接埠");

        jLabel41.setText("數據庫");

        jLabel14.setFont(jLabel14.getFont().deriveFont(jLabel14.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel14.setText("數據庫");

        jLabel6.setText("IP");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel11Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel11Layout.createSequentialGroup().addComponent(jLabel14)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel6)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(
                                                jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 237,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel11Layout.createSequentialGroup().addGroup(jPanel11Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel11Layout
                                                .createSequentialGroup().addComponent(jLabel15)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 119,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel11Layout
                                                .createSequentialGroup()
                                                .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 36,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 119,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(jPanel11Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel11Layout.createSequentialGroup()
                                                        .addPreferredGap(
                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jLabel41))
                                                .addGroup(jPanel11Layout.createSequentialGroup().addGap(1, 1, 1)
                                                        .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel11Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                .addComponent(jTextField8).addComponent(jTextField1,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 103,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel11Layout.setVerticalGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel11Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel14).addComponent(jLabel6).addComponent(jTextField2,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel15)
                                .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel17)
                                .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(9, 9, 9)
                        .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel16)
                                .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel41).addComponent(jTextField1,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(31, 31, 31)));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel12, javax.swing.GroupLayout.Alignment.TRAILING,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jPanel11, javax.swing.GroupLayout.Alignment.TRAILING,
                                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addContainerGap()));

        jTabbedPane1.addTab("設置", jPanel4);

        javax.swing.GroupLayout jPanel28Layout = new javax.swing.GroupLayout(jPanel28);
        jPanel28.setLayout(jPanel28Layout);
        jPanel28Layout.setHorizontalGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 760, Short.MAX_VALUE));
        jPanel28Layout.setVerticalGroup(jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGap(0, 566, Short.MAX_VALUE));

        jTabbedPane1.addTab("賓果", jPanel28);

        logo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        logo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/Logo.png"))); // NOI18N

        jLabel28.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel28.setText(
                "<html>\n<center><font size=\"6\"><b>真正楓之谷伺服器</b></font></center>\n<br>\n<center>Base on HelisiumDEV V148</center>\n<br>\n<center>程式界面由 潘先生(QQ:17498819) 編寫</center>\n<br>\n<center>相關BUG可以到QQ群裡討論, 群號碼185386815</center>\n<br>\n<center>此伺服器端模擬器由真正楓之谷團隊<small>(ZZMSTeam)</small>為楓之谷愛好者進行共同探討與研究JAVA相關而寫。</center>\n<br>\n<center>我們支持和鼓勵玩家在楓之谷官方進行遊戲，如果對閣下（任何個人、單位、團體、機關、企事業單位等）產生冒犯或者侵害，請您立即告知我們，我們將改正。</center>\n</html>");
        jLabel28.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        jLabel56.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel56.setText(
                "<html>\n<center><font size=\"5\" color=\"red\"><b>捐贈</b></font></center>\n<br>\n如果你覺得這個程式還不錯可以通過捐贈請我喝一瓶汽水\n</html>");

        jLabel62.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/donate_icon_dollar_48x48.png"))); // NOI18N
        jLabel62.setText("<html><a target=\"_blank\" href=\"http://goo.gl/EbHIYK\">USD Donate</a></html>");
        jLabel62.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabel62.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel62MouseClicked(evt);
            }
        });

        jLabel63.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/donate_icon_rmb_48x48.png"))); // NOI18N
        jLabel63.setText("<html><a target=\"_blank\" href=\"http://goo.gl/EbHIYK\">CNY Donate</a></html>");
        jLabel63.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jLabel63.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel63MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel10Layout.createSequentialGroup().addContainerGap().addComponent(jLabel56)
                        .addContainerGap())
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                        jPanel10Layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel62, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(34, 34, 34)
                                .addComponent(jLabel63, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(234, 234, 234)));
        jPanel10Layout.setVerticalGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel10Layout.createSequentialGroup()
                        .addComponent(jLabel56, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel62, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel63, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.PREFERRED_SIZE))));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel6Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(logo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
                                .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap()));
        jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel6Layout.createSequentialGroup().addContainerGap()
                        .addComponent(logo, javax.swing.GroupLayout.PREFERRED_SIZE, 145,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap()));

        logo.getAccessibleContext().setAccessibleDescription("");
        jLabel28.getAccessibleContext().setAccessibleName("");

        jTabbedPane1.addTab("關於", jPanel6);

        jLabel45.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel45.setText("Generated Code write by NetBeans IDE.This ServerManager that made for ZZMS");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jLabel45,
                        javax.swing.GroupLayout.DEFAULT_SIZE, 755, Short.MAX_VALUE))
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 595,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel45, javax.swing.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)));

        getAccessibleContext().setAccessibleName("ZZMS");

        bindingGroup.bind();

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton18ActionPerformed
        resetSetting(true);
    }// GEN-LAST:event_jButton18ActionPerformed

    private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton15ActionPerformed
        updateSetting(true);
    }// GEN-LAST:event_jButton15ActionPerformed

    private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton16ActionPerformed
        resetSetting(false);
    }// GEN-LAST:event_jButton16ActionPerformed

    private void jButton17ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton17ActionPerformed
        updateSetting(false);
    }// GEN-LAST:event_jButton17ActionPerformed

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jToggleButton1ActionPerformed
        JobConstants.LoginJob.valueOf((String) jComboBox4.getSelectedItem())
                .setEnableCreate(jToggleButton1.isSelected());
        jToggleButton1.setText(jToggleButton1.isSelected() ? "開啟" : "關閉");
        JOptionPane.showMessageDialog(null, "更變成功。");
    }// GEN-LAST:event_jToggleButton1ActionPerformed

    private void jComboBox4ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jComboBox4ActionPerformed
        JobConstants.LoginJob j = JobConstants.LoginJob.valueOf((String) jComboBox4.getSelectedItem());
        jToggleButton1.setSelected(j.enableCreate());
        jToggleButton1.setText(jToggleButton1.isSelected() ? "開啟" : "關閉");
    }// GEN-LAST:event_jComboBox4ActionPerformed

    private void jTextField13ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jTextField13ActionPerformed

    }// GEN-LAST:event_jTextField13ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton6ActionPerformed
        openWindow(Windows.BuffStatusCalculator);
    }// GEN-LAST:event_jButton6ActionPerformed

    private void jButton31ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton31ActionPerformed
        openWindow(Windows.SearchGenerator);
        if (!LoginServer.isShutdown() || searchServer) {
            return;
        }
        searchServer = true;
        if (server == null) {
            server = new Thread() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, "因未啟用伺服器, 直接啟用工具需要加載訊息消耗一定時間才能檢索除地圖跟怪物外的其他內容, 請點選確定繼續。");
                    MapleQuest.initQuests(false);
                    MapleItemInformationProvider.getInstance().runItems(false);
                    SkillFactory.load(false);
                    MapleLifeFactory.loadQuestCounts(false);
                    JOptionPane.showMessageDialog(null, "訊息加載完成, 現在可以檢索全部內容了。");
                    server = null;
                }
            };
            server.start();
        } else {
            JOptionPane.showMessageDialog(null, "正在執行中。");
        }
    }// GEN-LAST:event_jButton31ActionPerformed

    private void jButton30ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton30ActionPerformed
        openWindow(Windows.ConvertOpcodes);
    }// GEN-LAST:event_jButton30ActionPerformed

    private void jButton29ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton29ActionPerformed
        runTool(Tools.BuffInformation);
    }// GEN-LAST:event_jButton29ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton7ActionPerformed
        openWindow(Windows.ScriptExport);
    }// GEN-LAST:event_jButton7ActionPerformed

    private void jButton28ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton28ActionPerformed
        JOptionPane.showMessageDialog(null, "此功能未完成。");
    }// GEN-LAST:event_jButton28ActionPerformed

    private void jButton26ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton26ActionPerformed
        runTool(Tools.FixCharSets);
    }// GEN-LAST:event_jButton26ActionPerformed

    private void jButton25ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton25ActionPerformed
        runTool(Tools.FixShopItemsPrice);
    }// GEN-LAST:event_jButton25ActionPerformed

    private void jButton45ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton45ActionPerformed
        if (cashShopItemsPage < cashShopItems.size() / 100 - 1) {
            cashShopItemsPage++;
            initCashShopItemPannel();
        }
    }// GEN-LAST:event_jButton45ActionPerformed

    private void jButton44ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton44ActionPerformed
        if (cashShopItemsPage > 0) {
            cashShopItemsPage--;
            initCashShopItemPannel();
        }
    }// GEN-LAST:event_jButton44ActionPerformed

    private void jButton21ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton21ActionPerformed
        if (cashshopItemsTable.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(null, "未選擇商品。");
            return;
        }
        if (cashshopItemsTable.getSelectedRows().length > 1) {
            JOptionPane.showMessageDialog(null, "此操作不允許多選。");
            return;
        }
        int select = cashshopItemsTable.getSelectedRow();
        openWindow(Windows.CashShopItemEditor);
        int i = 0;
        ((CashShopItemEditor) windows.get(Windows.CashShopItemEditor)).setCashShopItem(
                new Object[]{cashshopItemsTable.getValueAt(select, i++), cashshopItemsTable.getValueAt(select, i++),
                    cashshopItemsTable.getValueAt(select, i++), cashshopItemsTable.getValueAt(select, i++),
                    cashshopItemsTable.getValueAt(select, i++), cashshopItemsTable.getValueAt(select, i++),
                    cashshopItemsTable.getValueAt(select, i++), cashshopItemsTable.getValueAt(select, i++),
                    cashshopItemsTable.getValueAt(select, i++), cashshopItemsTable.getValueAt(select, i++)});
    }// GEN-LAST:event_jButton21ActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton9ActionPerformed
        if (cashshopItemsTable.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(null, "未選擇商品。");
            return;
        }
        int[] rows = cashshopItemsTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            deleteCashShopItem((int) cashshopItemsTable.getValueAt(rows[i], 0));
        }
    }// GEN-LAST:event_jButton9ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton5ActionPerformed
        openWindow(Windows.CashShopItemAdder);
    }// GEN-LAST:event_jButton5ActionPerformed

    private void jButton59ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton59ActionPerformed

    }// GEN-LAST:event_jButton59ActionPerformed

    private void jButton33ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton33ActionPerformed
        JOptionPane.showMessageDialog(null, "此功能未完成。");
    }// GEN-LAST:event_jButton33ActionPerformed

    private void jButton37ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton37ActionPerformed
        MapleCharacter player = getSelectCharacter();
        if (player == null) {
            JOptionPane.showMessageDialog(null, "未選擇角色或者選擇的角色是離線狀態或不存在。");
        } else {
            player.getClient().getSession().writeAndFlush(LoadPacket.getPacket());
            JOptionPane.showMessageDialog(null, "操作成功。");
        }
    }// GEN-LAST:event_jButton37ActionPerformed

    private void jButton36ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton36ActionPerformed
        MapleCharacter player = getSelectCharacter();
        if (player == null) {
            JOptionPane.showMessageDialog(null, "未選擇角色或者選擇的角色是離線狀態或不存在。");
        } else {
            player.getClient().getSession().writeAndFlush(HexTool.getByteArrayFromHexString(jTextArea3.getText()));
            JOptionPane.showMessageDialog(null, "操作成功。");
        }
    }// GEN-LAST:event_jButton36ActionPerformed

    private void jButton35ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton35ActionPerformed
        MapleCharacter player = getSelectCharacter();
        if (player == null) {
            JOptionPane.showMessageDialog(null, "未選擇角色或者選擇的角色是離線狀態或不存在。");
        } else {
            player.getClient().disconnect(true, false);
            player.getClient().getSession().close();
            JOptionPane.showMessageDialog(null, "操作成功。");
        }
    }// GEN-LAST:event_jButton35ActionPerformed

    private void jButton34ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton34ActionPerformed
        String val_item = jTextField19.getText();
        String val_quan = jTextField20.getText();

        int item;
        short quan = 0;
        try {
            item = Integer.parseInt(val_item);
            quan = Short.parseShort(val_quan);
        } catch (NumberFormatException e) {
            item = 0;
        }
        if (item < 1 || quan < 1) {
            JOptionPane.showMessageDialog(null, "Debug:錯誤！");
            return;
        }

        MapleCharacter player = getSelectCharacter();
        if (player == null) {
            JOptionPane.showMessageDialog(null, "未選擇角色或者選擇的角色是離線狀態或不存在。");
        } else {
            player.gainItem(item, quan, "伺服器控制台發送道具");
            player.getClient().getSession().writeAndFlush(CWvsContext.broadcastMsg(1, "恭喜！獲得了運營員贈送的禮物。"));
            JOptionPane.showMessageDialog(null, "操作成功。");
        }
    }// GEN-LAST:event_jButton34ActionPerformed

    private void jButton32ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton32ActionPerformed
        this.refreshCharTable();
    }// GEN-LAST:event_jButton32ActionPerformed

    private void jButton43ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton43ActionPerformed
        JOptionPane.showMessageDialog(null, "從新載入數據會卡住比較久, 請點擊確定繼續。");
        MapleMonsterInformationProvider.getInstance().clearDrops();
        ReactorScriptManager.getInstance().clearDrops();
        JOptionPane.showMessageDialog(null, "數據載入完成。");
    }// GEN-LAST:event_jButton43ActionPerformed

    private void jButton42ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton42ActionPerformed
        RecvPacketOpcode.loadValues();
        SendPacketOpcode.loadValues();
        JOptionPane.showMessageDialog(null, "包頭加載完成。");
    }// GEN-LAST:event_jButton42ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton2ActionPerformed
        reStartServer();
    }// GEN-LAST:event_jButton2ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton4ActionPerformed
        PortalScriptManager.getInstance().clearScripts();
        ReactorScriptManager.getInstance().clearDrops();
        for (ChannelServer instance : ChannelServer.getAllInstances()) {
            instance.reloadEvents();
        }
        JOptionPane.showMessageDialog(null, "重載腳本成功。");
    }// GEN-LAST:event_jButton4ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton3ActionPerformed
        shutdownServer();
    }// GEN-LAST:event_jButton3ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton1ActionPerformed
        startServer();
        initCharacterPannel();
    }// GEN-LAST:event_jButton1ActionPerformed

    private void jButton41ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton41ActionPerformed
        ServerProperties.saveProperties();
    }// GEN-LAST:event_jButton41ActionPerformed

    private void availableActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_availableActionPerformed
        modifyServer(ServerModifyType.AVAILABLE, String.valueOf(available.isSelected()));
    }// GEN-LAST:event_availableActionPerformed

    private void showActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_showActionPerformed
        modifyServer(ServerModifyType.SHOW, String.valueOf(show.isSelected()));
    }// GEN-LAST:event_showActionPerformed

    private void jButton40ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton40ActionPerformed
        modifyServer(ServerModifyType.CHANNELS);
    }// GEN-LAST:event_jButton40ActionPerformed

    private void jButton39ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton39ActionPerformed
        modifyServer(ServerModifyType.CHANNELS, channelCount.getText());
    }// GEN-LAST:event_jButton39ActionPerformed

    private void jButton38ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton38ActionPerformed
        modifyServer(ServerModifyType.FLAG);
    }// GEN-LAST:event_jButton38ActionPerformed

    private void jButton27ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton27ActionPerformed
        modifyServer(ServerModifyType.FLAG, flag.getText());
    }// GEN-LAST:event_jButton27ActionPerformed

    private void changeMesoRate1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_changeMesoRate1ActionPerformed
        modifyServer(ServerModifyType.MESO);
    }// GEN-LAST:event_changeMesoRate1ActionPerformed

    private void changeDropRate1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_changeDropRate1ActionPerformed
        modifyServer(ServerModifyType.DROP);
    }// GEN-LAST:event_changeDropRate1ActionPerformed

    private void changeExpRate1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_changeExpRate1ActionPerformed
        modifyServer(ServerModifyType.EXP);
    }// GEN-LAST:event_changeExpRate1ActionPerformed

    private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton12ActionPerformed
        modifyServer(ServerModifyType.SCROLL_MSG);
    }// GEN-LAST:event_jButton12ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton11ActionPerformed
        modifyServer(ServerModifyType.SCROLL_MSG, scrollingMessage.getText());
    }// GEN-LAST:event_jButton11ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton10ActionPerformed
        modifyServer(ServerModifyType.WORLD_TIP);
    }// GEN-LAST:event_jButton10ActionPerformed

    private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton14ActionPerformed
        writeChatLog = !writeChatLog;
        jButton14.setText(writeChatLog ? "關閉訊息輸出" : "開啟訊息輸出");
    }// GEN-LAST:event_jButton14ActionPerformed

    private void jButton13ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton13ActionPerformed
        chatLog.setText("");
    }// GEN-LAST:event_jButton13ActionPerformed

    private void sendNpcTalkNoticeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_sendNpcTalkNoticeActionPerformed
        sendNotice(3);
    }// GEN-LAST:event_sendNpcTalkNoticeActionPerformed

    private void sendMsgNoticeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_sendMsgNoticeActionPerformed
        sendNotice(2);
    }// GEN-LAST:event_sendMsgNoticeActionPerformed

    private void sendWinNoticeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_sendWinNoticeActionPerformed
        sendNotice(1);
    }// GEN-LAST:event_sendWinNoticeActionPerformed

    private void sendNoticeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_sendNoticeActionPerformed
        sendNotice(0);
    }// GEN-LAST:event_sendNoticeActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton8ActionPerformed
        modifyServer(ServerModifyType.WORLD_TIP, worldTip.getText());
    }// GEN-LAST:event_jButton8ActionPerformed

    private void changeMesoRateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_changeMesoRateActionPerformed
        modifyServer(ServerModifyType.MESO, mesoRate.getText());
    }// GEN-LAST:event_changeMesoRateActionPerformed

    private void changeDropRateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_changeDropRateActionPerformed
        modifyServer(ServerModifyType.DROP, dropRate.getText());
    }// GEN-LAST:event_changeDropRateActionPerformed

    private void changeExpRateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_changeExpRateActionPerformed
        modifyServer(ServerModifyType.EXP, expRate.getText());
    }// GEN-LAST:event_changeExpRateActionPerformed

    private void worldListActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_worldListActionPerformed
        resetWorldPanel();
    }// GEN-LAST:event_worldListActionPerformed

    private void jButton68ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton68ActionPerformed
        runTool(Tools.UpdateSQLWZ);
    }// GEN-LAST:event_jButton68ActionPerformed

    private void jButton19ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton19ActionPerformed
        openWindow(Windows.FixJavaScript);
    }// GEN-LAST:event_jButton19ActionPerformed

    private void jLabel62MouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jLabel62MouseClicked
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("http://goo.gl/EbHIYK"));
        } catch (URISyntaxException | IOException ex) {
        }
    }// GEN-LAST:event_jLabel62MouseClicked

    private void jLabel63MouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_jLabel63MouseClicked
        openWindow(Windows.Donate);
    }// GEN-LAST:event_jLabel63MouseClicked

    private void jButton47ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton47ActionPerformed
        this.charTablePage += 1;
        this.refreshCharTable();
    }// GEN-LAST:event_jButton47ActionPerformed

    private void jButton46ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton46ActionPerformed
        this.charTablePage -= 1;
        if (charTablePage <= 0) {
            charTablePage = 1;
        }
        this.refreshCharTable(); // TODO add your handling code here:
    }// GEN-LAST:event_jButton46ActionPerformed

    private void jTextField17ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jTextField17ActionPerformed
    }// GEN-LAST:event_jTextField17ActionPerformed

    private void jTextField18ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jTextField18ActionPerformed
        this.charTableFilterName = jTextField18.getText();
        this.refreshCharTable();
    }// GEN-LAST:event_jTextField18ActionPerformed

    private void jTextField16FocusLost(java.awt.event.FocusEvent evt) {// GEN-FIRST:event_jTextField16FocusLost
        String text = jTextField16.getText();
        if (StringUtil.isNumber(text)) {
            this.charTableFilterMinLevel = Integer.parseInt(text);
            this.refreshCharTable();
        }
    }// GEN-LAST:event_jTextField16FocusLost

    private void jTextField17FocusLost(java.awt.event.FocusEvent evt) {// GEN-FIRST:event_jTextField17FocusLost
        String text = jTextField17.getText();
        if (StringUtil.isNumber(text)) {
            this.charTableFilterMaxLevel = Integer.parseInt(text);
            this.refreshCharTable();
        }
    }// GEN-LAST:event_jTextField17FocusLost

    private void jTextField18FocusLost(java.awt.event.FocusEvent evt) {// GEN-FIRST:event_jTextField18FocusLost
        String text = jTextField18.getText();
        this.charTableFilterName = text;
        this.refreshCharTable();
    }// GEN-LAST:event_jTextField18FocusLost

    private void jComboBox2ItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_jComboBox2ItemStateChanged
        this.charTableFilterGender = jComboBox2.getSelectedIndex();
        this.refreshCharTable();
    }// GEN-LAST:event_jComboBox2ItemStateChanged

    private void jComboBox3ItemStateChanged(java.awt.event.ItemEvent evt) {// GEN-FIRST:event_jComboBox3ItemStateChanged
        this.charTableFilterStatus = jComboBox3.getSelectedIndex();
        this.refreshCharTable();
    }// GEN-LAST:event_jComboBox3ItemStateChanged

    public static class Shutdown implements Runnable {

        @Override
        public void run() {
            if (MYSQL) {
                killProcess("mysqld.exe");
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        String regCode = Start.getRegCode();
        if (regCode == null || !GameConstants.regCodeCheck(regCode)) {
            RegCode regWin = new RegCode();
            regWin.setVisible(true);
            return;
        }
        if (ServerConstants.SYSTEM.equals("windows")) {
            MYSQL = runExe("MySQL/bin/mysqld.exe", "--character-set-server=UTF8");
        }
        WvsCenter.getInstance().initCharacterPannel();
        WvsCenter.getInstance().initCashShopItem();
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> WvsCenter.getInstance().setVisible(true));
    }

    @Override
    public void setVisible(boolean bln) {
        if (bln) {
            setLocationRelativeTo(null);
        }
        super.setVisible(bln);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel AppIcon;
    private javax.swing.JToggleButton available;
    private javax.swing.JTable cashshopItemsTable;
    private javax.swing.JButton changeDropRate;
    private javax.swing.JButton changeDropRate1;
    private javax.swing.JButton changeExpRate;
    private javax.swing.JButton changeExpRate1;
    private javax.swing.JButton changeMesoRate;
    private javax.swing.JButton changeMesoRate1;
    private javax.swing.JTextField channelCount;
    private javax.swing.JTable charTable;
    private javax.swing.JTextArea chatLog;
    private javax.swing.JTextField dropRate;
    private javax.swing.JTextField expRate;
    private javax.swing.JTextField flag;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton12;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton14;
    private javax.swing.JButton jButton15;
    private javax.swing.JButton jButton16;
    private javax.swing.JButton jButton17;
    private javax.swing.JButton jButton18;
    private javax.swing.JButton jButton19;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton21;
    private javax.swing.JButton jButton25;
    private javax.swing.JButton jButton26;
    private javax.swing.JButton jButton27;
    private javax.swing.JButton jButton28;
    private javax.swing.JButton jButton29;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton30;
    private javax.swing.JButton jButton31;
    private javax.swing.JButton jButton32;
    private javax.swing.JButton jButton33;
    private javax.swing.JButton jButton34;
    private javax.swing.JButton jButton35;
    private javax.swing.JButton jButton36;
    private javax.swing.JButton jButton37;
    private javax.swing.JButton jButton38;
    private javax.swing.JButton jButton39;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton40;
    private javax.swing.JButton jButton41;
    private javax.swing.JButton jButton42;
    private javax.swing.JButton jButton43;
    private javax.swing.JButton jButton44;
    private javax.swing.JButton jButton45;
    private javax.swing.JButton jButton46;
    private javax.swing.JButton jButton47;
    private javax.swing.JButton jButton48;
    private javax.swing.JButton jButton49;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton50;
    private javax.swing.JButton jButton51;
    private javax.swing.JButton jButton52;
    private javax.swing.JButton jButton53;
    private javax.swing.JButton jButton54;
    private javax.swing.JButton jButton55;
    private javax.swing.JButton jButton56;
    private javax.swing.JButton jButton57;
    private javax.swing.JButton jButton58;
    private javax.swing.JButton jButton59;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton60;
    private javax.swing.JButton jButton61;
    private javax.swing.JButton jButton62;
    private javax.swing.JButton jButton63;
    private javax.swing.JButton jButton64;
    private javax.swing.JButton jButton65;
    private javax.swing.JButton jButton66;
    private javax.swing.JButton jButton67;
    private javax.swing.JButton jButton68;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox10;
    private javax.swing.JCheckBox jCheckBox11;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBox jCheckBox4;
    private javax.swing.JCheckBox jCheckBox5;
    private javax.swing.JCheckBox jCheckBox6;
    private javax.swing.JCheckBox jCheckBox8;
    private javax.swing.JCheckBox jCheckBox9;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel53;
    private javax.swing.JLabel jLabel54;
    private javax.swing.JLabel jLabel55;
    private javax.swing.JLabel jLabel56;
    private javax.swing.JLabel jLabel57;
    private javax.swing.JLabel jLabel58;
    private javax.swing.JLabel jLabel59;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel60;
    private javax.swing.JLabel jLabel61;
    private javax.swing.JLabel jLabel62;
    private javax.swing.JLabel jLabel63;
    private javax.swing.JLabel jLabel64;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane12;
    private javax.swing.JScrollPane jScrollPane13;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JScrollPane jScrollPanel11;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextArea jTextArea3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField10;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField12;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JTextField jTextField14;
    private javax.swing.JTextField jTextField15;
    private javax.swing.JTextField jTextField16;
    private javax.swing.JTextField jTextField17;
    private javax.swing.JTextField jTextField18;
    private javax.swing.JTextField jTextField19;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField20;
    private javax.swing.JTextField jTextField21;
    private javax.swing.JTextField jTextField22;
    private javax.swing.JTextField jTextField23;
    private javax.swing.JTextField jTextField24;
    private javax.swing.JTextField jTextField25;
    private javax.swing.JTextField jTextField26;
    private javax.swing.JTextField jTextField27;
    private javax.swing.JTextField jTextField28;
    private javax.swing.JTextField jTextField29;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JLabel logo;
    private javax.swing.JTextField mesoRate;
    private javax.swing.JTextField noticeText;
    private javax.swing.JTextField scrollingMessage;
    private javax.swing.JButton sendMsgNotice;
    private javax.swing.JButton sendNotice;
    private javax.swing.JButton sendNpcTalkNotice;
    private javax.swing.JButton sendWinNotice;
    private javax.swing.JTable shopItemsTable;
    private javax.swing.JTable shopsTable;
    private javax.swing.JToggleButton show;
    private javax.swing.JComboBox worldList;
    private javax.swing.JTextField worldTip;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
