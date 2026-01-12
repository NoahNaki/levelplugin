package me.nakilex.npc.plugin.trait;

import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.trait.Trait;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandListTrait implements Trait {
    public static final String ID = "commands";
    private static final String KEY = "commands";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void onInteract(Npc npc, Player player) {
        List<String> commands = getCommands(npc);
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String replaced = command.replace("{player}", player.getName())
                    .replace("{npc}", npc.getName())
                    .replace("{npc_id}", String.valueOf(npc.getId()));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaced);
        }
    }

    @Override
    public Map<String, Object> onSave(Npc npc) {
        Map<String, Object> data = new HashMap<>();
        data.put(KEY, getCommands(npc));
        return data;
    }

    @Override
    public void onLoad(Npc npc, Map<String, Object> data) {
        if (data == null) {
            return;
        }
        Object commands = data.get(KEY);
        if (commands instanceof List<?> list) {
            List<String> values = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    values.add(entry.toString());
                }
            }
            npc.setTraitData(ID, Map.of(KEY, values));
        }
    }

    public List<String> getCommands(Npc npc) {
        Map<String, Object> data = npc.getTraitData(ID);
        if (data == null) {
            return List.of();
        }
        Object value = data.get(KEY);
        if (value instanceof List<?> list) {
            List<String> commands = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    commands.add(entry.toString());
                }
            }
            return commands;
        }
        return List.of();
    }

    public void setCommands(Npc npc, List<String> commands) {
        npc.setTraitData(ID, Map.of(KEY, new ArrayList<>(commands)));
    }
}
