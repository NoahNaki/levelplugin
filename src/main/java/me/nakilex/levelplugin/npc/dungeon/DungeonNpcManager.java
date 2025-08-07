package me.nakilex.levelplugin.npc.dungeon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Handles spawning and tracking dungeon-clearing NPCs per player.
 */
public class DungeonNpcManager {

    private final Main plugin;
    private final Map<UUID, ActiveNpc> active = new HashMap<>();

    public DungeonNpcManager(Main plugin) {
        this.plugin = plugin;
    }

    private record ActiveNpc(NPC npc, NpcDungeonRunner runner) {}

    /**
     * Spawn a dungeon NPC for the given player using default behavior.
     *
     * @return true if the NPC was spawned
     */
    public boolean spawn(Player player) {
        return spawn(player, room -> "dungeon_clear", room -> new TagClearCondition(room, "dungeon_mob"), 20L);
    }

    /**
     * Spawn a dungeon NPC with custom script and completion logic.
     */
    public boolean spawn(Player player,
                         Function<Dungeon.RoomInstance, String> scriptProvider,
                         Function<Dungeon.RoomInstance, RoomClearCondition> conditionFactory,
                         long checkInterval) {
        UUID id = player.getUniqueId();
        if (active.containsKey(id)) return false;
        DungeonManager dm = plugin.getDungeonManager();
        Dungeon dungeon = dm.getDungeon(player.getWorld());
        if (dungeon == null) return false;

        NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "DungeonRunner");
        npc.spawn(player.getLocation());

        NpcDungeonRunner runner = new NpcDungeonRunner(plugin, npc, dungeon, scriptProvider, conditionFactory, checkInterval);
        runner.start();
        active.put(id, new ActiveNpc(npc, runner));
        return true;
    }

    /**
     * Despawn the player's dungeon NPC if present.
     *
     * @return true if an NPC was despawned
     */
    public boolean despawn(Player player) {
        ActiveNpc data = active.remove(player.getUniqueId());
        if (data == null) return false;
        data.npc.destroy();
        return true;
    }
}

