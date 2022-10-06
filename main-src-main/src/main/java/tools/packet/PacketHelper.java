package tools.packet;

import client.*;
import client.character.stat.MapleBuffStat;
import client.character.stat.TSIndex;
import client.familiar.FamiliarCard;
import client.inventory.*;
import client.skill.Skill;
import client.skill.SkillEntry;
import constants.GameConstants;
import constants.ItemConstants;
import constants.SkillConstants;
import handling.BuffStat;
import handling.world.MapleCharacterLook;
import server.*;
import server.maps.MapleSummon;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.MapleShop;
import server.shops.MapleShopItem;
import server.stores.AbstractPlayerStore;
import server.stores.IMaplePlayerShop;
import tools.DateUtil;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class PacketHelper {

    private static void addStartedQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        boolean newPacket = true;
        mplew.write(newPacket);
        final List<MapleQuestStatus> started = chr.getStartedQuests();
        mplew.writeShort(started.size());
        if (started.size() > 0) {
            for (MapleQuestStatus q : started) {
                mplew.writeInt(q.getQuest().getId());
                if (q.hasMobKills()) {
                    StringBuilder sb = new StringBuilder();
                    for (Integer integer : q.getMobKills().values()) {
                        int kills = (integer);
                        sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
                    }
                    mplew.writeMapleAsciiString(sb.toString());
                } else {
                    mplew.writeMapleAsciiString(q.getCustomData() == null ? "" : q.getCustomData());
                }
            }
        }
        if (!newPacket) {
            mplew.writeShort(started.size());
            if (started.size() > 0) {
                for (MapleQuestStatus q : started) {
                    mplew.writeInt(q.getQuest().getId());
                }
            }
        }
        addNXQuestInfo(mplew, chr);
    }

    public static void addNXQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        int mm = 0;
        mplew.writeShort(mm);
        for (int m = 0; m < mm; m++) {
            mplew.writeMapleAsciiString("");
            mplew.writeMapleAsciiString("");
        }
        /*
         * mplew.writeShort(7); mplew.writeMapleAsciiString("1NX5211068");
         * mplew.writeMapleAsciiString("1"); mplew.writeMapleAsciiString("SE20130619");
         * mplew.writeMapleAsciiString("20130626060823");
         * mplew.writeMapleAsciiString("99NX5533018"); mplew.writeMapleAsciiString("1");
         * mplew.writeMapleAsciiString("1NX1003792"); mplew.writeMapleAsciiString("1");
         * mplew.writeMapleAsciiString("1NX1702337"); mplew.writeMapleAsciiString("1");
         * mplew.writeMapleAsciiString("1NX9102857"); mplew.writeMapleAsciiString("1");
         * mplew.writeMapleAsciiString("SE20130116"); mplew.writeMapleAsciiString("1");
         */
    }

    public static void GW_ItemPotSlot(MaplePacketLittleEndianWriter mplew) {
        // dwLifeID
        mplew.writeInt(0);
        // nLevel
        mplew.write(0);
        // nLastState
        mplew.write(0);
        // nSatiety
        mplew.writeInt(0);
        // nFriendly
        mplew.writeInt(0);
        // nRemainAbleFriendly
        mplew.writeInt(0);
        // nRemainFriendlyTime
        mplew.writeInt(0);
        // nMaximumIncLevel
        mplew.write(0);
        // nMaximumIncSatiety
        mplew.writeInt(0);
        // dateLastEatTime
        mplew.writeLong(0);
        // dateLastSleepStartTime
        mplew.writeLong(0);
        // dateLastDecSatietyTime
        mplew.writeLong(0);
        // dateConsumedLastTime
        mplew.writeLong(0);
    }

    public static void addCompletedQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        boolean newPacket = true;
        mplew.write(newPacket);
        final List<MapleQuestStatus> completed = chr.getCompletedQuests();
        mplew.writeShort(completed.size());
        if (completed.size() > 0) {
            for (MapleQuestStatus q : completed) {
                mplew.writeInt(q.getQuest().getId());
                mplew.writeLong(DateUtil.getQuestTimestamp(q.getCompletionTime()));
                // v139 changed from long to int
            }
        }
        if (!newPacket) {
            mplew.writeShort(completed.size());
            for (MapleQuestStatus q : completed) {
                mplew.writeInt(q.getQuest().getId());
            }
        }
    }

    public static void addSkillInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        final Map<Skill, SkillEntry> skills = chr.getSkills();
        boolean newPacket = true;
        mplew.write(newPacket);
        if (newPacket) {
            mplew.writeShort(skills.size());
            for (Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                Skill skil = skill.getKey();
                SkillEntry skillEntry = skill.getValue();
                mplew.writeInt(skil.getId());
                if (skil.isLinkSkills()) {
                    mplew.writeInt(skillEntry.teachId);
                } else if (skil.isTeachSkills()) {
                    mplew.writeInt(skillEntry.teachId > 0 ? skillEntry.teachId : chr.getId());
                } else {
                    mplew.writeInt(skillEntry.skillLevel);
                }
                addExpirationTime(mplew, skillEntry.expiration);
                if (SkillConstants.isSkillNeedMasterLevel(skil.getId())) {
                    mplew.writeInt(skillEntry.masterlevel);
                }
                if (SkillConstants.get紫扇傳授UnknownValue(skil.getId()) > 0) {
                    mplew.writeInt(skillEntry.masterlevel);
                }
            }
        } else {
            final Map<Integer, Integer> skillsWithoutMax = new LinkedHashMap<>();
            final Map<Integer, Long> skillsWithExpiration = new LinkedHashMap<>();
            final Map<Integer, Byte> skillsWithMax = new LinkedHashMap<>();

            for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                int skillId = skill.getKey().getId();
                skillsWithoutMax.put(skillId, skill.getValue().skillLevel);
                if (skill.getValue().expiration > 0L) {
                    skillsWithExpiration.put(skillId, skill.getValue().expiration);
                }
                if (SkillConstants.isSkillNeedMasterLevel(skillId)) {
                    skillsWithMax.put(skillId, skill.getValue().masterlevel);
                }
                if (SkillConstants.get紫扇傳授UnknownValue(skillId) > 0) {
                    skillsWithMax.put(skillId, skill.getValue().masterlevel);
                }
            }

            mplew.writeShort(skillsWithoutMax.size());
            for (final Entry<Integer, Integer> x : skillsWithoutMax.entrySet()) {
                mplew.writeInt((x.getKey()));
                mplew.writeInt((x.getValue()));
            }
            int amount = 0;
            mplew.writeShort(amount);
            for (int i = 0; i < amount; i++) {
                mplew.writeInt(0);
            }

            mplew.writeShort(skillsWithExpiration.size());
            for (final Entry<Integer, Long> x : skillsWithExpiration.entrySet()) {
                mplew.writeInt((x.getKey()));
                mplew.writeLong((x.getValue()));
            }
            amount = 0;
            mplew.writeShort(amount);
            for (int i = 0; i < amount; i++) {
                mplew.writeInt(0);
            }

            mplew.writeShort(skillsWithMax.size());
            for (final Entry<Integer, Byte> x : skillsWithMax.entrySet()) {
                mplew.writeInt((x.getKey()));
                mplew.writeInt((x.getValue()));
            }
            amount = 0;
            mplew.writeShort(amount);
            for (int i = 0; i < amount; i++) {
                mplew.writeInt(0);
            }
        }

        int amount = 0;
        mplew.writeShort(amount);
        for (int i = 0; i < amount; i++) {
            mplew.writeInt(0); // 技能ID
            mplew.writeShort(0);// 技能等級
        }

        // 連結技能
        amount = 0;
        mplew.writeInt(amount);
        for (int i = 0; i < amount; i++) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0); // 技能ID
            mplew.writeShort(0); // 技能等級
            mplew.writeLong(DateUtil.getFileTimestamp(-2)); // Time
        }
    }

    public static void addCoolDownInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        final List<MapleCoolDownValueHolder> cd = chr.getCooldowns();
        mplew.writeShort(cd.size());
        for (MapleCoolDownValueHolder cooling : cd) {
            mplew.writeInt(cooling.skillId);
            mplew.writeInt((int) (cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
        }
    }

    public static void addRocksInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        int[] mapz = chr.getRegRocks();
        for (int i = 0; i < 5; i++) {
            mplew.writeInt(mapz[i]);
        }

        int[] map = chr.getRocks();
        for (int i = 0; i < 10; i++) {
            mplew.writeInt(map[i]);
        }

        int[] maps = chr.getHyperRocks();
        for (int i = 0; i < 13; i++) {
            mplew.writeInt(maps[i]);
        }
    }

    public static void addMiniGameInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        short size = 0;
        mplew.writeShort(size);
        for (int i = 0; i < size; i++) {
            // dwOwnerAID
            mplew.writeInt(0);
            // sOwnerName
            mplew.writeInt(0);
            // lRewardGradeQ.dummy
            mplew.writeInt(0);
            // lRewardGradeQ._Myhead
            mplew.writeInt(0);
            // lRewardGradeQ._Mysize
            mplew.writeInt(0);
            mplew.write(0);
        }
    }

    public static void addRingInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> aRing = chr.getRings(true);
        List<MapleRing> cRing = aRing.getLeft();
        mplew.writeShort(cRing.size());
        for (MapleRing ring : cRing) {
            // CInPacket::DecodeBuffer 0x23
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 15);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
        }
        List<MapleRing> fRing = aRing.getMid();
        mplew.writeShort(fRing.size());
        for (MapleRing ring : fRing) {
            // CInPacket::DecodeBuffer 0x27
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeAsciiString(ring.getPartnerName(), 15);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
            mplew.writeInt(ring.getItemId());
        }
        List<MapleRing> mRing = aRing.getRight();
        mplew.writeShort(mRing.size());
        int marriageId = 30000;
        for (MapleRing ring : mRing) {
            // CInPacket::DecodeBuffer 0x34
            mplew.writeInt(marriageId);
            mplew.writeInt(chr.getId());
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeShort(3);
            mplew.writeInt(ring.getItemId());
            mplew.writeInt(ring.getItemId());
            mplew.writeAsciiString(chr.getName(), 15);
            mplew.writeAsciiString(ring.getPartnerName(), 15);
        }
    }

    public static void addMoneyInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeLong(chr.getMeso());
    }

    public static List<Item> getInventoryInfo(List<Item> equipped, int position) {
        return getInventoryInfo(equipped, position, 100);
    }

    public static List<Item> getInventoryInfo(List<Item> equipped, int position, int size) {
        List<Item> items = new LinkedList<>();
        for (Item item : equipped) {
            int pos = -item.getPosition();
            if (pos >= position && pos < position + size) {
                items.add(item);
            }
        }
        Collections.sort(items);
        return items;
    }

    public static void NonBPEquip_Decode(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, List<Item> equipped) {
        int v4 = 0;
        // 龍魔龍裝備(1000) + 機甲裝備(1100) + 機器人的現金裝備(1200)
        // 天使破壞者裝備(1300) + 拼圖(1400) + 未知[未確認](1500)
        // 獸魔裝備(5100) + 花狐裝備(5200) + 圖騰(5000)
        // 未知[未確認](6000)
        int[] startRang = { 1000, 1100, 1200, 1300, 1400, 1500, 5100, 1600, 5200, 5000, 6000 };
        int[] endRang = { 1004, 1105, 1207, 1305, 1425, 1512, 5106, 1603, 5201, 5003, 6025 };
        Iterator<Item> Iitem;
        Item item;
        do {
            Iitem = getInventoryInfo(equipped, startRang[v4], endRang[v4]).iterator();
            while (true) {
                item = Iitem.hasNext() ? Iitem.next() : null;
                mplew.writeShort(getItemPosition(item, false, false));
                if (item != null) {
                    GW_ItemSlotBase_Decode(mplew, item, chr);
                } else {
                    break;
                }
            }
            ++v4;
        } while (v4 < 11);
    }

    public static void VirtualEquipInventory__Decode(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, List<Item> equipped) {
        int[] startRang = { 20000, 21000 };
        int[] endRang = { 20024, 21024 };
        Iterator<Item> Iitem;
        Item item;
        int v4 = 0;
        do {
            Iitem = getInventoryInfo(equipped, startRang[v4], endRang[v4]).iterator();
            while (true) {
                item = Iitem.hasNext() ? Iitem.next() : null;
                mplew.writeShort(getItemPosition(item, false, false));
                if (item != null) {
                    GW_ItemSlotBase_Decode(mplew, item, chr);
                } else {
                    break;
                }
            }
            ++v4;
        } while (v4 < 2);
    }

    public static void addInventoryInfo3(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, List<Item> equipped) {
        int[] startRang = { 20001, 20049 };
        int[] endRang = { 20048, 20051 };
        Iterator<Item> Iitem;
        Item item;
        int v4 = 0;
        do {
            Iitem = getInventoryInfo(equipped, startRang[v4], endRang[v4]).iterator();
            while (true) {
                item = Iitem.hasNext() ? Iitem.next() : null;
                mplew.writeShort(getItemPosition(item, false, false));
                if (item != null) {
                    GW_ItemSlotBase_Decode(mplew, item, chr);
                } else {
                    break;
                }
            }
            ++v4;
        } while (v4 < 2);
    }

    public static void addPotionPotInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        if (chr.getPotionPots() == null) {
            mplew.writeInt(0);
            return;
        }
        mplew.writeInt(chr.getPotionPots().size());
        for (MaplePotionPot p : chr.getPotionPots()) {
            mplew.writeInt(p.getId());
            mplew.writeInt(p.getMaxValue());
            mplew.writeInt(p.getHp());
            mplew.writeInt(0);
            mplew.writeInt(p.getMp());

            mplew.writeLong(DateUtil.getFileTimestamp(p.getStartDate()));
            mplew.writeLong(DateUtil.getFileTimestamp(p.getEndDate()));
        }
    }

    public static void GW_CharacterStat__Decode(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean charList) {
        mplew.writeInt(chr.getId());
        mplew.writeInt(charList ? 0 : chr.getId());
        mplew.writeInt(charList ? 1 : 0);
        mplew.writeAsciiString(chr.getName(), 15);

        mplew.write(chr.getGender());
        mplew.write(0); // addCharStats unk
        mplew.write(chr.getSkinColor());
        mplew.writeInt(chr.getFace());
        mplew.writeInt(chr.getHair());
        mplew.write(-1); // nMixBaseHairColor
        mplew.write(0); // nMixAddHairColor
        mplew.write(0); // nMixHairBaseProb

        mplew.write(chr.getLevel());
        mplew.writeShort(chr.getJob());
        chr.getStat().connectData(mplew, chr);
        mplew.writeShort(chr.getRemainingAp());
        if (GameConstants.isSeparatedSp(chr.getJob())) {
            int size = chr.getRemainingSpSize();
            mplew.write(size);
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    mplew.write(i + 1);
                    mplew.writeInt(chr.getRemainingSp(i));
                }
            }
        } else {
            mplew.writeShort(chr.getRemainingSp());
        }
        mplew.writeLong(chr.getExp());
        mplew.writeInt(chr.getFame());
        mplew.writeInt(chr.getWeaponPoint()); // 神之子武器點數
        mplew.writeLong(chr.getGachExp());
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        mplew.writeInt(chr.getMapId());
        mplew.write(chr.getInitialSpawnpoint());
        mplew.writeShort(chr.getSubcategory());
        if (MapleJob.is惡魔(chr.getJob()) || MapleJob.is傑諾(chr.getJob()) || MapleJob.is幻獸師(chr.getJob())) {
            mplew.writeInt(chr.getFaceMarking());
        }
        mplew.writeShort(chr.getFatigue());
        mplew.writeInt(GameConstants.getCurrentDate());

        // CInPacket::Decode4 * 6
        for (MapleTrait.MapleTraitType t : MapleTrait.MapleTraitType.values()) {
            mplew.writeInt(chr.getTrait(t).getLocalExp());
        }

        // CInPacket::DecodeBuffer(0x15) {
        for (MapleTrait.MapleTraitType t : MapleTrait.MapleTraitType.values()) {
            mplew.writeShort(0); // today's trait points
        }
        mplew.write(0);
        mplew.writeLong(DateUtil.getFileTimestamp(-2L));
        // }

        mplew.writeInt(chr.getStat().pvpExp);
        mplew.write(chr.getStat().pvpRank);
        mplew.writeInt(chr.getBattlePoints());
        mplew.write(6);// idk
        mplew.write(7);
        mplew.writeInt(0);
        mplew.writeInt(0);
