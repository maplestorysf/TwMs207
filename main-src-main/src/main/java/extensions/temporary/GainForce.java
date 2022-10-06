/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extensions.temporary;

/**
 *
 * @author User
 */
public class GainForce {

    public enum GainForceType {
        DF(0), CARD(1), BLADES(2), TRIAL_FLAME(3), SOUL(4), AEGIS(5), ROCKET(6), TRIFLING_WHIM(7), STORM(8), UNK9(
                9), QUIVER_KARTRIGE(10), ASSASSIN(
                11), MESO(12), FOX(13), UNK14(14), BAT(15), UNK16(16), FLAMES_TRACK(17), UNK18(18), UNK19(19),;

        private int value;

        private GainForceType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private GainForceType gft;
    private int upRang = 0, downRang = 0, speed = 0, direction = 0, dely = 0;

    public GainForce(GainForceType gft) {
        this.gft = gft;
    }

    public void setUpRang(int upRang) {
        this.upRang = upRang;
    }

    public int getUpRang() {
        return upRang;
    }

    public void setDownRang(int downRang) {
        this.downRang = downRang;
    }

    public int getDownRang() {
        return downRang;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getSpeed() {
        return speed;
    }

    public void setDirection(int driction) {
        this.direction = driction;
    }

    public int getDirection() {
        return direction;
    }

    public void setDely(int dely) {
        this.dely = dely;
    }

    public int getDely() {
        return dely;
    }

}
