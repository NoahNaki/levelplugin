package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
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
    private static final int[] SPELL_SLOTS = {0, 2, 4, 6, 8};
    private static final SpellInputType[] SPELL_INPUTS = {
            SpellInputType.BASIC_ATTACK,
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
                    ctx -> createSpellItem(ctx.player(), playerClass, entry, input),
                    null));
        }
        return widgets;
    }

    private ItemStack createSpellItem(Player player,
                                      PlayerClass playerClass,
                                      SpellRegistry.SpellEntry entry,
                                      SpellInputType inputType) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + labelForInput(inputType));
        lore.add(TooltipUtil.sectionDivider());

        if (entry == null) {
            lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "No spell is currently bound."));
            return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "Unbound", lore);
        }

        String spellId = entry.definition().id();
        lore.addAll(describeSpell(playerClass, spellId));
        String damageLine = estimateDamageLine(player, playerClass, spellId, inputType);
        if (damageLine != null) {
            lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "0 DEF Estimate: " + ChatColor.RED + damageLine));
        }

        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + entry.definition().displayName(), lore);
    }

    private List<String> describeSpell(PlayerClass playerClass, String spellId) {
        List<String> lines = new ArrayList<>();
        if (spellId.startsWith("mage_heal")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Heals and cleanses nearby party members."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Range: " + ChatColor.AQUA + "10 blocks"));
            return lines;
        }
        if (spellId.startsWith("archer_windguard")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Party speed buff and cooldown reset."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Duration: " + ChatColor.AQUA + "5 seconds"));
            return lines;
        }
        if (spellId.startsWith("warrior_guarded_resolve")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Party ward that blocks 3 incoming hits."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Duration: " + ChatColor.AQUA + "5 seconds"));
            return lines;
        }
        if (spellId.startsWith("rogue_veil_counter")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Party buff: guaranteed crits + damage amp."));
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Duration: " + ChatColor.AQUA + "5 seconds"));
            return lines;
        }
        if (ClassUtil.isMageFamily(playerClass) && spellId.startsWith("mage_fireball")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Launches firebolts in front of you."));
            return lines;
        }
        if (ClassUtil.isArcherFamily(playerClass) && spellId.startsWith("archer_quickshot")) {
            lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Basic arrow shot (airborne fires 3-cone)."));
            return lines;
        }
        lines.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Combat spell."));
        return lines;
    }

    private String estimateDamageLine(Player player, PlayerClass playerClass, String spellId, SpellInputType inputType) {
        var stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int intelligence = stats.baseIntelligence + stats.bonusIntelligence;
        int dexterity = stats.baseDexterity + stats.bonusDexterity;
        int technique = stats.baseTechnique + stats.bonusTechnique;
        int strength = stats.baseStrength + stats.bonusStrength;

        if (inputType == SpellInputType.BASIC_ATTACK && !ClassUtil.isMageFamily(playerClass)
                && !ClassUtil.isArcherFamily(playerClass) && !ClassUtil.isRogueFamily(playerClass)) {
            double base = (1.0 + (strength * 0.5)) * (1.0 + technique * 0.001) * 0.60;
            return String.format("%.1f avg hit", base);
        }

        if (spellId.startsWith("mage_fireball_basic")) {
            return String.format("%.1f / bolt", computeInt(intelligence, technique, 3.2, 0.48));
        }
        if (spellId.startsWith("mage_fireball_barrage")) {
            return String.format("%.1f / bolt", computeInt(intelligence, technique, 3.8, 0.58));
        }
        if (spellId.startsWith("mage_fireball_inferno")) {
            return String.format("%.1f / bolt", computeInt(intelligence, technique, 5.0, 0.72));
        }
        if (spellId.startsWith("meteor")) {
            return String.format("%.1f impact", 14.5);
        }

        if (spellId.startsWith("archer_quickshot_basic")) {
            return String.format("%.1f / arrow", computeDex(dexterity, technique, 3.4, 0.30));
        }
        if (spellId.startsWith("archer_quickshot_seeker")) {
            return String.format("%.1f / arrow", computeDex(dexterity, technique, 3.4, 0.30));
        }
        if (spellId.startsWith("archer_quickshot_payload")) {
            return String.format("%.1f / arrow", computeDex(dexterity, technique, 3.4, 0.30));
        }
        if (spellId.startsWith("archer_homing_barrage")) {
            return String.format("%.1f / arrow", computeDex(dexterity, technique, 3.8, 0.34));
        }
        if (spellId.startsWith("archer_arrow_rain")) {
            return String.format("%.1f / volley arrow", computeDex(dexterity, technique, 6.8, 0.30));
        }

        if (spellId.startsWith("rogue_arc_basic")) {
            return "5.0 slash";
        }
        if (spellId.startsWith("warrior_execution_arc")) {
            return "6.4 strike + 1.3 DoT ticks";
        }
        if (spellId.startsWith("warrior_rupture_cyclone")) {
            return "2.8+ pulse sequence";
        }
        if (spellId.startsWith("warrior_titan_vault")) {
            return "7.2 impact";
        }
        return null;
    }

    private String labelForInput(SpellInputType inputType) {
        return switch (inputType) {
            case BASIC_ATTACK -> "Basic Attack";
            case SPELL_1 -> "Spell 1";
            case SPELL_2 -> "Spell 2";
            case SPELL_3 -> "Spell 3";
            case SPELL_4 -> "Spell 4";
        };
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
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) {
            widgetsByPlayer.remove(event.getPlayer().getUniqueId());
        }
    }
}
