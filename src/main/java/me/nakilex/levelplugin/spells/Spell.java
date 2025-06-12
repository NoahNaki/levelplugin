package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import me.nakilex.levelplugin.runes.model.RuneEffect;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single class spell, its cost, cooldown, and execution logic.
 */
public class Spell {
    private final String id;
    private final String displayName;
    private final String combo;
    private final double baseManaCost;
    private final long cooldownSeconds;     // ← renamed from int to long
    private final int levelReq;
    private final List<Material> allowedWeapons;
    private final String effectKey;
    private final double baseDamage;        // ← holds the pre-rune damage

    // static managers
    private static final CooldownManager cooldownMgr = CooldownManager.getInstance();

    public Spell(
        String id,
        String displayName,
        String combo,
        double baseManaCost,
        long cooldownSeconds,          // ← now a long
        int levelReq,
        List<Material> allowedWeapons,
        String effectKey,
        double baseDamage              // ← pass in the raw dmg here
    ) {
        this.id               = id;
        this.displayName      = displayName;
        this.combo            = combo;
        this.baseManaCost     = baseManaCost;
        this.cooldownSeconds  = cooldownSeconds;
        this.levelReq         = levelReq;
        this.allowedWeapons   = allowedWeapons;
        this.effectKey        = effectKey;
        this.baseDamage       = baseDamage;
    }

    // getters...
    public String getId()                { return id; }
    public String getDisplayName()       { return displayName; }
    public String getCombo()             { return combo; }
    public double getBaseManaCost()      { return baseManaCost; }
    public long   getCooldownSeconds()   { return cooldownSeconds; }
    public int    getLevelReq()          { return levelReq; }
    public List<Material> getAllowedWeapons() { return allowedWeapons; }
    public double getBaseDamage()        { return baseDamage; }
    public String getEffectKey()         { return effectKey; }

    /** for SpellCastContext’s baseSpell.getManaCost() */
    public double getManaCost() {
        return this.baseManaCost;
    }

    /** for SpellCastContext’s baseSpell.getCooldown() */
    public long getCooldown() {
        return this.cooldownSeconds;
    }

    /** Retrieves the dynamic mana cost for this player and spell. */
    public double getCurrentManaCost(Player player) {
        return Main.getInstance()
            .getManaTracker()
            .getCost(player.getUniqueId(), id, baseManaCost);
    }

    /** After a successful cast, record for dynamic cost adjustments. */
    public void recordSpellCast(Player player) {
        Main.getInstance()
            .getManaTracker()
            .recordCast(player.getUniqueId(), id, baseManaCost);
    }

    /**
     * Handles cooldown, runes, mana deduction, and effect dispatch.
     */
    public void castEffect(Player player) {
        UUID pid = player.getUniqueId();

        // 1) Cooldown guard
        if (cooldownMgr.isOnCooldown(pid, id)) {
            long rem = cooldownMgr.getRemainingTime(pid, id);
            //player.sendMessage("§c" + displayName + " cooling down: " + (rem/1000) + "s left");
            return;
        }

        // 2) Build our context (starts with default effectKey)
        SpellCastContext ctx = new SpellCastContext(this, player);

        // 3) Apply every equipped rune’s effects (MODIFIER + TRANSFORM params)
        List<Rune> runes = SpellManager.getInstance()
            .getRunesManager()
            .getRunesForSpell(player, id);

        for (Rune rune : runes) {
            for (RuneEffect eff : rune.getEffects()) {
                // always modify damage/cooldown
                ctx.addDamagePercent(eff.getBonusDamagePercent());
                ctx.reduceCooldownPercent(eff.getCooldownReductionPercent());

                // stack any newEffectKey
                if (eff.getNewEffectKey() != null) {
                    ctx.addEffectKey(eff.getNewEffectKey());
                }
                // pull in all extraParams (AOE, stun, projectiles, etc.)
                // pull in all extraParams (AOE, stun, projectiles, etc.) with priority
                for (Map.Entry<String, Object> e : eff.getExtraParams().entrySet()) {
                    ctx.putExtraParam(e.getKey(), e.getValue(), eff.getPriority());
                }

            }
        }

        // 4) Mana check & deduct
        double cost = ctx.getFinalManaCost();
        var ps    = StatsManager.getInstance().getPlayerStats(pid);
        if (ps.getCurrentMana() < Math.ceil(cost)) {
            player.sendMessage("§cNot enough mana (" + cost + ") to cast " + displayName);
            return;
        }
        int intCost = (int)Math.ceil(cost);
        ps.setCurrentMana(ps.getCurrentMana() - intCost);
        recordSpellCast(player);
        me.nakilex.levelplugin.player.attributes.managers.ManaIndicatorManager
            .getInstance().showCost(player, intCost);
        Main.getInstance().getQuestManager().handleCast(player, id);

        // 5) Start cooldown (ctx.getFinalCooldown returns 0 if applyCooldown==false)
        cooldownMgr.setCooldown(pid, id, ctx.getFinalCooldown() / 1000.0);

        // 6) Fire off every configured effect in order
        for (String key : ctx.getEffectKeys()) {
            SpellEffect effect = EffectRegistry.get(key);
            if (effect != null) {
                effect.apply(ctx);
            } else {
                player.sendMessage("§eUnknown effect: " + key);
            }
        }
    }


}
