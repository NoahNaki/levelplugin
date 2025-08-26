package me.nakilex.levelplugin.transmog.gui;

import me.nakilex.levelplugin.items.data.ArmorType;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.transmog.TransmogManager;
import me.nakilex.levelplugin.utils.GuiUtil;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** GUI allowing players to apply unlocked transmogs to an item. */
public class TransmogGUI implements CommandExecutor, Listener {
    private static final String TITLE = ChatColor.BLACK + "Transmog";
    private static final int ITEM_SLOT = 11;
    private static final int MODEL_SLOT = 15;
    private static final int CONFIRM_SLOT = 22;

    private final JavaPlugin plugin;
    private final TransmogManager manager;
    private final TransmogBrowser browser;
    private final Map<UUID, String> selectedModel = new HashMap<>();

    public TransmogGUI(JavaPlugin plugin, TransmogManager manager, TransmogBrowser browser) {
        this.plugin = plugin;
        this.manager = manager;
        this.browser = browser;
        plugin.getCommand("transmog").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.BLACK_STAINED_GLASS_PANE);
        GuiUtil.fillBorder(inv, filler);
        inv.setItem(MODEL_SLOT, GuiUtil.getNexoItem("book", ChatColor.YELLOW + "Select Skin"));
        inv.setItem(CONFIRM_SLOT, GuiUtil.getNexoItem("check", ChatColor.GREEN + "Apply"));
        player.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        open(p);
        return true;
    }

    private void updateModelSlot(Inventory inv, String modelId) {
        com.nexomc.nexo.items.ItemBuilder b = com.nexomc.nexo.api.NexoItems.itemFromId(modelId);
        ItemStack it = b != null ? b.build() : new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setLore(java.util.List.of(ChatColor.GRAY + "Left-click to change model"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            it.setItemMeta(meta);
        }
        inv.setItem(MODEL_SLOT, it);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        int raw = e.getRawSlot();
        if (raw == ITEM_SLOT) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                boolean w = WeaponType.matchType(cursor) != null;
                boolean a = ArmorType.matchType(cursor) != null;
                if (!w && !a) e.setCancelled(true);
            }
            return;
        }
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        if (raw == MODEL_SLOT) {
            ItemStack item = inv.getItem(ITEM_SLOT);
            if (item == null) return;
            boolean weapon = WeaponType.matchType(item) != null;
            boolean armor = ArmorType.matchType(item) != null;
            if (!weapon && !armor) return;
            browser.openSelector(p, weapon, id -> {
                selectedModel.put(p.getUniqueId(), id);
                updateModelSlot(inv, id);
            });
        } else if (raw == CONFIRM_SLOT) {
            ItemStack item = inv.getItem(ITEM_SLOT);
            String id = selectedModel.get(p.getUniqueId());
            if (item != null && id != null) {
                ItemUtil.applyNexoModel(item, id);
                p.getInventory().addItem(item);
                inv.setItem(ITEM_SLOT, null);
                inv.setItem(MODEL_SLOT, GuiUtil.getNexoItem("book", ChatColor.YELLOW + "Select Skin"));
                selectedModel.remove(p.getUniqueId());
                p.closeInventory();
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        Inventory inv = e.getInventory();
        ItemStack item = inv.getItem(ITEM_SLOT);
        if (item != null) {
            e.getPlayer().getInventory().addItem(item);
        }
        selectedModel.remove(e.getPlayer().getUniqueId());
    }
}
