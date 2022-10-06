/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.adventurer;

import client.character.stat.MapleBuffStat;
import client.MapleJob;
import client.MonsterStatus;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Itzik
 */
public class WarriorBuff extends AbstractBuffClass {

    public WarriorBuff() {
        buffs = new int[]{
            // 一轉
            1001003, // 自身強化Iron Body

            // 二轉
            // 狂戰士
            1101004, // 極速武器Weapon Booster
            1101006, // 激勵Rage
            1101013, // 鬥氣集中Combo Order
            // 見習騎士
            1220013, // 祝福護甲
            1200014, // 元素衝擊
            1201004, // 極速武器Weapon Booster
            1201011, // 烈焰之劍Flame Charge
            1201012, // 寒冰之劍Blizzard Charge
            // 槍騎兵
            1301004, // 極速武器Weapon Booster
            1301006, // 禦魔陣Iron Will
            1301007, // 神聖之火Hyper Body
            1301013, // 追隨者Evil Eye

            // 三轉
            // 十字軍
            1111003, // 黑暗之劍
            // 騎士
            1211008, // 雷鳴之劍Lightning Charge
            1211010, // 復原HP Recovery:
            1211011, // 戰鬥命令Combat Orders
            1211013, // 降魔咒Threaten
            1211014, // 超衝擊防禦Parashock Guard
            // 嗜血狂騎
            1311014, // 追隨者衝擊
            1311015, // 十字深鎖鏈Cross Surge
            1310016, // 黑暗守護

            // 四轉
            // 英雄
            1121000, // 楓葉祝福Maple Warrior
            1121010, // 鬥氣爆發Enrage
            // 聖騎士
            1220010, // 屬性強化
            1221000, // 楓葉祝福Maple Warrior
            1221009, // 騎士衝擊波
            1221015, // 自然之力Void Elemental
            1221016, // 守護者精神Guardian
            1221004, // 聖靈之劍Holy Charge
            // 黑騎士
            1321000, // 楓葉祝福Maple Warrior
            1321015, // 暗之獻祭
            1320019, // 轉生(狀態)

            // 超技
            // 英雄
            1121053, // 傳說冒險Epic Adventure
            1121054, // 劍士意念Cry Valhalla
            // 聖騎士
            1221053, // 傳說冒險Epic Adventure
            1221054, // 神域護佑Sacrosanctity
            // 黑騎士
            1321053, // 傳說冒險Epic Adventure
            1321054, // 黑暗飢渴Dark Thrist
        };
    }

