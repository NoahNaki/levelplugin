package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.quests.data.*;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;

import java.util.List;

/**
 * Intro quest that plays a short conversation with Piwan.
 */
public class NewBeginningQuest extends Quest implements QuestScript, QuestCompletionScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc536_done", 1),
                new QuestObjective(QuestObjectiveType.SELECT_CLASS, "ANY", 1),
                new QuestObjective(QuestObjectiveType.BUY, "class_weapon", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc537", 1)
        );
    }

    private final java.util.Set<java.util.UUID> awaitingMerchant = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> soldClothes = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> givenCoins = new java.util.HashSet<>();

    public NewBeginningQuest() {
        super(
                "newbeginning",
                "A New Beginning",
                "Meet Piwan after arriving in the new world.",
                createObjectives(),
                1,
                List.of("officeerrands"),
                null,
                QuestRewardCompat.create(150, 30, 0, List.of(),
                        List.of(PlayerClass.ARCHER, PlayerClass.WARRIOR,
                                PlayerClass.MAGE, PlayerClass.ROGUE)),
                null,
                null
        );
    }

    @Override
    public void onStart(Player player, Main plugin) {
        new BukkitRunnable() {
            boolean triggered = false;
            @Override
            public void run() {
                NPC npc = CitizensAPI.getNPCRegistry().getById(536);
                if (npc == null || !npc.isSpawned()) return;
                if (!player.isOnline()) { cancel(); return; }
                if (!npc.getEntity().getWorld().equals(player.getWorld())) return;
                if (!triggered && player.getLocation().distanceSquared(npc.getEntity().getLocation()) <= 100) {
                    playDialog(player, plugin, npc);
                    triggered = true;
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Listen for interactions with the Starter Merchant
        Listener merchantListener = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (!ChatColor.stripColor(npc.getName()).equalsIgnoreCase("Starter Merchant")) return;

                event.setCancelled(true);
                if (awaitingMerchant.contains(player.getUniqueId())) return;
                awaitingMerchant.add(player.getUniqueId());

                player.sendMessage(ChatColor.YELLOW + "Starter Merchant" + ChatColor.WHITE +
                        ": I'm sorry I can't sell you any equipment if you don't have any money, " +
                        "but those clothes you're wearing, I could certainly buy that off you in-exchange for some coins, whaddya say?");

                Main.getInstance().getDialogManager().startChoiceDialog(player, npc,
                        java.util.List.of("Yes", "No"), choice -> {
                            awaitingMerchant.remove(player.getUniqueId());
                            if (choice == 0) {
                                plugin.getEconomyManager().addCoins(player, 200);
                                soldClothes.add(player.getUniqueId());
                                player.sendMessage(ChatColor.GREEN + "You received 200 coins.");
                                player.performCommand("merchant starter_shop");
                            } else {
                                player.sendMessage(ChatColor.YELLOW + "Starter Merchant: Fair enough, have a nice day.");
                            }
                        });
            }
        };

        Bukkit.getPluginManager().registerEvents(merchantListener, plugin);
    }

    private void playDialog(Player player, Main plugin, NPC npc) {
        String[] lines = new String[] {
                "Hey you there! I could've sworn no one was standing there a second ago, how did you suddenly appear?",
                "You certainly don't look from around here, especially with those clothes, perhaps a noble from another country.",
                "Another world you say? Well you wouldn't be the first to make such bold claims, my mom said she once knew someone that claimed the same thing, said they were from a place called, \"ip\".",
                "I'm sure you have many questions, how about to start off I show you around my village.",
                "First things first, you're going to have to look like you're from this world, go talk to that merchant over there and buy some equipment.",
        };

        // Send the first line immediately when close with numbering
        player.sendMessage(ChatColor.GRAY + "[1/" + lines.length + "] "
                + ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[0]);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        final Listener[] listener = new Listener[1];
        listener[0] = new Listener() {
            int idx = 1;

            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC clicked = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (clicked.getId() != 536) return;
                event.setCancelled(true);

                if (idx >= lines.length) return;
                player.sendMessage(ChatColor.GRAY + "[" + (idx + 1) + "/" + lines.length + "] "
                        + ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[idx]);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                idx++;

                if (idx >= lines.length) {
                    org.bukkit.event.HandlerList.unregisterAll(listener[0]);
                    // Delay quest completion slightly so the final line can be read
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline()) return;
                        Main.getInstance().getQuestManager().handleTalk(player, "npc536_done");
                        moveNpc(player, npc, plugin);
                    }, 40L); // 2 seconds
                }
            }
        };
        org.bukkit.Bukkit.getPluginManager().registerEvents(listener[0], plugin);
    }

    /** Move Piwan to a new location for this player only. */
    private void moveNpc(Player player, NPC npc, Main plugin) {
        org.bukkit.Location loc = npc.getEntity().getLocation().clone().add(10, 0, 0);

        // Spawn the moved NPC with id 537 at the new location
        NPC moved = CitizensAPI.getNPCRegistry().getById(537);
        if (moved != null) {
            moved.spawn(loc);
            if (moved.isSpawned()) {
                moved.getEntity().teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                moved.getEntity().setGravity(false);
            }
            // Hide from everyone until the player walks away from NPC 536
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                p.hideEntity(plugin, moved.getEntity());
            }
        }

        // Show the moved NPC only after the player leaves the old one
        new BukkitRunnable() {
            boolean shown = false;
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (player.getLocation().distanceSquared(npc.getEntity().getLocation()) > 100) {
                    player.hideEntity(plugin, npc.getEntity());
                    if (moved != null && moved.isSpawned() && !shown) {
                        player.showEntity(plugin, moved.getEntity());
                        shown = true;
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Register follow-up dialog with the moved NPC
        registerFinalDialog(player, plugin);
    }

    /** Handle Piwan dialog after class selection and weapon purchase. */
    private void registerFinalDialog(Player player, Main plugin) {
        Listener[] handler = new Listener[1];
        handler[0] = new Listener() {
            int idx = 0;

            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (npc.getId() != 537) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(player.getUniqueId());
                if (prog == null || !prog.getQuest().getId().equals("newbeginning")) return;

                // Wait until the player has selected a class and bought the weapon
                if (prog.getProgress(1) < 1 || prog.getProgress(2) < 1) {
                    if (!givenCoins.contains(player.getUniqueId()) && !soldClothes.contains(player.getUniqueId())) {
                        plugin.getEconomyManager().addCoins(player, 100);
                        givenCoins.add(player.getUniqueId());
                        player.sendMessage(ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": Oh right, I should've realised you wouldn't have any currency belonging to this world, here, you can pay me back in the future.");
                    } else {
                        player.sendMessage(ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": Alright great now that you look like you belong here, now you just have to tell me what class you'll be going so that we can find you an appropriate weapon.");
                    }
                    event.setCancelled(true);
                    player.performCommand("class");
                    return;
                }

                event.setCancelled(true);

                PlayerClass pc = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
                String className = pc.name().substring(0, 1) + pc.name().substring(1).toLowerCase();
                String[] lines = new String[]{
                        "Ah you went with the " + className + ", I should have a spare weapon lying around here somewhere, let's see hmmmm",
                        "AH! here you go.",
                        "Now you're all set, I'm sure our paths will cross again adventurer, now go and explore the vast world of Eldrin."
                };

                if (idx >= lines.length) return;
                player.sendMessage(ChatColor.GRAY + "[" + (idx + 1) + "/" + lines.length + "] " +
                        ChatColor.YELLOW + npc.getName() + ChatColor.WHITE + ": " + lines[idx]);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                idx++;

                if (idx >= lines.length) {
                    HandlerList.unregisterAll(handler[0]);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getQuestManager().handleTalk(player, "npc537");
                        }
                    }, 20L);
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(handler[0], plugin);
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        // No additional completion logic
    }
}
