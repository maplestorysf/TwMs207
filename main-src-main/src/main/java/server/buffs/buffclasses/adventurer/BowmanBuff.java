/*
 * To change this template, choose Tools | Templates
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
 * @author Maple
 */
public class BowmanBuff extends AbstractBuffClass {

    public BowmanBuff() {
        buffs = new int[]{
            // 二轉
            // 獵人
            3101002, // 快速之弓Bow Booster
            3101004, // 無形之箭SoulArrow
            3101009, // 魔幻箭筒
            // 弩弓手
            3201002, // 快速之弩Bow Booster
            3201004, // 無形之箭Soul Arrow xBow

            // 三轉
            // 遊俠
            // TODO 3110007, //躲避
            // TODO 3110012, //集中專注
            // TODO 3111011, //終極射擊：箭
            3111000, // 集中
            // 狙擊手
            // TODO 3210007, //躲避
            3211011, // 痛苦殺手PainKiller
            // TODO 3211012, //終極射擊：弩
            // TODO 3210013, //反向傷害

            // 四轉
            // 箭神
            3121000, // 楓葉祝福MapleWarrior
            3121002, // 會心之眼SharpEyes
            3121007, // 爆發Illusion Step
            3121016, // 無限箭筒
            // 神射手
            3221000, // 楓葉祝福Maple Warrior
            3221002, // 會心之眼Sharp Eye
            3221006, // 爆發Illusion Step

            // 超技
            // 箭神
            3121053, // 傳說冒險Epic Adventure
            3121054, // 戰鬥準備Constentration
            // 神射手
            3221053, // 傳說冒險Epic Adventure
            3221054, // 專注弱點Bullseye Shot
        };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is冒險家(job) && MapleJob.is弓箭手(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 3101002: // Bow Booster
            case 3201002: // Bow Booster
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 3101004: // SoulArrow bow
            case 3201004: // SoulArrow xbow
                eff.statups.put(MapleBuffStat.EPAD, eff.info.get(MapleStatInfo.epad));
                eff.statups.put(MapleBuffStat.SoulArrow, eff.info.get(MapleStatInfo.x));
                break;
            case 3111000: // 集中
                eff.statups.put(MapleBuffStat.Concentration, eff.info.get(MapleStatInfo.x));
                break;
            case 3211011: // PainKiller
                eff.statups.put(MapleBuffStat.AntiMagicShell, eff.info.get(MapleStatInfo.asrR));
                eff.statups.put(MapleBuffStat.AntiMagicShell, eff.info.get(MapleStatInfo.terR));
                break;
            case 3121000: // Maple Warrior
            case 3221000: // Maple Warrior
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 3121002: // Sharp Eyes
            case 3221002: // Sharp Eye
                eff.statups.put(MapleBuffStat.SharpEyes,
                        (eff.info.get(MapleStatInfo.x) << 8) + eff.info.get(MapleStatInfo.criticaldamageMax));
                break;
            case 3121007: // Illusion Step
            case 3221006: // Illusion Step
                eff.statups.put(MapleBuffStat.DEX, eff.info.get(MapleStatInfo.dex));
                // add more
                break;
            case 3121016:
                eff.statups.put(MapleBuffStat.AdvancedQuiver, eff.info.get(MapleStatInfo.x));
                break;
            case 3121053: // Epic Adventure
            case 3221053: // Epic Adventure
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            case 3121054: // Consentration
                eff.statups.put(MapleBuffStat.BleedingToxin, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 3221054: // BullsEye Shot
                break;
            case 3101009: // 魔幻箭筒
                eff.statups.put(MapleBuffStat.QuiverCatridge, 101010);
                break;
            default:
                System.out.println("弓箭手技能未處理,技能代碼: " + skill);
                break;
        }
    }
}
