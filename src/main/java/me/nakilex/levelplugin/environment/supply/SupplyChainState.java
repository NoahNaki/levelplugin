package me.nakilex.levelplugin.environment.supply;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Persistent per-guild state for the supply chain manager. */
public final class SupplyChainState {
    private String activeChainId;
    private int stageIndex;
    private final Map<String, Integer> contributions = new HashMap<>();
    private final Map<String, Integer> contributorAmounts = new HashMap<>();
    private long productionCompleteAt;
    private long rotationEpoch;
    private final Set<String> completedChains = new HashSet<>();

    public static SupplyChainState load(ConfigurationSection section) {
        SupplyChainState state = new SupplyChainState();
        if (section == null) {
            return state;
        }
        state.activeChainId = section.getString("active_chain", null);
        state.stageIndex = section.getInt("stage_index", 0);
        state.productionCompleteAt = section.getLong("production_complete", 0L);
        state.rotationEpoch = section.getLong("rotation_epoch", Long.MIN_VALUE);
        ConfigurationSection contrib = section.getConfigurationSection("contributions");
        if (contrib != null) {
            for (String key : contrib.getKeys(false)) {
                state.contributions.put(key, contrib.getInt(key, 0));
            }
        }
        ConfigurationSection players = section.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                state.contributorAmounts.put(key, players.getInt(key, 0));
            }
        }
        state.completedChains.addAll(section.getStringList("completed"));
        return state;
    }

    public void save(ConfigurationSection section) {
        section.set("active_chain", activeChainId);
        section.set("stage_index", stageIndex);
        section.set("production_complete", productionCompleteAt);
        section.set("rotation_epoch", rotationEpoch);
        ConfigurationSection contrib = section.createSection("contributions");
        for (Map.Entry<String, Integer> e : contributions.entrySet()) {
            contrib.set(e.getKey(), e.getValue());
        }
        ConfigurationSection players = section.createSection("players");
        for (Map.Entry<String, Integer> e : contributorAmounts.entrySet()) {
            players.set(e.getKey(), e.getValue());
        }
        section.set("completed", completedChains.isEmpty() ? null : completedChains.toArray(new String[0]));
    }

    public String getActiveChainId() {
        return activeChainId;
    }

    public void setActiveChainId(String activeChainId) {
        this.activeChainId = activeChainId;
    }

    public int getStageIndex() {
        return stageIndex;
    }

    public void setStageIndex(int stageIndex) {
        this.stageIndex = stageIndex;
    }

    public Map<String, Integer> getContributions() {
        return contributions;
    }

    public Map<String, Integer> getContributorAmounts() {
        return contributorAmounts;
    }

    public long getProductionCompleteAt() {
        return productionCompleteAt;
    }

    public void setProductionCompleteAt(long productionCompleteAt) {
        this.productionCompleteAt = productionCompleteAt;
    }

    public long getRotationEpoch() {
        return rotationEpoch;
    }

    public void setRotationEpoch(long rotationEpoch) {
        this.rotationEpoch = rotationEpoch;
    }

    public Set<String> getCompletedChains() {
        return completedChains;
    }
}

