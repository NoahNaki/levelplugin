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

import java.util.*;

public class GuildSettingsGUI implements Listener {
    private final GuildManager manager;
    private GuildMemberGUI memberGUI;
    private final Map<UUID, Integer> roleIndex = new HashMap<>();
    private static final int SIZE = 45;
    private static final String TITLE = ChatColor.BLACK + "Guild Settings";
    private static final int BACK_SLOT = 0;
    private static final int[] PERM_SLOTS = {10,11,12,13,14};

    public GuildSettingsGUI(GuildManager manager) {
        this.manager = manager;
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public void setMemberGUI(GuildMemberGUI memberGUI) {
        this.memberGUI = memberGUI;
    }

    public void open(Player player) {
        Guild g = manager.getGuild(player.getUniqueId());
        if (g == null) return;
        int rIdx = roleIndex.getOrDefault(player.getUniqueId(), 0);
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left2", ChatColor.GRAY + "Back"));
        GuildPermission[] perms = GuildPermission.values();
        for (int i = 0; i < perms.length && i < PERM_SLOTS.length; i++) {
            inv.setItem(PERM_SLOTS[i], buildItem(g, perms[i], rIdx));
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
        player.openInventory(inv);
    }

    private ItemStack buildItem(Guild g, GuildPermission perm, int roleIdx) {
        GuildRole role = GuildRole.values()[roleIdx];
        boolean allowed = g.getPermissions(role).has(perm);
        return GuiUtil.createToggleItem(allowed,
                ChatColor.AQUA + perm.name().toLowerCase().replace('_', ' '),
                ChatColor.GRAY + "Role: " + ChatColor.WHITE + role.name(),
                "",
                ChatColor.YELLOW + "Left-click to cycle role",
                ChatColor.YELLOW + "Right-click to toggle");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!ChatColor.stripColor(e.getView().getTitle()).equals(ChatColor.stripColor(TITLE))) return;
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        Guild g = manager.getGuild(player.getUniqueId());
        if (g == null) return;
        int rIdx = roleIndex.getOrDefault(player.getUniqueId(), 0);
        int slot = e.getRawSlot();
        if (slot == BACK_SLOT) {
            if (memberGUI != null) {
                memberGUI.open(player);
            }
            return;
        }
        GuildPermission[] perms = GuildPermission.values();
        for (int i = 0; i < perms.length && i < PERM_SLOTS.length; i++) {
            if (slot == PERM_SLOTS[i]) {
                if (e.isLeftClick()) {
                    rIdx = (rIdx + 1) % GuildRole.values().length;
                    roleIndex.put(player.getUniqueId(), rIdx);
                } else if (e.isRightClick()) {
                    GuildRole role = GuildRole.values()[rIdx];
                    GuildPermission perm = perms[i];
                    manager.setPermission(player.getUniqueId(), role, perm, !g.getPermissions(role).has(perm));
                }
                open(player);
                return;
            }
        }
    }
}
