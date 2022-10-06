/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.beasttamer;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Maple
 */
public class BeastTamerBuff extends AbstractBuffClass {

    public BeastTamerBuff() {
        buffs = new int[]{110001501, // 召喚熊熊Bear Mode
            110001502, // 召喚雪豹Snow Leopard Mode
            110001503, // 召喚雀鷹Hawk Mode
            110001504, // 召喚貓咪Cat Mode
            112001009, // 集中打擊Bear Assault
            112111000, // 艾卡飛行
            112120022, // 阿樂的弱點探索
            112101016, // 小豹呼喚
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is幻獸師(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 112120023: // 阿樂的飽足感
                eff.statups.put(MapleBuffStat.IndieMHP, eff.info.get(MapleStatInfo.indieMhp));
                eff.statups.put(MapleBuffStat.IndieMMP, eff.info.get(MapleStatInfo.indieMmp));
                break;
            case 112120017: // 阿樂的竊取
                eff.statups.put(MapleBuffStat.IndiePMdR, eff.info.get(MapleStatInfo.v));
                break;
            case 112120018: // 阿樂的紙甲
                eff.statups.put(MapleBuffStat.IndieCr, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndieCrMax, eff.info.get(MapleStatInfo.y));
                break;
            case 112101016: // 小豹呼喚
                eff.statups.put(MapleBuffStat.Revive, eff.info.get(MapleStatInfo.x));
                break;
            case 112120021: // 強化阿樂的魅力
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.IndieMAD, eff.info.get(MapleStatInfo.indieMad));
                break;
            case 112120022: // 阿樂的弱點探索
                eff.statups.put(MapleBuffStat.IgnoreTargetDEF, eff.info.get(MapleStatInfo.x));
                break;
            case 112000012:
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));
                break;
            case 110001501:
                eff.statups.put(MapleBuffStat.AnimalChange, 1);
                break;
            case 110001502:
                eff.statups.put(MapleBuffStat.AnimalChange, 2);
                break;
            case 110001503:
                eff.statups.put(MapleBuffStat.AnimalChange, 3);
                break;
            case 110001504:
                eff.statups.put(MapleBuffStat.AnimalChange, 4);
                break;
            default:
                // System.out.println("BeastTamer skill not coded: " + skill);
                break;
        }
    }
}
