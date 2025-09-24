package me.nakilex.levelplugin.trinkets.effects;

import me.nakilex.levelplugin.trinkets.data.ActiveTrinketEffect;
import me.nakilex.levelplugin.trinkets.data.TrinketEffectDefinition;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import org.bukkit.entity.Player;

/**
 * Supported trinket effect behaviours.
 */
public enum TrinketEffectType {

    COOLDOWN_REDUCTION("Cooldown Reduction") {
        @Override
        public void applySpellContext(SpellCastContext ctx, TrinketEffectDefinition effect,
                                      Player player, ActiveTrinketEffect active) {
            ctx.reduceCooldownPercent(effect.getMagnitude());
        }

        @Override
        public int resolveMagnitudeTier(double magnitude) {
            return tierFromThresholds(magnitude, new double[]{10, 20, 35, 50});
        }
    },

    NO_COOLDOWN("No Cooldown") {
        @Override
        public void applySpellContext(SpellCastContext ctx, TrinketEffectDefinition effect,
                                      Player player, ActiveTrinketEffect active) {
            ctx.putExtraParam("applyCooldown", false);
        }

        @Override
        public String formatMagnitude(TrinketEffectDefinition effect) {
            return "No cooldown";
        }

        @Override
        public int resolveMagnitudeTier(double magnitude) {
            return 5;
        }
    },

    NO_MANA_COST("Mana Surge") {
        @Override
        public void applySpellContext(SpellCastContext ctx, TrinketEffectDefinition effect,
                                      Player player, ActiveTrinketEffect active) {
            ctx.makeManaFree();
        }

        @Override
        public String formatMagnitude(TrinketEffectDefinition effect) {
            return "No mana cost";
        }

        @Override
        public int resolveMagnitudeTier(double magnitude) {
            return 5;
        }
    },

    DAMAGE_MULTIPLIER("Damage Bonus") {
        @Override
        public void applySpellContext(SpellCastContext ctx, TrinketEffectDefinition effect,
                                      Player player, ActiveTrinketEffect active) {
            ctx.addDamagePercent(effect.getMagnitude());
        }

        @Override
        public double modifyOutgoingDamage(double damage, Player player,
                                           TrinketEffectDefinition effect,
                                           ActiveTrinketEffect active) {
            return damage * (1 + effect.getMagnitude() / 100.0);
        }

        @Override
        public int resolveMagnitudeTier(double magnitude) {
            return tierFromThresholds(magnitude, new double[]{15, 30, 60, 100});
        }
    },

    IMMORTALITY("Immortality") {
        @Override
        public boolean cancelIncomingDamage(Player player, TrinketEffectDefinition effect,
                                            ActiveTrinketEffect active) {
            return true;
        }

        @Override
        public String formatMagnitude(TrinketEffectDefinition effect) {
            return "Ignore all damage";
        }

        @Override
        public int resolveMagnitudeTier(double magnitude) {
            return 5;
        }
    },

    MISSING_HEALTH_DAMAGE("Last Stand") {
        @Override
        public void applySpellContext(SpellCastContext ctx, TrinketEffectDefinition effect,
                                      Player player, ActiveTrinketEffect active) {
            double bonus = computeBonus(player, effect);
            ctx.addDamagePercent(bonus);
        }

        @Override
        public double modifyOutgoingDamage(double damage, Player player,
                                           TrinketEffectDefinition effect,
                                           ActiveTrinketEffect active) {
            double bonus = computeBonus(player, effect);
            return damage * (1 + bonus / 100.0);
        }

        private double computeBonus(Player player, TrinketEffectDefinition effect) {
            double missing = Math.max(0.0, player.getMaxHealth() - player.getHealth());
            double missingPercent = player.getMaxHealth() <= 0 ? 0 : (missing / player.getMaxHealth()) * 100.0;
            return missingPercent * (effect.getMagnitude() / 100.0);
        }

        @Override
        public int resolveMagnitudeTier(double magnitude) {
            return tierFromThresholds(magnitude, new double[]{25, 50, 75, 100});
        }
    },

    ABSORPTION_SHIELD("Barrier") {
        @Override
        public void onActivate(Player player, TrinketEffectDefinition effect, ActiveTrinketEffect active) {
            double added = effect.getMagnitude();
            active.setAppliedAbsorption(added);
            player.setAbsorptionAmount(player.getAbsorptionAmount() + added);
        }

        @Override
        public void onExpire(Player player, TrinketEffectDefinition effect, ActiveTrinketEffect active) {
            double remaining = Math.max(0.0, player.getAbsorptionAmount() - active.getAppliedAbsorption());
            player.setAbsorptionAmount(remaining);
        }

        @Override
        public String formatMagnitude(TrinketEffectDefinition effect) {
            return "+" + trimTrailingZeros(effect.getMagnitude()) + "❤";
        }

        @Override
        public int resolveMagnitudeTier(double magnitude) {
            return tierFromThresholds(magnitude, new double[]{4, 8, 12, 16});
        }
    };

    private final String displayName;

    TrinketEffectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void applySpellContext(SpellCastContext ctx, TrinketEffectDefinition effect,
                                   Player player, ActiveTrinketEffect active) {
        // default no-op
    }

    public double modifyOutgoingDamage(double damage, Player player,
                                       TrinketEffectDefinition effect,
                                       ActiveTrinketEffect active) {
        return damage;
    }

    public boolean cancelIncomingDamage(Player player, TrinketEffectDefinition effect,
                                        ActiveTrinketEffect active) {
        return false;
    }

    public void onActivate(Player player, TrinketEffectDefinition effect, ActiveTrinketEffect active) {
        // default no-op
    }

    public void onExpire(Player player, TrinketEffectDefinition effect, ActiveTrinketEffect active) {
        // default no-op
    }

    public int resolveMagnitudeTier(double magnitude) {
        return 1;
    }

    public int resolveDurationTier(double seconds) {
        int tier = (int) Math.round(seconds);
        if (tier < 1) tier = 1;
        if (tier > 10) tier = 10;
        return tier;
    }

    public String formatMagnitude(TrinketEffectDefinition effect) {
        return formatPercent(effect.getMagnitude());
    }

    protected static int tierFromThresholds(double value, double[] thresholds) {
        for (int i = 0; i < thresholds.length; i++) {
            if (value <= thresholds[i]) {
                return i + 1;
            }
        }
        return thresholds.length + 1;
    }

    protected static String formatPercent(double value) {
        if (Math.abs(value - Math.round(value)) < 0.0001) {
            return (int) Math.round(value) + "%";
        }
        return String.format("%.1f%%", value);
    }

    protected static String trimTrailingZeros(double value) {
        if (Math.abs(value - Math.round(value)) < 0.0001) {
            return String.valueOf((int) Math.round(value));
        }
        return String.format("%.1f", value);
    }
}
