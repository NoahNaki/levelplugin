package me.nakilex.npc.core.model;

import java.util.Objects;

public class SkinData {
    private final String texture;
    private final String signature;

    public SkinData(String texture, String signature) {
        this.texture = texture;
        this.signature = signature;
    }

    public String getTexture() {
        return texture;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SkinData other)) {
            return false;
        }
        return Objects.equals(texture, other.texture) && Objects.equals(signature, other.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texture, signature);
    }
}
