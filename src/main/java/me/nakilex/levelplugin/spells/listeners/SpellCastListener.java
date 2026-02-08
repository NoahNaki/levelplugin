package me.nakilex.levelplugin.spells.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellRegistry;
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
        entry.handler().cast(new SpellContext(plugin, player, entry.definition(), event));
    }
}
