package me.nakilex.levelplugin.xprison.rebirth;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.currency.enums.ReceiveCause;
import dev.drawethree.xprison.api.currency.event.PlayerCurrencyReceiveEvent;
import dev.drawethree.xprison.api.pickaxelevels.XPrisonPickaxeLevelsAPI;
import dev.drawethree.xprison.api.pickaxelevels.model.PickaxeLevel;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Roblox-style infinite rebirth progression layered on top of X-Prison.
 *
 * <p>X-Prison remains the source of truth for pickaxe level/XP, currencies and enchants. LevelPlugin
 * owns only the account rebirth count plus the mining income accumulated during the current run.</p>
 */
public final class XPrisonRebirthManager implements Listener {

    public static final String DEBUG_PERMISSION = "levelplugin.rebirth.debug";

    private final Main plugin;
    private final NamespacedKey pickaxeIdKey;
    private final NamespacedKey pickaxeOwnerKey;
    private final Map<UUID, RunEarnings> runEarnings = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> rebirthing = ConcurrentHashMap.newKeySet();

    private XPrisonAPI api;
    private BukkitTask saveTask;
    private boolean enabled;

    public XPrisonRebirthManager(Main plugin) {
        this.plugin = plugin;
        this.pickaxeIdKey = new NamespacedKey(plugin, "rebirth_pickaxe_id");
        this.pickaxeOwnerKey = new NamespacedKey(plugin, "rebirth_pickaxe_owner");
    }

