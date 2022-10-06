package handling.channel.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleJob;
import client.familiar.MonsterFamiliar;
import client.MonsterStatus;
import client.inventory.MapleInventoryType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import server.MapleInventoryManipulator;
import server.Randomizer;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleNodes;
import server.movement.LifeMovementFragment;
import tools.FileoutputUtil;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.FamiliarPacket;
import tools.packet.MobPacket;

public class MobHandler {

    public static final void MoveMonster(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        MapleMonster monster = chr.getMap().getMonsterByOid(slea.readInt());
        if (monster == null) {
            return;
        }
        if (monster.getLinkCID() > 0) {
            return;
        }
        // dwTemplateID / 0x2710 == 250 || dwTemplateID / 0x2710 == 251
        // slea.readByte();
        short moveid = slea.readShort();
        boolean useSkill = slea.readByte() > 0;
        byte action = slea.readByte(); // bTeleportEnd
        int skill = slea.readInt();

        int skillId = skill & 0xFF000000;
        int skillLevel = skill & 0xFF0000;
        int delay = skill & 0xFFFF;

        int realskill = 0;
        int level = 0;

        if (useSkill) {
            byte size = monster.getNoSkills();
            boolean used = false;

            if (size > 0) {
                Pair<Integer, Integer> skillToUse = monster.getSkills().get((byte) Randomizer.nextInt(size));
                realskill = skillToUse.getLeft();
                level = skillToUse.getRight();

                MobSkill mobSkill = MobSkillFactory.getMobSkill(realskill, level);

                if ((mobSkill != null) && (!mobSkill.checkCurrentBuff(chr, monster))) {
                    long now = System.currentTimeMillis();
                    long ls = monster.getLastSkillUsed(realskill);

                    if ((ls == 0L) || ((now - ls > mobSkill.getCoolTime()) && (!mobSkill.onlyOnce()))) {
                        monster.setLastSkillUsed(realskill, now, mobSkill.getCoolTime());

                        int reqHp = (int) ((float) monster.getHp() / (float) monster.getMobMaxHp() * 100.0F);
                        if (reqHp <= mobSkill.getHP()) {
                            used = true;
                            mobSkill.applyEffect(chr, monster, true);
                        }
                    }
                }
            }
            if (!used) {
                realskill = 0;
                level = 0;
            }
        }
        List<Pair<Integer, Integer>> unk = new ArrayList<>();
        byte size = slea.readByte();
        for (int i = 0; i < size; i++) {
            unk.add(new Pair<>((int) slea.readShort(), (int) slea.readShort()));
        }
        List<Short> unk2 = new ArrayList<>();
        byte size2 = slea.readByte();
        for (int i = 0; i < size2; i++) {
            unk2.add(slea.readShort());
        }
        slea.skip(1);
        int crc0 = slea.readInt();
        boolean skipped = crc0 != 0 && size2 > 0;
        int crc1 = slea.readInt(); // CC DD FF 00 same for all mobs
        int crc2 = slea.readInt(); // CC DD FF 00 same for all mobs
        slea.skip(4); // 9D E1 87 48 same for all mobs

        if (monster.getId() == 9300281 && skipped) {
            if (slea.readByte() > 10) {
                slea.skip(8);
            } else {
                slea.seek(slea.getPosition() - 1L);
            }
        } else {
            int skipcount = 0;
            if (crc0 == 18) {
                skipcount = 2;
            }
            slea.skip(skipcount);
        }

        slea.skip(1); // 1?
        int duration = slea.readInt();
        Point mPos = slea.readPos();
        Point oPos = slea.readPos();

        mPos = monster.getPosition();
        List<LifeMovementFragment> res;
        try {
            res = MovementParse.parseMovement(slea, 2, chr);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("怪物移動錯誤Move_life :  AIOBE Type2");
            if (chr.isShowErr()) {
                chr.showInfo("移動", true, "怪物移動錯誤Move_life : AIOBE Type2");
            }
            FileoutputUtil.log(FileoutputUtil.Movement_Log, "怪物移動錯誤 AIOBE Type2 : 怪物ID " + monster.getId() + "\r\n錯誤訊息:"
                    + e + "\r\n封包:\r\n" + slea.toString(true));
            return;
        }

        if (monster.getController() != c.getPlayer()) {
            if (monster.isAttackedBy(c.getPlayer())) {// aggro and controller change
                monster.switchController(c.getPlayer(), true);
            } else {
                return;
            }
        } else if (action == -1 && monster.isControllerKnowsAboutAggro() && !monster.isFirstAttack()) {
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
        }
        boolean aggro = monster.isControllerHasAggro();
        if (aggro) {
            monster.setControllerKnowsAboutAggro(true);
        }

        if ((MapleJob.is夜光(chr.getJob())) && (Randomizer.isSuccess(20))) {
            chr.applyBlackBlessingBuff(1);
        }

        if (res != null && res.size() > 0) {
            MapleMap map = chr.getMap();
            c.getSession().writeAndFlush(MobPacket.moveMonsterResponse(monster.getObjectId(), moveid, monster.getMp(),
                    monster.isControllerHasAggro(), realskill, level));
            if (slea.available() != 30L) {
                System.err.println("怪物移動錯誤Move_life : slea.available != 30 剩餘封包長度: " + slea.available());
                if (chr.isShowErr()) {
                    chr.showInfo("移動", true, "怪物移動錯誤Move_life : slea.available != 30");
                }
                FileoutputUtil.log(FileoutputUtil.Movement_Log,
                        "怪物移動錯誤: slea.available != 36\r\n怪物ID: " + monster.getId() + "\r\n" + slea.toString(true));
                return;
            }
            MovementParse.updatePosition(res, monster, -1);
            Point endPos = monster.getTruePosition();
            map.moveMonster(monster, endPos);
            map.broadcastMessage(chr, MobPacket.moveMonster(useSkill, action, skill, monster.getObjectId(), duration,
                    mPos, oPos, res, unk2, unk), endPos);
            chr.getCheatTracker().checkMoveMonster(endPos);
        }
    }

