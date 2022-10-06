/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.commands;

import client.MapleCharacter;
import client.MapleClient;
import database.ManagerDatabasePool;
import handling.channel.ChannelServer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tools.FileoutputUtil;

public class CommandProcessor {

    private final static String commandPrefix = "@";
    private final static HashMap<String, MapleCommand> commands = new HashMap<>();
    private final static HashMap<Integer, ArrayList<String>> commandList = new HashMap<>();

    static {

        Class<?>[] CommandFiles = {PlayerCommand.class, InternCommand.class, GMCommand.class, SuperGMCommand.class,
            AdminCommand.class};

        for (Class<?> clasz : CommandFiles) {
            try {
                PlayerGMRank rankNeeded = (PlayerGMRank) clasz.getMethod("getPlayerLevelRequired", new Class<?>[]{}).invoke(null, (Object[]) null);
                Class<?>[] a = clasz.getDeclaredClasses();
                ArrayList<String> cL = new ArrayList<>();
                for (Class<?> c : a) {
                    try {
                        if (!Modifier.isAbstract(c.getModifiers()) && !c.isSynthetic()) {
                            Object o = c.getDeclaredConstructor().newInstance();
                            boolean enabled;
                            try {
                                enabled = c.getDeclaredField("enabled").getBoolean(c.getDeclaredField("enabled"));
                            } catch (NoSuchFieldException ex) {
                                enabled = true; // Enable all coded commands by default.
                            }
                            if (o instanceof CommandExecute && enabled) {
                                cL.add(c.getSimpleName().toLowerCase());
                                commands.put(c.getSimpleName().toLowerCase(), new MapleCommand((CommandExecute) o, rankNeeded.getLevel()));
                            }
                        }
                    } catch (InstantiationException | IllegalAccessException | SecurityException | IllegalArgumentException ex) {
                        FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, ex);
                    }
                }
                Collections.sort(cL);
                commandList.put(rankNeeded.getLevel(), cL);
            } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, ex);
            }
        }
    }

    private static void sendDisplayMessage(MapleClient c, String msg, CommandType type) {
        if (c.getPlayer() == null) {
            return;
        }
        switch (type) {
            case NORMAL:
                c.getPlayer().dropMessage(-11, msg);
                break;
            case TRADE:
                c.getPlayer().dropMessage(-2, "系統提示 : " + msg);
                break;
        }
    }

    public static void dropHelp(MapleClient c) {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n幫助");
        sb.append("\r\n@GM 訊息內容：發送訊息給線上的管理員。");
        sb.append("\r\n@解卡/@ea：如果卡死或者無法開啟NPC可以使用這個指令。");
        sb.append("\r\n@查看：顯示各種訊息並解卡。");
        sb.append("\r\n@卡圖：只有卡圖無法出來時才能使用。");
        sb.append("\r\n* 游標在話框閃爍的狀態下，按下Tab鍵；或是游標不在話框中的狀態下，");
        sb.append("\r\n按下數字鍵1、2、3，即可變更聊天對象。");
        for (String command : sb.toString().split("\r\n")) {
            c.getPlayer().dropMessage(-11, command);
        }
        if (!c.getPlayer().isIntern()) {
            return;
        }
        for (int i = 0; i <= c.getPlayer().getGmLevel(); i++) {
            dropCommandList(c, i);
        }
    }

    public static void dropCommandList(MapleClient c, int level) {
        dropCommandList(c, level, "");
    }

    public static void dropCommandList(MapleClient c, int level, String search) {
        if (!commandList.containsKey(level)) {
            return;
        }
        final PlayerGMRank pGMRank = PlayerGMRank.getByLevel(level);
        final StringBuilder comment = new StringBuilder("");
        if (c.isGM()) {
            comment.append('"').append("/").append('"').append("或");
        }
        comment.append('"').append(commandPrefix).append('"');
        c.getPlayer().dropMessage(-11, "-----------------------------------------------------------------------------------------");
        switch (pGMRank) {
            case NORMAL:
                c.getPlayer().dropMessage(-11, "玩家指令(前綴:" + comment + ")：");
                break;
            case INTERN:
                c.getPlayer().dropMessage(-11, "實習管理員指令(前綴:" + comment + ")：");
                break;
            case GM:
                c.getPlayer().dropMessage(-11, "遊戲管理員指令(前綴:" + comment + ")：");
                break;
            case SUPERGM:
                c.getPlayer().dropMessage(-11, "高級管理員指令(前綴:" + comment + ")：");
                break;
            case ADMIN:
                c.getPlayer().dropMessage(-11, "伺服器管理指令(前綴:" + comment + ")：");
                break;
            default:
                break;
        }
        c.getPlayer().dropMessage(-11, getCommandsForLevel(level, search));
    }

    public static boolean processCommand(MapleClient c, String line, CommandType type) {
        if (line.startsWith("/") && c.getPlayer().cheakSkipOnceChat() && type == CommandType.NORMAL) {
            return true;
        }

        if (line.startsWith("//") || line.startsWith(commandPrefix + commandPrefix)) {
            return false;
        }

        boolean dropUnFound = false;
        String mapleHelp = "";
        String searchHelp = "";
        String commandPrefixStr;
        switch (String.valueOf(line.charAt(0))) {
            case "/":
                commandPrefixStr = "/";
                break;
            case commandPrefix:
                commandPrefixStr = commandPrefix;
                break;
            default:
                commandPrefixStr = "";
                break;
        }
        if (!commandPrefixStr.isEmpty()) {
            String[] splitted = line.split(" ");
            String command = splitted[0].toLowerCase().substring(1);

            MapleCommand co = commands.get(command);
            if (co == null || co.getType() != type) {
                if (command.equals("幫助")) {
                    if (type == CommandType.TRADE) {
                        c.getPlayer().dropMessage(-2, "幫助 : " + "\r\n" + commandPrefixStr + "交易道具 <欄位:裝備/消耗/裝飾/其他/特殊> <道具名稱> [道具數量(可選,默認1)]");
                    } else {
                        dropHelp(c);
                    }
                    return true;
                }
                dropUnFound = true;
                mapleHelp = (type == CommandType.NORMAL ? "[楓之谷幫助]" : "") + "在對話視窗輸入 '" + commandPrefixStr + "幫助' 時可以看到使用 '" + commandPrefixStr + "' 的指令說明.";
            } else {
                try {
                    int ret = co.execute(c, splitted); // Don't really care about the return value. ;D
                } catch (Exception e) {
                    sendDisplayMessage(c, "使用指令出現錯誤：", type);
                    if (c.getPlayer().isIntern()) {
                        sendDisplayMessage(c, "錯誤：" + e, type);
                        FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, e);
                    }
                }
                return true;
            }
        }

        if (c.getPlayer().getGmLevel() > PlayerGMRank.NORMAL.getLevel()) {
            if (line.charAt(0) == '`' && c.getPlayer().getGmLevel() > 2 && type == CommandType.NORMAL) {
                for (final ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.broadcastGMMessage(
                            tools.packet.CField.multiChat("[管理互聊] " + c.getPlayer().getName(), line.substring(1), 4));
                }
                return true;
            }
            if (!commandPrefixStr.isEmpty()) {
                String[] splitted = line.split(" ");
                String command = splitted[0].toLowerCase().substring(1);

                MapleCommand co = commands.get(command);
                if (co == null || co.getType() != type) {
                    if (command.equals("幫助")) {
                        if (type == CommandType.NORMAL) {
                            dropHelp(c);
                        }
                        return true;
                    }
                    dropUnFound = true;
                    mapleHelp = (type == CommandType.NORMAL ? "[楓之谷幫助]" : "") + "在對話視窗輸入 '" + commandPrefixStr + "幫助' 時可以看到使用 '" + commandPrefixStr + "' 的指令列表.";
                    searchHelp = "輸入 '" + commandPrefixStr + "檢索指令 關鍵字詞' 可以檢索可用的 '" + commandPrefixStr + "' 指令.";
                } else {
                    if (c.getPlayer().getGmLevel() >= co.getReqGMLevel()) {
                        int ret = 0;
                        try {
                            ret = co.execute(c, splitted);
                        } catch (ArrayIndexOutOfBoundsException x) {
                            sendDisplayMessage(c, "使用指令出錯，此指令必須帶參數才能使用: " + x, type);
                        } catch (Exception e) {
                            FileoutputUtil.outputFileError(FileoutputUtil.CommandEx_Log, e);
                        }
                        if (ret > 0 && c.getPlayer() != null) { // incase d/c after command or something
                            if (c.getPlayer().isGM()) {
                                logCommandToDB(c.getPlayer(), line, "gmlog");
                            } else {
                                logCommandToDB(c.getPlayer(), line, "internlog");
                            }
                        }
                    } else {
                        sendDisplayMessage(c, "你沒有權限使用此指令。", type);
                    }
                    return true;
                }
            }
        }

        if (dropUnFound) {
            sendDisplayMessage(c, mapleHelp, type);
            if (!searchHelp.isEmpty()) {
                sendDisplayMessage(c, searchHelp, type);
            }
            return true;
        }
        return false;
    }

    public static void logCommandToDB(MapleCharacter player, String command, String table) {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + table + " (cid, command, mapid) VALUES (?, ?, ?)")) {
                ps.setInt(1, player.getId());
                ps.setString(2, command);
                ps.setInt(3, player.getMap().getId());
                ps.executeUpdate();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.SQL_Log, ex);
        }
    }

    private static String getCommandsForLevel(int level, String search) {
        String commandlist = "";
        List<String> comList = commandList.get(level);
        for (int i = 0; i < comList.size(); i++) {
            if (search.isEmpty() || comList.get(i).contains(search.toLowerCase())) {
                commandlist += comList.get(i);
                if (i + 1 < comList.size()) {
                    commandlist += ", ";
                }
            }
        }
        return commandlist;
    }
}
