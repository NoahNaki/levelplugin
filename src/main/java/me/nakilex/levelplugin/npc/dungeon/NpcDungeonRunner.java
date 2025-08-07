package me.nakilex.levelplugin.npc.dungeon;

import me.nakilex.levelplugin.dungeon.Dungeon;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Function;

/**
 * Drives an NPC through a dungeon by moving between rooms and triggering
 * Denizen scripts to handle combat for each room.
 */
public class NpcDungeonRunner {

    private final Plugin plugin;
    private final NPC npc;
    private final Dungeon dungeon;
    private final Function<Dungeon.RoomInstance, String> scriptProvider;
    private final Function<Dungeon.RoomInstance, RoomClearCondition> conditionFactory;
    private final long checkInterval;
    private int currentIndex;

    public NpcDungeonRunner(Plugin plugin,
                            NPC npc,
                            Dungeon dungeon,
                            Function<Dungeon.RoomInstance, String> scriptProvider,
                            Function<Dungeon.RoomInstance, RoomClearCondition> conditionFactory,
                            long checkInterval) {
        this.plugin = plugin;
        this.npc = npc;
        this.dungeon = dungeon;
        this.scriptProvider = scriptProvider;
        this.conditionFactory = conditionFactory;
        this.checkInterval = checkInterval;
    }

    /**
     * Begin the dungeon run.
     */
    public void start() {
        currentIndex = 0;
        proceed();
    }

    private void proceed() {
        if (currentIndex >= dungeon.getRooms().size()) {
            return;
        }
        Dungeon.RoomInstance room = dungeon.getRooms().get(currentIndex);
        npc.getNavigator().setTarget(room.center);
        String script = scriptProvider.apply(room);
        if (script != null && !script.isEmpty()) {
            DenizenScriptHelper.runScript(script, npc);
        }
        RoomClearCondition condition = conditionFactory.apply(room);
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (condition.isMet()) {
                task[0].cancel();
                currentIndex++;
                proceed();
            }
        }, checkInterval, checkInterval);
    }
}
