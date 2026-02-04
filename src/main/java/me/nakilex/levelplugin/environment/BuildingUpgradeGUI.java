package me.nakilex.levelplugin.environment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.utils.gui.widgets.NexoButtonWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** GUI for investing materials into a specific building upgrade. */
public class BuildingUpgradeGUI implements Listener {
    private static final String TITLE_PREFIX = "Upgrade ";
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;
    private final EnvironmentManager manager;
    private final List<GuiWidget> widgets;
    private final Map<UUID, String> openBuildings = new HashMap<>();
    public BuildingUpgradeGUI(EnvironmentManager manager) {
        this.manager = manager;
        this.widgets = buildWidgets();
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new NexoButtonWidget(
                CONFIRM_SLOT,
                "check",
                ChatColor.GREEN + "Confirm",
                context -> buildUpgradeLore(context.player(), getBuilding(context)),
                (click, context) -> {
                    String building = getBuilding(context);
                    if (building != null) {
                        manager.attemptUpgradeBuilding(context.player(), building);
                    }
                    context.player().closeInventory();
                }));
        widgetList.add(new NexoButtonWidget(
                CANCEL_SLOT,
                "cross",
                ChatColor.RED + "Cancel",
                null,
                (click, context) -> context.player().closeInventory()));
        return widgetList;
    }

    public void open(Player p, String building) {
        openBuildings.put(p.getUniqueId(), building);
        String nice = me.nakilex.levelplugin.utils.TextUtil.beautifyWords(building.replace('_', ' '));
        Inventory inv = GuiBuilder.create(27, TITLE_PREFIX + nice)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        renderWidgets(inv, p);
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) {
            return;
        }
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getView().getTopInventory().getSize()) {
            return;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
        widget.onClick(slot, e.getClick(), new GuiContext(player, e.getView().getTopInventory()));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith(TITLE_PREFIX)) {
            openBuildings.remove(e.getPlayer().getUniqueId());
        }
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private String getBuilding(GuiContext context) {
        String building = openBuildings.get(context.player().getUniqueId());
        if (building != null) {
            return building;
        }
        String title = context.player().getOpenInventory().getTitle();
        if (title.startsWith(TITLE_PREFIX)) {
            return title.substring(TITLE_PREFIX.length()).toLowerCase().replace(' ', '_');
        }
        return null;
    }

    private List<String> buildUpgradeLore(Player player, String building) {
        if (building == null) {
            return List.of();
        }
        int stage = manager.getPlayerBuildingStage(player, building);
        var nextData = manager.getBuildingStageManager().getStage(building, stage + 1);
        List<String> lore = new ArrayList<>();
        if (nextData == null) {
            return lore;
        }
        lore.add(ChatColor.GRAY + "Upgrade cost:");
        Guild guild = GuildManager.getInstance().getGuild(player.getUniqueId());
        double discount = guild != null ? guild.getUpgradeDiscount() : 0.0;
        int coins = me.nakilex.levelplugin.Main.getInstance().getEconomyManager().getBalance(player);
        for (var entry : nextData.materialCost.entrySet()) {
            Material material = entry.getKey();
            int baseAmount = entry.getValue();
            int needed = (int) Math.round(baseAmount * (1.0 - discount));
            boolean has = player.getInventory().containsAtLeast(new ItemStack(material, needed), needed);
            String matName = me.nakilex.levelplugin.utils.TextUtil.beautifyWords(material.name().toLowerCase().replace('_', ' '));
            String prefix = has ? ChatColor.GREEN + "\u2714" : ChatColor.RED + "\u2718";
            String amountText = needed < baseAmount
                    ? ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + baseAmount + ChatColor.RESET + ChatColor.GRAY + " -> " + ChatColor.WHITE + needed
                    : ChatColor.WHITE + "" + baseAmount;
            String line = prefix + ChatColor.GRAY + " - "
                    + amountText + ChatColor.DARK_GRAY + "x "
                    + ChatColor.WHITE + matName;
            lore.add(line);
        }
        int coinCost = nextData.coinCost;
        int discounted = (int) Math.round(coinCost * (1.0 - discount));
        boolean hasCoins = coins >= discounted;
        String prefix = hasCoins ? ChatColor.GREEN + "\u2714" : ChatColor.RED + "\u2718";
        String costText;
        if (discounted < coinCost) {
            costText = ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + coinCost + ChatColor.RESET + ChatColor.GRAY + " -> " + ChatColor.WHITE + discounted;
        } else {
            costText = ChatColor.WHITE + "" + coinCost;
        }
        String coinLine = prefix + ChatColor.GRAY + " - "
                + costText + " coins "
                + ChatColor.GOLD + " <glyph:coins_icon>";
        lore.add(coinLine);
        return lore;
    }
}
