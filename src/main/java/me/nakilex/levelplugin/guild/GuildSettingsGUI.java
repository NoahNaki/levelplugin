package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
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

import java.util.*;

public class GuildSettingsGUI implements Listener {
    private final GuildManager manager;
    private final Map<UUID, Integer> permIndex = new HashMap<>();
    private final Map<UUID, Integer> roleIndex = new HashMap<>();
    private static final int SIZE = 27;
    private static final String TITLE = ChatColor.BLACK + "Guild Settings";
    private static final int ITEM_SLOT = 13;

    public GuildSettingsGUI(GuildManager manager) {
        this.manager = manager;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void open(Player player) {
        Guild g = manager.getGuild(player.getUniqueId());
        if (g == null) return;
        int pIdx = permIndex.getOrDefault(player.getUniqueId(), 0);
        int rIdx = roleIndex.getOrDefault(player.getUniqueId(), 0);
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 18) inv.setItem(i, filler);
        }
        ItemStack item = buildItem(g, GuildPermission.values()[pIdx], rIdx);
        inv.setItem(ITEM_SLOT, item);
        player.openInventory(inv);
    }

    private ItemStack buildItem(Guild g, GuildPermission perm, int roleIdx) {
        ItemStack it = GuiUtil.getNexoItem("gear", ChatColor.YELLOW + perm.name().toLowerCase().replace('_', ' '));
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            GuildRole[] roles = GuildRole.values();
            for (int i = 0; i < roles.length; i++) {
                GuildRole r = roles[i];
                boolean allowed = g.getPermissions(r).has(perm);
                ChatColor c = i == roleIdx ? ChatColor.YELLOW : ChatColor.GRAY;
                lore.add(c + r.name() + ": " + (allowed ? ChatColor.GREEN + "✔" : ChatColor.RED + "✖"));
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Left-click: next permission");
            lore.add(ChatColor.WHITE + "Right-click: toggle role");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!ChatColor.stripColor(e.getView().getTitle()).equals(ChatColor.stripColor(TITLE))) return;
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        Guild g = manager.getGuild(player.getUniqueId());
        if (g == null) return;
        int pIdx = permIndex.getOrDefault(player.getUniqueId(), 0);
        int rIdx = roleIndex.getOrDefault(player.getUniqueId(), 0);
        if (e.getRawSlot() == ITEM_SLOT) {
            if (e.isLeftClick()) {
                pIdx = (pIdx + 1) % GuildPermission.values().length;
                rIdx = 0;
            } else if (e.isRightClick()) {
                GuildPermission perm = GuildPermission.values()[pIdx];
                GuildRole role = GuildRole.values()[rIdx];
                manager.setPermission(player.getUniqueId(), role, perm, !g.getPermissions(role).has(perm));
                rIdx = (rIdx + 1) % GuildRole.values().length;
            }
            permIndex.put(player.getUniqueId(), pIdx);
            roleIndex.put(player.getUniqueId(), rIdx);
            open(player);
        }
    }
}
