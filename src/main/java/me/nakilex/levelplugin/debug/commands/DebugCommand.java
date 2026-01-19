package me.nakilex.levelplugin.debug.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.chat.games.ChatGameStatus;
import me.nakilex.levelplugin.debug.gui.DebugGUI;
import me.nakilex.levelplugin.debug.BeaconEntityDebugManager;
import me.nakilex.levelplugin.debug.DropDebugManager;
import me.nakilex.levelplugin.debug.SpellInputDebugItem;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.pathfinding.DungeonExpeditionManager;
import me.nakilex.levelplugin.particles.ParticleAxis;
import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.RewardBombUtil;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TeleportUtils;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import me.nakilex.levelplugin.guild.GuildManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.key.Key;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Root debug command that hosts various developer utilities.
 * Supported subcommands:
 * <ul>
 *   <li><code>mobinfo</code> – toggles MythicMob kill debug output</li>
 *   <li><code>tps</code> – toggles TPS display on the sidebar scoreboard</li>
 *   <li><code>siege</code> – toggles fast guild siege capture mode</li>
 * </ul>
 */
public class DebugCommand implements TabExecutor {
    private final PlayerToggleManager mobDebugManager;
    private final PlayerScoreboardManager scoreboardManager;
    private final DebugGUI debugGUI;
    private final ChatGameManager chatGameManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final DungeonExpeditionManager dungeonExpeditionManager;
    private final DropDebugManager dropDebugManager;
    private final EnvironmentManager environmentManager;
    private final BeaconEntityDebugManager beaconEntityDebugManager;
    private final QuestManager questManager;

