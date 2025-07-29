package me.nakilex.levelplugin.npc.wandering;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.salvage.managers.SalvageManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Spawns a wandering merchant NPC with a llama companion.
 * Generates a random shop inventory when spawned.
 */
public class WanderingMerchantManager {
    private final Main plugin;
    private NPC merchant;
    private Llama llama;
    private WanderingMerchantGUI gui;
    private long lastSpawn = 0L;

    public WanderingMerchantManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isActive() {
        return merchant != null && merchant.isSpawned();
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
        merchant = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, ChatColor.GOLD + "Wandering Merchant");
        merchant.spawn(loc);
        llama = (Llama) loc.getWorld().spawnEntity(loc, EntityType.LLAMA);
        llama.setLeashHolder(merchant.getEntity());
        createShop(player);
        startLlamaTask();
        lastSpawn = System.currentTimeMillis();
    }

    private void startLlamaTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive()) { cancel(); return; }
                if (llama.getLocation().distanceSquared(merchant.getEntity().getLocation()) > 25) {
                    llama.teleport(merchant.getEntity().getLocation());
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
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

    public void despawn() {
        if (merchant != null) { merchant.destroy(); merchant = null; }
        if (llama != null) { llama.remove(); llama = null; }
        if (gui != null) { gui.closeAll(); gui = null; }
    }

    public long getLastSpawn() { return lastSpawn; }

    public NPC getMerchant() { return merchant; }
    public WanderingMerchantGUI getGui() { return gui; }
}
