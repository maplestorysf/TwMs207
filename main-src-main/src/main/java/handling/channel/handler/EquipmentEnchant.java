package handling.channel.handler;

import client.MapleClient;
import client.inventory.EchantEquipStat;
import client.inventory.EchantScroll;
import client.inventory.Equip;
import client.inventory.EquipStat;
import client.inventory.EquipStatsType;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ItemConstants;
import constants.ServerConstants;
import constants.FeaturesConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataTool;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;

public class EquipmentEnchant {

    // 星力強化
    private static final int[] sfSuccessProp = {950, 900, 850, 850, 800, 750, 700, 650, 600, 550, 450, 350, 300, 300,
        300, 300, 300, 300, 300, 300, 300, 300, 30, 20, 10};
    private static final int[] sfDestroyProp = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 200, 350, 400, 450, 600, 650, 750,
        800, 850, 900, 950, 1000, 1000, 1000};
    private static final int[] sfSuccessPropSup = {500, 500, 450, 400, 400, 400, 400, 400, 400, 370, 350, 350, 30, 20,
        10};
    private static final int[] sfDestroyPropSup = {0, 0, 0, 0, 0, 60, 100, 140, 200, 300, 400, 500, 1000, 1000, 1000};

    private static enum StarForceResult {

        降級(0), 成功(1), 損壞(2), 失敗(3), 默認(-1);

        private byte value;

        StarForceResult(int value) {
            this.value = (byte) value;
        }

        public byte getValue() {
            return value;
        }
    }

    public static void handlePacket(final LittleEndianAccessor slea, final MapleClient c) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte code = slea.readByte();
        switch (code) {
            case 0: { // 咒文的痕跡 強化
                slea.skip(4);
                short position = slea.readShort();
                short sposition = slea.readShort();
                short success = slea.readShort();
                success = 0;
                Equip item = (Equip) c.getPlayer()
                        .getInventory(position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP)
                        .getItem(position);
                if (item == null) {
                    c.getSession().writeAndFlush(CWvsContext.Enchant.getEnchantResult(item, item, 0, sposition));
                    return;
                }
                if (EquipStat.EnhanctBuff.EQUIP_MARK.check(item.getEnhanctBuff())) {
                    return;
                }
                ArrayList<EchantScroll> scrolls = getEchantScrolls(item);
                if (scrolls.isEmpty() || scrolls.size() < sposition - 1
                        || (scrolls.get(sposition).getViewType() < 4 && item.getUpgradeSlots() < 1)
                        || (scrolls.get(sposition).getViewType() == 4
                        && item.getUpgradeSlots() == ii.getSlots(item.getItemId()) + item.getHammer())
                        || (scrolls.get(sposition).getViewType() == 5 && item.getLevel()
                        + item.getUpgradeSlots() == ii.getSlots(item.getItemId()) + item.getHammer())
                        || !c.getPlayer().haveItem(4001832, scrolls.get(sposition).getCost())) {
                    c.getSession().writeAndFlush(CWvsContext.Enchant.getEnchantResult(item, item, 0, sposition));
                    if (c.getPlayer().isShowErr()) {
                        c.getPlayer().showInfo("咒文強化", true, "咒文強化檢測 找不到捲軸：" + (scrolls.isEmpty()) + " 收到的捲軸不在範圍："
                                + (scrolls.size() < sposition - 1) + " 捲軸為普通捲軸且沒有剩餘升級次數："
                                + (scrolls.get(sposition).getViewType() < 4 && item.getUpgradeSlots() < 1)
                                + " 捲軸為回真捲軸沒有上過卷："
                                + (scrolls.get(sposition).getViewType() == 4
                                && item.getUpgradeSlots() == ii.getSlots(item.getItemId()) + item.getHammer())
                                + " 捲軸為純白捲軸且沒有可拉次數："
                                + (scrolls.get(sposition).getViewType() == 5 && item.getLevel()
                                + item.getUpgradeSlots() != ii.getSlots(item.getItemId()) + item.getHammer())
                                + " 咒文不足：" + (!c.getPlayer().haveItem(4001832, scrolls.get(sposition).getCost())));
                    }
                    return;
                }
                EchantScroll scroll = scrolls.get(sposition);
                if (Randomizer.isSuccess(scroll.getSuccessRate()) || c.getPlayer().isGM()) {
                    success = 1;
                }
                if (c.getPlayer().isShowInfo()) {
                    if (c.getPlayer().isAdmin()) {
                        if (c.getPlayer().isInvincible()) {
                            c.getPlayer().dropMessage(-6, "伺服器管理員無敵狀態潛能等級提升成功率100%");
                        }
                    }
                    c.getPlayer().showMessage(8, "咒文強化 - 強化道具：" + item + " 選中捲軸:" + scroll.getName() + " 消耗咒文："
                            + scroll.getCost() + " 成功率：" + scroll.getSuccessRate() + "%  強化結果：" + (success == 1));
                }
                int mask = scroll.getMask();
                Equip oldEq = (Equip) item.copy();
                if (success == 1) {
                    if (scroll.getViewType() < 4) {
                        for (EchantEquipStat eStat : EchantEquipStat.values()) {
                            if (eStat.check(mask)) {
                                mask -= eStat.getValue();
                                switch (eStat) {
                                    case WATK:
                                        item.setWatk((short) (item.getWatk(0) + scroll.getAtk()));
                                        break;
                                    case MATK:
                                        item.setMatk((short) (item.getMatk(0) + scroll.getAtk()));
                                        break;
                                    case STR:
                                        item.setStr((short) (item.getStr(0) + scroll.getStat()));
                                        break;
                                    case DEX:
                                        item.setDex((short) (item.getDex(0) + scroll.getStat()));
                                        break;
                                    case INT:
                                        item.setInt((short) (item.getInt(0) + scroll.getStat()));
                                        break;
                                    case LUK:
                                        item.setLuk((short) (item.getLuk(0) + scroll.getStat()));
                                        break;
                                }
                                if (mask == 0) {
                                    break;
                                }
                            }
                        }
                        item.setLevel((byte) (item.getLevel() + 1));
                    } else if (scroll.getViewType() == 4) {
                        Equip newEquip = (Equip) ii.getEquipById(item.getItemId());
                        item = (Equip) item.reset(newEquip, false);
                    } else if (scroll.getViewType() == 5) {
                        item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                    }
                }

                MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(4001832), 4001832,
                        (short) scroll.getCost(), true, false);
                if (scroll.getViewType() < 4) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() - 1));
                }
                c.getSession().writeAndFlush(CWvsContext.Enchant.getEnchantResult(oldEq, item, success, sposition));
                c.getPlayer().forceReAddItem(item);
                break;
            }
            case 1: { // 星力強化
                slea.skip(4);
                short position = slea.readShort();
                byte mode = slea.readByte();
                boolean maplePoint = mode != 0;
                boolean safe = mode == 2;
                boolean mgsuccess = slea.readByte() == 1;
                if (mgsuccess) {
                    slea.skip(4);
                }
                slea.readInt(); // [01 00 00 00]
                slea.readInt(); // [FF FF FF FF]
                boolean mesoSafe = slea.readByte() == 1;
                slea.readByte(); // [01]
                final Equip item = (Equip) c.getPlayer()
                        .getInventory(position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP)
                        .getItem(position);
                if (item == null) {
                    c.getSession().writeAndFlush(
                            CWvsContext.Enchant.getStarForceResult(item, item, StarForceResult.失敗.getValue()));
                    return;
                }
                if (EquipStat.EnhanctBuff.EQUIP_MARK.check(item.getEnhanctBuff())) {
                    return;
                }
                int enhance = item.getEnhance();
                if (enhance < 12 || enhance > 16) {
                    mesoSafe = false;
                }
                int reqLevel = ii.getReqLevel(item.getItemId());
                boolean isSuperior = ii.isSuperiorEquip(item.getItemId());
                if (!maplePoint) {
                    if (mesoSafe) {
                        safe = true;
                    } else {
                        safe = false;
                    }
                }
                long meso = maplePoint ? safe ? 50 : 9
                        : getStarForceMeso(reqLevel, enhance, isSuperior) * (mesoSafe ? 2 : 1);
                if (item.getUpgradeSlots() > 0 || enhance >= ItemConstants.卷軸.getEnhanceTimes(item.getItemId())
                        || ii.isCash(item.getItemId()) || maplePoint ? c.getPlayer().getCSPoints(2) < meso
                        : c.getPlayer().getMeso() < meso) {
                    c.getSession().writeAndFlush(
                            CWvsContext.Enchant.getStarForceResult(item, item, StarForceResult.失敗.getValue()));
                    if (c.getPlayer().isShowErr()) {
                        c.getPlayer().showInfo("星力強化", true, "星力強化檢測 裝備是否有剩餘升級次數：" + (item.getUpgradeSlots() >= 1)
                                + " 裝備星級是否大於可升級的星數：" + (enhance >= ItemConstants.卷軸.getEnhanceTimes(item.getItemId()))
                                + " 裝備是否是為點裝：" + ii.isCash(item.getItemId()) + (maplePoint ? " 楓點" : " 楓幣") + "不足："
                                + (maplePoint ? c.getPlayer().getCSPoints(2) < meso : c.getPlayer().getMeso() < meso));
                    }
                    return;
                }
                final Equip source = (Equip) item.copy();
                int successprop = isSuperior ? sfSuccessPropSup[enhance] : sfSuccessProp[enhance];
                int destroyprop = isSuperior ? sfDestroyPropSup[enhance] : sfDestroyProp[enhance];
                boolean fall = false;
                int fallprop = 1000 - successprop - destroyprop;
                if ((isSuperior && enhance > 0) || (enhance >= 5 && enhance % 5 != 0)) {
                    fall = true;
                }
                StarForceResult result;
                if (Randomizer.nextInt(1000) < (successprop - (mgsuccess ? 0 : 100)) || item.getFailCount() >= 2) { // 成功
                    result = StarForceResult.成功;
                } else if (Randomizer.nextInt(1000) < destroyprop && !safe) { // 損壞概率
                    result = StarForceResult.損壞;
                } else if (fall) {
                    result = StarForceResult.降級;
                } else {
                    result = StarForceResult.失敗;
                }
                if (c.getPlayer().isShowInfo()) {
                    c.getPlayer().showMessage(8,
                            "星力強化 - 強化道具：" + item + " 當前星級：" + enhance + "星 消耗幣種：" + (maplePoint ? "楓點" : "楓幣") + " 消耗量："
                            + meso + " 成功機率：" + ((successprop - (mgsuccess ? 0.0 : 100.0)) / 10.0) + "% 小遊戲成功機率加成："
                            + (mgsuccess ? 10.0 : 0) + "% 損壞機率：" + (destroyprop / 10.0) + "% 失敗("
                            + (fall ? "下滑" : "維持") + ")機率：" + (fallprop / 10.0) + "% 強化結果：" + result.name());
                }
                if (c.getPlayer().isAdmin() && c.getPlayer().isInvincible()) {
                    result = StarForceResult.成功;
                    c.getPlayer().dropMessage(-6, "伺服器管理員無敵狀態升星成功率提升到100%");
                }
                // 道具處理
                switch (result) {
                    case 降級:
                        StarForceEnhanceItem(item, true);
                        item.setFailCount(item.getFailCount() + 1);
                        break;
                    case 成功:
                        StarForceEnhanceItem(item, false);
                        item.setFailCount(0);
                        break;
                    case 損壞:
                        for (int i = 0; i < enhance; i++) {
                            int newEnhance = item.getEnhance();
                            if (newEnhance == 0) {
                                break;
                            }
                            if (FeaturesConfig.saveSFLevel && !(isSuperior && newEnhance > 0)
                                    && !(newEnhance >= 5 && newEnhance % 5 != 0)) {
                                break;
                            }
                            StarForceEnhanceItem(item, true);
                        }
                        if (c.getPlayer().isShowInfo()) {
                            c.getPlayer().showMessage(8, "星力強化損毀處理 - 原星級：" + enhance + "損毀后星級：" + item.getEnhance());
                        }
                        item.setEnhanctBuff((short) (item.getEnhanctBuff() | EquipStat.EnhanctBuff.EQUIP_MARK.getValue()));
                        item.setFailCount(0);
                        break;
                    case 失敗:
                        item.setFailCount(0);
                        break;
                }
                if (maplePoint) {
                    c.getPlayer().modifyCSPoints(2, (int) -meso);
                } else {
                    c.getPlayer().gainMeso(-meso, false, false);
                }
                c.getSession().writeAndFlush(CWvsContext.Enchant.getStarForceResult(source, item, result.getValue()));
                c.getPlayer().forceReAddItem(item);
                break;
            }
            case 2: { // 星力強化 繼承
                slea.skip(4);
                short newPos = slea.readShort();
                short oldPos = slea.readShort();
                MapleInventory iv = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
                Equip nEquip = (Equip) iv.getItem(newPos);
                Equip equip = (Equip) iv.getItem(oldPos);
                if (equip == null || nEquip == null || equip.getItemId() != nEquip.getItemId()
                        || !EquipStat.EnhanctBuff.EQUIP_MARK.check(equip.getEnhanctBuff())
                        || EquipStat.EnhanctBuff.EQUIP_MARK.check(nEquip.getEnhanctBuff())) {
                    return;
                }
                c.getSession().writeAndFlush(CWvsContext.Enchant.getInheritance(equip.copy(), nEquip.inheritance(equip)));
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, oldPos, (short) 1, false);
                c.getPlayer().forceReAddItem(nEquip);
                break;
            }
            case 0x32: { // 咒文的痕跡 放置道具
                short position = slea.readShort();
                slea.skip(2);
                Equip item = (Equip) c.getPlayer()
                        .getInventory(position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP)
                        .getItem(position);
                if (item == null || EquipStat.EnhanctBuff.EQUIP_MARK.check(item.getEnhanctBuff())) {
                    return;
                }
                c.getSession().writeAndFlush(
                        CWvsContext.Enchant.getEnchantList(c, item, getEchantScrolls(item), ServerConstants.FEVER_TIME));
                break;
            }
            case 0x34: { // 星力強化 放置道具
                short position = slea.readShort();
                slea.readShort();
                boolean safe = slea.readByte() == 1;
                Equip item = (Equip) c.getPlayer()
                        .getInventory(position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP)
                        .getItem(position);
                if (item == null || EquipStat.EnhanctBuff.EQUIP_MARK.check(item.getEnhanctBuff())) {
                    return;
                }
                safe = safe ? item.getEnhance() >= 12 && item.getEnhance() <= 16 : safe;
                int reqLevel = ii.getReqLevel(item.getItemId());
                int enhance = item.getEnhance();
                boolean isSuperior = ii.isSuperiorEquip(item.getItemId());
                long meso = getStarForceMeso(reqLevel, enhance, isSuperior);
                int successprop = isSuperior ? sfSuccessPropSup[enhance] : sfSuccessProp[enhance];
                int destroyType = isSuperior ? sfDestroyPropSup[enhance] : sfDestroyProp[enhance];
                destroyType = (destroyType < 200 && destroyType > 0) ? 1 : (destroyType / 200);
                boolean fall = false;
                if ((isSuperior && enhance > 0) || (enhance >= 5 && enhance % 5 != 0)) {
                    fall = true;
                }
                Map<EchantEquipStat, Integer> stats = getStarForceStats(item.getEnhance(), item.getItemId(),
                        item.getWatk(0xFFFF - EquipStatsType.STAR_FORCE.getValue()),
                        item.getMatk(0xFFFF - EquipStatsType.STAR_FORCE.getValue()),
                        item.getWdef(0xFFFF - EquipStatsType.STAR_FORCE.getValue()), item.getReqLevel());
                c.getSession().writeAndFlush(CWvsContext.Enchant.getStarForcePreview(stats, meso * (safe ? 2 : 1),
                        successprop, destroyType, fall, item.getFailCount() >= 2));
                break;
            }
            case 0x35: { // 星力強化成功率加成遊戲
                c.getSession().writeAndFlush(CWvsContext.Enchant.getStarForceMiniGame());
                break;
            }
            default: {
                System.out.println("裝備強化系統,未知操作碼:" + code);
            }
        }
    }

    public static ArrayList<EchantScroll> getEchantScrolls(Equip eq) {
        ArrayList<EchantScroll> scrolls = new ArrayList<>();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ii.getSlots(eq.getItemId()) + eq.getHammer() <= 0) {
            return scrolls;
        }
        if (eq.getUpgradeSlots() > 0) {
            if (ItemConstants.類型.武器(eq.getItemId()) || ItemConstants.類型.心臟(eq.getItemId())
                    || ItemConstants.類型.雙刀(eq.getItemId())) {
                scrolls.add(EchantScroll.武器_100);
                scrolls.add(EchantScroll.武器_70);
                scrolls.add(EchantScroll.武器_30);
                scrolls.add(EchantScroll.武器_15);
            } else {
                scrolls.add(EchantScroll.攻擊力_100);
                scrolls.add(EchantScroll.攻擊力_70);
                scrolls.add(EchantScroll.攻擊力_30);
                scrolls.add(EchantScroll.攻擊力_15);
            }
        }
        if (eq.getUpgradeSlots() != ii.getSlots(eq.getItemId()) + eq.getHammer()) {
            scrolls.add(EchantScroll.回真卷軸);
        }
        if (eq.getLevel() + eq.getUpgradeSlots() != ii.getSlots(eq.getItemId()) + eq.getHammer()) {
            scrolls.add(EchantScroll.純白的咒文書);
        }
        return scrolls;
    }

    public static long getStarForceMeso(int equiplevel, int enhanced, boolean superior) {
        if (superior) {
            return getSuperiorStarForceMeso(equiplevel);
        }
        final long[] sfMeso100lv = {41000, 81000, 121000, 161000, 201000, 241000, 281000, 321000};
        final long[] sfMeso110lv = {54200, 107500, 160700, 214000, 267200, 320400, 373700, 426900, 480200, 533400};
        final long[] sfMeso120lv = {70100, 139200, 208400, 277500, 346600, 415700, 484800, 554000, 623100, 692200,
            5602100, 7085400, 8794500, 10742400, 12941800};
        final long[] sfMeso130lv = {88900, 176800, 264600, 352500, 440400, 528300, 616200, 704000, 791900, 879800,
            7122300, 9008200, 11181100, 13657700, 16454100, 19586000, 23069100, 26918600, 31149300, 35776100};
        final long[] sfMeso140lv = {110800, 220500, 330300, 440000, 549800, 659600, 769300, 879100, 988800, 1098600,
            8895400, 11250800, 13964700, 17057900, 20550500, 24462200, 28812500, 33620400, 38904500, 44683300,
            50974700, 57796700, 65166700, 73102200, 81620200};
        final long[] sfMeso150lv = {136000, 271000, 406000, 541000, 676000, 811000, 946000, 1081000, 1216000, 1351000,
            10940700, 13837700, 17175800, 20980200, 25275900, 30087200, 35437900, 41351400, 47850600, 54985200,
            62696400, 71087200, 80152000, 89912300, 100389000};
        if (equiplevel >= 0 && equiplevel <= 109) {
            return sfMeso100lv[enhanced];
        } else if (equiplevel >= 110 && equiplevel <= 119) {
            return sfMeso110lv[enhanced];
        } else if (equiplevel >= 120 && equiplevel <= 129) {
            return sfMeso120lv[enhanced];
        } else if (equiplevel >= 130 && equiplevel <= 139) {
            return sfMeso130lv[enhanced];
        } else if (equiplevel >= 140 && equiplevel <= 149) {
            return sfMeso140lv[enhanced];
        } else {
            return sfMeso150lv[enhanced];
        }
    }

    public static long getSuperiorStarForceMeso(int equiplevel) {
        final long[] sfMeso = {55832200, 55832200, 55832200, 55832200, 55832200, 55832200};
        if (equiplevel >= 0 && equiplevel <= 109) {
            equiplevel = 0;
        } else if (equiplevel >= 110 && equiplevel <= 119) {
            equiplevel = 1;
        } else if (equiplevel >= 120 && equiplevel <= 129) {
            equiplevel = 2;
        } else if (equiplevel >= 130 && equiplevel <= 139) {
            equiplevel = 3;
        } else if (equiplevel >= 140 && equiplevel <= 149) {
            equiplevel = 4;
        } else {
            equiplevel = 5;
        }
        return sfMeso[equiplevel];
    }

    public static Map<EchantEquipStat, Integer> getStarForceStats(int enhance, int itemId, int watk, int matk, int wdef,
            int reqLevel) {
        final Map<EchantEquipStat, Integer> stats = new HashMap<>();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int statusFlag = 0;
        int reqJob = ii.getReqJob(itemId);
        if ((reqJob & 0x1) != 0) { // 劍士
            statusFlag |= EchantEquipStat.STR.getValue();
            statusFlag |= EchantEquipStat.DEX.getValue();
        }
        if ((reqJob & 0x2) != 0) { // 法師
            statusFlag |= EchantEquipStat.INT.getValue();
            statusFlag |= EchantEquipStat.LUK.getValue();
        }
        if ((reqJob & 0x4) != 0) { // 弓箭手
            statusFlag |= EchantEquipStat.DEX.getValue();
            statusFlag |= EchantEquipStat.STR.getValue();
        }
        if ((reqJob & 0x8) != 0) { // 弓箭手
            statusFlag |= EchantEquipStat.LUK.getValue();
            statusFlag |= EchantEquipStat.DEX.getValue();
        }
        if ((reqJob & 0x10) != 0) { // 盜賊
            statusFlag |= EchantEquipStat.STR.getValue();
            statusFlag |= EchantEquipStat.DEX.getValue();
        }
        if (reqJob <= 0) { // 全職&&初心者
            statusFlag |= EchantEquipStat.STR.getValue();
            statusFlag |= EchantEquipStat.DEX.getValue();
            statusFlag |= EchantEquipStat.INT.getValue();
            statusFlag |= EchantEquipStat.LUK.getValue();
        }
        if (ii.isSuperiorEquip(itemId)) {
            switch (enhance) {
                case 0:
                    if (EchantEquipStat.STR.check(statusFlag)) {
                        stats.put(EchantEquipStat.STR, 19);
                    }
                    if (EchantEquipStat.DEX.check(statusFlag)) {
                        stats.put(EchantEquipStat.DEX, 19);
                    }
                    if (EchantEquipStat.INT.check(statusFlag)) {
                        stats.put(EchantEquipStat.INT, 19);
                    }
                    if (EchantEquipStat.LUK.check(statusFlag)) {
                        stats.put(EchantEquipStat.LUK, 19);
                    }
                    break;
                case 1:
                    if (EchantEquipStat.STR.check(statusFlag)) {
                        stats.put(EchantEquipStat.STR, 20);
                    }
                    if (EchantEquipStat.DEX.check(statusFlag)) {
                        stats.put(EchantEquipStat.DEX, 20);
                    }
                    if (EchantEquipStat.INT.check(statusFlag)) {
                        stats.put(EchantEquipStat.INT, 20);
                    }
                    if (EchantEquipStat.LUK.check(statusFlag)) {
                        stats.put(EchantEquipStat.LUK, 20);
                    }
                    break;
                case 2:
                    if (EchantEquipStat.STR.check(statusFlag)) {
                        stats.put(EchantEquipStat.STR, 22);
                    }
                    if (EchantEquipStat.DEX.check(statusFlag)) {
                        stats.put(EchantEquipStat.DEX, 22);
                    }
                    if (EchantEquipStat.INT.check(statusFlag)) {
                        stats.put(EchantEquipStat.INT, 22);
                    }
                    if (EchantEquipStat.LUK.check(statusFlag)) {
                        stats.put(EchantEquipStat.LUK, 22);
                    }
                    break;
                case 3:
                    if (EchantEquipStat.STR.check(statusFlag)) {
                        stats.put(EchantEquipStat.STR, 25);
                    }
                    if (EchantEquipStat.DEX.check(statusFlag)) {
                        stats.put(EchantEquipStat.DEX, 25);
                    }
                    if (EchantEquipStat.INT.check(statusFlag)) {
                        stats.put(EchantEquipStat.INT, 25);
                    }
                    if (EchantEquipStat.LUK.check(statusFlag)) {
                        stats.put(EchantEquipStat.LUK, 25);
                    }
                    break;
                case 4:
                    if (EchantEquipStat.STR.check(statusFlag)) {
                        stats.put(EchantEquipStat.STR, 29);
                    }
                    if (EchantEquipStat.DEX.check(statusFlag)) {
                        stats.put(EchantEquipStat.DEX, 29);
                    }
                    if (EchantEquipStat.INT.check(statusFlag)) {
                        stats.put(EchantEquipStat.INT, 29);
                    }
                    if (EchantEquipStat.LUK.check(statusFlag)) {
                        stats.put(EchantEquipStat.LUK, 29);
                    }
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    stats.put(EchantEquipStat.WATK, enhance + 4);
                    stats.put(EchantEquipStat.MATK, enhance + 4);
                    break;
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                    stats.put(EchantEquipStat.WATK, 15 + 2 * (enhance - 10));
                    stats.put(EchantEquipStat.MATK, 15 + 2 * (enhance - 10));
                    break;
            }
            return stats;
        }
        // 普通強化
        int max = 0;
        switch (enhance) {
            case 0:
            case 1:
            case 2:
                max = 5;
                break;
            case 3:
            case 4:
                max = 10;
                break;
            case 5:
            case 6:
            case 7:
                max = 15;
                break;
            case 8:
                max = 20;
                break;
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
                max = 25;
                break;
            case 14:
                max = 30;
                break;
        }
        if (ItemConstants.類型.武器(itemId) || ItemConstants.類型.雙刀(itemId)) {
            int allStats;
            if (enhance >= 0 && enhance < 5) {
                allStats = 2;
            } else if (enhance >= 5 && enhance < 10) {
                allStats = 2;
            } else if (enhance >= 10 && enhance < 15) {
                allStats = 3;
            } else if (enhance >= 15 && enhance < 20) {
                allStats = 3;
            } else {
                allStats = 11;
            }
            if (EchantEquipStat.STR.check(statusFlag)) {
                stats.put(EchantEquipStat.STR, allStats);
            }
            if (EchantEquipStat.DEX.check(statusFlag)) {
                stats.put(EchantEquipStat.DEX, allStats);
            }
            if (EchantEquipStat.INT.check(statusFlag)) {
                stats.put(EchantEquipStat.INT, allStats);
            }
            if (EchantEquipStat.LUK.check(statusFlag)) {
                stats.put(EchantEquipStat.LUK, allStats);
            }
            stats.put(EchantEquipStat.MHP, max);
            stats.put(EchantEquipStat.MMP, max);
            int atk = watk;
            int addAtk = 0;
            for (int i = 0; i < enhance + 1; i++) {
                addAtk = (int) Math.ceil((atk + 1) / 50.0D);
                atk += addAtk;
            }
            stats.put(EchantEquipStat.WATK, addAtk);
            atk = matk;
            addAtk = 0;
            for (int i = 0; i < enhance + 1; i++) {
                addAtk = (int) Math.ceil((atk + 1) / 50.0D);
                atk += addAtk;
            }
            stats.put(EchantEquipStat.MATK, addAtk);
        } else {
            int allStats;
            if (enhance >= 0 && enhance < 5) {
                allStats = 2;
            } else if (enhance >= 5 && enhance < 10) {
                allStats = 2;
            } else if (enhance >= 10 && enhance < 15) {
                allStats = 3;
            } else if (enhance >= 15 && enhance < 20) {
                allStats = 3;
            } else {
                allStats = 11;
            }
            if (EchantEquipStat.STR.check(statusFlag)) {
                stats.put(EchantEquipStat.STR, allStats);
            }
            if (EchantEquipStat.DEX.check(statusFlag)) {
                stats.put(EchantEquipStat.DEX, allStats);
            }
            if (EchantEquipStat.INT.check(statusFlag)) {
                stats.put(EchantEquipStat.INT, allStats);
            }
            if (EchantEquipStat.LUK.check(statusFlag)) {
                stats.put(EchantEquipStat.LUK, allStats);
            }

            if (!ItemConstants.類型.臉飾(itemId) && !ItemConstants.類型.眼飾(itemId) && !ItemConstants.類型.耳環(itemId)) {
                stats.put(EchantEquipStat.MHP, max);
            }

            if (enhance >= 15) {
                int value = Math.max((reqLevel / 10) - 6, 0);
                stats.put(EchantEquipStat.WATK, value + Math.max((enhance - 15), 1));
                stats.put(EchantEquipStat.MATK, value + Math.max((enhance - 15), 1));
            }
        }
        int stat = wdef;
        int addStat = 0;
        for (int i = 0; i < enhance + 1; i++) {
            addStat = (int) Math.ceil((stat) / 20.0D);
            stat += addStat;
        }
        // stats.put(EchantEquipStat.WDEF, addStat);
        // stat = nEquip.getMdef(0xFFFF - EquipStatsType.STAR_FORCE.getValue());
        // addStat = 0;
        // for (int i = 0; i < enhance + 1; i++) {
        // addStat = (int) Math.ceil((stat) / 20.0D);
        // stat += addStat;
        // }
        // stats.put(EchantEquipStat.MDEF, addStat);
        // int accavoid = 1;
        // switch (enhance) {
        // case 0:
        // case 1:
        // case 3:
        // case 5:
        // accavoid = 0;
        // break;
        // }
        // stats.put(EchantEquipStat.ACC, accavoid);
        // stats.put(EchantEquipStat.AVOID, accavoid);
        return stats;
    }

    public static void StarForceEnhanceItem(Item equip, boolean fall) {
        Equip nEquip = (Equip) equip;
        if (fall) {
            nEquip.setEnhance((byte) (Math.max(0, nEquip.getEnhance() - 1)));
        } else {
            nEquip.setEnhance((byte) (nEquip.getEnhance() + 1));
        }
        nEquip.setStarForce((byte) 17);
    }
}
