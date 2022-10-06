/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.others;

import client.character.stat.MapleBuffStat;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Weber
 */
public class Skill800011 extends AbstractBuffClass {

    public Skill800011() {
        this.buffs = new int[]{80001137, // 黑暗貓頭鷹
    };
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 80001137:
                eff.statups.put(MapleBuffStat.PDD, eff.info.get(MapleStatInfo.pdd));
                break;
        }
    }

}
