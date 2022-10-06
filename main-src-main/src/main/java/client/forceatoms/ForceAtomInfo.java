/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.forceatoms;

import client.MapleCharacter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tools.FileTime;

/**
 *
 * @author Weber
 */
public class ForceAtomInfo {

    private List<ForceAtom> atoms;
    private int nKey = 1;
    private final Lock lock;

    public ForceAtomInfo() {
        this.atoms = new LinkedList<>();
        this.lock = new ReentrantLock();
    }

    public void reset() {
        this.atoms.clear();
        this.nKey = 1;
    }

    public ForceAtom getNewAtom(MapleCharacter player, int skillID, boolean byMob) {
        lock.lock();
        try {
            this.nKey++;
            ForceAtom atom = new ForceAtom(player, nKey, skillID, FileTime.GetSystemTime().dwLowDateTime, byMob);
            this.atoms.add(atom);
            return atom;
        } finally {
            lock.unlock();
        }
    }

    public ForceAtom getAtom(int key) {
        lock.lock();
        try {
            for (ForceAtom atom : this.atoms) {
                if (atom.getKey() == key) {
                    return atom;
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void removeExpire() {
        lock.lock();
        try {
            List<Integer> toRemove = new ArrayList<>();
            for (ForceAtom atom : this.atoms) {
                if (!atom.isValid()) {
                    toRemove.add(this.atoms.indexOf(atom));
                }
            }
            toRemove.forEach(i -> this.atoms.remove((int) i));
        } finally {
            lock.unlock();
        }
    }

    public void removeAtom(int key) {
        lock.lock();
        try {
            int toRemove = -1;
            for (ForceAtom atom : this.atoms) {
                if (atom.getKey() == key) {
                    toRemove = this.atoms.indexOf(atom);
                }
            }
            this.atoms.remove(toRemove);
        } finally {
            lock.unlock();
        }
    }

    public void removeAtom(ForceAtom atom) {
        lock.lock();
        try {
            this.atoms.remove(atom);
        } finally {
            lock.unlock();
        }
    }
}
