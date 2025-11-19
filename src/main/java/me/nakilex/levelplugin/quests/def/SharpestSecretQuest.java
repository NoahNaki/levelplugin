package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.data.QuestResetScript;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quest that unlocks the enchanting system for players.
 */
public class SharpestSecretQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {
    public static final String ID = "sharpsecret";
    public static final String NPC_KAZAN_NAME = "Guard Kazan";
    public static final String NPC_OSIRIS_NAME = "Enchanter";

    public static final String NPC_INTRO_TARGET = "npc_guard_kazan_intro";
    public static final String NPC_RETURN_TARGET = "npc_guard_kazan_return";
    public static final String NPC_OSIRIS_TARGET = "npc_enchanter_secret";
    public static final String WAIT_FOR_NIGHT_TARGET = "sharpsecret_night";
    public static final String ORCHID_DISCOVERY_TARGET = "midnight_orchid";

    public static final int TALK_INTRO_INDEX = 0;
    public static final int WAIT_FOR_NIGHT_INDEX = 1;
    public static final int FIND_ORCHID_INDEX = 2;
    public static final int TALK_RETURN_INDEX = 3;
    public static final int TALK_OSIRIS_INDEX = 4;
    public static final int ENCHANT_INDEX = 5;

    private static final String CONFIG_PATH = "quest-settings.sharpsecret.midnight-orchid";
    private static final NamespacedKey ENCHANT_TOKEN_KEY = new NamespacedKey(Main.getInstance(), "enchant_token");

    private static final List<String> INTRO_DIALOG = List.of(
            "Swordsman Kazan|Yes, it's sharp enough to split moonlight.",
            "Swordsman Kazan|Want the secret? Fetch me the Midnight Orchid that blooms only after dusk inside these walls.",
            "Swordsman Kazan|Wait for nightfall, scour the quiet courtyards, and bring it back without bruising a petal.",
            "Swordsman Kazan|Do that and I'll introduce you to someone whose knowledge cuts deeper than any blade."
    );

    private static final List<String> RETURN_DIALOG = List.of(
            "Swordsman Kazan|What?! You actually found it—its petals still shimmer.",
            "Swordsman Kazan|Head to the building by the west entrance and tell Osiris you're here for the tasting.",
            "Swordsman Kazan|When he asks the riddle, answer 'Secret'. He'll owe you a favor—and a free enchant."
    );

    private static final List<String> OSIRIS_INTRO_DIALOG = List.of(
            "Osiris|You came for the tasting? Then taste this...",
            "Osiris|Sweet when kept, bitter when shared. I die when spoken. What am I?"
    );

    private static final List<String> OSIRIS_REMINDER_DIALOG = List.of(
            "Osiris|The boutique is yours. Place your gear on the table and feel the weave of mana yourself."
    );

    private static SharpestSecretQuest instance;
    private static boolean lifecycleRegistered = false;

    private final Map<UUID, BukkitTask> nightWatchers = new HashMap<>();
    private final Map<UUID, Listener> orchidListeners = new HashMap<>();
    private final Location orchidLocation;
    private final double orchidRadiusSquared;

    public SharpestSecretQuest() {
        super(
                ID,
                "The Sharpest Secret",
                "Recover the Midnight Orchid for Kazan and earn access to Osiris' enchanting atelier.",
                createObjectives(loadOrchidLocation()),
                5,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(1200, 0, 0, List.of()),
                null,
                INTRO_DIALOG,
                false
        );
        instance = this;
        Location center = loadOrchidLocation();
        this.orchidLocation = center;
        double radius = Main.getInstance().getConfig().getDouble(CONFIG_PATH + ".radius", 6.0);
        this.orchidRadiusSquared = radius * radius;
        registerLifecycleListener();
    }

