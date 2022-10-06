package client;

import client.character.stat.MapleBuffStat;
import client.skill.SkillEntry;
import client.skill.Skill;
import client.skill.SkillFactory;
import client.MapleTrait.MapleTraitType;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import constants.EventConstants;
import constants.GameConstants;
import constants.ItemConstants;
import handling.world.World;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildSkill;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.StructItemOption;
import server.StructSetItem;
import server.StructSetItem.SetItem;
import server.life.Element;
import tools.Pair;
import tools.Triple;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.JobPacket;

public class PlayerStats implements Serializable {

    private static final long serialVersionUID = -679541993413738569L;
    private final Map<Integer, Integer> setHandling = new HashMap<>(), skillsIncrement = new HashMap<>(),
            damageIncrease = new HashMap<>();
    private final EnumMap<Element, Integer> elemBoosts = new EnumMap<>(Element.class);
    private final List<Equip> durabilityHandling = new ArrayList<>(), equipLevelHandling = new ArrayList<>();
    private transient float shouldHealHP, shouldHealMP;
    private transient short 暴擊最小傷害, 暴擊最大傷害, 暴擊概率;
    private transient byte passive_mastery;
    private transient int 局部力量, 局部敏捷, 局部智力, 局部幸運, 局部最大HP, 局部最大MP, 魔法攻擊力, 物理攻擊力, hands, 星光能量;
    private transient int 增加最大HP, 增加最大MP, 百分比加成之後增加最大HP, 百分比加成之後增加最大MP;
    private transient int IndieStrFX, IndieDexFX, IndieLukFX, IndieIntFX;
    public transient int incMaxDF;
    private transient float localmaxbasedamage, localmaxbasepvpdamage, localmaxbasepvpdamageL;
    public transient boolean equippedWelcomeBackRing, hasClone, Berserk;
    public transient double expBuff, indieExp, dropBuff, mesoBuff, mesoGuard, mesoGuardMeso, expMod, dropMod,
            pickupRange, dam_r, bossdam_r;
    public transient int recoverHP, recoverMP, mpconReduce, mpconPercent, incMesoProp, reduceCooltime, coolTimeR,
            suddenDeathR, expLossReduceR, DAMreflect, DAMreflect_rate, ignoreDAMr, ignoreDAMr_rate, ignoreDAM,
            ignoreDAM_rate, hpRecover, hpRecoverProp, hpRecoverPercent, mpRecover, mpRecoverProp, 道具恢復效能提升,
            BUFF道具持續時間提升, 技能恢復效能提升, BUFF技能持續時間提升, incAllskill, combatOrders, ignoreTargetDEF, defRange, 召喚獸持續時間提升, 閃避率,
            迴避值, 移動速度, 最大移動速度, 跳躍力, harvestingTool, levelBonus, 異常抗性, 屬性抗性, pickRate, decreaseDebuff, equippedFairy,
            equippedSummon, pvpDamage, hpRecoverTime, mpRecoverTime, dot, dotTime, questBonus, pvpRank, pvpExp, 防御力,
            trueMastery, damX, reduceDamageRate, ReChargeChance, DMGreduceR;
    public transient int def, element_ice, element_fire, element_light, element_psn, raidenCount, raidenPorp, 格擋率;
    public int hp, maxhp, mp, maxmp, 力量, 敏捷, 幸運, 智力;
    private transient int percent_hp, percent_mp, percent_str, percent_dex, percent_int, percent_luk, percent_acc,
            percent_atk, percent_matk, percent_wdef, percent_mdef, add_hp, add_mp, add_str, add_dex, add_int, add_luk,
            add_acc, add_atk, add_matk, add_wdef, add_mdef;
    private final Map<Integer, Integer> add_skill_duration = new HashMap<>();
    private final Map<Integer, Integer> add_skill_attackCount = new HashMap<>();
    private final Map<Integer, Integer> add_skill_targetPlus = new HashMap<>();
    private transient int passivePlus;
    private final Map<Integer, Integer> add_skill_bossDamageRate = new HashMap<>();
    private final Map<Integer, Integer> add_skill_dotTime = new HashMap<>();
    private final Map<Integer, Integer> add_skill_prop = new HashMap<>();
    private final Map<Integer, Integer> add_skill_coolTimeR = new HashMap<>();
    private final Map<Integer, Integer> add_skill_ignoreMobpdpR = new HashMap<>();

    public void recalcLocalStats(MapleCharacter chra) {
        recalcLocalStats(false, chra);
    }

    private void resetLocalStats(final int job) {
        防御力 = 0;
        damX = 0;// 攻擊
        增加最大HP = 0;
        百分比加成之後增加最大HP = 0;
        增加最大MP = 0;
        百分比加成之後增加最大MP = 0;
        局部力量 = getStr();
        局部敏捷 = getDex();
        局部智力 = getInt();
        局部幸運 = getLuk();
        星光能量 = 0;
        IndieDexFX = 0;
        IndieIntFX = 0;
        IndieStrFX = 0;
        IndieLukFX = 0;
        移動速度 = 100;
        最大移動速度 = 140;
        跳躍力 = 100;
        格擋率 = 0;
        pickupRange = 0.0;// 撿取範圍
        decreaseDebuff = 0;
        異常抗性 = 0;
        屬性抗性 = 0;
        dot = 0;// 持續傷害
        questBonus = 1;
        dotTime = 0;// 持續傷害時間
        trueMastery = 0;// 熟練度
        percent_wdef = 0;// 增加百分比物理防禦
        percent_mdef = 0;// 增加百分比魔法防禦
        percent_hp = 0;// 增加百分比HP
        percent_mp = 0;// 增加百分比MP
        percent_str = 0;// 增加百分比力量
        percent_dex = 0;// 增加百分比敏捷
        percent_int = 0;// 增加百分比智力
        percent_luk = 0;// 增加百分比運氣
        percent_acc = 0;// 增加百分比命中
        percent_atk = 0;// 增加百分比攻擊
        percent_matk = 0;// 增加百分比魔攻
        add_wdef = 0;
        add_mdef = 0;
        add_hp = 0;
        add_mp = 0;
        add_str = 0;
        add_dex = 0;
        add_int = 0;
        add_luk = 0;
        add_acc = 0;
        add_atk = 0;
        add_matk = 0;
        暴擊概率 = 5;
        暴擊最小傷害 = 20;
        暴擊最大傷害 = 50;
        魔法攻擊力 = 0;
        物理攻擊力 = 0;
        閃避率 = 0;// 閃避百分比
        迴避值 = 0;
        pvpDamage = 0;
        mesoGuard = 50.0;// 楓幣護盾
        mesoGuardMeso = 0.0;
        dam_r = 100.0;// 增加百分比傷害 - 總傷
        bossdam_r = 100.0;// 增加BOSS百分比傷害 - BOSS傷
        expBuff = 100.0;// 經驗倍率加持
        indieExp = 100.0;// 加持獎勵經驗
        dropBuff = 100.0;// 掉寶BUFF
        mesoBuff = 100.0;// 楓幣BUFF
        recoverHP = 0;// 恢復HP量
        recoverMP = 0;// 恢復MP量
        mpconReduce = 0;
        mpconPercent = 100;// 恢復MP百分比量
        incMesoProp = 0;
        reduceCooltime = 0;// 技能冷卻
        coolTimeR = 0;// 技能冷卻
        suddenDeathR = 0;
        expLossReduceR = 0;
        DAMreflect = 0;
        DAMreflect_rate = 0;
        ignoreDAMr = 0;// 無視傷害百分比
        ignoreDAMr_rate = 0;// 無視傷害百分比概率
        ignoreDAM = 0;// 無視傷害
        ignoreDAM_rate = 0;// 無視傷害幾率
        ignoreTargetDEF = 0;// 無視目標防御力
        hpRecover = 0;// HP恢復
        hpRecoverProp = 0;// HP恢復概率
        hpRecoverPercent = 0;// HP恢復百分比
        mpRecover = 0;// MP恢復
        mpRecoverProp = 0;// MP恢復概率
        pickRate = 0;
        equippedWelcomeBackRing = false;
        equippedFairy = 0;
        equippedSummon = 0;
        hasClone = false;
        Berserk = false;
        道具恢復效能提升 = 0;
        BUFF道具持續時間提升 = 0;
        技能恢復效能提升 = 0;
        BUFF技能持續時間提升 = 0;
        召喚獸持續時間提升 = 0;
        dropMod = 1.0;
        expMod = 1.0;
        levelBonus = 0;
        incMaxDF = 0;
        incAllskill = 0;
        combatOrders = 0;
        defRange = isRangedJob(job) ? 200 : 0;
        durabilityHandling.clear();
        equipLevelHandling.clear();
        skillsIncrement.clear();
        damageIncrease.clear();
        setHandling.clear();
        add_skill_duration.clear();
        add_skill_attackCount.clear();
        add_skill_targetPlus.clear();
        passivePlus = 0;
        add_skill_dotTime.clear();
        add_skill_prop.clear();
        add_skill_coolTimeR.clear();
        add_skill_ignoreMobpdpR.clear();
        harvestingTool = 0;
        element_fire = 100;
        element_ice = 100;
        element_light = 100;
        element_psn = 100;
        def = 100;
        raidenCount = 0;
        raidenPorp = 0;
        reduceDamageRate = 0;
        ReChargeChance = 0; // 天使充電機率
        DMGreduceR = 0; // 傷害減少百分比
    }

    /**
     * 計算各類屬性狀態
     *
     * @param first_login 是否第一次登入
     * @param chra 角色實例
     */
    public void recalcLocalStats(boolean first_login, MapleCharacter chra) {
        if (chra.isClone()) {
            return; // clones share PlayerStats objects and do not need to be recalculated
        }
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int oldmaxhp = 局部最大HP;
        int localmaxhp_ = getMaxHp();
        int localmaxmp_ = getMaxMp();
        resetLocalStats(chra.getJob());
        for (MapleTraitType t : MapleTraitType.values()) {
            chra.getTrait(t).clearLocalExp();
        }
        StructItemOption soc;
        final Map<Skill, SkillEntry> sData = new HashMap<>();
        // 裝備屬性處理
        final Iterator<Item> itera = chra.getInventory(MapleInventoryType.EQUIPPED).newList().iterator();
        while (itera.hasNext()) {
            final Equip equip = (Equip) itera.next();
            if (equip.getPosition() == -11) {
                if (ItemConstants.類型.魔法武器(equip.getItemId())) {
                    final Map<String, Integer> eqstat = MapleItemInformationProvider.getInstance()
                            .getEquipStats(equip.getItemId());
                    if (eqstat != null) { // slow, poison, darkness, seal, freeze
                        if (eqstat.containsKey("incRMAF")) {
                            element_fire = eqstat.get("incRMAF");
                        }
                        if (eqstat.containsKey("incRMAI")) {
                            element_ice = eqstat.get("incRMAI");
                        }
                        if (eqstat.containsKey("incRMAL")) {
                            element_light = eqstat.get("incRMAL");
                        }
                        if (eqstat.containsKey("incRMAS")) {
                            element_psn = eqstat.get("incRMAS");
                        }
                        if (eqstat.containsKey("elemDefault")) {
                            def = eqstat.get("elemDefault");
                        }
                    }
                }
            }
            if ((equip.getItemId() / 10000 == 166 && equip.getAndroid() != null || equip.getItemId() / 10000 == 167)
                    && chra.getAndroid() == null) {
                final Equip android = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -32);
                final Equip heart = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -33);
                if (android != null && heart != null) {
                    chra.setAndroid(equip.getAndroid());
                }
            }
            // if (equip.getItemId() / 1000 == 1099) {
            // equippedForce += equip.getMp();
            // }
            chra.getTrait(MapleTraitType.craft).addLocalExp(equip.getHands(0xFFFF));
            localmaxhp_ += MapleJob.is惡魔復仇者(chra.getJob()) ? equip.getHp(0xFFFF) / 2 : equip.getHp(0xFFFF);
            localmaxmp_ += equip.getMp(0xFFFF);
            局部敏捷 += equip.getDex(0xFFFF);
            局部智力 += equip.getInt(0xFFFF);
            局部力量 += equip.getStr(0xFFFF);
            局部幸運 += equip.getLuk(0xFFFF);
            物理攻擊力 += equip.getWatk(0xFFFF);
            魔法攻擊力 += equip.getMatk(0xFFFF);
            防御力 += equip.getWdef(0xFFFF);
            移動速度 += equip.getSpeed(0xFFFF);
            跳躍力 += equip.getJump(0xFFFF);
            pvpDamage += equip.getPVPDamage();
            bossdam_r += equip.getBossDamage(0xFFFF);
            ignoreTargetDEF += equip.getIgnorePDR(0xFFFF);
            dam_r *= ((double) equip.getTotalDamage(0xFFFF) + 100.0) / 100.0;
            percent_str += equip.getAllStat(0xFFFF);
            percent_dex += equip.getAllStat(0xFFFF);
            percent_int += equip.getAllStat(0xFFFF);
            percent_luk += equip.getAllStat(0xFFFF);
            星光能量 += equip.getEnhance();
            switch (equip.getItemId()) {
                case 1112127: // Welcome Back
                    equippedWelcomeBackRing = true;
                    break;
                case 1112585: // 天使祝福
                    equippedSummon = 1085;
                    break;
                case 1112594: // 雪花天使祝福
                    equippedSummon = 1090;
                    break;
                case 1112586: // 黑天使祝福
                    equippedSummon = 1087;
                    break;
                case 1112663: // 白色精靈祝福
                    equippedSummon = 1179;
                    break;
                case 1113020: // 戰神祝福
                    equippedSummon = 80001262;
                    break;
                case 1112735: // 白天使祝福
                    equippedSummon = 80001154;
                    break;
                case 1114200: // 瑪瑙戒指 "遭遇"
                    equippedSummon = 80001518;
                    break;
                case 1114201: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001519;
                    break;
                case 1114202: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001520;
                    break;
                case 1114203: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001521;
                    break;
                case 1114204: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001522;
                    break;
                case 1114205: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001523;
                    break;
                case 1114206: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001524;
                    break;
                case 1114207: // 瑪瑙戒指 "成長"
                    equippedSummon = 80001525;
                    break;
                case 1114208: // 瑪瑙戒指 "成長"
                    equippedSummon = 80001526;
                    break;
                case 1114209: // 瑪瑙戒指 "成長"
                    equippedSummon = 80001527;
                    break;
                case 1114210: // 瑪瑙戒指 "成長"
                    equippedSummon = 80001528;
                    break;
                case 1114211: // 瑪瑙戒指 "成長"
                    equippedSummon = 80001529;
                    break;
                case 1114212: // 瑪瑙戒指 "成長"
                    equippedSummon = 80001530;
                    break;
                case 1114219: // 瑪瑙戒指 "遭遇"
                    equippedSummon = 80001715;
                    break;
                case 1114220: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001716;
                    break;
                case 1114221: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001717;
                    break;
                case 1114222: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001718;
                    break;
                case 1114223: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001719;
                    break;
                case 1114224: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001720;
                    break;
                case 1114225: // 瑪瑙戒指 "共用"
                    equippedSummon = 80001721;
                    break;
                case 1114226: // 瑪瑙戒指 "完成"
                    equippedSummon = 80001722;
                    break;
                case 1114227: // 瑪瑙戒指 "完成"
                    equippedSummon = 80001723;
                    break;
                case 1114228: // 瑪瑙戒指 "完成"
                    equippedSummon = 80001724;
                    break;
                case 1114229: // 瑪瑙戒指 "完成"
                    equippedSummon = 80001725;
                    break;
                case 1114230: // 瑪瑙戒指 "完成"
                    equippedSummon = 80001726;
                    break;
                case 1114231: // 瑪瑙戒指 "完成"
                    equippedSummon = 80001727;
                    break;
                default:
                    equippedFairy += GameConstants.Equipment_Bonus_EXP(equip.getItemId());
                    break;
            }

            if (equip.getItemId() / 1000 == 1099) {
                this.incMaxDF += equip.getMp(0xFFFF);
            }
            percent_hp += ii.getItemIncMHPr(equip.getItemId());
            percent_mp += ii.getItemIncMMPr(equip.getItemId());
            bossdam_r += equip.getBossDamage(0xFFFF);
            ignoreTargetDEF += equip.getIgnorePDR(0xFFFF);
            dam_r += equip.getTotalDamage(0xFFFF);

