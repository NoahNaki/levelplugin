package me.nakilex.levelplugin.runes.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.loader.RuneLoader;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * GUI for identifying Unidentified Runes via a chest inventory.
 * Players insert unidentified rune items into slots 0-6 and click the Identify button at slot 8.
 */
public class IdentifyRunesGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_PURPLE + "Runecarver - Identify Runes";
    private static final int SIZE = 9;
    private static final int IDENTIFY_SLOT = 8;

    private final Main plugin;
    private final RuneLoader runeLoader;
    private final NamespacedKey runeKey;

    public IdentifyRunesGUI(Main plugin, RunesManager runesManager) {
        this.plugin    = plugin;
        this.runeLoader = new RuneLoader(plugin);
        this.runeKey    = new NamespacedKey(plugin, "rune_id");

        this.runeLoader.loadRunes();

        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Opens the Identify GUI for the player. */
    public void openInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        // Place Identify button
        ItemStack button = new ItemStack(org.bukkit.Material.ANVIL);
        ItemMeta bm = button.getItemMeta();
        bm.setDisplayName(ChatColor.GREEN + "Identify Runes");
        button.setItemMeta(bm);
        inv.setItem(IDENTIFY_SLOT, button);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // Only care about our Identify GUI
        if (!TITLE.equals(e.getView().getTitle())) return;

        Player player     = (Player)e.getWhoClicked();
        Inventory top     = e.getView().getTopInventory();
        Inventory clicked = e.getClickedInventory();
        int rawSlot       = e.getRawSlot();
        InventoryAction act = e.getAction();

        // 1) Prevent direct placing of invalid items into slots 0..7
        if (clicked == top && rawSlot >= 0 && rawSlot < IDENTIFY_SLOT) {
            if (act.name().startsWith("PLACE") || act == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack toPlace = e.getCursor();
                boolean validUncarved =
                    toPlace != null &&
                        toPlace.getType() == Material.PAPER &&
                        toPlace.hasItemMeta() &&
                        toPlace.getItemMeta()
                            .getPersistentDataContainer()
                            .has(runeKey, PersistentDataType.STRING);

                if (!validUncarved) {
                    e.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Only unidentified runes may go into these slots!");
                    return;
                }
            }
        }

        // 2) Identify button click at slot 8
        if (clicked == top
            && rawSlot == IDENTIFY_SLOT
            && act == InventoryAction.PICKUP_ALL
        ) {
            e.setCancelled(true);

            // we'll record each individual identified rune ID, including stacks
            List<String> identified = new ArrayList<>();
            for (int i = 0; i < IDENTIFY_SLOT; i++) {
                ItemStack in = top.getItem(i);
                if (in == null || !in.hasItemMeta()) continue;

                var pdc = in.getItemMeta().getPersistentDataContainer();
                if (!pdc.has(runeKey, PersistentDataType.STRING)) continue;

                String id = pdc.get(runeKey, PersistentDataType.STRING);
                Rune rune = runeLoader.getRune(id);
                if (rune == null) continue;

                // capture how many were in that stack
                int count = in.getAmount();
                // clear the slot
                top.setItem(i, null);

                // create one identified Book, then set its count
                ItemStack out = createIdentifiedRuneItem(rune);
                out.setAmount(count);

                // give it to player
                player.getInventory().addItem(out);

                // record each individually (for message/counter)
                for (int k = 0; k < count; k++) {
                    identified.add(id);
                }
            }

            if (identified.isEmpty()) {
                player.sendMessage("§cYou have no unidentified runes to identify.");
            } else {
                player.sendMessage("§aIdentified " +
                    identified.size() + " rune(s): " + identified);
                player.closeInventory();
            }
            return;
        }

        // 3) Otherwise we allow the click, but schedule a bounce‐back check in 1 tick
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < IDENTIFY_SLOT; i++) {
                ItemStack in = top.getItem(i);
                if (in == null) continue;

                boolean valid =
                    in.getType() == Material.PAPER &&
                        in.hasItemMeta() &&
                        in.getItemMeta()
                            .getPersistentDataContainer()
                            .has(runeKey, PersistentDataType.STRING);

                if (!valid) {
                    top.setItem(i, null);
                    player.getInventory().addItem(in);
                    player.sendMessage(ChatColor.RED +
                        "Only unidentified runes may stay in this GUI. Returning invalid item.");
                }
            }
        }, 1L);
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!TITLE.equals(e.getView().getTitle())) return;

        Player player = (Player) e.getWhoClicked();
        for (int raw : e.getRawSlots()) {
            if (raw >= 0 && raw < IDENTIFY_SLOT) {
                // somebody is dragging something into an input slot
                ItemStack cursor = e.getOldCursor();
                boolean validUncarved =
                    cursor != null &&
                        cursor.getType() == Material.PAPER &&
                        cursor.hasItemMeta() &&
                        cursor.getItemMeta()
                            .getPersistentDataContainer()
                            .has(runeKey, PersistentDataType.STRING);

                if (!validUncarved) {
                    e.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1f);
                    player.sendMessage(ChatColor.RED + "Only unidentified runes may go here!");
                    return;
                }
            }
        }
    }

    private static final Material[] RUNE_TRIM_MATERIALS = {
        Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
        Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE
    };

    private Material chooseRuneMaterial(Rune rune) {
        int idx = Math.abs(rune.getId().hashCode()) % RUNE_TRIM_MATERIALS.length;
        return RUNE_TRIM_MATERIALS[idx];
    }

    ItemStack createIdentifiedRuneItem(Rune rune) {
        // 1) Create book & grab meta
        Material mat = chooseRuneMaterial(rune);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_ARMOR_TRIM,
            ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );

        // 2) Rarity → colour
        ChatColor rarityColor;
        switch (rune.getRarity()) {
            case COMMON:    rarityColor = ChatColor.GRAY;         break;
            case UNCOMMON:  rarityColor = ChatColor.GREEN;        break;
            case RARE:      rarityColor = ChatColor.BLUE;         break;
            case EPIC:      rarityColor = ChatColor.LIGHT_PURPLE; break;
            case LEGENDARY: rarityColor = ChatColor.GOLD;         break;
            default:        rarityColor = ChatColor.WHITE;        break;
        }

        // 3) Name in rarity colour
        meta.setDisplayName(rarityColor + rune.getDisplayName());

        // 4) Build lore
        List<String> lore = new ArrayList<>();

        // 4a) blank spacer
        lore.add("");

        // 4b) effect lines: static in gray (&7), dynamic in yellow (&e)
        // 4b) Effect lines: static in gray (&7), spell & numbers in yellow (&e)
        for (var e : rune.getEffects()) {
            switch (e.getType()) {
                case MODIFIER:
                    // "Increases " (gray) + Spell (yellow) + " damage by " (gray) + X% (yellow)
                    lore.add(
                        ChatColor.GRAY + "Increases "
                            + ChatColor.YELLOW + rune.getTargetSpell()
                            + ChatColor.GRAY + " damage by "
                            + ChatColor.YELLOW + String.format("%.1f%%", e.getBonusDamagePercent())
                    );
                    if (e.getCooldownReductionPercent() > 0) {
                        // "Reduces " + Spell + " cooldown by " + X%
                        lore.add(
                            ChatColor.GRAY + "Reduces "
                                + ChatColor.YELLOW + rune.getTargetSpell()
                                + ChatColor.GRAY + " cooldown by "
                                + ChatColor.YELLOW + String.format("%.1f%%", e.getCooldownReductionPercent())
                        );
                    }
                    break;

                case TRANSFORM:
                    // prettify enum key
                    String pretty = Arrays.stream(e.getNewEffectKey().split("_"))
                        .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase())
                        .collect(Collectors.joining(" "));
                    // "Transforms " + Spell + " into " + NewEffect
                    lore.add(
                        ChatColor.GRAY + "Transforms "
                            + ChatColor.YELLOW + rune.getTargetSpell()
                            + ChatColor.GRAY + " into "
                            + ChatColor.YELLOW + pretty
                    );
                    break;

                // … other effect types …
            }
        }


        // 4c) blank spacer
        lore.add("");

        // 4d) bottom line: if unique, prefix "UNIQUE "
        String bottomLabel = (rune.isUnique() ? "UNIQUE " : "")
            + rune.getRarity().name()
            + " RUNE";
        lore.add(
            rarityColor.toString()
                + ChatColor.BOLD
                + bottomLabel
        );

        meta.setLore(lore);

        // 5) Store rune ID in PDC
        meta.getPersistentDataContainer()
            .set(runeKey, PersistentDataType.STRING, rune.getId());

        item.setItemMeta(meta);
        return item;
    }
}