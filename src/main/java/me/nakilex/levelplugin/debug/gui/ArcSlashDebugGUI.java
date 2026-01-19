package me.nakilex.levelplugin.debug.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.nakilex.levelplugin.debug.ArcSlashDebugManager;
import me.nakilex.levelplugin.debug.ArcSlashDebugManager.ArcSlashConfig;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArcSlashDebugGUI implements Listener {
    private static final String TITLE = "Arc Slash Debug";
    private static final int GUI_SIZE = 54;
    private static final int SAVE_SLOT = 49;
    private static final int RESET_SLOT = 48;
    private static final int CLOSE_SLOT = 50;

    private static final int PARTICLE_SLOT = 10;
    private static final int POINTS_SLOT = 11;
    private static final int TICKS_SLOT = 12;
    private static final int FRAME_STEP_SLOT = 13;
    private static final int START_DISTANCE_SLOT = 14;
    private static final int TRAVEL_DISTANCE_SLOT = 15;
    private static final int RADIUS_X_MIN_SLOT = 16;
    private static final int RADIUS_X_MAX_SLOT = 19;
    private static final int RADIUS_Z_MIN_SLOT = 20;
    private static final int RADIUS_Z_MAX_SLOT = 21;
    private static final int START_ANGLE_SLOT = 22;
    private static final int END_ANGLE_SLOT = 23;
    private static final int BASE_TILT_MIN_SLOT = 24;
    private static final int BASE_TILT_MAX_SLOT = 25;
    private static final int LAYER_TILT_SLOT = 28;
    private static final int SIDE_SHIFT_SLOT = 29;
    private static final int WIDTH_SLOT = 30;
    private static final int FORWARD_OFFSET_SLOT = 32;
    private static final int RIGHT_OFFSET_SLOT = 33;
    private static final int UP_OFFSET_SLOT = 34;
    private static final int ROTATE_X_SLOT = 37;
    private static final int ROTATE_Y_SLOT = 38;
    private static final int ROTATE_Z_SLOT = 39;
    private static final int INFO_SLOT = 40;

    private static final double DISTANCE_STEP = 0.1;
    private static final double RADIUS_STEP = 0.1;
    private static final double ANGLE_STEP = 2.0;
    private static final double TILT_STEP = 1.0;
    private static final double SIDE_SHIFT_STEP = 0.05;
    private static final double OFFSET_STEP = 0.1;
    private static final double WIDTH_STEP = 0.05;
    private static final double ROTATION_STEP = 5.0;

    private static final int POINT_STEP = 2;
    private static final int TICK_STEP = 1;
    private static final int FRAME_STEP_STEP = 1;

    private static final List<Particle> PARTICLE_OPTIONS = List.of(
            Particle.END_ROD,
            Particle.CRIT,
            Particle.ENCHANT,
            Particle.CLOUD,
            Particle.FIREWORKS_SPARK,
            Particle.FLAME,
            Particle.SMOKE,
            Particle.HEART
    );

    private final ArcSlashDebugManager arcSlashDebugManager;
    private final Map<UUID, ArcSlashConfig> workingConfigs = new HashMap<>();

    public ArcSlashDebugGUI(ArcSlashDebugManager arcSlashDebugManager) {
        this.arcSlashDebugManager = arcSlashDebugManager;
    }

    public void open(Player player) {
        ArcSlashConfig config = arcSlashDebugManager.config();
        workingConfigs.put(player.getUniqueId(), config);
        player.openInventory(buildInventory(config));
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
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        ArcSlashConfig config = workingConfigs.get(player.getUniqueId());
        if (config == null) {
            config = arcSlashDebugManager.config();
            workingConfigs.put(player.getUniqueId(), config);
        }
        boolean rightClick = event.isRightClick();
        boolean shiftClick = event.isShiftClick();
        if (slot == SAVE_SLOT) {
            arcSlashDebugManager.applyConfig(config);
            arcSlashDebugManager.logConfig(config);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Arc slash settings saved.");
            player.openInventory(buildInventory(config));
            return;
        }
        if (slot == RESET_SLOT) {
            ArcSlashConfig reset = ArcSlashConfig.defaultConfig();
            workingConfigs.put(player.getUniqueId(), reset);
            arcSlashDebugManager.applyConfig(reset);
            player.openInventory(buildInventory(reset));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Arc slash settings reset to defaults.");
            return;
        }
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        handleConfigClick(config, slot, rightClick, shiftClick);
        arcSlashDebugManager.applyConfig(config);
        player.openInventory(buildInventory(config));
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

    private void handleConfigClick(ArcSlashConfig config, int slot, boolean rightClick, boolean shiftClick) {
        int direction = rightClick ? -1 : 1;
        int multiplier = shiftClick ? 5 : 1;
        if (slot == PARTICLE_SLOT) {
            config.setParticle(nextParticle(config.particle(), direction));
        } else if (slot == POINTS_SLOT) {
            config.setPoints(clampInt(config.points() + POINT_STEP * direction * multiplier, 2, 120));
        } else if (slot == TICKS_SLOT) {
            config.setTicks(clampInt(config.ticks() + TICK_STEP * direction * multiplier, 1, 40));
        } else if (slot == FRAME_STEP_SLOT) {
            config.setFrameStep(clampInt(config.frameStep() + FRAME_STEP_STEP * direction * multiplier, 1, 10));
        } else if (slot == START_DISTANCE_SLOT) {
            config.setStartDistance(clampDouble(config.startDistance() + DISTANCE_STEP * direction * multiplier, 0.0, 8.0));
        } else if (slot == TRAVEL_DISTANCE_SLOT) {
            config.setTravelDistance(clampDouble(config.travelDistance() + DISTANCE_STEP * direction * multiplier, 0.0, 8.0));
        } else if (slot == RADIUS_X_MIN_SLOT) {
            double value = clampDouble(config.radiusXMin() + RADIUS_STEP * direction * multiplier, 0.1, 6.0);
            config.setRadiusXMin(Math.min(value, config.radiusXMax()));
        } else if (slot == RADIUS_X_MAX_SLOT) {
            double value = clampDouble(config.radiusXMax() + RADIUS_STEP * direction * multiplier, 0.1, 6.0);
            config.setRadiusXMax(Math.max(value, config.radiusXMin()));
        } else if (slot == RADIUS_Z_MIN_SLOT) {
            double value = clampDouble(config.radiusZMin() + RADIUS_STEP * direction * multiplier, 0.1, 6.0);
            config.setRadiusZMin(Math.min(value, config.radiusZMax()));
        } else if (slot == RADIUS_Z_MAX_SLOT) {
            double value = clampDouble(config.radiusZMax() + RADIUS_STEP * direction * multiplier, 0.1, 6.0);
            config.setRadiusZMax(Math.max(value, config.radiusZMin()));
        } else if (slot == START_ANGLE_SLOT) {
            config.setStartAngleDegrees(clampDouble(config.startAngleDegrees() + ANGLE_STEP * direction * multiplier, -180.0, 180.0));
        } else if (slot == END_ANGLE_SLOT) {
            config.setEndAngleDegrees(clampDouble(config.endAngleDegrees() + ANGLE_STEP * direction * multiplier, -180.0, 180.0));
        } else if (slot == BASE_TILT_MIN_SLOT) {
            double value = clampDouble(config.baseTiltMin() + TILT_STEP * direction * multiplier, -90.0, 90.0);
            config.setBaseTiltMin(Math.min(value, config.baseTiltMax()));
        } else if (slot == BASE_TILT_MAX_SLOT) {
            double value = clampDouble(config.baseTiltMax() + TILT_STEP * direction * multiplier, -90.0, 90.0);
            config.setBaseTiltMax(Math.max(value, config.baseTiltMin()));
        } else if (slot == LAYER_TILT_SLOT) {
            config.setLayerTiltStep(clampDouble(config.layerTiltStep() + TILT_STEP * direction * multiplier, 0.0, 45.0));
        } else if (slot == SIDE_SHIFT_SLOT) {
            config.setSideShiftFactor(clampDouble(config.sideShiftFactor() + SIDE_SHIFT_STEP * direction * multiplier, -1.0, 1.0));
        } else if (slot == WIDTH_SLOT) {
            config.setWidth(clampDouble(config.width() + WIDTH_STEP * direction * multiplier, 0.0, 3.0));
        } else if (slot == FORWARD_OFFSET_SLOT) {
            config.setForwardOffset(clampDouble(config.forwardOffset() + OFFSET_STEP * direction * multiplier, -4.0, 6.0));
        } else if (slot == RIGHT_OFFSET_SLOT) {
            config.setRightOffset(clampDouble(config.rightOffset() + OFFSET_STEP * direction * multiplier, -4.0, 4.0));
        } else if (slot == UP_OFFSET_SLOT) {
            config.setUpOffset(clampDouble(config.upOffset() + OFFSET_STEP * direction * multiplier, -4.0, 4.0));
        } else if (slot == ROTATE_X_SLOT) {
            config.setRotateXDegrees(clampDouble(config.rotateXDegrees() + ROTATION_STEP * direction * multiplier, -180.0, 180.0));
        } else if (slot == ROTATE_Y_SLOT) {
            config.setRotateYDegrees(clampDouble(config.rotateYDegrees() + ROTATION_STEP * direction * multiplier, -180.0, 180.0));
        } else if (slot == ROTATE_Z_SLOT) {
            config.setRotateZDegrees(clampDouble(config.rotateZDegrees() + ROTATION_STEP * direction * multiplier, -180.0, 180.0));
        }
    }

    private Inventory buildInventory(ArcSlashConfig config) {
        GuiBuilder builder = GuiBuilder.create(GUI_SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .fillEmptySlots(false);

        builder.setItem(PARTICLE_SLOT, createParamItem(Material.BLAZE_POWDER, ChatColor.AQUA + "Particle",
                "Particle type used for the arc.",
                List.of("Current: " + ChatColor.WHITE + config.particle().name()),
                "to cycle forward", "to cycle backward", null, null));
        builder.setItem(POINTS_SLOT, createParamItem(Material.NETHER_STAR, ChatColor.AQUA + "Points",
                "How many particles form each arc.",
                List.of("Current: " + ChatColor.WHITE + config.points()),
                "to increase points", "to decrease points", "to increase faster", "to decrease faster"));
        builder.setItem(TICKS_SLOT, createParamItem(Material.CLOCK, ChatColor.AQUA + "Ticks",
                "How long the arc travels forward.",
                List.of("Current: " + ChatColor.WHITE + config.ticks()),
                "to increase duration", "to decrease duration", "to increase faster", "to decrease faster"));
        builder.setItem(FRAME_STEP_SLOT, createParamItem(Material.REPEATER, ChatColor.AQUA + "Frame Step",
                "Spacing between animation frames.",
                List.of("Current: " + ChatColor.WHITE + config.frameStep()),
                "to increase step", "to decrease step", "to increase faster", "to decrease faster"));
        builder.setItem(START_DISTANCE_SLOT, createParamItem(Material.ENDER_PEARL, ChatColor.AQUA + "Start Distance",
                "Initial distance in front of the player.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.startDistance())),
                "to push forward", "to pull back", "to push further", "to pull further"));
        builder.setItem(TRAVEL_DISTANCE_SLOT, createParamItem(Material.ELYTRA, ChatColor.AQUA + "Travel Distance",
                "How far the arc advances forward.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.travelDistance())),
                "to increase travel", "to decrease travel", "to increase faster", "to decrease faster"));
        builder.setItem(RADIUS_X_MIN_SLOT, createParamItem(Material.SHIELD, ChatColor.AQUA + "Radius X (Min)",
                "Minimum horizontal size of the arc.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.radiusXMin())),
                "to widen min radius", "to tighten min radius", "to widen faster", "to tighten faster"));
        builder.setItem(RADIUS_X_MAX_SLOT, createParamItem(Material.SHIELD, ChatColor.AQUA + "Radius X (Max)",
                "Maximum horizontal size of the arc.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.radiusXMax())),
                "to widen max radius", "to tighten max radius", "to widen faster", "to tighten faster"));
        builder.setItem(RADIUS_Z_MIN_SLOT, createParamItem(Material.IRON_BARS, ChatColor.AQUA + "Radius Z (Min)",
                "Minimum vertical depth of the arc.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.radiusZMin())),
                "to widen min depth", "to tighten min depth", "to widen faster", "to tighten faster"));
        builder.setItem(RADIUS_Z_MAX_SLOT, createParamItem(Material.IRON_BARS, ChatColor.AQUA + "Radius Z (Max)",
                "Maximum vertical depth of the arc.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.radiusZMax())),
                "to widen max depth", "to tighten max depth", "to widen faster", "to tighten faster"));
        builder.setItem(START_ANGLE_SLOT, createParamItem(Material.COMPASS, ChatColor.AQUA + "Start Angle",
                "Where the arc begins along the curve.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.startAngleDegrees()) + "°"),
                "to rotate forward", "to rotate backward", "to rotate faster", "to rotate faster"));
        builder.setItem(END_ANGLE_SLOT, createParamItem(Material.COMPASS, ChatColor.AQUA + "End Angle",
                "Where the arc ends along the curve.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.endAngleDegrees()) + "°"),
                "to rotate forward", "to rotate backward", "to rotate faster", "to rotate faster"));
        builder.setItem(BASE_TILT_MIN_SLOT, createParamItem(Material.FEATHER, ChatColor.AQUA + "Base Tilt (Min)",
                "Minimum tilt applied to the arc layers.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.baseTiltMin()) + "°"),
                "to increase min tilt", "to decrease min tilt", "to increase faster", "to decrease faster"));
        builder.setItem(BASE_TILT_MAX_SLOT, createParamItem(Material.FEATHER, ChatColor.AQUA + "Base Tilt (Max)",
                "Maximum tilt applied to the arc layers.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.baseTiltMax()) + "°"),
                "to increase max tilt", "to decrease max tilt", "to increase faster", "to decrease faster"));
        builder.setItem(LAYER_TILT_SLOT, createParamItem(Material.QUARTZ, ChatColor.AQUA + "Layer Tilt Step",
                "Separation between arc layers.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.layerTiltStep()) + "°"),
                "to add separation", "to reduce separation", "to add more", "to reduce more"));
        builder.setItem(SIDE_SHIFT_SLOT, createParamItem(Material.ARROW, ChatColor.AQUA + "Side Shift Factor",
                "Scales horizontal offset by arc size.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.sideShiftFactor())),
                "to push right", "to push left", "to push further", "to pull further"));
        builder.setItem(WIDTH_SLOT, createParamItem(Material.PAPER, ChatColor.AQUA + "Arc Width",
                "Thickness of the arc band.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.width())),
                "to thicken", "to thin", "to thicken faster", "to thin faster"));
        builder.setItem(FORWARD_OFFSET_SLOT, createParamItem(Material.OAK_SIGN, ChatColor.AQUA + "Forward Offset",
                "Extra push along the look direction.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.forwardOffset())),
                "to push forward", "to pull back", "to push further", "to pull further"));
        builder.setItem(RIGHT_OFFSET_SLOT, createParamItem(Material.ARROW, ChatColor.AQUA + "Right Offset",
                "Shift the arc left or right.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.rightOffset())),
                "to move right", "to move left", "to move further", "to move further"));
        builder.setItem(UP_OFFSET_SLOT, createParamItem(Material.FEATHER, ChatColor.AQUA + "Up Offset",
                "Raise or lower the arc.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.upOffset())),
                "to move up", "to move down", "to move further", "to move further"));
        builder.setItem(ROTATE_X_SLOT, createParamItem(Material.IRON_BARS, ChatColor.AQUA + "Rotate X",
                "Rotate the arc around the X axis.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.rotateXDegrees()) + "°"),
                "to rotate forward", "to rotate backward", "to rotate faster", "to rotate faster"));
        builder.setItem(ROTATE_Y_SLOT, createParamItem(Material.IRON_BLOCK, ChatColor.AQUA + "Rotate Y",
                "Rotate the arc around the Y axis.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.rotateYDegrees()) + "°"),
                "to rotate forward", "to rotate backward", "to rotate faster", "to rotate faster"));
        builder.setItem(ROTATE_Z_SLOT, createParamItem(Material.IRON_NUGGET, ChatColor.AQUA + "Rotate Z",
                "Rotate the arc around the Z axis.",
                List.of("Current: " + ChatColor.WHITE + formatDecimal(config.rotateZDegrees()) + "°"),
                "to rotate forward", "to rotate backward", "to rotate faster", "to rotate faster"));
        builder.setItem(INFO_SLOT, createInfoItem(config));
        builder.setItem(RESET_SLOT, GuiUtil.getNexoItem("refresh", ChatColor.YELLOW + "Reset Defaults"));
        builder.setItem(SAVE_SLOT, GuiUtil.getNexoItem("save", ChatColor.GREEN + "Save Settings"));
        builder.setItem(CLOSE_SLOT, GuiUtil.getNexoItem("cross", ChatColor.RED + "Close"));

        return builder.build();
    }

    private ItemStack createInfoItem(ArcSlashConfig config) {
        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Arc Preview");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = TooltipUtil.bulletList(
                    "Use /debug particlepreset arc",
                    "Left-click to spawn the arc.",
                    "Save to apply new values."
            );
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            info.setItemMeta(meta);
        }
        return info;
    }

    private ItemStack createParamItem(Material material, String name, String description, List<String> valueLines,
                                      String leftAction, String rightAction,
                                      String sneakLeftAction, String sneakRightAction) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new java.util.ArrayList<>();
            lore.addAll(TooltipUtil.bulletList(description));
            lore.addAll(TooltipUtil.bulletList(valueLines.toArray(new String[0])));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions(leftAction, rightAction));
            if (sneakLeftAction != null || sneakRightAction != null) {
                lore.addAll(TooltipUtil.sneakClickInstructions(sneakLeftAction, sneakRightAction));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Particle nextParticle(Particle current, int direction) {
        int index = PARTICLE_OPTIONS.indexOf(current);
        if (index < 0) {
            return PARTICLE_OPTIONS.get(0);
        }
        int next = (index + direction) % PARTICLE_OPTIONS.size();
        if (next < 0) {
            next += PARTICLE_OPTIONS.size();
        }
        return PARTICLE_OPTIONS.get(next);
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatDecimal(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
