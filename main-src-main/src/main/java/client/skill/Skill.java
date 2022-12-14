package client.skill;

import client.MapleJob;
import constants.GameConstants;
import constants.SkillConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import provider.MapleData;
import provider.MapleDataTool;
import server.MapleStatEffect;
import server.Randomizer;
import server.life.Element;
import tools.Pair;

public class Skill implements Comparator<Skill> {

    private String name = "", psdDamR = "", targetPlus = "";
    private final List<MapleStatEffect> effects = new ArrayList<>();
    private List<MapleStatEffect> pvpEffects = null;
    private List<Integer> animation = null;
    private final List<Pair<String, Integer>> requiredSkill = new ArrayList<>();
    private Element element = Element.NEUTRAL;
    private final int id;
    private int animationTime = 0, masterLevel = 0, maxLevel = 0, delay = 0, trueMax = 0, eventTamingMob = 0,
            skillTamingMob = 0, skillType = 0, psd = 0, psdSkill = 0; // 4 is alert
    private boolean invisible = false, chargeskill = false, timeLimited = false, combatOrders = false,
            pvpDisabled = false, magic = false, casterMove = false, pushTarget = false, pullTarget = false;
    private boolean isBuffSkill = false;
    private boolean isSummonSkill = false;
    private boolean notRemoved = false;
    private int hyper = 0;
    private int hyperStat = 0;
    private int reqLev = 0;
    private int maxDamageOver = 2147483647;
    private int fixLevel;
    private int vehicleID;
    private boolean petPassive = false;
    private int setItemReason;
    private int setItemPartsCount;
    int vskill = 0; // 0 = ?, 1 = skill, 2 = boost

