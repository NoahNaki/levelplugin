package me.nakilex.levelplugin.spells.listener;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.player.attributes.managers.CooldownIndicatorManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.spells.Spell;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.utils.PotionEffectUtil;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Generic spell listener handling all classes using predefined combos.
 * This replaces many duplicated *Spell listener classes.
 */
public class ClassSpellListener implements Listener {

    /** Repeating task invoking hold-style crouch abilities each tick */
    private final Map<UUID, BukkitTask> holdTasks = new HashMap<>();
    /** Track how many times a player's hold task has executed while sneaking */
    private final Map<UUID, Integer> holdRuns = new HashMap<>();
    /** Track last unsneak times for Witch double-sneak detection */
    private final Map<UUID, Long> lastUnsneak = new HashMap<>();
    /** Players who started sneaking and haven't used a sneak-click combo yet */
    private final Set<UUID> pendingSneak = new HashSet<>();
    /** Sneak releases scheduled to fire after a short delay */
    private final Map<UUID, BukkitTask> pendingUnsneak = new HashMap<>();
    /** NPC interactions to avoid spell casts on the same click */
    private final Map<UUID, Long> recentNpcInteractions = new HashMap<>();

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

    private static class HoldConfig {
        final long delay;
        final long period;
        final List<String> skills;
        /** Number of executions before the crouch tap combo should be cancelled */
        final int consumeAfterRuns;

        HoldConfig(long delay, long period, List<String> skills, int consumeAfterRuns) {
            this.delay = delay;
            this.period = period;
            this.skills = skills;
            this.consumeAfterRuns = consumeAfterRuns;
        }
    }

    private static final Map<PlayerClass, Triggers> MAP = new EnumMap<>(PlayerClass.class);
    private static final Map<PlayerClass, HoldConfig> HOLD_MAP = new EnumMap<>(PlayerClass.class);
    private static final EnumSet<PlayerClass> BOW_CLASSES = EnumSet.noneOf(PlayerClass.class);
    private static final Map<PlayerClass, Map<Trigger, Double>> MANUAL_TRIGGER_COOLDOWNS = new EnumMap<>(PlayerClass.class);
    private static final Map<PlayerClass, Double> BASIC_ATTACK_MIN_COOLDOWNS = new EnumMap<>(PlayerClass.class);
    private static final EnumSet<Material> DUNGEON_FLOWERS = EnumSet.of(
            Material.POPPY,
            Material.DANDELION,
            Material.BLUE_ORCHID,
            Material.ALLIUM
    );
    private static final String ATTACK_COOLDOWN_KEY = "basic_attack";
    private static final int SWING_LOCK_AMPLIFIER = 4;
    private static final int MAX_HASTE_AMPLIFIER = 3;
    private static final int MIN_SWING_TICKS = 6;
    private static final long NPC_INTERACT_GRACE_MS = 250L;
    static {
        for (PlayerClass pc : PlayerClass.values()) {
            if (ClassUtil.isArcherFamily(pc)) {
                BOW_CLASSES.add(pc);
            }
        }

        BASIC_ATTACK_MIN_COOLDOWNS.put(PlayerClass.ARCHER, 1.0);

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
        MANUAL_TRIGGER_COOLDOWNS.put(PlayerClass.AWAKWARRIOR, Map.of(Trigger.RIGHT, 2.0));

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
        t.left = List.of("sorcery_combo");
        t.right = List.of("teleport_strike");
        t.leftSneak = List.of("hailpiercer");
        t.rightSneak = List.of("meteor_of_doom");
        t.sneakStart = List.of("blazing_barrage");
        t.sneakPrep = List.of("Mana_Barrier");
        MAP.put(PlayerClass.AWAKMAGE, t);

        // Archmage class
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

        HOLD_MAP.put(PlayerClass.WITCH, new HoldConfig(
                0L,
                1L,
                List.of("mf_class_witch_holdshift_cruibile_count", "mf_class_witch_holdshift"),
                -1
        ));
        HOLD_MAP.put(PlayerClass.AWAKMAGE, new HoldConfig(
                0L,
                1L,
                List.of("Cryo_Prison"),
                3
        ));
    }

    private void cancelHoldTask(UUID id) {
        BukkitTask task = holdTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        holdRuns.remove(id);
    }

