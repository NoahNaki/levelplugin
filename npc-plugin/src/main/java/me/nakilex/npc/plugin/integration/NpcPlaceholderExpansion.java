package me.nakilex.npc.plugin.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.plugin.service.NpcService;
import org.bukkit.entity.Player;

public class NpcPlaceholderExpansion extends PlaceholderExpansion {
    private final NpcService service;

    public NpcPlaceholderExpansion(NpcService service) {
        this.service = service;
    }

    @Override
    public String getIdentifier() {
        return "npc";
    }

    @Override
    public String getAuthor() {
        return "Nakilex";
    }

    @Override
    public String getVersion() {
        return service.getPlugin().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        return switch (params.toLowerCase()) {
            case "selected_id" -> service.getRegistry().getSelectedNpcId(player.getUniqueId())
                    .map(String::valueOf)
                    .orElse("");
            case "selected_name" -> service.getRegistry().getSelectedNpc(player.getUniqueId())
                    .map(Npc::getName)
                    .orElse("");
            default -> "";
        };
    }
}
