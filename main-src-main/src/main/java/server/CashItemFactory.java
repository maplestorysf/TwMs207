package server;

import database.ManagerDatabasePool;
import handling.cashshop.RoyaCoupon;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Pair;

public class CashItemFactory {

    private final static CashItemFactory instance = new CashItemFactory();
    private final Map<Integer, CashItem> itemStats = new HashMap<>();
    private final Map<Integer, List<Integer>> itemPackage = new HashMap<>();
    private final Map<Integer, CashModItem> itemMods = new HashMap<>();
    private final Map<Integer, List<Integer>> openBox = new HashMap<>();
    private final Map<Integer, List<Pair<Integer, Integer>>> unkCoupon = new LinkedHashMap<>();
    private final Map<Integer, List<List<Pair<Integer, Integer>>>> unkCoupon2 = new LinkedHashMap<>();
    private final Map<Integer, RoyaCoupon> royaCoupon = new LinkedHashMap<>();
    private final MapleDataProvider data = MapleDataProviderFactory.getDataProvider("Etc");

    public static CashItemFactory getInstance() {
        return instance;
    }

    public void initialize(boolean reload) {
        if (reload) {
            itemStats.clear();
            itemPackage.clear();
            itemMods.clear();
            openBox.clear();
            unkCoupon.clear();
            unkCoupon2.clear();
            royaCoupon.clear();
        }
        if (!itemStats.isEmpty() || !itemPackage.isEmpty() || !itemMods.isEmpty() || !openBox.isEmpty()
                || !unkCoupon.isEmpty() || !unkCoupon2.isEmpty() || !royaCoupon.isEmpty()) {
            return;
        }
        final List<MapleData> cccc = data.getData("Commodity.img").getChildren();
        for (MapleData field : cccc) {
            final int SN = MapleDataTool.getIntConvert("SN", field, 0);

            final CashItem stats = new CashItem(SN, MapleDataTool.getIntConvert("ItemId", field, 0),
                    MapleDataTool.getIntConvert("Count", field, 1), MapleDataTool.getIntConvert("Price", field, 0),
                    MapleDataTool.getIntConvert("Period", field, 0), MapleDataTool.getIntConvert("Gender", field, 2),
                    MapleDataTool.getIntConvert("Class", field, -1), MapleDataTool.getIntConvert("OnSale", field, 0) > 0
                    && MapleDataTool.getIntConvert("Price", field, 0) > 0);
            if (SN > 0) {
                itemStats.put(SN, stats);
            }
        }

        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM cashshop_items");
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CashModItem ret = new CashModItem(rs.getInt("SN"), rs.getString("Note"), rs.getInt("ItemId"),
                            rs.getInt("Count"), rs.getInt("Price"), rs.getInt("Period"), rs.getInt("Gender"),
                            rs.getInt("Class"), rs.getInt("OnSale") > 0, rs.getInt("Main") > 0);
                    itemMods.put(ret.getSN(), ret);
                    ret.initFlags(getOriginSimpleItem(ret.getSN())); // init
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
        }

