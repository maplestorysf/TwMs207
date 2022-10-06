package scripting;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleJob;
import client.MapleQuestStatus;
import client.MapleStat;
import client.skill.Skill;
import client.skill.SkillEntry;
import client.skill.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import constants.ItemConstants;
import constants.SkillConstants;
import tools.SearchGenerator;
import database.ManagerDatabasePool;
import extensions.temporary.InGameDirectionEventOpcode;
import extensions.temporary.FieldEffectOpcode;
import extensions.temporary.GuildOpcode;
import extensions.temporary.ScriptMessageType;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.channel.handler.HiredMerchantHandler;
import handling.channel.handler.InventoryHandler;
import handling.channel.handler.PlayersHandler;
import handling.login.LoginInformationProvider;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.script.Invocable;
import server.CashItemFactory;
import server.MapleCarnivalChallenge;
import server.MapleCarnivalParty;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleSlideMenu;
import server.MapleSquad;
import server.MapleStatEffect;
import server.Randomizer;
import server.SpeedRunner;
import server.StructItemOption;
import server.Timer.CloneTimer;
import server.life.*;
import server.maps.MulungSystem;
import server.maps.Event_PyramidSubway;
import server.maps.MapleFoothold;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import server.shops.MapleShopFactory;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.GuildPacket;
import tools.packet.CWvsContext.InfoPacket;
import extensions.temporary.UserEffectOpcode;
import handling.cashshop.RoyaCoupon;
import server.MapleGachapon;
import server.MapleGachaponItem;
import tools.packet.CField.CScriptMan;

public class NPCConversationManager extends AbstractPlayerInteraction {

    private String getText;
    private final ScriptType type; // -1 = NPC, 0 = start quest, 1 = end quest
    private ScriptMessageType lastMsg = null;
    public boolean pendingDisposal = false;
    private final Invocable iv;
    public static final Map<Pair<Integer, MapleClient>, MapleNPC> npcRequestController = new HashMap<>();

    public NPCConversationManager(MapleClient c, int npc, int questid, String npcscript, ScriptType type,
            Invocable iv) {
        super(c, npc, questid, npcscript);
        this.type = type;
        this.iv = iv;
    }

    public Invocable getIv() {
        return iv;
    }

    public int getNpc() {
        return id;
    }

    public int getQuest() {
        return id2;
    }

    public String getScript() {
        return script;
    }

    public void setNpc(int id) {
        this.id = id;
    }

    public void setQuest(int id2) {
        this.id2 = id2;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public ScriptType getType() {
        return type;
    }

    public void safeDispose() {
        pendingDisposal = true;
    }

    public void dispose() {
        NPCScriptManager.getInstance().dispose(c);
    }

    public void sendSlideMenu(final int type, final String sel) {
        int lasticon = 0;
        c.getSession().writeAndFlush(CScriptMan.getSlideMenu(id, type, lasticon, sel));
        lastMsg = ScriptMessageType.SM_ASKSLIDEMENU;
    }

    public String getDimensionalMirror(MapleCharacter character) {
        return MapleSlideMenu.SlideMenu0.getSelectionInfo(character, id);
    }

    public String getSlideMenuSelection(int type) {
        switch (type) {
            case 0:
                return MapleSlideMenu.SlideMenu0.getSelectionInfo(getPlayer(), id);
            case 1:
                return MapleSlideMenu.SlideMenu1.getSelectionInfo(getPlayer(), id);
            case 2:
                return MapleSlideMenu.SlideMenu2.getSelectionInfo(getPlayer(), id);
            case 3:
                return MapleSlideMenu.SlideMenu3.getSelectionInfo(getPlayer(), id);
            case 4:
                return MapleSlideMenu.SlideMenu4.getSelectionInfo(getPlayer(), id);
            case 5:
                return MapleSlideMenu.SlideMenu5.getSelectionInfo(getPlayer(), id);
            case 6:
                return MapleSlideMenu.SlideMenu6.getSelectionInfo(getPlayer(), id);
            default:
                return MapleSlideMenu.SlideMenu0.getSelectionInfo(getPlayer(), id);
        }
    }

    public int[] getSlideMenuDataIntegers(int type, int selection) {
        switch (type) {
            case 0:
                return MapleSlideMenu.SlideMenu0.getDataIntegers(selection);
            case 1:
                return MapleSlideMenu.SlideMenu1.getDataIntegers(selection);
            case 2:
                return MapleSlideMenu.SlideMenu2.getDataIntegers(selection);
            case 3:
                return MapleSlideMenu.SlideMenu3.getDataIntegers(selection);
            case 4:
                return MapleSlideMenu.SlideMenu4.getDataIntegers(selection);
            case 5:
                return MapleSlideMenu.SlideMenu5.getDataIntegers(selection);
            case 6:
                return MapleSlideMenu.SlideMenu6.getDataIntegers(selection);
            default:
                return MapleSlideMenu.SlideMenu0.getDataIntegers(selection);
        }
    }

    public void say(String sMsg) {
        say(sMsg, false, false);
    }

    public void say(String sMsg, boolean prev, boolean next) {
        say(0, sMsg, prev, next);
    }

    public void say(int bParam, String sMsg, boolean prev, boolean next) {
        say(id, bParam, sMsg, prev, next);
    }

    public void say(int nSpeakerTemplateID, int bParam, String sMsg, boolean prev, boolean next) {
        say(nSpeakerTemplateID, -1, bParam, sMsg, prev, next);
    }

    public void say(int nSpeakerTemplateID, int nAnotherSpeakerTemplateID, int bParam, String sMsg, boolean prev,
            boolean next) {
        say(nSpeakerTemplateID, nAnotherSpeakerTemplateID, -1, bParam, sMsg, prev, next);
    }

    public void say(int nSpeakerTemplateID, int nAnotherSpeakerTemplateID, int nOtherSpeakerTemplateID, int bParam,
            String sMsg, boolean prev, boolean next) {
        say(4, nSpeakerTemplateID, nAnotherSpeakerTemplateID, nOtherSpeakerTemplateID, bParam, 0, sMsg, prev, next, 0);
    }

    public void say(int nSpeakerTypeID, int nSpeakerTemplateID, int nAnotherSpeakerTemplateID,
            int nOtherSpeakerTemplateID, int bParam, int eColor, String sMsg, boolean prev, boolean next, int tWait) {
        if (sMsg.contains("#L")) {
            askMenu(nSpeakerTypeID, nSpeakerTemplateID, nAnotherSpeakerTemplateID, nOtherSpeakerTemplateID, bParam,
                    eColor, sMsg);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession()
                .writeAndFlush(CScriptMan.OnScriptMessage(nSpeakerTypeID, nSpeakerTemplateID, nAnotherSpeakerTemplateID,
                        nOtherSpeakerTemplateID, lastMsg, bParam, eColor, new String[]{sMsg},
                        new int[]{prev ? 1 : 0, next ? 1 : 0, tWait}, null, null));
    }

    public void askYesNo(String sMsg) {
        askYesNo(0, sMsg);
    }

    public void askYesNo(int bParam, String sMsg) {
        askYesNo(id, bParam, sMsg);
    }

    public void askYesNo(int nSpeakerTemplateID, int bParam, String sMsg) {
        askYesNo(nSpeakerTemplateID, -1, bParam, sMsg);
    }

    public void askYesNo(int nSpeakerTemplateID, int nAnotherSpeakerTemplateID, int bParam, String sMsg) {
        askYesNo(nSpeakerTemplateID, nAnotherSpeakerTemplateID, -1, bParam, sMsg);
    }

    public void askYesNo(int nSpeakerTemplateID, int nAnotherSpeakerTemplateID, int nOtherSpeakerTemplateID, int bParam,
            String sMsg) {
        askYesNo(4, nSpeakerTemplateID, nAnotherSpeakerTemplateID, nOtherSpeakerTemplateID, bParam, 0, sMsg);
    }

    public void askYesNo(int nSpeakerTypeID, int nSpeakerTemplateID, int nAnotherSpeakerTemplateID,
            int nOtherSpeakerTemplateID, int bParam, int eColor, String sMsg) {
        if (sMsg.contains("#L")) {
            askMenu(nSpeakerTypeID, nSpeakerTemplateID, nAnotherSpeakerTemplateID, nOtherSpeakerTemplateID, bParam,
                    eColor, sMsg);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKYESNO;
        c.getSession()
                .writeAndFlush(CScriptMan.OnScriptMessage(nSpeakerTypeID, nSpeakerTemplateID, nAnotherSpeakerTemplateID,
                        nOtherSpeakerTemplateID, lastMsg, bParam, eColor, new String[]{sMsg}, null, null, null));
    }

    public void askMenu(String sMsg) {
        askMenu(0, sMsg);
    }

    public void askMenu(int bParam, String sMsg) {
        askMenu(id, bParam, sMsg);
    }

    public void askMenu(int nSpeakerTemplateID, int bParam, String sMsg) {
        askMenu(nSpeakerTemplateID, -1, bParam, sMsg);
    }

    public void askMenu(int nSpeakerTemplateID, int nAnotherSpeakerTemplateID, int bParam, String sMsg) {
        askMenu(nSpeakerTemplateID, nAnotherSpeakerTemplateID, -1, bParam, sMsg);
    }

    public void askMenu(int nSpeakerTemplateID, int nAnotherSpeakerTemplateID, int nOtherSpeakerTemplateID, int bParam,
            String sMsg) {
        askMenu(4, nSpeakerTemplateID, nAnotherSpeakerTemplateID, nOtherSpeakerTemplateID, bParam, 0, sMsg);
    }

    public void askMenu(int nSpeakerTypeID, int nSpeakerTemplateID, int nAnotherSpeakerTemplateID,
            int nOtherSpeakerTemplateID, int bParam, int eColor, String sMsg) {
        if (!sMsg.contains("#L")) {
            say(nSpeakerTypeID, nSpeakerTemplateID, nAnotherSpeakerTemplateID, nOtherSpeakerTemplateID, bParam, eColor,
                    sMsg, false, false, 0);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKMENU;
        c.getSession()
                .writeAndFlush(CScriptMan.OnScriptMessage(nSpeakerTypeID, nSpeakerTemplateID, nAnotherSpeakerTemplateID,
                        nOtherSpeakerTemplateID, lastMsg, bParam, eColor, new String[]{sMsg}, null, null, null));
    }

    public void sendNext(String text) {
        sendNext(text, id);
    }

    public void sendNext(String text, int id) {
        if (text.contains("#L")) { // sendNext will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "00 01", (byte) 0));
    }

    public void sendPlayerToNpc(String text) {
        sendNextS(text, (byte) 3, id);
    }

    public void sendNextNoESC(String text) {
        sendNextS(text, (byte) 1, id);
    }

    public void sendNextNoESC(String text, int id) {
        sendNextS(text, (byte) 1, id);
    }

    public void sendNextS(String text, byte type) {
        sendNextS(text, type, id);
    }

    public void sendNextS(String text, byte type, int idd) {
        sendNextS(text, type, idd, id);
    }

    public void sendNextS(String text, byte type, int idd, int npcid) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(npcid, lastMsg, text, "00 01", type, idd));
    }

    public void sendOthersTalk(String text, int npcid, boolean[] bottom) {
        sendOthersTalk(text, npcid, bottom, (byte) 1);
    }

    public void sendOthersTalk(String text, int npcid, boolean[] bottom, byte type) {
        String str = "";
        if (bottom.length >= 2) {
            for (int i = 0; i < 2; i++) {
                if (bottom[i]) {
                    str += "01";
                } else {
                    str += "00";
                }
                if (i < bottom.length - 1) {
                    str += " ";
                }
            }
        } else {
            str = "00 01";
        }
        if (text.contains("#L")) {
            lastMsg = ScriptMessageType.SM_ASKMENU;
            c.getSession().writeAndFlush(CScriptMan.getOthersTalk(id, lastMsg, npcid, text, "", type));
        } else {
            lastMsg = ScriptMessageType.SM_SAY;
            c.getSession().writeAndFlush(CScriptMan.getOthersTalk(id, lastMsg, npcid, text, str, type));
        }
    }

    public void sendNextSNew(String text, byte type, byte type2) {
        sendNextSNew(text, type, type2, id);
    }

    public void sendNextSNew(String text, byte type, byte type2, int idd) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleSNew(text, type, type2);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "00 01", type, type2, idd));
    }

    public void sendPrev(String text) {
        sendPrev(text, id);
    }

