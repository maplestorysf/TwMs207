/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import client.skill.SkillFactory;
import handling.channel.handler.AttackInfo;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import server.MapleStatEffect;
import server.life.MapleMonster;
import tools.AttackPair;
import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Fate
 */
public class CalcDamage {

    CRand32 rndGenForCharacter;
    // CRand32 rndForCheckDamageMiss;//not implement yet
    // CRand32 rndGenForMob;//not implement yet
    // int invalidCount;

    private int numRand = 11;// A number of random number for calculate damage

    public double get_range(double randomNum, double max, double min) {
        double v3 = max;
        double v4 = min;
        if (max > min) {
            v3 = min;
            v4 = max;
            return (randomNum % 10000000) * (v4 - v3) / 9999999.0 + v3;
        }
        if (max != min) {
            return (randomNum % 10000000) * (v4 - v3) / 9999999.0 + v3;
        }
        return max;
    }

    /*
     * void __thiscall CalcDamage::PDamageForPvM( CalcDamage *this, CharacterData
     * *cd, BasicStat *bs, const unsigned int ss, MobStat *dwMobID, int ms,
     * CMobTemplate *nCurMobZoneDataType, ZRef<PassiveSkillData> pTemplate, int
     * *pPsd, int bNextAttackCritical, int nAttackCount, int nDamagePerMob, int
     * nWeaponItemID, int nBulletItemID, int nAttackType, int nAction, SKILLENTRY
     * *bShadowPartner, int pSkill, int *nSLV, int *aDamage, int abCritical, int
     * nCriticalProb, int nCriticalDamage, int nTotalDAMr,
     * std::vector<long,std::allocator<long> > *nBossDAMr, int
     * anItemIgnoreTargetDEF, int nMobCount, int nDragonFury, int nAR01Pad, int
     * nMobMaxHP, ZMap<long,long,long> *tAttackTime, int mMicroBuffEndTime, int
     * tKeyDown, int nReincarnation, int nAdvancedChargeDamage, int bInvincible, int
     * nPartyID, int bShieldEquiped, bool bSerialAttack, int bAddAttack,
     * USERSTATICSTAT *nAttackDistance, bool pStaticStat, CALCDAMAGEINFO *bAlone,
     * bool pCalcDamageInfo, int *bAssistSkill, int pnCriticlalGrowResetCheck, int
     * bSoulDungeon, int nMobHPPercentage,
     * 
     */
    public List<Pair<Integer, Boolean>> PDamageForPvM(MapleCharacter chr, AttackInfo attack) {
        List<Pair<Integer, Boolean>> realDamageList = new ArrayList<>();
        for (AttackPair eachMob : attack.allDamage) {// For each monster
            MapleMonster monster = chr.getMap().getMonsterByOid(eachMob.objectid);

            long rand[] = new long[numRand];// we need save it as long to store unsigned int
            for (int i = 0; i < numRand; i++) {
                rand[i] = rndGenForCharacter.random();
            }
            byte index = 0;

            for (Pair<Long, Boolean> att : eachMob.attack) {// For each attack
                double realDamage = 0.0;
                boolean critical = false;

                index++;
                long unkRand1 = rand[index++ % numRand];

                // Adjusted Random Damage
                int maxDamage = (int) chr.getStat().getCurrentMaxBaseDamage();
                int minDamage = (int) chr.getStat().getCurrentMinBaseDamage();
                double adjustedRandomDamage = get_range(rand[index++ % numRand], maxDamage, minDamage);
                realDamage += adjustedRandomDamage;

                // Adjusted Damage By Monster's Physical Defense Rate
                double monsterPDRate = monster.getStats().getPDRate();
                double percentDmgAfterPDRate = Math.max(0.0, 100.0 - monsterPDRate);
                realDamage = percentDmgAfterPDRate / 100.0 * realDamage;

                // Adjusted Damage By Skill
                MapleStatEffect skillEffect = null;
                if (attack.skill > 0) {
                    skillEffect = SkillFactory.getSkill(attack.skill).getEffect(chr.getTotalSkillLevel(attack.skill));
                }
                if (skillEffect != null) {
                    realDamage = realDamage * (double) skillEffect.getDamage() / 100.0;
                }

                // Adjusted Critical Damage
                if (get_range(rand[index++ % numRand], 100, 0) < chr.getStat().getCritRate()) {
                    critical = true;
                    int maxCritDamage = chr.getStat().getMinCritDamage();
                    int minCritDamage = chr.getStat().getMinCritDamage();
                    int criticalDamageRate = (int) get_range(rand[index++ % numRand], maxCritDamage, minCritDamage);
                    realDamage = realDamage + (criticalDamageRate / 100.0 * (int) realDamage);// nexon convert
                    // realDamage to int when
                    // multiply with
                    // criticalDamageRate
                }

                realDamageList.add(new Pair<>((int) realDamage, critical));
            }
        }
        return realDamageList;
    }

    public CalcDamage() {
        rndGenForCharacter = new CRand32();
        // invalidCount = 0;
    }

    public void SetSeed(int seed1, int seed2, int seed3) {
        rndGenForCharacter.seed(seed1, seed2, seed3);
        // rndForCheckDamageMiss.Seed(seed1, seed2, seed3);//not implement yet
        // rndGenForMob.Seed(seed1, seed2, seed3);//not implement yet
    }

    public final void writeCalcDamageSeed(final MapleCharacter chr, final MaplePacketLittleEndianWriter mplew) {
        CRand32 crc32 = new CRand32();
        long v5 = crc32.random();
        long s2 = crc32.random();
        long v6 = crc32.random();
        crc32.seed(v5, s2, v6);
        chr.getCalcDamage().SetSeed((int) v5, (int) s2, (int) v6);
        mplew.writeInt((int) v5);
        mplew.writeInt((int) s2);
        mplew.writeInt((int) v6);
    }
}
