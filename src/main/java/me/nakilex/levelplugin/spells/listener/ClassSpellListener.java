package me.nakilex.levelplugin.spells.listener;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.SpellManager;
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

import java.util.*;

/**
 * Generic spell listener handling all classes using predefined combos.
 * This replaces many duplicated *Spell listener classes.
 */
public class ClassSpellListener implements Listener {

    /** Track sneaking timers for Witch hold-shift ability */
    private final Map<UUID, BukkitTask> holdTasks = new HashMap<>();
    /** Track last unsneak times for Witch double-sneak detection */
    private final Map<UUID, Long> lastUnsneak = new HashMap<>();

    private enum Trigger { LEFT, LEFT_SNEAK, RIGHT, RIGHT_SNEAK, SNEAK_START, SNEAK_END }

    private static class Triggers {
        List<String> left = Collections.emptyList();
        List<String> leftSneak = Collections.emptyList();
        List<String> right = Collections.emptyList();
        List<String> rightSneak = Collections.emptyList();
        List<String> sneakStart = Collections.emptyList();
        List<String> sneakEnd = Collections.emptyList();
    }

    private static final Map<PlayerClass, Triggers> MAP = new EnumMap<>(PlayerClass.class);
    static {
        // Archer class
        Triggers t = new Triggers();
        t.leftSneak = List.of("bow_drone");
        t.left = List.of("quick_shot");
        t.rightSneak = List.of("dragon_piercer");
        t.right = List.of("backstep");
        t.sneakStart = List.of("arrow_barrage");
        MAP.put(PlayerClass.ARCHER, t);

        // Phoenix Hunter class
        t = new Triggers();
        t.leftSneak = List.of("phoenix_rebirth");
        t.left = List.of("blazing_feathers");
        t.rightSneak = List.of("pyroclasmic_barrage");
        t.right = List.of("ashdance");
        t.sneakStart = List.of("flameburst_convergence");
        MAP.put(PlayerClass.PHOENIXHUNTER, t);

        // Mage class
        t = new Triggers();
        t.leftSneak = List.of("inferno_chains");
        t.left = List.of("fireball");
        t.rightSneak = List.of("meteor");
        t.right = List.of("blink");
        t.sneakStart = List.of("frost_nova");
        MAP.put(PlayerClass.MAGE, t);

        // Warrior class
        t = new Triggers();
        t.leftSneak = List.of("shockwave");
        t.left = List.of("brutal_strike");
        t.rightSneak = List.of("chain_hook");
        t.right = List.of("charge");
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
        t.leftSneak = List.of("air_strike");
        t.left = List.of("pistol_shot");
        t.rightSneak = List.of("focus_shot");
        t.right = List.of("shotgun_blast");
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
    }

    private void cast(Player player, List<String> combos, PlayerClass pc) {
        if (combos == null) return;
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

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        if (p.isSneaking()) cast(p, tr.leftSneak, pc);
        else cast(p, tr.left, pc);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getHand().ordinal() != 0) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        event.setCancelled(true);
        if (p.isSneaking()) cast(p, tr.rightSneak, pc);
        else cast(p, tr.right, pc);
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        if (event.isSneaking()) {
            cast(p, tr.sneakStart, pc);

            if (pc == PlayerClass.WITCH) {
                long now = System.currentTimeMillis();
                Long last = lastUnsneak.get(p.getUniqueId());
                if (last != null && now - last <= 500) {
                    // double crouch
                    MythicBukkit.inst().getAPIHelper().castSkill(p, "mf_class_witch_shiftshift");
                }

                // schedule hold-shift check
                // Cast hold-shift skill a little after the aura stacks reach
                // the required threshold (~1.25s). This ensures the Mythic
                // skill conditions have time to accumulate.
                BukkitTask task = Bukkit.getScheduler().runTaskLater(
                        Main.getPlugin(),
                        () -> {
                            if (p.isOnline() && p.isSneaking()) {
                                MythicBukkit.inst().getAPIHelper().castSkill(p, "mf_class_witch_holdshift");
                            }
                        },
                        30L
                );
                BukkitTask old = holdTasks.put(p.getUniqueId(), task);
                if (old != null) old.cancel();
            }
        } else {
            cast(p, tr.sneakEnd, pc);

            if (pc == PlayerClass.WITCH) {
                lastUnsneak.put(p.getUniqueId(), System.currentTimeMillis());
                BukkitTask task = holdTasks.remove(p.getUniqueId());
                if (task != null) task.cancel();
            }
        }
    }
}