    private static Location loadOrchidLocation() {
        Main plugin = Main.getInstance();
        World fallbackWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(CONFIG_PATH);
        String worldName = section != null ? section.getString("world", "mmorpg") : "mmorpg";
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = fallbackWorld;
        }
        double x = section != null ? section.getDouble("x", 212.5) : 212.5;
        double y = section != null ? section.getDouble("y", 72.0) : 72.0;
        double z = section != null ? section.getDouble("z", -62.5) : -62.5;
        return new Location(world, x, y, z);
    }

    private static List<QuestObjective> createObjectives(Location orchidLocation) {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, NPC_INTRO_TARGET, 1,
                        BeaconTargets.npcByName(NPC_KAZAN_NAME)),
                new QuestObjective(QuestObjectiveType.DISCOVER, WAIT_FOR_NIGHT_TARGET, 1),
                new QuestObjective(QuestObjectiveType.DISCOVER, ORCHID_DISCOVERY_TARGET, 1,
                        BeaconTargets.staticLoc(orchidLocation)),
                new QuestObjective(QuestObjectiveType.TALK, NPC_RETURN_TARGET, 1,
                        BeaconTargets.npcByName(NPC_KAZAN_NAME)),
                new QuestObjective(QuestObjectiveType.TALK, NPC_OSIRIS_TARGET, 1,
                        BeaconTargets.npcByName(NPC_OSIRIS_NAME)),
                new QuestObjective(QuestObjectiveType.ENCHANT, "ANY", 1)
        );
    }

    public static List<String> getReturnDialog() {
        return RETURN_DIALOG;
    }

    public static List<String> getOsirisIntroDialog() {
        return OSIRIS_INTRO_DIALOG;
    }

    public static List<String> getOsirisReminderDialog() {
        return OSIRIS_REMINDER_DIALOG;
    }

    @Override
    public void onStart(Player player, Main plugin) {
        resumeTracking(player, plugin);
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        cleanupPlayer(player.getUniqueId());
        ItemStack token = createEnchantToken();
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(token);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                "Osiris slips an Enchant Token into your palm.");
    }

    @Override
    public void onReset(Player player, Main plugin) {
        cleanupPlayer(player.getUniqueId());
    }

    private void resumeTracking(Player player, Main plugin) {
        if (player == null || plugin == null) {
            return;
        }
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        if (progress == null) {
            cleanupPlayer(player.getUniqueId());
            return;
        }
        if (progress.getProgress(WAIT_FOR_NIGHT_INDEX) < 1) {
            startNightWatcher(player, plugin);
        } else {
            stopNightWatcher(player.getUniqueId());
        }
        if (progress.getProgress(WAIT_FOR_NIGHT_INDEX) >= 1
                && progress.getProgress(FIND_ORCHID_INDEX) < 1) {
            ensureOrchidTracker(player, plugin);
        } else if (progress.getProgress(FIND_ORCHID_INDEX) >= 1) {
            stopOrchidListener(player.getUniqueId());
        }
    }

    private void startNightWatcher(Player player, Main plugin) {
        UUID uuid = player.getUniqueId();
        stopNightWatcher(uuid);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                QuestManager questManager = plugin.getQuestManager();
                PlayerQuestProgress progress = questManager.getProgress(uuid, ID);
                if (progress == null || progress.getProgress(WAIT_FOR_NIGHT_INDEX) >= 1) {
                    cancel();
                    nightWatchers.remove(uuid);
                    return;
                }
                Player current = Bukkit.getPlayer(uuid);
                if (current == null) {
                    return;
                }
                long time = current.getWorld().getTime();
                if (time >= 13000 && time <= 23000) {
                    questManager.handleDiscover(current, WAIT_FOR_NIGHT_TARGET);
                    ChatMessageUtil.send(current, ChatMessageUtil.MessageType.INFO,
                            "Moonlight spills over the rooftops—time to find the Midnight Orchid.");
                    cancel();
                    nightWatchers.remove(uuid);
                    resumeTracking(current, plugin);
                }
            }
        }.runTaskTimer(plugin, 20L, 100L);
        nightWatchers.put(uuid, task);
    }

    private void ensureOrchidTracker(Player player, Main plugin) {
        UUID uuid = player.getUniqueId();
        if (orchidListeners.containsKey(uuid)) {
            return;
        }
        Listener listener = new Listener() {
            @EventHandler
            public void onMove(PlayerMoveEvent event) {
                if (!event.getPlayer().getUniqueId().equals(uuid)) {
                    return;
                }
                checkForOrchid(event.getPlayer(), plugin);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (event.getPlayer().getUniqueId().equals(uuid)) {
                    HandlerList.unregisterAll(this);
                    orchidListeners.remove(uuid);
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        orchidListeners.put(uuid, listener);
        checkForOrchid(player, plugin);
    }

    private void checkForOrchid(Player player, Main plugin) {
        if (player == null || orchidLocation.getWorld() == null) {
            return;
        }
        QuestManager questManager = plugin.getQuestManager();
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        if (progress == null) {
            stopOrchidListener(player.getUniqueId());
            return;
        }
        if (progress.getProgress(WAIT_FOR_NIGHT_INDEX) < 1
                || progress.getProgress(FIND_ORCHID_INDEX) >= 1) {
            stopOrchidListener(player.getUniqueId());
            return;
        }
        if (!player.getWorld().equals(orchidLocation.getWorld())) {
            return;
        }
        long time = player.getWorld().getTime();
        if (time < 13000 || time > 23000) {
            return;
        }
        if (player.getLocation().distanceSquared(orchidLocation) > orchidRadiusSquared) {
            return;
        }
        questManager.handleDiscover(player, ORCHID_DISCOVERY_TARGET);
        giveMidnightOrchid(player);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "You gently pluck the Midnight Orchid, its petals glowing in your hands.");
        stopOrchidListener(player.getUniqueId());
    }

    private void stopNightWatcher(UUID uuid) {
        BukkitTask task = nightWatchers.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void stopOrchidListener(UUID uuid) {
        Listener listener = orchidListeners.remove(uuid);
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }

    private void cleanupPlayer(UUID uuid) {
        stopNightWatcher(uuid);
        stopOrchidListener(uuid);
    }

    private static void registerLifecycleListener() {
        if (lifecycleRegistered) {
            return;
        }
        lifecycleRegistered = true;
        Main plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> {
                            if (instance != null) {
                                instance.resumeTracking(event.getPlayer(), plugin);
                            }
                        }, 20L);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (instance != null) {
                    instance.cleanupPlayer(event.getPlayer().getUniqueId());
                }
            }
        }, plugin);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (instance == null) {
                return;
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                instance.resumeTracking(online, plugin);
            }
        });
    }

    private void giveMidnightOrchid(Player player) {
        ItemStack flower = new ItemStack(Material.BLUE_ORCHID);
        ItemMeta meta = flower.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Midnight Orchid");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "A bloom that opens only beneath moonlight.");
            meta.setLore(lore);
            flower.setItemMeta(meta);
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(flower);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private ItemStack createEnchantToken() {
        ItemStack shard = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = shard.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Enchant Token");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Redeem with Osiris for a complimentary enchant.");
            lore.addAll(TooltipUtil.bulletList("Shift-click the Enchant button to spend one."));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(ENCHANT_TOKEN_KEY, PersistentDataType.BYTE, (byte) 1);
            shard.setItemMeta(meta);
        }
        return shard;
    }

    public static boolean canAccessEnchanting(UUID uuid) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return false;
        }
        if (questManager.hasCompleted(uuid, ID)) {
            return true;
        }
        PlayerQuestProgress progress = questManager.getProgress(uuid, ID);
        return progress != null && progress.getProgress(TALK_OSIRIS_INDEX) >= 1;
    }

    public static boolean shouldReceiveFreeEnchant(UUID uuid) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return false;
        }
        PlayerQuestProgress progress = questManager.getProgress(uuid, ID);
        if (progress == null) {
            return false;
        }
        return progress.getProgress(TALK_OSIRIS_INDEX) >= 1
                && progress.getProgress(ENCHANT_INDEX) < 1;
    }

    public static boolean hasEnchantToken(Player player) {
        if (player == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isEnchantToken(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean consumeEnchantToken(Player player) {
        if (player == null) {
            return false;
        }
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isEnchantToken(stack)) {
                if (stack.getAmount() <= 1) {
                    player.getInventory().clear(slot);
                } else {
                    stack.setAmount(stack.getAmount() - 1);
                    player.getInventory().setItem(slot, stack);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isEnchantToken(ItemStack stack) {
        if (stack == null || stack.getType() != Material.AMETHYST_SHARD) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(ENCHANT_TOKEN_KEY, PersistentDataType.BYTE);
    }
}
