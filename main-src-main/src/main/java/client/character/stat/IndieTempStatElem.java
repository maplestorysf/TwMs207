/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.character.stat;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Weber
 */
public class IndieTempStatElem {

    int nValue;
    int nKey;
    int tStart;
    int tTerm;
    boolean isSkill;
    final Map<Integer, Integer> mValue;

    public IndieTempStatElem() {
        this.mValue = new HashMap<>();
        this.nValue = 0;
        this.nKey = 0;
        this.tTerm = 0;
        this.tStart = 0;
    }

    public int getValue() {
        return nValue;
    }

    public void setnValue(int nValue) {
        this.nValue = nValue;
    }

    public int getStart() {
        return tStart;
    }

    public void settStart(int tStart) {
        this.tStart = tStart;
    }

    public int getTerm() {
        return tTerm;
    }

    public void settTerm(int tTerm) {
        this.tTerm = tTerm;
    }

    public Map<Integer, Integer> getElem() {
        return mValue;
    }

    public int getKey() {
        return nKey;
    }

    public void setnKey(int nKey) {
        this.nKey = nKey;
    }

}
