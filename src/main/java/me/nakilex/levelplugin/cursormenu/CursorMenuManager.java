package me.nakilex.levelplugin.cursormenu;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import me.clip.placeholderapi.PlaceholderAPI;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cursormenu.model.MenuActor;
import me.nakilex.levelplugin.cursormenu.model.ItemPreset;
import me.nakilex.levelplugin.cursormenu.model.MenuButton;
import me.nakilex.levelplugin.cursormenu.model.MenuSection;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CursorMenuManager implements Listener {
    private final Main plugin;

    private final Map<String, MenuSection> sections = new LinkedHashMap<>();
    private final Map<String, ItemPreset> itemPresets = new LinkedHashMap<>();
    private final Map<UUID, MenuSession> activeSessions = new HashMap<>();
    private final Map<UUID, PreviewSession> activePreviews = new HashMap<>();
    private final Set<String> allowedCommands = new HashSet<>();
    private final Map<UUID, BukkitTask> soundLoopTasks = new HashMap<>();
    private final Map<UUID, List<BlockState>> temporarilyClearedBlocks = new HashMap<>();

    private CursorConfig config = CursorConfig.defaults();
    private BukkitTask tickTask;

    public CursorMenuManager(Main plugin) {
        this.plugin = plugin;
        ensureDefaultFiles();
        reload();
    }

    public void reload() {
        stopAllMenus();
        stopAllPreviews();
        loadConfig();
        loadAllowedCommands();
        loadItems();
        loadMenus();
        restartTickTask();
    }

    public Set<String> getMenuKeys() { return Collections.unmodifiableSet(sections.keySet()); }
    public Set<String> getItemPresetKeys() { return Collections.unmodifiableSet(itemPresets.keySet()); }

    public boolean runMenu(Player player, String menuKey) {
        MenuSession existing = activeSessions.get(player.getUniqueId());
        Location preservedReturn = existing != null && existing.returnLocation != null
                ? existing.returnLocation.clone()
                : player.getLocation().clone();
        GameMode preservedGameMode = existing != null ? existing.originalGameMode : player.getGameMode();
        return runMenu(player, menuKey, preservedReturn, preservedGameMode);
    }

    private boolean runMenu(Player player, String menuKey, Location returnLocation, GameMode originalMode) {
        MenuSection section = sections.get(menuKey.toLowerCase(Locale.ROOT));
        if (section == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    ChatColor.RED + "Unknown cursor menu '" + menuKey + "'.");
            return false;
        }
        stopMenu(player, false);

        Location camera = section.camera().clone();
        MenuSession session = new MenuSession(menuKey.toLowerCase(Locale.ROOT),
                returnLocation, originalMode, camera);

        if (player.teleport(camera)) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        enterCameraMode(player, session, camera);

        if (config.cameraBlockCheckEnabled()) {
            clearCameraObstructions(player, camera);
        }

        session.cursorAnchor = spawnCursorAnchor(camera);
        session.cursorDisplay = spawnCursorDisplay(camera);
        for (MenuActor actor : section.actors()) {
            SpawnedActor spawned = spawnMenuActor(player, section, actor);
            if (spawned != null) {
                session.actors.add(spawned);
            }
        }
        for (MenuButton button : section.buttons()) {
            TextDisplay display = spawnButtonDisplay(section, button);
            if (display != null) {
                session.buttons.add(new ButtonState(button, display));
            }
        }

        activeSessions.put(player.getUniqueId(), session);
        runAutoCommands(player, section);
        startMenuSound(player);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                ChatColor.GREEN + "Opened cursor menu: " + ChatColor.WHITE + menuKey);
        return true;
    }

    public boolean stopMenu(Player player, boolean teleportBack) {
        MenuSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return false;

        stopMenuSound(player);
        restoreCameraObstructions(player);
        cleanupSession(session);
        exitCameraMode(player, session);

        player.setGameMode(session.originalGameMode);

        if (teleportBack && session.returnLocation != null && player.isOnline()) {
            player.teleport(session.returnLocation);
        }
        return true;
    }

    public boolean showItemPreview(Player player, String itemId) {
        ItemPreset preset = itemPresets.get(itemId.toLowerCase(Locale.ROOT));
        if (preset == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    ChatColor.RED + "Unknown cursor item id '" + itemId + "'.");
            return false;
        }
        hideItemPreview(player);
        ItemDisplay display = spawnItemPreview(player, preset);
        if (display == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    ChatColor.RED + "Failed to create item preview.");
            return false;
        }
        activePreviews.put(player.getUniqueId(), new PreviewSession(preset, display));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                ChatColor.GREEN + "Showing cursor item: " + ChatColor.WHITE + itemId);
        return true;
    }

    public boolean hideItemPreview(Player player) {
        PreviewSession session = activePreviews.remove(player.getUniqueId());
        if (session == null) return false;
        if (session.display != null && !session.display.isDead()) {
            session.display.remove();
        }
        return true;
    }

    public void stopAllMenus() {
        for (UUID id : new ArrayList<>(activeSessions.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                stopMenu(player, true);
                continue;
            }
            MenuSession session = activeSessions.remove(id);
            if (session != null) {
                cleanupSession(session);
                if (session.cameraSeat != null && !session.cameraSeat.isDead()) {
                    session.cameraSeat.remove();
                }
            }
        }
    }

    public void stopAllPreviews() {
        for (PreviewSession session : new ArrayList<>(activePreviews.values())) {
            if (session.display != null && !session.display.isDead()) session.display.remove();
        }
        activePreviews.clear();
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (BukkitTask task : soundLoopTasks.values()) {
            task.cancel();
        }
        soundLoopTasks.clear();
        stopAllMenus();
        stopAllPreviews();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK
                && action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        MenuSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;
        event.setCancelled(true);

        ButtonState hovered = getHoveredButton(session);
        if (hovered == null) return;
        executeButtonAction(player, session, hovered.button);
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (activeSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!activeSessions.containsKey(player.getUniqueId())) {
            return;
        }
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        String command = message.substring(1).split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (allowedCommands.contains(command)) {
            return;
        }
        event.setCancelled(true);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                ChatColor.RED + "You cannot use that command while in menu mode.");
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { stopMenu(event.getPlayer(), true); hideItemPreview(event.getPlayer()); }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.joinRunEnabled() || config.joinRunMenu() == null || config.joinRunMenu().isBlank()) {
            return;
        }
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            runMenu(player, config.joinRunMenu());
            for (String command : config.joinRunCommands()) {
                dispatchButtonCommand(player, command);
            }
        }, Math.max(0, config.joinRunDelay()));
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!config.creatureSpawnProtectionEnabled()) {
            return;
        }
        Location spawn = event.getLocation();
        for (MenuSection section : sections.values()) {
            Location camera = section.camera();
            if (!camera.getWorld().getUID().equals(spawn.getWorld().getUID())) {
                continue;
            }
            if (camera.distanceSquared(spawn) <= config.creatureSpawnProtectionRadius() * config.creatureSpawnProtectionRadius()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            stopMenu(event.getPlayer(), true);
        }
    }

    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { stopMenu(event.getPlayer(), true); hideItemPreview(event.getPlayer()); }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        MenuSession session = activeSessions.get(event.getPlayer().getUniqueId());
        if (session == null || event.getTo() == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        Location locked = session.camera.clone();
        locked.setYaw(to.getYaw());
        locked.setPitch(to.getPitch());
        boolean movedPosition = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();
        if (movedPosition) {
            event.setTo(locked);
        }
    }

    private void cleanupSession(MenuSession session) {
        for (SpawnedActor actor : session.actors) {
            actor.destroy();
        }
        session.actors.clear();
        if (session.cursorAnchor != null && !session.cursorAnchor.isDead()) session.cursorAnchor.remove();
        if (session.cursorDisplay != null && !session.cursorDisplay.isDead()) session.cursorDisplay.remove();
        for (ButtonState button : session.buttons) {
            if (button.display != null && !button.display.isDead()) button.display.remove();
        }
    }

    private void enterCameraMode(Player player, MenuSession session, Location camera) {
        session.originalHelmet = cloneOrNull(player.getInventory().getHelmet());
        session.originalMainHand = cloneOrNull(player.getInventory().getItemInMainHand());
        session.originalOffHand = cloneOrNull(player.getInventory().getItemInOffHand());
        session.originalHeldSlot = player.getInventory().getHeldItemSlot();
        session.originalInvisible = player.isInvisible();
        session.originalCollidable = player.isCollidable();
        session.originalInvulnerable = player.isInvulnerable();
        session.originalAllowFlight = player.getAllowFlight();
        session.originalFlying = player.isFlying();
        session.originalWalkSpeed = player.getWalkSpeed();
        session.originalFlySpeed = player.getFlySpeed();

        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        player.updateInventory();
        if (player.getVehicle() != null) {
            player.leaveVehicle();
        }
        Pig seat = camera.getWorld().spawn(camera, Pig.class, pig -> {
            pig.setAI(false);
            pig.setInvisible(true);
            pig.setInvulnerable(true);
            pig.setGravity(false);
            pig.setSilent(true);
            pig.setCollidable(false);
            pig.setPersistent(false);
            pig.setAdult();
            pig.setRotation(camera.getYaw(), camera.getPitch());
        });
        player.setInvisible(true);
        player.setCollidable(false);
        player.setInvulnerable(true);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
        player.setFallDistance(0.0f);
        seat.addPassenger(player);
        session.cameraSeat = seat;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && session.cameraSeat != null && !session.cameraSeat.isDead()) {
                sendCameraPacket(player, session.cameraSeat);
            }
        });
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && session.cameraSeat != null && !session.cameraSeat.isDead()) {
                sendCameraPacket(player, session.cameraSeat);
            }
        }, 3L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && session.cameraSeat != null && !session.cameraSeat.isDead()) {
                sendCameraPacket(player, session.cameraSeat);
            }
        }, 10L);
    }

    private void exitCameraMode(Player player, MenuSession session) {
        sendCameraPacket(player, player);
        if (player.getVehicle() != null) {
            player.leaveVehicle();
        }
        if (session.cameraSeat != null && !session.cameraSeat.isDead()) {
            session.cameraSeat.remove();
        }
        player.setInvisible(session.originalInvisible);
        player.setCollidable(session.originalCollidable);
        player.setInvulnerable(session.originalInvulnerable);
        player.setAllowFlight(session.originalAllowFlight);
        player.setFlying(session.originalFlying);
        player.setWalkSpeed(session.originalWalkSpeed);
        player.setFlySpeed(session.originalFlySpeed);
        player.setFallDistance(0.0f);
        player.getInventory().setHelmet(cloneOrNull(session.originalHelmet));
        player.getInventory().setItemInMainHand(cloneOrNull(session.originalMainHand));
        player.getInventory().setItemInOffHand(cloneOrNull(session.originalOffHand));
        player.getInventory().setHeldItemSlot(session.originalHeldSlot);
        player.updateInventory();
    }

    private void sendCameraPacket(Player player, Entity entity) {
        try {
            WrapperPlayServerCamera packet = new WrapperPlayServerCamera(entity.getEntityId());
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Exception e) {
            plugin.getLogger().warning("[CursorMenu] Failed to send camera packet: " + e.getMessage());
        }
    }

    private ItemStack cloneOrNull(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }

    private void dispatchButtonCommand(Player player, String rawCommand) {
        if (rawCommand == null || rawCommand.isBlank()) return;
        String resolved = applyPlaceholders(player, rawCommand.trim());
        if (resolved.startsWith("[console]")) {
            String cmd = resolved.substring("[console]".length()).trim();
            if (!cmd.isEmpty()) plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            return;
        }
        if (resolved.startsWith("[player]")) {
            String cmd = resolved.substring("[player]".length()).trim();
            if (!cmd.isEmpty()) player.performCommand(cmd);
            return;
        }
        if (resolved.startsWith("[op]")) {
            String cmd = resolved.substring("[op]".length()).trim();
            if (cmd.isEmpty()) return;
            boolean wasOp = player.isOp();
            try {
                if (!wasOp) player.setOp(true);
                player.performCommand(cmd);
            } finally {
                if (!wasOp) player.setOp(false);
            }
            return;
        }
        if (resolved.startsWith("[server]")) {
            String targetServer = resolved.substring("[server]".length()).trim();
            if (!targetServer.isEmpty()) {
                player.performCommand("connect " + targetServer);
            }
            return;
        }
        player.performCommand(resolved.startsWith("/") ? resolved.substring(1) : resolved);
    }

    private void executeButtonAction(Player player, MenuSession session, MenuButton button) {
        MenuSection section = sections.get(session.menuKey);
        if (section == null) {
            return;
        }

        if (section.permission() != null && !section.permission().isBlank() && !player.hasPermission(section.permission())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, ChatColor.RED + "You do not have permission for this menu.");
            return;
        }
        if (button.permission() != null && !button.permission().isBlank() && !player.hasPermission(button.permission())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, ChatColor.RED + "You do not have permission to use this option.");
            return;
        }
        if (!evaluateCondition(player, button)) {
            return;
        }

        String randomCommand = pickRandomCommand(button);
        List<String> commandsToRun = randomCommand != null ? Collections.singletonList(randomCommand) : button.commands();

        int delay = Math.max(0, button.commandDelay());
        Runnable commandTask = () -> {
            for (String command : commandsToRun) {
                dispatchButtonCommand(player, command);
            }
            handlePostCommandFlow(player, session, button);
        };

        if (delay > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, commandTask, delay);
        } else {
            commandTask.run();
        }
    }

    private void handlePostCommandFlow(Player player, MenuSession session, MenuButton button) {
        if (button.nextMenuEnabled() && button.nextMenuKey() != null && !button.nextMenuKey().isBlank()) {
            if (button.nextMenuKey().equalsIgnoreCase(session.menuKey)) {
                plugin.getLogger().warning("[CursorMenu] Button '" + button.id() + "' points next-menu to the current menu; skipping transition.");
            } else {
            MenuSection next = sections.get(button.nextMenuKey().toLowerCase(Locale.ROOT));
            if (next == null) {
                plugin.getLogger().warning("[CursorMenu] Unknown next menu '" + button.nextMenuKey() + "' from button '" + button.id() + "'.");
            } else if (next.permission() != null && !next.permission().isBlank() && !player.hasPermission(next.permission())) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, ChatColor.RED + "You do not have permission for the next menu.");
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> runMenu(player, button.nextMenuKey()));
                return;
            }
            }
        }

        boolean shouldStop = button.stopMenuEnabled() || button.closeMenu();
        if (!shouldStop) {
            return;
        }
        stopMenu(player, !button.teleportEnabled() || button.teleportBackOriginal());
        if (button.teleportEnabled() && !button.teleportBackOriginal()) {
            teleportButtonTarget(player, button);
        }
    }

    private void teleportButtonTarget(Player player, MenuButton button) {
        if (button.teleportWorld() == null || button.teleportWorld().isBlank()) {
            plugin.getLogger().warning("[CursorMenu] Button '" + button.id() + "' has teleport enabled but no world configured.");
            return;
        }
        World world = Bukkit.getWorld(button.teleportWorld());
        if (world == null) {
            plugin.getLogger().warning("[CursorMenu] Button '" + button.id() + "' references unknown teleport world '" + button.teleportWorld() + "'.");
            return;
        }
        if (button.teleportX() == null || button.teleportY() == null || button.teleportZ() == null) {
            plugin.getLogger().warning("[CursorMenu] Button '" + button.id() + "' uses partial teleport coordinates; missing values fall back to player position.");
        }
        double x = button.teleportX() == null ? player.getLocation().getX() : button.teleportX();
        double y = button.teleportY() == null ? player.getLocation().getY() : button.teleportY();
        double z = button.teleportZ() == null ? player.getLocation().getZ() : button.teleportZ();
        player.teleport(new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch()));
    }

    private void runAutoCommands(Player player, MenuSection section) {
        if (!section.autoCommandsEnabled() || section.autoCommands() == null || section.autoCommands().isEmpty()) {
            return;
        }
        for (int i = 0; i < section.autoCommands().size(); i++) {
            String command = section.autoCommands().get(i);
            int delay = 0;
            if (section.autoCommandDelays() != null && i < section.autoCommandDelays().size()) {
                delay = Math.max(0, section.autoCommandDelays().get(i));
            }
            Runnable run = () -> dispatchButtonCommand(player, command);
            if (delay > 0) {
                plugin.getServer().getScheduler().runTaskLater(plugin, run, delay);
            } else {
                run.run();
            }
        }
    }

    private void clearCameraObstructions(Player player, Location camera) {
        int radius = config.cameraBlockCheckRadius();
        if (radius <= 0) {
            return;
        }
        List<BlockState> removed = new ArrayList<>();
        World world = camera.getWorld();
        int baseX = camera.getBlockX();
        int baseY = camera.getBlockY();
        int baseZ = camera.getBlockZ();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                    if (block.getType().isAir() || block.getType() == Material.BARRIER) {
                        continue;
                    }
                    removed.add(block.getState());
                    block.setType(Material.AIR, false);
                }
            }
        }
        temporarilyClearedBlocks.put(player.getUniqueId(), removed);
    }

    private void restoreCameraObstructions(Player player) {
        List<BlockState> states = temporarilyClearedBlocks.remove(player.getUniqueId());
        if (states == null || states.isEmpty()) {
            return;
        }
        for (BlockState state : states) {
            if (state == null || state.getWorld() == null) {
                continue;
            }
            state.update(true, false);
        }
    }

    private String applyPlaceholders(Player player, String input) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, input);
        }
        return input;
    }

    private boolean evaluateCondition(Player player, MenuButton button) {
        String variable = button.conditionVariable();
        String operator = button.conditionOperator();
        String expected = button.conditionValue();
        if (variable == null || variable.isBlank() || operator == null || operator.isBlank()) {
            return true;
        }

        String left = applyPlaceholders(player, variable).trim();
        String right = applyPlaceholders(player, expected == null ? "" : expected).trim();

        Double leftNum = tryParseDouble(left);
        Double rightNum = tryParseDouble(right);
        if (leftNum != null && rightNum != null) {
            return switch (operator) {
                case ">" -> leftNum > rightNum;
                case ">=" -> leftNum >= rightNum;
                case "<" -> leftNum < rightNum;
                case "<=" -> leftNum <= rightNum;
                case "==" -> Double.compare(leftNum, rightNum) == 0;
                case "!=" -> Double.compare(leftNum, rightNum) != 0;
                default -> {
                    plugin.getLogger().warning("[CursorMenu] Invalid numeric condition operator '" + operator
                            + "' for button '" + button.id() + "'.");
                    yield false;
                }
            };
        }

        return switch (operator) {
            case "==" -> left.equals(right);
            case "!=" -> !left.equals(right);
            default -> {
                plugin.getLogger().warning("[CursorMenu] Operator '" + operator + "' requires numeric values for button '" + button.id() + "'.");
                yield false;
            }
        };
    }

    private String pickRandomCommand(MenuButton button) {
        if (button.randomCommands() == null || button.randomCommands().isEmpty()) {
            return null;
        }
        if (button.randomChances() == null || button.randomChances().isEmpty()) {
            plugin.getLogger().warning("[CursorMenu] random-commands present but random-chances missing for button '" + button.id() + "'.");
            return null;
        }
        if (button.randomCommands().size() != button.randomChances().size()) {
            plugin.getLogger().warning("[CursorMenu] random-commands and random-chances size mismatch for button '" + button.id() + "'.");
            return null;
        }

        double total = 0.0;
        for (Double chance : button.randomChances()) {
            if (chance == null || chance <= 0.0) {
                continue;
            }
            total += chance;
        }
        if (total <= 0.0) {
            plugin.getLogger().warning("[CursorMenu] random-chances total is invalid for button '" + button.id() + "'.");
            return null;
        }

        double roll = ThreadLocalRandom.current().nextDouble(total);
        double progress = 0.0;
        for (int i = 0; i < button.randomCommands().size(); i++) {
            double chance = button.randomChances().get(i) == null ? 0.0 : button.randomChances().get(i);
            if (chance <= 0.0) {
                continue;
            }
            progress += chance;
            if (roll <= progress) {
                return button.randomCommands().get(i);
            }
        }
        return button.randomCommands().get(button.randomCommands().size() - 1);
    }

    private Double tryParseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private ButtonState getHoveredButton(MenuSession session) {
        if (session.cursorAnchor == null || session.cursorAnchor.isDead()) return null;
        Location cursor = session.cursorAnchor.getLocation();
        double bestDistance = 1.1;
        ButtonState best = null;
        for (ButtonState button : session.buttons) {
            if (button.display == null || button.display.isDead()) continue;
            double distance = button.display.getLocation().distance(cursor);
            double hoverRadius = computeHoverRadius(button.button);
            if (distance <= hoverRadius && distance <= bestDistance) {
                bestDistance = distance;
                best = button;
            }
        }
        return best;
    }

    private double computeHoverRadius(MenuButton button) {
        String plainText = ChatColor.stripColor(colorize(button.text()));
        int textLength = plainText == null ? 0 : plainText.length();
        double base = 0.55 + (textLength * 0.03 * Math.max(0.8, button.scale()));
        return clamp(base, 0.75, 2.75);
    }

    private void tick() {
        tickMenus();
        tickItemPreviews();
    }

    private void tickMenus() {
        Iterator<Map.Entry<UUID, MenuSession>> iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MenuSession> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            MenuSession session = entry.getValue();
            if (player == null || !player.isOnline()) {
                cleanupSession(session);
                iterator.remove();
                continue;
            }
            MenuSection section = sections.get(session.menuKey);
            if (section == null) {
                cleanupSession(session);
                iterator.remove();
                continue;
            }

            Location cursorLocation = computeCursorLocation(player, session.camera, section.distance());
            if (session.cursorAnchor != null && !session.cursorAnchor.isDead()) session.cursorAnchor.teleport(cursorLocation);
            if (session.cursorDisplay != null && !session.cursorDisplay.isDead()) session.cursorDisplay.teleport(cursorLocation);
            if (session.cameraSeat != null && !session.cameraSeat.isDead()) {
                Location locked = session.camera.clone();
                locked.setYaw(session.camera.getYaw());
                locked.setPitch(session.camera.getPitch());
                session.cameraSeat.teleport(locked);

                session.cameraRefreshTicks++;
                if (session.cameraRefreshTicks % 10 == 0) {
                    sendCameraPacket(player, session.cameraSeat);
                }
            }
            player.setFallDistance(0.0f);
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            if (!player.isFlying()) {
                player.setFlying(true);
            }

            ButtonState hovered = getHoveredButton(session);
            for (ButtonState button : session.buttons) {
                if (button.display == null || button.display.isDead()) continue;
                button.display.setGlowing(button == hovered);
            }
            for (SpawnedActor actor : session.actors) {
                actor.syncLocation(resolveActorLocation(section, actor.definition));
            }
        }
    }

    private void tickItemPreviews() {
        Iterator<Map.Entry<UUID, PreviewSession>> iterator = activePreviews.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PreviewSession> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            PreviewSession session = entry.getValue();
            if (player == null || !player.isOnline() || session.display == null || session.display.isDead()) {
                if (session.display != null && !session.display.isDead()) session.display.remove();
                iterator.remove();
                continue;
            }
            Location target = player.getEyeLocation().add(player.getLocation().getDirection().multiply(session.preset.distance()));
            target.add(session.preset.offsetX(), session.preset.offsetY(), session.preset.offsetZ());
            target.setYaw(player.getLocation().getYaw());
            target.setPitch(player.getLocation().getPitch());
            session.display.teleport(target);

            if (session.preset.rotateEnabled() && session.preset.rotateSpeed() > 0.0f) {
                session.rotation += session.preset.rotateSpeed();
                session.display.setTransformation(new Transformation(
                        new Vector3f(),
                        new AxisAngle4f((float) Math.toRadians(session.rotation), 0f, 1f, 0f),
                        new Vector3f(session.preset.scale(), session.preset.scale(), session.preset.scale()),
                        new AxisAngle4f()));
            }
        }
    }

    private Location computeCursorLocation(Player player, Location camera, double distance) {
        float dyaw = normalizeYaw(player.getLocation().getYaw() - camera.getYaw());
        float dpitch = player.getLocation().getPitch() - camera.getPitch();
        double x = clamp((dyaw / config.maxYaw) * config.maxX, -config.maxX, config.maxX);
        double y = clamp((-dpitch / config.maxPitch) * config.maxY, -config.maxY, config.maxY);

        Vector forward = camera.getDirection().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = new Vector(0, 1, 0);
        return camera.clone().add(forward.multiply(distance)).add(right.multiply(x)).add(up.multiply(y));
    }

    private ArmorStand spawnCursorAnchor(Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setMarker(true);
            stand.setInvisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSmall(true);
        });
    }

    private ItemDisplay spawnCursorDisplay(Location location) {
        Material material = Material.matchMaterial(config.cursorMaterial);
        if (material == null) material = Material.NETHER_STAR;
        final Material cursorMaterial = material;
        return location.getWorld().spawn(location, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(cursorMaterial));
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformation(new Transformation(
                    new Vector3f((float) config.cursorOffsetX, (float) config.cursorOffsetY, 0f),
                    new AxisAngle4f(),
                    new Vector3f((float) config.cursorScale, (float) config.cursorScale, (float) config.cursorScale),
                    new AxisAngle4f()));
        });
    }

    private TextDisplay spawnButtonDisplay(MenuSection section, MenuButton button) {
        Location loc = resolveScreenPosition(section.camera(), section.distance(), button.x(), button.y(), button.z());
        return loc.getWorld().spawn(loc, TextDisplay.class, display -> {
            Quaternionf tiltRotation = new Quaternionf()
                    .rotateXYZ(
                            (float) Math.toRadians(button.tiltX()),
                            (float) Math.toRadians(button.tiltY()),
                            (float) Math.toRadians(button.tiltZ())
                    );
            display.text(net.kyori.adventure.text.Component.text(colorize(button.text())));
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(true);
            display.setDefaultBackground(false);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    tiltRotation,
                    new Vector3f((float) button.scale(), (float) button.scale(), (float) button.scale()),
                    new Quaternionf()));
        });
    }

    private ItemDisplay spawnItemPreview(Player player, ItemPreset preset) {
        Material material = Material.matchMaterial(preset.material());
        if (material == null) {
            plugin.getLogger().warning("[CursorMenu] Invalid item material '" + preset.material() + "' in item preview preset.");
            return null;
        }
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(preset.distance()));
        ItemStack stack = new ItemStack(material);
        if (preset.customModelData() != null) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(preset.customModelData());
                stack.setItemMeta(meta);
            }
        }
        return loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
            display.setItemStack(stack);
            display.setBillboard(Display.Billboard.FIXED);
            display.setInterpolationDuration(2);
            display.setGlowing(preset.glowEnabled());
            display.setTransformation(new Transformation(
                    new Vector3f((float) preset.offsetX(), (float) preset.offsetY(), (float) preset.offsetZ()),
                    new AxisAngle4f(),
                    new Vector3f(preset.scale(), preset.scale(), preset.scale()),
                    new AxisAngle4f()));
        });
    }

    private Location resolveScreenPosition(Location camera, double distance, double x, double y, double z) {
        Vector forward = camera.getDirection().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = new Vector(0, 1, 0);
        return camera.clone().add(forward.multiply(distance + z)).add(right.multiply(x)).add(up.multiply(y));
    }

    private SpawnedActor spawnMenuActor(Player viewer, MenuSection section, MenuActor actor) {
        if (!"citizens-player".equalsIgnoreCase(actor.type())) {
            plugin.getLogger().warning("[CursorMenu] Unsupported actor type '" + actor.type()
                    + "' for actor '" + actor.id() + "' in menu '" + section.key() + "'.");
            return null;
        }
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
            plugin.getLogger().warning("[CursorMenu] Citizens is required for actor '" + actor.id()
                    + "' in menu '" + section.key() + "'.");
            return null;
        }

        Location spawn = resolveActorLocation(section, actor);
        String actorName = resolveActorName(viewer, actor);
        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, actorName);
        npc.setProtected(true);
        npc.data().setPersistent(NPC.Metadata.COLLIDABLE, false);
        npc.data().setPersistent(NPC.Metadata.TARGETABLE, false);
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false);
        npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_PLAYERLIST, true);
        npc.data().setPersistent(NPC.Metadata.REMOVE_FROM_TABLIST, true);
        applyActorSkin(viewer, actor, npc);
        if (!npc.spawn(spawn)) {
            npc.destroy();
            plugin.getLogger().warning("[CursorMenu] Failed to spawn actor '" + actor.id()
                    + "' in menu '" + section.key() + "'.");
            return null;
        }

        Entity entity = npc.getEntity();
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        if (entity instanceof LivingEntity living) {
            living.setAI(false);
            living.setInvulnerable(true);
            living.setSilent(true);
            living.setCollidable(false);
            living.setGravity(false);
        }
        entity.setPersistent(false);
        return new SpawnedActor(actor, npc);
    }

    private Location resolveActorLocation(MenuSection section, MenuActor actor) {
        Location spawn = resolveScreenPosition(section.camera(), section.distance(), actor.x(), actor.y(), actor.z());
        if (actor.lookAtCamera()) {
            Vector towardCamera = section.camera().toVector().subtract(spawn.toVector());
            if (towardCamera.lengthSquared() > 0.0001) {
                spawn.setDirection(towardCamera);
            }
        } else {
            spawn.setYaw(section.camera().getYaw() + actor.yaw());
            spawn.setPitch(section.camera().getPitch() + actor.pitch());
        }
        return spawn;
    }

    private String resolveActorName(Player viewer, MenuActor actor) {
        String rawName = actor.name();
        if (rawName == null || rawName.isBlank()) {
            rawName = "Profile";
        }
        String withPlaceholders = PlaceholderAPI.setPlaceholders(viewer, rawName);
        return colorize(withPlaceholders);
    }

    private void applyActorSkin(Player viewer, MenuActor actor, NPC npc) {
        if (!actor.useViewerSkin()) {
            return;
        }
        try {
            SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
            skinTrait.setSkinPersistent(viewer);
        } catch (Exception ex) {
            plugin.getLogger().warning("[CursorMenu] Failed to apply viewer skin to actor '" + actor.id()
                    + "': " + ex.getMessage());
        }
    }

    private void ensureDefaultFiles() {
        saveIfMissing("cursormenu.yml");
        saveIfMissing("cursormenu-items.yml");
        File menuFolder = new File(plugin.getDataFolder(), "cursormenu/menu");
        if (!menuFolder.exists()) menuFolder.mkdirs();
        saveIfMissing("cursormenu/menu/example.yml");
        saveIfMissing("cursormenu/menu/profile_selection.yml");
    }

    private void saveIfMissing(String path) {
        File target = new File(plugin.getDataFolder(), path);
        if (!target.exists()) plugin.saveResource(path, false);
    }

    private void loadConfig() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "cursormenu.yml"));
        config = new CursorConfig(
                yaml.getString("cursor.material", "NETHER_STAR"),
                yaml.getDouble("cursor.scale", 0.35),
                yaml.getDouble("cursor.offset-x", 0.0),
                yaml.getDouble("cursor.offset-y", 0.0),
                yaml.getDouble("movement.max-x", 2.2),
                yaml.getDouble("movement.max-y", 1.2),
                Math.max(5.0, yaml.getDouble("movement.max-yaw", 45.0)),
                Math.max(5.0, yaml.getDouble("movement.max-pitch", 30.0)),
                yaml.getString("sound.name"),
                (float) yaml.getDouble("sound.volume", 1.0),
                (float) yaml.getDouble("sound.pitch", 1.0),
                yaml.getBoolean("sound.loop.enabled", false),
                Math.max(1, yaml.getInt("sound.loop.duration", 40)),
                yaml.getBoolean("join-run.enabled", false),
                yaml.getLong("join-run.delay", 20L),
                yaml.getString("join-run.menu"),
                yaml.getStringList("join-run.commands"),
                yaml.getBoolean("creature-spawn-limits.enabled", false),
                yaml.getDouble("creature-spawn-limits.radius", 8.0),
                yaml.getBoolean("camera-block-check.enabled", false),
                Math.max(0, yaml.getInt("camera-block-check.radius", 1)),
                yaml.getBoolean("use-pumpkin-overlay", false)
        );
    }

    private void loadItems() {
        itemPresets.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "cursormenu-items.yml"));
        ConfigurationSection section = yaml.getConfigurationSection("items");
        ConfigurationSection legacySection = yaml.getConfigurationSection("display-items");
        if (section == null && legacySection == null) return;
        if (section != null) {
            parseItemSection(section);
        }
        if (legacySection != null) {
            parseItemSection(legacySection);
        }
    }

    private void parseItemSection(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(key);
            if (node == null) continue;

            ConfigurationSection offsetNode = node.getConfigurationSection("offset");
            double offsetX = offsetNode != null ? offsetNode.getDouble("x", node.getDouble("offset-x", 0.0))
                    : node.getDouble("offset-x", 0.0);
            double offsetY = offsetNode != null ? offsetNode.getDouble("y", node.getDouble("offset-y", 0.0))
                    : node.getDouble("offset-y", 0.0);
            double offsetZ = offsetNode != null ? offsetNode.getDouble("z", node.getDouble("offset-z", 0.0))
                    : node.getDouble("offset-z", 0.0);

            boolean rotateEnabled = node.contains("rotate") ? node.getBoolean("rotate", false)
                    : node.getDouble("rotate-speed", 0.0) > 0.0;
            ItemPreset preset = new ItemPreset(
                    node.getString("material", "STONE"),
                    node.contains("custom-model-data") ? node.getInt("custom-model-data") : null,
                    (float) node.getDouble("scale", 1.0),
                    node.getDouble("distance", 2.0),
                    offsetX,
                    offsetY,
                    offsetZ,
                    node.getBoolean("glow", false),
                    rotateEnabled,
                    (float) node.getDouble("rotate-speed", 0.0)
            );
            if (Material.matchMaterial(preset.material()) == null) {
                plugin.getLogger().warning("[CursorMenu] Invalid material '" + preset.material() + "' in item preset '" + key + "'.");
            }
            itemPresets.put(key.toLowerCase(Locale.ROOT), preset);
        }
    }

    private void loadAllowedCommands() {
        allowedCommands.clear();
        saveIfMissing("cursormenu-commands.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "cursormenu-commands.yml"));
        List<String> commands = yaml.getStringList("allowed-commands");
        for (String command : commands) {
            if (command == null || command.isBlank()) continue;
            allowedCommands.add(command.toLowerCase(Locale.ROOT));
        }
        allowedCommands.add("cursormenu");
        allowedCommands.add("cmenu");
    }

    private void loadMenus() {
        sections.clear();
        File dir = new File(plugin.getDataFolder(), "cursormenu/menu");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if (yaml.contains("menus")) {
                loadNativeMenus(yaml, file.getName());
            } else {
                loadCustomScreenMenus(yaml, file.getName());
            }
        }
    }

    private void loadNativeMenus(YamlConfiguration yaml, String sourceName) {
        ConfigurationSection menus = yaml.getConfigurationSection("menus");
        if (menus == null) {
            plugin.getLogger().warning("[CursorMenu] Malformed native menu file: " + sourceName + " (missing menus section).");
            return;
        }
        for (String key : menus.getKeys(false)) {
            ConfigurationSection node = menus.getConfigurationSection(key);
            if (node == null) continue;
            Location camera = parseLocation(node.getConfigurationSection("camera"));
            if (camera == null) {
                plugin.getLogger().warning("[CursorMenu] Invalid camera for menu '" + key + "' in " + sourceName + ".");
                continue;
            }
            double distance = node.getDouble("distance", 4.0);
            List<MenuButton> buttons = parseNativeButtons(node.getConfigurationSection("buttons"));
            List<MenuActor> actors = parseActors(node.getConfigurationSection("actors"));
            MenuSection section = new MenuSection(
                    key.toLowerCase(Locale.ROOT),
                    camera,
                    distance,
                    node.getString("permission"),
                    false,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    actors,
                    buttons
            );
            sections.put(section.key(), section);
        }
    }

    private List<MenuButton> parseNativeButtons(ConfigurationSection buttonSection) {
        List<MenuButton> buttons = new ArrayList<>();
        if (buttonSection == null) {
            return buttons;
        }
        for (String bKey : buttonSection.getKeys(false)) {
            ConfigurationSection bNode = buttonSection.getConfigurationSection(bKey);
            if (bNode == null) continue;
            buttons.add(new MenuButton(
                    bKey,
                    bNode.getString("text", bKey),
                    bNode.getDouble("x", 0.0),
                    bNode.getDouble("y", 0.0),
                    bNode.getDouble("z", 0.0),
                    bNode.getDouble("scale", 1.0),
                    bNode.getStringList("commands"),
                    bNode.getInt("command-delay", 0),
                    bNode.getString("permission"),
                    0.0, 0.0, 0.0,
                    null, null, null,
                    false, null,
                    bNode.getBoolean("close-on-click", true),
                    false, true,
                    null, null, null, null,
                    Collections.emptyList(), Collections.emptyList(),
                    bNode.getBoolean("close-on-click", true)
            ));
        }
        return buttons;
    }

    private void loadCustomScreenMenus(YamlConfiguration yaml, String sourceName) {
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection menuNode = yaml.getConfigurationSection(key);
            if (menuNode == null) {
                continue;
            }
            ConfigurationSection cameraNode = menuNode.getConfigurationSection("camera-position");
            Location camera = parseLocation(cameraNode);
            if (camera == null) {
                plugin.getLogger().warning("[CursorMenu] Invalid camera-position for imported menu '" + key + "' in " + sourceName + ".");
                continue;
            }
            double distance = cameraNode != null ? cameraNode.getDouble("distance", menuNode.getDouble("distance", 4.0))
                    : menuNode.getDouble("distance", 4.0);

            ConfigurationSection autoNode = menuNode.getConfigurationSection("auto-commands");
            boolean autoEnabled = autoNode != null && autoNode.getBoolean("enabled", false);
            List<String> autoCommands = autoNode != null ? autoNode.getStringList("commands") : Collections.emptyList();
            List<Integer> autoDelays = autoNode != null ? parseIntegerList(autoNode.getList("delays")) : Collections.emptyList();

            List<MenuButton> buttons = parseCustomButtons(menuNode.getConfigurationSection("layout"), key, sourceName);
            List<MenuActor> actors = parseActors(menuNode.getConfigurationSection("actors"));
            MenuSection section = new MenuSection(
                    key.toLowerCase(Locale.ROOT),
                    camera,
                    distance,
                    menuNode.getString("permission"),
                    autoEnabled,
                    autoCommands,
                    autoDelays,
                    actors,
                    buttons
            );
            sections.put(section.key(), section);
        }
    }

    private List<MenuActor> parseActors(ConfigurationSection actorSection) {
        List<MenuActor> actors = new ArrayList<>();
        if (actorSection == null) {
            return actors;
        }
        for (String id : actorSection.getKeys(false)) {
            ConfigurationSection node = actorSection.getConfigurationSection(id);
            if (node == null) {
                continue;
            }
            actors.add(new MenuActor(
                    id,
                    node.getString("type", "citizens-player"),
                    node.getString("name", id),
                    node.getBoolean("use-viewer-skin", true),
                    node.getBoolean("look-at-camera", false),
                    node.getDouble("x", 0.0),
                    node.getDouble("y", 0.0),
                    node.getDouble("z", 0.0),
                    (float) node.getDouble("yaw", 0.0),
                    (float) node.getDouble("pitch", 0.0)
            ));
        }
        return actors;
    }

    private List<MenuButton> parseCustomButtons(ConfigurationSection layoutSection, String menuKey, String sourceName) {
        List<MenuButton> buttons = new ArrayList<>();
        if (layoutSection == null) {
            return buttons;
        }
        for (String id : layoutSection.getKeys(false)) {
            ConfigurationSection node = layoutSection.getConfigurationSection(id);
            if (node == null) {
                continue;
            }
            ConfigurationSection tiltNode = node.getConfigurationSection("tilt");
            ConfigurationSection condNode = node.getConfigurationSection("condition");
            if (node.contains("condition") && condNode == null) {
                plugin.getLogger().warning("[CursorMenu] Button '" + id + "' in imported menu '" + menuKey + "' has malformed condition section.");
            }
            ConfigurationSection nextNode = node.getConfigurationSection("next-menu");
            ConfigurationSection stopNode = node.getConfigurationSection("stop-menu");
            ConfigurationSection tpNode = stopNode == null ? null : stopNode.getConfigurationSection("teleport");

            List<String> commandList = node.getStringList("command");
            if (commandList.isEmpty()) {
                String single = node.getString("command");
                if (single != null && !single.isBlank()) {
                    commandList = Collections.singletonList(single);
                }
            }

            buttons.add(new MenuButton(
                    id,
                    node.getString("name", id),
                    node.getDouble("x", 0.0),
                    node.getDouble("y", 0.0),
                    node.getDouble("z", 0.0),
                    node.getDouble("scale", 1.0),
                    commandList,
                    node.getInt("command-delay", 0),
                    node.getString("permission"),
                    tiltNode == null ? 0.0 : tiltNode.getDouble("x", 0.0),
                    tiltNode == null ? 0.0 : tiltNode.getDouble("y", 0.0),
                    tiltNode == null ? 0.0 : tiltNode.getDouble("z", 0.0),
                    condNode == null ? null : condNode.getString("variable"),
                    condNode == null ? null : condNode.getString("operator"),
                    condNode == null ? null : condNode.getString("value"),
                    nextNode != null && nextNode.getBoolean("enabled", false),
                    nextNode == null ? null : nextNode.getString("menu"),
                    stopNode != null && stopNode.getBoolean("enabled", false),
                    tpNode != null && tpNode.getBoolean("enabled", false),
                    tpNode != null && tpNode.getBoolean("back-original", true),
                    tpNode == null ? null : tpNode.getString("world"),
                    tpNode == null ? null : tpNode.getDouble("x"),
                    tpNode == null ? null : tpNode.getDouble("y"),
                    tpNode == null ? null : tpNode.getDouble("z"),
                    node.getStringList("random-commands"),
                    parseDoubleList(node.getList("random-chances")),
                    stopNode != null && stopNode.getBoolean("enabled", false)
            ));
            if (nextNode != null && nextNode.getBoolean("enabled", false)
                    && (nextNode.getString("menu") == null || nextNode.getString("menu").isBlank())) {
                plugin.getLogger().warning("[CursorMenu] Button '" + id + "' in imported menu '" + menuKey + "' enables next-menu without a target.");
            }
        }
        if (buttons.isEmpty()) {
            plugin.getLogger().warning("[CursorMenu] Imported menu '" + menuKey + "' in " + sourceName + " has no layout buttons.");
        }
        return buttons;
    }

    private Location parseLocation(ConfigurationSection section) {
        if (section == null) return null;
        World world = Bukkit.getWorld(section.getString("world", "world"));
        if (world == null) return null;
        return new Location(world,
                section.getDouble("x", 0.0),
                section.getDouble("y", 100.0),
                section.getDouble("z", 0.0),
                (float) section.getDouble("yaw", 0.0),
                (float) section.getDouble("pitch", 0.0));
    }

    private void restartTickTask() {
        if (tickTask != null) tickTask.cancel();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private List<Integer> parseIntegerList(List<?> rawList) {
        if (rawList == null) return Collections.emptyList();
        List<Integer> values = new ArrayList<>();
        for (Object raw : rawList) {
            if (raw instanceof Number number) {
                values.add(number.intValue());
            } else if (raw != null) {
                try {
                    values.add(Integer.parseInt(raw.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return values;
    }

    private List<Double> parseDoubleList(List<?> rawList) {
        if (rawList == null) return Collections.emptyList();
        List<Double> values = new ArrayList<>();
        for (Object raw : rawList) {
            if (raw instanceof Number number) {
                values.add(number.doubleValue());
            } else if (raw != null) {
                try {
                    values.add(Double.parseDouble(raw.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return values;
    }

    private static String colorize(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }

    private void startMenuSound(Player player) {
        stopMenuSound(player);
        if (config.soundName() == null || config.soundName().isBlank()) {
            return;
        }
        playMenuSound(player);
        if (config.soundLoopEnabled()) {
            BukkitTask loopTask = plugin.getServer().getScheduler().runTaskTimer(plugin,
                    () -> playMenuSound(player), config.soundLoopDuration(), config.soundLoopDuration());
            soundLoopTasks.put(player.getUniqueId(), loopTask);
        }
    }

    private void playMenuSound(Player player) {
        if (!player.isOnline() || config.soundName() == null || config.soundName().isBlank()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(config.soundName().toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, SoundCategory.MASTER, config.soundVolume(), config.soundPitch());
        } catch (IllegalArgumentException ignored) {
            player.playSound(player.getLocation(), config.soundName(), SoundCategory.MASTER, config.soundVolume(), config.soundPitch());
        }
    }

    private void stopMenuSound(Player player) {
        BukkitTask task = soundLoopTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        if (config.soundName() != null && !config.soundName().isBlank()) {
            player.stopSound(config.soundName(), SoundCategory.MASTER);
        }
    }

    private static float normalizeYaw(float yaw) {
        float value = yaw % 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private record CursorConfig(String cursorMaterial, double cursorScale, double cursorOffsetX, double cursorOffsetY,
                                double maxX, double maxY, double maxYaw, double maxPitch,
                                String soundName, float soundVolume, float soundPitch,
                                boolean soundLoopEnabled, int soundLoopDuration,
                                boolean joinRunEnabled, long joinRunDelay, String joinRunMenu, List<String> joinRunCommands,
                                boolean creatureSpawnProtectionEnabled, double creatureSpawnProtectionRadius,
                                boolean cameraBlockCheckEnabled, int cameraBlockCheckRadius,
                                boolean usePumpkinOverlay) {
        static CursorConfig defaults() {
            return new CursorConfig("NETHER_STAR", 0.35, 0.0, 0.0, 2.2, 1.2, 45.0, 30.0,
                    null, 1.0f, 1.0f, false, 40,
                    false, 20L, null, Collections.emptyList(),
                    false, 8.0, false, 1, false);
        }
    }

    private static final class MenuSession {
        private final String menuKey;
        private final Location returnLocation;
        private final GameMode originalGameMode;
        private final Location camera;
        private ArmorStand cursorAnchor;
        private ItemDisplay cursorDisplay;
        private Pig cameraSeat;
        private final List<SpawnedActor> actors = new ArrayList<>();
        private final List<ButtonState> buttons = new ArrayList<>();
        private ItemStack originalHelmet;
        private ItemStack originalMainHand;
        private ItemStack originalOffHand;
        private int originalHeldSlot;
        private boolean originalInvisible;
        private boolean originalCollidable;
        private boolean originalInvulnerable;
        private boolean originalAllowFlight;
        private boolean originalFlying;
        private float originalWalkSpeed;
        private float originalFlySpeed;
        private int cameraRefreshTicks;

        private MenuSession(String menuKey, Location returnLocation, GameMode originalGameMode, Location camera) {
            this.menuKey = menuKey;
            this.returnLocation = returnLocation;
            this.originalGameMode = originalGameMode;
            this.camera = camera;
        }
    }

    private static final class ButtonState {
        private final MenuButton button;
        private final TextDisplay display;

        private ButtonState(MenuButton button, TextDisplay display) {
            this.button = button;
            this.display = display;
        }
    }

    private static final class PreviewSession {
        private final ItemPreset preset;
        private final ItemDisplay display;
        private float rotation;

        private PreviewSession(ItemPreset preset, ItemDisplay display) {
            this.preset = preset;
            this.display = display;
        }
    }

    private static final class SpawnedActor {
        private final MenuActor definition;
        private final NPC npc;

        private SpawnedActor(MenuActor definition, NPC npc) {
            this.definition = definition;
            this.npc = npc;
        }

        private void syncLocation(Location location) {
            if (npc == null || location == null || !npc.isSpawned()) {
                return;
            }
            Entity entity = npc.getEntity();
            if (entity == null || entity.isDead()) {
                return;
            }
            entity.teleport(location);
            entity.setGravity(false);
        }

        private void destroy() {
            if (npc == null) {
                return;
            }
            try {
                npc.destroy();
            } catch (Exception ignored) {
            }
        }
    }
}
