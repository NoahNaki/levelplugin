package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.spells.SpellDefinition;
import me.nakilex.levelplugin.spells.SpellEffectUtil;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

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
    private final Map<UUID, BukkitTask> refreshTasks = new HashMap<>();

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
        startAutoRefresh(player);
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
        lore.add(ChatColor.DARK_GRAY + "Input: " + ChatColor.WHITE + labelForInput(inputType));

        if (entry == null) {
            lore.add(" ");
            lore.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED, "Status",
                    ChatColor.GRAY, "No spell is currently bound."));
            return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "Unbound", lore);
        }

        String spellId = entry.definition().id();
        SpellDefinition spell = entry.definition();
        lore.add(" ");
        lore.addAll(describeSpell(player, playerClass, spellId));
        lore.add(" ");

        String damageLine = estimateDamageLine(player, playerClass, spellId, inputType);
        lore.add(ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Base Damage");
        if (damageLine != null) {
            lore.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED, "Total Damage",
                    ChatColor.WHITE, damageLine + ChatColor.GRAY + " (0 DEF estimate)"));
            lore.addAll(buildBaseDamageBreakdown(player, spellId));
        } else {
            lore.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED, "Total Damage",
                    ChatColor.GRAY, "Utility / non-damage spell"));
        }

        lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.AQUA, "Mana Cost",
                ChatColor.WHITE, String.valueOf(spell.baseManaCost())));
        lore.add(TooltipUtil.iconLabelValueLine("✣", ChatColor.GOLD, ChatColor.GOLD, "Spell Type",
                ChatColor.WHITE, spell.movementSpell() ? "Movement" : "Combat"));
        lore.add(ChatColor.DARK_GRAY + "ID: " + spellId);

        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + entry.definition().displayName(), lore);
    }

    private List<String> describeSpell(Player player, PlayerClass playerClass, String spellId) {
        List<String> lines = new ArrayList<>();
        if (spellId.startsWith("mage_heal")) {
            double heal = estimateHealAmount(player, spellId);
            addHighlightedDescription(lines,
                    "Heals allies for ",
                    ChatColor.GREEN,
                    String.format("%.1f HP", heal),
                    " and cleanses nearby party members from debuffs.");
            lines.add(TooltipUtil.labelValueLine("Support", ChatColor.AQUA, "10 block range, mana restore on cast"));
            return lines;
        }
        if (spellId.startsWith("archer_windguard")) {
            addHighlightedDescription(lines,
                    "Grants nearby allies ",
                    ChatColor.AQUA,
                    "Speed II",
                    " and clears active spell cooldown chains.");
            lines.add(TooltipUtil.labelValueLine("Duration", ChatColor.AQUA, "5.0 seconds"));
            return lines;
        }
        if (spellId.startsWith("warrior_guarded_resolve")) {
            addHighlightedDescription(lines,
                    "Applies a ward that blocks ",
                    ChatColor.YELLOW,
                    "3 incoming hits",
                    " before breaking.");
            lines.add(TooltipUtil.labelValueLine("Duration", ChatColor.AQUA, "5.0 seconds"));
            return lines;
        }
        if (spellId.startsWith("rogue_veil_counter")) {
            addHighlightedDescription(lines,
                    "Grants allies ",
                    ChatColor.GOLD,
                    "guaranteed crits",
                    " and doubles outgoing damage.");
            lines.add(TooltipUtil.labelValueLine("Duration", ChatColor.AQUA, "5.0 seconds"));
            return lines;
        }
        if (ClassUtil.isMageFamily(playerClass) && spellId.startsWith("mage_fireball")) {
            addHighlightedDescription(lines,
                    "Launches firebolts dealing ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.BASIC_ATTACK),
                    " to a 0 DEF target.");
            return lines;
        }
        if (ClassUtil.isArcherFamily(playerClass) && spellId.startsWith("archer_quickshot")) {
            addHighlightedDescription(lines,
                    "Fires arrows dealing ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.BASIC_ATTACK),
                    " (airborne shots fan into a 3-cone).");
            return lines;
        }
        if (spellId.startsWith("mage_blink")) {
            addHighlightedDescription(lines,
                    "Teleports you up to ",
                    ChatColor.AQUA,
                    String.format("%.1f blocks", blinkRangeFor(spellId)),
                    " to the nearest safe destination.");
            return lines;
        }
        if (spellId.startsWith("blackhole")) {
            addHighlightedDescription(lines,
                    "Creates a pull zone that deals ",
                    ChatColor.RED,
                    blackholeTickDamageFor(spellId),
                    " while enemies remain inside.");
            return lines;
        }
        if (spellId.startsWith("meteor")) {
            addHighlightedDescription(lines,
                    "Calls down an impact for ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.SPELL_2),
                    " in an area.");
            return lines;
        }
        if (spellId.startsWith("archer_homing_barrage")) {
            addHighlightedDescription(lines,
                    "Unleashes homing arrows for ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.SPELL_1),
                    " each.");
            return lines;
        }
        if (spellId.startsWith("archer_arrow_rain")) {
            addHighlightedDescription(lines,
                    "Bombards a target zone for ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.SPELL_2),
                    " per volley arrow.");
            return lines;
        }
        if (spellId.startsWith("archer_skybound")) {
            addHighlightedDescription(lines,
                    "Launches you upward, then slam for ",
                    ChatColor.RED,
                    "5.4 + fall scaling",
                    " on landing.");
            return lines;
        }
        if (spellId.startsWith("warrior_execution_arc")) {
            addHighlightedDescription(lines,
                    "Spins through enemies for ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.SPELL_1),
                    " and light pull pressure.");
            return lines;
        }
        if (spellId.startsWith("warrior_rupture_cyclone")) {
            addHighlightedDescription(lines,
                    "Pulses around you for ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.SPELL_2),
                    " across the full sequence.");
            return lines;
        }
        if (spellId.startsWith("warrior_titan_vault")) {
            addHighlightedDescription(lines,
                    "Leaps forward and slams for ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.SPELL_3),
                    " on impact.");
            return lines;
        }
        if (spellId.startsWith("rogue_sky_ripper")) {
            addHighlightedDescription(lines,
                    "Performs a multi-hit aerial combo for ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.SPELL_1),
                    " per major hit.");
            return lines;
        }
        if (spellId.startsWith("rogue_phantom_cross")) {
            addHighlightedDescription(lines,
                    "Dashes through with combo strikes for ",
                    ChatColor.RED,
                    estimateDamageLine(player, playerClass, spellId, SpellInputType.SPELL_2),
                    " on primary hits.");
            return lines;
        }
        if (spellId.startsWith("rogue_razor_dash")) {
            addHighlightedDescription(lines,
                    "Dashes forward with fast slashes dealing ",
                    ChatColor.RED,
                    "4.2 arc damage",
                    " on sweep contacts.");
            return lines;
        }
        lines.add(ChatColor.GRAY + "Combat spell.");
        return lines;
    }

    private void addHighlightedDescription(List<String> lines,
                                           String prefix,
                                           ChatColor valueColor,
                                           String value,
                                           String suffix) {
        String highlighted = ChatColor.GRAY + (prefix == null ? "" : prefix)
                + (valueColor == null ? ChatColor.WHITE : valueColor) + (value == null ? "" : value)
                + ChatColor.GRAY + (suffix == null ? "" : suffix);
        lines.addAll(TooltipUtil.wrapLoreLine(highlighted, 170, ChatColor.GRAY.toString()));
    }

    private double estimateHealAmount(Player player, String spellId) {
        double base = spellId.startsWith("mage_heal_rejuvenation") ? 11.0
                : spellId.startsWith("mage_heal_party") ? 9.0 : 8.0;
        return SpellEffectUtil.computeIntTecScaledDamage(player, base, 0.35, 0.0);
    }

    private double blinkRangeFor(String spellId) {
        if (spellId.startsWith("mage_blink_rift")) {
            return 14.0;
        }
        if (spellId.startsWith("mage_blink_phase")) {
            return 11.0;
        }
        return 8.0;
    }

    private String blackholeTickDamageFor(String spellId) {
        if (spellId.startsWith("blackhole_singularity")) {
            return "2.5 / tick (+9.5 collapse)";
        }
        if (spellId.startsWith("blackhole_gravitywell")) {
            return "1.8 / tick";
        }
        return "1.2 / tick";
    }

    private List<String> buildBaseDamageBreakdown(Player player, String spellId) {
        List<String> lines = new ArrayList<>();
        var stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int intelligence = stats.baseIntelligence + stats.bonusIntelligence;
        int dexterity = stats.baseDexterity + stats.bonusDexterity;
        int technique = stats.baseTechnique + stats.bonusTechnique;

        if (spellId.startsWith("mage_fireball_basic")) {
            lines.addAll(intScalingBreakdown(intelligence, technique, 3.2, 0.48, "/ bolt"));
            return lines;
        }
        if (spellId.startsWith("mage_fireball_barrage")) {
            lines.addAll(intScalingBreakdown(intelligence, technique, 3.8, 0.58, "/ bolt"));
            return lines;
        }
        if (spellId.startsWith("mage_fireball_inferno")) {
            lines.addAll(intScalingBreakdown(intelligence, technique, 5.0, 0.72, "/ bolt"));
            return lines;
        }
        if (spellId.startsWith("archer_quickshot_basic")
                || spellId.startsWith("archer_quickshot_seeker")
                || spellId.startsWith("archer_quickshot_payload")) {
            lines.addAll(dexScalingBreakdown(dexterity, technique, 3.4, 0.30, "/ arrow"));
            return lines;
        }
        if (spellId.startsWith("archer_homing_barrage")) {
            lines.addAll(dexScalingBreakdown(dexterity, technique, 3.8, 0.34, "/ arrow"));
            return lines;
        }
        if (spellId.startsWith("archer_arrow_rain")) {
            lines.addAll(dexScalingBreakdown(dexterity, technique, 6.8, 0.30, "/ volley arrow"));
            return lines;
        }
        return lines;
    }

    private List<String> intScalingBreakdown(int intelligence, int technique, double base, double intScale, String suffix) {
        double preTechnique = Math.max(0.0, base + intelligence * intScale);
        double techniqueMult = 1.0 + technique * 0.001;
        double finalValue = preTechnique * techniqueMult;
        List<String> lines = new ArrayList<>();
        lines.add(TooltipUtil.iconLabelValueLine("✣", ChatColor.GOLD, ChatColor.GOLD, "Base",
                ChatColor.WHITE, String.format("%.1f + INT(%d×%.2f) = %.1f", base, intelligence, intScale, preTechnique)));
        lines.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.AQUA, "Technique",
                ChatColor.WHITE, String.format("×(1 + %d×0.001) = ×%.3f", technique, techniqueMult)));
        lines.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED, "Final",
                ChatColor.WHITE, String.format("%.1f %s", finalValue, suffix == null ? "" : suffix)));
        return lines;
    }

    private List<String> dexScalingBreakdown(int dexterity, int technique, double base, double dexScale, String suffix) {
        double preTechnique = Math.max(0.0, base + dexterity * dexScale);
        double techniqueMult = 1.0 + technique * 0.001;
        double finalValue = preTechnique * techniqueMult;
        List<String> lines = new ArrayList<>();
        lines.add(TooltipUtil.iconLabelValueLine("✣", ChatColor.GOLD, ChatColor.GOLD, "Base",
                ChatColor.WHITE, String.format("%.1f + DEX(%d×%.2f) = %.1f", base, dexterity, dexScale, preTechnique)));
        lines.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.AQUA, "Technique",
                ChatColor.WHITE, String.format("×(1 + %d×0.001) = ×%.3f", technique, techniqueMult)));
        lines.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED, "Final",
                ChatColor.WHITE, String.format("%.1f %s", finalValue, suffix == null ? "" : suffix)));
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
        if (spellId.startsWith("meteor_big")) {
            return String.format("%.1f impact", 23.0);
        }
        if (spellId.startsWith("meteor_double")) {
            return String.format("%.1f impact", 18.0);
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
        if (spellId.startsWith("rogue_sky_ripper")) {
            return "7.4 combo hit";
        }
        if (spellId.startsWith("rogue_phantom_cross")) {
            return "7.2 strike";
        }
        if (spellId.startsWith("rogue_razor_dash")) {
            return "Dash utility";
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


    private void startAutoRefresh(Player player) {
        stopAutoRefresh(player.getUniqueId());
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(me.nakilex.levelplugin.Main.getInstance(), () -> {
            if (player == null || !player.isOnline()) {
                stopAutoRefresh(player == null ? null : player.getUniqueId());
                return;
            }
            if (!GuiUtil.titleMatches(player.getOpenInventory().getTitle(), TITLE)) {
                stopAutoRefresh(player.getUniqueId());
                return;
            }
            Inventory top = player.getOpenInventory().getTopInventory();
            Inventory refreshed = GuiBuilder.create(9, TITLE).filler(Material.BLACK_STAINED_GLASS_PANE).build();
            List<GuiWidget> widgets = buildWidgets(player);
            widgetsByPlayer.put(player.getUniqueId(), widgets);
            GuiLayout layout = new GuiLayout(refreshed);
            GuiContext context = new GuiContext(player, refreshed);
            for (GuiWidget widget : widgets) {
                widget.contribute(layout, context);
            }
            top.setContents(refreshed.getContents());
        }, 20L, 20L);
        refreshTasks.put(player.getUniqueId(), task);
    }

    private void stopAutoRefresh(UUID playerId) {
        if (playerId == null) {
            return;
        }
        BukkitTask task = refreshTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
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
            UUID id = event.getPlayer().getUniqueId();
            widgetsByPlayer.remove(id);
            stopAutoRefresh(id);
        }
    }
}
