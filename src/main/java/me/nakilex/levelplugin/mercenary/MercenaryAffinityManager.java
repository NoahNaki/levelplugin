package me.nakilex.levelplugin.mercenary;

import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.codex.CodexManager;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcTagUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Central manager for mercenary friendship progression. Responsible for
 * loading tuning data from {@code mercenaries.yml}, tracking player
 * friendship points, and exposing gear score and benefit lookups for GUIs.
 */
public class MercenaryAffinityManager implements org.bukkit.event.Listener {

    private final Plugin plugin;
    private final Map<String, MercenaryGift> gifts = new LinkedHashMap<>();
    private final Map<Integer, Integer> gearScores = new HashMap<>();
    private final Map<Integer, MercenaryRole> roles = new HashMap<>();
    private final Map<Integer, List<String>> levelBenefits = new HashMap<>();
    private final NavigableMap<Integer, Integer> levelThresholds = new TreeMap<>();
    private FileConfiguration config;

    private final Map<UUID, Map<Integer, MercenaryFriendship>> friendships = new HashMap<>();
    private final Map<UUID, Long> giftHintCooldowns = new HashMap<>();

    public MercenaryAffinityManager(Plugin plugin) {
        this.plugin = plugin;
        reload();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        File cfgFile = new File(plugin.getDataFolder(), "mercenaries.yml");
        if (!cfgFile.exists()) {
            plugin.saveResource("mercenaries.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(cfgFile);

        loadGifts();
        loadGearScores();
        loadRoles();
        loadBenefits();
        loadThresholds();
    }

    private void loadGifts() {
        gifts.clear();
        ConfigurationSection section = config.getConfigurationSection("gifts");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            Material mat = Material.matchMaterial(section.getString(id + ".material", "PAPER"));
            int points = section.getInt(id + ".affinity", 5);
            String name = section.getString(id + ".name", ChatColor.LIGHT_PURPLE + "Gift");
            List<String> lore = section.getStringList(id + ".lore");
            MercenaryGift gift = new MercenaryGift(id, mat == null ? Material.PAPER : mat, name, lore, points);
            gifts.put(id, gift);
        }
    }

    private void loadGearScores() {
        gearScores.clear();
        ConfigurationSection section = config.getConfigurationSection("gearscore");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            try {
                gearScores.put(Integer.parseInt(id), section.getInt(id));
            } catch (NumberFormatException ex) {
                Bukkit.getLogger().warning("Invalid mercenary id in mercenaries.yml: " + id);
            }
        }
    }

