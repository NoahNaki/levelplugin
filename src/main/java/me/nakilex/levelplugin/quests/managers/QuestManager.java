package me.nakilex.levelplugin.quests.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.party.Party;
import me.nakilex.levelplugin.party.PartyManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.quests.util.QuestMessageUtil;
import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.quests.data.*;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.data.QuestResetScript;
import me.nakilex.levelplugin.utils.NpcNameUtil;
import net.citizensnpcs.api.npc.NPC;
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
    private final Map<String, TalkTargetInfo> talkTargetMap = new HashMap<>();
    private final Map<String, String> objectiveLabels = new HashMap<>();
    private final Map<String, String> npcQuestNameMap = new HashMap<>();
    // Allow multiple quests to be active per player
    private final Map<UUID, Map<String, PlayerQuestProgress>> activeQuests = new HashMap<>();
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
        npcQuestMap.clear();
        npcQuestNameMap.clear();
        objectiveLabels.clear();
        // Register quests here manually.
        Quest office = new me.nakilex.levelplugin.quests.def.OfficeErrandsQuest();
        registerQuest(office);
        Quest nb = new me.nakilex.levelplugin.quests.def.NewBeginningQuest();
        Quest seras = new me.nakilex.levelplugin.quests.def.SerasQuest();
        Quest hawieCrabs = new me.nakilex.levelplugin.quests.def.HawieHermitCrabQuest();
        Quest rahirScorpid = new me.nakilex.levelplugin.quests.def.RahirScorpidQuest();
        Quest yasiyaArena = new me.nakilex.levelplugin.quests.def.YasiyaArenaQuest();
        Quest skeggSpiders = new me.nakilex.levelplugin.quests.def.SkeggSpiderQuest();
        Quest zoyaDungeon = new me.nakilex.levelplugin.quests.def.ZoyaDungeonQuest();
        Quest stableKeeper = new me.nakilex.levelplugin.quests.def.StableKeeperQuest();
        Quest sharpSecret = new me.nakilex.levelplugin.quests.def.SharpestSecretQuest();
        registerQuest(nb);
        registerQuest(seras);
        registerQuest(hawieCrabs);
        registerQuest(rahirScorpid);
        registerQuest(yasiyaArena);
        registerQuest(skeggSpiders);
        registerQuest(zoyaDungeon);
        registerQuest(stableKeeper);
        registerQuest(sharpSecret);
        me.nakilex.levelplugin.quests.def.SharpestSecretQuest.registerTalkTargets(this);
        me.nakilex.levelplugin.quests.def.SharpestSecretQuest.registerObjectiveLabels(this);
        registerNpcQuest(me.nakilex.levelplugin.quests.def.SharpestSecretQuest.NPC_KAZAN_NAME,
                me.nakilex.levelplugin.quests.def.SharpestSecretQuest.ID);
        registerNpcQuest(me.nakilex.levelplugin.quests.def.SharpestSecretQuest.NPC_OSIRIS_NAME,
                me.nakilex.levelplugin.quests.def.SharpestSecretQuest.ID);
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

    public void registerNpcQuest(String npcName, String questId) {
        if (npcName == null) {
            return;
        }
        npcQuestNameMap.put(NpcNameUtil.normalize(npcName), questId);
    }

    public void registerTalkTarget(String target, String npcName, String displayName) {
        if (target == null || npcName == null) {
            return;
        }
        TalkTargetInfo info = new TalkTargetInfo(npcName, displayName);
        talkTargetMap.put(target, info);
    }

    public void registerObjectiveLabel(String target, String description) {
        if (target == null || description == null) {
            return;
        }
        objectiveLabels.put(target, description);
    }

    public Quest getQuestByNpcId(int npcId) {
        String id = npcQuestMap.get(npcId);
        return id == null ? null : quests.get(id);
    }

    public Quest getQuestByNpc(NPC npc) {
        if (npc == null) {
            return null;
        }
        Quest quest = getQuestByNpcId(npc.getId());
        if (quest != null) {
            return quest;
        }
        String normalized = NpcNameUtil.normalize(npc.getName());
        if (normalized == null) {
            return null;
        }
        String questId = npcQuestNameMap.get(normalized);
        return questId == null ? null : quests.get(questId);
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
            ConfigurationSection activeSec = sec.getConfigurationSection("active");
            if (activeSec != null) {
                Map<String, PlayerQuestProgress> map = new HashMap<>();
                for (String qid : activeSec.getKeys(false)) {
                    Quest quest = quests.get(qid);
                    if (quest == null) continue;

                    ConfigurationSection qSec = activeSec.getConfigurationSection(qid);
                    ConfigurationSection progSec;
                    List<String> flagsList = null;
                    if (qSec != null) {
                        progSec = qSec.getConfigurationSection("progress");
                        flagsList = qSec.getStringList("flags");
                    } else {
                        progSec = activeSec.getConfigurationSection(qid + ".progress");
                        flagsList = activeSec.getStringList(qid + ".flags");
                    }

                    PlayerQuestProgress prog = new PlayerQuestProgress(quest);
                    if (progSec != null) {
                        for (String key : progSec.getKeys(false)) {
                            int index = Integer.parseInt(key);
                            int value = progSec.getInt(key);
                            prog.setProgress(index, value);
                        }
                    }
                    if (flagsList != null) {
                        for (String flag : flagsList) {
                            prog.addFlag(flag);
                        }
                    }

                    map.put(qid, prog);
                }
                if (!map.isEmpty()) {
                    activeQuests.put(uuid, map);
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
            Map<String, PlayerQuestProgress> map = activeQuests.get(uuid);
            if (map != null && !map.isEmpty()) {
                ConfigurationSection activeSec = sec.createSection("active");
                for (PlayerQuestProgress progress : map.values()) {
                    String qid = progress.getQuest().getId();
                    ConfigurationSection qSec = activeSec.createSection(qid);
                    ConfigurationSection progSec = qSec.createSection("progress");
                    for (int i = 0; i < progress.getQuest().getObjectives().size(); i++) {
                        progSec.set(String.valueOf(i), progress.getProgress(i));
                    }
                    qSec.set("flags", new ArrayList<>(progress.getFlags()));
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

        Map<String, PlayerQuestProgress> map = activeQuests.get(player.getUniqueId());
        PlayerQuestProgress progress = map == null ? null : map.get(quest.getId());
        if (progress != null) {
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
        Map<String, PlayerQuestProgress> map = activeQuests.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        if (map.containsKey(id)) {
            return; // already started
        }
        map.put(id, new PlayerQuestProgress(quest));
        // always track the most recently accepted quest
        trackedQuests.put(player.getUniqueId(), quest.getId());
        saveProgress();
        sendStartMessage(player, quest);

        if (quest instanceof me.nakilex.levelplugin.quests.data.QuestScript script) {
            script.onStart(player, plugin);
        }
    }

    /**
     * Get progress for the currently tracked quest, if any.
     */
    public PlayerQuestProgress getProgress(UUID player) {
        String tracked = trackedQuests.get(player);
        if (tracked == null) return null;
        Map<String, PlayerQuestProgress> map = activeQuests.get(player);
        return map == null ? null : map.get(tracked);
    }

    /**
     * Get progress for a specific quest, if the player has it active.
     */
    public PlayerQuestProgress getProgress(UUID player, String questId) {
        Map<String, PlayerQuestProgress> map = activeQuests.get(player);
        return map == null ? null : map.get(questId);
    }

    /** Invoke any reset cleanup logic for a quest while the player object is available. */
    public void cleanupQuest(Player player, String questId) {
        Quest quest = quests.get(questId);
        if (quest instanceof QuestResetScript reset) {
            reset.onReset(player, plugin);
        }
    }

    public void setTrackedQuest(Player player, String questId) {
        trackedQuests.put(player.getUniqueId(), questId);
        saveProgress();
    }

    public String getTrackedQuest(UUID player) {
        return trackedQuests.get(player);
    }

    /** Mark a quest-specific flag for the given player. */
    public void setFlag(UUID player, String questId, String flag) {
        Map<String, PlayerQuestProgress> map = activeQuests.get(player);
        if (map == null) return;
        PlayerQuestProgress prog = map.get(questId);
        if (prog != null) {
            prog.addFlag(flag);
            if (debug) {
                Player p = Bukkit.getPlayer(player);
                String name = p != null ? p.getName() : player.toString();
                plugin.getLogger().info("[QuestDebug] Set flag " + questId + ":" + flag + " for " + name);
            }
            saveProgress();
        }
    }

    /** Check if the player currently has the specified quest flag. */
    public boolean hasFlag(UUID player, String questId, String flag) {
        Map<String, PlayerQuestProgress> map = activeQuests.get(player);
        PlayerQuestProgress prog = map == null ? null : map.get(questId);
        return prog != null && prog.hasFlag(flag);
    }

    /** Remove a quest flag from the player. */
    public void removeFlag(UUID player, String questId, String flag) {
        Map<String, PlayerQuestProgress> map = activeQuests.get(player);
        if (map == null) return;
        PlayerQuestProgress prog = map.get(questId);
        if (prog != null) {
            prog.removeFlag(flag);
            if (debug) {
                Player p = Bukkit.getPlayer(player);
                String name = p != null ? p.getName() : player.toString();
                plugin.getLogger().info("[QuestDebug] Removed flag " + questId + ":" + flag + " for " + name);
            }
            saveProgress();
        }
    }

    public void resetQuest(UUID player, String questId) {
        resetQuest(player, questId, false);
    }

    /**
     * Reset a quest for the given player.
     *
     * @param ignoreMain if true, allow resetting even when the quest is marked
     *                   as a main quest
     */
    public void resetQuest(UUID player, String questId, boolean ignoreMain) {
        resetQuestInternal(player, questId, ignoreMain);
        saveProgress();
    }

    private void resetQuestInternal(UUID player, String questId, boolean ignoreMain) {
        Quest quest = quests.get(questId);
        if (quest != null && quest.isMainQuest() && !ignoreMain) {
            return;
        }
        Player p = Bukkit.getPlayer(player);
        if (p != null && quest instanceof QuestResetScript reset) {
            reset.onReset(p, plugin);
        }
        Map<String, PlayerQuestProgress> map = activeQuests.get(player);
        if (map != null) {
            map.remove(questId);
            if (map.isEmpty()) {
                activeQuests.remove(player);
            }
        }
        Set<String> completed = completedQuests.get(player);
        if (completed != null) {
            completed.remove(questId);
        }
        String tracked = trackedQuests.get(player);
        if (questId.equals(tracked)) {
            trackedQuests.remove(player);
        }
    }

    /**
     * Remove all quest progress for a player.
     */
    public void clearPlayerData(UUID player) {
        Map<String, PlayerQuestProgress> active = activeQuests.get(player);
        if (active != null) {
            for (String qid : new java.util.ArrayList<>(active.keySet())) {
                resetQuestInternal(player, qid, true);
            }
        }
        Set<String> completed = completedQuests.get(player);
        if (completed != null) {
            for (String qid : new java.util.ArrayList<>(completed)) {
                resetQuestInternal(player, qid, true);
            }
        }
        trackedQuests.remove(player);
        saveProgress();
    }

    public void completeQuest(UUID player, String questId) {
        Map<String, PlayerQuestProgress> map = activeQuests.get(player);
        if (map != null) {
            map.remove(questId);
            if (map.isEmpty()) {
                activeQuests.remove(player);
            }
        }
        completedQuests.computeIfAbsent(player, k -> new HashSet<>()).add(questId);
        Player p = Bukkit.getPlayer(player);
        Quest quest = quests.get(questId);
        if (p != null && quest instanceof QuestResetScript reset) {
            reset.onReset(p, plugin);
        }
    }

    public String getQuestStatus(UUID player, String questId) {
        if (completedQuests.getOrDefault(player, Collections.emptySet()).contains(questId)) {
            return "§a" + questId + " is completed";
        }
        Map<String, PlayerQuestProgress> map = activeQuests.get(player);
        PlayerQuestProgress progress = map == null ? null : map.get(questId);
        if (progress != null) {
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
                if (id >= 16 && id <= 19) {
                    updateObjective(player, QuestObjectiveType.BUY, "starter_armor", 1);
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

    public void handleEnchant(Player player) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " enchanted an item");
        }
        updateObjective(player, QuestObjectiveType.ENCHANT, "ANY", 1);
    }

    public void handleDiscover(Player player, String regionId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " discovered " + regionId);
        }
        updateObjective(player, QuestObjectiveType.DISCOVER, regionId, 1);
    }

    public void handleConsumePotion(Player player, String potionId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " consumed " + potionId);
        }
        updateObjective(player, QuestObjectiveType.CONSUME_POTION, potionId, 1);
    }

    public void handleAuctionBuy(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " bought from auction " + itemId);
        }
        updateObjective(player, QuestObjectiveType.AUCTION_BUY, itemId, 1);
    }

    public void handleAuctionList(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " listed on auction " + itemId);
        }
        updateObjective(player, QuestObjectiveType.AUCTION_LIST, itemId, 1);
    }

    public void handleAuctionSell(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " sold on auction " + itemId);
        }
        updateObjective(player, QuestObjectiveType.AUCTION_SELL, itemId, 1);
    }

    public void handleAuctionBid(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " bid on auction " + itemId);
        }
        updateObjective(player, QuestObjectiveType.AUCTION_BID, itemId, 1);
    }

    public void handleTownUpgrade(Player player) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " upgraded town");
        }
        updateObjective(player, QuestObjectiveType.TOWN_UPGRADE, "ANY", 1);
    }

    public void handlePlayTime(Player player, int minutes) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " played " + minutes + "m");
        }
        updateObjective(player, QuestObjectiveType.PLAY_TIME, "MINUTES", minutes);
    }

    public void handleRepair(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " repaired " + itemId);
        }
        updateObjective(player, QuestObjectiveType.BLACKSMITH_REPAIR, itemId, 1);
    }

    public void handleReroll(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " rerolled " + itemId);
        }
        updateObjective(player, QuestObjectiveType.BLACKSMITH_REROLL, itemId, 1);
    }

    public void handleSalvage(Player player, String itemId) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " salvaged " + itemId);
        }
        updateObjective(player, QuestObjectiveType.SALVAGE, itemId, 1);
    }

    public void handleWaystoneUnlock(Player player, String id) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " unlocked waystone " + id);
        }
        updateObjective(player, QuestObjectiveType.WAYSTONE_UNLOCK, id, 1);
    }

    public void handleWaystoneUse(Player player, String id) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " used waystone " + id);
        }
        updateObjective(player, QuestObjectiveType.WAYSTONE_USE, id, 1);
    }

    public void handleCastCombo(Player player, String combo) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " combo " + combo);
        }
        updateObjective(player, QuestObjectiveType.CAST_COMBO, combo, 1);
    }


    public void handleDuelParticipate(Player player) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " participated in duel");
        }
        updateObjective(player, QuestObjectiveType.DUEL_PARTICIPATE, "ANY", 1);
    }

    public void handleDuelLose(Player player) {
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " lost a duel");
        }
        updateObjective(player, QuestObjectiveType.DUEL_LOSE, "ANY", 1);
    }

    public void handleArenaMatchComplete(Player player, String matchType) {
        if (player == null) {
            return;
        }
        String target = matchType == null ? "ANY" : matchType;
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " completed arena match " + target);
        }
        updateObjective(player, QuestObjectiveType.ARENA_MATCH, target, 1);
        if (!"ANY".equalsIgnoreCase(target)) {
            updateObjective(player, QuestObjectiveType.ARENA_MATCH, "ANY", 1);
        }
    }

    public void handleDungeonCreate(Player player, String dungeonKey) {
        if (player == null) {
            return;
        }
        String target = (dungeonKey == null || dungeonKey.isEmpty()) ? "ANY" : dungeonKey;
        if (debug) {
            plugin.getLogger().info("[QuestDebug] " + player.getName() + " saved dungeon " + target);
        }
        updateObjective(player, QuestObjectiveType.DUNGEON_CREATE, target, 1);
        if (!"ANY".equalsIgnoreCase(target)) {
            updateObjective(player, QuestObjectiveType.DUNGEON_CREATE, "ANY", 1);
        }
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
        Map<String, PlayerQuestProgress> map = activeQuests.get(uuid);
        if (map == null) return;
        outer:
        for (Iterator<Map.Entry<String, PlayerQuestProgress>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, PlayerQuestProgress> entry = it.next();
            PlayerQuestProgress progress = entry.getValue();
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
                    if (progress.isComplete()) {
                        if (debug) {
                            plugin.getLogger().info("[QuestDebug] " + player.getName() + " completed " + quest.getId());
                        }
                        it.remove();
                        if (map.isEmpty()) {
                            activeQuests.remove(uuid);
                        }
                        completedQuests.computeIfAbsent(uuid, k -> new HashSet<>()).add(quest.getId());
                        if (quest.getId().equals(trackedQuests.get(uuid))) {
                            trackedQuests.remove(uuid);
                        }
                        if (!"officeerrands".equalsIgnoreCase(quest.getId())) {
                            QuestMessageUtil.sendCompletionMessage(player,
                                    "§6§lQuest Complete!", quest.getName(),
                                    0, 0, quest.getReward());
                        }
                        giveRewards(player, quest);
                        if (quest instanceof me.nakilex.levelplugin.quests.data.QuestCompletionScript script) {
                            script.onComplete(player, plugin);
                        }
                    }
                    break outer;
                }
            }
        }
    }

    private void shareProgress(Player player, PlayerQuestProgress progress, int objectiveIndex, int amount) {
        Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) return;
        for (UUID memberId : party.getMembers()) {
            if (memberId.equals(player.getUniqueId())) continue;
            Map<String, PlayerQuestProgress> map = activeQuests.get(memberId);
            PlayerQuestProgress other = map == null ? null : map.get(progress.getQuest().getId());
            if (other != null) {
                QuestObjective obj = progress.getQuest().getObjectives().get(objectiveIndex);
                other.incrementProgress(objectiveIndex, amount, obj.isAllowOverflow(), obj.getAmount());
                if (debug) {
                    plugin.getLogger().info("[QuestDebug] Shared progress " + progress.getQuest().getId() + " to " + memberId);
                }
                Player p = Bukkit.getPlayer(memberId);
                if (p != null) {
                    if (other.isComplete()) {
                        if (debug) {
                            plugin.getLogger().info("[QuestDebug] Party member " + p.getName() + " completed " + other.getQuest().getId());
                        }
                        String oid = other.getQuest().getId();
                        map.remove(oid);
                        if (map.isEmpty()) {
                            activeQuests.remove(memberId);
                        }
                        if (oid.equals(trackedQuests.get(memberId))) {
                            trackedQuests.remove(memberId);
                        }
                        completedQuests.computeIfAbsent(memberId, k -> new HashSet<>()).add(other.getQuest().getId());
                        if (!"officeerrands".equalsIgnoreCase(other.getQuest().getId())) {
                            QuestMessageUtil.sendCompletionMessage(p,
                                    "§6§lQuest Complete!", other.getQuest().getName(),
                                    0, 0, other.getQuest().getReward());
                        }
                        giveRewards(p, other.getQuest());
                        if (other.getQuest() instanceof me.nakilex.levelplugin.quests.data.QuestCompletionScript script) {
                            script.onComplete(p, plugin);
                        }
                    }
                }
            }
        }
    }

    private void giveRewards(Player player, Quest quest) {
        applyReward(player, quest.getReward());
    }

    /**
     * Apply a raw {@link QuestReward} to the player. Extracted so other
     * systems like guild quests can reuse reward logic.
     */
    public void applyReward(Player player, QuestReward reward) {
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

        for (me.nakilex.levelplugin.player.classes.data.PlayerClass pc : reward.getUnlockClasses()) {
            StatsManager.getInstance().unlockClass(player.getUniqueId(), pc);
        }
    }

    /**
     * Provide a short description of a quest objective.
     */
    public String describeObjective(QuestObjective obj) {
        if (objectiveLabels.containsKey(obj.getTarget())) {
            return objectiveLabels.get(obj.getTarget());
        }
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
                if ("starter_armor".equalsIgnoreCase(obj.getTarget())) {
                    return "Buy starter armor";
                }
                if ("stablekeeper_horse".equalsIgnoreCase(obj.getTarget())) {
                    return "Buy a horse from the Stable Keeper";
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
            case ENCHANT:
                return "Enchant an item";
            case DISCOVER:
                return "Discover " + obj.getTarget();
            case CONSUME_POTION:
                return "Consume " + obj.getTarget();
            case PLAY_TIME:
                return "Play for " + obj.getAmount() + " minutes";
            case AUCTION_BUY:
                return "Buy from auction";
            case AUCTION_LIST:
                return "List an item on auction";
            case AUCTION_SELL:
                return "Sell an item on auction";
            case AUCTION_BID:
                return "Bid on an auction";
            case TOWN_UPGRADE:
                return "Upgrade your town";
            case BLACKSMITH_REPAIR:
                return "Repair an item";
            case BLACKSMITH_REROLL:
                return "Reroll an item";
            case SALVAGE:
                return "Salvage items";
            case WAYSTONE_UNLOCK:
                return "Unlock waystone " + obj.getTarget();
            case WAYSTONE_USE:
                return "Use waystone " + obj.getTarget();
            case CAST_COMBO:
                return "Cast combo " + obj.getTarget();
            case DUEL_PARTICIPATE:
                return "Participate in a duel";
            case DUEL_LOSE:
                return "Lose a duel";
            case LOOTCHEST_OPEN:
                return "Open loot chests";
            case SIEGE_PARTICIPATE:
                return "Participate in a siege";
            case DUEL_WIN:
                return "Win a duel";
            case ARENA_MATCH:
                return describeArenaObjective(obj.getTarget());
            case DUNGEON_CREATE:
                if (obj.getTarget() == null || obj.getTarget().equalsIgnoreCase("ANY")) {
                    return "Create and save a dungeon";
                }
                return "Save the dungeon \"" + obj.getTarget() + "\"";
            default:
                return obj.getTarget();
        }
    }

    private String describeArenaObjective(String target) {
        if (target == null || target.equalsIgnoreCase("ANY")) {
            return "Complete an /arena match";
        }
        String queueHint = " (/arena to queue)";
        String pretty = target.toLowerCase().replace('_', ' ');
        return "Complete a " + pretty + " /arena match" + queueHint;
    }

    /**
     * Display a styled quest start message to the player.
     */
    private void sendStartMessage(Player player, Quest quest) {
        if ("officeerrands".equalsIgnoreCase(quest.getId())) {
            return;
        }
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "", 45);
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§6§lQuest Started!");
        me.nakilex.levelplugin.utils.ChatFormatter.sendCenteredMessage(player, "§e" + quest.getName());
        me.nakilex.levelplugin.utils.ChatFormatter.constructDivider(player, "", 45);
    }

    private String beautifyName(String raw) {
        return MobNameUtil.getPlainDisplayName(raw);
    }

    private String resolveNpcName(String raw) {
        TalkTargetInfo info = talkTargetMap.get(raw);
        if (info != null) {
            return info.getDisplayName();
        }
        String idPart = raw;
        if (raw.toLowerCase().startsWith("npc")) {
            idPart = raw.substring(3);
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < idPart.length(); i++) {
            char c = idPart.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        if (digits.length() > 0) {
            try {
                int id = Integer.parseInt(digits.toString());
                net.citizensnpcs.api.npc.NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getById(id);
                if (npc != null) {
                    return npc.getName();
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return raw;
    }

    public boolean isTalkObjectiveForNpc(QuestObjective obj, NPC npc) {
        if (obj == null || npc == null || obj.getType() != QuestObjectiveType.TALK) {
            return false;
        }
        TalkTargetInfo info = talkTargetMap.get(obj.getTarget());
        if (info != null) {
            String normalized = NpcNameUtil.normalize(npc.getName());
            return normalized != null && normalized.equals(info.getNormalizedName());
        }
        String target = obj.getTarget().toLowerCase();
        return target.startsWith("npc" + npc.getId());
    }

    private static class TalkTargetInfo {
        private final String displayName;
        private final String normalizedName;

        TalkTargetInfo(String npcName, String displayName) {
            this.displayName = (displayName == null || displayName.isBlank()) ? npcName : displayName;
            this.normalizedName = NpcNameUtil.normalize(npcName);
        }

        String getDisplayName() {
            return displayName;
        }

        String getNormalizedName() {
            return normalizedName;
        }
    }

    public Set<String> getQuestIds() {
        return quests.keySet();
    }

    /** Number of quests the player has completed. */
    public int getCompletedQuestCount(java.util.UUID player) {
        return completedQuests.getOrDefault(player, java.util.Collections.emptySet()).size();
    }

    /** Total number of registered quests. */
    public int getTotalQuestCount() {
        return quests.size();
    }

    public boolean hasCompleted(UUID player, String questId) {
        return completedQuests.getOrDefault(player, Collections.emptySet()).contains(questId);
    }
}
