package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** GUI listing NPC discoveries with player head icons. */
public class NpcCodexGUI implements Listener {
    private static final String TITLE = ChatColor.BLACK + "Codex - NPCs";
    private static final int SIZE = 54;
    private static final int ITEMS_PER_PAGE = CodexGuiUtil.CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int BACK_SLOT = 49;

    private final CodexManager manager;
    private CodexMainGUI mainGui;
    private final Map<UUID, Integer> pageMap = new HashMap<>();

    public NpcCodexGUI(CodexManager manager, CodexMainGUI mainGui) {
        this.manager = manager;
        this.mainGui = mainGui;
    }

    public void setMainGui(CodexMainGUI gui) { this.mainGui = gui; }

    public void open(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, page);
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        UUID id = player.getUniqueId();
        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("NPCs", manager.getDiscoveredNpcCount(id) + "/" + manager.getTotalNpcCount());
        inv.setItem(4, CodexGuiUtil.createInfoBook("Discoveries", lines));

        List<NPC> npcs = manager.getAllNpcs();
        Set<String> discovered = new HashSet<>(manager.getDiscoveredNpcs(id));
        int start = page * ITEMS_PER_PAGE;
        for (int i = start, slot = 0; i < npcs.size() && slot < ITEMS_PER_PAGE; i++) {
            inv.setItem(CodexGuiUtil.CONTENT_SLOTS[slot++],
                    createNpcIcon(id, discovered, npcs.get(i)));
        }

        if (page > 0) inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (npcs.size() > (page + 1) * ITEMS_PER_PAGE) inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));

        player.openInventory(inv);
    }

    private ItemStack createNpcIcon(UUID id, Set<String> discovered, NPC npc) {
        String rawName = ChatColor.stripColor(npc.getName());
        String key = rawName.toLowerCase();
        boolean has = discovered.contains(key);
        if (has) {
            SkinTrait skin = npc.getOrAddTrait(SkinTrait.class);
            String texture = skin.getTexture();
            String display = ChatColor.GREEN + me.nakilex.levelplugin.utils.TextUtil.beautifyWords(rawName);
            ItemStack head;
            if (texture != null && !texture.isEmpty()) {
                head = HeadUtil.createCustomHead(texture, display, null);
            } else {
                head = new ItemStack(Material.PLAYER_HEAD);
            }
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(display);
                int discoveredCount = manager.getDiscoveredNpcCount(id);
                int total = manager.getTotalNpcCount();
                double progress = total == 0 ? 0 : (double) discoveredCount / total;
                List<String> lore = new ArrayList<>();
                String bar = GuiUtil.createProgressBar(progress, 15);
                lore.add(bar + " " + ChatColor.YELLOW + discoveredCount + ChatColor.GOLD + "/" + ChatColor.YELLOW + total
                        + ChatColor.GRAY + " (" + ChatColor.YELLOW + Math.round(progress * 100) + "%" + ChatColor.GRAY + ")");
                meta.setLore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                head.setItemMeta(meta);
            }
            return head;
        } else {
            ItemStack item = new ItemStack(Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.DARK_GRAY + "???");
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
            return item;
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int slot = e.getRawSlot();
        if (slot == PREV_SLOT) {
            int page = pageMap.getOrDefault(p.getUniqueId(), 0);
            open(p, Math.max(0, page - 1));
            return;
        }
        if (slot == NEXT_SLOT) {
            int page = pageMap.getOrDefault(p.getUniqueId(), 0);
            open(p, page + 1);
            return;
        }
        if (slot == BACK_SLOT && mainGui != null) {
            mainGui.open(p);
        }
    }
}
