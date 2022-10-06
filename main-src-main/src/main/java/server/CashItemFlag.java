/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author pungin
 */
public enum CashItemFlag {

    // -----------------------------Mask[0]
    // 道具ID [0x1]
    ItemId(0),
    // 數量 [0x2]
    Count(1),
    // 價錢(打折後) [0x4]
    Price(2),
    // [0x8]
    Bonus(3),
    // 排序 [0x10]
    Priority(4),
    // 道具時間 [0x20]
    Period(5),
    // 楓葉點數 [0x40]
    MaplePoint(6),
    // 楓幣 [0x80]
    Meso(7),
    // [0x100]
    ForPremiumUser(8),
    // 性別 [0x200]
    CommodityGender(9),
    // 出售中 [0x400]
    OnSale(10),
    // 商品狀態 [0x800] 0-NEW,1-SALE,2-HOT,3-EVENT,其他-無
    Class(11),
    // [0x1000]
    Limit(12),
    // [0x2000]
    PbCash(13),
    // [0x4000]
    PbPoint(14),
    // [0x8000]
    PbGift(15),
    // 套組訊息 [0x10000]
    PackageSN(16),
    // 需求人氣 [0x20000]
    ReqPOP(17),
    // 需求等級 [0x40000]
    ReqLEV(18),
    // 開始販售時間 [0x80000]
    TermStart(19),
    // 結束販售時間 [0x100000]
    TermEnd(20),
    // [0x200000]
    Refundable(21),
    // [0x400000]
    BombSale(22),
    // 分類訊息[0x800000]
    CategoryInfo(23),
    // 伺服限制[0x1000000]
    WorldLimit(24),
    // [0x2000000]
    Token(25),
    // 限量[0x4000000]
    LimitMax(26),
    // 需求任務[0x8000000]
    CheckQuestID(27),
    // 原始價格[0x10000000]
    OriginalPrice(28),
    // 打折[0x20000000]
    Discount(29),
    // 折扣率[0x40000000]
    DiscountRate(30),
    // 里程折扣與是否衹能使用里程[0x80000000]
    MileageInfo(31),
    // -----------------------------Mask[1]

    // [0x1]
    Zero(32),
    // 需求任務2[0x2]
    CheckQuestID2(33),
    // [0x4]
    UNK34(34),
    // [0x8]
    UNK35(35),
    // [0x10]
    UNK36(36),;

    private final int code;
    private final int first;

    private CashItemFlag(int code) {
        this.code = 1 << (code % 32);
        this.first = (int) Math.floor(code / 32);
    }

    public int getPosition() {
        return first;
    }

    public int getValue() {
        return code;
    }
}
