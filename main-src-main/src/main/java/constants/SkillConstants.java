/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import client.MapleJob;
import client.PlayerStats;
import java.util.LinkedList;
import java.util.List;
import server.ItemInformation;
import server.MapleItemInformationProvider;

/**
 *
 * @author pungin
 */
public class SkillConstants {

    public static boolean isSkill9200(int skillId) {
        int v1 = 10000 * skillId / 10000;
        return (skillId - 92000000 >= 1000000 || skillId % 10000 > 0) && v1 - 92000000 < 1000000 && v1 % 10000 == 0;
    }

    public static boolean is初心者Skill(int skillId) {
        int v1 = getJobBySkill(skillId);
        if (v1 - 40000 > 5 || v1 - 40000 < 0) {
//            return MapleJob.is初心者(v1);
            return MapleJob.isBeginner(v1);
        } else {
            return false;
        }
    }

    public static boolean is4thNotNeedMasterLevel(int skillId) {
        if (skillId <= 5320007) { // 雙倍幸運骰子
            if (skillId == 5320007) { // 雙倍幸運骰子
                return true;
            }
            if (skillId > 4210012) { // 貪婪
                if (skillId > 5220012) { // 反擊
                    if (skillId == 5220014) { // 雙倍幸運骰子
                        return true;
                    }
                    return skillId == 5221022; // 海盜砲擊艇
                } else {
                    if (skillId == 5220012) { // 反擊
                        return true;
                    }
                    if (skillId > 4340012) { // 致命的飛毒殺
                        // 反擊姿態 || 雙倍幸運骰子
                        if (skillId < 5120011 || skillId > 5120012) {
                            return false;
                        }
                        return true;
                    }
                    if (skillId == 4340012) { // 致命的飛毒殺
                        return true;
                    }
                    return skillId == 4340010; // 疾速
                }
            } else {
                if (skillId == 4210012) { // 貪婪
                    return true;
                }
                if (skillId > 2221009) { // Null(沒找到技能)
                    // Null(沒找到技能) || 射擊術
                    if (skillId == 2321010 || skillId == 3210015) {
                        return true;
                    }
                    return skillId == 4110012; // 鏢術精通
                } else {
                    // Null(沒找到技能) || 戰鬥精通 || 靈魂復仇
                    if (skillId == 2221009 || skillId == 1120012 || skillId == 1320011) {
                        return true;
                    }
                    return skillId == 2121009; // green card
                }
            }
        }
        if (skillId > 23120011) { // 旋風月光翻轉
            if (skillId > 35120014) { // 雙倍幸運骰子
                if (skillId == 51120000) { // 戰鬥大師
                    return true;
                }
                return skillId == 80001913; // 爆流拳
            } else {
                // 雙倍幸運骰子 || 進階光速雙擊 || 勇士的意志
                if (skillId == 35120014 || skillId == 23120013 || skillId == 23121008) {
                    return true;
                }
                return skillId == 33120010; // 狂暴天性
            }
        }
        if (skillId == 23120011) { // 旋風月光翻轉
            return true;
        }
        if (skillId <= 21120014) { // Null(沒找到技能)
            // Null(沒找到技能) || 雙胞胎猴子 || 楓葉淨化
            if (skillId == 21120014 || skillId == 5321004 || skillId == 5321006) {
                return true;
            }
            return skillId == 21120011; // 快速移動
        }
        if (skillId > 21121008) { // 楓葉淨化
            return skillId == 22171069; // 楓葉淨化
        }
        if (skillId == 21121008) { // 楓葉淨化
            return true;
        }
        if (skillId >= 21120020) { // 動力精通II
            if (skillId > 21120021) { // 終極研究 II
                return false;
            }
            return true;
        }
        return false;
    }

