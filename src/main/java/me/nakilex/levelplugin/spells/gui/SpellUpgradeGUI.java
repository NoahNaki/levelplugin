package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.spells.SpellProgression;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
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

public class SpellUpgradeGUI implements Listener {
    private static final String TITLE = "Spell Upgrades";
    private static final int[] SPELL_SLOTS = {11, 13, 15, 29, 31, 33};

    private final SpellProgressionManager progressionManager = SpellProgressionManager.getInstance();
    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public void open(Player player) {
        if (!ClassUtil.isMageFamily(PlayerClassManager.getInstance().getPlayerClass(player))) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Spell upgrades are currently available for mage classes.");
            return;
        }
        Inventory gui = GuiBuilder.create(45, TITLE).filler(Material.BLACK_STAINED_GLASS_PANE).build();
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
        List<String> spells = progressionManager.getClassBaseSpells(player);
        widgets.add(new ActionWidget(40, ctx -> createPointsItem(ctx.player()), null));
        for (int i = 0; i < spells.size() && i < SPELL_SLOTS.length; i++) {
            String spellId = spells.get(i);
            int slot = SPELL_SLOTS[i];
            widgets.add(new ActionWidget(slot,
                    ctx -> createSpellItem(ctx.player(), spellId),
                    (click, ctx) -> {
                        if (click.isRightClick()) {
                            if (progressionManager.refundPoint(ctx.player().getUniqueId(), spellId)) {
                                ChatMessageUtil.send(ctx.player(), ChatMessageUtil.MessageType.SUCCESS,
                                        "Refunded 1 spell point from " + getSpellName(spellId) + ".");
                                open(ctx.player());
                            } else {
                                ChatMessageUtil.send(ctx.player(), ChatMessageUtil.MessageType.WARNING,
                                        "No invested points to refund for this spell.");
                            }
                        } else {
                            if (progressionManager.investPoint(ctx.player().getUniqueId(), spellId)) {
                                ChatMessageUtil.send(ctx.player(), ChatMessageUtil.MessageType.SUCCESS,
                                        "Invested 1 spell point into " + getSpellName(spellId) + ".");
                                open(ctx.player());
                            } else {
                                ChatMessageUtil.send(ctx.player(), ChatMessageUtil.MessageType.WARNING,
                                        "Cannot invest in this spell right now.");
                            }
                        }
                    }));
        }
        return widgets;
    }

    private ItemStack createPointsItem(Player player) {
        int points = progressionManager.getSpellPoints(player.getUniqueId());
        return GuiUtil.createGuiItem(Material.NETHER_STAR, ChatColor.AQUA + "Spell Points",
                List.of(" ", ChatColor.GRAY + "Available: " + ChatColor.WHITE + points,
                        ChatColor.DARK_GRAY + "Invest points to empower mage spells."));
    }

    private ItemStack createSpellItem(Player player, String baseSpellId) {
        int level = progressionManager.getSpellLevel(player.getUniqueId(), baseSpellId);
        int max = progressionManager.getMaxLevel(baseSpellId);
        String name = getSpellName(baseSpellId);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current Tier: " + ChatColor.WHITE + tierName(level));
        lore.add(ChatColor.GRAY + "Progress: " + TooltipUtil.progressBar(level, Math.max(1, max), 12));
        lore.add(ChatColor.GRAY + "Invested: " + ChatColor.WHITE + level + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + max);
        lore.add(" ");
        SpellProgression progression = SpellRegistry.getInstance().getProgression(baseSpellId);
        if (progression != null) {
            lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Base: " + getSpellName(baseSpellId));
            for (int i = 0; i < progression.upgradeSpellIds().size(); i++) {
                lore.add(TooltipUtil.selectionLine(i < level,
                        ChatColor.GRAY + "Tier " + (i + 1) + ": " + getSpellName(progression.upgradeSpellIds().get(i))));
            }
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to invest 1 spell point", "to refund 1 spell point"));
        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE + name, lore);
    }

    private String getSpellName(String spellId) {
        SpellRegistry.SpellEntry entry = SpellRegistry.getInstance().getSpell(spellId);
        return entry == null ? spellId : entry.definition().displayName();
    }

    private String tierName(int level) {
        return switch (level) {
            case 0 -> "Base";
            case 1 -> "Advanced";
            default -> "Master";
        };
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
        if (GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            widgetsByPlayer.remove(event.getPlayer().getUniqueId());
        }
    }
}
