package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.spells.SpellRegistry;
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
    private static final String TITLE = "Spells";
    private static final int[] SPELL_SLOTS = {0, 2, 4, 6};
    private static final SpellInputType[] SPELL_INPUTS = {
            SpellInputType.SPELL_1,
            SpellInputType.SPELL_2,
            SpellInputType.SPELL_3,
            SpellInputType.SPELL_4
    };

    private final Map<UUID, List<GuiWidget>> widgetsByPlayer = new HashMap<>();

    public void open(Player player) {
        Inventory gui = GuiBuilder.create(9, TITLE).filler(Material.BLACK_STAINED_GLASS_PANE).build();
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
        PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
        SpellRegistry registry = SpellRegistry.getInstance();

        for (int i = 0; i < SPELL_INPUTS.length; i++) {
            SpellInputType input = SPELL_INPUTS[i];
            int slot = SPELL_SLOTS[i];
            SpellRegistry.SpellEntry entry = registry.resolveSpell(playerClass, null, null, input);
            widgets.add(new ActionWidget(slot,
                    ctx -> createSpellItem(ctx.player(), entry, input),
                    null));
        }
        return widgets;
    }

    private ItemStack createSpellItem(Player player, SpellRegistry.SpellEntry entry, SpellInputType inputType) {
        if (entry == null) {
            return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "Unbound Spell",
                    List.of(" ", ChatColor.GRAY + "Input: " + ChatColor.WHITE + inputType.name(),
                            ChatColor.DARK_GRAY + "No spell is currently bound."));
        }

        String spellId = entry.definition().id();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Input: " + ChatColor.WHITE + inputType.name());
        lore.add(" ");
        lore.addAll(describeSpell(player, spellId));

        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + entry.definition().displayName(), lore);
    }

    private List<String> describeSpell(Player player, String spellId) {
        List<String> lines = new ArrayList<>();
        var stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int intelligence = stats.baseIntelligence + stats.bonusIntelligence;
        int dexterity = stats.baseDexterity + stats.bonusDexterity;
        int technique = stats.baseTechnique + stats.bonusTechnique;

        if (spellId.startsWith("mage_heal")) {
            double heal = computeInt(intelligence, technique, 9.0, 0.35);
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Heals and cleanses nearby party members."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Range: " + ChatColor.AQUA + "10 blocks"));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Healing: " + ChatColor.GREEN + String.format("%.1f", heal)));
            return lines;
        }
        if (spellId.startsWith("blackhole")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Creates a pulling singularity."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Control-focused damage over time."));
            return lines;
        }
        if (spellId.startsWith("meteor")) {
            double damage = computeInt(intelligence, technique, 14.5, 0.0);
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Calls down a meteor strike."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Impact Damage: " + ChatColor.RED + String.format("%.1f", damage)));
            return lines;
        }
        if (spellId.startsWith("mage_blink")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Short range mobility blink."));
            return lines;
        }

        if (spellId.startsWith("archer_homing_barrage")) {
            double damage = computeDex(dexterity, technique, 3.8, 0.34);
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Fires a homing arrow barrage."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Per Arrow Damage: " + ChatColor.RED + String.format("%.1f", damage)));
            return lines;
        }
        if (spellId.startsWith("archer_arrow_rain")) {
            double damage = computeDex(dexterity, technique, 6.8, 0.30);
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Calls a rain of arrows on the target area."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Volley Damage: " + ChatColor.RED + String.format("%.1f", damage)));
            return lines;
        }
        if (spellId.startsWith("archer_skybound")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Launch upward and slam down from air."));
            return lines;
        }
        if (spellId.startsWith("archer_windguard")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Party speed buff + cooldown reset."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Range: " + ChatColor.AQUA + "30 blocks"));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Duration: " + ChatColor.AQUA + "5 seconds"));
            return lines;
        }

        if (spellId.startsWith("warrior_execution_arc")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Cyclone that pulls enemies and shreds nearby targets."));
            return lines;
        }
        if (spellId.startsWith("warrior_rupture_cyclone")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Cyclone pulse burst around the caster."));
            return lines;
        }
        if (spellId.startsWith("warrior_titan_vault")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Leap slam that can carry one enemy."));
            return lines;
        }
        if (spellId.startsWith("warrior_guarded_resolve")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Party ward: blocks 3 incoming hits."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Range: " + ChatColor.AQUA + "30 blocks"));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Duration: " + ChatColor.AQUA + "5 seconds"));
            return lines;
        }

        if (spellId.startsWith("rogue_sky_ripper") || spellId.startsWith("rogue_phantom_cross")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Burst melee combo spell."));
            return lines;
        }
        if (spellId.startsWith("rogue_razor_dash")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "High-speed mobility dash."));
            return lines;
        }
        if (spellId.startsWith("rogue_veil_counter")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Party crit + damage amplification buff."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Range: " + ChatColor.AQUA + "30 blocks"));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Duration: " + ChatColor.AQUA + "5 seconds"));
            return lines;
        }

        lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Spell details unavailable."));
        return lines;
    }

    private double computeInt(int intelligence, int technique, double base, double intScale) {
        double value = Math.max(0.0, base + intelligence * intScale);
        return value * (1.0 + technique * 0.001);
    }

    private double computeDex(int dexterity, int technique, double base, double dexScale) {
        double value = Math.max(0.0, base + dexterity * dexScale);
        return value * (1.0 + technique * 0.001);
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
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            widgetsByPlayer.remove(event.getPlayer().getUniqueId());
        }
    }
}
