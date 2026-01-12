package me.nakilex.npc.nms.v1_21_3;

import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.model.SkinData;
import me.nakilex.npc.core.nms.NmsBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class NmsBridgeV1_21_3 implements NmsBridge {
    private final Plugin plugin;

    public NmsBridgeV1_21_3(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void spawnNpc(Npc npc) {
        if (npc.getType() == EntityType.PLAYER) {
            throw new UnsupportedOperationException("Player NPC spawning requires NMS in v1_21_3");
        }
        Location location = npc.getPosition() == null ? null : npc.getPosition().toLocation();
        if (location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Entity entity = world.spawnEntity(location, npc.getType());
        npc.setUuid(entity.getUniqueId());
        npc.setEntityId(entity.getEntityId());
        applyFlags(npc, entity);
    }

    @Override
    public void despawnNpc(Npc npc) {
        Entity entity = getEntity(npc);
        if (entity != null) {
            entity.remove();
        }
        npc.setEntityId(null);
        npc.setUuid(null);
    }

    @Override
    public void teleportNpc(Npc npc, Location location) {
        Entity entity = getEntity(npc);
        if (entity != null && location != null) {
            entity.teleport(location);
        }
    }

    @Override
    public void setRotation(Npc npc, float yaw, float pitch) {
        Entity entity = getEntity(npc);
        if (entity != null) {
            Location location = entity.getLocation();
            location.setYaw(yaw);
            location.setPitch(pitch);
            entity.teleport(location);
        }
    }

    @Override
    public void setNameplateVisible(Npc npc, boolean visible) {
        Entity entity = getEntity(npc);
        if (entity != null) {
            entity.setCustomNameVisible(visible);
        }
    }

    @Override
    public void setCollision(Npc npc, boolean collidable) {
        Entity entity = getEntity(npc);
        if (entity != null) {
            entity.setCollidable(collidable);
        }
    }

    @Override
    public void setPushable(Npc npc, boolean pushable) {
        Entity entity = getEntity(npc);
        if (entity instanceof LivingEntity living) {
            living.setAI(pushable);
        }
    }

    @Override
    public void setGlowing(Npc npc, Player viewer, boolean glowing) {
        Entity entity = getEntity(npc);
        if (entity != null) {
            entity.setGlowing(glowing);
        }
    }

    @Override
    public void showNpc(Npc npc, Player viewer) {
        Entity entity = getEntity(npc);
        if (entity != null) {
            viewer.showEntity(plugin, entity);
        }
    }

    @Override
    public void hideNpc(Npc npc, Player viewer) {
        Entity entity = getEntity(npc);
        if (entity != null) {
            viewer.hideEntity(plugin, entity);
        }
    }

    @Override
    public void setTablist(Npc npc, boolean enabled) {
        // Only applies to player NPCs.
    }

    @Override
    public void applySkin(Npc npc, SkinData skinData) {
        // Implemented for player NPCs via NMS.
    }

    private void applyFlags(Npc npc, Entity entity) {
        entity.setInvulnerable(npc.getFlags().isInvulnerable());
        entity.setCollidable(npc.getFlags().isCollidable());
        entity.setCustomNameVisible(npc.getFlags().isVisible());
        entity.customName(Component.text(npc.getName()));
    }

    private Entity getEntity(Npc npc) {
        UUID uuid = npc.getUuid();
        if (uuid != null) {
            return Bukkit.getEntity(uuid);
        }
        return null;
    }
}
