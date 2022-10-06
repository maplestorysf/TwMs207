/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.character.stat;

import java.util.ArrayList;
import java.util.List;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author PacketBakery
 */
public class StopForceAtom {

    public int nIdx, nCount, nWeaponID;
    public List<Integer> aAngleInfo;

    public StopForceAtom() {
        this.nIdx = 0;
        this.nCount = 0;
        this.nWeaponID = 0;
        this.aAngleInfo = new ArrayList<>();
    }

    public void Decode(LittleEndianAccessor slea) {
        this.nIdx = slea.readInt();
        this.nCount = slea.readInt();
        this.nWeaponID = slea.readInt();
        int nSize = slea.readInt();
        for (int i = 0; i < nSize; i++) {
            this.aAngleInfo.add(slea.readInt());
        }
    }

    public void Encode(MaplePacketLittleEndianWriter mplew) {
        mplew.writeInt(this.nIdx);
        mplew.writeInt(this.nCount);
        mplew.writeInt(this.nWeaponID);
        mplew.writeInt(this.aAngleInfo.size());
        for (Integer nAngle : this.aAngleInfo) {
            mplew.writeInt(nAngle);
        }
    }
}
