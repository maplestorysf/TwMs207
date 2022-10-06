package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.PartTimeJob;
import constants.GameConstants;
import constants.JobConstants;
import constants.JobConstants.LoginJob;
import constants.ServerConfig;
import constants.ServerConstants;
import constants.ServerConstants.ServerType;
import constants.WorldConstants;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import java.io.File;
import java.util.List;
import java.util.Set;
import tools.DateUtil;
import tools.HexTool;
import tools.OpcodeEncryption;
import tools.data.MaplePacketLittleEndianWriter;

public class LoginPacket {

    public static byte[] getHello(byte[] sendIv, byte[] recvIv) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(0x2E);
        mplew.writeShort(ServerConstants.MAPLE_VERSION);
        mplew.writeMapleAsciiString(ServerConstants.MAPLE_LOGIN_PATCH);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(ServerConstants.MAPLE_TYPE.getType());

        mplew.write(0);
        mplew.writeShort(ServerConstants.MAPLE_VERSION);
        mplew.writeShort(ServerConstants.MAPLE_VERSION);
        mplew.writeShort(0);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(ServerConstants.MAPLE_TYPE.getType());
        mplew.writeInt(Integer.parseInt(ServerConstants.MAPLE_PATCH));
        mplew.writeInt(Integer.parseInt(ServerConstants.MAPLE_PATCH));
        mplew.writeInt(0);
        mplew.writeShort(1);

