package me.nakilex.levelplugin.npc.system.trait;

import me.nakilex.levelplugin.npc.system.NPC;

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
        this.updateSkins = false;
    }

    public void setSkinPersistent(NPC npc, String skinName, String signature, String texture) {
        setSkinPersistent(skinName, signature, texture);
        applyToNpc(npc, true);
    }

    public void setSkinName(NPC npc, String skinName, boolean forceUpdate) {
        this.skinName = skinName;
        if (forceUpdate) {
            this.signature = null;
            this.texture = null;
        }
        applyToNpc(npc, forceUpdate);
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

    @Override
    public void onSpawn(NPC npc) {
        applyToNpc(npc, true);
    }

    private void applyToNpc(NPC npc, boolean forceUpdate) {
        if (npc == null || npc.getCitizensNpc() == null) {
            return;
        }
        net.citizensnpcs.trait.SkinTrait citizensSkin = npc.getCitizensNpc().getOrAddTrait(net.citizensnpcs.trait.SkinTrait.class);
        if (texture != null && signature != null && !texture.isBlank() && !signature.isBlank()) {
            citizensSkin.setSkinPersistent(skinName, signature, texture);
            return;
        }
        if (skinName != null && !skinName.isBlank()) {
            citizensSkin.setSkinName(skinName, forceUpdate);
        }
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
