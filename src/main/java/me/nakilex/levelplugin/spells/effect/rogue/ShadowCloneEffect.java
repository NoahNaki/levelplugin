package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import me.nakilex.levelplugin.utils.MetadataTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class ShadowCloneEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        UUID id = player.getUniqueId();

        NPC existing = null;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.hasTrait(MetadataTrait.class) && npc.getTrait(MetadataTrait.class).getOwner().equals(id)) {
                existing = npc;
                break;
            }
        }
        if (existing != null) {
            Location cloneLoc = existing.getEntity().getLocation();
            existing.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.teleport(cloneLoc);
            player.sendMessage("§aYou swapped places with your shadow clone!");
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            return;
        }

        NPC clone = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "Shadow Clone");
        clone.spawn(player.getLocation());
        clone.getOrAddTrait(MetadataTrait.class).setOwner(id);
        clone.data().setPersistent("player-skin-name", player.getName());
        if (clone.getEntity() instanceof Player p) {
            p.getInventory().setArmorContents(player.getInventory().getArmorContents());
            p.getInventory().setItemInMainHand(new ItemStack(player.getInventory().getItemInMainHand()));
            p.getInventory().setItemInOffHand(new ItemStack(player.getInventory().getItemInOffHand()));
        }
        player.sendMessage("§aYou created a shadow clone!");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!clone.isSpawned()) return;
                Location loc = clone.getEntity().getLocation();
                clone.despawn();
                clone.destroy();
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                player.sendMessage("§cYour shadow clone exploded!");

                double damage = player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() * 1.5;
                for (org.bukkit.entity.Entity e : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        SpellUtils.dealWithChat(player, le, damage, "Shadow Clone");
                    }
                }
            }
        }.runTaskLater(Main.getInstance(), 100L);
    }
}
