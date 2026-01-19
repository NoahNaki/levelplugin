package me.nakilex.levelplugin.debug.commands;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import me.nakilex.levelplugin.guild.GuildManager;
import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.particle.type.ParticleType;
import org.bukkit.ChatColor;
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
                            "Usage: /debug particle <type> [count]");
                    return true;
                }
                ParticleNativeAPI api = ParticleService.getInstance().getApi();
                ParticleType type = resolveParticleType(api, args[1]);
                if (type == null) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.ERROR,
                            "Unknown particle type: " + args[1] + ".");
                    return true;
                }
                if (!type.isPresent()) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.ERROR,
                            "Particle type " + args[1] + " is not supported on this server.");
                    return true;
                }
                int count = 8;
                if (args.length >= 3) {
                    try {
                        count = Integer.parseInt(args[2]);
                        if (count <= 0) {
                            throw new NumberFormatException("count must be positive");
                        }
                    } catch (NumberFormatException e) {
                        ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.ERROR,
                                "Particle count must be a positive number.");
                        return true;
                    }
                }
                try {
                    type.packet(false, particlePlayer.getLocation().add(0, 1.0, 0), count)
                            .sendTo(particlePlayer);
                } catch (Exception e) {
                    ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.ERROR,
                            "Failed to spawn particle: " + e.getMessage());
                    return true;
                }
                ChatMessageUtil.send(particlePlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Spawned " + count + " " + args[1].toUpperCase(Locale.ROOT) + " particles.");
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
            return Collections.emptyList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("chatgame")) {
            return List.of("on", "off", "enable", "disable").stream()
                    .filter(opt -> opt.startsWith(args[2].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }

    private ParticleType resolveParticleType(ParticleNativeAPI api, String rawName) {
        String fieldName = rawName.toUpperCase(Locale.ROOT);
        ParticleType type = resolveParticleTypeFromList(api.LIST_1_13, fieldName);
        if (type != null) {
            return type;
        }
        type = resolveParticleTypeFromList(api.LIST_1_8, fieldName);
        if (type != null) {
            return type;
        }
        return resolveParticleTypeFromList(api.LIST_1_19_PART, fieldName);
    }

    private ParticleType resolveParticleTypeFromList(Object list, String fieldName) {
        try {
            Field field = list.getClass().getField(fieldName);
            Object value = field.get(list);
            if (value instanceof ParticleType particleType) {
                return particleType;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Particle not found in this list.
        }
        return null;
    }

}
