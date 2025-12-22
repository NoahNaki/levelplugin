package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.farming.managers.FarmingManager;
import me.nakilex.levelplugin.player.mining.managers.MiningManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Centralises life skill reward milestones and claims so both mining and farming
 * can share the same structures. Rewards are defined once per discipline and
 * can be persisted through {@link me.nakilex.levelplugin.player.config.PlayerConfig}.
 */
public class LifeSkillRewardManager {

    private static LifeSkillRewardManager instance;

    private final Map<ToolDiscipline, List<LifeSkillReward>> rewards = new EnumMap<>(ToolDiscipline.class);
    private final Map<ToolDiscipline, Map<UUID, Set<Integer>>> claimed = new EnumMap<>(ToolDiscipline.class);

    private final EconomyManager economyManager;
    private final MiningManager miningManager;
    private final FarmingManager farmingManager;
    private final MercenaryAffinityManager affinityManager;

    public LifeSkillRewardManager(Main plugin) {
        this.economyManager = plugin.getEconomyManager();
        this.miningManager = plugin.getMiningManager();
        this.farmingManager = plugin.getFarmingManager();
        this.affinityManager = plugin.getMercenaryAffinityManager();
        instance = this;

        initialiseRewards();
        for (ToolDiscipline discipline : ToolDiscipline.values()) {
            claimed.put(discipline, new java.util.HashMap<>());
        }
    }

    public static LifeSkillRewardManager getInstance() {
        return instance;
    }

    private void initialiseRewards() {
        rewards.put(ToolDiscipline.FARMING, createRewardList("Farming"));
        rewards.put(ToolDiscipline.MINING, createRewardList("Mining"));
    }

    private List<LifeSkillReward> createRewardList(String skillName) {
        return List.of(
                coinReward(1, skillName, 250),
                giftReward(3, skillName, "blossom_bundle"),
                statReward(5, skillName, StatType.VIT, 2, "+2 Vitality"),
                coinReward(8, skillName, 750),
                giftReward(10, skillName, "heroic_token"),
                statReward(12, skillName, StatType.STR, 2, "+2 Strength"),
                coinReward(15, skillName, 1500),
                statReward(18, skillName, StatType.WIL, 2, "+2 Will"),
                giftReward(20, skillName, "adventurers_feast"),
                statReward(24, skillName, StatType.AGI, 3, "+3 Agility"),
                coinReward(28, skillName, 3000),
                statReward(32, skillName, StatType.DEX, 3, "+3 Dexterity"),
                coinReward(36, skillName, 4500),
                statReward(40, skillName, StatType.VIT, 4, "+4 Vitality"),
                coinReward(45, skillName, 6000),
                statReward(50, skillName, StatType.TEC, 4, "+4 Technique")
        );
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
        List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.GRAY + "Rewards:");
        lore.add(ChatColor.GREEN + "• " + ChatColor.WHITE + "Friendship Gift");
        lore.add(ChatColor.DARK_GRAY + "   – " + ChatColor.WHITE + prettyGiftName(giftId));
        return new LifeSkillReward(level, title, lore, player -> {
            ItemStack gift = affinityManager.createGiftItem(giftId);
            if (gift != null) {
                player.getInventory().addItem(gift);
            }
        });
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
        claimed.computeIfAbsent(discipline, d -> new java.util.HashMap<>()).put(uuid, new HashSet<>(claimedLevels));
    }

    public Set<Integer> getClaimed(UUID uuid, ToolDiscipline discipline) {
        return new HashSet<>(claimed.getOrDefault(discipline, Map.of()).getOrDefault(uuid, Set.of()));
    }

    public void saveClaim(UUID uuid, ToolDiscipline discipline, int levelRequired) {
        claimed.computeIfAbsent(discipline, d -> new java.util.HashMap<>())
                .computeIfAbsent(uuid, u -> new HashSet<>())
                .add(levelRequired);
    }

    public boolean claimReward(Player player, ToolDiscipline discipline, LifeSkillReward reward) {
        UUID uuid = player.getUniqueId();
        if (isClaimed(uuid, discipline, reward.levelRequired())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You've already claimed this reward.");
            return false;
        }

        int currentLevel = getLevel(discipline, uuid);
        if (currentLevel < reward.levelRequired()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Reach level " + reward.levelRequired() + " to claim this reward.");
            return false;
        }

        reward.rewardAction().accept(player);
        saveClaim(uuid, discipline, reward.levelRequired());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Reward claimed!");
        return true;
    }

    private int getLevel(ToolDiscipline discipline, UUID uuid) {
        return switch (discipline) {
            case MINING -> miningManager.getLevel(uuid);
            case FARMING -> farmingManager.getLevel(uuid);
        };
    }

    public record LifeSkillReward(int levelRequired, String displayName, List<String> lore, Consumer<Player> rewardAction) {
    }
}
