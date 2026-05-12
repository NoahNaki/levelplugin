package me.nakilex.levelplugin.spells.summon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.CutsceneManager;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.spells.deck.SpellCardDefinition;
import me.nakilex.levelplugin.spells.deck.SpellDeckManager;
import me.nakilex.levelplugin.spells.deck.SpellDeckManager.SpellPullEntry;
import me.nakilex.levelplugin.spells.deck.SpellDeckManager.SpellPullResult;
import me.nakilex.levelplugin.spells.gui.SpellSummonGUI;
import me.nakilex.levelplugin.utils.BetterHudUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GlowUtil;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
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
import java.util.Set;
import java.util.UUID;

public class SpellSummonManager implements Listener {
    private static final String CUTSCENE_ID = "spell_pull";
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
    private static final double REVEAL_NAME_VERTICAL_OFFSET = 1.0;
    private static final int SINGLE_PULL_COST = 500;
    private static final int TEN_PULL_COST = 4500;

    private final Main plugin;
    private final SpellDeckManager deckManager;
    private final CutsceneManager cutsceneManager;
    private final Map<UUID, SummonSession> sessions = new HashMap<>();
    private SpellSummonGUI summonGUI;

    public SpellSummonManager(Main plugin, SpellDeckManager deckManager, CutsceneManager cutsceneManager) {
        this.plugin = plugin;
        this.deckManager = deckManager;
        this.cutsceneManager = cutsceneManager;
    }

    public void setSummonGUI(SpellSummonGUI summonGUI) {
        this.summonGUI = summonGUI;
    }

    public int getSummonCost(int amount) {
        return summonCostForAmount(amount);
    }

    public boolean shouldKeepSummonGuiOpen() {
        return false;
    }