    private void cast(Player player, List<String> combos, PlayerClass pc, Trigger trigger) {
        if (combos == null || combos.isEmpty()) return;
        if (Main.getInstance().getDialogManager().isDialogLockActive(player)) return;
        if (applyManualCooldown(player, pc, trigger, combos)) return;
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

    private boolean applyManualCooldown(Player player, PlayerClass pc, Trigger trigger, List<String> combos) {
        Map<Trigger, Double> map = MANUAL_TRIGGER_COOLDOWNS.get(pc);
        if (map == null) return false;
        Double seconds = map.get(trigger);
        if (seconds == null) return false;

        CooldownManager cd = CooldownManager.getInstance();
        UUID id = player.getUniqueId();
        String key = pc.name().toLowerCase() + ":" + trigger.name().toLowerCase();
        if (cd.isOnCooldown(id, key)) {
            long rem = cd.getRemainingTime(id, key);
            String label = combos.isEmpty() ? trigger.name() : combos.get(0);
            Spell spell = SpellManager.getInstance().getSpellById(pc.name().toLowerCase(), label);
            String display = spell != null ? spell.getDisplayName() : trigger.name();
            CooldownIndicatorManager.getInstance().show(player, display, rem, 0);
            return true;
        }
        cd.setCooldown(id, key, seconds);
        return false;
    }

    private PlayerClass getClass(Player player) {
        return StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
    }

    private void applySwingCooldownLock(Player player, long remainingMs) {
        int lockTicks = msToTicks(remainingMs);
        PotionEffectUtil.applyHiddenEffect(player, PotionEffectType.MINING_FATIGUE, lockTicks, SWING_LOCK_AMPLIFIER);
        PotionEffectUtil.removeEffect(player, PotionEffectType.HASTE);
    }

    private void applySwingReadyBoost(Player player, double attackSpeed, int cooldownTicks) {
        PotionEffectUtil.removeEffect(player, PotionEffectType.MINING_FATIGUE);
        int hasteTicks = Math.max(4, Math.min(10, cooldownTicks / 4));
        int hasteAmplifier = Math.max(0, Math.min(MAX_HASTE_AMPLIFIER, (int) Math.floor(attackSpeed * 2) - 1));
        PotionEffectUtil.applyHiddenEffect(player, PotionEffectType.HASTE, hasteTicks, hasteAmplifier);
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (!player.isOnline()) {
                return;
            }
            PotionEffectUtil.applyHiddenEffect(player, PotionEffectType.MINING_FATIGUE, cooldownTicks, SWING_LOCK_AMPLIFIER);
        });
    }

    private int msToTicks(long ms) {
        return (int) Math.max(1, Math.ceil(ms / 50.0));
    }

    private boolean tryBasicAttack(Player player, PlayerClass pc) {
        if (player.getAttackCooldown() < 1.0) return false;
        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        double cooldown = 1.0 / ps.attackSpeed;
        double minSwingSeconds = MIN_SWING_TICKS / 20.0;
        Double min = BASIC_ATTACK_MIN_COOLDOWNS.get(pc);
        if (min != null) {
            cooldown = Math.max(cooldown, min);
        }
        cooldown = Math.max(cooldown, minSwingSeconds);
        CooldownManager cd = CooldownManager.getInstance();
        UUID id = player.getUniqueId();
        if (cd.isOnCooldown(id, ATTACK_COOLDOWN_KEY)) {
            applySwingCooldownLock(player, cd.getRemainingTime(id, ATTACK_COOLDOWN_KEY));
            return false;
        }
        cd.setCooldown(id, ATTACK_COOLDOWN_KEY, cooldown);
        int cooldownTicks = msToTicks((long) (cooldown * 1000.0));
        applySwingReadyBoost(player, ps.attackSpeed, cooldownTicks);
        player.resetCooldown();
        return true;
    }

    private boolean consumeRecentNpcInteraction(Player player) {
        Long last = recentNpcInteractions.remove(player.getUniqueId());
        if (last == null) return false;
        return System.currentTimeMillis() - last <= NPC_INTERACT_GRACE_MS;
    }

    private boolean isDungeonFlowerBlock(Block block) {
        if (block == null) return false;
        if (!DUNGEON_FLOWERS.contains(block.getType())) return false;
        return Main.getInstance().getDungeonManager().isInstanceWorld(block.getWorld());
    }

    private boolean isLootChestBlock(Block block) {
        if (block == null) return false;
        FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(block);
        if (mechanic == null) return false;
        return mechanic.getItemID().equalsIgnoreCase(Main.getInstance().getLootChestManager().getCrateModelId());
    }

    private boolean shouldSkipRightClickCast(PlayerInteractEvent event) {
        if (consumeRecentNpcInteraction(event.getPlayer())) return true;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        Block clicked = event.getClickedBlock();
        if (clicked == null) return false;
        return isDungeonFlowerBlock(clicked) || isLootChestBlock(clicked);
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {
        Player p = event.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD) {
            return;
        }
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;

        if (!BOW_CLASSES.contains(pc)) {
            if (!tryBasicAttack(p, pc)) {
                event.setCancelled(true);
                return;
            }
        }

        if (p.isSneaking()) {
            cast(p, tr.leftSneak, pc, Trigger.LEFT_SNEAK);
            pendingSneak.remove(p.getUniqueId());
        } else {
            cast(p, tr.left, pc, Trigger.LEFT);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        ItemStack held = event.getItem();
        if (held == null || held.getType() == Material.AIR) {
            held = p.getInventory().getItemInMainHand();
        }
        boolean weapon = WeaponType.matchType(held) != null;
        if (!weapon) return;
        if (shouldSkipRightClickCast(event)) return;

        event.setCancelled(true);

        if (p.isSneaking()) {
            cast(p, tr.rightSneak, pc, Trigger.RIGHT_SNEAK);
            pendingSneak.remove(p.getUniqueId());
            return;
        }

        if (BOW_CLASSES.contains(pc) && !tryBasicAttack(p, pc)) return;

        cast(p, tr.right, pc, Trigger.RIGHT);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
        recentNpcInteractions.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player p = event.getPlayer();
        PlayerClass pc = getClass(p);
        Triggers tr = MAP.get(pc);
        if (tr == null) return;
        if (event.isSneaking()) {
            UUID id = p.getUniqueId();
            cancelHoldTask(id);
            // cancel any pending unsneak cast if player crouches again quickly
            BukkitTask pending = pendingUnsneak.remove(id);
            if (pending != null) {
                pending.cancel();
                // make sure any preparatory aura from the first crouch still fires
                if (!tr.sneakStart.isEmpty()) {
                    cast(p, tr.sneakStart, pc, Trigger.SNEAK_START);
                }
            }

            boolean doubleSneak = false;
            if (pc != PlayerClass.WITCH && !tr.sneakEnd.isEmpty()) {
                long now = System.currentTimeMillis();
                Long last = lastUnsneak.get(id);
                if (last != null && now - last <= 500) {
                    cast(p, tr.sneakEnd, pc, Trigger.SNEAK_END);
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
            }
            HoldConfig hold = HOLD_MAP.get(pc);
            if (hold != null) {
                holdRuns.put(id, 0);
                final UUID playerId = id;
                BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                        Main.getPlugin(),
                        () -> {
                            Player target = Bukkit.getPlayer(playerId);
                            if (target == null || !target.isOnline()) {
                                cancelHoldTask(playerId);
                                return;
                            }

                            int previousRuns = holdRuns.getOrDefault(playerId, 0);
                            if (!target.isSneaking()) {
                                if (previousRuns > 0) {
                                    cancelHoldTask(playerId);
                                }
                                return;
                            }

                            int runs = holdRuns.compute(playerId, (uuid, count) -> count == null ? 1 : count + 1);
                            for (String skill : hold.skills) {
                                MythicBukkit.inst().getAPIHelper().castSkill(target, skill);
                            }
                            if (hold.consumeAfterRuns > 0 && runs >= hold.consumeAfterRuns) {
                                pendingSneak.remove(playerId);
                            }
                        },
                        hold.delay,
                        hold.period
                );
                BukkitTask previous = holdTasks.put(playerId, task);
                if (previous != null) {
                    previous.cancel();
                }
            }
        } else {
            UUID id = p.getUniqueId();
            cancelHoldTask(id);
            boolean castSneak = pendingSneak.remove(id);
            if (castSneak) {
                BukkitTask old = pendingUnsneak.remove(id);
                if (old != null) old.cancel();
                BukkitTask task = Bukkit.getScheduler().runTaskLater(
                        Main.getPlugin(),
                        () -> {
                            if (p.isOnline() && !p.isSneaking()) {
                                cast(p, tr.sneakStart, pc, Trigger.SNEAK_START);
                            }
                            pendingUnsneak.remove(id);
                        },
                        4L
                );
                pendingUnsneak.put(id, task);
            } else if (pc == PlayerClass.WITCH) {
                cast(p, tr.sneakEnd, pc, Trigger.SNEAK_END);
            }

            lastUnsneak.put(id, System.currentTimeMillis());
        }
    }
}
