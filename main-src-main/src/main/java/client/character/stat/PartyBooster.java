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
import tools.data.MaplePacketLittleEndianWriter;

/**
 * TemporaryStat_PartyBooster
 *
 * @author Eric
 */
public class PartyBooster extends TemporaryStatBase {

    public int tCurrentTime;

    public PartyBooster() {
        super(false);
        this.tCurrentTime = 0;
        this.usExpireTerm = 0;
    }

    @Override
    public void encode(MaplePacketLittleEndianWriter mplew) {
        lock.lock();
        try {
            super.encode(mplew);
            mplew.write(this.nOption == 0 ? 0 : 1);
            mplew.writeInt(tCurrentTime);
            mplew.writeShort(usExpireTerm);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getExpireTerm() {
        return 1000 * usExpireTerm;
    }

    @Override
    public boolean isExpiredAt(long tCur) {
        lock.lock();
        try {
            return getExpireTerm() < tCur - tCurrentTime;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        super.reset();
        tCurrentTime = 0;
    }
}
