/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.pinkbean;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Pungin
 */
public class PinkBeanBuff extends AbstractBuffClass {

    public PinkBeanBuff() {
        buffs = new int[]{131001000, // 皮卡啾攻擊
            131001001, // 皮卡啾攻擊
            131001002, // 皮卡啾攻擊
            131001003, // 皮卡啾攻擊
            131001101, // 皮卡啾攻擊
            131001102, // 皮卡啾攻擊
            131001103, // 皮卡啾攻擊
            131002000, // 皮卡啾攻擊 1打
            131001004, // 咕嚕咕嚕
            131001009, // 大家加油！
            131001010, // 超烈焰溜溜球
            131001015, // 迷你啾出動
            131001113, // 電吉他
            131001106, // 鬼～臉
            131001206, // 大口吃肉
            131001306, // Zzz
            131001406, // 謎一般的雞尾酒
            131001506, // 皮卡啾的頭戴式耳機
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is皮卡啾(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 131001000: // 皮卡啾攻擊
            case 131002000: // 皮卡啾攻擊 1打
                eff.statups.put(MapleBuffStat.PinkbeanAttackBuff, 10);
                break;
            case 131001001: // 皮卡啾攻擊
            case 131001101: // 皮卡啾攻擊
                eff.statups.put(MapleBuffStat.PinkbeanAttackBuff, 30);
                break;
            case 131001002: // 皮卡啾攻擊
            case 131001102: // 皮卡啾攻擊
                eff.statups.put(MapleBuffStat.PinkbeanAttackBuff, 60);
                break;
            case 131001003: // 皮卡啾攻擊
            case 131001103: // 皮卡啾攻擊
                eff.statups.put(MapleBuffStat.PinkbeanAttackBuff, 100);
                break;
            case 131001004: // 咕嚕咕嚕
                eff.statups.put(MapleBuffStat.KeyDownMoving, 300);
                break;
            case 131001009: // 大家加油！
                eff.statups.put(MapleBuffStat.IndieEXP, eff.info.get(MapleStatInfo.indieExp));
                eff.statups.put(MapleBuffStat.IndieSpeed, eff.info.get(MapleStatInfo.indieSpeed));
                eff.statups.put(MapleBuffStat.IndiePADR, eff.info.get(MapleStatInfo.indiePadR));
                eff.statups.put(MapleBuffStat.IndieMADR, eff.info.get(MapleStatInfo.indieMadR));
                break;
            case 131001010: // 超烈焰溜溜球
                eff.statups.put(MapleBuffStat.PinkbeanYoYoStack, 0);
                break;
            case 131001015: // 迷你啾出動
                eff.statups.put(MapleBuffStat.PinkbeanMinibeenMove, 1);
                break;
            case 131001113: // 電吉他
                eff.statups.put(MapleBuffStat.IndiePADR, eff.info.get(MapleStatInfo.indiePadR));
                eff.statups.put(MapleBuffStat.IndieMADR, eff.info.get(MapleStatInfo.indieMadR));
                break;
            case 131001106: // 鬼～臉
                eff.statups.put(MapleBuffStat.PinkbeanRelax, 1);
                eff.statups.put(MapleBuffStat.IndieEXP, eff.info.get(MapleStatInfo.indieExp));
                // eff.monsterStatus.put(MonsterStatus.防禦率, eff.info.get(MapleStatInfo.z));
                break;
            case 131001206: // 大口吃肉
                eff.statups.put(MapleBuffStat.DotHealHPPerSecond, 386);// ?
                eff.statups.put(MapleBuffStat.PinkbeanRelax, 1);
                eff.statups.put(MapleBuffStat.IndieEXP, eff.info.get(MapleStatInfo.indieExp));
                break;
            case 131001306: // Zzz
                eff.statups.put(MapleBuffStat.PinkbeanRelax, 1);
                eff.statups.put(MapleBuffStat.IndiePADR, eff.info.get(MapleStatInfo.indiePadR));
                eff.statups.put(MapleBuffStat.IndieEXP, eff.info.get(MapleStatInfo.indieExp));
                break;
            case 131001406: // 謎一般的雞尾酒
                eff.statups.put(MapleBuffStat.PinkbeanRelax, 1);
                eff.statups.put(MapleBuffStat.IndieAsrR, eff.info.get(MapleStatInfo.indieAsrR));
                eff.statups.put(MapleBuffStat.IndieEXP, eff.info.get(MapleStatInfo.indieExp));
                break;
            case 131001506: // 皮卡啾的頭戴式耳機
                eff.statups.put(MapleBuffStat.PinkbeanRelax, 1);
                eff.statups.put(MapleBuffStat.IndiePADR, eff.info.get(MapleStatInfo.indiePadR));
                eff.statups.put(MapleBuffStat.IndieAsrR, eff.info.get(MapleStatInfo.indieAsrR));
                eff.statups.put(MapleBuffStat.IndieEXP, eff.info.get(MapleStatInfo.indieExp));
                break;
            default:
                System.out.println("未知的 皮卡啾(13100) Buff: " + skill);
                break;
        }
    }
}
