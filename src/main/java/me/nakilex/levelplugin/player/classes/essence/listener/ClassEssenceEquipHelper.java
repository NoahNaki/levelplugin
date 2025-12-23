package me.nakilex.levelplugin.player.classes.essence.listener;

import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager.StatType;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.player.classes.essence.gui.ClassEssenceGUI;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

final class ClassEssenceEquipHelper {

    private ClassEssenceEquipHelper() {}

    static void equip(Player player, StatsManager.PlayerStats ps, int idx, ItemStack essence, Inventory view) {
        if (player == null || ps == null || essence == null) return;

        PlayerClass essenceClass = ClassEssence.getClass(essence);
        Map<StatType, Integer> before = new HashMap<>();
        for (StatType st : ClassEssence.getStatTypes(essence)) {
            before.put(st, StatsManager.getInstance().getStatValue(player, st));
        }

        for (int i = 0; i < ps.equippedEssences.length; i++) {
            if (ps.equippedEssences[i] && i != idx) {
                ItemStack other = ps.essenceSlots[i];
                if (other != null && ClassEssence.isEssence(other)) {
                    for (StatType st : ClassEssence.getStatTypes(other)) {
                        before.putIfAbsent(st, StatsManager.getInstance().getStatValue(player, st));
                    }
                    ClassEssence.removeAttributes(player, other);
                    ClassEssence.setEquipped(other, false);
                    ClassEssence.addSlotTips(other);
                    if (view != null) {
                        view.setItem(ClassEssenceGUI.slotFromIndex(i), other);
                    }
                }
                ps.equippedEssences[i] = false;
            }
        }

        ClassEssence.setEquipped(essence, true);
        ClassEssence.setSoulbound(essence, true);
        ClassEssence.addSlotTips(essence);
        ClassEssence.applyAttributes(player, essence);
        ps.equippedEssences[idx] = true;

        ps.playerClass = essenceClass;
        ps.unlockedClasses.add(essenceClass);
        PlayerClassManager.getInstance().setPlayerClass(player, essenceClass);
        ItemUtil.refreshTooltips(player);

        ChatFormatter.constructDivider(player, "§6§l-", 45);
        ChatFormatter.sendCenteredMessage(player, "§6§lESSENCE EQUIPPED!");
        ChatFormatter.constructDivider(player, " ", 45);
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "You are now the §e§l" + essenceClass.getDisplayName() + " §7class!");
        ChatFormatter.constructDivider(player, " ", 45);
        for (StatType st : before.keySet()) {
            int after = StatsManager.getInstance().getStatValue(player, st);
            if (after != before.get(st)) {
                ChatColor col = after >= before.get(st) ? ChatColor.GREEN : ChatColor.RED;
                String name = GuiUtil.formatStatName(st);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        name + ": " + ChatColor.WHITE + before.get(st) + ChatColor.GRAY + " -> " + col + after);
            }
        }
        ChatFormatter.constructDivider(player, " ", 45);
        ChatFormatter.constructDivider(player, "§6§l-", 45);
    }

    static void unequip(Player player, StatsManager.PlayerStats ps, int idx, ItemStack essence) {
        if (player == null || ps == null || essence == null) return;

        Map<StatType, Integer> before = new HashMap<>();
        for (StatType st : ClassEssence.getStatTypes(essence)) {
            before.put(st, StatsManager.getInstance().getStatValue(player, st));
        }

        ClassEssence.removeAttributes(player, essence);
        ps.equippedEssences[idx] = false;
        ClassEssence.setEquipped(essence, false);

        ps.playerClass = PlayerClass.VILLAGER;
        PlayerClassManager.getInstance().setPlayerClass(player, PlayerClass.VILLAGER);
        ItemUtil.refreshTooltips(player);

        ChatFormatter.constructDivider(player, "§6§l-", 45);
        ChatFormatter.sendCenteredMessage(player, "§6§lESSENCE UNEQUIPPED!");
        ChatFormatter.constructDivider(player, " ", 45);
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "You are now the §e§lVillager §7class!");
        ChatFormatter.constructDivider(player, " ", 45);
        for (StatType st : before.keySet()) {
            int after = StatsManager.getInstance().getStatValue(player, st);
            ChatColor col = after >= before.get(st) ? ChatColor.GREEN : ChatColor.RED;
            String name = GuiUtil.formatStatName(st);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    name + ": " + ChatColor.WHITE + before.get(st) + ChatColor.GRAY + " -> " + col + after);
        }
        ChatFormatter.constructDivider(player, " ", 45);
        ChatFormatter.constructDivider(player, "§6§l-", 45);
    }
}

