package me.nakilex.levelplugin.cutscene;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.frames.Frame;
import me.nakilex.levelplugin.cutscene.frames.TeleportFrame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

    public CutsceneManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadCutscenes() {
        cutscenes.clear();
        File dir = new File(plugin.getDataFolder(), "cutscenes");
        if (!dir.exists()) {
            dir.mkdirs();
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
                long duration = map.get("duration") != null ? ((Number) map.get("duration")).longValue() : 2000L;
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
                        loc = new Location(plugin.getServer().getWorlds().get(0), x, y, z, yaw, pitch);
                    }
                }
                frames.add(new TeleportFrame(loc, duration, title, subtitle, actionBar, sound, command));
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
        stopCutscene(player);
        long delay = 0L;
        List<BukkitTask> tasks = new ArrayList<>();
        for (Frame frame : cs.getFrames()) {
            long ticks = Math.max(1L, frame.getDuration() / 50L);
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> frame.play(player, plugin), delay);
            tasks.add(task);
            delay += ticks;
        }
        BukkitTask endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> active.remove(player.getUniqueId()), delay);
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
    }

    /** Recording API **/
    public void startRecording(Player player, String id) {
        recordings.put(player.getUniqueId(), new RecordingSession(id));
    }

    public boolean isRecording(Player player) {
        return recordings.containsKey(player.getUniqueId());
    }

    public void addFrame(Player player, long duration) {
        RecordingSession session = recordings.get(player.getUniqueId());
        if (session == null) return;
        Location loc = player.getLocation();
        TeleportFrame frame = new TeleportFrame(loc, duration, null, null, null, null, null);
        session.frames.add(frame);
    }

    public void finishRecording(Player player) {
        RecordingSession session = recordings.remove(player.getUniqueId());
        if (session == null) return;

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
            }
            map.put("duration", frame.getDuration());
            list.add(map);
        }
        cfg.set("frames", list);
        try {
            cfg.save(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadCutscenes();
    }

    private static class RecordingSession {
        final String id;
        final List<TeleportFrame> frames = new ArrayList<>();

        RecordingSession(String id) {
            this.id = id;
        }
    }
}
