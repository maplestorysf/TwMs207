package client.familiar;

import client.MapleClient;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.StructItemOption;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterStats;
import server.maps.AnimatedMapleMapObject;
import server.maps.MapleMapObjectType;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.StaticLifeMovement;
import tools.HexTool;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.FamiliarPacket;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MonsterFamiliar extends AnimatedMapleMapObject implements Serializable {

    private static final long serialVersionUID = 795419937713738569L;
    private final int id;
    private final int familiar;
    private final int characterid;
    private String name;
    private byte grade;
    private byte gradelevel;
    private int option1;
    private int option2;
    private int option3;
    private short skill;
    private int exp;
    private double pad;
    private int index;

    public MonsterFamiliar(int id, int familiar, int characterid, String name, byte grade, byte gradelevel, int exp,
            short skill, int option1, int option2, int option3) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        this.id = id;
        this.familiar = familiar;
        this.characterid = characterid;
        this.name = name;
        this.grade = (byte) Math.min(Math.max(1, grade), 4);
        this.gradelevel = (byte) Math.min(Math.max(1, gradelevel), 5);
        this.exp = exp;
        this.skill = skill;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.pad = ii.getFamiliarTable_pad().get((int) this.grade).get(this.gradelevel - 1);
        super.setStance(0);
        super.setFh(0);
        super.setPosition(new Point(0, 0));
    }

    public byte getGrade() {
        return grade;
    }

    public void setGrade(byte grade) {
        this.grade = grade;
    }

    public byte getLevel() {
        return gradelevel;
    }

    public void setGradelevel(byte gradelevel) {
        this.gradelevel = gradelevel;
    }

    public int getOption1() {
        return option1;
    }

    public void setOption1(int option1) {
        this.option1 = option1;
    }

    public int getOption2() {
        return option2;
    }

    public void setOption2(int option2) {
        this.option2 = option2;
    }

    public int getOption3() {
        return option3;
    }

    public void setOption3(int option3) {
        this.option3 = option3;
    }

    public short getSkill() {
        return skill;
    }

    public void setSkill(short skill) {
        this.skill = skill;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public void upgrade() {
        this.grade += 1;
        this.gradelevel = 1;
        if (this.grade > 4) {
            this.grade = 4;
        }
    }

    public MonsterFamiliar(int characterid, int familiar, FamiliarCard card) {
        super.setFh(0);
        this.familiar = familiar;
        this.characterid = characterid;
        name = getOriginalName();
        id = Randomizer.nextInt();
        this.grade = card.getGrade();
        this.gradelevel = card.getLevel();
        this.skill = (short) (card.getSkill() <= 0 ? Randomizer.rand(800, 904) + 1 : card.getSkill());
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        this.pad = ii.getFamiliarTable_pad().get((int) this.grade).get(this.gradelevel - 1);
        if (this.option1 <= 0) {
            this.initOptions();
            return;
        }
        this.option1 = card.getOption1();
        this.option2 = card.getOption2();
        this.option3 = card.getOption3();
    }

    public void initOptions() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (int i = 0; i < 3; i++) {
            Map<Integer, List<StructItemOption>> optTypes = ii.getFamiliar_option().entrySet().stream()
                    .filter(p -> (p.getKey() / 10000) == this.grade)
                    .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
            List<List<StructItemOption>> opts = new ArrayList<>(optTypes.values());
            Collections.shuffle(opts);
            List<StructItemOption> opt = opts.get(0);
            this.setOption(i, opt.get(this.gradelevel).opID);
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getOption(int i) {
        switch (i) {
            case 2: {
                return this.option3;
            }
            case 1: {
                return this.option2;
            }
            case 0: {
                return this.option1;
            }
            default: {
                return 0;
            }
        }
    }

    public int setOption(int i, int i0) {
        switch (i) {
            case 0: {
                this.option1 = i0;
            }
            case 1: {
                this.option2 = i0;
            }
            case 2: {
                this.option3 = i0;
            }
            default: {
                return 0;
            }
        }
    }

    public String getOriginalName() {
        return getOriginalStats().getName();
    }

    public MapleMonsterStats getOriginalStats() {
        return MapleLifeFactory
                .getMonsterStats(MapleItemInformationProvider.getInstance().getFamiliar(familiar).getMobId());
    }

    public int getFamiliar() {
        return familiar;
    }

    public int getId() {
        return id;
    }

    public int getCharacterId() {
        return characterid;
    }

    public final String getName() {
        return name;
    }

    public void setName(String n) {
        name = n;
    }

    public double getPad() {
        return pad;
    }

    public void setPad(double pad) {
        this.pad = pad;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().writeAndFlush(FamiliarPacket.spawnFamiliar(this, true, false));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(FamiliarPacket.spawnFamiliar(this, false, false));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.FAMILIAR;
    }

    public final void updatePosition(List<LifeMovementFragment> movement) {
        for (LifeMovementFragment move : movement) {
            if (((move instanceof LifeMovement)) && ((move instanceof StaticLifeMovement))) {
                setFh(((StaticLifeMovement) move).getNewFh());
            }
        }
    }

    public void writeRegisterPacket(MaplePacketLittleEndianWriter mplew, boolean chr) {
        mplew.writeInt(this.getId());
        mplew.writeInt(0);
        mplew.writeInt(2);
        mplew.writeInt(this.getFamiliar());
        mplew.writeAsciiString(this.getName(), 15);
        mplew.write(0);
        mplew.writeShort(1);
        mplew.writeShort(this.getSkill());
        mplew.writeInt(this.getExp());
        mplew.writeShort(this.getLevel());
        mplew.writeShort(this.getOption1());
        mplew.writeShort(this.getOption2());
        mplew.writeShort(this.getOption3());
        mplew.write(0xB);
        mplew.write(this.getGrade());
        mplew.write(chr ? 32 : 0);
        mplew.write(chr ? 100 : 0);
        mplew.write(HexTool.getByteArrayFromHexString("8C FE 66 15"));
    }
}
