package server.buffs.buffclasses.cygnus;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

public class BlazeWizardBuff extends AbstractBuffClass {

    public BlazeWizardBuff() {
        buffs = new int[]{12001001, // 魔心防禦
            12000022, // 元素:火焰
            12100026, 12110024, 12120007, 12101005, // 自然力重置
            12101023, // 火之書
            12101024, // 燃燒
            12120013, // 火焰之魂
            12120013, // 火焰之魂
            12121003, // 火焰防護
            12121005, // 燃燒軍團
            12111023, // 火鳳凰
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is烈焰巫師(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 12001001: // 魔心防禦
                eff.statups.put(MapleBuffStat.MagicGuard, eff.info.get(MapleStatInfo.x));
                break;
            case 12100026:
            case 12110024:
            case 12120007:
            case 12000022: // 元素:火焰
                eff.statups.put(MapleBuffStat.IndieMAD, eff.info.get(MapleStatInfo.x));
                break;
            case 12101005: // 自然力重置
                eff.statups.put(MapleBuffStat.ElementalReset, eff.info.get(MapleStatInfo.x));
                break;
            case 12101023: // 火之書
                eff.statups.put(MapleBuffStat.IndieMAD, eff.info.get(MapleStatInfo.indieMad));
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));
                break;
            case 12111023: // 火鳳凰
                eff.statups.put(MapleBuffStat.FlareTrick, 1);
                break;
            case 12121003:
                eff.statups.put(MapleBuffStat.DamageReduce, eff.info.get(MapleStatInfo.x));
                break;
            case 12121005: // 燃燒軍團
                eff.statups.put(MapleBuffStat.DamR, eff.info.get(MapleStatInfo.damR));
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));
                break;
            case 12101024: // 燃燒
                eff.statups.put(MapleBuffStat.Ember, 1);
                break;
            case 12120013: // 火焰之魂
            case 12120014: // 火焰之魂
                eff.statups.put(MapleBuffStat.CarnivalDefence, eff.info.get(MapleStatInfo.y));
            default:
                // System.out.println("Unhandled Buff: " + skill);
                break;
        }
    }
}