    public void startSummon(Player player, int amount) {
        if (player == null || deckManager == null || cutsceneManager == null) {
            return;
        }
        if (amount <= 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Invalid summon amount.");
            return;
        }
        if (sessions.containsKey(player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You are already summoning spells.");
            return;
        }
        if (cutsceneManager.isInCutscene(player)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Finish your current cutscene first.");
            return;
        }
        if (!cutsceneManager.listCutscenes().contains(CUTSCENE_ID)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Spell summon cutscene is unavailable.");
            return;
        }

        int summonCost = getSummonCost(amount);
        if (!chargeSummonCost(player, summonCost)) {
            return;
        }

        SpellPullResult pulls = deckManager.pull(player, amount);
        if (pulls.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No spell cards available to pull.");
            refundSummonCost(player, summonCost);
            return;
        }

        Location returnLocation = player.getLocation().clone();
        SummonSession session = new SummonSession(returnLocation, pulls, summonCost);
        sessions.put(player.getUniqueId(), session);

        applyBlindnessTransition(player, CUTSCENE_TRANSITION_TICKS);
        playSound(player, "minecraft:block.portal.trigger", 0.9f, 1.4f);
        cutsceneManager.playCutscene(player, CUTSCENE_ID);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Crouch to skip animation.");
        Bukkit.getScheduler().runTaskLater(plugin, () -> prepareCutsceneView(player, session), 1L);

        for (int i = 0; i < pulls.pulls().size(); i++) {
            SpellPullEntry entry = pulls.pulls().get(i);
            scheduleSpawn(player, session, entry, SPAWN_DELAY_TICKS + i * SPAWN_INTERVAL_TICKS, i);
        }

        int totalTicks = calculateTotalDuration(session);
        session.tasks.add(Bukkit.getScheduler().runTaskLater(plugin,
                () -> tryFinishSummon(player, session), totalTicks));
        session.tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                () -> checkCutsceneState(player), 20L, 20L));
    }

    private void prepareCutsceneView(Player player, SummonSession session) {
        SummonSession active = sessions.get(player.getUniqueId());
        if (active != session) {
            return;
        }
        active.updateOrientationFromPlayer(player);
        active.setFrozenLocation(player.getLocation());
        hideCutsceneScoreboard(player, active);
        hideOtherPlayersForCutscene(player, active);
        BetterHudUtil.removeHud(player);
        startVisibilityEnforcementTask(player, active);
    }

    private void scheduleSpawn(Player player, SummonSession session, SpellPullEntry entry, int delay, int index) {
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
            Item item = spawnSummonItem(session.center, entry.card());
            item.setCustomNameVisible(false);
            session.spawned.add(item);
            session.entries.add(new SummonEntry(item, entry.card(), index));
            applyVisibility(player, item);
            startSpinTask(player, session);
            playSound(player, SPAWN_SOUND, SPAWN_VOLUME, SPAWN_PITCH);
        }, delay);
        session.tasks.add(task);
    }

    private Item spawnSummonItem(Location location, SpellCardDefinition card) {
        ItemStack stack = createSummonPlaceholderStack();
        World world = location.getWorld();
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        Item item = world.dropItem(location.clone(), stack);
        item.setGravity(false);
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setInvulnerable(true);
        item.setSilent(true);
        item.setCustomName("§dSealed Spell Card");
        item.setCustomNameVisible(true);
        return item;
    }

    private ItemStack createSummonPlaceholderStack() {
        ItemStack stack = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dSealed Spell Card");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createRevealedSummonStack(SpellCardDefinition card) {
        ItemStack stack = new ItemStack(card.rarity().displayMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(formatDisplayName(card));
            stack.setItemMeta(meta);
        }
        ItemUtil.applyRarityTooltipStyle(stack, card.rarity().itemRarity());
        return stack;
    }

    private void revealSpellItem(SummonEntry entry) {
        if (entry == null || entry.item().isDead()) {
            return;
        }
        entry.item().setItemStack(createRevealedSummonStack(entry.card()));
        entry.item().setCustomName(formatDisplayName(entry.card()));
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
                updateRingPositions(player, session, eased * (Math.PI * 2.0 * SPIN_TURNS));
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
        for (SummonEntry entry : session.entries) {
            if (entry.item().isDead()) {
                continue;
            }
            double offset = count == 1 ? 0.0 : (Math.PI * 2.0 * entry.index() / count);
            double theta = baseAngle + offset;
            Location target = session.center.clone()
                    .add(session.right.clone().multiply(Math.cos(theta) * radius))
                    .add(session.up.clone().multiply(Math.sin(theta) * radius));
            applySmoothVelocity(entry.item(), target);
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
                if (!player.isOnline() || entry.item().isDead()) {
                    return;
                }
                revealSpellItem(entry);
                entry.item().setCustomNameVisible(true);
                showRevealName(entry);
                applyGlow(player, entry.item(), entry.card());
                spawnRevealParticles(player, entry.item().getLocation(), entry.card().rarity().itemRarity());
                playRevealSound(player, entry.card().rarity().itemRarity());
                session.revealsDone++;
                if (session.revealsDone >= session.totalReveals) {
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> tryFinishSummon(player, session), END_BUFFER_TICKS);
                }
            }, delay);
            session.tasks.add(task);
        }
    }

    private void showRevealName(SummonEntry entry) {
        if (entry == null || entry.item().isDead() || entry.nameDisplay() != null) {
            return;
        }
        Location labelLoc = entry.item().getLocation().clone().add(0, REVEAL_NAME_VERTICAL_OFFSET, 0);
        TextDisplay display = labelLoc.getWorld().spawn(labelLoc, TextDisplay.class);
        display.setBillboard(Display.Billboard.CENTER);
        display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        display.setShadowed(false);
        display.setText(formatDisplayName(entry.card()));
        entry.setNameDisplay(display);
    }

    private void applyGlow(Player player, Item item, SpellCardDefinition card) {
        if (item == null || item.isDead() || card == null) {
            return;
        }
        Scoreboard board = player.getScoreboard();
        if (board == null) {
            var sbManager = plugin.getScoreboardManager();
            board = sbManager != null ? sbManager.getBoard(player) : null;
        }
        GlowUtil.applyGlowWithColor(item, card.rarity().itemRarity().getColor(), board);
    }

    private int calculateTotalDuration(SummonSession session) {
        int count = session.totalPulls;
        return SPAWN_DELAY_TICKS + session.spinTicks + REVEAL_START_DELAY_TICKS
                + count * REVEAL_INTERVAL_TICKS + END_BUFFER_TICKS;
    }

    private boolean chargeSummonCost(Player player, int cost) {
        if (player == null || cost <= 0) {
            return true;
        }
        if (plugin.getGemsManager() == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Gem economy is unavailable right now.");
            return false;
        }
        int gems = plugin.getGemsManager().getTotalUnits(player);
        if (gems < cost) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Not enough gems. Need §d" + cost + " <glyph:purple_orb_icon>§c.");
            return false;
        }
        try {
            plugin.getGemsManager().deductUnits(player, cost);
            return true;
        } catch (IllegalArgumentException ex) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Not enough gems. Need §d" + cost + " <glyph:purple_orb_icon>§c.");
            return false;
        }
    }

    private void refundSummonCost(Player player, int cost) {
        if (player == null || cost <= 0 || plugin.getGemsManager() == null) {
            return;
        }
        plugin.getGemsManager().addUnits(player, cost);
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
            revealSpellItem(entry);
            entry.item().setCustomNameVisible(true);
            showRevealName(entry);
            applyGlow(player, entry.item(), entry.card());
            spawnRevealParticles(player, entry.item().getLocation(), entry.card().rarity().itemRarity());
        }
        session.revealsDone = session.totalReveals;
    }

    private void tryFinishSummon(Player player, SummonSession session) {
        if (player == null || session == null || sessions.get(player.getUniqueId()) != session) {
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
            TextDisplay revealName = entry.nameDisplay();
            if (revealName != null && !revealName.isDead()) {
                revealName.remove();
            }
        }
        showOtherPlayersAfterCutscene(player, session);
        restoreCutsceneScoreboard(player, session);
        BetterHudUtil.addHud(player);
        me.nakilex.levelplugin.spells.input.SpellInputHudManager.getInstance().sync(player);
        if (teleport) {
            if (session.returnLocation != null) {
                player.teleport(session.returnLocation);
            }
            playSound(player, "minecraft:entity.player.levelup", 0.9f, 1.3f);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Spell summons complete. Spent §d" + session.summonCost + " <glyph:purple_orb_icon>§a.");
            SpellPullSummaryUtil.sendPullResult(player, session.pulls);
            if (summonGUI != null && player.isOnline()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> summonGUI.open(player), 2L);
            }
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

    private void hideCutsceneScoreboard(Player player, SummonSession session) {
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
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }
            viewer.hideEntity(plugin, other);
            session.hiddenPlayers.add(other.getUniqueId());
        }
    }

    private void startVisibilityEnforcementTask(Player viewer, SummonSession session) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            SummonSession active = sessions.get(viewer.getUniqueId());
            if (active != session || !viewer.isOnline()) {
                return;
            }
            hideOtherPlayersForCutscene(viewer, session);
        }, 10L, 10L);
        session.tasks.add(task);
    }

    private void showOtherPlayersAfterCutscene(Player viewer, SummonSession session) {
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
        if (sessions.containsKey(player.getUniqueId())) {
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
        if (rarity == ItemRarity.LEGENDARY || rarity == ItemRarity.MYTHIC || rarity == ItemRarity.FABLED) {
            count = 28;
            spread = 0.35;
            speed = 0.04;
        }
        player.spawnParticle(Particle.END_ROD, location, count, spread, spread, spread, speed);
        player.spawnParticle(Particle.ENCHANT, location, Math.max(8, count / 2), spread, spread, spread, 0.15);
    }

    private void playSound(Player player, String sound, float volume, float pitch) {
        if (player != null && sound != null && !sound.isBlank()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    private void applySmoothVelocity(Item item, Location target) {
        if (item == null || target == null) {
            return;
        }
        item.setVelocity(target.toVector().subtract(item.getLocation().toVector()));
    }

    private void applyBlindnessTransition(Player player, int ticks) {
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.BLINDNESS, Math.max(1, ticks), 0, false, false, false));
    }

    private String formatDisplayName(SpellCardDefinition card) {
        return card.rarity().color() + card.displayName();
    }

    public static int summonCostForAmount(int amount) {
        return amount >= 10 ? TEN_PULL_COST : SINGLE_PULL_COST;
    }

    private static class SummonSession {
        private final Location returnLocation;
        private final SpellPullResult pulls;
        private final List<BukkitTask> tasks = new ArrayList<>();
        private final List<Item> spawned = new ArrayList<>();
        private final List<SummonEntry> entries = new ArrayList<>();
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

        private SummonSession(Location returnLocation, SpellPullResult pulls, int summonCost) {
            this.returnLocation = returnLocation;
            this.pulls = pulls;
            this.totalPulls = Math.max(1, pulls.pulls().size());
            this.totalReveals = this.totalPulls;
            this.spinTicks = SPIN_TICKS + (this.totalPulls - 1) * SPAWN_INTERVAL_TICKS;
            this.summonCost = Math.max(0, summonCost);
        }

        private void updateOrientationFromPlayer(Player player) {
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
            base.add(forward.clone().multiply(BASE_OFFSET));
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
        private final SpellCardDefinition card;
        private final int index;
        private TextDisplay nameDisplay;

        private SummonEntry(Item item, SpellCardDefinition card, int index) {
            this.item = item;
            this.card = card;
            this.index = index;
        }

        private Item item() { return item; }
        private SpellCardDefinition card() { return card; }
        private int index() { return index; }
        private TextDisplay nameDisplay() { return nameDisplay; }
        private void setNameDisplay(TextDisplay nameDisplay) { this.nameDisplay = nameDisplay; }
    }
}
