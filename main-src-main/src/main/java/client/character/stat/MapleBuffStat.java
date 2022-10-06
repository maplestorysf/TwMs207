package client.character.stat;

import java.io.Serializable;
import handling.BuffStat;

public enum MapleBuffStat implements Serializable, BuffStat {
    NONE_START(-1),
    // ==========================Mask[0] - 1 - IDA[0xE]

    // 物理攻擊力提升
    IndiePAD(0, true),
    // 魔法攻擊力提升
    IndieMAD(1, true),
    // 物理防御力提升
    IndiePDD(2, true),
    // 魔法防御力提升
    // IndieMDD(-1, true),
    // 最大體力提升
    IndieMHP(3, true), // indieMaxHp, indieMhp
    // 最大體力百分比提升
    IndieMHPR(4, true), // indieMhpR
    // 最大魔法提升
    IndieMMP(5, true), // indieMaxMp
    // 最大魔法百分比提升
    IndieMMPR(6, true), // indieMmpR
    // 命中值提升
    IndieACC(7, true), // indieAcc
    // 迴避值提升
    IndieEVA(8, true),
    // 跳躍力提升
    IndieJump(9, true),
    // 移動速度提升
    IndieSpeed(10, true),
    // 全屬性提升
    IndieAllStat(11, true), // indieAllStat
    //
    IndieDodgeCriticalTime(12, true),
    // 經驗值獲得量提升
    IndieEXP(14, true),
    // 攻擊速度提升
    IndieBooster(15, true), // indieBooster
    //
    IndieFixedDamageR(15, true),
    //
    PyramidStunBuff(16, true),
    //
    PyramidFrozenBuff(17, true),
    //
    PyramidFireBuff(18, true),
    //
    PyramidBonusDamageBuff(19, true),
    //
    IndieRelaxEXP(20, true),
    // 力量提升
    IndieSTR(21, true),
    // 敏捷提升
    IndieDEX(22, true),
    // 智力提升
    IndieINT(23, true),
    // 幸運提升
    IndieLUK(24, true),
    // 傷害百分比提升
    IndieDamR(25, true), // indieDamR
    //
    IndieScriptBuff(26, true),
    //
    IndieMDF(27, true),
    // ==========================Mask[1] - 2 - IDA[0xD]

    // 傷害最大值提升
    IndieMaxDamageOver(28, true),
    // 異常抗性提升
    IndieAsrR(29, true), // indieAsrR
    // 屬性抗性提升
    IndieTerR(30, true), // indieTerR
    // 爆擊率提升
    IndieCr(31, true),
    // 物理防禦率提升
    IndiePDDR(32, true),
    // 最大爆擊提升
    IndieCrMax(33, true),
    // BOSS攻擊力提升
    IndieBDR(34, true),
    // 全屬性百分比提升
    IndieStatR(35, true),
    // 格擋提升
    IndieStance(36, true),
    // 無視怪物防禦率提升
    IndieIgnoreMobpdpR(37, true), // indieIgnoreMobpdpR
    //
    IndieEmpty(38, true),
    // 物理攻擊力百分比提升
    IndiePADR(39, true),
    // 魔法攻擊力百分比提升
    IndieMADR(40, true),
    // 最大爆擊傷害提升
    IndieCrMaxR(41, true),
    // 迴避值百分比提升
    IndieEVAR(42, true),
    // 魔法防禦率提升
    IndieMDDR(43, true),
    //
    IndieDrainHP(44, true),
    //
    IndiePMdR(45, true),
    // 傷害最大值百分比提升
    IndieMaxDamageOverR(46, true),
    //
    IndieForceJump(47, true),
    //
    IndieForceSpeed(48, true),
    //
    IndieQrPointTerm(49, true),
    //
    IDA_BUFF_50(50, true),
    //
    IDA_BUFF_51(51, true),
    //
    IDA_BUFF_52(52, true),
    //
    IDA_BUFF_53(53, true),
    //
    IDA_BUFF_54(54, true),
    //
    IDA_BUFF_55(55, true),
    //
    IDA_BUFF_56(56, true),
    //
    IDA_BUFF_57(57, true),
    //
    IDA_BUFF_58(58, true),
    //
    IDA_BUFF_59(59, true),
    //
    IndieStatCount(60, true),
    // 物理攻擊力
    PAD(61),
    // 物理防御力
    PDD(62),
    // 魔法攻擊力
    MAD(63),
    // 魔法防御力
    // MDD(-1),
    // 命中率
    ACC(64),
    // 迴避率
    EVA(65),
    // 手技
    Craft(66),
    // 移動速度
    Speed(67),
    // 跳躍力
    Jump(68),
    // 魔心防禦
    MagicGuard(69),
    // 隱藏術
    DarkSight(70),
    // 攻擊加速
    Booster(71),
    // 傷害反擊
    PowerGuard(72),
    // 神聖之火_最大MP
    MaxHP(73),
    // ==========================Mask[2] - 3 - IDA[0xC]

