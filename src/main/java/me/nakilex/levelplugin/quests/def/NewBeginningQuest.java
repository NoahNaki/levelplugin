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
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;

import java.util.List;

/**
 * Intro quest that plays a short conversation with Piwan.
 */
public class NewBeginningQuest extends Quest implements QuestScript, QuestCompletionScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc536_done", 1),
                new QuestObjective(QuestObjectiveType.BUY, "starter_armor", 1),
                new QuestObjective(QuestObjectiveType.SELECT_CLASS, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc536_final", 1)
        );
    }

    private final java.util.Set<java.util.UUID> awaitingMerchant = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> soldClothes = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> givenCoins = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> readyToShop = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> merchantDone = new java.util.HashSet<>();

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
                536,
                List.of(
                        "Hey you there! I could've sworn no one was standing there a second ago, how did you suddenly appear?",
                        "You certainly don't look from around here, especially with those clothes, perhaps a noble from another country.",
                        "Another world you say? Well you wouldn't be the first to make such bold claims, my mom said she once knew someone that claimed the same thing, said they were from a place called, \"ip\".",
                        "I'm sure you have many questions, how about to start off I show you around my village.",
                        "First things first, you're going to have to look like you're from this world, go talk to that merchant over there and buy some equipment."
                )
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

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(player.getUniqueId());
                if (prog == null || !prog.getQuest().getId().equals("newbeginning")) return;
                if (prog.getProgress(0) < 1) return; // wait until Piwan dialog finished

                event.setCancelled(true);

                java.util.UUID id = player.getUniqueId();

                // If the player already has coins from Piwan, just open the shop
                if (givenCoins.contains(id) && !merchantDone.contains(id)) {
                    merchantDone.add(id);
                    player.performCommand("merchant starter_shop");
                    return;
                }

                // After the dialog has been shown once, always open the shop
                if (merchantDone.contains(player.getUniqueId())) {
                    player.performCommand("merchant starter_shop");
                    return;
                }

                // If the player already made a choice, the next click should open the shop
                if (readyToShop.remove(player.getUniqueId())) {
                    player.performCommand("merchant starter_shop");
                    return;
                }

                if (awaitingMerchant.contains(player.getUniqueId())) return;
                awaitingMerchant.add(player.getUniqueId());

                plugin.getDialogManager().startDialog(player,
                        java.util.List.of("Starter Merchant|I'm sorry I can't sell you any equipment if you don't have any money, " +
                                "but those clothes you're wearing, I could certainly buy that off you in-exchange for some coins, whaddya say?"),
                        npc,
                        () -> plugin.getDialogManager().startChoiceDialog(player, npc,
                                java.util.List.of("Yes", "No"), choice -> {
                                    awaitingMerchant.remove(player.getUniqueId());
                                    merchantDone.add(player.getUniqueId());
                                    if (choice == 0) {
                                        plugin.getEconomyManager().addCoins(player, 200);
                                        soldClothes.add(player.getUniqueId());
                                        player.sendMessage(ChatColor.GOLD + "You received " +
                                                ChatColor.YELLOW + "200 ⛃ " +
                                                ChatColor.GOLD + "coins.");
                                    } else {
                                        player.sendMessage(ChatColor.YELLOW + "Starter Merchant: Fair enough, have a nice day.");
                                    }
                                    readyToShop.add(player.getUniqueId());
                                }));
            }
        };

        Bukkit.getPluginManager().registerEvents(merchantListener, plugin);
    }

    private void playDialog(Player player, Main plugin, NPC npc) {
        java.util.List<String> lines = java.util.List.of(
                "Hey you there! I could've sworn no one was standing there a second ago, how did you suddenly appear?",
                "You certainly don't look from around here, especially with those clothes, perhaps a noble from another country.",
                "Another world you say? Well you wouldn't be the first to make such bold claims, my mom said she once knew someone that claimed the same thing, said they were from a place called, \"ip\".",
                "I'm sure you have many questions, how about to start off I show you around my village.",
                "First things first, you're going to have to look like you're from this world, go talk to that merchant over there and buy some equipment."
        );

        plugin.getDialogManager().startDialog(player, lines, npc, () -> {
            if (player.isOnline()) {
                Main.getInstance().getQuestManager().handleTalk(player, "npc536_done");
                registerFinalDialog(player, plugin);
            }
        });
    }

    /** Handle Piwan dialog after class selection and weapon purchase. */
    private void registerFinalDialog(Player player, Main plugin) {
        Listener handler = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().equals(player)) return;
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (npc.getId() != 536) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(player.getUniqueId());
                if (prog == null || !prog.getQuest().getId().equals("newbeginning")) return;

                event.setCancelled(true);

                if (plugin.getDialogManager().hasSession(player)) {
                    NPC sessionNpc = plugin.getDialogManager().getSessionNpc(player);
                    if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                        plugin.getDialogManager().advanceDialog(player, plugin.getQuestManager());
                    }
                    return;
                }

                if (prog.getProgress(1) < 1) {
                    if (!givenCoins.contains(player.getUniqueId()) && !soldClothes.contains(player.getUniqueId())) {
                        plugin.getDialogManager().startDialog(player,
                                java.util.List.of("Piwan|Oh right, I should've realised you wouldn't have any currency belonging to this world, here, you can pay me back in the future."),
                                npc,
                                () -> {
                                    plugin.getEconomyManager().addCoins(player, 100);
                                    givenCoins.add(player.getUniqueId());
                                    player.sendMessage(ChatColor.GOLD + "You received "
                                            + ChatColor.YELLOW + "100 ⛃ " + ChatColor.GOLD + "coins.");
                                });
                    } else {
                        plugin.getDialogManager().startDialog(player,
                                java.util.List.of("Piwan|Go ahead and buy some new equipment."),
                                npc,
                                null);
                    }
                    return;
                }

                if (prog.getProgress(2) < 1) {
                    plugin.getDialogManager().startDialog(player,
                            java.util.List.of("Piwan|Alright great now that you look like you belong here, now you just have to tell me what class you'll be going so that we can find you an appropriate weapon."),
                            npc,
                            () -> player.performCommand("class"));
                    return;
                }

                PlayerClass pc = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
                String className = pc.name().substring(0, 1) + pc.name().substring(1).toLowerCase();
                java.util.List<String> lines = java.util.List.of(
                        "Ah you went with the " + className + ", I should have a spare weapon lying around here somewhere, let's see hmmmm",
                        "AH! here you go.",
                        "Now you're all set, I'm sure our paths will cross again adventurer, now go and explore the vast world of Eldrin."
                );

                plugin.getDialogManager().startDialog(player, lines, npc, () -> {
                    giveClassWeapon(player);
                    plugin.getQuestManager().handleTalk(player, "npc536_final");
                });
            }
        };

        Bukkit.getPluginManager().registerEvents(handler, plugin);
    }

    /** Give the starting weapon based on the player's chosen class. */
    private void giveClassWeapon(Player player) {
        PlayerClass pc = StatsManager.getInstance().getPlayerStats(player.getUniqueId()).playerClass;
        int id;
        switch (pc) {
            case WARRIOR -> id = 1;
            case ROGUE -> id = 2;
            case MAGE -> id = 3;
            default -> id = 4; // ARCHER or others
        }

        CustomItem template = ItemManager.getInstance().getCustomItem(id);
        if (template == null) return;

        CustomItem instance = new CustomItem(
                template.getId(), template.getBaseName(), template.getRarity(),
                template.getLevelRequirement(), template.getClassRequirement(),
                template.getMaterial(), template.getHpRange(), template.getDefRange(),
                template.getStrRange(), template.getAgiRange(), template.getIntelRange(),
                template.getDexRange(), template.isEgo(), template.getEgoKey()
        );
        ItemManager.getInstance().addInstance(instance);
        player.getInventory().addItem(ItemUtil.createItemStackFromCustomItem(instance, 1, player));
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        // No additional completion logic
    }
}
