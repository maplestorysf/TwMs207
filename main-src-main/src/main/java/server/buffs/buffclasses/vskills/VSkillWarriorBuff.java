/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.vskills;

import client.character.stat.MapleBuffStat;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Weber
 */
public class VSkillWarriorBuff extends AbstractBuffClass {

    public VSkillWarriorBuff() {
        this.buffs = new int[]{400011001, // 燃燒靈魂之劍

            400011006, // 惡魔覺醒
            400011010, // 惡魔狂亂

            400011057, // 耶夢加得
    };
    }

    @Override
    public boolean containsJob(int job) {
        return job == 40001;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 400011001:
                eff.statups.put(MapleBuffStat.交換攻擊, eff.info.get(MapleStatInfo.x));
                break;
            case 400011006:
                eff.statups.put(MapleBuffStat.IndieCr, eff.info.get(MapleStatInfo.indieCr));
                eff.statups.put(MapleBuffStat.IndieMDDR, eff.info.get(MapleStatInfo.ignoreMobpdpR));
                eff.statups.put(MapleBuffStat.IndieForceJump, 1);
                eff.statups.put(MapleBuffStat.交換攻擊, (int) eff.getLevel());
                break;
            case 400011010:
                eff.statups.put(MapleBuffStat.惡魔狂亂, eff.info.get(MapleStatInfo.x));
                break;
        }
    }

}
