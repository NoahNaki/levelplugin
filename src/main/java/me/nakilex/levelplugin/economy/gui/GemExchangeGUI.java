package me.nakilex.levelplugin.economy.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class GemExchangeGUI implements Listener {
    private static final String TITLE = "Gem Exchange";
    private static final Material FRAGMENT = Material.MEDIUM_AMETHYST_BUD;
    private static final Material SHARD    = Material.AMETHYST_SHARD;
    private static final Material CLUSTER  = Material.AMETHYST_CLUSTER;
    private static final int FRAGMENT_TO_SHARD_SLOT = 10;
    private static final int SHARD_TO_FRAGMENT_SLOT = 12;
    private static final int SHARD_TO_CLUSTER_SLOT = 14;
    private static final int CLUSTER_TO_SHARD_SLOT = 16;

    private final Main plugin;
    private final Inventory gui;
    private final GemsManager gemsManager;
    private final List<GuiWidget> widgets;

    public GemExchangeGUI(Main plugin, GemsManager gemsManager) {
        this.plugin = plugin;
        this.gemsManager  = gemsManager;
        this.gui = GuiBuilder.create(27, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        this.widgets = buildWidgets();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        widgetList.add(new ActionWidget(FRAGMENT_TO_SHARD_SLOT,
                context -> createExchangeItem(FRAGMENT, "64 Fragments → 1 Shard",
                        "Combine 64 fragments into 1 shard"),
                (click, context) -> handleConvert(context.player(), FRAGMENT, 64, SHARD, 1)));
        widgetList.add(new ActionWidget(SHARD_TO_FRAGMENT_SLOT,
                context -> createExchangeItem(SHARD, "1 Shard → 64 Fragments",
                        "Break 1 shard into 64 fragments"),
                (click, context) -> handleConvert(context.player(), SHARD, 1, FRAGMENT, 64)));
        widgetList.add(new ActionWidget(SHARD_TO_CLUSTER_SLOT,
                context -> createExchangeItem(SHARD, "64 Shards → 1 Cluster",
                        "Combine 64 shards into 1 cluster"),
                (click, context) -> handleConvert(context.player(), SHARD, 64, CLUSTER, 1)));
        widgetList.add(new ActionWidget(CLUSTER_TO_SHARD_SLOT,
                context -> createExchangeItem(CLUSTER, "1 Cluster → 64 Shards",
                        "Break 1 cluster into 64 shards"),
                (click, context) -> handleConvert(context.player(), CLUSTER, 1, SHARD, 64)));
        return widgetList;
    }

    public void open(Player player) {
        renderWidgets(player);
        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent ev) {
        if (!GuiUtil.titleMatches(ev.getView().getTitle(), TITLE)) return;
        ev.setCancelled(true);
        if (!(ev.getWhoClicked() instanceof Player player)) return;
        handleWidgetClick(ev, player);
    }

    private void handleConvert(Player p,
                               Material fromMat, int fromAmt,
                               Material toMat,   int toAmt) {
        @SuppressWarnings("unchecked")
        Map<Integer, ItemStack> map = (Map<Integer, ItemStack>) p.getInventory().all(fromMat);
        int total = map.values().stream().mapToInt(ItemStack::getAmount).sum();
        if (total < fromAmt) {
            send(p, MessageType.ERROR,
                    "Not enough " + fromMat.name().toLowerCase().replace('_',' ') + "! Need " + fromAmt + ".");
            return;
        }
        int rem = fromAmt;
        int unitValue;
        if (toMat == Material.MEDIUM_AMETHYST_BUD)      unitValue = 1;
        else if (toMat == Material.AMETHYST_SHARD)      unitValue = 64;
        else /* AMETHYST_CLUSTER */                     unitValue = 4096;

        ItemStack pretty = gemsManager.createCurrencyItem(toMat, toAmt, unitValue);
        if (!gemsManager.canFit(p, pretty)) {
            send(p, MessageType.ERROR, "You need at least one free inventory slot.");
            return;
        }

        for (var entry : map.entrySet()) {
            ItemStack stack = entry.getValue();
            int amt = stack.getAmount();

            if (amt <= rem) {
                p.getInventory().clear(entry.getKey());
                rem -= amt;
            } else {
                stack.setAmount(amt - rem);
                rem = 0;
            }

            if (rem == 0) break;
        }

        Map<Integer, ItemStack> overflow = p.getInventory().addItem(pretty);
        if (!overflow.isEmpty()) {
            overflow.values().forEach(item -> p.getWorld().dropItemNaturally(p.getLocation(), item));
        }
        send(p, MessageType.SUCCESS,
                "Converted " + fromAmt + " " + fromMat.name().toLowerCase().replace('_',' ') + " into " + toAmt + " " + toMat.name().toLowerCase().replace('_',' ') + "!");

        new BukkitRunnable() {
            @Override
            public void run() {
                open(p);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void renderWidgets(Player player) {
        GuiLayout layout = new GuiLayout(gui);
        GuiContext context = new GuiContext(player, gui);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private ItemStack createExchangeItem(Material material, String name, String description) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.addAll(TooltipUtil.clickInstructions("to convert", null));
        return GuiUtil.createGuiItem(material, ChatColor.LIGHT_PURPLE + name, lore);
    }

    
}
