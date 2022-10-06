package client;

import constants.GameConstants;
import tools.packet.CWvsContext.InfoPacket;

public class MapleTrait {

    public static enum MapleTraitType { // in order

        // 領導力
        charisma(500, MapleStat.CHARISMA), // ambition
        // 洞察力
        insight(500, MapleStat.INSIGHT),
        // 意志
        will(500, MapleStat.WILL), // willpower
        // 手藝
        craft(500, MapleStat.CRAFT), // diligence
        // 感性
        sense(500, MapleStat.SENSE), // empathy
        // 魅力
        charm(5000, MapleStat.CHARM);
        final int limit;
        final MapleStat stat;

        private MapleTraitType(int type, MapleStat theStat) {
            this.limit = type;
            this.stat = theStat;
        }

        public int getLimit() {
            return limit;
        }

        public MapleStat getStat() {
            return stat;
        }

        public static MapleTraitType getByQuestName(String q) {
            String qq = q.substring(0, q.length() - 3); // e.g. charmEXP, charmMin
            for (MapleTraitType t : MapleTraitType.values()) {
                if (t.name().equals(qq)) {
                    return t;
                }
            }
            return null;
        }
    }

    private final MapleTraitType type;
    private int exp = 0, localExp = 0;
    private short levelExp = 0;
    private byte level = 0;

    public MapleTrait(MapleTraitType t) {
        this.type = t;
    }

    public void setExp(int e) {
        this.exp = e;
        this.localExp = e;
        recalcLevel();
    }

    public void addExp(int e) {
        this.exp += e;
        this.localExp += e;
        if (e != 0) {
            recalcLevel();
        }
    }

    public void addExp(int e, MapleCharacter c) {
        addTrueExp(e * c.getClient().getChannelServer().getTraitRate(), c);
    }

    public void addTrueExp(int e, MapleCharacter c) {
        if (e != 0) {
            this.exp += e;
            this.localExp += e;
            c.updateSingleStat(type.stat, exp);
            c.getClient().getSession().writeAndFlush(InfoPacket.showTraitGain(type, e));
            recalcLevel();
        }
    }

    public boolean recalcLevel() {
        if (exp < 0) {
            exp = 0;
            localExp = 0;
            level = 0;
            levelExp = 0;
            return false;
        }
        final int oldLevel = level;
        for (byte i = 0; i < 100; i++) {
            if (GameConstants.getTraitExpNeededForLevel(i) > localExp) {
                levelExp = (short) (GameConstants.getTraitExpNeededForLevel(i) - localExp);
                level = (byte) (i - 1);
                return level > oldLevel;
            }
        }
        levelExp = 0;
        level = 100;
        exp = GameConstants.getTraitExpNeededForLevel(level);
        localExp = exp;
        return level > oldLevel;
    }

    public int getLevel() {
        return level;
    }

    public int getLevelExp() {
        return levelExp;
    }

    public int getExp() {
        return exp;
    }

    public int getLocalExp() {
        return localExp;
    }

    public void addLocalExp(int e) {
        this.localExp += e;
    }

    public void clearLocalExp() {
        this.localExp = exp;
    }

    public MapleTraitType getType() {
        return type;
    }
}
