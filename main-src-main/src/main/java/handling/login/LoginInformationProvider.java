package handling.login;

import client.MapleJob;
import constants.GameConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Triple;

public class LoginInformationProvider {

    public static enum JobInfoFlag {

        臉型(0x1),
        髮型(0x2),
        臉飾(0x4),
        耳朵(0x8),
        尾巴(0x10),
        帽子(0x20),
        衣服(0x40),
        褲裙(0x80),
        披風(0x100),
        鞋子(0x200),
        手套(0x400),
        武器(0x800),
        副手(0x1000),
        ;
        private final int value;

        private JobInfoFlag(int value) {
            this.value = value;
        }

        public int getVelue() {
            return value;
        }

        public boolean check(int x) {
            return (value & x) != 0;
        }
    }

    public static enum JobType {

        終極冒險家(-1, MapleJob.初心者.getId(), 100000000, JobInfoFlag.褲裙.getVelue()),
        末日反抗軍(0, MapleJob.市民.getId(), 931000000),
        冒險家(1, MapleJob.初心者.getId(), 4000011),
        皇家騎士團(2, MapleJob.貴族.getId(), 130030000, JobInfoFlag.披風.getVelue()),
        狂狼勇士(3, MapleJob.傳說.getId(), 914000000, JobInfoFlag.褲裙.getVelue()),
        龍魔導士(4, MapleJob.龍魔導士.getId(), 900010000, JobInfoFlag.褲裙.getVelue()),
        精靈遊俠(5, MapleJob.精靈遊俠.getId(), 910150000),
        惡魔(6, MapleJob.惡魔殺手.getId(), 931050310, JobInfoFlag.臉飾.getVelue() | JobInfoFlag.副手.getVelue()),
        幻影俠盜(7, MapleJob.幻影俠盜.getId(), 915000000, JobInfoFlag.披風.getVelue()),
        影武者(8, MapleJob.初心者.getId(), 103050900),
        米哈逸(9, MapleJob.米哈逸.getId(), 913070000, JobInfoFlag.褲裙.getVelue()),
        夜光(10, MapleJob.夜光.getId(), 927020080, JobInfoFlag.披風.getVelue()),
        凱撒(11, MapleJob.凱撒.getId(), 940001000),
        天使破壞者(12, MapleJob.天使破壞者.getId(), 940011000),
        重砲指揮官(13, MapleJob.初心者.getId(), 3000600),
        傑諾(14, MapleJob.傑諾.getId(), 931060089, JobInfoFlag.臉飾.getVelue()),
        神之子(15, MapleJob.神之子.getId(), 321000001, JobInfoFlag.披風.getVelue() | JobInfoFlag.副手.getVelue()),
        隱月(16, MapleJob.隱月.getId(), 927030050, JobInfoFlag.褲裙.getVelue() | JobInfoFlag.披風.getVelue()),
        皮卡啾(17, MapleJob.皮卡啾1轉.getId(), 927030090),
        凱內西斯(18, MapleJob.凱內西斯.getId(),331001100),
        卡蒂娜(19, MapleJob.卡蒂娜.getId(), 100000000),
        伊利恩(20, MapleJob.伊利恩.getId(), 100000000),
        // 非KMS海外職業
        蒼龍俠客(21, MapleJob.初心者.getId(), 743020100, JobInfoFlag.褲裙.getVelue()),
        劍豪(22, MapleJob.劍豪.getId(), 807100010, JobInfoFlag.帽子.getVelue() | JobInfoFlag.手套.getVelue()),
        陰陽師(23, MapleJob.陰陽師.getId(), 807100110, JobInfoFlag.帽子.getVelue() | JobInfoFlag.手套.getVelue()),
        幻獸師(24, MapleJob.幻獸師.getId(), 866100000, JobInfoFlag.臉飾.getVelue() | JobInfoFlag.耳朵.getVelue() | JobInfoFlag.尾巴.getVelue()),
        ;
        public int type, id, map;
        public int flag = JobInfoFlag.臉型.getVelue() | JobInfoFlag.髮型.getVelue() | JobInfoFlag.衣服.getVelue() | JobInfoFlag.鞋子.getVelue() | JobInfoFlag.武器.getVelue();

        private JobType(int type, int id, int map) {
            this.type = type;
            this.id = id;
            this.map = map;
        }

