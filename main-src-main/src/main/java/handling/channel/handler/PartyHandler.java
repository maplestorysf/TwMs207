package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.ExpeditionType;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import server.maps.MulungSystem;
import server.maps.FieldLimitType;
import server.quest.MapleQuest;
import tools.StringUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;

public class PartyHandler {

    public static final void DenyPartyRequest(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getParty() == null
                && c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.組隊邀請)) == null) {
            PartyOperation op = PartyOperation.getOpcode(slea.readByte());
            int fromId = slea.readInt();
            MapleParty party = World.Party.getPartyByLeaderId(fromId);
            if (party != null) {
                if (party.getExpeditionId() > 0) {
                    c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態下無法進行此操作。");
                    return;
                }
                switch (op) {
                    case PartyRes_InviteParty_AlreadyInvited: // Version.198 [推測]
                    case PartyRes_InviteParty_AlreadyInvitedByInviter: // 組隊邀請 / Version.198 [完成]
                        break;
                    case PartyRes_InviteParty_Rejected: // 拒絕組隊邀請 / Version.198 [完成]
                        // case 0x3B: // Version.176 [推測]
                        MapleCharacter cfrom = c.getChannelServer().getPlayerStorage()
                                .getCharacterById(party.getLeader().getId());
                        if (cfrom == null) {
                            break;
                        }
                        cfrom.dropMessage(5, new StringBuilder().append("'").append(c.getPlayer().getName())
                                .append("'玩家拒絕了組隊邀請。").toString());
                        break;
                    case PartyRes_InviteParty_Accepted: // 接受組隊邀請 / Version.198 [完成]
                        // case 0x3C: // Version.176 [推測]
                        if (party.getMembers().size() < 6) {
                            c.getPlayer().setParty(party);
                            World.Party.updateParty(party.getId(), PartyOperation.PartyRes_JoinParty_Done,
                                    new MaplePartyCharacter(c.getPlayer()));
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else {
                            c.getPlayer().dropMessage(5, "組隊成員已滿");
                        }
                        break;
                    default:
                        System.out.println(new StringBuilder()
                                .append("第二方收到組隊邀請處理( 0x").append(StringUtil
                                .getLeftPaddedStr(Integer.toHexString(op.getValue()).toUpperCase(), '0', 2))
                                .append(" ) 未知.").toString());
                }
            } else {
                c.getPlayer().dropMessage(5, "要參加的隊伍不存在。");
            }
        } else {
            PartyOperation op = PartyOperation.getOpcode(slea.readByte());
            int charId = slea.readInt();
            MapleCharacter cfrom;
            MapleParty party = World.Party.getParty(c.getPlayer().getParty().getId());
            switch (op) {
                case PartyRes_InviteParty_Accepted: // Version.198 [推測]
                    c.getPlayer().dropMessage(5, "您已經有一個組隊，無法加入其它組隊!");
                    break;
                // case 0x43: // Version.176 [推測]
                // break;
                case PartyRes_ApplyParty_Rejected: // Version.198 [推測]
                    cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(charId);
                    if (cfrom == null) {
                        break;
                    }
                    cfrom.dropMessage(5,
                            new StringBuilder().append(c.getPlayer().getName()).append("拒絕了組隊加入申請。").toString());
                    break;
                case PartyRes_ApplyParty_Accepted: // Version.198 [推測]
                    cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(charId);
                    if (cfrom == null) {
                        break;
                    }
                    if (party.getMembers().size() < 6) {
                        cfrom.setParty(party);
                        World.Party.updateParty(party.getId(), PartyOperation.PartyRes_JoinParty_Done,
                                new MaplePartyCharacter(cfrom));
                        cfrom.receivePartyMemberHP();
                        cfrom.updatePartyMemberHP();
                    } else {
                        c.getPlayer().dropMessage(5, "隊伍成員已滿。");
                        cfrom.dropMessage(5, "隊伍成員已滿。");
                    }
                    break;
                default:
                    System.out.println(new StringBuilder().append("第二方收到申請加入組隊處理( 0x")
                            .append(StringUtil.getLeftPaddedStr(Integer.toHexString(op.getValue()).toUpperCase(), '0', 2))
                            .append(" ) 未知.").toString());
            }
        }
    }

    public static final void PartyOperation(LittleEndianAccessor slea, MapleClient c) {
        PartyOperation operation = PartyOperation.getOpcode(slea.readByte());
        MapleParty party = c.getPlayer().getParty();
        MaplePartyCharacter partyPlayer = new MaplePartyCharacter(c.getPlayer());
        switch (operation) {
            case PartyReq_CreateNewParty: // 建立隊伍
                if (party == null) {
                    boolean privateParty = slea.readByte() == 0;
                    String partyName = slea.readMapleAsciiString();
                    party = World.Party.createParty(partyPlayer, privateParty, partyName);
                    c.getPlayer().setParty(party);
                    c.getSession().writeAndFlush(CWvsContext.PartyPacket.partyCreated(party));
                } else {
                    if (party.getExpeditionId() > 0) {
                        c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態無法進行此操作。");
                        return;
                    }
                    if ((partyPlayer.equals(party.getLeader())) && (party.getMembers().size() == 1)) {
                        c.getSession().writeAndFlush(CWvsContext.PartyPacket.partyCreated(party));
                    } else {
                        c.getPlayer().dropMessage(5, "你已經存在一個隊伍中，無法創建！");
                    }
                }
                break;
            case PartyReq_WithdrawParty: // 解散隊伍 / 離開隊伍
                if (party == null) {
                    break;
                }
                if (party.getExpeditionId() > 0) {
                    c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態無法進行此操作。");
                    return;
                }
                if (partyPlayer.equals(party.getLeader())) {
                    if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                        MulungSystem.failed(c.getPlayer());
                    }
                    if (c.getPlayer().getPyramidSubway() != null) {
                        c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                    }
                    World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyPlayer);
                    if (c.getPlayer().getEventInstance() != null) {
                        c.getPlayer().getEventInstance().disbandParty();
                    }
                } else {
                    if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                        MulungSystem.failed(c.getPlayer());
                    }
                    if (c.getPlayer().getPyramidSubway() != null) {
                        c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                    }
                    World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyPlayer);
                    if (c.getPlayer().getEventInstance() != null) {
                        c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                    }
                }
                c.getPlayer().setParty(null);
                break;
            case PartyReq_JoinParty: // 加入隊伍
                int partyid = slea.readInt();
                if (party == null) {
                    party = World.Party.getParty(partyid);
                    if (party != null) {
                        if (party.getExpeditionId() > 0) {
                            c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態無法進行此操作。");
                            return;
                        }
                        if ((party.getMembers().size() < 6)
                                && (c.getPlayer().getQuestNoAdd(MapleQuest.getInstance(GameConstants.組隊邀請)) == null)) {
                            c.getPlayer().setParty(party);
                            World.Party.updateParty(party.getId(), PartyOperation.PartyRes_JoinParty_Done, partyPlayer);
                            c.getPlayer().receivePartyMemberHP();
                            c.getPlayer().updatePartyMemberHP();
                        } else { // 組隊成員已滿
                            c.getSession().writeAndFlush(CWvsContext.PartyPacket
                                    .partyStatusMessage(PartyOperation.PartyRes_JoinParty_AlreadyFull));
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "要加入的隊伍不存在。");
                    }
                } else {
                    c.getPlayer().dropMessage(5, "你已經有一個隊伍，無法加入其它隊伍！");
                }
                break;
            case PartyReq_InviteParty: // 隊伍邀請
                if (party == null) {
                    party = World.Party.createParty(partyPlayer, false, c.getPlayer().getName() + "的隊伍");
                    c.getPlayer().setParty(party);
                    c.getSession().writeAndFlush(CWvsContext.PartyPacket.partyCreated(party));
                }
                String theName = slea.readMapleAsciiString();
                int theCh = World.Find.findChannel(theName);
                if (theCh > 0) {
                    MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage()
                            .getCharacterByName(theName);
                    if ((invited != null) && (invited.getParty() == null)
                            && (invited.getQuestNoAdd(MapleQuest.getInstance(GameConstants.組隊邀請)) == null)) {
                        if (party.getExpeditionId() > 0) {
                            c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態無法進行此操作。");
                            return;
                        }
                        if (party.getMembers().size() < 6) {
                            c.getSession().writeAndFlush(CWvsContext.PartyPacket
                                    .partyStatusMessage(PartyOperation.PartyRes_InviteParty_Sent, invited.getName()));
                            invited.getClient().getSession()
                                    .writeAndFlush(CWvsContext.PartyPacket.partyInvite(c.getPlayer()));
                        } else {
                            c.getSession().writeAndFlush(CWvsContext.PartyPacket
                                    .partyStatusMessage(PartyOperation.PartyRes_JoinParty_AlreadyFull));
                        }
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.PartyPacket
                                .partyStatusMessage(PartyOperation.PartyRes_JoinParty_AlreadyJoined));
                    }
                } else {
                    c.getSession().writeAndFlush(
                            CWvsContext.PartyPacket.partyStatusMessage(PartyOperation.PartyRes_InAnotherChanelBlockedUser));
                }
                break;
            case PartyReq_KickParty: // 強制退出
                if (party == null || !partyPlayer.equals(party.getLeader())) {
                    break;
                }
                if (party.getExpeditionId() > 0) {
                    c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態無法進行此操作。");
                    return;
                }
                MaplePartyCharacter expelled = party.getMemberById(slea.readInt());
                if (expelled != null) {
                    if ((GameConstants.isDojo(c.getPlayer().getMapId())) && (expelled.isOnline())) {
                        MulungSystem.failed(c.getPlayer());
                    }
                    if ((c.getPlayer().getPyramidSubway() != null) && (expelled.isOnline())) {
                        c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                    }
                    World.Party.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                    if (c.getPlayer().getEventInstance() != null) {
                        if (expelled.isOnline()) {
                            c.getPlayer().getEventInstance().disbandParty();
                        }
                    }
                }
                break;
            case PartyReq_ChangePartyBoss: // 變更隊長
                if (party == null) {
                    break;
                }
                if (party.getExpeditionId() > 0) {
                    c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態無法進行此操作。");
                    return;
                }
                MaplePartyCharacter newleader = party.getMemberById(slea.readInt());
                if ((newleader != null) && (partyPlayer.equals(party.getLeader()))) {
                    World.Party.updateParty(party.getId(), PartyOperation.PartyRes_ChangePartyBoss_Done, newleader);
                }
                break;
            case PartyReq_ApplyParty: // 申請隊伍
                if (party != null) {
                    if ((c.getPlayer().getEventInstance() != null) || (c.getPlayer().getPyramidSubway() != null)
                            || (party.getExpeditionId() > 0) || (GameConstants.isDojo(c.getPlayer().getMapId()))) {
                        c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態無法進行此操作。");
                        return;
                    }
                    if (partyPlayer.equals(party.getLeader())) {
                        World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyPlayer);
                    } else {
                        World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyPlayer);
                    }
                    c.getPlayer().setParty(null);
                }
                int partyid_ = slea.readInt();
                party = World.Party.getParty(partyid_);
                if ((party == null) || (party.getMembers().size() >= 6)) {
                    break;
                }
                if (party.getExpeditionId() > 0) {
                    c.getPlayer().dropMessage(5, "加入遠征隊伍的狀態無法進行此操作。");
                    return;
                }
                MapleCharacter cfrom = c.getPlayer().getMap().getCharacterById(party.getLeader().getId());
                if ((cfrom != null) && (cfrom.getQuestNoAdd(MapleQuest.getInstance(GameConstants.組隊請求)) == null)) {
                    c.getSession().writeAndFlush(CWvsContext.PartyPacket
                            .partyStatusMessage(PartyOperation.PartyRes_ApplyParty_Sent, c.getPlayer().getName()));
                    cfrom.getClient().getSession().writeAndFlush(CWvsContext.PartyPacket.partyRequestInvite(c.getPlayer()));
                } else {
                    c.getPlayer().dropMessage(5, "沒有在該地圖找到此隊伍的隊長。");
                }
                break;
            case PartyReq_SetAppliable:
                if (slea.readByte() > 0) {
                    c.getPlayer().getQuestRemove(MapleQuest.getInstance(GameConstants.組隊請求));
                } else {
                    c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.組隊請求));
                }
                break;
            case PartyReq_PartySetting: // 變更隊伍設定
                if (party == null) {
                    break;
                }
                boolean privateParty = slea.readByte() == 0;
                String partyName = slea.readMapleAsciiString();
                c.getPlayer().getParty().setName(partyName);
                c.getPlayer().getParty().setPrivate(privateParty);
                World.Party.updateParty(c.getPlayer().getParty().getId(), PartyOperation.PartyRes_PartySettingDone,
                        partyPlayer);
                break;
            default:
                System.out.println("隊伍操作處理, 操作碼(" + operation + ")未處理。");
                break;
        }
    }

    public static void AllowPartyInvite(LittleEndianAccessor slea, MapleClient c) {
        if (slea.readByte() > 0) {
            c.getPlayer().getQuestRemove(MapleQuest.getInstance(GameConstants.組隊邀請));
        } else {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.組隊邀請));
        }
    }

    public static void MemberSearch(LittleEndianAccessor slea, MapleClient c) {
        if ((c.getPlayer().isInBlockedMap())
                || (FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()))) {
            c.getPlayer().dropMessage(5, "無法在這個地方進行搜尋。");
            return;
        }
        List<MapleCharacter> members = new ArrayList<>();
        for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
            if (chr.getId() != c.getPlayer().getId() && chr.getParty() == null
                    && chr.getGmLevel() <= c.getPlayer().getGmLevel()) {
                members.add(chr);
            }
        }
        c.getSession().writeAndFlush(CWvsContext.PartyPacket.showMemberSearch(members));
    }

    public static final void PartySearch(LittleEndianAccessor slea, MapleClient c) {
        if ((c.getPlayer().isInBlockedMap())
                || (FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit()))) {
            c.getPlayer().dropMessage(5, "無法在這個地方進行搜尋。");
            return;
        }
        List<MapleParty> parties = new ArrayList<>();
        for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
            if (chr == null) {
                continue;
            }
            if (chr.getParty() == null) {
                continue;
            }
            if (chr.getParty().isPrivate()) {
                continue;
            }
            if (parties.contains(chr.getParty())) {
                continue;
            }
            if (c.getPlayer().getParty() != null && chr.getParty().getId() == c.getPlayer().getParty().getId()) {
                continue;
            }
            parties.add(chr.getParty());
        }

        c.getSession().writeAndFlush(CWvsContext.PartyPacket.showPartySearch(parties));
    }

    public static final void PartyListing(LittleEndianAccessor slea, MapleClient c) {
        int mode = slea.readByte();
        PartySearchType pst;
        switch (mode) {
            case -105:
            case -97:
            case 81:
            case 159:
                pst = PartySearchType.getById(slea.readInt());
                if ((pst == null) || (c.getPlayer().getLevel() > pst.maxLevel)
                        || (c.getPlayer().getLevel() < pst.minLevel)) {
                    return;
                }
                if ((c.getPlayer().getParty() == null) && (World.Party.searchParty(pst).size() < 10)) {
                    MapleParty party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()), pst.id);
                    c.getPlayer().setParty(party);
                    c.getSession().writeAndFlush(CWvsContext.PartyPacket.partyCreated(party));
                    PartySearch ps = new PartySearch(slea.readMapleAsciiString(),
                            pst.exped ? party.getExpeditionId() : party.getId(), pst);
                    World.Party.addSearch(ps);
                    if (pst.exped) {
                        c.getSession().writeAndFlush(CWvsContext.ExpeditionPacket
                                .expeditionStatus(World.Party.getExped(party.getExpeditionId()), true, false));
                    }
                    c.getSession().writeAndFlush(CWvsContext.PartyPacket.partyListingAdded(ps));
                } else {
                    c.getPlayer().dropMessage(1, "Unable to create. Please leave the party.");
                }
                break;
            case -103:
            case -95:
            case 83:
            case 161:
                pst = PartySearchType.getById(slea.readInt());
                if ((pst == null) || (c.getPlayer().getLevel() > pst.maxLevel)
                        || (c.getPlayer().getLevel() < pst.minLevel)) {
                    return;
                }
                c.getSession().writeAndFlush(CWvsContext.PartyPacket.getPartyListing(pst));
                break;
            case -102:
            case -94:
            case 84:
            case 162:
                break;
            case -101:
            case -93:
            case 85:
            case 163:
                MapleParty party = c.getPlayer().getParty();
                MaplePartyCharacter partyplayer = new MaplePartyCharacter(c.getPlayer());
                if (party != null) {
                    break;
                }
                int theId = slea.readInt();
                party = World.Party.getParty(theId);
                if (party != null) {
                    PartySearch ps = World.Party.getSearchByParty(party.getId());
                    if ((ps != null) && (c.getPlayer().getLevel() <= ps.getType().maxLevel)
                            && (c.getPlayer().getLevel() >= ps.getType().minLevel) && (party.getMembers().size() < 6)) {
                        c.getPlayer().setParty(party);
                        World.Party.updateParty(party.getId(), PartyOperation.PartyRes_JoinParty_Done, partyplayer);
                        c.getPlayer().receivePartyMemberHP();
                        c.getPlayer().updatePartyMemberHP();
                    } else {
                        c.getSession().writeAndFlush(
                                CWvsContext.PartyPacket.partyStatusMessage(PartyOperation.PartyRes_WithdrawParty_Unknown));
                    }
                } else {
                    MapleExpedition exped = World.Party.getExped(theId);
                    if (exped != null) {
                        PartySearch ps = World.Party.getSearchByExped(exped.getId());
                        if ((ps != null) && (c.getPlayer().getLevel() <= ps.getType().maxLevel)
                                && (c.getPlayer().getLevel() >= ps.getType().minLevel)
                                && (exped.getAllMembers() < exped.getType().maxMembers)) {
                            int partyId = exped.getFreeParty();
                            if (partyId < 0) {
                                c.getSession()
                                        .writeAndFlush(CWvsContext.PartyPacket.partyStatusMessage(PartyOperation.DISBAND));
                            } else if (partyId == 0) {
                                party = World.Party.createPartyAndAdd(partyplayer, exped.getId());
                                c.getPlayer().setParty(party);
                                c.getSession().writeAndFlush(CWvsContext.PartyPacket.partyCreated(party));
                                c.getSession()
                                        .writeAndFlush(CWvsContext.ExpeditionPacket.expeditionStatus(exped, true, false));
                                World.Party.expedPacket(exped.getId(),
                                        CWvsContext.ExpeditionPacket.expeditionJoined(c.getPlayer().getName()), null);
                                World.Party.expedPacket(exped.getId(),
                                        CWvsContext.ExpeditionPacket.expeditionUpdate(exped.getIndex(party.getId()), party),
                                        null);
                            } else {
                                c.getPlayer().setParty(World.Party.getParty(partyId));
                                World.Party.updateParty(partyId, PartyOperation.PartyRes_JoinParty_Done, partyplayer);
                                c.getPlayer().receivePartyMemberHP();
                                c.getPlayer().updatePartyMemberHP();
                                c.getSession()
                                        .writeAndFlush(CWvsContext.ExpeditionPacket.expeditionStatus(exped, true, false));
                                World.Party.expedPacket(exped.getId(),
                                        CWvsContext.ExpeditionPacket.expeditionJoined(c.getPlayer().getName()), null);
                            }
                        } else {
                            c.getSession().writeAndFlush(
                                    CWvsContext.ExpeditionPacket.expeditionError(0, c.getPlayer().getName()));
                        }
                    }
                }
                break;
            default:
                if (!c.getPlayer().isShowErr()) {
                    System.out.println("Unknown PartyListing : " + mode + "\n" + slea);
                }
        }
    }

    public static final void Expedition(LittleEndianAccessor slea, MapleClient c) {
        if ((c.getPlayer() == null) || (c.getPlayer().getMap() == null)) {
            return;
        }
        int mode = slea.readByte();
        String name;
        MapleParty part;
        MapleExpedition exped;
        int cid;
        Iterator i$;

        switch (mode) {
            case 76:// 64
            case 134:
                ExpeditionType et = ExpeditionType.getById(slea.readInt());
                if ((et != null) && (c.getPlayer().getParty() == null) && (c.getPlayer().getLevel() <= et.maxLevel)
                        && (c.getPlayer().getLevel() >= et.minLevel)) {
                    MapleParty party = World.Party.createParty(new MaplePartyCharacter(c.getPlayer()), et.exped);
                    c.getPlayer().setParty(party);
                    c.getSession().writeAndFlush(CWvsContext.PartyPacket.partyCreated(party));
                    c.getSession().writeAndFlush(CWvsContext.ExpeditionPacket
                            .expeditionStatus(World.Party.getExped(party.getExpeditionId()), true, false));
                } else {
                    c.getSession().writeAndFlush(CWvsContext.ExpeditionPacket.expeditionError(0, ""));
                }
                break;
            case 77:// 65
            case 135:
                name = slea.readMapleAsciiString();
                int theCh = World.Find.findChannel(name);
                if (theCh > 0) {
                    MapleCharacter invited = ChannelServer.getInstance(theCh).getPlayerStorage().getCharacterByName(name);
                    MapleParty party = c.getPlayer().getParty();
                    if ((invited != null) && (invited.getParty() == null) && (party != null)
                            && (party.getExpeditionId() > 0)) {
                        MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                        if ((me != null) && (me.getAllMembers() < me.getType().maxMembers)
                                && (invited.getLevel() <= me.getType().maxLevel)
                                && (invited.getLevel() >= me.getType().minLevel)) {
                            c.getSession()
                                    .writeAndFlush(CWvsContext.ExpeditionPacket.expeditionError(7, invited.getName()));
                            invited.getClient().getSession().writeAndFlush(
                                    CWvsContext.ExpeditionPacket.expeditionInvite(c.getPlayer(), me.getType().exped));
                        } else {
                            c.getSession()
                                    .writeAndFlush(CWvsContext.ExpeditionPacket.expeditionError(3, invited.getName()));
                        }
                    } else {
                        c.getSession().writeAndFlush(CWvsContext.ExpeditionPacket.expeditionError(2, name));
                    }
                } else {
                    c.getSession().writeAndFlush(CWvsContext.ExpeditionPacket.expeditionError(0, name));
                }
                break;
            case 78:// 66
            case 136:
                name = slea.readMapleAsciiString();
                int action = slea.readInt();
                int theChh = World.Find.findChannel(name);
                if (theChh <= 0) {
                    break;
                }
                MapleCharacter cfrom = ChannelServer.getInstance(theChh).getPlayerStorage().getCharacterByName(name);
                if ((cfrom != null) && (cfrom.getParty() != null) && (cfrom.getParty().getExpeditionId() > 0)) {
                    MapleParty party = cfrom.getParty();
                    exped = World.Party.getExped(party.getExpeditionId());
                    if ((exped != null) && (action == 8)) {
                        if ((c.getPlayer().getLevel() <= exped.getType().maxLevel)
                                && (c.getPlayer().getLevel() >= exped.getType().minLevel)
                                && (exped.getAllMembers() < exped.getType().maxMembers)) {
                            int partyId = exped.getFreeParty();
                            if (partyId < 0) {
                                c.getSession().writeAndFlush(CWvsContext.PartyPacket
                                        .partyStatusMessage(PartyOperation.PartyRes_WithdrawParty_Unknown));
                            } else if (partyId == 0) {
                                party = World.Party.createPartyAndAdd(new MaplePartyCharacter(c.getPlayer()),
                                        exped.getId());
                                c.getPlayer().setParty(party);
                                c.getSession().writeAndFlush(CWvsContext.PartyPacket.partyCreated(party));
                                c.getSession()
                                        .writeAndFlush(CWvsContext.ExpeditionPacket.expeditionStatus(exped, true, false));
                                World.Party.expedPacket(exped.getId(),
                                        CWvsContext.ExpeditionPacket.expeditionJoined(c.getPlayer().getName()), null);
                                World.Party.expedPacket(exped.getId(),
                                        CWvsContext.ExpeditionPacket.expeditionUpdate(exped.getIndex(party.getId()), party),
                                        null);
                            } else {
                                c.getPlayer().setParty(World.Party.getParty(partyId));
                                World.Party.updateParty(partyId, PartyOperation.PartyRes_JoinParty_Done,
                                        new MaplePartyCharacter(c.getPlayer()));
                                c.getPlayer().receivePartyMemberHP();
                                c.getPlayer().updatePartyMemberHP();
                                c.getSession()
                                        .writeAndFlush(CWvsContext.ExpeditionPacket.expeditionStatus(exped, false, false));
                                World.Party.expedPacket(exped.getId(),
                                        CWvsContext.ExpeditionPacket.expeditionJoined(c.getPlayer().getName()), null);
                            }
                        } else {
                            c.getSession().writeAndFlush(CWvsContext.ExpeditionPacket.expeditionError(3, cfrom.getName()));
                        }
                    } else if (action == 9) {
                        cfrom.getClient().getSession().writeAndFlush(CWvsContext.PartyPacket
                                .partyStatusMessage(PartyOperation.PartyRes_JoinParty_Done2, c.getPlayer().getName()));// 0x17+2
                        // 175
                    }
                }
                break;
            case 79:// 67
            case 137:
                part = c.getPlayer().getParty();
                if ((part == null) || (part.getExpeditionId() <= 0)) {
                    break;
                }
                exped = World.Party.getExped(part.getExpeditionId());
                if (exped != null) {
                    if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                        MulungSystem.failed(c.getPlayer());
                    }
                    if (exped.getLeader() == c.getPlayer().getId()) {
                        World.Party.disbandExped(exped.getId());
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().disbandParty();
                        }
                    } else if (part.getLeader().getId() == c.getPlayer().getId()) {
                        World.Party.updateParty(part.getId(), PartyOperation.DISBAND,
                                new MaplePartyCharacter(c.getPlayer()));
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().disbandParty();
                        }
                        World.Party.expedPacket(exped.getId(),
                                CWvsContext.ExpeditionPacket.expeditionLeft(c.getPlayer().getName()), null);
                    } else {
                        World.Party.updateParty(part.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(c.getPlayer()));
                        if (c.getPlayer().getEventInstance() != null) {
                            c.getPlayer().getEventInstance().leftParty(c.getPlayer());
                        }
                        World.Party.expedPacket(exped.getId(),
                                CWvsContext.ExpeditionPacket.expeditionLeft(c.getPlayer().getName()), null);
                    }
                    if (c.getPlayer().getPyramidSubway() != null) {
                        c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                    }
                    c.getPlayer().setParty(null);
                }
                break;
            case 80:// 68
            case 138:
                part = c.getPlayer().getParty();
                if ((part == null) || (part.getExpeditionId() <= 0)) {
                    break;
                }
                exped = World.Party.getExped(part.getExpeditionId());
                if ((exped != null) && (exped.getLeader() == c.getPlayer().getId())) {
                    cid = slea.readInt();
                    for (i$ = exped.getParties().iterator(); i$.hasNext();) {
                        int i = ((Integer) i$.next());
                        MapleParty par = World.Party.getParty(i);
                        if (par != null) {
                            MaplePartyCharacter expelled = par.getMemberById(cid);
                            if (expelled != null) {
                                if ((expelled.isOnline()) && (GameConstants.isDojo(c.getPlayer().getMapId()))) {
                                    MulungSystem.failed(c.getPlayer());
                                }
                                World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                if ((c.getPlayer().getEventInstance() != null) && (expelled.isOnline())) {
                                    c.getPlayer().getEventInstance().disbandParty();
                                }

                                if ((c.getPlayer().getPyramidSubway() != null) && (expelled.isOnline())) {
                                    c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                                }
                                World.Party.expedPacket(exped.getId(),
                                        CWvsContext.ExpeditionPacket.expeditionLeft(expelled.getName()), null);
                                break;
                            }
                        }
                    }
                }
                break;
            case 81:// 69
            case 139:
                part = c.getPlayer().getParty();
                if ((part == null) || (part.getExpeditionId() <= 0)) {
                    break;
                }
                exped = World.Party.getExped(part.getExpeditionId());
                if ((exped != null) && (exped.getLeader() == c.getPlayer().getId())) {
                    MaplePartyCharacter newleader = part.getMemberById(slea.readInt());
                    if (newleader != null) {
                        World.Party.updateParty(part.getId(), PartyOperation.PartyRes_ChangePartyBoss_Done, newleader);
                        exped.setLeader(newleader.getId());
                        World.Party.expedPacket(exped.getId(), CWvsContext.ExpeditionPacket.expeditionLeaderChanged(0),
                                null);
                    }
                }
                break;
            case 82:// 70
            case 140:
                part = c.getPlayer().getParty();
                if ((part == null) || (part.getExpeditionId() <= 0)) {
                    break;
                }
                exped = World.Party.getExped(part.getExpeditionId());
                if ((exped != null) && (exped.getLeader() == c.getPlayer().getId())) {
                    cid = slea.readInt();
                    for (i$ = exped.getParties().iterator(); i$.hasNext();) {
                        int i = ((Integer) i$.next());
                        MapleParty par = World.Party.getParty(i);
                        if (par != null) {
                            MaplePartyCharacter newleader = par.getMemberById(cid);
                            if ((newleader != null) && (par.getId() != part.getId())) {
                                World.Party.updateParty(par.getId(), PartyOperation.PartyRes_ChangePartyBoss_Done,
                                        newleader);
                            }
                        }
                    }
                }
                break;
            case 83:// 71
            case 141:
                part = c.getPlayer().getParty();
                if ((part == null) || (part.getExpeditionId() <= 0)) {
                    break;
                }
                exped = World.Party.getExped(part.getExpeditionId());
                if ((exped != null) && (exped.getLeader() == c.getPlayer().getId())) {
                    int partyIndexTo = slea.readInt();
                    if ((partyIndexTo < exped.getType().maxParty) && (partyIndexTo <= exped.getParties().size())) {
                        cid = slea.readInt();
                        for (i$ = exped.getParties().iterator(); i$.hasNext();) {
                            int i = ((Integer) i$.next());
                            MapleParty par = World.Party.getParty(i);
                            if (par != null) {
                                MaplePartyCharacter expelled = par.getMemberById(cid);
                                if ((expelled != null) && (expelled.isOnline())) {
                                    MapleCharacter chr = World.getStorage(expelled.getChannel())
                                            .getCharacterById(expelled.getId());
                                    if (chr == null) {
                                        break;
                                    }
                                    if (partyIndexTo < exped.getParties().size()) {
                                        MapleParty party = World.Party
                                                .getParty((exped.getParties().get(partyIndexTo)).intValue());
                                        if ((party == null) || (party.getMembers().size() >= 6)) {
                                            c.getPlayer().dropMessage(5, "Invalid party.");
                                            break;
                                        }
                                    }
                                    if (GameConstants.isDojo(c.getPlayer().getMapId())) {
                                        MulungSystem.failed(c.getPlayer());
                                    }
                                    World.Party.updateParty(i, PartyOperation.EXPEL, expelled);
                                    if (partyIndexTo < exped.getParties().size()) {
                                        MapleParty party = World.Party
                                                .getParty((exped.getParties().get(partyIndexTo)).intValue());
                                        if ((party != null) && (party.getMembers().size() < 6)) {
                                            World.Party.updateParty(party.getId(), PartyOperation.PartyRes_JoinParty_Done,
                                                    expelled);
                                            chr.receivePartyMemberHP();
                                            chr.updatePartyMemberHP();
                                            chr.getClient().getSession().writeAndFlush(
                                                    CWvsContext.ExpeditionPacket.expeditionStatus(exped, true, false));
                                        }
                                    } else {
                                        MapleParty party = World.Party.createPartyAndAdd(expelled, exped.getId());
                                        chr.setParty(party);
                                        chr.getClient().getSession()
                                                .writeAndFlush(CWvsContext.PartyPacket.partyCreated(party));
                                        chr.getClient().getSession().writeAndFlush(
                                                CWvsContext.ExpeditionPacket.expeditionStatus(exped, true, false));
                                        World.Party.expedPacket(exped.getId(), CWvsContext.ExpeditionPacket
                                                .expeditionUpdate(exped.getIndex(party.getId()), party), null);
                                    }
                                    if ((c.getPlayer().getEventInstance() != null) && (expelled.isOnline())) {
                                        c.getPlayer().getEventInstance().disbandParty();
                                    }

                                    if (c.getPlayer().getPyramidSubway() == null) {
                                        break;
                                    }
                                    c.getPlayer().getPyramidSubway().fail(c.getPlayer());
                                    break;
                                }
                            }
                        }
                    }

                }

                break;
            default:
                if (!c.getPlayer().isShowErr()) {
                    System.out.println("Unknown Expedition : " + mode + "\n" + slea);
                }
        }
    }
}
