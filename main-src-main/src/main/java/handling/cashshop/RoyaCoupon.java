/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.cashshop;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author pungin
 */
public class RoyaCoupon {

    public static class RoyaInfo {

        public int gender;
        public int styleId;
        public int chance;
        boolean showOnCS;

        RoyaInfo(int gender, int styleId, int chance, boolean showOnCS) {
            this.gender = gender;
            this.styleId = styleId;
            this.chance = chance;
            this.showOnCS = showOnCS;
        }
    }

    private final List<RoyaInfo> styles = new LinkedList<>();

    public void addStyle(int gender, int styleId, int chance, boolean showOnCS) {
        styles.add(new RoyaInfo(gender, styleId, chance, showOnCS));
    }

    public List<RoyaInfo> getMaleStyles(boolean onCS) {
        List<RoyaInfo> list = new LinkedList<>();
        for (RoyaInfo roya : styles) {
            if ((onCS && !roya.showOnCS) || roya.gender != 0) {
                continue;
            }
            list.add(roya);
        }
        return list;
    }

    public List<RoyaInfo> getFemaleStyles(boolean onCS) {
        List<RoyaInfo> list = new LinkedList<>();
        for (RoyaInfo roya : styles) {
            if ((onCS && !roya.showOnCS) || roya.gender != 1) {
                continue;
            }
            list.add(roya);
        }
        return list;
    }
}