    @Override
    public boolean containsJob(int job) {
        return MapleJob.is冒險家(job) && MapleJob.is劍士(job);
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 1001003:// 自身強化Iron Body
                eff.statups.clear();
                eff.statups.put(MapleBuffStat.IndiePDD, eff.info.get(MapleStatInfo.indiePdd));
                break;
            case 1101004: // 極速武器Weapon Booster
            case 1201004: // 極速武器Weapon Booster
            case 1301004: // 極速武器Weapon Booster
                eff.statups.put(MapleBuffStat.Booster, eff.info.get(MapleStatInfo.x));
                break;
            case 1220013: // 祝福護甲
                eff.statups.put(MapleBuffStat.BlessingArmor, eff.info.get(MapleStatInfo.x) + 1);
                break;
            case 1200014: // 元素衝擊
            case 1220010: // 屬性強化
                eff.statups.put(MapleBuffStat.ElementalCharge, 0);
                break;
            case 1101006: // 激勵Rage
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.PowerGuard, eff.info.get(MapleStatInfo.x));
                break;
            case 1101013: // 鬥氣集中Combo Order
                eff.statups.put(MapleBuffStat.ComboCounter, 1);
                break;
            case 1111003: // 黑暗之劍
                eff.monsterStatus.put(MonsterStatus.M_Darkness, eff.info.get(MapleStatInfo.x));
                break;
            case 1121010: // 鬥氣爆發Enrage
                eff.info.put(MapleStatInfo.time, -1);
                eff.statups.put(MapleBuffStat.Enrage,
                        (eff.info.get(MapleStatInfo.x)) * 100 + (eff.info.get(MapleStatInfo.mobCount)));
                eff.statups.put(MapleBuffStat.EnrageCrDamMin, eff.info.get(MapleStatInfo.z));
                eff.statups.put(MapleBuffStat.EnrageCr, eff.info.get(MapleStatInfo.y));
                break;
            case 1121054: // 劍士意念Cry Valhalla
                eff.statups.clear();
                eff.statups.put(MapleBuffStat.TerR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.AsrR, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 1201011:// 烈焰之劍Flame Charge
                eff.monsterStatus.put(MonsterStatus.M_RiseByToss, eff.info.get(MapleStatInfo.dot));
                break;
            case 1201012:// 寒冰之劍Blizzard Charge
                eff.monsterStatus.put(MonsterStatus.M_Freeze, 1);
                eff.monsterStatus.put(MonsterStatus.M_Speed, eff.info.get(MapleStatInfo.x));
                break;
            case 1211008: // 雷鳴之劍Lightning Charge
                eff.monsterStatus.put(MonsterStatus.M_Stun, 1);
                break;
            case 1221004: // 聖靈之劍Holy Charge
                eff.monsterStatus.put(MonsterStatus.M_Seal, eff.info.get(MapleStatInfo.x));
                break;
            case 1211010: // 復原HP Recovery:
                eff.hpR = eff.info.get(MapleStatInfo.x) / 100.0D;
                eff.statups.put(MapleBuffStat.Restoration, eff.info.get(MapleStatInfo.y));
                break;
            case 1211013: // 降魔咒Threaten
                eff.monsterStatus.put(MonsterStatus.M_PAD, eff.info.get(MapleStatInfo.x));
                eff.monsterStatus.put(MonsterStatus.M_PDR, eff.info.get(MapleStatInfo.x));
                eff.monsterStatus.put(MonsterStatus.M_MAD, eff.info.get(MapleStatInfo.x));
                eff.monsterStatus.put(MonsterStatus.M_MDR, eff.info.get(MapleStatInfo.x));
                eff.monsterStatus.put(MonsterStatus.M_EVA, eff.info.get(MapleStatInfo.z));
                break;
            case 1211014: // 超衝擊防禦Parashock Guard
                eff.statups.put(MapleBuffStat.ChargeBuff, eff.info.get(MapleStatInfo.x) / 2);
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                eff.statups.put(MapleBuffStat.IndiePDDR, eff.info.get(MapleStatInfo.z));
                break;
            case 1211011: // 戰鬥命令Combat Orders
                eff.statups.put(MapleBuffStat.CombatOrders, eff.info.get(MapleStatInfo.x));
                break;
            case 1221009: // 騎士衝擊波
                eff.statups.put(MapleBuffStat.ChargeBuff, (int) eff.getLevel());
                break;
            case 1221015: // 自然之力Void Elemental
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                break;
            case 1221016: // 守護者精神Guardian
            case 1221054: // 神域護佑Sacrosanctity
                eff.statups.put(MapleBuffStat.NotDamaged, 1);
                break;
            case 1301006: // 禦魔陣Iron Will
                eff.statups.put(MapleBuffStat.PDD, eff.info.get(MapleStatInfo.pdd));
                break;
            case 1301007: // 神聖之火Hyper Body
                eff.statups.put(MapleBuffStat.MaxMP, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.MaxHP, eff.info.get(MapleStatInfo.y));
                break;
            case 1301013: // 追隨者Evil Eye
                eff.statups.put(MapleBuffStat.Beholder, eff.info.get(MapleStatInfo.x));
                break;
            case 1311014: // 追隨者衝擊
                eff.statups.put(MapleBuffStat.Beholder, 1);
            case 1311015: // 十字深鎖鏈Cross Surge
                eff.statups.put(MapleBuffStat.CrossOverChain, eff.info.get(MapleStatInfo.x));
                break;
            case 1310016: // 黑暗守護
                eff.statups.put(MapleBuffStat.EPAD, eff.info.get(MapleStatInfo.epad));
                eff.statups.put(MapleBuffStat.EPDD, eff.info.get(MapleStatInfo.epdd));
                eff.statups.put(MapleBuffStat.IndieCr, eff.info.get(MapleStatInfo.indieCr));
                eff.statups.put(MapleBuffStat.ACC, eff.info.get(MapleStatInfo.acc));
                eff.statups.put(MapleBuffStat.EVA, eff.info.get(MapleStatInfo.eva));
                break;
            case 1320019: // 轉生(狀態)
                eff.statups.put(MapleBuffStat.Reincarnation, 1);
                eff.statups.put(MapleBuffStat.NotDamaged, 1);
                break;
            case 1321015: // 暗之獻祭
                eff.hpR = eff.info.get(MapleStatInfo.y);
                eff.statups.put(MapleBuffStat.IndieIgnoreMobpdpR, eff.info.get(MapleStatInfo.ignoreMobpdpR));
                eff.statups.put(MapleBuffStat.IndieBDR, eff.info.get(MapleStatInfo.indieBDR));
                break;
            case 1321054: // 黑暗飢渴
                eff.statups.put(MapleBuffStat.ComboDrain, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.IndiePAD, eff.info.get(MapleStatInfo.indiePad));
                break;
            case 1121000: // 楓葉祝福Maple Warrior
            case 1221000: // 楓葉祝福Maple Warrior
            case 1321000: // 楓葉祝福Maple Warrior
                eff.statups.put(MapleBuffStat.BasicStatUp, eff.info.get(MapleStatInfo.x));
                break;
            case 1121053: // 傳說冒險Epic Adventure
            case 1221053: // 傳說冒險Epic Adventure
            case 1321053: // 傳說冒險Epic Adventure
                eff.statups.put(MapleBuffStat.IndieDamR, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.IndieMaxDamageOver, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            default:
                System.out.println("劍士技能未處理,技能代碼: " + skill);
                break;
        }
    }
}
