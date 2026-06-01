package me.nakilex.levelplugin.quests.def;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quest to wake Perry using a special White Monster drink.
 */
public class WakePerryQuest extends Quest implements QuestScript {
    public static final String ID = "wakeperry";
    private static final int NPC_SHINY_ID = 2925;
    private static final int NPC_PERRY_SLEEP_ID = 2924;
    private static final int NPC_PERRY_AWAKE_ID = 3587;
    private static final String INTRO_TARGET = "npc2925_intro";
    private static final String WAKE_TARGET = "npc2924_awake";
    private static final String SHINY_CHOICE_FLAG_BASE = "wakeperry_choice_";
    private static final int WAKE_INDEX = 1;
    private static final NamespacedKey WHITE_MONSTER_KEY =
            new NamespacedKey(Main.getInstance(), "white_monster_quest_item");

    private static final List<String> SHINY_INTRO = List.of(
            "Shiny|Have you seen my friend Perry anywhere?",
            "Shiny|We agreed to meet up here at 11 AM",
            "Shiny|I guess he's probably still asleep, could you wake him up for me?"
    );
    private static final List<String> SHINY_DECLINE = List.of(
            "Shiny|Alright, cya around then!"
    );
    private static final List<String> SHINY_ACCEPT = List.of(
            "Shiny|Great! Now Perry isn't an ordinary sleeper, you're going to really have to do something drastic to wake him up, here is something we here call a \"White Monster\".",
            "Shiny|Give it to him and he'll wake up."
    );
    private static final List<String> SHINY_REMINDER = List.of(
            "Shiny|Please take that White Monster over to Perry and wake him up for me."
    );
    private static final List<String> PERRY_SLEEPING = List.of(
            "Perry|5 more minutes",
            "Perry|...",
            "Perry|*snore*",
            "Perry|White... Monster..."
    );
    private static final List<String> PERRY_AWAKE_DIALOG = List.of(
            "Perry|Okay okay I'm awake, thanks for the monster",
            "Perry|Who are you? Wait what's the time?!",
            "Perry|Oh no! I agreed to meet up with Shiny, thank you for waking me up random person"
    );

