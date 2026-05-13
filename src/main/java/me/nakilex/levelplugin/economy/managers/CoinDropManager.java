package me.nakilex.levelplugin.economy.managers;

import me.nakilex.levelplugin.Main;
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
    private static final int MAX_STACK_AMOUNT = 64;

    private final EconomyManager economyManager;

    public CoinDropManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
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
        for (CoinDenomination denomination : CoinDenomination.valuesDescending()) {
            int count = total / denomination.value;
            total %= denomination.value;
            while (count > 0) {
                int stackAmount = Math.min(MAX_STACK_AMOUNT, count);
                count -= stackAmount;
                spawnCoinStack(plugin, world, dropLocation, denomination, stackAmount, ownerId);
            }
        }
        return droppedTotal;
    }

    private static void spawnCoinStack(Main plugin,
                                       World world,
                                       Location location,
                                       CoinDenomination denomination,
                                       int stackAmount,
                                       UUID ownerId) {
        if (stackAmount <= 0) {
            return;
        }
        ItemStack stack = createCoinStack(denomination, stackAmount);
        Item item = world.dropItemNaturally(location, stack);
        item.setPickupDelay(10);
        item.setCustomName(denomination.displayName(stackAmount));
        item.setCustomNameVisible(true);
        item.setVelocity(item.getVelocity().add(new Vector(0.0, 0.08, 0.0)));
        PersistentDataContainer pdc = item.getPersistentDataContainer();
        pdc.set(COIN_VALUE_KEY, PersistentDataType.INTEGER, denomination.value * stackAmount);
        if (ownerId != null) {
            pdc.set(COIN_OWNER_KEY, PersistentDataType.STRING, ownerId.toString());
        }
        ModelEngineUtil.applyFirstAvailableModel(item, ModelEngineUtil.buildModelCandidates(denomination.modelId), plugin);
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
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
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
        item.remove();
        economyManager.addCoins(player, value, false);
        CurrencyMessageUtil.sendReceive(player, CurrencyMessageUtil.Currency.COINS, value);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.45f, 1.65f);
    }

    private boolean canPickupCoin(Player player, Entity item) {
        String owner = item.getPersistentDataContainer().get(COIN_OWNER_KEY, PersistentDataType.STRING);
        if (owner == null || owner.isBlank()) {
            return true;
        }
        return owner.equalsIgnoreCase(player.getUniqueId().toString());
    }

    private enum CoinDenomination {
        GOLD(100, Material.GOLD_NUGGET, "gold_coin.bbmodel", ChatColor.GOLD, "Gold Coin"),
        SILVER(10, Material.IRON_NUGGET, "silver_coin.bbmodel", ChatColor.WHITE, "Silver Coin"),
        COPPER(1, Material.COPPER_INGOT, "copper_coin.bbmodel", ChatColor.YELLOW, "Copper Coin");

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
            return new CoinDenomination[]{GOLD, SILVER, COPPER};
        }

        private String displayName(int amount) {
            String suffix = amount == 1 ? displayName : displayName + "s";
            return color + String.valueOf(amount) + "x " + suffix;
        }
    }
}
