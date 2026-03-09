package me.nakilex.levelplugin.codex.mastery;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.progression.objectives.ObjectiveProgressBus;
import me.nakilex.levelplugin.progression.objectives.ObjectiveProgressEvent;
import me.nakilex.levelplugin.progression.objectives.ObjectiveType;
import me.nakilex.levelplugin.progression.threshold.ThresholdRewardEngine;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Passive codex mastery rewards based on DISCOVER_CODEX objective milestones.
 */
public class CodexMasteryManager {
    private final EconomyManager economyManager;
    private final ThresholdRewardEngine<Integer> coinRewards = new ThresholdRewardEngine<>();
    private final Map<UUID, Integer> discoveredCount = new HashMap<>();
    private final Map<UUID, Integer> grantedLevel = new HashMap<>();

    public CodexMasteryManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
        coinRewards.put(10, 500);
        coinRewards.put(25, 1500);
        coinRewards.put(50, 5000);
        ObjectiveProgressBus.getInstance().subscribe(this::onObjectiveProgress);
    }

    private void onObjectiveProgress(ObjectiveProgressEvent event) {
        if (event == null || event.type() != ObjectiveType.DISCOVER_CODEX || event.playerId() == null) {
            return;
        }
        int after = discoveredCount.merge(event.playerId(), Math.max(1, event.amount()), Integer::sum);
        int level = coinRewards.resolveLevel(after);
        int beforeLevel = grantedLevel.getOrDefault(event.playerId(), 0);
        if (level <= beforeLevel) {
            return;
        }
        grantedLevel.put(event.playerId(), level);
        Integer reward = coinRewards.resolve(after);
        if (reward == null || reward <= 0) {
            return;
        }
        Player player = Bukkit.getPlayer(event.playerId());
        if (player == null) {
            return;
        }
        economyManager.addCoins(player, reward);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD,
                "Codex Mastery Lv." + level + " reached (+<glyph:coins_icon> " + reward + ")");
    }
}
