package me.nakilex.levelplugin.battlepass;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Coordinates battle pass progression, reward claiming and persistence.
 */
public class BattlePassManager {
    private final Main plugin;
    private final PlayerConfig playerConfig;
    private final EconomyManager economyManager;
    private final GemsManager gemsManager;
    private final PotionManager potionManager;
    private final ItemManager itemManager;

    private final List<BattlePassTier> tiers;
    private final Map<UUID, BattlePassProgress> progress = new HashMap<>();
    private final Map<UUID, Integer> pendingSaves = new HashMap<>();

    private static final int XP_PER_TIER = 1200;
    private static final int PREMIUM_COST_GEMS = 3000;
    private static final double LEVEL_XP_RATIO = 0.6;
    private static final int SAVE_THRESHOLD = 400;
    private static final int SEASON_ID = 1;
    private static final String SEASON_NAME = ChatColor.GOLD + "Season I: Adventurer's Path";

    private final int maxXp;

    public BattlePassManager(Main plugin,
                             PlayerConfig playerConfig,
                             EconomyManager economyManager,
                             GemsManager gemsManager,
                             PotionManager potionManager,
                             ItemManager itemManager) {
        this.plugin = plugin;
        this.playerConfig = playerConfig;
        this.economyManager = economyManager;
        this.gemsManager = gemsManager;
        this.potionManager = potionManager;
        this.itemManager = itemManager;
        this.tiers = Collections.unmodifiableList(buildDefaultTiers());
        this.maxXp = tiers.size() * XP_PER_TIER;
    }

    private List<BattlePassTier> buildDefaultTiers() {
        List<BattlePassTier> list = new ArrayList<>();
        list.add(createTier(list, coins(500), coins(1000)));
        list.add(createTier(list, gems(80), gems(160)));
        list.add(createTier(list, potions("healing_i", 2), potions("healing_ii", 2)));
        list.add(createTier(list, potions("mana_i", 2), potions("mana_ii", 2)));
        list.add(createTier(list,
                essenceReward(ChatColor.GRAY + "Random Common Essence", me.nakilex.levelplugin.items.data.ItemRarity.COMMON),
                essenceReward(ChatColor.GRAY + "Random Uncommon Essence", me.nakilex.levelplugin.items.data.ItemRarity.UNCOMMON)));
        list.add(createTier(list,
                itemReward(118, ChatColor.YELLOW + "Common Iron Helmet"),
                itemReward(133, ChatColor.LIGHT_PURPLE + "Epic Iron Helmet")));
        list.add(createTier(list, coins(1200), coins(2400)));
        list.add(createTier(list, gems(200), gems(400)));
        list.add(createTier(list,
                potions("healing_iii", 1),
                bundle(ChatColor.GOLD + "Master Alchemist Cache",
                        Material.BREWING_STAND,
                        List.of(ChatColor.GRAY + "- 1x Large Healing Potion",
                                ChatColor.GRAY + "- 1x Large Mana Potion"),
                        player -> {
                            givePotion(player, "healing_iii");
                            givePotion(player, "mana_iii");
                        })));
        list.add(createTier(list,
                essenceReward(ChatColor.GRAY + "Random Uncommon Essence", me.nakilex.levelplugin.items.data.ItemRarity.UNCOMMON),
                essenceReward(ChatColor.GRAY + "Random Rare Essence", me.nakilex.levelplugin.items.data.ItemRarity.RARE)));
        list.add(createTier(list,
                itemReward(119, ChatColor.YELLOW + "Common Iron Chestplate"),
                itemReward(134, ChatColor.LIGHT_PURPLE + "Epic Iron Chestplate")));
        list.add(createTier(list,
                itemReward(120, ChatColor.YELLOW + "Common Iron Leggings"),
                itemReward(135, ChatColor.LIGHT_PURPLE + "Epic Iron Leggings")));
        list.add(createTier(list,
                itemReward(121, ChatColor.YELLOW + "Common Iron Boots"),
                itemReward(136, ChatColor.LIGHT_PURPLE + "Epic Iron Boots")));
        list.add(createTier(list, coins(2000), coins(4000)));
        list.add(createTier(list, gems(500), gems(750)));
        list.add(createTier(list,
                essenceReward(ChatColor.GRAY + "Random Rare Essence", me.nakilex.levelplugin.items.data.ItemRarity.RARE),
                essenceReward(ChatColor.GRAY + "Random Epic Essence", me.nakilex.levelplugin.items.data.ItemRarity.EPIC)));
        list.add(createTier(list,
                bundle(ChatColor.GREEN + "Potion Resupply",
                        Material.POTION,
                        List.of(ChatColor.GRAY + "- 2x Medium Healing Potion",
                                ChatColor.GRAY + "- 2x Medium Mana Potion"),
                        player -> {
                            givePotions(player, "healing_ii", 2);
                            givePotions(player, "mana_ii", 2);
                        }),
                bundle(ChatColor.AQUA + "Arcane Elixir Cache",
                        Material.SPLASH_POTION,
                        List.of(ChatColor.GRAY + "- 2x Large Healing Potion",
                                ChatColor.GRAY + "- 2x Large Mana Potion",
                                ChatColor.GRAY + "- 400 Gems"),
                        player -> {
                            givePotions(player, "healing_iii", 2);
                            givePotions(player, "mana_iii", 2);
                            gemsManager.addUnits(player, 400);
                            CurrencyMessageUtil.sendReceive(player, CurrencyMessageUtil.Currency.GEMS, 400);
                        })));
        list.add(createTier(list,
                essenceReward(ChatColor.GRAY + "Random Epic Essence", me.nakilex.levelplugin.items.data.ItemRarity.EPIC),
                essenceReward(ChatColor.GRAY + "Random Legendary Essence", me.nakilex.levelplugin.items.data.ItemRarity.LEGENDARY)));
        list.add(createTier(list, coins(5000), coins(7500)));
        list.add(createTier(list, randomLegendaryPiece(), fullLegendarySet()));
        return list;
    }

