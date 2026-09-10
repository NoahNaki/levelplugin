package me.nakilex.levelplugin.dialogdemo.market;

/**
 * Every number that positions the market grid, in one mutable place.
 *
 * All of these are *server-side* values, so they take effect on the next
 * render with no restart and no pack rebuild -- that is the whole point of
 * the in-game editor. The one thing that is not tunable this way is a
 * glyph's baked ascent, which is why {@link #iconSize} selects between
 * pre-baked variants rather than setting a number the client reads.
 *
 * Coordinates are in logical GUI pixels, measured from the top-left of the
 * window art, with +y downward -- i.e. the same frame the texture is drawn
 * in, so a value read off the PNG can be typed straight in.
 */
public final class MarketLayout {

    public static final int COLS = 5;
    public static final int ROWS = 3;
    public static final int SLOTS = COLS * ROWS;

    /** Icon sizes baked into market.yml; nothing else will resolve. */
    public static final int[] BAKED_SIZES = {16, 24};

    /**
     * Top-left of the *first* slot's icon, relative to the window art.
     *
     * Measured from the texture by scratchpad/fit_grid.py: the slot fills sit
     * at x 40,78,116,154,192 and y 87,128,169, each 25x27, so a 16px icon
     * centres at (44, 92).
     */
    public int originX = 44;
    public int originY = 92;

    /**
     * Centre-to-centre spacing between adjacent slots.
     *
     * Both came out exact off the texture -- 38 across, 41 down, with no
     * rounding -- so a drifting row is a sign of a wrong advance, not of
     * these.
     */
    public int pitchX = 38;
    public int pitchY = 41;

    /** Which pre-baked icon size to use. Must be one of {@link #BAKED_SIZES}. */
    public int iconSize = 16;

    /**
     * A correction added to every good's own advance.
     *
     * Each {@link MarketGood} carries its measured advance, because the icons
     * are not all square and one shared constant would leave everything after
     * a non-square icon drifting. This is only the global nudge for when the
     * measurement itself is off; normally it stays 0.
     */
    public int advanceAdjust = 0;

    /** Where the window art itself sits, if it needs nudging off centre. */
    public int windowX = 0;
    public int windowY = 0;

    /** Per-slot correction, applied on top of the grid. Index 0..14, row-major. */
    public final int[] nudgeX = new int[SLOTS];
    public final int[] nudgeY = new int[SLOTS];

    public int slotX(int index) {
        return windowX + originX + (index % COLS) * pitchX + nudgeX[index];
    }

    public int slotY(int index) {
        return windowY + originY + (index / COLS) * pitchY + nudgeY[index];
    }

    public boolean sizeIsBaked(int size) {
        for (int baked : BAKED_SIZES) {
            if (baked == size) return true;
        }
        return false;
    }

    public void reset() {
        MarketLayout fresh = new MarketLayout();
        originX = fresh.originX; originY = fresh.originY;
        pitchX = fresh.pitchX;   pitchY = fresh.pitchY;
        iconSize = fresh.iconSize; advanceAdjust = fresh.advanceAdjust;
        windowX = fresh.windowX; windowY = fresh.windowY;
        java.util.Arrays.fill(nudgeX, 0);
        java.util.Arrays.fill(nudgeY, 0);
    }
}
