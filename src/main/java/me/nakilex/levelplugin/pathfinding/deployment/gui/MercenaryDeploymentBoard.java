package me.nakilex.levelplugin.pathfinding.deployment.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.pathfinding.deployment.MercenaryDeploymentDefinition;
import me.nakilex.levelplugin.pathfinding.deployment.MercenaryDeploymentManager;
import me.nakilex.levelplugin.pathfinding.deployment.MercenaryDeploymentState;
import me.nakilex.levelplugin.pathfinding.deployment.MercenaryDeploymentState.ActiveDeployment;
import me.nakilex.levelplugin.pathfinding.deployment.MercenaryDeploymentState.CompletedDeployment;
import me.nakilex.levelplugin.pathfinding.deployment.MercenarySpecialization;
import me.nakilex.levelplugin.quests.data.QuestReward;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Builds the mercenary deployment board inventory and encodes click actions
 * using persistent data values for the accompanying listener.
 */
public final class MercenaryDeploymentBoard {
    private MercenaryDeploymentBoard() {}

    public static final String TITLE = ChatColor.BLACK + "Mercenary Contracts";
    private static final int[] DAILY_SLOTS = {10, 12, 14, 16};
    private static final int[] ACTIVE_SLOTS = {29, 31, 33};
    private static final int[] COMPLETED_SLOTS = {38, 40, 42};
    private static final int INFO_SLOT = 22;

    public static final NamespacedKey KEY_ACTION = new NamespacedKey(Main.getInstance(), "merc_contract_action");
    public static final NamespacedKey KEY_ID = new NamespacedKey(Main.getInstance(), "merc_contract_id");

    public static final String ACTION_START = "start";
    public static final String ACTION_CLAIM = "claim";
    public static final String ACTION_CANCEL = "cancel";