    private BattlePassTier createTier(List<BattlePassTier> existing, BattlePassReward free, BattlePassReward premium) {
        return new BattlePassTier(existing.size() + 1, free, premium);
    }

    private BattlePassReward coins(int amount) {
        return new BattlePassReward(
                ChatColor.GOLD + String.format("%,d Coins", amount),
                List.of(ChatColor.GRAY + "Spendable with merchants and services."),
                () -> new ItemStack(Material.SUNFLOWER),
                (player, manager) -> {
                    economyManager.addCoins(player, amount);
                    CurrencyMessageUtil.sendReceive(player, CurrencyMessageUtil.Currency.COINS, amount);
                }
        );
    }

    private BattlePassReward gems(int amount) {
        return new BattlePassReward(
                ChatColor.LIGHT_PURPLE + String.format("%,d Gems", amount),
                List.of(ChatColor.GRAY + "Premium crafting currency."),
                () -> new ItemStack(Material.AMETHYST_SHARD),
                (player, manager) -> {
                    gemsManager.addUnits(player, amount);
                    CurrencyMessageUtil.sendReceive(player, CurrencyMessageUtil.Currency.GEMS, amount);
                }
        );
    }

    private BattlePassReward potions(String templateId, int amount) {
        PotionTemplate template = potionManager.getTemplate(templateId);
        String name = template != null ? ChatColor.translateAlternateColorCodes('&', template.getName()) : templateId;
        List<String> lore = List.of(ChatColor.GRAY + "Potion reward: " + ChatColor.WHITE + name,
                ChatColor.GRAY + "Quantity: " + ChatColor.WHITE + amount);
        return new BattlePassReward(
                ChatColor.AQUA + name + ChatColor.GRAY + " x" + amount,
                lore,
                () -> template != null && template.getNexoId() != null
                        ? me.nakilex.levelplugin.utils.GuiUtil.getNexoItem(template.getNexoId(), ChatColor.AQUA + name)
                        : new ItemStack(Material.POTION),
                (player, manager) -> givePotions(player, templateId, amount)
        );
    }

