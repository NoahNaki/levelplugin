package me.nakilex.levelplugin.npc.system.trait;

import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class RotationTrait implements NpcTrait {
    private float yaw;
    private float pitch;
    private boolean headOnly;
    private boolean linkedBody = true;

    public void rotateTo(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void rotateToFace(Location target, NPC npc) {
        if (target == null || npc == null || npc.getEntity() == null) {
            return;
        }
        Location source = npc.getEntity().getLocation();
        double dx = target.getX() - source.getX();
        double dy = target.getY() - source.getY();
        double dz = target.getZ() - source.getZ();
        double xz = Math.sqrt(dx * dx + dz * dz);
        this.yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        this.pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.max(0.0001, xz)));
    }

    @Override
    public void onTick(NPC npc) {
        Entity entity = npc.getEntity();
        if (entity == null || !entity.isValid()) {
            return;
        }
        Location current = entity.getLocation();
        current.setYaw(yaw);
        current.setPitch(pitch);
        entity.teleport(current);
    }

    public boolean isHeadOnly() {
        return headOnly;
    }

    public void setHeadOnly(boolean headOnly) {
        this.headOnly = headOnly;
    }

    public boolean isLinkedBody() {
        return linkedBody;
    }

    public void setLinkedBody(boolean linkedBody) {
        this.linkedBody = linkedBody;
    }
}
