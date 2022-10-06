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
public enum MessageOpcode {

    MS_DropPickUpMessage(0x0),
    MS_QuestRecordMessage(0x1),
    MS_QuestRecordMessageAddValidCheck(0x2),
    MS_CashItemExpireMessage(0x3),
    MS_IncEXPMessage(0x4),
    MS_IncSPMessage(0x5),
    MS_IncPOPMessage(0x6),
    MS_IncMoneyMessage(0x7),
    MS_IncGPMessage(0x8),
    MS_IncCommitmentMessage(0x9),
    MS_GiveBuffMessage(0xA),
    MS_GeneralItemExpireMessage(0xB),
    MS_SystemMessage(0xC),
    MS_D(0xD),
    MS_QuestRecordExMessage(0xE),
    MS_WorldShareRecordMessage(0xF),
    MS_ItemProtectExpireMessage(0x10),
    MS_ItemExpireReplaceMessage(0x11),
    MS_ItemAbilityTimeLimitedExpireMessage(0x12),
    MS_SkillExpireMessage(0x13),
    MS_IncNonCombatStatEXPMessage(0x14),
    MS_LimitNonCombatStatEXPMessage(0x15),
    MS_RecipeExpireMessage(0x16),
    MS_AndroidMachineHeartAlertMessage(0x17),
    MS_IncFatigueByRestMessage(0x18),
    MS_19(0x19),
    MS_IncPvPPointMessage(0x1A),
    MS_PvPItemUseMessage(0x1B),
    MS_WeddingPortalError(0x1C),
    MS_PvPHardCoreExpMessage(0x1D),
    MS_NoticeAutoLineChanged(0x1E),
    MS_EntryRecordMessage(0x1F),
    MS_EvolvingSystemMessage(0x20),
    MS_EvolvingSystemMessageWithName(0x21),
    MS_CoreInvenOperationMessage(0x22),
    MS_NxRecordMessage(0x23),
    MS_BlockedBehaviorTypeMessage(0x24),
    MS_IncWPMessage(0x25),
    MS_MaxWPMessage(0x26),
    MS_StylishKillMessage(0x27),
    // MS_BarrierEffectIgnoreMessage
    MS_ExpiredCashItemResultMessage(0x28),
    MS_CollectionRecordMessage(0x29),
    MS_RandomChanceMessage(0x2A),
    MS_2B(0x2B),
    MS_2C(0x2C),
    MS_2D(0x2D),
    MS_2E(0x2E),
    MS_2F(0x2F),
    MS_30(0x30),
    MS_31(0x31),
    MS_32(0x32),
    MS_33(0x33),
    MS_34(0x34),
    ;

    private final int value;

    private MessageOpcode(int value) {
        this.value = value;
    }

    public static MessageOpcode getType(int type) {
        for (MessageOpcode msT : values()) {
            if (msT.getValue() == type) {
                return msT;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }
}
