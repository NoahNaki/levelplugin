package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public record SpellBinding(String spellId,
                           Predicate<PlayerClass> classPredicate,
                           Predicate<ItemStack> weaponPredicate,
                           SpellInputMode inputMode,
                           String inputSequence,
                           SpellInputType inputType) {
    public SpellBinding {
        if (spellId == null || spellId.isBlank()) {
            throw new IllegalArgumentException("Spell id cannot be blank");
        }
        Objects.requireNonNull(classPredicate, "classPredicate");
        Objects.requireNonNull(weaponPredicate, "weaponPredicate");
        if (inputMode == null && inputType == null) {
            throw new IllegalArgumentException("Either input mode or input type must be provided");
        }
        if (inputMode != null && (inputSequence == null || inputSequence.isBlank())) {
            throw new IllegalArgumentException("Input sequence cannot be blank when input mode is set");
        }
    }

    public boolean matches(PlayerClass playerClass, ItemStack weapon, SpellInputMode mode, String sequence, SpellInputType type) {
        if (ClassUtil.isClassSystemEnabled() && (playerClass == null || !classPredicate.test(playerClass))) {
            return false;
        }
        if (!weaponPredicate.test(weapon)) {
            return false;
        }
        if (inputType != null) {
            return inputType == type;
        }
        if (inputMode == null || mode == null || sequence == null) {
            return false;
        }
        return inputMode == mode && inputSequence.equalsIgnoreCase(sequence.trim());
    }

    public boolean matches(PlayerClass playerClass, SpellInputMode mode, String sequence, SpellInputType type) {
        return matches(playerClass, null, mode, sequence, type);
    }

    public static SpellBinding forSequence(String spellId,
                                           Predicate<PlayerClass> classPredicate,
                                           SpellInputMode inputMode,
                                           String inputSequence) {
        return new SpellBinding(spellId.toLowerCase(Locale.ROOT), classPredicate, weapon -> true, inputMode, inputSequence, null);
    }

    public static SpellBinding forInputType(String spellId,
                                            Predicate<PlayerClass> classPredicate,
                                            SpellInputType inputType) {
        return new SpellBinding(spellId.toLowerCase(Locale.ROOT), classPredicate, weapon -> true, null, null, inputType);
    }

    public static SpellBinding forInputTypeWithWeapon(String spellId,
                                                      Predicate<PlayerClass> classPredicate,
                                                      Predicate<ItemStack> weaponPredicate,
                                                      SpellInputType inputType) {
        return new SpellBinding(spellId.toLowerCase(Locale.ROOT), classPredicate, weaponPredicate, null, null, inputType);
    }
}
