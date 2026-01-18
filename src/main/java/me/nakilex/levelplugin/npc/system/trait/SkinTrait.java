package me.nakilex.levelplugin.npc.system.trait;

public class SkinTrait implements NpcTrait {
    private String skinName;
    private String signature;
    private String texture;

    public void setSkinPersistent(String skinName, String signature, String texture) {
        this.skinName = skinName;
        this.signature = signature;
        this.texture = texture;
    }

    public String getTexture() {
        return texture;
    }

    public String getSignature() {
        return signature;
    }

    public String getSkinName() {
        return skinName;
    }
}
