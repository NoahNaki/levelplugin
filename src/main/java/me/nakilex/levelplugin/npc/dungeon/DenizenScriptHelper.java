package me.nakilex.levelplugin.npc.dungeon;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Utility to trigger Denizen scripts for an NPC.
 */
public final class DenizenScriptHelper {

    private DenizenScriptHelper() {
    }

    /**
     * Run the specified Denizen script with the given NPC as context.
     *
     * @param scriptName name of the Denizen script to execute
     * @param npc        the NPC used for script context
     */
    public static void runScript(String scriptName, NPC npc) {
        Plugin denizen = Bukkit.getPluginManager().getPlugin("Denizen");
        if (denizen == null || !denizen.isEnabled()) {
            return;
        }
        String command = "ex run s:" + scriptName + " npc:" + npc.getId();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