    // 神聖之火_最大HP
    MaxMP(74),
    // 神聖之光
    Invincible(75),
    // 無形之箭
    SoulArrow(76),
    // 昏迷
    Stun(77),
    // 中毒
    Poison(78),
    // 封印
    Seal(79),
    // 黑暗
    Darkness(80),
    // 鬥氣集中
    ComboCounter(81),
    // 更新BUFF用
    IDA_BUFF_82(82),
    //
    IDA_BUFF_83(83),
    //
    IDA_BUFF_84(84),
    // 屬性攻擊
    WeaponCharge(85),
    // 神聖祈禱
    HolySymbol(86),
    // 楓幣獲得量
    MesoUp(87),
    // 影分身
    ShadowPartner(88),
    // 勇者掠奪術
    PickPocket(89),
    // 楓幣護盾
    MesoGuard(90),
    //
    Thaw(91),
    // 虛弱
    Weakness(92),
    // 詛咒
    Curse(93),
    // 緩慢
    Slow(94),
    // 變身
    Morph(95),
    // 恢復
    Regen(96),
    // 楓葉祝福
    BasicStatUp(97),
    // 格擋
    Stance(98),
    // 銳利之眼
    SharpEyes(99),
    // 魔法反擊
    ManaReflection(100),
    // 誘惑
    Attract(101),
    // 不消耗發射道具
    NoBulletConsume(102),
    // 魔力無限
    Infinity(103),
    // 進階祝福
    AdvancedBless(104),
    //
    IllusionStep(105),
    // 致盲
    Blind(106),
    // 集中精力
    Concentration(107),
    // 不死化
    BanMap(108),
    // 英雄的回響
    MaxLevelBuff(109),
    // ==========================Mask[3] - 4 - IDA[0xB]

    // 楓幣獲得量(道具)
    MesoUpByItem(110),
    //
    IDA_BUFF_111(111),
    //
    IDA_BUFF_112(112),
    // 鬼魂變身
    Ghost(113),
    //
    Barrier(114),
    // 混亂
    ReverseInput(115),
    // 掉寶幾率(道具)
    ItemUpByItem(116),
    // 物理免疫
    RespectPImmune(117),
    // 魔法免疫
    RespectMImmune(118),
    // 解多人Buff用的
    DefenseAtt(119),
    // 解多人Buff用的
    DefenseState(120),
    // 最終傷害(道場技能:地火天爆/道具:法老的祝福)
    DojangBerserk(121),
    // 金剛不壞_無敵
    DojangInvincible(122),
    // 金剛不壞_盾牌效果
    DojangShield(123),
    // 聖魂劍士終極攻擊
    SoulMasterFinal(124),
    // 破風使者終極攻擊
    WindBreakerFinal(125),
    // 自然力重置
    ElementalReset(126),
    // 風影漫步
    HideAttack(127),
    // 組合無限
    EventRate(128),
    // 矛之鬥氣
    ComboAbilityBuff(129),
    // 嗜血連擊 [202]
    ComboDrain(130),
    // 宙斯之盾
    ComboBarrier(131),
    // 強化連擊(CMS_戰神抗壓)
    BodyPressure(132),
    // 釘錘(CMS_戰神威勢)
    RepeatEffect(133),
    // (CMS_天使狀態)
    ExpBuffRate(133),
    // 無法使用藥水
    StopPortion(134),
    // 影子
    StopMotion(135),
    // 恐懼
    Fear(136),
    //
    IDA_BUFF_137(137),
    // 緩慢術
    HiddenPieceOn(138),
    // 守護之力(CMS_魔法屏障)
    MagicShield(139),
    // 魔法抵抗．改
    MagicResistance(140),
    // 靈魂之石
    SoulStone(141),
    // ==========================Mask[4] - 5 - IDA[0xA]

    // 飛行
    Flying(142),
    // 冰凍
    Frozen(143),
    // 雷鳴之劍
    AssistCharge(144),
    // 鬥氣爆發
    Enrage(145),
    // 障礙
    DrawBack(146),
    // 無敵(隱‧鎖鏈地獄、自由精神等)
    NotDamaged(147),
    // 絕殺刃
    FinalCut(148),
    // 咆哮
    HowlingAttackDamage(149),
    // 狂獸附體
    BeastFormDamageUp(150),
    // 地雷
    Dance(151),
    // 增強_MaxHP
    EMHP(152),
    // 增強_MaxMP
    EMMP(153),
    // 增強_物理攻擊力
    EPAD(154),
    // 增強_魔法攻擊力
    EMAD(155),
    // 增強_物理防禦力
    EPDD(156),
    // 增強_魔法防禦力
    // EMDD(-1),
    // 全備型盔甲
    Guard(157),
    //
    IDA_BUFF_158(158),
    //
    IDA_BUFF_159(159),
    // 終極賽特拉特_PROC
    Cyclone(160),
    //
    IDA_BUFF_161(161),
    // 咆哮_會心一擊機率增加
    HowlingCritical(162),
    // 咆哮_MaxMP 增加
    HowlingMaxMP(163),
    // 咆哮_傷害減少
    HowlingDefence(164),
    // 咆哮_迴避機率
    HowlingEvasion(165),
    //
    Conversion(166),
    // 甦醒
    Revive(167),
    // 迷你啾出動
    PinkbeanMinibeenMove(168),
    // 潛入
    Sneak(167),
    // 合金盔甲
    Mechanic(168),
    // ==========================Mask[5] - 6 - IDA[0x9]

