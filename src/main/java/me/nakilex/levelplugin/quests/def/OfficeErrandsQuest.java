package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.fakeblock.FakeBlockManager;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;

import java.util.List;

public class OfficeErrandsQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {
    public static final String ID = "officeerrands";

    /** Per-player listeners for cleanup when the quest resets. */
    private final java.util.Map<java.util.UUID, java.util.List<Listener>> listeners = new java.util.HashMap<>();

    private static final String ELEVATOR_MUSIC_SOUND = "nexo:music.elevatormusic";
    private static final String ELEVATOR_ARRIVAL_SOUND = "nexo:music.elevatording";
    private static final String ELEVATOR_TARGET = "officeerrands_elevator";

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("redrocks");
        Location elevatorLoc = world == null ? null : new Location(world, 29.0, 142.0, -93.0);
        return java.util.List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc516", 1, BeaconTargets.npc(516)),
                new QuestObjective(QuestObjectiveType.DISCOVER, ELEVATOR_TARGET, 1,
                        false,
                        BeaconTargets.staticLoc(elevatorLoc),
                        "Enter the elevator.")
        );
    }

    /** Register a listener for this player so it can be cleaned up. */
    private void register(Player player, Listener listener, Main plugin) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        listeners.computeIfAbsent(player.getUniqueId(), k -> new java.util.ArrayList<>()).add(listener);
    }

    public OfficeErrandsQuest() {
        super(
                ID,
                "Office Errands",
                "Help around the office.",
                createObjectives(),
                1,
                java.util.List.of(),
                null,
                null,
                null,
                java.util.List.of(),
                true,
                false,
                true
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        if (plugin.getPlayerVisibilityManager() != null) {
            plugin.getPlayerVisibilityManager().updatePlayer(player);
        }
        World world = Bukkit.getWorld("redrocks");
        if (world == null) {
            // Lazily load the quest world if it isn't already present
            plugin.getWorldManager().ensureWorldsLoaded("redrocks");
            world = Bukkit.getWorld("redrocks");
        }
        if (world != null) {
            player.teleport(new Location(world, 19, 142, -47));
        }

        QuestGateManager gates = plugin.getQuestGateManager();
        String gateId = "office_elevator";
        String worldGateId = "world_elevator";
        String roomGateId = "world_elevator_room";

        // Apply blindness while the world loads
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 0, false, false, false));

        // Close the office elevator once the player should have the world loaded
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> gates.closeGate(player, gateId),
                40L);

        // Close the destination elevator and its interior gate since they
        // default to open for all players
        gates.closeGateInstant(player, worldGateId);
        gates.closeGateInstant(player, roomGateId);

        // After blindness wears off, send initial dialog line
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ChatFormatter.constructDivider(player, " ", 45);
            player.sendMessage(ChatColor.DARK_GRAY + "[1/1] " + ChatColor.YELLOW + player.getName()
                    + ChatColor.WHITE + ": Lights are off... looks like everyone’s gone. Guess that’s my cue.");
            ChatFormatter.constructDivider(player, " ", 45);
        }, 40L);

        // Listen for talking to the Janitor (NPC 516)
        final boolean[] dialogDone = {false};
        final List<String> lines = java.util.List.of(
                "Ilta|Took your time.",
                "<player>|Didn't realize how late it was.",
                "Ilta|Time’s slippery in places like this.",
                "Ilta|One minute you’re working late… next minute, the building’s watching to see if you’ll notice it’s not quite the same as you left it.",
                "<player>|What’s that supposed to mean?",
                "Ilta|It means you're not leaving the same way you came in."
        );

        Listener talkListener = new Listener() {
            @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!NpcApi.getRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = NpcApi.getRegistry().getNPC(event.getRightClicked());
                if (npc.getId() != 516) return;

                if (dialogDone[0]) return;

                if (plugin.getDialogManager().hasSession(player)) {
                    NPC sessionNpc = plugin.getDialogManager().getSessionNpc(player);
                    if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                        event.setCancelled(true);
                        plugin.getDialogManager().advanceDialog(player, plugin.getQuestManager());
                        return;
                    }
                }

                event.setCancelled(true);
                plugin.getDialogManager().startDialog(player, lines, npc, () -> {
                    dialogDone[0] = true;
                    gates.openGate(player, gateId);
                    plugin.getQuestManager().handleTalk(player, "npc516");

                    // If the player is already inside the elevator area when
                    // the dialog finishes, trigger the teleport sequence
                    Location loc = player.getLocation();
                    if (loc.getBlockX() >= 27 && loc.getBlockX() <= 31
                            && loc.getBlockZ() >= -95 && loc.getBlockZ() <= -90) {
                        plugin.getQuestManager().handleDiscover(player, ELEVATOR_TARGET);
                        startElevatorTeleport(player, loc, plugin, gates, gateId, worldGateId, roomGateId);
                    }
                });
            }
        };
        register(player, talkListener, plugin);

        // After speaking with the Janitor, detect when the player enters the elevator
        Listener moveListener = new Listener() {
            private boolean ready = false;

            @org.bukkit.event.EventHandler
            public void onMove(PlayerMoveEvent e) {
                if (!e.getPlayer().equals(player)) return;
                if (!ready) {
                    if (dialogDone[0]) {
                        ready = true;
                    } else {
                        return;
                    }
                }

                int minX = 27, maxX = 31, minZ = -95, maxZ = -90; // elevator bounds
                org.bukkit.Location to = e.getTo();
                if (to.getBlockX() >= minX && to.getBlockX() <= maxX && to.getBlockZ() >= minZ && to.getBlockZ() <= maxZ) {
                    plugin.getQuestManager().handleDiscover(player, ELEVATOR_TARGET);
                    HandlerList.unregisterAll(this);
                    startElevatorTeleport(player, to, plugin, gates, gateId, worldGateId, roomGateId);
                }
            }
        };
        register(player, moveListener, plugin);
    }

    /** Begin the elevator teleport sequence when the player steps inside. */
    private void startElevatorTeleport(Player player, Location triggerLoc,
                                       Main plugin, QuestGateManager gates,
                                       String gateId, String worldGateId,
                                       String roomGateId) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            gates.closeGate(player, gateId);

            Location musicLoc = player.getLocation();
            plugin.getLogger().info("[OfficeErrands] Starting elevator music for " + player.getName()
                    + " at " + musicLoc);
            player.playSound(musicLoc, ELEVATOR_MUSIC_SOUND, SoundCategory.MUSIC, 1f, 1f);

            World rWorld = Bukkit.getWorld("redrocks");
            if (rWorld == null) return;
            Location lampLoc = new Location(rWorld, 29, 148, -94);

            FakeBlockManager fbm = plugin.getFakeBlockManager();
            org.bukkit.block.data.Lightable off = (org.bukkit.block.data.Lightable) org.bukkit.Material.REDSTONE_LAMP.createBlockData();
            off.setLit(false);
            org.bukkit.block.data.Lightable on = (org.bukkit.block.data.Lightable) org.bukkit.Material.REDSTONE_LAMP.createBlockData();
            on.setLit(true);

            new org.bukkit.scheduler.BukkitRunnable() {
                int ticks = 0;
                boolean lit = false;

                @Override
                public void run() {
                    lit = !lit;
                    fbm.showFakeBlock(player, lampLoc, lit ? on : off);
                    ticks += 10;
                    if (ticks >= 200) {
                        cancel();
                        fbm.hideFakeBlock(player, lampLoc);

                        ChatFormatter.constructDivider(player, " ", 45);
                        player.sendMessage(ChatColor.DARK_GRAY + "[1/1] "
                                + ChatColor.YELLOW + player.getName()
                                + ChatColor.WHITE
                                + ": Huh that's weird, the elevator light's flickering.");
                        ChatFormatter.constructDivider(player, " ", 45);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            Location cur = player.getLocation();

                            Location originMin = new Location(triggerLoc.getWorld(), 27, 142, -95);
                            Location destMin = new Location(Bukkit.getWorld("world"), 102, 68, -97);
                            World destWorld = destMin.getWorld();

                            if (destWorld != null) {
                                destWorld.getChunkAt(destMin).load();
                                gates.closeGateInstant(player, roomGateId);
                                Location dest = destMin.clone().add(
                                        cur.getX() - originMin.getX(),
                                        cur.getY() - originMin.getY(),
                                        cur.getZ() - originMin.getZ());
                                dest.setYaw(cur.getYaw());
                                dest.setPitch(cur.getPitch());
                                plugin.getLogger().info("[OfficeErrands] Teleporting " + player.getName()
                                        + " to " + dest + " from " + cur);
                                player.teleport(dest);
                                plugin.getLogger().info("[OfficeErrands] Stopping elevator music for "
                                        + player.getName());
                                player.stopSound(ELEVATOR_MUSIC_SOUND, SoundCategory.MUSIC);
                                Location soundLoc = dest.clone();
                                Bukkit.getScheduler().runTaskLater(plugin,
                                    () -> {
                                        plugin.getLogger().info("[OfficeErrands] Playing elevator arrival sound for "
                                                + player.getName() + " in world "
                                                + player.getWorld().getName() + " at " + soundLoc);
                                        player.playSound(soundLoc, ELEVATOR_ARRIVAL_SOUND, SoundCategory.MASTER, 1f, 1f);
                                    }, 10L);
                                plugin.getQuestManager().startQuest(player, "newbeginning");

                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    // Delay closing until the destination chunks are fully
                                    // loaded client-side so the gates appear instead of
                                    // vanishing. The world gate then opens with its
                                    // elevator animation a short time later.
                                    gates.closeGateInstant(player, roomGateId);
                                    gates.closeGateInstant(player, worldGateId);
                                    Bukkit.getScheduler().runTaskLater(plugin,
                                            () -> gates.openGate(player, worldGateId), 40L);
                                }, 10L);

                                Listener exitListener = new Listener() {
                                    @org.bukkit.event.EventHandler
                                    public void onMove(PlayerMoveEvent ev) {
                                        if (!ev.getPlayer().equals(player)) return;
                                        Location l = ev.getTo();
                                        if (!l.getWorld().equals(destWorld)
                                                || l.getBlockX() < 101 || l.getBlockX() > 107
                                                || l.getBlockY() < 66 || l.getBlockY() > 76
                                                || l.getBlockZ() < -99 || l.getBlockZ() > -92) {
                                            HandlerList.unregisterAll(this);
                                            gates.openGate(player, roomGateId);
                                            plugin.getQuestManager().cleanupQuest(player, "officeerrands");
                                        }
                                    }
                                };
                                register(player, exitListener, plugin);
                            }
                        }, 40L);
                    }
                }
            }.runTaskTimer(plugin, 0L, 10L);
        }, 40L);
    }

    @Override
    public void onReset(Player player, Main plugin) {
        plugin.getDialogManager().resetDialog(player);
        java.util.List<Listener> list = listeners.remove(player.getUniqueId());
        if (list != null) {
            for (Listener l : list) {
                HandlerList.unregisterAll(l);
            }
        }
    }
    @Override
    public void onComplete(Player player, Main plugin) {
        if (plugin.getPlayerVisibilityManager() != null) {
            plugin.getPlayerVisibilityManager().updatePlayer(player);
        }
    }
}
