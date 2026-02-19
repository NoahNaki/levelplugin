package me.nakilex.levelplugin.debug.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.spells.impl.MageFireballBasicAttackSpell;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CommandUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MageFireballDebugCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final Map<UUID, ArmorStand> previewMap = new HashMap<>();

    public MageFireballDebugCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR, "Only players can use this command.");
            return true;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "ModelEngine is not enabled.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "spawn" -> handleSpawn(player, args);
            case "move" -> handleMove(player, args);
            case "rotate" -> handleRotate(player, args);
            case "face" -> handleFace(player);
            case "info" -> handleInfo(player);
            case "diag", "diagnose" -> handleDiagnose(player);
            case "trace" -> handleTrace(player, args);
            case "remove" -> handleRemove(player);
            default -> {
                sendUsage(player);
                yield true;
            }
        };
    }

    private boolean handleSpawn(Player player, String[] args) {
        removePreview(player);
        Vector dir = player.getEyeLocation().getDirection().clone();
        MageFireballBasicAttackSpell.FireballSpawnResult spawnResult =
                MageFireballBasicAttackSpell.spawnProjectileAnchor(plugin, player.getEyeLocation().clone(), dir);
        if (spawnResult == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Unable to spawn fireball preview (invalid world or direction).");
            return true;
        }
        ArmorStand stand = spawnResult.anchor();
        ModelEngineUtil.ModelApplyResult result = spawnResult.modelResult();
        previewMap.put(player.getUniqueId(), stand);

        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Spawned fireball preview. applied=" + result.applied() + " failed=" + result.failed());
        if (args.length == 4) {
            return handleMove(player, new String[]{"move", "relative", args[1], args[2], args[3]});
        }
        return true;
    }

    private boolean handleDiagnose(Player player) {
        List<String> loaded = ModelEngineUtil.getModelIdsSafely(plugin);
        List<String> blueprints = ModelEngineUtil.getBlueprintModelIds(plugin);
        List<String> candidates = MageFireballBasicAttackSpell.modelCandidates();

        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Loaded ModelEngine ids count=" + loaded.size());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Blueprint ids count=" + blueprints.size());

        for (String candidate : candidates) {
            boolean loadedMatch = loaded.stream().anyMatch(id -> id.equalsIgnoreCase(candidate));
            boolean blueprintMatch = blueprints.stream().anyMatch(id -> id.equalsIgnoreCase(candidate));
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Candidate '" + candidate + "': loaded=" + loadedMatch + " blueprint=" + blueprintMatch);
        }

        ArmorStand test = player.getWorld().spawn(player.getLocation().add(0, 1.2, 0), ArmorStand.class, it -> {
            it.setInvisible(true);
            it.setMarker(false);
            it.setSmall(true);
            it.setGravity(false);
        });
        ModelEngineUtil.ModelApplyResult result =
                ModelEngineUtil.applyFirstAvailableModel(test, candidates, plugin);
        test.remove();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Apply test result: applied=" + result.applied()
                        + " failed=" + result.failed() + " blueprintOnly=" + result.blueprintOnly());
        return true;
    }

    private boolean handleTrace(Player player, String[] args) {
        if (args.length < 2) {
            boolean enabled = MageFireballBasicAttackSpell.isDebugEnabled(player.getUniqueId());
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Fireball trace is " + (enabled ? "ON" : "OFF") + ". Use /fireballdebug trace <on|off>.");
            return true;
        }
        boolean enable = args[1].equalsIgnoreCase("on");
        if (!enable && !args[1].equalsIgnoreCase("off")) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /fireballdebug trace <on|off>");
            return true;
        }
        MageFireballBasicAttackSpell.setDebugEnabled(player.getUniqueId(), enable);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Fireball trace set to " + (enable ? "ON" : "OFF") + ".");
        return true;
    }

    private boolean handleMove(Player player, String[] args) {
        ArmorStand stand = getPreview(player);
        if (stand == null) {
            return true;
        }
        Location loc = stand.getLocation().clone();
        if (args.length == 3) {
            String axis = args[1].toLowerCase(Locale.ROOT);
            double amount;
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Amount must be a number.");
                return true;
            }
            Vector delta = axisVector(player, axis, amount);
            if (delta == null) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Axis must be forward|back|right|left|up|down.");
                return true;
            }
            loc.add(delta);
        } else if (args.length == 5 && args[1].equalsIgnoreCase("relative")) {
            try {
                double forward = Double.parseDouble(args[2]);
                double up = Double.parseDouble(args[3]);
                double right = Double.parseDouble(args[4]);
                Vector forwardVec = player.getEyeLocation().getDirection().clone().normalize().multiply(forward);
                Vector rightVec = forwardVec.clone().setY(0).normalize().crossProduct(new Vector(0, 1, 0)).multiply(-right);
                loc.add(forwardVec).add(rightVec).add(0, up, 0);
            } catch (NumberFormatException ex) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "Usage: /fireballdebug move relative <forward> <up> <right>");
                return true;
            }
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /fireballdebug move <forward|back|right|left|up|down> <amount>");
            return true;
        }
        stand.teleport(loc);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Moved preview to x=" + String.format("%.2f", loc.getX())
                        + " y=" + String.format("%.2f", loc.getY())
                        + " z=" + String.format("%.2f", loc.getZ()));
        return true;
    }

    private boolean handleRotate(Player player, String[] args) {
        ArmorStand stand = getPreview(player);
        if (stand == null) {
            return true;
        }
        if (args.length < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /fireballdebug rotate <yawDelta> [pitchDelta]");
            return true;
        }
        double yawDelta;
        double pitchDelta = 0.0;
        try {
            yawDelta = Double.parseDouble(args[1]);
            if (args.length >= 3) {
                pitchDelta = Double.parseDouble(args[2]);
            }
        } catch (NumberFormatException ex) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Rotation values must be numbers.");
            return true;
        }
        Location loc = stand.getLocation().clone();
        loc.setYaw((float) (loc.getYaw() + yawDelta));
        loc.setPitch((float) Math.max(-89.0, Math.min(89.0, loc.getPitch() + pitchDelta)));
        stand.teleport(loc);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Preview rotation yaw=" + String.format("%.1f", loc.getYaw())
                        + " pitch=" + String.format("%.1f", loc.getPitch()));
        return true;
    }

    private boolean handleFace(Player player) {
        ArmorStand stand = getPreview(player);
        if (stand == null) {
            return true;
        }
        ModelEngineUtil.orientEntityToVector(stand, player.getEyeLocation().getDirection());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Aligned preview to your current aim direction.");
        return true;
    }

    private boolean handleInfo(Player player) {
        ArmorStand stand = getPreview(player);
        if (stand == null) {
            return true;
        }
        Location loc = stand.getLocation();
        Location eye = player.getEyeLocation();
        Vector delta = loc.toVector().subtract(eye.toVector());
        Vector forward = eye.getDirection().clone().normalize();
        Vector flatForward = forward.clone().setY(0).normalize();
        Vector right = flatForward.clone().crossProduct(new Vector(0, 1, 0)).multiply(-1);
        double forwardOffset = delta.dot(forward);
        double rightOffset = delta.dot(right);
        double upOffset = delta.getY();

        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Current offsets from eye: forward=" + String.format("%.2f", forwardOffset)
                        + " up=" + String.format("%.2f", upOffset)
                        + " right=" + String.format("%.2f", rightOffset));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Rotation yaw=" + String.format("%.1f", loc.getYaw())
                        + " pitch=" + String.format("%.1f", loc.getPitch()));
        return true;
    }

    private boolean handleRemove(Player player) {
        removePreview(player);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Removed fireball preview.");
        return true;
    }

    private Vector axisVector(Player player, String axis, double amount) {
        Vector forward = player.getEyeLocation().getDirection().clone().normalize();
        Vector flatForward = forward.clone().setY(0);
        if (flatForward.lengthSquared() <= 0.00001) {
            flatForward = new Vector(0, 0, 1);
        }
        flatForward.normalize();
        Vector right = flatForward.clone().crossProduct(new Vector(0, 1, 0)).multiply(-1).normalize();
        return switch (axis) {
            case "forward" -> flatForward.multiply(amount);
            case "back" -> flatForward.multiply(-amount);
            case "right" -> right.multiply(amount);
            case "left" -> right.multiply(-amount);
            case "up" -> new Vector(0, amount, 0);
            case "down" -> new Vector(0, -amount, 0);
            default -> null;
        };
    }

    private ArmorStand getPreview(Player player) {
        ArmorStand stand = previewMap.get(player.getUniqueId());
        if (stand == null || !stand.isValid()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "No preview active. Run /fireballdebug spawn first.");
            previewMap.remove(player.getUniqueId());
            return null;
        }
        return stand;
    }

    private void removePreview(Player player) {
        ArmorStand existing = previewMap.remove(player.getUniqueId());
        if (existing != null && existing.isValid()) {
            existing.remove();
        }
    }

    private void sendUsage(Player player) {
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "/fireballdebug spawn | move | rotate | face | info | remove");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "/fireballdebug diagnose - checks loaded model ids and apply result");
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "/fireballdebug trace <on|off> - verbose cast/runtime debug chat");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandUtil.filterStartingWith(List.of("spawn", "move", "rotate", "face", "info", "diagnose", "trace", "remove"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("move")) {
            return CommandUtil.filterStartingWith(List.of("forward", "back", "right", "left", "up", "down"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("trace")) {
            return CommandUtil.filterStartingWith(List.of("on", "off"), args[1]);
        }
        return List.of();
    }
}
