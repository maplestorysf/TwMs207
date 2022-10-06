
/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.maps;

import client.MapleClient;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import tools.packet.CField;

public class MapleRune extends MapleMapObject {

    private final int type;
    private MapleMap map;

    public MapleRune(final int type, final Point pos, final MapleMap map) {
        this.type = type;
        this.map = map;
        this.setPosition(pos);
    }

    public final void setMap(final MapleMap map) {
        this.map = map;
    }

    public final MapleMap getMap() {
        return map;
    }

    public final int getRuneType() {
        return type;
    }

    @Override
    public final void sendSpawnData(final MapleClient client) {
        List<MapleRune> sRune = new ArrayList<>();
        sRune.add(this);
        client.getSession().writeAndFlush(CField.RunePacket.sRuneStone_ClearAndAllRegister(sRune));
    }

    @Override
    public final void sendDestroyData(final MapleClient client) {
        client.getSession().writeAndFlush(CField.RunePacket.sRuneStone_Disappear(client.getPlayer()));
        if (map != null) {
            map.setLastSpawnRune();
        }
    }

    @Override
    public final MapleMapObjectType getType() {
        return MapleMapObjectType.RUNE;
    }
}
