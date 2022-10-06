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
public class PirateBuff extends AbstractBuffClass {

    public PirateBuff() {
        buffs = new int[]{
            // 一轉
            5001005, // 衝鋒Dash

            // 二轉
            // 打手
            5101006, // 致命快打Knuckle Booster
            5101011, // 全神貫注Dark Clarity
            // 槍手
            5201003, // 迅雷再起Gun Booster
            5201008, // 無形彈藥Infinity Blast
            // 重砲兵
            5301002, // 加農砲推進器Cannon Booster
            5301003, // 猴子的魔法Monkey Magic

            // 三轉
            // 格鬥家
            5111007, // 幸運骰子Roll Of The Dice
            5111010, // 雲體風身
            // 神槍手
            5211007, // 幸運骰子Roll Of The Dice
            5211009, // 魔法彈丸Cross Cut Blast
            // 重砲兵隊長
            // 5311002, // 猴子的衝擊波
            5311004, // 幸運木桶Barrel Roulette
            5311005, // 幸運骰子Luck of the Die

            // 四轉
            // 拳霸
            5121000, // 楓葉祝福Maple Warrior
            5121009, // 最終極速Speed Infusion
            5120011, // 反擊姿態
            5120012, // 雙倍幸運骰子
            5121015, // 拳霸大師Crossbones
            // 槍神
            5220012, // 反擊
            5220014, // 雙倍幸運骰子
            5221000, // 楓葉祝福Maple Warrior
            5221004, // 瞬‧迅雷
            5221018, // 海盜風采Jolly Roger
            // 5221021, // 極速之指Quickdraw
            // 重砲指揮官
            5320007, // 雙倍幸運骰子
            5320008, // 神聖猴子的咒語
            5321005, // 楓葉祝福Maple Warrior
            5321010, // 百烈精神Pirate's Spirit

            // 超技
            // 拳霸
            // 5121052, // 家族之力
            5121053, // 傳說冒險Epic Adventure
            5121054, // 暴能續發Stimulating Conversation
            // 槍神
            5221053, // 傳說冒險Epic Adventure
            5221054, // 撫慰甘露Whaler's Potion
            // 重砲指揮官
            5321053, // 傳說冒險Epic Adventure
            5321054, // 壓制砲擊Buckshot
        };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is冒險家(job) && MapleJob.is海盜(job) && !MapleJob.is蒼龍俠客(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 5001005: // 衝鋒Dash
                eff.statups.put(MapleBuffStat.DashSpeed, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.DashJump, eff.info.get(MapleStatInfo.y));
                break;
            case 5221004: // 瞬‧迅雷
                eff.statups.put(MapleBuffStat.KeyDownMoving, eff.info.get(MapleStatInfo.x));
                break;
            case 5101011: // Dark Clarity
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.IndieACC, eff.info.get(MapleStatInfo.indieAcc));
                break;
            case 5101006: // Knuckle Booster
            case 5201003: // Gun Booster
            case 5301002: // Cannon Booster
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 5111007: // 幸運骰子Roll Of The Dice
            case 5211007: // 幸運骰子Roll Of The Dice
            case 5311005: // 幸運骰子Luck of the Die
            case 5120012: // 雙倍幸運骰子
            case 5220014: // 雙倍幸運骰子
            case 5320007: // 雙倍幸運骰子
                eff.statups.put(MapleBuffStat.Dice, 0);
                break;
            case 5111010: // 雲體風身
                eff.statups.put(MapleBuffStat.DamAbsorbShield, eff.info.get(MapleStatInfo.x));
                break;
            case 5121015: // 拳霸大師
                eff.statups.put(MapleBuffStat.DamR, eff.info.get(MapleStatInfo.x));
                break;
            case 5121009: // Speed Infusion
                eff.statups.put(MapleBuffStat.PartyBooster, eff.info.get(MapleStatInfo.x));
                break;
            case 5121054: // Stimulating Conversation
                // TODO 拳霸BUFF
                break;
            case 5221018: // 海盜風采 RogerJolly Roger
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.IndiePADR, eff.info.get(MapleStatInfo.indiePadR));
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.z));
                eff.statups.put(MapleBuffStat.EVA, eff.info.get(MapleStatInfo.eva));
                break;
            case 5201008: // Infinity Blast
                eff.statups.put(MapleBuffStat.NoBulletConsume, 0);
                break;
            case 5211009: // Cross Cut Blast
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 5221054: // Whaler's Potion
                // TODO 槍神BUFF
                break;
            case 5301003: // 猴子的魔法
            case 5320008: // 神聖猴子的咒語
                eff.statups.put(MapleBuffStat.IndieMHP, eff.info.get(MapleStatInfo.indieMhp));
                eff.statups.put(MapleBuffStat.IndieMMP, eff.info.get(MapleStatInfo.indieMmp));
                eff.statups.put(MapleBuffStat.IndieACC, eff.info.get(MapleStatInfo.indieAcc));
                eff.statups.put(MapleBuffStat.IndieEVA, eff.info.get(MapleStatInfo.indieEva));
                eff.statups.put(MapleBuffStat.IndieJump, eff.info.get(MapleStatInfo.indieJump));
                eff.statups.put(MapleBuffStat.IndieSpeed, eff.info.get(MapleStatInfo.indieSpeed));
                eff.statups.put(MapleBuffStat.IndieAllStat, eff.info.get(MapleStatInfo.indieAllStat));
                break;
            case 5311004: // Barrel Roulette
                eff.statups.put(MapleBuffStat.Roulette, 0);
                break;
            case 5321010: // 百烈精神
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.x));
                break;
            case 5321054: // 壓制砲擊Buckshot
                eff.statups.put(MapleBuffStat.BuckShot, 1);
                break;
            case 5120011: // 反擊姿態
            case 5220012: // 反擊
                eff.info.put(MapleStatInfo.cooltime, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.DamR, (int) eff.info.get(MapleStatInfo.damR)); // i think
                break;
            case 5121000: // Maple Warrior
            case 5221000: // Maple Warrior
            case 5321005: // Maple Warrior
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 5121053: // Epic Adventure
            case 5221053: // Epic Adventure
            case 5321053: // Epic Adventure
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            default:
                System.out.println("海盜技能未處理,技能代碼: " + skill);
                break;
        }
    }
}
