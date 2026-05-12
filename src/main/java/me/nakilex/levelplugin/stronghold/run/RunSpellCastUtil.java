package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.SpellCastManager;
import me.nakilex.levelplugin.spells.SpellContext;
import me.nakilex.levelplugin.spells.SpellDefinition;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.input.SpellInputEvent;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/** Shared Stronghold-style run spell casting behavior for run-scoped upgrades. */
public final class RunSpellCastUtil {
    public enum ManualCastTrigger {
        NONE,
        RIGHT_CLICK,
        LEFT_CLICK_BASIC
    }

    private RunSpellCastUtil() {
    }

    public static long computeAutoCastCooldownMs(Player player,
                                                 SpellDefinition definition,
                                                 int cooldownUpgradeTier,
                                                 long baseCooldownMs) {
        if (definition == null) {
            return Math.max(0L, baseCooldownMs);
        }
        long cooldown = Math.max(baseCooldownMs, SpellCastManager.getInstance().getCooldownMs(player, definition));
        int tier = Math.max(0, cooldownUpgradeTier);
        double multiplier = Math.max(0.45, 1.0 - (tier * 0.10));
        return Math.max(600L, Math.round(cooldown * multiplier));
    }

    public static boolean shouldAutoCastSpellNow(Player player, String spellId) {
        if (player == null || spellId == null || spellId.isBlank()) {
            return false;
        }
        String normalized = spellId.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("warrior_guarded_resolve")) {
            return countNearbyEnemies(player, 7.0) >= 3 || player.getHealth() <= Math.max(6.0, player.getMaxHealth() * 0.70);
        }
        return findNearestLockTarget(player, 16.0) != null;
    }

    public static LivingEntity findNearestLockTarget(Player caster, double range) {
        return SpellEffectUtil.getLivingTargets(caster.getLocation(), Math.max(2.0, range), living -> !living.equals(caster))
                .stream()
                .min(Comparator.comparingDouble(living -> living.getLocation().distanceSquared(caster.getLocation())))
                .orElse(null);
    }

    public static int countNearbyEnemies(Player player, double radius) {
        return SpellEffectUtil.getLivingTargets(player.getLocation(), Math.max(2.0, radius), living -> !living.equals(player))
                .size();
    }

    public static SpellRegistry.SpellEntry resolveOwnedManualSpell(Map<String, String> activeSpellByBase,
                                                                   ManualCastTrigger trigger) {
        if (activeSpellByBase == null || activeSpellByBase.isEmpty()) {
            return null;
        }
        SpellRegistry registry = SpellRegistry.getInstance();
        for (String spellId : activeSpellByBase.values()) {
            SpellRegistry.SpellEntry entry = registry.getSpell(spellId);
            if (entry == null || entry.definition() == null || manualCastTrigger(entry.definition()) != trigger) {
                continue;
            }
            return entry;
        }
        return null;
    }

    public static ManualCastTrigger manualCastTrigger(SpellDefinition definition) {
        if (definition == null || definition.id() == null) {
            return ManualCastTrigger.NONE;
        }
        if (definition.movementSpell()) {
            return ManualCastTrigger.RIGHT_CLICK;
        }
        String spellId = definition.id().toLowerCase(Locale.ROOT);
        if (spellId.startsWith("rogue_arc_basic")) {
            return ManualCastTrigger.LEFT_CLICK_BASIC;
        }
        return ManualCastTrigger.NONE;
    }

    public static SpellInputEvent createSyntheticInputEvent(Player player, String inputSequence) {
        return new SpellInputEvent(player, SpellInputType.BASIC_ATTACK, SpellInputMode.MOUSE_COMBO, inputSequence);
    }

    public static boolean castSpell(Main plugin,
                                    Player player,
                                    SpellRegistry.SpellEntry spellEntry,
                                    SpellInputEvent inputEvent,
                                    boolean consumeResources,
                                    Predicate<Player> canCast) {
        try {
            if (plugin == null || player == null || spellEntry == null || spellEntry.definition() == null || inputEvent == null) {
                return false;
            }
            if (canCast != null && !canCast.test(player)) {
                return false;
            }
            if (consumeResources && !SpellCastManager.getInstance().tryConsumeResources(player, spellEntry.definition())) {
                return false;
            }
            spellEntry.handler().cast(new SpellContext(plugin, player, spellEntry.definition(), inputEvent));
            SpellCastManager.getInstance().recordCast(player, spellEntry.definition());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
