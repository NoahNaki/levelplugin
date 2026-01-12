package me.nakilex.npc.core.persistence;

import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.model.NpcEquipment;
import me.nakilex.npc.core.model.NpcFlags;
import me.nakilex.npc.core.model.NpcPosition;
import me.nakilex.npc.core.model.SkinRef;
import me.nakilex.npc.core.model.SkinSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

class NpcYamlAdapter {
    Map<String, Object> serialize(Npc npc) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", npc.getId());
        data.put("name", npc.getName());
        data.put("type", npc.getType().name());
        putPosition(data, "position", npc.getPosition());
        putPosition(data, "home", npc.getHomePosition());
        data.put("viewRange", npc.getViewRange());
        data.put("modelEngineId", npc.getModelEngineId());
        data.put("flags", serializeFlags(npc.getFlags()));
        data.put("skin", serializeSkin(npc.getSkinRef()));
        data.put("hologram", serializeHologram(npc));
        data.put("combat", serializeCombat(npc));
        data.put("equipment", serializeEquipment(npc.getEquipment()));
        data.put("traits", npc.getTraitData());
        return data;
    }

    Npc deserialize(ConfigurationSection section) {
        int id = section.getInt("id");
        String name = section.getString("name", "NPC");
        String type = section.getString("type", "VILLAGER");
        NpcPosition position = readPosition(section.getConfigurationSection("position"));
        if (position == null) {
            return null;
        }
        Npc npc = new Npc(id, name, org.bukkit.entity.EntityType.valueOf(type), position);
        npc.setHomePosition(readPosition(section.getConfigurationSection("home")));
        npc.setViewRange(section.getDouble("viewRange", 32.0));
        npc.setModelEngineId(section.getString("modelEngineId"));
        deserializeFlags(section.getConfigurationSection("flags"), npc.getFlags());
        npc.setSkinRef(deserializeSkin(section.getConfigurationSection("skin")));
        deserializeHologram(section.getConfigurationSection("hologram"), npc);
        deserializeCombat(section.getConfigurationSection("combat"), npc);
        deserializeEquipment(section.getConfigurationSection("equipment"), npc.getEquipment());
        ConfigurationSection traitsSection = section.getConfigurationSection("traits");
        if (traitsSection != null) {
            for (String key : traitsSection.getKeys(false)) {
                Object value = traitsSection.get(key);
                if (value instanceof Map<?, ?> map) {
                    npc.setTraitData(key, new HashMap<>((Map<String, Object>) map));
                }
            }
        }
        return npc;
    }

    private void putPosition(Map<String, Object> data, String key, NpcPosition position) {
        if (position == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        values.put("world", position.getWorldName());
        values.put("x", position.getX());
        values.put("y", position.getY());
        values.put("z", position.getZ());
        values.put("yaw", position.getYaw());
        values.put("pitch", position.getPitch());
        data.put(key, values);
    }

    private NpcPosition readPosition(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String world = section.getString("world");
        if (world == null) {
            return null;
        }
        return new NpcPosition(world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }

    private Map<String, Object> serializeFlags(NpcFlags flags) {
        Map<String, Object> data = new HashMap<>();
        data.put("invulnerable", flags.isInvulnerable());
        data.put("pushable", flags.isPushable());
        data.put("collidable", flags.isCollidable());
        data.put("visible", flags.isVisible());
        data.put("tablist", flags.isTablist());
        data.put("glowing", flags.isGlowing());
        return data;
    }

    private void deserializeFlags(ConfigurationSection section, NpcFlags flags) {
        if (section == null) {
            return;
        }
        flags.setInvulnerable(section.getBoolean("invulnerable", flags.isInvulnerable()));
        flags.setPushable(section.getBoolean("pushable", flags.isPushable()));
        flags.setCollidable(section.getBoolean("collidable", flags.isCollidable()));
        flags.setVisible(section.getBoolean("visible", flags.isVisible()));
        flags.setTablist(section.getBoolean("tablist", flags.isTablist()));
        flags.setGlowing(section.getBoolean("glowing", flags.isGlowing()));
    }

    private Map<String, Object> serializeSkin(SkinRef skinRef) {
        if (skinRef == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("source", skinRef.getSource().name());
        data.put("value", skinRef.getValue());
        data.put("signature", skinRef.getSignature());
        return data;
    }

    private SkinRef deserializeSkin(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String source = section.getString("source");
        String value = section.getString("value");
        String signature = section.getString("signature");
        if (source == null || value == null) {
            return null;
        }
        return new SkinRef(SkinSource.valueOf(source), value, signature);
    }

    private Map<String, Object> serializeHologram(Npc npc) {
        Map<String, Object> data = new HashMap<>();
        data.put("lines", npc.getHologramConfig().getLines());
        data.put("offset", npc.getHologramConfig().getOffset());
        data.put("spacing", npc.getHologramConfig().getSpacing());
        data.put("viewRange", npc.getHologramConfig().getViewRange());
        return data;
    }

    private void deserializeHologram(ConfigurationSection section, Npc npc) {
        if (section == null) {
            return;
        }
        npc.getHologramConfig().setLines(section.getStringList("lines"));
        npc.getHologramConfig().setOffset(section.getDouble("offset", npc.getHologramConfig().getOffset()));
        npc.getHologramConfig().setSpacing(section.getDouble("spacing", npc.getHologramConfig().getSpacing()));
        npc.getHologramConfig().setViewRange(section.getDouble("viewRange", npc.getHologramConfig().getViewRange()));
    }

    private Map<String, Object> serializeCombat(Npc npc) {
        Map<String, Object> data = new HashMap<>();
        data.put("maxHealth", npc.getCombatConfig().getMaxHealth());
        data.put("damage", npc.getCombatConfig().getDamage());
        data.put("armor", npc.getCombatConfig().getArmor());
        data.put("aggroRange", npc.getCombatConfig().getAggroRange());
        return data;
    }

    private void deserializeCombat(ConfigurationSection section, Npc npc) {
        if (section == null) {
            return;
        }
        npc.getCombatConfig().setMaxHealth(section.getDouble("maxHealth", npc.getCombatConfig().getMaxHealth()));
        npc.getCombatConfig().setDamage(section.getDouble("damage", npc.getCombatConfig().getDamage()));
        npc.getCombatConfig().setArmor(section.getDouble("armor", npc.getCombatConfig().getArmor()));
        npc.getCombatConfig().setAggroRange(section.getDouble("aggroRange", npc.getCombatConfig().getAggroRange()));
    }

    private Map<String, Object> serializeEquipment(NpcEquipment equipment) {
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.getItems().entrySet()) {
            data.put(entry.getKey().name(), entry.getValue());
        }
        return data;
    }

    private void deserializeEquipment(ConfigurationSection section, NpcEquipment equipment) {
        if (section == null) {
            return;
        }
        Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);
        for (String key : section.getKeys(false)) {
            try {
                EquipmentSlot slot = EquipmentSlot.valueOf(key);
                ItemStack stack = section.getItemStack(key);
                if (stack != null) {
                    items.put(slot, stack);
                }
            } catch (IllegalArgumentException ignored) {
                // skip unknown
            }
        }
        items.forEach(equipment::setItem);
    }
}
