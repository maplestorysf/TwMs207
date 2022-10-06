package server.buffs.buffclasses.cygnus;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Maple
 */
public class WindArcherBuff extends AbstractBuffClass {

    public WindArcherBuff() {
        buffs = new int[]{13001022, // 元素： 風暴
            13101002, // 終極攻擊
            13101003, // 無形之箭
            13101006, // 風影漫步
            13101024, // 妖精援助
            13101023, // 快速之箭
            13111023, // 阿爾法
            13111024, // 翡翠花園
            13121004, // 風之祈禱
            13121005, // 會心之眼
            13120008, // 極限阿爾法
            13121053, // 守護者榮耀
            13121054, // 風暴使者
            13120003, // 風妖精之箭Ⅲ
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is皇家騎士團(job) && (job / 100) % 10 == 3;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 13001022: // 元素： 風暴
                eff.statups.put(MapleBuffStat.CygnusElementSkill, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 13101002: // 終極攻擊
                eff.statups.put(MapleBuffStat.SoulMasterFinal, eff.info.get(MapleStatInfo.x));
                break;
            case 13101003: // 無形之箭
                eff.statups.put(MapleBuffStat.SoulArrow, eff.info.get(MapleStatInfo.x));
                break;
            case 13101006: // 風影漫步
                eff.statups.put(MapleBuffStat.HideAttack, eff.info.get(MapleStatInfo.x));
                break;
            case 13101024: // 妖精援助
                eff.statups.put(MapleBuffStat.SoulArrow, 1);
                eff.statups.put(MapleBuffStat.CriticalBuff, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 13101023: // 快速之箭
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 13111023: // 阿爾法
                eff.statups.put(MapleBuffStat.Albatross, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndieMHP, eff.info.get(MapleStatInfo.indieMhp));
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));// true?
                eff.statups.put(MapleBuffStat.IndieCr, eff.info.get(MapleStatInfo.indieCr));
                break;
            case 13121000: // 皇家騎士
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 13120003: // 風妖精之箭Ⅲ
                eff.statups.put(MapleBuffStat.TriflingWhimOnOff, eff.info.get(MapleStatInfo.x));
                break;
            case 13120008: // 極限阿爾法
                eff.statups.put(MapleBuffStat.IndieMHP, eff.info.get(MapleStatInfo.indieMhp));
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));// true?
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieAsrR, eff.info.get(MapleStatInfo.indieAsrR));
                eff.statups.put(MapleBuffStat.IndieTerR, eff.info.get(MapleStatInfo.indieTerR));
                eff.statups.put(MapleBuffStat.IndieCr, eff.info.get(MapleStatInfo.indieCr));
                eff.statups.put(MapleBuffStat.IgnoreTargetDEF, 1);
                eff.statups.put(MapleBuffStat.Albatross, eff.info.get(MapleStatInfo.x));
                break;
            case 13111024: // 翡翠花園
                // spawn
                break;
            case 13121004: // 風之祈禱
                eff.statups.put(MapleBuffStat.ACCR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IllusionStep, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.DEXR, eff.info.get(MapleStatInfo.prop));
                // eff.statups.put(MapleBuffStat.IndieMHPR,
                // eff.info.get(MapleStatInfo.indieMhpR));
                break;
            case 13121005: // 會心之眼
                eff.statups.put(MapleBuffStat.SharpEyes,
                        (eff.info.get(MapleStatInfo.x) << 8) + eff.info.get(MapleStatInfo.criticaldamageMax));
                break;
            case 13121053: // 守護者榮耀
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            case 13121054: // 風暴使者
                eff.statups.put(MapleBuffStat.StormBringer, eff.info.get(MapleStatInfo.x));
                break;
            default:
                System.out.println("Unhandled Buff: " + skill);
                break;
        }
    }
}
