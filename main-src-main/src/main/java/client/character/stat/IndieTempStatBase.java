/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.character.stat;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import handling.BuffStat;

/**
 *
 * @author Weber
 */
public class IndieTempStatBase {

    protected Lock lock;
    protected int rOption;
    protected int nOption;
    protected int usExpireTerm;
    protected BuffStat buff;
    protected int tLastUpdated;

    int nSumForCalcDamageStat;
    boolean bSetSumValue;
    double dSumForCalcDamageStat;

    public int GetExpireTerm() {
        return 1000 * this.usExpireTerm;
    }

    public int GetReason() {
        this.lock.lock();
        try {
            return this.rOption;
        } finally {
            this.lock.unlock();
        }
    }

    public int GetValue() {
        this.lock.lock();
        try {
            return this.nOption;
        } finally {
            this.lock.unlock();
        }
    }

    public IndieTempStatBase(int nOption, int rOption, long tLastUpdated, int usExpireTerm) {
        this.nOption = nOption;
        this.rOption = rOption;
        this.tLastUpdated = (int) (tLastUpdated % 1000000000L);
        this.usExpireTerm = usExpireTerm;
        this.lock = new ReentrantLock();
    }

    public BuffStat getBuff() {
        this.lock.lock();
        try {
            return this.buff;
        } finally {
            this.lock.unlock();
        }
    }

    public long gettLastUpdated() {
        return (long) this.tLastUpdated;
    }
}
