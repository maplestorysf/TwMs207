package client;

import database.ManagerDatabasePool;
import tools.packet.CWvsContext.BuddyListPacket;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class BuddyList implements Serializable {

    private static final long serialVersionUID = 1413738569L;
    private final Map<Integer, BuddylistEntry> buddies;
    private byte capacity;
    private boolean changed = false;

    public BuddyList(byte capacity) {
        this.buddies = new LinkedHashMap<>();
        this.capacity = capacity;
    }

    public boolean contains(int characterId) {
        return buddies.containsKey(characterId);
    }

    public boolean containsVisible(int characterId) {
        BuddylistEntry ble = buddies.get(characterId);
        if (ble == null) {
            return false;
        }
        return ble.isVisible();
    }

    public byte getCapacity() {
        return capacity;
    }

    public void setCapacity(byte capacity) {
        this.capacity = capacity;
    }

    public BuddylistEntry get(int characterId) {
        return buddies.get(characterId);
    }

    public BuddylistEntry get(String characterName) {
        String lowerCaseName = characterName.toLowerCase();
        for (BuddylistEntry ble : buddies.values()) {
            if (ble.getName().toLowerCase().equals(lowerCaseName)) {
                return ble;
            }
        }
        return null;
    }

    public void put(BuddylistEntry entry) {
        buddies.put(entry.getCharacterId(), entry);
        changed = true;
    }

    public void remove(int characterId) {
        buddies.remove(characterId);
        changed = true;
    }

    public Collection<BuddylistEntry> getBuddies() {
        return buddies.values();
    }

    public boolean isFull() {
        return buddies.size() >= capacity;
    }

    public int[] getBuddyIds() {
        int buddyIds[] = new int[buddies.size()];
        int i = 0;
        for (BuddylistEntry ble : buddies.values()) {
            if (ble.isVisible()) {
                buddyIds[i++] = ble.getCharacterId();
            }
        }
        return buddyIds;
    }

    public void loadFromTransfer(final Map<CharacterNameAndId, Boolean> data) {
        CharacterNameAndId buddyid;
        for (final Map.Entry<CharacterNameAndId, Boolean> qs : data.entrySet()) {
            buddyid = qs.getKey();
            put(new BuddylistEntry(buddyid.getName(), buddyid.getId(), buddyid.getGroup(), -1, qs.getValue()));
        }
    }

    public void loadFromDb(int characterId) throws SQLException {
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT b.buddyid, b.pending, c.name as buddyname, b.groupname FROM buddies as b, characters as c WHERE c.id = b.buddyid AND b.characterid = ?")) {
                ps.setInt(1, characterId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        put(new BuddylistEntry(rs.getString("buddyname"), rs.getInt("buddyid"),
                                rs.getString("groupname"), -1, rs.getInt("pending") != 1));
                    }
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException se) {

        }
    }

    public void addBuddyRequest(MapleClient c, int cidFrom, String nameFrom, int channelFrom, int levelFrom,
            int jobFrom, int AccID) {
        BuddylistEntry buddy = new BuddylistEntry(nameFrom, cidFrom, "群組未指定", channelFrom, false);
        put(buddy);
        // 把請求好友的人加進列表裡
        c.getSession()
                .writeAndFlush(BuddyListPacket.requestBuddylistAdd(false, AccID, nameFrom, levelFrom, jobFrom, buddy));
        // 發送好友請求
        c.getSession().writeAndFlush(BuddyListPacket.addBuddy(buddy));
    }

    public void setChanged(boolean v) {
        this.changed = v;
    }

    public boolean changed() {
        return changed;
    }

    public static enum BuddyAddResult {

        // 好友列表已滿
        BUDDYLIST_FULL,
        // 已是好友關係
        ALREADY_ON_LIST,
        // 添加好友成功
        OK,
        // 添加好友成功
        NO
    }

    public static enum BuddyOperation {

        更新好友列表(0x00), UNK_1(0x01), 列表新增(0x02),
        // (+ %s)給朋友提出申請
        好友申請(0x03),
        // 好友目錄已經滿了。
        好友满(0x04),
        // 對方的好友目錄已經滿了。
        對方好友滿(0x05),
        // 已經是好友。
        已是好友(0x06),
        // 帳號朋友請求中.
        已申請帳號好友(0x07),
        // 這是登入在朋友等待列中的玩家.
        等待通過好友申請(0x08),
        // 不能把自己加入到好友。
        不能加自己(0x09),
        // 不能把管理員加入好友。
        不能加管理員(0x0A),
        // 沒登入的角色。
        角色不存在(0x0B),
        // 因為發生未知錯誤,你的請求無法處理。
        未知錯誤(0x0C),
        // 我有保留對方的目錄.
        有保留目錄(0x0D), UNK_E(0x0E), 增加好友(0x0F),
        // 因為發生未知錯誤,你的請求無法處理。
        未知錯誤2(0x10), 刪除好友(0x11),
        // 因為發生未知錯誤,你的請求無法處理。
        未知錯誤3(0x12), 更新好友頻道(0x13),
        // 0x14
        好友容量(0x15),
        // 因為發生未知錯誤,你的請求無法處理。
        未知錯誤4(0x16),
        // 0x17
        // 0x18
        拒絕好友申請(0x19),
        // (+ %s)'還沒有找回完整的力量無法建立好友.
        沒有找回力量(0x1A),
        // (+ %s)的朋友拒絕請求.
        的朋友拒絕請求(0x1B),
        // (+ %s)'玩家的\r\n邀請好友。 o|x
        玩家的邀請好友(0x1C),
        // (+ %s)拒絕好友邀請。
        拒絕好友邀請(0x1D),
        // 0x1E
        // 0x1F
        // 0x20
        // 0x21
        // 0x22
        // 0x23
        // 好友目錄已經滿了。
        好友满2(0x24),
        // 阻擋目錄已經滿了
        阻擋滿(0x25),
        // 0x26
        // 已經是好友。
        已是好友2(0x27),
        // 帳號朋友請求中.
        已申請帳號好友2(0x28),
        // 未登錄過的角色或是在新星世界中未上線的玩家.
        未登錄過(0x29),
        // 因為發生未知錯誤,你的請求無法處理。
        未知錯誤5(0x2A),
        // 不能把管理員加入好友。
        不能加管理員2(0x2B),
        // (+ %s)目前不在線上所以無法登錄為好友.
        不在線無法加(0x2C),
        // 0x2D
        // 0x2E
        // 0x2F
        // 因為發生未知錯誤,你的請求無法處理。
        未知錯誤6(0x30),
        // 0x31
        // 0x32
        // 0x33
        // 0x34
        UNK_35(0x35),
        // 好友的追隨功能為拒絕狀態.
        拒絕追隨狀態(0x36),
        // 無法找到好友.
        無法找到好友(0x37),
        // 因不明原因造成追隨功能失敗.
        追隨功能失敗(0x38),;

        private int code;

        private BuddyOperation(int code) {
            this.code = code;
        }

        public int getValue() {
            return code;
        }
    }
}
