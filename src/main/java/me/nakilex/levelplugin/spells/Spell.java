package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.CooldownIndicatorManager;

import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.player.level.managers.LevelManager;


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
    private final boolean mobility;         // if true use ManaCostTracker scaling

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
        boolean passive,
        boolean mobility
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
        this.mobility         = mobility;
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
        this(id, displayName, combo, baseManaCost, cooldownSeconds, levelReq, allowedWeapons, effectKey, baseDamage, false, false);
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
        if (mobility) {
            return Main.getInstance().getManaTracker().getCost(player.getUniqueId(), id, baseManaCost);
        }
        return baseManaCost;
    }

    /** No-op since cost scaling has been removed. */
    public void recordSpellCast(Player player) {
        if (mobility) {
            Main.getInstance().getManaTracker().recordCast(player.getUniqueId(), id, baseManaCost);
        }
    }

    /**
     * Handles cooldown, mana deduction, and effect dispatch.
     */
    public void castEffect(Player player) {
        UUID pid = player.getUniqueId();

        if (Main.getInstance().getDialogManager().hasSession(player)) {
            return;
        }

        // Guard against casting spells above the player's level
        int playerLevel = LevelManager.getInstance().getLevel(player);
        if (playerLevel < levelReq) {
            player.sendMessage("§cYou must be level " + levelReq + " to cast " + displayName + ".");
            return;
        }

        // Debug initial cast attempt
        Main.getPlugin().getLogger().info("[SpellCast] " + player.getName() +
                " attempts " + id + " via " + combo);

        // 0) Requirement check (weapon type + class)
        ItemStack hand = player.getInventory().getItemInMainHand();
        Material matCheck = hand != null ? hand.getType() : Material.AIR;
        if (hand != null && hand.hasItemMeta()) {
            var pdc = hand.getItemMeta().getPersistentDataContainer();
            if (pdc.has(me.nakilex.levelplugin.items.utils.ItemUtil.TEMPLATE_MATERIAL_KEY,
                        org.bukkit.persistence.PersistentDataType.STRING)) {
                String stored = pdc.get(me.nakilex.levelplugin.items.utils.ItemUtil.TEMPLATE_MATERIAL_KEY,
                                       org.bukkit.persistence.PersistentDataType.STRING);
                try { matCheck = Material.valueOf(stored); } catch (Exception ignore) {}
            }
        }
        me.nakilex.levelplugin.items.data.CustomItem ci =
                me.nakilex.levelplugin.items.managers.ItemManager.getInstance()
                        .getCustomItemFromItemStack(hand);
        boolean skipWeaponCheck = false;
        if (ci != null) {
            String reqRaw = ci.getClassRequirement();
            me.nakilex.levelplugin.player.classes.data.PlayerClass req = null;
            if (reqRaw != null && !reqRaw.isBlank()) {
                req = me.nakilex.levelplugin.player.classes.data.PlayerClass.fromString(reqRaw);
            }

            me.nakilex.levelplugin.player.classes.data.PlayerClass playerClass =
                    StatsManager.getInstance().getPlayerStats(pid).playerClass;
            if (!me.nakilex.levelplugin.player.classes.data.ClassUtil.meetsRequirement(playerClass, req)) {
                player.sendMessage("§cYou are not the right class to cast spells with this weapon.");
                return;
            }

            int weaponLevel = ci.getLevelRequirement();
            if (playerLevel < weaponLevel) {
                player.sendMessage("§cYou must be level " + weaponLevel
                        + " to cast spells with " + ci.getBaseName() + ".");
                return;
            }

            // If the item explicitly requires this class, allow any material
            skipWeaponCheck = req != null;
        }

        if (hand == null || matCheck == Material.AIR) {
            return; // empty hand - silently fail
        }

        if (!skipWeaponCheck && !allowedWeapons.isEmpty()) {
            WeaponType type = WeaponType.matchType(hand);
            if (type == null) {
                // Not a weapon, ignore without messaging
                return;
            }
            if (!allowedWeapons.contains(matCheck)) {
                player.sendMessage("§cYou can't cast " + displayName + " with this weapon.");
                return;
            }
        }
        // rank requirements removed

        // 1) Cooldown guard
        if (cooldownMgr.isOnCooldown(pid, id)) {
            long rem = cooldownMgr.getRemainingTime(pid, id);
            CooldownIndicatorManager.getInstance().show(player, displayName, rem, 0);
            return;
        }

        // 2) Build our context (starts with default effectKey)
        SpellCastContext ctx = new SpellCastContext(this, player);

        // Additional effect modifiers could be applied here
        me.nakilex.levelplugin.trinkets.managers.TrinketManager tManager =
                me.nakilex.levelplugin.Main.getInstance().getTrinketManager();
        if (tManager != null) {
            tManager.applySpellModifiers(player, ctx);
        }
        Main.getPlugin().getLogger().info("[Spell] " + id + " effects: " + ctx.getEffectKeys());
        Main.getPlugin().getLogger().info("[Spell] finalDamage=" + ctx.getFinalDamage() +
                " finalCost=" + ctx.getFinalManaCost());

        // 4) Mana check
        double cost = ctx.getFinalManaCost();
        var ps    = StatsManager.getInstance().getPlayerStats(pid);
        int intCost = (int)Math.ceil(cost);
        if (ps.getCurrentMana() < intCost) {
            player.sendMessage("§cNot enough mana (" + intCost + ") to cast " + displayName);
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

        // Trigger a sweeping strike for melee basic attacks
        if ("BASIC_ATTACK".equals(combo)) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (weapon != null) {
                String name = weapon.getType().name();
                if (name.endsWith("_SWORD") || name.endsWith("_SHOVEL")) {
                    double strength = ps.baseStrength + ps.bonusStrength;
                    double damage = ctx.getFinalDamage() + strength * 0.5;
                    int totalTec = ps.baseTechnique + ps.bonusTechnique;
                    damage *= (1.0 + totalTec * 0.003);
                    int totalDex = ps.baseDexterity + ps.bonusDexterity;
                    double critChance = (double) totalDex / (totalDex + 100.0);
                    if (Math.random() < critChance) {
                        damage *= 2;
                    }
                    me.nakilex.levelplugin.mob.utils.SweepAttack.perform(player, damage);
                }
            }
        }

        ps.setCurrentMana(ps.getCurrentMana() - intCost);
        recordSpellCast(player);
        Main.getInstance().getQuestManager().handleCast(player, id);

        // 6) Start cooldown (ctx.getFinalCooldown returns 0 if applyCooldown==false)
        long cdMs = ctx.getFinalCooldown();
        cooldownMgr.setCooldown(pid, id, cdMs / 1000.0);
        if (intCost > 0) {
            CooldownIndicatorManager.getInstance().show(player, displayName, 0, intCost);
        }
    }


}
