package me.nakilex.levelplugin.npc.system;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class NpcPlayerRenderer {
    private static final ProtocolManager PROTOCOL = ProtocolLibrary.getProtocolManager();

    private NpcPlayerRenderer() {
    }

    public static void spawnForAll(NPC npc) {
        if (npc == null || npc.getType() != org.bukkit.entity.EntityType.PLAYER || !npc.isSpawned()) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            spawnFor(viewer, npc);
        }
    }

    public static void spawnFor(Player viewer, NPC npc) {
        if (npc == null || npc.getType() != org.bukkit.entity.EntityType.PLAYER || !canRender(viewer, npc)) {
            return;
        }
        UUID uuid = npc.getPlayerNpcUuid();
        int entityId = npc.getPlayerNpcEntityId();
        Location loc = npc.getEntity().getLocation();
        WrappedGameProfile profile = buildProfile(npc, uuid);
        sendPlayerInfoAdd(viewer, profile);
        sendSpawnPlayer(viewer, entityId, uuid, loc);
        sendHeadRotation(viewer, entityId, loc.getYaw());
        scheduleTabListRemoval(viewer, uuid);
    }

    public static void despawnForAll(NPC npc) {
        if (npc == null || npc.getType() != org.bukkit.entity.EntityType.PLAYER || npc.getPlayerNpcUuid() == null) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            despawnFor(viewer, npc);
        }
    }

    public static void despawnFor(Player viewer, NPC npc) {
        if (npc == null || viewer == null) {
            return;
        }
        int entityId = npc.getPlayerNpcEntityId();
        UUID uuid = npc.getPlayerNpcUuid();
        sendDestroy(viewer, entityId);
        sendPlayerInfoRemove(viewer, uuid);
    }

    public static void teleportForAll(NPC npc) {
        if (npc == null || npc.getType() != org.bukkit.entity.EntityType.PLAYER || !npc.isSpawned()) {
            return;
        }
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            teleportFor(viewer, npc);
        }
    }

    public static void teleportFor(Player viewer, NPC npc) {
        if (!canRender(viewer, npc)) {
            return;
        }
        int entityId = npc.getPlayerNpcEntityId();
        Location loc = npc.getEntity().getLocation();
        PacketContainer packet = PROTOCOL.createPacket(com.comphenix.protocol.PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, entityId);
        packet.getDoubles().write(0, loc.getX());
        packet.getDoubles().write(1, loc.getY());
        packet.getDoubles().write(2, loc.getZ());
        packet.getBytes().write(0, angleToByte(loc.getYaw()));
        packet.getBytes().write(1, angleToByte(loc.getPitch()));
        packet.getBooleans().write(0, true);
        sendPacket(viewer, packet);
        sendHeadRotation(viewer, entityId, loc.getYaw());
    }

    private static boolean canRender(Player viewer, NPC npc) {
        if (viewer == null || npc == null || npc.getEntity() == null) {
            return false;
        }
        if (!viewer.isOnline()) {
            return false;
        }
        return viewer.getWorld().equals(npc.getEntity().getWorld());
    }

    private static WrappedGameProfile buildProfile(NPC npc, UUID uuid) {
        String name = npc.getName();
        String stripped = ChatColor.stripColor(name == null ? "" : name);
        if (stripped.isBlank()) {
            stripped = "NPC";
        }
        if (stripped.length() > 16) {
            stripped = stripped.substring(0, 16);
        }
        WrappedGameProfile profile = new WrappedGameProfile(uuid, stripped);
        SkinTrait trait = npc.getSkinTrait();
        if (trait != null && trait.getTexture() != null && trait.getSignature() != null) {
            profile.getProperties().put("textures",
                    new WrappedSignedProperty("textures", trait.getTexture(), trait.getSignature()));
        }
        return profile;
    }

    private static void sendPlayerInfoAdd(Player viewer, WrappedGameProfile profile) {
        PacketContainer packet = PROTOCOL.createPacket(com.comphenix.protocol.PacketType.Play.Server.PLAYER_INFO);
        try {
            writePlayerInfoActions(packet, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
        } catch (RuntimeException ex) {
            Main.getInstance().getLogger().warning("Failed to set NPC player info actions: " + ex.getMessage());
        }
        PlayerInfoData data = new PlayerInfoData(
                profile,
                0,
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText(profile.getName())
        );
        packet.getPlayerInfoDataLists().write(0, List.of(data));
        sendPacket(viewer, packet);
    }

    private static void sendPlayerInfoRemove(Player viewer, UUID uuid) {
        if (uuid == null) {
            return;
        }
        PacketContainer packet = PROTOCOL.createPacket(com.comphenix.protocol.PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packet.getUUIDLists().write(0, List.of(uuid));
        sendPacket(viewer, packet);
    }

    private static void writePlayerInfoActions(PacketContainer packet, EnumSet<EnumWrappers.PlayerInfoAction> actions) {
        try {
            if (writeActionsViaHandle(packet, actions)) {
                return;
            }
            com.comphenix.protocol.reflect.StructureModifier<EnumSet<?>> enumSets =
                    packet.getModifier().withType(EnumSet.class);
            if (enumSets.size() > 0) {
                enumSets.write(0, actions);
                return;
            }
            packet.getPlayerInfoActions().write(0, actions);
        } catch (RuntimeException ex) {
            Main.getInstance().getLogger().warning("Failed to set NPC player info actions for packet "
                    + packet.getType() + ": " + ex.getMessage());
        }
    }

    private static boolean writeActionsViaHandle(PacketContainer packet, EnumSet<EnumWrappers.PlayerInfoAction> actions) {
        if (packet == null) {
            return false;
        }
        Object handle = packet.getHandle();
        if (handle == null) {
            return false;
        }
        for (Field field : handle.getClass().getDeclaredFields()) {
            if (!EnumSet.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(handle, actions);
                return true;
            } catch (IllegalAccessException ignored) {
                return false;
            }
        }
        return false;
    }

    private static void sendSpawnPlayer(Player viewer, int entityId, UUID uuid, Location loc) {
        PacketContainer packet = PROTOCOL.createPacket(com.comphenix.protocol.PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, uuid);
        packet.getDoubles().write(0, loc.getX());
        packet.getDoubles().write(1, loc.getY());
        packet.getDoubles().write(2, loc.getZ());
        packet.getBytes().write(0, angleToByte(loc.getYaw()));
        packet.getBytes().write(1, angleToByte(loc.getPitch()));
        sendPacket(viewer, packet);
    }

    private static void sendDestroy(Player viewer, int entityId) {
        PacketContainer packet = PROTOCOL.createPacket(com.comphenix.protocol.PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists().write(0, List.of(entityId));
        sendPacket(viewer, packet);
    }

    private static void sendHeadRotation(Player viewer, int entityId, float yaw) {
        PacketContainer packet = PROTOCOL.createPacket(com.comphenix.protocol.PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        packet.getIntegers().write(0, entityId);
        packet.getBytes().write(0, angleToByte(yaw));
        sendPacket(viewer, packet);
    }

    private static void scheduleTabListRemoval(Player viewer, UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                sendPlayerInfoRemove(viewer, uuid);
            }
        }.runTaskLater(Main.getInstance(), 20L);
    }

    private static byte angleToByte(float angle) {
        return (byte) ((angle % 360) * 256 / 360);
    }

    private static void sendPacket(Player viewer, PacketContainer packet) {
        try {
            PROTOCOL.sendServerPacket(viewer, packet);
        } catch (Exception ex) {
            Main.getInstance().getLogger().warning("Failed to send NPC player packet: " + ex.getMessage());
        }
    }
}
