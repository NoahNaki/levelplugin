package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.data.QuestResetScript;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.UUID;

/**
 * Investigation quest that sends players to the abandoned castle and into the Crimson Reliquary.
 */
public class AbandonedCastleQuest extends Quest implements QuestScript, QuestResetScript {
    public static final String ID = "abandonedcastle";
    public static final String NPC_NAME = "Cedric";
    public static final int NPC_ID = 1650;

    private static final String WORLD_NAME = "world";
    private static final double CASTLE_X = 100.5;
    private static final double CASTLE_Y = 100;
    private static final double CASTLE_Z = 100.5;
    private static final double CASTLE_RADIUS = 10.0;
    private static final String CRIMSON_KEY = DungeonManager.normalizeKey("Crimson Reliquary");

    private static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    private static final String RETURN_TARGET = "npc" + NPC_ID + "_return";
    private static final String APPROACH_TARGET = "abandoned_castle_approach";
    private static final String ENTER_TARGET = "abandoned_castle_enter";
    private static final String CLEAR_TARGET = "abandoned_castle_clear";

    private static final int APPROACH_INDEX = 1;
    private static final int ENTER_INDEX = 2;
    private static final int CLEAR_INDEX = 3;

    private static final String NEAR_FLAG = "castle_near";

    private static boolean listenersRegistered;

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld(WORLD_NAME);
        Location beaconLoc = world == null ? null : new Location(world, CASTLE_X, CASTLE_Y, CASTLE_Z);
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1, BeaconTargets.npc(NPC_ID),
                        "Hear Cedric's warning"),
                new QuestObjective(QuestObjectiveType.EXPLORE, APPROACH_TARGET, 1, false,
                        beaconLoc == null ? null : BeaconTargets.staticLoc(beaconLoc),
                        "Head to the Abandoned Castle"),
                new QuestObjective(QuestObjectiveType.EXPLORE, ENTER_TARGET, 1, false,
                        beaconLoc == null ? null : BeaconTargets.staticLoc(beaconLoc),
                        "Enter the Crimson Reliquary dungeon"),
                new QuestObjective(QuestObjectiveType.DUNGEON_COMPLETE, CRIMSON_KEY, 1, false, null,
                        "Clear the Crimson Reliquary"),
                new QuestObjective(QuestObjectiveType.TALK, RETURN_TARGET, 1, BeaconTargets.npc(NPC_ID),
                        "Report back to Cedric")
        );
    }

    public AbandonedCastleQuest() {
        super(
                ID,
                "Abandoned Castle",
                "Investigate the disappearances near the abandoned castle and delve into the Crimson Reliquary.",
                createObjectives(),
                20,
                List.of(),
                null,
                QuestRewardCompat.create(15000, 5600, 0, List.of()),
                NPC_ID,
                List.of(
                        "Cedric|Rumors of some adventurers disappearing once they get near an abandoned castle have been spreading.",
                        "Cedric|Go check out what could be causing the disturbance at §8[§e100, 100, 100§8]§f.",
                        "Cedric|If you find your way into the Crimson Reliquary inside, make sure you come back in one piece."
                ),
                false,
                true,
                true
        );
        ensureListeners(Main.getInstance());
    }

    public static void registerTalkTargets(QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, NPC_NAME);
        questManager.registerTalkTarget(RETURN_TARGET, NPC_NAME, NPC_NAME);
    }

    @Override
    public void onStart(Player player, Main plugin) {
        ensureListeners(plugin);
    }

    @Override
    public void onReset(Player player, Main plugin) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null || player == null) {
            return;
        }
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        if (progress != null) {
            progress.setProgress(APPROACH_INDEX, 0);
            progress.setProgress(ENTER_INDEX, 0);
            questManager.saveProgress();
        }
        questManager.removeFlag(player.getUniqueId(), ID, NEAR_FLAG);
    }

    private static void ensureListeners(Main plugin) {
        if (listenersRegistered || plugin == null) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onMove(PlayerMoveEvent event) {
                if (event.getTo() != null) {
                    refreshObjectiveState(event.getPlayer(), event.getTo());
                }
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                refreshObjectiveState(event.getPlayer(), event.getPlayer().getLocation());
            }
        }, plugin);
        listenersRegistered = true;
    }

    private static void refreshObjectiveState(Player player, Location to) {
        if (player == null || to == null) {
            return;
        }

        Main plugin = Main.getInstance();
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }
        Quest quest = questManager.getQuestById(ID);
        if (quest == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PlayerQuestProgress progress = questManager.getProgress(uuid, ID);
        if (progress == null) {
            return;
        }

        boolean questComplete = questManager.hasCompleted(uuid, ID);
        if (questComplete) {
            return;
        }

        boolean clearedReliquary = progress.getProgress(CLEAR_INDEX) >= quest.getObjectives().get(CLEAR_INDEX).getAmount();

        boolean nearCastle = isNearCastle(to);
        boolean insideCrimson = isInCrimsonReliquary(to);

        if (insideCrimson) {
            if (progress.getProgress(APPROACH_INDEX) < 1) {
                progress.setProgress(APPROACH_INDEX, 1);
            }
            questManager.handleExplore(player, ENTER_TARGET);
        } else if (nearCastle) {
            questManager.handleExplore(player, APPROACH_TARGET);
        } else if (!clearedReliquary) {
            boolean entered = progress.getProgress(ENTER_INDEX) >= 1;
            if (!entered && progress.getProgress(APPROACH_INDEX) > 0) {
                progress.setProgress(APPROACH_INDEX, 0);
                progress.setProgress(ENTER_INDEX, 0);
                questManager.saveProgress();
            }
        }

        if (nearCastle || insideCrimson) {
            questManager.setFlag(uuid, ID, NEAR_FLAG);
        } else {
            questManager.removeFlag(uuid, ID, NEAR_FLAG);
        }
    }

    private static boolean isNearCastle(Location to) {
        World world = to.getWorld();
        if (world == null || !WORLD_NAME.equalsIgnoreCase(world.getName())) {
            return false;
        }
        double dx = to.getX() - CASTLE_X;
        double dy = to.getY() - CASTLE_Y;
        double dz = to.getZ() - CASTLE_Z;
        return (dx * dx + dy * dy + dz * dz) <= CASTLE_RADIUS * CASTLE_RADIUS;
    }

    private static boolean isInCrimsonReliquary(Location to) {
        World world = to.getWorld();
        if (world == null) {
            return false;
        }
        DungeonManager dungeonManager = Main.getInstance().getDungeonManager();
        if (dungeonManager == null) {
            return false;
        }
        String layout = dungeonManager.getInstanceLayout(world);
        return CRIMSON_KEY.equalsIgnoreCase(layout);
    }
}
