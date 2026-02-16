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
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
    private final Map<UUID, Integer> mobPages = new java.util.HashMap<>();
    private final Map<UUID, Integer> spawnerPages = new java.util.HashMap<>();

    private final String mainTitle = ChatUtil.applyEmojis("§8Mob Admin");
    private final String mobsTitle = ChatUtil.applyEmojis("§8Mobs");
    private final String spawnersTitle = ChatUtil.applyEmojis("§8Custom Spawners");

    public CustomMobAdminGUI(Main plugin,
                             CustomMobManager mobManager,
                             CustomMobSpawnerManager spawnerManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.spawnerManager = spawnerManager;
    }

    public void openMain(Player player) {
        Inventory inv = GuiBuilder.create(MAIN_SIZE, mainTitle)
                .filler(Material.BLACK_STAINED_GLASS_PANE)
                .border()
                .build();
        renderWidgets(inv, player, buildMainWidgets());

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

        Inventory inv = GuiBuilder.create(GUI_SIZE, mobsTitle)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        renderWidgets(inv, player, buildMobWidgets(defs, current, maxPage));

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

        Inventory inv = GuiBuilder.create(GUI_SIZE, spawnersTitle)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .border()
                .build();
        renderWidgets(inv, player, buildSpawnerWidgets(list, current, maxPage));

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
            if (handleWidgetClick(event, player, buildMainWidgets())) {
                return;
            }
            return;
        }

        if (GuiUtil.titleMatches(title, mobsTitle)) {
            List<CustomMobDefinition> defs = mobManager.getMobIds().stream()
                    .map(mobManager::getDefinition)
                    .flatMap(java.util.Optional::stream)
                    .toList();
            int page = mobPages.getOrDefault(player.getUniqueId(), 0);
            int maxPage = Math.max(0, (defs.size() - 1) / PAGE_SIZE);
            if (handleWidgetClick(event, player, buildMobWidgets(defs, page, maxPage))) {
                return;
            }
            return;
        }

        if (GuiUtil.titleMatches(title, spawnersTitle)) {
            List<CustomMobSpawner> list = spawnerManager.getSpawnerNames().stream()
                    .map(spawnerManager::getSpawner)
                    .flatMap(java.util.Optional::stream)
                    .toList();
            int page = spawnerPages.getOrDefault(player.getUniqueId(), 0);
            int maxPage = Math.max(0, (list.size() - 1) / PAGE_SIZE);
            if (handleWidgetClick(event, player, buildSpawnerWidgets(list, page, maxPage))) {
                return;
            }
        }
    }

    private ItemStack createMenuItem(Material material, String name, List<String> description, List<String> instructions) {
        List<String> lore = new ArrayList<>();
        lore.addAll(description);
        lore.add(" ");
        lore.addAll(instructions);
        return GuiUtil.createGuiItem(material, name, lore);
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
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
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
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
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

    private List<GuiWidget> buildMainWidgets() {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(11,
                context -> createMenuItem(Material.SPAWNER, "§aMobs",
                        TooltipUtil.bulletList("Spawn or inspect mobs."),
                        TooltipUtil.clickInstructions("to open mob list", null)),
                (click, context) -> openMobs(context.player(), mobPages.getOrDefault(context.player().getUniqueId(), 0))));
        widgets.add(new ActionWidget(15,
                context -> createMenuItem(Material.END_CRYSTAL, "§bSpawners",
                        TooltipUtil.bulletList("Manage mob spawners."),
                        TooltipUtil.clickInstructions("to open spawner list", null)),
                (click, context) -> openSpawners(context.player(), spawnerPages.getOrDefault(context.player().getUniqueId(), 0))));
        return widgets;
    }

    private List<GuiWidget> buildMobWidgets(List<CustomMobDefinition> defs, int page, int maxPage) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(BACK_SLOT,
                context -> GuiUtil.getNexoItem("arrow_left2", "§7Back"),
                (click, context) -> openMain(context.player())));
        widgets.add(new ActionWidget(PREV_SLOT,
                context -> page > 0 ? GuiUtil.getNexoItem("arrow_left", "§aPrevious Page") : null,
                (click, context) -> openMobs(context.player(), page - 1)));
        widgets.add(new ActionWidget(NEXT_SLOT,
                context -> page < maxPage ? GuiUtil.getNexoItem("arrow_right", "§aNext Page") : null,
                (click, context) -> openMobs(context.player(), page + 1)));

        int start = page * PAGE_SIZE;
        int end = Math.min(defs.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            CustomMobDefinition def = defs.get(i);
            int slot = GuiUtil.PAGED_SLOTS[i - start];
            widgets.add(new ActionWidget(slot,
                    context -> createMobItem(def),
                    (click, context) -> spawnMob(context.player(), def.id(), click.isRightClick())));
        }
        return widgets;
    }

    private List<GuiWidget> buildSpawnerWidgets(List<CustomMobSpawner> list, int page, int maxPage) {
        List<GuiWidget> widgets = new ArrayList<>();
        widgets.add(new ActionWidget(BACK_SLOT,
                context -> GuiUtil.getNexoItem("arrow_left2", "§7Back"),
                (click, context) -> openMain(context.player())));
        widgets.add(new ActionWidget(PREV_SLOT,
                context -> page > 0 ? GuiUtil.getNexoItem("arrow_left", "§aPrevious Page") : null,
                (click, context) -> openSpawners(context.player(), page - 1)));
        widgets.add(new ActionWidget(NEXT_SLOT,
                context -> page < maxPage ? GuiUtil.getNexoItem("arrow_right", "§aNext Page") : null,
                (click, context) -> openSpawners(context.player(), page + 1)));

        int start = page * PAGE_SIZE;
        int end = Math.min(list.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            CustomMobSpawner spawner = list.get(i);
            int slot = GuiUtil.PAGED_SLOTS[i - start];
            widgets.add(new ActionWidget(slot,
                    context -> createSpawnerItem(spawner),
                    (click, context) -> handleSpawnerAction(context.player(), spawner, click)));
        }
        return widgets;
    }

    private void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private boolean handleWidgetClick(InventoryClickEvent event, Player player, List<GuiWidget> widgets) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private void spawnMob(Player player, String mobId, boolean rightClick) {
        int amount = rightClick ? 5 : 1;
        var spawned = mobManager.spawn(mobId, player.getLocation(), amount);
        if (spawned.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Failed to spawn mob: " + mobId);
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Spawned " + spawned.size() + "x " + mobId + ".");
    }

    private void handleSpawnerAction(Player player, CustomMobSpawner spawner, org.bukkit.event.inventory.ClickType click) {
        if (click.isShiftClick() && click.isLeftClick()) {
            if (spawnerManager.moveSpawner(spawner.getName(), player.getLocation())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Moved spawner '" + spawner.getName() + "' to your location.");
                openSpawners(player, spawnerPages.getOrDefault(player.getUniqueId(), 0));
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Failed to move spawner.");
            }
            return;
        }

        if (click.isRightClick()) {
            boolean next = !spawner.isFieldBoss();
            spawnerManager.setFlag(spawner.getName(), "fieldboss", String.valueOf(next));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Field boss set to " + next + " for '" + spawner.getName() + "'.");
            openSpawners(player, spawnerPages.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        if (click.isLeftClick()) {
            Location loc = new Location(Bukkit.getWorld(spawner.getWorld()), spawner.getX(), spawner.getY(), spawner.getZ());
            if (loc.getWorld() != null) {
                player.teleport(loc);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Teleported to spawner '" + spawner.getName() + "'.");
            } else {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Spawner world is not loaded.");
            }
        }
    }
}
