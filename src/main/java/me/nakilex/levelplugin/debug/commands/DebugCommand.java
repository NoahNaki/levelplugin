package me.nakilex.levelplugin.debug.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import me.nakilex.levelplugin.debug.gui.DebugGUI;
import me.nakilex.levelplugin.debug.AutoCastManager;
import me.nakilex.levelplugin.duels.managers.DuelManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.Bukkit;
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
    private final AutoCastManager autoCastManager = new AutoCastManager();

    public DebugCommand(PlayerToggleManager mobDebugManager,
                        PlayerScoreboardManager scoreboardManager,
                        DebugGUI debugGUI) {
        this.mobDebugManager = mobDebugManager;
        this.scoreboardManager = scoreboardManager;
        this.debugGUI = debugGUI;
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
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege|autocast|" + statUsage + ">");
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

            case "autocast":
                if (!(sender instanceof Player p3)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(p3.getUniqueId());
                if (ps.playerClass != PlayerClass.MAGE) {
                    p3.sendMessage("Mage class required for autocast debug.");
                    return true;
                }
                boolean auto = autoCastManager.toggle(p3, "fireball");
                ToggleFeedbackUtil.sendToggle(p3, "Mage autocast", auto);
                return true;

            case "candamage":
                if (args.length < 2) {
                    sender.sendMessage("Usage: /debug candamage <target> [attacker]");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found: " + args[1]);
                    return true;
                }
                Player attacker;
                if (args.length >= 3) {
                    attacker = Bukkit.getPlayer(args[2]);
                    if (attacker == null) {
                        sender.sendMessage("Player not found: " + args[2]);
                        return true;
                    }
                } else if (sender instanceof Player pSelf) {
                    attacker = pSelf;
                } else {
                    sender.sendMessage("Console must specify attacker: /debug candamage <target> <attacker>");
                    return true;
                }
                boolean can = DuelManager.getInstance().canDamage(attacker.getUniqueId(), target.getUniqueId());
                sender.sendMessage(attacker.getName() + (can ? " can " : " cannot ") + "damage " + target.getName());
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

            default:
                sender.sendMessage("Unknown debug subcommand: " + sub);
                String statUsage2 = Arrays.stream(StatType.values())
                        .map(StatType::getAbbrev)
                        .collect(Collectors.joining("|"));
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege|autocast|" + statUsage2 + ">");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("mobinfo", "tps", "siege", "autocast", "hand", "candamage"));
            subs.addAll(Arrays.stream(StatType.values()).map(StatType::getAbbrev).toList());
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("candamage")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("candamage") && !(sender instanceof Player)) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
