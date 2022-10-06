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
public class VSkillMagicianBuff extends AbstractBuffClass {

    public VSkillMagicianBuff() {
        this.buffs = new int[]{400021003, // 主教=>聖靈祈禱

            400021006, // 煉獄=>聯盟繩索

            400021017, // 雪女招喚
            400021018, // 雪女招喚
    };
    }

    @Override
    public boolean containsJob(int job) {
        return job == 40002;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 400021003:
                eff.statups.put(MapleBuffStat.聖靈祈禱, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.DebuffTolerance, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.DotHealHPPerSecond, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndieMDDR, eff.info.get(MapleStatInfo.q));
                break;
            case 400021006:
                eff.statups.put(MapleBuffStat.IndieForceJump, 1);
                eff.statups.put(MapleBuffStat.BattlePvP_Helena_Mark, (int) eff.getLevel());
                eff.statups.put(MapleBuffStat.交換攻擊, (int) eff.getLevel());
                break;
            case 400021017: // 雪女招喚
            case 400021018: // 雪女招喚
                eff.statups.put(MapleBuffStat.ComboDrain, eff.info.get(MapleStatInfo.y));
                break;
        }
    }

}
