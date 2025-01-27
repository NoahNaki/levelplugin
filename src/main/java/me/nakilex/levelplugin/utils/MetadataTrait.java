package me.nakilex.levelplugin.utils;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

import java.util.UUID;

public class MetadataTrait extends Trait {
    private UUID owner;

    public MetadataTrait() {
        super("MetadataTrait");
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public UUID getOwner() {
        return owner;
    }

    @Override
    public void load(DataKey key) {
        String ownerId = key.getString("owner");
        if (ownerId != null && !ownerId.isEmpty()) {
            owner = UUID.fromString(ownerId);
        }
    }

    @Override
    public void save(DataKey key) {
        if (owner != null) {
            key.setString("owner", owner.toString());
        }
    }
}
