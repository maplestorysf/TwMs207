package handling;

import client.ClientRedirector;
import handling.farm.handler.FarmHandler;
import client.MapleClient;
import client.MapleJob;
import client.inventory.Item;
import client.inventory.MaplePet;
import client.inventory.PetDataFactory;
import constants.ServerConfig;
import constants.ServerConstants;
import constants.ServerConstants.ServerType;
import handling.cashshop.CashShopServer;
import handling.cashshop.handler.*;
import handling.channel.ChannelServer;
import handling.channel.handler.*;
import handling.farm.FarmServer;
import handling.farm.handler.FarmOperation;
import handling.login.LoginServer;
import handling.login.handler.*;
import handling.netty.MaplePacketDecoder;
import handling.world.World;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import server.CashItem;
import server.CashItemFactory;
import server.Randomizer;
import server.commands.CommandProcessor;
import server.maps.MapleMap;
import server.shark.SharkPacket;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.Pair;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.LoginPacket;
import tools.packet.CCashShop;
import tools.packet.CWvsContext;
import tools.packet.JobPacket;

public class MapleServerHandler extends ChannelHandlerAdapter {

    private final int world, channel;
    private final ServerType serverType;
    public final static int FARM_SERVER = -30;
    public final static int CASH_SHOP_SERVER = -10;
    public final static int LOGIN_SERVER = 0;
    private final List<String> BlockedIP = new ArrayList<>();
    private final Map<String, Pair<Long, Byte>> tracker = new ConcurrentHashMap<>();

