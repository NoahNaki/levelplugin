package me.nakilex.levelplugin.playerhead;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Developer preview for the dynamic player-object shader. */
public final class PlayerModelCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("This preview must be opened by a player.");
            return true;
        }

        Player target = args.length == 0 ? viewer : Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            viewer.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return true;
        }

        PlayerModelComponent.Type type = parseType(args.length > 1
                ? args[1] : PlayerModelTooltip.DEFAULT_TYPE.name());
        Integer offset = parseRange(args, 2, PlayerModelTooltip.DEFAULT_OFFSET, 0, 15);
        Integer scale = parseRange(args, 3, PlayerModelTooltip.DEFAULT_SCALE, 1, 15);
        Double speed = parseSpeed(args, 4, PlayerModelTooltip.DEFAULT_SPEED);
        PlayerModelComponent.Animation animation = parseAnimation(args.length > 5
                ? args[5] : PlayerModelTooltip.DEFAULT_ANIMATION.name());
        if (type == null || offset == null || scale == null || speed == null || animation == null) {
            viewer.sendMessage(Component.text(
                    "Usage: /playermodel [player] [head|bust|full] [offset 0-15] [scale 1-15] "
                            + "[speed 0.0-4.0] [idle|wave|walk|run|attack|crouch]",
                    NamedTextColor.RED));
            return true;
        }

        Component tooltip = PlayerModelTooltip.create(target, type, offset, scale, speed, animation);

        viewer.sendMessage(Component.text("Hover to preview " + target.getName(), NamedTextColor.AQUA)
                .hoverEvent(HoverEvent.showText(tooltip)));
        return true;
    }

    private static PlayerModelComponent.Type parseType(String input) {
        try {
            return PlayerModelComponent.Type.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static PlayerModelComponent.Animation parseAnimation(String input) {
        try {
            return PlayerModelComponent.Animation.valueOf(input.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Double parseSpeed(String[] args, int index, double fallback) {
        if (args.length <= index) return fallback;
        try {
            double value = Double.parseDouble(args[index]);
            return Double.isFinite(value) && value >= 0.0 && value <= 4.0 ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Integer parseRange(String[] args, int index, int fallback, int min, int max) {
        if (args.length <= index) return fallback;
        try {
            int value = Integer.parseInt(args[index]);
            return value >= min && value <= max ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) names.add(player.getName());
            }
            return names;
        }
        if (args.length == 2) return filter(args[1], Arrays.asList("head", "bust", "full"));
        if (args.length == 3) return filter(args[2], Arrays.asList("0", "4", "8", "12"));
        if (args.length == 4) return filter(args[3], Arrays.asList("1", "2", "3", "4"));
        if (args.length == 5) return filter(args[4], Arrays.asList("0", "0.5", "1", "1.5", "2", "3", "4"));
        if (args.length == 6) return filter(args[5], Arrays.asList("idle", "wave", "walk", "run", "attack", "crouch"));
        return List.of();
    }

    private static List<String> filter(String input, List<String> choices) {
        String prefix = input.toLowerCase(Locale.ROOT);
        return choices.stream().filter(choice -> choice.startsWith(prefix)).toList();
    }
}
