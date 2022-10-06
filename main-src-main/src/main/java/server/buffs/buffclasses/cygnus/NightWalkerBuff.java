/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.cygnus;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Fate
 */
public class NightWalkerBuff extends AbstractBuffClass {

    public NightWalkerBuff() {
        buffs = new int[]{14001003, // 隱身術
            14001021, // 元素 : 闇黑
            14001022, // 急速
            14001023, // 黑暗面
            14001027, // 暗影蝙蝠
            14101022, // 投擲助推器
            14111000, // 影分身
            14111025, // 精神投擲
            14111024, // 暗影僕從
            14121053,};
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is暗夜行者(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 14111024: // 暗影僕從
                eff.statups.put(MapleBuffStat.ShadowServant, eff.info.get(MapleStatInfo.x));
                break;
            case 14001003: // 隱身術
                eff.statups.put(MapleBuffStat.DarkSight, eff.info.get(MapleStatInfo.x));
                break;
            case 14001021: // 元素 : 闇黑
                eff.statups.put(MapleBuffStat.ElementDarkness, 1);
                break;
            case 14001022: // 急速
                eff.statups.put(MapleBuffStat.Jump, eff.info.get(MapleStatInfo.jump));
                eff.statups.put(MapleBuffStat.Speed, eff.info.get(MapleStatInfo.speed));
                break;
            case 14001023: // 黑暗面
                eff.statups.put(MapleBuffStat.DarkSight, eff.info.get(MapleStatInfo.x));
                break;
            case 14001027: // 暗影蝙蝠
                eff.statups.put(MapleBuffStat.NightWalkerBat, 1);
                break;
            case 14101022: // 投擲助推器
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 14111000: // 影分身
                eff.statups.put(MapleBuffStat.ShadowPartner, eff.info.get(MapleStatInfo.x));
                break;
            case 14111025: // 精神投擲
                eff.statups.put(MapleBuffStat.NoBulletConsume, 0);
                break;
            case 14121053:// 守護者的榮耀
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            default:
                // System.out.println("暗夜行者 skill not coded: " + skill);
                break;
        }
    }
}
