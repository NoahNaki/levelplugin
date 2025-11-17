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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

/**
 * Quest that has players design their first dungeon for Zoya.
 */
public class ZoyaDungeonQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {

    private static final String QUEST_ID = "zoyadungeon";
    private static final int NPC_ID = 1113;
    private static final String PORTAL_WORLD = "world";
    private static final int PORTAL_MIN_X = 642;
    private static final int PORTAL_MAX_X = 643;
    private static final int PORTAL_MIN_Y = 41;
    private static final int PORTAL_MAX_Y = 46;
    private static final int PORTAL_MIN_Z = -234;
    private static final int PORTAL_MAX_Z = -228;
    private static final int PORTAL_TRIGGER_X = 641;
    private static final int PORTAL_TRIGGER_Y = 40;
    private static final int PORTAL_TRIGGER_Z = -231;
    private static final int PORTAL_TRIGGER_RADIUS = 10;

    private static ZoyaDungeonQuest instance;
    private static boolean portalWatcherRegistered;

    private static final List<String> REMINDER_DIALOG = List.of(
            "Zoya|That shimmering portal beside me links straight to the dungeon creator.",
            "Zoya|Sketch something bold, save it, and the portal will return you to me at the plaza."
    );

    private static final List<String> COMPLETION_DIALOG = List.of(
            "Zoya|I see you've successfully created your first dungeon.",
            "Zoya|As more challengers attempt it, you'll earn rewards based on its popularity.",
            "Zoya|I wish you the best of luck, my new colleague." 
    );

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld(PORTAL_WORLD);
        double beaconX = (PORTAL_MIN_X + PORTAL_MAX_X) / 2.0 + 0.5;
        Location beaconLoc = world == null ? null : new Location(world, beaconX, 43, PORTAL_MIN_Z + 2);
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID, 1, BeaconTargets.npc(NPC_ID)),
                new QuestObjective(QuestObjectiveType.DUNGEON_CREATE, "ANY", 1,
                        beaconLoc == null ? null : BeaconTargets.staticLoc(beaconLoc)),
                new QuestObjective(QuestObjectiveType.TALK, "npc" + NPC_ID + "_return", 1, BeaconTargets.npc(NPC_ID))
        );
    }

    public ZoyaDungeonQuest() {
        super(
                QUEST_ID,
                "Blueprints of Legacy",
                "Help Zoya craft a legacy by creating and saving your first dungeon.",
                createObjectives(),
                1,
                List.of("serashelp"),
                null,
                QuestRewardCompat.create(1000, 500, 0, List.of()),
                NPC_ID,
                List.of(
                        "Zoya|Ah, so you're the adventurer everyone's been talking about. Now I see what they meant.",
                        "Zoya|The name is Zoya, and it's a pleasure to finally meet you.",
                        "Zoya|Say <player>, it seems like you've got quite the talent for clearing dungeons. Perhaps they're getting a little too easy?",
                        "Zoya|Have you ever considered creating your own challenge?",
                        "Zoya|Anyone can become a dungeon master here—the craft is in the quality of what you build.",
                        "Zoya|Head through that portal beside us. Once you're inside, /dungeon create will open so you can begin.",
                        "Zoya|Save your design when you're satisfied and return to me so we can see how the crowd likes it."
                ),
                false
        );
        instance = this;
        ensurePortalWatcher(Main.getInstance());
    }

    public static List<String> getReminderDialog() {
        return REMINDER_DIALOG;
    }

    public static List<String> getCompletionDialog() {
        return COMPLETION_DIALOG;
    }

    @Override
    public void onStart(Player player, Main plugin) {
        ensurePortalWatcher(plugin);
        Bukkit.getScheduler().runTask(plugin, () -> tryOpenCreator(player, plugin));
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        // Portal access remains available even after completion.
    }

    @Override
    public void onReset(Player player, Main plugin) {
        // Nothing additional.
    }

    private static void ensurePortalWatcher(Main plugin) {
        if (portalWatcherRegistered || plugin == null) {
            return;
        }
        Listener listener = new Listener() {
            @EventHandler
            public void onMove(PlayerMoveEvent event) {
                if (instance == null) {
                    return;
                }
                Player player = event.getPlayer();
                if (!instance.canUseCreatorPortal(player, plugin)) {
                    return;
                }
                if (instance.shouldTriggerPortal(event.getFrom(), event.getTo())) {
                    instance.openCreator(player, plugin);
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        portalWatcherRegistered = true;
    }

    private void tryOpenCreator(Player player, Main plugin) {
        if (!player.isOnline()) {
            return;
        }
        if (canUseCreatorPortal(player, plugin)
                && (isInsidePortal(player.getLocation()) || isNetherPortalTrigger(player.getLocation()))) {
            openCreator(player, plugin);
        }
    }

    private boolean canUseCreatorPortal(Player player, Main plugin) {
        PlayerQuestProgress progress = plugin.getQuestManager().getProgress(player.getUniqueId(), QUEST_ID);
        if (progress != null) {
            boolean introDone = progress.getProgress(0) >= getObjectives().get(0).getAmount();
            if (introDone) {
                return true;
            }
        }
        return plugin.getQuestManager().hasCompleted(player.getUniqueId(), QUEST_ID);
    }

    private void openCreator(Player player, Main plugin) {
        if (player == null || !player.isOnline()) {
            return;
        }
        me.nakilex.levelplugin.dungeon.DungeonManager manager = plugin.getDungeonManager();
        if (manager == null) {
            player.sendMessage(ChatColor.RED + "The dungeon manager is currently unavailable.");
            return;
        }
        me.nakilex.levelplugin.dungeon.DungeonBuilder builder = manager.getBuilder();
        if (builder == null) {
            player.sendMessage(ChatColor.RED + "The dungeon builder is currently unavailable.");
            return;
        }
        if (builder.isBuilding(player)) {
            return;
        }
        Location returnLocation = createReturnLocation();
        if (returnLocation == null) {
            player.sendMessage(ChatColor.RED + "The return location could not be prepared. Try again later.");
            return;
        }
        if (returnLocation.getWorld() != null) {
            returnLocation.getWorld().getChunkAt(returnLocation).load();
        }
        builder.setNextReturnLocation(player, returnLocation);
        boolean executed = player.performCommand("dungeon create");
        if (!executed) {
            builder.start(player, returnLocation);
        }
        player.sendMessage(ChatColor.AQUA + "Zoya's portal flares and /dungeon create opens before you.");
    }

    private boolean isInsidePortal(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(PORTAL_WORLD)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= PORTAL_MIN_X && x <= PORTAL_MAX_X
                && y >= PORTAL_MIN_Y && y <= PORTAL_MAX_Y
                && z >= PORTAL_MIN_Z && z <= PORTAL_MAX_Z;
    }

    private boolean shouldTriggerPortal(Location from, Location to) {
        if (to == null) {
            return false;
        }
        boolean enteringCuboid = isInsidePortal(to) && !isInsidePortal(from);
        boolean enteringPortalBlock = isNetherPortalTrigger(to) && !isNetherPortalTrigger(from);
        return enteringCuboid || enteringPortalBlock;
    }

    private boolean isNetherPortalTrigger(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(PORTAL_WORLD)) {
            return false;
        }
        if (location.getBlock().getType() != Material.NETHER_PORTAL) {
            return false;
        }
        double centerX = location.getBlockX() + 0.5;
        double centerY = location.getBlockY() + 0.5;
        double centerZ = location.getBlockZ() + 0.5;
        double dx = centerX - PORTAL_TRIGGER_X;
        double dy = centerY - PORTAL_TRIGGER_Y;
        double dz = centerZ - PORTAL_TRIGGER_Z;
        return (dx * dx + dy * dy + dz * dz) <= PORTAL_TRIGGER_RADIUS * PORTAL_TRIGGER_RADIUS;
    }

    private Location createReturnLocation() {
        World world = Bukkit.getWorld(PORTAL_WORLD);
        if (world == null) {
            return null;
        }
        return new Location(world, 638, 40, -231);
    }
}
