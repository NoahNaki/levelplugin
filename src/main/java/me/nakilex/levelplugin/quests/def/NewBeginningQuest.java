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
public class NewBeginningQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {
    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc536_done", 1),
                new QuestObjective(QuestObjectiveType.BUY, "starter_armor", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc536_again", 1),
                new QuestObjective(QuestObjectiveType.SELECT_CLASS, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc536_final", 1)
        );
    }

    /** Per-player listeners registered during the quest. */
    private final java.util.Map<java.util.UUID, java.util.List<Listener>> listeners = new java.util.HashMap<>();

    // Per-player flags stored via QuestManager now handle these states

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
                ),
                true
        );
    }

    /** Register a listener for the player so it can be cleaned up later. */
    private void register(Player player, Listener listener, Main plugin) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        listeners.computeIfAbsent(player.getUniqueId(), k -> new java.util.ArrayList<>()).add(listener);
    }

    @Override
    public void onStart(Player player, Main plugin) {
        // Ensure no lingering dialog effects from a previous bugged session
        plugin.getDialogManager().resetDialog(player);
        me.nakilex.levelplugin.quests.managers.QuestManager qm = Main.getInstance().getQuestManager();
        if (qm.isDebug()) {
            plugin.getLogger().info("[QuestDebug] Starting NewBeginning for " + player.getName());
        }
        qm.removeFlag(player.getUniqueId(), "newbeginning", "awaitingMerchant");
        qm.removeFlag(player.getUniqueId(), "newbeginning", "soldClothes");
        qm.removeFlag(player.getUniqueId(), "newbeginning", "givenCoins");
        qm.removeFlag(player.getUniqueId(), "newbeginning", "readyToShop");
        qm.removeFlag(player.getUniqueId(), "newbeginning", "merchantDone");

        NPC startNpc = CitizensAPI.getNPCRegistry().getById(536);
        boolean played = false;
        if (startNpc != null && startNpc.isSpawned() && player.isOnline() &&
                startNpc.getEntity().getWorld().equals(player.getWorld()) &&
                player.getLocation().distanceSquared(startNpc.getEntity().getLocation()) <= 100) {
            playDialog(player, plugin, startNpc);
            played = true;
        }

        if (!played) {
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
        }

        // Allow clicking Piwan to start the intro dialog if it hasn't played yet
        final java.util.UUID pid = player.getUniqueId();
        Listener startListener = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().getUniqueId().equals(pid)) return;
                Player pl = event.getPlayer();
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (npc.getId() != 536) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(pid, "newbeginning");
                if (prog == null) return;
                if (prog.getProgress(0) > 0) return;

                event.setCancelled(true);

                if (plugin.getDialogManager().hasSession(pl)) {
                    NPC sessionNpc = plugin.getDialogManager().getSessionNpc(pl);
                    if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                        plugin.getDialogManager().advanceDialog(pl, plugin.getQuestManager());
                    }
                    return;
                }

                playDialog(pl, plugin, npc);
            }
        };
        register(player, startListener, plugin);

        // Listen for interactions with the Starter Merchant
        Listener merchantListener = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().getUniqueId().equals(pid)) return;
                Player pl = event.getPlayer();
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (!ChatColor.stripColor(npc.getName()).equalsIgnoreCase("Starter Merchant")) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(pid, "newbeginning");
                if (prog == null) return;
                if (prog.getProgress(0) < 1) return; // wait until Piwan dialog finished

                event.setCancelled(true);

                if (qm.isDebug()) {
                    plugin.getLogger().info("[QuestDebug] Merchant click by " + pl.getName() +
                            " prog0=" + prog.getProgress(0) +
                            " flags=" + prog.getFlags());
                }

                if (plugin.getDialogManager().resumePendingChoice(pl, npc)) {
                    return;
                }

                if (plugin.getDialogManager().hasSession(pl)) {
                    NPC sessionNpc = plugin.getDialogManager().getSessionNpc(pl);
                    if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                        plugin.getDialogManager().advanceDialog(pl, plugin.getQuestManager());
                    }
                    return;
                }

                if (qm.hasFlag(pid, "newbeginning", "readyToShop") ||
                        qm.hasFlag(pid, "newbeginning", "merchantDone")) {
                    if (qm.isDebug()) {
                        plugin.getLogger().info("[QuestDebug] opening shop for " + pl.getName());
                    }
                    qm.removeFlag(pid, "newbeginning", "readyToShop");
                    qm.setFlag(pid, "newbeginning", "merchantDone");
                    pl.performCommand("merchant starter_shop");
                    return;
                }

                if (qm.hasFlag(pid, "newbeginning", "givenCoins")) {
                    qm.setFlag(pid, "newbeginning", "merchantDone");
                    pl.performCommand("merchant starter_shop");
                    return;
                }

                qm.setFlag(pid, "newbeginning", "awaitingMerchant");

                plugin.getDialogManager().startDialog(pl,
                        java.util.List.of("Starter Merchant|I'm sorry I can't sell you any equipment if you don't have any money, " +
                                "but those clothes you're wearing, I could certainly buy that off you in-exchange for some coins, whaddya say?"),
                        npc,
                        () -> Bukkit.getScheduler().runTaskLater(plugin, () ->
                                plugin.getDialogManager().startChoiceDialog(pl, npc,
                                        java.util.List.of("Yes", "No"),
                                        "newbeginning", "merchant_choice_", choice -> {
                                            qm.removeFlag(pid, "newbeginning", "awaitingMerchant");
                                            qm.setFlag(pid, "newbeginning", "merchantDone");
                                            if (choice == 0) {
                                                plugin.getEconomyManager().addCoins(pl, 200);
                                                qm.setFlag(pid, "newbeginning", "soldClothes");
                                                pl.sendMessage(ChatColor.GOLD + "You received " +
                                                        ChatColor.YELLOW + "200 <glyph:coins_icon> " +
                                                        ChatColor.GOLD + "coins.");
                                            } else {
                                                plugin.getDialogManager().startDialog(pl,
                                                        java.util.List.of("Starter Merchant|Fair enough, have a nice day."),
                                                        npc,
                                                        null);
                                                Bukkit.getScheduler().runTaskLater(plugin,
                                                        () -> plugin.getDialogManager().advanceDialog(pl, plugin.getQuestManager()),
                                                        1L);
                                            }
                                            if (qm.isDebug()) {
                                                plugin.getLogger().info("[QuestDebug] flag readyToShop set for " + pl.getName());
                                            }
                                            qm.setFlag(pid, "newbeginning", "readyToShop");
                                        }),
                                1L));

            }
        };

        register(player, merchantListener, plugin);
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
        me.nakilex.levelplugin.quests.managers.QuestManager qm = plugin.getQuestManager();
        final java.util.UUID pid = player.getUniqueId();
        Listener handler = new Listener() {
            @EventHandler(priority = EventPriority.LOWEST)
            public void onInteract(PlayerInteractEntityEvent event) {
                if (!event.getPlayer().getUniqueId().equals(pid)) return;
                Player pl = event.getPlayer();
                if (event.getHand() == EquipmentSlot.OFF_HAND) return;
                if (!CitizensAPI.getNPCRegistry().isNPC(event.getRightClicked())) return;
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked());
                if (npc.getId() != 536) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(pid, "newbeginning");
                if (prog == null) return;

                event.setCancelled(true);

                if (plugin.getDialogManager().hasSession(pl)) {
                    NPC sessionNpc = plugin.getDialogManager().getSessionNpc(pl);
                    if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                        plugin.getDialogManager().advanceDialog(pl, plugin.getQuestManager());
                    }
                    return;
                }

                if (prog.getProgress(1) < 1) {
                    if (!qm.hasFlag(pid, "newbeginning", "givenCoins") &&
                            !qm.hasFlag(pid, "newbeginning", "soldClothes")) {
                        plugin.getDialogManager().startDialog(pl,
                                java.util.List.of("Piwan|Oh right, I should've realised you wouldn't have any currency belonging to this world, here, you can pay me back in the future."),
                                npc,
                                () -> {
                                    plugin.getEconomyManager().addCoins(pl, 100);
                                    qm.setFlag(pid, "newbeginning", "givenCoins");
                                    pl.sendMessage(ChatColor.GOLD + "You received "
                                            + ChatColor.YELLOW + "100 <glyph:coins_icon> " + ChatColor.GOLD + "coins.");
                                });
                    } else {
                        plugin.getDialogManager().startDialog(pl,
                                java.util.List.of("Piwan|Go ahead and buy some new equipment."),
                                npc,
                                null);
                    }
                    return;
                }

                if (prog.getProgress(2) < 1) {
                    plugin.getDialogManager().startDialog(pl,
                            java.util.List.of("Piwan|Alright great now that you look like you belong here, now you just have to tell me what class you'll be going so that we can find you an appropriate weapon."),
                            npc,
                            () -> {
                                plugin.getQuestManager().handleTalk(pl, "npc536_again");
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                        if (pl.isOnline()) {
                                            pl.performCommand("class");
                                        }
                                    }, 20L);
                            });
                    return;
                }

                if (prog.getProgress(3) < 1) {
                    plugin.getDialogManager().startDialog(pl,
                            java.util.List.of("Piwan|Alright great now that you look like you belong here, now you just have to tell me what class you'll be going so that we can find you an appropriate weapon."),
                            npc,
                            () -> Bukkit.getScheduler().runTaskLater(plugin,
                                    () -> { if (pl.isOnline()) pl.performCommand("class"); }, 20L));
                    return;
                }

                PlayerClass pc = StatsManager.getInstance().getPlayerStats(pid).playerClass;
                String className = pc.name().substring(0, 1) + pc.name().substring(1).toLowerCase();
                java.util.List<String> lines = java.util.List.of(
                        "Ah you went with the " + className + ", I should have a spare weapon lying around here somewhere, let's see hmmmm",
                        "AH! here you go.",
                        "Now you're all set, I'm sure our paths will cross again adventurer, now go and explore the vast world of Eldrin."
                );

                plugin.getDialogManager().startDialog(pl, lines, npc, () -> {
                    giveClassWeapon(pl);
                    plugin.getQuestManager().handleTalk(pl, "npc536_final");
                });
            }
        };

        register(player, handler, plugin);
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
    public void onReset(Player player, Main plugin) {
        // Fully clear any dialog session to avoid stuck slowness effects
        plugin.getDialogManager().resetDialog(player);
        if (Main.getInstance().getQuestManager().isDebug()) {
            plugin.getLogger().info("[QuestDebug] Resetting NewBeginning for " + player.getName());
        }
        java.util.List<Listener> list = listeners.remove(player.getUniqueId());
        if (list != null) {
            for (Listener l : list) {
                HandlerList.unregisterAll(l);
            }
        }
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        // No additional completion logic
    }
}
