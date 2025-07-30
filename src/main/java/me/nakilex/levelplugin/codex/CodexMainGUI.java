package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Main menu for the discovery codex with category shortcuts. */
public class CodexMainGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_GREEN + "Codex";
    private static final int SIZE = 27;

    private final ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
    private final ItemStack locationsIcon;
    private final ItemStack npcIcon;
    private final ItemStack mobIcon;

    private final LocationCodexGUI locationGUI;
    private final NPCCodexGUI npcGUI;
    private final CodexGUI mobGUI;

    public CodexMainGUI(LocationCodexGUI locationGUI, NPCCodexGUI npcGUI, CodexGUI mobGUI) {
        this.locationGUI = locationGUI;
        this.npcGUI = npcGUI;
        this.mobGUI = mobGUI;
        this.locationsIcon = HeadUtil.createCustomHead(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmVlZjdlNTZjZGU3NDA3NzJkZmI3NmRkZDJmNTg0YmU4OTA3Yjg1OTc2NjhlNDAyNjM0OTg2NDY5MjMwYWE0OSJ9fX0=",
                ChatColor.YELLOW + "Locations", null);
        this.npcIcon = HeadUtil.createCustomHead(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGU3NTM0NzBlNjdlMzUwZGI2MDVhOTFmNDNhNmYxODJlZmY3NTlkNmI4ZThmNTY0MWVlYjdkNmViYjYxN2JlYyJ9fX0=",
                ChatColor.YELLOW + "NPCs", null);
        this.mobIcon = HeadUtil.createCustomHead(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWM1OGExMDYyMzZjMjM4MGI2MTEzZGY4NDhkZDAxN2I2OWFiYWZmYTQ5M2RhNjkyNzA4MTMyZjBiMjcyMTI3OCJ9fX0=",
                ChatColor.YELLOW + "Mobs", null);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        GuiUtil.fillBorder(inv, filler);
        inv.setItem(11, locationsIcon);
        inv.setItem(13, npcIcon);
        inv.setItem(15, mobIcon);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        String name = ChatColor.stripColor(meta.getDisplayName());
        Player player = (Player) e.getWhoClicked();
        switch (name) {
            case "Locations" -> locationGUI.open(player);
            case "NPCs" -> npcGUI.open(player);
            case "Mobs" -> mobGUI.open(player);
            default -> {}
        }
    }
}
