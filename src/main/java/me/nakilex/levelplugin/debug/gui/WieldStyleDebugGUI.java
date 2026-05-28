package me.nakilex.levelplugin.debug.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import me.nakilex.levelplugin.debug.WieldStyleDebugManager;
import me.nakilex.levelplugin.debug.WieldStyleDebugManager.WieldStyleConfig;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Runtime editor for the debug custom wield ItemDisplay pose and swing arc. */
public class WieldStyleDebugGUI implements Listener {
    private static final String TITLE = "Wield Style Debug";
    private static final int GUI_SIZE = 54;
    private static final int ENABLE_SLOT = 45;
    private static final int RESET_SLOT = 46;
    private static final int RANDOMIZE_SLOT = 47;
    private static final int TEST_SLOT = 48;
    private static final int SAVE_SLOT = 49;
    private static final int CLOSE_SLOT = 50;
    private static final int INFO_SLOT = 51;

    private final WieldStyleDebugManager wieldStyleDebugManager;
    private final Map<UUID, WieldStyleConfig> workingConfigs = new HashMap<>();
    private final List<GuiWidget> widgets;

    public WieldStyleDebugGUI(WieldStyleDebugManager wieldStyleDebugManager) {
        this.wieldStyleDebugManager = wieldStyleDebugManager;
        this.widgets = buildWidgets();
    }

