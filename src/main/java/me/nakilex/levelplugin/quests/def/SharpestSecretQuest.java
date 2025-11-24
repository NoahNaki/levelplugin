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
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;
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
    private static final NamespacedKey MIDNIGHT_ORCHID_KEY = new NamespacedKey(Main.getInstance(), "midnight_orchid");
    private static final long ORCHID_COOLDOWN_MS = 10_000L;
    private static final long BLOOM_START_TICK = 17_500L;
    private static final long BLOOM_END_TICK = 23_500L;
    private static final long DAY_RESET_TICK = 12_000L;
    private static final long BLOOM_TASK_INTERVAL = 100L;
    private static final String ORCHID_HOLOGRAM_TAG = "sharpsecret_orchid";
    private static final List<String> ORCHID_HOLOGRAM_LINES = List.of(
            ChatColor.GRAY + "Right-click to pluck"
    );

    private static final List<String> INTRO_DIALOG = List.of(
            "Swordsman Kazan|Yes, what is it, greenie? Oh—this sword? Sharp enough to split moonlight.",
            "Swordsman Kazan|How did I get it this sharp? Hah... that isn't a secret I spill without a favor in return.",
            "Swordsman Kazan|There's a barkeep at the tavern—the most beautiful you'll ever see—and I can't find the flowers she loves.",
            "Swordsman Kazan|They whisper that a Midnight Orchid blooms only within these walls, but I've never caught it.",
            "Swordsman Kazan|Under the large oak near the main entrance, a blue bloom opens only at midnight. Bring it to me and the sword's secret is yours."
    );

    private static final List<String> RETURN_DIALOG = List.of(
            "Swordsman Kazan|What?! You found it! The Midnight Orchid—petals still glimmering.",
            "Swordsman Kazan|Head to the library and talk to Osiris, tell him you're there for the tasting.",
            "Swordsman Kazan|When he asks his riddle, answer 'Secret'. That's your payment—and a free enchant."
    );

    private static final List<String> OSIRIS_INTRO_DIALOG = List.of(
            "Osiris|You came for the tasting? Then taste this...",
            "Osiris|Sweet when kept, bitter when shared.",
            "Osiris|I die when spoken.",
            "Osiris|What am I?"
    );

    private static final List<String> OSIRIS_REMINDER_DIALOG = List.of(
            "Osiris|My humble shop is yours. Place your gear upon the table and feel the mana weave."
    );

    private static SharpestSecretQuest instance;
    private static boolean lifecycleRegistered = false;

    private final Map<UUID, BukkitTask> nightWatchers = new HashMap<>();
    private final Map<UUID, Long> orchidCooldowns = new HashMap<>();
    private final Location orchidLocation;
    private final MultiLineHologram orchidHologram;
    private BlockData dormantBlockData;
    private boolean orchidBloomed;
    private BukkitTask bloomTask;

    public SharpestSecretQuest() {
        super(
                ID,
                "The Sharpest Secret",
                "Recover the Midnight Orchid for Kazan and earn access to Osiris' enchanting atelier.",
                createObjectives(loadOrchidLocation()),
                20,
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
        Location holoLocation = center.clone().add(0.5, 1.25, 0.5);
        this.orchidHologram = new MultiLineHologram(holoLocation, ORCHID_HOLOGRAM_TAG);
        if (center.getWorld() != null) {
            Block block = center.getBlock();
            if (block.getType() != Material.BLUE_ORCHID) {
                this.dormantBlockData = block.getBlockData().clone();
            }
        }
        registerLifecycleListener();
        registerOrchidInteractionListener(Main.getInstance());
        startOrchidBloomTask(Main.getInstance());
        updateOrchidBloom();
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
        double x = section != null ? section.getDouble("x", 195.0) : 195.0;
        double y = section != null ? section.getDouble("y", 68.0) : 68.0;
        double z = section != null ? section.getDouble("z", -217.0) : -217.0;
        return new Location(world, x, y, z);
    }

    private static List<QuestObjective> createObjectives(Location orchidLocation) {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, NPC_INTRO_TARGET, 1,
                        false,
                        BeaconTargets.npc(NPC_KAZAN_NAME),
                        "Speak with Guard Kazan about his blade."),
                new QuestObjective(QuestObjectiveType.DISCOVER, WAIT_FOR_NIGHT_TARGET, 1,
                        false,
                        null,
                        "Wait for midnight inside the town walls."),
                new QuestObjective(QuestObjectiveType.DISCOVER, ORCHID_DISCOVERY_TARGET, 1,
                        false,
                        BeaconTargets.staticLoc(orchidLocation),
                        "Pluck the Midnight Orchid beneath the great oak."),
                new QuestObjective(QuestObjectiveType.TALK, NPC_RETURN_TARGET, 1,
                        false,
                        BeaconTargets.npc(NPC_KAZAN_NAME),
                        "Bring the Midnight Orchid back to Guard Kazan."),
                new QuestObjective(QuestObjectiveType.TALK, NPC_OSIRIS_TARGET, 1,
                        false,
                        BeaconTargets.npc(NPC_OSIRIS_NAME),
                        "Head to the library and tell Osiris you're here for the tasting."),
                new QuestObjective(QuestObjectiveType.ENCHANT, "ANY", 1)
        );
    }

    public static List<String> getReturnDialog() {
        return RETURN_DIALOG;
    }

    public static List<String> getIntroDialog() {
        return INTRO_DIALOG;
    }

    public static void registerTalkTargets(QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(NPC_INTRO_TARGET, NPC_KAZAN_NAME, NPC_KAZAN_NAME);
        questManager.registerTalkTarget(NPC_RETURN_TARGET, NPC_KAZAN_NAME, NPC_KAZAN_NAME);
        questManager.registerTalkTarget(NPC_OSIRIS_TARGET, NPC_OSIRIS_NAME, "Osiris");
    }

    public static List<String> getOsirisIntroDialog() {
        return OSIRIS_INTRO_DIALOG;
    }

    public static List<String> getOsirisReminderDialog() {
        return OSIRIS_REMINDER_DIALOG;
    }

    public static List<String> getOsirisSuccessDialog(String playerName) {
        String speaker = (playerName == null || playerName.isBlank()) ? "Traveler" : playerName;
        return List.of(
                "Osiris|Correct. Welcome to my humble shop, " + speaker + ".",
                speaker + "|Wait... how do you know my name?",
                "Osiris|I know more about this town than most suspect. Now—shall we see what your equipment can become?"
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager != null) {
            PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
            if (progress != null && progress.getProgress(TALK_INTRO_INDEX) < 1) {
                questManager.handleTalk(player, NPC_INTRO_TARGET);
            }
        }
        resumeTracking(player, plugin);
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        cleanupPlayer(player.getUniqueId());
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
    }

    private void startNightWatcher(Player player, Main plugin) {
        UUID uuid = player.getUniqueId();
        stopNightWatcher(uuid);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                QuestManager questManager = plugin.getQuestManager();
                if (questManager == null) {
                    cancel();
                    nightWatchers.remove(uuid);
                    return;
                }
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
                    cancel();
                    nightWatchers.remove(uuid);
                    resumeTracking(current, plugin);
                }
            }
        }.runTaskTimer(plugin, 20L, 100L);
        nightWatchers.put(uuid, task);
    }

    private void registerOrchidInteractionListener(Main plugin) {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(ignoreCancelled = true)
            public void onInteract(PlayerInteractEvent event) {
                if (event.getHand() == EquipmentSlot.OFF_HAND) {
                    return;
                }
                if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                    return;
                }
                if (!isOrchidBlock(event.getClickedBlock())) {
                    return;
                }
                event.setCancelled(true);
                handleOrchidPluck(event.getPlayer());
            }
        }, plugin);
    }

    private void startOrchidBloomTask(Main plugin) {
        if (orchidLocation == null) {
            return;
        }
        if (bloomTask != null) {
            bloomTask.cancel();
        }
        bloomTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateOrchidBloom();
            }
        }.runTaskTimer(plugin, 20L, BLOOM_TASK_INTERVAL);
    }

    private void updateOrchidBloom() {
        if (orchidLocation == null) {
            return;
        }
        World world = orchidLocation.getWorld();
        if (world == null) {
            return;
        }
        Block block = orchidLocation.getBlock();
        if (dormantBlockData == null && block.getType() != Material.BLUE_ORCHID) {
            dormantBlockData = block.getBlockData().clone();
        }
        long time = world.getTime();
        if (!orchidBloomed && isMidnightWindow(time)) {
            bloomOrchid();
            return;
        }
        if (orchidBloomed && time < DAY_RESET_TICK) {
            witherOrchid();
            return;
        }
        if (orchidBloomed && block.getType() != Material.BLUE_ORCHID) {
            block.setType(Material.BLUE_ORCHID, false);
        }
    }

    private boolean isMidnightWindow(long time) {
        if (BLOOM_START_TICK <= BLOOM_END_TICK) {
            return time >= BLOOM_START_TICK && time <= BLOOM_END_TICK;
        }
        return time >= BLOOM_START_TICK || time <= BLOOM_END_TICK;
    }

    private void bloomOrchid() {
        if (orchidLocation == null || orchidLocation.getWorld() == null) {
            return;
        }
        Block block = orchidLocation.getBlock();
        block.setType(Material.BLUE_ORCHID, false);
        orchidBloomed = true;
        spawnOrchidHologram();
    }

    private void witherOrchid() {
        if (orchidLocation == null || orchidLocation.getWorld() == null) {
            return;
        }
        Block block = orchidLocation.getBlock();
        if (dormantBlockData != null) {
            block.setBlockData(dormantBlockData, false);
        } else {
            block.setType(Material.AIR, false);
        }
        orchidBloomed = false;
        despawnOrchidHologram();
    }

    private void spawnOrchidHologram() {
        if (orchidHologram == null || orchidLocation.getWorld() == null) {
            return;
        }
        orchidHologram.spawn(ORCHID_HOLOGRAM_LINES);
    }

    private void despawnOrchidHologram() {
        if (orchidHologram != null) {
            orchidHologram.despawn();
        }
    }

    private void handleOrchidPluck(Player player) {
        if (player == null) {
            return;
        }
        Main plugin = Main.getInstance();
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (questManager.hasCompleted(uuid, ID)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Osiris already trusts you with the tasting.");
            return;
        }
        PlayerQuestProgress progress = questManager.getProgress(uuid, ID);
        if (progress == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "The bloom recoils. Maybe Kazan can tell you more.");
            return;
        }
        if (progress.getProgress(WAIT_FOR_NIGHT_INDEX) < 1) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Patience. Wait until midnight inside the town walls.");
            return;
        }
        if (!orchidBloomed) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Only roots remain. Return when the moon crowns the oak.");
            return;
        }
        if (progress.getProgress(FIND_ORCHID_INDEX) >= 1) {
            if (hasMidnightOrchid(player)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "You've already plucked the Midnight Orchid—bring it to Kazan.");
                return;
            }
        }
        long now = System.currentTimeMillis();
        long last = orchidCooldowns.getOrDefault(uuid, 0L);
        if (now - last < ORCHID_COOLDOWN_MS) {
            long remaining = Math.max(1L, (ORCHID_COOLDOWN_MS - (now - last) + 999L) / 1000L);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "The petals need a moment to recover. Try again in " + remaining + "s.");
            return;
        }
        orchidCooldowns.put(uuid, now);
        if (progress.getProgress(FIND_ORCHID_INDEX) < 1) {
            questManager.handleDiscover(player, ORCHID_DISCOVERY_TARGET);
        }
        giveMidnightOrchid(player);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "You carefully pluck the Midnight Orchid, its glow lingering in your palm.");
    }

    public static void removeMidnightOrchid(Player player) {
        if (player == null) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isMidnightOrchid(stack)) {
                player.getInventory().clear(slot);
            }
        }
    }

    private boolean isOrchidBlock(Block block) {
        if (block == null || orchidLocation == null || orchidLocation.getWorld() == null) {
            return false;
        }
        if (!block.getWorld().equals(orchidLocation.getWorld())) {
            return false;
        }
        return block.getX() == orchidLocation.getBlockX()
                && block.getY() == orchidLocation.getBlockY()
                && block.getZ() == orchidLocation.getBlockZ();
    }

    private void shutdownOrchidTasks() {
        if (bloomTask != null) {
            bloomTask.cancel();
            bloomTask = null;
        }
        witherOrchid();
    }

    private void stopNightWatcher(UUID uuid) {
        BukkitTask task = nightWatchers.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void cleanupPlayer(UUID uuid) {
        stopNightWatcher(uuid);
        orchidCooldowns.remove(uuid);
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

            @EventHandler
            public void onPluginDisable(PluginDisableEvent event) {
                if (event.getPlugin().equals(plugin) && instance != null) {
                    instance.shutdownOrchidTasks();
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
            meta.setLore(TooltipUtil.questItemLore("Plucked beneath the oak as midnight struck.", true));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(ItemUtil.SOULBOUND_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(MIDNIGHT_ORCHID_KEY, PersistentDataType.BYTE, (byte) 1);
            flower.setItemMeta(meta);
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(flower);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    public static void giveEnchantToken(Player player) {
        if (player == null) {
            return;
        }
        ItemStack token = createEnchantToken();
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(token);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                "Osiris slips an Enchant Token into your palm.");
    }

    private static ItemStack createEnchantToken() {
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

    private static boolean isMidnightOrchid(ItemStack stack) {
        if (stack == null || stack.getType() != Material.BLUE_ORCHID) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(MIDNIGHT_ORCHID_KEY, PersistentDataType.BYTE);
    }

    public static boolean hasMidnightOrchid(Player player) {
        if (player == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isMidnightOrchid(stack)) {
                return true;
            }
        }
        return false;
    }
}
