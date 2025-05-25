package me.nakilex.levelplugin.spells.listener;

import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Placeholder for any future Basic Mage Attack event handling.
 * BasicMageEffect is purely immediate, so no listener logic is required.
 */
public class BasicMageListener implements Listener {
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        // 1) check class
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(p.getUniqueId());
        if (stats.playerClass != PlayerClass.MAGE) return;

        // 2) check wand in main hand
        Material mat = p.getInventory().getItemInMainHand().getType();
        if (!WeaponType.WAND.getMaterials().contains(mat)) return;

        // 3) fetch your “basic” Spell by combo key "B"
        Spell basic = SpellManager.getInstance().getSpell("mage", "B");
        if (basic == null) return;

        // 4) let the Spell handle cooldown, mana, runes and then fire your effect
        basic.castEffect(p);
        StatsManager.getInstance().recalcDerivedStats(p);
    }
}
