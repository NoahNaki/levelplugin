package me.nakilex.levelplugin.dungeon;

import me.nakilex.levelplugin.Main;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Spawns a simple NPC that walks through the rooms of the current dungeon
 * and kills nearby hostile mobs.
 */
public class DungeonNPCCommand implements CommandExecutor {
    private final Main plugin;
    private NPC npc;
    private BukkitRunnable task;

    public DungeonNPCCommand(Main plugin) {
        this.plugin = plugin;
    }

    private void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (npc != null) {
            if (npc.isSpawned()) npc.despawn();
            npc.destroy();
            npc = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            stop();
            player.sendMessage(ChatColor.YELLOW + "Dungeon helper despawned.");
            return true;
        }
        if (npc != null && npc.isSpawned()) {
            player.sendMessage(ChatColor.RED + "Helper already active.");
            return true;
        }
        Dungeon dungeon = findDungeon(player.getLocation());
        if (dungeon == null) {
            player.sendMessage(ChatColor.RED + "No dungeon found.");
            return true;
        }
        spawnHelper(player.getLocation(), dungeon);
        player.sendMessage(ChatColor.YELLOW + "Dungeon helper spawned.");
        return true;
    }

    private Dungeon findDungeon(Location loc) {
        for (Dungeon d : plugin.getDungeonManager().getActiveDungeons()) {
            if (d.getRoomContaining(loc) != null) return d;
        }
        return null;
    }

    private void spawnHelper(Location loc, Dungeon dungeon) {
        npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, ChatColor.GRAY + "Helper");
        npc.spawn(loc);
        npc.setProtected(false);

        List<Dungeon.RoomInstance> rooms = new ArrayList<>(dungeon.getRooms());
        rooms.sort(Comparator.comparingDouble(r -> r.center.distanceSquared(loc)));

        task = new BukkitRunnable() {
            int idx = 0;
            @Override
            public void run() {
                if (npc == null || !npc.isSpawned()) {
                    cancel();
                    return;
                }
                if (idx >= rooms.size()) {
                    cancel();
                    return;
                }
                Dungeon.RoomInstance r = rooms.get(idx);
                Location target = r.center.clone();
                if (npc.getEntity().getLocation().distanceSquared(target) < 4) {
                    clearHostiles(npc.getEntity().getLocation());
                    idx++;
                } else {
                    npc.getNavigator().setTarget(target);
                }
            }
        };
        task.runTaskTimer(plugin, 20L, 20L);
    }

    private void clearHostiles(Location loc) {
        for (var ent : loc.getWorld().getNearbyEntities(loc, 8, 4, 8)) {
            if (ent instanceof LivingEntity le && !(le instanceof Player)) {
                le.damage(le.getHealth(), (npc != null) ? npc.getEntity() : null);
            }
        }
    }
}
