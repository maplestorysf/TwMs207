package server;

import client.*;
import client.MapleTrait.MapleTraitType;
import client.character.stat.MapleBuffStat;
import client.character.stat.MapleDisease;
import client.character.stat.TemporaryStatBase;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.skill.Skill;
import client.skill.SkillFactory;
import constants.GameConstants;
import constants.ItemConstants;
import constants.SkillConstants;
import extensions.temporary.UserEffectOpcode;
import handling.channel.ChannelServer;
import handling.channel.handler.PlayerHandler;
import handling.world.MaplePartyCharacter;
import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleDataType;
import server.MapleCarnivalFactory.MCSkill;
import server.Timer.BuffTimer;
import server.buffs.BuffClassFetcher;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.*;
import tools.CaltechEval;
import tools.FileoutputUtil;
import tools.Pair;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.JobPacket;
import tools.packet.JobPacket.PhantomPacket;
import tools.packet.SkillPacket;

import java.awt.*;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

public class MapleStatEffect implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    public Map<MapleStatInfo, Integer> info;
    private Map<MapleTraitType, Integer> traits;
    private boolean overTime, skill, notRemoved, repeatEffect, partyBuff = true;
    public EnumMap<MapleBuffStat, Integer> statups;
    private ArrayList<Pair<Integer, Integer>> availableMap;
    public EnumMap<MonsterStatus, Integer> monsterStatus;
    private Point lt, rb;
    public Point lt2, rb2;
    private byte level;
    // private List<Pair<Integer, Integer>> randomMorph;
    private List<MapleDisease> cureDebuffs;
    private List<Integer> petsCanConsume, familiars, randomPickup;
    private List<Triple<Integer, Integer, Integer>> rewardItem;
    private byte familiarTarget, recipeUseCount, recipeValidDay, reqSkillLevel, slotCount, effectedOnAlly,
            effectedOnEnemy, type, preventslip, immortal, bs, slotPerLine;
    private short ignoreMob, mesoR, thaw, fatigueChange, lifeId, imhp, immp, inflation, useLevel, indiePdd, indieMdd,
            incPVPdamage, mobSkill, mobSkillLevel, effectDelay = 0;
    public double hpR, mpR;
    private int sourceid, recipe, moveTo, moneyCon, morphId = 0, expinc, exp, consumeOnPickup, runOnPickup, charColor,
            interval, rewardMeso, totalprob, cosmetic;
    private int weapon = 0;
    private int expBuff, itemup, mesoup, berserk, illusion, booster, cp, nuffSkill, combo;

    public static MapleStatEffect loadSkillEffectFromData(final MapleData source, final int skillid,
            final boolean overtime, final int level, final String variables, boolean notRemoved) {
        return loadFromData(source, skillid, true, overtime, level, variables, notRemoved);
    }

    public static MapleStatEffect loadItemEffectFromData(final MapleData source, final int itemid) {
        return loadFromData(source, itemid, false, false, 1, null, false);
    }

    private static void addBuffStatPairToListIfNotZero(final EnumMap<MapleBuffStat, Integer> list,
            final MapleBuffStat buffstat, final Integer val) {
        if (val != 0) {
            list.put(buffstat, val);
        }
    }

    private static int parseEval(String path, MapleData source, int def, String variables, int level) {
        if (variables == null) {
            return MapleDataTool.getIntConvert(path, source, def);
        }
        final MapleData dd = source.getChildByPath(path);
        if (dd == null) {
            return def;
        }
        if (dd.getType() != MapleDataType.STRING) {
            return MapleDataTool.getIntConvert(path, source, def);
        }
        String dddd = MapleDataTool.getString(dd).replace(variables, String.valueOf(level));

        int num = def;
        try {
            if (dddd.substring(0, 1).equals("-")) { // -30+3*x
                if (dddd.substring(1, 2).equals("u") || dddd.substring(1, 2).equals("d")) { // -u(x/2)
                    dddd = "n(" + dddd.substring(1, dddd.length()) + ")"; // n(u(x/2))
                } else {
                    dddd = "n" + dddd.substring(1, dddd.length()); // n30+3*x
                }
            } else if (dddd.substring(0, 1).equals("=")) { // lol nexon and their mistakes
                dddd = dddd.substring(1, dddd.length());
            } else if (dddd.contains("y")) { // AngelicBuster Exception
                dddd = dddd.replaceAll("y", "0");
            } else if (dddd.contains("X")) {
                String replace = dddd.replace("X", String.valueOf(level));
                dddd = replace;
            }

            // Just Enter
            if (dddd.contains("\\r\\n")) {
                String replace = dddd.replace("\\r\\n", "");
                dddd = replace;
            }
            if (dddd.contains("\r\n")) {
                String replace = dddd.replace("\r\n", "");
                dddd = replace;
            }

            if (dddd.endsWith("%")) {
                System.err.println("WZ異常未處理");
                String replace = dddd.replace("%", "");
                dddd = replace;
            }
            num = (int) (new CaltechEval(dddd).evaluate());
        } catch (Exception e) {
            System.err.println("parseEval異常(" + dddd + "):" + e);
        }
        return num;
    }

    private static MapleStatEffect loadFromData(final MapleData source, final int sourceid, final boolean skill,
            final boolean overTime, final int level, final String variables, boolean notRemoved) {
        final MapleStatEffect ret = new MapleStatEffect();
        ret.sourceid = sourceid;
        ret.skill = skill;
        ret.level = (byte) level;
        if (source == null) {
            return ret;
        }
        ret.info = new EnumMap<>(MapleStatInfo.class);
        for (final MapleStatInfo i : MapleStatInfo.values()) {
            if (i.isSpecial()) {
                ret.info.put(i, parseEval(i.name().substring(0, i.name().length() - 1), source, i.getDefault(),
                        variables, level));
            } else {
                ret.info.put(i, parseEval(i.name(), source, i.getDefault(), variables, level));
            }
        }
        ret.hpR = parseEval("hpR", source, 0, variables, level) / 100.0;
        ret.mpR = parseEval("mpR", source, 0, variables, level) / 100.0;
        ret.ignoreMob = (short) parseEval("ignoreMobpdpR", source, 0, variables, level);
        ret.thaw = (short) parseEval("thaw", source, 0, variables, level);
        ret.interval = parseEval("interval", source, 0, variables, level);
        ret.expinc = parseEval("expinc", source, 0, variables, level);
        ret.exp = parseEval("exp", source, 0, variables, level);
        ret.morphId = parseEval("morph", source, 0, variables, level);
        ret.cp = parseEval("cp", source, 0, variables, level);
        ret.cosmetic = parseEval("cosmetic", source, 0, variables, level);
        ret.slotCount = (byte) parseEval("slotCount", source, 0, variables, level);
        ret.slotPerLine = (byte) parseEval("slotPerLine", source, 0, variables, level);
        ret.preventslip = (byte) parseEval("preventslip", source, 0, variables, level);
        ret.useLevel = (short) parseEval("useLevel", source, 0, variables, level);
        ret.nuffSkill = parseEval("nuffSkill", source, 0, variables, level);
        ret.familiarTarget = (byte) (parseEval("familiarPassiveSkillTarget", source, 0, variables, level) + 1);
        ret.immortal = (byte) parseEval("immortal", source, 0, variables, level);
        ret.type = (byte) parseEval("type", source, 0, variables, level);
        ret.bs = (byte) parseEval("bs", source, 0, variables, level);
        ret.indiePdd = (short) parseEval("indiePdd", source, 0, variables, level);
        ret.indieMdd = (short) parseEval("indieMdd", source, 0, variables, level);
        ret.expBuff = parseEval("expBuff", source, 0, variables, level);
        ret.itemup = parseEval("itemupbyitem", source, 0, variables, level);
        ret.mesoup = parseEval("mesoupbyitem", source, 0, variables, level);
        ret.berserk = parseEval("berserk", source, 0, variables, level);
        ret.booster = parseEval("booster", source, 0, variables, level);
        ret.lifeId = (short) parseEval("lifeId", source, 0, variables, level);
        ret.inflation = (short) parseEval("inflation", source, 0, variables, level);
        ret.imhp = (short) parseEval("imhp", source, 0, variables, level);
        ret.immp = (short) parseEval("immp", source, 0, variables, level);
        ret.illusion = parseEval("illusion", source, 0, variables, level);

        ret.consumeOnPickup = parseEval("consumeOnPickup", source, 0, variables, level);
        if (ret.consumeOnPickup == 1) {
            if (parseEval("party", source, 0, variables, level) > 0) {
                ret.consumeOnPickup = 2;
            }
        }
        ret.runOnPickup = parseEval("runOnPickup", source, 0, variables, level);
        ret.recipe = parseEval("recipe", source, 0, variables, level);
        ret.recipeUseCount = (byte) parseEval("recipeUseCount", source, 0, variables, level);
        ret.recipeValidDay = (byte) parseEval("recipeValidDay", source, 0, variables, level);
        ret.reqSkillLevel = (byte) parseEval("reqSkillLevel", source, 0, variables, level);
        ret.effectedOnAlly = (byte) parseEval("effectedOnAlly", source, 0, variables, level);
        ret.effectedOnEnemy = (byte) parseEval("effectedOnEnemy", source, 0, variables, level);
        ret.incPVPdamage = (short) parseEval("incPVPDamage", source, 0, variables, level);
        ret.moneyCon = parseEval("moneyCon", source, 0, variables, level);
        ret.moveTo = parseEval("moveTo", source, -1, variables, level);
        ret.repeatEffect = ret.is战法灵气();

        ret.charColor = 0;
        String cColor = MapleDataTool.getString("charColor", source, null);
        if (cColor != null) {
            ret.charColor |= Integer.parseInt("0x" + cColor.substring(0, 2));
            ret.charColor |= Integer.parseInt("0x" + cColor.substring(2, 4) + "00");
            ret.charColor |= Integer.parseInt("0x" + cColor.substring(4, 6) + "0000");
            ret.charColor |= Integer.parseInt("0x" + cColor.substring(6, 8) + "000000");
        }
        ret.traits = new EnumMap<>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            int expz = parseEval(t.name() + "EXP", source, 0, variables, level);
            if (expz != 0) {
                ret.traits.put(t, expz);
            }
        }
        List<MapleDisease> cure = new ArrayList<>(5);
        if (parseEval("poison", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.中毒);
        }
        if (parseEval("seal", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.封印);
        }
        if (parseEval("darkness", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.黑暗);
        }
        if (parseEval("weakness", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.虛弱);
        }
        if (parseEval("curse", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.詛咒);
        }
        ret.cureDebuffs = cure;
        ret.petsCanConsume = new ArrayList<>();
        for (int i = 0; true; i++) {
            final int dd = parseEval(String.valueOf(i), source, 0, variables, level);
            if (dd > 0) {
                ret.petsCanConsume.add(dd);
            } else {
                break;
            }
        }
        final MapleData mdd = source.getChildByPath("0");
        if (mdd != null && mdd.getChildren().size() > 0) {
            ret.mobSkill = (short) parseEval("mobSkill", mdd, 0, variables, level);
            ret.mobSkillLevel = (short) parseEval("level", mdd, 0, variables, level);
        } else {
            ret.mobSkill = 0;
            ret.mobSkillLevel = 0;
        }
        final MapleData pd = source.getChildByPath("randomPickup");
        if (pd != null) {
            ret.randomPickup = new ArrayList<>();
            for (MapleData p : pd.getChildren()) {
                ret.randomPickup.add(MapleDataTool.getInt(p));
            }
        }
        final MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.lt = (Point) ltd.getData();
            ret.rb = (Point) source.getChildByPath("rb").getData();
            if (source.getChildByPath("lt2") != null) {
                ret.lt2 = (Point) source.getChildByPath("lt2").getData();
            } else {
                ret.lt2 = null;
            }
            if (source.getChildByPath("rb2") != null) {
                ret.rb2 = (Point) source.getChildByPath("rb2").getData();
            } else {
                ret.rb2 = null;
            }
        }
        final MapleData ltc = source.getChildByPath("con");
        if (ltc != null) {
            ret.availableMap = new ArrayList<>();
            for (MapleData ltb : ltc.getChildren()) {
                ret.availableMap.add(
                        new Pair<>(MapleDataTool.getInt("sMap", ltb, 0), MapleDataTool.getInt("eMap", ltb, 999999999)));
            }
        }
        final MapleData ltb = source.getChildByPath("familiar");
        if (ltb != null) {
            ret.fatigueChange = (short) (parseEval("incFatigue", ltb, 0, variables, level)
                    - parseEval("decFatigue", ltb, 0, variables, level));
            ret.familiarTarget = (byte) parseEval("target", ltb, 0, variables, level);
            final MapleData lta = ltb.getChildByPath("targetList");
            if (lta != null) {
                ret.familiars = new ArrayList<Integer>();
                for (MapleData ltz : lta.getChildren()) {
                    ret.familiars.add(MapleDataTool.getInt(ltz, 0));
                }
            }
        } else {
            ret.fatigueChange = 0;
        }
        int totalprob = 0;
        final MapleData lta = source.getChildByPath("reward");
        if (lta != null) {
            ret.rewardMeso = parseEval("meso", lta, 0, variables, level);
            final MapleData ltz = lta.getChildByPath("case");
            if (ltz != null) {
                ret.rewardItem = new ArrayList<Triple<Integer, Integer, Integer>>();
                for (MapleData lty : ltz.getChildren()) {
                    ret.rewardItem.add(new Triple<Integer, Integer, Integer>(MapleDataTool.getInt("id", lty, 0),
                            MapleDataTool.getInt("count", lty, 0), MapleDataTool.getInt("prop", lty, 0))); // todo:
                    // period (in
                    // minutes)
                    totalprob += MapleDataTool.getInt("prob", lty, 0);
                }
            }
        } else {
            ret.rewardMeso = 0;
        }
        ret.totalprob = totalprob;
        // start of server calculated stuffs
        if (ret.skill) {
            final int priceUnit = ret.info.get(MapleStatInfo.priceUnit); // Guild skills
            if (priceUnit > 0) {
                final int price = ret.info.get(MapleStatInfo.price);
                final int extendPrice = ret.info.get(MapleStatInfo.extendPrice);
                ret.info.put(MapleStatInfo.price, price * priceUnit);
                ret.info.put(MapleStatInfo.extendPrice, extendPrice * priceUnit);
            }
            switch (sourceid) {
                case 31220007:
                    ret.info.put(MapleStatInfo.attackCount, 2);
                    break;
                case 61101002:
                case 61110211:
                    ret.info.put(MapleStatInfo.attackCount, 3);
                    break;
                case 27101100:
                case 36001005:
                    ret.info.put(MapleStatInfo.attackCount, 4);
                    break;
                case 61120007:
                case 61121217:
                    ret.info.put(MapleStatInfo.attackCount, 5);
                case 1100002:
                case 1120013:
                case 1200002:
                case 1300002:
                case 2111007:
                case 2211007:
                case 2311007:
                case 3100001:
                case 3120008:
                case 3200001:
                case 12111007:
                case 21100010:
                case 21120012:
                case 22150004:
                case 22161005:
                case 22181004:
                case 23100006:
                case 23120012:
                case 32111010:
                case 33100009:
                case 33120011:
                    ret.info.put(MapleStatInfo.mobCount, 6);
                    break;
                case 24100003:
                case 24120002:
                case 35111004:
                case 35121005:
                case 35121013:
                    ret.info.put(MapleStatInfo.attackCount, 6);
                    ret.info.put(MapleStatInfo.bulletCount, 6);
                    break;
            }
            if (GameConstants.isNoDelaySkill(sourceid)) {
                ret.info.put(MapleStatInfo.mobCount, 6);
            }
        }
        if (!ret.skill && ret.info.get(MapleStatInfo.time) > -1) {
            ret.overTime = true;
        } else {
            ret.info.put(MapleStatInfo.time,
                    (ret.info.get(MapleStatInfo.time) * (ret.info.get(MapleStatInfo.time) > 0 ? 1000 : 1))); // items
            // have
            // their
            // times
            // stored
            // in ms,
            // of
            // course
            ret.info.put(MapleStatInfo.subTime, (ret.info.get(MapleStatInfo.subTime) * 1000));
            ret.overTime = overTime || ret.isMorph() || ret.isFinalAttack() || ret.isAngel()
                    || ret.getSummonMovementType() != null;
            ret.notRemoved = notRemoved;
        }
        ret.monsterStatus = new EnumMap<>(MonsterStatus.class);
        ret.statups = new EnumMap<>(MapleBuffStat.class);
        // 道具Effect處理
        if (ret.overTime && ret.getSummonMovementType() == null && !ret.isEnergyCharge() && !ret.skill) {
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.PAD, ret.info.get(MapleStatInfo.pad));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.PDD, ret.info.get(MapleStatInfo.pdd));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MAD, ret.info.get(MapleStatInfo.mad));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ACC, ret.info.get(MapleStatInfo.acc));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.EVA, ret.info.get(MapleStatInfo.eva));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.Speed,
                    sourceid == 32120001 || sourceid == 32120014 || sourceid == 32101003 ? ret.info.get(MapleStatInfo.x)
                            : ret.info.get(MapleStatInfo.speed));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.Jump, ret.info.get(MapleStatInfo.jump));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MaxMP, ret.info.get(MapleStatInfo.mhpR));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MaxHP, ret.info.get(MapleStatInfo.mmpR));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.Booster, ret.booster);
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.Thaw, Integer.valueOf(ret.thaw));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ExpBuffRate, ret.expBuff); // EXP
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.ItemUpByItem,
                    GameConstants.getModifier(ret.sourceid, ret.itemup)); // defaults to 2x
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.MesoUpByItem,
                    GameConstants.getModifier(ret.sourceid, ret.mesoup)); // defaults to 2x
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.Barrier, ret.illusion);
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.DojangBerserk, ret.berserk);
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.EMHP, ret.info.get(MapleStatInfo.emhp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.EMMP, ret.info.get(MapleStatInfo.emmp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.EPAD, ret.info.get(MapleStatInfo.epad));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.EMAD, ret.info.get(MapleStatInfo.emad));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.EPDD, ret.info.get(MapleStatInfo.epdd));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.Inflation, Integer.valueOf(ret.inflation));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.STR, ret.info.get(MapleStatInfo.str));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.DEX, ret.info.get(MapleStatInfo.dex));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.INT, ret.info.get(MapleStatInfo.int_));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.LUK, ret.info.get(MapleStatInfo.luk));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndiePAD, ret.info.get(MapleStatInfo.indiePad));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieMAD, ret.info.get(MapleStatInfo.indieMad));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieMHP, Integer.valueOf(ret.imhp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieMMP, Integer.valueOf(ret.immp)); // same one?
            // lol
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieMHPR, ret.info.get(MapleStatInfo.indieMhpR));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieMMPR, ret.info.get(MapleStatInfo.indieMmpR));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieMHP, ret.info.get(MapleStatInfo.indieMhp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieMMP, ret.info.get(MapleStatInfo.indieMmp));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.PVPDamage, Integer.valueOf(ret.incPVPdamage));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieJump, ret.info.get(MapleStatInfo.indieJump));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieSpeed,
                    ret.info.get(MapleStatInfo.indieSpeed));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieACC, ret.info.get(MapleStatInfo.indieAcc));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieEVA, ret.info.get(MapleStatInfo.indieEva));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieAllStat,
                    ret.info.get(MapleStatInfo.indieAllStat));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.IndieBooster,
                    ret.info.get(MapleStatInfo.indieBooster));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.PVPDamageSkill,
                    ret.info.get(MapleStatInfo.PVPdamage));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.NotDamaged, Integer.valueOf(ret.immortal));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.Event2, Integer.valueOf(ret.preventslip));
            addBuffStatPairToListIfNotZero(ret.statups, MapleBuffStat.Event2, ret.charColor > 0 ? 1 : 0);
        }
        // 技能Effect處理
        if (ret.skill) { // hack because we can't get from the datafile...
            boolean handle = BuffClassFetcher.getHandleMethod(ret, sourceid);
            if (!handle) {
                switch (sourceid) {
                    case 80001535: // 巴洛古
                    case 80001536: // 殘暴炎魔
                    case 80001537: // 闇黑龍王
                    case 80001538: // 希拉
                    case 80001539: // 凡雷恩
                    case 80001540: // 阿卡伊農
                    case 80001541: // 梅格耐斯
                    case 80001542: // 皮卡啾
                    case 80001543: // 西格諾斯
                    case 80001544: // 比艾樂
                    case 80001545: // 斑斑
                    case 80001546: // 血腥皇后
                    case 80001547: // 貝倫
                    case 800011125: // 森蘭丸
                    case 800011126: // 濃姬
                        // BOSS組隊組成加持
                        ret.statups.put(MapleBuffStat.BossWaitingLinesBuff, ret.info.get(MapleStatInfo.x));
                        break;
                    case 1211009:
                    case 1111007:
                    case 1311007: // magic crash
                    case 51111005: // Mihile's magic crash
                        ret.monsterStatus.put(MonsterStatus.M_HitCriDamR, 1);
                        break;
                    case 4001002: // disorder
                    case 14001002: // cygnus disorder
                        ret.monsterStatus.put(MonsterStatus.M_PAD, ret.info.get(MapleStatInfo.x));
                        ret.monsterStatus.put(MonsterStatus.M_PDR, ret.info.get(MapleStatInfo.y));
                        break;
                    case 32121018:
                        ret.monsterStatus.put(MonsterStatus.M_BMageDebuff, ret.info.get(MapleStatInfo.x));
                        break;
                    case 5221009: // Mind Control
                        ret.monsterStatus.put(MonsterStatus.M_BodyPressure, 1);
                        break;
                    case 4341003: // Monster Bomb
                        ret.monsterStatus.put(MonsterStatus.M_AddDamParty, (int) ret.info.get(MapleStatInfo.damage));
                        break;
                    case 1201006: // threaten
                        ret.monsterStatus.put(MonsterStatus.M_PAD, ret.info.get(MapleStatInfo.x));
                        ret.monsterStatus.put(MonsterStatus.M_PDR, ret.info.get(MapleStatInfo.x));
                        ret.monsterStatus.put(MonsterStatus.M_Darkness, ret.info.get(MapleStatInfo.z));
                        break;
                    case 22141001:
                    case 1211002: // charged blow
                    case 1111008: // shout
                    case 4211002: // assaulter
                    case 3101005: // arrow bomb
                    case 1111005: // coma: sword
                    case 4221007: // boomerang step
                    case 5101002: // Backspin Blow
                    case 5101003: // Double Uppercut
                    case 5121004: // Demolition
                    case 5121005: // Snatch
                    case 5121007: // Barrage
                    case 5201004: // pirate blank shot
                    case 4121008: // Ninja Storm
                    case 22151001:
                    case 4201004: // steal, new
                    case 33101001:
                    case 33101002:
                    case 32101001:
                    case 32111011:
                    case 32121004:
                    case 33111002:
                    case 33121002:
                    case 35101003:
                    case 35111015:
                    case 5111002: // energy blast
                    case 15101005:
                    case 4331005:
                    case 1121001: // magnet
                    case 1221001:
                    case 1321001:
                    case 9001020:
                    case 31111001:
                    case 31101002:
                    case 9101020:
                    case 2211003:
                    case 2311004:
                    case 3120010:
                    case 22181001:
                    case 21110006:
                    case 22131000:
                    case 5301001:
                    case 5311001:
                    case 5311002:
                    case 2221006:
                    case 5310008:
                        ret.monsterStatus.put(MonsterStatus.M_Stun, 1);
                        break;
                    case 90001004:
                    case 4321002:
                    case 1111003:
                    case 11111002:
                        ret.monsterStatus.put(MonsterStatus.M_Darkness, ret.info.get(MapleStatInfo.x));
                        break;
                    case 4221003:
                    case 4121003:
                    case 33121005:
                        ret.monsterStatus.put(MonsterStatus.M_Ambush, ret.info.get(MapleStatInfo.x));
                        ret.monsterStatus.put(MonsterStatus.M_MDR, ret.info.get(MapleStatInfo.x)); // removed for taunt
                        ret.monsterStatus.put(MonsterStatus.M_PDR, ret.info.get(MapleStatInfo.x)); // removed for taunt
                        break;
                    case 31121003:
                        ret.monsterStatus.put(MonsterStatus.M_Ambush, ret.info.get(MapleStatInfo.w));
                        ret.monsterStatus.put(MonsterStatus.M_MDR, ret.info.get(MapleStatInfo.x));
                        ret.monsterStatus.put(MonsterStatus.M_PDR, ret.info.get(MapleStatInfo.x));
                        ret.monsterStatus.put(MonsterStatus.M_MAD, ret.info.get(MapleStatInfo.x));
                        ret.monsterStatus.put(MonsterStatus.M_PAD, ret.info.get(MapleStatInfo.x));
                        ret.monsterStatus.put(MonsterStatus.M_ACC, ret.info.get(MapleStatInfo.x));
                        break;
                    case 23121002: // not sure if negative
                        ret.monsterStatus.put(MonsterStatus.M_PDR, -ret.info.get(MapleStatInfo.x));
                        break;
                    case 2201004: // cold beam
                    case 2221003:
                    case 2211002: // ice strike
                    case 3211003: // blizzard
                    case 2211006: // il elemental compo
                    case 2221007: // Blizzard
                    case 5211005: // Ice Splitter
                    case 2121006: // Paralyze
                    case 21120006: // Tempest
                    case 22121000:
                    case 90001006:
                    case 2221001:
                        ret.monsterStatus.put(MonsterStatus.M_Freeze, 1);
                        ret.info.put(MapleStatInfo.time, ret.info.get(MapleStatInfo.time) * 2); // freezing skills are a
                        // little strange
                        break;
                    case 2101003: // fp slow
                    case 2201003: // il slow
                    case 12101001:
                    case 90001002:
                        ret.monsterStatus.put(MonsterStatus.M_Speed, ret.info.get(MapleStatInfo.x));
                        break;
                    case 5011002:
                        ret.monsterStatus.put(MonsterStatus.M_Speed, ret.info.get(MapleStatInfo.z));
                        break;
                    case 23111002:
                    case 22161002: // phantom imprint
                        ret.monsterStatus.put(MonsterStatus.M_Mystery, ret.info.get(MapleStatInfo.x));
                        break;
                    case 90001003:
                        ret.monsterStatus.put(MonsterStatus.M_Poison, 1);
                        break;
                    case 4121004: // Ninja ambush
                    case 4221004:
                        ret.monsterStatus.put(MonsterStatus.M_Blind, (int) ret.info.get(MapleStatInfo.damage));
                        break;
                    case 2311005:
                        ret.monsterStatus.put(MonsterStatus.M_PImmune, 1);
                        break;
                    case 5211011:
                    case 5211015:
                    case 5211016:
                    case 5711001: // turret
                    case 2121005: // elquines
                    case 3201007:
                    case 3101007:
                    case 3211005: // golden eagle
                    case 3111005: // golden hawk
                    case 33111005:
                    case 3121006: // phoenix
                    case 23111008:
                    case 23111009:
                    case 23111010:
                        ret.statups.put(MapleBuffStat.SUMMON, 1);
                        ret.monsterStatus.put(MonsterStatus.M_Stun, 1);
                        break;
                    case 3221005: // frostprey
                    case 2221005: // ifrit
                        ret.statups.put(MapleBuffStat.SUMMON, 1);
                        ret.monsterStatus.put(MonsterStatus.M_Freeze, 1);
                        break;
                    case 2321003: // bahamut
                    case 5211002: // Pirate bird summon
                    case 11001004:
                    case 12001004:
                    case 12111004: // Itrit
                    case 13001004:
                    case 14001005:
                    case 15001004:
                    case 33101008: // summon - its raining mines
                    case 4111007: // dark flare
                    case 4211007: // dark flare
                    case 14111010: // dark flare
                    case 5321004:
                        ret.statups.put(MapleBuffStat.SUMMON, 1);
                        break;
                    case 80001034: // virtue
                    case 80001035: // virtue
                    case 80001036: // virtue
                        ret.statups.put(MapleBuffStat.AsrRByItem, 1);
                        break;
                    case 2211004: // il seal
                    case 2111004: // fp seal
                    case 12111002: // cygnus seal
                    case 90001005:
                        ret.monsterStatus.put(MonsterStatus.M_Seal, 1);
                        break;
                    case 24121003:
                        ret.info.put(MapleStatInfo.damage, ret.info.get(MapleStatInfo.v));
                        ret.info.put(MapleStatInfo.attackCount, ret.info.get(MapleStatInfo.w));
                        ret.info.put(MapleStatInfo.mobCount, ret.info.get(MapleStatInfo.x));
                        break;
                    case 4111003: // shadow web
                    case 14111001:
                        ret.monsterStatus.put(MonsterStatus.M_MImmune, 1);
                        break;
                    case 80001140: // 光之守護
                        ret.statups.put(MapleBuffStat.Stance, (int) ret.info.get(MapleStatInfo.prop));
                        break;
                    case 32120000:
                        ret.info.put(MapleStatInfo.dot, ret.info.get(MapleStatInfo.damage));
                        ret.info.put(MapleStatInfo.dotTime, 3);
                        break;
                    case 32120001:
                        ret.monsterStatus.put(MonsterStatus.M_Speed, ret.info.get(MapleStatInfo.speed));
                        break;
                    case 80001040:
                    case 20021110:
                    case 20031203:
                        ret.moveTo = ret.info.get(MapleStatInfo.x);
                        break;
                    case 80001089: // 飛天騎乘Soaring
                        ret.statups.put(MapleBuffStat.Flying, 1);
                        break;
                    case 80001427: // 疾速之輪
                        ret.statups.put(MapleBuffStat.IndieJump, ret.info.get(MapleStatInfo.indieJump));
                        ret.statups.put(MapleBuffStat.IndieSpeed, ret.info.get(MapleStatInfo.indieSpeed));
                        ret.statups.put(MapleBuffStat.IndieEXP, ret.info.get(MapleStatInfo.indieExp));
                        ret.statups.put(MapleBuffStat.IndieBooster, ret.info.get(MapleStatInfo.indieBooster));
                        break;
                    case 80001428: // 再生之輪
                        ret.statups.put(MapleBuffStat.IndieAsrR, ret.info.get(MapleStatInfo.indieAsrR));
                        ret.statups.put(MapleBuffStat.IndieTerR, ret.info.get(MapleStatInfo.indieTerR));
                        ret.statups.put(MapleBuffStat.IndieStance, ret.info.get(MapleStatInfo.indieStance));
                        ret.statups.put(MapleBuffStat.DotHealHPPerSecond, ret.info.get(MapleStatInfo.dotHealHPPerSecondR));
                        ret.statups.put(MapleBuffStat.DotHealMPPerSecond, ret.info.get(MapleStatInfo.dotHealMPPerSecondR));
                        break;
                    case 80001430: // 崩壞之輪
                    case 80001432: // 破滅之輪
                        ret.statups.put(MapleBuffStat.IndieEXP, ret.info.get(MapleStatInfo.indieExp));
                        ret.statups.put(MapleBuffStat.IndieBooster, ret.info.get(MapleStatInfo.indieBooster));
                        ret.statups.put(MapleBuffStat.IndieDamR, ret.info.get(MapleStatInfo.indieDamR));
                        break;
                    default:
                        break;
                }
            }

            if (MapleJob.isBeginner(sourceid / 10000)) {
                switch (sourceid % 10000) {
                    case 1087: // 黑天使
                        ret.statups.put(MapleBuffStat.IndiePAD, 10);
                        ret.statups.put(MapleBuffStat.IndieMAD, 10);
                        ret.statups.put(MapleBuffStat.Speed, 1);
                        break;
                    case 1085: // 大天使
                    case 1090: // 大天使
                        ret.statups.put(MapleBuffStat.IndiePAD, 5);
                        ret.statups.put(MapleBuffStat.IndieMAD, 5);
                        ret.statups.put(MapleBuffStat.Speed, 1);
                        break;
                    case 1179: // 白色天使
                        ret.statups.put(MapleBuffStat.IndiePAD, 12);
                        ret.statups.put(MapleBuffStat.IndieMAD, 12);
                        ret.statups.put(MapleBuffStat.Speed, 1);
                        break;
                    case 93: // 潛在開放
                        ret.statups.put(MapleBuffStat.AmplifyDamage, 1);
                        break;
                    case 1011: // 地火天爆Berserk fury
                        ret.statups.put(MapleBuffStat.DojangBerserk, ret.info.get(MapleStatInfo.x));
                        break;
                    case 1010: // 金剛不壞
                        ret.statups.put(MapleBuffStat.DojangInvincible, 1);
                        ret.statups.put(MapleBuffStat.DojangShield, 1);
                        break;
                    case 1001: // 治癒 || 潛入
                        if (sourceid / 10000 == 3001 || sourceid / 10000 == 3000) { // resistance is diff
                            ret.statups.put(MapleBuffStat.Sneak, ret.info.get(MapleStatInfo.x));
                        } else {
                            ret.statups.put(MapleBuffStat.Regen, ret.info.get(MapleStatInfo.x));
                        }
                        break;
                    case 1002: // 疾風之步
                        ret.statups.put(MapleBuffStat.Speed, ret.info.get(MapleStatInfo.speed));
                        break;
                    case 1005: // 英雄的回響Echo of Hero
                        ret.statups.put(MapleBuffStat.MaxLevelBuff, ret.info.get(MapleStatInfo.x));
                        break;
                    case 8001: // 實用的時空門
                        ret.statups.put(MapleBuffStat.SoulArrow, ret.info.get(MapleStatInfo.x));
                        break;
                    case 8002:// 實用的會心之眼
                        ret.statups.put(MapleBuffStat.SharpEyes, (ret.info.get(MapleStatInfo.x) << 8)
                                + ret.info.get(MapleStatInfo.y) + ret.info.get(MapleStatInfo.criticaldamageMax));
                        break;
                    case 8003: // 實用的神聖之火
                        ret.statups.put(MapleBuffStat.MaxMP, ret.info.get(MapleStatInfo.x));
                        ret.statups.put(MapleBuffStat.MaxHP, ret.info.get(MapleStatInfo.x));
                        break;
                    case 8004: // 有用的戰鬥命令
                        ret.statups.put(MapleBuffStat.CombatOrders, ret.info.get(MapleStatInfo.x));
                        break;
                    case 8005: // 有用的進階祝福
                        ret.statups.put(MapleBuffStat.AdvancedBless, 1);
                        break;
                    case 8006: // 有用的最終極速
                        ret.statups.put(MapleBuffStat.PartyBooster, ret.info.get(MapleStatInfo.x));
                        break;
                    case 103: // 冰砍
                        ret.monsterStatus.put(MonsterStatus.M_Stun, 1);
                        break;
                    case 99: // 寒冰碎擊
                    case 104: // 冰衝擊
                        ret.monsterStatus.put(MonsterStatus.M_Freeze, 1);
                        ret.info.put(MapleStatInfo.time, ret.info.get(MapleStatInfo.time) * 2); // freezing skills are a
                        // little strange
                        break;
                    case 1026: // 飛翔Soaring
                    case 1142: // 飛天騎乘Soaring
                        ret.statups.put(MapleBuffStat.Flying, 1);
                        break;
                }
            }
        }
        if (ret.isPoison()) {
            ret.monsterStatus.put(MonsterStatus.M_Poison, 1);
        }
        if (ret.getSummonMovementType() != null) {
            ret.statups.put(MapleBuffStat.SUMMON, 1);
        }
        if (ret.isMonsterRiding()) {
            ret.statups.put(MapleBuffStat.RideVehicle, GameConstants.getMountItem(ret.getSourceId(), null));
        }
        if (ret.isMorph()) {
            ret.statups.put(MapleBuffStat.Morph, ret.getMorph());
            if (ret.is狂龙变形()) {
                ret.statups.put(MapleBuffStat.Stance, ret.info.get(MapleStatInfo.prop));
                ret.statups.put(MapleBuffStat.Event, ret.info.get(MapleStatInfo.cr));
                ret.statups.put(MapleBuffStat.IndieBooster, ret.info.get(MapleStatInfo.indieBooster));
                ret.statups.put(MapleBuffStat.IndieDamR, ret.info.get(MapleStatInfo.indieDamR));
            }
        }

        if (ret.sourceid == 400021003) {
            int x = 1;
        }
        // OD用很爛的方式判定是不是BUFF，先試試看這個
        if (!ret.overTime) {
            ret.overTime = !ret.statups.isEmpty();
        }
        return ret;
    }

    /**
     * @param applyto
     * @param obj
     */
    public final void applyPassive(final MapleCharacter applyto, final MapleMapObject obj) {
        if (makeChanceResult() && !MapleJob.is惡魔殺手(applyto.getJob())) { // demon can't heal mp
            switch (sourceid) { // MP eater
                case 2100000:
                case 2200000:
                case 2300000:
                    if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                        return;
                    }
                    final MapleMonster mob = (MapleMonster) obj; // x is absorb percentage
                    if (!mob.getStats().isBoss()) {
                        final int absorbMp = Math.min((int) (mob.getMobMaxMp() * (getX() / 100.0)), mob.getMp());
                        if (absorbMp > 0) {
                            mob.setMp(mob.getMp() - absorbMp);
                            applyto.getStat().setMp(applyto.getStat().getMp() + absorbMp, applyto);
                            applyto.getClient().getSession().writeAndFlush(EffectPacket.showBuffEffect(true, applyto,
                                    sourceid, UserEffectOpcode.UserEffect_SkillUse, applyto.getLevel(), level));
                            applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffEffect(false, applyto, sourceid,
                                    UserEffectOpcode.UserEffect_SkillUse, applyto.getLevel(), level), false);
                        }
                    }
                    break;
            }
        }
    }

    public final boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, null, info.get(MapleStatInfo.time));
    }

    public final boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos, info.get(MapleStatInfo.time));
    }

    public final boolean applyTo(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary, Point pos, int newDuration) {
        if (isHeal() && (applyfrom.getMapId() == 749040100 || applyto.getMapId() == 749040100)) {
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return false; // z
        } else if ((isSoaring_Mount() && applyfrom.getBuffedValue(MapleBuffStat.RideVehicle) == null) || (isSoaring_Normal() && !applyfrom.getMap().canSoar())) {
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        } else if (sourceid == 4341006 && applyfrom.getBuffedValue(MapleBuffStat.ShadowPartner) == null) {
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        } else if (sourceid == 33101008 && (applyfrom.getBuffedValue(MapleBuffStat.Dance) == null || applyfrom.getBuffedValue(MapleBuffStat.SUMMON) != null || !applyfrom.canSummon())) {
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        } else if (isShadow() && applyfrom.getJob() / 100 % 10 != 4) { // pirate/shadow = dc
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        } else if (sourceid == 33101004 && applyfrom.getMap().isTown()) {
            applyfrom.dropMessage(5, "You may not use this skill in towns.");
            applyfrom.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
            return false;
        }
        int hpchange = calcHPChange(applyfrom, primary);
        int mpchange = calcMPChange(applyfrom, primary);
        final PlayerStats stat = applyto.getStat();
        if (primary) {
            if (info.get(MapleStatInfo.itemConNo) != 0 && !applyto.isClone() && !applyto.inPVP()) {
                if (!applyto.haveItem(info.get(MapleStatInfo.itemCon), info.get(MapleStatInfo.itemConNo), false, true)) {
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
                    return false;
                }
                MapleInventoryManipulator.removeById(applyto.getClient(), GameConstants.getInventoryType(info.get(MapleStatInfo.itemCon)), info.get(MapleStatInfo.itemCon), info.get(MapleStatInfo.itemConNo), false, true);
            }
        } else if (!primary && isResurrection()) {
            hpchange = stat.getMaxHp();
            applyto.setStance(0);
        }
        if (isDispel() && makeChanceResult()) {
            applyto.dispelDebuffs();
            if (sourceid == 2311001 && applyfrom.getParty() != null) {
                applyfrom.getParty().givePartyBuff(2311001, applyfrom.getId(), applyto.getId());
            } else {
                applyfrom.gainCooldownTime(2311012, -60);
            }
        } else if (isHeroWill()) {
            applyto.dispelDebuffs();
        } else if (skill && sourceid == 31221052) {
            overTime = true;
        } else if (skill && sourceid == 31011001) {
            int exceedOverload = applyto.getExceedOverload();
            int x = getX() * exceedOverload / 20;
            hpchange = stat.getMaxHp() / 100 * x;
        } else if (cureDebuffs.size() > 0) {
            for (final MapleDisease debuff : cureDebuffs) {
                applyfrom.dispelDebuff(debuff);
            }
        } else if (skill && sourceid == 36111008) { // 額外供應
            applyto.gainXenonSurplus(info.get(MapleStatInfo.x));
        } else if (skill && sourceid == 36121054) { // 阿瑪蘭斯發電機
            applyto.gainXenonSurplus(20);
        } else if (isMPRecovery()) {
            final int toDecreaseHP = ((stat.getMaxHp() / 100) * 10);
            if (stat.getHp() > toDecreaseHP) {
                hpchange += -toDecreaseHP; // -10% of max HP
                mpchange += ((toDecreaseHP / 100) * getY());
            } else {
                hpchange = stat.getHp() == 1 ? 0 : stat.getHp() - 1;
            }
        }
        final Map<MapleStat, Long> hpmpupdate = new EnumMap<>(MapleStat.class);
        if (applyto.isAlive()) {
            if (hpchange != 0) {
                if (hpchange < 0 && -hpchange > stat.getHp() && !applyto.hasDisease(MapleDisease.不死化) && skill) {
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
                    return false;
                }
                stat.setHp(stat.getHp() + hpchange, applyto);
            }
            if (mpchange != 0) {
                if (mpchange < 0 && -mpchange > stat.getMp() && skill) {
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
                    return false;
                }
                // short converting needs math.min cuz of overflow
                if ((mpchange < 0 && MapleJob.is惡魔殺手(applyto.getJob())) || !MapleJob.is惡魔殺手(applyto.getJob())) { // heal
                    stat.setMp(stat.getMp() + mpchange, applyto);
                }
                hpmpupdate.put(MapleStat.MP, Long.valueOf(stat.getMp()));
            }
            hpmpupdate.put(MapleStat.HP, Long.valueOf(stat.getHp()));
        }

        int powerchange = calcPowerChange(applyfrom);
        applyto.getClient().getSession().writeAndFlush(CWvsContext.updatePlayerStats(hpmpupdate, true, applyto));
        if (sourceid != 36001005 && powerchange != 0
                && applyto.getBuffedValue(MapleBuffStat.AmaranthGenerator) == null) {
            if (powerchange < 0 && -powerchange > applyto.getXenonSurplus()) {
                applyfrom.dropMessage(5, "使用技能時需要消耗的供給能源不足。");
                return false;
            }
            if (!statups.containsKey(MapleBuffStat.KeyDownMoving)
                    || applyto.getBuffedValue(MapleBuffStat.KeyDownMoving) == null) {
                applyto.gainXenonSurplus((short) powerchange);
            }
        }
        if (expinc != 0) {
            applyto.gainExp(expinc, true, true, false);
            applyto.getClient().getSession().writeAndFlush(EffectPacket.showDodgeChanceEffect());
        } else if (sourceid / 10000 == 238) {
            final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            final int mobid = ii.getCardMobId(sourceid);
            if (mobid > 0) {
                final boolean done = applyto.getMonsterBook().monsterCaught(applyto.getClient(), mobid,
                        MapleLifeFactory.getMonsterStats(mobid).getName());
                applyto.getClient().getSession().writeAndFlush(CWvsContext.getCard(done ? sourceid : 0, 1));
            }
        } else if (isReturnScroll()) {
            applyReturnScroll(applyto);
        } else if (useLevel > 0 && !skill) {
            applyto.setExtractor(new MapleExtractor(applyto, sourceid, useLevel * 50, 1440)); // no clue about time left
            applyto.getMap().spawnExtractor(applyto.getExtractor());
        } else if (isMistEruption()) {
            int i = info.get(MapleStatInfo.y);
            for (MapleAffectedArea m : applyto.getMap().getAllMistsThreadsafe()) {
                if (m.getOwnerId() == applyto.getId() && m.getSourceSkill().getId() == 2111003) {
                    if (m.getSchedule() != null) {
                        m.getSchedule().cancel(false);
                        m.setSchedule(null);
                    }
                    if (m.getPoisonSchedule() != null) {
                        m.getPoisonSchedule().cancel(false);
                        m.setPoisonSchedule(null);
                    }
                    applyto.getMap().broadcastMessage(CField.removeAffectedArea(m.getObjectId(), true));
                    applyto.getMap().removeMapObject(m);

                    i--;
                    if (i <= 0) {
                        break;
                    }
                }
            }
        } else if (cosmetic > 0) {
            if (cosmetic >= 30000) {
                applyto.setHair(cosmetic);
                applyto.updateSingleStat(MapleStat.HAIR, cosmetic);
            } else if (cosmetic >= 20000) {
                applyto.setFace(cosmetic);
                applyto.updateSingleStat(MapleStat.FACE, cosmetic);
            } else if (cosmetic < 100) {
                applyto.setSkinColor((byte) cosmetic);
                applyto.updateSingleStat(MapleStat.SKIN, cosmetic);
            }
            applyto.equipChanged();
        } else if (bs > 0) {
            if (!applyto.inPVP()) {
                return false;
            }
            final int x = Integer.parseInt(applyto.getEventInstance().getProperty(String.valueOf(applyto.getId())));
            applyto.getEventInstance().setProperty(String.valueOf(applyto.getId()), String.valueOf(x + bs));
            applyto.getClient().getSession().writeAndFlush(CField.getPVPScore(x + bs, false));
        } else if (info.get(MapleStatInfo.iceGageCon) > 0) {
            if (!applyto.inPVP()) {
                return false;
            }
            final int x = Integer.parseInt(applyto.getEventInstance().getProperty("icegage"));
            if (x < info.get(MapleStatInfo.iceGageCon)) {
                return false;
            }
            applyto.getEventInstance().setProperty("icegage", String.valueOf(x - info.get(MapleStatInfo.iceGageCon)));
            applyto.getClient().getSession().writeAndFlush(CField.getPVPIceGage(x - info.get(MapleStatInfo.iceGageCon)));
            applyto.applyIceGage(x - info.get(MapleStatInfo.iceGageCon));
        } else if (recipe > 0) {
            if (applyto.getTotalSkillLevel(recipe) > 0 || applyto.getProfessionLevel((recipe / 10000) * 10000) < reqSkillLevel) {
                return false;
            }
            applyto.changeSingleSkillLevel(SkillFactory.getCraft(recipe), Integer.MAX_VALUE, recipeUseCount,
                    recipeValidDay > 0 ? (System.currentTimeMillis() + recipeValidDay * 24L * 60 * 60 * 1000) : -1L);
        } else if (isComboRecharge()) {
            PlayerHandler.AranCombo(applyto, info.get(MapleStatInfo.y));
            applyto.getClient().getSession().writeAndFlush(CField.rechargeCombo(applyto.getCombo()));
            SkillFactory.getSkill(21000000).getEffect(10).applyComboBuff(applyto, applyto.getCombo());
        } else if (isDragonBlink()) {
            final MaplePortal portal = applyto.getMap().getPortal(Randomizer.nextInt(applyto.getMap().getPortals().size()));
            if (portal != null) {
                applyto.getClient().getSession().writeAndFlush(CField.dragonBlink(portal.getId()));
                applyto.getMap().movePlayer(applyto, portal.getPosition());
                applyto.checkFollow();
            }
        } else if (isSpiritClaw() && !applyto.isClone()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            boolean itemz = false;
            for (int i = 0; i <= use.getSlotLimit(); i++) { // impose order...
                Item item = use.getItem((byte) i);
                if (item != null) {
                    if (ItemConstants.類型.飛鏢(item.getItemId()) && item.getQuantity() >= 100) {
                        MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, (short) i,
                                (short) 100, false, true);
                        itemz = true;
                        break;
                    }
                }
            }
            if (!itemz) {
                return false;
            }
        } else if (isSpiritBlast() && !applyto.isClone()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            boolean itemz = false;
            for (int i = 0; i <= use.getSlotLimit(); i++) { // impose order...
                Item item = use.getItem((byte) i);
                if (item != null) {
                    if (ItemConstants.類型.子彈(item.getItemId()) && item.getQuantity() >= 100) {
                        MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, (short) i,
                                (short) 100, false, true);
                        itemz = true;
                        break;
                    }
                }
            }
            if (!itemz) {
                return false;
            }
        } else if (sourceid == 14111025 && !applyto.isClone()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            int Consume = getBulletConsume();
            boolean itemz = false;
            for (int i = 0; i <= use.getSlotLimit(); i++) { // impose order...
                Item item = use.getItem((byte) i);
                if (item != null) {
                    if (ItemConstants.類型.飛鏢(item.getItemId()) && item.getQuantity() >= Consume) {
                        MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, (short) i,
                                (short) Consume, false, true);
                        itemz = true;
                        break;
                    }
                }
            }
            if (!itemz) {
                return false;
            }

        } else if (cp != 0 && applyto.getCarnivalParty() != null) {
            applyto.getCarnivalParty().addCP(applyto, cp);
            applyto.CPUpdate(false, applyto.getAvailableCP(), applyto.getTotalCP(), 0);
            for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                chr.CPUpdate(true, applyto.getCarnivalParty().getAvailableCP(), applyto.getCarnivalParty().getTotalCP(),
                        applyto.getCarnivalParty().getTeam());
            }
        } else if (nuffSkill != 0 && applyto.getParty() != null) {
            final MCSkill skil = MapleCarnivalFactory.getInstance().getSkill(nuffSkill);
            if (skil != null) {
                final MapleDisease dis = skil.getDisease();
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (applyto.getParty() == null || chr.getParty() == null
                            || (chr.getParty().getId() != applyto.getParty().getId())) {
                        if (skil.targetsAll || Randomizer.nextBoolean()) {
                            if (dis == null) {
                                chr.dispel();
                            } else if (skil.getSkill() == null) {
                                chr.giveDebuff(dis, 1, 30000, dis.getDisease(), 1);
                            } else {
                                chr.giveDebuff(dis, skil.getSkill());
                            }
                            if (!skil.targetsAll) {
                                break;
                            }
                        }
                    }
                }
            }
        } else if ((effectedOnEnemy > 0 || effectedOnAlly > 0) && primary && applyto.inPVP()) {
            final int eventType = Integer.parseInt(applyto.getEventInstance().getProperty("type"));
            if (eventType > 0 || effectedOnEnemy > 0) {
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (chr.getId() != applyto.getId() && (effectedOnAlly > 0 ? (chr.getTeam() == applyto.getTeam())
                            : (chr.getTeam() != applyto.getTeam() || eventType == 0))) {
                        applyTo(applyto, chr, false, pos, newDuration);
                    }
                }
            }
        } else if (mobSkill > 0 && mobSkillLevel > 0 && primary && applyto.inPVP()) {
            if (effectedOnEnemy > 0) {
                final int eventType = Integer.parseInt(applyto.getEventInstance().getProperty("type"));
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (chr.getId() != applyto.getId() && (chr.getTeam() != applyto.getTeam() || eventType == 0)) {
                        chr.disease(mobSkill, mobSkillLevel);
                    }
                }
            } else if (sourceid == 2910000 || sourceid == 2910001) { // red flag
                applyto.getClient().getSession().writeAndFlush(EffectPacket.showBuffEffect(true, applyto, sourceid,
                        UserEffectOpcode.UserEffect_PlayPortalSE, applyto.getLevel(), level));
                applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffEffect(false, applyto, sourceid,
                        UserEffectOpcode.UserEffect_PlayPortalSE, applyto.getLevel(), level), false);

                applyto.getClient().getSession().writeAndFlush(EffectPacket
                        .showCraftingEffect("UI/UIWindow2.img/CTF/Effect", (byte) applyto.getDirection(), 0, 0));
                applyto.getMap().broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto,
                        "UI/UIWindow2.img/CTF/Effect", (byte) applyto.getDirection(), 0, 0), false);
                if (applyto.getTeam() == (sourceid - 2910000)) { // restore duh flag
                    if (sourceid == 2910000) {
                        applyto.getEventInstance().broadcastPlayerMsg(-7, "The Red Team's flag has been restored.");
                    } else {
                        applyto.getEventInstance().broadcastPlayerMsg(-7, "The Blue Team's flag has been restored.");
                    }
                    applyto.getMap().spawnAutoDrop(sourceid,
                            applyto.getMap().getGuardians().get(sourceid - 2910000).left);
                } else {
                    applyto.disease(mobSkill, mobSkillLevel);
                    if (sourceid == 2910000) {
                        applyto.getEventInstance().setProperty("redflag", String.valueOf(applyto.getId()));
                        applyto.getEventInstance().broadcastPlayerMsg(-7, "The Red Team's flag has been captured!");
                        applyto.getClient().getSession().writeAndFlush(EffectPacket.showCraftingEffect(
                                "UI/UIWindow2.img/CTF/Tail/Red", (byte) applyto.getDirection(), 600000, 0));
                        applyto.getMap()
                                .broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto,
                                        "UI/UIWindow2.img/CTF/Tail/Red", (byte) applyto.getDirection(), 600000, 0),
                                        false);
                    } else {
                        applyto.getEventInstance().setProperty("blueflag", String.valueOf(applyto.getId()));
                        applyto.getEventInstance().broadcastPlayerMsg(-7, "The Blue Team's flag has been captured!");
                        applyto.getClient().getSession().writeAndFlush(EffectPacket.showCraftingEffect(
                                "UI/UIWindow2.img/CTF/Tail/Blue", (byte) applyto.getDirection(), 600000, 0));
                        applyto.getMap()
                                .broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto,
                                        "UI/UIWindow2.img/CTF/Tail/Blue", (byte) applyto.getDirection(), 600000, 0),
                                        false);
                    }
                }
            } else {
                applyto.disease(mobSkill, mobSkillLevel);
            }
        } else if (randomPickup != null && randomPickup.size() > 0) {
            MapleItemInformationProvider.getInstance()
                    .getItemEffect(randomPickup.get(Randomizer.nextInt(randomPickup.size()))).applyTo(applyto);
        } else if (sourceid == 20031203 || sourceid == 20021110 || sourceid == 80001040) {
            applyto.changeMap(sourceid == 20031203 ? 150000000
                    : sourceid == 20021110 || sourceid == 80001040 ? 101050000 : 100000000, 0);
        }
        for (Entry<MapleTraitType, Integer> t : traits.entrySet()) {
            applyto.getTrait(t.getKey()).addExp(t.getValue(), applyto);
        }
        if (sourceid == 3111002) {
            final Skill elite = SkillFactory.getSkill(3120012);
            if (applyfrom.getTotalSkillLevel(elite) > 0) {
                return elite.getEffect(applyfrom.getTotalSkillLevel(elite)).applyTo(applyfrom, applyto, primary, pos,
                        newDuration);
            }
        } else if (sourceid == 3211002) {
            final Skill elite = SkillFactory.getSkill(3220012);
            if (applyfrom.getTotalSkillLevel(elite) > 0) {
                return elite.getEffect(applyfrom.getTotalSkillLevel(elite)).applyTo(applyfrom, applyto, primary, pos,
                        newDuration);
            }
        }
        if (isMechDoor()) {
            int newId = 0;
            boolean applyBuff = false;
            if (applyto.getMechDoors().size() >= 2) {
                final MapleOpenGate remove = applyto.getMechDoors().remove(0);
                newId = remove.getId();
                applyto.getMap().broadcastMessage(SkillPacket.removeOpenGate(remove, true));
                applyto.getMap().removeMapObject(remove);
            } else {
                for (MapleOpenGate d : applyto.getMechDoors()) {
                    if (d.getId() == newId) {
                        applyBuff = true;
                        newId = 1;
                        break;
                    }
                }
            }
            final MapleOpenGate door = new MapleOpenGate(applyto,
                    new Point(pos == null ? applyto.getTruePosition() : pos), newId);
            applyto.getMap().spawnMechDoor(door);
            applyto.addMechDoor(door);
            applyto.getClient().getSession().writeAndFlush(SkillPacket.mechPortal(door.getTruePosition()));
            if (!applyBuff) {
                return true; // do not apply buff until 2 doors spawned
            }
        }

        if (primary && availableMap != null) {
            for (Pair<Integer, Integer> e : availableMap) {
                if (applyto.getMapId() < e.left || applyto.getMapId() > e.right) {
                    applyto.getClient().getSession().writeAndFlush(CWvsContext.enableActions());
                    return true;
                }
            }
        }

        // 處理BUFF的地方
        if (getSummonMovementType() != null) {
            if (sourceid == 2111010) {
                handle綠水靈病毒(applyfrom, newDuration);
            }
            applySummonEffect(applyfrom, primary, pos, newDuration, 0);
        } else if (overTime && !isEnergyCharge()) {
            applyBuffEffect(applyfrom, applyto, primary, newDuration);
        }
        if (skill) {
            removeMonsterBuff(applyfrom);
        }
        if (primary) {
            if ((overTime || isHeal()) && !isEnergyCharge()) {
                applyBuff(applyfrom, newDuration);
            }
            if (isMonsterBuff()) {
                applyMonsterBuff(applyfrom);
            }
        }
        if (isMagicDoor()) { // 時空門Magic Door
            MapleDoor door = new MapleDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos), sourceid); // Current
            // Map
            // door
            if (door.getTownPortal() != null) {

                applyto.getMap().spawnDoor(door);
                applyto.addDoor(door);

                MapleDoor townDoor = new MapleDoor(door); // Town door
                applyto.addDoor(townDoor);
                door.getTown().spawnDoor(townDoor);

                door.first = false;

                if (applyto.getParty() != null) { // update town doors
                    applyto.silentPartyUpdate();
                }
            } else {
                applyto.dropMessage(5, "村莊裡已經沒有可以創造時空門的位置。");
            }
        } else if (isMist()) {
            int addx = 0;
            if (sourceid == 35121052 || sourceid == 33121012) {
                addx = -600;
                pos = pos != null ? pos : applyfrom.getPosition();
                pos = new Point(pos.x, pos.y + 70);
            }
            final Rectangle bounds = calculateBoundingBox(pos != null ? pos : applyfrom.getPosition(),
                    applyfrom.isFacingLeft(), addx);
            final MapleAffectedArea mist = new MapleAffectedArea(bounds, applyfrom, this);
            applyfrom.getMap().spawnAffectedArea(mist, getDuration(), false);
        } else if (isTimeLeap()) { // Time Leap
            applyto.getCooldowns().stream().filter((i) -> (i.skillId != 5121010)).forEach((i) -> {
                applyto.removeCooldown(i.skillId);
            });
        } else {
            for (WeakReference<MapleCharacter> chrz : applyto.getClones()) {
                if (chrz.get() != null) {
                    applyTo(chrz.get(), chrz.get(), primary, pos, newDuration);
                }
            }
        }
        if (fatigueChange != 0 && applyto.getSummonedFamiliar() != null
                && (familiars == null || familiars.contains(applyto.getSummonedFamiliar().getFamiliar()))) {
            // applyto.getSummonedFamiliar().addFatigue(applyto, fatigueChange);
        }
        if (rewardMeso != 0) {
            applyto.gainMeso(rewardMeso, false);
        }
        if (rewardItem != null && totalprob > 0) {
            for (Triple<Integer, Integer, Integer> reward : rewardItem) {
                if (MapleInventoryManipulator.checkSpace(applyto.getClient(), reward.left, reward.mid, "")
                        && reward.right > 0 && Randomizer.nextInt(totalprob) < reward.right) { // Total prob
                    if (GameConstants.getInventoryType(reward.left) == MapleInventoryType.EQUIP) {
                        final Item item = MapleItemInformationProvider.getInstance().getEquipById(reward.left);
                        item.setGMLog(
                                "Reward item (effect): " + sourceid + " 時間:" + FileoutputUtil.CurrentReadable_Date());
                        MapleInventoryManipulator.addbyItem(applyto.getClient(), item);
                    } else {
                        MapleInventoryManipulator.addById(applyto.getClient(), reward.left, reward.mid.shortValue(),
                                "Reward item (effect): " + sourceid + " 時間:" + FileoutputUtil.CurrentReadable_Date());
                    }
                }
            }
        }
        if (familiarTarget == 2 && applyfrom.getParty() != null && primary) { // to party
            for (MaplePartyCharacter mpc : applyfrom.getParty().getMembers()) {
                if (mpc.getId() != applyfrom.getId() && mpc.getChannel() == applyfrom.getClient().getChannel()
                        && mpc.getMapid() == applyfrom.getMapId() && mpc.isOnline()) {
                    MapleCharacter mc = applyfrom.getMap().getCharacterById(mpc.getId());
                    if (mc != null) {
                        applyTo(applyfrom, mc, false, null, newDuration);
                    }
                }
            }
        } else if (familiarTarget == 3 && primary) {
            for (MapleCharacter mc : applyfrom.getMap().getCharactersThreadsafe()) {
                if (mc.getId() != applyfrom.getId()) {
                    applyTo(applyfrom, mc, false, null, newDuration);
                }
            }
        }
        if (GameConstants.isTownSkill(sourceid)) {
            applyto.changeMap(info.get(MapleStatInfo.x), 0);
        }
        return true;
    }

    public void handle綠水靈病毒(final MapleCharacter applyto, final int time) {
        final java.util.Timer timer = new java.util.Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                for (MapleSummon x : applyto.getAllLinksummon()) {
                    applyto.getMap().removeSummon(x.getObjectId());
                }
                applyto.getAllLinksummon().clear();
                timer.cancel();
            }
        };
        timer.schedule(task, time);
    }

    /**
     * 處理召喚獸效果
     *
     * @param applyto 角色
     * @param primary
     * @param pos 坐標
     * @param newDuration 時間
     * @param monid 怪物的ID
     * @return boolean
     */
    public boolean applySummonEffect(MapleCharacter applyto, boolean primary, Point pos, int newDuration, int monid) {
        final SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType == null
                || (this.sourceid == 32111006 && applyto.getBuffedValue(MapleBuffStat.Revive) == null)
                || applyto.isClone()) {
            return false;
        }
        byte[] buff = null;
        int summId = sourceid;
        int localDuration = newDuration;
        Map<MapleBuffStat, Integer> localstatups = new HashMap<>(statups);
        summId = GameConstants.getMountSkill(summId, applyto);
        if (applyto.isShowInfo()) {
            applyto.showMessage(10, "開始召喚召喚獸 - 召喚獸技能: " + summId + " 持續時間: " + newDuration);
        }
        if (sourceid == 32111006) {
            localstatups.put(MapleBuffStat.Revive, 1);
        }
        // 召喚船員
        if (skill && (sourceid == 5201012 || sourceid == 5210015)) {
            List<Integer> skillIds = new ArrayList<>();
            for (int i = 0; i < (sourceid == 5201012 ? 3 : 4); i++) {
                skillIds.add(sourceid + i);
            }
            summId = skillIds.get(Randomizer.nextInt(skillIds.size()));
            if (sourceid == 5210015) {
                skillIds.remove(summId);
                int skillid = skillIds.get(Randomizer.nextInt(skillIds.size()));
                Point p2 = new Point(pos == null ? applyto.getPosition() : pos);
                p2.x -= 90;
                final MapleSummon sum = new MapleSummon(applyto, skillid, getLevel(), p2, summonMovementType);
                sum.setLinkmonid(monid);
                applyto.getMap().spawnSummon(sum);
                applyto.addSummon(sum);
            }
        } else if (skill && (sourceid == 23111008)) {
            // 元素騎士
            summId = sourceid + Randomizer.nextInt(3);
        }
        final MapleSummon tosummon = new MapleSummon(applyto, summId, getLevel(),
                new Point(pos == null ? applyto.getPosition() : pos), summonMovementType);
        if (!tosummon.isMultiSummon()) {
            applyto.cancelEffect(this, true, -1, localstatups);
        }
        if (!tosummon.isPuppet()) {
            applyto.getCheatTracker().resetSummonAttack();
        }
        tosummon.setLinkmonid(monid);
        if (sourceid == 42111003) { // 鬼神召喚
            MapleFootholdTree fhTree = applyto.getMap().getFootholds();
            Point p = tosummon.getPosition();
            Point p2 = new Point(p);
            Pair<MapleFoothold, MapleFoothold> border = fhTree.findFloorBorder(p);
            int left = 400;
            if (border != null && p != null) {
                if (p.x - left < border.left.getX1()) {
                    left = p.x - border.left.getX1();
                }
                if (p.x - left + 800 > border.right.getX2() && left == 400) {
                    left = p.x + 800 - border.right.getX2();
                }
            }
            p.x -= left;
            p2.x = p2.x - left + 800;
            // 調整第一個(左邊)的位置
            if (border != null && p.x < border.left.getX1()) {
                p.x = border.left.getX1();
            }
            MapleFoothold fh = applyto.getMap().getFootholds().findBelow(p, false);
            tosummon.setFh(fh == null ? 0 : fh.getId());
            tosummon.setPosition(p);
            tosummon.setStance(tosummon.getStance() & 0xFE);
            applyto.getMap().spawnSummon(tosummon);
            applyto.addSummon(tosummon);

            // 召喚第二個(右邊)
            if (border != null && p2.x > border.right.getX2()) {
                p2.x = border.right.getX2();
            }
            final MapleSummon sum = new MapleSummon(applyto, summId, getLevel(), p2, summonMovementType);
            sum.setLinkmonid(monid);
            sum.setStance(sum.getStance() | 1);
            applyto.getMap().spawnSummon(sum);
            applyto.addSummon(sum);
            applyto.getClient().getSession().writeAndFlush(CField.summonTeam(tosummon, sum));
        } else {
            applyto.getMap().spawnSummon(tosummon);
            applyto.addSummon(tosummon);
        }

        if ((info.get(MapleStatInfo.hcSummonHp)) > 0) {
            tosummon.addHP(info.get(MapleStatInfo.hcSummonHp).shortValue());
        } else if (this.sourceid == 3221014) {
            tosummon.addHP(info.get(MapleStatInfo.x).shortValue());
        }
        if (sourceid == 42100010) { // 式神炎舞 - 召喚獸
            Point p = pos == null ? applyto.getPosition() : pos;
            int x = p.x;
            for (int i = 0; i < 4; i++) {
                x += (applyto.isFacingLeft() ? -1 : 1) * 100;
                final MapleSummon sum = new MapleSummon(applyto, summId, getLevel(), new Point(x, p.y),
                        summonMovementType);
                applyto.getMap().spawnSummon(sum);
                applyto.addSummon(sum);
            }
        }
        long startTime = System.currentTimeMillis();
        if (localDuration > 0 || localDuration == -1) {
            CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, startTime, localstatups);
            ScheduledFuture schedule = Timer.BuffTimer.getInstance().schedule(cancelAction,
                    localDuration == -1 ? Long.MAX_VALUE : localDuration);
            applyto.registerEffect(this, startTime, schedule, localstatups, false, localDuration, applyto.getId());
        }
        if (localDuration == -1) {
            localDuration = 0;
        }

        Map<MapleBuffStat, Integer> stat = new HashMap<>();
        if (sourceid == 13120007) {
            applyto.dispelSkill(13111024);
        } else if (sourceid == 4341006) {
            applyto.cancelEffectFromBuffStat(MapleBuffStat.ShadowPartner);
        } else if (this.statups != null) {
            buff = BuffPacket.giveBuff(this.sourceid, localDuration, statups, this, applyto);
        }
        if (sourceid == 2111010) {
            applyto.addLinksummon(tosummon);
        }
        int cooldown = getCooldown(applyto);
        if (cooldown > 0) {
            if (sourceid == 35111002) {
                List<Integer> count = new ArrayList<>();
                final List<MapleSummon> summons = applyto.getSummonsReadLock();
                try {
                    for (MapleSummon summon : summons) {
                        if (summon.getSkill() == sourceid) {
                            count.add(summon.getObjectId());
                        }
                    }
                } finally {
                    applyto.unlockSummonsReadLock();
                }
                if (count.size() == 3) {
                    applyto.addCooldown(sourceid, startTime, cooldown * 1000);
                    applyto.getMap().broadcastMessage(
                            SkillPacket.teslaTriangle(applyto.getId(), count.get(0), count.get(1), count.get(2)));
                }
            } else {
                applyto.addCooldown(sourceid, startTime, cooldown * 1000);
            }
        }
        if (buff != null) {
            applyto.getClient().getSession().writeAndFlush(buff);
        }
        return true;
    }

    public final boolean applyReturnScroll(final MapleCharacter applyto) {
        if (moveTo != -1) {
            if (applyto.getMap().getReturnMapId() != applyto.getMapId() || sourceid == 2031010 || sourceid == 2030021) {
                MapleMap target;
                if (moveTo == 999999999) {
                    target = applyto.getMap().getReturnMap();
                } else {
                    target = ChannelServer.getInstance(applyto.getClient().getChannel()).getMapFactory().getMap(moveTo);
                    if (target.getId() / 10000000 != 60 && applyto.getMapId() / 10000000 != 61) {
                        if (target.getId() / 10000000 != 21 && applyto.getMapId() / 10000000 != 20) {
                            if (target.getId() / 10000000 != applyto.getMapId() / 10000000) {
                                return false;
                            }
                        }
                    }
                }
                applyto.changeMap(target, target.getPortal(0));
                return true;
            }
        }
        return false;
    }

    private boolean isSoulStone() {
        return skill && sourceid == 22181003;
    }

    private void applyBuff(final MapleCharacter applyfrom, int newDuration) {
        List<MapleCharacter> awarded = new ArrayList<>();

        if (isSoulStone()) {
            if (applyfrom.getParty() != null) {
                int membrs = 0;
                for (MapleCharacter chr : applyfrom.getMap().getCharactersThreadsafe()) {
                    if (!chr.isClone() && chr.getParty() != null
                            && chr.getParty().getId() == applyfrom.getParty().getId() && chr.isAlive()) {
                        membrs++;
                    }
                }
                while (awarded.size() < Math.min(membrs, info.get(MapleStatInfo.y))) {
                    for (MapleCharacter chr : applyfrom.getMap().getCharactersThreadsafe()) {
                        if (chr != null && !chr.isClone() && chr.isAlive() && chr.getParty() != null
                                && chr.getParty().getId() == applyfrom.getParty().getId() && !awarded.contains(chr)
                                && Randomizer.nextInt(info.get(MapleStatInfo.y)) == 0) {
                            awarded.add(chr);
                        }
                    }
                }
            }
        } else if (isPartyBuff() && (applyfrom.getParty() != null || isGmBuff() || applyfrom.inPVP())) {
            final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
            final List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.PLAYER));

            for (final MapleMapObject affectedmo : affecteds) {
                final MapleCharacter affected = (MapleCharacter) affectedmo;

                if (affected.getId() != applyfrom.getId() && (isGmBuff()
                        || (applyfrom.inPVP() && affected.getTeam() == applyfrom.getTeam()
                        && Integer.parseInt(applyfrom.getEventInstance().getProperty("type")) != 0)
                        || (applyfrom.getParty() != null && affected.getParty() != null
                        && applyfrom.getParty().getId() == affected.getParty().getId()))) {
                    awarded.add(affected);
                }
            }
        }

        for (MapleCharacter chr : awarded) {
            if (isPartyBuff() && chr.getParty() != null && !isHeal()) {
                chr.getParty().givePartyBuff(sourceid, applyfrom.getId(), chr.getId());
            }
            if ((isResurrection() && !chr.isAlive()) || (!isResurrection() && chr.isAlive())) {
                applyTo(applyfrom, chr, false, null, newDuration);
                chr.getClient().getSession().writeAndFlush(EffectPacket.showBuffEffect(true, chr, sourceid,
                        UserEffectOpcode.UserEffect_SkillUseBySummoned, applyfrom.getLevel(), level));
                chr.getMap().broadcastMessage(chr, EffectPacket.showBuffEffect(false, chr, sourceid,
                        UserEffectOpcode.UserEffect_SkillUseBySummoned, applyfrom.getLevel(), level), false);
            }
            if (isTimeLeap()) {
                for (MapleCoolDownValueHolder i : chr.getCooldowns()) {
                    if (i.skillId != 5121010) {
                        chr.removeCooldown(i.skillId);
                    }
                }
            }
        }

        if (isDispel()) {
            if (applyfrom.getParty() == null) {
                return;
            }
            int time = applyfrom.getParty().getPartyBuffs(applyfrom.getId()) * 60;
            if (time > 0) {
                applyfrom.gainCooldownTime(2311012, -time);
            }
            for (MaplePartyCharacter mc : applyfrom.getParty().getMembers()) {
                applyfrom.getParty().cancelPartyBuff(sourceid, mc.getId());
            }
        } else if (isPartyBuff() && !isHeal() && !isResurrection()) {
            if (applyfrom.getParty() != null) {
                applyfrom.getParty().givePartyBuff(sourceid, applyfrom.getId(), applyfrom.getId());
            }
            MapleStatEffect.applyPassiveBless(applyfrom);
        }
    }

    private void removeMonsterBuff(final MapleCharacter applyfrom) {
        List<MonsterStatus> cancel = new ArrayList<>();
        switch (sourceid) {
            case 1111007:
            case 51111005: // Mihile's magic crash
            case 1211009:
            case 1311007:
                cancel.add(MonsterStatus.M_PGuardUp);
                cancel.add(MonsterStatus.M_MGuardUp);
                cancel.add(MonsterStatus.M_PowerUp);
                cancel.add(MonsterStatus.M_MagicUp);
                break;
            default:
                return;
        }
        final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
        final List<MapleMapObject> affected = applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.MONSTER));
        int i = 0;

        for (final MapleMapObject mo : affected) {
            if (makeChanceResult()) {
                for (MonsterStatus stat : cancel) {
                    ((MapleMonster) mo).cancelStatus(stat);
                }
            }
            i++;
            if (i >= info.get(MapleStatInfo.mobCount)) {
                break;
            }
        }
    }

    public final void applyMonsterBuff(final MapleCharacter applyfrom) {
        final Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
        final boolean pvp = applyfrom.inPVP();
        final MapleMapObjectType objType = pvp ? MapleMapObjectType.PLAYER : MapleMapObjectType.MONSTER;
        final List<MapleMapObject> affected = sourceid == 35111005
                ? applyfrom.getMap().getMapObjectsInRange(applyfrom.getTruePosition(), Double.POSITIVE_INFINITY, Collections.singletonList(objType))
                : applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(objType));
        int i = 0;

        for (final MapleMapObject mo : affected) {
            if (makeChanceResult()) {
                for (Map.Entry<MonsterStatus, Integer> stat : getMonsterStati().entrySet()) {
                    if (pvp) {
                        MapleCharacter chr = (MapleCharacter) mo;
                        MapleDisease d = MonsterStatus.getLinkedDisease(stat.getKey());
                        if (d != null) {
                            chr.giveDebuff(d, stat.getValue(), getDuration(), d.getDisease(), 1);
                        }
                    } else {
                        MapleMonster mons = (MapleMonster) mo;
                        if (sourceid == 35111005 && mons.getStats().isBoss()) {
                            break;
                        }
                        mons.applyStatus(applyfrom,
                                new MonsterStatusEffect(stat.getKey(), stat.getValue(), sourceid, null, false),
                                isPoison(), isSubTime(sourceid) ? getSubTime() : getDuration(), true, this);
                    }
                }
                if (pvp && skill) {
                    MapleCharacter chr = (MapleCharacter) mo;
                    handleExtraPVP(applyfrom, chr);
                }
            }
            i++;
            if (i >= info.get(MapleStatInfo.mobCount) && sourceid != 35111005) {
                break;
            }
        }
    }

    public final boolean isSubTime(final int source) {
        switch (source) {
            case 1201006: // threaten
            case 23111008: // 元素騎士
            case 23111009: // 元素騎士
            case 23111010: // 元素騎士
            case 31101003: // 黑暗復仇
            case 31121003: // 魔力吶喊
            case 31121005: // 變形
                return true;// u there?
        }
        return false;
    }

    public final void handleExtraPVP(MapleCharacter applyfrom, MapleCharacter chr) {
        if (sourceid == 2311005 || sourceid == 5121005 || sourceid == 1201006
                || (MapleJob.isBeginner(sourceid / 10000) && sourceid % 10000 == 104)) { // doom, threaten, snatch
            final long starttime = System.currentTimeMillis();

            final int localsourceid = sourceid == 5121005 ? 90002000 : sourceid;
            final Map<MapleBuffStat, Integer> localstatups = new EnumMap<>(MapleBuffStat.class);
            switch (sourceid) {
                case 2311005:
                    localstatups.put(MapleBuffStat.Morph, 7);
                    break;
                case 1201006:
                    localstatups.put(MapleBuffStat.Team, (int) level);
                    break;
                case 5121005:
                    localstatups.put(MapleBuffStat.AmplifyDamage, 1);
                    break;
                default:
                    localstatups.put(MapleBuffStat.Morph, info.get(MapleStatInfo.x));
                    break;
            }
            chr.registerEffect(this, starttime,
                    BuffTimer.getInstance().schedule(new CancelEffectAction(chr, this, starttime, localstatups),
                            isSubTime(sourceid) ? getSubTime() : getDuration()),
                    localstatups, false, getDuration(), applyfrom.getId());
            chr.getClient().getSession()
                    .writeAndFlush(BuffPacket.giveBuff(localsourceid, getDuration(), localstatups, this, chr));
        }
    }

    public final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft) {
        return calculateBoundingBox(posFrom, facingLeft, lt, rb, info.get(MapleStatInfo.range));
    }

    public final Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft, int addedRange) {
        return calculateBoundingBox(posFrom, facingLeft, lt, rb, info.get(MapleStatInfo.range) + addedRange);
    }

    public static Rectangle calculateBoundingBox(final Point posFrom, final boolean facingLeft, final Point lt,
            final Point rb, final int range) {
        if (lt == null || rb == null) {
            return new Rectangle((facingLeft ? (-200 - range) : 0) + posFrom.x, (-100 - range) + posFrom.y, 200 + range,
                    100 + range);
        }
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x - range, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x + range, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    public final double getMaxDistanceSq() { // lt = infront of you, rb = behind you; not gonna distanceSq the two
        // points since this is in relative to player position which is (0,0) and
        // not both directions, just one
        final int maxX = Math.max(Math.abs(lt == null ? 0 : lt.x), Math.abs(rb == null ? 0 : rb.x));
        final int maxY = Math.max(Math.abs(lt == null ? 0 : lt.y), Math.abs(rb == null ? 0 : rb.y));
        return (maxX * maxX) + (maxY * maxY);
    }

    public final void setDuration(int d) {
        this.info.put(MapleStatInfo.time, d);
    }

    public final void applyKaiser_Combo(MapleCharacter applyto, short combo) {
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        this.combo = combo;
        stat.put(MapleBuffStat.SmashStack, (int) combo);
        applyto.getClient().getSession().writeAndFlush(CWvsContext.BuffPacket.giveBuff(0, 0, stat, this, applyto));
    }

    public final void applyXenon_Combo(MapleCharacter applyto, int combo) {
        this.combo = combo;
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        stat.put(MapleBuffStat.SurplusSupply, combo);
        applyto.getClient().getSession().writeAndFlush(CWvsContext.BuffPacket.giveBuff(0, 0, stat, this, applyto));
    }

    public final void applyComboBuff(MapleCharacter applyto, short combo) {
        this.combo = combo;
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        stat.put(MapleBuffStat.ComboAbilityBuff, (int) combo);

        long starttime = System.currentTimeMillis();

        applyto.registerEffect(this, starttime, null, applyto.getId());
        applyto.getClient().getSession()
                .writeAndFlush(CWvsContext.BuffPacket.giveBuff(this.sourceid, 0, stat, this, applyto));
    }

    public final void applyBlackBlessingBuff(MapleCharacter applyto, int combo) {
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        stat.put(MapleBuffStat.BlessOfDarkness, combo);
        applyto.getClient().getSession()
                .writeAndFlush(CWvsContext.BuffPacket.giveBuff(this.sourceid, 0, stat, this, applyto));
        applyto.getClient().getSession().writeAndFlush(CField.EffectPacket.showBlackBlessingEffect(applyto, 27100003));
    }

    public final void applyLunarTideBuff(MapleCharacter applyto) {
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        double hpx = applyto.getStat().getMaxHp() / applyto.getStat().getHp();
        double mpx = applyto.getStat().getMaxMp() / applyto.getStat().getMp();
        stat.put(MapleBuffStat.LifeTidal, hpx >= mpx ? 2 : 1);
        applyto.getClient().getSession()
                .writeAndFlush(CWvsContext.BuffPacket.giveBuff(this.sourceid, 0, stat, this, applyto));
    }

    public final void applyEnergyBuff(final MapleCharacter applyto, final boolean infinity, int targets) {
        final long starttime = System.currentTimeMillis();
        if (infinity) {
            applyto.registerEffect(this, starttime, null, applyto.getId());
            // applyto.getClient().getSession().writeAndFlush(BuffPacket.giveEnergyChargeTest(0,
            // info.get(MapleStatInfo.time) / 1000, targets));
            // applyto.getAllEffects().add(new Pair<>(MapleBuffStat.EnergyCharged, new
            // MapleBuffStatValueHolder(this, starttime, null, targets, 0,
            // applyto.getId())));
            final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
            stat.put(MapleBuffStat.EnergyCharged, targets);
            applyto.setBuffedValue(MapleBuffStat.EnergyCharged, targets);
            applyto.getClient().getSession().writeAndFlush(CWvsContext.BuffPacket.giveBuff(0, 0, stat, this, applyto));
        } else {
            final Map<MapleBuffStat, Integer> stat = new HashMap<>();
            stat.put(MapleBuffStat.EnergyCharged, 10000);
            applyto.cancelEffect(this, true, -1, stat);
            final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, stat);
            final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction,
                    ((starttime + info.get(MapleStatInfo.time)) - System.currentTimeMillis()));
            applyto.registerEffect(this, starttime, schedule, stat, false, info.get(MapleStatInfo.time),
                    applyto.getId());
            // applyto.getMap().broadcastMessage(applyto,
            // BuffPacket.giveEnergyChargeTest(applyto.getId(), 10000,
            // info.get(MapleStatInfo.time) / 1000), false);
            final EnumMap<MapleBuffStat, Integer> stat2 = new EnumMap<>(MapleBuffStat.class);
            stat2.put(MapleBuffStat.EnergyCharged, 10000);
            applyto.setBuffedValue(MapleBuffStat.EnergyCharged, 10000);
            applyto.getMap().broadcastMessage(CWvsContext.BuffPacket.giveBuff(0, 0, stat2, this, applyto));

        }
    }

    public void applySurplusSupply(MapleCharacter applyto) {
        Map<MapleBuffStat, Integer> statups = new HashMap<>();
        statups.put(MapleBuffStat.SurplusSupply, 0);
        List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.SurplusSupply, 0));
        applyto.getClient().getSession().write(JobPacket.XenonPacket.giveXenonSupply(0));
        long starttime = System.currentTimeMillis();
        applyto.cancelEffect(this, true, -1L, statups);
        CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, statups);
        ScheduledFuture schedule = Timer.BuffTimer.getInstance().schedule(cancelAction, Long.MAX_VALUE);
        applyto.registerEffect(this, starttime, schedule, statups, false, Integer.MAX_VALUE, applyto.getId());
    }

    public void applyExceed(MapleCharacter applyto) {
        final Map<MapleBuffStat, Integer> stats = new HashMap<>();
        stats.put(MapleBuffStat.OverloadCount, 1);
        applyto.getClient().announce(JobPacket.AvengerPacket.giveExceed((short) 1));
        long starttime = System.currentTimeMillis();
        applyto.cancelEffect(this, true, -1L, stats);
        CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, stats);
        ScheduledFuture schedule = Timer.BuffTimer.getInstance().schedule(cancelAction, Long.MAX_VALUE);
        applyto.registerEffect(this, starttime, schedule, stats, false, Integer.MAX_VALUE, applyto.getId());
    }

    public void applyReincarnation(final MapleCharacter applyfrom) {
        this.applyTo(applyfrom);
        applyfrom.setKillCount(this.getZ());
        applyfrom.getClient().getSession().writeAndFlush(CField.EffectPacket.showBuffEffect(true, applyfrom, 1320016,
                UserEffectOpcode.UserEffect_SkillUse, applyfrom.getLevel(), this.level, (byte) 0));
        applyfrom.getMap().broadcastMessage(applyfrom, CField.EffectPacket.showBuffEffect(false, applyfrom, 1320016,
                UserEffectOpcode.UserEffect_SkillUse, applyfrom.getLevel(), this.level, (byte) 0), false);
        applyfrom.getStat().heal(applyfrom);
        applyfrom.dispelDebuffs();
    }

    public void applyQuiverKartrige(final MapleCharacter applyfrom, boolean bFirst) {
        int currentMode = applyfrom == null ? 0 : applyfrom.getmod();
        if (bFirst) {
            currentMode = (currentMode == 0 ? 1 : currentMode == 1 ? 2 : currentMode == 2 ? 3 : 1);
            applyfrom.setmod(currentMode); // 設定箭矢模式
        }

        int skillid = 3101009;
        Skill skills = SkillFactory.getSkill(skillid);
        int skilllevel = applyfrom.getSkillLevel(skillid);
        MapleStatEffect infoEffect = skills.getEffect(skilllevel);

        int useCount = bFirst ? 0 : applyfrom.qcount;
        applyfrom.qcount = useCount;
        int totle = 0;
        switch (currentMode) {
            case 1: // 吸收箭矢
                totle = (0 * 1000000) + (useCount * 10000) + (0 * 100) + (0);
                break;
            case 2: // 毒箭矢
                totle = (0 * 1000000) + (13 * 10000) + (useCount * 100) + (40);
                break;
            case 3: // 魔法箭矢
                totle = (0 * 1000000) + (16 * 10000) + (0 * 100) + (useCount);
                break;
        }

        infoEffect.getStatups().put(MapleBuffStat.QuiverCatridge, totle); // 套用Buff
        infoEffect.applyBuffEffect(applyfrom);
    }

    public static void applyPassiveBless(MapleCharacter applyfrom) {
        int skillLevel = applyfrom.getSkillLevel(2300009);
        if (skillLevel > 0) {
            int buffToNumber = 1;
            if (applyfrom.getParty() != null) {
                buffToNumber = applyfrom.getParty().getPartyBuffs(applyfrom.getId());
            }
            Skill skil = SkillFactory.getSkill(2300009);
            if (skil == null) {
                return;
            }
            MapleStatEffect eff = applyfrom.inPVP() ? skil.getPVPEffect(skillLevel) : skil.getEffect(skillLevel);
            int gain = eff.getX();
            skillLevel = applyfrom.getSkillLevel(2320013);
            if (skillLevel > 0) {
                skil = SkillFactory.getSkill(2320013);
                if (skil != null) {
                    MapleStatEffect infoEffect = applyfrom.inPVP() ? skil.getPVPEffect(skillLevel)
                            : skil.getEffect(skillLevel);
                    gain = infoEffect.getX();
                }
            }
            eff.getStatups().clear();
            eff.getStatups().put(MapleBuffStat.BlessEnsenble, gain * buffToNumber);
            if (applyfrom.isShowInfo()) {
                applyfrom.dropMessage(5,
                        "發生主教特性增益技能, 增益基礎：" + gain + " 人數：" + buffToNumber + " 總增益：" + gain * buffToNumber);
            }
            eff.applyBuffEffect(applyfrom);
        }
    }

    public void applyBuffEffect(MapleCharacter chr) {
        applyBuffEffect(chr, info.get(MapleStatInfo.time) * (info.get(MapleStatInfo.time) > 0 ? 1000 : 1));
    }

    public void applyBuffEffect(MapleCharacter chr, int newDuration) {
        applyBuffEffect(chr, chr, false, newDuration);
    }

    private void applyBuffEffect(final MapleCharacter applyfrom, final MapleCharacter applyto, final boolean primary,
            final int newDuration) {
        if (statups.containsKey(MapleBuffStat.KeyDownMoving)
                && applyto.getBuffedValue(MapleBuffStat.KeyDownMoving) != null) {
            return;
        }
        int localDuration = newDuration;
        int localROption = getSourceId();
        if (primary) {
            localDuration = Math.max(newDuration, alchemistModifyVal(applyfrom, localDuration, false));
        }
        if (applyfrom.isShowInfo()) {
            applyfrom.showMessage(10, "開始 => applyBuffEffect ID: " + this.sourceid + " 持續時間: " + newDuration);
            StringBuilder buffsString = new StringBuilder();
            for (MapleBuffStat stat : this.statups.keySet()) {
                buffsString.append(stat.toString()).append(" ");
            }
            applyfrom.showMessage(10, "BUFF: " + buffsString.toString());
        }
        Map<MapleBuffStat, Integer> localstatups = new EnumMap<>(statups);
        boolean normal = true, showEffect = primary;

        if (isRuneStone()) {
            if (localstatups.containsKey(MapleBuffStat.DotHealHPPerSecond)) {
                localstatups.put(MapleBuffStat.DotHealHPPerSecond, this.calculateHealHPPerSecond(applyto));
            }
            if (localstatups.containsKey(MapleBuffStat.DotHealMPPerSecond)) {
                localstatups.put(MapleBuffStat.DotHealMPPerSecond, this.calculateHealMPPerSecond(applyto));
            }
        }

        switch (sourceid) {
            case 61101002: // 意志之劍
            case 61120007: // 進階意志之劍
                if (applyfrom.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11) == null) {
                    normal = false;
                } else {
                    localstatups.put(MapleBuffStat.StopForceAtomInfo, applyfrom.getTotalSkillLevel(61101002));
                    this.weapon = applyfrom.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11).getItemId();
                }
                break;
            case 42101002: // 影朋·花狐
                if (applyto.getHaku() != null) {
                    applyto.getHaku().setFigureHaku(true);
                    applyto.getMap().broadcastMessage(applyto, CField.spawnFigureHaku(applyto.getHaku()), true);
                    applyto.getMap().broadcastMessage(applyto, CField.hakuChangeEffect(applyto.getId()), true);
                    applyto.getMap().broadcastMessage(applyto, CField.hakuChange(applyto.getHaku()), true);
                }
                break;
            case 60001216:
            case 60001217:
                if (applyfrom.getStatForBuff(MapleBuffStat.ReshuffleSwitch) != null) {
                    applyfrom.cancelEffectFromBuffStat(MapleBuffStat.ReshuffleSwitch);
                }
                break;
            case 27110007:
                double hpx = applyfrom.getStat().getMaxHp() / applyfrom.getStat().getHp();
                double mpx = applyfrom.getStat().getMaxMp() / applyfrom.getStat().getMp();
                localstatups.put(MapleBuffStat.LifeTidal, hpx >= mpx ? 2 : 1);
                break;
            case 4221013: {
                localstatups.put(MapleBuffStat.IndiePAD,
                        info.get(MapleStatInfo.x) + (info.get(MapleStatInfo.kp) * applyfrom.currentBattleshipHP()));
                applyfrom.setBattleshipHP(0);
                applyfrom.refreshBattleshipHP();
                break;
            }
            case 5311004: {
                final int zz = Randomizer.nextInt(4) + 1;
                applyto.getMap().broadcastMessage(applyto,
                        CField.EffectPacket.showDiceEffect(applyto, sourceid, zz, -1, level), false);
                applyto.getClient().getSession().writeAndFlush(CField.EffectPacket.showDiceEffect(sourceid, zz, -1, level));
                localstatups.put(MapleBuffStat.Roulette, zz);
                break;
            }
            case 5211011:
            case 5211015:
            case 5211016: {
                if (applyfrom.getTotalSkillLevel(5220019) > 0) {
                    SkillFactory.getSkill(5220019).getEffect(applyfrom.getTotalSkillLevel(5220019))
                            .applyBuffEffect(applyfrom, applyto, primary, newDuration);
                }
                break;
            }
            case 42101001:
                SkillFactory.getSkill(42100010).getEffect(applyfrom.getTotalSkillLevel(42101001)).applyBuffEffect(applyfrom,
                        applyto, primary, newDuration);
                normal = false;
                break;
            case 35111013:
            case 15111011:
            case 5111007:
            case 5811007:
            case 5911007:
            case 5311005:
            case 5711011:
            case 5211007: {// dice
                final int diceValue = Randomizer.nextInt(6) + 1;
                applyto.getMap().broadcastMessage(applyto,
                        EffectPacket.showDiceEffect(applyto, sourceid, diceValue, -1, level), false);
                applyto.getClient().getSession().writeAndFlush(EffectPacket.showDiceEffect(sourceid, diceValue, -1, level));
                if (diceValue <= 1) {
                    return;
                }
                localstatups.put(MapleBuffStat.Dice, diceValue);
                applyto.getClient().getSession()
                        .writeAndFlush(BuffPacket.giveDice(diceValue, sourceid, localDuration, localstatups));
                normal = false;
                showEffect = false;
                break;
            }
            case 5720005:
            case 5120012:
            case 5220014:
            case 5320007: {// dice
                final int zz = Randomizer.nextInt(6) + 1;
                final int zz2 = makeChanceResult() ? (Randomizer.nextInt(6) + 1) : 0;
                applyto.getMap().broadcastMessage(applyto,
                        EffectPacket.showDiceEffect(applyto, sourceid, zz, zz2 > 0 ? -1 : 0, level), false);
                applyto.getClient().getSession()
                        .writeAndFlush(EffectPacket.showDiceEffect(sourceid, zz, zz2 > 0 ? -1 : 0, level));
                if (zz <= 1 && zz2 <= 1) {
                    return;
                }
                final int buffid = zz == zz2 ? (zz * 100) : (zz <= 1 ? zz2 : (zz2 <= 1 ? zz : (zz * 10 + zz2)));
                if (buffid >= 100) { // just because of animation lol
                    applyto.dropMessage(-6, "[Double Lucky Dice] You have rolled a Double Down! (" + (buffid / 100) + ")");
                } else if (buffid >= 10) {
                    applyto.dropMessage(-6, "[Double Lucky Dice] You have rolled two dice. (" + (buffid / 10) + " and "
                            + (buffid % 10) + ")");
                }
                localstatups.put(MapleBuffStat.Dice, buffid);
                applyto.getClient().getSession()
                        .writeAndFlush(BuffPacket.giveDice(zz, sourceid, localDuration, localstatups));
                normal = false;
                showEffect = false;
                break;
            }
            case 20031209: // 卡牌審判
            case 20031210: // 審判
                // 0 = 沒有, 1 = 爆擊, 2 = 掉寶, 3 = 抗性, 4 = 防禦, 5 = 攻擊回復HP, 6 <= 無
                int diceType = Randomizer.nextInt(this.sourceid == 20031209 ? 2 : 5) + 1;
                int atomSkillID = 24100003;
                if (applyto.getSkillLevel(24120002) > 0) {
                    atomSkillID = 24120002;
                }
                int attomCount = (atomSkillID == 24100003 ? 5 : 10);
                applyto.setCardStack((byte) 0);
                applyto.getMap().broadcastMessage(applyto,
                        PhantomPacket.gainCardStack(applyto, atomSkillID, Collections.emptyList(), attomCount), true);
                applyto.getMap().broadcastMessage(applyto,
                        CField.EffectPacket.showDiceEffect(applyto, this.sourceid, diceType, -1, this.level), false);
                applyto.getClient().getSession()
                        .writeAndFlush(CField.EffectPacket.showDiceEffect(this.sourceid, diceType, -1, this.level));
                localstatups.put(MapleBuffStat.Judgement, diceType);
                if (diceType == 5) {
                    localstatups.put(MapleBuffStat.VampiricTouch, this.info.get(MapleStatInfo.z));
                }
                applyto.getClient().getSession().writeAndFlush(
                        CWvsContext.BuffPacket.giveBuff(this.sourceid, localDuration, localstatups, this, applyto));
                applyto.getMap().broadcastMessage(CWvsContext.BuffPacket.giveForeignBuff(applyto, localstatups, this));
                normal = false;
                showEffect = false;
                break;
            case 33101006: { // 吞食
                applyto.clearLinkMid();
                MapleBuffStat theBuff = null;
                int theStat = info.get(MapleStatInfo.y);
                switch (Randomizer.nextInt(6)) {
                    case 0:
                        theBuff = MapleBuffStat.HowlingDefence;
                        break;
                    case 1:
                        theBuff = MapleBuffStat.HowlingEvasion;
                        break;
                    case 2:
                        theBuff = MapleBuffStat.Conversion;
                        theStat = info.get(MapleStatInfo.x);
                        break;
                    case 4:
                        theBuff = MapleBuffStat.HowlingAttackDamage;
                        break;
                    case 5:
                        theBuff = MapleBuffStat.BeastFormDamageUp;
                        break;
                }
                localstatups.put(theBuff, theStat);
                break;
            }
            case 5211006: // Homing Beacon
            case 22151002: // killer wings
            case 5220011: {// Bullseye
                if (applyto.getFirstLinkMid() > 0) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.GuidedBullet);
                    localstatups.put(MapleBuffStat.GuidedBullet, info.get(MapleStatInfo.mobCount));
                    applyto.getClient().getSession()
                            .writeAndFlush(BuffPacket.giveBuff(sourceid, localDuration, localstatups, this, applyto));
                } else {
                    return;
                }
                normal = false;
                break;
            }
            case 112000000:
            case 112100000:
            case 112110003: {
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.GuidedBullet, info.get(MapleStatInfo.x));
                break;
            }
            case 2120010:
            case 2220010:
            case 2320011: // arcane aim
                if (applyto.getAllLinkMid().size() > 3 && !applyto.getAllLinkMid().isEmpty()) {
                    int value = info.get(MapleStatInfo.x);
                    value = applyto.getAllLinkMid().values().stream().reduce(value, Integer::sum);
                    applyto.getClient().getSession().writeAndFlush(
                            BuffPacket.giveArcane(value == 0 ? info.get(MapleStatInfo.x) : value, sourceid, localDuration));
                } else {
                    return;
                }
                normal = false;
                break;
            case 4001003: {
                if (applyfrom.getTotalSkillLevel(4330001) > 0 && ((applyfrom.getJob() >= 430 && applyfrom.getJob() <= 434)
                        || (applyfrom.getJob() == 400 && applyfrom.getSubcategory() == 1))) {
                    SkillFactory.getSkill(4330001).getEffect(applyfrom.getTotalSkillLevel(4330001))
                            .applyBuffEffect(applyfrom, applyto, primary, newDuration);
                    return;
                } // fallthrough intended
            }
            /*
         * ID: 110001501 NAME: Bear Mode ID: 110001502 NAME: Snow Leopard Mode ID:
         * 110001503 NAME: Hawk Mode ID: 110001504 NAME: Cat Mode
             */
            case 110001501: // 召喚熊熊
            case 110001502: // 召喚雪豹
            case 110001503: // 召喚雀鷹
            case 110001504: { // 召喚貓咪

                System.out.println("You chose " + sourceid);
                break;
            }
            case 41001001: // 拔刀術
                if (applyfrom.getTotalSkillLevel(41110008) > 0) {
                    SkillFactory.getSkill(41110008).getEffect(applyfrom.getTotalSkillLevel(41110008)).applyTo(applyfrom);
                    return;
                }
                break;
            case 13111023: // 阿爾法
                if (applyfrom.getTotalSkillLevel(13120008) > 0) { // 極限阿爾法
                    SkillFactory.getSkill(13120008).getEffect(applyfrom.getTotalSkillLevel(13120008)).applyTo(applyfrom);
                    return;
                }
                break;
            case 13120003:
            case 13110022:
                return;
            case 13101022: {
                if (applyto.getTotalSkillLevel(13120003) > 0) {
                    SkillFactory.getSkill(13120003).getEffect(applyfrom.getTotalSkillLevel(13120003)).applyTo(applyfrom);
                    return;
                }
                if (applyto.getTotalSkillLevel(13110022) > 0) {
                    SkillFactory.getSkill(13110022).getEffect(applyfrom.getTotalSkillLevel(13110022)).applyTo(applyfrom);
                    return;
                }
                if (applyto.isBuffed(13101022)) {
                    return;
                }
            }
            case 27101202: // 黑暗之眼
                showEffect = false;
                break;
            case 11101022: // 沉月
            case 11111022: // 旭日
                if (applyto.getBuffedValue(MapleBuffStat.PoseType) != null) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.PoseType);
                }
                break;
            case 11121011: // 雙重力量（沉月）
            case 11121012: // 雙重力量（旭日）
                break;
            case 2321005: // 進階祝福
                applyto.cancelEffectFromBuffStat(MapleBuffStat.Bless); // 取消天使祝福
                break;
            case 3211005: {// golden eagle
                if (applyfrom.getTotalSkillLevel(3220005) > 0) {
                    SkillFactory.getSkill(3220005).getEffect(applyfrom.getTotalSkillLevel(3220005))
                            .applyBuffEffect(applyfrom, applyto, primary, newDuration);
                }
                break;
            }
            case 3111005: {// golden hawk
                if (applyfrom.getTotalSkillLevel(3120006) > 0) {
                    SkillFactory.getSkill(3120006).getEffect(applyfrom.getTotalSkillLevel(3120006))
                            .applyBuffEffect(applyfrom, applyto, primary, newDuration);
                }
                break;
            }
            case 1201012: // wk charges
            case 1211004:
            case 1221004:
            case 11111007:
            case 51111003: // Mihile's Radiant Charge
            case 21101006:
            case 21111005:
            case 15101006: { // Soul Arrow
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.WeaponCharge, 1);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto, stat, this), false);
                break;
            }
            case 3120006:
            case 3220005: { // Spirit Link
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.SpiritLink, 0);
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto, stat, this), false);
                break;
            }
            case 31121005: { // Dark Metamorphosis
                if (applyto.isHidden()) {
                    break;
                }
                final EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.DevilishPower, 6); // mob count
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto, stat, this), false);
                break;
            }
            case 2121004:
            case 2221004:
            case 2321004: { // Infinity
                localDuration = alchemistModifyVal(applyfrom, 4000, false);
                break;
            }
            case 4331003: { // Owl Spirit
                localstatups.put(MapleBuffStat.OWL_SPIRIT, info.get(MapleStatInfo.y));
                applyto.getClient().getSession()
                        .writeAndFlush(BuffPacket.giveBuff(sourceid, localDuration, localstatups, this, applyto));
                applyto.setBattleshipHP(info.get(MapleStatInfo.x)); // a variable that wouldnt' be used by a db
                normal = false;
                break;
            }
            case 1121010: // Enrage
                applyto.handleOrbconsume(1);
                break;
            case 2022746: // angel bless
            case 2022747: // d.angel bless
            case 2022823:
                if (applyto.isHidden()) {
                    break;
                }
                applyto.getMap().broadcastMessage(applyto, BuffPacket.giveForeignBuff(applyto, localstatups, this), false);
                break;
            case 31011000:
            case 31011004:
            case 31011005:
            case 31011006:
            case 31011007:
            case 31201000:
            case 31201007:
            case 31201008:
            case 31201009:
            case 31201010:
            case 31211000:
            case 31211007:
            case 31211008:
            case 31211009:
            case 31211010:
            case 31221000:
            case 31221009:
            case 31221010:
            case 31221011:
            case 31221012: {
                TemporaryStatBase exceed = applyto.getTempstats().getStatOption(MapleBuffStat.Exceed);
                localstatups.put(MapleBuffStat.Exceed, exceed.nOption);
                normal = true;
                break;
            }
            case 31221052: {
                int exceedOverload = applyto.getExceedOverload();
                int exceedMax = 20;
                Skill skil = SkillFactory.getSkill(31220044); // 超越—超載解放
                int skilllevel = applyto.getTotalSkillLevel(skil);
                if (skilllevel > 0) {
                    exceedMax -= 2;
                }
                exceedOverload = Math.min(exceedOverload + 5, exceedMax);
                localstatups.put(MapleBuffStat.OverloadCount, exceedOverload);
                applyto.getClient().getSession()
                        .writeAndFlush(BuffPacket.giveBuff(30010230, -1, localstatups, this, applyto));
                normal = false;
                break;
            }
            case 31011001: {
                TemporaryStatBase exceed = applyto.getTempstats().getStatOption(MapleBuffStat.Exceed);
                TemporaryStatBase exceedOverload = applyto.getTempstats().getStatOption(MapleBuffStat.OverloadCount);
                applyto.getClient().getSession().writeAndFlush(JobPacket.AvengerPacket.cancelExceed());
                int maxOverload = (short) ((applyto.getSkillLevel(31220044) <= 0) ? 20 : 18);
                applyto.addHP((int) (applyto.getStat().getCurrentMaxHp()
                        * (exceedOverload.nOption / maxOverload * this.getX() / 100.0)));
                if (exceed.isActivated()) {
                    exceed.nOption = Math.max(0, exceed.nOption - 2);
                    // localstatups.put(MapleBuffStat.Exceed, exceed.getValue());
                }
                int indiePMdR = 0;
                if (applyto.getSkillLevel(31210006) <= 0) { // 超越解放
                    indiePMdR = 5 + exceedOverload.nOption * this.getY() / 2 / maxOverload;
                } else {
                    indiePMdR = 15 + exceedOverload.nOption * applyto.getSkillLevel(31210006) * 2 / maxOverload;
                }
                exceedOverload.nOption = 0;
                localstatups.put(MapleBuffStat.IndiePMdR, indiePMdR);
                break;
            }
            case 131001004: // 咕嚕咕嚕
                BuffTimer.getInstance().schedule(() -> {
                    if (applyto.getBuffSource(MapleBuffStat.KeyDownMoving) == 131001004) {
                        applyto.getClient().getSession().writeAndFlush(JobPacket.PinkBeanPacket.咕嚕咕嚕變大());
                    }
                }, 3500L);
                break;
            case 40011288: // 拔刀姿勢
                short 剣気 = applyto.getJianQi();
                剣気 = (short) Math.ceil(剣気 * 0.70);
                applyto.setJianQi(剣気);
                MapleStatEffect effect = SkillFactory.getSkill(40011292)
                        .getEffect((int) Math.min(5, Math.max(1, Math.floor(剣気 / 200))));
                applyto.dispelBuff(40011291);
                effect.applyTo(applyto);
                break;
            case 42121021: // 花炎結界
            case 42121022: // 花狐的祝福
            case 42121023: // 幽玄氣息
                if (applyto.getHaku() == null || !applyto.getHaku().isFigureHaku()) {
                    return;
                }
                break;
            case 1211010: // 復原
                Integer healRate = applyto.getBuffedValue(MapleBuffStat.Restoration);
                localstatups.put(MapleBuffStat.Restoration, Math.min(40, (healRate == null ? 0 : healRate) + 10));
                break;
            case 1200014: // 元素衝擊
            case 1220010: // 屬性強化
                Integer val = applyto.getBuffedValue(MapleBuffStat.ElementalCharge);
                localstatups.put(MapleBuffStat.ElementalCharge, Math.min(10, (val == null ? 0 : val) + 2));
                break;
            case 1221009: // 騎士衝擊波
                val = applyto.getBuffedValue(MapleBuffStat.ElementalCharge);
                if (val != null && val == 10) {
                    applyto.cancelBuffStats(MapleBuffStat.ElementalCharge);
                } else {
                    return;
                }
                break;
            case 1311013: // 追隨者支配
            case 1311014: { // 追隨者衝擊
                for (MapleSummon sum : applyto.getSummonsReadLock()) {
                    if (sum.getSkill() == 1301013) {
                        switch (sourceid) {
                            case 1311013:
                                sum.setControl(!sum.getControl());
                                break;
                            case 1311014:
                                sum.setScream(true);
                                break;
                        }
                        break;
                    }
                }
                applyto.unlockSummonsReadLock();
                break;
            }
            case 1321015: // 暗之獻祭
                applyto.dispelSkill(1301013);
                if (applyto.skillisCooling(1321013)) { // 暗靈審判
                    applyto.removeCooldown(1321013);
                }
                break;
            case 2111011: // 元素適應(火、毒)
            case 27111004: // 魔力護盾
                val = applyto.getBuffedValue(MapleBuffStat.AntiMagicShell);
                if (val != null && val > 0) {
                    localstatups.put(MapleBuffStat.AntiMagicShell, Math.max(0, val - 1));
                    applyto.getClient().getSession().write(EffectPacket.showBuffEffect(true, applyto, sourceid,
                            UserEffectOpcode.UserEffect_SkillUse, applyto.getLevel(), level, (byte) 0));
                }
                break;
            case 2211012: // 元素適應(雷、冰)
                if (!applyto.isBuffed(2211012)) {
                    localDuration = -1;
                    localstatups.put(MapleBuffStat.AntiMagicShell, 0);
                } else {
                    applyto.dispelDebuffs();
                }
                break;
            case 35001002:
                if (applyfrom.getTotalSkillLevel(35120000) > 0) {
                    SkillFactory.getSkill(35120000).getEffect(applyfrom.getTotalSkillLevel(35120000))
                            .applyBuffEffect(applyfrom, applyto, primary, newDuration);
                    return;
                } // 真的沒有break;
            // fallthrough intended
            default:
                if (isMorph()) {
                    if (isIceKnight()) {
                        localstatups.put(MapleBuffStat.BuffLimit, 2);
                    }
                    localstatups.put(MapleBuffStat.Morph, getMorph(applyto));
                } else if (isInflation()) {
                    localstatups.put(MapleBuffStat.Inflation, (int) inflation);
                } else if (charColor > 0) {
                    localstatups.put(MapleBuffStat.Event2, 1);
                } else if (isMonsterRiding()) {
                    final int mountid = parseMountInfo(applyto, sourceid);
                    final int mountid2 = parseMountInfo_Pure(applyto, sourceid);
                    if (mountid != 0 && mountid2 != 0) {
                        if (applyto.getBuffSource(MapleBuffStat.RideVehicle) == 35001002) {
                            applyto.cancelEffect(applyto.getStatForBuff(MapleBuffStat.RideVehicle), false, -1);
                            return;
                        } else {
                            localstatups.put(MapleBuffStat.RideVehicle, mountid);
                        }
                    }
                } else if (isSoaring()) {
                    localstatups.put(MapleBuffStat.Flying, 1);
                } else if (berserk > 0) {
                    localstatups.put(MapleBuffStat.ExpBuffRate, 0);
                } else if (isBerserkFury()) {
                    localstatups.put(MapleBuffStat.DojangBerserk, 1);
                } else if (applyto.isInvincible() && sourceid == 80000329) {
                    if (applyto.isShowInfo()) {
                        applyto.dropMessage(-6, "定制技能 - GM無敵[原技能: 自由精神](在角色無敵狀態才會生效)");
                    }
                }
                break;
        }
        if (showEffect) {
            if (applyto.isHidden()) {
                applyto.getMap().broadcastGMMessage(applyto, EffectPacket.showBuffEffect(false, applyto, sourceid,
                        UserEffectOpcode.UserEffect_SkillUse, applyto.getLevel(), level), false);
            } else {
                applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffEffect(false, applyto, sourceid,
                        UserEffectOpcode.UserEffect_SkillUse, applyto.getLevel(), level), false);
            }
            List<WeakReference<MapleCharacter>> clones = applyto.getClones();
            final MapleMap map = applyto.getMap();
            for (WeakReference<MapleCharacter> clone : clones) {
                if (clone.get() != null) {
                    final MapleCharacter chr = clone.get();
                    if (chr.getMap() == map && chr.getDragon() != null) {
                        if (chr.isHidden()) {
                            map.broadcastGMMessage(chr, EffectPacket.showBuffEffect(false, chr, sourceid, UserEffectOpcode.UserEffect_SkillUse, chr.getLevel(), level), false);
                        } else {
                            map.broadcastMessage(chr, EffectPacket.showBuffEffect(false, chr, sourceid, UserEffectOpcode.UserEffect_SkillUse, chr.getLevel(), level), false);
                        }
                    }
                }
            }
        }

        if (isBerserk()) {
            applyto.setKillCount(getZ());
            applyto.getClient().getSession().writeAndFlush(EffectPacket.showBuffEffect(true, applyto, sourceid,
                    UserEffectOpcode.UserEffect_SkillUse, applyto.getLevel(), level, (byte) 0));
            applyto.dispelDebuffs();
            applyto.getStat().heal(applyto);
            if (applyto.skillisCooling(1321013)) { // 暗靈審判
                applyto.removeCooldown(1321013);
            }
        }
        if (isMechPassive()) {
            applyto.getClient().getSession().writeAndFlush(EffectPacket.showBuffEffect(true, applyto, sourceid - 1000,
                    UserEffectOpcode.UserEffect_SkillUse, applyto.getLevel(), level, (byte) 1));
        }

        Map<MapleBuffStat, Integer> localstatupsList = new HashMap<>(localstatups);

        if (!isMonsterRiding() && !isMechDoor() && getSummonMovementType() == null) {
            applyto.cancelEffect(this, true, -1, localstatupsList);
        }

        final long starttime = System.currentTimeMillis();
        final CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, starttime, localstatupsList);
        final ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction,
                localDuration == -1 ? Long.MAX_VALUE : localDuration);
        applyto.registerEffect(this, starttime, schedule, localstatupsList, false, localDuration, applyfrom.getId());

        if (localDuration == -1) {
            localDuration = 0;
        }
        // Broadcast effect to self
        if (normal && localstatups.size() > 0) {
            applyto.getClient().getSession().writeAndFlush(BuffPacket.giveBuff((skill ? sourceid : -sourceid), localDuration, localstatups, this, applyto));
            applyto.getMap().broadcastMessage(CWvsContext.BuffPacket.giveForeignBuff(applyto, localstatups, this));
        }
    }

    public static List<Pair<MapleBuffStat, Integer>> mapToList(Map<MapleBuffStat, Integer> map) {
        List<Pair<MapleBuffStat, Integer>> list = new ArrayList<>();
        for (Map.Entry<MapleBuffStat, Integer> entry : map.entrySet()) {
            list.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    public static int parseMountInfo(final MapleCharacter player, final int skillid) {
        switch (skillid) {
            case 80001000:
            case 1004: // Monster riding
            case 11004: // Monster riding
            case 10001004:
            case 20001004:
            case 20011004:
            case 20021004:
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -118) != null
                        && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -119) != null) {
                    return player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -118).getItemId();
                }
                return parseMountInfo_Pure(player, skillid);
            default:
                return GameConstants.getMountItem(skillid, player);
        }
    }

    public static int parseMountInfo_Pure(final MapleCharacter player, final int skillid) {
        switch (skillid) {
            case 80001000:
            case 1004: // Monster riding
            case 11004: // Monster riding
            case 10001004:
            case 20001004:
            case 20011004:
            case 20021004:
                if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null
                        && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
                    return player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItemId();
                }
                return 0;
            default:
                return GameConstants.getMountItem(skillid, player);
        }
    }

    private int calcHPChange(final MapleCharacter applyfrom, final boolean primary) {
        int hpchange = 0;
        if (this.sourceid == 9001000 || this.sourceid == 9101000) {
            hpchange = 500000;
        }
        if (info.get(MapleStatInfo.hp) != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, info.get(MapleStatInfo.hp), true);
                } else {
                    hpchange += info.get(MapleStatInfo.hp);
                }
                if (applyfrom.hasDisease(MapleDisease.不死化)) {
                    hpchange /= 2;
                }
            } else { // assumption: this is heal
                hpchange += makeHealHP(info.get(MapleStatInfo.hp) / 100.0, applyfrom.getStat().getTotalMagic(), 3, 5);
                if (applyfrom.hasDisease(MapleDisease.不死化)) {
                    hpchange = -hpchange;
                }
            }
        }
        if (hpR != 0) {
            double healHpRate = hpR;
            if (skill && sourceid == 1211010) {
                Integer value = applyfrom.getBuffedValue(MapleBuffStat.Restoration);
                healHpRate -= (value == null ? 0 : value) / 100.0D;
                if (healHpRate < 0.1) {
                    healHpRate = 0.1;
                }
            }
            hpchange += !skill && healHpRate == 1.0 && applyfrom.getStat().getCurrentMaxHp() > 99999 ? 99999
                    : (applyfrom.getStat().getCurrentMaxHp() * healHpRate);
            if (applyfrom.hasDisease(MapleDisease.不死化)) {
                hpchange /= 2;
            }
        }
        // actually receivers probably never get any hp when it's not heal but whatever
        if (primary) {
            if (info.get(MapleStatInfo.hpCon) != 0) {
                hpchange -= info.get(MapleStatInfo.hpCon);
            }
            if (info.get(MapleStatInfo.hpRCon) != 0) {
                hpchange -= applyfrom.getStat().getCurrentMaxHp() * info.get(MapleStatInfo.hpRCon) / 100;
            }
        }
        switch (this.sourceid) {
            case 4211001: // Chakra
                final PlayerStats stat = applyfrom.getStat();
                int v42 = getY() + 100;
                int v38 = Randomizer.rand(1, 100) + 100;
                hpchange = (int) ((v38 * stat.getLuk() * 0.033 + stat.getDex()) * v42 * 0.002);
                hpchange += makeHealHP(getY() / 100.0, applyfrom.getStat().getTotalLuk(), 2.3, 3.5);
                break;
        }
        if (hpchange < 0 && applyfrom.isBuffed(31221054)) { // 禁忌契約
            hpchange = 0;
        }
        return hpchange;
    }

    private static int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        return (int) ((Math.random() * ((int) (stat * upperfactor * rate) - (int) (stat * lowerfactor * rate) + 1))
                + (int) (stat * lowerfactor * rate));
    }

    private int calcMPChange(final MapleCharacter applyfrom, final boolean primary) {
        int mpchange = 0;
        if (info.get(MapleStatInfo.mp) != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, info.get(MapleStatInfo.mp), false); // recovery up doesn't
                // apply for mp
            } else {
                mpchange += info.get(MapleStatInfo.mp);
            }
        }
        if (mpR != 0) {
            mpchange += !skill && mpR == 1.0 && applyfrom.getStat().getCurrentMaxMp() > 99999 ? 99999
                    : (applyfrom.getStat().getCurrentMaxMp() * mpR);
        }
        if (MapleJob.is惡魔殺手(applyfrom.getJob())) {
            mpchange = 0;
        }
        if (primary) {
            if (info.get(MapleStatInfo.mpCon) != 0 && !MapleJob.is惡魔殺手(applyfrom.getJob())) {
                boolean free = false;
                if (applyfrom.getJob() == 411 || applyfrom.getJob() == 412) {
                    final Skill expert = SkillFactory.getSkill(4110012);
                    if (applyfrom.getTotalSkillLevel(expert) > 0) {
                        final MapleStatEffect eff = expert.getEffect(applyfrom.getTotalSkillLevel(expert));
                        if (eff.makeChanceResult()) {
                            free = true;
                        }
                    }
                }
                if (applyfrom.getBuffedValue(MapleBuffStat.Infinity) != null) {
                    mpchange = 0;
                } else if (!free) {
                    mpchange -= (info.get(MapleStatInfo.mpCon)
                            - (info.get(MapleStatInfo.mpCon) * applyfrom.getStat().mpconReduce / 100))
                            * (applyfrom.getStat().mpconPercent / 100.0);
                }
            } else if (info.get(MapleStatInfo.forceCon) != 0) {
                if (applyfrom.getBuffedValue(MapleBuffStat.InfinityForce) != null) {
                    mpchange = 0;
                } else {
                    mpchange -= info.get(MapleStatInfo.forceCon);
                }
            }
        }

        return mpchange;
    }

    public final int alchemistModifyVal(final MapleCharacter chr, final int val, final boolean withX) {
        if (!skill) {
            return (val * (100 + (withX ? chr.getStat().道具恢復效能提升 : chr.getStat().BUFF道具持續時間提升)) / 100);
        }
        return (val * (100 + (withX ? chr.getStat().道具恢復效能提升
                : (chr.getStat().BUFF技能持續時間提升 + (getSummonMovementType() == null ? 0 : chr.getStat().召喚獸持續時間提升))))
                / 100);
    }

    public final int calcPowerChange(final MapleCharacter applyfrom) {
        int powerchange = 0;
        if (!MapleJob.is傑諾(applyfrom.getJob())) {
            return powerchange;
        }
        if (getPowerEnergy() != 0) {
            powerchange -= getPowerEnergy();
        }
        return powerchange;
    }

    public final void setSourceId(final int newid) {
        sourceid = newid;
    }

    public final boolean isGmBuff() {
        switch (sourceid) {
            case 10001075: // Empress Prayer
            case 9001000: // 終極祝福
            case 9001001: // 終極輕功
            case 9001002: // 終極祈禱
            case 9001003: // GM的祝福
            case 9001005: // 復活
            case 9001008: // hyper body

            case 9101000:
            case 9101001:
            case 9101002:
            case 9101003:
            case 9101005:
            case 9101008:
                return true;
            default:
                return MapleJob.isBeginner(sourceid / 10000) && sourceid % 10000 == 1005;
        }
    }

    public final boolean isInflation() {
        return inflation > 0;
    }

    public final int getInflation() {
        return inflation;
    }

    public final boolean isEnergyCharge() {
        return skill && (sourceid == 5810001 || sourceid == 5100015 || sourceid == 5110001 || sourceid == 15100004);
    }

    public boolean isMonsterBuff() {
        switch (sourceid) {
            case 1211013: // Threaten
            case 1201006: // threaten
            case 2101003: // fp slow
            case 2201003: // il slow
            case 5011002:
            case 12101001: // cygnus slow
            case 2211004: // il seal
            case 2111004: // fp seal
            case 12111002: // cygnus seal
            case 2311005: // doom
            case 4111003: // shadow web
            case 14111001: // cygnus web
            case 4121004: // Ninja ambush
            case 4221004: // Ninja ambush
            case 22151001:
            case 22121000:
            case 22161002:
            case 4321002:
            case 4341003:
            case 90001002:
            case 90001003:
            case 90001004:
            case 90001005:
            case 90001006:
            case 1111007:
            case 51111005: // Mihile's magic crash
            case 1211009:
            case 1311007:
            case 35111005:
            case 32120000:
            case 32120001:
                return skill;
        }
        return false;
    }

    public final void setPartyBuff(boolean pb) {
        this.partyBuff = pb;
    }

    public boolean isPartyBuff() {
        if (lt == null || rb == null || !partyBuff) {
            return isSoulStone();
        }
        switch (sourceid) { // TODO 添加非組隊BUFF
            case 1201011:
            case 1201012:
            case 4341052:
            case 61121009:
            case 61101002:
            case 1221054:
            case 4111002:
            case 61110211:
            case 61120007:
            case 35121054:
            case 100001005:
            case 110001005:
            case 36121007:
            case 1211003:
            case 1211004:
            case 1211005:
            case 1211007:
            case 1211008:
            case 1221003:
            case 1221004:
            case 11111007:
            case 51111003: // Mihile's Radiant Charge
            case 12101005:
            case 4311001:
            case 4331003:
            case 4341002:
            case 35121005:
                return false;
        }
        if (MapleJob.isBeginner(sourceid / 10000)) {
            switch (sourceid % 10000) {
                case 8001: // 實用的時空門
                case 8002:// 實用的會心之眼
                case 8003: // 實用的神聖之火
                case 8004: // 有用的戰鬥命令
                case 8005: // 有用的進階祝福
                case 8006: // 有用的最終極速
                    return false;
            }
        }
        return !GameConstants.isNoDelaySkill(sourceid);
    }

    public final boolean isArcane() {
        return skill && (sourceid == 2320011 || sourceid == 2220010 || sourceid == 2120010);
    }

    public final boolean isHeal() {
        return skill && (sourceid == 2301002 || sourceid == 9101000 || sourceid == 9001000);
    }

    public final boolean isResurrection() {
        return skill && (sourceid == 9001005 || sourceid == 9101005 || sourceid == 2321006);
    }

    public final boolean isTimeLeap() {
        return skill && sourceid == 5121010;
    }

    public final int getHp() {
        return info.get(MapleStatInfo.hp);
    }

    public final int getMp() {
        return info.get(MapleStatInfo.mp);
    }

    public final int getDOTStack() {
        return info.get(MapleStatInfo.dotSuperpos);
    }

    public final double getHpR() {
        return hpR;
    }

    public final double getMpR() {
        return mpR;
    }

    public final int getMastery() {
        return info.get(MapleStatInfo.mastery);
    }

    public final int getWatk() {
        return info.get(MapleStatInfo.pad);
    }

    public final int getMatk() {
        return info.get(MapleStatInfo.mad);
    }

    public final int getWdef() {
        return info.get(MapleStatInfo.pdd);
    }

    public final int getMdef() {
        return info.get(MapleStatInfo.mdd);
    }

    public final int getAcc() {
        return info.get(MapleStatInfo.acc);
    }

    public final int getAccR() {
        return info.get(MapleStatInfo.ar);
    }

    public final int getAvoid() {
        return info.get(MapleStatInfo.eva);
    }

    public final int getAvoidX() {
        return info.get(MapleStatInfo.evaX);
    }

    public final int getSpeed() {
        return info.get(MapleStatInfo.speed);
    }

    public final int getJump() {
        return info.get(MapleStatInfo.jump);
    }

    public final int getSpeedMax() {
        return info.get(MapleStatInfo.speedMax);
    }

    public final int getPassiveSpeed() {
        return info.get(MapleStatInfo.psdSpeed);
    }

    public final int getPassiveJump() {
        return info.get(MapleStatInfo.psdJump);
    }

    public final int getDuration() {
        return info.get(MapleStatInfo.time);
    }

    public final int getSubTime() {
        return info.get(MapleStatInfo.subTime);
    }

    public final boolean isOverTime() {
        return overTime;
    }

    public boolean isNotRemoved() {
        return this.notRemoved;
    }

    public final Map<MapleBuffStat, Integer> getStatups() {
        return statups;
    }

    public final boolean sameSource(final MapleStatEffect effect) {
        if (effect == null) {
            return false;
        }
        boolean sameSrc = sourceid == effect.sourceid;
        switch (sourceid) { // All these are passive skills, will have to cast the normal ones.
            case 35120000: // Extreme Mech
                sameSrc = effect.sourceid == 35001002;
                break;
            case 35121013: // Mech: Siege Mode
                sameSrc = effect.sourceid == 35111004;
                break;
        }
        return sameSrc && skill == effect.skill;
    }

    public final int getCr() {
        return info.get(MapleStatInfo.cr);
    }

    public final int getT() {
        return info.get(MapleStatInfo.t);
    }

    public final int getU() {
        return info.get(MapleStatInfo.u);
    }

    public final int getV() {
        return info.get(MapleStatInfo.v);
    }

    public final int getW() {
        return info.get(MapleStatInfo.w);
    }

    public final int getX() {
        return info.get(MapleStatInfo.x);
    }

    public final int getY() {
        return info.get(MapleStatInfo.y);
    }

    public final int getS() {
        return info.get(MapleStatInfo.s);
    }

    public final int getZ() {
        return info.get(MapleStatInfo.z);
    }

    public final int getDamage() {
        return info.get(MapleStatInfo.damage);
    }

    public final int getPVPDamage() {
        return info.get(MapleStatInfo.PVPdamage);
    }

    public final int getAttackCount() {
        return info.get(MapleStatInfo.attackCount);
    }

    public final int getBulletCount() {
        return info.get(MapleStatInfo.bulletCount);
    }

    public final int getBulletConsume() {
        return info.get(MapleStatInfo.bulletConsume);
    }

    public final int getOnActive() {
        return info.get(MapleStatInfo.onActive);
    }

    public final int getMobCount() {
        return info.get(MapleStatInfo.mobCount);
    }

    public final int getMoneyCon() {
        return moneyCon;
    }

    public void setEffectDelay(short effectDelay) {
        this.effectDelay = effectDelay;
    }

    public final short getEffectDelay() {
        return effectDelay;
    }

    public final int getCooltimeReduceR() {
        return info.get(MapleStatInfo.coolTimeR);
    }

    public final int getMesoAcquisition() {
        return info.get(MapleStatInfo.mesoR);
    }

    public final int getCooldown(final MapleCharacter chra) {
        if (skill) {
            // TODO 設定條件技能不進入冷卻
            switch (sourceid) {
                case 1321013: // 黑暗審判
                    if (chra.hasBuffSkill(1321015) || chra.hasBuffSkill(1320019)) { // 暗之獻祭 || 重生
                        return 0;
                    }
                    break;
            }
            if (statups.containsKey(MapleBuffStat.AntiMagicShell)) {
                Integer val = chra.getBuffedValue(MapleBuffStat.AntiMagicShell);
                if (val == null) {
                    return 0;
                }
            }
        }
        // 超技能CD減少
        double rate = chra.getStat().getReduceCooltimeRate(getSourceId());
        double HyperSkill_ReduceCD = 0;
        if (rate > 0) {
            HyperSkill_ReduceCD = (rate / 100.0) * info.get(MapleStatInfo.cooltime);
        }

        // 角色卡CD減少
        double OtherReduce = 0;
        if (chra.getStat().coolTimeR > 0) {
            OtherReduce = ((chra.getStat().coolTimeR / 100.0) * info.get(MapleStatInfo.cooltime));
        }

        double TotalReduce = (HyperSkill_ReduceCD + OtherReduce);
        int Last = (int) (info.get(MapleStatInfo.cooltime) - TotalReduce);
        return Math.max(0, Last);
    }

    public final Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }

    public final int getBerserk() {
        return berserk;
    }

    public final boolean isHide() {
        return skill && (sourceid == 9001004 || sourceid == 9101004);
    }

    public final boolean isDragonBlood() {
        return skill && sourceid == 1311008;
    }

    public final boolean isRecovery() {
        return skill && (sourceid == 1001 || sourceid == 10001001 || sourceid == 20001001 || sourceid == 20011001
                || sourceid == 20021001 || sourceid == 11001 || sourceid == 35121005);
    }

    public final boolean isBerserk() {
        return skill && sourceid == 1320016 || sourceid == 1320019;
    }

    public final boolean isBeholder() {
        return skill && sourceid == 1321007 || sourceid == 1301013 /* || sourceid == 1311013 */;
    }

    public final boolean isMPRecovery() {
        return skill && sourceid == 5101005;
    }

    public final boolean isInfinity() {
        return skill && (sourceid == 2121004 || sourceid == 2221004 || sourceid == 2321004);
    }

    public final boolean isMonsterRiding_() {
        return skill && (sourceid == 1004 || sourceid == 10001004 || sourceid == 20001004 || sourceid == 20011004
                || sourceid == 11004 || sourceid == 20021004 || sourceid == 80001000);
    }

    public final boolean isMonsterRiding() {
        return skill && (isMonsterRiding_() || GameConstants.getMountItem(sourceid, null) != 0);
    }

    public final boolean isMagicDoor() {
        return skill && (sourceid == 2311002 || sourceid % 10000 == 8001);
    }

    public final boolean isMesoGuard() {
        return skill && sourceid == 4211005;
    }

    public final boolean isMechDoor() {
        return skill && sourceid == 35101005;
    }

    public final boolean isRuneStone() {
        return sourceid >= 80001427 && sourceid <= 80001432;
    }

    public final boolean isComboRecharge() {
        return skill && sourceid == 21111009;
    }

    public final boolean isDragonBlink() {
        return skill && sourceid == 22141004;
    }

    public final boolean isCharge() {
        switch (sourceid) {
            case 1211003:
            case 1211008:
            case 11111007:
            case 51111003: // Mihile's Radiant Charge
            case 12101005:
            case 15101006:
            case 21111005:
                return skill;
        }
        return false;
    }

    public final boolean isPoison() {
        return info.get(MapleStatInfo.dot) > 0 && info.get(MapleStatInfo.dotTime) > 0;
    }

    public boolean isMist() {// TODO MIST::添加技能
        switch (sourceid) {
            case 1076: // 奧茲的火牢術屏障
            case 2111003: // 致命毒霧
            case 2311011: // 神聖之泉
            case 4121015: // 絕對領域
            case 4221006: // 煙幕彈
            case 12111005: // 火牢術屏障
            case 14111006: // 毒炸彈
            case 22161003: // 聖療之光
            case 32121006: // 魔法屏障
            case 36121007: // 時空膠囊
            case 42111004: // 結界‧櫻
            case 42121005: // 結界‧桔梗
            case 12121005: // 燃燒軍團
            case 25111206: // 束縛術
            case 61121105: // 龍烈焰
            case 35121052: // 扭曲領域
            case 33121012:
            case 35121010:
            case 100001261: // 時間扭曲
            case 131001107: // 博拉多利
            case 131001207: // 帕拉美
            case 131001307: // 愛美麗
                return true;
        }
        return false;
    }

    // 無形鏢
    private boolean isSpiritClaw() {
        return skill && sourceid == 4111009 || sourceid == 14111007;
    }

    // 無形彈藥
    private boolean isSpiritBlast() {
        return skill && sourceid == 5201008;
    }

    private boolean isDispel() {
        return skill && (sourceid == 2311001 || sourceid == 9001000 || sourceid == 9101000);
    }

    private boolean isHeroWill() {
        switch (sourceid) {
            case 1121011:
            case 1221012:
            case 1321010:
            case 2121008:
            case 2221008:
            case 2321009:
            case 3121009:
            case 3221008:
            case 4121009:
            case 4221008:
            case 5121008:
            case 5221010:
            case 21121008:
            case 22171004:
            case 4341008:
            case 32121008:
            case 33121008:
            case 35121008:
            case 5321008:
            case 23121008:
            case 24121009:
            case 5721002:
                return skill;
        }
        return false;
    }

    public final boolean isAranCombo() {
        return sourceid == 21000000;
    }

    public final boolean isCombo() {
        switch (sourceid) {
            case 1111002:
            case 11111001: // Combo
            case 1101013:
                return skill;
        }
        return false;
    }

    public final boolean isMorph() {
        return morphId > 0;
    }

    public final int getMorph() {
        switch (sourceid) {
            case 15111002:
            case 5111005:
                return 1000;
            case 5121003:
            case 1203:
                return 1001;
            case 5101007:
                return 1002;
            case 13111005:
                return 1003;
        }
        return morphId;
    }

    public final boolean isDivineBody() {
        return skill && MapleJob.isBeginner(sourceid / 10000) && sourceid % 10000 == 1010;
    }

    public final boolean isDivineShield() {
        switch (sourceid) {
            case 1220013:
                return skill;
        }
        return false;
    }

    public final boolean isBerserkFury() {
        return skill && MapleJob.isBeginner(sourceid / 10000) && sourceid % 10000 == 1011;
    }

    public final int getMorph(final MapleCharacter chr) {
        final int morph = getMorph();
        switch (morph) {
            case 1000:
            case 1001:
            case 1003:
                return morph + (chr.getGender() == 1 ? 100 : 0);
        }
        return morph;
    }

    public final byte getLevel() {
        return level;
    }

    public final SummonMovementType getSummonMovementType() { // TODO 召喚獸::添加召喚獸類型,新的召喚獸可以在這裡添加
        if (!skill) {
            return null;
        }
        switch (sourceid) {
            case 3221014:
            case 4111007:
            case 4211007:
            case 4341006:
            case 5211014:
            case 5320011:
            case 5321003:
            case 5321004:
            case 5711001:
            case 13111024:
            case 13120007:
            case 14111010:
            case 33101008:
            case 33111003:
            case 35111002:
            case 35111005:
            case 35111011:
            case 35121003:
            case 35121009:
            case 35121010:
            case 61111002:
            case 36121002: // 能量領域：貫通
            case 36121013: // 能量領域：力場
            case 36121014: // 能量領域：支援
            case 14121003:
            case 112001007:
            case 5321052: // 滾動彩虹加農炮
            case 12111022: // 漩渦
            case 80011261: // 輪迴
            case 42111003: // 鬼神召喚
            case 42100010: // 式神炎舞 - 召喚獸
                return SummonMovementType.不會移動;
            case 3111005:
            case 3211005:
            case 23111008:
            case 23111009:
            case 23111010:
            case 33101011:
            case 14000027: // 暗影蝙蝠
                return SummonMovementType.跟隨飛行隨機移動攻擊;
            case 131002015: // 迷你啾
            case 5211002: // bird - pirate
                return SummonMovementType.盤旋攻擊怪死後跟隨;
            case 32111006:
            case 35121011:
            case 2111010:
                return SummonMovementType.自由移動;
            case 1301013:
            case 2121005:
            case 2211011:// 雷鳴風暴
            case 2221005:
            case 2321003:
            case 12001004:
            case 12111004:
            case 14001005:
            case 35111001:
            case 35111009:
            case 35111010:
            case 12000022: // 元素:火焰 12001020 12000026
            case 12100026: // 12100020 12101028
            case 12110024: // 12111028 12110020
            case 12120007: // 12120010 12120006
            case 22171052:
            case 32001014:
            case 32100010:
            case 32110017:
            case 32120019: // 死神
                return SummonMovementType.飛行跟隨;
            case 14111024: // 暗影僕從
                return SummonMovementType.跟隨移動跟隨攻擊;
            case 33001007:
            case 33001008:
            case 33001009:
            case 33001010:
            case 33001011:
            case 33001012:
            case 33001013:
            case 33001014:
            case 33001015:
                return SummonMovementType.美洲豹;
            case 5201012: // 二轉盲俠
            case 5201013: // 二轉瓦蕾莉
            case 5201014: // 二轉傑克
            case 5210015: // 三轉盲俠
            case 5210016: // 三轉瓦蕾莉
            case 5210017: // 三轉傑克
            case 5210018: // 三轉斯托納
            case 12120013: // 火焰之魂
            case 12120014:
                return SummonMovementType.移動跟隨;
        }
        if (isAngel()) {
            return SummonMovementType.飛行跟隨;
        }
        return null;
    }

    public boolean is集合船员() {
        switch (sourceid) {
            case 5201012: // 二轉盲俠
            case 5201013: // 二轉瓦蕾莉
            case 5201014: // 二轉傑克
            case 5210015: // 三轉盲俠
            case 5210016: // 三轉瓦蕾莉
            case 5210017: // 三轉傑克
            case 5210018: // 三轉斯托納
                return this.skill;
        }
        return false;
    }

    public boolean is船员统帅() {
        return (this.skill) && (this.sourceid == 5220019);
    }

    public final boolean isAngel() {
        return SkillConstants.isAngel(sourceid);
    }

    public final boolean isSkill() {
        return skill;
    }

    public final int getSourceId() {
        return sourceid;
    }

    public final boolean isIceKnight() {
        return skill && MapleJob.isBeginner(sourceid / 10000) && sourceid % 10000 == 1105;
    }

    public final boolean isSoaring() {
        return isSoaring_Normal() || isSoaring_Mount();
    }

    public final boolean isSoaring_Normal() {
        return skill && MapleJob.isBeginner(sourceid / 10000) && sourceid % 10000 == 1026;
    }

    public final boolean isSoaring_Mount() {
        return skill && ((MapleJob.isBeginner(sourceid / 10000) && sourceid % 10000 == 1142) || sourceid == 80001089);
    }

    public final boolean isFinalAttack() {
        switch (sourceid) {
            case 13101002:
            case 11101002:
            case 51100002:
                return skill;
        }
        return false;
    }

    public final boolean isMistEruption() {
        switch (sourceid) {
            case 2121003:
                return skill;
        }
        return false;
    }

    public final boolean isShadow() {
        switch (sourceid) {
            case 4111002: // shadowpartner
            case 14111000: // cygnus
            case 4211008:
            case 4331002:// Mirror Image
                return skill;
        }
        return false;
    }

    public final boolean isMechPassive() {
        switch (sourceid) {
            // case 35121005:
            case 35121013:
                return true;
        }
        return false;
    }

    /**
     *
     * @return true if the effect should happen based on it's probablity, false
     * otherwise
     */
    public final boolean makeChanceResult() {
        return info.get(MapleStatInfo.prop) >= 100 || Randomizer.nextInt(100) < info.get(MapleStatInfo.prop);
    }

    public final boolean makeSubChanceResult() {
        return info.get(MapleStatInfo.subProp) >= 100 || Randomizer.nextInt(100) < info.get(MapleStatInfo.subProp);
    }

    public final int getProb() {
        return info.get(MapleStatInfo.prop);
    }

    public final short getIgnoreMob() {
        return ignoreMob;
    }

    public final int getEnhancedHP() {
        return info.get(MapleStatInfo.emhp);
    }

    public final int getEnhancedMP() {
        return info.get(MapleStatInfo.emmp);
    }

    public final int getEnhancedWatk() {
        return info.get(MapleStatInfo.epad);
    }

    public final int getEnhancedWdef() {
        return info.get(MapleStatInfo.pdd);
    }

    public final int getEnhancedMatk() {
        return info.get(MapleStatInfo.emad);
    }

    public final int getEnhancedMdef() {
        return info.get(MapleStatInfo.emdd);
    }

    public final int getDOT() {
        return info.get(MapleStatInfo.dot);
    }

    public final int getDOTTime() {
        return info.get(MapleStatInfo.dotTime);
    }

    public final int getCriticalMax() {
        return info.get(MapleStatInfo.criticaldamageMax);
    }

    public final int getCriticalMin() {
        return info.get(MapleStatInfo.criticaldamageMin);
    }

    public final int getASRRate() {
        return info.get(MapleStatInfo.asrR);
    }

    public final int getTERRate() {
        return info.get(MapleStatInfo.terR);
    }

    public final int getDAMRate() {
        return info.get(MapleStatInfo.damR);
    }

    public final int getEXPLossRate() {
        return info.get(MapleStatInfo.expLossReduceR);
    }

    public final int getBuffTimeRate() {
        return info.get(MapleStatInfo.bufftimeR);
    }

    public final int getSuddenDeathR() {
        return info.get(MapleStatInfo.suddenDeathR);
    }

    public final int getPercentAcc() {
        return info.get(MapleStatInfo.accR);
    }

    public final int getPercentAvoid() {
        return info.get(MapleStatInfo.evaR);
    }

    public final int getSummonTimeInc() {
        return info.get(MapleStatInfo.summonTimeR);
    }

    public final int getMPConsumeEff() {
        return info.get(MapleStatInfo.mpConEff);
    }

    public final short getMesoRate() {
        return mesoR;
    }

    public final int getEXP() {
        return exp;
    }

    public final int getWdefToMdef() {
        return info.get(MapleStatInfo.pdd2mdd);
    }

    public final int getMdefToWdef() {
        return info.get(MapleStatInfo.mdd2pdd);
    }

    public final int getAvoidToHp() {
        return info.get(MapleStatInfo.eva2hp);
    }

    public final int getAccToMp() {
        return info.get(MapleStatInfo.acc2mp);
    }

    public final int getStrToDex() {
        return info.get(MapleStatInfo.str2dex);
    }

    public final int getDexToStr() {
        return info.get(MapleStatInfo.dex2str);
    }

    public final int getIntToLuk() {
        return info.get(MapleStatInfo.int2luk);
    }

    public final int getLukToDex() {
        return info.get(MapleStatInfo.luk2dex);
    }

    public final int getHpToDamageX() {
        return info.get(MapleStatInfo.mhp2damX);
    }

    public final int getMpToDamageX() {
        return info.get(MapleStatInfo.mmp2damX);
    }

    public final int getLevelToMaxHp() {
        return info.get(MapleStatInfo.lv2mhp);
    }

    public final int getLevelToMaxMp() {
        return info.get(MapleStatInfo.lv2mmp);
    }

    public final int getLevelToDamageX() {
        return info.get(MapleStatInfo.lv2damX);
    }

    public final int getLevelToWatk() {
        return info.get(MapleStatInfo.lv2pad);
    }

    public final int getLevelToMatk() {
        return info.get(MapleStatInfo.lv2mad);
    }

    public final int getLevelToWatkX() {
        return info.get(MapleStatInfo.lv2pdX);
    }

    public final int getLevelToMatkX() {
        return info.get(MapleStatInfo.lv2mdX);
    }

    public final int getAttackR() {
        return info.get(MapleStatInfo.padR);
    }

    public final int getAttackX() {
        return info.get(MapleStatInfo.padX);
    }

    public final int getMagicX() {
        return info.get(MapleStatInfo.madX);
    }

    public final int getPercentHP() {
        return info.get(MapleStatInfo.mhpR);
    }

    public final int getPercentMP() {
        return info.get(MapleStatInfo.mmpR);
    }

    public final int getConsumeOnPickup() {
        return consumeOnPickup;
    }

    public final int getRunOnPickup() {
        return runOnPickup;
    }

    public final int getSelfDestruction() {
        return info.get(MapleStatInfo.selfDestruction);
    }

    public final int getPowerEnergy() {
        return info.get(MapleStatInfo.powerCon);
    }

    public final int getPPRecovery() {
        return info.get(MapleStatInfo.ppRecovery);
    }

    public final int getPPCon() {
        return info.get(MapleStatInfo.ppCon);
    }

    public int getHealHPPerSecondR() {
        return info.get(MapleStatInfo.dotHealHPPerSecondR);
    }

    public int getHealMPPerSecondR() {
        return info.get(MapleStatInfo.dotHealMPPerSecondR);
    }

    public int calculateHealHPPerSecond(MapleCharacter chr) {
        return chr.getStat().getHp() * this.getHealHPPerSecondR() / 100;
    }

    public int calculateHealMPPerSecond(MapleCharacter chr) {
        return chr.getStat().getMp() * this.getHealMPPerSecondR() / 100;
    }

    public final int getCharColor() {
        return charColor;
    }

    public final List<Integer> getPetsCanConsume() {
        return petsCanConsume;
    }

    public final boolean isReturnScroll() {
        return skill && (sourceid == 80001040 || sourceid == 20021110 || sourceid == 20031203);
    }

    public final boolean isMechChange() {
        switch (sourceid) {
            case 35111004: // siege
            case 35001001: // flame
            case 35101009:
            case 35121013:
            case 35121005:
                return skill;
        }
        return false;
    }

    public final boolean isAnimalMode() {
        return skill
                && (sourceid == 110001501 || sourceid == 110001502 || sourceid == 110001503 || sourceid == 110001504);
    }

    public final int getRange() {
        return info.get(MapleStatInfo.range);
    }

    public final int getER() {
        return info.get(MapleStatInfo.er);
    }

    public final int getPrice() {
        return info.get(MapleStatInfo.price);
    }

    public final int getExtendPrice() {
        return info.get(MapleStatInfo.extendPrice);
    }

    public final int getPeriod() {
        return info.get(MapleStatInfo.period);
    }

    public final int getReqGuildLevel() {
        return info.get(MapleStatInfo.reqGuildLevel);
    }

    public final int getEXPRate() {
        return info.get(MapleStatInfo.expR);
    }

    public final short getLifeID() {
        return lifeId;
    }

    public final short getUseLevel() {
        return useLevel;
    }

    public final byte getSlotCount() {
        return slotCount;
    }

    public final int getStr() {
        return info.get(MapleStatInfo.str);
    }

    public final int getStrX() {
        return info.get(MapleStatInfo.strX);
    }

    public final int getStrFX() {
        return info.get(MapleStatInfo.strFX);
    }

    public final int getStrRate() {
        return info.get(MapleStatInfo.strR);
    }

    public final int getDex() {
        return info.get(MapleStatInfo.dex);
    }

    public final int getDexX() {
        return info.get(MapleStatInfo.dexX);
    }

    public final int getDexFX() {
        return info.get(MapleStatInfo.dexFX);
    }

    public final int getDexRate() {
        return info.get(MapleStatInfo.dexR);
    }

    public final int getInt() {
        return info.get(MapleStatInfo.int_);
    }

    public final int getIntX() {
        return info.get(MapleStatInfo.intX);
    }

    public final int getIntFX() {
        return info.get(MapleStatInfo.intFX);
    }

    public final int getIntRate() {
        return info.get(MapleStatInfo.intR);
    }

    public final int getLuk() {
        return info.get(MapleStatInfo.luk);
    }

    public final int getLukX() {
        return info.get(MapleStatInfo.lukX);
    }

    public final int getLukFX() {
        return info.get(MapleStatInfo.lukFX);
    }

    public final int getLukRate() {
        return info.get(MapleStatInfo.lukR);
    }

    public final int getMaxHpX() {
        return info.get(MapleStatInfo.mhpX);
    }

    public final int getMaxMpX() {
        return info.get(MapleStatInfo.mmpX);
    }

    public final int getMaxDemonFury() {
        return info.get(MapleStatInfo.MDF);
    }

    public final int getTargetPlus() {
        return info.get(MapleStatInfo.targetPlus);
    }

    public final int getPassivePlus() {
        return info.get(MapleStatInfo.passivePlus);
    }

    public final int getAccX() {
        return info.get(MapleStatInfo.accX);
    }

    public final int getMPConReduce() {
        return info.get(MapleStatInfo.mpConReduce);
    }

    public final int getIndieMHp() {
        return info.get(MapleStatInfo.indieMhp);
    }

    public final int getIndieMMp() {
        return info.get(MapleStatInfo.indieMmp);
    }

    public final int getIndieAllStat() {
        return info.get(MapleStatInfo.indieAllStat);
    }

    public final byte getType() {
        return type;
    }

    public int getBossDamage() {
        return info.get(MapleStatInfo.bdR);
    }

    public int getInterval() {
        return interval;
    }

    public ArrayList<Pair<Integer, Integer>> getAvailableMaps() {
        return availableMap;
    }

    public int getWDEFX() {
        return info.get(MapleStatInfo.pddX);
    }

    public int getMDEFX() {
        return info.get(MapleStatInfo.mddX);
    }

    public int getWDEFRate() {
        return info.get(MapleStatInfo.pddR);
    }

    public int getMDEFRate() {
        return info.get(MapleStatInfo.mddR);
    }

    public final int getSoulMPCon() {
        return info.get(MapleStatInfo.soulmpCon);
    }

    public final int getLevelToStr() {
        return info.get(MapleStatInfo.lv2str);
    }

    public final int getLevelToDex() {
        return info.get(MapleStatInfo.lv2dex);
    }

    public final int getLevelToInt() {
        return info.get(MapleStatInfo.lv2int);
    }

    public final int getLevelToLuk() {
        return info.get(MapleStatInfo.lv2luk);
    }

    public final int getPercentMATK() {
        return info.get(MapleStatInfo.madR);
    }

    public final int getKillRecoveryR() {
        return info.get(MapleStatInfo.killRecoveryR);
    }

    public final int getDamAbsorbShieldR() {
        return info.get(MapleStatInfo.damAbsorbShieldR);
    }

    public final int getSubProb() {
        return info.get(MapleStatInfo.subProp);
    }

    public int getWeapon() {
        return weapon;
    }

    public final int getCombo() {
        return this.combo;
    }

    public static class CancelEffectAction implements Runnable {

        private final MapleStatEffect effect;
        private final WeakReference<MapleCharacter> target;
        private final long startTime;
        private final Map<MapleBuffStat, Integer> statup;

        public CancelEffectAction(final MapleCharacter target, final MapleStatEffect effect, final long startTime,
                final Map<MapleBuffStat, Integer> statup) {
            this.effect = effect;
            this.target = new WeakReference<>(target);
            this.startTime = startTime;
            this.statup = statup;
        }

        @Override
        public void run() {
            final MapleCharacter realTarget = target.get();
            if (realTarget != null && !realTarget.isClone()) {
                realTarget.cancelEffect(effect, false, startTime, statup);
            }
        }
    }

    public final boolean isUnstealable() {
        for (MapleBuffStat b : statups.keySet()) {
            if (b == MapleBuffStat.BasicStatUp) {
                return true;
            }
        }
        return sourceid == 4221013;
    }

    public boolean is战法灵气() {
        return (this.skill)
                && ((this.sourceid == 32001003) || (this.sourceid == 32101003) || (this.sourceid == 32111012)
                || (this.sourceid == 32110000) || (this.sourceid == 32120000) || (this.sourceid == 32120001));
    }

    public boolean is狂龙变形() {
        return (this.skill) && ((this.sourceid == 61111008) || (this.sourceid == 61120008));
    }
}
