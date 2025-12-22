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
                coinReward(10, skillName, 500),
                giftReward(20, skillName, "blossom_bundle"),
                statReward(30, skillName, StatType.VIT, 2, "+2 Vitality"),
                coinReward(40, skillName, 1200),
                statReward(50, skillName, StatType.STR, 2, "+2 Strength"),
                giftReward(60, skillName, "heroic_token"),
                coinReward(70, skillName, 2000),
                statReward(80, skillName, StatType.WIL, 3, "+3 Will"),
                giftReward(90, skillName, "adventurers_feast"),
                statReward(100, skillName, StatType.AGI, 3, "+3 Agility"),
                coinReward(110, skillName, 3200),
                statReward(120, skillName, StatType.DEX, 3, "+3 Dexterity"),
                coinReward(130, skillName, 4500),
                statReward(140, skillName, StatType.VIT, 4, "+4 Vitality"),
                giftReward(150, skillName, "blossom_bundle"),
                coinReward(160, skillName, 6000),
                statReward(170, skillName, StatType.TEC, 4, "+4 Technique"),
                giftReward(180, skillName, "heroic_token"),
                coinReward(190, skillName, 7500),
                statReward(200, skillName, StatType.STR, 5, "+5 Strength"),
                giftReward(210, skillName, "adventurers_feast"),
                coinReward(220, skillName, 9000),
                statReward(230, skillName, StatType.AGI, 5, "+5 Agility"),
                coinReward(240, skillName, 10500),
                statReward(250, skillName, StatType.DEX, 5, "+5 Dexterity"),
                coinReward(260, skillName, 12000)
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
