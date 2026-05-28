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
    private static final int TEST_SLOT = 47;
    private static final int SAVE_SLOT = 48;
    private static final int CLOSE_SLOT = 49;
    private static final int INFO_SLOT = 50;

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
                            "Wield style settings reset to the manual horizontal baseline.");
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
        COOLDOWN(10, Material.CLOCK, "Cooldown Ticks", "Ticks before another slash can start.", 1.0, 1.0, 80.0,
                WieldStyleConfig::cooldownTicks, (config, value) -> config.setCooldownTicks(value.intValue())),
        SWING_TICKS(11, Material.REPEATER, "Swing Ticks", "Total duration of the slash animation.", 1.0, 2.0, 80.0,
                WieldStyleConfig::swingTicks, (config, value) -> config.setSwingTicks(value.intValue())),
        INTERPOLATION(12, Material.COMPARATOR, "Interpolation", "Display interpolation duration between teleports.", 1.0, 0.0, 20.0,
                WieldStyleConfig::interpolationDuration, (config, value) -> config.setInterpolationDuration(value.intValue())),
        SCALE(13, Material.AMETHYST_SHARD, "Weapon Scale", "Visual size of the ItemDisplay weapon.", 0.05, 0.10, 3.0,
                WieldStyleConfig::scale, WieldStyleConfig::setScale),
        IDLE_DISTANCE(14, Material.ENDER_PEARL, "Idle Forward", "Idle distance in front of the player's eyes.", 0.05, -2.0, 5.0,
                WieldStyleConfig::idleDistance, WieldStyleConfig::setIdleDistance),
        IDLE_RIGHT(15, Material.ARROW, "Idle Right", "Idle offset to the player's right side.", 0.05, -3.0, 3.0,
                WieldStyleConfig::idleRightOffset, WieldStyleConfig::setIdleRightOffset),
        IDLE_UP(16, Material.FEATHER, "Idle Up", "Idle vertical offset from the player's eyes.", 0.05, -3.0, 3.0,
                WieldStyleConfig::idleUpOffset, WieldStyleConfig::setIdleUpOffset),
        IDLE_YAW(19, Material.COMPASS, "Idle Yaw", "Idle entity yaw offset from player yaw.", 2.0, -180.0, 180.0,
                WieldStyleConfig::idleYawOffset, WieldStyleConfig::setIdleYawOffset),
        IDLE_PITCH(20, Material.COMPASS, "Idle Pitch", "Idle entity pitch.", 2.0, -90.0, 90.0,
                WieldStyleConfig::idlePitch, WieldStyleConfig::setIdlePitch),
        IDLE_LEFT_ROT(21, Material.IRON_BARS, "Idle Left Rotation", "Idle transformation left X rotation.", 2.0, -180.0, 180.0,
                WieldStyleConfig::idleLeftRotation, WieldStyleConfig::setIdleLeftRotation),
        IDLE_RIGHT_ROT(22, Material.IRON_NUGGET, "Idle Right Rotation", "Idle transformation right Y rotation.", 2.0, -180.0, 180.0,
                WieldStyleConfig::idleRightRotation, WieldStyleConfig::setIdleRightRotation),
        ARC_START(23, Material.BLAZE_ROD, "Arc Start Angle", "Starting angle for the swing position arc.", 2.0, -360.0, 360.0,
                WieldStyleConfig::swingAngleStart, WieldStyleConfig::setSwingAngleStart),
        ARC_SWEEP(24, Material.BLAZE_POWDER, "Arc Sweep", "How many degrees the position arc travels.", 2.0, -720.0, 720.0,
                WieldStyleConfig::swingAngleSweep, WieldStyleConfig::setSwingAngleSweep),
        SIDE_RADIUS(25, Material.SHIELD, "Side Radius", "Horizontal/right radius of the swing arc.", 0.05, -4.0, 4.0,
                WieldStyleConfig::swingSideRadius, WieldStyleConfig::setSwingSideRadius),
        UP_RADIUS(28, Material.ELYTRA, "Up Radius", "Vertical radius of the swing arc.", 0.05, -4.0, 4.0,
                WieldStyleConfig::swingUpRadius, WieldStyleConfig::setSwingUpRadius),
        UP_OFFSET(29, Material.FEATHER, "Swing Up Offset", "Vertical offset applied to the whole swing arc.", 0.05, -4.0, 4.0,
                WieldStyleConfig::swingUpOffset, WieldStyleConfig::setSwingUpOffset),
        FORWARD_BASE(30, Material.ENDER_EYE, "Forward Base", "Base forward distance during the swing.", 0.05, -2.0, 6.0,
                WieldStyleConfig::swingForwardBase, WieldStyleConfig::setSwingForwardBase),
        FORWARD_PEAK(31, Material.FIREWORK_ROCKET, "Forward Peak", "Extra forward push at the middle of the swing.", 0.05, -2.0, 6.0,
                WieldStyleConfig::swingForwardPeak, WieldStyleConfig::setSwingForwardPeak),
        SWING_YAW_START(32, Material.COMPASS, "Swing Yaw Start", "Starting entity yaw offset for the slash.", 2.0, -360.0, 360.0,
                WieldStyleConfig::swingYawStart, WieldStyleConfig::setSwingYawStart),
        SWING_YAW_SWEEP(33, Material.COMPASS, "Swing Yaw Sweep", "Yaw degrees added across the slash.", 2.0, -720.0, 720.0,
                WieldStyleConfig::swingYawSweep, WieldStyleConfig::setSwingYawSweep),
        SWING_PITCH_START(34, Material.COMPASS, "Swing Pitch Start", "Starting entity pitch for the slash.", 2.0, -180.0, 180.0,
                WieldStyleConfig::swingPitchStart, WieldStyleConfig::setSwingPitchStart),
        SWING_PITCH_SWEEP(37, Material.COMPASS, "Swing Pitch Sweep", "Pitch degrees added across the slash.", 2.0, -360.0, 360.0,
                WieldStyleConfig::swingPitchSweep, WieldStyleConfig::setSwingPitchSweep),
        LEFT_ROT_START(38, Material.IRON_BARS, "Left Rot Start", "Starting transformation left X rotation.", 2.0, -360.0, 360.0,
                WieldStyleConfig::swingLeftRotationStart, WieldStyleConfig::setSwingLeftRotationStart),
        LEFT_ROT_SWEEP(39, Material.IRON_BARS, "Left Rot Sweep", "Left X rotation added across the slash.", 2.0, -720.0, 720.0,
                WieldStyleConfig::swingLeftRotationSweep, WieldStyleConfig::setSwingLeftRotationSweep),
        RIGHT_ROT_START(40, Material.IRON_NUGGET, "Right Rot Start", "Starting transformation right Y rotation.", 2.0, -360.0, 360.0,
                WieldStyleConfig::swingRightRotationStart, WieldStyleConfig::setSwingRightRotationStart),
        RIGHT_ROT_SWEEP(41, Material.IRON_NUGGET, "Right Rot Sweep", "Right Y rotation added across the slash.", 2.0, -720.0, 720.0,
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
