package me.nakilex.levelplugin.player.battlepass;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassEntry;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassReward;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassRewardContext;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassRewardDefinition;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassRewardDefinition.TransmogUnlock;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassView;
import me.nakilex.levelplugin.player.battlepass.gui.BattlePassGUI;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.transmog.TransmogManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Primary manager for the battle pass system.  The manager owns the season
 * definition, reward catalogue, player progress state, and it implements the
 * {@link BattlePassProvider} interface consumed by the GUI.
 */
public class BattlePassManager implements BattlePassProvider {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withLocale(Locale.US).withZone(ZoneId.systemDefault());

    private final Main plugin;
    private final QuestManager questManager;
    private final ItemManager itemManager;
    private final FastTravelManager fastTravelManager;
    private final TransmogManager transmogManager;
    private final ProfileManager profileManager;
    private final BattlePassGUI gui;
    private final BattlePassRewardContext rewardContext;
    private final List<TierDefinition> tiers;
    private final Map<UUID, PlayerProgress> progressMap = new HashMap<>();
    private final String seasonLabel;
    private final Instant seasonEnds;

    public BattlePassManager(Main plugin,
                             QuestManager questManager,
                             ItemManager itemManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.itemManager = itemManager;
        this.fastTravelManager = plugin.getFastTravelManager();
        this.transmogManager = plugin.getTransmogManager();
        this.profileManager = ProfileManager.getInstance();
        this.seasonLabel = "Season of Arcane Echoes";
        this.seasonEnds = Instant.now().plus(45, ChronoUnit.DAYS);
        this.tiers = Collections.unmodifiableList(buildTierDefinitions());
        this.rewardContext = new BattlePassRewardContext(itemManager, fastTravelManager, transmogManager);
        this.gui = new BattlePassGUI(this);
    }

    public BattlePassGUI getGui() {
        return gui;
    }

    public void openMenu(Player player) {
        gui.open(player);
    }

    public void addProgress(Player player, int amount) {
        if (amount <= 0) return;
        PlayerProgress progress = progress(player.getUniqueId());
        if (progress.tier >= tiers.size()) {
            return;
        }
        progress.progress += amount;
        boolean leveled = false;
        while (progress.tier < tiers.size()) {
            int needed = xpRequiredForTier(progress.tier + 1);
            if (needed <= 0 || progress.progress < needed) {
                break;
            }
            progress.progress -= needed;
            progress.tier++;
            leveled = true;
            sendLevelUpMessage(player, progress.tier, progress.progress, xpRequiredForTier(progress.tier + 1));
        }
        if (progress.tier >= tiers.size()) {
            progress.tier = tiers.size();
            progress.progress = 0;
        }
        if (leveled) {
            gui.refresh();
        }
        persist(player.getUniqueId());
    }

    public void setPremium(UUID uuid, boolean active) {
        PlayerProgress progress = progress(uuid);
        progress.premiumActive = active;
        gui.refresh();
        persist(uuid);
    }

    @Override
    public BattlePassView view(UUID playerId) {
        PlayerProgress progress = progress(playerId);
        List<BattlePassEntry> entries = new ArrayList<>();
        int claimedFree = 0;
        int claimedPremium = 0;
        for (TierDefinition def : tiers) {
            boolean freeClaimed = progress.claimedFree.contains(def.tier());
            boolean premiumClaimed = progress.claimedPremium.contains(def.tier());
            if (freeClaimed) claimedFree++;
            if (premiumClaimed) claimedPremium++;
            boolean freeClaimable = def.tier() <= progress.tier;
            boolean premiumClaimable = def.tier() <= progress.tier && progress.premiumActive;
            BattlePassReward freeReward = new BattlePassReward(
                    def.free().resolveDisplayName(rewardContext),
                    def.free().tooltipLines(rewardContext),
                    freeClaimed,
                    freeClaimable
            );
            BattlePassReward premiumReward = new BattlePassReward(
                    def.premium().resolveDisplayName(rewardContext),
                    def.premium().tooltipLines(rewardContext),
                    premiumClaimed,
                    premiumClaimable
            );
            entries.add(new BattlePassEntry(def.tier(), freeReward, premiumReward));
        }
        int required = progress.tier >= tiers.size() ? 0 : xpRequiredForTier(progress.tier + 1);
        int current = required == 0 ? 0 : Math.min(progress.progress, required);
        return new BattlePassView(
                entries,
                progress.tier,
                current,
                required,
                progress.premiumActive,
                seasonLabel,
                formatSeasonEnd(),
                formatTimeRemaining(),
                tiers.size(),
                claimedFree,
                claimedPremium
        );
    }

