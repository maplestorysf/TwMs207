package handling.channel.handler;

import client.character.stat.MapleBuffStat;
import client.skill.Skill;
import client.skill.SkillFactory;
import client.skill.SkillMacro;
import client.forceatoms.ForceAtom;
import client.familiar.MonsterFamiliar;
import client.*;
import client.anticheat.CheatingOffense;
import server.skills.SkillHandleFetcher;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleAndroid;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.VMatrixRecord;
import client.inventory.VMatrixRecord.VCoreRecordState;
import server.client.CoreDataEntry;
import client.skill.vcore.VCoreFactory;
import constants.GameConstants;
import constants.ItemConstants;
import constants.ServerConstants;
import constants.SkillConstants;
import handling.RecvPacketOpcode;
import handling.channel.ChannelServer;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

import scripting.NPCScriptManager;
import server.*;
import server.Timer.BuffTimer;
import server.Timer.CloneTimer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.events.MapleSnowball;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.life.MobAttackInfo;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.FieldLimitType;
import server.maps.MapleHaku;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringTool;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CCashShop;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.JobPacket;
import tools.packet.MobPacket;
import tools.packet.SkillPacket;
import extensions.temporary.UserEffectOpcode;
import java.util.Map.Entry;
import server.maps.MapleAffectedArea;
import server.maps.SummonMovementType;
import tools.packet.FamiliarPacket;
import tools.packet.JobPacket.XenonPacket;

public class PlayerHandler {

    public static int fox = 0;

    public static int isFinisher(int skillid) {
        switch (skillid) {
            case 1101012:
                return 1;
            case 1111003:
                return 1;
            case 1121015:
                return 4;
        }
        return 0;
    }

    public static void ChangeSkillMacro(LittleEndianAccessor slea, MapleCharacter chr) {
        int num = slea.readByte();

        for (int i = 0; i < num; i++) {
            String name = slea.readMapleAsciiString();
            int shout = slea.readByte();
            int[] skill = new int[3];
            for (int j = 0; j < 3; j++) {
                int skillId = slea.readInt();
                if (chr.getTotalSkillLevel(skillId) != 0) {
                    skill[j] = skillId;
                } else {
                    skill[j] = 0;
                }
            }

            SkillMacro macro = new SkillMacro(skill[0], skill[1], skill[2], name, shout, i);
            chr.updateMacros(i, macro);
        }
    }

