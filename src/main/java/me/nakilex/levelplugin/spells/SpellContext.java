package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.input.SpellInputEvent;
import org.bukkit.entity.Player;

import java.util.Objects;

public record SpellContext(Main plugin, Player player, SpellDefinition spell, SpellInputEvent inputEvent) {
    public SpellContext {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(spell, "spell");
        Objects.requireNonNull(inputEvent, "inputEvent");
    }
}
