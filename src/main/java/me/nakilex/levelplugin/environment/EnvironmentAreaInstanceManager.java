package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.animatedlb.AnimatedLeaderboard;
import me.nakilex.levelplugin.animatedlb.LeaderboardDataProvider;
import me.nakilex.levelplugin.animatedlb.MockLeaderboardDataProvider;
import me.nakilex.levelplugin.animatedlb.PlayerStatsLeaderboardDataProvider;
import me.nakilex.levelplugin.dungeon.VoidWorldGenerator;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CuboidTemplate;
import me.nakilex.levelplugin.utils.FireworkUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.advancement.AdvancementToastUtil;
import me.nakilex.levelplugin.advancement.model.AdvancementDisplay;
import me.nakilex.levelplugin.advancement.model.AdvancementKey;
import me.nakilex.levelplugin.advancement.model.BaseAdvancement;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debug/test harness for the new environment-area flow. It captures configured
 * cuboid templates directly from the source world and pastes them into a fresh
 * instanced flat world, mirroring the stronghold/dungeon template workflow.
 */
public final class EnvironmentAreaInstanceManager implements Listener {
    private static EnvironmentAreaInstanceManager instance;

    private static final String SOURCE_WORLD = "flatland";
    private static final int PASTE_X = 0;
    private static final int PASTE_Y = -40;
    private static final int PASTE_Z = 0;
    private static final int AREA_SPACING_BLOCKS = 1500;
    /**
     * Runtime movement border should be anchored to the pasted instance area
     * itself (not absolute source-world coordinates), so every player gets a
     * valid per-session cuboid regardless of world/offset.
     */
    private static final int BORDER_MIN_Y_OFFSET = -108; // matches provided -44 relative to paste Y=-40
    private static final String HOLOGRAM_TAG_PREFIX = "environment_area_build:";
    private static final int BUILD_COST_COINS = 100;
    private static final long PAYMENT_ANIMATION_TICKS = 28L;
    private static final int BUILD_ANIMATION_TOTAL_TICKS = 40;
    private static final int MIN_BUILD_SPEED_PERCENT = 1;
    private static final int MAX_BUILD_SPEED_PERCENT = 100;
    private static int buildSpeedPercent = 100;
    private static final long COIN_SEND_INTERVAL_TICKS = 2L;
    private static final List<CoinVisual> PAYMENT_COIN_VISUALS = List.of(
            new CoinVisual(100, Material.GOLD_NUGGET, "gold_coin"),
            new CoinVisual(10, Material.IRON_NUGGET, "iron_coin"),
            new CoinVisual(1, Material.COPPER_INGOT, "copper_coin")
    );

    private static final Cuboid AREA = new Cuboid(4058, -44, -3603, 3489, 330, -3145);
    private static final Cuboid KINGDOM_MINE_AREA = new Cuboid(3885, 81, -3502, 3774, -24, -3366);
    private static final Cuboid FINISHED_WORLD_AREA = new Cuboid(4058, -44, -2685, 3489, 330, -3143);
    private static final WorldPoint FINISHED_WORLD_ANCHOR = new WorldPoint(3489, 77, -3143);
    private static final WorldPoint EMPTY_WORLD_ANCHOR = new WorldPoint(3489, 77, -3603);
    private static final WorldPoint FINISHED_WORLD_SPAWN = new WorldPoint(3840, 108, -2934);
    private static final WorldPoint EMPTY_WORLD_SPAWN = projectFinishedToEmpty(FINISHED_WORLD_SPAWN);
    private static final WorldPoint KINGDOM_ANIMATED_LB = new WorldPoint(3810, 105, -3377);

    private static final List<BuildingTemplate> BUILDINGS = List.of(
            new BuildingTemplate(1, "bar", "Bar", Material.BRICKS,
                    new Cuboid(3821, 95, -2852, 3780, 160, -2805),
                    projectFinishedToEmpty(new Cuboid(3821, 95, -2852, 3780, 160, -2805)),
                    projectFinishedToEmpty(new WorldPoint(3799, 100, -2851))),
            new BuildingTemplate(2, "blacksmith", "Blacksmith", Material.ANVIL,
                    new Cuboid(3875, 80, -2976, 3922, 151, -3035),
                    projectFinishedToEmpty(new Cuboid(3875, 80, -2976, 3922, 151, -3035)),
                    projectFinishedToEmpty(new WorldPoint(3883, 90, -2982))),
            new BuildingTemplate(3, "fishing", "Fishing", Material.WATER_BUCKET,
                    new Cuboid(3860, 85, -2807, 3921, 161, -2880),
                    projectFinishedToEmpty(new Cuboid(3860, 85, -2807, 3921, 161, -2880)),
                    projectFinishedToEmpty(new WorldPoint(3877, 92, -2836))),
            new BuildingTemplate(4, "palace", "Palace", Material.STONE_BRICKS,
                    new Cuboid(3717, 113, -2849, 3583, 313, -3027),
                    projectFinishedToEmpty(new Cuboid(3717, 113, -2849, 3583, 313, -3027)),
                    projectFinishedToEmpty(new WorldPoint(3693, 125, -2934))),
            new BuildingTemplate(5, "farm", "Farm", Material.HAY_BLOCK,
                    new Cuboid(3796, 82, -3059, 3753, 179, -2996),
                    projectFinishedToEmpty(new Cuboid(3796, 82, -3059, 3753, 179, -2996)),
                    projectFinishedToEmpty(new WorldPoint(3795, 97, -3032))),
            new BuildingTemplate(6, "house", "House", Material.OAK_PLANKS,
                    new Cuboid(3810, 89, -3003, 3838, 131, -3035),
                    projectFinishedToEmpty(new Cuboid(3810, 89, -3003, 3838, 131, -3035)),
                    projectFinishedToEmpty(new WorldPoint(3822, 97, -3004))),
            new BuildingTemplate(7, "house_2", "House 2", Material.SPRUCE_PLANKS,
                    new Cuboid(3856, 90, -2961, 3819, 129, -3000),
                    projectFinishedToEmpty(new Cuboid(3856, 90, -2961, 3819, 129, -3000)),
                    projectFinishedToEmpty(new WorldPoint(3825, 98, -2986))),
            new BuildingTemplate(8, "house_3", "House 3", Material.BIRCH_PLANKS,
                    new Cuboid(3755, 92, -2842, 3717, 167, -2884),
                    projectFinishedToEmpty(new Cuboid(3755, 92, -2842, 3717, 167, -2884)),
                    projectFinishedToEmpty(new WorldPoint(3757, 102, -2875))),
            new BuildingTemplate(9, "house_4", "House 4", Material.JUNGLE_PLANKS,
                    new Cuboid(3782, 98, -2862, 3812, 133, -2900),
                    projectFinishedToEmpty(new Cuboid(3782, 98, -2862, 3812, 133, -2900)),
                    projectFinishedToEmpty(new WorldPoint(3799, 101, -2897))),
            new BuildingTemplate(10, "house_5", "House 5", Material.ACACIA_PLANKS,
                    new Cuboid(3813, 131, -2909, 3849, 97, -2872),
                    projectFinishedToEmpty(new Cuboid(3813, 131, -2909, 3849, 97, -2872)),
                    projectFinishedToEmpty(new WorldPoint(3832, 99, -2887))),
            new BuildingTemplate(11, "house_6", "House 6", Material.DARK_OAK_PLANKS,
                    new Cuboid(3790, 121, -2797, 3745, 59, -2757),
                    projectFinishedToEmpty(new Cuboid(3790, 121, -2797, 3745, 59, -2757)),
                    projectFinishedToEmpty(new WorldPoint(3759, 92, -2817))),
            new BuildingTemplate(12, "house_7", "House 7", Material.MANGROVE_PLANKS,
                    new Cuboid(3765, 120, -2841, 3729, 84, -2812),
                    projectFinishedToEmpty(new Cuboid(3765, 120, -2841, 3729, 84, -2812)),
                    projectFinishedToEmpty(new WorldPoint(3753, 98, -2842))),
            new BuildingTemplate(13, "house_8", "House 8", Material.CHERRY_PLANKS,
                    new Cuboid(3742, 87, -2996, 3775, 138, -2937),
                    projectFinishedToEmpty(new Cuboid(3742, 87, -2996, 3775, 138, -2937)),
                    projectFinishedToEmpty(new WorldPoint(3780, 100, -2975)))
    );