    // 暴走形态
    BeastFormMaxHP(169),
    // 幸运骰子
    Dice(170),
    // 祝福护甲
    BlessingArmor(171),
    // 攻擊增加百分比
    DamR(172),
    // 瞬間移動精通
    TeleportMasteryOn(173),
    // 戰鬥命令
    CombatOrders(174),
    // 追隨者
    Beholder(175),
    // 裝備潛能無效化
    DispelItemOption(176),
    //
    Inflation(177),
    // 龍神的庇護
    OnixDivineProtection(178),
    // 未知
    Web(179),
    // 天使祝福
    Bless(180),
    // 解多人Buff用的
    TimeBomb(181),
    // 解多人Buff用的
    DisOrder(182),
    // 解多人Buff用的
    Thread(183),
    //
    Team(184),
    // 解多人Buff用的
    Explosion(185),
    //
    BuffLimit(186),
    // 力量
    STR(187),
    // 智力
    INT(188),
    // 敏捷
    DEX(189),
    // 幸運
    LUK(190),
    // 未知(未確定)
    DispelItemOptionByField(191),
    // 龍捲風(異常狀態)
    DarkTornado(192),
    // 未知(未確定)
    PVPDamage(193),
    // 未知
    PvPScoreBonus(194),
    // 更新BUFF用
    PvPInvincible(195),
    // 解多人Buff用的
    PvPRaceEffect(196),
    // 解多人Buff用的
    WeaknessMdamage(197),
    // 凍結
    Frozen2(198),
    // 解多人Buff用的
    PVPDamageSkill(199),
    // 未知(未確定)(90002000)
    AmplifyDamage(200),
    // 冰騎士
    // IceKnight(-1),
    // 更新BUFF用
    Shock(201),
    // 無限力量
    InfinityForce(202),
    // 更新BUFF用
    IncMaxHP(203),
    // ==========================Mask[6] - 7 - IDA[0x8]

    // 未知(未確定)
    IncMaxMP(204),
    // 聖十字魔法盾
    HolyMagicShell(205),
    // 無需蓄力[核爆術]
    KeyDownTimeIgnore(206),
    // 神秘狙擊
    ArcaneAim(207),
    // 大魔法師
    MasterMagicOn(208),
    // 異常抗性
    AsrR(209),
    // 屬性抗性
    TerR(210),
    // 水之盾
    DamAbsorbShield(211),
    // 變形
    DevilishPower(212),
    // 随机橡木桶
    Roulette(213),
    // 靈魂灌注_攻擊增加
    SpiritLink(214),
    // 神圣拯救者的祝福
    AsrRByItem(215),
    // 光明綠化
    Event(216),
    // 靈魂灌注_爆擊率增加
    CriticalBuff(217),
    // 更新BUFF用
    DropRate(218),
    // 更新BUFF用
    PlusExpRate(219),
    // 更新BUFF用
    ItemInvincible(220),
    // 更新BUFF用
    Awake(221),
    // 更新BUFF用
    ItemCritical(222),
    // 更新BUFF用
    ItemEvade(223),
    // 未知(未確定)
    Event2(224),
    // 吸血鬼之觸
    VampiricTouch(225),
    // 提高防禦力[猜測]
    DDR(226),
    // 更新BUFF用
    IncCriticalDamMin(-1),
    // 更新BUFF用
    IncCriticalDamMax(-1),
    // 更新BUFF用
    IncTerR(227),
    // 更新BUFF用
    IncAsrR(228),
    // 更新BUFF用
    DeathMark(229),
    // 更新BUFF用
    UsefulAdvancedBless(230),
    // 更新BUFF用
    Lapidification(231),
    // 更新BUFF用
    VenomSnake(232),
    // 更新BUFF用
    CarnivalAttack(233),
    // ==========================Mask[7] - 8 - IDA[0x7]

