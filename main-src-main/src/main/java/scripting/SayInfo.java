/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting;

import java.util.LinkedList;
import java.util.List;
import tools.Pair;

/**
 *
 * @author pungin
 */
public class SayInfo {

    private int nSpeakerTypeID = 0;
    private int nSpeakerTemplateID = 0;
    private int nAnotherSpeakerTemplateID = 0;
    private short bParam = 0;
    private byte eColor = 0;
    private final List<Pair<String, Integer>> msgs = new LinkedList<>();
    private int msgPos = 0;
    private boolean theStart = true;
    private boolean theEnd = true;

    public int getSpeakerTypeID() {
        return nSpeakerTypeID;
    }

    public void setSpeakerTypeID(int n) {
        nSpeakerTypeID = n;
    }

    public int getSpeakerTemplateID() {
        return nSpeakerTemplateID;
    }

    public void setSpeakerTemplateID(int n) {
        nSpeakerTemplateID = n;
    }

    public int getAnotherSpeakerTemplateID() {
        return nAnotherSpeakerTemplateID;
    }

    public void setAnotherSpeakerTemplateID(int n) {
        nAnotherSpeakerTemplateID = n;
    }

    public int getParam() {
        return bParam;
    }

    public void setParam(short bParam) {
        this.bParam = bParam;
    }

    public int getColor() {
        return eColor;
    }

    public void setColor(byte eColor) {
        this.eColor = eColor;
    }

    public Pair<String, Integer> getLastMsg() {
        if (msgPos > 0) {
            return msgs.get(--msgPos);
        } else {
            return null;
        }
    }

    public Pair<String, Integer> getMsg() {
        if (msgPos <= msgs.size()) {
            return msgs.get(msgPos++);
        } else {
            return null;
        }
    }

    public void addMsg(String msg, int nSpeakerTemplateID) {
        msgs.add(new Pair<>(msg, nSpeakerTemplateID));
    }

    public int available() {
        return msgs.size() - msgPos - 1;
    }

    public boolean isTheStart() {
        return theStart;
    }

    public void setStart(boolean bool) {
        theStart = bool;
    }

    public boolean isTheEnd() {
        return theEnd;
    }

    public void setEnd(boolean bool) {
        theEnd = bool;
    }
}
