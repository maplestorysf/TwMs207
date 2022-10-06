/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.vskills;

import client.MapleJob;
import client.character.stat.MapleBuffStat;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Weber
 */
public class VSkillBowmanBuff extends AbstractBuffClass {

    public VSkillBowmanBuff() {
        this.buffs = new int[]{400031003, // 狂風呼嘯
    };
    }

    @Override
    public boolean containsJob(int job) {
        return job == 40003;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 400031003: // 狂風呼嘯
                eff.statups.put(MapleBuffStat.狂風呼嘯, 1);
                break;
            case 400031006:
                eff.statups.put(MapleBuffStat.IncMonsterBattleCaptureRate, eff.info.get(MapleStatInfo.x));
                break;
        }
    }

}
