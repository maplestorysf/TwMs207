/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.hero;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Charmander
 */
public class AranBuff extends AbstractBuffClass {

    public AranBuff() {
        buffs = new int[]{21000000, // 矛之鬥氣
            21001003, // Polearm Booster
            21001008, // 強化連擊
            21101006, // Snow Charge
            21101005, // Combo Drain
            21111001, // Might
            21111009, // Combo Recharge
            21111012, // Maha Blessing
            21121007, // Combo Barrier
            21121000, // Maple Warrior
            21121054, // Unlimited Combo
            21121053, // Heroic Memories
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is狂狼勇士(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 21000000: // 矛之鬥氣
                eff.statups.put(MapleBuffStat.ComboAbilityBuff, 100);
                break;
            case 21001003: // 神速之矛
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 21001008: // 強化連擊
                eff.statups.put(MapleBuffStat.BodyPressure, eff.info.get(MapleStatInfo.x));
                break;
            case 21101006: // 寒冰屬性
                eff.statups.put(MapleBuffStat.WeaponCharge, eff.info.get(MapleStatInfo.x));
                break;
            case 21101005: // 吸血術
                eff.statups.put(MapleBuffStat.AranDrain, eff.info.get(MapleStatInfo.x));
                break;
            case 21111001: // Might
                eff.statups.put(MapleBuffStat.RepeatEffect, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.EPAD, eff.info.get(MapleStatInfo.epad));
                eff.statups.put(MapleBuffStat.EPDD, eff.info.get(MapleStatInfo.epdd));
                break;
            case 21111009: // Combo Recharge
                eff.statups.put(MapleBuffStat.ComboAbilityBuff, eff.info.get(MapleStatInfo.x));
                break;
            case 21111012: // Maha Blessing
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.IndieMAD, eff.info.get(MapleStatInfo.indieMad));
                break;
            case 21121007: // Combo Barrier
                eff.statups.put(MapleBuffStat.ComboBarrier, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.PDD, eff.info.get(MapleStatInfo.pdd));
                break;
            case 21121000: // Maple Warrior
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 21121054: // Unlimited Combo
                eff.statups.put(MapleBuffStat.EventRate, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 21121053: // Heroic Memories
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            default:
                // System.out.println("Aran skill not coded: " + skill);
                break;
        }
    }
}
