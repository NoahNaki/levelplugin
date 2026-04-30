package me.nakilex.levelplugin.debug.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.chat.games.ChatGameStatus;
import me.nakilex.levelplugin.debug.gui.DebugGUI;
import me.nakilex.levelplugin.debug.BeaconEntityDebugManager;
import me.nakilex.levelplugin.debug.DropDebugManager;
import me.nakilex.levelplugin.debug.ArcSlashDebugManager;
import me.nakilex.levelplugin.debug.gui.ArcSlashDebugGUI;
import me.nakilex.levelplugin.debug.gui.StrongholdAssetDebugGUI;
import me.nakilex.levelplugin.debug.gui.StrongholdTemplateDebugGUI;
import me.nakilex.levelplugin.debug.gui.StrongholdScalingDebugGUI;
import me.nakilex.levelplugin.debug.gui.WarriorCycloneDebugGUI;
import me.nakilex.levelplugin.debug.MobStatusDebugItem;
import me.nakilex.levelplugin.debug.SpellInputDebugItem;
import me.nakilex.levelplugin.debug.StrongholdDebugGenerator;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.PetManager.PetPullResult;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import me.nakilex.levelplugin.pet.utils.PetPullSummaryUtil;
import me.nakilex.levelplugin.mob.custom.CustomMobStatus;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.guild.Guild;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.pathfinding.DungeonExpeditionManager;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticleService;
import me.nakilex.levelplugin.particles.presets.ElementalPresets;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.spells.SpellCastManager;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.RewardBombUtil;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.guild.siege.GuildSiegeManager;
import me.nakilex.levelplugin.guild.GuildManager;
import me.nakilex.levelplugin.items.listeners.StaticItemListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
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
    private static final Set<UUID> INVENTORY_DEBUG_ENABLED = ConcurrentHashMap.newKeySet();
    private static final List<String> LOOT_CHEST_ANIMATION_OPTIONS = List.of("idle", "idle_mouve", "idle_move", "opening", "opening_rare", "closing");
    private final Map<UUID, UUID> lootChestAnimationPreviewEntities = new ConcurrentHashMap<>();

    public static boolean isInventoryDebugEnabled(UUID playerId) {
        return playerId != null && INVENTORY_DEBUG_ENABLED.contains(playerId);
    }
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
    private final ArcSlashDebugManager arcSlashDebugManager;
    private final ArcSlashDebugGUI arcSlashDebugGUI;
    private final PetManager petManager;

    public DebugCommand(PlayerToggleManager mobDebugManager,
                        PlayerScoreboardManager scoreboardManager,
                        DebugGUI debugGUI,
                        ChatGameManager chatGameManager,
                        MercenaryExpeditionManager expeditionManager,
                        DungeonExpeditionManager dungeonExpeditionManager,
                        DropDebugManager dropDebugManager,
                        EnvironmentManager environmentManager,
                        BeaconEntityDebugManager beaconEntityDebugManager,
                        QuestManager questManager,
                        ArcSlashDebugManager arcSlashDebugManager,
                        ArcSlashDebugGUI arcSlashDebugGUI,
                        PetManager petManager) {
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
        this.arcSlashDebugManager = arcSlashDebugManager;
        this.arcSlashDebugGUI = arcSlashDebugGUI;
        this.petManager = petManager;
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
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege|drops|cityowner|citymax|chatgame|expedition|dungeonexpedition|beaconentity|spellinput|spellcooldown|spellmanacost|stunstick|poisonstick|tauntstick|fearstick|slowstick|particle|particlepath|particlepreset|petpull|inventorydebug|rewardbomb|warriorcyclone|stronghold|strongholdxp|lootchestanimation|" + statUsage + ">");
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
                sender.sendMessage("Fast siege mode " + (fast ? "enabled" : "disabled")
                        + " (quick timer + boosted capture)");
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
            case "spellcooldown":
                SpellCastManager.setCooldownsEnabled(!SpellCastManager.areCooldownsEnabled());
                sender.sendMessage(ChatColor.YELLOW + "Spell cooldowns are now "
                        + (SpellCastManager.areCooldownsEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled")
                        + ChatColor.YELLOW + ".");
                return true;
            case "spellmanacost":
                SpellCastManager.setManaCostsEnabled(!SpellCastManager.areManaCostsEnabled());
                sender.sendMessage(ChatColor.YELLOW + "Spell mana costs are now "
                        + (SpellCastManager.areManaCostsEnabled() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled")
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

            case "petpull":
                if (!(sender instanceof Player petPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                if (petManager == null) {
                    PetChatUtil.send(petPlayer, "Pet system is not available.");
                    return true;
                }
                if (args.length < 2) {
                    PetChatUtil.send(petPlayer, "Usage: /debug petpull <amount>");
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    PetChatUtil.send(petPlayer, "Amount must be a number.");
                    return true;
                }
                if (amount <= 0) {
                    PetChatUtil.send(petPlayer, "Amount must be at least 1.");
                    return true;
                }
                PetPullResult pullResult = petManager.pullPets(petPlayer, amount);
                if (pullResult.kept().isEmpty() && pullResult.discarded().isEmpty()) {
                    PetChatUtil.send(petPlayer, "No pets available to pull.");
                    return true;
                }
                PetChatUtil.send(petPlayer, ChatColor.YELLOW + "Pet pulls:");
                PetPullSummaryUtil.sendSummary(petPlayer, "Pulled", pullResult.kept());
                PetPullSummaryUtil.sendSummary(petPlayer, "Auto-discarded", pullResult.discarded());
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
            case "warriorcyclone":
                if (!(sender instanceof Player cyclonePlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                WarriorCycloneDebugGUI.getInstance().open(cyclonePlayer);
                return true;
            case "stunstick":
                if (!(sender instanceof Player stunPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                stunPlayer.getInventory().addItem(MobStatusDebugItem.create(CustomMobStatus.STUNNED));
                ChatMessageUtil.send(stunPlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Stun stick added to your inventory.");
                return true;
            case "poisonstick":
                if (!(sender instanceof Player poisonPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                poisonPlayer.getInventory().addItem(MobStatusDebugItem.create(CustomMobStatus.POISONED));
                ChatMessageUtil.send(poisonPlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Poison stick added to your inventory.");
                return true;
            case "tauntstick":
                if (!(sender instanceof Player tauntPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                tauntPlayer.getInventory().addItem(MobStatusDebugItem.create(CustomMobStatus.TAUNTED));
                ChatMessageUtil.send(tauntPlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Taunt stick added to your inventory.");
                return true;
            case "fearstick":
                if (!(sender instanceof Player fearPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                fearPlayer.getInventory().addItem(MobStatusDebugItem.create(CustomMobStatus.FEARED));
                ChatMessageUtil.send(fearPlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Fear stick added to your inventory.");
                return true;
            case "slowstick":
                if (!(sender instanceof Player slowPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                slowPlayer.getInventory().addItem(MobStatusDebugItem.create(CustomMobStatus.SLOWED));
                ChatMessageUtil.send(slowPlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Slow stick added to your inventory.");
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

            case "particle":
                if (!(sender instanceof Player particleGuiPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                if (args.length < 2) {
                    ChatMessageUtil.send(particleGuiPlayer, ChatMessageUtil.MessageType.WARNING,
                            "Usage: /debug particle <arc>");
                    return true;
                }
                if (!args[1].equalsIgnoreCase(ArcSlashDebugManager.getArcPresetId())) {
                    ChatMessageUtil.send(particleGuiPlayer, ChatMessageUtil.MessageType.ERROR,
                            "Unknown particle GUI. Available: " + ArcSlashDebugManager.getArcPresetId());
                    return true;
                }
                arcSlashDebugGUI.open(particleGuiPlayer);
                return true;

            case "particlepreset":
                if (!(sender instanceof Player presetPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                if (args.length < 2) {
                    ChatMessageUtil.send(presetPlayer, ChatMessageUtil.MessageType.WARNING,
                            "Usage: /debug particlepreset <"
                                    + String.join("|", ArcSlashDebugManager.getPresetIds())
                                    + "|" + String.join("|", ElementalPresets.getPresetNames()) + ">");
                    return true;
                }
                if (args[1].equalsIgnoreCase(ArcSlashDebugManager.getArcPresetId())) {
                    arcSlashDebugManager.toggle(presetPlayer);
                    return true;
                }
                ParticlePreset preset = ElementalPresets.getPreset(args[1]);
                if (preset == null) {
                    ChatMessageUtil.send(presetPlayer, ChatMessageUtil.MessageType.ERROR,
                            "Unknown preset. Available: "
                                    + String.join(", ", ArcSlashDebugManager.getPresetIds())
                                    + ", " + String.join(", ", ElementalPresets.getPresetNames()));
                    return true;
                }
                new ParticleService(Main.getInstance()).renderPreset(presetPlayer, preset);
                ChatMessageUtil.send(presetPlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Rendered particle preset " + preset.name() + ".");
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

            case "inventorydebug":
                if (!(sender instanceof Player inventoryDebugPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                toggleInventoryDebug(inventoryDebugPlayer);
                return true;

            case "stronghold":
                if (!(sender instanceof Player strongholdPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                if (args.length < 2) {
                    ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.WARNING,
                            "Usage: /debug stronghold <generate test|towerwall|overlap [percent]|templates|assets|scaling|output|spells|endsession <player>|waveskip <wave>>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("spells")) {
                    var runManager = Main.getInstance().getStrongholdRunManager();
                    if (runManager == null) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.ERROR,
                                "Stronghold run manager unavailable.");
                        return true;
                    }
                    List<String> possibilities = runManager.getSpellPossibilities(strongholdPlayer);
                    if (possibilities.isEmpty()) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.WARNING,
                                "No active Stronghold run spell possibilities available.");
                        return true;
                    }
                    ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.INFO,
                            "Current Stronghold spell possibilities:");
                    for (String line : possibilities) {
                        strongholdPlayer.sendMessage(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + line);
                    }
                    return true;
                }
                if (args[1].equalsIgnoreCase("endsession")) {
                    if (args.length < 3) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.WARNING,
                                "Usage: /debug stronghold endsession <player>");
                        return true;
                    }
                    Player targetPlayer = Bukkit.getPlayerExact(args[2]);
                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.ERROR,
                                "Player not found or offline: " + args[2]);
                        return true;
                    }
                    var runManager = Main.getInstance().getStrongholdRunManager();
                    if (runManager == null || !runManager.forceEndSession(targetPlayer)) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.ERROR,
                                "Target is not in an active Stronghold run.");
                        return true;
                    }
                    ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.SUCCESS,
                            "Ended Stronghold session for " + ChatColor.WHITE + targetPlayer.getName() + ChatColor.GREEN + ".");
                    return true;
                }
                if (args[1].equalsIgnoreCase("waveskip")) {
                    if (args.length < 3) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.WARNING,
                                "Usage: /debug stronghold waveskip <desired wave>");
                        return true;
                    }
                    int desiredWave;
                    try {
                        desiredWave = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ex) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.ERROR,
                                "Invalid wave. Example: /debug stronghold waveskip 30");
                        return true;
                    }
                    var runManager = Main.getInstance().getStrongholdRunManager();
                    if (runManager == null || !runManager.forceWaveSkip(strongholdPlayer, desiredWave)) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.ERROR,
                                "You must be in an active Stronghold run to waveskip.");
                        return true;
                    }
                    ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.SUCCESS,
                            "Stronghold wave set to " + ChatColor.WHITE + desiredWave + ChatColor.GREEN + ".");
                    return true;
                }
                if (args[1].equalsIgnoreCase("assets")) {
                    StrongholdAssetDebugGUI.getInstance().open(strongholdPlayer);
                    return true;
                }
                if (args[1].equalsIgnoreCase("templates")) {
                    StrongholdTemplateDebugGUI.getInstance().open(strongholdPlayer);
                    return true;
                }
                if (args[1].equalsIgnoreCase("scaling")) {
                    StrongholdScalingDebugGUI.getInstance().open(strongholdPlayer);
                    return true;
                }
                if (args[1].equalsIgnoreCase("output")) {
                    boolean outputEnabled = StrongholdDebugGenerator.toggleStrongholdConsoleOutput();
                    ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.SUCCESS,
                            "Stronghold console debug output is now " + (outputEnabled ? "ON" : "OFF") + ".");
                    return true;
                }
                if (args[1].equalsIgnoreCase("overlap")) {
                    if (args.length == 2) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.INFO,
                                "Stronghold overlap threshold: "
                                        + String.format("%.2f", StrongholdDebugGenerator.getMaxOverlapPercent()) + "%");
                        return true;
                    }
                    try {
                        double value = Double.parseDouble(args[2]);
                        StrongholdDebugGenerator.setMaxOverlapPercent(value);
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.SUCCESS,
                                "Set stronghold overlap threshold to "
                                        + String.format("%.2f", StrongholdDebugGenerator.getMaxOverlapPercent()) + "%.");
                    } catch (NumberFormatException ex) {
                        ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.ERROR,
                                "Invalid percent. Example: /debug stronghold overlap 2.5");
                    }
                    return true;
                }
                if (args.length < 3 || !args[1].equalsIgnoreCase("generate")) {
                    ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.WARNING,
                            "Usage: /debug stronghold <generate test|towerwall|overlap [percent]|templates|assets|scaling|output|spells|endsession <player>|waveskip <wave>>");
                    return true;
                }
                if (args[2].equalsIgnoreCase("test")) {
                    return StrongholdDebugGenerator.generateTest(strongholdPlayer);
                }
                if (args[2].equalsIgnoreCase("towerwall")) {
                    return StrongholdDebugGenerator.generateTowerWall(strongholdPlayer);
                }
                ChatMessageUtil.send(strongholdPlayer, ChatMessageUtil.MessageType.ERROR,
                        "Unknown stronghold template: " + args[2] + ". Available: test, towerwall");
                return true;
            case "strongholdxp":
                if (!(sender instanceof Player strongholdXpPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                if (args.length < 2) {
                    ChatMessageUtil.send(strongholdXpPlayer, ChatMessageUtil.MessageType.WARNING,
                            "Usage: /debug strongholdxp <amount>");
                    return true;
                }
                int xpAmount;
                try {
                    xpAmount = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    ChatMessageUtil.send(strongholdXpPlayer, ChatMessageUtil.MessageType.ERROR,
                            "Invalid amount. Example: /debug strongholdxp 150");
                    return true;
                }
                if (xpAmount <= 0) {
                    ChatMessageUtil.send(strongholdXpPlayer, ChatMessageUtil.MessageType.ERROR,
                            "XP amount must be greater than 0.");
                    return true;
                }
                var runManager = Main.getInstance().getStrongholdRunManager();
                if (runManager == null || !runManager.addDebugXp(strongholdXpPlayer, xpAmount)) {
                    ChatMessageUtil.send(strongholdXpPlayer, ChatMessageUtil.MessageType.ERROR,
                            "You must be inside an active Stronghold run to add Stronghold XP.");
                    return true;
                }
                ChatMessageUtil.send(strongholdXpPlayer, ChatMessageUtil.MessageType.SUCCESS,
                        "Added " + ChatColor.WHITE + xpAmount + ChatColor.GREEN + " Stronghold XP.");
                return true;
            case "lootchestanimation":
                if (!(sender instanceof Player lootChestAnimationPlayer)) {
                    sender.sendMessage(ChatColor.RED + "Players only.");
                    return true;
                }
                if (args.length < 2) {
                    ChatMessageUtil.send(lootChestAnimationPlayer, ChatMessageUtil.MessageType.WARNING,
                            "Usage: /debug lootchestanimation <" + String.join("|", LOOT_CHEST_ANIMATION_OPTIONS) + ">");
                    return true;
                }
                return spawnLootChestAnimationPreview(lootChestAnimationPlayer, args[1]);

            default:
                sender.sendMessage("Unknown debug subcommand: " + sub);
                String statUsage2 = Arrays.stream(StatType.values())
                        .map(StatType::getAbbrev)
                        .collect(Collectors.joining("|"));
                sender.sendMessage("Usage: /debug <mobinfo|tps|siege|drops|cityowner|citymax|chatgame|expedition|dungeonexpedition|beaconentity|spellinput|spellcooldown|spellmanacost|stunstick|poisonstick|tauntstick|fearstick|slowstick|particle|particlepath|particlepreset|petpull|inventorydebug|rewardbomb|warriorcyclone|stronghold|strongholdxp|lootchestanimation|" + statUsage2 + ">");
                return true;
        }
    }

    private boolean spawnLootChestAnimationPreview(Player player, String animationName) {
        if (animationName == null || animationName.isBlank()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Animation name is required.");
            return true;
        }
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "ModelEngine is not enabled on this server.");
            return true;
        }
        LootChestManager lootChestManager = Main.getInstance().getLootChestManager();
        if (lootChestManager == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Loot chest manager is not available.");
            return true;
        }

        UUID existingId = lootChestAnimationPreviewEntities.remove(player.getUniqueId());
        if (existingId != null) {
            Entity existing = org.bukkit.Bukkit.getEntity(existingId);
            if (existing != null && existing.isValid()) {
                existing.remove();
            }
        }

        org.bukkit.Location spawnLoc = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(2.0));
        spawnLoc.setY(player.getLocation().getY() + 0.1);
        ArmorStand preview = player.getWorld().spawn(spawnLoc, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(false);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setInvulnerable(true);
            stand.addScoreboardTag("debug_lootchest_animation_preview");
        });

        ModelEngineUtil.ModelApplyResult modelResult = ModelEngineUtil.applyFirstAvailableModel(
                preview,
                ModelEngineUtil.buildModelCandidates(lootChestManager.getCrateModelId()),
                Main.getInstance()
        );
        if (modelResult.applied().isEmpty()) {
            preview.remove();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Could not apply loot chest model. Make sure the crate model is loaded.");
            return true;
        }

        List<String> keywords = resolveLootChestAnimationKeywords(animationName);
        boolean loop = animationName.toLowerCase().contains("idle");
        boolean played = ModelEngineUtil.playBestAnimation(preview, keywords, loop, false);
        if (!played) {
            played = ModelEngineUtil.playAnimationByName(preview, animationName, loop);
        }
        if (!played) {
            List<String> runtime = ModelEngineUtil.getRuntimeAnimationNames(preview);
            preview.remove();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Could not play animation '" + animationName + "' via keywords " + keywords
                            + ". Runtime animations: "
                            + (runtime.isEmpty() ? "(none)" : String.join(", ", runtime)));
            return true;
        }

        lootChestAnimationPreviewEntities.put(player.getUniqueId(), preview.getUniqueId());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Spawned loot chest animation preview with '" + animationName + "'.");
        return true;
    }

    private List<String> resolveLootChestAnimationKeywords(String animationName) {
        if (animationName == null || animationName.isBlank()) {
            return List.of("idle");
        }
        String normalized = animationName.trim().toLowerCase();
        return switch (normalized) {
            case "opening_rare" -> List.of("opening_rare", "opening", "open", "rare");
            case "opening" -> List.of("opening", "open");
            case "closing" -> List.of("closing", "close");
            case "idle_mouve", "idle_move" -> List.of("idle_mouve", "idle_move", "idle", "move");
            case "idle" -> List.of("idle");
            default -> List.of(normalized);
        };
    }

    private void toggleInventoryDebug(Player player) {
        if (INVENTORY_DEBUG_ENABLED.remove(player.getUniqueId())) {
            clearInventoryDebugSession(player);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Inventory debug disabled. Cleared crafting slots (0-4).");
            return;
        }
        INVENTORY_DEBUG_ENABLED.add(player.getUniqueId());
        applyInventoryDebugSession(player);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Inventory debug enabled. Filled crafting slots (0-4) with GUI shortcuts.");
    }

    private void applyInventoryDebugSession(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (player.getOpenInventory().getTopInventory() instanceof org.bukkit.inventory.CraftingInventory craftingInventory) {
            StaticItemListener.applyCraftingShortcutItems(player, craftingInventory);
        }
        player.updateInventory();
    }

    private void clearInventoryDebugSession(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (player.getOpenInventory().getTopInventory() instanceof org.bukkit.inventory.CraftingInventory craftingInventory) {
            StaticItemListener.clearCraftingShortcutItems(craftingInventory);
        }
        player.updateInventory();
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
                    "spellinput", "spellcooldown", "spellmanacost", "stunstick", "poisonstick", "tauntstick", "fearstick", "slowstick", "petpull",
                    "particle", "particlepath", "particlepreset", "inventorydebug", "warriorcyclone", "stronghold", "strongholdxp", "lootchestanimation"));
            subs.addAll(Arrays.stream(StatType.values()).map(StatType::getAbbrev).toList());
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("strongholdxp")) {
            return List.of("50", "100", "150", "250").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("stronghold")) {
            return List.of("generate", "overlap", "templates", "assets", "scaling", "output", "spells", "endsession", "waveskip").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stronghold") && args[1].equalsIgnoreCase("waveskip")) {
            return List.of("1", "15", "30", "45", "60").stream()
                    .filter(opt -> opt.startsWith(args[2].toLowerCase()))
                    .toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stronghold") && args[1].equalsIgnoreCase("endsession")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stronghold") && args[1].equalsIgnoreCase("generate")) {
            return List.of("test", "towerwall").stream()
                    .filter(opt -> opt.startsWith(args[2].toLowerCase()))
                    .toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("stronghold") && args[1].equalsIgnoreCase("overlap")) {
            return List.of("0.0", "1.0", "2.5", "5.0").stream()
                    .filter(opt -> opt.startsWith(args[2].toLowerCase()))
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
        } else if (args.length == 2 && args[0].equalsIgnoreCase("particlepreset")) {
            String filter = args[1].toLowerCase();
            List<String> options = new ArrayList<>(ElementalPresets.getPresetNames());
            options.addAll(ArcSlashDebugManager.getPresetIds());
            return options.stream()
                    .filter(name -> name.toLowerCase().startsWith(filter))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("particle")) {
            return ArcSlashDebugManager.getPresetIds().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("chatgame")) {
            return List.of("on", "off", "enable", "disable").stream()
                    .filter(opt -> opt.startsWith(args[2].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("petpull")) {
            return List.of("1", "5", "10").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("lootchestanimation")) {
            return LOOT_CHEST_ANIMATION_OPTIONS.stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }

}