    private BattlePassReward essenceReward(String title, me.nakilex.levelplugin.items.data.ItemRarity rarity) {
        List<String> lore = List.of(ChatColor.GRAY + "Unidentified essence of " + rarity.name().toLowerCase());
        return new BattlePassReward(
                title,
                lore,
                () -> new ItemStack(Material.ENCHANTED_BOOK),
                (player, manager) -> {
                    PlayerClass[] classes = PlayerClass.values();
                    PlayerClass clazz = classes[ThreadLocalRandom.current().nextInt(classes.length)];
                    ItemStack essence = ClassEssence.generateEssence(clazz, rarity, 0);
                    giveItem(player, essence);
                    ChatMessageUtil.send(player, MessageType.REWARD,
                            "Received a " + rarity.name().toLowerCase() + " class essence!");
                }
        );
    }

    private BattlePassReward itemReward(int templateId, String title) {
        List<String> lore = List.of(ChatColor.GRAY + "Battle pass exclusive item.");
        return new BattlePassReward(
                title,
                lore,
                () -> new ItemStack(Material.CHEST),
                (player, manager) -> {
                    CustomItem template = itemManager.getTemplateById(templateId);
                    if (template == null) {
                        plugin.getLogger().warning("Missing item template for battle pass reward: " + templateId);
                        return;
                    }
                    CustomItem instance = itemManager.rollNewInstance(templateId);
                    if (instance == null) {
                        plugin.getLogger().warning("Failed to roll instance for item " + templateId);
                        return;
                    }
                    ItemStack stack = ItemUtil.createItemStackFromCustomItem(instance, 1, player);
                    giveItem(player, stack);
                    ChatMessageUtil.send(player, MessageType.REWARD,
                            "Received " + ChatColor.stripColor(template.getBaseName()));
                }
        );
    }

    private BattlePassReward bundle(String title, Material icon, List<String> lines, java.util.function.Consumer<Player> action) {
        return new BattlePassReward(
                title,
                lines,
                () -> new ItemStack(icon),
                (player, manager) -> {
                    action.accept(player);
                    ChatMessageUtil.send(player, MessageType.REWARD, "Bundle delivered!");
                }
        );
    }

    private BattlePassReward randomLegendaryPiece() {
        List<Integer> ids = List.of(138, 139, 140, 141);
        return new BattlePassReward(
                ChatColor.GOLD + "Legendary Iron Cache",
                List.of(ChatColor.GRAY + "Grants one random legendary iron armor piece."),
                () -> new ItemStack(Material.SHULKER_BOX),
                (player, manager) -> {
                    int choice = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
                    CustomItem template = itemManager.getTemplateById(choice);
                    if (template == null) {
                        plugin.getLogger().warning("Missing legendary template " + choice);
                        return;
                    }
                    CustomItem instance = itemManager.rollNewInstance(choice);
                    if (instance == null) return;
                    giveItem(player, ItemUtil.createItemStackFromCustomItem(instance, 1, player));
                    ChatMessageUtil.send(player, MessageType.REWARD,
                            "Received a legendary " + ChatColor.stripColor(template.getBaseName()) + "!");
                }
        );
    }

    private BattlePassReward fullLegendarySet() {
        List<Integer> ids = List.of(138, 139, 140, 141);
        return new BattlePassReward(
                ChatColor.LIGHT_PURPLE + "Legendary Iron Wardrobe",
                List.of(ChatColor.GRAY + "Unlock the complete legendary iron armor set."),
                () -> new ItemStack(Material.NETHERITE_BLOCK),
                (player, manager) -> {
                    for (int id : ids) {
                        CustomItem instance = itemManager.rollNewInstance(id);
                        if (instance == null) continue;
                        giveItem(player, ItemUtil.createItemStackFromCustomItem(instance, 1, player));
                    }
                    ChatMessageUtil.send(player, MessageType.REWARD,
                            "Legendary armor set delivered!");
                }
        );
    }

    private void givePotions(Player player, String templateId, int amount) {
        for (int i = 0; i < amount; i++) {
            givePotion(player, templateId);
        }
    }

    private void givePotion(Player player, String templateId) {
        PotionTemplate template = potionManager.getTemplate(templateId);
        if (template == null) {
            plugin.getLogger().warning("Unknown potion template " + templateId + " for battle pass reward");
            return;
        }
        PotionInstance instance = potionManager.createInstance(template);
        giveItem(player, instance.toItemStack(plugin));
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
    }

