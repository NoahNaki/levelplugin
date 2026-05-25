package me.nakilex.levelplugin.npc.commands;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.trait.LookCloseTrait;
import me.nakilex.levelplugin.npc.system.trait.SkinLayersTrait;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class NpcCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "Usage: /npc <create|skin|skinlayers|lookclose>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "skin" -> handleSkin(sender, args);
            case "skinlayers" -> handleSkinLayers(sender, args);
            case "lookclose" -> handleLookClose(sender, args);
            default -> {
                send(sender, "Unknown subcommand. Use create, skin, skinlayers, or lookclose.");
                yield true;
            }
        };
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "Only players can create NPCs.");
            return true;
        }
        if (args.length < 2) {
            send(sender, "Usage: /npc create <name> [type]");
            return true;
        }
        String name = args[1];
        EntityType type = EntityType.PLAYER;
        if (args.length >= 3) {
            try {
                type = EntityType.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                send(sender, "Unknown entity type: " + args[2]);
                return true;
            }
        }
        NPC npc = NpcApi.getRegistry().createNpc(type, name);
        npc.spawn(player.getLocation());
        send(sender, "Created NPC #" + npc.getId() + " " + ChatColor.GOLD + name + ChatColor.GRAY + " (" + type + ").");
        return true;
    }

    private boolean handleSkin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "Usage: /npc skin <id> <-c | -t <skin> <signature> <texture> | <skinName>>");
            return true;
        }
        NPC npc = getNpcById(sender, args[1]);
        if (npc == null) return true;
        SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);
        if ("-c".equalsIgnoreCase(args[2])) {
            trait.clearTexture();
            send(sender, "Cleared skin texture for NPC #" + npc.getId() + ".");
            return true;
        }
        if ("-t".equalsIgnoreCase(args[2])) {
            if (args.length < 6) {
                send(sender, "Usage: /npc skin <id> -t <skin> <signature> <texture>");
                return true;
            }
            trait.setSkinPersistent(npc, args[3], args[4], args[5]);
            send(sender, "Applied persistent skin payload to NPC #" + npc.getId() + ".");
            return true;
        }
        String skinName = args[2];
        trait.setSkinName(npc, skinName, true);
        send(sender, "Set skin name for NPC #" + npc.getId() + " to " + ChatColor.GOLD + skinName + ChatColor.GRAY + ".");
        return true;
    }

    private boolean handleSkinLayers(CommandSender sender, String[] args) {
        if (args.length < 4) {
            send(sender, "Usage: /npc skinlayers <id> <layer> <true|false>");
            return true;
        }
        NPC npc = getNpcById(sender, args[1]);
        if (npc == null) return true;
        SkinLayersTrait trait = npc.getOrAddTrait(SkinLayersTrait.class);
        SkinLayersTrait.Layer layer;
        try {
            layer = SkinLayersTrait.Layer.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            send(sender, "Unknown layer. Use: " + List.of(SkinLayersTrait.Layer.values()));
            return true;
        }
        boolean visible = Boolean.parseBoolean(args[3]);
        trait.setVisible(layer, visible);
        send(sender, "Set layer " + layer + " to " + visible + " for NPC #" + npc.getId() + ".");
        return true;
    }

    private boolean handleLookClose(CommandSender sender, String[] args) {
        if (args.length < 3) {
            send(sender, "Usage: /npc lookclose <id> <toggle|range|randomlook|perplayer>");
            return true;
        }
        NPC npc = getNpcById(sender, args[1]);
        if (npc == null) return true;
        LookCloseTrait trait = npc.getOrAddTrait(LookCloseTrait.class);
        String action = args[2].toLowerCase(Locale.ROOT);
        switch (action) {
            case "toggle" -> {
                trait.lookClose(!trait.isEnabled());
                send(sender, "Look-close " + (trait.isEnabled() ? "enabled" : "disabled") + " for NPC #" + npc.getId() + ".");
            }
            case "range" -> {
                if (args.length < 4) {
                    send(sender, "Usage: /npc lookclose <id> range <value>");
                    return true;
                }
                try {
                    trait.setRange(Double.parseDouble(args[3]));
                } catch (NumberFormatException ex) {
                    send(sender, "Range must be a number.");
                    return true;
                }
                send(sender, "Look-close range set to " + trait.getRange() + " for NPC #" + npc.getId() + ".");
            }
            case "randomlook" -> {
                if (args.length < 4) {
                    send(sender, "Usage: /npc lookclose <id> randomlook <true|false>");
                    return true;
                }
                trait.setRandomLookEnabled(Boolean.parseBoolean(args[3]));
                send(sender, "Random look set to " + trait.isRandomLookEnabled() + " for NPC #" + npc.getId() + ".");
            }
            case "perplayer" -> {
                if (args.length < 4) {
                    send(sender, "Usage: /npc lookclose <id> perplayer <true|false>");
                    return true;
                }
                trait.setPerPlayer(Boolean.parseBoolean(args[3]));
                send(sender, "Per-player look set to " + trait.isPerPlayer() + " for NPC #" + npc.getId() + ".");
            }
            default -> send(sender, "Unknown lookclose action. Use toggle, range, randomlook, perplayer.");
        }
        return true;
    }

    private NPC getNpcById(CommandSender sender, String rawId) {
        int id;
        try {
            id = Integer.parseInt(rawId);
        } catch (NumberFormatException ex) {
            send(sender, "Invalid NPC id: " + rawId);
            return null;
        }
        NPC npc = NpcApi.getRegistry().getById(id);
        if (npc == null) {
            send(sender, "NPC #" + id + " was not found.");
        }
        return npc;
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.GRAY + ChatUtil.applyEmojis(message));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("create", "skin", "skinlayers", "lookclose");
        }
        if (args.length == 3 && "lookclose".equalsIgnoreCase(args[0])) {
            return List.of("toggle", "range", "randomlook", "perplayer");
        }
        if (args.length == 3 && "skinlayers".equalsIgnoreCase(args[0])) {
            return List.of(SkinLayersTrait.Layer.values()).stream().map(Enum::name).collect(Collectors.toList());
        }
        if (args.length == 4 && "skinlayers".equalsIgnoreCase(args[0])) {
            return List.of("true", "false");
        }
        List<String> ids = new ArrayList<>();
        for (NPC npc : NpcApi.getRegistry().sorted()) {
            ids.add(String.valueOf(npc.getId()));
        }
        if (args.length == 2 && !"create".equalsIgnoreCase(args[0])) {
            return ids;
        }
        return List.of();
    }
}
