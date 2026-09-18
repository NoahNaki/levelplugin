package me.nakilex.levelplugin.playerhead;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;

import java.util.UUID;

/** Builds the RGB-marked player object consumed by the bundled text shader. */
public final class PlayerModelComponent {
    private static final int MARKER_RED_HIGH_NIBBLE = 0x70;
    private static final double SPEED_STEPS_PER_MULTIPLIER = 4.0;

    /**
     * The layout advance of the underlying player-head object. The shader grows the quad out of an
     * 8x8 base ({@code position.xy += corner * (pixelSize - vec2(8.0))}) but never touches the text
     * cursor, so the line only ever moves on by these 8 pixels no matter how large the model is.
     */
    public static final int OBJECT_ADVANCE_PIXELS = 8;

    private PlayerModelComponent() {
    }

    public enum Type {
        HEAD(0, 10, 10), BUST(1, 18, 22), FULL(2, 18, 34);

        private final int id;
        private final int unitWidth;
        private final int unitHeight;

        Type(int id, int unitWidth, int unitHeight) {
            this.id = id;
            this.unitWidth = unitWidth;
            this.unitHeight = unitHeight;
        }

        /** Model footprint in GUI pixels at the given scale, matching the vertex shader's modelUnits. */
        public int pixelWidth(int scale) {
            return unitWidth * Math.max(1, scale);
        }

        public int pixelHeight(int scale) {
            return unitHeight * Math.max(1, scale);
        }
    }

    public enum Animation {
        IDLE(0), WAVE(1), WALK(2), RUN(3), ATTACK(4), CROUCH(5);

        private final int id;

        Animation(int id) {
            this.id = id;
        }
    }

    public static Component create(UUID playerId, Type type, int verticalOffset, int scale) {
        return create(playerId, type, verticalOffset, scale, 1.0, Animation.IDLE);
    }

    public static Component create(UUID playerId, Type type, int verticalOffset, int scale,
                                   double speedMultiplier, Animation animation) {
        if (playerId == null) throw new IllegalArgumentException("playerId cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (animation == null) throw new IllegalArgumentException("animation cannot be null");
        if (verticalOffset < 0 || verticalOffset > 15) {
            throw new IllegalArgumentException("verticalOffset must be between 0 and 15");
        }
        if (scale < 1 || scale > 15) {
            throw new IllegalArgumentException("scale must be between 1 and 15");
        }
        if (!Double.isFinite(speedMultiplier) || speedMultiplier < 0.0 || speedMultiplier > 4.0) {
            throw new IllegalArgumentException("speedMultiplier must be between 0.0 and 4.0");
        }

        return Component.object(ObjectContents.playerHead(playerId))
                .color(TextColor.color(marker(type, verticalOffset, scale, speedMultiplier, animation)));
    }

    /**
     * The same portrait built from a skin we hold directly rather than from the account behind
     * {@code playerId}. Spoofed players wear a donor account's skin, so resolving the skin from
     * their UUID would show a different face in the card than the one standing in the world.
     */
    public static Component create(UUID playerId, String name, String skinTexture, String skinSignature,
                                   Type type, int verticalOffset, int scale,
                                   double speedMultiplier, Animation animation) {
        if (skinTexture == null || skinTexture.isBlank()) {
            return create(playerId, type, verticalOffset, scale, speedMultiplier, animation);
        }
        PlayerHeadObjectContents contents = ObjectContents.playerHead()
                .id(playerId)
                .name(name)
                .profileProperty(PlayerHeadObjectContents.property("textures", skinTexture, skinSignature))
                .build();
        return Component.object(contents)
                .color(TextColor.color(marker(type, verticalOffset, scale, speedMultiplier, animation)));
    }

    /** Packs the render settings into the marker colour the resource pack's shader reads. */
    private static int marker(Type type, int verticalOffset, int scale,
                              double speedMultiplier, Animation animation) {
        int speedStep = Math.min(15, (int) Math.round(speedMultiplier * SPEED_STEPS_PER_MULTIPLIER));
        int red = MARKER_RED_HIGH_NIBBLE | speedStep;
        int green = (type.id << 4) | verticalOffset;
        int blue = (scale << 4) | animation.id;
        return (red << 16) | (green << 8) | blue;
    }
}
