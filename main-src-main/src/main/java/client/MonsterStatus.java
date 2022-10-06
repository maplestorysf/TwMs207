/*
* 一、從封包轉成MonsterStatus的值：(範例都是以新版為主)
*     X = 從封包知道他是第幾個MASK(舊的是倒著算，新的是正著算，因為新的把反向封包去掉了)
*     範例：(舊的0x800，MASK=2；新的MASK=0)
*          MASK[0] [00 08 00 00]
*          MASK[1] [00 04 00 00]
*          MASK[2] [00 02 00 00]
*     Y = 從封包找到你要的值例如：0x800[00 08 00 00]，用小算盤先到16進制輸入800，然後按Rsh再按1，再按等於，然後一直按到變一為止，按等於的次數就是我們要的值
*     (其實也可以轉成十進制去看他是2的幾次方，次方數就是Y)或 Y = (Math.log(0x800) / Math.log(2))
*     MonsterStatus的值 = (31 - Y) + (X * 32)
*
* 二、從MonsterStatus的值轉成封包：(範例都是以新版為主)
*     用小算盤：拿37來當範例，輸入37 Mod 32，就會顯示 5，然後 31 - 5 顯示 26 ，再按1 Lsh 26就是 67108864(0x4000000)，再來37 / 32，就會顯示 1
*     轉成封包就是：
*     MASK[0] [00 00 00 00]
*     MASK[1] [00 00 00 04]
*     MASK[2] [00 00 00 00]
*     
*     也可以用公式： 1 << (31 - (X % 32))
*     
*※有寫完成的就代表已經確認過的
 */
package client;

import client.character.stat.MapleDisease;
import java.io.Serializable;
import handling.BuffStat;

/**
 *
 * @author o黯淡o
 */
public enum MonsterStatus implements Serializable, BuffStat {

    // =========== MASK[0]
    // 物攻
    M_PAD(0),
    // 物防
    M_PDR(1),
    // 魔攻
    M_MAD(2),
    // 魔防
    M_MDR(3),
    // 命中
    M_ACC(4),
    // 迴避
    M_EVA(5),
    // 速度[完成]
    M_Speed(6),
    // 暈眩
    M_Stun(7),
    // 結冰、麻痺
    M_Freeze(8),
    // 中毒[完成]
    M_Poison(9),
    // 封印、沉默
    M_Seal(10),
    // 黑暗
    M_Darkness(11),
    // 物攻提升
    M_PowerUp(12),
    // 魔攻提升
    M_MagicUp(13),
    // 物防提升
    M_PGuardUp(14),
    // 魔防提升
    M_MGuardUp(15),
    // 攻擊免疫
    M_PImmune(16),
    // 魔法免疫
    M_MImmune(17),
    // 影網
    M_Web(18),
    //
    M_HardSkin(19),
    // 忍者伏擊
    M_Ambush(20),
    // 武器涂毒
    M_Venom(21),
    // 致盲
    M_Blind(22),
    // 技能封印
    M_SealSkill(23),
    M_Dazzle(24),
    // 反射物攻
    M_PCounter(25),
    // 反射魔攻
    M_MCounter(26),
    // 痛苦提升
    M_RiseByToss(27),
    M_BodyPressure(28),
    M_Weakness(29),
    // 無法觸碰角色
    M_Showdown(30),
    // 魔法無效
    M_MagicCrash(31),
    // =========== MASK[1]

    M_DamagedElemAttr(32),
    M_Dark(33),
    M_Mystery(34),
    M_AddDamParty(35),
    M_HitCriDamR(36),
    M_Fatality(37),
    M_Lifting(38),
    // 死亡
    M_DeadlyCharge(39),
    M_Smite(40),
    M_AddDamSkill(41),
    M_Incizing(42),
    M_DodgeBodyAttack(43),
    M_DebuffHealing(44),
    M_AddDamSkill2(45),
    M_BodyAttack(46),
    M_TempMoveAbility(47),
    M_FixDamRBuff(48),
    M_ElementDarkness(49),
    // 另一個咬擊
    M_AreaInstallByHit(50),
    M_BMageDebuff(50),
    M_JaguarProvoke(51),
    M_JaguarBleeding(52),
    M_DarkLightning(53),
    M_PinkbeanFlowerPot(54),
    M_BattlePvP_Helena_Mark(55),
    M_PsychicLock(56),
    M_PsychicLockCoolTime(57),
    M_PsychicGroundMark(58),
    M_PowerImmune(59),
    M_PsychicForce(60),
    M_MultiPMDR(61),
    MBS62(62),
    M_ElementResetBySummon(63),
    // =========== MASK[2]
    M_BahamutLightElemAddDam(64),
    MBS65(65),
    MBS66(66),
    DEFUALT_1(66),
    MBS67(67),
    DEFAULT_2(68),
    MBS69(69),
    MBS70(70),
    MBS71(71),
    MBS72(72),
    MBS73(73),
    M_Explosion(74),
    MBS75(75),
    MBS76(76),
    MBS77(77),
    MBS78(78, true),
    MBS79(79),
    MBS80(80),
    MBS81(81),
    // 持續扣血 - 破滅之輪
    M_Burned(82, true),
    M_Invincible(83, true),
    M_ExchangeAttack(84, true),
    M_MultiPMADDR(85, true),
    M_LinkTeam(86, true),
    M_SoulExplosion(87, true),
    M_SeperateSoulP(88, true),
    M_SeperateSoulC(89, true),
    M_Ember(90, true),
    M_TrueSight(91, true),
    MBS92(92, true),
    M_Laser(93, true),
    DEFAULT_12(94, true),
    DEFAULT_13(95, true),
    // =========== MASK[3]
    MBS96(96),;

    static final long serialVersionUID = 0L;
    private final int i;
    private final int position;
    private final boolean end;
    private final int bitNumber;

    private MonsterStatus(int i) {
        this.i = 1 << (31 - (i % 32)); // 如果要變舊的，就把減31去掉，詳細請參考頂端說明
        this.position = (int) Math.floor(i / 32);
        this.end = false;
        this.bitNumber = i;
    }

    private MonsterStatus(int i, boolean end) {
        this.i = 1 << (31 - (i % 32)); // 如果要變舊的，就把減31去掉，詳細請參考頂端說明
        this.position = (int) Math.floor(i / 32);
        this.end = end;
        this.bitNumber = i;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public boolean isEmpty() {
        return end;
    }

    public int getBitNumber() {
        return bitNumber;
    }

    @Override
    public int getValue() {
        return i;
    }

    public static final MapleDisease getLinkedDisease(final MonsterStatus skill) {
        switch (skill) {
            case M_Stun:
            case M_MImmune:
                return MapleDisease.昏迷;
            case M_Poison:
            case M_Dazzle:
                return MapleDisease.中毒;
            case M_Seal:
            case M_HitCriDamR:
                return MapleDisease.封印;
            case M_Freeze:
                return MapleDisease.冰凍;
            case M_Darkness:
                return MapleDisease.黑暗;
            case M_Speed:
                return MapleDisease.緩慢;
        }
        return null;
    }

    @Override
    public String toString() {
        return this.name() + "(" + this.bitNumber + ", " + (this.end ? "true" : ":") + ")";
    }

}