    public void open(Player player) {
        workingConfigs.put(player.getUniqueId(), wieldStyleDebugManager.config());
        player.openInventory(buildInventory(player));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        handleWidgetClick(event, player);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            workingConfigs.remove(player.getUniqueId());
        }
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new ArrayList<>();
        for (WieldParam param : WieldParam.values()) {
            widgetList.add(new ActionWidget(param.slot(),
                    context -> createParamItem(param, getConfig(context)),
                    (click, context) -> handleParamClick(click, context, param)));
        }
        widgetList.add(new ActionWidget(ENABLE_SLOT,
                context -> GuiUtil.getNexoItem("swap", ChatColor.GREEN + "Toggle Preview",
                        TooltipUtil.bulletList("Enable or disable the ItemDisplay weapon preview.",
                                "Current: " + (wieldStyleDebugManager.isEnabled(context.player()) ? "enabled" : "disabled"))),
                (click, context) -> {
                    wieldStyleDebugManager.toggle(context.player());
                    context.player().openInventory(buildInventory(context.player()));
                }));
        widgetList.add(new ActionWidget(RESET_SLOT,
                context -> GuiUtil.getNexoItem("refresh", ChatColor.YELLOW + "Reset Defaults",
                        TooltipUtil.bulletList("Reset every runtime pose and swing value.")),
                (click, context) -> {
                    wieldStyleDebugManager.resetConfig();
                    workingConfigs.put(context.player().getUniqueId(), wieldStyleDebugManager.config());
                    ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.INFO,
                            "Wield style settings reset to the overhead baseline.");
                    context.player().openInventory(buildInventory(context.player()));
                }));
        widgetList.add(new ActionWidget(RANDOMIZE_SLOT,
                context -> createRandomizeItem(),
                (click, context) -> {
                    WieldStyleConfig config = getConfig(context);
                    wieldStyleDebugManager.randomizeSwingConfig(config);
                    wieldStyleDebugManager.applyConfig(config);
                    wieldStyleDebugManager.playOnce(context.player());
                    ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.SUCCESS,
                            "Randomized swing values without changing weapon size or swing ticks.");
                    context.player().openInventory(buildInventory(context.player()));
                }));
        widgetList.add(new ActionWidget(TEST_SLOT,
                context -> GuiUtil.getNexoItem("play", ChatColor.AQUA + "Test Swing",
                        TooltipUtil.bulletList("Apply the current values and play one slash immediately.")),
                (click, context) -> {
                    wieldStyleDebugManager.applyConfig(getConfig(context));
                    wieldStyleDebugManager.playOnce(context.player());
                    context.player().openInventory(buildInventory(context.player()));
                }));
        widgetList.add(new ActionWidget(SAVE_SLOT,
                context -> GuiUtil.getNexoItem("save", ChatColor.GREEN + "Log Settings",
                        TooltipUtil.bulletList("Apply values and print the full config to console.")),
                (click, context) -> {
                    WieldStyleConfig config = getConfig(context);
                    wieldStyleDebugManager.applyConfig(config);
                    wieldStyleDebugManager.logConfig(config);
                    ChatMessageUtil.send(context.player(), ChatMessageUtil.MessageType.SUCCESS,
                            "Wield style settings applied and logged.");
                    context.player().openInventory(buildInventory(context.player()));
                }));
        widgetList.add(new ActionWidget(CLOSE_SLOT,
                context -> GuiUtil.getNexoItem("cross", ChatColor.RED + "Close"),
                (click, context) -> context.player().closeInventory()));
        widgetList.add(new ActionWidget(INFO_SLOT,
                context -> createInfoItem(context.player()),
                null));
        return widgetList;
    }

    private void handleParamClick(ClickType click, GuiContext context, WieldParam param) {
        WieldStyleConfig config = getConfig(context);
        int direction = click.isRightClick() ? -1 : 1;
        int multiplier = click.isShiftClick() ? 5 : 1;
        double next = param.value(config) + (param.step() * direction * multiplier);
        param.apply(config, clampDouble(next, param.min(), param.max()));
        wieldStyleDebugManager.applyConfig(config);
        context.player().openInventory(buildInventory(context.player()));
    }

    private WieldStyleConfig getConfig(GuiContext context) {
        WieldStyleConfig config = workingConfigs.get(context.player().getUniqueId());
        if (config == null) {
            config = wieldStyleDebugManager.config();
            workingConfigs.put(context.player().getUniqueId(), config);
        }
        return config;
    }

    private void handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return;
        }
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
    }

    private Inventory buildInventory(Player player) {
        GuiBuilder builder = GuiBuilder.create(GUI_SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .fillEmptySlots(false);
        Inventory inventory = builder.build();
        renderWidgets(inventory, player);
        return inventory;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private ItemStack createParamItem(WieldParam param, WieldStyleConfig config) {
        ItemStack item = new ItemStack(param.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + param.displayName());
            List<String> lore = new ArrayList<>();
            lore.addAll(TooltipUtil.bulletList(param.description()));
            lore.addAll(TooltipUtil.bulletList("Current: " + ChatColor.WHITE + param.formatValue(config),
                    "Step: " + ChatColor.WHITE + param.formatStep()));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to increase", "to decrease"));
            lore.addAll(TooltipUtil.sneakClickInstructions("to increase x5", "to decrease x5"));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRandomizeItem() {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Randomize Swing");
            meta.setLore(TooltipUtil.bulletList(
                    "Randomizes the big swing shape/rotation values.",
                    "Weapon scale and swing ticks stay unchanged.",
                    "Forward Base/Peak only get small nudges."));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(Player player) {
        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Editor Notes");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = TooltipUtil.bulletList(
                    "/debug wield gui opens this editor.",
                    "Left-click increases, right-click decreases.",
                    "Sneak-click changes by five steps.",
                    "Changes apply live to idle preview; Test Swing plays the arc.",
                    "Preview: " + (wieldStyleDebugManager.isEnabled(player) ? "enabled" : "disabled")
            );
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            info.setItemMeta(meta);
        }
        return info;
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private enum WieldParam {
        ARC_START(10, Material.BLAZE_ROD, "Arc Start Angle", "Where around the swing circle the weapon starts.", 5.0, -360.0, 360.0,
                WieldStyleConfig::swingAngleStart, WieldStyleConfig::setSwingAngleStart),
        ARC_SWEEP(11, Material.BLAZE_POWDER, "Arc Sweep", "How far around the swing circle the weapon travels.", 5.0, -720.0, 720.0,
                WieldStyleConfig::swingAngleSweep, WieldStyleConfig::setSwingAngleSweep),
        SIDE_RADIUS(12, Material.SHIELD, "Side Radius", "How wide the swing moves left and right.", 0.05, -4.0, 4.0,
                WieldStyleConfig::swingSideRadius, WieldStyleConfig::setSwingSideRadius),
        UP_RADIUS(13, Material.ELYTRA, "Up Radius", "How tall the swing moves up and down.", 0.05, -4.0, 4.0,
                WieldStyleConfig::swingUpRadius, WieldStyleConfig::setSwingUpRadius),
        UP_OFFSET(14, Material.FEATHER, "Swing Up Offset", "Moves the whole swing higher or lower.", 0.05, -4.0, 4.0,
                WieldStyleConfig::swingUpOffset, WieldStyleConfig::setSwingUpOffset),
        FORWARD_BASE(15, Material.ENDER_EYE, "Forward Base", "How far in front of the player the swing sits.", 0.05, -2.0, 6.0,
                WieldStyleConfig::swingForwardBase, WieldStyleConfig::setSwingForwardBase),
        FORWARD_PEAK(16, Material.FIREWORK_ROCKET, "Forward Peak", "Extra forward push at the middle of the swing.", 0.05, -2.0, 6.0,
                WieldStyleConfig::swingForwardPeak, WieldStyleConfig::setSwingForwardPeak),
        SWING_YAW_START(19, Material.COMPASS, "Yaw Start", "Which side direction the weapon faces at swing start.", 5.0, -360.0, 360.0,
                WieldStyleConfig::swingYawStart, WieldStyleConfig::setSwingYawStart),
        SWING_YAW_SWEEP(20, Material.COMPASS, "Yaw Sweep", "How much the weapon turns left/right during the swing.", 5.0, -720.0, 720.0,
                WieldStyleConfig::swingYawSweep, WieldStyleConfig::setSwingYawSweep),
        SWING_PITCH_START(21, Material.COMPASS, "Pitch Start", "How far up/down the weapon points at swing start.", 5.0, -180.0, 180.0,
                WieldStyleConfig::swingPitchStart, WieldStyleConfig::setSwingPitchStart),
        SWING_PITCH_SWEEP(22, Material.COMPASS, "Pitch Sweep", "How much the weapon tips up/down during the swing.", 5.0, -360.0, 360.0,
                WieldStyleConfig::swingPitchSweep, WieldStyleConfig::setSwingPitchSweep),
        LEFT_ROT_START(23, Material.IRON_BARS, "Left Rot Start", "Main X-axis model rotation at swing start.", 5.0, -360.0, 360.0,
                WieldStyleConfig::swingLeftRotationStart, WieldStyleConfig::setSwingLeftRotationStart),
        LEFT_ROT_SWEEP(24, Material.IRON_BARS, "Left Rot Sweep", "How much the X-axis model rotation changes.", 5.0, -720.0, 720.0,
                WieldStyleConfig::swingLeftRotationSweep, WieldStyleConfig::setSwingLeftRotationSweep),
        RIGHT_ROT_START(25, Material.IRON_NUGGET, "Right Rot Start", "Main Y-axis model rotation at swing start.", 5.0, -360.0, 360.0,
                WieldStyleConfig::swingRightRotationStart, WieldStyleConfig::setSwingRightRotationStart),
        RIGHT_ROT_SWEEP(28, Material.IRON_NUGGET, "Right Rot Sweep", "How much the Y-axis model rotation changes.", 5.0, -720.0, 720.0,
                WieldStyleConfig::swingRightRotationSweep, WieldStyleConfig::setSwingRightRotationSweep);
        private final int slot;
        private final Material material;
        private final String displayName;
        private final String description;
        private final double step;
        private final double min;
        private final double max;
        private final Function<WieldStyleConfig, Number> getter;
        private final BiConsumer<WieldStyleConfig, Double> setter;

        WieldParam(int slot, Material material, String displayName, String description, double step, double min, double max,
                   Function<WieldStyleConfig, Number> getter, BiConsumer<WieldStyleConfig, Double> setter) {
            this.slot = slot;
            this.material = material;
            this.displayName = displayName;
            this.description = description;
            this.step = step;
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
        }

        int slot() { return slot; }
        Material material() { return material; }
        String displayName() { return displayName; }
        String description() { return description; }
        double step() { return step; }
        double min() { return min; }
        double max() { return max; }
        double value(WieldStyleConfig config) { return getter.apply(config).doubleValue(); }
        void apply(WieldStyleConfig config, double value) { setter.accept(config, value); }
        String formatValue(WieldStyleConfig config) { return formatDecimal(value(config)); }
        String formatStep() { return formatDecimal(step); }
    }
}