    private void loadRoles() {
        roles.clear();
        ConfigurationSection section = config.getConfigurationSection("roles");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            try {
                int npcId = Integer.parseInt(id);
                roles.put(npcId, MercenaryRole.fromString(section.getString(id)));
            } catch (NumberFormatException ex) {
                Bukkit.getLogger().warning("Invalid mercenary id in mercenaries.yml (roles): " + id);
            }
        }
    }

    private void loadBenefits() {
        levelBenefits.clear();
        ConfigurationSection section = config.getConfigurationSection("benefits");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                List<String> list = new ArrayList<>();
                for (String line : section.getStringList(key)) {
                    list.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                levelBenefits.put(level, list);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void loadThresholds() {
        levelThresholds.clear();
        ConfigurationSection section = config.getConfigurationSection("levels");
        if (section == null) {
            levelThresholds.put(1, 0);
            levelThresholds.put(2, 50);
            levelThresholds.put(3, 100);
            levelThresholds.put(4, 175);
            levelThresholds.put(5, 250);
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                levelThresholds.put(level, section.getInt(key));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public Collection<MercenaryGift> getGifts() {
        return gifts.values();
    }

    public ItemStack createGiftItem(String id) {
        MercenaryGift gift = gifts.get(id);
        return gift == null ? null : gift.getIcon();
    }

    public MercenaryGift matchGift(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) {
            return null;
        }
        for (MercenaryGift gift : gifts.values()) {
            ItemStack icon = gift.getIcon();
            if (icon.getType() != stack.getType()) {
                continue;
            }
            if (icon.getItemMeta() != null && stack.getItemMeta() != null
                    && icon.getItemMeta().getDisplayName().equals(stack.getItemMeta().getDisplayName())) {
                return gift;
            }
        }
        return null;
    }

    public int getGearScore(int npcId) {
        return gearScores.getOrDefault(npcId, 0);
    }

    public Collection<Integer> getMercenaryIds() {
        return Collections.unmodifiableSet(gearScores.keySet());
    }

    public Collection<Integer> getUnlockedMercenaryIds(UUID playerId) {
        if (playerId == null) {
            return getMercenaryIds();
        }
        List<Integer> unlocked = new ArrayList<>();
        for (int id : getMercenaryIds()) {
            if (isUnlocked(playerId, id)) {
                unlocked.add(id);
            }
        }
        return unlocked;
    }

    public boolean isUnlocked(UUID playerId, int npcId) {
        CodexManager codexManager = resolveCodexManager();
        if (codexManager == null || playerId == null) {
            return true;
        }
        String name = getMercenaryName(npcId);
        if (name == null || name.isBlank()) {
            return true;
        }
        return codexManager.getDiscoveredNpcs(playerId).stream()
                .anyMatch(found -> found.equalsIgnoreCase(name));
    }

    public MercenaryRole getRole(int npcId) {
        return roles.getOrDefault(npcId, MercenaryRole.DPS);
    }

    public String getRoleLabel(int npcId) {
        return TextUtil.beautifyWords(getRole(npcId).name());
    }

    public List<String> getBenefits(int level) {
        return levelBenefits.getOrDefault(level, TooltipUtil.bulletList("No perks configured"));
    }

    public MercenaryFriendship getFriendship(UUID playerId, int npcId) {
        return friendships.computeIfAbsent(playerId, uuid -> new HashMap<>())
                .computeIfAbsent(npcId, id -> new MercenaryFriendship(0, 1));
    }

    public MercenaryFriendship addAffinity(Player player, int npcId, int amount) {
        MercenaryFriendship friendship = getFriendship(player.getUniqueId(), npcId);
        friendship.addPoints(amount);
        int newLevel = computeLevel(friendship.getPoints());
        friendship.setLevel(newLevel);
        save(player.getUniqueId());
        return friendship;
    }

    /**
     * Apply the player's held gift to the specified mercenary, consuming one
     * stack item and updating their affinity progress. Returns {@code true}
     * when a valid gift was applied.
     */
    public boolean handGift(Player player, int npcId, String npcName) {
        return handGift(player, npcId, npcName, matchGift(player.getInventory().getItemInMainHand()));
    }

    private boolean handGift(Player player, int npcId, String npcName, MercenaryGift gift) {
        if (gift == null) {
            player.sendMessage(ChatColor.RED + "Hold a mercenary gift in your main hand.");
            return false;
        }
        MercenaryFriendship existing = getFriendship(player.getUniqueId(), npcId);
        int maxThreshold = thresholdForLevel(5);
        if (existing.getLevel() >= 5 && existing.getPoints() >= maxThreshold) {
            player.sendMessage(ChatColor.GOLD + npcName + ChatColor.RED + " is already at max friendship.");
            return false;
        }
        MercenaryFriendship friendship = addAffinity(player, npcId, gift.getAffinityValue());
        String giftName = gift.getIcon().getItemMeta() != null
                ? gift.getIcon().getItemMeta().getDisplayName()
                : ChatColor.WHITE + gift.getId();

        ItemStack hand = player.getInventory().getItemInMainHand();
        int remaining = hand.getAmount() - 1;
        if (remaining <= 0) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(remaining);
        }

        player.sendMessage(ChatColor.GREEN + "You gave " + giftName + ChatColor.GREEN
                + " to " + ChatColor.GOLD + npcName + ChatColor.GREEN + " (Level " + friendship.getLevel() + ")");
        return true;
    }

    public int computeLevel(int points) {
        int level = 1;
        for (Map.Entry<Integer, Integer> entry : levelThresholds.entrySet()) {
            if (points >= entry.getValue()) {
                level = Math.max(level, entry.getKey());
            }
        }
        return Math.min(level, 5);
    }

    public int thresholdForLevel(int level) {
        return levelThresholds.getOrDefault(level, 0);
    }

    public void loadPlayer(UUID playerId) {
        File file = getPlayerFile(playerId);
        if (!file.exists()) {
            return;
        }
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("friendship");
        if (section == null) {
            return;
        }
        Map<Integer, MercenaryFriendship> map = friendships.computeIfAbsent(playerId, id -> new HashMap<>());
        for (String key : section.getKeys(false)) {
            try {
                int npc = Integer.parseInt(key);
                int points = section.getInt(key + ".points");
                int level = section.getInt(key + ".level", computeLevel(points));
                map.put(npc, new MercenaryFriendship(points, level));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void save(UUID playerId) {
        Map<Integer, MercenaryFriendship> map = friendships.get(playerId);
        if (map == null) {
            return;
        }
        File file = getPlayerFile(playerId);
        FileConfiguration data = new YamlConfiguration();
        for (Map.Entry<Integer, MercenaryFriendship> entry : map.entrySet()) {
            data.set("friendship." + entry.getKey() + ".points", entry.getValue().getPoints());
            data.set("friendship." + entry.getKey() + ".level", entry.getValue().getLevel());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save mercenary friendship for " + playerId + ": " + e.getMessage());
        }
    }

    private File getPlayerFile(UUID playerId) {
        File folder = new File(plugin.getDataFolder(), "mercenaries");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, "player_" + playerId + ".yml");
    }

    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (NpcTagUtil.isNpc(event.getPlayer())) {
            return;
        }
        loadPlayer(event.getPlayer().getUniqueId());
    }

    @org.bukkit.event.EventHandler
    public void onMercenaryGift(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (!NpcApi.getRegistry().isNPC(event.getRightClicked())) {
            return;
        }
        me.nakilex.levelplugin.npc.system.NPC npc = NpcApi.getRegistry().getNPC(event.getRightClicked());
        if (npc == null) {
            return;
        }
        int npcId = npc.getId();
        if (!gearScores.containsKey(npcId)) {
            return;
        }
        recordDiscovery(event.getPlayer(), npc);
        Player player = event.getPlayer();
        MercenaryGift heldGift = matchGift(player.getInventory().getItemInMainHand());
        if (!player.isSneaking()) {
            if (heldGift != null && shouldSendGiftHint(player.getUniqueId())) {
                player.sendMessage(ChatColor.GRAY + "Sneak + right-click with your gift to give it to "
                        + ChatColor.GOLD + npc.getName() + ChatColor.GRAY + ".");
            }
            return;
        }

        if (heldGift == null) {
            player.sendMessage(ChatColor.RED + "Hold a mercenary gift in your main hand.");
            return;
        }

        handGift(player, npcId, npc.getName(), heldGift);
    }

    private boolean shouldSendGiftHint(UUID playerId) {
        long now = System.currentTimeMillis();
        Long lastHint = giftHintCooldowns.get(playerId);
        if (lastHint != null && now - lastHint < 3000) {
            return false;
        }
        giftHintCooldowns.put(playerId, now);
        return true;
    }

    private CodexManager resolveCodexManager() {
        if (plugin instanceof Main main) {
            return main.getCodexManager();
        }
        return null;
    }

    private String getMercenaryName(int npcId) {
        NPC npc = NpcApi.getRegistry().getById(npcId);
        if (npc == null || npc.getName() == null) {
            return null;
        }
        return ChatColor.stripColor(npc.getName());
    }

    private void recordDiscovery(Player player, NPC npc) {
        CodexManager codexManager = resolveCodexManager();
        if (codexManager == null || player == null || npc == null) {
            return;
        }
        String name = ChatColor.stripColor(npc.getName());
        if (name == null || name.isBlank()) {
            return;
        }
        codexManager.recordNpc(player, name);
    }
}
