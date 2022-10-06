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
 * TemporaryStat_GuidedBullet
 *
 * @author Eric
 */
public class GuidedBullet extends TemporaryStatBase {

    public int dwMobID;
    public int dwUserID;

    public GuidedBullet() {
        super(false);
        this.dwMobID = 0;
        this.dwUserID = 0;
    }

    @Override
    public void encode(MaplePacketLittleEndianWriter mplew) {
        lock.lock();
        try {
            super.encode(mplew);
            // dwMobID = chr.getFirstLinkMid();
            // dwUserID = chr.getId();
            mplew.writeInt(dwMobID);
            mplew.writeInt(dwUserID);
        } finally {
            lock.unlock();
        }
    }

    public int GetMobID() {
        lock.lock();
        try {
            return dwMobID;
        } finally {
            lock.unlock();
        }
    }

    public int GetUserID() {
        lock.lock();
        try {
            return dwUserID;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.dwMobID = 0;
        this.dwUserID = 0;
    }
}
