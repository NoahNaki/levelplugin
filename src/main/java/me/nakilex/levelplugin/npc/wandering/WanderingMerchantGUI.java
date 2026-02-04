package me.nakilex.levelplugin.npc.wandering;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/** Simple GUI displaying random offers from the wandering merchant. */
public class WanderingMerchantGUI implements Listener {

    private final Plugin plugin;
    private final EconomyManager economy;
    private final Inventory inv;
    private final Map<Integer, WanderingMerchantOffer> offers = new HashMap<>();
    private final Set<UUID> viewers = new HashSet<>();
    private final List<GuiWidget> widgets;

    public WanderingMerchantGUI(Plugin plugin, List<WanderingMerchantOffer> list) {
        this.plugin = plugin;
        this.economy = Main.getInstance().getEconomyManager();
        this.inv = GuiBuilder.create(27, "Wandering Merchant")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        for (int i = 0; i < list.size(); i++) {
            WanderingMerchantOffer of = list.get(i);
            offers.put(10 + i, of);
        }
        this.widgets = buildWidgets();
        renderWidgets(inv, null);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Collection<WanderingMerchantOffer> getOffers() { return offers.values(); }

    private ItemStack decorate(WanderingMerchantOffer offer, Player viewer) {
        ItemStack stack = offer.getItem().clone();
        ItemUtil.updateTooltip(stack, viewer);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(" ");
            if (offer.getStock() > 0) {
                lore.add(ChatColor.GOLD + "Price: " + ChatColor.YELLOW + offer.getCost() + " <glyph:coins_icon>");
                lore.add(ChatColor.GRAY + "Stock: " + offer.getStock());
                lore.addAll(TooltipUtil.clickInstructions("to purchase", null));
            } else {
                lore.add(ChatColor.RED + "Out of stock!");
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public void open(Player player) {
        viewers.add(player.getUniqueId());
        renderWidgets(inv, player);
        player.openInventory(inv);
    }

    public void closeAll() {
        for (UUID id : viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.getOpenInventory().getTopInventory().equals(inv)) {
                p.closeInventory();
            }
        }
        viewers.clear();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;
        if (handleWidgetClick(e, (Player) e.getWhoClicked())) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(inv)) {
            viewers.remove(e.getPlayer().getUniqueId());
        }
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        for (Map.Entry<Integer, WanderingMerchantOffer> entry : offers.entrySet()) {
            int slot = entry.getKey();
            WanderingMerchantOffer offer = entry.getValue();
            widgetList.add(new ActionWidget(slot,
                    context -> decorate(offer, context.player()),
                    (click, context) -> handleOfferPurchase(context.player(), offer)));
        }
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
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
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void handleOfferPurchase(Player player, WanderingMerchantOffer offer) {
        if (offer.getStock() <= 0) {
            send(player, MessageType.ERROR, "Out of stock!");
            return;
        }
        if (economy.getBalance(player) < offer.getCost()) {
            send(player, MessageType.ERROR, "Not enough coins!");
            return;
        }
        economy.deductCoins(player, offer.getCost());
        player.getInventory().addItem(offer.getItem());
        offer.decrement();
        renderWidgets(inv, player);
    }
}
