package me.nakilex.levelplugin.player.classes.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.ClassSelectionUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassSelectionGUI implements Listener {
    private static final String TITLE = "Class Selection";
    private static final int[] CLASS_SLOTS = {10, 12, 14, 16};
    private static final ClassSelectionGUI INSTANCE = new ClassSelectionGUI();

    private static final List<PlayerClass> BASE_CLASSES = List.of(
            PlayerClass.WARRIOR,
            PlayerClass.ROGUE,
            PlayerClass.MAGE,
            PlayerClass.ARCHER
    );

    private static final Map<PlayerClass, Material> CLASS_ICONS = Map.of(
            PlayerClass.WARRIOR, Material.IRON_SWORD,
            PlayerClass.ROGUE, Material.SHEARS,
            PlayerClass.MAGE, Material.BLAZE_ROD,
            PlayerClass.ARCHER, Material.BOW
    );

    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    private ClassSelectionGUI() {
    }

    public static ClassSelectionGUI getInstance() {
        return INSTANCE;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        Inventory gui = GuiBuilder.create(27, TITLE)
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .build();
        List<GuiWidget> widgets = buildWidgets(player);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        GuiLayout layout = new GuiLayout(gui);
        GuiContext context = new GuiContext(player, gui);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
        player.openInventory(gui);
    }

    private List<GuiWidget> buildWidgets(Player player) {
        List<GuiWidget> widgets = new ArrayList<>();
        PlayerClass current = PlayerClassManager.getInstance().getPlayerClass(player);
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        for (int i = 0; i < BASE_CLASSES.size() && i < CLASS_SLOTS.length; i++) {
            PlayerClass candidate = BASE_CLASSES.get(i);
            int slot = CLASS_SLOTS[i];
            widgets.add(new ActionWidget(slot,
                    ctx -> createClassItem(candidate, current, stats.unlockedClasses.contains(candidate)),
                    (click, ctx) -> {
                        if (ClassSelectionUtil.applyClassSelection(ctx.player(), candidate, true)) {
                            ChatMessageUtil.send(ctx.player(), ChatMessageUtil.MessageType.SUCCESS,
                                    "Class changed to " + candidate.getDisplayName() + ".");
                        } else {
                            ChatMessageUtil.send(ctx.player(), ChatMessageUtil.MessageType.ERROR,
                                    "Unable to change class right now.");
                        }
                    }));
        }
        widgets.add(new ActionWidget(4, ctx -> createInfoItem(current), null));
        return widgets;
    }

    private ItemStack createInfoItem(PlayerClass current) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + (current != null ? current.getDisplayName() : "None"));
        lore.add(ChatColor.DARK_GRAY + "Choose any base class below.");
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to change class", null));
        return GuiUtil.createGuiItem(Material.NETHER_STAR, ChatColor.GOLD + "Class Selection", lore);
    }

    private ItemStack createClassItem(PlayerClass candidate, PlayerClass current, boolean unlocked) {
        Material icon = CLASS_ICONS.getOrDefault(candidate, Material.BOOK);
        boolean selected = candidate == current;
        List<String> lore = new ArrayList<>();
        lore.add(TooltipUtil.selectionLine(selected, ChatColor.WHITE + "Selected"));
        lore.add(TooltipUtil.selectionLine(unlocked, ChatColor.WHITE + "Unlocked"));
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to switch class", null));
        return GuiUtil.createGuiItem(icon,
                (selected ? ChatColor.GREEN : ChatColor.YELLOW) + candidate.getDisplayName(),
                lore);
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
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        GuiWidget widget = widgets.stream().filter(w -> w.handlesSlot(slot)).findFirst().orElse(null);
        if (widget != null) {
            widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        widgetsByPlayer.remove(player.getUniqueId());
    }
}