    private static final Map<Integer, BuildingTemplate> BUILDINGS_BY_SLOT = BUILDINGS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(BuildingTemplate::slot, building -> building));

    private final Main plugin;
    private final Map<UUID, EnvironmentAreaSession> sessions = new HashMap<>();
    private final Map<UUID, BukkitTask> activeBuildTasks = new HashMap<>();
    private final Map<UUID, Map<Integer, Long>> buildFinishAtByProfile = new HashMap<>();
    private BukkitTask buildTimerTask;
    private BukkitTask hologramRefreshTask;
    private final Map<String, List<String>> lastHologramLinesByTag = new HashMap<>();
    private final Map<UUID, AnimatedLeaderboard> animatedLeaderboardsByOwner = new HashMap<>();
    private final Map<String, CuboidTemplate> templateCache = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.Set<Integer>> builtSlotsByProfile = new HashMap<>();
    private final Map<UUID, Integer> farmBuildingLevelByProfile = new HashMap<>();
    private final Map<UUID, Integer> palaceBuildingLevelByProfile = new HashMap<>();
    private final Map<UUID, Integer> blacksmithBuildingLevelByProfile = new HashMap<>();
    private final Map<UUID, Location> lastValidLocations = new HashMap<>();
    private final Map<UUID, UUID> pendingCoopInvites = new HashMap<>(); // invitee -> owner
    private final Map<UUID, Long> interactDebounceMs = new HashMap<>();
    private final Map<UUID, UUID> coopOwnerByMember = new HashMap<>(); // member -> owner
    private final Map<UUID, UUID> coopPartnerByOwner = new HashMap<>(); // owner -> member
    private final Map<UUID, UUID> pendingConfirmJoinOwner = new HashMap<>();
    private final Map<UUID, PendingBuildAction> pendingBuildActions = new HashMap<>();
    private static final String COOP_CONFIRM_TITLE = "Confirm Co-op Join";
    private static final String BUILD_CONFIRM_TITLE = "Confirm Build Action";
    private static final String ENV_AREA_CLONE_KEY = "levelplugin_env_area_clone";

    private EnvironmentAreaInstanceManager(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startHologramRefreshTask();
        startBuildTimerTask();
    }

    private void startBuildTimerTask() {
        if (buildTimerTask != null) buildTimerTask.cancel();
        buildTimerTask = new BukkitRunnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                for (EnvironmentAreaSession session : new ArrayList<>(sessions.values())) {
                    Player owner = Bukkit.getPlayer(session.ownerId());
                    if (owner == null || !owner.isOnline()) continue;
                    UUID scoped = resolveProfileScopedId(owner);
                    Map<Integer, Long> map = buildFinishAtByProfile.get(scoped);
                    if (map == null || map.isEmpty()) continue;
                    List<Integer> finished = new ArrayList<>();
                    for (var e : map.entrySet()) {
                        if (e.getValue() <= now) {
                            int slot = e.getKey();
                            markBuiltForProfile(owner, slot);
                            if (slot == 5) setFarmBuildingLevel(scoped, Math.max(1, resolveCurrentLevel(owner, slot)));
                            if (slot == 4) setPalaceBuildingLevel(scoped, Math.max(1, resolveCurrentLevel(owner, slot)));
                            if (slot == 2) setBlacksmithBuildingLevel(scoped, Math.max(1, resolveCurrentLevel(owner, slot)));
                            refreshBuildHologram(session, slot);
                            finished.add(slot);
                        }
                    }
                    finished.forEach(map::remove);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startHologramRefreshTask() {
        if (hologramRefreshTask != null) {
            hologramRefreshTask.cancel();
        }
        hologramRefreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (EnvironmentAreaSession session : new ArrayList<>(sessions.values())) {
                    if (session == null) continue;
                    Player owner = Bukkit.getPlayer(session.ownerId());
                    if (owner == null || !owner.isOnline() || !owner.getWorld().equals(session.world())) continue;
                    for (BuildingTemplate building : BUILDINGS) {
                        Location marker = findMarker(session, building);
                        if (marker == null || marker.distanceSquared(owner.getLocation()) > (28 * 28)) continue;
                        refreshBuildHologram(session, building.slot());
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 40L);
    }

    public static EnvironmentAreaInstanceManager getInstance(Main plugin) {
        if (instance == null) {
            instance = new EnvironmentAreaInstanceManager(plugin);
        }
        return instance;
    }

    public boolean initialize(Player target) {
        if (target == null || !target.isOnline()) {
            return false;
        }
        World source = Bukkit.getWorld(SOURCE_WORLD);
        if (source == null) {
            ChatMessageUtil.send(target, ChatMessageUtil.MessageType.ERROR,
                    "Environment source world '" + SOURCE_WORLD + "' is not loaded.");
            return false;
        }

        CuboidTemplate areaTemplate = getOrCaptureTemplate(source, "area:base", AREA);
        // Warm mine template cache early; allows deferred gameplay setup after join without first-hit capture delay.
        Cuboid resolvedMineArea = resolveKingdomTemplateCuboid(KINGDOM_MINE_AREA);
        getOrCaptureTemplate(source, "area:mine", resolvedMineArea);
        Map<Integer, CuboidTemplate> buildingTemplates = new HashMap<>();
        for (BuildingTemplate building : BUILDINGS) {
            buildingTemplates.put(building.slot(),
                    getOrCaptureTemplate(source, "building:" + building.id().toLowerCase(Locale.ROOT), building.source()));
        }

        World world = recreateWorld(target.getUniqueId());
        if (world == null) {
            ChatMessageUtil.send(target, ChatMessageUtil.MessageType.ERROR,
                    "Could not create environment instance world.");
            return false;
        }

        SlotOffset offset = slotOffsetFor(target.getUniqueId());
        int originX = PASTE_X + offset.dx();
        int originY = PASTE_Y;
        int originZ = PASTE_Z + offset.dz();
        areaTemplate.paste(world, originX, originY, originZ);
        EnvironmentAreaSession old = sessions.remove(target.getUniqueId());
        if (old != null) {
            old.removeHolograms();
        }
        removeAnimatedLeaderboard(target.getUniqueId());

        WorldCuboid border = createSessionBorder(world, originX, originY, originZ);
        EnvironmentAreaSession session = new EnvironmentAreaSession(target.getUniqueId(), world, buildingTemplates, originX, originY, originZ, border);
        sessions.put(target.getUniqueId(), session);
        spawnBuildHolograms(session);
        applySavedBuilds(target, session);
        spawnAnimatedLeaderboard(session);

        Location spawn = toPastedLocation(world, EMPTY_WORLD_SPAWN, originX, originY, originZ);
        if (spawn == null) {
            spawn = new Location(world,
                    originX + (AREA.width() / 2.0),
                    originY + 1.0,
                    originZ + (AREA.depth() / 2.0));
        }
        spawn = spawn.clone().add(0.5, 0.0, 0.5);
        spawn.setYaw(90.0f); // west
        spawn.setPitch(0.0f);
        world.setSpawnLocation(spawn);
        lastValidLocations.put(target.getUniqueId(), spawn.clone());
        target.teleport(spawn);
        ChatMessageUtil.send(target, ChatMessageUtil.MessageType.SUCCESS,
                "Initialized environment area in " + ChatColor.WHITE + world.getName() + ChatColor.GREEN + ".");
        return true;
    }

    public boolean isMineBlock(Player player, Block block) {
        if (player == null || block == null || block.getWorld() == null) {
            return false;
        }
        UUID ownerId = resolveAreaOwner(player.getUniqueId());
        EnvironmentAreaSession session = sessions.get(ownerId);
        if (session == null || !session.world().equals(block.getWorld())) {
            return false;
        }
        Cuboid resolvedMineArea = resolveKingdomTemplateCuboid(KINGDOM_MINE_AREA);
        WorldCuboid mine = toPastedCuboid(resolvedMineArea, session.originX(), session.originY(), session.originZ());
        return mine.contains(block.getLocation());
    }

    public boolean hasSession(UUID playerId) {
        return playerId != null && sessions.containsKey(playerId);
    }

    public void invite(Player owner, Player target) {
        if (owner == null || target == null) {
            return;
        }
        if (owner.getUniqueId().equals(target.getUniqueId())) {
            ChatMessageUtil.send(owner, ChatMessageUtil.MessageType.ERROR, "You cannot invite yourself.");
            return;
        }
        EnvironmentAreaSession ownerSession = sessions.get(owner.getUniqueId());
        if (ownerSession == null) {
            ChatMessageUtil.send(owner, ChatMessageUtil.MessageType.ERROR, "You don't have an initialized debug area.");
            return;
        }
        pendingCoopInvites.put(target.getUniqueId(), owner.getUniqueId());
        ChatMessageUtil.send(owner, ChatMessageUtil.MessageType.SUCCESS, "Invited " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + " to your debug area.");
        ChatMessageUtil.send(target, ChatMessageUtil.MessageType.INFO,
                ChatColor.WHITE + owner.getName() + ChatColor.GRAY + " invited you to their debug area.");
        Component accept = Component.text(ChatColor.GREEN + "[Accept]")
                .clickEvent(ClickEvent.runCommand("/coop accept " + owner.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept this co-op invite")));
        Component deny = Component.text(ChatColor.RED + "[Deny]")
                .clickEvent(ClickEvent.runCommand("/coop deny " + owner.getName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to deny this co-op invite")));
        target.sendMessage(accept.append(Component.text(" ")).append(deny));
    }

    public void accept(Player player) {
        if (player == null) {
            return;
        }
        UUID ownerId = pendingCoopInvites.remove(player.getUniqueId());
        if (ownerId == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "You have no pending debug area invites.");
            return;
        }
        EnvironmentAreaSession ownerSession = sessions.get(ownerId);
        if (ownerSession == null || ownerSession.world() == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "That debug area is no longer active.");
            return;
        }
        if (hasSession(player.getUniqueId())) {
            pendingConfirmJoinOwner.put(player.getUniqueId(), ownerId);
            openCoopConfirm(player, ownerId);
            return;
        }
        completeDebugCoopJoin(player, ownerId, ownerSession);
    }

    private void completeDebugCoopJoin(Player player, UUID ownerId, EnvironmentAreaSession ownerSession) {
        Location tp = lastValidLocations.getOrDefault(ownerId,
                new Location(ownerSession.world(), ownerSession.originX() + (AREA.width() / 2.0), ownerSession.originY() + 1.0, ownerSession.originZ() + (AREA.depth() / 2.0)));
        coopOwnerByMember.put(player.getUniqueId(), ownerId);
        coopPartnerByOwner.put(ownerId, player.getUniqueId());
        player.teleport(tp);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Joined debug area.");
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null) {
            ChatMessageUtil.send(owner, ChatMessageUtil.MessageType.INFO, ChatColor.WHITE + player.getName() + ChatColor.GRAY + " joined your debug area.");
        }
    }

    private void openCoopConfirm(Player player, UUID ownerId) {
        var inv = Bukkit.createInventory(null, 27, COOP_CONFIRM_TITLE);
        inv.setItem(11, me.nakilex.levelplugin.utils.GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm",
                me.nakilex.levelplugin.utils.TooltipUtil.bulletList(
                        ChatColor.RED + "This will delete the progress",
                        ChatColor.RED + "of your current kingdom.")));
        inv.setItem(15, me.nakilex.levelplugin.utils.GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
        player.openInventory(inv);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                "Joining this co-op will delete the progress of your current kingdom.");
    }

    @EventHandler
    public void onCoopConfirmClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!COOP_CONFIRM_TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        UUID ownerId = pendingConfirmJoinOwner.get(player.getUniqueId());
        if (ownerId == null) {
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() == 11) {
            EnvironmentAreaSession ownerSession = sessions.get(ownerId);
            if (ownerSession != null) {
                completeDebugCoopJoin(player, ownerId, ownerSession);
            }
            pendingConfirmJoinOwner.remove(player.getUniqueId());
            player.closeInventory();
        } else if (event.getRawSlot() == 15) {
            pendingConfirmJoinOwner.remove(player.getUniqueId());
            player.closeInventory();
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Co-op join cancelled.");
        }
    }

    public boolean hasPendingInvite(UUID playerId) {
        return playerId != null && pendingCoopInvites.containsKey(playerId);
    }

    public UUID getPendingInviteOwner(UUID inviteeId) {
        return inviteeId == null ? null : pendingCoopInvites.get(inviteeId);
    }

    public boolean clearPendingInvite(UUID inviteeId, UUID ownerId) {
        if (inviteeId == null || ownerId == null) return false;
        UUID existing = pendingCoopInvites.get(inviteeId);
        if (!ownerId.equals(existing)) return false;
        pendingCoopInvites.remove(inviteeId);
        return true;
    }

    public UUID resolveAreaOwner(UUID playerId) {
        if (playerId == null) return null;
        return coopOwnerByMember.getOrDefault(playerId, playerId);
    }

    public boolean isDebugCoopParticipant(UUID playerId) {
        return playerId != null && (coopOwnerByMember.containsKey(playerId) || coopPartnerByOwner.containsKey(playerId));
    }

    public void sendCoopInfo(Player player) {
        UUID id = player.getUniqueId();
        UUID owner = resolveAreaOwner(id);
        UUID partner = coopPartnerByOwner.get(owner);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Debug Area Owner: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(owner).getName());
        if (partner != null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Debug Area Partner: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(partner).getName());
        }
    }

    public String getDebugCoopPartnerName(UUID ownerId) {
        if (ownerId == null) return null;
        UUID partner = coopPartnerByOwner.get(ownerId);
        if (partner == null) return null;
        return Bukkit.getOfflinePlayer(partner).getName();
    }

    public boolean hasCoopPartner(UUID ownerId) {
        return ownerId != null && coopPartnerByOwner.containsKey(ownerId);
    }

    public void kick(Player ownerPlayer, Player target) {
        UUID owner = ownerPlayer.getUniqueId();
        UUID partner = coopPartnerByOwner.get(owner);
        if (partner == null || !partner.equals(target.getUniqueId())) {
            ChatMessageUtil.send(ownerPlayer, ChatMessageUtil.MessageType.ERROR, "That player is not your debug area partner.");
            return;
        }
        coopPartnerByOwner.remove(owner);
        coopOwnerByMember.remove(partner);
        ChatMessageUtil.send(ownerPlayer, ChatMessageUtil.MessageType.SUCCESS, "Removed " + target.getName() + " from debug co-op.");
        ChatMessageUtil.send(target, ChatMessageUtil.MessageType.WARNING, "You were removed from debug co-op.");
    }

    public void removeKingdom(UUID playerId) {
        if (playerId == null) return;
        EnvironmentAreaSession session = sessions.remove(playerId);
        if (session != null) {
            session.removeHolograms();
            Bukkit.unloadWorld(session.world(), false);
            deleteWorldFolder(session.world().getName());
        }
        removeAnimatedLeaderboard(playerId);
        lastValidLocations.remove(playerId);
        UUID partner = coopPartnerByOwner.remove(playerId);
        if (partner != null) coopOwnerByMember.remove(partner);
        UUID owner = coopOwnerByMember.remove(playerId);
        if (owner != null) coopPartnerByOwner.remove(owner);
    }

    public void visit(Player visitor, Player owner) {
        if (visitor == null || owner == null) return;
        EnvironmentAreaSession session = sessions.get(owner.getUniqueId());
        if (session == null) {
            ChatMessageUtil.send(visitor, ChatMessageUtil.MessageType.ERROR, "That player's kingdom is not active.");
            return;
        }
        Location tp = lastValidLocations.getOrDefault(owner.getUniqueId(),
                new Location(session.world(), session.originX() + (AREA.width() / 2.0), session.originY() + 1.0, session.originZ() + (AREA.depth() / 2.0)));
        visitor.teleport(tp);
    }


    public List<String> scanBlocks(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return List.of();
        }
        World source = Bukkit.getWorld(SOURCE_WORLD);
        if (source == null) {
            return List.of("Source world not loaded: " + SOURCE_WORLD);
        }

        BuildingTemplate building = BUILDINGS.stream()
                .filter(candidate -> candidate.id().equalsIgnoreCase(templateName)
                        || candidate.displayName().equalsIgnoreCase(templateName))
                .findFirst()
                .orElse(null);
        if (building == null) {
            return List.of("Unknown template: " + templateName);
        }

        CuboidTemplate template = getOrCaptureTemplate(source,
                "building:" + building.id().toLowerCase(Locale.ROOT), building.source());
        Map<Material, Integer> counts = new HashMap<>();
        for (CuboidTemplate.BlockCopy copy : template.blocks()) {
            counts.merge(copy.data().getMaterial(), 1, Integer::sum);
        }

        List<Map.Entry<Material, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((left, right) -> {
            int byCount = Integer.compare(right.getValue(), left.getValue());
            if (byCount != 0) {
                return byCount;
            }
            return left.getKey().name().compareTo(right.getKey().name());
        });

        List<String> lines = new ArrayList<>();
        lines.add("Template " + building.id() + " block totals:");
        for (Map.Entry<Material, Integer> entry : sorted) {
            String pretty = entry.getKey().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            lines.add(entry.getValue() + "x " + pretty);
        }
        return lines;
    }

    public List<String> templateNames() {
        List<String> names = new ArrayList<>();
        for (BuildingTemplate building : BUILDINGS) {
            names.add(building.id());
        }
        return names;
    }

    private CuboidTemplate capture(World source, Cuboid cuboid) {
        return CuboidTemplate.capture(
                new Location(source, cuboid.x1(), cuboid.y1(), cuboid.z1()),
                new Location(source, cuboid.x2(), cuboid.y2(), cuboid.z2()),
                false);
    }

    private CuboidTemplate getOrCaptureTemplate(World source, String templateKey, Cuboid cuboid) {
        String worldScopedKey = source.getUID() + ":" + templateKey;
        return templateCache.computeIfAbsent(worldScopedKey, ignored -> capture(source, cuboid));
    }


    private World recreateWorld(UUID ownerId) {
        String worldName = "environment_" + ownerId.toString().substring(0, 8).toLowerCase(Locale.ROOT);
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            for (Player player : new ArrayList<>(existing.getPlayers())) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            Bukkit.unloadWorld(existing, false);
        }
        deleteWorldFolder(worldName);

        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generator(new VoidWorldGenerator());
        creator.generateStructures(false);
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            return null;
        }
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setTime(6000L);
        world.setSpawnLocation(PASTE_X + AREA.width() / 2, PASTE_Y + 1, PASTE_Z + AREA.depth() / 2);
        return world;
    }

    private void deleteWorldFolder(String worldName) {
        Path path = Bukkit.getWorldContainer().toPath().resolve(worldName);
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ex) {
                            plugin.getLogger().warning("Could not delete environment world file " + p + ": " + ex.getMessage());
                        }
                    });
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not delete environment world folder '" + worldName + "': " + ex.getMessage());
        }
    }

    private void spawnBuildHolograms(EnvironmentAreaSession session) {
        purgeExistingHologramsForOwner(session);
        session.removeHolograms();
        for (BuildingTemplate building : BUILDINGS) {
            session.holograms().addAll(buildHologramEntitiesForSlot(session, building));
        }
    }

    private void purgeExistingHologramsForOwner(EnvironmentAreaSession session) {
        if (session == null || session.world() == null) return;
        String ownerPrefix = HOLOGRAM_TAG_PREFIX + session.ownerId() + ":";
        for (Entity entity : session.world().getEntities()) {
            if (entity == null || entity.isDead()) continue;
            for (String tag : entity.getScoreboardTags()) {
                if (tag != null && tag.startsWith(ownerPrefix)) {
                    entity.remove();
                    break;
                }
            }
        }
    }

    private List<Entity> buildHologramEntitiesForSlot(EnvironmentAreaSession session, BuildingTemplate building) {
        Location marker = findMarker(session, building);
        String tag = HOLOGRAM_TAG_PREFIX + session.ownerId() + ":" + building.slot();
        List<String> lines = buildHologramLinesForSlot(session, building);
        lastHologramLinesByTag.put(tag, new ArrayList<>(lines));
        return spawnClickableHologram(marker, tag, lines);
    }

    private void refreshBuildHologram(EnvironmentAreaSession session, int slot) {
        if (session == null) return;
        BuildingTemplate building = BUILDINGS_BY_SLOT.get(slot);
        if (building == null) return;
        String tag = HOLOGRAM_TAG_PREFIX + session.ownerId() + ":" + slot;
        List<String> nextLines = buildHologramLinesForSlot(session, building);
        List<String> previousLines = lastHologramLinesByTag.get(tag);
        if (previousLines != null && previousLines.equals(nextLines)) {
            return;
        }
        removeBuildHologram(session, tag);
        session.holograms().addAll(spawnClickableHologram(findMarker(session, building), tag, nextLines));
        lastHologramLinesByTag.put(tag, new ArrayList<>(nextLines));
    }

    private List<String> buildHologramLinesForSlot(EnvironmentAreaSession session, BuildingTemplate building) {
        Player owner = Bukkit.getPlayer(session.ownerId());
        UUID scoped = owner != null ? resolveProfileScopedId(owner) : scopedProfileId(session.ownerId(), 0);
        int currentLevel = owner != null ? resolveCurrentLevel(owner, building.slot()) : 0;
        boolean isBuilt = isSlotBuilt(scoped, building.slot(), currentLevel);
        Long finishAt = getBuildFinishAt(scoped, building.slot());
        if (finishAt != null) {
            long remaining = Math.max(0, (finishAt - System.currentTimeMillis()) / 1000L);
            return java.util.List.of(
                    ChatColor.GOLD + "" + ChatColor.BOLD + "UPGRADING " + ChatColor.WHITE + building.displayName().toUpperCase(Locale.ROOT),
                    ChatColor.YELLOW + "Time Remaining: " + ChatColor.WHITE + SpeedUpScrollUtil.formatDuration(remaining),
                    " ",
                    ChatColor.WHITE + "Right Click " + ChatColor.GRAY + "to speed up"
            );
        }
        String actionText = isBuilt ? "Level Up " : "Build ";
        String clickAction = isBuilt ? "to level up" : "to build";
        if (!isBuilt) {
            currentLevel = 0;
        }
        int nextLevel = isBuilt ? (owner != null ? resolveNextLevel(owner, building.slot()) : 1) : 1;
        if (isBuilt && (nextLevel <= currentLevel || nextLevel > maxLevelForSlot(building.slot()))) {
            return java.util.List.of(
                    ChatColor.GREEN + "" + ChatColor.BOLD + "BUILT " + ChatColor.WHITE + building.displayName().toUpperCase(Locale.ROOT),
                    ChatColor.GRAY + "Stage " + ChatColor.WHITE + currentLevel,
                    ChatColor.DARK_GRAY + "Max stage reached"
            );
        }
        int cost = getUpgradeCostForSlotLevel(building.slot(), nextLevel);
        Map<Material, Integer> materialCosts = getMaterialCostsForSlotLevel(building.slot(), nextLevel);
        boolean hasCoins = owner != null && plugin.getEconomyManager().getBalance(owner) >= cost;
        String levelLine = ChatColor.GOLD + "" + ChatColor.BOLD + "STAGE "
                + ChatColor.YELLOW + currentLevel + " "
                + ChatColor.GREEN + ">" + ChatColor.DARK_GREEN + ">"
                + ChatColor.GREEN + ">" + ChatColor.DARK_GREEN + "> "
                + ChatColor.GOLD + ChatColor.BOLD + "STAGE " + ChatColor.YELLOW + nextLevel;
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GREEN + "" + ChatColor.BOLD + actionText.toUpperCase(Locale.ROOT) + ChatColor.WHITE + building.displayName().toUpperCase(Locale.ROOT));
        lines.add(levelLine);
        lines.add(ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString() + "--------------------");
        lines.add(ChatColor.AQUA + "Requirements:");
        for (Map.Entry<Material, Integer> entry : materialCosts.entrySet()) {
            boolean hasMaterial = owner != null && countInInventory(owner, entry.getKey()) >= entry.getValue();
            lines.add((hasMaterial ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                    + ChatColor.WHITE + entry.getValue() + ChatColor.DARK_GRAY + "x "
                    + ChatColor.WHITE + materialDisplay(entry.getKey()));
        }
        lines.add((hasCoins ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                + ChatColor.WHITE + cost + ChatColor.DARK_GRAY + "x " + ChatColor.GOLD + "<glyph:coins_icon>");
        lines.add(" ");
        lines.add(ChatColor.WHITE + "Right Click " + ChatColor.GRAY + clickAction);
        return lines;
    }

    private boolean isSlotBuilt(UUID scoped, int slot, int currentLevel) {
        return loadBuiltSlots(scoped).contains(slot);
    }

    private Location findMarker(EnvironmentAreaSession session, BuildingTemplate building) {
        World world = session.world();
        WorldCuboid placementBounds = toPastedCuboid(building.placement(), session.originX(), session.originY(), session.originZ());
        Location configured = toPastedLocation(world, building.hologramPoint(), session.originX(), session.originY(), session.originZ());
        if (configured != null) {
            return configured.add(0.5, 1.0, 0.5);
        }
        Block fallback = findFirstBlock(world, placementBounds, building.marker(), false);
        if (fallback != null) {
            return fallback.getLocation().add(0.5, 2.0, 0.5);
        }
        return placementBounds.centerTop(world, 2.0);
    }

    private List<Entity> spawnClickableHologram(Location base, String tag, List<String> lines) {
        List<Entity> entities = new ArrayList<>();
        final float sharedLeftAnchor = -0.40f;
        final double lineStep = 0.25d;
        plugin.getLogger().info("[EnvironmentArea/HologramDebug] tag=" + tag
                + " base=" + base.getBlockX() + "," + base.getBlockY() + "," + base.getBlockZ()
                + " lineCount=" + lines.size()
                + " billboard=CENTER align=LEFT lineWidth=320 fixedLeftAnchor=" + sharedLeftAnchor);
        Interaction clicker = base.getWorld().spawn(base.clone().add(0, -1.0, 0), Interaction.class, interaction -> {
            interaction.setInteractionWidth(4.5f);
            interaction.setInteractionHeight(5.5f);
            interaction.addScoreboardTag(tag);
        });
        entities.add(clicker);

        int requirementsHeaderIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line != null && ChatColor.stripColor(line).trim().equalsIgnoreCase("requirements:")) {
                requirementsHeaderIndex = i;
                break;
            }
        }

        int requirementsStartIndex = requirementsHeaderIndex + 1;
        int requirementsEndIndex = requirementsStartIndex - 1;
        for (int i = requirementsStartIndex; i < lines.size(); i++) {
            String stripped = ChatColor.stripColor(lines.get(i) == null ? "" : lines.get(i)).trim();
            if (stripped.isEmpty() || stripped.equalsIgnoreCase("right click to build")
                    || stripped.equalsIgnoreCase("right click to level up")) {
                break;
            }
            requirementsEndIndex = i;
        }

        entities.addAll(spawnHologramSegment(base, tag, lines, 0, requirementsStartIndex, 0, lineStep, false, sharedLeftAnchor));
        if (requirementsStartIndex <= requirementsEndIndex) {
            // Nudge requirement rows upward so they visually sit right under the "Requirements:" header.
            entities.addAll(spawnHologramSegment(base, tag, lines, requirementsStartIndex, requirementsEndIndex + 1, 0.38d, lineStep, true, sharedLeftAnchor));
        }
        entities.addAll(spawnHologramSegment(base, tag, lines, requirementsEndIndex + 1, lines.size(), 0, lineStep, false, sharedLeftAnchor));
        return entities;
    }

    private int countInInventory(Player player, Material material) {
        if (player == null || material == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != material) {
                continue;
            }
            total += stack.getAmount();
        }
        return total;
    }

    private List<Entity> spawnHologramSegment(Location base,
                                              String tag,
                                              List<String> lines,
                                              int startInclusive,
                                              int endExclusive,
                                              double baseOffset,
                                              double lineStep,
                                              boolean leftAligned,
                                              float sharedLeftAnchor) {
        List<Entity> entities = new ArrayList<>();
        if (startInclusive >= endExclusive || startInclusive < 0 || endExclusive > lines.size()) {
            return entities;
        }
        String blockText = String.join("\n", lines.subList(startInclusive, endExclusive));
        TextDisplay display = (TextDisplay) base.getWorld().spawnEntity(
                base.clone().add(0, -(startInclusive * lineStep) + baseOffset, 0),
                EntityType.TEXT_DISPLAY
        );
        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(leftAligned ? TextDisplay.TextAlignment.LEFT : TextDisplay.TextAlignment.CENTER);
        display.setLineWidth(320);
        if (leftAligned) {
            display.setTransformation(new Transformation(
                    new Vector3f(sharedLeftAnchor, 0f, 0f),
                    new AxisAngle4f(),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f()
            ));
        }
        plugin.getLogger().info("[EnvironmentArea/HologramDebug] segment start=" + startInclusive
                + " end=" + endExclusive
                + " leftAligned=" + leftAligned
                + " lineCount=" + (endExclusive - startInclusive));
        display.setShadowRadius(0f);
        display.setShadowStrength(0f);
        display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        display.setText(blockText);
        display.addScoreboardTag(tag);
        entities.add(display);
        return entities;
    }

    private void playUpgradeCelebration(Player player, Location marker) {
        if (player == null || marker == null) return;
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        FireworkUtil.burstWithinArea(marker,
                marker.getBlockX() - 2, marker.getBlockY() - 2, marker.getBlockZ() - 2,
                marker.getBlockX() + 2, marker.getBlockY() + 3, marker.getBlockZ() + 2,
                6);
    }

    private void handleInteract(Player player, Entity entity, Runnable cancelAction) {
        if (player == null || entity == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = interactDebounceMs.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 150L) {
            cancelAction.run();
            return;
        }
        for (String tag : entity.getScoreboardTags()) {
            if (!tag.startsWith(HOLOGRAM_TAG_PREFIX)) {
                continue;
            }
            interactDebounceMs.put(player.getUniqueId(), now);
            cancelAction.run();
            handleBuildTag(player, tag);
            return;
        }
    }

    private void handleBuildTag(Player player, String tag) {
        String payload = tag.substring(HOLOGRAM_TAG_PREFIX.length());
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            return;
        }
        UUID ownerId;
        int slot;
        try {
            ownerId = UUID.fromString(parts[0]);
            slot = Integer.parseInt(parts[1]);
        } catch (IllegalArgumentException ex) {
            return;
        }
        UUID sessionOwner = resolveAreaOwner(player.getUniqueId());
        if (!ownerId.equals(sessionOwner)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "This environment area belongs to another player.");
            return;
        }
        EnvironmentAreaSession session = sessions.get(sessionOwner);
        BuildingTemplate building = BUILDINGS_BY_SLOT.get(slot);
        if (session == null || building == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Environment build session is no longer active.");
            return;
        }
        CuboidTemplate template = session.buildingTemplates().get(slot);
        if (template == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Template is missing for this build slot.");
            return;
        }
        UUID scoped = resolveProfileScopedId(player);
        int currentLevel = resolveCurrentLevel(player, slot);
        boolean isBuilt = isSlotBuilt(scoped, slot, currentLevel);
        Long finishAt = getBuildFinishAt(scoped, slot);
        if (finishAt != null && finishAt > System.currentTimeMillis()) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (!SpeedUpScrollUtil.isSpeedUpScroll(hand)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        ChatColor.WHITE + "Right click with a Speed Up Scroll" + ChatColor.GRAY + " to reduce build time.");
                refreshBuildHologram(session, slot);
                return;
            }
            int seconds = SpeedUpScrollUtil.getSeconds(hand);
            if (seconds <= 0) {
                refreshBuildHologram(session, slot);
                return;
            }
            long updatedFinish = Math.max(System.currentTimeMillis(), finishAt - (seconds * 1000L));
            setBuildFinishAt(scoped, slot, updatedFinish);
            hand.setAmount(Math.max(0, hand.getAmount() - 1));
            if (hand.getAmount() <= 0) {
                player.getInventory().setItemInMainHand(null);
            }
            refreshBuildHologram(session, slot);
            return;
        }
        if (isBuilt && maxLevelForSlot(slot) <= 1) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    building.displayName() + " is already built.");
            return;
        }
        int nextLevel = isBuilt ? resolveNextLevel(player, slot) : 1;
        if (nextLevel > maxLevelForSlot(slot)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    building.displayName() + " is already at max level.");
            return;
        }
        openBuildConfirm(player, tag, slot, building.displayName(), nextLevel);
    }

    private void openBuildConfirm(Player player, String tag, int slot, String buildingName, int nextLevel) {
        var inv = Bukkit.createInventory(null, 27, BUILD_CONFIRM_TITLE);
        String actionName = nextLevel <= 1 ? "Build " : "Level Up ";
        int cost = getUpgradeCostForSlotLevel(slot, Math.max(1, nextLevel));
        Map<Material, Integer> materialCosts = getMaterialCostsForSlotLevel(slot, Math.max(1, nextLevel));
        List<String> lore = new ArrayList<>();
        lore.addAll(TooltipUtil.bulletList(
                ChatColor.GRAY + actionName + ChatColor.WHITE + buildingName,
                ChatColor.GRAY + "Target Level: " + ChatColor.WHITE + nextLevel));
        lore.add(ChatColor.AQUA + "Requirements:");
        for (Map.Entry<Material, Integer> entry : materialCosts.entrySet()) {
            lore.add(ChatColor.RED + "✘ " + ChatColor.WHITE + entry.getValue() + ChatColor.DARK_GRAY + "x "
                    + ChatColor.WHITE + materialDisplay(entry.getKey()));
        }
        lore.add(ChatColor.RED + "✘ " + ChatColor.WHITE + cost + ChatColor.DARK_GRAY + "x " + ChatColor.GOLD + "<glyph:coins_icon>");
        inv.setItem(11, me.nakilex.levelplugin.utils.GuiUtil.getNexoItem("check", ChatColor.GREEN + "Confirm",
                lore));
        inv.setItem(15, me.nakilex.levelplugin.utils.GuiUtil.getNexoItem("cross", ChatColor.RED + "Cancel"));
        pendingBuildActions.put(player.getUniqueId(), new PendingBuildAction(tag, slot));
        player.openInventory(inv);
    }

    private int resolveNextLevel(Player player, int slot) {
        if (slot == 2) return getBlacksmithBuildingLevel(player) + 1;
        if (slot == 4) return getPalaceBuildingLevel(player) + 1;
        if (slot == 5) return getFarmBuildingLevel(player) + 1;
        return 1;
    }

    private int resolveCurrentLevel(Player player, int slot) {
        if (slot == 2) return getBlacksmithBuildingLevel(player);
        if (slot == 4) return getPalaceBuildingLevel(player);
        if (slot == 5) return getFarmBuildingLevel(player);
        return loadBuiltSlots(resolveProfileScopedId(player)).contains(slot) ? 1 : 0;
    }

    private int maxLevelForSlot(int slot) {
        if (slot == 2) return 12;
        if (slot == 4) return 10;
        if (slot == 5) return 3;
        return 1;
    }


    private long computeBuildDurationMs(int slot, int level) {
        long base = 2L * 60L * 1000L;
        long scaled = (long) (base * Math.pow(1.8, Math.max(0, level - 1)));
        return Math.min(8L * 60L * 60L * 1000L, scaled);
    }

    private void setBuildFinishAt(UUID scoped, int slot, long finishAtMs) {
        buildFinishAtByProfile.computeIfAbsent(scoped, k -> new HashMap<>()).put(slot, finishAtMs);
    }

    private void clearBuildFinishAt(UUID scoped, int slot) {
        Map<Integer, Long> map = buildFinishAtByProfile.get(scoped);
        if (map == null) return;
        map.remove(slot);
        if (map.isEmpty()) buildFinishAtByProfile.remove(scoped);
    }

    private Long getBuildFinishAt(UUID scoped, int slot) {
        Map<Integer, Long> map = buildFinishAtByProfile.get(scoped);
        return map == null ? null : map.get(slot);
    }
    private int getUpgradeCostForSlotLevel(int slot, int nextLevel) {
        int level = Math.max(1, nextLevel);
        int base = switch (slot) {
            case 2 -> 350;
            case 4 -> 500;
            case 5 -> 300;
            default -> 250;
        };
        return base + (level * level * 120);
    }

    private Map<Material, Integer> getMaterialCostsForSlotLevel(int slot, int nextLevel) {
        Map<Material, Integer> costs = new LinkedHashMap<>();
        int safeLevel = Math.max(1, nextLevel);
        costs.put(Material.COBBLESTONE, 24 * safeLevel);
        if (slot == 2) {
            if (safeLevel >= 5) costs.put(Material.RAW_IRON, 48 * (safeLevel - 3));
            if (safeLevel >= 8) costs.put(Material.RAW_GOLD, 32 * (safeLevel - 6));
            if (safeLevel >= 11) costs.put(Material.DIAMOND, 10 * (safeLevel - 9));
        }
        return costs;
    }

    private String materialDisplay(Material material) {
        return me.nakilex.levelplugin.utils.TextUtil.beautifyWords(material.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }

    private void executeBuildAction(Player player, String tag, int slot) {
        UUID sessionOwner = resolveAreaOwner(player.getUniqueId());
        EnvironmentAreaSession session = sessions.get(sessionOwner);
        BuildingTemplate building = BUILDINGS_BY_SLOT.get(slot);
        if (session == null || building == null) return;
        UUID scoped = resolveProfileScopedId(player);
        java.util.Set<Integer> builtSlots = loadBuiltSlots(scoped);
        if (slot == 2 && builtSlots.contains(slot)) {
            int upgraded = upgradeBlacksmithLevel(player, scoped);
            if (upgraded > 0) {
                showBuildingProgressToast(player, "Blacksmith", upgraded, Material.ANVIL, true);
                playUpgradeCelebration(player, findMarker(session, building));
                if (upgraded >= 12) removeBuildHologram(session, tag);
                else refreshBuildHologram(session, slot);
            }
            return;
        }
        if (slot == 4 && builtSlots.contains(slot)) {
            int upgraded = upgradePalaceLevel(player, scoped);
            if (upgraded > 0) {
                showBuildingProgressToast(player, "Palace", upgraded, Material.STONE_BRICKS, true);
                playUpgradeCelebration(player, findMarker(session, building));
                if (upgraded >= 10) removeBuildHologram(session, tag);
                else refreshBuildHologram(session, slot);
            }
            return;
        }
        if (slot == 5 && builtSlots.contains(slot)) {
            int upgraded = upgradeFarmLevel(player, scoped);
            if (upgraded > 0) {
                showBuildingProgressToast(player, "Farm", upgraded, Material.HAY_BLOCK, true);
                playUpgradeCelebration(player, findMarker(session, building));
                if (upgraded >= 3) removeBuildHologram(session, tag);
                else refreshBuildHologram(session, slot);
            }
            return;
        }
        CuboidTemplate template = session.buildingTemplates().get(slot);
        if (template == null) return;
        WorldCuboid destinationArea = toPastedCuboid(building.placement(), session.originX(), session.originY(), session.originZ());
        Location destinationMarker = destinationArea.centerTop(session.world(), 1.0);
        int nextLevel = resolveNextLevel(player, slot);
        int cost = getUpgradeCostForSlotLevel(slot, nextLevel);
        Map<Material, Integer> materialCosts = getMaterialCostsForSlotLevel(slot, nextLevel);
        if (!EnvironmentManager.isDebugIgnoreBuildingMaterialCosts()) {
            for (Map.Entry<Material, Integer> entry : materialCosts.entrySet()) {
                if (!player.getInventory().containsAtLeast(new ItemStack(entry.getKey(), entry.getValue()), entry.getValue())) {
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                            "Missing materials: " + ChatColor.WHITE + entry.getValue() + " " + materialDisplay(entry.getKey()) + ChatColor.RED + ".");
                    return;
                }
            }
        }
        if (!EnvironmentManager.isDebugIgnoreBuildingMaterialCosts()) {
            int coins = plugin.getEconomyManager().getBalance(player);
            if (coins < cost) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                        "You need " + ChatColor.GOLD + cost + " <glyph:coins_icon>"
                                + ChatColor.RED + " to build this.");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.7f);
                return;
            }
            plugin.getEconomyManager().deductCoins(player, cost);
            for (Map.Entry<Material, Integer> entry : materialCosts.entrySet()) {
                player.getInventory().removeItem(new ItemStack(entry.getKey(), entry.getValue()));
            }
            playCoinPaymentVisual(player, destinationMarker, cost);
        }
        BukkitTask existing = activeBuildTasks.remove(sessionOwner);
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    activeBuildTasks.remove(sessionOwner);
                    cancel();
                    return;
                }
                long buildDurationMs = computeBuildDurationMs(slot, nextLevel);
                long finishAt = System.currentTimeMillis() + buildDurationMs;
                UUID profileScoped = resolveProfileScopedId(player);
                setBuildFinishAt(profileScoped, slot, finishAt);
                refreshBuildHologram(session, slot);
                long totalTicks = Math.max(1L, Math.round((buildDurationMs / 1000.0) * 20.0));
                buildTemplateLayered(player, session, building, template, destinationArea, destinationMarker, totalTicks, () -> {
                    markBuiltForProfile(player, slot);
                    clearBuildFinishAt(profileScoped, slot);
                    if (slot == 4) {
                        setPalaceBuildingLevel(profileScoped, Math.max(1, nextLevel));
                    }
                    if (slot == 2) {
                        setBlacksmithBuildingLevel(profileScoped, Math.max(1, nextLevel));
                    }
                    if (slot == 5) {
                        setFarmBuildingLevel(profileScoped, Math.max(1, nextLevel));
                    }
                    refreshBuildHologram(session, slot);
                });
                if (slot != 5 && slot != 4 && slot != 2) {
                    removeBuildHologram(session, tag);
                } else {
                    refreshBuildHologram(session, slot);
                }
                activeBuildTasks.remove(sessionOwner);
            }
        }.runTaskLater(plugin, PAYMENT_ANIMATION_TICKS);
        activeBuildTasks.put(sessionOwner, task);
    }

    @EventHandler
    public void onBuildConfirmClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!BUILD_CONFIRM_TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        PendingBuildAction pending = pendingBuildActions.get(player.getUniqueId());
        if (pending == null) {
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() == 11) {
            executeBuildAction(player, pending.tag(), pending.slot());
            pendingBuildActions.remove(player.getUniqueId());
            player.closeInventory();
        } else if (event.getRawSlot() == 15) {
            pendingBuildActions.remove(player.getUniqueId());
            player.closeInventory();
        }
    }


    public record RuntimeCuboid(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    public RuntimeCuboid projectFinishedSelectionForPlayer(Player player, int x1, int y1, int z1, int x2, int y2, int z2) {
        if (player == null) return null;
        UUID ownerId = resolveAreaOwner(player.getUniqueId());
        EnvironmentAreaSession session = sessions.get(ownerId);
        if (session == null || session.world() == null) return null;
        Cuboid resolved = resolveKingdomTemplateCuboid(new Cuboid(x1, y1, z1, x2, y2, z2));
        WorldCuboid pasted = toPastedCuboid(resolved, session.originX(), session.originY(), session.originZ());
        return new RuntimeCuboid(session.world(),
                Math.min(pasted.x1(), pasted.x2()), Math.min(pasted.y1(), pasted.y2()), Math.min(pasted.z1(), pasted.z2()),
                Math.max(pasted.x1(), pasted.x2()), Math.max(pasted.y1(), pasted.y2()), Math.max(pasted.z1(), pasted.z2()));
    }

    public int maxAllBuilds(Player player) {
        if (player == null) return 0;
        UUID ownerId = resolveAreaOwner(player.getUniqueId());
        EnvironmentAreaSession session = sessions.get(ownerId);
        if (session == null) {
            if (!initialize(player)) return 0;
            session = sessions.get(ownerId);
            if (session == null) return 0;
        }
        UUID scoped = resolveProfileScopedId(player);
        java.util.Set<Integer> built = builtSlotsByProfile.computeIfAbsent(scoped, ignored -> new java.util.HashSet<>());
        int added = 0;
        for (BuildingTemplate building : BUILDINGS) {
            int slot = building.slot();
            if (built.contains(slot)) continue;
            CuboidTemplate template = session.buildingTemplates().get(slot);
            if (template == null) continue;
            WorldCuboid area = toPastedCuboid(building.placement(), session.originX(), session.originY(), session.originZ());
            pasteBuiltTemplate(session, building, template, area, true);
            removeBuildHologram(session, HOLOGRAM_TAG_PREFIX + session.ownerId() + ":" + slot);
            built.add(slot);
            if (slot == 5) {
                setFarmBuildingLevel(scoped, 3);
            } else if (slot == 4) {
                setPalaceBuildingLevel(scoped, 10);
            } else if (slot == 2) {
                setBlacksmithBuildingLevel(scoped, 12);
            }
            added++;
        }
        saveBuiltSlots(scoped);
        return added;
    }
    private UUID resolveAreaOwner(Player player) {
        return player.getUniqueId();
    }

    private UUID resolveProfileScopedId(Player player) {
        Integer slot = me.nakilex.levelplugin.player.profile.ProfileManager.getInstance().getActiveSlot(player.getUniqueId());
        int safeSlot = slot == null ? 0 : Math.max(0, slot);
        return scopedProfileId(resolveAreaOwner(player), safeSlot);
    }

    private UUID scopedProfileId(UUID ownerId, int slot) {
        String key = ownerId + ":" + Math.max(0, slot);
        return UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void markBuiltForProfile(Player player, int slot) {
        UUID scoped = resolveProfileScopedId(player);
        builtSlotsByProfile.computeIfAbsent(scoped, ignored -> new java.util.HashSet<>()).add(slot);
        saveBuiltSlots(scoped);
    }

    public int getFarmBuildingLevel(Player player) {
        if (player == null) return 0;
        UUID scoped = resolveProfileScopedId(player);
        return farmBuildingLevelByProfile.computeIfAbsent(scoped,
                id -> plugin.getPlayerConfig().getConfig().getInt("players." + id + ".environment.area.farm-building-level", 0));
    }

    public int getPalaceBuildingLevel(Player player) {
        if (player == null) return 0;
        UUID scoped = resolveProfileScopedId(player);
        return palaceBuildingLevelByProfile.computeIfAbsent(scoped,
                id -> plugin.getPlayerConfig().getConfig().getInt("players." + id + ".environment.area.palace-building-level", 0));
    }

    public int getBlacksmithBuildingLevel(Player player) {
        if (player == null) return 0;
        UUID scoped = resolveProfileScopedId(player);
        return blacksmithBuildingLevelByProfile.computeIfAbsent(scoped,
                id -> Math.max(0, plugin.getPlayerConfig().getConfig().getInt("players." + id + ".environment.area.blacksmith-building-level", 0)));
    }


    private void setFarmBuildingLevel(UUID scoped, int level) {
        int clamped = Math.max(0, Math.min(3, level));
        farmBuildingLevelByProfile.put(scoped, clamped);
        plugin.getPlayerConfig().getConfig().set("players." + scoped + ".environment.area.farm-building-level", clamped);
        // keep legacy key synced for backward compatibility
        plugin.getPlayerConfig().getConfig().set("players." + scoped + ".environment.area.farm-level", clamped);
        plugin.getPlayerConfig().saveConfigFile();
    }

    private int upgradeFarmLevel(Player player, UUID scoped) {
        int current = getFarmBuildingLevel(player);
        if (current >= 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Farm is already at max level.");
            return 0;
        }
        int cost = BUILD_COST_COINS;
        if (EnvironmentManager.isDebugIgnoreBuildingMaterialCosts()) {
            setFarmBuildingLevel(scoped, current + 1);
            return current + 1;
        }
        int coins = plugin.getEconomyManager().getBalance(player);
        if (coins < cost) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "You need " + ChatColor.GOLD + cost + " <glyph:coins_icon>" + ChatColor.RED + " to upgrade the farm.");
            return 0;
        }
        plugin.getEconomyManager().deductCoins(player, cost);
        setFarmBuildingLevel(scoped, current + 1);
        return current + 1;
    }

    private void setPalaceBuildingLevel(UUID scoped, int level) {
        int clamped = Math.max(0, Math.min(10, level));
        palaceBuildingLevelByProfile.put(scoped, clamped);
        plugin.getPlayerConfig().getConfig().set("players." + scoped + ".environment.area.palace-building-level", clamped);
        plugin.getPlayerConfig().saveConfigFile();
    }

    private void setBlacksmithBuildingLevel(UUID scoped, int level) {
        int clamped = Math.max(0, Math.min(12, level));
        blacksmithBuildingLevelByProfile.put(scoped, clamped);
        plugin.getPlayerConfig().getConfig().set("players." + scoped + ".environment.area.blacksmith-building-level", clamped);
    }

    private int upgradeBlacksmithLevel(Player player, UUID scoped) {
        int current = getBlacksmithBuildingLevel(player);
        if (current >= 12) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Blacksmith is already at max level.");
            return 0;
        }
        int cost = getUpgradeCostForSlotLevel(2, current + 1);
        if (EnvironmentManager.isDebugIgnoreBuildingMaterialCosts()) {
            setBlacksmithBuildingLevel(scoped, current + 1);
            plugin.getPlayerConfig().saveConfigFile();
            return current + 1;
        }
        int coins = plugin.getEconomyManager().getBalance(player);
        if (coins < cost) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "You need " + ChatColor.GOLD + cost + " <glyph:coins_icon>" + ChatColor.RED + " to upgrade the blacksmith.");
            return 0;
        }
        plugin.getEconomyManager().deductCoins(player, cost);
        setBlacksmithBuildingLevel(scoped, current + 1);
        plugin.getPlayerConfig().saveConfigFile();
        return current + 1;
    }

    private int upgradePalaceLevel(Player player, UUID scoped) {
        int current = getPalaceBuildingLevel(player);
        if (current >= 10) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, "Palace is already at max level.");
            return 0;
        }
        int cost = BUILD_COST_COINS;
        if (EnvironmentManager.isDebugIgnoreBuildingMaterialCosts()) {
            setPalaceBuildingLevel(scoped, Math.max(1, current + 1));
            return Math.max(1, current + 1);
        }
        int coins = plugin.getEconomyManager().getBalance(player);
        if (coins < cost) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "You need " + ChatColor.GOLD + cost + " <glyph:coins_icon>" + ChatColor.RED + " to upgrade the palace.");
            return 0;
        }
        plugin.getEconomyManager().deductCoins(player, cost);
        setPalaceBuildingLevel(scoped, Math.max(1, current + 1));
        return Math.max(1, current + 1);
    }

    private void applySavedBuilds(Player player, EnvironmentAreaSession session) {
        UUID scoped = resolveProfileScopedId(player);
        java.util.Set<Integer> built = loadBuiltSlots(scoped);
        for (Integer slot : built) {
            BuildingTemplate building = BUILDINGS_BY_SLOT.get(slot);
            CuboidTemplate template = session.buildingTemplates().get(slot);
            if (building == null || template == null) continue;
            WorldCuboid area = toPastedCuboid(building.placement(), session.originX(), session.originY(), session.originZ());
            pasteBuiltTemplate(session, building, template, area, false);
            removeBuildHologram(session, HOLOGRAM_TAG_PREFIX + session.ownerId() + ":" + slot);
        }
    }


    private void pasteBuiltTemplate(EnvironmentAreaSession session,
                                    BuildingTemplate building,
                                    CuboidTemplate template,
                                    WorldCuboid area,
                                    boolean copyNpcs) {
        if (session == null || building == null || template == null || area == null || session.world() == null) {
            return;
        }
        template.paste(session.world(), area.minX(), area.minY(), area.minZ());
        if (copyNpcs) {
            copyCitizensNpcsIntoBuiltBuilding(session, building, area);
        }
    }

    private java.util.Set<Integer> loadBuiltSlots(UUID scoped) {
        java.util.Set<Integer> cached = builtSlotsByProfile.get(scoped);
        if (cached != null) return cached;
        var cfg = plugin.getPlayerConfig().getConfig().getIntegerList("players." + scoped + ".environment.area.built-slots");
        java.util.Set<Integer> built = new java.util.HashSet<>(cfg);
        builtSlotsByProfile.put(scoped, built);
        return built;
    }

    private void saveBuiltSlots(UUID scoped) {
        plugin.getPlayerConfig().getConfig().set("players." + scoped + ".environment.area.built-slots",
                new java.util.ArrayList<>(builtSlotsByProfile.getOrDefault(scoped, java.util.Set.of())));
        plugin.getPlayerConfig().saveConfigFile();
    }

    public void clearProfileKingdomProgress(UUID playerId, int slot) {
        if (playerId == null) return;
        UUID scoped = scopedProfileId(playerId, slot);
        builtSlotsByProfile.remove(scoped);
        plugin.getPlayerConfig().getConfig().set("players." + scoped + ".environment.area.built-slots", null);
        plugin.getPlayerConfig().saveConfigFile();
    }

    private void playCoinPaymentVisual(Player player, Location destinationMarker, int amount) {
        List<PaymentVisual> coinVisuals = PAYMENT_COIN_VISUALS.stream()
                .map(v -> new PaymentVisual(v.value(), v.material(), v.modelId()))
                .toList();
        playPaymentVisual(player, destinationMarker, coinVisuals, amount, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    private void playPaymentVisual(Player player, Location destinationMarker, List<PaymentVisual> visuals, int amount, Sound pingSound) {
        if (player == null || destinationMarker == null || amount <= 0) {
            return;
        }
        World world = destinationMarker.getWorld();
        if (world == null || player.getWorld() == null || !player.getWorld().equals(world)) {
            return;
        }
        Location target = destinationMarker.clone().add(0.5, 1.0, 0.5);
        List<PaymentVisual> emissions = buildVisualSequence(amount, visuals);
        if (emissions.isEmpty()) {
            return;
        }
        new BukkitRunnable() {
            int sent = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !player.getWorld().equals(world) || sent >= emissions.size()) {
                    cancel();
                    return;
                }
                Location source = player.getLocation().clone().add(0.0, 1.1, 0.0);
                PaymentVisual visual = emissions.get(sent);
                Item drop = world.dropItem(source, new org.bukkit.inventory.ItemStack(visual.material(), 1));
                drop.setPickupDelay(Integer.MAX_VALUE);
                drop.setCanMobPickup(false);
                drop.setUnlimitedLifetime(false);
                if (visual.modelId() != null && !visual.modelId().isBlank()) {
                    ModelEngineUtil.applyFirstAvailableModel(drop, java.util.List.of(visual.modelId()), plugin);
                }
                var vec = target.toVector().subtract(drop.getLocation().toVector());
                if (vec.lengthSquared() > 0.001) {
                    drop.setVelocity(vec.normalize().multiply(0.42));
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (drop.isValid()) {
                        drop.remove();
                    }
                }, 12L);
                world.playSound(target, pingSound, 0.45f, 1.5f + (sent * 0.01f));
                sent++;
            }
        }.runTaskTimer(plugin, 0L, COIN_SEND_INTERVAL_TICKS);
    }

    private List<PaymentVisual> buildVisualSequence(int amount, List<PaymentVisual> visuals) {
        if (amount <= 0 || visuals == null || visuals.isEmpty()) {
            return List.of();
        }
        int remaining = amount;
        List<PaymentVisual> sequence = new ArrayList<>();
        for (PaymentVisual visual : visuals) {
            int count = remaining / visual.value();
            remaining %= visual.value();
            for (int i = 0; i < count; i++) {
                sequence.add(visual);
            }
        }
        return sequence;
    }


    private void removeBuildHologram(EnvironmentAreaSession session, String tag) {
        if (session == null || tag == null || tag.isBlank()) {
            return;
        }
        lastHologramLinesByTag.remove(tag);
        session.holograms().removeIf(entity -> {
            if (entity == null || entity.isDead()) {
                return true;
            }
            if (!entity.getScoreboardTags().contains(tag)) {
                return false;
            }
            entity.remove();
            return true;
        });
    }

    private void buildTemplateLayered(Player player,
                                      EnvironmentAreaSession session,
                                      BuildingTemplate building,
                                      CuboidTemplate template,
                                      WorldCuboid destinationArea,
                                      Location destinationMarker,
                                      long totalTicks,
                                      Runnable onComplete) {
        if (player == null || session == null || building == null || template == null
                || destinationArea == null || destinationMarker == null) {
            return;
        }
        int baseX = destinationArea.minX();
        int baseY = destinationArea.minY();
        int baseZ = destinationArea.minZ();
        List<CuboidTemplate.BlockCopy> copies = new ArrayList<>(template.blocks());
        copies.sort(Comparator.comparingInt(CuboidTemplate.BlockCopy::y));
        plugin.getLogger().info("[EnvironmentArea] Building '" + building.id() + "' for " + player.getName()
                + " -> sourceDims=" + template.width() + "x" + template.height() + "x" + template.depth()
                + ", destMin=" + baseX + "," + baseY + "," + baseZ
                + ", destMax=" + destinationArea.maxX() + "," + destinationArea.maxY() + "," + destinationArea.maxZ()
                + ", blockCount=" + copies.size());
        int animationTicks = scaledBuildAnimationTicks(totalTicks);
        int blocksPerTick = Math.max(1, copies.size() / animationTicks);
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                World world = session.world();
                if (world == null) {
                    cancel();
                    return;
                }
                for (int i = 0; i < blocksPerTick && index < copies.size(); i++, index++) {
                    CuboidTemplate.BlockCopy copy = copies.get(index);
                    world.getBlockAt(baseX + copy.x(), baseY + copy.y(), baseZ + copy.z())
                            .setBlockData(copy.data(), false);
                }
                if (index >= copies.size()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    FireworkUtil.burstWithinArea(destinationMarker,
                            destinationArea.minX(), destinationArea.minY(), destinationArea.minZ(),
                            destinationArea.maxX(), destinationArea.maxY(), destinationArea.maxZ(),
                            8);
                    int currentLevel = resolveCurrentLevel(player, building.slot());
                    showBuildingProgressToast(player, building.displayName(), currentLevel, building.marker(), currentLevel > 1);
                    copyCitizensNpcsIntoBuiltBuilding(session, building, destinationArea);
                    if (onComplete != null) onComplete.run();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void showBuildingProgressToast(Player player, String buildingName, int level, Material icon, boolean leveledUp) {
        if (player == null || buildingName == null) return;
        String clean = buildingName.toLowerCase(Locale.ROOT).replace(' ', '_');
        String verb = leveledUp ? "Upgrade " : "Build ";
        AdvancementDisplay display = new AdvancementDisplay.Builder(icon == null ? Material.PAPER : icon)
                .title(verb + buildingName)
                .descriptionLine("Level " + Math.max(1, level))
                .frameType(AdvancementDisplay.FrameType.TASK)
                .showToast(true)
                .announceChat(false)
                .build();
        BaseAdvancement toastAdvancement = new BaseAdvancement(
                new AdvancementKey("kingdom_build", clean + "_stage_" + Math.max(1, level)),
                display,
                1,
                null
        );
        AdvancementToastUtil.showToast(player, toastAdvancement);
    }

    private static int scaledBuildAnimationTicks(long baseTicks) {
        int speed = Math.max(MIN_BUILD_SPEED_PERCENT, Math.min(MAX_BUILD_SPEED_PERCENT, buildSpeedPercent));
        long safeBaseTicks = Math.max(1L, baseTicks);
        return Math.max(1, (int) Math.round(safeBaseTicks * (100.0 / speed)));
    }

    public static int getBuildSpeedPercent() {
        return buildSpeedPercent;
    }

    public static void setBuildSpeedPercent(int speedPercent) {
        buildSpeedPercent = Math.max(MIN_BUILD_SPEED_PERCENT, Math.min(MAX_BUILD_SPEED_PERCENT, speedPercent));
    }

    private void copyCitizensNpcsIntoBuiltBuilding(EnvironmentAreaSession session,
                                                   BuildingTemplate building,
                                                   WorldCuboid destinationArea) {
        if (session == null || building == null || destinationArea == null) return;
        World sourceWorld = Bukkit.getWorld(SOURCE_WORLD);
        if (sourceWorld == null) {
            plugin.getLogger().warning("[EnvironmentArea] Could not copy Citizens NPCs for building '" + building.id()
                    + "': source world '" + SOURCE_WORLD + "' is unavailable.");
            return;
        }
        Cuboid sourceCuboid = building.source();
        int sourceMinX = sourceCuboid.minX();
        int sourceMinY = sourceCuboid.minY();
        int sourceMinZ = sourceCuboid.minZ();
        int found = 0;
        int spawned = 0;
        for (net.citizensnpcs.api.npc.NPC template : CitizensAPI.getNPCRegistry()) {
            if (template.data().get(ENV_AREA_CLONE_KEY, false)) {
                continue;
            }
            Location npcLocation = template.isSpawned() && template.getEntity() != null
                    ? template.getEntity().getLocation()
                    : template.getStoredLocation();
            if (!isInsideSelection(npcLocation, sourceWorld, sourceCuboid)) continue;
            found++;

            int relX = npcLocation.getBlockX() - sourceMinX;
            int relY = npcLocation.getBlockY() - sourceMinY;
            int relZ = npcLocation.getBlockZ() - sourceMinZ;
            Location dest = new Location(
                    session.world(),
                    destinationArea.minX() + relX + 0.5,
                    destinationArea.minY() + relY,
                    destinationArea.minZ() + relZ + 0.5,
                    npcLocation.getYaw(),
                    npcLocation.getPitch());

            org.bukkit.entity.EntityType type = template.isSpawned() && template.getEntity() != null
                    ? template.getEntity().getType()
                    : org.bukkit.entity.EntityType.PLAYER;
            net.citizensnpcs.api.npc.NPC clone = cloneCitizensNpc(template, type);
            clone.spawn(dest);
            if (!clone.isSpawned() || clone.getEntity() == null) {
                plugin.getLogger().warning("[EnvironmentArea] Citizens clone failed to spawn for templateId="
                        + template.getId() + " name='" + template.getName() + "' type=" + type
                        + " dest=" + dest.getBlockX() + "," + dest.getBlockY() + "," + dest.getBlockZ());
                continue;
            }
            // Copy common Citizens metadata after spawn so failed/invalid persisted data
            // cannot block the clone from appearing in-world.
            clone.data().setPersistent(net.citizensnpcs.api.npc.NPC.Metadata.NAMEPLATE_VISIBLE,
                    template.data().get(net.citizensnpcs.api.npc.NPC.Metadata.NAMEPLATE_VISIBLE, true));
            clone.data().setPersistent(net.citizensnpcs.api.npc.NPC.Metadata.DEFAULT_PROTECTED,
                    template.data().get(net.citizensnpcs.api.npc.NPC.Metadata.DEFAULT_PROTECTED, true));
            clone.data().setPersistent(ENV_AREA_CLONE_KEY, true);
            spawned++;
            plugin.getLogger().info("[EnvironmentArea] Copied Citizens NPC templateId=" + template.getId()
                    + " name='" + template.getName() + "' for building='" + building.id() + "'"
                    + " source=" + npcLocation.getBlockX() + "," + npcLocation.getBlockY() + "," + npcLocation.getBlockZ()
                    + " -> dest=" + dest.getBlockX() + "," + dest.getBlockY() + "," + dest.getBlockZ());
        }

        if (found == 0) {
            plugin.getLogger().warning("[EnvironmentArea] No Citizens NPC templates found inside building source cuboid for '"
                    + building.id() + "'. sourceBounds=[" + sourceCuboid.minX() + "," + sourceCuboid.minY() + "," + sourceCuboid.minZ()
                    + "] to [" + sourceCuboid.maxX() + "," + sourceCuboid.maxY() + "," + sourceCuboid.maxZ() + "]");
            return;
        }
        plugin.getLogger().info("[EnvironmentArea] Copied Citizens NPC templates for building='" + building.id()
                + "': found=" + found + ", spawned=" + spawned);
    }

    private boolean isInsideSelection(Location location, World expectedWorld, Cuboid selection) {
        if (location == null || expectedWorld == null || selection == null) return false;
        if (!expectedWorld.equals(location.getWorld())) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= selection.minX() && x <= selection.maxX()
                && y >= selection.minY() && y <= selection.maxY()
                && z >= selection.minZ() && z <= selection.maxZ();
    }

    private net.citizensnpcs.api.npc.NPC cloneCitizensNpc(net.citizensnpcs.api.npc.NPC template,
                                                           org.bukkit.entity.EntityType fallbackType) {
        try {
            java.lang.reflect.Method copyMethod = template.getClass().getMethod("copy");
            Object copied = copyMethod.invoke(template);
            if (copied instanceof net.citizensnpcs.api.npc.NPC copiedNpc) {
                return copiedNpc;
            }
        } catch (Throwable ignored) {
        }
        return CitizensAPI.getNPCRegistry().createNPC(fallbackType, template.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        EnvironmentAreaSession session = sessions.get(playerId);
        if (session == null) return;
        if (!player.getWorld().equals(session.world())) return;
        Location to = event.getTo();
        if (session.border().contains(to)) {
            lastValidLocations.put(playerId, to.clone());
            return;
        }
        Location fallback = lastValidLocations.get(playerId);
        if (fallback == null || !session.world().equals(fallback.getWorld()) || !session.border().contains(fallback)) {
            fallback = toPastedLocation(session.world(), EMPTY_WORLD_SPAWN, session.originX(), session.originY(), session.originZ());
            if (fallback == null) {
                fallback = new Location(session.world(),
                        session.originX() + (AREA.width() / 2.0),
                        session.originY() + 1.0,
                        session.originZ() + (AREA.depth() / 2.0));
            }
            fallback = fallback.clone().add(0.5, 0.0, 0.5);
        }
        player.teleport(fallback);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "You cannot leave your area border.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWaterFlow(BlockFromToEvent event) {
        Block block = event.getBlock();
        if (block == null || (block.getType() != Material.WATER && block.getType() != Material.LAVA)) return;
        if (isEnvironmentSessionWorld(block.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFluidLevelChange(FluidLevelChangeEvent event) {
        Block block = event.getBlock();
        if (block == null || (block.getType() != Material.WATER && block.getType() != Material.LAVA)) return;
        if (isEnvironmentSessionWorld(block.getWorld())) {
            event.setCancelled(true);
        }
    }

    private boolean isEnvironmentSessionWorld(World world) {
        if (world == null) return false;
        for (EnvironmentAreaSession session : sessions.values()) {
            if (session != null && world.equals(session.world())) return true;
        }
        return false;
    }

    private record Cuboid(int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX() { return Math.min(x1, x2); }
        int minY() { return Math.min(y1, y2); }
        int minZ() { return Math.min(z1, z2); }
        int maxX() { return Math.max(x1, x2); }
        int maxY() { return Math.max(y1, y2); }
        int maxZ() { return Math.max(z1, z2); }
        int width() { return Math.abs(x1 - x2) + 1; }
        int depth() { return Math.abs(z1 - z2) + 1; }
        boolean contains(WorldPoint point) {
            if (point == null) {
                return false;
            }
            return point.x() >= minX() && point.x() <= maxX()
                    && point.y() >= minY() && point.y() <= maxY()
                    && point.z() >= minZ() && point.z() <= maxZ();
        }
        Cuboid translate(int dx, int dy, int dz) {
            return new Cuboid(x1 + dx, y1 + dy, z1 + dz, x2 + dx, y2 + dy, z2 + dz);
        }
    }

    private record BuildingTemplate(int slot,
                                    String id,
                                    String displayName,
                                    Material marker,
                                    Cuboid source,
                                    Cuboid placement,
                                    WorldPoint hologramPoint) { }

    private record WorldPoint(int x, int y, int z) { }


    private WorldCuboid toPastedCuboid(Cuboid source, int originX, int originY, int originZ) {
        return new WorldCuboid(
                originX + (source.minX() - AREA.minX()),
                originY + (source.minY() - AREA.minY()),
                originZ + (source.minZ() - AREA.minZ()),
                originX + (source.maxX() - AREA.minX()),
                originY + (source.maxY() - AREA.minY()),
                originZ + (source.maxZ() - AREA.minZ()));
    }

    private WorldCuboid createSessionBorder(World world, int originX, int originY, int originZ) {
        WorldCuboid pastedArea = toPastedCuboid(AREA, originX, originY, originZ);
        return new WorldCuboid(
                pastedArea.minX(),
                originY + BORDER_MIN_Y_OFFSET,
                pastedArea.minZ(),
                pastedArea.maxX(),
                world.getMaxHeight() - 1,
                pastedArea.maxZ());
    }

    private static Cuboid projectFinishedToEmpty(Cuboid finishedSelection) {
        int dx = EMPTY_WORLD_ANCHOR.x() - FINISHED_WORLD_ANCHOR.x();
        int dy = EMPTY_WORLD_ANCHOR.y() - FINISHED_WORLD_ANCHOR.y();
        int dz = EMPTY_WORLD_ANCHOR.z() - FINISHED_WORLD_ANCHOR.z();
        return finishedSelection.translate(dx, dy, dz);
    }

    private static WorldPoint projectFinishedToEmpty(WorldPoint finishedPoint) {
        int dx = EMPTY_WORLD_ANCHOR.x() - FINISHED_WORLD_ANCHOR.x();
        int dy = EMPTY_WORLD_ANCHOR.y() - FINISHED_WORLD_ANCHOR.y();
        int dz = EMPTY_WORLD_ANCHOR.z() - FINISHED_WORLD_ANCHOR.z();
        return new WorldPoint(finishedPoint.x() + dx, finishedPoint.y() + dy, finishedPoint.z() + dz);
    }

    private static Cuboid resolveKingdomTemplateCuboid(Cuboid cuboid) {
        if (cuboid == null) {
            return null;
        }
        boolean inBaseArea = isInsideCuboid(cuboid, AREA);
        if (inBaseArea) {
            return cuboid;
        }
        boolean inFinishedArea = isInsideCuboid(cuboid, FINISHED_WORLD_AREA);
        if (inFinishedArea) {
            return projectFinishedToEmpty(cuboid);
        }
        return cuboid;
    }

    private static boolean isInsideCuboid(Cuboid inner, Cuboid outer) {
        if (inner == null || outer == null) {
            return false;
        }
        return inner.minX() >= outer.minX() && inner.maxX() <= outer.maxX()
                && inner.minY() >= outer.minY() && inner.maxY() <= outer.maxY()
                && inner.minZ() >= outer.minZ() && inner.maxZ() <= outer.maxZ();
    }

    private static WorldPoint resolveKingdomTemplatePoint(WorldPoint point) {
        if (point == null) {
            return null;
        }
        if (AREA.contains(point)) {
            return point;
        }
        if (FINISHED_WORLD_AREA.contains(point)) {
            return projectFinishedToEmpty(point);
        }
        return point;
    }

    private void spawnAnimatedLeaderboard(EnvironmentAreaSession session) {
        if (session == null || session.world() == null) {
            return;
        }
        removeAnimatedLeaderboard(session.ownerId());
        WorldPoint resolvedSourcePoint = resolveKingdomTemplatePoint(KINGDOM_ANIMATED_LB);
        Location origin = toPastedLocation(session.world(), resolvedSourcePoint, session.originX(), session.originY(), session.originZ());
        if (origin == null) {
            return;
        }
        origin.add(0.5D, 0.0D, 0.0D);
        float yaw = (float) plugin.getConfig().getDouble("animatedlb.yaw", 0.0D);
        origin.setYaw(yaw + 180.0F);
        origin.setPitch(0.0F);

        LeaderboardDataProvider provider = plugin instanceof Main main
                ? new PlayerStatsLeaderboardDataProvider(main)
                : new MockLeaderboardDataProvider();
        AnimatedLeaderboard board = new AnimatedLeaderboard(
                plugin,
                provider,
                origin,
                (float) plugin.getConfig().getDouble("animatedlb.scale", 0.85D),
                plugin.getConfig().getInt("animatedlb.cycle-duration", 200),
                plugin.getConfig().getInt("animatedlb.row-count", 10),
                plugin.getConfig().getDouble("animatedlb.animation-speed", 1.0D));
        board.spawn();
        animatedLeaderboardsByOwner.put(session.ownerId(), board);
    }

    private void removeAnimatedLeaderboard(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        AnimatedLeaderboard board = animatedLeaderboardsByOwner.remove(ownerId);
        if (board != null) {
            board.remove();
        }
    }

    private Location toPastedLocation(World world, WorldPoint source, int originX, int originY, int originZ) {
        if (world == null || source == null) {
            return null;
        }
        return new Location(
                world,
                originX + (source.x() - AREA.minX()),
                originY + (source.y() - AREA.minY()),
                originZ + (source.z() - AREA.minZ()));
    }

    private Block findFirstBlock(World world, WorldCuboid cuboid, Material material, boolean includeY) {
        if (world == null || cuboid == null || material == null) {
            return null;
        }
        for (int x = cuboid.minX(); x <= cuboid.maxX(); x++) {
            if (includeY) {
                for (int y = cuboid.minY(); y <= cuboid.maxY(); y++) {
                    for (int z = cuboid.minZ(); z <= cuboid.maxZ(); z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == material) {
                            return block;
                        }
                    }
                }
                continue;
            }
            int y = cuboid.minY();
            for (int z = cuboid.minZ(); z <= cuboid.maxZ(); z++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == material) {
                    return block;
                }
            }
        }
        return null;
    }


    private record WorldCuboid(int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX() { return Math.min(x1, x2); }
        int minY() { return Math.min(y1, y2); }
        int minZ() { return Math.min(z1, z2); }
        int maxX() { return Math.max(x1, x2); }
        int maxY() { return Math.max(y1, y2); }
        int maxZ() { return Math.max(z1, z2); }

        Location centerTop(World world, double yOffset) {
            return new Location(world,
                    (minX() + maxX()) / 2.0 + 0.5,
                    minY() + yOffset,
                    (minZ() + maxZ()) / 2.0 + 0.5);
        }

        boolean contains(Location location) {
            if (location == null) return false;
            double x = location.getX(), y = location.getY(), z = location.getZ();
            return x >= minX() && x <= maxX() + 1
                    && y >= minY() && y <= maxY() + 1
                    && z >= minZ() && z <= maxZ() + 1;
        }
    }

    private record CoinVisual(int value, Material material, String modelId) { }
    private record PaymentVisual(int value, Material material, String modelId) { }

    private record EnvironmentAreaSession(UUID ownerId,
                                          World world,
                                          Map<Integer, CuboidTemplate> buildingTemplates,
                                          List<Entity> holograms,
                                          int originX,
                                          int originY,
                                          int originZ,
                                          WorldCuboid border) {
        private EnvironmentAreaSession(UUID ownerId, World world, Map<Integer, CuboidTemplate> buildingTemplates,
                                       int originX, int originY, int originZ, WorldCuboid border) {
            this(ownerId, world, buildingTemplates, new ArrayList<>(), originX, originY, originZ, border);
        }

        private void removeHolograms() {
            for (Entity hologram : holograms) {
                if (hologram != null && !hologram.isDead()) {
                    hologram.remove();
                }
            }
            holograms.clear();
        }
    }

    private SlotOffset slotOffsetFor(UUID ownerId) {
        int hash = Math.abs(ownerId.hashCode());
        int col = hash % 8;
        int row = (hash / 8) % 8;
        return new SlotOffset(col * AREA_SPACING_BLOCKS, row * AREA_SPACING_BLOCKS);
    }

    private record SlotOffset(int dx, int dz) {}
    private record PendingBuildAction(String tag, int slot) {}

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) {
            return;
        }
        shutdown();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        EnvironmentAreaSession session = sessions.remove(id);
        lastValidLocations.remove(id);
        if (session == null) {
            return;
        }
        removeAnimatedLeaderboard(id);
        session.removeHolograms();
        Bukkit.unloadWorld(session.world(), false);
        deleteWorldFolder(session.world().getName());
    }

    public void shutdown() {
        if (hologramRefreshTask != null) {
            hologramRefreshTask.cancel();
            hologramRefreshTask = null;
        }
        for (BukkitTask task : new ArrayList<>(activeBuildTasks.values())) {
            if (task != null) {
                task.cancel();
            }
        }
        activeBuildTasks.clear();
        for (EnvironmentAreaSession session : new ArrayList<>(sessions.values())) {
            if (session != null) {
                session.removeHolograms();
                    }
        }
        for (AnimatedLeaderboard board : new ArrayList<>(animatedLeaderboardsByOwner.values())) {
            if (board != null) {
                board.remove();
            }
        }
        animatedLeaderboardsByOwner.clear();
        cleanupEnvironmentAreaCitizensClones();
        sessions.clear();
        lastValidLocations.clear();
        lastHologramLinesByTag.clear();
    }

    private void cleanupEnvironmentAreaCitizensClones() {
        int removed = 0;
        for (net.citizensnpcs.api.npc.NPC npc : CitizensAPI.getNPCRegistry()) {
            if (!npc.data().get(ENV_AREA_CLONE_KEY, false)) {
                continue;
            }
            CitizensAPI.getNPCRegistry().deregister(npc);
            removed++;
        }
        if (removed > 0) {
            plugin.getLogger().info("[EnvironmentArea] Cleaned up " + removed + " session Citizens clones.");
        }
    }
}