    public static final void FriendlyDamage(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        slea.skip(4);
        MapleMonster mobto = map.getMonsterByOid(slea.readInt());

        if ((mobfrom != null) && (mobto != null) && (mobto.getStats().isFriendly())) {
            int damage = mobto.getStats().getLevel() * Randomizer.nextInt(mobto.getStats().getLevel()) / 2;
            mobto.damage(chr, damage, true);
            checkShammos(chr, mobto, map);
        }
    }

    @SuppressWarnings("empty-statement")
    public static final void MobBomb(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        slea.skip(4);
        slea.readInt();

        if ((mobfrom != null) && (mobfrom.getBuff(MonsterStatus.M_AddDamParty) != null))
            ;
    }

    public static final void checkShammos(MapleCharacter chr, MapleMonster mobto, MapleMap map) {
        MapleMap mapp;
        if ((!mobto.isAlive()) && (mobto.getStats().isEscort())) {
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) {
                if ((chrz.getParty() != null) && (chrz.getParty().getLeader().getId() == chrz.getId())) {
                    if (!chrz.haveItem(2022698)) {
                        break;
                    }
                    MapleInventoryManipulator.removeById(chrz.getClient(), MapleInventoryType.USE, 2022698, 1, false,
                            true);
                    mobto.heal((int) mobto.getMobMaxHp(), mobto.getMobMaxMp(), true);
                    return;
                }

            }

            map.broadcastMessage(CWvsContext.broadcastMsg(6, "Your party has failed to protect the monster."));
            mapp = chr.getMap().getForcedReturnMap();
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) {
                chrz.changeMap(mapp, mapp.getPortal(0));
            }
        } else if ((mobto.getStats().isEscort()) && (mobto.getEventInstance() != null)) {
            mobto.getEventInstance().setProperty("HP", String.valueOf(mobto.getHp()));
        }
    }

    public static final void MonsterBomb(int oid, MapleCharacter chr) {
        MapleMonster monster = chr.getMap().getMonsterByOid(oid);

        if ((monster == null) || (!chr.isAlive()) || (chr.isHidden()) || (monster.getLinkCID() > 0)) {
            return;
        }
        byte selfd = monster.getStats().getSelfD();
        if (selfd != -1) {
            chr.getMap().killMonster(monster, chr, false, false, selfd);
        }
    }

    public static final void AutoAggro(int monsteroid, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null) || (chr.isHidden())) {
            return;
        }
        MapleMonster monster = chr.getMap().getMonsterByOid(monsteroid);

        if ((monster != null) && (chr.getTruePosition().distanceSq(monster.getTruePosition()) < 200000.0D)
                && (monster.getLinkCID() <= 0)) {
            if (monster.getController() != null) {
                if (chr.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(chr, true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else {
                monster.switchController(chr, true);
            }
        }
    }

    public static final void HypnotizeDmg(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt());
        slea.skip(4);
        int to = slea.readInt();
        slea.skip(1);
        int damage = slea.readInt();

        MapleMonster mob_to = chr.getMap().getMonsterByOid(to);

        if ((mob_from != null) && (mob_to != null) && (mob_to.getStats().isFriendly())) {
            if (damage > 30000) {
                return;
            }
            mob_to.damage(chr, damage, true);
            checkShammos(chr, mob_to, chr.getMap());
        }
    }

    public static final void DisplayNode(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt());
        if (mob_from != null) {
            chr.getClient().getSession().writeAndFlush(MobPacket.getNodeProperties(mob_from, chr.getMap()));
        }
    }

    public static final void MobNode(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMonster mob_from = chr.getMap().getMonsterByOid(slea.readInt());
        int newNode = slea.readInt();
        int nodeSize = chr.getMap().getNodes().size();
        if ((mob_from != null) && (nodeSize > 0)) {
            MapleNodes.MapleNodeInfo mni = chr.getMap().getNode(newNode);
            if (mni == null) {
                return;
            }
            if (mni.attr == 2) {
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                        chr.getMap().talkMonster("Please escort me carefully.", 5120035, mob_from.getObjectId());
                        break;
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().talkMonster("Please escort me carefully.", 5120051, mob_from.getObjectId());
                }
            }

            mob_from.setLastNode(newNode);
            if (chr.getMap().isLastNode(newNode)) {
                switch (chr.getMapId() / 100) {
                    case 9211200:
                    case 9211201:
                    case 9211202:
                    case 9211203:
                    case 9211204:
                    case 9320001:
                    case 9320002:
                    case 9320003:
                        chr.getMap().broadcastMessage(CWvsContext.broadcastMsg(5, "Proceed to the next stage."));
                        chr.getMap().removeMonster(mob_from);
                }
            }
        }
    }

    public static final void RenameFamiliar(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        MonsterFamiliar mf = c.getPlayer().getFamiliars().get(Integer.valueOf(slea.readInt()));
        String newName = slea.readMapleAsciiString();
        if ((mf != null) && (mf.getName().equals(mf.getOriginalName()))
                && (MapleCharacterUtil.isEligibleCharName(newName, false))) {
            mf.setName(newName);
            c.getSession().writeAndFlush(FamiliarPacket.renameFamiliar(mf));
        } else {
            chr.dropMessage(1, "Name was not eligible.");
        }
        c.getSession().writeAndFlush(CWvsContext.enableActions());
    }

    public static final void SpawnFamiliar(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        // c.getPlayer().updateTick(slea.readInt());
        // int mId = slea.readInt();
        // c.getSession().writeAndFlush(CWvsContext.enableActions());
        // c.getPlayer().removeFamiliar();
        // if ((c.getPlayer().getFamiliars().containsKey(mId)) && (slea.readByte() > 0))
        // {
        // MonsterFamiliar mf = c.getPlayer().getFamiliars().get(Integer.valueOf(mId));
        // if (mf.getFatigue() > 0) {
        // c.getPlayer().dropMessage(1, "Please wait " + mf.getFatigue() + " seconds to
        // summon it.");
        // } else {
        // c.getPlayer().spawnFamiliar(mf, false);
        // }
        // }
    }

    public static final void MoveFamiliar(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(17);
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 6, chr);
        if (chr != null && chr.getSummonedFamiliar() != null && res.size() > 0) {
            Point pos = chr.getSummonedFamiliar().getPosition();
            MovementParse.updatePosition(res, chr.getSummonedFamiliar(), 0);
            chr.getSummonedFamiliar().updatePosition(res);
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr,
                        FamiliarPacket.moveFamiliar(chr.getId(), 0, pos, new Point(0, 0), res), chr.getTruePosition());
            }
        }
    }

    public static final void AttackFamiliar(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        // if (chr.getSummonedFamiliar() == null) {
        // return;
        // }
        // slea.skip(6);
        // int skillid = slea.readInt();
        //
        // SkillFactory.FamiliarEntry f = SkillFactory.getFamiliar(skillid);
        // if (f == null) {
        // return;
        // }
        // byte unk = slea.readByte();
        // byte size = slea.readByte();
        // List<Triple<Integer, Integer, List<Integer>>> attackPair = new
        // ArrayList(size);
        // for (int i = 0; i < size; i++) {
        // int oid = slea.readInt();
        // int type = slea.readInt();
        // slea.skip(10);
        // byte si = slea.readByte();
        // List<Integer> attack = new ArrayList<>(si);
        // for (int x = 0; x < si; x++) {
        // attack.add(slea.readInt());
        // }
        // attackPair.add(new Triple<>(oid, type, attack));
        // }
        // if ((attackPair.isEmpty()) ||
        // (!chr.getCheatTracker().checkFamiliarAttack(chr)) || (attackPair.size() >
        // f.targetCount)) {
        // return;
        // }
        // MapleMonsterStats oStats = chr.getSummonedFamiliar().getOriginalStats();
        // chr.getMap().broadcastMessage(chr, CField.familiarAttack(chr.getId(), unk,
        // attackPair), chr.getTruePosition());
        // for (Triple<Integer, Integer, List<Integer>> attack : attackPair) {
        // MapleMonster mons = chr.getMap().getMonsterByOid(attack.left);
        // if ((mons != null) && (mons.isAlive()) && (!mons.getStats().isFriendly()) &&
        // (mons.getLinkCID() <= 0) && (attack.right.size() <= f.attackCount))
        // {
        // if ((chr.getTruePosition().distanceSq(mons.getTruePosition()) > 640000.0D) ||
        // (chr.getSummonedFamiliar().getTruePosition().distanceSq(mons.getTruePosition())
        // > GameConstants.getAttackRange(f.lt, f.rb))) {
        // chr.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER_SUMMON);
        // }
        // for (Iterator i$ = attack.right.iterator(); i$.hasNext();) {
        // int damage = ((Integer) i$.next());
        // if (damage <= oStats.getPhysicalAttack() * 4) {
        // mons.damage(chr, damage, true);
        // }
        // }
        // if ((f.makeChanceResult()) && (mons.isAlive())) {
        // for (MonsterStatus s : f.status) {
        // mons.applyStatus(chr, new MonsterStatusEffect(s, (int) f.speed,
        // MonsterStatusEffect.genericSkill(s), null, false), false, f.time * 1000,
        // false, null);
        // }
        // if (f.knockback) {
        // mons.switchController(chr, true);
        // }
        // }
        // }
        // }
        // chr.getSummonedFamiliar().addFatigue(chr, attackPair.size());
    }

    public static final void TouchFamiliar(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        // if (chr.getSummonedFamiliar() == null) {
        // return;
        // }
        // slea.skip(6);
        // byte unk = slea.readByte();
        //
        // MapleMonster target = chr.getMap().getMonsterByOid(slea.readInt());
        // if (target == null) {
        // return;
        // }
        // int type = slea.readInt();
        // slea.skip(4);
        // int damage = slea.readInt();
        // int maxDamage =
        // chr.getSummonedFamiliar().getOriginalStats().getPhysicalAttack() * 5;
        // if (damage < maxDamage) {
        // damage = maxDamage;
        // }
        // if ((!target.getStats().isFriendly()) &&
        // (chr.getCheatTracker().checkFamiliarAttack(chr))) {
        // chr.getMap().broadcastMessage(chr, CField.touchFamiliar(chr.getId(), unk,
        // target.getObjectId(), type, 600, damage), chr.getTruePosition());
        // target.damage(chr, damage, true);
        // chr.getSummonedFamiliar().addFatigue(chr);
        // }
    }

    public static final void UseFamiliar(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        // if ((chr == null) || (!chr.isAlive()) || (chr.getMap() == null) ||
        // (chr.hasBlockedInventory())) {
        // c.getSession().writeAndFlush(CWvsContext.enableActions());
        // return;
        // }
        // c.getPlayer().updateTick(slea.readInt());
        // short slot = slea.readShort();
        // int itemId = slea.readInt();
        // Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        //
        // c.getSession().writeAndFlush(CWvsContext.enableActions());
        // if ((toUse == null) || (toUse.getQuantity() < 1) || (toUse.getItemId() !=
        // itemId) || (itemId / 10000 != 287)) {
        // return;
        // }
        // StructFamiliar f =
        // MapleItemInformationProvider.getInstance().getFamiliarByItem(itemId);
        // if (MapleLifeFactory.getMonsterStats(f.getMobId()).getLevel() <=
        // c.getPlayer().getLevel()) {
        // MonsterFamiliar mf = c.getPlayer().getFamiliars().get(f.getGrade());
        // if (mf != null) {
        // if (mf.getVitality() >= 3) {
        // mf.setExpiry(Math.min(System.currentTimeMillis() + 7776000000L,
        // mf.getExpiry() + 2592000000L));
        // } else {
        // mf.setVitality(mf.getVitality() + 1);
        // mf.setExpiry(mf.getExpiry() + 2592000000L);
        // }
        // } else {
        // mf = new MonsterFamiliar(c.getPlayer().getId(),
        // ItemConstants.getFamiliarByItemID(itemId), System.currentTimeMillis() +
        // 2592000000L);
        // c.getPlayer().getFamiliars().put(mf.getId(), mf);
        // }
        // MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot,
        // (short) 1, false, false);
        // c.getSession().writeAndFlush(CField.registerFamiliar(mf));
        // return;
        // }
    }
}
