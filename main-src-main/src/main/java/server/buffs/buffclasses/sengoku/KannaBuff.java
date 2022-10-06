package server.buffs.buffclasses.sengoku;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import client.MonsterStatus;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Charmander
 */
public class KannaBuff extends AbstractBuffClass {

    public KannaBuff() {
        buffs = new int[]{42101023, // 幽玄氣息
            42121023, // 幽玄氣息
            42101021, // 花炎結界
            42121021, // 花炎結界
            42101022, // 花狐的祝福
            42121022, // 花狐的祝福

            42101002, // 影朋‧花狐
            42101003, // 扇‧孔雀
            42111004, // 結界‧櫻
            42121005, // 結界‧桔梗
            42121006, // 曉月勇者
            42121000, // 破邪連擊符
            42120003, // 猩猩火酒
            42121004, // 退魔流星符
            42121052, // 百鬼夜行
            42121054, // 結界‧破魔
            42121053, // 公主的加護

            42101004, // 紫扇仰波‧焰
            42111006, // 紫扇仰波‧零
            42121008, // Mighty Shikigami Haunting
            42121024, // 紫扇白狐
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is陰陽師(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 42101023: // 幽玄氣息
            case 42121023: // 幽玄氣息
                eff.statups.put(MapleBuffStat.IgnoreTargetDEF, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.prop));
                break;
            case 42101021: // 花炎結界
                eff.statups.put(MapleBuffStat.FireBarrier, 3);
                break;
            case 42121021: // 花炎結界
                eff.statups.put(MapleBuffStat.FireBarrier, 6);
                break;
            case 42101022: // 花狐的祝福
            case 42121022: // 花狐的祝福
                eff.statups.put(MapleBuffStat.HAKU_BLESS, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndiePDD, eff.info.get(MapleStatInfo.indiePdd));
                break;
            case 42101002: // 影朋‧花狐
                eff.statups.put(MapleBuffStat.ChangeFoxMan, 1);
                break;
            case 42101003: // 扇‧孔雀
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 42111004: // 結界‧櫻
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.y));
                break;
            case 42121005: // 結界‧桔梗
                eff.statups.put(MapleBuffStat.KannaBDR, eff.info.get(MapleStatInfo.bdR));
                break;
            case 42121006: // 曉月勇者
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 42121000: // 破邪連擊符
                eff.statups.put(MapleBuffStat.KeyDownMoving, 120);
                break;
            case 42120003: // 猩猩火酒
                eff.monsterStatus.put(MonsterStatus.M_Ambush, eff.info.get(MapleStatInfo.expR));
                break;
            case 42121004: // 退魔流星符
            case 42121052: // 百鬼夜行
                eff.monsterStatus.put(MonsterStatus.M_Freeze, eff.info.get(MapleStatInfo.time));
                break;
            case 42121054: // 結界‧破魔
                eff.statups.put(MapleBuffStat.BLACKHEART_CURSE, 1);
                break;
            case 42121053: // 公主的加護
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 42101004: // 紫扇仰波‧焰
            case 42111006: // 紫扇仰波‧零
            case 42121008: // Mighty Shikigami Haunting
                eff.statups.put(MapleBuffStat.SHIKIGAMI, -eff.info.get(MapleStatInfo.x));
                break;
            case 42121024: // 紫扇白狐
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            default:
                System.out.println("Kanna skill not coded: " + skill);
                break;
        }
    }
}
