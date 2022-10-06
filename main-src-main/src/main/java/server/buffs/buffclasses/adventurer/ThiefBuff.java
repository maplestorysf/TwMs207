package server.buffs.buffclasses.adventurer;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import client.MonsterStatus;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

public class ThiefBuff extends AbstractBuffClass {

    public ThiefBuff() {
        buffs = new int[]{
            // 一轉
            4001003, // 隱身術
            4001005, // 速度激發
            // 下忍
            4301003, // 自我速度激發

            // 二轉
            // 刺客
            4101003, // 極速暗殺
            // 俠盜
            // TODO 4200013, // 爆擊
            4201002, // 快速之刀
            4201009, // 輪迴
            4201011, // 楓幣護盾
            // 中忍
            4311005, // 輪迴
            4311009, // 神速雙刀

            // 三轉
            // 暗殺者
            4111001, // 影網術
            4111002, // 影分身
            4111009, // 無形鏢
            // 神偷
            4211003, // 勇者掠奪術
            4211005, // 楓幣護盾
            4211008, // 影分身
            // 隱忍
            4330001, // 進階隱身術
            // TODO 4330009, // 暗影迴避
            4331002, // 替身術
            4331006, // 隱‧鎖鏈地獄

            // 四轉
            // 夜使者
            4121000, // 楓葉祝福
            4121014, // 黑暗能量
            4121017, // 挑釁契約
            // 暗影神偷
            // TODO 4220015, // 致命爆擊
            4221000, // 楓葉祝福
            4221013, // 暗殺本能
            // 影武者
            4341000, // 楓葉祝福
            4341002, // 絕殺刃
            4341007, // 荊棘特效

            // 超技
            // 夜使者
            4121053, // 傳說冒險
            4121054, // 出血毒素
            // 暗影神偷
            4221053, // 傳說冒險
            4221054, // 翻轉硬幣
            // 影武者
            4341052, // 修羅
            4341054, // 隱藏刀
            4341053, // 傳說冒險
        };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is冒險家(job) && MapleJob.is盜賊(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 4001002: // 詛咒術
                break;
            case 4001005: // 速度激發
            case 4301003: // 自我速度激發
                eff.statups.put(MapleBuffStat.Speed, eff.info.get(MapleStatInfo.speed));
                eff.statups.put(MapleBuffStat.Jump, eff.info.get(MapleStatInfo.jump));
                break;
            case 4001003: // 隱身術Dark Sight
                eff.statups.put(MapleBuffStat.DarkSight, eff.info.get(MapleStatInfo.x));
                break;
            case 4101003: // 極速暗殺Claw Booster
            case 4201002: // 快速之刀Dagger Booster
            case 4311009: // 神速雙刀
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 4201011: // 楓幣護盾Meso Guard
            case 4211005: // 楓幣護盾
                eff.statups.put(MapleBuffStat.MesoGuard, eff.info.get(MapleStatInfo.x));
                break;
            case 4201009: // 輪迴
            case 4311005: // 輪迴
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 4211003: // 勇者掠奪術Pick Pocket
                eff.statups.put(MapleBuffStat.PickPocket, eff.info.get(MapleStatInfo.x));
                break;
            case 4111002: // 影分身Shadow Partner
            case 4211008: // 影分身Shadow Partner
            case 4331002: // 替身術
                eff.statups.put(MapleBuffStat.ShadowPartner, eff.info.get(MapleStatInfo.x));
                break;
            case 4111009: // 無形鏢Shadow Star
                eff.statups.put(MapleBuffStat.NoBulletConsume, 0);
                break;
            case 4121014: // 黑暗能量Dark Harmony
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));// test - works without
                break;
            case 4121017: // 挑釁契約
                eff.monsterStatus.put(MonsterStatus.M_Showdown, eff.info.get(MapleStatInfo.x));
                break;
            case 4330001: // 進階隱身術
                eff.statups.put(MapleBuffStat.DarkSight, (int) eff.getLevel());
                break;
            case 4331006: // 隱‧鎖鏈地獄
                eff.statups.put(MapleBuffStat.NotDamaged, 1);
                eff.statups.put(MapleBuffStat.IgnoreAllImmune, 1);
                break;
            case 4341002:
                eff.info.put(MapleStatInfo.time, 60 * 1000);
                eff.hpR = -eff.info.get(MapleStatInfo.x) / 100.0;
                eff.statups.put(MapleBuffStat.FinalCut, eff.info.get(MapleStatInfo.y));
                break;
            case 4341007: // 荊棘特效
                eff.statups.put(MapleBuffStat.Stance, eff.info.get(MapleStatInfo.prop));
                eff.statups.put(MapleBuffStat.PAD, eff.info.get(MapleStatInfo.epad));
                break;
            case 4221013: // 暗殺本能Shadow Instinct
                break;
            case 4121000: // 楓葉祝福Maple Warrior
            case 4221000: // 楓葉祝福Maple Warrior
            case 4341000: // 楓葉祝福
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 4121054: // 出血毒素
                eff.statups.put(MapleBuffStat.BleedingToxin, eff.info.get(MapleStatInfo.x));
                break;
            case 4221054: // 翻轉硬幣 TODO
                // eff.statups.put(MapleBuffStat.FlipTheCoin, eff.info.get(MapleStatInfo.x));
                break;
            case 4341052: // 修羅
                eff.statups.put(MapleBuffStat.Asura, eff.info.get(MapleStatInfo.x));
                break;
            case 4341054: // 隱藏刀
                eff.statups.put(MapleBuffStat.WindBreakerFinal, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
            case 4121053: // 傳說冒險Epic Adventure
            case 4221053: // 傳說冒險Epic Adventure
            case 4341053: // 傳說冒險
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            default:
                System.out.println("盜賊技能未處理,技能代碼: " + skill);
                break;
        }
    }
}