            final Integer set = ii.getSetItemID(equip.getItemId());
            if (set != null && set > 0) {
                int value = 1;
                if (setHandling.containsKey(set)) {
                    value += setHandling.get(set);
                }
                setHandling.put(set, value); // id of Set, number of items to go with the set
            }
            if (ii.getEquipSkills(equip.getItemId()) != null) {
                Map<String, Integer> equipStats;
                for (final int zzz : ii.getEquipSkills(equip.getItemId())) {
                    final Skill skil = SkillFactory.getSkill(zzz);
                    equipStats = ii.getEquipStats(equip.getItemId());
                    if (skil != null && ((equip.getIncSkill() > 0 && skil.canBeLearnedBy(chra.getJob()))
                            || (equipStats != null && equipStats.get("fixLevel") > 0))) { // dont go over masterlevel :D
                        int value = 0;
                        if (equipStats != null && equipStats.get("fixLevel") > 0) {
                            value += equipStats.get("fixLevel");
                        } else {
                            value = 1;
                            if (skillsIncrement.get(skil.getId()) != null) {
                                value += skillsIncrement.get(skil.getId());
                            }
                        }
                        skillsIncrement.put(skil.getId(), value);
                    }
                }
            }
            final Pair<Integer, Integer> ix = handleEquipAdditions(ii, chra, first_login, sData, equip.getItemId());
            if (ix != null) {
                localmaxhp_ += ix.getLeft();
                localmaxmp_ += ix.getRight();
            }
            // 潛能屬性處理
            if (equip.getState(false) >= 17 || equip.getState(true) >= 17) {
                final int[] potentials = {equip.getPotential(1, false), equip.getPotential(2, false),
                    equip.getPotential(3, false), equip.getPotential(1, true), equip.getPotential(2, true),
                    equip.getPotential(3, true),};
                for (final int i : potentials) {
                    if (i > 0) {
                        int potLevel = ii.getReqLevel(equip.getItemId()) / 10;
                        potLevel = potLevel >= 20 ? 19 : potLevel;
                        soc = ii.getPotentialInfo(i).get(potLevel);
                        if (soc != null) {
                            localmaxhp_ += soc.get("incMHP");
                            localmaxmp_ += soc.get("incMMP");
                            handleItemOption(soc, chra, first_login, sData);
                        }
                    }
                }
            }
            // 星岩屬性處理
            if (equip.getSocketState() > 15) {
                final int[] sockets = {equip.getSocket(1), equip.getSocket(2), equip.getSocket(3)};
                for (final int i : sockets) {
                    if (i > 0) {
                        soc = ii.getSocketInfo(i);
                        if (soc != null) {
                            localmaxhp_ += soc.get("incMHP");
                            localmaxmp_ += soc.get("incMMP");
                            handleItemOption(soc, chra, first_login, sData);
                        }
                    }
                }
            }
            // 耐久度處理
            if (equip.getDurability() > 0) {
                durabilityHandling.add(equip);
            }
            //
            if (GameConstants.getMaxLevel(equip.getItemId()) > 0
                    && (GameConstants.getStatFromWeapon(equip.getItemId()) == null
                    ? (equip.getEquipLevel() <= GameConstants.getMaxLevel(equip.getItemId()))
                    : (equip.getEquipLevel() < GameConstants.getMaxLevel(equip.getItemId())))) {
                equipLevelHandling.add(equip);
            }
        }
        // 套裝屬性處理
        final Iterator<Entry<Integer, Integer>> iter = setHandling.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<Integer, Integer> entry = iter.next();
            final StructSetItem set = ii.getSetItem(entry.getKey());
            if (set != null) {
                final Map<Integer, SetItem> itemz = set.getItems();
                for (Entry<Integer, SetItem> ent : itemz.entrySet()) {
                    if (ent.getKey() <= entry.getValue()) {
                        SetItem se = ent.getValue();
                        局部力量 += se.incSTR + se.incAllStat;
                        局部敏捷 += se.incDEX + se.incAllStat;
                        局部智力 += se.incINT + se.incAllStat;
                        局部幸運 += se.incLUK + se.incAllStat;
                        物理攻擊力 += se.incPAD;
                        魔法攻擊力 += se.incMAD;
                        移動速度 += se.incSpeed;
                        localmaxhp_ += MapleJob.is惡魔復仇者(chra.getJob()) ? se.incMHP / 2 : se.incMHP;
                        localmaxmp_ += se.incMMP;
                        percent_hp += se.incMHPr;
                        percent_mp += se.incMMPr;
                        防御力 += se.incPDD;
                        if (se.option1 > 0 && se.option1Level > 0) {
                            soc = ii.getPotentialInfo(se.option1).get(se.option1Level);
                            if (soc != null) {
                                localmaxhp_ += soc.get("incMHP");
                                localmaxmp_ += soc.get("incMMP");
                                handleItemOption(soc, chra, first_login, sData);
                            }
                        }
                        if (se.option2 > 0 && se.option2Level > 0) {
                            soc = ii.getPotentialInfo(se.option2).get(se.option2Level);
                            if (soc != null) {
                                localmaxhp_ += soc.get("incMHP");
                                localmaxmp_ += soc.get("incMMP");
                                handleItemOption(soc, chra, first_login, sData);
                            }
                        }
                    }
                }
            }
        }
        handleProfessionTool(chra);
        double extraExpRate = 1.0;
        for (Item item : chra.getInventory(MapleInventoryType.CASH).newList()) {
            if (item.getItemId() / 10000 == 521) {
                double rate = ii.getExpCardRate(item.getItemId());
                if (item.getItemId() != 5210009 && rate > 1.0) {
                    if (!ii.isExpOrDropCardTime(item.getItemId())
                            || chra.getLevel() > ii.getExpCardMaxLevel(item.getItemId())
                            || (item.getExpiration() == System.currentTimeMillis() && !chra.isIntern())) {
                        if (item.getExpiration() == -1L && !chra.isIntern()) {
                            chra.dropMessage(5, ii.getName(item.getItemId()) + "屬性錯誤，經驗值加成無效。");
                        }
                        continue;
                    }
                    switch (item.getItemId()) {
                        case 5211000:
                        case 5211001:
                        case 5211002:
                            extraExpRate *= rate;
                            break;
                        default:
                            if (expMod < rate) {
                                expMod = rate;
                            }
                    }
                }
            } else if (dropMod == 1.0 && item.getItemId() / 10000 == 536) {
                if (item.getItemId() >= 5360000 && item.getItemId() < 5360100) {
                    if (!ii.isExpOrDropCardTime(item.getItemId())
                            || (item.getExpiration() > System.currentTimeMillis() && !chra.isIntern())) {
                        if (item.getExpiration() == -1L && !chra.isIntern()) {
                            chra.dropMessage(5, ii.getName(item.getItemId()) + "屬性錯誤，掉寶機率加成無效。");
                        }
                        continue;
                    }
                    dropMod = 2.0;
                }
            } else if (item.getItemId() == 5710000) {
                questBonus = 2;
            } else if (item.getItemId() == 5590000) {
                levelBonus += 5;
            }
        }
        expMod = Math.max(extraExpRate, expMod);
        if (expMod > 0 && EventConstants.DoubleExpTime) {
            expMod *= 2.0;
        }
        if (dropMod > 0 && EventConstants.DoubleDropTime) {
            dropMod *= 2.0;
        }
        for (Item item : chra.getInventory(MapleInventoryType.ETC).list()) {
            switch (item.getItemId()) {
                case 4030003:
                    break;
                case 4030004:
                    break;
                case 4031864:
                    break;
            }
        }

        // add to localmaxhp_ if percentage plays a role in it, else add_hp
        handleBuffStats(chra);
        Integer buff = chra.getBuffedValue(MapleBuffStat.EMHP);
        if (buff != null) {
            localmaxhp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.EMMP);
        if (buff != null) {
            localmaxmp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieMHP);
        if (buff != null) {
            localmaxhp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieMMP);
        if (buff != null) {
            localmaxmp_ += buff;
        }
        handlePassiveSkills(chra);// 被動技能
        handleHyperCoolDownReduce(chra); // 處理超技冷卻時間減少
        if (chra.getGuildId() > 0) {
            final MapleGuild g = World.Guild.getGuild(chra.getGuildId());
            if (g != null && g.getSkills().size() > 0) {
                final long now = System.currentTimeMillis();
                for (MapleGuildSkill gs : g.getSkills()) {
                    if (gs.timestamp > now && gs.activator.length() > 0) {
                        final MapleStatEffect e = SkillFactory.getSkill(gs.skillID).getEffect(gs.level);
                        暴擊概率 += e.getCr();
                        物理攻擊力 += e.getAttackX();
                        魔法攻擊力 += e.getMagicX();
                        expBuff *= e.getEXPRate() / 100.0;
                        閃避率 += e.getER();
                        percent_wdef += e.getWDEFRate();
                        percent_mdef += e.getMDEFRate();
                    }
                }
            }
        }
        for (Pair<Integer, Integer> ix : chra.getCharacterCard().getCardEffects()) {
            // System.out.println("[角色卡] 等級: " + ix.getRight() + " 技能: " + ix.getLeft() + "
            // - " + SkillFactory.getSkillName(ix.getLeft()));
            final MapleStatEffect e = SkillFactory.getSkill(ix.getLeft()).getEffect(ix.getRight());
            percent_wdef += e.getWDEFRate();
            damX += e.getLevelToWatkX() * chra.getLevel() * 0.5D;
            damX += e.getLevelToMatkX() * chra.getLevel() * 0.5D;
            物理攻擊力 += (e.getLevelToWatk() * chra.getLevel());
            percent_hp += e.getPercentHP();
            percent_mp += e.getPercentMP();
            魔法攻擊力 += (e.getLevelToMatk() * chra.getLevel());
            道具恢復效能提升 += e.getMPConsumeEff();
            percent_acc += e.getPercentAcc();
            暴擊概率 += e.getCr();
            跳躍力 += e.getPassiveJump();
            移動速度 += e.getPassiveSpeed();
            閃避率 += e.getPercentAvoid();
            damX += (e.getLevelToDamageX() * chra.getLevel());
            召喚獸持續時間提升 += e.getSummonTimeInc();
            expLossReduceR += e.getEXPLossRate();
            異常抗性 += e.getASRRate();
            // ignoreMobDamR
            suddenDeathR += e.getSuddenDeathR();
            BUFF技能持續時間提升 += e.getBuffTimeRate();
            // onHitHpRecoveryR
            // onHitMpRecoveryR
            coolTimeR += e.getCooltimeReduceR();
            incMesoProp += e.getMesoAcquisition();
            damX += Math.floor((e.getHpToDamageX() * oldmaxhp) / 100.0f);
            damX += Math.floor((e.getMpToDamageX() * oldmaxhp) / 100.0f);
            // finalAttackDamR
            暴擊最大傷害 += e.getCriticalMax();
            ignoreTargetDEF += e.getIgnoreMob();
            局部力量 += e.getStrX();
            局部敏捷 += e.getDexX();
            局部智力 += e.getIntX();
            局部幸運 += e.getLukX();
            IndieStrFX += e.getStrFX();
            IndieDexFX += e.getDexFX();
            IndieIntFX += e.getIntFX();
            IndieLukFX += e.getLukFX();
            localmaxhp_ += e.getMaxHpX();
            localmaxmp_ += e.getMaxMpX();
            物理攻擊力 += e.getAttackX();
            魔法攻擊力 += e.getMagicX();
            bossdam_r += e.getBossDamage();
        }

        // 角色內在能力
        for (int i = 0; i < 3; i++) {
            InnerSkillValueHolder innerSkill = chra.getInnerSkills()[i];
            if (innerSkill == null) {
                continue;
            }
            MapleStatEffect InnerEffect = SkillFactory.getSkill(innerSkill.getSkillId())
                    .getEffect(innerSkill.getSkillLevel());
            if (InnerEffect == null) {
                continue;
            }
            防御力 += InnerEffect.getWDEFX();
            percent_wdef += InnerEffect.getWDEFRate();
            percent_mdef += InnerEffect.getMDEFRate();
            percent_hp += InnerEffect.getPercentHP();
            percent_mp += InnerEffect.getPercentMP();
            閃避率 += InnerEffect.getPercentAvoid();
            暴擊概率 = (short) (this.暴擊概率 + InnerEffect.getCr());
            跳躍力 += InnerEffect.getPassiveJump();
            移動速度 += InnerEffect.getPassiveSpeed();
            IndieStrFX += InnerEffect.getStrFX();
            IndieDexFX += InnerEffect.getDexFX();
            IndieIntFX += InnerEffect.getIntFX();
            IndieLukFX += InnerEffect.getLukFX();
            localmaxhp_ += InnerEffect.getMaxHpX();
            localmaxmp_ += InnerEffect.getMaxMpX();
            this.物理攻擊力 += InnerEffect.getAttackX();
            this.魔法攻擊力 += InnerEffect.getMagicX();
            if (InnerEffect.getDexToStr() > 0) {
                IndieStrFX = (int) (IndieStrFX + Math.floor(getDex() * InnerEffect.getDexToStr() / 100.0F));
            }
            if (InnerEffect.getStrToDex() > 0) {
                IndieDexFX = (int) (IndieDexFX + Math.floor(getStr() * InnerEffect.getStrToDex() / 100.0F));
            }
            if (InnerEffect.getIntToLuk() > 0) {
                IndieLukFX = (int) (IndieLukFX + Math.floor(getInt() * InnerEffect.getIntToLuk() / 100.0F));
            }
            if (InnerEffect.getLukToDex() > 0) {
                IndieDexFX = (int) (IndieDexFX + Math.floor(getLuk() * InnerEffect.getLukToDex() / 100.0F));
            }
            if (InnerEffect.getLevelToWatk() > 0) {
                物理攻擊力 = (int) (this.物理攻擊力 + Math.floor(chra.getLevel() / InnerEffect.getLevelToWatk()));
            }
            if (InnerEffect.getLevelToMatk() > 0) {
                魔法攻擊力 = (int) (this.魔法攻擊力 + Math.floor(chra.getLevel() / InnerEffect.getLevelToMatk()));
            }
            bossdam_r += InnerEffect.getBossDamage();
            addTargetPlus(0, InnerEffect.getTargetPlus());
            passivePlus += InnerEffect.getPassivePlus();
        }

        局部力量 += Math.floor((局部力量 * percent_str) / 100.0f) + IndieStrFX;
        局部敏捷 += Math.floor((局部敏捷 * percent_dex) / 100.0f) + IndieDexFX;
        局部智力 += Math.floor((局部智力 * percent_int) / 100.0f) + IndieIntFX;
        局部幸運 += Math.floor((局部幸運 * percent_luk) / 100.0f) + IndieLukFX;
        物理攻擊力 += Math.floor((物理攻擊力 * percent_atk) / 100.0f);
        魔法攻擊力 += Math.floor((魔法攻擊力 * percent_matk) / 100.0f);
        局部智力 += Math.floor((局部智力 * percent_matk) / 100.0f);

        防御力 += Math.floor((局部力量 * 1.2) + ((局部敏捷 + 局部幸運) * 0.5) + (局部智力 * 0.4));
        防御力 += Math.min(30000, Math.floor((防御力 * percent_wdef) / 100.0f));

        hands = 局部敏捷 + 局部智力 + 局部幸運;
        calculateFame(chra);
        ignoreTargetDEF += chra.getTrait(MapleTraitType.charisma).getLevel() / 10;
        pvpDamage += chra.getTrait(MapleTraitType.charisma).getLevel() / 10;
        異常抗性 += chra.getTrait(MapleTraitType.will).getLevel() / 5;

        localmaxhp_ += 增加最大HP;
        if (MapleJob.is陰陽師(chra.getJob())) {
            localmaxhp_ += 增加最大MP;
        }
        localmaxhp_ += (chra.getTrait(MapleTraitType.will).getLevel() / 5) * 100;// 性向系統血量百分比後處理
        localmaxhp_ += Math.floor((percent_hp * localmaxhp_) / 100.0f);
        if (MapleJob.is陰陽師(chra.getJob())) {
            localmaxhp_ += Math.floor((percent_mp * localmaxhp_) / 100.0f);
        }
        localmaxhp_ += 百分比加成之後增加最大HP;

        局部最大HP = Math.min(500000, Math.abs(Math.max(-500000, localmaxhp_)));

        localmaxmp_ += 增加最大MP;
        localmaxmp_ += (chra.getTrait(MapleTraitType.sense).getLevel() / 5) * 100;// 性向系統魔量百分比後處理
        localmaxmp_ += Math.floor((percent_mp * localmaxmp_) / 100.0f);
        localmaxmp_ += 百分比加成之後增加最大MP;

        局部最大MP = Math.min(500000, Math.abs(Math.max(-500000, localmaxmp_)));
        閃避率 += (Math.round(迴避值 * 閃避率) / 100);
        if (chra.getEventInstance() != null && chra.getEventInstance().getName().startsWith("PVP")) { // hack
            MapleStatEffect eff;
            局部最大HP = Math.min(40000, 局部最大HP * 3); // approximate.
            局部最大MP = Math.min(20000, 局部最大MP * 2);
            // not sure on 20000 cap
            for (int i : pvpSkills) {
                Skill skil = SkillFactory.getSkill(i);
                if (skil != null && skil.canBeLearnedBy(chra.getJob())) {
                    sData.put(skil, new SkillEntry((byte) 1, (byte) 0, -1));
                    eff = skil.getEffect(1);
                    switch ((i / 1000000) % 10) {
                        case 1:
                            if (eff.getX() > 0) {
                                pvpDamage += (防御力 / eff.getX());
                            }
                            break;
                        case 3:
                            hpRecoverProp += eff.getProb();
                            hpRecover += eff.getX();
                            mpRecoverProp += eff.getProb();
                            mpRecover += eff.getX();
                            break;
                        case 5:
                            暴擊概率 += eff.getProb();
                            暴擊最大傷害 = 100;
                            break;
                    }
                    break;
                }
            }
            eff = chra.getStatForBuff(MapleBuffStat.Morph);
            if (eff != null && eff.getSourceId() % 10000 == 1105) { // ice knight
                局部最大HP = 500000;
                局部最大MP = 500000;
            }
        }
        chra.changeSkillLevel_Skip(sData, false);
        if (MapleJob.is惡魔殺手(chra.getJob()) || MapleJob.is凱內西斯(chra.getJob())) {
            局部最大MP = GameConstants.getMPByJob(chra);
            List<Integer> demonShields = Collections
                    .unmodifiableList(Arrays.asList(1099000, 1099002, 1099003, 1099004));
            Equip shield = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            if (shield != null && demonShields.contains(shield.getItemId())) {
                局部最大MP += shield.getMp(0xFFFF);
            }
        } else if (MapleJob.is神之子(chra.getJob())) {
            局部最大MP = 100;
        } else if (MapleJob.is陰陽師(chra.getJob())) {
            局部最大MP = 100;
            局部最大MP += this.incMaxDF;
        } else if (MapleJob.isNotMpJob(chra.getJob()) && chra.getJob() != 3001) {
            局部最大MP = 0;
        }
        if (MapleJob.is惡魔復仇者(chra.getJob())) {
            chra.getClient().getSession().writeAndFlush(JobPacket.AvengerPacket.giveAvengerHpBuff(hp));
        }
        CalcPassive_SharpEye(chra);
        CalcPassive_Mastery(chra);
        recalcPVPRank(chra);
        if (first_login) {
            chra.silentEnforceMaxHpMp();
            relocHeal(chra);
        } else {
            chra.enforceMaxHpMp();
        }
        calculateMaxBaseDamage(Math.max(魔法攻擊力, 物理攻擊力), pvpDamage, chra);
        trueMastery = Math.min(100, trueMastery);
        if (oldmaxhp != 0 && oldmaxhp != 局部最大HP) {
            chra.updatePartyMemberHP();
        }
    }

    public void handleLinkSkills(MapleCharacter chr) {
        Skill bx;
        int bof;
        MapleStatEffect eff;
        bx = SkillFactory.getSkill(80000000);// 百烈祝福
        bof = chr.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            局部力量 += eff.getStrX();
            局部敏捷 += eff.getDexX();
            局部智力 += eff.getIntX();
            局部幸運 += eff.getLukX();
            percent_hp += eff.getHpR();
            percent_mp += eff.getMpR();
        }
        bx = SkillFactory.getSkill(80000001);// 後續待發
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            bossdam_r += eff.getBossDamage();
        }
        bx = SkillFactory.getSkill(80000002);// 致命本能
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            暴擊概率 += eff.getCr();
        }
        bx = SkillFactory.getSkill(80000003);// 疾風傳授
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            局部力量 += eff.getStrX();
            局部敏捷 += eff.getStrX();
            局部智力 += eff.getStrX();
            局部幸運 += eff.getStrX();
            物理攻擊力 += bx.getEffect(bof).getAttackX();
        }
        bx = SkillFactory.getSkill(80000004);// 疾風傳授
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            int damage = ((5 * eff.getX()) + 100);
            dam_r += (damage);
            bossdam_r += (damage);
        }

        bx = SkillFactory.getSkill(80000005);// 波米艾特
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            ignoreTargetDEF += eff.getIgnoreMob();
        }
        bx = SkillFactory.getSkill(80000006);// 鋼鐵意志
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            percent_hp += eff.getPercentHP();
        }
        bx = SkillFactory.getSkill(80000024);// 紫扇傳授2
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            int damage = (eff.getDAMRate());
            dam_r += (damage);
            bossdam_r += (damage);
        }

        bx = SkillFactory.getSkill(80000047);// 疾風傳授
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            局部力量 += eff.getStrX();
            局部敏捷 += eff.getStrX();
            局部智力 += eff.getStrX();
            局部幸運 += eff.getStrX();
        }
        bx = SkillFactory.getSkill(80000050);// 狂暴鬥氣
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            int damage = (5 * eff.getX());
            dam_r += (damage);
            bossdam_r += (damage);
        }

        bx = SkillFactory.getSkill(80000066);// 西格諾斯祝福
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            int ADD_ASR = eff.getASRRate();
            int ADD_TER = ADD_ASR;
            異常抗性 += ADD_ASR;
            屬性抗性 += ADD_TER;
        }
        bx = SkillFactory.getSkill(80000067);// 西格諾斯祝福
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            int ADD_ASR = eff.getASRRate();
            int ADD_TER = ADD_ASR;
            異常抗性 += ADD_ASR;
            屬性抗性 += ADD_TER;
        }
        bx = SkillFactory.getSkill(80000068);// 西格諾斯祝福
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            int ADD_ASR = eff.getASRRate();
            int ADD_TER = ADD_ASR;
            異常抗性 += ADD_ASR;
            屬性抗性 += ADD_TER;
        }
        bx = SkillFactory.getSkill(80000069);// 西格諾斯祝福
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            int ADD_ASR = eff.getASRRate();
            int ADD_TER = ADD_ASR;
            異常抗性 += ADD_ASR;
            屬性抗性 += ADD_TER;
        }
        bx = SkillFactory.getSkill(80000070);// 西格諾斯祝福
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            int ADD_ASR = eff.getASRRate();
            int ADD_TER = ADD_ASR;
            異常抗性 += ADD_ASR;
            屬性抗性 += ADD_TER;
        }
        bx = SkillFactory.getSkill(80000110);// 時之祝福
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            ignoreTargetDEF += eff.getIgnoreMob();
        }
        bx = SkillFactory.getSkill(80010006);// 精靈集中
        bof = chr.getTotalSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            bossdam_r += eff.getBossDamage();
            暴擊概率 += eff.getCr();
            percent_hp += eff.getPercentHP();
            percent_mp += eff.getPercentMP();
        }
    }

    private void handlePassiveSkills(MapleCharacter chra) {
        Skill bx;
        int bof;
        MapleStatEffect eff;

        // 連結技能
        handleLinkSkills(chra);

        // 精靈的祝福
        bx = SkillFactory.getSkill(GameConstants.getBOF_ForJob(chra.getJob()));
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            物理攻擊力 += eff.getX();
            魔法攻擊力 += eff.getY();
        }

        // 女皇的祝福
        bx = SkillFactory.getSkill(GameConstants.getEmpress_ForJob(chra.getJob()));
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            物理攻擊力 += eff.getX();
            魔法攻擊力 += eff.getY();
        }
        // 聯盟的意志
        bx = SkillFactory.getSkill(GameConstants.ge聯盟意志_ForJob(chra.getJob()));
        bof = chra.getSkillLevel(bx);
        if (bof > 0) {
            eff = bx.getEffect(bof);
            局部力量 += eff.getStrX();
            局部敏捷 += eff.getDexX();
            局部幸運 += eff.getLukX();
            局部智力 += eff.getIntX();
            物理攻擊力 += eff.getAttackX();
            魔法攻擊力 += eff.getMagicX();
        }
        int job = chra.getJob();
        if (MapleJob.is冒險家(job)) {
            if (MapleJob.is劍士(job)) {
                // 戰鬥技能
                bx = SkillFactory.getSkill(1000009);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    移動速度 += eff.getPassiveSpeed();
                    跳躍力 += eff.getPassiveJump();
                    最大移動速度 += eff.getSpeedMax();
                    增加最大HP += eff.getLevelToMaxHp() * chra.getLevel();
                    格擋率 += eff.info.get(MapleStatInfo.stanceProp);
                }
                // 自身強化
                bx = SkillFactory.getSkill(1001003);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
            } else if (MapleJob.is法師(job)) {
                // 魔力增幅
                bx = SkillFactory.getSkill(2000006);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_mp += eff.getPercentMP();
                    百分比加成之後增加最大MP += eff.getLevelToMaxMp() * chra.getLevel();
                    Equip eq = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
                    if (eq != null && ItemConstants.類型.短杖(eq.getItemId())) {
                        暴擊概率 += 3;
                    }
                }
                // 魔力之盾
                bx = SkillFactory.getSkill(2000010);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    add_wdef += 防御力 * eff.getWDEFX() / 100;
                }
            } else if (MapleJob.is弓箭手(job)) {
                // 霸王箭
                bx = SkillFactory.getSkill(3000001);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    暴擊概率 += eff.getProb();
                }
                // 精通射手
                bx = SkillFactory.getSkill(3000002);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    迴避值 += eff.getAvoid();
                    defRange += eff.getRange();
                    移動速度 += eff.getSpeed();
                    最大移動速度 += eff.getU();
                }
            } else if (MapleJob.is盜賊(job) && chra.getSubcategory() == 0) {
                // 幻化術
                bx = SkillFactory.getSkill(4000000);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    迴避值 += eff.getY();
                }
                // 速度激發
                bx = SkillFactory.getSkill(4001005);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    最大移動速度 += eff.getSpeedMax();
                }
            } else if (MapleJob.is海盜(job) && !MapleJob.is重砲指揮官(job) && !MapleJob.is蒼龍俠客(job)) {
                // 能力極限
                bx = SkillFactory.getSkill(5000000);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    跳躍力 = +eff.getPassiveJump();
                    最大移動速度 += eff.getSpeedMax();
                }
            }
            // 女皇的強化
            bx = SkillFactory.getSkill(74);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }
            // 女皇的強化
            bx = SkillFactory.getSkill(80);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                levelBonus += bx.getEffect(bof).getX();
            }
        } else if (MapleJob.is皇家騎士團(job)) {
            // 女皇的傲氣
            bx = SkillFactory.getSkill(10000074);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int MaxHP = eff.getX();
                int MaxMP = eff.getX();
                percent_hp += MaxHP;
                percent_mp += MaxMP;
            }
            // 自然旋律
            bx = SkillFactory.getSkill(10000246);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += Math.min(eff.getLevelToStr(), (int) Math.floor(chra.getLevel() / 2));
            }
            // 自然旋律
            bx = SkillFactory.getSkill(10000247);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部敏捷 += Math.min(eff.getLevelToDex(), (int) Math.floor(chra.getLevel() / 2));
            }
            // 自然旋律
            bx = SkillFactory.getSkill(10000248);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部智力 += Math.min(eff.getLevelToInt(), (int) Math.floor(chra.getLevel() / 2));
            }
            // 自然旋律
            bx = SkillFactory.getSkill(10000249);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部幸運 += Math.min(eff.getLevelToLuk(), (int) Math.floor(chra.getLevel() / 2));
            }
            // 西格諾斯祝福（劍士）
            bx = SkillFactory.getSkill(10000255);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_ASR = eff.getASRRate();
                int ADD_TER = eff.getASRRate();
                異常抗性 += ADD_ASR;
                屬性抗性 += ADD_TER;
            }
            // 西格諾斯祝福（法師）
            bx = SkillFactory.getSkill(10000256);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_ASR = eff.getASRRate();
                int ADD_TER = eff.getASRRate();
                異常抗性 += ADD_ASR;
                屬性抗性 += ADD_TER;
            }
            // 西格諾斯祝福（弓箭手）
            bx = SkillFactory.getSkill(10000257);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_ASR = eff.getASRRate();
                int ADD_TER = eff.getASRRate();
                異常抗性 += ADD_ASR;
                屬性抗性 += ADD_TER;
            }
            // 西格諾斯祝福（盜賊）
            bx = SkillFactory.getSkill(10000258);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_ASR = eff.getASRRate();
                int ADD_TER = eff.getASRRate();
                異常抗性 += ADD_ASR;
                屬性抗性 += ADD_TER;
            }
            // 西格諾斯祝福（海盜）
            bx = SkillFactory.getSkill(10000259);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_ASR = eff.getASRRate();
                int ADD_TER = eff.getASRRate();
                異常抗性 += ADD_ASR;
                屬性抗性 += ADD_TER;
            }
        } else if (MapleJob.is末日反抗軍(job) && !MapleJob.is惡魔(job) && !MapleJob.is傑諾(job)) {
            // 效能
            bx = SkillFactory.getSkill(30000002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                道具恢復效能提升 += eff.getX() - 100;
            }
        }

        if (MapleJob.is英雄(job)) {
            // 武器精通
            bx = SkillFactory.getSkill(1100000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
            }
            // 體能訓練
            bx = SkillFactory.getSkill(1100009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 恢復術
            bx = SkillFactory.getSkill(1110011);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getASRRate();
                屬性抗性 += eff.getTERRate();
            }
            // 戰鬥精通
            bx = SkillFactory.getSkill(1120012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 進階終極攻擊
            bx = SkillFactory.getSkill(1120013);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getAttackX();
                addDamageIncrease(1100002, eff.getDamage());
            }
            // 反抗姿態
            bx = SkillFactory.getSkill(1120014);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                格擋率 += eff.info.get(MapleStatInfo.stanceProp);
            }
        } else if (MapleJob.is聖騎士(job)) {
            // 武器精通
            bx = SkillFactory.getSkill(1200000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
            }
            // 體能訓練
            bx = SkillFactory.getSkill(1200009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 盾防精通
            bx = SkillFactory.getSkill(1210001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                異常抗性 += bx.getEffect(bof).getASRRate();
                屬性抗性 += bx.getEffect(bof).getTERRate();
            }
            // 反抗姿態
            bx = SkillFactory.getSkill(1220017);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                格擋率 += eff.info.get(MapleStatInfo.stanceProp);
            }
        } else if (MapleJob.is黑騎士(job)) {
            // 武器精通
            bx = SkillFactory.getSkill(1300000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
            }
            // 體能訓練
            bx = SkillFactory.getSkill(1300009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 暗黑之力
            bx = SkillFactory.getSkill(1310009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
                暴擊最小傷害 += eff.getCriticalMin();
                hpRecoverProp += eff.getProb();
                hpRecoverPercent += eff.getX();
            }
            // 恢復術
            bx = SkillFactory.getSkill(1310010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getASRRate();
                屬性抗性 += eff.getTERRate();
            }
            // 反抗姿態
            bx = SkillFactory.getSkill(1320017);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                格擋率 += eff.info.get(MapleStatInfo.stanceProp);
            }
        } else if (MapleJob.is大魔導士_火毒(job)) {
            bx = SkillFactory.getSkill(2100007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                局部智力 += bx.getEffect(bof).getIntX();
            }
            bx = SkillFactory.getSkill(2110000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dotTime += eff.getX();
                dot += eff.getZ();
            }
            bx = SkillFactory.getSkill(2110001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                mpconPercent += eff.getX() - 100;
                int Damage = eff.getY();
                dam_r += Damage;
                bossdam_r += Damage;
            }
            bx = SkillFactory.getSkill(2121003);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(2111003, eff.getX());
            }
            // 大師魔法
            bx = SkillFactory.getSkill(2120012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                魔法攻擊力 += eff.getMagicX();
                BUFF技能持續時間提升 += eff.getX();
            }
            bx = SkillFactory.getSkill(2121005);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                屬性抗性 += bx.getEffect(bof).getTERRate();
            }
            bx = SkillFactory.getSkill(2121009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                魔法攻擊力 += bx.getEffect(bof).getMagicX();
            }
            bx = SkillFactory.getSkill(2120010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                bossdam_r *= (eff.getX() * eff.getY() + 100.0) / 100.0;
                ignoreTargetDEF += eff.getIgnoreMob();
            }
        } else if (MapleJob.is大魔導士_冰雷(job)) {
            bx = SkillFactory.getSkill(2200007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                局部智力 += bx.getEffect(bof).getIntX();
            }
            bx = SkillFactory.getSkill(2210000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                dot += bx.getEffect(bof).getZ();
            }
            bx = SkillFactory.getSkill(2210001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                mpconPercent += eff.getX() - 100;
                dam_r += eff.getY();
                bossdam_r += eff.getY();
            }
            // 大師魔法
            bx = SkillFactory.getSkill(2220013);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                魔法攻擊力 += eff.getMagicX();
                BUFF技能持續時間提升 += eff.getX();
            }
            bx = SkillFactory.getSkill(2221005);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                屬性抗性 += bx.getEffect(bof).getTERRate();
            }
            bx = SkillFactory.getSkill(2221009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                魔法攻擊力 += bx.getEffect(bof).getMagicX();
            }
            bx = SkillFactory.getSkill(2220010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += dam_r * ((eff.getX() * eff.getY() + 100.0) / 100.0);
                bossdam_r *= bossdam_r * ((eff.getX() * eff.getY() + 100.0) / 100.0);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
        } else if (MapleJob.is主教(job)) {
            bx = SkillFactory.getSkill(2300007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                局部智力 += bx.getEffect(bof).getIntX();
            }
            bx = SkillFactory.getSkill(2310008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                暴擊概率 += bx.getEffect(bof).getCr();
            }
            // 大師魔法
            bx = SkillFactory.getSkill(2320012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                魔法攻擊力 += eff.getMagicX();
                BUFF技能持續時間提升 += eff.getX();
            }
            bx = SkillFactory.getSkill(2321010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                魔法攻擊力 += bx.getEffect(bof).getMagicX();
            }
            bx = SkillFactory.getSkill(2320005);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                異常抗性 += bx.getEffect(bof).getASRRate();
            }
            bx = SkillFactory.getSkill(2320011);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += dam_r * ((eff.getX() * eff.getY() + 100.0) / 100.0);
                bossdam_r += bossdam_r * ((eff.getX() * eff.getY() + 100.0) / 100.0);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
        } else if (MapleJob.is箭神(job)) {
            // 體能訓練
            bx = SkillFactory.getSkill(3100006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 召喚鳳凰
            bx = SkillFactory.getSkill(3111005);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int WdefAndMdef = eff.getWDEFRate();
                percent_wdef += WdefAndMdef;
                percent_mdef += WdefAndMdef;
            }
            // 飛影位移
            bx = SkillFactory.getSkill(3111010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
            }
            // 集中專注
            bx = SkillFactory.getSkill(3110012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_ASR = eff.getASRRate();
                int ADD_TER = eff.getTERRate();
                異常抗性 += ADD_ASR;
                屬性抗性 += ADD_TER;
            }

            // 射擊術
            bx = SkillFactory.getSkill(3110014);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 弓術精通
            bx = SkillFactory.getSkill(3120005);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getX();
                trueMastery += eff.getMastery();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 驟雨狂矢-強化傷害
            bx = SkillFactory.getSkill(3120046);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(3121015, (bx.getEffect(bof).getDAMRate()));
            }
            // 暴風神射-強化傷害
            bx = SkillFactory.getSkill(3120049);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(3121020, (bx.getEffect(bof).getDAMRate()));
            }
            // 爆發
            bx = SkillFactory.getSkill(3121007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
        } else if (MapleJob.is神射手(job)) {
            bx = SkillFactory.getSkill(3200006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 召喚銀隼
            bx = SkillFactory.getSkill(3211005);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int MandWDef = eff.getWDEFRate();
                percent_wdef += MandWDef;
                percent_mdef += MandWDef;
            }
            // 飛影位移
            bx = SkillFactory.getSkill(3211010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
            }
            // 痛苦殺手
            bx = SkillFactory.getSkill(3211011);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_ASR = eff.getASRRate();
                int ADD_TER = eff.getTERRate();
                異常抗性 += ADD_ASR;
                屬性抗性 += ADD_TER;
            }

            // 射擊術
            bx = SkillFactory.getSkill(3210014);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
                ignoreTargetDEF += eff.getIgnoreMob();
            }

            // 弩術精通
            bx = SkillFactory.getSkill(3220004);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getX();
                trueMastery += eff.getMastery();
                暴擊最小傷害 += eff.getCriticalMin();
            }

            // 爆發
            bx = SkillFactory.getSkill(3221006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
            }

        } else if (MapleJob.is夜使者(job)) {
            // 精準暗器
            bx = SkillFactory.getSkill(4100000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
            }
            // 強力投擲
            bx = SkillFactory.getSkill(4100001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getProb();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 體能訓練
            bx = SkillFactory.getSkill(4100007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部敏捷 += eff.getDexX();
                局部幸運 += eff.getLukX();
            }
            // 永恆黑暗
            bx = SkillFactory.getSkill(4110008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getASRRate();
                percent_hp += eff.getPercentHP();
                屬性抗性 += eff.getTERRate();
            }
            // 鏢術精通
            bx = SkillFactory.getSkill(4110012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.info.get(MapleStatInfo.pdR);
            }
            // 藥劑精通
            bx = SkillFactory.getSkill(4110014);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊最小傷害 += eff.getCriticalMin();
                暴擊最大傷害 += eff.getCriticalMax();
                道具恢復效能提升 += eff.getX() - 100;
            }
            // 瞬身迴避
            bx = SkillFactory.getSkill(4120002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                閃避率 += eff.getProb();
            }
            // 暗器精通
            bx = SkillFactory.getSkill(4120012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊最大傷害 += eff.getCriticalMax();
                trueMastery += eff.getMastery();
                物理攻擊力 += eff.getX();
            }
            // 黑暗能量
            bx = SkillFactory.getSkill(4121014);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 絕對領域
            bx = SkillFactory.getSkill(4121015);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                bossdam_r += eff.getBossDamage();
            }
        } else if (MapleJob.is暗影神偷(job)) {
            // 精準之刀
            bx = SkillFactory.getSkill(4200000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
            }
            // 體能訓練
            bx = SkillFactory.getSkill(4200007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部敏捷 += eff.getDexX();
                局部幸運 += eff.getLukX();
            }
            // 強化盾
            bx = SkillFactory.getSkill(4200010);
            bof = chra.getPassiveLevel(bx);
            boolean equip = true;
            Item shield = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            if (shield == null) {
                equip = false;
            }
            if (bof > 0 && equip && ItemConstants.類型.盾牌(shield.getItemId())) {
                eff = bx.getEffect(bof);
                percent_wdef += eff.getWDEFRate();
                percent_mdef += eff.getMDEFRate();
                閃避率 += bx.getEffect(bof).getER();
                物理攻擊力 += eff.getY();
            }
            // 貪婪
            bx = SkillFactory.getSkill(4210012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                mesoBuff *= (eff.getMesoRate() + 100.0) / 100.0;
                pickRate += eff.getU();
                mesoGuard -= eff.getV();
                mesoGuardMeso -= eff.getW();
                // 楓幣炸彈
                addDamageIncrease(4211006, eff.getX());
                物理攻擊力 += eff.getAttackX();
            }
            // 永恆黑暗
            bx = SkillFactory.getSkill(4210013);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getASRRate();
                percent_hp += eff.getPercentHP();
                屬性抗性 += eff.getTERRate();
            }
            // 瞬身迴避
            bx = SkillFactory.getSkill(4220002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                閃避率 += eff.getProb();
            }
            // 進階精準之刀
            bx = SkillFactory.getSkill(4220012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊最小傷害 += eff.getCriticalMin();
                trueMastery += eff.getMastery();
                物理攻擊力 += eff.getX();
            }
            // 致命爆擊
            bx = SkillFactory.getSkill(4220015);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊最大傷害 += eff.getCriticalMax();
            }
            // 瞬步連擊
            bx = SkillFactory.getSkill(4221007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.info.get(MapleStatInfo.pdR);
            }
            // 暗殺本能
            bx = SkillFactory.getSkill(4221013);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
        } else if (MapleJob.is影武者(job) || (job == 400 && chra.getSubcategory() == 1)) {
            // 下忍 被動
            // 精準雙刀
            bx = SkillFactory.getSkill(4300000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
            }
            // 自我速度激發
            bx = SkillFactory.getSkill(4301003);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                最大移動速度 += eff.getSpeedMax();
            }
            // 中忍 被動
            // 體能訓練
            bx = SkillFactory.getSkill(4310006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部敏捷 += eff.getDexX();
                局部幸運 += eff.getLukX();
            }
            // 隱忍 被動
            // 竊取生命
            bx = SkillFactory.getSkill(4330007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                hpRecoverProp += eff.getProb();
                hpRecoverPercent += eff.getX();
            }
            // 激進黑暗
            bx = SkillFactory.getSkill(4330008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getASRRate();
                percent_hp += eff.getPercentHP();
                屬性抗性 += eff.getTERRate();
            }
            // 血雨暴風狂斬
            bx = SkillFactory.getSkill(4331000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(4301004, eff.getDAMRate());
                addDamageIncrease(4321006, eff.getDAMRate());
            }
            // 影武者 被動
            // 幻影替身
            bx = SkillFactory.getSkill(4341006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                閃避率 += eff.getER();
                percent_wdef += eff.getWDEFRate();
                percent_mdef += eff.getMDEFRate();
            }
            // 雙刀流精通
            bx = SkillFactory.getSkill(4300000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
            }
        } else if (MapleJob.is拳霸(job)) {
            // 體能突破
            bx = SkillFactory.getSkill(5100009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                格擋率 += eff.info.get(MapleStatInfo.stanceProp);
            }
            bx = SkillFactory.getSkill(5110008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) { // Backspin Blow, Double Uppercut, and Corkscrew Blow
                eff = bx.getEffect(bof);
                addDamageIncrease(5101002, eff.getX());
                addDamageIncrease(5101003, eff.getY());
                addDamageIncrease(5101004, eff.getZ());
            }
            // 爆擊鬥氣
            bx = SkillFactory.getSkill(5110011);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
                暴擊概率 += eff.getProb();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 拳霸大師
            bx = SkillFactory.getSkill(5121015);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
                異常抗性 += eff.getTERRate();
                屬性抗性 += eff.getTERRate();
            }
        } else if (MapleJob.is槍神(job)) {
            // 體能訓練
            bx = SkillFactory.getSkill(5200009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 神槍手耐性
            bx = SkillFactory.getSkill(5210012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_wdef += eff.getWDEFRate();
                percent_mdef += eff.getWDEFRate();
                增加最大HP += eff.getX();
                增加最大MP += eff.getX();
            }
            // 金屬外殼
            bx = SkillFactory.getSkill(5210013);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 無盡追擊
            bx = SkillFactory.getSkill(5220001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊最小傷害 += eff.getCriticalMin();
                暴擊最大傷害 += eff.getCriticalMin();
            }
            // 瞬‧冰火連擊
            bx = SkillFactory.getSkill(5220001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) { // Flamethrower and Ice Splitter
                eff = bx.getEffect(bof);
                addDamageIncrease(5211004, eff.getDamage());
                addDamageIncrease(5211005, eff.getDamage());
            }
            // 船員指令
            bx = SkillFactory.getSkill(5220019);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                maxhp += eff.getMaxHpX();
            }
        } else if (MapleJob.is重砲指揮官(job) || chra.getSubcategory() == 2) {
            // 百烈祝福。
            bx = SkillFactory.getSkill(110);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
                局部智力 += eff.getIntX();
                局部幸運 += eff.getLukX();
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
            }
            // 加農砲升級
            bx = SkillFactory.getSkill(5010003);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getAttackX();
                防御力 += eff.getWDEFX();
            }
            // 百烈訓練
            bx = SkillFactory.getSkill(5300008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 終極狀態
            bx = SkillFactory.getSkill(5310007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                異常抗性 += eff.getASRRate();
                percent_wdef += eff.getWDEFRate();
            }
            bx = SkillFactory.getSkill(5310006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                物理攻擊力 += bx.getEffect(bof).getAttackX();
            }

            // 猴子的衝擊波
            bx = SkillFactory.getSkill(5311002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
            }
            // 幸運木桶
            bx = SkillFactory.getSkill(5311004);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
            }
            // 炎熱加農砲
            bx = SkillFactory.getSkill(5320009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
                trueMastery += eff.getMastery();
                ignoreTargetDEF += eff.getIgnoreMob();
            }
        } else if (MapleJob.is蒼龍俠客(job)) {
            // 俠客之道
            bx = SkillFactory.getSkill(5080022);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Avoid = eff.getAvoid();
                int Acc = eff.getAcc();
                int Range = eff.getRange();
                int sp = eff.getSpeed();
                int Msp = eff.getU();
                迴避值 += Avoid;
                defRange += Range;
                移動速度 += sp;
                最大移動速度 += Msp;
            }
            // 俠客秘訣
            bx = SkillFactory.getSkill(5700011);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
                格擋率 += eff.info.get(MapleStatInfo.stanceProp);
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
            }
            // 真氣流貫
            bx = SkillFactory.getSkill(5701013);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStr();
                局部敏捷 += eff.getDex();
            }
            // 預知眼
            bx = SkillFactory.getSkill(5710021);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                迴避值 += eff.getAvoidX();
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 必殺一擊
            bx = SkillFactory.getSkill(5720060);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊最小傷害 += eff.getCriticalMin();
                暴擊最大傷害 += eff.getCriticalMax();
                trueMastery += eff.getMastery();
            }
            // 金剛不壞
            bx = SkillFactory.getSkill(5720061);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_wdef += eff.getWDEFRate();
                percent_mdef += eff.getMDEFRate();
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
                異常抗性 += eff.getASRRate();
                屬性抗性 += eff.getTERRate();
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
            }

        } else if (MapleJob.is聖魂劍士(job)) {
            bx = SkillFactory.getSkill(11000005);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                percent_hp += bx.getEffect(bof).getPercentHP();
            }
        } else if (MapleJob.is烈焰巫師(job)) {
            bx = SkillFactory.getSkill(12120008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                dot += bx.getEffect(bof).getY();
            }
            bx = SkillFactory.getSkill(12000024);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                reduceDamageRate += bx.getEffect(bof).getX();
            }
            bx = SkillFactory.getSkill(12000025);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                percent_mp += bx.getEffect(bof).getPercentMP();
                百分比加成之後增加最大MP += bx.getEffect(bof).getLevelToMaxMp() * chra.getLevel();
            }
            bx = SkillFactory.getSkill(12100008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                局部智力 += bx.getEffect(bof).getIntX();
            }
            bx = SkillFactory.getSkill(12110001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                mpconPercent += eff.getX() - 100;
                dam_r += eff.getY();
                bossdam_r += eff.getY();
            }
            bx = SkillFactory.getSkill(12110001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                mpconPercent += eff.getX() - 100;
                int Damage = eff.getY();
                dam_r += Damage;
                bossdam_r += Damage;
            }

            bx = SkillFactory.getSkill(12111004);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                屬性抗性 += bx.getEffect(bof).getTERRate();
            }
        } else if (MapleJob.is破風使者(job)) {
            bx = SkillFactory.getSkill(13000001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                defRange += bx.getEffect(bof).getRange();
            }
            bx = SkillFactory.getSkill(13110008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                閃避率 += bx.getEffect(bof).getER();
            }
            bx = SkillFactory.getSkill(13110003);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
                暴擊最小傷害 += eff.getCriticalMin();
            }
        } else if (MapleJob.is暗夜行者(job)) {
            bx = SkillFactory.getSkill(14110003);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                道具恢復效能提升 += eff.getX() - 100;
                BUFF道具持續時間提升 += eff.getY() - 100;
            }
            bx = SkillFactory.getSkill(14000001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                defRange += bx.getEffect(bof).getRange();
            }
            // 激進黑暗
            bx = SkillFactory.getSkill(14110009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getASRRate();
                percent_hp += eff.getPercentHP();
                屬性抗性 += eff.getTERRate();
            }
            // 激進闇黑
            bx = SkillFactory.getSkill(14110026);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                異常抗性 += eff.getASRRate();
                屬性抗性 += eff.getTERRate();
            }
        } else if (MapleJob.is閃雷悍將(job)) {
            // 極限迴避
            bx = SkillFactory.getSkill(15000000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                移動速度 += eff.getSpeedMax();
                跳躍力 = +eff.getPassiveJump();
            }
            // 初階爆擊
            bx = SkillFactory.getSkill(15000006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 增加生命
            bx = SkillFactory.getSkill(15000008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
            }
            // 體能訓練
            bx = SkillFactory.getSkill(15100009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 鍛鍊
            bx = SkillFactory.getSkill(15100024);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
            }
            // 雷聲
            bx = SkillFactory.getSkill(15100025);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getAttackX();
            }
            // 爆擊鬥氣
            bx = SkillFactory.getSkill(15110009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
                暴擊概率 += eff.getProb();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 連鎖
            bx = SkillFactory.getSkill(15110025);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getX();
            }
            // 指虎精通
            bx = SkillFactory.getSkill(15120006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
                物理攻擊力 += eff.getX();
                物理攻擊力 += eff.getCriticalMin();
            }
            // 刺激
            bx = SkillFactory.getSkill(15120007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                閃避率 += eff.getER();
                reduceDamageRate += eff.info.get(MapleStatInfo.ignoreMobDamR);
            }
        } else if (MapleJob.is狂狼勇士(job)) {
            // 戰鬥衝刺
            bx = SkillFactory.getSkill(21001001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                移動速度 += eff.getPassiveSpeed();
                最大移動速度 += eff.getSpeedMax();
            }

            // 體能訓練
            bx = SkillFactory.getSkill(21100008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 寒冰屬性
            bx = SkillFactory.getSkill(21101006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
            }
            // 攀爬 攻擊
            bx = SkillFactory.getSkill(21110010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                bossdam_r += eff.getBossDamage();
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 伺機攻擊
            bx = SkillFactory.getSkill(21110002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                addDamageIncrease(21000004, bx.getEffect(bof).getW());
            }

            // 攻擊戰術
            bx = SkillFactory.getSkill(21120001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getX();
                trueMastery += eff.getMastery();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 終極攻擊
            bx = SkillFactory.getSkill(21120002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                addDamageIncrease(21100007, bx.getEffect(bof).getZ());
            }
            // 防禦戰術
            bx = SkillFactory.getSkill(21120004);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                DMGreduceR += eff.getT();
                percent_hp += eff.getPercentHP();
            }
            // 快速移動
            bx = SkillFactory.getSkill(21120011);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(21101011, eff.getDAMRate());
                addDamageIncrease(21100002, eff.getDAMRate());
                addDamageIncrease(21110003, eff.getDAMRate());
            }
            // 屠魔勇氣
            bx = SkillFactory.getSkill(21120014);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getBossDamage();
            }
        } else if (MapleJob.is龍魔導士(job)) {
            bx = SkillFactory.getSkill(22000000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                魔法攻擊力 += bof;
            }
            // 守護之力
            bx = SkillFactory.getSkill(22131001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
            }
            bx = SkillFactory.getSkill(22150000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                mpconPercent += eff.getX() - 100;
                int Damage = eff.getY();
                dam_r += Damage;
                bossdam_r += Damage;
            }
            bx = SkillFactory.getSkill(22160000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getDamage();
                bossdam_r += eff.getDamage();
            }
            bx = SkillFactory.getSkill(22170001); // magic mastery, this is an invisible skill
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                魔法攻擊力 += eff.getX();
                trueMastery += eff.getMastery();
                暴擊最小傷害 += eff.getCriticalMin();
            }
        } else if (MapleJob.is精靈遊俠(job)) {
            // 王的資格
            bx = SkillFactory.getSkill(20020112);
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                移動速度 += eff.getPassiveSpeed();
                跳躍力 += eff.getPassiveJump();
                chra.getTrait(MapleTraitType.charm).addLocalExp(GameConstants.getTraitExpNeededForLevel(30));
            }
            bx = SkillFactory.getSkill(23000001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                閃避率 += bx.getEffect(bof).getER();
            }
            bx = SkillFactory.getSkill(23100008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            bx = SkillFactory.getSkill(23110004);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                閃避率 += bx.getEffect(bof).getProb();
            }
            bx = SkillFactory.getSkill(23110004);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                addDamageIncrease(23101001, bx.getEffect(bof).getDAMRate());
            }
            bx = SkillFactory.getSkill(23121004);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                閃避率 += bx.getEffect(bof).getProb();
            }
            bx = SkillFactory.getSkill(23120009);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                物理攻擊力 += bx.getEffect(bof).getX();
            }
            bx = SkillFactory.getSkill(23120010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                ignoreTargetDEF += bx.getEffect(bof).getX(); // or should we do 100?
            }
            bx = SkillFactory.getSkill(23120011);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                addDamageIncrease(23101001, bx.getEffect(bof).getDAMRate());
            }
            bx = SkillFactory.getSkill(23120012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                物理攻擊力 += bx.getEffect(bof).getAttackX();
            }
        } else if (MapleJob.is幻影俠盜(job)) {
            // 致命本能
            bx = SkillFactory.getSkill(20030204);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
                暴擊最小傷害 += eff.getCriticalMin();
            }

            // 高洞察力
            bx = SkillFactory.getSkill(20030206);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部敏捷 += eff.getDexX();
                閃避率 += eff.getER();
            }
            // 快速迴避
            bx = SkillFactory.getSkill(24000003);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                閃避率 += eff.getER();
            }
            // 幻影瞬步
            bx = SkillFactory.getSkill(24001002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                移動速度 += eff.getPassiveSpeed();
                跳躍力 += eff.getPassiveJump();
            }
            // 幸運富翁
            bx = SkillFactory.getSkill(24100006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部幸運 += eff.getLukX();
            }
            // 幸運幻影
            bx = SkillFactory.getSkill(24111002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部幸運 += eff.getLukX();
            }

            // 幻影迴避
            bx = SkillFactory.getSkill(24110004);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                閃避率 += eff.getProb();
            }

            // 國王突刺
            bx = SkillFactory.getSkill(24111006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(24101002, eff.getDAMRate());
            }

            // 爆擊天賦
            bx = SkillFactory.getSkill(24110007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 死神卡牌
            bx = SkillFactory.getSkill(24120002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                閃避率 += eff.getSubProb();
            }
        } else if (MapleJob.is隱月(job)) {
            bx = SkillFactory.getSkill(25000105);// 乾坤一体
            bof = chra.getSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
                // wdef += eff.getWdefX();
                // mdef += eff.getMdefX();
            }
            bx = SkillFactory.getSkill(25101205); // 后方移动
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                閃避率 += eff.getER();
            }
            bx = SkillFactory.getSkill(25100106); // 拳甲修炼
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
            }
            bx = SkillFactory.getSkill(25100108);// 力量锻炼
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
            }
            bx = SkillFactory.getSkill(25110107);// 精灵凝聚第3招
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                // percent_damage += eff.getDAMRate();
                物理攻擊力 += eff.getAttackX();
            }
            bx = SkillFactory.getSkill(25110108); // 招魂式
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                // wdef += eff.getWdefX();
                // mdef += eff.getMdefX();
                異常抗性 += eff.getASRRate();
                屬性抗性 += eff.getTERRate();
            }
            bx = SkillFactory.getSkill(25120112); // 精灵凝聚第4招
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                // percent_ignore_mob_def_rate += eff.getIgnoreMob();
                // percent_boss_damage_rate += eff.getBossDamage();
            }
            bx = SkillFactory.getSkill(25120113); // 高级拳甲修炼
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
                暴擊最小傷害 += eff.getCriticalMin();
                暴擊最大傷害 += eff.getCriticalMax();
            }
        } else if (MapleJob.is夜光(job)) {
            // 滲透
            bx = SkillFactory.getSkill(20040218);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 光魔法強化
            bx = SkillFactory.getSkill(27000106);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Damage = eff.getX(); // mdR = x
                addDamageIncrease(27001100, Damage);
                addDamageIncrease(27101100, Damage);
                addDamageIncrease(27101101, Damage);
                addDamageIncrease(27111100, Damage);
                addDamageIncrease(27111101, Damage);
                addDamageIncrease(27121100, Damage);
            }
            // 黑暗魔法強化
            bx = SkillFactory.getSkill(27000207);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Damage = eff.getX(); // mdR = x
                addDamageIncrease(27001201, Damage);
                addDamageIncrease(27101202, Damage);
                addDamageIncrease(27111202, Damage);
                addDamageIncrease(27121201, Damage);
                addDamageIncrease(27121202, Damage);
                addDamageIncrease(27120211, Damage);
            }

            // 魔法防禦
            bx = SkillFactory.getSkill(27000003);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                防御力 += eff.getWDEFX();
            }
            // 智慧昇華
            bx = SkillFactory.getSkill(27100006);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部智力 += eff.getIntX();
            }
            // 閃光瞬步
            bx = SkillFactory.getSkill(27001002);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                跳躍力 += eff.getPassiveJump();
                移動速度 += eff.getPassiveSpeed();
            }
            // 滲透
            bx = SkillFactory.getSkill(27110007);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                if (chra.getStat().getMPPercent() > chra.getStat().getHPPercent()) {
                    int dam = eff.getX();
                    dam_r += dam;
                    bossdam_r += dam;
                } else {
                    暴擊概率 += eff.getProb();
                }
            }
            // 精通魔法
            bx = SkillFactory.getSkill(27120007);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += eff.getMastery();
                魔法攻擊力 += eff.getX();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 光暗精通 TODO: 增加平衡時間
            bx = SkillFactory.getSkill(27120008);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(27111303, eff.getDAMRate());
            }
        } else if (MapleJob.is惡魔(job)) {
            // 魔族之血
            bx = SkillFactory.getSkill(30010185);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                格擋率 += eff.getX();
                chra.getTrait(MapleTraitType.will).addLocalExp(GameConstants.getTraitExpNeededForLevel(eff.getY()));
                chra.getTrait(MapleTraitType.charisma).addLocalExp(GameConstants.getTraitExpNeededForLevel(eff.getZ()));
            }
            if (MapleJob.is惡魔殺手(job)) {
                // 惡魔之怒
                bx = SkillFactory.getSkill(30010112);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    bossdam_r += eff.getBossDamage();
                    mpRecover += eff.getX();
                    mpRecoverProp += eff.getBossDamage(); // yes
                }
                // 死亡詛咒
                bx = SkillFactory.getSkill(30010111);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    hpRecoverPercent += eff.getX();
                    hpRecoverProp += eff.getProb(); // yes
                }
                // HP增加
                bx = SkillFactory.getSkill(31000003);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                // 惡魔狂斬 1次強化
                bx = SkillFactory.getSkill(31100007);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(31000004, eff.getDAMRate());
                    addDamageIncrease(31001006, eff.getDAMRate());
                    addDamageIncrease(31001007, eff.getDAMRate());
                    addDamageIncrease(31001008, eff.getDAMRate());
                }
                // 體能訓練
                bx = SkillFactory.getSkill(31100005);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    局部力量 += eff.getStrX();
                    局部敏捷 += eff.getDexX();
                }
                // 惡魔狂斬 2次強化
                bx = SkillFactory.getSkill(31110010);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(31000004, eff.getDAMRate());
                    addDamageIncrease(31001006, eff.getDAMRate());
                    addDamageIncrease(31001007, eff.getDAMRate());
                    addDamageIncrease(31001008, eff.getX());
                }
                // 力量防禦
                bx = SkillFactory.getSkill(31110008);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    閃避率 += eff.getX();
                    // HACK: shouldn't be here
                    hpRecoverPercent += eff.getY();
                    hpRecoverProp += eff.getX();
                    // mpRecover += eff.getY(); // handle in takeDamage
                    // mpRecoverProp += eff.getX();
                }
                // 強化惡魔之力
                bx = SkillFactory.getSkill(31110009);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    mpRecover += 1;
                    mpRecoverProp += eff.getProb();
                }
                // 黑暗拘束
                bx = SkillFactory.getSkill(31121006);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    ignoreTargetDEF += bx.getEffect(bof).getIgnoreMob();
                }
                // 惡魔狂斬最終強化
                bx = SkillFactory.getSkill(31120011);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(31000004, eff.getDAMRate());
                    addDamageIncrease(31001006, eff.getDAMRate());
                    addDamageIncrease(31001007, eff.getDAMRate());
                    addDamageIncrease(31001008, eff.getX());
                }
                // 進階武器精通
                bx = SkillFactory.getSkill(31120008);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    物理攻擊力 += eff.getAttackX();
                    trueMastery += eff.getMastery();
                    暴擊最小傷害 += eff.getCriticalMin();
                }
                // 堅硬肌膚
                bx = SkillFactory.getSkill(31120009);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    percent_wdef += bx.getEffect(bof).getT();
                }
            } else if (MapleJob.is惡魔復仇者(job)) {
                // TODO:轉換星盾 公式
                bx = SkillFactory.getSkill(30010232);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    // for (int i = 1 ; i <= starForce ; i++) {
                    // if (i < 20) {
                    // addmaxhp += 60;
                    // } else if (i == 20) {
                    // addmaxhp += 310;
                    // } else if (i < 30) {
                    // addmaxhp += 60;
                    // } else if (i == 31) {
                    // addmaxhp += 835;
                    // } else if (i < 64) {
                    // addmaxhp += 85;
                    // }
                    // }
                }
                // 效能提升
                bx = SkillFactory.getSkill(30010231);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    道具恢復效能提升 += eff.getX() - 100;
                }
                // 惡魔敏捷
                bx = SkillFactory.getSkill(31010003);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    移動速度 += eff.getPassiveSpeed();
                    最大移動速度 += eff.getSpeedMax();
                    跳躍力 += eff.getPassiveJump();
                    暴擊概率 += eff.getCr();
                }
                // 鋼鐵意志
                bx = SkillFactory.getSkill(31200004);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWDEFRate();
                    percent_mdef += eff.getMDEFRate();
                }
                // 魔劍精通
                bx = SkillFactory.getSkill(31200005);
                // 進階魔劍精通
                Skill bx2 = SkillFactory.getSkill(31220006);
                bof = chra.getPassiveLevel(bx);
                int bof2 = chra.getPassiveLevel(bx2);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    if (bof2 <= 0) {
                        trueMastery += eff.getMastery();
                    }
                }
                if (bof2 > 0) {
                    eff = bx.getEffect(bof);
                    trueMastery += eff.getMastery();
                    物理攻擊力 += eff.getAttackX();
                    暴擊最小傷害 += eff.getCriticalMin();
                }
                // 潛在力量
                bx = SkillFactory.getSkill(31200006);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    增加最大HP += eff.getMaxHpX();
                    防御力 += eff.getWDEFX();
                }
                // 超越苦痛
                bx = SkillFactory.getSkill(31210005);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    for (int i = 0; i < 5; i++) {
                        int[] aSkills = new int[]{
                            // 超越 : 十文字斬
                            31011004,
                            // 超越：惡魔風暴
                            31201007,
                            // 超越：月光斬
                            31211007,
                            // 超越 : 逆十文字斬
                            31221009,};
                        for (int nSkill : aSkills) {
                            if (i == 0) {
                                nSkill = nSkill / 100 * 100;
                            } else {
                                nSkill += i - 1;
                            }
                            addDamageIncrease(nSkill, eff.getDAMRate());
                        }
                    }
                }
                // 防衛技術
                bx = SkillFactory.getSkill(31220005);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    ignoreTargetDEF += eff.getIgnoreMob();
                    // 盾牌衝鋒
                    addDamageIncrease(31211002, eff.getX());
                    addDamageIncrease(31211011, eff.getX());
                    // 盾牌追擊
                    addDamageIncrease(31220051, eff.getX());
                    addDamageIncrease(31221001, eff.getX());
                    addDamageIncrease(31221004, eff.getX());
                }
                // 楓葉祝福
                bx = SkillFactory.getSkill(31221008);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                // 盾牌追擊
                bx = SkillFactory.getSkill(31221001);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    // 超越：月光斬
                    int nSkill = 31211007;
                    for (int i = 0; i < 5; i++) {
                        if (i == 0) {
                            nSkill = nSkill / 100 * 100;
                        } else {
                            nSkill += i - 1;
                        }
                        addDamageIncrease(nSkill, eff.getDAMRate());
                    }
                }
                // 防禦粉碎
                bx = SkillFactory.getSkill(31221002);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    // 超越：惡魔風暴
                    int nSkill = 31201007;
                    for (int i = 0; i < 5; i++) {
                        if (i == 0) {
                            nSkill = nSkill / 100 * 100;
                        } else {
                            nSkill += i - 1;
                        }
                        addDamageIncrease(nSkill, eff.info.get(MapleStatInfo.damPlus));
                    }
                }
                // 超越—強化威力
                bx = SkillFactory.getSkill(31220043);
                bof = chra.getPassiveLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    for (int i = 0; i < 5; i++) {
                        int[] aSkills = new int[]{
                            // 超越 : 十文字斬
                            31011004,
                            // 超越：惡魔風暴
                            31201007,
                            // 超越：月光斬
                            31211007,
                            // 超越 : 逆十文字斬
                            31221009,};
                        for (int nSkill : aSkills) {
                            if (i == 0) {
                                nSkill = nSkill / 100 * 100;
                            } else {
                                nSkill += i - 1;
                            }
                            addDamageIncrease(nSkill, eff.getDAMRate());
                        }
                    }
                }
            }
        } else if (MapleJob.is煉獄巫師(job)) {

            // 長杖精通一轉
            bx = SkillFactory.getSkill(32000015);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                魔法攻擊力 += eff.getMagicX();
                暴擊概率 += eff.getCr();
                防御力 += eff.getWDEFX();
            }

            // 黃色光環
            bx = SkillFactory.getSkill(32001016);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int add_speed = eff.getPassiveSpeed();
                int add_MaxSpeed = eff.getSpeedMax();
                int add_jump = eff.getPassiveJump();
                int add_ER = eff.getER();
                移動速度 += add_speed;
                最大移動速度 += add_MaxSpeed;
                閃避率 += add_ER;
                跳躍力 += add_jump;
            }

            // 智慧昇華
            bx = SkillFactory.getSkill(32100007);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部智力 += eff.getIntX();
            }

            // 普通轉換
            bx = SkillFactory.getSkill(32100008);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
            }
            // 紅色光環
            bx = SkillFactory.getSkill(32101009);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                recoverHP = eff.getHp();
            }
            // 戰鬥精通
            bx = SkillFactory.getSkill(32110001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Damage = eff.getDAMRate();
                dam_r += Damage;
                bossdam_r += Damage;
                暴擊最小傷害 += eff.getCriticalMin();
            }

            // 戰鬥衝刺
            bx = SkillFactory.getSkill(32111015);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Damage = eff.getDAMRate();
                addDamageIncrease(32101001, Damage);
            }

            // 神經刺激
            bx = SkillFactory.getSkill(32110019);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int add_speed = eff.getPassiveSpeed();
                int add_MaxSpeed = eff.getSpeedMax();
                int add_ER = eff.getER();
                移動速度 += add_speed;
                最大移動速度 += add_MaxSpeed;
                閃避率 += add_ER;
            }
            // 藍色繩索
            bx = SkillFactory.getSkill(32111012);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getASRRate();
                屬性抗性 += eff.getTERRate();
                percent_wdef += eff.getWDEFRate();
            }
            // 煉獄鬥氣
            bx = SkillFactory.getSkill(32121010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
            }

            // 進階黑色繩索
            bx = SkillFactory.getSkill(32120000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                魔法攻擊力 += eff.getMagicX();
            }
            // 技能加速
            bx = SkillFactory.getSkill(32120020);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
                percent_matk += eff.getPercentMATK();
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 黑暗世紀
            bx = SkillFactory.getSkill(32121004);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Damage = eff.getDAMRate();
                addDamageIncrease(32101001, Damage);
                addDamageIncrease(32111015, Damage);
                addDamageIncrease(32101001, Damage);
            }
            // 黑色光環
            bx = SkillFactory.getSkill(32121017);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_matk += eff.getPercentMATK();
            }
        } else if (MapleJob.is狂豹獵人(job)) {
            // 自動射擊設備
            bx = SkillFactory.getSkill(33000005);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getAttackX();
                percent_acc += eff.getPercentAcc();
                defRange += eff.getRange();
                移動速度 += eff.getSpeed();
            }
            // 自然的憤怒
            bx = SkillFactory.getSkill(33000034);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
            }
            // 體能訓練
            bx = SkillFactory.getSkill(33100010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部敏捷 += eff.getDexX();
                局部力量 += eff.getStrX();
            }
            // 美洲豹精通
            bx = SkillFactory.getSkill(33100014);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                格擋率 += eff.info.get(MapleStatInfo.stanceProp);
            }

            // 騎乘精通
            bx = SkillFactory.getSkill(33110000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Damage = eff.getDamage();
                dam_r += Damage;
                bossdam_r += Damage;
            }

            // 狂獸附體
            bx = SkillFactory.getSkill(33111007);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int addition = 0;
                if (chra.getTotalSkillLevel(33120044) > 0) { // 狂獸附體-最大 HP增加
                    addition = 10;
                }
                percent_hp += eff.getPercentHP() + addition;
            }

            // 迴避
            bx = SkillFactory.getSkill(33110008);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);

                局部敏捷 += eff.getDexX();
            }
            // 弩術精通
            bx = SkillFactory.getSkill(33120000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getX();
                trueMastery += eff.getMastery();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            // 狂暴天性
            bx = SkillFactory.getSkill(33120010);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
                percent_wdef += eff.getWDEFRate();
            }
            // 自然之力
            bx = SkillFactory.getSkill(33120015);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getASRRate();
                屬性抗性 += eff.getTERRate();
            }
        } else if (MapleJob.is機甲戰神(job)) {
            // 機甲戰神精通
            bx = SkillFactory.getSkill(35100000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                物理攻擊力 += bx.getEffect(bof).getAttackX();
            }
            // 體能訓練
            bx = SkillFactory.getSkill(35100011);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 機甲防禦系統
            bx = SkillFactory.getSkill(35110018);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
                防御力 += eff.getWDEFX();
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 金屬拳精通
            bx = SkillFactory.getSkill(35110014);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) { // ME-07 Drillhands, Atomic Hammer
                eff = bx.getEffect(bof);
                addDamageIncrease(35001003, eff.getDAMRate());
                addDamageIncrease(35101003, eff.getDAMRate());
            }
            // 合金盔甲終極
            bx = SkillFactory.getSkill(35120000);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                trueMastery += bx.getEffect(bof).getMastery();
            }
            // 機器人精通
            bx = SkillFactory.getSkill(35120001);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) { // Satellite
                eff = bx.getEffect(bof);
                addDamageIncrease(35111005, eff.getX());
                addDamageIncrease(35111011, eff.getX());
                addDamageIncrease(35121009, eff.getX());
                addDamageIncrease(35121010, eff.getX());
                addDamageIncrease(35121011, eff.getX());
                召喚獸持續時間提升 += eff.getY();
            }
            // 終極賽特拉特
            bx = SkillFactory.getSkill(35121006);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) { // Satellite
                eff = bx.getEffect(bof);
                addDamageIncrease(35111001, eff.getDAMRate());
                addDamageIncrease(35111009, eff.getDAMRate());
                addDamageIncrease(35111010, eff.getDAMRate());
            }
        } else if (MapleJob.is傑諾(job)) {
            // 效率管道
            bx = SkillFactory.getSkill(36101003);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                增加最大HP += eff.getMaxHpX();
                增加最大MP += eff.getMaxMpX();
            }
        } else if (MapleJob.is劍豪(job)) {
            // 天賦的才能
            bx = SkillFactory.getSkill(40010000);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int add_speed = eff.getPassiveSpeed();
                int add_MaxSpeed = eff.getU();
                int add_jump = eff.getPassiveJump();
                int add_ER = eff.getER();
                int add_Mastery = eff.getMastery();
                移動速度 += add_speed;
                最大移動速度 += add_MaxSpeed;
                trueMastery += add_Mastery;
                閃避率 += add_ER;
                跳躍力 += add_jump;
            }
            // 一刀兩斷
            bx = SkillFactory.getSkill(41120008);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Innore = eff.getIgnoreMob();
                int DMG = eff.getDAMRate();
                ignoreTargetDEF += Innore;
                dam_r += DMG;
                bossdam_r += DMG;
            }
            // 攻守兼備
            bx = SkillFactory.getSkill(40010067);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int DMG = eff.getDAMRate();
                dam_r += DMG;
                bossdam_r += DMG;
            }
            // 劍豪道
            bx = SkillFactory.getSkill(41000003);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            // 秘劍‧斑鳩
            bx = SkillFactory.getSkill(41100006);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊最小傷害 += eff.getCriticalMin();
                暴擊最大傷害 += eff.getCriticalMax();
            }
            // 柳身
            bx = SkillFactory.getSkill(41110006);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int avoid = eff.getY();
                閃避率 += avoid;
            }
            // 拔刀術‧心體技
            bx = SkillFactory.getSkill(41110008);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int CR = eff.getY();
                int ATX = eff.getAttackX();
                暴擊概率 += CR;
                物理攻擊力 += ATX;
                魔法攻擊力 += ATX;
            }
            // 鷹爪閃
            bx = SkillFactory.getSkill(41110008);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(41101001, eff.getDAMRate());
                addDamageIncrease(41101002, eff.getDAMRate());
                addDamageIncrease(41101003, eff.getDAMRate());
            }
            // 旋風斬
            bx = SkillFactory.getSkill(41110008);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(41101001, eff.getDAMRate());
                addDamageIncrease(41101002, eff.getDAMRate());
            }
        } else if (MapleJob.is陰陽師(job)) {
            // 五行的加護
            bx = SkillFactory.getSkill(40020000);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_HP = eff.getPercentHP();
                percent_hp += ADD_HP;
            }
            // 無限的靈力
            bx = SkillFactory.getSkill(40020001);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Mastery = eff.getMastery();
                trueMastery += Mastery;
            }
            // 紫扇傳授
            bx = SkillFactory.getSkill(40020002);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int DMG = eff.getDAMRate();
                dam_r += DMG;
                bossdam_r += DMG;
            }

            // 紫扇傳授2
            bx = SkillFactory.getSkill(40020110);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int DAM = eff.getDAMRate();
                dam_r += DAM;
                bossdam_r += DAM;
            }
            // 陰陽道
            bx = SkillFactory.getSkill(42000003);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部智力 += eff.getIntX();
                局部幸運 += eff.getLukX();
            }
            // 花狐的心意
            bx = SkillFactory.getSkill(42000008);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                移動速度 += eff.getPassiveSpeed();
            }

            // 紫扇仰波‧焰
            bx = SkillFactory.getSkill(42101004);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int DAM = eff.getDAMRate();
                addDamageIncrease(42001000, DAM);
            }
            // 紫扇仰波‧零
            bx = SkillFactory.getSkill(42111006);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int DMG = eff.getDAMRate();
                addDamageIncrease(42001000, DMG);
            }
            // 伏魔纏氣
            bx = SkillFactory.getSkill(42110009);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Mag = eff.getMagicX();
                int CR = eff.getCr();
                暴擊概率 += CR;
                魔法攻擊力 += Mag;
            }
            // 華扇
            bx = SkillFactory.getSkill(42110009);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int Mastery = eff.getMastery();
                int DMG = eff.getDAMRate();
                trueMastery += Mastery;
                dam_r += DMG;
                bossdam_r += DMG;
            }
        } else if (MapleJob.is米哈逸(job)) {
            // Mihile 1st Job Passive Skills
            bx = SkillFactory.getSkill(51000000); // Mihile || HP Boost
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                percent_hp += bx.getEffect(bof).getPercentHP();
            }
            bx = SkillFactory.getSkill(51000001); // Mihile || Soul Shield
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_wdef += eff.getX();
                percent_mdef += eff.getX();
            }
            bx = SkillFactory.getSkill(51000002); // Mihile || Soul Devotion
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                移動速度 += eff.getPassiveSpeed();
                跳躍力 += eff.getPassiveJump();
            }

            // Mihile 2nd Job Passive Skills
            bx = SkillFactory.getSkill(51100000); // Mihile || Physical Training
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(5001002, eff.getX());
                addDamageIncrease(5001003, eff.getY());
                局部力量 += eff.getStrX();
                局部敏捷 += eff.getDexX();
            }
            bx = SkillFactory.getSkill(51120002); // Mihile || Final Attack && Advanced Final Attack
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getAttackX();
                addDamageIncrease(51100002, eff.getDamage());
            }

            // Mihile 3rd Job Passive Skills
            bx = SkillFactory.getSkill(51110000); // Mihile || Self Recovery
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                hpRecoverProp += eff.getProb();
                hpRecover += eff.getX();
                mpRecoverProp += eff.getProb();
                mpRecover += eff.getX();
            }
            bx = SkillFactory.getSkill(51110001); // Mihile || Intense Focus
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部力量 += eff.getStrX();
                // Add Attack Speed here
            }
            bx = SkillFactory.getSkill(51110002); // Mihile || Righteous Indignation
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                異常抗性 += eff.getX();
                percent_atk += eff.getX();
                暴擊最小傷害 += eff.getCriticalMin();
            }

            // Mihile 4th Job Passive Skills
            bx = SkillFactory.getSkill(51120000); // Mihile || Combat Mastery
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            bx = SkillFactory.getSkill(51120001); // Mihile || Expert Sword Mastery
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += bx.getEffect(bof).getX();
                trueMastery += eff.getMastery();
                暴擊最小傷害 += eff.getCriticalMin();
            }
            bx = SkillFactory.getSkill(51120003); // Mihile || Soul Asylum
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                percent_wdef += bx.getEffect(bof).getT();
            }
        } else if (MapleJob.is凱撒(job)) {
            // 鋼鐵意志
            bx = SkillFactory.getSkill(60000222);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
            }
            // 鎧甲保護
            bx = SkillFactory.getSkill(61000003);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int WDEF = eff.getWDEFX();
                防御力 += WDEF;
                格擋率 += eff.getProb();
            }
            // 龍旋
            bx = SkillFactory.getSkill(61001002);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                移動速度 += eff.getPassiveSpeed();
                最大移動速度 += eff.getSpeedMax();
            }
            // 防禦模式
            bx = SkillFactory.getSkill(61100005);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int WDEF = eff.getWDEFX();
                防御力 += WDEF;
                percent_hp = eff.getPercentHP();
            }
            // 劍技專精
            bx = SkillFactory.getSkill(61100006);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                Item weapon = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (weapon != null) {
                    MapleWeaponType weaponType = ItemConstants.武器類型(weapon.getItemId());
                    if (weaponType == MapleWeaponType.雙手劍) {
                        trueMastery += eff.getMastery();
                    }
                }
            }
            // 戰力強化
            bx = SkillFactory.getSkill(61100007);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                局部力量 += eff.getStrX();
            }
            // 攻擊模式
            bx = SkillFactory.getSkill(61100008);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getAttackX();
                暴擊概率 += eff.getCr();
                bossdam_r += eff.getBossDamage();
            }
            // 進階劍龍連斬
            bx = SkillFactory.getSkill(61100009);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(61001000, eff.getDAMRate());
            }
            // 二階防禦模式
            bx = SkillFactory.getSkill(61110005);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int WDEF = eff.getWDEFX();
                防御力 += WDEF;
                percent_hp = eff.getPercentHP();
            }
            // 進階戰力強化
            bx = SkillFactory.getSkill(61110007);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getPercentHP();
                局部力量 += eff.getStrX();
            }
            // 二階攻擊模式
            bx = SkillFactory.getSkill(61110010);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getAttackX();
                暴擊概率 += eff.getCr();
                bossdam_r += eff.getBossDamage();
            }
            // 終極劍龍連斬
            bx = SkillFactory.getSkill(61110015);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(61001000, eff.getDAMRate());
            }
            // 三階防禦模式
            bx = SkillFactory.getSkill(61120010);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int WDEF = eff.getWDEFX();
                防御力 += WDEF;
                percent_hp = eff.getPercentHP();
            }
            // 勇氣
            bx = SkillFactory.getSkill(61120011);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                ignoreTargetDEF += eff.getIgnoreMob();
            }
            // 進階之劍精通
            bx = SkillFactory.getSkill(61120012);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                Item weapon = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (weapon != null) {
                    MapleWeaponType weaponType = ItemConstants.武器類型(weapon.getItemId());
                    if (weaponType == MapleWeaponType.雙手劍) {
                        trueMastery += eff.getMastery();
                        物理攻擊力 += eff.getAttackX();
                        暴擊最小傷害 += eff.getCriticalMin();
                    }
                }
            }
            // 三階攻擊模式
            bx = SkillFactory.getSkill(61120013);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                物理攻擊力 += eff.getAttackX();
                暴擊概率 += eff.getCr();
                bossdam_r += eff.getBossDamage();
            }
            // 最後的劍龍連斬
            bx = SkillFactory.getSkill(61120020);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                addDamageIncrease(61001000, eff.getDAMRate());
            }
        } else if (MapleJob.is天使破壞者(job)) {
            // 真正的繼承人
            bx = SkillFactory.getSkill(60010217);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                Item weapon = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (weapon != null) {
                    MapleWeaponType weaponType = ItemConstants.武器類型(weapon.getItemId());
                    if (weaponType == MapleWeaponType.靈魂射手) {
                        trueMastery += eff.getMastery();
                    }
                }
            }
            // 親和力 Ⅰ
            bx = SkillFactory.getSkill(65000003);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int SPM = eff.getSpeedMax();
                int JP = eff.getPassiveJump();
                int SP = eff.getSpeed();
                移動速度 += SP;
                跳躍力 += JP;
                最大移動速度 += SPM;
                ReChargeChance += eff.getX();
            }
            // 內在力量
            bx = SkillFactory.getSkill(65100004);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部敏捷 += eff.getDexX();
            }
            // 親和力 Ⅱ
            bx = SkillFactory.getSkill(65100004);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int ADD_ASR = eff.getASRRate();
                int ADD_TER = eff.getTERRate();
                異常抗性 += ADD_ASR;
                屬性抗性 += ADD_TER;
            }
            // 寧靜心靈
            bx = SkillFactory.getSkill(65110005);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                增加最大HP += eff.getMaxHpX();
                防御力 += eff.getWDEFX();
            }

            // 親和力 Ⅲ
            bx = SkillFactory.getSkill(65110006);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部敏捷 += eff.getDexX();
                dam_r += eff.getDAMRate();
                bossdam_r += eff.getDAMRate();
            }
            // 三位一體
            bx = SkillFactory.getSkill(65121101);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                int 攻擊 = eff.getDAMRate();
                dam_r += 攻擊;
                bossdam_r += 攻擊;
                ignoreTargetDEF += eff.getIgnoreMob();
            }
        } else if (MapleJob.is神之子(job)) {
            System.err.println("職業未處理被動技能:" + job);
        } else if (MapleJob.is幻獸師(job)) {
            bx = SkillFactory.getSkill(112000011); // Well Fed
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                局部智力 += eff.getIntX();
                percent_hp += bx.getEffect(bof).getPercentHP();
            }
            bx = SkillFactory.getSkill(112000010); // Dumb Luck
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                percent_wdef += eff.getWDEFRate();
                dam_r += eff.getDAMRate();
            }
            bx = SkillFactory.getSkill(112000015); // Fort Follow-Up
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                dam_r += eff.getDAMRate();
            }
            bx = SkillFactory.getSkill(112000014); // Bear Strength
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getCr();
                暴擊最小傷害 += eff.getCriticalMin();
                暴擊最大傷害 += eff.getCriticalMax();
                魔法攻擊力 += eff.getMagicX();
            }
            bx = SkillFactory.getSkill(112000013); // Fort the Brave
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                魔法攻擊力 += eff.getMagicX();
            }
            bx = SkillFactory.getSkill(112000020); // Billowing Trumpet
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                魔法攻擊力 += eff.getMagicX();
            }
        } else if (MapleJob.is皮卡啾(job)) {
            // 皮卡啾之力
            bx = SkillFactory.getSkill(131000014);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                暴擊概率 += eff.getY();
                暴擊概率 += Math.ceil((double) chra.getLevel() / eff.getZ());
                dam_r += chra.getLevel() * eff.getW();
                物理攻擊力 += Math.ceil(chra.getLevel() / 2.0D/* getS */) * eff.getV();
            }
            // 皮卡啾的品格
            bx = SkillFactory.getSkill(131000016);
            bof = chra.getPassiveLevel(bx);
            if (bof > 0) {
                eff = bx.getEffect(bof);
                移動速度 += eff.getPassiveSpeed();
                跳躍力 += eff.getPassiveJump();
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
                ignoreTargetDEF += eff.getIgnoreMob();
            }
        } else if (MapleJob.is凱內西斯(job)) {
            System.err.println("職業未處理被動技能:" + job);
        } else if (MapleJob.is冒險家(job)) {
        } else {
            System.err.println("職業未處理被動技能:" + job);
        }
    }

    public void handleHyperCoolDownReduce(MapleCharacter chr) {
        Skill bx;
        int bof;
        MapleStatEffect eff;

        switch (chr.getJob()) {
            case 122:
                // 鬼神之擊-冷卻減免
                bx = SkillFactory.getSkill(1220051);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(1221011, eff.getCooltimeReduceR());
                }
                break;
            case 132:
                // 轉生-冷卻減免
                bx = SkillFactory.getSkill(1320047);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(1320016, eff.getCooltimeReduceR());
                    addCoolTimeReduce(1320019, eff.getCooltimeReduceR());
                }
                break;
            case 212:
                // 地獄爆發-冷卻減免
                bx = SkillFactory.getSkill(2120051);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(2121003, eff.getCooltimeReduceR());
                }
                break;
            case 232:
                // 聖十字魔法盾-冷卻減免
                bx = SkillFactory.getSkill(2320045);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(2311009, eff.getCooltimeReduceR());
                }
                break;
            case 322:
                // 必殺狙擊-冷卻減免
                bx = SkillFactory.getSkill(3220051);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(3221007, eff.getCooltimeReduceR());
                }
                break;
            case 434:
                // 穢土轉生-冷卻減免
                bx = SkillFactory.getSkill(4340051);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(4341011, eff.getCooltimeReduceR());
                }
                break;
            case 1412:
                // 闇黑天魔-冷卻時間重置
                bx = SkillFactory.getSkill(14120046);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(14121003, eff.getCooltimeReduceR());
                }
                break;
            case 2112:
                // 鬥氣襲擊-減少冷卻時間
                bx = SkillFactory.getSkill(21120044);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(21111009, eff.getCooltimeReduceR());
                }
                break;
            case 2217:
                // 龍神之怒-減少冷卻時間
                bx = SkillFactory.getSkill(22170047);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(22181002, eff.getCooltimeReduceR());
                }
                break;
            case 2412:
                // 卡牌風暴-減少冷卻時間
                bx = SkillFactory.getSkill(24120044);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(24121005, eff.getCooltimeReduceR());
                }
                break;
            case 3212:
                // 黑暗閃電-冷卻減免
                bx = SkillFactory.getSkill(32120048);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(32110020, eff.getCooltimeReduceR());
                    addCoolTimeReduce(32111003, eff.getCooltimeReduceR());
                    addCoolTimeReduce(32111016, eff.getCooltimeReduceR());
                }

                // 颶風-冷卻減免
                bx = SkillFactory.getSkill(32120051);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(32121003, eff.getCooltimeReduceR());
                }
                // 黑暗世紀-冷卻減免
                bx = SkillFactory.getSkill(32120057);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(32121004, eff.getCooltimeReduceR());
                }
                // 魔法屏障-冷卻減免
                bx = SkillFactory.getSkill(32120063);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(32121006, eff.getCooltimeReduceR());
                }
                break;
            case 3312:
                // 召喚美洲豹-冷卻減免
                bx = SkillFactory.getSkill(33120048);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    // 全部代碼都是 召喚美洲豹 ~"~
                    addCoolTimeReduce(33001007, eff.getCooltimeReduceR());
                    addCoolTimeReduce(33001008, eff.getCooltimeReduceR());
                    addCoolTimeReduce(33001009, eff.getCooltimeReduceR());
                    addCoolTimeReduce(33001010, eff.getCooltimeReduceR());
                    addCoolTimeReduce(33001011, eff.getCooltimeReduceR());
                    addCoolTimeReduce(33001012, eff.getCooltimeReduceR());
                    addCoolTimeReduce(33001013, eff.getCooltimeReduceR());
                    addCoolTimeReduce(33001014, eff.getCooltimeReduceR());
                    addCoolTimeReduce(33001015, eff.getCooltimeReduceR());
                }
                break;
            case 3512:
                // 磁場-冷卻減免
                bx = SkillFactory.getSkill(35120045);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(35111002, eff.getCooltimeReduceR());
                }
                break;
            case 4112:
                // 一閃-減少冷卻時間
                bx = SkillFactory.getSkill(41120051);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(41121002, eff.getCooltimeReduceR());
                }
                break;
            case 4212:
                // 結界-桔梗-減少冷卻時間
                bx = SkillFactory.getSkill(42120050);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(42121005, eff.getCooltimeReduceR());
                }
                break;
            case 6512:
                // 魔力彩帶-減少冷卻時間
                bx = SkillFactory.getSkill(65120048);
                bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    eff = bx.getEffect(bof);
                    addCoolTimeReduce(65121002, eff.getCooltimeReduceR());
                }
                break;
        }
    }

    private void handleBuffStats(MapleCharacter chra) {
        MapleStatEffect eff = chra.getStatForBuff(MapleBuffStat.RideVehicle);
        if (eff != null && eff.getSourceId() == 33001001) { // jaguar
            暴擊概率 += eff.getW();
            percent_hp += eff.getZ();
        }
        Integer buff = chra.getBuffedValue(MapleBuffStat.Dice);
        if (buff != null) {
            percent_wdef += GameConstants.getDiceStat(buff, 2);
            percent_mdef += GameConstants.getDiceStat(buff, 2);
            percent_hp += GameConstants.getDiceStat(buff, 3);
            percent_mp += GameConstants.getDiceStat(buff, 3);
            暴擊概率 += GameConstants.getDiceStat(buff, 4);
            dam_r *= (GameConstants.getDiceStat(buff, 5) + 100.0) / 100.0;
            bossdam_r *= (GameConstants.getDiceStat(buff, 5) + 100.0) / 100.0;
            expBuff *= GameConstants.getDiceStat(buff, 6) / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.HayatoHPR);
        if (buff != null) {
            percent_hp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.HayatoMPR);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieMHPR);
        if (buff != null) {
            percent_hp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieMMPR);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DDR);
        if (buff != null) {
            percent_wdef += buff;
            percent_mdef += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.AsrR);
        if (buff != null) {
            異常抗性 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.TerR);
        if (buff != null) {
            屬性抗性 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.Infinity);
        if (buff != null) {
            percent_matk += buff - 1;
        }
        buff = chra.getBuffedValue(MapleBuffStat.OnixDivineProtection);
        if (buff != null) {
            閃避率 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.PVPDamage);
        if (buff != null) {
            pvpDamage += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.PVPDamageSkill);
        if (buff != null) {
            pvpDamage += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.BeastFormMaxHP);
        if (buff != null) {
            percent_hp += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.BLUE_AURA_OLD);
        if (eff != null) {
            percent_wdef += eff.getZ() + eff.getY();
            percent_mdef += eff.getZ() + eff.getY();
        }
        buff = chra.getBuffedValue(MapleBuffStat.Conversion);
        if (buff != null) {
            percent_hp += buff;
        } else {
            buff = chra.getBuffedValue(MapleBuffStat.MaxMP);
            if (buff != null) {
                percent_hp += buff;
            }
        }
        buff = chra.getBuffedValue(MapleBuffStat.MaxHP);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.HowlingEvasion);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MasterMagicOn);
        if (buff != null) {
            BUFF技能持續時間提升 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.STR);
        if (buff != null) {
            局部力量 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DEX);
        if (buff != null) {
            局部敏捷 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DEXR);
        if (buff != null) {
            局部敏捷 += (局部敏捷 * buff / 100);
        }
        buff = chra.getBuffedValue(MapleBuffStat.INT);
        if (buff != null) {
            局部智力 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.LUK);
        if (buff != null) {
            局部幸運 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieAllStat);
        if (buff != null) {
            局部力量 += buff;
            局部敏捷 += buff;
            局部智力 += buff;
            局部幸運 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.EPDD);
        if (buff != null) {
            防御力 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.PDD);
        if (buff != null) {
            防御力 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.BasicStatUp);
        if (buff != null) {
            final double d = buff.doubleValue() / 100.0;
            局部力量 += d * 力量; // base only
            局部敏捷 += d * 敏捷;
            局部幸運 += d * 幸運;
            局部智力 += d * 智力;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MaxLevelBuff);
        if (buff != null) {
            final double d = buff.doubleValue() / 100.0;
            物理攻擊力 += (int) (物理攻擊力 * d);
            魔法攻擊力 += (int) (魔法攻擊力 * d);
        }
        buff = chra.getBuffedValue(MapleBuffStat.ComboAbilityBuff);
        if (buff != null) {
            物理攻擊力 += buff / 10;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MesoGuard);
        if (buff != null) {
            mesoGuardMeso += buff.doubleValue();
        }
        buff = chra.getBuffedValue(MapleBuffStat.ExpBuffRate);
        if (buff != null) {
            expBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieEXP);
        if (buff != null) {
            indieExp *= (buff.doubleValue() + 100.0) / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ItemUpByItem);
        if (buff != null) {
            dropBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MesoUpByItem);
        if (buff != null) {
            mesoBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MesoUp);
        if (buff != null) {
            mesoBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndiePAD);
        if (buff != null) {
            物理攻擊力 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndiePADR);
        if (buff != null) {
            percent_atk += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.IndieMAD);
        if (buff != null) {
            魔法攻擊力 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.PAD);
        if (buff != null) {
            物理攻擊力 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.SpiritLink);
        if (buff != null) {
            暴擊概率 += buff;
            dam_r *= (buff + 100.0) / 100.0;
            bossdam_r *= (buff + 100.0) / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.EPAD);
        if (buff != null) {
            物理攻擊力 += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.BattlePvP_Helena_Mark);
        if (eff != null) {
            物理攻擊力 += eff.getWatk();
        }
        buff = chra.getBuffedValue(MapleBuffStat.MAD);
        if (buff != null) {
            魔法攻擊力 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.Speed);
        if (buff != null) {
            移動速度 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.Jump);
        if (buff != null) {
            跳躍力 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DashSpeed);
        if (buff != null) {
            移動速度 += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DashJump);
        if (buff != null) {
            跳躍力 += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.AmplifyDamage);
        if (eff != null) {
            暴擊概率 = 100; // INTENSE
            異常抗性 = 100; // INTENSE

            防御力 += eff.getX();
            物理攻擊力 += eff.getX();
            魔法攻擊力 += eff.getX();
        }
        buff = chra.getBuffedValue(MapleBuffStat.HowlingAttackDamage);
        if (buff != null) {
            dam_r *= (buff.doubleValue() + 100.0) / 100.0;
            bossdam_r *= (buff.doubleValue() + 100.0) / 100.0;
        }
        buff = chra.getBuffedSkill_Y(MapleBuffStat.FinalCut);
        if (buff != null) {
            dam_r *= buff.doubleValue() / 100.0;
            bossdam_r *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedSkill_Y(MapleBuffStat.OWL_SPIRIT);
        if (buff != null) {
            dam_r *= buff.doubleValue() / 100.0;
            bossdam_r *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.DojangBerserk);
        if (buff != null) {
            dam_r *= buff.doubleValue() / 100.0;
            bossdam_r *= buff.doubleValue() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.Bless);
        if (eff != null) {
            物理攻擊力 += eff.getX();
            魔法攻擊力 += eff.getY();
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.Concentration);
        if (buff != null) {
            mpconReduce += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.AdvancedBless);
        if (eff != null) {
            物理攻擊力 += eff.getX();
            魔法攻擊力 += eff.getY();
            mpconReduce += eff.getMPConReduce();
        }
        eff = chra.getStatForBuff(MapleBuffStat.MagicResistance);
        if (eff != null) {
            異常抗性 += eff.getX();
        }

        eff = chra.getStatForBuff(MapleBuffStat.ComboCounter);
        buff = chra.getBuffedValue(MapleBuffStat.ComboCounter);
        if (eff != null && buff != null) {
            dam_r *= ((100.0 + ((eff.getV() + eff.getDAMRate()) * (buff - 1))) / 100.0);
            bossdam_r *= ((100.0 + ((eff.getV() + eff.getDAMRate()) * (buff - 1))) / 100.0);
        }
        eff = chra.getStatForBuff(MapleBuffStat.SUMMON);
        if (eff != null) {
            if (eff.getSourceId() == 35121010) { // amp
                dam_r *= (eff.getX() + 100.0) / 100.0;
                bossdam_r *= (eff.getX() + 100.0) / 100.0;
            }
        }
        eff = chra.getStatForBuff(MapleBuffStat.DARK_AURA_OLD);
        if (eff != null) {
            dam_r *= (eff.getX() + 100.0) / 100.0;
            bossdam_r *= (eff.getX() + 100.0) / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.BODY_BOOST);
        if (eff != null) {
            dam_r *= (eff.getV() + 100.0) / 100.0;
            bossdam_r *= (eff.getV() + 100.0) / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.Beholder);
        if (eff != null) {
            trueMastery += eff.getMastery();
        }
        eff = chra.getStatForBuff(MapleBuffStat.Mechanic);
        if (eff != null) {
            暴擊概率 += eff.getCr();
        }
        eff = chra.getStatForBuff(MapleBuffStat.ExpBuffRate);
        if (eff != null && eff.getBerserk() > 0) {
            dam_r *= eff.getBerserk() / 100.0;
            bossdam_r *= eff.getBerserk() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.WeaponCharge);
        if (eff != null) {
            dam_r *= eff.getDamage() / 100.0;
            bossdam_r *= eff.getDamage() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.PickPocket);
        if (eff != null) {
            pickRate = eff.getProb();
        }
        eff = chra.getStatForBuff(MapleBuffStat.DamR);
        if (eff != null) {
            dam_r *= (eff.getDAMRate() + 100.0) / 100.0;
            bossdam_r *= (eff.getDAMRate() + 100.0) / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.AssistCharge);
        if (eff != null) {
            dam_r *= eff.getDamage() / 100.0;
            bossdam_r *= eff.getDamage() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.HideAttack);
        if (eff != null) {
            dam_r *= eff.getDamage() / 100.0;
            bossdam_r *= eff.getDamage() / 100.0;
        }
        eff = chra.getStatForBuff(MapleBuffStat.BlessingArmor);
        if (eff != null) {
            物理攻擊力 += eff.getEnhancedWatk();
        }
        buff = chra.getBuffedSkill_Y(MapleBuffStat.DarkSight);
        if (buff != null) {
            dam_r *= (buff + 100.0) / 100.0;
            bossdam_r *= (buff + 100.0) / 100.0;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.Enrage);
        if (buff != null) {
            dam_r *= (buff + 100.0) / 100.0;
            bossdam_r *= (buff + 100.0) / 100.0;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.CombatOrders);
        if (buff != null) {
            combatOrders += buff;
        }
        eff = chra.getStatForBuff(MapleBuffStat.SharpEyes);
        if (eff != null) {
            暴擊概率 += eff.getX();
            暴擊最大傷害 += eff.getY();
        }
        buff = chra.getBuffedValue(MapleBuffStat.HowlingDefence);
        if (buff != null) {
            暴擊概率 += buff;
        }
        if (移動速度 > 140) {
            移動速度 = 140;
        }
        if (跳躍力 > 123) {
            跳躍力 = 123;
        }
        buff = chra.getBuffedValue(MapleBuffStat.RideVehicle);
        if (buff != null) {
            跳躍力 = 120;
            switch (buff) {
                case 1:
                    移動速度 = 150;
                    break;
                case 2:
                    移動速度 = 170;
                    break;
                case 3:
                    移動速度 = 180;
                    break;
                default:
                    移動速度 = 200; // lol
                    break;
            }
        }
        eff = chra.getStatForBuff(MapleBuffStat.DispelItemOptionByField);
        if (eff != null) {
            物理攻擊力 = Integer.MAX_VALUE;
        }
    }

    public boolean checkEquipLevels(final MapleCharacter chr, long gain) {
        if (chr.isClone()) {
            return false;
        }
        boolean changed = false;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Equip> all = new ArrayList<>(equipLevelHandling);
        for (Equip eq : all) {
            int lvlz = eq.getEquipLevel();
            eq.setItemEXP(Math.min(eq.getItemEXP() + gain, Long.MAX_VALUE));

            if (eq.getEquipLevel() > lvlz) { // lvlup
                for (int i = eq.getEquipLevel() - lvlz; i > 0; i--) {
                    // now for the equipment increments...
                    final Map<Integer, Map<String, Integer>> inc = ii.getEquipIncrements(eq.getItemId());
                    int extra = eq.getYggdrasilWisdom();
                    switch (extra) {
                        case 1:
                            inc.get(lvlz + i).put("STRMin", 1);
                            inc.get(lvlz + i).put("STRMax", 3);
                            break;
                        case 2:
                            inc.get(lvlz + i).put("DEXMin", 1);
                            inc.get(lvlz + i).put("DEXMax", 3);
                            break;
                        case 3:
                            inc.get(lvlz + i).put("INTMin", 1);
                            inc.get(lvlz + i).put("INTMax", 3);
                            break;
                        case 4:
                            inc.get(lvlz + i).put("LUKMin", 1);
                            inc.get(lvlz + i).put("LUKMax", 3);
                            break;
                        default:
                            break;
                    }
                    if (inc != null && inc.containsKey(lvlz + i)) { // flair = 1
                        eq = ii.levelUpEquip(eq, inc.get(lvlz + i));
                    }
                    // UGH, skillz
                    if (GameConstants.getStatFromWeapon(eq.getItemId()) == null
                            && GameConstants.getMaxLevel(eq.getItemId()) < (lvlz + i) && Math.random() < 0.1
                            && eq.getIncSkill() <= 0 && ii.getEquipSkills(eq.getItemId()) != null) {
                        for (int zzz : ii.getEquipSkills(eq.getItemId())) {
                            final Skill skil = SkillFactory.getSkill(zzz);
                            if (skil != null && skil.canBeLearnedBy(chr.getJob())) { // dont go over masterlevel :D
                                eq.setIncSkill(skil.getId());
                                chr.dropMessage(5, "Your skill has gained a levelup: " + skil.getName() + " +1");
                            }
                        }
                    }
                }
                changed = true;
            }
            chr.forceReAddItem(eq.copy());
        }
        if (changed) {
            chr.equipChanged();
            chr.getClient().getSession().writeAndFlush(EffectPacket.showItemLevelupEffect());
            chr.getMap().broadcastMessage(chr, EffectPacket.showItemLevelupEffect(chr), false);
        }
        return changed;
    }

    public boolean checkEquipDurabilitys(final MapleCharacter chr, int gain) {
        return checkEquipDurabilitys(chr, gain, false);
    }

    public boolean checkEquipDurabilitys(final MapleCharacter chr, int gain, boolean aboveZero) {
        if (chr.isClone() || chr.inPVP()) {
            return true;
        }
        List<Equip> all = new ArrayList<>(durabilityHandling);
        for (Equip item : all) {
            if (item != null && ((item.getPosition() >= 0) == aboveZero)) {
                item.setDurability(item.getDurability() + gain);
                if (item.getDurability() < 0) { // shouldnt be less than 0
                    item.setDurability(0);
                }
            }
        }
        for (Equip eqq : all) {
            if (eqq != null && eqq.getDurability() == 0 && eqq.getPosition() < 0) { // > 0 went to negative
                if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    chr.getClient().getSession().writeAndFlush(InventoryPacket.getInventoryFull());
                    chr.getClient().getSession().writeAndFlush(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                durabilityHandling.remove(eqq);
                final short pos = chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot();
                MapleInventoryManipulator.unequip(chr.getClient(), eqq.getPosition(), pos);
            } else if (eqq != null) {
                chr.forceReAddItem(eqq.copy());
            }
        }
        return true;
    }

    private void CalcPassive_SharpEye(final MapleCharacter player) {
        Skill critSkill;
        int critlevel;
        if (MapleJob.is惡魔殺手(player.getJob())) {
            critSkill = SkillFactory.getSkill(30010022);
            critlevel = player.getTotalSkillLevel(critSkill);
            if (critlevel > 0) {
                暴擊概率 += critSkill.getEffect(critlevel).getProb();
                this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
            }
        } else if (MapleJob.is精靈遊俠(player.getJob())) {
            critSkill = SkillFactory.getSkill(20020022);
            critlevel = player.getTotalSkillLevel(critSkill);
            if (critlevel > 0) {
                暴擊概率 += critSkill.getEffect(critlevel).getProb();
                this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
            }
        } else if (MapleJob.is末日反抗軍(player.getJob())) {
            critSkill = SkillFactory.getSkill(30000022);
            critlevel = player.getTotalSkillLevel(critSkill);
            if (critlevel > 0) {
                暴擊概率 += critSkill.getEffect(critlevel).getProb();
                this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
            }
        }
        switch (player.getJob()) { // Apply passive Critical bonus
            case 410: // Assasin
            case 411: // Hermit
            case 412: { // Night Lord
                critSkill = SkillFactory.getSkill(4100001); // Critical Throw
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    暴擊概率 += (short) (critSkill.getEffect(critlevel).getProb());
                    暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 2412: { // Phantom
                critSkill = SkillFactory.getSkill(24120006);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                    this.物理攻擊力 += critSkill.getEffect(critlevel).getAttackX();
                }
                break;
            }
            case 1410:
            case 1411:
            case 1412: { // Night Walker
                critSkill = SkillFactory.getSkill(14100001);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getProb());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 3100:
            case 3110:
            case 3111:
            case 3112: {
                critSkill = SkillFactory.getSkill(31100006);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getCr());
                    this.物理攻擊力 += critSkill.getEffect(critlevel).getAttackX();
                }
                break;
            }
            case 2300:
            case 2310:
            case 2311:
            case 2312: {
                critSkill = SkillFactory.getSkill(23000003);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getCr());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 3210:
            case 3211:
            case 3212: {
                critSkill = SkillFactory.getSkill(32100006);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getCr());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 434: {
                critSkill = SkillFactory.getSkill(4340010);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getProb());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 520:
            case 521:
            case 522: {
                critSkill = SkillFactory.getSkill(5200007);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getCr());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 1211:
            case 1212: {
                critSkill = SkillFactory.getSkill(12110000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getCr());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 530:
            case 531:
            case 532: {
                critSkill = SkillFactory.getSkill(5300004);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getCr());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 500:
            case 510:
            case 511:
            case 512: { // Buccaner, Viper
                critSkill = SkillFactory.getSkill(5110000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) critSkill.getEffect(critlevel).getProb();
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                // 爆擊精通
                critSkill = SkillFactory.getSkill(5000007);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) critSkill.getEffect(critlevel).getCr();
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                // 衝壓暴擊
                critSkill = SkillFactory.getSkill(5100008);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) critSkill.getEffect(critlevel).getCr();
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                // final Skill critSkill2 = SkillFactory.getSkill(5100008);
                // final int critlevel2 = player.getTotalSkillLevel(critSkill);
                // if (critlevel2 > 0) {
                // this.passive_sharpeye_rate += (short)
                // critSkill2.getEffect(critlevel2).getCr();
                // this.passive_sharpeye_min_percent +=
                // critSkill2.getEffect(critlevel2).getCriticalMin();
                // }
                break;
            }
            case 1511:
            case 1512: {
                critSkill = SkillFactory.getSkill(15110000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getProb());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 2111:
            case 2112: {
                critSkill = SkillFactory.getSkill(21110000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) ((critSkill.getEffect(critlevel).getX() * critSkill.getEffect(critlevel).getY())
                            + critSkill.getEffect(critlevel).getCr());
                }
                break;
            }
            case 300:
            case 310:
            case 311:
            case 312:
            case 320:
            case 321:
            case 322: { // Bowman
                critSkill = SkillFactory.getSkill(3000001);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getProb());
                }
                break;
            }
            case 1300:
            case 1310:
            case 1311:
            case 1312: { // Bowman
                critSkill = SkillFactory.getSkill(13000000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getProb());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 2214:
            case 2215:
            case 2216:
            case 2217:
            case 2218: { // Evan
                critSkill = SkillFactory.getSkill(22140000);
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.暴擊概率 += (short) (critSkill.getEffect(critlevel).getProb());
                    this.暴擊最小傷害 += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
        }
    }

    private void CalcPassive_Mastery(final MapleCharacter player) {
        if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11) == null) {
            passive_mastery = 0;
            return;
        }
        final int skil;
        final MapleWeaponType weaponType = ItemConstants
                .武器類型(player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11).getItemId());
        trueMastery += weaponType.getBaseMastery();

        if (MapleJob.is皮卡啾(player.getJob())) {
            if (player.getSkillLevel(131000014) <= 0) {
                passive_mastery = 0;
                return;
            }
            final MapleStatEffect eff = SkillFactory.getSkill(131000014)
                    .getEffect(player.getTotalSkillLevel(131000014));
            switch (weaponType) {
                case 單手劍:
                case 單手斧:
                case 單手棍:
                case 雙手斧:
                case 雙手劍:
                case 雙手棍:
                case 槍:
                case 矛:
                    passive_mastery = (byte) eff.getU();
                    break;
            }
            trueMastery = passive_mastery;
            return;
        }

        boolean padR = false;
        boolean acc = true;
        switch (weaponType) {
            case 閃亮克魯:
                if (player.getSkillLevel(27120007) > 0) {
                    skil = 27120007;
                } else {
                    skil = 27100005;
                }
                break;
            case 靈魂射手:
                if (player.getSkillLevel(65120005) > 0) {
                    skil = 65120005;
                } else {
                    skil = 65100003;
                }
                break;
            case 魔劍:
                if (player.getSkillLevel(31220006) > 0) {
                    skil = 31220006;
                } else {
                    skil = 31200005;
                }
                break;
            case 弓:
                skil = MapleJob.is皇家騎士團(player.getJob()) ? 13100000 : 3100000;
                break;
            case 拳套:
                skil = (MapleJob.is蒼龍俠客(player.getJob()) ? 5710022 : 4100000);
                padR = true;
                break;
            case 手杖:
                skil = player.getTotalSkillLevel(24120006) > 0 ? 24120006 : 24100004;
                break;
            case 加農炮:
                skil = 5300005;
                break;
            case 短劍:
            case 雙刀:
                skil = player.getJob() >= 430 && player.getJob() <= 434 ? 4300000 : 4200000;
                break;
            case 弩:
                skil = MapleJob.is末日反抗軍(player.getJob()) ? 33100000 : 3200000;
                break;
            case 單手斧:
            case 單手棍:
                skil = MapleJob.is末日反抗軍(player.getJob()) ? 31100004
                        : (MapleJob.is皇家騎士團(player.getJob()) ? 11100000 : (player.getJob() > 112 ? 1200000 : 1100000)); // hero/pally
                break;
            case 雙手斧:
            case 單手劍:
            case 雙手劍:
            case 雙手棍:
                skil = MapleJob.is皇家騎士團(player.getJob()) ? 11100000 : (player.getJob() > 112 ? 1200000 : 1100000); // hero/pally
                break;
            case 矛:
                skil = MapleJob.is狂狼勇士(player.getJob()) ? 21100000 : 1300000;
                break;
            case 槍:
                skil = (MapleJob.is蒼龍俠客(player.getJob()) ? 5710022 : 1300000);
                padR = true;
                break;
            case 指虎:
                skil = MapleJob.is皇家騎士團(player.getJob()) ? 15100001 : 5100001;
                break;
            case 火槍:
                skil = MapleJob.is末日反抗軍(player.getJob()) ? 35100000
                        : (MapleJob.is蒼龍俠客(player.getJob()) ? 5700000 : 5200000);
                break;
            case 雙弩槍:
                skil = 23100005;
                break;
            case 長杖:
            case 短杖:
                acc = false;
                skil = MapleJob.is末日反抗軍(player.getJob()) ? 32100006
                        : (player.getJob() <= 212 ? 2100006
                        : (player.getJob() <= 222 ? 2200006
                        : (player.getJob() <= 232 ? 2300006
                        : (player.getJob() <= 2000 ? 12100007 : 22120002))));
                break;
            default:
                passive_mastery = 0;
                return;
        }
        if (player.getSkillLevel(skil) <= 0) {
            passive_mastery = 0;
            return;
        }
        final MapleStatEffect eff = SkillFactory.getSkill(skil).getEffect(player.getTotalSkillLevel(skil));
        if (acc) {
            if (!padR && (skil != 1300000 || skil != 4100000)) {
                if (skil == 35100000) {
                    物理攻擊力 += eff.getX();
                }
            } else {
                percent_atk += eff.getAccR();
            }
        } else {
            魔法攻擊力 += eff.getX();
        }
        暴擊概率 += eff.getCr();
        passive_mastery = (byte) eff.getMastery();
        trueMastery += passive_mastery;
    }

    private void calculateFame(final MapleCharacter player) {
        player.getTrait(MapleTraitType.charm).addLocalExp(player.getFame());
        for (MapleTraitType t : MapleTraitType.values()) {
            player.getTrait(t).recalcLevel();
        }
    }

    public final short getMinCritDamage() {
        return (short) Math.min(暴擊最小傷害, 暴擊最大傷害);
    }

    public final short getMaxCritDamage() {
        return (short) Math.max(暴擊最小傷害, 暴擊最大傷害);
    }

    public final short getCritRate() {
        return 暴擊概率;
    }

    public final byte passive_mastery() {
        return passive_mastery; // * 5 + 10 for mastery %
    }

    public final void calculateMaxBaseDamage(final int watk, final int pvpDamage, MapleCharacter chra) {
        if (watk <= 0) {
            localmaxbasedamage = 1;
            localmaxbasepvpdamage = 1;
        } else {
            final Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            final int job = chra.getJob();
            final MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.沒有武器
                    : ItemConstants.武器類型(weapon_item.getItemId());
            int stat, statpvp;
            final boolean mage = MapleJob.is法師(job);
            switch (weapon) {
                case 能量劍:
                    stat = 4 * (局部力量 + 局部敏捷 + 局部幸運);
                    statpvp = 4 * (力量 + 敏捷 + 幸運);
                    break;
                case 靈魂射手:
                case 弓:
                case 弩:
                case 火槍:
                case 雙弩槍:
                    stat = 4 * 局部敏捷 + 局部力量;
                    statpvp = 4 * +敏捷 + 力量;
                    break;
                case 短劍:
                    stat = 4 * 局部幸運 + 局部敏捷;
                    statpvp = 4 * +幸運 + 敏捷;
                    if (MapleJob.is盜賊(job)) {
                        stat = 局部力量;
                        statpvp = 力量;
                    }
                    break;
                case 拳套:
                case 手杖:
                    stat = 4 * 局部幸運 + 局部敏捷;
                    statpvp = 4 * +幸運 + 敏捷;
                    break;
                case 魔劍:
                    stat = 局部最大HP / 7 + 局部力量;
                    statpvp = maxhp / 7 + 力量;
                    break;
                default:
                    if (mage) {
                        stat = 4 * 局部智力 + 局部幸運;
                        statpvp = 4 * +智力 + 幸運;
                    } else {
                        stat = 4 * 局部力量 + 局部敏捷;
                        statpvp = 4 * +力量 + 敏捷;
                    }
                    break;
            }
            localmaxbasedamage = weapon.getMaxDamageMultiplier(job) * stat * watk / 100.0f;
            localmaxbasepvpdamage = weapon.getMaxDamageMultiplier(job) * statpvp * (100.0f + (pvpDamage / 100.0f));
            localmaxbasepvpdamageL = weapon.getMaxDamageMultiplier(job) * stat * (100.0f + (pvpDamage / 100.0f));
            localmaxbasedamage *= dam_r / 100.0D;
            localmaxbasepvpdamage *= dam_r / 100.0D;
            localmaxbasepvpdamageL *= dam_r / 100.0D;
        }
    }

    public final float getHealHP() {
        return shouldHealHP;
    }

    public final float getHealMP() {
        return shouldHealMP;
    }

    public final void relocHeal(MapleCharacter chra) {
        if (chra.isClone()) {
            return;
        }
        final int playerjob = chra.getJob();

        shouldHealHP = 10 + recoverHP; // Reset
        shouldHealMP = MapleJob.is惡魔殺手(chra.getJob()) ? 0 : (3 + recoverMP + (局部智力 / 10)); // i think
        mpRecoverTime = 0;
        hpRecoverTime = 0;
        if (playerjob == 111 || playerjob == 112) {
            final Skill effect = SkillFactory.getSkill(1110000); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                MapleStatEffect eff = effect.getEffect(lvl);
                if (eff.getHp() > 0) {
                    shouldHealHP += eff.getHp();
                    hpRecoverTime = 4000;
                }
                shouldHealMP += eff.getMp();
                mpRecoverTime = 4000;
            }

        } else if (playerjob == 1111 || playerjob == 1112) {
            final Skill effect = SkillFactory.getSkill(11110000); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealMP += effect.getEffect(lvl).getMp();
                mpRecoverTime = 4000;
            }
        } else if (MapleJob.is精靈遊俠(playerjob)) {
            final Skill effect = SkillFactory.getSkill(20020109); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealHP += (effect.getEffect(lvl).getX() * 局部最大HP) / 100;
                hpRecoverTime = 4000;
                shouldHealMP += (effect.getEffect(lvl).getX() * 局部最大MP) / 100;
                mpRecoverTime = 4000;
            }
        } else if (MapleJob.is蒼龍俠客(playerjob) && playerjob != 508) {
            final Skill effect = SkillFactory.getSkill(5700005); // Perseverance
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                final MapleStatEffect eff = effect.getEffect(lvl);
                shouldHealHP += eff.getX();
                shouldHealMP += eff.getX();
                hpRecoverTime = eff.getY();
                mpRecoverTime = eff.getY();
            }
        } else if (playerjob == 3111 || playerjob == 3112) {
            final Skill effect = SkillFactory.getSkill(31110009); // Improving MP Recovery
            final int lvl = chra.getSkillLevel(effect);
            if (lvl > 0) {
                shouldHealMP += effect.getEffect(lvl).getY();
                mpRecoverTime = 4000;
            }
        }
        if (chra.getChair() != 0) { // Is sitting on a chair.
            shouldHealHP += 99; // Until the values of Chair heal has been fixed,
            shouldHealMP += 99; // MP is different here, if chair data MP = 0, heal + 1.5
        } else if (chra.getMap() != null) { // Because Heal isn't multipled when there's a chair :)
            final float recvRate = chra.getMap().getRecoveryRate();
            if (recvRate > 0) {
                shouldHealHP *= recvRate;
                shouldHealMP *= recvRate;
            }
        }
    }

    public final void connectData(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeShort(力量);
        mplew.writeShort(敏捷);
        mplew.writeShort(智力);
        mplew.writeShort(幸運);
        mplew.writeInt(hp);
        mplew.writeInt(maxhp);
        mplew.writeInt(mp);
        mplew.writeInt(maxmp);
    }

    public final void zeroData(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
        mplew.writeInt(0);
        mplew.write(0xFF);
        mplew.write(0);
        mplew.writeInt(maxhp);
        mplew.writeInt(maxmp);
        mplew.write(0);
        mplew.writeInt(chr.getSecondHair());
        mplew.writeInt(chr.getSecondFace());
        mplew.writeInt(maxhp);
        mplew.writeInt(maxmp);
    }

    private final static int[] allJobs = {0, 10000, 10000000, 20000000, 20010000, 20020000, 20030000, 20040000,
        20050000, 30000000, 30010000, 50000000};
    public final static int[] pvpSkills = {1000007, 2000007, 3000006, 4000010, 5000006, 5010004, 11000006, 12000006,
        13000005, 14000006, 15000005, 21000005, 22000002, 23000004, 31000005, 32000012, 33000004, 35000005};

    public static int getSkillByJob(final int skillID, final int job) { // test
        return skillID + (MapleJob.getBeginner((short) job) * 10000);
    }

    public final int getSkillIncrement(final int skillID) {
        if (skillsIncrement.containsKey(skillID)) {
            return skillsIncrement.get(skillID);
        }
        return 0;
    }

    public final int getElementBoost(final Element key) {
        if (elemBoosts.containsKey(key)) {
            return elemBoosts.get(key);
        }
        return 0;
    }

    public final int getDamageIncrease(final int key) {
        if (damageIncrease.containsKey(key)) {
            return damageIncrease.get(key) + damX;
        }
        return damX;
    }

    public final int getStarForce() {
        return 星光能量;
    }

    public void heal_noUpdate(MapleCharacter chra) {
        setHp(getCurrentMaxHp(), chra);
        setMp(getCurrentMaxMp(), chra);
    }

    public void heal(MapleCharacter chra) {
        heal_noUpdate(chra);
        chra.updateSingleStat(MapleStat.HP, getCurrentMaxHp());
        chra.updateSingleStat(MapleStat.MP, getCurrentMaxMp());
    }

    public Pair<Integer, Integer> handleEquipAdditions(MapleItemInformationProvider ii, MapleCharacter chra,
            boolean first_login, Map<Skill, SkillEntry> sData, final int itemId) {
        final List<Triple<String, String, String>> additions = ii.getEquipAdditions(itemId);
        if (additions == null) {
            return null;
        }
        int localmaxhp_x = 0, localmaxmp_x = 0;
        int skillid = 0, skilllevel = 0;
        String craft, job, level;
        for (final Triple<String, String, String> add : additions) {
            if (add.getMid().contains("con")) {
                continue;
            }
            final int right = Integer.parseInt(add.getRight());
            switch (add.getLeft()) {
                case "elemboost":
                    craft = ii.getEquipAddReqs(itemId, add.getLeft(), "craft");
                    if (add.getMid().equals("elemVol") && (craft == null || craft != null
                            && chra.getTrait(MapleTraitType.craft).getLocalExp() >= Integer.parseInt(craft))) {
                        int value = Integer.parseInt(add.getRight().substring(1, add.getRight().length()));
                        final Element key = Element.getFromChar(add.getRight().charAt(0));
                        if (elemBoosts.get(key) != null) {
                            value += elemBoosts.get(key);
                        }
                        elemBoosts.put(key, value);
                    }
                    break;
                case "mobcategory": // skip the category, thinkings too expensive to have yet another Map<Integer,
                    // Integer> for damage calculations
                    if (add.getMid().equals("damage")) {
                        dam_r *= (right + 100.0) / 100.0;
                        bossdam_r += (right + 100.0) / 100.0;
                    }
                    break;
                case "critical": // lv critical lvl?
                    boolean canJob = false,
                     canLevel = false;
                    job = ii.getEquipAddReqs(itemId, add.getLeft(), "job");
                    if (job != null) {
                        if (job.contains(",")) {
                            final String[] jobs = job.split(",");
                            for (final String x : jobs) {
                                if (chra.getJob() == Integer.parseInt(x)) {
                                    canJob = true;
                                }
                            }
                        } else if (chra.getJob() == Integer.parseInt(job)) {
                            canJob = true;
                        }
                    }
                    level = ii.getEquipAddReqs(itemId, add.getLeft(), "level");
                    if (level != null) {
                        if (chra.getLevel() >= Integer.parseInt(level)) {
                            canLevel = true;
                        }
                    }
                    if ((job != null && canJob || job == null) && (level != null && canLevel || level == null)) {
                        switch (add.getMid()) {
                            case "prob":
                                暴擊概率 += right;
                                break;
                            case "damage":
                                暴擊最小傷害 += right;
                                暴擊最大傷害 += right; // ???CONFIRM - not sure if this is max or minCritDmg
                                break;
                        }
                    }
                    break;
                case "boss": // ignore prob, just add
                    craft = ii.getEquipAddReqs(itemId, add.getLeft(), "craft");
                    if (add.getMid().equals("damage") && (craft == null || craft != null
                            && chra.getTrait(MapleTraitType.craft).getLocalExp() >= Integer.parseInt(craft))) {
                        bossdam_r *= (right + 100.0) / 100.0;
                    }
                    break;
                case "mobdie": // lv, hpIncRatioOnMobDie, hpRatioProp, mpIncRatioOnMobDie, mpRatioProp, modify
                    // =D, don't need mob to die
                    craft = ii.getEquipAddReqs(itemId, add.getLeft(), "craft");
                    if ((craft == null || craft != null
                            && chra.getTrait(MapleTraitType.craft).getLocalExp() >= Integer.parseInt(craft))) {
                        switch (add.getMid()) {
                            case "hpIncOnMobDie":
                                hpRecover += right;
                                hpRecoverProp += 5;
                                break;
                            case "mpIncOnMobDie":
                                mpRecover += right;
                                mpRecoverProp += 5;
                                break;
                        }
                    }
                    break;
                case "skill": // all these are additional skills
                    if (first_login) {
                        craft = ii.getEquipAddReqs(itemId, add.getLeft(), "craft");
                        if ((craft == null || craft != null
                                && chra.getTrait(MapleTraitType.craft).getLocalExp() >= Integer.parseInt(craft))) {
                            switch (add.getMid()) {
                                case "id":
                                    skillid = right;
                                    break;
                                case "level":
                                    skilllevel = right;
                                    break;
                            }
                        }
                    }
                    break;
                case "hpmpchange":
                    switch (add.getMid()) {
                        case "hpChangerPerTime":
                            recoverHP += right;
                            break;
                        case "mpChangerPerTime":
                            recoverMP += right;
                            break;
                    }
                    break;
                case "statinc":
                    boolean canJobx = false,
                     canLevelx = false;
                    job = ii.getEquipAddReqs(itemId, add.getLeft(), "job");
                    if (job != null) {
                        if (job.contains(",")) {
                            final String[] jobs = job.split(",");
                            for (final String x : jobs) {
                                if (chra.getJob() == Integer.parseInt(x)) {
                                    canJobx = true;
                                }
                            }
                        } else if (chra.getJob() == Integer.parseInt(job)) {
                            canJobx = true;
                        }
                    }
                    level = ii.getEquipAddReqs(itemId, add.getLeft(), "level");
                    if (level != null && chra.getLevel() >= Integer.parseInt(level)) {
                        canLevelx = true;
                    }
                    if ((!canJobx && job != null) || (!canLevelx && level != null)) {
                        continue;
                    }
                    if (itemId == 1142367) {
                        final int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                        if (day != 1 && day != 7) {
                            continue;
                        }
                    }
                    switch (add.getMid()) {
                        case "incPAD":
                            物理攻擊力 += right;
                            break;
                        case "incMAD":
                            魔法攻擊力 += right;
                            break;
                        case "incSTR":
                            局部力量 += right;
                            break;
                        case "incDEX":
                            局部敏捷 += right;
                            break;
                        case "incINT":
                            局部智力 += right;
                            break;
                        case "incLUK":
                            局部幸運 += right;
                            break;
                        case "incJump":
                            跳躍力 += right;
                            break;
                        case "incMHP":
                            localmaxhp_x += right;
                            break;
                        case "incMMP":
                            localmaxmp_x += right;
                            break;
                        case "incPDD":
                            防御力 += right;
                            break;
                        case "incEVA":
                            迴避值 += right;
                            break;
                        case "incSpeed":
                            移動速度 += right;
                            break;
                        case "incMMPr":
                            percent_mp += right;
                            break;
                    }
                    break;
            }
        }
        if (skillid != 0 && skilllevel != 0) {
            sData.put(SkillFactory.getSkill(skillid), new SkillEntry((byte) skilllevel, (byte) 0, -1));
        }
        return new Pair<>(localmaxhp_x, localmaxmp_x);
    }

    public void handleItemOption(StructItemOption soc, MapleCharacter chra, boolean first_login,
            Map<Skill, SkillEntry> hmm) {
        局部力量 += soc.get("incSTR");
        局部敏捷 += soc.get("incDEX");
        局部智力 += soc.get("incINT");
        局部幸運 += soc.get("incLUK");
        迴避值 += soc.get("incEVA");
        // incEVA -> increase dodge
        移動速度 += soc.get("incSpeed");
        跳躍力 += soc.get("incJump");
        物理攻擊力 += soc.get("incPAD");
        魔法攻擊力 += soc.get("incMAD");
        防御力 += soc.get("incPDD");
        percent_str += soc.get("incSTRr");
        percent_dex += soc.get("incDEXr");
        percent_int += soc.get("incINTr");
        percent_luk += soc.get("incLUKr");
        percent_hp += soc.get("incMHPr");
        percent_mp += soc.get("incMMPr");
        percent_acc += soc.get("incACCr");
        閃避率 += soc.get("incEVAr");
        percent_atk += soc.get("incPADr");
        percent_matk += soc.get("incMADr");
        percent_wdef += soc.get("incPDDr");
        percent_mdef += soc.get("incMDDr");
        暴擊概率 += soc.get("incCr");
        bossdam_r *= (soc.get("incDAMr") + 100.0) / 100.0;
        if (soc.get("boss") <= 0) {
            dam_r *= (soc.get("incDAMr") + 100.0) / 100.0;
        }
        recoverHP += soc.get("RecoveryHP"); // This shouldn't be here, set 4 seconds.
        recoverMP += soc.get("RecoveryMP"); // This shouldn't be here, set 4 seconds.
        if (soc.get("HP") > 0) { // Should be heal upon attacking
            hpRecover += soc.get("HP");
            hpRecoverProp += soc.get("prop");
        }
        if (soc.get("MP") > 0 && !MapleJob.is惡魔殺手(chra.getJob())) {
            mpRecover += soc.get("MP");
            mpRecoverProp += soc.get("prop");
        }
        ignoreTargetDEF += soc.get("ignoreTargetDEF");
        if (soc.get("ignoreDAM") > 0) {
            ignoreDAM += soc.get("ignoreDAM");
            ignoreDAM_rate += soc.get("prop");
        }
        incAllskill += soc.get("incAllskill");
        if (soc.get("ignoreDAMr") > 0) {
            ignoreDAMr += soc.get("ignoreDAMr");
            ignoreDAMr_rate += soc.get("prop");
        }
        道具恢復效能提升 += soc.get("RecoveryUP"); // only for hp items and skills
        暴擊最小傷害 += soc.get("incCriticaldamageMin");
        暴擊最大傷害 += soc.get("incCriticaldamageMax");
        屬性抗性 += soc.get("incTerR"); // elemental resistance = avoid element damage from monster
        異常抗性 += soc.get("incAsrR"); // abnormal status = disease
        if (soc.get("DAMreflect") > 0) {
            DAMreflect += soc.get("DAMreflect");
            DAMreflect_rate += soc.get("prop");
        }
        mpconReduce += soc.get("mpconReduce");
        reduceCooltime += soc.get("reduceCooltime"); // in seconds
        incMesoProp += soc.get("incMesoProp"); // mesos + %
        dropBuff *= (100 + soc.get("incRewardProp")) / 100.0; // extra drop rate for item
        if (first_login && soc.get("skillID") > 0) {
            hmm.put(SkillFactory.getSkill(getSkillByJob(soc.get("skillID"), chra.getJob())),
                    new SkillEntry((byte) 1, (byte) 0, -1));
        }
        bossdam_r *= (soc.get("bdR") + 100.0) / 100.0;
        ignoreTargetDEF *= (soc.get("imdR") + 100.0) / 100.0;
        // poison, stun, etc (uses level field -> cast disease to mob/player), face?
    }

    public final void handleProfessionTool(final MapleCharacter chra) {
        if (chra.getProfessionLevel(92000000) > 0 || chra.getProfessionLevel(92010000) > 0) {
            final Iterator<Item> itera = chra.getInventory(MapleInventoryType.EQUIP).newList().iterator();
            while (itera.hasNext()) { // goes to first harvesting tool and stops
                final Equip equip = (Equip) itera.next();
                if (equip.getDurability() != 0
                        && (equip.getItemId() / 10000 == 150 && chra.getProfessionLevel(92000000) > 0)
                        || (equip.getItemId() / 10000 == 151 && chra.getProfessionLevel(92010000) > 0)) {
                    if (equip.getDurability() > 0) {
                        durabilityHandling.add(equip);
                    }
                    harvestingTool = equip.getPosition();
                    break;
                }
            }
        }
    }

    public void recalcPVPRank(MapleCharacter chra) {
        this.pvpRank = 10;
        this.pvpExp = chra.getTotalBattleExp();
        for (int i = 0; i < 10; i++) {
            if (pvpExp > GameConstants.getPVPExpNeededForLevel(i + 1)) {
                pvpRank--;
                pvpExp -= GameConstants.getPVPExpNeededForLevel(i + 1);
            }
        }
    }

    public int getHPPercent() {
        return (int) Math.ceil((hp * 100.0) / 局部最大HP);
    }

    public int getMPPercent() {
        return (int) Math.ceil((mp * 100.0) / 局部最大MP);
    }

    public final void init(MapleCharacter chra) {
        recalcLocalStats(chra);
    }

    public final int getStr() {
        return 力量;
    }

    public final int getDex() {
        return 敏捷;
    }

    public final int getInt() {
        return 智力;
    }

    public final int getLuk() {
        return 幸運;
    }

    public final int getHp() {
        return hp;
    }

    public final int getMp() {
        return mp;
    }

    public final int getMaxHp() {
        return maxhp;
    }

    public final int getMaxMp() {
        return maxmp;
    }

    public final void setStr(final short str, MapleCharacter chra) {
        this.力量 = str;
        recalcLocalStats(chra);
    }

    public final void setDex(final short dex, MapleCharacter chra) {
        this.敏捷 = dex;
        recalcLocalStats(chra);
    }

    public final void setInt(final short int_, MapleCharacter chra) {
        this.智力 = int_;
        recalcLocalStats(chra);
    }

    public final void setLuk(final short luk, MapleCharacter chra) {
        this.幸運 = luk;
        recalcLocalStats(chra);
    }

    public final boolean setHp(final int newhp, MapleCharacter chra) {
        return setHp(newhp, false, chra);
    }

    public final boolean setHp(int newhp, boolean silent, MapleCharacter chra) {
        final int oldHp = hp;
        int thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > 局部最大HP) {
            thp = 局部最大HP;
        }
        this.hp = thp;

        if (chra != null) {
            if (!silent) {
                chra.checkBerserk();
                chra.updatePartyMemberHP();
            }
            if (oldHp > hp && !chra.isAlive()) {
                chra.playerDead();
            }
            if (MapleJob.is惡魔復仇者(chra.getJob())) {
                chra.getClient().getSession().writeAndFlush(JobPacket.AvengerPacket.giveAvengerHpBuff(hp));
            }
        }
        return hp != oldHp;
    }

    public final boolean setMp(final int newmp, final MapleCharacter chra) {
        final int oldMp = mp;
        int tmp = newmp;
        if (tmp < 0) {
            tmp = 0;
        }
        if (tmp > 局部最大MP) {
            tmp = 局部最大MP;
        }
        this.mp = tmp;
        return mp != oldMp;
    }

    public final void setMaxHp(final int hp, MapleCharacter chra) {
        this.maxhp = hp;
        recalcLocalStats(chra);
    }

    public final void setMaxMp(final int mp, MapleCharacter chra) {
        this.maxmp = mp;
        recalcLocalStats(chra);
    }

    public final void setInfo(final int maxhp, final int maxmp, final int hp, final int mp) {
        this.maxhp = maxhp;
        this.maxmp = maxmp;
        this.hp = hp;
        this.mp = mp;
    }

    public final int getTotalStr() {
        return 局部力量;
    }

    public final int getTotalDex() {
        return 局部敏捷;
    }

    public final int getTotalInt() {
        return 局部智力;
    }

    public final int getTotalLuk() {
        return 局部幸運;
    }

    public final int getTotalWatk() {
        return 物理攻擊力;
    }

    public final int getTotalMagic() {
        return 魔法攻擊力;
    }

    public final int getTotalWDef() {
        return 防御力;
    }

    public final int getTotalEva() {
        return 迴避值;
    }

    public final int getCurrentMaxHp() {
        return 局部最大HP;
    }

    public final int getCurrentMaxMp() {
        return 局部最大MP;
    }

    public final int getHands() {
        return hands; // Only used for stimulator/maker skills
    }

    public final float getCurrentMinBaseDamage() {
        return localmaxbasedamage;
    }

    public final float getCurrentMaxBaseDamage() {
        return localmaxbasedamage;
    }

    public final float getCurrentMaxBasePVPDamage() {
        return localmaxbasepvpdamage;
    }

    public final float getCurrentMaxBasePVPDamageL() {
        return localmaxbasepvpdamageL;
    }

    public final boolean isRangedJob(final int job) {
        return MapleJob.is蒼龍俠客(job) || MapleJob.is精靈遊俠(job) || MapleJob.is重砲指揮官(job) || job == 400 || (job / 10 == 52)
                || (job / 100 == 3) || (job / 100 == 13) || (job / 100 == 14) || (job / 100 == 33) || (job / 100 == 35)
                || (job / 10 == 41);
    }

    public int getCoolTimeR() {
        if (this.coolTimeR > 5) {
            return 5;
        }
        return this.coolTimeR;
    }

    public int getReduceCooltime() {
        if (this.reduceCooltime > 5) {
            return 5;
        }
        return this.reduceCooltime;
    }

    public int getLimitBreak(MapleCharacter chra) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();

        int limitBreak = 999999;
        Equip weapon = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        if (weapon != null) {
            limitBreak = ii.getLimitBreak(weapon.getItemId()) + weapon.getLimitBreak();

            Equip subweapon = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            if ((subweapon != null) && (ItemConstants.類型.武器(subweapon.getItemId()))) {
                int subWeaponLB = ii.getLimitBreak(subweapon.getItemId()) + subweapon.getLimitBreak();
                if (subWeaponLB > limitBreak) {
                    limitBreak = subWeaponLB;
                }
            }
        }
        return limitBreak;
    }

    public int getAttackCount(int skillId) {
        if (add_skill_attackCount.containsKey(skillId)) {
            return (add_skill_attackCount.get(skillId));
        }
        return 0;
    }

    public int getMobCount(int skillId, int sourceCount) {
        int addTarget;
        if (sourceCount > 1 && add_skill_targetPlus.containsKey(0)) {
            addTarget = add_skill_targetPlus.get(0);
        } else {
            addTarget = 0;
        }
        if (add_skill_targetPlus.containsKey(skillId)) {
            return add_skill_targetPlus.get(skillId) + addTarget;
        }
        return addTarget;
    }

    public int getPassivePlus() {
        return passivePlus;
    }

    public int getReduceCooltimeRate(int skillId) {
        if (this.add_skill_coolTimeR.containsKey(skillId)) {
            return (this.add_skill_coolTimeR.get(skillId));
        }
        return 0;
    }

    public int getIgnoreMobpdpR(int skillId) {
        if (add_skill_ignoreMobpdpR.containsKey(skillId)) {
            return (add_skill_ignoreMobpdpR.get(skillId)) + ignoreTargetDEF;
        }
        return this.ignoreTargetDEF;
    }

    public double getDamageRate() {
        return dam_r;
    }

    public double getBossDamageRate() {
        return bossdam_r;
    }

    public double getBossDamageRate(int skillId) {
        if (add_skill_bossDamageRate.containsKey(skillId)) {
            return (add_skill_bossDamageRate.get(skillId)) + bossdam_r;
        }
        return bossdam_r;
    }

    public int getDuration(int skillId) {
        if (add_skill_duration.containsKey(skillId)) {
            return (add_skill_duration.get(skillId));
        }
        return 0;
    }

    public void addDamageIncrease(int skillId, int val) { // 增加伤害
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (damageIncrease.containsKey(skillId)) {
            int oldval = (damageIncrease.get(Integer.valueOf(skillId)));
            damageIncrease.put(skillId, oldval + val);
        } else {
            damageIncrease.put(skillId, val);
        }
    }

    public void addTargetPlus(int skillId, int val) { // 增加攻击目标数
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (add_skill_targetPlus.containsKey(skillId)) {
            int oldval = (add_skill_targetPlus.get(Integer.valueOf(skillId)));
            add_skill_targetPlus.put(skillId, oldval + val);
        } else {
            add_skill_targetPlus.put(skillId, val);
        }
    }

    public void addAttackCount(int skillId, int val) { // 增加攻击次数
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (add_skill_attackCount.containsKey(skillId)) {
            int oldval = (add_skill_attackCount.get(Integer.valueOf(skillId)));
            add_skill_attackCount.put(skillId, oldval + val);
        } else {
            add_skill_attackCount.put(skillId, val);
        }
    }

    public void addBossDamageRate(int skillId, int val) { // 增加BOSS伤害
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (add_skill_bossDamageRate.containsKey(skillId)) {
            int oldval = (add_skill_bossDamageRate.get(Integer.valueOf(skillId)));
            add_skill_bossDamageRate.put(skillId, oldval + val);
        } else {
            add_skill_bossDamageRate.put(skillId, val);
        }
    }

    public void addIgnoreMobpdpRate(int skillId, int val) {// 增加无视怪物防御
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (add_skill_ignoreMobpdpR.containsKey(skillId)) {
            int oldval = (add_skill_ignoreMobpdpR.get(Integer.valueOf(skillId)));
            add_skill_ignoreMobpdpR.put(skillId, oldval + val);
        } else {
            add_skill_ignoreMobpdpR.put(skillId, val);
        }
    }

    public void addBuffDuration(int skillId, int val) { // 增加BUFF时间
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (add_skill_duration.containsKey(skillId)) {
            int oldval = (add_skill_duration.get(Integer.valueOf(skillId)));
            add_skill_duration.put(skillId, oldval + val);
        } else {
            add_skill_duration.put(skillId, val);
        }
    }

    public void addDotTime(int skillId, int val) { // 增加持续掉血时间
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (add_skill_dotTime.containsKey(skillId)) {
            int oldval = (add_skill_dotTime.get(Integer.valueOf(skillId)));
            add_skill_dotTime.put(skillId, oldval + val);
        } else {
            add_skill_dotTime.put(skillId, val);
        }
    }

    public void addCoolTimeReduce(int skillId, int val) { // 增加减少冷却时间
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (add_skill_coolTimeR.containsKey(skillId)) {
            int oldval = (add_skill_coolTimeR.get(Integer.valueOf(skillId)));
            add_skill_coolTimeR.put(skillId, oldval + val);
        } else {
            add_skill_coolTimeR.put(skillId, val);
        }
    }

    public void addSkillProp(int skillId, int val) { // 增加技能概率
        if ((skillId < 0) || (val <= 0)) {
            return;
        }
        if (add_skill_prop.containsKey(skillId)) {
            int oldval = (add_skill_prop.get(Integer.valueOf(skillId)));
            add_skill_prop.put(skillId, oldval + val);
        } else {
            add_skill_prop.put(skillId, val);
        }
    }
}