        final MapleData b = data.getData("CashPackage.img");
        for (MapleData c : b.getChildren()) {
            if (c.getChildByPath("SN") == null) {
                continue;
            }
            final List<Integer> packageItems = new ArrayList<>();
            for (MapleData d : c.getChildByPath("SN").getChildren()) {
                packageItems.add(MapleDataTool.getIntConvert(d));
            }
            itemPackage.put(Integer.parseInt(c.getName()), packageItems);
        }

        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM royacoupon");
                    ResultSet rs = ps.executeQuery()) {
                RoyaCoupon ret;
                int couponId;
                while (rs.next()) {
                    couponId = rs.getInt("couponId");
                    if (royaCoupon.containsKey(couponId)) {
                        ret = royaCoupon.get(couponId);
                    } else {
                        ret = new RoyaCoupon();
                        royaCoupon.put(couponId, ret);
                    }
                    ret.addStyle(rs.getInt("gender"), rs.getInt("styleId"), rs.getInt("chance"),
                            rs.getInt("showOnCS") > 0);
                }
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException e) {
        }

        List<Integer> availableSN = new LinkedList<>();
        availableSN.add(20001141);
        availableSN.add(20001142);
        availableSN.add(20001143);
        availableSN.add(20001144);
        availableSN.add(20001145);
        availableSN.add(20001146);
        availableSN.add(20001147);
        openBox.put(5533003, availableSN); // 七色彩紅帽箱

        availableSN = new LinkedList<>();
        availableSN.add(20000462);
        availableSN.add(20000463);
        availableSN.add(20000464);
        availableSN.add(20000465);
        availableSN.add(20000466);
        availableSN.add(20000467);
        availableSN.add(20000468);
        availableSN.add(20000469);
        openBox.put(5533000, availableSN); // 戰國壽司帽箱子

        availableSN = new LinkedList<>();
        availableSN.add(20800259);
        availableSN.add(20800260);
        availableSN.add(20800263);
        availableSN.add(20800264);
        availableSN.add(20800265);
        availableSN.add(20800267);
        openBox.put(5533001, availableSN); // 天使光射武器箱

        availableSN = new LinkedList<>();
        availableSN.add(20800270);
        availableSN.add(20800271);
        availableSN.add(20800272);
        availableSN.add(20800273);
        availableSN.add(20800274);
        openBox.put(5533002, availableSN); // 騎士團長的武器

        if (!unkCoupon.containsKey(5680157)) { // 楓之谷皇家風格
            List<Pair<Integer, Integer>> li = new ArrayList<>();
            li.add(new Pair<>(1702523, 1702523)); // [晴天彩虹, 晴天彩虹]);
            li.add(new Pair<>(1072934, 1072934)); // [彩虹運動鞋, 彩虹運動鞋]
            li.add(new Pair<>(1082588, 1082588)); // [彩虹彈珠, 彩虹彈珠]
            li.add(new Pair<>(1062207, 1062207)); // [齁一波一短褲, 齁一波一短褲]
            li.add(new Pair<>(1042319, 1042319)); // [齁一波一T恤, 齁一波一T恤]
            li.add(new Pair<>(1004180, 1004180)); // [齁一波一帽子, 齁一波一帽子]
            li.add(new Pair<>(1702512, 1702512)); // [主角的權杖, 主角的權杖]
            li.add(new Pair<>(1102688, 1102688)); // [爆爆鞭炮, 爆爆鞭炮]
            li.add(new Pair<>(1071078, 1071078)); // [玻璃高跟鞋, 玻璃高跟鞋]
            li.add(new Pair<>(1070061, 1070061)); // [玻璃運動鞋, 玻璃運動鞋]
            li.add(new Pair<>(1051392, 1051392)); // [派對公主, 派對公主]
            li.add(new Pair<>(1050322, 1050322)); // [派對王子, 派對王子]
            li.add(new Pair<>(1004158, 1004158)); // [LED老鼠髮圈, LED老鼠髮圈]
            li.add(new Pair<>(1702503, 1702503)); // [氣泡氣泡 泡泡射擊, 氣泡氣泡 泡泡射擊]
            li.add(new Pair<>(1102674, 1102674)); // [飯團大逃出騷動, 飯團大逃出騷動]
            li.add(new Pair<>(1071076, 1071076)); // [花色郊遊鞋子, 花色郊遊鞋子]
            li.add(new Pair<>(1070059, 1070059)); // [彩色郊遊鞋子, 彩色郊遊鞋子]
            li.add(new Pair<>(1051390, 1051390)); // [迎春花郊遊, 迎春花郊遊]
            li.add(new Pair<>(1050319, 1050319)); // [天空郊遊, 天空郊遊]
            li.add(new Pair<>(1001097, 1001097)); // [花遊貝雷帽, 花遊貝雷帽]
            li.add(new Pair<>(1000074, 1000074)); // [春遊貝雷帽, 春遊貝雷帽]
            li.add(new Pair<>(1702485, 1702485)); // [今日的一袋子, 今日的一袋子]
            li.add(new Pair<>(1102669, 1102669)); // [狂購神妖精, 狂購神妖精]
            li.add(new Pair<>(1072897, 1072897)); // [藍色莫卡辛, 藍色莫卡辛]
            li.add(new Pair<>(1051382, 1051382)); // [可愛的指引, 可愛的指引]
            li.add(new Pair<>(1050310, 1050310)); // [閃亮指引, 閃亮指引]
            li.add(new Pair<>(1001095, 1001095)); // [粉色咚咚, 粉色咚咚]
            li.add(new Pair<>(1000072, 1000072)); // [藍色咚咚, 藍色咚咚]
            li.add(new Pair<>(1702486, 1702486)); // [冬柏飄散著, 冬柏飄散著]
            li.add(new Pair<>(1102667, 1102667)); // [雲上充滿月亮, 雲上充滿月亮]
            li.add(new Pair<>(1072901, 1072901)); // [五色珠子, 五色珠子]
            li.add(new Pair<>(1051383, 1051383)); // [佳人隱月, 佳人隱月]
            li.add(new Pair<>(1050311, 1050311)); // [月光隱月, 月光隱月]
            li.add(new Pair<>(1001092, 1001092)); // [隱月花簪子, 隱月花簪子]
            li.add(new Pair<>(1000069, 1000069)); // [隱月花帽, 隱月花帽]
            li.add(new Pair<>(1702468, 1702468)); // [巧克力棒, 巧克力棒]
            li.add(new Pair<>(1082565, 1082565)); // [巧克力蝴蝶裝飾, 巧克力蝴蝶裝飾]
            li.add(new Pair<>(1072876, 1072876)); // [小熊鞋, 小熊鞋]
            li.add(new Pair<>(1051372, 1051372)); // [巧克女孩, 巧克女孩]
            li.add(new Pair<>(1050304, 1050304)); // [巧克男孩, 巧克男孩]
            li.add(new Pair<>(1003998, 1003998)); // [白巧克力兔耳, 白巧克力兔耳]
            li.add(new Pair<>(1702473, 1702473)); // [黯影執行者, 黯影執行者]
            li.add(new Pair<>(1102632, 1102632)); // [暗影贖罪者, 暗影贖罪者]
            li.add(new Pair<>(1071074, 1071074)); // [黯影長靴, 黯影長靴]
            li.add(new Pair<>(1070057, 1070057)); // [黯影羅馬鞋, 黯影羅馬鞋]
            li.add(new Pair<>(1051373, 1051373)); // [血色黑大衣, 血色黑大衣]
            li.add(new Pair<>(1050305, 1050305)); // [血腥黑外套, 血腥黑外套]
            li.add(new Pair<>(1004002, 1004002)); // [黯影頭巾, 黯影頭巾]
            li.add(new Pair<>(1702464, 1702464)); // [閃亮亮的朋友, 閃亮亮的朋友]
            li.add(new Pair<>(1102621, 1102621)); // [嘮叨喇叭, 嘮叨喇叭]
            li.add(new Pair<>(1072868, 1072868)); // [制服皮鞋, 制服皮鞋]
            li.add(new Pair<>(1051369, 1051369)); // [女僕的矜持, 女僕的矜持]
            li.add(new Pair<>(1050302, 1050302)); // [管家的品格, 管家的品格]
            li.add(new Pair<>(1003972, 1003972)); // [藍蝴蝶結花邊帽子, 藍蝴蝶結花邊帽子]
            li.add(new Pair<>(1003971, 1003971)); // [藍蝴蝶結禮帽, 藍蝴蝶結禮帽]
            li.add(new Pair<>(1702457, 1702457)); // [美味冰斧, 美味冰斧]
            li.add(new Pair<>(1102619, 1102619)); // [美味冰企鵝, 美味冰企鵝]
            li.add(new Pair<>(1072862, 1072862)); // [愛心布丁托鞋, 愛心布丁托鞋]
            li.add(new Pair<>(1051367, 1051367)); // [涼爽冰塊, 涼爽冰塊]
            li.add(new Pair<>(1050300, 1050300)); // [涼爽冰塊, 涼爽冰塊]
            li.add(new Pair<>(1003958, 1003958)); // [粉紅糯米冰, 粉紅糯米冰]
            li.add(new Pair<>(1003957, 1003957)); // [冰薄荷綠糯米, 冰薄荷綠糯米]
            li.add(new Pair<>(1702451, 1702451)); // [超級明星麥克風, 超級明星麥克風]
            li.add(new Pair<>(1102608, 1102608)); // [超級明星鏡球, 超級明星鏡球]
            li.add(new Pair<>(1072852, 1072852)); // [超級明星鞋, 超級明星鞋]
            li.add(new Pair<>(1051362, 1051362)); // [超級明星洋裝, 超級明星洋裝]
            li.add(new Pair<>(1050296, 1050296)); // [超級明星套裝, 超級明星套裝]
            li.add(new Pair<>(1003945, 1003945)); // [超級明星王冠, 超級明星王冠]
            li.add(new Pair<>(1702442, 1702442)); // [棒球棒, 棒球棒]
            li.add(new Pair<>(1102593, 1102593)); // [飄浮棒球, 飄浮棒球]
            li.add(new Pair<>(1072836, 1072836)); // [棒球鞋, 棒球鞋]
            li.add(new Pair<>(1051357, 1051357)); // [粉紅棒球服, 粉紅棒球服]
            li.add(new Pair<>(1050291, 1050291)); // [汀奇棒球, 汀奇棒球]
            li.add(new Pair<>(1003909, 1003909)); // [粉紅蘇打帽, 粉紅蘇打帽]
            li.add(new Pair<>(1702433, 1702433)); // [塞里曼德, 塞里曼德]
            li.add(new Pair<>(1102583, 1102583)); // [小恐龍波比, 小恐龍波比]
            li.add(new Pair<>(1072831, 1072831)); // [火焰靴, 火焰靴]
            li.add(new Pair<>(1051352, 1051352)); // [泰勒米多勒, 泰勒米多勒]
            li.add(new Pair<>(1050285, 1050285)); // [泰勒米多勒, 泰勒米多勒]
            li.add(new Pair<>(1003892, 1003892)); // [躍升之鑽, 躍升之鑽]
            li.add(new Pair<>(1702424, 1702424)); // [時尚鋼鐵, 時尚鋼鐵]
            li.add(new Pair<>(1082527, 1082527)); // [高爾夫手套, 高爾夫手套]
            li.add(new Pair<>(1072823, 1072823)); // [高爾夫運動鞋, 高爾夫運動鞋]
            li.add(new Pair<>(1042264, 1042264)); // [卡拉高爾夫T恤, 卡拉高爾夫T恤]
            li.add(new Pair<>(1061206, 1061206)); // [高爾夫短裙, 高爾夫短裙]
            li.add(new Pair<>(1060182, 1060182)); // [高爾夫短褲, 高爾夫短褲]
            li.add(new Pair<>(1003867, 1003867)); // [神射帽, 神射帽]
            li.add(new Pair<>(1702415, 1702415)); // [作夢的糖果枕頭, 作夢的糖果枕頭]
            li.add(new Pair<>(1072808, 1072808)); // [小羊拖鞋, 小羊拖鞋]
            li.add(new Pair<>(1082520, 1082520)); // [小羊毛手套, 小羊毛手套]
            li.add(new Pair<>(1052605, 1052605)); // [小羊睡袍, 小羊睡袍]
            li.add(new Pair<>(1003831, 1003831)); // [小羊髮夾, 小羊髮夾]
            li.add(new Pair<>(1702406, 1702406)); // [殞落魔法方塊, 殞落魔法方塊]
            li.add(new Pair<>(1102537, 1102537)); // [魔法之星斗篷, 魔法之星斗篷]
            li.add(new Pair<>(1051347, 1051347)); // [魔法之星洋裝, 魔法之星洋裝]
            li.add(new Pair<>(1050283, 1050283)); // [魔法之星套裝, 魔法之星套裝]
            li.add(new Pair<>(1003809, 1003809)); // [神秘黑蝴蝶結, 神秘黑蝴蝶結]
            li.add(new Pair<>(1003808, 1003808)); // [神秘黑絲帽, 神秘黑絲帽]
            unkCoupon.put(5680157, li);
        }
        if (!unkCoupon.containsKey(5680159)) { // 楓之谷皇家風格
            List<Pair<Integer, Integer>> li = new ArrayList<>();
            li.add(new Pair<>(1702523, 1702523)); // [晴天彩虹, 晴天彩虹]
            li.add(new Pair<>(1072934, 1072934)); // [彩虹運動鞋, 彩虹運動鞋]
            li.add(new Pair<>(1082588, 1082588)); // [彩虹彈珠, 彩虹彈珠]
            li.add(new Pair<>(1062207, 1062207)); // [齁一波一短褲, 齁一波一短褲]
            li.add(new Pair<>(1042319, 1042319)); // [齁一波一T恤, 齁一波一T恤]
            li.add(new Pair<>(1004180, 1004180)); // [齁一波一帽子, 齁一波一帽子]
            li.add(new Pair<>(1052762, 1052762)); // [香蕉背帶, 香蕉背帶]
            li.add(new Pair<>(1072838, 1072838)); // [貓熊拖鞋, 貓熊拖鞋]
            li.add(new Pair<>(1062175, 1062175)); // [撲通撲通貼身牛仔褲, 撲通撲通貼身牛仔褲]
            li.add(new Pair<>(1062208, 1062208)); // [膝蓋兔寶貝褲子, 膝蓋兔寶貝褲子]
            li.add(new Pair<>(1042216, 1042216)); // [楓葉小隊棒球上衣, 楓葉小隊棒球上衣]
            li.add(new Pair<>(1042125, 1042125)); // [小兔包包黃色T恤, 小兔包包黃色T恤]
            li.add(new Pair<>(1003807, 1003807)); // [愛心墨鏡, 愛心墨鏡]
            li.add(new Pair<>(1002598, 1002598)); // [可愛兔子耳朵, 可愛兔子耳朵]
            li.add(new Pair<>(1003912, 1003912)); // [今天是小狗！, 今天是小狗！]
            li.add(new Pair<>(1003586, 1003586)); // [薄荷星馬藍帽, 薄荷星馬藍帽]
            li.add(new Pair<>(1004181, 1004181)); // [糖果派對色帶髮圈, 糖果派對色帶髮圈]
            li.add(new Pair<>(1702525, 1702525)); // [料理的完成, 料理的完成]
            li.add(new Pair<>(1702405, 1702405)); // [星光愛心魔術棒, 星光愛心魔術棒]
            li.add(new Pair<>(1702443, 1702443)); // [人偶師的詛咒, 人偶師的詛咒]
            li.add(new Pair<>(1102700, 1102700)); // [小希爾, 小希爾]
            li.add(new Pair<>(1102644, 1102644)); // [小精靈, 小精靈]
            li.add(new Pair<>(1102547, 1102547)); // [紫水晶的夢想, 紫水晶的夢想]
            li.add(new Pair<>(1012437, 1012437)); // [手掌臉部裝飾, 手掌臉部裝飾]
            li.add(new Pair<>(1022230, 1022230)); // [巴尼玻璃, 巴尼玻璃]
            li.add(new Pair<>(5530638, 5530638)); // [[30天]水墨畫泡泡戒指交換券, [30天]水墨畫泡泡戒指交換券]
            li.add(new Pair<>(5530637, 5530637)); // [[30天]水墨畫名牌戒指交換券, [30天]水墨畫名牌戒指交換券]
            li.add(new Pair<>(5680262, 5680262)); // [我愛法國效果道具交換券, 我愛法國效果道具交換券]
            li.add(new Pair<>(5065100, 5065100)); // [特別版連發煙火, 特別版連發煙火]
            li.add(new Pair<>(5121033, 5121033)); // [星星糖果, 星星糖果]
            li.add(new Pair<>(5450010, 5450010)); // [貓商人奈洛, 貓商人奈洛]
            li.add(new Pair<>(5390009, 5390009)); // [名叫朋友的喇叭, 名叫朋友的喇叭]
            li.add(new Pair<>(5390000, 5390000)); // [炎熱喇叭, 炎熱喇叭]
            unkCoupon.put(5680159, li);
        }
        if (!unkCoupon2.containsKey(5069001)) { // 優質大師零件
            List<List<Pair<Integer, Integer>>> li = new ArrayList<>();
            List<Pair<Integer, Integer>> li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券,
            // 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1102729, 1102729)); // [閃耀燈籠, 閃耀燈籠]
            li2.add(new Pair<>(1102748, 1102748)); // [兔子熊露營袋, 兔子熊露營袋]
            li2.add(new Pair<>(1102712, 1102712)); // [輾轉不寐的度假勝地, 輾轉不寐的度假勝地]
            li2.add(new Pair<>(1102706, 1102706)); // [叮咚歐洛拉, 叮咚歐洛拉]
            li2.add(new Pair<>(1082588, 1082588)); // [彩虹彈珠, 彩虹彈珠]
            li2.add(new Pair<>(1102688, 1102688)); // [爆爆鞭炮, 爆爆鞭炮]
            li2.add(new Pair<>(1102674, 1102674)); // [飯團大逃出騷動, 飯團大逃出騷動]
            li2.add(new Pair<>(1102669, 1102669)); // [狂購神妖精, 狂購神妖精]
            li2.add(new Pair<>(1102667, 1102667)); // [雲上充滿月亮, 雲上充滿月亮]
            li2.add(new Pair<>(1082565, 1082565)); // [巧克力蝴蝶裝飾, 巧克力蝴蝶裝飾]
            li.add(new ArrayList<>(li2));
            li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券, 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1000076, 1001098)); // [赤紅黃昏, 藍色新野]
            li2.add(new Pair<>(1004279, 1004279)); // [松鼠帽, 松鼠帽]
            li2.add(new Pair<>(1004213, 1004213)); // [呼啦呼啦羽毛裝飾, 呼啦呼啦羽毛裝飾]
            li2.add(new Pair<>(1004192, 1004192)); // [DoReMi耳機, DoReMi耳機]
            li2.add(new Pair<>(1004180, 1004180)); // [齁一波一帽子, 齁一波一帽子]
            li2.add(new Pair<>(1004158, 1004158)); // [LED老鼠髮圈, LED老鼠髮圈]
            li2.add(new Pair<>(1000074, 1001097)); // [春遊貝雷帽, 花遊貝雷帽]
            li2.add(new Pair<>(1000072, 1001095)); // [藍色咚咚, 粉色咚咚]
            li2.add(new Pair<>(1000069, 1001092)); // [隱月花帽, 隱月花簪子]
            li2.add(new Pair<>(1003998, 1003998)); // [白巧克力兔耳, 白巧克力兔耳]
            li.add(new ArrayList<>(li2));
            li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券, 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1702538, 1702538)); // [露水燈籠, 露水燈籠]
            li2.add(new Pair<>(1702540, 1702540)); // [在這裡！手電筒, 在這裡！手電筒]
            li2.add(new Pair<>(1702535, 1702535)); // [呼啦呼啦小企鵝, 呼啦呼啦小企鵝]
            li2.add(new Pair<>(1702528, 1702528)); // [木琴旋律, 木琴旋律]
            li2.add(new Pair<>(1702523, 1702523)); // [晴天彩虹, 晴天彩虹]
            li2.add(new Pair<>(1702512, 1702512)); // [主角的權杖, 主角的權杖]
            li2.add(new Pair<>(1702503, 1702503)); // [氣泡氣泡 泡泡射擊, 氣泡氣泡 泡泡射擊]
            li2.add(new Pair<>(1702485, 1702485)); // [今日的一袋子, 今日的一袋子]
            li2.add(new Pair<>(1702486, 1702486)); // [冬柏飄散著, 冬柏飄散著]
            li2.add(new Pair<>(1702468, 1702468)); // [巧克力棒, 巧克力棒]
            li.add(new ArrayList<>(li2));
            li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券, 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1050339, 1051408)); // [光輝燈籠, 燦爛燈籠]
            li2.add(new Pair<>(1050341, 1051410)); // [叢林露營造型, 叢林露營造型]
            li2.add(new Pair<>(1050337, 1051406)); // [夏威夷情侶, 夏威夷情侶]
            li2.add(new Pair<>(1050335, 1051405)); // [旋律少年, 旋律少女]
            li2.add(new Pair<>(1062207, 1062207)); // [齁一波一短褲, 齁一波一短褲]
            li2.add(new Pair<>(1042319, 1042319)); // [齁一波一T恤, 齁一波一T恤]
            li2.add(new Pair<>(1050322, 1051392)); // [派對王子, 派對公主]
            li2.add(new Pair<>(1050319, 1051390)); // [天空郊遊, 迎春花郊遊]
            li2.add(new Pair<>(1050310, 1051382)); // [閃亮指引, 可愛的指引]
            li2.add(new Pair<>(1050311, 1051383)); // [月光隱月, 佳人隱月]
            li2.add(new Pair<>(1050304, 1051372)); // [巧克男孩, 巧克女孩]
            li.add(new ArrayList<>(li2));
            li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券, 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1072978, 1072978)); // [燈籠唐鞋, 燈籠唐鞋]
            li2.add(new Pair<>(1072998, 1072998)); // [兔子熊拖鞋, 兔子熊拖鞋]
            li2.add(new Pair<>(1072951, 1072951)); // [呼啦呼啦串珠腳環, 呼啦呼啦串珠腳環]
            li2.add(new Pair<>(1072943, 1072943)); // [合鳴鞋, 合鳴鞋]
            li2.add(new Pair<>(1072934, 1072934)); // [彩虹運動鞋, 彩虹運動鞋]
            li2.add(new Pair<>(1070061, 1071078)); // [玻璃運動鞋, 玻璃高跟鞋]
            li2.add(new Pair<>(1070059, 1071076)); // [彩色郊遊鞋子, 花色郊遊鞋子]
            li2.add(new Pair<>(1072897, 1072897)); // [藍色莫卡辛, 藍色莫卡辛]
            li2.add(new Pair<>(1072901, 1072901)); // [五色珠子, 五色珠子]
            li2.add(new Pair<>(1072876, 1072876)); // [小熊鞋, 小熊鞋]
            li.add(new ArrayList<>(li2));
            unkCoupon2.put(5069001, li);
        }
        if (!unkCoupon2.containsKey(5069000)) { // 大師零件
            List<List<Pair<Integer, Integer>>> li = new ArrayList<>();
            List<Pair<Integer, Integer>> li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券,
            // 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1102729, 1102729)); // [閃耀燈籠, 閃耀燈籠]
            li2.add(new Pair<>(1102748, 1102748)); // [兔子熊露營袋, 兔子熊露營袋]
            li2.add(new Pair<>(1102712, 1102712)); // [輾轉不寐的度假勝地, 輾轉不寐的度假勝地]
            li2.add(new Pair<>(1102706, 1102706)); // [叮咚歐洛拉, 叮咚歐洛拉]
            li2.add(new Pair<>(1082588, 1082588)); // [彩虹彈珠, 彩虹彈珠]
            li2.add(new Pair<>(1102688, 1102688)); // [爆爆鞭炮, 爆爆鞭炮]
            li2.add(new Pair<>(1102674, 1102674)); // [飯團大逃出騷動, 飯團大逃出騷動]
            li2.add(new Pair<>(1102669, 1102669)); // [狂購神妖精, 狂購神妖精]
            li2.add(new Pair<>(1102667, 1102667)); // [雲上充滿月亮, 雲上充滿月亮]
            li2.add(new Pair<>(1082565, 1082565)); // [巧克力蝴蝶裝飾, 巧克力蝴蝶裝飾]
            li.add(new ArrayList<>(li2));
            li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券, 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1000076, 1001098)); // [赤紅黃昏, 藍色新野]
            li2.add(new Pair<>(1004279, 1004279)); // [松鼠帽, 松鼠帽]
            li2.add(new Pair<>(1004213, 1004213)); // [呼啦呼啦羽毛裝飾, 呼啦呼啦羽毛裝飾]
            li2.add(new Pair<>(1004192, 1004192)); // [DoReMi耳機, DoReMi耳機]
            li2.add(new Pair<>(1004180, 1004180)); // [齁一波一帽子, 齁一波一帽子]
            li2.add(new Pair<>(1004158, 1004158)); // [LED老鼠髮圈, LED老鼠髮圈]
            li2.add(new Pair<>(1000074, 1001097)); // [春遊貝雷帽, 花遊貝雷帽]
            li2.add(new Pair<>(1000072, 1001095)); // [藍色咚咚, 粉色咚咚]
            li2.add(new Pair<>(1000069, 1001092)); // [隱月花帽, 隱月花簪子]
            li2.add(new Pair<>(1003998, 1003998)); // [白巧克力兔耳, 白巧克力兔耳]
            li.add(new ArrayList<>(li2));
            li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券, 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1702538, 1702538)); // [露水燈籠, 露水燈籠]
            li2.add(new Pair<>(1702540, 1702540)); // [在這裡！手電筒, 在這裡！手電筒]
            li2.add(new Pair<>(1702535, 1702535)); // [呼啦呼啦小企鵝, 呼啦呼啦小企鵝]
            li2.add(new Pair<>(1702528, 1702528)); // [木琴旋律, 木琴旋律]
            li2.add(new Pair<>(1702523, 1702523)); // [晴天彩虹, 晴天彩虹]
            li2.add(new Pair<>(1702512, 1702512)); // [主角的權杖, 主角的權杖]
            li2.add(new Pair<>(1702503, 1702503)); // [氣泡氣泡 泡泡射擊, 氣泡氣泡 泡泡射擊]
            li2.add(new Pair<>(1702485, 1702485)); // [今日的一袋子, 今日的一袋子]
            li2.add(new Pair<>(1702486, 1702486)); // [冬柏飄散著, 冬柏飄散著]
            li2.add(new Pair<>(1702468, 1702468)); // [巧克力棒, 巧克力棒]
            li.add(new ArrayList<>(li2));
            li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券, 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1050339, 1051408)); // [光輝燈籠, 燦爛燈籠]
            li2.add(new Pair<>(1050341, 1051410)); // [叢林露營造型, 叢林露營造型]
            li2.add(new Pair<>(1050337, 1051406)); // [夏威夷情侶, 夏威夷情侶]
            li2.add(new Pair<>(1050335, 1051405)); // [旋律少年, 旋律少女]
            li2.add(new Pair<>(1062207, 1062207)); // [齁一波一短褲, 齁一波一短褲]
            li2.add(new Pair<>(1042319, 1042319)); // [齁一波一T恤, 齁一波一T恤]
            li2.add(new Pair<>(1050322, 1051392)); // [派對王子, 派對公主]
            li2.add(new Pair<>(1050319, 1051390)); // [天空郊遊, 迎春花郊遊]
            li2.add(new Pair<>(1050310, 1051382)); // [閃亮指引, 可愛的指引]
            li2.add(new Pair<>(1050311, 1051383)); // [月光隱月, 佳人隱月]
            li2.add(new Pair<>(1050304, 1051372)); // [巧克男孩, 巧克女孩]
            li.add(new ArrayList<>(li2));
            li2 = new ArrayList<>();
            li2.add(new Pair<>(5530697, 5530698)); // [閃亮蝴蝶美髮券(男)交換券, 閃亮蝴蝶美髮券(女)交換券]
            li2.add(new Pair<>(1072978, 1072978)); // [燈籠唐鞋, 燈籠唐鞋]
            li2.add(new Pair<>(1072998, 1072998)); // [兔子熊拖鞋, 兔子熊拖鞋]
            li2.add(new Pair<>(1072951, 1072951)); // [呼啦呼啦串珠腳環, 呼啦呼啦串珠腳環]
            li2.add(new Pair<>(1072943, 1072943)); // [合鳴鞋, 合鳴鞋]
            li2.add(new Pair<>(1072934, 1072934)); // [彩虹運動鞋, 彩虹運動鞋]
            li2.add(new Pair<>(1070061, 1071078)); // [玻璃運動鞋, 玻璃高跟鞋]
            li2.add(new Pair<>(1070059, 1071076)); // [彩色郊遊鞋子, 花色郊遊鞋子]
            li2.add(new Pair<>(1072897, 1072897)); // [藍色莫卡辛, 藍色莫卡辛]
            li2.add(new Pair<>(1072901, 1072901)); // [五色珠子, 五色珠子]
            li2.add(new Pair<>(1072876, 1072876)); // [小熊鞋, 小熊鞋]
            li.add(new ArrayList<>(li2));
            unkCoupon2.put(5069000, li);
        }
    }

    public final List<CashItem> getAllItems() {
        List<CashItem> allItem = new LinkedList<>();
        for (CashItem ci : itemStats.values()) {
            allItem.add(ci);
        }
        return allItem;
    }

    public final List<CashItem> getHideAllDefaultItems() {
        List<CashItem> allItem = new LinkedList<>();
        for (CashItem ci : itemStats.values()) {
            CashModItem csMod = getSimpleItem(ci.getSN());
            if (ci.isOnSale() && (csMod == null || !csMod.isOnSale())) {
                allItem.add(ci);
            }
        }
        return allItem;
    }

    public final List<CashModItem> getAllModItems() {
        List<CashModItem> allItem = new LinkedList<>();
        for (CashModItem csMod : getAllModInfo()) {
            CashItem ci = getOriginSimpleItem(csMod.getSN());
            if (!csMod.sameAsCashItem() && (ci == null || !(ci.isOnSale() && !csMod.isOnSale()))) {
                allItem.add(csMod);
            }
        }
        return allItem;
    }

    public final List<CashModItem> getMainItems() {
        List<CashModItem> mainItem = new LinkedList<>();
        for (CashModItem csMod : getAllModInfo()) {
            if (csMod.isMainItem()) {
                mainItem.add(csMod);
            }
        }
        return mainItem;
    }

    public final CashModItem getSimpleItem(int sn) {
        return itemMods.containsKey(sn) ? itemMods.get(sn) : null;
    }

    public final CashModItem getItem(int sn) {
        final CashModItem stats = getSimpleItem(sn);
        if (stats == null || !stats.isOnSale()) {
            return null;
        }
        return stats;
    }

    public final CashItem getOriginSimpleItem(int sn) {
        return itemStats.containsKey(sn) ? itemStats.get(sn) : null;
    }

    public final CashItem getOriginItem(int sn) {
        final CashItem stats = getOriginSimpleItem(sn);
        if (stats == null || !stats.isOnSale()) {
            return null;
        }
        return stats;
    }

    public final List<Integer> getPackageItems(int itemId) {
        return itemPackage.get(itemId);
    }

    public final Collection<CashModItem> getAllModInfo() {
        return itemMods.values();
    }

    public final Map<Integer, List<Integer>> getRandomItemInfo() {
        return openBox;
    }

    public final Map<Integer, List<Pair<Integer, Integer>>> getUnkCoupon() {
        return unkCoupon;
    }

    public final Map<Integer, List<List<Pair<Integer, Integer>>>> getUnkCoupon2() {
        return unkCoupon2;
    }

    public final Map<Integer, RoyaCoupon> getRoyaCoupon() {
        return royaCoupon;
    }

    public final int getItemSN(int itemid) {
        for (Entry<Integer, CashModItem> ci : itemMods.entrySet()) {
            if (ci.getValue().getItemId() == itemid) {
                return ci.getValue().getSN();
            }
        }
        for (Entry<Integer, CashItem> ci : itemStats.entrySet()) {
            if (ci.getValue().getItemId() == itemid) {
                return ci.getValue().getSN();
            }
        }
        return 0;
    }

    public void addModItem(CashModItem cModItem) {
        if (!itemMods.containsKey(cModItem.getSN())) {
            itemMods.put(cModItem.getSN(), cModItem);
            cModItem.initFlags(getOriginSimpleItem(cModItem.getSN()));

            try {
                Connection con = ManagerDatabasePool.getConnection();
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO cashshop_items (SN, Note, ItemId, Count, Price, Period, Gender, Class, OnSale, Main) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, cModItem.getSN());
                    ps.setString(2, cModItem.getNote());
                    ps.setInt(3, cModItem.getItemId());
                    ps.setInt(4, cModItem.getCount());
                    ps.setInt(5, cModItem.getPrice());
                    ps.setInt(6, cModItem.getPeriod());
                    ps.setInt(7, cModItem.getGender());
                    ps.setInt(8, cModItem.getClass_());
                    ps.setInt(9, cModItem.isOnSale() ? 1 : 0);
                    ps.setInt(10, cModItem.isMainItem() ? 1 : 0);
                    ps.execute();
                }
                ManagerDatabasePool.closeConnection(con);
            } catch (SQLException ex) {
            }
        }
    }

    public void updateModItem(CashModItem cModItem) {
        itemMods.put(cModItem.getSN(), cModItem);
        cModItem.initFlags(getOriginSimpleItem(cModItem.getSN()));

        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE cashshop_items SET Note = ?, ItemId = ?, Count = ?, Price = ?, Period = ?, Gender = ?, Class = ?, OnSale = ?, Main = ? WHERE SN = ?")) {
                ps.setString(1, cModItem.getNote());
                ps.setInt(2, cModItem.getItemId());
                ps.setInt(3, cModItem.getCount());
                ps.setInt(4, cModItem.getPrice());
                ps.setInt(5, cModItem.getPeriod());
                ps.setInt(6, cModItem.getGender());
                ps.setInt(7, cModItem.getClass_());
                ps.setInt(8, cModItem.isOnSale() ? 1 : 0);
                ps.setInt(9, cModItem.isMainItem() ? 1 : 0);
                ps.setInt(10, cModItem.getSN());
                ps.execute();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
        }
    }

    public void deleteModItem(int sn) {
        itemMods.remove(sn);
        try {
            Connection con = ManagerDatabasePool.getConnection();
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM cashshop_items WHERE SN = ?")) {
                ps.setInt(1, sn);
                ps.execute();
            }
            ManagerDatabasePool.closeConnection(con);
        } catch (SQLException ex) {
        }
    }
}
