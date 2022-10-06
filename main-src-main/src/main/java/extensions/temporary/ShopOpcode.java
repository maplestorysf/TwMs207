/*
 * To change this license header), choose License Headers in Project Properties.
 * To change this template file), choose Tools | Templates
 * and open the template in the editor.
 */
package extensions.temporary;

/**
 *
 * @author pungin
 */
public enum ShopOpcode {

    ShopReq_Buy(0x0),
    ShopReq_Sell(0x1),
    ShopReq_Recharge(0x2),
    ShopReq_Close(0x3),
    ShopReq_StarCoinRes(0x4),
    ShopRes_BuySuccess(0x0),
    ShopRes_BuyNoStock(0x1),
    ShopRes_BuyNoMoney(0x2),
    ShopRes_BuyNoPoint(0x3),
    ShopRes_BuyNoFloor(0x4),
    ShopRes_5(0x5),
    ShopRes_BuyNoQuestEx(0x6),
    ShopRes_BuyInvalidTime(0x7),
    ShopRes_BuyUnknown(0x8),
    ShopRes_SellSuccess(0x9),
    ShopRes_SellNoStock(0xA),
    ShopRes_SellIncorrectRequest(0xB),
    ShopRes_SellOverflow(0xC),
    ShopRes_SellLimitPriceAtOnetime(0xD),
    ShopRes_SellUnkonwn(0xE),
    ShopRes_RechargeSuccess(0xF),
    ShopRes_RechargeNoStock(0x10),
    ShopRes_RechargeNoMoney(0x11),
    ShopRes_RechargeIncorrectRequest(0x12),
    ShopRes_RechargeUnknown(0x13),
    ShopRes_BuyNoToken(0x14),
    ShopRes_BuyNoStarCoin(0x15),
    ShopRes_LimitLevel_Less(0x16),
    ShopRes_LimitLevel_More(0x17),
    ShopRes_CantBuyAnymore(0x18),
    ShopRes_FailedByBuyLimit(0x19),
    ShopRes_TradeBlocked(0x1A),
    ShopRes_NpcRandomShopReset(0x1B),
    ShopRes_BuyStockOver(0x1C),
    ShopRes_DisabledNPC(0x1D),
    ShopRes_TradeBlockedNotActiveAccount(0x1E),
    ShopRes_TradeBlockedSnapShot(0x1F),
    ShopRes_MarketTempBlock(0x20),
    ShopRes_UnalbeWorld(0x21),
    ShopRes_UnalbeShopVersion(0x22),
    ShopRes_23(0x23),
    ShopRes_24(0x24),
    ShopRes_25(0x25),
    ShopRes_26(0x26),
    ShopRes_27(0x27),
    ShopRes_28(0x28),
    ;

    private final int value;

    private ShopOpcode(int value) {
        this.value = value;
    }

    public static ShopOpcode getOpcode(int type) {
        for (ShopOpcode gro : values()) {
            if (gro.getValue() == type) {
                return gro;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
