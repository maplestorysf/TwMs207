/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.world;

public enum PartyOperation {

    PartyReq_LoadParty(0x0), PartyReq_CreateNewParty(0x1), PartyReq_WithdrawParty(0x2), PartyReq_JoinParty(0x3),
    // 隊伍邀請
    PartyReq_InviteParty(0x4), PartyReq_InviteIntrusion(0x5), PartyReq_KickParty(0x6), PartyReq_ChangePartyBoss(0x7),
    // 隊伍邀請回覆
    PartyReq_ApplyParty(0x8), PartyReq_SetAppliable(0x9), PartyReq_ClearIntrusion(0xA), PartyReq_CreateNewParty_Group(
            0xB), PartyReq_JoinParty_Group(0xC), PartyReq_PartySetting(0xD), PartyReq_LoadStarPlanetPoint(0xE),
    // 0xF
    // 0x10
    // 0x11
    // 0x12
    // 更新隊伍
    PartyRes_LoadParty_Done(0x13),
    // 更新隊員線上狀態
    LOG_ONOFF(0x013),
    // 建立隊伍
    PartyRes_CreateNewParty_Done(0x14), PartyRes_CreateNewParty_AlreayJoined(0x15), PartyRes_CreateNewParty_Beginner(
            0x16), PartyRes_CreateNewParty_Unknown(
            0x17), PartyRes_CreateNewParty_byNonBoss(0x18), PartyRes_WithdrawParty_Done(0x19),
    // 離開隊伍
    LEAVE(0x19),
    // 強制退出
    EXPEL(0x19),
    // 解散隊伍
    DISBAND(0x19), PartyRes_WithdrawParty_NotJoined(0x1A), PartyRes_WithdrawParty_Unknown(0x1B),
    // 加入隊伍
    PartyRes_JoinParty_Done(0x1C),
    // 已有隊伍
    PartyRes_JoinParty_Done2(0x1D), PartyRes_JoinParty_AlreadyJoined(0x1E),
    // 組隊成員已滿
    PartyRes_JoinParty_AlreadyFull(0x1F), PartyRes_JoinParty_OverDesiredSize(0x20), PartyRes_JoinParty_UnknownUser(
            0x21), PartyRes_JoinParty_Unknown(
            0x22), PartyRes_JoinIntrusion_Done(0x23), PartyRes_JoinIntrusion_UnknownParty(0x24),
    // 邀請'(null)'加入組隊。
    PartyRes_InviteParty_Sent(0x25), PartyRes_InviteParty_BlockedUser(0x26), PartyRes_InviteParty_AlreadyInvited(
            0x27), PartyRes_InviteParty_AlreadyInvitedByInviter(0x28), PartyRes_InviteParty_Rejected(
            0x29), PartyRes_InviteParty_Accepted(0x2A), PartyRes_InviteIntrusion_Sent(
            0x2B), PartyRes_InviteIntrusion_BlockedUser(0x2C), PartyRes_InviteIntrusion_AlreadyInvited(
            0x2D), PartyRes_InviteIntrusion_AlreadyInvitedByInviter(
            0x2E), PartyRes_InviteIntrusion_Rejected(
            0x2F), PartyRes_InviteIntrusion_Accepted(
            0x30), PartyRes_KickParty_Done(
            0x31), PartyRes_KickParty_FieldLimit(
            0x32), PartyRes_KickParty_Unknown(
            0x33), PartyRes_Unknown_34(0x34),
    // 委任隊長
    PartyRes_ChangePartyBoss_Done(0x35),
    // 委任隊長(斷線)
    PartyRes_ChangePartyBoss_Done_DC(0x35), PartyRes_ChangePartyBoss_NotSameField(
            0x36), PartyRes_ChangePartyBoss_NoMemberInSameField(0x37), PartyRes_ChangePartyBoss_NotSameChannel(
            0x38), PartyRes_ChangePartyBoss_Unknown(0x39), PartyRes_AdminCannotCreate(
            0x3A), PartyRes_AdminCannotInvite(0x3B), PartyRes_InAnotherWorld(0x3C),
    // 找不到玩家
    PartyRes_InAnotherChanelBlockedUser(0x3D), PartyRes_UserMigration(0x3E), PartyRes_ChangeLevelOrJob(
            0x3F), PartyRes_Unknown_40(0x40), PartyRes_UpdateShutdownStatus(0x41), PartyRes_SetAppliable(
            0x42), PartyRes_SetAppliableFailed(0x43), PartyRes_SuccessToSelectPQReward(
            0x44), PartyRes_FailToSelectPQReward(0x45), PartyRes_ReceivePQReward(
            0x46), PartyRes_FailToRequestPQReward(0x47), PartyRes_CanNotInThisField(
            0x48), PartyRes_ApplyParty_Sent(0x49), PartyRes_ApplyParty_UnknownParty(
            0x4A), PartyRes_ApplyParty_BlockedUser(
            0x4B), PartyRes_ApplyParty_AlreadyApplied(
            0x4C), PartyRes_ApplyParty_AlreadyAppliedByApplier(
            0x4D), PartyRes_ApplyParty_AlreadyFull(
            0x4E), PartyRes_ApplyParty_Rejected(
            0x4F), PartyRes_ApplyParty_Accepted(
            0x50), PartyRes_FoundPossibleMember(
            0x51), PartyRes_FoundPossibleParty(
            0x52),
    // 更新訊息
    PartyRes_PartySettingDone(0x53), PartyRes_Load_StarGrade_Result(0x54), PartyRes_Load_StarGrade_Result2(
            0x55), PartyRes_Member_Rename(0x56), PartyRes_Unknown_57(0x57), PartyRes_Unknown_58(
            0x58), PartyRes_Unknown_59(0x59), PartyRes_Unknown_5A(0x5A), PartyInfo_TownPortalChanged(
            0x5B), PartyInfo_OpenGate(0x5C), ExpeditionReq_Load(0x5D), ExpeditionReq_CreateNew(
            0x5E), ExpeditionReq_Invite(0x5F), ExpeditionReq_ResponseInvite(
            0x60), ExpeditionReq_Withdraw(0x61), ExpeditionReq_Kick(
            0x62), ExpeditionReq_ChangeMaster(
            0x63), ExpeditionReq_ChangePartyBoss(
            0x64), ExpeditionReq_RelocateMember(
            0x65), ExpeditionNoti_Load_Done(
            0x66), ExpeditionNoti_Load_Fail(
            0x67), ExpeditionNoti_CreateNew_Done(
            0x68), ExpeditionNoti_Join_Done(
            0x69), ExpeditionNoti_You_Joined(
            0x6A), ExpeditionNoti_You_Joined2(
            0x6B), ExpeditionNoti_Join_Fail(
            0x6C), ExpeditionNoti_Withdraw_Done(
            0x6D), ExpeditionNoti_You_Withdrew(
            0x6E), ExpeditionNoti_Kick_Done(
            0x6F), ExpeditionNoti_You_Kicked(
            0x70), ExpeditionNoti_Removed(
            0x71), ExpeditionNoti_MasterChanged(
            0x72), ExpeditionNoti_Modified(
            0x73), ExpeditionNoti_Modified2(
            0x74), ExpeditionNoti_Invite(
            0x75), ExpeditionNoti_ResponseInvite(
            0x76), ExpeditionNoti_Create_Fail_By_Over_Weekly_Counter(
            0x77), ExpeditionNoti_Invite_Fail_By_Over_Weekly_Counter(
            0x78), ExpeditionNoti_Apply_Fail_By_Over_Weekly_Counter(
            0x79), ExpeditionNoti_Invite_Fail_By_Blocked_Behavior(
            0x7A), AdverNoti_LoadDone(
            0x7B), AdverNoti_Change(
            0x7C), AdverNoti_Remove(
            0x7D), AdverNoti_GetAll(
            0x7E), AdverNoti_Apply(
            0x7F), AdverNoti_ResultApply(
            0x80), AdverNoti_AddFail(
            0x81), AdverReq_Add(
            0x82), AdverReq_Remove(
            0x83), AdverReq_GetAll(
            0x84), AdverReq_RemoveUserFromNotiList(
            0x85), AdverReq_Apply(
            0x86), AdverReq_ResultApply(
            0x87), PP_NO(
            0x88),;

    private int code;

    private PartyOperation(int code) {
        this.code = code;
    }

    public static PartyOperation getOpcode(int type) {
        for (PartyOperation pop : values()) {
            if (pop.getValue() == type) {
                return pop;
            }
        }
        return null;
    }

    public int getValue() {
        return code;
    }
}
