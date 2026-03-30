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
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.stream.Stream;

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
        File legacyFile = new File(plugin.getDataFolder(), "custom_mob_spells.yml");
        if (!legacyFile.exists()) {
            plugin.saveResource("custom_mob_spells.yml", false);
        }
        File folder = new File(plugin.getDataFolder(), "custom_mob_spells");
        if (!folder.exists()) {
            folder.mkdirs();
            saveBundledFolderDefaults();
        }
        loadLegacyFile(legacyFile);
        loadFolderScripts(folder);
    }

    private void loadLegacyFile(File file) {
        if (file == null || !file.exists()) {
            return;
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
            registerActions(spellId, spellNode.getMapList("actions"));
        }
    }

    private void loadFolderScripts(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return;
        }
        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            paths.filter(path -> Files.isRegularFile(path))
                    .filter(path -> path.getFileName() != null && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                    .forEach(path -> {
                        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(path.toFile());
                        List<Map<?, ?>> actions = cfg.getMapList("actions");
                        if (actions == null || actions.isEmpty()) {
                            return;
                        }
                        String relative = folder.toPath().relativize(path).toString().replace('\\', '/');
                        if (relative.toLowerCase(Locale.ROOT).endsWith(".yml")) {
                            relative = relative.substring(0, relative.length() - 4);
                        }
                        String explicitId = cfg.getString("id", "").trim();
                        registerActions(relative, actions);
                        if (!explicitId.isBlank()) {
                            registerActions(explicitId, actions);
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private void saveBundledFolderDefaults() {
        if (plugin == null) {
            return;
        }
        plugin.saveResource("custom_mob_spells/cursed_archer_shoot_1.yml", false);
        plugin.saveResource("custom_mob_spells/cursed_archer_shoot_2.yml", false);
        plugin.saveResource("custom_mob_spells/cursed_archer_shoot_3.yml", false);
        plugin.saveResource("custom_mob_spells/cursed_mage_spell_1.yml", false);
        plugin.saveResource("custom_mob_spells/cursed_mage_spell_2.yml", false);
        plugin.saveResource("custom_mob_spells/cursed_mage_spell_3.yml", false);
        plugin.saveResource("custom_mob_spells/cursed_knight_attack_1.yml", false);
        plugin.saveResource("custom_mob_spells/cursed_knight_attack_2.yml", false);
        plugin.saveResource("custom_mob_spells/cursed_knight_attack_3.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_archer_shoot.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_archer_throw_bomb.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_assassin_shadowstep.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_assassin_stab.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_assassin_slash.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_warrior_sword_slam.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_warrior_shield_rush.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_shaman_fireball.yml", false);
        plugin.saveResource("custom_mob_spells/goblin_shaman_heal.yml", false);
    }

    private void registerActions(String spellId, List<Map<?, ?>> rawActions) {
        if (spellId == null || spellId.isBlank() || rawActions == null || rawActions.isEmpty()) {
            return;
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

    public record SpellExecutionContext(Mob caster,
                                        Player target,
                                        me.nakilex.levelplugin.mob.custom.CustomMobInstance instance,
                                        CustomMobDefinition.CustomMobSpell spell) {
    }

    public record SpellActionDef(String type, YamlConfiguration params) {
    }

    @FunctionalInterface
    public interface ScriptActionHandler {
        void handle(SpellActionDef action, SpellExecutionContext context);
    }
}
