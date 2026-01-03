package me.nakilex.levelplugin.npc.handlers;

import me.nakilex.levelplugin.npc.dialog.NPCDialogManager;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.def.EssenceWeaversLessonQuest;
import me.nakilex.levelplugin.quests.gui.QuestState;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles the Essence Weaver tutorial interactions.
 */
public class EssenceWeaverLessonNpcHandler extends AbstractQuestNpcHandler {

    public EssenceWeaverLessonNpcHandler(QuestManager questManager, NPCDialogManager dialogManager) {
        super(EssenceWeaversLessonQuest.ID, questManager, dialogManager);
    }

    @Override
    public boolean handle(Player player, NPC npc, Quest quest, QuestState state,
                          QuestManager questManager, NPCDialogManager dialogManager) {
        if (state == QuestState.AVAILABLE) {
            dialogManager.startDialog(player, quest, npc);
            return true;
        }
        if (state == QuestState.LOCKED) {
            questManager.meetsRequirements(player, quest);
            return true;
        }

        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), quest.getId());
        boolean introDone = progress != null && progress.getProgress(0) >= 1;
        boolean upgradeTried = progress != null && progress.getProgress(1) >= 1;
        boolean swapped = progress != null && progress.getProgress(2) >= 1;

        if (!introDone && progress != null) {
            dialogManager.startDialog(player,
                    quest.getDialogLines(),
                    npc,
                    () -> {
                        questManager.handleTalk(player, EssenceWeaversLessonQuest.NPC_NAME.equalsIgnoreCase(npc.getName())
                                ? "npc_essence_weaver_intro"
                                : "npc" + npc.getId());
                        ensureIntroEssence(player);
                    });
            return true;
        }

        if (questManager.hasCompleted(player.getUniqueId(), EssenceWeaversLessonQuest.ID)) {
            player.performCommand("essenceupgrade");
            return true;
        }

        if (!upgradeTried) {
            player.performCommand("essenceupgrade");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Use the essence altar to invest a duplicate or attempt a star upgrade. Then press F to swap to another essence.");
            return true;
        }

        if (!swapped) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Press F to swap to another essence and complete your training.");
            return true;
        }

        player.performCommand("essenceupgrade");
        return true;
    }

    private void ensureIntroEssence(Player player) {
        if (player == null) {
            return;
        }
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        Set<PlayerClass> ownedClasses = getOwnedEssenceClasses(player, stats);
        PlayerClass owned = selectOwnedClass(stats, ownedClasses);
        PlayerClass missing = selectMissingClass(ownedClasses, owned);
        giveQuestEssence(player, owned);
        if (missing != null) {
            giveQuestEssence(player, missing);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Essence Weaver|Take these essences to begin your training.");
    }

    private Set<PlayerClass> getOwnedEssenceClasses(Player player, StatsManager.PlayerStats stats) {
        Set<PlayerClass> owned = new HashSet<>();
        if (stats != null && stats.essenceSlots != null) {
            for (ItemStack stack : stats.essenceSlots) {
                PlayerClass clazz = ClassEssence.getClass(stack);
                if (clazz != null) {
                    owned.add(clazz);
                }
            }
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            PlayerClass clazz = ClassEssence.getClass(stack);
            if (clazz != null) {
                owned.add(clazz);
            }
        }
        return owned;
    }

    private PlayerClass selectOwnedClass(StatsManager.PlayerStats stats, Set<PlayerClass> owned) {
        if (!owned.isEmpty()) {
            return owned.iterator().next();
        }
        if (stats != null && stats.playerClass != null) {
            return stats.playerClass;
        }
        List<PlayerClass> pool = ClassEssence.getCoreEssencePool();
        return pool.isEmpty() ? PlayerClass.MAGE : pool.get(0);
    }

    private PlayerClass selectMissingClass(Set<PlayerClass> owned, PlayerClass ownedClass) {
        List<PlayerClass> pool = ClassEssence.getCoreEssencePool();
        for (PlayerClass clazz : pool) {
            if (!owned.contains(clazz) && clazz != ownedClass) {
                return clazz;
            }
        }
        for (PlayerClass clazz : pool) {
            if (clazz != ownedClass) {
                return clazz;
            }
        }
        return ownedClass;
    }

    private void giveQuestEssence(Player player, PlayerClass clazz) {
        if (player == null || clazz == null) {
            return;
        }
        ItemStack essence = ClassEssence.generateEssence(clazz);
        markQuestEssence(essence);
        player.getInventory().addItem(essence).values()
                .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private void markQuestEssence(ItemStack stack) {
        if (stack == null) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(ItemUtil.SOULBOUND_KEY, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
    }
}
