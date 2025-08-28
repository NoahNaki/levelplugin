package me.nakilex.levelplugin.transmog.gui;

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

import java.util.*;
import java.util.function.Consumer;

/** Simple paginated browser for unlocked transmogs. */
public class TransmogBrowser implements CommandExecutor, Listener {
    private static final int PAGE_SIZE = GuiUtil.PAGED_SLOTS.length;
    private final JavaPlugin plugin;
    private final TransmogManager manager;
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, Consumer<String>> callbacks = new HashMap<>();
    private final Map<UUID, Boolean> weaponView = new HashMap<>();

    public TransmogBrowser(JavaPlugin plugin, TransmogManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        plugin.getCommand("transmogbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String title(int page) {
        return ChatColor.BLACK + "Transmogs - Page " + (page + 1);
    }

    public void openSelector(Player player, boolean weapon, Consumer<String> cb) {
        callbacks.put(player.getUniqueId(), cb);
        open(player, weapon, pages.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, boolean weapon, int page) {
        weaponView.put(player.getUniqueId(), weapon);
        pages.put(player.getUniqueId(), page);
        List<String> models = new ArrayList<>(manager.getKnownModels(weapon));
        Collections.sort(models);
        Inventory inv = Bukkit.createInventory(null, 54, title(page));
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        GuiUtil.fillBorder(inv, filler);
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < models.size(); i++) {
            String id = models.get(start + i);
            boolean unlocked = manager.isUnlocked(player.getUniqueId(), id);
            ItemStack it;
            if (unlocked) {
                com.nexomc.nexo.items.ItemBuilder b = com.nexomc.nexo.api.NexoItems.itemFromId(id);
                it = b != null ? b.build() : new ItemStack(Material.BARRIER);
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(" ");
                    meta.setLore(null);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                    it.setItemMeta(meta);
                }
            } else {
                it = new ItemStack(Material.LIGHT_GRAY_DYE);
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GRAY + "???");
                    it.setItemMeta(meta);
                }
            }
            inv.setItem(GuiUtil.PAGED_SLOTS[i], it);
        }
        if (page > 0) inv.setItem(45, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (models.size() > (page + 1) * PAGE_SIZE)
            inv.setItem(53, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        player.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        open(p, true, 0); // default to weapon view for commands
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith(ChatColor.BLACK + "Transmogs")) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int page = pages.getOrDefault(p.getUniqueId(), 0);
        boolean weapon = weaponView.getOrDefault(p.getUniqueId(), true);
        List<String> models = new ArrayList<>(manager.getKnownModels(weapon));
        Collections.sort(models);
        int slot = e.getRawSlot();
        if (slot == 45 && page > 0) {
            open(p, weapon, page - 1);
            return;
        }
        if (slot == 53 && models.size() > (page + 1) * PAGE_SIZE) {
            open(p, weapon, page + 1);
            return;
        }
        for (int i = 0; i < GuiUtil.PAGED_SLOTS.length; i++) {
            if (GuiUtil.PAGED_SLOTS[i] == slot) {
                int idx = page * PAGE_SIZE + i;
                if (idx >= models.size()) return;
                String id = models.get(idx);
                if (!manager.isUnlocked(p.getUniqueId(), id)) return;
                Consumer<String> cb = callbacks.remove(p.getUniqueId());
                weaponView.remove(p.getUniqueId());
                if (cb != null) cb.accept(id);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().startsWith(ChatColor.BLACK + "Transmogs")) {
            callbacks.remove(e.getPlayer().getUniqueId());
            weaponView.remove(e.getPlayer().getUniqueId());
        }
    }
}
