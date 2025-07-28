package me.nakilex.levelplugin.cutscene;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.frames.Frame;
import me.nakilex.levelplugin.cutscene.frames.TeleportFrame;
import me.nakilex.levelplugin.cutscene.frames.Keyframe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public class CutsceneManager {
    private final Main plugin;
    private final Map<String, Cutscene> cutscenes = new HashMap<>();
    private final Map<UUID, List<BukkitTask>> active = new HashMap<>();
    private final Map<UUID, RecordingSession> recordings = new HashMap<>();
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final Set<UUID> awaitingTitle = new HashSet<>();

    /** Returns true if the player is currently in a cutscene. */
    public boolean isInCutscene(Player player) {
        return active.containsKey(player.getUniqueId());
    }

    public CutsceneManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadCutscenes() {
        cutscenes.clear();
        File dir = new File(plugin.getDataFolder(), "cutscenes");
        if (!dir.exists()) {
            dir.mkdirs();
            // Copy example cutscene from the jar on first run
            plugin.saveResource("cutscenes/intro.yml", false);
        } else {
            File intro = new File(dir, "intro.yml");
            if (!intro.exists()) {
                plugin.saveResource("cutscenes/intro.yml", false);
            }
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String id = cfg.getString("id", file.getName().replace(".yml", ""));
            List<Map<?, ?>> framesSec = cfg.getMapList("frames");
            List<Frame> frames = new ArrayList<>();
            for (Map<?, ?> map : framesSec) {
                String pos = (String) map.get("pos");
                String world = (String) map.get("world");
                String type = map.containsKey("type") ? (String) map.get("type") : "teleport";
                String lookAtStr = (String) map.get("lookAt");
                long duration = map.get("duration") != null ? ((Number) map.get("duration")).longValue() : 2000L;
                double speed = map.get("speed") != null ? ((Number) map.get("speed")).doubleValue() : 0.0;
                String title = (String) map.get("title");
                String subtitle = (String) map.get("subtitle");
                String actionBar = (String) map.get("actionBar");
                String sound = (String) map.get("sound");
                String command = (String) map.get("command");
                Location loc = null;
                if (pos != null) {
                    String[] parts = pos.split(" ");
                    if (parts.length >= 5) {
                        double x = Double.parseDouble(parts[0]);
                        double y = Double.parseDouble(parts[1]);
                        double z = Double.parseDouble(parts[2]);
                        float yaw = Float.parseFloat(parts[3]);
                        float pitch = Float.parseFloat(parts[4]);
                        var worldObj = world != null ? Bukkit.getWorld(world) : plugin.getServer().getWorlds().get(0);
                        if (worldObj == null && "world2".equalsIgnoreCase(world)) {
                            worldObj = Bukkit.getWorld("world");
                        }
                        loc = new Location(worldObj, x, y, z, yaw, pitch);
                    }
                }

                Location lookAt = null;
                if (lookAtStr != null) {
                    String[] pa = lookAtStr.split(" ");
                    if (pa.length >= 3) {
                        double lx = Double.parseDouble(pa[0]);
                        double ly = Double.parseDouble(pa[1]);
                        double lz = Double.parseDouble(pa[2]);
                        var worldObj = world != null ? Bukkit.getWorld(world) : plugin.getServer().getWorlds().get(0);
                        if (worldObj == null && "world2".equalsIgnoreCase(world)) {
                            worldObj = Bukkit.getWorld("world");
                        }
                        lookAt = new Location(worldObj, lx, ly, lz);
                    }
                }

                if ("key".equalsIgnoreCase(type) || "keyframe".equalsIgnoreCase(type)) {
                    frames.add(new Keyframe(loc, lookAt, duration, world));
                } else {
                    frames.add(new TeleportFrame(loc, duration, title, subtitle, actionBar, sound, command, world, speed));
                }
            }
            cutscenes.put(id, new Cutscene(id, frames));
        }
    }

    public Set<String> listCutscenes() {
        return cutscenes.keySet();
    }

    public void playCutscene(Player player, String id) {
        Cutscene cs = cutscenes.get(id);
        if (cs == null) return;
        var settings = plugin.getSettingsManager();
        if (settings != null && settings.getSettings(player).isAutoSkipCutscenes()) {
            Location end = null;
            for (Frame f : cs.getFrames()) {
                if (f instanceof TeleportFrame tf && tf.getLocation() != null) {
                    end = tf.getLocation().clone();
                    if (tf.getWorldName() != null) {
                        var w = plugin.getServer().getWorld(tf.getWorldName());
                        if (w != null) end.setWorld(w);
                    }
                } else if (f instanceof Keyframe k && k.getLocation() != null) {
                    end = k.getLocation().clone();
                    if (k.getWorldName() != null) {
                        var w = plugin.getServer().getWorld(k.getWorldName());
                        if (w != null) end.setWorld(w);
                    }
                }
            }
            if (end != null) {
                player.teleport(end);
            }
            return;
        }
        stopCutscene(player);
        states.put(player.getUniqueId(), new PlayerState(player.getGameMode(), player.getAllowFlight(), player.isFlying()));
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        var sbManager = plugin.getScoreboardManager();
        if (sbManager != null) sbManager.removeBoard(player);

        TextComponent skip = new TextComponent(ChatColor.YELLOW + "[Skip Cutscene]");
        skip.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cutscene skip"));
        skip.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to skip")));
        player.spigot().sendMessage(skip);
        long delay = 0L;
        List<BukkitTask> tasks = new ArrayList<>();
        Location curr = player.getLocation().clone();
        for (Frame frame : cs.getFrames()) {
            long ticks;
            if (frame instanceof TeleportFrame tf && tf.getSpeed() > 0 && tf.getLocation() != null) {
                Location target = tf.getLocation().clone();
                if (tf.getWorldName() != null) {
                    var w = plugin.getServer().getWorld(tf.getWorldName());
                    if (w != null) target.setWorld(w);
                }
                if (curr.getWorld() != null && target.getWorld() != null && curr.getWorld().equals(target.getWorld())) {
                    double dist = curr.distance(target);
                    double scaled = Math.pow(tf.getSpeed(), 1.5);
                    long move = Math.max(1L, Math.round(dist / scaled * 20.0));
                    ticks = move + Math.round(tf.getDuration() / 50.0);
                } else {
                    ticks = Math.round(tf.getDuration() / 50.0);
                }
                curr = target;
            } else {
                ticks = Math.max(1L, frame.getDuration() / 50L);
                if (frame instanceof Keyframe k && k.getLocation() != null) {
                    Location t = k.getLocation().clone();
                    if (k.getWorldName() != null) {
                        var w = plugin.getServer().getWorld(k.getWorldName());
                        if (w != null) t.setWorld(w);
                    }
                    curr = t;
                }
            }
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                BukkitTask inner = frame.play(player, plugin);
                if (inner != null) {
                    tasks.add(inner);
                }
            }, delay);
            tasks.add(task);
            delay += ticks;
        }
        PlayerState st = states.get(player.getUniqueId());
        if (st != null) st.endLocation = curr.clone();
        BukkitTask endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            active.remove(player.getUniqueId());
            restore(player);
        }, delay);
        tasks.add(endTask);
        active.put(player.getUniqueId(), tasks);
    }

    public void stopCutscene(Player player) {
        List<BukkitTask> list = active.remove(player.getUniqueId());
        if (list != null) {
            for (BukkitTask task : list) {
                task.cancel();
            }
        }
        restore(player);
    }

    public void skipCutscene(Player player) {
        List<BukkitTask> list = active.remove(player.getUniqueId());
        if (list != null) {
            for (BukkitTask task : list) {
                task.cancel();
            }
        }
        PlayerState state = states.remove(player.getUniqueId());
        if (state != null && state.endLocation != null) {
            player.teleport(state.endLocation);
        }
        restore(player, state);
    }

    /** Recording API **/
    public void startRecording(Player player, String id) {
        if (recordings.containsKey(player.getUniqueId())) return;
        RecordingSession session = new RecordingSession(id, player);
        recordings.put(player.getUniqueId(), session);

        player.getInventory().clear();
        player.getInventory().setItem(0, createTool(Material.STICK, ChatColor.GOLD + "Add Frame"));
        player.getInventory().setItem(1, createTool(Material.FEATHER, ChatColor.AQUA + "Speed: " + session.speed));
        player.getInventory().setItem(2, createTool(Material.ENDER_PEARL, ChatColor.YELLOW + (session.movement ? "Mode: Move" : "Mode: Teleport")));
        player.getInventory().setItem(3, createTool(Material.CLOCK, ChatColor.LIGHT_PURPLE + "Pause: " + session.pause + "ms"));
        player.getInventory().setItem(4, createTool(Material.PAPER, ChatColor.BLUE + "Add Title"));
        player.getInventory().setItem(7, createTool(Material.LIME_DYE, ChatColor.GREEN + "Save"));
        player.getInventory().setItem(8, createTool(Material.BARRIER, ChatColor.RED + "Cancel"));
    }

    public boolean isRecording(Player player) {
        return recordings.containsKey(player.getUniqueId());
    }

    public void cancelRecording(Player player) {
        RecordingSession session = recordings.remove(player.getUniqueId());
        if (session == null) return;
        restoreInventory(player, session);
    }

    public void addFrame(Player player, long duration) {
        RecordingSession session = recordings.get(player.getUniqueId());
        if (session == null) return;
        Location loc = player.getLocation();
        double speed = session.movement ? session.speed : 0;
        String t = session.pendingTitle;
        String sub = session.pendingSubtitle;
        session.pendingTitle = null;
        session.pendingSubtitle = null;
        TeleportFrame frame = new TeleportFrame(loc, duration, t, sub, null, null, null, loc.getWorld().getName(), speed);
        session.frames.add(frame);
    }

    public void addFrame(Player player) {
        RecordingSession session = recordings.get(player.getUniqueId());
        if (session == null) return;
        addFrame(player, session.pause);
    }

    public boolean isAwaitingTitle(Player player) {
        return awaitingTitle.contains(player.getUniqueId());
    }

    public void promptTitle(Player player) {
        if (!recordings.containsKey(player.getUniqueId())) return;
        awaitingTitle.add(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Type title as '&5Main_sub' or 'cancel'");
    }

    public void handleTitleChat(Player player, String message) {
        if (!awaitingTitle.remove(player.getUniqueId())) return;
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.RED + "Title entry cancelled.");
            return;
        }
        RecordingSession session = recordings.get(player.getUniqueId());
        if (session == null) return;
        String main = message;
        String sub = "";
        int space = message.indexOf(' ');
        if (space >= 0) {
            main = message.substring(0, space);
            sub = message.substring(space + 1);
        }
        main = main.replace('_', ' ');
        session.pendingTitle = main;
        session.pendingSubtitle = sub;
        player.sendMessage(ChatColor.GREEN + "Title will apply to the next frame.");
    }

    public void finishRecording(Player player) {
        RecordingSession session = recordings.remove(player.getUniqueId());
        if (session == null) return;

        if (session.pendingTitle != null || session.pendingSubtitle != null) {
            TeleportFrame frame = new TeleportFrame(null, 0L, session.pendingTitle,
                    session.pendingSubtitle, null, null, null, null, 0);
            session.frames.add(frame);
            session.pendingTitle = null;
            session.pendingSubtitle = null;
        }

        File dir = new File(plugin.getDataFolder(), "cutscenes");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, session.id + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("id", session.id);
        List<Map<String, Object>> list = new ArrayList<>();
        for (TeleportFrame frame : session.frames) {
            Map<String, Object> map = new HashMap<>();
            Location l = frame.getLocation();
            if (l != null) {
                String pos = l.getX() + " " + l.getY() + " " + l.getZ() + " " + l.getYaw() + " " + l.getPitch();
                map.put("pos", pos);
                map.put("world", frame.getWorldName());
            }
            map.put("duration", frame.getDuration());
            if (frame.getSpeed() > 0) {
                map.put("speed", frame.getSpeed());
            }
            if (frame.getTitle() != null) {
                map.put("title", frame.getTitle());
            }
            if (frame.getSubtitle() != null) {
                map.put("subtitle", frame.getSubtitle());
            }
            if (frame.getActionBar() != null) {
                map.put("actionBar", frame.getActionBar());
            }
            if (frame.getSound() != null) {
                map.put("sound", frame.getSound());
            }
            if (frame.getCommand() != null && !frame.getCommand().isEmpty()) {
                map.put("command", frame.getCommand());
            }
            list.add(map);
        }
        cfg.set("frames", list);
        try {
            cfg.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadCutscenes();
        restoreInventory(player, session);
    }

    private void restore(Player player) {
        PlayerState state = states.remove(player.getUniqueId());
        restore(player, state);
    }

    private void restore(Player player, PlayerState state) {
        if (state != null) {
            player.setGameMode(state.mode);
            player.setAllowFlight(state.allowFlight);
            player.setFlying(state.flying);
        }
        var sbManager = plugin.getScoreboardManager();
        if (sbManager != null) sbManager.createBoard(player);
    }

    private static class RecordingSession {
        final String id;
        final List<TeleportFrame> frames = new ArrayList<>();
        final ItemStack[] contents;
        final ItemStack[] armor;
        int speed = 4;
        boolean movement = true;
        long pause = 0L;
        String pendingTitle = null;
        String pendingSubtitle = null;

        RecordingSession(String id, Player player) {
            this.id = id;
            this.contents = player.getInventory().getContents().clone();
            this.armor = player.getInventory().getArmorContents().clone();
        }
    }

    private static class PlayerState {
        final GameMode mode;
        final boolean allowFlight;
        final boolean flying;
        Location endLocation;

        PlayerState(GameMode mode, boolean allowFlight, boolean flying) {
            this.mode = mode;
            this.allowFlight = allowFlight;
            this.flying = flying;
        }
    }

    private ItemStack createTool(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void restoreInventory(Player player, RecordingSession session) {
        player.getInventory().setContents(session.contents);
        player.getInventory().setArmorContents(session.armor);
    }

    public void changeSpeed(Player player, int delta) {
        RecordingSession session = recordings.get(player.getUniqueId());
        if (session == null) return;
        session.speed += delta;
        if (session.speed > 10) session.speed = 1;
        if (session.speed < 1) session.speed = 10;
        updateEditorItems(player, session);
    }

    public void toggleMovement(Player player) {
        RecordingSession session = recordings.get(player.getUniqueId());
        if (session == null) return;
        session.movement = !session.movement;
        updateEditorItems(player, session);
    }

    public void changePause(Player player, long delta) {
        RecordingSession session = recordings.get(player.getUniqueId());
        if (session == null) return;
        session.pause += delta;
        if (session.pause < 0) session.pause = 0;
        updateEditorItems(player, session);
    }

    private void updateEditorItems(Player player, RecordingSession session) {
        player.getInventory().setItem(1, createTool(Material.FEATHER, ChatColor.AQUA + "Speed: " + session.speed));
        player.getInventory().setItem(2, createTool(Material.ENDER_PEARL, ChatColor.YELLOW + (session.movement ? "Mode: Move" : "Mode: Teleport")));
        player.getInventory().setItem(3, createTool(Material.CLOCK, ChatColor.LIGHT_PURPLE + "Pause: " + session.pause + "ms"));
    }
}
