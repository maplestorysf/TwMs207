package handling.channel.handler;

import client.character.stat.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.skill.Skill;
import client.skill.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import constants.ServerConstants;
import extensions.temporary.UserEffectOpcode;
import handling.MapleServerHandler;
import handling.cashshop.CashShopServer;
import handling.cashshop.handler.CashShopOperation;
import handling.channel.ChannelServer;
import handling.farm.FarmServer;
import handling.farm.handler.FarmOperation;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.exped.MapleExpedition;
import handling.world.guild.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import server.commands.PlayerGMRank;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.OpcodeEncryption;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CCashShop;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuddyListPacket;
import tools.packet.CWvsContext.GuildPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.FarmPacket;
import tools.packet.JobPacket.AvengerPacket;
import tools.packet.LoginPacket;
import tools.packet.PetPacket;

public class InterServerHandler {

    public static void EnterMTS(MapleClient c, MapleCharacter chr) {
        if (chr.卡圖 == chr.getMapId() && chr.getMapId() / 1000000 != 4) {
            chr.changeMap(100000000, 0);
        }
        chr.卡圖 = 0;
        c.announce(CWvsContext.enableActions());
    }

    public static void BackToCharList(final LittleEndianAccessor slea, MapleClient c) {
        final String account = slea.readMapleAsciiString();
        if (c.getAccountName() == null) {
            c.disconnect(true, false);
            return;
        }
        if (ServerConstants.REDIRECTOR && c.checkSecuredAccountName(account)) {
            if (c.getChannel() <= MapleServerHandler.LOGIN_SERVER) {
                return;
            }
            String code = World.Redirector.addRedirector(c);
            c.announce(CWvsContext.broadcastMsg(""));
            c.announce(CField.autoLogin(code));
            c.disconnect(true, false);
        } else {
            c.announce(CField.exitGame());
            c.disconnect(true, false);
        }
    }

