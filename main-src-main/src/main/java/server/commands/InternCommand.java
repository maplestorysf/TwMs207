package server.commands;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.character.stat.MapleDisease;
import client.MapleStat;
import client.skill.SkillFactory;
import client.anticheat.ReportType;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import constants.ItemConstants;
import extensions.temporary.ScriptMessageType;
import tools.SearchGenerator;
import handling.channel.ChannelServer;
import handling.world.CheaterData;
import handling.world.World;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import scripting.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleSquad.MapleSquadType;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkillFactory;
import server.life.MonsterGlobalDropEntry;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.quest.MapleQuest;
import server.shops.MapleShopFactory;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CField.CScriptMan;
import tools.packet.CWvsContext;

/**
 *
 * @author Emilyx3
 */
public class InternCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.INTERN;
    }

    public static class hide extends 隱藏 {

    }

    public static class 隱藏 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().isHidden()) {
                c.getPlayer().dispelBuff(9001004);
                c.getPlayer().dropMessage(-5, "隱藏已關閉。");
            } else {
                SkillFactory.getSkill(9001004).getEffect(1).applyTo(c.getPlayer());
                c.getPlayer().dropMessage(-5, "隱藏已開啟。");
            }
            return 0;
        }
    }

    public static class HideChat extends 隱藏聊天可見 {

    }

    public static class 隱藏聊天可見 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().isHiddenChatCanSee()) {
                c.getPlayer().setHiddenChatCanSee(false);
                c.getPlayer().dropMessage(-11, "當前隱藏狀態時聊天訊息玩家可見性：不可見");
            } else {
                c.getPlayer().setHiddenChatCanSee(true);
                c.getPlayer().dropMessage(-11, "當前隱藏狀態時聊天訊息玩家可見性：可見");
            }
            return 0;
        }
    }

    public static class heal extends 治愈 {

    }

    public static class 治愈 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().heal(c.getPlayer());
            c.getPlayer().dispelDebuffs();
            return 0;
        }
    }

    public static class healmap extends 治愈全圖 {

    }

    public static class 治愈全圖 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                if (mch != null) {
                    mch.getStat().heal(mch);
                    mch.dispelDebuffs();
                }
            }
            return 1;
        }
    }

    public static class tempban extends 限時鎖帳 {

    }

    public static class 限時鎖帳 extends 封鎖帳號 {

        public 限時鎖帳() {
            tempBan = true;
        }
    }

    public static class Ban extends 封鎖帳號 {

    }

    public static class 封鎖帳號 extends CommandExecute {

        protected boolean tempBan = false;
        protected boolean hellban = false, ipBan = false, macBan = false;

        private String getCommand() {
            if (hellban) {
                return "完全鎖帳";
            } else if (ipBan) {
                return tempBan ? "限時鎖IP" : "封鎖IP";
            } else if (macBan) {
                return tempBan ? "限時鎖MAC" : "封鎖MAC";
            } else if (tempBan) {
                return "限時鎖帳";
            } else {
                return "封鎖帳號";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder reasons = new StringBuilder("封鎖帳號理由: ");
            for (MapleClient.BanReason r : MapleClient.BanReason.values()) {
                reasons.append(r.getReason()).append(" - ").append(r.name()).append(", ");
            }
            if (splitted.length < (tempBan ? 4 : 3)) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> <理由(中文或編號)>" + (tempBan ? " <時間(小時)>" : ""));
                c.getPlayer().dropMessage(-11, reasons.toString());
                return 0;
            }

            final String name = splitted[1];
            MapleClient.BanReason reason = null;
            for (MapleClient.BanReason r : MapleClient.BanReason.values()) {
                if ((splitted[2].matches("^\\d+$") && r.getReason() == Integer.parseInt(splitted[2]))
                        || splitted[2].equals(r.name())) {
                    reason = r;
                    break;
                }
            }
            if (reason == null) {
                c.getPlayer().dropMessage(-11, "沒有這個理由,請從下面的理由選擇(中文或編號):");
                c.getPlayer().dropMessage(-11, reasons.toString());
                return 0;
            }
            Calendar cal = null;
            if (tempBan) {
                final int numHour = Integer.parseInt(splitted[3]);
                cal = Calendar.getInstance();
                cal.add(Calendar.HOUR, numHour);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("角色").append(name).append("因[").append(reason.name()).append("]被").append(c.getPlayer().getName())
                    .append("進行");
            if (hellban) {
                sb.append("完全");
            } else if (tempBan) {
                sb.append("限時");
            }
            sb.append("封鎖帳號");
            if (ipBan) {
                sb.append("/IP");
            }
            if (ipBan) {
                sb.append("/MAC");
            }
            sb.append("處理.");
            if (cal != null && !hellban && tempBan) {
                sb.append("限時鎖帳到 ").append(DateFormat.getInstance().format(cal.getTime()));
            }

            MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (target != null) {
                if ((c.getPlayer().getGmLevel() > target.getGmLevel() || c.getPlayer().isAdmin())
                        && !target.getClient().isGM() && !target.isAdmin()) {
                    if (target.ban(sb.toString(), cal, reason, false, hellban || ipBan, hellban || macBan, hellban)) {
                        c.getPlayer().dropMessage(-11, "[" + getCommand() + "] " + name + " 已經被封鎖帳號");
                        return 1;
                    } else {
                        c.getPlayer().dropMessage(-11, "[" + getCommand() + "] 封帳失敗");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(-11, "[" + getCommand() + "] 無法封鎖GM...");
                    return 1;
                }
            } else {
                if (MapleClient.ban(name, sb.toString(), cal, reason, false, false, hellban || ipBan, hellban || macBan,
                        hellban)) {
                    c.getPlayer().dropMessage(-11, "[" + getCommand() + "] " + name + " 已經被離線封鎖帳號");
                    return 1;
                } else {
                    c.getPlayer().dropMessage(-11, "[" + getCommand() + "] " + name + "封帳失敗");
                    return 0;
                }
            }
        }
    }

    public static class DC extends 下線 {

    }

    public static class 下線 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> ([玩家名稱] [玩家名稱]...)");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage()
                    .getCharacterByName(splitted[splitted.length - 1]);
            if (victim != null && c.getPlayer().getGmLevel() >= victim.getGmLevel()) {
                victim.getClient().getSession().close();
                System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                victim.getClient().disconnect(true, false);
                return 1;
            } else {
                c.getPlayer().dropMessage(-11, "受害者不存在或不在線上。");
                return 0;
            }
        }
    }

    public static class KILL extends 殺 {

    }

    public static class 殺 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱> ([玩家名稱] [玩家名稱]...)");
                return 0;
            }
            MapleCharacter victim = null;
            for (int i = 1; i < splitted.length; i++) {
                try {
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(-11, "沒找到受害者 " + splitted[i]);
                }
                if (player.allowedToTarget(victim) && player.getGmLevel() >= victim.getGmLevel()) {
                    victim.getStat().setHp((short) 0, victim);
                    victim.getStat().setMp((short) 0, victim);
                    victim.updateSingleStat(MapleStat.HP, victim.getStat().getHp());
                    victim.updateSingleStat(MapleStat.MP, victim.getStat().getMp());
                }
            }
            return 1;
        }
    }

    public static class Whereami extends 我在哪裡 {
    }

    public static class 我在哪裡 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "你所在的地圖為 " + c.getPlayer().getMap().toString());
            return 1;
        }
    }

    public static class online extends 線上 {
    }

    public static class 線上 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String online = "";
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                online += cs.getPlayerStorage().getOnlinePlayers(true);
            }
            c.getPlayer().dropMessage(-11, online);
            return 1;
        }
    }

    public static class CharInfo extends 角色訊息 {
    }

    public static class 角色訊息 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱>");
                return 0;
            }
            StringBuilder builder = new StringBuilder();
            final MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (other == null) {
                builder.append("輸入的角色不存在...");
                c.getPlayer().dropMessage(-11, builder.toString());
                return 0;
            }
            c.getPlayer().showPlayerStats(other);
            if (other.getClient().getLastPing() <= 0) {
                other.getClient().sendPing();
            }
            return 1;
        }
    }

    public static class Cheaters extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<CheaterData> cheaters = World.getCheaters();
            for (int x = cheaters.size() - 1; x >= 0; x--) {
                CheaterData cheater = cheaters.get(x);
                c.getPlayer().dropMessage(-11, cheater.getInfo());
            }
            return 1;
        }
    }

    public static class gotos extends 去往 {
    }

    public static class 去往 extends CommandExecute {

        private static final HashMap<String, Integer> gotomaps = new HashMap<>();

        static {
            gotomaps.put("專業技術村", 910001000);
            gotomaps.put("納希沙漠", 260000100);
            gotomaps.put("楓之島", 1010000);
            gotomaps.put("結婚村莊", 680000000);
            gotomaps.put("另一個水世界", 860000000);
            gotomaps.put("水之都", 230000000);
            gotomaps.put("駁船碼頭城", 541000000);
            // gotomaps.put("cwk", 610030000);
            gotomaps.put("埃德爾斯坦", 310000000);
            gotomaps.put("艾靈森林", 300000000);
            gotomaps.put("魔法森林", 101000000);
            gotomaps.put("愛里涅湖水", 101071300);
            gotomaps.put("精靈之林", 101050000);
            gotomaps.put("冰原雪域", 211000000);
            gotomaps.put("耶雷弗", 130000000);
            // gotomaps.put("florina", 120000300);
            gotomaps.put("自由市場", 910000000);
            gotomaps.put("未來之門", 271000000);
            gotomaps.put("工作場所", 180000000);
            gotomaps.put("幸福村", 209000000);
            gotomaps.put("維多利亞港", 104000000);
            gotomaps.put("弓箭手村", 100000000);
            gotomaps.put("藥靈幻境", 251000000);
            gotomaps.put("鄉村鎮", 551000000);
            gotomaps.put("墮落城市", 103000000);
            // gotomaps.put("korean", 222000000);
            gotomaps.put("神木村", 240000000);
            gotomaps.put("玩具城", 220000000);
            gotomaps.put("馬來西亞", 550000000);
            gotomaps.put("桃花仙境", 250000000);
            gotomaps.put("鯨魚號", 120000000);
            gotomaps.put("新葉城", 600000000);
            // gotomaps.put("omega", 221000000);
            gotomaps.put("天空之城", 200000000);
            gotomaps.put("萬神殿", 400000000);
            gotomaps.put("皮卡啾", 270050100);
            // gotomaps.put("phantom", 610010000);
            gotomaps.put("勇士之村", 102000000);
            gotomaps.put("瑞恩村", 140000000);
            gotomaps.put("昭和村", 801000000);
            gotomaps.put("新加坡", 540000000);
            gotomaps.put("六條岔道", 104020000);
            gotomaps.put("奇幻村", 105000000);
            gotomaps.put("楓之港", 2000000);
            gotomaps.put("綠樹村", 866000000);
            gotomaps.put("三扇門", 270000000);
            gotomaps.put("黃昏的勇士之村", 273000000);
            gotomaps.put("克林森烏德城", 301000000);
            gotomaps.put("海怒斯", 230040420);
            gotomaps.put("闇黑龍王", 240060200);
            gotomaps.put("混沌闇黑龍王", 240060201);
            gotomaps.put("格瑞芬多", 240020101);
            gotomaps.put("噴火龍", 240020401);
            gotomaps.put("殘暴炎魔", 280030100);
            gotomaps.put("混沌殘暴炎魔", 280030000);
            gotomaps.put("拉圖斯", 220080001);
            gotomaps.put("選邊站", 109020001);
            gotomaps.put("向上攀升", 109030101);
            gotomaps.put("障礙競走", 109040000);
            gotomaps.put("滾雪球", 109060000);
            gotomaps.put("江戶村", 800000000);
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <地圖名>");
            } else {
                if (gotomaps.containsKey(splitted[1])) {
                    MapleMap target = c.getChannelServer().getMapFactory().getMap(gotomaps.get(splitted[1]));
                    if (target == null) {
                        c.getPlayer().dropMessage(-11, "地圖不存在");
                        return 0;
                    }
                    MaplePortal targetPortal = target.getPortal(0);
                    c.getPlayer().changeMap(target, targetPortal);
                } else {
                    if (splitted[1].equals("列表")) {
                        c.getPlayer().dropMessage(-11, "地圖列表: ");
                        StringBuilder sb = new StringBuilder();
                        for (String s : gotomaps.keySet()) {
                            sb.append(s).append(", ");
                        }
                        c.getPlayer().dropMessage(-11, sb.substring(0, sb.length() - 2));
                    } else {
                        c.getPlayer().dropMessage(-11,
                                "指令錯誤, 使用方法: " + splitted[0] + " <地圖名>(你可以使用 " + splitted[0] + " 列表 來獲取可用地圖列表)");
                    }
                }
            }
            return 1;
        }
    }

    public static class clock extends 時鐘 {
    }

    public static class 時鐘 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " (時間:默認60秒)");
            }
            c.getPlayer().getMap()
                    .broadcastMessage(CField.getClock(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 60)));
            return 1;
        }
    }

    public static class warpHere extends 來這裡 {

    }

    public static class 來這裡 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <玩家名稱>");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if (c.getPlayer().inPVP() || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "請稍後再試");
                    return 0;
                }
                victim.changeMap(c.getPlayer().getMap(),
                        c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
            } else {
                int ch = World.Find.findChannel(splitted[1]);
                if (ch < 0) {
                    c.getPlayer().dropMessage(5, "未找到");
                    return 0;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null || victim.inPVP()
                        || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "請稍後再試");
                    return 0;
                }
                c.getPlayer().dropMessage(5, "受害者正在更變頻道");
                victim.dropMessage(5, "正在更變頻道");
                if (victim.getMapId() != c.getPlayer().getMapId()) {
                    final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory()
                            .getMap(c.getPlayer().getMapId());
                    victim.changeMap(mapp, mapp.findClosestPortal(c.getPlayer().getTruePosition()));
                }
                victim.changeChannel(c.getChannel());
            }
            return 1;
        }
    }

    public static class warp extends 傳送 {
    }

    public static class 傳送 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0]);
                c.getPlayer().dropMessage(-11, "用法一:(要傳送的玩家名稱) <地圖ID> (傳送點ID:默認無)");
                c.getPlayer().dropMessage(-11, "用法二:<要傳送到的玩家名稱>");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null && c.getPlayer().getGmLevel() >= victim.getGmLevel() && !victim.inPVP()
                    && !c.getPlayer().inPVP()) {
                if (splitted.length == 2) {
                    c.getPlayer().changeMap(victim.getMap(),
                            victim.getMap().findClosestSpawnpoint(victim.getTruePosition()));
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory()
                            .getMap(Integer.parseInt(splitted[2]));
                    if (target == null) {
                        c.getPlayer().dropMessage(-11, "地圖不存在");
                        return 0;
                    }
                    MaplePortal targetPortal = null;
                    if (splitted.length > 3) {
                        try {
                            targetPortal = target.getPortal(Integer.parseInt(splitted[3]));
                        } catch (IndexOutOfBoundsException e) {
                            // noop, assume the gm didn't know how many portals there are
                            c.getPlayer().dropMessage(5, "傳送點的選擇無效");
                        } catch (NumberFormatException a) {
                            // noop, assume that the gm is drunk
                        }
                    }
                    if (targetPortal == null) {
                        targetPortal = target.getPortal(0);
                    }
                    victim.changeMap(target, targetPortal);
                }
            } else {
                try {
                    int ch = World.Find.findChannel(splitted[1]);
                    if (ch < 0) {
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        if (target == null) {
                            c.getPlayer().dropMessage(-11, "地圖不存在");
                            return 0;
                        }
                        MaplePortal targetPortal = null;
                        if (splitted.length > 2) {
                            try {
                                targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                            } catch (IndexOutOfBoundsException e) {
                                // noop, assume the gm didn't know how many portals there are
                                c.getPlayer().dropMessage(5, "傳送點的選擇無效");
                            } catch (NumberFormatException a) {
                                // noop, assume that the gm is drunk
                            }
                        }
                        if (targetPortal == null) {
                            targetPortal = target.getPortal(0);
                        }
                        c.getPlayer().changeMap(target, targetPortal);
                    } else {
                        victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().dropMessage(-11, "正在更變頻道, 請稍候");
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                            c.getPlayer().changeMap(mapp, mapp.findClosestPortal(victim.getTruePosition()));
                        }
                        c.getPlayer().changeChannel(ch);
                    }
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(-11, "出現錯誤: " + e.getMessage());
                    return 0;
                }
            }
            return 1;
        }
    }

    public static class speak extends 說 {

    }

    public static class 說 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                if (!c.getPlayer().isGM()) {
                    sb.append("實習 ");
                }
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                World.Broadcast.broadcastMessage(CWvsContext.broadcastMsg(c.getPlayer().isGM() ? 6 : 5, sb.toString()));
            } else {
                c.getPlayer().dropMessage(-11, splitted[0] + " <內容>");
                return 0;
            }
            return 1;
        }
    }

    public static class find extends 搜尋 {
    }

    public static class 搜尋 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length == 2) {
                c.getPlayer().dropMessage(-11, "請提供搜尋訊息");
            } else {
                boolean err = false;
                if (splitted.length == 1) {
                    err = true;
                } else {
                    String typeName = splitted[1];
                    String search = StringUtil.joinStringFrom(splitted, 2);
                    SearchGenerator.SearchType type = SearchGenerator.SearchType.valueOf(typeName);
                    if (type != SearchGenerator.SearchType.未知) {
                        if (!SearchGenerator.foundData(type.getValue(), search)) {
                            c.getPlayer().dropMessage(-11, "搜尋不到此" + type.name());
                            return 0;
                        }
                        switch (type) {
                            case 髮型:
                            case 臉型:
                                Set<Integer> keySet = SearchGenerator.getSearchData(type, search).keySet();
                                int[] styles = new int[keySet.size()];
                                int i = 0;
                                for (int key : keySet) {
                                    styles[i] = key;
                                    i++;
                                }
                                c.getSession().writeAndFlush(CScriptMan.getNPCTalkStyle(9010000, "", styles, 0, false));
                                break;
                            default:
                                String str = SearchGenerator.searchData(type, search);
                                c.getSession().writeAndFlush(
                                        CScriptMan.getNPCTalk(9010000, ScriptMessageType.SM_ASKMENU, str, "", (byte) 0));
                                break;
                        }
                        return 1;
                    }
                    err = true;
                }
                if (err) {
                    StringBuilder sb = new StringBuilder("");
                    for (SearchGenerator.SearchType searchType : SearchGenerator.SearchType.values()) {
                        if (searchType != SearchGenerator.SearchType.未知) {
                            sb.append(searchType.name()).append("/");
                        }
                    }
                    c.getPlayer().dropMessage(-11, splitted[0] + ": <類型> <搜尋訊息>");
                    c.getPlayer().dropMessage(-11, "類型:" + sb.toString());
                }
            }
            return 0;
        }
    }

    public static class WhosFirst extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            // probably bad way to do it
            final long currentTime = System.currentTimeMillis();
            List<Pair<String, Long>> players = new ArrayList<>();
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (!chr.isIntern()) {
                    players.add(new Pair<>(
                            MapleCharacterUtil.makeMapleReadable(chr.getName())
                            + (currentTime - chr.getCheatTracker().getLastAttack() > 600000 ? " (AFK)" : ""),
                            chr.getChangeTime()));
                }
            }
            Collections.sort(players, new WhoComparator());
            StringBuilder sb = new StringBuilder("List of people in this map in order, counting AFK (10 minutes):  ");
            for (Pair<String, Long> z : players) {
                sb.append(z.left).append(", ");
            }
            c.getPlayer().dropMessage(-11, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }

        public static class WhoComparator implements Comparator<Pair<String, Long>>, Serializable {

            @Override
            public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                if (o1.right > o2.right) {
                    return 1;
                } else if (Objects.equals(o1.right, o2.right)) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    }

    public static class WhosLast extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                StringBuilder sb = new StringBuilder("whoslast [type] where type can be:  ");
                for (MapleSquadType t : MapleSquadType.values()) {
                    sb.append(t.name()).append(", ");
                }
                c.getPlayer().dropMessage(-11, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            final MapleSquadType t = MapleSquadType.valueOf(splitted[1].toLowerCase());
            if (t == null) {
                StringBuilder sb = new StringBuilder("whoslast [type] where type can be:  ");
                for (MapleSquadType z : MapleSquadType.values()) {
                    sb.append(z.name()).append(", ");
                }
                c.getPlayer().dropMessage(-11, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            if (t.queuedPlayers.get(c.getChannel()) == null) {
                c.getPlayer().dropMessage(-11, "The queue has not been initialized in this channel yet.");
                return 0;
            }
            c.getPlayer().dropMessage(-11, "Queued players: " + t.queuedPlayers.get(c.getChannel()).size());
            StringBuilder sb = new StringBuilder("List of participants:  ");
            for (Pair<String, String> z : t.queuedPlayers.get(c.getChannel())) {
                sb.append(z.left).append('(').append(z.right).append(')').append(", ");
            }
            c.getPlayer().dropMessage(-11, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }
    }

    public static class WhosNext extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                StringBuilder sb = new StringBuilder("whosnext [type] where type can be:  ");
                for (MapleSquadType t : MapleSquadType.values()) {
                    sb.append(t.name()).append(", ");
                }
                c.getPlayer().dropMessage(-11, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            final MapleSquadType t = MapleSquadType.valueOf(splitted[1].toLowerCase());
            if (t == null) {
                StringBuilder sb = new StringBuilder("whosnext [type] where type can be:  ");
                for (MapleSquadType z : MapleSquadType.values()) {
                    sb.append(z.name()).append(", ");
                }
                c.getPlayer().dropMessage(-11, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            if (t.queue.get(c.getChannel()) == null) {
                c.getPlayer().dropMessage(-11, "The queue has not been initialized in this channel yet.");
                return 0;
            }
            c.getPlayer().dropMessage(-11, "Queued players: " + t.queue.get(c.getChannel()).size());
            StringBuilder sb = new StringBuilder("List of participants:  ");
            final long now = System.currentTimeMillis();
            for (Pair<String, Long> z : t.queue.get(c.getChannel())) {
                sb.append(z.left).append('(').append(StringUtil.getReadableMillis(z.right, now)).append(" ago),");
            }
            c.getPlayer().dropMessage(-11, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }
    }

    public static class killMob extends 清怪 {

    }

    public static class 清怪 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " (範圍:默認全圖) (地圖D:默認當前地圖)");
            }
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(-11, "地圖不存在");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range,
                    Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (!mob.getStats().isBoss() || mob.getStats().isPartyBonus() || c.getPlayer().isGM()) {
                    map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
                }
            }
            c.getPlayer().monsterMultiKill();
            return 1;
        }
    }

    public static class itemvac extends 全屏撿物 {
    }

    public static class 全屏撿物 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final List<MapleMapObject> items = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(),
                    GameConstants.maxViewRangeSq(), Arrays.asList(MapleMapObjectType.ITEM));
            MapleMapItem mapitem;
            for (MapleMapObject item : items) {
                mapitem = (MapleMapItem) item;
                if (mapitem.getMeso() > 0) {
                    c.getPlayer().gainMeso(mapitem.getMeso(), true);
                } else if (mapitem.getItem() == null || !MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(),
                        true, mapitem.getDropper() instanceof MapleMonster)) {
                    continue;
                }
                mapitem.setPickedUp(true);
                c.getPlayer().getMap().broadcastMessage(
                        CField.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()),
                        mapitem.getPosition());
                c.getPlayer().getMap().removeMapObject(item);

            }
            return 1;
        }
    }

    public static class clearbuff extends 清除BUFF {
    }

    public static class 清除BUFF extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().cancelAllBuffs();
            return 1;
        }
    }

    public static class cc extends 換頻 {
    }

    public static class changechannel extends 換頻 {
    }

    public static class 換頻 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().changeChannel(Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class Reports extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<CheaterData> cheaters = World.getReports();
            for (int x = cheaters.size() - 1; x >= 0; x--) {
                CheaterData cheater = cheaters.get(x);
                c.getPlayer().dropMessage(-11, cheater.getInfo());
            }
            return 1;
        }
    }

    public static class ClearReport extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                StringBuilder ret = new StringBuilder("report [ign] [all/");
                for (ReportType type : ReportType.values()) {
                    ret.append(type.theId).append('/');
                }
                ret.setLength(ret.length() - 1);
                c.getPlayer().dropMessage(-11, ret.append(']').toString());
                return 0;
            }
            final MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "Does not exist");
                return 0;
            }
            final ReportType type = ReportType.getByString(splitted[2]);
            if (type != null) {
                victim.clearReports(type);
            } else {
                victim.clearReports();
            }
            c.getPlayer().dropMessage(5, "Done.");
            return 1;
        }
    }

    public static class fake extends 假重載 {
    }

    public static class 假重載 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().fakeRelog();
            return 1;
        }
    }

    public static class fly extends 飛 {
    }

    public static class 飛 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SkillFactory.getSkill(1146).getEffect(1).applyTo(c.getPlayer());
            SkillFactory.getSkill(1142).getEffect(1).applyTo(c.getPlayer());
            c.getPlayer().dispelBuff(1146);
            return 1;
        }
    }

    public static class openNpc extends 打開NPC {
    }

    public static class 打開NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <NPCID> (特殊:默認空)");
                return 0;
            }
            NPCScriptManager.getInstance().start(c, Integer.parseInt(splitted[1]),
                    splitted.length > 2 ? StringUtil.joinStringFrom(splitted, 2) : null);
            return 1;
        }
    }

    public static class shop extends 打開商店 {
    }

    public static class openshop extends 打開商店 {
    }

    public static class 打開商店 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <商店ID>");
                return 0;
            }
            MapleShopFactory.getInstance().getShop(Integer.parseInt(splitted[1])).sendShop(c);
            return 1;
        }
    }

    public static class cleardrops extends 清理掉寶 {
    }

    public static class 清理掉寶 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().removeDrops();
            return 1;
        }
    }

    public static class 商店 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory shop = MapleShopFactory.getInstance();
            int shopId = Integer.parseInt(splitted[1]);
            if (shop.getShop(shopId) != null) {
                shop.getShop(shopId).sendShop(c);
            }
            return 1;
        }
    }

    public static class killnear extends 殺附近 {
    }

    public static class 殺附近 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> players = map.getMapObjectsInRange(c.getPlayer().getPosition(), 25000,
                    Arrays.asList(MapleMapObjectType.PLAYER));
            for (MapleMapObject closeplayers : players) {
                MapleCharacter playernear = (MapleCharacter) closeplayers;
                if (playernear.isAlive() && playernear != c.getPlayer()
                        && playernear.getGmLevel() < c.getPlayer().getGmLevel()) {
                    playernear.getStat().setHp((short) 0, playernear);
                    playernear.getStat().setMp((short) 0, playernear);
                    playernear.updateSingleStat(MapleStat.HP, playernear.getStat().getHp());
                    playernear.updateSingleStat(MapleStat.MP, playernear.getStat().getMp());
                    playernear.dropMessage(5, "你太靠近管理員了");
                }
            }
            return 1;
        }
    }

    public static class ManualEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getChannelServer().manualEvent(c.getPlayer())) {
                for (MapleCharacter chrs : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    // chrs.dropMessage(0, "MapleGM is hosting an event! Use the @joinevent command
                    // to join the event!");
                    // chrs.dropMessage(0, "Event Map: " + c.getPlayer().getMap().getMapName());
                    // World.Broadcast.broadcastMessage(CWvsContext.broadcastMsg(25, 0, "MapleGM is
                    // hosting an event! Use the @joinevent command to join the event!"));
                    // World.Broadcast.broadcastMessage(CWvsContext.broadcastMsg(26, 0, "Event Map:
                    // " + c.getPlayer().getMap().getMapName()));
                    chrs.getClient().getSession().writeAndFlush(CWvsContext.broadcastMsg(
                            GameConstants.isEventMap(chrs.getMapId()) ? 0 : 25, c.getChannel(),
                            "Event : MapleGM is hosting an event! Use the @joinevent command to join the event!"));
                    chrs.getClient().getSession()
                            .writeAndFlush(CWvsContext.broadcastMsg(GameConstants.isEventMap(chrs.getMapId()) ? 0 : 26,
                                    c.getChannel(), "Event : Event Channel: " + c.getChannel() + " Event Map: "
                                    + c.getPlayer().getMap().getMapName()));
                }
            } else {
                for (MapleCharacter chrs : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    // World.Broadcast.broadcastMessage(CWvsContext.broadcastMsg(22, 0, "Enteries to
                    // the GM event are closed. The event has began!"));
                    chrs.getClient().getSession()
                            .writeAndFlush(CWvsContext.broadcastMsg(GameConstants.isEventMap(chrs.getMapId()) ? 0 : 22,
                                    c.getChannel(),
                                    "Event : Enteries to the GM event are closed. The event has began!"));
                }
            }
            return 1;
        }
    }

    public static class ActiveBomberman extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (player.getMapId() != 109010100) {
                player.dropMessage(5, "This command is only usable in map 109010100.");
            } else {
                c.getChannelServer().toggleBomberman(c.getPlayer());
                for (MapleCharacter chr : player.getMap().getCharacters()) {
                    if (!chr.isIntern()) {
                        chr.cancelAllBuffs();
                        chr.giveDebuff(MapleDisease.封印, MobSkillFactory.getMobSkill(120, 1));
                        // MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE,
                        // 2100067, chr.getItemQuantity(2100067, false), true, true);
                        // chr.gainItem(2100067, 30);
                        // MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.ETC,
                        // 4031868, chr.getItemQuantity(4031868, false), true, true);
                        // chr.gainItem(4031868, (short) 5);
                        // chr.dropMessage(0, "You have been granted 5 jewels(lifes) and 30 bombs.");
                        // chr.dropMessage(0, "Pick up as many bombs and jewels as you can!");
                        // chr.dropMessage(0, "Check inventory for Bomb under use");
                    }
                }
                for (MapleCharacter chrs : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    chrs.getClient().getSession()
                            .writeAndFlush(CWvsContext.broadcastMsg(GameConstants.isEventMap(chrs.getMapId()) ? 0 : 22,
                                    c.getChannel(), "Event : Bomberman event has started!"));
                }
                player.getMap().broadcastMessage(CField.getClock(60));
            }
            return 1;
        }
    }

    public static class DeactiveBomberman extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (player.getMapId() != 109010100) {
                player.dropMessage(5, "This command is only usable in map 109010100.");
            } else {
                c.getChannelServer().toggleBomberman(c.getPlayer());
                int count = 0;
                String winner = "";
                for (MapleCharacter chr : player.getMap().getCharacters()) {
                    if (!chr.isGM()) {
                        if (count == 0) {
                            winner = chr.getName();
                            count++;
                        } else {
                            winner += " , " + chr.getName();
                        }
                    }
                }
                for (MapleCharacter chrs : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    chrs.getClient().getSession()
                            .writeAndFlush(CWvsContext.broadcastMsg(GameConstants.isEventMap(chrs.getMapId()) ? 0 : 22,
                                    c.getChannel(), "Event : Bomberman event has ended! The winners are: " + winner));
                }
            }
            return 1;
        }
    }

    public static class clearinv extends 清理道具 {
    }

    public static class 清理道具 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2 || player.hasBlockedInventory()) {
                c.getPlayer().dropMessage(5, splitted[0] + " <道具欄:裝備 / 消耗 / 其他 / 裝飾 / 特殊 / 全部>");
                return 0;
            } else {
                MapleInventoryType type;
                if (splitted[1].equalsIgnoreCase("裝備")) {
                    type = MapleInventoryType.EQUIP;
                } else if (splitted[1].equalsIgnoreCase("消耗")) {
                    type = MapleInventoryType.USE;
                } else if (splitted[1].equalsIgnoreCase("其他")) {
                    type = MapleInventoryType.ETC;
                } else if (splitted[1].equalsIgnoreCase("裝飾")) {
                    type = MapleInventoryType.SETUP;
                } else if (splitted[1].equalsIgnoreCase("特殊")) {
                    type = MapleInventoryType.CASH;
                } else if (splitted[1].equalsIgnoreCase("全部")) {
                    type = null;
                } else {
                    c.getPlayer().dropMessage(5, "找不到道具欄 < 裝備 / 消耗 / 其他 / 裝飾 / 特殊 / 全部>");
                    return 0;
                }
                if (type == null) { // All, a bit hacky, but it's okay
                    MapleInventoryType[] invs = {MapleInventoryType.EQUIP, MapleInventoryType.USE,
                        MapleInventoryType.SETUP, MapleInventoryType.ETC, MapleInventoryType.CASH};
                    for (MapleInventoryType t : invs) {
                        type = t;
                        MapleInventory inv = c.getPlayer().getInventory(type);
                        short start = -1;
                        for (short i = 0; i < inv.getSlotLimit() + 1; i++) {
                            if (inv.getItem(i) != null) {
                                start = i;
                                break;
                            }
                        }
                        if (start == -1) {
                            c.getPlayer().dropMessage(5, "此道具欄沒有道具");
                            return 0;
                        }
                        short end = 0;
                        for (short i = start; i < inv.getSlotLimit() + 1; i++) {
                            if (inv.getItem(i) != null) {
                                MapleInventoryManipulator.removeFromSlot(c, type, i, inv.getItem(i).getQuantity(),
                                        true);
                            } else {
                                end = i;
                                break;// Break at first empty space.
                            }
                        }
                        c.getPlayer().dropMessage(5, "已清除第" + start + "格到第" + end + "格的道具");
                    }
                } else {
                    MapleInventory inv = c.getPlayer().getInventory(type);
                    short start = -1;
                    for (short i = 0; i < inv.getSlotLimit() + 1; i++) {
                        if (inv.getItem(i) != null) {
                            start = i;
                            break;
                        }
                    }
                    if (start == -1) {
                        c.getPlayer().dropMessage(5, "此道具欄沒有道具");
                        return 0;
                    }
                    short end = 0;
                    for (short i = start; i < inv.getSlotLimit() + 1; i++) {
                        if (inv.getItem(i) != null) {
                            MapleInventoryManipulator.removeFromSlot(c, type, i, inv.getItem(i).getQuantity(), true);
                        } else {
                            end = i;
                            break;// Break at first empty space.
                        }
                    }
                    c.getPlayer().dropMessage(5, "已清除第" + start + "格到第" + end + "格的道具");
                }
                return 1;
            }
        }
    }

    public static class Bob extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonster mob = MapleLifeFactory.getMonster(9400551);
            for (int i = 0; i < 10; i++) {
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            }
            return 1;
        }
    }

    public static class KILLMAP extends 殺全圖 {
    }

    public static class 殺全圖 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter map : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (map != null && !map.isIntern()) {
                    map.getStat().setHp((short) 0, map);
                    map.getStat().setMp((short) 0, map);
                    map.updateSingleStat(MapleStat.HP, map.getStat().getHp());
                    map.updateSingleStat(MapleStat.MP, map.getStat().getMp());
                }
            }
            return 1;
        }
    }

    public static class speakColor extends 說話顏色 {
    }

    public static class 說話顏色 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <顏色值>");
                return 0;
            }
            try {
                c.getPlayer().setChatType((short) Short.parseShort(splitted[1]));
                c.getPlayer().dropMessage(-11, "說話顏色更變完成。");
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "出現未知錯誤。");
            }
            return 1;
        }
    }

    public static class FindCommand extends 檢索指令 {

    }

    public static class 檢索指令 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <關鍵字詞>");
                return 0;
            }
            c.getPlayer().dropMessage(-11, "檢索指令(關鍵字詞:" + splitted[1] + ")結果如下:");
            for (int i = 0; i <= c.getPlayer().getGmLevel(); i++) {
                CommandProcessor.dropCommandList(c, i, splitted[1]);
            }
            return 1;
        }
    }

    public static class globaldrop extends 全域掉寶 {
    }

    public static class 全域掉寶 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            final List<MonsterGlobalDropEntry> drops = MapleMonsterInformationProvider.getInstance().getGlobalDrop();
            StringBuilder name = new StringBuilder();
            if (drops != null && drops.size() > 0) {
                int num = 0;
                int itemId;
                int ch;
                MonsterGlobalDropEntry de;
                for (int i = 0; i < drops.size(); i++) {
                    de = drops.get(i);
                    if (de.chance > 0 && (de.questid <= 0
                            || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                        itemId = de.itemId;
                        if (num == 0) {
                            name.append("全域掉寶數據如下\r\n");
                            name.append("--------------------------------------\r\n");
                        }
                        String namez = "#z" + itemId + "#";
                        if (itemId == 0) { // meso
                            itemId = 4031041; // display sack of cash
                            namez = (de.Minimum * c.getChannelServer().getMesoRate(c.getPlayer().getWorld())) + "到"
                                    + (de.Maximum * c.getChannelServer().getMesoRate(c.getPlayer().getWorld())) + "楓幣";
                        }
                        ch = de.chance * c.getChannelServer().getDropRate(c.getPlayer().getWorld());
                        name.append(num + 1).append(") #i").append(itemId).append(":#").append(namez).append("(")
                                .append(itemId).append(")")
                                .append(de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0
                                        ? ("[" + MapleQuest.getInstance(de.questid).toString() + "]")
                                        : "")
                                .append("\r\n掉寶幾率：")
                                .append(Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0)
                                .append("%").append(" 來源：").append(de.addFrom).append("\r\n\r\n");
                        num++;
                    }
                }
            }
            if (name.length() > 0) {
                c.getSession().writeAndFlush(CScriptMan.getNPCTalk(9010000, ScriptMessageType.SM_SAY, name.toString(),
                        "00 00", (byte) 0, 9010000));
            } else {
                c.getPlayer().dropMessage(1, "全域掉寶無數據。");
            }
            return 1;
        }
    }

    public static class item extends 製作道具 {
    }

    public static class 製作道具 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-11, splitted[0] + " <道具ID>");
                return 0;
            }
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);

            if (!c.getPlayer().isAdmin()) {
                for (int i : GameConstants.itemBlock) {
                    if (itemId == i) {
                        c.getPlayer().dropMessage(5, "當前管理員等級沒有製作此道具的權限");
                        return 0;
                    }
                }
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, "此道具不存在");
            } else {
                Item item;
                short flag = (short) ItemFlag.LOCK.getValue();

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    item = ii.getEquipById(itemId);
                } else {
                    item = new Item(itemId, (byte) 0, quantity, (byte) 0);
                }

                MaplePet pet = null;
                if (ItemConstants.類型.寵物(itemId)) {
                    pet = MaplePet.createPet(itemId);
                    item.setExpiration(System.currentTimeMillis() + (ii.getLife(itemId) * 24 * 60 * 60 * 1000));
                    if (pet != null) {
                        item.setPet(pet);
                    }
                }
                item.setUniqueId(MapleInventoryManipulator.getUniqueId(item.getItemId(), pet));
                if (!c.getPlayer().isGM()) {
                    item.setFlag(flag);
                }
                if (!c.getPlayer().isAdmin()) {
                    item.setOwner(c.getPlayer().getName());
                }
                item.setGMLog(c.getPlayer().getName() + " 使用 " + splitted[0] + " 指令制作, 時間:"
                        + FileoutputUtil.CurrentReadable_Time());
                MapleInventoryManipulator.addbyItem(c, item);
            }
            return 1;
        }
    }

    public static class mob extends 怪物 {
    }

    public static class 怪物 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonster mob = null;
            for (final MapleMapObject monstermo : c.getPlayer().getMap().getMapObjectsInRange(
                    c.getPlayer().getPosition(), 100000, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.isAlive()) {
                    c.getPlayer().dropMessage(-11, "怪物: " + mob.toString());
                    break; // only one
                }
            }
            if (mob == null) {
                c.getPlayer().dropMessage(-11, "沒找到任何怪物");
            }
            return 1;
        }
    }

    public static class owl extends 獲取貓頭鷹 {
    }

    public static class 獲取貓頭鷹 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().haveItem(2310000) || !c.getPlayer().canHold(2310000)) {
                c.getPlayer().dropMessage(1, "道具欄空間不足或者已經有貓頭鷹了了。");
            } else {
                c.getPlayer().gainItem(2310000, 1, splitted[0] + " 指令獲取");
            }
            return 1;
        }
    }

    public static class MapDrop extends 地圖掉寶 {

    }

    public static class 地圖掉寶 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().start(c, 9010000, "MonsterDrops");
            return 1;
        }
    }
}
