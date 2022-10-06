/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.forceatoms.ForceAtom;
import client.character.stat.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import constants.ServerConfig;
import handling.SendPacketOpcode;
import java.awt.Point;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import server.MapleStatInfo;
import server.Randomizer;
import server.life.MapleMonster;
import server.maps.MapleSummon;
import tools.DateUtil;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Itzik
 */
public class JobPacket {

    public static class WildHunterPacket {

        public static byte[] sendJaguarSkill(int skillid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_JaguarSkill.getValue());
            mplew.writeInt(skillid);
            return mplew.getPacket();
        }
    }

    public static class ThiefPacket {

        public static byte[] OnOffFlipTheCoin(boolean on) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserFlipTheCoinEnabled.getValue());
            mplew.write(on ? 1 : 0);

            return mplew.getPacket();
        }
    }

    public static class Cygnus {

        public static byte[] OrbitalFlame(int cid, int skillid, int effect, int direction, int range) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ForceAtomCreate.getValue());
            mplew.write(0);
            mplew.writeInt(cid);
            /* ����� */
            mplew.writeInt(0x11);
            /* ��ų�� ���� Ÿ�� ���� */
            mplew.write(1);
            mplew.writeInt(0);
            mplew.writeInt(skillid);
            mplew.write(1);
            mplew.writeInt(2);
            mplew.writeInt(effect); // effect
            mplew.writeInt(17);
            mplew.writeInt(17);
            mplew.writeInt(90);
            mplew.writeZeroBytes(12);
            mplew.writeInt(Randomizer.nextInt());
            mplew.writeInt(8);
            mplew.writeInt(0); // 1.2.252+
            mplew.write(0);
            mplew.writeInt(direction);
            mplew.writeInt(range);

            return mplew.getPacket();
        }
    }

    public static class ZeroPacket {

        public static byte[] shockWave(int skillid, int delay, int direction, Point pos) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_UserFinalAttackRequest.getValue());

            mplew.writeInt(skillid);
            mplew.writeInt(101000102);
            mplew.writeInt(56);
            mplew.writeInt(delay);
            mplew.writeInt(0);
            mplew.write(direction);
            mplew.writePos(pos);

            return mplew.getPacket();
        }
    }

    public static class KinesisPacket {

        public static byte[] givePsychicPoint(int job, int mp) {
            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.KinesisPsychicPoint, mp);

            return CWvsContext.BuffPacket.giveBuff(job, 0, statups, null, null);
        }

        public static void PsychicUltimateDamager(final LittleEndianAccessor slea, final MapleClient c) {// TODO : 終極技 :
                                                                                                         // 梅泰利爾
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.ReleasePsychicArea.getValue());
            mplew.writeInt(c.getPlayer().getId());
            mplew.writeInt(slea.readInt());

            c.getSession().writeAndFlush(mplew.getPacket());
        }

        public static void PsychicDamage(int mobcount, final MapleClient c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            // mplew.writeShort(SendPacketOpcode.PSYCHIC_DAMAGE.getValue());
            mplew.writeInt(mobcount);
            mplew.writeInt(1);

            c.getSession().writeAndFlush(mplew.getPacket());
        }

        public static void PsychicAttack(final LittleEndianAccessor slea, final MapleClient c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.CreatePsychicArea.getValue());
            mplew.writeInt(c.getPlayer().getId());
            mplew.write(1);
            int nAction = slea.readInt();
            mplew.writeInt(nAction);
            int nActionSpeed = slea.readInt();
            mplew.writeInt(nActionSpeed);

            // CUser::FindUnapprovedPsychicArea
            final int mobcount = slea.readInt();

            int unk = slea.readInt();

            mplew.writeInt(unk);

            // param.nSkillID
            final int skillid = slea.readInt();
            mplew.writeInt(skillid);

            int nSLV = slea.readShort();
            mplew.writeShort(nSLV);

            int nPsychicAreaKey = (0xFFFFFFFF - mobcount) + 1;
            mplew.writeInt(nPsychicAreaKey);

            // param.nDurationTime
            final int unknown_i = slea.readInt();
            mplew.writeInt(unknown_i != 0xFFFFFFFF ? unknown_i + 4000 : unknown_i);

            // param.isLeft.second
            mplew.write(slea.readByte());

            // param.nSkeletonFilePathIdx
            final short unknown_si = slea.readShort();
            mplew.writeShort(unknown_si != 0xFFFF ? unknown_si : 0);

            // param.nSkeletonAniIdx
            final short unknown_sii = slea.readShort();
            mplew.writeShort(unknown_sii != 0xFFFF ? unknown_sii : 0);

            // param.nSkeletonLoop
            final short unknown_siii = slea.readShort();
            mplew.writeShort(unknown_siii != 0xFFFF ? unknown_siii : 0);

            // param.posStart.second 8Byte
            mplew.writePos(slea.readPos());
            mplew.writePos(slea.readPos());

            /* PPoint Check */
            c.getPlayer().givePPoint(skillid);

            c.getSession().writeAndFlush(mplew.getPacket());
        }

        public static void CancelPsychicGrep(final LittleEndianAccessor slea, final MapleClient c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.ReleasePsychicLock.getValue());
            mplew.writeInt(c.getPlayer().getId());
            mplew.writeInt(slea.readInt());

            c.getSession().writeAndFlush(mplew.getPacket());
        }

        public static void PsychicGrep(final LittleEndianAccessor slea, final MapleClient c) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.CreatePsychicLock.getValue());
            /* First AttackInfo Start */
            mplew.writeInt(c.getPlayer().getId());
            mplew.write(1);

            // nSkillID
            final int skillid = slea.readInt();
            mplew.writeInt(skillid);

            // nSLV
            mplew.writeShort(slea.readShort());

            // nAction
            mplew.writeInt(slea.readInt());

            // nActionSpeed
            mplew.writeInt(slea.readInt());
            /* First AttackInfo End */

            int i = 0;
            int point = 0;
            boolean end = false;
            MapleMonster target = null;
            while (true) {
                end = (slea.readByte() <= 0);
                mplew.write(!end ? 1 : 0);
                if (!end) {
                    mplew.write(!end ? 1 : 0);
                    mplew.writeInt(slea.readInt());
                } else {
                    break;
                }
                // CUser::FindUnapprovedPsychicLock
                slea.skip(4);

                // param.nPsychicLockKey
                mplew.writeInt((i) + 1);

                // param.dwMobID
                final int monsterid = slea.readInt();
                mplew.writeInt(monsterid); // ���� ���̵�.

                // param.nStuffID
                mplew.writeShort(slea.readShort());
                if (monsterid != 0) {
                    target = c.getPlayer().getMap().getMonsterByOid(monsterid);
                }
                slea.skip(2);

                // param.nMobMaxHP
                mplew.writeInt(monsterid != 0 ? (int) target.getHp() : 100);
                // param.nMobCurHP
                mplew.writeInt(monsterid != 0 ? (int) target.getHp() : 100);
                // param.posRel.first
                mplew.write(slea.readByte());

                // param.posStart.second 8Byte
                mplew.writePos(slea.readPos());
                mplew.writePos(slea.readPos());

                // param.posRel.second 8Byte
                mplew.writePos(slea.readPos());
                mplew.writePos(slea.readPos());
                i++;
            }
            /* PPoint Check */
            c.getPlayer().givePPoint(skillid);

            c.getSession().writeAndFlush(mplew.getPacket());
        }
    }

    public static class PhantomPacket {

        public static byte[] addStolenSkill(int jobNum, int index, int skill, int level) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ChangeStealMemoryResult.getValue());
            mplew.write(1);
            mplew.write(0);
            mplew.writeInt(jobNum);
            mplew.writeInt(index);
            mplew.writeInt(skill);
            mplew.writeInt(level);
            mplew.writeInt(0);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] removeStolenSkill(int jobNum, int index) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ChangeStealMemoryResult.getValue());
            mplew.write(1);
            mplew.write(3);
            mplew.writeInt(jobNum);
            mplew.writeInt(index);
            mplew.write(0);

            return mplew.getPacket();
        }

        public static byte[] replaceStolenSkill(int base, int skill) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ResultSetStealSkill.getValue());
            mplew.write(1);
            mplew.write(skill > 0 ? 1 : 0);
            mplew.writeInt(base);
            mplew.writeInt(skill);

            return mplew.getPacket();
        }

        public static byte[] gainCardStack(MapleCharacter chr, int atomSkillID, List<Integer> targets, int count) {
            List<ForceAtom> forceinfo = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                forceinfo.add(chr.getNewAtom(atomSkillID, false));
            }
            return CField.OnCreateForceAtom(false, chr, targets, 1, atomSkillID, forceinfo, null, null, 0, 0, 0);
        }

        public static byte[] updateCardStack(final int total) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_IncJudgementStack.getValue());
            mplew.write(total);

            return mplew.getPacket();
        }

    }

    public static class AngelicPacket {

        public static byte[] DressUpTime(byte type) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_Message.getValue());
            mplew.write(type);
            mplew.writeShort(7707);
            mplew.write(2);
            mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
            return mplew.getPacket();
        }

        public static byte[] updateDress(int transform, MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_UserSetDefaultWingItem.getValue());
            mplew.writeInt(chr.getId());
            mplew.writeInt(transform);
            return mplew.getPacket();
        }

        public static byte[] lockSkill(int skillid) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_SetOffStateForOnOffSkill.getValue());
            mplew.writeInt(skillid);
            return mplew.getPacket();
        }

        public static byte[] unlockSkill() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_ResetOnStateForOnOffSkill.getValue());
            mplew.writeInt(0);
            return mplew.getPacket();
        }

        public static byte[] absorbingSoulSeeker(int characterid, int size, Point essence1, Point essence2, int skillid,
                boolean creation) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ForceAtomCreate.getValue());
            mplew.write(!creation ? 0 : 1);
            mplew.writeInt(characterid);
            if (!creation) {
                // false
                mplew.writeInt(3);
                mplew.write(1);
                mplew.write(size);
                mplew.writeZeroBytes(3);
                mplew.writeShort(essence1.x);
                mplew.writeShort(essence1.y);
                mplew.writeShort(essence2.y);
                mplew.writeShort(essence2.x);
            } else {
                // true
                mplew.writeShort(essence1.x);
                mplew.writeShort(essence1.y);
                mplew.writeInt(4);
                mplew.write(1);
                mplew.writeShort(essence1.y);
                mplew.writeShort(essence1.x);
            }
            mplew.writeInt(skillid);
            if (!creation) {
                for (int i = 0; i < 2; i++) {
                    mplew.write(1);
                    mplew.writeInt(Randomizer.rand(19, 20));
                    mplew.writeInt(1);
                    mplew.writeInt(Randomizer.rand(18, 19));
                    mplew.writeInt(Randomizer.rand(20, 23));
                    mplew.writeInt(Randomizer.rand(36, 55));
                    mplew.writeInt(540);
                    mplew.writeShort(0);// new 142
                    mplew.writeZeroBytes(6);// new 143
                }
            } else {
                mplew.write(1);
                mplew.writeInt(Randomizer.rand(6, 21));
                mplew.writeInt(1);
                mplew.writeInt(Randomizer.rand(42, 45));
                mplew.writeInt(Randomizer.rand(4, 7));
                mplew.writeInt(Randomizer.rand(267, 100));
                mplew.writeInt(0);// 540
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
            mplew.write(0);
            return mplew.getPacket();
        }

        public static byte[] SoulSeekerRegen(MapleCharacter chr, int sn) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_ForceAtomCreate.getValue());
            mplew.write(1);
            mplew.writeInt(chr.getId());
            mplew.writeInt(sn);
            mplew.writeInt(4);
            mplew.write(1);
            mplew.writeInt(sn);
            mplew.writeInt(65111007); // hide skills
            mplew.write(1);
            mplew.writeInt(Randomizer.rand(0x06, 0x10));
            mplew.writeInt(1);
            mplew.writeInt(Randomizer.rand(0x28, 0x2B));
            mplew.writeInt(Randomizer.rand(0x03, 0x04));
            mplew.writeInt(Randomizer.rand(0xFA, 0x49));
            mplew.writeInt(0);
            mplew.writeLong(0);
            mplew.write(0);
            return mplew.getPacket();
        }

        public static byte[] SoulSeeker(MapleCharacter chr, int skillid, int sn, int sc1, int sc2) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_ForceAtomCreate.getValue());
            mplew.write(0);
            mplew.writeInt(chr.getId());
            mplew.writeInt(3);
            mplew.write(1);
            mplew.writeInt(sn);
            if (sn >= 1) {
                mplew.writeInt(sc1);// SHOW_ITEM_GAIN_INCHAT
                if (sn == 2) {
                    mplew.writeInt(sc2);
                }
            }
            mplew.writeInt(65111007); // hide skills
            for (int i = 0; i < 2; i++) {
                mplew.write(1);
                mplew.writeInt(i + 2);
                mplew.writeInt(1);
                mplew.writeInt(Randomizer.rand(0x0F, 0x10));
                mplew.writeInt(Randomizer.rand(0x1B, 0x22));
                mplew.writeInt(Randomizer.rand(0x1F, 0x24));
                mplew.writeInt(540);
                mplew.writeInt(0);// wasshort new143
                mplew.writeInt(0);// new143
            }
            mplew.write(0);
            return mplew.getPacket();
        }
    }

    public static class LuminousPacket {

        public static byte[] updateLuminousGauge(int gauge, int type) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_ChangeLarknessStack.getValue());
            mplew.writeInt(gauge);
            mplew.write(type);

            return mplew.getPacket();
        }
    }

    public static class XenonPacket {

        public static byte[] giveXenonSupply(int amount) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.SurplusSupply);

            mplew.writeShort(amount);
            mplew.writeInt(30020232); // skill id
            mplew.writeInt(-1); // duration
            mplew.writeZeroBytes(18);

            return mplew.getPacket();
        }

        public static byte[] giveAmaranthGenerator() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.SurplusSupply, 0);
            statups.put(MapleBuffStat.AmaranthGenerator, 0);
            PacketHelper.writeBuffMask(mplew, statups);

            mplew.writeShort(20); // gauge fill
            mplew.writeInt(30020232); // skill id
            mplew.writeInt(-1); // duration

            mplew.writeShort(1);
            mplew.writeInt(36121054); // skill id
            mplew.writeInt(10000); // duration

            mplew.writeZeroBytes(5);
            mplew.writeInt(1000);
            mplew.writeInt(1);
            mplew.writeZeroBytes(1);

            mplew.writeZeroBytes(69); // for no dc

            return mplew.getPacket();
        }
    }

    public static class AvengerPacket {

        public static byte[] giveAvengerHpBuff(int hp) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());

            PacketHelper.writeSingleMask(mplew, MapleBuffStat.LifeTidal);
            mplew.writeShort(3);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeInt(hp);

            mplew.writeInt(0);

            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);

            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] giveExceed(short amount) {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());
            PacketHelper.writeSingleMask(mplew, MapleBuffStat.OverloadCount);
            mplew.writeShort(amount);
            mplew.writeInt(30010230); // 超越
            mplew.writeInt(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            mplew.writeInt(0);

            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);

            mplew.writeInt(0);

            return mplew.getPacket();
        }

        public static byte[] cancelExceed() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());
            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.OverloadCount, 0);
            PacketHelper.writeBuffMask(mplew, statups);
            return mplew.getPacket();
        }

        public static byte[] cancelExceedAttack() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());
            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.Exceed, 0);
            PacketHelper.writeBuffMask(mplew, statups);
            return mplew.getPacket();
        }
    }

    public static class BeastTamerPacket {

        public static byte[] ModeCancel() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());

            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.AnimalChange, 0);
            PacketHelper.writeBuffMask(mplew, statups);

            return mplew.getPacket();
        }

        public static byte[] BearMode() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());

            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.AnimalChange, 0);
            PacketHelper.writeBuffMask(mplew, statups);
            mplew.writeShort(1);
            mplew.writeInt(110001501);
            mplew.writeInt(-419268850);
            mplew.writeLong(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(1);
            mplew.writeInt(0);

            mplew.writeZeroBytes(69); // for no dc

            return mplew.getPacket();
        }

        public static byte[] LeopardMode() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());

            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.AnimalChange, 0);
            PacketHelper.writeBuffMask(mplew, statups);
            mplew.writeShort(2);
            mplew.writeInt(110001502);
            mplew.writeInt(-419263978);
            mplew.writeLong(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(1);
            mplew.writeInt(0);

            mplew.writeZeroBytes(69); // for no dc

            return mplew.getPacket();
        }

        public static byte[] HawkMode() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());

            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.AnimalChange, 0);
            PacketHelper.writeBuffMask(mplew, statups);
            mplew.writeShort(3);
            mplew.writeInt(110001503);
            mplew.writeInt(-419263978);
            mplew.writeLong(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(1);
            mplew.writeInt(0);

            mplew.writeZeroBytes(69); // for no dc

            return mplew.getPacket();
        }

        public static byte[] CatMode() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());

            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.AnimalChange, 0);
            PacketHelper.writeBuffMask(mplew, statups);
            mplew.writeShort(4);
            mplew.writeInt(110001504);
            mplew.writeInt(-419263978);
            mplew.writeLong(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(1);
            mplew.writeInt(0);

            mplew.writeZeroBytes(69); // for no dc

            return mplew.getPacket();
        }

        public static byte[] LeopardRoar() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());

            Map<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
            statups.put(MapleBuffStat.IndieMaxDamageOver, statups.get(MapleStatInfo.indieDamR));
            statups.put(MapleBuffStat.IndieDamR, statups.get(MapleStatInfo.indieMaxDamageOver));
            PacketHelper.writeBuffMask(mplew, statups);
            mplew.writeShort(4);
            mplew.writeInt(110001504);
            mplew.writeInt(-419263978);
            mplew.writeLong(0);
            mplew.writeInt(0);
            mplew.write(0);
            mplew.write(1);
            mplew.writeInt(0);

            mplew.writeZeroBytes(69); // for no dc

            return mplew.getPacket();
        }
    }

    public static class PinkBeanPacket {

        public static byte[] 咕嚕咕嚕變大() {
            if (ServerConfig.LOG_PACKETS) {
                System.out.println("調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            }
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatSet.getValue());

            PacketHelper.writeSingleMask(mplew, MapleBuffStat.PinkbeanRollingGrade);

            mplew.writeZeroBytes(5);
            mplew.write(1);
            mplew.writeZeroBytes(13);

            return mplew.getPacket();
        }

        public static byte[] cancel咕嚕咕嚕() {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_TemporaryStatReset.getValue());

            PacketHelper.writeSingleMask(mplew, MapleBuffStat.PinkbeanRollingGrade);

            mplew.writeZeroBytes(53);
            mplew.write(0x40);
            mplew.writeZeroBytes(6);
            mplew.write(0x42);

            return mplew.getPacket();
        }
    }

    public static class BattleMagicPacket {

        public static byte[] GrimContractAttack(MapleCharacter chr) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

            mplew.writeShort(SendPacketOpcode.LP_SummonedAssistAttackRequest.getValue());
            mplew.writeInt(chr.getId());
            int oid = 0;
            for (MapleSummon ms : chr.getMap().getAllSummonsThreadsafe()) {
                if (ms.getOwner().equals(chr) && MapleJob.is煉獄巫師(ms.getSkill() / 10000)) {
                    oid = ms.getObjectId();
                    break;
                }
            }
            mplew.writeInt(oid);
            mplew.writeInt(0); // TODO: ???
            return mplew.getPacket();
        }
    }
}
