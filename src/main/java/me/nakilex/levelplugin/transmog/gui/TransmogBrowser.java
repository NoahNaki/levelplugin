package me.nakilex.levelplugin.transmog.gui;

import me.nakilex.levelplugin.transmog.TransmogManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.items.data.WeaponType;
import me.nakilex.levelplugin.items.data.ArmorType;
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
    private TransmogGUI gui; // optional back-reference for returning items on cancel
    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, Consumer<String>> callbacks = new HashMap<>();
    private final Map<UUID, Boolean> weaponView = new HashMap<>();
    private final Map<UUID, WeaponType> weaponFilter = new HashMap<>();
    private final Map<UUID, ArmorType> armorFilter = new HashMap<>();

    public TransmogBrowser(JavaPlugin plugin, TransmogManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        plugin.getCommand("transmogbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Provide a reference to the Transmog GUI so we can return held items if
     * the player closes the browser without making a selection.
     */
    public void setGui(TransmogGUI gui) {
        this.gui = gui;
    }

    private String title(int page) {
        return ChatColor.BLACK + "Transmogs - Page " + (page + 1);
    }

    public void openSelector(Player player, WeaponType wType, ArmorType aType, Consumer<String> cb) {
        callbacks.put(player.getUniqueId(), cb);
        if (wType != null) {
            weaponFilter.put(player.getUniqueId(), wType);
            weaponView.put(player.getUniqueId(), true);
            open(player, true, pages.getOrDefault(player.getUniqueId(), 0));
        } else {
            armorFilter.put(player.getUniqueId(), aType);
            weaponView.put(player.getUniqueId(), false);
            open(player, false, pages.getOrDefault(player.getUniqueId(), 0));
        }
    }

    private void open(Player player, boolean weapon, int page) {
        weaponView.put(player.getUniqueId(), weapon);
        pages.put(player.getUniqueId(), page);
        List<String> models = new ArrayList<>(manager.getKnownModels(weapon));
        if (weapon) {
            WeaponType filter = weaponFilter.get(player.getUniqueId());
            if (filter != null) {
                models.removeIf(id -> manager.getWeaponType(id) != filter);
            }
        } else {
            ArmorType filter = armorFilter.get(player.getUniqueId());
            if (filter != null) {
                models.removeIf(id -> manager.getArmorType(id) != filter);
            }
        }
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
        if (weapon) {
            WeaponType filter = weaponFilter.get(p.getUniqueId());
            if (filter != null) {
                models.removeIf(id -> manager.getWeaponType(id) != filter);
            }
        } else {
            ArmorType filter = armorFilter.get(p.getUniqueId());
            if (filter != null) {
                models.removeIf(id -> manager.getArmorType(id) != filter);
            }
        }
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
                weaponFilter.remove(p.getUniqueId());
                armorFilter.remove(p.getUniqueId());
                if (cb != null) cb.accept(id);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().startsWith(ChatColor.BLACK + "Transmogs")) {
            UUID id = e.getPlayer().getUniqueId();
            boolean pending = callbacks.remove(id) != null;
            weaponView.remove(id);
            weaponFilter.remove(id);
            armorFilter.remove(id);
            // If the browser closed while awaiting a callback, return the item
            // the player had placed in the transmog GUI.
            if (pending && gui != null && e.getPlayer() instanceof Player p) {
                gui.cancelSelection(p);
            }
        }
    }
}
