package me.nakilex.levelplugin.player.classes.essence.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.SealingCharm;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TextUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

/**
 * Blacksmith-style GUI for investing duplicate essences and upgrading stars.
 */
public class ClassEssenceUpgradeGUI implements Listener {

    private static final String INVEST_TITLE = "Essence: Invest";
    private static final String STAR_TITLE = "Essence: Star Upgrade";
    private static final String RESEAL_TITLE = "Essence: Reseal";

    private static final int GUI_SIZE = 45;
    private static final int SACRIFICE_SLOT = 11;
    private static final int TARGET_SLOT = 15;
    private static final int STAR_SLOT = 13;
    private static final int RESEAL_CHARM_SLOT = 11;
    private static final int RESEAL_ESSENCE_SLOT = 13;
    private static final int INVEST_ARROW_SLOT = 13;
    private static final int CONFIRM_SLOT = 22;
    private static final int LEFT_ARROW_SLOT = 9;
    private static final int RIGHT_ARROW_SLOT = 17;
    private static final int INVEST_ALL_SLOT = 31;
    private static final int INVEST_EQUIPPED_SLOT = 32;
    private static final int INVEST_INFO_SLOT = 30;

    private static final Set<UUID> SWITCHING = new HashSet<>();

    private enum Mode {
        INVEST(INVEST_TITLE, "Invest"),
        STAR(STAR_TITLE, "Star Upgrade"),
        RESEAL(RESEAL_TITLE, "Reseal");

        private final String title;
        private final String display;

        Mode(String title, String display) {
            this.title = title;
            this.display = display;
        }

        Mode next() {
            return switch (this) {
                case INVEST -> STAR;
                case STAR -> RESEAL;
                case RESEAL -> INVEST;
            };
        }

        Mode previous() {
            return switch (this) {
                case INVEST -> RESEAL;
                case STAR -> INVEST;
                case RESEAL -> STAR;
            };
        }

        String title() {
            return title;
        }

        String display() {
            return display;
        }
    }

    private record ResealCost(int coins, int charms) {}

    private static void setNavigationArrows(Inventory gui, Mode mode) {
        Mode prev = mode.previous();
        Mode next = mode.next();
        gui.setItem(LEFT_ARROW_SLOT, navItem(prev, true));
        gui.setItem(RIGHT_ARROW_SLOT, navItem(next, false));
    }

    private static ItemStack navItem(Mode target, boolean left) {
        String glyph = left ? "arrow_left" : "arrow_right";
        return GuiUtil.getNexoItem(glyph, ChatColor.GRAY + target.display());
    }

    private static Mode modeFromTitle(String title) {
        if (INVEST_TITLE.equals(title)) return Mode.INVEST;
        if (STAR_TITLE.equals(title)) return Mode.STAR;
        if (RESEAL_TITLE.equals(title)) return Mode.RESEAL;
        return null;
    }

    private static ResealCost getResealCost(ItemRarity rarity) {
        int tier = Math.max(1, rarity.ordinal() + 1);
        int coins = 500 * tier;
        return new ResealCost(coins, tier);
    }

