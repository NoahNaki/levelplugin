package me.nakilex.levelplugin.spells;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.ego.EgoRarity;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.spells.context.SpellCastContext;
import me.nakilex.levelplugin.spells.context.SpellCastContextCompat;
import me.nakilex.levelplugin.spells.effect.SpellEffect;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.spells.registry.EffectRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import me.nakilex.levelplugin.items.utils.ItemUtil;

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

        // 0) Requirement check (level or weapon rank)
        boolean ego = false;
        int rank = 0;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR ||
            (!allowedWeapons.isEmpty() && !allowedWeapons.contains(hand.getType()))) {
            player.sendMessage("§cYou can't cast " + displayName + " with this weapon.");
            return;
        }

        if (hand.hasItemMeta()) {
            PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
            if (pdc.has(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER)) {
                ego = true;
                rank = pdc.get(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER);
            } else {
                // Mythic items may lack the ego data so fall back on the display name
                String name = hand.getItemMeta().getDisplayName();
                if (name != null) {
                    String stripped = org.bukkit.ChatColor.stripColor(name);
                    String lower = stripped.toLowerCase();
                    String prefix = null;
                    if (lower.contains("abyssion")) prefix = "abyssion";
                    else if (lower.contains("necroslayer")) prefix = "death";
                    else if (lower.contains("windrune") || lower.contains("windreaver"))
                        prefix = "windrune";
                    if (prefix != null) {
                        ego = true;
                        rank = 1; // start at rank 1 like normal ego weapons
                        // initialize basic ego data so tooltip works
                        ItemMeta meta = hand.getItemMeta();
                        PersistentDataContainer mpdc = meta.getPersistentDataContainer();
                        mpdc.set(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING, prefix + "_ego");
                        mpdc.set(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
                        mpdc.set(ItemUtil.EGO_EXP_KEY, PersistentDataType.INTEGER, 0);
                        mpdc.set(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING, EgoRarity.RARE.name());
                        hand.setItemMeta(meta);
                        ItemUtil.updateEgoWeaponTooltip(hand, player);
                    }
                }
            }
        }
        if (rank < levelReq) {
            player.sendMessage("§cYour weapon must be rank " + levelReq + " to cast " + displayName);
            return;
        }

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