//        addPartTimeJob(mplew, MapleCharacter.getPartTime(chr.getId()));
        chr.getCharacterCard().connectData(mplew);
        mplew.writeReversedLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        // Function {
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        // }
        mplew.writeZeroBytes(25);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
    }

    public static void AvatarLook__Decode(MaplePacketLittleEndianWriter mplew, MapleCharacterLook chr, boolean mega, boolean second) {
        mplew.write(second ? chr.getSecondGender() : chr.getGender()); // nGender
        mplew.write(second ? chr.getSecondSkinColor() : chr.getSkinColor()); // nSkin
        mplew.writeInt(second ? chr.getSecondFace() : chr.getFace()); // nFace
        mplew.writeInt(chr.getJob()); // nJob
        mplew.write(mega ? 0 : 1);
        mplew.writeInt(second ? chr.getSecondHair() : chr.getHair()); // anHairEquip

        final Map<Byte, Integer> myEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> equip = second ? chr.getSecondEquips(true) : chr.getEquips(true);
        for (final Entry<Byte, Integer> item : equip.entrySet()) {
            if ((item.getKey()) < -127) {
                continue;
            }
            byte pos = (byte) ((item.getKey()) * -1);

            if ((pos < 100) && (myEquip.get(pos) == null)) {
                myEquip.put(pos, item.getValue());
            } else if ((pos > 100) && (pos != 111)) {
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getValue());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getValue());
            }
        }
        final Map<Byte, Integer> totemEquip = chr.getTotems();

        for (Map.Entry<Byte, Integer> entry : myEquip.entrySet()) {
            int weapon = entry.getValue();
            if (ItemConstants.武器類型(weapon) == (second ? MapleWeaponType.璃 : MapleWeaponType.琉)) {
                continue;
            }
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);

        for (Map.Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);

        for (Map.Entry<Byte, Integer> entry : totemEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);

        boolean zero = MapleJob.is神之子(chr.getJob());
        Integer cWeapon = equip.get((byte) -111);
        Integer Weapon = equip.get((byte) -11);
        Integer Shield = equip.get((byte) -10);
        mplew.writeInt(cWeapon != null ? cWeapon : 0); // nWeaponStickerID
        mplew.writeInt(Weapon != null ? Weapon : 0); // nWeaponID
        mplew.writeInt(!zero && Shield != null ? Shield : 0); // nSubWeaponID
        mplew.writeInt(!MapleJob.is精靈遊俠(chr.getJob()) ? chr.getElf() : chr.getElf() == 0 ? 1 : 0);// 精靈耳朵 // bDrawElfEar
        mplew.write(0); // TODO: ?
        // 寵物
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(!second ? chr.getPet(i) : 0); // anPetID
        }
        if (MapleJob.is惡魔(chr.getJob())) {
            mplew.writeInt(chr.getFaceMarking()); // nDemonSlayerDefFaceAcc
        } else if (MapleJob.is傑諾(chr.getJob())) {
            mplew.writeInt(chr.getFaceMarking()); // nXenonDefFaceAcc
        } else if (MapleJob.is神之子(chr.getJob())) {
            mplew.write(second); // bIsZeroBetaLook
        } else if (MapleJob.is幻獸師(chr.getJob())) {
            mplew.writeInt(chr.getFaceMarking());
            mplew.write(1);
            mplew.writeInt(chr.getEars());
            mplew.write(1);
            mplew.writeInt(chr.getTail());
        }
        mplew.write(0); // nMixedHairColor
        mplew.write(0); // nMixHairPercent
        mplew.writeZeroBytes(5);// 186+
    }

    public static void addExpirationTime(MaplePacketLittleEndianWriter mplew, long time) {
        mplew.writeLong(DateUtil.getFileTimestamp(time));
    }

    public static int getItemPosition(Item item, boolean trade, boolean bagSlot) {
        if (item == null) {
            return 0;
        }
        short pos = item.getPosition();
        if (pos <= -1) {
            pos = (short) (pos * -1);
            if (pos > 100 && pos < 1000) {
                pos = (short) (pos - 100);
            }
        }
        if (bagSlot) {
            return pos % 100 - 1;
        } else if (!trade && item.getType() == 1) {
            return pos;
        } else {
            return pos;
        }
    }

    public static void GW_ItemSlotBase_Decode(MaplePacketLittleEndianWriter mplew, Item item) {
        GW_ItemSlotBase_Decode(mplew, item, null);
    }

    public static void GW_ItemSlotBase_Decode(final MaplePacketLittleEndianWriter mplew, final Item item,  final MapleCharacter chr) {
        if (item == null) {
            mplew.write(0);
            return;
        }
        mplew.write(item.getPet() != null ? 3 : item.getType());

        GW_ItemSlotBase_RawDecode(mplew, item, chr);
        if (item.getPet() != null) { // Pet
            GW_ItemSlotPet_RawDecode(mplew, item, item.getPet());
        } else if (item.getType() == 1) {
            GW_ItemSlotEquip_RawDecode(mplew, item);
        } else {
            GW_ItemSlotBundle_RawDecode(mplew, item);
        }
    }

    public static void GW_ItemSlotBase_RawDecode(final MaplePacketLittleEndianWriter mplew, final Item item, final MapleCharacter chr) {
        mplew.writeInt(item.getItemId());
        boolean hasUniqueId = item.getUniqueId() > 0 && !ItemConstants.類型.結婚戒指(item.getItemId()) && item.getItemId() / 10000 != 166;
        // marriage rings arent cash items so dont have uniqueids, but we assign them
        // anyway for the sake of rings
        mplew.write(hasUniqueId ? 1 : 0);
        if (hasUniqueId) {
            mplew.writeLong(item.getUniqueId());
        }
        addExpirationTime(mplew, item.getExpiration());
        mplew.writeInt(chr == null ? -1 : chr.getExtendedSlots().indexOf(item.getItemId()));
    }

    public static void GW_ItemSlotEquip_RawDecode(final MaplePacketLittleEndianWriter mplew, final Item item) {
        boolean hasUniqueId = item.getUniqueId() > 0 && !ItemConstants.類型.結婚戒指(item.getItemId()) && item.getItemId() / 10000 != 166;
        final Equip equip = Equip.calculateEquipStats((Equip) item);

        // v201 + sub_2291FB0
        boolean v1 = false;
        mplew.write(v1);
        if (v1) {
            int a2 = 0;
            mplew.writeZeroBytes(4 * a2);
            mplew.writeZeroBytes(4 * (a2 << 28 >> 28));
            mplew.writeZeroBytes(4 * (a2 << 24 >> 28));
            mplew.writeZeroBytes(6 * (a2 << 20 >> 28));
            mplew.writeZeroBytes(4 * (a2 << 16 >> 28));
            boolean v3 = false;
            mplew.write(v3);
        }

        GW_ItemSlotEquipBase__Decode(mplew, equip);

        // 擁有者名字
        mplew.writeMapleAsciiString(equip.getOwner());
        // 潛能等級 17 = 特殊rare, 18 = 稀有epic, 19 = 罕見unique, 20 = 傳說legendary, potential
        // flags. special grade is 14 but it crashes
        mplew.write(equip.getState(true) > 0 && equip.getState(true) < 17 ? equip.getState(false) | 0x20 : equip.getState(false));
        // 裝備星級
        mplew.write(equip.getEnhance());
        // 潛在能力
        for (int i = 1; i <= 3; i++) {
            mplew.writeShort(equip.getState(false) > 0 && equip.getState(false) < 17 ? 0 : equip.getPotential(i, false));
        }
        // 附加潛能
        for (int i = 1; i <= 3; i++) {
            mplew.writeShort(equip.getState(true) > 0 && equip.getState(true) < 17 ? i == 1 ? equip.getState(true) : 0 : equip.getPotential(i, true));
        }
        // 鐵砧
        mplew.writeShort(equip.getFusionAnvil() % 10000);
        // Alien Stone FLAG
        mplew.writeShort(equip.getSocketState());
        // Alien Stone能力(Item.wz/Install/0306.img) > 0 = 安裝, 0 = 空, -1 = 無.
        for (int i = 1; i <= 3; i++) {
            mplew.writeShort(equip.getSocket(i) % 10000);
        }

        mplew.writeInt(0);

        // Ver182手記……這裡為0或沒有這個那麼將無法使用楓方塊
        if (!hasUniqueId) {
            mplew.writeLong(/* equip.getInventoryId() */equip.getUniqueId()); // some tracking ID
        }
        mplew.writeLong(DateUtil.getFileTimestamp(-2)); // ftEquipped
        mplew.writeInt(-1); // nPrevBonusExpRate

        // GW_CashItemOption::Decode{
        mplew.writeLong(0); // liCashItemSN
        mplew.writeLong(DateUtil.getFileTimestamp(-2)); // ftExpireDate
        mplew.writeInt(0); // nGrade
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(0); // anOption
        }
        // }

        // 靈魂寶珠
        mplew.writeShort(equip.getSoulName());
        // 靈魂捲軸
        mplew.writeShort(equip.getSoulEnchanter());
        // 靈魂潛能
        mplew.writeShort(equip.getSoulPotential());
        if (item.getItemId() / 10000 == 172) {
            mplew.writeShort(0);
            mplew.writeInt(0);
            mplew.writeShort(0);
        }
        // 突破傷害上限
        mplew.writeInt(equip.getMaxDamage());
        mplew.writeLong(DateUtil.getFileTimestamp(-2));
    }

    public static void GW_ItemSlotBundle_RawDecode(final MaplePacketLittleEndianWriter mplew, final Item item) {
        mplew.writeShort(item.getQuantity());
        mplew.writeMapleAsciiString(item.getOwner());
        mplew.writeShort(item.getFlag());
        if (ItemConstants.類型.可充值道具(item.getItemId()) || item.getItemId() == 4001886 /* 強烈的力量結晶 */ || item.getItemId() / 10000 == 302 || item.getItemId() / 10000 == 287 /* || 經驗椅子 */) {
            mplew.writeLong(/* (int) */(item.getInventoryId() <= 0 ? -1 : item.getInventoryId()));
        }

        mplew.writeInt(0);

        // CInPacket::DecodeBuffer 0x11
        int familiarId = ItemConstants.getFamiliarByItemID(item.getItemId());
        FamiliarCard card = item.getFamiliarCard();
        mplew.writeInt(familiarId);
        mplew.writeShort(familiarId > 0 && card != null ? item.getFamiliarCard().getLevel() : 1);
        mplew.writeShort(familiarId > 0 && card != null ? item.getFamiliarCard().getLevel() : 1);
        mplew.writeShort(familiarId > 0 && card != null ? item.getFamiliarCard().getLevel() : 1);
        mplew.writeShort(familiarId > 0 && card != null ? item.getFamiliarCard().getOption1() : 0);
        mplew.writeShort(familiarId > 0 && card != null ? item.getFamiliarCard().getOption2() : 0);
        mplew.writeShort(familiarId > 0 && card != null ? item.getFamiliarCard().getOption3() : 0);
        mplew.write(familiarId > 0 && card != null ? item.getFamiliarCard().getGrade() : 0);
    }

    public static void GW_ItemSlotPet_RawDecode(MaplePacketLittleEndianWriter mplew, Item item, MaplePet pet) {
        // sPetName
        mplew.writeAsciiString(pet.getName(), 13);
        // nLevel
        mplew.write(pet.getLevel());
        // nTameness
        mplew.writeShort(pet.getCloseness());
        // nRepleteness
        mplew.write(pet.getFullness());

        // dateDead
        long timeNow = System.currentTimeMillis();
        if (item == null) {
            mplew.writeLong(DateUtil.getKoreanTimestamp((long) (timeNow * 1.5)));
        } else {
            long expiration = item.getExpiration();
            if (expiration <= 0) {
                mplew.writeLong(0);
            } else {
                addExpirationTime(mplew, expiration <= timeNow ? -1L : expiration);
            }
        }

        // nPetAttribute
        mplew.writeShort(0);
        // usPetSkill
        mplew.writeShort(pet.getFlags());
        // nRemainLife
        mplew.writeInt(Math.max(pet.getLimitedLife(), 0));
        // nAttribute
        mplew.writeShort(pet.isCanPickup() ? 0 : 2);
        // nActiveState
        mplew.write(pet.getSummoned() ? pet.getSummonedValue() : 0); // 位置
        // nAutoBuffSkill
        mplew.writeInt(pet.getBuffSkill()); // 裝備的BUFF技能
        // nPetHue
        mplew.writeInt(0); // 訓練箱子技能
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(-1);
        // nGiantRate
        mplew.writeShort(100); // 114 || 112
    }

    public static void GW_ItemSlotEquipBase__Decode(MaplePacketLittleEndianWriter mplew, Equip equip) {
        int head_1 = 0;
        int head_2 = 0;
        if (equip.getStats().size() > 0) {
            for (EquipStat stat : equip.getStats()) {
                if (stat.getPosition() == 1) {
                    head_1 |= stat.getValue();
                } else if (stat.getPosition() == 2) {
                    head_2 |= stat.getValue();
                }
            }
        }
        mplew.writeInt(head_1);
        if (equip.getStats().contains(EquipStat.SLOTS)) {
            mplew.write(equip.getUpgradeSlots());
        }
        if (equip.getStats().contains(EquipStat.LEVEL)) {
            mplew.write(equip.getLevel());
        }
        if (equip.getStats().contains(EquipStat.STR)) {
            mplew.writeShort(equip.getStr(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.DEX)) {
            mplew.writeShort(equip.getDex(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.INT)) {
            mplew.writeShort(equip.getInt(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.LUK)) {
            mplew.writeShort(equip.getLuk(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.MHP)) {
            mplew.writeShort(equip.getHp(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.MMP)) {
            mplew.writeShort(equip.getMp(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.WATK)) {
            mplew.writeShort(equip.getWatk(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.MATK)) {
            mplew.writeShort(equip.getMatk(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.WDEF)) {
            mplew.writeShort(equip.getWdef(0xFFFF));
        }
        // if (equip.getStats().contains(EquipStat.MDEF)) {
        // mplew.writeShort(equip.getMdef(0xFFFF));
        // }
        // if (equip.getStats().contains(EquipStat.ACC)) {
        // mplew.writeShort(equip.getAcc(0xFFFF));
        // }
        // if (equip.getStats().contains(EquipStat.AVOID)) {
        // mplew.writeShort(equip.getAvoid(0xFFFF));
        // }
        if (equip.getStats().contains(EquipStat.HANDS)) {
            mplew.writeShort(equip.getHands(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.SPEED)) {
            mplew.writeShort(equip.getSpeed(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.JUMP)) {
            mplew.writeShort(equip.getJump(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.FLAG)) {
            mplew.writeInt(equip.getFlag());
        }
        if (equip.getStats().contains(EquipStat.INC_SKILL)) {
            mplew.write(equip.getIncSkill() > 0 ? 1 : 0);
        }
        if (equip.getStats().contains(EquipStat.ITEM_LEVEL)) {
            mplew.write(Math.max(equip.getBaseLevel(), equip.getEquipLevel())); // Item level
        }
        if (equip.getStats().contains(EquipStat.ITEM_EXP)) {
            mplew.writeLong(equip.getExpPercentage() * 100000); // Item Exp... 10000000 = 100%
        }
        if (equip.getStats().contains(EquipStat.DURABILITY)) {
            mplew.writeInt(equip.getDurability());
        }
        if (equip.getStats().contains(EquipStat.VICIOUS_HAMMER)) {
            mplew.writeShort(equip.getViciousHammer());
            mplew.writeShort(equip.getPlatinumHammer());
        }
        if (equip.getStats().contains(EquipStat.PVP_DAMAGE)) {
            mplew.writeShort(equip.getPVPDamage());
        }
        if (equip.getStats().contains(EquipStat.DOWNLEVEL)) {
            mplew.write(0);
        }
        if (equip.getStats().contains(EquipStat.ENHANCT_BUFF)) {
            mplew.writeShort(equip.getEnhanctBuff());
        }
        if (equip.getStats().contains(EquipStat.DURABILITY_SPECIAL)) {
            mplew.writeInt(equip.getDurability());
        }
        if (equip.getStats().contains(EquipStat.REQUIRED_LEVEL)) {
            mplew.write(equip.getReqLevel());
        }
        if (equip.getStats().contains(EquipStat.YGGDRASIL_WISDOM)) {
            mplew.write(equip.getYggdrasilWisdom());
        }
        if (equip.getStats().contains(EquipStat.FINAL_STRIKE)) {
            mplew.write(equip.getFinalStrike());
        }
        if (equip.getStats().contains(EquipStat.BOSS_DAMAGE)) {
            mplew.write(equip.getBossDamage(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.IGNORE_PDR)) {
            mplew.write(equip.getIgnorePDR(0xFFFF));
        }

        mplew.writeInt(head_2);
        if (equip.getStats().contains(EquipStat.TOTAL_DAMAGE)) {
            mplew.write(equip.getTotalDamage(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.ALL_STAT)) {
            mplew.write(equip.getAllStat(0xFFFF));
        }
        if (equip.getStats().contains(EquipStat.KARMA_COUNT)) {
            mplew.write(equip.getKarmaCount());
        }
        if (equip.getStats().contains(EquipStat.FLAME)) {
            mplew.writeLong(System.currentTimeMillis());
        }
        if (equip.getStats().contains(EquipStat.STAR_FORCE)) {
            mplew.write(0);
            mplew.write(equip.getStarForce());
            mplew.write(0);
            mplew.write(0);
        }
        // if (equip.isTrace()) {
        // mplew.write(1);
        // }
    }

    public static void serializeMovementList(MaplePacketLittleEndianWriter mplew, int duration, Point mPos, Point oPos,
            List<LifeMovementFragment> moves) {
        mplew.writeInt(duration); // m_tEncodedGatherDuration
        mplew.writePos(mPos); // m_x_CS // m_y_CS
        mplew.writePos(oPos);
        mplew.write(moves.size());
        for (LifeMovementFragment move : moves) {
            move.serialize(mplew);
        }
    }

    public static void addAnnounceBox(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        if ((chr.getPlayerShop() != null) && (chr.getPlayerShop().isOwner(chr))
                && (chr.getPlayerShop().getShopType() != 1) && (chr.getPlayerShop().isAvailable())) {
            addInteraction(mplew, chr.getPlayerShop());
        } else {
            mplew.write(0);
        }
    }

    public static void addInteraction(MaplePacketLittleEndianWriter mplew, IMaplePlayerShop shop) {
        mplew.write(shop.getGameType()); // m_nMiniRoomType
        mplew.writeInt(((AbstractPlayerStore) shop).getObjectId()); // m_dwMiniRoomSN
        mplew.writeMapleAsciiString(shop.getDescription());
        if (shop.getShopType() != IMaplePlayerShop.HIRED_MERCHANT) {
            mplew.write(shop.getPassword().length() > 0 ? 1 : 0);
        }
        mplew.write(shop.getItemId() % 10000); // nSpec
        mplew.write(shop.getSize()); // nCurUsers
        mplew.write(shop.getMaxSize()); // nMaxUsers
        if (shop.getShopType() != IMaplePlayerShop.HIRED_MERCHANT) {
            mplew.write(shop.isOpen() ? 0 : 1);
        }
    }

    public static void CharacterData_Decode(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        long dbcharFlag = 0xFF_FF_FF_FF_FF_FF_FF_FFL; // FF FF FF FF FF FF DF FF v148+
        mplew.writeLong(dbcharFlag);
        // nCombatOrders
        mplew.write(0);
        // aPetActiveSkillCoolTime
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(-3);
        }

        int nAddTail = 0;
        mplew.write(nAddTail); // 應該是拼圖
        for (int i = 1; i < nAddTail; i++) {
            mplew.writeInt(0);
        }

        int FILETIME_Count = 0;
        mplew.writeInt(FILETIME_Count);
        for (int i = 0; i < FILETIME_Count; i++) {
            // nWillEXP
            mplew.writeInt(0);
            // FILETIME
            mplew.writeInt(0);
            // MONSTERLIFE_INVITEINFO
            mplew.writeInt(0);
        }

        boolean v215 = false;
        mplew.writeBoolean(v215);
        if (v215) {
            mplew.write(0);
            int v11 = 0;
            mplew.writeInt(v11);
            for (int i = 0; i < v11; i++) {
                mplew.writeLong(0);
            }

            int v14 = 0;
            mplew.writeInt(v14);
            for (int i = 0; i < v14; i++) {
                mplew.writeLong(0);
            }
        }

        if ((dbcharFlag & 1) != 0) {
            GW_CharacterStat__Decode(mplew, chr, false); // 角色狀態訊息
            mplew.write(chr.getBuddylist().getCapacity()); // 好友上限
            mplew.write(chr.getBlessOfFairyOrigin() != null); // 精靈的祝福
            if (chr.getBlessOfFairyOrigin() != null) {
                mplew.writeMapleAsciiString(chr.getBlessOfFairyOrigin());
            }
            mplew.write(chr.getBlessOfEmpressOrigin() != null); // 女皇的祝福
            if (chr.getBlessOfEmpressOrigin() != null) {
                mplew.writeMapleAsciiString(chr.getBlessOfEmpressOrigin());
            }
            // 終極冒險家訊息
            MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
            mplew.write((ultExplorer != null) && (ultExplorer.getCustomData() != null));
            if ((ultExplorer != null) && (ultExplorer.getCustomData() != null)) {
                mplew.writeMapleAsciiString(ultExplorer.getCustomData());
            }
            mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
            mplew.writeLong(DateUtil.getFileTimestamp(-2L));
            // v201 sub_510A90
            PacketHelper.UnkFunctin6(mplew);
        }
        if ((dbcharFlag & 2) != 0) {
            addMoneyInfo(mplew, chr);// 楓幣
            // CInPacket::DecodeBuffe(0xC) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(0); // 小鋼珠
            mplew.writeInt(chr.getCSPoints(2)); // 楓葉點數
            // }
        }
        // GW_ExpConsumeItem
        if ((dbcharFlag & 0x2000008) != 0) {
            int GW_ExpConsumeItemCount = 0;
            mplew.writeInt(GW_ExpConsumeItemCount);
            for (int i = 0; i > GW_ExpConsumeItemCount; i++) {
                // dummyBLD.dwNPCID
                mplew.writeInt(0);
                // dummyBLD.nItemIndex
                mplew.writeInt(0);
                // dummyBLD.nItemID
                mplew.writeInt(0);
                // dummyBLD.nCount
                mplew.writeLong(0);
            }
        }
//        if ((dbcharFlag & 0x80) != 0) {
//            int nSlotHyper = 0;
//            mplew.writeInt(nSlotHyper);
//            for (int i = 0; i < nSlotHyper; i++) {
//                SlotHyper(mplew);
//            }
//            UnkFunction(mplew, chr);
//            int v22 = 0;
//            mplew.writeInt(v22);
//            for (int i = 1; i < v22; i++) {
//                mplew.writeInt(0);
//            }
//            boolean v24 = false;
//            mplew.writeBoolean(v24);
//            if (v24) {
//                GW_MonsterBattleLadder_UserInfo(mplew);
//            }
//            int v25 = 0;
//            mplew.write(v25);
//            if (v25 > 0) {
//                for (int i = 0; i < v25; i++) {
//                    GW_MonsterBattleRankInfo(mplew);
//                }
//            }
//            int v26 = 0;
//            mplew.write(v26);
//            if (v26 > 0) {
//                for (int i = 0; i < v26; i++) {
//                    GW_MonsterBattleRankInfo(mplew);
//                }
//            }
//        }

        if ((dbcharFlag & 0x80) != 0) { // 道具欄位個數
            for (int i = 1; i <= 5; i++) {
                MapleInventoryType type = MapleInventoryType.getByType((byte) i);
                mplew.write(chr.getInventory(type).getSlotLimit());
            }
        }

        if ((dbcharFlag & 0x100000) != 0) {
            MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(122700));
            if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()) {
                mplew.writeLong(DateUtil.getFileTimestamp(Long.parseLong(stat.getCustomData())));
            } else {
                mplew.writeLong(DateUtil.getFileTimestamp(-2L));
            }
        }

        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        final List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        Iterator<Item> items;
        if ((dbcharFlag & 4) != 0) { // 角色裝備
            boolean v248 = false;
            mplew.writeBoolean(v248);

            items = getInventoryInfo(equipped, 1, 99).iterator(); // 普通裝備
            while (true) {
                Item v52 = items.hasNext() ? items.next() : null;
                mplew.writeShort(getItemPosition(v52, false, false));
                if (v52 != null) {
                    GW_ItemSlotBase_Decode(mplew, v52, chr);
                } else {
                    break;
                }
            }

            items = getInventoryInfo(equipped, 100, 900).iterator();// 現金裝備
            while (true) {
                Item v58 = items.hasNext() ? items.next() : null;
                mplew.writeShort(getItemPosition(v58, false, false));
                if (v58 != null) {
                    GW_ItemSlotBase_Decode(mplew, v58, chr);
                } else {
                    break;
                }
            }

            if (!v248) {
                items = chr.getInventory(MapleInventoryType.EQUIP).newList().iterator();// 裝備
                while (true) {
                    Item v61 = items.hasNext() ? items.next() : null;
                    mplew.writeShort(getItemPosition(v61, false, false));
                    if (v61 != null) {
                        GW_ItemSlotBase_Decode(mplew, v61, chr);
                    } else {
                        break;
                    }
                }
            }
            NonBPEquip_Decode(mplew, chr, equipped); // 龍魔導裝備欄..等等
            VirtualEquipInventory__Decode(mplew, chr, equipped); // 不知道

            // 201 +
            int v151 = 0;
            mplew.writeInt(v151);
            for (int i = 0; i < v151; i++) {
                mplew.writeLong(8);
                // sub_4EBE40
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeShort(0);
                mplew.writeMapleAsciiString("");
                mplew.writeInt(0);
                mplew.writeLong(8);
            }
        }
        if ((dbcharFlag & 10) != 0) {
            addInventoryInfo3(mplew, chr, equipped); // 不知道
        }

        // 其他欄位
        int other = 2;
        do {
            MapleInventoryType[] mit = { MapleInventoryType.UNDEFINED, MapleInventoryType.EQUIP, MapleInventoryType.USE, MapleInventoryType.SETUP, MapleInventoryType.ETC, MapleInventoryType.CASH };
            items = chr.getInventory(mit[other]).newList().iterator();
            while (true) {
                Item item = items.hasNext() ? items.next() : null;
                mplew.write(getItemPosition(item, false, false));
                if (item != null) {
                    GW_ItemSlotBase_Decode(mplew, item, chr);
                } else {
                    break;
                }
            }
            ++other;
        } while (other <= 5);

        int v197;
        int pos;
        List<Integer> extendedSlots;
        for (int nTi = 2; nTi <= 4; nTi++) { // 椅子包||礦物包之類的額外背包
            // get_item_type_from_typeindex() {
            int v71;
            switch (nTi) {
            case 1:
                v71 = 0x4;
                break;
            case 2:
                v71 = 0x8;
                break;
            case 3:
                v71 = 0x10;
                break;
            case 4:
                v71 = 0x20;
                break;
            case 5:
                v71 = 0x40;
                break;
            default:
                v71 = 0;
                break;
            }
            // }
            if ((dbcharFlag & v71) != 0) {
                extendedSlots = nTi != 3 ? new LinkedList<>() : chr.getExtendedSlots();
                mplew.writeInt(extendedSlots.size()); // sValue._m_pStr
                for (int nCount = 0; nCount < extendedSlots.size(); nCount++) {
                    mplew.writeInt(nCount);
                    if (nTi - 2 <= 2 && nCount >= 0) {
                        if (nTi == 2 || nTi == 3) {
                            v197 = 2;
                        } else {
                            v197 = nTi == 4 ? 7 : 0;
                        }
                        if (nCount < v197) {
                            // BagData::Decode {
                            mplew.writeInt(chr.getExtendedSlot(nTi)); // this->nBagItemID
                            for (Item item : chr.getInventory(MapleInventoryType.getByType((byte) nTi)).list()) {
                                pos = getItemPosition(item, false, true);
                                if (pos <= 0) {
                                    continue;
                                }
                                mplew.writeInt(pos);
                                if (pos <= 0x13) {
                                    GW_ItemSlotBase_Decode(mplew, item, chr);
                                }
                            }
                            mplew.writeInt(-1);
                            // }
                        }
                    }
                }
            }
        }

        if ((dbcharFlag & 0x1000000) != 0) {
            // nSenseEXP
            int unkSize = 0;
            mplew.writeInt(unkSize);
            for (int i = unkSize; i > 0; i--) {
                mplew.writeInt(0);
                mplew.writeLong(0);
            }
        }
        if ((dbcharFlag & 0x40000000) != 0) {
            // DayLimit.nWill
            int unkSize = 0;
            mplew.writeInt(unkSize);
            for (int i = unkSize; i > 0; i--) {
                mplew.writeLong(0);
                mplew.writeLong(0);
            }
        }
        if ((dbcharFlag & 0x800000) != 0) {
            while (true) {
                pos = 0;
                mplew.write(pos);
                if (pos == 0) {
                    break;
                }
                GW_ItemPotSlot(mplew);
            }
        }
        if ((dbcharFlag & 0x100) != 0) {
            addSkillInfo(mplew, chr);// 技能訊息
        }
        if ((dbcharFlag & 0x8000) != 0) {
            addCoolDownInfo(mplew, chr);// 冷卻技能訊息
        }
        if ((dbcharFlag & 0x200) != 0) {
            addStartedQuestInfo(mplew, chr);// 已開始任務訊息
        }
        if ((dbcharFlag & 0x4000) != 0) {
            addCompletedQuestInfo(mplew, chr);// 已完成任務訊息
        }
        if ((dbcharFlag & 0x400) != 0) {
            addMiniGameInfo(mplew, chr);// 小遊戲訊息
        }
        if ((dbcharFlag & 0x800) != 0) {
            addRingInfo(mplew, chr);// 戒指訊息
        }
        if ((dbcharFlag & 0x1000) != 0) {
            addRocksInfo(mplew, chr);
        }
        if ((dbcharFlag & 0x40000) != 0) {
            chr.QuestInfoPacket(mplew);// 任務數據
        }
        if ((dbcharFlag & 0x2000000000L) != 0) { // 完成劇情書任務時角色的樣子
            int i1 = 0;
            mplew.writeShort(i1);
            for (int i = 0; i < i1; i++) {
                mplew.writeInt(0); // 任務ID
                AvatarLook__Decode(mplew, chr, false, false);
            }
        }
        if ((dbcharFlag & 0x80000) != 0) {
            short v316 = 0;
            mplew.writeShort(v316);
            for (int i = 0; i < v316; i++) {
                mplew.writeInt(0);
                mplew.writeShort(0);

            }
        }
        boolean unk = true;
        mplew.write(unk);
        if (unk) {
            if ((dbcharFlag & 0x8000000000L) != 0) {
                int i6 = 0;
                mplew.writeInt(i6);
                for (int i = 0; i < i6; i++) {
                    mplew.writeInt(0);
                    mplew.writeMapleAsciiString("");
                    // CSimpleStrMap::InitFromRawString
                }
            }
        }
        if ((dbcharFlag & 0x100000000000L) != 0) {
            int i2 = 0;
            mplew.writeInt(i2);
            for (int i = 0; i < i2; i++) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }
        if ((dbcharFlag & 0x200000) != 0) {
            if (MapleJob.is狂豹獵人(chr.getJob())) {
                addJaguarInfo(mplew, chr); // 狂豹的豹訊息
            }
        }
        if ((dbcharFlag & 0x80000000000L) != 0) {
            if (MapleJob.is神之子(chr.getJob())) {
                addZeroInfo(mplew, chr, new EnumMap<>(MapleZeroStat.class));// 神之子訊息
            }
        }
        if ((dbcharFlag & 0x4000000) != 0) {
            int GW_NpcShopBuyLimit = 0;
            mplew.writeShort(GW_NpcShopBuyLimit); // 應該是商店限購的已購買數量(如培羅德)
            for (int i = 0; i < GW_NpcShopBuyLimit; i++) {
                int v179 = 0;
                int v180 = 0;
                mplew.writeShort(v179); // 數量
                mplew.writeInt(v180); // NPCID
                if (v179 != 0 && v180 != 0) {
                    for (int j = 0; j < v179; j++) {
                        // dummyBLD.dwNPCID
                        mplew.writeInt(v180); // NPCID 應該是跟v180一致?
                        // dummyBLD.nItemIndex
                        mplew.writeShort(0); // 商品位置嗎
                        // dummyBLD.nItemID
                        mplew.writeInt(0); // 道具ID
                        // dummyBLD.nCount
                        mplew.writeShort(0); // 已經購買次數
                        // dummyBLD.ftDate
                        mplew.writeLong(0); // 時間吧
                    }
                }
            }
        }
        if ((dbcharFlag & 0x20000000) != 0) { // [60] Byte幻影複製技能訊息
            for (int i = 1; i < 6; i++) {
                addStolenSkills(mplew, chr, i, false); // 60
            }
        }
        if ((dbcharFlag & 0x10000000) != 0) { // [20] Byte幻影複製技能選擇訊息
            addChosenSkills(mplew, chr); // 20
        }
        if ((dbcharFlag & 0x80000000) != 0) {
            addAbilityInfo(mplew, chr);// 角色內在能力訊息
        }
        if ((dbcharFlag & 0x40000000000000L) != 0) {
            int v183 = 0;
            mplew.writeShort(v183);
            for (int i = 0; i < v183; i++) {
                mplew.writeInt(0);
                mplew.writeInt(0); // SoulCollection
            }
        }
        if ((dbcharFlag & 0x100000000L) != 0) {
            addHonorInfo(mplew, chr);// 內在能力聲望訊息
        }
        if ((dbcharFlag & 0x200000000000L) != 0) {
            addCoreAura(mplew, chr);
            mplew.write(1);
        }
        if ((dbcharFlag & 0x400000000000L) != 0) {
            int OX_System = 0;
            mplew.writeShort(OX_System); // for <short> length write 2 shorts
            for (int i = 0; i < OX_System; i++) {
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeMapleAsciiString("");
                mplew.write(0);
                mplew.writeLong(0);
                mplew.writeInt(0);
                mplew.writeMapleAsciiString("");
                mplew.write(0);
                mplew.write(0);
                mplew.writeLong(0);
                mplew.writeMapleAsciiString("");
            }
        }
        if ((dbcharFlag & 0x800000000000L) != 0) { // 經驗椅子的經驗
            int i8 = 0;
            mplew.writeShort(i8); // 個數
            for (int i = 0; i < i8; i++) {
                // CInPacket::DecodeBuffer 0x14
                mplew.writeInt(0);
                mplew.write(0);
                mplew.writeShort(0);
                mplew.write(0);
                mplew.writeInt(0); // 道具ID
                mplew.writeLong(0); // 0
            }
        }
        if ((dbcharFlag & 0x1000000000000L) != 0) {
            addRedLeafInfo(mplew, chr);
        }
        if ((dbcharFlag & 0x2000000000000L) != 0) {
            int i4 = 0;
            mplew.writeShort(i4);
            for (int i = 0; i < i4; i++) {
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeLong(0);
                mplew.writeLong(0);
                mplew.writeInt(0);
                int result = 0;
                mplew.writeInt(result);
                for (int j = 0; j < result; j++) {
                    mplew.writeInt(0);
                }
            }
        }
        if ((dbcharFlag & 0x200000000L) != 0) {// 3
            boolean b5 = true;
            mplew.writeBoolean(b5);// 1
            if (b5) {
                int v1 = 0;
                mplew.writeShort(v1);// 2
                for (int i = 0; i < v1; i++) {
                    mplew.writeShort(0);
                    int i5 = 0;
                    mplew.writeShort(0);
                    for (int j = 0; j < i5; j++) {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        // CharacterData::SetEntryRecord
                    }
                }
            } else {
                int i6 = 0;
                mplew.writeShort(i6);
                for (int i = 0; i < i6; i++) {
                    mplew.writeShort(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    // CharacterData::SetEntryRecord
                    // CharacterData::SetERModified
                }
            }
        }
        if ((dbcharFlag & 0x400000000L) != 0) { // ReturnEffectInfo::Decode
            int v3 = 0;
            mplew.write(v3);
            if (v3 > 0) {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, null);
                // nUsedUItemID
                mplew.writeInt(0);
            }
        }
        if ((dbcharFlag & 0x800000000L) != 0) {
            mplew.writeInt(MapleJob.is天使破壞者(chr.getJob()) ? 21173 : 0);
            mplew.writeInt(MapleJob.is天使破壞者(chr.getJob()) ? 37141 : 0);
            mplew.writeInt(MapleJob.is天使破壞者(chr.getJob()) ? 1051291 : 0);
            mplew.write(0);
            mplew.writeInt(-1);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if ((dbcharFlag & 0x20000000000000L) != 0) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeLong(DateUtil.getFileTimestamp(-2));
        }
        if ((dbcharFlag & 0x1000000000L) != 0) { // 進化系統訊息
            addElabEquip(mplew, chr);
        }
        if ((dbcharFlag & 0x20000000000L) != 0) {
            boolean v4 = false;
            mplew.write(v4);
            if (v4) {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, null);
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }
        if ((dbcharFlag & 0x40000000000L) != 0) {
            mplew.writeInt(0);
            mplew.writeLong(DateUtil.getFileTimestamp(-2));
            mplew.writeInt(0);
        }
        if ((dbcharFlag & 0x80000000000L) != 0) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeLong(DateUtil.getFileTimestamp(-2));
            mplew.writeInt(0xA);
        }
        if ((dbcharFlag & 0x800000000000000L) != 0) {
            int v3 = 0;
            mplew.writeInt(v3);
            if (v3 > 0) {
                for (int i = 0; i < v3; i++) {
                    mplew.writeInt(0);
                    mplew.write(0);
                    mplew.write(0);
                    mplew.write(0);
                }
            }
            mplew.writeInt(0);
            mplew.writeLong(0);
        }
        int k = 0;
        mplew.writeShort(k);
        for (int kk = 0; kk < k; kk++) {
            mplew.writeInt(0);
            mplew.writeMapleAsciiString("");
        }
        if ((dbcharFlag & 0x4000000000000L) != 0) {
            int i3 = 0;
            mplew.writeShort(i3);
            for (int i = 0; i < i3; i3++) {
                mplew.writeInt(0);
                mplew.writeMapleAsciiString("");
            }
        }
        UnkFunction5(mplew);
        if ((dbcharFlag & 0x1000000000000000L) != 0) {
            int i10 = 0;
            mplew.writeShort(i10);
            for (int i = 0; i < i10; i10++) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }
        if ((dbcharFlag & 0x2000000000000000L) != 0) {
            mplew.writeInt(chr.getVMatrixRecords().size());
            for (VMatrixRecord pRecord : chr.getVMatrixRecords()) {
                pRecord.encode(mplew);
            }
        }
        if ((dbcharFlag & 0x4000000000000000L) != 0) {
            mplew.writeInt(chr.getClient().getAccID());
            mplew.writeInt(chr.getId());
            mplew.writeInt(0);
            mplew.writeInt(-1);
            mplew.writeInt(Integer.MAX_VALUE);
            mplew.writeLong(DateUtil.getFileTimestamp(-2));
            mplew.writeLong(0);
            mplew.writeLong(0);
            int a2 = 0;
            mplew.writeInt(a2);
            for (int i = 0; i < a2; i++) {
                mplew.writeLong(0);
                //{
                mplew.writeInt(0);
                mplew.write(0);
                mplew.write(0);
                mplew.writeLong(0);
                mplew.writeMapleAsciiString("");
                //}
            }
            int v6 = 0;
            mplew.writeInt(v6);
            for (int i = 0; i < v6; i++) {
                mplew.writeInt(0);

                mplew.writeInt(0);
                mplew.write(0);
                mplew.writeLong(0);
            }
        }
        if ((dbcharFlag & 0x20) != 0) {
            int unkCount = 0;
            mplew.writeInt(unkCount);
            if (unkCount > 0) {
                for (int i = 0; i < unkCount; i++) {
                    mplew.writeLong(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeLong(0);
                }
            }
        }
        if ((dbcharFlag & 0x4000000000000L) != 0) { // 跟克梅勒茲航海有關
            boolean v190 = true;
            mplew.writeBoolean(v190);
            if (v190) {
                mplew.write(0);
                mplew.writeInt(1);
                mplew.writeInt(0);
                mplew.writeInt(100);
                mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
            }
            int v192 = 0;
            mplew.writeShort(v192);
            for (int i = 0; i < v192; i++) {
                mplew.write(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
            int v194 = 0;
            mplew.writeShort(v194);
            for (int i = 0; i < v194; i++) { // 航海的出售的道具
                mplew.writeInt(0); // 道具ID
                mplew.writeInt(0);
                mplew.writeLong(0);
            }
        }
        if ((dbcharFlag & 0x8000000000000L) != 0) {
            mplew.write(0);
        }
        if ((dbcharFlag & 0x10000000000000L) != 0) {
            int v5 = 0;
            mplew.writeInt(v5);
            for (int i = 0; i < v5; i++) {
                mplew.writeShort(0);
                mplew.writeShort(0);
            }
            int v8 = 0;
            mplew.writeInt(v8);
            for (int i = 0; i < v8; i++) {
                mplew.writeShort(0);
                mplew.writeInt(0);
            }
        }
        if ((dbcharFlag & 0x2000000) != 0) {
            mplew.write(0);
            if (false) {
                // for
                mplew.writeInt(0);
            }
        }
    }

    public static void SlotHyper(final MaplePacketLittleEndianWriter mplew) {
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
    }

    public static void UnkFunction(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getId());
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
    }

    public static void GW_MonsterBattleLadder_UserInfo(final MaplePacketLittleEndianWriter mplew) {
        // nWorldID
        mplew.writeInt(0);
        // dwAccountID
        mplew.writeInt(0);
        // dwCharacterID
        mplew.writeInt(0);
        // nPoint
        mplew.writeInt(0);
        // nWin
        mplew.writeInt(0);
        // nDefeat
        mplew.writeInt(0);
        // nDraw
        mplew.writeInt(0);
        // nCountBattle
        mplew.writeInt(0);
        // nCountPVP
        mplew.writeInt(0);
        // ftBPUpdateTime
        mplew.writeLong(0);
        // ftPVPUpdateTime
        mplew.writeLong(0);
        // ftRegisterDate
        mplew.writeLong(0);
    }

    public static void GW_MonsterBattleRankInfo(final MaplePacketLittleEndianWriter mplew) {
        // nType
        mplew.write(0);
        // nRank
        mplew.writeInt(0);
        // nWorldID
        mplew.write(0);
        // dwAccountID
        mplew.writeInt(0);
        // dwCharacterID
        mplew.writeInt(0);
        // nPoint
        mplew.writeInt(0);
        // dwMobID1
        mplew.writeInt(0);
        // dwMobID2
        mplew.writeInt(0);
        // dwMobID3
        mplew.writeInt(0);
        // sCharacterName
        mplew.writeMapleAsciiString("");
    }

    public static void UnkFunction5(final MaplePacketLittleEndianWriter mplew) {
        int result = 0;
        mplew.writeInt(result);
        for (int i = 0; i < result; i++) {
            mplew.writeInt(0);
            mplew.writeMapleAsciiString("");
        }
    }

    public static void UnkFunctin6(final MaplePacketLittleEndianWriter mplew) {
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
    }

    public static int getSkillBook(final int i) {
        switch (i) {
        case 1:
        case 2:
            return 4;
        case 3:
            return 3;
        case 4:
            return 2;
        }
        return 0;
    }

    public static void addAbilityInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeShort(chr.getInnerSkillSize());
        InnerSkillValueHolder[] innerSkills = chr.getInnerSkills();
        for (int i = 0; i < chr.getInnerSkillSize(); ++i) {
            InnerSkillValueHolder innerSkill = innerSkills[i];
            mplew.write(innerSkill == null ? 0 : innerSkill.getPosition());
            mplew.writeInt(innerSkill == null ? 0 : innerSkill.getSkillId()); // d 7000000 id ++, 71 = char cards
            mplew.write(innerSkill == null ? 0 : innerSkill.getSkillLevel()); // level
            mplew.write(innerSkill == null ? 0 : innerSkill.getRank()); // rank, C, B, A, and S
        }

    }

    public static void addHonorInfo(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getHonorLevel()); // 之前是聲望等級honor lvl
        mplew.writeInt(chr.getHonourExp()); // 之前是聲望經驗值,現在是聲望honor exp
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
    }

    public static void addElabEquip(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        MapleInventory iv = chr.getInventory(MapleInventoryType.ELAB);
        List<Item> equippedList = iv.newList();
        List<Item> esped = new ArrayList<>();
        for (Item item : equippedList) {
            if (item.getESPos() != 0) {
                esped.add(item);
            }
        }
        mplew.writeShort(esped.size());
        for (Item item : esped) {
            mplew.writeShort(item.getESPos() - 1);
            mplew.writeInt(item.getItemId());
            mplew.writeInt(item.getQuantity());
        }
        mplew.writeShort(iv.list().size());
        if (iv.list().size() > 0) {
            for (Item item : iv.list()) {
                mplew.writeShort(item.getPosition() - 1);
                mplew.writeInt(item.getItemId());
                mplew.writeInt(item.getQuantity());
            }
        }
    }

    public static void addCoreAura(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        MapleCoreAura aura = chr.getCoreAura();
        // mplew.writeInt(aura.getId()); //nvr change//176-
        // mplew.writeInt(chr.getId());
        mplew.writeInt(0);
        int level = chr.getSkillLevel(80001151) > 0 ? chr.getSkillLevel(80001151) : chr.getSkillLevel(1214);
        mplew.writeInt(level);
        mplew.writeInt(aura.getExpire());// timer
        mplew.writeInt(0);
        mplew.writeInt(0);

        mplew.writeInt(aura.getAtt());// wep att
        mplew.writeInt(aura.getDex());// dex
        mplew.writeInt(aura.getLuk());// luk
        mplew.writeInt(aura.getMagic());// magic att
        mplew.writeInt(aura.getInt());// int
        mplew.writeInt(aura.getStr());// str

        mplew.writeInt(0);
        mplew.writeInt(aura.getTotal());// max
        mplew.writeInt(0);
        mplew.writeInt(0);

        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis() + 86400000L));
        mplew.write(MapleJob.is蒼龍俠客(chr.getJob()) && MapleJob.is幻獸師(chr.getJob()) ? 1 : 0);
    }

    public static void addStolenSkills(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, int jobNum, boolean writeJob) {
        if (writeJob) {
            mplew.writeInt(jobNum);
        }
        int count = 0;
        if (chr.getStolenSkills() != null) {
            for (Pair<Integer, Boolean> sk : chr.getStolenSkills()) {
                if (MapleJob.getJobGrade(sk.left / 10000) == jobNum) {
                    mplew.writeInt(sk.left);
                    count++;
                    if (count >= GameConstants.getNumSteal(jobNum)) {
                        break;
                    }
                }
            }
        }
        while (count < GameConstants.getNumSteal(jobNum)) { // for now?
            mplew.writeInt(0);
            count++;
        }
    }

    public static void addChosenSkills(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        for (int i = 1; i <= 5; i++) {
            boolean found = false;
            if (chr.getStolenSkills() != null) {
                for (Pair<Integer, Boolean> sk : chr.getStolenSkills()) {
                    if (MapleJob.getJobGrade(sk.left / 10000) == i && sk.right) {
                        mplew.writeInt(sk.left);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                mplew.writeInt(0);
            }
        }
    }

    public static void addMonsterBookInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        if (chr.getMonsterBook().getSetScore() > 0) {
            chr.getMonsterBook().writeFinished(mplew);
        } else {
            chr.getMonsterBook().writeUnfinished(mplew);
        }

        mplew.writeInt(chr.getMonsterBook().getSet());
    }

    public static void CShopDlg_SetShopDlg(MaplePacketLittleEndianWriter mplew, int sid, MapleShop shop, MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        mplew.writeInt(0); // m_nSelectNpcItemID
        mplew.writeInt(sid); // m_dwNpcTemplateID
        mplew.writeInt(1); // m_nStarCoin[00 30 DC 1E]
        mplew.write(0); // Boolean
        mplew.writeInt(GameConstants.getCurrentDate());
        mplew.write(shop.getRanks().size() > 0);
        if (shop.getRanks().size() > 0) {
            mplew.write(shop.getRanks().size());
            for (Pair<Integer, String> s : shop.getRanks()) {
                mplew.writeInt(s.left);
                mplew.writeMapleAsciiString(s.right);
            }
        }
        mplew.writeInt(0); // m_nShopVerNo
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(shop.getItems().size() + c.getPlayer().getRebuy().size()); // nCount
        for (MapleShopItem item : shop.getItems()) {
            addShopItemInfo(mplew, item, shop, ii, null, c.getPlayer());
            if (shop.getRanks().size() > 0) {
                mplew.write(item.getRank() >= 0);
                if (item.getRank() >= 0) {
                    mplew.write(item.getRank());
                }
            }
            mplew.write(0);
        }
        byte rank = 0;
        for (Item i : c.getPlayer().getRebuy()) {
            addShopItemInfo(mplew, new MapleShopItem(i, rank), shop, ii, i, c.getPlayer());
            if (shop.getRanks().size() > 0) {
                mplew.write(0);
            }
            mplew.write(i == null ? 0 : 1);
            if (i != null) {
                PacketHelper.GW_ItemSlotBase_Decode(mplew, i);
            }
            rank++;
        }
    }

    /*
     * Categories: 0 - 標題 1 - 裝備 2 - 消耗 3 - 裝飾 4 - 其他 5 - 配方 6 - 卷軸 7 - 特殊 8 - 七週年 9
     * - 紐扣 10 - 入場券 11 - 材料 12 - 新楓之谷 13 - 運動會 14 - 楓核心 15 - 一日 16 - 海盜 80 - 喬 81 -
     * 海麗蜜 82 - 小龍 83 - 李卡司
     */
    public static void addShopItemInfo(MaplePacketLittleEndianWriter mplew, MapleShopItem item, MapleShop shop, MapleItemInformationProvider ii, Item i, MapleCharacter chr) {
        mplew.writeInt(item.getItemId()); // item.nItemID
        mplew.writeInt(item.getCategory()); // item.nTabIndex
        mplew.writeInt(item.getExpiration()); // // item.nItemPeriod 使用時限(單位分鐘)
        mplew.writeInt(ItemConstants.類型.裝備(item.getItemId()) && item.hasPotential() ? 1 : 0); // item.nPotentialGrade
        mplew.writeInt(item.getPrice()); // item.nPrice
        mplew.writeInt(item.getReqItem()); // item.nTokenItemID 貨幣道具
        mplew.writeInt(item.getReqItemQ()); // item.nTokenPrice 消耗貨幣數量
        mplew.writeInt(0); // item.nPointQuestID 點數道具
        mplew.writeInt(0); // item.nPointPrice 消耗點數數量
        mplew.writeInt(0); // item.nStarCoin
        boolean unk = true;
        mplew.write(unk);
        if (unk) {
            // Func {
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeMapleAsciiString("");
            mplew.writeInt(0);
            mplew.writeMapleAsciiString("");
            mplew.writeLong(0);
            mplew.writeLong(0);
            mplew.writeMapleAsciiString("");
            int nUnkSize = 0;
            mplew.writeInt(nUnkSize);
            for (int j = 0; j < nUnkSize; j++) {
                mplew.writeLong(0);
            }
            // }
        }
        mplew.writeInt(0); // item.nBuyLimit 允許購買次數
        mplew.writeInt(item.getMinLevel()); // item.nLevelLimited 購買等級限制
        mplew.writeShort(0); // item.nShowLevMin
        mplew.writeShort(0); // item.nShowLevMax

        // BuyLimit::DecodeResetInfo {
        int nType = 0;
        mplew.write(nType);
        if (nType == 1 || nType == 3 || nType == 4) {
            int nLimitSize = 0;
            mplew.writeInt(nLimitSize);
            for (int j = 0; j < nLimitSize; j++) {
                mplew.writeLong(0); // ft.dwLowDateTime
            }
        }
        // }

        mplew.write(0); // item.bWorldBlock
        mplew.writeLong(DateUtil.getFileTimestamp(-2L)); // item.ftSellStart
        mplew.writeLong(DateUtil.getFileTimestamp(-1L)); // item.ftSellEnd
        mplew.writeInt(0); // item.nQuestID
        mplew.writeShort(0);
        mplew.write(false);
        mplew.writeInt(0); // item.nQuestExID
        mplew.writeMapleAsciiString(""); // item.sQuestExKey
        mplew.writeInt(0); // item.nQuestExValue

        int slotMax = ii.getSlotMax(item.getItemId());
        if (ItemConstants.類型.可充值道具(item.getItemId())) {
            mplew.writeLong(Double.doubleToLongBits(Math.max(ii.getUnitPrice(item.getItemId()), 0.0))); // item.dUnitPrice
            // mplew.writeShort(1);
            // mplew.writeInt(0);
            // mplew.writeShort(BitTools.doubleToShortBits(1.0 +
            // Math.max(ii.getUnitPrice(item.getItemId()), 0.0)));
        } else {
            int quantity = item.getQuantity() == 0 ? slotMax : item.getQuantity();
            mplew.writeShort(quantity); // item.nQuantity 購買數量
            slotMax = quantity > 1 ? 1 : item.getBuyable() == 0 ? slotMax : item.getBuyable(); // 可購買數量
        }
        mplew.writeShort(slotMax); // item.nMaxPerSlot

        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString("1900010100");
        mplew.writeMapleAsciiString("2079010100");

        // { 商店親密度? Start
        for (int j = 0; j < 4; j++) {
            mplew.writeInt(0); // red leaf high price probably
        }
        int idarr[] = new int[] { 9410165, 9410166, 9410167, 9410168, 9410198 };
        for (int k = 0; k < 5; k++) {
            mplew.writeInt(idarr[k]);
            mplew.writeInt(chr.getFriendShipPoints()[k]);
        }
        // } 商店親密度? End
    }

    public static void addJaguarInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(chr.getIntNoRecord(GameConstants.JAGUAR));
        for (int i = 0; i < 5; i++) {
            mplew.writeInt(33001007 + i);
        }
    }

    public static void addZeroInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, Map<MapleZeroStat, Integer> mystats) {
        int mask = 0;
        if (mystats != null) {
            for (MapleZeroStat statupdate : mystats.keySet()) {
                mask |= statupdate.getValue();
            }
        }
        mplew.writeShort(mask);
        if ((mask & MapleZeroStat.bIsBeta.getValue()) != 0) {
            mplew.write(mystats.get(MapleZeroStat.bIsBeta).byteValue()); // bool
        }
        if ((mask & MapleZeroStat.nSubHP.getValue()) != 0) {
            mplew.writeInt(mystats.get(MapleZeroStat.nSubHP).byteValue());
        }
        if ((mask & MapleZeroStat.nSubMP.getValue()) != 0) {
            mplew.writeInt(mystats.get(MapleZeroStat.nSubMP).byteValue());
        }
        if ((mask & MapleZeroStat.nSubSkin.getValue()) != 0) {
            mplew.write(mystats.get(MapleZeroStat.nSubSkin).byteValue());
        }
        if ((mask & MapleZeroStat.nSubHair.getValue()) != 0) {
            mplew.writeInt(mystats.get(MapleZeroStat.nSubHair).byteValue());
        }
        if ((mask & MapleZeroStat.nSubFace.getValue()) != 0) {
            mplew.writeInt(mystats.get(MapleZeroStat.nSubFace).byteValue());
        }
        if ((mask & MapleZeroStat.nSubMHP.getValue()) != 0) {
            mplew.writeInt(mystats.get(MapleZeroStat.nSubMHP).byteValue());
        }
        if ((mask & MapleZeroStat.nSubMMP.getValue()) != 0) {
            mplew.writeInt(mystats.get(MapleZeroStat.nSubMMP).byteValue());
        }
        if ((mask & MapleZeroStat.dbcharZeroLinkCashPart.getValue()) != 0) {
            mplew.writeInt(mystats.get(MapleZeroStat.dbcharZeroLinkCashPart).byteValue());
        }
        if ((mask & MapleZeroStat.nMixBaseHair.getValue()) != 0) {
            mplew.writeInt(0); // nMixBaseHairColor
            mplew.writeInt(0); // nMixAddHairColor
            mplew.writeInt(0); // nMixHairBaseProb
        }
    }

    public static void addFarmInfo(MaplePacketLittleEndianWriter mplew, MapleClient c, int idk) {
        mplew.writeMapleAsciiString(c.getFarm().getName());
        mplew.writeInt(c.getFarm().getWaru());
        mplew.writeInt(c.getFarm().getLevel());
        mplew.writeInt(c.getFarm().getExp());
        mplew.writeInt(c.getFarm().getAestheticPoints());
        mplew.writeInt(0); // gems

        mplew.write((byte) idk);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(1);
    }

    public static void addLuckyLogoutInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(0);
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(0);
        }
    }

    public static void addRedLeafInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        int idarr[] = new int[] { 9410165, 9410166, 9410167, 9410168, 9410198 };
        mplew.writeInt(chr.getClient().getAccID());
        mplew.writeInt(chr.getId());
        int size = 5;
        mplew.writeInt(size);
        mplew.writeInt(0);
        // CInPacket::DecodeBuffer 0x28
        for (int i = 0; i < size; i++) {
            mplew.writeInt(idarr[i]);
            mplew.writeInt(chr.getFriendShipPoints()[i]);
        }
    }

    public static void addPartTimeJob(MaplePacketLittleEndianWriter mplew, PartTimeJob parttime) {
        mplew.write(parttime.getJob());
        if (parttime.getJob() > 0 && parttime.getJob() <= 5) {
            mplew.writeReversedLong(parttime.getTime());
        } else {
            mplew.writeReversedLong(DateUtil.getFileTimestamp(-2));
        }
        mplew.writeInt(parttime.getReward());
        mplew.write(parttime.getReward() > 0);
    }

    public static void SecondaryStat_DecodeForLocal(MaplePacketLittleEndianWriter mplew, int buffid, int bufflength, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect, MapleCharacter chr) {
        PacketHelper.writeBuffMask(mplew, statups);

        // 處理部分BUFF是動態數值
        addBuffInfo(MapleBuffStat.STR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.INT, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DEX, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.LUK, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PAD, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PDD, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MAD, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ACC, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EVA, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EVAR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Craft, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Speed, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Jump, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EMHP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EMMP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EPAD, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EMAD, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EPDD, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MagicGuard, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DarkSight, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Booster, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PowerGuard, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Guard, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MaxHP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MaxMP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Invincible, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SoulArrow, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Stun, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Shock, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Poison, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Seal, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Darkness, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ComboCounter, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_82, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_83, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_84, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.WeaponCharge, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ElementalCharge, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HolySymbol, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MesoUp, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ShadowPartner, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PickPocket, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MesoGuard, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Thaw, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Weakness, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.WeaknessMdamage, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Curse, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Slow, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.TimeBomb, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BuffLimit, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Team, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DisOrder, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Thread, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Morph, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Ghost, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Regen, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BasicStatUp, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Stance, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SharpEyes, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ManaReflection, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Attract, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Magnet, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MagnetArea, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_251, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_252, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_253, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_254, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_255, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NoBulletConsume, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.StackBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Trinity, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Infinity, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AdvancedBless, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IllusionStep, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Blind, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Concentration, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BanMap, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MaxLevelBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Barrier, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DojangShield, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ReverseInput, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MesoUpByItem, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_111, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_112, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ItemUpByItem, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RespectPImmune, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RespectMImmune, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DefenseAtt, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DefenseState, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DojangBerserk, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DojangInvincible, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SoulMasterFinal, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.WindBreakerFinal, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ElementalReset, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HideAttack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EventRate, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ComboAbilityBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ComboDrain, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ComboBarrier, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PartyBarrier, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BodyPressure, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ExpBuffRate, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.StopPortion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.StopMotion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Fear, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_137, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MagicShield, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MagicResistance, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SoulStone, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Flying, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NewFlying, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NaviFlying, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Frozen, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Frozen2, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Web, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Enrage, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NotDamaged, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FinalCut, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HowlingAttackDamage, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BeastFormDamageUp, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Dance, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.OnCapsule, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HowlingCritical, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HowlingMaxMP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HowlingDefence, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HowlingEvasion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Conversion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Sneak, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Mechanic, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DrawBack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BeastFormMaxHP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Dice, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BlessingArmor, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BlessingArmorIncPAD, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DamR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.TeleportMasteryOn, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CombatOrders, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Beholder, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DispelItemOption, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DispelItemOptionByField, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Inflation, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.OnixDivineProtection, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Bless, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Explosion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DarkTornado, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncMaxHP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncMaxMP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PVPDamage, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PVPDamageSkill, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PvPScoreBonus, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PvPInvincible, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PvPRaceEffect, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HolyMagicShell, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.InfinityForce, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AmplifyDamage, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KeyDownTimeIgnore, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MasterMagicOn, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AsrR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AsrRByItem, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.TerR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DamAbsorbShield, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Roulette, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Event, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SpiritLink, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CriticalBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DropRate, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PlusExpRate, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ItemInvincible, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ItemCritical, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ItemEvade, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Event2, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.VampiricTouch, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DDR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncTerR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncAsrR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DeathMark, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PainMark, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.UsefulAdvancedBless, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Lapidification, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.VampDeath, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.VampDeathSummon, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.VenomSnake, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CarnivalAttack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CarnivalDefence, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CarnivalExp, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SlowAttack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PyramidEffect, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HollowPointBullet, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KeyDownMoving, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KeyDownAreaMoving, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CygnusElementSkill, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IgnoreTargetDEF, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Invisible, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ReviveOnce, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AntiMagicShell, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EnrageCr, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EnrageCrDamMin, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BlessOfDarkness, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.LifeTidal, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Judgement, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DojangLuckyBonus, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HitCriDamR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Larkness, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SmashStack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ReshuffleSwitch, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SpecialAction, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ArcaneAim, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.StopForceAtomInfo, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SoulGazeCriDamR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SoulRageCount, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PowerTransferGauge, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AffinitySlug, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SoulExalt, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HiddenPieceOn, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BossShield, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MobZoneState, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.GiveMeHeal, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.TouchMe, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Contagion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ComboUnlimited, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IgnorePCounter, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IgnoreAllCounter, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IgnorePImmune, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IgnoreAllImmune, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FinalJudgement, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_289, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KnightsAura, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IceAura, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FireAura, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.VengeanceOfAngel, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HeavensDoor, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Preparation, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BullsEye, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncEffectHPPotion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncEffectMPPotion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SoulMP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FullSoulMP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SoulSkillDamageUp, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BleedingToxin, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IgnoreMobDamR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Asura, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_301, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FlipTheCoin, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.UnityOfPower, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Stimulate, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ReturnTeleport, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CapDebuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DropRIncrease, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IgnoreMobpdpR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BdR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Exceed, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DiabolikRecovery, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FinalAttackProp, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ExceedOverload, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DevilishPower, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.OverloadCount, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BuckShot, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FireBomb, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HalfstatByDebuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SurplusSupply, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SetBaseDamage, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AmaranthGenerator, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.StrikerHyperElectric, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EventPointAbsorb, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.EventAssemble, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.StormBringer, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ACCR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DEXR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Albatross, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Translucence, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PoseType, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.LightOfSpirit, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ElementSoul, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.GlimmeringTime, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Restoration, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ComboCostInc, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ChargeBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.TrueSight, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CrossOverChain, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ChillingStep, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Reincarnation, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DotBasedBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BlessEnsenble, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ExtremeArchery, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.QuiverCatridge, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AdvancedQuiver, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.UserControlMob, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ImmuneBarrier, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ArmorPiercing, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ZeroAuraStr, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ZeroAuraSpd, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CriticalGrowing, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.QuickDraw, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BowMasterConcentration, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.TimeFastABuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.TimeFastBBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.GatherDropR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AimBox2D, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_368, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncMonsterBattleCaptureRate, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.CursorSniping, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DebuffTolerance, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DotHealHPPerSecond, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DotHealMPPerSecond, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SpiritGuard, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PreReviveOnce, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SetBaseDamageByBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.LimitMP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ReflectDamR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ComboTempest, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MHPCutR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MMPCutR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SelfWeakness, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ElementDarkness, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FlareTrick, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Ember, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Dominion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SiphonVitality, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DarknessAscension, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BossWaitingLinesBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DamageReduce, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ShadowServant, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ShadowIllusion, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AddAttackCount, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ComplusionSlant, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.JaguarSummoned, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.JaguarCount, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SSFShootingAttack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DevilCry, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ShieldAttack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BMageAura, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DarkLighting, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AttackCountX, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BMageDeath, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BombTime, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NoDebuff, mplew, buffid, bufflength, statups);
//        addBuffInfo(MapleBuffStat.XenonAegisSystem, mplew, buffid, bufflength, statups);
//        addBuffInfo(MapleBuffStat.AngelicBursterSoulSeeker, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HiddenPossession, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NightWalkerBat, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NightLordMark, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.WizardIgnite, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_413, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_414, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_418, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_419, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BattlePvP_Helena_Mark, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BattlePvP_Helena_WindSpirit, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BattlePvP_LangE_Protection, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BattlePvP_LeeMalNyun_ScaleUp, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BattlePvP_Revive, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PinkbeanAttackBuff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RandAreaAttack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BattlePvP_Mike_Shield, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BattlePvP_Mike_Bugle, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PinkbeanRelax, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PinkbeanYoYoStack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.狂風呼嘯, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NextAttackEnhance, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AranBeyonderDamAbsorb, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AranCombotempastOption, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.NautilusFinalAttack, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ViperTimeLeap, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RoyalGuardState, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RoyalGuardPrepare, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MichaelSoulLink, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.MichaelStanceLink, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.TriflingWhimOnOff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AddRangeOnOff, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KinesisPsychicPoint, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KinesisPsychicOver, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KinesisPsychicShield, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KinesisIncMastery, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KinesisPsychicEnergeShield, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BladeStance, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DebuffActiveSkillHPCon, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.DebuffIncHP, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BowMasterMortalBlow, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AngelicBursterSoulResonance, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Fever, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IgnisRore, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RpSiksin, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AnimalChange, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_441, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_442, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_438, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncMaxDamage, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_439, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_436, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.BLACKHEART_CURSE, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AURA_BOOST, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HayatoStance, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HayatoMPR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HayatoStanceBonus, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.WILLOW_DODGE, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.SHIKIGAMI, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HayatoPAD, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HayatoHPR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.JINSOKU, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HayatoCr, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FireBarrier, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.ChangeFoxMan, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HAKU_BLESS, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.KannaBDR, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.PLAYERS_BUFF435, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Fever, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.COUNTERATTACK, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_444, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_159, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_529, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_531, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_532, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_533, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_534, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_535, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_536, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_537, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_539, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_540, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_544, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.FixCoolTime, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IncMobRateDummy, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AdrenalinBoost, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AranSmashSwing, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AranDrain, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.AranBoostEndHunt, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.HiddenHyperLinkMaximization, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RWCylinder, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RWCombination, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RWMagnumBlow, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RWBarrier, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RWBarrierHeal, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RWMaximizeCannon, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.RWOverHeat, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.UsingScouter, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Stigma, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_500, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_501, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_502, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_503, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_504, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_505, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.元氣覺醒, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.能量爆炸, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_508, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_509, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_510, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.交換攻擊, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.聖靈祈禱, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_513, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_514, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_515, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_417, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_516, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.散式投擲, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_518, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_519, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.滅殺刃影, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_522, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.惡魔狂亂, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_524, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_525, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_526, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_554, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_555, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_556, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_557, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_446, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_552, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_553, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_447, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_550, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_549, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.Cyclone, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_551, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_554, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_542, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_543, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_555, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_541, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_542, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_543, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_559, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_161, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_560, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_561, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_562, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_563, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_564, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_565, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_566, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_567, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_568, mplew, buffid, bufflength, statups);
        addBuffInfo(MapleBuffStat.IDA_BUFF_569, mplew, buffid, bufflength, statups);

        if (statups.containsKey(MapleBuffStat.SoulMP)) {
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.SoulMP).xOption);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.FullSoulMP)) {
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.FullSoulMP).xOption);
        }
        int nBuffForSpecSize = 0;
        mplew.writeShort(nBuffForSpecSize);
        for (int i = 0; i < nBuffForSpecSize; ++i) {
            mplew.writeInt(0); // dwItemID
            mplew.write(0); // bEnable
        }
        if (statups.containsKey(MapleBuffStat.HayatoStance)) {
            mplew.writeInt(statups.containsKey(MapleBuffStat.HayatoStanceBonus) ? 41110008 : 0);
        }
        mplew.write(statups.getOrDefault(MapleBuffStat.DefenseAtt, 0)); // DefenseAtt
        mplew.write(statups.getOrDefault(MapleBuffStat.DefenseState, 0)); // DefenseState
        mplew.write(statups.containsKey(MapleBuffStat.PoseType) ? 7 : 0);
        mplew.writeInt(0);
        if (statups.containsKey(MapleBuffStat.Dice)) {
            for (int j = 0; j < 22; ++j) {
                mplew.writeInt(0);
            }
        }
        if (statups.containsKey(MapleBuffStat.KeyDownMoving)) {
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.KillingPoint)) {
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.PinkbeanRollingGrade)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.Judgement)) {
            switch (statups.get(MapleBuffStat.Judgement)) {
            case 1:
            case 2:
            case 4:
                mplew.writeInt(effect.getSourceId() == 20031209 ? 10 : 20);
                break;
            case 3:
                mplew.writeInt(2020); // 2020 <== 抗性20%, 屬性20%
                break;
            default:
                mplew.writeInt(0);
                break;
            }
        }
        if (statups.containsKey(MapleBuffStat.StackBuff)) {
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.Trinity)) {
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.ElementalCharge)) {
            int value = statups.get(MapleBuffStat.ElementalCharge);
            mplew.write(value / 2);
            mplew.writeShort(value * 4);
            mplew.write(value);
            mplew.write(value);
        }
        if (statups.containsKey(MapleBuffStat.LifeTidal)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.AntiMagicShell)) {
            int value = statups.get(MapleBuffStat.AntiMagicShell);
            // 魔力護盾
            mplew.write(buffid == 27111004 ? 0 : value);
        }
        if (statups.containsKey(MapleBuffStat.Larkness)) {
            int ct = (int) System.currentTimeMillis();
            int[] val;
            switch (buffid) {
            case 20040216:
                val = new int[] { 20040216, 0 };
                break;
            case 20040217:
                val = new int[] { 20040217, 0 };
                break;
            case 20040219:
                val = new int[] { 20040216, 20040217 };
                break;
            case 20040220:
                val = new int[] { 20040217, 20040216 };
                break;
            default:
                val = new int[] { 0, 0 };
                break;
            }
            for (int k = 0; k < 2; ++k) {
                mplew.writeInt(val[k]);
                mplew.writeInt(ct);
            }
            mplew.writeInt(chr.getDarkGauge()); // 暗值
            mplew.writeInt(chr.getLightGauge()); // 光值
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IgnoreTargetDEF)) {
            if (effect.getSourceId() == 15001022) {
                mplew.writeInt(statups.get(MapleBuffStat.IgnoreTargetDEF) / 5);
            } else {
                mplew.writeInt(0);
            }
        }
        if (statups.containsKey(MapleBuffStat.StopForceAtomInfo)) {
            chr.getTempstats().pStopForceAtom.Encode(mplew);
        }
        if (statups.containsKey(MapleBuffStat.SmashStack)) {
            int comboType = 0;
            if (effect.getCombo() >= 100) {
                comboType = 1;
            } else if (effect.getCombo() >= 300) {
                comboType = 2;
            }
            mplew.writeInt(comboType);
        }
        if (statups.containsKey(MapleBuffStat.MobZoneState)) {
            List<Integer> ss = new ArrayList<>();
            ss.add(1);
            ss.add(1);
            ss.add(0); // 如果 value <= 0 結束

            for (int s : ss) {
                mplew.writeInt(s);
            }
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_161)) {
            int size = 0;
            mplew.writeInt(size);
            for (int i = 0; i < size; i++) {
                mplew.writeInt(0);
            }
        }
        if (statups.containsKey(MapleBuffStat.Slow)) {
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.Slow).bOption);
        }
        if (statups.containsKey(MapleBuffStat.IceAura)) {
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.IceAura).bOption);
        }
        if (statups.containsKey(MapleBuffStat.KnightsAura)) {
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.KnightsAura).bOption);
        }
        if (statups.containsKey(MapleBuffStat.IgnoreMobpdpR)) {
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.IgnoreMobpdpR).bOption);
        }
        if (statups.containsKey(MapleBuffStat.BdR)) {
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.BdR).bOption);
        }
        if (statups.containsKey(MapleBuffStat.DropRIncrease)) {
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.DropRIncrease).xOption);
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.DropRIncrease).bOption);
        }
        if (statups.containsKey(MapleBuffStat.PoseType)) {
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.PoseType).bOption);
        }
        if (statups.containsKey(MapleBuffStat.Beholder)) {
            boolean found = false;
            for (MapleSummon ms : chr.getMap().getAllSummonsThreadsafe()) {
                if (ms.getOwner().equals(chr)) {
                    mplew.writeInt(ms.getControl() ? 1311013 : 1301013);
                    mplew.writeInt(ms.getScream() ? 1311014 : 0);
                    found = true;
                    break;
                }
            }
            if (!found) {
                mplew.writeInt(1301013);
                mplew.writeInt(0);
            }
        }
        if (statups.containsKey(MapleBuffStat.CrossOverChain)) {
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.CrossOverChain).xOption);
        }
        if (statups.containsKey(MapleBuffStat.Reincarnation)) {
            mplew.writeInt(chr.getKillCount());
        }
        if (statups.containsKey(MapleBuffStat.ExtremeArchery)) {
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.ExtremeArchery).bOption);
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.ExtremeArchery).xOption);
        }
        if (statups.containsKey(MapleBuffStat.QuiverCatridge)) {
            mplew.writeInt(chr.getmod());
        }
        if (statups.containsKey(MapleBuffStat.ImmuneBarrier)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.ZeroAuraStr)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.ZeroAuraSpd)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.ArmorPiercing)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SharpEyes)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.AdvancedBless)) {
            mplew.writeInt(0);
        }
