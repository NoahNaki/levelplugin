package me.nakilex.levelplugin.horse.traits;

import me.nakilex.levelplugin.horse.data.HorseData;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Listens for player interactions to trigger horse traits.
 */
public class TraitActivationListener implements Listener {

    private final HorseManager horseManager;

    public TraitActivationListener(HorseManager horseManager) {
        this.horseManager = horseManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isInsideVehicle() || !(player.getVehicle() instanceof AbstractHorse)) return;

        UUID uuid = player.getUniqueId();
        HorseData data = horseManager.getHorse(uuid);
        if (data == null) return;

        String traitId = data.getTraitId();
        if (traitId == null) return;

        HorseTrait trait = TraitRegistry.get(traitId);
        if (trait == null) return;

        CooldownManager cd = CooldownManager.getInstance();
        String key = "horse_trait_" + traitId;
        if (cd.isOnCooldown(uuid, key)) {
            long remain = cd.getRemainingTime(uuid, key) / 1000L;
            send(player, MessageType.WARNING, "Trait on cooldown for " + remain + "s.");
            return;
        }

        trait.apply(player, (AbstractHorse) player.getVehicle());
        cd.setCooldown(uuid, key, trait.getCooldownSeconds());
        send(player, MessageType.SUCCESS, "Trait activated: " + traitId);
    }
}
