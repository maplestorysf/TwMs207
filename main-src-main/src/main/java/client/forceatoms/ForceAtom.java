package client.forceatoms;

import client.MapleCharacter;
import client.skill.SkillFactory;
import constants.GameConstants;
import java.awt.Point;
import server.MapleStatEffect;
import server.Randomizer;
import tools.FileTime;

/**
 *
 * @author o黯淡o
 */
public class ForceAtom {

    private int nCount, nInc, nFirstImpact, nSecondImpact, nAngle, nStartDelay, dwCreateTime, nMaxHitCount, nEffectIdx,
            nArriveDir, nArriveRange;
    private Point ptStart;
    private int nNum;

    public ForceAtom(MapleCharacter player, int count, int skillId, int dwTime, boolean byMob) {
        this.nCount = 0;
        this.nInc = 0;
        this.nFirstImpact = 0;
        this.nSecondImpact = 0;
        this.nAngle = 0;
        this.nStartDelay = 0;
        this.nMaxHitCount = 0;
        this.nEffectIdx = 0;
        this.nArriveDir = 0;
        this.nArriveRange = 0;
        this.nNum = 0;
        this.dwCreateTime = 0;
        this.ptStart = new Point(0, 0);
        this.Define(player, count, skillId, dwTime, byMob);
    }

    public int getInc() {
        return this.nInc;
    }

    public int getFirstImpact() {
        return this.nFirstImpact;
    }

    public int getSecondImpact() {
        return this.nSecondImpact;
    }

    public int getAngle() {
        return this.nAngle;
    }

    public int getStartDelay() {
        return this.nStartDelay;
    }

    public int getCreateTime() {
        return this.dwCreateTime;
    }

    public int getMaxHitCount() {
        return this.nMaxHitCount;
    }

    public int getEffectIdx() {
        return this.nEffectIdx;
    }

    public Point getStart() {
        return this.ptStart;
    }

    public void setStartDelay(int delay) {
        this.nStartDelay = delay;
    }

