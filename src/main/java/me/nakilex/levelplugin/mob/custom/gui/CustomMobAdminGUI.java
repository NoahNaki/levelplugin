package me.nakilex.levelplugin.mob.custom.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobDefinition;
import me.nakilex.levelplugin.mob.custom.CustomMobManager;
import me.nakilex.levelplugin.mob.custom.spawner.CustomMobSpawner;
import me.nakilex.levelplugin.mob.custom.spawner.CustomMobSpawnerManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomMobAdminGUI implements Listener {
    private static final int PAGE_SIZE = GuiUtil.PAGED_SLOTS.length;
    private static final int GUI_SIZE = 54;
    private static final int MAIN_SIZE = 27;
    private static final int BACK_SLOT = 49;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final Main plugin;
    private final CustomMobManager mobManager;
    private final CustomMobSpawnerManager spawnerManager;
    private final NamespacedKey mobKey;
    private final NamespacedKey spawnerKey;
    private final Map<UUID, Integer> mobPages = new java.util.HashMap<>();
    private final Map<UUID, Integer> spawnerPages = new java.util.HashMap<>();

    private final String mainTitle = ChatUtil.applyEmojis("§8Custom Mob Admin");
    private final String mobsTitle = ChatUtil.applyEmojis("§8Custom Mobs");
    private final String spawnersTitle = ChatUtil.applyEmojis("§8Custom Spawners");

    public CustomMobAdminGUI(Main plugin,
                             CustomMobManager mobManager,
                             CustomMobSpawnerManager spawnerManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.spawnerManager = spawnerManager;
        this.mobKey = new NamespacedKey(plugin, "cm_mob_id");
        this.spawnerKey = new NamespacedKey(plugin, "cm_spawner_id");
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, MAIN_SIZE, mainTitle);
        GuiUtil.fillBorder(inv, GuiUtil.createFiller(Material.BLACK_STAINED_GLASS_PANE));

        inv.setItem(11, createMenuItem(Material.SPAWNER, "§aCustom Mobs",
                TooltipUtil.bulletList("Spawn or inspect custom mobs."),
                TooltipUtil.clickInstructions("to open mob list", null)));

        inv.setItem(15, createMenuItem(Material.END_CRYSTAL, "§bSpawners",
                TooltipUtil.bulletList("Manage custom mob spawners."),
                TooltipUtil.clickInstructions("to open spawner list", null)));

        player.openInventory(inv);
    }

    public void openMobs(Player player, int page) {
        List<CustomMobDefinition> defs = mobManager.getMobIds().stream()
                .map(mobManager::getDefinition)
                .flatMap(java.util.Optional::stream)
                .toList();
        int maxPage = Math.max(0, (defs.size() - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, maxPage));
        mobPages.put(player.getUniqueId(), current);

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, mobsTitle);
        GuiUtil.fillBorder(inv, GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left2", "§7Back"));
        if (current > 0) {
            inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", "§aPrevious Page"));
        }
        if (current < maxPage) {
            inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", "§aNext Page"));
        }

        int start = current * PAGE_SIZE;
        int end = Math.min(defs.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            CustomMobDefinition def = defs.get(i);
            ItemStack item = createMobItem(def);
            inv.setItem(GuiUtil.PAGED_SLOTS[i - start], item);
        }

        player.openInventory(inv);
    }

    public void openSpawners(Player player, int page) {
        List<CustomMobSpawner> list = spawnerManager.getSpawnerNames().stream()
                .map(spawnerManager::getSpawner)
                .flatMap(java.util.Optional::stream)
                .toList();
        int maxPage = Math.max(0, (list.size() - 1) / PAGE_SIZE);
        int current = Math.max(0, Math.min(page, maxPage));
        spawnerPages.put(player.getUniqueId(), current);

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, spawnersTitle);
        GuiUtil.fillBorder(inv, GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left2", "§7Back"));
        if (current > 0) {
            inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", "§aPrevious Page"));
        }
        if (current < maxPage) {
            inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", "§aNext Page"));
        }

        int start = current * PAGE_SIZE;
        int end = Math.min(list.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            CustomMobSpawner spawner = list.get(i);
            ItemStack item = createSpawnerItem(spawner);
            inv.setItem(GuiUtil.PAGED_SLOTS[i - start], item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!GuiUtil.titleMatches(title, mainTitle)
                && !GuiUtil.titleMatches(title, mobsTitle)
                && !GuiUtil.titleMatches(title, spawnersTitle)) {
            return;
        }
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (GuiUtil.titleMatches(title, mainTitle)) {
            handleMainClick(player, event.getSlot());
            return;
        }

        if (GuiUtil.titleMatches(title, mobsTitle)) {
            handleMobClick(player, event.getSlot(), clicked, event.isRightClick());
            return;
        }

        if (GuiUtil.titleMatches(title, spawnersTitle)) {
            handleSpawnerClick(player, event.getSlot(), clicked, event.isLeftClick(), event.isRightClick(), event.isShiftClick());
        }
    }

    private void handleMainClick(Player player, int slot) {
        if (slot == 11) {
            openMobs(player, mobPages.getOrDefault(player.getUniqueId(), 0));
        } else if (slot == 15) {
            openSpawners(player, spawnerPages.getOrDefault(player.getUniqueId(), 0));
        }
    }

    private void handleMobClick(Player player, int slot, ItemStack item, boolean rightClick) {
        if (slot == BACK_SLOT) {
            openMain(player);
            return;
        }
        if (slot == PREV_SLOT) {
            int page = mobPages.getOrDefault(player.getUniqueId(), 0) - 1;
            openMobs(player, page);
            return;
        }
        if (slot == NEXT_SLOT) {
            int page = mobPages.getOrDefault(player.getUniqueId(), 0) + 1;
            openMobs(player, page);
            return;
        }

        String mobId = getIdFromItem(item, mobKey);
        if (mobId == null) {
            return;
        }
        int amount = rightClick ? 5 : 1;
        var spawned = mobManager.spawn(mobId, player.getLocation(), amount);
        if (spawned.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to spawn custom mob: " + mobId);
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Spawned " + spawned.size() + "x " + mobId + ".");
    }

    private void handleSpawnerClick(Player player, int slot, ItemStack item, boolean leftClick, boolean rightClick, boolean shiftClick) {
        if (slot == BACK_SLOT) {
            openMain(player);
            return;
        }
        if (slot == PREV_SLOT) {
            int page = spawnerPages.getOrDefault(player.getUniqueId(), 0) - 1;
            openSpawners(player, page);
            return;
        }
        if (slot == NEXT_SLOT) {
            int page = spawnerPages.getOrDefault(player.getUniqueId(), 0) + 1;
            openSpawners(player, page);
            return;
        }

        String spawnerName = getIdFromItem(item, spawnerKey);
        if (spawnerName == null) {
            return;
        }
        CustomMobSpawner spawner = spawnerManager.getSpawner(spawnerName).orElse(null);
        if (spawner == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Spawner no longer exists.");
            return;
        }

        if (shiftClick && leftClick) {
            if (spawnerManager.moveSpawner(spawnerName, player.getLocation())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Moved spawner '" + spawnerName + "' to your location.");
                openSpawners(player, spawnerPages.getOrDefault(player.getUniqueId(), 0));
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Failed to move spawner.");
            }
            return;
        }

        if (rightClick) {
            boolean next = !spawner.isFieldBoss();
            spawnerManager.setFlag(spawnerName, "fieldboss", String.valueOf(next));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Field boss set to " + next + " for '" + spawnerName + "'.");
            openSpawners(player, spawnerPages.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        if (leftClick) {
            Location loc = new Location(Bukkit.getWorld(spawner.getWorld()), spawner.getX(), spawner.getY(), spawner.getZ());
            if (loc.getWorld() != null) {
                player.teleport(loc);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Teleported to spawner '" + spawnerName + "'.");
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Spawner world is not loaded.");
            }
        }
    }

    private ItemStack createMenuItem(Material material, String name, List<String> description, List<String> instructions) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.addAll(description);
            lore.add(" ");
            lore.addAll(instructions);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMobItem(CustomMobDefinition def) {
        Material icon = resolveSpawnEgg(def.entityType().name());
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + def.id());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Display: " + ChatColor.WHITE + ChatColor.stripColor(def.displayName()));
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.WHITE + def.levelRange().format());
            lore.add(ChatColor.GRAY + "Boss: " + ChatColor.WHITE + (def.boss() ? "Yes" : "No"));
            if (def.models() != null && !def.models().isEmpty()) {
                lore.add(ChatColor.GRAY + "Models: " + ChatColor.WHITE + def.models().size());
            }
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to spawn 1", "to spawn 5"));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(mobKey, PersistentDataType.STRING, def.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpawnerItem(CustomMobSpawner spawner) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + spawner.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Mob: " + ChatColor.WHITE + spawner.getMobId());
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + spawner.getWorld());
            lore.add(ChatColor.GRAY + "Group: " + ChatColor.WHITE + spawner.getSpawnerGroup());
            lore.add(String.format("§7Location: §f%.1f %.1f %.1f", spawner.getX(), spawner.getY(), spawner.getZ()));
            lore.add(ChatColor.GRAY + "Active: " + ChatColor.WHITE + spawner.getActiveMobs().size());
            lore.add(ChatColor.GRAY + "Field Boss: " + ChatColor.WHITE + (spawner.isFieldBoss() ? "Yes" : "No"));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to teleport", "to toggle field boss"));
            lore.addAll(TooltipUtil.sneakClickInstructions("to move spawner here", null));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(spawnerKey, PersistentDataType.STRING, spawner.getName());
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getIdFromItem(ItemStack item, NamespacedKey key) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private Material resolveSpawnEgg(String entityType) {
        if (entityType == null) {
            return Material.SPAWNER;
        }
        try {
            return Material.valueOf(entityType.toUpperCase() + "_SPAWN_EGG");
        } catch (IllegalArgumentException ex) {
            return Material.SPAWNER;
        }
    }
}