        return mplew.getPacket();
    }

    public static byte[] getHelloOld(byte[] sendIv, byte[] recvIv) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(0x0E);
        mplew.writeShort(ServerConstants.MAPLE_VERSION);
        mplew.writeMapleAsciiString(ServerConstants.MAPLE_PATCH);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(ServerConstants.MAPLE_TYPE.getType());

        return mplew.getPacket();
    }

    public static final byte[] showMapleStory() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(6);

        mplew.writeShort(SendPacketOpcode.SHOW_MAPLESTORY.getValue());
        mplew.write(1);

        return mplew.getPacket();
    }

    public static final byte[] getPing() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);

        mplew.writeShort(SendPacketOpcode.LP_AliveReq.getValue());

        return mplew.getPacket();
    }

    public static byte[] getLoginBackground() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_AUTH.getValue());
        // UI.wz/MapLogin.img ... MapLogin2.img
        String[] bg = {"MapLogin", "MapLogin0", "MapLogin1", "MapLogin2"};
        mplew.writeMapleAsciiString(bg[(int) (Math.random() * bg.length)]);
        mplew.writeInt(GameConstants.getCurrentDate());
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] getAuthSuccessRequest(MapleClient client) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CheckPasswordResult.getValue());
        getAuthSuccess(mplew, client, false);

        return mplew.getPacket();
    }

    public static final byte[] getSecondAuthSuccess(MapleClient client) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AccountInfoResult.getValue());
        getAuthSuccess(mplew, client, true);

        return mplew.getPacket();
    }

    public static final void getAuthSuccess(MaplePacketLittleEndianWriter mplew, MapleClient client, boolean second) {
        mplew.write(0);
        if (!second) {
            mplew.writeMapleAsciiString(client.getAccountName());
        }
        mplew.writeInt(client.getAccID());
//        mplew.write(client.getGender());
        /* 是否為管理員帳號
         * 1 - 不受地圖使用位移技能限制
         * 2 - 可以使用/前綴指令
         * 3 - 不受部分異常狀態/怪物BUFF影響
         */
        mplew.write(client.isGM());

        int accountStuff = 0;
        boolean bManagerAccount = client.isSuperGM();
        if (bManagerAccount) {
            /* 管理員帳號
             * 限制某些效果(例如反擊怪物)
             * 當前面設置為管理員帳號後不開啟這個無法扔道具
             */
            accountStuff |= 0x10;
        }
        boolean bUnkAccount = client.isGM();
        if (bUnkAccount) {
            /* 未知帳號
             * 不顯示首次進入遊戲顯示網頁
             */
            accountStuff |= 0x20000;
        }
        boolean bTesterAccount = false;
        if (bTesterAccount) {
            /* 測試帳號
             * 效果未知
             */
            accountStuff |= 0x20;
        }
        boolean bSubTesterAccount = false;
        if (bSubTesterAccount) {
            /* 測試子帳號
             * 效果未知
             */
            accountStuff |= 0x2000;
        }
        mplew.writeInt(accountStuff);

        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0x24);
        mplew.write(0);
        mplew.write(0); // 1 = 帳號禁止說話
        mplew.writeLong(0); // 禁止說話期限
        mplew.write(0);
        mplew.writeLong(0);
        if (second) {
            mplew.writeMapleAsciiString(client.getAccountName());
        } else {
            boolean unkBool = false;
            mplew.write(false);
        }
        mplew.writeMapleAsciiString(client.getSecurityAccountName());
        mplew.writeMapleAsciiString("");
        mplew.write(JobConstants.enableJobs);
        if (JobConstants.enableJobs) {
            mplew.write(JobConstants.jobOrder);
            for (LoginJob j : LoginJob.values()) {
                mplew.write(j.enableCreate());
                mplew.writeShort(1);
            }
        }
        mplew.write(0);// 176+
        mplew.writeInt(-1);// 176+
        mplew.write(0);
    }

    /*
     * location: UI.wz/Login.img/Notice/text reasons: useful: 32 - server under
     * maintenance check site for updates 35 - your computer is running thirdy part
     * programs close them and play again 36 - due to high population char creation
     * has been disabled 43 - revision needed your ip is temporary blocked 75-78 are
     * cool for auto register
     */
    public static final byte[] getLoginFailed(int reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        mplew.writeShort(SendPacketOpcode.LP_CheckPasswordResult.getValue());
        mplew.write(reason);

        return mplew.getPacket();
    }

    /*
     * string = 從%s年%s月%s日%s時以後才可以登入遊戲。若多次遭鎖定，將有可能直接進行終止遊戲合約。
     * 
     * reason: 0 - 請確認您的Gash帳號與遊戲帳號是否為正常狀態。 1 -
     * 因確認出現辱罵、騷擾言語、淫亂字眼而帶給他人不良感受或公然侮辱、誹謗他人而被鎖定的賬號。 2 -
     * 未受到Nexon或新楓之谷正式許可後，進行宣傳活動而被鎖定ID。 3 - 查看如下內容時發現有違反運營政策行為被鎖定的ID。 1.用現金交易道具或是楓幣
     * 2.交易Gash點數或楓之谷帳號 4 - 查看如下內容時發現 有違反運營政策 行為被鎖定的ID. 1.使用不是公司提供或是不允許的電腦程式
     * 2.迴避遊戲資安系統 3.竄改遊戲檔案或是DATA 4.重複亂檢舉測謊機 5 - 查看如下內容時發現有違反運營政策行為被鎖定的ID。
     * 1.Gash點數/遊戲金幣/遊戲道具詐騙 2.散佈外掛網址 6 - 因為確認未成年者付款、帳號盜用等的非正常方法來購買Gash點數的記錄而被鎖定的帳號。
     * 7 - 查看如下內容時發現 有違反運營政策 暫時性限制使用遊戲的帳號。 1.重複跟違反運營政策的使用者交易楓幣、道具
     * 2.跟違反運營商政策的使用者組隊並不當獲得經驗值 8 - (同1) 9 - 因正在處理以下內容中的一種事項而無法進行遊戲。 1.一對一詢問要求的內容處理
     * 2.正在對遊戲中發生的問題進行修正 3.確認到非正常的遊戲記錄或違反運營政策事項而進行調查 若在30分內，無法解除的話，請使用官網一對一詢問功能。。 10
     * - 查看如下內容時發現有違反運營政策行為被鎖定的ID。 1.使用手機、信用卡、商品券等，可以儲值Gash點數方法的詐騙
     * 2.使用手機、信用卡、商品券等，可以儲值Gash點數方法來交易道具或楓幣 11 - 違反官方所訂的使用者規章等遊戲規定 12 - (同4) 13 -
     * 確認到使用外掛或是盜用遊戲資料行為而鎖定的ID. 14 - 此遊戲帳號因使用不法程式，已經由系統自動鎖定，鎖定資訊及相關說明請參見楓之谷官網公告。 15
     * - 暴露其他顧客的個人資料(名稱、地址、所屬、電話號碼、IP等)而被鎖定的帳號。 16 - 查看如下內容時發現有違反運營政策行為被鎖定的ID。
     * 1.遊戲內進行、誘導、宣傳、支援、協助賭錢等賭博行為。 2.遊戲內參加賭錢等賭博行為 3.用個人影片播放遊戲內進行的賭錢等賭博行為 17 - (同16)
     * 18 - 查看如下內容時發現有違反運營政策行為被鎖定的ID。 1.遊戲內使用Bug 2.跟使用Bug的對象協助搬移道具 3.跟其他玩家說明使用Bug的方法
     * 19 - (同18) 20 - (同18) 大於20都同4
     */
    public static byte[] getTempBan(long timestampTill, byte reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);

        mplew.writeShort(SendPacketOpcode.LP_CheckPasswordResult.getValue());
        mplew.write(2);
        mplew.write(reason);
        mplew.writeLong(timestampTill);

        return mplew.getPacket();
    }

    public static byte[] getWorldSelected(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        byte lastWorld = c.getAccountWorld(c.getAccountName());
        mplew.writeShort(SendPacketOpcode.LP_LatestConnectedWorld.getValue());
        mplew.writeInt(lastWorld == 0 ? -3 : lastWorld);

        return mplew.getPacket();
    }

    public static final byte[] deleteCharResponse(int cid, int state) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_DeleteCharacterResult.getValue());
        mplew.writeInt(cid);
        mplew.write(state);

        return mplew.getPacket();
    }

    public static final byte[] createCharResponse(int state) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CheckSPWOnCreateNewCharacterResult.getValue());
        mplew.write(state);
        switch (state) {
            case 0x0:
                mplew.write(JobConstants.enableJobs);
                if (JobConstants.enableJobs) {
                    mplew.write(JobConstants.jobOrder);
                    for (LoginJob j : LoginJob.values()) {
                        mplew.write(j.enableCreate());
                        mplew.writeShort(1);
                    }
                }
                break;
        }

        return mplew.getPacket();
    }

    public static final byte[] createCharCheckCode(File file, byte value1, byte value2, byte value3, byte value4) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CheckSPWOnCreateNewCharacterResult.getValue());
        mplew.write(0x45);
        mplew.write(value1);
        mplew.write(value2);
        mplew.write(value3);
        mplew.write(value4);
        mplew.writeFile(file);

        return mplew.getPacket();
    }

    public static byte[] secondPwError(byte mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.LP_CheckSPWResult.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] sendAuthResponse(int response) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AUTH_RESPONSE.getValue());
        mplew.writeInt(response);

        return mplew.getPacket();
    }

    public static byte[] enableRecommended(int world) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_LatestConnectedWorld.getValue());
        mplew.writeInt(world);

        return mplew.getPacket();
    }

    public static byte[] sendRecommended(int world, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_RecommendWorldMessage.getValue());
        mplew.write(message != null ? 1 : 0);
        if (message != null) {
            mplew.writeInt(world);
            mplew.writeMapleAsciiString(message);
        }
        return mplew.getPacket();
    }

    public static byte[] ResetScreen() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RESET_SCREEN.getValue());
        mplew.write(HexTool
                .getByteArrayFromHexString("02 08 00 32 30 31 32 30 38 30 38 00 08 00 32 30 31 32 30 38 31 35 00"));

        return mplew.getPacket();
    }

    public static byte[] onWorldInformation(WorldConstants.Option world) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WorldInformation.getValue());
        mplew.write(world.getWorld());
        mplew.writeMapleAsciiString(LoginServer.getServerName());
        mplew.write(world.getFlag());
        mplew.writeMapleAsciiString(world.getWorldTip());
        int lastChannel = 1;
        Set<Integer> channels = ChannelServer.getAllInstance();
        for (int i = world.getChannelCount(); i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        mplew.write(lastChannel);
        for (int i = 1; i <= lastChannel; i++) {
            int load = 0;
            if (channels.contains(i)) {
                for (MapleCharacter player : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                    if (player != null && player.getWorld() == world.getWorld()) {
                        load++;
                    }
                }
            } else {
                load = 1200;
            }
            mplew.writeMapleAsciiString(world.name() + "-" + i);
            mplew.writeInt(ServerConfig.CHANNEL_MAX_CHAR_VIEW == 0 ? 0 : Math.max(load * 55 / ServerConfig.CHANNEL_MAX_CHAR_VIEW, 1));
            mplew.write(world.getWorld());
            mplew.write(i - 1);
            mplew.write(0);
        }
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] onWorldInformation() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_WorldInformation.getValue());
        ;
        mplew.write(-1);
        mplew.write(0);
        mplew.write(0); // Boolean [0:-、1:遊戲帳號保護政策]
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getServerStatus(int status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 類型(status)： [1] 您所選擇的伺服器人數較多，建議您選擇其他伺服器來創建角色或進行遊戲！ [2]
         * 您所選擇的伺服器人數已滿，請您選擇其他伺服器來創建角色或進行遊戲！
         */
        mplew.writeShort(SendPacketOpcode.SERVERSTATUS.getValue());
        mplew.write(status);

        return mplew.getPacket();
    }

    public static byte[] getChannelSelected() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SetPhysicalWorldID.getValue());
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] OnSelectWorldResult(String worldType, String secondpw, List<Integer> charPos, List<MapleCharacter> chars, int charslots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_SelectWorldResult.getValue());
        int unk = 0;
        mplew.write(unk);
        if (unk != 0 && unk != 12) {
            return mplew.getPacket();
        }

        // true ==> Recv 0x00B5
        mplew.write(false);
        mplew.writeMapleAsciiString(worldType);
        mplew.writeInt(4);
        mplew.write("reboot".equals(worldType));
        mplew.writeInt(0); // 176+閃耀之星數量
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));// 176+
        for (int i = 0; i < 0; i++) { // 閃耀之星數量循環
            mplew.writeInt(0);// 176+
            mplew.writeLong(0);// 176+
        }
        mplew.write(0);
        mplew.writeInt(charPos.size());
        // 角色順序
        charPos.forEach(mplew::writeInt);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            AvatarData__Decode(mplew, chr);

            mplew.write(0);

            boolean ranking = !chr.isIntern() && chr.getLevel() >= 30;
            mplew.write(ranking ? 1 : 0);
            if (ranking) {
                mplew.writeInt(chr.getRank()); // 全體排行名次
                mplew.writeInt(chr.getRankMove()); // 全體排行升降情況 <0下降 =0 持平 >0 上升
                mplew.writeInt(chr.getWorldRank()); // 世界排行名次
                mplew.writeInt(chr.getWorldankMove()); // 世界排行升降情況 <0下降 =0 持平 >0 上升
            }
        }
        mplew.write(3);
        mplew.write(secondpw != null && secondpw.length() > 0);// 第二組密碼
        mplew.write(1);
        mplew.writeInt(charslots);
        mplew.writeInt(0); // 50級角色卡角色數量
        int nEventNewCharJob = -1;
        mplew.writeInt(nEventNewCharJob);
        boolean fireAndice = false; // 變更角色名稱開關(在角色上方的)
        mplew.write(fireAndice);
        if (fireAndice) {
            mplew.writeLong(DateUtil.getFileTimestamp(130977216000000000L)); // 開始
            mplew.writeLong(DateUtil.getFileTimestamp(130990175990000000L)); // 結束
            int c_size = 0;
            mplew.writeInt(c_size);
            if (c_size > 0) {
                mplew.writeInt(0);
            }
        }

        mplew.writeReversedLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        mplew.write(0); // 變更角色名稱個數(在名字下方的) m_nRenameCount
        mplew.write(0); // 協議書開關[-1:開、0:關]
        //mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeReversedLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));

        return mplew.getPacket();
    }

    public static byte[] OnCreateNewCharacterResult(MapleCharacter chr, boolean worked) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CreateNewCharacterResult.getValue());
        mplew.write(worked ? 0 : 1);
        AvatarData__Decode(mplew, chr);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] charNameResponse(String charname, boolean nameUsed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_CheckDuplicatedIDResult.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);

        return mplew.getPacket();
    }

    private static void AvatarData__Decode(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        PacketHelper.GW_CharacterStat__Decode(mplew, chr, true);
        mplew.writeInt(0);
        if (MapleJob.is神之子(chr.getJob())) {
            PacketHelper.AvatarLook__Decode(mplew, chr, true, true);
        }
        PacketHelper.AvatarLook__Decode(mplew, chr, true, false);
    }

    public static byte[] enableSpecialCreation(int accid, boolean enable) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPECIAL_CREATION.getValue());
        mplew.writeInt(accid);
        mplew.write(enable ? 0 : 1);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] partTimeJob(int cid, short type, long time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_AlbaRequestResult.getValue());
        mplew.writeInt(cid);
        mplew.write(0);
        mplew.write(type);
        // 1) 0A D2 CD 01 70 59 9F EA
        // 2) 0B D2 CD 01 B0 6B 9C 18
        mplew.writeReversedLong(DateUtil.getFileTimestamp(time));
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] updatePartTimeJob(PartTimeJob partTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(21);

        mplew.writeShort(SendPacketOpcode.LP_AlbaRequestResult.getValue());
        mplew.writeInt(partTime.getCharacterId());
        mplew.write(0);
        PacketHelper.addPartTimeJob(mplew, partTime);

        return mplew.getPacket();
    }

    public static byte[] sendLink() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SEND_LINK.getValue());
        mplew.write(1);
        mplew.write(ServerConstants.getGateway_IP());
        mplew.writeShort(0x2057);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] genderNeeded() {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.CHOOSE_GENDER.getValue());
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] genderChanged(boolean success) {

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.GENDER_SET.getValue());
        mplew.write(success);

        return mplew.getPacket();
    }

    public static byte[] secondPasswordWindows(int unk1, int showChangePaswBtn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(4);

        mplew.writeShort(SendPacketOpcode.LP_CheckSPWExistResult.getValue());
        mplew.write(unk1);
        mplew.write(showChangePaswBtn);

        return mplew.getPacket();
    }

    public static byte[] onClientOpcodeEncyption(int blockSize, byte[] data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LP_ClientPacketOpocde.getValue());
        mplew.writeInt(blockSize);
        mplew.writeInt(data.length);
        mplew.write(data);

        return mplew.getPacket();
    }
}
