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
package server.life;

public class MonsterGlobalDropEntry {

    public MonsterGlobalDropEntry(int itemId, int chance, int continent, byte dropType, int Minimum, int Maximum,
            int questid, int elite, int channelType, String addFrom) {
        this.itemId = itemId;
        this.chance = chance;
        this.dropType = dropType;
        this.continent = continent;
        this.questid = questid;
        this.Minimum = Minimum;
        this.Maximum = Maximum;
        this.elite = elite;
        this.channelType = channelType;
        this.addFrom = addFrom;
    }

    public MonsterGlobalDropEntry(int itemId, int chance, int continent, byte dropType, int Minimum, int Maximum,
            int questid, int elite, int channelType, String addFrom, boolean onlySelf) {
        this.itemId = itemId;
        this.chance = chance;
        this.dropType = dropType;
        this.continent = continent;
        this.questid = questid;
        this.Minimum = Minimum;
        this.Maximum = Maximum;
        this.elite = elite;
        this.channelType = channelType;
        this.addFrom = addFrom;
        this.onlySelf = onlySelf;
    }

    public byte dropType;
    public int itemId, chance, Minimum, Maximum, continent, questid, elite, channelType;
    public boolean onlySelf = false;
    public String addFrom;
}
