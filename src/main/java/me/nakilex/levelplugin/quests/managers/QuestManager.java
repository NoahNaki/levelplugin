package me.nakilex.levelplugin.quests.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.quests.data.*;
import me.nakilex.levelplugin.quests.gui.QuestState;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QuestManager {
    private final Main plugin;
    private final PartyManager partyManager;
    private final LevelManager levelManager;
    private final Map<String, Quest> quests = new HashMap<>();
    private final Map<Integer, String> npcQuestMap = new HashMap<>();
    private final Map<UUID, PlayerQuestProgress> activeQuests = new HashMap<>();
    private final Map<UUID, Set<String>> completedQuests = new HashMap<>();
    private final Map<UUID, String> trackedQuests = new HashMap<>();
    private boolean debug = false;
    private FileConfiguration progressConfig;
    private File progressFile;

    public QuestManager(Main plugin, PartyManager partyManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.levelManager = plugin.getLevelManager();
        registerDefaultQuests();
        loadProgressFile();
        loadProgress();
    }


    private void loadProgressFile() {
        progressFile = new File(plugin.getDataFolder(), "player_quests.yml");
        if (!progressFile.exists()) {
            try {
                progressFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create player_quests.yml: " + e.getMessage());
            }
        }
        progressConfig = YamlConfiguration.loadConfiguration(progressFile);
    }

    private void registerDefaultQuests() {
        quests.clear();
        // Register quests here manually.
        Quest tutorial = new me.nakilex.levelplugin.quests.def.TutorialQuest();
        registerQuest(tutorial);
        plugin.getLogger().info("Registered " + quests.size() + " quests.");
    }

    public void registerQuest(Quest quest) {
        quests.put(quest.getId(), quest);
        if (quest.getNpcGiverId() != null) {
            npcQuestMap.put(quest.getNpcGiverId(), quest.getId());
        }
    }

    public void registerNpcQuest(int npcId, String questId) {
        npcQuestMap.put(npcId, questId);
    }

    public Quest getQuestByNpcId(int npcId) {
        String id = npcQuestMap.get(npcId);
        return id == null ? null : quests.get(id);
    }

    public Map<Integer, String> getNpcQuestMap() {
        return npcQuestMap;
    }

    private void loadProgress() {
        activeQuests.clear();
        completedQuests.clear();
        ConfigurationSection players = progressConfig.getConfigurationSection("players");
        if (players == null) return;
        for (String uuidStr : players.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            ConfigurationSection sec = players.getConfigurationSection(uuidStr);
            if (sec == null) continue;
            List<String> completed = sec.getStringList("completed");
            if (!completed.isEmpty()) {
                completedQuests.put(uuid, new HashSet<>(completed));
            }
            String activeId = sec.getString("active.id");
            if (activeId != null) {
                Quest quest = quests.get(activeId);
                if (quest != null) {
                    PlayerQuestProgress progress = new PlayerQuestProgress(quest);
                    ConfigurationSection progSec = sec.getConfigurationSection("active.progress");
                    if (progSec != null) {
                        for (String key : progSec.getKeys(false)) {
                            int index = Integer.parseInt(key);
                            int value = progSec.getInt(key);
                            progress.setProgress(index, value);
                        }
                    }
                    activeQuests.put(uuid, progress);
                }
            }
            String tracked = sec.getString("tracked");
            if (tracked != null) {
                trackedQuests.put(uuid, tracked);
            }
        }
    }

    public void saveProgress() {
        progressConfig.set("players", null);
        ConfigurationSection root = progressConfig.createSection("players");
        Set<UUID> all = new HashSet<>();
        all.addAll(completedQuests.keySet());
        all.addAll(activeQuests.keySet());
        for (UUID uuid : all) {
            ConfigurationSection sec = root.createSection(uuid.toString());
            Set<String> completed = completedQuests.get(uuid);
            if (completed != null) {
                sec.set("completed", new ArrayList<>(completed));
            }
            PlayerQuestProgress progress = activeQuests.get(uuid);
            if (progress != null) {
                sec.set("active.id", progress.getQuest().getId());
                for (int i = 0; i < progress.getQuest().getObjectives().size(); i++) {
                    sec.set("active.progress." + i, progress.getProgress(i));
                }
            }
            String tracked = trackedQuests.get(uuid);
            if (tracked != null) {
                sec.set("tracked", tracked);
            }
        }
        try {
            progressConfig.save(progressFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player_quests.yml: " + e.getMessage());
        }
    }

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Collection<Quest> getQuests() {
        return quests.values();
    }

    public QuestState getQuestState(Player player, Quest quest) {
        if (completedQuests.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(quest.getId())) {
            return QuestState.COMPLETED;
        }

        PlayerQuestProgress progress = activeQuests.get(player.getUniqueId());
        if (progress != null && progress.getQuest().getId().equals(quest.getId())) {
            if (progress.isComplete()) {
                return QuestState.COMPLETED;
            }
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                if (progress.getProgress(i) > 0) {
                    return QuestState.IN_PROGRESS;
                }
            }
            return QuestState.ACCEPTED;
        }

        return meetsRequirements(player, quest) ? QuestState.AVAILABLE : QuestState.LOCKED;
    }

    public void startQuest(Player player, String id) {
        Quest quest = quests.get(id);
        if (quest == null) {
            player.sendMessage("§cQuest not found: " + id);
            return;
        }
        if (!requirementsMet(player, quest)) {
            return;
        }
        activeQuests.put(player.getUniqueId(), new PlayerQuestProgress(quest));
        trackedQuests.putIfAbsent(player.getUniqueId(), quest.getId());
        player.sendMessage("§aStarted quest: " + quest.getName());
    }

    public PlayerQuestProgress getProgress(UUID player) {
        return activeQuests.get(player);
    }

    public void setTrackedQuest(Player player, String questId) {
        trackedQuests.put(player.getUniqueId(), questId);
    }

    public String getTrackedQuest(UUID player) {
        return trackedQuests.get(player);
    }

    public void resetQuest(UUID player, String questId) {
        PlayerQuestProgress progress = activeQuests.get(player);
        if (progress != null && progress.getQuest().getId().equals(questId)) {
            activeQuests.remove(player);
        }
        Set<String> completed = completedQuests.get(player);
        if (completed != null) {
            completed.remove(questId);
        }
    }

    public void completeQuest(UUID player, String questId) {
        activeQuests.remove(player);
        completedQuests.computeIfAbsent(player, k -> new HashSet<>()).add(questId);
    }

    public String getQuestStatus(UUID player, String questId) {
        if (completedQuests.getOrDefault(player, Collections.emptySet()).contains(questId)) {
            return "§a" + questId + " is completed";
        }
        PlayerQuestProgress progress = activeQuests.get(player);
        if (progress != null && progress.getQuest().getId().equals(questId)) {
            StringBuilder sb = new StringBuilder("§e" + questId + " progress:");
            Quest quest = progress.getQuest();
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                sb.append(" \u00BB ").append(obj.getTarget()).append(": ")
                        .append(progress.getProgress(i)).append("/")
                        .append(obj.getAmount());
            }
            return sb.toString();
        }
        return "§c" + questId + " not started";
    }

    public boolean toggleDebug() {
        debug = !debug;
        return debug;
    }

    public boolean isDebug() {
        return debug;
    }

    public void handleKill(Player player, String mobType) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " killed " + mobType);
        }
        updateObjective(player, QuestObjectiveType.KILL, mobType, 1);
    }

    public void handleBuy(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " bought " + itemId);
        }
        updateObjective(player, QuestObjectiveType.BUY, itemId, 1);

        try {
            int id = Integer.parseInt(itemId);
            me.nakilex.levelplugin.items.data.CustomItem tpl = plugin.getItemManager().getTemplateById(id);
            if (tpl != null) {
                String classReq = tpl.getClassRequirement();
                PlayerClass playerClass = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
                if (classReq != null && playerClass.name().equalsIgnoreCase(classReq)) {
                    updateObjective(player, QuestObjectiveType.BUY, "class_weapon", 1);
                }
            }
        } catch (NumberFormatException ignore) {
        }
    }

    public void handleUpgrade(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " upgraded " + itemId);
        }
        updateObjective(player, QuestObjectiveType.UPGRADE, itemId, 1);
    }

    public void handleCast(Player player, String spellId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " cast " + spellId);
        }
        updateObjective(player, QuestObjectiveType.CAST, spellId, 1);
    }

    public void handleCraft(Player player, String itemType) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " crafted " + itemType);
        }
        updateObjective(player, QuestObjectiveType.CRAFT, itemType, 1);
    }

    public void handleDuel(Player player) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " won a duel");
        }
        updateObjective(player, QuestObjectiveType.DUEL, "WIN", 1);
    }

    public void handleEscort(Player player, String target) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " escorted " + target);
        }
        updateObjective(player, QuestObjectiveType.ESCORT, target, 1);
    }

    public void handleTalk(Player player, String npcId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " talked to " + npcId);
        }
        updateObjective(player, QuestObjectiveType.TALK, npcId, 1);
    }

    public void handleExplore(Player player, String regionId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " explored " + regionId);
        }
        updateObjective(player, QuestObjectiveType.EXPLORE, regionId, 1);
    }

    public void handleClassSelect(Player player) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " selected a class");
        }
        updateObjective(player, QuestObjectiveType.SELECT_CLASS, "ANY", 1);
    }

    private boolean requirementsMet(Player player, Quest quest) {
        return checkRequirements(player, quest, true);
    }

    public boolean meetsRequirements(Player player, Quest quest) {
        return checkRequirements(player, quest, false);
    }

    private boolean checkRequirements(Player player, Quest quest, boolean sendMsg) {
        int level = levelManager.getLevel(player);
        if (level < quest.getLevelRequirement()) {
            if (sendMsg)
                player.sendMessage("§cYou must be level " + quest.getLevelRequirement() + " to start this quest.");
            return false;
        }

        PlayerClass required = quest.getClassRequirement();
        if (required != null) {
            PlayerClass current = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
            if (current != required) {
                if (sendMsg)
                    player.sendMessage("§cThis quest requires the " + required.name() + " class.");
                return false;
            }
        }

        Set<String> completed = completedQuests.getOrDefault(player.getUniqueId(), Collections.emptySet());
        for (String req : quest.getQuestRequirements()) {
            if (!completed.contains(req)) {
                if (sendMsg)
                    player.sendMessage("§cYou must complete \"" + req + "\" first.");
                return false;
            }
        }

        return true;
    }

    private void updateObjective(Player player, QuestObjectiveType type, String target, int amount) {
        UUID uuid = player.getUniqueId();
        PlayerQuestProgress progress = activeQuests.get(uuid);
        if (progress == null) return;
        Quest quest = progress.getQuest();
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            QuestObjective obj = quest.getObjectives().get(i);
            if (obj.getType() == type && obj.getTarget().equalsIgnoreCase(target)) {
                progress.incrementProgress(i, amount, obj.isAllowOverflow(), obj.getAmount());
                if (debug) {
                    plugin.getLogger().info("[QuestDebug] " + player.getName() + " progressed " + quest.getId()
                            + " objective " + i + " -> " + progress.getProgress(i) + "/" + obj.getAmount());
                }
                shareProgress(player, progress, i, amount);
                player.sendMessage("§e[Quest] " + describeObjective(obj) + ": "
                        + progress.getProgress(i) + "/" + obj.getAmount());
                if (progress.isComplete()) {
                    if (debug) {
                        plugin.getLogger().info("[QuestDebug] " + player.getName() + " completed " + quest.getId());
                    }
                    activeQuests.remove(uuid);
                    completedQuests.computeIfAbsent(uuid, k -> new HashSet<>()).add(quest.getId());
                    if (quest.getId().equals(trackedQuests.get(uuid))) {
                        trackedQuests.remove(uuid);
                    }
                    sendCompletionMessage(player, quest);
                    giveRewards(player, quest);
                }
                break;
            }
        }
    }

    private void shareProgress(Player player, PlayerQuestProgress progress, int objectiveIndex, int amount) {
        Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) return;
        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(player.getUniqueId())) continue;
            PlayerQuestProgress other = activeQuests.get(memberId);
            if (other != null && other.getQuest().getId().equals(progress.getQuest().getId())) {
                QuestObjective obj = progress.getQuest().getObjectives().get(objectiveIndex);
                other.incrementProgress(objectiveIndex, amount, obj.isAllowOverflow(), obj.getAmount());
                if (debug) {
                    plugin.getLogger().info("[QuestDebug] Shared progress " + progress.getQuest().getId() + " to " + memberId);
                }
                Player p = Bukkit.getPlayer(memberId);
                if (p != null) {
                    p.sendMessage("§e[Party Quest] " + describeObjective(obj) + ": "
                            + other.getProgress(objectiveIndex) + "/" + obj.getAmount());
                    if (other.isComplete()) {
                        if (debug) {
                            plugin.getLogger().info("[QuestDebug] Party member " + p.getName() + " completed " + other.getQuest().getId());
                        }
                        activeQuests.remove(memberId);
                        completedQuests.computeIfAbsent(memberId, k -> new HashSet<>()).add(other.getQuest().getId());
                        sendCompletionMessage(p, other.getQuest());
                        giveRewards(p, other.getQuest());
                    }
                }
            }
        }
    }

    private void giveRewards(Player player, Quest quest) {
        QuestReward reward = quest.getReward();
        if (reward == null) return;

        if (reward.getXp() > 0) {
            levelManager.addXP(player, reward.getXp());
        }
        if (reward.getCoins() > 0) {
            plugin.getEconomyManager().addCoins(player, reward.getCoins());
        }
        if (reward.getGems() > 0) {
            plugin.getGemsManager().addUnits(player, reward.getGems());
        }
        if (!reward.getItemIds().isEmpty()) {
            for (int id : reward.getItemIds()) {
                me.nakilex.levelplugin.items.data.CustomItem tpl = plugin.getItemManager().getTemplateById(id);
                if (tpl != null) {
                    me.nakilex.levelplugin.items.data.CustomItem inst = plugin.getItemManager().rollNewInstance(id);
                    player.getInventory().addItem(
                        me.nakilex.levelplugin.items.utils.ItemUtil.createItemStackFromCustomItem(inst, 1, player)
                    );
                }
            }
        }
        if (!reward.getRuneIds().isEmpty()) {
            for (String runeId : reward.getRuneIds()) {
                me.nakilex.levelplugin.runes.model.Rune rune = plugin.getRunesManager().getRuneById(runeId);
                if (rune != null) {
                    org.bukkit.inventory.ItemStack item = plugin.getRunesManager().createUncarvedRuneItem(rune);
                    player.getInventory().addItem(item);
                }
            }
        }
    }

    /**
     * Display a styled quest completion message to the player.
     */
    private void sendCompletionMessage(Player player, Quest quest) {
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§a§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§6§lQuest Complete!");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§e" + quest.getName());
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§a§l-", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§aRewards:");

        QuestReward reward = quest.getReward();
        if (reward != null) {
            if (reward.getXp() > 0) {
                player.sendMessage("§a- §7" + reward.getXp() + " XP");
            }
            if (reward.getCoins() > 0) {
                player.sendMessage("§a- §7" + reward.getCoins() + " Coins");
            }
            if (reward.getGems() > 0) {
                player.sendMessage("§a- §7" + reward.getGems() + " Gems");
            }
            for (int id : reward.getItemIds()) {
                me.nakilex.levelplugin.items.data.CustomItem tpl = plugin.getItemManager().getTemplateById(id);
                String name = tpl != null ? tpl.getBaseName() : ("Item " + id);
                player.sendMessage("§a- §7" + name);
            }
            for (String runeId : reward.getRuneIds()) {
                me.nakilex.levelplugin.runes.model.Rune rune = plugin.getRunesManager().getRuneById(runeId);
                String name = rune != null ? rune.getDisplayName() : runeId;
                player.sendMessage("§a- §7" + name);
            }
        }
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "§a§l-", 45);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    /**
     * Provide a short description of a quest objective.
     */
    public String describeObjective(QuestObjective obj) {
        switch (obj.getType()) {
            case KILL:
                return "Kill " + beautifyName(obj.getTarget());
            case COLLECT:
                return "Collect " + obj.getTarget();
            case INTERACT:
                return "Interact " + obj.getTarget();
            case BUY:
                if ("class_weapon".equalsIgnoreCase(obj.getTarget())) {
                    return "Buy class weapon";
                }
                return "Buy " + obj.getTarget();
            case UPGRADE:
                return "Upgrade " + obj.getTarget();
            case CAST:
                return "Cast " + obj.getTarget();
            case CRAFT:
                return "Craft " + obj.getTarget();
            case DUEL:
                return "Win a duel";
            case ESCORT:
                return "Escort " + obj.getTarget();
            case TALK:
                return "Talk to " + resolveNpcName(obj.getTarget());
            case EXPLORE:
                return "Explore " + obj.getTarget();
            case SELECT_CLASS:
                return "Select a class";
            default:
                return obj.getTarget();
        }
    }

    private String beautifyName(String raw) {
        String name = raw;
        try {
            org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.valueOf(raw.toUpperCase());
            String key = type.getKey().getKey();
            name = key.replace('_', ' ');
        } catch (IllegalArgumentException ignored) {}
        String[] parts = name.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private String resolveNpcName(String raw) {
        String idStr = raw;
        if (raw.toLowerCase().startsWith("npc")) {
            idStr = raw.substring(3);
        }
        try {
            int id = Integer.parseInt(idStr);
            net.citizensnpcs.api.npc.NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getById(id);
            if (npc != null) {
                return npc.getName();
            }
        } catch (NumberFormatException ignored) {}
        return raw;
    }

    public Set<String> getQuestIds() {
        return quests.keySet();
    }

    public boolean hasCompleted(UUID player, String questId) {
        return completedQuests.getOrDefault(player, Collections.emptySet()).contains(questId);
    }
}
