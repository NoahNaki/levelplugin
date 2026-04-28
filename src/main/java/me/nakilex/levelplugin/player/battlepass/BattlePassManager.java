package me.nakilex.levelplugin.player.battlepass;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassEntry;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassReward;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassRewardContext;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassRewardDefinition;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassRewardDefinition.DirectItemGrant;
import me.nakilex.levelplugin.player.battlepass.data.BattlePassView;
import me.nakilex.levelplugin.player.battlepass.gui.BattlePassGUI;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.SealingCharm;
import me.nakilex.levelplugin.player.mining.items.MiningMaterial;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import java.util.function.Supplier;

/**
 * Primary manager for the battle pass system.  The manager owns the season
 * definition, reward catalogue, player progress state, and it implements the
 * {@link BattlePassProvider} interface consumed by the GUI.
 */
public class BattlePassManager implements BattlePassProvider {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy").withLocale(Locale.US).withZone(ZoneId.systemDefault());

    private static final int[] HORSE_DISTANCE_MILESTONES_METERS = {250, 1000, 2500, 5000};
    private static final int[] HORSE_DISTANCE_MILESTONE_XP = {100, 220, 360, 520};
    private static final int[] HORSE_JUMP_MILESTONES = {25, 75, 150, 300};
    private static final int[] HORSE_JUMP_MILESTONE_XP = {80, 160, 280, 420};

