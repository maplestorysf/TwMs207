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
public class VSkillThiefBuff extends AbstractBuffClass {

    public VSkillThiefBuff() {
        this.buffs = new int[]{400041002, // 滅殺刃影
            400041003, // 滅殺刃影
            400041004, 400041005,
            400041001, // 散式投擲
    };
    }

    @Override
    public boolean containsJob(int job) {
        return job == 40004;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 400041002:
            case 400041003:
            case 400041004:
            case 400041005:
                eff.statups.put(MapleBuffStat.滅殺刃影, 3);
                break;
            case 400041001:
                eff.statups.put(MapleBuffStat.IndieForceJump, 1);
                eff.statups.put(MapleBuffStat.散式投擲, 1);
                break;
        }
    }

}
