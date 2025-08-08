package me.nakilex.levelplugin.cursormenu.display;

import me.nakilex.levelplugin.cursormenu.util.ColorParser;
import me.nakilex.levelplugin.utils.HologramUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Manager for creating simple text displays for players.
 */
public class TextDisplayManager extends AbstractDisplayManager<org.bukkit.entity.TextDisplay> {

    @Override
    public void show(Player player, Object data) {
        if (!(data instanceof String text)) return;
        hide(player);
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(2));
        org.bukkit.entity.TextDisplay display = HologramUtil.spawnTextDisplay(loc, ColorParser.parse(text));
        activeDisplays.put(player.getUniqueId(), display);
    }
}
