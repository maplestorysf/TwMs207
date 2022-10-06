/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.skill.vcore;

import server.client.CoreDataEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.StringUtil;

/**
 *
 * @author Weber
 */
public class VCoreFactory {

    public static class Enforcement {

        private static final Map<Integer, VCoreEnforementEntry> 技能 = new HashMap<>();
        private static final Map<Integer, VCoreEnforementEntry> 強化 = new HashMap<>();
        private static final Map<Integer, VCoreEnforementEntry> 特殊 = new HashMap<>();

        public static void load() {
            final MapleData vcoreData = MapleDataProviderFactory.getDataProvider("Etc").getData("VCore.img");
            final MapleData forSkill = vcoreData.getChildByPath("Enforcement/Skill");
            final MapleData forEnforce = vcoreData.getChildByPath("Enforcement/Enforce");
            final MapleData forSpecial = vcoreData.getChildByPath("Enforcement/Special");
            for (MapleData sub : forSkill.getChildren()) {
                int level = Integer.parseInt(sub.getName());
                int nextExp = MapleDataTool.getInt("nextExp", sub);
                int expEnforce = MapleDataTool.getInt("nextExp", sub);
                int extract = MapleDataTool.getInt("nextExp", sub);
                技能.put(level, new VCoreEnforementEntry(level, nextExp, expEnforce, extract));
            }
            for (MapleData sub : forEnforce.getChildren()) {
                int level = Integer.parseInt(sub.getName());
                int nextExp = MapleDataTool.getInt("nextExp", sub);
                int expEnforce = MapleDataTool.getInt("nextExp", sub);
                int extract = MapleDataTool.getInt("nextExp", sub);
                強化.put(level, new VCoreEnforementEntry(level, nextExp, expEnforce, extract));
            }
            for (MapleData sub : forSpecial.getChildren()) {
                int level = Integer.parseInt(sub.getName());
                int nextExp = MapleDataTool.getInt("nextExp", sub);
                int expEnforce = MapleDataTool.getInt("nextExp", sub);
                int extract = MapleDataTool.getInt("nextExp", sub);
                特殊.put(level, new VCoreEnforementEntry(level, nextExp, expEnforce, extract));
            }
        }

        public static int getNextExp(int type, int level) {
            switch (type) {
                case 0:
                    return 技能.get(level).getNextExp();
                case 1:
                    return 強化.get(level).getNextExp();
                case 3:
                    return 特殊.get(level).getNextExp();
                default:
                    return 0;
            }
        }

        public static int getExpEnforce(int type, int level) {
            switch (type) {
                case 0:
                    return 技能.get(level).getExpEnforce();
                case 1:
                    return 強化.get(level).getExpEnforce();
                case 3:
                    return 特殊.get(level).getExpEnforce();
                default:
                    return 0;
            }
        }

        public static int getExtract(int type, int level) {
            switch (type) {
                case 0:
                    return 技能.get(level).getExtract();
                case 1:
                    return 強化.get(level).getExtract();
                case 3:
                    return 特殊.get(level).getExtract();
                default:
                    return 0;
            }
        }
    }

    public static class CoreData {

        private static final Map<Integer, CoreDataEntry> coreData = new HashMap<>();
        private static final Map<Integer, List<Integer>> jobMapToCoresId = new HashMap<>();

        public static void load() {
            final MapleData vCore = MapleDataProviderFactory.getDataProvider("Etc").getData("VCore.img");
            final MapleData _coreData = vCore.getChildByPath("CoreData");
            for (MapleData sub : _coreData.getChildren()) {
                int coreId = Integer.parseInt(sub.getName());
                String name = MapleDataTool.getString("name", sub);
                String desc = MapleDataTool.getString("desc", sub);
                int type = MapleDataTool.getInt("type", sub);
                int maxLevel = MapleDataTool.getInt("maxLevel", sub);
                List<Integer> jobs = new ArrayList<>();
                String jobValue = MapleDataTool.getString("job/0", sub);
                int jobId = 0;
                if (StringUtil.isNumber(jobValue)) {
                    jobId = Integer.parseInt(jobValue);
                } else if (!jobValue.equals("all")) {
                    continue;
                }
                if (!jobMapToCoresId.containsKey(jobId)) {
                    jobMapToCoresId.put(jobId, new ArrayList<>());
                }
                jobMapToCoresId.get(jobId).add(coreId);
                jobs.add(jobId);
                List<Integer> connectSkill = new ArrayList<>();
                // System.out.println(coreId);
                MapleData connectSkillData = sub.getChildByPath("connectSkill");
                if (connectSkillData != null) {
                    int connectSkillId = MapleDataTool.getInt("connectSkill/0", sub);
                    connectSkill.add(connectSkillId);
                }
                coreData.put(coreId, new CoreDataEntry(coreId, name, desc, type, maxLevel, jobs, connectSkill));
            }
        }

        public static List<Integer> getVCoresIdByJob(int job) {
            List<Integer> coresId = jobMapToCoresId.get(job);
            return coresId;
        }

        public static List<Integer> getAllVCoresId() {
            List<Integer> coresId = new ArrayList<>();
            for (List<Integer> sub : jobMapToCoresId.values()) {
                coresId.addAll(sub);
            }
            return coresId;
        }

        public static CoreDataEntry getCoreData(int coreId) {
            return coreData.get(coreId);
        }
    }

}
