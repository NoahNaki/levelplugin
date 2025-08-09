package me.nakilex.levelplugin.customscreenmenu;

import me.nakilex.levelplugin.customscreenmenu.command.CursorMenuCommand;
import me.nakilex.levelplugin.customscreenmenu.listener.MenuListener;
import me.nakilex.levelplugin.customscreenmenu.menu.MenuManager;
import me.nakilex.levelplugin.customscreenmenu.menu.SchedulerAdapter;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomScreenMenuPlugin extends JavaPlugin {
    private MenuManager menuManager;
    private SchedulerAdapter scheduler;
    private Metrics metrics;

    @Override
    public void onEnable() {
        Plugin hud = getServer().getPluginManager().getPlugin("BetterHud");
        if (hud == null || !hud.isEnabled()) {
            getLogger().severe("BetterHud not found, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.scheduler = new SchedulerAdapter(this);
        this.menuManager = new MenuManager(this, scheduler);
        this.menuManager.loadMenus();
        CursorMenuCommand cmd = new CursorMenuCommand(this);
        getCommand("cursormenu").setExecutor(cmd);
        getCommand("cursormenu").setTabCompleter(cmd);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        this.metrics = new Metrics(this, 12345);
    }

    @Override
    public void onDisable() {
        if (menuManager != null) {
            menuManager.closeAll();
        }
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public SchedulerAdapter getScheduler() {
        return scheduler;
    }
}
