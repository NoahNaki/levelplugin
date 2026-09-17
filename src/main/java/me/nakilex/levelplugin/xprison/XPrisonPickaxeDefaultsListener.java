package me.nakilex.levelplugin.xprison;

import dev.drawethree.xprison.api.XPrisonAPI;
import dev.drawethree.xprison.api.autosell.XPrisonAutoSellAPI;
import dev.drawethree.xprison.api.enchants.XPrisonEnchantsAPI;
import dev.drawethree.xprison.api.enchants.model.XPrisonEnchantment;
import dev.drawethree.xprison.api.mines.XPrisonMinesAPI;
import dev.drawethree.xprivatemines.api.XPrivateMinesAPI;
import dev.drawethree.xprivatemines.api.manager.PrivateMinesManager;
import dev.drawethree.xprivatemines.api.model.PrivateMine;
import me.nakilex.levelplugin.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bakes Efficiency 1500, Fly and Autosell into every prison pickaxe as free, hidden defaults
 * instead of purchasable enchants, and gates flight to inside a mine (public or private).
 *
 * <p>X-Prison's own "enabled" flag on an enchant also disables its functional dispatch
 * (confirmed by decompiling EnchantsManager - forEachEffectiveEnchant/handleBlockBreak both gate
 * on isEnabled()), so Fly/Autosell/Efficiency stay enabled:true in their JSON and this class
 * force-grants the levels and scrubs the GUI/lore of those 3 entries instead.</p>
 */
public final class XPrisonPickaxeDefaultsListener implements Listener {

    private static final int TARGET_EFFICIENCY_LEVEL = 1500;
    private static final int TARGET_FLY_LEVEL = 1;

