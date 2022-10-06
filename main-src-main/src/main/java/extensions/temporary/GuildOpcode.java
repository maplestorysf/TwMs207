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
public enum GuildOpcode {

    GuildReq_LoadGuild(0x0), GuildReq_FindGuildByCid(0x1), GuildReq_FindGuildByGID(0x2), GuildReq_InputGuildName(
            0x3), GuildReq_CheckGuildName(0x4), GuildReq_CreateGuildAgree(0x5), GuildReq_CreateNewGuild(
            0x6), GuildReq_InviteGuild(0x7), GuildReq_JoinGuild(0x8), GuildReq_JoinGuildDirect(
            0x9), GuildReq_UpdateJoinState(0xA), GuildReq_WithdrawGuild(0xB), GuildReq_KickGuild(
            0xC), GuildReq_RemoveGuild(0xD), GuildReq_IncMaxMemberNum(
            0xE), GuildReq_ChangeLevel(0xF), GuildReq_ChangeJob(
            0x10), GuildReq_SetGuildName(0x11), GuildReq_SetGradeName(
            0x12), GuildReq_SetMemberGrade(0x13), GuildReq_SetMark(
            0x14), GuildReq_SetNotice(0x15), GuildReq_InputMark(
            0x16), GuildReq_CheckQuestWaiting(
            0x17), GuildReq_CheckQuestWaiting2(
            0x18), GuildReq_InsertQuestWaiting(
            0x19), GuildReq_CancelQuestWaiting(
            0x1A), GuildReq_RemoveQuestCompleteGuild(
            0x1B), GuildReq_IncPoint(
            0x1C), GuildReq_IncCommitment(
            0x1D), GuildReq_DecGGP(
            0x1E), GuildReq_DecIGP(
            0x1F), GuildReq_SetQuestTime(
            0x20), GuildReq_ShowGuildRanking(
            0x21), GuildReq_SetSkill(
            0x22), GuildReq_SkillLevelSetUp(
            0x23), GuildReq_ResetGuildBattleSkill(
            0x24), GuildReq_UseActiveSkill(
            0x25), GuildReq_UseADGuildSkill(
            0x26), GuildReq_ExtendSkill(
            0x27), GuildReq_ChangeGuildMaster(
            0x28), GuildReq_FromGuildMember_GuildSkillUse(
            0x29), GuildReq_SetGGP(
            0x2A), GuildReq_SetIGP(
            0x2B), GuildReq_BattleSkillOpen(
            0x2C), GuildReq_Search(
            0x2D), GuildReq_CreateNewGuild_Block(
            0x2E), GuildReq_CreateNewAlliance_Block(
            0x2F),
    // GuildReq_ChatN_FindGuildIDByCID [-] BY 198
    GuildRes_LoadGuild_Done(0x30), GuildRes_FindGuild_Done(0x31), GuildRes_CheckGuildName_Available(
            0x32), GuildRes_CheckGuildName_AlreadyUsed(0x33), GuildRes_CheckGuildName_Unknown(
            0x34), GuildRes_CreateGuildAgree_Reply(0x35), GuildRes_CreateGuildAgree_Unknown(
            0x36), GuildRes_CreateNewGuild_Done(0x37), GuildRes_CreateNewGuild_AlreayJoined(
            0x38), GuildRes_CreateNewGuild_GuildNameAlreayExist(
            0x39), GuildRes_CreateNewGuild_Beginner(
            0x3A), GuildRes_CreateNewGuild_Disagree(
            0x3B), GuildRes_CreateNewGuild_NotFullParty(
            0x3C), GuildRes_CreateNewGuild_Unknown(
            0x3D), GuildRes_JoinGuild_Done(
            0x3E), GuildRes_JoinGuild_AlreadyJoined(
            0x3F),
    // 0x40 [+] BY 198
    GuildRes_JoinGuild_AlreadyFull(0x41),
    // 0x42 [+] BY 198
    GuildRes_JoinGuild_UnknownUser(0x43), GuildRes_JoinGuild_NonRequestFindUser(0x44), GuildRes_JoinGuild_Unknown(
            0x45), GuildRes_JoinRequest_Done(0x46), GuildRes_JoinRequest_DoneToUser(
            0x47), GuildRes_JoinRequest_AlreadyFull(0x48), GuildRes_JoinRequest_LimitTime(
            0x49), GuildRes_JoinRequest_Unknown(0x4A), GuildRes_JoinCancelRequest_Done(
            0x4B), GuildRes_WithdrawGuild_Done(0x4C), GuildRes_WithdrawGuild_NotJoined(
            0x4D), GuildRes_WithdrawGuild_Unknown(0x4E), GuildRes_KickGuild_Done(
            0x4F), GuildRes_KickGuild_NotJoined(
            0x50), GuildRes_KickGuild_Unknown(
            0x51), GuildRes_RemoveGuild_Done(
            0x52), GuildRes_RemoveGuild_NotExist(
            0x53), GuildRes_RemoveGuild_Unknown(
            0x54), GuildRes_RemoveRequestGuild_Done(
            0x55), GuildRes_InviteGuild_BlockedUser(
            0x56), GuildRes_InviteGuild_AlreadyInvited(
            0x57), GuildRes_InviteGuild_Rejected(
            0x58), GuildRes_AdminCannotCreate(
            0x59), GuildRes_AdminCannotInvite(
            0x5A), GuildRes_IncMaxMemberNum_Done(
            0x5B), GuildRes_IncMaxMemberNum_Unknown(
            0x5C), GuildRes_ChangeMemberName(
            0x5D), GuildRes_ChangeRequestUserName(
            0x5E), GuildRes_ChangeLevelOrJob(
            0x5F), GuildRes_NotifyLoginOrLogout(
            0x50), GuildRes_SetGradeName_Done(
            0x61), GuildRes_SetGradeName_Unknown(
            0x62), GuildRes_SetMemberGrade_Done(
            0x63), GuildRes_SetMemberGrade_Unknown(
            0x64), GuildRes_SetMemberCommitment_Done(
            0x65), GuildRes_SetMark_Done(
            0x66), GuildRes_SetMark_Unknown(
            0x67), GuildRes_SetNotice_Done(
            0x68), GuildRes_InsertQuest(
            0x69), GuildRes_NoticeQuestWaitingOrder(
            0x6A), GuildRes_SetGuildCanEnterQuest(
            0x6B), GuildRes_IncPoint_Done(
            0x6C), GuildRes_ShowGuildRanking(
            0x6D), GuildRes_SetGGP_Done(
            0x6E), GuildRes_SetIGP_Done(
            0x6F), GuildRes_GuildQuest_NotEnoughUser(
            0x70), GuildRes_GuildQuest_RegisterDisconnected(
            0x71), GuildRes_GuildQuest_NoticeOrder(
            0x72), GuildRes_Authkey_Update(
            0x73), GuildRes_SetSkill_Done(
            0x74), GuildRes_SetSkill_Extend_Unknown(
            0x75), GuildRes_SetSkill_LevelSet_Unknown(
            0x76), GuildRes_SetSkill_ResetBattleSkill(
            0x77), GuildRes_UseSkill_Success(
            0x78), GuildRes_UseSkill_Err(
            0x79), GuildRes_ChangeName_Done(
            0x7A), GuildRes_ChangeName_Unknown(
            0x7B), GuildRes_ChangeMaster_Done(
            0x7C), GuildRes_ChangeMaster_Unknown(
            0x7D), GuildRes_BlockedBehaviorCreate(
            0x7E), GuildRes_BlockedBehaviorJoin(
            0x7F), GuildRes_BattleSkillOpen(
            0x80), GuildRes_GetData(
            0x81), GuildRes_Rank_Reflash(
            0x82), GuildRes_FindGuild_Error(
            0x83), GuildRes_ChangeMaster_Pinkbean(
            0x84),;

    private final int value;

    private GuildOpcode(int value) {
        this.value = value;
    }

    public static GuildOpcode getOpcode(int type) {
        for (GuildOpcode gro : values()) {
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