    public static boolean isNot4thNeedMasterLevel(int skillId) {
        if (skillId > 101100101) { // 進階武器投擲
            if (skillId > 101110203) { // 進階旋風落葉斬
                if (skillId == 101120104) { // 進階碎地猛擊
                    return true;
                }
                return skillId == 101120204; // 進階暴風裂擊
            } else {
                // 進階旋風落葉斬 || 進階迴旋之刃 || 進階旋風
                if (skillId == 101110203 || skillId == 101100201 || skillId == 101110102) {
                    return true;
                }
                return skillId == 101110200; // 進階旋風急轉彎
            }
        } else {
            if (skillId == 101100101) { // 進階武器投擲
                return true;
            }
            if (skillId > 4331002) { // 替身術
                // 荊棘特效 || 短刀護佑
                if (skillId == 4340007 || skillId == 4341004) {
                    return true;
                }
                return skillId == 101000101; // 進階威力震擊
            } else {
                // 替身術 || 狂刃風暴 || 翔空落葉斬
                if (skillId == 4331002 || skillId == 4311003 || skillId == 4321006) {
                    return true;
                }
                return skillId == 4330009; // 暗影迴避
            }
        }
    }

    public static boolean isSkillNeedMasterLevel(int skillId) {
        if (is4thNotNeedMasterLevel(skillId)
                || skillId - 92000000 < 1000000 && (skillId % 10000) == 0
                || isSkill9200(skillId)
                || MapleJob.isJob8000(skillId)
                || is初心者Skill(skillId)
                || MapleJob.isJob9500(skillId)) {
            return false;
        } else {
            int jobid = getJobBySkill(skillId);
            int jobTimes = MapleJob.get轉數(jobid);
            return (jobid - 40000 > 5 || jobid - 40000 < 0) && skillId != 42120024 // 影朋‧花狐
                    && !MapleJob.is幻獸師(jobid)
                    && (isNot4thNeedMasterLevel(skillId) || jobTimes == 4 && !MapleJob.is神之子(jobid));
        }
    }

    public static int get紫扇傳授UnknownValue(int skillId) {
        int result;
        if (skillId == 40020002 || skillId == 80000004) {
            result = 100;
        } else {
            result = 0;
        }
        return result;
    }

    public static int getJobBySkill(int skillId) {
        int result = skillId / 10000;
        if (skillId / 10000 == 8000) {
            result = skillId / 100;
        }
        return result;
    }

    public static boolean isApplicableSkill(int skil) {
        return ((skil < 80000000 || skil >= 100000000) && (skil % 10000 < 8000 || skil % 10000 > 8006)
                && !isAngel(skil)) || skil >= 92000000 || (skil >= 80000000 && skil < 80010000); // no additional/decent
        // skills
    }

    public static boolean isApplicableSkill_(int skil) { // not applicable to saving but is more of temporary
        for (int i : PlayerStats.pvpSkills) {
            if (skil == i) {
                return true;
            }
        }
        return (skil >= 90000000 && skil < 92000000) || (skil % 10000 >= 8000 && skil % 10000 <= 8003) || isAngel(skil);
    }

    public static boolean isRidingSKill(int skil) {
        return (skil >= 80001000 && skil < 80010000);
    }

