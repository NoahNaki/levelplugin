package me.nakilex.npc.core.nms;

import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.model.SkinData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NmsBridge {
    void spawnNpc(Npc npc);

    void despawnNpc(Npc npc);

    void teleportNpc(Npc npc, Location location);

    void setRotation(Npc npc, float yaw, float pitch);

    void setNameplateVisible(Npc npc, boolean visible);

    void setCollision(Npc npc, boolean collidable);

    void setPushable(Npc npc, boolean pushable);

    void setGlowing(Npc npc, Player viewer, boolean glowing);

    void showNpc(Npc npc, Player viewer);

    void hideNpc(Npc npc, Player viewer);

    void setTablist(Npc npc, boolean enabled);

    void applySkin(Npc npc, SkinData skinData);
}
