package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class KingdomCommand implements CommandExecutor, TabCompleter {
    private final EnvironmentAreaInstanceManager manager;
    private final KingdomGUI gui;

    public KingdomCommand(EnvironmentAreaInstanceManager manager, KingdomGUI gui) {
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) {
            if (!manager.teleportToKingdom(player)) {
                gui.open(player);
            }
            return true;
        }
        if ("menu".equalsIgnoreCase(args[0]) || "gui".equalsIgnoreCase(args[0])) {
            gui.open(player);
            return true;
        }
        if ("create".equalsIgnoreCase(args[0])) {
            if (manager.isInitializing(player.getUniqueId())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        "Your kingdom is already being prepared.");
                return true;
            }
            manager.removeKingdom(player.getUniqueId());
            // Capture and paste run asynchronously; initialize reports its own progress and
            // completion, so announcing success here would claim the kingdom is ready early.
            manager.initialize(player);
            return true;
        }
        if ("visit".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Usage: /kingdom visit <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Player not found.");
                return true;
            }
            manager.visit(player, target);
            return true;
        }
        if ("buildings".equalsIgnoreCase(args[0])) {
            gui.openBuildings(player);
            return true;
        }
        if ("members".equalsIgnoreCase(args[0])) {
            gui.openMembers(player);
            return true;
        }
        if ("delete".equalsIgnoreCase(args[0])) {
            gui.openDeleteConfirmation(player);
            return true;
        }
        if ("minearea".equalsIgnoreCase(args[0])) {
            return handleMineArea(player, args);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                "Usage: /kingdom [menu|create|visit <player>|buildings|members|delete|minearea]");
        return true;
    }

    /**
     * Tunes the kingdom mine plot. Corners are captured in the template world, so the plot is
     * authored once and every kingdom instance inherits it.
     */
    private boolean handleMineArea(Player player, String[] args) {
        if (!player.hasPermission("levelplugin.kingdom.admin")) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "You cannot edit the mine plot.");
            return true;
        }
        String sourceWorld = EnvironmentAreaInstanceManager.sourceWorldName();

        boolean shellCorner = args.length >= 2
                && ("shell1".equalsIgnoreCase(args[1]) || "shell2".equalsIgnoreCase(args[1]));
        if (args.length >= 2 && (shellCorner
                || "pos1".equalsIgnoreCase(args[1]) || "pos2".equalsIgnoreCase(args[1]))) {
            if (!player.getWorld().getName().equals(sourceWorld)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Stand in '" + sourceWorld + "' - mine plot corners are template coordinates.");
                return true;
            }
            int[] r = shellCorner ? manager.kingdomMineShellRegion() : manager.kingdomMineRegion();
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            boolean first = args[1].endsWith("1");
            if (shellCorner) {
                if (first) manager.setKingdomMineShellRegion(x, y, z, r[3], r[4], r[5]);
                else manager.setKingdomMineShellRegion(r[0], r[1], r[2], x, y, z);
            } else if (first) {
                manager.setKingdomMineRegion(x, y, z, r[3], r[4], r[5]);
            } else {
                manager.setKingdomMineRegion(r[0], r[1], r[2], x, y, z);
            }
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    args[1].toLowerCase() + " set to " + x + ", " + y + ", " + z + ".");
            showMineArea(player);
            return true;
        }

        if (args.length >= 8 && "set".equalsIgnoreCase(args[1])) {
            try {
                manager.setKingdomMineRegion(
                        Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]),
                        Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]));
            } catch (NumberFormatException exception) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Usage: /kingdom minearea set <x1> <y1> <z1> <x2> <y2> <z2>");
                return true;
            }
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Mine plot updated.");
            showMineArea(player);
            return true;
        }

        showMineArea(player);
        return true;
    }

    private void showMineArea(Player player) {
        int[] sh = manager.kingdomMineShellRegion();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Protected build shell: " + sh[0] + "," + sh[1] + "," + sh[2]
                        + " -> " + sh[3] + "," + sh[4] + "," + sh[5]);
        int[] r = manager.kingdomMineRegion();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Ore cuboid in " + EnvironmentAreaInstanceManager.sourceWorldName() + ": "
                        + r[0] + "," + r[1] + "," + r[2] + " -> " + r[3] + "," + r[4] + "," + r[5]
                        + "  (" + (r[3] - r[0] + 1) + "x" + (r[4] - r[1] + 1) + "x" + (r[5] - r[2] + 1) + ")");
        int[] live = manager.kingdomMineRegionInInstance(player.getUniqueId());
        if (live == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "No active kingdom; run /kingdom create to see where it lands.");
            return;
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Lands in your kingdom at: " + live[0] + "," + live[1] + "," + live[2]
                        + " -> " + live[3] + "," + live[4] + "," + live[5]);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Run /kingdom create to rebuild with the new plot.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("menu", "create", "visit", "buildings", "members", "delete", "minearea");
        if (args.length == 2 && "minearea".equalsIgnoreCase(args[0])) return List.of("pos1", "pos2", "shell1", "shell2", "set");
        return null;
    }
}
