package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.quests.data.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import java.util.List;

public class OfficeErrandsQuest extends Quest implements QuestScript {

    private static List<QuestObjective> createObjectives() {
        World world = Bukkit.getWorld("redrocks");
        Location beacon = world != null ? new Location(world, 29.5, 142, -92.5) : null;
        return java.util.List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc516", 1, beacon)
        );
    }

    public OfficeErrandsQuest() {
        super(
                "officeerrands",
                "Office Errands",
                "Help around the office.",
                createObjectives(),
                1,
                java.util.List.of(),
                null,
                null,
                null,
                java.util.List.of()
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        World world = Bukkit.getWorld("redrocks");
        if (world != null) {
            player.teleport(new Location(world, 19, 142, -47));
        }

        QuestGateManager gates = plugin.getQuestGateManager();
        String gateId = "office_elevator";

        // Apply blindness and close the elevator gate
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 0, false, false, false));
        gates.closeGate(player, gateId);

        // After blindness wears off, send initial dialog line
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(ChatColor.GRAY + "[1/1] " + player.getName() + ChatColor.WHITE + ": Lights are off... looks like everyone’s gone. Guess that’s my cue.");
        }, 40L);

        // Listen for talking to the Janitor (NPC 516)
        final Listener[] talkListener = new Listener[1];
        final int[] idx = {0};
        final boolean[] dialogDone = {false};
        final String[] lines = new String[] {
                "Ilta|Took your time.",
                "<player>|Didn't realize how late it was.",
                "Ilta|Time’s slippery in places like this.",
                "Ilta|One minute you’re working late… next minute, the building’s watching to see if you’ll notice it’s not quite the same as you left it.",
                "<player>|What’s that supposed to mean?",
                "Ilta|It means you're not leaving the same way you came in."
        };

        talkListener[0] = new Listener() {
            @org.bukkit.event.EventHandler
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (npc.getId() != 516) return;

                if (idx[0] >= lines.length) {
                    event.setCancelled(true);
                    return;
                }

                String[] parts = lines[idx[0]].split("\\|", 2);
                String speaker = parts[0].equals("<player>") ? player.getName() : parts[0];
                String msg = parts[1];
                player.sendMessage(ChatColor.GRAY + "[" + (idx[0] + 1) + "/" + lines.length + "] " + ChatColor.YELLOW + speaker + ChatColor.WHITE + ": " + msg);
                idx[0]++;

                if (idx[0] >= lines.length) {
                    dialogDone[0] = true;
                    gates.openGate(player, gateId);
                    HandlerList.unregisterAll(talkListener[0]);
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(talkListener[0], plugin);

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
                    HandlerList.unregisterAll(this);

                    gates.closeGate(player, gateId); // close immediately

                    World rWorld = Bukkit.getWorld("redrocks");
                    if (rWorld == null) return;
                    org.bukkit.Location lampLoc = new org.bukkit.Location(rWorld, 29, -148, -93);

                    var fbm = plugin.getFakeBlockManager();
                    org.bukkit.block.data.type.RedstoneLamp off = (org.bukkit.block.data.type.RedstoneLamp) org.bukkit.Material.REDSTONE_LAMP.createBlockData();
                    off.setLit(false);
                    org.bukkit.block.data.type.RedstoneLamp on = (org.bukkit.block.data.type.RedstoneLamp) org.bukkit.Material.REDSTONE_LAMP.createBlockData();
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

                                player.sendMessage(ChatColor.GRAY + "[1/1] " + player.getName() + ChatColor.WHITE + ": Huh that's weird, the elevator light's flickering.");

                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    org.bukkit.Location cur = player.getLocation();

                                    // Compute offset inside elevator region
                                    double offX = cur.getX() - minX;
                                    double offY = cur.getY() - 142;
                                    double offZ = cur.getZ() - minZ;

                                    int newMinX = 4249;
                                    int newMinY = -33;
                                    int newMinZ = -1212;

                                    org.bukkit.World destWorld = Bukkit.getWorld("flatland");
                                    if (destWorld != null) {
                                        org.bukkit.Location dest = new org.bukkit.Location(destWorld, newMinX + offX, newMinY + offY, newMinZ + offZ, cur.getYaw(), cur.getPitch());
                                        player.teleport(dest);
                                        plugin.getQuestManager().handleTalk(player, "npc516");
                                    }
                                }, 40L);
                            }
                        }
                    }.runTaskTimer(plugin, 0L, 10L);
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(moveListener, plugin);
    }
}
