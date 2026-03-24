package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.gui.ClassSelectionGUI;
import me.nakilex.levelplugin.quests.data.*;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
import me.nakilex.levelplugin.utils.NpcNameUtil;

import java.util.List;

/**
 * Intro quest that plays a short conversation with Piwan.
 */
public class NewBeginningQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {
    private static final int PIWAN_NPC_ID = 546;
    private static final int STARTER_MERCHANT_NPC_ID = 547;

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, "npc546_done", 1, BeaconTargets.npc(PIWAN_NPC_ID)),
                new QuestObjective(QuestObjectiveType.BUY, "starter_armor", 1, BeaconTargets.npc(STARTER_MERCHANT_NPC_ID)),
                new QuestObjective(QuestObjectiveType.TALK, "npc546_again", 1, BeaconTargets.npc(PIWAN_NPC_ID)),
                new QuestObjective(QuestObjectiveType.SELECT_CLASS, "ANY", 1),
                new QuestObjective(QuestObjectiveType.TALK, "npc546_final", 1, BeaconTargets.npc(PIWAN_NPC_ID))
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
                546,
                List.of(
                        "Hey you there! I could've sworn no one was standing there a second ago, how did you suddenly appear?",
                        "You certainly don't look from around here, especially with those clothes, perhaps a noble from another country.",
                        "Another world you say? Well you wouldn't be the first to make such bold claims, my mom said she once knew someone that claimed the same thing, said they were from a place called, \"Japan\".",
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

        NpcInteraction startNpc = resolveNpcInteraction(PIWAN_NPC_ID);
        boolean played = false;
        if (startNpc != null && player.isOnline() && isNpcSpawned(startNpc.npc(), startNpc.citizensNpc())) {
            Location npcLocation = getNpcLocation(startNpc.npc(), startNpc.citizensNpc());
            if (npcLocation != null
                    && npcLocation.getWorld() != null
                    && npcLocation.getWorld().equals(player.getWorld())
                    && player.getLocation().distanceSquared(npcLocation) <= 25) {
                playDialog(player, plugin, startNpc.npc(), startNpc.citizensNpc());
                played = true;
            }
        }

        if (!played) {
            new BukkitRunnable() {
                boolean triggered = false;
                @Override
                public void run() {
                    NpcInteraction npc = resolveNpcInteraction(PIWAN_NPC_ID);
                    if (npc == null || !isNpcSpawned(npc.npc(), npc.citizensNpc())) return;
                    if (!player.isOnline()) { cancel(); return; }
                    Location npcLocation = getNpcLocation(npc.npc(), npc.citizensNpc());
                    if (npcLocation == null || npcLocation.getWorld() == null) return;
                    if (!npcLocation.getWorld().equals(player.getWorld())) return;
                    if (!triggered && player.getLocation().distanceSquared(npcLocation) <= 25) {
                        playDialog(player, plugin, npc.npc(), npc.citizensNpc());
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
                NpcInteraction clicked = resolveNpcInteraction(event);
                if (clicked == null || clicked.id() != PIWAN_NPC_ID) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(pid, "newbeginning");
                if (prog == null) return;
                if (prog.getProgress(0) > 0) return;

                event.setCancelled(true);

                if (plugin.getDialogManager().hasSession(pl)) {
                    if (plugin.getDialogManager().isSessionNpc(pl, clicked.id())) {
                        plugin.getDialogManager().advanceDialog(pl, plugin.getQuestManager());
                    }
                    return;
                }

                playDialog(pl, plugin, clicked.npc(), clicked.citizensNpc());
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
                NpcInteraction clicked = resolveNpcInteraction(event);
                if (clicked == null || !isNpcName(clicked.name(), "Starter Merchant")) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(pid, "newbeginning");
                if (prog == null) return;
                if (prog.getProgress(0) < 1) return; // wait until Piwan dialog finished

                event.setCancelled(true);

                if (qm.isDebug()) {
                    plugin.getLogger().info("[QuestDebug] Merchant click by " + pl.getName() +
                            " prog0=" + prog.getProgress(0) +
                            " flags=" + prog.getFlags());
                }

                if (resumePendingChoice(pl, clicked)) {
                    return;
                }

                if (plugin.getDialogManager().hasSession(pl)) {
                    if (plugin.getDialogManager().isSessionNpc(pl, clicked.id())) {
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

                startDialog(pl,
                        java.util.List.of("Starter Merchant|I'm sorry I can't sell you any equipment if you don't have any money, " +
                                "but those clothes you're wearing, I could certainly buy that off you in-exchange for some coins, whaddya say?"),
                        clicked,
                        () -> Bukkit.getScheduler().runTaskLater(plugin, () ->
                                startChoiceDialog(pl, clicked,
                                        java.util.List.of("Yes", "No"),
                                        "newbeginning", "merchant_choice_", choice -> {
                                            qm.removeFlag(pid, "newbeginning", "awaitingMerchant");
                                            qm.setFlag(pid, "newbeginning", "merchantDone");
                                            if (choice == 0) {
                                                plugin.getEconomyManager().addCoins(pl, 200);
                                                qm.setFlag(pid, "newbeginning", "soldClothes");
                                                me.nakilex.levelplugin.utils.CurrencyMessageUtil.sendReceive(pl,
                                                        me.nakilex.levelplugin.utils.CurrencyMessageUtil.Currency.COINS, 200);
                                            } else {
                                                startDialog(pl,
                                                        java.util.List.of("Starter Merchant|Fair enough, have a nice day."),
                                                        clicked,
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

    private void playDialog(Player player, Main plugin, NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        java.util.List<String> lines = java.util.List.of(
                "Hey you there! I could've sworn no one was standing there a second ago, how did you suddenly appear?",
                "You certainly don't look from around here, especially with those clothes, perhaps a noble from another country.",
                "Another world you say? Well you wouldn't be the first to make such bold claims, my mom said she once knew someone that claimed the same thing, said they were from a place called, \"Japan\".",
                "I'm sure you have many questions, how about to start off I show you around my village.",
                "First things first, you're going to have to look like you're from this world, go talk to that merchant over there and buy some equipment."
        );

        if (npc != null) {
            plugin.getDialogManager().startDialog(player, lines, npc, () -> {
                if (player.isOnline()) {
                    Main.getInstance().getQuestManager().handleTalk(player, "npc546_done");
                    registerFinalDialog(player, plugin);
                }
            });
            return;
        }
        plugin.getDialogManager().startDialog(player, lines, citizensNpc, () -> {
            if (player.isOnline()) {
                Main.getInstance().getQuestManager().handleTalk(player, "npc546_done");
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
                NpcInteraction clicked = resolveNpcInteraction(event);
                if (clicked == null || clicked.id() != PIWAN_NPC_ID) return;

                PlayerQuestProgress prog = plugin.getQuestManager().getProgress(pid, "newbeginning");
                if (prog == null) return;

                event.setCancelled(true);

                if (plugin.getDialogManager().hasSession(pl)) {
                    if (plugin.getDialogManager().isSessionNpc(pl, clicked.id())) {
                        plugin.getDialogManager().advanceDialog(pl, plugin.getQuestManager());
                    }
                    return;
                }

                if (prog.getProgress(1) < 1) {
                    if (!qm.hasFlag(pid, "newbeginning", "givenCoins") &&
                            !qm.hasFlag(pid, "newbeginning", "soldClothes")) {
                        startDialog(pl,
                                java.util.List.of("Piwan|Oh right, I should've realised you wouldn't have any currency belonging to this world, here, you can pay me back in the future."),
                                clicked,
                                () -> {
                                    plugin.getEconomyManager().addCoins(pl, 100);
                                    qm.setFlag(pid, "newbeginning", "givenCoins");
                                    me.nakilex.levelplugin.utils.CurrencyMessageUtil.sendReceive(pl,
                                            me.nakilex.levelplugin.utils.CurrencyMessageUtil.Currency.COINS, 100);
                                });
                    } else {
                        startDialog(pl,
                                java.util.List.of("Piwan|Go ahead and buy some new equipment."),
                                clicked,
                                null);
                    }
                    return;
                }

                if (prog.getProgress(2) < 1) {
                    startDialog(pl,
                            java.util.List.of("Piwan|Alright great now that you look like you belong here, now you just have to tell me what class you'll be going so that we can find you an appropriate weapon."),
                            clicked,
                            () -> {
                                plugin.getQuestManager().handleTalk(pl, "npc546_again");
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                        if (pl.isOnline()) {
                                            ClassSelectionGUI.getInstance().open(pl);
                                        }
                                    }, 20L);
                            });
                    return;
                }

                if (prog.getProgress(3) < 1) {
                    startDialog(pl,
                            java.util.List.of("Piwan|Alright great now that you look like you belong here, now you just have to tell me what class you'll be going so that we can find you an appropriate weapon."),
                            clicked,
                            () -> Bukkit.getScheduler().runTaskLater(plugin,
                                    () -> { if (pl.isOnline()) ClassSelectionGUI.getInstance().open(pl); }, 20L));
                    return;
                }

                PlayerClass pc = StatsManager.getInstance().getPlayerStats(pid).playerClass;
                String className = pc.name().substring(0, 1) + pc.name().substring(1).toLowerCase();
                java.util.List<String> lines = java.util.List.of(
                        "You went with the " + className + ", I should have a spare weapon & essence lying around here somewhere, let's see hmmmm",
                        "AH! here you go, check out the class essence in your /essence menu.",
                        "Now you're all set, I'm sure our paths will cross again adventurer, now go and explore the vast world of Eldrin."
                );

                startDialog(pl, lines, clicked, () -> {
                    giveClassWeapon(pl);
                    // Populate and equip a starting class essence
                    StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(pl.getUniqueId());
                    if (ps.essenceSlots[0] == null) {
                        ItemStack essence = ClassEssence.generateEssence(ps.playerClass);
                        ClassEssence.setEquipped(essence, true);
                        ClassEssence.setSoulbound(essence, true);
                        ClassEssence.addSlotTips(essence);
                        ClassEssence.applyAttributes(pl, essence);
                        ps.essenceSlots[0] = essence;
                        ps.equippedEssences[0] = true;
                        ItemUtil.refreshTooltips(pl);
                    }
                    plugin.getQuestManager().handleTalk(pl, "npc546_final");
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
            case MAGE -> id = 2002;
            case ARCHER -> id = 2003;
            case WARRIOR, ROGUE -> id = 2001;
            default -> id = 2001;
        }

        CustomItem template = ItemManager.getInstance().getCustomItem(id);
        if (template == null) return;

        CustomItem instance = new CustomItem(
                template.getId(), template.getBaseName(), template.getRarity(),
                template.getLevelRequirement(), template.getClassRequirement(),
                template.getMaterial(), template.getHpRange(), template.getDefRange(),
                template.getStrRange(), template.getAgiRange(), template.getIntelRange(),
                template.getDexRange(), template.getWilRange(), template.getTecRange()
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
        plugin.getQuestManager().startQuest(player, "serashelp");
        plugin.getQuestManager().startQuest(player, me.nakilex.levelplugin.quests.def.WayfarersMarkQuest.ID, false);
    }

    private void startDialog(Player player, List<String> lines, NpcInteraction npc, Runnable finish) {
        if (npc.npc() != null) {
            Main.getInstance().getDialogManager().startDialog(player, lines, npc.npc(), finish);
        } else {
            Main.getInstance().getDialogManager().startDialog(player, lines, npc.citizensNpc(), finish);
        }
    }

    private void startChoiceDialog(Player player, NpcInteraction npc, List<String> options,
                                   String questId, String flagBase,
                                   java.util.function.Consumer<Integer> callback) {
        if (npc.npc() != null) {
            Main.getInstance().getDialogManager().startChoiceDialog(player, npc.npc(), options, questId, flagBase, callback);
        } else {
            Main.getInstance().getDialogManager().startChoiceDialog(player, npc.citizensNpc(), options, questId, flagBase, callback);
        }
    }

    private boolean isNpcName(String npcName, String expectedName) {
        if (npcName == null || expectedName == null) {
            return false;
        }
        return NpcNameUtil.equalsNormalized(npcName, expectedName);
    }

    private boolean resumePendingChoice(Player player, NpcInteraction clicked) {
        if (clicked.npc() != null) {
            return Main.getInstance().getDialogManager().resumePendingChoice(player, clicked.npc());
        }
        return Main.getInstance().getDialogManager().resumePendingChoice(player, clicked.citizensNpc());
    }

    private NpcInteraction resolveNpcInteraction(PlayerInteractEntityEvent event) {
        if (event == null || event.getRightClicked() == null) {
            return null;
        }
        NPC npc = NpcApi.getRegistry().getNPC(event.getRightClicked());
        net.citizensnpcs.api.npc.NPC citizensNpc = npc == null
                ? CitizensAPI.getNPCRegistry().getNPC(event.getRightClicked())
                : null;
        if (npc == null && citizensNpc == null) {
            return null;
        }
        return new NpcInteraction(npc, citizensNpc);
    }

    private NpcInteraction resolveNpcInteraction(int npcId) {
        NPC npc = NpcApi.getRegistry().getById(npcId);
        net.citizensnpcs.api.npc.NPC citizensNpc = npc == null
                ? CitizensAPI.getNPCRegistry().getById(npcId)
                : null;
        if (npc == null && citizensNpc == null) {
            return null;
        }
        return new NpcInteraction(npc, citizensNpc);
    }

    private Location getNpcLocation(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            if (npc.isSpawned() && npc.getEntity() != null) {
                return npc.getEntity().getLocation();
            }
            return null;
        }
        if (citizensNpc != null) {
            if (citizensNpc.isSpawned() && citizensNpc.getEntity() != null) {
                return citizensNpc.getEntity().getLocation();
            }
            return null;
        }
        return null;
    }

    private boolean isNpcSpawned(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
        if (npc != null) {
            return npc.isSpawned() && npc.getEntity() != null;
        }
        if (citizensNpc != null) {
            return citizensNpc.isSpawned() && citizensNpc.getEntity() != null;
        }
        return false;
    }

    private static final class NpcInteraction {
        private final NPC npc;
        private final net.citizensnpcs.api.npc.NPC citizensNpc;

        private NpcInteraction(NPC npc, net.citizensnpcs.api.npc.NPC citizensNpc) {
            this.npc = npc;
            this.citizensNpc = citizensNpc;
        }

        private NPC npc() {
            return npc;
        }

        private net.citizensnpcs.api.npc.NPC citizensNpc() {
            return citizensNpc;
        }

        private int id() {
            return npc != null ? npc.getId() : citizensNpc.getId();
        }

        private String name() {
            return npc != null ? npc.getName() : citizensNpc.getName();
        }
    }
}
