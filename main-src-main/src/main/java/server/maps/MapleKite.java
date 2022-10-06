package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import java.awt.Point;
import tools.packet.CField;

/**
 *
 * @author 寒霜天地
 */
public class MapleKite extends MapleMapObject {

    private final int itemId;
    private final MapleCharacter owner;
    private final String message;
    private final Point position;

    public MapleKite(int itemId, MapleCharacter owner, String message, Point position) {
        this.itemId = itemId;
        this.owner = owner;
        this.message = message;
        this.position = position;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.KITE;
    }

    @Override
    public void sendSpawnData(final MapleClient c) {
        c.getSession().write(CField.spawnKite(getObjectId(), itemId, message, owner.getName(), position));
    }

    public byte[] getSpawnKitePacket() {
        return CField.spawnKite(getObjectId(), itemId, message, owner.getName(), position);
    }

    @Override
    public void sendDestroyData(final MapleClient c) {
        c.getSession().write(CField.destroyKite(getObjectId(), false));
    }

    public byte[] getDestroyKitePacket() {
        return CField.destroyKite(getObjectId(), false);
    }
}
