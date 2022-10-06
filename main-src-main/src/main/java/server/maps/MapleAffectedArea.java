package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import client.skill.Skill;
import client.skill.SkillFactory;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.ScheduledFuture;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.life.MobSkill;
import tools.packet.CField;

public class MapleAffectedArea extends MapleMapObject {

    private Rectangle areaBox;
    private MapleStatEffect source;
    private MobSkill skill;
    private boolean isMobMist, isPoisonMist, isRecovery, isHolyFountain, isShelter, isGiveBuff = false,
            isFaceLeft = false;
    private int skillDelay, skilllevel, ownerId, mistType, healCount;
    private ScheduledFuture<?> schedule = null, poisonSchedule = null;

    public MapleAffectedArea(Rectangle areaBox, MapleMonster mob, MobSkill skill) {
        this.areaBox = areaBox;
        ownerId = mob.getId();
        this.skill = skill;
        skilllevel = skill.getSkillLevel();
        isMobMist = true;
        isPoisonMist = true;
        isRecovery = false;
        mistType = 0;
        skillDelay = 0;
    }

    public MapleAffectedArea(Rectangle areaBox, MapleCharacter owner, MapleStatEffect source) {
        this.areaBox = areaBox;
        ownerId = owner.getId();
        this.source = source;
        skillDelay = 10;
        isMobMist = false;
        isPoisonMist = false;
        isRecovery = false;
        healCount = 0;
        isHolyFountain = false;
        isFaceLeft = owner.isFacingLeft();
        skilllevel = owner.getTotalSkillLevel(SkillFactory.getSkill(source.getSourceId()));
        switch (source.getSourceId()) { // TODO MIST::添加技能類型
            case 12121005: // 燃燒軍團
            case 35121010:
                mistType = 0;
                skillDelay = 2;
                isGiveBuff = true;
                break;
            case 42111004: // 結界‧櫻
            case 42121005: // 結界‧桔梗
                mistType = 0;
                skillDelay = 10;
                isGiveBuff = true;
                break;
            case 42101005: // 妖雲召喚
                mistType = 0;
                skillDelay = 7;
                isPoisonMist = true;
                break;
            case 2311011:
                mistType = 0;
                healCount = source.getY();
                isHolyFountain = true;
                break;
            case 2100010:
            case 4121015:
            case 24121052:
            case 25111206:
            case 61121105:
            case 35121052:
            case 33121012:
            case 100001261:
                mistType = 0;
                break;
            case 4221006:
                mistType = 3;
                skillDelay = 3;
                isPoisonMist = true;
                break;
            case 32121006:
                mistType = 3;
                break;
            case 36121007: // 时间胶囊
                mistType = 5;
                break;
            case 2201009:
                mistType = 0;
                skillDelay = 3;
                break;
            case 1076:
            case 2111003:
            case 12111005:
            case 14111006:
                mistType = 0;
                isPoisonMist = true;
                break;
            case 22161003:
                mistType = 0;
                isRecovery = true;
                break;
        }
    }

    public MapleAffectedArea(Rectangle areaBox, MapleCharacter owner) {
        this.areaBox = areaBox;
        ownerId = owner.getId();
        source = new MapleStatEffect();
        source.setSourceId(2111003);
        skilllevel = 30;
        mistType = 0;
        isMobMist = false;
        isPoisonMist = false;
        skillDelay = 10;
        isFaceLeft = owner.isFacingLeft();
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.AFFECTED_AREA;
    }

    @Override
    public Point getPosition() {
        return areaBox.getLocation();
    }

    public Skill getSourceSkill() {
        if (source != null) {
            return SkillFactory.getSkill(source.getSourceId());
        } else {
            return SkillFactory.getSkill(skill.getSkillId());
        }
    }

    public void setSchedule(ScheduledFuture<?> s) {
        schedule = s;
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setPoisonSchedule(ScheduledFuture<?> s) {
        poisonSchedule = s;
    }

    public ScheduledFuture<?> getPoisonSchedule() {
        return poisonSchedule;
    }

    public boolean isFaceLeft() {
        return isFaceLeft;
    }

    public boolean isMobMist() {
        return isMobMist;
    }

    public boolean isPoisonMist() {
        return isPoisonMist;
    }

    public boolean isGiveBuff() {
        return isGiveBuff;
    }

    public boolean isShelter() {
        return isShelter;
    }

    public boolean isRecovery() {
        return isRecovery;
    }

    public boolean isHolyFountain() {
        return isHolyFountain;
    }

    public int getHealCount() {
        return isHolyFountain() ? healCount : 0;
    }

    public void setHealCount(int count) {
        healCount = count;
    }

    public int getMistType() {
        return mistType;
    }

    public int getSkillDelay() {
        return skillDelay;
    }

    public int getSkillLevel() {
        return skilllevel;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public MobSkill getMobSkill() {
        return skill;
    }

    public Rectangle getBox() {
        return areaBox;
    }

    public MapleStatEffect getSource() {
        return source;
    }

    public byte[] fakeSpawnData(int level) {
        return CField.spawnAffectedArea(this);
    }

    @Override
    public void sendSpawnData(final MapleClient c) {
        c.getSession().writeAndFlush(CField.spawnAffectedArea(this));
    }

    @Override
    public void sendDestroyData(final MapleClient c) {
        c.getSession().writeAndFlush(CField.removeAffectedArea(getObjectId(), false));
    }

    public boolean makeChanceResult() {
        return source.makeChanceResult();
    }
}
