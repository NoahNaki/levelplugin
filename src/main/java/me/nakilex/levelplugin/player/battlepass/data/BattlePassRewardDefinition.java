package me.nakilex.levelplugin.player.battlepass.data;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.utils.NumberUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Definition of an actionable battle pass reward.  The definition is responsible
 * for describing the actual game rewards (coins, gems, items, etc.)
 * so that the manager can both render a tooltip snapshot and grant the reward
 * when claimed.
 */
public final class BattlePassRewardDefinition {

    private final String displayName;
    private final List<String> extraLore;
    private final int xp;
    private final int coins;
    private final int gems;
    private final Map<Integer, Integer> itemIds;
    private final List<DirectItemGrant> directItems;

    private BattlePassRewardDefinition(Builder builder) {
        this.displayName = builder.displayName;
        this.extraLore = List.copyOf(builder.extraLore);
        this.xp = builder.xp;
        this.coins = builder.coins;
        this.gems = builder.gems;
        this.itemIds = Collections.unmodifiableMap(new LinkedHashMap<>(builder.itemIds));
        this.directItems = List.copyOf(builder.directItems);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String displayName() {
        return displayName;
    }

    public List<String> extraLore() {
        return extraLore;
    }

    public int xp() {
        return xp;
    }

    public int coins() {
        return coins;
    }

    public int gems() {
        return gems;
    }

    public Map<Integer, Integer> itemIds() {
        return itemIds;
    }

    public List<DirectItemGrant> directItems() {
        return directItems;
    }

    public boolean hasQuestRewards() {
        return xp > 0 || coins > 0 || gems > 0 || !itemIds.isEmpty();
    }

    public QuestReward toQuestReward() {
        if (!hasQuestRewards()) {
            return null;
        }
        List<Integer> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : itemIds.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                items.add(entry.getKey());
            }
        }
        return new QuestReward(xp, coins, gems, items);
    }

    /**
     * Produce coloured tooltip lines summarising the reward contents.
     */
    public List<String> tooltipLines(BattlePassRewardContext context) {
        ItemManager itemManager = context != null ? context.itemManager() : null;
        List<String> lines = new ArrayList<>();
        if (coins > 0) {
            lines.add(ChatColor.GRAY + "• " + ChatColor.GOLD + NumberUtil.formatCommas(coins)
                    + " <glyph:coins_icon> Coins");
        }
        if (gems > 0) {
            lines.add(ChatColor.GRAY + "• " + ChatColor.LIGHT_PURPLE + NumberUtil.formatCommas(gems)
                    + " <glyph:purple_orb_icon> Gems");
        }
        if (xp > 0) {
            lines.add(ChatColor.GRAY + "• " + ChatColor.AQUA + NumberUtil.formatCommas(xp) + ChatColor.GRAY + " XP");
        }
        for (Map.Entry<Integer, Integer> entry : itemIds.entrySet()) {
            if (itemManager == null) continue;
            CustomItem template = itemManager.getTemplateById(entry.getKey());
            if (template == null) continue;
            String name = template.getName();
            int amount = entry.getValue();
            String suffix = amount > 1 ? ChatColor.GRAY + " x" + amount : "";
            lines.add(ChatColor.GRAY + "• " + ChatColor.WHITE + name + suffix);
        }
        for (DirectItemGrant grant : directItems) {
            String suffix = grant.amount() > 1 ? ChatColor.GRAY + " x" + grant.amount() : "";
            lines.add(ChatColor.GRAY + "• " + ChatColor.WHITE + grant.displayName() + suffix);
        }
        lines.addAll(extraLore);
        return lines;
    }

    /**
     * Produce plain-text summary segments suitable for chat messages.
     */
    public List<String> plainSummary(BattlePassRewardContext context) {
        ItemManager itemManager = context != null ? context.itemManager() : null;
        List<String> segments = new ArrayList<>();
        if (coins > 0) {
            segments.add(NumberUtil.formatCommas(coins) + " Coins");
        }
        if (gems > 0) {
            segments.add(NumberUtil.formatCommas(gems) + " Gems");
        }
        if (xp > 0) {
            segments.add(NumberUtil.formatCommas(xp) + " XP");
        }
        for (Map.Entry<Integer, Integer> entry : itemIds.entrySet()) {
            if (itemManager == null) continue;
            CustomItem template = itemManager.getTemplateById(entry.getKey());
            if (template == null) continue;
            StringBuilder sb = new StringBuilder(template.getName());
            if (entry.getValue() > 1) {
                sb.append(" x").append(entry.getValue());
            }
            segments.add(sb.toString());
        }
        for (DirectItemGrant grant : directItems) {
            if (grant.amount() > 1) {
                segments.add(grant.amount() + "x " + grant.displayName());
            } else {
                segments.add(grant.displayName());
            }
        }
        return segments;
    }

    /**
     * Resolve a neutral display name for the reward when none was explicitly
     * provided in the builder.  The name intentionally omits colour codes so
     * the GUI can apply its own styling.
     */
    public String resolveDisplayName(BattlePassRewardContext context) {
        ItemManager itemManager = context != null ? context.itemManager() : null;
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (coins > 0) {
            return NumberUtil.formatCommas(coins) + " Coins";
        }
        if (gems > 0) {
            return NumberUtil.formatCommas(gems) + " Gems";
        }
        if (!itemIds.isEmpty() && itemManager != null) {
            Integer first = itemIds.keySet().iterator().next();
            CustomItem template = itemManager.getTemplateById(first);
            if (template != null) {
                return template.getName();
            }
        }
        if (!directItems.isEmpty()) {
            DirectItemGrant grant = directItems.get(0);
            if (grant.amount() > 1) {
                return grant.amount() + "x " + grant.displayName();
            }
            return grant.displayName();
        }
        if (xp > 0) {
            return NumberUtil.formatCommas(xp) + " XP";
        }
        return "Reward";
    }

    public static final class Builder {
        private String displayName;
        private final List<String> extraLore = new ArrayList<>();
        private int xp;
        private int coins;
        private int gems;
        private final Map<Integer, Integer> itemIds = new LinkedHashMap<>();
        private final List<DirectItemGrant> directItems = new ArrayList<>();

        public Builder displayName(String name) {
            this.displayName = name;
            return this;
        }

        public Builder descriptionLine(String line) {
            if (line != null && !line.isBlank()) {
                extraLore.add(line);
            }
            return this;
        }

        public Builder xp(int amount) {
            if (amount > 0) {
                this.xp += amount;
            }
            return this;
        }

        public Builder coins(int amount) {
            if (amount > 0) {
                this.coins += amount;
            }
            return this;
        }

        public Builder gems(int amount) {
            if (amount > 0) {
                this.gems += amount;
            }
            return this;
        }

        public Builder addItem(int itemId) {
            return addItem(itemId, 1);
        }

        public Builder addItem(int itemId, int amount) {
            if (amount > 0) {
                itemIds.merge(itemId, amount, Integer::sum);
            }
            return this;
        }

        public Builder directItem(String name, int amount, Supplier<ItemStack> factory) {
            if (factory == null || name == null || name.isBlank() || amount <= 0) {
                return this;
            }
            directItems.add(new DirectItemGrant(name, amount, factory));
            return this;
        }

        public BattlePassRewardDefinition build() {
            return new BattlePassRewardDefinition(this);
        }
    }

    public static final class DirectItemGrant {
        private final String displayName;
        private final int amount;
        private final Supplier<ItemStack> factory;

        public DirectItemGrant(String displayName, int amount, Supplier<ItemStack> factory) {
            this.displayName = ChatColor.stripColor(Objects.requireNonNull(displayName, "displayName"));
            this.amount = Math.max(1, amount);
            this.factory = Objects.requireNonNull(factory, "factory");
        }

        public String displayName() {
            return displayName;
        }

        public int amount() {
            return amount;
        }

        public Supplier<ItemStack> factory() {
            return factory;
        }
    }
}
