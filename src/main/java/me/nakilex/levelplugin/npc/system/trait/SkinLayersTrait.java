package me.nakilex.levelplugin.npc.system.trait;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class SkinLayersTrait implements NpcTrait {
    public enum Layer {
        CAPE,
        HAT,
        JACKET,
        LEFT_SLEEVE,
        RIGHT_SLEEVE,
        LEFT_PANTS,
        RIGHT_PANTS
    }

    private final EnumMap<Layer, Boolean> visibleByLayer = new EnumMap<>(Layer.class);

    public SkinLayersTrait() {
        for (Layer layer : Layer.values()) {
            visibleByLayer.put(layer, true);
        }
    }

    public boolean isVisible(Layer layer) {
        return visibleByLayer.getOrDefault(layer, false);
    }

    public void setVisible(Layer layer, boolean visible) {
        if (layer == null) {
            return;
        }
        visibleByLayer.put(layer, visible);
    }

    public Map<Layer, Boolean> getVisibilitySnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(visibleByLayer));
    }
}
