/* 
 *     This file is part of Development, a MapleStory Emulator Project. 
 *     Copyright (C) 2015 Eric Smith <muffinman75013@yahoo.com> 
 * 
 *     This program is free software: you can redistribute it and/or modify 
 *     it under the terms of the GNU General Public License as published by 
 *     the Free Software Foundation, either version 3 of the License, or 
 *     (at your option) any later version. 
 * 
 *     This program is distributed in the hope that it will be useful, 
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *     GNU General Public License for more details. 
 * 
 *     You should have received a copy of the GNU General Public License 
 */
package client.character.stat;

import client.MapleCharacter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tools.data.MaplePacketLittleEndianWriter;

/**
 * TemporaryStatBase
 *
 * @author Eric
 */
public class TemporaryStatBase {

    public int yOption;
    public long tLastUpdated;
    public int mOption;
    final public boolean bDynamicTermSet;
    final public Lock lock;
    public int xOption;
    public int rOption;
    public int usExpireTerm;
    public int cOption;
    public int tOption;
    public int bOption;
    public int nOption;
    public int sOption;
    public boolean isSkill;

    public TemporaryStatBase(boolean bDynamicTermSet) {
        this.nOption = 0;
        this.rOption = 0;
        this.tLastUpdated = System.currentTimeMillis();
        this.lock = new ReentrantLock();
        this.bDynamicTermSet = bDynamicTermSet;
    }

    public void encode(MaplePacketLittleEndianWriter mplew) {
        lock.lock();
        try {
            mplew.writeInt(this.nOption);
            mplew.writeInt(this.rOption);
            mplew.write(this.bOption);
            mplew.writeInt((int) tLastUpdated);
            if (bDynamicTermSet) {
                mplew.writeShort(usExpireTerm);
            }
        } finally {
            lock.unlock();
        }
    }

    public int getExpireTerm() {
        if (bDynamicTermSet) {
            return 1000 * usExpireTerm;
        }
        return Integer.MAX_VALUE;
    }

    public int getMaxValue() {
        return 10000;
    }

    public boolean isActivated() {
        lock.lock();
        try {
            return nOption >= 10000;
        } finally {
            lock.unlock();
        }
    }

    public boolean isExpiredAt(long tCur) {
        lock.lock();
        try {
            if (bDynamicTermSet) {
                return getExpireTerm() > tCur - tLastUpdated;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public int getReason() {
        lock.lock();
        try {
            return isSkill ? rOption : -rOption;
        } finally {
            lock.unlock();
        }
    }

    public int getValue() {
        lock.lock();
        try {
            return nOption;
        } finally {
            lock.unlock();
        }
    }

    public void reset() {
        this.nOption = 0;
        this.rOption = 0;
        this.tLastUpdated = 0;
        this.sOption = 0;
        this.bOption = 0;
        this.yOption = 0;
    }
}
