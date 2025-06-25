package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listener for the Death Knight class. Maps simple inputs to Mythic skills.
 */
public class DeathKnightSpell implements Listener {

    private static final Set<Material> VALID_WEAPONS = EnumSet.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);

    public DeathKnightSpell() {
        // Passive soul barrier stacking when sneaking
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.isSneaking()) continue;
                    ItemStack item = p.getInventory().getItemInMainHand();
                    if (item == null || !item.hasItemMeta()) continue;
                    var pdc = item.getItemMeta().getPersistentDataContainer();
                    if (!pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) continue;
                    String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
                    if (id == null || !id.startsWith("death_knight")) continue;
                    if (!VALID_WEAPONS.contains(item.getType())) continue;
                    castSpell(p, "LLL"); // Soul Barrier stack/skill
                }
            }
        }.runTaskTimer(Main.getInstance(), 10L, 10L);
    }

    private boolean hasEgo(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) return false;
        String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
        return id != null && id.startsWith("death_knight");
    }

    private boolean validWeapon(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && VALID_WEAPONS.contains(item.getType());
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!hasEgo(player) || !validWeapon(player)) return;
        if (player.isSneaking()) {
            castSpell(player, "RRR"); // Death Sentence
        } else {
            castSpell(player, "BASIC_ATTACK"); // Death Strike
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasEgo(player) || !validWeapon(player)) return;
        event.setCancelled(true);
        if (player.isSneaking()) {
            castSpell(player, "LRR"); // Necrotic Whirlwind
        } else {
            castSpell(player, "LRL"); // Phantom Charge
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!hasEgo(player) || !validWeapon(player)) return;
        castSpell(player, "RLL"); // Wraithbound Chains
    }

    private void castSpell(Player player, String combo) {
        Spell spell = SpellManager.getInstance().getSpell("deathknight", combo);
        if (spell == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            return;
        }
        if (!spell.getAllowedWeapons().contains(player.getInventory().getItemInMainHand().getType())) {
            player.sendMessage("§cYou must hold a valid deathknight weapon!");
            return;
        }
        spell.castEffect(player);
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
