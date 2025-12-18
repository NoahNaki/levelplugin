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
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    private static boolean listenersRegistered;

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
            public void onRightClick(NPCRightClickEvent event) {
                NPC npc = event.getNPC();
                Player player = event.getClicker();
                if (npc == null || player == null) {
                    return;
                }

                if (npc.getId() == NPC_SHINY_ID) {
                    handleShinyClick(player, npc, event);
                } else if (npc.getId() == NPC_PERRY_SLEEP_ID) {
                    handlePerryClick(player, npc, event);
                }
            }
        }, Main.getInstance());
    }

    private void handleShinyClick(Player player, NPC npc, NPCRightClickEvent event) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        NPCDialogManager dialogManager = Main.getInstance().getDialogManager();
        if (questManager == null || dialogManager == null) {
            return;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        boolean completed = questManager.hasCompleted(player.getUniqueId(), ID);

        if (dialogManager.resumePendingChoice(player, npc)) {
            event.setCancelled(true);
            return;
        }

        if (dialogManager.hasSession(player)) {
            NPC sessionNpc = dialogManager.getSessionNpc(player);
            if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                event.setCancelled(true);
                dialogManager.advanceDialog(player, questManager);
            }
            return;
        }

        if (completed) {
            event.setCancelled(true);
            dialogManager.startDialog(player, List.of("Shiny|Thanks for waking Perry up!"), npc, null);
            return;
        }

        if (progress != null) {
            event.setCancelled(true);
            dialogManager.startDialog(player, SHINY_REMINDER, npc, () -> giveWhiteMonster(player));
            return;
        }

        event.setCancelled(true);
        dialogManager.startDialog(player, SHINY_INTRO, npc, () ->
                dialogManager.startChoiceDialog(player, npc, List.of("Yes", "No"),
                        null, null, choice -> {
                            if (choice == 0) {
                                acceptQuest(player, npc, questManager, dialogManager);
                            } else {
                                dialogManager.startDialog(player, SHINY_DECLINE, npc, null);
                            }
                        }));
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

    private void handlePerryClick(Player player, NPC npc, NPCRightClickEvent event) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        NPCDialogManager dialogManager = Main.getInstance().getDialogManager();
        if (questManager == null || dialogManager == null) {
            return;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        boolean completed = questManager.hasCompleted(player.getUniqueId(), ID);

        if (dialogManager.hasSession(player)) {
            NPC sessionNpc = dialogManager.getSessionNpc(player);
            if (sessionNpc != null && sessionNpc.getId() == npc.getId()) {
                event.setCancelled(true);
                dialogManager.advanceDialog(player, questManager);
            }
            return;
        }

        if (progress == null) {
            event.setCancelled(true);
            dialogManager.startDialog(player, PERRY_SLEEPING, npc, null);
            return;
        }

        if (progress.getProgress(WAKE_INDEX) >= 1 || completed) {
            event.setCancelled(true);
            dialogManager.startDialog(player, List.of("Perry|See you around!"), npc, null);
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        EquipmentSlot slot = null;
        if (isWhiteMonster(main)) {
            slot = EquipmentSlot.HAND;
        } else if (isWhiteMonster(off)) {
            slot = EquipmentSlot.OFF_HAND;
        }
        if (slot == null) {
            event.setCancelled(true);
            dialogManager.startDialog(player, PERRY_SLEEPING, npc, null);
            return;
        }

        event.setCancelled(true);
        consumeOne(player, slot);
        swapNpcStates();
        NPC awake = CitizensAPI.getNPCRegistry().getById(NPC_PERRY_AWAKE_ID);
        NPC dialogNpc = awake != null ? awake : npc;
        dialogManager.startDialog(player, PERRY_AWAKE_DIALOG, dialogNpc, () -> {
            questManager.handleTalk(player, WAKE_TARGET);
        });
    }

    private void swapNpcStates() {
        setNpcVisible(NPC_PERRY_SLEEP_ID, false);
        setNpcVisible(NPC_PERRY_AWAKE_ID, true);
    }

    private void setNpcVisible(int npcId, boolean visible) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null) {
            return;
        }
        if (visible) {
            if (!npc.isSpawned() && npc.getStoredLocation() != null) {
                npc.spawn(npc.getStoredLocation());
            }
        } else if (npc.isSpawned()) {
            npc.despawn();
        }
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
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES);
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
    }
}
