/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.resistance;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import constants.GameConstants;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Sunny
 */
public class DemonBuff extends AbstractBuffClass {

    public DemonBuff() {
        buffs = new int[]{
            // 惡魔殺手
            31001001, // 惡魔推進器
            31101003, // 黑暗復仇
            31111004, // 黑暗耐力
            31121007, // 無限力量
            31121004, // 楓葉祝福
            31121005, // 變形Dark Metamorphosis
            31121002, // 吸血鬼之觸
            31121054, // 高貴血統
            // 惡魔復仇者
            31011000, // 超越 : 十文字斬
            31011004, // 超越 : 十文字斬(2階)
            31011005, // 超越 : 十文字斬(3階)
            31011006, // 超越 : 十文字斬(4階)
            31011007, // 超越 : 十文字斬(5階)
            31201000, // 超越：惡魔風暴
            31201007, // 超越：惡魔風暴(2階)
            31201008, // 超越：惡魔風暴(3階)
            31201009, // 超越：惡魔風暴(4階)
            31201010, // 超越：惡魔風暴(5階)
            31211000, // 超越：月光斬
            31211007, // 超越：月光斬(2階)
            31211008, // 超越：月光斬(3階)
            31211009, // 超越：月光斬(4階)
            31211010, // 超越：月光斬(5階)
            31221000, // 超越 : 逆十文字斬
            31221009, // 超越 : 逆十文字斬(2階)
            31221010, // 超越 : 逆十文字斬(3階)
            31221011, // 超越 : 逆十文字斬(4階)
            31221012, // 超越 : 逆十文字斬(5階)
            31011001, // 超載解放
            31201002, // 急速惡魔
            31201003, // 深淵之怒
            31211001, // 噬魂爆發
            31211003, // 邪惡強化
            31211004, // 急速療癒
            31221004, // 地獄之力
            31221008, // 楓葉祝福
            31221053, // 自由意志
            31221054, // 禁忌契約
            30010230, // 超越
            30010242, // 血之限界
        };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is惡魔(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 30010230: // 超越
                eff.statups.put(MapleBuffStat.OverloadCount, 1);
                break;
            case 30010242: // 血之限界
                eff.statups.put(MapleBuffStat.LifeTidal, eff.info.get(MapleStatInfo.x));
                break;
            // 惡魔殺手
            case 31001001: // 惡魔推進器
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 31101003: // 黑暗復仇
                eff.statups.put(MapleBuffStat.PowerGuard, eff.info.get(MapleStatInfo.y));
                break;
            case 31111004: // 黑暗耐力
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.z));
                eff.statups.put(MapleBuffStat.DDR, eff.info.get(MapleStatInfo.x));
                break;
            case 31121005: // 變形Dark Metamorphosis
                eff.statups.put(MapleBuffStat.DamR, eff.info.get(MapleStatInfo.damR));
                eff.statups.put(MapleBuffStat.IndieMHPR, eff.info.get(MapleStatInfo.indieMhpR));
                eff.statups.put(MapleBuffStat.DevilishPower, 1);
                break;
            case 31121007: // 無限力量
                eff.statups.put(MapleBuffStat.InfinityForce, 1);
                break;
            case 31121004: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 31121002: // 吸血鬼之觸
                eff.statups.put(MapleBuffStat.VampiricTouch, eff.info.get(MapleStatInfo.x));
                break;
            case 31121054: // 高貴血統
                eff.statups.put(MapleBuffStat.ShadowPartner, eff.info.get(MapleStatInfo.x));
                break;
            // 惡魔復仇者
            case 31011001: // 超載解放
                eff.statups.put(MapleBuffStat.IndieMHPR, eff.info.get(MapleStatInfo.indieMhpR));
                break;
            case 31201002: // 急速惡魔
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 31201003: // 深淵之怒
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 31211001: // 噬魂爆發
                eff.info.put(MapleStatInfo.time, 8000);
                eff.statups.put(MapleBuffStat.KeyDownMoving, 1);
                break;
            case 31211003: // 邪惡強化
                eff.statups.put(MapleBuffStat.DamAbsorbShield, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.z));
                break;
            case 31211004: // 急速療癒
                eff.statups.put(MapleBuffStat.DiabolikRecovery, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndieMHPR, eff.info.get(MapleStatInfo.indieMhpR));
                break;
            case 31221004: // 地獄之力
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));
                break;
            case 31221008: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 31221053: // 自由意志
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            case 31221054: // 禁忌契約
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            default:
                if (GameConstants.isExceedAttack(skill)) {
                    eff.statups.put(MapleBuffStat.Exceed, 1);
                    eff.info.put(MapleStatInfo.time, 15000);
                } else {
                    System.out.println("未知的 惡魔(3100) Buff: " + skill);
                }
                break;
        }
    }
}
