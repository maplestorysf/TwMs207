/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.commands;

/**
 *
 * @author Pungin
 */
public enum PlayerGMRank {

    NORMAL(0),
    INTERN(1),
    GM(2),
    SUPERGM(3),
    ADMIN(4);

    private final int level;

    private PlayerGMRank(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static PlayerGMRank getByLevel(int level) {
        for (PlayerGMRank i : PlayerGMRank.values()) {
            if (i.getLevel() == level) {
                return i;
            }
        }
        return PlayerGMRank.NORMAL;
    }
}
