package me.nakilex.prisonenchants;

import me.nakilex.prisonenchants.effect.EnchantEffectManager;
import me.nakilex.prisonenchants.hook.EdPrisonBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PrisonEnchantsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> EFFECTS = List.of("tornado", "blackhole", "meteors", "acidrain");

    private final JavaPlugin plugin;
    private final EnchantEffectManager effects;
    private final EdPrisonBridge edPrison;

    PrisonEnchantsCommand(JavaPlugin plugin, EnchantEffectManager effects, EdPrisonBridge edPrison) {
        this.plugin = plugin;
        this.effects = effects;
        this.edPrison = edPrison;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " test <effect> [player]");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("prisonenchants.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            plugin.reloadConfig();
            effects.reload();
            sender.sendMessage(ChatColor.GREEN + "PrisonEnchants reloaded.");
            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {
            if (!sender.hasPermission("prisonenchants.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            if (args.length < 2) return false;
            Player player = args.length >= 3 ? Bukkit.getPlayerExact(args[2])
                    : sender instanceof Player p ? p : null;
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            Location target = player.getTargetBlockExact(8) != null
                    ? player.getTargetBlockExact(8).getLocation()
                    : player.getLocation().subtract(0, 1, 0);
            effects.trigger(normalize(args[1]), player, target, Math.max(1.0,
                    edPrison.enchantLevel(player.getUniqueId(), normalize(args[1]))));
            return true;
        }

        // EdPrison executes this as console from the enchant's configured action.
        // /prisonenchants trigger <id> <player> <world> <x> <y> <z>
        if (args[0].equalsIgnoreCase("trigger") && args.length >= 7) {
            String id = normalize(args[1]);
            Player player = Bukkit.getPlayerExact(args[2]);
            World world = Bukkit.getWorld(args[3]);
            if (player == null || world == null || !EFFECTS.contains(id)) return true;
            try {
                double x = Double.parseDouble(args[4]);
                double y = Double.parseDouble(args[5]);
                double z = Double.parseDouble(args[6]);
                double level = Math.max(1.0, edPrison.enchantLevel(player.getUniqueId(), id));
                effects.trigger(id, player, new Location(world, x, y, z), level);
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Rejected malformed EdPrison enchant trigger: " + String.join(" ", args));
            }
            return true;
        }

        return false;
    }

    private String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return prefix(List.of("test", "reload"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("test")) return prefix(EFFECTS, args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("test")) {
            return prefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
        }
        return List.of();
    }

    private List<String> prefix(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(value);
        return out;
    }
}
