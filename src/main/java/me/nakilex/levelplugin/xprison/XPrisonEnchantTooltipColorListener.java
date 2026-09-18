package me.nakilex.levelplugin.xprison;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.enchants.model.XPrisonEnchantment;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.GuiUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Recolours X-Prison's enchant tooltips per currency: token enchants lime, gem enchants magenta.
 *
 * <p>X-Prison has a single tooltip template ({@code enchants.yml} {@code enchant_menu.item.lore})
 * shared by every enchant, with no way to vary it by currency, so the colour swap has to happen
 * after X-Prison has built the item. Every yellow piece of the tooltip - the section headers, the
 * {@code |} bars and the numbers - becomes the currency's colour. Everything else (white labels,
 * the grey description, the enchant's own name colour) is left exactly as configured.</p>
 *
 * <p>Deliberately conservative: an item is only touched when its display name matches a registered
 * X-Prison enchant, so filler panes, tabs, arrows and the pickaxe itself are never rewritten.</p>
 */
public final class XPrisonEnchantTooltipColorListener implements Listener {

    private static final String PICKAXE_MENU_TITLE = "Pickaxe Menu";
    private static final String DISENCHANT_MENU_TITLE = "Enchant Refund";

    /** The colour X-Prison's shipped tooltip template uses for every header, bar and number. */
    private static final TextColor TEMPLATE_COLOR = NamedTextColor.YELLOW;
    private static final TextColor TOKEN_COLOR = NamedTextColor.GREEN;
    private static final TextColor GEM_COLOR = NamedTextColor.LIGHT_PURPLE;

    /** Matches a MiniMessage tag such as {@code <green>}, {@code </green>} or {@code <#ff00ff>}. */
    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("<[^<>]+>");

    private final Main plugin;

    /** Lower-cased enchant display name -> that enchant's currency name. Rebuilt on demand. */
    private final Map<String, String> currencyByEnchantName = new HashMap<>();

    /** Guards the one-shot diagnostic in {@link #logDiagnosticOnce(String)}. */
    private boolean diagnosticLogged;

    /** How many enchants the map was last built from, so it can be rebuilt when that changes. */
    private int enchantCount = -1;

    public XPrisonEnchantTooltipColorListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isEnchantMenu(event.getView().getTitle())) {
            recolourNextTick(player, event.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(InventoryClickEvent event) {
        // Any click can make X-Prison redraw the menu (tab switch, paging, filter, a purchase),
        // which rebuilds the items in the template's own yellow. Re-apply after every one.
        if (event.getWhoClicked() instanceof Player player && isEnchantMenu(event.getView().getTitle())) {
            recolourNextTick(player, event.getView().getTopInventory());
        }
    }

    private void recolourNextTick(Player player, Inventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Only re-check that the player is still looking at an enchant menu. Comparing the
            // Inventory objects is not reliable: the API can hand back a different wrapper object
            // for the same underlying inventory, and that comparison silently skipped every item.
            if (!player.isOnline() || !isEnchantMenu(player.getOpenInventory().getTitle())
                    || PickaxeMenuGUI.isPickaxeMenu(player.getOpenInventory().getTopInventory())) {
                return;
            }
            Map<String, String> currencies = currencyByEnchantName();
            if (currencies.isEmpty()) {
                logDiagnosticOnce("no X-Prison enchants were visible through the API");
                return;
            }
            int recoloured = 0;
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                if (recolourEnchantIcon(inventory, slot, currencies)) {
                    recoloured++;
                }
            }
            if (recoloured == 0) {
                // Print both sides of the failed lookup: the names the menu is showing and the
                // names we indexed. A mismatch between them is the whole story.
                logDiagnosticOnce("matched 0 enchant icons in a menu of " + inventory.getSize()
                        + " slots. Menu item names: " + sample(menuItemNames(inventory))
                        + " | known enchant names: " + sample(currencies.keySet()));
            }
            player.updateInventory();
        });
    }

    /** @return true when this slot held an enchant icon and its tooltip was recoloured. */
    private boolean recolourEnchantIcon(Inventory inventory, int slot, Map<String, String> currencies) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null || meta.lore() == null) {
            return false;
        }

        String plainName = PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName())
                .trim()
                .toLowerCase(Locale.ROOT);
        String currency = currencies.get(plainName);
        if (currency == null) {
            return false;
        }

        TextColor target = colourFor(currency);
        if (target == null) {
            return false;
        }

        List<Component> lore = meta.lore();
        List<Component> recoloured = new ArrayList<>(lore.size());
        for (Component line : lore) {
            recoloured.add(recolour(line, target));
        }
        meta.lore(recoloured);
        // The icon's own name takes the currency colour and bold, so an enchant reads as one
        // colour from its title down. Unlike the lore this overrides whatever colour is configured,
        // since each enchant sets its own and there is no single colour to key off.
        meta.displayName(paint(meta.displayName(), target));
        item.setItemMeta(meta);
        // Write the stack back explicitly: getItem() is not guaranteed to hand back a live
        // reference, so mutating it in place is not enough on its own.
        inventory.setItem(slot, item);
        return true;
    }

    private List<String> menuItemNames(Inventory inventory) {
        List<String> names = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getItemMeta() == null || item.getItemMeta().displayName() == null) {
                continue;
            }
            names.add(PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName())
                    .trim()
                    .toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private String sample(Collection<String> values) {
        return values.stream().limit(6).collect(Collectors.joining(", "))
                + (values.size() > 6 ? ", ... (" + values.size() + " total)" : "");
    }

    /**
     * Logs once per server start. This runs on every menu open, so a repeating warning would spam
     * the console; one line is enough to tell whether the listener is matching anything at all.
     */
    private void logDiagnosticOnce(String detail) {
        if (diagnosticLogged) {
            return;
        }
        diagnosticLogged = true;
        plugin.getLogger().warning("[EnchantTooltipColor] Tooltips were left unchanged: " + detail
                + ". Enchant tooltips will keep X-Prison's configured colours.");
    }

    /**
     * Force {@code target} and bold onto a component and everything under it, whatever it had.
     *
     * <p>Applied to every node rather than just the root because a child that explicitly sets its
     * own colour or turns bold off would otherwise keep winning over an inherited style.</p>
     */
    private Component paint(Component component, TextColor target) {
        Component result = component.color(target).decoration(TextDecoration.BOLD, true);
        if (result.children().isEmpty()) {
            return result;
        }
        List<Component> children = new ArrayList<>(result.children().size());
        for (Component child : result.children()) {
            children.add(paint(child, target));
        }
        return result.children(children);
    }

    /**
     * Swap the template's yellow for {@code target} throughout a lore line, children included.
     *
     * <p>A child with no colour of its own inherits its parent's, so recursing over the whole tree
     * and only rewriting explicitly-yellow nodes preserves the tooltip's other colours.</p>
     */
    private Component recolour(Component component, TextColor target) {
        // Compare by RGB value, not by object: X-Prison's yellow can arrive either as
        // NamedTextColor.YELLOW or as a plain hex colour with the same value, and those two are
        // not equal to each other.
        TextColor colour = component.color();
        boolean isTemplateColour = colour != null && colour.value() == TEMPLATE_COLOR.value();
        Component result = isTemplateColour ? component.color(target) : component;
        if (result.children().isEmpty()) {
            return result;
        }
        List<Component> children = new ArrayList<>(result.children().size());
        for (Component child : result.children()) {
            children.add(recolour(child, target));
        }
        return result.children(children);
    }

    private TextColor colourFor(String currencyName) {
        return switch (currencyName.toLowerCase(Locale.ROOT)) {
            case "tokens" -> TOKEN_COLOR;
            case "gems" -> GEM_COLOR;
            default -> null;
        };
    }

    /**
     * Enchants can be registered after this listener is constructed (X-Prison addons, our own
     * integrated enchants), so the map is filled on first use and refreshed whenever the enchant
     * count changes rather than being built once at startup.
     */
    private Map<String, String> currencyByEnchantName() {
        Collection<XPrisonEnchantment> enchants;
        try {
            enchants = XPrisonAPI.getInstance().getEnchantsApi().getAllEnchantments();
        } catch (RuntimeException | LinkageError ex) {
            return Map.of();
        }
        if (enchants.size() != enchantCount) {
            enchantCount = enchants.size();
            currencyByEnchantName.clear();
            for (XPrisonEnchantment enchant : enchants) {
                String currency = enchant.getCurrencyName();
                if (currency == null) {
                    continue;
                }
                // Index the enchant's own name and its icon name: the icon in the menu is titled
                // with the GUI name, which an enchant is free to configure differently.
                index(enchant.getNameWithoutColor(), currency);
                index(enchant.getName(), currency);
                if (enchant.getGuiProperties() != null) {
                    index(enchant.getGuiProperties().getGuiName(), currency);
                }
            }
        }
        return currencyByEnchantName;
    }

    private void index(String name, String currency) {
        String plain = stripFormatting(name);
        if (!plain.isEmpty()) {
            currencyByEnchantName.put(plain, currency);
        }
    }

    /**
     * Reduce a configured name to the bare text the player sees.
     *
     * <p>X-Prison's own {@code getNameWithoutColor()} only strips legacy {@code section} codes, so
     * on a MiniMessage-configured server it hands back e.g. {@code <green>Speed</green>} - which
     * never matches the item's real display name. Strip MiniMessage tags as well.</p>
     */
    private String stripFormatting(String name) {
        if (name == null) {
            return "";
        }
        String stripped = MINIMESSAGE_TAG.matcher(name).replaceAll("");
        stripped = ChatColor.stripColor(stripped);
        return stripped == null ? "" : stripped.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * The pickaxe and disenchant menus have fixed titles; the per-enchant upgrade menu is titled
     * with the enchant's own name, so it is recognised by looking that title up in the enchant map.
     */
    private boolean isEnchantMenu(String title) {
        if (GuiUtil.titleMatches(title, PICKAXE_MENU_TITLE)
                || GuiUtil.titleMatches(title, DISENCHANT_MENU_TITLE)) {
            return true;
        }
        String plainTitle = ChatColor.stripColor(title);
        return plainTitle != null
                && currencyByEnchantName().containsKey(plainTitle.trim().toLowerCase(Locale.ROOT));
    }
}
