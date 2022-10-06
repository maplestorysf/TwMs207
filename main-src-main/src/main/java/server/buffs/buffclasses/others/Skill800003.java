/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.others;

import client.character.stat.MapleBuffStat;
import server.MapleStatEffect;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Weber
 */
public class Skill800003 extends AbstractBuffClass {

    public Skill800003() {
        buffs = new int[]{80000329 // 自由精神 (for GM)
    };
    }

    @Override
    public boolean containsJob(int job) {
        return true;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 80000329:
                eff.statups.put(MapleBuffStat.NotDamaged, 1);
                eff.setDuration(Integer.MAX_VALUE);
                break;
        }
    }
}
