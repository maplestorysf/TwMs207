/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.zero;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Itzik
 */
public class ZeroBuff extends AbstractBuffClass {

    public ZeroBuff() { // since only beginner job has buffs we put them in first job buffs
        buffs = new int[]{100001005, // Temple Recall
            100001263, // 時之威能
            100001264, // 聖靈神速
            100001268, // Rhinne's Protection
            100001269, 100001270, 100001272};
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is神之子(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        // If this initial check and the corresponding arrays are removed,
        // there should not be any impact (i.e., it will keep its functionality).
        if (!containsSkill(skill)) {
            return;
        }

        switch (skill) {
            case 100001005: // Focused Time
                eff.statups.put(MapleBuffStat.DispelItemOptionByField, eff.info.get(MapleStatInfo.x));
                break;
            case 100001268: // Rhinne's Protection
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 100001263: // 時之威能
                eff.statups.put(MapleBuffStat.ZeroAuraStr, 1);
                eff.statups.put(MapleBuffStat.IndieTerR, eff.info.get(MapleStatInfo.indieTerR));
                eff.statups.put(MapleBuffStat.IndieAsrR, eff.info.get(MapleStatInfo.indieAsrR));
                eff.statups.put(MapleBuffStat.IndiePDD, eff.info.get(MapleStatInfo.indiePdd));
                eff.statups.put(MapleBuffStat.IndieMAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indieMad));
                break;
            case 100001264: // 聖靈神速
                eff.statups.put(MapleBuffStat.ZeroAuraSpd, 1);
                eff.statups.put(MapleBuffStat.IndieACC, eff.info.get(MapleStatInfo.indieAcc));
                eff.statups.put(MapleBuffStat.IndieEVA, eff.info.get(MapleStatInfo.indieEva));
                eff.statups.put(MapleBuffStat.IndieJump, eff.info.get(MapleStatInfo.indieJump));
                eff.statups.put(MapleBuffStat.IndieSpeed, eff.info.get(MapleStatInfo.indieSpeed));
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));
                break;
            default:
                // System.out.println("Unhandled Buff: " + skill);
                break;
        }
    }
}
