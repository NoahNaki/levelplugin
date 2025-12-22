package me.nakilex.levelplugin.potions.gui;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.potions.data.PotionInstance;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class PotionBrowser implements CommandExecutor, Listener {
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    private static final int PAGE_SIZE = 28;

    private final JavaPlugin plugin;
    private final PotionManager potionManager;

    public PotionBrowser(JavaPlugin plugin, PotionManager potionManager) {
        this.plugin = plugin;
        this.potionManager = potionManager;
        plugin.getCommand("potionbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String title(int page) {
        return "Potions - Page " + (page + 1);
    }

    private static ItemStack nexoItem(String id, String name) {
        ItemBuilder b = NexoItems.itemFromId(id);
        if (b == null) return new ItemStack(Material.BARRIER);
        ItemStack it = b.build();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack filler() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(" "); it.setItemMeta(m); }
        return it;
    }

    private void open(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, SIZE, title(page));
        ItemStack fill = filler();
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, fill);
        }

        List<PotionTemplate> templates = new ArrayList<>(potionManager.getAllTemplates());
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= templates.size()) break;
            PotionTemplate tpl = templates.get(idx);
            PotionInstance inst = potionManager.createInstance(tpl);
            ItemStack stack = inst.toItemStack(plugin);
            // mark with uuid for retrieval
            stack.getItemMeta().getPersistentDataContainer()
                .set(new org.bukkit.NamespacedKey(plugin, "potion_uuid"), PersistentDataType.STRING, inst.getUuid().toString());
            int row = 1 + (i / 7);
            int col = 1 + (i % 7);
            gui.setItem(row * COLS + col, stack);
        }

        if (page > 0) gui.setItem(SIZE - COLS, nexoItem("arrow_left", ChatColor.GREEN + "Previous Page"));
        if (templates.size() > (page + 1) * PAGE_SIZE) gui.setItem(SIZE - 1, nexoItem("arrow_right", ChatColor.GREEN + "Next Page"));

        player.openInventory(gui);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            send(sender, MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        open((Player) sender, 0);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().getTitle().startsWith(ChatColor.BLACK + "Potions")) return;
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();
        String stripped = ChatColor.stripColor(e.getView().getTitle());
        int page = Integer.parseInt(stripped.split(" ")[stripped.split(" ").length -1]) -1;

        if (name.equals(ChatColor.GREEN + "Next Page")) {
            open(player, page + 1);
            return;
        }
        if (name.equals(ChatColor.GREEN + "Previous Page")) {
            if (page > 0) open(player, page -1);
            return;
        }

        // give potion
        PotionInstance inst = potionManager.getInstanceFromItem(clicked);
        if (inst != null) {
            player.getInventory().addItem(clicked.clone());
            send(player, MessageType.SUCCESS, "You received: " + ChatColor.WHITE + name);
        }
    }
}
