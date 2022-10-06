package tools.packet;

import client.character.stat.MapleBuffStat;
import client.skill.Skill;
import client.forceatoms.ForceAtom;
import client.familiar.MonsterFamiliar;
import client.*;
import client.inventory.*;
import constants.GameConstants;
import constants.ItemConstants;
import constants.QuickMove.QuickMoveNPC;
import constants.ServerConfig;
import constants.ServerConstants;
import constants.SkillConstants;
import extensions.temporary.InGameDirectionEventOpcode;
import extensions.temporary.FieldEffectOpcode;
import extensions.temporary.ScriptMessageType;
import handling.SendPacketOpcode;
import handling.channel.handler.AttackInfo;
import handling.world.World;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import java.awt.Point;
import java.util.*;
import server.MaplePackageActions;
import server.MapleTrade;
import server.events.MapleSnowball;
import server.life.MapleNPC;
import server.maps.*;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.MapleShop;
import tools.AttackPair;
import tools.HexTool;
import tools.Pair;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;
import extensions.temporary.UserEffectOpcode;
import extensions.temporary.MessageOpcode;
import extensions.temporary.ShopOpcode;
import extensions.temporary.StorageType;
import handling.InteractionOpcode;
import server.subsystem.bingo.MapleBingoRecord;
import tools.DateUtil;
import tools.Rect;

public class CField {

    public static byte[] getPacketFromHexString(String hex) {
        return HexTool.getByteArrayFromHexString(hex);
    }

    public static byte[] getServerIP(MapleClient c, int port, int clientId) {
        return getServerIP(c, port, clientId, 0);
    }

    public static byte[] getServerIP(MapleClient c, int port, int characterId, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SelectCharacterResult.getValue());
        mplew.write(type);
        mplew.write(0);

        mplew.write(ServerConstants.getGateway_IP());
        mplew.writeShort(port);
        if (c.getTempIP().length() > 0) {
            for (String s : c.getTempIP().split(",")) {
                mplew.write(Integer.parseInt(s));
            }
            mplew.writeShort(port);
        } else {
            mplew.writeInt(0); // uChatIp
            mplew.writeShort(0); // uChatPort
        }

        mplew.writeInt(characterId);
        mplew.writeInt(characterId); // dwCharacterID

        mplew.write(0);
        mplew.writeInt(0); // ulArgument

        mplew.write(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] autoLogin(String code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_IssueReloginCookie.getValue());
        mplew.writeMapleAsciiString(code);

