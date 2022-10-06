/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.subsystem.bingo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapleBingoSystem {

    public static final int BINGO_TABLE_SIZE = 5 * 5;
    public static final int BINGO_REWARD_SIZE = 12;

    public static MapleBingoSystem instance;

    public static final MapleBingoSystem getInstance() {
        if (instance == null) {
            instance = new MapleBingoSystem();
        }
        return instance;
    }

    private List<Integer> rewards;

    private int[] bingoTable;

    public MapleBingoSystem() {
        this.bingoTable = new int[BINGO_TABLE_SIZE];
        this.rewards = new ArrayList<>();
        for (int i = 0; i < BINGO_TABLE_SIZE; i++) {
            this.bingoTable[i] = i + 1;
        }
        this.loadConfig();
    }

    public final void loadConfig() {
        // TODO: 從DB之類的讀取目前賓果遊戲的設定
        // 讀表、獎勵
        this.rewards.clear();
        for (int i = 0; i < BINGO_REWARD_SIZE; i++) {
            this.rewards.add(2432934);
        }
    }

    public String getEncodeBingoTable() {
        return Arrays.stream(this.bingoTable).mapToObj(i -> Character.toString((char) (i + 0x40)))
                .collect(Collectors.joining(""));
    }

    public List<Integer> getRewards() {
        return this.rewards;
    }

}
