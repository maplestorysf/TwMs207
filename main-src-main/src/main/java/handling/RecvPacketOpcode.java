/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import tools.StringUtil;

/**
 *
 * @author Pungin
 */
public enum RecvPacketOpcode implements WritableIntValueHolder {

    CP_DUMMY_CODE(false),
    CP_BEGIN_SOCKET(false),
    CP_CheckLoginAuthInfo(false),
    CP_SelectWorld,
    CP_CheckSPWRequest,
    CP_CheckSPWExistRequest,
    CP_MigrateIn(false),
    CP_SelectCharacter,
    CP_WorldInfoRequest,
    CP_LogoutWorld(false),
    CP_CheckDuplicatedID,
    CP_CreateNewCharacter,
    CP_CreateNewCharacterInCS,
    CP_CreateNewCharacter_PremiumAdventurer,
    CP_DeleteCharacter,
    CP_ExceptionLog(false),
    CP_AlbaRequest,
    CP_UpdateCharacterCard,
    CP_CheckCenterAndGameAreConnected,
    CP_AliveAck(false),
    CP_ClientDumpLog,
    SET_GENDER(false),
    SERVERSTATUS_REQUEST,
    GET_SERVER(false),
    CLIENT_START(false),
    CP_UserTransferFieldRequest,
    CP_UserTransferChannelRequest,
    CP_UserMigrateToCashShopRequest,
    CP_UserMigrateToPvpRequest,
    CP_UserTransferAswanRequest,
    CP_UserTransferAswanReadyRequest,
    CP_AswanRetireRequest,
    CP_UserRequestPvPStatus,
    CP_UserMigrateToPveRequest,
    CP_UserFinalAttackRequest,
    CP_UserMove,
    CP_UserSitRequest,
    CP_UserPortableChairSitRequest,
    CP_UserMeleeAttack,
    CP_UserShootAttack,
    CP_UserMagicAttack,
    CP_UserBodyAttack,
    CP_UserAreaDotAttack,
    CP_UserMovingShootAttackPrepare,
    CP_UserHit,
    CP_UserAttackUser,
    CP_UserChat,
    CP_UserADBoardClose,
    CP_UserEmotion,
    CP_AndroidEmotion,
    CP_UserActivateEffectItem,
    CP_UserMonkeyEffectItem,
    CP_UserActivateNickItem,
    CP_UserActivateDamageSkin,
    CP_UserDefaultWingItem,
    CP_UserBanMapByMob,
    CP_UserSelectNpc,
    CP_UserScriptMessageAnswer,
    CP_UserShopRequest,
    CP_UserTrunkRequest,
    CP_UserEntrustedShopRequest,
    CP_UserStoreBankRequest,
    CP_UserParcelRequest,
    CP_UserEffectLocal,
    CP_UserSpecialEffectLocal,
    CP_ShopScannerRequest,
    CP_ShopLinkRequest,
    CP_AdminShopRequest,
    CP_UserGatherItemRequest,
    CP_UserSortItemRequest,
    CP_UserChangeSlotPositionRequest,
    CP_UserPopOrPushBagItemToInven,
    CP_UserBagToBagItem,
    CP_UserStatChangeItemUseRequest,
    CP_UserStatChangeItemCancelRequest,
    CP_UserMobSummonItemUseRequest,
    CP_UserPetFoodItemUseRequest,
    CP_UserTamingMobFoodItemUseRequest,
    CP_UserScriptItemUseRequest,
    CP_UserRecipeOpenItemUseRequest,
    CP_UserConsumeCashItemUseRequest,
    CP_UserAdditionalSlotExtendItemUseRequest,
    CP_UserCashPetPickUpOnOffRequest,
    CP_UserCashPetSkillSettingRequest,
    CP_UserOptionChangeRequest,
    CP_UserDestroyPetItemRequest,
    CP_UserSkillLearnItemUseRequest,
    CP_UserExpConsumeItemUseRequest,
    CP_UserShopScannerItemUseRequest,
    CP_UserMapTransferItemUseRequest,
    CP_UserPortalScrollUseRequest,
    CP_BEGIN_USER,
    CP_UserFieldTransferRequest,
    CP_UserUpgradeItemUseRequest,
    CP_UserUpgradeAssistItemUseRequest,
    CP_UserHyperUpgradeItemUseRequest,
    CP_UserItemOptionUpgradeItemUseRequest,
    CP_UserAdditionalOptUpgradeItemUseRequest,
    CP_UserItemSlotExtendItemUseRequest,
    CP_UserWeaponTempItemOptionRequest,
    CP_UserItemSkillSocketUpgradeItemUseRequest,
    CP_UserItemSkillOptionUpgradeItemUseRequest,
    CP_UserFreeMiracleCubeItemUseRequest,
    CP_UserEquipmentEnchantWithSingleUIRequest,
    CP_UserBagItemUseRequest,
    CP_UserItemReleaseRequest,
    CP_UserToadsHammerRequest,
    CP_UserAbilityUpRequest,
    CP_UserAbilityMassUpRequest,
    CP_UserChangeStatRequest,
    CP_SetSonOfLinkedSkillRequest,
    CP_UserSkillUpRequest,
    CP_UserSkillUseRequest,
    CP_UserSkillCancelRequest,
    CP_UserSkillPrepareRequest,
    CP_UserDropMoneyRequest,
    CP_UserGivePopularityRequest,
    CP_UserCharacterInfoRequest,
    CP_UserActivatePetRequest,
    CP_UserRegisterPetAutoBuffRequest,
    CP_UserTemporaryStatUpdateRequest,
    CP_UserPortalScriptRequest,
    CP_UserPortalTeleportRequest,
    CP_UserMapTransferRequest,
    CP_UserAntiMacroItemUseRequest,
    CP_UserAntiMacroSkillUseRequest,
    CP_UserOldAntiMacroQuestionResult,
    CP_UserAntiMacroRefreshRequest,
    CP_UserClaimRequest,
    CP_UserQuestRequest,
    CP_UserMedalReissueRequest,
    CP_UserCalcDamageStatSetRequest,
    CP_UserB2BodyRequest,
    CP_UserThrowGrenade,
    CP_UserDestroyGrenade,
    CP_UserCreateAuraByGrenade,
    CP_UserSetMoveGrenade,
    CP_UserMacroSysDataModified,
    CP_UserSelectNpcItemUseRequest,
    CP_UserItemMakeRequest,
    CP_UserRepairDurabilityAll,
    CP_UserRepairDurability,
    CP_UserFollowCharacterRequest,
    CP_UserSelectPQReward,
    CP_UserRequestPQReward,
    CP_SetPassenserResult,
    CP_UserRequestInstanceTable,
    CP_UserRequestCreateItemPot,
    CP_UserRequestRemoveItemPot,
    CP_UserRequestIncItemPotLifeSatiety,
    CP_UserRequestCureItemPotLifeSick,
    CP_UserRequestComplateToItemPot,
    CP_UserRequestRespawn,
    CP_UserConsumeHairItemUseRequest,
    CP_UserRequestCharacterPotentialSkillRandSet,
    CP_UserRequestCharacterPotentialSkillRandSetUI,
    CP_UserForceAtomCollision,
    CP_ZeroTag,
    CP_ZeroShareCashEquipPart,
    CP_UserLuckyItemUseRequest,
    CP_UserMobMoveAbilityChange,
    CP_BroadcastMsg,
    CP_GroupMessage,
    CP_Whisper,
    CP_Messenger,
    CP_MiniRoom,
    CP_PartyRequest,
    CP_PartyResult,
    CP_PartyInvitableSet,
    CP_ExpeditionRequest,
    CP_PartyAdverRequest,
    CP_GuildRequest,
    CP_GuildResult,
    CP_GuildJoinRequest,
    CP_GuildJoinCancelRequest,
    CP_GuildJoinAccept,
    CP_GuildJoinReject,
    CP_Admin,
    CP_Log,
    CP_FriendRequest,
    CP_MemoFlagRequest,
    CP_EnterTownPortalRequest,
    CP_EnterOpenGateRequest,
    CP_FuncKeyMappedModified,
    CP_RPSGame,
    CP_MarriageRequest,
    CP_WeddingWishListRequest,
    CP_AllianceRequest,
    CP_AllianceResult,
    CP_TalkToTutor,
    CP_RequestIncCombo,
    CP_RequestDecCombo,
    CP_MakingSkillRequest,
    CP_BroadcastEffectToSplit,
    CP_BroadcastOneTimeActionToSplit,
    CP_UserTransferFreeMarketRequest,
    CP_UserRequestSetStealSkillSlot,
    CP_UserRequestStealSkillMemory,
    CP_UserRequestStealSkillList,
    CP_UserRequestStealSkill,
    CP_UserRequestFlyingSwordStart,
    CP_UserHyperSkillUpRequest,
    CP_UserHyperSkillResetRequset,
    CP_UserHyperStatSkillUpRequest,
    CP_UserHyperStatSkillResetRequest,
    CP_RequestReloginCookie,
    CP_WaitQueueRequest,
    CP_CheckTrickOrTreatRequest,
    CP_MapleStyleBonusRequest,
    CP_UserAntiMacroQuestionResult,
    CP_UserPinkbeanYoYoStack,
    CP_UserQuickMoveScript,
    CP_UserSelectAndroid,
    CP_UserCompleteNpcSpeech,
    CP_UserMobDropMesoPickup,
    CP_RequestEventList,
    CP_AddAttackReset,
    CP_UseFamiliarCard,
    BINGO,
    CP_CharacterBurning,
    CP_UpdateCharacterSelectList,
    CP_DirectGoToField,
    CP_UserUpdateMatrix,
    CP_PetMove,
    CP_PetAction,
    CP_PetInteractionRequest,
    CP_PetDropPickUpRequest,
    CP_PetStatChangeItemUseRequest,
    CP_PetUpdateExceptionListRequest,
    CP_PetFoodItemUseRequest,
    CP_SkillPetMove,
    CP_SkillPetAction,
    CP_SummonedMove,
    CP_SummonedAttack,
    CP_SummonedHit,
    CP_SummonedSkill,
    CP_SummonedAssistAttackDone,
    CP_Remove,
    CP_DragonMove,
    USE_ITEM_QUEST,
    CP_AndroidMove,
    CP_AndroidActionSet,
    UPDATE_QUEST,
    QUEST_ITEM,
    CP_FoxManMove,
    CP_FoxManActionSetUseRequest,
    CP_QuickslotKeyMappedModified,
    CP_PassiveskillInfoUpdate,
    CP_DirectionNodeCollision,
    CP_CheckProcess,
    CP_EgoEquipGaugeCompleteReturn,
    CP_EgoEquipCreateUpgradeItem,
    CP_EgoEquipCreateUpgradeItemCostRequest,
    CP_EgoEquipTalkRequest,
    CP_EgoEquipCheckUpdateItemRequest,
    CP_InheritanceInfoRequest,
    CP_InheritanceUpgradeRequest,
    CP_UserUpdateMapleTVShowTime,
    CP_RequestArrowPlaterObj,
    LP_GuildTransfer,
    LP_GuildTransfer2,
    DMG_FLAME,
    SHINING_STAR_WORLD,
    BOSS_LIST,
    BBS_OPERATION,
    EXIT_GAME,
    ORBITAL_FLAME,
    PAM_SONG,
    TRANSFORM_PLAYER,
    ATTACK_ON_TITAN_SELECT,
    ENTER_MTS,
    SOLOMON,
    GACH_EXP,
    CHRONOSPHERE,
    USE_FLASH_CUBE,
    SAVE_DAMAGE_SKIN,
    CHANGE_DAMAGE_SKIN,
    REMOVE_DAMAGE_SKIN,
    PSYCHIC_GREP_R,
    CANCEL_PSYCHIC_GREP_R,
    PSYCHIC_ATTACK_R,
    PSYCHIC_DAMAGE_R,
    PSYCHIC_ULTIMATE_R,
    CP_MobMove,
    CP_MobApplyCtrl,
    CP_MobTimeBombEnd,
    CP_MobLiftingEnd,
    CP_NpcMove,
    CP_DropPickUpRequest,
    CP_ReactorHit,
    CP_ReactorClick,
    CP_DecomposerRequest,
    UPDATE_ENV,
    CP_SnowBallHit,
    CP_SnowBallTouch,
    PLAYER_UPDATE,
    CP_PartyMemberCandidateRequest,
    CP_UrusPartyMemberCandidateRequest,
    CP_PartyCandidateRequest,
    CP_GatherRequest,
    CP_GatherEndNotice,
    CP_MakeEnterFieldPacketForQuickMove,
    CP_RuneStoneUseReq,
    CP_RuneStoneSkillReq,
    CP_CashShopChargeParamRequest,
    CP_CashShopQueryCashRequest,
    CP_CashShopCashItemRequest,
    CP_CashShopCheckCouponRequest,
    CP_CashShopMemberShopRequest,
    CP_CashShopCoodinationRequest,
    CP_CashShopCheckMileageRequest,
    CP_CheckSPWOnCreateNewCharacter,
    CP_GoldHammerRequest,
    CP_GoldHammerComplete,
    CP_PlatinumHammerRequest,
    CP_BattleRecordOnOffRequest,
    REWARD,
    EFFECT_SWITCH,
    UNKNOWN,
    USE_ABYSS_SCROLL,
    MONSTER_BOOK_DROPS,
    RSA_KEY,
    MAPLETV,
    CRASH_INFO,
    GUEST_LOGIN,
    TOS,
    VIEW_SERVERLIST,
    REDISPLAY_SERVERLIST,
    CHAR_SELECT_NO_PIC,
    AUTH_REQUEST,
    VIEW_REGISTER_PIC,
    VIEW_SELECT_PIC,
    CLIENT_FAILED,
    ENABLE_SPECIAL_CREATION,
    CREATE_SPECIAL_CHAR,
    AUTH_SECOND_PASSWORD,
    WRONG_PASSWORD,
    ENTER_FARM,
    CHANGE_CODEX_SET,
    CODEX_UNK,
    USE_NEBULITE,
    USE_ALIEN_SOCKET,
    USE_ALIEN_SOCKET_RESPONSE,
    USE_NEBULITE_FUSION,
    TOT_GUIDE,
    GET_BOOK_INFO,
    USE_FAMILIAR,
    SPAWN_FAMILIAR,
    RENAME_FAMILIAR,
    PET_BUFF,
    USE_TREASURE_CHEST,
    SOLOMON_EXP,
    NEW_YEAR_CARD,
    XMAS_SURPRISE,
    TWIN_DRAGON_EGG,
    YOUR_INFORMATION,
    FIND_FRIEND,
    PINKBEAN_CHOCO_OPEN,
    PINKBEAN_CHOCO_SUMMON,
    BUY_SILENT_CRUSADE,
    CASSANDRAS_COLLECTION,
    BUDDY_ADD,
    PVP_SUMMON,
    MOVE_FAMILIAR,
    TOUCH_FAMILIAR,
    ATTACK_FAMILIAR,
    REVEAL_FAMILIAR,
    FRIENDLY_DAMAGE,
    HYPNOTIZE_DMG,
    MOB_BOMB,
    MOB_NODE,
    DISPLAY_NODE,
    MONSTER_CARNIVAL,
    CLICK_REACTOR,
    CANDY_RANKING,
    COCONUT,
    SHIP_OBJECT,
    PLACE_FARM_OBJECT,
    FARM_SHOP_BUY,
    FARM_COMPLETE_QUEST,
    FARM_NAME,
    HARVEST_FARM_BUILDING,
    USE_FARM_ITEM,
    RENAME_MONSTER,
    NURTURE_MONSTER,
    EXIT_FARM,
    FARM_QUEST_CHECK,
    FARM_FIRST_ENTRY,
    PYRAMID_BUY_ITEM,
    CLASS_COMPETITION,
    MAGIC_WHEEL,
    BLACK_FRIDAY,
    RECEIVE_GIFT_EFFECT,
    UPDATE_RED_LEAF,
    CLICK_BINGO_CARD,
    PRESS_BINGO,
    DRESSUP_TIME,
    OS_INFORMATION,
    LUCKY_LOGOUT,
    MESSENGER_RANKING;