        return mplew.getPacket();
    }

    public static byte[] exitGame() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ReturnToTitle.getValue());

        return mplew.getPacket();
    }

    public static byte[] getChannelChange(int port) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MigrateCommand.getValue());
        mplew.write(1);
        mplew.write(ServerConstants.getGateway_IP());
        mplew.writeShort(port);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getPVPType(int type, List<Pair<Integer, String>> players1, int team, boolean enabled,
            int lvl) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPFieldEnter.getValue());
        mplew.write(type);
        mplew.write(lvl);
        mplew.write(enabled ? 1 : 0);
        mplew.write(0);
        if (type > 0) {
            mplew.write(team);
            mplew.writeInt(players1.size());
            for (Pair<Integer, String> pl : players1) {
                mplew.writeInt(pl.left);
                mplew.writeMapleAsciiString(pl.right);
                mplew.writeShort(2660);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] getPVPTransform(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPTeamChange.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] getPVPDetails(List<Pair<Integer, Integer>> players) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPModeChange.getValue());
        mplew.write(1);
        mplew.write(0);
        mplew.writeInt(players.size());
        for (Pair<Integer, Integer> pl : players) {
            mplew.writeInt(pl.left);
            mplew.write(pl.right);
        }

        return mplew.getPacket();
    }

    public static byte[] enablePVP(boolean enabled) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPStateChange.getValue());
        mplew.write(enabled ? 1 : 2);

        return mplew.getPacket();
    }

    public static byte[] getPVPScore(int score, boolean kill) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPUpdateCount.getValue());
        mplew.writeInt(score);
        mplew.write(kill ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getPVPResult(List<Pair<Integer, MapleCharacter>> flags, int exp, int winningTeam,
            int playerTeam) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPModeResult.getValue());
        mplew.writeInt(flags.size());
        for (Pair<Integer, MapleCharacter> f : flags) {
            mplew.writeInt(f.right.getId());
            mplew.writeMapleAsciiString(f.right.getName());
            mplew.writeInt(f.left);
            mplew.write(f.right.getTeam() + 1);
            mplew.write(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(exp);
        mplew.write(0);
        mplew.writeShort(100);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(winningTeam);
        mplew.write(playerTeam);

        return mplew.getPacket();
    }

    public static byte[] getPVPTeam(List<Pair<Integer, String>> players) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPUpdateTeamInfo.getValue());
        mplew.writeInt(players.size());
        for (Pair<Integer, String> pl : players) {
            mplew.writeInt(pl.left);
            mplew.writeMapleAsciiString(pl.right);
            mplew.write(0x0A);
            mplew.write(0x64);
        }

        return mplew.getPacket();
    }

    public static byte[] getPVPScoreboard(List<Pair<Integer, MapleCharacter>> flags, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPUpdateRankInfo.getValue());
        mplew.writeShort(flags.size());
        for (Pair<Integer, MapleCharacter> f : flags) {
            mplew.writeInt(f.right.getId());
            mplew.writeMapleAsciiString(f.right.getName());
            mplew.writeInt(f.left);
            mplew.write(type == 0 ? 0 : f.right.getTeam() + 1);
            mplew.writeInt(0);
        }
        mplew.writeShort(flags.size());
        for (Pair<Integer, MapleCharacter> f : flags) {
            mplew.writeInt(f.right.getId());
            mplew.writeMapleAsciiString(f.right.getName());
            mplew.writeInt(f.left);
            mplew.write(type == 0 ? 0 : f.right.getTeam() + 1);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] getPVPPoints(int p1, int p2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPTeamScore.getValue());
        mplew.writeInt(p1);
        mplew.writeInt(p2);

        return mplew.getPacket();
    }

    public static byte[] getPVPKilled(String lastWords) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPReviveMessage.getValue());
        mplew.writeMapleAsciiString(lastWords);

        return mplew.getPacket();
    }

    public static byte[] getPVPMode(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPScreenEffect.getValue());
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] getPVPIceHPBar(int hp, int maxHp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PVPIceKnightHPChange.getValue());
        mplew.writeInt(hp);
        mplew.writeInt(maxHp);

        return mplew.getPacket();
    }

    public static byte[] getCaptureFlags(MapleMap map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CAPTURE_FLAGS.getValue());
        mplew.writeRect(map.getArea(0));
        mplew.writeInt(map.getGuardians().get(0).left.x);
        mplew.writeInt(map.getGuardians().get(0).left.y);
        mplew.writeRect(map.getArea(1));
        mplew.writeInt(map.getGuardians().get(1).left.x);
        mplew.writeInt(map.getGuardians().get(1).left.y);

        return mplew.getPacket();
    }

    public static byte[] getCapturePosition(MapleMap map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        Point p1 = map.getPointOfItem(2910000);
        Point p2 = map.getPointOfItem(2910001);
        mplew.writeShort(SendPacketOpcode.CAPTURE_POSITION.getValue());
        mplew.write(p1 == null ? 0 : 1);
        if (p1 != null) {
            mplew.writeInt(p1.x);
            mplew.writeInt(p1.y);
        }
        mplew.write(p2 == null ? 0 : 1);
        if (p2 != null) {
            mplew.writeInt(p2.x);
            mplew.writeInt(p2.y);
        }

        return mplew.getPacket();
    }

    public static byte[] resetCapture() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CAPTURE_RESET.getValue());

        return mplew.getPacket();
    }

    public static byte[] gameMsg(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserNoticeMsg.getValue());
        mplew.writeMapleAsciiString(msg);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] getSaveBuff(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetBuffProtector.getValue());
        mplew.writeInt(itemId);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] setSkillMap(int skillId, int key) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FuncKeySetByScript.getValue());
        mplew.write(key != 0);
        mplew.writeInt(skillId);
        if (key != 0) {
            mplew.writeInt(key);
        }

        return mplew.getPacket();
    }

    public static byte[] innerPotentialMsg(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.INNER_ABILITY_MSG.getValue());
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] updateInnerPotential(int skill, int level, byte position, byte rank) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CharacterPotentialSet.getValue());
        mplew.write(1); // unlock
        mplew.write(1); // 0 = no update
        mplew.writeShort(position); // 1-3
        mplew.writeInt(skill); // skill id (7000000+)
        mplew.writeShort(level); // level, 0 = blank inner ability
        mplew.writeShort(rank); // rank
        mplew.write(1); // 0 = no update

        return mplew.getPacket();
    }

    public static byte[] innerPotentialResetMessage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.INNER_ABILITY_RESET_MSG.getValue());
        mplew.write(HexTool.getByteArrayFromHexString(
                "26 00 49 6E 6E 65 72 20 50 6F 74 65 6E 74 69 61 6C 20 68 61 73 20 62 65 65 6E 20 72 65 63 6F 6E 66 69 67 75 72 65 64 2E 01"));

        return mplew.getPacket();
    }

    public static byte[] updateHonour(int honourLevel, int honourExp, boolean levelup) {
        /*
         * data: 03 00 00 00 69 00 00 00 01
         */
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CharacterHonorExp.getValue());
        mplew.writeInt(honourLevel);
        mplew.writeInt(honourExp);
        mplew.write(levelup ? 1 : 0); // shows level up effect

        return mplew.getPacket();
    }

    public static byte[] onSetField(MapleCharacter chr) {
        return setField(chr, true, null, 0);
    }

    public static byte[] getWarpToMap(MapleMap to, int spawnPoint, MapleCharacter chr) {
        return setField(chr, false, to, spawnPoint);
    }

    private static byte[] setField(MapleCharacter chr, boolean CharInfo, MapleMap to, int spawnPoint) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetField.getValue());
        // SetChannelID
        mplew.writeInt(chr.getClient().getChannel() - 1);
        // m_wstr
        mplew.write(0);
        mplew.writeInt(0);
        // bPopupDlg
        mplew.write(CharInfo ? 1 : 2);
        mplew.writeInt(0);

        mplew.write(0);
        // nFieldWidth
        int bottom = chr.getMap().getBottom();
        int top = chr.getMap().getTop();
        mplew.writeInt(bottom - top);
        // nFieldHeight
        int right = chr.getMap().getRight();
        int left = chr.getMap().getLeft();
        mplew.writeInt(right - left);
        // bCharacterData
        mplew.write(CharInfo);

        int nNotifierCheck = 0;
        mplew.writeShort(nNotifierCheck); // size :: v104
        if (nNotifierCheck != 0) {
            // pBlockReasonIter
            mplew.writeMapleAsciiString("");
            for (int i = 0; i < nNotifierCheck; i++) {
                // sNotifierMessage
                mplew.writeMapleAsciiString("");
            }
        }

        if (CharInfo) {
            chr.getCalcDamage().writeCalcDamageSeed(chr, mplew);
            PacketHelper.CharacterData_Decode(mplew, chr);
            // UnkFunction Start
            while (true) {
                int v17 = 0;
                mplew.writeInt(v17);
                if (v17 == 0) {
                    break;
                }
                // UnkFunction2 Start
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeLong(0);
                mplew.writeLong(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                // UnkFunction2 End
            }
            // UnkFunction End
            PacketHelper.addLuckyLogoutInfo(mplew, chr);
        } else {
            mplew.writeBoolean(false);
            mplew.writeInt(to.getId());
            mplew.write(spawnPoint);
            mplew.writeInt(chr.getStat().getHp());
            boolean v12 = false;
            mplew.writeBoolean(v12);
            if (v12) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }
        // CWvsContext::SetWhiteFadeInOut
        mplew.write(0);
        // bChatBlockReason
        mplew.write(0);
        // paramFieldInit.ftServer
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        // paramFieldInit.nMobStatAdjustRate
        mplew.writeInt(100);

        /* FieldCustom */
        boolean bCFieldCustom = false; // to.isCustomMap()
        mplew.writeBoolean(bCFieldCustom);
        if (bCFieldCustom) {
            mplew.writeInt(0); // partyBonusExpRate
            mplew.writeMapleAsciiString("" /* to.getCustomMapBgm() */); // BGM
            mplew.writeInt(0/* to.getCustomBgMapID() */); // bgFieldID
        }
        // CWvsContext::OnInitPvPStat
        mplew.writeBoolean(false);
        // bCanNotifyAnnouncedQuest
        mplew.write(0);
        // bCField::DrawStackEventGauge
        mplew.write(GameConstants.isSeparatedSp(chr.getJob()) ? 1 : 0); // v109

        // CheckPacket
        byte[] data = FamiliarPacket.getWarpToMap(chr, CharInfo);
        mplew.writeInt(data.length);
        mplew.write(data);

        // nCField::DrawStackEventGauge
        boolean v88 = false;
        mplew.writeBoolean(v88);
        if (v88) {
            mplew.writeInt(0);
        }
//        if (GameConstants.isBanBanBaseField(chr.getMapId())) {
//            int v18 = 0;
//            mplew.write(v18);
//            for (int i = 0; i < v18; i++) {
//                mplew.writeMapleAsciiString("");
//            }
//        }
        CUser_StarPlanetRank_Decode(mplew);

        boolean bDecodeStarPlanetRoundInfo = false;
        mplew.writeBoolean(bDecodeStarPlanetRoundInfo);
        if (bDecodeStarPlanetRoundInfo) { // Function
            DecodeStarPlanetRoundInfo(mplew);
        }

        DecodeTextEquipInfo(mplew);
        DecodeFreezeHotEventInfo(mplew);
        DecodeEventBestFriendInfo(mplew);

        int v3_7394 = -1;
        mplew.writeInt(v3_7394);

        // 星期日楓之谷活動 "UI/UIWindow6.img/loginNoticePopup/sundayMaple_" + Date
        mplew.writeMapleAsciiString("");

        mplew.writeInt(0);

        byte v55 = (byte) 150;
        mplew.write(v55);

        // sub_238C760
        int v5 = 0;
        mplew.writeInt(v5);
        for (int i = 0; i < v5; i++) {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    private static void CUser_StarPlanetRank_Decode(final MaplePacketLittleEndianWriter mplew) {
        boolean result = false;
        mplew.writeBoolean(result);
        if (result) {
            // nRoundID
            mplew.writeInt(0);
            int v5 = 0;
            mplew.write(v5);
            if (v5 >= 10) {
                for (int i = 0; i <= 10; i++) {
                    // anPoint
                    mplew.writeInt(0);
                    // anRanking
                    mplew.writeInt(0);
                    // atLastCheckRank
                    mplew.writeInt(0);
                }
            } else {
                // anPoint
                mplew.writeInt(0);
                // anRanking
                mplew.writeInt(0);
                // atLastCheckRank
                mplew.writeInt(0);
            }
            // ftShiningStarExpiredTime
            mplew.writeLong(0);
            // nShiningStarPickedCount
            mplew.writeInt(0);
            // nRoundStarPoint
            mplew.writeInt(0);
        }
    }

    private static void DecodeStarPlanetRoundInfo(final MaplePacketLittleEndianWriter mplew) {
        // m_nStarPlanetRoundID
        mplew.writeInt(0);
        // m_nStarPlanetRoundState
        mplew.write(0);
        // m_ftStarPlanetRoundEndDate
        mplew.writeLong(0);
    }

    private static void DecodeTextEquipInfo(final MaplePacketLittleEndianWriter mplew) {
        int nCount = 0;
        mplew.writeInt(nCount);
        for (int i = 0; i < nCount; i++) {
            // nBodyPart
            mplew.writeInt(0);
            // sText
            mplew.writeMapleAsciiString("");
        }
    }

    private static void DecodeFreezeHotEventInfo(final MaplePacketLittleEndianWriter mplew) {
        // nAccountType
        mplew.write(0);
        // dwAccountID
        mplew.writeInt(0);
    }

    private static void DecodeEventBestFriendInfo(final MaplePacketLittleEndianWriter mplew) {
        // m_dwEventBestFriendAID
        mplew.writeInt(0);
    }

    public static byte[] removeBGLayer(boolean remove, int map, byte layer, int duration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_BG_LAYER.getValue());
        mplew.write(remove ? 1 : 0); // Boolean show or remove
        mplew.writeInt(map);
        mplew.write(layer); // Layer to show/remove
        mplew.writeInt(duration);

        return mplew.getPacket();
    }

    public static byte[] setMapObjectVisible(List<Pair<String, Byte>> objects) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetMapObjectVisible.getValue());
        mplew.write(objects.size());
        for (Pair<String, Byte> object : objects) {
            mplew.writeMapleAsciiString(object.getLeft());
            mplew.write(object.getRight());
        }

        return mplew.getPacket();
    }

    public static byte[] onChnageBackground(List<Pair<String, Integer>> flags) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANGE_BACKGROUND.getValue());
        mplew.write(flags == null ? 1 : flags.size());
        if (flags != null) {
            for (Pair<String, Integer> f : flags) {
                mplew.writeMapleAsciiString(f.left);
                mplew.write(f.right.byteValue());
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        } else {
            // 官方的包
            mplew.writeMapleAsciiString("default");
            mplew.write(1);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        /*
         * Map.wz/Obj/login.img/WorldSelect/background/background number Backgrounds ids
         * sometime have more than one background anumation Background are like layers,
         * backgrounds in the packets are removed, so the background which was hiden by
         * the last one is shown.
         */

        return mplew.getPacket();
    }

    public static byte[] serverBlocked(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_TransferChannelReqIgnored.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] pvpBlocked(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_TransferPvpReqIgnored.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] showEquipEffect() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FieldSpecificData.getValue());

        return mplew.getPacket();
    }

    public static byte[] showEquipEffect(int team) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FieldSpecificData.getValue());
        mplew.writeShort(team);

        return mplew.getPacket();
    }

    public static byte[] multiChat(String name, String chattext, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_GroupMessage.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(chattext);

        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithCS(String target, boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Whisper.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        mplew.write(2);
        mplew.writeInt(-1);

        return mplew.getPacket();
    }

    public static byte[] getWhisper(String sender, int channel, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Whisper.getValue());
        mplew.write(18);
        mplew.writeMapleAsciiString(sender);
        mplew.writeShort(channel - 1);
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    public static byte[] getWhisperReply(String target, byte reply) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Whisper.getValue());
        mplew.write(10);
        mplew.writeMapleAsciiString(target);
        mplew.write(reply);

        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithMap(String target, int mapid, boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Whisper.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        byte val = 1;
        mplew.write(val);
        mplew.writeInt(mapid);
        if (!buddy && val == 1) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] getFindReply(String target, int channel, boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Whisper.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        mplew.write(3);
        mplew.writeInt(channel - 1);

        return mplew.getPacket();
    }

    public static byte[] showMapEffect(String path) {
        return OnFieldEffect(new String[]{path}, FieldEffectOpcode.FieldEffect_Screen);// was 3
    }

    public static byte[] MapNameDisplay(int mapid) {
        return OnFieldEffect(new String[]{"maplemap/enter/" + mapid}, FieldEffectOpcode.FieldEffect_Screen);
    }

    public static byte[] Aran_Start() {
        return OnFieldEffect(new String[]{"Aran/balloon"}, FieldEffectOpcode.FieldEffect_Screen);
    }

    public static byte[] musicChange(String song) {
        return OnFieldEffect(new String[]{song}, FieldEffectOpcode.FieldEffect_ChangeBGM);// was 6
    }

    public static byte[] showScreenAutoLetterBox(String effect) {
        return OnFieldEffect(new String[]{effect}, FieldEffectOpcode.FieldEffect_Screen_AutoLetterBox);// was 3
    }

    public static byte[] playSound(String sound) {
        return OnFieldEffect(new String[]{sound}, FieldEffectOpcode.FieldEffect_Sound);// was 4
    }

    public static byte[] stageClear(int value) {
        return OnFieldEffect(FieldEffectOpcode.FieldEffect_StageClear, null, new int[]{value});
    }

    public static byte[] OnFieldEffect(FieldEffectOpcode type, String[] strs, int[] values) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FieldEffect.getValue());
        mplew.write(type.getValue());
        switch (type) {
            case FieldEffect_Summon:
                mplew.write(values[0]); // nType
                mplew.writeInt(values[1]); // rx
                mplew.writeInt(values[2]); // ry
                break;
            case FieldEffect_Object:
                mplew.writeMapleAsciiString(strs[0]); // sName
                break;
            case FieldEffect_Object_Disable:
                mplew.writeMapleAsciiString(strs[0]); // sName
                mplew.write(values[0]); // Boolean bCheckPreWord
                break;
            case FieldEffect_Screen:
                mplew.writeMapleAsciiString(strs[0]);
                break;
            case FieldEffect_Screen_AutoLetterBox:
                mplew.writeMapleAsciiString(strs[0]);
                mplew.writeInt(values[0]);
                break;
            case FieldEffect_TopScreen:
                mplew.writeMapleAsciiString(strs[0]);
                break;
            case FieldEffect_Screen_Delayed:
                mplew.writeMapleAsciiString(strs[0]);
                mplew.writeInt(values[0]);
                break;
            case FieldEffect_TopScreen_Delayed:
                mplew.writeMapleAsciiString(strs[0]);
                mplew.writeInt(values[0]);
                break;
            case FieldEffect_Sound: // 播放音樂
                mplew.writeMapleAsciiString(strs[0]);
                mplew.writeInt(1000);
                break;
            case FieldEffect_MobHPTag:
                mplew.writeInt(values[0]); // sAniamtionName
                mplew.writeLong(values[1]); // MaxHP
                mplew.writeLong(values[2]); // sKeyName
                mplew.write(values[3]); // it
                mplew.write(values[4]); // uHeight
                break;
            case FieldEffect_ChangeBGM: // 更變背景音樂
                mplew.writeMapleAsciiString(strs[0]);
                mplew.writeInt(values[0]);
                mplew.writeInt(values[1]);
                break;
            case FieldEffect_BGMVolumeOnly:
                mplew.write(values[0]); // boolean
                break;
            case FieldEffect_BGMVolume:
                mplew.writeInt(values[0]);
                mplew.writeInt(values[0]);
                break;
            case UNK_A:
                mplew.writeInt(values[0]);
                break;
            case UNK_18:
                mplew.writeMapleAsciiString(strs[0]);
                break;
            case FieldEffect_Tremble:
                mplew.write(values[0]);
                mplew.writeInt(values[1]);
                mplew.writeShort(values[2]);
                break;
            case FieldEffect_RewordRullet:
                mplew.writeInt(values[0]);
                mplew.writeInt(values[1]);
                mplew.writeInt(values[2]);
                break;
            case FieldEffect_FloatingUI:
                mplew.writeMapleAsciiString(strs[0]);
                mplew.write(values[0]);
                mplew.write(values[1]);
                break;
            case FieldEffect_Blind: // 背景變暗
                mplew.write(values[0]); // boolean
                mplew.writeShort(values[1]); // bBinary
                mplew.writeShort(values[2]); // usR
                mplew.writeShort(values[3]); // aniInfo.bLoop
                mplew.writeShort(values[4]); // r.p
                mplew.writeInt(values[5]); // pLayer.m_pInterface
                break;
            case UNK_1F:
                mplew.write(values[0]);
                mplew.writeShort(values[1]);
                mplew.writeShort(values[2]);
                mplew.writeShort(values[3]);
                mplew.writeShort(values[4]);
                mplew.writeInt(values[5]);
                mplew.write(values[6]);
                break;
            case FieldEffect_GrayScale:
                mplew.writeShort(values[0]);
                mplew.write(values[1]);
                break;
            case FieldEffect_ColorChange:
                mplew.writeShort(values[0]);
                mplew.writeShort(values[1]);
                mplew.writeShort(values[2]);
                mplew.writeShort(values[3]);
                mplew.writeInt(values[4]);
                mplew.writeInt(values[5]);
                switch (values[0]) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        break;
                    case 4:
                        mplew.writeInt(values[6]);
                        break;
                    case 5:
                    case 6:
                    case 7:
                        break;
                }
                break;
            case FieldEffect_OnOffLayer:
                mplew.write(values[0]);
                mplew.writeInt(values[1]);
                mplew.writeMapleAsciiString(strs[0]);
                if (values[0] != 0) {
                    if (values[0] != 2) {
                        if (values[0] == 1) {
                            mplew.writeInt(values[2]);
                            mplew.writeInt(values[3]);
                        }
                    }
                } else {
                    mplew.writeInt(values[2]);
                    mplew.writeInt(values[3]); // nRY
                    mplew.writeInt(values[4]); // nZ
                    mplew.writeMapleAsciiString(strs[1]);
                    mplew.writeInt(values[5]);
                    mplew.write(values[6]); // bPostRender
                }
                break;
            case FieldEffect_Overlap:
                mplew.writeInt(values[0]);
                break;
            case FieldEffect_Overlap_Detail:
                mplew.writeInt(values[0]);
                mplew.writeInt(values[1]);
                mplew.writeInt(values[2]);
                mplew.write(values[3]);
                break;
            case FieldEffect_Remove_Overlap_Detail:
                mplew.writeInt(values[0]);
                break;
            case FieldEffect_StageClear:
                mplew.writeInt(values[0]);
                break;
            case FieldEffect_TopScreen_WithOrigin:
                mplew.writeMapleAsciiString(strs[0]);
                mplew.write(values[0]);
                break;
            case FieldEffect_SpineScreen:
                mplew.write(values[0]); // boolean
                mplew.write(values[1]); // boolean
                mplew.write(values[2]); // boolean
                mplew.writeInt(values[3]);
                mplew.writeMapleAsciiString(strs[0]);
                mplew.writeMapleAsciiString(strs[1]);
                mplew.write(values[4]);
                if (values[4] > 0) {
                    mplew.writeMapleAsciiString(strs[2]);
                }
                break;
            case FieldEffect_OffSpineScreen:
                mplew.writeMapleAsciiString(strs[0]);
                mplew.writeInt(values[0]);
                if (values[0] > 0) {
                    if (values[0] - 1 > 0) {
                        if (values[0] - 1 == 1) {
                            mplew.writeMapleAsciiString(strs[1]);
                        }
                    } else {
                        mplew.writeInt(values[1]);
                    }
                }
                break;
            case UNK_C:
                mplew.writeMapleAsciiString(strs[0]);
                mplew.writeInt(values[0]);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] OnFieldEffect(String[] strs, FieldEffectOpcode type) {
        return CField.OnFieldEffect(type, strs, new int[]{0, 0});
    }

    public static byte[] trembleEffect(int type, int delay) {
        return CField.OnFieldEffect(FieldEffectOpcode.FieldEffect_Tremble, null, new int[]{type, delay, 30});
    }

    public static byte[] environmentMove(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_ENV.getValue());
        mplew.writeMapleAsciiString(env);
        mplew.writeInt(mode);

        return mplew.getPacket();
    }

    public static byte[] getUpdateEnvironment(MapleMap map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_ENV.getValue());
        mplew.writeInt(map.getEnvironment().size());
        for (Map.Entry<String, Integer> mp : map.getEnvironment().entrySet()) {
            mplew.writeMapleAsciiString(mp.getKey());
            mplew.writeInt(mp.getValue());
        }

        return mplew.getPacket();
    }

    public static byte[] startMapEffect(String msg, int itemid, boolean active) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_BlowWeather.getValue());
        mplew.write(active ? 0 : 1);
        mplew.writeInt(itemid);
        if (active) {
            mplew.writeMapleAsciiString(msg);
            mplew.write(0); // Boolean
        }
        return mplew.getPacket();
    }

    public static byte[] removeMapEffect() {
        return startMapEffect(null, 0, false);
    }

    public static byte[] getGMEffect(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AdminResult.getValue());
        mplew.write(value);
        mplew.writeZeroBytes(17);

        return mplew.getPacket();
    }

    public static byte[] showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Quiz.getValue());
        mplew.write(askQuestion ? 1 : 0);
        mplew.write(questionSet);
        mplew.writeShort(questionId);

        return mplew.getPacket();
    }

    public static byte[] showEventInstructions() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Desc.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getClockTime(int hour, int min, int sec) {
        return OnClock((byte) 1, -1, new byte[]{(byte) hour, (byte) min, (byte) sec}, (byte) -1, -1, false);
    }

    public static byte[] getClock(int iTime) {
        return OnClock((byte) 2, iTime, null, (byte) -1, -1, false);
    }

    public static byte[] getPVPClock(int bClockType, int iTime) {
        return OnClock((byte) 3, iTime, null, (byte) bClockType, -1, false);
    }

    private static byte[] OnClock(byte bMode, int iTime, byte[] aTime, byte bClockType, int bLeftArrow,
            boolean bBoolean) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Clock.getValue());
        mplew.write(bMode);
        switch (bMode) {
            case 0:
                // 未知
                mplew.writeInt(iTime);
                break;
            case 1:
                // 當前時鐘:時/分/秒
                int iNum = 0;
                if (aTime != null) {
                    for (byte anATime : aTime) {
                        mplew.write(anATime);
                        iNum++;
                        if (iNum == 3) {
                            break;
                        }
                    }
                }
                for (int i = 0; i < iNum; i++) {
                    mplew.write(0);
                }
                break;
            case 2:
                // 頂部計時器 - 剩餘時間:
                mplew.writeInt(iTime);
                break;
            case 3:
                // 特殊類型頂部計時器
                /*
             * 0 - 剩下時間 1 - RED 剩下時間 BLUE 2 - 剩下時間 3 - 剩下時間 4 - CAPTURE THE FLAG
                 */
                mplew.write(bClockType);
                mplew.writeInt(iTime);
                break;
            case 4:
                // 斑斑計時條
                mplew.writeInt(bLeftArrow);
                mplew.writeInt(iTime);
                break;
            case 5:
                // 快進/快退計時器
                /*
             * true - 快退 false - 快進
                 */
                mplew.write(bBoolean);
                mplew.writeInt(iTime);
                break;
            case 6:
                // 毫秒計時器
                mplew.writeInt(iTime);
                break;
            case 7:
                // 靜止計時器
                /*
             * true - 靜止 false - 計時
                 */
                mplew.write(bBoolean);
                mplew.writeInt(iTime);
                break;
            case 9:
                mplew.write(bBoolean);
                mplew.writeInt(iTime);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] boatPacket(int effect, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CONTIMOVE.getValue());
        mplew.write(effect);
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] setBoatState(int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CONTISTATE.getValue());
        mplew.write(effect);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] onSetQuestClear() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_SetQuestClear.getValue());
        return mplew.getPacket();
    }

    public static byte[] stopClock() {
        return getPacketFromHexString(Integer.toHexString(SendPacketOpcode.LP_DestroyClock.getValue()) + " 00");
    }

    public static byte[] showAriantScoreBoard() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ShowArenaResult.getValue());

        return mplew.getPacket();
    }

    public static byte[] sendPyramidUpdate(int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_StalkResult.getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] sendPyramidResult(byte rank, int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MassacreResult.getValue());
        mplew.write(rank);
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] quickSlot(String skil) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_QuickslotMappedInit.getValue());
        mplew.write(skil == null ? 0 : 1);
        if (skil != null) {
            String[] slots = skil.split(",");
            for (int i = 0; i < 32; i++) {
                mplew.writeInt(Integer.parseInt(slots[i]));
            }
        }

        return mplew.getPacket();
    }

    public static byte[] getMovingPlatforms(MapleMap map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FootHoldMove.getValue());
        mplew.writeInt(map.getPlatforms().size());
        for (MapleNodes.MaplePlatform mp : map.getPlatforms()) {
            mplew.writeMapleAsciiString(mp.name);
            mplew.writeInt(mp.start);
            mplew.writeInt(mp.SN.size());
            for (Integer SN : mp.SN) {
                mplew.writeInt(SN);
            }
            mplew.writeInt(mp.speed);
            mplew.writeInt(mp.x1);
            mplew.writeInt(mp.x2);
            mplew.writeInt(mp.y1);
            mplew.writeInt(mp.y2);
            mplew.writeInt(mp.x1);
            mplew.writeInt(mp.y1);
            mplew.write(mp.r);
            mplew.write(0);
        }

        return mplew.getPacket();
    }

    public static byte[] sendPyramidKills(int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MassacreIncGauge.getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] sendPVPMaps() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PvPStatusResult.getValue());
        mplew.write(3); // max amount of players
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(10); // how many peoples in each map
            mplew.writeZeroBytes(120);
        }
        mplew.writeShort(150); // 經驗值加倍活動(1.5倍)
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] OnCreateForceAtom(boolean bByMob, MapleCharacter chr, List<Integer> dwTargetID,
            int nForceAtomType, int dwTarget, List<ForceAtom> forceInfo, Point ptForcedTarget, Rect rcStart,
            int nBulletItemID, int nArriveDir, int nArriveRange) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ForceAtomCreate.getValue());

        mplew.write(bByMob);
        if (bByMob) {
            mplew.writeInt(chr.getId());
        }
        mplew.writeInt(bByMob ? dwTargetID.get(0) : chr.getId());
        mplew.writeInt(nForceAtomType);

        if (!(nForceAtomType == 0 || nForceAtomType == 9 || nForceAtomType == 14)) {
            boolean bToMob = true;
            mplew.write(bToMob);
            switch (nForceAtomType) {
                case 2:
                case 3:
                case 6:
                case 7:
                case 11:
                case 12:
                case 13:
                case 17:
                case 19:
                case 20:
                    mplew.writeInt(dwTargetID.size()); // size
                    for (Integer aDwTargetID : dwTargetID) {
                        mplew.writeInt(aDwTargetID);
                    }
                    break;
                default:
                    mplew.writeInt(dwTargetID.get(0));
                    break;
            }
            mplew.writeInt(dwTarget);
        }

        for (ForceAtom info : forceInfo) {
            if (chr.isShowInfo()) {
                chr.dropMessage(-5, "[ForceAtom] Key: " + info.getKey() + " Inc: " + info.getInc());
            }
            mplew.write(1); // while on/off
            mplew.writeInt(info.getKey()); // count // dwKey // = 0 會卡在地圖上
            mplew.writeInt(info.getInc()); // color // nInc
            mplew.writeInt(info.getFirstImpact()); // nFirstImpact
            mplew.writeInt(info.getSecondImpact()); // nSecondImpact
            mplew.writeInt(info.getAngle()); // nAngle
            mplew.writeInt((info.getStartDelay())); // 0 // nStartDelay
            mplew.writeInt(info.getStart().x); // ptStart.x
            mplew.writeInt(info.getStart().y); // ptStart.y
            mplew.writeInt(info.getCreateTime()); // dwCreateTime
            mplew.writeInt(info.getMaxHitCount());
            mplew.writeInt(info.getEffectIdx()); // 0
            mplew.writeInt(0);
        }

        mplew.write(0);

        if (nForceAtomType == 11) {
            mplew.writeInt(rcStart.getLeft());
            mplew.writeInt(rcStart.getTop());
            mplew.writeInt(rcStart.getRight());
            mplew.writeInt(rcStart.getBottom());
            mplew.writeInt(nBulletItemID);
        }
        if (nForceAtomType == 9 || nForceAtomType == 15) {
            mplew.writeInt(rcStart.getLeft());
            mplew.writeInt(rcStart.getTop());
            mplew.writeInt(rcStart.getRight());
            mplew.writeInt(rcStart.getBottom());
        }
        if (nForceAtomType == 16) {
            mplew.writeInt(rcStart.getLeft()); // 客戶端會做-10
            mplew.writeInt(rcStart.getTop()); // 客戶端會做-10
        }
        if (nForceAtomType == 17) {
            mplew.writeInt(nArriveDir);
            mplew.writeInt(nArriveRange);
        }
        if (nForceAtomType == 18) {
            mplew.writeInt(ptForcedTarget.x);
            mplew.writeInt(ptForcedTarget.y);
        }

        return mplew.getPacket();
    }

    public static byte[] achievementRatio(int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetAchieveRate.getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] getQuickMoveInfo(List<QuickMoveNPC> qm) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetQuickMoveInfo.getValue());
        mplew.write(qm.size());
        for (int i = 0; i < qm.size(); i++) {
            mplew.writeInt(i);
            mplew.writeInt(qm.get(i).getId());
            mplew.writeInt(qm.get(i).getType());
            mplew.writeInt(qm.get(i).getLevel());
            mplew.writeMapleAsciiString(qm.get(i).getDescription());
            mplew.writeLong(DateUtil.getFileTimestamp(-2));
            mplew.writeLong(DateUtil.getFileTimestamp(-1));
        }

        return mplew.getPacket();
    }

    public static byte[] spawnPlayerMapobject(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserEnterField.getValue());
        mplew.writeInt(chr.getId()); // dwCharacterID
        CUserRemote_Init(mplew, chr);

        return mplew.getPacket();
    }

    private static void CUserRemote_Init(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(chr.getLevel()); // m_nLevel
        mplew.writeMapleAsciiString(chr.getName()); // m_sCharacterName

        // m_sParentName
        MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(111111));
        mplew.writeMapleAsciiString((ultExplorer != null && ultExplorer.getCustomData() != null) ? ultExplorer.getCustomData() : "");

        MapleGuild gs;
        if (chr.getGuildId() > 0) {
            gs = World.Guild.getGuild(chr.getGuildId());
        } else {
            gs = null;
        }
        mplew.writeMapleAsciiString(gs == null ? "" : gs.getName()); // m_sGuildName
        mplew.writeShort(gs == null ? 0 : gs.getLogoBG()); // m_nGuildMarkBg
        mplew.write(gs == null ? 0 : gs.getLogoBGColor()); // m_nGuildMarkBgColor
        mplew.writeShort(gs == null ? 0 : gs.getLogo()); // m_nGuildMark
        mplew.write(gs == null ? 0 : gs.getLogoColor()); // m_nGuildMarkColor

        mplew.write(chr.getGender()); // m_nGender
        // mplew.write(0); // m_nAccountGender
        mplew.writeInt(chr.getFame()); // m_nPopularity
        // mplew.writeInt(0); // m_nFarmLevel
        mplew.writeInt(0); // m_nNameTagMark

        chr.getTempstats().EncodeForRemote(mplew);

        mplew.writeShort(chr.getJob()); // m_nJobCode
        mplew.writeShort(chr.getSubcategory()); // m_nSubJobCode
        mplew.writeInt(0);// [33 01 00 00] // m_nTotalCHUC
        mplew.writeInt(0);
        PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
        if (MapleJob.is神之子(chr.getJob())) {
            PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
        }

        PacketHelper.UnkFunctin6(mplew);

        mplew.writeInt(0); // dwDriverID
        mplew.writeInt(0); // dwPassenserID

        int buffSrc = chr.getBuffSource(MapleBuffStat.RideVehicle);
        if ((chr.getBuffedValue(MapleBuffStat.Flying) != null) && (buffSrc > 0)) {// 妮娜的魔法阵 1C 7B 1D 00 //5C 58 8A 00
            addMountId(mplew, chr, buffSrc);
            mplew.writeInt(chr.getId());
            mplew.writeInt(0);
        } else {
            mplew.writeInt(0);
            mplew.writeInt(0);
            int size = 0;
            mplew.writeInt(size);
            for (int i = 0; i < size; i++) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }

        mplew.writeInt(Math.min(250, chr.getInventory(MapleInventoryType.CASH).countById(5110000))); // 頭上「紅心巧克力」個數
        mplew.writeInt(chr.getItemEffect());// [76 72 4C 00] - 撥水柱特效
        mplew.writeInt(0);
        mplew.writeInt(chr.getTitleEffect());// [51 75 38 00] - 與風暴同在
        ///
        mplew.write(0);
        ///
        mplew.writeInt(chr.getDamageSkin());// 數據1：[0C 00 00 00] 數據2：[09 04 00 00]傷害字型
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(-1);
        mplew.writeMapleAsciiString("");
        mplew.writeMapleAsciiString("");
        mplew.writeShort(-1);
        mplew.writeShort(-1);
        mplew.write(0);
        mplew.writeInt(GameConstants.getInventoryType(chr.getChair()) == MapleInventoryType.SETUP ? chr.getChair() : 0); // m_nPortableChairID
        String text = "";
        mplew.writeInt(text.length()); // 椅子文字長度
        if (text.length() > 0) {
            mplew.writeMapleAsciiString(text); // 椅子文字 m_sPortableChairMsg
        }
        int nCount = 0;
        mplew.writeInt(nCount);
        for (int i = 0; i < nCount; i++) {
            mplew.writeInt(0); // vIndex.baseclass_0.baseclass_0.___u0._s0.llVal
        }
        boolean bUnk = false;
        mplew.write(bUnk);
        if (bUnk) {
            // function sub_81F990
        }
        mplew.writeInt(0);
        mplew.writeInt(0); // new v143
        mplew.writePos(chr.getTruePosition());
        mplew.write(chr.getStance()); // m_nMoveAction
        mplew.writeShort(chr.getFh()); // dwSN

        mplew.write(0);
        // mplew.write(chr.getHaku() != null && MapleJob.is陰陽師(chr.getJob()));
        // if (chr.getHaku() != null && MapleJob.is陰陽師(chr.getJob())) {
        // MapleHaku haku = chr.getHaku();
        // mplew.writeInt(haku.getObjectId());
        // mplew.writeInt(40020109);
        // mplew.write(1);
        // mplew.writePos(haku.getPosition());
        // mplew.write(0);
        // mplew.writeShort(haku.getStance());
        // mplew.write(0);
        // }
        for (int i = 0; i <= 3; i++) { // 寵物
            MaplePet pet = chr.getSummonedPet(i);
            mplew.write(pet != null);
            if (pet == null) {
                break;
            }
            mplew.writeInt(i);
            PetPacket.addPetInfo(mplew, chr, pet, false);
        }
        mplew.write(false);

        mplew.writeInt(chr.getMount() != null ? chr.getMount().getLevel() : 1); // 骑宠等级 默认是1级
        mplew.writeInt(chr.getMount() != null ? chr.getMount().getExp() : 0);
        mplew.writeInt(chr.getMount() != null ? chr.getMount().getFatigue() : 0);
        mplew.write(false);
        PacketHelper.addAnnounceBox(mplew, chr);
        mplew.write((chr.getChalkboard() != null) && (chr.getChalkboard().length() > 0) ? 1 : 0); // m_bADBoardRemote
        if ((chr.getChalkboard() != null) && (chr.getChalkboard().length() > 0)) {
            mplew.writeMapleAsciiString(chr.getChalkboard());
        }

        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        List<MapleRing> allrings = rings.getLeft();
        allrings.addAll(rings.getMid());
        addRingInfo(mplew, allrings);
        addRingInfo(mplew, allrings);
        addMRingInfo(mplew, rings.getRight(), chr);
        int v65 = 0;
        mplew.write(v65);
        for (int o = 0; o < v65; o++) {
            nCount = 0;
            mplew.writeInt(nCount);
            for (int i = 0; i < nCount; i++) {
                mplew.writeInt(0);
            }
        }
        int m_nDelayedEffectFlag = chr.getStat().Berserk ? 1 : 0;
        mplew.write(m_nDelayedEffectFlag);
        if ((m_nDelayedEffectFlag & 1) != 0) {
        }
        if ((m_nDelayedEffectFlag & 2) != 0) {
        }
        if ((m_nDelayedEffectFlag & 8) != 0) {
            mplew.writeInt(0); // m_tDelayedPvPEffectTime
        } else if ((m_nDelayedEffectFlag & 0x10) != 0) {
            mplew.writeInt(0); // m_tDelayedPvPEffectTime
        }
        if ((m_nDelayedEffectFlag & 0x20) != 0) {
            mplew.writeInt(0); // m_tHitPeriodRemain_Revive
        }
        mplew.writeInt(chr.getMount().getItemId()); // m_nEvanDragonGlide_Riding 骑宠id

        if (MapleJob.is凱撒(chr.getJob())) {
            String x = chr.getOneInfo(12860, "extern");
            mplew.writeInt(x == null ? 0 : Integer.parseInt(x)); // m_nKaiserMorphRotateHueExtern
            x = chr.getOneInfo(12860, "inner");
            mplew.writeInt(x == null ? 0 : Integer.parseInt(x)); // m_nKaiserMorphRotateHueInnner
            x = chr.getOneInfo(12860, "premium");
            mplew.write(x == null ? 0 : Integer.parseInt(x)); // m_bKaiserMorphPrimiumBlack
        }

        mplew.writeInt(0); // CUser::SetMakingMeisterSkillEff

        // PacketHelper.addFarmInfo(mplew, chr.getClient(), 0);
        for (int i = 0; i < 20; i = i + 4) {
            mplew.write(-1); // m_aActiveEventNameTag
        }

        // m_CustomizeEff.sEffectInfo
        int v84 = 0;
        mplew.writeInt(v84);
        if (v84 > 0) {
            mplew.writeMapleAsciiString("");
        }
        mplew.write(1); // m_bSoulEffect
        if (false) {
            int v87 = 0;
            mplew.writeInt(v87);
            for (int i = 0; i < v87; i++) {
                mplew.writeInt(0);
            }
        }
        // CUser::SetFlareBlink
        // m_pFlameWizardHelper.p
        boolean v90 = false;
        mplew.write(v90);
        if (v90) {
            boolean v91 = false;
            mplew.write(v91);
            if (v91) {
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
            }
        }

        mplew.write(0); // for CUser::StarPlanetRank::Decode
        mplew.writeInt(0); // for CUser::DecodeStarPlanetTrendShopLook
        mplew.writeInt(0); // for CUser::DecodeTextEquipInfo

        mplew.write(0); // CUser::DecodeFreezeHotEventInfo
        mplew.writeInt(0);

        mplew.writeInt(0); // CUser::DecodeEventBestFriendInfo

        mplew.write(MapleJob.is凱內西斯(chr.getJob())); // 心靈本能 142001007
        mplew.write(1); // bBeastFormWingOnOff

        mplew.writeInt(0); // mesoChairCount

        mplew.writeInt(1051291); // 1051291

        mplew.writeInt(0);
        mplew.writeInt(0);

        mplew.writeInt(0); // for
        mplew.writeInt(0); // for
        mplew.writeInt(0); // for

        mplew.writeInt(0);
    }

    public static byte[] removePlayerFromMap(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserLeaveField.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] getChatText(int cidfrom, String text, boolean whiteBG, int show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserChat.getValue());
        mplew.writeInt(cidfrom);
        mplew.write(whiteBG ? 1 : 0);
        mplew.writeMapleAsciiString(text);
        mplew.write(show);
        mplew.write(0);
        mplew.write(-1);

        return mplew.getPacket();
    }

    public static byte[] updateDamageSkin(int cid, int damageSkin) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserSetDamageSkin.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(damageSkin);

        return mplew.getPacket();
    }

    public static byte[] getEffectSwitch(int cid, List<Integer> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.EFFECT_SWITCH_RESPONSE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(items.size());
        for (int i : items) {
            mplew.writeInt(i);
        }
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] getScrollEffect(int chr, int scroll, int toScroll) {
        return getScrollEffect(chr, Equip.ScrollResult.SUCCESS, false, false, scroll, toScroll);
    }

    public static byte[] getScrollEffect(int chr, Equip.ScrollResult scrollSuccess, boolean legendarySpirit,
            boolean whiteScroll, int scroll, int toScroll) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserItemUpgradeEffect.getValue());
        mplew.writeInt(chr);
        mplew.write(
                scrollSuccess == Equip.ScrollResult.SUCCESS ? 1 : scrollSuccess == Equip.ScrollResult.CURSE ? 2 : 0);
        mplew.write(legendarySpirit ? 1 : 0);
        mplew.writeInt(scroll);
        mplew.writeInt(toScroll);
        mplew.write(whiteScroll ? 1 : 0);
        mplew.write(0);// ?

        return mplew.getPacket();
    }

    public static byte[] showEnchanterEffect(int cid, byte result) {
        tools.data.MaplePacketLittleEndianWriter mplew = new tools.data.MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ENCHANTER_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(result);

        return mplew.getPacket();
    }

    public static byte[] showSoulScrollEffect(int cid, byte result, boolean destroyed) {
        tools.data.MaplePacketLittleEndianWriter mplew = new tools.data.MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SOULSCROLL_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(result);
        mplew.write(destroyed ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] showMagnifyingEffect(int chr, short pos, boolean bonusPot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MAGNIFYING_EFFECT.getValue());
        mplew.writeInt(chr);
        mplew.writeShort(pos);
        mplew.write(bonusPot ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] showPotentialReset(int chrId, boolean success, int itemid, boolean bonus) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(bonus ? SendPacketOpcode.SHOW_BONUS_POTENTIAL_RESET.getValue()
                : SendPacketOpcode.SHOW_POTENTIAL_RESET.getValue());
        mplew.writeInt(chrId);
        mplew.write(success ? 1 : 0);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] showPotentialEx(int chrId, boolean success, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_POTENTIAL_EXPANSION.getValue());
        mplew.writeInt(chrId);
        mplew.write(success);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] showBonusPotentialEx(int chrId, boolean success, int itemid, boolean broken) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_FIREWORKS_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.write(success);
        mplew.writeInt(itemid);
        mplew.write(broken);

        return mplew.getPacket();
    }

    public static byte[] sendImperialBodyDestory(List<Point> im) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.IMPERIAL_BODY_DESTORY.getValue());
        mplew.writeShort(0); // 不知道什麼用,有 [78 00] [00 00] [DA 02]
        mplew.write(im.size());
        for (Point pos : im) {
            mplew.writePos(pos);
        }

        return mplew.getPacket();
    }

    public static byte[] changeSalonRespons() {
        return getTaiwanSpecialPacket(SendPacketOpcode.FLASH_CUBE_RESPONSE, -1, (byte) 1, new int[]{1}, null, null,
                null, null);
    }

    public static byte[] getSalonRespons(MapleCharacter chr) {
        return getTaiwanSpecialPacket(SendPacketOpcode.FLASH_CUBE_RESPONSE, -1, (byte) 3, new int[]{1}, null, null,
                null, chr);
    }

    public static byte[] showMapleCubeCost(int value, long cost) {
        return getTaiwanSpecialPacket(SendPacketOpcode.FLASH_CUBE_RESPONSE, 3994895, (byte) 3, new int[]{value},
                new long[]{cost}, null, null, null);
    }

    public static byte[] showFlashCubeEquip(Item item) {
        return getTaiwanSpecialPacket(SendPacketOpcode.FLASH_CUBE_RESPONSE, 5062017, (byte) 3, new int[]{0}, null,
                item, null, null);
    }

    public static byte[] getFlashCubeRespons(int itemId, int value) {
        return getTaiwanSpecialPacket(SendPacketOpcode.FLASH_CUBE_RESPONSE, itemId, (byte) 1, new int[]{value}, null,
                null, null, null);
    }

    public static byte[] getShimmerCubeRespons() {
        return getTaiwanSpecialPacket(SendPacketOpcode.SHIMMER_CUBE_RESPONSE, 5062020, (byte) 1, new int[]{0}, null,
                null, null, null);
    }

    public static byte[] getShimmerCubeRespons(int line, List<Integer> selects) {
        return getTaiwanSpecialPacket(SendPacketOpcode.SHIMMER_CUBE_RESPONSE, 5062020, (byte) 7, new int[]{0, line},
                null, null, selects, null);
    }

    private static byte[] getTaiwanSpecialPacket(SendPacketOpcode opcode, int cubeId, byte op, int[] value,
            long[] value2, Item item, List<Integer> selects, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        int v2 = 0; // 不知什麼用 [184數據]
        switch (opcode) {
            case SHIMMER_CUBE_RESPONSE:
                v2 += 0x80CC; // 32972
                break;
            case FLASH_CUBE_RESPONSE:
                v2 += 0xE3C8; // 58312
                break;
            // case 0x5A6:
            // v2 += 0x1EA0; // 7840
            // break;
        }

        mplew.writeShort(opcode.getValue());

        int pointer;
        switch (cubeId) {
            case -1: // 皇家相簿
                switch (op) {
                    case 1:
                        // 更變造型
                        pointer = 0x2c504c33; // 743459891
                        break;
                    case 3:
                        // 打開-預覽 [完成-184]
                        pointer = 0xeb70f39c; // -344919140
                        break;
                    case 7:
                    case 0xB:
                        // 更新臉型/髮型儲存數據 [完成-184]
                        pointer = 0x2e3c4666; // 775702118
                        break;
                    default:
                        pointer = 0;
                        System.err.println("未知指針值的皇家相簿操作：" + cubeId);
                        break;
                }
                break;
            case 3994895: // 楓方塊 [完成-182]
                pointer = 0xc1d4ba08; // -1043023352
                break;
            case 5062017: // 閃耀方塊 [完成-182]
                pointer = 0xd01d76a9; // -803375447
                break;
            case 5062019: // 閃耀鏡射方塊 [完成-182]
                pointer = 0x6ba1513b; // 1805734203
                break;
            case 5062020: // 閃炫方塊 [完成-182]
                if (op == 7) { // 顯示洗出的潛能
                    pointer = 0x458de9f3; // 1166928371
                } else { // 選擇潛能
                    pointer = 0xe5ea6bb8; // -437621832
                }
                break;
            case 5062021: // 新對等方塊
            default:
                pointer = 0;
                System.err.println("未知指針值的台方塊：" + cubeId);
        }

        mplew.writeInt(pointer);
        mplew.write(op);
        // 楓方塊：「0 - 更新價格; 1 - 詢問是否洗道具; 2 - 重新加載道具潛能 ; 大於3 - 取下道具」
        // 皇家相簿：pos
        mplew.writeInt(value[0]);
        switch (op) {
            case 1:
                break;
            case 3:
                if (cubeId == -1) { // 皇家相簿::打開-預覽
                    mplew.writeInt(25631);
                    mplew.write(0);
                    Map<Integer, List<Pair<Integer, Integer>>> salon = chr == null ? null : chr.getSalon();
                    mplew.writeInt(salon == null || !salon.containsKey(30000) ? 3 : salon.get(30000).size());
                    mplew.writeInt(salon == null || !salon.containsKey(20000) ? 3 : salon.get(20000).size());

                    mplew.writeInt(salon == null || !salon.containsKey(30000) ? 3 : salon.get(30000).size());
                    if (salon == null || !salon.containsKey(30000)) {
                        mplew.writeZeroBytes(21);
                    } else {
                        for (Pair<Integer, Integer> hair : salon.get(30000)) {
                            mplew.writeInt(hair.left); // 髮型Id
                            // mplew.write(hair.right); // -1 or 0不知
                            mplew.write(0);
                            mplew.writeShort(0);
                        }
                    }

                    mplew.writeInt(salon == null || !salon.containsKey(20000) ? 3 : salon.get(20000).size());
                    if (salon == null || !salon.containsKey(20000)) {
                        mplew.writeZeroBytes(12);
                    } else {
                        for (Pair<Integer, Integer> face : salon.get(20000)) {
                            mplew.writeInt(face.left); // 臉型Id
                        }
                    }
                } else if (item != null) { // 閃耀顯示洗後結果
                    PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
                } else if (value2 != null) { // 楓方塊顯示消耗楓幣
                    mplew.writeLong(value2[0]);
                }
                break;
            case 7: // 閃耀顯示洗後結果
                mplew.writeInt(value[1]);
                mplew.writeInt(selects.size());
                selects.forEach(mplew::writeInt);
                break;
            case 0x6F: // Happy Day
                mplew.write(value[1]); // 每週加碼
                mplew.write(value[2]); // 每週加碼+
                mplew.writeInt(value[3]);
                mplew.writeShort(0);
                // for { // 獎品
                mplew.write(7); // HD的季度第幾季度吧
                mplew.writeBoolean(false); // 是否已兌換
                mplew.write(3); // 類型(3 普通, 4 每個賬號只能兌換一次)
                mplew.writeInt(2472001); // 道具ID
                // }
                // for { // 可能是已兌換獎品的訊息
                mplew.write(7); // HD的季度第幾季度吧
                mplew.writeBoolean(true); // 是否已兌換
                mplew.write(0);
                mplew.writeLong(0); // 兌換日期
                // }
                break;
            default:
                System.err.println("台方塊操作碼：" + op);
        }

        return mplew.getPacket();
    }

    public static byte[] getBossPartyCheckDone() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_BossPartyCheckDone.getValue());
        int type = 1;
        mplew.writeInt(type);

        return mplew.getPacket();
    }

    public static byte[] getShowBossListWait(MapleCharacter chr, int usType, int[] Value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_UserWaitQueueReponse.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(usType);
        switch (usType) {
            case 11: {
                mplew.write(Value[0]); // nResultCode
                mplew.writeInt(Value[1]); // 40905 ??
                mplew.writeInt(Value[2]); // waitingQueueID
                mplew.writeInt(0); // nHideQuest
                mplew.writeInt(0);
                mplew.writeInt(0); // dwReason
                mplew.writeInt(Value[3]); // dwEnterField
                break;
            }
            case 13:
            case 14: {
                mplew.write(Value[0]);
                mplew.writeInt(Value[1]);
                mplew.writeInt(Value[2]);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                break;
            }
            case 18:
                mplew.write(Value[0]);
                mplew.writeInt(Value[1]);
                mplew.writeInt(Value[2]);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                break;
            case 20:
                mplew.write(Value[0]);
                break;
            case 21:
                int v3 = 0;
                mplew.write(v3);
                for (int v34 = 0; v34 < v3; v34++) {
                    mplew.write(0);
                }
                break;
            case 22:
                break;
            case 23:
                break;
            case 24:
                break;
            default:
                mplew.write(Value[0]);
                mplew.writeInt(Value[1]);
                mplew.writeInt(Value[2]);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] getShowBingo(MapleBingoRecord bingoRecord) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_OpenBingo.getValue());
        mplew.write(0);
        mplew.writeInt(2);
        mplew.writeInt(-1);
        bingoRecord.encode(mplew);
        mplew.writeInt(14); // 未知
        mplew.writeInt(14); // 未知
        return mplew.getPacket();
    }

    public static byte[] showNebuliteEffect(int chr, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_NEBULITE_EFFECT.getValue());
        mplew.writeInt(chr);
        mplew.write(success ? 1 : 0);
        mplew.writeMapleAsciiString(success ? "Successfully mounted Nebulite." : "Failed to mount Nebulite.");

        return mplew.getPacket();
    }

    public static byte[] useNebuliteFusion(int cid, int itemId, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_FUSION_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(success ? 1 : 0);
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] pvpAttack(int cid, int playerLevel, int skill, int skillLevel, int speed, int mastery, int projectile, int attackCount, int chargeTime, int stance, int direction, int range, int linkSkill, int linkSkillLevel, boolean movementSkill, boolean pushTarget, boolean pullTarget, List<AttackPair> attack) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserHitByUser.getValue());
        mplew.writeInt(cid);
        mplew.write(playerLevel);
        mplew.writeInt(skill);
        mplew.write(skillLevel);
        mplew.writeInt(linkSkill != skill ? linkSkill : 0);
        mplew.write(linkSkillLevel != skillLevel ? linkSkillLevel : 0);
        mplew.write(direction);
        mplew.write(movementSkill ? 1 : 0);
        mplew.write(pushTarget ? 1 : 0);
        mplew.write(pullTarget ? 1 : 0);
        mplew.write(0);
        mplew.writeShort(stance);
        mplew.write(speed);
        mplew.write(mastery);
        mplew.writeInt(projectile);
        mplew.writeInt(chargeTime);
        mplew.writeInt(range);
        mplew.write(attack.size());
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(attackCount);
        mplew.write(0);
        mplew.write(0);
        for (AttackPair p : attack) {
            mplew.writeInt(p.objectid);
            mplew.writeInt(0);
            mplew.writePos(p.point);
            mplew.write(0);
            mplew.writeInt(0);
            for (Pair<Long, Boolean> atk : p.attack) {
                mplew.writeLong(atk.left);
                mplew.writeInt(0);
                mplew.write(atk.right);
                mplew.writeShort(0);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] getPVPMist(int cid, int mistSkill, int mistLevel, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserDotByUser.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(mistSkill);
        mplew.write(mistLevel);
        mplew.writeInt(damage);
        mplew.write(8);
        mplew.writeInt(1000);

        return mplew.getPacket();
    }

    public static byte[] pvpCool(int cid, List<Integer> attack) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserResetAllDot.getValue());
        mplew.writeInt(cid);
        mplew.write(attack.size());
        for (Iterator i$ = attack.iterator(); i$.hasNext();) {
            int b = ((Integer) i$.next());
            mplew.writeInt(b);
        }

        return mplew.getPacket();
    }

    public static byte[] summonTeam(MapleSummon summon1, MapleSummon summon2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SUMMON_TEAM.getValue());
        mplew.writeInt(summon1.getOwnerId());
        mplew.writeShort(20); // 20
        mplew.writeInt(summon1.getObjectId());
        mplew.writeInt(summon2.getObjectId());
        mplew.writeShort(summon1.getPosition().x);
        mplew.writeShort(summon2.getPosition().x);
        mplew.writeShort(0);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] followEffect(int initiator, int replier, Point toMap) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserFollowCharacter.getValue());
        mplew.writeInt(initiator);
        mplew.writeInt(replier);
        mplew.writeLong(0);
        if (replier == 0) {
            mplew.write(toMap == null ? 0 : 1);
            if (toMap != null) {
                mplew.writeInt(toMap.x);
                mplew.writeInt(toMap.y);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] showPQReward(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserShowPQReward.getValue());
        mplew.writeInt(cid);
        for (int i = 0; i < 6; i++) {
            mplew.write(0);
        }

        return mplew.getPacket();
    }

    public static byte[] craftMake(int cid, int something, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserSetOneTimeAction.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(something);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] craftFinished(int cid, int craftID, int ranking, int itemId, int quantity, int exp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserMakingSkillResult.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(craftID);
        mplew.writeInt(ranking);
        if (ranking == 25 || ranking == 26 || ranking == 27) {
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
        }
        mplew.writeInt(exp);

        return mplew.getPacket();
    }

    public static byte[] harvestResult(int cid, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserGatherResult.getValue());
        mplew.writeInt(cid);
        mplew.write(success ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] playerDamaged(int cid, int dmg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserHitByCounter.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(dmg);

        return mplew.getPacket();
    }

    public static byte[] showPyramidEffect(int chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PyramidLethalAttack.getValue());
        mplew.writeInt(chr);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] pamsSongEffect(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_UserSetDamageSkin_Premium.getValue());
        mplew.writeInt(cid);
        return mplew.getPacket();
    }

    public static byte[] hakuChangeEffect(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FoxManShowChangeEffect.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] figureHakuRemove(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FoxManLeaveField.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] hakuUseBuff(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FoxManExclResult.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] foxManModified(int cid, int equip) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FoxManModified.getValue());
        mplew.writeInt(cid);
        mplew.write(equip > 0 ? 1 : 0); // flag
        if (equip > 0) {
            // m_anFoxManEquip[0]
            mplew.writeInt(equip);
        }

        return mplew.getPacket();
    }

    public static byte[] spawnFigureHaku(MapleHaku haku) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FoxManEnterField.getValue());
        mplew.writeInt(haku.getOwner());
        mplew.writeShort(0); // m_pInfo
        mplew.writePos(haku.getPosition()); // m_ptPos.x, m_ptPos.y
        mplew.write(haku.getStance()); // m_nMoveAction
        mplew.writeShort(0); // pfh
        mplew.writeInt(0); // m_nUpgrade
        mplew.writeInt(haku.getWeapon()); // m_anFoxManEquip[0]

        return mplew.getPacket();
    }

    public static byte[] hakuChange(MapleHaku haku) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SkillPetState.getValue());
        mplew.writeInt(haku.getOwner());
        mplew.writeInt(haku.getObjectId());
        mplew.write(haku.isFigureHaku() ? 2 : 1);

        return mplew.getPacket();
    }

    public static byte[] hakuUnk(int cid, int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SkillPetState.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.write(0);
        mplew.writeMapleAsciiString("lol");

        return mplew.getPacket();
    }

    public static byte[] spawnHaku(MapleHaku haku) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SkillPetTransferField.getValue());
        mplew.writeInt(haku.getOwner());
        mplew.writeInt(haku.getObjectId());
        mplew.writeInt(40020109);
        mplew.write(haku.isFigureHaku() ? 2 : 1);
        mplew.writePos(haku.getPosition());
        mplew.write(0);
        mplew.writeShort(haku.getStance());

        return mplew.getPacket();
    }

    public static byte[] moveHaku(int cid, int oid, int duration, Point startPos, Point endPos,
            List<LifeMovementFragment> res) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SkillPetMove.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        PacketHelper.serializeMovementList(mplew, duration, startPos, endPos, res);

        return mplew.getPacket();
    }

    public static byte[] spawnDragon(MapleDragon d) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DragonEnterField.getValue());
        mplew.writeInt(d.getOwner());
        mplew.writeInt(d.getPosition().x);
        mplew.writeInt(d.getPosition().y);
        mplew.write(d.getStance());
        mplew.writeShort(0);
        mplew.writeShort(d.getJobId());

        return mplew.getPacket();
    }

    public static byte[] removeDragon(int chrid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DragonLeaveField.getValue());
        mplew.writeInt(chrid);

        return mplew.getPacket();
    }

    public static byte[] moveDragon(MapleDragon d, int duration, Point mPos, Point oPos,
            List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DragonMove.getValue());
        mplew.writeInt(d.getOwner());
        PacketHelper.serializeMovementList(mplew, duration, mPos, oPos, moves);

        return mplew.getPacket();
    }

    public static byte[] spawnAndroid(MapleCharacter cid, MapleAndroid android) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AndroidEnterField.getValue());
        mplew.writeInt(cid.getId());
        mplew.write(android.getType());
        mplew.writePos(android.getPos());
        mplew.write(android.getStance());
        mplew.writeShort(0);
        mplew.writeShort(android.getSkin() >= 2000 ? android.getSkin() - 2000 : android.getSkin());
        mplew.writeShort(android.getHair() - 30000);
        mplew.writeShort(android.getFace() - 20000);
        mplew.writeMapleAsciiString(android.getName());
        mplew.writeInt(0); // 大於0為使用透明耳飾→透明機器人耳飾感應氣(2892000)
        mplew.writeLong(DateUtil.getFileTimestamp(-2)); // 機器人商店使用券(2436755) 時間
        for (short i = -1200; i > -1208; i--) {
            Equip eq = (Equip) cid.getInventory(MapleInventoryType.EQUIPPED).getItem(i);
            mplew.writeInt(eq != null ? eq.getItemId() : 0);
            mplew.writeInt(eq != null ? eq.getFusionAnvil() : 0);
        }

        return mplew.getPacket();
    }

    public static byte[] moveAndroid(int cid, int duration, Point mPos, Point oPos, List<LifeMovementFragment> res) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AndroidMove.getValue());
        mplew.writeInt(cid);
        PacketHelper.serializeMovementList(mplew, duration, mPos, oPos, res);

        return mplew.getPacket();
    }

    public static byte[] showAndroidEmotion(int cid, byte emo1, byte emo2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // Packet: 97 DB 00 00 04 E7 FD C4 FF 05 00 00 03 00 3A 1C 52 04 07 00 41 6E 64
        // 72 6F 69 64 85 4D 0F 00 00 00 00 00 00 00 00 00 BF 09 10
        // and more 63 zero bytes
        mplew.writeShort(SendPacketOpcode.LP_AndroidActionSet.getValue());
        mplew.writeInt(cid);
        mplew.write(emo1);
        mplew.write(emo2);

        return mplew.getPacket();
    }

    public static byte[] updateAndroidEquip(MapleCharacter chr, short position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AndroidModified.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1 << (Math.abs(position) - 1200));
        Equip eq = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(position);
        mplew.writeInt(eq != null ? eq.getItemId() : 0);
        mplew.writeInt(eq != null ? eq.getFusionAnvil() : 0);
        // mplew.write(0);//178+ 作用未知
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] updateAndroidLook(MapleCharacter cid, MapleAndroid android) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AndroidModified.getValue());
        mplew.writeInt(cid.getId());
        mplew.write(0);
        mplew.writeShort(android.getSkin() >= 2000 ? android.getSkin() - 2000 : android.getSkin());
        mplew.writeShort(android.getHair() - 30000);
        mplew.writeShort(android.getFace() - 20000);
        mplew.writeMapleAsciiString(android.getName());

        return mplew.getPacket();
    }

    public static byte[] deactivateAndroid(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AndroidLeaveField.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] removeAndroidHeart() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Message.getValue());
        mplew.write(MessageOpcode.MS_AndroidMachineHeartAlertMessage.getValue());

        return mplew.getPacket();
    }

    public static byte[] movePlayer(int cid, int duration, Point mPos, Point oPos, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserMove.getValue());
        mplew.writeInt(cid);
        PacketHelper.serializeMovementList(mplew, duration, mPos, oPos, moves);

        return mplew.getPacket();
    }

    public static byte[] closeRangeAttack(MapleCharacter chr, int skilllevel, int itemId, AttackInfo attackInfo,
            boolean hasMoonBuff) {
        return addAttackInfo(SendPacketOpcode.LP_UserMeleeAttack, chr, skilllevel, itemId, attackInfo, hasMoonBuff);
    }

    public static byte[] rangedAttack(MapleCharacter chr, int skilllevel, int itemId, AttackInfo attackInfo) {
        return addAttackInfo(SendPacketOpcode.LP_UserShootAttack, chr, skilllevel, itemId, attackInfo, false);
    }

    public static byte[] magicAttack(MapleCharacter chr, int skilllevel, int itemId, AttackInfo attackInfo) {
        return addAttackInfo(SendPacketOpcode.LP_UserMagicAttack, chr, skilllevel, itemId, attackInfo, false);
    }

    public static byte[] passiveAttack(MapleCharacter chr, int skilllevel, int itemId, AttackInfo attackInfo,
            boolean hasMoonBuff) {
        return addAttackInfo(SendPacketOpcode.LP_UserBodyAttack, chr, skilllevel, itemId, attackInfo, hasMoonBuff);
    }

    private static byte[] addAttackInfo(SendPacketOpcode op, MapleCharacter chr, int skilllevel, int itemId,
            AttackInfo attackInfo, boolean hasMoonBuff) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(op.getValue());

        mplew.writeInt(chr.getId());

        addAttackBody(mplew, op, chr, skilllevel, itemId, attackInfo, hasMoonBuff);

        return mplew.getPacket();
    }

    private static void addAttackBody(MaplePacketLittleEndianWriter mplew, SendPacketOpcode op, MapleCharacter chr,
            int skilllevel, int itemId, AttackInfo attackInfo, boolean hasMoonBuff) {
        mplew.write(op == SendPacketOpcode.LP_UserShootAttack);
        mplew.write(attackInfo.tbyte);
        mplew.write(chr.getLevel());
        skilllevel = attackInfo.skill > 0 ? skilllevel : 0;
        mplew.write(skilllevel);
        if (skilllevel > 0) {
            mplew.writeInt(attackInfo.skill);
        }

        int job = SkillConstants.getJobBySkill(attackInfo.skill);
        if (MapleJob.is神之子(job)) {
            short zero1 = 0;
            short zero2 = 0;
            mplew.write(zero1 > 0 || zero2 > 0);
            if (zero1 > 0 || zero2 > 0) {
                mplew.writeShort(zero1);
                mplew.writeShort(zero2);
                boolean v13 = false;
                if (v13) {
                    addZeroAttackBody(mplew, chr, skilllevel, itemId, attackInfo);
                }
                return;
            }
        }

        if (attackInfo.skill != 101110104) { // 進階旋風(吸入)

            if (SkillConstants.getHyperAddBullet(attackInfo.skill) > 0
                    || SkillConstants.getHyperAddAttack(attackInfo.skill) > 0) {
                byte v18 = 0;
                mplew.write(v18);
                if (v18 > 0) {
                    mplew.writeInt(0);
                }
            }

            if (attackInfo.skill == 80001850) {
                byte v19 = 0;
                mplew.write(v19);
                if (v19 > 0) {
                    mplew.writeInt(0);
                }
            }

            if (SkillConstants.is紫扇仰波(attackInfo.skill) || SkillConstants.is初心者紫扇仰波(attackInfo.skill)) {
                byte v20 = 0;
                mplew.write(v20);
                if (v20 > 0) {
                    mplew.writeInt(0);
                }
            }

            mplew.write(0);
            mplew.write(attackInfo.direction);
            mplew.writeInt(0);
            if ((attackInfo.direction & 2) != 0) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
            int unk = 1;
            if ((attackInfo.direction & 8) != 0) {
                mplew.write(unk);
            }
            mplew.writeShort(attackInfo.display);
            if ((attackInfo.display & 0x7FFF) <= 0x58E) {
                mplew.write(attackInfo.speed);
                mplew.write(chr.getStat().passive_mastery());
                mplew.writeInt(itemId > 0 ? itemId : attackInfo.charge);
                boolean v19 = unk == 0;
                boolean v42 = unk < 0;
                if (!(v19 | v42)) {
                    for (AttackPair oned : attackInfo.allDamage) {
                        if (oned.attack != null) {
                            mplew.writeInt(oned.objectid);
                            if (oned.objectid > 0) {
                                mplew.write(7);
                                mplew.write(0); // boolean
                                mplew.write(0); // boolean
                                mplew.writeShort(0);
                                // 紅玉咒印 || 紅玉咒印
                                if (attackInfo.skill == 42111002 || attackInfo.skill == 80011050) {
                                    mplew.write(oned.attack.size());
                                    for (Pair<Long, Boolean> eachd : oned.attack) {
                                        mplew.writeLong(eachd.left + (!eachd.right ? 0 : Long.MIN_VALUE));
                                    }
                                } else {
                                    for (Pair<Long, Boolean> eachd : oned.attack) {
                                        mplew.writeLong(eachd.left + (!eachd.right ? 0 : Long.MIN_VALUE));
                                    }
                                    if (attackInfo.skill == 142100010 // 心靈領域
                                            || attackInfo.skill == 142110003 // 猛烈心靈
                                            || attackInfo.skill == 142110015 // 猛烈心靈2
                                            || attackInfo.skill == 142111002 // 擷取心靈
                                            || attackInfo.skill == 142111002
                                            || attackInfo.skill > 142119999 && (attackInfo.skill <= 142120002
                                            || attackInfo.skill == 142120014)) {// 終極技 - 心靈射擊 || 猛烈心靈2
                                        mplew.writeInt(0);
                                    }
                                }
                            }
                        }
                    }
                }
                // 核爆術 || 雷霆萬鈞 || 黃泉十字架 || 龍氣息
                if (attackInfo.skill == 2321001 || attackInfo.skill == 2221052 || attackInfo.skill == 11121052
                        || attackInfo.skill == 12121054) {
                    mplew.writeInt(0);
                } else if (SkillConstants.sub_9F5282(attackInfo.skill) || SkillConstants.sub_9F529C(attackInfo.skill)
                        || attackInfo.skill == 101000202 // 暗影降臨(劍氣)
                        || attackInfo.skill == 101000102 // 進階威力震擊(衝擊波)
                        || attackInfo.skill == 80001762) { // 解放雷之輪
                    mplew.writeInt(attackInfo.position.x);
                    mplew.writeInt(attackInfo.position.y);
                }
                if (SkillConstants.isKeyDownSkillWithPos(attackInfo.skill)) {
                    mplew.writePos(attackInfo.skillposition);
                }
                if (attackInfo.skill == 51121009) { // 閃光交叉
                    mplew.write(0);
                }
                if (attackInfo.skill == 112110003) { // 隊伍攻擊
                    mplew.writeInt(0);
                }
                if (attackInfo.skill == 42100007) { // 御身消滅
                    mplew.writeShort(0);
                    int size = 0;
                    mplew.write(size);
                    for (int i = 0; i < size; i++) {
                        mplew.writeShort(0);
                        mplew.writeShort(0);
                    }
                }
            }
        }

        // 增加空數據以防止未更新包導致38 Error的問題
        mplew.writeZeroBytes(100);
    }

    private static void addZeroAttackBody(MaplePacketLittleEndianWriter mplew, MapleCharacter chr,
            int skilllevel, int itemId, AttackInfo attackInfo) {
        skilllevel = attackInfo.skill > 0 ? skilllevel : 0;
        mplew.write(skilllevel);
        if (skilllevel > 0) {
            mplew.writeInt(attackInfo.skill);
        }

        mplew.write(0);
        mplew.write(attackInfo.direction);
        mplew.writeInt(0);
        if ((attackInfo.direction & 2) != 0) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if ((attackInfo.direction & 8) != 0) {
            mplew.write(0);
        }

        mplew.writeShort(attackInfo.display);
        if ((attackInfo.display & 0x7FFF) < 0x532) {
            mplew.write(attackInfo.speed);
            mplew.write(chr.getStat().passive_mastery());
            mplew.writeInt(itemId > 0 ? itemId : attackInfo.charge);
            for (AttackPair oned : attackInfo.allDamage) {
                if (oned.attack != null) {
                    mplew.writeInt(oned.objectid);
                    if (oned.objectid > 0) {
                        mplew.write(7);
                        mplew.write(0); // boolean
                        mplew.write(0); // boolean
                        mplew.writeShort(0);
                        for (Pair<Long, Boolean> eachd : oned.attack) {
                            mplew.writeLong(eachd.left + (!eachd.right ? 0 : Long.MIN_VALUE));
                        }
                    }
                }
            }
            if (SkillConstants.sub_9F5282(attackInfo.skill) || SkillConstants.sub_9F529C(attackInfo.skill)
                    || attackInfo.skill == 101000102) { // 進階威力震擊(衝擊波)
                mplew.writeInt(attackInfo.position.x);
                mplew.writeInt(attackInfo.position.y);
            }
            if (SkillConstants.isKeyDownSkillWithPos(attackInfo.skill)) {
                mplew.writePos(attackInfo.skillposition);
            }
        }
    }

    public static byte[] skillEffect(int cid, int skillId, byte level, short display, byte direction, byte speed,
            Point position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserSkillPrepare.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(skillId);
        mplew.write(level);
        mplew.write(display);
        mplew.write(direction);
        mplew.write(speed);
        if (position != null) {
            mplew.writePos(position);
        }

        return mplew.getPacket();
    }

    public static byte[] skillMoveAttack(MapleCharacter from, int skillId, short display, byte unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserMovingShootAttackPrepare.getValue());
        mplew.writeInt(from.getId());
        mplew.write(0);
        mplew.write(1);
        mplew.writeInt(skillId);
        mplew.writeShort(display);
        mplew.write(unk);
        if (skillId == 33121009 || skillId == 33121214) {
            mplew.writePos(from.getPosition()); // Position
        }

        return mplew.getPacket();
    }

    public static byte[] skillCancel(MapleCharacter from, int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserSkillCancel.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);

        return mplew.getPacket();
    }

    public static byte[] damagePlayer(int cid, int type, int damage, int monsteridfrom, byte direction, int skillid,
            int pDMG, boolean pPhysical, int pID, byte pType, Point pPos, byte offset, int offset_d, int fake) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserHit.getValue());
        mplew.writeInt(cid);
        mplew.write(type);
        mplew.writeInt(damage);
        mplew.write(0);
        mplew.write(0);
        if (type < -1) {
            if (type == -8) {
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        } else {
            mplew.writeInt(monsteridfrom);
            mplew.write(direction);
            mplew.writeInt(skillid);

            mplew.writeInt(0);
            mplew.writeInt(pDMG);
            mplew.write(0);
            if (pDMG > 0) {
                mplew.write(pPhysical ? 1 : 0);
                mplew.writeInt(pID);
                mplew.write(pType);
                mplew.writePos(pPos);
            }
            mplew.write(offset);
            if ((offset & 1) != 0) {
                mplew.writeInt(offset_d);
            }
        }
        mplew.writeInt(damage);
        if (damage == -1) {
            mplew.writeInt(fake);
        }

        return mplew.getPacket();
    }

    public static byte[] facialExpression(MapleCharacter from, int expression) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserEmotion.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        mplew.writeInt(-1);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] itemEffect(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_UserSetActiveEffectItem.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        mplew.writeInt(-1); // not sure, added in v146.1
        mplew.write(0);
        System.out.println("Item Effect:\r\nCharacter ID: " + characterid + "\r\nItem ID: " + itemid);
        return mplew.getPacket();
    }

    public static byte[] showTitle(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserSetActiveNickItem.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] showAngelicBuster(int characterid, int tempid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserSetDefaultWingItem.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(tempid);

        return mplew.getPacket();
    }

    public static byte[] showChair(int characterid, int itemid, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserSetActivePortableChair.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);
        mplew.writeInt(text.length() > 0 ? 1 : 0);
        if (text.length() != 0 && (itemid / 1000 == 3014)) {
            mplew.writeMapleAsciiString(text);
        }
        int unkSize = 0;
        mplew.writeInt(unkSize);
        for (int i = 0; i < unkSize; ++i) {
            mplew.writeInt(0);
        }
        boolean unkBool = false;
        mplew.write(unkBool);
        if (unkBool) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.writeInt(0);
        boolean unkBool2 = false;
        mplew.write(unkBool2);
        if (unkBool) {
            // func
        }
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] updateCharLook(MapleCharacter chr, boolean second) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserAvatarModified.getValue());
        mplew.writeInt(chr.getId());
        int unk = 1;
        mplew.write(unk);
        if ((unk & 1) != 0) {
            PacketHelper.AvatarLook__Decode(mplew, chr, false, second);
        }
        if ((unk & 8) != 0) {
            PacketHelper.AvatarLook__Decode(mplew, chr, false, second);
        }
        PacketHelper.UnkFunctin6(mplew);
        if ((unk & 2) != 0) {
            mplew.write(0);
        }
        if ((unk & 4) != 0) {
            mplew.write(0);
        }
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        List<MapleRing> allrings = rings.getLeft();
        allrings.addAll(rings.getMid());
        addRingInfo(mplew, allrings);
        addRingInfo(mplew, allrings);
        addMRingInfo(mplew, rings.getRight(), chr);
        mplew.writeInt(0); // -> charid to follow (4)
        mplew.writeInt(0x0F);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] updatePartyMemberHP(int cid, int curhp, int maxhp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserHP.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);

        return mplew.getPacket();
    }

    public static byte[] loadGuildName(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserGuildNameChanged.getValue());
        mplew.writeInt(chr.getId());
        if (chr.getGuildId() <= 0) {
            mplew.writeShort(0);
        } else {
            MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
            } else {
                mplew.writeShort(0);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] loadGuildIcon(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserGuildMarkChanged.getValue());
        mplew.writeInt(chr.getId());
        if (chr.getGuildId() <= 0) {
            mplew.writeZeroBytes(6);
        } else {
            MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
            } else {
                mplew.writeZeroBytes(6);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] changeTeam(int cid, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserPvPTeamChanged.getValue());
        mplew.writeInt(cid);
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] onGatherActionSet(int cid, int tool) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_GatherActionSet.getValue());
        mplew.writeInt(cid);
        if (tool > 0) {
            mplew.write(1);
            mplew.write(0);
            mplew.writeShort(0);
            mplew.writeInt(tool);
            mplew.writeZeroBytes(30);
        } else {
            mplew.write(0);
            mplew.writeZeroBytes(33);
        }

        return mplew.getPacket();
    }

    public static byte[] getPVPHPBar(int cid, int hp, int maxHp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UpdatePvPHPTag.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(hp);
        mplew.writeInt(maxHp);

        return mplew.getPacket();
    }

    public static byte[] cancelChair(int id, int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserSitResult.getValue());
        if (id == -1) {
            mplew.writeInt(cid);
            mplew.write(0);
        } else {
            mplew.writeInt(cid);
            mplew.write(1);
            mplew.writeShort(id);
        }

        return mplew.getPacket();
    }

    public static byte[] instantMapWarp(byte portal) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserTeleport.getValue());
        mplew.write(0);
        mplew.write(portal);
        mplew.writeZeroBytes(8);

        return mplew.getPacket();
    }

    public static byte[] CurentMapWarp(Point pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserTeleport.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.writeInt(6850036);
        mplew.writePos(pos);

        return mplew.getPacket();
    }

    public static byte[] updateQuestInfo(int quest, int npc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserQuestResult.getValue());
        mplew.write(0x0B);
        mplew.writeInt(quest);
        mplew.writeInt(npc);
        mplew.writeInt(0);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] updateQuestFinish(int quest, int npc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserQuestResult.getValue());
        mplew.write(0x0B);
        mplew.writeInt(quest);
        mplew.writeInt(npc);
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] sendHint(String hint, int width, int height) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserBalloonMsg.getValue());
        mplew.writeMapleAsciiString(hint);
        mplew.writeShort(width < 1 ? Math.max(hint.length() * 10, 40) : width);
        mplew.writeShort(Math.max(height, 5));
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] updateCombo(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ModCombo.getValue());
        mplew.writeInt(value);

        return mplew.getPacket();
    }

    public static byte[] rechargeCombo(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_IncComboByComboRecharge.getValue());
        mplew.writeInt(value);

        return mplew.getPacket();
    }

    public static byte[] getFollowMessage(String msg) {
        return getGameMessage(11, msg);
    }

    public static byte[] getGameMessage(int colour, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserChatMsg.getValue());
        mplew.writeShort(colour);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] getBuffZoneEffect(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserBuffzoneEffect.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] getTimeBombAttack() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserTimeBombAttack.getValue());
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(10);
        mplew.writeInt(6);

        return mplew.getPacket();
    }

    public static byte[] moveFollow(int duration, Point otherStart, Point myStart, Point otherEnd,
            List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserPassiveMove.getValue());
        PacketHelper.serializeMovementList(mplew, duration, otherStart, myStart, moves);
        mplew.write(17);
        for (int i = 0; i < 8; i++) {
            mplew.write(0);
        }

        // if (true) {
        // byte[] unk = new byte[17];
        // byte unkVal = (byte) unk.length;
        // byte tempVal = 0;
        // mplew.write(unkVal);
        // for (int i = 0 ; i < unkVal ; unkVal = (byte) (tempVal & 0xF)) {
        // boolean bool = (i & 0x80000001) == 0;
        // if ((i & 0x80000001) < 0) {
        // bool = (((i & 0x80000001) - 1) | 0xFFFFFFFE) == -1;
        // }
        // if (bool) {
        // tempVal = unk[i];
        // mplew.write(tempVal);
        // } else {
        // tempVal >>= 4;
        // }
        // unkVal = sub_73E15C((v9 + 60), 0xFFFFFFFFu);
        // ++i;
        // }
        // }
        mplew.write(0);
        mplew.writePos(otherEnd);
        mplew.writePos(otherStart);
        mplew.writeZeroBytes(100);

        return mplew.getPacket();
    }

    public static byte[] getFollowMsg(int opcode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserFollowCharacterFailed.getValue());
        mplew.writeLong(opcode);

        return mplew.getPacket();
    }

    public static byte[] registerFamiliar(MonsterFamiliar mf) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // mplew.writeShort(SendPacketOpcode.REGISTER_FAMILIAR.getValue());
        // mplew.writeLong(mf.getId());
        // mf.writeRegisterPacket(mplew, false);
        // mplew.writeShort(mf.getVitality() >= 3 ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] createUltimate(int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CreateNewCharacterResult_PremiumAdventurer.getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] onGatherRequestResult(int oid, int msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_GatherRequestResult.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(msg);

        return mplew.getPacket();
    }

    public static byte[] openBag(int index, int itemId, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserBagItemUseResult.getValue());
        mplew.writeInt(index);
        mplew.writeInt(itemId);
        mplew.writeShort(firstTime ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] dragonBlink(int portalId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_RandomTeleportKey.getValue());
        mplew.write(portalId);

        return mplew.getPacket();
    }

    public static byte[] getPVPIceGage(int score) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetGagePoint.getValue());
        mplew.writeInt(score);

        return mplew.getPacket();
    }

    public static byte[] hayatoJianQi(short value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HAYATO_JIAN_QI.getValue());
        mplew.writeShort(value);

        return mplew.getPacket();
    }

    public static byte[] spawnObtacleAtomBomb(List<ObtacleAtom> bombs) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ObtacleAtomCreate.getValue());
        // Number of bomb objects to spawn. You can also just send multiple packets
        // instead of putting them all in one packet.
        mplew.writeInt(bombs.size());
        // Unknown, this part is from IDA.
        byte unk = 0;
        mplew.write(unk); // animation data or some shit
        if (unk == 1) {
            mplew.writeInt(300); // from Effect.img/BasicEff/ObtacleAtomCreate/%d
            mplew.write(0); // rest idk
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        for (ObtacleAtom bomb : bombs) {
            mplew.write(1);
            // Bomb type. Determines the graphics used as well as the projectile's hitbox.
            // Pointer to an entry in Effect.wz/BasicEff.img/ObtacleAtom/
            mplew.writeInt(bomb.getType());
            // This must be unique for every bomb you spawn on the map. Give it an ObjectID,
            // or something.
            mplew.writeInt(bomb.getUniqueID());
            // Spawnpoint, X origin.
            mplew.writeInt(bomb.getPosition().x);
            // Spawnpoint, Y origin.
            mplew.writeInt(bomb.getPosition().y);
            // Maximum movement speed. Roughly 2 * pixels per second.
            mplew.writeInt(bomb.getMaxSpeed());
            // Acceleration. Always below 5 in GMS, unsure exactly how it's calculated.
            // Setting this to 0 makes a permanent, stationary bomb.
            mplew.writeInt(bomb.getAcceleration());
            // No idea, set it to 0. If you find out what this does, please let me know.
            mplew.writeInt(bomb.getUnk());
            // Affects exploding, the higher the number, the quicker it explodes. 25 is the
            // value GMS uses.
            mplew.writeInt(bomb.getExplodeSpeed());
            // Percent of the character's Max HP to deal as damage. You can set this to
            // negative values, which will heal the player.
            // Damage dealt by projectiles ignores all defenses, resistances, and evasion.
            // Your source must support damage type -5, which is what these projectiles use.
            mplew.writeInt(bomb.getDamagePercent());
            // Time, in milliseconds, to wait until actually spawning the projectile.
            mplew.writeInt(bomb.getSpawnDelay());
            // The maximum distance the projectile will move before exploding. Measured in
            // pixels.
            mplew.writeInt(bomb.getDistance());
            // Direction. Behaves oddly; from 0 upwards, the angle goes clock wise, until it
            // hits 80, then you add 80 and it'll continue going clockwise, until it hits
            // 240, add 80 and it'll continue to go clockwise until 360 is hit then it
            // starts over.
            // Varies among different projectile types.
            mplew.writeInt(bomb.getAngle());
        }

        return mplew.getPacket();
    }

    public static byte[] getAggroRankInfo(List<String> names) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AggroRankInfo.getValue());
        mplew.writeInt(names.size());
        for (String name : names) {
            mplew.writeMapleAsciiString(name);
        }

        return mplew.getPacket();
    }

    public static byte[] getDeathCountInfo(boolean individual, int count) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(individual ? SendPacketOpcode.LP_IndividualDeathCountInfo.getValue()
                : SendPacketOpcode.LP_DeathCountInfo.getValue());
        mplew.writeInt(count);

        return mplew.getPacket();
    }

    public static byte[] skillCooldown(int sid, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SkillCooltimeSet.getValue());
        mplew.writeInt(1);
        mplew.writeInt(sid);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] spawnSpecial(int skillid, int x1, int y1, int x2, int y2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetSlownDown.getValue());
        mplew.writeInt(3);
        mplew.writeInt(skillid);
        mplew.writeInt(0);
        mplew.writeInt(x1);
        mplew.writeInt(y1);
        mplew.writeInt(x2);
        mplew.writeInt(y2);

        return mplew.getPacket();
    }

    public static byte[] showFusionAnvil(int itemId, int giveItemId, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CashLookChangeResult.getValue());
        mplew.write(success ? 1 : 0);
        mplew.writeInt(itemId);
        mplew.writeInt(giveItemId);

        return mplew.getPacket();
    }

    public static byte[] dropItemFromMapObject(MapleMapItem drop, Point dropfrom, Point dropto, byte nDropType) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DropEnterField.getValue());

        mplew.write(0); // eDropType
        mplew.write(nDropType);
        mplew.writeInt(drop.getObjectId()); // bNoMove

        // if (地圖上不存在OID為drop.getObjectId()掉寶)
        boolean bIsMoney = drop.getMeso() > 0;
        mplew.write(bIsMoney); // bIsMoney
        mplew.writeInt(0); // nDropMotionType
        mplew.writeInt(0/* Randomizer.nextInt(255) */); // nDropSpeed
        mplew.writeInt(0); // IDA -> thisInt / 100.0 // fRand
        mplew.writeInt(drop.getItemId()); // nInfo
        mplew.writeInt(drop.getOwner()); // dwOwnerID
        mplew.write(drop.getDropType()); // nOwnType
        mplew.writePos(dropto); // ptDrop
        mplew.writeInt(0); // dwSourceID
        mplew.writeInt(0);
        mplew.writeLong(0);
        mplew.writeInt(0);
        // sub_609840{
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeLong(0);
        // }
        mplew.write(0);
        mplew.write(false);
        mplew.writeInt(0);
        boolean bFakeMoney = true;
        if (!bIsMoney || drop.getItemId() != 0) {
            bFakeMoney = false;
        }
        if (nDropType == 0 || nDropType == 1 || nDropType == 3 || nDropType == 4) {
            mplew.writePos(dropfrom); // tempPoint
            mplew.writeInt(0); // tDelay
        }
        mplew.write(false); // bExplosiveDrop

        if (!bFakeMoney || (drop.getItemId() != 2910000 && drop.getItemId() != 2910001)) {
            if (drop.getItemId() != 2910000 && drop.getItemId() != 2910001 && !bIsMoney) {
                // m_dateExpire
                PacketHelper.addExpirationTime(mplew, drop.getItem().getExpiration());
            }
            mplew.write(drop.isPlayerDrop() ? 0 : 1); // bByPet
            mplew.write(false);
            mplew.writeShort(0); // nFallingVY
            mplew.write(false); // bFadeInEffect
            // mplew.write(0); // nMakeType

            // bCollisionPickUp
            switch (drop.getItemId()) {
                case 2023484: // 連續擊殺模式
                case 2023494: // 連續擊殺模式
                case 2023495: // 連續擊殺模式
                    // case 2023496: // 獲得楓幣量2倍！
                    // case 2023497: // 移動速度兩倍！
                    // case 2023498: // Buff
                    mplew.writeInt(1);
                    break;
                default:
                    mplew.writeInt(0);
            }
            mplew.write(drop.getState()); // 潛能等級特效 nItemGrade
            mplew.write(0); // bPrepareCollisionPickUp
        }

        return mplew.getPacket();
    }

    public static byte[] explodeDrop(int oid) {
        return removeItemFromMap(oid, 4, 0, 0);
    }

    public static byte[] removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, 0);
    }

    public static byte[] removeItemFromMap(int oid, int nState, int cid, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DropLeaveField.getValue());

        mplew.write(nState);
        mplew.writeInt(oid); // pLayer.m_pInterface

        if (nState >= 2) {
            switch (nState) {
                case 2:
                case 3:
                case 5:
                    // pr.p->dwPickupID
                    mplew.writeInt(cid);
                    break;
                case 4:
                    // pLayer.m_pInterface
                    mplew.writeShort(655);
                    break;
            }
        }
        if (nState <= 10001) {
            if (nState == 10001) {
                mplew.writeInt(slot);
            } else {
                switch (nState) {
                    case 5:
                        mplew.writeInt(slot);
                        break;
                }
            }
        }

        return mplew.getPacket();
    }

    public static byte[] spawnAffectedArea(MapleAffectedArea mist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AffectedAreaCreated.getValue());
        mplew.writeInt(mist.getObjectId());
        mplew.write(mist.getMistType());
        mplew.writeInt(mist.getOwnerId());
        int skillId = mist.isMobMist() ? mist.getMobSkill().getSkillId() : mist.getSourceSkill().getId();
        mplew.writeInt(skillId);
        mplew.write(mist.getSkillLevel());
        mplew.writeShort(mist.getSkillDelay());
        mplew.writeRect(mist.getBox());
        mplew.writeInt(mist.isShelter() ? 1 : 0);
        mplew.writePos(mist.getPosition());
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeBoolean(false);
        mplew.writeInt(0);
        switch (skillId) {
            case 4121015:
            case 51120057:
            case 33111013: // 連弩陷阱
            case 33121012:
            case 33121016: // 鑽孔集裝箱
            case 35121052: // 扭曲領域
            case 131001107: // 博拉多利
            case 131001207: // 帕拉美
                mplew.write(!mist.isFaceLeft());
                break;
        }
        mplew.writeReversedInt(mist.isMobMist() ? mist.getMobSkill().getDuration() : mist.getSource().getDuration());

        return mplew.getPacket();
    }

    public static byte[] getMobSkillInstalledFire(int oid, List<Integer> unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobSkillInstalledFire.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(0);
        mplew.writeInt(unk.size());
        for (int mm : unk) {
            mplew.writeInt(mm);
        }

        return mplew.getPacket();
    }

    public static byte[] removeAffectedArea(int oid, boolean eruption) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AffectedAreaRemoved.getValue());
        mplew.writeInt(oid);
        mplew.write(eruption ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] spawnDoor(int oid, int skillId, Point pos, boolean animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_TownPortalCreated.getValue());
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(oid);
        mplew.writeInt(skillId);
        mplew.writePos(pos);

        return mplew.getPacket();
    }

    public static byte[] removeDoor(int oid, boolean animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_TownPortalRemoved.getValue());
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] spawnKiteError() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CreateMessgaeBoxFailed.getValue());

        return mplew.getPacket();
    }

    public static byte[] spawnKite(int oid, int itemId, String message, String name, Point pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MessageBoxEnterField.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(message);
        mplew.writeMapleAsciiString(name);
        mplew.writePos(pos);

        return mplew.getPacket();
    }

    public static byte[] destroyKite(int oid, boolean animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MessageBoxLeaveField.getValue());
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    // [8A 16 25 00] [03] [9D 07 7F 01] [06 01 00 06]
    public static byte[] triggerReactor(final MapleCharacter chr, MapleReactor reactor, int stance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ReactorChangeState.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getTruePosition());
        mplew.writeShort(stance);
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(chr == null ? 0 : chr.getId());

        return mplew.getPacket();
    }

    public static byte[] spawnReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ReactorEnterField.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getReactorId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getTruePosition());
        mplew.write(reactor.getFacingDirection());
        mplew.writeMapleAsciiString(reactor.getName());

        return mplew.getPacket();
    }

    public static byte[] destroyReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ReactorLeaveField.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getPosition());

        return mplew.getPacket();
    }

    public static byte[] makeDecomposer(int cid, String cname, Point pos, int timeLeft, int itemId, int fee) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DecomposerEnterField.getValue());
        mplew.writeInt(cid);
        mplew.writeMapleAsciiString(cname);
        mplew.writeInt(pos.x);
        mplew.writeInt(pos.y);
        mplew.writeShort(timeLeft);
        mplew.writeInt(itemId);
        mplew.writeInt(fee);

        return mplew.getPacket();
    }

    public static byte[] removeDecomposer(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DecomposerLeaveField.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(1);

        return mplew.getPacket();
    }

    public static byte[] rollSnowball(int type, MapleSnowball.MapleSnowballs ball1,
            MapleSnowball.MapleSnowballs ball2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SnowBallState.getValue());
        mplew.write(type);
        mplew.writeInt(ball1 == null ? 0 : ball1.getSnowmanHP() / 75);
        mplew.writeInt(ball2 == null ? 0 : ball2.getSnowmanHP() / 75);
        mplew.writeShort(ball1 == null ? 0 : ball1.getPosition());
        mplew.write(0);
        mplew.writeShort(ball2 == null ? 0 : ball2.getPosition());
        mplew.writeZeroBytes(11);

        return mplew.getPacket();
    }

    public static byte[] enterSnowBall() {
        return rollSnowball(0, null, null);
    }

    public static byte[] hitSnowBall(int team, int damage, int distance, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SnowBallHit.getValue());
        mplew.write(team);
        mplew.writeShort(damage);
        mplew.write(distance);
        mplew.write(delay);

        return mplew.getPacket();
    }

    public static byte[] snowballMessage(int team, int message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SnowBallMsg.getValue());
        mplew.write(team);
        mplew.writeInt(message);

        return mplew.getPacket();
    }

    public static byte[] leftKnockBack() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SnowBallTouch.getValue());

        return mplew.getPacket();
    }

    public static byte[] hitCoconut(boolean spawn, int id, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CoconutHit.getValue());
        mplew.writeShort(spawn ? 0x8000 : id);
        mplew.writeShort(0); // 延遲時間
        mplew.write(spawn ? 0 : type);

        return mplew.getPacket();
    }

    public static byte[] coconutScore(int[] coconutscore) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CoconutScore.getValue());
        mplew.writeShort(coconutscore[0]);
        mplew.writeShort(coconutscore[1]);

        return mplew.getPacket();
    }

    public static byte[] updateAriantScore(List<MapleCharacter> players) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ArenaScore.getValue());
        mplew.write(players.size());
        for (MapleCharacter i : players) {
            mplew.writeMapleAsciiString(i.getName());
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] sheepRanchInfo(byte wolf, byte sheep) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_BattlefieldEnter.getValue());
        mplew.write(wolf);
        mplew.write(sheep);

        return mplew.getPacket();
    }

    public static byte[] sheepRanchClothes(int cid, byte clothes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_BattlefieldScore.getValue());
        mplew.writeInt(cid);
        mplew.write(clothes);

        return mplew.getPacket();
    }

    public static byte[] updateWitchTowerKeys(int keys) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_BattlefieldTeamChanged.getValue());
        mplew.write(keys);

        return mplew.getPacket();
    }

    public static byte[] showChaosZakumShrine(boolean spawned, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAOS_ZAKUM_SHRINE.getValue());
        mplew.write(spawned ? 1 : 0);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] showChaosHorntailShrine(boolean spawned, int time) {
        return showHorntailShrine(spawned, time);
    }

    public static byte[] showHorntailShrine(boolean spawned, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HORNTAIL_SHRINE.getValue());
        mplew.write(spawned ? 1 : 0);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] getRPSMode(byte mode, int mesos, int selection, int answer) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_RPSGame.getValue());
        mplew.write(mode);
        switch (mode) {
            case 6:
                if (mesos == -1) {
                    break;
                }
                mplew.writeInt(mesos);
                break;
            case 8:
                mplew.writeInt(9000019);
                break;
            case 11:
                mplew.write(selection);
                mplew.write(answer);
        }

        return mplew.getPacket();
    }

    public static byte[] messengerInvite(String from, int messengerid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Messenger.getValue());
        mplew.write(3);
        mplew.writeMapleAsciiString(from);
        mplew.write(1);// channel?
        mplew.writeInt(messengerid);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Messenger.getValue());
        mplew.write(0);
        mplew.write(position);
        PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
        mplew.writeMapleAsciiString(from);
        mplew.write(channel);
        mplew.write(1); // v140
        mplew.writeInt(chr.getJob());

        return mplew.getPacket();
    }

    public static byte[] removeMessengerPlayer(int position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Messenger.getValue());
        mplew.write(2);
        mplew.write(position);

        return mplew.getPacket();
    }

    public static byte[] updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Messenger.getValue());
        mplew.write(0); // v140.
        mplew.write(position);
        PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
        mplew.writeMapleAsciiString(from);
        mplew.write(channel);
        mplew.write(0); // v140.
        mplew.writeInt(chr.getJob()); // doubt it's the job, lol. v140.

        return mplew.getPacket();
    }

    public static byte[] joinMessenger(int position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Messenger.getValue());
        mplew.write(1);
        mplew.write(position);

        return mplew.getPacket();
    }

    public static byte[] messengerChat(String charname, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Messenger.getValue());
        mplew.write(6);
        mplew.writeMapleAsciiString(charname);
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    public static byte[] messengerNote(String text, int mode, int mode2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Messenger.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(text);
        mplew.write(mode2);

        return mplew.getPacket();
    }

    public static byte[] messengerOpen(byte type, List<MapleCharacter> chars) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_LikePoint.getValue());
        mplew.write(type); // 7 in messenger open ui 8 new ui
        if (chars.isEmpty()) {
            mplew.writeShort(0);
        }
        for (MapleCharacter chr : chars) {
            mplew.write(1);
            mplew.writeInt(chr.getId());
            mplew.writeInt(0); // likes
            mplew.writeLong(0); // some time
            mplew.writeMapleAsciiString(chr.getName());
            PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
        }

        return mplew.getPacket();
    }

    public static byte[] messengerCharInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Messenger.getValue());
        mplew.write(0x0B);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeInt(chr.getJob());
        mplew.writeInt(chr.getFame());
        mplew.writeInt(0); // likes
        MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
        mplew.writeMapleAsciiString(gs != null ? gs.getName() : "-");
        MapleGuildAlliance alliance = World.Alliance.getAlliance(gs != null ? gs.getAllianceId() : 0);
        mplew.writeMapleAsciiString(alliance != null ? alliance.getName() : "");
        mplew.write(2);

        return mplew.getPacket();
    }

    public static byte[] removeFromPackageList(boolean remove, int Package) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Parcel.getValue());
        mplew.write(24);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);

        return mplew.getPacket();
    }

    public static byte[] sendPackageMSG(byte operation, List<MaplePackageActions> packages) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_Parcel.getValue());
        mplew.write(operation);

        switch (operation) {
            case 9:
                mplew.write(1);
                break;
            case 10:
                mplew.write(0);
                mplew.write(packages.size());

                for (MaplePackageActions dp : packages) {
                    mplew.writeInt(dp.getPackageId());
                    mplew.writeAsciiString(dp.getSender(), 13);
                    mplew.writeLong(dp.getMesos());
                    mplew.writeLong(DateUtil.getFileTimestamp(dp.getSentTime()));
                    mplew.writeZeroBytes(205);

                    if (dp.getItem() != null) {
                        mplew.write(1);
                        PacketHelper.GW_ItemSlotBase_Decode(mplew, dp.getItem());
                    } else {
                        mplew.write(0);
                    }
                }
                mplew.write(0);
        }

        return mplew.getPacket();
    }

    public static byte[] getKeymap(MapleKeyLayout layout, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FuncKeyMappedInit.getValue());
        layout.writeData(mplew, chr);

        return mplew.getPacket();
    }

    public static byte[] petAutoHP(int m_nPetConsumeItemID) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PetConsumeItemInit.getValue());
        mplew.writeInt(m_nPetConsumeItemID);

        return mplew.getPacket();
    }

    public static byte[] petAutoMP(int m_nPetConsumeMPItemID) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PetConsumeMPItemInit.getValue());
        mplew.writeInt(m_nPetConsumeMPItemID);

        return mplew.getPacket();
    }

    public static byte[] petAutoCure(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_AUTO_CURE.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] petAutoBuff(int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // mplew.writeShort(SendPacketOpcode.PET_AUTO_BUFF.getValue());
        mplew.writeInt(skillId);

        return mplew.getPacket();
    }

    private static void addRingInfo(MaplePacketLittleEndianWriter mplew, List<MapleRing> rings) {
        mplew.write(rings.size());
        for (MapleRing ring : rings) {
            mplew.writeInt(1);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
            mplew.writeInt(ring.getItemId());
        }
    }

    private static void addMRingInfo(MaplePacketLittleEndianWriter mplew, List<MapleRing> rings, MapleCharacter chr) {
        mplew.write(rings.size());
        for (MapleRing ring : rings) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeInt(ring.getItemId());
        }
    }

    public static byte[] getBuffBar(long millis) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUFF_BAR.getValue());
        mplew.writeLong(millis);

        return mplew.getPacket();
    }

    public static byte[] getBoosterFamiliar(int cid, int familiar, int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOOSTER_FAMILIAR.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(familiar);
        mplew.writeLong(id);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] viewSkills(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserDamageOnFallingCheck.getValue());
        List<Integer> skillz = new ArrayList<>();
        for (Skill sk : chr.getSkills().keySet()) {
            if ((sk.canBeLearnedBy(chr.getJob())) && (GameConstants.canSteal(sk)) && (!skillz.contains(sk.getId()))) {
                skillz.add(sk.getId());
            }
        }
        mplew.write(1);
        mplew.writeInt(chr.getId());
        mplew.writeInt(skillz.isEmpty() ? 2 : 4);
        mplew.writeInt(chr.getJob());
        mplew.writeInt(skillz.size());
        skillz.forEach(mplew::writeInt);

        return mplew.getPacket();
    }

    public static byte[] spawnArrowBlaster(MapleCharacter chr, int a) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FIELDATTACKOBJ_CREATE.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(1);
        mplew.writeInt(chr.getId());
        mplew.writeInt(0);
        mplew.writeInt((int) chr.getPosition().getX());
        mplew.writeInt((int) chr.getPosition().getY());
        mplew.write(a);

        return mplew.getPacket();
    }

    public static byte[] controlArrowBlaster(int a) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FIELDATTACKOBJ_SETATTACK.getValue());
        mplew.writeInt(a);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] cancelArrowBlaster(int b) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_FIELDATTACKOBJ_REMOVE_BYLIST.getValue());
        mplew.writeInt(1);
        mplew.writeInt(b);

        return mplew.getPacket();
    }

    public static byte[] getDeathTip(int op, boolean voice, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_OpenUIOnDead.getValue());

        mplew.writeInt(op);
        mplew.write(voice);
        // 0 - 普通死亡, 返回村莊
        // 1 - 使用組隊點數復活
        // 2 - 空白
        // 3 - 所在地圖復活
        // 4 - 戰鬥結束後將自動復活。
        // 5 - 靈魂之石的力量復活
        // 6 - 原地復活術
        // 7 - 高級服務復活
        // 8 - 韓文
        // 9 - 戰鬥機器人
        // 10 - 戰鬥機器人
        mplew.writeInt(type);
        if (type != 15) {
            if (((op >> 1) & 1) != 0) {
                if (type != 11) {
                    // if
                    mplew.writeInt(0);
                }
            }
        }

        return mplew.getPacket();
    }

    public static class InteractionPacket {

        public static byte[] getTradeInvite(MapleCharacter c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
            mplew.write(InteractionOpcode.交易邀請.getValue());
            mplew.write(4);// was 3
            mplew.writeMapleAsciiString(c.getName());
            // mplew.writeInt(c.getLevel());
            mplew.writeInt(c.getJob());
            return mplew.getPacket();
        }

        public static byte[] getTradeMesoSet(byte number, long meso) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
            mplew.write(InteractionOpcode.設定楓幣.getValue());
            mplew.write(number);
            mplew.writeLong(meso);
            return mplew.getPacket();
        }

        public static byte[] getTradeItemAdd(byte number, Item item) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
            mplew.write(InteractionOpcode.設置物品.getValue());
            mplew.write(number);
            mplew.write(item.getPosition());
            PacketHelper.GW_ItemSlotBase_Decode(mplew, item);

            return mplew.getPacket();
        }

        public static byte[] getTradeStart(MapleClient c, MapleTrade trade, byte number) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
            // mplew.write(PlayerInteractionHandler.Interaction.START_TRADE.action);
            // if (number != 0){//13 a0
            //// mplew.write(HexTool.getByteArrayFromHexString("13 01 01 03 FE 53 00 00 40
            // 08 00 00 00 E2 7B 00 00 01 E9 50 0F 00 03 62 98 0F 00 04 56 BF 0F 00 05 2A E7
            // 0F 00 07 B7 5B 10 00 08 3D 83 10 00 09 D3 D1 10 00 0B 13 01 16 00 11 8C 1F 11
            // 00 12 BF 05 1D 00 13 CB 2C 1D 00 31 40 6F 11 00 32 6B 46 11 00 35 32 5C 19 00
            // 37 20 E2 11 00 FF 03 B6 98 0F 00 05 AE 0A 10 00 09 CC D0 10 00 FF FF 00 00 00
            // 00 13 01 16 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 0B 00 4D 6F
            // 6D 6F 6C 6F 76 65 73 4B 48 40 08"));
            // mplew.write(19);
            // mplew.write(1);
            // PacketHelper.addCharLook(mplew, trade.getPartner().getChr(), false);
            // mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
            // mplew.writeShort(trade.getPartner().getChr().getJob());
            // }else{
            mplew.write(20);
            mplew.write(4);
            mplew.write(2);
            mplew.write(number);

            if (number == 1) {
                mplew.write(0);
                PacketHelper.AvatarLook__Decode(mplew, trade.getPartner().getChr(), false, false);
                mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
                mplew.writeShort(trade.getPartner().getChr().getJob());
            }
            mplew.write(number);
            PacketHelper.AvatarLook__Decode(mplew, c.getPlayer(), false, false);
            mplew.writeMapleAsciiString(c.getPlayer().getName());
            mplew.writeShort(c.getPlayer().getJob());
            mplew.write(255);
            // }
            return mplew.getPacket();
        }

        public static byte[] getTradeConfirmation() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
            mplew.write(InteractionOpcode.確認交易.getValue());

            return mplew.getPacket();
        }

        public static byte[] TradeMessage(byte UserSlot, byte message) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
            mplew.write(InteractionOpcode.退出.getValue());
            mplew.write(UserSlot);
            mplew.write(message);

            return mplew.getPacket();
        }

        public static byte[] getTradeCancel(byte UserSlot, int unsuccessful) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_MiniRoom.getValue());
            mplew.write(InteractionOpcode.退出.getValue());
            mplew.write(UserSlot);
            mplew.write(7);// was2

            return mplew.getPacket();
        }
    }

    public static class CScriptMan {

        public static byte[] OnScriptMessage(int nSpeakerTypeID, int nSpeakerTemplateID, int nAnotherSpeakerTemplateID,
                int nOtherSpeakerTemplateID, ScriptMessageType msgType, int bParam, int eColor, String[] msg,
                int[] value, int[][] values, MaplePet[] pets) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ScriptMessage.getValue());

            mplew.write(nSpeakerTypeID);
            mplew.writeInt(nSpeakerTemplateID);

            mplew.write(nAnotherSpeakerTemplateID > -1);
            if (nAnotherSpeakerTemplateID > -1) {
                mplew.writeInt(nAnotherSpeakerTemplateID);
            }

            mplew.write(msgType.getType());

            if (nOtherSpeakerTemplateID > -1) {
                bParam = bParam | 0x4;
            }
            mplew.writeShort(bParam);
            mplew.write(eColor);

            switch (msgType) {
                case SM_SAY:
                    CScriptMan.OnSay(mplew, msg[0], nOtherSpeakerTemplateID, (short) bParam, value[0] > 0, value[1] > 0,
                            value[2]);
                    break;
                case UNK_1:
                    mplew.writeMapleAsciiString(msg[0]);
                    mplew.write(value[0]);
                    mplew.write(value[1]);
                    mplew.writeInt(value[2]);
                    break;
                case SM_SAYIMAGE:
                    CScriptMan.OnSayImage(mplew, msg);
                    break;
                case SM_ASKYESNO:
                    CScriptMan.OnAskYesNo(mplew, msg[0], nOtherSpeakerTemplateID, (short) bParam);
                    break;
                case SM_ASKACCEPT:
                    CScriptMan.OnAskYesNo(mplew, msg[0], nOtherSpeakerTemplateID, (short) bParam);
                    break;
                case UNK_12:
                    mplew.writeInt(value[0]);
                    mplew.writeMapleAsciiString(msg[0]);
                    break;
                case SM_ASKTEXT:
                    CScriptMan.OnAskText(mplew, msg[0], msg[1], nOtherSpeakerTemplateID, (short) bParam, (short) value[0],
                            (short) value[1]);
                    break;
                case SM_ASKBOXTEXT:
                    CScriptMan.OnAskBoxText(mplew, msg[0], msg[1], nOtherSpeakerTemplateID, (short) bParam,
                            (short) value[0], (short) value[1]);
                    break;
                case SM_ASKBOXTEXT_BGIMG:
                    CScriptMan.OnAskBoxText_BgImg(mplew, (short) value[0], msg[0], msg[1], (short) value[1],
                            (short) value[2], (short) value[3], (short) value[4]);
                    break;
                case SM_ASKNUMBER:
                    CScriptMan.OnAskNumber(mplew, msg[0], value[0], value[1], value[2]);
                    break;
                case SM_ASKMENU:
                    CScriptMan.OnAskMenu(mplew, msg[0], nOtherSpeakerTemplateID, (short) bParam);
                    break;
                case UNK7:
                    if ((bParam & 0x4) != 0) {
                        mplew.writeInt(value[0]);
                    }
                    mplew.writeMapleAsciiString(msg[0]);
                    mplew.writeInt(value[1]);
                    break;
                case SM_ASKAVATAREX:
                    CScriptMan.OnAskAvatar(mplew, value[0] > 0, value[1] > 0, msg[0], values[0], value[2]);
                    break;
                case SM_ASKAVATAREXZERO:
                    CScriptMan.OnAskAvatarZero(mplew, msg[0], value[0], values[0], value[1], values[1], value[2]);
                    break;
                case SM_ASKMIXHAIR:
                    CScriptMan.OnAskMixHair(mplew, value[0] > 0, value[1] > 0, (byte) value[2], msg[0], values[0],
                            value[3]);
                    break;
                case SM_ASKMIXHAIR_EX_ZERO:
                    CScriptMan.OnAskMixHairExZero(mplew, (byte) value[0], msg[0], values[0], value[1], value[2]);
                    break;
                case SM_ASKCUSTOMMIXHAIR:
                    CScriptMan.OnAskCustomMixHair(mplew, value[0] > 0, value[1], value[2], msg[0]);
                    break;
                case SM_ASKCUSTOMMIXHAIR_AND_PROB:
                    CScriptMan.OnAskCustomMixHairAndProb(mplew, value[0] > 0, value[1], value[2], msg[0]);
                    break;
                case SM_ASKMIXHAIR_NEW:
                    CScriptMan.OnAskMixHairNew(mplew, value[0] > 0, value[1] > 0, (byte) value[2], msg[0], (byte) value[3],
                            value[4], value[5], value[6], value[7], value[8], value[9]);
                    break;
                case SM_ASKMIXHAIR_NEW_EX_ZERO:
                    CScriptMan.OnAskMixHairNewExZero(mplew, (byte) value[0], msg[0], (byte) value[1], value[2], value[3],
                            value[4], value[5], value[6], value[7], value[8], value[9]);
                    break;
                case SM_ASKANDROID:
                    CScriptMan.OnAskAndroid(mplew, msg[0], values[0], value[0]);
                    break;
                case SM_ASKPET:
                    CScriptMan.OnAskPet(mplew, msg[0], pets);
                    break;
                case SM_ASKPETALL:
                    CScriptMan.OnAskPetAll(mplew, msg[0], pets, value[0] > 0);
                    break;
                case SM_ASKACTIONPET_EVOLUTION:
                    CScriptMan.OnAskActionPetEvolution(mplew, msg[0], pets);
                    break;
                case SM_ASKQUIZ:
                    CScriptMan.OnInitialQuiz(mplew, value[0] > 0, msg[0], msg[1], msg[2], value[1], value[2], value[3]);
                    break;
                case SM_ASKSPEEDQUIZ:
                    CScriptMan.OnInitialSpeedQuiz(mplew, value[0] > 0, value[1], value[2], value[3], value[4], value[5]);
                    break;
                case SM_ASKICQUIZ:
                    CScriptMan.OnICQuiz(mplew, value[0] > 0, msg[0], msg[1], value[1]);
                    break;
                case SM_ASKSLIDEMENU:
                    CScriptMan.OnAskSlideMenu(mplew, value[0], value[1], msg[0]);
                    break;
                case SM_ASKSELECTMENU:
                    CScriptMan.OnAskSelectMenu(mplew, value[0]);
                    break;
                case SM_ASKANGELICBUSTER:
                    CScriptMan.OnAskAngelicBuster();
                    break;
                case SM_SAY_ILLUSTRATION:
                    CScriptMan.OnSayIllustration(mplew, nOtherSpeakerTemplateID, (short) bParam, msg[0], value[0] > 0,
                            value[1] > 0, value[2], value[3], value[4], value[5], false);
                    break;
                case SM_SAY_DUAL_ILLUSTRATION:
                    CScriptMan.OnSayIllustration(mplew, nOtherSpeakerTemplateID, (short) bParam, msg[0], value[0] > 0,
                            value[1] > 0, value[2], value[3], value[4], value[5], true);
                    break;
                case SM_ASKYESNO_ILLUSTRATION:
                    CScriptMan.OnAskYesNoIllustration(mplew, nOtherSpeakerTemplateID, (short) bParam, msg[0], value[0],
                            value[1], value[2], value[3], false);
                    break;
                case SM_ASKACCEPT_ILLUSTRATION:
                    CScriptMan.OnAskYesNoIllustration(mplew, nOtherSpeakerTemplateID, (short) bParam, msg[0], value[0],
                            value[1], value[2], value[3], false);
                    break;
                case SM_ASKMENU_ILLUSTRATION:
                    CScriptMan.OnAskMenuIllustration(mplew, msg[0], value[0], value[1], value[2], value[3], false);
                    break;
                case SM_ASKYESNO_DUAL_ILLUSTRATION:
                    CScriptMan.OnAskYesNoIllustration(mplew, nOtherSpeakerTemplateID, (short) bParam, msg[0], value[0],
                            value[1], value[2], value[3], true);
                    break;
                case SM_ASKACCEPT_DUAL_ILLUSTRATION:
                    CScriptMan.OnAskYesNoIllustration(mplew, nOtherSpeakerTemplateID, (short) bParam, msg[0], value[0],
                            value[1], value[2], value[3], true);
                    break;
                case SM_ASKMENU_DUAL_ILLUSTRATION:
                    CScriptMan.OnAskMenuIllustration(mplew, msg[0], value[0], value[1], value[2], value[3], true);
                    break;
                case SM_ASK_WEAPONBOX:
                    CScriptMan.OnAskWeaponBox(mplew, msg[0], value[0], values[0]);
                    break;
                case SM_ASK_USER_SURVEY:
                    CScriptMan.OnAskUserSurvey(mplew, value[0], value[1] > 0, msg[0]);
                    break;
                case SM_ASK_SCREEN_SHINNING_STAR_MSG:
                    CScriptMan.OnAskScreenShinningStarMsg();
                    break;
                case SM_ASKNUMBER_KEYPAD:
                    CScriptMan.OnAskNumberUseKeyPad(mplew, value[0]);
                    break;
                case SM_SPINOFF_GUITAR_RHYTHMGAME:
                    CScriptMan.OnSpinOffGuitarRhythmGame(mplew, value[0], value[1], value[2], value[1], msg[0]);
                    break;
                case SM_ASK_GHOSTPARK_ENTER_UI:
                    CScriptMan.OnGhostParkEnter(mplew);
                    break;
                case UNK_19:
                    mplew.write(value[0]);
                    if (value[0] == 0) {
                        mplew.writeMapleAsciiString(msg[0]);
                        mplew.writeInt(value[1]);
                        mplew.writeInt(value[2]);
                        mplew.writeInt(value[3]);
                        mplew.writeInt(value[4]);
                        mplew.writeInt(value[5]);
                    }
                    break;
                case UNK_1A:
                    mplew.writeMapleAsciiString(msg[0]);
                    mplew.writeInt(value[0]);
                    break;
                case UNK_3D:
                case UNK_3E:
                    int a6;
                    if ((bParam & 0x4) != 0) {
                        mplew.writeInt(0);
                    }
                    if (msgType == ScriptMessageType.UNK_3D) {
                        a6 = 0;
                    } else {
                        a6 = 1;
                    }
                    mplew.writeMapleAsciiString(msg[0]);
                    mplew.write(value[1]);
                    mplew.write(value[2]);
                    mplew.writeInt(value[3]);
                    mplew.writeInt(value[4]);
                    if (a6 == 1) {
                        mplew.writeInt(value[5]);
                        mplew.writeInt(value[6]);
                    } else {
                        mplew.write(value[5]);
                    }
                    break;
                case UNK_45:
                    mplew.writeMapleAsciiString(msg[0]);
                    mplew.writeInt(value[0]);
                    break;
                case UNK_46:
                    mplew.writeMapleAsciiString(msg[0]);
                    mplew.write(value[0]);
                    mplew.write(value[1]);
                    mplew.write(value[2]);
                    mplew.write(value[3]);
                    mplew.writeInt(value[4]);
                    break;
                default:
                    break;
            }

            return mplew.getPacket();
        }

        static void OnSay(MaplePacketLittleEndianWriter mplew, String sMsg, int nSpeakerTemplateID, short bParam,
                boolean bPrev, boolean bNext, int tWait) {
            if ((bParam & 0x4) != 0) {
                mplew.writeInt(nSpeakerTemplateID);
            }
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(bPrev);
            mplew.write(bNext);
            mplew.writeInt(tWait);
        }

        static void OnSayImage(MaplePacketLittleEndianWriter mplew, String[] images) {
            mplew.write(images.length);
            for (String str : images) {
                mplew.writeMapleAsciiString(str);
            }
        }

        static void OnAskYesNo(MaplePacketLittleEndianWriter mplew, String sMsg, int nSpeakerTemplateID,
                short bParam) {
            if ((bParam & 0x4) != 0) {
                mplew.writeInt(nSpeakerTemplateID);
            }
            mplew.writeMapleAsciiString(sMsg);
        }

        static void OnAskText(MaplePacketLittleEndianWriter mplew, String sMsg, String sDef,
                int nSpeakerTemplateID, short bParam, short nLenMin, short nLenMax) {
            if ((bParam & 0x4) != 0) {
                mplew.writeInt(nSpeakerTemplateID);
            }
            mplew.writeMapleAsciiString(sMsg); // 訊息文字
            mplew.writeMapleAsciiString(sDef); // 預設內容
            mplew.writeShort(nLenMin); // 最小長度限制[0=不限制]
            mplew.writeShort(nLenMax); // 最大長度限制[0=不限制]
        }

        static void OnAskBoxText(MaplePacketLittleEndianWriter mplew, String sMsg, String sDef,
                int nSpeakerTemplateID, short bParam, short nCol, short nLine) {
            if ((bParam & 0x4) != 0) {
                mplew.writeInt(nSpeakerTemplateID);
            }
            mplew.writeMapleAsciiString(sMsg); // 訊息文字
            mplew.writeMapleAsciiString(sDef); // 預設內容
            mplew.writeShort(nCol);
            mplew.writeShort(nLine);
        }

        static void OnAskBoxText_BgImg(MaplePacketLittleEndianWriter mplew, short nBackgrndIdx, String sMsg,
                String sDef, short nCol, short nLine, short nFontSize, short nFontTopMargin) {
            mplew.writeShort(nBackgrndIdx);
            mplew.writeMapleAsciiString(sMsg);
            mplew.writeMapleAsciiString(sDef);
            mplew.writeShort(nCol);
            mplew.writeShort(nLine);
            mplew.writeShort(nFontSize);
            mplew.writeShort(nFontTopMargin);
        }

        static void OnAskNumber(MaplePacketLittleEndianWriter mplew, String sMsg, int nDef, int nMin, int nMax) {
            mplew.writeMapleAsciiString(sMsg);
            mplew.writeInt(nDef);
            mplew.writeInt(nMin);
            mplew.writeInt(nMax);
        }

        static void OnAskMenu(MaplePacketLittleEndianWriter mplew, String sMsg, int nSpeakerTemplateID,
                short bParam) {
            if ((bParam & 0x4) != 0) {
                mplew.writeInt(nSpeakerTemplateID);
            }
            mplew.writeMapleAsciiString(sMsg);
        }

        static void OnAskAvatar(MaplePacketLittleEndianWriter mplew, boolean bAngelicBuster, boolean bZeroBeta,
                String sMsg, int[] aCode, int nCardID) {
            mplew.write(bAngelicBuster);
            mplew.write(bZeroBeta);
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(aCode.length);
            for (int nCode : aCode) {
                mplew.writeInt(nCode);
            }
            mplew.writeInt(nCardID);
        }

        static void OnAskAvatarZero(MaplePacketLittleEndianWriter mplew, String sMsg, int nGender, int[] aCode,
                int nGender2, int[] aCode2, int nCardID) {
            mplew.writeMapleAsciiString(sMsg);
            mplew.writeInt(nGender);
            mplew.write(aCode.length);
            for (int nCode : aCode) {
                mplew.writeInt(nCode);
            }
            mplew.writeInt(nCardID);

            mplew.writeInt(nGender2);
            mplew.write(aCode2.length);
            for (int nCode : aCode2) {
                mplew.writeInt(nCode);
            }
            mplew.writeInt(nCardID);
        }

        static void OnAskMixHair(MaplePacketLittleEndianWriter mplew, boolean bAngelicBuster, boolean bZeroBeta,
                byte typ, String sMsg, int[] paCode, int unk) {
            mplew.write(bAngelicBuster);
            mplew.write(bZeroBeta);
            mplew.write(typ);
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(paCode.length);
            for (int nCode : paCode) {
                mplew.writeInt(nCode);
            }
            if (typ == 3) {
                mplew.writeInt(unk);
            }
            if (typ == 6) {
                mplew.writeInt(unk);
            }
        }

        static void OnAskMixHairExZero(MaplePacketLittleEndianWriter mplew, byte typ, String sMsg, int[] paCode,
                int unk, int unk2) {
            mplew.write(typ);
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(paCode.length);
            for (int nCode : paCode) {
                mplew.writeInt(nCode);
            }
            mplew.writeInt(unk);
            mplew.writeInt(unk2);
        }

        static void OnAskCustomMixHair(MaplePacketLittleEndianWriter mplew, boolean bAngelicBuster, int nGender,
                int nCode, String sMsg) {
            mplew.write(bAngelicBuster);
            mplew.writeInt(nGender);
            mplew.writeInt(nCode);
            mplew.writeMapleAsciiString(sMsg);
        }

        static void OnAskCustomMixHairAndProb(MaplePacketLittleEndianWriter mplew, boolean bAngelicBuster,
                int nGender, int nCode, String sMsg) {
            mplew.write(bAngelicBuster);
            mplew.writeInt(nGender);
            mplew.writeInt(nCode);
            mplew.writeMapleAsciiString(sMsg);
        }

        static void OnAskMixHairNew(MaplePacketLittleEndianWriter mplew, boolean bAngelicBuster,
                boolean bZeroBeta, byte typ, String sMsg, byte nUnk, int nBaseColor, int nMixColor, int nAlpha,
                int nBaseColor2, int nMixColor2, int nAlpha2) {
            mplew.write(bAngelicBuster);
            mplew.write(bZeroBeta);
            mplew.write(typ);
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(nUnk);
            if (nUnk == 3) {
                mplew.writeInt(nBaseColor);
                mplew.writeInt(nMixColor);
                mplew.writeInt(nAlpha);
            } else if (nUnk == 6) {
                mplew.writeInt(nBaseColor);
                mplew.writeInt(nMixColor);
                mplew.writeInt(nAlpha);
                mplew.writeInt(nBaseColor2);
                mplew.writeInt(nMixColor2);
                mplew.writeInt(nAlpha2);
            }
        }

        static void OnAskMixHairNewExZero(MaplePacketLittleEndianWriter mplew, byte typ, String sMsg, byte nUnk,
                int nBaseColor, int nMixColor, int nAlpha, int nBaseColor2, int nMixColor2, int nAlpha2, int nUnk2,
                int nUnk3) {
            mplew.write(typ);
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(nUnk);
            mplew.writeInt(nBaseColor);
            mplew.writeInt(nMixColor);
            mplew.writeInt(nAlpha);
            mplew.writeInt(nBaseColor2);
            mplew.writeInt(nMixColor2);
            mplew.writeInt(nAlpha2);
            mplew.writeInt(nUnk2);
            mplew.writeInt(nUnk3);
        }

        static void OnAskAndroid(MaplePacketLittleEndianWriter mplew, String sMsg, int[] aCode, int nCardID) {
            mplew.writeMapleAsciiString(sMsg); // 預覽對話介紹
            mplew.write(aCode.length); // style個數
            for (int nCode : aCode) {
                mplew.writeInt(nCode); // style ID
            }
            mplew.writeInt(nCardID); // Card ID
        }

        static void OnAskPet(MaplePacketLittleEndianWriter mplew, String sMsg, MaplePet[] pets) {
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(pets.length);
            for (MaplePet pet : pets) {
                mplew.writeLong(pet.getUniqueId());
                mplew.write(0); // petUnk
            }
        }

        static void OnAskPetAll(MaplePacketLittleEndianWriter mplew, String sMsg, MaplePet[] pets,
                boolean bExceptionExist) {
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(pets.length);
            mplew.write(bExceptionExist);
            for (MaplePet pet : pets) {
                mplew.writeLong(pet.getUniqueId());
                mplew.write(0); // petUnk
            }
        }

        static void OnAskActionPetEvolution(MaplePacketLittleEndianWriter mplew, String sItemID,
                MaplePet[] pets) {
            mplew.writeMapleAsciiString(sItemID);
            int unkVal = 11;
            if (unkVal == 11) {
                mplew.write(pets.length);
                for (MaplePet pet : pets) {
                    mplew.writeLong(pet.getUniqueId());
                    mplew.write(0); // petUnk
                }
            }
        }

        static void OnInitialQuiz(MaplePacketLittleEndianWriter mplew, boolean bUnk, String sTitle,
                String sProblemText, String sUnk, int nMinInput, int nMaxInput, int tRemain) {
            mplew.write(bUnk);
            if (!bUnk) {
                mplew.writeMapleAsciiString(sTitle);
                mplew.writeMapleAsciiString(sProblemText);
                mplew.writeMapleAsciiString(sUnk);
                mplew.writeInt(nMinInput);
                mplew.writeInt(nMaxInput);
                mplew.writeInt(tRemain); // * 1000
            }
        }

        static void OnInitialSpeedQuiz(MaplePacketLittleEndianWriter mplew, boolean bUnk, int nType,
                int dwAnswer, int nCorrect, int nRemain, int tRemain) {
            mplew.write(bUnk);
            if (!bUnk) {
                mplew.writeInt(nType);
                mplew.writeInt(dwAnswer);
                mplew.writeInt(nCorrect);
                mplew.writeInt(nRemain);
                mplew.writeInt(tRemain); // * 1000
            }
        }

        static void OnICQuiz(MaplePacketLittleEndianWriter mplew, boolean bUnk, String sQuestion, String sHint,
                int tRemain) {
            mplew.write(bUnk);
            if (!bUnk) {
                mplew.writeMapleAsciiString(sQuestion);
                mplew.writeMapleAsciiString(sHint);
                mplew.writeInt(tRemain); // * 1000
            }
        }

        static void OnAskSlideMenu(MaplePacketLittleEndianWriter mplew, int nDlgType, int nDefaultSelect,
                String s) {
            mplew.writeInt(nDlgType); // 選單類型
            mplew.writeInt(nDefaultSelect);
            mplew.writeMapleAsciiString(s);
        }

        static void OnAskSelectMenu(MaplePacketLittleEndianWriter mplew, int nDlgType) {
            mplew.writeInt(nDlgType);
            if (nDlgType == 0 || nDlgType == 1) {
                mplew.writeInt(0); // nDefaultSelect
                int str_Size = 0;
                mplew.writeInt(str_Size);
                for (int i = 0; i < str_Size; i++) {
                    mplew.writeMapleAsciiString("");
                }
            }
        }

        static void OnAskAngelicBuster() {
        }

        static void OnSayIllustration(MaplePacketLittleEndianWriter mplew, int nSpeakerTemplateID, short bParam,
                String sMsg, boolean bPrev, boolean bNext, int nNpcId, int nFaceIndex, int bIsLeft, int nFaceIndex2,
                boolean bIsDual) {
            if ((bParam & 0x4) != 0) {
                mplew.writeInt(nSpeakerTemplateID);
            }
            mplew.writeMapleAsciiString(sMsg);
            mplew.write(bPrev);
            mplew.write(bNext);
            mplew.writeInt(nNpcId);
            mplew.writeInt(nFaceIndex);
            if (bIsDual) {
                mplew.writeInt(bIsLeft);
                mplew.writeInt(nFaceIndex2);
            } else {
                mplew.write(bIsLeft);
            }
        }

        static void OnAskYesNoIllustration(MaplePacketLittleEndianWriter mplew, int nSpeakerTemplateID,
                short bParam, String sMsg, int nNpcId, int nFaceIndex, int bIsLeft, int nFaceIndex2,
                boolean bIsDual) {
            if ((bParam & 0x4) != 0) {
                mplew.writeInt(nSpeakerTemplateID);
            }
            mplew.writeMapleAsciiString(sMsg);
            mplew.writeInt(nNpcId);
            mplew.writeInt(nFaceIndex);
            if (bIsDual) {
                mplew.writeInt(bIsLeft);
                mplew.writeInt(nFaceIndex2);
            } else {
                mplew.write(bIsLeft);
            }
        }

        static void OnAskMenuIllustration(MaplePacketLittleEndianWriter mplew, String sMsg, int nNpcId,
                int nFaceIndex, int bIsLeft, int nFaceIndex2, boolean bIsDual) {
            mplew.writeMapleAsciiString(sMsg);
            mplew.writeInt(nNpcId);
            mplew.writeInt(nFaceIndex);
            if (bIsDual) {
                mplew.writeInt(bIsLeft);
                mplew.writeInt(nFaceIndex2);
            } else {
                mplew.write(bIsLeft);
            }
        }

        static void OnAskWeaponBox(MaplePacketLittleEndianWriter mplew, String sMsg, int nWeaponBox,
                int[] aWeaponList) {
            mplew.writeMapleAsciiString(sMsg);
            mplew.writeInt(nWeaponBox);
            mplew.writeInt(aWeaponList.length);
            for (int i : aWeaponList) {
                mplew.writeInt(i);
            }
        }

        static void OnAskUserSurvey(MaplePacketLittleEndianWriter mplew, int nTalkType, boolean bShowExitBtn,
                String sTalkMsg) {
            mplew.writeInt(nTalkType);
            mplew.write(bShowExitBtn);
            mplew.writeMapleAsciiString(sTalkMsg);
        }

        static void OnAskScreenShinningStarMsg() {
        }

        static void OnAskNumberUseKeyPad(MaplePacketLittleEndianWriter mplew, int nResult) {
            mplew.writeInt(nResult);
        }

        static void OnSpinOffGuitarRhythmGame(MaplePacketLittleEndianWriter mplew, int nUnk, int nUnk2,
                int nUnk3, int nMusicNumber, String sSoundUOL) {
            mplew.writeInt(nUnk);
            if (nUnk == 0) {
                mplew.writeInt(nUnk2);
                mplew.writeInt(nUnk3);
                return;
            }
            if (nUnk != 1) {
                return;
            }
            mplew.writeInt(nMusicNumber);
            mplew.writeMapleAsciiString(sSoundUOL);
        }

        static void OnGhostParkEnter(MaplePacketLittleEndianWriter mplew) {
            int size = 0;
            mplew.writeInt(size);
            for (int i = 0; i < size; i++) {
                mplew.writeInt(0);
                mplew.writeInt(0); // nIncRate
                mplew.writeInt(0); // nBonusRate
            }
        }

        public static byte[] getNPCTalk(int npc, ScriptMessageType msgType, String talk, String endBytes, byte type) {
            return getNPCTalk(npc, msgType, talk, endBytes, type, npc);
        }

        public static byte[] getNPCTalk(int npc, ScriptMessageType msgType, String talk, String endBytes, byte type,
                int diffNPC) {
            return getNPCTalk(npc, msgType, talk, endBytes, type, (byte) 0, diffNPC);
        }

        public static byte[] getNPCTalk(int npc, ScriptMessageType nMsgType, String talk, String endBytes, byte bParam,
                byte type2, int nSpeakerTemplateID) {
            return OnScriptMessage(4, npc, -1, (bParam & 0x4) != 0 ? nSpeakerTemplateID : -1, nMsgType, bParam, type2,
                    new String[]{talk},
                    new int[]{endBytes.startsWith("00 ") ? 0 : 1, endBytes.endsWith(" 00") ? 0 : 1, 0}, null, null);
        }

        public static byte[] getOthersTalk(int npc, ScriptMessageType msgType, int npc_unk, String talk,
                String endBytes, byte type) {
            return OnScriptMessage(3, npc, npc_unk > 0 ? npc_unk : -1, -1, msgType, type, 0, new String[]{talk},
                    new int[]{endBytes.startsWith("00 ") ? 0 : 1, endBytes.endsWith(" 00") ? 0 : 1, 0}, null, null);
        }

        public static byte[] getZeroNPCTalk(int npc, ScriptMessageType nMsgType, String talk, String endBytes,
                byte bParam, int nSpeakerTemplateID) {
            return OnScriptMessage(3, 0, npc, nSpeakerTemplateID, nMsgType, bParam, 0, new String[]{talk},
                    new int[]{endBytes.startsWith("00 ") ? 0 : 1, endBytes.endsWith(" 00") ? 0 : 1, 0}, null, null);
        }

        public static byte[] getSengokuNPCTalk(boolean unknown, int npc, ScriptMessageType nMsgType, byte bParam,
                int nSpeakerTemplateID, String talk, boolean next, boolean prev, boolean pic) {
            return OnScriptMessage(unknown ? 4 : 3, 0, npc, nSpeakerTemplateID, nMsgType, bParam, 0,
                    new String[]{talk},
                    new int[]{!next ? 0 : 1, !prev ? 0 : 1, nSpeakerTemplateID, pic ? 1 : 0, 0, 0}, null, null);
        }

        public static byte[] getEnglishQuiz(int npc, byte bParam, int nSpeakerTemplateID, String talk,
                String endBytes) {
            return OnScriptMessage(4, npc, -1, nSpeakerTemplateID, ScriptMessageType.SM_ASKANDROID, bParam, 0,
                    new String[]{talk},
                    new int[]{endBytes.startsWith("00 ") ? 0 : 1, endBytes.endsWith(" 00") ? 0 : 1, 0}, null, null);
        }

        public static byte[] getAdviceTalk(String[] wzinfo) {
            return OnScriptMessage(8, 0, -1, -1, ScriptMessageType.SM_SAYIMAGE, 1, 0, wzinfo, null, null, null);
        }

        public static byte[] getSlideMenu(int npcid, int type, int lasticon, String sel) {
            return OnScriptMessage(4, npcid, -1, -1, ScriptMessageType.SM_ASKSLIDEMENU, 0, 0, new String[]{sel},
                    new int[]{type, type == 0 ? lasticon : 0}, null, null);
        }

        public static byte[] getNPCTalkStyle(int npc, String talk, int[] args, int card, boolean second) {
            return OnScriptMessage(4, npc, -1, -1, ScriptMessageType.SM_ASKAVATAREX, 0, 0, new String[]{talk},
                    new int[]{second ? 1 : 0, 0, card}, new int[][]{args}, null);
        }

        public static byte[] getAndroidTalkStyle(int npc, String talk, int[] args, int card) {
            return OnScriptMessage(4, npc, -1, -1, ScriptMessageType.SM_ASKANDROID, 0, 0, new String[]{talk},
                    new int[]{card}, new int[][]{args}, null);
        }

        public static byte[] getNPCTalkNum(int npc, String talk, int def, int min, int max) {
            return OnScriptMessage(4, npc, -1, -1, ScriptMessageType.SM_ASKNUMBER, 0, 0, new String[]{talk},
                    new int[]{def, min, max}, null, null);
        }

        public static byte[] getNPCTalkText(int npc, String talk) {
            return OnScriptMessage(4, npc, -1, -1, ScriptMessageType.SM_ASKTEXT, 0, 0, new String[]{talk, ""},
                    new int[]{0, 0}, null, null);
        }

        public static byte[] getNPCTalkQuiz(int npc, String caption, String talk, int time) {
            return OnScriptMessage(4, npc, -1, -1, ScriptMessageType.SM_ASKQUIZ, 0, 0,
                    new String[]{caption, talk, ""}, new int[]{0, 0, 0xF, time}, null, null);
        }

        public static byte[] getSelfTalkText(String text) {
            return OnScriptMessage(3, 0, 0, -1, ScriptMessageType.SM_SAY, 3, 0, new String[]{text},
                    new int[]{0, 1, 0}, null, null);
        }

        public static byte[] getNPCTutoEffect(String effect) {
            return OnScriptMessage(3, 0, -1, -1, ScriptMessageType.SM_SAYIMAGE, 1, 0, new String[]{effect},
                    new int[]{1}, null, null);
        }

        public static byte[] getJobSelection(int npcid, int job) {
            return OnScriptMessage(3, 0, npcid, -1, ScriptMessageType.SM_ASKSELECTMENU, 1, 1, null, new int[]{job},
                    null, null);
        }

        public static byte[] getAngelicBusterAvatarSelect(int npc) {
            return OnScriptMessage(4, npc, -1, -1, ScriptMessageType.SM_ASKANGELICBUSTER, 0, 0, null, null, null, null);
        }

        public static byte[] getEvanTutorial(String data) {
            return OnScriptMessage(8, 0, -1, -1, ScriptMessageType.SM_SAYIMAGE, 1, 0, new String[]{data},
                    new int[]{1}, null, null);
        }

        public static byte[] getArisanNPCTalk(int npc, boolean read, ScriptMessageType nMsgType, byte bParam,
                byte result, String talk) {
            return OnScriptMessage(8, npc, read ? 0 : -1, -1, nMsgType, bParam, result, new String[]{talk}, null,
                    null, null);
        }

        public static byte[] getDreamWorldNPCTalk(int npc, boolean read, ScriptMessageType nMsgType, byte bParam,
                byte result, int nSpeakerTemplateID, String talk) {
            return OnScriptMessage(8, npc, read ? 0 : -1, nSpeakerTemplateID, nMsgType, bParam, result,
                    new String[]{talk}, null, null, null);
        }

        public static byte[] NPCTalk() {
            // 2400009 남자, 2400010 여자
            return OnScriptMessage(3, 0, -1, 2400010, ScriptMessageType.SM_SAY, 0x25, 0, new String[]{"#face1#滾開！"},
                    new int[]{1, 1, 0}, null, null);
        }
    }

    public static class NPCPacket {

        static void CNpc_Init(MaplePacketLittleEndianWriter mplew, MapleNPC life, boolean m_bEnabled) {
            mplew.writeShort(life.getPosition().x); // m_ptPos.x
            mplew.writeShort(life.getCy()); // m_ptPos.y
            mplew.write(0); // m_bMove
            mplew.write(life.getF() == 1 ? 0 : 1); // m_nMoveAction
            mplew.writeShort(life.getFh()); // CWvsPhysicalSpace2D::GetFoothold(dwSN);
            mplew.writeShort(life.getRx0()); // m_rgHorz.low
            mplew.writeShort(life.getRx1()); // m_rgHorz.high
            mplew.write(m_bEnabled);
            mplew.writeInt(0); // CNpc::SetPresentItem(nItemID)
            mplew.write(0); // m_nPresentTimeState
            mplew.writeInt(-1); // m_tPresent

            int m_nNoticeBoardType = 0;
            mplew.writeInt(m_nNoticeBoardType);
            if (m_nNoticeBoardType == 1) {
                mplew.writeInt(0); // m_nNoticeBoardValue
            }
            mplew.writeInt(0); // tAlpha
            mplew.writeInt(0);
            mplew.writeMapleAsciiString(""); // sLocalRepeatEffect
            boolean v44 = false;
            mplew.write(v44);
            if (v44) {
                // CScreenInfo::Decode{
                mplew.write(0); // CScreenInfo::CreateScreenInfo(nType)
                // unk_sub(mplew);
                // }
            }
        }

        public static byte[] spawnNPC(MapleNPC life, boolean show) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_NpcEnterField.getValue());
            mplew.writeInt(life.getObjectId());
            mplew.writeInt(life.getId());
            CNpc_Init(mplew, life, show);

            return mplew.getPacket();
        }

        public static byte[] resetNPC(int objectid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_NpcCharacterBaseAction.getValue());
            mplew.writeInt(objectid);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] removeNPC(int objectid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_NpcLeaveField.getValue());
            mplew.writeInt(objectid);

            return mplew.getPacket();
        }

        public static byte[] removeNPCController(int objectid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_NpcChangeController.getValue());
            mplew.write(0);
            mplew.writeInt(objectid);

            return mplew.getPacket();
        }

        public static byte[] spawnNPCRequestController(MapleNPC life, boolean MiniMap) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_NpcChangeController.getValue());
            mplew.write(1);
            mplew.writeInt(life.getObjectId());
            mplew.writeInt(life.getId());
            CNpc_Init(mplew, life, MiniMap);

            return mplew.getPacket();
        }

        public static byte[] toggleNPCShow(int oid, boolean hide) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.NPC_TOGGLE_VISIBLE.getValue());
            mplew.writeInt(oid);
            mplew.write(hide ? 0 : 1);

            return mplew.getPacket();
        }

        public static byte[] setNPCSpecialAction(int oid, String action, int time, boolean unk) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_NpcSpecialAction.getValue());
            mplew.writeInt(oid);
            mplew.writeMapleAsciiString(action);
            mplew.writeInt(time);
            mplew.write(unk);

            return mplew.getPacket();
        }

        public static byte[] NPCSpecialAction(int oid, int value, int x, int y) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_NpcUpdateLimitedInfo.getValue());
            mplew.writeInt(oid);
            mplew.writeInt(value);
            mplew.writeInt(x);
            mplew.writeInt(y);

            return mplew.getPacket();
        }

        public static byte[] setNPCScriptable() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_NpcSetScript.getValue());
            List<Pair<Integer, String>> npcs = new LinkedList<>();
            npcs.add(new Pair<>(9070006,
                    "Why...why has this happened to me? My knightly honor... My knightly pride..."));
            npcs.add(new Pair<>(9000021, "Are you enjoying the event?"));
            mplew.write(npcs.size());
            for (Pair<Integer, String> s : npcs) {
                mplew.writeInt(s.getLeft());
                mplew.writeMapleAsciiString(s.getRight());
                mplew.writeInt(0);
                mplew.writeInt(Integer.MAX_VALUE);
            }
            return mplew.getPacket();
        }

        public static byte[] getNPCShop(int sid, MapleShop shop, MapleClient c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_OpenShopDlg.getValue());
            boolean unk_Boolean = false;
            mplew.write(unk_Boolean);
            if (unk_Boolean) {
                mplew.writeInt(0);
            }
            PacketHelper.CShopDlg_SetShopDlg(mplew, sid, shop, c);

            return mplew.getPacket();
        }

        public static byte[] confirmShopTransaction(ShopOpcode opShop, MapleShop shop, MapleClient c, int indexBought) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ShopResult.getValue());
            mplew.write(opShop.getValue());
            switch (opShop) {
                case ShopRes_MarketTempBlock:
                    break;
                case ShopRes_BuySuccess:
                    mplew.write(indexBought >= 0);
                    if (indexBought >= 0) {
                        mplew.writeInt(indexBought);
                    } else {
                        int size = 0;
                        mplew.write(size);
                        if (size > 0) {
                            mplew.writeInt(0);
                        }
                        mplew.writeInt(0);
                    }
                    break;
                case ShopRes_SellSuccess:
                    PacketHelper.CShopDlg_SetShopDlg(mplew, shop.getNpcId(), shop, c);
                    break;
                case ShopRes_BuyNoStock:
                case ShopRes_SellNoStock:
                case ShopRes_RechargeNoStock:
                    break;
                case ShopRes_BuyStockOver:
                    mplew.writeInt(0);
                    break;
                case ShopRes_BuyNoMoney:
                case ShopRes_RechargeNoMoney:
                    break;
                case ShopRes_5:
                    break;
                case ShopRes_BuyUnknown:
                    break;
                case ShopRes_CantBuyAnymore:
                    break;
                case ShopRes_BuyNoToken:
                    break;
                case ShopReq_Close:
                    break;
                case ShopRes_BuyNoStarCoin:
                    break;
                case ShopRes_BuyNoFloor:
                    mplew.writeInt(0);
                    break;
                case ShopRes_UnalbeShopVersion:
                    boolean unk = false;
                    mplew.write(unk);
                    if (unk) {
                        PacketHelper.CShopDlg_SetShopDlg(mplew, shop.getNpcId(), shop, c);
                    }
                    break;
                case ShopRes_BuyNoQuestEx:
                    break;
                case ShopRes_LimitLevel_Less:
                    mplew.writeInt(0);
                    break;
                case ShopRes_LimitLevel_More:
                    mplew.writeInt(0);
                    break;
                case ShopRes_TradeBlocked:
                    break;
                case ShopRes_FailedByBuyLimit:
                    mplew.writeInt(0);
                    break;
                case ShopRes_NpcRandomShopReset:
                    break;
                case ShopRes_BuyInvalidTime:
                    break;
                case ShopRes_SellOverflow:
                    break;
                case ShopRes_SellLimitPriceAtOnetime:
                    break;
                case ShopRes_TradeBlockedNotActiveAccount:
                    break;
                case ShopRes_TradeBlockedSnapShot:
                    mplew.writeInt(0);
                    break;
                case ShopRes_UnalbeWorld:
                    break;
                case ShopRes_23:
                    break;
                case ShopRes_24:
                case ShopRes_26:
                    break;
                case ShopRes_25:
                    break;
                case ShopRes_27:
                    break;
                case ShopRes_28:
                    break;
                default:
                    break;
                case ShopRes_RechargeSuccess:
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] getStorage(StorageType storage, int npcId, byte slots, long meso, MapleInventoryType type,
                Collection<Item> items) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TrunkResult.getValue());
            mplew.write(storage.getType());
            long mask = 0;
            switch (storage) {
                case TAKE_OUT: // 0x09
                case STORE: // 0x0D
                    mplew.write(slots);
                    mplew.writeLong(type.getBitfieldEncoding());
                    mplew.write(items.size());
                    for (Item item : items) {
                        PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
                    }
                    break;
                case ARRANGE: // 0x0F
                    mask = 0x7C;
                    mplew.write(slots);
                    mplew.writeLong(mask);
                    if ((mask & 2) != 0) {
                        mplew.writeLong(meso);
                    }
                    mplew.writeZeroBytes(3);
                    mplew.write(items.size());
                    for (Item item : items) {
                        PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
                    }
                    mplew.write(0);
                    break;
                case FULL: // 0x11
                    break;
                case MESO: // 0x13
                    mask = 0x2;
                    mplew.write(slots);
                    mplew.writeLong(mask);
                    if ((mask & 2) != 0) {
                        mplew.writeLong(meso);
                    }
                    break;
                case OPEN: // 0x16
                    mask = 0x7E;
                    mplew.writeInt(npcId);
                    mplew.write(slots);
                    mplew.writeLong(mask);
                    if ((mask & 2) != 0) {
                        mplew.writeLong(meso);
                    }
                    mplew.writeShort(0);
                    mplew.write((byte) items.size());
                    for (Item item : items) {
                        PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
                    }
                    mplew.writeShort(0);
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] takeOutStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
            return getStorage(StorageType.TAKE_OUT, 0, slots, 0, type, items);
        }

        public static byte[] storeStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
            return getStorage(StorageType.STORE, 0, slots, 0, type, items);
        }

        public static byte[] arrangeStorage(byte slots, Collection<Item> items) {
            return getStorage(StorageType.ARRANGE, 0, slots, 0, null, items);
        }

        public static byte[] getStorageFull() {
            return getStorage(StorageType.FULL, 0, (byte) 0, 0, null, null);
        }

        public static byte[] mesoStorage(byte slots, long meso) {
            return getStorage(StorageType.MESO, 0, slots, meso, null, null);
        }

        public static byte[] getStorage(int npcId, byte slots, Collection<Item> items, long meso) {
            return getStorage(StorageType.OPEN, npcId, slots, meso, null, items);
        }
    }

    public static class SummonPacket {

        public static byte[] spawnSummon(MapleSummon summon, boolean animated) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedEnterField.getValue());
            mplew.writeInt(summon.getOwnerId());

            mplew.writeInt(summon.getObjectId());
            mplew.writeInt(summon.getSkill()); // nSkillID
            mplew.write(summon.getOwnerLevel()); // nCharLevel
            mplew.write(summon.getSkillLevel()); // nSLV
            mplew.writePos(summon.getPosition());
            mplew.write(summon.isFacingLeft() ? 5 : 4); // nMoveAction
            mplew.writeShort(summon.getFh()); // nCurFoothold
            mplew.write(summon.getMovementType().getValue()); // nMoveAbility
            mplew.write(summon.getAttackType()); // nAssistType
            mplew.write(animated ? 1 : 0); // nEnterType
            mplew.writeInt(summon.getLinkmonid()); // dwMobID
            mplew.write(0); // bFlyMob
            mplew.write(1); // bBeforeFirstAttack
            mplew.writeInt(0); // nLookID
            mplew.writeInt(0); // nBulletID

            MapleCharacter chr = summon.getOwner();
            boolean sendAddCharLook = ((summon.getSkill() == 4341006 || summon.getSkill() == 14111024)
                    && (chr != null));
            mplew.write(sendAddCharLook ? 1 : 0); // Mirrored Target
            if (sendAddCharLook) { // Mirrored Target
                PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
            }
            if (summon.getSkill() == 35111002) {// 磁場 Rock 'n Shock
                boolean v8 = false;
                mplew.write(v8); // m_nTeslaCoilState
                if (v8) {
                    int v33 = 0;
                    do {
                        mplew.writeShort(0); // pTriangle.x
                        mplew.writeShort(0); // pTriangle.y
                        v33++;
                    } while (v33 < 3);
                }
            } else if (summon.getSkill() == 400051014) { // 火龍風暴
                mplew.writeLong(0);
            }

            if (summon.isSpecialSummon()) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            } else if (summon.getSkill() == 42111003) { // 鬼神召喚
                mplew.writeShort(summon.getPosition().x);
                mplew.writeShort(summon.getPosition().y);
                mplew.writeShort(0);
                mplew.writeShort(0);
            } else if (summon.getSkill() - 400051028 <= 4) {
                mplew.write(0);
            }

            mplew.write(0); // m_bJaguarActive
            int delay = 260000;
            switch (summon.getSkill()) {
                case 32120019:
                    delay = 5000;
                    break;
                case 32110017:
                    delay = 8000;
                    break;
                case 32100010:
                    delay = 9000;
                    break;
            }
            mplew.writeInt(delay); // m_tSummonTerm
            mplew.write(0);
            mplew.writeInt(0);
            // 召喚美洲豹
            if (summon.getSkill() - 33001007 <= 8 && summon.getSkill() - 33001007 >= 0) {
                mplew.write(0);
                mplew.writeInt(0);
            }

            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] removeSummon(int ownerId, int objId) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedLeaveField.getValue());
            mplew.writeInt(ownerId);
            mplew.writeInt(objId);
            mplew.write(10);

            return mplew.getPacket();
        }

        public static byte[] removeSummon(MapleSummon summon, boolean animated) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedLeaveField.getValue());
            mplew.writeInt(summon.getOwnerId());
            mplew.writeInt(summon.getObjectId());
            if (animated) {
                switch (summon.getSkill()) {
                    case 35121003:
                        mplew.write(10);
                        break;
                    case 33101008:
                    case 35111001:
                    case 35111002:
                    case 35111005:
                    case 35111009:
                    case 35111010:
                    case 35111011:
                    case 35121009:
                    case 35121010:
                    case 35121011:
                        mplew.write(5);
                        break;
                    default:
                        mplew.write(4);
                        break;
                }
            } else {
                mplew.write(1);
            }

            return mplew.getPacket();
        }

        public static byte[] moveSummon(int cid, int oid, int duration, Point startPos, Point endPos,
                List<LifeMovementFragment> moves) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedMove.getValue());
            mplew.writeInt(cid);
            mplew.writeInt(oid);
            PacketHelper.serializeMovementList(mplew, duration, startPos, endPos, moves);

            return mplew.getPacket();
        }

        public static byte[] summonAttack(int cid, int summonSkillId, byte nAction, byte tbyte, List<AttackPair> allDamage, int m_nCharLevel, boolean bCounterAttack) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedAttack.getValue());

            mplew.writeInt(cid);

            mplew.writeInt(summonSkillId);

            mplew.write(m_nCharLevel);
            mplew.write(nAction);

            int nAttackCount = tbyte & 0xF;
            int nMobCount = allDamage.size();
            tbyte = (byte) ((nMobCount << 4) + nAttackCount);
            mplew.write(tbyte);

            for (AttackPair attackEntry : allDamage) {
                if (attackEntry.attack != null) {
                    mplew.writeInt(attackEntry.objectid);
                    if (attackEntry.objectid > 0) {
                        mplew.write(7);
                        Pair<Long, Boolean> eachd;
                        for (int i = 0; i < nAttackCount; i++) {
                            if (attackEntry.attack == null || attackEntry.attack.size() < i + 1) {
                                mplew.writeLong(0);
                            } else {
                                eachd = attackEntry.attack.get(i);
                                mplew.writeLong(eachd.left + (!eachd.right ? 0 : Long.MIN_VALUE));
                            }
                        }
                    }
                }
            }
            mplew.write(bCounterAttack);

            // 韓版IDB裡面的數據
            byte pMob = 0;
            byte bNoAction = 0;
            mplew.write(bNoAction);
            mplew.writeShort(pMob);
            mplew.writeShort(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] pvpSummonAttack(int cid, int playerLevel, int oid, int animation, Point pos,
                List<AttackPair> attack) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedAttackPvP.getValue());
            mplew.writeInt(cid);
            mplew.writeInt(oid);
            mplew.write(playerLevel);
            mplew.write(animation);
            mplew.writePos(pos);
            mplew.writeInt(0);
            mplew.write(attack.size());
            for (AttackPair p : attack) {
                mplew.writeInt(p.objectid);
                mplew.writePos(p.point);
                mplew.write(p.attack.size());
                mplew.write(0);
                for (Pair<Long, Boolean> atk : p.attack) {
                    mplew.writeLong(atk.left + (!atk.right ? 0 : Long.MIN_VALUE));
                }
            }

            return mplew.getPacket();
        }

        public static byte[] summonSkill(int cid, int summonSkillId, int newStance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedSkill.getValue());
            mplew.writeInt(cid);
            mplew.writeInt(summonSkillId);
            mplew.write(newStance);

            return mplew.getPacket();
        }

        public static byte[] damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedHPTagUpdate.getValue());
            mplew.writeInt(cid);
            mplew.writeInt(summonSkillId);
            mplew.write(unkByte);
            mplew.writeInt(damage);
            mplew.writeInt(monsterIdFrom);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] activeSummoneAttack(MapleSummon summon, boolean active) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_SummonedAttackActive.getValue());
            mplew.writeInt(summon.getOwnerId());
            mplew.writeInt(summon.getObjectId());
            mplew.writeBoolean(active);
            return mplew.getPacket();
        }

    }

    public static class UIPacket {

        public static byte[] getDirectionStatus(boolean enable) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_InGameCurNodeEventEnd.getValue());
            mplew.write(enable ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] openUI(int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);

            mplew.writeShort(SendPacketOpcode.LP_UserOpenUI.getValue());
            mplew.writeInt(type);

            return mplew.getPacket();
        }

        public static byte[] openUIOption(int type, int npc) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(14);

            mplew.writeShort(SendPacketOpcode.LP_UserOpenUIWithOption.getValue());
            mplew.writeInt(type);
            mplew.writeInt(npc);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] sendRedLeaf(int points, boolean viewonly) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(10);

            mplew.writeShort(SendPacketOpcode.LP_UserOpenUIWithOption.getValue());
            mplew.writeInt(0x73);
            mplew.writeInt(points);
            mplew.write(viewonly ? 1 : 0); // if view only, then complete button is disabled

            return mplew.getPacket();
        }

        public static byte[] DublStartAutoMove() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AndroidEmotionLocal.getValue());
            mplew.write(3);
            mplew.writeInt(2);

            return mplew.getPacket();
        }

        public static byte[] lockKey(boolean enable) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SetDirectionMode.getValue());
            mplew.write(enable ? 1 : 0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        // 1 Enable 0: Disable
        public static byte[] lockUI(boolean enable) {
            return UIPacket.lockUI(enable ? 1 : 0, enable ? 1 : 0);
        }

        public static byte[] lockUI(int enable, int enable2) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SetInGameDirectionMode.getValue());
            mplew.write(enable > 0 ? 1 : 0);
            if (enable > 0) {
                mplew.writeShort(enable2);
                mplew.write(enable < 0 ? 1 : 0);
            } else {
                mplew.write(enable < 0 ? 1 : 0);
            }
            return mplew.getPacket();
        }

        // 1 Enable 0: Disable
        public static byte[] disableOthers(boolean enable) {
            return disableOthers(enable, enable ? 1 : 0);
        }

        public static byte[] disableOthers(boolean enable, int enable2) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SetStandAloneMode.getValue());
            mplew.write(enable ? 1 : 0);
            if (enable) {
                mplew.writeShort(enable2);
                mplew.write(0);
            } else {
                mplew.write(!enable ? 1 : 0);
            }

            return mplew.getPacket();
        }

        public static byte[] summonHelper(boolean summon) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserHireTutor.getValue());
            mplew.write(summon ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] summonMessage(boolean isUI, int type, String message) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserTutorMsg.getValue());
            mplew.write(isUI ? 1 : 0);
            if (isUI) {
                mplew.writeInt(type);
                mplew.writeInt(7000);
            } else {
                mplew.writeMapleAsciiString(message);
                mplew.writeInt(200);
                mplew.writeInt(10000);
            }

            return mplew.getPacket();
        }

        public static byte[] getDirectionInfo(InGameDirectionEventOpcode type, int value) {
            return getDirectionInfo(type, value, 0);
        }

        public static byte[] getDirectionInfo(InGameDirectionEventOpcode type, int value, int value2) {
            return getDirectionEffect(type, null, new int[]{value, value2, 0, 0, 0, 0, 0, 0});
        }

        public static byte[] getDirectionInfo(String data, int value, int x, int y, int a, int b) {
            return getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay, data,
                    new int[]{value, x, y, a, b, 0, 0, 0});
        }

        public static byte[] getDirectionEffect(String data, int value, int x, int y) {
            return getDirectionEffect(data, value, x, y, 0);
        }

        static byte[] getDirectionEffect(String data, int value, int x, int y, int npc) {
            // [02] [02 00 31 31] [84 03 00 00] [00 00 00 00] [88 FF FF FF] [01] [00 00 00
            // 00] [01] [29 C2 1D 00] [00] [00]
            // [mod] [data ] [value ] [value2 ] [value3 ] [a1] [a3 ] [a2] [npc ] [ ] [a4]
            return getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_EffectPlay, data,
                    new int[]{value, x, y, 1, 1, 0, 0, npc});
        }

        public static byte[] getDirectionInfoNew(byte x, int value) {
            return getDirectionInfoNew(x, value, 0, 0);
        }

        public static byte[] getDirectionInfoNew(byte x, int value, int a, int b) {
            // [mod] [data] [value] [value2] [value3] [a1] ....
            return getDirectionEffect(InGameDirectionEventOpcode.InGameDirectionEvent_CameraMove, null,
                    new int[]{x, value, a, b, 0, 0, 0, 0});
        }

        // int value, int value2, int value3, int a1, int a2, int a3, int a4, int npc
        public static byte[] getDirectionEffect(InGameDirectionEventOpcode type, String data, int[] value) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserInGameDirectionEvent.getValue());
            mplew.write(type.getValue());
            switch (type) {
                case InGameDirectionEvent_ForcedAction:
                    mplew.writeInt(value[0]);
                    if (value[0] <= 0x553) {
                        mplew.writeInt(value[1]);
                    }
                    break;
                case InGameDirectionEvent_Delay:
                    mplew.writeInt(value[0]);
                    break;
                case InGameDirectionEvent_EffectPlay:
                    mplew.writeMapleAsciiString(data);
                    mplew.writeInt(value[0]);
                    mplew.writeInt(value[1]);
                    mplew.writeInt(value[2]);
                    mplew.write(value[3]);
                    if (value[3] > 0) {
                        mplew.writeInt(value[5]);
                    }
                    mplew.write(value[4]);
                    if (value[4] > 0) {
                        mplew.writeInt(value[6]);
                        mplew.write(value[6] > 0 ? 0 : 1); // 暫時解決
                        mplew.write(value[7]);
                    }
                    break;
                case InGameDirectionEvent_ForcedInput:
                    mplew.writeInt(value[0]);
                    break;
                case InGameDirectionEvent_PatternInputRequest:
                    mplew.writeMapleAsciiString(data);
                    mplew.writeInt(value[0]);
                    mplew.writeInt(value[1]);
                    mplew.writeInt(value[2]);
                    break;
                case InGameDirectionEvent_CameraMove:
                    mplew.write(value[0]);
                    mplew.writeInt(value[1]);
                    if (value[1] > 0) {
                        if (value[0] == 0) {
                            mplew.writeInt(value[2]);
                            mplew.writeInt(value[3]);
                        }
                    }
                    break;
                case InGameDirectionEvent_CameraOnCharacter:
                    mplew.write(value[0]);
                    break;
                case InGameDirectionEvent_CameraZoom:
                    mplew.writeInt(value[0]);
                    mplew.writeInt(value[1]);
                    mplew.writeInt(value[2]);
                    mplew.writeInt(value[3]);
                    mplew.writeInt(value[4]);
                    break;
                case InGameDirectionEvent_CameraReleaseFromUserPoint:
                    // CCameraWork::ReleaseCameraFromUserPoint
                    break;
                case InGameDirectionEvent_VansheeMode:// 是否隱藏角色[1 - 隱藏, 0 - 顯示]
                    mplew.write(value[0]);
                    break;
                case InGameDirectionEvent_FaceOff:
                    mplew.writeInt(value[0]);
                    break;
                case InGameDirectionEvent_Monologue:
                    mplew.writeMapleAsciiString(data);
                    mplew.write(value[0]);
                    break;
                case InGameDirectionEvent_MonologueScroll:
                    mplew.writeMapleAsciiString(data);
                    mplew.write(value[0]);
                    mplew.writeShort(value[1]);
                    mplew.writeInt(value[2]);
                    mplew.writeInt(value[3]);
                    break;
                case InGameDirectionEvent_AvatarLookSet:
                    mplew.write(value[0]);
                    for (int i = 0; i >= value[0]; i++) {
                        mplew.writeInt(value[1]); // 要重寫
                    }
                    break;
                case InGameDirectionEvent_RemoveAdditionalEffect:
                    break;
                case InGameDirectionEvent_ForcedMove:
                    mplew.writeInt(value[0]);
                    mplew.writeInt(value[1]);
                    break;
                case InGameDirectionEvent_ForcedFlip:
                    mplew.writeInt(value[0]);
                    break;
                case InGameDirectionEvent_InputUI:
                    mplew.write(value[0]);
                    break;
                default:
                    System.out.println("CField.getDirectionInfo() is Unknow mod :: [" + type + "]");
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] getDirectionFacialExpression(int expression, int duration) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserEmotionLocal.getValue());
            mplew.writeInt(expression);
            mplew.writeInt(duration);
            mplew.write(0);

            /*
             * Facial Expressions: 0 - Normal 1 - F1 2 - F2 3 - F3 4 - F4 5 - F5 6 - F6 7 -
             * F7 8 - Vomit 9 - Panic 10 - Sweetness 11 - Kiss 12 - Wink 13 - Ouch! 14 - Goo
             * goo eyes 15 - Blaze 16 - Star 17 - Love 18 - Ghost 19 - Constant Sigh 20 -
             * Sleepy 21 - Flaming hot 22 - Bleh 23 - No Face
             */
            return mplew.getPacket();
        }

        public static byte[] moveScreen(int x) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.MOVE_SCREEN_X.getValue());
            mplew.writeInt(x);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] screenDown() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.MOVE_SCREEN_DOWN.getValue());

            return mplew.getPacket();
        }

        public static byte[] resetScreen() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.RESET_SCREEN.getValue());

            return mplew.getPacket();
        }

        public static byte[] reissueMedal(int itemId, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_MedalReissueResult.getValue());
            mplew.write(type);
            mplew.writeInt(itemId);

            return mplew.getPacket();
        }

        public static byte[] playMovie(String data, boolean show) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserPlayMovieClip.getValue());
            mplew.writeMapleAsciiString(data);
            mplew.write(show ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] playMovieURL(String data) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserPlayMovieClipURL.getValue());
            mplew.writeMapleAsciiString(data);

            return mplew.getPacket();
        }

        public static byte[] setRedLeafStatus(int joejoe, int hermoninny, int littledragon, int ika) {
            // packet made to set status
            // should remove it and make a handler for it, it's a recv opcode
            /*
             * slea: E2 9F 72 00 5D 0A 73 01 E2 9F 72 00 04 00 00 00 00 00 00 00 75 96 8F 00
             * 55 01 00 00 76 96 8F 00 00 00 00 00 77 96 8F 00 00 00 00 00 78 96 8F 00 00 00
             * 00 00
             */
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            // mplew.writeShort();
            mplew.writeInt(7512034); // no idea
            mplew.writeInt(24316509); // no idea
            mplew.writeInt(7512034); // no idea
            mplew.writeInt(4); // no idea
            mplew.writeInt(0); // no idea
            mplew.writeInt(9410165); // joe joe
            mplew.writeInt(joejoe); // amount points added
            mplew.writeInt(9410166); // hermoninny
            mplew.writeInt(hermoninny); // amount points added
            mplew.writeInt(9410167); // little dragon
            mplew.writeInt(littledragon); // amount points added
            mplew.writeInt(9410168); // ika
            mplew.writeInt(ika); // amount points added

            return mplew.getPacket();
        }
    }

    public static class RunePacket {

        public static byte[] sRuneStone_ClearAndAllRegister(List<MapleRune> runes) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_RuneStoneClearAndAllRegister.getValue());

            mplew.writeInt(runes.size());
            mplew.writeInt(0);
            for (MapleRune rune : runes) {
                sRuneStone_Info(mplew, rune);
            }
            return mplew.getPacket();
        }

        public static byte[] sRuneStone_Appear(MapleRune rune) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_RuneStoneAppear.getValue());
            mplew.writeInt(rune.getObjectId()); // indexOfMapRune
            mplew.writeInt(0);
            sRuneStone_Info(mplew, rune);

            return mplew.getPacket();
        }

        static void sRuneStone_Info(final MaplePacketLittleEndianWriter mplew, final MapleRune rune) {
            mplew.writeInt(rune.getRuneType()); // ERuneStoneType
            mplew.writeInt(rune.getPosition().x);
            mplew.writeInt(rune.getPosition().y);
            mplew.write(0); // m_bFlip
        }

        public static byte[] sRuneStone_Disappear(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_RuneStoneDisappear.getValue());

            mplew.writeInt(0); // indexOfMapRune
            mplew.writeInt(chr.getId());
            mplew.writeInt(0);
            mplew.write(1);

            return mplew.getPacket();
        }

        public static byte[] runeAction(int type, int time) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_RuneStoneUseAck.getValue());
            mplew.writeInt(type);
            mplew.writeInt(time);

            return mplew.getPacket();
        }

        public static byte[] showRuneEffect(int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_RuneStoneSkillAck.getValue());
            mplew.writeInt(type);

            return mplew.getPacket();
        }
    }

    public static class ZeroPacket {

        public static byte[] gainWeaponPoint(int gain) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_Message.getValue());
            mplew.write(MessageOpcode.MS_IncWPMessage.getValue());
            mplew.writeInt(gain);

            return mplew.getPacket();
        }

        public static byte[] updateWeaponPoint(int wp) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ZeroWP.getValue());
            mplew.writeInt(wp);

            return mplew.getPacket();
        }

        public static byte[] UseWeaponScroll(int Success) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EgoEquipCheckUpgradeItemResult.getValue());
            mplew.writeShort(1);
            mplew.write(0);
            mplew.writeInt(Success);

            return mplew.getPacket();
        }

        public static byte[] UseWeaponScrollStart() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EgoEquipGaugeCompleteReturn.getValue());

            return mplew.getPacket();
        }

        public static byte[] OpenWeaponUI(int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EgoEquipCreateUpgradeItemCostInfo.getValue());
            mplew.writeInt(type);
            mplew.writeInt((type == 1) ? 100000 : 50000);
            mplew.writeInt((type == 1) ? 600 : 500);
            mplew.write(0);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] OpenZeroUpgrade(int type, int level, int action, int weapon) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_InheritanceComplete.getValue());
            mplew.write(0);
            mplew.write(action);
            mplew.writeInt(type);
            mplew.writeInt(level);
            mplew.writeInt(weapon + 10001);
            mplew.writeInt(weapon + 1);

            return mplew.getPacket();
        }

        public static byte[] MultTag(MapleCharacter chr, int Gender) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ZeroTag.getValue());
            mplew.writeInt(chr.getId());
            PacketHelper.AvatarLook__Decode(mplew, chr, false, (Gender != 0));
            int v7 = 2;
            do {
                mplew.writeInt(0);
                while (true) {
                    int res = 255;
                    mplew.write(res);
                    if (res == 255) {
                        break;
                    }
                    mplew.writeInt(0);
                }
                v7 += 36;
            } while (v7 < 74);

            return mplew.getPacket();
        }
    }

    public static class EffectPacket {

        public static byte[] showEffect(boolean self, MapleCharacter chr, UserEffectOpcode effect, int[] value,
                String[] str, Map<Integer, Integer> itemMap, Item[] items) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            if (!self && chr != null) {
                mplew.writeShort(SendPacketOpcode.LP_UserEffectRemote.getValue());
                mplew.writeInt(chr.getId());
            } else {
                mplew.writeShort(SendPacketOpcode.LP_UserEffectLocal.getValue());
            }
            mplew.write(effect.getValue());

            switch (effect) { // 排序根據IDA
                // SWITCH 開始
                case UserEffect_ReservedEffectRepeat:
                    mplew.writeMapleAsciiString(str[0]); // sItemName
                    mplew.write(value[0]);
                    if (value[0] == 0) {
                        mplew.writeInt(value[1]); // tDuration / 1000
                        mplew.writeInt(value[2]); // rx
                        mplew.writeInt(value[3]); // ry
                        break;
                    } else {
                        mplew.write(value[1]);
                        if (value[1] == 0) {
                            break;
                        } else {
                            mplew.write(value[2]); // boolean
                            mplew.writeInt(value[3]); // nTextY
                            mplew.writeInt(value[4]); // bFlip
                        }
                    }
                    break;
                case UserEffect_ReservedEffect:
                    mplew.write(value[0]); // boolean bFlip
                    mplew.writeInt(value[1]); // nRange
                    mplew.writeInt(value[2]); // nNameHeight
                    mplew.writeMapleAsciiString(str[0]); // sMsg
                    break;
                case UserEffect_AvatarOriented:
                    mplew.writeMapleAsciiString(str[0]); // sItemTypeName
                    break;
                case UserEffect_AvatarOrientedRepeat:
                    mplew.write(value[0]);
                    if (value[0] != 0) {
                        mplew.writeMapleAsciiString(str[0]); // sItemTypeName
                        mplew.writeInt(value[1]); // x
                        mplew.writeInt(value[2]); // y
                    }
                    break;
                case UserEffect_AvatarOrientedMultipleRepeat:
                    mplew.writeMapleAsciiString(str[0]); // sItemTypeName
                    mplew.writeInt(value[0]); // x
                    mplew.writeInt(value[1]); // y
                    break;
                case UserEffect_PlaySoundWithMuteBGM:
                    mplew.writeMapleAsciiString(str[0]); // sMsg
                    break;
                // SWITCH DEFAULT IF
                case UserEffect_FadeInOut:
                    mplew.writeInt(value[0]); // tFadeIn
                    mplew.writeInt(value[1]); // tDelay
                    mplew.writeInt(value[2]); // tFadeOut
                    mplew.write(value[3]); // y
                    break;
                case UserEffect_BlindEffect:
                    mplew.write(value[0]); // bOn
                    break;
                case UserEffect_PlayExclSoundWithDownBGM:
                    mplew.writeMapleAsciiString(str[0]); // sMsg
                    break;
                // SWITCH結束 IF開始
                case UserEffect_SpeechBalloon:
                    mplew.write(value[0]); // boolean bNormal
                    mplew.writeInt(value[1]); // nRange
                    mplew.writeInt(value[2]); // nNameHeight
                    mplew.writeMapleAsciiString(str[0]); // sMsg
                    mplew.writeInt(value[3]); // nSLV
                    mplew.writeInt(value[4]); // pLayer.m_pInterface
                    mplew.writeInt(value[5]); // pBaseFont.m_pInterface
                    mplew.writeInt(value[6]);
                    mplew.writeInt(value[7]); // sItemTypeName._m_pStr
                    mplew.writeInt(value[8]); // sMsg.y
                    mplew.writeInt(value[9]); // NPCID bFlip
                    break;
                case UserEffect_TextEffect:
                    mplew.writeMapleAsciiString(str[0]); // sItemTypeName
                    mplew.writeInt(value[0]);
                    mplew.writeInt(value[1]); // nTextWidth
                    mplew.writeInt(value[2]); // nNameHeight
                    mplew.writeInt(value[3]); // nNameWidth
                    mplew.writeInt(value[4]); // nTextHeight
                    mplew.writeInt(value[5]); // nBaseHeight
                    mplew.writeInt(value[6]); // nTextY
                    mplew.writeInt(value[7]); // bFlip
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                case UserEffect_Aiming:
                    mplew.writeInt(0); // NPC
                    mplew.writeInt(0);
                    mplew.writeInt(0); // nPlateNo
                    mplew.writeInt(0); // nRange
                    mplew.writeInt(0); // nNameHeight
                    mplew.writeInt(0); // bFlip
                    break;
                case UserEffect_PickUpItem:
                    PacketHelper.GW_ItemSlotBase_Decode(mplew, items[0], null);
                    break;
                case UserEffect_BiteAttack_ReceiveSuccess:
                    break;
                case UserEffect_BiteAttack_ReceiveFail:
                    break;
                // IF結束 SWITCH 開始
                case UserEffect_JobEffect:
                    break;
                case UserEffect_LevelUp:
                    break;
                case UserEffect_SkillUse:
                case UserEffect_SkillUseBySummoned:
                    if (effect.getValue() == UserEffectOpcode.UserEffect_SkillUseBySummoned.getValue()) {
                        mplew.writeInt(0); // sMsg.x
                    }
                    int skill = 0;
                    mplew.writeInt(skill); // sLog._m_pStr
                    mplew.write(0); // nCharLevel
                    mplew.write(0); // nSLV
                    if (skill == 22170074) {
                        mplew.write(0); // v2->m_pDragon.p
                    } else if (skill == 1320016) {
                        mplew.write(false);
                    } else if (skill == 4331006) {
                        mplew.write(false); // bLightnessOn
                        mplew.writeInt(0);
                    } else {
                        if (skill == 3211010 || skill == 3111010 || skill == 1100012) {
                            mplew.write(0); // nRet
                            mplew.writeInt(0); // nNameHeight
                            mplew.writeInt(0); // bFlip
                            return mplew.getPacket();
                        }
                        if (skill == 35001006) {
                            return mplew.getPacket();
                        }
                        if (skill == 91001020 || skill == 91001021 || skill == 91001017 || skill == 91001018) {
                            return mplew.getPacket();
                        }
                        if (skill == 33111007) {
                            return mplew.getPacket();
                        }
                        if (skill == 30001062) {
                            mplew.write(false); // bLightnessOn
                            mplew.writeShort(0); // pCurrentItemSlot.baseclass_0.baseclass_0.dummy[0]
                            mplew.writeShort(0); // pCurrentItemSlot.p
                            return mplew.getPacket();
                        }
                        if (skill == 30001064) {
                            mplew.write(0);
                            return mplew.getPacket();
                        }
                        if (skill == 60001218 || skill == 60011218) {
                            mplew.writeInt(0); // nRet
                            mplew.writeInt(0); // m_ptRopeConnectDest.x
                            mplew.writeInt(0); // m_ptRopeConnectDest.y
                            return mplew.getPacket();
                        }
                        if (skill == 20041222 || skill == 15001021 || skill == 20051284) {
                            mplew.writeInt(0); // m_ptBlinkLightOrigin.x
                            mplew.writeInt(0); // m_ptBlinkLightOrigin.y
                            mplew.writeInt(0); // m_ptBlinkLightDest.x
                            mplew.writeInt(0); // m_ptBlinkLightDest.y
                            return mplew.getPacket();
                        }
                    }
                    if (SkillConstants.is_super_nova_skill(skill)) {
                        mplew.writeInt(0); // sMsg.x
                        mplew.writeInt(0); // sMsg.y
                        return mplew.getPacket();
                    }
                    if (skill == 80001851) {
                        return mplew.getPacket();
                    }
                    if (skill != 12001027 && skill != 12001028) {
                        if (skill == 142121008) {
                            return mplew.getPacket();
                        }
                        if (SkillConstants.is_rw_multi_charge_skill(skill)) {
                            mplew.writeInt(0);
                            return mplew.getPacket();
                        }
                        if (SkillConstants.is_unregisterd_skill(skill)) {
                            mplew.write(0);
                            return mplew.getPacket();
                        }
                        if (skill < 101100100 || skill > 101100101) {
                        } else {
                            if (SkillConstants.is_match_skill(false, skill)) {
                                return mplew.getPacket();
                            }
                            return mplew.getPacket();
                        }
                    }
                    break;
                case UserEffect_SkillAffected:
                    skill = value[0];
                    byte skillLevel = (byte) value[1];
                    mplew.writeInt(skill); // sItemTypeName._m_pSt
                    mplew.write(skillLevel);
                    if (skill == 25121006 || skill == 31111003) {
                        mplew.writeInt(value[2]);
                    }
                    break;
                case UserEffect_SkillAffected_Ex:
                    mplew.writeInt(value[0]);
                    mplew.write(value[1]);
                    mplew.writeInt(value[2]);
                    mplew.writeInt(value[3]);
                    break;
                case UserEffect_SkillSpecialAffected:
                    mplew.writeInt(value[0]);
                    mplew.write(value[1]);
                    break;
                case UserEffect_SkillAffected_Select:
                    mplew.writeInt(value[0]);
                    mplew.writeInt(value[1]);
                    mplew.writeInt(value[2]);
                    mplew.write(value[3]);
                    mplew.write(value[4]);
                    break;
                case UserEffect_SkillSpecial:
                    mplew.writeInt(value[0]);
                    if (value[0] == 11121013 || value[0] == 12100029 || value[0] == 13121009
                            || value[0] == 36110005 || value[0] == 65101006) {
                        mplew.writeInt(value[1]);
                        mplew.writeInt(value[2]);
                        mplew.writeInt(value[3]);
                    }
                    if (value[0] == 32111016) {
                        mplew.write(value[1]);
                        mplew.write(value[2]);
                        mplew.writeInt(value[3]); // m_ptBlinkLightOrigin.x
                        mplew.writeInt(value[4]); // m_ptBlinkLightOrigin.y
                        mplew.writeInt(value[5]); // m_ptBlinkLightDest.x
                        mplew.writeInt(value[6]); // m_ptBlinkLightDest.y
                    }
                    break;
                case UserEffect_SkillPreLoopEnd:
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                case UserEffect_Resist:
                    break;
                case UserEffect_Quest:
                    mplew.write(itemMap.size());
                    int v3 = 0;
                    if (itemMap.size() <= v3) {
                        mplew.writeMapleAsciiString("");
                        mplew.writeInt(0);
                    }
                    itemMap.forEach((key, value1) -> {
                        mplew.writeInt(key);
                        mplew.writeInt(value1);
                    });
                    break;
                case UserEffect_LotteryUIResult:
                    mplew.write(0);
                    mplew.write(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                case UserEffect_Pet:
                    mplew.write(value[0]);
                    mplew.writeInt(value[1]); // pet index
                    if (chr == null) {
                        mplew.writeInt(value[2]);
                    }
                    break;
                case UserEffect_ProtectOnDieItemUse:
                    mplew.writeInt(value[0]); // 1 = 護身符, 2 = 紡織輪, 4 = 戰鬥機器人
                    mplew.write(value[1]); // 剩餘次數
                    mplew.write(value[2]);
                    switch (value[0]) {
                        case 1:
                        case 2:
                            break;
                        default:
                            mplew.writeInt(value[3]); // ItemId
                    }
                    break;
                case UNK_D:
                    mplew.writeInt(0);
                    break;
                case UNK_27:
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.write(0);
                    break;
                case UserEffect_PlayPortalSE:
                case UserEffect_JobChanged:
                case UserEffect_QuestComplete:
                    break;
                case UserEffect_ActQuestComplete:
                    // if (!CLoadMemoryMan::IsExceptFunc(TSingleton<CLoadMemoryMan>::ms_pInstance,
                    // 1)) {
                    mplew.writeInt(0); // 可能是任務ID
                    // }
                    break;
                case UserEffect_IncDecHPEffect:
                    mplew.write(value[0]);
                    break;
                case UserEffect_SoulStoneUse:
                    mplew.writeInt(value[0]);
                    break;
                case UserEffect_BattlePvP_IncDecHp:
                    mplew.writeInt(0);
                    mplew.writeMapleAsciiString("");
                    break;
                case UserEffect_IncDecHPEffect_EX:
                    mplew.writeInt(value[0]);
                    mplew.write(0);
                    break;
                case UserEffect_BuffItemEffect:
                    mplew.writeInt(0);
                    break;
                case UserEffect_SquibEffect:
                    mplew.writeMapleAsciiString("");
                    break;
                case UserEffect_LotteryUse:
                    mplew.writeInt(0);
                    mplew.write(str != null && str.length > 0 && str[0].length() > 0);
                    if (str != null && str.length > 0 && str[0].length() > 0) {
                        mplew.writeMapleAsciiString(str[0]);
                    }
                    break;
                case UserEffect_MonsterBookCardGet:
                    break;
                case UserEffect_ItemLevelUp:
                    break;
                case UserEffect_ItemMaker:
                    mplew.writeInt(value[0]);
                    break;
                case UserEffect_FieldExpItemConsumed:
                    mplew.writeInt(value[0]);
                    break;
                case UserEffect_ExpItemConsumed:
                    break;
                case UNK_23:
                    break;
                case UserEffect_UpgradeTombItemUse:
                    mplew.write(value[0]);
                    break;
                case UserEffect_BattlefieldItemUse:
                    // if (get_field() && get_field()->m_nType == 19)
                    mplew.writeMapleAsciiString(str[0]);
                    break;
                case UserEffect_IncDecHPRegenEffect:
                    mplew.writeInt(value[0]);
                    break;
                case UNK_28:
                case UNK_29:
                case UNK_2A:
                case UNK_2B:
                    break;
                case UserEffect_IncubatorUse:
                    mplew.writeInt(value[0]);
                    mplew.writeMapleAsciiString(str[0]);
                    break;
                case UserEffect_EffectUOL:
                    mplew.writeMapleAsciiString(str[0]);
                    mplew.write((byte) value[0]);
                    mplew.writeInt(value[1]);
                    mplew.writeInt(value[2]);
                    if (value[2] == 2) {
                        mplew.writeInt(value[3]);
                    }
                    break;
                case UserEffect_PvPRage:
                    mplew.writeInt(0);
                    break;
                case UserEffect_PvPChampion:
                    mplew.writeInt(0);
                    break;
                case UserEffect_PvPGradeUp:
                    break;
                case UserEffect_PvPRevive:
                    break;
                case UserEffect_MobSkillHit:
                    mplew.writeInt(value[0]);
                    mplew.writeInt(value[1]);
                    break;
                case UserEffect_AswanSiegeAttack:
                    mplew.write(value[0]);
                    break;
                case UserEffect_BossShieldCount:
                    mplew.writeInt(value[0]);
                    mplew.writeInt(value[1]);
                    break;
                case UserEffect_ResetOnStateForOnOffSkill:
                    break;
                case UserEffect_JewelCraft:
                    mplew.write(value[0]);
                    switch (value[0]) {
                        case 5:
                            break;
                        case 0:
                        case 2:
                        case 3:
                            mplew.writeInt(value[1]);
                            break;
                        case 1:
                        case 4:
                            mplew.writeInt(value[1]);
                            break;
                    }
                    break;
                case UserEffect_ConsumeEffect:
                    mplew.writeInt(value[0]);
                    break;
                case UserEffect_PetBuff:
                    break;
                case UserEffect_LeftMonsterNumber:
                    mplew.writeInt(0);
                    break;
                case UserEffect_RobbinsBomb:
                    boolean res3f = false;
                    mplew.write(res3f);
                    if (!res3f) {
                        mplew.writeInt(0);
                        mplew.write(0);
                    }
                    break;
                case UserEffect_SkillMode:
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                case UserEffect_Point:
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                case UserEffect_IncDecHPEffect_Delayed:
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                case UserEffect_Lightness:
                    boolean v1 = false;
                    mplew.write(v1);
                    if (v1) {
                        mplew.writeInt(0);
                    }
                    break;
                case User_ActionSetUsed:
                    mplew.writeShort(value[0]);
                    mplew.writeInt(value[1]);
                    mplew.write(value[2]);
                    mplew.write(value[3]);
                    mplew.write(value[4]);
                    break;
                case UNK_47:
                    // if unc {
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    // }
                    break;
                case UNK_48:
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    break;
                case UNK_2C:
                    mplew.writeInt(0);
                    break;
                case UNK_2D:
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] showLevelupEffect(MapleCharacter chr) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_LevelUp, null, null, null, null);
        }

        public static byte[] showDiceEffect(int skillid, int effectid, int effectid2, int level) {
            return showDiceEffect(null, skillid, effectid, effectid2, level);
        }

        public static byte[] showDiceEffect(MapleCharacter chr, int skillid, int effectid, int effectid2, int level) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_SkillAffected_Select,
                    new int[]{effectid, effectid2, skillid, level, 0}, null, null, null);
        }

        public static byte[] getShowItemGain(Map<Integer, Integer> items) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_Quest, null, null, items, null);
        }

        public static byte[] showPetLevelUp(byte index) {
            return showPetLevelUp(null, index);
        }

        public static byte[] showPetLevelUp(MapleCharacter chr, byte index) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_Pet, new int[]{0, index, 0}, null, null,
                    null);
        }

        public static byte[] showBlackBlessingEffect(MapleCharacter chr, int value) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_SkillSpecial,
                    new int[]{value, 0, 0, 0, 0, 0, 0, 0}, null, null, null);
        }

        public static byte[] useAmulet(int amuletType, byte timesleft, byte daysleft, int itemId) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_ProtectOnDieItemUse,
                    new int[]{amuletType, timesleft, daysleft, itemId}, null, null, null);
        }

        public static byte[] Mulung_DojoUp() {
            return showEffect(true, null, UserEffectOpcode.UserEffect_PlayPortalSE, null, null, null, null);
        }

        public static byte[] showJobChangeEffect(MapleCharacter chr) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_JobChanged, null, null, null, null);
        }

        public static byte[] showQuetCompleteEffect() {
            return showQuetCompleteEffect(null);
        }

        public static byte[] showQuetCompleteEffect(MapleCharacter chr) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_QuestComplete, null, null, null, null);
        }

        public static byte[] showHealed(int amount) {
            return EffectPacket.showHealed(null, amount);
        }

        public static byte[] showHealed(MapleCharacter chr, int amount) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_IncDecHPEffect_EX, new int[]{amount},
                    null, null, null);
        }

        public static byte[] showMonsterBookEffect() {
            return showEffect(true, null, UserEffectOpcode.UserEffect_MonsterBookCardGet, null, null, null, null);
        }

        public static byte[] showRewardItemAnimation(int itemId, String effect) {
            return showRewardItemAnimation(itemId, effect, null);
        }

        public static byte[] showRewardItemAnimation(int itemId, String effect, MapleCharacter chr) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_LotteryUse, new int[]{itemId},
                    new String[]{effect}, null, null);
        }

        public static byte[] showItemLevelupEffect() {
            return showItemLevelupEffect(null);
        }

        public static byte[] showItemLevelupEffect(MapleCharacter chr) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_ItemLevelUp, null, null, null, null);
        }

        public static byte[] ItemMaker_Success() {
            return ItemMaker_Success(null);
        }

        public static byte[] ItemMaker_Success(MapleCharacter chr) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_ItemMaker, null, null, null, null);
        }

        public static byte[] showDodgeChanceEffect() {
            return showEffect(true, null, UserEffectOpcode.UserEffect_ExpItemConsumed, null, null, null, null);
        }

        public static byte[] showWZEffect(String data) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_ReservedEffect, new int[]{0, 0, 0},
                    new String[]{data}, null, null);
        }

        public static byte[] showWZEffectNew(String data) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_AvatarOriented, null, new String[]{data}, null,
                    null);
        }

        public static byte[] TutInstructionalBalloon(String data) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserEffectLocal.getValue());
            mplew.write(UserEffectOpcode.UserEffect_AvatarOriented.getValue());// ?
            mplew.writeMapleAsciiString(data);
            mplew.writeInt(1);

            return mplew.getPacket();
        }

        public static byte[] playSoundEffect(String data) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_PlaySoundWithMuteBGM, null, new String[]{data},
                    null, null);
        }

        public static byte[] playVoiceEffect(String data) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_PlayExclSoundWithDownBGM, null,
                    new String[]{data}, null, null);
        }

        public static byte[] showCashItemEffect(int itemId) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_SoulStoneUse, new int[]{itemId}, null, null,
                    null);
        }

        public static byte[] showChampionEffect() {
            return EffectPacket.showChampionEffect(null);
        }

        static byte[] showChampionEffect(MapleCharacter chr) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_PvPChampion, new int[]{0x7530}, null, null,
                    null);
        }

        public static byte[] showCraftingEffect(String effect, byte direction, int time, int mode) {
            return EffectPacket.showCraftingEffect(null, effect, direction, time, mode);
        }

        public static byte[] showCraftingEffect(MapleCharacter chr, String effect, byte direction, int time, int mode) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_EffectUOL, new int[]{direction, time, mode, 0},
                    new String[]{effect}, null, null);
        }

        public static byte[] showWeirdEffect(String effect, int itemId) {
            return showWeirdEffect(null, effect, itemId);
        }

        public static byte[] showWeirdEffect(MapleCharacter chr, String effect, int itemId) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_EffectUOL, new int[]{1, 0, 2, itemId},
                    new String[]{effect}, null, null);
        }

        public static byte[] showBlackBGEffect(int value, int value2, int value3, byte value4) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_FadeInOut,
                    new int[]{value, value2, value3, value4}, null, null, null);
        }

        public static byte[] unsealBox(int reward) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserEffectLocal.getValue());
            mplew.write(UserEffectOpcode.UserEffect_MobSkillHit.getValue());
            mplew.write(1);
            mplew.writeInt(reward);
            mplew.writeInt(1);

            return mplew.getPacket();
        }

        public static byte[] showDarkEffect(boolean dark) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_BlindEffect, new int[]{dark ? 1 : 0}, null,
                    null, null);
        }

        public static byte[] showRechargeEffect() {
            return showEffect(true, null, UserEffectOpcode.UserEffect_ResetOnStateForOnOffSkill, null, null, null,
                    null);
        }

        public static byte[] showWZEffect3(String data, int[] value) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_ReservedEffectRepeat, value,
                    new String[]{data}, null, null);
        }

        public static byte[] showFireEffect(MapleCharacter chr) {
            return showEffect(chr == null, chr, UserEffectOpcode.UserEffect_SkillPreLoopEnd, null, null, null, null);
        }

        public static byte[] showItemTopMsgEffect(Item item) {
            return showEffect(true, null, UserEffectOpcode.UserEffect_PickUpItem, null, null, null,
                    new Item[]{item});
        }

        public static byte[] showBuffEffect(boolean self, MapleCharacter chr, int skillid, UserEffectOpcode effect,
                int playerLevel, int skillLevel) {
            return showBuffEffect(self, chr, skillid, effect, playerLevel, skillLevel, (byte) 3);
        }

        public static byte[] showBuffEffect(boolean self, MapleCharacter chr, int skillid, UserEffectOpcode effect,
                int playerLevel, int skillLevel, byte direction) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            switch (effect) {
                case UserEffect_SkillUse:
                case UserEffect_SkillUseBySummoned:
                    break;
                default:
                    System.err.println("未處理的Buff效果" + effect.name());
                    return mplew.getPacket();
            }

            if (!self && chr != null) {
                mplew.writeShort(SendPacketOpcode.LP_UserEffectRemote.getValue());
                mplew.writeInt(chr.getId());
            } else {
                mplew.writeShort(SendPacketOpcode.LP_UserEffectLocal.getValue());
            }

            mplew.write(effect.getValue());

            if (effect == UserEffectOpcode.UserEffect_SkillUseBySummoned) {
                mplew.writeInt(0);
            }
            mplew.writeInt(skillid);
            mplew.write(playerLevel);

            if (skillid == 51100006) {
                return mplew.getPacket();
            }

            mplew.write(skillLevel);

            if (skillid == 22160000) { // 龍神的護佑
                mplew.write(0);
            } else if (skillid == 1320016) { // 轉生
                mplew.write(0); // boolean
            } else {
                if (skillid == 4331006) { // 隱‧鎖鏈地獄
                    mplew.write(0);
                    mplew.writeInt(0);
                }
                if (skillid == 3211010 || skillid == 3111010) { // 飛影位移
                    mplew.write(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    return mplew.getPacket();
                }
                if (skillid == 35001006) { // 火箭推進器
                    return mplew.getPacket();
                }
                if (skillid == 91001020 || skillid == 91001021 || skillid == 91001017 || skillid == 91001018) { // 起來吧,勇士
                    // ||
                    // 我還想再跑
                    // ||
                    // 前線召喚
                    // ||
                    // 公會定期聚會
                    return mplew.getPacket();
                }
                if (skillid == 33111007) { // 狂獸附體
                    return mplew.getPacket();
                }
                if (skillid == 30001062) { // 獵人的呼喚
                    mplew.write(0);
                    mplew.writeShort(0);
                    mplew.writeShort(0);
                    return mplew.getPacket();
                }
                if (skillid == 30001061) { // 捕獲
                    mplew.write(0);
                    mplew.writeShort(0);
                    mplew.writeShort(0);
                    return mplew.getPacket();
                }
                if (skillid == 60001218 || skillid == 60011218) { // 縱向連接 || 魔法起重機
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    return mplew.getPacket();
                }
                if (skillid == 20041222 || skillid == 15001021 || skillid == 20051284 || skillid == 5081021) { // 星光順移
                    // || 閃光
                    // || 縮地
                    // ||
                    // 縱步突打
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    return mplew.getPacket();
                }
                if (skillid == 4221052 || skillid == 65121052) { // IDA_FUC 暗影霧殺 || 超級超新星
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    return mplew.getPacket();
                }
                if (skillid == 12001027 || skillid == 12001028) { // 火步行
                    return mplew.getPacket();
                }
                if (skillid == 80011068) { // 紫扇仰波‧烈
                    return mplew.getPacket();
                }
                if (skillid == 80001132) { // 獵殺殭屍
                    mplew.write(0);
                    return mplew.getPacket();
                }
            }

            return mplew.getPacket();
        }
    }

    public static class Bingo {

        public static byte[] sendBingoCard(int stage, List<Byte> card) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_CARD.getValue());
            mplew.writeInt(stage);
            if (stage == 1) {
                mplew.writeInt(1);
                mplew.writeInt(6);// message size
                mplew.writeMapleAsciiString("請稍等一下呦.");
                mplew.writeMapleAsciiString("號碼出來後請用滑鼠點擊呦.");
                mplew.writeMapleAsciiString("賓果完成後請按下賓果按鈕呦.");
                mplew.writeMapleAsciiString("I say 賓! U say 果! 賓! 果! 賓! 果!");
                mplew.writeMapleAsciiString("覺得賓果遊戲好好玩的話請尖叫~呦.");
                mplew.writeMapleAsciiString("遊戲即將開始.");
            } else {
                mplew.writeInt(0);
            }
            mplew.writeInt(5);
            mplew.writeInt(5);
            mplew.writeInt(25);
            for (byte number : card) {
                mplew.writeInt(number);
            }
            return mplew.getPacket();
        }

        public static byte[] sendBingoBallCall(int number, int remain) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_BALL_CALL.getValue());
            mplew.writeInt(number);
            mplew.writeInt(remain);

            return mplew.getPacket();
        }

        /* not sure. maybe sound effect things? */
        public static byte[] sendBingoResponse() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_RESPONSE.getValue());

            return mplew.getPacket();
        }

        public static byte[] sendBingoRanking(int cid, String name, int stage, int rank) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_RANKING.getValue());
            mplew.writeInt(1);
            mplew.writeInt(cid);
            mplew.writeMapleAsciiString(name);
            mplew.writeInt(stage);
            mplew.writeInt(rank);

            return mplew.getPacket();
        }

        /* no idea */
        public static byte[] sendBingoFailed(int cid, String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_RANKING.getValue());
            mplew.writeInt(0);
            mplew.writeInt(cid);
            mplew.writeMapleAsciiString(name);
            mplew.writeInt(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        /* unused */
        public static byte[] sendBingoUnk(int unk) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_UNK.getValue());
            mplew.writeInt(unk);

            return mplew.getPacket();
        }

        /* what is this used for? */
        public static byte[] sendBingoResult(int stage, int rank) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_RESULT.getValue());
            mplew.writeInt(stage);
            mplew.writeInt(rank);

            return mplew.getPacket();
        }

        public static byte[] markBingoCard(byte index, byte number, List<Byte> bingoLines) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_MARK.getValue());
            mplew.writeInt(index);
            mplew.writeInt(number);
            mplew.writeInt(0);
            mplew.writeInt(bingoLines.size());
            for (byte direction : bingoLines) {
                mplew.writeInt(direction);
            }
            return mplew.getPacket();
        }

        public static byte[] setBingoUI(int mode, int stage) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BINGO_UI.getValue());
            mplew.writeInt(mode);
            mplew.writeInt(stage);
            mplew.write(0);

            return mplew.getPacket();
        }
    }

    public static byte[] enchantResult(int result, int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.STRENGTHEN_UI.getValue());
        mplew.writeInt(result);// 0=fail/1=sucess/2=idk/3=shows stats
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] sendSealedBox(short slot, int itemId, List<Integer> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SEALED_BOX.getValue());
        mplew.writeShort(slot);
        mplew.writeInt(itemId);
        mplew.writeInt(items.size());
        for (int item : items) {
            mplew.writeInt(item);
        }

        return mplew.getPacket();
    }

    public static byte[] getRandomResponse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RANDOM_RESPONSE.getValue());
        mplew.write(12);
        mplew.writeShort(1);
        mplew.writeLong(1);
        mplew.writeInt(100);
        mplew.writeInt(GameConstants.getCurrentDate());

        return mplew.getPacket();
    }

    public static byte[] getCassandrasCollection() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CASSANDRAS_COLLECTION.getValue());
        mplew.write(6);

        return mplew.getPacket();
    }

    public static byte[] getLuckyLuckyMonstory() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobDropMesoPickup.getValue());
        mplew.writeShort(1);
        mplew.write(30);

        return mplew.getPacket();
    }

    private static void addMountId(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, int buffSrc) {
        Item c_mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -123);
        Item mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -18);
        int mountId = GameConstants.getMountItem(buffSrc, chr);
        if ((mountId == 0) && (c_mount != null)
                && (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -124) != null)) {
            mplew.writeInt(c_mount.getItemId());
        } else if ((mountId == 0) && (mount != null)
                && (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -19) != null)) {
            mplew.writeInt(mount.getItemId());
        } else {
            mplew.writeInt(mountId);
        }
    }

    public static byte[] harvestResultEffect(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_UserMakingMeisterSkillEff.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] playAmbientSound(String data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PlayAmbientSound.getValue());
        mplew.writeMapleAsciiString(data);
        mplew.writeInt(100);

        return mplew.getPacket();
    }

    public static byte[] stopAmbientSound(String data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_StopAmbientSound.getValue());
        mplew.writeMapleAsciiString(data);

        return mplew.getPacket();
    }

    private static final int CAMERA_SWITCH_TYPE_NORMAL = 0x0;
    private static final int CAMERA_SWITCH_TYPE_POSITION = 0x1;
    private static final int CAMERA_SWITCH_TYPE_BACK = 0x2;
    private static final int CAMERA_SWITCH_TYPE_POSITION_BY_CID = 0x3;

    public static byte[] cameraSwitch(String sTargetName, int tTime) {
        return cameraSwitch(CAMERA_SWITCH_TYPE_NORMAL, sTargetName, null, tTime);
    }

    public static byte[] cameraSwitchPos(int nX, int nY, int tTime) {
        return cameraSwitch(CAMERA_SWITCH_TYPE_POSITION, null, new int[]{nX, nY}, tTime);
    }

    public static byte[] cameraSwitchBack() {
        return cameraSwitch(CAMERA_SWITCH_TYPE_BACK, null, null, 0);
    }

    public static byte[] cameraSwitchPosByCID(String sTargetName, int dwCharaterID, boolean bIsSet, int tResetTime) {
        return cameraSwitch(CAMERA_SWITCH_TYPE_POSITION_BY_CID, sTargetName, new int[]{dwCharaterID, bIsSet ? 1 : 0},
                tResetTime);
    }

    private static byte[] cameraSwitch(int nType, String sTargetName, int[] values, int tTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CameraSwitch.getValue());
        mplew.write(nType);
        switch (nType) {
            case CAMERA_SWITCH_TYPE_NORMAL:
                mplew.writeMapleAsciiString(sTargetName); // sTargetName
                mplew.writeInt(tTime); // tTime
                break;
            case CAMERA_SWITCH_TYPE_POSITION:
                mplew.writeInt(values[0]); // nX
                mplew.writeInt(values[1]); // nY
                mplew.writeInt(tTime); // tTime
                break;
            case CAMERA_SWITCH_TYPE_BACK:
                break;
            case CAMERA_SWITCH_TYPE_POSITION_BY_CID:
                mplew.writeInt(values[0]); // dwCharaterID
                mplew.write(values[1]); // boolean -> Set Or Reset
                mplew.writeInt(tTime); // tResetTime
                mplew.writeMapleAsciiString(sTargetName);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] onDamageSkinSaveSuccess(MapleCharacter chr) {
        return onDamageSkinSaveResult(chr, 0, 4);
    }

    public static byte[] onDamageSkinChangeSuccess(MapleCharacter chr) {
        return onDamageSkinSaveResult(chr, 1, 4);
    }

    public static byte[] onDamageSkinSaveError(MapleCharacter chr, int code) {
        return onDamageSkinSaveResult(chr, 1, code);
    }

    public static byte[] onDamageSkinChangeError(MapleCharacter chr, int code) {
        return onDamageSkinSaveResult(chr, 2, code);
    }

    public static byte[] onDamageSkinDeleteSuccess(MapleCharacter chr) {
        return onDamageSkinSaveResult(chr, 3, 0);
    }

    public static byte[] onDamageSkinDeleteError(MapleCharacter chr, int code) {
        return onDamageSkinSaveResult(chr, 2, code);
    }

    private static byte[] onDamageSkinSaveResult(MapleCharacter chr, int action, int message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_DamageSkinSaveResult.getValue());
        mplew.write(action);
        mplew.write(message);
        // ActiveDamageSkin
        mplew.write(1);
        int damageSkin = chr.getDamageSkin();
        List<Integer> savedDamageSkins = chr.getSaveDamageSkin();
        mplew.writeInt(damageSkin);
        mplew.writeInt(ItemConstants.傷害字型.getDamageSkinItemIDBySkinID(damageSkin));
        mplew.write(savedDamageSkins.contains(damageSkin)); // bNotSaved
        mplew.writeMapleAsciiString("");
        // PremiumDamageSkin
        mplew.writeInt(-1);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.writeMapleAsciiString("");
        mplew.writeShort(29); // TODO : 帳號新增 damageSkinSlot
        mplew.writeShort(savedDamageSkins.size());
        for (Integer savedDamageSkin : savedDamageSkins) {
            mplew.writeInt(savedDamageSkin);
            mplew.writeInt(ItemConstants.傷害字型.getDamageSkinItemIDBySkinID(savedDamageSkin));
            mplew.write(0);
            mplew.writeMapleAsciiString("");
        }
        return mplew.getPacket();
    }

    public static byte[] onUserSetFieldFloating(int i, int i0, int i1, int i2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_UserSetFieldFloating.getValue());
        mplew.writeInt(i);
        mplew.writeInt(i0);
        mplew.writeInt(i1);
        mplew.writeInt(i2);
        return mplew.getPacket();
    }

    public static byte[] onAddPopupSay(int npcId, int period, String text, String type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_AddPopupSay.getValue());
        mplew.writeInt(npcId);
        mplew.writeInt(period);
        mplew.writeMapleAsciiString(text);
        mplew.writeMapleAsciiString(type);
        return mplew.getPacket();
    }

}