    public static boolean isAngel(int skillId) {
        if (MapleJob.isBeginner(skillId / 10000) || skillId / 100000 == 800) {
            switch (skillId % 10000) {
                case 1085: // 大天使 [等級上限：1]\n召喚被大天使祝福封印的大天使。
                case 1087: // 黑天使 [等級上限：1]\n召喚被黑天使祝福封印的大天使。
                case 1090: // 大天使 [等級上限：1]\n召喚被大天使祝福封印的大天使。
                case 1179: // 白色天使 [最高等級： 1]\n召喚出被封印的聖潔天使。
                case 86: // 大天使祝福 [等級上限：1]\n得到大天使的祝福。
                    return true;
            }
        }
        switch (skillId) {
            case 80000052: // 恶魔之息 获得恶魔的力量，攻击力和魔法攻击力增加6，HP、MP增加5%，可以和其他增益叠加。
            case 80000053: // 恶魔召唤 获得恶魔的力量，攻击力和魔法攻击力增加13，HP、MP增加10%，可以和其他增益叠加。
            case 80000054: // 恶魔契约 获得恶魔的力量，攻击力和魔法攻击力增加15，HP、MP增加20%，可以和其他增益叠加。
            case 80000086: // 戰神祝福 [等級上限：1]\n得到戰神的祝福。
            case 80001154: // 白色天使 [最高等級：1]\n召喚被白天使的祝福封印的白天使。
            case 80001262: // 戰神祝福 [等級上限：1]\n召喚戰神
            case 80001518: // 元素瑪瑙 召喚瑪瑙戒指中的#c元素瑪瑙#.
            case 80001519: // 火焰瑪瑙 召喚瑪瑙戒指中的#c火焰瑪瑙#.
            case 80001520: // 閃電瑪瑙 召喚瑪瑙戒指中的#c火焰瑪瑙#.
            case 80001521: // 冰凍瑪瑙 召喚瑪瑙戒指中的#c冰凍瑪瑙#.
            case 80001522: // 大地瑪瑙 召喚瑪瑙戒指中的#c大地瑪瑙#.
            case 80001523: // 黑暗瑪瑙 召喚瑪瑙戒指中的#c黑暗瑪瑙#.
            case 80001524: // 神聖瑪瑙 召喚瑪瑙戒指中的#c神聖瑪瑙#.
            case 80001525: // 火精靈瑪瑙 召喚瑪瑙戒指中的#c火精靈瑪瑙#.
            case 80001526: // 電子瑪瑙 召喚瑪瑙戒指中的#c電子瑪瑙#.
            case 80001527: // 水精靈瑪瑙 召喚瑪瑙戒指中的#c水精靈瑪瑙#.
            case 80001528: // 地精靈瑪瑙 召喚瑪瑙戒指中的#c地精靈瑪瑙#.
            case 80001529: // 惡魔瑪瑙 召喚瑪瑙戒指中的#c惡魔瑪瑙#.
            case 80001530: // 天使瑪瑙 召喚瑪瑙戒指中的#c天使瑪瑙#.
            case 80001715: // 元素瑪瑙
            case 80001716: // 火焰瑪瑙
            case 80001717: // 閃電瑪瑙
            case 80001718: // 冰凍瑪瑙
            case 80001719: // 大地瑪瑙
            case 80001720: // 黑暗瑪瑙
            case 80001721: // 神聖瑪瑙
            case 80001722: // 火精靈瑪瑙
            case 80001723: // 電子精靈瑪瑙
            case 80001724: // 水精靈瑪瑙
            case 80001725: // 地精靈瑪瑙
            case 80001726: // 惡魔瑪瑙
            case 80001727: // 天使瑪瑙
                return true;
        }
        return false;
    }

    public static boolean is紫扇仰波(int id) {
        return id == 42001000 || id > 42001004 && id <= 42001006;
    }

    public static boolean is初心者紫扇仰波(int id) {
        return id == 40021185 || id == 42001006 || id == 80011067;
    }

    public static boolean sub_9F5282(int id) {
        return id == 4221052 || id == 65121052; // 暗影霧殺 || 超級超新星
    }

    public static boolean sub_9F529C(int id) {
        return id == 13121052 // 季風
                || id == 14121052 // 道米尼奧
                || id == 15121052 // 海神降臨
                || id == 80001429 // 崩壞之輪行蹤
                || id == 80001431 // 破滅之輪行蹤
                || id == 100001283; // 暗影之雨
    }

    public static boolean isKeyDownSkillWithPos(int id) {
        return id == 13111020 || id == 112111016; // 寒冰亂舞 || 旋風飛行
    }

    public static int getHyperAddBullet(int id) {
        if (id == 4121013) { // 四飛閃
            return 4120051; // 四飛閃-攻擊加成
        } else if (id == 5321012) { // 加農砲連擊
            return 5320051; // 加農砲連擊-獎勵攻擊
        }
        return 0;
    }

