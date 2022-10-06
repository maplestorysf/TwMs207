/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.cygnus;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Fate
 */
public class ThunderBreakerBuff extends AbstractBuffClass {

    public ThunderBreakerBuff() {
        buffs = new int[]{15001022, // 元素： 雷電
            15101022, // 致命快打
            15101006, // 雷鳴
            15111023, // 渦流
            15111024, // 磁甲
            15121005, // 最終極速
            15121004, // 引雷
            15111022, // 疾風
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is閃雷悍將(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 15001022: // 元素： 雷電
                eff.statups.put(MapleBuffStat.CygnusElementSkill, 1);
                break;
            case 15100004: // 蓄能激發
                eff.statups.put(MapleBuffStat.BattlePvP_Helena_Mark, 0);
                break;
            case 15101022: // 致命快打
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 15101006: // 雷鳴
                eff.statups.put(MapleBuffStat.WeaponCharge, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.HowlingAttackDamage, eff.info.get(MapleStatInfo.z));
                break;
            case 15111022: // 疾風
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 15111023: // 渦流
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.accR));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.terR));
                break;
            case 15111024: // 磁甲
                eff.statups.put(MapleBuffStat.DamAbsorbShield, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 15121005: // 最終極速
                eff.statups.put(MapleBuffStat.PartyBooster, eff.info.get(MapleStatInfo.x));
                break;
            case 15121004: // 引雷
                eff.statups.put(MapleBuffStat.ShadowPartner, eff.info.get(MapleStatInfo.x));
                break;
            default:
                // System.out.println("Hayato skill not coded: " + skill);
                break;
        }
    }
}
