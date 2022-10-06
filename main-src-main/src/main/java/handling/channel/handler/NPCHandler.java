package handling.channel.handler;

import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.MapleClient;
import client.MapleCharacter;
import constants.GameConstants;
import client.MapleQuestStatus;
import client.RockPaperScissors;
import client.inventory.ItemFlag;
import constants.FeaturesConfig;
import constants.ItemConstants;
import constants.QuickMove;
import extensions.temporary.ScriptMessageType;
import extensions.temporary.ShopOpcode;
import handling.SendPacketOpcode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.shops.MapleShop;
import server.MapleInventoryManipulator;
import server.MapleStorage;
import server.life.MapleNPC;
import server.quest.MapleQuest;
import scripting.NPCScriptManager;
import scripting.NPCConversationManager;
import scripting.ScriptType;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import tools.packet.CField;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CWvsContext;

public class NPCHandler {

    public static void NPCAnimation(LittleEndianAccessor slea, MapleClient c) {
        if (slea.available() < 10) {
            return;
        }

        if (c.getPlayer() == null) {
            return;
        }
        MapleMap map = c.getPlayer().getMap();
        if (map == null) {
            return;
        }

        int NPCOid = slea.readInt();
        MapleNPC npc = map.getNPCByOid(NPCOid);
        if (npc == null) {
            if (!NPCConversationManager.npcRequestController.containsKey(new Pair<>(NPCOid, c))) {
                if (c.getPlayer().isShowErr()) {
                    c.getPlayer().showInfo("NPC動作", true, "地圖上不存在NPC, OID=" + NPCOid);
                }
                return;
            }
        } else if (!c.getPlayer().isMapObjectVisible(npc)) {
            return;
        }

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.LP_NpcMove.getValue());

        mplew.writeInt(NPCOid);
        mplew.write(slea.readByte());
        mplew.write(slea.readByte());
        mplew.writeInt(slea.readInt());

        if (slea.available() > 0) {
            mplew.write(slea.read((int) slea.available()));
        }
        mplew.writeZeroBytes(100);

