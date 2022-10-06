/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import client.MapleCharacter;
import java.util.ArrayList;
import java.util.List;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;

/**
 *
 * @author Weber
 */
public class GrowHelpFactory {

    private class GrowHelpField {

        public List<Integer> jobExcept = new ArrayList<>();
        public int beginLv;
        public int endLv;
        public int field;
        public int moveLv;
        public boolean isBoss;
        public int quest;
        public int nfield;
        public int quest2;
        public int nfield2;

        public GrowHelpField() {
            // TODO: 好像還有一些任務、NPC資料
        }

        public boolean checkJob(int job) {
            return !this.jobExcept.contains(job);
        }

    }

    private final MapleDataProvider data = MapleDataProviderFactory.getDataProvider("Etc");
    private final List<GrowHelpField> fields = new ArrayList<>();
    private static final GrowHelpFactory instance = new GrowHelpFactory();

    public static GrowHelpFactory getInstance() {
        return instance;
    }

    public void load() {
        MapleData growHelpData = data.getData("GrowHelp.img");

        MapleData fieldsData = growHelpData.getChildByPath("field");
        MapleData fieldsDefault = fieldsData.getChildByPath("default");

        for (MapleData fieldData : fieldsDefault.getChildren()) {
            GrowHelpField field = new GrowHelpField();
            int fieldId = Integer.parseInt(fieldData.getName());
            field.beginLv = MapleDataTool.getInt("beginLv", fieldData);
            field.endLv = MapleDataTool.getInt("endLv", fieldData);
            field.field = MapleDataTool.getInt("field", fieldData);
            field.moveLv = MapleDataTool.getInt("moveLv", fieldData);
            field.isBoss = MapleDataTool.getInt("isBoss", fieldData, 0) == 1;
            field.quest = MapleDataTool.getInt("condition/quest", fieldData, 0);
            field.quest2 = MapleDataTool.getInt("condition/ques2t", fieldData, 0);
            field.nfield = MapleDataTool.getInt("condition/nfield", fieldData, -1);
            field.nfield2 = MapleDataTool.getInt("condition/nfield2", fieldData, -1);
            if (fieldData.getChildByPath("jobExcept") != null) {
                for (MapleData exceptData : fieldData.getChildByPath("jobExcept").getChildren()) {
                    field.jobExcept.add(MapleDataTool.getInt(exceptData));
                }
            }
            fields.add(field);
        }
    }

    public boolean canEnterMap(MapleCharacter player, int mapId) {
        // TODO: 有個沙小印章的成就可以無視等級
        for (GrowHelpField field : fields) {
            if (mapId == field.field || mapId == field.nfield || mapId == field.nfield2) {
                int level = player.getLevel();
                int job = player.getJob();
                if (level >= field.beginLv && (level <= field.moveLv || field.isBoss) && (field.checkJob(job))) {
                    return true;
                }
            }
        }
        return false;
    }

}
