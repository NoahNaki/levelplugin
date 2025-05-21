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
    private static final int SIZE = 9;
    private static final int IDENTIFY_SLOT = 8;
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

        // 0) Block all interaction during the animation
        if (animating) {
            e.setCancelled(true);
            return;
        }

        Player player     = (Player) e.getWhoClicked();
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

            // 2a) Collect all the identified ItemStacks (but don't give them yet)
            List<ItemStack> outputs = collectIdentifiedItems(top);
            if (outputs.isEmpty()) {
                player.sendMessage(ChatColor.RED + "You have no unidentified runes to identify.");
                return;
            }

            // 2b) Kick off the cycling animation
            startCycleAnimation(player, top, outputs);
            return;
        }

        // 3) Otherwise allow the click, but schedule a bounce‐back check in 1 tick
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

    /**
     * Read slots 0..7, turn each uncarved rune into a fresh identified ItemStack.
     * We don’t give them to the player yet—we’ll do that after the animation.
     */
    private List<ItemStack> collectIdentifiedItems(Inventory top) {
        List<ItemStack> outputs = new ArrayList<>();
        for (int i = 0; i < IDENTIFY_SLOT; i++) {
            ItemStack in = top.getItem(i);
            if (in == null || !in.hasItemMeta()) continue;
            var pdc = in.getItemMeta().getPersistentDataContainer();
            if (!pdc.has(runeKey, PersistentDataType.STRING)) continue;

            Rune rune = runeLoader.getRune(pdc.get(runeKey, PersistentDataType.STRING));
            if (rune == null) continue;

            int count = in.getAmount();
            // clear now so that later you only see the cycling visuals
            top.setItem(i, null);

            // create N individual books so they stack properly
            for (int k = 0; k < count; k++) {
                outputs.add(createIdentifiedRuneItem(rune));
            }
        }
        return outputs;
    }

    private void startCycleAnimation(Player player, Inventory top, List<ItemStack> outputs) {
        animating = true;
        final int totalCycles = 20;      // number of frames
        final long tickInterval = 2L;    // every 2 ticks (~0.1s)
        AtomicInteger frame = new AtomicInteger(0);

        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int f = frame.getAndIncrement();

            // cycle each slot through a random trim material
            for (int i = 0; i < IDENTIFY_SLOT; i++) {
                Material mat = RUNE_TRIM_MATERIALS[
                    ThreadLocalRandom.current().nextInt(RUNE_TRIM_MATERIALS.length)
                    ];
                ItemStack fake = new ItemStack(mat);
                ItemMeta m = fake.getItemMeta();
                m.setDisplayName(ChatColor.GRAY + "???");
                fake.setItemMeta(m);
                top.setItem(i, fake);
            }

            // once we've shown enough frames, finish up
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