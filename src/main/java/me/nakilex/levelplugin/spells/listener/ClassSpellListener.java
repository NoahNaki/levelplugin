package me.nakilex.levelplugin.spells.listener;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.WeaponType;

import java.util.*;

/**
 * Generic spell listener handling all classes using predefined combos.
 * This replaces many duplicated *Spell listener classes.
 */
public class ClassSpellListener implements Listener {

    /** Repeating task applying the hold-shift counter each tick while sneaking */
    private final Map<UUID, BukkitTask> holdCountTasks = new HashMap<>();
    /** Repeating task attempting to cast the hold-shift spell each tick */
    private final Map<UUID, BukkitTask> holdCastTasks = new HashMap<>();
    /** Track last unsneak times for Witch double-sneak detection */
    private final Map<UUID, Long> lastUnsneak = new HashMap<>();
    /** Players who started sneaking and haven't used a sneak-click combo yet */
    private final Set<UUID> pendingSneak = new HashSet<>();
    /** Sneak releases scheduled to fire after a short delay */
    private final Map<UUID, BukkitTask> pendingUnsneak = new HashMap<>();

    private enum Trigger { LEFT, LEFT_SNEAK, RIGHT, RIGHT_SNEAK, SNEAK_START, SNEAK_END }

    private static class Triggers {
        List<String> left = Collections.emptyList();
        List<String> leftSneak = Collections.emptyList();
        List<String> right = Collections.emptyList();
        List<String> rightSneak = Collections.emptyList();
        List<String> sneakStart = Collections.emptyList();
        List<String> sneakEnd = Collections.emptyList();
        /** Mythic skill names to cast immediately on crouch before unsneak */
        List<String> sneakPrep = Collections.emptyList();
    }

