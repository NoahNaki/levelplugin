package me.nakilex.levelplugin.lootchests.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LootChestListener implements Listener {
    private static final long OPEN_DEBOUNCE_MS = 2_500L;

    private final LootChestManager lootChestManager;
    private final BattlePassManager battlePassManager;
    private final Map<UUID, Integer> lastOpenedChestByPlayer = new HashMap<>();
    private final Map<UUID, Long> nextAllowedOpenAtByPlayer = new HashMap<>();

    public LootChestListener(LootChestManager lootChestManager, BattlePassManager battlePassManager) {
        this.lootChestManager = lootChestManager;
        this.battlePassManager = battlePassManager;
    }

    @EventHandler
    public void onFurnitureInteract(NexoFurnitureInteractEvent event) {
        // 1) Which furniture did the player click?
        FurnitureMechanic mech = event.getMechanic();

        // Only handle our crate furniture
        if (!lootChestManager.isLootChestMechanic(mech)) {
            return;
        }

        // Locate our chestId from the clicked furniture's base block.
        Location loc = event.getBaseEntity().getLocation().getBlock().getLocation();
        event.setCancelled(openLootChest(event.getPlayer(), loc));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Material type = event.getClickedBlock().getType();
        if (type != Material.CHEST && type != Material.TRAPPED_CHEST) {
            return;
        }
        Location loc = event.getClickedBlock().getLocation();
        if (openLootChest(event.getPlayer(), loc)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onModelInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand stand)) {
            return;
        }
        Integer chestId = lootChestManager.getChestIdFromModelEntity(stand);
        if (chestId == null) {
            return;
        }
        Location loc = lootChestManager.getLocationForChestId(chestId);
        if (loc == null) {
            return;
        }
        if (openLootChest(event.getPlayer(), loc)) {
            event.setCancelled(true);
        }
    }

    public boolean openLootChest(Player player, Location loc) {
        Integer chestId = lootChestManager.getChestIdAtLocation(loc);
        if (chestId == null) {
            return false;
        }
        if (isDebouncedChestOpen(player, chestId)) {
            return true;
        }
        Main.getInstance().getDialogManager().recordDialogCooldown(player);

        // Build the custom loot GUI.
        Inventory lootGui = lootChestManager.buildLootInventory(chestId, player);

        // ─────────────────────────────────────────────────────────────────────
        // 6) Update each ItemStack’s tooltip (lore) before the player sees it
        for (int slot = 0; slot < lootGui.getSize(); slot++) {
            ItemStack stack = lootGui.getItem(slot);
            if (stack == null || stack.getType().isAir()) continue;

            // This mutates the ItemStack’s lore in place based on the player’s stats:
            ItemUtil.updateTooltip(stack, player);
        }
        // ─────────────────────────────────────────────────────────────────────

        var runManager = Main.getInstance().getStrongholdRunManager();
        boolean strongholdRunActive = runManager != null && runManager.getStageStatus(player.getUniqueId()) != null;
        if (strongholdRunActive) {
            for (int slot = 0; slot < lootGui.getSize(); slot++) {
                ItemStack stack = lootGui.getItem(slot);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                runManager.storeLootToResultStorage(player, stack.clone());
            }
            lootChestManager.playOpeningAnimation(chestId, lootGui);
            org.bukkit.Bukkit.getScheduler().runTaskLater(Main.getInstance(),
                    () -> lootChestManager.playClosingAnimation(chestId), 12L);
            GuildQuestManager.getInstance().handleLootChestOpen(player);
            markChestOpened(player, chestId);
            return true;
        }

        // 7) Open the inventory
        lootChestManager.playOpeningAnimation(chestId, lootGui);
        player.openInventory(lootGui);
        markChestOpened(player, chestId);

        // 8) Track guild quest progress
        GuildQuestManager.getInstance().handleLootChestOpen(player);

        int gearScore = lootChestManager.peekSession(player.getUniqueId()) != null
                ? lootChestManager.peekSession(player.getUniqueId()).gearScore()
                : ItemUtil.calculateTotalGearScore(player);
        LootChestManager.ChestProgress strongholdProgress =
                lootChestManager.recordStrongholdChestOpen(player.getUniqueId(), chestId, loc.getWorld());
        if (strongholdProgress != null) {
            ChatMessageUtil.send(player, MessageType.INFO,
                    ChatColor.GRAY + "Stronghold chests opened: "
                            + ChatColor.GOLD + strongholdProgress.opened()
                            + ChatColor.GRAY + "/" + ChatColor.GOLD + strongholdProgress.total()
                            + ChatColor.GRAY + ".");
        }
        int currentStreak = lootChestManager.getCurrentLootStreak(player.getUniqueId());
        int nextBonusPercent = lootChestManager.getNextStreakBonusPercent(player.getUniqueId());
        if (currentStreak > 0) {
            ChatMessageUtil.send(player, MessageType.INFO,
                    ChatColor.GRAY + "Loot streak " + ChatColor.GOLD + "x" + currentStreak
                            + ChatColor.GRAY + " active. Next close bonus: "
                            + ChatColor.GOLD + "+" + nextBonusPercent + "% coins"
                            + ChatColor.GRAY + ".");
        } else {
            ChatMessageUtil.send(player, MessageType.INFO,
                    ChatColor.GRAY + "Open another chest within " + ChatColor.YELLOW + "3 minutes"
                            + ChatColor.GRAY + " for up to "
                            + ChatColor.GOLD + "+40% coin streak bonus" + ChatColor.GRAY + ".");
        }
        awardBattlePassProgress(player, gearScore);
        return true;
    }

    private boolean isDebouncedChestOpen(Player player, int chestId) {
        if (player == null) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        int lastChestId = lastOpenedChestByPlayer.getOrDefault(playerId, Integer.MIN_VALUE);
        long nextAllowedAt = nextAllowedOpenAtByPlayer.getOrDefault(playerId, 0L);
        return lastChestId == chestId && System.currentTimeMillis() < nextAllowedAt;
    }

    private void markChestOpened(Player player, int chestId) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        lastOpenedChestByPlayer.put(playerId, chestId);
        nextAllowedOpenAtByPlayer.put(playerId, System.currentTimeMillis() + OPEN_DEBOUNCE_MS);
    }

    private void awardBattlePassProgress(Player player, int gearScore) {
        if (battlePassManager == null) {
            return;
        }
        int battlePassXp = Math.max(120, Math.min(400, 80 + (int) (gearScore * 0.3)));
        battlePassManager.addProgress(
                player,
                battlePassXp,
                "for opening a scaled loot chest"
        );
    }
}