    public static int getHyperAddAttack(int id) {
        if (id > 12120011) { // 極致熾烈
            if (id > 41121001) { // 神速無雙
                if (id > 61121100) { // 藍焰恐懼
                    if (id > 112101009) { // 電光石火
                        if (id == 112111004) { // 隊伍轟炸
                            return 112120050; // 隊伍轟炸-臨時目標
                        } else if (id > 112119999 && id <= 112120003) { // 朋友發射
                            return 112120053;
                        }
                        return 0;
                    }
                    if (id == 112101009) { // 電光石火
                        return 112120048; // 電光石火-攻擊加成
                    }
                    if (id != 61121201) { // 藍焰恐懼(變身)
                        if (id > 65121006 && (id <= 65121008 || id == 65121101)) { // 三位一體
                            return 65120051; // 三位一體-三重反擊
                        }
                        return 0;
                    }
                } else if (id != 61121100) { // 藍焰恐懼
                    switch (id) {
                        case 41121002: // 一閃
                            return 41120050; // 一閃-次數強化
                        case 41121018: // 瞬殺斬
                        case 41121021: // 瞬殺斬
                            return 41120048; // 瞬殺斬-次數強化
                        case 42121000: // 破邪連擊符
                            return 42120045; // 破邪連擊符-次數強化
                        case 51121007: // 靈魂突擊
                            return 51120051; // 靈魂突擊-獎勵加成
                        case 51121008: // 聖光爆發
                            return 51120048; // 聖光爆發-攻擊加成
                    }
                    return 0;
                }
                return 61120045; // 藍焰恐懼-加碼攻擊
            }
            if (id == 41121001) { // 神速無雙
                return 41120044; // 神速無雙-次數強化
            } else if (id > 21121013) { // 終極之矛
                switch (id) {
                    // 龍神之怒
                    case 22181002:
                        // ".img/SlideMenu/0/Recommend";
                        return 0;
                    // 鬼斬
                    case 25121005:
                        // 鬼斬-次數強化
                        return 25120148;
                    // 惡魔佈雷斯
                    case 31111005:
                        // 惡魔氣息-攻擊加成
                        return 31120044;
                    // 惡魔衝擊
                    case 31121001:
                        // 惡魔衝擊-攻擊加成
                        return 31120050;
                    case 32111003:
                        return 0;
                    // 巨型火炮：IRON-B
                    case 35121016:
                        // 巨型火炮：IRON-B-追加攻擊
                        return 35120051;
                    default:
                        break;
                }
            } else {
                if (id == 21121013) { // 終極之矛
                    // goto LABEL_115;
                }
                if (id == 13121000 + 2) { // 破風之箭
                    return 13120048; // 破風之箭-次數強化
                }
                if (id - (13121000 + 2) == 1000000) { // 四倍緩慢
                    return 14120045; // 五倍緩慢-爆擊率
                }
                if (id - (13121000 + 2) == 1990020 || id - (13121000 + 2) == 1999001) { // 疾風 || 颱風
                    return 15120045; // 疾風-次數強化
                }
                if (id - (13121000 + 2) == 2000000) { // 霹靂
                    return 15120048; // 霹靂-次數強化
                }
                if (id - (13121000 + 2) - 2000000 == 5999002 + 1) { // 終極之矛
                    // LABEL_115:
                    return 21120047; // 終極之矛-加碼攻擊
                } else if (id - (13121000 + 2) - 2000000 - (5999002 + 1) == 1) { // 極冰暴風
                    return 21120049; // 極冰暴風-加碼攻擊
                }
            }
        } else {
            if (id == (12120009 + 2)) { // 極致熾烈
                return 12120046; // 極致熾烈-追加反擊
            }
            if (id <= 5121017) { // 爆烈衝擊波
                if (id >= 5121016) { // 蓄能衝擊波
                    return 5120051; // 蓄能衝擊波-攻擊加成
                }
                if (id <= 3121015) { // 驟雨狂矢
                    switch (id) {
                        case 3121015: // 驟雨狂矢
                            return 3120048; // 驟雨狂矢-攻擊加成
                        case 1120017: // 狂暴攻擊
                        case 1121008: // 狂暴攻擊
                            return 1120051; // 狂暴攻擊-攻擊加成
                        case 1221009: // 騎士衝擊波
                            return 1220048; // 騎士衝擊波-攻擊加成
                        case 1221011: // 鬼神之擊
                            return 1220050; // 鬼神之擊-攻擊加成
                        case 2121003: // 地獄爆發
                            return 2120049; // 地獄爆發-攻擊加成
                        case 2121006: // 梅杜莎之眼
                            return 2120048; // 梅杜莎之眼-次數強化
                        case 2221006: // 閃電連擊
                            return 2220048; // 閃電連擊-攻擊加成
                    }
                    return 0;
                }
                if (id == 3121020) { // 暴風神射
                    return 3120051; // 暴風神射-多重射擊
                }
                if (id == 3221017) { // 光速神弩
                    return 3220048; // 光速神弩-攻擊加成
                }
                if (id == 4221007) { // 瞬步連擊
                    return 4220048; // 瞬步連擊-攻擊加成
                }
                if (id == 4331000) { // 血雨暴風狂斬
                    return 4340045; // 血雨暴風狂斬-攻擊加成
                }
                if (id == 4341009) { // 幻影箭
                    return 4340048; // 幻影箭-攻擊加成
                }
                if (id != 5121007) { // 閃‧連殺
                    return 0;
                }
                return 5120048; // 閃．連殺-攻擊加成
            }
            if (id > 5721064) { // 穿心掌打
                if (id == (11121101 + 2) || id - (11121101 + 2) == 100) { // 新月分裂 || 太陽穿刺
                    return 11120048; // 分裂與穿刺-次數強化
                } else if (id - (11121101 + 2) == 878923 // 元素火焰
                        || id - (11121101 + 2) == 978925 || id - (11121101 + 2) == 988925
                        || id - (11121101 + 2) == 998907) { // 元素火焰 IV
                    return 12120045; // 元素火焰-速發反擊
                }
            } else {
                if (id == 5721064) { // 穿心掌打
                    return 5720048; // 穿心掌打-次數強化
                }
                if (id == 5121020) { // 閃‧瞬連殺
                    return 5120048; // 閃．連殺-攻擊加成
                }
                if (id - 5121020 == 99996) { // 爆頭射擊
                    return 5220047; // 爆頭射擊-攻擊加成
                } else {
                    if (id - 5121020 == 198991) {
                        // goto LABEL_116;
                    }
                    if (id - 5121020 == 199980) {// 加農砲火箭
                        return 5320048; // 加農砲火箭-攻擊加成
                    }
                    if (id - 5121020 == 199984) { // 雙胞胎猴子
                        // LABEL_116:
                        return 5320043; // 雙胞胎猴子-傷害分裂
                    } else if (id - 5121020 == 600041) { // 龍襲亂舞
                        return 5720045; // 龍襲亂舞-次數強化
                    }
                }
            }
        }
        return 0;
    }