    // Carnival防禦
    CarnivalDefence(234),
    // 更新BUFF用
    CarnivalExp(235),
    // 更新BUFF用
    SlowAttack(236),
    // 角設預設Buff
    PyramidEffect(237),
    // 角設預設Buff
    KillingPoint(238),
    // 更新BUFF用
    HollowPointBullet(239),
    // 按壓型技能進行
    KeyDownMoving(240),
    // 無視防禦力
    IgnoreTargetDEF(241),
    // 復活一次
    ReviveOnce(242),
    // 幻影斗蓬
    Invisible(243),
    // 爆擊機率增加
    EnrageCr(244),
    // 最小爆擊傷害
    EnrageCrDamMin(245),
    // 審判
    Judgement(246),
    // 增加_物理攻擊
    DojangLuckyBonus(247),
    // 更新BUFF用
    PainMark(248),
    // IDA移動Buff
    Magnet(249),
    // IDA移動Buff
    MagnetArea(250),
    // 更新BUFF用
    IDA_BUFF_251(251),
    // 更新BUFF用
    IDA_BUFF_252(252),
    // 更新BUFF用
    IDA_BUFF_253(253),
    // 更新BUFF用
    IDA_BUFF_254(254),
    // 更新BUFF用
    IDA_BUFF_255(255),
    // IDA移動Buff
    VampDeath(256),
    // 解多人Buff用的
    BlessingArmorIncPAD(257),
    // 黑暗之眼
    KeyDownAreaMoving(258),
    // 光蝕 & 暗蝕
    Larkness(259),
    // 黑暗強化 & 全面防禦
    StackBuff(260),
    // 黑暗祝福
    BlessOfDarkness(261),
    // 抵禦致命異常狀態
    // (如 元素適應(火、毒), 元素適應(雷、冰), 聖靈守護)
    AntiMagicShell(262),
    // 血之限界
    LifeTidal(263),
    // 更新BUFF用
    HitCriDamR(264),
    // 凱撒變型值
    SmashStack(265),
    // ==========================Mask[8] - 9 - IDA[0x6]

    // 堅韌護甲
    PartyBarrier(266),
    // 凱撒模式切換
    ReshuffleSwitch(267),
    // 更新BUFF用
    SpecialAction(268),
    // IDA移動Buff
    VampDeathSummon(269),
    // 意志之劍
    StopForceAtomInfo(270),
    // 會心一擊傷害
    SoulGazeCriDamR(271),
    // 更新BUFF用
    SoulRageCount(272),
    // 靈魂傳動
    PowerTransferGauge(273),
    // 天使親和力
    AffinitySlug(274),
    // 三位一體
    Trinity(275),
    // IDA特殊Buff
    IncMaxDamage(276),
    // 更新BUFF用
    BossShield(277),
    // 功能不知道
    MobZoneState(278),
    // IDA移動Buff
    GiveMeHeal(279),
    // 更新BUFF用
    TouchMe(280),
    // 更新BUFF用
    Contagion(281),
    // 更新BUFF用
    ComboUnlimited(282),
    // 繼承人
    SoulExalt(283),
    // 未知(未確定)
    IgnorePCounter(284),
    // 靈魂深造
    IgnoreAllCounter(285),
    // 更新BUFF用
    IgnorePImmune(286),
    // 隱‧鎖鏈地獄
    IgnoreAllImmune(287),
    // 終極審判[猜測]
    FinalJudgement(288),
    // 更新BUFF用
    IDA_BUFF_289(289),
    // 冰雪結界
    IceAura(290),
    // 火靈結界[猜測]
    FireAura(291),
    // 復仇天使
    VengeanceOfAngel(292),
    // 天堂之門
    HeavensDoor(293),
    // 更新BUFF用
    Preparation(294),
    // 減少格擋率
    BullsEye(295),
    // 更新BUFF用
    IncEffectHPPotion(296),
    // 更新BUFF用
    IncEffectMPPotion(297),
    // 出血毒素
    BleedingToxin(298),
    // 更新BUFF用
    IgnoreMobDamR(299),
    // 修羅
    Asura(300),
    //
    IDA_BUFF_301(301),
    // 翻轉硬幣
    FlipTheCoin(302),
    // 統合能量
    UnityOfPower(303),
    // 暴能續發
    Stimulate(304),
    // IDA特殊Buff
    ReturnTeleport(305),
    // ==========================Mask[9] - 10 - IDA[0x5]

    // 功能不知道
    DropRIncrease(306),
    // 功能不知道
    IgnoreMobpdpR(307),
    // BOSS傷害
    BdR(308),
    // 更新BUFF用
    CapDebuff(309),
    // 超越
    Exceed(310),
    // 急速療癒
    DiabolikRecovery(311),
    // 更新BUFF用
    FinalAttackProp(312),
    // 超越_負載
    ExceedOverload(313),
    // 超越_負載量
    OverloadCount(314),
    // 壓制砲擊/沉月-攻擊數量
    BuckShot(315),
    // 更新BUFF用
    FireBomb(316),
    // 更新BUFF用
    HalfstatByDebuff(317),
    // 傑諾能量
    SurplusSupply(318),
    // IDA特殊Buff
    SetBaseDamage(319),
    // IDA BUFF列表更新用
    EVAR(320),
    // 傑諾飛行
    NewFlying(321),
    // 阿瑪蘭斯發電機
    AmaranthGenerator(322),
    // 解多人Buff用的
    OnCapsule(323),
    // 元素： 風暴
    CygnusElementSkill(324),
    // 開天闢地[猜測]
    StrikerHyperElectric(325),
    // 更新BUFF用
    EventPointAbsorb(326),
    // 更新BUFF用
    EventAssemble(327),
    // 風暴使者
    StormBringer(328),
    // 光之劍-命中提升
    ACCR(329),
    // 迴避提升
    DEXR(330),
    // 阿爾法
    Albatross(331),
    //
    Translucence(332),
    // 雙重力量 : 沉月/旭日
    PoseType(333),
    // 光之劍
    LightOfSpirit(334),
    // 元素： 靈魂
    ElementSoul(335),
    // 雙重力量 : 沉月/旭日
    GlimmeringTime(336),
    // 更新BUFF用
    TrueSight(337),
    // 更新BUFF用
    SoulExplosion(338),
    // ==========================Mask[10] - 11 - IDA[0x4]