    private static final Map<PlayerClass, Triggers> MAP = new EnumMap<>(PlayerClass.class);
    private static final EnumSet<PlayerClass> BOW_CLASSES = EnumSet.noneOf(PlayerClass.class);
    private static final String ATTACK_COOLDOWN_KEY = "basic_attack";
    static {
        for (PlayerClass pc : PlayerClass.values()) {
            if (ClassUtil.isArcherFamily(pc)) {
                BOW_CLASSES.add(pc);
            }
        }

        // Archer class
        Triggers t = new Triggers();
        t.leftSneak = List.of("bow_drone");
        t.left = List.of("backstep");
        t.rightSneak = List.of("dragon_piercer");
        t.right = List.of("quick_shot");
        t.sneakStart = List.of("arrow_barrage");
        MAP.put(PlayerClass.ARCHER, t);

        // Phoenix Hunter class
        t = new Triggers();
        t.leftSneak = List.of("pyroclasmic_barrage");
        t.left = List.of("ashdance");
        t.rightSneak = List.of("phoenix_rebirth");
        t.right = List.of("blazing_feathers");
        t.sneakStart = List.of("flameburst_convergence");
        MAP.put(PlayerClass.PHOENIXHUNTER, t);

        // Mage class
        t = new Triggers();
        t.leftSneak = List.of("inferno_chains");
        t.left = List.of("fireball");
        t.rightSneak = List.of("meteor");
        t.right = List.of("blink");
        t.sneakStart = List.of("frost_nova");
        t.sneakPrep = List.of("Frost_Nova");
        MAP.put(PlayerClass.MAGE, t);

        // Warrior class
        t = new Triggers();
        t.leftSneak = List.of("shockwave");
        t.left = List.of("brutal_strike");
        t.rightSneak = List.of("chain_hook");
        t.right = List.of("heroic_leap");
        t.sneakStart = List.of("shield_barrier");
        MAP.put(PlayerClass.WARRIOR, t);

        // Barbarian class
        t = new Triggers();
        t.leftSneak = List.of("eternal_fury");
        t.left = List.of("rageblade");
        t.rightSneak = List.of("double_edge");
        t.right = List.of("primal_axe");
        t.sneakStart = List.of("war_cry");
        MAP.put(PlayerClass.BARBARIAN, t);

        // Paladin class
        t = new Triggers();
        t.leftSneak = List.of("last_stand");
        t.left = List.of("holy_strike");
        t.rightSneak = List.of("heavenly_shield");
        t.right = List.of("bound_seal");
        t.sneakStart = List.of("hammer_of_justice");
        MAP.put(PlayerClass.PALADIN, t);

        // Deadeye class
        t = new Triggers();
        t.leftSneak = List.of("focus_shot");
        t.left = List.of("shotgun_blast");
        t.rightSneak = List.of("air_strike");
        t.right = List.of("pistol_shot");
        t.sneakStart = List.of("sniper_backup");
        MAP.put(PlayerClass.DEADEYE, t);

        // Death Knight class (two combos on sneak start)
        t = new Triggers();
        t.leftSneak = List.of("death_sentence");
        t.left = List.of("death_strike");
        t.rightSneak = List.of("necrotic_whirlwind");
        t.right = List.of("phantom_charge");
        t.sneakStart = List.of("wraithbound_chains", "soul_barrier");
        MAP.put(PlayerClass.DEATHKNIGHT, t);

        // Abyssion class
        t = new Triggers();
        t.leftSneak = List.of("abyssal_smash");
        t.left = List.of("aqua_slash");
        t.rightSneak = List.of("tidal_wave");
        t.right = List.of("abyssal_dash");
        t.sneakStart = List.of("aqua_aura");
        MAP.put(PlayerClass.ABYSSION, t);

        // Rogue class
        t = new Triggers();
        t.leftSneak = List.of("blade_dance");
        t.left = List.of("blade_slash");
        t.rightSneak = List.of("dagger_throw");
        t.right = List.of("assassin_dash");
        t.sneakStart = List.of("shadow_walk");
        MAP.put(PlayerClass.ROGUE, t);

        // Awakened Rogue class
        t = new Triggers();
        t.left = List.of("lethal_combo");
        t.right = List.of("ravaging_dash");
        t.rightSneak = List.of("crimson_arc");
        t.leftSneak = List.of("last_dance");
        // Death Bloom is the class's sneak ability
        t.sneakStart = List.of("death_bloom");
        t.sneakPrep = List.of("Death_Bloom");
        MAP.put(PlayerClass.AWAKROGUE, t);

        // Awakened Warrior class
        t = new Triggers();
        t.left = List.of("brutal_combo");
        t.right = List.of("berserkers_leap");
        t.rightSneak = List.of("relentless_whirlwind");
        t.leftSneak = List.of("strike_of_fury");
        t.sneakStart = List.of("bloodbound_barrier");
        t.sneakEnd = List.of("vicious_strike");
        MAP.put(PlayerClass.AWAKWARRIOR, t);

        // Awakened Archer class
        t = new Triggers();
        t.left = List.of("evasive_shot");
        t.right = List.of("blasting_combo");
        t.leftSneak = List.of("rapid_arrows");
        t.rightSneak = List.of("shot_of_destruction");
        t.sneakStart = List.of("volley_of_arrows");
        MAP.put(PlayerClass.AWAKARCHER, t);

        // Awakened Mage class
        t = new Triggers();
        t.left = List.of("arcane_slash");
        t.right = List.of("blizzard");
        t.leftSneak = List.of("arcane_devastation");
        t.rightSneak = List.of("chains_of_void");
        t.sneakStart = List.of("meteor_storm");
        t.sneakEnd = List.of("cloak_of_hastur");
        MAP.put(PlayerClass.ARCHMAGE, t);

        // Dragonian class
        t = new Triggers();
        t.leftSneak = List.of("taotie_dragon");
        t.left = List.of("dragonian_slash");
        t.rightSneak = List.of("dragonian_rs");
        t.right = List.of("dragonian_lunge");
        t.sneakStart = List.of("dragonian_ss");
        MAP.put(PlayerClass.DRAGONIAN, t);

        // Dragon Warrior class
        t = new Triggers();
        t.leftSneak = List.of("dragonborn");
        t.left = List.of("dragon_slash");
        t.rightSneak = List.of("dragon_breath");
        t.right = List.of("dragon_dash");
        t.sneakStart = List.of("dragon_zone");
        MAP.put(PlayerClass.DRAGONWARRIOR, t);

        // Windrune class (known internally as GALEGLAIVE)
        t = new Triggers();
        t.leftSneak = List.of("windbound_fury");
        t.left = List.of("gale_slash");
        t.rightSneak = List.of("dancing_blade");
        t.right = List.of("vault");
        t.sneakStart = List.of("cloudpiercer");
        t.sneakEnd = List.of("torrent");
        MAP.put(PlayerClass.GALEGLAIVE, t);

        // Arctic Knight class
        t = new Triggers();
        t.leftSneak = List.of("permafrost_lance");
        t.left = List.of("frost_strike");
        t.rightSneak = List.of("frozen_shield");
        t.right = List.of("glacial_impalement");
        t.sneakStart = List.of("arctic_charge");
        MAP.put(PlayerClass.ARCTICKNIGHT, t);

        // Witch class
        t = new Triggers();
        t.left = List.of("mf_class_witch_normalattack");
        t.leftSneak = List.of("mf_class_witch_sneak_leftclick");
        t.right = List.of("mf_class_witch_rightclick");
        t.rightSneak = List.of("mf_class_witch_sneak_rightclick");
        // hold-shift counter begins on crouch
        t.sneakStart = List.of(
                "mf_class_witch_holdshift_cruibile_count"
        );
        // double-crouch counter starts when the player uncrouches
        t.sneakEnd = List.of(
                "mf_class_witch_shiftshift_cruibile_count"
        );
        MAP.put(PlayerClass.WITCH, t);

        // Cleric class (no default combos yet)
        MAP.put(PlayerClass.CLERIC, new Triggers());
    }