    private static List<Integer> SoulSkills;

    public static List<Integer> getSoulSkills() {
        if (SoulSkills != null) {
            return SoulSkills;
        }
        SoulSkills = new LinkedList<>();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (ItemInformation itInfo : ii.getAllItems()) {
            if (ItemConstants.類型.靈魂寶珠(itInfo.itemId)) {
                SoulSkills.add(ii.getSoulSkill(itInfo.itemId));
            }
        }
        return SoulSkills;
    }

    public static int SkillIncreaseMobCount(int sk) {
        int inc = 0;
        switch (sk) {
            case 112001008:// 鮮魚龍捲風
            case 112101009:// 電光石火
            case 112111004:// 隊伍轟炸
            case 61111218:// 龍劍風
            case 61111100:// 龍劍風
            case 51121008:// 聖光爆發
            case 42121000:// 破邪連擊符
            case 41121021:// 瞬殺斬
            case 41121018:// 瞬殺斬
            case 41121009:// 鷹爪閃
            case 36121012:// 偽裝掃蕩：轟炸
            case 36121011:// 偽裝掃蕩：砲擊
            case 36121000:// 疾風劍舞
            case 35121015:// 巨型火炮：SPLASH-F
            case 33121002:// 音爆
            case 32121003:// 颶風
            case 31201001:// 蝙蝠群
            case 27121303:// 絕對擊殺
            case 27121202:// 暗黑烈焰
            case 24121000:// 連犽突進
            case 24121005:// 卡牌風暴
            case 15121002:// 霹靂
            case 13121002:// 破風之箭
            case 12120011:// 極致熾烈
            case 11121203:// 太陽穿刺
            case 11121103:// 新月分裂
            case 5721007:// 俠客突襲
            case 5321000:// 加農砲火箭
            case 5121016:// 蓄能衝擊波
            case 4341004:// 短刀護佑
            case 4331000:// 血雨暴風狂斬
            case 4221007:// 瞬步連擊
            case 4121017:// 挑釁契約
            case 3221017:// 光速神孥
            case 3121015:// 驟雨狂矢
            case 2221012:// 冰鋒刃
            case 2221006:// 閃電連擊
            case 2211007:// 瞬間移動精通
            case 1211008:// 雷鳴之劍
            case 1121008:// 狂暴攻擊
                inc = 2;
                break;
            case 1221004:// 聖靈之劍
            case 1201012:// 寒冰之劍
            case 1201011:// 烈焰之劍
                inc = 3;
                break;
            case 4341052:
                inc = 4;
                break;
        }
        return inc;
    }

