package me.nakilex.levelplugin.debug.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.StrongholdDebugGenerator;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StrongholdTemplateDebugGUI implements Listener {
    private static final String TITLE = "Stronghold Templates";
    private static final int GUI_SIZE = 54;
    private static StrongholdTemplateDebugGUI instance;
    private final Map<UUID, Map<Integer, String>> slotTemplateByPlayer = new HashMap<>();
    private final Map<UUID, Map<String, StrongholdDebugGenerator.TemplateConnectionInfo>> templateInfoByPlayer = new HashMap<>();
    private boolean registered;

    private StrongholdTemplateDebugGUI() {
    }

    public static StrongholdTemplateDebugGUI getInstance() {
        if (instance == null) {
            instance = new StrongholdTemplateDebugGUI();
        }
        return instance;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        ensureRegistered();

        Map<String, StrongholdDebugGenerator.TemplateConnectionInfo> templates =
                StrongholdDebugGenerator.inspectTemplateConnections();
        if (templates.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Could not read stronghold templates. Ensure source world 'flatland' is loaded.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, TITLE);
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);

        Map<Integer, String> slotToTemplate = new HashMap<>();
        int slot = 10;
        for (Map.Entry<String, StrongholdDebugGenerator.TemplateConnectionInfo> entry : templates.entrySet()) {
            while (slot < GUI_SIZE && (slot % 9 == 0 || slot % 9 == 8)) {
                slot++;
            }
            if (slot >= GUI_SIZE) {
                break;
            }
            inv.setItem(slot, templateItem(entry.getKey(), entry.getValue()));
            slotToTemplate.put(slot, entry.getKey());
            slot++;
        }

        slotTemplateByPlayer.put(player.getUniqueId(), slotToTemplate);
        templateInfoByPlayer.put(player.getUniqueId(), templates);
        player.openInventory(inv);
    }

    private void ensureRegistered() {
        if (registered) {
            return;
        }
        Main plugin = Main.getInstance();
        if (plugin != null) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
    }

    private void fill(Inventory inv, Material material) {
        ItemStack filler = GuiUtil.createFiller(material);
        GuiUtil.fillBorder(inv, filler);
    }

    private ItemStack templateItem(String id, StrongholdDebugGenerator.TemplateConnectionInfo info) {
        Material icon = pickMaterial(id);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Connectors: " + ChatColor.YELLOW + info.connectorCount());
        lore.add(ChatColor.GRAY + "Sides: " + ChatColor.AQUA + formatSides(info.sides()));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to print connector info in chat", null));
        return GuiUtil.createGuiItem(icon, ChatColor.GOLD + id, lore);
    }

    private Material pickMaterial(String id) {
        if (id.startsWith("tower")) {
            return Material.COBBLESTONE_WALL;
        }
        if (id.startsWith("t_section")) {
            return Material.CHAIN;
        }
        if (id.startsWith("gate")) {
            return Material.IRON_BARS;
        }
        if (id.startsWith("deadend")) {
            return Material.BARRIER;
        }
        if (id.startsWith("connector")) {
            return Material.OAK_FENCE;
        }
        return Material.STONE_BRICKS;
    }

    private String formatSides(List<BlockFace> sides) {
        if (sides == null || sides.isEmpty()) {
            return "none";
        }
        EnumSet<BlockFace> present = EnumSet.copyOf(sides);
        List<String> labels = new ArrayList<>();
        Map<BlockFace, String> names = new EnumMap<>(BlockFace.class);
        names.put(BlockFace.NORTH, "N");
        names.put(BlockFace.EAST, "E");
        names.put(BlockFace.SOUTH, "S");
        names.put(BlockFace.WEST, "W");
        for (BlockFace side : List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
            if (present.contains(side)) {
                labels.add(names.get(side));
            }
        }
        return String.join(", ", labels);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        Map<Integer, String> slots = slotTemplateByPlayer.get(player.getUniqueId());
        if (slots == null) {
            return;
        }
        String id = slots.get(event.getRawSlot());
        if (id == null) {
            return;
        }

        Map<String, StrongholdDebugGenerator.TemplateConnectionInfo> cachedInfo = templateInfoByPlayer.get(player.getUniqueId());
        StrongholdDebugGenerator.TemplateConnectionInfo info = cachedInfo == null ? null : cachedInfo.get(id);
        if (info == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Could not inspect template '" + id + "'.");
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Template " + id + " -> connectors: " + info.connectorCount()
                        + " (sides: " + formatSides(info.sides()) + ").");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        UUID playerId = player.getUniqueId();
        slotTemplateByPlayer.remove(playerId);
        templateInfoByPlayer.remove(playerId);
    }
}
