/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.client;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Weber
 */
public class CoreDataEntry {

    private int coreId;
    private String name;
    private String desc;
    private int type;
    private int maxLevel;
    public List<Integer> job;
    public List<Integer> connectSkill;

    public CoreDataEntry(int coreId, String name, String desc, int type, int maxLevel, List<Integer> job,
            List<Integer> connectSkill) {
        this.coreId = coreId;
        this.name = name;
        this.desc = desc;
        this.type = type;
        this.maxLevel = maxLevel;
        this.job = new ArrayList<>(job);
        this.connectSkill = new ArrayList<>(connectSkill);
    }

    public int getCoreId() {
        return coreId;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public int getType() {
        return type;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public List<Integer> getJob() {
        return job;
    }

    public List<Integer> getConnectSkill() {
        return connectSkill;
    }

    public boolean hasConnectSkill(int skillId) {
        return connectSkill.contains(skillId);
    }

    public boolean hasJob(int jobId) {
        return job.contains(jobId);
    }

    @Override
    public String toString() {
        return "[V技能核心] 核心編號：" + this.coreId + " 名稱：" + this.name + " 描述：" + this.desc;
    }

}