    public static boolean is_super_nova_skill(int nSkillID) {
        return nSkillID == 4221052 || nSkillID == 65121052;
    }

    public static boolean is_rw_multi_charge_skill(int nSkillID) {
        boolean v1 = true;
        if (nSkillID > 37110001) {
            if (nSkillID == 37110004 || nSkillID == 37111000) {
                return true;
            }
            v1 = nSkillID == 37111003;
        } else {
            if (nSkillID == 37110001) {
                return true;
            }
            if (nSkillID > 37100002) {
                v1 = nSkillID == 37101001;
            } else {
                if (nSkillID == 37100002 || nSkillID == 37000010) {
                    return true;
                }
                v1 = nSkillID == 37001001;
            }
        }
        return v1;
    }

    public static boolean is_unregisterd_skill(int nSkillID) {
        boolean result;
        int v1;
        v1 = nSkillID / 10000;
        if (nSkillID / 10000 == 8000) {
            v1 = nSkillID / 100;
        }
        if (nSkillID > 0 && v1 == 9500) {
            result = false;
        } else {
            result = nSkillID / 10000000 == 9;
        }
        return result;
    }

    public static boolean is_match_skill(boolean bIsBeta, int nSkillID) {
        int v2 = nSkillID / 10000;
        int v3 = nSkillID / 10000;
        boolean result;
        if (nSkillID / 10000 == 8000) {
            v3 = nSkillID / 100;
        }
        if (v2 == 8000) {
            v2 = nSkillID / 100;
        }
        if (MapleJob.isBeginner(v3) || (nSkillID > 0 && v2 == 9500)) {
            result = true;
        } else {
            result = is_zero_alpha_skill(nSkillID) && !bIsBeta || is_zero_beta_skill(nSkillID) && bIsBeta;
        }
        return result;
    }

    public static boolean is_zero_alpha_skill(int nSkillID) {
        boolean result;
        int v2 = nSkillID / 10000;
        if (nSkillID / 10000 == 8000) {
            v2 = nSkillID / 100;
        }
        if (is_zero_skill(nSkillID) || MapleJob.isBeginner(v2)) {
            result = false;
        } else {
            result = nSkillID % 1000 / 100 == 2;
        }
        return result;
    }

    public static boolean is_zero_beta_skill(int nSkillID) {
        boolean result;
        int v2 = nSkillID / 10000;
        if (nSkillID / 10000 == 8000) {
            v2 = nSkillID / 100;
        }
        if (is_zero_skill(nSkillID) || MapleJob.isBeginner(v2)) {
            result = false;
        } else {
            result = nSkillID % 1000 / 100 == 1;
        }
        return result;
    }

    public static boolean is_zero_skill(int nSkillID) {
        int v1 = nSkillID / 10000;
        boolean v2;
        if (nSkillID / 10000 == 8000) {
            v1 = nSkillID / 100;
        }
        if (v1 == 10000 || v1 == 10100 || v1 == 10110 || v1 == 10111 || v1 == 10112) {
            v2 = true;
        } else {
            v2 = false;
        }
        return v2;
    }

    public static int getAvailableVCoreSpace(int level) {
        if (level >= 200) {
            return 4 + (level - 250) / 5;
        } else {
            return 4;
        }
    }
}
