package me.nakilex.levelplugin.mercenary;

import me.nakilex.levelplugin.dungeon.DungeonLayout;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.*;

/**
 * Handles mercenary expeditions that leverage the dungeon list. The manager is
 * intentionally lightweight: it tracks timing and success chances while leaving
 * combat to the existing mercenary framework.
 */
public class MercenaryExpeditionManager {
    private final Plugin plugin;
    private final MercenaryAffinityManager affinityManager;
    private final DungeonManager dungeonManager;
    private final EconomyManager economyManager;
    private final Map<String, ExpeditionDefinition> expeditions = new LinkedHashMap<>();
    private final Map<UUID, ActiveExpedition> active = new HashMap<>();

    public MercenaryExpeditionManager(Plugin plugin,
                                      MercenaryAffinityManager affinityManager,
                                      DungeonManager dungeonManager,
                                      EconomyManager economyManager) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        this.dungeonManager = dungeonManager;
        this.economyManager = economyManager;
        reload();
        startTick();
    }

    public void reload() {
        expeditions.clear();
        List<Map.Entry<String, String>> layouts = new ArrayList<>(dungeonManager.getLayoutEntries());
        layouts.sort(Comparator.comparing(entry -> ChatColor.stripColor(entry.getValue())));
        for (Map.Entry<String, String> entry : layouts) {
            ExpeditionDefinition def = toDefinition(entry.getKey(), entry.getValue());
            expeditions.put(def.id(), def);
        }
    }

    public Collection<ExpeditionDefinition> getExpeditions() {
        return expeditions.values();
    }

    public boolean isOnExpedition(UUID playerId) {
        return active.containsKey(playerId);
    }

    public ActiveExpedition getActive(UUID playerId) {
        return active.get(playerId);
    }

    public ActiveExpedition startExpedition(Player player, int npcId, ExpeditionDefinition definition) {
        int gs = affinityManager.getGearScore(npcId);
        double success = successChance(gs, definition.threat());
        int seconds = adjustedDuration(gs, definition.threat(), definition.baseDurationSeconds(),
                affinityManager.getFriendship(player.getUniqueId(), npcId).getLevel());
        ActiveExpedition expedition = new ActiveExpedition(npcId, definition, Instant.now().plusSeconds(seconds), success);
        active.put(player.getUniqueId(), expedition);
        player.sendMessage(ChatColor.GREEN + "Sent mercenary " + npcId + " to " + definition.displayName());
        return expedition;
    }

    public void complete(Player player) {
        ActiveExpedition expedition = active.remove(player.getUniqueId());
        if (expedition == null) {
            return;
        }
        boolean success = Math.random() * 100 <= expedition.getSuccessChance();
        if (success) {
            grantRewards(player, expedition);
            player.sendMessage(ChatColor.GREEN + "Expedition success! Rewards delivered from " + expedition.getDefinition().displayName());
        } else {
            player.sendMessage(ChatColor.RED + "Expedition failed. Your mercenary returns empty handed.");
        }
    }

    public double successChance(int gs, int threat) {
        if (threat <= 0) {
            return 100.0;
        }
        if (gs >= threat) {
            return 100.0;
        }
        double deficit = threat - gs;
        return Math.max(25.0, 100.0 - (deficit * 0.05));
    }

    public int adjustedDuration(int gs, int threat, int baseSeconds, int friendshipLevel) {
        double modifier = 1.0;
        if (gs > threat && threat > 0) {
            double ratio = (double) gs / (double) threat;
            modifier -= Math.min(0.35, (ratio - 1.0) * 0.15);
        }
        if (friendshipLevel >= 4) {
            modifier -= 0.1;
        }
        modifier = Math.max(0.5, modifier);
        return (int) Math.round(baseSeconds * modifier);
    }

    private ExpeditionDefinition toDefinition(String layoutKey, String display) {
        int threat = dungeonManager.getThreatLevel(layoutKey);
        DungeonLayout layout = dungeonManager.getLayout(layoutKey);
        int rooms = layout == null ? 0 : countRooms(layout);
        int baseDuration = estimateDuration(threat, rooms);
        String colored = ChatColor.AQUA + display;
        return new ExpeditionDefinition(layoutKey, colored, threat, baseDuration);
    }

    private int countRooms(DungeonLayout layout) {
        int rooms = 0;
        for (int x = 0; x < DungeonLayout.WIDTH; x++) {
            for (int y = 0; y < DungeonLayout.HEIGHT; y++) {
                if (layout.get(x, y) != me.nakilex.levelplugin.dungeon.RoomType.NONE) {
                    rooms++;
                }
            }
        }
        return rooms;
    }

    private int estimateDuration(int threat, int rooms) {
        double duration = threat * 1.1 + rooms * 20;
        return (int) Math.max(600, Math.round(duration));
    }

    private void grantRewards(Player player, ActiveExpedition expedition) {
        if (economyManager == null) {
            return;
        }
        int friendship = affinityManager.getFriendship(player.getUniqueId(), expedition.getNpcId()).getLevel();
        int coins = rewardFor(expedition.getDefinition().threat(), friendship);
        economyManager.addCoins(player, coins, false);
        player.sendMessage(ChatColor.GOLD + "You received " + ChatColor.YELLOW + NumberUtil.format(coins)
                + ChatColor.GOLD + " coins from the expedition.");
    }

    int rewardFor(int threat, int friendshipLevel) {
        double reward = Math.max(50, threat * 0.4);
        if (friendshipLevel >= 5) {
            reward *= 1.25;
        }
        return (int) Math.round(reward);
    }

    private void startTick() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Instant now = Instant.now();
                Iterator<Map.Entry<UUID, ActiveExpedition>> it = active.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, ActiveExpedition> entry = it.next();
                    if (now.isAfter(entry.getValue().getEndTime())) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            complete(player);
                        }
                        it.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 5);
    }
}
