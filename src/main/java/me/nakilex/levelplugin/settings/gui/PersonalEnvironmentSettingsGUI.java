package me.nakilex.levelplugin.settings.gui;

import me.nakilex.levelplugin.settings.environment.PersonalTimeType;
import me.nakilex.levelplugin.settings.environment.PersonalWeatherType;
import me.nakilex.levelplugin.settings.environment.PlayerEnvironmentService;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PersonalEnvironmentSettingsGUI implements Listener {

    private static final int GUI_SIZE = 45;
    private static final int WEATHER_SLOT = 20;
    private static final int TIME_SLOT = 24;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final PlayerEnvironmentService environmentService;
    private final String title = ChatUtil.applyEmojis("§8Personal Environment");
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new java.util.HashMap<>();
    private SettingsGUI settingsGUI;

    public PersonalEnvironmentSettingsGUI(PlayerEnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    public void setSettingsGUI(SettingsGUI settingsGUI) {
        this.settingsGUI = settingsGUI;
    }

    public void open(Player player) {
        Inventory inv = GuiBuilder.create(GUI_SIZE, title)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        List<GuiWidget> widgets = buildWidgets(player);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String viewTitle = LEGACY.serialize(event.getView().title());
        if (!GuiUtil.titleMatches(viewTitle, title)) {
            return;
        }
        if (!handleWidgetClick(event, player)) {
            event.setCancelled(true);
        }
    }

    private List<GuiWidget> buildWidgets(Player player) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(0, ctx -> GuiUtil.getNexoItem("arrow_left2", "§7Back"),
                (click, context) -> {
                    if (settingsGUI != null) {
                        settingsGUI.openSettingsMenu(context.player());
                    }
                }));

        widgets.add(new ActionWidget(WEATHER_SLOT,
                ctx -> createWeatherItem(player),
                (click, context) -> cycleWeather(context.player(), click.isLeftClick())));
        widgets.add(new ActionWidget(TIME_SLOT,
                ctx -> createTimeItem(player),
                (click, context) -> cycleTime(context.player(), click.isLeftClick())));
        return widgets;
    }

    private ItemStack createWeatherItem(Player player) {
        PersonalWeatherType active = environmentService.getCurrentWeatherOrReset(player);
        ItemStack item = GuiUtil.getNexoItem("placeholder_weather", "§bPersonal Weather");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("§7Set client-side weather only");
            lore.add("§7for your character.");
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(active == PersonalWeatherType.CLEAR, "Clear"));
            lore.add(TooltipUtil.selectionLine(active == PersonalWeatherType.RAIN, "Rain"));
            lore.add(TooltipUtil.selectionLine(active == PersonalWeatherType.THUNDER, "Thunder"));
            lore.add(TooltipUtil.selectionLine(active == PersonalWeatherType.RESET, "World Default"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTimeItem(Player player) {
        PersonalTimeType active = environmentService.getCurrentTimeOrReset(player);
        ItemStack item = GuiUtil.getNexoItem("placeholder_time", "§bPersonal Time");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add("§7Set client-side time only");
            lore.add("§7for your character.");
            lore.add(" ");
            lore.add(TooltipUtil.selectionLine(active == PersonalTimeType.DAY, "Day"));
            lore.add(TooltipUtil.selectionLine(active == PersonalTimeType.NIGHT, "Night"));
            lore.add(TooltipUtil.selectionLine(active == PersonalTimeType.SUNSET, "Sunset"));
            lore.add(TooltipUtil.selectionLine(active == PersonalTimeType.RESET, "World Default"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to go forward", "to go backward"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void cycleWeather(Player player, boolean forward) {
        PersonalWeatherType current = environmentService.getCurrentWeatherOrReset(player);
        environmentService.applyWeather(player, current.cycle(forward));
        open(player);
    }

    private void cycleTime(Player player, boolean forward) {
        PersonalTimeType current = environmentService.getCurrentTimeOrReset(player);
        environmentService.applyTime(player, current.cycle(forward));
        open(player);
    }

    private void renderWidgets(Inventory inv, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inv);
        GuiContext context = new GuiContext(player, inv);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
        GuiWidget widget = widgets.stream().filter(w -> w.handlesSlot(slot)).findFirst().orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }
}
