/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.character.stat;

/**
 * TwoStateTemporaryStat
 *
 * @author Eric
 */
public class TwoStateTemporaryStat extends TemporaryStatBase {

    public TwoStateTemporaryStat(boolean bDynamicTermSet) {
        super(bDynamicTermSet);
    }

    @Override
    public int getMaxValue() {
        return 0;
    }

    @Override
    public boolean isActivated() {
        lock.lock();
        try {
            return nOption != 0;
        } finally {
            lock.unlock();
        }
    }
}
