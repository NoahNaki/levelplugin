package me.nakilex.levelplugin.debug.listeners;

import me.nakilex.levelplugin.debug.StunStickDebugItem;
import me.nakilex.levelplugin.mob.custom.CustomMobInstance;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class StunStickDebugListener implements Listener {
    private static final int STUN_DURATION_TICKS = 40;

    private final CustomMobManager mobManager;

    public StunStickDebugListener(CustomMobManager mobManager) {
        this.mobManager = mobManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!StunStickDebugItem.isDebugStick(held)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        CustomMobInstance instance = mobManager.getInstance(entity).orElse(null);
        if (instance == null) {
            return;
        }
        boolean wasStunned = instance.isStunned();
        if (mobManager.stun(entity, STUN_DURATION_TICKS) && !wasStunned) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Stunned " + instance.definition().displayName() + " for 2 seconds.");
        }
    }
}
