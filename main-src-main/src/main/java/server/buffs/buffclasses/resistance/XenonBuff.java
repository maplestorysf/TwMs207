/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.resistance;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Maple
 */
public class XenonBuff extends AbstractBuffClass {

    public XenonBuff() {
        buffs = new int[]{30021237, // 自由飛行(Done)
            36001002, // 傾斜功率(Done)
            36101001, // 離子推進器
            36101002, // 線性透視(Done)
            36101003, // 效率管道
            36101004, // 能量加速器(Done)
            36111006, // 虛擬投影(Done)
            36111003, // 全面防禦
            36121003, // 全域代碼
            36121004, // 攻擊矩陣(Done)
            36121008, // 楓葉祝福(Done)
            36121054, // 發電機
            36121007, // 時空膠囊
            36111004, // 神盾系統
            36110005, // 三角列陣
            36001005,};
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is傑諾(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 30021237: // 自由飛行(Done)
                eff.info.put(MapleStatInfo.time, eff.info.get(MapleStatInfo.time) / 1000);
                eff.statups.put(MapleBuffStat.NewFlying, 1);
                break;
            case 36001002: // 傾斜功率
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 36101001: // 離子推進器
                eff.statups.put(MapleBuffStat.KeyDownMoving, eff.info.get(MapleStatInfo.x));
                break;
            case 36101002: // 線性透視
                eff.statups.put(MapleBuffStat.CriticalBuff, eff.info.get(MapleStatInfo.x));
                break;
            case 36101003: // 效率管道
                eff.statups.put(MapleBuffStat.IndieMMPR, eff.info.get(MapleStatInfo.indieMmpR));
                eff.statups.put(MapleBuffStat.IndieMHPR, eff.info.get(MapleStatInfo.indieMhpR));
                break;
            case 36101004: // 能量加速器
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 36111006: // 虛擬投影
                eff.info.put(MapleStatInfo.time, -1);
                eff.statups.put(MapleBuffStat.ShadowPartner, eff.info.get(MapleStatInfo.x));
                break;
            case 36111003: // 全面防禦
                eff.statups.put(MapleBuffStat.StackBuff, eff.info.get(MapleStatInfo.prop));
                eff.statups.put(MapleBuffStat.DamAbsorbShield, eff.info.get(MapleStatInfo.z));
                break;
            case 36121003: // 全域代碼
                eff.statups.put(MapleBuffStat.IndieBDR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 36121004: // 攻擊矩陣
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IgnoreTargetDEF, eff.info.get(MapleStatInfo.y));
                break;
            case 36121008: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 36121054:// 發電機
                eff.statups.put(MapleBuffStat.SurplusSupply, 20);
                eff.statups.put(MapleBuffStat.AmaranthGenerator, 1);
                break;
            case 36121007:// 時空膠囊
                eff.info.put(MapleStatInfo.time, 10000);// AddBugFixTest
                break;
            case 36111004: // 神盾系統
                eff.statups.put(MapleBuffStat.XenonAegisSystem, eff.info.get(MapleStatInfo.x));
                break;
            default:
                // System.out.println("Unhandled Xenon Buff: " + skill);
                break;
        }
    }
}
