package me.nakilex.levelplugin.debug.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.impl.WarriorExecutionArcSpell;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class WarriorCycloneDebugGUI implements Listener {
    private static final String TITLE = "Warrior Cyclone Debug";
    private static WarriorCycloneDebugGUI instance;
    private boolean registered;

    private WarriorCycloneDebugGUI() {
    }

    public static WarriorCycloneDebugGUI getInstance() {
        if (instance == null) {
            instance = new WarriorCycloneDebugGUI();
        }
        return instance;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        ensureRegistered();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        render(inv);
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

    private void render(Inventory inv) {
        WarriorExecutionArcSpell.CycloneVisualConfig cfg = WarriorExecutionArcSpell.getVisualConfig();
        inv.setItem(10, numberItem(Material.IRON_BARS, "Orbit Radius", cfg.orbitRadius(), 0.1, 0.25));
        inv.setItem(11, numberItem(Material.SCAFFOLDING, "Base Height", cfg.baseHeight(), 0.05, 0.2));
        inv.setItem(12, numberItem(Material.LIGHT_BLUE_STAINED_GLASS, "Height Wave", cfg.heightWaveAmplitude(), 0.05, 0.1));
        inv.setItem(13, numberItem(Material.CLOCK, "Angular Speed", cfg.angularSpeed(), 0.05, 0.15));

        inv.setItem(15, angleItem(Material.BLAZE_POWDER, "Arm Pitch", cfg.armPitchDegrees()));
        inv.setItem(16, angleItem(Material.BLAZE_ROD, "Arm Yaw", cfg.armYawDegrees()));
        inv.setItem(17, angleItem(Material.STICK, "Arm Roll", cfg.armRollDegrees()));

        List<String> invisLore = new ArrayList<>();
        invisLore.add(ChatColor.GRAY + "Current: " + (cfg.invisibleStand() ? ChatColor.GREEN + "Invisible" : ChatColor.YELLOW + "Visible"));
        invisLore.add(" ");
        invisLore.addAll(TooltipUtil.clickInstructions("to toggle", null));
        inv.setItem(31, GuiUtil.createGuiItem(cfg.invisibleStand() ? Material.ENDER_EYE : Material.ARMOR_STAND,
                ChatColor.GOLD + "Stand Invisibility", invisLore));

        List<String> resetLore = TooltipUtil.bulletList("Resets cyclone visual tuning to defaults.");
        resetLore.add(" ");
        resetLore.addAll(TooltipUtil.clickInstructions("to reset", null));
        inv.setItem(49, GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "Reset Defaults", resetLore));
    }

    private ItemStack numberItem(Material material, String name, double value, double step, double bigStep) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + String.format("%.2f", value));
        lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Left/Right: ±" + step);
        lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Shift+Left/Right: ±" + bigStep);
        return GuiUtil.createGuiItem(material, ChatColor.YELLOW + name, lore);
    }

    private ItemStack angleItem(Material material, String name, double value) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + String.format("%.1f°", value));
        lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Left/Right: ±5°");
        lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Shift+Left/Right: ±15°");
        return GuiUtil.createGuiItem(material, ChatColor.AQUA + name, lore);
    }

    private void fill(Inventory inv, Material material) {
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, GuiUtil.createGuiItem(material, " ", List.of()));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        double sign = (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) ? -1.0 : 1.0;
        boolean shift = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;

        switch (slot) {
            case 10 -> WarriorExecutionArcSpell.updateVisualConfig(cfg ->
                    cfg.setOrbitRadius(cfg.orbitRadius() + sign * (shift ? 0.25 : 0.1)));
            case 11 -> WarriorExecutionArcSpell.updateVisualConfig(cfg ->
                    cfg.setBaseHeight(cfg.baseHeight() + sign * (shift ? 0.2 : 0.05)));
            case 12 -> WarriorExecutionArcSpell.updateVisualConfig(cfg ->
                    cfg.setHeightWaveAmplitude(cfg.heightWaveAmplitude() + sign * (shift ? 0.1 : 0.05)));
            case 13 -> WarriorExecutionArcSpell.updateVisualConfig(cfg ->
                    cfg.setAngularSpeed(cfg.angularSpeed() + sign * (shift ? 0.15 : 0.05)));
            case 15 -> WarriorExecutionArcSpell.updateVisualConfig(cfg ->
                    cfg.setArmPitchDegrees(cfg.armPitchDegrees() + sign * (shift ? 15.0 : 5.0)));
            case 16 -> WarriorExecutionArcSpell.updateVisualConfig(cfg ->
                    cfg.setArmYawDegrees(cfg.armYawDegrees() + sign * (shift ? 15.0 : 5.0)));
            case 17 -> WarriorExecutionArcSpell.updateVisualConfig(cfg ->
                    cfg.setArmRollDegrees(cfg.armRollDegrees() + sign * (shift ? 15.0 : 5.0)));
            case 31 -> WarriorExecutionArcSpell.updateVisualConfig(cfg -> cfg.setInvisibleStand(!cfg.invisibleStand()));
            case 49 -> WarriorExecutionArcSpell.updateVisualConfig(cfg -> {
                cfg.setOrbitRadius(2.0);
                cfg.setBaseHeight(0.20);
                cfg.setHeightWaveAmplitude(0.0);
                cfg.setAngularSpeed(0.30);
                cfg.setArmPitchDegrees(0.0);
                cfg.setArmYawDegrees(0.0);
                cfg.setArmRollDegrees(90.0);
                cfg.setInvisibleStand(true);
            });
            default -> {
                return;
            }
        }
        render(event.getView().getTopInventory());
    }
}