    // 靈魂球BUFF
    SoulMP(339),
    // 靈魂BUFF
    FullSoulMP(340),
    //
    SoulSkillDamageUp(341),
    // 元素衝擊
    ElementalCharge(342),
    // 復原
    Restoration(343),
    // 十字深鎖鏈
    CrossOverChain(344),
    // 騎士衝擊波
    ChargeBuff(345),
    // 重生
    Reincarnation(346),
    // 超衝擊防禦光環
    KnightsAura(347),
    // 寒冰迅移
    ChillingStep(348),
    //
    DotBasedBuff(349),
    // 祝福福音
    BlessEnsenble(350),
    // 更新BUFF用
    ComboCostInc(351),
    // 功能不知道
    ExtremeArchery(352),
    // IDA特殊Buff
    NaviFlying(353),
    // 魔幻箭筒 進階顫抖
    QuiverCatridge(354),
    //
    AdvancedQuiver(355),
    // IDA移動Buff
    UserControlMob(356),
    // IDA特殊Buff
    ImmuneBarrier(357),
    // 壓制
    ArmorPiercing(358),
    // 時之威能 [202]
    ZeroAuraStr(359),
    // 聖靈神速 [202]
    ZeroAuraSpd(360),
    // 更新BUFF用
    CriticalGrowing(361),
    // 更新BUFF用
    QuickDraw(362),
    // 更新BUFF用
    BowMasterConcentration(363),
    // 更新BUFF用
    TimeFastABuff(364),
    // 更新BUFF用
    TimeFastBBuff(365),
    // 更新BUFF用
    GatherDropR(366),
    // 大砲(95001002)
    AimBox2D(367),
    // 更新BUFF用
    IDA_BUFF_368(368),
    // 更新BUFF用
    IncMonsterBattleCaptureRate(369),
    // 更新BUFF用
    CursorSniping(370),
    // 更新BUFF用
    DebuffTolerance(371),
    // ==========================Mask[11] - 12 - IDA[0x3]

    // 無視怪物傷害(重生的輪行蹤) [202確定]
    DotHealHPPerSecond(372),
    // 更新BUFF用
    DotHealMPPerSecond(373),
    // 靈魂結界
    SpiritGuard(374),
    // 死裡逃生
    PreReviveOnce(375),
    // IDA特殊Buff [202]
    SetBaseDamageByBuff(37),
    // 更新BUFF用
    LimitMP(377),
    // 更新BUFF用
    ReflectDamR(378),
    // 更新BUFF用 [202]
    ComboTempest(379),
    // 更新BUFF用
    MHPCutR(380),
    // 更新BUFF用
    MMPCutR(381),
    // IDA移動Buff
    SelfWeakness(382),
    // 元素 : 闇黑
    ElementDarkness(383),
    // 本鳳凰 [202]
    FlareTrick(384),
    // 燃燒 [202]
    Ember(385),
    // 更新BUFF用
    Dominion(386),
    // 更新BUFF用
    SiphonVitality(387),
    // 更新BUFF用
    DarknessAscension(388),
    // 更新BUFF用
    BossWaitingLinesBuff(389),
    // 更新BUFF用 [202]
    DamageReduce(390),
    // 暗影僕從
    ShadowServant(391),
    // 更新BUFF用
    ShadowIllusion(392),
    // 功能不知道
    KnockBack(393),
    // 更新BUFF用
    AddAttackCount(394),
    // 更新BUFF用
    ComplusionSlant(395),
    // 召喚美洲豹
    JaguarSummoned(396),
    // 自由精神
    JaguarCount(397),
    // 功能不知道
    SSFShootingAttack(398),
    // 更新BUFF用
    DevilCry(399),
    // 功能不知道
    ShieldAttack(400),
    // 光環效果 [202]
    BMageAura(401),
    // 黑暗閃電
    DarkLighting(402),
    // 戰鬥精通
    AttackCountX(403),
    // 死神契約 [202]
    BMageDeath(404),
    // 更新BUFF用
    BombTime(405),
    // 更新BUFF用
    NoDebuff(406),
    // 神盾系統
    XenonAegisSystem(405),
    // 索魂精通 [確定]
    AngelicBursterSoulSeeker(406),
    // 更新BUFF用
    BattlePvP_Mike_Shield(407),
    // 更新BUFF用
    BattlePvP_Mike_Bugle(408),
    // 小狐仙
    HiddenPossession(409),
    // 暗影蝙蝠
    NightWalkerBat(410),
    // 更新BUFF用
    NightLordMark(411),
    // 燎原之火
    WizardIgnite(412),
    //
    IDA_BUFF_413(413),
    //
    IDA_BUFF_414(414),
    // 花炎結界
    FireBarrier(415),
    // 影朋‧花狐
    ChangeFoxMan(416),
    //
    IDA_BUFF_417(417),
    //
    IDA_BUFF_418(418),
    //
    IDA_BUFF_419(419),
    // 危機繩索
    AURA_BOOST(420),
    // 拔刀術
    HayatoStance(421),
    // 拔刀術-新技體
    HayatoStanceBonus(422),
    // 制敵之先
    COUNTERATTACK(423),
    // 柳身
    WILLOW_DODGE(424),
    // 紫扇仰波
    SHIKIGAMI(425),
    // 武神招來_物理攻擊力 [202]
    HayatoPAD(425),
    // 武神招來_最大HP% [202]
    HayatoHPR(426),
    // 武神招來_最大MP% [202]
    HayatoMPR(427),
    // 拔刀術
    BATTOUJUTSU_STANCE(428),
    // ==========================Mask[12] - 13 - IDA[0x2]

