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
public class VSkillPirateBuff extends AbstractBuffClass {

    public VSkillPirateBuff() {
        this.buffs = new int[]{400051002, // 元氣覺醒
            400051011, // 能量爆炸
            400051009, // 多重屬性：M-FL
    };
    }

    @Override
    public boolean containsJob(int job) {
        return job == 40005;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 400051002:
                eff.statups.put(MapleBuffStat.元氣覺醒, eff.info.get(MapleStatInfo.w));
                break;
            case 400051011:
                eff.statups.put(MapleBuffStat.能量爆炸, 1);
                break;
            case 400051009:
                eff.statups.put(MapleBuffStat.HowlingDefence, eff.info.get(MapleStatInfo.z));
                break;
        }
    }

}
