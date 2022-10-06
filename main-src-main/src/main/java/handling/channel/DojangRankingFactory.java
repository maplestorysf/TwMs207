/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel;

/**
 *
 * @author Maple
 */

import client.AvartarLook;

import java.util.ArrayList;
import java.util.List;

public class DojangRankingFactory {

    private static final int[] rankLimits = new int[]{20, 40, 100};
    private final DojangRankList[] ranks;

    public class DojangRankDataEntry {

        public int job;
        public int level;
        public int point;
        public int ranking;
        public String name;
        public boolean hasAvartar = false;
        public AvartarLook avatar;
    }

    public class DojangRankList {

        final int type;
        final List<DojangRankDataEntry> data;

        public DojangRankList(int type) {
            this.type = type;
            data = new ArrayList<>();
        }

        public List<DojangRankDataEntry> getData() {
            return data;
        }

        public void loadRankData() {
            // TODO
        }
    }

    private static DojangRankingFactory instance = new DojangRankingFactory();

    private DojangRankingFactory() {
        ranks = new DojangRankList[3];
        for (int i = 0; i < 3; i++) {
            ranks[i] = new DojangRankList(i);
        }
    }

    public static DojangRankingFactory getInstance() {
        return instance;
    }

    public void load() {
        for (int i = 0; i < 3; i++) {
            ranks[i].loadRankData();
        }
    }

    public DojangRankList getRankList(int type) {
        return ranks[type];
    }

}
