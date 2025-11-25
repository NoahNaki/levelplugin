package me.nakilex.levelplugin.mercenary;

import me.nakilex.levelplugin.dungeon.DungeonLayout;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import me.nakilex.levelplugin.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
    private final LootChestManager lootChestManager;
    private final Map<String, ExpeditionDefinition> expeditions = new LinkedHashMap<>();
    private final Map<UUID, ActiveExpedition> active = new HashMap<>();
    private final Map<UUID, ExpeditionRewards> pendingRewards = new HashMap<>();

    public MercenaryExpeditionManager(Plugin plugin,
                                      MercenaryAffinityManager affinityManager,
                                      DungeonManager dungeonManager,
                                      EconomyManager economyManager,
                                      LootChestManager lootChestManager) {
        this.plugin = plugin;
        this.affinityManager = affinityManager;
        this.dungeonManager = dungeonManager;
        this.economyManager = economyManager;
        this.lootChestManager = lootChestManager;
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

    public ActiveExpedition startExpedition(Player player, List<Integer> npcIds, ExpeditionDefinition definition) {
        int combinedGs = npcIds.stream().mapToInt(affinityManager::getGearScore).sum();
        double success = successChance(npcIds, definition.threat(), definition.recommendedGearScore());
        int friendship = averageFriendship(player.getUniqueId(), npcIds);
        int seconds = adjustedDuration(combinedGs, definition.threat(), definition.baseDurationSeconds(), friendship);
        ActiveExpedition expedition = new ActiveExpedition(new ArrayList<>(npcIds), definition,
                Instant.now().plusSeconds(seconds), success);
        active.put(player.getUniqueId(), expedition);
        player.sendMessage(ChatColor.GREEN + "Sent mercenaries to " + definition.displayName());
        return expedition;
    }

    public void complete(Player player) {
        ActiveExpedition expedition = active.remove(player.getUniqueId());
        if (expedition == null) {
            return;
        }
        boolean success = Math.random() * 100 <= expedition.getSuccessChance();
        if (success) {
            ExpeditionRewards rewards = generateRewards(player, expedition);
            pendingRewards.merge(player.getUniqueId(), rewards, (existing, added) -> {
                ExpeditionRewards merged = new ExpeditionRewards().coins(existing.coins() + added.coins());
                existing.loot().forEach(merged::addLoot);
                added.loot().forEach(merged::addLoot);
                return merged;
            });
            player.sendMessage(ChatColor.GREEN + "Expedition success! Open the rewards menu to claim loot from "
                    + expedition.getDefinition().displayName());
        } else {
            player.sendMessage(ChatColor.RED + "Expedition failed. Your mercenary returns empty handed.");
        }
    }

    public double successChance(List<Integer> npcIds, int threat, int recommendedGs) {
        if (threat <= 0) {
            return 100.0;
        }
        int totalGs = npcIds.stream().mapToInt(affinityManager::getGearScore).sum();
        double ratio = recommendedGs <= 0 ? 1.0 : (double) totalGs / (double) recommendedGs;
        double base = 40.0 + Math.min(60.0, ratio * 45.0);
        boolean hasTank = npcIds.stream().anyMatch(id -> affinityManager.getRole(id) == MercenaryRole.TANK);
        boolean hasDps = npcIds.stream().anyMatch(id -> affinityManager.getRole(id) == MercenaryRole.DPS);
        boolean hasSupport = npcIds.stream().anyMatch(id -> affinityManager.getRole(id) == MercenaryRole.SUPPORT);
        if (hasTank && hasDps && hasSupport) {
            base += 15.0;
        }
        return Math.min(100.0, Math.max(15.0, base));
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

    public ExpeditionRewards getPendingRewards(UUID playerId) {
        return pendingRewards.get(playerId);
    }

    public void setPendingRewards(UUID playerId, ExpeditionRewards rewards) {
        if (rewards == null) {
            pendingRewards.remove(playerId);
        } else {
            pendingRewards.put(playerId, rewards);
        }
    }

    public void clearPending(UUID playerId) {
        pendingRewards.remove(playerId);
    }

    public int recommendedGearScore(DungeonLayout layout, int threat) {
        int rooms = layout == null ? 0 : countRooms(layout);
        int baseline = Math.max(1200, threat * 450);
        return baseline + rooms * 60;
    }

    private ExpeditionDefinition toDefinition(String layoutKey, String display) {
        int threat = dungeonManager.getThreatLevel(layoutKey);
        DungeonLayout layout = dungeonManager.getLayout(layoutKey);
        int rooms = layout == null ? 0 : countRooms(layout);
        int baseDuration = estimateDuration(threat, rooms);
        int recommendedGs = recommendedGearScore(layout, threat);
        String colored = ChatColor.AQUA + display;
        return new ExpeditionDefinition(layoutKey, colored, threat, baseDuration, recommendedGs);
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

    private ExpeditionRewards generateRewards(Player player, ActiveExpedition expedition) {
        ExpeditionRewards rewards = new ExpeditionRewards();
        int friendship = averageFriendship(player.getUniqueId(), expedition.getNpcIds());
        int coins = rewardFor(expedition.getDefinition().threat(), friendship);
        rewards.coins(coins);
        if (lootChestManager != null) {
            int rolls = Math.max(2, expedition.getDefinition().threat() / 2);
            for (int i = 0; i < rolls; i++) {
                rewards.addLoot(lootChestManager.getRandomLootForTier(expedition.getDefinition().threat(), null, null));
            }
        }
        return rewards;
    }

    int rewardFor(int threat, int friendshipLevel) {
        double reward = Math.max(50, threat * 0.4);
        if (friendshipLevel >= 5) {
            reward *= 1.25;
        }
        return (int) Math.round(reward);
    }

    public int averageFriendship(UUID playerId, List<Integer> npcIds) {
        if (npcIds.isEmpty()) {
            return 1;
        }
        return (int) Math.round(npcIds.stream()
                .map(id -> affinityManager.getFriendship(playerId, id).getLevel())
                .mapToInt(Integer::intValue)
                .average()
                .orElse(1.0));
    }

    public void salvageRemaining(Player player, List<ItemStack> leftovers) {
        if (economyManager == null) {
            return;
        }
        int coins = 0;
        for (ItemStack stack : leftovers) {
            if (stack == null) {
                continue;
            }
            CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(stack);
            if (ci != null) {
                coins += SalvageManager.getInstance().getSellPrice(ci);
            }
        }
        if (coins > 0) {
            economyManager.addCoins(player, coins, false);
            player.sendMessage(ChatColor.YELLOW + "Unused loot was salvaged for "
                    + ChatColor.GOLD + NumberUtil.formatCommas(coins) + ChatColor.YELLOW + " coins.");
        }
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
