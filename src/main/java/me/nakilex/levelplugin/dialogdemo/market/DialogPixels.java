package me.nakilex.levelplugin.dialogdemo.market;

import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.glyphs.Glyph;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Pixel-level layout primitives for glyph art inside a native dialog.
 *
 * Shared by every screen that paints art rather than stacking text, because
 * each of these encodes a rule that is not obvious and is expensive to
 * rediscover. See {@code docs/native_dialog_demo.md} for the derivations.
 */
public final class DialogPixels {

    private DialogPixels() {
    }

    /** Text lines advance 9 logical pixels. Everything vertical follows from this. */
    public static final int LINE_HEIGHT = 9;

    /**
     * Our own space font, at {@code assets/minecraft/font/space.json}.
     *
     * Nexo's {@code Shift} glyphs live in its generated {@code default.json},
     * which this server fails to deserialize, so they render as missing-glyph
     * marks. This is a separate file with its own codepoints and is unaffected.
     */
    private static final Key SPACE_FONT = Key.key("minecraft:space");
    private static final int SHIFT_NEG_BASE = 0xF800;
    private static final int SHIFT_POS_BASE = 0xF810;
    private static final int[] SHIFT_STEPS = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024};

    /**
     * A horizontal offset in GUI pixels.
     *
     * Powers of two from 1 to 1024 exist in both signs, so every integer in
     * range decomposes exactly -- no rounding and no accumulation.
     */
    public static Component shift(int pixels) {
        if (pixels == 0) {
            return Component.empty();
        }
        int base = pixels < 0 ? SHIFT_NEG_BASE : SHIFT_POS_BASE;
        int remaining = Math.abs(pixels);
        StringBuilder chars = new StringBuilder();
        for (int i = SHIFT_STEPS.length - 1; i >= 0 && remaining > 0; i--) {
            while (remaining >= SHIFT_STEPS[i]) {
                chars.appendCodePoint(base + i);
                remaining -= SHIFT_STEPS[i];
            }
        }
        return Component.text(chars.toString()).font(SPACE_FONT);
    }

    /** Resolve a Nexo glyph by id, or a visible marker if the pack lacks it. */
    public static Component glyph(String glyphId) {
        Glyph glyph = NexoPlugin.instance().fontManager().glyphFromID(glyphId);
        if (glyph == null) {
            return Component.text("[missing:" + glyphId + "]", NamedTextColor.RED);
        }
        return glyph.glyphComponent().color(NamedTextColor.WHITE);
    }

    /**
     * The ascent that puts art of the given height with its top at {@code y}
     * when emitted on {@code line}, or -1 if the art does not reach that line.
     *
     * A glyph's top lands at (baseline - ascent) and lines advance 9px. The
     * window slices pin the constant: they sit at art y = line*9 with ascent 9.
     * So {@code ascent = line*9 + 9 - y}, independent of height.
     *
     * Callers emit art on every line it overlaps, because a dialog hit-tests a
     * click to a single 9px strip at the line's baseline -- one emission leaves
     * most of a tall glyph dead to the cursor.
     */
    public static int ascentFor(int y, int height, int line) {
        // Line L's strip covers art rows [L*9+2, L*9+11]; art covers [y, y+h].
        int strip = line * LINE_HEIGHT;
        if (strip <= y - 11 || strip >= y + height - 2) {
            return -1;
        }
        int ascent = strip + LINE_HEIGHT - y;
        return (ascent >= 1 && ascent <= height + 8) ? ascent : -1;
    }

    /** The last line this art reaches -- where it must be drawn to sit on top. */
    public static int lastLineFor(int y, int height, int lineCount) {
        int last = -1;
        for (int line = 0; line < lineCount; line++) {
            if (ascentFor(y, height, line) >= 0) {
                last = line;
            }
        }
        return last;
    }

    /**
     * A hover tooltip drawn with a resource-pack frame instead of the vanilla
     * purple box.
     *
     * The frame comes from the {@code tooltip_style} item component, so it is
     * only available to *item* hovers -- a plain {@code showText} always
     * renders vanilla. Wrapping the text in a throwaway item unlocks it. Any
     * material works: a hover tooltip never draws the item's icon.
     */
    public static HoverEvent<HoverEvent.ShowItem> styledTooltip(Component title, List<Component> lore, String style) {
        ItemStack carrier = new ItemStack(Material.PAPER);
        ItemMeta meta = carrier.getItemMeta();
        if (meta != null) {
            meta.displayName(title.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
            carrier.setItemMeta(meta);
        }
        ItemUtil.setKeyedComponent(carrier, DataComponentTypes.TOOLTIP_STYLE, Key.key("minecraft:" + style));
        return carrier.asHoverEvent();
    }
}
