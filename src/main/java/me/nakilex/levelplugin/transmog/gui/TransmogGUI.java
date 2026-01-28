package me.nakilex.levelplugin.transmog.gui;

import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.transmog.TransmogManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/** GUI allowing players to apply unlocked transmogs to an item. */
public class TransmogGUI implements CommandExecutor, Listener {
    private static final String TITLE = "Transmog";
    private static final int ITEM_SLOT = 11;
    private static final int MODEL_SLOT = 15;
    private static final int CONFIRM_SLOT = 22;

    private final JavaPlugin plugin;
    private final TransmogManager manager;
    private final TransmogBrowser browser;
    private final Map<UUID, String> selectedModel = new HashMap<>();
    /** inventories waiting for a model selection */
    private final Map<UUID, Inventory> pending = new HashMap<>();
    /** players who opened the model browser */
    private final Set<UUID> browsing = new HashSet<>();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public TransmogGUI(JavaPlugin plugin, TransmogManager manager, TransmogBrowser browser) {
        this.plugin = plugin;
        this.manager = manager;
        this.browser = browser;
        this.browser.setGui(this);
        plugin.getCommand("transmog").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void open(Player player) {
        Inventory inv = GuiBuilder.create(27, TITLE)
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildWidgets();
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
        inv.setItem(ITEM_SLOT, null);
        player.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        open(p);
        return true;
    }

    private void updateModelSlot(Inventory inv, String modelId) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to change model", null));
        ItemStack it = GuiUtil.getNexoItem(modelId, " ", lore);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            it.setItemMeta(meta);
        }
        inv.setItem(MODEL_SLOT, it);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        Inventory inv = e.getView().getTopInventory();
        int raw = e.getRawSlot();
        if (raw >= inv.getSize()) {
            return; // allow interaction with the player's inventory
        }
        if (raw == ITEM_SLOT) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                boolean w = WeaponType.matchType(cursor) != null;
                boolean a = ArmorType.matchType(cursor) != null;
                if (!w && !a) {
                    e.setCancelled(true);
                }
            }
            return;
        }
        Player p = (Player) e.getWhoClicked();
        if (handleWidgetClick(e, p)) {
            return;
        }
        e.setCancelled(true);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(MODEL_SLOT,
                context -> GuiUtil.getNexoItem("book", ChatColor.YELLOW + "Select Skin"),
                (click, context) -> handleModelClick(context.player(), context.inventory())));
        widgets.add(new ActionWidget(CONFIRM_SLOT,
                context -> GuiUtil.getNexoItem("check", ChatColor.GREEN + "Apply"),
                (click, context) -> handleConfirmClick(context.player(), context.inventory())));
        return widgets;
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
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

    private void handleModelClick(Player player, Inventory inv) {
        ItemStack item = inv.getItem(ITEM_SLOT);
        if (item == null) return;
        WeaponType wType = WeaponType.matchType(item);
        ArmorType aType = ArmorType.matchType(item);
        if (wType == null && aType == null) return;
        browsing.add(player.getUniqueId());
        browser.openSelector(player, wType, aType, id -> {
            UUID uid = player.getUniqueId();
            Inventory back = pending.remove(uid);
            if (back != null) {
                selectedModel.put(uid, id);
                updateModelSlot(back, id);
                browsing.remove(uid);
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(back));
            }
        });
    }

    private void handleConfirmClick(Player player, Inventory inv) {
        ItemStack item = inv.getItem(ITEM_SLOT);
        String id = selectedModel.get(player.getUniqueId());
        if (item == null || id == null) {
            return;
        }
        WeaponType itemWeapon = WeaponType.matchType(item);
        ArmorType itemArmor = ArmorType.matchType(item);
        WeaponType modelWeapon = manager.getWeaponType(id);
        ArmorType modelArmor = manager.getArmorType(id);
        if ((itemWeapon != null && itemWeapon != modelWeapon) ||
                (itemArmor != null && itemArmor != modelArmor)) {
            player.sendMessage(ChatColor.RED + "That skin can't be applied to this item.");
            return;
        }
        ItemUtil.applyNexoModel(item, id);
        selectedModel.remove(player.getUniqueId());
        player.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        UUID id = e.getPlayer().getUniqueId();
        Inventory inv = e.getInventory();
        if (browsing.contains(id)) {
            pending.put(id, inv);
            return; // wait for browser selection
        }
        ItemStack item = inv.getItem(ITEM_SLOT);
        if (item != null) {
            e.getPlayer().getInventory().addItem(item);
        }
        selectedModel.remove(id);
        pending.remove(id);
        browsing.remove(id);
        widgetsByPlayer.remove(id);
    }

    /** Return the pending item if the player closed the browser without choosing. */
    public void cancelSelection(Player p) {
        UUID id = p.getUniqueId();
        Inventory inv = pending.remove(id);
        if (inv != null) {
            ItemStack item = inv.getItem(ITEM_SLOT);
            if (item != null) {
                p.getInventory().addItem(item);
            }
        }
        browsing.remove(id);
        selectedModel.remove(id);
    }
}