    // 更新BUFF用
    IDA_BUFF_429(429),
    // 更新BUFF用
    IDA_BUFF_430(430),
    // 迅速
    JINSOKU(431),
    // 一閃
    HayatoCr(432),
    // 花狐的祝福
    HAKU_BLESS(433),
    // 結界‧桔梗 BOSS傷
    KannaBDR(434),
    // 解多人Buff用的
    PLAYERS_BUFF435(435),
    // 更新BUFF用
    IDA_BUFF_436(436),
    // 結界‧破魔
    BLACKHEART_CURSE(437),
    // 更新BUFF用
    IDA_BUFF_438(438),
    // 更新BUFF用
    IDA_BUFF_439(439),
    // 精靈召喚模式
    AnimalChange(440),
    // 更新BUFF用
    IDA_BUFF_441(441),
    // 更新BUFF用
    IDA_BUFF_442(442),
    // 更新BUFF用
    IDA_BUFF_443(443),
    // 更新BUFF用
    IDA_BUFF_444(444),
    // IDA特殊Buff
    IDA_SPECIAL_BUFF_4(445),
    // 更新BUFF用
    IDA_BUFF_446(446),
    // 更新BUFF用
    IDA_BUFF_447(447),
    // ==========================Mask[13] - 14 - IDA[0x0]

