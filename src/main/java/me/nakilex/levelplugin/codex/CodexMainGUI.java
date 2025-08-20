package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Main Codex menu providing category selection. */
public class CodexMainGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Codex";
    private static final int SIZE = 27;

    // base64 textures for category icons
    private static final String LOC_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmVlZjdlNTZjZGU3NDA3NzJkZmI3NmRkZDJmNTg0YmU4OTA3Yjg1OTc2NjhlNDAyNjM0OTg2NDY5MjMwYWE0OSJ9fX0=";
    private static final String NPC_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGU3NTM0NzBlNjdlMzUwZGI2MDVhOTFmNDNhNmYxODJlZmY3NTlkNmI4ZThmNTY0MWVlYjdkNmViYjYxN2JlYyJ9fX0=";
    private static final String MOB_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWM1OGExMDYyMzZjMjM4MGI2MTEzZGY4NDhkZDAxN2I2OWFiYWZmYTQ5M2RhNjkyNzA4MTMyZjBiMjcyMTI3OCJ9fX0=";

    private final MobCodexGUI mobGui;
    private final NpcCodexGUI npcGui;
    private final LocationCodexGUI locGui;

    public CodexMainGUI(MobCodexGUI m, NpcCodexGUI n, LocationCodexGUI l) {
        this.mobGui = m;
        this.npcGui = n;
        this.locGui = l;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .build();
        inv.setItem(11, createHead(LOC_HEAD, ChatColor.GREEN + "Locations"));
        inv.setItem(13, createHead(NPC_HEAD, ChatColor.YELLOW + "NPCs"));
        inv.setItem(15, createHead(MOB_HEAD, ChatColor.RED + "Mobs"));
        player.openInventory(inv);
    }

    private ItemStack createHead(String b64, String name) {
        return HeadUtil.createCustomHead(b64, name, null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot == 11) locGui.open(p);
        else if (slot == 13) npcGui.open(p);
        else if (slot == 15) mobGui.open(p);
    }
}