    public static Inventory create(Main plugin,
                                   MercenaryDeploymentManager manager,
                                   Player viewer,
                                   MercenaryDeploymentState state) {
        GuiBuilder builder = GuiBuilder.create(54, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border();

        builder.setItem(INFO_SLOT, infoItem());

        List<MercenaryDeploymentDefinition> deployments = manager.getDailyDeployments();
        for (int i = 0; i < DAILY_SLOTS.length && i < deployments.size(); i++) {
            MercenaryDeploymentDefinition def = deployments.get(i);
            ItemStack item = createDailyItem(viewer, def);
            encode(item, ACTION_START, def.id());
            builder.setItem(DAILY_SLOTS[i], item);
        }

        Iterator<ActiveDeployment> activeIt = state.activeDeployments().iterator();
        for (int slot : ACTIVE_SLOTS) {
            if (!activeIt.hasNext()) break;
            ActiveDeployment active = activeIt.next();
            MercenaryDeploymentDefinition def = manager.getDefinition(active.deploymentId()).orElse(null);
            if (def == null) continue;
            ItemStack item = createActiveItem(def, active);
            encode(item, ACTION_CANCEL, active.deploymentId());
            builder.setItem(slot, item);
        }

        Iterator<CompletedDeployment> completedIt = state.completedDeployments().iterator();
        for (int slot : COMPLETED_SLOTS) {
            if (!completedIt.hasNext()) break;
            CompletedDeployment completed = completedIt.next();
            MercenaryDeploymentDefinition def = manager.getDefinition(completed.deploymentId()).orElse(null);
            if (def == null) continue;
            ItemStack item = createCompletedItem(def, completed);
            encode(item, ACTION_CLAIM, completed.deploymentId());
            builder.setItem(slot, item);
        }

        return builder.build();
    }

    private static ItemStack infoItem() {
        ItemStack item = GuiUtil.getNexoItem("pack1_scroll2", ChatColor.GOLD + "Contract Briefing");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Send bound mercenaries on background missions.");
            lore.add(ChatColor.GRAY + "Success rates improve with the recommended specialization.");
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("on a contract to dispatch", "an active contract to recall"));
            lore.add(ChatColor.GRAY + "Claim completed reports from the lower row.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createDailyItem(Player viewer, MercenaryDeploymentDefinition def) {
        ItemStack item = new ItemStack(def.recommended().icon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(def.displayName());
            List<String> lore = new ArrayList<>(def.description());
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + formatMinutes(def.durationMinutes()));
            lore.add(ChatColor.GRAY + "Difficulty: " + ChatColor.YELLOW + GuiUtil.glyphStars(def.difficulty()));
            lore.add(ChatColor.GRAY + "Base Success: " + ChatColor.YELLOW + percentage(def.baseSuccessChance()));
            lore.add(ChatColor.GRAY + "Recommended: " + ChatColor.YELLOW + def.recommended().displayName());
            lore.add(" ");
            lore.add(ChatColor.GREEN + "Primary Rewards:");
            appendRewardLore(lore, def.successReward());
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to send a squad", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createActiveItem(MercenaryDeploymentDefinition def, ActiveDeployment active) {
        ItemStack item = GuiUtil.getNexoItem("hourglass", ChatColor.AQUA + def.displayName());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            long now = System.currentTimeMillis();
            long remaining = active.remaining(now);
            double elapsed = active.durationMillis() - remaining;
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Specialist: " + ChatColor.YELLOW + active.specialization().displayName());
            lore.add(ChatColor.GRAY + "Chance: " + ChatColor.YELLOW + percentage(active.successChance()));
            lore.add(ChatColor.GRAY + "Reward Mod: " + ChatColor.YELLOW + String.format("%.0f%%", active.rewardMultiplier() * 100));
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Time Remaining: " + ChatColor.YELLOW + formatDuration(remaining));
            lore.add(TooltipUtil.progressBar(elapsed, active.durationMillis(), 20));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions(null, "to recall (no reward)"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createCompletedItem(MercenaryDeploymentDefinition def, CompletedDeployment completed) {
        ItemStack item = GuiUtil.getNexoItem(completed.success() ? "check" : "cross",
                (completed.success() ? ChatColor.GREEN : ChatColor.RED) + def.displayName());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Specialist: " + ChatColor.YELLOW + completed.specialization().displayName());
            lore.add(ChatColor.GRAY + "Reported Chance: " + ChatColor.YELLOW + percentage(completed.successChance()));
            lore.add(ChatColor.GRAY + "Reward Mod: " + ChatColor.YELLOW + String.format("%.0f%%", completed.rewardMultiplier() * 100));
            lore.add(" ");
            lore.add(ChatColor.GREEN + "Reported Rewards:");
            QuestReward reward = completed.success() ? def.successReward() : def.failureReward();
            appendRewardLore(lore, reward);
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to claim", null));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void appendRewardLore(List<String> lore, QuestReward reward) {
        if (reward == null) {
            lore.add(ChatColor.GRAY + "- None");
            return;
        }
        String expLabel = ChatFormatter.experienceLabel();
        String expColor = ChatFormatter.experienceColor();
        if (reward.getXp() > 0) {
            lore.add(ChatColor.GRAY + "- " + expColor + reward.getXp() + ChatColor.RESET + " <glyph:experience_orb_icon> " + expLabel);
        }
        if (reward.getCoins() > 0) {
            lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + reward.getCoins() + " <glyph:coins_icon>");
        }
        if (reward.getGems() > 0) {
            lore.add(ChatColor.GRAY + "- " + ChatColor.LIGHT_PURPLE + reward.getGems() + " <glyph:purple_orb_icon>");
        }
        if (!reward.getItemIds().isEmpty()) {
            lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + reward.getItemIds().size() + " item drop(s)");
        }
        if (!reward.getUnlockClasses().isEmpty()) {
            lore.add(ChatColor.GRAY + "- Unlocks class token");
        }
    }

    private static String formatMinutes(int minutes) {
        if (minutes < 60) {
            return minutes + "m";
        }
        int hours = minutes / 60;
        int rem = minutes % 60;
        return hours + "h " + rem + "m";
    }

    private static String percentage(double value) {
        return String.format("%.0f%%", value * 100.0);
    }

    private static String formatDuration(long millis) {
        if (millis <= 0) {
            return "Completed";
        }
        long seconds = millis / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remSeconds = seconds % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + remSeconds + "s";
        }
        return remSeconds + "s";
    }

    private static void encode(ItemStack item, String action, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(KEY_ID, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
    }
}
