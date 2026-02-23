package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
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
                                refresh(ctx.player());
                            } else {
                                ChatMessageUtil.send(ctx.player(), ChatMessageUtil.MessageType.WARNING,
                                        "No invested points to refund for this spell.");
                            }
                        } else {
                            if (progressionManager.investPoint(ctx.player().getUniqueId(), spellId)) {
                                ChatMessageUtil.send(ctx.player(), ChatMessageUtil.MessageType.SUCCESS,
                                        "Invested 1 spell point into " + getSpellName(spellId) + ".");
                                refresh(ctx.player());
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
        String effectiveSpellId = progressionManager.getEffectiveSpellId(player.getUniqueId(), baseSpellId);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current Tier: " + ChatColor.LIGHT_PURPLE + tierName(level));
        lore.add(ChatColor.GRAY + "Progress: " + TooltipUtil.expProgressBar(level, Math.max(1, max), 14));
        lore.add(ChatColor.GRAY + "Invested: " + ChatColor.WHITE + level + ChatColor.DARK_GRAY + "/" + ChatColor.WHITE + max);
        lore.add(" ");
        lore.addAll(describeSpell(player, baseSpellId, effectiveSpellId));
        lore.add(" ");
        SpellProgression progression = SpellRegistry.getInstance().getProgression(baseSpellId);
        if (progression != null) {
            lore.add(ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + "Base: " + getSpellName(baseSpellId));
            for (int i = 0; i < progression.upgradeSpellIds().size(); i++) {
                lore.add(TooltipUtil.selectionLine(i < level,
                        ChatColor.GRAY + "Tier " + (i + 1) + ": " + getSpellName(progression.upgradeSpellIds().get(i))));
                lore.add(ChatColor.DARK_GRAY + "   ↳ " + ChatColor.GRAY + describeUpgradeTier(baseSpellId, i + 1));
            }
        }
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to invest 1 spell point", "to refund 1 spell point"));
        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE + name, lore);
    }

    private List<String> describeSpell(Player player, String baseSpellId, String effectiveSpellId) {
        List<String> lines = new ArrayList<>();
        var stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int intelligence = stats.baseIntelligence + stats.bonusIntelligence;
        int technique = stats.baseTechnique + stats.bonusTechnique;

        if (baseSpellId.startsWith("mage_fireball")) {
            double damage = compute(intelligence, technique, effectiveSpellId.contains("inferno") ? 5.0 : effectiveSpellId.contains("barrage") ? 3.8 : 3.2,
                    effectiveSpellId.contains("inferno") ? 0.72 : effectiveSpellId.contains("barrage") ? 0.58 : 0.48);
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Hurls " + ChatColor.GOLD + (effectiveSpellId.contains("barrage") || effectiveSpellId.contains("inferno") ? "3" : "1")
                    + ChatColor.GRAY + " fireballs in a cone."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Direct Damage: " + ChatColor.RED + String.format("%.1f", damage)));
            if (effectiveSpellId.contains("barrage") || effectiveSpellId.contains("inferno")) {
                lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Explosion Splash: " + ChatColor.GOLD + (effectiveSpellId.contains("inferno") ? "Heavy" : "Medium")));
            }
            return lines;
        }
        if (baseSpellId.startsWith("meteor")) {
            double damage = compute(intelligence, technique, effectiveSpellId.contains("big") ? 23.0 : effectiveSpellId.contains("double") ? 18.0 : 14.5,
                    0.0);
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Calls down a " + ChatColor.GOLD + "devastating meteor" + ChatColor.GRAY + "."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Impact Damage: " + ChatColor.RED + String.format("%.1f", damage)));
            return lines;
        }
        if (baseSpellId.startsWith("blackhole")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Creates a " + ChatColor.DARK_PURPLE + "pulling singularity" + ChatColor.GRAY + "."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Core DoT and pull strength increase per tier."));
            return lines;
        }
        if (baseSpellId.startsWith("mage_heal")) {
            double heal = compute(intelligence, technique, effectiveSpellId.contains("rejuvenation") ? 11.0 : effectiveSpellId.contains("party") ? 9.0 : 8.0,
                    0.35);
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Restores health and grants support effects."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Healing: " + ChatColor.GREEN + String.format("%.1f", heal)));
            return lines;
        }
        if (baseSpellId.startsWith("mage_blink")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Teleports to a safe location with momentum."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Higher tiers add damage pathing and buffs."));
        }
        return lines;
    }

    private double compute(int intelligence, int technique, double base, double intScale) {
        double value = Math.max(0.0, base + intelligence * intScale);
        return value * (1.0 + technique * 0.001);
    }

    private String describeUpgradeTier(String baseSpellId, int tier) {
        if (baseSpellId.startsWith("mage_fireball")) {
            return tier == 1
                    ? "Unlocks 3-shot cone + medium splash explosion."
                    : "Upgrades to inferno volley: larger splash and stronger burn.";
        }
        if (baseSpellId.startsWith("blackhole")) {
            return tier == 1
                    ? "Wider pull radius and stronger core damage-over-time."
                    : "Singularity collapse detonates at the end for burst damage.";
        }
        if (baseSpellId.startsWith("mage_heal")) {
            return tier == 1
                    ? "Adds stronger heal burst, regeneration and larger mana restore."
                    : "Converts to party pulse heal with shared support effects.";
        }
        if (baseSpellId.startsWith("mage_blink")) {
            return tier == 1
                    ? "Leaves a damaging rift trail along your blink path."
                    : "Adds post-blink defensive buffs (speed + resistance).";
        }
        if (baseSpellId.startsWith("meteor")) {
            return tier == 1
                    ? "Bigger impact radius with stronger impact and DoT damage."
                    : "Cataclysm tier massively increases radius and impact damage.";
        }
        return "Enhances this spell's power and utility.";
    }

    private void refresh(Player player) {
        Inventory gui = GuiBuilder.create(45, TITLE).filler(Material.BLACK_STAINED_GLASS_PANE).build();
        List<GuiWidget> widgets = buildWidgets(player);
        widgetsByPlayer.put(player.getUniqueId(), widgets);
        GuiLayout layout = new GuiLayout(gui);
        GuiContext context = new GuiContext(player, gui);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
        if (GuiUtil.titleMatches(player.getOpenInventory().getTitle(), TITLE)
                && player.getOpenInventory().getTopInventory().getSize() == gui.getSize()) {
            player.getOpenInventory().getTopInventory().setContents(gui.getContents());
        } else {
            player.openInventory(gui);
        }
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
