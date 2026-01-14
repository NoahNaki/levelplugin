package me.nakilex.levelplugin.npc.nms.entity;

import me.nakilex.levelplugin.npc.nms.NmsImpl;
import me.nakilex.levelplugin.npc.system.NPC;
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
        EntityHumanNPC human = new EntityHumanNPC(
                NmsImpl.getMinecraftServer(),
                level,
                profile,
                clientInformation,
                npc);
        NmsImpl.applyPosition(human.getHandle(), location);
        NmsImpl.addEntityToWorld(world, level, human.getHandle());
        NmsImpl.addOrRemoveFromPlayerList(level, human.getHandle(), true);
        return human.getBukkitEntity();
    }

    public static void despawn(Player npcPlayer) {
        if (npcPlayer == null) {
            return;
        }
        Object level = NmsImpl.getServerLevel(npcPlayer.getWorld());
        Object handle = NmsImpl.getServerPlayer(npcPlayer);
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