    @Override
    public void claimReward(Player player, int tier, boolean premiumReward) {
        PlayerProgress progress = progress(player.getUniqueId());
        if (tier > progress.tier) {
            ChatMessageUtil.send(player, MessageType.ERROR, "You have not unlocked this tier yet.");
            return;
        }
        TierDefinition definition = tierDefinition(tier).orElse(null);
        if (definition == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "That reward is not available.");
            return;
        }
        Set<Integer> claimedSet = premiumReward ? progress.claimedPremium : progress.claimedFree;
        if (claimedSet.contains(tier)) {
            ChatMessageUtil.send(player, MessageType.INFO, "You have already claimed this reward.");
            return;
        }
        if (premiumReward && !progress.premiumActive) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Activate the premium pass to claim this reward.");
            return;
        }
        BattlePassRewardDefinition rewardDefinition = premiumReward ? definition.premium() : definition.free();
        QuestReward questReward = rewardDefinition.toQuestReward();
        if (questReward != null) {
            questManager.applyReward(player, questReward);
        }
        if (!rewardDefinition.essences().isEmpty()) {
            grantEssences(player, rewardDefinition.essences());
        }
        if (rewardDefinition.profileSlots() > 0) {
            grantProfileSlots(player.getUniqueId(), rewardDefinition.profileSlots());
        }
        if (!rewardDefinition.fastTravelUnlocks().isEmpty()) {
            grantFastTravelUnlocks(player, rewardDefinition.fastTravelUnlocks());
        }
        if (!rewardDefinition.transmogs().isEmpty()) {
            grantTransmogUnlocks(player.getUniqueId(), rewardDefinition.transmogs());
        }
        claimedSet.add(tier);
        ChatMessageUtil.send(
                player,
                MessageType.REWARD,
                "Claimed Tier " + tier + (premiumReward ? " Premium" : " Free") + " reward!"
        );
        List<String> summary = rewardDefinition.plainSummary(rewardContext);
        if (!summary.isEmpty()) {
            ChatMessageUtil.send(
                    player,
                    MessageType.INFO,
                    ChatColor.GRAY + "Rewards: " + ChatColor.YELLOW + String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, summary)
            );
        }
        gui.refresh();
        persist(player.getUniqueId());
    }

    @Override
    public void handleBack(Player player) {
        ChatMessageUtil.send(player, MessageType.INFO, "Use /battlepass to reopen the menu anytime.");
    }

    @Override
    public void openReceivedItems(Player player) {
        PlayerProgress progress = progress(player.getUniqueId());
        ChatMessageUtil.send(
                player,
                MessageType.INFO,
                ChatColor.YELLOW + "Free rewards claimed: " + progress.claimedFree.size() + "/" + tiers.size()
        );
        if (progress.premiumActive()) {
            ChatMessageUtil.send(
                    player,
                    MessageType.INFO,
                    ChatColor.AQUA + "Premium rewards claimed: " + progress.claimedPremium.size() + "/" + tiers.size()
            );
        } else {
            ChatMessageUtil.send(player, MessageType.WARNING, "Unlock the premium pass to track premium rewards.");
        }
    }

    public void saveProgress(UUID uuid, FileConfiguration config, String path) {
        PlayerProgress progress = progressMap.get(uuid);
        if (progress == null) return;
        config.set(path + ".tier", progress.tier);
        config.set(path + ".progress", progress.progress);
        config.set(path + ".premium", progress.premiumActive);
        config.set(path + ".claimed.free", new ArrayList<>(progress.claimedFree));
        config.set(path + ".claimed.premium", new ArrayList<>(progress.claimedPremium));
    }

    public void loadProgress(UUID uuid, FileConfiguration config, String path) {
        PlayerProgress progress = progress(uuid);
        progress.tier = clamp(config.getInt(path + ".tier", 0), 0, tiers.size());
        progress.progress = Math.max(0, config.getInt(path + ".progress", 0));
        progress.premiumActive = config.getBoolean(path + ".premium", false);
        progress.claimedFree.clear();
        progress.claimedPremium.clear();
        progress.claimedFree.addAll(config.getIntegerList(path + ".claimed.free"));
        progress.claimedPremium.addAll(config.getIntegerList(path + ".claimed.premium"));
        progress.claimedFree.removeIf(t -> t < 1 || t > tiers.size());
        progress.claimedPremium.removeIf(t -> t < 1 || t > tiers.size());
        int needed = progress.tier >= tiers.size() ? 0 : xpRequiredForTier(progress.tier + 1);
        if (needed > 0) {
            progress.progress = Math.min(progress.progress, needed);
        } else {
            progress.progress = 0;
        }
    }

    private void grantEssences(Player player, List<BattlePassRewardDefinition.EssenceGrant> grants) {
        for (BattlePassRewardDefinition.EssenceGrant grant : grants) {
            for (int i = 0; i < grant.amount(); i++) {
                ItemStack essence = ClassEssence.generateEssence(grant.playerClass(), grant.rarity(), grant.starLevel());
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(essence);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(stack -> player.getWorld().dropItem(player.getLocation(), stack));
                }
            }
        }
    }

    private void grantProfileSlots(UUID uuid, int count) {
        if (profileManager == null || count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            profileManager.unlockNextSlot(uuid);
        }
    }

    private void grantFastTravelUnlocks(Player player, List<String> unlocks) {
        if (fastTravelManager == null || player == null) {
            return;
        }
        for (String id : unlocks) {
            if (id == null || id.isBlank()) continue;
            if (fastTravelManager.isUnlocked(player, id)) {
                continue;
            }
            fastTravelManager.unlock(player, id, true);
        }
    }

    private void grantTransmogUnlocks(UUID uuid, List<TransmogUnlock> unlocks) {
        if (transmogManager == null || uuid == null) {
            return;
        }
        for (TransmogUnlock unlock : unlocks) {
            if (unlock == null) continue;
            if (unlock.weaponType() != null) {
                transmogManager.unlockModel(uuid, unlock.modelId(), unlock.weaponType(), null);
            } else {
                transmogManager.unlockModel(uuid, unlock.modelId(), null, unlock.armorType());
            }
        }
    }

    private void sendLevelUpMessage(Player player, int newTier, int currentProgress, int nextRequired) {
        ChatMessageUtil.send(
                player,
                MessageType.REWARD,
                ChatColor.YELLOW + "Battle Pass Tier " + ChatColor.GOLD + newTier + ChatColor.YELLOW + " unlocked!"
        );
        if (newTier < tiers.size() && nextRequired > 0) {
            ChatMessageUtil.send(
                    player,
                    MessageType.INFO,
                    ChatColor.GRAY + "Progress to Tier " + (newTier + 1) + ": "
                            + TooltipUtil.progressBar(currentProgress, nextRequired, 20)
                            + ChatColor.GRAY + " " + currentProgress + "/" + nextRequired
            );
        } else {
            ChatMessageUtil.send(player, MessageType.INFO, "All battle pass tiers completed!");
        }
    }

    private Optional<TierDefinition> tierDefinition(int tier) {
        return tiers.stream().filter(def -> def.tier() == tier).findFirst();
    }

    private PlayerProgress progress(UUID uuid) {
        return progressMap.computeIfAbsent(uuid, id -> new PlayerProgress());
    }

    private void persist(UUID uuid) {
        if (plugin.getPlayerConfig() != null) {
            plugin.getPlayerConfig().savePlayerData(uuid);
        }
    }

    private String formatSeasonEnd() {
        return DATE_FORMATTER.format(seasonEnds);
    }

    private String formatTimeRemaining() {
        Duration remaining = Duration.between(Instant.now(), seasonEnds);
        if (remaining.isNegative() || remaining.isZero()) {
            return "Ended";
        }
        long days = remaining.toDays();
        remaining = remaining.minus(days, ChronoUnit.DAYS);
        long hours = remaining.toHours();
        remaining = remaining.minus(hours, ChronoUnit.HOURS);
        long minutes = remaining.toMinutes();
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        sb.append(minutes).append("m");
        return sb.toString().trim();
    }

    private int xpRequiredForTier(int tier) {
        if (tier > tiers.size()) {
            return 0;
        }
        // Increasing requirement that scales with the tier number.
        return 1000 + (tier - 1) * 250;
    }

    private List<TierDefinition> buildTierDefinitions() {
        List<TierDefinition> defs = new ArrayList<>();
        int tier = 1;
        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Explorer's Allowance")
                        .coins(750)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Premium Gem Pouch")
                        .gems(25)
                        .xp(500)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .addItem(27)
                        .xp(500)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .coins(1000)
                        .addItem(28)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .gems(10)
                        .addItem(29)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .xp(1500)
                        .addItem(26)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .coins(1250)
                        .addItem(52)
                        .unlockFastTravel("rowan")
                        .build(),
                BattlePassRewardDefinition.builder()
                        .gems(20)
                        .addItem(53)
                        .addItem(54)
                        .unlockWeaponTransmog("witchcaster_animated-staff", WeaponType.WAND)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .xp(2000)
                        .addItem(56)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .coins(2000)
                        .addItem(57)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .gems(25)
                        .addItem(58)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .coins(2500)
                        .xp(2500)
                        .addItem(59)
                        .unlockWeaponTransmog("arctic_knight_animated_weapon_set_bow", WeaponType.BOW)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Mage Essence Cache")
                        .addEssence(PlayerClass.MAGE, ItemRarity.RARE, 1)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Mage Essence")
                        .addEssence(PlayerClass.AWAKMAGE, ItemRarity.EPIC, 2)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Warrior's Essence")
                        .addEssence(PlayerClass.WARRIOR, ItemRarity.RARE, 1)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Warrior Essence")
                        .addEssence(PlayerClass.AWAKWARRIOR, ItemRarity.EPIC, 2)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Archer's Essence")
                        .addEssence(PlayerClass.ARCHER, ItemRarity.RARE, 1)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Archer Essence")
                        .addEssence(PlayerClass.AWAKARCHER, ItemRarity.EPIC, 2)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Rogue's Essence")
                        .addEssence(PlayerClass.ROGUE, ItemRarity.RARE, 1)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Rogue Essence")
                        .addEssence(PlayerClass.AWAKROGUE, ItemRarity.EPIC, 2)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .coins(5000)
                        .addItem(138)
                        .unlockProfileSlot()
                        .build(),
                BattlePassRewardDefinition.builder()
                        .gems(40)
                        .addItem(139)
                        .unlockWeaponTransmog("molten_magma_animated_weapon_set_sword", WeaponType.SWORD)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .xp(5000)
                        .addItem(140)
                        .unlockWeaponTransmog("dwarven_weapon_assortment_sword", WeaponType.SWORD)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .coins(6000)
                        .gems(50)
                        .addItem(141)
                        .unlockClass(PlayerClass.ARCHMAGE)
                        .build()));

        return defs;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class PlayerProgress {
        private int tier;
        private int progress;
        private boolean premiumActive;
        private final Set<Integer> claimedFree = new HashSet<>();
        private final Set<Integer> claimedPremium = new HashSet<>();

        public boolean premiumActive() {
            return premiumActive;
        }
    }

    private record TierDefinition(int tier, BattlePassRewardDefinition free, BattlePassRewardDefinition premium) { }
}
