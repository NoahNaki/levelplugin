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
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * GUI for identifying Unidentified Runes via a chest inventory.
 * Players insert unidentified rune items into slots 0-6 and click the Identify button at slot 8.
 */
public class IdentifyRunesGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_PURPLE + "Runecarver - Identify Runes";
    private static final int SIZE          = 54;  // 5 rows × 9 cols
    private static final int    IDENTIFY_SLOT  = 5 * 9 + 4;  // row 5, col 4 → slot 49
    private boolean animating = false;
    private BukkitTask animationTask;

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

    public void openInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);

        // 1) Prep our two pane-types
        ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = grayPane.getItemMeta();
        gm.setDisplayName(" ");
        grayPane.setItemMeta(gm);

        ItemStack purplePane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta pm = purplePane.getItemMeta();
        pm.setDisplayName(" ");
        purplePane.setItemMeta(pm);

        // 2) Fill rows 4 & 5 with gray
        for (int row = 4; row <= 5; row++) {
            for (int col = 0; col < 9; col++) {
                inv.setItem(row * 9 + col, grayPane);
            }
        }

        // 3) Overwrite the “+” around the eye to purple
        //    (row 4, cols 3,4,5) and (row 5, cols 3,5)
        int r4 = 4 * 9;
        inv.setItem(r4 + 3, purplePane);
        inv.setItem(r4 + 4, purplePane);
        inv.setItem(r4 + 5, purplePane);

        int r5 = 5 * 9;
        inv.setItem(r5 + 3, purplePane);
        inv.setItem(r5 + 5, purplePane);

        // 4) Finally place the Identify-Rune eye at slot 49
        ItemStack button = new ItemStack(Material.ENDER_EYE);
        ItemMeta bm = button.getItemMeta();
        bm.setDisplayName(ChatColor.GREEN + "Identify Runes");
        button.setItemMeta(bm);
        inv.setItem(IDENTIFY_SLOT, button);

        player.openInventory(inv);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // 1) Only our GUI
        if (!TITLE.equals(e.getView().getTitle())) return;
        // 2) Block during animation
        if (animating) {
            e.setCancelled(true);
            return;
        }

        Inventory top       = e.getView().getTopInventory();
        Inventory clicked   = e.getClickedInventory();
        int rawSlot         = e.getRawSlot();
        InventoryAction act = e.getAction();
        Player player       = (Player)e.getWhoClicked();

        // 3) Handle shift‐click from player→GUI
        if (clicked != top && act == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // only allow shift-click if item is a PAPER with your runeKey
            ItemStack current = e.getCurrentItem();
            boolean valid = current != null
                && current.getType() == Material.PAPER
                && current.hasItemMeta()
                && current.getItemMeta().getPersistentDataContainer()
                .has(runeKey, PersistentDataType.STRING);
            if (!valid) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Only unidentified runes may be shift-clicked into this GUI!");
            }
            return;
        }

        // 4) From here on, we're only dealing with clicks *inside* the GUI
        if (clicked != top) return;

        // 5) Block double-click collect-to-cursor
        if (act == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }

        // 6) Identify-eye slot
        if (rawSlot == IDENTIFY_SLOT) {
            if (act == InventoryAction.PICKUP_ALL) {
                e.setCancelled(true);
                List<ItemStack> outs = collectIdentifiedItems(top);
                if (outs.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "You have no unidentified runes to identify.");
                } else {
                    startCycleAnimation(player, top, outs);
                }
            } else {
                e.setCancelled(true);
            }
            return;
        }

        // 7) Pane-guard
        ItemStack clickedItem = top.getItem(rawSlot);
        if (clickedItem != null) {
            Material m = clickedItem.getType();
            if (m == Material.GRAY_STAINED_GLASS_PANE ||
                m == Material.PURPLE_STAINED_GLASS_PANE) {
                e.setCancelled(true);
                return;
            }
        }

        // 8) Input slots: only allow valid PAPER runes
        if (rawSlot >= 0 && rawSlot < IDENTIFY_SLOT) {
            switch (act) {
                case PLACE_ONE:
                case PLACE_ALL:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                case MOVE_TO_OTHER_INVENTORY:  // covers shift-click from within GUI
                case CLONE_STACK:               // creative
                    ItemStack toPlace;
                    if (act == InventoryAction.SWAP_WITH_CURSOR || act == InventoryAction.HOTBAR_SWAP) {
                        int hotbar = e.getHotbarButton();
                        toPlace = player.getInventory().getItem(hotbar);
                    } else {
                        toPlace = e.getCursor();
                    }
                    boolean validRune = toPlace != null
                        && toPlace.getType() == Material.PAPER
                        && toPlace.hasItemMeta()
                        && toPlace.getItemMeta().getPersistentDataContainer()
                        .has(runeKey, PersistentDataType.STRING);
                    if (!validRune) {
                        e.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Only unidentified runes may go into these slots!");
                    }
                    // otherwise let it through
                    break;
                default:
                    // all other actions (pickup, etc.) are fine
                    break;
            }
        }
    }



    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        // 1) Only our GUI
        if (!TITLE.equals(e.getView().getTitle())) return;
        // 2) Block during animation
        if (animating) {
            e.setCancelled(true);
            return;
        }

        Inventory top = e.getView().getTopInventory();
        Player player = (Player)e.getWhoClicked();

        for (int rawSlot : e.getRawSlots()) {
            // only care about slots in our GUI
            if (rawSlot < 0 || rawSlot >= SIZE) continue;

            // 3) Block drag onto identify-button
            if (rawSlot == IDENTIFY_SLOT) {
                e.setCancelled(true);
                return;
            }

            // 4) Block drag onto filler panes
            ItemStack existing = top.getItem(rawSlot);
            if (existing != null) {
                Material m = existing.getType();
                if (m == Material.GRAY_STAINED_GLASS_PANE ||
                    m == Material.PURPLE_STAINED_GLASS_PANE) {
                    e.setCancelled(true);
                    return;
                }
            }

            // 5) Input slots must only get valid runes
            if (rawSlot >= 0 && rawSlot < IDENTIFY_SLOT) {
                ItemStack cursor = e.getOldCursor();
                boolean valid = cursor != null
                    && cursor.getType() == Material.PAPER
                    && cursor.hasItemMeta()
                    && cursor.getItemMeta().getPersistentDataContainer()
                    .has(runeKey, PersistentDataType.STRING);
                if (!valid) {
                    e.setCancelled(true);
                    player.playSound(player.getLocation(),
                        Sound.BLOCK_ANVIL_LAND,
                        0.5f, 1f);
                    player.sendMessage(ChatColor.RED + "Only unidentified runes may go here!");
                    return;
                }
            }
        }
        // if none of the dragged slots are in the GUI we just let it through
    }


    private List<ItemStack> collectIdentifiedItems(Inventory top) {
        List<ItemStack> outputs = new ArrayList<>();

        for (int i = 0; i < IDENTIFY_SLOT; i++) {
            ItemStack in = top.getItem(i);
            if (in == null)                        continue;
            if (in.getType() != Material.PAPER)    continue;  // ✦ only paper runes
            if (!in.hasItemMeta())                 continue;

            PersistentDataContainer pdc = in.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(runeKey, PersistentDataType.STRING)) continue;

            String id = pdc.get(runeKey, PersistentDataType.STRING);
            Rune rune = runeLoader.getRune(id);
            if (rune == null)                      continue;

            int count = in.getAmount();
            top.setItem(i, null);                  // clear slot immediately

            for (int k = 0; k < count; k++) {
                outputs.add(createIdentifiedRuneItem(rune));
            }
        }

        return outputs;
    }


    private void startCycleAnimation(Player player, Inventory top, List<ItemStack> outputs) {
        animating = true;
        final int totalCycles   = 20;
        final long tickInterval = 2L;  // 0.1s per frame
        AtomicInteger frame     = new AtomicInteger(0);

        // 1) build the full list of candidate slots (rows 0–3)
        int rows         = SIZE / 9;       // e.g. 54/9 = 6
        int fillerStart  = rows - 2;      // rows 4 & 5 are filler
        List<Integer> inputSlots = new ArrayList<>();
        for (int row = 0; row < fillerStart; row++) {
            for (int col = 0; col < 9; col++) {
                inputSlots.add(row * 9 + col);
            }
        }

        // 2) but only animate as many as we have runes
        int want = Math.min(outputs.size(), inputSlots.size());
        List<Integer> slotsToAnimate = inputSlots.subList(0, want);

        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int f = frame.getAndIncrement();

            // clear the old frame
            for (int slot : slotsToAnimate) {
                top.setItem(slot, null);
            }

            // draw only the first 'want' placeholders
            for (int slot : slotsToAnimate) {
                Material mat = RUNE_TRIM_MATERIALS[
                    ThreadLocalRandom.current().nextInt(RUNE_TRIM_MATERIALS.length)
                    ];
                ItemStack fake = new ItemStack(mat);
                ItemMeta m = fake.getItemMeta();

                //  • name = "???"
                m.setDisplayName(ChatColor.GRAY + "???");
                //  • strip off any vanilla lore
                m.setLore(Collections.emptyList());

                m.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_ARMOR_TRIM,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP
                );

                fake.setItemMeta(m);
                top.setItem(slot, fake);
            }

            if (f >= totalCycles) {
                animationTask.cancel();
                finishAnimation(player, top, outputs);
            }
        }, 0L, tickInterval);
    }



    /**
     * After the cycle, give them their actual runes, send the message, and close.
     */
    private void finishAnimation(Player player, Inventory top, List<ItemStack> outputs) {
        animating = false;

        // clear any stragglers (should already be blank)
        for (int i = 0; i < IDENTIFY_SLOT; i++) {
            top.setItem(i, null);
        }

        // hand out the identified runes
        int total = 0;
        for (ItemStack out : outputs) {
            player.getInventory().addItem(out);
            total++;
        }

        player.sendMessage(ChatColor.GREEN + "Identified " + total + " rune(s)!");
        player.closeInventory();
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