    private final Main plugin;
    private final QuestManager questManager;
    private final ItemManager itemManager;
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
        this.seasonLabel = "Season of Arcane Echoes";
        this.seasonEnds = Instant.now().plus(45, ChronoUnit.DAYS);
        this.tiers = Collections.unmodifiableList(buildTierDefinitions());
        this.rewardContext = new BattlePassRewardContext(itemManager);
        this.gui = new BattlePassGUI(this);
    }

    public BattlePassGUI getGui() {
        return gui;
    }

    public void openMenu(Player player) {
        gui.open(player);
    }

    public void addProgress(Player player, int amount) {
        addProgress(player, amount, null);
    }

    public void addProgress(Player player, int amount, String reason) {
        if (player == null) {
            return;
        }
        addProgressInternal(player, amount);
    }

    private boolean addProgressInternal(Player player, int amount) {
        if (player == null || amount <= 0) {
            return false;
        }
        PlayerProgress progress = progress(player.getUniqueId());
        if (progress.tier >= tiers.size()) {
            return false;
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
        return true;
    }

    public boolean setPremium(UUID uuid, boolean active) {
        PlayerProgress progress = progress(uuid);
        if (progress.premiumActive == active) {
            return false;
        }
        progress.premiumActive = active;
        gui.refresh();
        persist(uuid);
        return true;
    }

    public boolean hasPremium(UUID uuid) {
        return progress(uuid).premiumActive();
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
        claimRewardInternal(player, tier, premiumReward, true, false);
    }

    @Override
    public int claimAllAvailable(Player player) {
        if (player == null) {
            return 0;
        }
        PlayerProgress progress = progress(player.getUniqueId());
        int claimed = 0;
        for (int tier = 1; tier <= progress.tier; tier++) {
            if (claimRewardInternal(player, tier, false, false, true)) {
                claimed++;
            }
            if (progress.premiumActive && claimRewardInternal(player, tier, true, false, true)) {
                claimed++;
            }
        }
        if (claimed > 0) {
            ChatMessageUtil.send(player, MessageType.SUCCESS,
                    "Claimed " + ChatColor.YELLOW + claimed + ChatColor.GREEN + " battle pass rewards.");
            gui.refresh();
            persist(player.getUniqueId());
        } else {
            ChatMessageUtil.send(player, MessageType.INFO, "No claimable rewards right now.");
        }
        return claimed;
    }

    private boolean claimRewardInternal(Player player,
                                        int tier,
                                        boolean premiumReward,
                                        boolean notify,
                                        boolean deferRefreshPersist) {
        if (player == null) {
            return false;
        }
        PlayerProgress progress = progress(player.getUniqueId());
        if (tier > progress.tier) {
            if (notify) {
                ChatMessageUtil.send(player, MessageType.ERROR, "You have not unlocked this tier yet.");
            }
            return false;
        }
        TierDefinition definition = tierDefinition(tier).orElse(null);
        if (definition == null) {
            if (notify) {
                ChatMessageUtil.send(player, MessageType.ERROR, "That reward is not available.");
            }
            return false;
        }
        Set<Integer> claimedSet = premiumReward ? progress.claimedPremium : progress.claimedFree;
        if (claimedSet.contains(tier)) {
            if (notify) {
                ChatMessageUtil.send(player, MessageType.INFO, "You have already claimed this reward.");
            }
            return false;
        }
        if (premiumReward && !progress.premiumActive) {
            if (notify) {
                ChatMessageUtil.send(player, MessageType.ERROR, "Activate the premium pass to claim this reward.");
            }
            return false;
        }
        BattlePassRewardDefinition rewardDefinition = premiumReward ? definition.premium() : definition.free();
        QuestReward questReward = rewardDefinition.toQuestReward();
        if (questReward != null) {
            questManager.applyReward(player, questReward);
        }
        if (!rewardDefinition.directItems().isEmpty()) {
            grantDirectItems(player, rewardDefinition.directItems());
        }
        claimedSet.add(tier);
        if (notify) {
            ChatMessageUtil.send(
                    player,
                    MessageType.REWARD,
                    "Claimed Tier " + tier + (premiumReward ? " Premium" : " Free") + " reward!"
            );
            List<String> summary = rewardDefinition.formattedSummary(rewardContext);
            if (!summary.isEmpty()) {
                ChatMessageUtil.send(
                        player,
                        MessageType.INFO,
                        ChatColor.GRAY + "Rewards: " + String.join(ChatColor.GRAY + ", ", summary)
                );
            }
        }
        if (!deferRefreshPersist) {
            gui.refresh();
            persist(player.getUniqueId());
        }
        return true;
    }

    @Override
    public void handleBack(Player player) {
        ChatMessageUtil.send(player, MessageType.INFO, "Use /battlepass to reopen the menu anytime.");
    }

    @Override
    public List<String> activeChallenges(UUID playerId) {
        PlayerProgress progress = progress(playerId);
        List<String> lines = new ArrayList<>();
        int distanceStage = Math.min(progress.horseDistanceStage, HORSE_DISTANCE_MILESTONES_METERS.length - 1);
        int jumpStage = Math.min(progress.horseJumpStage, HORSE_JUMP_MILESTONES.length - 1);

        if (progress.horseDistanceStage >= HORSE_DISTANCE_MILESTONES_METERS.length) {
            lines.add(ChatColor.GREEN + "• Horse Travel: Complete");
        } else {
            int target = HORSE_DISTANCE_MILESTONES_METERS[distanceStage];
            int current = (int) Math.floor(progress.horseDistanceMeters);
            lines.add(ChatColor.YELLOW + "• Travel " + target + "m while mounted: "
                    + ChatColor.WHITE + Math.min(current, target) + ChatColor.GRAY + "/" + ChatColor.WHITE + target);
        }

        if (progress.horseJumpStage >= HORSE_JUMP_MILESTONES.length) {
            lines.add(ChatColor.GREEN + "• Horse Jumping: Complete");
        } else {
            int target = HORSE_JUMP_MILESTONES[jumpStage];
            lines.add(ChatColor.YELLOW + "• Complete " + target + " mounted jumps: "
                    + ChatColor.WHITE + Math.min(progress.horseJumpCount, target) + ChatColor.GRAY + "/" + ChatColor.WHITE + target);
        }
        return lines;
    }

    public void recordHorseChallengeProgress(Player player, double distanceMeters, int jumps) {
        if (player == null || (!Double.isFinite(distanceMeters) && jumps <= 0)) {
            return;
        }
        PlayerProgress progress = progress(player.getUniqueId());
        if (Double.isFinite(distanceMeters) && distanceMeters > 0) {
            progress.horseDistanceMeters += distanceMeters;
            while (progress.horseDistanceStage < HORSE_DISTANCE_MILESTONES_METERS.length
                    && progress.horseDistanceMeters >= HORSE_DISTANCE_MILESTONES_METERS[progress.horseDistanceStage]) {
                int stage = progress.horseDistanceStage;
                int xp = HORSE_DISTANCE_MILESTONE_XP[stage];
                addProgress(player, xp, "for horse travel challenge");
                ChatMessageUtil.send(player, MessageType.SUCCESS,
                        "Horse challenge complete: traveled " + HORSE_DISTANCE_MILESTONES_METERS[stage]
                                + "m mounted (" + ChatColor.YELLOW + "+" + xp + " Battle Pass XP" + ChatColor.GREEN + ").");
                progress.horseDistanceStage++;
            }
        }

        if (jumps > 0) {
            progress.horseJumpCount += jumps;
            while (progress.horseJumpStage < HORSE_JUMP_MILESTONES.length
                    && progress.horseJumpCount >= HORSE_JUMP_MILESTONES[progress.horseJumpStage]) {
                int stage = progress.horseJumpStage;
                int xp = HORSE_JUMP_MILESTONE_XP[stage];
                addProgress(player, xp, "for horse jumping challenge");
                ChatMessageUtil.send(player, MessageType.SUCCESS,
                        "Horse challenge complete: performed " + HORSE_JUMP_MILESTONES[stage]
                                + " mounted jumps (" + ChatColor.YELLOW + "+" + xp + " Battle Pass XP" + ChatColor.GREEN + ").");
                progress.horseJumpStage++;
            }
        }

        persist(player.getUniqueId());
    }

    public void saveProgress(UUID uuid, FileConfiguration config, String path) {
        PlayerProgress progress = progressMap.get(uuid);
        if (progress == null) return;
        config.set(path + ".tier", progress.tier);
        config.set(path + ".progress", progress.progress);
        config.set(path + ".premium", progress.premiumActive);
        config.set(path + ".claimed.free", new ArrayList<>(progress.claimedFree));
        config.set(path + ".claimed.premium", new ArrayList<>(progress.claimedPremium));
        config.set(path + ".horse.distance", progress.horseDistanceMeters);
        config.set(path + ".horse.distance-stage", progress.horseDistanceStage);
        config.set(path + ".horse.jumps", progress.horseJumpCount);
        config.set(path + ".horse.jump-stage", progress.horseJumpStage);
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
        progress.horseDistanceMeters = Math.max(0.0, config.getDouble(path + ".horse.distance", 0.0));
        progress.horseDistanceStage = clamp(config.getInt(path + ".horse.distance-stage", 0), 0, HORSE_DISTANCE_MILESTONES_METERS.length);
        progress.horseJumpCount = Math.max(0, config.getInt(path + ".horse.jumps", 0));
        progress.horseJumpStage = clamp(config.getInt(path + ".horse.jump-stage", 0), 0, HORSE_JUMP_MILESTONES.length);
    }

    private void grantDirectItems(Player player, List<DirectItemGrant> grants) {
        if (player == null || grants == null || grants.isEmpty()) {
            return;
        }
        for (DirectItemGrant grant : grants) {
            if (grant == null) continue;
            ItemStack stack = safeCreateItem(grant);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            stack.setAmount(Math.max(1, grant.amount()));
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(rem -> player.getWorld().dropItem(player.getLocation(), rem));
            }
        }
    }

    private ItemStack safeCreateItem(DirectItemGrant grant) {
        try {
            ItemStack item = grant.factory().get();
            if (item == null) {
                return new ItemStack(Material.AIR);
            }
            return item.clone();
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to create battle pass item: " + ex.getMessage());
            return new ItemStack(Material.AIR);
        }
    }

    private Supplier<ItemStack> sealingCharm(int amount) {
        int count = Math.max(1, amount);
        return () -> {
            ItemStack stack = SealingCharm.create(count);
            stack.setAmount(count);
            return stack;
        };
    }

    private Supplier<ItemStack> potion(String templateId) {
        return () -> createPotionStack(templateId);
    }

    private Supplier<ItemStack> miningMaterial(MiningMaterial material, int amount) {
        int count = Math.max(1, amount);
        return () -> {
            if (material == null) {
                return new ItemStack(Material.AIR);
            }
            ItemStack stack = material.createItem(count);
            stack.setAmount(count);
            return stack;
        };
    }

    private Supplier<ItemStack> essence(PlayerClass clazz, ItemRarity rarity, int starLevel) {
        PlayerClass target = clazz == null ? PlayerClass.MAGE : clazz;
        ItemRarity raritySafe = rarity == null ? ItemRarity.UNCOMMON : rarity;
        int stars = Math.max(0, starLevel);
        return () -> ClassEssence.generateEssence(target, raritySafe, stars);
    }

    private Supplier<ItemStack> mercenaryGift(String giftId, int amount) {
        int count = Math.max(1, amount);
        return () -> {
            MercenaryAffinityManager affinity = plugin.getMercenaryAffinityManager();
            if (affinity == null) {
                return new ItemStack(Material.AIR);
            }
            ItemStack gift = affinity.createGiftItem(giftId);
            if (gift == null) {
                return new ItemStack(Material.AIR);
            }
            gift.setAmount(count);
            return gift;
        };
    }

    private ItemStack createPotionStack(String templateId) {
        PotionManager potionManager = plugin.getPotionManager();
        if (potionManager == null || templateId == null || templateId.isBlank()) {
            return new ItemStack(Material.POTION);
        }
        PotionTemplate template = potionManager.getTemplate(templateId);
        if (template == null) {
            return new ItemStack(Material.POTION);
        }
        PotionInstance instance = potionManager.createInstance(template);
        return instance.toItemStack(plugin);
    }

    private void sendLevelUpMessage(Player player, int newTier, int currentProgress, int nextRequired) {
        int clampedProgress = Math.max(0, currentProgress);
        ChatFormatter.constructDivider(player, "§6§l-", 45);
        ChatFormatter.sendCenteredMessage(player, "§6§lBATTLE PASS LEVEL UP");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "You reached " + ChatColor.GOLD + "Tier " + newTier + ChatColor.GRAY + "!");

        if (newTier < tiers.size() && nextRequired > 0) {
            ChatFormatter.sendCenteredMessage(player, ChatColor.GRAY + "Next Tier Progress:");
            ChatFormatter.sendCenteredMessage(player,
                    TooltipUtil.expProgressBar(clampedProgress, nextRequired, 20)
                            + ChatColor.GRAY + " " + clampedProgress + "/" + nextRequired);
        } else {
            ChatFormatter.sendCenteredMessage(player, ChatColor.GREEN + "All battle pass tiers completed!");
        }

        ChatFormatter.sendCenteredMessage(player, ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/battlepass "
                + ChatColor.GRAY + "to claim rewards.");
        ChatFormatter.constructDivider(player, "§6§l-", 45);
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
                        .displayName("Coal Hauler's Stipend")
                        .coins(500)
                        .directItem("Coal", 16, miningMaterial(MiningMaterial.COAL, 16))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Premium Coal Cache")
                        .coins(750)
                        .gems(5)
                        .directItem("Coal", 32, miningMaterial(MiningMaterial.COAL, 32))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Prospector's Find")
                        .xp(600)
                        .directItem("Raw Iron", 12, miningMaterial(MiningMaterial.IRON, 12))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Refined Ore Stockpile")
                        .xp(900)
                        .coins(900)
                        .directItem("Raw Iron", 24, miningMaterial(MiningMaterial.IRON, 24))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Field Medic Supplies")
                        .directItem("Healing Potion", 1, potion("healing_i"))
                        .coins(300)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Arcane Tonic Reserve")
                        .directItem("Healing Potion", 1, potion("healing_ii"))
                        .gems(10)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Mercenary Welcome Parcel")
                        .xp(750)
                        .directItem("Blossom Bundle", 3, mercenaryGift("blossom_bundle", 3))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Veteran's Tribute")
                        .coins(1200)
                        .directItem("Heroic Token", 2, mercenaryGift("heroic_token", 2))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Sealed Essence Kit")
                        .directItem("Sealing Charm", 2, sealingCharm(2))
                        .xp(1200)
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Master Sealer Cache")
                        .directItem("Sealing Charm", 4, sealingCharm(4))
                        .coins(1500)
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Explorer's Kit")
                        .coins(1500)
                        .gems(10)
                        .directItem("Coal", 24, miningMaterial(MiningMaterial.COAL, 24))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Premium Expedition Cache")
                        .coins(2000)
                        .gems(20)
                        .directItem("Raw Iron", 32, miningMaterial(MiningMaterial.IRON, 32))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Miner's Dividend")
                        .coins(2500)
                        .directItem("Raw Iron", 32, miningMaterial(MiningMaterial.IRON, 32))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Smith's Premium Bundle")
                        .coins(3000)
                        .directItem("Sealing Charm", 5, sealingCharm(5))
                        .directItem("Coal", 32, miningMaterial(MiningMaterial.COAL, 32))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Season Completion Cache")
                        .coins(3500)
                        .gems(25)
                        .directItem("Healing Potion", 1, potion("healing_ii"))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Premium Season Completion Cache")
                        .coins(4500)
                        .gems(35)
                        .directItem("Healing Potion", 1, potion("healing_iii"))
                        .directItem("Sealing Charm", 6, sealingCharm(6))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Mercenary Favor Cache")
                        .coins(2200)
                        .directItem("Heroic Token", 3, mercenaryGift("heroic_token", 3))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Trusted Ally's Trove")
                        .xp(1800)
                        .directItem("Adventurer's Feast", 1, mercenaryGift("adventurers_feast", 1))
                        .directItem("Heroic Token", 2, mercenaryGift("heroic_token", 2))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Frontier Supply Crate")
                        .coins(1800)
                        .xp(1200)
                        .directItem("Coal", 24, miningMaterial(MiningMaterial.COAL, 24))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Premier Frontier Crate")
                        .coins(2300)
                        .xp(1500)
                        .gems(15)
                        .directItem("Raw Iron", 24, miningMaterial(MiningMaterial.IRON, 24))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Arcane Studies Cache")
                        .coins(1200)
                        .directItem(PlayerClass.AWAKMAGE.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKMAGE, ItemRarity.UNCOMMON, 0))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Arcana Vault")
                        .gems(15)
                        .directItem(PlayerClass.AWAKMAGE.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKMAGE, ItemRarity.RARE, 0))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Martial Discipline Pack")
                        .coins(1400)
                        .directItem(PlayerClass.AWAKWARRIOR.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKWARRIOR, ItemRarity.UNCOMMON, 0))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Legion Cache")
                        .gems(18)
                        .directItem(PlayerClass.AWAKWARRIOR.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKWARRIOR, ItemRarity.RARE, 0))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Sharpshooter's Satchel")
                        .coins(1400)
                        .directItem(PlayerClass.AWAKARCHER.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKARCHER, ItemRarity.UNCOMMON, 0))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Ranger Kit")
                        .gems(18)
                        .directItem(PlayerClass.AWAKARCHER.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKARCHER, ItemRarity.RARE, 0))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Allied Quartermaster's Pack")
                        .coins(2600)
                        .xp(1900)
                        .directItem("Blossom Bundle", 6, mercenaryGift("blossom_bundle", 6))
                        .directItem("Heroic Token", 2, mercenaryGift("heroic_token", 2))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Mercenary Feast Shipment")
                        .coins(3400)
                        .gems(18)
                        .directItem("Adventurer's Feast", 2, mercenaryGift("adventurers_feast", 2))
                        .directItem("Heroic Token", 3, mercenaryGift("heroic_token", 3))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Shadow Operative Pack")
                        .coins(1400)
                        .directItem(PlayerClass.AWAKROGUE.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKROGUE, ItemRarity.UNCOMMON, 0))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Shadow Cache")
                        .gems(18)
                        .directItem(PlayerClass.AWAKROGUE.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKROGUE, ItemRarity.RARE, 0))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Elite Expedition Rewards")
                        .xp(1600)
                        .coins(2600)
                        .directItem("Sealing Charm", 3, sealingCharm(3))
                        .directItem("Coal", 32, miningMaterial(MiningMaterial.COAL, 32))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Premier Expedition Rewards")
                        .xp(2000)
                        .coins(3200)
                        .gems(20)
                        .directItem("Healing Potion", 1, potion("healing_iii"))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Season Finale Trove")
                        .coins(5000)
                        .gems(40)
                        .directItem("Raw Iron", 48, miningMaterial(MiningMaterial.IRON, 48))
                        .directItem("Sealing Charm", 6, sealingCharm(6))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Premium Finale Hoard")
                        .coins(6000)
                        .gems(55)
                        .directItem("Sealing Charm", 8, sealingCharm(8))
                        .directItem("Healing Potion", 2, potion("healing_iii"))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Gemcutter's Dividend")
                        .coins(3200)
                        .gems(12)
                        .directItem("Raw Iron", 40, miningMaterial(MiningMaterial.IRON, 40))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Master Gem Hoard")
                        .coins(3800)
                        .gems(28)
                        .directItem("Coal", 48, miningMaterial(MiningMaterial.COAL, 48))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Alchemist's Cache")
                        .coins(1800)
                        .xp(1800)
                        .directItem("Healing Potion", 1, potion("healing_iii"))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Grand Alchemical Reserve")
                        .coins(2600)
                        .xp(2400)
                        .gems(20)
                        .directItem("Healing Potion", 2, potion("healing_iii"))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Sealwright Supplies")
                        .coins(2400)
                        .directItem("Sealing Charm", 4, sealingCharm(4))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Sealwright Arsenal")
                        .coins(3200)
                        .gems(24)
                        .directItem("Sealing Charm", 6, sealingCharm(6))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Cleric's Boon")
                        .coins(1600)
                        .directItem(PlayerClass.AWAKROGUE.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKROGUE, ItemRarity.UNCOMMON, 0))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Awakened Cleric's Cache")
                        .gems(22)
                        .directItem(PlayerClass.AWAKROGUE.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.AWAKROGUE, ItemRarity.RARE, 0))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Frontline Renown Pack")
                        .coins(2800)
                        .directItem(PlayerClass.BARBARIAN.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.BARBARIAN, ItemRarity.UNCOMMON, 0))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Dragon Warrior's Tribute")
                        .gems(26)
                        .directItem(PlayerClass.DRAGONWARRIOR.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.DRAGONWARRIOR, ItemRarity.RARE, 0))
                        .build()));

        defs.add(new TierDefinition(tier++,
                BattlePassRewardDefinition.builder()
                        .displayName("Eternal Champion's Cache")
                        .coins(6500)
                        .gems(45)
                        .directItem("Raw Iron", 64, miningMaterial(MiningMaterial.IRON, 64))
                        .directItem("Sealing Charm", 8, sealingCharm(8))
                        .build(),
                BattlePassRewardDefinition.builder()
                        .displayName("Ascendant Champion's Hoard")
                        .coins(7500)
                        .gems(65)
                        .directItem("Healing Potion", 2, potion("healing_iii"))
                        .directItem(PlayerClass.ARCHMAGE.getDisplayName() + " Essence", 1,
                                essence(PlayerClass.ARCHMAGE, ItemRarity.RARE, 0))
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
        private double horseDistanceMeters;
        private int horseDistanceStage;
        private int horseJumpCount;
        private int horseJumpStage;

        public boolean premiumActive() {
            return premiumActive;
        }
    }

    private record TierDefinition(int tier, BattlePassRewardDefinition free, BattlePassRewardDefinition premium) { }
}
