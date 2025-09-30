package me.nakilex.levelplugin.player.battlepass.data;

import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Definition of an actionable battle pass reward.  The definition is responsible
 * for describing the actual game rewards (coins, gems, items, essences, etc.)
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
    private final List<EssenceGrant> essences;
    private final List<PlayerClass> classUnlocks;
    private final int profileSlots;
    private final List<String> fastTravelUnlocks;
    private final List<TransmogUnlock> transmogs;

    private BattlePassRewardDefinition(Builder builder) {
        this.displayName = builder.displayName;
        this.extraLore = List.copyOf(builder.extraLore);
        this.xp = builder.xp;
        this.coins = builder.coins;
        this.gems = builder.gems;
        this.itemIds = Collections.unmodifiableMap(new LinkedHashMap<>(builder.itemIds));
        this.essences = List.copyOf(builder.essences);
        this.classUnlocks = List.copyOf(builder.classUnlocks);
        this.profileSlots = builder.profileSlots;
        this.fastTravelUnlocks = List.copyOf(builder.fastTravelUnlocks);
        this.transmogs = List.copyOf(builder.transmogs);
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

    public List<EssenceGrant> essences() {
        return essences;
    }

    public List<PlayerClass> classUnlocks() {
        return classUnlocks;
    }

    public int profileSlots() {
        return profileSlots;
    }

    public List<String> fastTravelUnlocks() {
        return fastTravelUnlocks;
    }

    public List<TransmogUnlock> transmogs() {
        return transmogs;
    }

    public boolean hasQuestRewards() {
        return xp > 0 || coins > 0 || gems > 0 || !itemIds.isEmpty() || !classUnlocks.isEmpty();
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
        return new QuestReward(xp, coins, gems, items, classUnlocks);
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
            ItemRarity rarity = template.getRarity();
            ChatColor color = rarity != null ? rarity.getColor() : ChatColor.WHITE;
            int amount = entry.getValue();
            String suffix = amount > 1 ? ChatColor.GRAY + " x" + amount : "";
            lines.add(ChatColor.GRAY + "• " + color + template.getName() + suffix);
        }
        for (EssenceGrant grant : essences) {
            ChatColor color = grant.rarity().getColor();
            String rarityName = TextUtil.beautifyWords(grant.rarity().name());
            String amount = grant.amount() > 1 ? ChatColor.GRAY + " x" + grant.amount() : "";
            String stars = grant.starLevel() > 0
                    ? ChatColor.GOLD + " +" + grant.starLevel() + "★"
                    : "";
            lines.add(ChatColor.GRAY + "• " + color + grant.playerClass().getDisplayName()
                    + ChatColor.GRAY + " Essence" + amount
                    + ChatColor.DARK_GRAY + " (" + rarityName + stars + ChatColor.DARK_GRAY + ")");
        }
        for (PlayerClass clazz : classUnlocks) {
            lines.add(ChatColor.GRAY + "• " + ChatColor.AQUA + "Unlocks Class: "
                    + ChatColor.YELLOW + clazz.getDisplayName());
        }
        if (profileSlots > 0) {
            String slots = profileSlots == 1
                    ? "Unlocks +1 Character Slot"
                    : "Unlocks +" + profileSlots + " Character Slots";
            lines.add(ChatColor.GRAY + "• " + ChatColor.GREEN + slots);
        }
        for (String unlock : fastTravelUnlocks) {
            String name = context != null ? context.fastTravelDisplayName(unlock)
                    : TextUtil.beautifyWords(unlock.replace('-', ' ').replace('_', ' '));
            lines.add(ChatColor.GRAY + "• " + ChatColor.DARK_AQUA + "Fast Travel: "
                    + ChatColor.AQUA + name);
        }
        for (TransmogUnlock unlock : transmogs) {
            String display = context != null ? context.transmogDisplayName(unlock)
                    : TextUtil.beautifyWords(unlock.modelId().replace('-', ' ').replace('_', ' '));
            lines.add(ChatColor.GRAY + "• " + ChatColor.LIGHT_PURPLE + "Transmog Skin: "
                    + ChatColor.DARK_PURPLE + display);
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
        for (EssenceGrant grant : essences) {
            String rarityName = TextUtil.beautifyWords(grant.rarity().name());
            StringBuilder sb = new StringBuilder(rarityName)
                    .append(' ')
                    .append(grant.playerClass().getDisplayName())
                    .append(" Essence");
            if (grant.amount() > 1) {
                sb.append(" x").append(grant.amount());
            }
            if (grant.starLevel() > 0) {
                sb.append(" (+").append(grant.starLevel()).append('★').append(')');
            }
            segments.add(sb.toString());
        }
        for (PlayerClass clazz : classUnlocks) {
            segments.add("Unlocks Class: " + clazz.getDisplayName());
        }
        if (profileSlots > 0) {
            segments.add(profileSlots == 1 ? "+1 Character Slot" : "+" + profileSlots + " Character Slots");
        }
        for (String unlock : fastTravelUnlocks) {
            String name = context != null ? context.fastTravelDisplayName(unlock)
                    : TextUtil.beautifyWords(unlock.replace('-', ' ').replace('_', ' '));
            segments.add("Fast Travel: " + name);
        }
        for (TransmogUnlock unlock : transmogs) {
            String display = context != null ? context.transmogDisplayName(unlock)
                    : TextUtil.beautifyWords(unlock.modelId().replace('-', ' ').replace('_', ' '));
            segments.add("Transmog Skin: " + display);
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
        if (!essences.isEmpty()) {
            return essences.get(0).playerClass().getDisplayName() + " Essence";
        }
        if (!classUnlocks.isEmpty()) {
            return classUnlocks.get(0).getDisplayName() + " Unlock";
        }
        if (profileSlots > 0) {
            return profileSlots == 1 ? "Character Slot Unlock" : profileSlots + " Character Slots";
        }
        if (!fastTravelUnlocks.isEmpty()) {
            String name = context != null ? context.fastTravelDisplayName(fastTravelUnlocks.get(0))
                    : TextUtil.beautifyWords(fastTravelUnlocks.get(0).replace('-', ' ').replace('_', ' '));
            return name + " Fast Travel";
        }
        if (!transmogs.isEmpty()) {
            String display = context != null ? context.transmogDisplayName(transmogs.get(0))
                    : TextUtil.beautifyWords(transmogs.get(0).modelId().replace('-', ' ').replace('_', ' '));
            return display + " Transmog";
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
        private final List<EssenceGrant> essences = new ArrayList<>();
        private final List<PlayerClass> classUnlocks = new ArrayList<>();
        private int profileSlots;
        private final List<String> fastTravelUnlocks = new ArrayList<>();
        private final List<TransmogUnlock> transmogs = new ArrayList<>();

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

        public Builder addEssence(PlayerClass clazz, ItemRarity rarity, int starLevel) {
            return addEssence(clazz, rarity, starLevel, 1);
        }

        public Builder addEssence(PlayerClass clazz, ItemRarity rarity, int starLevel, int amount) {
            if (clazz != null && rarity != null && amount > 0) {
                essences.add(new EssenceGrant(clazz, rarity, Math.max(0, starLevel), amount));
            }
            return this;
        }

        public Builder unlockClass(PlayerClass clazz) {
            if (clazz != null && !classUnlocks.contains(clazz)) {
                classUnlocks.add(clazz);
            }
            return this;
        }

        public Builder unlockProfileSlot() {
            profileSlots++;
            return this;
        }

        public Builder unlockProfileSlots(int count) {
            if (count > 0) {
                profileSlots += count;
            }
            return this;
        }

        public Builder unlockFastTravel(String id) {
            if (id != null && !id.isBlank()) {
                String key = id.trim().toLowerCase();
                if (!fastTravelUnlocks.contains(key)) {
                    fastTravelUnlocks.add(key);
                }
            }
            return this;
        }

        public Builder unlockWeaponTransmog(String modelId, WeaponType type) {
            if (modelId != null && !modelId.isBlank() && type != null) {
                transmogs.add(new TransmogUnlock(modelId.trim(), type, null));
            }
            return this;
        }

        public Builder unlockArmorTransmog(String modelId, ArmorType type) {
            if (modelId != null && !modelId.isBlank() && type != null) {
                transmogs.add(new TransmogUnlock(modelId.trim(), null, type));
            }
            return this;
        }

        public BattlePassRewardDefinition build() {
            return new BattlePassRewardDefinition(this);
        }
    }

    public static final class EssenceGrant {
        private final PlayerClass playerClass;
        private final ItemRarity rarity;
        private final int starLevel;
        private final int amount;

        public EssenceGrant(PlayerClass playerClass, ItemRarity rarity, int starLevel, int amount) {
            this.playerClass = Objects.requireNonNull(playerClass, "playerClass");
            this.rarity = Objects.requireNonNull(rarity, "rarity");
            this.starLevel = Math.max(0, starLevel);
            this.amount = Math.max(1, amount);
        }

        public PlayerClass playerClass() {
            return playerClass;
        }

        public ItemRarity rarity() {
            return rarity;
        }

        public int starLevel() {
            return starLevel;
        }

        public int amount() {
            return amount;
        }
    }

    public static final class TransmogUnlock {
        private final String modelId;
        private final WeaponType weaponType;
        private final ArmorType armorType;

        public TransmogUnlock(String modelId, WeaponType weaponType, ArmorType armorType) {
            this.modelId = Objects.requireNonNull(modelId, "modelId");
            if ((weaponType == null) == (armorType == null)) {
                throw new IllegalArgumentException("Exactly one of weaponType or armorType must be provided");
            }
            this.weaponType = weaponType;
            this.armorType = armorType;
        }

        public String modelId() {
            return modelId;
        }

        public WeaponType weaponType() {
            return weaponType;
        }

        public ArmorType armorType() {
            return armorType;
        }
    }
}