    public void sendPrev(String text, int id) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "01 00", (byte) 0));
    }

    public void getAdviceTalk(String[] wzinfo) {
        lastMsg = ScriptMessageType.SM_SAYIMAGE;
        c.getSession().writeAndFlush(CScriptMan.getAdviceTalk(wzinfo));
    }

    public void sendZeroTalk(String talk) {
        c.getSession()
                .writeAndFlush(CScriptMan.getZeroNPCTalk(id, ScriptMessageType.SM_SAY, talk, "01 00", (byte) 3, 0));
    }

    public void sendSangokuTalk(String talk, int idd, boolean next, boolean prev) {
        sendSangokuTalk(talk, 5, idd, next, prev);
    }

    public void sendSangokuTalk(boolean unknown, String talk, int idd, boolean next, boolean prev) {
        sendSangokuTalk(unknown, talk, 5, idd, next, prev);
    }

    public void sendSangokuTalk(String talk, int idd, boolean next, boolean prev, boolean pic) {
        sendSangokuTalk(talk, 5, idd, next, prev, pic);
    }

    public void sendSangokuTalk(boolean unknown, String talk, int idd, boolean next, boolean prev, boolean pic) {
        sendSangokuTalk(unknown, talk, 5, idd, next, prev, pic);
    }

    public void sendSangokuTalk(String talk, int type, int idd, boolean next, boolean prev) {
        sendSangokuTalk(false, talk, ScriptMessageType.SM_SAY_ILLUSTRATION.getType(), (byte) type, idd, next, prev,
                true);
    }

    public void sendSangokuTalk(boolean unknown, String talk, int type, int idd, boolean next, boolean prev) {
        sendSangokuTalk(unknown, talk, ScriptMessageType.SM_SAY_ILLUSTRATION.getType(), (byte) type, idd, next, prev,
                true);
    }

    public void sendSangokuTalk(String talk, int type, int idd, boolean next, boolean prev, boolean pic) {
        sendSangokuTalk(false, talk, ScriptMessageType.SM_SAY_ILLUSTRATION.getType(), (byte) type, idd, next, prev,
                pic);
    }

    public void sendSangokuTalk(boolean unknown, String talk, int type, int idd, boolean next, boolean prev,
            boolean pic) {
        sendSangokuTalk(unknown, talk, ScriptMessageType.SM_SAY_ILLUSTRATION.getType(), (byte) type, idd, next, prev,
                pic);
    }

    public void sendSangokuTalk(boolean unknown, String talk, int msgType, int type, int idd, boolean next,
            boolean prev, boolean pic) {
        ScriptMessageType tt = ScriptMessageType.getNPCTalkType(msgType);
        c.getSession()
                .writeAndFlush(CScriptMan.getSengokuNPCTalk(unknown, id, tt, (byte) type, idd, talk, next, prev, pic));
        lastMsg = tt;
    }

    public void sendArisanNPCTalk(String talk) { // 阿里山
        sendArisanNPCTalk(false, ScriptMessageType.SM_ASKACCEPT.getType(), (byte) 56, (byte) 1, talk);
    }

    public void sendArisanNPCTalk(boolean read, byte msgType, byte type, byte result, String talk) {
        ScriptMessageType tt = ScriptMessageType.getNPCTalkType(msgType);
        c.getSession().writeAndFlush(CScriptMan.getArisanNPCTalk(0, false, tt, (byte) type, (byte) result, talk));
        lastMsg = tt;
    }

    public void sendDreamWorldNPCTalk(String talk) { // 虛幻之森
        sendDreamWorldNPCTalk(false, ScriptMessageType.SM_ASKYESNO.getType(), (byte) 0x05, (byte) 0x00, talk);
    }

    public void sendDreamWorldNPCTalk(boolean read, byte msgType, byte type, byte result, String talk) {
        ScriptMessageType tt = ScriptMessageType.getNPCTalkType(msgType);
        c.getSession()
                .writeAndFlush(CScriptMan.getDreamWorldNPCTalk(0, false, tt, (byte) type, (byte) result, id, talk));
        lastMsg = tt;
    }

    public void sendPrevS(String text, byte type) {
        sendPrevS(text, type, id);
    }

    public void sendPrevS(String text, byte type, int idd) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "01 00", type, idd));
    }

    public void sendPrevSNew(String text, byte type, byte type2) {
        sendPrevSNew(text, type, type2, id);
    }

    public void sendPrevSNew(String text, byte type, byte type2, int idd) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleSNew(text, type, type2);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "01 00", type, type2, idd));
    }

    public void sendNextPrev(String text) {
        sendNextPrev(text, id);
    }

    public void sendNextPrev(String text, int id) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "01 01", (byte) 0));
    }

    public void PlayerToNpc(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text) {
        sendNextPrevS(text, (byte) 3);
    }

    public void sendNextPrevS(String text, byte type) {
        sendNextPrevS(text, type, id);
    }

    public void sendNextPrevS(String text, byte type, int idd) {
        sendNextPrevS(text, type, idd, id);
    }

    public void sendNextPrevS(String text, byte type, int idd, int npcid) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(npcid, lastMsg, text, "01 01", type, idd));
    }

    public void sendNextPrevSNew(String text, byte type, byte type2) {
        sendNextPrevSNew(text, type, type2, id);
    }

    public void sendNextPrevSNew(String text, byte type, byte type2, int idd) {
        sendNextPrevSNew(text, type, type2, idd, id);
    }

    public void sendNextPrevSNew(String text, byte type, byte type2, int idd, int npcid) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleSNew(text, type, type2);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(npcid, lastMsg, text, "01 01", type, type2, idd));
    }

    public void sendOk(String text) {
        sendOk(text, id);
    }

    public void sendOk(String text, int id) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "00 00", (byte) 0));
    }

    public void sendOkS(String text, byte type) {
        sendOkS(text, type, id);
    }

    public void sendOkS(String text, byte type, int idd) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "00 00", type, idd));
    }

    public void sendOkSNew(String text, byte type, byte type2) {
        sendOkSNew(text, type, type2, id);
    }

    public void sendOkSNew(String text, byte type, byte type2, int idd) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleSNew(text, type, type2);
            return;
        }
        lastMsg = ScriptMessageType.SM_SAY;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "00 00", type, type2, idd));
    }

    public void sendSelfTalk(String text) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type.getValue());
            return;
        }
        c.getSession().writeAndFlush(CScriptMan.getSelfTalkText(text));
        lastMsg = ScriptMessageType.SM_SAY;
    }

    public void sendYesNo(String text) {
        sendYesNo(text, id);
    }

    public void sendYesNo(String text, int id) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKYESNO;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "", (byte) 0));
    }

    public void sendYesNoS(String text, byte type) {
        sendYesNoS(text, type, id);
    }

    public void sendYesNoS(String text, byte type, int idd) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleS(text, type);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKYESNO;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "", type, idd));
    }

    public void sendYesNoSNew(String text, byte type, byte type2) {
        sendYesNoSNew(text, type, type2, id);
    }

    public void sendYesNoSNew(String text, byte type, byte type2, int idd) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimpleSNew(text, type, type2);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKYESNO;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "", type, type2, idd));
    }

    public void sendAcceptDecline(String text) {
        askAcceptDecline(text);
    }

    public void sendAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text);
    }

    public void askAcceptDecline(String text) {
        askAcceptDecline(text, id);
    }

    public void askAcceptDecline(String text, int id) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKACCEPT;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "", (byte) 0));
    }

    public void askAcceptDeclineNoESC(String text) {
        askAcceptDeclineNoESC(text, id);
    }

    public void askAcceptDeclineNoESC(String text, int id) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKACCEPT;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "", (byte) 1));
    }

    public void askAngelicBusterAvatar() {
        c.getSession().writeAndFlush(CScriptMan.getAngelicBusterAvatarSelect(id));
        lastMsg = ScriptMessageType.SM_ASKANGELICBUSTER;
    }

    public void askAvatar(String text, int[] args) {
        askAvatar(text, args, 0);
    }

    public void askAvatar(String text, int[] args, int card) {
        c.getSession().writeAndFlush(CScriptMan.getNPCTalkStyle(id, text, args, card, false));
        lastMsg = ScriptMessageType.SM_ASKAVATAREX;
    }

    public boolean revivePet(int uniqueId, int itemId) {
        Item item = c.getPlayer().getInventory(MapleInventoryType.CASH).findByUniqueId(uniqueId);
        if (item == null) {
            return false;
        }
        if (!ItemConstants.類型.寵物(item.getItemId())) {
            return false;
        }
        if (item.getPet() == null) {
            return false;
        }
        if (MapleItemInformationProvider.getInstance().getLimitedLife(item.getItemId()) != 0) {
            return false;
        }
        if (item.getExpiration() <= 0 || item.getExpiration() > System.currentTimeMillis()) {
            return false;
        }
        switch (itemId) {
            case 4070000:
            case 5180000:
            case 5689000:
                item.setExpiration(System.currentTimeMillis() + (90 * 24 * 60 * 60 * 1000L));
                break;
            case 5180003:
                item.setExpiration(0);
                break;
            default:
                return false;
        }
        c.getPlayer().forceReAddItem_Flag(item);
        return true;
    }

    public MaplePet[] getDeadPets() {
        List<MaplePet> petList = new LinkedList<>();

        for (final Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
            if (!ItemConstants.類型.寵物(item.getItemId())) {
                continue;
            }
            if (item.getPet() == null) {
                continue;
            }
            if (MapleItemInformationProvider.getInstance().getLimitedLife(item.getItemId()) != 0) {
                continue;
            }
            if (item.getExpiration() <= 0 || item.getExpiration() > System.currentTimeMillis()) {
                continue;
            }
            petList.add(item.getPet());
        }
        MaplePet[] petArray = new MaplePet[petList.size()];
        for (int i = 0; i < petArray.length; i++) {
            petArray[i] = petList.get(i);
        }
        return petList.isEmpty() ? null : petArray;
    }

    public void askPet(String text, MaplePet[] pets) {
        c.getSession().writeAndFlush(CScriptMan.OnScriptMessage(4, id, -1, -1, ScriptMessageType.SM_ASKPET, 0, 0,
                new String[]{text}, null, null, pets));
        lastMsg = ScriptMessageType.SM_ASKPET;
    }

    public void sendSimple(String text) {
        sendSimple(text, id);
    }

    public void sendSimple(String text, int id) {
        if (!text.contains("#L")) { // sendSimple will dc otherwise!
            sendNext(text);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKMENU;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, lastMsg, text, "", (byte) 0));
    }

    public void sendSimpleS(String text, byte type) {
        sendSimpleS(text, type, id);
    }

    public void sendSimpleS(String text, byte type, int idd) {
        if (!text.contains("#L")) { // sendSimple will dc otherwise!
            sendNextS(text, type);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKMENU;
        c.getSession().writeAndFlush(CScriptMan.getNPCTalk(id, ScriptMessageType.SM_ASKMENU, text, "", type, idd));
    }

    public void sendSimpleSNew(String text, byte type, byte type2) {
        sendSimpleSNew(text, type, type2, id);
    }

    public void sendSimpleSNew(String text, byte type, byte type2, int idd) {
        if (!text.contains("#L")) { // sendSimple will dc otherwise!
            sendNextSNew(text, type, type2);
            return;
        }
        lastMsg = ScriptMessageType.SM_ASKMENU;
        c.getSession()
                .writeAndFlush(CScriptMan.getNPCTalk(id, ScriptMessageType.SM_ASKMENU, text, "", type, type2, idd));
    }

    public void sendStyle(String text, int styles[]) {
        sendStyle(text, styles, 0);
    }

    public void sendStyle(String text, int styles[], int card) {
        c.getSession().writeAndFlush(CScriptMan.getNPCTalkStyle(id, text, styles, card, false));
        lastMsg = ScriptMessageType.SM_ASKAVATAREX;
    }

    public void sendSecondStyle(String text, int styles[]) {
        sendSecondStyle(text, styles, 0);
    }

    public void sendSecondStyle(String text, int styles[], int card) {
        c.getSession().writeAndFlush(CScriptMan.getNPCTalkStyle(id, text, styles, card, true));
        lastMsg = ScriptMessageType.SM_ASKAVATAREX;
    }

    public void sendGetNumber(String text, int def, int min, int max) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(CScriptMan.getNPCTalkNum(id, text, def, min, max));
        lastMsg = ScriptMessageType.SM_ASKNUMBER;
    }

    public void sendGetText(String text) {
        sendGetText(text, id);
    }

    public void sendGetText(String text, int id) {
        if (text.contains("#L")) { // will dc otherwise!
            sendSimple(text);
            return;
        }
        c.getSession().writeAndFlush(CScriptMan.getNPCTalkText(id, text));
        lastMsg = ScriptMessageType.SM_ASKTEXT;
    }

    public void setGetText(String text) {
        this.getText = text;
    }

    public String getText() {
        return getText;
    }

    public void setHair(int hair) {
        if (hairExists(hair)) {
            getPlayer().setHair(hair);
            getPlayer().updateSingleStat(MapleStat.HAIR, hair);
            getPlayer().equipChanged();
        }
    }

    public void setFace(int face) {
        if (faceExists(face)) {
            getPlayer().setFace(face);
            getPlayer().updateSingleStat(MapleStat.FACE, face);
            getPlayer().equipChanged();
        }
    }

    public void setSkin(int color) {
        getPlayer().setSkinColor((byte) color);
        getPlayer().updateSingleStat(MapleStat.SKIN, color);
        getPlayer().equipChanged();
    }

    public static boolean hairExists(int hair) {
        return MapleItemInformationProvider.getInstance().itemExists(hair);
    }

    public static boolean faceExists(int face) {
        return MapleItemInformationProvider.getInstance().itemExists(face);
    }

    public int[] getCanHair(int[] hairs) {
        List<Integer> canHair = new ArrayList<>();
        List<Integer> cantHair = new ArrayList<>();
        for (int hair : hairs) {
            if (hairExists(hair)) {
                canHair.add(hair);
            } else {
                cantHair.add(hair);
            }
        }
        if (cantHair.size() > 0 && c.getPlayer().isShowErr()) {
            StringBuilder sb = new StringBuilder("正在讀取的髮型里有");
            sb.append(cantHair.size()).append("個髮型用戶端不支援顯示，已經被清除：");
            for (int i = 0; i < cantHair.size(); i++) {
                sb.append(cantHair.get(i));
                if (i < cantHair.size() - 1) {
                    sb.append(",");
                }
            }
            playerMessage(sb.toString());
        }
        int[] getHair = new int[canHair.size()];
        for (int i = 0; i < canHair.size(); i++) {
            getHair[i] = canHair.get(i);
        }
        return getHair;
    }

    public int[] getCanFace(int[] faces) {
        List<Integer> canFace = new ArrayList<>();
        List<Integer> cantFace = new ArrayList<>();
        for (int face : faces) {
            if (faceExists(face)) {
                canFace.add(face);
            } else {
                cantFace.add(face);
            }
        }
        if (cantFace.size() > 0 && c.getPlayer().isShowErr()) {
            StringBuilder sb = new StringBuilder("正在讀取的臉型里有");
            sb.append(cantFace.size()).append("個臉型用戶端不支援顯示，已經被清除：");
            for (int i = 0; i < cantFace.size(); i++) {
                sb.append(cantFace.get(i));
                if (i < cantFace.size() - 1) {
                    sb.append(",");
                }
            }
            playerMessage(sb.toString());
        }
        int[] getFace = new int[canFace.size()];
        for (int i = 0; i < canFace.size(); i++) {
            getFace[i] = canFace.get(i);
        }
        return getFace;
    }

    public static boolean itemExists(int itemId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Pair<Integer, String> item : ii.getAllItems2()) {
            if (item.getLeft() == itemId) {
                return true;
            }
        }
        return false;
    }

    public int setRandomAvatar(int ticket, int... args_all) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        int args = args_all[Randomizer.nextInt(args_all.length)];
        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            if (faceExists(args)) {
                c.getPlayer().setFace(args);
                c.getPlayer().updateSingleStat(MapleStat.FACE, args);
            }
        } else {
            if (hairExists(args)) {
                c.getPlayer().setHair(args);
                c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
            }
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public int setRandomAndroid(int ticket, int... args_all) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        int args = args_all[Randomizer.nextInt(args_all.length)];
        if (args < 100) {
            c.getPlayer().getAndroid().setSkin(args);
            c.getPlayer().getAndroid().saveToDb();
        } else if (args < 30000) {
            if (faceExists(args)) {
                c.getPlayer().getAndroid().setFace(args);
                c.getPlayer().getAndroid().saveToDb();
            }
        } else {
            if (hairExists(args)) {
                c.getPlayer().getAndroid().setHair(args);
                c.getPlayer().getAndroid().saveToDb();
            }
        }
        CField.updateAndroidLook(c.getPlayer(), c.getPlayer().getAndroid());
        c.getPlayer().setAndroid(c.getPlayer().getAndroid()); // Respawn it
        c.getPlayer().equipChanged();

        return 1;
    }

    public int setRandomAvatar(int ticket) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);
        int args = getRoya(ticket);
        if (args <= 0) {
            return -1;
        }
        if (args < 100) {
            this.c.getPlayer().setSkinColor((byte) args);
            this.c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            if (faceExists(args)) {
                this.c.getPlayer().setFace(args);
                this.c.getPlayer().updateSingleStat(MapleStat.FACE, args);
            }
        } else {
            if (hairExists(args)) {
                this.c.getPlayer().setHair(args);
                this.c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
            }
        }
        this.c.getPlayer().equipChanged();
        return 1;
    }

    public int setRandomAndroid(int ticket) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);
        int args = getRoya(ticket);
        if (args <= 0) {
            return -1;
        }
        if (args < 100) {
            c.getPlayer().getAndroid().setSkin(args);
            c.getPlayer().getAndroid().saveToDb();
        } else if (args < 30000) {
            if (faceExists(args)) {
                c.getPlayer().getAndroid().setFace(args);
                c.getPlayer().getAndroid().saveToDb();
            }
        } else {
            if (hairExists(args)) {
                c.getPlayer().getAndroid().setHair(args);
                c.getPlayer().getAndroid().saveToDb();
            }
        }
        CField.updateAndroidLook(c.getPlayer(), c.getPlayer().getAndroid());
        c.getPlayer().setAndroid(c.getPlayer().getAndroid()); // Respawn it
        c.getPlayer().equipChanged();
        return 1;
    }

    public int[] getHairRoyaCoupon() {
        CashItemFactory cif = CashItemFactory.getInstance();
        List<Integer> roya = new LinkedList<>();
        for (int itemId : cif.getRoyaCoupon().keySet()) {
            if (itemId / 1000 == 5150) {
                roya.add(itemId);
            }
        }
        int[] hairRoya = new int[roya.size()];
        for (int i = 0; i < roya.size(); i++) {
            hairRoya[i] = roya.get(i);
        }
        return hairRoya;
    }

    public int[] getFaceRoyaCoupon() {
        CashItemFactory cif = CashItemFactory.getInstance();
        List<Integer> roya = new LinkedList<>();
        for (int itemId : cif.getRoyaCoupon().keySet()) {
            if (itemId / 1000 == 5152) {
                roya.add(itemId);
            }
        }
        int[] hairRoya = new int[roya.size()];
        for (int i = 0; i < roya.size(); i++) {
            hairRoya[i] = roya.get(i);
        }
        return hairRoya;
    }

    public int getRoya(int itemId) {
        return getRoya(itemId, c.getPlayer().getGender());
    }

    public int getRoya(int itemId, int gender) {
        Map<Integer, RoyaCoupon> royaCoupon = CashItemFactory.getInstance().getRoyaCoupon();
        if (royaCoupon == null || !royaCoupon.containsKey(itemId)) {
            return 0;
        }
        List<RoyaCoupon.RoyaInfo> royaList = gender == 0 ? royaCoupon.get(itemId).getMaleStyles(false)
                : royaCoupon.get(itemId).getFemaleStyles(false);
        if (royaList == null || royaList.isEmpty()) {
            return 0;
        }
        RoyaCoupon.RoyaInfo roya = null;
        int times = 0;
        while (roya == null) {
            roya = royaList.get(Randomizer.nextInt(royaList.size()));
            if (Randomizer.nextInt(999999) >= roya.chance) {
                roya = null;
            }
            if (times > 300) {
                return 0;
            }
            times++;
        }
        return roya.styleId;
    }

    public int setAvatar(int ticket, int args) {
        if (!haveItem(ticket)) {
            return -1;
        }
        gainItem(ticket, (short) -1);

        if (args < 100) {
            c.getPlayer().setSkinColor((byte) args);
            c.getPlayer().updateSingleStat(MapleStat.SKIN, args);
        } else if (args < 30000) {
            if (faceExists(args)) {
                c.getPlayer().setFace(args);
                c.getPlayer().updateSingleStat(MapleStat.FACE, args);
            }
        } else {
            if (hairExists(args)) {
                c.getPlayer().setHair(args);
                c.getPlayer().updateSingleStat(MapleStat.HAIR, args);
            }
        }
        c.getPlayer().equipChanged();

        return 1;
    }

    public void sendStorage() {
        c.getPlayer().setConversation(4);
        c.getPlayer().getStorage().sendStorage(c, id);
    }

    public void openShop(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(c);
    }

    public void openShopNPC(int id) {
        MapleShopFactory.getInstance().getShop(id).sendShop(c, this.id);
    }

    public int gachapon(int type) {
        MapleGachaponItem gitem = MapleGachapon.randomItem(type);
        if (gitem == null || !MapleItemInformationProvider.getInstance().itemExists(gitem.getItemId())) {
            return -1;
        }
        int quantity = MapleGachapon.gainItem(gitem);
        if (quantity <= 0) {
            return -1;
        }
        final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, gitem.getItemId(), (short) quantity);

        if (item == null) {
            return -1;
        }

        if (gitem.getSmegaType() > -1) {
            World.Broadcast.broadcastMessage(CWvsContext.getGachaponMega(c.getPlayer().getName(), "楓葉轉蛋機",
                    (quantity > 1 ? ("x" + quantity) : ""), item, c.getChannel(), gitem.getSmegaType()));
        }
        return item.getItemId();
    }

    public int gainGachaponItem(int id, int quantity) {
        return gainGachaponItem(id, quantity, c.getPlayer().getMap().getStreetName());
    }

    public int gainGachaponItem(int id, int quantity, final String msg) {
        return gainGachaponItem(id, quantity, msg, (byte) 0);
    }

    public int gainGachaponItem(int id, int quantity, final String msg, int rareness) {
        try {
            if (!MapleItemInformationProvider.getInstance().itemExists(id)) {
                return -1;
            }
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, id, (short) quantity);

            if (item == null) {
                return -1;
            }
            if (rareness > -1) {
                World.Broadcast.broadcastMessage(CWvsContext.getGachaponMega(c.getPlayer().getName(), "楓葉轉蛋機", msg,
                        item, c.getChannel(), rareness));
            }
            c.getSession().writeAndFlush(InfoPacket.getShowItemGain(item.getItemId(), (short) quantity, true));
            return item.getItemId();
        } catch (Exception e) {
        }
        return -1;
    }

    public int useNebuliteGachapon() {
        try {
            if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 1
                    || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 1
                    || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 1
                    || c.getPlayer().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < 1
                    || c.getPlayer().getInventory(MapleInventoryType.CASH).getNumFreeSlot() < 1) {
                return -1;
            }
            int grade; // Default D
            final int chance = Randomizer.nextInt(100); // cannot gacha S, only from alien cube.
            if (chance < 1) { // Grade A
                grade = 3;
            } else if (chance < 3) { // Grade B
                grade = 2;
            } else if (chance < 40) { // Grade C
                grade = 1;
            } else { // grade == 0
                grade = Randomizer.nextInt(100) < 25 ? 5 : 0; // 25% again to get premium ticket piece
            }
            int newId = 0;
            if (grade == 5) {
                newId = 4420000;
            } else {
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final List<StructItemOption> pots = new LinkedList<>(ii.getAllSocketInfo(grade).values());
                while (newId == 0) {
                    StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
                    if (pot != null) {
                        newId = pot.opID;
                    }
                }
            }
            final Item item = MapleInventoryManipulator.addbyId_Gachapon(c, newId, (short) 1);
            if (item == null) {
                return -1;
            }
            if (grade >= 3 && grade != 5) {
                World.Broadcast.broadcastMessage(
                        CWvsContext.getGachaponMega(c.getPlayer().getName(), "楓之谷世界", null, item, c.getChannel(), 0));
            }
            c.getSession().writeAndFlush(InfoPacket.getShowItemGain(newId, (short) 1, true));
            gainItem(2430748, (short) 1);
            gainItemSilent(5220094, (short) -1);
            return item.getItemId();
        } catch (Exception e) {
            System.out.println("[Error] Failed to use Nebulite Gachapon. " + e);
        }
        return -1;
    }

    public void changeJob(short job) {
        c.getPlayer().changeJob(job);
    }

    public void startQuest(int idd) {
        startQuest(idd, id);
    }

    public void completeQuest(int idd) {
        MapleQuest.getInstance(idd).complete(getPlayer(), id);
    }

    public void forfeitQuest(int idd) {
        MapleQuest.getInstance(idd).forfeit(getPlayer());
    }

    public void forceStartQuest() {
        MapleQuest.getInstance(id2).forceStart(getPlayer(), getNpc(), null);
    }

    @Override
    public void forceStartQuest(int idd) {
        MapleQuest.getInstance(idd).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(String customData) {
        MapleQuest.getInstance(id2).forceStart(getPlayer(), getNpc(), customData);
    }

    public void forceCompleteQuest() {
        MapleQuest.getInstance(id2).forceComplete(getPlayer(), getNpc());
    }

    @Override
    public void forceCompleteQuest(final int idd) {
        MapleQuest.getInstance(idd).forceComplete(getPlayer(), getNpc());
    }

    public String getQuestCustomData() {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)).getCustomData();
    }

    public String getQuestCustomData(int quest) {
        return c.getPlayer().getQuestNAdd(MapleQuest.getInstance(quest)).getCustomData();
    }

    public void setQuestCustomData(String customData) {
        getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)).setCustomData(customData);
        getPlayer().updateQuest(getPlayer().getQuestNAdd(MapleQuest.getInstance(id2)));
    }

    public void setQuestCustomData(final int idd, String customData) {
        getPlayer().getQuestNAdd(MapleQuest.getInstance(idd)).setCustomData(customData);
        getPlayer().updateQuest(getPlayer().getQuestNAdd(MapleQuest.getInstance(idd)));
    }

    public long getMeso() {
        return getPlayer().getMeso();
    }

    public void gainAp(final int amount) {
        c.getPlayer().gainAp((short) amount);
    }

    public void expandInventory(byte type, int amt) {
        c.getPlayer().expandInventory(type, amt);
    }

    public final void clearSkills() {
        final Map<Skill, SkillEntry> skills = new HashMap<>(getPlayer().getSkills());
        final Map<Skill, SkillEntry> newList = new HashMap<>();
        for (Entry<Skill, SkillEntry> skill : skills.entrySet()) {
            newList.put(skill.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
        }
        getPlayer().changeSkillsLevel(newList);
        newList.clear();
        skills.clear();
    }

    public boolean hasSkill(int skillid) {
        Skill theSkill = SkillFactory.getSkill(skillid);
        if (theSkill != null) {
            return c.getPlayer().getSkillLevel(theSkill) > 0;
        }
        return false;
    }

    public void spawnNPCRequestController(int npcid, int x, int y) {
        spawnNPCRequestController(npcid, x, y, 0);
    }

    public void spawnNPCRequestController(int npcid, int x, int y, int f) {
        spawnNPCRequestController(npcid, x, y, f, npcid);
    }

    public void spawnNPCRequestController(int npcid, int x, int y, int f, int oid) {
        if (npcRequestController.containsKey(new Pair<>(oid, c))) {
            npcRequestController.remove(new Pair<>(oid, c));
        }
        MapleNPC npc;
        npc = c.getPlayer().getMap().getNPCById(npcid);
        if (npc == null) {
            npc = MapleLifeFactory.getNPC(npcid);
            if (npc == null) {
                return;
            }
            npc.setPosition(new Point(x, y));
            npc.setCy(y);
            npc.setRx0(x - 50);
            npc.setRx1(x + 50);
            npc.setF(f);
            MapleFoothold fh = c.getPlayer().getMap().getFootholds().findBelow(new Point(x, y), false);
            npc.setFh(fh == null ? 0 : fh.getId());
            npc.setCustom(true);
            npc.setObjectId(oid);
        }
        npcRequestController.put(new Pair<>(oid, c), npc);
        c.getSession().writeAndFlush(NPCPacket.spawnNPCRequestController(npc, true));// isMiniMap
        c.getSession().writeAndFlush(NPCPacket.setNPCSpecialAction(npc.getObjectId(), "summon", 0, false));
    }

    public void getNPCBubble(int npcid, String data, int exclamation, int bubbleType, int time, int directionTime) {
        c.getSession().writeAndFlush(
                CField.EffectPacket.showEffect(true, c.getPlayer(), UserEffectOpcode.UserEffect_SpeechBalloon,
                        new int[]{exclamation, bubbleType, 0, time, 1, 0, 0, 0, 4, npcid}, new String[]{data},
                        null, null));
        if (directionTime > -1) {
            exceTime(directionTime > 0 ? directionTime : time);
        }
    }

    public void setNPCSpecialAction(int npcid, String action) {
        setNPCSpecialAction(npcid, action, 0, false);
    }

    public void setNPCSpecialAction(int npcid, String action, int time, boolean unk) {
        setNPCSpecialAction(npcid, action, time, unk, -1);
    }

    public void setNPCSpecialAction(int npcid, String action, int time, boolean unk, int directionTime) {
        final MapleNPC npc;
        if (npcRequestController.containsKey(new Pair<>(npcid, c))) {
            npc = npcRequestController.get(new Pair<>(npcid, c));
        } else {
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.setNPCSpecialAction(npc.getObjectId(), action, time, unk));
        if (directionTime > -1) {
            exceTime(directionTime > 0 ? directionTime : time);
        }
    }

    public void updateNPCSpecialAction(int oid, int value, int x, int y) {
        final MapleNPC npc;
        if (npcRequestController.containsKey(new Pair<>(oid, c))) {
            npc = npcRequestController.get(new Pair<>(oid, c));
        } else {
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.NPCSpecialAction(npc.getObjectId(), value, x, y));
    }

    public void getNPCDirectionEffect(int npcid, String data, int value, int x, int y) {
        final MapleNPC npc;
        if (npcRequestController.containsKey(new Pair<>(npcid, c))) {
            npc = npcRequestController.get(new Pair<>(npcid, c));
        } else {
            return;
        }
        c.getSession().writeAndFlush(
                CField.UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay, data,
                        new int[]{value, x, y, 1, 1, 0, npc.getObjectId(), 0}));
    }

    public void removeNPCRequestController(int oid) {
        final MapleNPC npc;
        if (npcRequestController.containsKey(new Pair<>(oid, c))) {
            npc = npcRequestController.get(new Pair<>(oid, c));
        } else {
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.removeNPCController(npc.getObjectId()));
        c.getSession().writeAndFlush(NPCPacket.removeNPC(npc.getObjectId()));
        npcRequestController.remove(new Pair<>(oid, c));
    }

    public final void resetNPCController(final int npcId) {
        final MapleNPC npc;
        if (npcRequestController.containsKey(new Pair<>(npcId, c))) {
            npc = npcRequestController.get(new Pair<>(npcId, c));
        } else {
            return;
        }
        c.getSession().writeAndFlush(NPCPacket.resetNPC(npc.getObjectId()));
    }

    public void forcedAction(int[] values) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedAction.getValue(), null, values);
    }

    public void exceTime(int time) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_Delay.getValue(), null, new int[]{time});
    }

    public void getEventEffect(String data, int[] values) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay.getValue(), data, values);
    }

    public void playerWaite() {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedInput.getValue(), null,
                new int[]{0});
    }

    public void playerMoveLeft() {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedInput.getValue(), null,
                new int[]{1});
    }

    public void playerMoveRight() {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedInput.getValue(), null,
                new int[]{2});
    }

    public void playerJump() {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedInput.getValue(), null,
                new int[]{3});
    }

    public void playerMoveDown() {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedInput.getValue(), null,
                new int[]{4});
    }

    public void forcedInput(int input) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedInput.getValue(), null,
                new int[]{input});
    }

    public final void patternInput(String data, int[] values) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_PatternInputRequest.getValue(), data,
                values);
    }

    public final void cameraMove(int[] values) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_CameraMove.getValue(), null, values);
    }

    public final void cameraOnCharacter(int value) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_CameraOnCharacter.getValue(), null,
                new int[]{value});
    }

    public final void cameraZoom(int[] values) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_CameraZoom.getValue(), null, values);
    }

    public final void hidePlayer(boolean hide) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_VansheeMode.getValue(), null,
                new int[]{hide ? 1 : 0});
    }

    public final void faceOff(int value) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_FaceOff.getValue(), null,
                new int[]{value});
    }

    public void sendTellStory(String data, boolean lastLine) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_Monologue.getValue(), data,
                new int[]{lastLine ? 1 : 0});
    }

    public void removeAdditionalEffect() {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_RemoveAdditionalEffect.getValue(), null,
                null);
    }

    public void forcedMove(int value, int value1) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedMove.getValue(), null,
                new int[]{value, value1});
    }

    public void forcedFlip(int value) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_ForcedFlip.getValue(), null,
                new int[]{value});
    }

    public void inputUI(int value) {
        getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_InputUI.getValue(), null,
                new int[]{value});
    }

    public void getDirectionEffect(int mod, String data, int[] values) {
        InGameDirectionEventOpcode type = InGameDirectionEventOpcode.getType(mod);
        c.getSession().writeAndFlush(UIPacket.getDirectionEffect(type, data, values));
        if (lastMsg != null) {
            return;
        }
        switch (type) {
            case InGameDirectionEvent_Delay:
            case InGameDirectionEvent_ForcedInput:
            case InGameDirectionEvent_PatternInputRequest:
            case InGameDirectionEvent_CameraMove:
            case InGameDirectionEvent_CameraZoom:
                lastMsg = ScriptMessageType.SM_ASKINGAMEDIRECTION;
                break;
            case InGameDirectionEvent_Monologue:
                lastMsg = ScriptMessageType.SM_MONOLOGUE;
                break;
        }
    }

    public void getDirectionFacialExpression(int expression, int duration) {
        c.getSession().writeAndFlush(UIPacket.getDirectionFacialExpression(expression, duration));
    }

    public void playMovie(String data) {
        playMovie(data, true);
    }

    @Override
    public void playMovie(String data, boolean show) {
        super.playMovie(data, show);
        lastMsg = ScriptMessageType.SM_PLAYMOVIECLIP;
    }

    @Override
    public void playMovieURL(String data) {
        super.playMovieURL(data);
        lastMsg = ScriptMessageType.SM_PLAYMOVIECLIP_URL;
    }

    public void updateBuddyCapacity(int capacity) {
        c.getPlayer().setBuddyCapacity((byte) capacity);
    }

    public int getBuddyCapacity() {
        return c.getPlayer().getBuddyCapacity();
    }

    public int partyMembersInMap() {
        int inMap = 0;
        if (getPlayer().getParty() == null) {
            return inMap;
        }
        for (MapleCharacter char2 : getPlayer().getMap().getCharactersThreadsafe()) {
            if (char2.getParty() != null && char2.getParty().getId() == getPlayer().getParty().getId()) {
                inMap++;
            }
        }
        return inMap;
    }

    public List<MapleCharacter> getPartyMembers() {
        if (getPlayer().getParty() == null) {
            return null;
        }
        List<MapleCharacter> chars = new LinkedList<>(); // creates an empty array full of shit..
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null) { // double check <3
                    chars.add(ch);
                }
            }
        }
        return chars;
    }

    public void warpPartyWithExp(int mapId, int exp) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null)
                    || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
            }
        }
    }

    public void warpPartyWithExpMeso(int mapId, int exp, int meso) {
        if (getPlayer().getParty() == null) {
            warp(mapId, 0);
            gainExp(exp);
            gainMeso(meso);
            return;
        }
        MapleMap target = getMap(mapId);
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = c.getChannelServer().getPlayerStorage().getCharacterByName(chr.getName());
            if ((curChar.getEventInstance() == null && getPlayer().getEventInstance() == null)
                    || curChar.getEventInstance() == getPlayer().getEventInstance()) {
                curChar.changeMap(target, target.getPortal(0));
                curChar.gainExp(exp, true, false, true);
                curChar.gainMeso(meso, true);
            }
        }
    }

    public MapleSquad getSquad(String type) {
        return c.getChannelServer().getMapleSquad(type);
    }

    public int getSquadAvailability(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        }
        return squad.getStatus();
    }

    public boolean registerExpedition(String type, int minutes, String startText) {
        if (c.getChannelServer().getMapleSquad(type) == null) {
            final MapleSquad squad = new MapleSquad(c.getChannel(), type, c.getPlayer(), minutes * 60 * 1000,
                    startText);
            final boolean ret = c.getChannelServer().addMapleSquad(squad, type);
            if (ret) {
                final MapleMap map = c.getPlayer().getMap();
                map.broadcastMessage(CField.getClock(minutes * 60));
                map.broadcastMessage(CWvsContext.broadcastMsg(-6, startText));
            } else {
                squad.clear();
            }
            return ret;
        }
        return false;
    }

    public boolean registerSquad(String type, int minutes, String startText) {
        if (c.getChannelServer().getMapleSquad(type) == null) {
            final MapleSquad squad = new MapleSquad(c.getChannel(), type, c.getPlayer(), minutes * 60 * 1000,
                    startText);
            final boolean ret = c.getChannelServer().addMapleSquad(squad, type);
            if (ret) {
                final MapleMap map = c.getPlayer().getMap();
                map.broadcastMessage(CField.getClock(minutes * 60));
                map.broadcastMessage(CWvsContext.broadcastMsg(6, c.getPlayer().getName() + startText));
            } else {
                squad.clear();
            }
            return ret;
        }
        return false;
    }

    public boolean getSquadList(String type, byte type_) {
        try {
            final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
            if (squad == null) {
                return false;
            }
            switch (type_) {
                case 0:
                case 3:
                    // Normal viewing
                    sendNext(squad.getSquadMemberString(type_));
                    break;
                case 1:
                    // Squad Leader banning, Check out banned participant
                    sendSimple(squad.getSquadMemberString(type_));
                    break;
                case 2:
                    if (squad.getBannedMemberSize() > 0) {
                        sendSimple(squad.getSquadMemberString(type_));
                    } else {
                        sendNext(squad.getSquadMemberString(type_));
                    }
                    break;
                default:
                    break;
            }
            return true;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            return false;
        }
    }

    public byte isSquadLeader(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else {
            if (squad.getLeader() != null && squad.getLeader().getId() == c.getPlayer().getId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public boolean reAdd(String eim, String squad) {
        EventInstanceManager eimz = getDisconnected(eim);
        MapleSquad squadz = getSquad(squad);
        if (eimz != null && squadz != null) {
            squadz.reAddMember(getPlayer());
            eimz.registerPlayer(getPlayer());
            return true;
        }
        return false;
    }

    public void banMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.banMember(pos);
        }
    }

    public void acceptMember(String type, int pos) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad != null) {
            squad.acceptMember(pos);
        }
    }

    public int addMember(String type, boolean join) {
        try {
            final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
            if (squad != null) {
                return squad.addMember(c.getPlayer(), join);
            }
            return -1;
        } catch (NullPointerException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, ex);
            return -1;
        }
    }

    public byte isSquadMember(String type) {
        final MapleSquad squad = c.getChannelServer().getMapleSquad(type);
        if (squad == null) {
            return -1;
        } else {
            if (squad.getMembers().contains(c.getPlayer())) {
                return 1;
            } else if (squad.isBanned(c.getPlayer())) {
                return 2;
            } else {
                return 0;
            }
        }
    }

    public void resetReactors() {
        getPlayer().getMap().resetReactors();
    }

    public void genericGuildMessage(int code) {
        c.getSession().writeAndFlush(GuildPacket.genericGuildMessage(GuildOpcode.getOpcode(code)));
    }

    public void disbandGuild() {
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0 || c.getPlayer().getGuildRank() != 1) {
            return;
        }
        World.Guild.disbandGuild(gid);
    }

    public void increaseGuildCapacity(boolean trueMax) {
        if (c.getPlayer().getMeso() < 500000 && !trueMax) {
            c.getSession().writeAndFlush(CWvsContext.broadcastMsg(1, "You do not have enough mesos."));
            return;
        }
        final int gid = c.getPlayer().getGuildId();
        if (gid <= 0) {
            return;
        }
        if (World.Guild.increaseGuildCapacity(gid, trueMax)) {
            if (!trueMax) {
                c.getPlayer().gainMeso(-500000, true, true);
            } else {
                gainGP(-25000);
            }
            sendNext("Your guild capacity has been raised...");
        } else if (!trueMax) {
            sendNext("Please check if your guild capacity is full. (Limit: 100)");
        } else {
            sendNext(
                    "Please check if your guild capacity is full, if you have the GP needed or if subtracting GP would decrease a guild level. (Limit: 200)");
        }
    }

    public void displayGuildRanks() {
        c.getSession().writeAndFlush(GuildPacket.showGuildRanks(id, MapleGuildRanking.getInstance().getRank()));
    }

    public boolean removePlayerFromInstance() {
        if (c.getPlayer().getEventInstance() != null) {
            c.getPlayer().getEventInstance().removePlayer(c.getPlayer());
            return true;
        }
        return false;
    }

    public boolean isPlayerInstance() {
        return c.getPlayer().getEventInstance() != null;
    }

    public void makeTaintedEquip(byte slot) {
        Equip sel = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        sel.setStr((short) 69);
        sel.setDex((short) 69);
        sel.setInt((short) 69);
        sel.setLuk((short) 69);
        sel.setHp((short) 69);
        sel.setMp((short) 69);
        sel.setWatk((short) 69);
        sel.setMatk((short) 69);
        sel.setWdef((short) 69);
        sel.setMdef((short) 69);
        sel.setAcc((short) 69);
        sel.setAvoid((short) 69);
        sel.setHands((short) 69);
        sel.setSpeed((short) 69);
        sel.setJump((short) 69);
        sel.setUpgradeSlots((byte) 69);
        sel.setViciousHammer((byte) 69);
        sel.setEnhance((byte) 69);
        c.getPlayer().equipChanged();
        c.getPlayer().fakeRelog();
    }

    public void changeStat(byte slot, int type, int amount) {
        Equip sel = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        switch (type) {
            case 0:
                sel.setStr((short) amount);
                break;
            case 1:
                sel.setDex((short) amount);
                break;
            case 2:
                sel.setInt((short) amount);
                break;
            case 3:
                sel.setLuk((short) amount);
                break;
            case 4:
                sel.setHp((short) amount);
                break;
            case 5:
                sel.setMp((short) amount);
                break;
            case 6:
                sel.setWatk((short) amount);
                break;
            case 7:
                sel.setMatk((short) amount);
                break;
            case 8:
                sel.setWdef((short) amount);
                break;
            case 9:
                sel.setMdef((short) amount);
                break;
            case 10:
                sel.setAcc((short) amount);
                break;
            case 11:
                sel.setAvoid((short) amount);
                break;
            case 12:
                sel.setHands((short) amount);
                break;
            case 13:
                sel.setSpeed((short) amount);
                break;
            case 14:
                sel.setJump((short) amount);
                break;
            case 15:
                sel.setUpgradeSlots((byte) amount);
                break;
            case 16:
                sel.setViciousHammer((byte) amount);
                break;
            case 17:
                sel.setLevel((byte) amount);
                break;
            case 18:
                sel.setEnhance((byte) amount);
                break;
            case 19:
                sel.setPotential(amount, 1, false);
                sel.updateState(false);
                break;
            case 20:
                sel.setPotential(amount, 2, false);
                sel.updateState(false);
                break;
            case 21:
                sel.setPotential(amount, 3, false);
                sel.updateState(false);
                break;
            case 22:
                sel.setPotential(amount, 1, true);
                sel.updateState(true);
                break;
            case 23:
                sel.setPotential(amount, 2, true);
                sel.updateState(true);
                break;
            case 24:
                sel.setOwner(getText());
                break;
            default:
                break;
        }
        c.getPlayer().equipChanged();
        c.getPlayer().fakeRelog();
    }

    public void openPackageDeliverer() {
        c.getPlayer().setConversation(2);
        c.getSession().writeAndFlush(CField.sendPackageMSG((byte) 9, null));
    }

    public void openMerchantItemStore() {
        c.getPlayer().setConversation(3);
        HiredMerchantHandler.displayMerch(c);
        c.getSession().writeAndFlush(CWvsContext.enableActions());
    }

    public void sendPVPWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(0x32));
        c.getSession().writeAndFlush(CField.sendPVPMaps());
    }

    public void sendAzwanWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(0x46));
    }

    public void sendOpenJobChangeUI() {
        c.getSession().writeAndFlush(UIPacket.openUI(0xA4)); // job selections change depending on ur job
    }

    public void sendTimeGateWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(0xA8));
    }

    public void sendRepairWindow() {
        c.getSession().writeAndFlush(UIPacket.openUIOption(0x21, id));
    }

    public void sendJewelCraftWindow() {
        c.getSession().writeAndFlush(UIPacket.openUIOption(0x68, id));
    }

    public void sendVMatrixWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(0x46B));
    }

    public void sendRedLeaf(boolean viewonly, boolean autocheck) {
        if (autocheck) {
            viewonly = c.getPlayer().getFriendShipToAdd() == 0;
        }
        c.getSession().writeAndFlush(UIPacket.sendRedLeaf(viewonly ? 0 : c.getPlayer().getFriendShipToAdd(), viewonly));
    }

    public void sendProfessionWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(42));
    }

    public void sendQuestWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(6));
    }

    public void sendBeastTamerGiftWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(194));
    }

    public void sendCassandrasCollectionWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(127));
    }

    public void sendLuckyLuckyMonstoryWindow() {
        c.getSession().writeAndFlush(UIPacket.openUI(120));
    }

    public void OpenUI(int ui) {
        c.getPlayer().getMap().broadcastMessage(UIPacket.openUI(ui));
    }

    public void getMulungRanking() {
        c.getSession().writeAndFlush(CWvsContext.getMulungRanking());
    }

    public final int getDojoPoints() {
        return dojo_getPts();
    }

    public final int getDojoRecord() {
        return c.getPlayer().getIntNoRecord(GameConstants.DOJO_RECORD);
    }

    public void reloadAllFamiliars() {
        this.getPlayer().reloadAllFamiliars();
    }

    public void setDojoRecord(final boolean reset, final boolean take, int amount) {
        if (reset) {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD)).setCustomData("0");
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData("0");
        } else if (take) {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD))
                    .setCustomData(String.valueOf(c.getPlayer().getIntRecord(GameConstants.DOJO_RECORD) - amount));
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO))
                    .setCustomData(String.valueOf(c.getPlayer().getIntRecord(GameConstants.DOJO_RECORD) - amount));
        } else {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO_RECORD))
                    .setCustomData(String.valueOf(c.getPlayer().getIntRecord(GameConstants.DOJO_RECORD) + 1));
        }
    }

    public boolean start_DojoAgent(final boolean dojo, final boolean party, final int mapid) {
        if (dojo) {
            return MulungSystem.warpStartDojo(c.getPlayer(), party, getMap(mapid));
        }
        return MulungSystem.warpStartAgent(c.getPlayer(), party);
    }

    public boolean start_PyramidSubway(final int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpStartPyramid(c.getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpStartSubway(c.getPlayer());
    }

    public boolean bonus_PyramidSubway(final int pyramid) {
        if (pyramid >= 0) {
            return Event_PyramidSubway.warpBonusPyramid(c.getPlayer(), pyramid);
        }
        return Event_PyramidSubway.warpBonusSubway(c.getPlayer());
    }

    public final short getKegs() {
        return c.getChannelServer().getFireWorks().getKegsPercentage();
    }

    public void giveKegs(final int kegs) {
        c.getChannelServer().getFireWorks().giveKegs(c.getPlayer(), kegs);
    }

    public final short getSunshines() {
        return c.getChannelServer().getFireWorks().getSunsPercentage();
    }

    public void addSunshines(final int kegs) {
        c.getChannelServer().getFireWorks().giveSuns(c.getPlayer(), kegs);
    }

    public final short getDecorations() {
        return c.getChannelServer().getFireWorks().getDecsPercentage();
    }

    public void addDecorations(final int kegs) {
        try {
            c.getChannelServer().getFireWorks().giveDecs(c.getPlayer(), kegs);
        } catch (Exception e) {
        }
    }

    public final MapleCarnivalParty getCarnivalParty() {
        return c.getPlayer().getCarnivalParty();
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return c.getPlayer().getNextCarnivalRequest();
    }

    public final MapleCarnivalChallenge getCarnivalChallenge(MapleCharacter chr) {
        return new MapleCarnivalChallenge(chr);
    }

    public int setAndroid(int args) {
        if (args < 100) {
            c.getPlayer().getAndroid().setSkin(args);
            c.getPlayer().getAndroid().saveToDb();
        } else if (args < 30000) {
            if (faceExists(args)) {
                c.getPlayer().getAndroid().setFace(args);
                c.getPlayer().getAndroid().saveToDb();
            }
        } else {
            if (hairExists(args)) {
                c.getPlayer().getAndroid().setHair(args);
                c.getPlayer().getAndroid().saveToDb();
            }
        }
        CField.updateAndroidLook(c.getPlayer(), c.getPlayer().getAndroid());
        c.getPlayer().setAndroid(c.getPlayer().getAndroid()); // Respawn it
        c.getPlayer().equipChanged();
        return 1;
    }

    public int getAndroidStat(final String type) {
        switch (type) {
            case "HAIR":
                return c.getPlayer().getAndroid().getHair();
            case "FACE":
                return c.getPlayer().getAndroid().getFace();
            case "GENDER":
                return c.getPlayer().getAndroid().getGender();
        }
        return -1;
    }

    public void reloadChar() {
        getPlayer().getClient().getSession().writeAndFlush(CField.onSetField(getPlayer()));
        getPlayer().getMap().removePlayer(getPlayer());
        getPlayer().getMap().addPlayer(getPlayer());
    }

    public void askAndroid(String text, int[] args) {
        askAndroid(text, args, 0);
    }

    public void askAndroid(String text, int[] args, int card) {
        c.getSession().writeAndFlush(CField.CScriptMan.getAndroidTalkStyle(id, text, args, card));
        lastMsg = ScriptMessageType.SM_ASKANDROID;
    }

    @Override
    public MapleCharacter getChar() {
        return getPlayer();
    }

    public static int editEquipById(MapleCharacter chr, int max, int itemid, String stat, int newval) {
        return editEquipById(chr, max, itemid, stat, (short) newval);
    }

    public static int editEquipById(MapleCharacter chr, int max, int itemid, String stat, short newval) {
        // Is it an equip?
        if (!MapleItemInformationProvider.getInstance().isEquip(itemid)) {
            return -1;
        }

        // Get List
        List<Item> equips = chr.getInventory(MapleInventoryType.EQUIP).listById(itemid);
        List<Item> equipped = chr.getInventory(MapleInventoryType.EQUIPPED).listById(itemid);

        // Do you have any?
        if (equips.isEmpty() && equipped.isEmpty()) {
            return 0;
        }

        int edited = 0;

        // edit items
        for (Item itm : equips) {
            Equip e = (Equip) itm;
            if (edited >= max) {
                break;
            }
            edited++;
            switch (stat) {
                case "str":
                    e.setStr(newval);
                    break;
                case "dex":
                    e.setDex(newval);
                    break;
                case "int":
                    e.setInt(newval);
                    break;
                case "luk":
                    e.setLuk(newval);
                    break;
                case "watk":
                    e.setWatk(newval);
                    break;
                case "matk":
                    e.setMatk(newval);
                    break;
                default:
                    return -2;
            }
        }
        for (Item itm : equipped) {
            Equip e = (Equip) itm;
            if (edited >= max) {
                break;
            }
            edited++;
            switch (stat) {
                case "str":
                    e.setStr(newval);
                    break;
                case "dex":
                    e.setDex(newval);
                    break;
                case "int":
                    e.setInt(newval);
                    break;
                case "luk":
                    e.setLuk(newval);
                    break;
                case "watk":
                    e.setWatk(newval);
                    break;
                case "matk":
                    e.setMatk(newval);
                    break;
                default:
                    return -2;
            }
        }

        // Return items edited
        return (edited);
    }

    public Triple<String, Map<Integer, String>, Long> getSpeedRun(String typ) {
        final ExpeditionType expedtype = ExpeditionType.valueOf(typ);
        if (SpeedRunner.getSpeedRunData(expedtype) != null) {
            return SpeedRunner.getSpeedRunData(expedtype);
        }
        return new Triple<String, Map<Integer, String>, Long>("", new HashMap<Integer, String>(), 0L);
    }

    public boolean getSR(Triple<String, Map<Integer, String>, Long> ma, int sel) {
        if (ma.mid.get(sel) == null || ma.mid.get(sel).length() <= 0) {
            dispose();
            return false;
        }
        sendOk(ma.mid.get(sel));
        return true;
    }

    public Equip getEquip(int itemid) {
        return (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
    }

    public void setExpiration(Object statsSel, long expire) {
        if (statsSel instanceof Equip) {
            ((Equip) statsSel).setExpiration(System.currentTimeMillis() + (expire * 24 * 60 * 60 * 1000));
        }
    }

    public void setLock(Object statsSel) {
        if (statsSel instanceof Equip) {
            Equip eq = (Equip) statsSel;
            if (eq.getExpiration() == -1) {
                eq.setFlag(eq.getFlag() | ItemFlag.LOCK.getValue());
            } else {
                eq.setFlag(eq.getFlag() | ItemFlag.UNTRADABLE.getValue());
            }
        }
    }

    public boolean addFromDrop(Object statsSel) {
        if (statsSel instanceof Item) {
            final Item it = (Item) statsSel;
            return MapleInventoryManipulator.checkSpace(getClient(), it.getItemId(), it.getQuantity(), it.getOwner())
                    && MapleInventoryManipulator.addFromDrop(getClient(), it);
        }
        return false;
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type) {
        return replaceItem(slot, invType, statsSel, offset, type, false);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int offset, String type, boolean takeSlot) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        Item item = getPlayer().getInventory(inv).getItem((byte) slot);
        if (item == null || statsSel instanceof Item) {
            item = (Item) statsSel;
        }
        if (offset > 0) {
            if (inv != MapleInventoryType.EQUIP) {
                return false;
            }
            Equip eq = (Equip) item;
            if (takeSlot) {
                if (eq.getUpgradeSlots() < 1) {
                    return false;
                } else {
                    eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() - 1));
                }
                if (eq.getExpiration() == -1) {
                    eq.setFlag(eq.getFlag() | ItemFlag.LOCK.getValue());
                } else {
                    eq.setFlag(eq.getFlag() | ItemFlag.UNTRADABLE.getValue());
                }
            }
            if (type.equalsIgnoreCase("Slots")) {
                eq.setUpgradeSlots((byte) (eq.getUpgradeSlots() + offset));
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("Level")) {
                eq.setLevel((byte) (eq.getLevel() + offset));
            } else if (type.equalsIgnoreCase("Hammer")) {
                eq.setViciousHammer((byte) (eq.getViciousHammer() + offset));
            } else if (type.equalsIgnoreCase("STR")) {
                eq.setStr((short) (eq.getStr(0) + offset));
            } else if (type.equalsIgnoreCase("DEX")) {
                eq.setDex((short) (eq.getDex(0) + offset));
            } else if (type.equalsIgnoreCase("INT")) {
                eq.setInt((short) (eq.getInt(0) + offset));
            } else if (type.equalsIgnoreCase("LUK")) {
                eq.setLuk((short) (eq.getLuk(0) + offset));
            } else if (type.equalsIgnoreCase("HP")) {
                eq.setHp((short) (eq.getHp(0) + offset));
            } else if (type.equalsIgnoreCase("MP")) {
                eq.setMp((short) (eq.getMp(0) + offset));
            } else if (type.equalsIgnoreCase("WATK")) {
                eq.setWatk((short) (eq.getWatk(0) + offset));
            } else if (type.equalsIgnoreCase("MATK")) {
                eq.setMatk((short) (eq.getMatk(0) + offset));
            } else if (type.equalsIgnoreCase("WDEF")) {
                eq.setWdef((short) (eq.getWdef(0) + offset));
            } else if (type.equalsIgnoreCase("Hands")) {
                eq.setHands((short) (eq.getHands(0) + offset));
            } else if (type.equalsIgnoreCase("Speed")) {
                eq.setSpeed((short) (eq.getSpeed(0) + offset));
            } else if (type.equalsIgnoreCase("Jump")) {
                eq.setJump((short) (eq.getJump(0) + offset));
            } else if (type.equalsIgnoreCase("ItemEXP")) {
                eq.setItemEXP(eq.getItemEXP() + offset);
            } else if (type.equalsIgnoreCase("Expiration")) {
                eq.setExpiration(eq.getExpiration() + offset);
            } else if (type.equalsIgnoreCase("Flag")) {
                eq.setFlag(eq.getFlag() + offset);
            }
            item = eq.copy();
        }
        MapleInventoryManipulator.removeFromSlot(getClient(), inv, (short) slot, item.getQuantity(), false);
        return MapleInventoryManipulator.addFromDrop(getClient(), item);
    }

    public boolean replaceItem(int slot, int invType, Object statsSel, int upgradeSlots) {
        return replaceItem(slot, invType, statsSel, upgradeSlots, "Slots");
    }

    public boolean isCash(final int itemId) {
        return MapleItemInformationProvider.getInstance().isCash(itemId);
    }

    public int getTotalStat(final int itemId) {
        return MapleItemInformationProvider.getInstance()
                .getTotalStat((Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId));
    }

    public int getReqLevel(final int itemId) {
        return MapleItemInformationProvider.getInstance().getReqLevel(itemId);
    }

    public MapleStatEffect getEffect(int buff) {
        return MapleItemInformationProvider.getInstance().getItemEffect(buff);
    }

    public void buffGuild(final int buff, final int duration, final String msg) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.getItemEffect(buff) != null && getPlayer().getGuildId() > 0) {
            final MapleStatEffect mse = ii.getItemEffect(buff);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                    if (chr.getGuildId() == getPlayer().getGuildId()) {
                        mse.applyTo(chr, chr, true, null, duration);
                        chr.dropMessage(5, "Your guild has gotten a " + msg + " buff.");
                    }
                }
            }
        }
    }

    public boolean createAlliance(String alliancename) {
        MapleParty pt = c.getPlayer().getParty();
        MapleCharacter otherChar = c.getChannelServer().getPlayerStorage()
                .getCharacterById(pt.getMemberByIndex(1).getId());
        if (otherChar == null || otherChar.getId() == c.getPlayer().getId()) {
            return false;
        }
        try {
            return World.Alliance.createAlliance(alliancename, c.getPlayer().getId(), otherChar.getId(),
                    c.getPlayer().getGuildId(), otherChar.getGuildId());
        } catch (Exception re) {
            return false;
        }
    }

    public boolean addCapacityToAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId()
                        && World.Alliance.changeAllianceCapacity(gs.getAllianceId())) {
                    gainMeso(-MapleGuildAlliance.CHANGE_CAPACITY_COST);
                    return true;
                }
            }
        } catch (Exception re) {
        }
        return false;
    }

    public boolean disbandAlliance() {
        try {
            final MapleGuild gs = World.Guild.getGuild(c.getPlayer().getGuildId());
            if (gs != null && c.getPlayer().getGuildRank() == 1 && c.getPlayer().getAllianceRank() == 1) {
                if (World.Alliance.getAllianceLeader(gs.getAllianceId()) == c.getPlayer().getId()
                        && World.Alliance.disbandAlliance(gs.getAllianceId())) {
                    return true;
                }
            }
        } catch (Exception re) {
        }
        return false;
    }

    public ScriptMessageType getLastMsg() {
        return lastMsg;
    }

    public final void setLastMsg(final ScriptMessageType last) {
        this.lastMsg = last;
    }

    public final void maxAllSkills() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (SkillConstants.isApplicableSkill(skil.getId()) && skil.getId() < 90000000) { // no
                // db/additionals/resistance
                // skills
                sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(),
                        SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        getPlayer().changeSkillsLevel(sa);
    }

    public final void maxSkillsByJob() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (SkillConstants.isApplicableSkill(skil.getId()) && skil.canBeLearnedBy(getPlayer().getJob())
                    && !skil.isInvisible()) { // no db/additionals/resistance skills
                sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(),
                        SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        getPlayer().changeSkillsLevel(sa);
    }

    public final void removeSkillsByJob() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (SkillConstants.isApplicableSkill(skil.getId()) && skil.canBeLearnedBy(getPlayer().getJob())) { // no
                // db/additionals/resistance
                // skills
                sa.put(skil,
                        new SkillEntry((byte) -1, (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        getPlayer().changeSkillsLevel(sa);
    }

    public final void maxSkillsByJobId(int jobid) {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (SkillConstants.isApplicableSkill(skil.getId()) && skil.canBeLearnedBy(getPlayer().getJob())
                    && skil.getId() >= jobid * 1000000 && skil.getId() < (jobid + 1) * 1000000 && !skil.isInvisible()) {
                sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(),
                        SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        getPlayer().changeSkillsLevel(sa);
    }

    public final void resetStats(int str, int dex, int z, int luk) {
        c.getPlayer().resetStats(str, dex, z, luk);
    }

    public void killAllMonsters(int mapid) {
        MapleMap map = c.getChannelServer().getMapFactory().getMap(mapid);
        map.killAllMonsters(false); // No drop.
    }

    public final void killAllMobs() {
        c.getPlayer().getMap().killAllMonsters(true);
    }

    public final void levelUp() {
        c.getPlayer().levelUp();
    }

    public void cleardrops() {
        MapleMonsterInformationProvider.getInstance().clearDrops();
    }

    public final boolean dropItem(int slot, int invType, int quantity) {
        MapleInventoryType inv = MapleInventoryType.getByType((byte) invType);
        if (inv == null) {
            return false;
        }
        return MapleInventoryManipulator.drop(c, inv, (short) slot, (short) quantity, true);
    }

    public final List<Integer> getAllPotentialInfo() {
        List<Integer> list = new ArrayList<>(MapleItemInformationProvider.getInstance().getAllPotentialInfo().keySet());
        Collections.sort(list);
        return list;
    }

    public final List<Integer> getAllPotentialInfoSearch(String content) {
        List<Integer> list = new ArrayList<>();
        for (Entry<Integer, List<StructItemOption>> i : MapleItemInformationProvider.getInstance().getAllPotentialInfo()
                .entrySet()) {
            for (StructItemOption ii : i.getValue()) {
                if (ii.toString().contains(content)) {
                    list.add(i.getKey());
                }
            }
        }
        Collections.sort(list);
        return list;
    }

    public void MakeGMItem(byte slot, MapleCharacter player) {
        MapleInventory equip = player.getInventory(MapleInventoryType.EQUIP);
        Equip eu = (Equip) equip.getItem(slot);
        int item = equip.getItem(slot).getItemId();
        short hand = eu.getHands(0);
        byte level = eu.getLevel();
        Equip nItem = new Equip(item, slot, (byte) 0);
        nItem.setStr((short) 32767); // STR
        nItem.setDex((short) 32767); // DEX
        nItem.setInt((short) 32767); // INT
        nItem.setLuk((short) 32767); // LUK
        nItem.setUpgradeSlots((byte) 0);
        nItem.setHands(hand);
        nItem.setLevel(level);
        player.getInventory(MapleInventoryType.EQUIP).removeItem(slot);
        player.getInventory(MapleInventoryType.EQUIP).addFromDB(nItem);
    }

    public final String getPotentialInfo(final int id) {
        final List<StructItemOption> potInfo = MapleItemInformationProvider.getInstance().getPotentialInfo(id);
        final StringBuilder builder = new StringBuilder("#b#ePOTENTIAL INFO FOR ID: ");
        builder.append(id);
        builder.append("#n#k\r\n\r\n");
        int minLevel = 1, maxLevel = 10;
        for (StructItemOption item : potInfo) {
            builder.append("#eLevels ");
            builder.append(minLevel);
            builder.append("~");
            builder.append(maxLevel);
            builder.append(": #n");
            builder.append(item.get(potInfo.toString()));
            minLevel += 10;
            maxLevel += 10;
            builder.append("\r\n");
        }
        return builder.toString();
    }

    public final void sendRPS() {
        c.getSession().writeAndFlush(CField.getRPSMode((byte) 8, -1, -1, -1));
    }

    public final void setQuestRecord(Object ch, final int questid, final String data) {
        ((MapleCharacter) ch).getQuestNAdd(MapleQuest.getInstance(questid)).setCustomData(data);
    }

    public final void doWeddingEffect(final Object ch) {
        final MapleCharacter chr = (MapleCharacter) ch;
        final MapleCharacter player = getPlayer();
        getMap().broadcastMessage(CWvsContext.yellowChat(player.getName() + ", do you take " + chr.getName()
                + " as your wife and promise to stay beside her through all downtimes, crashes, and lags?"));
        CloneTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (chr == null || player == null) {
                    warpMap(680000500, 0);
                } else {
                    chr.getMap().broadcastMessage(CWvsContext.yellowChat(chr.getName() + ", do you take "
                            + player.getName()
                            + " as your husband and promise to stay beside him through all downtimes, crashes, and lags?"));
                }
            }
        }, 10000);
        CloneTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (chr == null || player == null) {
                    if (player != null) {
                        setQuestRecord(player, 160001, "3");
                        setQuestRecord(player, 160002, "0");
                    } else if (chr != null) {
                        setQuestRecord(chr, 160001, "3");
                        setQuestRecord(chr, 160002, "0");
                    }
                    warpMap(680000500, 0);
                } else {
                    setQuestRecord(player, 160001, "2");
                    setQuestRecord(chr, 160001, "2");
                    sendNPCText(
                            player.getName() + " and " + chr.getName() + ", I wish you two all the best on your "
                            + chr.getClient().getChannelServer().getServerName() + " journey together!",
                            9201002);
                    chr.getMap().startExtendedMapEffect("You may now kiss the bride, " + player.getName() + "!",
                            5120006);
                    if (chr.getGuildId() > 0) {
                        World.Guild.guildPacket(chr.getGuildId(), CWvsContext.sendMarriage(false, chr.getName()));
                    }
                    if (player.getGuildId() > 0) {
                        World.Guild.guildPacket(player.getGuildId(), CWvsContext.sendMarriage(false, player.getName()));
                    }
                }
            }
        }, 20000); // 10 sec 10 sec

    }

    public void putKey(int key, int type, int action) {
        getPlayer().changeKeybinding(key, (byte) type, action);
        getClient().getSession().writeAndFlush(CField.getKeymap(getPlayer().getKeyLayout(), getPlayer()));
    }

    public void doRing(final String name, final int itemid) {
        PlayersHandler.DoRing(getClient(), name, itemid);
    }

    public int getNaturalStats(final int itemid, final String it) {
        Map<String, Integer> eqStats = MapleItemInformationProvider.getInstance().getEquipStats(itemid);
        if (eqStats != null && eqStats.containsKey(it)) {
            return eqStats.get(it);
        }
        return 0;
    }

    public boolean isEligibleName(String t) {
        return MapleCharacterUtil.canCreateChar(t, getPlayer().isIntern())
                && (!LoginInformationProvider.getInstance().isForbiddenName(t) || getPlayer().isIntern());
    }

    public String checkDrop(int mobId) {
        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(mobId);
        final List<MonsterGlobalDropEntry> drops = MapleMonsterInformationProvider.getInstance().getGlobalDrop();
        if ((ranks != null && ranks.size() > 0) || (drops != null && drops.size() > 0)) {
            int num = 0;
            int itemId;
            int ch;
            MonsterDropEntry de;
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < ranks.size(); i++) {
                de = ranks.get(i);
                if (de.chance > 0) {
                    itemId = de.itemId;
                    if (num == 0) {
                        name.append("怪物#e#o").append(mobId).append("##n的掉寶數據如下\r\n");
                        name.append("--------------------------------------\r\n");
                    }
                    String namez = "#z" + itemId + "#";
                    if (itemId == 0) { // meso
                        long minMeso = de.Minimum * getClient().getChannelServer().getMesoRate(getPlayer().getWorld());
                        long maxMeso = de.Maximum * getClient().getChannelServer().getMesoRate(getPlayer().getWorld());
                        namez = " " + (minMeso != maxMeso ? (minMeso + "~" + maxMeso) : minMeso);
                    }
                    ch = de.chance * getClient().getChannelServer().getDropRate(getPlayer().getWorld());
                    name.append(num + 1).append(") ");
                    if (itemId == 0) {
                        name.append("#fUI/UIWindow2.img/QuestIcon/7/0#");
                    } else {
                        name.append("#i").append(itemId).append(":#");
                    }
                    name.append(namez);

                    if (c.getPlayer().isIntern()) {
                        if (itemId != 0) {
                            name.append("(").append(itemId).append(")");
                        }
                        if (de.questid > 0) {
                            name.append("[")
                                    .append(MapleQuest.getInstance(de.questid).getName().length() > 0
                                            ? MapleQuest.getInstance(de.questid).toString()
                                            : ("需求任務:" + de.questid))
                                    .append("]");
                        }
                        name.append("\r\n掉寶幾率：")
                                .append(Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0)
                                .append("%").append(" 來源：").append(de.addFrom).append("\r\n");
                    }
                    name.append("\r\n");
                    num++;
                }
            }

            // 加載全域掉寶
            if (drops != null && drops.size() > 0) {
                MonsterGlobalDropEntry dge;
                for (int i = 0; i < drops.size(); i++) {
                    dge = drops.get(i);
                    if (dge.chance > 0) {
                        itemId = dge.itemId;
                        String namez = "#z" + itemId + "#";
                        if (itemId == 0) { // meso
                            long minMeso = dge.Minimum * c.getChannelServer().getMesoRate(c.getPlayer().getWorld());
                            long maxMeso = dge.Maximum * c.getChannelServer().getMesoRate(c.getPlayer().getWorld());
                            namez = " " + (minMeso != maxMeso ? (minMeso + "~" + maxMeso) : minMeso);
                        }
                        ch = dge.chance * c.getChannelServer().getDropRate(c.getPlayer().getWorld());
                        name.append(num + 1).append(") ");
                        if (itemId == 0) {
                            name.append("#fUI/UIWindow2.img/QuestIcon/7/0#");
                        } else {
                            name.append("#i").append(itemId).append(":#");
                        }
                        name.append(namez);
                        if (c.getPlayer().isIntern()) {
                            if (itemId != 0) {
                                name.append("(").append(itemId).append(")");
                            }
                            if (dge.questid > 0) {
                                name.append("[")
                                        .append(MapleQuest.getInstance(dge.questid).getName().length() > 0
                                                ? MapleQuest.getInstance(dge.questid).toString()
                                                : ("需求任務:" + dge.questid))
                                        .append("]");
                            }
                            name.append("\r\n掉寶幾率：")
                                    .append(Integer.valueOf(ch >= 999999 ? 1000000 : ch).doubleValue() / 10000.0)
                                    .append("%").append(" 來源：").append(dge.addFrom).append("(全域)\r\n");
                        }
                        name.append("\r\n");
                        num++;
                    }
                }
            }
            if (name.length() > 0) {
                return name.toString();
            }

        }
        return "此怪物無掉寶數據。";
    }

    public String getLeftPadded(final String in, final char padchar, final int length) {
        return StringUtil.getLeftPaddedStr(in, padchar, length);
    }

    public void handleDivorce() {
        if (getPlayer().getMarriageId() <= 0) {
            sendNext("Please make sure you have a marriage.");
            return;
        }
        final int chz = World.Find.findChannel(getPlayer().getMarriageId());
        if (chz == -1) {
            // sql queries
            try {
                Connection con = ManagerDatabasePool.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE queststatus SET customData = ? WHERE characterid = ? AND (quest = ? OR quest = ?)");
                ps.setString(1, "0");
                ps.setInt(2, getPlayer().getMarriageId());
                ps.setInt(3, 160001);
                ps.setInt(4, 160002);
                ps.executeUpdate();
                ps.close();

                ps = con.prepareStatement("UPDATE characters SET marriageid = ? WHERE id = ?");
                ps.setInt(1, 0);
                ps.setInt(2, getPlayer().getMarriageId());
                ps.executeUpdate();
                ps.close();
                ManagerDatabasePool.closeConnection(con);
            } catch (SQLException e) {
                outputFileError(e);
                return;
            }
            setQuestRecord(getPlayer(), 160001, "0");
            setQuestRecord(getPlayer(), 160002, "0");
            getPlayer().setMarriageId(0);
            sendNext("You have been successfully divorced...");
            return;
        } else if (chz < -1) {
            sendNext("Please make sure your partner is logged on.");
            return;
        }
        MapleCharacter cPlayer = ChannelServer.getInstance(chz).getPlayerStorage()
                .getCharacterById(getPlayer().getMarriageId());
        if (cPlayer != null) {
            cPlayer.dropMessage(1, "Your partner has divorced you.");
            cPlayer.setMarriageId(0);
            setQuestRecord(cPlayer, 160001, "0");
            setQuestRecord(getPlayer(), 160001, "0");
            setQuestRecord(cPlayer, 160002, "0");
            setQuestRecord(getPlayer(), 160002, "0");
            getPlayer().setMarriageId(0);
            sendNext("You have been successfully divorced...");
        } else {
            sendNext("An error occurred...");
        }
    }

    public String getReadableMillis(long startMillis, long endMillis) {
        return StringUtil.getReadableMillis(startMillis, endMillis);
    }

    public void sendUltimateExplorer() {
        getClient().getSession().writeAndFlush(CWvsContext.ultimateExplorer());
    }

    public void sendPendant(boolean b) {
        c.getSession().writeAndFlush(CWvsContext.pendantSlot(b));
    }

    public void changeJobById(short job) {
        c.getPlayer().changeJob(job);
    }

    public int getJobId() {
        return getPlayer().getJob();
    }

    public int getLevel() {
        return getPlayer().getLevel();
    }

    public int getEquipId(byte slot) {
        MapleInventory equip = getPlayer().getInventory(MapleInventoryType.EQUIP);
        Equip eu = (Equip) equip.getItem(slot);
        return equip.getItem(slot).getItemId();
    }

    public int getUseId(byte slot) {
        MapleInventory use = getPlayer().getInventory(MapleInventoryType.USE);
        return use.getItem(slot).getItemId();
    }

    public int getSetupId(byte slot) {
        MapleInventory setup = getPlayer().getInventory(MapleInventoryType.SETUP);
        return setup.getItem(slot).getItemId();
    }

    public int getCashId(byte slot) {
        MapleInventory cash = getPlayer().getInventory(MapleInventoryType.CASH);
        return cash.getItem(slot).getItemId();
    }

    public int getETCId(byte slot) {
        MapleInventory etc = getPlayer().getInventory(MapleInventoryType.ETC);
        return etc.getItem(slot).getItemId();
    }

    public String EquipList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new LinkedList<>();
        for (Item item : equip.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    public String UseList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory use = c.getPlayer().getInventory(MapleInventoryType.USE);
        List<String> stra = new LinkedList<>();
        for (Item item : use.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    public String CashList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory cash = c.getPlayer().getInventory(MapleInventoryType.CASH);
        List<String> stra = new LinkedList<>();
        for (Item item : cash.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    public String ETCList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory etc = c.getPlayer().getInventory(MapleInventoryType.ETC);
        List<String> stra = new LinkedList<>();
        for (Item item : etc.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    public String SetupList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory setup = c.getPlayer().getInventory(MapleInventoryType.SETUP);
        List<String> stra = new LinkedList<>();
        for (Item item : setup.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    public String PotentialedEquipList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new LinkedList<>();
        for (Item item : equip.list()) {
            Equip eq = (Equip) item;
            if (eq.getPotential(1, true) != 0) {
                stra.add("\r\n#L" + item.getPosition() + "##v" + item.getItemId() + "# - "
                        + (eq.getPotential(2, true) != 0 ? 2 : 1) + " additional potential lines #l");
            }
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }

    public String EquipPotentialList(short slot) {
        Equip equip = (Equip) getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
        StringBuilder sb = new StringBuilder();
        int[] potentials;
        potentials = new int[]{equip.getPotential(1, false), equip.getPotential(2, false),
            equip.getPotential(3, false)};
        for (int i : potentials) {
            StructItemOption op = MapleItemInformationProvider.getInstance()
                    .getPotentialInfo(equip.getPotential(1, false))
                    .get(MapleItemInformationProvider.getInstance().getReqLevel(equip.getItemId()) / 10);
            sb.append("\r\nPotential ").append(i).append(" - ").append(op.toString());
        }
        return sb.toString();
    }

    public void showFredrick() {
        HiredMerchantHandler.showFredrick(c);
    }

    public String searchId(int type, String search) {
        return MapleInventoryManipulator.searchId(type, search);
    }

    public int parseInt(String s) {
        return Integer.parseInt(s);
    }

    public byte parseByte(String s) {
        return Byte.parseByte(s);
    }

    public short parseShort(String s) {
        return Short.parseShort(s);
    }

    public long parseLong(String s) {
        return Long.parseLong(s);
    }

    public void getEventEnvelope(int questid, int time) {
        CWvsContext.getEventEnvelope(questid, time);
    }

    public void write(Object o) {
        c.getSession().writeAndFlush(o);
    }

    public void openUIOption(int type) {
        CField.UIPacket.openUIOption(type, id);
    }

    public void sendAttackOnTitanScore(int type) {
        c.getSession().writeAndFlush(CField.UIPacket.openUIOption(type, 1));
    }

    public List<Triple<Short, String, Integer>> rankList(short[] ranks, String[] names, int[] values) {
        List<Triple<Short, String, Integer>> list = new LinkedList<>();
        if (ranks.length != names.length || names.length != values.length || values.length != ranks.length) {
            return null;
        }
        for (int i = 0; i < ranks.length; i++) {
            list.add(new Triple<>(ranks[i], names[i], values[i]));
        }
        return list;
    }

    public void dragonShoutReward(int reward) {
        int itemid;
        switch (reward) {
            case 0:
                itemid = 1102207;
                break;
            case 1:
                itemid = 1122080;
                break;
            case 2:
                itemid = 2041213;
                break;
            case 3:
                itemid = 2022704;
                break;
            default:
                itemid = 2022704;
                break;
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleInventoryType invtype = GameConstants.getInventoryType(itemid);
        if (!MapleInventoryManipulator.checkSpace(c, itemid, 1, "")) {
            return;
        }
        if (invtype.equals(MapleInventoryType.EQUIP) && !ItemConstants.類型.可充值道具(itemid)) {
            final Equip item = (Equip) (ii.getEquipById(itemid));
            switch (reward) {
                case 0: // 9% ATT, 9% MAGIC, 30% Boss Damage
                    item.setPotential(40051, 1, false); // 9% Att
                    item.setPotential(40052, 2, false); // 9% Magic
                    item.setPotential(40601, 3, false); // 30% Boss Damage
                    item.updateState(false);
                    break;
                case 1: // 30% All Stat
                    item.setPotential(40086, 1, false); // 9% All Stat
                    item.setPotential(40086, 2, false); // 9% All Stat
                    item.setPotential(40086, 3, false); // 9% All Stat
                    item.setSocket(ii.getSocketInfo(3063280).opID, 1); // 3% All Stat
                    item.updateState(false);
                    break;
            }
            item.setOwner("Hyperious");
            item.setGMLog("從腳本 " + this.id + "(" + id2 + ")[" + script + "] (The Dragon's Shout PQ)中獲得 時間: "
                    + FileoutputUtil.CurrentReadable_Time());
            final String name = ii.getName(itemid);
            if (itemid / 10000 == 114 && name != null && name.length() > 0) { // medal
                final String msg = "<" + name + ">獲得稱號。";
                c.getPlayer().dropMessage(-1, msg);
                c.getPlayer().dropMessage(5, msg);
            }
            MapleInventoryManipulator.addbyItem(c, item.copy());
        } else {
            MapleInventoryManipulator.addById(c, itemid, (short) 1, "Hyperious", null, 0, MapleInventoryManipulator.DAY,
                    "從腳本 " + this.id + "(" + id2 + ")[" + script + "] 中獲得 時間: "
                    + FileoutputUtil.CurrentReadable_Time());
        }
        c.getSession().writeAndFlush(InfoPacket.getShowItemGain(itemid, (short) 1, true));
    }

    public String searchData(int type, String search) {
        return SearchGenerator.searchData(type, search);
    }

    public int[] getSearchData(int type, String search) {
        Map<Integer, String> data = SearchGenerator.getSearchData(type, search);
        if (data.isEmpty()) {
            return null;
        }
        int[] searches = new int[data.size()];
        int i = 0;
        for (int key : data.keySet()) {
            searches[i] = key;
            i++;
        }
        return searches;
    }

    public boolean foundData(int type, String search) {
        return SearchGenerator.foundData(type, search);
    }

    public boolean partyHaveItem(int itemid, short quantity) {
        if (getPlayer().getParty() == null) {
            return false;
        }
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            for (ChannelServer channel : ChannelServer.getAllInstances()) {
                MapleCharacter ch = channel.getPlayerStorage().getCharacterById(chr.getId());
                if (ch != null) {
                    if (!ch.haveItem(itemid, quantity)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public final boolean scrollItem(final short scroll, final short item) {
        return InventoryHandler.UseUpgradeScroll(scroll, item, (short) 0, getClient(), getPlayer(), 0, false, false);
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /*
     * public final int WEAPON_RENTAL = 57463816;
     * 
     * public int weaponRentalState() { if
     * (c.getPlayer().getIntNoRecord(WEAPON_RENTAL) == 0) { return 0; } return
     * (System.currentTimeMillis() / (60 * 1000) -
     * c.getPlayer().getIntNoRecord(WEAPON_RENTAL)) >= 15 ? 1 : 2; }
     * 
     * public void setWeaponRentalUnavailable() {
     * c.getPlayer().getQuestNAdd(MapleQuest.getInstance(WEAPON_RENTAL)).
     * setCustomData("" + System.currentTimeMillis() / (60 * 1000)); }
     */
    public MapleQuest getQuestById(int questId) {
        return MapleQuest.getInstance(questId);
    }

    public int getEquipLevelById(int itemId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        return ii.getEquipStats(itemId).get("reqLevel");
    }

    public void sendGMBoard(String url) {
        c.getSession().writeAndFlush(CWvsContext.gmBoard(c.getNextClientIncrenement(), url));
    }

    public void addPendantSlot(int days) {
        c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.墜飾欄))
                .setCustomData(String.valueOf(System.currentTimeMillis() + ((long) days * 24 * 60 * 60 * 1000)));
        c.getSession().writeAndFlush(
                CWvsContext.updatePendantSlot(System.currentTimeMillis() + ((long) days * 24 * 60 * 60 * 1000)));
    }

    public void setPendantSlot(long time) {
        c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.墜飾欄)).setCustomData(String.valueOf(time));
        c.getSession().writeAndFlush(CWvsContext.updatePendantSlot(time));
    }

    public long getCustomMeso() {
        return c.getPlayer().getLongNoRecord(GameConstants.CUSTOM_BANK);
    }

    public void setCustomMeso(long meso) {
        c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.CUSTOM_BANK)).setCustomData(meso + "");
    }

    public void enter_931060110() {
        try {
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                    "Effect/CharacterEff.img/farmEnterTuto/menuUI", new int[]{6000, 285, 186, 1, 0, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{900}));
            Thread.sleep(900);
            c.getSession().writeAndFlush(CWvsContext.getTopMsg("First, click MENU at the bottom of the screen."));
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/CharacterEff.img/farmEnterTuto/mouseMoveToMenu",
                            new int[]{1740, -114, -14, 1, 3, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1680}));
            Thread.sleep(1680);
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                    "Effect/CharacterEff.img/farmEnterTuto/mouseClick", new int[]{1440, 246, 196, 1, 3, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1440}));
            Thread.sleep(1440);
            c.getSession().writeAndFlush(CWvsContext.getTopMsg("Now, select Go to Farm."));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{600}));
            Thread.sleep(600);
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                    "Effect/CharacterEff.img/farmEnterTuto/menuOpen", new int[]{50000, 285, 186, 1, 2, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{600}));
            Thread.sleep(600);
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/CharacterEff.img/farmEnterTuto/mouseMoveToMyfarm",
                            new int[]{750, 246, 196, 1, 2, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{720}));
            Thread.sleep(720);
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/CharacterEff.img/farmEnterTuto/menuMouseOver",
                            new int[]{50000, 285, 186, 1, 2, 0, 0, 0}));
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                    "Effect/CharacterEff.img/farmEnterTuto/mouseClick", new int[]{50000, 246, 166, 1, 3, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1440}));
            Thread.sleep(1440);
        } catch (InterruptedException ex) {
        }
    }

    public void enter_931060120() {
        try {
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                    "Effect/CharacterEff.img/farmEnterTuto/character", new int[]{120000, -200, 0, 1, 1, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1200}));
            Thread.sleep(1200);
            c.getSession().writeAndFlush(CWvsContext.getTopMsg("Hover over any other character..."));
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/CharacterEff.img/farmEnterTuto/mouseMoveToChar",
                            new int[]{1680, -400, -210, 1, 3, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1650}));
            Thread.sleep(1650);
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                    "Effect/CharacterEff.img/farmEnterTuto/mouseUp", new int[]{600, -190, -30, 1, 3, 0, 0, 0}));
            c.getSession().writeAndFlush(CWvsContext.getTopMsg("Then right-click."));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{540}));
            Thread.sleep(540);
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                    "Effect/CharacterEff.img/farmEnterTuto/mouseClick", new int[]{1200, -190, -30, 1, 3, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1200}));
            Thread.sleep(1200);
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/CharacterEff.img/farmEnterTuto/characterMenu",
                            new int[]{50000, -200, 0, 1, 2, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{900}));
            Thread.sleep(900);
            c.getSession().writeAndFlush(CWvsContext.getTopMsg("When the Character Menu appears, click Go to Farm."));
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/CharacterEff.img/farmEnterTuto/mouseMoveToOtherfarm",
                            new int[]{1440, -190, -30, 1, 5, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1380}));
            Thread.sleep(1380);
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/CharacterEff.img/farmEnterTuto/menuMouseOver",
                            new int[]{50000, -200, 0, 1, 4, 0, 0, 0}));
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                    "Effect/CharacterEff.img/farmEnterTuto/mouseClick", new int[]{60000, -130, 150, 1, 6, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1200}));
            Thread.sleep(1200);
        } catch (InterruptedException ex) {
        }
    }

    public static String getMobImg(int mob) {
        MapleMonster monster = MapleLifeFactory.getMonster(mob);
        if (monster.getStats().getLink() != 0) {
            mob = monster.getStats().getLink();
        }
        String mobStr = String.valueOf(mob);
        while (mobStr.length() < 7) {
            String newStr = "0" + mobStr;
            mobStr = newStr;
        }
        return "#fMob/" + mobStr + ".img/stand/0#";
    }

    public void showBeastTamerTutScene() {
        c.getSession().writeAndFlush(CField.UIPacket.lockUI(false));
        NPCScriptManager.getInstance().dispose(c);
        c.removeClickedNPC();
        NPCScriptManager.getInstance().start(c, 9390305, "BeastTamerTut01");
    }

    public void showBeastTamerTutScene1() {
        try {
            c.getSession().writeAndFlush(UIPacket.getDirectionStatus(true));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1000}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1000}));
            Thread.sleep(2000);
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/Direction14.img/effect/ShamanBT/balloonMsg/10",
                            new int[]{2000, 0, -120, 1, 0, 0, 0, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{800}));
            Thread.sleep(800);
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_CameraMove, null, new int[]{0, 1000, 700, 0}));
            c.getSession().writeAndFlush(CField.UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1200}));
            Thread.sleep(1200);
            c.getSession().writeAndFlush(
                    UIPacket.getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay,
                            "Effect/Direction14.img/effect/ShamanBT/BalloonMsg1/7",
                            new int[]{2000, 571, -120, 1, 0, 0, 0, 0}));
            c.getSession().writeAndFlush(
                    CField.OnFieldEffect(new String[]{"ShamanBTTuto/sound0"}, FieldEffectOpcode.FieldEffect_Sound));
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{3000}));
            Thread.sleep(3000);
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{1000}));
            c.getSession().writeAndFlush(UIPacket.getDirectionEffect(
                    InGameDirectionEventOpcode.InGameDirectionEvent_Delay, null, new int[]{500}));
            Thread.sleep(1500);
            c.getSession().writeAndFlush(UIPacket.getDirectionFacialExpression(4, 5000));
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }
        NPCScriptManager.getInstance().dispose(c);
        c.removeClickedNPC();
        NPCScriptManager.getInstance().start(c, 9390305, "BeastTamerTut02");
    }

    public String getSkillMenu(int skillMaster, int job) {
        String menu = "";
        int bookMin = 3;
        int bookMax = 3;
        if (MapleJob.is神之子(job)) {
            bookMin = 0;
            bookMax = 1;
        } else if (MapleJob.is影武者(job)) {
            if (skillMaster < 20) {
                bookMin = 2;
                bookMax = 4;
            } else {
                bookMin = 5;
                bookMax = 5;
            }
        } else if (!GameConstants.isSeparatedSp(job)) {
            bookMin = 0;
            bookMax = 0;
        }
        for (Map.Entry<Skill, SkillEntry> ret : c.getPlayer().getSkills().entrySet()) {
            int skillBook = GameConstants.getSkillBookBySkill(ret.getKey().getId());
            int skillJob = ret.getKey().getId() / 100000;
            if (skillBook < bookMin || skillBook > bookMax || skillJob != job / 10) {
                continue;
            }
            if ((ret.getKey().getMaxLevel() > 10 || skillMaster < 20)
                    && ret.getValue().masterlevel < ret.getKey().getMaxLevel() && ret.getKey().getMasterLevel() > 0) {
                if (skillMaster > 20) {
                    if (ret.getValue().masterlevel >= 30 || ret.getValue().masterlevel < 20
                            || ret.getKey().getMaxLevel() <= 20) {
                        continue;
                    }
                } else if (skillMaster > 10) {
                    if (ret.getValue().masterlevel >= 20) {
                        continue;
                    }
                }
                menu += "\r\n#L" + ret.getKey().getId() + "# #s" + ret.getKey().getId() + "# #fn字體##fs14##e#q"
                        + ret.getKey().getId() + "##n#fs##fn##l";
            }
        }
        return menu;
    }

    public boolean canUseSkillBook(int skillId, int masterLevel) {
        if (masterLevel > 0) {
            int job = skillId / 10000;
            if (masterLevel == 10 && (MapleJob.中忍.getId() > job || MapleJob.隱忍.getId() < job)) {
                return false;
            }
            final Skill CurrSkillData = SkillFactory.getSkill(skillId);
            int skillLevel = c.getPlayer().getSkillLevel(CurrSkillData);
            if (skillLevel >= CurrSkillData.getMaxLevel()) {
                return false;
            }
            if ((skillLevel >= 5 && masterLevel <= 20) || (skillLevel >= 15 && masterLevel <= 30 && masterLevel > 20)) {
                return true;
            }
        }
        return false;
    }

    public void useSkillBook(int skillId, int masterLevel) {
        if (!canUseSkillBook(skillId, masterLevel)) {
            return;
        }
        final Skill CurrSkillData = SkillFactory.getSkill(skillId);
        if (masterLevel == 10) {
            masterLevel = CurrSkillData.getMaxLevel();
        }
        masterLevel = masterLevel > CurrSkillData.getMaxLevel() ? CurrSkillData.getMaxLevel() : masterLevel;
        c.getPlayer().changeSingleSkillLevel(CurrSkillData, c.getPlayer().getSkillLevel(CurrSkillData),
                (byte) masterLevel);
        c.getPlayer().getMap().broadcastMessage(CWvsContext.useSkillBook(c.getPlayer(), 0, 0, true, true));
        c.getSession().writeAndFlush(CWvsContext.enableActions());
    }

    public void getJobSelection(int job) {
        c.getSession().writeAndFlush(CScriptMan.getJobSelection(id, job));
        lastMsg = ScriptMessageType.SM_ASKSELECTMENU;
    }

    public void checkMedalQuest() {
        MapleQuest.MedalQuest m = null;
        for (MapleQuest.MedalQuest mq : MapleQuest.MedalQuest.values()) {
            for (int i : mq.maps) {
                if (c.getPlayer().getMapId() == i) {
                    m = mq;
                    break;
                }
            }
        }
        if (m != null && c.getPlayer().getLevel() >= m.level && c.getPlayer().getQuestStatus(m.questid) != 2) {
            if (c.getPlayer().getQuestStatus(m.lquestid) != 1) {
                MapleQuest.getInstance(m.lquestid).forceStart(c.getPlayer(), 0, "0");
            }
            if (c.getPlayer().getQuestStatus(m.questid) != 1) {
                MapleQuest.getInstance(m.questid).forceStart(c.getPlayer(), 0, null);
                final StringBuilder sb = new StringBuilder("enter=");
                for (int i = 0; i < m.maps.length; i++) {
                    sb.append("0");
                }
                c.getPlayer().updateInfoQuest(m.questid - 2005, sb.toString());
                MapleQuest.getInstance(m.questid - 1995).forceStart(c.getPlayer(), 0, "0");
            }
            String quest = c.getPlayer().getInfoQuest(m.questid - 2005);
            if (quest.length() != m.maps.length + 6) { // enter= is 6
                final StringBuilder sb = new StringBuilder("enter=");
                for (int i = 0; i < m.maps.length; i++) {
                    sb.append("0");
                }
                quest = sb.toString();
                c.getPlayer().updateInfoQuest(m.questid - 2005, quest);
            }
            final MapleQuestStatus stat = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(m.questid - 1995));
            if (stat.getCustomData() == null) { // just a check.
                stat.setCustomData("0");
            }
            int number = Integer.parseInt(stat.getCustomData());
            final StringBuilder sb = new StringBuilder("enter=");
            boolean changedd = false;
            for (int i = 0; i < m.maps.length; i++) {
                boolean changed = false;
                if (c.getPlayer().getMapId() == m.maps[i]) {
                    if (quest.substring(i + 6, i + 7).equals("0")) {
                        sb.append("1");
                        changed = true;
                        changedd = true;
                    }
                }
                if (!changed) {
                    sb.append(quest.substring(i + 6, i + 7));
                }
            }
            if (changedd) {
                number++;
                c.getPlayer().updateInfoQuest(m.questid - 2005, sb.toString());
                MapleQuest.getInstance(m.questid - 1995).forceStart(c.getPlayer(), 0, String.valueOf(number));
                c.getPlayer().dropMessage(-1, "探險" + number + "/" + m.maps.length + "個地區");
                c.getPlayer().dropMessage(-1, "稱號- " + String.valueOf(m) + "挑戰中");
                c.getSession().writeAndFlush(CWvsContext
                        .showQuestMsg("稱號- " + String.valueOf(m) + "挑戰中" + number + "/" + m.maps.length + "完成"));
            }
        }
    }
}