//        if (statups.containsKey(MapleBuffStat.DebuffTolerance)) {
//            mplew.writeInt(0);
//        }
        if (statups.containsKey(MapleBuffStat.DotHealHPPerSecond)) {
            mplew.writeInt(effect.info.get(MapleStatInfo.time));
        }
        if (statups.containsKey(MapleBuffStat.DotHealMPPerSecond)) {
            mplew.writeInt(effect.info.get(MapleStatInfo.time));
        }
        if (statups.containsKey(MapleBuffStat.SpiritGuard)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_446)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.AddAttackCount)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.ShieldAttack)) {
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.SSFShootingAttack)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.BMageAura)) {
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.BMageAura).xOption);
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.BMageAura).sOption);
        }
        if (statups.containsKey(MapleBuffStat.BattlePvP_Helena_Mark)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.PinkbeanAttackBuff)) {
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.PinkbeanAttackBuff).xOption);
        }
        if (statups.containsKey(MapleBuffStat.RoyalGuardState)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.MichaelSoulLink)) {
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.MichaelSoulLink).xOption);
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.MichaelSoulLink).sOption);
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.MichaelSoulLink).cOption);
            mplew.writeInt(chr.getTempstats().getStatOption(MapleBuffStat.MichaelSoulLink).yOption);
        }
        if (statups.containsKey(MapleBuffStat.AdrenalinBoost)) {
            mplew.write(chr.getTempstats().getStatOption(MapleBuffStat.AdrenalinBoost).sOption);
        }
        if (statups.containsKey(MapleBuffStat.RWCylinder)) {
            mplew.write(0);
            mplew.writeShort(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_559)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.RWMagnumBlow)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.RWBarrier)) {
            mplew.writeShort(0);
            mplew.write(0);
        }

        mplew.writeInt(0);

        if (statups.containsKey(MapleBuffStat.BladeStance)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.DarkSight)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_500)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_254)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.CriticalGrowing)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.Ember)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.PickPocket)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_417)) {
            mplew.writeShort(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_418)) {
            mplew.writeShort(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_419)) {
            mplew.writeShort(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_518)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.VampDeath)) {
            mplew.writeInt(0);
        }

        for (TSIndex pIndex : TSIndex.values()) {
            if (statups.containsKey(TSIndex.get_CTS_from_TSIndex(pIndex.getIndex()))) {
                chr.getTempstats().getStatOption(TSIndex.get_CTS_from_TSIndex(pIndex.getIndex())).encode(mplew);
            }
        }
        // SecondaryStat_DecodeIndieTempStat(mplew, buffid, statups, chr);
        for (MapleBuffStat buffStat : statups.keySet()) {
            if (buffStat.isIndie()) {
                chr.getTempstats().encodeIndieTempStat(mplew, buffStat);
            }
        }

        if (statups.containsKey(MapleBuffStat.RWMovingEvar)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_550)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_553)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_253)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_554)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_551)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_548)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.NewFlying)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.RideVehicleExpire)) {
            // function sub_9028B0
            mplew.write(1);
            // function sub_9028B0
            mplew.write(1);
        }

        if (statups.containsKey(MapleBuffStat.COUNT_PLUS1)) {
            mplew.write(0);
            // function sub_9028B0
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_530)) {
            // function sub_9028B0
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_159)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.COUNT_PLUS1)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_542)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_543)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_544)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.IDA_BUFF_539)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
    }

    public static void addBuffInfo(MapleBuffStat stat, MaplePacketLittleEndianWriter mplew, int buffid, int bufflength, Map<MapleBuffStat, Integer> statups) {
        if (statups.containsKey(stat)) {
            boolean isEnDecode4Byte = MapleBuffStat.SecondaryStat_EnDecode4Byte(stat);
            if (isEnDecode4Byte) {
                mplew.writeInt(statups.get(stat));
            } else {
                mplew.writeShort(statups.get(stat));
            }
            mplew.writeInt(buffid);
            mplew.writeInt(bufflength);
        }
    }

    // public static void
    // SecondaryStat_DecodeIndieTempStat(MaplePacketLittleEndianWriter mplew, int
    // buffid, Map<MapleBuffStat, Integer> statups, MapleCharacter chr) {
    // for (Map.Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
    // if (stat.getKey().isIndie()) {
    // CWvsContext.BuffPacket.IndieTempStat_Decode(mplew,
    // chr.getBuffStatValueHolders(stat.getKey()), buffid);
    // }
    // }
    // }
    public static <E extends BuffStat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup) {
        writeSingleMask(mplew, statup, GameConstants.MAX_BUFFSTAT);
    }

    public static <E extends BuffStat> void writeSingleMobMask(MaplePacketLittleEndianWriter mplew, E statup) {
        writeSingleMask(mplew, statup, GameConstants.MAX_MOBSTAT);
    }

    public static <E extends BuffStat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup,
            int maxMask) {
        for (int i = 0; i < maxMask; i++) {
            mplew.writeInt(i == statup.getPosition() ? statup.getValue() : 0);
        }
    }

    public static <E extends BuffStat> void writeMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups) {
        writeMask(mplew, statups, GameConstants.MAX_BUFFSTAT);
    }

    public static <E extends BuffStat> void writeMobMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups) {
        writeMask(mplew, statups, GameConstants.MAX_MOBSTAT);
    }

    public static <E extends BuffStat> void writeMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups,
            int maxMask) {
        int[] mask = new int[maxMask];
        for (BuffStat statup : statups) {
            mask[(statup.getPosition())] |= statup.getValue();
        }
        for (int i = 0; i < mask.length; i++) {
            mplew.writeInt(mask[i]);
        }
    }

    public static <E extends BuffStat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Collection<Pair<E, Integer>> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (Pair<E, Integer> statup : statups) {
            mask[statup.left.getPosition()] |= statup.left.getValue();
        }
        for (int i = 0; i < mask.length; i++) {
            mplew.writeInt(mask[i]);
        }
    }

    public static <E extends BuffStat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Map<E, Integer> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (BuffStat statup : statups.keySet()) {
            mask[(statup.getPosition())] |= statup.getValue();
        }
        for (int i = 0; i < mask.length; i++) {
            mplew.writeInt(mask[i]);
        }
    }

    public static class CS_COMMODITY {

        public static void DecodeModifiedData(MaplePacketLittleEndianWriter mplew, CashModItem csMod) {
            int[] mask = new int[2];
            for (CashItemFlag cf : csMod.flags) {
                mask[cf.getPosition()] |= cf.getValue();
            }
            for (int i = 0; i < mask.length; i++) {
                mplew.writeInt(mask[i]);
            }

            // [0x1]nItemId
            if (csMod.flags.contains(CashItemFlag.ItemId)) {
                mplew.writeInt(csMod.getItemId());
            }
            // [0x2]nCount
            if (csMod.flags.contains(CashItemFlag.Count)) {
                mplew.writeShort(csMod.getCount());
            }
            // [0x10]nPriority
            if (csMod.flags.contains(CashItemFlag.Priority)) {
                mplew.write(csMod.getPriority());
            }
            // [0x4]nPrice
            if (csMod.flags.contains(CashItemFlag.Price)) {
                mplew.writeInt(csMod.getPrice());
            }
            // [0x10000000]nOriginalPrice
            if (csMod.flags.contains(CashItemFlag.OriginalPrice)) {
                mplew.writeInt(0);
            }
            // [0x2000000]nToken
            if (csMod.flags.contains(CashItemFlag.Token)) {
                mplew.writeInt(0);
            }
            // [0x8]bBonus
            if (csMod.flags.contains(CashItemFlag.Bonus)) {
                mplew.write(false);
            }
            // [0x1]bZero
            if (csMod.flags.contains(CashItemFlag.Zero)) {
                mplew.write(false);
            }
            // [0x20]nPeriod
            if (csMod.flags.contains(CashItemFlag.Period)) {
                mplew.writeShort(csMod.getPeriod());
            }
            // [0x20000]nReqPOP
            if (csMod.flags.contains(CashItemFlag.ReqPOP)) {
                mplew.writeShort(0);
            }
            // [0x40000]nReqLEV
            if (csMod.flags.contains(CashItemFlag.ReqLEV)) {
                mplew.writeShort(0);
            }
            // [0x40]nMaplePoint
            if (csMod.flags.contains(CashItemFlag.MaplePoint)) {
                mplew.writeInt(0);
            }
            // [0x80]nMeso
            if (csMod.flags.contains(CashItemFlag.Meso)) {
                mplew.writeInt(0);
            }
            // [0x100]bForPremiumUser
            if (csMod.flags.contains(CashItemFlag.ForPremiumUser)) {
                mplew.write(false);
            }
            // [0x200]nCommodityGender
            if (csMod.flags.contains(CashItemFlag.CommodityGender)) {
                mplew.write(0);
            }
            // [0x400]bOnSale
            if (csMod.flags.contains(CashItemFlag.OnSale)) {
                mplew.write(csMod.isOnSale());
            }
            // [0x800]nClass
            if (csMod.flags.contains(CashItemFlag.Class)) {
                mplew.write(csMod.getClass_());
            }
            // [0x1000]nLimit
            if (csMod.flags.contains(CashItemFlag.Limit)) {
                mplew.write(0);
            }
            // [0x2000]nPbCash
            if (csMod.flags.contains(CashItemFlag.PbCash)) {
                mplew.writeShort(0);
            }
            // [0x4000]nPbPoint
            if (csMod.flags.contains(CashItemFlag.PbPoint)) {
                mplew.writeShort(0);
            }
            // [0x8000]nPbGift
            if (csMod.flags.contains(CashItemFlag.PbGift)) {
                mplew.writeShort(0);
            }
            // [0x10000]aPackageSN
            if (csMod.flags.contains(CashItemFlag.PackageSN)) {
                List<Integer> pack = CashItemFactory.getInstance().getPackageItems(csMod.getSN());
                if (pack == null) {
                    mplew.write(0);
                } else {
                    mplew.write(pack.size());
                    pack.forEach(mplew::writeInt);
                }
            }
            // [0x80000]TermStart
            if (csMod.flags.contains(CashItemFlag.TermStart)) {
                mplew.writeInt(0);
            }
            // [0x100000]TermEnd
            if (csMod.flags.contains(CashItemFlag.TermEnd)) {
                mplew.writeInt(0);
            }
            // [0x200000]bRefundable
            if (csMod.flags.contains(CashItemFlag.Refundable)) {
                mplew.write(false);
            }
            // [0x400000]bBombSale
            if (csMod.flags.contains(CashItemFlag.BombSale)) {
                mplew.write(false);
            }
            // [0x800000]
            if (csMod.flags.contains(CashItemFlag.CategoryInfo)) {
                mplew.write(0); // nForcedCategory
                mplew.write(0); // nForcedSubCategory
            }
            // [0x1000000]nWorldLimit
            if (csMod.flags.contains(CashItemFlag.WorldLimit)) {
                mplew.write(0);
            }
            // [0x4000000]nLimitMax
            if (csMod.flags.contains(CashItemFlag.LimitMax)) {
                mplew.write(0);
            }
            // [0x8000000]nCheckQuestID
            if (csMod.flags.contains(CashItemFlag.CheckQuestID)) {
                mplew.writeInt(0);
            }
            // [0x20000000]bDiscount
            if (csMod.flags.contains(CashItemFlag.Discount)) {
                mplew.write(0);
            }
            // [0x40000000]dDiscountRate
            if (csMod.flags.contains(CashItemFlag.DiscountRate)) {
                mplew.writeLong(0);
            }
            // [0x80000000]
            if (csMod.flags.contains(CashItemFlag.MileageInfo)) {
                mplew.write(0); // nMileageRate
                mplew.write(false); // bOnlyMileage
            }
            // [0x2]nCheckQuestID
            if (csMod.flags.contains(CashItemFlag.CheckQuestID2)) {
                mplew.writeInt(0);
            }
            // [0x4]
            if (csMod.flags.contains(CashItemFlag.UNK34)) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
            // [0x8]
            if (csMod.flags.contains(CashItemFlag.UNK35)) {
                mplew.write(0);
            }
            // [0x10]
            if (csMod.flags.contains(CashItemFlag.UNK36)) {
                mplew.write(0);
            }
        }
    }
}
