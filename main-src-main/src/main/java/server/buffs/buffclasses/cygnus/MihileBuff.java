package server.buffs.buffclasses.cygnus;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

public class MihileBuff extends AbstractBuffClass {

    public MihileBuff() {
        buffs = new int[]{51101003, // 快速之劍
            51101004, // 激勵
            51111003, // 閃耀激發
            51111004, // 靈魂抗性
            51121004, // 格檔
            51121005, // 楓葉祝福
            51121006, // 靈魂之怒
            51121053, // 明日女皇
            51121054, // 神聖護石
            51001006, // 皇家之盾
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is米哈逸(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        if (!containsSkill(skill)) {
            return;
        }

        switch (skill) {
            case 51101003: // 快速之劍
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x) * 2);
                break;
            case 51101004: // 激勵
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.PowerGuard, eff.info.get(MapleStatInfo.x));
                break;
            case 51111003: // 閃耀激發
                eff.statups.put(MapleBuffStat.WeaponCharge, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.HowlingAttackDamage, eff.info.get(MapleStatInfo.z));
                break;
            case 51111004: // 靈魂抗性
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.z));
                eff.statups.put(MapleBuffStat.DDR, eff.info.get(MapleStatInfo.x));
                break;
            case 51121004: // 格檔
                eff.statups.put(MapleBuffStat.Stance, (int) eff.info.get(MapleStatInfo.prop));
                break;
            case 51121005: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 51121006: // 靈魂之怒
                eff.statups.put(MapleBuffStat.HowlingAttackDamage, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.HowlingDefence, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.HowlingDefence, eff.info.get(MapleStatInfo.z));
                break;
            case 51001006: // 皇家之盾
                eff.statups.put(MapleBuffStat.RoyalGuardState, 1);
                break;
            case 51111008:
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.MichaelSoulLink, 0);
                break;
            case 51121053:
                break;
            case 51121054:
                break;
            default:
                // System.out.println("Unhandled Buff: " + skill);
                break;
        }
    }
}