    public List<BattlePassTier> getTiers() {
        return tiers;
    }

    public int getSeasonId() {
        return SEASON_ID;
    }

    public String getSeasonName() {
        return SEASON_NAME;
    }

    public int getXpPerTier() {
        return XP_PER_TIER;
    }

    public int getPremiumCostGems() {
        return PREMIUM_COST_GEMS;
    }

    public BattlePassTier getTier(int index) {
        if (index <= 0 || index > tiers.size()) return null;
        return tiers.get(index - 1);
    }

    public BattlePassProgress getProgress(UUID uuid) {
        return progress.computeIfAbsent(uuid, id -> new BattlePassProgress(SEASON_ID));
    }

    public void loadProgress(UUID uuid, int storedSeason, int xp, boolean premium,
                             Set<Integer> freeClaims, Set<Integer> premiumClaims) {
        BattlePassProgress prog = getProgress(uuid);
        if (storedSeason != SEASON_ID) {
            prog.setSeasonId(SEASON_ID);
            prog.setXp(0);
            prog.setPremium(false);
            prog.clearClaims();
            return;
        }
        prog.setSeasonId(storedSeason);
        prog.setXp(Math.min(xp, maxXp));
        prog.setPremium(premium);
        prog.clearClaims();
        if (freeClaims != null) {
            for (Integer tier : freeClaims) {
                if (tier != null && tier >= 1 && tier <= tiers.size()) {
                    prog.markClaimed(tier, false);
                }
            }
        }
        if (premiumClaims != null) {
            for (Integer tier : premiumClaims) {
                if (tier != null && tier >= 1 && tier <= tiers.size()) {
                    prog.markClaimed(tier, true);
                }
            }
        }
    }

    public void addLevelXp(UUID uuid, int amount) {
        if (amount <= 0) return;
        int bonus = (int) Math.ceil(amount * LEVEL_XP_RATIO);
        addBattlePassXp(uuid, bonus);
    }

    public void addBonusXp(Player player, int amount, String reason) {
        if (amount <= 0) return;
        int gained = addBattlePassXp(player.getUniqueId(), amount);
        if (gained <= 0) {
            return;
        }
        if (reason != null && !reason.isEmpty()) {
            ChatMessageUtil.send(player, MessageType.REWARD,
                    "Gained " + gained + " Battle Pass XP for " + reason + ".");
        }
    }

    public int addXp(UUID uuid, int amount) {
        if (amount <= 0) return 0;
        return addBattlePassXp(uuid, amount);
    }

    public int setTierLevel(UUID uuid, int level) {
        int clampedLevel = Math.max(0, Math.min(level, tiers.size()));
        BattlePassProgress progress = getProgress(uuid);
        int targetXp = clampedLevel * XP_PER_TIER;
        return applyXpValue(uuid, progress, targetXp, targetXp > progress.getXp(), false, true);
    }

    private int addBattlePassXp(UUID uuid, int amount) {
        if (amount <= 0) {
            return 0;
        }
        return adjustBattlePassXp(uuid, amount, true, true, false);
    }

    private int adjustBattlePassXp(UUID uuid, int delta, boolean announceUnlocks, boolean trackPending, boolean immediateSave) {
        if (delta == 0) {
            return 0;
        }
        BattlePassProgress progress = getProgress(uuid);
        int newXp = progress.getXp() + delta;
        return applyXpValue(uuid, progress, newXp, announceUnlocks, trackPending, immediateSave);
    }

