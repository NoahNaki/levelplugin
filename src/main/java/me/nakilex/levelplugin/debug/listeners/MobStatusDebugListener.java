package me.nakilex.levelplugin.debug.listeners;

import me.nakilex.levelplugin.debug.MobStatusDebugItem;
import me.nakilex.levelplugin.mob.custom.CustomMobInstance;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.mob.custom.CustomMobStatus;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class MobStatusDebugListener implements Listener {
    private static final int DEFAULT_DURATION_TICKS = 40;

    private final CustomMobManager mobManager;

    public MobStatusDebugListener(CustomMobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        CustomMobStatus status = MobStatusDebugItem.resolveStatus(held).orElse(null);
        if (status == null) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        CustomMobInstance instance = mobManager.getInstance(entity).orElse(null);
        if (instance == null) {
            return;
        }
        boolean applied = switch (status) {
            case STUNNED -> mobManager.stun(entity, DEFAULT_DURATION_TICKS);
            case POISONED -> mobManager.poison(entity, DEFAULT_DURATION_TICKS);
            case TAUNTED -> mobManager.taunt(entity, player, DEFAULT_DURATION_TICKS);
            case FEARED -> mobManager.fear(entity, player, DEFAULT_DURATION_TICKS);
            case SLOWED -> mobManager.slow(entity, DEFAULT_DURATION_TICKS);
        };
        if (applied) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    buildMessage(status, instance.definition().displayName()));
        }
    }

    private String buildMessage(CustomMobStatus status, String displayName) {
        return switch (status) {
            case STUNNED -> "Stunned " + displayName + " for 2 seconds.";
            case POISONED -> "Poisoned " + displayName + " for 2 seconds.";
            case TAUNTED -> "Taunted " + displayName + " for 2 seconds.";
            case FEARED -> "Feared " + displayName + " for 2 seconds.";
            case SLOWED -> "Slowed " + displayName + " for 2 seconds.";
        };
    }
}
