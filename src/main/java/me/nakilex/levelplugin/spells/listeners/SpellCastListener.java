package me.nakilex.levelplugin.spells.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.spells.SpellAccessUtil;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.spells.input.SpellInputEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
            return;
        }
        String effectiveSpellId = SpellProgressionManager.getInstance()
                .getEffectiveSpellId(player.getUniqueId(), entry.definition().id());
        SpellRegistry.SpellEntry effectiveEntry = SpellRegistry.getInstance().getSpell(effectiveSpellId);
        if (effectiveEntry != null) {
            entry = effectiveEntry;
        }
        if (ClassUtil.isMageFamily(playerClass) && !SpellAccessUtil.isHoldingValidClassWeapon(player)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "You must be a mage and hold a valid wand to cast mage skills.");
            return;
        }
        entry.handler().cast(new SpellContext(plugin, player, entry.definition(), event));
    }
}
