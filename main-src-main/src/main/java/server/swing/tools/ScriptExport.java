/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.swing.tools;

import client.MonsterStatus;
import constants.GameConstants;
import extensions.temporary.InGameDirectionEventOpcode;
import extensions.temporary.FieldEffectOpcode;
import extensions.temporary.MessageOpcode;
import extensions.temporary.ScriptMessageType;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import server.MaplePortal;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import server.shark.SharkPacket;
import server.shark.SharkReader;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;
import extensions.temporary.UserEffectOpcode;

/**
 *
 * @author pungin
 */
public class ScriptExport extends javax.swing.JFrame {

    private File file = null;
    private SharkReader sharkReader = null;
    private String consume = "";
    private String scr = "";
    private final Map<Integer, Integer> npcs = new HashMap<>();
    private int mapId = 0;
    boolean questStart = true;
    private static final SimpleDateFormat sdfT = new SimpleDateFormat("yyyy年MM月dd日HH時mm分ss秒");
    private final MapleMapFactory mapFactory = new MapleMapFactory(0xFF);
    private int[] spawnNPC = null;
    private boolean lockUI = false;

    /**
     * Creates new form ScriptExport
     */
    public ScriptExport() {
        initComponents();
    }

    @Override
    public void setVisible(boolean bln) {
        if (bln) {
            setLocationRelativeTo(null);
        }
        super.setVisible(bln);
    }

