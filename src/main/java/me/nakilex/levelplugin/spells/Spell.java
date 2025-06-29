package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


import java.util.List;
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
    private final double baseDamage;        // base unmodified damage
    private final boolean passive;          // if true skip mana cost indicator

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
        double baseDamage,             // ← pass in the raw dmg here
        boolean passive
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
        this.passive          = passive;
    }

    public Spell(
        String id,
        String displayName,
        String combo,
        double baseManaCost,
        long cooldownSeconds,
        int levelReq,
        List<Material> allowedWeapons,
        String effectKey,
        double baseDamage
    ) {
        this(id, displayName, combo, baseManaCost, cooldownSeconds, levelReq, allowedWeapons, effectKey, baseDamage, false);
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
    public boolean isPassive()           { return passive; }

    /** for SpellCastContext’s baseSpell.getManaCost() */
    public double getManaCost() {
        return this.baseManaCost;
    }

    /** for SpellCastContext’s baseSpell.getCooldown() */
    public long getCooldown() {
        return this.cooldownSeconds;
    }

    /** Returns the base mana cost. Dynamic increases were removed. */
    public double getCurrentManaCost(Player player) {
        return baseManaCost;
    }

    /** No-op since cost scaling has been removed. */
    public void recordSpellCast(Player player) {
        // intentionally left blank
    }

    /**
     * Handles cooldown, mana deduction, and effect dispatch.
     */
    public void castEffect(Player player) {
        UUID pid = player.getUniqueId();

        // Debug initial cast attempt
        Main.getPlugin().getLogger().info("[SpellCast] " + player.getName() +
                " attempts " + id + " via " + combo);

        // 0) Requirement check (weapon type + class)
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR ||
            (!allowedWeapons.isEmpty() && !allowedWeapons.contains(hand.getType()))) {
            player.sendMessage("§cYou can't cast " + displayName + " with this weapon.");
            return;
        }
        me.nakilex.levelplugin.items.data.CustomItem ci = me.nakilex.levelplugin.items.managers.ItemManager
                .getInstance().getCustomItemFromItemStack(hand);
        if (ci != null) {
            String reqRaw = ci.getClassRequirement();
            me.nakilex.levelplugin.player.classes.data.PlayerClass req = null;
            try {
                if (reqRaw != null && !reqRaw.isBlank()) {
                    req = me.nakilex.levelplugin.player.classes.data.PlayerClass.valueOf(reqRaw.toUpperCase());
                }
            } catch (IllegalArgumentException ignored) {}
            me.nakilex.levelplugin.player.classes.data.PlayerClass playerClass =
                    StatsManager.getInstance().getPlayerStats(pid).playerClass;
            if (!me.nakilex.levelplugin.player.classes.data.ClassUtil.meetsRequirement(playerClass, req)) {
                player.sendMessage("§cYou are not the right class to cast spells with this weapon.");
                return;
            }
        }
        // rank and ego requirements removed

        // 1) Cooldown guard
        if (cooldownMgr.isOnCooldown(pid, id)) {
            long rem = cooldownMgr.getRemainingTime(pid, id);
            //player.sendMessage("§c" + displayName + " cooling down: " + (rem/1000) + "s left");
            return;
        }

        // 2) Build our context (starts with default effectKey)
        SpellCastContext ctx = new SpellCastContext(this, player);

        // Additional effect modifiers could be applied here
        Main.getPlugin().getLogger().info("[Spell] " + id + " effects: " + ctx.getEffectKeys());
        Main.getPlugin().getLogger().info("[Spell] finalDamage=" + ctx.getFinalDamage() +
                " finalCost=" + ctx.getFinalManaCost());

        // 4) Mana check
        double cost = ctx.getFinalManaCost();
        var ps    = StatsManager.getInstance().getPlayerStats(pid);
        if (ps.getCurrentMana() < Math.ceil(cost)) {
            player.sendMessage("§cNot enough mana (" + cost + ") to cast " + displayName);
            return;
        }

        // 5) Attempt to fire effects first to see if Mythic cooldown allows it
        for (String key : ctx.getEffectKeys()) {
            SpellEffect effect = EffectRegistry.get(key);
            if (effect != null) {
                // Apply the effect and rely on the context to track success
                effect.apply(ctx);
            } else {
                player.sendMessage("§eUnknown effect: " + key);
            }
        }

        boolean success = SpellCastContextCompat.wasSuccessful(ctx);

        if (!success) {
            // Effect failed (likely Mythic cooldown) so skip cost/cooldown
            return;
        }

        int intCost = (int)Math.ceil(cost);
        ps.setCurrentMana(ps.getCurrentMana() - intCost);
        recordSpellCast(player);
        if (!passive && intCost > 0) {
            me.nakilex.levelplugin.player.attributes.managers.ManaIndicatorManager
                .getInstance().showCost(player, intCost);
        }
        Main.getInstance().getQuestManager().handleCast(player, id);

        // 6) Start cooldown (ctx.getFinalCooldown returns 0 if applyCooldown==false)
        cooldownMgr.setCooldown(pid, id, ctx.getFinalCooldown() / 1000.0);
    }


}
