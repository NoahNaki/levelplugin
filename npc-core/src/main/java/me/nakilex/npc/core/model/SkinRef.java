package me.nakilex.npc.core.model;

import java.util.Objects;

public class SkinRef {
    private SkinSource source;
    private String value;
    private String signature;

    public SkinRef(SkinSource source, String value, String signature) {
        this.source = source;
        this.value = value;
        this.signature = signature;
    }

    public SkinSource getSource() {
        return source;
    }

    public void setSource(SkinSource source) {
        this.source = source;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isTexturePair() {
        return source == SkinSource.TEXTURES && value != null && signature != null;
    }

    public SkinRef copy() {
        return new SkinRef(source, value, signature);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SkinRef other)) {
            return false;
        }
        return source == other.source
                && Objects.equals(value, other.value)
                && Objects.equals(signature, other.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, value, signature);
    }
}