    private int applyXpValue(UUID uuid,
                             BattlePassProgress progress,
                             int newXp,
                             boolean announceUnlocks,
                             boolean trackPending,
                             boolean immediateSave) {
        int beforeUnlocked = progress.unlockedTiers(XP_PER_TIER, tiers.size());
        int beforeXp = progress.getXp();
        int clamped = Math.max(0, Math.min(maxXp, newXp));
        if (clamped == beforeXp) {
            if (immediateSave) {
                saveProgress(uuid);
            }
            return 0;
        }

        progress.setXp(clamped);
        int afterUnlocked = progress.unlockedTiers(XP_PER_TIER, tiers.size());
        int delta = clamped - beforeXp;

        if (trackPending && delta > 0) {
            pendingSaves.merge(uuid, delta, Integer::sum);
        } else {
            pendingSaves.remove(uuid);
        }

        if (announceUnlocks && afterUnlocked > beforeUnlocked) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                for (int tier = beforeUnlocked + 1; tier <= afterUnlocked; tier++) {
                    ChatMessageUtil.send(player, MessageType.REWARD,
                            "Battle Pass Tier " + tier + " unlocked! Use /bp to claim.");
                }
            }
            saveProgress(uuid);
        } else if (immediateSave || (trackPending && pendingSaves.getOrDefault(uuid, 0) >= SAVE_THRESHOLD)) {
            saveProgress(uuid);
        }

        return delta;
    }

    public boolean claim(Player player, int tierIndex, boolean premiumTrack) {
        BattlePassTier tier = getTier(tierIndex);
        if (tier == null) return false;
        BattlePassProgress prog = getProgress(player.getUniqueId());
        if (!isTierUnlocked(prog, tierIndex)) {
            ChatMessageUtil.send(player, MessageType.ERROR, "You haven't unlocked Tier " + tierIndex + " yet.");
            return false;
        }
        if (premiumTrack && !prog.hasPremium()) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Unlock the premium track to claim this reward.");
            return false;
        }
        if (prog.isClaimed(tierIndex, premiumTrack)) {
            ChatMessageUtil.send(player, MessageType.WARNING, "Reward already claimed.");
            return false;
        }
        BattlePassReward reward = premiumTrack ? tier.premiumReward() : tier.freeReward();
        if (reward == null) {
            ChatMessageUtil.send(player, MessageType.WARNING, "No reward defined for this tier.");
            return false;
        }
        reward.grant(player, this);
        prog.markClaimed(tierIndex, premiumTrack);
        saveProgress(player.getUniqueId());
        ChatMessageUtil.send(player, MessageType.SUCCESS,
                "Claimed Tier " + tierIndex + (premiumTrack ? " Premium" : " Free") + " reward!");
        return true;
    }

    public boolean unlockPremium(Player player) {
        BattlePassProgress prog = getProgress(player.getUniqueId());
        if (prog.hasPremium()) {
            ChatMessageUtil.send(player, MessageType.WARNING, "Premium track already unlocked.");
            return false;
        }
        try {
            gemsManager.deductUnits(player, PREMIUM_COST_GEMS);
        } catch (IllegalArgumentException ex) {
            ChatMessageUtil.send(player, MessageType.ERROR, "You need "
                    + CurrencyMessageUtil.formatAmount(CurrencyMessageUtil.Currency.GEMS, PREMIUM_COST_GEMS)
                    + ChatColor.RED + " to unlock premium.");
            return false;
        }
        setPremiumStatus(player.getUniqueId(), true);
        CurrencyMessageUtil.sendLoss(player, CurrencyMessageUtil.Currency.GEMS, PREMIUM_COST_GEMS);
        ChatMessageUtil.send(player, MessageType.SUCCESS, "Premium track unlocked! Enjoy enhanced rewards.");
        return true;
    }

    /**
     * Forcefully update premium track access for a player without applying any cost or additional logic.
     *
     * @param uuid    the player to modify
     * @param premium whether premium should be enabled
     * @return {@code true} if the premium state changed, {@code false} otherwise
     */
    public boolean setPremiumStatus(UUID uuid, boolean premium) {
        BattlePassProgress prog = getProgress(uuid);
        if (prog.hasPremium() == premium) {
            return false;
        }
        prog.setPremium(premium);
        saveProgress(uuid);
        return true;
    }

    private boolean isTierUnlocked(BattlePassProgress progress, int tierIndex) {
        return progress.getXp() >= tierIndex * XP_PER_TIER;
    }

    private void saveProgress(UUID uuid) {
        pendingSaves.remove(uuid);
        playerConfig.savePlayerData(uuid);
    }

    public void saveAll() {
        for (UUID uuid : progress.keySet()) {
            playerConfig.savePlayerData(uuid);
        }
    }
}
