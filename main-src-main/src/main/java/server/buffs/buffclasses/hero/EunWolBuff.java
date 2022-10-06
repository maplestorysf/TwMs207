package server.buffs.buffclasses.hero;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import client.MonsterStatus;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

public class EunWolBuff extends AbstractBuffClass {

    public EunWolBuff() {
        buffs = new int[]{20050286, // 死裡逃生
            25111209, // 換魂
            25101009, // 小狐仙
            25121108, // 楓葉祝福
            25121209, // 靈魂結界
            25111206, // 束縛術
            25121132, // 英雄誓約
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is隱月(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 20050286: // 死裡逃生
                eff.statups.put(MapleBuffStat.PreReviveOnce, eff.info.get(MapleStatInfo.x));
                break;
            case 25111209: // 換魂
                eff.statups.put(MapleBuffStat.ReviveOnce, eff.info.get(MapleStatInfo.x));
                break;
            case 25101009: // 小狐仙
                eff.info.put(MapleStatInfo.time, -1);
                eff.statups.put(MapleBuffStat.HiddenPossession, 1);
                break;
            case 25121108: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 25121209: // 靈魂結界
                eff.statups.put(MapleBuffStat.SpiritGuard, eff.info.get(MapleStatInfo.x));
                break;
            case 25111206: // 束縛術
                eff.monsterStatus.put(MonsterStatus.M_Freeze, 1);
                eff.info.put(MapleStatInfo.time, eff.getLevel() * 10);
                break;
            case 25121132: // 英雄誓約
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            default:
                System.out.println("未知的 隱月(2500) Buff: " + skill);
                break;
        }
    }
}