    // 能量獲得 [202]
    BattlePvP_Helena_Mark(448),
    // 更新BUFF用
    BattlePvP_Helena_WindSpirit(449),
    // 預設Buff-3
    BattlePvP_LangE_Protection(450),
    // 更新BUFF用
    BattlePvP_LeeMalNyun_ScaleUp(451),
    // 更新BUFF用
    BattlePvP_Revive(452),
    // 皮卡啾攻擊
    PinkbeanAttackBuff(453),
    // 皮卡啾未知
    PinkbeanRelax(454),
    // 預設Buff-4
    PinkbeanRollingGrade(455),
    // 烈焰溜溜球
    PinkbeanYoYoStack(456),
    // 曉月流基本技_提升攻擊力#damR%
    RandAreaAttack(457),
    // 更新BUFF用
    NextAttackEnhance(458),
    // 更新BUFF用
    AranBeyonderDamAbsorb(459),
    // 更新BUFF用
    AranCombotempastOption(460),
    // 更新BUFF用
    NautilusFinalAttack(461),
    // 更新BUFF用
    ViperTimeLeap(462),
    // 更新BUFF用
    RoyalGuardState(463),
    // 更新BUFF用
    RoyalGuardPrepare(464),
    // 更新BUFF用 [202]
    MichaelSoulLink(465),
    // 更新BUFF用
    MichaelStanceLink(466),
    // 更新BUFF用
    TriflingWhimOnOff(467),
    // 更新BUFF用
    AddRangeOnOff(468),
    // ESP數量
    KinesisPsychicPoint(469),
    // 更新BUFF用
    KinesisPsychicOver(470),
    // 更新BUFF用
    KinesisPsychicShield(471),
    // 更新BUFF用
    KinesisIncMastery(472),
    // 更新BUFF用
    KinesisPsychicEnergeShield(473),
    // 更新BUFF用
    BladeStance(474),
    // 更新BUFF用
    DebuffActiveSkillHPCon(475),
    // 更新BUFF用
    DebuffIncHP(476),
    // 更新BUFF用
    BowMasterMortalBlow(477),
    // 更新BUFF用
    AngelicBursterSoulResonance(478),
    // 更新BUFF用
    Fever(479),
    // //依古尼斯咆哮 [確定]
    IgnisRore(480),
    // 更新BUFF用
    RpSiksin(481),
    // 更新BUFF用
    TeleportMasteryRange(-1),
    // 更新BUFF用
    FixCoolTime(482),
    // 更新BUFF用
    IncMobRateDummy(483),
    // 更新BUFF用
    AdrenalinBoost(484),
    // 更新BUFF用
    AranSmashSwing(485),
    // 吸血術
    AranDrain(486),
    // 更新BUFF用
    AranBoostEndHunt(487),
    // 更新BUFF用
    HiddenHyperLinkMaximization(488),
    // 更新BUFF用
    RWCylinder(489),
    // 更新BUFF用
    RWCombination(490),
    // 更新BUFF用
    RWMagnumBlow(491),
    // 更新BUFF用
    RWBarrier(492),
    // 更新BUFF用
    RWBarrierHeal(493),
    // 更新BUFF用
    RWMaximizeCannon(494),
    // 更新BUFF用
    RWOverHeat(495),
    // 更新BUFF用
    UsingScouter(496),
    // 更新BUFF用
    RWMovingEvar(497),
    // 更新BUFF用
    IDA_BUFF_498(498),
    // 更新BUFF用 [202]
    Stigma(499),
    // 更新BUFF用
    IDA_BUFF_500(500),
    // 更新BUFF用
    IDA_BUFF_501(501),
    // 更新BUFF用
    IDA_BUFF_502(502),
    // 更新BUFF用
    IDA_BUFF_503(503),
    // 更新BUFF用
    IDA_BUFF_504(504),
    // 更新BUFF用
    IDA_BUFF_505(505),
    // 更新BUFF用
    元氣覺醒(506),
    // 更新BUFF用
    能量爆炸(507),
    // 更新BUFF用
    IDA_BUFF_508(508),
    // 更新BUFF用
    IDA_BUFF_509(509),
    // 更新BUFF用
    IDA_BUFF_510(510),
    // 更新BUFF用
    交換攻擊(511),
    // 更新BUFF用
    聖靈祈禱(512),
    // 更新BUFF用
    IDA_BUFF_513(513),
    // 更新BUFF用
    IDA_BUFF_514(514),
    // 更新BUFF用
    IDA_BUFF_515(515),
    // 更新BUFF用
    IDA_BUFF_516(516),
    // 更新BUFF用
    散式投擲(517),
    // 更新BUFF用
    IDA_BUFF_518(518),
    // 更新BUFF用
    IDA_BUFF_519(519),
    // 更新BUFF用
    滅殺刃影(520),
    // 更新BUFF用
    狂風呼嘯(521),
    // 更新BUFF用
    IDA_BUFF_522(522),
    // 更新BUFF用
    惡魔狂亂(523),
    // 更新BUFF用
    IDA_BUFF_524(524),
    // 更新BUFF用
    IDA_BUFF_525(525),
    // 更新BUFF用
    IDA_BUFF_526(526),
    // 更新BUFF用
    IDA_BUFF_527(527),
    // 更新BUFF用
    IDA_BUFF_528(528),
    // 更新BUFF用
    IDA_BUFF_529(529),
    // 更新BUFF用
    IDA_BUFF_530(530),
    // 更新BUFF用
    IDA_BUFF_531(531),
    // 更新BUFF用
    IDA_BUFF_532(532),
    // 更新BUFF用
    IDA_BUFF_533(533),
    // 更新BUFF用
    IDA_BUFF_534(534),
    // 更新BUFF用
    IDA_BUFF_535(535),
    // 更新BUFF用
    IDA_BUFF_536(536),
    // 更新BUFF用
    IDA_BUFF_537(537),
    // 更新BUFF用
    IDA_BUFF_538(538),
    // 更新BUFF用
    IDA_BUFF_539(539),
    // 更新BUFF用
    IDA_BUFF_540(540),
    // 更新BUFF用
    IDA_BUFF_541(541),
    // 更新BUFF用
    IDA_BUFF_542(542),
    // 更新BUFF用
    IDA_BUFF_543(543),
    // 更新BUFF用
    IDA_BUFF_544(544),
    // 更新BUFF用
    IDA_BUFF_545(545),
    // 更新BUFF用
    IDA_BUFF_546(546),
    // 更新BUFF用
    IDA_BUFF_547(547),
    // 更新BUFF用
    IDA_BUFF_548(548),
    // 更新BUFF用
    IDA_BUFF_549(549),
    // 更新BUFF用
    IDA_BUFF_550(550),
    // 更新BUFF用
    IDA_BUFF_551(551),
    // 更新BUFF用
    IDA_BUFF_552(552),
    // 更新BUFF用
    IDA_BUFF_553(553),
    // 更新BUFF用
    IDA_BUFF_554(554),
    // 更新BUFF用
    IDA_BUFF_555(555),
    // 更新BUFF用
    IDA_BUFF_556(556),
    // 更新BUFF用
    IDA_BUFF_557(557),
    // 更新BUFF用
    IDA_BUFF_558(558),
    // 更新BUFF用
    IDA_BUFF_559(559),
    // 更新BUFF用
    IDA_BUFF_560(560),
    // 更新BUFF用
    IDA_BUFF_561(561),
    // 更新BUFF用
    IDA_BUFF_562(562),
    // 更新BUFF用
    IDA_BUFF_563(563),
    // 更新BUFF用
    IDA_BUFF_564(564),
    // 更新BUFF用
    IDA_BUFF_565(565),
    // 更新BUFF用
    IDA_BUFF_566(566),
    // 更新BUFF用
    IDA_BUFF_567(567),
    // 更新BUFF用
    IDA_BUFF_568(568),
    // 更新BUFF用
    IDA_BUFF_569(569),
    // 預設Buff-5
    EnergyCharged(570),
    // 衝鋒_速度
    DashSpeed(571),
    // 衝鋒_跳躍
    DashJump(572),
    // 怪物騎乘
    RideVehicle(573),
    // 最終極速
    PartyBooster(574),
    // 指定攻擊(無盡追擊)
    GuidedBullet(575),
    // 預設Buff-1
    Undead(576),
    // 預設Buff-2
    RideVehicleExpire(577),
    // 更新BUFF用
    COUNT_PLUS1(578),
    // 更新BUFF用
    NONE_END(608),
    NONE(-1),
    // TODO: 召喚獸
    SUMMON(-1, true),
    // -----------------[已停用的Buff]
    // 黑色繩索
    DARK_AURA_OLD(888),
    // 藍色繩索
    BLUE_AURA_OLD(888),
    // 黃色繩索
    YELLOW_AURA_OLD(888),
    // 貓頭鷹召喚
    OWL_SPIRIT(888),
    // 超級體
    BODY_BOOST(888),;
    private static final long serialVersionUID = 0L;
    private final int nValue;
    private final int nPos;
    private boolean isIndie = false;

