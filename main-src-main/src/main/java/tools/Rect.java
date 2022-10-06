/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

/**
 *
 * @author o黯淡o
 */
public class Rect implements java.io.Serializable {
    private int left, top, right, bottom;

    public Rect(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public void setLeft(int value) {
        this.left = value;
    }

    public int getLeft() {
        return left;
    }

    public void setRight(int value) {
        this.right = value;
    }

    public int getRight() {
        return right;
    }

    public void setTop(int value) {
        this.top = value;
    }

    public int getTop() {
        return top;
    }

    public void setBottom(int value) {
        this.bottom = value;
    }

    public int getBottom() {
        return bottom;
    }
}
