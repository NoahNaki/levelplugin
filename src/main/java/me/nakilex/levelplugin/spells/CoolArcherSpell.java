package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.persistence.PersistentDataType;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Simple listener that maps basic interactions to MythicMobs skills for the
 * CoolArcher test class.
 */
public class CoolArcherSpell implements Listener {

    // Archer bows use sword items as the base to apply Nexo models
    private static final Set<Material> VALID_WEAPONS = EnumSet.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD);

    private boolean hasEgoArcher(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) return false;
        String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
        return id != null && id.startsWith("archer");
    }

    private boolean validWeapon(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && VALID_WEAPONS.contains(item.getType());
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!hasEgoArcher(player) || !validWeapon(player)) return;

        Main.getPlugin().getLogger().info("[CA] left click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "RRR"); // Deadly Javelin
        } else {
            castSpell(player, "BASIC_ATTACK"); // Quick Shot
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!hasEgoArcher(player) || !validWeapon(player)) return;
        event.setCancelled(true);

        Main.getPlugin().getLogger().info("[CA] right click " + player.getName() + " sneaking=" + player.isSneaking());

        if (player.isSneaking()) {
            castSpell(player, "LLL"); // Dragon Piercer
        } else {
            castSpell(player, "LRL"); // Backstep
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!hasEgoArcher(player) || !validWeapon(player)) return;
        Main.getPlugin().getLogger().info("[CA] toggle sneak by " + player.getName());
        castSpell(player, "LLR"); // Arrow Barrage
    }

    private void castSpell(Player player, String combo) {
        Spell spell = SpellManager.getInstance().getSpell("coolarcher", combo);
        Main.getPlugin().getLogger().info("[CA] castSpell combo=" + combo + " spell=" + (spell!=null));
        if (spell == null) {
            MythicBukkit.inst().getAPIHelper().castSkill(player, combo);
            return;
        }

        // weapon check
        if (!spell.getAllowedWeapons().contains(player.getInventory().getItemInMainHand().getType())) {
            player.sendMessage("§cYou must hold a valid coolarcher weapon!");
            return;
        }
        int level = StatsManager.getInstance().getLevel(player);
        if (level < spell.getLevelReq()) {
            player.sendMessage("§cYou are not high enough level for " + spell.getDisplayName());
            return;
        }

        spell.castEffect(player);
        StatsManager.getInstance().recalcDerivedStats(player);
    }
}
