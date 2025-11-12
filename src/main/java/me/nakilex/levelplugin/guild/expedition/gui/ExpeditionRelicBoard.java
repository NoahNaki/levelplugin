package me.nakilex.levelplugin.guild.expedition.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.guild.expedition.ExpeditionRelicDefinition;
import me.nakilex.levelplugin.guild.expedition.ExpeditionRelicState;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GUI used for managing expedition relic progression. */
public final class ExpeditionRelicBoard {
    public static final String TITLE = ChatColor.DARK_PURPLE + "Guild Expedition Reliquary";
    public static final NamespacedKey KEY_ACTION = new NamespacedKey(Main.getInstance(), "expedition_action");
    public static final String ACTION_INVEST = "invest";
    public static final String ACTION_START = "start";
    public static final String ACTION_MAINTAIN = "maintain";

    private ExpeditionRelicBoard() {}

    public static Inventory create(ExpeditionRelicDefinition target,
                                   ExpeditionRelicDefinition active,
                                   ExpeditionRelicState state) {
        GuiBuilder builder = GuiBuilder.create(54, TITLE)
                .filler(Material.PURPLE_STAINED_GLASS_PANE)
                .border();

        builder.setItem(4, targetItem(target, state));
        builder.setItem(22, contributionItem(state, target));
        builder.setItem(29, investItem(target));
        builder.setItem(31, startItem(target, state));
        builder.setItem(33, maintenanceItem(active != null ? active : target, state));
        builder.setItem(49, activeItem(active, state));
        return builder.build();
    }

    private static ItemStack targetItem(ExpeditionRelicDefinition def, ExpeditionRelicState state) {
        ItemStack stack = new ItemStack(Material.CONDUIT);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + def.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + def.getDescription());
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Required Progress: " + ChatColor.GOLD + def.getProgressRequired());
            lore.add(ChatColor.WHITE + "Current Progress: " + ChatColor.AQUA + state.getProgress());
            lore.add(ChatColor.DARK_GRAY + TooltipUtil.progressBar(state.getProgress(), def.getProgressRequired(), 24));
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Investment Cost: " + ChatColor.YELLOW + def.getInvestmentCost() + " guild coins");
            lore.add(ChatColor.GRAY + "Progress per Invest: " + ChatColor.YELLOW + def.getProgressPerInvestment());
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Requires " + ChatColor.YELLOW + def.getRequiredBuilding()
                    + ChatColor.GRAY + " stage " + ChatColor.YELLOW + def.getRequiredStage());
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack contributionItem(ExpeditionRelicState state, ExpeditionRelicDefinition def) {
        ItemStack stack = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Investment Ledger");
            List<String> lore = new ArrayList<>();
            Map<String, Integer> contributions = state.getContributions();
            if (contributions.isEmpty()) {
                lore.add(ChatColor.GRAY + "No contributions logged yet.");
            } else {
                lore.add(ChatColor.GRAY + "Top contributors:");
                contributions.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(3)
                        .forEach(entry -> lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.YELLOW
                                + name(entry.getKey()) + ChatColor.GRAY + ": " + ChatColor.AQUA + entry.getValue()));
            }
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Guild Rewards: " + ChatColor.YELLOW + "+" + def.getGuildCoinReward()
                    + ChatColor.GRAY + " coins, " + ChatColor.AQUA + "+" + def.getGuildExpReward() + " XP");
            lore.add(ChatColor.GRAY + "Battle Pass: " + ChatColor.AQUA + "+" + def.getBattlePassReward());
            lore.add(" ");
            lore.add(ChatColor.DARK_GRAY + "Unlocked relics: " + ChatColor.LIGHT_PURPLE + state.getUnlockedRelics().size());
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack investItem(ExpeditionRelicDefinition def) {
        ItemStack stack = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Invest " + def.getInvestmentCost() + " Guild Coins");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Spend guild treasury coins to advance the expedition.");
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to invest from the guild treasury", null));
            meta.setLore(lore);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(KEY_ACTION, PersistentDataType.STRING, ACTION_INVEST);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack startItem(ExpeditionRelicDefinition def, ExpeditionRelicState state) {
        ItemStack stack = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Launch Expedition Run");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Layout: " + ChatColor.YELLOW + def.getLayoutKey());
            lore.add(ChatColor.GRAY + "Time Limit: " + ChatColor.YELLOW + def.getTimeLimitMinutes() + "m");
            lore.add(" ");
            if (state.getProgress() < def.getProgressRequired()) {
                lore.add(ChatColor.RED + "Fill the progress bar before launching.");
            } else {
                lore.add(ChatColor.GREEN + "Ready for launch! Gather your party leader and start.");
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to begin the relic expedition", null));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(KEY_ACTION, PersistentDataType.STRING, ACTION_START);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack maintenanceItem(ExpeditionRelicDefinition def, ExpeditionRelicState state) {
        ItemStack stack = new ItemStack(Material.PRISMARINE_CRYSTALS);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Deliver Upkeep Supplies");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Deposit " + ChatColor.GOLD + def.getMaintenanceBundle() + "x "
                    + def.getMaintenanceMaterial().name().toLowerCase() + ChatColor.GRAY + " to extend relic uptime.");
            lore.add(ChatColor.GRAY + "Each bundle adds " + ChatColor.YELLOW + def.getMaintenanceExtensionDays()
                    + ChatColor.GRAY + " day(s).");
            lore.add(" ");
            if (state.getMaintenanceBufferDays() > 0) {
                lore.add(ChatColor.GRAY + "Stored upkeep: " + ChatColor.AQUA + state.getMaintenanceBufferDays() + " day(s)");
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to deposit from your hand", null));
            meta.setLore(lore);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(KEY_ACTION, PersistentDataType.STRING, ACTION_MAINTAIN);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack activeItem(ExpeditionRelicDefinition def, ExpeditionRelicState state) {
        ItemStack stack = new ItemStack(def == null ? Material.BARRIER : Material.END_CRYSTAL);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (def == null) {
                meta.setDisplayName(ChatColor.RED + "No Active Relic");
                meta.setLore(List.of(ChatColor.GRAY + "Complete an expedition to empower your guild."));
            } else {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Active Relic: " + def.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + def.getEffectDescription());
                lore.add(" ");
                long today = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
                long remaining = Math.max(0, state.getActiveRelicExpiryEpochDay() - today);
                lore.add(ChatColor.GRAY + "Duration Remaining: " + ChatColor.AQUA + remaining + " day(s)");
                if (!state.getMaintenanceContributors().isEmpty()) {
                    lore.add(" ");
                    lore.add(ChatColor.GRAY + "Recent upkeep donors:");
                    state.getMaintenanceContributors().entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .limit(3)
                            .forEach(entry -> lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.YELLOW
                                    + name(entry.getKey()) + ChatColor.GRAY + ": " + ChatColor.AQUA + entry.getValue()));
                }
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String name(String uuidString) {
        try {
            UUID id = UUID.fromString(uuidString);
            OfflinePlayer player = Bukkit.getOfflinePlayer(id);
            String name = player.getName();
            return name != null ? name : id.toString().substring(0, 8);
        } catch (IllegalArgumentException ex) {
            return uuidString;
        }
    }
}
