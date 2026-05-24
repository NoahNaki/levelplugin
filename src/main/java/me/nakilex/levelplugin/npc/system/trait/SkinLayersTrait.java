package me.nakilex.levelplugin.npc.system.trait;

import me.nakilex.levelplugin.npc.system.NPC;

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

    @Override
    public void onSpawn(NPC npc) {
        applyToNpc(npc);
    }

    @Override
    public void onTick(NPC npc) {
        applyToNpc(npc);
    }

    public Map<Layer, Boolean> getVisibilitySnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(visibleByLayer));
    }

    public byte toBitMask() {
        byte mask = 0;
        mask = applyBit(mask, Layer.CAPE, 0);
        mask = applyBit(mask, Layer.JACKET, 1);
        mask = applyBit(mask, Layer.LEFT_SLEEVE, 2);
        mask = applyBit(mask, Layer.RIGHT_SLEEVE, 3);
        mask = applyBit(mask, Layer.LEFT_PANTS, 4);
        mask = applyBit(mask, Layer.RIGHT_PANTS, 5);
        mask = applyBit(mask, Layer.HAT, 6);
        return mask;
    }

    private byte applyBit(byte mask, Layer layer, int bit) {
        if (!isVisible(layer)) {
            return mask;
        }
        return (byte) (mask | (1 << bit));
    }

    private void applyToNpc(NPC npc) {
        if (npc == null || npc.getCitizensNpc() == null) {
            return;
        }
        npc.getCitizensNpc().data().setPersistent("player-skin-layers", toBitMask());
    }
}
