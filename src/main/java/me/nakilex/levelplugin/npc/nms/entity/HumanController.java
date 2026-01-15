package me.nakilex.levelplugin.npc.nms.entity;

import me.nakilex.levelplugin.npc.nms.NmsImpl;
import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class HumanController {
    private HumanController() {
    }

    public static org.bukkit.entity.Entity spawn(NPC npc, Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        Object level = NmsImpl.getServerLevel(world);
        UUID uuid = npc.getOrCreateNpcUuid();
        String profileName = sanitizeProfileName(npc.getName(), npc.getId());
        Object profile = NmsImpl.createGameProfile(uuid, profileName);
        Object clientInformation = NmsImpl.createClientInformation();
        Object server = NmsImpl.getMinecraftServer();
        if (level == null || profile == null || clientInformation == null || server == null) {
            Bukkit.getLogger().warning("[NPC] Missing NMS components for player NPC spawn.");
            return null;
        }
        EntityHumanNPC human = new EntityHumanNPC(
                server,
                level,
                profile,
                clientInformation,
                npc);
        Object handle = human.getHandle();
        if (handle == null) {
            Bukkit.getLogger().warning("[NPC] Unable to construct ServerPlayer for NPC.");
            return null;
        }
        if (NmsImpl.getPlayerConnection(handle) == null) {
            Bukkit.getLogger().severe("[NPC] Missing connection for player NPC " + npc.getId()
                    + " (cookie/listener construction likely failed). Aborting spawn.");
            return null;
        }
        NmsImpl.applyPosition(handle, location);
        logNpcConnection(handle);
        return human.getBukkitEntity();
    }

    public static void despawn(Player npcPlayer) {
        if (npcPlayer == null) {
            return;
        }
        Object level = NmsImpl.getServerLevel(npcPlayer.getWorld());
        Object handle = NmsImpl.getServerPlayer(npcPlayer);
        NmsImpl.addOrRemoveFromPlayerList(level, handle, false);
        NmsImpl.disconnectPlayer(handle, "NPC removed");
        NmsImpl.removePlayerFromWorld(level, handle);
    }

    private static String sanitizeProfileName(String name, int id) {
        String stripped = name == null ? "" : ChatColor.stripColor(name).trim();
        if (stripped.isBlank()) {
            stripped = "NPC-" + id;
        }
        if (stripped.length() > 16) {
            stripped = stripped.substring(0, 16);
        }
        return stripped;
    }

    private static void logNpcConnection(Object handle) {
        if (handle == null) {
            return;
        }
        try {
            java.lang.reflect.Field connectionField = handle.getClass().getField("connection");
            Object connection = connectionField.get(handle);
            boolean hasConnection = connection != null;
            boolean isListener = connection != null
                    && "net.minecraft.server.network.ServerGamePacketListenerImpl".equals(connection.getClass().getName());
            Bukkit.getLogger().info("[NPC] Spawned player NPC connection=" + hasConnection
                    + " listener=" + isListener);
        } catch (ReflectiveOperationException ex) {
            Bukkit.getLogger().info("[NPC] Spawned player NPC connection=false listener=false");
        }
    }
}