    // Must match X-Prison's own config.yml "supported-pickaxes" list.
    private static final EnumSet<Material> SUPPORTED_PICKAXES = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.COPPER_PICKAXE, Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE
    );

    private final Main plugin;

    private XPrisonAPI xprisonApi;
    private XPrisonEnchantsAPI enchantsApi;
    private XPrisonMinesAPI minesApi;
    private XPrisonAutoSellAPI autoSellApi;
    private PrivateMinesManager privateMinesManager;

    private XPrisonEnchantment flyEnchant;
    private XPrisonEnchantment autosellEnchant;
    private XPrisonEnchantment efficiencyEnchant;

    private List<String> flyKeepPermissions = new ArrayList<>();

    private final Set<UUID> grantedFlightByUs = new HashSet<>();
    private final Map<Inventory, Set<Integer>> scrubbedSlots = new HashMap<>();

    private BukkitTask safetyNetTask;
    private boolean enabled;

    public XPrisonPickaxeDefaultsListener(Main plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("X-Prison")) {
            return;
        }

        try {
            xprisonApi = XPrisonAPI.getInstance();
        } catch (RuntimeException | LinkageError ex) {
            plugin.getLogger().warning("Could not access X-Prison API for pickaxe defaults: " + ex.getMessage());
            return;
        }
        if (xprisonApi == null) {
            return;
        }

        enchantsApi = xprisonApi.getEnchantsApi();
        minesApi = xprisonApi.getMinesApi();
        autoSellApi = xprisonApi.getAutoSellApi();

        flyEnchant = enchantsApi.getByName("fly");
        autosellEnchant = enchantsApi.getByName("autosell");
        efficiencyEnchant = enchantsApi.getByName("efficiency");

        if (flyEnchant == null || autosellEnchant == null || efficiencyEnchant == null) {
            plugin.getLogger().warning("Could not find fly/autosell/efficiency enchants; pickaxe defaults disabled.");
            return;
        }

        loadFlyKeepPermissions();

        if (Bukkit.getPluginManager().isPluginEnabled("X-PrivateMines")) {
            try {
                XPrivateMinesAPI privateMinesApi = XPrivateMinesAPI.getInstance();
                if (privateMinesApi != null) {
                    privateMinesManager = privateMinesApi.getMinesManager();
                }
            } catch (RuntimeException | LinkageError ex) {
                plugin.getLogger().warning("Could not access X-PrivateMines API for fly gating: " + ex.getMessage());
            }
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (Player player : Bukkit.getOnlinePlayers()) {
            ensureAllPickaxes(player);
        }

        // Also re-scans every online player's inventory each tick, not just flight - X-Prison can
        // hand out a fresh pickaxe (first-join, kits, admin give) at any time outside of a join or
        // hand-swap event, and nothing else would ever notice and fix it up.
        safetyNetTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ensureAllPickaxes(player);
                refreshFlight(player);
            }
        }, 20L, 20L);

        enabled = true;
        plugin.getLogger().info("Pickaxe defaults active: Efficiency " + TARGET_EFFICIENCY_LEVEL
                + " + Fly forced free/hidden on every prison pickaxe, full AutoSell force-enabled per player"
                + (privateMinesManager != null ? " (public + private mines)." : " (public mines only)."));
    }

    public void disable() {
        if (safetyNetTask != null) {
            safetyNetTask.cancel();
            safetyNetTask = null;
        }
        grantedFlightByUs.clear();
        scrubbedSlots.clear();
        enabled = false;
    }

    private void loadFlyKeepPermissions() {
        try {
            File file = new File(plugin.getServer().getPluginsFolder(), "X-Prison/enchants.yml");
            if (!file.isFile()) {
                return;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            List<String> configured = yaml.getStringList("fly.keep-permissions");
            if (configured != null && !configured.isEmpty()) {
                flyKeepPermissions = configured;
            }
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Could not read X-Prison fly.keep-permissions: " + ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------
    // Baseline enchant levels
    // ----------------------------------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        ensureAllPickaxes(event.getPlayer());
        refreshFlight(event.getPlayer());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (isSupportedPickaxe(held)) {
                ensurePickaxeDefaults(player, player.getInventory(), player.getInventory().getHeldItemSlot());
            }
            refreshFlight(player);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        grantedFlightByUs.remove(event.getPlayer().getUniqueId());
    }

    private void ensureAllPickaxes(Player player) {
        ensureAutoSellToggle(player);
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isSupportedPickaxe(contents[slot])) {
                ensurePickaxeDefaults(player, inv, slot);
            }
        }
    }

    private boolean isSupportedPickaxe(ItemStack item) {
        return item != null && SUPPORTED_PICKAXES.contains(item.getType());
    }

    private void ensurePickaxeDefaults(Player player, PlayerInventory inv, int slot) {
        ItemStack pickaxe = inv.getItem(slot);
        if (!isSupportedPickaxe(pickaxe)) {
            return;
        }

        boolean needsFly = enchantsApi.getEnchantLevel(pickaxe, flyEnchant) < TARGET_FLY_LEVEL;
        boolean needsEfficiency = enchantsApi.getEnchantLevel(pickaxe, efficiencyEnchant) < TARGET_EFFICIENCY_LEVEL;

        ItemStack updated = pickaxe;
        if (needsFly) {
            updated = enchantsApi.setEnchantLevel(player, updated, flyEnchant, TARGET_FLY_LEVEL);
        }
        if (needsEfficiency) {
            updated = enchantsApi.setEnchantLevel(player, updated, efficiencyEnchant, TARGET_EFFICIENCY_LEVEL);
        }

        boolean metaChanged = false;
        ItemMeta meta = updated.getItemMeta();
        if (meta != null) {
            if (meta.getEnchantLevel(Enchantment.EFFICIENCY) != TARGET_EFFICIENCY_LEVEL) {
                meta.addEnchant(Enchantment.EFFICIENCY, TARGET_EFFICIENCY_LEVEL, true);
                metaChanged = true;
            }
            if (!meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                metaChanged = true;
            }

            List<Component> lore = meta.lore();
            if (lore != null) {
                List<Component> scrubbed = stripHiddenEnchantLines(lore);
                if (scrubbed.size() != lore.size()) {
                    meta.lore(scrubbed);
                    metaChanged = true;
                }
            }

            if (metaChanged || needsFly || needsEfficiency) {
                updated.setItemMeta(meta);
            }
        }

        if (needsFly || needsEfficiency || metaChanged) {
            inv.setItem(slot, updated);
        }
    }

    /**
     * Full-inventory autosell (X-Prison's separate AutoSell module, {@code /autosell}), not the
     * chance-based Autosell enchant - a pure per-player preference with no enchant-level gate.
     */
    private void ensureAutoSellToggle(Player player) {
        if (!autoSellApi.hasAutoSellEnabled(player)) {
            autoSellApi.enableAutoSell(player);
        }
    }

    private List<Component> stripHiddenEnchantLines(List<Component> lore) {
        String flyName = flyEnchant.getNameWithoutColor();
        String autosellName = autosellEnchant.getNameWithoutColor();
        String efficiencyName = efficiencyEnchant.getNameWithoutColor();

        List<Component> result = new ArrayList<>(lore.size());
        for (Component line : lore) {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            if (containsWord(plain, flyName) || containsWord(plain, autosellName) || containsWord(plain, efficiencyName)) {
                continue;
            }
            result.add(line);
        }
        return result;
    }

    private boolean containsWord(String haystack, String needle) {
        return needle != null && !needle.isBlank()
                && haystack != null
                && haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    // ----------------------------------------------------------------------------------------
    // Enchant menu scrubbing - remove Fly/Autosell/Efficiency icons from any X-Prison GUI
    // ----------------------------------------------------------------------------------------

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!enabled) {
            return;
        }
        scheduleScrub(event.getInventory(), 1L);
        // X-Prison can finish its first render one or two ticks after InventoryOpenEvent.
        scheduleScrub(event.getInventory(), 3L);
        scheduleScrub(event.getInventory(), 6L);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        Set<Integer> hidden = scrubbedSlots.get(top);
        if (hidden != null && event.getClickedInventory() == top && hidden.contains(event.getSlot())) {
            event.setCancelled(true);
            return;
        }
        scheduleScrub(top, 2L);
        scheduleScrub(top, 5L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        scrubbedSlots.remove(event.getInventory());
    }

    private void scheduleScrub(Inventory inventory, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (enabled && inventory.getViewers().stream().anyMatch(viewer -> viewer instanceof Player)) {
                scrubInventory(inventory);
            }
        }, delay);
    }

    private void scrubInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        String flyName = flyEnchant.getNameWithoutColor();
        String autosellName = autosellEnchant.getNameWithoutColor();
        String efficiencyName = efficiencyEnchant.getNameWithoutColor();

        Set<Integer> hidden = null;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || !item.hasItemMeta()) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            Component displayName = meta.displayName();
            Component itemName = meta.itemName();
            String plain = displayName != null
                    ? PlainTextComponentSerializer.plainText().serialize(displayName)
                    : itemName != null
                    ? PlainTextComponentSerializer.plainText().serialize(itemName)
                    : meta.getDisplayName();
            if (containsWord(plain, flyName) || containsWord(plain, autosellName) || containsWord(plain, efficiencyName)) {
                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta fillerMeta = filler.getItemMeta();
                fillerMeta.displayName(Component.empty());
                filler.setItemMeta(fillerMeta);
                inventory.setItem(slot, filler);
                if (hidden == null) {
                    hidden = new HashSet<>();
                }
                hidden.add(slot);
            }
        }
        if (hidden != null) {
            scrubbedSlots.put(inventory, hidden);
        }
    }

    // ----------------------------------------------------------------------------------------
    // Region-gated flight
    // ----------------------------------------------------------------------------------------

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!enabled) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        refreshFlight(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (!enabled) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> refreshFlight(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (enabled) {
            refreshFlight(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (enabled) {
            Bukkit.getScheduler().runTask(plugin, () -> refreshFlight(event.getPlayer()));
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (enabled) {
            Bukkit.getScheduler().runTask(plugin, () -> refreshFlight(event.getPlayer()));
        }
    }

    private void refreshFlight(Player player) {
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return;
        }
        for (String permission : flyKeepPermissions) {
            if (permission != null && !permission.isEmpty() && player.hasPermission(permission)) {
                return;
            }
        }

        boolean holdingFlyPickaxe = isSupportedPickaxe(player.getInventory().getItemInMainHand())
                && enchantsApi.getEnchantLevel(player.getInventory().getItemInMainHand(), flyEnchant) >= TARGET_FLY_LEVEL;
        boolean inMiningArea = holdingFlyPickaxe && isInMiningArea(player.getLocation());

        UUID id = player.getUniqueId();
        if (holdingFlyPickaxe && inMiningArea) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                grantedFlightByUs.add(id);
            }
        } else if (grantedFlightByUs.remove(id)) {
            if (player.isFlying()) {
                player.setFlying(false);
            }
            player.setAllowFlight(false);
        }
    }

    private boolean isInMiningArea(Location location) {
        if (minesApi.getMineAtLocation(location) != null) {
            return true;
        }
        if (privateMinesManager != null) {
            PrivateMine privateMine = privateMinesManager.getPrivateMineAtLocation(location);
            if (privateMine != null && privateMine.getMine() != null && privateMine.isInMine(location)) {
                return true;
            }
        }
        return false;
    }
}
