package me.nakilex.levelplugin.npc.system.trait;

public class SkinTrait implements NpcTrait {
    private String skinName;
    private String signature;
    private String texture;
    private boolean updateSkins = true;
    private boolean fetchDefaultSkin = true;
    private ModelType modelType = ModelType.WIDE;

    public enum ModelType {
        SLIM,
        WIDE
    }

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

    public void clearTexture() {
        this.signature = null;
        this.texture = null;
    }

    public boolean shouldUpdateSkins() {
        return updateSkins;
    }

    public void setShouldUpdateSkins(boolean updateSkins) {
        this.updateSkins = updateSkins;
    }

    public boolean shouldFetchDefaultSkin() {
        return fetchDefaultSkin;
    }

    public void setFetchDefaultSkin(boolean fetchDefaultSkin) {
        this.fetchDefaultSkin = fetchDefaultSkin;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType == null ? ModelType.WIDE : modelType;
    }
}
