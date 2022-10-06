/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.adventurer;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Administrator
 */
public class ChivalrousBuff extends AbstractBuffClass {

    public ChivalrousBuff() {
        buffs = new int[]{
            // 一轉
            5081023, // 追影連擊
            // 二轉
            5701013, // 真氣流貫
            // 三轉
            5711024, // 天地無我
            // 四轉
            5721000, // 楓葉祝福
            5721066, // 千斤墜
            // 超技
            5721053, // 史詩冒險Epic Adventure
            5721054, // 醉臥竹林
        };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is蒼龍俠客(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 5081023: // 追影連擊
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 5701013: // 真氣流貫
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieCr, eff.info.get(MapleStatInfo.indieCr));
                eff.statups.put(MapleBuffStat.STR, eff.info.get(MapleStatInfo.str));
                eff.statups.put(MapleBuffStat.DEX, eff.info.get(MapleStatInfo.dex));
                break;
            case 5711024: // 天地無我
                eff.statups.put(MapleBuffStat.IllusionStep, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.ACCR, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.DEXR, eff.info.get(MapleStatInfo.prop));
                break;
            case 5721000: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 5721066: // 千斤墜
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.prop));
                eff.statups.put(MapleBuffStat.IndieCr, eff.info.get(MapleStatInfo.indieCr));
                eff.statups.put(MapleBuffStat.IndieAllStat, eff.info.get(MapleStatInfo.indieAllStat));
                eff.statups.put(MapleBuffStat.IndieBDR, eff.info.get(MapleStatInfo.bdR));
                break;
            case 5721053: // 史詩冒險Epic Adventure
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            case 5721054: // 醉臥竹林Epic Adventure
                eff.statups.put(MapleBuffStat.IndieMHPR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.DamAbsorbShield, eff.info.get(MapleStatInfo.w));
                break;
            default:
                System.out.println("未知的 蒼龍俠客(572) Buff技能: " + skill);
                break;
        }
    }
}
