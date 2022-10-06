package server.maps;

import java.awt.Point;
import java.util.concurrent.ScheduledFuture;

public class ObtacleAtom {

    private Point Position;
    private int type, uniqueID, maxSpeed, acceleration, unk, explodeSpeed, damagePercent, spawnDelay, distance, angle;
    private ScheduledFuture<?> schedule = null, poisonSchedule = null;

    public ObtacleAtom(Point Position) {
        this.Position = Position;
        this.type = 1;
        this.uniqueID = 1;
        this.maxSpeed = 25;
        this.acceleration = 3;
        this.unk = 0;
        this.explodeSpeed = 25;
        this.damagePercent = -5;
        this.spawnDelay = 1000;
        this.distance = 800;
        this.angle = 80;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setUniqueID(int uniqueID) {
        this.uniqueID = uniqueID;
    }

    public int getUniqueID() {
        return uniqueID;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public void setAcceleration(int acceleration) {
        this.acceleration = acceleration;
    }

    public int getAcceleration() {
        return acceleration;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }

    public int getUnk() {
        return unk;
    }

    public void setExplodeSpeed(int explodeSpeed) {
        this.explodeSpeed = explodeSpeed;
    }

    public int getExplodeSpeed() {
        return explodeSpeed;
    }

    public void setDamagePercent(int damagePercent) {
        this.damagePercent = damagePercent;
    }

    public int getDamagePercent() {
        return damagePercent;
    }

    public void setSpawnDelay(int spawnDelay) {
        this.spawnDelay = spawnDelay;
    }

    public int getSpawnDelay() {
        return spawnDelay;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getAngle() {
        return angle;
    }

    public void setPosition(Point position) {
        this.Position = position;
    }

    public Point getPosition() {
        return Position;
    }

    public void setSchedule(ScheduledFuture<?> s) {
        this.schedule = s;
    }

    public ScheduledFuture<?> getSchedule() {
        return schedule;
    }

    public void setPoisonSchedule(ScheduledFuture<?> s) {
        this.poisonSchedule = s;
    }

    public ScheduledFuture<?> getPoisonSchedule() {
        return poisonSchedule;
    }
}
