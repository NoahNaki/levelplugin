package me.nakilex.levelplugin.mob.custom.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generic YAML-driven spell script runtime for custom mobs.
 */
public final class CustomMobSpellScriptEngine {
    private final Main plugin;
    private final Map<String, List<SpellActionDef>> scriptedActions = new HashMap<>();

    public CustomMobSpellScriptEngine(Main plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean execute(String spellId,
                           SpellExecutionContext context,
                           ScriptActionHandler handler) {
        if (spellId == null || context == null || handler == null) {
            return false;
        }
        List<SpellActionDef> actions = scriptedActions.get(spellId.toLowerCase(Locale.ROOT));
        if (actions == null || actions.isEmpty()) {
            return false;
        }
        runAction(actions, 0, context, handler);
        return true;
    }

    private void runAction(List<SpellActionDef> actions,
                           int index,
                           SpellExecutionContext context,
                           ScriptActionHandler handler) {
        if (index >= actions.size() || context.caster() == null || context.target() == null) {
            return;
        }
        SpellActionDef action = actions.get(index);
        if ("delay".equals(action.type())) {
            long ticks = Math.max(0L, action.params().getLong("ticks", 0L));
            Bukkit.getScheduler().runTaskLater(plugin, () -> runAction(actions, index + 1, context, handler), ticks);
            return;
        }
        handler.handle(action, context);
        runAction(actions, index + 1, context, handler);
    }

    private void load() {
        scriptedActions.clear();
        if (plugin == null) {
            return;
        }
        File file = new File(plugin.getDataFolder(), "custom_mob_spells.yml");
        if (!file.exists()) {
            plugin.saveResource("custom_mob_spells.yml", false);
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection spells = cfg.getConfigurationSection("spells");
        if (spells == null) {
            return;
        }
        for (String spellId : spells.getKeys(false)) {
            ConfigurationSection spellNode = spells.getConfigurationSection(spellId);
            if (spellNode == null) {
                continue;
            }
            List<Map<?, ?>> rawActions = spellNode.getMapList("actions");
            if (rawActions == null || rawActions.isEmpty()) {
                continue;
            }
            List<SpellActionDef> parsed = new ArrayList<>();
            for (Map<?, ?> raw : rawActions) {
                if (raw == null || raw.isEmpty()) {
                    continue;
                }
                Object typeToken = raw.get("type");
                if (!(typeToken instanceof String type) || type.isBlank()) {
                    continue;
                }
                YamlConfiguration actionParams = new YamlConfiguration();
                for (Map.Entry<?, ?> entry : raw.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    if ("type".equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                        continue;
                    }
                    actionParams.set(String.valueOf(entry.getKey()), entry.getValue());
                }
                parsed.add(new SpellActionDef(type.toLowerCase(Locale.ROOT), actionParams));
            }
            if (!parsed.isEmpty()) {
                scriptedActions.put(spellId.toLowerCase(Locale.ROOT), List.copyOf(parsed));
            }
        }
    }

    public record SpellExecutionContext(Mob caster,
                                        Player target,
                                        CustomMobDefinition.CustomMobSpell spell) {
    }

    public record SpellActionDef(String type, YamlConfiguration params) {
    }

    @FunctionalInterface
    public interface ScriptActionHandler {
        void handle(SpellActionDef action, SpellExecutionContext context);
    }
}
