package me.nakilex.levelplugin.screen;

import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

/**
 * Simple manager for spawning and tracking {@link TextDisplay}s in front of players.
 */
public class TextDisplayManager extends AbstractDisplayManager<TextDisplay> {

    /**
     * Show a new {@link TextDisplay} for the given player, replacing any existing one.
     *
     * @param player viewer
     * @param location spawn location
     * @param text text content
     * @return spawned display
     */
    public TextDisplay show(Player player, Location location, String text) {
        hide(player);
        TextDisplay display = DisplayUtil.spawn(location, TextDisplay.class, td -> {
            td.setText(text);
            td.setBillboard(Display.Billboard.CENTER);
            td.setShadowRadius(0);
            td.setShadowStrength(0);
        });
        activeDisplays.put(player.getUniqueId(), display);
        return display;
    }
}
