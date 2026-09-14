package me.nakilex.levelplugin.animatedlb;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;

public record RowDisplay(TextDisplay left,
                         TextDisplay right,
                         Vector leftBaseOffset,
                         Vector rightBaseOffset,
                         int index) {

    public void setText(String leftText, String rightText) {
        left.setText(leftText);
        right.setText(rightText);
    }

    /** Overload for a left side that mixes fonts (e.g. a player-head glyph run) and can't be a plain legacy string. */
    public void setText(Component leftText, String rightText) {
        left.text(leftText);
        right.setText(rightText);
    }

    public void teleportToBase(Location origin) {
        left.teleport(origin.clone().add(leftBaseOffset));
        right.teleport(origin.clone().add(rightBaseOffset));
    }

    public void teleportWithOffset(Location origin, Vector slideOffset) {
        left.teleport(origin.clone().add(leftBaseOffset).add(slideOffset));
        right.teleport(origin.clone().add(rightBaseOffset).add(slideOffset));
    }

    public void setOpacity(byte opacity) {
        left.setTextOpacity(opacity);
        right.setTextOpacity(opacity);
    }
}