    private MapleBuffStat(int nValue) {
        this.nValue = 1 << (31 - (nValue % 32));
        this.nPos = (int) Math.floor(nValue / 32);
    }

    private MapleBuffStat(int nValue, boolean isIndie) {
        this.nValue = 1 << (31 - (nValue % 32));
        this.nPos = (int) Math.floor(nValue / 32);
        this.isIndie = isIndie;
    }

    @Override
    public int getPosition() {
        return nPos;
    }

    @Override
    public int getValue() {
        return nValue;
    }

    public static MapleBuffStat getMapleBuffStat(int buff) {
        int buf = 1 << (31 - (buff % 32));
        int fir = (int) Math.floor(buff / 32);
        for (MapleBuffStat bb : values()) {
            if (bb.nValue == buf && bb.nPos == fir) {
                return bb;
            }
        }
        return MapleBuffStat.IndiePAD;
    }

    public final boolean isIndie() {
        return isIndie;
    }

    public static boolean SecondaryStat_EnDecode4Byte(MapleBuffStat stat) {
        switch (stat) {
            case CarnivalDefence:
            case SpiritLink:
            case DojangLuckyBonus:
            case SoulGazeCriDamR:
            case PowerTransferGauge:
            case ReturnTeleport:
            case ShadowPartner:
            case IncMaxDamage:
            // case Cyclone:
            // case IDA_SPECIAL_BUFF_4:
            case SetBaseDamage:
            case NaviFlying:
            // case ImmuneBarrier:
            case QuiverCatridge:
            case Dance:
            case SetBaseDamageByBuff:
            case EMHP:
            case EMMP:
            case EPAD:
            case EPDD:
            case DotHealHPPerSecond:
            case DotHealMPPerSecond:
            case MagnetArea:
            case VampDeath:
            case IDA_BUFF_537:
            case IDA_BUFF_301:
            case IDA_BUFF_545:
            case IDA_BUFF_417:
            case IDA_BUFF_161:
                return true;
        }
        return false;
    }

    public static boolean isMovementAffectingStat(MapleBuffStat stat) {
        switch (stat) {
            case Speed:
            case Jump:
            case Stun:
            case Weakness:
            case Slow:
            case Morph:
            case Ghost:
            case BasicStatUp:
            case Attract:
            case RideVehicle:
            case DashSpeed:
            case DashJump:
            case Flying:
            case Frozen:
            case Frozen2:
            case Lapidification:
            case IndieSpeed:
            case IndieJump:
            case KeyDownMoving:
            case EnergyCharged:
            case Mechanic:
            case Magnet:
            case MagnetArea:
            case VampDeath:
            case VampDeathSummon:
            case GiveMeHeal:
            case PvPScoreBonus:
            case NewFlying:
            case QuiverCatridge:
            case UserControlMob:
            case Dance:
            /**
             * *
             *
             */
            case SelfWeakness:
            case Undead:
            case IDA_SPECIAL_BUFF_4:
            case BattlePvP_Helena_WindSpirit:
            case BattlePvP_LeeMalNyun_ScaleUp:
            case TouchMe:
                return true;
        }
        return false;
    }

    public static MapleBuffStat getCTSFromTSIndex(int nIdx) {
        switch (nIdx) {
            case 0:
                return EnergyCharged;
            case 1:
                return DashSpeed;
            case 2:
                return DashJump;
            case 3:
                return RideVehicle;
            case 4:
                return PartyBooster;
            case 5:
                return GuidedBullet;
            case 6:
                return Undead;
            case 7:
                return RideVehicleExpire;
            case 8:
                return COUNT_PLUS1;
            default: {
                return null;
            }
        }
    }

    public static boolean isTSFlag(MapleBuffStat nIdx) {
        switch (nIdx) {
            case EnergyCharged:
            case DashSpeed:
            case DashJump:
            case RideVehicle:
            case PartyBooster:
            case GuidedBullet:
            case Undead:
            case RideVehicleExpire:
            case COUNT_PLUS1:
                return true;
            default:
                return false;
        }
    }
}
