package me.nakilex.levelplugin.spells.gui;

import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.ClassUtil;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.spells.input.SpellInputType;
import me.nakilex.levelplugin.spells.progression.SpellProgressionManager;
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
        lore.add(" ");
        lore.addAll(describeSpell(playerClass, spellId));
        lore.add(" ");

        String damageLine = estimateDamageLine(player, playerClass, spellId, inputType);
        String elementLabel = ClassUtil.isMageFamily(playerClass) ? "Thunder" : "Power";
        lore.add(ChatColor.GRAY + "Either way, gain " + ChatColor.WHITE + "+50 "
                + ChatColor.GOLD + "✣ " + ChatColor.GRAY + "Neutral and "
                + ChatColor.WHITE + "+10 " + ChatColor.YELLOW + "✦ " + ChatColor.GRAY + elementLabel);

        lore.add(ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Base Damage");
        if (damageLine != null) {
            lore.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED, "Total Damage",
                    ChatColor.WHITE, damageLine + ChatColor.GRAY + " (0 DEF estimate)"));
            lore.add(TooltipUtil.iconLabelValueLine("✣", ChatColor.GOLD, ChatColor.GOLD, "Damage",
                    ChatColor.WHITE, neutralPercentFor(playerClass)));
            lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.YELLOW, ChatColor.YELLOW, elementLabel,
                    ChatColor.WHITE, elementPercentFor(playerClass)));
        } else {
            lore.add(TooltipUtil.iconLabelValueLine("✖", ChatColor.RED, ChatColor.RED, "Total Damage",
                    ChatColor.GRAY, "Utility / non-damage spell"));
        }

        lore.add(" ");
        lore.add(ChatColor.BLUE + "" + ChatColor.BOLD + "??? Archetype");
        lore.add(ChatColor.GRAY + "Ability Points: " + ChatColor.WHITE
                + SpellProgressionManager.getInstance().getSpellPoints(player.getUniqueId()));
        lore.add(ChatColor.GRAY + "Min ??? Archetype: " + ChatColor.WHITE + "6");
        lore.add(ChatColor.DARK_GRAY + spellId);

        return GuiUtil.createGuiItem(Material.ENCHANTED_BOOK,
                ChatColor.GOLD + "" + ChatColor.BOLD + entry.definition().displayName(), lore);
    }

    private List<String> describeSpell(PlayerClass playerClass, String spellId) {
        List<String> lines = new ArrayList<>();
        if (spellId.startsWith("mage_heal")) {
            lines.add(ChatColor.GRAY + "Heals and cleanses nearby party members.");
            lines.add(TooltipUtil.labelValueLine("Range", ChatColor.AQUA, "10 blocks"));
            return lines;
        }
        if (spellId.startsWith("archer_windguard")) {
            lines.add(ChatColor.GRAY + "Party speed buff and cooldown reset.");
            lines.add(TooltipUtil.labelValueLine("Duration", ChatColor.AQUA, "5 seconds"));
            return lines;
        }
        if (spellId.startsWith("warrior_guarded_resolve")) {
            lines.add(ChatColor.GRAY + "Party ward that blocks 3 incoming hits.");
            lines.add(TooltipUtil.labelValueLine("Duration", ChatColor.AQUA, "5 seconds"));
            return lines;
        }
        if (spellId.startsWith("rogue_veil_counter")) {
            lines.add(ChatColor.GRAY + "Party buff: guaranteed crits + damage amp.");
            lines.add(TooltipUtil.labelValueLine("Duration", ChatColor.AQUA, "5 seconds"));
            return lines;
        }
        if (ClassUtil.isMageFamily(playerClass) && spellId.startsWith("mage_fireball")) {
            lines.add(ChatColor.GRAY + "Launches firebolts in front of you.");
            return lines;
        }
        if (ClassUtil.isArcherFamily(playerClass) && spellId.startsWith("archer_quickshot")) {
            lines.add(ChatColor.GRAY + "Basic arrow shot (airborne fires 3-cone).");
            return lines;
        }
        if (spellId.startsWith("mage_blink")) {
            lines.add(ChatColor.GRAY + "Teleport mobility spell.");
            return lines;
        }
        if (spellId.startsWith("blackhole")) {
            lines.add(ChatColor.GRAY + "Pulling control zone that damages over time.");
            return lines;
        }
        if (spellId.startsWith("meteor")) {
            lines.add(ChatColor.GRAY + "Delayed impact nuke with area damage.");
            return lines;
        }
        if (spellId.startsWith("archer_homing_barrage")) {
            lines.add(ChatColor.GRAY + "Fires multiple homing arrows.");
            return lines;
        }
        if (spellId.startsWith("archer_arrow_rain")) {
            lines.add(ChatColor.GRAY + "Rains arrows over a target area.");
            return lines;
        }
        if (spellId.startsWith("archer_skybound")) {
            lines.add(ChatColor.GRAY + "Aerial mobility and slam finisher.");
            return lines;
        }
        if (spellId.startsWith("warrior_execution_arc")) {
            lines.add(ChatColor.GRAY + "Cyclone slash that pulls and chips enemies.");
            return lines;
        }
        if (spellId.startsWith("warrior_rupture_cyclone")) {
            lines.add(ChatColor.GRAY + "Pulse cyclone burst around you.");
            return lines;
        }
        if (spellId.startsWith("warrior_titan_vault")) {
            lines.add(ChatColor.GRAY + "Leap, carry, and slam impact spell.");
            return lines;
        }
        if (spellId.startsWith("rogue_sky_ripper")) {
            lines.add(ChatColor.GRAY + "Aerial multi-hit execution combo.");
            return lines;
        }
        if (spellId.startsWith("rogue_phantom_cross")) {
            lines.add(ChatColor.GRAY + "Forward combo strike with finisher.");
            return lines;
        }
        if (spellId.startsWith("rogue_razor_dash")) {
            lines.add(ChatColor.GRAY + "Fast mobility slash dash.");
            return lines;
        }
        lines.add(ChatColor.GRAY + "Combat spell.");
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

    private String neutralPercentFor(PlayerClass playerClass) {
        if (ClassUtil.isMageFamily(playerClass)) {
            return "85%";
        }
        if (ClassUtil.isArcherFamily(playerClass)) {
            return "90%";
        }
        if (ClassUtil.isRogueFamily(playerClass)) {
            return "92%";
        }
        return "95%";
    }

    private String elementPercentFor(PlayerClass playerClass) {
        if (ClassUtil.isMageFamily(playerClass)) {
            return "15%";
        }
        if (ClassUtil.isArcherFamily(playerClass)) {
            return "10%";
        }
        if (ClassUtil.isRogueFamily(playerClass)) {
            return "8%";
        }
        return "5%";
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
