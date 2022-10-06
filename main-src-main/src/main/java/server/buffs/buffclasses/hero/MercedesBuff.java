package server.buffs.buffclasses.hero;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

public class MercedesBuff extends AbstractBuffClass {

    public MercedesBuff() {
        buffs = new int[]{23101002, // 雙弩槍推進器
            23101003, // 靈魂灌注
            23111004, // 依古尼斯咆哮
            23111005, // 水之盾
            23121000, // 伊修塔爾之環
            23121004, // 遠古意志
            23121005, // 楓葉祝福
            23121053, // 英雄誓言
            23121054, // 精靈祝福
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is精靈遊俠(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 23101002: // 雙弩槍推進器
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 23101003: // 靈魂灌注
                eff.statups.put(MapleBuffStat.SpiritLink, eff.info.get(MapleStatInfo.damage));
                eff.statups.put(MapleBuffStat.CriticalBuff, eff.info.get(MapleStatInfo.x));
                break;
            case 23111004: // 依古尼斯咆哮
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.AddAttackCount, 1);
                break;
            case 23111005: // 水之盾
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.terR));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.terR));
                eff.statups.put(MapleBuffStat.DamAbsorbShield, eff.info.get(MapleStatInfo.x));
                break;
            case 23121000: // 伊修塔爾之環
                eff.statups.put(MapleBuffStat.KeyDownMoving, 0);
                break;
            case 23121004: // 遠古意志
                eff.statups.put(MapleBuffStat.IndiePADR, (int) eff.info.get(MapleStatInfo.indiePadR));
                eff.statups.put(MapleBuffStat.EMHP, (int) eff.info.get(MapleStatInfo.emhp));
                break;
            case 23121005: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 23121053: // 英雄誓言
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            case 23121054: // 精靈祝福
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.IndieStance, eff.info.get(MapleStatInfo.x));
                break;
            default:
                System.out.println("未知的 精靈遊俠(2300) Buff: " + skill);
                break;
        }
    }
}