    public ForceAtom Define(MapleCharacter player, int count, int skillId, int createTime, boolean byMob) {
        this.nCount = count;
        int linked = GameConstants.getLinkedAttackSkill(skillId);
        int skillLevel = player.getSkillLevel(linked);
        MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(player.getSkillLevel(linked));
        this.nMaxHitCount = effect.getAttackCount();
        switch (linked) {
            case 31000004:
            case 31001006:
            case 31001007:
            case 30010166:
            case 30011167:
            case 30011168:
            case 30011169:
            case 30011170:
            case 31110008:
            case 31221001: { // 盾牌追擊
                this.nInc = 3;
                this.nFirstImpact = byMob ? Randomizer.rand(0x20, 0x30) : Randomizer.rand(0x0F, 0x20);
                this.nSecondImpact = byMob ? Randomizer.rand(3, 4) : Randomizer.rand(0x15, 0x30);
                this.nAngle = byMob ? Randomizer.rand(0x0, 0xFF) : Randomizer.rand(0x30, 0x50);
                this.nStartDelay = 540;
                break;
            }
            case 13121054:
            case 13100022:
            case 13120003:
            case 13110022: // 風妖精之箭
                this.nInc = 1;
                this.nStartDelay = 39;
                this.nFirstImpact = byMob ? Randomizer.rand(0x20, 0x30) : Randomizer.rand(0x0F, 0x20);
                this.nSecondImpact = byMob ? Randomizer.rand(3, 4) : Randomizer.rand(0x15, 0x30);
                this.nAngle = byMob ? Randomizer.rand(0x0, 0xFF) : Randomizer.rand(0x30, 0x50);
                break;
            case 65111100: { // 靈魂探求者
                this.nInc = 1;
                this.nStartDelay = 540;
                this.nFirstImpact = byMob ? Randomizer.rand(0x20, 0x30) : Randomizer.rand(0x0F, 0x20);
                this.nSecondImpact = byMob ? Randomizer.rand(3, 4) : Randomizer.rand(0x15, 0x30);
                this.nAngle = byMob ? Randomizer.rand(0x0, 0xFF) : Randomizer.rand(0x30, 0x50);
                break;
            }
            case 61110211: // 意志之劍
            case 61101002: // 意志之劍
            case 61120007: { // 進階意志之劍
                this.nFirstImpact = Randomizer.rand(15, 29);
                this.nSecondImpact = Randomizer.rand(5, 6);
                this.nAngle = Randomizer.rand(35, 50);
                this.nInc = (linked != 61120007) ? 4 : 2;
                this.nStartDelay = byMob ? 0 : 540;
                this.nMaxHitCount = 0;
                break;
            }
            case 25100009:
            case 25100010:
            case 25120110: {
                this.nInc = byMob ? (linked != 25100010) ? 5 : 4 : 1;
                if (linked == 25120110) {
                    this.nInc = this.nInc + 1;
                }
                this.nStartDelay = byMob ? 0 : 630;
                this.nMaxHitCount = 0;
                break;
            }
            case 24100003:
            case 24120002: {
                this.nFirstImpact = Randomizer.rand(15, 29);
                this.nSecondImpact = Randomizer.rand(5, 6);
                this.nAngle = Randomizer.rand(3, 12);
                this.nInc = (linked != 24120002) ? 1 : 2;
                this.nStartDelay = 0;
                this.nMaxHitCount = 0;
                break;
            }
            case 22141017:
            case 22170070: {
                this.nInc = 1;
                this.nFirstImpact = 42;
                this.nSecondImpact = 3;
                this.nAngle = 321;
                this.nStartDelay = 0;
                this.ptStart = new Point(player.getPosition());
                this.nMaxHitCount = 0;
                break;
            }
            case 12001020:
            case 12100020:
            case 12110020:
            case 12120006: {
                this.nInc = 4;
                this.nFirstImpact = skillLevel;
                this.nSecondImpact = skillLevel;
                this.nAngle = 90;
                this.nStartDelay = 0;
                this.nMaxHitCount = 8;
                this.nArriveDir = 1;
                this.nArriveRange = 300;
                break;
            }
            case 4211006: {
                this.nInc = 1;
                this.nStartDelay = 0;
                break;
            }
            case 14001027: {
                this.nInc = 1;
                this.nFirstImpact = 1;
                this.nSecondImpact = 5;
                this.nAngle = Randomizer.rand(30, 50);
                this.nStartDelay = 500;
            }
            case 31221014: { // 天使破壞者: 靈魂探求者
                this.nInc = 3;
                this.nFirstImpact = byMob ? Randomizer.rand(0x20, 0x30) : Randomizer.rand(0x0F, 0x20);
                this.nSecondImpact = byMob ? Randomizer.rand(3, 4) : Randomizer.rand(0x15, 0x30);
                this.nAngle = byMob ? Randomizer.rand(0x0, 0xFF) : Randomizer.rand(0x30, 0x50);
                break;
            }
            default:
                System.out.println("[ForceAtom] 尚未處理的技能 ID:" + skillId + "連結技能:" + linked);
                break;
        }
        this.dwCreateTime = createTime;
        return this;
    }

    public int getKey() {
        return this.nCount;
    }

    public int getNum() {
        return nNum;
    }

    public void setNum(int nNum) {
        this.nNum = nNum;
    }

    public void reset() {
        this.nNum = 0;
    }

    public boolean isValid() {
        return !(FileTime.GetSystemTime().dwLowDateTime - this.dwCreateTime > 5 * 1000);
    }

    public static boolean isAtomSkill(int skillId) {
        int linked = GameConstants.getLinkedAttackSkill(skillId);
        switch (linked) {
            case 31000004:
            case 31001006:
            case 31001007:
            case 30010166:
            case 30011167:
            case 30011168:
            case 30011169:
            case 30011170:
            case 31110008:
            case 31221001:
            case 13121054:
            case 13100022:
            case 13120003:
            case 13110022:
            case 65111100:
            case 61110211: // 意志之劍
            case 61101002: // 意志之劍
            case 61120007:
            case 25100009:
            case 25100010:
            case 25120110:
            case 24100003:
            case 24120002:
            case 22141017:
            case 22170070:
            case 12001020:
            case 12100020:
            case 12110020:
            case 12120006:
            case 4211006:
            case 14001027:
            case 31221014:
                return true;
            default:
                return false;
        }
    }

    public static int getForceatomAttackSkill(int skillId) {
        switch (skillId) {
            case 65111100: {
                return 65111007;
            }
            case 31221001: {
                return 31221014;
            }
            default: {
                return skillId;
            }
        }
    }
}
