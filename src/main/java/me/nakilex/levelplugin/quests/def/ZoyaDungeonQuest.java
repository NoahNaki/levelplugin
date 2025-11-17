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
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quest that has players design their first dungeon for Zoya.
 */
public class ZoyaDungeonQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {

    private static final String QUEST_ID = "zoyadungeon";
    private static final int NPC_ID = 1113;
    private static final String PORTAL_WORLD = "world";
    private static final int PORTAL_X = 643;
    private static final int PORTAL_MIN_Y = 40;
    private static final int PORTAL_MAX_Y = 46;
    private static final int PORTAL_MIN_Z = -233;
    private static final int PORTAL_MAX_Z = -229;

    private static final List<String> REMINDER_DIALOG = List.of(
            "Zoya|That shimmering portal beside me links straight to the dungeon creator.",
            "Zoya|Step inside between x=643 and z=-233 to -229 and I'll have /dungeon create ready for you.",
            "Zoya|Sketch something bold, save it, and the portal will return you to me at the plaza." 
    );

    private static final List<String> COMPLETION_DIALOG = List.of(
            "Zoya|I see you've successfully created your first dungeon.",
            "Zoya|As more challengers attempt it, you'll earn rewards based on its popularity.",
            "Zoya|I wish you the best of luck, my new colleague." 
    );

    private final Map<UUID, List<Listener>> listeners = new HashMap<>();

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld(PORTAL_WORLD);
        Location beaconLoc = world == null ? null : new Location(world, PORTAL_X + 0.5, 43, PORTAL_MIN_Z + 2);
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
    }

    public static List<String> getReminderDialog() {
        return REMINDER_DIALOG;
    }

    public static List<String> getCompletionDialog() {
        return COMPLETION_DIALOG;
    }

    @Override
    public void onStart(Player player, Main plugin) {
        registerPortalWatcher(player, plugin);
        Bukkit.getScheduler().runTask(plugin, () -> tryOpenCreator(player, plugin));
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        cleanup(player);
    }

    @Override
    public void onReset(Player player, Main plugin) {
        cleanup(player);
    }

    private void registerPortalWatcher(Player player, Main plugin) {
        UUID playerId = player.getUniqueId();
        Listener listener = new Listener() {
            private boolean wasInside = isInsidePortal(player.getLocation());

            @EventHandler
            public void onMove(PlayerMoveEvent event) {
                if (!event.getPlayer().getUniqueId().equals(playerId)) {
                    return;
                }
                Player mover = event.getPlayer();
                if (!isCreatorObjectiveActive(mover, plugin)) {
                    return;
                }
                boolean inside = isInsidePortal(event.getTo());
                if (inside && !wasInside) {
                    openCreator(mover, plugin);
                }
                wasInside = inside;
            }
        };
        register(playerId, listener, plugin);
    }

    private void tryOpenCreator(Player player, Main plugin) {
        if (!player.isOnline()) {
            return;
        }
        if (isCreatorObjectiveActive(player, plugin) && isInsidePortal(player.getLocation())) {
            openCreator(player, plugin);
        }
    }

    private boolean isCreatorObjectiveActive(Player player, Main plugin) {
        PlayerQuestProgress progress = plugin.getQuestManager().getProgress(player.getUniqueId(), QUEST_ID);
        if (progress == null) {
            return false;
        }
        boolean introDone = progress.getProgress(0) >= getObjectives().get(0).getAmount();
        boolean dungeonSaved = progress.getProgress(1) >= getObjectives().get(1).getAmount();
        return introDone && !dungeonSaved;
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
        builder.start(player, returnLocation);
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
        return x == PORTAL_X && y >= PORTAL_MIN_Y && y <= PORTAL_MAX_Y
                && z >= PORTAL_MIN_Z && z <= PORTAL_MAX_Z;
    }

    private Location createReturnLocation() {
        World world = Bukkit.getWorld(PORTAL_WORLD);
        if (world == null) {
            return null;
        }
        return new Location(world, 638.5, 40, -231.5);
    }

    private void register(UUID playerId, Listener listener, Main plugin) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        listeners.computeIfAbsent(playerId, k -> new ArrayList<>()).add(listener);
    }

    private void cleanup(Player player) {
        List<Listener> regs = listeners.remove(player.getUniqueId());
        if (regs != null) {
            for (Listener listener : regs) {
                HandlerList.unregisterAll(listener);
            }
        }
    }
}
