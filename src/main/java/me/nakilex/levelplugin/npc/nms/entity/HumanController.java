package me.nakilex.levelplugin.npc.nms.entity;

import com.mojang.authlib.GameProfile;
import me.nakilex.levelplugin.npc.nms.NmsImpl;
import me.nakilex.levelplugin.npc.system.NPC;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ClientInformation;
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
        GameProfile profile = new GameProfile(uuid, profileName);
        EntityHumanNPC human = new EntityHumanNPC(
                MinecraftServer.getServer(),
                level,
                profile,
                ClientInformation.createDefault(),
                npc);
        human.setPos(location.getX(), location.getY(), location.getZ());
        human.setYRot(location.getYaw());
        human.setXRot(location.getPitch());
        human.setYHeadRot(location.getYaw());
        NmsImpl.addEntityToWorld(world, level, human);
        NmsImpl.addOrRemoveFromPlayerList(level, human, true);
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
}
