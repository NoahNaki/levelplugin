package me.nakilex.levelplugin.cutscene;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import me.nakilex.levelplugin.cutscene.playback.CutscenePlayback;
import me.nakilex.levelplugin.cutscene.CutsceneLoader;
import me.nakilex.levelplugin.cutscene.CutsceneFormatException;
import me.nakilex.levelplugin.cutscene.CutsceneIO;
import me.nakilex.levelplugin.cutscene.frames.ActorActionFrame;
import me.nakilex.levelplugin.cutscene.frames.BranchFrame;
import me.nakilex.levelplugin.cutscene.frames.DialogueFrame;
import me.nakilex.levelplugin.cutscene.frames.EffectFrame;
import me.nakilex.levelplugin.cutscene.frames.Frame;
import me.nakilex.levelplugin.cutscene.frames.Keyframe;
import me.nakilex.levelplugin.cutscene.frames.TeleportFrame;
import me.nakilex.levelplugin.cutscene.frames.WaitFrame;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class CutsceneManager {
    private final Main plugin;
    private final Map<String, Cutscene> cutscenes = new HashMap<>();
    private final Map<UUID, CutscenePlayback> activePlaybacks = new HashMap<>();
    private final List<CutscenePlayback> runningPlaybacks = new ArrayList<>();
    private final Map<UUID, RecordingSession> recordings = new HashMap<>();
    private final Map<UUID, RecordingPrompt> prompts = new HashMap<>();

    /** Returns true if the player is currently in a cutscene. */
    public boolean isInCutscene(Player player) {
        return activePlaybacks.containsKey(player.getUniqueId());
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
            try {
                Cutscene cutscene = CutsceneLoader.load(plugin, cfg, file.getName().replace(".yml", ""));
                cutscenes.put(cutscene.getId(), cutscene);
            } catch (CutsceneFormatException ex) {
                plugin.getLogger().warning("Failed to load cutscene from " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    public Set<String> listCutscenes() {
        return cutscenes.keySet();
    }

    public void playCutscene(Player player, String id) {
        playCutscene(Collections.singletonList(player), id);
    }

    public void playCutscene(Collection<Player> players, String id) {
        Cutscene cutscene = cutscenes.get(id);
        if (cutscene == null) {
            return;
        }
        List<Player> participants = new ArrayList<>();
        for (Player viewer : players) {
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }
            if (shouldAutoSkip(viewer, cutscene)) {
                teleportToEnd(viewer, cutscene);
            } else {
                stopCutscene(viewer);
                participants.add(viewer);
            }
        }
        if (participants.isEmpty()) {
            return;
        }
        CutscenePlayback[] holder = new CutscenePlayback[1];
        CutscenePlayback playback = new CutscenePlayback(plugin, cutscene, participants, () -> onPlaybackFinished(holder[0]));
        holder[0] = playback;
        runningPlaybacks.add(playback);
        TextComponent skip = new TextComponent(ChatColor.YELLOW + "[Skip Cutscene]");
        skip.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cutscene skip"));
        skip.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to skip")));
        for (Player viewer : participants) {
            activePlaybacks.put(viewer.getUniqueId(), playback);
            viewer.spigot().sendMessage(skip);
        }
        playback.start();
    }

    public void stopCutscene(Player player) {
        CutscenePlayback playback = activePlaybacks.remove(player.getUniqueId());
        if (playback != null) {
            playback.skip(player);
        }
    }

    public void skipCutscene(Player player) {
        CutscenePlayback playback = activePlaybacks.remove(player.getUniqueId());
        if (playback != null) {
            playback.skip(player);
        }
    }

    private boolean shouldAutoSkip(Player player, Cutscene cutscene) {
        var settings = plugin.getSettingsManager();
        return settings != null && settings.getSettings(player).isAutoSkipCutscenes();
    }

    private void teleportToEnd(Player player, Cutscene cutscene) {
        Location end = cutscene.resolveEndLocation();
        if (end != null) {
            player.teleport(end.clone());
        }
    }

    private void onPlaybackFinished(CutscenePlayback playback) {
        runningPlaybacks.remove(playback);
        activePlaybacks.entrySet().removeIf(entry -> entry.getValue() == playback);
    }


    /** Recording API **/
    public void startRecording(Player player, String id) {
        if (recordings.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are already recording a cutscene.");
            return;
        }
        RecordingSession session = new RecordingSession(id, player);
        recordings.put(player.getUniqueId(), session);

        player.getInventory().clear();
        updateEditorItems(player, session);
        player.sendMessage(ChatColor.AQUA + "Recording '" + id + "'. Use the hotbar tools to add frames.");
    }

    public boolean isRecording(Player player) {
        return recordings.containsKey(player.getUniqueId());
    }

    public void cancelRecording(Player player) {
        UUID uuid = player.getUniqueId();
        RecordingSession session = recordings.remove(uuid);
        if (session == null) {
            return;
        }
        prompts.remove(uuid);
        restoreInventory(player, session);
        player.sendMessage(ChatColor.YELLOW + "Recording cancelled.");
    }

    public void addFrame(Player player, long duration) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        long previous = session.durationMs;
        session.durationMs = duration;
        addFrame(player);
        session.durationMs = previous;
    }

    public void addFrame(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        Location location = player.getLocation();
        EffectSettings effects = buildPendingEffects(session);
        if (session.mode != RecordingMode.TELEPORT && !effects.isEmpty()) {
            session.frames.add(new EffectFrame(effects, 0L));
        }
        switch (session.mode) {
            case TELEPORT -> {
                double speed = Math.max(0.0, Math.min(20.0, session.teleportSpeed));
                String worldName = location.getWorld() != null ? location.getWorld().getName() : null;
                session.frames.add(new TeleportFrame(location.clone(), session.durationMs, effects, worldName, speed));
            }
            case SMOOTH -> {
                Location look = session.pendingLook != null ? session.pendingLook.clone() : null;
                String worldName = location.getWorld() != null ? location.getWorld().getName() : null;
                session.frames.add(new Keyframe(location.clone(), look, session.durationMs, worldName));
            }
            case WAIT -> session.frames.add(new WaitFrame(session.durationMs, null));
        }
        session.pendingTitle = null;
        session.pendingSubtitle = null;
        session.pendingActionBar = null;
        session.pendingBundles.clear();

        player.sendMessage(ChatColor.GRAY + "Added " + session.mode.displayName().toLowerCase(Locale.ROOT)
                + " frame (" + session.frames.size() + " total).");
        updateEditorItems(player, session);
    }

    public void finishRecording(Player player) {
        UUID uuid = player.getUniqueId();
        RecordingSession session = recordings.remove(uuid);
        if (session == null) {
            return;
        }
        prompts.remove(uuid);

        File dir = new File(plugin.getDataFolder(), "cutscenes");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, session.id + ".yml");

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("version", 2);
        cfg.set("id", session.id);

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (!session.description.isEmpty()) {
            metadata.put("description", session.description);
        }
        if (!session.tags.isEmpty()) {
            metadata.put("tags", new ArrayList<>(session.tags));
        }
        if (session.autoStart) {
            metadata.put("autoStart", true);
        }
        if (!metadata.isEmpty()) {
            cfg.set("metadata", metadata);
        }

        List<Map<String, Object>> frames = new ArrayList<>();
        for (Frame frame : session.frames) {
            Map<String, Object> map = serializeFrame(frame);
            if (!map.isEmpty()) {
                frames.add(map);
            }
        }
        cfg.set("frames", frames);

        try {
            cfg.save(file);
            player.sendMessage(ChatColor.GREEN + "Saved cutscene '" + session.id + "'.");
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to save cutscene '" + session.id + "': " + ex.getMessage());
            player.sendMessage(ChatColor.RED + "Failed to save cutscene. Check console for details.");
        }

        loadCutscenes();
        restoreInventory(player, session);
    }

    public void cycleMode(Player player, int delta) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        RecordingMode[] values = RecordingMode.values();
        int index = (session.mode.ordinal() + delta) % values.length;
        if (index < 0) {
            index += values.length;
        }
        session.mode = values[index];
        player.sendMessage(ChatColor.YELLOW + "Mode set to " + session.mode.displayName() + ".");
        updateEditorItems(player, session);
    }

    public void adjustDuration(Player player, long delta) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        session.durationMs = Math.max(0L, session.durationMs + delta);
        player.sendMessage(ChatColor.AQUA + "Frame duration set to " + session.durationMs + "ms.");
        updateEditorItems(player, session);
    }

    public void adjustTeleportSpeed(Player player, double delta) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        session.teleportSpeed = Math.max(0.0, Math.min(20.0, session.teleportSpeed + delta));
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Teleport speed: "
                + (session.teleportSpeed <= 0 ? "instant" : String.format(Locale.ROOT, "%.1f bps", session.teleportSpeed)) + ".");
        updateEditorItems(player, session);
    }

    public void captureLookTarget(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        Location look;
        Block block = player.getTargetBlockExact(80);
        if (block != null) {
            look = block.getLocation().add(0.5, 0.5, 0.5);
        } else {
            Location eye = player.getEyeLocation();
            look = eye.clone().add(eye.getDirection().normalize().multiply(5));
        }
        session.pendingLook = look;
        player.sendMessage(ChatColor.GREEN + "Look target captured.");
        updateEditorItems(player, session);
    }

    public void clearLookTarget(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        session.pendingLook = null;
        player.sendMessage(ChatColor.YELLOW + "Cleared look target.");
        updateEditorItems(player, session);
    }

    public void promptTitle(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        beginPrompt(player, new RecordingPrompt() {
            @Override
            public void start(Player player, RecordingSession session) {
                player.sendMessage(ChatColor.YELLOW + "Enter a title for the next frame. Use 'Title|Subtitle'.");
                player.sendMessage(ChatColor.GRAY + "Type 'clear' to remove or 'cancel' to abort.");
            }

            @Override
            public boolean handle(Player player, RecordingSession session, String message) {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.RED + "Title entry cancelled.");
                    return true;
                }
                if (message.equalsIgnoreCase("clear")) {
                    session.pendingTitle = null;
                    session.pendingSubtitle = null;
                    player.sendMessage(ChatColor.YELLOW + "Cleared pending title.");
                    return true;
                }
                String[] parts = message.split(Pattern.quote("|"), 2);
                session.pendingTitle = parts[0].trim();
                session.pendingSubtitle = parts.length > 1 ? parts[1].trim() : null;
                player.sendMessage(ChatColor.GREEN + "Title set for the next frame.");
                return true;
            }
        });
    }

    public void promptActionBar(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        beginPrompt(player, new RecordingPrompt() {
            @Override
            public void start(Player player, RecordingSession session) {
                player.sendMessage(ChatColor.YELLOW + "Enter an action bar message for the next frame.");
                player.sendMessage(ChatColor.GRAY + "Type 'clear' to remove or 'cancel' to abort.");
            }

            @Override
            public boolean handle(Player player, RecordingSession session, String message) {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.RED + "Action bar entry cancelled.");
                    return true;
                }
                if (message.equalsIgnoreCase("clear")) {
                    session.pendingActionBar = null;
                    player.sendMessage(ChatColor.YELLOW + "Cleared pending action bar.");
                    return true;
                }
                session.pendingActionBar = message.trim();
                player.sendMessage(ChatColor.GREEN + "Action bar set for the next frame.");
                return true;
            }
        });
    }

    public void promptBundles(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        beginPrompt(player, new RecordingPrompt() {
            @Override
            public void start(Player player, RecordingSession session) {
                if (!session.pendingBundles.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "Current bundles: " + String.join(", ", session.pendingBundles));
                }
                player.sendMessage(ChatColor.YELLOW + "Enter bundle names separated by spaces.");
                player.sendMessage(ChatColor.GRAY + "Type 'clear' to remove or 'cancel' to abort.");
            }

            @Override
            public boolean handle(Player player, RecordingSession session, String message) {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.RED + "Bundle entry cancelled.");
                    return true;
                }
                if (message.equalsIgnoreCase("clear")) {
                    session.pendingBundles.clear();
                    player.sendMessage(ChatColor.YELLOW + "Cleared pending bundles.");
                    return true;
                }
                session.pendingBundles.clear();
                for (String token : message.split("[,\s]+")) {
                    String cleaned = token.trim();
                    if (!cleaned.isEmpty()) {
                        session.pendingBundles.add(cleaned.toLowerCase(Locale.ROOT));
                    }
                }
                player.sendMessage(ChatColor.GREEN + "Bundles applied to the next frame.");
                return true;
            }
        });
    }

    public void promptDescription(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        beginPrompt(player, new RecordingPrompt() {
            @Override
            public void start(Player player, RecordingSession session) {
                player.sendMessage(ChatColor.YELLOW + "Enter a cutscene description.");
                player.sendMessage(ChatColor.GRAY + "Type 'clear' to remove or 'cancel' to abort.");
            }

            @Override
            public boolean handle(Player player, RecordingSession session, String message) {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.RED + "Description entry cancelled.");
                    return true;
                }
                if (message.equalsIgnoreCase("clear")) {
                    session.description = "";
                    player.sendMessage(ChatColor.YELLOW + "Cleared description.");
                    return true;
                }
                session.description = message.trim();
                player.sendMessage(ChatColor.GREEN + "Description updated.");
                return true;
            }
        });
    }

    public void promptTags(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        beginPrompt(player, new RecordingPrompt() {
            @Override
            public void start(Player player, RecordingSession session) {
                if (!session.tags.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "Current tags: " + String.join(", ", session.tags));
                }
                player.sendMessage(ChatColor.YELLOW + "Enter tags separated by spaces or commas.");
                player.sendMessage(ChatColor.GRAY + "Type 'clear' to remove or 'cancel' to abort.");
            }

            @Override
            public boolean handle(Player player, RecordingSession session, String message) {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.RED + "Tag entry cancelled.");
                    return true;
                }
                if (message.equalsIgnoreCase("clear")) {
                    session.tags.clear();
                    player.sendMessage(ChatColor.YELLOW + "Cleared tags.");
                    return true;
                }
                session.tags.clear();
                for (String token : message.split("[,\s]+")) {
                    String cleaned = token.trim();
                    if (!cleaned.isEmpty()) {
                        session.tags.add(cleaned);
                    }
                }
                player.sendMessage(ChatColor.GREEN + "Tags updated.");
                return true;
            }
        });
    }

    public void toggleAutoStart(Player player) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        session.autoStart = !session.autoStart;
        player.sendMessage(ChatColor.DARK_AQUA + "Auto-start " + (session.autoStart ? "enabled." : "disabled."));
        updateEditorItems(player, session);
    }

    public boolean handleChat(Player player, String message) {
        RecordingPrompt prompt = prompts.get(player.getUniqueId());
        if (prompt == null) {
            return false;
        }
        RecordingSession session = getSession(player);
        if (session == null) {
            prompts.remove(player.getUniqueId());
            return false;
        }
        boolean completed = prompt.handle(player, session, message);
        if (completed) {
            prompts.remove(player.getUniqueId());
            updateEditorItems(player, session);
        }
        return true;
    }

    private RecordingSession getSession(Player player) {
        return recordings.get(player.getUniqueId());
    }

    private void beginPrompt(Player player, RecordingPrompt prompt) {
        RecordingSession session = getSession(player);
        if (session == null) {
            return;
        }
        prompts.put(player.getUniqueId(), prompt);
        prompt.start(player, session);
    }

    private Map<String, Object> serializeFrame(Frame frame) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (frame instanceof TeleportFrame teleport) {
            map.put("type", "teleport");
            map.put("duration", teleport.getDuration());
            if (teleport.getLocation() != null) {
                map.put("pos", CutsceneIO.formatLocation(teleport.getLocation()));
            }
            if (teleport.getWorldName() != null) {
                map.put("world", teleport.getWorldName());
            }
            if (teleport.getSpeed() > 0) {
                map.put("speed", teleport.getSpeed());
            }
            if (!teleport.getEffects().isEmpty()) {
                map.put("effects", teleport.getEffects().toMap());
            }
        } else if (frame instanceof Keyframe keyframe) {
            map.put("type", "keyframe");
            map.put("duration", keyframe.getDuration());
            if (keyframe.getLocation() != null) {
                map.put("pos", CutsceneIO.formatLocation(keyframe.getLocation()));
            }
            if (keyframe.getWorldName() != null) {
                map.put("world", keyframe.getWorldName());
            }
            if (keyframe.getLookAt() != null) {
                map.put("lookAt", CutsceneIO.formatVector(keyframe.getLookAt()));
            }
        } else if (frame instanceof WaitFrame wait) {
            map.put("type", "wait");
            map.put("duration", wait.getDuration());
            if (wait.getActorToAwait() != null) {
                map.put("actor", wait.getActorToAwait());
            }
        } else if (frame instanceof DialogueFrame dialogue) {
            map.put("type", "dialogue");
            map.put("duration", dialogue.getDuration());
            if (dialogue.getSpeaker() != null) {
                map.put("actor", dialogue.getSpeaker());
            }
            if (dialogue.getMessage() != null) {
                map.put("text", dialogue.getMessage());
            }
            if (dialogue.getSubtitle() != null) {
                map.put("subtitle", dialogue.getSubtitle());
            }
            if (!dialogue.getEffects().isEmpty()) {
                map.put("effects", dialogue.getEffects().toMap());
            }
        } else if (frame instanceof EffectFrame effect) {
            map.put("type", "effect");
            map.put("duration", effect.getDuration());
            if (!effect.getEffects().isEmpty()) {
                map.put("effects", effect.getEffects().toMap());
            }
        } else if (frame instanceof ActorActionFrame actor) {
            map.put("type", "actor");
            map.put("duration", actor.getDuration());
            if (actor.getActorName() != null) {
                map.put("actor", actor.getActorName());
            }
            if (actor.getAction() != null) {
                map.put("action", actor.getAction());
            }
            if (!actor.getParameters().isEmpty()) {
                map.put("params", new LinkedHashMap<>(actor.getParameters()));
            }
        } else if (frame instanceof BranchFrame branch) {
            map.put("type", "branch");
            map.put("duration", branch.getDuration());
            if (branch.getPermission() != null) {
                map.put("permission", branch.getPermission());
            }
            if (branch.isInvert()) {
                map.put("invert", true);
            }
            if (branch.getMessage() != null) {
                map.put("message", branch.getMessage());
            }
        }
        return map;
    }

    private EffectSettings buildPendingEffects(RecordingSession session) {
        EffectSettings.Builder builder = EffectSettings.builder();
        if (session.pendingTitle != null && !session.pendingTitle.isEmpty()) {
            builder.title(session.pendingTitle);
        }
        if (session.pendingSubtitle != null && !session.pendingSubtitle.isEmpty()) {
            builder.subtitle(session.pendingSubtitle);
        }
        if (session.pendingActionBar != null && !session.pendingActionBar.isEmpty()) {
            builder.actionBar(session.pendingActionBar);
        }
        for (String bundle : session.pendingBundles) {
            builder.referenceBundle(bundle);
        }
        return builder.build();
    }

    private String describeEffects(RecordingSession session) {
        List<String> parts = new ArrayList<>();
        if (session.pendingTitle != null) {
            parts.add("Title");
        }
        if (session.pendingSubtitle != null) {
            parts.add("Subtitle");
        }
        if (session.pendingActionBar != null) {
            parts.add("ActionBar");
        }
        if (!session.pendingBundles.isEmpty()) {
            parts.add("Bundles(" + session.pendingBundles.size() + ")");
        }
        if (parts.isEmpty()) {
            return ChatColor.RED + "None";
        }
        return ChatColor.GREEN + String.join(ChatColor.GRAY + ", " + ChatColor.GREEN, parts);
    }

    private List<String> buildMetadataLore(RecordingSession session) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Left-click: set description");
        lore.add(ChatColor.GRAY + "Right-click: set tags");
        lore.add(ChatColor.GRAY + "Sneak-click: toggle auto-start");
        lore.add(ChatColor.DARK_GRAY + "Desc: " + ChatColor.WHITE + (session.description.isEmpty() ? "None" : session.description));
        lore.add(ChatColor.DARK_GRAY + "Tags: " + ChatColor.WHITE + (session.tags.isEmpty() ? "None" : String.join(", ", session.tags)));
        lore.add(ChatColor.DARK_GRAY + "Auto-start: " + (session.autoStart ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        return lore;
    }

    private static class RecordingSession {
        final String id;
        final List<Frame> frames = new ArrayList<>();
        final ItemStack[] contents;
        final ItemStack[] armor;
        RecordingMode mode = RecordingMode.TELEPORT;
        double teleportSpeed = 4.0;
        long durationMs = 2000L;
        String pendingTitle = null;
        String pendingSubtitle = null;
        String pendingActionBar = null;
        final List<String> pendingBundles = new ArrayList<>();
        Location pendingLook = null;
        boolean autoStart = false;
        String description = "";
        final List<String> tags = new ArrayList<>();

        RecordingSession(String id, Player player) {
            this.id = id;
            this.contents = player.getInventory().getContents().clone();
            this.armor = player.getInventory().getArmorContents().clone();
        }
    }

    private enum RecordingMode {
        TELEPORT("Teleport"),
        SMOOTH("Smooth"),
        WAIT("Wait");

        private final String displayName;

        RecordingMode(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private ItemStack createTool(Material mat, String name) {
        return createTool(mat, name, List.of());
    }

    private ItemStack createTool(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void restoreInventory(Player player, RecordingSession session) {
        player.getInventory().setContents(session.contents);
        player.getInventory().setArmorContents(session.armor);
        player.updateInventory();
    }

    private void updateEditorItems(Player player, RecordingSession session) {
        player.getInventory().setItem(0, createTool(Material.STICK, ChatColor.GOLD + "Add Frame",
                List.of(ChatColor.GRAY + "Capture the current position")));
        player.getInventory().setItem(1, createTool(Material.ENDER_PEARL, ChatColor.YELLOW + "Mode: " + session.mode.displayName(),
                List.of(ChatColor.GRAY + "Left-click: next", ChatColor.GRAY + "Right-click: previous")));
        player.getInventory().setItem(2, createTool(Material.CLOCK, ChatColor.AQUA + "Duration: " + session.durationMs + "ms",
                List.of(ChatColor.GRAY + "Left-click: +500ms", ChatColor.GRAY + "Right-click: -500ms",
                        ChatColor.GRAY + "Sneak: +/-1000ms")));
        String speedLabel = session.teleportSpeed <= 0 ? "Instant" : String.format(Locale.ROOT, "%.1f bps", session.teleportSpeed);
        player.getInventory().setItem(3, createTool(Material.SUGAR, ChatColor.LIGHT_PURPLE + "Speed: " + speedLabel,
                List.of(ChatColor.GRAY + "Left-click: increase", ChatColor.GRAY + "Right-click: decrease",
                        ChatColor.GRAY + "Sneak: +/-0.5")));
        player.getInventory().setItem(4, createTool(Material.COMPASS, ChatColor.GREEN + "Look Target: "
                + (session.pendingLook == null ? ChatColor.RED + "None" : ChatColor.GREEN + "Set"),
                List.of(ChatColor.GRAY + "Left-click: capture target", ChatColor.GRAY + "Right-click: clear")));
        player.getInventory().setItem(5, createTool(Material.PAPER, ChatColor.BLUE + "Effects: " + describeEffects(session),
                List.of(ChatColor.GRAY + "Left-click: title/subtitle", ChatColor.GRAY + "Right-click: action bar",
                        ChatColor.GRAY + "Sneak + Right-click: bundles")));
        player.getInventory().setItem(6, createTool(Material.BOOK, ChatColor.DARK_AQUA + "Metadata", buildMetadataLore(session)));
        player.getInventory().setItem(7, createTool(Material.LIME_DYE, ChatColor.GREEN + "Save"));
        player.getInventory().setItem(8, createTool(Material.BARRIER, ChatColor.RED + "Cancel"));
        player.updateInventory();
    }

    private interface RecordingPrompt {
        void start(Player player, RecordingSession session);

        boolean handle(Player player, RecordingSession session, String message);
    }
}
