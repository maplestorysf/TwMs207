package tools.packet;

import client.MonsterStatus;
import client.MonsterStatusEffect;
import constants.GameConstants;
import extensions.temporary.FieldEffectOpcode;
import handling.SendPacketOpcode;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import server.life.EliteMonsterInfo;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleNodes;
import server.movement.LifeMovementFragment;
import tools.HexTool;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

public class MobPacket {

    public static byte[] damageMonster(int oid, long damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobDamaged.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeLong(damage);

        return mplew.getPacket();
    }

    public static byte[] damageFriendlyMob(MapleMonster mob, long damage, boolean display) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobDamaged.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(display ? 1 : 2);
        mplew.writeLong(damage > 2147483647L ? 2147483647 : (int) damage);
        mplew.writeLong(mob.getHp() > 2147483647L ? (int) (mob.getHp() / mob.getMobMaxHp() * 2147483647.0D)
                : (int) mob.getHp());
        mplew.writeLong(mob.getMobMaxHp() > 2147483647L ? 2147483647 : (int) mob.getMobMaxHp());

        return mplew.getPacket();
    }

    public static byte[] killMonster(int oid, int animation, boolean azwan) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (azwan) {
            mplew.writeShort(SendPacketOpcode.LP_MinionLeaveField.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.LP_MobLeaveField.getValue());
        }
        boolean a = false; // idk
        boolean b = false; // idk
        if (azwan) {
            mplew.write(a ? 1 : 0);
            mplew.write(b ? 1 : 0);
        }
        mplew.writeInt(oid);
        if (azwan) {
            if (a) {
                mplew.write(0);
                if (b) {
                    // set mob temporary stat
                } else {
                    // set mob temporary stat
                }
            } else if (b) {
                // idk
            } else {
                // idk
            }
            return mplew.getPacket();
        }
        mplew.write(animation);
        if (animation == 4) {
            mplew.writeInt(-1);
        }

        return mplew.getPacket();
    }

    public static byte[] suckMonster(int oid, int chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobLeaveField.getValue());
        mplew.writeInt(oid);
        mplew.write(4);
        mplew.writeInt(chr);

        return mplew.getPacket();
    }

    public static byte[] healMonster(int oid, int heal) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobDamaged.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeLong(-heal);

        return mplew.getPacket();
    }

    public static byte[] MobToMobDamage(int oid, int dmg, int mobid, boolean azwan) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (azwan) {
            mplew.writeShort(SendPacketOpcode.LP_MinionGenBeyondSplit.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.LP_MobAttackedByMob.getValue());
        }
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(dmg);
        mplew.writeInt(mobid);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] getMobSkillEffect(int oid, int skillid, int cid, int skilllevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobSpecialEffectBySkill.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(skillid);
        mplew.writeInt(cid);
        mplew.writeShort(skilllevel);

        return mplew.getPacket();
    }

    public static byte[] getMobCoolEffect(int oid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobEffectByItem.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] showMonsterHP(int oid, int remhppercentage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobHPIndicator.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);

        return mplew.getPacket();
    }

    public static byte[] showCygnusAttack(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CYGNUS_ATTACK.getValue());
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] showMonsterResist(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_RESIST.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(0);
        mplew.writeShort(1);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] showBossHP(MapleMonster mob) {
        return CField.OnFieldEffect(FieldEffectOpcode.FieldEffect_MobHPTag, null,
                new int[] { mob.getId() == 9400589 ? 9300184 : mob.getId(), mob.getHp() <= 0 ? -1
                        : (int) (mob.getMobMaxHp() > 0 ? (double) mob.getHp() * mob.getMaxHp() / mob.getMobMaxHp()
                                : mob.getHp()),
                        mob.getMaxHp(), mob.getStats().getTagColor(), mob.getStats().getTagBgColor() });
    }

    public static byte[] showBossHP(int monsterId, long currentHp, int maxHp, long finalmaxHP) {
        return CField.OnFieldEffect(FieldEffectOpcode.FieldEffect_MobHPTag, null,
                new int[] { monsterId,
                        currentHp <= 0 ? -1
                                : (int) (finalmaxHP > 0 ? (double) currentHp * maxHp / finalmaxHP : currentHp),
                        maxHp, 6, 5 });
    }

    public static byte[] moveMonster(boolean useskill, int action, int skill, int oid, int duration, Point mPos,
            Point oPos, List<LifeMovementFragment> moves) {
        return moveMonster(useskill, action, skill, oid, duration, mPos, oPos, moves, null, null);
    }

    public static byte[] moveMonster(boolean useskill, int action, int skill, int dwMobID, int duration, Point mPos,
            Point oPos, List<LifeMovementFragment> moves, List<Short> unk2, List<Pair<Integer, Integer>> unk3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobMove.getValue());
        mplew.writeInt(dwMobID);
        mplew.write(useskill); // bNextAttackPossible & 0x1FF
        mplew.write(action);

        mplew.writeInt(skill);
        mplew.write(unk3 == null ? 0 : unk3.size());
        if (unk3 != null) {
            for (Pair<Integer, Integer> i : unk3) {
                mplew.writeShort(i.left);
                mplew.writeShort(i.right);
            }
        }
        mplew.write(unk2 == null ? 0 : unk2.size());
        if (unk2 != null) {
            for (Short i : unk2) {
                mplew.writeShort(i);
            }
        }
        PacketHelper.serializeMovementList(mplew, duration, mPos, oPos, moves);
        mplew.write(false);
        mplew.write(false);

        return mplew.getPacket();
    }

    public static byte[] spawnMonster(MapleMonster life, int spawnType, int link, boolean azwan) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobEnterField.getValue());
        mplew.write(0); // bSealedInsteadDead
        mplew.writeInt(life.getObjectId()); // dwMobId
        mplew.write(1); // nCalcDamageIndex
        mplew.writeInt(life.getId()); // dwTemplateID
        addMonsterStatus(mplew, life);
        Collection<MonsterStatusEffect> buffs = life.getStati().values();
        EncodeTemporary(mplew, buffs);
        addMonsterInformation(mplew, life, true, false, (byte) spawnType, link);

        return mplew.getPacket();
    }

    public static byte[] controlMonster(MapleMonster life, boolean newSpawn, boolean aggro, boolean azwan) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobChangeController.getValue());

        mplew.write(aggro ? 2 : 1);

        mplew.writeInt(life.getObjectId());
        mplew.write(1);// 1 = Control normal, 5 = Control none?
        mplew.writeInt(life.getId());// idk?
        addMonsterStatus(mplew, life);
        Collection<MonsterStatusEffect> buffs = life.getStati().values();
        EncodeTemporary(mplew, buffs);
        addMonsterInformation(mplew, life, newSpawn, false, (byte) /* (life.isFake() ? 1 : 0) */ -1, 0);

        return mplew.getPacket();
    }

    public static void addMonsterStatus(MaplePacketLittleEndianWriter mplew, MapleMonster life) {
        if (life.getStati().size() <= 1) {
            life.addEmpty();
        }
        mplew.write(life.getChangedStats() != null);
        if (life.getChangedStats() != null) {
            long hp = life.getChangedStats().finalmaxHP != 0 ? life.getChangedStats().finalmaxHP : life.getChangedStats().hp;
            mplew.writeLong(hp < 0 ? Long.MAX_VALUE : hp);
            mplew.writeInt(life.getChangedStats().mp);
            mplew.writeInt(life.getChangedStats().exp);
            mplew.writeInt(life.getChangedStats().watk);
            mplew.writeInt(life.getChangedStats().matk);
            mplew.writeInt(life.getChangedStats().PDRate);
            mplew.writeInt(life.getChangedStats().MDRate);
            mplew.writeInt(life.getChangedStats().acc);
            mplew.writeInt(life.getChangedStats().eva);
            mplew.writeInt(life.getChangedStats().pushed);
            mplew.writeInt(life.getChangedStats().speed);// new 141?
            mplew.writeInt(life.getChangedStats().level);
            mplew.writeInt(0); // nUserCount
        }
    }

    public static void addMonsterInformation(MaplePacketLittleEndianWriter mplew, MapleMonster life, boolean newSpawn, boolean summon, int spawnType, int link) {
        mplew.writePos(life.getTruePosition());
        mplew.write(life.getStance());
        if (life.isSpecial()) {
            mplew.write(0);
        }
        // if (life.getStats().getSkeleton() == 1) {
        // mplew.write(0);
        // }
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getOriginFh());
        mplew.writeShort(newSpawn ? spawnType : life.isFake() ? -4 : -1); // 0+:summon effect, -2:fade in, -3:after
                                                                          // remove link mob, -4:fake
        if ((spawnType == -3) || (spawnType >= 0)) {
            mplew.writeInt(link); // summonType= -3:link mob oid, 0+:effect delay
        }
        mplew.write(life.getCarnivalTeam());
        mplew.writeLong(life.getHp());
        mplew.writeInt(0); // nEffectItemID
        Rectangle banRange = life.getBanRange();
        if (banRange != null) { // 巡邏怪物的偵測範圍?
            mplew.writeInt(banRange.x); // m_nPatrolScopeX1
            mplew.writeInt(banRange.y); // m_nPatrolScopeX2
            mplew.writeInt(banRange.x + banRange.width); // nDetectX
            mplew.writeInt(banRange.y + banRange.height); // nSenseX
        }
        mplew.writeInt(0); // m_nPhase
        mplew.writeInt(0); // m_nCurZoneDataType
        mplew.writeInt(0); // m_dwRefImgMobID
        mplew.write(0);
        mplew.writeInt(-1);
        mplew.writeInt(0);
        mplew.writeInt(-1);
        mplew.write(0);

        int v20 = 0;
        mplew.writeInt(v20);
        if (v20 > 0) {
            do {
                // m_mLastActiveAttackEndTime
                mplew.writeInt(0);
                mplew.writeInt(0);
                v20--;
            } while (v20 != 1);
        }

        // 怪物大小(菁英怪物無效, 始終200)
        mplew.writeInt(life.getMobSize()); // m_nScale

        // 菁英怪物類型(頭上標識) 0 - 黃色劍 1 - 橙色劍 2 - 紅色劍
        mplew.writeInt(life.getEliteType()); // m_nEliteGrade
        if (life.isEliteMob()) {
            EliteMonsterInfo emi = life.getEliteInfo();
            mplew.writeInt(emi.getEliteAttribute().size());
            for (Map.Entry<EliteMonsterInfo.EliteAttribute, Integer> entry : emi.getEliteAttribute().entrySet()) {
                mplew.writeInt(entry.getKey().getValue()); // m_vEliteSkillID
                mplew.writeInt(entry.getValue()); // 不知
            }
            // 名稱顯示菁英屬性
            mplew.writeInt(1); // m_nEliteType
        }

        // m_pShootingMobStat
        byte v1 = 0;
        mplew.write(v1);
        // ShootingMobStat::Decode
        if (v1 > 0) {
            mplew.writeInt(0);// *v2 nMovePattern
            mplew.writeInt(0);// *(v2 + 1) nMoveRange
            mplew.writeInt(0);// *(v2 + 2) nBulletUpgrade
            mplew.writeInt(0);// *(v2 + 3) nMoveSpeed
            mplew.writeInt(0);// *(v2 + 4) nMoveAngle
        }

        v1 = 0;
        mplew.write(v1);
        if (v1 > 0) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        v1 = 0;
        mplew.writeInt(v1);
        for (int i = 0; i < v1; i++) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        int unkVal = 0;
        mplew.writeInt(unkVal);
        if (unkVal > 0) {
            for (int i = 0; i < unkVal; i++) {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }
        if (true) {
            mplew.write(HexTool.getByteArrayFromHexString("FF 00 00 00")); // m_nScale
        }
        mplew.write(0);

        // pvp怪物
        if (life.getId() / 10000 == 961) {
            mplew.writeMapleAsciiString(""); // 怪物名稱
        }

        mplew.writeInt(0);

        // 為了修正2D骨骼動畫的怪物導致 38 Error增加的空數據
        mplew.writeZeroBytes(100);
    }

    public static byte[] stopControllingMonster(MapleMonster life, boolean azwan) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobChangeController.getValue());
        mplew.write(0);
        mplew.writeInt(life.getObjectId());
        // if (azwan) {
        // mplew.write(0);
        // mplew.writeInt(0);
        // mplew.write(0);
        // addMonsterStatus(mplew, life);
        //
        // mplew.writePos(life.getTruePosition());
        // mplew.write(life.getStance());
        // mplew.writeShort(0);
        // mplew.writeShort(life.getFh());
        // mplew.write(life.isFake() ? -4 : -1);
        // mplew.write(life.getCarnivalTeam());
        // mplew.writeInt(63000);
        // mplew.writeInt(0);
        // mplew.writeInt(0);
        // mplew.write(-1);
        // }

        return mplew.getPacket();
    }

    public static byte[] makeMonsterReal(MapleMonster life, boolean azwan) {
        return spawnMonster(life, -1, 0, azwan);
    }

    public static byte[] makeMonsterFake(MapleMonster life, boolean azwan) {
        return spawnMonster(life, -4, 0, azwan);
    }

    public static byte[] makeMonsterEffect(MapleMonster life, int effect, boolean azwan) {
        return spawnMonster(life, effect, 0, azwan);
    }

    public static byte[] moveMonsterResponse(int dwMobID, short nMobCtrlSN, int currentMp, boolean bNextAttackPossible,
            int nSkillID, int nSLV) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobCtrlAck.getValue());
        mplew.writeInt(dwMobID);

        mplew.writeShort(nMobCtrlSN);
        mplew.write(bNextAttackPossible);
        mplew.writeInt(currentMp);
        mplew.writeInt(nSkillID);
        mplew.write(nSLV);
        mplew.writeInt(0); // nForcedAttackIdx

        return mplew.getPacket();
    }

    // public static byte[] getMonsterSkill(int objectid) {
    // MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
    //
    // mplew.writeShort(SendPacketOpcode.MONSTER_SKILL.getValue());
    // mplew.writeInt(objectid);
    // mplew.writeLong(0);
    //
    // return mplew.getPacket();
    // }
    public static byte[] getMonsterTeleport(int objectid, int x, int y) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TELE_MONSTER.getValue());
        mplew.writeInt(objectid);
        mplew.writeInt(x);
        mplew.writeInt(y);

        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus(MapleMonster mons, MonsterStatusEffect ms) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobStatSet.getValue());
        mplew.writeInt(mons.getObjectId());
        SingleProcessStatSet(mplew, ms);
        // System.out.println("applyMonsterStatus 1");

        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus(MapleMonster mons, List<MonsterStatusEffect> mse) {
        if ((mse.size() <= 0) || (mse.get(0) == null)) {
            return CWvsContext.enableActions();
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobStatSet.getValue());
        mplew.writeInt(mons.getObjectId());
        ProcessStatSet(mplew, mse);
        // System.out.println("applyMonsterStatus 2");

        return mplew.getPacket();
    }

    public static byte[] cancelMonsterStatus(MapleMonster mons, MonsterStatusEffect ms) {
        List<MonsterStatusEffect> mse = new ArrayList<>();
        mse.add(ms);
        return cancelMonsterStatus(mons, mse);
    }

    public static byte[] cancelMonsterStatus(MapleMonster mons, List<MonsterStatusEffect> mse) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobStatReset.getValue());
        mplew.writeInt(mons.getObjectId());
        writeMaskFromList(mplew, mse);
        for (MonsterStatusEffect buff : mse) {
            if (buff.getStati() == MonsterStatus.M_Burned) {
                mplew.writeInt(0);
                int v6 = 0;
                mplew.writeInt(v6);
                if (v6 > 0) {
                    do {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                        --v6;
                    } while (v6 == 0);
                }
            }
        }
        mplew.write(2);
        // if (MobStat::IsMovementAffectingStat)
        mplew.write(1);
        // System.out.println("cancelMonsterStatus");

        return mplew.getPacket();
    }

    public static byte[] talkMonster(int oid, int itemId, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobSpeaking.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(500);
        mplew.writeInt(itemId);
        mplew.write(itemId <= 0 ? 0 : 1);
        mplew.write((msg == null) || (msg.length() <= 0) ? 0 : 1);
        if ((msg != null) && (msg.length() > 0)) {
            mplew.writeMapleAsciiString(msg);
        }
        mplew.writeInt(1);

        return mplew.getPacket();
    }

    public static byte[] removeTalkMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobMessaging.getValue());
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static final byte[] getNodeProperties(MapleMonster objectid, MapleMap map) {
        if (objectid.getNodePacket() != null) {
            return objectid.getNodePacket();
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_PROPERTIES.getValue());
        mplew.writeInt(objectid.getObjectId());
        mplew.writeInt(map.getNodes().size());
        mplew.writeInt(objectid.getPosition().x);
        mplew.writeInt(objectid.getPosition().y);
        for (MapleNodes.MapleNodeInfo mni : map.getNodes()) {
            mplew.writeInt(mni.x);
            mplew.writeInt(mni.y);
            mplew.writeInt(mni.attr);
            if (mni.attr == 2) {
                mplew.writeInt(500);
            }
        }
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);

        objectid.setNodePacket(mplew.getPacket());
        return objectid.getNodePacket();
    }

    public static byte[] showMagnet(int mobid, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MAGNET.getValue());
        mplew.writeInt(mobid);
        mplew.write(success ? 1 : 0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] catchMonster(int mobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_MobCatchEffect.getValue());
        mplew.writeInt(mobid);
        mplew.writeInt(itemid);
        mplew.write(success);

        return mplew.getPacket();
    }

    public static void EncodeTemporary(MaplePacketLittleEndianWriter mplew, Collection<MonsterStatusEffect> buffs) {
        Set<MonsterStatus> mobstat = new HashSet<>();
        writeMaskFromList(mplew, buffs);
        for (MonsterStatusEffect buff : buffs) {
            mobstat.add(buff.getStati());
            if (buff.getStati().getBitNumber() < MonsterStatus.M_Burned.getBitNumber()) {
                mplew.writeInt(buff.getX()); // n
                if (buff.getMobSkill() != null) {
                    mplew.writeShort(buff.getMobSkill().getSkillId());
                    mplew.writeShort(buff.getMobSkill().getSkillLevel());
                } else {
                    mplew.writeInt(buff.getSkill() > 0 ? buff.getSkill() : 0); // r
                }
                mplew.writeShort((short) ((buff.getCancelTask() - System.currentTimeMillis()) / 1000)); // t
            }
        }
        if (mobstat.contains(MonsterStatus.M_PDR)) { // 1
            mplew.writeInt(0); // c
        }
        if (mobstat.contains(MonsterStatus.M_MDR)) { // 3
            mplew.writeInt(0); // c
        }
        if (mobstat.contains(MonsterStatus.M_Speed)) { // 6
            mplew.write(0); // c
        }
        if (mobstat.contains(MonsterStatus.M_Freeze)) { // 8
            mplew.writeInt(0); // c
        }
        if (mobstat.contains(MonsterStatus.M_PCounter)) { // 25
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_MCounter)) { // 26
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_PCounter) || mobstat.contains(MonsterStatus.M_MCounter)) {
            int n = 0, c = 0, b = 0;
            for (MonsterStatusEffect buff : buffs) {
                if (buff.getStati().equals(MonsterStatus.M_PCounter)
                        || buff.getStati().equals(MonsterStatus.M_MCounter)) {
                    n = buff.getX();
                }
            }
            mplew.writeInt(c); // c
            mplew.write(b); // b boolean
            mplew.writeInt(n); // n
        }
        if (mobstat.contains(MonsterStatus.M_Dark)) { // 33
            mplew.writeInt(0); // w
            mplew.writeInt(0); // u
            mplew.writeInt(0); // p
        }
        if (mobstat.contains(MonsterStatus.M_AddDamParty)) { // 35
            mplew.writeInt(0); // w
            mplew.writeInt(0); // u
            mplew.writeInt(0); // p
        }
        if (mobstat.contains(MonsterStatus.M_TempMoveAbility)) { // 47
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_FixDamRBuff)) { // 48
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_Fatality)) { // 37
            mplew.writeInt(0); // w
            mplew.writeInt(0); // u
        }
        if (mobstat.contains(MonsterStatus.M_Smite)) { // 40
            mplew.writeInt(0); // w
            mplew.writeInt(0); // u
            mplew.writeInt(0); // p
        }
        if (mobstat.contains(MonsterStatus.M_AreaInstallByHit)) { // 50
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_DarkLightning)) { // 53
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_BattlePvP_Helena_Mark)) { // 55
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_MultiPMDR)) { // 61
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.MBS62)) { // 62
            mplew.writeInt(0); // w
            mplew.writeInt(0); // u
            mplew.writeInt(0); // p
            mplew.writeInt(0); // p
        }
        // mask[1] & 1
        if (mobstat.contains(MonsterStatus.M_ElementResetBySummon)) { // 63
            mplew.writeInt(0); // w
            mplew.writeInt(0);
        }
        if (mobstat.contains(MonsterStatus.MBS66)) { // 66
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_BahamutLightElemAddDam)) { // 64
            mplew.writeInt(0); // p
            mplew.writeInt(0); // c
            mplew.writeInt(0);
        }
        // mask[2] & 0x40000000
        if (mobstat.contains(MonsterStatus.MBS73)) { // 74
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.MBS75)) { // 75
            mplew.writeInt(0);
        }
        if (mobstat.contains(MonsterStatus.MBS79)) { // 79
            mplew.writeInt(0);
        }
        if (mobstat.contains(MonsterStatus.M_Burned)) { // 82
            List<MonsterStatusEffect> bleedBuffs = new ArrayList<>();
            // MonsterStatusEffect
            buffs.stream().filter((buff) -> (buff.getStati().getBitNumber() == MonsterStatus.M_Burned.getBitNumber() && buff.getMobSkill() != null)).forEach((buff) -> {
                bleedBuffs.add(buff);
            });
            mplew.write(bleedBuffs.size());
            if (bleedBuffs.size() > 0) {
                bleedBuffs.forEach((buff) -> {
                    mplew.writeInt(8695624); // dwCharacterID
                    mplew.writeInt(buff.getSkill()); // nSkillID 技能ID
                    mplew.writeLong(7100); // nDamage 每秒傷害?
                    mplew.writeInt(1000); // tInterval 延遲毫秒 : dotInterval * 1000
                    mplew.writeInt(187277775); // tEnd
                    mplew.writeInt(16450); // tDotAnimation
                    mplew.writeInt(15); // nDotCount dotTime
                    mplew.writeInt(0); // nSuperPos
                    mplew.writeInt(1); // tAttackDelay
                    mplew.writeInt(7100); // nDotTickIdx 每秒傷害?
                    mplew.writeInt(0); // nDotTickDamR
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                });
            }
        }
        if (mobstat.contains(MonsterStatus.M_Invincible)) { // 83
            mplew.write(0); // n // nInvincible
            mplew.write(0); // b // bBalogDisable
        }
        if (mobstat.contains(MonsterStatus.M_ExchangeAttack)) { // 84
            mplew.write(0); // b
        }
        if (mobstat.contains(MonsterStatus.M_MultiPMADDR)) { // 85
            int result = 0;
            mplew.write(result);
            if (result != 0) {
                mplew.writeInt(0); // nPAD
                mplew.writeInt(0); // nMAD
                mplew.writeInt(0); // nPDR
                mplew.writeInt(0); // nMDR
            }
        }
        if (mobstat.contains(MonsterStatus.M_LinkTeam)) { // 86
            mplew.writeMapleAsciiString(""); // sLinkTeam
        }
        if (mobstat.contains(MonsterStatus.MBS96)) { // 96
            mplew.writeInt(0);
        }
        // mask[2] & 0x4000
        if (mobstat.contains(MonsterStatus.M_SoulExplosion)) { // 87
            mplew.writeInt(0); // n
            mplew.writeInt(0); // r
            mplew.writeInt(0); // w
        }
        // mask[2] & 0x2000
        if (mobstat.contains(MonsterStatus.M_SeperateSoulP)) { // 88
            mplew.writeInt(0); // n
            mplew.writeInt(0); // r
            mplew.writeShort(0); // t
            mplew.writeInt(0); // w
            mplew.writeInt(0); // u
        }
        if (mobstat.contains(MonsterStatus.M_SeperateSoulC)) { // 89
            mplew.writeInt(0); // n
            mplew.writeInt(0); // r
            mplew.writeShort(0); // t
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_Ember)) { // 90
            mplew.writeInt(0); // n
            mplew.writeInt(0); // r
            mplew.writeInt(0); // t
            mplew.writeInt(0); // w
            mplew.writeInt(0); // u
        }
        if (mobstat.contains(MonsterStatus.M_TrueSight)) { // 91
            mplew.writeInt(0); // n
            mplew.writeInt(0); // r
            mplew.writeInt(0); // t
            mplew.writeInt(0); // c
            mplew.writeInt(0); // p
            mplew.writeInt(0); // u
            mplew.writeInt(0); // w
        }
        if (mobstat.contains(MonsterStatus.M_Laser)) { // 93
            mplew.writeInt(0); // n
            mplew.writeInt(0); // r
            mplew.writeInt(0); // t
            mplew.writeInt(0); // w
            mplew.writeInt(0); // u
        }
        // mask[2] & 0x40
        if (mobstat.contains(MonsterStatus.DEFAULT_13)) { // 95
            mplew.writeInt(0); // n
            mplew.writeInt(0); // r
            mplew.writeInt(0); // t
        }
    }

    public static void SingleProcessStatSet(MaplePacketLittleEndianWriter mplew, MonsterStatusEffect buff) {
        Set<MonsterStatusEffect> ss = new HashSet<>();
        ss.add(buff);
        ProcessStatSet(mplew, ss);
    }

    public static void ProcessStatSet(MaplePacketLittleEndianWriter mplew, Collection<MonsterStatusEffect> buffs) {
        EncodeTemporary(mplew, buffs);
        mplew.writeShort(buffs.iterator().next().getEffectDelay());
        mplew.write(1);
        // if (MobStat::IsMovementAffectingStat)
        mplew.write(1);
    }

    private static void writeMaskFromList(MaplePacketLittleEndianWriter mplew, Collection<MonsterStatusEffect> ss) {
        int[] mask = new int[GameConstants.MAX_MOBSTAT];
        for (MonsterStatusEffect statup : ss) {
            mask[(statup.getStati().getPosition())] |= statup.getStati().getValue();
        }
        for (int i = 0; i < mask.length; i++) {
            mplew.writeInt(mask[(i)]);
        }
    }
}