    private void log(final String msg) {
        String file = "解包腳本/";
        if (consume.isEmpty()) {
            consume = sdfT.format(Calendar.getInstance().getTime());
        }
        switch (scr) {
            case "cm":
                file += "npc/";
                break;
            case "pi":
                file += "傳送點/";
                break;
            case "im":
                file += "道具/";
                break;
            case "ms":
                file += "地圖/";
                break;
            case "rm":
                file += "反應堆/";
                break;
            case "qm":
                file += "任務/";
                break;
            default:
                file += "未知/";
        }
        file += consume + ".js";

        FileOutputStream out = null;
        try {
            File outputFile = new File(file);
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(file, true);
            OutputStreamWriter osw = new OutputStreamWriter(out, "UTF-8");
            osw.write(msg);
            osw.flush();
        } catch (IOException ess) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    private void Dispose(boolean warp) {
        if (!"pi".equals(scr) && !"rm".equals(scr)) {
            if (warp) {
                writeScript("dispose();", true);
            }
            writeScript("    } else {", false);
            writeScript("dispose();", true);
            writeScript("    }", false);
            writeScript("}", false);
        } else {
            writeScript("}", false);
        }
        scr = "";
    }

    private void setScript(String scr, String consume, boolean warp) {
        if (!this.scr.isEmpty()) {
            Dispose(warp);
        }
        if (!"qm".equals(scr)) {
            questStart = true;
        }
        this.scr = scr;
        this.consume = consume;
        if (questStart) {
            writeScript("/* global " + scr + " */", false);
            if (!"pi".equals(scr) && !"rm".equals(scr)) {
                writeScript("var status = -1;", false);
            }
            writeScript("", false);
        }
        if ("pi".equals(scr)) {
            writeScript("function enter(pi) {", false);
        } else if ("rm".equals(scr)) {
            writeScript("function act() {", false);
        } else {
            if ("qm".equals(scr)) {
                if (questStart) {
                    writeScript("function start(mode, type, selection) {", false);
                } else {
                    writeScript("", false);
                    writeScript("function end(mode, type, selection) {", false);
                }
            } else {
                writeScript("function action(mode, type, selection) {", false);
            }
            if ("ms".equals(scr)) {
                writeScript("    if (mode === 0) {", false);
                writeScript("        status--;", false);
                writeScript("    } else {", false);
                writeScript("        status++;", false);
                writeScript("    }", false);
            } else {
                writeScript("    if (mode === 1) {", false);
                writeScript("        status++;", false);
                writeScript("    } else {", false);
                writeScript("        status--;", false);
                writeScript("    }", false);
            }
            writeScript("", false);
            writeScript("    var i = -1;", false);
            writeScript("    if (status <= i++) {", false);
            writeScript("dispose();", true);
            writeScript("    } else if (status === i++) {", false);
        }
    }

    private void writeScript(String info, boolean prefix) {
        if (scr.isEmpty()) {
            System.out.println("未寫腳本:" + info);
            return;
        }
        log((prefix ? ((scr.equals("pi") ? "    " : "        ") + scr + ".") : "") + info + "\r\n");
    }

    private void handleRecv(LittleEndianAccessor slea, RecvPacketOpcode recv) {
        String info = "";
        boolean prefix = false;
        boolean warp = true;
        String scr = "";
        String consume = "";
        switch (recv) {
            case CP_UserSelectNpc: {
                int oid = slea.readInt();
                if (npcs.containsKey(oid)) {
                    scr = "cm";
                    consume = String.valueOf(npcs.get(oid));
                } else {
                    System.err.println("NPC談話錯誤, 找不到OID為" + oid + "的NPC");
                }
                break;
            }
            case CP_UserQuestRequest: {
                final byte action = slea.readByte();
                int quest = slea.readInt();
                switch (action) {
                    case 5: // Scripted End Quest
                        questStart = false;
                    case 1: // Start Quest
                    case 4: // Scripted Start Quest
                        if (lockUI) {
                            questStart = true;
                            return;
                        }
                        final int npc = slea.readInt();
                        if (action == 1) {
                            if (npc == 0 && quest > 0) {
                                break;
                            }
                            final MapleQuest q = MapleQuest.getInstance(quest);
                            if (q == null || !q.hasEndScript()) {
                                break;
                            }
                        }
                        scr = "qm";
                        consume = String.valueOf(quest);
                        break;
                }
                break;
            }
            case CP_UserScriptItemUseRequest: {
                slea.readInt();
                slea.readShort();
                final int itemId = slea.readInt();
                scr = "im";
                consume = "consume_" + itemId;
                break;
            }
            case CP_UserPortalScriptRequest: {
                if (lockUI) {
                    return;
                }
                slea.skip(1);
                String portal_name = slea.readMapleAsciiString();
                MapleMap map = mapFactory.getMap(mapId);
                MaplePortal portal = null;
                if (map != null) {
                    portal = map.getPortal(portal_name);
                }
                if (portal == null || portal.getScriptName() == null) {
                    consume = mapId + "_" + portal_name;
                } else {
                    consume = portal.getScriptName();
                }
                scr = "pi";
                break;
            }
            case CP_UserScriptMessageAnswer:
            case CP_DirectionNodeCollision: {
                info += "    } else if (status === i++) {";
                break;
            }
            default:
                // System.out.println("未處理Recv包, 包頭" + recv.name());
                return;
        }
        if (!info.isEmpty()) {
            writeScript(info, prefix);
        }
        if (!scr.isEmpty()) {
            setScript(scr, consume, warp);
        }
    }

    private void handleSend(LittleEndianAccessor slea, SendPacketOpcode send) {
        String info = "";
        boolean prefix = false;
        boolean warp = true;
        String scr = "";
        String consume = "";
        switch (send) {
            case LP_InGameCurNodeEventEnd: {
                prefix = true;
                info += "getDirectionStatus(";
                info += slea.readByte() == 1;
                info += ");";
                break;
            }
            case LP_SetDirectionMode: {
                prefix = true;
                info += "lockKey(";
                info += slea.readByte() > 0;
                info += ");";
                break;
            }
            case LP_SetInGameDirectionMode: {
                prefix = true;
                info += "lockUI(";
                byte enable = slea.readByte();
                lockUI = enable > 0;
                info += enable;
                if (enable > 0) {
                    info += ", ";
                    info += slea.readShort();
                }
                info += ");";
                break;
            }
            case LP_SetStandAloneMode: {
                prefix = true;
                info += "disableOthers(";
                info += slea.readByte() == 1;
                info += ");";
                break;
            }
            case LP_UserEmotionLocal: {
                prefix = true;
                info += "getDirectionFacialExpression(";
                info += slea.readInt();
                info += ", ";
                info += slea.readInt();
                info += ");";
                break;
            }
            case LP_ScriptMessage: {
                prefix = true;
                int nSpeakerTypeID = slea.readByte();
                int nSpeakerTemplateID = slea.readInt();
                int nAnotherSpeakerTemplateID;
                boolean npc = slea.readByte() > 0;
                if (npc) {
                    nAnotherSpeakerTemplateID = slea.readInt();
                } else {
                    nAnotherSpeakerTemplateID = -1;
                }
                ScriptMessageType msgType = ScriptMessageType.getNPCTalkType(slea.readByte());
                short bParam = slea.readShort();
                byte eColor = slea.readByte();

                int nOtherSpeakerTemplateID = -1;
                switch (msgType) {
                    case SM_SAY:
                        if ((bParam & 0x4) != 0) {
                            nOtherSpeakerTemplateID = slea.readInt();
                        }
                        info += "say(";
                        info += nSpeakerTypeID;
                        info += ", ";
                        info += nSpeakerTemplateID;
                        info += ", ";
                        info += nAnotherSpeakerTemplateID;
                        info += ", ";
                        info += nOtherSpeakerTemplateID;
                        info += ", ";
                        info += bParam;
                        info += ", ";
                        info += eColor;
                        info += ", \"";
                        info += slea.readMapleAsciiString().replace("\r\n", "\\r\\n");
                        info += "\", ";
                        info += (slea.readByte() > 0);
                        info += ", ";
                        info += (slea.readByte() > 0);
                        info += ", ";
                        info += slea.readInt();
                        info += ");";
                        break;
                    case SM_SAYIMAGE:
                        info += "getAdviceTalk([";
                        byte size = slea.readByte();
                        for (int i = 0; i < size; i++) {
                            if (i != 0) {
                                info += ", ";
                            }
                            info += "\"" + slea.readMapleAsciiString().replace("\r\n", "\\r\\n") + "\"";
                        }
                        info += "]);";
                        break;
                    case SM_ASKYESNO:
                    case SM_ASKACCEPT:
                        if ((bParam & 0x4) != 0) {
                            nOtherSpeakerTemplateID = slea.readInt();
                        }
                        info += "askYesNo(";
                        info += nSpeakerTypeID;
                        info += ", ";
                        info += nSpeakerTemplateID;
                        info += ", ";
                        info += nAnotherSpeakerTemplateID;
                        info += ", ";
                        info += nOtherSpeakerTemplateID;
                        info += ", ";
                        info += bParam;
                        info += ", ";
                        info += eColor;
                        info += ", \"";
                        info += slea.readMapleAsciiString().replace("\r\n", "\\r\\n");
                        info += "\");";
                        break;
                    case SM_ASKTEXT:
                        info += "sendGetText(\"";
                        info += slea.readMapleAsciiString().replace("\r\n", "\\r\\n");
                        slea.readMapleAsciiString();
                        slea.readShort();
                        slea.readShort();
                        info += "\");";
                        break;
                    case SM_ASKNUMBER:
                        info += "sendGetNumber(\"";
                        info += slea.readMapleAsciiString().replace("\r\n", "\\r\\n");
                        info += "\", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ");";
                        break;
                    case SM_ASKMENU:
                        if ((bParam & 0x4) != 0) {
                            nOtherSpeakerTemplateID = slea.readInt();
                        }
                        info += "askMenu(";
                        info += nSpeakerTypeID;
                        info += ", ";
                        info += nSpeakerTemplateID;
                        info += ", ";
                        info += nAnotherSpeakerTemplateID;
                        info += ", ";
                        info += nOtherSpeakerTemplateID;
                        info += ", ";
                        info += bParam;
                        info += ", ";
                        info += eColor;
                        info += ", \"";
                        info += slea.readMapleAsciiString().replace("\r\n", "\\r\\n");
                        info += "\");";
                        break;
                    default:
                        System.out.println("發現需要處理的未知封包(" + send.name() + "),類型:" + msgType.name());
                        return;
                }
                break;
            }
            case LP_UserInGameDirectionEvent: {
                prefix = true;
                InGameDirectionEventOpcode type = InGameDirectionEventOpcode.getType(slea.readByte());
                switch (type) {
                    case InGameDirectionEvent_ForcedAction: {
                        info += "forcedAction([";
                        int value = slea.readInt();
                        info += value;
                        if (value <= 0x553) {
                            info += ", ";
                            info += slea.readInt();
                        }
                        info += "]);";
                        break;
                    }
                    case InGameDirectionEvent_Delay: {
                        info += "exceTime(";
                        info += slea.readInt();
                        info += ");";
                        break;
                    }
                    case InGameDirectionEvent_EffectPlay: {
                        info += "getEventEffect(";
                        info += "\"" + slea.readMapleAsciiString().replace("\r\n", "\\r\\n") + "\", [";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        byte value = slea.readByte();
                        info += value;
                        info += ", ";
                        int value3 = 0;
                        if (value > 0) {
                            value3 = slea.readInt();
                        }
                        byte value2 = slea.readByte();
                        info += value2;
                        info += ", ";
                        info += value3;
                        if (value2 > 0) {
                            info += ", ";
                            info += slea.readInt();
                            info += ", ";
                            slea.readByte();
                            info += slea.readByte();
                        }
                        info += "]);";
                        break;
                    }
                    case InGameDirectionEvent_ForcedInput: {
                        int nInput = slea.readInt();
                        switch (nInput) {
                            case 0:
                                info += "playerWaite();";
                                break;
                            case 1:
                                info += "playerMoveLeft();";
                                break;
                            case 2:
                                info += "playerMoveRight();";
                                break;
                            case 3:
                                info += "playerJump();";
                                break;
                            case 4:
                                info += "playerMoveDown();";
                                break;
                            default:
                                info += "forcedInput(";
                                info += nInput;
                                info += ");";
                                break;
                        }
                        break;
                    }
                    case InGameDirectionEvent_PatternInputRequest: {
                        info += "patternInput(";
                        info += "\"" + slea.readMapleAsciiString().replace("\r\n", "\\r\\n") + "\", [";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += "]);";
                        break;
                    }
                    case InGameDirectionEvent_CameraMove: {
                        info += "cameraMove([";
                        byte value = slea.readByte();
                        info += value;
                        info += ", ";
                        int value1 = slea.readInt();
                        info += value1;
                        if (value1 > 0) {
                            if (value == 0) {
                                info += ", ";
                                info += slea.readInt();
                                info += ", ";
                                info += slea.readInt();
                            }
                        }
                        info += "]);";
                        break;
                    }
                    case InGameDirectionEvent_CameraOnCharacter: {
                        info += "cameraOnCharacter(";
                        info += slea.readByte();
                        info += ");";
                        break;
                    }
                    case InGameDirectionEvent_CameraZoom: {
                        info += "cameraZoom([";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += "]);";
                        break;
                    }
                    case InGameDirectionEvent_VansheeMode: {
                        info += "hidePlayer(";
                        info += (slea.readByte() > 0);
                        info += ");";
                        break;
                    }
                    case InGameDirectionEvent_FaceOff: {
                        info += "faceOff(";
                        info += slea.readInt();
                        info += ");";
                        break;
                    }
                    case InGameDirectionEvent_Monologue: {
                        info += "sendTellStory(\"";
                        info += slea.readMapleAsciiString().replace("\r\n", "\\r\\n");
                        info += "\", ";
                        info += slea.readByte() > 0;
                        info += ");";
                        break;
                    }
                    case InGameDirectionEvent_RemoveAdditionalEffect: {
                        info += "removeAdditionalEffect();";
                        break;
                    }
                    case InGameDirectionEvent_ForcedMove: {
                        info += "forcedMove(";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ");";
                        break;
                    }
                    case InGameDirectionEvent_ForcedFlip: {
                        info += "forcedFlip(";
                        info += slea.readInt();
                        info += ");";
                        break;
                    }
                    case InGameDirectionEvent_InputUI: {
                        info += "inputUI(";
                        info += slea.readByte();
                        info += ");";
                        break;
                    }
                    default: {
                        System.out.println("發現需要處理的未知封包(" + send.name() + "),類型:" + type.name());
                        return;
                    }
                }
                break;
            }

            case LP_UserEffectLocal: {
                prefix = true;
                int eff = slea.readByte();
                UserEffectOpcode effect = null;
                for (UserEffectOpcode i : UserEffectOpcode.values()) {
                    if (i.getValue() == eff) {
                        effect = i;
                        break;
                    }
                }
                if (effect == null) {
                    System.out.println("CField.EffectPacket.showEffect() is Unknow effect :: [" + eff + "]");
                    return;
                }
                switch (effect) {
                    case UserEffect_ReservedEffectRepeat: {
                        info += "showWZEffect3(";
                        info += "\"" + slea.readMapleAsciiString().replace("\r\n", "\\r\\n") + "\", [";
                        byte value = slea.readByte();
                        info += value;
                        if (value == 0) {
                            info += ", ";
                            info += slea.readInt();
                            info += ", ";
                            info += slea.readInt();
                            info += ", ";
                            info += slea.readInt();
                            info += "]);";
                            break;
                        }
                        value = slea.readByte();
                        info += ", ";
                        info += value;
                        if (value == 0) {
                            info += "]);";
                            break;
                        }
                        info += ", ";
                        info += slea.readByte();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += "]);";
                        break;
                    }
                    case UserEffect_SpeechBalloon: {
                        info += "getNPCBubble(";
                        int exclamation = slea.readByte();
                        int bubbleType = slea.readInt();
                        slea.readInt();
                        String data = slea.readMapleAsciiString().replace("\r\n", "\\r\\n");
                        int time = slea.readInt();
                        slea.readInt();
                        slea.readInt();
                        slea.readInt();
                        slea.readInt();
                        slea.readInt();
                        int npcid = slea.readInt();
                        info += npcid + ", ";
                        info += "\"" + data + "\", ";
                        info += exclamation + ", ";
                        info += bubbleType + ", ";
                        info += time + ", ";
                        info += -1;
                        info += ");";
                        break;
                    }
                    case UserEffect_BlindEffect: {
                        info += "showDarkEffect(";
                        info += slea.readByte() == 1;
                        info += ");";
                        break;
                    }
                    case UserEffect_ReservedEffect: {
                        info += "showWZEffect(";
                        slea.readByte();
                        slea.readInt();
                        slea.readInt();
                        info += "\"" + slea.readMapleAsciiString().replace("\r\n", "\\r\\n") + "\"";
                        info += ");";
                        break;
                    }
                    case UserEffect_AvatarOriented: {
                        info += "showWZEffectNew(";
                        info += "\"" + slea.readMapleAsciiString().replace("\r\n", "\\r\\n") + "\"";
                        info += ");";
                        break;
                    }
                    case UserEffect_PlaySoundWithMuteBGM: {
                        info += "playSoundEffect(";
                        info += "\"" + slea.readMapleAsciiString().replace("\r\n", "\\r\\n") + "\"";
                        info += ");";
                        break;
                    }
                    case UserEffect_PlayExclSoundWithDownBGM: {
                        info += "playVoiceEffect(";
                        info += "\"" + slea.readMapleAsciiString().replace("\r\n", "\\r\\n") + "\"";
                        info += ");";
                        break;
                    }
                    case UserEffect_FadeInOut: {
                        info += "showBlackBGEffect(";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readByte();
                        info += ");";
                        break;
                    }
                    case UserEffect_Quest: {
                        byte size = slea.readByte();
                        for (int i = 0; i < size; i++) {
                            info = "gainItem(";
                            info += slea.readInt();
                            info += ", ";
                            info += slea.readInt();
                            info += ");";
                        }
                        writeScript(info, prefix);
                        return;
                    }
                    case UserEffect_PlayPortalSE: {
                        info += "playPortalSE();";
                        break;
                    }
                    case UserEffect_TextEffect: {
                        info += "showTextEffect(\"";
                        info += slea.readMapleAsciiString().replace("\r\n", "\\r\\n");
                        info += "\", [";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += "]);";
                        break;
                    }
                    case UserEffect_QuestComplete:
                    case UserEffect_PickUpItem: {
                        return;
                    }
                    default:
                        System.out.println("發現需要處理的未知封包(" + send.name() + "),類型:" + effect.name());
                        return;
                }
                break;
            }
            case LP_SetField: {
                npcs.clear();
                int mapId;
                byte spawnPoint;
                slea.readInt();
                slea.readByte();
                slea.readInt();
                boolean CharInfo = slea.readByte() == 1;
                slea.readInt();
                slea.readByte();
                slea.readInt();
                slea.readInt();
                slea.readByte();
                int v104 = slea.readShort();
                if (v104 != 0) {
                    slea.readMapleAsciiString();
                    for (int i = 0; i < v104; i++) {
                        slea.readMapleAsciiString();
                    }
                }
                if (CharInfo) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();

                    long mask = slea.readLong();
                    slea.readByte();
                    for (int i = 0; i < 3; i++) {
                        slea.readInt();
                    }

                    int nAddTail = slea.readByte();
                    for (int i = 1; i < nAddTail; i++) {
                        slea.readInt();
                    }

                    int FILETIME_Count = slea.readInt();
                    for (int i = 0; i < FILETIME_Count; i++) {
                        slea.readInt();
                        slea.readLong();
                    }

                    boolean v215 = slea.readByte() == 1;
                    if (v215) {
                        slea.readByte();
                        int v11 = slea.readInt();
                        for (int i = 0; i < v11; i++) {
                            slea.readLong();
                        }

                        int v14 = slea.readInt();
                        for (int i = 0; i < v14; i++) {
                            slea.readLong();
                        }
                    }

                    if ((mask & 1) != 0) {
                        slea.readInt();
                        slea.readInt();
                        slea.readInt();
                        String s = slea.readAsciiString(15);
                        slea.readByte();
                        slea.readByte();
                        slea.readByte();
                        slea.readInt();
                        slea.readInt();
                        slea.readByte();
                        slea.readByte();
                        slea.readByte();
                        int level = slea.readByte();
                        if (level < 0) {
                            level += 256;
                        }
                        System.out.println("等級:" + level);
                        short job = slea.readShort();
                        System.out.println("職業:" + job);

                        System.out.println("力量:" + slea.readShort());
                        System.out.println("敏捷:" + slea.readShort());
                        System.out.println("智力:" + slea.readShort());
                        System.out.println("幸運:" + slea.readShort());
                        System.out.println("HP:" + slea.readInt());
                        System.out.println("MaxHP:" + slea.readInt());
                        System.out.println("MP:" + slea.readInt());
                        System.out.println("MaxMP:" + slea.readInt());

                        slea.readShort();
                        if (GameConstants.isSeparatedSp(job)) {
                            int size = slea.readByte();
                            for (int i = 0; i < size; i++) {
                                slea.readByte();
                                slea.readInt();
                            }
                        } else {
                            slea.readShort();
                        }
                        slea.readLong();
                        slea.readInt();
                        slea.readInt();
                        slea.readLong();
                        slea.readLong();
                        mapId = slea.readInt();
                        spawnPoint = slea.readByte();
                    } else {
                        mapId = 0;
                        spawnPoint = 0;
                        System.err.println("LP_SetField包處理異常");
                    }
                } else {
                    slea.readByte();
                    mapId = slea.readInt();
                    spawnPoint = slea.readByte();
                }
                this.mapId = mapId;
                if (!"pi".equals(this.scr) && !"rm".equals(this.scr)) {
                    writeScript("dispose();", true);
                }
                prefix = true;
                info += "warp(";
                info += (mapId < 0 ? mapId + Integer.MAX_VALUE : mapId);
                info += ", ";
                info += spawnPoint;
                info += ");";
                scr = "ms";
                consume = String.valueOf(mapId);
                warp = false;
                break;
            }
            case LP_NpcChangeController: {
                prefix = true;
                boolean spawn = slea.readByte() == 1;
                if (spawn) {
                    int oid = slea.readInt();
                    int npcid = slea.readInt();
                    spawnNPC = new int[]{npcid, slea.readShort(), slea.readShort(), slea.readByte() == 1 ? 0 : 1, oid};
                    npcs.put(oid, npcid);
                    return;
                } else {
                    info += "removeNPCRequestController(";
                    info += String.valueOf(slea.readInt());
                }
                info += ");";
                break;
            }
            case LP_NpcEnterField: {
                npcs.put(slea.readInt(), slea.readInt());
                return;
            }
            case LP_NpcLeaveField: {
                npcs.remove(slea.readInt());
                return;
            }
            case LP_NpcSpecialAction: {
                prefix = true;
                info += "setNPCSpecialAction(";
                info += String.valueOf(slea.readInt());
                info += ", ";
                String action = slea.readMapleAsciiString();
                if (action.equals("summon")) {
                    if (spawnNPC == null || spawnNPC.length < 5) {
                        System.err.println("召喚NPC錯誤 - " + spawnNPC);
                        return;
                    }
                    info = "";
                    info += "spawnNPCRequestController(";
                    info += String.valueOf(spawnNPC[0]);
                    info += ", ";
                    info += String.valueOf(spawnNPC[1]);
                    info += ", ";
                    info += String.valueOf(spawnNPC[2]);
                    info += ", ";
                    info += String.valueOf(spawnNPC[3]);
                    info += ", ";
                    info += String.valueOf(spawnNPC[4]);
                    info += ");";
                    spawnNPC = null;
                    break;
                }
                info += "\"" + action + "\"";
                info += ", ";
                info += String.valueOf(slea.readInt());
                info += ", ";
                info += String.valueOf(slea.readByte() == 1);
                info += ");";
                break;
            }
            case LP_NpcUpdateLimitedInfo: {
                prefix = true;
                info += "updateNPCSpecialAction(";
                info += String.valueOf(slea.readInt());
                info += ", ";
                info += String.valueOf(slea.readInt());
                info += ", ";
                info += String.valueOf(slea.readInt());
                info += ", ";
                info += String.valueOf(slea.readInt());
                info += ");";
                break;
            }
            case LP_UserPlayMovieClip: {
                prefix = true;
                info += "playMovie(";
                info += "\"" + slea.readMapleAsciiString() + "\"";
                info += ", ";
                info += String.valueOf(slea.readByte() == 1);
                info += ");";
                break;
            }
            case LP_UserPlayMovieClipURL: {
                prefix = true;
                info += "playMovieURL(";
                info += "\"" + slea.readMapleAsciiString() + "\"";
                info += ");";
                break;
            }
            case LP_UserOpenUIWithOption: {
                prefix = true;
                info += "openUIOption(";
                info += String.valueOf(slea.readInt());
                info += ");";
                break;
            }
            case LP_ChangeSkillRecordResult: {
                prefix = true;
                slea.skip(3);
                short size = slea.readShort();
                for (int i = 0; i < size; i++) {
                    info = "";
                    info += "teachSkill(";
                    info += String.valueOf(slea.readInt());
                    info += ", ";
                    info += String.valueOf(slea.readInt());
                    info += ", ";
                    info += String.valueOf(slea.readInt());
                    info += ");";
                    writeScript(info, prefix);
                    slea.readLong();
                }
                return;
            }
            case LP_MobEnterField: {
                prefix = true;
                slea.readByte();
                slea.readInt();
                slea.readByte();
                info += "spawnMob(";
                info += String.valueOf(slea.readInt());

                if (slea.readByte() == 1) {
                    slea.readLong();
                    for (int i = 0; i < 12; i++) {
                        slea.readInt();
                    }
                }
                int[] mask = new int[GameConstants.MAX_MOBSTAT];
                for (int i = 0; i < mask.length; i++) {
                    mask[i] = slea.readInt();
                }
                List<Integer> buffs = buffStatusCal(mask);
                for (int bitNumber : buffs) {
                    if (bitNumber < MonsterStatus.M_Burned.getBitNumber()) {
                        slea.readInt();
                        slea.readInt();
                        slea.readShort();
                    }
                }
                if (buffs.contains(MonsterStatus.M_PDR.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_MDR.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_PCounter.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_MCounter.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_PCounter.getBitNumber())
                        || buffs.contains(MonsterStatus.M_MCounter.getBitNumber())) {
                    slea.readInt();
                    slea.readByte(); // boolean
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_Fatality.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_Explosion.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_MultiPMADDR.getBitNumber())) {
                    if (slea.readByte() > 0) {
                        slea.readInt();
                        slea.readInt();
                        slea.readInt();
                        slea.readInt();
                    }
                }
                if (buffs.contains(MonsterStatus.M_DeadlyCharge.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_Incizing.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_Speed.getBitNumber())) {
                    slea.readByte();
                }
                if (buffs.contains(MonsterStatus.M_BMageDebuff.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_DarkLightning.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_BattlePvP_Helena_Mark.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_MultiPMDR.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_Freeze.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_Burned.getBitNumber())) {
                    int v4 = slea.readByte();
                    int v23 = 0;
                    if (v4 > 0) {
                        do {
                            slea.readInt();
                            slea.readInt();
                            slea.readLong();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            slea.readInt();
                            ++v23;
                        } while (v23 < v4);
                    }
                }
                if (buffs.contains(MonsterStatus.M_Invincible.getBitNumber())) {
                    slea.readByte();
                    slea.readByte();
                }
                if (buffs.contains(MonsterStatus.M_ExchangeAttack.getBitNumber())) {
                    slea.readByte();
                }
                if (buffs.contains(MonsterStatus.M_AddDamParty.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_LinkTeam.getBitNumber())) {
                    slea.readMapleAsciiString();
                }
                if (buffs.contains(MonsterStatus.M_SoulExplosion.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_SeperateSoulP.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readShort();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_SeperateSoulC.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readShort();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_Ember.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_TrueSight.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.DEFUALT_1.getBitNumber())) {
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_Laser.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_ElementResetBySummon.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                    slea.readInt();
                }
                if (buffs.contains(MonsterStatus.M_BahamutLightElemAddDam.getBitNumber())) {
                    slea.readInt();
                    slea.readInt();
                }

                info += ", ";
                info += String.valueOf(slea.readShort());
                info += ", ";
                info += String.valueOf(slea.readShort());
                info += ");";
                break;
            }
            case LP_FuncKeySetByScript: {
                prefix = true;
                info += "setSkillMap(";
                boolean key = slea.readByte() == 1;
                info += String.valueOf(slea.readInt());
                info += ", ";
                if (key) {
                    info += String.valueOf(slea.readInt());
                } else {
                    info += String.valueOf(0);
                }
                info += ");";
                break;
            }
            case LP_FieldEffect: {
                prefix = true;
                FieldEffectOpcode type = FieldEffectOpcode.getType(slea.readByte());
                switch (type) {
                    case FieldEffect_Object:
                        info += "environmentChange(\"";
                        info += slea.readMapleAsciiString();
                        info += "\");";
                        break;
                    case FieldEffect_Screen:
                        info += "showMapEffect(\"";
                        info += slea.readMapleAsciiString();
                        info += "\");";
                        break;
                    case FieldEffect_Screen_AutoLetterBox:
                        info += "showScreenAutoLetterBox(\"";
                        info += slea.readMapleAsciiString();
                        info += "\");";
                        break;
                    case FieldEffect_Sound:
                        info += "playSound(\"";
                        info += slea.readMapleAsciiString();
                        info += "\");";
                        break;
                    case FieldEffect_ChangeBGM:
                        info += "changeMusic(\"";
                        info += slea.readMapleAsciiString();
                        info += "\");";
                        break;
                    case FieldEffect_Tremble:
                        info += "trembleEffect(";
                        info += slea.readByte();
                        info += ", ";
                        info += slea.readInt();
                        info += ");";
                        break;
                    case FieldEffect_Blind:
                        info += "darkEnv(";
                        info += (slea.readByte() > 0);
                        info += ", ";
                        info += slea.readShort();
                        slea.readShort();
                        slea.readShort();
                        slea.readShort();
                        info += ", ";
                        info += slea.readInt();
                        info += ");";
                        break;
                    case FieldEffect_OnOffLayer: {
                        byte value = slea.readByte();
                        int value1 = slea.readInt();
                        info += "showOnOffLayer([\"";
                        info += slea.readMapleAsciiString();
                        if (value != 0) {
                            if (value == 2) {
                                info += "\"], [";
                                info += String.valueOf(value);
                                info += ", ";
                                info += String.valueOf(value1);
                            } else if (value == 1) {
                                info += "\"], [";
                                info += String.valueOf(value);
                                info += ", ";
                                info += String.valueOf(value1);
                                info += ", ";
                                info += slea.readInt();
                                info += ", ";
                                info += slea.readInt();
                            }
                        } else {
                            int value2 = slea.readInt();
                            int value3 = slea.readInt();
                            int value4 = slea.readInt();
                            info += "\", \"";
                            info += slea.readMapleAsciiString();
                            info += "\"], [";
                            info += String.valueOf(value);
                            info += ", ";
                            info += String.valueOf(value1);
                            info += ", ";
                            info += String.valueOf(value2);
                            info += ", ";
                            info += String.valueOf(value3);
                            info += ", ";
                            info += String.valueOf(value4);
                            info += ", ";
                            info += slea.readInt();
                            info += ", ";
                            info += slea.readByte();
                        }
                        info += "]);";
                        break;
                    }
                    case FieldEffect_Overlap_Detail:
                        info += "showOverlapDetail([";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readByte();
                        info += "]);";
                        break;
                    case FieldEffect_Remove_Overlap_Detail:
                        info += "removeOverlapDetail(";
                        info += slea.readInt();
                        info += ");";
                        break;
                    case FieldEffect_StageClear:
                        info += "showStageClear(";
                        info += slea.readInt();
                        info += ");";
                        break;
                    default:
                        System.out.println("發現需要處理的未知封包(" + send.name() + "),類型:" + type.name());
                        return;
                }
                break;
            }
            case LP_ScriptProgressMessage: {
                prefix = true;
                info += "getTopMsg(\"";
                info += slea.readMapleAsciiString();
                info += "\");";
                break;
            }
            case LP_ScriptProgressMessageBySoul: {
                prefix = true;
                info += "getTopMsg2(\"";
                info += slea.readMapleAsciiString();
                info += "\");";
                break;
            }
            case LP_Message: {
                prefix = true;
                MessageOpcode msType = MessageOpcode.getType(slea.readByte());
                switch (msType) {
                    case MS_QuestRecordMessage: {
                        int quest = slea.readInt();
                        short status = slea.readByte();
                        switch (status) {
                            case 1:
                                info += "forceStartQuest(";
                                info += String.valueOf(quest);
                                String str = slea.readMapleAsciiString();
                                if (!str.isEmpty()) {
                                    info += ", \"";
                                    info += str + "\"";
                                }
                                info += ");";
                                break;
                            case 2:
                                info += "forceCompleteQuest(";
                                info += String.valueOf(quest);
                                info += ");";
                                break;
                            default:
                                return;
                        }
                        break;
                    }
                    case MS_QuestRecordExMessage: {
                        info += "updateInfoQuest(";
                        info += String.valueOf(slea.readInt());
                        info += ", ";
                        info += "\"" + slea.readMapleAsciiString() + "\"";
                        info += ");";
                        break;
                    }
                    default:
                        return;
                }
                break;
            }
            case LP_PlayAmbientSound: {
                prefix = true;
                info += "playAmbientSound(\"";
                info += slea.readMapleAsciiString();
                slea.readInt();
                info += "\");";
                break;
            }
            case LP_StopAmbientSound: {
                prefix = true;
                info += "stopAmbientSound(\"";
                info += slea.readMapleAsciiString();
                info += "\");";
                break;
            }
            case LP_CameraSwitch: {
                prefix = true;
                int nType = slea.readByte();
                switch (nType) {
                    case 0: // CAMERA_SWITCH_TYPE_NORMAL
                        info += "cameraSwitch(\"";
                        info += slea.readMapleAsciiString();
                        info += "\", ";
                        info += slea.readInt();
                        info += ");";
                        break;
                    case 1: // CAMERA_SWITCH_TYPE_POSITION
                        info += "cameraSwitchPos(";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ", ";
                        info += slea.readInt();
                        info += ");";
                        break;
                    case 2: // CAMERA_SWITCH_TYPE_BACK
                        info += "cameraSwitchBack();";
                        break;
                }
                break;
            }
            default:
                // System.out.println("未處理Send包, 包頭" + send.name());
                return;
        }
        if (!info.isEmpty()) {
            writeScript(info, prefix);
        }
        if (!scr.isEmpty()) {
            setScript(scr, consume, warp);
        }
    }

    private List<Integer> buffStatusCal(int[] mask) {
        List<Integer> values = new ArrayList<>();
        int value = 0;
        for (int i = 0; i < mask.length; i++) {
            int buffstat = mask[i];
            while (buffstat > 1) {
                value++;
                if ((buffstat & 1) != 0) {
                    values.add(31 - value + i * 32);
                }
                buffstat = buffstat >> 1;
            }
        }
        return values;
    }

    private void readPacket(SharkPacket packet) {
        final LittleEndianAccessor slea = new LittleEndianAccessor(new ByteArrayByteStream(packet.info));
        if (packet.outbound) {
            for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
                if (recv.getValue() == packet.opcode) {
                    try {
                        handleRecv(slea, recv);
                    } catch (Exception ex) {
                        System.err.println("解客戶端包錯誤, 包頭" + recv.name() + "\r\n" + slea.toString(true));
                    }
                    break;
                }
            }
        } else {
            for (final SendPacketOpcode send : SendPacketOpcode.values()) {
                if (send.getValue() == packet.opcode) {
                    try {
                        handleSend(slea, send);
                    } catch (Exception ex) {
                        System.err.println("解伺服器包錯誤, 包頭" + send.name() + "\r\n" + slea.toString(true));
                    }
                    break;
                }
            }
        }
    }

    private void export() {
        if (file == null) {
            System.out.println("文件為空");
            return;
        }
        if (!file.exists()) {
            System.out.println("文件不存在");
            return;
        }
        sharkReader = new SharkReader(file);

        while (sharkReader.available() > 0) {
            readPacket(sharkReader.read());
        }
        Dispose(true);
        System.out.println("解包完成");
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

        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("封包to腳本解包器");

        jLabel1.setText("msb檔案路徑");

        jButton1.setText("...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("解包");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup().addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 262,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButton1))
                                .addComponent(jButton2, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
                .createSequentialGroup().addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel1)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton1ActionPerformed
        JFileChooser fd = new JFileChooser();
        fd.showOpenDialog(null);
        file = fd.getSelectedFile();
        if (file != null) {
            if (!file.exists()) {
                System.out.println("檔案不存在");
                return;
            }
            jTextField1.setText(file.getPath());
        }
    }// GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton2ActionPerformed
        if (file == null) {
            JOptionPane.showMessageDialog(null, "請先選擇要解包的楓鯊檔案(.msb)");
            return;
        }
        if (!file.exists()) {
            System.out.println("文件不存在");
            return;
        }
        consume = "";
        scr = "";
        questStart = true;
        export();
    }// GEN-LAST:event_jButton2ActionPerformed
     // Variables declaration - do not modify//GEN-BEGIN:variables

    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}
