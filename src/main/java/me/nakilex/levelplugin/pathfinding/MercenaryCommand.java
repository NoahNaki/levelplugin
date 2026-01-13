package me.nakilex.levelplugin.pathfinding;

import me.nakilex.levelplugin.pathfinding.MercenaryManager.Mode;
import me.nakilex.levelplugin.pathfinding.npc.ArcherMercenary;
import me.nakilex.levelplugin.pathfinding.npc.RogueMercenary;
import me.nakilex.levelplugin.pathfinding.npc.MageMercenary;
import me.nakilex.levelplugin.pathfinding.npc.PathNpc;
import me.nakilex.levelplugin.pathfinding.npc.WarriorMercenary;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Command to bind and control mercenary NPCs. */
public class MercenaryCommand implements CommandExecutor, TabCompleter {
    private final MercenaryManager manager;

    public MercenaryCommand(MercenaryManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
              sender.sendMessage("Usage: /mercenary bind <id> <class> <player> | unbind <id> <player> | hostile | target");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
              case "bind" -> {
                  if (args.length < 4) {
                      sender.sendMessage("Usage: /mercenary bind <id> <class> <player>");
                      return true;
                  }
                  int id;
                  try {
                      id = Integer.parseInt(args[1]);
                  } catch (NumberFormatException e) {
                      sender.sendMessage("Invalid mercenary id");
                      return true;
                  }
                  String cls = args[2].toLowerCase(Locale.ROOT);
                  PathNpc profile = switch (cls) {
                      case "rogue" -> new RogueMercenary();
                      case "mage" -> new MageMercenary();
                      case "warrior" -> new WarriorMercenary();
                      case "archer" -> new ArcherMercenary();
                      default -> null;
                  };
                  if (profile == null) {
                      sender.sendMessage("Unknown class. Use rogue, mage, warrior or archer");
                      return true;
                  }
                  Player target = Bukkit.getPlayer(args[3]);
                  if (target == null) {
                      sender.sendMessage("Player not found");
                      return true;
                  }
                  boolean bound = manager.bind(id, target, profile);
                  sender.sendMessage(bound ? "Mercenary bound" : "Failed to bind mercenary");
                  return true;
              }
            case "unbind" -> {
                if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("Player not found");
                        return true;
                    }
                    boolean unbound = manager.unbindAll(target);
                    sender.sendMessage(unbound ? "All mercenaries unbound" : "No mercenaries to unbind");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("Usage: /mercenary unbind <id> <player> or /mercenary unbind <player>");
                    return true;
                }
                int id;
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid mercenary id");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage("Player not found");
                    return true;
                }
                boolean unbound = manager.unbind(id, target);
                sender.sendMessage(unbound ? "Mercenary unbound" : "Failed to unbind mercenary");
                return true;
            }
            case "hostile" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can set mercenary mode");
                    return true;
                }
                if (manager.setMode(p, Mode.HOSTILE)) {
                    sender.sendMessage("Mercenary set to hostile mode");
                } else {
                    sender.sendMessage("You have no bound mercenary");
                }
                return true;
            }
            case "target" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can set mercenary mode");
                    return true;
                }
                if (manager.setMode(p, Mode.TARGET)) {
                    sender.sendMessage("Mercenary set to target mode");
                } else {
                    sender.sendMessage("You have no bound mercenary");
                }
                return true;
            }
            default -> sender.sendMessage("Unknown subcommand");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("bind", "unbind", "hostile", "target").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("bind")) {
                List<String> ids = new ArrayList<>();
                for (NPC npc : NpcApi.getRegistry().sorted()) {
                    String id = Integer.toString(npc.getId());
                    if (id.startsWith(args[1])) ids.add(id);
                }
                return ids;
            }
            if (args[0].equalsIgnoreCase("unbind")) {
                List<String> out = new ArrayList<>();
                for (NPC npc : NpcApi.getRegistry().sorted()) {
                    String id = Integer.toString(npc.getId());
                    if (id.startsWith(args[1])) out.add(id);
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String name = p.getName();
                    if (name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(name);
                }
                return out;
            }
        }
          if (args.length == 3) {
              if (args[0].equalsIgnoreCase("bind")) {
                  return List.of("rogue", "mage", "warrior", "archer").stream()
                          .filter(c -> c.startsWith(args[2].toLowerCase(Locale.ROOT)))
                          .collect(Collectors.toList());
              }
              if (args[0].equalsIgnoreCase("unbind")) {
                  return Bukkit.getOnlinePlayers().stream()
                          .map(Player::getName)
                          .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                          .collect(Collectors.toList());
              }
          }
          if (args.length == 4 && args[0].equalsIgnoreCase("bind")) {
              return Bukkit.getOnlinePlayers().stream()
                      .map(Player::getName)
                      .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[3].toLowerCase(Locale.ROOT)))
                      .collect(Collectors.toList());
          }
          return new ArrayList<>();
    }
}
