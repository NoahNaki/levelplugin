package me.nakilex.levelplugin.items.v2;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemInstance {
    private final int definitionId;
    private final UUID instanceUuid;
    private final Integer charges;
    private final Map<ItemStatType, Double> rolledStats;

    public ItemInstance(int definitionId,
                        UUID instanceUuid,
                        Integer charges,
                        Map<ItemStatType, Double> rolledStats) {
        this.definitionId = definitionId;
        this.instanceUuid = instanceUuid;
        this.charges = charges;
        this.rolledStats = rolledStats == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(rolledStats));
    }

    public int definitionId() {
        return definitionId;
    }

    public UUID instanceUuid() {
        return instanceUuid;
    }

    public Integer charges() {
        return charges;
    }

    public Map<ItemStatType, Double> rolledStats() {
        return rolledStats;
    }
}