    public static void ChangeKeymap(LittleEndianAccessor slea, MapleCharacter chr) {
        if (slea.available() > 8L && chr != null) {
            slea.skip(4);
            int numChanges = slea.readInt();

            String sKey = "";
            String sType = "";
            String sAction = "";
            for (int i = 0; i < numChanges; i++) {
                int key = slea.readInt();
                sKey += key + ", ";
                byte type = slea.readByte();
                sType += type + ", ";
                int action = slea.readInt();
                sAction += action + ", ";
                if ((type == 1) && (action >= 1000)) {
                    Skill skil = SkillFactory.getSkill(action);
                    if ((skil != null) && (((!skil.isFourthJob()) && (!skil.isBeginnerSkill()) && (skil.isInvisible())
                            && (chr.getTotalSkillLevel(skil) <= 0)) || (GameConstants.isLinkedAttackSkill(action))
                            || (action % 10000 < 1000))) {
                        continue;
                    }
                }
                chr.changeKeybinding(key, type, action);
            }
            // System.out.println("Key:" + sKey);
            // System.out.println("Type:" + sType);
            // System.out.println("Action:" + sAction);
        } else if (chr != null) {
            int type = slea.readInt();
            int data = slea.readInt();
            switch (type) {
                case 1:
                    if (data <= 0) {
                        chr.getQuestRemove(MapleQuest.getInstance(GameConstants.HP_ITEM));
                    } else {
                        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.HP_ITEM)).setCustomData(String.valueOf(data));
                    }
                    break;
                case 2:
                    if (data <= 0) {
                        chr.getQuestRemove(MapleQuest.getInstance(GameConstants.MP_ITEM));
                    } else {
                        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.MP_ITEM)).setCustomData(String.valueOf(data));
                    }
                    break;
            }
        }
    }

    public static void ChangePetBuff(LittleEndianAccessor slea, MapleCharacter chr) {
        slea.readInt(); // 0
        int skill = slea.readInt();
        slea.readByte(); // 0
        if (skill <= 0) {
            chr.getQuestRemove(MapleQuest.getInstance(GameConstants.BUFF_ITEM));
        } else {
            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.BUFF_ITEM)).setCustomData(String.valueOf(skill));
        }
    }

    public static void UseTitle(int itemId, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        if (itemId != 0) {
            Item toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);
            if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1 || itemId / 10000 != 370) {
                c.announce(CWvsContext.enableActions());
                return;
            }
        }
        chr.setTitleEffect(itemId);
        chr.getMap().broadcastMessage(chr, CField.showTitle(chr.getId(), itemId), false);
    }

    public static void UpdateDamageSkin(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        byte mode = slea.readByte();

        switch (mode) {
            case 0: // save
            {
                if (chr.getSaveDamageSkin().contains(chr.getDamageSkin())) {
                    chr.getClient().announce(CField.onDamageSkinChangeError(chr, 7));
                    return;
                }
                chr.updateSaveDamageSkin(chr.getDamageSkin());
                chr.getClient().announce(CField.onDamageSkinSaveSuccess(chr));
                break;
            }
            case 2: // active
            {
                int damageSkin = slea.readShort();
                if (chr.getSaveDamageSkin().size() >= 29) {
                    chr.getClient().announce(CField.onDamageSkinChangeError(chr, 6));
                    return;
                }
                chr.setDamageSkin(damageSkin);
                chr.getClient().announce(CField.onDamageSkinChangeSuccess(chr));
                //
                byte[] packet = CField.updateDamageSkin(chr.getId(), damageSkin);
                if (chr.isHidden()) {
                    chr.getMap().broadcastGMMessage(chr, packet, true);
                } else {
                    chr.getMap().broadcastMessage(chr, packet, true);
                }
               List<WeakReference<MapleCharacter>> clones = chr.getClones();
                for (WeakReference<MapleCharacter> cln : clones) {
                    if (cln.get() != null) {
                        final MapleCharacter clone = cln.get();
                        byte[] packetClone = CField.updateDamageSkin(clone.getId(), damageSkin);
                        if (!clone.isHidden()) {
                            clone.getMap().broadcastMessage(clone, packetClone, true);
                        } else {
                            clone.getMap().broadcastGMMessage(clone, packetClone, true);
                        }
                    }
                }
                break;
            }
            case 1: // remove
            {
                int damageSkin = slea.readShort();
                if (!chr.getSaveDamageSkin().contains(damageSkin)) {
                    chr.getClient().announce(CField.onDamageSkinDeleteError(chr, 7));
                    return;
                }
                chr.updateSaveDamageSkin(damageSkin);
                chr.getClient().announce(CField.onDamageSkinDeleteSuccess(chr));
                break;
            }
        }

    }

    public static void AngelicChange(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        int transform = slea.readInt();
        if (transform == 5010094) {
            chr.getMap().broadcastMessage(chr, CField.showAngelicBuster(chr.getId(), transform), false);
            chr.getMap().broadcastMessage(chr, CField.updateCharLook(chr, transform == 5010094), false);
            c.announce(CWvsContext.enableActions());
        } else {
            chr.getMap().broadcastMessage(chr, CField.showAngelicBuster(chr.getId(), transform), false);
            chr.getMap().broadcastMessage(chr, CField.updateCharLook(chr, transform == 5010093), false);
            c.announce(CWvsContext.enableActions());
        }
    }

    public static void DressUpTime(LittleEndianAccessor slea, final MapleClient c) {
        byte type = slea.readByte();
        // System.out.println("abtype " + type);
        if (type == 1) {
            // PlayerHandler.AngelicChange(slea, c, chr);
            if (MapleJob.is天使破壞者(c.getPlayer().getJob())) {
                c.announce(JobPacket.AngelicPacket.DressUpTime(type));
                c.announce(JobPacket.AngelicPacket.updateDress(5010094, c.getPlayer()));
                // }
            } else {
                c.announce(CWvsContext.enableActions());
                // return;
            }
        }
    }
    // if (type != 1) {// || !SkillConstants.isAngelicBuster(c.getPlayer().getJob())
    // c.announce(CWvsContext.enableActions());
    // return;
    // }
    // c.announce(JobPacket.AngelicPacket.DressUpTime(type));
    // }

    public static void UserForceAtomCollision(LittleEndianAccessor slea, final MapleClient c) {
        final int size = slea.readInt();
        final int job = c.getPlayer().getJob();

        final int skillid = slea.readInt();
        final Skill skill = SkillFactory.getSkill(skillid);
        if (skill == null && !MapleJob.is惡魔殺手(job)) {
            return;
        }
        final int skillLevel = c.getPlayer().getSkillLevel(skill);
        final MapleStatEffect effect = !MapleJob.is惡魔殺手(job) ? skill != null ? skill.getEffect(skillLevel) : null
                : null;
        if (effect == null && !MapleJob.is惡魔殺手(job)) {
            return;
        }
        final List<ForceAtom> forceAtoms = new ArrayList<>();
        final List<Integer> objectIds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final int forceAtomKey = slea.readInt();
            final int unk2 = slea.readByte();
            final int monsterObjectId = slea.readInt();
            MapleMonster mon = c.getPlayer().getMap().getMonsterByOid(monsterObjectId);
            ForceAtom forceAtom = c.getPlayer().getAtom(forceAtomKey);
            forceAtoms.add(forceAtom);
            objectIds.add(monsterObjectId);
        }
        SkillHandleFetcher.onCollision(c.getPlayer(), skill, forceAtoms, objectIds);
    }

    public static void UseChair(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }

        slea.readInt();
        final int itemId = slea.readInt();
        slea.readByte();
        slea.readInt();
        String chairString = slea.readMapleAsciiString();
        final MapleInventoryType type = GameConstants.getInventoryType(itemId); // ---修正無法使用特殊欄椅子
        Item toUse = chr.getInventory(type).findById(itemId);
        if (toUse == null) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
            return;
        }

        if (GameConstants.isFishingMap(chr.getMapId()) && itemId == 3011000) {
            chr.startFishingTask();
        }
        chr.setChair(itemId);
        chr.getMap().broadcastMessage(chr, CField.showChair(chr.getId(), itemId, chairString), false);
        c.announce(CWvsContext.enableActions());
    }

    public static void CancelChair(short id, MapleClient c, MapleCharacter chr) {
        if (id == -1) {
            chr.cancelFishingTask();
            chr.setChair(0);
            c.announce(CField.cancelChair(-1, chr.getId()));
            if (chr.getMap() != null) {
                chr.getMap().broadcastMessage(chr, CField.showChair(chr.getId(), 0, ""), false);
            }
        } else {
            chr.setChair(id);
            c.announce(CField.cancelChair(id, chr.getId()));
        }
    }

    public static void TrockAddMap(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte addrem = slea.readByte();
        byte vip = slea.readByte();

        switch (vip) {
            case 1:
                if (addrem == 0) {
                    chr.deleteFromRegRocks(slea.readInt());
                } else if (addrem == 1) {
                    if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                        chr.addRegRockMap();
                    } else {
                        chr.dropMessage(1, "This map is not available to enter for the list.");
                    }
                }
                break;
            case 2:
                if (addrem == 0) {
                    chr.deleteFromRocks(slea.readInt());
                } else if (addrem == 1) {
                    if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                        chr.addRockMap();
                    } else {
                        chr.dropMessage(1, "This map is not available to enter for the list.");
                    }
                }
                break;
            case 3:
            case 5:
                if (addrem == 0) {
                    chr.deleteFromHyperRocks(slea.readInt());
                } else if (addrem == 1) {
                    if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                        chr.addHyperRockMap();
                    } else {
                        chr.dropMessage(1, "This map is not available to enter for the list.");
                    }
                }
                break;
            default:
                break;
        }
        c.announce(CCashShop.OnMapTransferResult(chr, vip, addrem == 0));
    }

    public static void CharInfoRequest(int objectid, MapleClient c, MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        MapleCharacter player = c.getPlayer().getMap().getCharacterById(objectid);
        c.announce(CWvsContext.enableActions());
        if (player != null/* && (!player.isGM() || c.getPlayer().isGM()) */) {
            c.announce(CWvsContext.charInfo(player, c.getPlayer().getId() == objectid));
        }
    }

    public static void AdminCommand(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (!c.getPlayer().isIntern()) {
            return;
        } else if (c.getPlayer().isIntern()) {
            return;
        }
        byte mode = slea.readByte();
        String victim;
        MapleCharacter target;
        switch (mode) {
            case 0x00: // Level1~Level8 & Package1~Package2
                int[][] toSpawn = MapleItemInformationProvider.getInstance().getSummonMobs(slea.readInt());
                for (int[] toSpawnChild : toSpawn) {
                    if (Randomizer.nextInt(101) <= toSpawnChild[1]) {
                        c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(toSpawnChild[0]),
                                c.getPlayer().getPosition());
                    }
                }
                c.announce(CWvsContext.enableActions());
                break;
            case 0x01: { // /d (inv)
                byte type = slea.readByte();
                MapleInventory in = c.getPlayer().getInventory(MapleInventoryType.getByType(type));
                for (short i = 0; i < in.getSlotLimit(); i++) {
                    if (in.getItem(i) != null) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.getByType(type), i,
                                in.getItem(i).getQuantity(), false);
                    }
                    return;
                }
                break;
            }
            case 0x02: // Exp
                c.getPlayer().setExp(slea.readInt());
                break;
            case 0x03: // /ban <name>
                victim = slea.readMapleAsciiString();
                String reason = victim + " permanent banned by " + c.getPlayer().getName();
                target = c.getChannelServer().getPlayerStorage().getCharacterByName(victim);
                if (target != null) {
                    String readableTargetName = MapleCharacter.makeMapleReadable(target.getName());
                    String ip = target.getClient().getSession().remoteAddress().toString().split(":")[0];
                    reason += readableTargetName + " (IP: " + ip + ")";
                    target.ban(reason, false, false);
                    c.announce(CField.getGMEffect(4));
                } else if (MapleCharacter.ban(victim, reason, false)) {
                    c.announce(CField.getGMEffect(4));
                } else {
                    c.announce(CField.getGMEffect(6));
                }
                break;
            case 0x04: // /block <name> <duration (in days)>
                // <HACK/BOT/AD/HARASS/CURSE/SCAM/MISCONDUCT/SELL/ICASH/TEMP/GM/IPROGRAM/MEGAPHONE>
                victim = slea.readMapleAsciiString();
                int type = slea.readByte(); // reason
                int duration = slea.readInt();
                String description = slea.readMapleAsciiString();
                reason = c.getPlayer().getName() + " used /ban to ban";
                target = c.getChannelServer().getPlayerStorage().getCharacterByName(victim);
                if (target != null) {
                    String readableTargetName = MapleCharacter.makeMapleReadable(target.getName());
                    String ip = target.getClient().getSession().remoteAddress().toString().split(":")[0];
                    reason += readableTargetName + " (IP: " + ip + ")";
                    if (duration == -1) {
                        target.ban(description + " " + reason, true);
                    } else {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DATE, duration);
                        target.ban(description, cal, MapleClient.BanReason.getByReason(type), false, false, false, false);
                    }
                    c.announce(CField.getGMEffect(4));
                } else if (MapleCharacter.ban(victim, reason, false)) {
                    c.announce(CField.getGMEffect(4));
                } else {
                    c.announce(CField.getGMEffect(6));
                }
                break;
            case 0x10: // /h, information by vana (and tele mode f1) ... hide ofcourse
                if (slea.readByte() > 0) {
                    SkillFactory.getSkill(9101004).getEffect(1).applyTo(c.getPlayer());
                } else {
                    c.getPlayer().dispelBuff(9101004);
                }
                break;
            case 0x11: // Entering a map
                switch (slea.readByte()) {
                    case 0:// /u
                        StringBuilder sb = new StringBuilder("USERS ON THIS MAP: ");
                        for (MapleCharacter mc : c.getPlayer().getMap().getCharacters()) {
                            sb.append(mc.getName());
                            sb.append(" ");
                        }
                        c.getPlayer().dropMessage(5, sb.toString());
                        break;
                    case 12:// /uclip and entering a map
                        break;
                }
                break;
            case 0x12: // Send
                victim = slea.readMapleAsciiString();
                int mapId = slea.readInt();
                c.getChannelServer().getPlayerStorage().getCharacterByName(victim)
                        .changeMap(c.getChannelServer().getMapFactory().getMap(mapId));
                break;
            case 0x15: // Kill
                int mobToKill = slea.readInt();
                int amount = slea.readInt();
                List<MapleMapObject> monsterx = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(),
                        Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                for (int x = 0; x < amount; x++) {
                    MapleMonster monster = (MapleMonster) monsterx.get(x);
                    if (monster.getId() == mobToKill) {
                        c.getPlayer().getMap().killMonster(monster, c.getPlayer(), false, false, (byte) 1);
                    }
                }
                break;
            case 0x16: // Questreset
                MapleQuest.getInstance(slea.readShort()).forfeit(c.getPlayer());
                break;
            case 0x17: // Summon
                int mobId = slea.readInt();
                int quantity = slea.readInt();
                for (int i = 0; i < quantity; i++) {
                    c.getPlayer().getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(mobId),
                            c.getPlayer().getPosition());
                }
                break;
            case 0x18: // Maple & Mobhp
                int mobHp = slea.readInt();
                c.getPlayer().dropMessage(5, "Monsters HP");
                List<MapleMapObject> monsters = c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(),
                        Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                for (MapleMapObject mobs : monsters) {
                    MapleMonster monster = (MapleMonster) mobs;
                    if (monster.getId() == mobHp) {
                        c.getPlayer().dropMessage(5, monster.getName() + ": " + monster.getHp());
                    }
                }
                break;
            case 0x1E: // Warn
                victim = slea.readMapleAsciiString();
                String message = slea.readMapleAsciiString();
                target = c.getChannelServer().getPlayerStorage().getCharacterByName(victim);
                if (target != null) {
                    target.getClient().getSession().writeAndFlush(CWvsContext.broadcastMsg(1, message));
                    c.announce(CField.getGMEffect(0x1E));
                } else {
                    c.announce(CField.getGMEffect(0x1E));
                }
                break;
            case 0x24:// /Artifact Ranking
                break;
            case 0x77: // Testing purpose
                if (slea.available() == 4) {
                    System.out.println(slea.readInt());
                } else if (slea.available() == 2) {
                    System.out.println(slea.readShort());
                }
                break;
            case 0x28: // /召喚
                break;
            default:
                System.out.println("遇到新的GM封包(模式:0x" + Integer.toHexString(mode) + ")" + slea.toString());
                break;
        }
    }

    public static void TakeDamage(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.isHidden() || chr.getMap() == null) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        slea.skip(4);
        chr.updateTick(slea.readInt());
        byte type = slea.readByte();
        slea.skip(1);
        int damage = slea.readInt();
        slea.skip(2);
        boolean isDeadlyAttack = false;
        boolean pPhysical = false;
        int oid = 0;
        int monsteridfrom = 0;
        int fake = 0;
        int mpattack = 0;
        int skillid = 0;
        int pID = 0;
        int pDMG = 0;
        byte direction = 0;
        byte pType = 0;
        Point pPos = new Point(0, 0);
        MapleMonster attacker = null;
        if (chr.isIntern() && chr.isInvincible()) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        PlayerStats stats = chr.getStat();

        if (chr.isShowInfo()) {
            chr.showInfo("角色受傷", false, "受傷類型: " + type + " 受傷數值: " + damage);
        }
        if ((type != -2) && (type != -3) && (type != -4)) {
            slea.readInt(); // oid
            monsteridfrom = slea.readInt();
            oid = slea.readInt();
            attacker = chr.getMap().getMonsterByOid(oid);
            direction = slea.readByte();
            if ((attacker == null) || (attacker.getId() != monsteridfrom) || (attacker.getLinkCID() > 0)
                    || (attacker.isFake()) || (attacker.getStats().isFriendly())) {
                return;
            }
            // 幻影劇情被發現
            if (chr.getMapId() == 915000300) {
                MapleMap to = chr.getClient().getChannelServer().getMapFactory().getMap(915000200);
                chr.dropMessage(5, "You've been found out! Retreat!");
                chr.changeMap(to, to.getPortal(1));
                return;
            }
            // 活動地圖,炸彈
            if (attacker.getId() == 9300166 && chr.getMapId() == 910025200) {
                int rocksLost = Randomizer.rand(1, 5);
                while (chr.itemQuantity(4031469) < rocksLost) {
                    rocksLost--;
                }
                if (rocksLost > 0) {
                    chr.gainItem(4031469, -rocksLost);
                    Item toDrop = MapleItemInformationProvider.getInstance().getEquipById(4031469);
                    for (int i = 0; i < rocksLost; i++) {
                        chr.getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(),
                                true, true);
                    }
                }
            }
            if ((type != -1) && (damage > 0)) {
                MobAttackInfo attackInfo = attacker.getStats().getMobAttack(type);
                if (attackInfo != null) {
                    if ((attackInfo.isElement) && (stats.屬性抗性 > 0) && (Randomizer.nextInt(100) < stats.屬性抗性)) {
                        System.out.println(new StringBuilder().append("Avoided ER from mob id: ").append(monsteridfrom)
                                .toString());
                        return;
                    }
                    if (attackInfo.isDeadlyAttack()) {
                        isDeadlyAttack = true;
                        mpattack = stats.getMp() - 1;
                    } else {
                        mpattack += attackInfo.getMpBurn();
                    }
                    MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(),
                            attackInfo.getDiseaseLevel());
                    if ((skill != null) && ((damage == -1) || (damage > 0))) {
                        skill.applyEffect(chr, attacker, false);
                    }
                    attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                }
            }
        }

        if (stats.DMGreduceR > 0 && damage > 1) {
            damage *= (double) ((100.0 - stats.DMGreduceR) / 100.0d);
        }

        if (type != -4 && type != -3) {
            skillid = slea.readInt();
            pDMG = slea.readInt();
        }

        byte defType = slea.readByte();
        slea.skip(1);
        if (chr.isShowInfo()) {
            chr.showInfo("角色受傷", false,
                    "受到傷害: " + damage + " 技能ID: " + skillid + " 反射傷害: " + pDMG + " defType: " + defType);
        }
        if (defType == 1) {

        }
        if (skillid != 0 && pDMG > 0) {
            pPhysical = slea.readByte() > 0;
            pID = slea.readInt();
            pType = slea.readByte();
            slea.readPos();
            pPos = slea.readPos();
        }
        if (damage == -1) {
            fake = 4020002 + (chr.getJob() / 10 - 40) * 100000;
            if ((fake != 4120002) && (fake != 4220002)) {
                fake = 4120002;
            }
            if ((type == -1) && (chr.getJob() == 122) && (attacker != null)
                    && (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null)
                    && (chr.getTotalSkillLevel(1220006) > 0)) {
                MapleStatEffect eff = SkillFactory.getSkill(1220006).getEffect(chr.getTotalSkillLevel(1220006));
                attacker.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.M_Stun, 1, 1220006, null, false), false,
                        eff.getDuration(), true, eff);
                fake = 1220006;
            }

            if (chr.getTotalSkillLevel(fake) <= 0) {
                return;
            }
        }

        if ((damage < -1) || (damage > 500000)) {
            c.announce(CWvsContext.enableActions());
            return;
        }

        SkillHandleFetcher.onDamage(chr, attacker);

        if (Randomizer.isSuccess(chr.getStat().閃避率) || (pPhysical && pDMG > 0 && chr.getTotalSkillLevel(skillid) > 0)) {
            switch (skillid) {
                case 1101006:
                    Skill skill = SkillFactory.getSkill(skillid);
                    MapleStatEffect eff = skill.getEffect(chr.getTotalSkillLevel(skillid));
                    long enemyDMG = Math.min((int) (damage * (eff.getY() / 100.0D)), attacker.getMobMaxHp() / 2L);
                    if (chr.isShowInfo()) {
                        chr.showInfo("角色受傷", false, "減少後傷害: " + damage + " 處理反射傷害: " + enemyDMG + " 解析反射傷害: " + pDMG
                                + " 技能ID: " + skillid + " - " + skill.getName());
                    }
                    if (enemyDMG > pDMG) {
                        enemyDMG = pDMG;
                    }
                    attacker.damage(chr, enemyDMG, true, skillid);
                default:
                    if (attacker != null) {
                        attacker.damage(chr, pDMG, true, skillid);
                    }
            }
        }
        if (stats.reduceDamageRate > 0) {
            if (chr.getJob() >= 1210 && chr.getJob() <= 1212) {
                chr.addMP((int) -(damage * (stats.reduceDamageRate / 100.0D)));
            }
            damage = (int) (damage - damage * (stats.reduceDamageRate / 100.0D));
        }
        chr.getCheatTracker().checkTakeDamage(damage);
        Pair<Double, Boolean> modify = chr.modifyDamageTaken(damage, attacker);
        damage = modify.left.intValue();
        if (chr.isShowInfo()) {
            chr.dropMessage(5, "最終受到傷害 " + damage);
        }
        if (damage > 0) {
            chr.getCheatTracker().setAttacksWithoutHit(false);

            if (chr.getBuffedValue(MapleBuffStat.Morph) != null) {
                chr.cancelMorphs();
            }

            boolean mpAttack = (chr.getBuffedValue(MapleBuffStat.Mechanic) != null)
                    && (chr.getBuffSource(MapleBuffStat.Mechanic) != 35121005);
            if (chr.getBuffedValue(MapleBuffStat.MagicGuard) != null || chr.getTotalSkillLevel(27000003) > 0) {
                int hploss = 0;
                int mploss = 0;
                if (isDeadlyAttack) {
                    if (stats.getHp() > 1) {
                        hploss = stats.getHp() - 1;
                    }
                    if (stats.getMp() > 1) {
                        mploss = stats.getMp() - 1;
                    }
                    if (chr.getBuffedValue(MapleBuffStat.Infinity) != null) {
                        mploss = 0;
                    }
                    chr.addMPHP(-hploss, -mploss);
                } else {
                    if (chr.getTotalSkillLevel(27000003) > 0) {// 魔法防禦
                        Skill skill = SkillFactory.getSkill(27000003);
                        int bof = chr.getTotalSkillLevel(skill);
                        MapleStatEffect eff = skill.getEffect(bof);
                        mploss = (int) (damage * ((eff.getX() + 100) / 100.0));
                    } else {
                        mploss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MagicGuard).doubleValue() / 100.0D))
                                + mpattack;
                    }
                    hploss = damage - mploss;
                    if (chr.getBuffedValue(MapleBuffStat.Infinity) != null) {
                        mploss = 0;
                    } else if (mploss > stats.getMp()) {
                        mploss = stats.getMp();
                        hploss = damage - mploss + mpattack;
                    }
                    chr.addMPHP(-hploss, -mploss);
                }
            } else if (chr.getStat().mesoGuardMeso > 0.0D) {
                int mesoloss = (int) (damage * (chr.getStat().mesoGuardMeso / 100.0D));
                if (chr.getMeso() < mesoloss) {
                    chr.gainMeso(-chr.getMeso(), false);
                    chr.cancelBuffStats(new MapleBuffStat[]{MapleBuffStat.MesoGuard});
                } else {
                    chr.gainMeso(-mesoloss, false);
                }
                if ((isDeadlyAttack) && (stats.getMp() > 1)) {
                    mpattack = stats.getMp() - 1;
                }
                chr.addMPHP(-damage, -mpattack);
            } else if (isDeadlyAttack) {
                chr.addMPHP(stats.getHp() > 1 ? -(stats.getHp() - 1) : 0,
                        (stats.getMp() > 1) && (!mpAttack) ? -(stats.getMp() - 1) : 0);
            } else {
                chr.addMPHP(-damage, mpAttack ? 0 : -mpattack);
            }
            if ((chr.inPVP()) && (chr.getStat().getHPPercent() <= 20)) {
                chr.getStat();
                SkillFactory.getSkill(PlayerStats.getSkillByJob(93, chr.getJob())).getEffect(1).applyTo(chr);
            }
        }
        byte offset = 0;
        int offset_d = 0;
        if (slea.available() == 1L) {
            offset = slea.readByte();
            if ((offset == 1) && (slea.available() >= 4L)) {
                offset_d = slea.readInt();
            }
            if ((offset < 0) || (offset > 2)) {
                offset = 0;
            }
        }

        if (chr.isShowInfo()) {
            chr.showInfo("玩家掉血", false, "類型: " + type + " 怪物ID: " + monsteridfrom + " 傷害: " + damage + " fake: " + fake
                    + " direction: " + direction + " oid: " + oid + " offset: " + offset);
        }

        chr.getMap().broadcastMessage(chr, CField.damagePlayer(chr.getId(), type, damage, monsteridfrom, direction,
                skillid, pDMG, pPhysical, pID, pType, pPos, offset, offset_d, fake), false);

        MapleAndroid a = chr.getAndroid();
        if (a != null) {
            a.showEmotion(chr, "alert");
        }

        List<WeakReference<MapleCharacter>> clones = c.getPlayer().getClones();
        for (WeakReference<MapleCharacter> cln : clones) {
            if (cln.get() != null) {
                cln.get().getMap().broadcastMessage(cln.get(), CField.damagePlayer(cln.get().getId(), type, damage, monsteridfrom, direction, skillid, pDMG, pPhysical, pID, pType, pPos, offset, offset_d, fake), false);
            }
        }
    }

    public static void AranCombo(MapleCharacter chr, int toAdd) {
        if (chr != null && (chr.getTotalSkillLevel(21000000) > 0 || chr.getTotalSkillLevel(21110000) > 0)
                && toAdd != 0) {
            short combo = chr.getCombo();
            combo = (short) Math.min(30000, combo + toAdd);
            chr.setCombo(combo);
            ShowAranCombo(chr, combo);
        }
    }

    public static void ShowAranCombo(MapleCharacter chr, short combo) {
        if (chr != null && (chr.getTotalSkillLevel(21000000) > 0 || chr.getTotalSkillLevel(21110000) > 0)) {
            if (combo < 0) {
                combo = (short) (chr.getShowCombo() + 1);
            }
            chr.setShowCombo(combo);
            chr.getClient().getSession().writeAndFlush(CField.updateCombo(combo));
        }
    }

    public static void AranCombo_Reduce(MapleCharacter chr) {
        if (chr != null && (chr.getTotalSkillLevel(21000000) > 0 || chr.getTotalSkillLevel(21110000) > 0)) {
            chr.setCombo(chr.getCombo());
        }
    }

    public static void UseItemEffect(int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = chr
                .getInventory(
                        (itemId == 4290001) || (itemId == 4290000) ? MapleInventoryType.ETC : MapleInventoryType.CASH)
                .findById(itemId);
        if ((toUse == null) || (toUse.getItemId() != itemId) || (toUse.getQuantity() < 1)) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (itemId != 5510000) {
            chr.setItemEffect(itemId);
        }
        chr.getMap().broadcastMessage(chr, CField.itemEffect(chr.getId(), itemId), false);
    }

    public static void CancelItemEffect(int id, MapleCharacter chr) {
        chr.cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(-id), false, -1L);
    }

    public static void CancelBuffHandler(int sourceid, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }

        Skill skill = SkillFactory.getSkill(sourceid);
        int checkSkilllevel = chr.getTotalSkillLevel(GameConstants.getLinkedAttackSkill(sourceid));
        if (chr.isShowInfo()) {
            chr.showMessage(10, "收到取消技能BUFF 技能ID " + sourceid + " 技能名字 " + SkillFactory.getSkillName(sourceid));
        }

        if (GameConstants.isExceedAttack(sourceid)) {
            chr.getClient().getSession().writeAndFlush(JobPacket.AvengerPacket.cancelExceedAttack());
        }
        if (skill.isChargeSkill()) {
            MapleStatEffect effect = skill.getEffect(chr.getTotalSkillLevel(skill));
            if (effect.getCooldown(chr) > 0) {
                chr.addCooldown(sourceid, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
            }
            chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, sourceid), false);
        }

        if (sourceid == 4221054) {
            chr.acaneAim = 0;
            chr.dualBrid = 0;
        }
        // TODO: 有沒有更ˋ好ㄉ寫法RRRRR
        if (skill.getId() == 13101022) {
            if (chr.getSkillLevel(13120003) > 0) {
                skill = SkillFactory.getSkill(13120003);
                checkSkilllevel = chr.getSkillLevel(13120003);
            } else if (chr.getSkillLevel(13110022) > 0) {
                skill = SkillFactory.getSkill(13110022);
                checkSkilllevel = chr.getSkillLevel(13110022);
            }
        }

        chr.cancelEffect(skill.getEffect(checkSkilllevel), false, -1L);

        if (sourceid == 27101202) {
            chr.getClient().getSession().writeAndFlush(
                    BuffPacket.cancelBuff(Arrays.asList(new MapleBuffStat[]{MapleBuffStat.KeyDownAreaMoving}), chr));
            chr.getMap().broadcastMessage(chr, BuffPacket.onResetTemporaryStat(chr.getId(),
                    Arrays.asList(new MapleBuffStat[]{MapleBuffStat.KeyDownAreaMoving})), false);
        }

        if (sourceid == 131001004) {
            chr.getClient().getSession().writeAndFlush(JobPacket.PinkBeanPacket.cancel咕嚕咕嚕());
        }

        if (sourceid >= 33001007 && sourceid <= 33001015) {
            chr.getClient().getSession().writeAndFlush(
                    BuffPacket.cancelBuff(Arrays.asList(new MapleBuffStat[]{MapleBuffStat.JaguarSummoned}), chr));
            for (MapleSummon ms : chr.getSummonsReadLock()) {
                if (ms.getSkill() >= 33001007 && ms.getSkill() <= 33001015) {
                    ms.sendDestroyData(chr.getClient());
                }
            }
            chr.unlockSummonsReadLock();
            // chr.getClient().getSession().writeAndFlush(CWvsContext.cancelJaguarRiding());
        }
        if (sourceid == 36121054) { // 阿瑪蘭斯發電機
            chr.getClient().getSession().writeAndFlush(XenonPacket.giveXenonSupply(chr.getXenonSurplus()));
        }
    }

    public static void CancelMech(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        int sourceid = slea.readInt();
        if ((sourceid % 10000 < 1000) && (SkillFactory.getSkill(sourceid) == null)) {
            sourceid += 1000;
        }
        Skill skill = SkillFactory.getSkill(sourceid);
        if (skill == null) {
            return;
        }
        if (skill.isChargeSkill()) {
            chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, sourceid), false);
        } else {
            chr.cancelEffect(skill.getEffect(slea.readByte()), false, -1L);
        }
    }

    public static void spawnSpecial(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int t_count = slea.readInt();
        int skillid = slea.readInt();
        slea.skip(4);
        int total = slea.readShort();
        for (int i = 0; i < total; i++) {
            int x1 = slea.readInt();
            int y1 = slea.readInt();
            int x2 = slea.readInt();
            int y2 = slea.readInt();
            Rectangle bounds = new Rectangle(x1, y1 - 5, (x2 - x1), (y2 - y1) + 50);

            MapleAffectedArea mist = new MapleAffectedArea(bounds, chr,
                    SkillFactory.getSkill(skillid).getEffect(chr.getTotalSkillLevel(skillid)));
            chr.getMap().spawnAffectedArea(mist, 6 * 1000, false);
        }
    }

    public static void releaseTempestBlades(LittleEndianAccessor slea, MapleCharacter chr) {
        if (!chr.isAlive()) {
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        int skillid = chr.getBuffSource(MapleBuffStat.StopForceAtomInfo);
        int skillLevel = chr.getSkillLevel(skillid);

        if (skillLevel <= 0) {
            return;
        }

        Skill skill = SkillFactory.getSkill(skillid);
        MapleStatEffect effect = skill.getEffect(skillLevel);
        int bulletCount = effect.getBulletCount();

        boolean advanced = skillid == 61120007 || skillid == 61121217;
        boolean transform = skillid == 61110211 || skillid == 61121217;
        final int mobcount = slea.readInt();
        final List<Integer> oids = new ArrayList<>();
        final List<ForceAtom> forceinfo = new ArrayList<>();
        for (int i = 0; i < mobcount; i++) {
            int oid = slea.readInt();
            if (chr.getMap().getMonsterByOid(oid) != null) {
                oids.add(oid);
                forceinfo.add(chr.getNewAtom(skillid, false));
            }
        }
        chr.getMap().broadcastMessage(
                CField.OnCreateForceAtom(false, chr, oids, 2, skillid, forceinfo, null, null, 0, 0, 0));
        if (skill != null) {
            int checkSkilllevel = chr.getTotalSkillLevel(GameConstants.getLinkedAttackSkill(skillid));
            chr.cancelEffect(skill.getEffect(checkSkilllevel), false, -1L);
        } else if (chr.isShowErr()) {
            chr.showInfo("技能代碼異常", true, "TODO 意志之劍");
        }
    }

    public static void OrbitalFlame(final LittleEndianAccessor slea, final MapleClient c) {
        MapleCharacter chr = c.getPlayer();

        int tempskill = slea.readInt();
        byte unk = slea.readByte();
        int direction = slea.readShort();
        int skillid = 0;
        int elementid = 0;
        int effect = 0;
        switch (tempskill) {
            case 12001020:
                skillid = 12000026;
                elementid = 12000022;
                effect = 1;
                break;
            case 12100020:
                skillid = 12100028;
                elementid = 12100026;
                effect = 2;
                break;
            case 12110020:
                skillid = 12110028;
                elementid = 12110024;
                effect = 3;
                break;
            case 12120006:
                skillid = 12120010;
                elementid = 12120007;
                effect = 4;
                break;
        }
        MapleStatEffect flame = SkillFactory.getSkill(tempskill).getEffect(chr.getSkillLevel(tempskill));
        if (flame != null && chr.getSkillLevel(elementid) > 0) {
            // if (!chr.getSummonsReadLock().contains(elementid)) {
            MapleStatEffect element = SkillFactory.getSkill(elementid).getEffect(chr.getSkillLevel(elementid));
            MapleSummon summon = new MapleSummon(chr, element, chr.getPosition(), SummonMovementType.飛行跟隨);
            List<MapleSummon> summons = chr.getSummonsReadLock();
            try {
                if (!summons.contains(summon)) {
                    summons.add(summon);
                    chr.getMap().spawnSummon(summon);
                    // element.applyTo(chr);
                }
            } finally {
                chr.unlockSummonsReadLock();
            }
        }
        chr.getMap().broadcastMessage(
                JobPacket.Cygnus.OrbitalFlame(chr.getId(), skillid, effect, direction, flame.getRange()));
    }

    public static void ZeroShockWave(LittleEndianAccessor slea, MapleClient c) {
        int skillid = slea.readInt();
        if (skillid == 101000101) {
            int delay = slea.readInt();
            slea.skip(4);
            byte direction = slea.readByte();
            Point position = slea.readPos();
            slea.skip(1);
            c.announce(JobPacket.ZeroPacket.shockWave(skillid, delay, direction, position));
        }
    }

    public static void QuickSlot(LittleEndianAccessor slea, MapleCharacter chr) {
        if ((chr != null)) {
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < 32; i++) {
                ret.append(slea.readInt()).append(",");
            }
            ret.deleteCharAt(ret.length() - 1);
            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.QUICK_SLOT)).setCustomData(ret.toString());
        }
    }

    public static void SkillEffect(LittleEndianAccessor slea, MapleCharacter chr) {
        int skillId = slea.readInt();
        if (skillId >= 91000000 && skillId < 100000000) {
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        byte level = slea.readByte();
        byte display = slea.readByte();
        byte direction = slea.readByte();
        byte speed = slea.readByte();
        Point position = null;
        if (slea.available() == 4L) {
            position = slea.readPos();
        } else if (slea.available() == 8) {
            position = slea.readPos();
        }

        Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAttackSkill(skillId));
        if (chr == null || skill == null || chr.getMap() == null) {
            return;
        }
        int skilllevel_serv = chr.getTotalSkillLevel(skill);
        if (skilllevel_serv > 0 && skilllevel_serv == level && skill.isChargeSkill()) {

            if (chr.getJob() == 422) {
                chr.getClient().getSession().writeAndFlush(SkillPacket.canLuckyMoney(true/* keydown_skill != 0 */));
            }
            chr.getMap().broadcastMessage(chr,
                    CField.skillEffect(chr.getId(), skillId, level, display, direction, speed, position), false);
            MapleStatEffect eff = skill.getEffect(skilllevel_serv);
            if (eff != null && eff.statups.containsKey(MapleBuffStat.KeyDownMoving)
                    && chr.getBuffedValue(MapleBuffStat.KeyDownMoving) == null) {
                eff.applyTo(chr);
            }
        }
    }

    public static void SpecialSkill(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.hasBlockedInventory()) || (chr.getMap() == null) || (slea.available() < 9L)) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        int dwTime = slea.readInt();
        int skillid = slea.readInt();
        if (skillid >= 91000000 && skillid < 100000000) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        // int xy1 = 0;
        // int xy2 = 0;
        // if (skillid == 65111100) {
        // xy1 = slea.readShort();
        // xy2 = slea.readShort();
        // int soulnum = slea.readByte();
        // int scheck = 0;
        // int scheck2 = 0;
        // if (soulnum == 1) {
        // scheck = slea.readInt();
        // } else if (soulnum == 2) {
        // scheck = slea.readInt();
        // scheck2 = slea.readInt();
        // }
        // c.announce(JobPacket.AngelicPacket.SoulSeeker(chr, skillid, soulnum, scheck,
        // scheck2));
        // c.announce(JobPacket.AngelicPacket.unlockSkill());
        // c.announce(CField.EffectPacket.showRechargeEffect());
        // c.announce(CWvsContext.enableActions());
        // return;
        // }
        if (MapleJob.is神之子(skillid / 10000)) {
            slea.readByte(); // zero
        }
        int skillLevel = slea.readByte();

        Skill skill = SkillFactory.getSkill(skillid);
        if ((skill == null)
                || ((SkillConstants.isAngel(skillid))
                && (chr.getStat().equippedSummon / 10000000 == 8 ? chr.getStat().equippedSummon != skillid
                : chr.getStat().equippedSummon % 10000 != skillid % 10000))
                || ((chr.inPVP()) && (skill.isPVPDisabled()))) {
            c.announce(CWvsContext.enableActions());
            return;
        }

        if (MapleJob.is凱內西斯(chr.getJob())) {
            chr.givePPoint(skillid);
        }

        int checkSkilllevel = 0;
        if (checkSkilllevel == 0) {
            checkSkilllevel = chr.getTotalSkillLevel(GameConstants.getLinkedAttackSkill(skillid));
        }
        if (chr.isShowInfo()) {
            chr.showMessage(8, "[SpecialSkill] - 技能ID：" + skill.getName() + "(" + skillid + ") 技能等級：" + skillLevel);
            if (GameConstants.getLinkedAttackSkill(skillid) != skillid) {
                chr.showMessage(8, "[SpecialSkill] - 連接技能ID：" + GameConstants.getLinkedAttackSkill(skillid) + " 連接技能等級："
                        + checkSkilllevel);
            }
        }
        if (checkSkilllevel <= 0 || checkSkilllevel != skillLevel) {
            if ((!GameConstants.isMulungSkill(skillid)) && (!GameConstants.isPyramidSkill(skillid))
                    && !SkillConstants.isAngel(skillid)) {
                if (chr.isShowErr()) {
                    chr.showMessage(8,
                            new StringBuilder().append("[SpecialSkill] 使用技能出現異常 技能ID：").append(skillid)
                                    .append(" 角色技能等級：").append(checkSkilllevel).append(" 封包獲取等級：").append(skillLevel)
                                    .append(" 是否相同：").append(checkSkilllevel == skillLevel).toString());
                }
                c.announce(CWvsContext.enableActions());
                return;
            }
            if (GameConstants.isMulungSkill(skillid)) {
                if (chr.getMapId() / 10000 != 92502) {
                    return;
                }
                if (chr.getMulungEnergy() < 10000) {
                    return;
                }
                chr.modifyMulungEnergy(false);
            } else if ((GameConstants.isPyramidSkill(skillid)) && (chr.getMapId() / 10000 != 92602)
                    && (chr.getMapId() / 10000 != 92601)) {
                return;
            }
        }
        if (GameConstants.isEventMap(chr.getMapId())) {
            for (MapleEventType t : MapleEventType.values()) {
                MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                if ((e.isRunning()) && (!chr.isIntern())) {
                    for (int i : e.getType().mapids) {
                        if (chr.getMapId() == i) {
                            chr.dropMessage(5, "無法在這裡使用.");
                            return;
                        }
                    }
                }
            }
        }
        skillLevel = chr.getTotalSkillLevel(GameConstants.getLinkedAttackSkill(skillid));
        MapleStatEffect effect = chr.inPVP() ? skill.getPVPEffect(skillLevel) : skill.getEffect(skillLevel);
        if (effect.isMPRecovery()
                && chr.getStat().getHp() < chr.getStat().getMaxHp() * effect.info.get(MapleStatInfo.x) / 100) {
            chr.dropMessage(5, "HP不足無法使用技能。");
            c.announce(CWvsContext.enableActions());
            return;
        }
        // 盾牌追擊消耗HP
        if (31221001 == skillid && !chr.isBuffed(31221054)) {
            if (chr.getStat().getHp() < chr.getStat().getMaxHp() * effect.info.get(MapleStatInfo.z) / 100) {
                chr.dropMessage(5, "HP不足無法使用技能。");
                c.announce(CWvsContext.enableActions());
                return;
            }
            int hpchange = chr.getStat().getCurrentMaxHp() * effect.info.get(MapleStatInfo.z) / 100;
            chr.addHP(-hpchange);
        }

        if (effect.getCooldown(chr) > 0) {
            if (chr.skillisCooling(skillid)) {
                c.announce(CWvsContext.enableActions());
                return;
            }
            if (skillid != 35111002 && skillid != 2301002) {
                chr.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
            }
        }
        for (int skil : SkillConstants.getSoulSkills()) {
            if (skil == skillid) {
                if (skillid == chr.getEquippedSoulSkill()) {
                    chr.checkSoulState(true);
                    break;
                } else {
                    return;
                }
            }
        }
        int mobID;
        MapleMonster mob;
        final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);

        switch (skillid) {
            case 1221016: // 守護者精神
                Rectangle bounds = effect.calculateBoundingBox(chr.getOldPosition(), chr.isFacingLeft());
                List<MapleCharacter> chrs = chr.getMap().getCharactersIntersect(bounds);
                for (MapleCharacter obj : chrs) {
                    if (obj.getParty() != null && obj.getParty().getId() == chr.getParty().getId() && !obj.isAlive()
                            && obj.getId() != chr.getId()) {
                        obj.getStat().setHp(obj.getStat().getCurrentMaxHp(), obj);
                        obj.setStance(0);
                        effect.applyTo(chr, obj, true, null, effect.getDuration());
                        effect.applyTo(chr);
                        return;
                    }
                }
                c.announce(CWvsContext.enableActions());
                break;
            case 12111022: // 漩渦
                Point pos = slea.readPos();
                slea.skip(3);
                mob = chr.getMap().getMonsterByOid(slea.readInt());
                effect.applySummonEffect(chr, true, pos, effect.getDuration(), mob.getId());
                c.announce(CWvsContext.enableActions());
                break;
            case 12001028: // 火步行
                c.announce(SkillPacket.FireStep());
                c.announce(CWvsContext.enableActions());
                break;
            case 12101025: // 火球連結
                pos = slea.readPos();
                Point pos2 = slea.readPos();
                slea.skip(1);
                int s = slea.readByte();
                c.announce(SkillPacket.ConveyTo());
                if (s == 1) {
                    c.announce(CField.CurentMapWarp(pos));
                }
                break;
            case 20031210: // 審判
                effect.applyTo(c.getPlayer(), slea.readPos());
                break;
            case 31221001: // 盾牌追擊
            case 65111100: // 靈魂探求者
                List<Integer> objectIds = new ArrayList<>();
                slea.skip(4);
                slea.readPos();
                byte nMobs = slea.readByte();
                for (int i = 0; i < nMobs; i++) {
                    objectIds.add(slea.readInt());
                }
                SkillHandleFetcher.onSpecialSkill(chr, objectIds, skill);
                c.announce(CWvsContext.enableActions());
                break;
            case 9001020:
            case 9101020:
            case 31111003: // 血腥烏鴉
                nMobs = slea.readByte();
                slea.skip(3);
                for (int i = 0; i < nMobs; i++) {
                    int mobId = slea.readInt();
                    mob = chr.getMap().getMonsterByOid(mobId);
                    if (mob == null) {
                        continue;
                    }
                    mob.switchController(chr, mob.isControllerHasAggro());
                    mob.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.M_Stun, 1, skillid, null, false), false,
                            effect.getDuration(), true, effect);
                }
                chr.getMap()
                        .broadcastMessage(chr,
                                CField.EffectPacket.showBuffEffect(false, chr, skillid,
                                        UserEffectOpcode.UserEffect_SkillUse, chr.getLevel(), skillLevel, slea.readByte()),
                                chr.getTruePosition());
                c.announce(CWvsContext.enableActions());
                break;
            case 30001061: // 捕獲
                mobID = slea.readInt();
                mob = chr.getMap().getMonsterByOid(mobID);
                if (mob != null) {
                    boolean success = (mob.getHp() <= mob.getMobMaxHp() / 2L) && (mob.getId() >= 9304000)
                            && (mob.getId() < 9305000);
                    chr.getMap().broadcastMessage(chr, CField.EffectPacket.showBuffEffect(false, chr, skillid,
                            UserEffectOpcode.UserEffect_SkillUse, chr.getLevel(), skillLevel, (byte) (success ? 1 : 0)),
                            chr.getTruePosition());
                    if (success) {
                        chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAGUAR))
                                .setCustomData(String.valueOf((mob.getId() - 9303999) * 10));
                        chr.getMap().killMonster(mob, chr, true, false, (byte) 1);
                        chr.cancelEffectFromBuffStat(MapleBuffStat.RideVehicle);
                        c.announce(SkillPacket.updateJaguar(chr));
                    } else {
                        chr.dropMessage(5, "怪物體力過高，補抓失敗。");
                    }
                }
                c.announce(CWvsContext.enableActions());
                break;
            case 30001062: // 獵人的呼喚
                chr.dropMessage(5, "沒有能被召喚的怪物，請先補抓怪物。");
                c.announce(CWvsContext.enableActions());
                break;
            case 33001016: // 爪攻擊
            case 33001025: // 挑釁
            case 33101115: // 歧路
            case 33101215: // 歧路
            case 33111015: // 音暴
            case 33121017: // 美洲豹靈魂
            case 33121255: // 狂暴之怒
                MapleSummon jaguar = null;
                for (MapleSummon ms : chr.getSummonsReadLock()) {
                    if (ms.getSkill() == 33001010) {
                        jaguar = ms;
                    }
                }
                chr.unlockSummonsReadLock();
                if (jaguar == null) {
                    chr.dropMessage(5, "沒有召喚的美洲豹，請先招喚美洲豹。");
                    return;
                }
                jaguar.setAttackSkill(skillid);
                jaguar.setAttackSkillLv(checkSkilllevel);
                c.announce(JobPacket.WildHunterPacket.sendJaguarSkill(skillid));
                c.announce(CWvsContext.enableActions());
                break;
            case 20040216: // 光蝕
            case 20040217: // 暗蝕
            case 20040219: // 平衡
            case 20040220: // 平衡
            case 20041239: // 光/暗黑模式轉換
                chr.changeLuminousMode(skillid);
                c.announce(CWvsContext.enableActions());
                break;
            case 25100009: // 小狐仙精通
                slea.skip(1);
                int tt = slea.readInt();
                c.announce(SkillPacket.隱月小狐仙(c.getPlayer().getId(), 25100010, tt, false));
                fox = 50;
                break;
            case 25120110: // 火狐精通
                slea.skip(1);
                int ftt = slea.readInt();
                c.announce(SkillPacket.隱月小狐仙(c.getPlayer().getId(), 25120115, ftt, false));
                fox = 100;
                break;
            case 36001005: // 追縱火箭
                int powerchange = effect.calcPowerChange(chr);
                if (powerchange != 0 && chr.getBuffedValue(MapleBuffStat.AmaranthGenerator) == null) {
                    if (powerchange < 0 && -powerchange > chr.getXenonSurplus()) {
                        chr.dropMessage(5, "使用技能時需要消耗的供給能源不足。");
                        return;
                    }
                    chr.gainXenonSurplus((short) powerchange);
                }
                byte mobCount = slea.readByte();
                List<Integer> Oid = new ArrayList<>();
                for (int i = 0; i < mobCount; i++) {
                    Oid.add(slea.readInt());
                }
                if (chr.isShowInfo()) {
                    chr.dropMessage(5, "追縱火箭 - " + " oldId: " + Oid + " 技能ID: " + skillid);
                }
                chr.handleAccurateRocket(Oid, skillid);
                break;
            case 4211006: // 楓幣炸彈
                byte level = slea.readByte();
                int Delay = slea.readShort();
                chr.handleMesosbomb(skillid, Delay);
                break;
            case 4221054:
                c.announce(JobPacket.ThiefPacket.OnOffFlipTheCoin(false));
                c.announce(CWvsContext.enableActions());
                effect.applyTo(chr);
                chr.dualBrid = 0;
                break;
            case 3101009: // 魔幻箭筒
                MapleStatEffect eff = new MapleStatEffect();
                eff.applyQuiverKartrige(chr, true);
                c.announce(CWvsContext.enableActions());
                break;
            case 4341003:
                chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, skillid), false);
                break;
            case 2121052: // 藍焰斬
                bounds = effect.calculateBoundingBox(chr.getOldPosition(), chr.isFacingLeft());
                List<MapleMapObject> mons = chr.getMap().getMapObjectsInRect(bounds,
                        Collections.singletonList(MapleMapObjectType.MONSTER));
                if (!mons.isEmpty()) {
                    chr.getMap().broadcastMessage(SkillPacket.getTrialFlame(chr.getId(), mons.get(0).getObjectId()));
                }
                break;
            case 11101120: // 潛行突襲
            case 11101220: // 皇家衝擊
            case 11101121: // 殘像追擊
            case 11101221: // 焚影
            case 11111120: // 月影
            case 11111220: // 光芒四射
            case 11111121: // 月光十字架
            case 11111221: // 日光十字架
            case 11121101: // 月光之舞
            case 11121201: // 疾速黃昏
            case 11121103: // 新月分裂
            case 11121203: // 太陽穿刺
                if (chr.getBuffSource(MapleBuffStat.PoseType) == 11121005) {
                    if (chr.getBuffedValue(MapleBuffStat.PoseType) == 1) {

                    } else {

                    }
                }
                break;
            case 33101005: // 咆哮
                mobID = chr.getFirstLinkMid();
                mob = chr.getMap().getMonsterByOid(mobID);
                chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, skillid), false);
                if (mob != null) {
                    boolean success = (mob.getStats().getLevel() < chr.getLevel()) && (mob.getId() < 9000000)
                            && (!mob.getStats().isBoss());
                    if (success) {
                        chr.getMap().broadcastMessage(MobPacket.suckMonster(mob.getObjectId(), chr.getId()));
                        chr.getMap().killMonster(mob, chr, false, false, (byte) -1);
                    } else {
                        chr.dropMessage(5, "The monster has too much physical strength, so you cannot catch it.");
                    }
                } else {
                    chr.dropMessage(5, "No monster was sucked. The skill failed.");
                }
                c.announce(CWvsContext.enableActions());
                break;
            case 110001500: // 解除模式
                stat.clear();
                c.announce(JobPacket.BeastTamerPacket.ModeCancel());
                c.announce(CWvsContext.enableActions());
                break;
            case 110001501: // 召喚熊熊
            case 110001502: // 召喚雪豹
            case 110001503: // 召喚雀鷹
            case 110001504: // 召喚貓咪
                slea.skip(3);
                stat.clear();
                stat.put(MapleBuffStat.AnimalChange, skillid - 110001500);
                chr.setBuffedValue(MapleBuffStat.AnimalChange, skillid - 110001500);
                c.announce(CWvsContext.BuffPacket.giveBuff(skillid, 0, stat, null, chr));
                c.announce(CWvsContext.enableActions());
                break;
            // case 11101022:
            // case 11111022:
            // case 11121005:
            // case 11121011:
            // case 11121012:
            // chr.changeWarriorStance(skillid);
            // c.announce(CWvsContext.enableActions());
            // break;
            default:
                pos = null;
                if ((slea.available() == 5L) || (slea.available() == 7L)) {
                    pos = slea.readPos();
                    boolean faceLeft = slea.readByte() == 0;
                    int stance = chr.getStance();
                    if (faceLeft) {
                        stance &= 0xFE;
                    } else {
                        stance |= 1;
                    }
                    chr.setStance(stance);
                }
                if (effect.isMagicDoor()) {
                    if (!FieldLimitType.MysticDoor.check(chr.getMap().getFieldLimit())) {
                        effect.applyTo(c.getPlayer(), pos);
                    } else {
                        c.announce(CWvsContext.enableActions());
                    }
                } else {
                    int mountid = MapleStatEffect.parseMountInfo(c.getPlayer(), skill.getId());
                    if ((mountid != 0) && (mountid != GameConstants.getMountItem(skill.getId(), c.getPlayer()))
                            && (c.getPlayer().getBuffedValue(MapleBuffStat.RideVehicle) == null)
                            && (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -122) == null)
                            && (!GameConstants.isMountItemAvailable(mountid, c.getPlayer().getJob()))) {
                        c.announce(CWvsContext.enableActions());
                        return;
                    }

                    if (effect.getSourceId() == 5321004) {
                        effect.applyTo(chr, pos);
                        effect = SkillFactory.getSkill(5320011).getEffect(skillLevel);
                        if (pos != null) {
                            pos.x -= 90;
                        }
                        if (effect == null) {
                            break;
                        }
                        effect.applyTo(chr, pos);
                    } else {
                        if (effect.isHide() && chr.isHidden()) {
                            chr.cancelEffect(effect, false, -1);
                            c.announce(CWvsContext.enableActions());
                            return;
                        }
                        // 召喚船員
                        if (skillid == 5201012 || skillid == 5210015) {
                            switch (skillid) {
                                case 5201012:
                                    skill = SkillFactory.getSkill(5210015);
                                    skillLevel = chr.getTotalSkillLevel(skill);
                                    if (skillLevel > 0) {
                                        effect = chr.inPVP() ? skill.getPVPEffect(skillLevel) : skill.getEffect(skillLevel);
                                    }
                                    break;
                            }
                        }
                        effect.applyTo(chr, pos);
                    }
                }
        }
        if (MapleJob.is天使破壞者(chr.getJob())) {
            chr.Handle_ReCharge(effect, skillid, c, true);
        }
        if (skillid == 2001009) {
            if (chr.getBuffSource(MapleBuffStat.ChillingStep) == 2201009) {
                int rdz = server.Randomizer.nextInt(100);
                MapleStatEffect eff = SkillFactory.getSkill(2201009).getEffect(chr.getTotalSkillLevel(2201009));
                int g_rate = eff.getProb();
                if (rdz <= g_rate) {
                    Point newUserPos = chr.getPosition();
                    c.announce(CField.spawnSpecial(2201009, newUserPos.x, newUserPos.y, newUserPos.x + 179,
                            newUserPos.y + 21));
                }
            }
        }
        c.getPlayer().monsterMultiKill();
    }

    public static void ArrowBlasterAction(LittleEndianAccessor slea, final MapleClient c, MapleCharacter chr) {
        final int a = slea.readByte();
        final int x = slea.readInt();
        final int y = slea.readInt();
        c.getPlayer().getMap().broadcastMessage(chr, CField.spawnArrowBlaster(chr, a), false);
        c.announce(CField.spawnArrowBlaster(chr, a));
        c.announce(CField.controlArrowBlaster(chr.getId()));
        BuffTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                c.announce(CField.cancelArrowBlaster(chr.getId()));
            }
        }, 30000L);
    }

    public static void attack(LittleEndianAccessor slea, MapleClient c, RecvPacketOpcode header) {
        MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        if (chr.hasBlockedInventory() || chr.getMap() == null) {
            chr.dropMessage(5, "現在還不能進行攻擊。");
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (chr.isIntern() && !chr.isAdmin() && chr.getMap().isBossMap()) {
            chr.dropMessage(5, "管理員不能打BOSS。");
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (!chr.isAdmin() && chr.getMap().isMarketMap()) {
            chr.dropMessage(5, "在自由市場無法使用技能。");
            c.announce(CWvsContext.enableActions());
            return;
        }

        int level = chr.getLevel();
        try {
            switch (header) {
                case CP_UserMeleeAttack: // 近戰攻擊
                case CP_UserShootAttack: // 射擊攻擊
                case CP_UserMagicAttack: // 魔法攻擊
                case CP_UserBodyAttack: // 身體攻擊
                case CP_UserAreaDotAttack:// Dot攻擊(寒冰迅移)
                    AttackInfo attack = DamageParse.parseDamage(slea, chr, header);
                    if (attack == null) {
                        chr.dropMessage(5, "當前狀態限制了攻擊。");
                        c.announce(CWvsContext.enableActions());
                        return;
                    }
                    userAttack(attack, c, chr);
                    break;
                case CP_SummonedAttack: // 召喚獸攻擊
                    SummonHandler.SummonAttack(slea, c, chr);
                    break;
            }
        } finally {
            if (level < chr.getLevel()) {
                chr.setLastLevelup(System.currentTimeMillis());
            }
            chr.monsterMultiKill();
        }
    }

    public static void userAttack(AttackInfo attack, MapleClient c, MapleCharacter chr) {
        if (attack == null) {
            chr.dropMessage(5, "當前狀態限制了攻擊。");
            c.announce(CWvsContext.enableActions());
            return;
        }
        final boolean mirror = chr.getBuffedValue(MapleBuffStat.ShadowPartner) != null;
        boolean hasMoonBuff = chr.getBuffedValue(MapleBuffStat.PoseType) != null;
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage();
        Item shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
        int attackCount = shield != null && ItemConstants.類型.雙刀(shield.getItemId()) ? 2 : 1;
        int skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;
        boolean noBullet = true;
        if (attack.isShootAttack) {
            if (MapleJob.is弓箭手(chr.getJob())) {
                noBullet = !MapleJob.is冒險家(chr.getJob()) && !MapleJob.is皇家騎士團(chr.getJob());
            } else if (MapleJob.is盜賊(chr.getJob())) {
                noBullet = !MapleJob.is冒險家(chr.getJob()) && !MapleJob.is皇家騎士團(chr.getJob());
            } else if (MapleJob.is海盜(chr.getJob())) {
                noBullet = !MapleJob.is冒險家(chr.getJob()) || MapleJob.is拳霸(attack.skill / 10000)
                        || MapleJob.is重砲指揮官(chr.getJob()) || MapleJob.is蒼龍俠客(chr.getJob());
            }
            if (attack.skill == 0) {
                if (MapleJob.is盜賊(chr.getJob())) {
                    noBullet = noBullet && !MapleJob.is影武者(chr.getJob()) && !MapleJob.is幻影俠盜(chr.getJob())
                            && !MapleJob.is傑諾(chr.getJob());
                } else if (MapleJob.is海盜(chr.getJob())) {
                    noBullet = noBullet && !MapleJob.is皇家騎士團(chr.getJob()) && !MapleJob.is隱月(chr.getJob())
                            && !MapleJob.is傑諾(chr.getJob());
                }
            } else {
                switch (attack.skill) {
                    case 13101020: // 妖精護盾
                        noBullet = true;
                        break;
                }
            }
        }

        if (attack.skill == 0 && !(attack.isMeleeAttack || attack.isShootAttack)) {
            chr.dropMessage(5, "當前狀態限制了攻擊。");
            c.announce(CWvsContext.enableActions());
            return;
        }

        // 判斷是不為普通攻擊
        if (attack.skill != 0) {
            // chr.dropMessage(-1, "Attack Skill: " + attack.skill);//debug mode
            skill = SkillFactory.getSkill(attack.skill);
            if (skill == null || (SkillConstants.isAngel(attack.skill)
                    && chr.getStat().equippedSummon % 10000 != attack.skill % 10000)) {
                c.announce(CWvsContext.enableActions());
                return;
            }

            skillLevel = chr.getTotalSkillLevel(skill);
            if (chr.isBuffed(40011288)) { // 拔刀姿勢
                switch (skill.getId()) {
                    case 40011289: // 疾風五月雨刃
                    case 41001005:
                    case 41001004:
                    case 41001000: // 三連斬 - 疾
                    case 41101009:
                    case 41101008:
                    case 41101000: // 三連斬 - 風
                    case 41001011: // 曉月流跳躍
                    case 41111012:
                    case 41111011:
                    case 41111000: // 三連斬 - 迅
                    case 41120013:
                    case 41120012:
                    case 41120011:
                    case 41121012:
                    case 41121011:
                    case 41121000: // 三連斬 - 雷
                        skillLevel = 0;
                }
            } else if (chr.isBuffed(40011291)) { // 一般姿勢效果
                switch (skill.getId()) {
                    case 40011290: // 百人一閃
                    case 41001009: // 拔刀術 - 疾
                    case 41001012: // 曉月流瞬步
                    case 41101011: // 拔刀術 - 風
                    case 41111014: // 拔刀術 - 迅
                    case 41121016: // 拔刀術 - 雷
                        skillLevel = 0;
                }
            }
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                if (chr.isShowErr()) {
                    chr.dropMessage(5,
                            "攻擊效果為空。使用技能: " + skill.getId() + " - " + skill.getName() + " 技能等級: " + skillLevel);
                }
                return;
            }

            // if (effect.statups.containsKey(MapleBuffStat.KeyDownMoving) &&
            // chr.getBuffedValue(MapleBuffStat.KeyDownMoving) == null &&
            // chr.getBuffedValue(MapleBuffStat.HayatoStance) == null) {
            // chr.dropMessage(5, "當前狀態限制了攻擊。");
            // c.announce(CWvsContext.enableActions());
            // return;
            // }
            if (GameConstants.isEventMap(chr.getMapId())) {
                for (MapleEventType t : MapleEventType.values()) {
                    MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                    if ((e.isRunning()) && (!chr.isIntern())) {
                        for (int i : e.getType().mapids) {
                            if (chr.getMapId() == i) {
                                chr.dropMessage(5, "無法在這裡使用。");
                                return;
                            }
                        }
                    }
                }
            }

            if (GameConstants.isExceedAttack(skill.getId())) {
                chr.handleExceedAttack(skill.getId());
            }

            // 凱內西斯增加PP值
            if (attack.skill != 0 && MapleJob.is凱內西斯(chr.getJob())) {
                c.getPlayer().givePPoint(attack.skill);
            }

            // 神之子更變性別技能
            switch (attack.skill) {
                case 101001100:
                case 101101100:
                case 101111100:
                case 101121100:
                    chr.zeroChange(false);
                    break;
                case 101001200:
                case 101101200:
                case 101111200:
                case 101121200:
                    chr.zeroChange(true);
                    break;
            }

            if (MapleJob.is天使破壞者(chr.getJob())) {
                chr.Handle_ReCharge(effect, attack.skill, c, false);
            }

            long money = effect.getMoneyCon();
            if (money != 0) {
                if (money > chr.getMeso()) {
                    money = chr.getMeso();
                }
                chr.gainMeso(-money, false);
            }

            if (noBullet || effect.getBulletCount() < effect.getAttackCount()) {
                attackCount = effect.getAttackCount();
            } else {
                attackCount = effect.getBulletCount();
            }

            if ((effect.getCooldown(chr) > 0 && !attack.isBodyAttack) && attack.skill != 24121005/* 卡牌風暴 */
                    && attack.skill != 2221012) {
                if (chr.skillisCooling(attack.skill) && !effect.statups.containsKey(MapleBuffStat.DevilishPower)) {
                    chr.dropMessage(5, "技能由於冷卻時間限制，暫時無法使用。");
                    c.announce(CWvsContext.enableActions());
                    return;
                }
                if (!chr.skillisCooling(attack.skill)) {
                    if ((!effect.statups.containsKey(MapleBuffStat.KeyDownMoving)
                            || chr.getBuffedValue(MapleBuffStat.KeyDownMoving) == null)
                            && chr.getBuffedValue(MapleBuffStat.DevilishPower) == null) {
                        chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
                    }
                }
            }

            if (attack.skill == 15121001) {
                // attackCount +=
                // Math.min(chr.getBuffedValue(MapleBuffStat.INDIE_IGNORE_MOB_PDP_R) / 5,
                // chr.getStat().raidenCount);
            } else if (attack.skill == 15111022) {
                effect.applyTo(chr);
            }

            if (attack.skill == 131001010 || attack.skill == 131001011) {
                effect = SkillFactory.getSkill(131001010).getEffect(1);
                Integer value = chr.getBuffedValue(MapleBuffStat.PinkbeanYoYoStack);
                if (value == null || value <= 0) {
                    chr.dropMessage(5, "沒有準備好的溜溜球所以無法使用技能。");
                    c.announce(CWvsContext.enableActions());
                    return;
                }
                chr.setBuffedValue(MapleBuffStat.PinkbeanYoYoStack, Math.max(0, --value));
            }

            if (attack.skill == 40011289 || attack.skill == 40011290) { // 疾風五月雨刃 || 百人一閃
                if (chr.getJianQi() < 1000) {
                    chr.dropMessage(5, "目前劍氣不足無法使用此技能");
                    c.announce(CWvsContext.enableActions());
                    return;
                }
                chr.setJianQi((short) 200);
            }
        }

        // 最後處理傷害訊息
        attack = DamageParse.Modify_AttackCrit(attack, chr, effect);

        // 傷害次數最後計算
        attackCount *= mirror ? 2 : 1;
        attackCount *= hasMoonBuff ? 2 : 1;

        int visProjectile = 0;
        int projectile = 0;
        if (chr.getBuffedValue(MapleBuffStat.SoulArrow) == null && !noBullet && attack.skill != 95001000/* 安裝的箭座 */) {
            Item ipp = chr.getInventory(MapleInventoryType.USE).getItem(attack.starSlot);
            if (ipp == null) {
                if (chr.isShowErr()) {
                    chr.showInfo("攻擊", true, "無需要消耗的道具");
                }
                return;
            }
            projectile = ipp.getItemId();

            if (attack.cashSlot > 0) {
                if (chr.getInventory(MapleInventoryType.CASH).getItem(attack.cashSlot) == null) {
                    return;
                }
                visProjectile = chr.getInventory(MapleInventoryType.CASH).getItem(attack.cashSlot).getItemId();
            } else {
                visProjectile = projectile;
            }

            if (chr.getBuffedValue(MapleBuffStat.NoBulletConsume) == null) {
                int bulletConsume = attackCount;
                if (effect != null && effect.getBulletConsume() != 0) {
                    bulletConsume = effect.getBulletConsume() * (mirror ? 2 : 1);
                }
                if (chr.getJob() == 412 && bulletConsume > 0
                        && ipp.getQuantity() < MapleItemInformationProvider.getInstance().getSlotMax(projectile)) {
                    Skill expert = SkillFactory.getSkill(4120010);
                    if (chr.getTotalSkillLevel(expert) > 0) {
                        MapleStatEffect eff = expert.getEffect(chr.getTotalSkillLevel(expert));
                        if (eff.makeChanceResult()) {
                            ipp.setQuantity((short) (ipp.getQuantity() + 1));
                            c.announce(CWvsContext.InventoryPacket.updateInventorySlot(ipp, false));
                            bulletConsume = 0;
                            c.announce(CWvsContext.InventoryPacket.updateInventoryFull());
                        }
                    }
                }
                if (bulletConsume > 0 && !MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile,
                        bulletConsume, false, true)) {
                    chr.dropMessage(5, "您的箭/子彈/飛鏢不足。");
                    return;
                }
            }
        } else if ((chr.getJob() >= 3500) && (chr.getJob() <= 3512)) {
            visProjectile = 2333000;
        } else if (MapleJob.is重砲指揮官(chr.getJob())) {
            visProjectile = 2333001;
        }

        int projectileWatk = 0;
        if (projectile != 0) {
            projectileWatk = MapleItemInformationProvider.getInstance().getWatkForProjectile(projectile);
        }

        if (attack.skill == 1321013) {
            maxdamage += chr.getStat().getCurrentMaxHp();
        }

        if (effect != null) {
            switch (attack.skill) {
                case 4100012:
                case 4120019:
                    maxdamage *= (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)
                            + effect.getX() * chr.getLevel()) / 100.0D;
                    break;
                case 3101005:
                    maxdamage *= effect.getX() / 100.0D;
                    break;
                case 4001344:
                case 4121007:
                case 14001004:
                case 14111005:
                    maxdamage = Math.max(maxdamage,
                            chr.getStat().getTotalLuk() * 5.0F * (chr.getStat().getTotalWatk() + projectileWatk) / 100.0F);
                    break;
                case 20021166:
                case 4111004:
                    maxdamage = 53000.0D;
                default:
                    maxdamage *= (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0D;
                    break;
            }
        }

        if (attack.isMeleeAttack) {
            // 活動，攻擊雪球，普通攻擊
            if (((chr.getMapId() == 109060000) || (chr.getMapId() == 109060002) || (chr.getMapId() == 109060004))
                    && (attack.skill == 0)) {
                MapleSnowball.MapleSnowballs.hitSnowball(chr);
            }

            // 消耗鬥氣的技能
            if (isFinisher(attack.skill) > 0) {
                int numFinisherOrbs = 0;
                Integer comboBuff = chr.getBuffedValue(MapleBuffStat.ComboCounter);
                if (comboBuff != null) {
                    numFinisherOrbs = comboBuff - 1;
                }
                if (numFinisherOrbs <= 0) {
                    return;
                }
                chr.handleOrbconsume(isFinisher(attack.skill));
                maxdamage *= numFinisherOrbs;
            }
        }

        // 停止跟隨
        chr.checkFollow();

        // 給地圖上的玩家顯示當前玩家使用技能的效果
        byte[] packet;
        if (attack.isMeleeAttack) {
            packet = CField.closeRangeAttack(chr, skillLevel, visProjectile, attack, hasMoonBuff);
        } else if (attack.isShootAttack) {
            packet = CField.rangedAttack(chr, skillLevel, visProjectile, attack);
        } else if (attack.isMagicAttack || attack.isDotAttack) {
            packet = CField.magicAttack(chr, skillLevel, visProjectile, attack);
        } else if (attack.isBodyAttack) {
            packet = CField.passiveAttack(chr, skillLevel, visProjectile, attack, hasMoonBuff);
        } else {
            if (chr.isShowErr()) {
                chr.showInfo("攻擊", true, "獲取對應封包出錯");
            }
            return;
        }
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, packet, chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, packet, false);
        }

        // 召喚迷你啾
        if (chr.getBuffedValue(MapleBuffStat.PinkbeanMinibeenMove) != null && attack.targets > 0
                && Randomizer.nextInt(100) < (chr.getTotalSkillLevel(131001012) > 0 ? 25 : 15)) {
            List<MapleSummon> summons = chr.getSummonsReadLock();
            int sumNums = 0;
            for (MapleSummon summon : summons) {
                if (summon.getSkill() == 131002015) {
                    sumNums++;
                }
            }
            chr.unlockSummonsReadLock();
            if (sumNums < 3) {
                MapleStatEffect eff = chr.inPVP() ? SkillFactory.getSkill(131002015).getPVPEffect(1)
                        : SkillFactory.getSkill(131002015).getEffect(1);
                eff.applySummonEffect(chr, false, null, 10000, 0);
            }
        }

        // 攻擊傷害處理
        DamageParse.applyAttack(attack, skill, c.getPlayer(), attackCount, maxdamage, effect, mirror);

        if (attack.skill == 80001770) {
            chr.setLastLevelup(0);
        }

        // 處理克隆人的攻擊
        List<WeakReference<MapleCharacter>> clones = chr.getClones();
        for (int i = 0; i < clones.size(); i++) {
            if (clones.get(i).get() != null) {
                final MapleCharacter clone = clones.get(i).get();
                final Skill skil2 = skill;
                final int skillLevel2 = skillLevel;
                final int attackCount2 = attackCount;
                final int visProjectile2 = visProjectile;
                final double maxdamage2 = maxdamage;
                final MapleStatEffect eff2 = effect;
                final AttackInfo attack2 = DamageParse.DivideAttack(attack, chr.isIntern() ? 1 : 4);
                final byte[] packetClone;
                if (attack.isMeleeAttack) {
                    packetClone = CField.closeRangeAttack(clone, skillLevel2, visProjectile2, attack2, hasMoonBuff);
                } else if (attack.isShootAttack) {
                    packetClone = CField.rangedAttack(clone, skillLevel2, visProjectile2, attack2);
                } else if (attack.isMagicAttack) {
                    packetClone = CField.magicAttack(clone, skillLevel2, visProjectile2, attack2);
                } else if (attack.isBodyAttack) {
                    packetClone = CField.passiveAttack(clone, skillLevel2, visProjectile2, attack2, hasMoonBuff);
                } else {
                    continue;
                }
                Timer.CloneTimer.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        if (!clone.isHidden()) {
                            clone.getMap().broadcastMessage(packetClone);
                        } else {
                            clone.getMap().broadcastGMMessage(clone, packetClone, false);
                        }
                        DamageParse.applyAttack(attack2, skil2, chr, attackCount2, maxdamage2, eff2, mirror);
                    }
                }, 500 * i + 500);
            }
        }
    }

    public static void DropMeso(int meso, MapleCharacter chr) {
        if ((!chr.isAlive()) || (meso < 10) || (meso > 50000) || (meso > chr.getMeso())) {
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        chr.gainMeso(-meso, false, true);
        chr.getMap().spawnMesoDrop(meso, chr.getTruePosition(), chr, chr, true, (byte) 0);
        chr.getCheatTracker().checkDrop(true);
    }

    public static void ChangeAndroidEmotion(int emote, MapleCharacter chr) {
        // if ((emote > 0) && (chr != null) && (chr.getMap() != null) &&
        // (!chr.isHidden()) && (emote <= 17) && (chr.getAndroid() != null))
        // chr.getMap().broadcastMessage(CField.showAndroidEmotion(chr.getId(), emote));
    }

    public static void AndroidShop(LittleEndianAccessor slea, MapleCharacter chr) {
        int cid = slea.readInt();
        int androidType = slea.readInt();
        int unk1 = slea.readInt();
        int unk2 = slea.readInt();
        MapleAndroid android = chr.getAndroid();
        if (android == null || android.getType() != androidType || chr.getId() != cid) {
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }

        int npcId;
        switch (android.getItemId()) {
            case 1662027: // 女僕機器人
            case 1662039: // 殺人鯨機器人
            case 1662041: // 萬聖節裝飾
            case 1662053: // 史烏機器人
            case 1662072: // 戰鬥機器人(女)
            case 1662073: // 戰鬥機器人(男)
            case 1666000: // 女僕機器人
            case 1666001: // 女僕機器人
            case 1666002: // 初音未來機器人
                npcId = 9330194;
                break;
            default:
                FileoutputUtil.log("日誌/機器人.txt", "機器人未處理: [" + cid + "][" + android.getItemId() + "][" + androidType + "]["
                        + unk1 + "][" + unk2 + "]");
                chr.dropMessage(1, "[Error]請將訊息反饋給管理員:\r\n[機器人未處理]:" + android.getItemId() + "(" + androidType + ")");
                chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
                return;
        }

        final MapleNPC npc = MapleLifeFactory.getNPC(npcId);

        if (npc == null) {
            chr.dropMessage(1, "[Error]請將訊息反饋給管理員:\r\n[機器人商店]::" + npcId);
            chr.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
        } else if (npc.hasShop()) {
            chr.setConversation(1);
            npc.sendShop(chr.getClient());
        } else {
            NPCScriptManager.getInstance().start(chr.getClient(), npcId, null);
        }
    }

    public static void MoveAndroid(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int duration = slea.readInt();
        Point mPos = slea.readPos();
        Point oPos = slea.readPos();
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3, chr);

        if (res != null && chr != null && !res.isEmpty() && chr.getMap() != null && chr.getAndroid() != null) {
            if (slea.available() != 0) {
                System.out.println("slea.available() != 0 (機器人移動出錯) 剩餘封包長度: " + slea.available());
                FileoutputUtil.log(FileoutputUtil.Movement_Log,
                        "slea.available != 0 (機器人移動出錯) 封包: " + slea.toString(true));
                return;
            }
            mPos = new Point(chr.getAndroid().getPos());
            chr.getAndroid().updatePosition(res);
            chr.getMap().broadcastMessage(chr, CField.moveAndroid(chr.getId(), duration, mPos, oPos, res), false);
        }
    }

    public static void MoveHaku(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(17);
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 6, chr);

        if (res != null && chr != null && !res.isEmpty() && chr.getMap() != null && chr.getHaku() != null) {
            Point mPos = chr.getHaku().getPosition();
            chr.getHaku().updatePosition(res);
            chr.getMap().broadcastMessage(chr,
                    CField.moveHaku(chr.getId(), chr.getHaku().getObjectId(), 0, mPos, new Point(0, 0), res), false);
        }
    }

    // TODO 花狐動作
    public static void HakuAction(LittleEndianAccessor slea, MapleCharacter chr) {
        int oid = slea.readInt();
        int unk1 = slea.readInt();
        int unk2 = slea.readInt();
        MapleHaku haku = chr.getHaku();
        if (haku == null || haku.getObjectId() != oid) {
            return;
        }
        if (chr.isShowInfo()) {
            chr.showInfo("花狐動作", false, "Oid::" + oid + " unk1::" + unk1 + " unk2::" + unk2);
        }
    }

    public static void HakuUseBuff(LittleEndianAccessor slea, MapleCharacter chr) {
        Point pos = slea.readPos();
        int mode = slea.readInt();
        byte unk2 = slea.readByte();
        byte unk3 = slea.readByte();
        byte unk4 = slea.readByte();
        MapleHaku haku = chr.getHaku();
        if (haku == null || !haku.isFigureHaku()) {
            return;
        }
        if (chr.isShowInfo()) {
            chr.showInfo("花狐使用Buff", false, "pos::(" + pos.x + "," + pos.y + ") mode::" + mode + " unk2::" + unk2
                    + " unk3::" + unk3 + " unk4::" + unk4);
        }

        int skillId = 0;
        switch (mode) {
            case 1:
                skillId = 42121020; // 花狐的回復
                break;
            case 3:
                skillId = 42121021; // 花炎結界
                break;
            case 4:
                skillId = 42121022; // 花狐的祝福
                break;
            case 5:
                skillId = 42121023; // 幽玄氣息
                break;
            default:
                chr.dropMessage(1, "[Error]請將訊息反饋給管理員:\r\n[" + pos.x + "," + pos.y + "][" + mode + "][" + unk2 + "][" + unk3
                        + "][" + unk4 + "]");
                return;
        }
        Skill skill = SkillFactory.getSkill(skillId);
        if (skill == null) {
            return;
        }
        MapleStatEffect effect = skill.getEffect(chr.getTotalSkillLevel(skillId));
        if (effect == null) {
            return;
        }
        if (!chr.skillisCooling(skillId)) {
            effect.applyTo(chr);
            if (effect.getCooldown(chr) > 0) {
                chr.addCooldown(skillId, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
            }
        }

        chr.getMap().broadcastMessage(chr, CField.hakuUseBuff(chr.getId()), true);
        chr.getClient().getSession().writeAndFlush(CField.EffectPacket.showEffect(true, chr,
                UserEffectOpcode.User_ActionSetUsed, new int[]{0, mode, unk2, unk3, unk4}, null, null, null));
        chr.getMap().broadcastMessage(chr, CField.EffectPacket.showEffect(false, chr,
                UserEffectOpcode.User_ActionSetUsed, new int[]{0, mode, unk2, unk3, unk4}, null, null, null), false);
    }

    public static void ChangeEmotion(final int emote, final MapleCharacter chr) {
        if (emote > 7) {
            final int emoteid = 5159992 + emote;
            final MapleInventoryType type = GameConstants.getInventoryType(emoteid);
            if (chr.getInventory(type).findById(emoteid) == null) {
                chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM,
                        Integer.toString(emoteid));
                return;
            }
        }
        if (emote > 0 && chr != null && chr.getMap() != null) { // O_o
            if (chr.isHidden()) {
                chr.getMap().broadcastGMMessage(chr, CField.facialExpression(chr, emote), false);
            } else {
                chr.getMap().broadcastMessage(chr, CField.facialExpression(chr, emote), false);
            }
            List<WeakReference<MapleCharacter>> clones = chr.getClones();
            for (int i = 0; i < clones.size(); i++) {
                if (clones.get(i).get() != null) {
                    final MapleCharacter clone = clones.get(i).get();
                    CloneTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            if (chr.isHidden()) {
                                chr.getMap().broadcastGMMessage(null, CField.facialExpression(clone, emote), true);
                            } else {
                                clone.getMap().broadcastMessage(CField.facialExpression(clone, emote));
                            }
                        }
                    }, 500 * i + 500);
                }
            }
        }
    }

    public static void Heal(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        chr.updateTick(slea.readInt());
        if (slea.available() >= 8L) {
            slea.skip(slea.available() >= 12L ? 8 : 4);
        }
        int healHP = slea.readShort();
        int healMP = slea.readShort();

        PlayerStats stats = chr.getStat();

        if (stats.getHp() <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((healHP != 0) && (chr.canHP(now + 1000L))) {
            if (healHP > stats.getHealHP()) {
                healHP = (int) stats.getHealHP();
            }
            chr.addHP(healHP);
        }
        if ((healMP != 0) && (!MapleJob.is惡魔殺手(chr.getJob())) && (chr.canMP(now + 1000L))) {
            if (healMP > stats.getHealMP()) {
                healMP = (int) stats.getHealMP();
            }
            chr.addMP(healMP);
        }
    }

    public static void MovePlayer(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(10);
        int duration = slea.readInt();
        Point mPos = slea.readPos();
        Point oPos = slea.readPos();

        if (chr == null) {
            return;
        }
        final Point Original_Pos = chr.getPosition();
        List<LifeMovementFragment> res;
        try {
            res = MovementParse.parseMovement(slea, 1, chr);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println(new StringBuilder().append("AIOBE Type1:").toString());
            return;
        }

        if (res != null && c.getPlayer().getMap() != null) {
            if (slea.available() != 0) {
                System.err.println("角色移動錯誤: 玩家" + chr.getName() + "(" + MapleJob.getName(MapleJob.getById(chr.getJob()))
                        + ") slea.available != 0 剩餘封包長度: " + slea.available());
                if (chr.isShowErr()) {
                    chr.showInfo("移動", true, "角色移動錯誤: slea.available != 0 剩餘封包長度: " + slea.available());
                }
                FileoutputUtil.log(FileoutputUtil.Movement_Log,
                        "角色移動錯誤: 玩家" + chr.getName() + "(" + MapleJob.getName(MapleJob.getById(chr.getJob()))
                        + ") slea.available != 0 封包: " + slea.toString(true));
                return;
            }
            final MapleMap map = c.getPlayer().getMap();

            if (chr.isHidden()) {
                chr.setLastRes(res);
                c.getPlayer().getMap().broadcastGMMessage(chr,
                        CField.movePlayer(chr.getId(), duration, Original_Pos, oPos, res), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(),
                        CField.movePlayer(chr.getId(), duration, Original_Pos, oPos, res), false);
            }

            MovementParse.updatePosition(res, chr, 0);
            final Point pos = chr.getTruePosition();
            map.movePlayer(chr, pos);
            if ((chr.getFollowId() > 0) && (chr.isFollowOn()) && (chr.isFollowInitiator())) {
                MapleCharacter fol = map.getCharacterById(chr.getFollowId());
                if (fol != null) {
                    Point original_pos = fol.getPosition();
                    fol.getClient().getSession()
                            .writeAndFlush(CField.moveFollow(duration, Original_Pos, original_pos, pos, res));
                    MovementParse.updatePosition(res, fol, 0);
                    map.movePlayer(fol, pos);
                    map.broadcastMessage(fol, CField.movePlayer(fol.getId(), duration, original_pos, oPos, res), false);
                } else {
                    chr.checkFollow();
                }
            }
            List<WeakReference<MapleCharacter>> clones = chr.getClones();
            for (int i = 0; i < clones.size(); i++) {
                if (clones.get(i).get() != null) {
                    final MapleCharacter clone = clones.get(i).get();
                    final List<LifeMovementFragment> res3 = res;
                    Timer.CloneTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (clone.getMap() == map) {
                                    if (clone.isHidden()) {
                                        map.broadcastGMMessage(clone,
                                                CField.movePlayer(clone.getId(), duration, Original_Pos, oPos, res3),
                                                false);
                                    } else {
                                        map.broadcastMessage(clone,
                                                CField.movePlayer(clone.getId(), duration, Original_Pos, oPos, res3),
                                                false);
                                    }
                                    MovementParse.updatePosition(res3, clone, 0);
                                    map.movePlayer(clone, pos);
                                }
                            } catch (Exception e) {
                                // very rarely swallowed
                            }
                        }
                    }, 500 * i + 500);
                }
            }

            int count = c.getPlayer().getFallCounter();
            boolean samepos = (pos.y > c.getPlayer().getOldPosition().y)
                    && (Math.abs(pos.x - c.getPlayer().getOldPosition().x) < 5);
            if ((samepos) && ((pos.y > map.getBottom() + 250) || (map.getFootholds().findBelow(pos, false) == null))) {
                if (count > 5) {
                    c.getPlayer().changeMap(map, map.getPortal(0));
                    c.getPlayer().setFallCounter(0);
                } else {
                    count++;
                    c.getPlayer().setFallCounter(count);
                }
            } else if (count > 0) {
                c.getPlayer().setFallCounter(0);
            }
            c.getPlayer().setOldPosition(pos);
            if ((!samepos) && (c.getPlayer().getBuffSource(MapleBuffStat.DARK_AURA_OLD) == 32120000)) {
                c.getPlayer().getStatForBuff(MapleBuffStat.DARK_AURA_OLD).applyMonsterBuff(c.getPlayer());
            } else if ((!samepos) && (c.getPlayer().getBuffSource(MapleBuffStat.YELLOW_AURA_OLD) == 32120001)) {
                c.getPlayer().getStatForBuff(MapleBuffStat.YELLOW_AURA_OLD).applyMonsterBuff(c.getPlayer());
            }
        }
    }

    public static void ChangeMapSpecial(String portal_name, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        MaplePortal portal = chr.getMap().getPortal(portal_name);

        // if (chr.getGMLevel() > ServerConstants.PlayerGMRank.GM.getLevel()) {
        // chr.dropMessage(6, new
        // StringBuilder().append(portal.getScriptName()).append("
        // accessed").toString());
        // }
        if ((portal != null) && (!chr.hasBlockedInventory())) {
            portal.enterPortal(c);
        } else {
            c.announce(CWvsContext.enableActions());
        }
    }

    public static void ChangeMap(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        if (slea.available() != 0L) {
            slea.readByte();
            int targetid = slea.readInt();
            // slea.readInt();
            MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
            if (slea.available() >= 7L) {
                chr.updateTick(slea.readInt());
            }
            slea.skip(1);
            int wheelType = slea.readShort();
            boolean wheel;
            if (chr.getAndroid() != null
                    && (chr.getAndroid().getItemId() == 1662072 || chr.getAndroid().getItemId() == 1662073)) {
                wheel = wheelType == 7;
            } else if (StringTool.parseBoolean(chr.getTempValue("靈魂之石"))) {
                wheel = wheelType > 0;
                chr.setTempValue("靈魂之石", null);
            } else {
                wheel = wheelType > 0 && chr.haveItem(5510000, 1, false, true);
            }
            wheel = wheel && !GameConstants.isEventMap(chr.getMapId()) && chr.getMapId() / 1000000 != 925;

            if (targetid != -1 && !chr.isAlive()) {
                chr.setStance(0);
                if (chr.getEventInstance() != null && chr.getEventInstance().revivePlayer(chr) && chr.isAlive()) {
                    return;
                }
                if (chr.getPyramidSubway() != null) {
                    chr.getStat().setHp(50, chr);
                    chr.getPyramidSubway().fail(chr);
                    return;
                }

                if (!wheel) {
                    if ((chr.getMapId() == 321001100 || chr.getMapId() == 321001000) && MapleJob.is神之子(chr.getJob())) {
                        chr.getStat().setHp(chr.getStat().getMaxHp(), chr);
                    } else {
                        chr.getStat().setHp(50, chr);
                    }

                    MapleMap to = chr.getMap().getReturnMap();
                    chr.changeMap(to, to.getPortal(0));
                } else {
                    if (chr.getAndroid() != null
                            && (chr.getAndroid().getItemId() == 1662072 || chr.getAndroid().getItemId() == 1662073)) {
                        c.announce(CField.EffectPacket.useAmulet(4, (byte) 0, (byte) 0, chr.getAndroid().getItemId()));
                        MapleItemInformationProvider.getInstance().getItemEffect(2002100).applyTo(chr);
                    } else {
                        c.announce(CField.EffectPacket.useAmulet(2,
                                (byte) (chr.getInventory(MapleInventoryType.CASH).countById(5510000) - 1), (byte) 0,
                                5510000));
                        MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);
                    }
                    chr.getStat().setHp(chr.getStat().getCurrentMaxHp(), chr);
                    chr.getStat().setMp(chr.getStat().getCurrentMaxMp(), chr);
                    chr.updateSingleStat(MapleStat.HP, chr.getStat().getCurrentMaxHp());
                    chr.updateSingleStat(MapleStat.MP, chr.getStat().getCurrentMaxMp());

                    MapleMap to = chr.getMap();
                    chr.changeMap(to, to.getPortal(0));
                }
            } else if (targetid != -1) {
                int divi = chr.getMapId() / 100;
                boolean unlock = false;
                boolean warp = false;
                if (chr.getMapId() == 4000005) {
                    warp = targetid == 104000000;
                    unlock = targetid == 104000000;
                } else if (chr.getMapId() == 743020100) {
                    warp = targetid == 743030000;
                } else if (chr.getMapId() == 743020101) {
                    warp = targetid == 743030002;
                } else if (chr.getMapId() == 743020102) {
                    warp = targetid == 743000203;
                } else if (chr.getMapId() == 743020103) {
                    warp = targetid == 743020402;
                } else if (chr.getMapId() == 743020200) {
                    warp = targetid == 743030001;
                } else if (chr.getMapId() == 743020201) {
                    warp = targetid == 743030003;
                } else if (chr.getMapId() == 743020401) {
                    warp = targetid == 743030201;
                } else if (chr.getMapId() == 743020400) {
                    warp = targetid == 743020000;
                } else if (chr.getMapId() == 912060300) {
                    warp = targetid == 912060400;
                } else if (chr.getMapId() == 912060400) {
                    warp = targetid == 912060500;
                } else if (chr.getMapId() == 913070071) {
                    warp = targetid == 130000000;
                    unlock = true;
                } else if (divi == 9130401) {
                    warp = (targetid / 100 == 9130400) || (targetid / 100 == 9130401);
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9130400) {
                    warp = (targetid / 100 == 9130400) || (targetid / 100 == 9130401);
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9140900) {
                    warp = (targetid == 914090011) || (targetid == 914090012) || (targetid == 914090013)
                            || (targetid == 140090000);
                } else if ((divi == 9120601) || (divi == 9140602) || (divi == 9140603) || (divi == 9140604)
                        || (divi == 9140605)) {
                    warp = (targetid == 912060100) || (targetid == 912060200) || (targetid == 912060300)
                            || (targetid == 912060400) || (targetid == 912060500) || (targetid == 3000100);
                    unlock = true;
                } else if (divi == 9101500) {
                    warp = (targetid == 910150006) || (targetid == 101050010);
                    unlock = true;
                } else if ((divi == 9140901) && (targetid == 140000000)) {
                    unlock = true;
                    warp = true;
                } else if ((divi == 9240200) && (targetid == 924020000)) {
                    unlock = true;
                    warp = true;
                } else if ((targetid == 980040000) && (divi >= 9800410) && (divi <= 9800450)) {
                    warp = true;
                } else if ((divi == 9140902) && ((targetid == 140030000) || (targetid == 140000000))) {
                    unlock = true;
                    warp = true;
                } else if ((divi == 9000900) && (targetid / 100 == 9000900) && (targetid > chr.getMapId())) {
                    warp = true;
                } else if ((divi / 1000 == 9000) && (targetid / 100000 == 9000)) {
                    unlock = (targetid < 900090000) || (targetid > 900090004);
                    warp = true;
                } else if ((divi / 10 == 1020) && (targetid == 1020000 || targetid == 4000026)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 900090101) && (targetid == 100030100)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 2010000) && (targetid == 104000000)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 106020001) || (chr.getMapId() == 106020502)) {
                    if (targetid == chr.getMapId() - 1) {
                        unlock = true;
                        warp = true;
                    }
                } else if ((chr.getMapId() == 0) && (targetid == 10000)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 931000011) && (targetid == 931000012)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 931000021) && (targetid == 931000030)) {
                    unlock = true;
                    warp = true;
                }
                if (unlock) {
                    c.announce(CField.UIPacket.lockUI(false));
                    c.announce(CField.UIPacket.disableOthers(false));
                    c.announce(CField.UIPacket.lockKey(false));
                    c.announce(CWvsContext.enableActions());
                }
                if (warp) {
                    MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                } else if (chr.isShowInfo()) {
                    chr.showInfo("未觸發傳送", true, "unlock-" + unlock + " warp-" + warp + " targetid-" + targetid);
                    c.announce(CWvsContext.enableActions());
                }
            } else if ((portal != null) && (!chr.hasBlockedInventory())) {
                portal.enterPortal(c);
            } else {
                c.announce(CWvsContext.enableActions());
            }
        }
    }

    public static void InnerPortal(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(1);
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
        int toX = slea.readShort();
        int toY = slea.readShort();

        if (portal == null) {
            return;
        }
        if (portal.getPosition().distanceSq(chr.getTruePosition()) > 22500.0D) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
            return;
        }
        chr.getMap().movePlayer(chr, new Point(toX, toY));
        chr.checkFollow();
    }

    public static void snowBall(LittleEndianAccessor slea, MapleClient c) {
        c.announce(CWvsContext.enableActions());
    }

    public static void leftKnockBack(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getMapId() / 10000 == 10906) {
            c.announce(CField.leftKnockBack());
            c.announce(CWvsContext.enableActions());
        }
    }

    public static void ReIssueMedal(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        MapleQuest q = MapleQuest.getInstance(slea.readInt());
        int itemid = slea.readInt();
        if (q == null || q.getMedalItem() != itemid || itemid <= 0 || chr.getQuestStatus(q.getId()) != 2) {
            c.announce(CField.UIPacket.reissueMedal(itemid, 4));
            return;
        }
        if (chr.haveItem(itemid)) {
            c.announce(CField.UIPacket.reissueMedal(itemid, 3));
            return;
        }
        if (!MapleInventoryManipulator.checkSpace(c, itemid, 1, "")) {
            c.announce(CField.UIPacket.reissueMedal(itemid, 2));
            return;
        }
        int count = StringTool.parseInt(chr.getOneInfo(GameConstants.重新補發勳章, "count"));
        int price = 100 * (int) Math.pow(10, Math.min(count, 4));
        if (chr.getMeso() < price) {
            c.announce(CField.UIPacket.reissueMedal(itemid, 1));
            return;
        }
        chr.gainMeso(-price, true, true);
        MapleInventoryManipulator.addById(c, itemid, (byte) 1, new StringBuilder().append("從任務[").append(q)
                .append("]重新補發, 時間").append(FileoutputUtil.CurrentReadable_Date()).toString());
        chr.updateOneInfo(GameConstants.重新補發勳章, "count", String.valueOf(++count));
        c.announce(CField.UIPacket.reissueMedal(itemid, 0));
    }

    public static void MessengerRanking(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        c.announce(CField.messengerOpen(slea.readByte(), null));
    }

    public static void PlayerUpdate(MapleCharacter chr) {
        // boolean autoSave = true;
        if (chr == null || chr.getMap() == null) {
            return;
        }
        if (chr.getCheatTracker().canSaveDB()) {
            long startTime = System.currentTimeMillis();
            chr.saveToDB(false, false);
            if (chr.isShowInfo()) {
                chr.dropMessage(-11, "保存數據，耗時 " + (System.currentTimeMillis() - startTime) + " 毫秒");
            }
        } else if (chr.isShowInfo()) {
            chr.dropMessage(-11, "保存數據，距上次距保存經過了 " + chr.getCheatTracker().getlastSaveTime() + " 秒");
        }
    }

    public static void LoadPlayerSuccess(MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        String msg = ServerConstants.WELCOME_MSG.replaceAll("# 0#", c.getChannelServer().getServerName());
        if (!msg.isEmpty()) {
            c.announce(CField.sendHint(msg, 200, 5));
            // if (!GameConstants.isTutorialMap(chr.getMapId())) {
            // c.announce(CField.startMapEffect(msg, 5122000, true));
            // }
        }
        if (GameConstants.isTutorialMap(chr.getMapId())) {
            chr.dropMessage(5, "你的經驗倍率將會被設置為1倍，直到您完成出生劇情為止。");
        }
        chr.dropMessage(5, "在'快速移動'裡有很多功能哦，有空可以看看！");

        chr.iNeedSystemProcess();
        if (chr.checkMaxStat()) {
            chr.resetStats(4, 4, 4, 4, true);
            c.announce(CWvsContext.broadcastMsg(1, "[警告] 由於您的屬性點有錯誤,系統將自動重置你的屬性點!"));
        }
    }

    public static void GuildTransfer(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int action = slea.readShort();

        switch (action) {
            case 0:
                GrowHelpFactory gh = GrowHelpFactory.getInstance();
                int mapid = slea.readInt();
                if (gh.canEnterMap(chr, mapid)) {
                    chr.changeMap(ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(mapid));
                }
                switch (mapid) {
                    case 102000003:
                        NPCScriptManager.getInstance().start(c, 10202);
                        break;
                }
                break;
            case 2:
                c.announce(CWvsContext.enableActions());
                break;
        }
    }

    public static void getEventList(MapleClient c) {
        c.announce(CWvsContext.getEventList(false));
    }

    public static class ZeroHandler {

        private static int posz;
        private static int typez;
        private static int posz1;

        public static void ZeroScrollStart(final LittleEndianAccessor slea, final MapleCharacter chr,
                final MapleClient c) {
            c.announce(CField.ZeroPacket.UseWeaponScrollStart());
            Equip equip1;
            Equip equip2;
            equip1 = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            equip2 = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            Equip nEquip2 = (Equip) equip2;
            if (typez == 2) {
                if (equip1.getItemId() == 1560000) {
                } else if (equip2.getItemId() % 1572000 >= 0 && equip2.getItemId() % 1572000 <= 7) {
                    c.announce(CWvsContext.enableActions());
                    c.announce(CField.enchantResult(posz, nEquip2.getItemId()));
                    chr.dropMessage(5, "" + equip1.getItemId() + equip2.getItemId());
                }
            }
        }

        public static void openWeaponUI(final LittleEndianAccessor slea, final MapleClient c) {
            int type = slea.readInt();
            byte type2 = slea.readByte();
            if (type2 == 0) {
                c.announce(CField.ZeroPacket.OpenWeaponUI(type));
            } else {

            }
        }

        public static void talkZeroNpc(final LittleEndianAccessor slea, final MapleClient c) {
            c.announce(CField.CScriptMan.NPCTalk());
            c.announce(CWvsContext.enableActions());
        }

        public static void useZeroScroll(final LittleEndianAccessor slea, final MapleClient c) {
            int type = slea.readInt();
            typez = type;
            int pos = slea.readInt();
            posz = pos;
            slea.skip(8);
            int Success = slea.readInt();

            c.announce(CField.ZeroPacket.UseWeaponScroll(Success));
        }

        public static void openZeroUpgrade(final LittleEndianAccessor slea, final MapleClient c) {
            MapleCharacter player = c.getPlayer();
            Equip alpha;
            alpha = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            Equip ep = (Equip) alpha;
            int action = 1, level = 0, type = 0;
            switch (ep.getItemId()) {
                case 1562000:
                    type = 1;
                    level = 100;
                    break;
                case 1562001:
                    type = 2;
                    level = 110;
                    break;
                case 1562002:
                    type = 2;
                    level = 120;
                    break;
                case 1562003:
                    type = 2;
                    level = 130;
                    break;
                case 1562004:
                    type = 4;
                    level = 140;
                    break;
                case 1562005:
                    type = 5;
                    level = 150;
                    break;
                case 15602006:
                    type = 6;
                    level = 160;
                    break;
                case 1562007:
                    action = 0;
                    type = 0;
                    level = 0;
                    break;
            }
            if (player.getLevel() < level) {
                action = 0;
            }
            c.announce(CField.ZeroPacket.OpenZeroUpgrade(type, level, action, ep.getItemId()));
        }

    }

    public static void EffectSwitch(final LittleEndianAccessor slea, final MapleClient c) {
        int pos = slea.readInt();
        c.getPlayer().updateEffectSwitch(pos);
        if (!c.getPlayer().isHidden()) {
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(),
                    CField.getEffectSwitch(c.getPlayer().getId(), c.getPlayer().getEffectSwitch()), true);
        } else {
            c.getPlayer().getMap().broadcastGMMessage(c.getPlayer(),
                    CField.getEffectSwitch(c.getPlayer().getId(), c.getPlayer().getEffectSwitch()), true);
        }
    }

    public static void SaveDamageSkin(final LittleEndianAccessor slea, final MapleClient c) {
        int damageSkin = slea.readInt();
        MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        List<Integer> saveDamageSkin = chr.getSaveDamageSkin();
        if (damageSkin == 0 || damageSkin == 1050
                || (saveDamageSkin != null && (saveDamageSkin.contains(damageSkin) || saveDamageSkin.size() > 28))) {
            return;
        }
        chr.updateSaveDamageSkin(damageSkin);
        c.announce(CWvsContext.getSaveDamageSkin(chr.getDamageSkin(), chr.getSaveDamageSkin()));
    }

    public static void ChangeDamageSkin(final LittleEndianAccessor slea, final MapleClient c) {
        int damageSkin = slea.readInt();
        MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        List<Integer> saveDamageSkin = chr.getSaveDamageSkin();
        if (damageSkin != 0 && damageSkin != 1050 && (saveDamageSkin == null || !saveDamageSkin.contains(damageSkin))) {
            return;
        }
        if (chr.getMeso() < 100000) {
            chr.dropMessage(1, "楓幣不足。");
            return;
        }
        chr.gainMeso(-100000, false);
        chr.setDamageSkin(damageSkin);
        c.announce(CWvsContext.getChangeDamageSkin());
        byte[] packet = CField.updateDamageSkin(chr.getId(), damageSkin);
        if (chr.isHidden()) {
            chr.getMap().broadcastGMMessage(chr, packet, false);
        } else {
            chr.getMap().broadcastMessage(chr, packet, false);
        }
        List<WeakReference<MapleCharacter>> clones = chr.getClones();
        for (WeakReference<MapleCharacter> cln : clones) {
            if (cln.get() != null) {
                final MapleCharacter clone = cln.get();
                byte[] packetClone = CField.updateDamageSkin(clone.getId(), damageSkin);
                if (!clone.isHidden()) {
                    clone.getMap().broadcastMessage(clone, packetClone, false);
                } else {
                    clone.getMap().broadcastGMMessage(clone, packetClone, false);
                }
            }
        }
    }

    public static void RemoveDamageSkin(final LittleEndianAccessor slea, final MapleClient c) {
        int damageSkin = slea.readInt();
        MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        List<Integer> saveDamageSkin = chr.getSaveDamageSkin();
        if (saveDamageSkin == null || !saveDamageSkin.contains(damageSkin)) {
            return;
        }
        chr.updateSaveDamageSkin(damageSkin);
        c.announce(CWvsContext.getRemoveDamageSkin(chr.getDamageSkin(), chr.getSaveDamageSkin()));
    }

    public static void UpdatePinkbeenYoyo(final MapleClient c) {
        Skill skill = SkillFactory.getSkill(131001010);
        if (skill == null) {
            return;
        }
        MapleCharacter chr = c.getPlayer();
        Integer value = null;
        try {
            value = chr.getBuffedValue(MapleBuffStat.PinkbeanYoYoStack);
        } catch (Exception ex) {
        }
        MapleStatEffect effect = skill.getEffect(chr.getTotalSkillLevel(skill));
        if (effect == null) {
            return;
        }
        if (value == null) {
            effect.applyTo(chr);
            return;
        }

        final long start = c.getPlayer().getBuffedStartTime(MapleBuffStat.PinkbeanYoYoStack);
        if ((System.currentTimeMillis() - start) < 7000 || value >= 8) {
            return;
        }
        chr.setBuffedValue(MapleBuffStat.PinkbeanYoYoStack, Math.min(8, ++value));
        effect.applyTo(chr);
    }

    public static void MonsterBan(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null || slea.available() < 4) {
            return;
        }

        int mobid = slea.readInt();
        for (MapleMonster mob : chr.getMap().getAllMonster()) {
            if (mob.getId() == mobid) {
                MobSkill mobSkill = MobSkillFactory.getMobSkill(129, 1);
                if (mobSkill != null) {
                    mobSkill.applyEffect(chr, mob, false);
                }
                break;
            }
        }
    }

    public static void RequestCharacterPotentialSkillRandSet(LittleEndianAccessor slea, MapleClient c,
            MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory() || chr.inPVP()) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        int itemId = slea.readInt();
        byte slot = (byte) slea.readInt();
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem((short) slot);
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId / 1000 != 2702) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        chr.renewInnerSkills(itemId);
        chr.equipChanged();
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, toUse.getPosition(), (byte) 1, false);
        c.announce(CWvsContext.enableActions());
    }

    public static void RequestCharacterPotentialSkillRandSetUI(LittleEndianAccessor slea, MapleClient c,
            MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null || chr.hasBlockedInventory() || chr.inPVP()) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        boolean isLock = slea.readInt() > 0;
        List<Integer> lockPosition = new ArrayList<>();
        if (isLock) {
            int lockNum = slea.readInt();
            for (int i = 0; i < lockNum; i++) {
                lockPosition.add(slea.readInt());
            }
        }
        int cost = 100;
        int honor = chr.getHonourExp();
        int rank = chr.getInnerRank();
        if (isLock) {
            switch (rank) {
                case 1:
                    cost += 400;
                    break;
                case 2:
                    cost += 5000;
                    break;
                case 3:
                    cost += 10000;
                    break;
                default:
                    break;
            }
            switch (lockPosition.size()) {
                case 1:
                    cost += 3000;
                    break;
                case 2:
                    cost += 8000;
                    break;
            }
        }
        if (honor < cost || (isLock && (rank < 1 || lockPosition.size() > 2))) {
            c.announce(CField.gameMsg("重新設定能力失敗。"));
            c.announce(CWvsContext.enableActions());
            return;
        }
        chr.setHonourExp(honor - cost);
        c.announce(CWvsContext.updateHonorExp(chr.getHonourExp()));
        chr.renewInnerSkills(isLock ? -2 : 0, lockPosition);
        chr.equipChanged();
        c.announce(CField.gameMsg("重新設定能力成功。"));
        c.announce(CWvsContext.enableActions());
    }

    public static void DestroyPetItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.readInt();
        long uniqueId = slea.readLong();
        if (c == null || chr == null) {
            if (c != null) {
                c.announce(CWvsContext.enableActions());
            }
            return;
        }
        MapleInventory inventory = chr.getInventory(MapleInventoryType.CASH);
        Item item = inventory.findByUniqueId((int) uniqueId);
        if (item == null) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        inventory.removeItem(item.getPosition());
        c.announce(CWvsContext.InventoryPacket.clearInventoryItem(inventory.getType(), item.getPosition(), false));
        c.announce(CWvsContext.InfoPacket.itemExpired(item.getItemId()));
        c.announce(CWvsContext.enableActions());
    }

    public static void SetSonOfLinkedSkill(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        chr.dropMessage(1, "當前版本暫時無法傳授技能。");
        // if (chr == null || chr.getMap() == null || chr.hasBlockedInventory() ||
        // chr.getLevel() < 70) {
        // c.announce(CWvsContext.enableActions());
        // return;
        // }
        // int skillId = slea.readInt();
        // if (chr.getSkillLevel(skillId) < 1) {
        // c.announce(CWvsContext.enableActions());
        // return;
        // }
        // int toChrId = slea.readInt();
        // Pair<String, Integer> toChrInfo = MapleCharacterUtil.getNameById(toChrId, chr.getWorld());
        // if (toChrInfo == null) {
        // c.announce(CWvsContext.enableActions());
        // return;
        // }
        // int toChrAccId = toChrInfo.getRight();
        // String toChrName = toChrInfo.getLeft();
        // MapleQuest quest = MapleQuest.getInstance(7783);
        // if (quest != null && chr.getAccountID() == toChrAccId) {
        // int toSkillId;
        // if (MapleJob.is重砲指揮官(chr.getJob())) {
        // toSkillId = 80000000; // 百烈祝福
        // } else if (MapleJob.is蒼龍俠客(chr.getJob())) {
        // toSkillId = 80001151; // 寶盒的護佑
        // } else if (MapleJob.is聖魂劍士(chr.getJob())) {
        // toSkillId = 80000066; // 西格諾斯祝福(聖魂)
        // } else if (MapleJob.is烈焰巫師(chr.getJob())) {
        // toSkillId = 80000067; // 西格諾斯祝福(烈焰)
        // } else if (MapleJob.is破風使者(chr.getJob())) {
        // toSkillId = 80000068; // 西格諾斯祝福(破風)
        // } else if (MapleJob.is暗夜行者(chr.getJob())) {
        // toSkillId = 80000069; // 西格諾斯祝福(暗夜)
        // } else if (MapleJob.is閃雷悍將(chr.getJob())) {
        // toSkillId = 80000070; // 西格諾斯祝福(閃雷)
        // } else if (MapleJob.is精靈遊俠(chr.getJob())) {
        // toSkillId = 80001040; // 精靈的祝福
        // } else if (MapleJob.is幻影俠盜(chr.getJob())) {
        // toSkillId = 80000002; // 致命本能
        // } else if (MapleJob.is夜光(chr.getJob())) {
        // toSkillId = 80000005; // 波米艾特
        // } else if (MapleJob.is隱月(chr.getJob())) {
        // toSkillId = 80000169; // 死裡逃生
        // } else if (MapleJob.is惡魔殺手(chr.getJob())) {
        // toSkillId = 80000001; // 後續待發
        // } else if (MapleJob.is惡魔復仇者(chr.getJob())) {
        // toSkillId = 80000050; // 狂暴鬥氣
        // } else if (MapleJob.is傑諾(chr.getJob())) {
        // toSkillId = 80000047; // 合成邏輯
        // } else if (MapleJob.is劍豪(chr.getJob())) {
        // toSkillId = 80000003; // 疾風傳授
        // } else if (MapleJob.is陰陽師(chr.getJob())) {
        // toSkillId = 80000004; // 紫扇傳授
        // } else if (MapleJob.is米哈逸(chr.getJob())) {
        // toSkillId = 80001140; // 光之守護
        // } else if (MapleJob.is凱撒(chr.getJob())) {
        // toSkillId = 80000006; // 鋼鐵意志
        // } else if (MapleJob.is天使破壞者(chr.getJob())) {
        // toSkillId = 80001155; // 靈魂契約
        // } else if (MapleJob.is神之子(chr.getJob())) {
        // toSkillId = 80000110; // 時之祝福
        // } else if (MapleJob.is幻獸師(chr.getJob())) {
        // toSkillId = 80010006; // 精靈集中
        // } else {
        // chr.dropMessage(1, "傳授技能失敗，請將你的職業反饋給管理員。");
        // c.announce(CWvsContext.enableActions());
        // return;
        // }
        //
        // if (chr.setSonOfLinkedSkill(toSkillId, toChrId) > 0 && toSkillId >= 80000000)
        // {
        // chr.changeSonOfLinkedSkill(skillId, toChrId);
        // quest.forceComplete(chr, 0);
        // c.announce(CWvsContext.setSonOfLinkedSkillResult(skillId, toChrId,
        // toChrName));
        // } else {
        // chr.dropMessage(1, "傳授技能失敗角色[" + toChrName + "]已經獲得該技能");
        // }
        // } else {
        // chr.dropMessage(1, "傳授技能失敗。");
        // }
        //
        // c.announce(CWvsContext.enableActions());
    }

    public static void OnUpdateMatrix(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        final List<VMatrixRecord> vmrs = new ArrayList<>();
        VMatrixRecord vmr = null;
        int nType = slea.readInt();
        if (chr.isShowInfo()) {
            chr.dropMessage(6, "[V矩陣操作] OPOCDE：" + nType);
        }
        switch (nType) {
            case 0: {
                final int nSlot = slea.readInt();
                if (nSlot < chr.getVMatrixRecords().size()
                        && chr.getActiveVMatrixRecordsCount() < SkillConstants.getAvailableVCoreSpace(chr.getLevel())) {
                    vmr = chr.getVMatrixRecords().get(nSlot);
                    CoreDataEntry data = VCoreFactory.CoreData.getCoreData(vmr.getIconID());
                    chr.dropMessage(6, "[V矩陣操作] " + data);
                    // 處理強化核心所強化的技能等級
                    if (data.getType() == 2) {
                        for (VMatrixRecord i : chr.getVMatrixRecords()) {
                            CoreDataEntry subData = VCoreFactory.CoreData.getCoreData(i.getIconID());
                            if (subData.getType() == 2 && i.getState() == VCoreRecordState.ACTIVE) {
                                i.setState(VCoreRecordState.INACTIVE);
                            }
                        }
                    }
                    vmr.setState(VCoreRecordState.ACTIVE);
                    chr.updateVSkillRecords();
                    c.announce(CWvsContext.OnVMatrixUpdate(chr.getVMatrixRecords(), 0, nSlot));
                }
                break;
            }
            case 1: {
                final int nSlot = slea.readInt();
                if (nSlot < chr.getVMatrixRecords().size()) {
                    vmr = chr.getVMatrixRecords().get(nSlot);
                    CoreDataEntry data = VCoreFactory.CoreData.getCoreData(vmr.getIconID());
                    chr.dropMessage(6, "[V矩陣操作] " + data);
                    vmr.setState(VCoreRecordState.INACTIVE);
                    chr.updateVSkillRecords();
                    c.announce(CWvsContext.OnVMatrixUpdate(chr.getVMatrixRecords(), 0, nSlot));
                }
                break;
            }
            case 2:
                break;
            case 3: {
                // 強化
                int nSlot = slea.readInt();
                VMatrixRecord vmrFeed = null;
                if (nSlot < chr.getVMatrixRecords().size()) {
                    vmr = chr.getVMatrixRecords().get(nSlot);
                    CoreDataEntry vcore = VCoreFactory.CoreData.getCoreData(vmr.getIconID());
                    int requiredExp = VCoreFactory.Enforcement.getNextExp(vcore.getType(), vmr.getSkillLv());
                    final int nFeedSize = slea.readInt();
                    for (int i = 0; i < nFeedSize; i++) {
                        int nSlotFeed = slea.readInt();
                        if (nSlotFeed < chr.getVMatrixRecords().size()) {
                            vmrFeed = chr.getVMatrixRecords().get(nSlot);
                            vmrs.add(vmrFeed);
                        }
                    }
                    int addedExp = 0;
                    int preLevel = vmr.getSkillLv();
                    for (int i = 0; i < vmrs.size(); i++) {
                        CoreDataEntry vcoreFeed = VCoreFactory.CoreData.getCoreData(vmrs.get(i).getIconID());
                        int enforceExp = VCoreFactory.Enforcement.getExpEnforce(vcoreFeed.getType(), vmrFeed.getSkillLv());
                        addedExp += enforceExp;
                        chr.getVMatrixRecords().remove(vmrs.get(i));
                    }
                    vmr.setExp(vmr.getExp() + addedExp);
                    while (vmr.getExp() >= requiredExp) {
                        vmr.setSkillLv(vmr.getSkillLv() + 1);
                        vmr.setExp(vmr.getExp() - requiredExp);
                        requiredExp = VCoreFactory.Enforcement.getNextExp(vcore.getType(), vmr.getSkillLv());
                    }
                    c.announce(CWvsContext.OnVMatrixUpdate(chr.getVMatrixRecords(), 1, 0));
                    c.announce(CWvsContext.OnVMatrixEnforceResult(nSlot, addedExp, preLevel, vmr.getSkillLv()));
                }
                break;
            }
            case 5:
                final int nSlotSize = slea.readInt();
                int[] nSlots = new int[]{-1, -1, -1};
                for (int i = 0; i < nSlotSize; ++i) {
                    nSlots[i] = slea.readInt();
                    if (nSlots[i] < chr.getVMatrixRecords().size()) {
                        vmrs.add(chr.getVMatrixRecords().get(nSlots[i]));
                    }
                } // 分解
                int extractAdd = 0;
                for (int i = 0; i < vmrs.size(); i++) {
                    if (vmrs.get(i) != null) {
                        CoreDataEntry coreData = VCoreFactory.CoreData.getCoreData(vmrs.get(i).getIconID());
                        int extract = VCoreFactory.Enforcement.getExtract(coreData.getType(), vmrs.get(i).getSkillLv());
                        extractAdd += extract;
                        chr.getVMatrixRecords().remove(vmrs.get(i));
                    }
                }
                chr.addVMatrixShard(extractAdd);
                for (VMatrixRecord i : chr.getVMatrixRecords()) {
                    Skill sk = SkillFactory.getSkill(i.getSkillID1());
                    if (sk != null) {
                        chr.changeSingleSkillLevel(sk, i.isActive() ? i.getSkillLv() : 0, (byte) i.getMasterLv());
                    }
                }
                c.announce(CWvsContext.OnVMatrixUpdate(chr.getVMatrixRecords(), 4, 0));
                c.announce(CWvsContext.OnVMatrixShardUpdate(extractAdd));
                break;
            default:
                break;
        }
    }

    public static void useFamiliarCard(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.readByte();
        int unk = slea.readInt();
        int type = slea.readByte();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (chr.isShowInfo()) {
            chr.dropMessage(-5, "[萌獸系統] 客戶端操作：" + type);
        }
        switch (type) {
            case 10: {
                c.announce(FamiliarPacket.H(chr, slea.readInt()));
                break;
            }
            case 8: {
                short slot = slea.readShort();
                slea.skip(2);
                int index = slea.readInt();
                Item item = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
                if (item != null && item.getItemId() == 5743003) {
                    MonsterFamiliar mf = chr.getFamiliars().get(index);
                    mf.initOptions();
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false, false);
                    c.announce(FamiliarPacket.addFamiliarCard(chr, mf));
                    return;
                }
                c.announce(CWvsContext.enableActions());
                break;
            }
            case 7: {
                int index = slea.readInt();
                slea.skip(4);
                if (!chr.getFamiliars().containsKey(index)) {
                    break;
                }
                if (chr.getFamiliars().remove(index) == null) {
                    break;
                }
                c.announce(FamiliarPacket.showAllFamiliar(chr));
                break;
            }
            case 6: {
                int index = slea.readInt();
                slea.skip(4);
                if (!chr.getFamiliars().containsKey(index)) {
                    return;
                }
                MonsterFamiliar mf = chr.getFamiliars().get(index);
                mf.setName(slea.readMapleAsciiString());
                c.announce(FamiliarPacket.addFamiliarCard(chr, mf));
                break;
            }
            case 5: {
                int index1 = slea.readInt();
                slea.skip(4);
                int index2 = slea.readInt();
                slea.skip(4);
                if (!chr.getFamiliars().containsKey(index1) || !chr.getFamiliars().containsKey(index2)) {
                    return;
                }
                MonsterFamiliar mf1 = chr.getFamiliars().get(index1);
                MonsterFamiliar mf2 = chr.getFamiliars().get(index2);
                if (mf1 == null || mf2 == null) {
                    return;
                }
                if (mf1.getGrade() != mf2.getGrade()) {
                    return;
                }
                if (mf1.getLevel() != 5 || mf2.getLevel() != 5) {
                    return;
                }
                int price = (50000 * (mf2.getGrade() + 1) * 2);
                if (chr.getMeso() < price) {
                    return;
                }
                mf1.upgrade();
                chr.getFamiliars().remove(index2);
                chr.gainMeso(-price, true);
                c.announce(FamiliarPacket.bl(chr));
                c.announce(FamiliarPacket.showAllFamiliar(chr));
                break;
            }
            case 2: {
                short slot = slea.readShort();
                Item item = chr.getInventory(MapleInventoryType.USE).getItem(slot);
                if (item == null) {
                    return;
                }
                if (item.getFamiliarCard() == null) {
                    return;
                }
                int quantity = item.getQuantity();
                if (quantity >= 1 && item.getItemId() / 10000 == 287) {
                    if (chr.getFamiliars().size() >= 60) {
                        chr.dropMessage(1, "萌寵圖鑑數量已經滿了");
                        return;
                    }
                    MonsterFamiliar mf = new MonsterFamiliar(chr.getId(),
                            ItemConstants.getFamiliarByItemID(item.getItemId()), item.getFamiliarCard());
                    mf.setIndex(chr.getFamiliars().size());
                    chr.getFamiliars().put(mf.getId(), mf);
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false, false);
                    c.announce(FamiliarPacket.addFamiliarCard(chr, mf));
                    break;
                }
            }
            case 1: {
                if (slea.available() == 0 && (unk & 1) != 0) {
                    chr.removeFamiliar();
                    return;
                }
                final MonsterFamiliar mf = chr.getSummonedFamiliar();
                HashMap<Integer, Integer> attackInfo = new HashMap<>();
                if (mf == null) {
                    return;
                }
                slea.readInt();
                int count = slea.readByte() / 2;
                int maxDmg = (int) chr.getStat().getCurrentMaxBaseDamage();
                int minDmg = Math.min((int) (maxDmg * maxDmg / 100.0), 0);
                for (int i = 0; i < count; i++) {
                    attackInfo.put(slea.readInt(), (int) (Randomizer.rand(minDmg, maxDmg) * mf.getPad()));
                }
                slea.readByte();
                chr.getMap().broadcastGMMessage(chr, FamiliarPacket.attackFamiliar(chr.getId(), unk, attackInfo), true);
                for (Entry<Integer, Integer> entry : attackInfo.entrySet()) {
                    MapleMonster mon = chr.getMap().getMonsterByOid(entry.getKey());
                    if (mon != null) {
                        mon.damage(chr, entry.getValue(), true);
                    }
                }
                break;
            }
            case 0: {
                int index = slea.readInt();
                if (slea.available() == 4 && (unk & 1) != 0) {
                    chr.removeFamiliar();
                    if (!chr.getFamiliars().containsKey(index)) {
                        return;
                    }
                    chr.spawnFamiliar(chr.getFamiliars().get(index), false);
                    return;
                }
                moveFamiliarHandler(slea, c, chr);
            }
        }
    }

    public static void moveFamiliarHandler(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        // slea.skip((slea.available() < 64L) ? 9 : 10);
        slea.readByte();
        int duration = slea.readInt();
        Point oPos = slea.readPos();
        List<LifeMovementFragment> moves = MovementParse.parseMovement(slea, 6, chr);
        if (chr != null && chr.getSummonedFamiliar() != null && moves.size() > 0) {
            Point pos = chr.getSummonedFamiliar().getPosition();
            MovementParse.updatePosition(moves, (server.maps.AnimatedMapleMapObject) chr.getSummonedFamiliar(), 0);
            chr.getSummonedFamiliar().updatePosition(moves);
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr, FamiliarPacket.moveFamiliar(chr.getId(), duration, pos, oPos, moves),
                        chr.getTruePosition());
            }
        }
    }
}
