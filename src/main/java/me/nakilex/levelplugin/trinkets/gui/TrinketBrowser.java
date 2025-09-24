package me.nakilex.levelplugin.trinkets.gui;

import me.nakilex.levelplugin.trinkets.data.TrinketEffectDefinition;
import me.nakilex.levelplugin.trinkets.data.TrinketTemplate;
import me.nakilex.levelplugin.trinkets.managers.TrinketManager;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Provides a paginated browser for viewing and spawning configured trinkets.
 */
public class TrinketBrowser implements CommandExecutor, Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int PAGE_SIZE = GuiUtil.PAGED_SLOTS.length;
    private static final String TITLE_PREFIX = ChatColor.BLACK + "Trinket Browser - Page ";

    private final JavaPlugin plugin;
    private final TrinketManager manager;
    private final NamespacedKey templateKey;

    public TrinketBrowser(JavaPlugin plugin, TrinketManager manager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.manager = Objects.requireNonNull(manager, "manager");
        this.templateKey = new NamespacedKey(plugin, "trinket_browser_template");

        if (plugin.getCommand("trinketbrowser") != null) {
            plugin.getCommand("trinketbrowser").setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can browse trinkets.");
            return true;
        }
        openPage(player, 0);
        return true;
    }

    private void openPage(Player player, int page) {
        List<TrinketTemplate> templates = sortedTemplates();
        int maxPage = Math.max(0, (templates.size() - 1) / PAGE_SIZE);
        int clampedPage = Math.min(Math.max(page, 0), maxPage);

        Inventory inventory = GuiBuilder.create(INVENTORY_SIZE, TITLE_PREFIX + (clampedPage + 1))
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .fillEmptySlots(true)
                .build();

        int startIndex = clampedPage * PAGE_SIZE;
        for (int slotIndex = 0; slotIndex < PAGE_SIZE; slotIndex++) {
            int templateIndex = startIndex + slotIndex;
            if (templateIndex >= templates.size()) {
                break;
            }
            TrinketTemplate template = templates.get(templateIndex);
            ItemStack preview = createPreviewItem(template);
            inventory.setItem(GuiUtil.PAGED_SLOTS[slotIndex], preview);
        }

        ItemStack previous = GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous Page");
        ItemStack next = GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next Page");
        inventory.setItem(INVENTORY_SIZE - 9, previous);
        inventory.setItem(INVENTORY_SIZE - 1, next);

        player.openInventory(inventory);
    }

    private ItemStack createPreviewItem(TrinketTemplate template) {
        TrinketEffectDefinition midpoint = template.getDefaultEffect();
        ItemStack base = template.createItemStack(null, null, null, null, midpoint);
        ItemMeta meta = base.getItemMeta();
        if (meta == null) {
            return base;
        }

        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        int spacerIndex = lore.indexOf(" ");
        if (spacerIndex >= 0) {
            lore.remove(spacerIndex);
        } else {
            spacerIndex = lore.size();
        }

        List<String> rangeLines = buildRangeLore(template);
        lore.addAll(spacerIndex, rangeLines);
        lore.add(spacerIndex + rangeLines.size(), " ");

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(templateKey, PersistentDataType.STRING, template.getId());

        meta.setLore(lore);
        base.setItemMeta(meta);
        return base;
    }

    private List<String> buildRangeLore(TrinketTemplate template) {
        List<String> lines = new ArrayList<>();
        double minMagnitude = template.getMagnitudeRange().getMin();
        double maxMagnitude = template.getMagnitudeRange().getMax();
        TrinketEffectDefinition minEffect = new TrinketEffectDefinition(template.getEffectType(), minMagnitude, 0);
        TrinketEffectDefinition maxEffect = new TrinketEffectDefinition(template.getEffectType(), maxMagnitude, 0);
        String magnitudeRange = ChatColor.DARK_GRAY + "Possible Magnitude: " + ChatColor.GRAY
                + template.getEffectType().formatMagnitude(minEffect) + ChatColor.DARK_GRAY + " - "
                + ChatColor.GRAY + template.getEffectType().formatMagnitude(maxEffect);

        String magnitudeTiers = ChatColor.DARK_GRAY + "Magnitude Tiers: " + ChatColor.GRAY
                + TextUtil.toRomanNumeral(template.getEffectType().resolveMagnitudeTier(minMagnitude))
                + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY
                + TextUtil.toRomanNumeral(template.getEffectType().resolveMagnitudeTier(maxMagnitude));

        double minDuration = template.getDurationRange().getMin();
        double maxDuration = template.getDurationRange().getMax();
        String durationRange = ChatColor.DARK_GRAY + "Duration Range: " + ChatColor.GRAY
                + formatSeconds(minDuration) + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + formatSeconds(maxDuration);

        String durationTiers = ChatColor.DARK_GRAY + "Duration Tiers: " + ChatColor.GRAY
                + TextUtil.toRomanNumeral(template.getEffectType().resolveDurationTier(minDuration))
                + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY
                + TextUtil.toRomanNumeral(template.getEffectType().resolveDurationTier(maxDuration));

        lines.add(" ");
        lines.add(magnitudeRange);
        lines.add(magnitudeTiers);
        lines.add(durationRange);
        lines.add(durationTiers);
        return lines;
    }

    private String formatSeconds(double seconds) {
        if (Math.abs(seconds - Math.rint(seconds)) < 0.0001) {
            return ((int) Math.round(seconds)) + "s";
        }
        return String.format(Locale.US, "%.1fs", seconds);
    }

    private List<TrinketTemplate> sortedTemplates() {
        List<TrinketTemplate> templates = new ArrayList<>(manager.getTemplates());
        templates.sort(Comparator.comparing(t -> ChatColor.stripColor(t.getFormattedName()), String.CASE_INSENSITIVE_ORDER));
        return templates;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        String strippedTitle = ChatColor.stripColor(title);
        int currentPage = 0;
        try {
            String[] parts = strippedTitle.split(" ");
            currentPage = Integer.parseInt(parts[parts.length - 1]) - 1;
        } catch (NumberFormatException ignored) {
        }

        String displayName = clicked.getItemMeta().getDisplayName();
        List<TrinketTemplate> templates = sortedTemplates();
        int maxPage = Math.max(0, (templates.size() - 1) / PAGE_SIZE);

        if (displayName.equals(ChatColor.GREEN + "Next Page")) {
            int nextPage = currentPage >= maxPage ? 0 : currentPage + 1;
            openPage(player, nextPage);
            return;
        }

        if (displayName.equals(ChatColor.GREEN + "Previous Page")) {
            int previousPage = currentPage <= 0 ? maxPage : currentPage - 1;
            openPage(player, previousPage);
            return;
        }

        PersistentDataContainer container = clicked.getItemMeta().getPersistentDataContainer();
        String templateId = container.get(templateKey, PersistentDataType.STRING);
        if (templateId == null) {
            return;
        }

        ItemStack trinket = manager.createItem(templateId);
        if (trinket == null) {
            player.sendMessage(ChatColor.RED + "Failed to create trinket for template " + templateId + ".");
            return;
        }

        player.getInventory().addItem(trinket);
        ItemMeta meta = trinket.getItemMeta();
        if (meta != null) {
            player.sendMessage(ChatColor.GREEN + "You received " + meta.getDisplayName());
        }
    }
}

