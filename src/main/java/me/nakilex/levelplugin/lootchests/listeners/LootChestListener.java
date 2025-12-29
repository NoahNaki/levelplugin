package me.nakilex.levelplugin.lootchests.listeners;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.player.battlepass.BattlePassManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.guild.quests.GuildQuestManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class LootChestListener implements Listener {

    private final LootChestManager lootChestManager;
    private final BattlePassManager battlePassManager;

    public LootChestListener(LootChestManager lootChestManager, BattlePassManager battlePassManager) {
        this.lootChestManager = lootChestManager;
        this.battlePassManager = battlePassManager;
    }

    @EventHandler
    public void onFurnitureInteract(NexoFurnitureInteractEvent event) {
        // 1) Which furniture did the player click?
        FurnitureMechanic mech = event.getMechanic();

        // Only handle our crate furniture
        if (!lootChestManager.getCrateModelId().equals(mech.getItemID())) {
            return;
        }

        // 2) Cancel default behavior (so the barrier block doesn’t break/open itself)
        event.setCancelled(true);

        // 3) Locate our chestId from the clicked furniture's base block
        Location loc = event.getBaseEntity().getLocation().getBlock().getLocation();
        Integer chestId = lootChestManager.getChestIdAtLocation(loc);
        if (chestId == null) {
            return; // not one of our managed chests
        }

        // 4) Identify player
        Player player = event.getPlayer();
        Main.getInstance().getDialogManager().recordDialogCooldown(player);

        // 5) Build the custom loot GUI
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

        // 7) Open the inventory
        player.openInventory(lootGui);

        // 8) Track guild quest progress
        GuildQuestManager.getInstance().handleLootChestOpen(player);

        int gearScore = lootChestManager.peekSession(player.getUniqueId()) != null
                ? lootChestManager.peekSession(player.getUniqueId()).gearScore()
                : ItemUtil.calculateTotalGearScore(player);
        awardBattlePassProgress(player, gearScore);
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