    public static void EnterCS(final MapleClient c, final MapleCharacter chr) {
        if (chr.hasBlockedInventory() || chr.getMap() == null || chr.getEventInstance() != null
                || c.getChannelServer() == null) {
            c.announce(CField.serverBlocked(2));
            MapleCharacter farmtransfer = FarmServer.getPlayerStorage().getPendingCharacter(chr.getId());
            if (farmtransfer != null) {
                c.announce(FarmPacket.farmMessage("訪問農場時無法進入商城"));
            }
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (World.getPendingCharacterSize(chr.getWorld()) >= 30) {
            chr.dropMessage(1, "伺服器忙, 請稍後再試.");
            c.announce(CWvsContext.enableActions());
            return;
        }
        ChannelServer ch = ChannelServer.getInstance(c.getChannel());
        chr.changeRemoval();
        if (chr.getBuffedValue(MapleBuffStat.SUMMON) != null) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.SUMMON, -1);
        }
        World.ChannelChange_Data(c, chr, MapleServerHandler.CASH_SHOP_SERVER);
        ch.removePlayer(chr);
        c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
        chr.saveToDB(false, false);
        if (chr.getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
            World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
        }
        chr.getMap().removePlayer(chr);
        c.announce(CField.getChannelChange(Integer.parseInt(CashShopServer.getIP().split(":")[1])));
        c.setPlayer(null);
        c.setReceiving(false);
    }

    public static void EnterFarm(final MapleClient c, final MapleCharacter chr) {
        if (chr.hasBlockedInventory() || chr.getMap() == null || chr.getEventInstance() != null
                || c.getChannelServer() == null) {
            c.announce(CField.serverBlocked(2));
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (World.getPendingCharacterSize(chr.getWorld()) >= 30) {
            chr.dropMessage(1, "伺服器忙, 請稍後再試.");
            c.announce(CWvsContext.enableActions());
            return;
        }
        ChannelServer ch = ChannelServer.getInstance(c.getChannel());
        chr.changeRemoval();
        if (chr.getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
            World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
        }
        World.ChannelChange_Data(c, chr, MapleServerHandler.FARM_SERVER);
        ch.removePlayer(chr);
        c.updateLoginState(3, c.getSessionIPAddress());
        chr.saveToDB(false, false);
        chr.getMap().removePlayer(chr);
        c.announce(CField.getChannelChange(Integer.parseInt(FarmServer.getIP().split(":")[1])));
        c.setPlayer(null);
        c.setReceiving(false);
    }

    public static void MigrateIn(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.available() < 8) {
            System.out.println("登入出錯");
            return;
        }
        final int world = slea.readInt();
        final int playerid = slea.readInt();

        ServerConstants.ServerType logToServer = ServerConstants.ServerType.頻道伺服器;
        MapleCharacter player = CashShopServer.getPlayerStorage() == null ? null : CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
        if (player != null) {
            logToServer = ServerConstants.ServerType.購物商城;
        }
        if (player == null) {
            player = FarmServer.getPlayerStorage() == null ? null : FarmServer.getPlayerStorage().getPendingCharacter(playerid);
            if (player != null) {
                logToServer = ServerConstants.ServerType.FARM_SERVER;
            }
        }
        boolean transfer = false;
        if (player == null) {
            logToServer = ServerConstants.ServerType.頻道伺服器;
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv == null || cserv.getPlayerStorage() == null) {
                    continue;
                }
                player = cserv.getPlayerStorage().getPendingCharacter(playerid);
                if (player != null) {
                    System.out.println("更變頻道:" + cserv.getChannel());
                    c.setChannel(cserv.getChannel());
                    break;
                }
            }
            if (player == null) { // Player isn't in storage, probably isn't CC
                Triple<String, String, Integer> ip = LoginServer.getLoginAuth(playerid);
                String s = c.getSessionIPAddress();
                if (ip == null || !s.substring(s.indexOf('/') + 1, s.length()).equals(ip.left)) {
                    if (ip != null) {
                        LoginServer.putLoginAuth(playerid, ip.left, ip.mid, ip.right);
                    }
                    c.getSession().close();
                    System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                    return;
                }
                c.setTempIP(ip.mid);
                c.setChannel(ip.right);
                player = MapleCharacter.loadCharFromDB(playerid, c, true);
                System.out.println("從數據庫中獲取角色");
                transfer = true;
            } else {
                player.ReconstructChr(player, c);
                System.out.println("重構角色");
                transfer = false;
            }
        }

        c.setPlayer(player);
        System.out.println("設置用戶端角色: " + player.getName());
        c.setAccID(player.getAccountID());
        System.out.println("設置用戶端賬號ID: " + player.getAccountID());

        // 客戶端包頭混淆
        final byte[] desKey = new byte[24];
        final String charIDStr = String.valueOf(playerid);
        System.arraycopy(charIDStr.getBytes(), 0, desKey, 0, charIDStr.getBytes().length);
        final byte[] lastKeyPart = slea.read(16 - charIDStr.getBytes().length);
        System.arraycopy(lastKeyPart, 0, desKey,  charIDStr.getBytes().length, lastKeyPart.length);
        System.arraycopy(desKey, 0, desKey,  16, 8);
        c.setOpecodeCrypto(new OpcodeEncryption(desKey));
        byte[] mapping = c.getOpecodeCrypto().initMap();
        c.announce(LoginPacket.onClientOpcodeEncyption(4, mapping));

        switch (logToServer) {
            case 購物商城:
                player.setClient(c);
                c.setTempIP(player.getOneTempValue("Transfer", "TempIP"));
                c.setAccountName(player.getOneTempValue("Transfer", "AccountName"));
                CashShopOperation.EnterCS(player, c);
                break;
            case 拍賣場:
                break;
            case FARM_SERVER:
                player.setClient(c);
                c.setTempIP(player.getOneTempValue("Transfer", "TempIP"));
                c.setAccountName(player.getOneTempValue("Transfer", "AccountName"));
                FarmOperation.EnterFarm(player, c);
                break;
            case 頻道伺服器:
                player.updateOneTempValue("Transfer", "TempIP", null);
                player.updateOneTempValue("Transfer", "AccountName", null);
                player.updateOneTempValue("Transfer", "Channel", null);
                EnterChannel(player, c, transfer);
                break;
            default:
                return;
        }
    }

    public static void EnterChannel(final MapleCharacter player, final MapleClient c, final boolean transfer) {
        final ChannelServer channelServer = c.getChannelServer();

        if (ServerConstants.IS_BETA_FOR_ADMINS) {
            PlayerGMRank rank = PlayerGMRank.ADMIN;
            if (player.getGmLevel() != rank.getLevel()) {
                player.setGmLevel(rank);
            }
            if (c.getGmLevel() != rank.getLevel()) {
                c.setGmLevel(rank);
            }
        }

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
            System.out.println("Remote Hack");
            return;
        }
        final int state = c.getLoginState();
        System.out.println("狀態 = " + c.getLoginState());
        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL || state == MapleClient.LOGIN_NOTLOGGEDIN) {
            World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        System.out.println("正在將角色添加到頻道");
        channelServer.addPlayer(player);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        c.announce(CWvsContext.onEventNameTag(new int[]{-1, -1, -1, -1, -1}));
        c.announce(CWvsContext.getEventList(true));
        c.announce(CField.onSetField(player));
        c.announce(CCashShop.onAuthenCodeChanged()); // Enable CashShop
        c.announce(CWvsContext.onHourChanged((short) calendar.get(Calendar.DAY_OF_WEEK), 1));
        c.announce(CField.onSetQuestClear());
        // 0x020F:
        c.announce(CWvsContext.onSetTamingMobInfo(player, false));
        c.announce(CWvsContext.updateSkills(c.getPlayer().getSkills(), false));// skill to 0 "fix"
        c.getPlayer().updateVSkillRecords();
        c.announce(InventoryPacket.updateInventoryFull());
        c.announce(CWvsContext.onForcedStatReset());
        // c.announce(CWvsContext.updateSkills(c.getPlayer().getSkills(),
        // false));//skill to 0 "fix"
        c.announce(CWvsContext.broadcastMsg(channelServer.getServerMessage(player.getWorld())));
        if (player.isIntern()) {// GM登入自動隱身並無敵處理
            SkillFactory.getSkill(9001004).getEffect(1).applyTo(player);
            player.setInvincible(true);
        }
        if (player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.墜飾欄)) != null
                && player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.墜飾欄)).getCustomData() != null
                && "0".equals(player.getQuestNoAdd(MapleQuest.getInstance(GameConstants.墜飾欄)).getCustomData())) {// 更新永久墜飾欄
            c.announce(CWvsContext.updatePendantSlot(0));
        }

        player.getMap().addPlayer(player);

        // 進入的頻道類型提示
        if (channelServer.getChannelType() != ChannelServer.ChannelType.普通) {
            c.getSession().writeAndFlush(CField.EffectPacket.showEffect(true, player, UserEffectOpcode.UserEffect_TextEffect, new int[]{50, 1500, 4, 0, -120, 1, 4, 2}, new String[]{"#fnDFKai-SB##fs20#您已進入#fnMingLiU##e#r★" + channelServer.getChannelType().name() + "頻道★#fnDFKai-SB##n#k,部分怪物得到強化並且獎勵增加!!"}, null, null));
        }
        try {
            // Start of buddylist
            final int buddyIds[] = player.getBuddylist().getBuddyIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            if (player.getParty() != null) {
                final MapleParty party = player.getParty();
                World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
                if (party.getExpeditionId() > 0) {
                    final MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                    if (me != null) {
                        c.announce(CWvsContext.ExpeditionPacket.expeditionStatus(me, false, true));
                    }
                }
            }
            final CharacterIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                player.getBuddylist().get(onlineBuddy.getCharacterId()).setChannel(onlineBuddy.getChannel());
            }
            c.announce(BuddyListPacket.getBuddylist(player.getBuddylist().getBuddies()));

            // Start of Messenger
            final MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getChannel());
            }

            // Start of Guild and alliance
            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.announce(GuildPacket.showGuildInfo(player.getGuild()));
                final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    final List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (byte[] pack : packetList) {
                            if (pack != null) {
                                c.announce(pack);
                            }
                        }
                    }
                } else { // guild not found, change guild id
                    player.setGuildId(0);
                    player.setGuildRank((byte) 5);
                    player.setAllianceRank((byte) 5);
                    player.saveGuildStatus();
                }
            }
            // c.announce(FamilyPacket.getFamilyData());
            // c.announce(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.Login_Error, e);
        }
        player.sendMacros();
        player.showNote();
        player.sendImp();
        player.updatePartyMemberHP();
        player.startFairySchedule(false);
        player.baseSkills(); // fix people who've lost skills.
        if (MapleJob.is神之子(player.getJob())) {
            c.announce(CWvsContext.updateSkills(player.getSkills(), false));
        }
        int job = c.getPlayer().getJob();
        c.announce(CField.getKeymap(player.getKeyLayout(), player));// fix keylayout?
        player.updatePetAuto();
        player.expirationTask(true, transfer);
        c.announce(CWvsContext.updateMaplePoint(player.getCSPoints(2)));
        player.checkBerserk();
        if (MapleJob.is惡魔復仇者(player.getJob())) {
            c.announce(AvengerPacket.giveAvengerHpBuff(player.getStat().getHp()));
        }
        if (MapleJob.is劍豪(player.getJob()) && !player.isBuffed(40011288) // 拔刀姿勢
                && !player.isBuffed(40011291)) { // 一般姿勢效果
            SkillFactory.getSkill(40011291)
                    .getEffect((int) Math.min(5, Math.max(1, Math.floor(player.getJianQi() / 200)))).applyTo(player);
        }
        player.spawnClones();
        player.spawnSavedPets();
        if (player.getStat().equippedSummon > 0) {
            Skill skil = SkillFactory
                    .getSkill(player.getStat().equippedSummon + (MapleJob.getBeginner(player.getJob()) * 1000));
            if (skil != null && skil.getEffect(1) != null) {
                skil.getEffect(1).applyTo(player);
            }
        }
        MapleInventory equipped = player.getInventory(MapleInventoryType.EQUIPPED);
        List<Short> slots = new ArrayList<>();
        for (Item item : equipped.newList()) {
            slots.add(item.getPosition());
        }

        if (c.getPlayer().isEquippedSoulWeapon() && transfer) {
            c.getPlayer().setSoulCount((short) 0);
            c.announce(CWvsContext.BuffPacket.giveSoulGauge(c.getPlayer().getSoulCount(),
                    c.getPlayer().getEquippedSoulSkill()));
        }

        // 登入召喚寵物
        if (transfer) {
            for (final MaplePet pet : c.getPlayer().getSummonedPets()) {
                c.announce(PetPacket.showPet(c.getPlayer(), pet, false, false));
            }
        }

        // 清理斷線未處理的方塊任務
        c.getPlayer().clearInfoQuest(GameConstants.台方塊);

        player.updateReward();
    }

    public static final void ChangeChannel(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr, final boolean room) {
        if (chr == null || chr.hasBlockedInventory() || chr.getEventInstance() != null || chr.getMap() == null
                || chr.isInBlockedMap() || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (World.getPendingCharacterSize(chr.getWorld()) >= 30) {
            chr.dropMessage(1, "伺服器忙, 請稍後再試。");
            c.announce(CWvsContext.enableActions());
            return;
        }
        final int chc = slea.readByte() + 1;
        int mapid = 0;
        if (room) {
            mapid = slea.readInt();
        }
        chr.updateTick(slea.readInt());
        if (!World.isChannelAvailable(chc, chr.getWorld())) {
            chr.dropMessage(1, "該頻道玩家已滿，請切換到其他頻道進行遊戲。");
            c.announce(CWvsContext.enableActions());
            return;
        }

        /*
         * if (c.getMVPLevel() <= 0 && ChannelServer.getInstance(chc) != null &&
         * ChannelServer.getInstance(chc).getChannelType() ==
         * ChannelServer.ChannelType.MVP) {
         * c.getSession().writeAndFlush(CWvsContext.broadcastMsg(1, "您沒有進入MIP頻道的權限."));
         * c.getSession().writeAndFlush(CWvsContext.enableActions()); return; }
         */
        if (room && (mapid < 910000001 || mapid > 910000022)) {
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (c.getChannel() != chc) {
            chr.changeChannel(chc);
        }
        if (room) {
            if (chr.getMapId() != mapid) {
                final MapleMap warpz = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(mapid);
                if (warpz != null) {
                    chr.changeMap(warpz, warpz.getPortal("out00"));
                }
            }
            c.announce(CWvsContext.enableActions());
        }
    }
}