    public Skill(final int id) {
        super();
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Skill loadFromData(final int id, final MapleData data, final MapleData delayData) {
        Skill ret = new Skill(id);

        final int skillType = MapleDataTool.getInt("skillType", data, -1);
        final String elem = MapleDataTool.getString("elemAttr", data, null);
        if (elem != null) {
            ret.element = Element.getFromChar(elem.charAt(0));
        }
        ret.skillType = skillType;
        ret.invisible = MapleDataTool.getInt("invisible", data, 0) > 0;
        ret.notRemoved = (MapleDataTool.getInt("notRemoved", data, 0) > 0);
        ret.timeLimited = MapleDataTool.getInt("timeLimited", data, 0) > 0;
        ret.combatOrders = MapleDataTool.getInt("combatOrders", data, 0) > 0;
        ret.fixLevel = MapleDataTool.getInt("fixLevel", data, 0);
        ret.masterLevel = MapleDataTool.getInt("masterLevel", data, 0);

        ret.psd = MapleDataTool.getInt("psd", data, 0);
        if (ret.psd == 1) {
            final MapleData psdskill = data.getChildByPath("psdSkill");
            if (psdskill != null) {
                ret.psdSkill = Integer.parseInt(data.getChildByPath("psdSkill").getChildren().get(0).getName());
            }
        }

        ret.eventTamingMob = MapleDataTool.getInt("eventTamingMob", data, 0);
        ret.skillTamingMob = MapleDataTool.getInt("skillTamingMob", data, 0);
        ret.vehicleID = MapleDataTool.getInt("vehicleID", data, 0);
        ret.hyper = MapleDataTool.getInt("hyper", data, 0);
        ret.hyperStat = MapleDataTool.getInt("hyperStat", data, 0);
        ret.reqLev = MapleDataTool.getInt("reqLev", data, 0);

        boolean hasVskillMod = data.getChildByPath("vSkill") != null;
        if (hasVskillMod) {
            ret.vskill = MapleDataTool.getInt("vSkill", data); // returns 0 if not found
        }

        ret.petPassive = (MapleDataTool.getInt("petPassive", data, 0) > 0);
        ret.setItemReason = MapleDataTool.getInt("setItemReason", data, 0);
        ret.setItemPartsCount = MapleDataTool.getInt("setItemPartsCount", data, 0);
        final MapleData inf = data.getChildByPath("info");

        if (inf != null) {
            ret.pvpDisabled = MapleDataTool.getInt("pvp", inf, 1) <= 0;
            ret.magic = MapleDataTool.getInt("magicDamage", inf, 0) > 0;
            ret.casterMove = MapleDataTool.getInt("casterMove", inf, 0) > 0;
            ret.pushTarget = MapleDataTool.getInt("pushTarget", inf, 0) > 0;
            ret.pullTarget = MapleDataTool.getInt("pullTarget", inf, 0) > 0;
        }
        final MapleData effect = data.getChildByPath("effect");
        boolean isBuff;
        switch (skillType) {
            case 2:
                isBuff = true;
                break;
            case 3:
                // final attack
                ret.animation = new ArrayList<>();
                ret.animation.add(0);
                isBuff = effect != null;
                switch (id) {
                    case 20040216:
                    case 20040217:
                    case 20040219:
                    case 20040220:
                    case 20041239:
                        isBuff = true;
                }
                break;
            default:
                MapleData action_ = data.getChildByPath("action");
                final MapleData hit = data.getChildByPath("hit");
                final MapleData ball = data.getChildByPath("ball");
                boolean action = false;
                if (action_ == null) {
                    if (data.getChildByPath("prepare/action") != null) {
                        action_ = data.getChildByPath("prepare/action");
                        action = true;
                    }
                }
                isBuff = effect != null && hit == null && ball == null;
                if (action_ != null) {
                    String d;
                    if (action) { // prepare
                        d = MapleDataTool.getString(action_, null);
                    } else {
                        d = MapleDataTool.getString("0", action_, null);
                    }
                    if (d != null) {
                        isBuff |= d.equals("alert2");
                        final MapleData dd = delayData.getChildByPath(d);
                        if (dd != null) {
                            for (MapleData del : dd.getChildren()) {
                                ret.delay += Math.abs(MapleDataTool.getInt("delay", del, 0));
                            }
                            if (ret.delay > 30) {
                                ret.delay = (int) Math.round(ret.delay * 11.0 / 16.0);
                                ret.delay -= (ret.delay % 30);
                            }
                        }
                        if (SkillFactory.getDelay(d) != null) { // this should return true always
                            ret.animation = new ArrayList<>();
                            ret.animation.add(SkillFactory.getDelay(d));
                            if (!action) {
                                for (MapleData ddc : action_.getChildren()) {
                                    if (!MapleDataTool.getString(ddc, d).equals(d)) {
                                        String c = MapleDataTool.getString(ddc);
                                        if (SkillFactory.getDelay(c) != null) {
                                            ret.animation.add(SkillFactory.getDelay(c));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // TODO BUFF::??????????????????
                switch (id) {
                    case 1076: // ????????????????????????
                    case 2111002: // ????????????
                    case 2111003: // ????????????
                    case 2301002: // ????????????
                    case 2321001: // ?????????
                    case 4301004: // ?????????
                    case 12111005: // ???????????????
                    case 14111006: // ?????????
                    case 32121006: // ????????????
                    case 36121007: // ????????????
                    case 100001266:
                        isBuff = false;
                        break;
                    case 93: // ????????????(?????????)
                    case 1004: // ????????????
                    case 1026: // ??????
                    case 1101013: // ????????????
                    case 1121016: // ????????????
                    case 1210016: // ????????????
                    case 1111002:
                    case 1111007:
                    case 1211009:
                    case 1220013:
                    case 1311014: // ???????????????
                    case 1320009:
                    case 2101010: // ????????????
                    case 2120010:
                    case 2121009:
                    case 2201009: // ????????????
                    case 2220010:
                    case 2221009:
                    case 2311006:
                    case 2320011:
                    case 2321010:
                    case 3120006:
                    case 3121002:
                    case 3220005:
                    case 3221002:
                    case 4111001:
                    case 4111009:
                    case 4211003:
                    case 4221013:
                    case 4321000:
                    case 4331003:
                    case 4341002:
                    case 5001005:
                    case 5110001:
                    case 5111005:
                    case 5111007:
                    case 5120011:
                    case 5120012:
                    case 5121003:
                    case 5121009:
                    case 5121015:
                    case 5211001:
                    case 5211002:
                    case 5211006:
                    case 5211007:
                    case 5211009:
                    case 5220002:
                    case 5220011:
                    case 5220012:
                    case 5221004:
                    case 5311004:
                    case 5311005:
                    case 5320007:
                    case 5321003:
                    case 5321004:
                    case 5721066: // ?????????
                    case 5081023: // ????????????
                    case 5701013: // ????????????
                    case 5711024: // ????????????
                    case 5721000: // ????????????
                    case 5701005:
                    case 5711001:
                    case 5711011:
                    case 5220014:// dice2 cosair
                    case 5720005:
                    case 5721002:
                    case 9001004:
                    case 9101004:
                    case 10000093:
                    case 10001004:
                    case 10001026:
                    case 11101022: // ??????
                    case 11111022: // ??????
                    case 11121012: // ????????????????????????
                    case 11121011: // ????????????????????????
                    case 12000022: // ??????:??????
                    case 12101023: // ?????????
                    case 12101024: // ??????
                    case 13111005:
                    case 13001022: // ????????? ??????
                    case 13101023: // ????????????
                    case 13101024: // ????????????
                    case 13111023: // ?????????
                    case 13121004: // ????????????
                    case 13121005: // ????????????
                    case 13120008: // ???????????????
                    case 13121053: // ???????????????
                    case 13121054: // ????????????
                    case 14111007:
                    case 14001021: // ?????? : ??????
                    case 14001022: // ??????
                    case 14001023: // ?????????
                    case 14001027: // ????????????
                    case 15001003:
                    case 15100004:
                    case 15101006:
                    case 15111002:
                    case 15111005:
                    case 15111006:
                    case 15111011:
                    case 15001022: // ????????? ??????
                    case 15101022: // ????????????
                    case 15111022: // ??????
                    case 15111023: // ??????
                    case 15111024: // ??????
                    case 15121005: // ????????????
                    case 15121004: // ??????
                    case 20000093:
                    case 20001004:
                    case 20001026:
                    case 20010093:
                    case 20011004:
                    case 20011026:
                    case 20020093:
                    case 20021026:
                    case 20031209:
                    case 20031210:
                    case 21000000:
                    case 21001008:
                    case 22121001:
                    case 22131001:
                    case 22131002:
                    case 22141002:
                    case 22151002:
                    case 22151003:
                    case 22161002:
                    case 22161004:
                    case 22171000:
                    case 22171004:
                    case 22181000:
                    case 22181003:
                    case 22181004:
                    case 24101005:
                    case 24111002:
                    case 24121008:
                    case 24121009:
                    case 27001004: // ???????????? - Mana Well
                    case 27100003: // ???????????? - Black Blessing
                    case 27101004: // ???????????? - Booster
                    case 27101202: // ???????????? - Pressure Void
                    case 27111004: // ???????????? - Shadow Shell
                    case 27111005: // ???????????? - Dusk Guard
                    case 27111006: // ???????????? - Photic Meditation
                    case 27110007: // ????????????
                    case 27121005: // ???????????? - Dark Crescendo
                    case 27121006: // ???????????? - Arcane Pitch
                    case 30000093:
                    case 30001026:
                    case 30010093:
                    case 30011026:
                    case 31121005:
                    case 32001003:
                    case 32101003:
                    case 32110000:
                    case 32110007:
                    case 32110008:
                    case 32110009:
                    case 32111005:
                    case 32111006:
                    case 32120000:
                    case 32120001:
                    case 32121003:
                    case 32121017: // ????????????
                    case 32121018: // ??????????????????
                    case 32111012: // ????????????
                    case 32101009: // ????????????
                    case 32001016: // ????????????
                    case 32100010: // ????????????I
                    case 32110017: // ????????????II
                    case 32120019: // ????????????III
                    case 32111016: // ????????????
                    case 33101006:
                    case 33111003:
                    case 35001001:
                    case 35001002:
                    case 35101005:
                    case 35101007:
                    case 35101009:
                    case 35111001:
                    case 35111002:
                    case 35111004:
                    case 35111005:
                    case 35111009:
                    case 35111010:
                    case 35111011:
                    case 35111013:
                    case 35120000:
                    case 35120014:
                    case 35121003:
                    case 35121005:
                    case 35121006:
                    case 35121009:
                    case 35121010:
                    case 35121013:
                    case 36111006:
                    case 40011289: // ??????????????????
                    case 40011290: // ????????????
                    case 40011186: // ????????????????????????
                    case 41001001: // ?????????
                    case 41110008: // ?????????????????????
                    case 41101003: // ????????????
                    case 41110006: // ??????
                    case 41101005: // ????????????
                    case 41121002: // ??????
                    case 41121003: // ??????
                    case 41121005: // ????????????
                    case 41121014: // ??????????????????
                    case 41121015: // ????????????
                    case 41121054: // ????????????
                    case 41121053: // ???????????????
                    case 42101002: // ???????????????
                    case 42101004: // ??????????????????
                    case 42111006: // ??????????????????
                    case 42121008:
                    case 42111004: // ????????????
                    case 42121005: // ???????????????
                    case 42121024: // ????????????
                    case 42121054: // ???????????????
                    case 42121053: // ???????????????
                    case 42101022: // ???????????????
                    case 42121022: // ???????????????
                    case 42121000: // ???????????????
                    case 42120003: // ????????????
                    case 42121004: // ???????????????
                    case 42121052: // ????????????
                    case 50001214:
                    case 51101003:
                    case 51111003:
                    case 51111004:
                    case 51121004:
                    case 51121005:
                    case 60001216:
                    case 60001217:
                    case 61101002:
                    case 61111008:
                    case 65121003: // ????????????
                    case 61120007:
                    case 61120008:
                    case 61120011:
                    case 80001000:
                    case 80001089:
                    case 80001427:
                    case 80001428:
                    case 80001430:
                    case 80001432:
                    case 5111010:
                    case 1221014:
                    case 1310016:
                    case 1321014:
                    case 3101004:
                    case 3111011:
                    case 3201004:
                    case 3211012:
                    case 4341052:
                    case 5100015:
                    case 5220019:
                    case 5221015:
                    case 5720012:
                    case 5721003:
                    case 1121053:// ???????????????
                    case 1221053:
                    case 1321053:
                    case 2121053:
                    case 2221053:
                    case 2321053:
                    case 3121053:
                    case 3221053:
                    case 3321053:
                    case 4121053:
                    case 4221053:
                    case 5121053:
                    case 5221053:// ???????????????
                    case 31201003:// ?????????????????????
                    case 31211003:
                    case 31211004:
                    case 31221004:
                    case 31221054:// ?????????????????????
                    case 31221053:// ????????????
                    case 32121053:
                    case 33121053:
                    case 31121053:
                    case 35121053:// ????????????
                    case 24121053:// ????????????
                    case 23121053:
                    case 27121053:
                    case 25101009: // ?????????
                    case 25121132:
                    case 21121053:
                    case 22171053:// ????????????
                    case 80001140:
                    case 20050286:
                    case 25111209:
                    case 25111211:
                    case 25121209:
                    case 14110030:
                    case 1221009:
                    case 5121052:
                    case 51121052:
                    case 14121004:
                    case 14121052:
                    case 35121054:// ????????????:??????
                    case 100001005:
                    case 110001005:
                    case 35121055:
                    case 33001007:
                    case 131001015: // ???????????????
                    case 131001004: // ????????????
                    case 2321052: // ????????????
                    case 131001001: // ???????????????
                    case 131001002: // ???????????????
                    case 131001003: // ???????????????
                    case 131001101: // ???????????????
                    case 131001102: // ???????????????
                    case 131001103: // ???????????????
                    case 131002000: // ???????????????
                    case 131001000: // ???????????????
                    case 131001010: // ??????????????????
                    case 131001113: // ?????????
                    case 80000329: // ????????????
                    case 36101001: // ???????????????
                    case 36111004: // ????????????
                    case 23121000: // ??????????????????
                    case 31211001: // ????????????
                    case 41121001: // ????????????
                    // BOSS??????????????????
                    case 80001535: // ?????????
                    case 80001536: // ????????????
                    case 80001537: // ????????????
                    case 80001538: // ??????
                    case 80001539: // ?????????
                    case 80001540: // ????????????
                    case 80001541: // ????????????
                    case 80001542: // ?????????
                    case 80001543: // ????????????
                    case 80001544: // ?????????
                    case 80001545: // ??????
                    case 80001546: // ????????????
                    case 80001547: // ??????
                    case 800011125: // ?????????
                    case 800011126: // ??????
                    case 13101022:
                        isBuff = true;
                }
                if (GameConstants.isExceedAttack(id)) {
                    isBuff = true;
                }
                if (SkillConstants.isAngel(id)/* || GameConstants.isSummon(id) */) {
                    isBuff = false;
                }
                break;
        }
        ret.chargeskill = data.getChildByPath("keydown") != null;

        final MapleData level = data.getChildByPath("common");
        if (level != null) {
            ret.maxLevel = MapleDataTool.getInt("maxLevel", level, 1); // 10 just a failsafe, shouldn't actually happens
            ret.psdDamR = MapleDataTool.getString("damR", level, ""); // for the psdSkill tag
            ret.targetPlus = MapleDataTool.getString("targetPlus", level, "");
            ret.trueMax = ret.maxLevel + (ret.combatOrders ? 2 : 0);
            for (int i = 1; i <= ret.trueMax; i++) {
                ret.effects.add(MapleStatEffect.loadSkillEffectFromData(level, id, isBuff, i, "x", ret.notRemoved));
            }
            ret.maxDamageOver = MapleDataTool.getInt("MDamageOver", level, 999999);
        } else {
            for (final MapleData leve : data.getChildByPath("level").getChildren()) {
                ret.effects.add(MapleStatEffect.loadSkillEffectFromData(leve, id, isBuff,
                        Byte.parseByte(leve.getName()), null, ret.notRemoved));
            }
            ret.maxLevel = ret.effects.size();
            ret.trueMax = ret.effects.size();
        }
        final MapleData level2 = data.getChildByPath("PVPcommon");
        if (level2 != null) {
            ret.pvpEffects = new ArrayList<>();
            for (int i = 1; i <= ret.trueMax; i++) {
                ret.pvpEffects.add(MapleStatEffect.loadSkillEffectFromData(level2, id, isBuff, i, "x", ret.notRemoved));
            }
        }
        final MapleData reqDataRoot = data.getChildByPath("req");
        if (reqDataRoot != null) {
            for (final MapleData reqData : reqDataRoot.getChildren()) {
                ret.requiredSkill.add(new Pair<>(reqData.getName(), MapleDataTool.getInt(reqData, 1)));
            }
        }
        ret.animationTime = 0;
        if (effect != null) {
            for (final MapleData effectEntry : effect.getChildren()) {
                ret.animationTime += MapleDataTool.getIntConvert("delay", effectEntry, 0);
            }
        }
        ret.isBuffSkill = isBuff;
        switch (id) {
            case 27000207:
            case 27001100:
            case 27001201:
                ret.masterLevel = ret.maxLevel;
        }

        ret.isSummonSkill = (data.getChildByPath("summon") != null);
        return ret;
    }

    public MapleStatEffect getEffect(final int level) {
        if (effects.size() < level) {
            if (effects.size() > 0) { // incAllskill
                return effects.get(effects.size() - 1);
            }
            return null;
        } else if (level <= 0) {
            return effects.get(0);
        }
        return effects.get(level - 1);
    }

    public MapleStatEffect getPVPEffect(final int level) {
        if (pvpEffects == null) {
            return getEffect(level);
        }
        if (pvpEffects.size() < level) {
            if (pvpEffects.size() > 0) { // incAllskill
                return pvpEffects.get(pvpEffects.size() - 1);
            }
            return null;
        } else if (level <= 0) {
            return pvpEffects.get(0);
        }
        return pvpEffects.get(level - 1);
    }

    public int getSkillType() {
        return skillType;
    }

    public List<Integer> getAllAnimation() {
        return animation;
    }

    public int getAnimation() {
        if (animation == null) {
            return -1;
        }
        return (animation.get(Randomizer.nextInt(animation.size())));
    }

    public boolean isPVPDisabled() {
        return pvpDisabled;
    }

    public boolean isVSkill() {
        int job = this.id / 10000;
        return job >= 40000 && job <= 40005;
    }

    public boolean isChargeSkill() {
        return chargeskill;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public boolean hasRequiredSkill() {
        return requiredSkill.size() > 0;
    }

    public List<Pair<String, Integer>> getRequiredSkills() {
        return requiredSkill;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getTrueMax() {
        return trueMax;
    }

    public boolean IsNotRemoved() {
        return notRemoved;
    }

    public boolean combatOrders() {
        return combatOrders;
    }

    public boolean canBeLearnedBy(int job) {
        int skillForJob = id / 10000;
        return MapleJob.getJobGrade(skillForJob) <= MapleJob.getJobGrade(job) && MapleJob.isSameJob(job, skillForJob);
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public boolean isFourthJob() {
        if (isHyper()) {
            return true;
        }

        switch (id) {
            case 1120012:
            case 1320011: // ????????????(10/30)27001100
            case 3110014:
            case 4320005:
            case 4340010:
            case 4340012:
            case 5120011:
            case 5120012:
            case 5220012:
            case 5220014:
            case 5321006:
            case 5720008:
            case 21120011:
            case 21120014:
            case 22171004:
            case 22181004:
            case 23120011:
            case 23120013:
            case 23121008:
            case 33120010:
            case 33121005:
            case 51120000: // ????????????(15/15)
                return false;
        }

        switch (this.id / 10000) {
            case 2312:
            case 2412:
            case 2217:
            case 2218:
            case 2512:
            case 2712:
            case 3122:
            case 6112:
            case 6512:
                return true;
            case 10100:
                return this.id == 101000101;
            case 10110:
                return (this.id == 101100101) || (this.id == 101100201);
            case 10111:
                return (this.id == 101110102) || (this.id == 101110200) || (this.id == 101110203);
            case 10112:
                return (this.id == 101120104) || (this.id == 101120204);
        }
        if ((getMaxLevel() <= 15 && !invisible && getMasterLevel() <= 0)) {
            return false;
        }
        // ????????????
        if (id / 10000 >= 2210 && id / 10000 <= 2218) {
            return ((id / 10000) % 10) >= 7 || getMasterLevel() > 0;
        }
        // ????????????
        if (id / 10000 >= 430 && id / 10000 <= 434) {
            return ((id / 10000) % 10) == 4 || getMasterLevel() > 0;
        }
        if (this.id == 40020002) {
            return true;
        }
        // ?????????
        return ((id / 10000) % 10) == 2 && id < 90000000 && !isBeginnerSkill();
    }

    public Element getElement() {
        return element;
    }

    public int getvehicleID() {
        return vehicleID;
    }

    public int getAnimationTime() {
        return animationTime;
    }

    public int getMasterLevel() {
        return masterLevel;
    }

    public int getDelay() {
        return delay;
    }

    public int getTamingMob() {
        return eventTamingMob;
    }

    public int getSkillTamingMob() {
        return eventTamingMob;
    }

    public boolean isBeginnerSkill() {
        return MapleJob.isBeginner(id / 10000);
    }

    public boolean isMagic() {
        return magic;
    }

    public boolean isHyper() {
        // return hyper > 0;
        return (hyper > 0) && (reqLev > 0);
    }

    public int getHyper() {
        return hyper;
    }

    public int getHyperStat() {
        return hyperStat;
    }

    public boolean isHyperStat() {
        return hyperStat > 0;
    }

    public int getReqLevel() {
        return reqLev;
    }

    public int getMaxDamageOver() {
        return maxDamageOver;
    }

    public boolean isMovement() {
        return casterMove;
    }

    public boolean isPush() {
        return pushTarget;
    }

    public boolean isBuffSkill() {
        return this.isBuffSkill;
    }

    public boolean isSummonSkill() {
        return this.isSummonSkill;
    }

    public boolean isPull() {
        return pullTarget;
    }

    public boolean isAdminSkill() {
        int jobId = id / 10000;
        return MapleJob.is?????????(jobId);
    }

    public boolean isInnerSkill() {
        int jobId = id / 10000;
        return jobId == 7000;
    }

    public boolean isSpecialSkill() {
        int jobId = id / 10000;
        switch (jobId) {
            case 7000:
            case 7100:
            case 8000:
            case 9000:
            case 9100:
            case 9200:
            case 9201:
            case 9202:
            case 9203:
            case 9204:
            case 9500:
                return true;
            default:
                return false;
        }
    }

    public boolean isPetPassive() {
        return this.petPassive;
    }

    public int getSetItemReason() {
        return this.setItemReason;
    }

    public int geSetItemPartsCount() {
        return this.setItemPartsCount;
    }

    @Override
    public int compare(Skill o1, Skill o2) {
        return (Integer.valueOf(o1.getId()).compareTo(o2.getId()));
    }

    public boolean isTeachSkills() {
        switch (this.id) {
            case 110: // ???????????????
            case 1214: // ???????????????
            case 10000255: // ??????????????????????????????
            case 10000256: // ??????????????????????????????
            case 10000257: // ?????????????????????????????????
            case 10000258: // ??????????????????????????????
            case 10000259: // ??????????????????????????????
            case 20021110: // ???????????????
            case 20030204: // ????????????
            case 20040218: // ??????
            case 20050286: // ????????????
            case 30010112: // ????????????
            case 30010241: // ????????????
            case 30020233: // ????????????
            case 40010001: // ????????????
            case 40020002: // ????????????
            case 50001214: // ????????????
            case 60000222: // ????????????
            case 60011219: // ????????????
            case 100000271: // ????????????
            case 110000800: // ????????????
                return true;
        }
        return false;
    }

    // TODO ????????????
    public boolean isLinkSkills() {
        switch (this.id) {
            case 80000000: // ????????????
            case 80001151: // ???????????????
            case 80000066: // ??????????????????(??????)
            case 80000067: // ??????????????????(??????)
            case 80000068: // ??????????????????(??????)
            case 80000069: // ??????????????????(??????)
            case 80000070: // ??????????????????(??????)
            case 80001040: // ???????????????
            case 80000002: // ????????????
            case 80000005: // ????????????
            case 80000169: // ????????????
            case 80000001: // ????????????
            case 80000050: // ????????????
            case 80000047: // ????????????
            case 80000003: // ????????????
            case 80000004: // ????????????
            case 80001140: // ????????????
            case 80000006: // ????????????
            case 80001155: // ????????????
            case 80000110: // ????????????
            case 80010006: // ????????????
                return true;
        }
        return false;
    }

    public boolean isLinkedAttackSkill() {
        return GameConstants.isLinkedAttackSkill(id);
    }

    public int getFixLevel() {
        return fixLevel;
    }

    public int getPsdSkill() {
        return psdSkill;
    }

    public int getPsd() {
        return psd;
    }

    public String getPsdDamR() {
        return psdDamR;
    }

    public String getPsdtarget() {
        return targetPlus;
    }

    public int getVSkill() {
        return this.vskill;
    }

}
