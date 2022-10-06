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
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.character.stat.MapleDisease;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.PetCommand;
import constants.FeaturesConfig;
import constants.GameConstants;
import java.awt.Point;
import java.util.List;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.maps.FieldLimitType;
import server.maps.MapleMapItem;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.data.LittleEndianAccessor;
import tools.packet.CField.EffectPacket;
import tools.packet.CWvsContext;
import tools.packet.PetPacket;

public class PetHandler {

    public static final void SpawnPet(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        chr.updateTick(slea.readInt());
        chr.spawnPet(slea.readByte(), slea.readByte() > 0);
    }

    public static final void Pet_AutoPotion(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        /*
         * 1E 02 00 Skip F5 2A 0A 00 666357 Tick 02 00 2 slot 85 84 1E 00 2000005 ItemID
         */
        slea.skip(1);
        chr.updateTick(slea.readInt());
        final short slot = slea.readShort();
        if (chr == null || !chr.isAlive() || chr.getMapId() == 749040100 || chr.getMap() == null
                || chr.hasDisease(MapleDisease.無法使用藥水)) {
            return;
        }
        final Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        int itemID = slea.readInt();
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemID) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        final long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
            chr.dropMessage(5, "冷卻時間尚未結束.");
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit())) { // cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }
        } else {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
        }
    }

    public static final void PetChat(final int petid, final short command, final String text, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getSummonedPet(petid) == null) {
            return;
        }
        chr.getMap().broadcastMessage(chr, PetPacket.petChat(chr.getId(), command, text, (byte) petid), true);
    }

    public static final void PetCommand(final MaplePet pet, final PetCommand petCommand, final MapleClient c,
            final MapleCharacter chr) {

        if (petCommand == null) {
            return;
        }
        byte petIndex = chr.getPetIndex(pet);
        boolean success = false;
        if (Randomizer.nextInt(99) <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness()
                        + (petCommand.getIncrease() * c.getChannelServer().getTraitRate());
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().writeAndFlush(EffectPacket.showPetLevelUp(petIndex));
                    chr.getMap().broadcastMessage(EffectPacket.showPetLevelUp(chr, petIndex));
                }
                c.getSession().writeAndFlush(PetPacket.updatePet(
                        chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            }
        }
        chr.getMap().broadcastMessage(
                PetPacket.commandResponse(chr.getId(), (byte) petCommand.getSkillId(), petIndex, success, false));
    }

    public static final void PetAutoFood(final LittleEndianAccessor slea, final MapleClient c,
            final MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        final byte index = (byte) slea.readInt();
        MaplePet pet = chr.getSummonedPet(index);
        if (pet == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }

        c.getPlayer().updateTick(slea.readInt());

        short slot = slea.readShort();
        final int itemId = slea.readInt();
        Item petFood = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (petFood == null || petFood.getItemId() != itemId || petFood.getQuantity() <= 0 || itemId / 10000 != 212) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        boolean gainCloseness = false;

        if (Randomizer.nextInt(99) <= 50) {
            gainCloseness = true;
        }
        if (pet.getFullness() < 100) {
            int newFullness = pet.getFullness() + 30;
            if (newFullness > 100) {
                newFullness = 100;
            }
            pet.setFullness(newFullness);

            if (gainCloseness && pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + 1;
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().writeAndFlush(EffectPacket.showPetLevelUp(index));
                    chr.getMap().broadcastMessage(EffectPacket.showPetLevelUp(chr, index));
                }
            }
            c.getSession().writeAndFlush(PetPacket
                    .updatePet(chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            chr.getMap().broadcastMessage(c.getPlayer(),
                    PetPacket.commandResponse(chr.getId(), (byte) 1, index, true, true), true);
        } else {
            if (gainCloseness) {
                int newCloseness = pet.getCloseness() - 1;
                if (newCloseness < 0) {
                    newCloseness = 0;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness < GameConstants.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() - 1);
                }
            }
            c.getSession().writeAndFlush(PetPacket
                    .updatePet(chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            chr.getMap().broadcastMessage(chr,
                    PetPacket.commandResponse(chr.getId(), (byte) 1, chr.getPetIndex(pet), false, true), true);
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, true, false);
        c.getSession().writeAndFlush(CWvsContext.enableActions());
    }

    public static final void PetFood(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        int previousFullness = 100;
        MaplePet pet = null;
        if (chr == null) {
            return;
        }
        for (final MaplePet pets : chr.getSummonedPets()) {
            if (pets.getFullness() < previousFullness) {
                previousFullness = pets.getFullness();
                pet = pets;
            }
        }
        if (pet == null) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }

        c.getPlayer().updateTick(slea.readInt());
        short slot = slea.readShort();
        final int itemId = slea.readInt();
        Item petFood = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (petFood == null || petFood.getItemId() != itemId || petFood.getQuantity() <= 0 || itemId / 10000 != 212) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        boolean gainCloseness = false;

        if (Randomizer.nextInt(99) <= 50) {
            gainCloseness = true;
        }
        if (pet.getFullness() < 100) {
            int newFullness = pet.getFullness() + 30;
            if (newFullness > 100) {
                newFullness = 100;
            }
            pet.setFullness(newFullness);
            final byte index = chr.getPetIndex(pet);

            if (gainCloseness && pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + 1;
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().writeAndFlush(EffectPacket.showPetLevelUp(index));
                    chr.getMap().broadcastMessage(EffectPacket.showPetLevelUp(chr, index));
                }
            }
            c.getSession().writeAndFlush(PetPacket
                    .updatePet(chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            chr.getMap().broadcastMessage(c.getPlayer(),
                    PetPacket.commandResponse(chr.getId(), (byte) 1, index, true, true), true);
        } else {
            if (gainCloseness) {
                int newCloseness = pet.getCloseness() - 1;
                if (newCloseness < 0) {
                    newCloseness = 0;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness < GameConstants.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() - 1);
                }
            }
            c.getSession().writeAndFlush(PetPacket
                    .updatePet(chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            chr.getMap().broadcastMessage(chr,
                    PetPacket.commandResponse(chr.getId(), (byte) 1, chr.getPetIndex(pet), false, true), true);
        }
        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, true, false);
        c.getSession().writeAndFlush(CWvsContext.enableActions());
    }

    public static final void MovePet(final LittleEndianAccessor slea, final MapleCharacter chr) {
        if (chr == null) {
            return;
        }

        final int petIndex = (int) slea.readInt();
        byte unk = slea.readByte();
        int duration = slea.readInt();
        Point mPos = slea.readPos();
        Point oPos = slea.readPos();
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3, chr);
        if (res != null && !res.isEmpty() && chr.getMap() != null) { // map crash hack
            if (slea.available() != 0) {
                System.out.println("slea.available != 0 (寵物移動出錯) 剩餘封包長度: " + slea.available());
                FileoutputUtil.log(FileoutputUtil.Movement_Log,
                        "slea.available != 0 (寵物移動出錯) 封包: " + slea.toString(true));
                return;
            }
            final MaplePet pet = chr.getSummonedPet(petIndex);
            if (pet != null) {
                mPos = pet.getPos();
                pet.updatePosition(res);
                chr.getMap().broadcastMessage(PetPacket.movePet(chr.getId(), petIndex, duration, mPos, oPos, res));
            } else {
                if (chr.isShowErr()) {
                    chr.showInfo("寵物移動", true, "寵物移動出錯,寵物為空,petIndex:" + petIndex);
                }
            }
        }
    }

    public static void AllowPetLoot(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            c.getSession().writeAndFlush(CWvsContext.enableActions());
            return;
        }
        slea.skip(4);
        int data = slea.readShort();
        if (data > 0) {
            chr.getQuestNAdd(MapleQuest.getInstance(122902)).setCustomData(String.valueOf(data));
        } else {
            chr.getQuestRemove(MapleQuest.getInstance(122902));
        }
        for (final MaplePet pet : chr.getSummonedPets()) {
            pet.setCanPickup(data > 0);
            chr.getClient().getSession().writeAndFlush(PetPacket.updatePet(
                    chr.getInventory(MapleInventoryType.CASH).getItem((short) (byte) pet.getInventoryPosition())));
        }
        c.getSession().writeAndFlush(PetPacket.showPetPickUpMsg(data > 0, 1));
    }
}
