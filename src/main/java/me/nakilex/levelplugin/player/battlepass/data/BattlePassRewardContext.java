package me.nakilex.levelplugin.player.battlepass.data;

import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.transmog.TransmogManager;
import me.nakilex.levelplugin.utils.TextUtil;

/**
 * Provides lookup helpers so reward definitions can resolve names for
 * additional systems such as fast travel points or transmogs without
 * coupling themselves to the underlying managers.
 */
public record BattlePassRewardContext(
        ItemManager itemManager,
        FastTravelManager fastTravelManager,
        TransmogManager transmogManager
) {
    public String fastTravelDisplayName(String id) {
        if (id == null || id.isBlank()) {
            return "Fast Travel";
        }
        if (fastTravelManager != null) {
            FastTravelPoint point = fastTravelManager.getPoint(id);
            if (point != null) {
                return point.getName();
            }
        }
        return TextUtil.beautifyWords(id.replace('-', ' ').replace('_', ' '));
    }

    public String transmogDisplayName(BattlePassRewardDefinition.TransmogUnlock unlock) {
        if (unlock == null || unlock.modelId().isBlank()) {
            return "Transmog";
        }
        String modelId = unlock.modelId();
        return TextUtil.beautifyWords(modelId.replace('-', ' ').replace('_', ' '));
    }
}