        c.getSession().writeAndFlush(mplew.getPacket());
    }

    public static final void NPCShop(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        ShopOpcode opShop = ShopOpcode.getOpcode(slea.readByte());
        if (chr == null) {
            return;
        }

        switch (opShop) {
            case ShopReq_Buy: {// 購買
                MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                short slot = slea.readShort();
                int itemId = slea.readInt();
                short quantity = slea.readShort();
                int unitprice = slea.readInt();
                shop.buy(c, slot, itemId, quantity, unitprice);
                break;
            }
            case ShopReq_Sell: {// 出售
                MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                byte slot = (byte) slea.readShort();
                int itemId = slea.readInt();
                short quantity = slea.readShort();
                shop.sell(c, GameConstants.getInventoryType(itemId), slot, quantity);
                break;
            }
            case ShopReq_Recharge: {// 充值
                MapleShop shop = chr.getShop();
                if (shop == null) {
                    return;
                }
                byte slot = (byte) slea.readShort();
                shop.recharge(c, slot);
                break;
            }
            case ShopReq_Close: { // 關閉
                chr.setConversation(0);
                break;
            }
            case ShopReq_StarCoinRes: {
                chr.setConversation(0);
                break;
            }
            default:
                chr.setConversation(0);
        }
        if (slea.available() > 0 && chr.isShowErr()) {
            chr.showInfo("商店操作", true, "有未讀取完的數據" + slea.toString());
        }
    }

    public static final void NPCTalk(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final MapleNPC npc = chr.getMap().getNPCByOid(slea.readInt());
        if (npc == null) {
            if (c.getPlayer().isShowErr()) {
                c.getPlayer().showInfo("NPC對話", true, "當前地圖不存在此NPC");
            }
            return;
        }
        if (chr.hasBlockedInventory()) {
            if (c.getPlayer().isShowErr()) {
                c.getPlayer().showInfo("NPC對話", true, "無法進行對話,hasBlockedInventory-" + chr.hasBlockedInventory());
            }
            NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
            if (cm == null
                    || (cm.getType() != ScriptType.ON_USER_ENTER && cm.getType() != ScriptType.ON_FIRST_USER_ENTER)) {
                c.getPlayer().dropMessage(-1, "你當前已經和1個NPC對話了. 如果不是請輸入 @解卡 指令進行解卡。");
            }
            return;
        }
        if (npc.hasShop()) {
            chr.setConversation(1);
            npc.sendShop(c);
        } else {
            NPCScriptManager.getInstance().start(c, npc.getId(), null);
        }
    }

    public static final void NpcQuestAction(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        int quest = slea.readInt(); // 38003
        int npc = slea.readInt(); // 3002007
        byte unk = slea.readByte(); // 1
        int unk2 = slea.readInt(); // 500008
        if (chr == null) {
            return;
        }
        final MapleQuest q = MapleQuest.getInstance(quest);
        if (npc == 0 && quest > 0) {
            q.forceStart(chr, npc, null);
        } else if (!q.hasStartScript()) {
            q.start(chr, npc);
        }
        // if (chr.hasBlockedInventory()) {
        // if (c.getPlayer().isShowErr()) {
        // c.getPlayer().showInfo("NPC對話", true, "無法進行對話,hasBlockedInventory-" +
        // chr.hasBlockedInventory());
        // }
        // NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
        // if (cm == null || (cm.getType() != ScriptType.ON_USER_ENTER && cm.getType()
        // != ScriptType.ON_FIRST_USER_ENTER)) {
        // c.getPlayer().dropMessage(-1, "你當前已經和1個NPC對話了. 如果不是請輸入 @解卡 指令進行解卡。");
        // }
        // return;
        // }
        // //c.getPlayer().updateTick(slea.readInt());
        // NPCScriptManager.getInstance().startQuest(c, npc, quest);
    }

    public static final void QuestAction(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte action = slea.readByte();
        int quest = (int) slea.readUInt();
        if (quest == 20734) {
            c.getSession().writeAndFlush(CWvsContext.ultimateExplorer());
            return;
        }
        if (chr == null) {
            return;
        }

        boolean 冰原雪域的長老任務;
        switch (quest) {
            case 1430:
            case 1434:
            case 1438:
            case 1441:
            case 1444:
                冰原雪域的長老任務 = true;
                break;
            default:
                冰原雪域的長老任務 = false;
        }
        if (冰原雪域的長老任務 && c.getPlayer().getQuestStatus(quest) != 1 && c.getPlayer().getMapId() != 211000001) {// 冒險家3轉傳送
            final server.maps.MapleMap mapz = handling.channel.ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(211000001);
            c.getPlayer().changeMap(mapz, mapz.getPortal(0));
        }

        final MapleQuest q = MapleQuest.getInstance(quest);
        switch (action) {
            case 0: { // Restore lost item
                // chr.updateTick(slea.readInt());
                slea.readInt();
                final int itemid = slea.readInt();
                q.RestoreLostItem(chr, itemid);
                break;
            }
            case 1: { // Start Quest
                final int npc = slea.readInt();
                if (q.hasStartScript()) {
                    break;
                }
                if (chr.isShowInfo()) {
                    chr.showInfo("系統任務開始", false, "NPC: " + npc + " 任務：" + q);
                }
                q.start(chr, npc);
                break;
            }
            case 2: { // Complete Quest
                final int npc = slea.readInt();
                // chr.updateTick(slea.readInt());
                slea.readInt();

                if (q.hasEndScript()) {
                    return;
                }
                if (chr.isShowInfo()) {
                    chr.showInfo("系統任務完成", false, "NPC: " + npc + " 任務：" + q);
                }
                if (slea.available() >= 4) {
                    q.complete(chr, npc, slea.readInt());
                } else {
                    q.complete(chr, npc);
                }
                // c.getSession().writeAndFlush(CField.completeQuest(c.getPlayer(), quest));
                // c.getSession().writeAndFlush(CField.updateQuestInfo(c.getPlayer(), quest,
                // npc, (byte)14));
                // 6 = start quest
                // 7 = unknown error
                // 8 = equip is full
                // 9 = not enough mesos
                // 11 = due to the equipment currently being worn wtf o.o
                // 12 = you may not posess more than one of this item
                break;
            }
            case 3: { // Forfeit Quest
                if (GameConstants.canForfeit(q.getId())) {
                    if (chr.isShowInfo()) {
                        chr.showInfo("系統任務放棄", false, "任務：" + q);
                    }
                    q.forfeit(chr);
                } else {
                    chr.dropMessage(1, "無法放棄這個任務.");
                }
                break;
            }
            case 4: { // Scripted Start Quest
                final int npc = slea.readInt();

                // 紀錄勳章的相關的任務自動完成
                if (q.getMedalItem() > 0 && ItemConstants.類型.勳章(q.getMedalItem())) {
                    if (chr.haveItem(q.getMedalItem()) && chr.getQuestStatus(quest) != 2) {
                        q.forceComplete(chr, npc);
                    }
                    break;
                }
                if (chr.isShowInfo()) {
                    chr.showInfo("任務開始腳本", false, "NPC：" + npc + " 任務：" + q);
                }
                if (chr.hasBlockedInventory()) {
                    if (c.getPlayer().isShowErr()) {
                        c.getPlayer().showInfo("NPC對話", true, "無法進行對話,hasBlockedInventory-" + chr.hasBlockedInventory());
                    }
                    NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
                    if (cm == null || (cm.getType() != ScriptType.ON_USER_ENTER && cm.getType() != ScriptType.ON_FIRST_USER_ENTER)) {
                        c.getPlayer().dropMessage(-1, "你當前已經和1個NPC對話了. 如果不是請輸入 @解卡 指令進行解卡。");
                    }
                    return;
                }
                // c.getPlayer().updateTick(slea.readInt());
                NPCScriptManager.getInstance().startQuest(c, npc, quest);
                break;
            }
            case 5: { // Scripted End Quest
                final int npc = slea.readInt();
                if (chr.hasBlockedInventory()) {
                    if (c.getPlayer().isShowErr()) {
                        c.getPlayer().showInfo("NPC對話", true, "無法進行對話,hasBlockedInventory-" + chr.hasBlockedInventory());
                    }
                    NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
                    if (cm == null || (cm.getType() != ScriptType.ON_USER_ENTER && cm.getType() != ScriptType.ON_FIRST_USER_ENTER)) {
                        c.getPlayer().dropMessage(-1, "你當前已經和1個NPC對話了. 如果不是請輸入 @解卡 指令進行解卡。");
                    }
                    return;
                }
                if (chr.isShowInfo()) {
                    chr.showInfo("完成任務腳本", false, "NPC：" + npc + " 任務：" + q);
                }
                // c.getPlayer().updateTick(slea.readInt());
                NPCScriptManager.getInstance().endQuest(c, npc, quest, false);
                // c.getSession().writeAndFlush(EffectPacket.showQuetCompleteEffect());
                // chr.getMap().broadcastMessage(chr,
                // EffectPacket.showQuetCompleteEffect(chr.getId()), false);
                break;
            }
            case 6: {
                c.getPlayer().dropMessage(6, "任務 : " + q.getName() + "(" + q.getId() + ") ACTION=6");
            }
        }
    }

    @SuppressWarnings("empty-statement")
    public static final void Storage(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte mode = slea.readByte();
        if (chr == null) {
            return;
        }
        MapleStorage storage = chr.getStorage();

        switch (mode) {
            case 3: { // 第二組密碼驗證(未完成)
                String secondpw = slea.readMapleAsciiString();
                if (c.CheckSecondPassword(secondpw)) {
                    c.getPlayer().setConversation(4);
                    c.getPlayer().getStorage().sendStorage(c, 0);
                }
            }
            case 4: { // 取出
                if (chr.getMeso() < 1000L) {
                    chr.dropMessage(1, "楓幣不足。");
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                byte type = slea.readByte();
                byte slot = storage.getSlot(MapleInventoryType.getByType(type), slea.readByte());
                Item item = storage.takeOut(slot);
                if (item != null) {
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int flag = item.getFlag();
                    if (ItemFlag.KARMA.check(flag)) {
                        flag -= ItemFlag.KARMA.getValue();
                    }
                    if (ItemFlag.KARMA_ACC.check(flag)) {
                        flag -= ItemFlag.KARMA_ACC.getValue();
                    }
                    if (ii.isAccountShared(item.getItemId()) && ii.isSharableOnce(item.getItemId())) {
                        flag |= ItemFlag.UNTRADABLE.getValue();
                    }
                    item.setFlag(flag);
                    if (!MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                        storage.store(item);
                        chr.dropMessage(1, "道具欄空間不足。");
                    } else {
                        chr.gainMeso(-1000L, false, false);
                        MapleInventoryManipulator.addFromDrop(c, item);
                        storage.sendTakenOut(c, GameConstants.getInventoryType(item.getItemId()));
                    }
                } else {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                }
                break;
            }
            case 5: { // 放入倉庫
                if (chr.getMeso() < 500L) {
                    chr.dropMessage(1, "楓幣不足。");
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                byte slot = (byte) slea.readShort();
                int itemId = slea.readInt();
                MapleInventoryType type = GameConstants.getInventoryType(itemId);
                short quantity = slea.readShort();
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (quantity < 1) {
                    return;
                }
                if (storage.isFull()) {
                    c.getSession().writeAndFlush(CField.NPCPacket.getStorageFull());
                    return;
                }
                if (chr.getInventory(type).getItem((short) slot) == null) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }

                Item item = chr.getInventory(type).getItem((short) slot).copy();

                if (ii.isCash(item.getItemId())) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                int flag = item.getFlag();
                if ((ii.isPickupRestricted(item.getItemId())) && (storage.findById(item.getItemId()) != null)) {
                    c.getSession().writeAndFlush(CWvsContext.enableActions());
                    return;
                }
                if ((item.getItemId() == itemId) && ((item.getQuantity() >= quantity) || ItemConstants.類型.可充值道具(itemId))) {
                    if (ii.isAccountShared(item.getItemId()) && ItemFlag.UNTRADABLE.check(flag)) {
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    }
                    if (ii.isShareTagEnabled(item.getItemId()) && !ItemFlag.KARMA_ACC.check(flag)
                            && !ItemFlag.KARMA.check(flag)) {
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    }
                    if ((ii.isDropRestricted(item.getItemId()) || ItemFlag.UNTRADABLE.check(flag))
                            && !ItemFlag.KARMA.check(flag)) {
                        c.getSession().writeAndFlush(CWvsContext.enableActions());
                        return;
                    }
                    if (ItemConstants.類型.可充值道具(itemId)) {
                        quantity = item.getQuantity();
                    }
                    chr.gainMeso(-500L, false, false);
                    MapleInventoryManipulator.removeFromSlot(c, type, (short) slot, quantity, false);
                    item.setQuantity(quantity);
                    storage.store(item);
                } else {
                    return;
                }
                storage.sendStored(c, GameConstants.getInventoryType(itemId));
                break;
            }
            case 6:
                storage.arrange();
                storage.update(c);
                break;
            case 7: { // 楓幣存取操作
                long meso = slea.readLong();
                long storageMesos = storage.getMeso();
                long playerMesos = chr.getMeso();

                if ((meso > 0L && storageMesos >= meso) || (meso < 0L && playerMesos >= -meso)) {
                    if (meso < 0L && storageMesos - meso > FeaturesConfig.STORAGE_MESO_MAX) { // 保管
                        meso = -(FeaturesConfig.STORAGE_MESO_MAX - storageMesos);
                        if (-meso > playerMesos) {
                            return;
                        }
                    } else if (meso > 0L && playerMesos + meso > FeaturesConfig.STORAGE_MESO_MAX) { // 取出
                        meso = FeaturesConfig.STORAGE_MESO_MAX - playerMesos;
                        if (meso > storageMesos) {
                            return;
                        }
                    }
                    storage.setMeso(storageMesos - meso);
                    chr.gainMeso(meso, false, false);
                } else {
                    return;
                }
                storage.sendMeso(c);
                break;
            }
            case 8:
                storage.close();
                chr.setConversation(0);
                break;
            default:
                System.out.println("未處理的倉庫操作碼 : " + mode);
        }
    }

    public static void NPCMoreTalk(final LittleEndianAccessor slea, final MapleClient c) {
        final ScriptMessageType lastMsg = ScriptMessageType.getNPCTalkType(slea.readByte()); // 00 (last msg type I
        // think)

        if (lastMsg == ScriptMessageType.SM_ASKAVATAREX && slea.available() >= 4) {
            slea.readShort();
        }
        byte action = -1;
        if (slea.available() > 0) {
            action = slea.readByte(); // 00 = end chat, 01 == follow
        }

        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
        if (cm == null || c.getPlayer().getConversation() == 0 || cm.getLastMsg() != lastMsg) {
            if (c.getPlayer().isShowErr()) {
                c.getPlayer().showInfo("NPC交談", true,
                        "cm(=null:" + (cm == null) + ") Conversation(" + c.getPlayer().getConversation()
                        + ") lastMsg(cm.lastMsg:" + (cm == null ? 0 : cm.getLastMsg()) + " lastMsg:" + lastMsg
                        + ")");
            }
            return;
        }
        cm.setLastMsg(null);
        int selection = -1;
        if (lastMsg == ScriptMessageType.SM_ASKTEXT || lastMsg == ScriptMessageType.SM_MONOLOGUE) {
        } else if (slea.available() >= 4) {
            selection = slea.readInt();
        } else if (slea.available() > 0) {
            selection = slea.readByte();
        }
        switch (lastMsg) {
            case SM_SAYIMAGE: {
                break;
            }
            case SM_ASKTEXT: {
                if (action != 0) {
                    cm.setGetText(slea.readMapleAsciiString());
                } else {
                    cm.dispose();
                    return;
                }
                break;
            }
            case SM_ASKNUMBER: {
                if (selection == -1) {
                    cm.dispose();
                    return;
                }
                break;
            }
            case SM_PLAYMOVIECLIP: {
                if (action == 0) {
                    c.getSession().writeAndFlush(CWvsContext.getTopMsg("影片播放失敗。"));
                }
                break;
            }
            case SM_PLAYMOVIECLIP_URL: {
                if (action == -1) {
                    action = 1;
                }
                break;
            }
            case SM_MONOLOGUE: {
                if (slea.available() > 0) {
                    if (c.getPlayer().isShowErr()) {
                        c.getPlayer().showInfo("Direction", true, "包未讀完 " + slea.toString());
                    }
                }
                if (action != -1) {
                    if (c.getPlayer().isShowErr()) {
                        c.getPlayer().showInfo("Direction", true, "action - " + action);
                    }
                }
                action = 1;
                break;
            }
            default: {
                if (selection < -1 || action == -1) {
                    cm.dispose();
                    return;
                }
            }
        }

        if (null == cm.getType()) {
            NPCScriptManager.getInstance().action(c, action, lastMsg.getType(), selection);
        } else {
            switch (cm.getType()) {
                case QUEST_START:
                    NPCScriptManager.getInstance().startQuest(c, action, lastMsg.getType(), selection);
                    break;
                case QUEST_END:
                    NPCScriptManager.getInstance().endQuest(c, action, lastMsg.getType(), selection);
                    break;
                default:
                    NPCScriptManager.getInstance().action(c, action, lastMsg.getType(), selection);
                    break;
            }
        }
    }

    public static void DirectionComplete(final LittleEndianAccessor slea, final MapleClient c) {
        final NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);

        if (slea.available() > 0) {
            if (c.getPlayer().isShowErr()) {
                c.getPlayer().showInfo("DirectionComplete", true, "包未讀完 " + slea.toString());
            }
        }
        if (cm == null || c.getPlayer().getConversation() == 0) {
            if (c.getPlayer().isShowErr()) {
                c.getPlayer().showInfo("DirectionComplete", true,
                        "cm(=null:" + (cm == null) + ") Conversation(" + c.getPlayer().getConversation() + ")");
            }
            return;
        }
        cm.setLastMsg(null);
        if (null == cm.getType()) {
            NPCScriptManager.getInstance().action(c, (byte) 1, (byte) 0, 0);
        } else {
            switch (cm.getType()) {
                case QUEST_START:
                    NPCScriptManager.getInstance().startQuest(c, (byte) 1, (byte) 0, 0);
                    break;
                case QUEST_END:
                    NPCScriptManager.getInstance().endQuest(c, (byte) 1, (byte) 0, 0);
                    break;
                default:
                    NPCScriptManager.getInstance().action(c, (byte) 1, (byte) 0, 0);
                    break;
            }
        }
    }

    public static final void repairAll(final MapleClient c) {
        Equip eq;
        double rPercentage;
        int price = 0;
        Map<String, Integer> eqStats;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<Equip, Integer> eqs = new HashMap<>();
        final MapleInventoryType[] types = {MapleInventoryType.EQUIP, MapleInventoryType.EQUIPPED};
        for (MapleInventoryType type : types) {
            for (Item item : c.getPlayer().getInventory(type).newList()) {
                if (item instanceof Equip) { // redundant
                    eq = (Equip) item;
                    if (eq.getDurability() >= 0) {
                        eqStats = ii.getEquipStats(eq.getItemId());
                        if (eqStats.containsKey("durability") && eqStats.get("durability") > 0
                                && eq.getDurability() < eqStats.get("durability")) {
                            rPercentage = (100.0
                                    - Math.ceil((eq.getDurability() * 1000.0) / (eqStats.get("durability") * 10.0)));
                            eqs.put(eq, eqStats.get("durability"));
                            price += (int) Math.ceil(rPercentage * ii.getPrice(eq.getItemId())
                                    / (ii.getReqLevel(eq.getItemId()) < 70 ? 100.0 : 1.0));
                        }
                    }
                }
            }
        }
        if (eqs.size() <= 0 || c.getPlayer().getMeso() < price) {
            return;
        }
        c.getPlayer().gainMeso(-price, true);
        Equip ez;
        for (Entry<Equip, Integer> eqqz : eqs.entrySet()) {
            ez = eqqz.getKey();
            ez.setDurability(eqqz.getValue());
            c.getPlayer().forceReAddItem(ez.copy());
        }
    }

    public static final void repair(final LittleEndianAccessor slea, final MapleClient c) {
        if (slea.available() < 4) { // leafre for now
            return;
        }
        final int position = slea.readInt(); // who knows why this is a int
        final MapleInventoryType type = position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
        final Item item = c.getPlayer().getInventory(type).getItem((byte) position);
        if (item == null) {
            return;
        }
        final Equip eq = (Equip) item;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final Map<String, Integer> eqStats = ii.getEquipStats(item.getItemId());
        if (eq.getDurability() < 0 || !eqStats.containsKey("durability") || eqStats.get("durability") <= 0
                || eq.getDurability() >= eqStats.get("durability")) {
            return;
        }
        final double rPercentage = (100.0
                - Math.ceil((eq.getDurability() * 1000.0) / (eqStats.get("durability") * 10.0)));
        // drpq level 105 weapons - ~420k per %; 2k per durability point
        // explorer level 30 weapons - ~10 mesos per %
        final int price = (int) Math
                .ceil(rPercentage * ii.getPrice(eq.getItemId()) / (ii.getReqLevel(eq.getItemId()) < 70 ? 100.0 : 1.0)); // /
        // 100
        // for
        // level
        // 30?
        if (c.getPlayer().getMeso() < price) {
            return;
        }
        c.getPlayer().gainMeso(-price, false);
        eq.setDurability(eqStats.get("durability"));
        c.getPlayer().forceReAddItem(eq.copy());
    }

    public static final void UpdateQuest(final LittleEndianAccessor slea, final MapleClient c) {
        final MapleQuest quest = MapleQuest.getInstance((int) slea.readUInt());
        if (quest != null) {
            c.getPlayer().updateQuest(c.getPlayer().getQuest(quest), true);
        }
    }

    public static final void UseItemQuest(final LittleEndianAccessor slea, final MapleClient c) {
        final short slot = slea.readShort();
        final int itemId = slea.readInt();
        final Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
        final int qid = slea.readInt();
        final MapleQuest quest = MapleQuest.getInstance(qid);
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Pair<Integer, List<Integer>> questItemInfo = null;
        boolean found = false;
        for (Item i : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
            if (i.getItemId() / 10000 == 422) {
                questItemInfo = ii.questItemInfo(i.getItemId());
                if (questItemInfo != null && questItemInfo.getLeft() == qid && questItemInfo.getRight() != null && questItemInfo.getRight().contains(itemId)) {
                    found = true;
                    break; // i believe it's any order
                }
            }
        }
        if (quest != null && found && item != null && item.getQuantity() > 0 && item.getItemId() == itemId) {
            final int newData = slea.readInt();
            final MapleQuestStatus stats = c.getPlayer().getQuestNoAdd(quest);
            if (stats != null && stats.getStatus() == 1) {
                stats.setCustomData(String.valueOf(newData));
                c.getPlayer().updateQuest(stats, true);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, (short) 1, false);
            }
        }
    }

    public static final void RPSGame(LittleEndianAccessor slea, MapleClient c) {
        if ((slea.available() == 0L) || (c.getPlayer() == null) || (c.getPlayer().getMap() == null)
                || (!c.getPlayer().getMap().containsNPC(9000019))) {
            if ((c.getPlayer() != null) && (c.getPlayer().getRPS() != null)) {
                c.getPlayer().getRPS().dispose(c);
            }
            return;
        }
        byte mode = slea.readByte();
        switch (mode) {
            case 0:
            case 5:
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().reward(c);
                }
                if (c.getPlayer().getMeso() >= 1000L) {
                    c.getPlayer().setRPS(new RockPaperScissors(c, mode));
                } else {
                    c.getSession().writeAndFlush(CField.getRPSMode((byte) 8, -1, -1, -1));
                }
                break;
            case 1:
                if ((c.getPlayer().getRPS() == null) || (!c.getPlayer().getRPS().answer(c, slea.readByte()))) {
                    c.getSession().writeAndFlush(CField.getRPSMode((byte) 13, -1, -1, -1));
                }
                break;
            case 2:
                if ((c.getPlayer().getRPS() == null) || (!c.getPlayer().getRPS().timeOut(c))) {
                    c.getSession().writeAndFlush(CField.getRPSMode((byte) 13, -1, -1, -1));
                }
                break;
            case 3:
                if ((c.getPlayer().getRPS() == null) || (!c.getPlayer().getRPS().nextRound(c))) {
                    c.getSession().writeAndFlush(CField.getRPSMode((byte) 13, -1, -1, -1));
                }
                break;
            case 4:
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().dispose(c);
                } else {
                    c.getSession().writeAndFlush(CField.getRPSMode((byte) 13, -1, -1, -1));
                }
                break;
        }
    }

    public static void OpenQuickMoveNpc(final LittleEndianAccessor slea, final MapleClient c) {
        final int npcid = slea.readInt();
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().isInBlockedMap()) {
            NPCConversationManager cm = NPCScriptManager.getInstance().getCM(c);
            if (cm == null
                    || (cm.getType() != ScriptType.ON_USER_ENTER && cm.getType() != ScriptType.ON_FIRST_USER_ENTER)) {
                c.getPlayer().dropMessage(-1, "你當前已經和1個NPC對話了. 如果不是請輸入 @解卡 指令進行解卡。");
            }
            return;
        }
        int npcId = -1;
        QuickMove qm = QuickMove.getByMap(c.getPlayer().getMapId());
        if (qm != null) {
            long npcs = qm.getNPCFlag();
            for (QuickMove.QuickMoveNPC npc : QuickMove.QuickMoveNPC.values()) {
                if (npc.check(npcs) && npc.getId() == npcid && npc.getLevel() <= c.getPlayer().getLevel()) {
                    npcId = npcid;
                    break;
                }
            }
        }

        if (npcId == -1 && QuickMove.GLOBAL_NPC != 0 && !GameConstants.isBossMap(c.getPlayer().getMapId())
                && !GameConstants.isTutorialMap(c.getPlayer().getMapId())
                && (c.getPlayer().getMapId() / 100 != 9100000 || c.getPlayer().getMapId() == 910000000)) {
            for (QuickMove.QuickMoveNPC npc : QuickMove.QuickMoveNPC.values()) {
                if (npc.check(QuickMove.GLOBAL_NPC) && npc.getId() == npcid
                        && npc.getLevel() <= c.getPlayer().getLevel()) {
                    npcId = npcid;
                    break;
                }
            }
        }

        if (npcId == -1) {
            System.err.println("未找到QuickMove的NPC:" + npcId);
            return;
        }
        final MapleNPC npc = MapleLifeFactory.getNPC(npcId);
        if (npc == null) {
            System.err.println("未找到QuickMove的NPC:" + npcId);
            return;
        }
        if (npc.hasShop()) {
            c.getPlayer().setConversation(1);
            npc.sendShop(c);
        } else {
            NPCScriptManager.getInstance().start(c, npcId, null);
        }
    }

    public static void OpenQuickMoveSpecial(final LittleEndianAccessor slea, MapleClient c) {
        final int selection = slea.readInt();
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().isInBlockedMap()) {
            return;
        }
        QuickMove.QuickMoveNPC quickMove = null;
        QuickMove qm = QuickMove.getByMap(c.getPlayer().getMapId());
        int i = 0;
        if (qm != null) {
            long npcs = qm.getNPCFlag();
            for (QuickMove.QuickMoveNPC npc : QuickMove.QuickMoveNPC.values()) {
                if (!npc.show(c.getPlayer().getMapId()) || !npc.check(npcs)) {
                    continue;
                }
                if (selection == i++ && npc.getId() == 0 && npc.getLevel() <= c.getPlayer().getLevel()) {
                    quickMove = npc;
                    break;
                }
            }
        }

        if (quickMove == null && QuickMove.GLOBAL_NPC != 0 && !GameConstants.isBossMap(c.getPlayer().getMapId())
                && !GameConstants.isTutorialMap(c.getPlayer().getMapId())
                && (c.getPlayer().getMapId() / 100 != 9100000 || c.getPlayer().getMapId() == 910000000)) {
            for (QuickMove.QuickMoveNPC npc : QuickMove.QuickMoveNPC.values()) {
                if (!npc.show(c.getPlayer().getMapId()) || !npc.check(QuickMove.GLOBAL_NPC)) {
                    continue;
                }
                if (selection == i++ && npc.getId() == 0 && npc.getLevel() <= c.getPlayer().getLevel()) {
                    quickMove = npc;
                    break;
                }
            }
        }

        if (quickMove == null) {
            System.err.println("未找到QuickMove動作, 選項:" + selection);
            return;
        }
        int npcId = 0;
        String special = null;
        switch (quickMove) {
            case 聚合功能:
                npcId = 9000226;
                special = "聚合功能";
                break;
            default:
                System.err.println("未處理QuickMove動作, 類型:" + quickMove.name());
        }
        if (npcId > 0) {
            final MapleNPC npc = MapleLifeFactory.getNPC(npcId);
            if (npc.hasShop()) {
                c.getPlayer().setConversation(1);
                npc.sendShop(c);
            } else {
                NPCScriptManager.getInstance().start(c, npcId, special);
            }
        }
    }

    public static void OpenZeroQuickMoveSpecial(final LittleEndianAccessor slea, MapleClient c) {
        final int type = slea.readShort();
        final int selection = slea.readShort();
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().isInBlockedMap() || c.getPlayer().getLevel() < 10) {
            return;
        }
        if (type == 2) {
            switch (selection) {
                case 17491:
                    c.getPlayer().changeMap(321190100, 0);
                    break;
                case 17996:
                    c.getPlayer().changeMap(321100000, 0);
                    break;
            }
        }
    }
}