    private short code = 0x7FFE;
    private final boolean CheckState;

    private RecvPacketOpcode() {
        this.CheckState = true;
    }

    private RecvPacketOpcode(final boolean CheckState) {
        this.CheckState = CheckState;
    }

    @Override
    public void setValue(short code) {
        this.code = code;
    }

    @Override
    public final short getValue() {
        return code;
    }

    public final boolean NeedsChecking() {
        return CheckState;
    }

    public static String nameOf(short value) {
        for (RecvPacketOpcode header : RecvPacketOpcode.values()) {
            if (header.getValue() == value) {
                return header.name();
            }
        }
        return "UNKNOWN";
    }

    public static boolean isSpamHeader(RecvPacketOpcode header) {
        switch (header.name()) {
            case "CP_DUMMY_CODE":
//            case "CP_AliveAck":
            case "CP_NpcMove":
            case "CP_MobMove":
            case "CP_UserMove":
            case "CP_AndroidMove":
            case "CP_DragonMove":
            case "CP_SummonedMove":
            case "CP_PetMove":
//            case "CP_UserHit":
//            case "CP_BEGIN_SOCKET":
            case "CLIENT_START":
//            case "CP_MobApplyCtrl":
//            case "CP_UserQuestRequest":
            case "CP_UserChangeStatRequest":
            case "CP_FuncKeyMappedModified":
//            case "CP_UserPortalTeleportRequest":
            case "CP_SkillPetMove":
//            case "CP_UserMeleeAttack":
//            case "CP_UserShootAttack":
//            case "CP_UserRequestInstanceTable":
//            case "CP_UserMagicAttack":
//            case "CP_PassiveskillInfoUpdate":
//            case "CP_RequestIncCombo":
//            case "CP_UserHyperSkillUpRequest":
//            case "CP_UserHyperSkillResetRequset":
//            case "CP_UserDefaultWingItem":
            case "CP_CheckProcess":
//            case "CP_UserPinkbeanYoYoStack":
                return true;
            default:
                return false;
        }
    }

    public static void loadValues() {
        String fileName = "recvops.properties";
        Properties props = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(fileName);
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(fileInputStream, StringUtil.codeString(fileName)))) {
            props.load(br);
        } catch (IOException ex) {
            InputStream in = RecvPacketOpcode.class.getClassLoader().getResourceAsStream("properties/" + fileName);
            if (in == null) {
                System.out.println("錯誤: 未加載 " + fileName + " 檔案");
                return;
            }
            try {
                props.load(in);
                in.close();
            } catch (IOException e) {
                throw new RuntimeException("加載 " + fileName + " 檔案出錯", e);
            }
        }
        ExternalCodeTableGetter.populateValues(props, values());
    }

    static {
        loadValues();
    }
}
