package me.nakilex.npc.core.model;

import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Npc {
    private final int id;
    private String name;
    private EntityType type;
    private NpcPosition position;
    private NpcPosition homePosition;
    private UUID uuid;
    private Integer entityId;
    private final NpcFlags flags = new NpcFlags();
    private double viewRange = 32.0;
    private SkinRef skinRef;
    private final NpcHologramConfig hologramConfig = new NpcHologramConfig();
    private final NpcEquipment equipment = new NpcEquipment();
    private String modelEngineId;
    private final NpcCombatConfig combatConfig = new NpcCombatConfig();
    private final Map<String, Map<String, Object>> traitData = new HashMap<>();

    public Npc(int id, String name, EntityType type, NpcPosition position) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public NpcPosition getPosition() {
        return position;
    }

    public void setPosition(NpcPosition position) {
        this.position = position;
    }

    public NpcPosition getHomePosition() {
        return homePosition;
    }

    public void setHomePosition(NpcPosition homePosition) {
        this.homePosition = homePosition;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public NpcFlags getFlags() {
        return flags;
    }

    public double getViewRange() {
        return viewRange;
    }

    public void setViewRange(double viewRange) {
        this.viewRange = viewRange;
    }

    public SkinRef getSkinRef() {
        return skinRef;
    }

    public void setSkinRef(SkinRef skinRef) {
        this.skinRef = skinRef;
    }

    public NpcHologramConfig getHologramConfig() {
        return hologramConfig;
    }

    public NpcEquipment getEquipment() {
        return equipment;
    }

    public String getModelEngineId() {
        return modelEngineId;
    }

    public void setModelEngineId(String modelEngineId) {
        this.modelEngineId = modelEngineId;
    }

    public NpcCombatConfig getCombatConfig() {
        return combatConfig;
    }

    public Map<String, Map<String, Object>> getTraitData() {
        return Collections.unmodifiableMap(traitData);
    }

    public void setTraitData(String traitId, Map<String, Object> data) {
        if (data == null) {
            traitData.remove(traitId);
        } else {
            traitData.put(traitId, new HashMap<>(data));
        }
    }

    public Map<String, Object> getTraitData(String traitId) {
        Map<String, Object> data = traitData.get(traitId);
        return data == null ? null : new HashMap<>(data);
    }

    public Npc copy(int newId, String newName) {
        Npc copy = new Npc(newId, newName, type, position == null ? null : position.copy());
        copy.setHomePosition(homePosition == null ? null : homePosition.copy());
        copy.setViewRange(viewRange);
        copy.setSkinRef(skinRef == null ? null : skinRef.copy());
        copy.setModelEngineId(modelEngineId);
        copy.getFlags().setInvulnerable(flags.isInvulnerable());
        copy.getFlags().setPushable(flags.isPushable());
        copy.getFlags().setCollidable(flags.isCollidable());
        copy.getFlags().setVisible(flags.isVisible());
        copy.getFlags().setTablist(flags.isTablist());
        copy.getFlags().setGlowing(flags.isGlowing());
        copy.getHologramConfig().setLines(hologramConfig.getLines());
        copy.getHologramConfig().setOffset(hologramConfig.getOffset());
        copy.getHologramConfig().setSpacing(hologramConfig.getSpacing());
        copy.getHologramConfig().setViewRange(hologramConfig.getViewRange());
        copy.getEquipment().clear();
        equipment.getItems().forEach(copy.getEquipment()::setItem);
        copy.getCombatConfig().setMaxHealth(combatConfig.getMaxHealth());
        copy.getCombatConfig().setDamage(combatConfig.getDamage());
        copy.getCombatConfig().setArmor(combatConfig.getArmor());
        copy.getCombatConfig().setAggroRange(combatConfig.getAggroRange());
        traitData.forEach((key, value) -> copy.setTraitData(key, value));
        return copy;
    }
}