    public static void openInvest(Player player, ItemStack target) {
        Inventory gui = GuiBuilder.create(GUI_SIZE, INVEST_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        setNavigationArrows(gui, Mode.INVEST);
        gui.setItem(SACRIFICE_SLOT, null);
        gui.setItem(TARGET_SLOT, target);
        gui.setItem(INVEST_ARROW_SLOT, createInvestArrow());
        gui.setItem(CONFIRM_SLOT, GuiUtil.getNexoItem("check", ChatColor.GREEN + "Invest"));
        gui.setItem(INVEST_ALL_SLOT, createInvestAllButton());
        gui.setItem(INVEST_EQUIPPED_SLOT, createInvestEquippedButton());
        gui.setItem(INVEST_INFO_SLOT, createInvestInfo());
        player.openInventory(gui);
    }

    public static void openStar(Player player, ItemStack target) {
        Inventory gui = GuiBuilder.create(GUI_SIZE, STAR_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        setNavigationArrows(gui, Mode.STAR);
        gui.setItem(STAR_SLOT, target);
        updateStarButton(gui);
        player.openInventory(gui);
    }

    public static void openReseal(Player player, ItemStack essence, ItemStack charms) {
        Inventory gui = GuiBuilder.create(GUI_SIZE, RESEAL_TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        setNavigationArrows(gui, Mode.RESEAL);
        gui.setItem(RESEAL_ESSENCE_SLOT, essence);
        gui.setItem(RESEAL_CHARM_SLOT, charms);
        updateResealButton(gui);
        player.openInventory(gui);
    }

    private static void updateStarButton(Inventory gui) {
        ItemStack essence = gui.getItem(STAR_SLOT);
        ItemStack button;
        if (essence != null && ClassEssence.isEssence(essence)) {
            int star = ClassEssence.getStar(essence);
            if (star >= 5) {
                button = GuiUtil.getNexoItem("cross", ChatColor.RED + "Max Star");
            } else {
                int cost = (star + 1) * 1000;
                int[] chances = {33, 15, 10, 5, 2};
                int chance = chances[Math.min(star, chances.length - 1)];
                button = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Upgrade");
                ItemMeta meta = button.getItemMeta();
                if (meta != null) {
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Cost: " + ChatColor.GOLD + "<glyph:coins_icon> " + cost,
                            ChatColor.GRAY + "Success Chance: " + ChatColor.GOLD + chance + "%"
                    ));
                    button.setItemMeta(meta);
                }
            }
        } else {
            button = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Upgrade");
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(ChatColor.GRAY + "Place an essence to upgrade."));
                button.setItemMeta(meta);
            }
        }
        gui.setItem(CONFIRM_SLOT, button);
    }

    private static void updateResealButton(Inventory gui) {
        ItemStack essence = gui.getItem(RESEAL_ESSENCE_SLOT);
        ItemStack charms = gui.getItem(RESEAL_CHARM_SLOT);
        ItemStack button;

        if (essence == null || !ClassEssence.isEssence(essence)) {
            button = GuiUtil.getNexoItem("cross", ChatColor.RED + "Place Essence");
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(ChatColor.GRAY + "Insert a soulbound essence to reseal."));
                button.setItemMeta(meta);
            }
        } else if (!ClassEssence.isSoulbound(essence)) {
            button = GuiUtil.getNexoItem("cross", ChatColor.RED + "Already Unbound");
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(ChatColor.GRAY + "This essence is not soulbound."));
                button.setItemMeta(meta);
            }
        } else if (charms != null && charms.getType() != Material.AIR && !SealingCharm.isCharm(charms)) {
            button = GuiUtil.getNexoItem("cross", ChatColor.RED + "Invalid Charm");
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(ChatColor.GRAY + "Use sealing charms made from enchanted paper."));
                button.setItemMeta(meta);
            }
        } else {
            ItemRarity rarity = ClassEssence.getRarity(essence);
            ResealCost cost = rarity == null ? new ResealCost(0, 0) : getResealCost(rarity);
            int currentCharms = (charms != null && SealingCharm.isCharm(charms)) ? charms.getAmount() : 0;
            button = GuiUtil.getNexoItem("check", ChatColor.GREEN + "Reseal");
            ItemMeta meta = button.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Required Charms: " + ChatColor.AQUA + cost.charms()
                        + ChatColor.GRAY + " (" + currentCharms + ChatColor.GRAY + ")");
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + "<glyph:coins_icon> " + cost.coins());
                meta.setLore(lore);
                button.setItemMeta(meta);
            }
        }

        gui.setItem(CONFIRM_SLOT, button);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        Mode mode = modeFromTitle(title);
        if (mode == null) return;

        if (e.getClickedInventory() != e.getView().getTopInventory()) {
            if (mode == Mode.RESEAL && e.getClickedInventory() == e.getWhoClicked().getInventory() && e.isShiftClick()) {
                e.setCancelled(true);
            }
            return;
        }

        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        Inventory inv = e.getInventory();
        int slot = e.getRawSlot();

        if (slot == LEFT_ARROW_SLOT || slot == RIGHT_ARROW_SLOT) {
            Mode targetMode = slot == LEFT_ARROW_SLOT ? mode.previous() : mode.next();
            ItemStack carry = null;
            switch (mode) {
                case INVEST -> {
                    carry = inv.getItem(TARGET_SLOT);
                    ItemStack sacrifice = inv.getItem(SACRIFICE_SLOT);
                    if (sacrifice != null) player.getInventory().addItem(sacrifice);
                }
                case STAR -> carry = inv.getItem(STAR_SLOT);
                case RESEAL -> {
                    carry = inv.getItem(RESEAL_ESSENCE_SLOT);
                    ItemStack charm = inv.getItem(RESEAL_CHARM_SLOT);
                    if (charm != null) player.getInventory().addItem(charm);
                }
            }
            SWITCHING.add(player.getUniqueId());
            switch (targetMode) {
                case INVEST -> openInvest(player, carry);
                case STAR -> openStar(player, carry);
                case RESEAL -> openReseal(player, carry, null);
            }
            return;
        }

        switch (mode) {
            case INVEST -> handleInvestClick(player, inv, slot, e);
            case STAR -> handleStarClick(player, inv, slot, e);
            case RESEAL -> handleResealClick(player, inv, slot, e);
        }
    }

    private static void handleInvestClick(Player player, Inventory inv, int slot, InventoryClickEvent e) {
        if (slot == SACRIFICE_SLOT || slot == TARGET_SLOT) {
            e.setCancelled(false);
            return;
        }

        if (slot == INVEST_ALL_SLOT) {
            investAllDuplicates(player, inv);
            return;
        }

        if (slot == INVEST_EQUIPPED_SLOT) {
            investIntoEquippedEssences(player, inv);
            return;
        }

        if (slot != CONFIRM_SLOT) return;

        ItemStack target = inv.getItem(TARGET_SLOT);
        ItemStack sacrifice = inv.getItem(SACRIFICE_SLOT);
        if (target != null && sacrifice != null &&
                ClassEssence.isEssence(target) && ClassEssence.isEssence(sacrifice) &&
                ClassEssence.getClass(target) == ClassEssence.getClass(sacrifice)) {
            ItemRarity sacRarity = ClassEssence.getRarity(sacrifice);
            int amount = ClassEssence.getInvestExp(sacRarity) + ClassEssence.getExp(sacrifice);
            int adjusted = computeStackAdjustedExp(target, amount);
            if (adjusted <= 0) {
                send(player, MessageType.ERROR, "Split your essence stack to invest.");
                return;
            }
            notifyStackSplit(player, target, adjusted);
            ItemRarity upgraded = ClassEssence.addExp(target, adjusted);
            inv.setItem(TARGET_SLOT, target);
            if (sacrifice.getAmount() > 1) {
                ItemStack remainder = sacrifice.clone();
                remainder.setAmount(sacrifice.getAmount() - 1);
                inv.setItem(SACRIFICE_SLOT, remainder);
            } else {
                inv.setItem(SACRIFICE_SLOT, null);
            }
            String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
            String expColor = me.nakilex.levelplugin.utils.ChatFormatter.experienceColor();
            send(player, MessageType.SUCCESS,
                    "Invested essence (" + expColor + "+" + adjusted + ChatColor.RESET
                            + " <glyph:experience_orb_icon> " + expLabel + ")");
            if (upgraded != null) {
                send(player, MessageType.SUCCESS, "Essence rarity increased to "
                        + upgraded.getColor() + TextUtil.beautifyWords(upgraded.name()));
            }
            Main.getInstance().getQuestManager().handleUpgrade(player, "essence");
            playInvestSound(player);
        } else {
            send(player, MessageType.ERROR, "Essences must match class.");
        }
    }

    private static ItemStack createInvestAllButton() {
        ItemStack button = GuiUtil.getNexoItem("arrow_up", ChatColor.GOLD + "Invest All Duplicates");
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Move every matching essence",
                    ChatColor.GRAY + "from your inventory into",
                    ChatColor.GRAY + "the target for EXP.",
                    "",
                    ChatColor.YELLOW + "Left Click" + ChatColor.GRAY + " while the target",
                    ChatColor.GRAY + "slot is filled to consume",
                    ChatColor.GRAY + "all duplicates."));
            button.setItemMeta(meta);
        }
        return button;
    }

    private static ItemStack createInvestEquippedButton() {
        ItemStack button = GuiUtil.getNexoItem("refresh", ChatColor.AQUA + "Fuel Equipped Essences");
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Finds duplicate essences",
                    ChatColor.GRAY + "for any equipped class",
                    ChatColor.GRAY + "and invests them automatically."));
            button.setItemMeta(meta);
        }
        return button;
    }

    private static ItemStack createInvestInfo() {
        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Investment Tips");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Place an essence in the target slot.",
                    ChatColor.GRAY + "Use the duplicate buttons to consume",
                    ChatColor.GRAY + "all matching essences from your bag.",
                    "",
                    ChatColor.DARK_GRAY + "Prevents equipped essences",
                    ChatColor.DARK_GRAY + "from being consumed."));
            info.setItemMeta(meta);
        }
        return info;
    }

    private static ItemStack createInvestArrow() {
        ItemStack arrow = GuiUtil.getNexoItem("arrow_right", ChatColor.GRAY + "Invest into");
        ItemMeta meta = arrow.getItemMeta();
        if (meta != null) {
            meta.setLore(List.of(ChatColor.GRAY + "Sacrifice essence feeds the target."));
            arrow.setItemMeta(meta);
        }
        return arrow;
    }

    private static void investAllDuplicates(Player player, Inventory inv) {
        ItemStack target = inv.getItem(TARGET_SLOT);
        if (target == null || !ClassEssence.isEssence(target)) {
            send(player, MessageType.ERROR, "Place an essence in the target slot first.");
            return;
        }

        PlayerClass clazz = ClassEssence.getClass(target);
        ItemStack sacrifice = inv.getItem(SACRIFICE_SLOT);
        int sacrificeExp = 0;
        if (sacrifice != null && ClassEssence.isEssence(sacrifice)
                && ClassEssence.getClass(sacrifice) == clazz) {
            sacrificeExp = getInvestValue(sacrifice);
        }
        int availableExp = tallyInventoryForClass(player, clazz, target);
        int potentialExp = availableExp + sacrificeExp;
        int adjusted = computeStackAdjustedExp(target, potentialExp);
        if (adjusted <= 0) {
            send(player, MessageType.ERROR, "Split your essence stack to invest.");
            return;
        }
        int expGained = siphonInventoryForClass(player, clazz, target);
        if (sacrifice != null && ClassEssence.isEssence(sacrifice)
                && ClassEssence.getClass(sacrifice) == clazz) {
            expGained += sacrificeExp;
            inv.setItem(SACRIFICE_SLOT, null);
        }

        if (expGained <= 0) {
            send(player, MessageType.ERROR, "No duplicate essences found to invest.");
            return;
        }

        adjusted = computeStackAdjustedExp(target, expGained);
        if (adjusted <= 0) {
            send(player, MessageType.ERROR, "Split your essence stack to invest.");
            return;
        }
        notifyStackSplit(player, target, adjusted);
        ItemRarity upgraded = ClassEssence.addExp(target, adjusted);
        inv.setItem(TARGET_SLOT, target);
        player.updateInventory();

        String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
        String expColor = me.nakilex.levelplugin.utils.ChatFormatter.experienceColor();
        send(player, MessageType.SUCCESS,
                "Invested duplicates (" + expColor + "+" + adjusted + ChatColor.RESET
                        + " <glyph:experience_orb_icon> " + expLabel + ")");
        if (upgraded != null) {
            send(player, MessageType.SUCCESS, "Essence rarity increased to "
                    + upgraded.getColor() + TextUtil.beautifyWords(upgraded.name()));
        }
        Main.getInstance().getQuestManager().handleUpgrade(player, "essence");
        playInvestSound(player);
    }

    private static void investIntoEquippedEssences(Player player, Inventory inv) {
        StatsManager.PlayerStats stats = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        int totalExp = 0;

        for (int i = 0; i < stats.essenceSlots.length; i++) {
            ItemStack essence = stats.essenceSlots[i];
            if (essence == null || !stats.equippedEssences[i] || !ClassEssence.isEssence(essence)) continue;

            PlayerClass clazz = ClassEssence.getClass(essence);
            int availableExp = tallyInventoryForClass(player, clazz, essence);
            int adjusted = computeStackAdjustedExp(essence, availableExp);
            if (adjusted <= 0) {
                continue;
            }
            int gained = siphonInventoryForClass(player, clazz, essence);
            if (gained <= 0) continue;

            adjusted = computeStackAdjustedExp(essence, gained);
            if (adjusted <= 0) {
                continue;
            }
            notifyStackSplit(player, essence, adjusted);
            ItemRarity upgraded = ClassEssence.addExp(essence, adjusted);
            stats.essenceSlots[i] = essence;
            totalExp += adjusted;
            if (upgraded != null) {
                send(player, MessageType.SUCCESS, clazz.getDisplayName() + " essence rarity increased to "
                        + upgraded.getColor() + TextUtil.beautifyWords(upgraded.name()));
            }
        }

        if (totalExp <= 0) {
            send(player, MessageType.ERROR, "No duplicate essences found for your equipped slots.");
            return;
        }

        player.updateInventory();
        String expLabel = me.nakilex.levelplugin.utils.ChatFormatter.experienceLabel();
        String expColor = me.nakilex.levelplugin.utils.ChatFormatter.experienceColor();
        send(player, MessageType.SUCCESS,
                ChatColor.GREEN + "Invested " + expColor + totalExp + ChatColor.RESET
                        + " <glyph:experience_orb_icon> " + expLabel + " into equipped essences.");
        Main.getInstance().getQuestManager().handleUpgrade(player, "essence");
        playInvestSound(player);
    }

    private static int tallyInventoryForClass(Player player, PlayerClass clazz, ItemStack target) {
        if (clazz == null) return 0;
        int exp = 0;
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getStorageContents()) {
            if (!isInvestableDuplicate(stack, clazz, target)) continue;
            exp += getInvestValue(stack);
        }
        ItemStack offHand = inventory.getItemInOffHand();
        if (isInvestableDuplicate(offHand, clazz, target)) {
            exp += getInvestValue(offHand);
        }
        return exp;
    }

    private static int siphonInventoryForClass(Player player, PlayerClass clazz, ItemStack target) {
        if (clazz == null) return 0;

        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        int expGained = 0;

        for (int i = 0; i < storage.length; i++) {
            ItemStack stack = storage[i];
            if (!isInvestableDuplicate(stack, clazz, target)) continue;
            expGained += getInvestValue(stack);
            storage[i] = null;
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (isInvestableDuplicate(offHand, clazz, target)) {
            expGained += getInvestValue(offHand);
            inventory.setItemInOffHand(null);
        }

        inventory.setStorageContents(storage);
        return expGained;
    }

    private static boolean isInvestableDuplicate(ItemStack stack, PlayerClass clazz, ItemStack target) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        if (!ClassEssence.isEssence(stack) || ClassEssence.isEquipped(stack)) return false;
        if (target != null && stack.equals(target)) return false;
        return ClassEssence.getClass(stack) == clazz;
    }

    private static int getInvestValue(ItemStack stack) {
        if (stack == null || !ClassEssence.isEssence(stack)) return 0;
        ItemRarity sacRarity = ClassEssence.getRarity(stack);
        int per = ClassEssence.getInvestExp(sacRarity) + ClassEssence.getExp(stack);
        return per * stack.getAmount();
    }

    private static int computeStackAdjustedExp(ItemStack target, int expGained) {
        if (target == null || expGained <= 0) return 0;
        int stackSize = Math.max(1, target.getAmount());
        if (stackSize <= 1) {
            return expGained;
        }
        return expGained / stackSize;
    }

    private static void notifyStackSplit(Player player, ItemStack target, int perEssence) {
        if (target == null || player == null) return;
        if (target.getAmount() <= 1) return;
        send(player, MessageType.INFO, "Investment split across stack: " + perEssence + " each.");
    }

    private static void playInvestSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
    }

    private static void handleStarClick(Player player, Inventory inv, int slot, InventoryClickEvent e) {
        if (slot == STAR_SLOT) {
            e.setCancelled(false);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> updateStarButton(inv));
            return;
        }
        if (slot != CONFIRM_SLOT) return;

        ItemStack essence = inv.getItem(STAR_SLOT);
        if (essence != null && ClassEssence.isEssence(essence)) {
            int star = ClassEssence.getStar(essence);
            if (star < 5) {
                int cost = (star + 1) * 1000;
                int[] chances = {33, 15, 10, 5, 2};
                int chance = chances[Math.min(star, chances.length - 1)];
                EconomyManager econ = Main.getInstance().getEconomyManager();
                if (econ.getBalance(player) >= cost) {
                    econ.deductCoins(player, cost);
                    if (new Random().nextInt(100) < chance) {
                        ClassEssence.upgradeStar(essence);
                        send(player, MessageType.SUCCESS, "Star upgrade succeeded!");
                    } else {
                        send(player, MessageType.ERROR, "Star upgrade failed!");
                    }
                    inv.setItem(STAR_SLOT, essence);
                    updateStarButton(inv);
                    Main.getInstance().getQuestManager().handleUpgrade(player, "essence");
                } else {
                    send(player, MessageType.ERROR, "You need " + cost + " coins.");
                }
            } else {
                send(player, MessageType.ERROR, "Essence is already max star.");
            }
        } else {
            send(player, MessageType.ERROR, "Place an essence to upgrade.");
        }
    }

    private static void handleResealClick(Player player, Inventory inv, int slot, InventoryClickEvent e) {
        if (slot == RESEAL_ESSENCE_SLOT) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                if (!ClassEssence.isEssence(cursor) || !ClassEssence.isSoulbound(cursor)) {
                    e.setCancelled(true);
                    send(player, MessageType.ERROR, "Only soulbound essences can be resealed.");
                    return;
                }
            }
            e.setCancelled(false);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> updateResealButton(inv));
            return;
        }
        if (slot == RESEAL_CHARM_SLOT) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR && !SealingCharm.isCharm(cursor)) {
                e.setCancelled(true);
                send(player, MessageType.ERROR, "Insert sealing charms made from enchanted paper.");
                return;
            }
            e.setCancelled(false);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> updateResealButton(inv));
            return;
        }
        if (slot != CONFIRM_SLOT) return;

        ItemStack essence = inv.getItem(RESEAL_ESSENCE_SLOT);
        if (essence == null || !ClassEssence.isEssence(essence)) {
            send(player, MessageType.ERROR, "Place a soulbound essence to reseal.");
            return;
        }
        if (!ClassEssence.isSoulbound(essence)) {
            send(player, MessageType.ERROR, "This essence is already tradable.");
            return;
        }

        ItemRarity rarity = ClassEssence.getRarity(essence);
        ResealCost cost = rarity == null ? new ResealCost(0, 0) : getResealCost(rarity);
        ItemStack charms = inv.getItem(RESEAL_CHARM_SLOT);
        if (charms == null || !SealingCharm.isCharm(charms) || charms.getAmount() < cost.charms()) {
            send(player, MessageType.ERROR, "You need " + cost.charms() + " sealing charms.");
            return;
        }

        EconomyManager econ = Main.getInstance().getEconomyManager();
        if (econ.getBalance(player) < cost.coins()) {
            send(player, MessageType.ERROR, "You need " + cost.coins() + " coins.");
            return;
        }

        econ.deductCoins(player, cost.coins());
        if (charms.getAmount() > cost.charms()) {
            ItemStack remainder = charms.clone();
            remainder.setAmount(charms.getAmount() - cost.charms());
            inv.setItem(RESEAL_CHARM_SLOT, remainder);
        } else {
            inv.setItem(RESEAL_CHARM_SLOT, null);
        }

        ClassEssence.setSoulbound(essence, false);
        inv.setItem(RESEAL_ESSENCE_SLOT, essence);
        updateResealButton(inv);
        send(player, MessageType.SUCCESS, ChatColor.GREEN + "Essence resealed and ready for trade.");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        Player player = (Player) e.getPlayer();
        if (SWITCHING.remove(player.getUniqueId())) return;

        Inventory inv = e.getInventory();
        Mode mode = modeFromTitle(title);
        if (mode == null) return;

        switch (mode) {
            case INVEST -> {
                ItemStack target = inv.getItem(TARGET_SLOT);
                ItemStack sacrifice = inv.getItem(SACRIFICE_SLOT);
                if (target != null) player.getInventory().addItem(target);
                if (sacrifice != null) player.getInventory().addItem(sacrifice);
            }
            case STAR -> {
                ItemStack target = inv.getItem(STAR_SLOT);
                if (target != null) player.getInventory().addItem(target);
            }
            case RESEAL -> {
                ItemStack essence = inv.getItem(RESEAL_ESSENCE_SLOT);
                ItemStack charms = inv.getItem(RESEAL_CHARM_SLOT);
                if (essence != null) player.getInventory().addItem(essence);
                if (charms != null) player.getInventory().addItem(charms);
            }
        }
    }
}
