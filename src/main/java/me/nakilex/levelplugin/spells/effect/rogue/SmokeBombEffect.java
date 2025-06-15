package me.nakilex.levelplugin.spells.effect.rogue;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.utils.SpellUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SmokeBombEffect implements SpellEffect {
    @Override
    public void apply(SpellCastContext ctx) {
        Player player = ctx.getPlayer();
        World world = player.getWorld();

        // Throw a skull item as the bomb
        ItemStack skull = new ItemStack(Material.WITHER_SKELETON_SKULL);
        Item bomb = world.dropItem(player.getEyeLocation(), skull);
        bomb.setPickupDelay(Integer.MAX_VALUE);
        bomb.setCustomName("smoke_bomb");
        bomb.setCustomNameVisible(false);
        bomb.setGravity(true);
        bomb.setVelocity(player.getLocation().getDirection().multiply(0.9));

        world.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1f);

        new BukkitRunnable() {
            int ticks = 0;
            boolean activated = false;

            @Override
            public void run() {
                if (!bomb.isValid()) { cancel(); return; }

                if (!activated && (bomb.isOnGround() || ticks > 20)) {
                    activated = true;
                    startSmoke(bomb);
                }

                if (activated && ticks >= 80) {
                    cancel();
                }

                ticks++;
            }

            private void startSmoke(Item item) {
                Location loc = item.getLocation();
                new BukkitRunnable() {
                    int life = 0;

                    @Override
                    public void run() {
                        if (life++ >= 80) {
                            item.remove();
                            cancel();
                            return;
                        }
                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, item.getLocation(), 10, 0.5, 0.5, 0.5, 0.01);
                        for (Entity e : world.getNearbyEntities(loc, 3, 2, 3)) {
                            if (e instanceof LivingEntity le && !le.equals(player)) {
                                SpellUtils.dealWithChat(player, le, ctx.getFinalDamage() / 4.0, "Smoke Bomb");
                                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 1, false, false));
                            }
                        }
                    }
                }.runTaskTimer(Main.getInstance(), 0L, 20L);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
