package me.nakilex.levelplugin.spells.input;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.utils.BetterHudUtil;
import me.nakilex.levelplugin.utils.WorldExclusionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class SpellInputHudManager implements Listener {
    public static final String SPELL_INPUT_HUD = "spell_input";

    private static final SpellInputHudManager INSTANCE = new SpellInputHudManager();

    public static SpellInputHudManager getInstance() {
        return INSTANCE;
    }

    private SpellInputHudManager() {
    }

    public void sync(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        boolean enabled = shouldEnable(player);
        BetterHudUtil.setHud(player, SPELL_INPUT_HUD, enabled);
        if (!enabled) {
            SpellInputDisplayManager.getInstance().clear(player);
        }
    }

    public void remove(Player player) {
        if (player == null) {
            return;
        }
        BetterHudUtil.removeHud(player, SPELL_INPUT_HUD);
        SpellInputDisplayManager.getInstance().clear(player);
    }

    public void syncNextTick(Player player) {
        if (player == null) {
            return;
        }
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> sync(player));
    }

    private boolean shouldEnable(Player player) {
        if (WorldExclusionUtil.isExcluded(player)) {
            return false;
        }
        Integer activeSlot = ProfileManager.getInstance().getActiveSlot(player.getUniqueId());
        if (activeSlot == null || activeSlot < 0) {
            return false;
        }
        SettingsManager settingsManager = Main.getInstance().getSettingsManager();
        return settingsManager != null
                && settingsManager.getSettings(player).getSpellInputMode() == SpellInputMode.MOUSE_COMBO;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            if (player.isOnline()) {
                sync(player);
            }
        }, 45L);
    }
}
