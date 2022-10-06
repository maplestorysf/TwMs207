package server.buffs.buffclasses.hero;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Maple
 */
public class LuminousBuff extends AbstractBuffClass {

    public LuminousBuff() {
        buffs = new int[]{27001004, // 擴充魔力 - Mana Well
            27101202, // 黑暗之眼 - Pressure Void
            27100003, // 黑暗祝福 - Black Blessing
            27101004, // 極速詠唱 - Booster
            27111004, // 魔力護盾 - Shadow Shell
            27111005, // 光暗之盾 - Dusk Guard
            27111006, // 團隊精神 - Photic Meditation
            27121005, // 黑暗強化 - Dark Crescendo
            27121006, // 黑暗魔心 - Arcane Pitch
            27121009, // 楓葉祝福
            27121053, // 英雄誓言
            20040219, // 平衡
            20040220, // 平衡
            27110007, // 光暗轉換
    };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is夜光(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 27001004: // 擴充魔力 - Mana Well
                eff.statups.put(MapleBuffStat.IndieMMPR, eff.info.get(MapleStatInfo.indieMmpR));
                break;
            case 27101202: // 黑暗之眼 - Pressure Void
                eff.info.put(MapleStatInfo.time, 8000);
                eff.statups.put(MapleBuffStat.KeyDownMoving, 1);
                eff.statups.put(MapleBuffStat.KeyDownAreaMoving, (int) eff.getLevel());
                break;
            case 27100003: // 黑暗祝福 - Black Blessing
                eff.statups.put(MapleBuffStat.BlessOfDarkness, 1);// 球的個數?應該
                break;
            case 27101004: // 極速詠唱 - Booster
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 27111004: // 魔力護盾 - Shadow Shell
                eff.info.put(MapleStatInfo.time, -1);
                eff.statups.put(MapleBuffStat.AntiMagicShell, 3);
                break;
            case 27111005: // 光暗之盾 - Dusk Guard
                eff.statups.put(MapleBuffStat.IndiePDD, eff.info.get(MapleStatInfo.pdd));
                break;
            case 27111006: // 團隊精神 - Photic Meditation
                eff.statups.put(MapleBuffStat.EMAD, eff.info.get(MapleStatInfo.emad));
                break;
            case 27110007: // 光暗轉換
                eff.statups.put(MapleBuffStat.LifeTidal, 1);
                break;
            case 27121005: // 黑暗強化 - Dark Crescendo
                eff.statups.put(MapleBuffStat.StackBuff, eff.info.get(MapleStatInfo.x));
                break;
            case 27121006: // 黑暗魔心 - Arcane Pitch
                eff.statups.put(MapleBuffStat.ElementalReset, eff.info.get(MapleStatInfo.y));
                break;
            case 27121009: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 27121053: // 英雄誓言
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            case 20040219: // 平衡
            case 20040220: // 平衡
                eff.statups.put(MapleBuffStat.Larkness, 2);
                break;
            default:
                System.out.println("夜光未註冊 - Buff: " + skill);
                break;
        }
    }
}
