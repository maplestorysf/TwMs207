/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.character.stat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Weber
 */
public class IndieTempStat {

    private final Lock lock;
    private Map<Integer, IndieTempStatElem> mElem;
    private int nSumForCalcDamageStat;
    private boolean bSetSumValue;
    private double dSumForCalcDamageStat;

    public IndieTempStat() {
        this.lock = new ReentrantLock();
        this.mElem = new HashMap<>();
        this.dSumForCalcDamageStat = 1.0;
        this.nSumForCalcDamageStat = 0;
        this.bSetSumValue = false;
    }

    public void encode(MaplePacketLittleEndianWriter mplew) {
        lock.lock();
        try {
            mplew.writeInt(this.mElem.size());
            for (Map.Entry<Integer, IndieTempStatElem> entry : this.mElem.entrySet()) {
                IndieTempStatElem its = entry.getValue();
                mplew.writeInt(its.isSkill ? its.getKey() : -its.getKey());
                mplew.writeInt(its.getValue());
                mplew.writeInt(its.getStart()); // start
                mplew.writeInt(its.getStart()); // start
                mplew.writeInt(its.getTerm()); //
                mplew.writeInt(its.getElem().size());
                for (Map.Entry<Integer, Integer> sub : its.getElem().entrySet()) {
                    mplew.writeInt(sub.getKey());
                    mplew.writeInt(sub.getValue());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public long getRemainTime(int nSkillID) {
        return this.getRemainTime(nSkillID, System.currentTimeMillis());
    }

    public long getRemainTime(int nSkillID, long tCur) {
        lock.lock();
        try {
            if (this.mElem.containsKey(nSkillID)) {
                IndieTempStatElem elm = this.mElem.get(nSkillID);
                return elm.getTerm() + elm.getStart() - tCur;
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }

    public int getValue(int nSkillID) {
        lock.lock();
        try {
            if (this.mElem.containsKey(nSkillID)) {
                IndieTempStatElem elm = this.mElem.get(nSkillID);
                return elm.nValue;
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }

    public int getValueSum() {
        lock.lock();
        try {
            int nValueSum = 0;
            for (IndieTempStatElem tse : this.mElem.values()) {
                nValueSum += tse.nValue;
            }
            return nValueSum;
        } finally {
            lock.unlock();
        }
    }

    public Map<Integer, IndieTempStatElem> getMElem() {
        lock.lock();
        try {
            return this.mElem;
        } finally {
            lock.unlock();
        }
    }
}
