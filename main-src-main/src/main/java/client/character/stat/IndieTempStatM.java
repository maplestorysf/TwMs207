/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.character.stat;

import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Weber
 */
public class IndieTempStatM extends IndieTempStatBase {

    public IndieTempStatM(MapleBuffStat buffstat, int nOption, int rOption, int usExpireTerm) {
        super(nOption, rOption, System.currentTimeMillis(), usExpireTerm);
        this.buff = buffstat;
    }

    public void Encode(MaplePacketLittleEndianWriter mplew) {
        this.lock.lock();
        try {
            mplew.writeInt(this.rOption);
            mplew.writeInt(this.nOption);
            mplew.writeInt(this.tLastUpdated);
            mplew.writeInt(this.tLastUpdated);
            mplew.writeInt(this.GetExpireTerm() / 1000);
            mplew.writeInt(0);
            int i = 0;
            while (i < 0) {
                i = i + 1;
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        } finally {
            this.lock.unlock();
        }

    }
}
