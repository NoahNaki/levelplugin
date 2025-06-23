package me.nakilex.levelplugin.spells;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.Player;

/**
 * Provides simple access for casting Phoenix Hunter MythicMobs skills.
 */
public class PhoenixHunterSpell {

    public void castSkill(Player player, String skillName) {
        MythicBukkit.inst().getAPIHelper().castSkill(player, skillName);
    }
}
