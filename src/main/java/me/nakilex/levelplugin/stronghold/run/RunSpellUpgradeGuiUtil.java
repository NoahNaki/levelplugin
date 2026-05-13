package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.spells.SpellIconUtil;
import me.nakilex.levelplugin.spells.SpellRegistry;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Shared renderer for Stronghold-style run spell upgrade choice menus. */
public final class RunSpellUpgradeGuiUtil {
    public static final int GUI_SIZE = 45;
    public static final int[] CHOICE_SLOTS = {11, 13, 15};
    public static final int DEFAULT_REROLL_SLOT = 31;

    private RunSpellUpgradeGuiUtil() {
    }

    public record SpellUpgradeView(
            String displayName,
            String description,
            String baseSpellId,
            String resultSpellId,
            int currentRank,
            boolean unlock,
            List<String> extraDetails,
            String clickAction
    ) {}

    public static <T> void populateChoices(Inventory inv,
                                           List<T> choices,
                                           Function<T, ItemStack> renderer,
                                           boolean includeReroll) {
        populateChoices(inv, choices, renderer, includeReroll, DEFAULT_REROLL_SLOT);
    }

    public static <T> void populateChoices(Inventory inv,
                                           List<T> choices,
                                           Function<T, ItemStack> renderer,
                                           boolean includeReroll,
                                           int rerollSlot) {
        if (inv == null || choices == null || renderer == null) {
            return;
        }
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inv.getSize(); slot++) {
            inv.setItem(slot, filler);
        }
        for (int i = 0; i < CHOICE_SLOTS.length && i < choices.size(); i++) {
            inv.setItem(CHOICE_SLOTS[i], renderer.apply(choices.get(i)));
        }
        if (includeReroll && rerollSlot >= 0 && rerollSlot < inv.getSize()) {
            inv.setItem(rerollSlot, rerollItem());
        }
    }

    public static int choiceIndex(int slot) {
        for (int i = 0; i < CHOICE_SLOTS.length; i++) {
            if (CHOICE_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack createSpellUpgradeChoiceItem(SpellUpgradeView view) {
        if (view == null) {
            return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "Missing Upgrade", List.of());
        }
        int nextRank = view.unlock() ? 1 : Math.max(1, view.currentRank() + 1);
        ItemStack item = createSpellUpgradeIcon(view.displayName(), view.resultSpellId(), nextRank);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (meta.getDisplayName() == null || meta.getDisplayName().isBlank()) {
            meta.setDisplayName(ChatColor.GOLD + view.displayName());
        }
        List<String> lore = new ArrayList<>();
        appendWrappedBulletBlock(lore, view.description());
        lore.add(" ");
        lore.add(TooltipUtil.sectionHeader("Spell Upgrade"));
        lore.add(TooltipUtil.iconLabelValueLine("✣", ChatColor.GOLD, ChatColor.GRAY, "Current Rank",
                ChatColor.WHITE, String.valueOf(Math.max(0, view.currentRank()))));
        lore.add(TooltipUtil.iconLabelValueLine("✦", ChatColor.AQUA, ChatColor.GRAY, "Next Spell",
                ChatColor.WHITE, resolveUpgradeSpellDisplay(view.resultSpellId())));
        if (view.extraDetails() != null && !view.extraDetails().isEmpty()) {
            lore.addAll(view.extraDetails());
        }
        lore.add(" ");
        lore.add(TooltipUtil.sectionHeader("Spell Effect"));
        StrongholdSpellTooltipUtil.appendSpellEffectLore(lore, view.resultSpellId());
        lore.add(TooltipUtil.sectionHeader("Upgrade Effect"));
        StrongholdSpellTooltipUtil.appendUpgradeDeltaLore(lore, view.baseSpellId(), Math.max(0, view.currentRank()), view.unlock());
        lore.add(TooltipUtil.sectionDividerByPixels(150));
        lore.addAll(TooltipUtil.clickInstructions(
                view.clickAction() == null || view.clickAction().isBlank() ? "to choose this upgrade" : view.clickAction(), null));
        meta.setLore(lore);
        item.setItemMeta(meta);
        TooltipUtil.centerItemName(item);
        return item;
    }

    public static String resolveUpgradeSpellDisplay(String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return "Unknown";
        }
        SpellRegistry.SpellEntry entry = SpellRegistry.getInstance().getSpell(spellId);
        if (entry == null || entry.definition() == null || entry.definition().displayName() == null) {
            return TextUtil.beautifyWords(spellId);
        }
        return entry.definition().displayName();
    }

    public static ItemStack rerollItem() {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Refresh all three current");
        lore.add(ChatColor.GRAY + "upgrade choices.");
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to reroll options", null));
        return GuiUtil.getNexoItem("refresh", ChatColor.RED + "Reroll Upgrades", lore);
    }

    private static ItemStack createSpellUpgradeIcon(String displayName, String spellId, int nextRank) {
        return SpellIconUtil.createSpellIcon(spellId, displayName, nextRank);
    }

    private static void appendWrappedBulletBlock(List<String> lore, String description) {
        if (lore == null || description == null || description.isBlank()) {
            return;
        }
        List<String> wrapped = TooltipUtil.wrapLoreLine(ChatColor.GRAY + description.trim(), 210,
                ChatColor.DARK_GRAY + "  " + ChatColor.GRAY);
        if (wrapped.isEmpty()) {
            return;
        }
        lore.add(TooltipUtil.bulletLine(wrapped.get(0)));
        for (int i = 1; i < wrapped.size(); i++) {
            lore.add(wrapped.get(i));
        }
    }


}
