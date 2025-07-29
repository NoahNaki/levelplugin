package me.nakilex.levelplugin.npc.wandering;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Spawns a wandering merchant NPC with a llama companion.
 * Generates a random shop inventory when spawned.
 */
public class WanderingMerchantManager {
    private final Main plugin;
    private WanderingTrader merchant;
    private TraderLlama llama1;
    private TraderLlama llama2;
    private WanderingMerchantGUI gui;
    private long lastSpawn = 0L;

    public WanderingMerchantManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return merchant != null && !merchant.isDead();
    }

    public void spawnNear(Player player) {
        if (isActive()) return;
        Location base = player.getLocation().clone();
        base.add(player.getLocation().getDirection().multiply(-8));
        base.getWorld().getChunkAtAsync(base).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> spawn(base, player));
        });
    }

    private void spawn(Location loc, Player player) {
        merchant = (WanderingTrader) loc.getWorld().spawnEntity(loc, EntityType.WANDERING_TRADER);
        merchant.setCustomName(ChatColor.GOLD + "Wandering Merchant");
        merchant.setCustomNameVisible(true);
        llama1 = (TraderLlama) loc.getWorld().spawnEntity(loc, EntityType.TRADER_LLAMA);
        llama2 = (TraderLlama) loc.getWorld().spawnEntity(loc, EntityType.TRADER_LLAMA);
        llama1.setLeashHolder(merchant);
        llama2.setLeashHolder(merchant);
        createShop(player);
        lastSpawn = System.currentTimeMillis();
    }

    private void createShop(Player basis) {
        List<WanderingMerchantOffer> offers = new ArrayList<>();
        int level = Main.getInstance().getLevelManager().getLevel(basis);
        for (int i = 0; i < 6; i++) {
            CustomItem item = ItemManager.getInstance().generateItem("mob", level);
            ItemStack stack = ItemUtil.createItemStackFromCustomItem(item, 1, null);
            int cost = SalvageManager.getInstance().getTotalStats(item) * 2 + 5;
            offers.add(new WanderingMerchantOffer(stack, cost, 1));
        }
        gui = new WanderingMerchantGUI(plugin, offers);
    }

    public void openShop(Player player) {
        if (gui != null) gui.open(player);
    }

    public void closeShop() {
        if (gui != null) gui.closeAll();
    }


    public void despawn() {
        if (merchant != null) { merchant.remove(); merchant = null; }
        if (llama1 != null) { llama1.remove(); llama1 = null; }
        if (llama2 != null) { llama2.remove(); llama2 = null; }
        if (gui != null) { gui.closeAll(); gui = null; }
    }

    public long getLastSpawn() { return lastSpawn; }

    public WanderingTrader getMerchant() { return merchant; }
    public WanderingMerchantGUI getGui() { return gui; }
}
