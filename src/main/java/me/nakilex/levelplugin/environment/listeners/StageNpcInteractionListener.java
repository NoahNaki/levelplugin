package me.nakilex.levelplugin.environment.listeners;

import me.nakilex.levelplugin.environment.stage.StageNpc;
import me.nakilex.levelplugin.npc.listeners.NPCClickListener;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Bridges MythicMob-based staged NPCs with the regular Citizens interaction pipeline by detecting
 * the metadata stored on spawned entities and delegating to {@link NPCClickListener}.
 */
public class StageNpcInteractionListener implements Listener {

    private final NPCClickListener npcClickListener;

    public StageNpcInteractionListener(NPCClickListener npcClickListener) {
        this.npcClickListener = npcClickListener;
    }

    private boolean handle(Player player, Entity entity) {
        if (player == null || entity == null) return false;
        Integer interactionId = StageNpc.resolveInteractionId(entity);
        if (interactionId == null) return false;

        NPC npc = CitizensAPI.getNPCRegistry().getById(interactionId);
        if (npc == null) {
            return false;
        }

        npcClickListener.handleInteraction(player, npc);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (handle(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (handle(event.getPlayer(), event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}

