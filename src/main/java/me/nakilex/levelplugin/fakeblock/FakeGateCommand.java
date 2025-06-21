package me.nakilex.levelplugin.fakeblock;

import me.nakilex.levelplugin.Main;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.nakilex.levelplugin.fakeblock.GateAnimation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Simple in-game editor for fake gates using a wand similar to WorldEdit.
 */
public class FakeGateCommand implements CommandExecutor, Listener {
    private final QuestGateManager manager;
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final ItemStack wand;

    public FakeGateCommand(Main plugin) {
        this.manager = plugin.getQuestGateManager();
        // Configure wand item
        wand = new ItemStack(Material.MACE);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Fake Gate Mace");
            wand.setItemMeta(meta);
        }

        plugin.getCommand("fakegate").setExecutor(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players.");
            return true;
        }
        if (args.length == 0) return false;
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "wand":
                player.getInventory().addItem(wand.clone());
                player.sendMessage(ChatColor.GREEN + "Wand given.");
                return true;
            case "list":
                var ids = manager.getGateIds();
                if (ids.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "No gates defined.");
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (String idKey : ids) {
                        QuestGate g = manager.getGate(idKey);
                        boolean closed = g != null && g.isClosed(player.getUniqueId());
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(idKey).append("(").append(closed ? "closed" : "open").append(")");
                    }
                    player.sendMessage(ChatColor.YELLOW + "Gates: " + sb);
                }
                return true;
            case "create":
                if (args.length < 3) return false;
                Selection sel = selections.get(player.getUniqueId());
                if (sel == null || sel.pos1 == null || sel.pos2 == null) {
                    player.sendMessage(ChatColor.RED + "Select two positions first.");
                    return true;
                }
                String id = args[1].toLowerCase();
                Material mat = Material.matchMaterial(args[2]);
                if (mat == null) mat = Material.BARRIER;
                GateAnimation anim = args.length > 3 ? GateAnimation.fromString(args[3]) : GateAnimation.INSTANT;
                long ticks = 40L;
                if (args.length > 4) {
                    try { ticks = Math.round(Double.parseDouble(args[4]) * 20.0); } catch (NumberFormatException ignored) {}
                }
                QuestGate gate = new QuestGate(id, sel.pos1, sel.pos2, mat.createBlockData(), true, anim, ticks);
                manager.createGate(gate);
                player.sendMessage(ChatColor.YELLOW + "Gate " + id + " created and closed.");
                return true;
            case "toggle":
                if (args.length < 2) return false;
                if (manager.toggleGate(player, args[1])) {
                    QuestGate g = manager.getGate(args[1]);
                    boolean closed = g != null && g.isClosed(player.getUniqueId());
                    player.sendMessage(ChatColor.YELLOW + "Gate " + args[1] + " is now " + (closed ? "closed" : "open") + ".");
                } else {
                    player.sendMessage(ChatColor.RED + "Gate not found.");
                }
                return true;
            case "remove":
                if (args.length < 2) return false;
                if (manager.removeGate(args[1])) {
                    player.sendMessage(ChatColor.GREEN + "Gate removed.");
                } else {
                    player.sendMessage(ChatColor.RED + "Gate not found.");
                }
                return true;
            case "debug":
                boolean enabled = manager.toggleDebug();
                player.sendMessage(ChatColor.YELLOW + "Gate debug " + (enabled ? "enabled" : "disabled"));
                return true;
            default:
                return false;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack inHand = event.getItem();
        if (inHand == null || !inHand.isSimilar(wand)) return;
        if (event.getClickedBlock() == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        Selection sel = selections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
        if (event.getAction().name().contains("LEFT")) {
            sel.pos1 = event.getClickedBlock().getLocation();
            player.sendMessage(ChatColor.AQUA + "Pos1 set " + format(sel.pos1));
        } else if (event.getAction().name().contains("RIGHT")) {
            sel.pos2 = event.getClickedBlock().getLocation();
            player.sendMessage(ChatColor.AQUA + "Pos2 set " + format(sel.pos2));
        }
    }

    private static String format(Location loc) {
        return loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
    }

    private static class Selection {
        Location pos1;
        Location pos2;
    }
}