    private void cast(Player player, List<String> combos, PlayerClass pc) {
        if (combos == null) return;
        if (Main.getInstance().getDialogManager().hasSession(player)) return;
        for (String id : combos) {
            Spell spell = SpellManager.getInstance().getSpellById(pc.name().toLowerCase(), id);
            if (spell == null) {
                MythicBukkit.inst().getAPIHelper().castSkill(player, id);
            } else {
                spell.castEffect(player);
                StatsManager.getInstance().recalcDerivedStats(player);
            }
        }
    }

    private PlayerClass getClass(Player player) {
        return StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
    }

    private boolean tryBasicAttack(Player player) {
        if (player.getAttackCooldown() < 1.0) return false;
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double cooldown = 1.0 / ps.attackSpeed;
        CooldownManager cd = CooldownManager.getInstance();
        UUID id = player.getUniqueId();
        if (cd.isOnCooldown(id, ATTACK_COOLDOWN_KEY)) return false;
        cd.setCooldown(id, ATTACK_COOLDOWN_KEY, cooldown);
        player.resetCooldown();
        return true;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;

        if (!BOW_CLASSES.contains(pc)) {
            if (!tryBasicAttack(p)) {
                event.setCancelled(true);
                return;
            }
        }

        if (p.isSneaking()) {
            cast(p, tr.leftSneak, pc);
            pendingSneak.remove(p.getUniqueId());
        } else {
            cast(p, tr.left, pc);
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        boolean weapon = WeaponType.matchType(event.getItem()) != null;
        if (!weapon) return;

        event.setCancelled(true);

        if (p.isSneaking()) {
            cast(p, tr.rightSneak, pc);
            pendingSneak.remove(p.getUniqueId());
            return;
        }

        if (BOW_CLASSES.contains(pc) && !tryBasicAttack(p)) return;

        cast(p, tr.right, pc);
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        if (event.isSneaking()) {
            UUID id = p.getUniqueId();
            // cancel any pending unsneak cast if player crouches again quickly
            BukkitTask pending = pendingUnsneak.remove(id);
            if (pending != null) {
                pending.cancel();
                // make sure any preparatory aura from the first crouch still fires
                if (!tr.sneakStart.isEmpty()) {
                    cast(p, tr.sneakStart, pc);
                }
            }

            boolean doubleSneak = false;
            if (pc != PlayerClass.WITCH && !tr.sneakEnd.isEmpty()) {
                long now = System.currentTimeMillis();
                Long last = lastUnsneak.get(id);
                if (last != null && now - last <= 500) {
                    cast(p, tr.sneakEnd, pc);
                    doubleSneak = true;
                }
            }

            if (pc != PlayerClass.WITCH) {
                if (!doubleSneak && !tr.sneakStart.isEmpty()) {
                    pendingSneak.add(id);
                }
                for (String skill : tr.sneakPrep) {
                    MythicBukkit.inst().getAPIHelper().castSkill(p, skill);
                }
            }

            if (pc == PlayerClass.WITCH) {
                long now = System.currentTimeMillis();
                Long last = lastUnsneak.get(id);
                if (last != null && now - last <= 500) {
                    // double crouch
                    MythicBukkit.inst().getAPIHelper().castSkill(p, "mf_class_witch_shiftshift");
                }

                BukkitTask countTask = Bukkit.getScheduler().runTaskTimer(
                        Main.getPlugin(),
                        () -> {
                            if (p.isOnline() && p.isSneaking()) {
                                MythicBukkit.inst().getAPIHelper().castSkill(p, "mf_class_witch_holdshift_cruibile_count");
                            }
                        },
                        0L, 1L
                );
                BukkitTask castTask = Bukkit.getScheduler().runTaskTimer(
                        Main.getPlugin(),
                        () -> {
                            if (p.isOnline() && p.isSneaking()) {
                                MythicBukkit.inst().getAPIHelper().castSkill(p, "mf_class_witch_holdshift");
                            }
                        },
                        0L, 1L
                );

                BukkitTask old = holdCountTasks.put(p.getUniqueId(), countTask);
                if (old != null) old.cancel();
                old = holdCastTasks.put(p.getUniqueId(), castTask);
                if (old != null) old.cancel();
            }
        } else {
            UUID id = p.getUniqueId();
            boolean castSneak = pendingSneak.remove(id);
            if (castSneak) {
                BukkitTask old = pendingUnsneak.remove(id);
                if (old != null) old.cancel();
                BukkitTask task = Bukkit.getScheduler().runTaskLater(
                        Main.getPlugin(),
                        () -> {
                            if (p.isOnline() && !p.isSneaking()) {
                                cast(p, tr.sneakStart, pc);
                            }
                            pendingUnsneak.remove(id);
                        },
                        4L
                );
                pendingUnsneak.put(id, task);
            } else if (pc == PlayerClass.WITCH) {
                cast(p, tr.sneakEnd, pc);
            }

            lastUnsneak.put(id, System.currentTimeMillis());

            if (pc == PlayerClass.WITCH) {
                BukkitTask task = holdCountTasks.remove(id);
                if (task != null) task.cancel();
                task = holdCastTasks.remove(id);
                if (task != null) task.cancel();
            }
        }
    }
}
