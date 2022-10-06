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

import client.character.stat.PartyBooster;
import client.character.stat.TemporaryStatBase;
import client.MapleCharacter;
import java.util.Arrays;
import tools.data.MaplePacketLittleEndianWriter;

/**
 * TSIndex Handles the TemporaryStat indexes for a TwoStateTemporaryStat.
 *
 * @author Eric
 */
public abstract class TSIndex_bk {

    private static final TemporaryStatBase[] encoders = {new EnergyCharged(), new DashSpeed(), new DashJump(),
        new RideVehicle(), new PartyBooster(), new GuidedBullet(), new Undead(), new Undead(), // 未知
        new RideVehicleExpire()};

    // public static void encodeAll(MaplePacketLittleEndianWriter mplew,
    // MapleCharacter chr) {
    // Arrays.stream(encoders).forEachOrdered(stat -> stat.EncodeForClient(chr,
    // mplew));
    // }
    //
    // public static void encode(MapleBuffStat nBuff, MaplePacketLittleEndianWriter
    // mplew, MapleCharacter chr) {
    // switch (nBuff) {
    // case EnergyCharged:
    // new EnergyCharged().EncodeForClient(chr, mplew);
    // break;
    // case DashSpeed:
    // new DashSpeed().EncodeForClient(chr, mplew);
    // break;
    // case DashJump:
    // new DashJump().EncodeForClient(chr, mplew);
    // break;
    // case RideVehicle:
    // new RideVehicle().EncodeForClient(chr, mplew);
    // break;
    // case PartyBooster:
    // new PartyBooster().EncodeForClient(chr, mplew);
    // break;
    // case GuidedBullet:
    // new GuidedBullet().EncodeForClient(chr, mplew);
    // break;
    // case Undead:
    // new Undead().EncodeForClient(chr, mplew);
    // break;
    // case RideVehicleExpire:
    // new RideVehicleExpire().EncodeForClient(chr, mplew);
    // break;
    // case COUNT_PLUS1:
    // new RideVehicleExpire().EncodeForClient(chr, mplew);
    // break;
    // }
    // }
}