    public DebugCommand(PlayerToggleManager mobDebugManager,
                        PlayerScoreboardManager scoreboardManager,
                        DebugGUI debugGUI,
                        ChatGameManager chatGameManager,
                        MercenaryExpeditionManager expeditionManager,
                        DungeonExpeditionManager dungeonExpeditionManager,
                        DropDebugManager dropDebugManager,
                        EnvironmentManager environmentManager,
                        BeaconEntityDebugManager beaconEntityDebugManager,
                        QuestManager questManager) {
        this.mobDebugManager = mobDebugManager;
        this.scoreboardManager = scoreboardManager;
        this.debugGUI = debugGUI;
        this.chatGameManager = chatGameManager;
        this.expeditionManager = expeditionManager;
        this.dungeonExpeditionManager = dungeonExpeditionManager;
        this.dropDebugManager = dropDebugManager;
        this.environmentManager = environmentManager;
        this.beaconEntityDebugManager = beaconEntityDebugManager;
        this.questManager = questManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) {
                debugGUI.open(p);
            } else {
                String statUsage = Arrays.stream(StatType.values())
                        .map(StatType::getAbbrev)
                        .collect(Collectors.joining("|"));
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege|cityowner|citymax|chatgame|expedition|dungeonexpedition|beaconentity|spellinput|particle|particlepath|" + statUsage + ">");
            }
            return true;
        }

        String sub = args[0].toLowerCase();
        StatType statType = StatType.fromAbbrev(sub);
        if (statType != null) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Players only.");
                return true;
            }
            StatsManager statsMgr = StatsManager.getInstance();
            StatsManager.PlayerStats ps = statsMgr.getPlayerStats(p.getUniqueId());
            if (args.length >= 2) {
                try {
                    int val = Integer.parseInt(args[1]);
                    statsMgr.setBaseStat(ps, statType, val);
                    statsMgr.recalcDerivedStats(p);
                    if (statType == StatType.TEC) {
                        p.sendMessage(String.format("%s set to %d (%.2f atk/s)",
                                statType.getDisplayName(), val, ps.attackSpeed));
                    } else {
                        p.sendMessage(String.format("%s set to %d", statType.getDisplayName(), val));
                    }
                } catch (NumberFormatException e) {
                    p.sendMessage("Usage: /debug " + statType.getAbbrev() + " <value>");
                }
            } else {
                int total = statsMgr.getStatValue(p, statType);
                if (statType == StatType.TEC) {
                    p.sendMessage(String.format("%s: %d (%.2f atk/s)",
                            statType.getDisplayName(), total, ps.attackSpeed));
                } else {
                    p.sendMessage(String.format("%s: %d", statType.getDisplayName(), total));
                }
            }
            return true;
        }

        switch (sub) {
            case "mobinfo":
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can toggle mob info debugging.");
                    return true;
                }
                boolean enabled = mobDebugManager.toggle(p);
                ToggleFeedbackUtil.sendToggle(p, "Mob info debug", enabled);
                return true;

            case "tps":
                if (!(sender instanceof Player p2)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                boolean tpsEnabled = scoreboardManager.toggleTps(p2);
                ToggleFeedbackUtil.sendToggle(p2, "TPS display", tpsEnabled);
                return true;

            case "siege":
                boolean fast = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().toggleFastCapture();
                sender.sendMessage("Fast siege mode " + (fast ? "enabled" : "disabled"));
                return true;

            case "expedition":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                boolean enable = !expeditionManager.isInstantExpeditions();
                expeditionManager.setInstantExpeditions(enable);
                sender.sendMessage(ChatColor.YELLOW + "Expedition timers "
                        + (enable ? ChatColor.GREEN + "set to instant" : ChatColor.RED + "restored to normal") + ChatColor.YELLOW + ".");
                return true;

            case "dungeonexpedition":
                if (!(sender instanceof Player dungeonPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                dungeonExpeditionManager.startCrimsonReliquaryExpedition(dungeonPlayer);
                return true;

            case "drops":
                boolean forced = dropDebugManager.toggleForceMobDrops();
                sender.sendMessage(ChatColor.YELLOW + "Mob loot drops are now "
                        + (forced ? ChatColor.GREEN + "100%" : ChatColor.RED + "respecting configured chances")
                        + ChatColor.YELLOW + ".");
                return true;

            case "cityowner":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /debug cityowner <guild name>");
                    return true;
                }
                String guildName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                boolean updated = GuildSiegeManager.getInstance().debugAssignOwner(guildName);
                if (updated) {
                    sender.sendMessage(ChatColor.GREEN + "Assigned castle ownership to guild " + guildName + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + "Could not assign ownership. Is the guild name correct?");
                }
                return true;

            case "citymax":
                if (!(sender instanceof Player cityPlayer)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (!hasTownOwnership(cityPlayer)) {
                    return true;
                }
                EnvironmentManager.TownMaxResult result = environmentManager.maxTownProgress(cityPlayer);
                cityPlayer.sendMessage(result.message());
                return true;

            case "rewardbomb":
                if (!(sender instanceof Player pRb)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                org.bukkit.block.Block target = pRb.getTargetBlockExact(20);
                if (target == null) {
                    pRb.sendMessage(ChatColor.RED + "Look at a block within 20 blocks to start the reward bomb.");
                    return true;
                }
                RewardBombUtil.startRewardBomb(Main.getInstance(), target.getLocation(),
                        me.nakilex.levelplugin.debug.DebugRewardUtil::rollDebugReward, 100, pRb);
                pRb.sendMessage(ChatColor.YELLOW + "Reward bomb triggered for testing.");
                return true;

            case "spellinput":
                if (!(sender instanceof Player spellPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                spellPlayer.getInventory().addItem(SpellInputDebugItem.create());
                ChatMessageUtil.send(spellPlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Spell input debug stick added to your inventory.");
                return true;

            case "beaconentity":
                if (!(sender instanceof Player beaconPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                BeaconEntityDebugManager.ToggleOutcome toggle = beaconEntityDebugManager.toggle(beaconPlayer);
                if (!toggle.success()) {
                    if (toggle.errorMessage() != null) {
                        beaconPlayer.sendMessage(toggle.errorMessage());
                    }
                    return true;
                }
                ToggleFeedbackUtil.sendToggle(beaconPlayer, "Beacon entity", toggle.enabled());
                return true;

            case "particle":
                if (!(sender instanceof Player particlePlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                if (args.length < 2) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.WARNING,
                            "Usage: /debug particle <type> [count] [ticks] [center=self|look]"
                                    + " | /debug particle ring <type> <radius> [points] [ticks] [axis=x|y|z|look] [center=self|look]"
                                    + " | /debug particle arc <type> <radius> <degrees> [points] [ticks] [axis=x|y|z|look] [center=self|look]");
                    return true;
                }
                String mode = args[1].toLowerCase(Locale.ROOT);
                int argOffset = 2;
                boolean explicitMode = mode.equals("spawn") || mode.equals("ring") || mode.equals("arc");
                if (!explicitMode) {
                    mode = "spawn";
                    argOffset = 1;
                }
                if (args.length <= argOffset) {
                    String usage = switch (mode) {
                        case "ring" -> "Usage: /debug particle ring <particle> <radius> [points] [ticks] [axis=x|y|z|look] [center=self|look]";
                        case "arc" -> "Usage: /debug particle arc <particle> <radius> <degrees> [points] [ticks] [axis=x|y|z|look] [center=self|look]";
                        default -> "Usage: /debug particle spawn <particle> [count] [ticks] [center=self|look]";
                    };
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.WARNING, usage);
                    return true;
                }
                String particleName = args[argOffset];
                ParticleService particleService = ParticleService.getInstance();
                Optional<ParticlePreset> resolved = particleService.resolvePreset(particleName);
                if (resolved.isEmpty()) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.ERROR,
                            "Unknown particle type: " + particleName + ".");
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.INFO,
                            "Available presets: " + String.join(", ", particleService.getPresetNames()));
                    return true;
                }
                ParticlePreset preset = resolved.get();
                if (mode.equals("ring")) {
                    if (args.length <= argOffset + 1) {
                        ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.WARNING,
                                "Usage: /debug particle ring <particle> <radius> [points] [ticks] [axis=x|y|z|look] [center=self|look]");
                        return true;
                    }
                    double radius = parsePositiveDouble(args, argOffset + 1, "Radius must be a positive number.", particlePlayer);
                    if (radius <= 0) {
                        return true;
                    }
                    ParseIntResult pointsResult = parseOptionalInt(args, argOffset + 2, 24, "Points must be a positive number.", particlePlayer);
                    if (pointsResult.value() <= 0) {
                        return true;
                    }
                    ParseIntResult ticksResult = parseOptionalInt(args, pointsResult.nextIndex(), 100, "Ticks must be a positive number.", particlePlayer);
                    if (ticksResult.value() <= 0) {
                        return true;
                    }
                    ParticleOptions options = parseOptions(args, ticksResult.nextIndex(), ParticleAxis.Y, CenterMode.LOOK, particlePlayer);
                    Location center = resolveCenter(particlePlayer, options.centerMode());
                    runParticleShape(particlePlayer, ticksResult.value(), () ->
                            particleService.sendRing(particlePlayer, preset, center, radius, pointsResult.value(),
                                    options.axis(), particlePlayer.getLocation()));
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.SUCCESS,
                            "Spawned ring of " + particleName.toUpperCase(Locale.ROOT) + " particles for 5 seconds.");
                    return true;
                }
                if (mode.equals("arc")) {
                    if (args.length <= argOffset + 2) {
                        ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.WARNING,
                                "Usage: /debug particle arc <particle> <radius> <degrees> [points] [ticks] [axis=x|y|z|look] [center=self|look]");
                        return true;
                    }
                    double radius = parsePositiveDouble(args, argOffset + 1, "Radius must be a positive number.", particlePlayer);
                    if (radius <= 0) {
                        return true;
                    }
                    double degrees = parsePositiveDouble(args, argOffset + 2, "Degrees must be a positive number.", particlePlayer);
                    if (degrees <= 0) {
                        return true;
                    }
                    ParseIntResult pointsResult = parseOptionalInt(args, argOffset + 3, 24, "Points must be a positive number.", particlePlayer);
                    if (pointsResult.value() <= 0) {
                        return true;
                    }
                    ParseIntResult ticksResult = parseOptionalInt(args, pointsResult.nextIndex(), 100, "Ticks must be a positive number.", particlePlayer);
                    if (ticksResult.value() <= 0) {
                        return true;
                    }
                    ParticleOptions options = parseOptions(args, ticksResult.nextIndex(), ParticleAxis.LOOK, CenterMode.LOOK, particlePlayer);
                    Location center = resolveCenter(particlePlayer, options.centerMode());
                    runParticleShape(particlePlayer, ticksResult.value(), () ->
                            particleService.sendArc(particlePlayer, preset, center, radius, degrees, pointsResult.value(),
                                    options.axis(), particlePlayer.getLocation()));
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.SUCCESS,
                            "Spawned arc of " + particleName.toUpperCase(Locale.ROOT) + " particles for 5 seconds.");
                    return true;
                }
                ParseIntResult countResult = parseOptionalInt(args, argOffset + 1, 8, "Particle count must be a positive number.", particlePlayer);
                if (countResult.value() <= 0) {
                    return true;
                }
                ParseIntResult ticksResult = parseOptionalInt(args, countResult.nextIndex(), 100, "Ticks must be a positive number.", particlePlayer);
                if (ticksResult.value() <= 0) {
                    return true;
                }
                ParticleOptions options = parseOptions(args, ticksResult.nextIndex(), ParticleAxis.Y, CenterMode.SELF, particlePlayer);
                Location center = resolveCenter(particlePlayer, options.centerMode()).add(0, 1.0, 0);
                try {
                    runParticleShape(particlePlayer, ticksResult.value(), () ->
                            particleService.sendToPlayer(particlePlayer, preset, center, countResult.value()));
                } catch (Exception e) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.ERROR,
                            "Failed to spawn particle: " + e.getMessage());
                    return true;
                }
                ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Spawned " + countResult.value() + " " + particleName.toUpperCase(Locale.ROOT) + " particles for 5 seconds.");
                return true;

            case "particlepath":
                if (!(sender instanceof Player particlePlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                if (args.length < 2) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.WARNING,
                            "Usage: /debug particlepath <npcId|off>");
                    return true;
                }
                String rawTarget = args[1];
                if (rawTarget.equalsIgnoreCase("off") || rawTarget.equalsIgnoreCase("clear")) {
                    questManager.clearParticlePathDebugTarget(particlePlayer.getUniqueId());
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.SUCCESS,
                            "Quest particle path debug disabled.");
                    return true;
                }
                int npcId;
                try {
                    npcId = Integer.parseInt(rawTarget);
                } catch (NumberFormatException e) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.ERROR,
                            "NPC id must be a number.");
                    return true;
                }
                net.citizensnpcs.api.npc.NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
                if (npc == null) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.ERROR,
                            "Citizens NPC not found for id " + npcId + ".");
                    return true;
                }
                questManager.setParticlePathDebugTarget(particlePlayer.getUniqueId(), npcId);
                ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Quest particle path debug set to NPC " + npcId + " (" + npc.getName() + ").");
                if (questManager.resolveCitizensNpcLocation(npcId) == null) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.WARNING,
                            "NPC " + npcId + " has no stored location; particles may not show until it spawns.");
                }
                return true;

            case "hand":
                if (!(sender instanceof Player p4)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                ItemStack held = p4.getInventory().getItemInMainHand();
                if (held == null || held.getType() == Material.AIR) {
                    p4.sendMessage("No item in hand.");
                    return true;
                }
                p4.sendMessage(ChatColor.YELLOW + "=== Hand Debug ===");
                p4.sendMessage(ChatColor.GRAY + "Type: " + held.getType());
                ItemMeta meta = held.getItemMeta();
                if (meta != null) {
                    if (meta.hasDisplayName()) {
                        p4.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.RESET + meta.getDisplayName());
                    }
                    if (meta.hasLore()) {
                        p4.sendMessage(ChatColor.GRAY + "Lore:");
                        for (String l : meta.getLore()) {
                            p4.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.RESET + l);
                        }
                    }
                    Key style = held.getData(DataComponentTypes.TOOLTIP_STYLE);
                    if (style != null) {
                        p4.sendMessage(ChatColor.GRAY + "Tooltip style: " + style.asString());
                    }
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();
                    if (!pdc.getKeys().isEmpty()) {
                        p4.sendMessage(ChatColor.GRAY + "PDC:" );
                        for (NamespacedKey k : pdc.getKeys()) {
                            String val = pdc.get(k, PersistentDataType.STRING);
                            if (val == null) {
                                Integer i = pdc.get(k, PersistentDataType.INTEGER);
                                if (i != null) val = i.toString();
                            }
                            if (val == null) {
                                Long l = pdc.get(k, PersistentDataType.LONG);
                                if (l != null) val = l.toString();
                            }
                            if (val == null) {
                                Double d = pdc.get(k, PersistentDataType.DOUBLE);
                                if (d != null) val = d.toString();
                            }
                            if (val == null) {
                                Byte b = pdc.get(k, PersistentDataType.BYTE);
                                if (b != null) val = b.toString();
                            }
                            if (val == null) {
                                Short s = pdc.get(k, PersistentDataType.SHORT);
                                if (s != null) val = s.toString();
                            }
                            p4.sendMessage(ChatColor.DARK_GRAY + "- " + k + " = " + val);
                        }
                    }
                }
                return true;

            case "chatgame":
                handleChatGameToggle(sender, args);
                return true;

            default:
                sender.sendMessage("Unknown debug subcommand: " + sub);
                String statUsage2 = Arrays.stream(StatType.values())
                        .map(StatType::getAbbrev)
                        .collect(Collectors.joining("|"));
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege|cityowner|citymax|autocast|chatgame|expedition|dungeonexpedition|beaconentity|spellinput|particle|particlepath|" + statUsage2 + ">");
                return true;
        }
    }

    private boolean hasTownOwnership(Player player) {
        String owner = GuildSiegeManager.getInstance().getOwnerGuild();
        if (owner == null) {
            return true;
        }

        Guild guild = GuildManager.getInstance().getGuild(player.getUniqueId());
        if (guild == null || !owner.equalsIgnoreCase(guild.getName())) {
            ChatFormatter.sendCenteredMessage(player, ChatColor.RED + "Your guild does not control this town.");
            return false;
        }

        return true;
    }

    private void handleChatGameToggle(CommandSender sender, String[] args) {
        if (chatGameManager == null) {
            sender.sendMessage(ChatColor.RED + "Chat games are not initialized.");
            return;
        }
        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list"))) {
            sender.sendMessage(ChatColor.YELLOW + "Chat games:");
            for (ChatGameStatus status : chatGameManager.getStatuses()) {
                ChatColor stateColor = status.enabled() ? ChatColor.GREEN : ChatColor.RED;
                String playable = status.playable() ? "" : ChatColor.DARK_RED + " (unavailable)";
                sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.AQUA + status.id()
                        + ChatColor.GRAY + " (" + status.displayName() + "): "
                        + stateColor + (status.enabled() ? "enabled" : "disabled") + playable);
            }
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /debug chatgame <id> <on|off>");
            return;
        }
        String id = args[1].toLowerCase();
        String toggle = args[2].toLowerCase();
        Boolean enable = switch (toggle) {
            case "on", "enable", "enabled" -> true;
            case "off", "disable", "disabled" -> false;
            default -> null;
        };
        if (enable == null) {
            sender.sendMessage(ChatColor.RED + "Specify 'on' or 'off'.");
            return;
        }
        boolean success = chatGameManager.setGameEnabled(id, enable);
        if (!success) {
            sender.sendMessage(ChatColor.RED + "Unknown chat game: " + id);
            return;
        }
        sender.sendMessage(ChatColor.GRAY + "Chat game '" + ChatColor.AQUA + id + ChatColor.GRAY + "' is now "
                + (enable ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GRAY + ".");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("mobinfo", "tps", "siege", "cityowner", "citymax", "autocast",
                    "hand", "chatgame", "expedition", "dungeonexpedition", "rewardbomb", "drops", "beaconentity",
                    "spellinput", "particle", "particlepath"));
            subs.addAll(Arrays.stream(StatType.values()).map(StatType::getAbbrev).toList());
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chatgame")) {
            List<String> options = new ArrayList<>();
            options.add("list");
            if (chatGameManager != null) {
                chatGameManager.getStatuses().forEach(status -> options.add(status.id()));
            }
            return options.stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("cityowner")) {
            List<String> guilds = GuildManager.getInstance().getGuilds().stream()
                    .map(Guild::getName)
                    .collect(Collectors.toCollection(ArrayList::new));
            return guilds.stream()
                    .filter(g -> g.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("particlepath")) {
            List<String> npcIds = new ArrayList<>();
            npcIds.add("off");
            for (net.citizensnpcs.api.npc.NPC npc : CitizensAPI.getNPCRegistry()) {
                npcIds.add(String.valueOf(npc.getId()));
            }
            String filter = args[1].toLowerCase();
            return npcIds.stream()
                    .filter(id -> id.toLowerCase().startsWith(filter))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("particle")) {
            List<String> options = new ArrayList<>();
            options.addAll(List.of("spawn", "ring", "arc"));
            options.addAll(ParticleService.getInstance().getPresetNames());
            return options.stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("particle")) {
            if (args[1].equalsIgnoreCase("spawn")
                    || args[1].equalsIgnoreCase("ring")
                    || args[1].equalsIgnoreCase("arc")) {
                return ParticleService.getInstance().getPresetNames().stream()
                        .filter(opt -> opt.toLowerCase().startsWith(args[2].toLowerCase()))
                        .toList();
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("chatgame")) {
            return List.of("on", "off", "enable", "disable").stream()
                    .filter(opt -> opt.startsWith(args[2].toLowerCase()))
                    .toList();
        } else if (args.length >= 4 && args[0].equalsIgnoreCase("particle")) {
            String current = args[args.length - 1].toLowerCase();
            List<String> optionHints = List.of("axis=x", "axis=y", "axis=z", "axis=look", "center=self", "center=look");
            return optionHints.stream()
                    .filter(opt -> opt.startsWith(current))
                    .toList();
        }
        return Collections.emptyList();
    }

    private double parsePositiveDouble(String[] args, int index, String error, Player player) {
        if (args.length <= index) {
            return -1;
        }
        try {
            double value = Double.parseDouble(args[index]);
            if (value <= 0) {
                throw new NumberFormatException("value must be positive");
            }
            return value;
        } catch (NumberFormatException e) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, error);
            return -1;
        }
    }

    private ParseIntResult parseOptionalInt(String[] args, int index, int fallback, String error, Player player) {
        if (args.length <= index) {
            return new ParseIntResult(fallback, index);
        }
        if (!isNumber(args[index])) {
            return new ParseIntResult(fallback, index);
        }
        try {
            int value = Integer.parseInt(args[index]);
            if (value <= 0) {
                throw new NumberFormatException("value must be positive");
            }
            return new ParseIntResult(value, index + 1);
        } catch (NumberFormatException e) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, error);
            return new ParseIntResult(-1, index);
        }
    }

    private ParticleOptions parseOptions(String[] args, int startIndex, ParticleAxis defaultAxis, CenterMode defaultCenter,
                                         Player player) {
        ParticleAxis axis = defaultAxis;
        CenterMode centerMode = defaultCenter;
        for (int i = startIndex; i < args.length; i++) {
            String token = args[i].toLowerCase(Locale.ROOT);
            if (token.startsWith("axis=")) {
                String rawAxis = token.substring("axis=".length());
                axis = ParticleAxis.fromToken(rawAxis).orElse(defaultAxis);
                if (axis == defaultAxis && !rawAxis.isBlank()) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "Unknown axis '" + rawAxis + "'. Using " + defaultAxis.name().toLowerCase(Locale.ROOT) + ".");
                }
            } else if (token.startsWith("center=")) {
                String rawCenter = token.substring("center=".length());
                centerMode = CenterMode.fromToken(rawCenter).orElse(defaultCenter);
                if (centerMode == defaultCenter && !rawCenter.isBlank()) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                            "Unknown center '" + rawCenter + "'. Using " + defaultCenter.name().toLowerCase(Locale.ROOT) + ".");
                }
            }
        }
        return new ParticleOptions(axis, centerMode);
    }

    private Location resolveCenter(Player player, CenterMode centerMode) {
        if (centerMode == CenterMode.LOOK) {
            Location target = TeleportUtils.resolveLineOfSightTarget(
                    player,
                    player.getEyeLocation().getDirection(),
                    12.0,
                    0.2);
            if (target != null) {
                return target;
            }
        }
        return player.getLocation().clone();
    }

    private boolean isNumber(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void runParticleShape(Player player, int ticks, Runnable action) {
        if (ticks <= 0) {
            action.run();
            return;
        }
        new BukkitRunnable() {
            int remaining = ticks;

            @Override
            public void run() {
                if (!player.isOnline() || remaining <= 0) {
                    cancel();
                    return;
                }
                action.run();
                remaining--;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private enum CenterMode {
        SELF,
        LOOK;

        private static Optional<CenterMode> fromToken(String raw) {
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "self", "player" -> Optional.of(SELF);
                case "look", "target" -> Optional.of(LOOK);
                default -> Optional.empty();
            };
        }
    }

    private record ParticleOptions(ParticleAxis axis, CenterMode centerMode) {
    }

    private record ParseIntResult(int value, int nextIndex) {
    }

}
