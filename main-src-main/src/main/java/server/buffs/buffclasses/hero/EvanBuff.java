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
public class EvanBuff extends AbstractBuffClass {

    public EvanBuff() {
        buffs = new int[]{22001012, // 魔心防禦Magic Guard
            22131001, // Magic Shield
            22131002, // Elemental Decrease
            22111020, // 極速詠唱
            22151003, // Magic Resistance
            22161004, // 龍神的庇護Onyx Shroud
            22161005, // 瞬間移動精通
            22171000, // Maple Warrior
            22181003, // Soul Stone
            22181004, // Onyx Will
            22181000, // Blessing of the Onyx
            22171054, // Frenzied Soul
            22171053, // Heroic Memories
            22171052, // 聖歐尼斯龍
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is龍魔導士(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {

            case 22001012: // 魔心防禦Magic Guard [完成]
                eff.statups.put(MapleBuffStat.MagicGuard, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.Invincible, eff.info.get(MapleStatInfo.y));
                break;
            case 22141016: // 自然力重置 [完成]
                eff.statups.put(MapleBuffStat.ElementalReset, eff.info.get(MapleStatInfo.x));
                break;
            case 22111020: // 極速詠唱 [完成]
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 22171068: // 楓葉祝福 [完成]
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 22171073: // 歐尼斯的祝福 [完成]
                eff.statups.put(MapleBuffStat.EMAD, eff.info.get(MapleStatInfo.emad));
                eff.statups.put(MapleBuffStat.EPDD, eff.info.get(MapleStatInfo.epdd));
                break;
            case 22171052: // 聖歐尼斯龍
                eff.statups.put(MapleBuffStat.IndieAsrR, eff.info.get(MapleStatInfo.indieAsrR));
                break;
            default:
                // System.out.println("Evan skill not coded: " + skill);
                break;
        }
    }
}
