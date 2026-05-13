package me.nakilex.levelplugin.pet.summon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.CutsceneManager;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.PetManager.PetPullDetailed;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.pet.PetManager.PetPullEntry;
import me.nakilex.levelplugin.pet.gui.PetSummonGUI;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import me.nakilex.levelplugin.pet.utils.PetDisplayUtil;
import me.nakilex.levelplugin.pet.utils.PetPullSummaryUtil;
import me.nakilex.levelplugin.pet.utils.PetFeedbackUtil;
import me.nakilex.levelplugin.utils.BetterHudUtil;
import me.nakilex.levelplugin.utils.GlowUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.*;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class PetSummonManager implements Listener {
    private static final String CUTSCENE_ID = "pet_pull";
    private static final int SPAWN_DELAY_TICKS = 20;
    private static final int SPAWN_INTERVAL_TICKS = 10;
    private static final int SPIN_TICKS = 120;
    private static final int REVEAL_START_DELAY_TICKS = 20;
    private static final int REVEAL_INTERVAL_TICKS = 14;
    private static final int END_BUFFER_TICKS = 20;
    private static final int CUTSCENE_TRANSITION_TICKS = 24;
    private static final double BASE_OFFSET = 5.5;
    private static final double RING_RADIUS = 3.8;
    private static final double SPIN_TURNS = 3.5;
    private static final String SPAWN_SOUND = "minecraft:entity.experience_orb.pickup";
    private static final String REVEAL_SOUND = "minecraft:block.note_block.chime";
    private static final String ORBIT_SOUND = "minecraft:block.amethyst_block.chime";
    private static final String LEGENDARY_REVEAL_SOUND = "minecraft:ui.toast.challenge_complete";
    private static final float SPAWN_VOLUME = 0.7f;
    private static final float SPAWN_PITCH = 1.3f;
    private static final float REVEAL_VOLUME = 0.9f;
    private static final float REVEAL_PITCH = 1.1f;
    private static final float LEGENDARY_REVEAL_VOLUME = 1.25f;
    private static final float LEGENDARY_REVEAL_PITCH = 0.9f;
    private static final float ORBIT_VOLUME = 0.4f;
    private static final float ORBIT_PITCH = 1.6f;
    private static final double SUMMON_X = 234;
    private static final double SUMMON_Y = 177;
    private static final double SUMMON_Z = -203;
    private static final int SINGLE_PULL_COST = 100;
    private static final int TEN_PULL_COST = 900;
    private static final double REVEAL_MODEL_VERTICAL_OFFSET = -0.9;
    private static final double REVEAL_NAME_VERTICAL_OFFSET = 1.55;

    private final Main plugin;
    private final PetManager petManager;
    private final CutsceneManager cutsceneManager;
    private final Map<UUID, SummonSession> sessions = new HashMap<>();
    private PetSummonGUI summonGUI;

    public PetSummonManager(Main plugin, PetManager petManager, CutsceneManager cutsceneManager) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.cutsceneManager = cutsceneManager;
    }

    public void setSummonGUI(PetSummonGUI summonGUI) {
        this.summonGUI = summonGUI;
    }

    public int getPityPullsSinceLegendary(UUID playerId) {
        return petManager == null ? 0 : petManager.getPityPullsSinceLegendary(playerId);
    }

    public int getPityThreshold() {
        return petManager == null ? 60 : petManager.getPityThreshold();
    }

    public int getPityThreshold(UUID playerId) {
        return petManager == null ? 60 : petManager.getEffectivePityThreshold(playerId);
    }

    public int getPullsUntilPityLegendary(UUID playerId) {
        return petManager == null ? 60 : petManager.getPullsUntilPityLegendary(playerId);
    }

    public int getSummonCost(UUID playerId, int amount) {
        int base = summonCostForAmount(amount);
        if (petManager == null) {
            return base;
        }
        return petManager.applyActiveEffectReduction(playerId, PetEffectType.GACHA_GEM_COST_REDUCTION, base, 0.80);
    }

    public Map<ItemRarity, Double> getGachaRates() {
        return petManager == null ? Map.of() : petManager.getGachaRates();
    }

    public Map<ItemRarity, Double> getGachaRates(UUID playerId) {
        return petManager == null ? Map.of() : petManager.getGachaRates(playerId);
    }

    public int getBannerLevel(UUID playerId) {
        return petManager == null ? 1 : petManager.getBannerLevel(playerId);
    }

    public int getBannerLevelProgress(UUID playerId) {
        return petManager == null ? 0 : petManager.getBannerLevelProgress(playerId);
    }

    public int getBannerLevelRequirement(UUID playerId) {
        return petManager == null ? 0 : petManager.getBannerLevelRequirement(playerId);
    }

    public int getMaxBannerLevel() {
        return petManager == null ? 10 : petManager.getMaxBannerLevel();
    }

    public List<ItemRarity> getGachaRarities() {
        return petManager == null ? List.of() : petManager.getGachaRarities();
    }

    public boolean shouldKeepSummonGuiOpen(UUID playerId) {
        if (playerId == null || petManager == null) {
            return false;
        }
        return petManager.getProfile(playerId).autoSkipSummonAnimation();
    }


    public void startSummon(Player player, int amount) {
        if (player == null || petManager == null || cutsceneManager == null) {
            return;
        }
        if (amount <= 0) {
            PetChatUtil.send(player, "Invalid summon amount.");
            return;
        }
        if (sessions.containsKey(player.getUniqueId())) {
            PetChatUtil.send(player, "You are already summoning pets.");
            return;
        }
        if (cutsceneManager.isInCutscene(player)) {
            PetChatUtil.send(player, "Finish your current cutscene first.");
            return;
        }
        if (!cutsceneManager.listCutscenes().contains(CUTSCENE_ID)) {
            PetChatUtil.send(player, "Pet summon cutscene is unavailable.");
            return;
        }

        int summonCost = getSummonCost(player.getUniqueId(), amount);
        if (!chargeSummonCost(player, summonCost)) {
            return;
        }

        PetPullDetailed detailed = petManager.pullPetsDetailed(player, amount);
        if (detailed.kept().isEmpty() && detailed.discarded().isEmpty()) {
            PetChatUtil.send(player, "No pets available to pull.");
            refundSummonCost(player, summonCost);
            return;
        }

        if (petManager.getProfile(player.getUniqueId()).autoSkipSummonAnimation()) {
            finishInstantSummon(player, detailed, summonCost);
            return;
        }

        Location returnLocation = player.getLocation().clone();
        petManager.setPendingSummonReturn(player.getUniqueId(), returnLocation);

        SummonSession session = new SummonSession(returnLocation, detailed, summonCost);
        sessions.put(player.getUniqueId(), session);

        PetFeedbackUtil.applyBlindnessTransition(player, CUTSCENE_TRANSITION_TICKS);
        PetFeedbackUtil.playSummonTransition(player);
        cutsceneManager.playCutscene(player, CUTSCENE_ID);
        PetChatUtil.send(player, "Crouch to skip animation.");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            SummonSession active = sessions.get(player.getUniqueId());
            if (active != null) {
                active.updateOrientationFromPlayer(player);
                active.setFrozenLocation(player.getLocation());
                hideCutsceneScoreboard(player, active);
                hideOtherPlayersForCutscene(player, active);
                hidePetsForCutscene(player);
                BetterHudUtil.removeHud(player);
                startVisibilityEnforcementTask(player, active);
            }
        }, 1L);

        for (int i = 0; i < detailed.pulls().size(); i++) {
            PetPullEntry entry = detailed.pulls().get(i);
            int spawnDelay = SPAWN_DELAY_TICKS + i * SPAWN_INTERVAL_TICKS;
            scheduleSpawn(player, session, entry, spawnDelay, i);
        }

        int totalTicks = calculateTotalDuration(session);
        session.tasks.add(Bukkit.getScheduler().runTaskLater(plugin,
                () -> tryFinishSummon(player, session), totalTicks));
        session.tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                () -> checkCutsceneState(player), 20L, 20L));
    }

    private void scheduleSpawn(Player player,
                               SummonSession session,
                               PetPullEntry entry,
                               int delay,
                               int index) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player == null || !player.isOnline()) {
                return;
            }
            if (session.center == null) {
                session.updateOrientationFromPlayer(player);
            }
            if (session.center == null) {
                return;
            }
            Item item = spawnSummonItem(session.center, entry.definition());
            item.setCustomNameVisible(false);
            session.spawned.add(item);
            Interaction modelAnchor = spawnModelAnchor(item.getLocation());
            session.entries.add(new SummonEntry(item, modelAnchor, entry.definition(), index));
            applyVisibility(player, modelAnchor);
            applyVisibility(player, item);
            startSpinTask(player, session);
            playSound(player, SPAWN_SOUND, SPAWN_VOLUME, SPAWN_PITCH);
        }, delay);
        session.tasks.add(task);
    }

    private Item spawnSummonItem(Location location, PetDefinition definition) {
        ItemStack stack = createSummonStack(definition);
        World world = location.getWorld();
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        Location spawn = location.clone();
        Item item = world.dropItem(spawn, stack);
        item.setGravity(false);
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setInvulnerable(true);
        item.setSilent(true);
        item.setCustomName(PetDisplayUtil.formatDisplayName(definition));
        item.setCustomNameVisible(true);
        return item;
    }

    private Interaction spawnModelAnchor(Location location) {
        World world = location == null ? null : location.getWorld();
        if (world == null) {
            return null;
        }
        Location spawn = location.clone().add(0, REVEAL_MODEL_VERTICAL_OFFSET, 0);
        return world.spawn(spawn, Interaction.class, inter -> {
            inter.setInteractionWidth(0.8f);
            inter.setInteractionHeight(0.8f);
            inter.setGravity(false);
            inter.setInvulnerable(true);
            inter.setPersistent(false);
        });
    }

    private void revealModelForEntry(SummonEntry entry) {
        if (entry == null || entry.modelAnchor() == null || entry.modelAnchor().isDead()) {
            return;
        }
        if (entry.definition().modelIds().isEmpty()) {
            return;
        }
        ModelEngineUtil.applyModels(entry.modelAnchor(), entry.definition().modelIds(), plugin);
    }

    private void showRevealName(SummonEntry entry) {
        if (entry == null || entry.item().isDead()) {
            return;
        }
        if (entry.nameDisplay() != null && !entry.nameDisplay().isDead()) {
            return;
        }
        Location base = entry.modelAnchor() != null && !entry.modelAnchor().isDead()
                ? entry.modelAnchor().getLocation()
                : entry.item().getLocation();
        Location labelLoc = base.clone().add(0, REVEAL_NAME_VERTICAL_OFFSET, 0);
        TextDisplay display = labelLoc.getWorld().spawn(labelLoc, TextDisplay.class);
        display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setShadowed(false);
        display.setText(PetDisplayUtil.formatDisplayName(entry.definition()));
        entry.setNameDisplay(display);
    }

    private void removeRevealPlaceholder(SummonEntry entry) {
        if (entry == null || entry.item().isDead()) {
            return;
        }
        entry.item().remove();
    }

    private ItemStack createSummonStack(PetDefinition definition) {
        ItemStack stack = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PetDisplayUtil.formatDisplayName(definition));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void applyGlow(Player player, Item item, PetDefinition definition) {
        if (item == null || item.isDead()) {
            return;
        }
        ItemRarity rarity = definition.rarity();
        if (rarity != null) {
            Scoreboard board = player.getScoreboard();
            if (board == null) {
                var sbManager = plugin.getScoreboardManager();
                board = sbManager != null ? sbManager.getBoard(player) : null;
            }
            GlowUtil.applyGlowWithColor(item, rarity.getColor(), board);
        }
    }

    private Location faceTarget(Location source, Location target) {
        if (source == null || target == null) {
            return source;
        }
        Vector direction = target.toVector().subtract(source.toVector());
        if (direction.lengthSquared() <= 0.0001) {
            return source;
        }
        source.setDirection(direction);
        return source;
    }

    private int calculateTotalDuration(SummonSession session) {
        int count = session.totalPulls;
        return SPAWN_DELAY_TICKS
                + session.spinTicks
                + REVEAL_START_DELAY_TICKS
                + count * REVEAL_INTERVAL_TICKS
                + END_BUFFER_TICKS;
    }

    private void startSpinTask(Player player, SummonSession session) {
        if (session.spinTask != null) {
            return;
        }
        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= session.spinTicks) {
                    cancel();
                    session.spinTask = null;
                    scheduleReveal(player, session);
                    return;
                }
                double progress = tick / (double) session.spinTicks;
                double eased = 1.0 - Math.pow(1.0 - progress, 3);
                session.spinProgress = eased;
                double baseAngle = eased * (Math.PI * 2.0 * SPIN_TURNS);
                updateRingPositions(player, session, baseAngle);
                if (tick % 12 == 0) {
                    playSound(player, ORBIT_SOUND, ORBIT_VOLUME, ORBIT_PITCH);
                }
                tick++;
            }
        };
        session.spinTask = runnable;
        session.tasks.add(runnable.runTaskTimer(plugin, 0L, 1L));
    }

    private void updateRingPositions(Player player, SummonSession session, double baseAngle) {
        int count = Math.max(1, session.totalPulls);
        if (session.center == null || session.right == null || session.up == null) {
            return;
        }
        double radius = RING_RADIUS * (0.25 + 0.75 * session.spinProgress);
        for (int i = 0; i < session.entries.size(); i++) {
            SummonEntry entry = session.entries.get(i);
            if (entry.item().isDead()) {
                continue;
            }
            double offset = count == 1 ? 0.0 : (Math.PI * 2.0 * entry.index() / count);
            double theta = baseAngle + offset;
            Vector right = session.right;
            Vector up = session.up;
            Location target = session.center.clone()
                    .add(right.clone().multiply(Math.cos(theta) * radius))
                    .add(up.clone().multiply(Math.sin(theta) * radius));
            applySmoothVelocity(entry.item(), target);
            if (entry.modelAnchor() != null && !entry.modelAnchor().isDead()) {
                Location modelLoc = target.clone().add(0, REVEAL_MODEL_VERTICAL_OFFSET, 0);
                modelLoc = faceTarget(modelLoc, player.getEyeLocation());
                entry.modelAnchor().teleport(modelLoc);
            }
            if (entry.nameDisplay() != null && !entry.nameDisplay().isDead()) {
                entry.nameDisplay().teleport(target.clone().add(0, REVEAL_NAME_VERTICAL_OFFSET, 0));
            }
            player.spawnParticle(Particle.PORTAL, target, 4, 0.08, 0.08, 0.08, 0.01);
        }
    }

    private void scheduleReveal(Player player, SummonSession session) {
        for (int i = 0; i < session.entries.size(); i++) {
            SummonEntry entry = session.entries.get(i);
            int delay = REVEAL_START_DELAY_TICKS + i * REVEAL_INTERVAL_TICKS;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (entry.item().isDead()) {
                    return;
                }
                entry.item().setCustomNameVisible(true);
                revealModelForEntry(entry);
                showRevealName(entry);
                applyGlow(player, entry.item(), entry.definition());
                removeRevealPlaceholder(entry);
                spawnRevealParticles(player, entry.item().getLocation(), entry.definition().rarity());
                playRevealSound(player, entry.definition().rarity());
                session.revealsDone++;
                if (session.revealsDone >= session.totalReveals) {
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> tryFinishSummon(player, session), END_BUFFER_TICKS);
                }
            }, delay);
            session.tasks.add(task);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        SummonSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        Location frozen = session.frozenLocation();
        if (frozen == null) {
            session.setFrozenLocation(event.getFrom());
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (to.getX() != frozen.getX() || to.getY() != frozen.getY() || to.getZ() != frozen.getZ()
                || to.getYaw() != frozen.getYaw() || to.getPitch() != frozen.getPitch()) {
            event.setTo(frozen.clone());
        }
    }

    private void applyVisibility(Player viewer, Entity entity) {
        if (viewer == null || entity == null || entity.isDead()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (entity.isDead()) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.equals(viewer)) {
                    player.showEntity(plugin, entity);
                } else {
                    player.hideEntity(plugin, entity);
                }
            }
        }, 1L);
    }


    private void playRevealSound(Player player, ItemRarity rarity) {
        if (rarity == ItemRarity.LEGENDARY || rarity == ItemRarity.MYTHIC || rarity == ItemRarity.FABLED) {
            playSound(player, LEGENDARY_REVEAL_SOUND, LEGENDARY_REVEAL_VOLUME, LEGENDARY_REVEAL_PITCH);
            return;
        }
        playSound(player, REVEAL_SOUND, REVEAL_VOLUME, REVEAL_PITCH);
    }

    private void spawnRevealParticles(Player player, Location location, ItemRarity rarity) {
        if (player == null || location == null) {
            return;
        }
        int count = 10;
        double spread = 0.2;
        double speed = 0.02;
        Particle accent = Particle.END_ROD;
        if (rarity == ItemRarity.LEGENDARY || rarity == ItemRarity.MYTHIC || rarity == ItemRarity.FABLED) {
            count = 28;
            spread = 0.35;
            speed = 0.04;
            accent = Particle.FIREWORK;
            player.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 8, 0.22, 0.22, 0.22, 0.01);
            player.spawnParticle(Particle.ENCHANT, location, 18, 0.45, 0.45, 0.45, 0.03);
        }
        player.spawnParticle(Particle.END_ROD, location, count, spread, spread, spread, speed);
        if (accent != Particle.END_ROD) {
            player.spawnParticle(accent, location, 16, 0.35, 0.35, 0.35, 0.02);
        }
    }

    public static int summonCostForAmount(int amount) {
        return amount >= 10 ? TEN_PULL_COST : SINGLE_PULL_COST;
    }

    private boolean chargeSummonCost(Player player, int cost) {
        if (player == null || cost <= 0) {
            return true;
        }
        if (plugin.getGemsManager() == null) {
            PetChatUtil.send(player, "Gem economy is unavailable right now.");
            return false;
        }
        int gems = plugin.getGemsManager().getTotalUnits(player);
        if (gems < cost) {
            PetChatUtil.send(player, "Not enough gems. Need §d" + cost + " <glyph:purple_orb_icon>§7.");
            return false;
        }
        try {
            plugin.getGemsManager().deductUnits(player, cost);
            return true;
        } catch (IllegalArgumentException ex) {
            PetChatUtil.send(player, "Not enough gems. Need §d" + cost + " <glyph:purple_orb_icon>§7.");
            return false;
        }
    }

    private void refundSummonCost(Player player, int cost) {
        if (player == null || cost <= 0 || plugin.getGemsManager() == null) {
            return;
        }
        plugin.getGemsManager().addUnits(player, cost);
    }

    private void finishInstantSummon(Player player, PetPullDetailed detailed, int cost) {
        if (player == null) {
            return;
        }
        PetFeedbackUtil.playSummonComplete(player);
        PetChatUtil.send(player, "Pet summon complete. Spent §d" + cost + " <glyph:purple_orb_icon>§7.");
        PetPullSummaryUtil.sendSummary(player, "Pulled", detailed.kept());
        PetPullSummaryUtil.sendSummary(player, "Auto-discarded", detailed.discarded());
    }

    private void playSound(Player player, String sound, float volume, float pitch) {
        if (player == null || sound == null || sound.isBlank()) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void applySmoothVelocity(Item item, Location target) {
        if (item == null || target == null) {
            return;
        }
        Location current = item.getLocation();
        Vector velocity = target.toVector().subtract(current.toVector());
        item.setVelocity(velocity);
    }

    private void checkCutsceneState(Player player) {
        if (player == null) {
            return;
        }
        SummonSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!player.isOnline()) {
            finishSession(player, false);
            return;
        }
        if (!cutsceneManager.isInCutscene(player)) {
            if (session.revealsDone < session.totalReveals) {
                forceRevealAll(player, session);
            }
            finishSession(player, true);
        }
    }

    private void forceRevealAll(Player player, SummonSession session) {
        for (SummonEntry entry : session.entries) {
            if (entry.item().isDead()) {
                continue;
            }
            entry.item().setCustomNameVisible(true);
            revealModelForEntry(entry);
            showRevealName(entry);
            applyGlow(player, entry.item(), entry.definition());
            removeRevealPlaceholder(entry);
            spawnRevealParticles(player, entry.item().getLocation(), entry.definition().rarity());
        }
        session.revealsDone = session.totalReveals;
    }

    private void tryFinishSummon(Player player, SummonSession session) {
        if (player == null || session == null) {
            return;
        }
        SummonSession active = sessions.get(player.getUniqueId());
        if (active != session) {
            return;
        }
        if (session.revealsDone < session.totalReveals) {
            return;
        }
        finishSession(player, true);
    }

    private void finishSession(Player player, boolean teleport) {
        if (player == null) {
            return;
        }
        SummonSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (cutsceneManager.isInCutscene(player)) {
            cutsceneManager.stopCutscene(player);
        }
        for (BukkitTask task : session.tasks) {
            task.cancel();
        }
        for (Item item : session.spawned) {
            if (item != null && !item.isDead()) {
                item.remove();
            }
        }
        for (SummonEntry entry : session.entries) {
            Interaction modelAnchor = entry.modelAnchor();
            if (modelAnchor != null && !modelAnchor.isDead()) {
                modelAnchor.remove();
            }
            TextDisplay revealName = entry.nameDisplay();
            if (revealName != null && !revealName.isDead()) {
                revealName.remove();
            }
        }
        showOtherPlayersAfterCutscene(player, session);
        restorePetsAfterCutscene(player);
        restoreCutsceneScoreboard(player, session);
        BetterHudUtil.addHud(player);
        me.nakilex.levelplugin.spells.input.SpellInputHudManager.getInstance().sync(player);
        if (teleport) {
            Location returnLocation = session.returnLocation;
            if (returnLocation != null) {
                player.teleport(returnLocation);
            }
            petManager.clearPendingSummonReturn(player.getUniqueId());
            PetFeedbackUtil.playSummonComplete(player);
            PetChatUtil.send(player, "Pet summons complete. Spent §d" + session.summonCost + " <glyph:purple_orb_icon>§7.");
            PetPullSummaryUtil.sendSummary(player, "Pulled", session.pulls.kept());
            PetPullSummaryUtil.sendSummary(player, "Auto-discarded", session.pulls.discarded());
            if (summonGUI != null && player.isOnline()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> summonGUI.open(player), 2L);
            }
        }
    }

    private void hideCutsceneScoreboard(Player player, SummonSession session) {
        if (player == null || session == null) {
            return;
        }
        var sbManager = plugin.getScoreboardManager();
        if (sbManager != null) {
            sbManager.setBoardSuppressed(player, true);
            return;
        }
        if (session.originalScoreboard == null) {
            session.originalScoreboard = player.getScoreboard();
        }
        var manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getNewScoreboard());
        }
    }

    private void restoreCutsceneScoreboard(Player player, SummonSession session) {
        if (player == null || session == null) {
            return;
        }
        var sbManager = plugin.getScoreboardManager();
        if (sbManager != null) {
            sbManager.setBoardSuppressed(player, false);
            return;
        }
        if (session.originalScoreboard != null) {
            player.setScoreboard(session.originalScoreboard);
            return;
        }
        var manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    private void hideOtherPlayersForCutscene(Player viewer, SummonSession session) {
        if (viewer == null || session == null) {
            return;
        }
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            viewer.hideEntity(plugin, other);
            session.hiddenPlayers.add(other.getUniqueId());
        }
    }


    private void hidePetsForCutscene(Player viewer) {
        if (petManager != null) {
            petManager.hidePetsForCutsceneViewer(viewer);
        }
    }

    private void restorePetsAfterCutscene(Player viewer) {
        if (petManager != null) {
            petManager.restorePetsForCutsceneViewer(viewer);
        }
    }

    private void startVisibilityEnforcementTask(Player viewer, SummonSession session) {
        if (viewer == null || session == null) {
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            SummonSession active = sessions.get(viewer.getUniqueId());
            if (active != session || !viewer.isOnline()) {
                return;
            }
            hideOtherPlayersForCutscene(viewer, session);
            hidePetsForCutscene(viewer);
        }, 10L, 10L);
        session.tasks.add(task);
    }

    private void showOtherPlayersAfterCutscene(Player viewer, SummonSession session) {
        if (viewer == null || session == null) {
            return;
        }
        for (UUID hiddenId : Set.copyOf(session.hiddenPlayers)) {
            Player hidden = Bukkit.getPlayer(hiddenId);
            if (hidden != null && hidden.isOnline()) {
                viewer.showEntity(plugin, hidden);
            }
        }
        session.hiddenPlayers.clear();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SummonSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            finishSession(player, false);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        SummonSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        forceRevealAll(player, session);
        finishSession(player, true);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (sessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location pending = petManager.getPendingSummonReturn(player.getUniqueId());
        if (pending == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(pending);
            petManager.clearPendingSummonReturn(player.getUniqueId());
            PetChatUtil.send(player, "Your pet summon was interrupted; you were returned safely.");
        });

        for (Map.Entry<UUID, SummonSession> activeEntry : sessions.entrySet()) {
            Player viewer = Bukkit.getPlayer(activeEntry.getKey());
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }
            viewer.hideEntity(plugin, player);
            activeEntry.getValue().hiddenPlayers.add(player.getUniqueId());
        }
    }

    private static class SummonSession {
        private final Location returnLocation;
        private final PetPullDetailed pulls;
        private final List<SummonEntry> entries = new ArrayList<>();
        private final List<Item> spawned = new ArrayList<>();
        private final List<BukkitTask> tasks = new ArrayList<>();
        private final int totalPulls;
        private final int totalReveals;
        private final int spinTicks;
        private final int summonCost;
        private final Set<UUID> hiddenPlayers = new HashSet<>();
        private Location center;
        private Vector right;
        private Vector up;
        private Location frozenLocation;
        private org.bukkit.scheduler.BukkitRunnable spinTask;
        private Scoreboard originalScoreboard;
        private double spinProgress;
        private int revealsDone;

        private SummonSession(Location returnLocation,
                              PetPullDetailed pulls,
                              int summonCost) {
            this.returnLocation = returnLocation;
            this.pulls = pulls;
            this.totalPulls = Math.max(1, pulls.pulls().size());
            this.totalReveals = this.totalPulls;
            this.center = null;
            this.right = null;
            this.up = null;
            this.spinTicks = SPIN_TICKS + (this.totalPulls - 1) * SPAWN_INTERVAL_TICKS;
            this.summonCost = Math.max(0, summonCost);
            this.frozenLocation = null;
            this.spinProgress = 0.0;
            this.revealsDone = 0;
            this.originalScoreboard = null;
        }

        private void updateOrientationFromPlayer(Player player) {
            if (player == null) {
                return;
            }
            Location base = player.getLocation().clone();
            Vector direction = player.getLocation().getDirection();
            if (direction.lengthSquared() < 0.001) {
                direction = new Vector(0, 0, 1);
            }
            Vector forward = direction.normalize();
            Vector up = new Vector(0, 1, 0);
            Vector right = forward.clone().crossProduct(up).normalize();
            if (right.lengthSquared() < 0.001) {
                right = new Vector(1, 0, 0);
            }
            Vector offset = forward.clone().multiply(BASE_OFFSET);
            base.add(offset);
            base.add(0, 1.2, 0);
            this.center = base;
            this.right = right;
            this.up = up;
        }

        private Location frozenLocation() {
            return frozenLocation == null ? null : frozenLocation.clone();
        }

        private void setFrozenLocation(Location location) {
            frozenLocation = location == null ? null : location.clone();
        }
    }

    private static class SummonEntry {
        private final Item item;
        private final Interaction modelAnchor;
        private final PetDefinition definition;
        private final int index;
        private TextDisplay nameDisplay;

        private SummonEntry(Item item, Interaction modelAnchor, PetDefinition definition, int index) {
            this.item = item;
            this.modelAnchor = modelAnchor;
            this.definition = definition;
            this.index = index;
        }

        private Item item() { return item; }
        private Interaction modelAnchor() { return modelAnchor; }
        private PetDefinition definition() { return definition; }
        private int index() { return index; }
        private TextDisplay nameDisplay() { return nameDisplay; }
        private void setNameDisplay(TextDisplay nameDisplay) { this.nameDisplay = nameDisplay; }
    }
}
