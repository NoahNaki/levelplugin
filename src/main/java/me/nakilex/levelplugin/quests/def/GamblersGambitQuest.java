package me.nakilex.levelplugin.quests.def;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.data.QuestResetScript;
import me.nakilex.levelplugin.quests.data.QuestRepeatType;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Number-guessing side quest run by the gambler NPC.
 */
public class GamblersGambitQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {
    public static final String ID = "gamblersgambit";
    public static final int NPC_ID = 1417;
    public static final String NPC_NAME = "High Stakes Gambler";
    public static final int ENTRY_FEE = 1000;
    public static final double DROP_RATE_BONUS = 10.0;
    private static final long DROP_RATE_DURATION_MS = 10 * 60 * 1000L;

    public static final int INTRO_OBJECTIVE_INDEX = 0;
    public static final int GUESS_OBJECTIVE_INDEX = 1;

    private static final String INTRO_TARGET = "npc" + NPC_ID + "_intro";
    private static final String GUESS_TARGET = "gamblers_gambit_guess";
    private static final String TARGET_FLAG_PREFIX = "gambit_target_";
    private static final String GUESS_FLAG = "awaiting_guess";
    private static final String CHOICE_FLAG_BASE = "gambit_choice_";
    private static final NamespacedKey REWARD_KEY = new NamespacedKey(Main.getInstance(), "gambit_aquamarine");
    private static final NamespacedKey BONUS_KEY = new NamespacedKey(Main.getInstance(), "gambit_bonus_until");

    private static final List<String> OFFER_DIALOG = List.of(
            "Gambler|Hey you, you seem like you like to live life on the edge, wanna play a little game for the cheap price of <glyph:coins_icon> 1,000 coins?"
    );
    private static final List<String> REPEAT_DIALOG = List.of(
            "Gambler|Come to test your luck again?"
    );
    private static final List<String> ACCEPT_DIALOG = List.of(
            "Gambler|I'm going to think of a number and you have to guess it, if you get it right, I'll give you a little gift that might come in handy someday.",
            "Gambler|Let's go gambling!!"
    );
    private static final List<String> DECLINE_DIALOG = List.of(
            "Gambler|Awh dang it!!"
    );
    private static final List<String> SUCCESS_DIALOG = List.of(
            "Gambler|YIPPIEEEE!!"
    );

