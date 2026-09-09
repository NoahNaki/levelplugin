package me.nakilex.levelplugin.pet.listeners;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.currency.enums.ReceiveCause;
import dev.drawethree.xprison.api.currency.event.PlayerCurrencyReceiveEvent;
import dev.drawethree.xprison.api.currency.model.XPrisonCurrency;
import dev.drawethree.xprison.api.mines.model.Mine;
import dev.drawethree.xprison.api.pickaxelevels.event.PlayerPickaxeExpGainEvent;
import dev.drawethree.xprison.api.shared.events.XPrisonBlockBreakEvent;
import dev.drawethree.xprison.api.shared.events.XPrisonBulkBlockBreakEvent;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pet.PetEffectDefinition;
import me.nakilex.levelplugin.pet.PetEffectType;
import me.nakilex.levelplugin.pet.PetInstance;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bridges summoned pet effects into X-Prison and owns the stateful prison-pet
 * mechanics. All bonus currency is paid through X-Prison's currency API.
 */
public final class PetXPrisonEffectListener implements Listener {

    private static final int PET_XP_BLOCK_INTERVAL = 20;
    private static final long CRYSTAL_RUSH_DURATION_MS = 18_000L;
    private static final long VAULT_HUNT_DURATION_MS = 25_000L;
    private static final Particle.DustOptions CRYSTAL_DUST =
            new Particle.DustOptions(Color.fromRGB(95, 235, 255), 1.25f);
    private static final Particle.DustOptions VAULT_DUST =
            new Particle.DustOptions(Color.fromRGB(255, 190, 45), 1.35f);
    private static final BlockFace[] EXPOSED_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final Main plugin;
    private final PetManager petManager;
    private final Map<UUID, MechanicState> states = new ConcurrentHashMap<>();
    private final Set<UUID> internalRewardPlayers = ConcurrentHashMap.newKeySet();

