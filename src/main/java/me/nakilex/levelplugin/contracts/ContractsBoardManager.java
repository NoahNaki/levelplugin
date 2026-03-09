package me.nakilex.levelplugin.contracts;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.progression.objectives.ObjectiveProgressBus;
import me.nakilex.levelplugin.progression.objectives.ObjectiveProgressEvent;
import me.nakilex.levelplugin.progression.objectives.ObjectiveType;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * First-pass weekly contracts tracker built on top of ObjectiveProgressBus.
 */
public class ContractsBoardManager {

    private final EconomyManager economyManager;
    private final List<ContractDefinition> activeContracts = new ArrayList<>();
    private final Map<UUID, Map<String, Integer>> progressByPlayer = new HashMap<>();

    public ContractsBoardManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
        ObjectiveProgressBus.getInstance().subscribe(this::onObjectiveProgress);
    }

    public void load(FileConfiguration config) {
        activeContracts.clear();
        if (config == null || !config.isConfigurationSection("contracts")) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection("contracts");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection node = section.getConfigurationSection(id);
            if (node == null) continue;
            ObjectiveType type;
            try {
                type = ObjectiveType.valueOf(node.getString("type", "KILL_MOB").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            String target = node.getString("target", "*");
            int required = Math.max(1, node.getInt("required", 1));
            int rewardCoins = Math.max(0, node.getInt("reward_coins", 0));
            activeContracts.add(new ContractDefinition(id, type, target, required, rewardCoins));
        }
    }

    private void onObjectiveProgress(ObjectiveProgressEvent event) {
        if (event == null || event.playerId() == null) return;
        Map<String, Integer> playerProgress = progressByPlayer.computeIfAbsent(event.playerId(), ignored -> new HashMap<>());
        for (ContractDefinition contract : activeContracts) {
            if (contract.objectiveType() != event.type()) continue;
            if (!"*".equals(contract.target()) && !contract.target().equalsIgnoreCase(event.target())) continue;

            int before = playerProgress.getOrDefault(contract.id(), 0);
            int after = Math.min(contract.requiredAmount(), before + Math.max(0, event.amount()));
            playerProgress.put(contract.id(), after);
            if (before < contract.requiredAmount() && after >= contract.requiredAmount() && contract.rewardCoins() > 0) {
                Player player = Bukkit.getPlayer(event.playerId());
                if (player != null) {
                    economyManager.addCoins(player, contract.rewardCoins());
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                            "Contract complete: " + contract.id() + " (+<glyph:coins_icon> " + contract.rewardCoins() + ")");
                }
            }
        }
    }

    public int getProgress(UUID playerId, String contractId) {
        return progressByPlayer.getOrDefault(playerId, Collections.emptyMap()).getOrDefault(contractId, 0);
    }

    public List<ContractDefinition> getActiveContracts() {
        return Collections.unmodifiableList(activeContracts);
    }
}
