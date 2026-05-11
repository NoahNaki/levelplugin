package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.spells.deck.SpellCardDefinition;
import me.nakilex.levelplugin.spells.deck.SpellDeckManager;
import me.nakilex.levelplugin.spells.deck.SpellDeckRarity;
import me.nakilex.levelplugin.spells.input.SpellInputType;
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

public class SpellUpgradeGUI implements Listener {
    private static final String TITLE = "Spell Deck";
    private static final int INFO_SLOT = 4;
    private static final int EQUIPPED_SLOT = 22;
    private static final int[] FIREBALL_SLOTS = {10, 11, 12, 13, 14, 15};

    private final SpellDeckManager deckManager = SpellDeckManager.getInstance();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public void open(Player player) {
        Inventory gui = GuiBuilder.create(27, TITLE).filler(Material.BLACK_STAINED_GLASS_PANE).build();
        List<GuiWidget> widgets = buildWidgets();
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        render(gui, player, widgets);
        player.openInventory(gui);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(INFO_SLOT, ctx -> createInfoItem(ctx.player()), null));
        widgets.add(new ActionWidget(EQUIPPED_SLOT, ctx -> createEquippedItem(ctx.player()), null));
        List<SpellCardDefinition> fireballs = deckManager.getDefinitionsForFamily("fireball");
        for (int i = 0; i < Math.min(FIREBALL_SLOTS.length, fireballs.size()); i++) {
            SpellCardDefinition card = fireballs.get(i);
            widgets.add(new ActionWidget(FIREBALL_SLOTS[i], ctx -> createCardItem(ctx.player(), card),
                    (click, ctx) -> {
                        deckManager.equip(ctx.player(), SpellInputType.BASIC_ATTACK, card.cardId());
                        refresh(ctx.player());
                    }));
        }
        return widgets;
    }

    private void render(Inventory gui, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(gui);
        GuiContext context = new GuiContext(player, gui);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private ItemStack createInfoItem(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Pull spell cards, then equip the rarity");
        lore.add(ChatColor.GRAY + "you want to cast from your spell deck.");
        lore.add(" ");
        lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.AQUA,
                "Pity", ChatColor.WHITE, deckManager.getPityPullsSinceLegendary(player.getUniqueId())
                        + "/" + deckManager.getPityThreshold() + " toward Legendary+"));
        lore.add(" ");
        lore.addAll(TooltipUtil.bulletList("Left-click a pulled card to equip it.",
                "For now, Fireball cards equip to Basic Attack.",
                "Use /debug spellpull <amount> to test pulls."));
        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK, ChatColor.AQUA + "Spell Deck", lore);
    }

    private ItemStack createEquippedItem(Player player) {
        SpellCardDefinition equipped = deckManager.getEquippedCard(player.getUniqueId(), SpellInputType.BASIC_ATTACK);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "Slot: " + ChatColor.WHITE + "Basic Attack");
        lore.add(" ");
        if (equipped == null) {
            lore.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED,
                    "Equipped", ChatColor.GRAY, "None"));
            lore.add(ChatColor.GRAY + "Pull a Fireball card and click it to equip.");
            return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "No Spell Equipped", lore);
        }
        lore.add(TooltipUtil.iconLabelValueLine("✔", ChatColor.GREEN, ChatColor.GREEN,
                "Equipped", equipped.rarity().color(), equipped.displayName()));
        lore.add(TooltipUtil.iconLabelValueLine("◆", equipped.rarity().color(), equipped.rarity().color(),
                "Rarity", ChatColor.WHITE, equipped.rarity().displayName()));
        lore.add(ChatColor.DARK_GRAY + "ID: " + equipped.cardId());
        return GuiUtil.createGuiItem(equipped.rarity().displayMaterial(),
                equipped.rarity().color() + "Equipped: " + equipped.displayName(), lore);
    }

    private ItemStack createCardItem(Player player, SpellCardDefinition card) {
        int copies = deckManager.getCopies(player.getUniqueId(), card.cardId());
        boolean owned = copies > 0;
        SpellCardDefinition equipped = deckManager.getEquippedCard(player.getUniqueId(), SpellInputType.BASIC_ATTACK);
        boolean selected = equipped != null && equipped.cardId().equalsIgnoreCase(card.cardId());
        SpellDeckRarity rarity = card.rarity();
        List<String> lore = new ArrayList<>();
        lore.add(TooltipUtil.iconLabelValueLine("◆", rarity.color(), rarity.color(),
                "Rarity", ChatColor.WHITE, rarity.displayName()));
        lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.AQUA,
                "Copies", ChatColor.WHITE, String.valueOf(copies)));
        lore.add(TooltipUtil.selectionLine(selected, selected ? "Equipped" : "Not equipped"));
        lore.add(" ");
        lore.add(ChatColor.WHITE + "Effects");
        for (String line : card.effectLines()) {
            lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + line));
        }
        if (!card.tradeoffLines().isEmpty()) {
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Tradeoff");
            for (String line : card.tradeoffLines()) {
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + line));
            }
        }
        lore.add(" ");
        if (owned) {
            lore.addAll(TooltipUtil.clickInstructions("to equip this Fireball", null));
        } else {
            lore.add(ChatColor.RED + "Locked " + ChatColor.GRAY + "Pull this card first.");
        }
        return GuiUtil.createGuiItem(owned ? rarity.displayMaterial() : Material.GRAY_WOOL,
                rarity.color() + card.displayName(), lore);
    }

    private void refresh(Player player) {
        if (player == null || player.getOpenInventory() == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!GuiUtil.titleMatches(player.getOpenInventory().getTitle(), TITLE)) {
            return;
        }
        Inventory refreshed = GuiBuilder.create(27, TITLE).filler(Material.BLACK_STAINED_GLASS_PANE).build();
        List<GuiWidget> widgets = buildWidgets();
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        render(refreshed, player, widgets);
        top.setContents(refreshed.getContents());
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
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        List<GuiWidget> widgets = widgetsByPlayer.get(player.getUniqueId());
        if (widgets == null) {
            return;
        }
        GuiContext context = new GuiContext(player, event.getView().getTopInventory());
        for (GuiWidget widget : widgets) {
            if (widget.handlesSlot(event.getRawSlot())) {
                widget.onClick(event.getRawSlot(), event.getClick(), context);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            widgetsByPlayer.remove(event.getPlayer().getUniqueId());
        }
    }
}