    private static GamblersGambitQuest instance;
    private static boolean listenersRegistered;
    private final Map<UUID, Integer> guessTargets = new ConcurrentHashMap<>();

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.TALK, INTRO_TARGET, 1,
                        false, BeaconTargets.npc(NPC_ID),
                        "Pay the entry fee to hear the gambler's offer."),
                new QuestObjective(QuestObjectiveType.TALK, GUESS_TARGET, 1,
                        false, BeaconTargets.npc(NPC_ID),
                        "Match the gambler's hidden number between 1 and 10.")
        );
    }

    public GamblersGambitQuest() {
        super(
                ID,
                "Gamblers' Gambit",
                "Pay the ante and guess the gambler's secret number to earn a lucky keepsake.",
                createObjectives(),
                10,
                List.of(),
                null,
                QuestRewardCompat.create(200, 0, 0, List.of()),
                NPC_ID,
                OFFER_DIALOG,
                false,
                true,
                true,
                QuestRepeatType.DAILY
        );
        instance = this;
        registerLifecycleListeners();
    }

    public static GamblersGambitQuest getInstance() {
        return instance;
    }

    public static List<String> getOfferDialog() {
        return OFFER_DIALOG;
    }

    public static List<String> getAcceptDialog() {
        return ACCEPT_DIALOG;
    }

    public static List<String> getDeclineDialog() {
        return DECLINE_DIALOG;
    }

    public static List<String> getSuccessDialog() {
        return SUCCESS_DIALOG;
    }

    public static List<String> getRepeatDialog() {
        return REPEAT_DIALOG;
    }

    public static String getChoiceFlagBase() {
        return CHOICE_FLAG_BASE;
    }

    public static String getIntroTarget() {
        return INTRO_TARGET;
    }

    public static String getGuessTarget() {
        return GUESS_TARGET;
    }

    public static void registerTalkTargets(QuestManager questManager) {
        if (questManager == null) {
            return;
        }
        questManager.registerTalkTarget(INTRO_TARGET, NPC_NAME, NPC_NAME);
        questManager.registerTalkTarget(GUESS_TARGET, NPC_NAME, NPC_NAME);
    }

    @Override
    public void onStart(Player player, Main plugin) {
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }
        questManager.handleTalk(player, INTRO_TARGET);
        armGuess(player, questManager, false);
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        cleanup(player.getUniqueId(), plugin.getQuestManager());
        giveReward(player);
    }

    @Override
    public void onReset(Player player, Main plugin) {
        cleanup(player.getUniqueId(), plugin.getQuestManager());
    }

    public void remindGuess(Player player) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return;
        }
        armGuess(player, questManager, true);
    }

    public boolean isAwaitingGuess(UUID uuid) {
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return false;
        }
        PlayerQuestProgress progress = questManager.getProgress(uuid, ID);
        if (progress == null || progress.getProgress(GUESS_OBJECTIVE_INDEX) >= 1) {
            return false;
        }
        return questManager.hasFlag(uuid, ID, GUESS_FLAG);
    }

    public void handleGuess(Player player, int guess) {
        Main plugin = Main.getInstance();
        QuestManager questManager = plugin.getQuestManager();
        if (questManager == null) {
            return;
        }
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        if (progress == null || progress.getProgress(GUESS_OBJECTIVE_INDEX) >= 1) {
            cleanup(player.getUniqueId(), questManager);
            return;
        }

        int target = ensureTarget(player.getUniqueId(), progress, questManager);
        if (guess != target) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Not quite. The gambler chuckles and shuffles the deck. You'll need to start over.");
            questManager.resetQuest(player.getUniqueId(), ID, true);
            questManager.startQuest(player, ID, false);
            return;
        }

        questManager.removeFlag(player.getUniqueId(), ID, GUESS_FLAG);

        NPC npc = CitizensAPI.getNPCRegistry().getById(NPC_ID);
        me.nakilex.levelplugin.npc.dialog.NPCDialogManager dialogManager = plugin.getDialogManager();
        if (dialogManager != null && npc != null) {
            dialogManager.startDialog(player, SUCCESS_DIALOG, npc, () ->
                    questManager.handleTalk(player, GUESS_TARGET));
            new BukkitRunnable() {
                @Override
                public void run() {
                    dialogManager.advanceDialog(player, questManager);
                }
            }.runTask(plugin);
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "YIPPIEEEE!!");
            questManager.handleTalk(player, GUESS_TARGET);
        }
        cleanup(player.getUniqueId(), questManager);
    }

    private void armGuess(Player player, QuestManager questManager, boolean sendPrompt) {
        if (player == null || questManager == null) {
            return;
        }
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        if (progress == null || progress.getProgress(GUESS_OBJECTIVE_INDEX) >= 1) {
            cleanup(player.getUniqueId(), questManager);
            return;
        }
        ensureTarget(player.getUniqueId(), progress, questManager);
        questManager.setFlag(player.getUniqueId(), ID, GUESS_FLAG);
        if (sendPrompt) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Type a number between 1 and 10 in chat to match the gambler's pick.");
        }
    }

    private int ensureTarget(UUID uuid, PlayerQuestProgress progress, QuestManager questManager) {
        Integer existing = guessTargets.get(uuid);
        if (existing != null) {
            return existing;
        }

        Integer parsed = parseTargetFlag(progress);
        if (parsed != null) {
            guessTargets.put(uuid, parsed);
            return parsed;
        }

        int target = ThreadLocalRandom.current().nextInt(1, 11);
        guessTargets.put(uuid, target);
        clearTargetFlags(uuid, questManager, progress);
        questManager.setFlag(uuid, ID, TARGET_FLAG_PREFIX + target);
        return target;
    }

    private Integer parseTargetFlag(PlayerQuestProgress progress) {
        if (progress == null) {
            return null;
        }
        for (String flag : progress.getFlags()) {
            if (flag != null && flag.startsWith(TARGET_FLAG_PREFIX)) {
                try {
                    return Integer.parseInt(flag.substring(TARGET_FLAG_PREFIX.length()));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed target flags
                }
            }
        }
        return null;
    }

    private void clearTargetFlags(UUID uuid, QuestManager questManager, PlayerQuestProgress progress) {
        if (questManager == null || progress == null) {
            return;
        }
        List<String> flags = new ArrayList<>(progress.getFlags());
        for (String flag : flags) {
            if (flag != null && flag.startsWith(TARGET_FLAG_PREFIX)) {
                questManager.removeFlag(uuid, ID, flag);
            }
        }
    }

    private void cleanup(UUID uuid, QuestManager questManager) {
        guessTargets.remove(uuid);
        if (questManager == null) {
            return;
        }
        questManager.removeFlag(uuid, ID, GUESS_FLAG);
        PlayerQuestProgress progress = questManager.getProgress(uuid, ID);
        clearTargetFlags(uuid, questManager, progress);
    }

    private void giveReward(Player player) {
        if (player == null) {
            return;
        }
        ItemStack reward = createRewardItem();
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                "The gambler presses a gleaming charm into your hand.");
    }

    private ItemStack createRewardItem() {
        ItemBuilder builder = NexoItems.itemFromId("pack1_aquamarine");
        ItemStack stack = builder != null ? builder.build() : new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Highroller's Aquamarine");
            List<String> lore = new java.util.ArrayList<>();
            lore.add(ChatColor.GRAY + "A charm said to shimmer brighter after a lucky streak.");
            lore.add(" ");
            lore.addAll(TooltipUtil.bulletList("Consume to gain +10% mob drop chance for 10 minutes."));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("Consume the aquamarine", null));
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Quest Item");
            lore.add(ChatColor.RED + "Soulbound");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(REWARD_KEY, PersistentDataType.BYTE, (byte) 1);
            pdc.set(me.nakilex.levelplugin.items.utils.ItemUtil.SOULBOUND_KEY, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static double resolveDropBonus(Player player) {
        if (player == null) {
            return 0.0;
        }
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Long until = pdc.get(BONUS_KEY, PersistentDataType.LONG);
        if (until == null || until <= System.currentTimeMillis()) {
            if (until != null) {
                pdc.remove(BONUS_KEY);
            }
            return 0.0;
        }
        return DROP_RATE_BONUS;
    }

    private static void grantDropBonus(Player player) {
        if (player == null) {
            return;
        }
        long expires = System.currentTimeMillis() + DROP_RATE_DURATION_MS;
        player.getPersistentDataContainer().set(BONUS_KEY, PersistentDataType.LONG, expires);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                ChatColor.AQUA + "You feel fortune favor you. (+10% mob drop chance for 10 minutes)");
    }

    private void registerLifecycleListeners() {
        if (listenersRegistered) {
            return;
        }
        listenersRegistered = true;
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onChat(AsyncPlayerChatEvent event) {
                UUID uuid = event.getPlayer().getUniqueId();
                GamblersGambitQuest quest = GamblersGambitQuest.getInstance();
                if (quest == null || !quest.isAwaitingGuess(uuid)) {
                    return;
                }
                String raw = event.getMessage();
                if (raw == null) {
                    return;
                }

                event.setCancelled(true);
                Integer guess = parseGuess(raw.trim());
                if (guess == null) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> ChatMessageUtil.send(
                            event.getPlayer(),
                            ChatMessageUtil.MessageType.INFO,
                            "The gambler is waiting. Type a number between 1 and 10 to lock in your guess."));
                } else {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> quest.handleGuess(event.getPlayer(), guess));
                }
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                GamblersGambitQuest quest = GamblersGambitQuest.getInstance();
                if (quest != null) {
                    quest.remindGuess(event.getPlayer());
                }
            }

            @EventHandler
            public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
                if (event.getItem() == null) {
                    return;
                }
                ItemStack item = event.getItem();
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.getPersistentDataContainer().has(REWARD_KEY, PersistentDataType.BYTE)) {
                    return;
                }
                event.setCancelled(true);
                Player player = event.getPlayer();
                ItemStack inHand = event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND
                        ? player.getInventory().getItemInMainHand()
                        : player.getInventory().getItemInOffHand();
                if (inHand == null || !inHand.isSimilar(item)) {
                    return;
                }
                if (inHand.getAmount() > 1) {
                    inHand.setAmount(inHand.getAmount() - 1);
                } else {
                    if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        player.getInventory().setItemInOffHand(null);
                    }
                }
                grantDropBonus(player);
            }

            @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
            public void onNpcInteract(net.citizensnpcs.api.event.NPCRightClickEvent event) {
                if (event.getNPC() == null || event.getNPC().getId() != NPC_ID) {
                    return;
                }
                Player player = event.getClicker();
                QuestManager questManager = Main.getInstance().getQuestManager();
                if (questManager == null || questManager.getProgress(player.getUniqueId(), ID) == null) {
                    return;
                }
                me.nakilex.levelplugin.npc.dialog.NPCDialogManager dialogManager = Main.getInstance().getDialogManager();
                if (dialogManager != null && dialogManager.hasSession(player)) {
                    net.citizensnpcs.api.npc.NPC sessionNpc = dialogManager.getSessionNpc(player);
                    if (sessionNpc != null && sessionNpc.getId() == NPC_ID) {
                        dialogManager.advanceDialog(player, questManager);
                        event.setCancelled(true);
                    }
                }
            }
        }, Main.getInstance());
    }

    private static Integer parseGuess(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            int guess = Integer.parseInt(raw);
            if (guess >= 1 && guess <= 10) {
                return guess;
            }
        } catch (NumberFormatException ignored) {
            // Ignore
        }
        return null;
    }
}
