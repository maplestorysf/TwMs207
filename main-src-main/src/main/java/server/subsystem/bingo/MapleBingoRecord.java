/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.subsystem.bingo;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static server.subsystem.bingo.MapleBingoSystem.BINGO_REWARD_SIZE;
import static server.subsystem.bingo.MapleBingoSystem.BINGO_TABLE_SIZE;
import tools.DateUtil;
import tools.data.MaplePacketLittleEndianWriter;

/**
 *
 * @author Weber
 */
public class MapleBingoRecord {

    private final int[] answers;
    private final int[] rewardsTaken;
    private long startTime = -1;
    private long endTime = -1;

    public MapleBingoRecord() {
        this.answers = new int[25];
        this.rewardsTaken = new int[12];
        for (int i = 0; i < MapleBingoSystem.BINGO_TABLE_SIZE; i++) {
            this.answers[i] = 0;
        }
        for (int i = 0; i < MapleBingoSystem.BINGO_REWARD_SIZE; i++) {
            this.rewardsTaken[i] = 0;
        }
    }

    public void setAnswer(int position, boolean checked) {
        this.answers[position] = checked ? 1 : 0;
    }

    public void setRedward(int position, boolean isTaken) {
        this.answers[position] = isTaken ? 1 : 0;
    }

    public String getEncodeRewardTaken() {
        return Arrays.stream(this.rewardsTaken).mapToObj(i -> (i > 0 ? "1" : "0")).collect(Collectors.joining(""));
    }

    public String getEncodeAnswers() {
        return Arrays.stream(this.answers).mapToObj(String::valueOf).collect(Collectors.joining(""));
    }

    public void encode(MaplePacketLittleEndianWriter mplew) {
        mplew.writeMapleAsciiString(this.getEncodeAnswers());
        MapleBingoSystem bingoSystm = MapleBingoSystem.getInstance();
        ;

        mplew.writeMapleAsciiString(bingoSystm.getEncodeBingoTable());
        List<Integer> rewards = bingoSystm.getRewards();
        for (int i = 0; i < MapleBingoSystem.BINGO_REWARD_SIZE; i++) {
            mplew.writeInt(rewards.get(i));
        }
        mplew.writeMapleAsciiString(this.getEncodeRewardTaken());
        mplew.writeMapleAsciiString(DateUtil.getBingoTimeString(startTime));
        mplew.writeMapleAsciiString(DateUtil.getBingoTimeString(startTime));
    }

    public void load(String answersStr, String rewardTakenStr, long startTime, long endTime) {
        int[] dbAnswers = answersStr.codePoints().map(Character::getNumericValue).toArray();
        int[] dbrewardsTaken = rewardTakenStr.codePoints().map(Character::getNumericValue).toArray();
        for (int i = 0; i < Math.max(dbAnswers.length, BINGO_TABLE_SIZE); ++i) {
            this.setAnswer(i, dbAnswers[i] > 0);
        }
        for (int i = 0; i < Math.max(dbrewardsTaken.length, BINGO_REWARD_SIZE); ++i) {
            this.setRedward(i, dbrewardsTaken[i] > 0);
        }
        this.setStartTime(startTime);
        this.setEndTime(endTime);
    }

    void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

}