    public void enable() {
        if (enabled || !plugin.getConfig().getBoolean("xprison-rebirth.enabled", true)) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("X-Prison")) {
            plugin.getLogger().warning("[Rebirth] X-Prison is not enabled; rebirth progression is disabled.");
            return;
        }
        try {
            api = XPrisonAPI.getInstance();
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("[Rebirth] Could not access X-Prison API: " + ex.getMessage());
            return;
        }
        if (api == null) {
            plugin.getLogger().warning("[Rebirth] X-Prison API returned null; rebirth progression is disabled.");
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        long intervalSeconds = Math.max(10L,
                plugin.getConfig().getLong("xprison-rebirth.persistence-save-interval-seconds", 30L));
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::flushDirty,
                intervalSeconds * 20L, intervalSeconds * 20L);
        enabled = true;
        warnAboutPickaxeCap();
        plugin.getLogger().info("[Rebirth] Infinite X-Prison rebirth progression enabled.");
    }

    public void disable() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        flushAll();
        HandlerList.unregisterAll(this);
        runEarnings.clear();
        dirtyPlayers.clear();
        rebirthing.clear();
        api = null;
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled && api != null;
    }

    /**
     * Runs after the existing pet HIGH-priority modifiers so rebirth multiplies the final mining payout.
     * Only mining-generated money/tokens are affected; pay, give, redeem, refunds, etc. stay unchanged.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCurrencyReceive(PlayerCurrencyReceiveEvent event) {
        if (!isEnabled() || event.getCurrency() == null || !isMiningReward(event.getCause())) {
            return;
        }

        String currency = event.getCurrency().getName().toLowerCase(Locale.ROOT);
        if (!currency.equals(moneyCurrency()) && !currency.equals(tokenCurrency())) {
            return;
        }

        BigDecimal amount = event.getAmountExact();
        if (amount == null || amount.signum() <= 0) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        BigDecimal multiplier = BigDecimal.valueOf(multiplierFor(getRebirths(playerId)));
        BigDecimal adjusted = amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        event.setAmountExact(adjusted);

        RunEarnings earnings = earnings(playerId);
        if (currency.equals(moneyCurrency())) {
            earnings.money = earnings.money.add(adjusted);
        } else {
            earnings.tokens = earnings.tokens.add(adjusted);
        }
        dirtyPlayers.add(playerId);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flush(event.getPlayer().getUniqueId());
        runEarnings.remove(event.getPlayer().getUniqueId());
    }

    public RebirthPreview preview(Player player) {
        if (!isEnabled()) {
            return RebirthPreview.unavailable("X-Prison rebirth integration is disabled.");
        }

        UUID playerId = player.getUniqueId();
        int rebirths = getRebirths(playerId);
        int requiredLevel = requiredLevelFor(rebirths);
        PickaxeLookup pickaxe = findUniquePickaxe(player);
        int currentLevel = pickaxe.single() == null ? 0 : pickaxe.single().level();
        RunEarnings earned = earnings(playerId);

        BigDecimal money = api.getCurrencyApi().getBalanceExact(player, moneyCurrency());
        BigDecimal tokens = api.getCurrencyApi().getBalanceExact(player, tokenCurrency());
        BigDecimal gems = api.getCurrencyApi().getBalanceExact(player, gemCurrency());
        long reward = calculateGemReward(rebirths, currentLevel, requiredLevel, earned.money, earned.tokens);

        String blocker = null;
        if (pickaxe.count() == 0) {
            blocker = "No X-Prison pickaxe was found in your inventory.";
        } else if (pickaxe.count() > 1) {
            blocker = "Keep exactly one X-Prison pickaxe in your inventory before rebirthing.";
        } else {
            blocker = pickaxeBindingBlocker(player, pickaxe.single());
            if (blocker == null && currentLevel < requiredLevel) {
                blocker = "Reach Pickaxe Level " + requiredLevel + " first.";
            }
        }

        return new RebirthPreview(
                true,
                rebirths,
                currentLevel,
                requiredLevel,
                multiplierFor(rebirths),
                multiplierFor(rebirths + 1),
                money,
                tokens,
                gems,
                earned.money,
                earned.tokens,
                reward,
                blocker == null,
                blocker,
                pickaxe.count());
    }

    public RebirthResult rebirth(Player player) {
        return rebirth(player, false);
    }

    /** Debug force bypasses only the level requirement, never the unique-pickaxe safety check. */
    public RebirthResult rebirth(Player player, boolean debugForce) {
        if (!isEnabled()) {
            return RebirthResult.failure("X-Prison rebirth integration is disabled.");
        }
        UUID playerId = player.getUniqueId();
        if (!rebirthing.add(playerId)) {
            return RebirthResult.failure("A rebirth is already being processed.");
        }

        try {
            int rebirths = getRebirths(playerId);
            int requiredLevel = requiredLevelFor(rebirths);
            PickaxeLookup lookup = findUniquePickaxe(player);
            if (lookup.count() == 0 || lookup.single() == null) {
                return RebirthResult.failure("You need an X-Prison pickaxe in your inventory.");
            }
            if (lookup.count() > 1) {
                return RebirthResult.failure("Keep exactly one X-Prison pickaxe in your inventory before rebirthing.");
            }

            LocatedPickaxe pickaxe = lookup.single();
            String bindingBlocker = pickaxeBindingBlocker(player, pickaxe);
            if (bindingBlocker != null) {
                return RebirthResult.failure(bindingBlocker);
            }
            if (!debugForce && pickaxe.level() < requiredLevel) {
                return RebirthResult.failure("Reach Pickaxe Level " + requiredLevel + " first.");
            }

            RunEarnings earned = earnings(playerId);
            long gemReward = calculateGemReward(rebirths, pickaxe.level(), requiredLevel, earned.money, earned.tokens);
            var currencyApi = api.getCurrencyApi();
            BigDecimal oldMoney = currencyApi.getBalanceExact(player, moneyCurrency());
            BigDecimal oldTokens = currencyApi.getBalanceExact(player, tokenCurrency());
            BigDecimal oldGems = currencyApi.getBalanceExact(player, gemCurrency());
            ItemStack originalPickaxe = pickaxe.item().clone();

            try {
                if (!currencyApi.setBalance(player, moneyCurrency(), BigDecimal.ZERO)) {
                    throw new IllegalStateException("X-Prison refused to reset money");
                }
                if (!currencyApi.setBalance(player, tokenCurrency(), BigDecimal.ZERO)) {
                    throw new IllegalStateException("X-Prison refused to reset tokens");
                }

                XPrisonPickaxeLevelsAPI levelsApi = api.getPickaxeLevelsApi();
                // X-Prison writes a fresh copy into the slot on every call, so always re-read the live stack.
                levelsApi.setPickaxeExp(player, livePickaxe(player, pickaxe), 0L);
                levelsApi.setPickaxeLevel(player, livePickaxe(player, pickaxe), 1);

                ItemStack resetPickaxe = livePickaxe(player, pickaxe);
                var resetLevel = resetPickaxe == null ? java.util.Optional.<PickaxeLevel>empty()
                        : levelsApi.getPickaxeLevel(resetPickaxe);
                long resetExp = resetPickaxe == null ? -1L : levelsApi.getPickaxeExp(resetPickaxe);
                if (resetLevel.isEmpty() || resetLevel.get().getLevel() != 1 || resetExp != 0L) {
                    throw new IllegalStateException("X-Prison did not fully reset the pickaxe level/XP (level="
                            + resetLevel.map(l -> String.valueOf(l.getLevel())).orElse("none") + ", xp=" + resetExp + ")");
                }

                // Bind only once the reset succeeded, so a rolled-back attempt never locks the account.
                ensurePickaxeBinding(player, pickaxe);

                if (gemReward > 0 && !currencyApi.addBalance(
                        player, gemCurrency(), BigDecimal.valueOf(gemReward), ReceiveCause.GIVE)) {
                    throw new IllegalStateException("X-Prison refused to award rebirth gems");
                }

                int newRebirths = rebirths + 1;
                runEarnings.put(playerId, new RunEarnings());
                plugin.getPlayerConfig().setXPrisonRebirthState(
                        playerId, newRebirths, BigDecimal.ZERO, BigDecimal.ZERO);
                dirtyPlayers.remove(playerId);
                plugin.getPlayerConfig().setXPrisonPickaxeLevel(playerId, 1);

                return RebirthResult.success(newRebirths, gemReward, multiplierFor(newRebirths));
            } catch (Throwable failure) {
                plugin.getLogger().warning("[Rebirth] Rebirth transaction failed for " + player.getName()
                        + "; attempting rollback: " + failure.getMessage());
                restorePickaxe(player, pickaxe, originalPickaxe);
                safeSetBalance(player, moneyCurrency(), oldMoney);
                safeSetBalance(player, tokenCurrency(), oldTokens);
                safeSetBalance(player, gemCurrency(), oldGems);
                return RebirthResult.failure("Rebirth failed and was rolled back. Check the server console.");
            }
        } finally {
            rebirthing.remove(playerId);
        }
    }

    private static ItemStack livePickaxe(Player player, LocatedPickaxe location) {
        return location.offhand()
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItem(location.slot());
    }

    public int getRebirths(UUID playerId) {
        return Math.max(0, plugin.getPlayerConfig().getXPrisonRebirthCount(playerId));
    }

    public int requiredLevelFor(int rebirths) {
        int start = Math.max(1, plugin.getConfig().getInt("xprison-rebirth.requirement.start-level", 10));
        int step = Math.max(1, plugin.getConfig().getInt("xprison-rebirth.requirement.levels-per-rebirth", 5));
        long required = (long) start + (long) Math.max(0, rebirths) * step;
        return required > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) required;
    }

    public double multiplierFor(int rebirths) {
        double base = Math.max(1.0D, plugin.getConfig().getDouble("xprison-rebirth.multiplier.base", 1.0D));
        double per = Math.max(0.0D, plugin.getConfig().getDouble("xprison-rebirth.multiplier.per-rebirth", 0.5D));
        return base + Math.max(0, rebirths) * per;
    }

    public long calculateGemReward(int rebirths, int currentLevel, int requiredLevel,
                                   BigDecimal runMoney, BigDecimal runTokens) {
        long base = Math.max(0L, plugin.getConfig().getLong("xprison-rebirth.gem-reward.base", 10L));
        double perRebirth = Math.max(0.0D,
                plugin.getConfig().getDouble("xprison-rebirth.gem-reward.per-rebirth", 1.0D));
        double moneyScale = Math.max(1.0D,
                plugin.getConfig().getDouble("xprison-rebirth.gem-reward.money-scale", 100_000.0D));
        double moneyWeight = Math.max(0.0D,
                plugin.getConfig().getDouble("xprison-rebirth.gem-reward.money-log10-weight", 8.0D));
        double tokenScale = Math.max(1.0D,
                plugin.getConfig().getDouble("xprison-rebirth.gem-reward.token-scale", 10_000.0D));
        double tokenWeight = Math.max(0.0D,
                plugin.getConfig().getDouble("xprison-rebirth.gem-reward.token-log10-weight", 6.0D));
        long perExtraLevel = Math.max(0L,
                plugin.getConfig().getLong("xprison-rebirth.gem-reward.per-extra-level", 1L));
        int extraLevelCap = Math.max(0,
                plugin.getConfig().getInt("xprison-rebirth.gem-reward.extra-level-cap", 25));
        long maxReward = Math.max(base,
                plugin.getConfig().getLong("xprison-rebirth.gem-reward.max-per-rebirth", 1_000_000L));

        double money = positiveDouble(runMoney);
        double tokens = positiveDouble(runTokens);
        long moneyReward = (long) Math.floor(Math.log10(1.0D + money / moneyScale) * moneyWeight);
        long tokenReward = (long) Math.floor(Math.log10(1.0D + tokens / tokenScale) * tokenWeight);
        long rebirthReward = (long) Math.floor(Math.max(0, rebirths) * perRebirth);
        int extraLevels = Math.min(extraLevelCap, Math.max(0, currentLevel - requiredLevel));
        long extraReward = (long) extraLevels * perExtraLevel;

        long total;
        try {
            total = Math.addExact(base, Math.addExact(rebirthReward,
                    Math.addExact(moneyReward, Math.addExact(tokenReward, extraReward))));
        } catch (ArithmeticException overflow) {
            total = maxReward;
        }
        return Math.max(0L, Math.min(maxReward, total));
    }

    public void setRebirthsForDebug(UUID playerId, int rebirths) {
        RunEarnings earned = earnings(playerId);
        plugin.getPlayerConfig().setXPrisonRebirthState(
                playerId, Math.max(0, rebirths), earned.money, earned.tokens);
        dirtyPlayers.remove(playerId);
    }

    public void setRunEarningsForDebug(UUID playerId, BigDecimal money, BigDecimal tokens) {
        RunEarnings earned = new RunEarnings(positive(money), positive(tokens));
        runEarnings.put(playerId, earned);
        plugin.getPlayerConfig().setXPrisonRebirthState(
                playerId, getRebirths(playerId), earned.money, earned.tokens);
        dirtyPlayers.remove(playerId);
    }

    public boolean setPickaxeLevelForDebug(Player player, int level) {
        if (!isEnabled()) return false;
        PickaxeLookup lookup = findUniquePickaxe(player);
        if (lookup.count() != 1 || lookup.single() == null) return false;
        try {
            api.getPickaxeLevelsApi().setPickaxeLevel(player, lookup.single().item(), Math.max(1, level));
            return true;
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("[Rebirth] Debug level set failed: " + ex.getMessage());
            return false;
        }
    }

    public boolean unbindPickaxeForDebug(Player player) {
        if (!isEnabled()) return false;
        PickaxeLookup lookup = findUniquePickaxe(player);
        if (lookup.count() != 1 || lookup.single() == null) return false;

        var item = lookup.single().item();
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.remove(pickaxeIdKey);
        pdc.remove(pickaxeOwnerKey);
        item.setItemMeta(meta);
        plugin.getPlayerConfig().setXPrisonRebirthPickaxeId(player.getUniqueId(), null);
        return true;
    }

    public boolean giveMiningRewardForDebug(Player player, String currency, BigDecimal amount) {
        if (!isEnabled() || amount == null || amount.signum() <= 0) return false;
        String normalized = currency == null ? "" : currency.toLowerCase(Locale.ROOT);
        if (!normalized.equals(moneyCurrency()) && !normalized.equals(tokenCurrency())) return false;
        return api.getCurrencyApi().addBalance(player, normalized, amount, ReceiveCause.MINING);
    }

    public int actualXPrisonMaxPickaxeLevel() {
        if (!isEnabled()) return 0;
        PickaxeLevel max = api.getPickaxeLevelsApi().getMaxPickaxeLevel();
        return max == null ? 0 : max.getLevel();
    }

    public int desiredPracticalMaxPickaxeLevel() {
        return Math.max(100, plugin.getConfig().getInt("xprison-rebirth.practical-pickaxe-max-level", 10_000));
    }

    private PickaxeLookup findUniquePickaxe(Player player) {
        if (!isEnabled()) return new PickaxeLookup(0, null);
        List<LocatedPickaxe> found = new ArrayList<>();
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack item = storage[slot];
            if (item == null || item.getType().isAir()) continue;
            var level = api.getPickaxeLevelsApi().getPickaxeLevel(item);
            if (level.isPresent()) {
                found.add(new LocatedPickaxe(slot, false, item, level.get().getLevel(),
                        api.getPickaxeLevelsApi().getPickaxeExp(item)));
            }
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) {
            var level = api.getPickaxeLevelsApi().getPickaxeLevel(offhand);
            if (level.isPresent()) {
                found.add(new LocatedPickaxe(-1, true, offhand, level.get().getLevel(),
                        api.getPickaxeLevelsApi().getPickaxeExp(offhand)));
            }
        }
        return new PickaxeLookup(found.size(), found.size() == 1 ? found.getFirst() : null);
    }

    private String pickaxeBindingBlocker(Player player, LocatedPickaxe pickaxe) {
        if (pickaxe == null) return "No X-Prison pickaxe was found in your inventory.";

        var pdc = pickaxe.item().getItemMeta().getPersistentDataContainer();
        String itemOwner = pdc.get(pickaxeOwnerKey, PersistentDataType.STRING);
        if (itemOwner != null && !itemOwner.equals(player.getUniqueId().toString())) {
            return "That pickaxe is bound to another player's rebirth progression.";
        }

        String accountPickaxeId = plugin.getPlayerConfig().getXPrisonRebirthPickaxeId(player.getUniqueId());
        if (accountPickaxeId == null || accountPickaxeId.isBlank()) return null;

        String itemPickaxeId = pdc.get(pickaxeIdKey, PersistentDataType.STRING);
        if (!accountPickaxeId.equals(itemPickaxeId)) {
            return "Use the pickaxe bound to your rebirth progression.";
        }
        return null;
    }

    private void ensurePickaxeBinding(Player player, LocatedPickaxe pickaxe) {
        UUID playerId = player.getUniqueId();
        String accountPickaxeId = plugin.getPlayerConfig().getXPrisonRebirthPickaxeId(playerId);
        // Inventory contents may be copies; edit the live stack and write it back into its slot.
        ItemStack live = livePickaxe(player, pickaxe);
        if (live == null || live.getType().isAir()) {
            throw new IllegalStateException("Pickaxe disappeared before it could be bound");
        }
        var meta = live.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        String itemPickaxeId = pdc.get(pickaxeIdKey, PersistentDataType.STRING);

        if (accountPickaxeId == null || accountPickaxeId.isBlank()) {
            accountPickaxeId = itemPickaxeId == null || itemPickaxeId.isBlank()
                    ? UUID.randomUUID().toString() : itemPickaxeId;
            plugin.getPlayerConfig().setXPrisonRebirthPickaxeId(playerId, accountPickaxeId);
        }

        pdc.set(pickaxeIdKey, PersistentDataType.STRING, accountPickaxeId);
        pdc.set(pickaxeOwnerKey, PersistentDataType.STRING, playerId.toString());
        live.setItemMeta(meta);
        if (pickaxe.offhand()) {
            player.getInventory().setItemInOffHand(live);
        } else {
            player.getInventory().setItem(pickaxe.slot(), live);
        }
    }

    private void restorePickaxe(Player player, LocatedPickaxe location, ItemStack original) {
        try {
            // Prefer the X-Prison API so its level listeners (mine tier/snapshot) are restored too.
            api.getPickaxeLevelsApi().setPickaxeExp(player, livePickaxe(player, location), location.exp());
            api.getPickaxeLevelsApi().setPickaxeLevel(player, livePickaxe(player, location), location.level());
            player.updateInventory();
            return;
        } catch (Throwable apiRestoreFailure) {
            plugin.getLogger().warning("[Rebirth] API rollback of the pickaxe failed for " + player.getName()
                    + "; restoring the full item snapshot instead: " + apiRestoreFailure.getMessage());
        }
        try {
            if (location.offhand()) {
                player.getInventory().setItemInOffHand(original);
            } else {
                player.getInventory().setItem(location.slot(), original);
            }
            player.updateInventory();
        } catch (Throwable restoreFailure) {
            plugin.getLogger().severe("[Rebirth] Could not restore pickaxe for " + player.getName()
                    + " after failed rebirth: " + restoreFailure.getMessage());
        }
    }

    private void safeSetBalance(Player player, String currency, BigDecimal value) {
        try {
            api.getCurrencyApi().setBalance(player, currency, value);
        } catch (Throwable rollbackFailure) {
            plugin.getLogger().severe("[Rebirth] Could not rollback " + currency + " for " + player.getName()
                    + ": " + rollbackFailure.getMessage());
        }
    }

    private RunEarnings earnings(UUID playerId) {
        return runEarnings.computeIfAbsent(playerId, id -> new RunEarnings(
                plugin.getPlayerConfig().getXPrisonRebirthRunMoney(id),
                plugin.getPlayerConfig().getXPrisonRebirthRunTokens(id)));
    }

    private void flushDirty() {
        for (UUID playerId : List.copyOf(dirtyPlayers)) {
            flush(playerId);
        }
    }

    private void flushAll() {
        for (UUID playerId : List.copyOf(runEarnings.keySet())) {
            flush(playerId);
        }
    }

    private void flush(UUID playerId) {
        RunEarnings earned = runEarnings.get(playerId);
        if (earned == null) return;
        plugin.getPlayerConfig().setXPrisonRebirthState(
                playerId, getRebirths(playerId), earned.money, earned.tokens);
        dirtyPlayers.remove(playerId);
    }

    private void warnAboutPickaxeCap() {
        int actual = actualXPrisonMaxPickaxeLevel();
        int desired = desiredPracticalMaxPickaxeLevel();
        if (actual > 0 && actual < desired) {
            plugin.getLogger().warning("[Rebirth] X-Prison pickaxe max-level is " + actual + ", while rebirth progression is configured for a practical cap of " + desired + ".");
            plugin.getLogger().warning("[Rebirth] Raise levels-formula.max-level in plugins/X-Prison/pickaxe-levels.yml and restart the server. Rebirth eligibility itself does not cap leveling.");
        }
    }

    private String moneyCurrency() {
        return plugin.getConfig().getString("xprison-rebirth.currencies.money", "money").toLowerCase(Locale.ROOT);
    }

    private String tokenCurrency() {
        return plugin.getConfig().getString("xprison-rebirth.currencies.tokens", "tokens").toLowerCase(Locale.ROOT);
    }

    private String gemCurrency() {
        return plugin.getConfig().getString("xprison-rebirth.currencies.gems", "gems").toLowerCase(Locale.ROOT);
    }

    private static boolean isMiningReward(ReceiveCause cause) {
        return ReceiveCause.MINING.equals(cause) || ReceiveCause.MINING_OTHERS.equals(cause);
    }

    private static BigDecimal positive(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private static double positiveDouble(BigDecimal value) {
        if (value == null || value.signum() <= 0) return 0.0D;
        double result = value.doubleValue();
        return Double.isFinite(result) ? Math.max(0.0D, result) : Double.MAX_VALUE;
    }

    private static final class RunEarnings {
        private BigDecimal money;
        private BigDecimal tokens;

        private RunEarnings() {
            this(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        private RunEarnings(BigDecimal money, BigDecimal tokens) {
            this.money = positive(money);
            this.tokens = positive(tokens);
        }
    }

    private record LocatedPickaxe(int slot, boolean offhand, ItemStack item, int level, long exp) {}
    private record PickaxeLookup(int count, LocatedPickaxe single) {}

    public record RebirthPreview(
            boolean available,
            int rebirths,
            int currentLevel,
            int requiredLevel,
            double currentMultiplier,
            double nextMultiplier,
            BigDecimal moneyBalance,
            BigDecimal tokenBalance,
            BigDecimal gemBalance,
            BigDecimal runMoneyEarned,
            BigDecimal runTokensEarned,
            long gemReward,
            boolean eligible,
            String blocker,
            int pickaxeCount) {

        private static RebirthPreview unavailable(String blocker) {
            return new RebirthPreview(false, 0, 0, 0, 1.0D, 1.0D,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, 0L, false, blocker, 0);
        }
    }

    public record RebirthResult(boolean success, String message, int newRebirths, long gemsAwarded,
                                double newMultiplier) {
        public static RebirthResult success(int newRebirths, long gemsAwarded, double newMultiplier) {
            return new RebirthResult(true, "Rebirth complete.", newRebirths, gemsAwarded, newMultiplier);
        }

        public static RebirthResult failure(String message) {
            return new RebirthResult(false, message, -1, 0L, 1.0D);
        }
    }
}