    public PetXPrisonEffectListener(Main plugin, PetManager petManager) {
        this.plugin = plugin;
        this.petManager = petManager;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickActiveMechanics, 10L, 10L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCurrencyReceive(PlayerCurrencyReceiveEvent event) {
        if (!isMiningReward(event.getCause()) || event.getCurrency() == null) {
            return;
        }

        Player player = onlinePlayer(event.getPlayer());
        if (player == null || internalRewardPlayers.contains(player.getUniqueId())) {
            return;
        }

        PetInstance pet = petManager.getActivePet(player.getUniqueId()).orElse(null);
        if (pet == null) {
            clearState(player.getUniqueId());
            return;
        }

        String currencyId = event.getCurrency().getName().toLowerCase(Locale.ROOT);
        BigDecimal amount = event.getAmountExact();
        if (amount == null || amount.signum() <= 0) {
            return;
        }

        PetEffectType flatEffect = switch (currencyId) {
            case "money" -> PetEffectType.XPRISON_MONEY_BOOST;
            case "tokens" -> PetEffectType.XPRISON_TOKEN_BOOST;
            case "gems" -> PetEffectType.XPRISON_GEM_BOOST;
            default -> null;
        };
        if (flatEffect != null) {
            double bonus = petManager.getActiveEffectValue(player.getUniqueId(), flatEffect);
            if (bonus > 0.0) {
                amount = amount.multiply(BigDecimal.valueOf(1.0 + bonus));
            }
        }

        MechanicState state = stateFor(player, pet);
        long now = System.currentTimeMillis();
        if (hasEffect(pet, PetEffectType.SEISMIC_OVERDRIVE) && state.overdriveUntilMs > now) {
            amount = amount.multiply(BigDecimal.valueOf(overdriveMultiplier(pet)));
        }

        event.setAmountExact(amount);
        recordCycleEarning(state, pet, currencyId, amount);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickaxeXpGain(PlayerPickaxeExpGainEvent event) {
        Player player = event.getPlayer();
        long amount = event.getAmount();
        if (player == null || amount <= 0) {
            return;
        }

        PetInstance pet = petManager.getActivePet(player.getUniqueId()).orElse(null);
        if (pet == null) {
            return;
        }

        double multiplier = 1.0;
        double flatBonus = petManager.getActiveEffectValue(
                player.getUniqueId(), PetEffectType.XPRISON_PICKAXE_XP_BOOST);
        if (flatBonus > 0.0) {
            multiplier *= 1.0 + flatBonus;
        }

        MechanicState state = stateFor(player, pet);
        if (hasEffect(pet, PetEffectType.SEISMIC_OVERDRIVE)
                && state.overdriveUntilMs > System.currentTimeMillis()) {
            multiplier *= overdriveMultiplier(pet);
        }
        if (multiplier <= 1.0) {
            return;
        }

        double boosted = amount * multiplier;
        long adjusted = boosted >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(boosted);
        event.setAmount(Math.max(amount, adjusted));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrisonBlockBreak(XPrisonBlockBreakEvent event) {
        Player player = event.getPlayer();
        List<Block> blocks = event.getBlocks();
        if (player == null || blocks == null || blocks.isEmpty()) {
            return;
        }
        handlePrisonBlocks(player, blocks.size(), blocks, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrisonBulkBlockBreak(XPrisonBulkBlockBreakEvent event) {
        Player player = event.getPlayer();
        long blocks = event.getTotalBlocks();
        if (player == null || blocks <= 0) {
            return;
        }
        handlePrisonBlocks(player, blocks, List.of(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearState(event.getPlayer().getUniqueId());
    }

    private void handlePrisonBlocks(Player player, long amount, List<Block> brokenBlocks,
                                    boolean bulkBreak) {
        PetInstance pet = petManager.getActivePet(player.getUniqueId()).orElse(null);
        if (pet == null) {
            clearState(player.getUniqueId());
            return;
        }

        MechanicState state = stateFor(player, pet);
        grantMiningPetXp(player, state, amount);

        if (!brokenBlocks.isEmpty()) {
            resolveBrokenTargets(player, state, brokenBlocks);
        } else if (bulkBreak) {
            resolveBulkTarget(player, state);
        }

        if (hasEffect(pet, PetEffectType.SOUL_HARVEST)) {
            chargeSoulHarvest(player, pet, state, amount);
        } else if (hasEffect(pet, PetEffectType.CRYSTAL_RUSH)) {
            chargeCrystalRush(player, pet, state, amount, brokenBlocks);
        } else if (hasEffect(pet, PetEffectType.SEISMIC_OVERDRIVE)) {
            chargeSeismicOverdrive(player, pet, state, amount);
        } else if (hasEffect(pet, PetEffectType.BURIED_VAULT)) {
            chargeBuriedVault(player, pet, state, amount, brokenBlocks);
        }
    }

    private void grantMiningPetXp(Player player, MechanicState state, long blocks) {
        long total = saturatingAdd(state.petXpBlockRemainder, blocks);
        int petXp = (int) Math.min(Integer.MAX_VALUE, total / PET_XP_BLOCK_INTERVAL);
        state.petXpBlockRemainder = total % PET_XP_BLOCK_INTERVAL;
        if (petXp > 0) {
            petManager.addActivePetXp(player.getUniqueId(), petXp);
        }
    }

    private void chargeSoulHarvest(Player player, PetInstance pet, MechanicState state, long blocks) {
        state.chargeBlocks = saturatingAdd(state.chargeBlocks, blocks);
        int threshold = soulHarvestThreshold(pet);
        if (state.chargeBlocks < threshold || state.earnings.isEmpty()) {
            return;
        }

        state.chargeBlocks %= threshold;
        double share = 0.03 + safeTier(pet) * 0.008 + 0.02 * levelRatio(pet);
        Map<String, BigDecimal> cycle = takeEarnings(state);
        List<String> rewards = new ArrayList<>();
        for (String currencyId : List.of("money", "tokens", "gems")) {
            BigDecimal earned = cycle.getOrDefault(currencyId, BigDecimal.ZERO);
            BigDecimal reward = rounded(earned.multiply(BigDecimal.valueOf(share)));
            if (awardCurrency(player, currencyId, reward)) {
                rewards.add(formatCurrency(currencyId, reward));
            }
        }

        Location fx = player.getLocation().add(0.0, 1.0, 0.0);
        player.getWorld().spawnParticle(Particle.SCULK_SOUL, fx, 24, 0.8, 0.8, 0.8, 0.03);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, fx, 18, 0.7, 0.6, 0.7, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.55f, 1.35f);
        if (!rewards.isEmpty()) {
            PetChatUtil.send(player, ChatColor.LIGHT_PURPLE + "Soul Harvest! "
                    + ChatColor.GRAY + "Reaped " + String.join(ChatColor.DARK_GRAY + ", " + ChatColor.GRAY, rewards) + ".");
        }
    }

    private void chargeCrystalRush(Player player, PetInstance pet, MechanicState state,
                                   long blocks, List<Block> brokenBlocks) {
        if (state.activity == Activity.CRYSTAL_RUSH) {
            return;
        }
        state.chargeBlocks = saturatingAdd(state.chargeBlocks, blocks);
        int threshold = crystalRushThreshold(pet);
        if (state.chargeBlocks < threshold) {
            return;
        }

        Set<BlockKey> targets = chooseTargets(player, brokenBlocks, 3, 6);
        if (targets.size() < 3) {
            state.chargeBlocks = threshold;
            return;
        }

        state.chargeBlocks %= threshold;
        state.activity = Activity.CRYSTAL_RUSH;
        state.targets.clear();
        state.targets.addAll(targets);
        state.targetsTotal = targets.size();
        state.targetsFound = 0;
        state.activityStartedAtMs = System.currentTimeMillis();
        state.activityExpiresAtMs = state.activityStartedAtMs + CRYSTAL_RUSH_DURATION_MS;

        BigDecimal recentGems = takeEarning(state, "gems");
        BigDecimal minimum = BigDecimal.valueOf(Math.max(1, pet.level()) * 25L);
        double cycleShare = 0.12 + safeTier(pet) * 0.015;
        state.crystalRewardPool = rounded(recentGems.multiply(BigDecimal.valueOf(cycleShare)).max(minimum));

        openActivityBar(player, state, ChatColor.AQUA + "Crystal Rush: 0/" + state.targetsTotal,
                BarColor.BLUE);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.45f);
        PetChatUtil.send(player, ChatColor.AQUA + "Crystal Rush! " + ChatColor.GRAY
                + "Break all three glowing crystal blocks before time runs out.");
    }

    private void chargeSeismicOverdrive(Player player, PetInstance pet, MechanicState state, long blocks) {
        long now = System.currentTimeMillis();
        if (state.overdriveUntilMs > now) {
            return;
        }
        state.chargeBlocks = saturatingAdd(state.chargeBlocks, blocks);
        int threshold = seismicThreshold(pet);
        if (state.chargeBlocks < threshold) {
            return;
        }

        state.chargeBlocks %= threshold;
        int durationSeconds = 8 + safeTier(pet) * 2 + Math.max(0, pet.level()) / 35;
        state.activity = Activity.SEISMIC_OVERDRIVE;
        state.activityStartedAtMs = now;
        state.overdriveUntilMs = now + durationSeconds * 1_000L;
        state.activityExpiresAtMs = state.overdriveUntilMs;
        openActivityBar(player, state, ChatColor.GOLD + "Seismic Overdrive", BarColor.RED);

        Location fx = player.getLocation().add(0.0, 0.25, 0.0);
        player.getWorld().spawnParticle(Particle.EXPLOSION, fx, 3, 0.6, 0.2, 0.6, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.75f, 1.2f);
        PetChatUtil.send(player, ChatColor.GOLD + "Seismic Overdrive! " + ChatColor.GRAY
                + "Mining rewards and pickaxe XP are multiplied by "
                + ChatColor.YELLOW + String.format(Locale.US, "%.2fx", overdriveMultiplier(pet))
                + ChatColor.GRAY + " for " + durationSeconds + " seconds.");
    }

    private void chargeBuriedVault(Player player, PetInstance pet, MechanicState state,
                                   long blocks, List<Block> brokenBlocks) {
        if (state.activity == Activity.BURIED_VAULT) {
            return;
        }
        state.chargeBlocks = saturatingAdd(state.chargeBlocks, blocks);
        int threshold = buriedVaultThreshold(pet);
        if (state.chargeBlocks < threshold) {
            return;
        }

        Set<BlockKey> targets = chooseTargets(player, brokenBlocks, 1, 8);
        if (targets.isEmpty()) {
            state.chargeBlocks = threshold;
            return;
        }

        state.chargeBlocks %= threshold;
        state.activity = Activity.BURIED_VAULT;
        state.targets.clear();
        state.targets.addAll(targets);
        state.targetsTotal = 1;
        state.targetsFound = 0;
        state.activityStartedAtMs = System.currentTimeMillis();
        state.activityExpiresAtMs = state.activityStartedAtMs + VAULT_HUNT_DURATION_MS;
        state.vaultGrade = rollVaultGrade(safeTier(pet));

        BigDecimal recentTokens = takeEarning(state, "tokens");
        BigDecimal tokenMinimum = BigDecimal.valueOf(Math.max(1, pet.level()) * 500L);
        state.vaultTokenReward = rounded(recentTokens.multiply(BigDecimal.valueOf(0.08))
                .max(tokenMinimum).multiply(BigDecimal.valueOf(state.vaultGrade.multiplier)));

        BigDecimal recentGems = takeEarning(state, "gems");
        if (state.vaultGrade.gemReward) {
            BigDecimal gemMinimum = BigDecimal.valueOf(Math.max(1, pet.level()) * 8L);
            state.vaultGemReward = rounded(recentGems.multiply(BigDecimal.valueOf(0.05))
                    .max(gemMinimum).multiply(BigDecimal.valueOf(state.vaultGrade.multiplier)));
        } else {
            state.vaultGemReward = BigDecimal.ZERO;
        }

        openActivityBar(player, state, state.vaultGrade.color + state.vaultGrade.label
                + " Vault: follow the particles", BarColor.YELLOW);
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.8f, 1.25f);
        PetChatUtil.send(player, ChatColor.GOLD + "Your Vault Hound found a "
                + state.vaultGrade.color + state.vaultGrade.label + " Vault"
                + ChatColor.GOLD + "! " + ChatColor.GRAY + "Follow the gold particles and mine it.");
    }

    private void resolveBrokenTargets(Player player, MechanicState state,
                                      Collection<Block> brokenBlocks) {
        if (state.targets.isEmpty()) {
            return;
        }
        Set<BlockKey> broken = new HashSet<>();
        for (Block block : brokenBlocks) {
            if (block != null) {
                broken.add(BlockKey.of(block));
            }
        }
        int matched = 0;
        for (BlockKey target : new ArrayList<>(state.targets)) {
            if (broken.contains(target) && state.targets.remove(target)) {
                matched++;
            }
        }
        for (int i = 0; i < matched; i++) {
            if (state.activity == Activity.CRYSTAL_RUSH) {
                rewardCrystalTarget(player, state);
            } else if (state.activity == Activity.BURIED_VAULT) {
                rewardBuriedVault(player, state);
            }
        }
    }

    /**
     * X-Prison intentionally omits block locations from its bulk-break event. Count one
     * active target per bulk proc so area-mining enchants can complete target mechanics,
     * while ordinary mining still requires the player to find the marked block.
     */
    private void resolveBulkTarget(Player player, MechanicState state) {
        if (state.targets.isEmpty()) {
            return;
        }
        BlockKey target = state.targets.iterator().next();
        state.targets.remove(target);
        if (state.activity == Activity.CRYSTAL_RUSH) {
            rewardCrystalTarget(player, state);
        } else if (state.activity == Activity.BURIED_VAULT) {
            rewardBuriedVault(player, state);
        }
    }

    private void rewardCrystalTarget(Player player, MechanicState state) {
        state.targetsFound++;
        boolean complete = state.targetsFound >= state.targetsTotal;
        BigDecimal share = state.crystalRewardPool.multiply(BigDecimal.valueOf(complete ? 0.50 : 0.25));
        share = rounded(share);
        awardCurrency(player, "gems", share);

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK,
                0.9f, complete ? 1.8f : 1.35f + state.targetsFound * 0.12f);
        player.getWorld().spawnParticle(Particle.END_ROD,
                player.getLocation().add(0.0, 1.0, 0.0), 12, 0.45, 0.55, 0.45, 0.03);
        if (complete) {
            PetChatUtil.send(player, ChatColor.AQUA + "Crystal Rush complete! " + ChatColor.GRAY
                    + "Recovered " + formatCurrency("gems", state.crystalRewardPool) + ".");
            finishActivity(state);
        } else if (state.bossBar != null) {
            state.bossBar.setTitle(ChatColor.AQUA + "Crystal Rush: " + state.targetsFound
                    + "/" + state.targetsTotal);
        }
    }

    private void rewardBuriedVault(Player player, MechanicState state) {
        state.targetsFound = 1;
        List<String> rewards = new ArrayList<>();
        if (awardCurrency(player, "tokens", state.vaultTokenReward)) {
            rewards.add(formatCurrency("tokens", state.vaultTokenReward));
        }
        if (awardCurrency(player, "gems", state.vaultGemReward)) {
            rewards.add(formatCurrency("gems", state.vaultGemReward));
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.25f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.55f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                player.getLocation().add(0.0, 1.0, 0.0), 24, 0.7, 0.7, 0.7, 0.04);
        PetChatUtil.send(player, state.vaultGrade.color + state.vaultGrade.label + " Vault opened! "
                + ChatColor.GRAY + "Recovered "
                + (rewards.isEmpty()
                ? "no currency because the X-Prison reward could not be delivered."
                : String.join(ChatColor.DARK_GRAY + " and " + ChatColor.GRAY, rewards) + "."));
        finishActivity(state);
    }

    private Set<BlockKey> chooseTargets(Player player, List<Block> justBroken, int count, int radius) {
        Block anchor = justBroken.stream().filter(java.util.Objects::nonNull).findFirst().orElse(null);
        if (anchor == null) {
            anchor = player.getTargetBlockExact(10);
        }
        if (anchor == null) {
            anchor = player.getLocation().clone().subtract(0.0, 1.0, 0.0).getBlock();
        }

        Mine mine;
        try {
            mine = XPrisonAPI.getInstance().getMinesApi().getMineAtLocation(anchor.getLocation());
            if (mine == null) {
                mine = XPrisonAPI.getInstance().getMinesApi().getMineAtLocation(player.getLocation());
            }
        } catch (RuntimeException | LinkageError ignored) {
            return Set.of();
        }
        if (mine == null) {
            return Set.of();
        }

        Set<BlockKey> excluded = new HashSet<>();
        for (Block block : justBroken) {
            if (block != null) {
                excluded.add(BlockKey.of(block));
            }
        }

        List<Block> candidates = new ArrayList<>();
        World world = anchor.getWorld();
        int minY = Math.max(world.getMinHeight(), anchor.getY() - 4);
        int maxY = Math.min(world.getMaxHeight() - 1, anchor.getY() + 4);
        for (int x = anchor.getX() - radius; x <= anchor.getX() + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = anchor.getZ() - radius; z <= anchor.getZ() + radius; z++) {
                    Block candidate = world.getBlockAt(x, y, z);
                    BlockKey key = BlockKey.of(candidate);
                    Material type = candidate.getType();
                    if (excluded.contains(key) || !type.isSolid() || type == Material.BEDROCK
                            || type == Material.BARRIER || type.getHardness() < 0.0f
                            || !mine.isInMine(candidate.getLocation())) {
                        continue;
                    }
                    if (isExposed(candidate, excluded)) {
                        candidates.add(candidate);
                    }
                }
            }
        }
        Collections.shuffle(candidates, ThreadLocalRandom.current());

        Set<BlockKey> selected = new HashSet<>();
        for (Block candidate : candidates) {
            selected.add(BlockKey.of(candidate));
            if (selected.size() >= count) {
                break;
            }
        }
        return selected;
    }

    private static boolean isExposed(Block block, Set<BlockKey> blocksBeingBroken) {
        for (BlockFace face : EXPOSED_FACES) {
            Block neighbor = block.getRelative(face);
            if (neighbor.getType().isAir() || blocksBeingBroken.contains(BlockKey.of(neighbor))) {
                return true;
            }
        }
        return false;
    }

    private void tickActiveMechanics() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, MechanicState> entry : new ArrayList<>(states.entrySet())) {
            UUID playerId = entry.getKey();
            MechanicState state = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);
            PetInstance pet = petManager.getActivePet(playerId).orElse(null);
            if (player == null || !player.isOnline() || pet == null
                    || !pet.definition().id().equalsIgnoreCase(state.petId)) {
                clearState(playerId);
                continue;
            }

