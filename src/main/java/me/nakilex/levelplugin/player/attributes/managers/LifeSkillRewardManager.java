package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.player.attributes.lifeskill.LifeSkillProgression;
import me.nakilex.levelplugin.player.attributes.lifeskill.LifeSkillRegistry;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Centralises life skill reward milestones and claims so both mining and farming
 * can share the same structures. Rewards are defined once per discipline and
 * can be persisted through {@link PlayerConfig}.
 */
public class LifeSkillRewardManager {

    private static LifeSkillRewardManager instance;

    private final Map<ToolDiscipline, List<LifeSkillReward>> rewards = new EnumMap<>(ToolDiscipline.class);
    private final Map<ToolDiscipline, Map<UUID, Set<Integer>>> claimed = new EnumMap<>(ToolDiscipline.class);

    private final Main plugin;
    private final EconomyManager economyManager;
    private final MiningManager miningManager;
    private final FarmingManager farmingManager;
    private final FishingManager fishingManager;
    private final Supplier<MercenaryAffinityManager> affinitySupplier;
    private final Map<ToolDiscipline, LifeSkillProgression> progressionByDiscipline;

    public LifeSkillRewardManager(Main plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.miningManager = plugin.getMiningManager();
        this.farmingManager = plugin.getFarmingManager();
        this.fishingManager = plugin.getFishingManager();
        this.affinitySupplier = plugin::getMercenaryAffinityManager;
        this.progressionByDiscipline = LifeSkillRegistry.progressions(plugin);
        instance = this;

        initialiseRewards();
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            claimed.put(discipline, new HashMap<>());
        }
    }

    public static LifeSkillRewardManager getInstance() {
        return instance;
    }

    private void initialiseRewards() {
        rewards.put(ToolDiscipline.FARMING, createRewardList("Farming"));
        rewards.put(ToolDiscipline.MINING, createRewardList("Mining"));
        rewards.put(ToolDiscipline.FISHING, createRewardList("Fishing"));
        rewards.put(ToolDiscipline.WOODCUTTING, createRewardList("Woodcutting"));
    }

    private List<LifeSkillReward> createRewardList(String skillName) {
        List<LifeSkillReward> list = new ArrayList<>();
        list.addAll(List.of(
                coinReward(1, skillName, 200),
                giftReward(2, skillName, "blossom_bundle"),
                statReward(3, skillName, StatType.VIT, 1, "+1 Vitality"),
                coinReward(4, skillName, 400),
                statReward(5, skillName, StatType.STR, 1, "+1 Strength"),
                giftReward(6, skillName, "heroic_token"),
                coinReward(7, skillName, 600),
                statReward(8, skillName, StatType.WIL, 1, "+1 Will"),
                giftReward(9, skillName, "adventurers_feast"),
                statReward(10, skillName, StatType.AGI, 1, "+1 Agility"),
                coinReward(11, skillName, 800),
                statReward(12, skillName, StatType.DEX, 1, "+1 Dexterity"),
                coinReward(13, skillName, 1000),
                statReward(14, skillName, StatType.VIT, 2, "+2 Vitality"),
                giftReward(15, skillName, "blossom_bundle"),
                coinReward(16, skillName, 1200),
                statReward(17, skillName, StatType.WIL, 2, "+2 Will"),
                giftReward(18, skillName, "heroic_token"),
                coinReward(19, skillName, 1400),
                statReward(20, skillName, StatType.STR, 3, "+3 Strength"),
                giftReward(21, skillName, "adventurers_feast"),
                coinReward(22, skillName, 1600),
                statReward(23, skillName, StatType.AGI, 3, "+3 Agility"),
                coinReward(24, skillName, 1800)
        ));

        List<StatType> statCycle = List.of(StatType.VIT, StatType.STR, StatType.AGI, StatType.DEX, StatType.WIL);
        String[] gifts = {"blossom_bundle", "heroic_token", "adventurers_feast"};
        int statIndex = 0;
        int giftIndex = 0;
        for (int level = 25; level <= 100; level++) {
            if (level % 15 == 0) {
                list.add(giftReward(level, skillName, gifts[giftIndex++ % gifts.length]));
                continue;
            }
            if (level % 5 == 0) {
                StatType stat = statCycle.get(statIndex++ % statCycle.size());
                int amount = level >= 50 ? 3 : 2;
                list.add(statReward(level, skillName, stat, amount, "+" + amount + " " + statLabel(stat)));
                continue;
            }
            int coins = 1800 + (level - 24) * 100;
            list.add(coinReward(level, skillName, coins));
        }
        return list;
    }

    private LifeSkillReward coinReward(int level, String skillName, int coins) {
        String title = ChatColor.GOLD + skillName + " Level " + level;
        String rewardLine = ChatColor.GREEN + "• " + ChatColor.WHITE + "+" + coins + " <glyph:coins_icon>";
        return new LifeSkillReward(level, title, List.of(ChatColor.GRAY + "Rewards:", rewardLine),
                player -> economyManager.addCoins(player, coins, false));
    }

    private LifeSkillReward statReward(int level, String skillName, StatType stat, int amount, String label) {
        String title = ChatColor.GOLD + skillName + " Level " + level;
        List<String> lore = List.of(
                ChatColor.GRAY + "Rewards:",
                ChatColor.GREEN + "• " + ChatColor.WHITE + label
        );
        return new LifeSkillReward(level, title, lore,
                player -> StatsManager.getInstance().addBaseStat(player.getUniqueId(), stat, amount));
    }

    private LifeSkillReward giftReward(int level, String skillName, String giftId) {
        String title = ChatColor.GOLD + skillName + " Level " + level;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Rewards:");
        lore.add(ChatColor.GREEN + "• " + ChatColor.WHITE + "Friendship Gift");
        lore.add(ChatColor.DARK_GRAY + "   – " + ChatColor.WHITE + prettyGiftName(giftId));
        return new LifeSkillReward(level, title, lore, player -> {
            MercenaryAffinityManager affinityManager = affinitySupplier.get();
            if (affinityManager == null) {
                plugin.getLogger().warning("MercenaryAffinityManager is not ready; skipping gift reward for " + player.getName());
                return;
            }

            ItemStack gift = affinityManager.createGiftItem(giftId);
            if (gift != null) {
                player.getInventory().addItem(gift);
            }
        });
    }

    private String statLabel(StatType stat) {
        return switch (stat) {
            case VIT -> "Vitality";
            case STR -> "Strength";
            case AGI -> "Agility";
            case INT -> "Intelligence";
            case DEX -> "Dexterity";
            case WIL -> "Will";
            case TEC -> "Technique";
        };
    }

    private String prettyGiftName(String giftId) {
        String[] parts = giftId.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            builder.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1));
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    public List<LifeSkillReward> getRewards(ToolDiscipline discipline) {
        return rewards.getOrDefault(discipline, List.of());
    }

    public boolean isClaimed(UUID uuid, ToolDiscipline discipline, int levelRequired) {
        return claimed.getOrDefault(discipline, Map.of())
                .getOrDefault(uuid, Set.of())
                .contains(levelRequired);
    }

    public void setClaimed(UUID uuid, ToolDiscipline discipline, Set<Integer> claimedLevels) {
        claimed.computeIfAbsent(discipline, d -> new HashMap<>()).put(uuid, new HashSet<>(claimedLevels));
    }

    public Set<Integer> getClaimed(UUID uuid, ToolDiscipline discipline) {
        return new HashSet<>(claimed.getOrDefault(discipline, Map.of()).getOrDefault(uuid, Set.of()));
    }

    public void saveClaim(UUID uuid, ToolDiscipline discipline, int levelRequired) {
        claimed.computeIfAbsent(discipline, d -> new HashMap<>())
                .computeIfAbsent(uuid, u -> new HashSet<>())
                .add(levelRequired);
    }

    public boolean claimReward(Player player, ToolDiscipline discipline, LifeSkillReward reward) {
        UUID uuid = player.getUniqueId();
        if (reward == null) {
            return false;
        }
        return claimRewardsUpTo(player, discipline, reward.levelRequired());
    }

    public boolean claimRewardsUpTo(Player player, ToolDiscipline discipline, int targetLevel) {
        UUID uuid = player.getUniqueId();
        int currentLevel = getLevel(discipline, uuid);
        if (currentLevel < targetLevel) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Reach level " + targetLevel + " to claim this reward.");
            return false;
        }

        List<LifeSkillReward> rewardsForSkill = getRewards(discipline);
        List<LifeSkillReward> newlyClaimed = new ArrayList<>();
        for (LifeSkillReward reward : rewardsForSkill) {
            if (reward.levelRequired() > targetLevel || reward.levelRequired() > currentLevel) {
                continue;
            }
            if (isClaimed(uuid, discipline, reward.levelRequired())) {
                continue;
            }
            reward.rewardAction().accept(player);
            saveClaim(uuid, discipline, reward.levelRequired());
            newlyClaimed.add(reward);
        }

        if (newlyClaimed.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You've already claimed these rewards.");
            return false;
        }

        newlyClaimed.sort(Comparator.comparingInt(LifeSkillReward::levelRequired));
        String skillName = discipline.name().substring(0, 1).toUpperCase() + discipline.name().substring(1).toLowerCase();
        String message = "Claimed " + ChatColor.YELLOW + skillName + ChatColor.WHITE
                + " rewards for level " + ChatColor.YELLOW + targetLevel + ChatColor.WHITE;
        if (newlyClaimed.size() > 1) {
            message += " (including previous levels)";
        }
        message += ".";
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD, message);
        ChatFormatter.sendIndentedMessage(player, ChatColor.GREEN + "Rewards:");
        for (String line : buildRewardLines(newlyClaimed)) {
            ChatFormatter.sendIndentedMessage(player, line);
        }
        return true;
    }

    private List<String> buildRewardLines(List<LifeSkillReward> rewards) {
        List<String> lines = new ArrayList<>();
        for (LifeSkillReward reward : rewards) {
            for (String line : reward.lore()) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String stripped = ChatColor.stripColor(line).trim();
                if (stripped.equalsIgnoreCase("Rewards:")) {
                    continue;
                }
                if (stripped.isEmpty()) {
                    continue;
                }
                if (stripped.startsWith("–")) {
                    lines.add(ChatColor.DARK_GRAY + "  " + line.trim());
                    continue;
                }
                String cleaned = line.replace("• ", "").replace("•", "").trim();
                lines.add(ChatColor.GREEN + "- " + cleaned);
            }
        }
        return lines;
    }

    private int getLevel(ToolDiscipline discipline, UUID uuid) {
        LifeSkillProgression progression = progressionByDiscipline.get(discipline);
        return progression == null ? 1 : progression.getLevel(uuid);
    }

    public record LifeSkillReward(int levelRequired, String displayName, List<String> lore, Consumer<Player> rewardAction) {
    }
}
