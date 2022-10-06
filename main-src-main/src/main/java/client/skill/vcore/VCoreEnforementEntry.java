/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.skill.vcore;

public class VCoreEnforementEntry {

    private final int level;
    private final int nextExp;
    private final int expEnforce;
    private final int extract;

    public VCoreEnforementEntry(int level, int nextExp, int expEnforce, int extract) {
        this.level = level;
        this.nextExp = nextExp;
        this.expEnforce = expEnforce;
        this.extract = extract;
    }

    public int getLevel() {
        return level;
    }

    public int getNextExp() {
        return nextExp;
    }

    public int getExpEnforce() {
        return expEnforce;
    }

    public int getExtract() {
        return extract;
    }

    @Override
    public String toString() {
        return "[V技能強化資料] 等級：" + this.level + " 升級所需經驗：" + this.nextExp + " 強化經驗：" + this.expEnforce + " 分解碎片："
                + this.extract;
    }

}