    private static WakePerryQuest instance;
    private static boolean listenersRegistered;
    private final Map<UUID, Integer> sleepLineIndex = new HashMap<>();

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1,
                        false, BeaconTargets.npc(NPC_SHINY_ID),
                        "Agree to help Shiny wake Perry."),
                new QuestObjective(QuestObjectiveType.TALK, WAKE_TARGET, 1,
                        false, BeaconTargets.npc(NPC_PERRY_SLEEP_ID),
                        "Give Perry the White Monster to wake him up.")
        );
    }

    public WakePerryQuest() {
        super(
                ID,
                "Wake Perry Up",
                "Help Shiny rouse Perry with a special White Monster.",
                createObjectives(),
                40,
                List.of(),
                null,
                QuestRewardCompat.create(10000, 5000, 0, List.of()),
                NPC_SHINY_ID,
                Collections.emptyList(),
                false,
                true,
                true
        );
        instance = this;
        registerListeners();
    }

    public static void registerTalkTargets(QuestManager questManager) {
        if (questManager == null) return;
        questManager.registerTalkTarget(INTRO_TARGET, "Shiny", "Shiny");
        questManager.registerTalkTarget(WAKE_TARGET, "Perry", "Perry");
    }

    private void registerListeners() {
        if (listenersRegistered) {
            return;
        }
        listenersRegistered = true;

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onChunkLoad(ChunkLoadEvent event) {
                syncNpcVisibility();
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), WakePerryQuest.this::syncNpcVisibility, 1L);
            }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void onInteract(PlayerInteractEvent event) {
                if (event.getHand() == EquipmentSlot.OFF_HAND) {
                    return;
                }
                if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                        && isWhiteMonster(event.getItem())) {
                    event.setCancelled(true);
                }
            }
        }, Main.getInstance());

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::syncNpcVisibility, 1L);
    }

    public static boolean handleNpcInteraction(Player player, NPC npc, EquipmentSlot hand) {
        if (instance == null) {
            return false;
        }
        return instance.handleClick(player, npc, hand);
    }

    private boolean handleClick(Player player, NPC npc, EquipmentSlot hand) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        NPCDialogManager dialogManager = Main.getInstance().getDialogManager();
        if (questManager == null || dialogManager == null) {
            return false;
        }
        if (npc.getId() != NPC_SHINY_ID && npc.getId() != NPC_PERRY_SLEEP_ID && npc.getId() != NPC_PERRY_AWAKE_ID) {
            return false;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        boolean completed = questManager.hasCompleted(player.getUniqueId(), ID);

        if (dialogManager.resumePendingChoice(player, npc)) {
            return true;
        }

        if (dialogManager.hasChoiceSession(player)) {
            return true;
        }

        if (dialogManager.hasSession(player)) {
            NPC sessionNpc = dialogManager.getSessionNpc(player);
            if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                dialogManager.handlePrimaryInput(player);
            }
            return true;
        }

        if (npc.getId() == NPC_SHINY_ID) {
            handleShinyInteraction(player, npc, progress, completed, questManager, dialogManager);
            return true;
        }

        if (npc.getId() == NPC_PERRY_SLEEP_ID) {
            handleSleepingPerry(player, npc, hand, progress, completed, questManager, dialogManager);
            return true;
        }

        handleAwakePerry(player, npc, progress, completed, questManager, dialogManager);
        return true;
    }

    private void handleShinyInteraction(Player player, NPC npc, PlayerQuestProgress progress, boolean completed,
                                        QuestManager questManager, NPCDialogManager dialogManager) {
        if (completed) {
            dialogManager.startDialog(player, List.of("Shiny|Thanks for waking Perry up!"), npc, null);
            return;
        }

        if (questManager.hasFlag(player.getUniqueId(), ID, SHINY_CHOICE_FLAG_BASE + "pending")) {
            openShinyChoice(player, npc, questManager, dialogManager);
            return;
        }

        if (progress != null) {
            dialogManager.startDialog(player, SHINY_REMINDER, npc, () -> giveWhiteMonster(player));
            return;
        }

        dialogManager.startDialog(player, SHINY_INTRO, npc, () ->
                openShinyChoice(player, npc, questManager, dialogManager));
    }

    private void openShinyChoice(Player player, NPC npc, QuestManager questManager, NPCDialogManager dialogManager) {
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                dialogManager.startChoiceDialog(player, npc, List.of("Yes", "No"),
                        ID, SHINY_CHOICE_FLAG_BASE, choice -> handleShinyChoice(player, npc, questManager, dialogManager, choice)), 1L);
    }

    private void handleShinyChoice(Player player, NPC npc, QuestManager questManager,
                                   NPCDialogManager dialogManager, Integer choice) {
        UUID uuid = player.getUniqueId();
        questManager.removeFlag(uuid, ID, SHINY_CHOICE_FLAG_BASE + "pending");
        questManager.removeFlag(uuid, ID, SHINY_CHOICE_FLAG_BASE + "0");
        questManager.removeFlag(uuid, ID, SHINY_CHOICE_FLAG_BASE + "1");
        if (choice == null) {
            return;
        }
        if (choice == 0) {
            acceptQuest(player, npc, questManager, dialogManager);
        } else {
            dialogManager.startDialog(player, SHINY_DECLINE, npc, null);
        }
    }

    private void handleSleepingPerry(Player player, NPC npc, EquipmentSlot hand, PlayerQuestProgress progress,
                                     boolean completed, QuestManager questManager, NPCDialogManager dialogManager) {
        if (progress == null) {
            sendSleepingLine(player);
            return;
        }

        if (progress.getProgress(WAKE_INDEX) >= 1 || completed) {
            dialogManager.startDialog(player, List.of("Perry|See you around!"), npc, null);
            return;
        }

        EquipmentSlot slot = findWhiteMonsterSlot(player, hand);
        if (slot == null) {
            sendSleepingLine(player);
            return;
        }

        consumeOne(player, slot);
        syncNpcVisibilityForPlayer(player);
        NPC awake = NpcApi.getRegistry().getById(NPC_PERRY_AWAKE_ID);
        NPC dialogNpc = awake != null ? awake : npc;
        questManager.handleTalk(player, WAKE_TARGET);
        startAutoDialog(player, dialogNpc, PERRY_AWAKE_DIALOG, null, dialogManager, questManager);
    }

    private void handleAwakePerry(Player player, NPC npc, PlayerQuestProgress progress, boolean completed,
                                  QuestManager questManager, NPCDialogManager dialogManager) {
        if (progress == null && !completed) {
            sendSleepingLine(player);
            return;
        }

        if (progress != null && progress.getProgress(WAKE_INDEX) < 1 && !completed) {
            dialogManager.startDialog(player, PERRY_AWAKE_DIALOG, npc, () -> questManager.handleTalk(player, WAKE_TARGET));
            return;
        }

        dialogManager.startDialog(player, List.of("Perry|Thanks again for the pick-me-up!"), npc, null);
    }

    private EquipmentSlot findWhiteMonsterSlot(Player player, EquipmentSlot preferred) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        if (preferred == EquipmentSlot.HAND && isWhiteMonster(main)) {
            return EquipmentSlot.HAND;
        }
        if (preferred == EquipmentSlot.OFF_HAND && isWhiteMonster(off)) {
            return EquipmentSlot.OFF_HAND;
        }
        if (isWhiteMonster(main)) {
            return EquipmentSlot.HAND;
        }
        if (isWhiteMonster(off)) {
            return EquipmentSlot.OFF_HAND;
        }
        return null;
    }

    private void acceptQuest(Player player, NPC npc,
                             QuestManager questManager,
                             NPCDialogManager dialogManager) {
        if (!questManager.meetsRequirements(player, this)) {
            return;
        }
        questManager.startQuest(player, ID);
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        if (progress == null) {
            return;
        }
        questManager.handleTalk(player, INTRO_TARGET);
        dialogManager.startDialog(player, SHINY_ACCEPT, npc, null);
        giveWhiteMonster(player);
    }

    private void setNpcVisible(Player player, NPC npc, boolean visible) {
        if (player == null || npc == null) {
            return;
        }
        if (!npc.isSpawned() && npc.getStoredLocation() != null) {
            npc.spawn(npc.getStoredLocation());
        }
        if (!npc.isSpawned() || npc.getEntity() == null) {
            return;
        }
        if (visible) {
            player.showEntity(Main.getInstance(), npc.getEntity());
        } else {
            player.hideEntity(Main.getInstance(), npc.getEntity());
        }
    }

    private void syncNpcVisibilityForPlayer(Player player) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null || player == null) {
            return;
        }
        NPC sleeping = NpcApi.getRegistry().getById(NPC_PERRY_SLEEP_ID);
        NPC awake = NpcApi.getRegistry().getById(NPC_PERRY_AWAKE_ID);
        if (sleeping == null || awake == null) {
            return;
        }
        boolean shouldShowAwake = questManager.hasCompleted(player.getUniqueId(), ID);
        PlayerQuestProgress prog = questManager.getProgress(player.getUniqueId(), ID);
        if (prog != null && prog.getProgress(WAKE_INDEX) >= 1) {
            shouldShowAwake = true;
        }
        setNpcVisible(player, sleeping, !shouldShowAwake);
        setNpcVisible(player, awake, shouldShowAwake);
    }

    private void giveWhiteMonster(Player player) {
        if (hasWhiteMonster(player)) {
            return;
        }
        ItemStack drink = createWhiteMonster();
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(drink);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private boolean hasWhiteMonster(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isWhiteMonster(stack)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createWhiteMonster() {
        ItemStack drink = new ItemStack(org.bukkit.Material.POTION);
        ItemMeta meta = drink.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "White Monster");
            List<String> lore = new ArrayList<>(TooltipUtil.questItemLore("A strong pick-me-up from Shiny.", true));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(ItemUtil.SOULBOUND_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(WHITE_MONSTER_KEY, PersistentDataType.BYTE, (byte) 1);
            drink.setItemMeta(meta);
        }
        return drink;
    }

    private boolean isWhiteMonster(ItemStack stack) {
        if (stack == null || stack.getType() != org.bukkit.Material.POTION) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(WHITE_MONSTER_KEY, PersistentDataType.BYTE);
    }

    private void consumeOne(Player player, EquipmentSlot slot) {
        ItemStack inHand = slot == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        if (inHand == null) {
            return;
        }
        if (inHand.getAmount() > 1) {
            inHand.setAmount(inHand.getAmount() - 1);
            if (slot == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(inHand);
            } else {
                player.getInventory().setItemInOffHand(inHand);
            }
        } else {
            if (slot == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
            player.getInventory().setItemInOffHand(null);
            }
        }
    }

    @Override
    public void onStart(Player player, Main plugin) {
        giveWhiteMonster(player);
        syncNpcVisibility();
    }

    private void sendSleepingLine(Player player) {
        int idx = sleepLineIndex.getOrDefault(player.getUniqueId(), 0);
        String line = PERRY_SLEEPING.get(idx % PERRY_SLEEPING.size());
        player.sendMessage(ChatColor.GRAY + line);
        sleepLineIndex.put(player.getUniqueId(), (idx + 1) % PERRY_SLEEPING.size());
    }

    private void startAutoDialog(Player player, NPC npc, List<String> lines, Runnable finish,
                                 NPCDialogManager dialogManager, QuestManager questManager) {
        dialogManager.startDialog(player, lines, npc, finish);
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    cancel();
                    return;
                }
                if (!dialogManager.hasSession(player)) {
                    cancel();
                    return;
                }
                dialogManager.handlePrimaryInput(player);
            }
        }.runTaskTimer(Main.getInstance(), 20L, 40L);
    }

    private void syncNpcVisibility() {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            syncNpcVisibilityForPlayer(player);
        }
    }
}
