/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.vskills;

import server.MapleStatEffect;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Weber
 */
public class VSkillCommonBuff extends AbstractBuffClass {

    public VSkillCommonBuff() {
        this.buffs = new int[]{400001002, // 進階會心之眼
            400001005, // 實用的進階祝福
    };
    }

    @Override
    public boolean containsJob(int job) {
        return job == 40000;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
    }
}
