package me.nakilex.levelplugin.utils.gui.dynamic;

import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Config-backed inventory menu system inspired by DeluxeMenus style workflows.
 *
 * <p>Foundation goals:</p>
 * <ul>
 *     <li>Define menus in YAML (title, size, items, click actions)</li>
 *     <li>Reuse existing GUI utils ({@link GuiBuilder}, {@link GuiUtil})</li>
 *     <li>Provide small placeholder support for player context</li>
 * </ul>
 */
public final class DynamicMenuManager implements Listener {
    private static final String CONFIG_NAME = "dynamic_menus.yml";
    private static DynamicMenuManager instance;

    private final JavaPlugin plugin;
    private final Map<String, DynamicMenuDefinition> menus = new HashMap<>();
    private final Map<UUID, OpenMenuState> openMenus = new HashMap<>();

    private DynamicMenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        ensureConfigExists();
        reload();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static synchronized DynamicMenuManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new DynamicMenuManager(plugin);
        }
        return instance;
    }

    public void reload() {
        menus.clear();
        File file = new File(plugin.getDataFolder(), CONFIG_NAME);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("menus");
        if (root == null) {
            plugin.getLogger().warning("[DynamicMenu] No menus section found in " + CONFIG_NAME);
            return;
        }

        for (String menuId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(menuId);
            if (section == null) {
                continue;
            }
            DynamicMenuDefinition definition = DynamicMenuDefinition.fromSection(menuId, section);
            menus.put(menuId.toLowerCase(Locale.ROOT), definition);
        }

        plugin.getLogger().info("[DynamicMenu] Loaded " + menus.size() + " menu definitions.");
    }

    public boolean openMenu(Player player, String menuId) {
        DynamicMenuDefinition definition = menus.get(menuId.toLowerCase(Locale.ROOT));
        if (definition == null) {
            return false;
        }

        String renderedTitle = renderTitle(definition, player);
        Inventory inventory = GuiBuilder.create(definition.size(), renderedTitle)
                .filler(definition.filler())
                .fillEmptySlots(definition.fillEmptySlots())
                .build();
        if (definition.border()) {
            GuiUtil.fillBorder(inventory, GuiUtil.createFiller(definition.filler()));
        }

        Map<Integer, DynamicMenuItemDefinition> clickMap = new HashMap<>();
        for (DynamicMenuItemDefinition itemDef : definition.items()) {
            ItemStack icon = itemDef.toItem(player);
            if (icon == null) {
                continue;
            }
            inventory.setItem(itemDef.slot(), icon);
            if (itemDef.hasActions()) {
                clickMap.put(itemDef.slot(), itemDef);
            }
        }

        openMenus.put(player.getUniqueId(), new OpenMenuState(definition.id(), renderedTitle, clickMap));
        player.openInventory(inventory);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        OpenMenuState state = openMenus.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (!GuiUtil.titleMatches(event.getView().getTitle(), state.rawTitle())) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        event.setCancelled(true);
        DynamicMenuItemDefinition itemDef = state.slotActions().get(event.getRawSlot());
        if (itemDef == null) {
            return;
        }

        List<String> commands = event.getClick().isRightClick() ? itemDef.rightClickCommands() : itemDef.leftClickCommands();
        executeCommands(player, commands);
        if (itemDef.closeOnClick()) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        OpenMenuState state = openMenus.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (!GuiUtil.titleMatches(event.getView().getTitle(), state.rawTitle())) {
            return;
        }
        openMenus.remove(player.getUniqueId());
    }

    private String renderTitle(DynamicMenuDefinition definition, Player player) {
        String base = applyPlaceholders(definition.title(), player);
        String prefix = applyPlaceholders(definition.titlePrefix(), player);
        String suffix = applyPlaceholders(definition.titleSuffix(), player);
        String composed = prefix + base + suffix;
        if (definition.centerTitle()) {
            return TextUtil.centerInventoryTitle(composed);
        }
        return composed;
    }

    private void executeCommands(Player player, List<String> commands) {
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String command = applyPlaceholders(raw, player).trim();
            if (command.regionMatches(true, 0, "[player]", 0, 8)) {
                String payload = command.substring(8).trim();
                Bukkit.dispatchCommand(player, stripLeadingSlash(payload));
            } else if (command.regionMatches(true, 0, "[console]", 0, 9)) {
                String payload = command.substring(9).trim();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripLeadingSlash(payload));
            } else if (command.regionMatches(true, 0, "[message]", 0, 9)) {
                String payload = command.substring(9).trim();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', payload));
            } else if (command.regionMatches(true, 0, "[menu]", 0, 6)) {
                String targetMenu = command.substring(6).trim();
                if (!openMenu(player, targetMenu)) {
                    player.sendMessage(ChatColor.RED + "Menu not found: " + targetMenu);
                }
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripLeadingSlash(command));
            }
        }
    }

    private String stripLeadingSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private String applyPlaceholders(String input, Player player) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input)
                .replace("%player_name%", player.getName())
                .replace("%player_display_name%", player.getDisplayName());
    }

    private void ensureConfigExists() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("[DynamicMenu] Failed to create plugin data folder.");
            return;
        }
        File file = new File(dataFolder, CONFIG_NAME);
        if (!file.exists()) {
            plugin.saveResource(CONFIG_NAME, false);
        }
    }

    private record OpenMenuState(
            String menuId,
            String rawTitle,
            Map<Integer, DynamicMenuItemDefinition> slotActions
    ) {
    }
}

