/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting;

import client.MapleClient;
import client.inventory.MapleInventoryType;
import javax.script.Invocable;

/**
 *
 * @author Weber
 */
public class ItemScriptManager extends NPCConversationManager {

    private final int itemId;
    private final int slot;

    public ItemScriptManager(MapleClient c, int npc, int itemId, int slot, String npcscript, ScriptType type,
            Invocable iv) {
        super(c, npc, -1, npcscript, type, iv);
        this.slot = slot;
        this.itemId = itemId;
    }

    public void removeSelfItem() {
        this.removeSelfItem((short) 1);
    }

    public void removeSelfItem(short quantity) {
        this.removeFromSlot(itemId / 1000000, (byte) slot, quantity);
    }

}