            if (state.activity == Activity.NONE) {
                continue;
            }
            if (now >= state.activityExpiresAtMs) {
                expireActivity(player, state);
                continue;
            }

            updateActivityBar(state, now);
            if (state.activity == Activity.SEISMIC_OVERDRIVE) {
                Location fx = player.getLocation().add(0.0, 0.25, 0.0);
                player.spawnParticle(Particle.DUST, fx, 8, 0.9, 0.2, 0.9, 0.0,
                        new Particle.DustOptions(Color.fromRGB(255, 105, 35), 1.15f));
            } else {
                showTargetParticles(player, state);
            }
        }
    }

    private void showTargetParticles(Player player, MechanicState state) {
        Particle.DustOptions dust = state.activity == Activity.CRYSTAL_RUSH ? CRYSTAL_DUST : VAULT_DUST;
        for (BlockKey target : state.targets) {
            World world = Bukkit.getWorld(target.worldId);
            if (world == null || !world.equals(player.getWorld())) {
                continue;
            }
            Location center = new Location(world, target.x + 0.5, target.y + 0.55, target.z + 0.5);
            player.spawnParticle(Particle.DUST, center, 8, 0.42, 0.42, 0.42, 0.0, dust);
            if (state.activity == Activity.CRYSTAL_RUSH) {
                player.spawnParticle(Particle.END_ROD, center, 2, 0.25, 0.25, 0.25, 0.01);
            } else {
                player.spawnParticle(Particle.HAPPY_VILLAGER, center, 2, 0.25, 0.25, 0.25, 0.01);
            }
        }
    }

    private void expireActivity(Player player, MechanicState state) {
        if (state.activity == Activity.CRYSTAL_RUSH) {
            PetChatUtil.send(player, ChatColor.GRAY + "Crystal Rush ended with " + ChatColor.AQUA
                    + state.targetsFound + "/" + state.targetsTotal + ChatColor.GRAY + " crystals found.");
        } else if (state.activity == Activity.BURIED_VAULT) {
            PetChatUtil.send(player, ChatColor.GRAY + "The buried vault slipped away.");
        } else if (state.activity == Activity.SEISMIC_OVERDRIVE) {
            player.playSound(player.getLocation(), Sound.BLOCK_DEEPSLATE_BREAK, 0.7f, 0.75f);
            PetChatUtil.send(player, ChatColor.GRAY + "Seismic Overdrive has ended.");
        }
        finishActivity(state);
    }

    private void openActivityBar(Player player, MechanicState state, String title, BarColor color) {
        removeBossBar(state);
        state.bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
        state.bossBar.setProgress(1.0);
        state.bossBar.addPlayer(player);
    }

    private static void updateActivityBar(MechanicState state, long now) {
        if (state.bossBar == null) {
            return;
        }
        long duration = Math.max(1L, state.activityExpiresAtMs - state.activityStartedAtMs);
        double progress = (state.activityExpiresAtMs - now) / (double) duration;
        state.bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    private static void finishActivity(MechanicState state) {
        removeBossBar(state);
        state.activity = Activity.NONE;
        state.targets.clear();
        state.targetsTotal = 0;
        state.targetsFound = 0;
        state.activityStartedAtMs = 0L;
        state.activityExpiresAtMs = 0L;
        state.overdriveUntilMs = 0L;
        state.crystalRewardPool = BigDecimal.ZERO;
        state.vaultTokenReward = BigDecimal.ZERO;
        state.vaultGemReward = BigDecimal.ZERO;
        state.vaultGrade = VaultGrade.IRON;
    }

    private MechanicState stateFor(Player player, PetInstance pet) {
        UUID playerId = player.getUniqueId();
        MechanicState current = states.get(playerId);
        String petId = pet.definition().id().toLowerCase(Locale.ROOT);
        if (current != null && current.petId.equals(petId)) {
            return current;
        }
        clearState(playerId);
        MechanicState created = new MechanicState(petId);
        states.put(playerId, created);
        return created;
    }

    private void clearState(UUID playerId) {
        MechanicState removed = states.remove(playerId);
        if (removed != null) {
            removeBossBar(removed);
        }
    }

    private static void removeBossBar(MechanicState state) {
        if (state.bossBar != null) {
            state.bossBar.removeAll();
            state.bossBar = null;
        }
    }

    private static void recordCycleEarning(MechanicState state, PetInstance pet,
                                           String currencyId, BigDecimal amount) {
        boolean relevant = hasEffect(pet, PetEffectType.SOUL_HARVEST)
                || (hasEffect(pet, PetEffectType.CRYSTAL_RUSH) && "gems".equals(currencyId))
                || (hasEffect(pet, PetEffectType.BURIED_VAULT)
                && ("tokens".equals(currencyId) || "gems".equals(currencyId)));
        if (relevant) {
            state.earnings.merge(currencyId, amount, BigDecimal::add);
        }
    }

    private static Map<String, BigDecimal> takeEarnings(MechanicState state) {
        Map<String, BigDecimal> result = new HashMap<>(state.earnings);
        state.earnings.clear();
        return result;
    }

    private static BigDecimal takeEarning(MechanicState state, String currencyId) {
        BigDecimal result = state.earnings.remove(currencyId);
        return result == null ? BigDecimal.ZERO : result;
    }

    private boolean awardCurrency(Player player, String currencyId, BigDecimal amount) {
        BigDecimal safeAmount = rounded(amount);
        if (safeAmount.signum() <= 0) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        internalRewardPlayers.add(playerId);
        try {
            return XPrisonAPI.getInstance().getCurrencyApi().addBalance(
                    player, currencyId, safeAmount, ReceiveCause.MINING_OTHERS);
        } catch (RuntimeException | LinkageError error) {
            plugin.getLogger().warning("Could not award " + currencyId
                    + " for a prison pet mechanic: " + error.getMessage());
            return false;
        } finally {
            internalRewardPlayers.remove(playerId);
        }
    }

    private String formatCurrency(String currencyId, BigDecimal amount) {
        try {
            XPrisonCurrency currency = XPrisonAPI.getInstance().getCurrencyApi().getCurrency(currencyId);
            if (currency != null) {
                return currency.format(amount) + " " + currency.getDisplayName();
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return rounded(amount).toPlainString() + " " + currencyId;
    }

    private static boolean hasEffect(PetInstance pet, PetEffectType type) {
        if (pet == null || type == null) {
            return false;
        }
        for (PetEffectDefinition effect : pet.appliedEffects()) {
            if (effect != null && effect.type() == type && effect.baseValue() > 0.0) {
                return true;
            }
        }
        return false;
    }

    private static int soulHarvestThreshold(PetInstance pet) {
        return Math.max(300, 600 - safeTier(pet) * 30 - Math.max(1, pet.level()));
    }

    private static int crystalRushThreshold(PetInstance pet) {
        return Math.max(250, 500 - safeTier(pet) * 25 - Math.max(1, pet.level()));
    }

    private static int seismicThreshold(PetInstance pet) {
        return Math.max(500, 1_200 - safeTier(pet) * 60 - Math.max(1, pet.level()) * 2);
    }

    private static int buriedVaultThreshold(PetInstance pet) {
        return Math.max(400, 850 - safeTier(pet) * 35 - Math.max(1, pet.level()));
    }

    private static double overdriveMultiplier(PetInstance pet) {
        return 1.35 + safeTier(pet) * 0.10 + Math.min(0.15, Math.max(1, pet.level()) / 1_000.0);
    }

    private static int safeTier(PetInstance pet) {
        return Math.max(1, pet == null ? 1 : pet.tier());
    }

    private static double levelRatio(PetInstance pet) {
        if (pet == null || pet.definition().maxLevel() <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, pet.level() / (double) pet.definition().maxLevel()));
    }

    private static VaultGrade rollVaultGrade(int tier) {
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        double mythicChance = 1.5 + tier * 0.5;
        double diamondChance = 7.0 + tier;
        double goldChance = 23.0 + tier * 2.0;
        if (roll < mythicChance) {
            return VaultGrade.MYTHIC;
        }
        if (roll < mythicChance + diamondChance) {
            return VaultGrade.DIAMOND;
        }
        if (roll < mythicChance + diamondChance + goldChance) {
            return VaultGrade.GOLD;
        }
        return VaultGrade.IRON;
    }

    private static BigDecimal rounded(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static boolean isMiningReward(ReceiveCause cause) {
        return ReceiveCause.MINING.equals(cause) || ReceiveCause.MINING_OTHERS.equals(cause);
    }

    private static Player onlinePlayer(OfflinePlayer offlinePlayer) {
        if (offlinePlayer == null) {
            return null;
        }
        Player player = offlinePlayer.getPlayer();
        return player != null && player.isOnline() ? player : null;
    }

    private enum Activity {
        NONE,
        CRYSTAL_RUSH,
        SEISMIC_OVERDRIVE,
        BURIED_VAULT
    }

    private enum VaultGrade {
        IRON("Iron", ChatColor.WHITE, 1.0, false),
        GOLD("Gold", ChatColor.GOLD, 2.0, false),
        DIAMOND("Diamond", ChatColor.AQUA, 5.0, true),
        MYTHIC("Mythic", ChatColor.LIGHT_PURPLE, 12.0, true);

        private final String label;
        private final ChatColor color;
        private final double multiplier;
        private final boolean gemReward;

        VaultGrade(String label, ChatColor color, double multiplier, boolean gemReward) {
            this.label = label;
            this.color = color;
            this.multiplier = multiplier;
            this.gemReward = gemReward;
        }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    private static final class MechanicState {
        private final String petId;
        private final Map<String, BigDecimal> earnings = new HashMap<>();
        private final Set<BlockKey> targets = new HashSet<>();
        private long chargeBlocks;
        private long petXpBlockRemainder;
        private Activity activity = Activity.NONE;
        private long activityStartedAtMs;
        private long activityExpiresAtMs;
        private long overdriveUntilMs;
        private int targetsTotal;
        private int targetsFound;
        private BigDecimal crystalRewardPool = BigDecimal.ZERO;
        private BigDecimal vaultTokenReward = BigDecimal.ZERO;
        private BigDecimal vaultGemReward = BigDecimal.ZERO;
        private VaultGrade vaultGrade = VaultGrade.IRON;
        private BossBar bossBar;

        private MechanicState(String petId) {
            this.petId = petId;
        }
    }
}
