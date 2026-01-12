package me.nakilex.npc.plugin;

import me.nakilex.npc.plugin.command.NpcCommand;
import me.nakilex.npc.plugin.listener.NpcInteractionListener;
import me.nakilex.npc.plugin.service.NpcService;
import org.bukkit.plugin.java.JavaPlugin;

public class NpcPlugin extends JavaPlugin {
    private NpcService npcService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        npcService = new NpcService(this);
        npcService.start();
        NpcCommand command = new NpcCommand(npcService);
        getCommand("npc").setExecutor(command);
        getCommand("npc").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new NpcInteractionListener(npcService), this);
    }

    @Override
    public void onDisable() {
        if (npcService != null) {
            npcService.stop();
        }
    }
}
