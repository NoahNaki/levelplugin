package me.nakilex.levelplugin.economy.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.DropDebugManager;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

/** Handles physical coin drops that convert to balance only after pickup. */
public class CoinDropManager implements Listener {
    private static final NamespacedKey COIN_VALUE_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(CoinDropManager.class), "coin_drop_value");
    private static final NamespacedKey COIN_OWNER_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(CoinDropManager.class), "coin_drop_owner");
    private static final NamespacedKey COIN_STACK_ID_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(CoinDropManager.class), "coin_drop_stack_id");
    private static final NamespacedKey COIN_DENOMINATION_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(CoinDropManager.class), "coin_drop_denomination");
    private static final NamespacedKey COIN_UNIT_VALUE_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(CoinDropManager.class), "coin_drop_unit_value");
    private static final NamespacedKey COIN_MODEL_KEY = new NamespacedKey(JavaPlugin.getProvidingPlugin(CoinDropManager.class), "coin_drop_model");
    private static final int MAX_STACK_AMOUNT = 64;

    private final EconomyManager economyManager;
    private final DropDebugManager dropDebugManager;

    public CoinDropManager(EconomyManager economyManager, DropDebugManager dropDebugManager) {
        this.economyManager = economyManager;
        this.dropDebugManager = dropDebugManager;
    }

    public static int dropCoins(Main plugin,
                                EconomyManager economyManager,
                                Player owner,
                                Location location,
                                int amount,
                                boolean applyBoost) {
        if (plugin == null || economyManager == null || location == null || amount <= 0) {
            return 0;
        }
        int total = economyManager.calculateCoinReward(amount, applyBoost);
        if (total <= 0) {
            return 0;
        }
        World world = location.getWorld();
        if (world == null) {
            return 0;
        }
        int droppedTotal = total;
        UUID ownerId = owner == null ? null : owner.getUniqueId();
        Location dropLocation = location.clone().add(0.0, 0.35, 0.0);
        int spawnIndex = 0;
        for (CoinDenomination denomination : CoinDenomination.valuesDescending()) {
            int count = total / denomination.value;
            total %= denomination.value;
            while (count > 0) {
                int stackAmount = Math.min(MAX_STACK_AMOUNT, count);
                count -= stackAmount;
                spawnCoinStack(plugin, world, dropLocation, denomination, stackAmount, owner, ownerId, spawnIndex++);
            }
        }
        return droppedTotal;
    }

    private static void spawnCoinStack(Main plugin,
                                       World world,
                                       Location location,
                                       CoinDenomination denomination,
                                       int stackAmount,
                                       Player owner,
                                       UUID ownerId,
                                       int spawnIndex) {
        if (stackAmount <= 0) {
            return;
        }
        ItemStack stack = createCoinStack(denomination, stackAmount);
        Item item = world.dropItemNaturally(scatterLocation(location, spawnIndex), stack);
        item.setPickupDelay(10);
        item.setCustomName(denomination.displayName(stackAmount));
        item.setCustomNameVisible(true);
        item.setVelocity(item.getVelocity().add(new Vector(0.0, 0.08, 0.0)));
        PersistentDataContainer pdc = item.getPersistentDataContainer();
        int value = denomination.value * stackAmount;
        pdc.set(COIN_VALUE_KEY, PersistentDataType.INTEGER, value);
        pdc.set(COIN_DENOMINATION_KEY, PersistentDataType.STRING, denomination.name());
        pdc.set(COIN_UNIT_VALUE_KEY, PersistentDataType.INTEGER, denomination.value);
        if (ownerId != null) {
            pdc.set(COIN_OWNER_KEY, PersistentDataType.STRING, ownerId.toString());
        }
        ModelEngineUtil.ModelApplyResult modelResult = ModelEngineUtil.applyFirstAvailableModel(item, ModelEngineUtil.buildModelCandidates(denomination.modelId), plugin);
        String appliedModel = modelResult.applied().isEmpty() ? "none" : String.join(",", modelResult.applied());
        pdc.set(COIN_MODEL_KEY, PersistentDataType.STRING, appliedModel);
        sendDropDebug(owner, item, denomination, stackAmount, value, appliedModel, modelResult);
    }

    private static ItemStack createCoinStack(CoinDenomination denomination, int stackAmount) {
        ItemStack stack = new ItemStack(denomination.material, Math.max(1, Math.min(MAX_STACK_AMOUNT, stackAmount)));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(denomination.displayName(stackAmount));
            meta.setLore(List.of(
                    ChatColor.GRAY + "Pick up to receive " + ChatColor.GOLD + (denomination.value * stackAmount)
                            + ChatColor.GRAY + " <glyph:coins_icon> coins."
            ));
            meta.getPersistentDataContainer().set(COIN_STACK_ID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Location scatterLocation(Location origin, int spawnIndex) {
        if (origin == null || spawnIndex <= 0) {
            return origin;
        }
        double angle = spawnIndex * 2.399963229728653; // golden angle keeps nearby drops visually separated.
        double radius = 0.22 + (spawnIndex % 4) * 0.09;
        return origin.clone().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCoinMerge(ItemMergeEvent event) {
        if (isCoinDrop(event.getEntity()) || isCoinDrop(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCoinPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Item item = event.getItem();
        Integer value = item.getPersistentDataContainer().get(COIN_VALUE_KEY, PersistentDataType.INTEGER);
        if (value == null || value <= 0) {
            return;
        }
        if (!canPickupCoin(player, item)) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        sendPickupDebug(player, item, value);
        item.remove();
        economyManager.addCoins(player, value, false);
        CurrencyMessageUtil.sendReceive(player, CurrencyMessageUtil.Currency.COINS, value);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.45f, 1.65f);
    }

    private static void sendDropDebug(Player owner,
                                      Item item,
                                      CoinDenomination denomination,
                                      int stackAmount,
                                      int value,
                                      String appliedModel,
                                      ModelEngineUtil.ModelApplyResult modelResult) {
        Main main = Main.getInstance();
        DropDebugManager debugManager = main == null ? null : main.getDropDebugManager();
        if (owner == null || debugManager == null || !debugManager.isCoinPickupDebugEnabled(owner.getUniqueId())) {
            return;
        }
        owner.sendMessage(ChatColor.DARK_GRAY + "[CoinDebug] " + ChatColor.GRAY + "Spawned "
                + ChatColor.GOLD + value + ChatColor.GRAY + " coins as "
                + denomination.color + stackAmount + "x " + denomination.displayName
                + ChatColor.GRAY + " entity=" + ChatColor.WHITE + shortId(item.getUniqueId())
                + ChatColor.GRAY + " material=" + ChatColor.WHITE + item.getItemStack().getType()
                + ChatColor.GRAY + " model=" + ChatColor.WHITE + appliedModel
                + formatModelFailures(modelResult));
    }

    private void sendPickupDebug(Player player, Item item, int value) {
        if (dropDebugManager == null || !dropDebugManager.isCoinPickupDebugEnabled(player.getUniqueId())) {
            return;
        }
        PersistentDataContainer pdc = item.getPersistentDataContainer();
        String denomination = pdc.getOrDefault(COIN_DENOMINATION_KEY, PersistentDataType.STRING, "unknown");
        Integer unitValue = pdc.get(COIN_UNIT_VALUE_KEY, PersistentDataType.INTEGER);
        String model = pdc.getOrDefault(COIN_MODEL_KEY, PersistentDataType.STRING, "unknown");
        String owner = pdc.getOrDefault(COIN_OWNER_KEY, PersistentDataType.STRING, "none");
        ItemStack stack = item.getItemStack();
        player.sendMessage(ChatColor.DARK_GRAY + "[CoinDebug] " + ChatColor.GRAY + "Picked up "
                + ChatColor.GOLD + value + ChatColor.GRAY + " coins from "
                + ChatColor.WHITE + denomination
                + ChatColor.GRAY + " stackAmount=" + ChatColor.WHITE + stack.getAmount()
                + ChatColor.GRAY + " unit=" + ChatColor.WHITE + (unitValue == null ? "unknown" : unitValue)
                + ChatColor.GRAY + " material=" + ChatColor.WHITE + stack.getType()
                + ChatColor.GRAY + " model=" + ChatColor.WHITE + model
                + ChatColor.GRAY + " entity=" + ChatColor.WHITE + shortId(item.getUniqueId())
                + ChatColor.GRAY + " owner=" + ChatColor.WHITE + shortOwner(owner));
    }

    private static String formatModelFailures(ModelEngineUtil.ModelApplyResult modelResult) {
        if (modelResult == null || modelResult.failed().isEmpty()) {
            return "";
        }
        return ChatColor.GRAY + " failedModels=" + ChatColor.RED + String.join(",", modelResult.failed());
    }

    private static String shortId(UUID uuid) {
        if (uuid == null) {
            return "unknown";
        }
        String value = uuid.toString();
        return value.substring(0, Math.min(8, value.length()));
    }

    private static String shortOwner(String owner) {
        if (owner == null || owner.isBlank() || owner.equalsIgnoreCase("none")) {
            return "none";
        }
        return owner.substring(0, Math.min(8, owner.length()));
    }

    private boolean isCoinDrop(Entity entity) {
        return entity instanceof Item item
                && item.getPersistentDataContainer().has(COIN_VALUE_KEY, PersistentDataType.INTEGER);
    }

    private boolean canPickupCoin(Player player, Entity item) {
        String owner = item.getPersistentDataContainer().get(COIN_OWNER_KEY, PersistentDataType.STRING);
        if (owner == null || owner.isBlank()) {
            return true;
        }
        return owner.equalsIgnoreCase(player.getUniqueId().toString());
    }

    private enum CoinDenomination {
        GOLD(100, Material.GOLD_NUGGET, "gold_coin", ChatColor.GOLD, "Gold Coin"),
        IRON(10, Material.IRON_NUGGET, "iron_coin", ChatColor.WHITE, "Iron Coin"),
        COPPER(1, Material.COPPER_INGOT, "copper_coin", ChatColor.YELLOW, "Copper Coin");

        private final int value;
        private final Material material;
        private final String modelId;
        private final ChatColor color;
        private final String displayName;

        CoinDenomination(int value, Material material, String modelId, ChatColor color, String displayName) {
            this.value = value;
            this.material = material;
            this.modelId = modelId;
            this.color = color;
            this.displayName = displayName;
        }

        private static CoinDenomination[] valuesDescending() {
            return new CoinDenomination[]{GOLD, IRON, COPPER};
        }

        private String displayName(int amount) {
            String suffix = amount == 1 ? displayName : displayName + "s";
            return color + String.valueOf(amount) + "x " + suffix;
        }
    }
}
