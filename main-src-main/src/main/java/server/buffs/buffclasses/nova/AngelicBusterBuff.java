/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.nova;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Maple
 */
public class AngelicBusterBuff extends AbstractBuffClass {

    public AngelicBusterBuff() {
        buffs = new int[]{60011219, // 靈魂契約Terms and Conditions
            65001002, // 抒情十字
            65101002, // 靈魂傳動 Power Transfer
            65111003, // 遠古召喚Dragon Whistle
            65111004, // 鐵之蓮華Iron Blossom
            65121011, // 索魂精通
            65121003, // 靈魂震動
            65121004, // 凝視靈魂Star Gazer
            65121009, // 超新星之勇士Nova Warrior
            65121053, // 終極契約Final Contract
            65121054, // 靈魂深造Pretty Exaltation
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is天使破壞者(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 60011219: // 靈魂契約Terms and Conditions
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 65001002: // 抒情十字
                eff.statups.put(MapleBuffStat.IndieMHP, eff.info.get(MapleStatInfo.indieMhp));
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));
                break;
            case 65101002: // 靈魂傳動 Power Transfer
                eff.statups.put(MapleBuffStat.PowerTransferGauge, eff.info.get(MapleStatInfo.x));
                break;
            case 65111003: // 遠古召喚Dragon Whistle
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 65111004: // 鐵之蓮華Iron Blossom
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.prop));
                break;
            case 65121003: // 靈魂震動
                eff.info.put(MapleStatInfo.time, 8000);
                eff.statups.put(MapleBuffStat.KeyDownMoving, 1);
                break;
            case 65121004: // 凝視靈魂Star Gazer
                eff.statups.put(MapleBuffStat.SoulGazeCriDamR, eff.info.get(MapleStatInfo.x));
                break;
            case 65121009: // 超新星之勇士Nova Warrior
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 65121011: // 索魂精通
                eff.statups.put(MapleBuffStat.AngelicBursterSoulSeeker, eff.info.get(MapleStatInfo.x));
                break;
            case 65121053: // 終極契約
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.asrR));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.terR));
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.indieStance));
                eff.statups.put(MapleBuffStat.CriticalBuff, eff.info.get(MapleStatInfo.x));
                break;
            case 65121054: // 靈魂深造
                // eff.statups.put(MapleBuffStat.RECHARGE, eff.info.get(MapleStatInfo.x));
                break;
            default:
                // System.out.println("Unhandled Buff: " + skill);
                break;
        }
    }
}
