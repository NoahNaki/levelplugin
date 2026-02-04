package me.nakilex.levelplugin.items.v2.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.v2.ItemDefinition;
import me.nakilex.levelplugin.items.v2.ItemGeneration;
import me.nakilex.levelplugin.items.v2.ItemGenerationMode;
import me.nakilex.levelplugin.items.v2.ItemRegistry;
import me.nakilex.levelplugin.items.v2.ItemRequirements;
import me.nakilex.levelplugin.items.v2.ItemStatType;
import me.nakilex.levelplugin.items.v2.ItemType;
import me.nakilex.levelplugin.items.v2.ItemVisuals;
import me.nakilex.levelplugin.items.v2.StatValue;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.IntegerInputPrompt;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemFactoryGUI implements CommandExecutor, Listener {
    private static final int SIZE = 54;

    private static final int NAME_SLOT = 10;
    private static final int TYPE_SLOT = 12;
    private static final int RARITY_SLOT = 14;
    private static final int LEVEL_SLOT = 16;

    private static final int CLASS_SLOT = 28;
    private static final int MATERIAL_SLOT = 30;
    private static final int MODEL_SLOT = 32;
    private static final int STATS_SLOT = 34;

    private static final int PREVIEW_SLOT = 49;
    private static final int SAVE_SLOT = 48;
    private static final int RESET_SLOT = 50;
    private static final int CLOSE_SLOT = 45;

    private final JavaPlugin plugin;
    private final Map<UUID, Draft> drafts = new LinkedHashMap<>();

    public ItemFactoryGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("itemfactory").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String title() {
        return ChatUtil.applyEmojis("§8Item Factory");
    }

    private Draft draft(Player player) {
        return drafts.computeIfAbsent(player.getUniqueId(), id -> Draft.createDefault());
    }

    private void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, title());
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        GuiUtil.fillBorder(inv, filler);

        Draft draft = draft(player);

        inv.setItem(NAME_SLOT, buildNameItem(draft));
        inv.setItem(TYPE_SLOT, buildTypeItem(draft));
        inv.setItem(RARITY_SLOT, buildRarityItem(draft));
        inv.setItem(LEVEL_SLOT, buildLevelItem(draft));

        inv.setItem(CLASS_SLOT, buildClassItem(draft));
        inv.setItem(MATERIAL_SLOT, buildMaterialItem(draft));
        inv.setItem(MODEL_SLOT, buildModelItem(draft));
        inv.setItem(STATS_SLOT, buildStatsItem(draft));

        inv.setItem(PREVIEW_SLOT, buildPreviewItem(draft));
        inv.setItem(SAVE_SLOT, buildSaveItem());
        inv.setItem(RESET_SLOT, buildResetItem());
        inv.setItem(CLOSE_SLOT, buildCloseItem());

        player.openInventory(inv);
    }

    private ItemStack buildNameItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + draft.name);
        lore.addAll(TooltipUtil.clickInstructions("to edit", null));
        return GuiUtil.createGuiItem(Material.NAME_TAG, ChatColor.AQUA + "Name", lore);
    }

    private ItemStack buildTypeItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + draft.type.name());
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.createGuiItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Type", lore);
    }

    private ItemStack buildRarityItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + draft.rarity.getColor() + draft.rarity.name());
        lore.addAll(TooltipUtil.clickInstructions("to cycle forward", "to cycle backward"));
        return GuiUtil.createGuiItem(Material.NETHER_STAR, ChatColor.AQUA + "Rarity", lore);
    }

    private ItemStack buildLevelItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + draft.level);
        lore.addAll(TooltipUtil.clickInstructions("to edit", null));
        return GuiUtil.createGuiItem(Material.EXPERIENCE_BOTTLE, ChatColor.AQUA + "Level Requirement", lore);
    }

    private ItemStack buildClassItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        String classes = draft.classes.isEmpty()
                ? "Any"
                : draft.classes.stream().map(PlayerClass::getDisplayName).reduce((a, b) -> a + ", " + b).orElse("Any");
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + classes);
        lore.addAll(TooltipUtil.clickInstructions("to edit", null));
        return GuiUtil.createGuiItem(Material.BOOK, ChatColor.AQUA + "Class Requirements", lore);
    }

    private ItemStack buildMaterialItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + draft.baseMaterial.name());
        lore.addAll(TooltipUtil.clickInstructions("to edit", null));
        return GuiUtil.createGuiItem(Material.ANVIL, ChatColor.AQUA + "Base Material", lore);
    }

    private ItemStack buildModelItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + draft.modelKey);
        lore.addAll(TooltipUtil.clickInstructions("to edit", null));
        return GuiUtil.createGuiItem(Material.ARMOR_STAND, ChatColor.AQUA + "Model Key", lore);
    }

    private ItemStack buildStatsItem(Draft draft) {
        List<String> lore = new ArrayList<>();
        if (draft.stats.isEmpty()) {
            lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + "None");
        } else {
            draft.stats.forEach((key, value) ->
                    lore.add(ChatColor.GRAY + key.name() + ": " + ChatColor.WHITE + value.formatForLore()));
        }
        lore.addAll(TooltipUtil.clickInstructions("to edit", null));
        lore.addAll(TooltipUtil.bulletList("Format: str 2-4, agi 1-2, hp 5"));
        return GuiUtil.createGuiItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "Stats", lore);
    }

    private ItemStack buildPreviewItem(Draft draft) {
        ItemStack preview = new ItemStack(draft.baseMaterial);
        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(draft.rarity.getColor() + draft.name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + draft.type.name());
            lore.add(ChatColor.GRAY + "Level: " + ChatColor.WHITE + draft.level);
            if (!draft.classes.isEmpty()) {
                String classes = draft.classes.stream()
                        .map(PlayerClass::getDisplayName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Any");
                lore.add(ChatColor.GRAY + "Classes: " + ChatColor.WHITE + classes);
            }
            if (!draft.stats.isEmpty()) {
                lore.add("");
                draft.stats.forEach((key, value) ->
                        lore.add(ChatColor.GRAY + key.name() + ": " + ChatColor.GREEN + "+" + value.formatForLore()));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            preview.setItemMeta(meta);
        }
        return preview;
    }

    private ItemStack buildSaveItem() {
        List<String> lore = TooltipUtil.clickInstructions("to save", null);
        return GuiUtil.getNexoItem("check", ChatColor.GREEN + "Save Item", lore);
    }

    private ItemStack buildResetItem() {
        List<String> lore = TooltipUtil.clickInstructions("to reset", null);
        return GuiUtil.getNexoItem("cross", ChatColor.RED + "Reset Draft", lore);
    }

    private ItemStack buildCloseItem() {
        List<String> lore = TooltipUtil.clickInstructions("to close", null);
        return GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "Close", lore);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        open(player);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle()).equalsIgnoreCase("Item Factory")) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Draft draft = draft(player);
        int slot = event.getRawSlot();

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (slot == NAME_SLOT) {
            startTextPrompt(player, "Enter a new item name:", input -> {
                draft.name = input.trim();
                open(player);
            });
            return;
        }

        if (slot == TYPE_SLOT) {
            draft.type = cycleEnum(ItemType.values(), draft.type, event.isRightClick());
            open(player);
            return;
        }

        if (slot == RARITY_SLOT) {
            draft.rarity = cycleEnum(ItemRarity.values(), draft.rarity, event.isRightClick());
            open(player);
            return;
        }

        if (slot == LEVEL_SLOT) {
            IntegerInputPrompt prompt = IntegerInputPrompt.nonNegativeWithMax(
                    plugin,
                    player,
                    ChatColor.YELLOW + "Enter a level requirement (0-100):",
                    "Invalid level.",
                    "Level out of range.",
                    () -> 100,
                    value -> {
                        draft.level = value;
                        open(player);
                    }
            );
            startPrompt(player, prompt);
            return;
        }

        if (slot == CLASS_SLOT) {
            startTextPrompt(player, "Enter classes (comma-separated) or 'any':", input -> {
                draft.classes = parseClasses(input);
                open(player);
            });
            return;
        }

        if (slot == MATERIAL_SLOT) {
            startTextPrompt(player, "Enter a base material (e.g., DIAMOND_SWORD):", input -> {
                Material material = parseMaterial(input, draft.baseMaterial);
                if (material != null) {
                    draft.baseMaterial = material;
                }
                open(player);
            });
            return;
        }

        if (slot == MODEL_SLOT) {
            startTextPrompt(player, "Enter a model key (e.g., weapon_oak_wand):", input -> {
                String trimmed = input.trim();
                if (!trimmed.isBlank()) {
                    draft.modelKey = trimmed;
                }
                open(player);
            });
            return;
        }

        if (slot == STATS_SLOT) {
            startTextPrompt(player, "Enter stats (e.g., str 2-4, agi 1-2, hp 5):", input -> {
                draft.stats = parseStats(input);
                open(player);
            });
            return;
        }

        if (slot == RESET_SLOT) {
            drafts.put(player.getUniqueId(), Draft.createDefault());
            open(player);
            return;
        }

        if (slot == SAVE_SLOT) {
            ItemRegistry registry = Main.getInstance().getItemRegistryV2();
            ItemDefinition definition = new ItemDefinition(
                    0,
                    draft.name,
                    draft.type,
                    draft.rarity,
                    new ItemRequirements(draft.level, draft.classes),
                    new ItemVisuals(draft.baseMaterial, draft.modelKey),
                    new ItemGeneration(ItemGenerationMode.HANDMADE, null),
                    draft.stats,
                    Map.of(),
                    2
            );
            ItemDefinition created = registry.createNew(definition);
            player.sendMessage(ChatColor.GREEN + "Created item #" + created.id() + ": " + created.name());
            drafts.put(player.getUniqueId(), Draft.createDefault());
            open(player);
        }
    }

    private void startTextPrompt(Player player, String promptText, java.util.function.Consumer<String> onAccept) {
        startPrompt(player, new StringPrompt() {
            @Override
            public String getPromptText(org.bukkit.conversations.ConversationContext context) {
                return ChatColor.YELLOW + promptText;
            }

            @Override
            public Prompt acceptInput(org.bukkit.conversations.ConversationContext context, String input) {
                Bukkit.getScheduler().runTask(plugin, () -> onAccept.accept(input));
                return Prompt.END_OF_CONVERSATION;
            }
        });
    }

    private void startPrompt(Player player, Prompt prompt) {
        ConversationFactory factory = new ConversationFactory(plugin)
                .withLocalEcho(false)
                .withFirstPrompt(prompt)
                .addConversationAbandonedListener(event -> Bukkit.getScheduler().runTask(plugin, () -> open(player)));
        factory.buildConversation(player).begin();
    }

    private static <T extends Enum<T>> T cycleEnum(T[] values, T current, boolean reverse) {
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                index = i;
                break;
            }
        }
        int next = reverse ? index - 1 : index + 1;
        if (next < 0) next = values.length - 1;
        if (next >= values.length) next = 0;
        return values[next];
    }

    private static List<PlayerClass> parseClasses(String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isBlank() || trimmed.equalsIgnoreCase("any")) {
            return List.of();
        }
        String[] parts = trimmed.split(",");
        List<PlayerClass> classes = new ArrayList<>();
        for (String part : parts) {
            PlayerClass pc = PlayerClass.fromString(part.trim());
            if (pc != null && pc != PlayerClass.VILLAGER) {
                classes.add(pc);
            }
        }
        return classes;
    }

    private static Material parseMaterial(String input, Material fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        try {
            return Material.valueOf(input.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static Map<ItemStatType, StatValue> parseStats(String input) {
        Map<ItemStatType, StatValue> stats = new EnumMap<>(ItemStatType.class);
        if (input == null || input.isBlank()) {
            return stats;
        }
        String[] entries = input.split(",");
        for (String entry : entries) {
            String trimmed = entry.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length < 2) {
                continue;
            }
            ItemStatType statType = parseStatType(parts[0]);
            if (statType == null) {
                continue;
            }
            try {
                StatValue value = StatValue.fromLegacyRangeString(parts[1]);
                stats.put(statType, value);
            } catch (Exception ignored) {
            }
        }
        return stats;
    }

    private static ItemStatType parseStatType(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase();
        return switch (key) {
            case "int" -> ItemStatType.INTEL;
            case "def", "defense" -> ItemStatType.DEF;
            default -> ItemStatType.fromKey(key);
        };
    }

    private static class Draft {
        private String name;
        private ItemType type;
        private ItemRarity rarity;
        private int level;
        private List<PlayerClass> classes;
        private Material baseMaterial;
        private String modelKey;
        private Map<ItemStatType, StatValue> stats;

        private static Draft createDefault() {
            Draft draft = new Draft();
            draft.name = "New Item";
            draft.type = ItemType.MISC;
            draft.rarity = ItemRarity.COMMON;
            draft.level = 1;
            draft.classes = List.of();
            draft.baseMaterial = Material.DIAMOND;
            draft.modelKey = "unassigned";
            draft.stats = new EnumMap<>(ItemStatType.class);
            return draft;
        }
    }
}
