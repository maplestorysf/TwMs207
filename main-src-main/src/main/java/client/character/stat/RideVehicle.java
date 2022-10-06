package client.character.stat;

import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
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
public class RideVehicle extends TemporaryStatBase {

    public RideVehicle() {
        super(false);
    }

    @Override
    public void encode(MaplePacketLittleEndianWriter mplew) {
        lock.lock();
        try {
            // this.rOption = chr.getBuffSource(MapleBuffStat.RideVehicle);
            //
            // Item b_mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short)
            // -118);
            // Item c_mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short)
            // -18);
            //
            // if ((GameConstants.getMountItem(this.rOption, chr) == 0) && (b_mount != null)
            // && (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -119) !=
            // null)) {
            // this.nOption = b_mount.getItemId();
            // } else if ((GameConstants.getMountItem(this.rOption, chr) == 0) && (c_mount
            // != null)
            // && (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -19) !=
            // null)) {
            // this.nOption = c_mount.getItemId();
            // } else {
            // this.nOption = GameConstants.getMountItem(this.rOption, chr);
            // }
            // if (this.rOption < 0) {
            // this.rOption = 0;
            // }
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
