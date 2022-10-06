package server.buffs.buffclasses.resistance;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import client.MonsterStatus;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

public class MechanicBuff extends AbstractBuffClass {

    public MechanicBuff() {
        buffs = new int[]{35101006, // 機甲戰神極速
            35001002, // 合金盔甲: 人型
            35120000, // 合金盔甲終極
            35111004, // 合金盔甲: 重機槍
            35121005, // 合金盔甲: 導彈罐
            35121013, // 合金盔甲: 重機槍
            35101007, // 全備型盔甲 (Perfect Armor)
            35111002, // 磁場
            35111005, // 加速器 : EX-7
            35121003, // 戰鬥機器 : 巨人錘
            35111011, // 治療機器人 : H-LX
            35121009, // 機器人工廠 : RM1
            35121011, 35111001, // 賽特拉特
            35111010, // 衛星
            35111009, // 賽特拉特
            35121010, // 擴音器 : AF-11
            35121006, // 終極賽特拉特
            35121007 // 楓葉祝福
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is機甲戰神(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 35121010: // 擴音器 : AF-11
                eff.info.put(MapleStatInfo.time, 60000);
                eff.statups.put(MapleBuffStat.HowlingAttackDamage, eff.info.get(MapleStatInfo.x));
                break;
            case 35101006: // 機甲戰神極速
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 35101005: // 開放通道 : GX-9
                eff.statups.put(MapleBuffStat.SoulArrow, eff.info.get(MapleStatInfo.x));
                break;
            case 35120000: // 合金盔甲終極
            case 35001002: // 合金盔甲: 人型
                eff.statups.put(MapleBuffStat.RideVehicle, 1932016);
                eff.statups.put(MapleBuffStat.EMHP, eff.info.get(MapleStatInfo.emhp));
                eff.statups.put(MapleBuffStat.EMMP, eff.info.get(MapleStatInfo.emmp));
                eff.statups.put(MapleBuffStat.EPAD, eff.info.get(MapleStatInfo.epad));
                eff.statups.put(MapleBuffStat.EPDD, eff.info.get(MapleStatInfo.epdd));
                eff.statups.put(MapleBuffStat.IndieSpeed, eff.info.get(MapleStatInfo.indieSpeed));
                break;
            case 35101007: // 全備型盔甲 (Perfect Armor)
                eff.statups.put(MapleBuffStat.Guard, eff.info.get(MapleStatInfo.x));
                break;
            case 35111002: // 磁場
                eff.statups.put(MapleBuffStat.SUMMON, 1);
                eff.monsterStatus.put(MonsterStatus.M_Stun, 1);
                break;
            case 35111005: // 加速器 : EX-7
                eff.statups.put(MapleBuffStat.SUMMON, 1);
                eff.monsterStatus.put(MonsterStatus.M_Speed, eff.info.get(MapleStatInfo.x));
                eff.monsterStatus.put(MonsterStatus.M_PDR, eff.info.get(MapleStatInfo.y));
                break;
            case 35121003: // 戰鬥機器 : 巨人錘
                eff.statups.put(MapleBuffStat.SUMMON, 1);
                break;
            case 35111011: // 治療機器人 : H-LX
            case 35121009: // 機器人工廠 : RM1
            case 35121011:
                eff.statups.put(MapleBuffStat.SUMMON, 1);
                break;
            case 35111001: // 賽特拉特
            case 35111010: // 衛星
            case 35111009: // 賽特拉特
                eff.statups.put(MapleBuffStat.PickPocket, 1);
                break;
            case 35121006: // 終極賽特拉特
                eff.statups.put(MapleBuffStat.Cyclone, eff.info.get(MapleStatInfo.x));
                // 終極賽特拉特_吸收
                // eff.statups.put(MapleBuffStat.Cyclone, eff.info.get(MapleStatInfo.y));
                break;
            case 35121013: // 合金盔甲: 重機槍
            case 35111004: // 合金盔甲: 重機槍
                eff.statups.put(MapleBuffStat.Mechanic, (int) eff.getLevel()); // ya wtf
                break;
            case 35121005: // 合金盔甲: 導彈罐
                eff.statups.put(MapleBuffStat.Mechanic, (int) eff.getLevel()); // ya wtf
                break;
            case 35121007:
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            default:
                System.out.println("未知的機甲戰神 Buff: " + skill);
                break;
        }
    }
}
