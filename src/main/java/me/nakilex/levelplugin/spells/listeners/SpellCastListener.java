package me.nakilex.levelplugin.spells.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.SpellInputDebugItem;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.spells.SpellAccessUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.spells.input.SpellInputEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class SpellCastListener implements Listener {
    private final Main plugin;

    public SpellCastListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpellInput(SpellInputEvent event) {
        Player player = event.getPlayer();
        var playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        SpellRegistry.SpellEntry entry = SpellRegistry.getInstance().resolveSpell(playerClass,
                event.getInputMode(), event.getInputSequence(), event.getInputType());
        if (entry == null) {
            sendDebug(player, ChatColor.RED + "No spell binding matched for "
                    + ChatColor.WHITE + event.getInputType().name()
                    + ChatColor.GRAY + " (" + ChatColor.WHITE + event.getInputMode().name()
                    + ChatColor.GRAY + ", seq=" + ChatColor.WHITE + event.getInputSequence() + ChatColor.GRAY + ").");
            return;
        }
        sendDebug(player, ChatColor.GREEN + "Resolved spell binding "
                + ChatColor.WHITE + entry.definition().id()
                + ChatColor.GRAY + " from "
                + ChatColor.WHITE + event.getInputType().name()
                + ChatColor.GRAY + " (" + ChatColor.WHITE + event.getInputMode().name()
                + ChatColor.GRAY + ", seq=" + ChatColor.WHITE + event.getInputSequence() + ChatColor.GRAY + ").");
        String effectiveSpellId = SpellProgressionManager.getInstance()
                .getEffectiveSpellId(player.getUniqueId(), entry.definition().id());
        SpellRegistry.SpellEntry effectiveEntry = SpellRegistry.getInstance().getSpell(effectiveSpellId);
        if (effectiveEntry != null) {
            entry = effectiveEntry;
            sendDebug(player, ChatColor.AQUA + "Effective spell id "
                    + ChatColor.WHITE + effectiveSpellId + ChatColor.GRAY + ".");
        }
        if (ClassUtil.isMageFamily(playerClass) && !SpellAccessUtil.isHoldingValidClassWeapon(player)) {
            if (!SpellAccessUtil.isHoldingLifeSkillTool(player) && SpellAccessUtil.isHoldingWeapon(player)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "You must hold a valid class weapon to cast mage skills.");
            }
            sendDebug(player, ChatColor.RED + "Cast blocked: invalid class weapon for mage.");
            return;
        }
        sendDebug(player, ChatColor.GREEN + "Casting "
                + ChatColor.WHITE + entry.definition().id()
                + ChatColor.GRAY + " now.");
        entry.handler().cast(new SpellContext(plugin, player, entry.definition(), event));
        sendDebug(player, ChatColor.GREEN + "Handler executed for "
                + ChatColor.WHITE + entry.definition().id()
                + ChatColor.GRAY + ".");
    }

    private void sendDebug(Player player, String message) {
        if (player == null) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!SpellInputDebugItem.isDebugStick(held)) {
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                ChatColor.AQUA + "Spell Cast Debug" + ChatColor.GRAY + ": " + message);
    }
}
