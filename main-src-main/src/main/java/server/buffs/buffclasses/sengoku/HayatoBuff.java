package server.buffs.buffclasses.sengoku;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Charmander
 */
public class HayatoBuff extends AbstractBuffClass {

    public HayatoBuff() {
        buffs = new int[]{40011186, // 劍豪初心者拔刀術
            40011288, // 拔刀姿勢
            40011289, // 疾風五月雨刃
            40011290, // 百人一閃
            40011291, // 一般姿勢效果
            40011292, // 拔刀姿勢效果
            41001010, // 曉月流基本技
            41121014, // 疾風五月雨刃
            41110006, // 柳身
            41110009, // 心頭滅卻
            41101003, // 武神招來
            41101005, // 秘劍‧隼
            41121002, // 一閃
            41121003, // 剛健
            41121005, // 曉月勇者
            41001001, // 拔刀術
            41120006, // 迅速
            41121001, // 神速無雙
            41121015, // 制敵之先
            41110008, // 拔刀術‧心體技
            41121054, // 無雙十刃之型
            41121053, // 公主的加護
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is劍豪(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 40011186: // 劍豪初心者拔刀術
                eff.info.put(MapleStatInfo.time, 600000);
                eff.statups.put(MapleBuffStat.Speed, 10);
                break;
            case 40011288: // 拔刀姿勢
                eff.info.put(MapleStatInfo.time, -1);
                eff.statups.put(MapleBuffStat.HayatoStance, eff.info.get(MapleStatInfo.prop));
                break;
            case 40011289: // 疾風五月雨刃
            case 40011290: // 百人一閃
            case 41121014: // 疾風五月雨刃
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 40011291: // 一般姿勢效果
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndieMHPR, eff.info.get(MapleStatInfo.indieMhpR));
                eff.statups.put(MapleBuffStat.IndieMMPR, eff.info.get(MapleStatInfo.indieMmpR));
                eff.statups.put(MapleBuffStat.IndieIgnoreMobpdpR, eff.info.get(MapleStatInfo.indieIgnoreMobpdpR));
                eff.statups.put(MapleBuffStat.IndiePADR, eff.info.get(MapleStatInfo.indiePadR));
                break;
            case 40011292: // 拔刀姿勢效果
                eff.statups.put(MapleBuffStat.CriticalBuff, 35);
                eff.statups.put(MapleBuffStat.HayatoStanceBonus, 2);
                eff.statups.put(MapleBuffStat.IndieBDR, eff.info.get(MapleStatInfo.indieBDR));
                eff.statups.put(MapleBuffStat.IndieBooster, eff.info.get(MapleStatInfo.indieBooster));
                break;
            case 41001010: // 曉月流基本技
                eff.statups.put(MapleBuffStat.DamR, eff.info.get(MapleStatInfo.damR));
                eff.statups.put(MapleBuffStat.IDA_BUFF_529, 1);
                break;
            case 41110006: // 柳身
                eff.statups.put(MapleBuffStat.WILLOW_DODGE, eff.info.get(MapleStatInfo.damR));
                break;
            case 41110009: // 心頭滅卻
                eff.statups.put(MapleBuffStat.Regen, eff.info.get(MapleStatInfo.damage));
                break;
            case 41001001: // 拔刀術
                eff.statups.put(MapleBuffStat.CriticalBuff, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.HayatoStance, eff.info.get(MapleStatInfo.prop));
                eff.statups.put(MapleBuffStat.BATTOUJUTSU_STANCE, -1);
                break;
            case 41110008: // 拔刀術‧心體技
                eff.statups.put(MapleBuffStat.CriticalBuff, eff.info.get(MapleStatInfo.y));
                eff.statups.put(MapleBuffStat.HayatoStance, eff.info.get(MapleStatInfo.prop));
                eff.statups.put(MapleBuffStat.BATTOUJUTSU_STANCE, -1);
                eff.statups.put(MapleBuffStat.HayatoStanceBonus, 0);
                break;
            case 41101003: // 武神招來
                eff.statups.put(MapleBuffStat.Jump, eff.info.get(MapleStatInfo.jump));
                eff.statups.put(MapleBuffStat.Speed, eff.info.get(MapleStatInfo.speed));
                eff.statups.put(MapleBuffStat.HayatoPAD, eff.info.get(MapleStatInfo.padX));
                eff.statups.put(MapleBuffStat.HayatoHPR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.HayatoMPR, eff.info.get(MapleStatInfo.x));
                break;
            case 41101005: // 秘劍‧隼
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 41120006: // 迅速
                eff.statups.put(MapleBuffStat.JINSOKU, eff.info.get(MapleStatInfo.prop));
                break;
            case 41121001: // 神速無雙
                eff.statups.put(MapleBuffStat.KeyDownMoving, 0);
                break;
            case 41121002: // 一閃
                eff.statups.put(MapleBuffStat.HayatoCr, eff.info.get(MapleStatInfo.prop));
                break;
            case 41121003: // 剛健
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.y));
                break;
            case 41121005: // 曉月勇者
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 41121015: // 制敵之先
                eff.statups.put(MapleBuffStat.COUNTERATTACK, 0);
                break;
            case 41121054: // 無雙十刃之型
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.y));
                break;
            case 41121053: // 公主的加護
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            default:
                System.out.println("Hayato skill not coded: " + skill);
                break;
        }
    }
}
