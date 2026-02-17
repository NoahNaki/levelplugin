package me.nakilex.levelplugin.player.attributes.lifeskill;

import java.util.UUID;

/** Generic progression contract shared by life-skill managers. */
public interface LifeSkillProgression {
    int getLevel(UUID uuid);
    int getXP(UUID uuid);
    void addXP(UUID uuid, int amount);
    void setLevel(UUID uuid, int level);
    int getXpRequired(int level);
    int getMaxLevel();
}
