package me.nakilex.levelplugin.npc.nms.entity;

import com.mojang.authlib.GameProfile;
import me.nakilex.levelplugin.npc.nms.NmsImpl;
import me.nakilex.levelplugin.npc.system.NPC;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
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
        ServerLevel level = NmsImpl.getServerLevel(world);
        UUID uuid = npc.getOrCreateNpcUuid();
        String profileName = sanitizeProfileName(npc.getName(), npc.getId());
        GameProfile profile = NmsImpl.createGameProfile(uuid, profileName);
        ClientInformation clientInformation = NmsImpl.createClientInformation();
        MinecraftServer server = MinecraftServer.getServer();
        EntityHumanNPC human = new EntityHumanNPC(
                server,
                level,
                profile,
                clientInformation,
                npc);
        NmsImpl.applyPosition(human, location);
        NmsImpl.addEntityToWorld(level, human);
        NmsImpl.addOrRemoveFromPlayerList(level, human, true);
        logNpcConnection(human);
        return human.getBukkitEntity();
    }

    public static void despawn(Player npcPlayer) {
        if (npcPlayer == null) {
            return;
        }
        ServerLevel level = NmsImpl.getServerLevel(npcPlayer.getWorld());
        var handle = NmsImpl.getServerPlayer(npcPlayer);
        NmsImpl.addOrRemoveFromPlayerList(level, handle, false);
        npcPlayer.remove();
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

    private static void logNpcConnection(EntityHumanNPC human) {
        if (human == null) {
            return;
        }
        boolean isListener = human.connection instanceof ServerGamePacketListenerImpl;
        boolean hasConnection = human.connection != null;
        Bukkit.getLogger().info("[NPC] Spawned player NPC connection=" + hasConnection
                + " listener=" + isListener);
    }
}
