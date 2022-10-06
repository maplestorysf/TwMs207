package tools.packet;

import client.BuddyList.BuddyOperation;
import client.*;
import client.MapleStat.Temp;
import client.character.stat.MapleBuffStat;
import client.character.stat.MapleDisease;
import client.familiar.MonsterFamiliar;
import client.inventory.*;
import client.skill.Skill;
import client.skill.SkillEntry;
import client.skill.SkillMacro;
import constants.EventListConstants;
import constants.GameConstants;
import constants.ItemConstants;
import constants.ServerConfig;
import extensions.temporary.GuildOpcode;
import extensions.temporary.MessageOpcode;
import handling.SendPacketOpcode;
import handling.cashshop.RoyaCoupon;
import handling.channel.DojangRankingFactory;
import handling.channel.DojangRankingFactory.DojangRankDataEntry;
import handling.channel.DojangRankingFactory.DojangRankList;
import handling.channel.MapleGeneralRanking.CandyRankingInfo;
import handling.channel.MapleGuildRanking;
import handling.world.*;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import handling.world.guild.*;
import server.*;
import server.life.PlayerNPC;
import server.stores.HiredMerchant;
import server.stores.MaplePlayerShopItem;
import tools.DateUtil;
import tools.Pair;
import tools.StringUtil;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.PacketHelper.CS_COMMODITY;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import static tools.packet.PacketHelper.addZeroInfo;

public class CWvsContext {

    public static byte[] enableActions() {
        return updatePlayerStats(new EnumMap<>(MapleStat.class), true, null);
    }

    public static byte[] updatePlayerStats(Map<MapleStat, Long> stats, MapleCharacter chr) {
        return updatePlayerStats(stats, false, chr);
    }

    public static byte[] updatePlayerStats(Map<MapleStat, Long> mystats, boolean itemReaction, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_StatChanged.getValue());
        mplew.write(itemReaction ? 1 : 0);
        mplew.write(0); // ?

        long updateMask = 0L;
        for (MapleStat statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        mplew.writeLong(updateMask);
        for (final Entry<MapleStat, Long> statupdate : mystats.entrySet()) {
            switch (statupdate.getKey()) {
                case SKIN:
                case LEVEL:
                case BATTLE_RANK:
                case ICE_GAGE:
                    mplew.write((statupdate.getValue()).byteValue());
                    break;
                case JOB:
                    mplew.writeShort((statupdate.getValue()).shortValue());
                    mplew.writeShort(chr.getSubcategory());
                    break;
                case STR:
                case DEX:
                case INT:
                case LUK:
                case AVAILABLEAP:
                case FATIGUE:
                    mplew.writeShort((statupdate.getValue()).shortValue());
                    break;
                case AVAILABLESP:
                    if (GameConstants.isSeparatedSp(chr.getJob())) {
                        mplew.write(chr.getRemainingSpSize());
                        for (int i = 0; i < chr.getRemainingSps().length; i++) {
                            if (chr.getRemainingSp(i) > 0) {
                                mplew.write(i + 1);
                                mplew.writeInt(chr.getRemainingSp(i));
                            }
                        }
                    } else {
                        mplew.writeShort(chr.getRemainingSp());
                    }
                    break;
                case TRAIT_LIMIT:
                    mplew.writeInt((statupdate.getValue()).intValue());
                    mplew.writeInt((statupdate.getValue()).intValue());
                    mplew.writeInt((statupdate.getValue()).intValue());
                    break;
                case EXP:
                case GACHAPONEXP:
                case MESO:
                    mplew.writeLong((statupdate.getValue()));
                    break;
                case PET:
                    mplew.writeLong((statupdate.getValue()).intValue());
                    mplew.writeLong((statupdate.getValue()).intValue());
                    mplew.writeLong((statupdate.getValue()).intValue());
                    break;
                default:
                    mplew.writeInt((statupdate.getValue()).intValue());
            }
        }

        mplew.write(-1);
        mplew.write(0);
        mplew.write(0);

        if ((updateMask == 0L) && (!itemReaction)) {
            mplew.write(1);
        }

        mplew.write(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] setTemporaryStats(short str, short dex, short _int, short luk, short watk, short matk,
            short acc, short avoid, short speed, short jump) {
        Map<Temp, Integer> stats = new EnumMap<>(MapleStat.Temp.class);

        stats.put(MapleStat.Temp.STR, (int) str);
        stats.put(MapleStat.Temp.DEX, (int) dex);
        stats.put(MapleStat.Temp.INT, (int) _int);
        stats.put(MapleStat.Temp.LUK, (int) luk);
        stats.put(MapleStat.Temp.WATK, (int) watk);
        stats.put(MapleStat.Temp.MATK, (int) matk);
        stats.put(MapleStat.Temp.ACC, (int) acc);
        stats.put(MapleStat.Temp.AVOID, (int) avoid);
        stats.put(MapleStat.Temp.SPEED, (int) speed);
        stats.put(MapleStat.Temp.JUMP, (int) jump);

        return temporaryStats(stats);
    }

    public static byte[] temporaryStats_Aran() {
        Map<Temp, Integer> stats = new EnumMap<>(MapleStat.Temp.class);

        stats.put(MapleStat.Temp.STR, 999);
        stats.put(MapleStat.Temp.DEX, 999);
        stats.put(MapleStat.Temp.INT, 999);
        stats.put(MapleStat.Temp.LUK, 999);
        stats.put(MapleStat.Temp.WATK, 255);
        stats.put(MapleStat.Temp.ACC, 999);
        stats.put(MapleStat.Temp.AVOID, 999);
        stats.put(MapleStat.Temp.SPEED, 140);
        stats.put(MapleStat.Temp.JUMP, 120);

        return temporaryStats(stats);
    }

    public static byte[] temporaryStats_Balrog(MapleCharacter chr) {
        Map<Temp, Integer> stats = new EnumMap<>(MapleStat.Temp.class);

        int offset = 1 + (chr.getLevel() - 90) / 20;
        stats.put(MapleStat.Temp.STR, chr.getStat().getTotalStr() / offset);
        stats.put(MapleStat.Temp.DEX, chr.getStat().getTotalDex() / offset);
        stats.put(MapleStat.Temp.INT, chr.getStat().getTotalInt() / offset);
        stats.put(MapleStat.Temp.LUK, chr.getStat().getTotalLuk() / offset);
        stats.put(MapleStat.Temp.WATK, chr.getStat().getTotalWatk() / offset);
        stats.put(MapleStat.Temp.MATK, chr.getStat().getTotalMagic() / offset);

        return temporaryStats(stats);
    }

    public static byte[] temporaryStats(Map<MapleStat.Temp, Integer> mystats) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ForcedStatSet.getValue());
        int updateMask = 0;
        for (MapleStat.Temp statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        mplew.writeInt(updateMask);
        for (final Entry<MapleStat.Temp, Integer> statupdate : mystats.entrySet()) {
            switch (statupdate.getKey()) {
            case SPEED:
            case JUMP:
            case UNKNOWN:
                mplew.write((statupdate.getValue()).byteValue());
                break;
            default:
                mplew.writeShort((statupdate.getValue()).shortValue());
            }
        }

        return mplew.getPacket();
    }

