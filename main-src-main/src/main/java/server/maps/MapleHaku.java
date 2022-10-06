/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import java.awt.Point;
import java.util.List;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.StaticLifeMovement;
import tools.packet.CField;

/**
 *
 * @author Itzik
 */
public class MapleHaku extends AnimatedMapleMapObject {

    private final int owner;
    private final int jobid;
    private int weapon;
    private boolean figureHaku;
    private Point pos = new Point(0, 0);

    public MapleHaku(MapleCharacter owner) {
        this.owner = owner.getId();
        jobid = owner.getJob();
        figureHaku = false;

        MapleInventory equipped = owner.getInventory(MapleInventoryType.EQUIPPED);
        if (equipped == null || equipped.getItem((short) -5200) == null) {
            weapon = 0;
        } else {
            weapon = ((Item) equipped.getItem((short) -5200)).getItemId();
        }

        if (!MapleJob.is陰陽師(jobid)) {
            throw new RuntimeException("花狐被不是陰陽師職業的角色創建");
        }
        setPosition(owner.getTruePosition());
        setStance(owner.getStance());
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.getSession().writeAndFlush(CField.spawnHaku(this));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().writeAndFlush(CField.removeDragon(this.owner));
    }

    public int getOwner() {
        return this.owner;
    }

    public int getJobId() {
        return this.jobid;
    }

    public void setWeapon(int id) {
        weapon = id;
    }

    public int getWeapon() {
        return weapon;
    }

    public void setFigureHaku(boolean stats) {
        this.figureHaku = stats;
    }

    public boolean isFigureHaku() {
        return figureHaku;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.HAKU;
    }

    public final Point getPos() {
        return this.pos;
    }

    public final void setPos(Point pos) {
        this.pos = pos;
    }

    public final void updatePosition(List<LifeMovementFragment> movement) {
        for (LifeMovementFragment move : movement) {
            if ((move instanceof LifeMovement)) {
                if ((move instanceof StaticLifeMovement)) {
                    setPos(((LifeMovement) move).getPosition());
                }
                setStance(((LifeMovement) move).getNewstate());
                setFh(((LifeMovement) move).getNewFh());
            }
        }
    }
}
