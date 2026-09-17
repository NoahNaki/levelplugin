package me.nakilex.levelplugin.serverboard;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.currency.model.XPrisonCurrency;
import dev.drawethree.xprison.api.miningstats.model.MiningStats;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import dev.drawethree.xprison.api.currency.event.PlayerCurrencyReceiveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.math.BigDecimal;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LevelPlugin's own live mining action bar - Coins/Gems/Tokens earned per minute plus blocks per
 * second - replacing X-Prison's built-in one (disabled via mining-stats.yml mining-hud.enabled:
 * false) because that HUD formats currencies through each currency's plain-string `prefix:` in
 * currencies.yml, which cannot carry the font tag our own icons_pack.yml glyphs need (see the
 * nexo-glyph-font-namespace memory/note) - X-Prison's own icons there are stuck with plain "$"/
 * mahjong-tile-style Unicode symbols, or literal broken tag text if a font tag is force-fed in.
 * Building our own bar sidesteps that entirely: it's a real Adventure {@link Component} sent via
 * {@code Player.sendActionBar}, so {@code .font(Key.key("nexo","default"))} just works.
 * <p>
 * Rates are derived by polling {@link MiningStats#getBlocksMined()} /
 * {@link MiningStats#getCurrenciesEarned()} (both cumulative session counters) once a second and
 * keeping a 60-sample rolling window, rather than re-deriving X-Prison's own internal rate
 * tracking - {@code MiningRateTracker} and the HUD placeholder classes backing X-Prison's own
 * %..._per_min% placeholders are internal, not part of its public API.
 */
public final class PrisonMiningActionBar implements Listener {

    private static final Key NEXO_DEFAULT_FONT = Key.key("nexo", "default");
    private static final char ICON_COINS = 'ꑗ';
    private static final char ICON_GEMS = 'ꑜ';
    private static final char ICON_TOKENS = 'ꨲ'; // our own tokens_icon glyph, not the old external-pack one
    private static final long IDLE_HIDE_MILLIS = 5000L;
    private static final int WINDOW_SECONDS = 60;

    private final Map<UUID, Deque<Sample>> history = new HashMap<>();
    private final Map<UUID, Long> lastActiveMillis = new HashMap<>();
    /** Currencies paid by command-driven mining rewards, which X-Prison does not add to MiningStats. */
    private final Map<UUID, Double> supplementalGems = new HashMap<>();
    private final Map<UUID, XPrisonCurrency> supplementalGemTypes = new HashMap<>();
    private BukkitTask task;

    public void enable(Plugin plugin) {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void disable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        history.clear();
        lastActiveMillis.clear();
        supplementalGems.clear();
        supplementalGemTypes.clear();
    }

    /**
     * Lucky-block rewards such as the Amethyst Block's {@code gems give} command are paid through
     * X-Prison's currency API but are intentionally absent from {@link MiningStats}. Keep a
     * cumulative side-channel for those payouts so the HUD can show the same currencies the player
     * actually receives.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCurrencyReceive(PlayerCurrencyReceiveEvent event) {
        if (event.getPlayer() == null || event.getCurrency() == null
                || !"gems".equalsIgnoreCase(event.getCurrency().getName())) {
            return;
        }
        BigDecimal amount = event.getAmountExact();
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        UUID playerId = event.getPlayer().getUniqueId();
        supplementalGems.merge(playerId, amount.doubleValue(), Double::sum);
        supplementalGemTypes.put(playerId, event.getCurrency());
    }

    private void tick() {
        if (!PrisonScoreboardStats.xPrisonEnabled()) {
            return;
        }
        dev.drawethree.xprison.api.miningstats.XPrisonMiningStatsAPI miningStatsApi;
        try {
            miningStatsApi = XPrisonAPI.getInstance().getMiningStatsApi();
        } catch (RuntimeException | LinkageError ex) {
            return;
        }
        if (miningStatsApi == null) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            MiningStats stats;
            try {
                stats = miningStatsApi.getStats(player);
            } catch (RuntimeException ex) {
                continue;
            }
            if (stats == null) {
                continue;
            }

            UUID id = player.getUniqueId();
            Deque<Sample> deque = history.computeIfAbsent(id, k -> new ArrayDeque<>());
            Sample previous = deque.peekLast();
            Map<XPrisonCurrency, Double> currencies = new HashMap<>();
            if (stats.getCurrenciesEarned() != null) {
                currencies.putAll(stats.getCurrenciesEarned());
            }
            XPrisonCurrency gemCurrency = supplementalGemTypes.get(id);
            Double extraGems = supplementalGems.get(id);
            if (gemCurrency != null && extraGems != null && extraGems > 0.0D) {
                currencies.merge(gemCurrency, extraGems, Double::sum);
            }
            Sample sample = new Sample(stats.getBlocksMined(), currencies);
            deque.addLast(sample);
            while (deque.size() > WINDOW_SECONDS) {
                deque.removeFirst();
            }

            int bps = previous == null ? 0 : Math.max(0, sample.blocksMined - previous.blocksMined);
            if (bps > 0) {
                lastActiveMillis.put(id, now);
            }
            Long lastActive = lastActiveMillis.get(id);
            if (lastActive == null || now - lastActive > IDLE_HIDE_MILLIS) {
                continue;
            }

            Sample earliest = deque.peekFirst();
            double coinsPerMin = earnedSince(earliest, sample, "money");
            double gemsPerMin = earnedSince(earliest, sample, "gems");
            double tokensPerMin = earnedSince(earliest, sample, "tokens");

            player.sendActionBar(buildBar(coinsPerMin, gemsPerMin, tokensPerMin, bps));
        }

        // Drop history for players who went offline so this doesn't leak.
        history.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        lastActiveMillis.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        supplementalGems.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        supplementalGemTypes.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
    }

    private static double earnedSince(Sample earliest, Sample latest, String currencyName) {
        double delta = amountOf(latest, currencyName) - (earliest == null ? 0.0 : amountOf(earliest, currencyName));
        return Math.max(0.0, delta);
    }

    private static double amountOf(Sample sample, String currencyName) {
        for (Map.Entry<XPrisonCurrency, Double> entry : sample.currenciesEarned.entrySet()) {
            if (entry.getKey() != null && currencyName.equalsIgnoreCase(entry.getKey().getName())) {
                return entry.getValue() == null ? 0.0 : entry.getValue();
            }
        }
        return 0.0;
    }

    /**
     * Builds every piece as a child of a neutral, font-less root - NOT by starting the chain
     * with {@code icon(...)} itself. Adventure style inheritance flows from a component down to
     * its own children, so if the first {@code icon(...)} call were the root, every later
     * {@code .append()} would become ITS child and inherit {@code nexo:default} too - which is
     * exactly what happened before this fix: the digits/slashes have no glyph in that font, so
     * the numbers rendered as tofu and only the actual icon characters showed correctly.
     */
    private static Component buildBar(double coinsPerMin, double gemsPerMin, double tokensPerMin, int bps) {
        // Icons stay WHITE (untinted) so the texture's real colours show - a coloured tint here
        // multiplies onto the texture and shifts it (a golden coin tinted green looks wrong, a
        // purple orb tinted aqua looks blue). Only the number text is coloured per currency,
        // matching CurrencyMessageUtil's existing convention (YELLOW coins, LIGHT_PURPLE gems).
        return Component.empty()
                .append(icon(ICON_COINS))
                .append(Component.text(PrisonScoreboardStats.formatShort(coinsPerMin) + "/m  ", NamedTextColor.GOLD))
                .append(icon(ICON_TOKENS))
                .append(Component.text(PrisonScoreboardStats.formatShort(tokensPerMin) + "/m  ", NamedTextColor.GREEN))
                .append(icon(ICON_GEMS))
                .append(Component.text(PrisonScoreboardStats.formatShort(gemsPerMin) + "/m  ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(bps + " BPS", NamedTextColor.DARK_AQUA));
    }

    private static Component icon(char glyph) {
        return Component.text(String.valueOf(glyph)).font(NEXO_DEFAULT_FONT).color(NamedTextColor.WHITE);
    }

    private record Sample(int blocksMined, Map<XPrisonCurrency, Double> currenciesEarned) {
    }
}
