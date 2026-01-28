package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.mercenary.gui.MercenaryFriendshipGUI;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.HeadUtil;
import me.nakilex.levelplugin.utils.NpcNameUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

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
    private static final String TITLE = "Codex - NPCs";
    private static final int SIZE = 54;
    private static final int ITEMS_PER_PAGE = GuiUtil.PAGED_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int BACK_SLOT = 49;

    private final NamespacedKey npcKey;
    private final CodexManager manager;
    private CodexMainGUI mainGui;
    private final MercenaryAffinityManager affinityManager;
    private final MercenaryFriendshipGUI friendshipGUI;
    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final List<GuiWidget> widgets;

    public NpcCodexGUI(Plugin plugin,
                       CodexManager manager,
                       CodexMainGUI mainGui,
                       MercenaryAffinityManager affinityManager,
                       MercenaryFriendshipGUI friendshipGUI) {
        this.npcKey = new NamespacedKey(plugin, "codex_npc_id");
        this.manager = manager;
        this.mainGui = mainGui;
        this.affinityManager = affinityManager;
        this.friendshipGUI = friendshipGUI;
        this.widgets = buildWidgets();
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
            inv.setItem(GuiUtil.PAGED_SLOTS[slot++],
                    createNpcIcon(id, discovered, npcs.get(i)));
        }

        renderWidgets(inv, player);

        player.openInventory(inv);
    }

    private ItemStack createNpcIcon(UUID id, Set<String> discovered, NPC npc) {
        String rawName = ChatColor.stripColor(npc.getName());
        String key = NpcNameUtil.normalize(rawName);
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
                String bar = TooltipUtil.progressBar(discoveredCount, total, 15);
                lore.add(bar + " " + ChatColor.YELLOW + discoveredCount + ChatColor.GOLD + "/" + ChatColor.YELLOW + total
                        + ChatColor.GRAY + " (" + ChatColor.YELLOW + Math.round(progress * 100) + "%" + ChatColor.GRAY + ")");
                if (isMercenary(npc.getId())) {
                    lore.add(" ");
                    lore.addAll(TooltipUtil.bulletList("Mercenary affinity tracked."));
                    lore.addAll(TooltipUtil.clickInstructions("to view affinity & perks", null));
                    meta.getPersistentDataContainer().set(npcKey, PersistentDataType.INTEGER, npc.getId());
                }
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
        if (!GuiUtil.titleMatches(e.getView().getTitle(), TITLE)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (handleWidgetClick(e, p)) {
            return;
        }
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        Integer npcId = readNpcId(clicked);
        if (npcId == null) {
            return;
        }
        if (affinityManager == null || friendshipGUI == null) {
            return;
        }

        if (!isMercenary(npcId)) {
            p.sendMessage(ChatColor.RED + "Affinity is not tracked for this NPC.");
            return;
        }

        NPC npc = NpcApi.getRegistry().getById(npcId);
        String npcName = npc != null ? ChatColor.stripColor(npc.getName()) : ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (npcName == null || npcName.isBlank()) {
            npcName = "Mercenary " + npcId;
        }
        affinityManager.loadPlayer(p.getUniqueId());
        friendshipGUI.openWithBack(p, npcId, npcName, viewer -> open(viewer));
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new java.util.ArrayList<>();
        widgetList.add(new ActionWidget(PREV_SLOT,
                context -> createPrevItem(context.player()),
                (click, context) -> handlePrev(context.player())));
        widgetList.add(new ActionWidget(NEXT_SLOT,
                context -> createNextItem(context.player()),
                (click, context) -> handleNext(context.player())));
        widgetList.add(new ActionWidget(BACK_SLOT,
                context -> GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"),
                (click, context) -> {
                    if (mainGui != null) {
                        mainGui.open(context.player());
                    }
                }));
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
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

    private ItemStack createPrevItem(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        return page > 0 ? GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous") : null;
    }

    private ItemStack createNextItem(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        int total = manager.getAllNpcs().size();
        return total > (page + 1) * ITEMS_PER_PAGE
                ? GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next")
                : null;
    }

    private void handlePrev(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, Math.max(0, page - 1));
    }

    private void handleNext(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, page + 1);
    }

    private boolean isMercenary(int npcId) {
        return affinityManager != null && affinityManager.getMercenaryIds().contains(npcId);
    }

    private Integer readNpcId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        Integer value = meta.getPersistentDataContainer().get(npcKey, PersistentDataType.INTEGER);
        return value;
    }
}