    public static byte[] onForcedStatReset() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ForcedStatReset.getValue());

        return mplew.getPacket();
    }

    public static byte[] updateVSkills(Map<Skill, SkillEntry> update, boolean clear) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_ChangeSkillRecordResult.getValue());
        boolean unk = false;
        mplew.writeBoolean(true); // get_update_time
        mplew.write(0);
        mplew.writeBoolean(unk); // masterlevel?
        mplew.writeShort(update.size());
        for (Map.Entry z : update.entrySet()) {
            Skill skill = ((Skill) z.getKey());
            SkillEntry entry = ((SkillEntry) z.getValue());
            mplew.writeInt(skill.getId());
            mplew.writeInt(clear ? 0 : entry.skillLevel);
            mplew.writeInt(entry.masterlevel);
            PacketHelper.addExpirationTime(mplew, entry.expiration);
        }
        mplew.write(/* hyper ? 0x0C : */4);
        return mplew.getPacket();
    }

    public static byte[] updateSkills(Map<Skill, SkillEntry> update, boolean hyper) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ChangeSkillRecordResult.getValue());
        boolean unk = false;
        mplew.writeBoolean(true); // get_update_time
        mplew.write(0);
        mplew.writeBoolean(unk); // masterlevel?
        mplew.writeShort(update.size());
        for (Map.Entry z : update.entrySet()) {
            Skill skill = ((Skill) z.getKey());
            SkillEntry entry = ((SkillEntry) z.getValue());
            mplew.writeInt(skill.getId());
            mplew.writeInt(entry.teachId > 0 ? entry.teachId : entry.skillLevel);
            mplew.writeInt(entry.masterlevel);
            PacketHelper.addExpirationTime(mplew, entry.expiration);
        }
        mplew.write(/* hyper ? 0x0C : */4);

        return mplew.getPacket();
    }

    public static byte[] updateZeroStats(final MapleCharacter chr, Map<MapleZeroStat, Integer> mystats) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ZeroInfo.getValue());

        if (MapleJob.is神之子(chr.getJob())) {
            addZeroInfo(mplew, chr, mystats);// 神之子訊息
        }

        return mplew.getPacket();
    }

    public static byte[] OnVMatrixUpdate(List<VMatrixRecord> aVMatrixRecord, int type, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_VMatrixUpdate.getValue());
        mplew.writeInt(aVMatrixRecord.size());
        for (VMatrixRecord pMatrix : aVMatrixRecord) {
            pMatrix.encode(mplew);
        }
        mplew.writeBoolean(type >= 0);
        if (type >= 0) {
            mplew.writeInt(type);
            switch (type) {
            case 0:
                mplew.writeInt(slot);
                break;
            }
        }
        return mplew.getPacket();
    }

    public static byte[] OnVMatrixShardUpdate(int count) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_VCoreDecomeposeResultDialog.getValue());
        mplew.writeInt(count);
        return mplew.getPacket();
    }

    public static byte[] OnVMatrixEnforceResult(int slot, int exp, int preLevel, int nextLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_VMatrixEnhanceResultDialog.getValue());
        mplew.writeInt(slot);
        mplew.writeInt(exp);
        mplew.writeInt(preLevel);
        mplew.writeInt(nextLevel);
        return mplew.getPacket();
    }

    public static byte[] OnNodeStoneResult(int nItemID, int nSkillID1, int nSkillID2, int nSkillID3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_VCoreGemstoneDialog.getValue());
        mplew.writeInt(nItemID);
        mplew.writeInt(1);
        mplew.writeInt(nSkillID1);
        mplew.writeInt(nSkillID2);
        mplew.writeInt(nSkillID3);
        return mplew.getPacket();
    }

    public static byte[] showVCoreExpiredDialog(List<VMatrixRecord> records) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_VCoreExpired.getValue());
        mplew.writeInt(records.size());
        for (VMatrixRecord record : records) {
            mplew.writeInt(record.getIconID());
        }
        return mplew.getPacket();
    }

    public static byte[] showVCoreCraftResultDialog(VMatrixRecord record) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_VMatrixCraftResultDialog.getValue());
        mplew.writeInt(record.getIconID());
        mplew.writeInt(record.getSkillLv());
        mplew.writeInt(record.getSkillID1());
        mplew.writeInt(record.getSkillID2());
        mplew.writeInt(record.getSkillID3());
        return mplew.getPacket();
    }

    public static byte[] giveFameErrorResponse(int op) {
        return OnFameResult(op, null, true, 0);
    }

    public static byte[] OnFameResult(int op, String charname, boolean raise, int newFame) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_GivePopularityResult.getValue());
        mplew.write(op);
        if ((op == 0) || (op == 5)) {
            mplew.writeMapleAsciiString(charname == null ? "" : charname);
            mplew.write(raise ? 1 : 0);
            if (op == 0) {
                mplew.writeInt(newFame);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] fullClientDownload() {
        // Opens "http://maplestory.nexon.net/support/game-download"
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FULL_CLIENT_DOWNLOAD.getValue());

        return mplew.getPacket();
    }

    public static byte[] setSonOfLinkedSkillResult(int skillId, int toChrId, String toChrName) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetSonOfLinkedSkillResult.getValue());
        mplew.writeInt(0);
        mplew.writeInt(skillId);
        mplew.writeInt(toChrId);
        mplew.writeMapleAsciiString(toChrName);

        return mplew.getPacket();
    }

    public static class AntiMacro {

        private static class AntiType {

            public static byte 未找到角色 = (byte) 0;
            public static byte 非攻擊狀態 = (byte) 1;
            public static byte 已經通過 = (byte) 2;
            public static byte 正在測試 = (byte) 3;
            public static byte 儲存截圖 = (byte) 6;
            public static byte 測謊反饋訊息 = (byte) 7;
            public static byte 認證圖片 = (byte) 8;
            public static byte 測謊失敗 = (byte) 9;
            public static byte 失敗截圖 = (byte) 10;
            public static byte 通過測謊 = (byte) 11;
            public static byte 通過訊息 = (byte) 12;
        }

        public static byte[] cantFindPlayer() {
            return antiMacroResult(AntiType.未找到角色, (byte) MapleAntiMacro.SYSTEM_ANTI, null, null, 0);
        }

        public static byte[] nonAttack() {
            return antiMacroResult(AntiType.非攻擊狀態, (byte) MapleAntiMacro.SYSTEM_ANTI, null, null, 0);
        }

        public static byte[] alreadyPass() {
            return antiMacroResult(AntiType.已經通過, (byte) MapleAntiMacro.SYSTEM_ANTI, null, null, 0);
        }

        public static byte[] antiMacroNow() {
            return antiMacroResult(AntiType.正在測試, (byte) MapleAntiMacro.SYSTEM_ANTI, null, null, 0);
        }

        public static byte[] screenshot(String str) {
            return antiMacroResult(AntiType.儲存截圖, (byte) MapleAntiMacro.SYSTEM_ANTI, str, null, 0);
        }

        public static byte[] antiMsg(int mode, String str) {
            return antiMacroResult(AntiType.測謊反饋訊息, (byte) mode, str, null, 0);
        }

        public static byte[] getImage(byte mode, File file, int times) {
            return antiMacroResult(AntiType.認證圖片, mode, null, file, times);
        }

        public static byte[] failure(int mode) {
            return antiMacroResult(AntiType.測謊失敗, (byte) mode, null, null, 0);
        }

        public static byte[] failureScreenshot(String str) {
            return antiMacroResult(AntiType.失敗截圖, (byte) MapleAntiMacro.GM_SKILL_ANTI, str, null, 0);
        }

        public static byte[] success(int mode) {
            return antiMacroResult(AntiType.通過測謊, (byte) mode, null, null, 0);
        }

        public static byte[] successMsg(int mode, String str) {
            return antiMacroResult(AntiType.通過訊息, (byte) mode, str, null, 0);
        }

        private static byte[] antiMacroResult(byte type, byte antiMode, String str, File file, int times) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AntiMacroResult.getValue());
            mplew.write(type);
            mplew.write(antiMode); // 2 = show msg/save screenshot/maple admin picture(mode 6)
            if (type == AntiType.認證圖片) {
                mplew.write(times);
                mplew.write(false); // if false time is 05:00
                mplew.writeFile(file);
                return mplew.getPacket();
            }
            if (type == AntiType.測謊失敗 || type == AntiType.通過測謊) {
            }
            if (type == AntiType.測謊失敗 || type == AntiType.通過測謊) {
            }
            if (type == AntiType.儲存截圖) { // save screenshot
                mplew.writeMapleAsciiString(str); // file name
                return mplew.getPacket();
            }
            if (type != AntiType.測謊反饋訊息) {
                if (type == AntiType.通過訊息) {
                    mplew.writeMapleAsciiString(str); // passed lie detector message
                } else {
                    if (type != AntiType.失敗截圖) {
                        return mplew.getPacket();
                    }
                    mplew.writeMapleAsciiString(str); // failed lie detector, file name (for screenshot)
                }
                return mplew.getPacket();
            }
            mplew.writeMapleAsciiString(str); // file name for screenshot

            return mplew.getPacket();
        }

        public static byte[] antiMacroBomb(boolean error, int mapid, int channel) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AntiMacroBombResult.getValue());
            mplew.write(error ? 2 : 1);
            mplew.writeInt(mapid);
            mplew.writeInt(channel);

            return mplew.getPacket();
        }
    }

    public static byte[] report(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ClaimResult.getValue());
        mplew.write(mode);
        if (mode == 2) {
            mplew.write(0);
            mplew.writeInt(1); // times left to report
        }

        return mplew.getPacket();
    }

    public static byte[] OnStarPlanetUserCount(Map<Integer, Pair<Integer, Integer>> data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);
        mplew.writeInt(data.size());
        for (Map.Entry<Integer, Pair<Integer, Integer>> entry : data.entrySet()) {
            mplew.writeInt(entry.getValue().left);
            mplew.writeInt(entry.getValue().right);
        }
        return mplew.getPacket();
    }

    public static byte[] OnClaimSvrStatusChanged(boolean enable) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.LP_ClaimSvrStatusChanged.getValue());
        mplew.write(enable ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] onSetClaimSvrAvailableTime(int from, int to) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);

        mplew.writeShort(SendPacketOpcode.LP_SetClaimSvrAvailableTime.getValue());
        mplew.write(from);
        mplew.write(to);

        return mplew.getPacket();
    }

    public static byte[] onSetTamingMobInfo(MapleCharacter chr, boolean levelup) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetTamingMobInfo.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getMount().getLevel());
        mplew.writeInt(chr.getMount().getExp());
        mplew.writeInt(chr.getMount().getFatigue());
        mplew.write(levelup ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] showQuestCompletion(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_QuestClear.getValue());
        mplew.writeInt(id);

        return mplew.getPacket();
    }

    public static byte[] useSkillBook(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SkillLearnItemResult.getValue());
        mplew.write(0);
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(maxlevel);
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] useAPSPReset(boolean spReset, int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(spReset ? SendPacketOpcode.LP_SkillResetItemResult.getValue()
                : SendPacketOpcode.LP_AbilityResetItemResult.getValue());
        mplew.write(1);
        mplew.writeInt(cid);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] expandCharacterSlots(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CharSlotIncItemResult.getValue());
        mplew.writeInt(mode);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] finishedGather(int type) {
        return gatherSortItem(true, type);
    }

    public static byte[] finishedSort(int type) {
        return gatherSortItem(false, type);
    }

    public static byte[] gatherSortItem(boolean gather, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(gather ? SendPacketOpcode.LP_SortItemResult.getValue()
                : SendPacketOpcode.LP_GatherItemResult.getValue());
        mplew.write(1);
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] updateExpPotion(int mode, int id, int itemId, boolean firstTime, int level, int potionDstLevel,
            int exp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ExpConsumeResetItemResult.getValue());
        mplew.write(mode);
        mplew.write(1); // bool for get_update_time
        mplew.writeInt(id);
        if (id != 0) {
            mplew.write(1); // not even being read how rude of nexon
            if (mode == 1) {
                mplew.writeInt(0);
            }
            if (mode == 2) {
                mplew.write(firstTime ? 1 : 0); // 1 on first time then it turns 0
                mplew.writeInt(itemId);
                if (itemId != 0) {
                    mplew.writeInt(level); // level, confirmed
                    mplew.writeInt(potionDstLevel); // max level with potion
                    mplew.writeLong(exp); // random, more like potion id
                }
            }
        }

        return mplew.getPacket();
    }

    public static byte[] updateGender(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_GENDER.getValue());
        mplew.write(chr.getGender());

        return mplew.getPacket();
    }

    public static byte[] charInfo(MapleCharacter chr, boolean isSelf) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CharacterInfo.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob());
        mplew.writeShort(chr.getSubcategory());
        mplew.write(chr.getStat().pvpRank);
        mplew.writeInt(chr.getFame());
        MapleMarriage marriage = chr.getMarriage();
        mplew.write(marriage != null && marriage.getId() != 0);
        if (marriage != null && marriage.getId() != 0) {
            mplew.writeInt(marriage.getId()); // marriage id
            mplew.writeInt(marriage.getHusbandId()); // husband char id
            mplew.writeInt(marriage.getWifeId()); // wife char id
            mplew.writeShort(3); // msg type
            mplew.writeInt(chr.getMarriageItemId()); // ring id husband
            mplew.writeInt(chr.getMarriageItemId()); // ring id wife
            mplew.writeAsciiString(marriage.getHusbandName(), 15); // husband name
            mplew.writeAsciiString(marriage.getWifeName(), 15); // wife name
        }
        List<Integer> prof = chr.getProfessions();
        mplew.write(prof.size());
        prof.forEach(mplew::writeShort);
        if (chr.getGuildId() <= 0) {
            mplew.writeMapleAsciiString("-");
            mplew.writeMapleAsciiString("");
        } else {
            MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                if (gs.getAllianceId() > 0) {
                    MapleGuildAlliance allianceName = World.Alliance.getAlliance(gs.getAllianceId());
                    if (allianceName != null) {
                        mplew.writeMapleAsciiString(allianceName.getName());
                    } else {
                        mplew.writeMapleAsciiString("");
                    }
                } else {
                    mplew.writeMapleAsciiString("");
                }
            } else {
                mplew.writeMapleAsciiString("-");
                mplew.writeMapleAsciiString("");
            }
        }
        mplew.write(isSelf ? -1 : 0);
        mplew.write(0);
        mplew.writeMapleAsciiString("");
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);

        for (MaplePet pet : chr.getSummonedPets()) {
            mplew.write(true);
            mplew.writeInt(chr.getPetIndex(pet));
            mplew.writeInt(pet.getPetItemId());
            mplew.writeMapleAsciiString(pet.getName());
            mplew.write(pet.getLevel());
            mplew.writeShort(pet.getCloseness());
            mplew.write(pet.getFullness());
            mplew.writeShort(pet.getFlags());
            Item inv = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (pet.getSummonedValue() == 2 ? -130 : pet.getSummonedValue() == 1 ? -114 : -138));
            mplew.writeInt(inv == null ? 0 : inv.getItemId());
            mplew.writeInt(-1);
        }
        mplew.write(false);

        int wishlistSize = chr.getWishlistSize();
        mplew.write(wishlistSize);
        if (wishlistSize > 0) {
            int[] wishlist = chr.getWishlist();
            for (int x = 0; x < wishlistSize; x++) {
                mplew.writeInt(wishlist[x]);
            }
        }

        Item medal = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -46);
        mplew.writeInt(medal == null ? 0 : medal.getItemId());
        List<Pair<Integer, Long>> medalQuests = chr.getCompletedMedals();
        mplew.writeShort(medalQuests.size());
        for (Pair<Integer, Long> x : medalQuests) {
            mplew.writeInt(x.left);
            mplew.writeLong(x.right);
        }

        boolean damageSkinInfo = true;
        mplew.write(damageSkinInfo); // damageSkin
        if (damageSkinInfo) {
            // ActiveDamageSkin
            int damageSkin = chr.getDamageSkin();
            List<Integer> savedDamageSkins = chr.getSaveDamageSkin();
            mplew.writeInt(damageSkin);
            mplew.writeInt(ItemConstants.傷害字型.getDamageSkinItemIDBySkinID(damageSkin));
            mplew.write(savedDamageSkins.contains(damageSkin)); // bNotSaved
            mplew.writeMapleAsciiString("QQ");
            // PremiumDamageSkin
            mplew.writeInt(-1);
            mplew.writeInt(0);
            mplew.write(1);
            mplew.writeMapleAsciiString("");

            mplew.writeShort(255); // TODO : 帳號新增 damageSkinSlot
            mplew.writeShort(savedDamageSkins.size());
            for (int i = 0; i < savedDamageSkins.size(); i++) {
                mplew.writeInt(savedDamageSkins.get(i));
                mplew.writeInt(ItemConstants.傷害字型.getDamageSkinItemIDBySkinID(savedDamageSkins.get(i)));
                mplew.write(0);
                mplew.writeMapleAsciiString("");
            }
        }

        for (MapleTrait.MapleTraitType t : MapleTrait.MapleTraitType.values()) {
            mplew.write(chr.getTrait(t).getLevel());
        }

        List<Integer> chairs = new ArrayList<>();
        for (Item i : chr.getInventory(MapleInventoryType.SETUP).newList()) {
            if ((i.getItemId() / 10000 == 301) && (!chairs.contains(i.getItemId()))) {
                chairs.add(i.getItemId());
            }
        }
        mplew.writeInt(chairs.size());
        chairs.forEach(mplew::writeInt);

        List<Integer> medals = new ArrayList<>();
        for (Item i : chr.getInventory(MapleInventoryType.EQUIP).newList()) {
            if (i.getItemId() >= 1142000 && i.getItemId() < 1152000) {
                medals.add(i.getItemId());
            }
        }
        mplew.writeInt(medals.size());
        for (int i : medals) {
            mplew.writeInt(i);
        }

        return mplew.getPacket();
    }

    public static byte[] getMonsterBookInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOOK_INFO.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getLevel());
        chr.getMonsterBook().writeCharInfoPacket(mplew);

        return mplew.getPacket();
    }

    public static byte[] spawnPortal(int townId, int targetId, int skillId, Point pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_TownPortal.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        if ((townId != 999999999) && (targetId != 999999999)) {
            mplew.writeInt(skillId);
            mplew.writePos(pos);
        }

        return mplew.getPacket();
    }

    public static byte[] echoMegaphone(String name, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ECHO_MESSAGE.getValue());
        mplew.write(0);
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(message);

        return mplew.getPacket();
    }

    public static byte[] showQuestMsg(String msg) {
        return broadcastMsg(5, msg);
    }

    public static byte[] Mulung_Pts(int recv, int total) {
        return showQuestMsg(new StringBuilder().append("You have received ").append(recv)
                .append(" training points, for the accumulated total of ").append(total).append(" training points.")
                .toString());
    }

    public static byte[] broadcastMsg(String message) {
        return broadcastMessage(4, 0, 0, new String[] { message }, true, null);
    }

    public static byte[] broadcastMsg(int type, String message) {
        return broadcastMessage(type, 0, 0, new String[] { message }, false, null);
    }

    public static byte[] broadcastMsg(int type, int channel, String message) {
        return broadcastMessage(type, channel, 0, new String[] { message }, false, null);
    }

    public static byte[] broadcastMsg(int type, int channel, String message, boolean smegaEar) {
        return broadcastMessage(type, channel, 0, new String[] { message }, smegaEar, null);
    }

    public static byte[] itemMegaphone(String msg, boolean whisper, int channel, Item item) {
        return broadcastMessage(8, channel, 0, new String[] { msg }, whisper, item);
    }

    public static byte[] tripleSmega(List<String> message, boolean ear, int channel) {
        String[] msgs = new String[3];
        for (int i = 0; i < msgs.length; i++) {
            msgs[i] = message.size() >= i + 1 ? message.get(i) : "";
        }
        return broadcastMessage(10, channel, 0, msgs, ear, null);
    }

    public static byte[] cubeMega(String msg, Item item) {
        return broadcastMessage(11, 0, 0, new String[] { msg }, true, item);
    }

    public static byte[] getEventEnvelope(int questID, int time) {
        return broadcastMessage(24, questID, time, null, false, null);
    }

    public static byte[] getGachaponMega(String name, String gacha, String message, Item item, int channel,
            int rareness) {
        return broadcastMessage(33, channel, rareness,
                new String[] { name + " : 恭喜" + name + "從" + gacha + "獲得{"
                        + (item != null ? MapleItemInformationProvider.getInstance().getName(item.getItemId()) : "")
                        + "}" + (message == null ? "" : message) + "。" },
                true, item);
    }

    private static byte[] broadcastMessage(int type, int value, int value2, String[] message, boolean bool,
            final Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_BroadcastMsg.getValue());
        mplew.write(type);
        if (type == 4) {
            mplew.write(bool); // 頂部滾動消息開關
        }
        if (type != 13 && type != 14 && type != 32 && (type != 4 || (type == 4 && bool))) {
            mplew.writeMapleAsciiString(message == null || message.length < 1 ? "" : message[0]);
        }
        switch (type) {
        case 3:
        case 23:
            mplew.write(value - 1); // 頻道(從0開始)
            mplew.write(bool ? 1 : 0);
            break;
        case 28: // 愛心喇叭
        case 29: // 骷髏喇叭
        case 26: // 蛋糕喇叭
        case 27: // 派餅喇叭
            mplew.write(value - 1); // 頻道(從0開始)
            mplew.write(bool ? 1 : 0); // 能否密語
            break;
        case 8:
            mplew.write(value - 1); // 頻道(從0開始)
            mplew.write(bool ? 1 : 0); // 能否密語
            mplew.write(item != null);
            if (item != null) {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
            }
            break;
        case 9:
            mplew.write(value - 1); // 頻道(從0開始)
            break;
        case 10:
            int lines = message == null ? 0 : message.length - 1;
            mplew.write(lines);
            if (lines > 1) {
                mplew.writeMapleAsciiString(message == null || message.length < 2 ? "" : message[1]);
            }
            if (lines > 2) {
                mplew.writeMapleAsciiString(message == null || message.length < 3 ? "" : message[2]);
            }
            mplew.write(value - 1); // 頻道(從0開始)
            mplew.write(bool ? 1 : 0); // 能否密語
            break;
        case 12:
            mplew.writeInt(value - 1); // 頻道(從0開始)
            break;
        case 24:
            mplew.writeInt(value); // 任務ID
            mplew.writeInt(value2); // 時間(秒)
            mplew.write(bool);
            if (bool) {
                mplew.writeZeroBytes(24);
            }
            break;
        case 17:
            mplew.write(item != null);
            if (item != null) {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
            }
            break;
        case 21:
            PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
            break;
        default:
            if (type != 22) {
                if (type == 30 || type == 31) {
                    mplew.writeInt(value - 1); // 頻道(從0開始)
                    mplew.writeMapleAsciiString(message == null || message.length < 2 ? "" : message[1]);
                    PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
                    // read??
                }
                // PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
            }
            break;
        }

        switch (type) {
        case 0:
            break;
        case 1:
            break;
        case 2:
            break;
        case 17:
            break;
        case 3:
        case 8:
        case 9:
        case 10:
        case 23:
        case 25:
        case 26:
        case 27:
        case 28:
            break;
        case 21:
        case 22:
            break;
        case 30:
        case 31:
            break;
        case 33:
            mplew.writeInt(item != null ? item.getItemId() : 0);
            mplew.writeInt(Math.max(value - 1, -1)); // 頻道(從0開始)
            mplew.writeInt(value2); // 類型
            mplew.write(item != null);
            if (item != null) {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
            }
            break;
        case 4:
            break;
        case 5:
            break;
        case 11:
            mplew.writeInt(item == null ? 0 : item.getItemId());
            if (bool) {
                mplew.write(item != null);
                if (item != null) {
                    PacketHelper.GW_ItemSlotBase_Decode(mplew, item);
                }
            }
            break;
        case 6:
        case 18:
            mplew.writeInt((value >= 1000000) && (value < 6000000) ? value : 0);
            break;
        case 7:
            mplew.writeInt(0);
            break;
        case 12:
        case 24:
            break;
        case 13:
            mplew.writeInt(0);
            mplew.writeInt(0);
            break;
        case 14:
            mplew.writeInt(0);
            break;
        case 15:
            break;
        case 16:
            mplew.writeInt(0);
            break;
        case 20:
            mplew.writeInt(0);
            mplew.writeInt(0);
            break;
        case 32:
            mplew.writeMapleAsciiString(message == null || message.length < 2 ? "" : message[1]);
            mplew.writeInt(0);
            break;
        }
        return mplew.getPacket();
    }

    public static byte[] getPeanutResult(int itemId, short quantity, int itemId2, short quantity2, int ourItem) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_IncubatorResult.getValue());
        mplew.writeInt(itemId);
        mplew.writeShort(quantity);
        mplew.writeInt(ourItem);
        mplew.writeInt(itemId2);
        mplew.writeInt(quantity2);
        mplew.write(0);
        mplew.write(0); // Boolean

        return mplew.getPacket();
    }

    public static byte[] getOwlOpen() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ShopScannerResult.getValue());
        mplew.write(0xA);
        mplew.write(GameConstants.owlItems.length);
        for (int i : GameConstants.owlItems) {
            mplew.writeInt(i);
        }

        return mplew.getPacket();
    }

    public static byte[] getOwlSearched(int itemSearch, List<HiredMerchant> hms) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ShopScannerResult.getValue());
        mplew.write(0x9);
        mplew.writeInt(0);
        mplew.writeInt(itemSearch);
        int size = 0;

        for (HiredMerchant hm : hms) {
            size += hm.searchItem(itemSearch).size();
        }

        mplew.writeInt(size);
        for (HiredMerchant hm : hms) {
            for (Iterator<HiredMerchant> i = hms.iterator(); i.hasNext();) {
                hm = i.next();
                final List<MaplePlayerShopItem> items = hm.searchItem(itemSearch);
                for (MaplePlayerShopItem item : items) {
                    mplew.writeMapleAsciiString(hm.getOwnerName());
                    mplew.writeInt(hm.getMap().getId());
                    mplew.writeMapleAsciiString(hm.getDescription());
                    mplew.writeInt(item.item.getQuantity());
                    mplew.writeInt(item.bundles);
                    mplew.writeInt(item.price);
                    switch (2) {
                    case 0:
                        mplew.writeInt(hm.getOwnerId());
                        break;
                    case 1:
                        mplew.writeInt(hm.getStoreId());
                        break;
                    default:
                        mplew.writeInt(hm.getObjectId());
                    }

                    mplew.write(hm.getFreeSlot() == -1 ? 1 : 0);
                    mplew.write(GameConstants.getInventoryType(itemSearch).getType());
                    if (GameConstants.getInventoryType(itemSearch) == MapleInventoryType.EQUIP) {
                        PacketHelper.GW_ItemSlotBase_Decode(mplew, item.item);
                    }
                }
            }
        }
        return mplew.getPacket();
    }

    public static byte[] getOwlMessage(int msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.LP_ShopLinkResult.getValue());
        mplew.write(msg);

        return mplew.getPacket();
    }

    public static byte[] sendWeddingGive() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WeddingGiftResult.getValue());
        mplew.write(9);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] sendWeddingReceive() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WeddingGiftResult.getValue());
        mplew.write(10);
        mplew.writeLong(-1L);
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] giveWeddingItem() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WeddingGiftResult.getValue());
        mplew.write(11);
        mplew.write(0);
        mplew.writeLong(0L);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] receiveWeddingItem() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WeddingGiftResult.getValue());
        mplew.write(15);
        mplew.writeLong(0L);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] sendCashPetFood(boolean success, byte index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3 + (success ? 1 : 0));

        mplew.writeShort(SendPacketOpcode.LP_CashPetFoodResult.getValue());
        mplew.write(success ? 0 : 1);
        if (success) {
            mplew.write(index);
        }

        return mplew.getPacket();
    }

    public static byte[] getFusionAnvil(boolean success, int index, int itemID) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3 + (success ? 1 : 0));

        mplew.writeShort(SendPacketOpcode.LP_CashLookChangeResult.getValue());
        mplew.writeBoolean(success);
        mplew.writeInt(index);
        mplew.writeInt(itemID);

        return mplew.getPacket();
    }

    public static byte[] yellowChat(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetWeekEventMessage.getValue());
        mplew.write(true); // 是否顯示
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] shopDiscount(int percent) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetPotionDiscountRate.getValue());
        mplew.write(percent);

        return mplew.getPacket();
    }

    public static byte[] catchMob(int mobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_BridleMobCatchFail.getValue());
        mplew.write(success);
        mplew.writeInt(itemid);
        mplew.writeInt(mobid);

        return mplew.getPacket();
    }

    public static byte[] spawnPlayerNPC(PlayerNPC npc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ImitatedNPCData.getValue());
        mplew.write(1);
        mplew.writeInt(npc.getId());
        mplew.writeMapleAsciiString(npc.getName());
        PacketHelper.AvatarLook__Decode(mplew, npc, true, false);

        return mplew.getPacket();
    }

    public static byte[] disabledNPC(List<Integer> ids) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3 + ids.size() * 4);

        mplew.writeShort(SendPacketOpcode.LP_LimitedNPCDisableInfo.getValue());
        mplew.write(ids.size());
        for (Integer i : ids) {
            mplew.writeInt(i);
        }

        return mplew.getPacket();
    }

    public static byte[] getCard(int itemid, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MonsterBookSetCard.getValue());
        mplew.write(itemid > 0 ? 1 : 0);
        if (itemid > 0) {
            mplew.writeInt(itemid);
            mplew.writeInt(level);
        }
        return mplew.getPacket();
    }

    public static byte[] changeCardSet(int set) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MonsterBookSetCover.getValue());
        mplew.writeInt(set);

        return mplew.getPacket();
    }

    public static byte[] upgradeBook(Item book, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOOK_STATS.getValue());
        mplew.writeInt(book.getPosition());
        PacketHelper.GW_ItemSlotBase_Decode(mplew, book, chr);

        return mplew.getPacket();
    }

    public static byte[] getCardDrops(int cardid, List<Integer> drops) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CARD_DROPS.getValue());
        mplew.writeInt(cardid);
        mplew.writeShort(drops == null ? 0 : drops.size());
        if (drops != null) {
            for (Integer de : drops) {
                mplew.writeInt(de);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] getFamiliarInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FAMILIAR_INFO.getValue());
        mplew.writeInt(chr.getFamiliars().size());
        for (MonsterFamiliar mf : chr.getFamiliars().values()) {
            mf.writeRegisterPacket(mplew, true);
        }
        List<Pair<Integer, Long>> size = new ArrayList<>();
        for (Item i : chr.getInventory(MapleInventoryType.USE).list()) {
            if (i.getItemId() / 10000 == 287) {
                StructFamiliar f = MapleItemInformationProvider.getInstance().getFamiliarByItem(i.getItemId());
                if (f != null) {
                    size.add(new Pair<>(f.getGrade(), i.getInventoryId()));
                }
            }
        }
        mplew.writeInt(size.size());
        for (Pair<?, ?> s : size) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(((Integer) s.left));
            mplew.writeLong(((Long) s.right));
            mplew.write(0);
        }
        size.clear();

        return mplew.getPacket();
    }

    public static byte[] updateWebBoard(boolean result) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WebBoardAuthkeyUpdate.getValue());
        mplew.writeBoolean(result);

        return mplew.getPacket();
    }

    public static byte[] MulungEnergy(int energy) {
        return sendPyramidEnergy("energy", String.valueOf(energy));
    }

    public static byte[] sendPyramidEnergy(String type, String amount) {
        return sendString(1, type, amount);
    }

    public static byte[] sendGhostPoint(String type, String amount) {
        return sendString(2, type, amount);
    }

    public static byte[] sendGhostStatus(String type, String amount) {
        return sendString(3, type, amount);
    }

    public static byte[] sendString(int type, String object, String amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        switch (type) {
        case 1:
            mplew.writeShort(SendPacketOpcode.LP_SessionValue.getValue());
            break;
        case 2:
            mplew.writeShort(SendPacketOpcode.LP_PartyValue.getValue());
            break;
        case 3:
            mplew.writeShort(SendPacketOpcode.LP_FieldValue.getValue());
        }

        mplew.writeMapleAsciiString(object);
        mplew.writeMapleAsciiString(amount);

        return mplew.getPacket();
    }

    public static byte[] fairyPendantMessage(int termStart, int incExpR) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(14);

        mplew.writeShort(SendPacketOpcode.LP_BonusExpRateChanged.getValue());
        mplew.writeInt(17);
        mplew.writeInt(0);

        mplew.writeLong(incExpR);

        return mplew.getPacket();
    }

    public static byte[] potionDiscountMessage(int type, int potionDiscR) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(10);

        mplew.writeShort(SendPacketOpcode.POTION_BONUS.getValue());
        mplew.writeInt(type);
        mplew.writeInt(potionDiscR);

        return mplew.getPacket();
    }

    public static byte[] sendLevelup(boolean family, int level, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_NotifyLevelUp.getValue());
        mplew.write(family ? 1 : 2);
        mplew.writeInt(level);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] sendMarriage(boolean family, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_NotifyWedding.getValue());
        mplew.write(family ? 1 : 0);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] sendJobup(boolean family, int jobid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_NotifyJobChange.getValue());
        mplew.write(family ? 1 : 0);
        mplew.writeInt(jobid);
        mplew.writeMapleAsciiString(new StringBuilder().append(!family ? "> " : "").append(name).toString());

        return mplew.getPacket();
    }

    public static byte[] getAvatarMega(MapleCharacter chr, int channel, int itemId, List<String> text, boolean ear) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AvatarMegaphoneUpdateMessage.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(chr.getName());
        for (String i : text) {
            mplew.writeMapleAsciiString(i);
        }
        mplew.writeInt(channel - 1);
        mplew.write(ear ? 1 : 0);
        PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
        mplew.write(false);

        return mplew.getPacket();
    }

    public static byte[] GMPoliceMessage(boolean dc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.GM_POLICE.getValue());
        mplew.write(dc ? 10 : 0);

        return mplew.getPacket();
    }

    public static byte[] GMPoliceMessage(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DataCRCCheckFailed.getValue());
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] pendantSlot(boolean p) { // slot -59
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetBuyEquipExt.getValue());
        mplew.write(p ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] followRequest(int chrid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetPassenserRequest.getValue());
        mplew.writeInt(chrid);

        return mplew.getPacket();
    }

    public static byte[] getTopMsg(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ScriptProgressMessage.getValue());
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] getTopMsg2(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ScriptProgressMessageBySoul.getValue());
        mplew.writeMapleAsciiString(msg);
        mplew.writeInt(2);
        return mplew.getPacket();
    }

    public static byte[] getProgressMessageFont(String sMsg, int nFontNameType, int nFontSize, int nFontColorType,
            int nFadeOutDelay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ProgressMessageFont.getValue());
        mplew.writeInt(nFontNameType);
        mplew.writeInt(nFontSize);
        mplew.writeInt(nFontColorType);
        mplew.writeInt(nFadeOutDelay);
        mplew.writeMapleAsciiString(sMsg);

        return mplew.getPacket();
    }

    public static byte[] getNewTopMsg() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ScriptProgressItemMessage.getValue());
        mplew.writeInt(0);
        mplew.writeMapleAsciiString("Welcome to Maple World. We're one big family!");

        return mplew.getPacket();
    }

    public static byte[] showMidMsg(String s, int l) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetStaticScreenMessage.getValue());
        mplew.write(l);
        mplew.writeMapleAsciiString(s);
        mplew.write(s.length() > 0 ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] getMidMsg(String msg, boolean keep, int index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetStaticScreenMessage.getValue());
        mplew.write(index);
        mplew.writeMapleAsciiString(msg);
        mplew.write(keep ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] clearMidMsg() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_OffStaticScreenMessage.getValue());

        return mplew.getPacket();
    }

    public static byte[] getSpecialMsg(String msg, int type, boolean show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WeatherEffectNotice.getValue());
        mplew.writeMapleAsciiString(msg);
        mplew.writeInt(type);
        mplew.writeInt(show ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] getWeatherEffectNoticeY(String msg, int type, boolean show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WeatherEffectNoticeY.getValue());
        mplew.writeMapleAsciiString(msg);
        mplew.writeInt(type);
        mplew.writeInt(show ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] CakePieMsg() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_IncCharmByCashPRMsg.getValue());

        return mplew.getPacket();
    }

    public static byte[] gmBoard(int increnement, String url) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GM_STORY_BOARD.getValue());
        mplew.writeInt(increnement); // Increnement number
        mplew.writeMapleAsciiString(url);

        return mplew.getPacket();
    }

    public static byte[] loadInformation(byte mode, int location, int birthday, int favoriteAction,
            int favoriteLocation, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.YOUR_INFORMATION.getValue());
        mplew.write(mode);
        if (mode == 2) {
            mplew.writeInt(location);
            mplew.writeInt(birthday);
            mplew.writeInt(favoriteAction);
            mplew.writeInt(favoriteLocation);
        } else if (mode == 4) {
            mplew.write(success ? 1 : 0);
        }

        return mplew.getPacket();
    }

    public static byte[] saveInformation(boolean fail) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.YOUR_INFORMATION.getValue());
        mplew.write(4);
        mplew.write(fail ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] myInfoResult() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FIND_FRIEND.getValue());
        mplew.write(6);
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] findFriendResult(byte mode, List<MapleCharacter> friends, int error, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FIND_FRIEND.getValue());
        mplew.write(mode);
        switch (mode) {
        case 6:
            mplew.writeInt(0);
            mplew.writeInt(0);
            break;
        case 8:
            mplew.writeShort(friends.size());
            for (MapleCharacter mc : friends) {
                mplew.writeInt(mc.getId());
                mplew.writeMapleAsciiString(mc.getName());
                // mplew.write(mc.getLevel());
                // mplew.writeShort(mc.getJob());
                // mplew.writeInt(0);
                // mplew.writeInt(0);
            }
            break;
        case 9:
            mplew.write(error);
            break;
        case 11:
            mplew.writeInt(chr.getId());
            PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
            break;
        }

        return mplew.getPacket();
    }

    public static byte[] showBackgroundEffect(String eff, int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.VISITOR.getValue());
        mplew.writeMapleAsciiString(eff);
        mplew.write(value);

        return mplew.getPacket();
    }

    public static byte[] sendPinkBeanChoco() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PINKBEAN_CHOCO.getValue());
        mplew.writeInt(0);
        mplew.write(1);
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] changeChannelMsg(int channel, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(9 + msg.length());

        mplew.writeShort(SendPacketOpcode.AUTO_CC_MSG.getValue());
        mplew.writeInt(channel);
        mplew.writeMapleAsciiString(msg);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] pamSongUI() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PAM_SONG.getValue());
        return mplew.getPacket();
    }

    public static byte[] ultimateExplorer() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_OpenUICreatePremiumAdventurer.getValue());

        return mplew.getPacket();
    }

    public static byte[] professionInfo(String skil, int level1, int level2, int chance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ResultInstanceTable.getValue());
        mplew.writeMapleAsciiString(skil);
        mplew.writeInt(level1);
        mplew.writeInt(level2);
        mplew.write(1);
        mplew.writeInt((skil.startsWith("9200")) || (skil.startsWith("9201")) ? 100 : chance);

        return mplew.getPacket();
    }

    public static byte[] updateHonorExp(int honor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CharacterHonorExp.getValue());
        mplew.writeInt(honor);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] showAzwanKilled() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ReadyForRespawn.getValue());

        return mplew.getPacket();
    }

    public static byte[] showSilentCrusadeMsg(byte type, short chapter) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 類型(type)： [0] 十字獵人介面 [2] 道具欄空間不足。 [3] 未知的原因失敗。
         */
        mplew.writeShort(SendPacketOpcode.LP_CrossHunterCompleteResult.getValue());
        mplew.write(type);
        mplew.writeShort(chapter - 1);

        return mplew.getPacket();
    }

    public static byte[] getSilentCrusadeMsg(byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 類型(type)： [0] 申請的道具購買已完全。 [1] 道具不足。 [2] 道具欄空間不足。 [3] 無法再擁有的道具。 [4] 現在無法購買道具。
         */
        mplew.writeShort(SendPacketOpcode.LP_CrossHunterShopResult.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] showSCShopMsg(byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CrossHunterShopResult.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] updateImpTime() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ItemCoolTimeChange.getValue());
        mplew.writeInt(0);
        mplew.writeLong(0L);

        return mplew.getPacket();
    }

    public static byte[] updateImp(MapleImp imp, int mask, int index, boolean login) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ItemPotChange.getValue());
        mplew.write(login ? 0 : 1);
        mplew.writeInt(index + 1);
        mplew.writeInt(mask);
        if ((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0) {
            Pair<?, ?> i = MapleItemInformationProvider.getInstance().getPot(imp.getItemId());
            if (i == null) {
                return enableActions();
            }
            mplew.writeInt(((Integer) i.left));
            mplew.write(imp.getLevel());
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0) || ((mask & MapleImp.ImpFlag.STATE.getValue()) != 0)) {
            mplew.write(imp.getState());
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.FULLNESS.getValue()) != 0)) {
            mplew.writeInt(imp.getFullness());
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.CLOSENESS.getValue()) != 0)) {
            mplew.writeInt(imp.getCloseness());
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.CLOSENESS_LEFT.getValue()) != 0)) {
            mplew.writeInt(1);
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.MINUTES_LEFT.getValue()) != 0)) {
            mplew.writeInt(0);
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0) || ((mask & MapleImp.ImpFlag.LEVEL.getValue()) != 0)) {
            mplew.write(1);
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.FULLNESS_2.getValue()) != 0)) {
            mplew.writeInt(imp.getFullness());
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.UPDATE_TIME.getValue()) != 0)) {
            mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.CREATE_TIME.getValue()) != 0)) {
            mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.AWAKE_TIME.getValue()) != 0)) {
            mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.SLEEP_TIME.getValue()) != 0)) {
            mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.MAX_CLOSENESS.getValue()) != 0)) {
            mplew.writeInt(100);
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.MAX_DELAY.getValue()) != 0)) {
            mplew.writeInt(1000);
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.MAX_FULLNESS.getValue()) != 0)) {
            mplew.writeInt(1000);
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.MAX_ALIVE.getValue()) != 0)) {
            mplew.writeInt(1);
        }
        if (((mask & MapleImp.ImpFlag.SUMMONED.getValue()) != 0)
                || ((mask & MapleImp.ImpFlag.MAX_MINUTES.getValue()) != 0)) {
            mplew.writeInt(10);
        }
        mplew.write(0);

        return mplew.getPacket();
    }

    // public static byte[] getMulungRanking(MapleClient c, List<DojoRankingInfo>
    // all) {
    // final MaplePacketLittleEndianWriter mplew = new
    // MaplePacketLittleEndianWriter();
    // mplew.writeShort(SendPacketOpcode.MULUNG_DOJO_RANKING.getValue());
    // MapleDojoRanking data = MapleDojoRanking.getInstance();
    // mplew.writeInt(all.size()); // size
    // for (DojoRankingInfo info : all) {
    // mplew.writeShort(info.getRank());
    // mplew.writeMapleAsciiString(info.getName());
    // mplew.writeLong(info.getTime());
    // }
    // return mplew.getPacket();
    // }
    public static byte[] getMulungRanking() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_DojangRanking.getValue());

        // 三種排名
        encodeDojangRanking(mplew, 0);
        encodeDojangRanking(mplew, 1);
        encodeDojangRanking(mplew, 2);
        mplew.write(0);
        return mplew.getPacket();
    }

    private static void encodeDojangRanking(MaplePacketLittleEndianWriter mplew, int nType) {

        DojangRankList rank = DojangRankingFactory.getInstance().getRankList(nType);

        mplew.writeInt(0); // ??
        mplew.write(nType);//
        mplew.writeInt(0); // nJob
        mplew.writeInt(0); // nLevel
        mplew.writeInt(-1); // nPoint
        mplew.writeInt(-1); // nRanking
        mplew.writeInt(-1); // nPercent
        mplew.writeInt(-1); // nLastPoint
        mplew.writeInt(-1); // nLastRanking
        mplew.writeInt(-1); // nLastPercent
        mplew.write(nType);//
        List<DojangRankDataEntry> rankData = rank.getData();
        mplew.writeInt(rankData.size());
        for (int i = 0; i < rankData.size(); i++) {
            DojangRankDataEntry record = rankData.get(i);
            mplew.writeInt(record.job);
            mplew.writeInt(record.level);
            mplew.writeInt(record.point);
            mplew.writeInt(record.ranking);
            mplew.writeMapleAsciiString(record.name);
            mplew.writeBoolean(record.hasAvartar);
            if (record.hasAvartar) {
                mplew.writeZeroBytes(120);
                // TMSv202 -> sub_0047D510
            }
        }
    }

    public static byte[] getMulungMessage(boolean dc, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetAdDisplayInfo.getValue());
        mplew.write(dc ? 1 : 0);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] getCandyRanking(MapleClient c, List<CandyRankingInfo> all) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(10);

        mplew.writeShort(SendPacketOpcode.CANDY_RANKING.getValue());
        mplew.writeInt(all.size());
        for (CandyRankingInfo info : all) {
            mplew.writeShort(info.getRank());
            mplew.writeMapleAsciiString(info.getName());
        }
        return mplew.getPacket();
    }

    public static byte[] showChronosphere(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAY_OF_CHRONOSPHERE.getValue());

        mplew.writeInt(chr.getFreeChronosphere());
        mplew.writeInt(chr.getChronosphere());

        return mplew.getPacket();
    }

    public static byte[] errorChronosphere() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ERROR_CHRONOSPHERE.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] SystemProcess() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CheckProcess.getValue());
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] getEventList(boolean isEntry) {
        if (ServerConfig.LOG_PACKETS) {
            System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_RequestEventList.getValue());
        mplew.writeInt(0);
        mplew.write(isEntry ? 1 : 0); // Boolean
        if (isEntry) {
            mplew.writeMapleAsciiString(EventListConstants.EVENT_NAME);
            mplew.write(0); // Boolean
            mplew.writeInt(0);
            mplew.writeInt(EventListConstants.values().length); // 活動數量
            int i = 0;
            for (EventListConstants event : EventListConstants.values()) {
                mplew.writeInt(0x15B + i); // 未知
                mplew.writeMapleAsciiString(event.getEventName());
                mplew.writeMapleAsciiString("");
                mplew.writeInt(9); // 未知
                mplew.writeInt(9); // 未知
                mplew.writeInt(event.getStartTime());
                mplew.writeInt(event.getEndTime());
                mplew.writeInt(0); // 未知
                mplew.writeInt(1); // 未知
                mplew.write(0); // 未知
                mplew.write(0); // 未知
                mplew.write(0); // 未知
                mplew.write(0); // 未知
                mplew.write(1); // 未知
                mplew.writeInt(event.getItemQuantity());
                if (event.getItemQuantity() > 0) {
                    for (int itemId : event.getItemId()) {
                        mplew.writeInt(itemId);
                    }
                }
                mplew.writeInt(0); // 未知
                mplew.write(0); // 未知
                mplew.write(0); // 未知
                i++;
            }
        }
        mplew.writeInt(0); // 未知

        return mplew.getPacket();
    }

    public static class AlliancePacket {

        public static byte[] getAllianceInfo(MapleGuildAlliance alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(12);
            mplew.write(alliance == null ? 0 : 1);
            if (alliance != null) {
                addAllianceInfo(mplew, alliance);
            }

            return mplew.getPacket();
        }

        private static void addAllianceInfo(MaplePacketLittleEndianWriter mplew, MapleGuildAlliance alliance) {
            mplew.writeInt(alliance.getId());
            mplew.writeMapleAsciiString(alliance.getName());
            for (int i = 1; i <= 5; i++) {
                mplew.writeMapleAsciiString(alliance.getRank(i));
            }
            mplew.write(alliance.getNoGuilds());
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                mplew.writeInt(alliance.getGuildId(i));
            }
            mplew.writeInt(alliance.getCapacity());
            mplew.writeMapleAsciiString(alliance.getNotice());
        }

        public static byte[] getGuildAlliance(MapleGuildAlliance alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(13);
            if (alliance == null) {
                mplew.writeInt(0);
                return mplew.getPacket();
            }
            int noGuilds = alliance.getNoGuilds();
            MapleGuild[] g = new MapleGuild[noGuilds];
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                g[i] = World.Guild.getGuild(alliance.getGuildId(i));
                if (g[i] == null) {
                    return CWvsContext.enableActions();
                }
            }
            mplew.writeInt(noGuilds);
            for (MapleGuild gg : g) {
                CWvsContext.GuildPacket.GUILDDATA_Decode(mplew, gg);
            }
            return mplew.getPacket();
        }

        public static byte[] allianceMemberOnline(int alliance, int gid, int id, boolean online) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(14);
            mplew.writeInt(alliance);
            mplew.writeInt(gid);
            mplew.writeInt(id);
            mplew.write(online ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] removeGuildFromAlliance(MapleGuildAlliance alliance, MapleGuild expelledGuild,
                boolean expelled) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(16);
            addAllianceInfo(mplew, alliance);
            CWvsContext.GuildPacket.GUILDDATA_Decode(mplew, expelledGuild);
            mplew.write(expelled ? 1 : 0);

            return mplew.getPacket();
        }

        public static byte[] addGuildToAlliance(MapleGuildAlliance alliance, MapleGuild newGuild) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(18);
            addAllianceInfo(mplew, alliance);
            mplew.writeInt(newGuild.getId());
            CWvsContext.GuildPacket.GUILDDATA_Decode(mplew, newGuild);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] sendAllianceInvite(String allianceName, MapleCharacter inviter) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(3);
            mplew.writeInt(inviter.getGuildId());
            mplew.writeMapleAsciiString(inviter.getName());
            mplew.writeMapleAsciiString(allianceName);

            return mplew.getPacket();
        }

        public static byte[] getAllianceUpdate(MapleGuildAlliance alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(23);
            addAllianceInfo(mplew, alliance);

            return mplew.getPacket();
        }

        public static byte[] createGuildAlliance(MapleGuildAlliance alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(15);
            addAllianceInfo(mplew, alliance);
            int noGuilds = alliance.getNoGuilds();
            MapleGuild[] g = new MapleGuild[noGuilds];
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                g[i] = World.Guild.getGuild(alliance.getGuildId(i));
                if (g[i] == null) {
                    return CWvsContext.enableActions();
                }
            }
            for (MapleGuild gg : g) {
                CWvsContext.GuildPacket.GUILDDATA_Decode(mplew, gg);
            }
            return mplew.getPacket();
        }

        public static byte[] updateAlliance(MapleGuildCharacter mgc, int allianceid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(24);
            mplew.writeInt(allianceid);
            mplew.writeInt(mgc.getGuildId());
            mplew.writeInt(mgc.getId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getJobCode());

            return mplew.getPacket();
        }

        public static byte[] updateAllianceLeader(int allianceid, int newLeader, int oldLeader) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(25);
            mplew.writeInt(allianceid);
            mplew.writeInt(oldLeader);
            mplew.writeInt(newLeader);

            return mplew.getPacket();
        }

        public static byte[] allianceRankChange(int aid, String[] ranks) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(26);
            mplew.writeInt(aid);
            for (String r : ranks) {
                mplew.writeMapleAsciiString(r);
            }

            return mplew.getPacket();
        }

        public static byte[] updateAllianceRank(MapleGuildCharacter mgc) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(27);
            mplew.writeInt(mgc.getId());
            mplew.write(mgc.getAllianceRank());

            return mplew.getPacket();
        }

        public static byte[] changeAllianceNotice(int allianceid, String notice) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(28);
            mplew.writeInt(allianceid);
            mplew.writeMapleAsciiString(notice);

            return mplew.getPacket();
        }

        public static byte[] disbandAlliance(int alliance) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(29);
            mplew.writeInt(alliance);

            return mplew.getPacket();
        }

        public static byte[] changeAlliance(MapleGuildAlliance alliance, boolean in) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(1);
            mplew.write(in ? 1 : 0);
            mplew.writeInt(in ? alliance.getId() : 0);
            int noGuilds = alliance.getNoGuilds();
            MapleGuild[] g = new MapleGuild[noGuilds];
            for (int i = 0; i < noGuilds; i++) {
                g[i] = World.Guild.getGuild(alliance.getGuildId(i));
                if (g[i] == null) {
                    return CWvsContext.enableActions();
                }
            }
            mplew.write(noGuilds);
            for (int i = 0; i < noGuilds; i++) {
                mplew.writeInt(g[i].getId());

                Collection<MapleGuildCharacter> members = g[i].getMembers();
                mplew.writeInt(members.size());
                for (MapleGuildCharacter mgc : members) {
                    mplew.writeInt(mgc.getId());
                    mplew.write(in ? mgc.getAllianceRank() : 0);
                }
            }

            return mplew.getPacket();
        }

        public static byte[] changeAllianceLeader(int allianceid, int newLeader, int oldLeader) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(2);
            mplew.writeInt(allianceid);
            mplew.writeInt(oldLeader);
            mplew.writeInt(newLeader);

            return mplew.getPacket();
        }

        public static byte[] changeGuildInAlliance(MapleGuildAlliance alliance, MapleGuild guild, boolean add) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(4);
            mplew.writeInt(add ? alliance.getId() : 0);
            mplew.writeInt(guild.getId());
            Collection<MapleGuildCharacter> members = guild.getMembers();
            mplew.writeInt(members.size());
            for (MapleGuildCharacter mgc : members) {
                mplew.writeInt(mgc.getId());
                mplew.write(add ? mgc.getAllianceRank() : 0);
            }

            return mplew.getPacket();
        }

        public static byte[] changeAllianceRank(int allianceid, MapleGuildCharacter player) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_AllianceResult.getValue());
            mplew.write(5);
            mplew.writeInt(allianceid);
            mplew.writeInt(player.getId());
            mplew.writeInt(player.getAllianceRank());

            return mplew.getPacket();
        }
    }

    public static class BuddyListPacket {

        public static byte[] buddylistIDA(BuddyOperation op, Collection<BuddylistEntry> buddylist, BuddylistEntry buddy,
                String str, int[] values) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_FriendResult.getValue());
            mplew.write(op.getValue() + 0x17);
            switch (op) {
            case 更新好友列表:
                addBuddylist(mplew, buddylist);
                break;
            case 好友申請:
                mplew.writeMapleAsciiString(str);
                break;
            case 增加好友:
                addBuddyInfo(mplew, buddy);
                break;
            case 刪除好友: {
                mplew.write(0);
                int characterid = values[0];
                mplew.writeInt(characterid);
                break;
            }
            case 更新好友頻道: {
                int characterid = values[0];
                int accountid = values[1];
                mplew.writeInt(characterid);
                mplew.writeInt(accountid);
                if (characterid < 0 || accountid < 0) {
                    break;
                }
                mplew.write(0);
                int channel = values[2];
                mplew.writeInt(channel);
                int unk = 0;
                mplew.write(unk);
                mplew.write(1);
                if (unk > 0) {
                    mplew.writeMapleAsciiString("");
                }
                break;
            }
            case 列表新增:
                mplew.write(values[0]); // 1為帳號好友添加
                mplew.writeInt(values[1]); // 角色ID
                mplew.writeInt(values[2]); // 帳號ID
                mplew.writeMapleAsciiString(str); // 角色名稱(or 備註?)
                mplew.writeInt(values[3]); // 等級
                mplew.writeInt(values[4]); // 職業
                mplew.writeInt(0);
                addNewBuddy(mplew, buddy);
                break;
            case 玩家的邀請好友:
                mplew.writeInt(0);
                mplew.writeMapleAsciiString(str);
                break;
            case UNK_1:
                unkBuddyAdd(mplew, buddy);
                break;
            case 好友容量:
                mplew.write(values[0]);
                break;
            case 未知錯誤:
            case 未知錯誤2:
            case 未知錯誤3:
            case 未知錯誤4:
            case 未知錯誤5:
            case 未知錯誤6:
                break;
            case 不在線無法加:
                mplew.writeMapleAsciiString(str);
                break;
            case 好友满:
            case 好友满2:
                break;
            case 阻擋滿:
                break;
            case 對方好友滿:
                break;
            case 已是好友:
            case 已是好友2:
                break;
            case 等待通過好友申請:
                break;
            case 不能加自己:
                break;
            case 未登錄過:
                break;
            case 角色不存在:
                break;
            case 不能加管理員:
            case 不能加管理員2:
                break;
            case 有保留目錄:
                break;
            case 已申請帳號好友:
            case 已申請帳號好友2:
                break;
            case 沒有找回力量:
                mplew.writeMapleAsciiString(str);
                break;
            case 的朋友拒絕請求:
                mplew.writeMapleAsciiString(str);
                break;
            case 拒絕好友邀請:
                mplew.writeMapleAsciiString(str);
                break;
            case UNK_E:
                mplew.writeInt(0);
                break;
            case UNK_35:
                break;
            case 拒絕追隨狀態:
                break;
            case 無法找到好友:
                break;
            case 追隨功能失敗:
                break;
            }

            return mplew.getPacket();
        }

        public static void addBuddyInfo(MaplePacketLittleEndianWriter mplew, BuddylistEntry buddy) {
            mplew.writeInt(buddy.getCharacterId());
            mplew.writeAsciiString(buddy.getName(), 15);
            mplew.write(buddy.isVisible() ? 0 : 1);// if adding = 2
            mplew.writeInt(buddy.getChannel() == -1 ? -1 : buddy.getChannel() - 1);
            mplew.writeAsciiString(buddy.getGroup(), 18);
            mplew.writeZeroBytes(295);
        }

        public static void addBuddylist(MaplePacketLittleEndianWriter mplew, Collection<BuddylistEntry> buddylist) {
            mplew.writeInt(buddylist.size());
            for (BuddylistEntry buddy : buddylist) {
                addBuddyInfo(mplew, buddy);
            }
        }

        public static void addNewBuddy(MaplePacketLittleEndianWriter mplew, BuddylistEntry buddy) {
            if (buddy != null) {
                addBuddyInfo(mplew, buddy);
            }
        }

        public static void unkBuddyAdd(MaplePacketLittleEndianWriter mplew, BuddylistEntry buddy) {
            mplew.writeInt(0);
            int value = 0;
            mplew.writeInt(value);
            int result;
            if (value > 0) {
                result = 0;
            } else {
                result = 0;
            }
            if ((result & 0x80000000) == 0) {
                addBuddyInfo(mplew, buddy);
            }
        }

        public static byte[] buddylistMessage(BuddyOperation op) {
            return buddylistIDA(op, null, null, null, null);
        }

        public static byte[] buddylistPrompt(BuddyOperation op, String nameFrom) {
            return buddylistIDA(op, null, null, nameFrom, null);
        }

        public static byte[] getBuddylist(Collection<BuddylistEntry> buddylist) {
            return buddylistIDA(BuddyOperation.更新好友列表, buddylist, null, null, null);
        }

        public static byte[] unkBuddyList(int mode) {
            return buddylistIDA(BuddyOperation.UNK_E, null, null, null, null);
        }

        public static byte[] deleteBuddy(int characterid) {
            return buddylistIDA(BuddyOperation.刪除好友, null, null, null, new int[] { characterid });
        }

        public static byte[] addBuddy(BuddylistEntry buddy) {
            return buddylistIDA(BuddyOperation.增加好友, null, buddy, null, null);
        }

        public static byte[] requestBuddylistAdd(boolean accountBuddy, int AccID, String nameFrom, int levelFrom,
                int jobFrom, BuddylistEntry buddy) {
            return buddylistIDA(BuddyOperation.列表新增, null, buddy, nameFrom,
                    new int[] { accountBuddy ? 1 : 0, buddy.getCharacterId(), AccID, levelFrom, jobFrom });
        }

        public static byte[] updateBuddyChannel(int characterid, int channel, int accountid) {
            return buddylistIDA(BuddyOperation.更新好友頻道, null, null, null, new int[] { characterid, accountid, channel });
        }

        public static byte[] updateBuddyCapacity(int capacity) {
            return buddylistIDA(BuddyOperation.好友容量, null, null, null, new int[] { capacity });
        }
    }

    public static byte[] giveKilling(int x) {
        if (ServerConfig.LOG_PACKETS) {
            System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
        PacketHelper.writeSingleMask(mplew, MapleBuffStat.KeyDownMoving);
        // mplew.writeInt(0);
        // mplew.write(0);
        // mplew.writeInt(x);
        // mplew.writeZeroBytes(6);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.writeInt(x);
        return mplew.getPacket();
    }

    public static class ExpeditionPacket {

        public static byte[] expeditionStatus(MapleExpedition me, boolean created, boolean silent) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ExpeditionNoti.getValue());
            mplew.write(created ? 86 : silent ? 72 : 76);// 74
            mplew.writeInt(me.getType().exped);
            mplew.writeInt(0);
            for (int i = 0; i < 6; i++) {
                if (i < me.getParties().size()) {
                    MapleParty party = World.Party.getParty((me.getParties().get(i)).intValue());

                    CWvsContext.PartyPacket.PARTYDATA__Decode(-1, party, mplew, false, true);
                } else {
                    CWvsContext.PartyPacket.PARTYDATA__Decode(-1, null, mplew, false, true);
                }

            }

            return mplew.getPacket();
        }

        public static byte[] expeditionError(int errcode, String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ExpeditionNoti.getValue());
            mplew.write(100);// 88
            mplew.writeInt(errcode);
            mplew.writeMapleAsciiString(name);

            return mplew.getPacket();
        }

        public static byte[] expeditionMessage(int code) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ExpeditionNoti.getValue());
            mplew.write(code);

            return mplew.getPacket();
        }

        public static byte[] expeditionJoined(String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ExpeditionNoti.getValue());
            mplew.write(87);// 75
            mplew.writeMapleAsciiString(name);

            return mplew.getPacket();
        }

        public static byte[] expeditionLeft(String name) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ExpeditionNoti.getValue());
            mplew.write(91);// 79
            mplew.writeMapleAsciiString(name);

            return mplew.getPacket();
        }

        public static byte[] expeditionLeaderChanged(int newLeader) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ExpeditionNoti.getValue());
            mplew.write(96);// 84
            mplew.writeInt(newLeader);

            return mplew.getPacket();
        }

        public static byte[] expeditionUpdate(int partyIndex, MapleParty party) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_ExpeditionNoti.getValue());
            mplew.write(97);// 85
            mplew.writeInt(0);
            mplew.writeInt(partyIndex);

            CWvsContext.PartyPacket.PARTYDATA__Decode(-1, party, mplew, false, true);

            return mplew.getPacket();
        }

        public static byte[] expeditionInvite(MapleCharacter from, int exped) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ExpeditionNoti.getValue());
            mplew.write(99);// 87
            mplew.writeInt(from.getLevel());
            mplew.writeInt(from.getJob());
            mplew.writeInt(0);
            mplew.writeMapleAsciiString(from.getName());
            mplew.writeInt(exped);

            return mplew.getPacket();
        }
    }

    public static class PartyPacket {

        public static byte[] partyOperationIDA(PartyOperation op, MapleParty party, MapleCharacter chr,
                MaplePartyCharacter pch, PartySearch ps, Point[] pos, int[] values, String[] strs) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_PartyResult.getValue());

            mplew.write(op.getValue());

            switch (op) {
            case PartyReq_InviteParty:
                mplew.writeInt(chr.getId());
                mplew.writeMapleAsciiString(chr.getName());
                mplew.writeInt(chr.getLevel());
                mplew.writeInt(chr.getJob());
                mplew.writeInt(chr.getSubcategory());
                mplew.write(0);
                boolean unk = true;
                mplew.write(unk);
                if (unk) {
                    mplew.write(0);
                }
                break;
            case PartyRes_JoinParty_OverDesiredSize:
                break;
            case PartyReq_InviteIntrusion:
                mplew.writeInt(chr.getId());
                mplew.writeMapleAsciiString(chr.getName());
                mplew.writeInt(chr.getLevel());
                mplew.writeInt(chr.getJob());
                mplew.writeInt(chr.getSubcategory());
                break;
            case PartyRes_InviteParty_Sent:
            case PartyRes_InviteIntrusion_Sent:
                mplew.writeMapleAsciiString(strs[0]);
                break;
            case PartyReq_ApplyParty:
                mplew.writeInt(chr.getId());
                mplew.writeMapleAsciiString(chr.getName());
                mplew.writeInt(chr.getLevel());
                mplew.writeInt(chr.getJob());
                mplew.writeInt(chr.getSubcategory());
                break;
            case PartyRes_ApplyParty_Sent:
                mplew.writeMapleAsciiString(strs[0]);
                break;
            case PartyRes_LoadParty_Done:
            case LOG_ONOFF:
                mplew.writeInt(party.getId());
                PartyPacket.PARTYDATA__Decode(values[0], party, mplew, op == PartyOperation.LOG_ONOFF);
                break;
            case PartyRes_CreateNewParty_Done:
                mplew.writeInt(party.getId());
                mplew.writeInt(999999999);
                mplew.writeInt(999999999);
                mplew.writeInt(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.write(party.isPrivate() ? 0 : 1);
                mplew.write(0);
                mplew.write(0);
                mplew.writeMapleAsciiString(party.getName());
                break;
            case PartyRes_WithdrawParty_Done:
            case LEAVE:
            case EXPEL:
            case DISBAND:
                // 離開隊伍 || 驅逐隊伍 || 解散隊伍
                mplew.writeInt(party.getId());
                mplew.writeInt(pch.getId());
                mplew.write(op != PartyOperation.DISBAND);
                if (op == PartyOperation.DISBAND) {
                    return mplew.getPacket();
                }
                mplew.write(op == PartyOperation.EXPEL);
                mplew.writeMapleAsciiString(pch.getName());
                PartyPacket.PARTYDATA__Decode(values[0], party, mplew, op == PartyOperation.LEAVE);
                break;
            case PartyRes_JoinParty_Done:
                mplew.writeInt(party.getId());
                mplew.writeMapleAsciiString(pch.getName());
                mplew.write(0);
                mplew.writeInt(0);
                PartyPacket.PARTYDATA__Decode(values[0], party, mplew, false);
                break;
            case PartyRes_JoinParty_Done2:
                mplew.write(0);
                mplew.writeInt(0);
                break;
            case PartyRes_UserMigration:
                mplew.writeInt(party.getId());
                PartyPacket.PARTYDATA__Decode(values[0], party, mplew, true); // TODO 組隊
                break;
            case PartyRes_ChangeLevelOrJob:
                mplew.writeInt(values[0]);
                mplew.writeInt(values[1]);
                mplew.writeInt(values[2]);
                break;
            case PartyInfo_TownPortalChanged:
                int animation = values[0];
                mplew.write(animation);
                mplew.writeInt(values[1]);
                mplew.writeInt(values[2]);
                mplew.writeInt(values[3]);
                mplew.writePos(pos[0]);
                if (animation < 6) {
                    break;
                }
                mplew.writeInt(values[4]); // 與PartyRes_ChangePartyBoss_Done相同
                mplew.write(values[5]);
                break;
            case PartyRes_ChangePartyBoss_Done:
            case PartyRes_ChangePartyBoss_Done_DC:
                mplew.writeInt(pch.getId());
                mplew.write(op == PartyOperation.PartyRes_ChangePartyBoss_Done_DC);
                break;
            case PartyRes_Unknown_40:
                mplew.writeInt(values[0]);
                mplew.write(values[1]);
                break;
            case PartyRes_UpdateShutdownStatus:
                mplew.writeInt(values[0]);
                mplew.write(values[1]);
                break;
            case PartyRes_SetAppliable:
                mplew.write(values[0]);
                break;
            case PartyRes_SetAppliableFailed:
                break;
            case PartyRes_FoundPossibleMember:
            case PartyRes_FoundPossibleParty:
                mplew.writeMapleAsciiString(strs[0]);
                if (true) {
                    mplew.writeInt(values[0]);
                    mplew.writeInt(values[1]);
                    mplew.writeInt(values[2]);
                    if (op != PartyOperation.PartyRes_FoundPossibleMember) {
                        mplew.writeInt(values[3]);
                    }
                }
                break;
            case PartyRes_Member_Rename:
                mplew.writeInt(party.getId());
                // if (v2->m_nPartyID == party.getId()) {
                PartyPacket.PARTYDATA__Decode(values[0], party, mplew, false);
                // }
                break;
            case AdverNoti_Change:
            case AdverNoti_Remove:
            case AdverNoti_GetAll:
            case AdverNoti_ResultApply:
            case AdverNoti_AddFail:
                break;
            case AdverNoti_Apply:
                mplew.writeInt(chr.getParty() == null ? 0 : chr.getParty().getId());
                mplew.writeMapleAsciiString(chr.getName());
                mplew.writeInt(chr.getLevel());
                mplew.writeInt(chr.getJob());
                mplew.writeInt(chr.getSubcategory());
                break;
            case PartyRes_SuccessToSelectPQReward:
                mplew.writeInt(0);
                mplew.writeMapleAsciiString("");
                mplew.write(0);
                break;
            case PartyRes_FailToSelectPQReward:
                if (true) {
                    mplew.write(0);
                }
                break;
            case PartyRes_ReceivePQReward:
                break;
            case PartyRes_FailToRequestPQReward:
                break;
            case PartyRes_PartySettingDone:
                mplew.write(false);
                mplew.writeMapleAsciiString("");
                break;
            case PartyRes_Load_StarGrade_Result:
                int unkSize = 0;
                mplew.writeInt(unkSize);
                for (int i = 0; i < unkSize; i++) {
                    mplew.writeInt(0); // dwCID
                    mplew.writeInt(0); // nGrade
                }
                break;
            case PartyRes_Load_StarGrade_Result2:
                mplew.writeInt(chr.getParty() == null ? 0 : chr.getParty().getId());
                unkSize = 0;
                mplew.writeInt(unkSize);
                for (int i = 0; i < unkSize; i++) {
                    mplew.writeInt(0); // dwCID
                    mplew.writeInt(0); // nGrade
                }
                break;
            case PartyRes_Unknown_57:
                mplew.write(0);
                break;
            case PartyRes_ChangePartyBoss_NotSameField:
                break;
            case PartyRes_ChangePartyBoss_NoMemberInSameField:
                break;
            case PartyRes_ChangePartyBoss_NotSameChannel:
                break;
            case PartyRes_CreateNewParty_AlreayJoined:
                break;
            case PartyRes_CreateNewParty_Beginner:
                break;
            case PartyRes_WithdrawParty_NotJoined:
                break;
            case PartyRes_JoinParty_AlreadyJoined:
                break;
            case PartyRes_JoinParty_AlreadyFull:
                break;
            case PartyRes_AdminCannotCreate:
                break;
            case PartyRes_KickParty_FieldLimit:
                break;
            case PartyRes_CanNotInThisField:
                break;
            case PartyRes_InAnotherWorld:
                break;
            case PartyRes_InAnotherChanelBlockedUser:
                break;
            case PartyRes_Unknown_34:
                break;
            case PartyRes_Unknown_58:
                break;
            default:
                break;
            }/*
              * if (op.getValue() > PartyOperation.UNK_35.getValue()) { if (op.getValue() <=
              * PartyOperation.UNK_73.getValue()) { if (op.getValue() <
              * PartyOperation.UNK_71.getValue()) { switch (op) { case MSG_43:
              * mplew.writeMapleAsciiString(strs[0]); break; case UNK_39:
              * mplew.writeInt(values[0]); PartyPacket.PARTYDATA__Decode(-1, party, mplew,
              * true); // TODO 組隊 break; case UNK_3A: mplew.writeInt(values[0]);
              * mplew.writeInt(values[1]); mplew.writeInt(values[2]); break; case
              * PARTY_PORTAL: int animation = values[0]; mplew.write(animation);
              * mplew.writeInt(values[1]); mplew.writeInt(values[2]);
              * mplew.writeInt(values[3]); mplew.writePos(pos[0]); if (animation <= 5) {
              * break; } mplew.writeInt(values[4]); // 與UNK_3B相同 mplew.write(values[5]);
              * break; case UNK_3B: mplew.writeInt(values[0]); mplew.write(values[1]); break;
              * case UNK_3C: mplew.write(values[0]); break; case UNK_3D: break; case UNK_4B:
              * case UNK_4C: mplew.writeMapleAsciiString(strs[0]); if (true) { break; }
              * mplew.writeInt(values[0]); mplew.writeInt(values[1]);
              * mplew.writeInt(values[2]); if (true) { break; } mplew.writeInt(values[3]);
              * break; case UNK_3E: mplew.writeInt(values[0]);
              * mplew.writeMapleAsciiString(strs[0]); mplew.write(values[1]); break; case
              * UNK_3F: case UNK_40: case UNK_41: break; case INFO_UPDATE:
              * mplew.write(party.isPrivate() ? 0 : 1);
              * mplew.writeMapleAsciiString(party.getName()); break; case UNK_4E: { int value
              * = values[0]; mplew.writeInt(value); for (int i = value; i > 1; i--) {
              * mplew.writeInt(0); mplew.writeInt(0); } break; } case UNK_4F: {
              * mplew.writeInt(values[0]); int value = values[1]; mplew.writeInt(value); for
              * (int i = value; i > 1; i--) { mplew.writeInt(0); mplew.writeInt(0); } break;
              * } case UNK_42: break; case UNK_37: break; case MSG_38: break; default: break;
              * } return mplew.getPacket(); } } if (op.getValue() ==
              * PartyOperation.UNK_74.getValue()) { mplew.writeInt(values[0]);
              * mplew.writeMapleAsciiString(strs[0]); mplew.writeInt(values[1]);
              * mplew.writeInt(values[2]); mplew.writeInt(values[3]); } if (op.getValue() >
              * PartyOperation.UNK_74.getValue() && op.getValue() <=
              * PartyOperation.UNK_76.getValue()) { } return mplew.getPacket(); } if
              * (op.getValue() == PartyOperation.UNK_35.getValue()) { return
              * mplew.getPacket(); } if (op.getValue() <= PartyOperation.MSG_19.getValue()) {
              * if (op.getValue() != PartyOperation.MSG_19.getValue()) { if (op.getValue() >
              * PartyOperation.UNK_11.getValue()) { int value = op.getValue() - 18; if (value
              * <= 0) { return mplew.getPacket(); } int value2 = value - 3; if (value2 <= 0)
              * { // 離開隊伍 || 驅逐隊伍 || 解散隊伍 mplew.writeInt(party.getId());
              * mplew.writeInt(pch.getId()); mplew.write(op != PartyOperation.DISBAND); if
              * (op == PartyOperation.DISBAND) { mplew.writeInt(pch.getId()); return
              * mplew.getPacket(); } mplew.write(op == PartyOperation.EXPEL);
              * mplew.writeMapleAsciiString(pch.getName());
              * PartyPacket.PARTYDATA__Decode(values[0], party, mplew, op ==
              * PartyOperation.LEAVE); } int value3 = value2 - 1; if (value3 <= 0) { } if
              * (value3 == 2) { // 加入隊伍 mplew.writeInt(party.getId());
              * mplew.writeMapleAsciiString(pch.getName());
              * PartyPacket.PARTYDATA__Decode(values[0], party, mplew, false); } } if
              * (op.getValue() == PartyOperation.UNK_11.getValue()) { return
              * mplew.getPacket(); } int value = op.getValue() - 4; if (value <= 0) {
              * mplew.writeInt(chr.getParty() == null ? 0 : chr.getParty().getId());
              * mplew.writeMapleAsciiString(chr.getName()); mplew.writeInt(chr.getLevel());
              * mplew.writeInt(chr.getJob()); mplew.writeInt(0); mplew.write(0); int unk = 0;
              * mplew.write(unk); if (unk > 0) { mplew.write(0); } return mplew.getPacket();
              * } int value2 = value - 1; if (value2 > 0) { int value3 = value2 - 3; if
              * (value3 > 0) { int value4 = value3 - 7; if (value4 <= 0) { // 更新隊伍 ||
              * 更新隊員線上狀態 mplew.writeInt(party.getId());
              * PartyPacket.PARTYDATA__Decode(values[0], party, mplew, op ==
              * PartyOperation.LOG_ONOFF); } if (value4 == 1) {
              * mplew.writeInt(party.getId()); mplew.writeInt(999999999);
              * mplew.writeInt(999999999); mplew.writeInt(0); mplew.writeShort(0);
              * mplew.writeShort(0); mplew.write(party.isPrivate() ? 0 : 1);
              * mplew.writeMapleAsciiString(party.getName()); } return mplew.getPacket(); }
              * mplew.writeInt(chr.getId()); mplew.writeMapleAsciiString(chr.getName());
              * mplew.writeInt(chr.getLevel()); mplew.writeInt(chr.getJob());
              * mplew.writeInt(0); } else { mplew.writeInt(chr.getId());
              * mplew.writeMapleAsciiString(chr.getName()); mplew.writeInt(chr.getLevel());
              * mplew.writeInt(chr.getJob()); mplew.writeInt(0); } } } if (op.getValue() >
              * PartyOperation.UNK_2E.getValue()) { int value = op.getValue() - 48; if (value
              * > 0) { return mplew.getPacket(); } // 委任隊長 || 自動委任隊長
              * mplew.writeInt(pch.getId()); mplew.write(op ==
              * PartyOperation.CHANGE_LEADER_DC); } else { if (op.getValue() ==
              * PartyOperation.UNK_2E.getValue()) { return mplew.getPacket(); } int value =
              * op.getValue() - 26; if (value <= 0) { return mplew.getPacket(); } int value2
              * = value - 1; if (value <= 0) { return mplew.getPacket(); } int value3 =
              * value2 - 1; if (value3 > 0) { int value4 = value3 - 5; if (value4 <= 0 ||
              * value4 == 6) { mplew.writeMapleAsciiString(strs[0]); } } }
              */

            return mplew.getPacket();
        }

        public static byte[] partyCreated(MapleParty party) { // 建立隊伍
            return partyOperationIDA(PartyOperation.PartyRes_CreateNewParty_Done, party, null, null, null, null, null,
                    null);
        }

        public static byte[] partyInvite(MapleCharacter from) { // 隊伍邀請
            return partyOperationIDA(PartyOperation.PartyReq_InviteParty, null, from, null, null, null, null, null);
        }

        public static byte[] partyRequestInvite(MapleCharacter from) { // 隊伍邀請回覆
            return partyOperationIDA(PartyOperation.PartyReq_ApplyParty, null, from, null, null, null, null, null);
        }

        public static byte[] partyStatusMessage(PartyOperation op) {
            return partyStatusMessage(op, null);
        }

        public static byte[] partyStatusMessage(PartyOperation op, String charname) {
            if (charname == null) {
                return partyOperationIDA(op, null, null, null, null, null, null, null);
            } else {
                return partyOperationIDA(op, null, null, null, null, null, null, new String[] { charname });
            }
        }

        public static void PARTYDATA__Decode(int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew,
                boolean leaving) {
            PARTYDATA__Decode(forchannel, party, lew, leaving, false);
        }

        public static void PARTYDATA__Decode(int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew,
                boolean leaving, boolean exped) {
            List<MaplePartyCharacter> partymembers;
            if (party == null) {
                partymembers = new ArrayList<>();
            } else {
                partymembers = new ArrayList<>(party.getMembers());
            }
            while (partymembers.size() < 6) {
                partymembers.add(new MaplePartyCharacter());
            }
            // CInPacket::DecodeBuffer{
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getId());
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeAsciiString(partychar.getName(), 15);
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getJobId());
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getSubcategory());
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getLevel());
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.isOnline() ? partychar.getChannel() - 1 : -2);
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(0);
            }
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(0);
            }
            lew.writeInt(party == null ? 0 : party.getLeader().getId());
            // }
            if (exped) {
                return;
            }
            // CInPacket::DecodeBuffer{
            for (MaplePartyCharacter partychar : partymembers) {
                lew.writeInt(partychar.getChannel() == forchannel ? partychar.getMapid() : 0);
            }
            // }
            // CInPacket::DecodeBuffer{
            for (MaplePartyCharacter partychar : partymembers) {
                if ((partychar.getChannel() == forchannel) && (!leaving)) {
                    lew.writeInt(partychar.getDoorTown());
                    lew.writeInt(partychar.getDoorTarget());
                    lew.writeInt(partychar.getDoorSkill());
                    lew.writeInt(partychar.getDoorPosition().x);
                    lew.writeInt(partychar.getDoorPosition().y);
                } else {
                    lew.writeInt(leaving ? 999999999 : 0);
                    lew.writeInt(leaving ? 999999999 : 0);
                    lew.writeInt(0);
                    lew.writeInt(leaving ? -1 : 0);
                    lew.writeInt(leaving ? -1 : 0);
                }
            }
            // }
            lew.write(party == null || party.isPrivate() ? 0 : 1);
            lew.write(false);
            lew.writeMapleAsciiString(party == null ? "" : party.getName());
            // Function{
            lew.write(false);
            lew.write(0);
            int unkSize = 0;
            lew.write(unkSize);
            for (int i = 0; i < unkSize; i++) {
                lew.writeInt(0);
                lew.writeInt(0);
                lew.writeMapleAsciiString("");
                lew.writeInt(0);
            }
            for (int i = 0; i < 3; i++) {
                lew.write(0);
                lew.write(0);
                boolean unk = false;
                lew.write(unk);
                if (unk) {
                    // func
                }
                lew.write(0);
                lew.writeInt(0);
                lew.writeLong(0); // double
                lew.writeInt(0);
                lew.writeInt(0);
            }
            // }
        }

        public static byte[] updateParty(int forChannel, MapleParty party, PartyOperation op,
                MaplePartyCharacter target) {
            return partyOperationIDA(op, party, null, target, null, null, new int[] { forChannel }, null);
        }

        public static byte[] partyPortal(int townId, int targetId, int skillId, Point position, boolean animation) {
            return partyOperationIDA(PartyOperation.PartyInfo_TownPortalChanged, null, null, null, null,
                    new Point[] { position }, new int[] { animation ? 0 : 1, townId, targetId, skillId }, null);
        }

        public static byte[] getPartyListing(PartySearchType pst) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_PartyResult.getValue());
            mplew.write(0x60); // Version.176 [推測]
            mplew.writeInt(pst.id);
            final List<PartySearch> parties = World.Party.searchParty(pst);
            mplew.writeInt(parties.size());
            for (PartySearch party : parties) {
                mplew.writeInt(0);
                mplew.writeInt(2);
                if (pst.exped) {
                    MapleExpedition me = World.Party.getExped(party.getId());
                    mplew.writeInt(me.getType().maxMembers);
                    mplew.writeInt(party.getId());
                    mplew.writeAsciiString(party.getName(), 48);
                    for (int i = 0; i < 5; i++) {
                        if (i < me.getParties().size()) {
                            MapleParty part = World.Party.getParty(me.getParties().get(i));
                            if (part != null) {
                                PARTYDATA__Decode(-1, part, mplew, false, true);
                            } else {
                                mplew.writeZeroBytes(202);
                            }
                        } else {
                            mplew.writeZeroBytes(202);
                        }
                    }
                } else {
                    mplew.writeInt(0);
                    mplew.writeInt(party.getId());
                    mplew.writeAsciiString(party.getName(), 48);
                    PARTYDATA__Decode(-1, World.Party.getParty(party.getId()), mplew, false, true);
                }

                mplew.writeShort(0);
            }

            return mplew.getPacket();
        }

        public static byte[] partyListingAdded(PartySearch ps) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_PartyResult.getValue());
            mplew.write(0x70); // Version.176 [推測]
            mplew.writeInt(ps.getType().id);
            mplew.writeInt(0);
            mplew.writeInt(1);
            if (ps.getType().exped) {
                MapleExpedition me = World.Party.getExped(ps.getId());
                mplew.writeInt(me.getType().maxMembers);
                mplew.writeInt(ps.getId());
                mplew.writeAsciiString(ps.getName(), 48);
                for (int i = 0; i < 5; i++) {
                    if (i < me.getParties().size()) {
                        MapleParty party = World.Party.getParty(me.getParties().get(i));
                        if (party != null) {
                            PARTYDATA__Decode(-1, party, mplew, false, true);
                        } else {
                            mplew.writeZeroBytes(202);
                        }
                    } else {
                        mplew.writeZeroBytes(202);
                    }
                }
            } else {
                mplew.writeInt(0);
                mplew.writeInt(ps.getId());
                mplew.writeAsciiString(ps.getName(), 48);
                PARTYDATA__Decode(-1, World.Party.getParty(ps.getId()), mplew, false, true);
            }
            mplew.writeShort(0);

            return mplew.getPacket();
        }

        public static byte[] showMemberSearch(List<MapleCharacter> players) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_PartyMemberCandidateResult.getValue());
            mplew.write(players.size());
            for (MapleCharacter chr : players) {
                mplew.writeInt(chr.getId());
                mplew.writeMapleAsciiString(chr.getName());
                mplew.writeInt(chr.getJob());
                mplew.write(chr.getLevel());
            }
            return mplew.getPacket();
        }

        public static byte[] showPartySearch(List<MapleParty> chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_PartyCandidateResult.getValue());
            mplew.write(chr.size());
            for (MapleParty party : chr) {
                mplew.writeInt(party.getId());
                mplew.writeMapleAsciiString(party.getLeader().getName());
                mplew.write(party.getLeader().getLevel());
                mplew.write(party.getLeader().isOnline() ? 1 : 0);
                mplew.writeMapleAsciiString(party.getName());
                mplew.write(party.getMembers().size());
                for (MaplePartyCharacter partyChr : party.getMembers()) {
                    mplew.writeInt(partyChr.getId());
                    mplew.writeMapleAsciiString(partyChr.getName());
                    mplew.writeInt(partyChr.getJobId());
                    mplew.write(partyChr.getLevel());
                    mplew.write(partyChr.isOnline() ? 1 : 0);
                    mplew.write(-1);
                }
            }
            return mplew.getPacket();
        }
    }

    public static class GuildPacket {

        public static byte[] OnGuildResult(GuildOpcode opGuild, MapleGuild cGuild, MapleCharacter dwCharacter,
                MapleGuildCharacter dwGuildMember, MapleGuildSkill cSkill, String sMsg, int nValue,
                List<MapleGuildRanking.GuildRankingInfo> aGuildRanking) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_GuildResult.getValue());

            mplew.write(opGuild.getValue());
            switch (opGuild) {
            case GuildReq_InputGuildName:
                break;
            case GuildReq_InputMark:
                break;
            case GuildReq_CreateGuildAgree:
                mplew.writeMapleAsciiString(sMsg); // sName
                break;
            case GuildReq_InviteGuild:
                if (cGuild == null || dwCharacter == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // dwInviterID
                }
                mplew.writeMapleAsciiString(dwCharacter.getName()); // sRequestUserName
                mplew.writeInt(dwCharacter.getLevel()); // nLevel
                mplew.writeInt(dwCharacter.getJob()); // nJobCode
                mplew.writeInt(dwCharacter.getSubcategory()); // nSubJobCode
                break;
            case GuildRes_InviteGuild_BlockedUser:
                mplew.writeMapleAsciiString(sMsg); // sMsg
                break;
            case GuildRes_InviteGuild_AlreadyInvited:
                mplew.writeMapleAsciiString(sMsg); // sMsg
                break;
            case GuildRes_InviteGuild_Rejected:
                mplew.writeMapleAsciiString(sMsg); // sMsg
                break;
            case GuildRes_LoadGuild_Done:
                mplew.write(false);
                mplew.write(cGuild != null);
                if (cGuild != null) {
                    GUILDDATA_Decode(mplew, cGuild);
                    mplew.writeInt(GameConstants.GuildNeedPoint.length);
                    for (int nNeedPoint : GameConstants.GuildNeedPoint) {
                        mplew.writeInt(nNeedPoint);
                    }
                }
                break;
            case GuildRes_FindGuild_Done:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_nFindGuildID
                }
                GUILDDATA_Decode(mplew, cGuild);
                break;
            case GuildRes_CheckGuildName_AlreadyUsed:
                break;
            case GuildRes_CreateNewGuild_Done:
                GUILDDATA_Decode(mplew, cGuild);
                break;
            case GuildRes_CreateNewGuild_Disagree:
                break;
            case GuildRes_JoinGuild_Done:
                if (dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(dwGuildMember.getGuildId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // m_dwCharacterID
                GUILDMEMBER_Decode(mplew, dwGuildMember);
                break;
            // case 0x40:
            // mplew.writeInt(0);
            // mplew.writeInt(0);
            // break;
            case GuildRes_JoinRequest_Done:
                if (dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(dwGuildMember.getGuildId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // sNewMasterName._m_pStr
                GUILDMEMBER_Decode(mplew, dwGuildMember);
                break;
            case GuildRes_WithdrawGuild_Done:
            case GuildRes_KickGuild_Done:
                if (dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(dwGuildMember.getGuildId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // sNewMasterName._m_pStr
                mplew.writeMapleAsciiString(dwGuildMember.getName()); // sCharacterName
                break;
            case GuildRes_RemoveGuild_Done:
                mplew.writeInt(nValue); // m_guild.nGuildID
                break;
            case GuildRes_RemoveRequestGuild_Done:
                break;
            case GuildRes_IncMaxMemberNum_Done:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.write(cGuild.getCapacity()); // m_guild.nMaxMemberNum
                break;
            case GuildRes_SetMemberCommitment_Done:
                if (dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(dwGuildMember.getGuildId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // m_dwCharacterID
                mplew.writeInt(dwGuildMember.getGuildContribution()); // m_guild.aMemberData.a[v108].nCommitment
                mplew.writeInt(500); // m_guild.aMemberData.a[v108].nDayCommitment
                mplew.writeInt(350); // m_guild.aMemberData.a[v108].nIGP
                mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis())); // ftCommitmentIncTime.dwLowDateTime
                break;
            case GuildRes_SetIGP_Done:
                if (cGuild == null || dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // m_dwCharacterID
                mplew.writeInt(0); // m_guild.aMemberData.a[v112].nIGP
                break;
            case GuildRes_SetMemberGrade_Done:
                if (dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(dwGuildMember.getGuildId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // m_dwCharacterID
                mplew.write(dwGuildMember.getGuildRank()); // m_guild.aMemberData.a[v116].nGrade
                break;
            case GuildRes_SetGradeName_Done:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeMapleAsciiString(cGuild.getRankTitle(1)); // m_guild.asGradeName.a[0]
                mplew.writeMapleAsciiString(cGuild.getRankTitle(2)); // m_guild.asGradeName.a[1]
                mplew.writeMapleAsciiString(cGuild.getRankTitle(3)); // m_guild.asGradeName.a[2]
                mplew.writeMapleAsciiString(cGuild.getRankTitle(4)); // m_guild.asGradeName.a[3]
                mplew.writeMapleAsciiString(cGuild.getRankTitle(5)); // m_guild.asGradeName.a[4]
                break;
            case GuildRes_SetMark_Done:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeShort(cGuild.getLogoBG()); // m_guild.nMarkBg
                mplew.write(cGuild.getLogoBGColor()); // m_guild.nMarkBgColor
                mplew.writeShort(cGuild.getLogo()); // m_guild.nMark
                mplew.write(cGuild.getLogoColor()); // m_guild.nMarkColor
                break;
            case GuildRes_IncPoint_Done:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeInt(cGuild.getGP()); // m_guild.nPoint
                mplew.writeInt(cGuild.getLevel()); // m_guild.nLevel
                mplew.writeInt(0);
                break;
            case GuildRes_SetGGP_Done:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeInt(0); // m_guild.nGGP
                break;
            case GuildRes_SetNotice_Done:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeMapleAsciiString(cGuild.getNotice()); // GuildRes_SetNotice_Done
                break;
            case GuildRes_ChangeMemberName:
                if (dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(dwGuildMember.getGuildId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // m_dwCharacterID
                mplew.writeMapleAsciiString(dwGuildMember.getName()); // m_guild.aMemberData.a[v138].sCharacterName
                break;
            case GuildRes_ChangeRequestUserName:
                if (cGuild == null || dwCharacter == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwCharacter.getId()); // m_dwCharacterID
                mplew.writeMapleAsciiString(dwCharacter.getName()); // m_guild.aRequestUserData.a[v139].sCharacterName
                break;
            case GuildRes_ChangeLevelOrJob:
                if (dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(dwGuildMember.getGuildId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // m_dwCharacterID
                mplew.writeInt(dwGuildMember.getLevel()); // m_guild.aMemberData.a[v142].nLevel
                mplew.writeInt(dwGuildMember.getJobCode()); // m_guild.aMemberData.a[v142].nJob
                break;
            case GuildRes_NotifyLoginOrLogout:
                if (dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(dwGuildMember.getGuildId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // m_dwCharacterID
                mplew.write(dwGuildMember.isOnline()); // m_guild.aMemberData.a[v149].bOnLine
                mplew.write(0);
                break;
            case GuildRes_CreateGuildAgree_Unknown:
                break;
            case GuildRes_CreateNewGuild_Unknown:
                break;
            case GuildRes_RemoveGuild_Unknown:
                break;
            case GuildRes_IncMaxMemberNum_Unknown:
                break;
            case GuildRes_ShowGuildRanking:
                mplew.writeInt(nValue); // nNPCID
                mplew.writeInt(aGuildRanking.size());
                int i = 0;
                for (MapleGuildRanking.GuildRankingInfo cGuildRank : aGuildRanking) {
                    mplew.writeShort(i++); // e.nRank
                    mplew.writeMapleAsciiString(cGuildRank.getName()); // e.sGuildName
                    mplew.writeInt(cGuildRank.getGP()); // e.nPoint
                    mplew.writeInt(cGuildRank.getLogo()); // e.nMark
                    mplew.writeInt(cGuildRank.getLogoColor()); // e.nMarkColor
                    mplew.writeInt(cGuildRank.getLogoBg()); // e.nMarkBG
                    mplew.writeInt(cGuildRank.getLogoBgColor()); // e.nMarkBGColor
                }
                break;
            case GuildRes_SetSkill_ResetBattleSkill:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                break;
            case GuildRes_SetSkill_Done:
                if (cGuild == null || cSkill == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeInt(cSkill.skillID); // nSkillID
                mplew.writeInt(0); // MaxLevel
                GUILDDATA_SKILLENTRY_Decode(mplew, cSkill);
                break;
            case GuildRes_SetSkill_Extend_Unknown:
                break;
            case GuildRes_SetSkill_LevelSet_Unknown:
                mplew.write(false);
                break;
            case GuildRes_BattleSkillOpen:
                mplew.writeInt(0); // m_nTotalGuildBattleSP
                break;
            case GuildRes_UseSkill_Err:
                mplew.writeInt(0); // nType
                break;
            case GuildRes_ChangeName_Done:
                if (cGuild == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeMapleAsciiString(cGuild.getName()); // sMsg
                break;
            case GuildRes_ChangeMaster_Done:
                if (cGuild == null || dwGuildMember == null) {
                    mplew.writeInt(0);
                    break;
                } else {
                    mplew.writeInt(cGuild.getId()); // m_guild.nGuildID
                }
                mplew.writeInt(dwGuildMember.getId()); // m_dwCharacterID
                mplew.writeInt(cGuild.getLeaderId()); // m_dwNewMasterCharacterID
                boolean bAllianceChange = true;
                mplew.write(bAllianceChange);
                if (bAllianceChange) {
                    mplew.writeInt(cGuild.getAllianceId()); // m_guild.nAllianceID
                }
                break;
            case GuildRes_GuildQuest_NoticeOrder:
                mplew.write(0); // nChannelID
                mplew.writeInt(0); // nQuestID
                break;
            case GuildRes_JoinCancelRequest_Done:
                mplew.writeInt(nValue); // m_dwCharacterID
                break;
            case GuildRes_JoinGuild_NonRequestFindUser:
                mplew.writeInt(nValue); // m_dwCharacterID
                break;
            case GuildRes_Rank_Reflash:
                mplew.writeInt(nValue); // m_guild.nRank
                break;
            case GuildRes_GuildQuest_NotEnoughUser:
                break;
            case GuildRes_GuildQuest_RegisterDisconnected:
                break;
            case GuildRes_CreateNewGuild_AlreayJoined:
                break;
            case GuildRes_CreateNewGuild_Beginner:
                break;
            case GuildRes_JoinGuild_AlreadyJoined:
                break;
            case GuildRes_JoinGuild_AlreadyFull:
                break;
            // case 0x42:
            // break;
            case GuildRes_WithdrawGuild_NotJoined:
                break;
            case GuildRes_KickGuild_NotJoined:
                break;
            case GuildRes_AdminCannotCreate:
                break;
            case GuildRes_JoinGuild_UnknownUser:
                break;
            case GuildRes_BlockedBehaviorCreate:
                break;
            case GuildRes_BlockedBehaviorJoin:
                break;
            case GuildRes_SetMark_Unknown:
                break;
            case GuildRes_JoinRequest_LimitTime:
                break;
            case GuildReq_CreateNewGuild_Block:
                break;
            case GuildReq_CreateNewAlliance_Block:
                break;
            case GuildRes_ChangeMaster_Pinkbean:
                break;
            case GuildRes_Authkey_Update:
            case GuildRes_UseSkill_Success:
                break;
            default:
                break;
            }

            return mplew.getPacket();
        }

        public static byte[] genericGuildMessage(GuildOpcode opGuild) {
            return OnGuildResult(opGuild, null, null, null, null, null, 0, null);
        }

        public static byte[] createGuildNotice(String sName) {
            return OnGuildResult(GuildOpcode.GuildReq_CreateGuildAgree, null, null, null, null, sName, 0, null);
        }

        public static byte[] guildInvite(MapleGuild cGuild, MapleCharacter dwCharacter) {
            return OnGuildResult(GuildOpcode.GuildReq_InviteGuild, cGuild, dwCharacter, null, null, null, 0, null);
        }

        public static byte[] showGuildInfo(MapleGuild cGuild) {
            return OnGuildResult(GuildOpcode.GuildRes_LoadGuild_Done, cGuild, null, null, null, null, 0, null);
        }

        public static byte[] getGuildReceipt(MapleGuild cGuild) {
            return OnGuildResult(GuildOpcode.GuildRes_FindGuild_Done, cGuild, null, null, null, null, 0, null);
        }

        public static byte[] newGuildInfo(MapleGuild cGuild) {
            return OnGuildResult(GuildOpcode.GuildRes_CreateNewGuild_Done, cGuild, null, null, null, null, 0, null);
        }

        public static byte[] newGuildMember(MapleGuildCharacter dwGuildMember) {
            return OnGuildResult(GuildOpcode.GuildRes_JoinGuild_Done, null, null, dwGuildMember, null, null, 0, null);
        }

        public static byte[] newGuildJoinMember(MapleGuildCharacter dwGuildMember) {
            return OnGuildResult(GuildOpcode.GuildRes_JoinRequest_Done, null, null, dwGuildMember, null, null, 0, null);
        }

        public static byte[] removeGuildJoin(int cid) {
            return OnGuildResult(GuildOpcode.GuildRes_JoinCancelRequest_Done, null, null, null, null, null, cid, null);
        }

        public static byte[] memberLeft(MapleGuildCharacter dwGuildMember, boolean bExpelled) {
            return OnGuildResult(
                    bExpelled ? GuildOpcode.GuildRes_KickGuild_Done : GuildOpcode.GuildRes_WithdrawGuild_Done, null,
                    null, dwGuildMember, null, null, 0, null);
        }

        public static byte[] guildDisband(int gid) {
            return OnGuildResult(GuildOpcode.GuildRes_RemoveGuild_Done, null, null, null, null, null, gid, null);
        }

        public static byte[] denyGuildInvitation(String charname) {
            return OnGuildResult(GuildOpcode.GuildRes_InviteGuild_Rejected, null, null, null, null, charname, 0, null);
        }

        public static byte[] guildCapacityChange(MapleGuild cGuild) {
            return OnGuildResult(GuildOpcode.GuildRes_IncMaxMemberNum_Done, cGuild, null, null, null, null, 0, null);
        }

        public static byte[] guildMemberLevelJobUpdate(MapleGuildCharacter dwGuildMember) {
            return OnGuildResult(GuildOpcode.GuildRes_ChangeLevelOrJob, null, null, dwGuildMember, null, null, 0, null);
        }

        public static byte[] guildMemberOnline(MapleGuildCharacter dwGuildMember) {
            return OnGuildResult(GuildOpcode.GuildRes_NotifyLoginOrLogout, null, null, dwGuildMember, null, null, 0,
                    null);
        }

        public static byte[] rankTitleChange(MapleGuild cGuild) {
            return OnGuildResult(GuildOpcode.GuildRes_SetGradeName_Done, cGuild, null, null, null, null, 0, null);
        }

        public static byte[] changeRank(MapleGuildCharacter dwGuildMember) { // 變更公會職位
            return OnGuildResult(GuildOpcode.GuildRes_SetMemberGrade_Done, null, null, dwGuildMember, null, null, 0,
                    null);
        }

        public static byte[] guildContribution(MapleGuildCharacter dwGuildMember) {
            return OnGuildResult(GuildOpcode.GuildRes_SetMemberCommitment_Done, null, null, dwGuildMember, null, null,
                    0, null);
        }

        public static byte[] guildEmblemChange(MapleGuild cGuild) {
            return OnGuildResult(GuildOpcode.GuildRes_SetMark_Done, cGuild, null, null, null, null, 0, null);
        }

        public static byte[] guildNotice(MapleGuild cGuild) {
            return OnGuildResult(GuildOpcode.GuildRes_SetNotice_Done, cGuild, null, null, null, null, 0, null);
        }

        public static byte[] updateGP(MapleGuild cGuild) {
            return OnGuildResult(GuildOpcode.GuildRes_IncPoint_Done, cGuild, null, null, null, null, 0, null);
        }

        public static byte[] showGuildRanks(int nNPCID, List<MapleGuildRanking.GuildRankingInfo> aGuildRanking) {
            return OnGuildResult(GuildOpcode.GuildRes_ShowGuildRanking, null, null, null, null, null, nNPCID,
                    aGuildRanking);
        }

        public static byte[] guildSkillPurchased(MapleGuild cGuild, MapleGuildSkill cSkill) {
            return OnGuildResult(GuildOpcode.GuildRes_SetSkill_Done, cGuild, null, null, cSkill, null, 0, null);
        }

        public static byte[] guildLeaderChanged(MapleGuild cGuild, MapleGuildCharacter dwGuildMember) {
            return OnGuildResult(GuildOpcode.GuildRes_ChangeMaster_Done, cGuild, null, dwGuildMember, null, null, 0,
                    null);
        }

        public static void GUILDMEMBER_Decode(MaplePacketLittleEndianWriter mplew, MapleGuildCharacter mgc) {
            mplew.writeAsciiString(mgc.getName(), 15);
            mplew.writeInt(mgc.getJobCode());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(mgc.getAllianceRank());
            mplew.writeInt(mgc.getGuildContribution());
            mplew.writeInt(0);// 可能是GP+IGP
            mplew.writeInt(0);// IGP
            mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        }

        public static void GUILDDATA_Decode(MaplePacketLittleEndianWriter mplew, MapleGuild guild) {
            mplew.writeInt(guild.getId()); // nGuildID
            mplew.writeMapleAsciiString(guild.getName()); // sGuildName
            for (int i = 1; i <= 5; i++) {
                mplew.writeMapleAsciiString(guild.getRankTitle(i));
            }
            guild.addMemberData(mplew);
            guild.addMemberForm(mplew);
            mplew.writeInt(guild.getCapacity()); // nMaxMemberNum
            mplew.writeShort(guild.getLogoBG()); // nMarkBg
            mplew.write(guild.getLogoBGColor()); // nMarkBgColor
            mplew.writeShort(guild.getLogo()); // nMark
            mplew.write(guild.getLogoColor()); // nMarkColor
            mplew.writeMapleAsciiString(guild.getNotice()); // sNotice
            mplew.writeInt(guild.getGP()); // nPoint
            mplew.writeInt(guild.getGP()); // nSeasonPoint
            mplew.writeInt(guild.getAllianceId() > 0 ? guild.getAllianceId() : 0); // nAllianceID
            mplew.write(guild.getLevel()); // nLevel
            mplew.writeShort(0); // nRank
            mplew.writeInt(0); // nGGP
            mplew.writeShort(guild.getSkills().size());
            for (MapleGuildSkill skill : guild.getSkills()) {
                mplew.writeInt(skill.skillID);
                GUILDDATA_SKILLENTRY_Decode(mplew, skill);
            }
            boolean unk = false;
            mplew.write(unk);
            if (unk) {
                mplew.write(0); // structGuildSetting.nJoinSetting
                mplew.writeInt(0); // structGuildSetting.nReqLevel
            }
        }

        public static void GUILDDATA_SKILLENTRY_Decode(MaplePacketLittleEndianWriter mplew, MapleGuildSkill skill) {
            mplew.writeShort(skill.level); // nLevel
            mplew.writeLong(DateUtil.getFileTimestamp(skill.timestamp)); // dateExpire
            mplew.writeMapleAsciiString(skill.purchaser); // strBuyCharacterName
            mplew.writeMapleAsciiString(skill.activator); // strExtendCharacterName
        }

        public static byte[] showSearchGuilds(List<MapleGuild> guilds) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_GuildSearchResult.getValue());
            mplew.writeInt(guilds.size());
            for (MapleGuild guild : guilds) {
                mplew.writeInt(guild.getId());
                mplew.writeInt(guild.getLevel());
                mplew.writeMapleAsciiString(guild.getName());
                mplew.writeMapleAsciiString(guild.getMGC(guild.getLeaderId()).getName());
                mplew.writeInt(guild.getMembers().size());
                mplew.writeInt(guild.getAverageLevel());
            }

            return mplew.getPacket();
        }

        public static byte[] BBSThreadList(List<MapleBBSThread> bbs, int start) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
            mplew.write(0x6);
            if (bbs == null) {
                mplew.write(0);
                mplew.writeLong(0L);
                return mplew.getPacket();
            }
            int threadCount = bbs.size();
            MapleBBSThread notice = null;
            for (MapleBBSThread b : bbs) {
                if (b.isNotice()) {
                    notice = b;
                    break;
                }
            }
            mplew.write(notice == null ? 0 : 1);
            if (notice != null) {
                addThread(mplew, notice);
            }
            if (threadCount < start) {
                start = 0;
            }
            mplew.writeInt(threadCount);
            int pages = Math.min(10, threadCount - start);
            mplew.writeInt(pages);
            for (int i = 0; i < pages; i++) {
                addThread(mplew, bbs.get(start + i));
            }

            return mplew.getPacket();
        }

        private static void addThread(MaplePacketLittleEndianWriter mplew, MapleBBSThread rs) {
            mplew.writeInt(rs.localthreadID);
            mplew.writeInt(rs.ownerID);
            mplew.writeMapleAsciiString(rs.name);
            mplew.writeLong(DateUtil.getKoreanTimestamp(rs.timestamp));
            mplew.writeInt(rs.icon);
            mplew.writeInt(rs.getReplyCount());
        }

        public static byte[] showThread(MapleBBSThread thread) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
            mplew.write(7);
            mplew.writeInt(thread.localthreadID);
            mplew.writeInt(thread.ownerID);
            mplew.writeLong(DateUtil.getKoreanTimestamp(thread.timestamp));
            mplew.writeMapleAsciiString(thread.name);
            mplew.writeMapleAsciiString(thread.text);
            mplew.writeInt(thread.icon);
            mplew.writeInt(thread.getReplyCount());
            for (MapleBBSThread.MapleBBSReply reply : thread.replies.values()) {
                mplew.writeInt(reply.replyid);
                mplew.writeInt(reply.ownerID);
                mplew.writeLong(DateUtil.getKoreanTimestamp(reply.timestamp));
                mplew.writeMapleAsciiString(reply.content);
            }

            return mplew.getPacket();
        }
    }

    public static class InfoPacket {

        private static byte[]  OnMessage(MessageOpcode op, long[] values, String[] strs, Map<ExpMasks, Integer[]> expMap, Map<MapleTrait.MapleTraitType, Integer> traitMap) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_Message.getValue());
            mplew.write(op.getValue());
            switch (op) {
                case MS_DropPickUpMessage:
                    mplew.writeInt((int) values[0]); // Unknown Time
                    mplew.write((int) values[1]);
                    mplew.write((byte) values[2]);
                    if (values[2] != 4) {
                        switch ((int) values[2]) {
                            case -1:
                                break;
                            case 1:
                                mplew.write((int) values[3]);
                                mplew.writeInt((int) values[4]);
                                mplew.writeShort((int) values[5]);
                                break;
                            case 8:
                                mplew.writeInt((int) values[3]);
                                mplew.writeShort((int) values[4]);
                                break;
                            case 0:
                                mplew.writeInt((int) values[3]);
                                mplew.writeInt((int) values[4]);
                                break;
                            case 2:
                                mplew.writeInt((int) values[3]);
                                mplew.writeLong(values[4]);
                                break;
                            case -2:
                                break;
                            case -3:
                                break;
                            case -4:
                                break;
                            case -5:
                                break;
                            case 5:
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                case MS_QuestRecordMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.write((int) values[1]);
                    switch ((int) values[1]) {
                        case 1:
                            mplew.writeMapleAsciiString(strs[0]);
                            break;
                        case 2:
                            mplew.writeLong(values[2]);
                            break;
                        case 0:
                            mplew.write((int) values[2]);
                            break;
                    }
                    break;
                case MS_QuestRecordMessageAddValidCheck:
                    mplew.writeInt((int) values[0]);
                    mplew.write((int) values[1]);
                    mplew.write((int) values[2]);
                    switch ((int) values[2]) {
                        case 1:
                            mplew.writeMapleAsciiString(strs[0]);
                            break;
                        case 2:
                            mplew.writeLong(values[3]);
                            break;
                        case 0:
                            mplew.write((int) values[3]);
                            break;
                    }
                    break;
                case MS_QuestRecordExMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_WorldShareRecordMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_CashItemExpireMessage:
                    mplew.writeInt((int) values[0]);
                    break;
                case MS_IncEXPMessage:
                    mplew.write((int) values[0]);
                    mplew.writeLong(values[1]);
                    mplew.write((int) values[2]);
                    long dbCharFlag = 0;
                    if (expMap == null) {
                        expMap = new HashMap<>();
                    }
                    for (final Map.Entry<ExpMasks, Integer[]> flagEntry : expMap.entrySet()) {
                        dbCharFlag |= flagEntry.getKey().getValue();
                    }
                    mplew.writeLong(dbCharFlag);

                    if (expMap.containsKey(ExpMasks.SelectedMobBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.SelectedMobBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.PartyBonusPercentage)) {
                        mplew.write(expMap.get(ExpMasks.PartyBonusPercentage)[0].byteValue());
                    }
                    int nQuestBonusRate = 0;
                    if (values[2] > 0) {
                        mplew.write(nQuestBonusRate);
                    }
                    if (nQuestBonusRate > 0) {
                        mplew.write(0); // nQuestBonusRemainCount
                    }
                    if (expMap.containsKey(ExpMasks.WeddingBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.WeddingBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.PartyBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.PartyBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.ItemBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.ItemBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.PremiumIPBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.PremiumIPBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.RainbowWeekEventBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.RainbowWeekEventBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.BoomUpEventBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.BoomUpEventBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.PlusExpBuffBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.PlusExpBuffBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.PsdBonusExpRate)) {
                        mplew.writeInt(expMap.get(ExpMasks.PsdBonusExpRate)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.IndieBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.IndieBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.RelaxBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.RelaxBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.InstallItemBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.InstallItemBonusExp)[0]);
                    }
                    // if (flag.containsKey(ExpMasks.AswanWinnerBonusExp)) {
                    // mplew.writeInt(flag.get(ExpMasks.AswanWinnerBonusExp)[0]);
                    // }
                    if (expMap.containsKey(ExpMasks.ExpByIncExpR)) {
                        mplew.writeInt(expMap.get(ExpMasks.ExpByIncExpR)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.ValuePackBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.ValuePackBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.ExpByIncPQExpR)) {
                        mplew.writeInt(expMap.get(ExpMasks.ExpByIncPQExpR)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.BaseAddExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.BaseAddExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.BloodAllianceBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.BloodAllianceBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.FreezeHotEventBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.FreezeHotEventBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.RestFieldBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.RestFieldBonusExp)[0]); // nRestFieldBonusExp
                        mplew.writeInt(expMap.get(ExpMasks.RestFieldBonusExp)[1]); // nRestFieldExpRate
                    }
                    if (expMap.containsKey(ExpMasks.UserHPRateBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.UserHPRateBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.FieldValueBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.FieldValueBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.MobKillBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.MobKillBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.LiveEventBonusExp)) {
                        mplew.writeInt(expMap.get(ExpMasks.LiveEventBonusExp)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_1)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_1)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_2)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_2)[1]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_2)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_2)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_3)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_3)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_4)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_4)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_5)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_5)[0]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_4)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_4)[1]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_5)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_5)[1]);
                    }
                    if (expMap.containsKey(ExpMasks.Unk_6)) {
                        mplew.writeInt(expMap.get(ExpMasks.Unk_6)[1]);
                    }
                    break;
                case MS_IncNonCombatStatEXPMessage:
                    dbCharFlag = 0;
                    if (traitMap == null) {
                        traitMap = new HashMap<>();
                    }
                    for (final Map.Entry<MapleTrait.MapleTraitType, Integer> flagEntry : traitMap.entrySet()) {
                        dbCharFlag |= flagEntry.getKey().getStat().getValue();
                    }
                    mplew.writeLong(dbCharFlag);
                    if (traitMap.containsKey(MapleTrait.MapleTraitType.charisma)) {
                        mplew.writeInt(traitMap.get(MapleTrait.MapleTraitType.charisma));
                    }
                    if (traitMap.containsKey(MapleTrait.MapleTraitType.insight)) {
                        mplew.writeInt(traitMap.get(MapleTrait.MapleTraitType.insight));
                    }
                    if (traitMap.containsKey(MapleTrait.MapleTraitType.will)) {
                        mplew.writeInt(traitMap.get(MapleTrait.MapleTraitType.will));
                    }
                    if (traitMap.containsKey(MapleTrait.MapleTraitType.craft)) {
                        mplew.writeInt(traitMap.get(MapleTrait.MapleTraitType.craft));
                    }
                    if (traitMap.containsKey(MapleTrait.MapleTraitType.sense)) {
                        mplew.writeInt(traitMap.get(MapleTrait.MapleTraitType.sense));
                    }
                    if (traitMap.containsKey(MapleTrait.MapleTraitType.charm)) {
                        mplew.writeInt(traitMap.get(MapleTrait.MapleTraitType.charm));
                    }
                    break;
                case MS_LimitNonCombatStatEXPMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.writeInt((int) values[1]);
                    mplew.writeInt((int) values[2]);
                    break;
                case MS_RecipeExpireMessage:
                    mplew.writeLong(values[0]);
                    break;
                case MS_IncSPMessage:
                    mplew.writeShort((int) values[0]);
                    mplew.write((int) values[1]);
                    break;
                case MS_IncPOPMessage:
                    mplew.writeInt((int) values[0]);
                    break;
                case MS_IncMoneyMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.writeInt((int) values[1]);
                    if (values[1] == 24) {
                        mplew.writeMapleAsciiString(strs[0]);
                    }
                    break;
                case MS_IncPvPPointMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.writeInt((int) values[1]);
                    break;
                case MS_PvPItemUseMessage:
                    mplew.writeMapleAsciiString(strs[0]);
                    mplew.writeMapleAsciiString(strs[1]);
                    break;
                case MS_IncGPMessage:
                    mplew.writeInt((int) values[0]);
                    break;
                case MS_IncCommitmentMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.write((int) values[1]);
                    break;
                case MS_GiveBuffMessage:
                    mplew.writeInt((int) values[0]);
                    break;
                case MS_GeneralItemExpireMessage:
                    mplew.write((int) values[0]);
                    while (true) {
                        mplew.writeInt(0); // ItemID
                        break;
                    }
                    break;
                case MS_SystemMessage:
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_D:
                    mplew.writeInt((int) values[0]);
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_ItemProtectExpireMessage:
                    mplew.write((int) values[0]);
                    while (true) {
                        mplew.writeInt(0); // ItemID
                        break;
                    }
                    break;
                case MS_ItemExpireReplaceMessage:
                    mplew.write(strs.length);
                    for (String s : strs) {
                        mplew.writeMapleAsciiString(s);
                    }
                    break;
                case MS_ItemAbilityTimeLimitedExpireMessage:
                    mplew.write((int) values[0]);
                    while (true) {
                        mplew.writeInt(0); // ItemID
                        break;
                    }
                    break;
                case MS_SkillExpireMessage:
                    mplew.write(values.length);
                    for (long i : values) {
                        mplew.writeInt((int) i);
                    }
                    break;
                case MS_IncFatigueByRestMessage:
                    break;
                case MS_19:
                    break;
                case MS_WeddingPortalError:
                    mplew.write((int) values[0]);
                    break;
                case MS_PvPHardCoreExpMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.writeInt((int) values[1]);
                    break;
                case MS_NoticeAutoLineChanged:
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_EntryRecordMessage:
                    mplew.write((int) values[0]);
                    switch((int) values[0]) {
                        case 0:
                            mplew.writeShort((int) values[1]);
                            mplew.writeInt((int) values[2]);
                            mplew.writeInt((int) values[3]);
                            break;
                        case 1:
                            mplew.writeInt((int) values[1]);
                            break;
                        case 3:
                            break;
                        case 4:
                            break;
                        case 5:
                            break;
                        case 2:
                            break;
                        case 6:
                            mplew.writeInt((int) values[1]);
                            mplew.writeInt((int) values[2]);
                            mplew.writeInt((int) values[3]);
                            break;
                        default:
                            break;
                    }
                    break;
                case MS_EvolvingSystemMessage:
                    mplew.write((int) values[0]);
                    mplew.write((int) values[1]);
                    break;
                case MS_EvolvingSystemMessageWithName:
                    mplew.write((int) values[0]);
                    mplew.write((int) values[1]);
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_CoreInvenOperationMessage:
                    mplew.write((int) values[0]);
                    switch((int) values[0]) {
                        case 0x16:
                            mplew.writeInt((int) values[1]);
                            mplew.writeInt((int) values[2]);
                            break;
                        case 0x17:
                            break;
                        case 0x18:
                            break;
                        case 0x19:
                            mplew.writeInt((int) values[1]);
                            mplew.writeInt((int) values[2]);
                            break;
                        case 0x1A:
                            break;
                        case 0x1B:
                            break;
                        default:
                            break;
                    }
                    break;
                case MS_NxRecordMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_BlockedBehaviorTypeMessage:
                    mplew.writeInt((int) values[0]);
                    break;
                case MS_IncWPMessage:
                    mplew.writeInt((int) values[0]);
                    break;
                case MS_MaxWPMessage:
                    break;
                case MS_StylishKillMessage:
                    mplew.write((int) values[0]);
                    if (values[0] > 0) {
                        if (values[0] - 1 == 0) {
                            // DisplayComboKill
                            mplew.writeInt((int) values[1]); // nCount
                            mplew.writeInt((int) values[2]); // dwMobID
                            mplew.writeInt((int) values[3]);
                            mplew.writeInt((int) values[4]);
                        }
                    } else {
                        // DisplayMultiKill
                        mplew.writeLong(values[1]); // nBonus
                        mplew.writeInt((int) values[2]);
                        mplew.writeInt((int) values[3]); // nCount
                        mplew.writeInt((int) values[4]);
                    }
                    break;
                case MS_ExpiredCashItemResultMessage:
                    break;
                case MS_CollectionRecordMessage:
                    mplew.writeInt((int) values[0]);
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_RandomChanceMessage:
                    mplew.write((int) values[0]);
                    break;
                case MS_2B:
                    mplew.writeInt((int) values[0]);
                    break;
                case MS_2C:
                    mplew.writeInt((int) values[0]);
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_2D:
                    mplew.writeInt((int) values[0]);
                    break;
                case MS_2E:
                    mplew.writeInt(values.length);
                    for (long l : values) {
                        mplew.writeLong(l);
                    }
                    break;
                case MS_2F:
                    // 省略
                    break;
                case MS_30:
                    // 省略
                    break;
                case MS_31:
                    mplew.writeInt((int) values[0]);
                    mplew.writeInt((int) values[1]);
                    mplew.writeInt((int) values[2]);
                    mplew.writeLong(values[3]);
                    mplew.writeLong(values[4]);
                    mplew.writeLong(values[5]);
                    break;
                case MS_32:
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                case MS_33:
                    mplew.write((int) values[0]);
                    if (values[0] == 1) {
                        mplew.writeInt((int) values[1]);
                        mplew.writeShort((int) values[2]);
                    } else {
                        if (values[0] > 0) {
                            if (values[0] == 2) {
                                mplew.writeInt((int) values[1]);
                            }
                        } else {
                            mplew.writeInt((int) values[1]);
                            mplew.writeInt((int) values[2]);
                        }
                    }
                    break;
                case MS_34:
                    mplew.writeMapleAsciiString(strs[0]);
                    break;
                default:
                    break;
            }

            return mplew.getPacket();
        }

        public static byte[] showMesoGain(long gain, boolean inChat) {
            if (!inChat) {
                return OnMessage(MessageOpcode.MS_DropPickUpMessage, new long[] {0, 0, 1, 0, gain, 0}, null, null, null);
            } else {
                return OnMessage(MessageOpcode.MS_IncMoneyMessage, new long[] {gain, -1}, new String[] {""}, null, null);
            }
        }

        public static byte[] getShowInventoryStatus(int mode) {
            return OnMessage(MessageOpcode.MS_DropPickUpMessage, new long[] {0, 0, mode, 0, 0, 0}, null, null, null);
        }

        public static byte[] getShowItemGain(int itemId, short quantity) {
            return OnMessage(MessageOpcode.MS_DropPickUpMessage, new long[] {0, 0, 0, itemId, quantity}, null, null, null);
        }

        public static byte[] getShowItemGain(int itemId, short quantity, boolean inChat) {
            if (inChat) {
                Map<Integer, Integer> items = new HashMap<>();
                items.put(itemId, (int) quantity);
                return CField.EffectPacket.getShowItemGain(items);
            } else {
                return getShowItemGain(itemId, quantity);
            }
        }

        public static byte[] updateQuest(MapleQuestStatus quest) {
            long [] values = new long[] {quest.getQuest().getId(), quest.getStatus()};
            String[] strs = null;
            switch (quest.getStatus()) {
                case 0:
                    values = new long[] {quest.getQuest().getId(), quest.getStatus(), 0};
                    break;
                case 1:
                    strs = new String[] {quest.getCustomData() != null ? quest.getCustomData() : ""};
                    break;
                case 2:
                    values = new long[] {quest.getQuest().getId(), quest.getStatus(), DateUtil.getFileTimestamp(System.currentTimeMillis())};
                    break;
            }
            return OnMessage(MessageOpcode.MS_QuestRecordMessage, values, strs, null, null);
        }

        public static byte[] updateQuestMobKills(MapleQuestStatus status) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<?> i$ = status.getMobKills().values().iterator(); i$.hasNext();) {
                int kills = ((Integer) i$.next());
                sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
            }
            return OnMessage(MessageOpcode.MS_QuestRecordMessage, new long[] {status.getQuest().getId(), 1}, new String[] {sb.toString()}, null, null);
        }

        public static byte[] itemExpired(int itemid) {
            return OnMessage(MessageOpcode.MS_CashItemExpireMessage, new long[] {itemid}, null, null, null);
        }

        public static byte[] OnIncEXPMessage(long nIncEXP, boolean bOnQuest, boolean bIsLastHit, Map<ExpMasks, Integer[]> flag) {
            return OnMessage(MessageOpcode.MS_IncEXPMessage, new long[] {bIsLastHit ? 1 : 0, nIncEXP, bOnQuest ? 1 : 0}, null, flag, null);
        }

        public static byte[] getSPMsg(byte sp, short job) {
            return OnMessage(MessageOpcode.MS_IncSPMessage, new long[] {job, sp}, null, null, null);
        }

        public static byte[] getShowFameGain(int gain) {
            return OnMessage(MessageOpcode.MS_IncPOPMessage, new long[] {gain}, null, null, null);
        }

        public static byte[] getGPMsg(int gain) {
            return OnMessage(MessageOpcode.MS_IncGPMessage, new long[] {gain}, null, null, null);
        }

        public static byte[] getGPContribution(int gain) {
            return OnMessage(MessageOpcode.MS_IncCommitmentMessage, new long[] {gain, 0}, null, null, null);
        }

        public static byte[] getStatusMsg(int itemid) {
            return OnMessage(MessageOpcode.MS_GiveBuffMessage, new long[] {itemid}, null, null, null);
        }

        public static byte[] showInfo(String info) {
            return OnMessage(MessageOpcode.MS_SystemMessage, null, new String[] {info}, null, null);
        }

        public static byte[] updateInfoQuest(int quest, String data) {
            return OnMessage(MessageOpcode.MS_QuestRecordExMessage, new long[] {quest}, new String[] {data}, null, null);
        }

        public static byte[] showItemReplaceMessage(List<String> message) {
            return OnMessage(MessageOpcode.MS_ItemExpireReplaceMessage, null, (String[]) message.toArray(), null, null);
        }

        public static byte[] showTraitGain(MapleTrait.MapleTraitType trait, int amount) {
            Map<MapleTrait.MapleTraitType, Integer> traitMap = new HashMap<>();
            traitMap.put(trait, amount);
            return OnMessage(MessageOpcode.MS_IncNonCombatStatEXPMessage, null, null, null, traitMap);
        }

        public static byte[] getBPMsg(int amount) {
            return OnMessage(MessageOpcode.MS_PvPHardCoreExpMessage, new long[] {amount, 0}, null, null, null);
        }

        public static byte[] getShowCoreGain(int core, int quantity) {
            return OnMessage(MessageOpcode.MS_CoreInvenOperationMessage, new long[] {0x16, core, quantity}, null, null, null);
        }

        public static byte[] OnStylishKillMessage(int killType, long value, int value2) {
            long[] values = new long[] {killType};
            if (killType > 0) {
                if (killType - 1 == 0) {
                    values = new long[] {killType, value, value2, 0, value2};
                }
            } else {
                values = new long[] {killType, value, value2, value2, 0};
            }
            return OnMessage(MessageOpcode.MS_StylishKillMessage, values, null, null, null);
        }
    }

    public static class BuffPacket {

        public static byte[] giveSoulGauge(int count, int skillid) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.SoulMP);
            mplew.writeShort(count);
            mplew.writeInt(skillid);// skill
            mplew.writeInt(0);
            mplew.writeInt(1000);
            mplew.writeInt(skillid);// soulskill
            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.writeLong(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] cancelSoulGauge() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.SoulMP);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] giveSoulEffect(int skillid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.FullSoulMP);
            mplew.writeShort(12000); // time
            mplew.writeInt(skillid);
            mplew.writeInt(640000);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.writeLong(0);
            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] giveForeignSoulEffect(int cid, int skillid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserTemporaryStatSet.getValue());
            mplew.writeInt(cid);
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.FullSoulMP);
            mplew.writeInt(skillid);
            mplew.writeLong(0x60000000000L);
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeInt(0);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] cancelSoulEffect() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.FullSoulMP);

            return mplew.getPacket();
        }

        public static byte[] cancelForeignSoulEffect(int cid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserTemporaryStatReset.getValue());
            mplew.writeInt(cid);
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.FullSoulMP);
            mplew.write(1);

            return mplew.getPacket();
        }

        public static byte[] giveDice(int buffid, int skillid, int duration, Map<MapleBuffStat, Integer> statups) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeBuffMask(mplew, statups);

            mplew.writeShort(Math.max(buffid / 100, Math.max(buffid / 10, buffid % 10))); // 1-6

            mplew.writeInt(skillid); // skillid
            mplew.writeInt(duration);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeShort(0);
            mplew.writeInt(GameConstants.getDiceStat(buffid, 3)); // HP
            mplew.writeInt(GameConstants.getDiceStat(buffid, 3)); // MP
            mplew.writeInt(GameConstants.getDiceStat(buffid, 4)); // 爆擊
            mplew.writeZeroBytes(20); // idk
            mplew.writeInt(GameConstants.getDiceStat(buffid, 2)); // 物理防禦
            mplew.writeZeroBytes(12); // idk
            mplew.writeInt(GameConstants.getDiceStat(buffid, 5)); // 傷害增加
            mplew.writeZeroBytes(16); // idk
            mplew.writeInt(GameConstants.getDiceStat(buffid, 6)); // 增加經驗獲取輛
            mplew.writeZeroBytes(20);
            mplew.writeInt(1000);// new 143
            mplew.write(1);
            mplew.writeInt(0);// new143
            return mplew.getPacket();
        }

        public static byte[] giveHoming(int skillid, int mobid, int x) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.GuidedBullet);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeInt(1);
            mplew.writeLong(skillid);
            mplew.write(0);
            mplew.writeLong(mobid);
            mplew.writeShort(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);// v112
            return mplew.getPacket();
        }

        public static byte[] giveArcane(int value, int skillid, int duration) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.ArcaneAim);
            mplew.writeShort(value);
            mplew.writeInt(skillid);
            mplew.writeInt(duration);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.writeShort(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeZeroBytes(9);
            return mplew.getPacket();
        }

        public static byte[] giveEnergyChargeTest(int bar, int bufflength) {
            return giveEnergyChargeTest(-1, bar, bufflength);
        }

        public static byte[] giveEnergyChargeTest(int cid, int bar, int bufflength) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            if (cid == -1) {
                mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            } else {
                mplew.writeShort(SendPacketOpcode.LP_UserTemporaryStatSet.getValue());
                mplew.writeInt(cid);
            }
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.EnergyCharged);
            mplew.writeZeroBytes(5);
            mplew.writeInt(0); // 技能ID
            mplew.writeInt(Math.min(bar, 10000));
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(0);
            // mplew.writeInt(bar >= 10000 ? bufflength : 0);
            mplew.writeLong(0);
            mplew.writeShort(6);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] giveBuff(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups,
                MapleStatEffect effect, MapleCharacter chr) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.SecondaryStat_DecodeForLocal(mplew, buffid, bufflength, statups, effect, chr);

            mplew.writeShort(effect != null ? effect.getEffectDelay() : 0); // effectDelay
            mplew.write(0);
            mplew.write(0); // bFirstSet
            mplew.write(1); // bTemporaryOnShow || 1101013

            for (Map.Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
                if (MapleBuffStat.isMovementAffectingStat(stat.getKey())) {
                    if (buffid == 131001004) {
                        mplew.write(0x21);
                    } else {
                        mplew.write(buffid == 32001016 ? 8 : 0);
                    }
                }
            }

            mplew.writeInt(0);

            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static void IsDefaultBuff(MaplePacketLittleEndianWriter mplew, Map<MapleBuffStat, Integer> statups,
                MapleStatEffect effect, MapleCharacter chr, int buffid, int bufflength) {
            if (statups.containsKey(MapleBuffStat.EnergyCharged)) {
                mplew.writeInt(statups.get(MapleBuffStat.EnergyCharged)); // Value
                mplew.writeInt(buffid); // SkillID
                mplew.write(1);
                mplew.writeInt(0);// 1
            }

            if (statups.containsKey(MapleBuffStat.DashSpeed)) {
                mplew.writeInt(statups.get(MapleBuffStat.EnergyCharged)); // Value
                mplew.writeInt(buffid); // SkillID
                mplew.write(1);
                mplew.writeInt(0);// 2
                mplew.writeShort(0);
            }

            if (statups.containsKey(MapleBuffStat.DashJump)) {
                mplew.writeInt(statups.get(MapleBuffStat.EnergyCharged)); // Value
                mplew.writeInt(buffid); // SkillID
                mplew.write(1);
                mplew.writeInt(0);// 3
                mplew.writeShort(0);
            }

            if (statups.containsKey(MapleBuffStat.RideVehicle)) {
                mplew.writeInt(statups.get(MapleBuffStat.RideVehicle)); // Value
                mplew.writeInt(buffid); // SkillID
                mplew.write(1);
                mplew.writeInt(0);// 4
            }

            if (statups.containsKey(MapleBuffStat.PartyBooster)) {
                mplew.writeInt(statups.get(MapleBuffStat.PartyBooster)); // Value
                mplew.writeInt(buffid); // SkillID
                mplew.write(1);
                mplew.writeInt(1);// 5
                mplew.write(1);
                mplew.writeInt(1);
                mplew.writeShort(0x159); // Value
            }

            if (statups.containsKey(MapleBuffStat.GuidedBullet)) {
                mplew.writeInt(statups.get(MapleBuffStat.GuidedBullet)); // Value
                mplew.writeInt(buffid); // SkillID
                mplew.write(0);
                mplew.writeInt(0);// 6
                mplew.writeInt(0); // 怪物OID
                mplew.writeInt(0);
            }

            if (statups.containsKey(MapleBuffStat.Undead)) {
                mplew.writeInt(statups.get(MapleBuffStat.Undead)); // Value
                mplew.writeInt(buffid); // SkillID
                mplew.write(1);
                mplew.writeInt(0);// 7
                mplew.writeShort(0);
            }

            if (statups.containsKey(MapleBuffStat.RideVehicleExpire)) {
                mplew.writeInt(statups.get(MapleBuffStat.RideVehicleExpire)); // Value
                mplew.writeInt(buffid); // SkillID
                mplew.write(1);
                mplew.writeInt(0);// 8
            }
        }

        public static byte[] giveDebuff(MapleDisease statups, int x, int skillid, int level, int duration) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, statups);
            mplew.writeShort(x);
            mplew.writeShort(skillid);
            mplew.writeShort(level);
            mplew.writeInt(duration);
            mplew.writeShort(0);
            mplew.writeShort(0);
            // mplew.write(1);
            mplew.write(0);
            // mplew.write(1);
            mplew.writeZeroBytes(30);
            // System.out.println(HexTool.toString(mplew.getPacket()));
            return mplew.getPacket();
        }

        public static byte[] cancelBuff(List<MapleBuffStat> statups, MapleCharacter chr) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());

            PacketHelper.writeMask(mplew, statups);

            for (MapleBuffStat stat : statups) {
                if (stat.isIndie() && !constants.GameConstants.isSpecialIndieBuff(stat)) {
                    chr.getTempstats().encodeIndieTempStat(mplew, stat);
                    // System.out.println("堆疊Buff::" + stat.name()); // 查看排列順序用
                    // IndieTempStat_Decode(mplew, chr.getBuffStatValueHolders(stat), 0);
                }
            }

            for (MapleBuffStat z : statups) {
                if (MapleBuffStat.isMovementAffectingStat(z)) {
                    mplew.write(0);
                }
                if (z == MapleBuffStat.PoseType) {
                    mplew.write(0);
                }
                if (z == MapleBuffStat.RideVehicle) {
                    mplew.write(0); // boolean
                }
            }

            return mplew.getPacket();
        }

        public static byte[] cancelDebuff(MapleDisease mask) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());

            PacketHelper.writeSingleMask(mplew, mask);
            mplew.write(3);
            mplew.write(1);
            mplew.writeLong(0);
            mplew.write(0);// v112
            return mplew.getPacket();
        }

        public static byte[] getQuiverKartrige(int count, int mode, List<Pair<MapleBuffStat, Integer>> statups) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            switch (mode) {
            case 1:
                count = 1010 + count * 10000;
                break;
            case 2:
                count = 100010 + count * 100;
                break;
            case 3:
                count = 101000 + count;
                break;
            }
            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeBuffMask(mplew, statups);
            mplew.writeInt(count);
            mplew.writeInt(3101009);
            mplew.writeInt(DateUtil.getTime(System.currentTimeMillis()));
            mplew.writeZeroBytes(5);
            mplew.writeInt(mode);
            mplew.writeLong(0);
            mplew.write(1);
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] getQuiverKartrigecount(int count, int mode) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            switch (mode) {
            case 1:
                count = 10000 * count;
                break;
            case 2:
                count = 130040 + count * 100;
                break;
            case 3:
                count = 160000 + count;
                break;
            }
            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.QuiverCatridge);
            mplew.writeInt(count);
            mplew.writeInt(3101009);
            mplew.writeInt(DateUtil.getTime(System.currentTimeMillis()));
            mplew.writeZeroBytes(5);
            mplew.writeInt(mode);
            mplew.writeLong(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] getPressureVoid(int level) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.KeyDownAreaMoving);
            mplew.writeShort(level);
            mplew.writeInt(27101202);
            mplew.writeInt(0);
            mplew.writeZeroBytes(18);

            return mplew.getPacket();
        }

        // public static byte[] get记录(MapleCharacter chr) {
        // MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //
        // mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        // PacketHelper.writeSingleMask(mplew, MapleBuffStat.光暗转换);
        // mplew.writeShort(2);
        // mplew.writeInt(20040219);
        // mplew.writeInt(20000);
        // mplew.writeZeroBytes(5);
        // mplew.writeInt(20040218);
        // mplew.writeInt(0);
        // mplew.writeInt(20040217);
        // mplew.writeInt(0);
        // mplew.writeInt(chr.getDarkTotal());
        // mplew.writeInt(chr.getLightTotal());
        // mplew.writeInt(chr.getDarkType()); //个数
        // mplew.writeInt(chr.getLightType()); //个数
        // mplew.writeLong(DateUtil.getTime(System.currentTimeMillis()));//time
        // mplew.writeInt(1000);
        // mplew.write(1);
        // mplew.writeInt(0);
        // return mplew.getPacket();
        // }
        public static byte[] getDoubleDefense(int buffid, List<Pair<MapleBuffStat, Integer>> statups,
                MapleCharacter chr, int x) {// 双重防御
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeBuffMask(mplew, statups);

            for (Pair<MapleBuffStat, Integer> stat : statups) {
                mplew.writeShort(stat.getRight());
                mplew.writeInt(buffid);
                mplew.writeInt(0);
            }
            mplew.writeZeroBytes(5);
            mplew.write(x);
            mplew.writeLong(0);
            mplew.write(1);
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        // public static byte[] getElementsRush(int value, int skill) {//元素衝擊
        // MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        // PacketHelper.writeSingleMask(mplew, MapleBuffStat.元素衝擊);
        // mplew.writeShort(2 * value);
        // mplew.writeInt(skill);
        // mplew.writeInt(0);
        // mplew.writeInt(0);
        // mplew.write(0x0D);
        // mplew.write(value);
        // mplew.writeShort(value * 8);
        // mplew.write(2 * value);
        // mplew.write(2 * value);
        // mplew.writeZeroBytes(13);
        // return mplew.getPacket();
        // }
        // public static byte[] getRebirthContract(int value) {//轉生
        // MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        // PacketHelper.writeSingleMask(mplew, MapleBuffStat.重生契约);
        // mplew.writeShort(1);
        // mplew.writeInt(1320019);
        // mplew.writeInt(0);
        // mplew.writeInt(0);
        // mplew.write(0x08);
        // mplew.write(value);
        // mplew.writeZeroBytes(16);
        // return mplew.getPacket();
        // }
        // public static byte[] getInjuryReplacement(int value, int time) {//反向傷害
        // MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        // PacketHelper.writeSingleMask(mplew, MapleBuffStat.反向傷害);
        // mplew.writeInt(value);
        // mplew.writeInt(3210013);
        // mplew.writeInt(time);
        // mplew.writeZeroBytes(18);
        // return mplew.getPacket();
        // }
        public static byte[] getUltimateInfinite(int value, int time, int skill) {// 魔力無限
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.Infinity);
            mplew.writeShort(value);
            mplew.writeInt(skill);
            mplew.writeInt(time);
            mplew.writeZeroBytes(18);
            return mplew.getPacket();
        }

        public static byte[] giveForeignBuff(final MapleCharacter chr, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserTemporaryStatSet.getValue());
            mplew.writeInt(chr.getId());
            chr.getTempstats().EncodeForRemote(mplew);

            mplew.writeShort(0);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] giveForeignDebuff(int cid, final MapleDisease statups, int skillid, int level, int x) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserTemporaryStatSet.getValue());
            mplew.writeInt(cid);

            PacketHelper.writeSingleMask(mplew, statups);
            if (skillid == 125) {
                mplew.writeShort(0);
                mplew.write(0);
            }
            mplew.writeShort(x);
            mplew.writeShort(skillid);
            mplew.writeShort(level);
            mplew.writeShort(0); // same as give_buff
            mplew.writeShort(0); // Delay
            mplew.write(1);
            mplew.write(1);
            mplew.write(0);// v112
            mplew.writeZeroBytes(20);
            return mplew.getPacket();
        }

        public static byte[] onResetTemporaryStat(int cid, List<MapleBuffStat> statups) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserTemporaryStatReset.getValue());
            mplew.writeInt(cid);
            PacketHelper.writeMask(mplew, statups);// 36 bytes
            // mplew.write(3);
            mplew.write(1);
            // mplew.write(0);//v112

            return mplew.getPacket();
        }

        public static byte[] cancelForeignDebuff(int cid, MapleDisease mask) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserTemporaryStatReset.getValue());
            mplew.writeInt(cid);

            PacketHelper.writeSingleMask(mplew, mask);// 48 bytes
            // mplew.write(3);
            mplew.write(1);
            // mplew.write(0);//v112
            return mplew.getPacket();
        }

    }

    public static class InventoryPacket {

        public static byte[] modifyInventory(boolean updateTick, final ModifyInventory mod) {
            return modifyInventory(updateTick, Collections.singletonList(mod));
        }

        public static byte[] modifyInventory(boolean updateTick, final List<ModifyInventory> mods) {
            final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_InventoryOperation.getValue());
            // CWvsContext::SetExclRequestSent
            mplew.write(updateTick ? 1 : 0);
            // nCount
            mplew.write(mods.size());
            // bNotRemoveAddInfo
            mplew.write(0); // Unk
            int nBeforeCount = 0;
            boolean bSN = false;
            for (ModifyInventory mod : mods) {
                // nPos
                mplew.write(mod.getMode());
                // nTI
                mplew.write(mod.getInventoryType());
                // nCurItemID
                mplew.writeShort(mod.getPosition());
                switch (mod.getMode()) {
                case ModifyInventory.Types.ADD: {// add item
                    PacketHelper.GW_ItemSlotBase_Decode(mplew, mod.getItem());
                    break;
                }
                case ModifyInventory.Types.UPDATE: {// update quantity
                    // nBagMaxSlot
                    mplew.writeShort(mod.getQuantity());
                    break;
                }
                case ModifyInventory.Types.MOVE: {// move
                    // nTI
                    mplew.writeShort(mod.getNewPosition());
                    if (mod.getInventoryType() == 1 && (mod.getPosition() < 0 || mod.getNewPosition() < 0)) {
                        nBeforeCount = 1;
                    }
                    break;
                }
                case ModifyInventory.Types.REMOVE: {// remove
                    if (mod.getInventoryType() == 1 && mod.getPosition() < 0) {
                        nBeforeCount = 1;
                    }
                    break;
                }
                case 4: {
                    mplew.writeLong(0);
                    break;
                }
                case ModifyInventory.Types.MOVE_TO_BAG: {
                    // nNumber
                    mplew.writeInt(mod.getNewPosition());
                    break;
                }
                case ModifyInventory.Types.UPDATE_IN_BAG: {
                    int nCurItemID = mod.getPosition();
                    int v109;
                    if (mod.getInventoryType() == 2 || mod.getInventoryType() == 3) {
                        v109 = 212;
                    } else if (mod.getInventoryType() == 4) {
                        v109 = 712;
                    } else {
                        v109 = 100;
                    }
                    if (mod.getQuantity() > 2 // nBagMaxSlot
                            || nCurItemID < 101 || nCurItemID > v109) {
                        break;
                    }
                    // nNumber
                    mplew.writeShort(mod.getNewPosition());
                    break;
                }
                case ModifyInventory.Types.REMOVE_IN_BAG: {
                    break;
                }
                case ModifyInventory.Types.MOVE_IN_BAG: {
                    // nBagPos
                    mplew.writeShort(mod.getNewPosition());
                }
                case ModifyInventory.Types.ADD_IN_BAG: {
                    PacketHelper.GW_ItemSlotBase_Decode(mplew, mod.getItem());
                    break;
                }
                case 10: {
                    break;
                }
                default: {
                    break;
                }
                }
                mplew.write(false);
                mod.clear();
            }
            if (nBeforeCount > 0) {
                mplew.write(bSN);
                // CUserLocal::SetSecondaryStatChangedPoint(bSN);
            }

            return mplew.getPacket();
        }

        public static byte[] updateInventoryFull() {
            return modifyInventory(false, new ArrayList<>());
        }

        public static byte[] getInventoryFull() {
            return modifyInventory(true, new ArrayList<>());
        }

        public static byte[] addInventorySlot(Item item) {
            return addInventorySlot(item, false);
        }

        public static byte[] addInventorySlot(Item item, boolean fromDrop) {
            return modifyInventory(fromDrop,
                    new ModifyInventory(!GameConstants.isInBag(item.getPosition(),
                            GameConstants.getInventoryType(item.getItemId()).getType()) ? ModifyInventory.Types.ADD
                                    : ModifyInventory.Types.ADD_IN_BAG,
                            item));
        }

        public static byte[] updateInventorySlot(Item item, boolean fromDrop) {
            return modifyInventory(fromDrop,
                    new ModifyInventory(!GameConstants.isInBag(item.getPosition(),
                            GameConstants.getInventoryType(item.getItemId()).getType()) ? ModifyInventory.Types.UPDATE
                                    : ModifyInventory.Types.UPDATE_IN_BAG,
                            item));
        }

        public static byte[] clearInventoryItem(MapleInventoryType type, short slot, boolean fromDrop) {
            return modifyInventory(fromDrop,
                    new ModifyInventory(
                            !GameConstants.isInBag(slot, type.getType()) ? ModifyInventory.Types.REMOVE
                                    : ModifyInventory.Types.REMOVE_IN_BAG,
                            null, slot, null, (short) 0, type.getType()));
        }

        public static byte[] updateSpecialItemUse(Item item) {
            List<ModifyInventory> lMods = new ArrayList<>();
            lMods.add(new ModifyInventory(
                    !GameConstants.isInBag(item.getPosition(),
                            GameConstants.getInventoryType(item.getItemId()).getType()) ? ModifyInventory.Types.REMOVE
                                    : ModifyInventory.Types.REMOVE_IN_BAG,
                    null, item.getPosition(), null, (short) 0,
                    GameConstants.getInventoryType(item.getItemId()).getType()));
            lMods.add(new ModifyInventory(!GameConstants.isInBag(item.getPosition(),
                    GameConstants.getInventoryType(item.getItemId()).getType()) ? ModifyInventory.Types.ADD
                            : ModifyInventory.Types.ADD_IN_BAG,
                    item));
            return modifyInventory(false, lMods);
        }

        public static byte[] updateSpecialItemUse_(Item item) {
            return modifyInventory(false,
                    new ModifyInventory(!GameConstants.isInBag(item.getPosition(),
                            GameConstants.getInventoryType(item.getItemId()).getType()) ? ModifyInventory.Types.ADD
                                    : ModifyInventory.Types.ADD_IN_BAG,
                            item));
        }

        public static byte[] updateEquippedItem(Equip eq) {
            return modifyInventory(false,
                    new ModifyInventory(!GameConstants.isInBag(eq.getPosition(),
                            GameConstants.getInventoryType(eq.getItemId()).getType()) ? ModifyInventory.Types.ADD
                                    : ModifyInventory.Types.ADD_IN_BAG,
                            eq));
        }

        public static byte[] scrolledItem(Item scroll, Item item, boolean destroyed) {
            List<ModifyInventory> lMods = new ArrayList<>();
            lMods.add(new ModifyInventory(
                    scroll.getQuantity() > 0 ? ModifyInventory.Types.UPDATE : ModifyInventory.Types.REMOVE, scroll));
            lMods.add(new ModifyInventory(ModifyInventory.Types.REMOVE, item));
            if (!destroyed) {
                lMods.add(new ModifyInventory(ModifyInventory.Types.ADD, item));
            }
            // bSN = true
            return modifyInventory(true, lMods);
        }

        public static byte[] moveAndUpgradeItem(Item item, short oldpos) {
            List<ModifyInventory> lMods = new ArrayList<>();
            lMods.add(new ModifyInventory(!GameConstants.isInBag(item.getPosition(),
                    GameConstants.getInventoryType(item.getItemId()).getType()) ? ModifyInventory.Types.REMOVE
                            : ModifyInventory.Types.REMOVE_IN_BAG,
                    item, oldpos));
            lMods.add(new ModifyInventory(ModifyInventory.Types.ADD, item, oldpos));
            lMods.add(new ModifyInventory(ModifyInventory.Types.MOVE, item, oldpos));
            return modifyInventory(true, lMods);
        }

        public static byte[] dropInventoryItem(MapleInventoryType type, short src) {
            return modifyInventory(true,
                    new ModifyInventory(!GameConstants.isInBag(src, type.getType()) ? ModifyInventory.Types.REMOVE
                            : ModifyInventory.Types.REMOVE_IN_BAG, null, src, null, (short) 0, type.getType()));
        }

        public static byte[] dropInventoryItemUpdate(Item item) {
            return modifyInventory(true,
                    new ModifyInventory(!GameConstants.isInBag(item.getPosition(),
                            GameConstants.getInventoryType(item.getItemId()).getType()) ? ModifyInventory.Types.UPDATE
                                    : ModifyInventory.Types.UPDATE_IN_BAG,
                            item));
        }

        public static byte[] getSlotUpdate(byte invType, short newSlots) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_InventoryGrow.getValue());
            mplew.write(invType);
            mplew.write(newSlots);

            return mplew.getPacket();
        }

        public static byte[] getShowInventoryFull() {
            return CWvsContext.InfoPacket.getShowInventoryStatus(255);
        }

        public static byte[] showItemUnavailable() {
            return CWvsContext.InfoPacket.getShowInventoryStatus(254);
        }
    }

    public static byte[] updateHyperSp(int mode, int remainSp) {
        return updateSpecialStat("hyper", 0x1C, mode, remainSp);
    }

    public static byte[] updateSpecialStat(String stat, int array, int mode, int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ResultInstanceTable.getValue());
        mplew.writeMapleAsciiString(stat);
        mplew.writeInt(array);
        mplew.writeInt(mode);
        mplew.write(1);
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] updateSpecialStat(String stat, int array, int mode, boolean is, int amount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ResultInstanceTable.getValue());
        mplew.writeMapleAsciiString(stat);
        mplew.writeInt(array);
        mplew.writeInt(mode);
        mplew.write(is ? 1 : 0);
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] updateMaplePoint(int maplepoints) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetMaplePoint.getValue());
        mplew.writeInt(maplepoints);

        return mplew.getPacket();
    }

    public static byte[] onEventNameTag(int[] titles) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_EventNameTagInfo.getValue());
        for (int i = 0; i < 5; i++) {
            mplew.writeMapleAsciiString("");
            if (titles.length < i + 1) {
                mplew.write(-1);
            } else {
                mplew.write(titles[i]);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] getRoyaCouponInfo() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CashShopPreviewInfo.getValue());

        CashItemFactory cif = CashItemFactory.getInstance();
        Map<Integer, List<Pair<Integer, Integer>>> unkCoupon = cif.getUnkCoupon();
        mplew.write(unkCoupon.size());
        for (Map.Entry<Integer, List<Pair<Integer, Integer>>> unk : unkCoupon.entrySet()) {
            mplew.write(unk.getValue().size() > 0);
            if (unk.getValue().size() > 0) {
                mplew.writeInt(unk.getKey());
                mplew.write(0);
                mplew.writeShort(unk.getValue().size());
                for (Pair<Integer, Integer> couponPair : unk.getValue()) {
                    mplew.writeInt(couponPair.getLeft());
                    mplew.writeInt(couponPair.getRight());
                }
            }
        }

        Map<Integer, List<List<Pair<Integer, Integer>>>> unkCoupon2 = cif.getUnkCoupon2();
        mplew.write(unkCoupon2.size());
        for (Map.Entry<Integer, List<List<Pair<Integer, Integer>>>> unk : unkCoupon2.entrySet()) {
            mplew.write(unk.getValue().size() > 0);
            if (unk.getValue().size() > 0) {
                mplew.writeInt(unk.getKey());
                mplew.writeShort(unk.getValue().size());
                for (List<Pair<Integer, Integer>> couponPairList : unk.getValue()) {
                    mplew.write(couponPairList.size() > 0);
                    if (couponPairList.size() > 0) {
                        mplew.writeShort(couponPairList.size());
                        for (Pair<Integer, Integer> couponPair : couponPairList) {
                            mplew.writeInt(couponPair.getLeft());
                            mplew.writeInt(couponPair.getRight());
                        }
                    }
                }
            }
        }

        Map<Integer, RoyaCoupon> royaCoupon = cif.getRoyaCoupon();
        mplew.writeInt(royaCoupon.size());
        List<RoyaCoupon.RoyaInfo> styles;
        for (Map.Entry<Integer, RoyaCoupon> royaEntry : royaCoupon.entrySet()) {
            mplew.writeInt(royaEntry.getKey());

            styles = royaEntry.getValue().getMaleStyles(true);
            mplew.writeInt(styles.size());
            for (RoyaCoupon.RoyaInfo info : styles) {
                mplew.writeInt(info.styleId);
            }
            styles = royaEntry.getValue().getFemaleStyles(true);
            mplew.writeInt(styles.size());
            for (RoyaCoupon.RoyaInfo info : styles) {
                mplew.writeInt(info.styleId);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] updatePendantSlot(long time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_PendantSlotIncResult.getValue());
        mplew.writeLong(time);

        return mplew.getPacket();
    }

    public static byte[] magicWheel(int type, List<Integer> items, String data, int endSlot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAGIC_WHEEL.getValue());
        mplew.write(type);
        switch (type) {
        case 3:
            mplew.write(items.size());
            for (int item : items) {
                mplew.writeInt(item);
            }
            mplew.writeMapleAsciiString(data); // nexon encrypt the item and then send the string
            mplew.write(endSlot);
            break;
        case 5:
            // <Character Name> got <Item Name>.
            break;
        case 6:
            // You don't have a Magic Gachapon Wheel in your Inventory.
            break;
        case 7:
            // You don't have any Inventory Space.\r\n You must have 2 or more slots
            // available\r\n in each of your tabs.
            break;
        case 8:
            // Please try this again later.
            break;
        case 9:
            // Failed to delete Magic Gachapon Wheel item.
            break;
        case 0xA:
            // Failed to receive Magic Gachapon Wheel item.
            break;
        case 0xB:
            // You cannot move while Magic Wheel window is open.
            break;
        }

        return mplew.getPacket();
    }

    public static class Reward {

        public static byte[] receiveReward(int id, byte mode, int quantity) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.REWARD.getValue());
            mplew.write(mode); // mode
            switch (mode) { // mode
            case 0x0A:
                mplew.writeInt(0);
                break;
            case 0x0B:
                mplew.writeInt(id);
                mplew.writeInt(quantity); // quantity
                // Popup: You have received the Maple Points.\r\n( %d maple point )
                break;
            case 0x0C:
                mplew.writeInt(id);
                // Popup You have received the Game item.
                break;
            case 0x0E:
                mplew.writeInt(id);
                mplew.writeInt(quantity); // quantity
                // Popup: You have received the Mesos.\r\n( %d meso )
                break;
            case 0x0F:
                mplew.writeInt(id);
                mplew.writeInt(quantity); // quantity
                // Popup: You have received the Exp.\r\n( %d exp )
                break;
            case 0x14:
                // Popup: Failed to receive the Maple Points.
                break;
            case 0x15:
                mplew.write(0);
                // Popup: Failed to receive the Game Item.
                break;
            case 0x16:
                mplew.write(0);
                // Popup: Failed to receive the Game Item.
                break;
            case 0x17:
                // Popup: Failed to receive the Mesos.
                break;
            case 0x18:
                // Popup: Failed to receive the Exp.
                break;
            case 0x21:
                mplew.write(0); // 66
                // No inventory space
                break;
            }

            return mplew.getPacket();
        }

        public static byte[] updateReward(int id, byte mode, List<MapleReward> rewards, int option) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.REWARD.getValue());
            mplew.write(mode);
            switch (mode) {
            case 0x09:
                mplew.writeInt(rewards.size());
                if (rewards.size() > 0) {
                    for (MapleReward reward : rewards) {
                        boolean empty = reward.getId() < 1;
                        mplew.writeInt(empty ? 0 : reward.getId()); // 0 = blank 1+ = gift
                        if (!empty) {
                            if ((option & 1) != 0) {
                                mplew.writeLong(reward.getReceiveDate());
                                mplew.writeLong(reward.getExpireDate());
                            }
                            if ((option & 2) != 0) { // nexon do here a3 & 2 when a3 is 9
                                mplew.writeInt(0);
                                mplew.writeInt(0);
                                mplew.writeInt(0);
                                mplew.writeInt(0);
                                mplew.writeInt(0);
                                mplew.writeInt(0);
                                mplew.writeMapleAsciiString("");
                                mplew.writeMapleAsciiString("");
                                mplew.writeMapleAsciiString("");
                            }
                            mplew.writeInt(reward.getType()); // 3 = 楓葉點數 4 = 楓幣 5 = 經驗值
                            mplew.writeInt(reward.getItem());
                            mplew.writeInt(/* itemQ */reward.getItem() > 0 ? 1 : 0); // item quantity (?)
                            mplew.writeInt(0);
                            mplew.writeLong(0L);
                            mplew.writeInt(0);
                            mplew.writeInt(reward.getMaplePoints());
                            mplew.writeInt(reward.getMeso());
                            mplew.writeInt(reward.getExp());
                            mplew.writeInt(0);
                            mplew.writeInt(0);
                            mplew.writeMapleAsciiString("");
                            mplew.writeMapleAsciiString("");
                            mplew.writeMapleAsciiString("");
                            if ((option & 4) != 0) {
                                mplew.writeMapleAsciiString("");
                            }
                            if ((option & 8) != 0) {
                                mplew.writeMapleAsciiString(reward.getDesc());
                            }
                            mplew.writeInt(0);
                            mplew.writeInt(0);
                            mplew.writeInt(0);

                            // sub_412737
                            while (true) {
                                byte v4 = 0;
                                mplew.write(v4);
                                if (v4 >= 0) {
                                    break;
                                }
                            }

                            int result = 0;
                            if (result > 0) {
                                int a3 = 0;
                                do {
                                    mplew.writeMapleAsciiString("");
                                    a3++;

                                    // sub_412737
                                    while (true) {
                                        byte v4 = 0;
                                        mplew.write(v4);
                                        if (v4 >= 0) {
                                            break;
                                        }
                                    }

                                } while (a3 < result);
                            }
                        }
                    }
                }
                break;
            case 0x0B: // 獲得楓點。\r\n(%d 楓點)
                mplew.writeInt(id);
                mplew.writeInt(0); // 楓葉點數數量
                break;
            case 0x0C: // 獲得此道具。
                mplew.writeInt(id);
                break;
            case 0x0E: // 獲得楓幣。\r\n(%d 楓幣)
                mplew.writeInt(id);
                mplew.writeInt(0); // 楓幣數量
                break;
            case 0x0F: // 獲得經驗值。\r\n(%d 經驗值)
                mplew.writeInt(id);
                mplew.writeInt(0); // 經驗值數量
                break;
            case 0x14: // 楓點領取失敗。
                break;
            case 0x15: // 道具領取失敗。
                mplew.write(0);
                break;
            case 0x16: // 現金道具領取失敗。
                mplew.write(0);
                break;
            case 0x17: // 楓幣領取失敗。
                break;
            case 0x18: // 經驗值領取失敗。
                break;
            case 0x21: // 獎勵領取失敗。請再試一次。
                mplew.write(0);
                break;
            }

            return mplew.getPacket();
        }
    }

    public static class Enchant {

        public static byte[] getEnchantList(MapleClient c, Equip item, ArrayList<EchantScroll> scrolls,
                boolean feverTime) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EquipmentEnchantDisplay.getValue());
            mplew.write(0x32);
            mplew.write(feverTime ? 1 : 0);
            mplew.write(scrolls.size());
            for (EchantScroll scroll : scrolls) {
                mplew.writeInt(scroll.getViewType()); // 控制捲軸外觀
                mplew.writeMapleAsciiString(scroll.getName()); // 捲軸名稱
                mplew.writeInt(scroll.getViewType() > 3 ? scroll.getViewType() - 3 : 0); // 捲軸說明類型
                mplew.writeInt(0);
                mplew.writeInt(scroll.getMask());
                if (scroll.getMask() > 0) {
                    mplew.writeInt(scroll.getAtk());
                    mplew.writeInt(scroll.getAtk());
                    if (scroll.getStat() > 0) {
                        mplew.writeInt(scroll.getStat());
                        mplew.writeInt(scroll.getStat());
                        mplew.writeInt(scroll.getStat());
                        mplew.writeInt(scroll.getStat());
                    }
                }
                mplew.writeInt(scroll.getCost());
                mplew.writeInt(0);
                mplew.write(scroll.getViewType() == 0 ? 1 : 0);
            }

            return mplew.getPacket();
        }

        public static byte[] getStarForcePreview(final Map<EchantEquipStat, Integer> stats, long meso, int successprop,
                int destroytype, boolean canfall, boolean chancetime) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EquipmentEnchantDisplay.getValue());
            mplew.write(0x34);
            mplew.write(canfall ? 1 : 0); // 失敗是否降級 ? 1 : 0
            mplew.writeLong(meso); // 消耗量
            mplew.writeLong(0); // 折扣原始消耗量 0 - 不使用折扣
            mplew.writeLong(0); // MVP原始消耗量 0 - 不顯示MVP
            mplew.write(0);
            mplew.write(0);
            mplew.writeLong(9);
            mplew.writeLong(50);
            mplew.writeInt(successprop);
            mplew.writeInt(destroytype); // 損壞類型 0 - 無, 1 - 非常低, 2 - 低, 3 - 有機率, 4 - 高, 5 - 會爆
            mplew.writeInt(0); // 原始成功概率 0 - 不顯示成功率變更
            mplew.writeInt(1);
            mplew.write(chancetime);// chancetime ? 1 : 0
            int equipStats = 0;
            for (EchantEquipStat stat : stats.keySet()) {
                if (!stats.containsKey(stat) || stats.get(stat) <= 0) {
                    continue;
                }
                equipStats |= stat.getValue();
            }
            mplew.writeInt(equipStats);
            if (equipStats != 0) {
                for (EchantEquipStat stat : EchantEquipStat.values()) {
                    if (!stats.containsKey(stat) || stats.get(stat) <= 0) {
                        continue;
                    }
                    mplew.writeInt(stats.get(stat));
                }
            }

            return mplew.getPacket();
        }

        public static byte[] getStarForceMiniGame() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EquipmentEnchantDisplay.getValue());
            mplew.write(0x35);
            mplew.write(0);
            mplew.writeInt(Randomizer.nextInt());
            return mplew.getPacket();
        }

        public static byte[] getEnchantResult(Equip item, Equip item2, int result, int scrollnumber) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EquipmentEnchantDisplay.getValue());
            mplew.write(0x64);
            mplew.write(0);
            mplew.writeInt(result); // 0 - 失敗, 1 - 成功, 2 - 損壞
            mplew.writeMapleAsciiString(result == 0 ? "[失敗]" : result == 1 ? "[成功]" : "[損壞]");
            PacketHelper.GW_ItemSlotBase_Decode(mplew, item, null);
            if (result == 2) {
                mplew.writeShort(0);
            } else {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, item2, null);
            }

            return mplew.getPacket();
        }

        public static byte[] getStarForceResult(Item oldItem, Item newItem, byte result) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EquipmentEnchantDisplay.getValue());
            mplew.write(0x65);
            mplew.writeInt(result); // 0 - 下降, 1 - 成功, 2 - 損壞, 3 - 失敗
            mplew.write(0);
            if (oldItem == null) {
                mplew.writeShort(0);
            } else {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, oldItem, null);
            }
            if (newItem == null) {
                mplew.writeShort(0);
            } else {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, newItem, null);
            }

            return mplew.getPacket();
        }

        public static byte[] getInheritance(Item oldItem, Item newItem) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_EquipmentEnchantDisplay.getValue());
            mplew.write(0x67);
            if (oldItem == null) {
                mplew.writeShort(0);
            } else {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, oldItem, null);
            }
            if (newItem == null) {
                mplew.writeShort(0);
            } else {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, newItem, null);
            }

            return mplew.getPacket();
        }
    }

    public static byte[] cancelJaguarRiding() {
        if (ServerConfig.LOG_PACKETS) {
            System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());
        PacketHelper.writeSingleMask(mplew, MapleBuffStat.RideVehicle);
        mplew.writeShort(263);
        return mplew.getPacket();
    }

    public static byte[] getChangeDamageSkin() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANGE_DAMAGE_SKIN_RESPONSE.getValue());

        return mplew.getPacket();
    }

    public static byte[] getSaveDamageSkin(int damageSkin, List<Integer> saveDamageSkin) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SAVE_DAMAGE_SKIN_RESPONSE.getValue());
        getDamageSkinInfo(mplew, damageSkin, saveDamageSkin);

        return mplew.getPacket();
    }

    public static byte[] getRemoveDamageSkin(int damageSkin, List<Integer> saveDamageSkin) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_DAMAGE_SKIN_RESPONSE.getValue());
        getDamageSkinInfo(mplew, damageSkin, saveDamageSkin);

        return mplew.getPacket();
    }

    public static void getDamageSkinInfo(MaplePacketLittleEndianWriter mplew, int damageSkin,
            List<Integer> saveDamageSkin) {
        mplew.writeInt(damageSkin);
        mplew.writeInt(saveDamageSkin.size());
        for (int damage : saveDamageSkin) {
            mplew.writeInt(damage);
        }
    }

    public static byte[] getMacros(SkillMacro[] macros) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MacroSysDataInit.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count);
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                mplew.writeMapleAsciiString(macro.getName());
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }

        return mplew.getPacket();
    }

    public static byte[] onHourChanged(short dayOfWeek, int unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_HourChanged.getValue());
        mplew.writeShort(dayOfWeek);
        mplew.writeShort(unk);
        return mplew.getPacket();
    }

    public static void SetSaleInfo(MaplePacketLittleEndianWriter mplew) {
        CashItemFactory cif = CashItemFactory.getInstance();

        // 限時販售
        int[] aCashItem = new int[] { 110100075 };
        mplew.writeInt(aCashItem.length);
        for (int i = 0; i < aCashItem.length; i++) {
            mplew.writeInt(aCashItem[i]);
        }

        // 商城道具
        List<CashItem> csHideItems = cif.getHideAllDefaultItems(); // 關閉預設物品
        List<CashModItem> csItems = cif.getAllModItems();
        mplew.writeShort(csHideItems.size() + csItems.size());
        // 隱藏不出售的商品
        for (CashItem csItem : csHideItems) {
            mplew.writeInt(csItem.getSN());

            int[] mask = new int[2];
            CashItemFlag flag = CashItemFlag.OnSale;
            mask[flag.getPosition()] |= flag.getValue();
            for (int i = 0; i < mask.length; i++) {
                mplew.writeInt(mask[i]);
            }
            mplew.write(false);
        }
        // 自定義商品寫入
        for (CashModItem csMod : csItems) {
            mplew.writeInt(csMod.getSN());
            CS_COMMODITY.DecodeModifiedData(mplew, csMod);
        }

        // 套組名稱
        short nCashPackageNameSize = 0;
        mplew.writeShort(nCashPackageNameSize);
        for (int i = 0; i < nCashPackageNameSize; i++) {
            mplew.writeInt(0); // nSN
            mplew.writeMapleAsciiString(""); // m_mCashPackageName
        }

        // 隨機箱子
        Map<Integer, List<Integer>> mCashRandomItems = cif.getRandomItemInfo();
        mplew.writeInt(mCashRandomItems.size()); // nCashRandomItemCount
        for (Map.Entry<Integer, List<Integer>> eCashRandomItem : mCashRandomItems.entrySet()) {
            mplew.writeInt(eCashRandomItem.getKey());
            if (eCashRandomItem.getKey() / 1000 != 5533) {
                break;
            }
            mplew.writeInt(eCashRandomItem.getValue().size());
            for (int itemSN : eCashRandomItem.getValue()) {
                mplew.writeInt(itemSN);
            }
        }

        int nCount = 0;
        mplew.writeInt(nCount);
        for (int i = 0; i < nCount; i++) {
            mplew.writeInt(i);
            mplew.writeInt(0); // 道具SN[F7 A4 98 00] [F8 A4 98 00]
            mplew.writeInt(0); // 道具ID[96 95 4E 00閃亮髮型卷] [C8 9D 4E 00閃亮整型卷]
            mplew.writeLong(0); // [00 A0 C1 29 E5 82 CE 01]
            mplew.writeLong(0); // [00 80 39 0F 01 93 CE 01]
            mplew.writeInt(0); // [00 00 00 00]

            int[] unkData = { 0, 0x14, 0x1E, 0x28, 0x32 };
            mplew.writeInt(unkData.length);
            for (int j = 0; j < unkData.length; j++) {
                mplew.writeInt(unkData[i]);
            }
        }
    }

    public static byte[] onMiniMapOnOff(boolean onoff) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_MiniMapOnOff.getValue());
        mplew.writeBoolean(onoff);
        return mplew.getPacket();
    }
}
