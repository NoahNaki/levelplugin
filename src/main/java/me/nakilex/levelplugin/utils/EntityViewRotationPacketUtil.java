package me.nakilex.levelplugin.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Sends per-viewer rotation packets so an entity can appear with different yaw/pitch
 * to each player without changing the server-side entity rotation.
 */
public final class EntityViewRotationPacketUtil {

    private EntityViewRotationPacketUtil() {
    }

    public static void sendEntityLook(ProtocolManager protocolManager,
                                      Player viewer,
                                      Entity entity,
                                      float yaw,
                                      float pitch) {
        if (protocolManager == null || viewer == null || entity == null || !viewer.isOnline()) {
            return;
        }

        try {
            PacketContainer lookPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_LOOK);
            lookPacket.getIntegers().write(0, entity.getEntityId());
            lookPacket.getBytes().write(0, angleToByte(yaw));
            lookPacket.getBytes().write(1, angleToByte(pitch));
            lookPacket.getBooleans().write(0, entity.isOnGround());
            protocolManager.sendServerPacket(viewer, lookPacket);

            PacketContainer headPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            headPacket.getIntegers().write(0, entity.getEntityId());
            headPacket.getBytes().write(0, angleToByte(yaw));
            protocolManager.sendServerPacket(viewer, headPacket);
        } catch (Exception ignored) {
        }
    }

    public static void sendFaceLocation(ProtocolManager protocolManager,
                                        Player viewer,
                                        Entity entity,
                                        Location target) {
        if (entity == null || target == null) {
            return;
        }
        Location source = entity.getLocation();
        float yaw = MobUtil.lookYaw(source, target);
        float pitch = MobUtil.lookPitch(source, target);
        sendEntityLook(protocolManager, viewer, entity, yaw, pitch);
    }

    private static byte angleToByte(float degrees) {
        return (byte) Math.floorMod(Math.round(degrees * 256.0f / 360.0f), 256);
    }
}
