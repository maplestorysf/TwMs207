package extensions.temporary;

/**
 *
 * @author Pungin
 */
public enum UserEffectOpcode {

    // 等級提升
    UserEffect_LevelUp(0x0),
    // 近端技能特效
    UserEffect_SkillUse(0x1),
    // 遠端技能特效
    UserEffect_SkillUseBySummoned(0x2),
    // 0x3
    // 特殊技能特效
    UserEffect_SkillAffected(0x4),
    // 機甲戰神-輔助機器特效
    UserEffect_SkillAffected_Ex(0x5), UserEffect_SkillAffected_Select(0x6), UserEffect_SkillSpecialAffected(0x7),
    // 物品獲得/丟棄文字特效
    UserEffect_Quest(0x8),
    // 寵物等級提升
    UserEffect_Pet(0x9),
    // 技能飛行體特效
    UserEffect_SkillSpecial(0xA),
    // 抵抗異常狀態
    UserEffect_Resist(0xB),
    // 使用護身符
    UserEffect_ProtectOnDieItemUse(0xC), UNK_D(0xD), UserEffect_PlayPortalSE(0xE),
    // 職業變更
    UserEffect_JobChanged(0xF),
    // 任務完成
    UserEffect_QuestComplete(0x10),
    // 回復特效(Byte)
    UserEffect_IncDecHPEffect(0x11), UserEffect_BuffItemEffect(0x12), UserEffect_SquibEffect(0x13),
    // 拾取怪物卡片[188-完成]
    UserEffect_MonsterBookCardGet(0x14), // mCardGet
    UserEffect_LotteryUse(0x15), UserEffect_ItemLevelUp(0x16), UserEffect_ItemMaker(0x17),
    // 0x18 [Int] MESO+
    UserEffect_ExpItemConsumed(0x19),
    // 連續擊殺時獲得的經驗提示
    UserEffect_FieldExpItemConsumed(0x1A),
    // 顯示WZ的效果
    UserEffect_ReservedEffect(0x1B),
    // 聊天窗顯示"消耗1個原地復活術 ，於角色所在原地進行復活！（尚餘Byte個）"
    UserEffect_UpgradeTombItemUse(0x1C), UserEffect_BattlefieldItemUse(0x1D),
    // 顯示WZ的效果2
    UserEffect_AvatarOriented(0x1E), UserEffect_AvatarOrientedRepeat(0x1F), UserEffect_AvatarOrientedMultipleRepeat(
            0x20), UserEffect_IncubatorUse(0x21),
    // WZ聲音
    UserEffect_PlaySoundWithMuteBGM(0x22),
    // WZ聲音
    UserEffect_PlayExclSoundWithDownBGM(0x22), UNK_23(0x23),
    // 商城道具效果
    UserEffect_SoulStoneUse(0x24),
    // 回復特效(Int)
    UserEffect_IncDecHPEffect_EX(0x25), UserEffect_IncDecHPRegenEffect(0x26), UNK_27(0x27), UNK_28(0x28), UNK_29(
            0x29), UNK_2A(0x2A), UNK_2B(0x2B), UNK_2C(0x2C), UNK_2D(0x2D),
    // 採集/挖礦
    UserEffect_EffectUOL(0x2E), UserEffect_PvPRage(0x2F), UserEffect_PvPChampion(0x30), UserEffect_PvPGradeUp(
            0x31), UserEffect_PvPRevive(0x32), UserEffect_JobEffect(0x33),
    // 背景變黑
    UserEffect_FadeInOut(0x34), UserEffect_MobSkillHit(0x35), UserEffect_AswanSiegeAttack(0x36),
    // 影武者出生劇情背景黑暗特效
    UserEffect_BlindEffect(0x37), UserEffect_BossShieldCount(0x38),
    // 天使技能充能效果
    UserEffect_ResetOnStateForOnOffSkill(0x39), UserEffect_JewelCraft(0x3A), UserEffect_ConsumeEffect(
            0x3B), UserEffect_PetBuff(0x3C), UserEffect_LotteryUIResult(0x3D), UserEffect_LeftMonsterNumber(
            0x3E), UserEffect_ReservedEffectRepeat(0x3F), UserEffect_RobbinsBomb(0x40), UserEffect_SkillMode(
            0x41), UserEffect_ActQuestComplete(0x42), UserEffect_Point(0x43),
    // NPC說話特效
    UserEffect_SpeechBalloon(0x44),
    // 特殊頂部訊息[如燃燒場地]
    UserEffect_TextEffect(0x45),
    // 暗夜行者技能特效
    UserEffect_SkillPreLoopEnd(0x46), UNK_47(0x47), UNK_48(0x48), UserEffect_Aiming(0x49), // Effect/BasicEff.img/aiming/%d
    // 獲得道具頂部提示 UI/UIWindow.img/FloatNotice/%d/DrawOrigin/icon
    UserEffect_PickUpItem(0x4A), UserEffect_BattlePvP_IncDecHp(0x4B), UserEffect_BiteAttack_ReceiveSuccess(0x4C), // Effect/OnUserEff.img/urus/catch
    // 烏勒斯接住人時候Catch字樣
    UserEffect_BiteAttack_ReceiveFail(0x4D), // Effect/ItemEff.img/2270002/fail
    UserEffect_IncDecHPEffect_Delayed(0x4E), UserEffect_Lightness(0x4F),
    // 花狐技能
    User_ActionSetUsed(0x50),;

    private final int value;

    private UserEffectOpcode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
