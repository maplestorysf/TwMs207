/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extensions.temporary;

/**
 *
 * @author pungin
 */
public enum CashItemOpcode {

    CashItemReq_WebShopOrderGetList(0x0), CashItemReq_LoadLocker(0x1), CashItemReq_LoadWish(0x2), CashItemReq_Buy(
            0x3), CashItemReq_Gift(0x4), CashItemReq_SetWish(0x5), CashItemReq_IncSlotCount(
            0x6), CashItemReq_IncTrunkCount(0x7), CashItemReq_IncCharSlotCount(
            0x8), CashItemReq_IncBuyCharCount(0x9), CashItemReq_EnableEquipSlotExt(
            0xA), CashItemReq_CancelPurchase(0xB), CashItemReq_ConfirmPurchase(
            0xC), CashItemReq_Destroy(0xD), CashItemReq_MoveLtoS(
            0xE), CashItemReq_MoveStoL(0xF), CashItemReq_Expire(
            0x10), CashItemReq_Use(0x11), CashItemReq_StatChange(
            0x12), CashItemReq_SkillChange(
            0x13), CashItemReq_SkillReset(
            0x14), CashItemReq_DestroyPetItem(
            0x15), CashItemReq_SetPetName(
            0x16), CashItemReq_SetPetLife(
            0x17), CashItemReq_SetPetSkill(
            0x18), CashItemReq_SetItemName(
            0x19), CashItemReq_SetAndroidName(
            0x1A), CashItemReq_SendMemo(
            0x1B), CashItemReq_GetAdditionalCashShopInfo(
            0x1C), CashItemReq_GetMaplePoint(
            0x1D), CashItemReq_UseMaplePointFromGameSvr(
            0x1E), CashItemReq_Rebate(
            0x1F), CashItemReq_UseCoupon(
            0x20), CashItemReq_GiftCoupon(
            0x21), CashItemReq_Couple(
            0x22), CashItemReq_BuyPackage(
            0x23), CashItemReq_GiftPackage(
            0x24), CashItemReq_BuyNormal(
            0x25), CashItemReq_ApplyWishListEvent(
            0x26), CashItemReq_MovePetStat(
            0x27), CashItemReq_FriendShip(
            0x28), CashItemReq_ShopScan(
            0x29), CashItemReq_ShopOptionScan(
            0x2A), CashItemReq_ShopScanSell(
            0x2B), CashItemReq_LoadPetExceptionList(
            0x2C), CashItemReq_UpdatePetExceptionList(
            0x2D), CashItemReq_DestroyScript(
            0x2E), CashItemReq_CashItemCreate(
            0x2F), CashItemReq_PurchaseRecord(
            0x30), CashItemReq_DeletePurchaseRecord(
            0x31), CashItemReq_TradeDone(
            0x32), CashItemReq_BuyDone(
            0x33), CashItemReq_TradeSave(
            0x34), CashItemReq_TradeLog(
            0x35), CashItemReq_CharacterSale(
            0x36), CashItemReq_SellCashItemBundleToShop(
            0x37), CashItemReq_Refund(
            0x38), CashItemReq_ConfirmRefund(
            0x39), CashItemReq_CancelRefund(
            0x3A), CashItemReq_SetItemNonRefundable(
            0x3B), CashItemReq_WebShopOrderBuyItems(
            0x3C), CashItemReq_UseCashRandomItem(
            0x3D), CashItemReq_UseMaplePointGiftToken(
            0x3E), CashItemReq_BuyByToken(
            0x3F), CashItemReq_Buy_ByMeso(
            0x40), CashItemReq_UpgradeValuePack(
            0x41), CashItemReq_BuyFarmGift(
            0x42), CashItemReq_CashItemGachapon(
            0x43), CashItemReq_GiftScript(
            0x44), CashItemReq_MoveToAuctionStore(
            0x45), CashItemReq_ClearCashOption(
            0x46), CashItemReq_MasterPiece(
            0x47), CashItemReq_DestroyCoupleRings(
            0x48), CashitemReq_DestroyFriendshipRings(
            0x49), CashItemReq_LockerTransfer(
            0x4A), CashItemReq_TradeLogForAuction(
            0x4B), CashItemReq_MoveToLockerFromAuction(
            0x4C), CashItemReq_NexonStarCouponUse(
            0x4D), CashItemRes_CharacterSaleSuccess(
            0x0), CashItemRes_CharacterSaleFail(
            0x1), CashItemRes_LimitGoodsCount_Changed(
            0x2), CashItemRes_WebShopOrderGetList_Done(
            0x3), CashItemRes_WebShopOrderGetList_Failed(
            0x4), CashItemRes_WebShopReceive_Done(
            0x5), CashItemRes_LoadLocker_Done(
            0x6), CashItemRes_LoadLocker_Failed(
            0x7), CashItemRes_LoadGift_Done(
            0x8), CashItemRes_LoadGift_Failed(
            0x9), CashItemRes_LoadWish_Done(
            0xA), CashItemRes_LoadWish_Failed(
            0xB), CashItemRes_SetWish_Done(
            0xC), CashItemRes_SetWish_Failed(
            0xD), CashItemRes_Buy_Done(
            0xE), CashItemRes_Buy_Failed(
            0xF), CashItemRes_UseCoupon_Done(
            0x10), CashItemRes_NexonStarCouponUse_Done(
            0x11), CashItemRes_NexonStarCoupon_Failed(
            0x12), CashItemRes_UseCoupon_Done_NormalItem(
            0x13), CashItemRes_GiftCoupon_Done(
            0x14), CashItemRes_UseCoupon_Failed(
            0x15), CashItemRes_UseCoupon_CashItem_Failed(
            0x16), CashItemRes_Gift_Done(
            0x17), CashItemRes_Gift_Failed(
            0x18), CashItemRes_IncSlotCount_Done(
            0x19), CashItemRes_IncSlotCount_Failed(
            0x1A), CashItemRes_IncTrunkCount_Done(
            0x1B), CashItemRes_IncTrunkCount_Failed(
            0x1C), CashItemRes_IncCharSlotCount_Done(
            0x1D), CashItemRes_IncCharSlotCount_Failed(
            0x1E), CashItemRes_IncBuyCharCount_Done(
            0x1F), CashItemRes_IncBuyCharCount_Failed(
            0x20), CashItemRes_EnableEquipSlotExt_Done(
            0x21), CashItemRes_EnableEquipSlotExt_Failed(
            0x22), CashItemRes_MoveLtoS_Done(
            0x23), CashItemRes_MoveLtoS_Failed(
            0x24), CashItemRes_MoveStoL_Done(
            0x25), CashItemRes_MoveStoL_Failed(
            0x26), CashItemRes_Destroy_Done(
            0x27), CashItemRes_Destroy_Failed(
            0x28), CashItemRes_Expire_Done(
            0x29), CashItemRes_Expire_Failed(
            0x2A), CashItemRes_Use_Done(
            0x2B), CashItemRes_Use_Failed(
            0x2C), CashItemRes_StatChange_Done(
            0x2D), CashItemRes_StatChange_Failed(
            0x2E), CashItemRes_SkillChange_Done(
            0x2F), CashItemRes_SkillChange_Failed(
            0x30), CashItemRes_SkillReset_Done(
            0x31), CashItemRes_SkillReset_Failed(
            0x32), CashItemRes_DestroyPetItem_Done(
            0x33), CashItemRes_DestroyPetItem_Failed(
            0x34), CashItemRes_SetPetName_Done(
            0x35), CashItemRes_SetPetName_Failed(
            0x36), CashItemRes_SetPetLife_Done(
            0x37), CashItemRes_SetPetLife_Failed(
            0x38), CashItemRes_MovePetStat_Failed(
            0x39), CashItemRes_MovePetStat_Done(
            0x3A), CashItemRes_SetPetSkill_Failed(
            0x3B), CashItemRes_SetPetSkill_Done(
            0x3C), CashItemRes_SendMemo_Done(
            0x3D), CashItemRes_SendMemo_Warning(
            0x3E), CashItemRes_SendMemo_Failed(
            0x3F), CashItemRes_GetMaplePoint_Done(
            0x40), CashItemRes_GetMaplePoint_Failed(
            0x41), CashItemRes_UseMaplePointFromGameSvr_Done(
            0x42), CashItemRes_UseMaplePointFromGameSvr_Failed(
            0x43), CashItemRes_CashItemGachapon_Done(
            0x44), CashItemRes_CashItemGachapon_Failed(
            0x45), CashItemRes_Rebate_Done(
            0x46), CashItemRes_Rebate_Failed(
            0x47), CashItemRes_Couple_Done(
            0x48), CashItemRes_Couple_Failed(
            0x49), CashItemRes_BuyPackage_Done(
            0x4A), CashItemRes_BuyPackage_Failed(
            0x4B), CashItemRes_GiftPackage_Done(
            0x4C), CashItemRes_GiftPackage_Failed(
            0x4D), CashItemRes_BuyNormal_Done(
            0x4E), CashItemRes_BuyNormal_Failed(
            0x4F), CashItemRes_ApplyWishListEvent_Done(
            0x50), CashItemRes_ApplyWishListEvent_Failed(
            0x51), CashItemRes_Friendship_Done(
            0x52), CashItemRes_Friendship_Failed(
            0x53), CashItemRes_LoadExceptionList_Done(
            0x54), CashItemRes_LoadExceptionList_Failed(
            0x55), CashItemRes_UpdateExceptionList_Done(
            0x56), CashItemRes_UpdateExceptionList_Failed(
            0x57), CashItemRes_DestroyScript_Done(
            0x58), CashItemRes_DestroyScript_Failed(
            0x59), CashItemRes_CashItemCreate_Done(
            0x5A), CashItemRes_CashItemCreate_Failed(
            0x5B), CashItemRes_ClearOptionScript_Done(
            0x5C), CashItemRes_ClearOptionScript_Failed(
            0x5D), CashItemRes_Bridge_Failed(
            0x5E), CashItemRes_PurchaseRecord_Done(
            0x5F), CashItemRes_PurchaseRecord_Failed(
            0x60), CashItemRes_DeletePurchaseRecord_Done(
            0x61), CashItemRes_DeletePurchaseRecord_Failed(
            0x62), CashItemRes_Refund_OK(
            0x63), CashItemRes_Refund_Done(
            0x64), CashItemRes_Refund_Failed(
            0x65), CashItemRes_UseRandomCashItem_Done(
            0x66), CashItemRes_UseRandomCashItem_Failed(
            0x67), CashItemRes_SetAndroidName_Done(
            0x68), CashItemRes_SetAndroidName_Failed(
            0x69), CashItemRes_UseMaplePointGiftToken_Done(
            0x6A), CashItemRes_UseMaplePointGiftToken_Failed(
            0x6B), CashItemRes_BuyByToken_Done(
            0x6C), CashItemRes_BuyByToken_Failed(
            0x6D), CashItemRes_UpgradeValuePack_Done(
            0x6E), CashItemRes_UpgradeValuePack_Failed(
            0x6F), CashItemRes_EventCashItem_Buy_Result(
            0x70), CashItemRes_BuyFarmGift_Done(
            0x71), CashItemRes_BuyFarmGift_Failed(
            0x72), CashItemRes_GiftScript_Done(
            0x73), CashItemRes_GiftScript_Failed(
            0x74), CashItemRes_AvatarMegaphone_Queue_Full(
            0x75), CashItemRes_AvatarMegaphone_Level_Limit(
            0x76), CashItemRes_MovoCashItemToAuction_Done(
            0x77), CashItemRes_MovoCashItemToAuction_Failed(
            0x78), CashItemRes_MasterPiece_Done(
            0x79), CashItemRes_MasterPiece_Failed(
            0x7A), CashItemRes_DestroyCoupleRings_Done(
            0x7B), CashItemRes_DestroyCoupleRings_Failed(
            0x7C), CashItemRes_DestroyFriendShipRings_Done(
            0x7D), CashItemRes_DestroyFriendShipRings_Failed(
            0x7E), CashItemRes_LockerTransfer_Done(
            0x7F), CashItemRes_LockerTransfer_Failed(
            0x80), CashItemRes_MovoCashItemToLockerFromAuction_Done(
            0x81), CashItemRes_MovoCashItemToLockerFromAuction_Failed(
            0x82), CashItemFailReason_Unknown(
            0x0), CashItemFailReason_Timeout(
            0x1), CashItemFailReason_CashDaemonDBError(
            0x2), CashItemFailReason_NoRemainCash(
            0x3), CashItemFailReason_GiftUnderAge(
            0x4), CashItemFailReason_GiftLimitOver(
            0x5), CashItemFailReason_GiftSameAccount(
            0x6), CashItemFailReason_GiftUnknownRecipient(
            0x7), CashItemFailReason_GiftRecipientGenderMismatch(
            0x8), CashItemFailReason_GiftRecipientLockerFull(
            0x9), CashItemFailReason_BuyStoredProcFailed(
            0xA), CashItemFailReason_GiftStoredProcFailed(
            0xB), CashItemFailReason_GiftNoReceiveCharacter(
            0xC), CashItemFailReason_GiftNoSenderCharacter(
            0xD), CashItemFailReason_InvalidCoupon(
            0xE), CashItemFailReason_ExpiredCoupon(
            0xF), CashItemFailReason_UsedCoupon(
            0x10), CashItemFailReason_CouponForCafeOnly(
            0x11), CashItemFailReason_CouponForCafeOnly_Used(
            0x12), CashItemFailReason_CouponForCafeOnly_Expired(
            0x13), CashItemFailReason_NotAvailableCoupon(
            0x14), CashItemFailReason_GenderMisMatch(
            0x15), CashItemFailReason_GiftNormalItem(
            0x16), CashItemFailReason_GiftMaplePoint(
            0x17), CashItemFailReason_NoEmptyPos(
            0x18), CashItemFailReason_ForPremiumUserOnly(
            0x19), CashItemFailReason_BuyCoupleStoredProcFailed(
            0x1A), CashItemFailReason_BuyFriendshipStoredProcFailed(
            0x1B), CashItemFailReason_NotAvailableTime(
            0x1C), CashItemFailReason_NoStock(
            0x1D), CashItemFailReason_PurchaseLimitOver(
            0x1E), CashItemFailReason_NoRemainMeso(
            0x1F), CashItemFailReason_IncorrectSSN2(
            0x20), CashItemFailReason_IncorrectSPW(
            0x21), CashItemFailReason_ForNoPurchaseExpUsersOnly(
            0x22), CashItemFailReason_AlreadyApplied(
            0x23), CashItemFailReason_WebShopUnknown(
            0x24), CashItemFailReason_WebShopInventoryCount(
            0x25), CashItemFailReason_WebShopBuyStoredProcFailed(
            0x26), CashItemFailReason_WebShopInvalidOrder(
            0x27), CashItemFailReason_GachaponLimitOver(
            0x28), CashItemFailReason_NoUser(
            0x29), CashItemFailReason_WrongCommoditySN(
            0x2A), CashItemFailReason_CouponLimitError(
            0x2B), CashItemFailReason_CouponLimitError_Hour(
            0x2C), CashItemFailReason_CouponLimitError_Day(
            0x2D), CashItemFailReason_CouponLimitError_Week(
            0x2E), CashItemFailReason_BridgeNotConnected(
            0x2F), CashItemFailReason_TooYoungToBuy(
            0x30), CashItemFailReason_GiftTooYoungToRecv(
            0x31), CashItemFailReason_LimitOverTheItem(
            0x32), CashItemFailReason_CashLock(
            0x33), CashItemFailReason_FindSlotPos(
            0x34), CashItemFailReason_GetItem(
            0x35), CashItemFailReason_DestroyCashItem(
            0x36), CashItemFailReason_NotSaleTerm(
            0x37), CashItemFailReason_InvalidCashItem(
            0x38), CashItemFailReason_InvalidRandomCashItem(
            0x39), CashItemFailReason_ReceiveItem(
            0x3A), CashItemFailReason_UseRandomCashItem(
            0x3B), CashItemFailReason_NotGameSvr(
            0x3C), CashItemFailReason_NotShopSvr(
            0x3D), CashItemFailReason_ItemLockerIsFull(
            0x3E), CashItemFailReason_NoAndroid(
            0x3F), CashItemFailReason_DBQueryFailed(
            0x40), CashItemFailReason_UserSaveFailed(
            0x41), CashItemFailReason_CannotBuyMonthlyOnceItem(
            0x42), CashItemFailReason_OnlyCashItem(
            0x43), CashItemFailReason_NotEnoughMaplePoint(
            0x44), CashItemFailReason_TooMuchMaplePointAlready(
            0x45), CashItemFailReason_GiveMaplePointUnknown(
            0x46), CashItemFailReason_OnWorld(
            0x47), CashItemFailReason_NoRemainToken(
            0x48), CashItemFailReason_GiftToken(
            0x49), CashItemFailReason_LimitOverCharacter(
            0x4A), CashItemFailReason_CurrentValuePack(
            0x4B), CashItemFailReason_NoRemainCashMileage(
            0x4C), CashItemFailReason_NotEquipItem(
            0x4D), CashItemFailReason_DoNotReceiveCashItemInvenFull(
            0x4E), CashItemFailReason_DoNotCheckQuest(
            0x4F), CashItemFailReason_SpecialServerUnable(
            0x50), CashItemFailReason_BuyWSLimit(
            0x51), CashItemFailReason_NoNISMS(
            0x52), CashItemFailReason_RefundExpired(
            0x53), CashItemFailReason_NoRefundItem(
            0x54), CashItemFailReason_NoRefundPackage(
            0x55), CashItemFailReason_PurchaseItemLimitOver(
            0x56), CashItemFailReason_OTPStateError(
            0x57), CashItemFailReason_WrongPassword(
            0x58), CashItemFailReason_CountOver(
            0x59), CashItemFailReason_Reissuing(
            0x5A), CashItemFailReason_NotExist(
            0x5B), CashItemFailReason_NotAvailableLockerTransfer(
            0x5C), CashItemFailReason_DormancyAccount(
            0x5D),;

    private final int value;

    private CashItemOpcode(int value) {
        this.value = value;
    }

    public static CashItemOpcode getType(int type) {
        for (CashItemOpcode et : values()) {
            if (et.getValue() == type) {
                return et;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
