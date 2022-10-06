package client;

public enum ExpMasks {

    // [Int] 活動獎勵經驗值(+%d)
    SelectedMobBonusExp(0x1),
    // [Byte] 怪物額外經驗值(+%d%)
    MONSTER_PERCENT(0x2),
    // [Byte] 活動組隊經驗值(+%d)
    PartyBonusPercentage(0x4),
    // 0x8 [Byte]
    // [Int] 組隊經驗值(+%d)
    PartyBonusExp(0x10),
    // [Int] 結婚紅利經驗值(+%d)
    WeddingBonusExp(0x20),
    // [Int] 道具裝備紅利經驗值(+%d)
    ItemBonusExp(0x40),
    // [Int] 高級服務贈送經驗值(+%d)
    PremiumIPBonusExp(0x80),
    // [Int] 彩虹週獎勵經驗值(+%d)
    RainbowWeekEventBonusExp(0x100),
    // [Int] 爆發獎勵經驗值(+%d)
    BoomUpEventBonusExp(0x200),
    // [Int] 秘藥額外經驗值(+%d)
    PlusExpBuffBonusExp(0x400),
    // [Int] (null)額外經驗值(+%d) - null處客戶端會判斷是否有精靈遊俠的LINK「精靈的祝福」跟神之子角色卡「神使 神之子」
    PsdBonusExpRate(0x800),
    // [Int] 加持獎勵經驗值(+%d)
    IndieBonusExp(0x1000),
    // [Int] 休息獎勵經驗值(+%d)
    RelaxBonusExp(0x2000),
    // [Int] 道具獎勵經驗值(+%d)
    InstallItemBonusExp(0x4000),
    // [Int] 阿斯旺勝利者獎勵經驗值(+%d)
    AswanWinnerBonusExp(0x8000),
    // [Int] 依道具%增加經驗值(+%d)
    ExpByIncExpR(0x10000),
    // [Int] 超值包獎勵經驗值(+%d)
    ValuePackBonusExp(0x20000),
    // [Int] 依道具的組隊任務%增加經驗值(+%d)
    ExpByIncPQExpR(0x40000),
    // [Int] 獲得追加經驗值(+%d)
    BaseAddExp(0x80000),
    // [Int] 家族經驗值獎勵(+%d)
    BloodAllianceBonusExp(0x100000),
    // [Int] 冷凍勇士經驗值獎勵(+ %d)
    FreezeHotEventBonusExp(0x200000),
    // d-[Int]x-[Int] 燃燒場地獎勵經驗 x%(+%d)
    RestFieldBonusExp(0x400000),
    UserHPRateBonusExp(0x800000),
    FieldValueBonusExp(0x1000000),
    MobKillBonusExp(0x2000000),
    LiveEventBonusExp(0x4000000),
    Unk_1(0x8000000),
    Unk_2(0x10000000),
    Unk_3(0x20000000),
    Unk_4(0x40000000),
    Unk_5(0x80000000),
    Unk_6(0x100000000L),
    ;

    final long value;

    private ExpMasks(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

}