    public MapleServerHandler(ServerType serverType, int world, int channel) {
        this.world = world;
        this.channel = channel;
        this.serverType = serverType;
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (cause.getMessage() != null) {
            System.err.println("[連結異常] " + cause.getMessage());
            cause.printStackTrace();
            cause.getLocalizedMessage();
            // FileoutputUtil.printError("連結異常.txt", cause.getMessage());
        }
        MapleClient client = (MapleClient) ctx.attr(MapleClient.CLIENT_KEY).get();
        if ((client != null) && (client.getPlayer() != null)) {
            client.getPlayer().saveToDB(true, this.serverType == ServerType.購物商城);
            // FileoutputUtil.printError("連結異常.txt", cause, "連結異常 by: 角色:" +
            // client.getPlayer().getName() + " 職業:" + client.getPlayer().getJob() + " 地圖:"
            // + client.getPlayer().getMap());
        }
        ctx.close();
        // MapleClient client = (MapleClient)
        // session.getAttribute(MapleClient.CLIENT_KEY);
        // log.error(MapleClient.getLogMessage(client, cause.getMessage()), cause);
        // cause.printStackTrace();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        // Start of IP checking
        final String address = ctx.channel().remoteAddress().toString().split(":")[0];
        System.out.println("[連結線程] " + address + " 已連結");

        if (BlockedIP.contains(address) || !checkTracker(address)) {
            ctx.channel().close();
            return;
        }
        // End of IP checking.
        String IP = address.substring(address.indexOf('/') + 1, address.length());

        if (serverType == ServerType.FARM_SERVER) {
            if (FarmServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        } else if (serverType == ServerType.購物商城) {
            if (CashShopServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        } else if (serverType == ServerType.登入伺服器) {
            if (LoginServer.isShutdown()) {
                ctx.channel().close();
                return;
            }
        } else if (serverType == ServerType.頻道伺服器) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                ctx.channel().close();
                return;
            }
            if (!LoginServer.containsIPAuth(IP)) {
                ctx.channel().close();
                return;
            }
        } else {
            System.out.println("[連結錯誤] 未知類型: " + channel);
            ctx.channel().close();
            return;
        }

        LoginServer.removeIPAuth(IP);
        // IV used to decrypt packets from client.
        final byte ivRecv[] = new byte[]{(byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255)};
        // IV used to encrypt packets for client.
        final byte ivSend[] = new byte[]{(byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255), (byte) Randomizer.nextInt(255)};
        MapleAESOFB sendCypher = new MapleAESOFB(ivSend, (short) (0xFFFF - ServerConstants.MAPLE_VERSION));
        MapleAESOFB recvCypher = new MapleAESOFB(ivRecv, ServerConstants.MAPLE_VERSION);
        final MapleClient client = new MapleClient(sendCypher, recvCypher, ctx.channel());
        client.setChannel(channel);
        MaplePacketDecoder.DecoderState decoderState = new MaplePacketDecoder.DecoderState();
        ctx.channel().attr(MaplePacketDecoder.DECODER_STATE_KEY).set(decoderState);

        byte[] handShakePacket;
        if (serverType == ServerType.登入伺服器) {
            handShakePacket = LoginPacket.getHello(ivSend, ivRecv);
        } else {
            handShakePacket = LoginPacket.getHelloOld(ivSend, ivRecv);
        }
        ctx.channel().writeAndFlush(handShakePacket);

        byte[] hp = new byte[handShakePacket.length + 2];
        hp[0] = (byte) 0xFF;
        hp[1] = (byte) 0xFF;
        for (int i = 2; i < handShakePacket.length + 2; i++) {
            hp[i] = handShakePacket[i - 2];
        }
        if (ServerConfig.LOG_SHARK) {
            final SharkPacket sp = new SharkPacket(hp, false);
            client.sl.log(sp);
        }

        System.out.println("握手包發送到 " + address);
        Random r = new Random();
        client.setSessionId(r.nextLong()); // Generates a random session id.
        ctx.channel().attr(MapleClient.CLIENT_KEY).set(client);
        World.Client.addClient(client);

        if (LoginServer.isAdminOnly()) {
            StringBuilder sb = new StringBuilder();
            sb.append("IoSession opened ").append(address);
            System.out.println(sb.toString());
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        MapleClient client = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();

        if (client != null) {
            try {
                client.disconnect(true, this.serverType == ServerType.購物商城);
            } finally {
                boolean finsh = World.Client.removeClient(client);
                System.out.println("移除客戶端 - " + finsh);
                ctx.channel().close();
                ctx.channel().attr(MapleClient.CLIENT_KEY).remove();
                ctx.channel().attr(MaplePacketDecoder.DECODER_STATE_KEY).remove();
                client = null;
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object status) throws Exception {
        MapleClient client = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();

        if (client != null) {
            client.sendPing();
        }
        super.userEventTriggered(ctx, status);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object message) {
        if (message == null || ctx.channel() == null) {
            return;
        }
        final LittleEndianAccessor slea = new LittleEndianAccessor(new ByteArrayByteStream((byte[]) message));
        if (slea.available() < 2) {
            return;
        }
        final MapleClient c = (MapleClient) ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        if (c == null || !c.isReceiving()) {
            return;
        }
        short header_num = slea.readShort();
        
        
        if( c.getOpecodeCrypto() != null ) {
            header_num = (short) c.getOpecodeCrypto().mapOpcode(header_num);
        } 

        for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
            if (recv.getValue() == header_num) {
                if (recv.NeedsChecking()) {
                    if (!c.isLoggedIn()) {
                        return;
                    }
                }
                // } else if
                // (!checkTracker(ctx.channel().remoteAddress().toString().split(":")[0])) {
                // return;
                // }
                try {
                    if (c.getPlayer() != null && c.isMonitored()) {
                        try (FileWriter fw = new FileWriter(new File(FileoutputUtil.Monitor_Dir + c.getPlayer().getName() + "_log.txt"), true)) {
                            fw.write(String.valueOf(recv) + " (" + Integer.toHexString(header_num) + ") Handled: \r\n" + slea.toString() + "\r\n");
                            fw.flush();
                        }
                    }
                    handlePacket(recv, slea, c);
                } catch (Exception e) {
                    if (c.getPlayer() != null && c.getPlayer().isShowErr()) {
                        c.getPlayer().showInfo("數據包異常", true, "包頭:" + recv.name() + "(0x" + Integer.toHexString(header_num).toUpperCase() + ")");
                    }
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                    FileoutputUtil.log(FileoutputUtil.PacketEx_Log, "Packet: " + header_num + "\r\n" + slea.toString(true));
                }

                return;
            }
        }
        if (ServerConfig.LOG_PACKETS) {
            final byte[] packet = slea.read((int) slea.available());
            final StringBuilder sb = new StringBuilder("發現未知用戶端數據包 - (包頭:0x" + Integer.toHexString(header_num) + ")");
            System.err.println(sb.toString());
            sb.append(":\r\n").append(HexTool.toString(packet)).append("\r\n").append(HexTool.toStringFromAscii(packet));
            FileoutputUtil.log(FileoutputUtil.UnknownPacket_Log, sb.toString());
        }
    }

    public boolean checkTracker(String address) {
        final Pair<Long, Byte> track = tracker.get(address);

        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;

            final long difference = System.currentTimeMillis() - track.left;
            if (difference < 2000) { // Less than 2 sec
                count++;
            } else if (difference > 20000) { // Over 20 sec
                count = 1;
            }
            if (count >= 10) {
                BlockedIP.add(address);
                System.out.println("[登入服務] IP:" + address + " 連結次數超過限制斷開連結");
                tracker.remove(address); // Cleanup
                return false;
            }
        }
        tracker.put(address, new Pair<>(System.currentTimeMillis(), count));
        return true;
    }

    public static void handlePacket(final RecvPacketOpcode header, final LittleEndianAccessor slea, final MapleClient c) throws Exception {
        switch (header) {
            case CP_LogoutWorld:
                if (!ServerConstants.REDIRECTOR) {
                    System.out.println("Redirector login packet recieved, but server is not set to redirector. Please change it in ServerConstants!");
                } else {
                    slea.skip(2);
                    String code = slea.readMapleAsciiString();
                    Map<String, ClientRedirector> redirectors = World.Redirector.getRedirectors();
                    if (!redirectors.containsKey(code) || redirectors.get(code).isLogined()) {
                        if (redirectors.get(code).isLogined()) {
                            redirectors.remove(code);
                        }
                        c.getSession().close();
                        System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                    } else {
                        ClientRedirector redirector = redirectors.get(code);
                        redirector.setLogined(true);
                        c.loginData(redirector.getAccount());
                        c.getSession().writeAndFlush(LoginPacket.getSecondAuthSuccess(c));
                    }
                }
                break;
            case CRASH_INFO:
                System.out.println("Crash" + slea.toString());
                break;
            case CP_BEGIN_SOCKET:
                byte mapleType = slea.readByte();
                short mapleVersion = slea.readShort();
                String maplePatch = String.valueOf(slea.readShort());
                if ((mapleType != (ServerConstants.MAPLE_TYPE.getType())) || (mapleVersion != ServerConstants.MAPLE_VERSION) || (!maplePatch.equals(ServerConstants.MAPLE_PATCH.split(":")[0]))) {
                    c.getSession().close();
                    System.err.println("伺服器主動斷開用戶端連結,調用位置: " + new java.lang.Throwable().getStackTrace()[0]);
                    System.out.println(c.getSessionIPAddress() + " 用戶端不正確,斷開連接");
                } else {
                    System.out.println(c.getSessionIPAddress() + " 連接到遊戲!");
                }
                break;
            case CLIENT_START:
                c.getSession().writeAndFlush(LoginPacket.showMapleStory());
                break;
            case CP_AliveAck:
                c.pongReceived();
                break;
            case CP_DUMMY_CODE:
                break;
            case CP_CheckLoginAuthInfo:
                CharLoginHandler.login(slea, c);
                break;
            case WRONG_PASSWORD:
                if (c.getSessionIPAddress().contains("8.31.99.141") || c.getSessionIPAddress().contains("8.31.99.143") || c.getSessionIPAddress().contains("127.0.0.1")) {
                    c.loginData("admin");
                    c.getSession().writeAndFlush(LoginPacket.getAuthSuccessRequest(c));
                }
                break;
            case SET_GENDER:
                CharLoginHandler.SetGender(slea, c);
                break;
            case AUTH_REQUEST:
                CharLoginHandler.handleAuthRequest(slea, c);
                break;
            case CLIENT_FAILED:
                break;
            case VIEW_SERVERLIST:
                if (slea.readByte() == 0) {
                    CharLoginHandler.ServerListRequest(c);
                }
                break;
            case REDISPLAY_SERVERLIST:
            case CP_WorldInfoRequest:
                CharLoginHandler.ServerListRequest(c);
                break;
            case CP_SelectWorld:
                CharLoginHandler.CharlistRequest(slea, c);
                break;
            case CP_UpdateCharacterSelectList:
                CharLoginHandler.changeCharPosition(slea, c);
                break;
            case SERVERSTATUS_REQUEST:
                CharLoginHandler.ServerStatusRequest(c);
                break;
            case CP_CheckDuplicatedID:
                CharLoginHandler.CheckCharName(slea.readMapleAsciiString(), c);
                break;
            case CP_CheckSPWExistRequest:
                CharLoginHandler.CreateCharClick(slea, c);
                break;
            case CP_CreateNewCharacter:
            case CREATE_SPECIAL_CHAR:
                CharLoginHandler.CreateChar(slea, c);
                break;
            case CP_CheckSPWOnCreateNewCharacter:
                CharLoginHandler.CreateChar2Pw(slea, c);
                break;
            case CP_CreateNewCharacter_PremiumAdventurer:
                CharLoginHandler.CreateUltimate(slea, c);
                break;
            case CP_DeleteCharacter:
                CharLoginHandler.DeleteChar(slea, c);
                break;
            case CHAR_SELECT_NO_PIC:
                CharLoginHandler.CharacterSelected(slea, c, false, false);
                break;
            case VIEW_REGISTER_PIC:
                CharLoginHandler.CharacterSelected(slea, c, true, false);
                break;
            case CP_AlbaRequest:
                CharLoginHandler.PartJob(slea, c);
                break;
            case CP_SelectCharacter:
            case CP_DirectGoToField:
                CharLoginHandler.CharacterSelected(slea, c, false, false);
                break;
            case VIEW_SELECT_PIC:
                CharLoginHandler.CharacterSelected(slea, c, true, true);
                break;
            case CP_CheckSPWRequest:
                CharLoginHandler.CharacterSelected(slea, c, false, true);
                break;
            case CP_UserRegisterPetAutoBuffRequest:
                slea.readInt();
                int skillid = slea.readInt();
                c.getSession().writeAndFlush(CWvsContext.enableActions());
                break;
            case CP_UpdateCharacterCard:
                CharLoginHandler.updateCCards(slea, c);
                break;
            case CP_ExceptionLog:
                FileoutputUtil.log(FileoutputUtil.Client_Feedback, "\r\n" + slea.readMapleAsciiString());
                break;
            case CP_ClientDumpLog:
                System.err.println("收到用戶端的報錯: \r\n(詳細請看\"日誌/用戶端_報錯.txt\")\r\n(" + slea.toString());
                if (slea.available() < 8) {
                    System.out.println(slea.toString());
                    return;
                }
                short type = slea.readShort();
                String type_str = "Unknown?!";
                switch (type) {
                    case 0x01:
                        type_str = "SendBackupPacket";
                        break;
                    case 0x02:
                        type_str = "Crash Report";
                        break;
                    case 0x03:
                        type_str = "Exception";
                        break;
                    default:
                        break;
                }
                int errortype = slea.readInt(); // example error 38
                // if (errortype == 0) { // i don't wanna log error code 0 stuffs, (usually some
                // bounceback to login)
                // return;
                // }
                short data_length = slea.readShort();
                slea.skip(4); // ?B3 86 01 00 00 00 FF 00 00 00 00 00 9E 05 C8 FF 02 00 CD 05 C9 FF 7D 00 00
                // 00 3F 00 00 00 00 00 02 77 01 00 25 06 C9 FF 7D 00 00 00 40 00 00 00 00 00 02
                // C1 02
                short opcodeheader = slea.readShort();
                byte[] opcode = slea.read((int) slea.available());
                int packetLen = (int) slea.available() + 2;
                String AccountName = "null";
                String charName = "null";
                String charLevel = "null";
                String charJob = "null";
                String Map = "null";
                try {
                    AccountName = c.getAccountName();
                } catch (Throwable e) {
                }
                try {
                    charName = c.getPlayer().getName();
                } catch (Throwable e) {
                }
                try {
                    charLevel = String.valueOf(c.getPlayer().getLevel());
                } catch (Throwable e) {
                }
                try {
                    charJob = MapleJob.getById(c.getPlayer().getJob()).name() + "(" + String.valueOf(c.getPlayer().getJob())
                            + ")";
                } catch (Throwable e) {
                }
                try {
                    Map = c.getPlayer().getMap().toString();
                } catch (Throwable e) {
                }
                // System.err.println("[用戶端 報錯] 錯誤代碼 " + errortype + " 類型 " + type_str +
                // "\r\n\t[數據] 長度 " + data_length + " [" + SendPacketOpcode.nameOf(opcodeheader)
                // + " | " + opcodeheader + "]\r\n" + HexTool.toString(slea.read((int)
                // slea.available())));
                String tab = "";
                for (int i = 4; i > SendPacketOpcode.nameOf(opcodeheader).length() / 8; i--) {
                    tab += "\t";
                }
                String t = packetLen >= 10 ? packetLen >= 100 ? packetLen >= 1000 ? "" : " " : "  " : "   ";
                FileoutputUtil.log(FileoutputUtil.Client_Error,
                        "\r\n" + "帳號:" + AccountName + "\r\n" + "角色:" + charName + "(等級:" + charLevel + ")" + "\r\n" + "職業:"
                        + charJob + "\r\n" + "地圖:" + Map + "\r\n" + "錯誤類型: " + type_str + "(" + errortype + ")\r\n"
                        + "\r\n" + "[LP]\t" + SendPacketOpcode.nameOf(opcodeheader) + tab + " \t包頭:"
                        + HexTool.getOpcodeToString(opcodeheader) + t + "[" + (data_length - 4) + "字元]\r\n" + "\r\n"
                        + (opcode.length < 1 ? "" : (HexTool.toString(opcode) + "\r\n"))
                        + (opcode.length < 1 ? "" : (HexTool.toStringFromAscii(opcode) + "\r\n")) + "\r\n");
                break;
            case ENABLE_SPECIAL_CREATION:
                c.getSession().writeAndFlush(LoginPacket.enableSpecialCreation(c.getAccID(), true));
                break;
            case RSA_KEY:
                break;
            case GET_SERVER:
                c.getSession().writeAndFlush(LoginPacket.getLoginBackground());
                break;
            // END OF LOGIN SERVER
            case CP_UserTransferChannelRequest:
            case CP_UserTransferFreeMarketRequest:
                InterServerHandler.ChangeChannel(slea, c, c.getPlayer(), header == RecvPacketOpcode.CP_UserTransferFreeMarketRequest);
                break;
            case CP_MigrateIn:
                InterServerHandler.MigrateIn(slea, c);
                break;
            case CP_UserMigrateToPvpRequest:
            case CP_UserRequestPvPStatus:
                PlayersHandler.EnterPVP(slea, c);
                break;
            case CP_UserMobMoveAbilityChange:
                PlayersHandler.RespawnPVP(slea, c);
                break;
            case CP_UserMigrateToPveRequest:
                PlayersHandler.LeavePVP(slea, c);
                break;
            case CP_UserTransferAswanRequest:
                PlayersHandler.EnterAzwan(slea, c);
                break;
            case CP_UserTransferAswanReadyRequest:
                PlayersHandler.EnterAzwanEvent(slea, c);
                break;
            case CP_AswanRetireRequest:
                PlayersHandler.LeaveAzwan(slea, c);
                c.getSession().writeAndFlush(CField.showScreenAutoLetterBox("hillah/fail"));
                c.getSession().writeAndFlush(CField.UIPacket.openUIOption(0x45, 0));
                break;
            case CP_UserAttackUser:
                PlayersHandler.AttackPVP(slea, c);
                break;
            case PVP_SUMMON:
                SummonHandler.SummonPVP(slea, c);
                break;
            case ENTER_FARM:
                InterServerHandler.EnterFarm(c, c.getPlayer());
                break;
            case FARM_COMPLETE_QUEST:
                FarmHandler.completeQuest(slea, c);
                break;
            case FARM_NAME:
                FarmHandler.createFarm(slea, c);
                break;
            case PLACE_FARM_OBJECT:
                FarmHandler.placeBuilding(slea, c);
                break;
            case FARM_SHOP_BUY:
                FarmHandler.buy(slea, c);
                break;
            case HARVEST_FARM_BUILDING:
                FarmHandler.harvest(slea, c);
                break;
            case USE_FARM_ITEM:
                FarmHandler.useItem(slea, c);
                break;
            case RENAME_MONSTER:
                FarmHandler.renameMonster(slea, c);
                break;
            case NURTURE_MONSTER:
                FarmHandler.nurtureMonster(slea, c);
                break;
            case FARM_QUEST_CHECK:
                FarmHandler.checkQuestStatus(slea, c);
                break;
            case FARM_FIRST_ENTRY:
                FarmHandler.firstEntryReward(slea, c);
                break;
            case EXIT_FARM:
                FarmOperation.LeaveFarm(slea, c, c.getPlayer());
                break;
            case CP_UserMigrateToCashShopRequest:
                InterServerHandler.EnterCS(c, c.getPlayer());
                break;
            case ENTER_MTS:
                InterServerHandler.EnterMTS(c, c.getPlayer());
                break;
            case CP_UserFinalAttackRequest:

                break;
            case CP_UserMove:
                PlayerHandler.MovePlayer(slea, c, c.getPlayer());
                break;
            case CP_UserCharacterInfoRequest:
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.CharInfoRequest(slea.readInt(), c, c.getPlayer());
                break;
            case ORBITAL_FLAME:
                PlayerHandler.OrbitalFlame(slea, c);
                break;
            case PSYCHIC_GREP_R:
                JobPacket.KinesisPacket.PsychicGrep(slea, c);
                break;
            case PSYCHIC_ATTACK_R:
                JobPacket.KinesisPacket.PsychicAttack(slea, c);
                break;
            case PSYCHIC_ULTIMATE_R:
                JobPacket.KinesisPacket.PsychicUltimateDamager(slea, c);
                break;
            case PSYCHIC_DAMAGE_R:
                JobPacket.KinesisPacket.PsychicDamage(slea.readInt(), c);
                break;
            case CANCEL_PSYCHIC_GREP_R:
                slea.skip(8);
                JobPacket.KinesisPacket.CancelPsychicGrep(slea, c);
                break;
            case CP_UserMeleeAttack:
            case CP_UserShootAttack:
            case CP_UserMagicAttack:
            case CP_UserBodyAttack:
            case CP_SummonedAttack:
            case CP_SummonedAssistAttackDone:
            case CP_UserAreaDotAttack:
            case DMG_FLAME:
                PlayerHandler.attack(slea, c, header);
                break;
            case CP_UserSkillUseRequest:
                PlayerHandler.SpecialSkill(slea, c, c.getPlayer());
                break;
            case GET_BOOK_INFO:
                PlayersHandler.MonsterBookInfoRequest(slea, c, c.getPlayer());
                break;
            case MONSTER_BOOK_DROPS:
                PlayersHandler.MonsterBookDropsRequest(slea, c, c.getPlayer());
                break;
            case CHANGE_CODEX_SET:
                PlayersHandler.ChangeSet(slea, c, c.getPlayer());
                break;
            case CP_UserRequestInstanceTable:
                PlayersHandler.updateSpecialStat(slea, c);
                break;
            case CP_MakingSkillRequest:
                ItemMakerHandler.CraftComplete(slea, c, c.getPlayer());
                break;
            case CP_BroadcastOneTimeActionToSplit:
                ItemMakerHandler.CraftMake(slea, c, c.getPlayer());
                break;
            case CP_BroadcastEffectToSplit:
                ItemMakerHandler.CraftEffect(slea, c, c.getPlayer());
                break;
            case CP_GatherRequest:
                ItemMakerHandler.StartHarvest(slea, c, c.getPlayer());
                break;
            case CP_GatherEndNotice:
                ItemMakerHandler.StopHarvest(slea, c, c.getPlayer());
                break;
            case CP_DecomposerRequest:
                ItemMakerHandler.MakeExtractor(slea, c, c.getPlayer());
                break;
            case CP_UserBagItemUseRequest:
                ItemMakerHandler.UseBag(slea, c, c.getPlayer());
                break;
            case USE_FAMILIAR:
                MobHandler.UseFamiliar(slea, c, c.getPlayer());
                break;
            case SPAWN_FAMILIAR:
                MobHandler.SpawnFamiliar(slea, c, c.getPlayer());
                break;
            case RENAME_FAMILIAR:
                MobHandler.RenameFamiliar(slea, c, c.getPlayer());
                break;
            case MOVE_FAMILIAR:
                MobHandler.MoveFamiliar(slea, c, c.getPlayer());
                break;
            case ATTACK_FAMILIAR:
                MobHandler.AttackFamiliar(slea, c, c.getPlayer());
                break;
            case TOUCH_FAMILIAR:
                MobHandler.TouchFamiliar(slea, c, c.getPlayer());
                break;
            case REVEAL_FAMILIAR:
                break;
            case CP_UserRecipeOpenItemUseRequest:
                ItemMakerHandler.UseRecipe(slea, c, c.getPlayer());
                break;
            case CP_SkillPetMove:
                PlayerHandler.MoveHaku(slea, c, c.getPlayer());
                break;
            case CP_SkillPetAction:
                PlayerHandler.HakuAction(slea, c.getPlayer());
                break;
            case CP_FoxManActionSetUseRequest:
                PlayerHandler.HakuUseBuff(slea, c.getPlayer());
                break;
            case CP_AndroidMove:
                PlayerHandler.MoveAndroid(slea, c, c.getPlayer());
                break;
            case CP_UserEmotion:
                PlayerHandler.ChangeEmotion(slea.readInt(), c.getPlayer());
                break;
            case CP_AndroidEmotion:
                PlayerHandler.ChangeAndroidEmotion(slea.readInt(), c.getPlayer());
                break;
            case CP_UserSelectAndroid:
                PlayerHandler.AndroidShop(slea, c.getPlayer());
                break;
            case CP_UserHit:
                PlayerHandler.TakeDamage(slea, c, c.getPlayer());
                break;
            case CP_UserChangeStatRequest:
                PlayerHandler.Heal(slea, c.getPlayer());
                break;
            case CP_SetSonOfLinkedSkillRequest:
                PlayerHandler.SetSonOfLinkedSkill(slea, c, c.getPlayer());
                break;
            case CP_UserSkillCancelRequest:
                PlayerHandler.CancelBuffHandler(slea.readInt(), c.getPlayer());
                break;
            case CP_UserEffectLocal:
                PlayerHandler.CancelMech(slea, c.getPlayer());
                break;
            case CP_UserSpecialEffectLocal:
                PlayerHandler.spawnSpecial(slea, c, c.getPlayer());
                break;
            case CP_UserStatChangeItemCancelRequest:
                PlayerHandler.CancelItemEffect(slea.readInt(), c.getPlayer());
                break;
            case CP_UserActivateNickItem:
                PlayerHandler.UseTitle(slea.readInt(), c, c.getPlayer());
                break;
            case CP_UserActivateDamageSkin:
                PlayerHandler.UpdateDamageSkin(slea, c.getPlayer());
                break;
            case CP_UserDefaultWingItem:
                PlayerHandler.AngelicChange(slea, c, c.getPlayer());
                break;
            case DRESSUP_TIME:
                PlayerHandler.DressUpTime(slea, c);
                break;
            case CP_UserPortableChairSitRequest:
                PlayerHandler.UseChair(slea, c, c.getPlayer());
                break;
            case CP_UserSitRequest:
                PlayerHandler.CancelChair(slea.readShort(), c, c.getPlayer());
                break;
            case CP_UserMonkeyEffectItem:
                break; // whatever
            case CP_UserActivateEffectItem:
                PlayerHandler.UseItemEffect(slea.readInt(), c, c.getPlayer());
                break;
            case CP_UserSkillPrepareRequest:
                PlayerHandler.SkillEffect(slea, c.getPlayer());
                break;
            case CP_QuickslotKeyMappedModified:
                PlayerHandler.QuickSlot(slea, c.getPlayer());
                break;
            case CP_UserDropMoneyRequest:
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.DropMeso(slea.readInt(), c.getPlayer());
                break;
            case CP_FuncKeyMappedModified:
                PlayerHandler.ChangeKeymap(slea, c.getPlayer());
                break;
            case PET_BUFF:
                PlayerHandler.ChangePetBuff(slea, c.getPlayer());
                break;
            case UPDATE_ENV:
                // We handle this in MapleMap
                break;
            case CP_UserTransferFieldRequest:
                if (c.getPlayer().getOneTempValue("Transfer", "Channel") != null) {
                    CashShopOperation.LeaveCS(slea, c, c.getPlayer());
                } else {
                    PlayerHandler.ChangeMap(slea, c, c.getPlayer());
                }
                break;
            case CP_UserPortalScriptRequest:
                slea.skip(1);
                PlayerHandler.ChangeMapSpecial(slea.readMapleAsciiString(), c, c.getPlayer());
                break;
            case CP_UserPortalTeleportRequest:
                PlayerHandler.InnerPortal(slea, c, c.getPlayer());
                break;
            case CP_UserMapTransferRequest:
                PlayerHandler.TrockAddMap(slea, c, c.getPlayer());
                break;
            case CP_UserAntiMacroItemUseRequest:
            case CP_UserAntiMacroSkillUseRequest:
                PlayersHandler.AntiMacro(slea, c, c.getPlayer(), header == RecvPacketOpcode.CP_UserAntiMacroItemUseRequest);
                break;
            case CP_UserAntiMacroQuestionResult:
            case CP_UserOldAntiMacroQuestionResult:
                PlayersHandler.AntiMacroQuestion(slea, c, c.getPlayer());
                break;
            case CP_UserAntiMacroRefreshRequest:
                PlayersHandler.AntiMacroRefresh(slea, c, c.getPlayer());
                break;
            case CP_RequestIncCombo:
                PlayerHandler.ShowAranCombo(c.getPlayer(), (short) -1);
                break;
            case CP_RequestDecCombo:
                PlayerHandler.AranCombo_Reduce(c.getPlayer());
                break;
            case CP_UserMacroSysDataModified:
                PlayerHandler.ChangeSkillMacro(slea, c.getPlayer());
                break;
            case CP_UserGivePopularityRequest:
                PlayersHandler.GiveFame(slea, c, c.getPlayer());
                break;
            case TRANSFORM_PLAYER:
                PlayersHandler.TransformPlayer(slea, c, c.getPlayer());
                break;
            case CP_MemoFlagRequest:
                PlayersHandler.Note(slea, c.getPlayer());
                break;
            case CP_EnterTownPortalRequest:
                PlayersHandler.UseDoor(slea, c.getPlayer());
                break;
            case CP_EnterOpenGateRequest:
                PlayersHandler.UseMechDoor(slea, c.getPlayer());
                break;
            case CP_ReactorHit:
                PlayersHandler.HitReactor(slea, c);
                break;
            case CLICK_REACTOR:
            case CP_ReactorClick:
                PlayersHandler.TouchReactor(slea, c);
                break;
            case CP_RuneStoneUseReq:
                PlayersHandler.TouchRune(slea, c.getPlayer());
                break;
            case CP_RuneStoneSkillReq:
                PlayersHandler.UseRune(slea, c.getPlayer());
                break;
            case CP_UserADBoardClose:
                c.getPlayer().setChalkboard(null);
                break;
            case CP_UserGatherItemRequest:
                InventoryHandler.ItemSort(slea, c);
                break;
            case CP_UserSortItemRequest:
                InventoryHandler.ItemGather(slea, c);
                break;
            case CP_UserChangeSlotPositionRequest:
                InventoryHandler.ItemMove(slea, c);
                break;
            case CP_UserPopOrPushBagItemToInven:
                InventoryHandler.MoveBag(slea, c);
                break;
            case CP_UserBagToBagItem:
                InventoryHandler.SwitchBag(slea, c);
                break;
            case CP_UserItemMakeRequest:
                ItemMakerHandler.ItemMaker(slea, c);
                break;
            case CP_DropPickUpRequest:
                InventoryHandler.Pickup_Player(slea, c);
                break;
            case CP_UserConsumeCashItemUseRequest:
                InventoryHandler.UseCashItem(slea, c);
                break;
            case CP_UserStatChangeItemUseRequest:
                InventoryHandler.UseItem(slea, c, c.getPlayer());
                break;
            case CP_UserConsumeHairItemUseRequest:
                InventoryHandler.UseCosmetic(slea, c, c.getPlayer());
                break;
            case CP_UserWeaponTempItemOptionRequest:
                c.getPlayer().checkSoulState(false);
                break;
            case CP_UserItemSkillSocketUpgradeItemUseRequest:
                InventoryHandler.UseSoulEnchanter(slea, c, c.getPlayer());
                break;
            case CP_UserItemSkillOptionUpgradeItemUseRequest:
                InventoryHandler.UseSoulScroll(slea, c, c.getPlayer());
                break;
            case CP_UserEquipmentEnchantWithSingleUIRequest:
                EquipmentEnchant.handlePacket(slea, c);
                break;
            case CP_UserItemReleaseRequest:
                InventoryHandler.UseMagnify(slea, c);
                break;
            case CP_UserScriptItemUseRequest:
                InventoryHandler.UseScriptedNPCItem(slea, c, c.getPlayer());
                break;
            case CP_UserPortalScrollUseRequest:
                InventoryHandler.UseReturnScroll(slea, c, c.getPlayer());
                break;
            case USE_NEBULITE:
                InventoryHandler.UseNebulite(slea, c);
                break;
            case USE_ALIEN_SOCKET:
                InventoryHandler.UseAlienSocket(slea, c);
                break;
            case USE_ALIEN_SOCKET_RESPONSE:
                slea.skip(4); // all 0
                c.getSession().writeAndFlush(CCashShop.useAlienSocket(false));
                break;
            case CP_GoldHammerRequest:
                InventoryHandler.UseGoldenHammer(slea, c);
                break;
            case CP_GoldHammerComplete:
                slea.skip(4);
                c.getSession().writeAndFlush(CCashShop.GoldenHammer((byte) 2, slea.readInt()));
                break;
            case CP_PlatinumHammerRequest:
                InventoryHandler.UsePlatinumHammer(slea, c);
                break;
            case USE_NEBULITE_FUSION:
                InventoryHandler.UseNebuliteFusion(slea, c);
                break;
            case CP_UserUpgradeItemUseRequest:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseUpgradeScroll(slea.readShort(), slea.readShort(), slea.readShort(), c, c.getPlayer(),
                        slea.readByte() > 0);
                break;
            case CP_UserUpgradeAssistItemUseRequest:
            case CP_UserHyperUpgradeItemUseRequest:
            case CP_UserItemOptionUpgradeItemUseRequest:
            case CP_UserAdditionalOptUpgradeItemUseRequest:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseUpgradeScroll(slea.readShort(), slea.readShort(), (short) 0, c, c.getPlayer(), false);// slea.readByte()
                // >
                // 0);
                break;
            case USE_ABYSS_SCROLL:
                InventoryHandler.UseAbyssScroll(slea, c);
                break;
            case CP_UserItemSlotExtendItemUseRequest:
                InventoryHandler.UseCarvedSeal(slea, c);
                break;
            case CP_UserFreeMiracleCubeItemUseRequest:
                InventoryHandler.UseCube(slea, c);
                break;
            case USE_FLASH_CUBE:
                InventoryHandler.UseFlashCube(slea, c);
                break;
            case SAVE_DAMAGE_SKIN:
                PlayerHandler.SaveDamageSkin(slea, c);
                break;
            case CHANGE_DAMAGE_SKIN:
                PlayerHandler.ChangeDamageSkin(slea, c);
                break;
            case REMOVE_DAMAGE_SKIN:
                PlayerHandler.RemoveDamageSkin(slea, c);
                break;
            case CP_UserMobSummonItemUseRequest:
                InventoryHandler.UseSummonBag(slea, c, c.getPlayer());
                break;
            case USE_TREASURE_CHEST:
                InventoryHandler.UseTreasureChest(slea, c, c.getPlayer());
                break;
            case CP_UserSkillLearnItemUseRequest:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseSkillBook((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case CP_UserExpConsumeItemUseRequest:
                InventoryHandler.UseExpPotion(slea, c, c.getPlayer());
                break;
            case CP_UserAdditionalSlotExtendItemUseRequest:
                InventoryHandler.UseAdditionalItem(slea, c);
                break;
            case CP_UserTamingMobFoodItemUseRequest:
                InventoryHandler.UseMountFood(slea, c, c.getPlayer());
                break;
            case CP_UserSelectNpcItemUseRequest:
                InventoryHandler.UseRewardItem(slea, c, c.getPlayer());
                break;
            case SOLOMON_EXP:
                InventoryHandler.UseExpItem(slea, c, c.getPlayer());
                break;
            case HYPNOTIZE_DMG:
                MobHandler.HypnotizeDmg(slea, c.getPlayer());
                break;
            case MOB_NODE:
                MobHandler.MobNode(slea, c.getPlayer());
                break;
            case DISPLAY_NODE:
                MobHandler.DisplayNode(slea, c.getPlayer());
                break;
            case CP_MobMove:
                MobHandler.MoveMonster(slea, c, c.getPlayer());
                break;
            case CP_MobApplyCtrl:
                MobHandler.AutoAggro(slea.readInt(), c.getPlayer());
                break;
            case FRIENDLY_DAMAGE:
                MobHandler.FriendlyDamage(slea, c.getPlayer());
                break;
            case CP_UserMedalReissueRequest:
                PlayerHandler.ReIssueMedal(slea, c, c.getPlayer());
                break;
            case CP_MobTimeBombEnd:
                MobHandler.MonsterBomb(slea.readInt(), c.getPlayer());
                break;
            case CP_MobLiftingEnd:
                break;
            case MOB_BOMB:
                MobHandler.MobBomb(slea, c.getPlayer());
                break;
            case CP_UserShopRequest:
                NPCHandler.NPCShop(slea, c, c.getPlayer());
                break;
            case CP_UserSelectNpc:
                NPCHandler.NPCTalk(slea, c, c.getPlayer());
                break;
            case CP_UserCompleteNpcSpeech:
                NPCHandler.NpcQuestAction(slea, c, c.getPlayer());
                break;
            case CP_UserScriptMessageAnswer:
                NPCHandler.NPCMoreTalk(slea, c);
                break;
            case CP_DirectionNodeCollision:
                NPCHandler.DirectionComplete(slea, c);
                break;
            case CP_NpcMove:
                NPCHandler.NPCAnimation(slea, c);
                break;
            case CP_UserQuestRequest:
                NPCHandler.QuestAction(slea, c, c.getPlayer());
                break;
            case CP_MapleStyleBonusRequest:
                NPCHandler.NpcQuestAction(slea, c, c.getPlayer());
                break;
            case TOT_GUIDE:
                break;
            case CP_UserTrunkRequest:
                NPCHandler.Storage(slea, c, c.getPlayer());
                break;
            case CP_UserChat:
                if (c.getPlayer() != null && c.getPlayer().getMap() != null) {
                    c.getPlayer().updateTick(slea.readInt());
                    ChatHandler.GeneralChat(slea.readMapleAsciiString(), slea.readByte(), c, c.getPlayer());
                }
                break;
            case CP_GroupMessage:
                ChatHandler.Others(slea, c, c.getPlayer());
                break;
            case CP_Whisper:
                ChatHandler.Command(slea, c);
                break;
            case CP_Messenger:
                ChatHandler.Messenger(slea, c);
                break;
            case CP_UserAbilityMassUpRequest:
                StatsHandling.AutoAssignAP(slea, c, c.getPlayer());
                break;
            case CP_UserAbilityUpRequest:
                StatsHandling.DistributeAP(slea, c, c.getPlayer());
                break;
            case CP_UserSkillUpRequest:
                StatsHandling.DistributeSP(slea, c, c.getPlayer());
                break;
            case CP_MiniRoom:
                PlayerInteractionHandler.PlayerInteraction(slea, c, c.getPlayer());
                break;
            case CP_BroadcastMsg:
                ChatHandler.AdminChat(slea, c, c.getPlayer());
                break;
            case CP_Admin:
                PlayerHandler.AdminCommand(slea, c, c.getPlayer());
                break;
            case CP_Log:
                CommandProcessor.logCommandToDB(c.getPlayer(), slea.readMapleAsciiString(), "adminlog");
                break;
            case CP_GuildRequest:
                GuildHandler.Guild(slea, c);
                break;
            case CP_GuildResult:
                slea.skip(1);
                GuildHandler.DenyGuildRequest(slea.readMapleAsciiString(), c);
                break;
            case CP_GuildJoinRequest:
                GuildHandler.JoinGuildRequest(slea.readInt(), c);
                break;
            case CP_GuildJoinCancelRequest:
                GuildHandler.JoinGuildCancel(c);
                break;
            case CP_GuildJoinAccept:
                GuildHandler.AddGuildMember(slea, c);
                break;
            case CP_GuildJoinReject:
                GuildHandler.DenyGuildJoin(slea, c);
                break;
            case CP_AllianceRequest:
                AllianceHandler.HandleAlliance(slea, c, false);
                break;
            case CP_AllianceResult:
                AllianceHandler.HandleAlliance(slea, c, true);
                break;
            case CP_MakeEnterFieldPacketForQuickMove:
                NPCHandler.OpenQuickMoveNpc(slea, c);
                break;
            case CP_UserQuickMoveScript:
                NPCHandler.OpenQuickMoveSpecial(slea, c);
                break;
            case CP_RequestEventList:
                PlayerHandler.getEventList(c);
                break;
            case CP_AddAttackReset:
                NPCHandler.OpenZeroQuickMoveSpecial(slea, c);
                break;
            case BBS_OPERATION:
                BBSHandler.BBSOperation(slea, c);
                break;
            case CP_PartyRequest:
                PartyHandler.PartyOperation(slea, c);
                break;
            case CP_PartyResult:
                PartyHandler.DenyPartyRequest(slea, c);
                break;
            case CP_PartyInvitableSet:
                PartyHandler.AllowPartyInvite(slea, c);
                break;
            case CP_FriendRequest:
                BuddyListHandler.BuddyOperation(slea, c);
                break;
            case CP_TalkToTutor:
                UserInterfaceHandler.CygnusSummon_NPCRequest(c);
                break;
            case SHIP_OBJECT:
                UserInterfaceHandler.ShipObjectRequest(slea.readInt(), c);
                break;
            case CP_CashShopCashItemRequest:
                CashShopOperation.BuyCashItem(slea, c, c.getPlayer());
                break;
            case CP_CashShopMemberShopRequest:
                CashShopOperation.sendCSgift(slea, c);
                break;
            case CP_CashShopCheckCouponRequest:
                slea.skip(2);
                String code = slea.readMapleAsciiString();
                CashShopOperation.CouponCode(code, c);
                // CashShopOperation.doCSPackets(c);
                break;
            case CP_CashShopCoodinationRequest:
                CashShopOperation.SwitchCategory(slea, c);
                break;
            case TWIN_DRAGON_EGG:
                System.out.println("TWIN_DRAGON_EGG: " + slea.toString());
                final CashItem item = CashItemFactory.getInstance().getOriginSimpleItem(10003055);
                Item itemz = c.getPlayer().getCashInventory().toItem(item);
                // Aristocat
                // c.getSession().writeAndFlushAndFlush(CSPacket.sendTwinDragonEgg(true, true,
                // 38, itemz, 1));
                break;
            case XMAS_SURPRISE:
                System.out.println("XMAS_SURPRISE: " + slea.toString());
                break;
            case CP_CashShopQueryCashRequest:
                CashShopOperation.CSUpdate(c);
                break;
            case CP_UserRequestCreateItemPot:
                ItemMakerHandler.UsePot(slea, c);
                break;
            case CP_UserRequestRemoveItemPot:
                ItemMakerHandler.ClearPot(slea, c);
                break;
            case CP_UserRequestIncItemPotLifeSatiety:
                ItemMakerHandler.FeedPot(slea, c);
                break;
            case CP_UserRequestCureItemPotLifeSick:
                ItemMakerHandler.CurePot(slea, c);
                break;
            case CP_UserRequestComplateToItemPot:
                ItemMakerHandler.RewardPot(slea, c);
                break;
            case CP_SummonedHit:
                slea.skip(4);
                SummonHandler.DamageSummon(slea, c.getPlayer());
                break;
            case CP_SummonedMove:
                SummonHandler.MoveSummon(slea, c.getPlayer());
                break;
            case CP_DragonMove:
                SummonHandler.MoveDragon(slea, c.getPlayer());
                break;
            case CP_SummonedSkill:
                SummonHandler.SubSummon(slea, c.getPlayer());
                break;
            case CP_Remove:
                SummonHandler.RemoveSummon(slea, c);
                break;
            case CP_UserActivatePetRequest:
                PetHandler.SpawnPet(slea, c, c.getPlayer());
                break;
            case CP_PetMove:
                PetHandler.MovePet(slea, c.getPlayer());
                break;
            case CP_PetAction:
                // System.out.println("Pet chat: " + slea.toString());
                if (slea.available() < 8) {
                    break;
                }
                final int petid = c.getPlayer().getPetIndex(slea.readInt());
                c.getPlayer().updateTick(slea.readInt());
                PetHandler.PetChat(petid, slea.readShort(), slea.readMapleAsciiString(), c.getPlayer());
                break;
            case CP_PetInteractionRequest:
                MaplePet pet;
                pet = c.getPlayer().getSummonedPet(c.getPlayer().getPetIndex(slea.readInt()));
                slea.readByte(); // always 0?
                if (pet == null) {
                    return;
                }
                PetHandler.PetCommand(pet, PetDataFactory.getPetCommand(pet.getPetItemId(), slea.readByte()), c,
                        c.getPlayer());
                break;
            case CP_UserPetFoodItemUseRequest:
                PetHandler.PetFood(slea, c, c.getPlayer());
                break;
            case CP_PetFoodItemUseRequest:
                PetHandler.PetAutoFood(slea, c, c.getPlayer());
                break;
            case CP_PetDropPickUpRequest:
                InventoryHandler.Pickup_Pet(slea, c);
                break;
            case CP_PetStatChangeItemUseRequest:
                PetHandler.Pet_AutoPotion(slea, c, c.getPlayer());
                break;
            case MONSTER_CARNIVAL:
                MonsterCarnivalHandler.MonsterCarnival(slea, c);
                break;
            case CP_UserParcelRequest:
                PackageHandler.handleAction(slea, c);
                break;
            case CP_UserEntrustedShopRequest:
                HiredMerchantHandler.UseHiredMerchant(c, true);
                break;
            case CP_UserStoreBankRequest:
                HiredMerchantHandler.MerchantItemStore(slea, c);
                break;
            case CP_UserTemporaryStatUpdateRequest:
                // Ignore for now
                break;
            case MAPLETV:
                break;
            case CP_SnowBallTouch:
                PlayerHandler.leftKnockBack(slea, c);
                break;
            case CP_SnowBallHit:
                PlayerHandler.snowBall(slea, c);
                break;
            case COCONUT:
                PlayersHandler.hitCoconut(slea, c);
                break;
            case CP_UserRepairDurability:
                NPCHandler.repair(slea, c);
                break;
            case CP_UserRepairDurabilityAll:
                NPCHandler.repairAll(c);
                break;
            case BUY_SILENT_CRUSADE:
                PlayersHandler.buySilentCrusade(slea, c);
                break;
            // case GAME_POLL:
            // UserInterfaceHandler.InGame_Poll(slea, c);
            // break;
            case CP_ShopScannerRequest:
                InventoryHandler.Owl(slea, c);
                break;
            case CP_ShopLinkRequest:
                InventoryHandler.OwlWarp(slea, c);
                break;
            case CP_UserShopScannerItemUseRequest:
                InventoryHandler.OwlMinerva(slea, c);
                break;
            case CP_RPSGame:
                NPCHandler.RPSGame(slea, c);
                break;
            case UPDATE_QUEST:
                NPCHandler.UpdateQuest(slea, c);
                break;
            case USE_ITEM_QUEST:
                NPCHandler.UseItemQuest(slea, c);
                break;
            case CP_UserFollowCharacterRequest:
                PlayersHandler.FollowRequest(slea, c);
                break;
            case CP_SetPassenserResult:
            case CP_UserRequestPQReward:
                PlayersHandler.FollowReply(slea, c);
                break;
            case CP_MarriageRequest:
                PlayersHandler.RingAction(slea, c);
                break;
            case SOLOMON:
                PlayersHandler.Solomon(slea, c);
                break;
            case GACH_EXP:
                PlayersHandler.GachExp(slea, c);
                break;
            case CP_PartyMemberCandidateRequest:
                PartyHandler.MemberSearch(slea, c);
                break;
            case CP_PartyCandidateRequest:
                PartyHandler.PartySearch(slea, c);
                break;
            case CP_PartyAdverRequest:
                PartyHandler.PartyListing(slea, c);
                break;
            case CP_ExpeditionRequest:
                PartyHandler.Expedition(slea, c);
                break;
            case CP_UserMapTransferItemUseRequest:
                InventoryHandler.TeleRock(slea, c);
                break;
            case CP_UserRequestRespawn:
                PlayersHandler.reviveAzwan(slea, c);
                break;
            case PLAYER_UPDATE:
                PlayerHandler.PlayerUpdate(c.getPlayer());
                break;
            case PAM_SONG:
                InventoryHandler.PamSong(slea, c);
                break;
            case CP_UserClaimRequest:
                PlayersHandler.Report(slea, c);
                break;
            // working
            case CP_UserRequestStealSkill:
                slea.readInt();
                break;
            // working
            case CP_UserRequestStealSkillList:
                PlayersHandler.viewSkills(slea, c);
                break;
            // working
            case CP_UserRequestStealSkillMemory:
                PlayersHandler.StealSkill(slea, c);
                break;
            case CP_UserRequestSetStealSkillSlot:
                PlayersHandler.ChooseSkill(slea, c);
                break;
            case CP_UserRequestFlyingSwordStart:
                PlayerHandler.releaseTempestBlades(slea, c.getPlayer());
                break;
            case MAGIC_WHEEL:
                System.out.println("[MAGIC_WHEEL] [" + slea.toString() + "]");
                PlayersHandler.magicWheel(slea, c);
                break;
            case REWARD:
                PlayersHandler.onReward(slea, c);
                break;
            case BLACK_FRIDAY:
                PlayersHandler.blackFriday(slea, c);
            case UPDATE_RED_LEAF:
                PlayersHandler.updateRedLeafHigh(slea, c);
                break;
            case CP_UserHyperStatSkillUpRequest:
                StatsHandling.DistributeHyperStat(slea, c, c.getPlayer());
                break;
            case CP_UserHyperSkillUpRequest:
                StatsHandling.DistributeHyper(slea, c, c.getPlayer());
                break;
            case CP_UserHyperSkillResetRequset:
                StatsHandling.ResetHyper(slea, c, c.getPlayer());
                break;
            case CP_UserBanMapByMob:
                PlayerHandler.MonsterBan(slea, c.getPlayer());
                break;
            case CP_UserForceAtomCollision:
                PlayerHandler.UserForceAtomCollision(slea, c);
                break;
            case CP_UserRequestCharacterPotentialSkillRandSet:
                PlayerHandler.RequestCharacterPotentialSkillRandSet(slea, c, c.getPlayer());
                break;
            case CP_UserRequestCharacterPotentialSkillRandSetUI:
                PlayerHandler.RequestCharacterPotentialSkillRandSetUI(slea, c, c.getPlayer());
                break;
            case CP_CheckTrickOrTreatRequest:
                c.getSession().writeAndFlush(CWvsContext.enableActions());
                break;
            case EXIT_GAME:
                if (!ServerConstants.REDIRECTOR) {
                    c.getSession().writeAndFlush(CField.exitGame());
                }
                break;
            case CP_RequestReloginCookie:
                InterServerHandler.BackToCharList(slea, c);
                break;
            case MESSENGER_RANKING:
                PlayerHandler.MessengerRanking(slea, c, c.getPlayer());
                break;
            case OS_INFORMATION:
                System.out.println(c.getSessionIPAddress());
                break;
            case CP_UserCalcDamageStatSetRequest:// wat does it do?
                break;
            case CP_PassiveskillInfoUpdate:
                break;
            case CASSANDRAS_COLLECTION:
                PlayersHandler.CassandrasCollection(slea, c);
                break;
            case CP_UserMobDropMesoPickup:
                PlayersHandler.LuckyLuckyMonstory(slea, c);
                break;
            case CHRONOSPHERE:
                PlayersHandler.UseChronosphere(slea, c, c.getPlayer());
                break;
            case LP_GuildTransfer:
            case LP_GuildTransfer2:
                PlayerHandler.GuildTransfer(slea, c, c.getPlayer());// 遊戲嚮導
                break;
            case CP_UserCashPetPickUpOnOffRequest:
                PetHandler.AllowPetLoot(slea, c, c.getPlayer());
                break;
            case CP_UserDestroyPetItemRequest:
                PlayerHandler.DestroyPetItem(slea, c, c.getPlayer());
                break;
            case CP_RequestArrowPlaterObj:
                PlayerHandler.ArrowBlasterAction(slea, c, c.getPlayer());
                break;
            case CP_CheckProcess:
                SystemProcess.SystemProcess(slea, c, c.getPlayer());
                break;
            case CP_EgoEquipGaugeCompleteReturn:
                PlayerHandler.ZeroHandler.ZeroScrollStart(slea, c.getPlayer(), c);
                break;
            case CP_EgoEquipCreateUpgradeItemCostRequest:
                PlayerHandler.ZeroHandler.openWeaponUI(slea, c);
                break;
            case CP_EgoEquipTalkRequest:
                PlayerHandler.ZeroHandler.talkZeroNpc(slea, c);
                break;
            case CP_EgoEquipCheckUpdateItemRequest:
                PlayerHandler.ZeroHandler.useZeroScroll(slea, c);
                break;
            case CP_InheritanceInfoRequest:
                PlayerHandler.ZeroHandler.openZeroUpgrade(slea, c);
                break;
            case CP_InheritanceUpgradeRequest:
                break;
            case CP_EgoEquipCreateUpgradeItem:
                break;
            case CP_UserUpdateMapleTVShowTime:
                PlayerHandler.LoadPlayerSuccess(c, c.getPlayer());
                break;
            case ATTACK_ON_TITAN_SELECT:
                if (c.getPlayer().getMapId() != 814000000) {
                    break;
                }
                int select = slea.readInt();
                switch (select) {
                    case 0:
                        select = 814000600;
                        break;
                    case 1:
                        select = 814010000;
                        break;
                    default:
                        select = 814030000;
                        break;
                }
                final MapleMap mapz = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(select);
                c.getPlayer().changeMap(mapz, mapz.getPortal(0));
                break;
            case EFFECT_SWITCH:
                PlayerHandler.EffectSwitch(slea, c);
                break;
            case CP_UserPinkbeanYoYoStack:
                PlayerHandler.UpdatePinkbeenYoyo(c);
                break;
            case CLICK_BINGO_CARD:
                PlayersHandler.clickBingoCard(slea, c);
                break;
            case PRESS_BINGO:
                PlayersHandler.pressBingo(slea, c);
                break;
            case BINGO:
                PlayersHandler.openBingo(slea, c);
                break;
            case CP_UseFamiliarCard: {
                PlayerHandler.useFamiliarCard(slea, c, c.getPlayer());
                break;
            }
            case BOSS_LIST:
                PlayersHandler.GoBossListEvent(slea, c);
                break;
            case CP_WaitQueueRequest:
                PlayersHandler.GoBossListWait(slea, c);
                break;
            case CP_UserUpdateMatrix:
                PlayerHandler.OnUpdateMatrix(slea, c, c.getPlayer());
                break;
            default:
                System.err.println("[發現未處理數據包] Recv [" + header.toString() + "]");
                break;
        }
    }

    public ServerType getServerType() {
        return this.serverType;
    }
}
