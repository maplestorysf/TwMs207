package client.character.stat;

import client.character.stat.TemporaryStatBase;
import client.MapleCharacter;
import tools.data.MaplePacketLittleEndianWriter;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Fate
 */
public class DashSpeed extends TemporaryStatBase {

    public DashSpeed() {
        super(true);
    }

    @Override
    public void encode(MaplePacketLittleEndianWriter mplew) {
        lock.lock();
        try {
            super.encode(mplew);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getExpireTerm() {
        return 1000 * usExpireTerm;
    }
}