        private JobType(int type, int id, int map, int flag) {
            this.type = type;
            this.id = id;
            this.map = map;
            this.flag |= flag;
        }

        public static JobType getByType(int g) {
            for (JobType e : JobType.values()) {
                if (e.type == g) {
                    return e;
                }
            }
            return null;
        }

        public static JobType getById(int g) {
            for (JobType e : JobType.values()) {
                if (e.id == g) {
                    return e;
                }
            }
            return null;
        }
    }

    private final static LoginInformationProvider instance = new LoginInformationProvider();
    protected final List<String> ForbiddenName = new ArrayList<>();
    protected final Map<String, String> Curse = new HashMap<>();
    // gender, val, job
    protected final Map<Triple<Integer, Integer, Integer>, List<Integer>> makeCharInfo = new HashMap<>();
    // 0 = eyes 1 = hair 2 = haircolor 3 = skin 4 = top 5 = bottom 6 = shoes 7 =
    // weapon

    public static LoginInformationProvider getInstance() {
        return instance;
    }

    protected LoginInformationProvider() {
        final MapleDataProvider prov = MapleDataProviderFactory.getDataProvider("Etc");
        MapleData nameData = prov.getData("ForbiddenName.img");
        for (final MapleData data : nameData.getChildren()) {
            ForbiddenName.add(MapleDataTool.getString(data));
        }
        nameData = prov.getData("Curse.img");
        for (final MapleData data : nameData.getChildren()) {
            String[] curse = MapleDataTool.getString(data).split(",");
            Curse.put(curse[0], curse[1]);
            ForbiddenName.add(curse[0]);
        }
        final MapleData infoData = prov.getData("MakeCharInfo.img");
        for (MapleData dat : infoData.getChildren()) {
            if (!dat.getName().matches("^\\d+$") && !dat.getName().equals("000_1")) {
                continue;
            }
            for (MapleData d : dat.getChildren()) {
                int gender;
                if (d.getName().startsWith("female")) {
                    gender = 1;
                } else if (d.getName().startsWith("male")) {
                    gender = 0;
                } else {
                    continue;
                }

                for (MapleData da : d.getChildren()) {
                    Triple<Integer, Integer, Integer> key = new Triple<>(gender, Integer.parseInt(da.getName()),
                            dat.getName().equals("000_1") ? 1 : Integer.parseInt(dat.getName()));
                    List<Integer> our = makeCharInfo.get(key);
                    if (our == null) {
                        our = new ArrayList<>();
                        makeCharInfo.put(key, our);
                    }
                    for (MapleData dd : da.getChildren()) {
                        if (dd.getName().equalsIgnoreCase("color")) {
                            for (MapleData dda : dd.getChildren()) {
                                for (MapleData ddd : dda.getChildren()) {
                                    our.add(MapleDataTool.getInt(ddd, -1));
                                }
                            }
                        } else if (!dd.getName().equalsIgnoreCase("name")) {
                            our.add(MapleDataTool.getInt(dd, -1));
                        }
                    }
                }
            }
        }

        final MapleData uA = infoData.getChildByPath("UltimateAdventurer");
        for (MapleData dat : uA.getChildren()) {
            final Triple<Integer, Integer, Integer> key = new Triple<>(-1, Integer.parseInt(dat.getName()),
                    JobType.終極冒險家.type);
            List<Integer> our = makeCharInfo.get(key);
            if (our == null) {
                our = new ArrayList<>();
                makeCharInfo.put(key, our);
            }
            for (MapleData d : dat.getChildren()) {
                our.add(MapleDataTool.getInt(d, -1));
            }
        }
    }

    public static boolean isExtendedSpJob(int jobId) {
        return GameConstants.isSeparatedSp(jobId);
    }

    public final boolean isForbiddenName(final String in) {
        for (final String name : ForbiddenName) {
            if (in.toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String getCurseMsg(String in) {
        for (Map.Entry<String, String> entry : Curse.entrySet()) {
            in = in.replaceAll("(?i)" + entry.getKey(), entry.getValue());
        }
        return in;
    }

    public final boolean isEligibleItem(final int gender, final int val, final int job, final int item) {
        if (item < 0) {
            return false;
        }
        final Triple<Integer, Integer, Integer> key = new Triple<>(gender, val, job);
        final List<Integer> our = makeCharInfo.get(key);
        if (our == null) {
            return false;
        }
        return our.contains(item);
    }
}
