/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.adventurer;

import client.character.stat.MapleBuffStat;
import server.MapleStatEffect;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Weber
 */
public class BeginnerBuff extends AbstractBuffClass {

    public BeginnerBuff() {
        buffs = new int[]{0001105, // 冰騎士
    };
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 0001105:
                eff.statups.put(MapleBuffStat.BuffLimit, 2);
                break;
        }
    }

}
