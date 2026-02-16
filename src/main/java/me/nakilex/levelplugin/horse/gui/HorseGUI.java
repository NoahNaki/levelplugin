package me.nakilex.levelplugin.horse.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.horse.data.HorseData;
import me.nakilex.levelplugin.horse.managers.HorseManager;
import me.nakilex.levelplugin.horse.managers.HorseTrailService;
import me.nakilex.levelplugin.quests.def.StableKeeperQuest;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class HorseGUI implements Listener {

    private final HorseManager horseManager;
    private final EconomyManager economyManager;
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();
    private final Map<UUID, BukkitTask> refreshTasks = new HashMap<>();
    private static final int REROLL_COST = 300;

    public HorseGUI(HorseManager horseManager, EconomyManager economyManager) {
        this.horseManager = horseManager;
        this.economyManager = economyManager;
    }

    public void openHorseMenu(Player player) {
        if (!StableKeeperQuest.hasUnlockedHorseMenu(player.getUniqueId())) {
            send(player, MessageType.WARNING,
                    "Complete 'Feathered Famine' with the Stable Keeper to unlock horse rerolls.");
            return;
        }
        UUID playerUUID = player.getUniqueId();
        Inventory gui = GuiBuilder.create(36, "Horse Menu")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        List<GuiWidget> widgets = buildWidgets();
        widgetsByPlayer.put(playerUUID, widgets);
        renderWidgets(gui, player, widgets);

        player.openInventory(gui);
        startAutoUpdate(player, gui);
    }

    private ItemStack createHorseInfoItem(HorseData horseData) {
        if (horseData != null) {
            String speedStars = GuiUtil.glyphStars(Math.min(horseData.getSpeed(), 5));
            String jumpStars = GuiUtil.glyphStars(Math.min(horseData.getJumpHeight(), 5));
            String formattedType = horseData.getType().substring(0, 1).toUpperCase() + horseData.getType().substring(1).toLowerCase();

            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("§7Type: §f" + formattedType);
            lore.add("§7Speed: §6" + speedStars);
            lore.add("§7Jump: §6" + jumpStars);
            lore.add("§7Trail: §d" + horseManager.formatTrailPresetName(horseData.getTrailPreset()));
            return GuiUtil.createGuiItem(Material.BOOK, "§bYour Horse", lore);
        }

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§7You don't own a horse yet!");
        return GuiUtil.createGuiItem(Material.BARRIER, "§cNo Horse Owned", lore);
    }

    private ItemStack createRerollButton(UUID playerUUID) {
        boolean free = StableKeeperQuest.shouldReceiveFreeReroll(playerUUID);
        String costLine = free
                ? "§aCost: §20 (first horse)"
                : "§7Cost: §6<glyph:coins_icon>" + REROLL_COST;
        String reminder = free
                ? "§7The Stable Keeper is covering this one."
                : "§7Click to buy a new horse!";
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§cYour current horse will be deleted.");
        lore.add(" ");
        lore.add(costLine);
        lore.add(" ");
        lore.add(reminder);
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to buy", null));
        return GuiUtil.createGuiItem(Material.SADDLE, "§aBuy a New Horse", lore);
    }

    private ItemStack createTrailButton(UUID playerUUID) {
        HorseData horseData = horseManager.getHorse(playerUUID);
        String current = horseData != null ? horseData.getTrailPreset() : HorseTrailService.OFF_PRESET;
        List<String> options = horseManager.getTrailPresetOptions();

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("§7Current Trail: §b" + horseManager.formatTrailPresetName(current));
        lore.add(" ");
        lore.add("§7Available:");

        int preview = Math.min(5, options.size());
        for (int i = 0; i < preview; i++) {
            String option = options.get(i);
            lore.add(TooltipUtil.selectionLine(option.equalsIgnoreCase(current), horseManager.formatTrailPresetName(option)));
        }
        if (options.size() > preview) {
            lore.add("§8... and " + (options.size() - preview) + " more");
        }

        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cycle trail", "to cycle backwards"));
        return GuiUtil.createGuiItem(Material.BLAZE_POWDER, "§dHorse Trail", lore);
    }

    private void updateHorseInfo(Inventory inventory, UUID playerUUID) {
        List<GuiWidget> widgets = widgetsByPlayer.get(playerUUID);
        if (widgets == null) {
            return;
        }
        renderWidgets(inventory, Bukkit.getPlayer(playerUUID), widgets);
    }

    public void startAutoUpdate(Player player, Inventory inventory) {
        UUID playerUUID = player.getUniqueId();
        stopAutoUpdate(playerUUID);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                Main.getInstance(),
                () -> {
                    if (!player.isOnline()) {
                        stopAutoUpdate(playerUUID);
                        return;
                    }
                    if (player.getOpenInventory().getTopInventory() != inventory) {
                        stopAutoUpdate(playerUUID);
                        return;
                    }
                    updateHorseInfo(inventory, playerUUID);
                },
                0L, 20L
        );
        refreshTasks.put(playerUUID, task);
    }

    private void stopAutoUpdate(UUID playerUUID) {
        BukkitTask task = refreshTasks.remove(playerUUID);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void handleSaddleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!"Horse Menu".equals(event.getView().getTitle())) {
            return;
        }
        if (handleWidgetClick(event, player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handleHorseMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!"Horse Menu".equals(event.getView().getTitle())) {
            return;
        }
        stopAutoUpdate(player.getUniqueId());
    }

    private void handleReroll(Player player, Inventory inventory) {
        UUID playerUUID = player.getUniqueId();

        int rerollCost = getRerollCost(playerUUID);
        if (rerollCost > 0) {
            int playerBalance = economyManager.getBalance(player);
            if (playerBalance < rerollCost) {
                send(player, MessageType.ERROR,
                        "You don't have enough coins to buy a new horse! (Cost: §6" + rerollCost + " <glyph:coins_icon>§c)");
                return;
            }
            economyManager.deductCoins(player, rerollCost);
        }

        horseManager.dismountHorse(player);
        horseManager.rerollHorse(playerUUID);
        updateHorseInfo(inventory, playerUUID);

        var questManager = Main.getInstance().getQuestManager();
        if (questManager != null) {
            questManager.handleBuy(player, StableKeeperQuest.HORSE_BUY_TARGET);
        }

        if (rerollCost == 0) {
            send(player, MessageType.SUCCESS, "You received your first horse on the house!");
        } else {
            send(player, MessageType.SUCCESS,
                    "You bought a new horse for §6" + rerollCost + " <glyph:coins_icon>§a!");
        }
    }

    private void handleTrailCycle(Player player, boolean backwards, Inventory inventory) {
        HorseData horseData = horseManager.getHorse(player.getUniqueId());
        if (horseData == null) {
            return;
        }
        String next = horseManager.cycleTrailPreset(horseData.getTrailPreset(), backwards);
        horseManager.setTrailPreset(player.getUniqueId(), next);
        updateHorseInfo(inventory, player.getUniqueId());
        send(player, MessageType.SUCCESS,
                "Horse trail set to §b" + horseManager.formatTrailPresetName(next) + "§a.");
    }

    private int getRerollCost(UUID playerUUID) {
        return StableKeeperQuest.shouldReceiveFreeReroll(playerUUID) ? 0 : REROLL_COST;
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(11,
                context -> createHorseInfoItem(horseManager.getHorse(context.player().getUniqueId())),
                null));
        widgets.add(new ActionWidget(13,
                context -> createRerollButton(context.player().getUniqueId()),
                (click, context) -> {
                    if (click == org.bukkit.event.inventory.ClickType.LEFT) {
                        handleReroll(context.player(), context.inventory());
                    }
                }));
        widgets.add(new ActionWidget(15,
                context -> createTrailButton(context.player().getUniqueId()),
                (click, context) -> {
                    if (click == org.bukkit.event.inventory.ClickType.LEFT || click == org.bukkit.event.inventory.ClickType.RIGHT) {
                        handleTrailCycle(context.player(), click == org.bukkit.event.inventory.ClickType.RIGHT, context.inventory());
                    }
                }));
        return widgets;
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        if (player == null) {
            return;
        }
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
        for (GuiWidget widget : widgets) {
            if (widget.handlesSlot(slot)) {
                event.setCancelled(true);
                widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
                return true;
            }
        }
        return false;
    }
}
