package me.nakilex.levelplugin.ego;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic representation of an Ego Weapon. Each weapon has a rarity
 * and rank that can increase with experience. Ranks unlock skills
 * (represented by a string identifier).
 */
public class EgoWeapon {
    private final String id;
    private String name;
    private EgoRarity rarity;
    private int rank;
    private int exp;
    private final Map<Integer, String> rankSkills = new HashMap<>();

    public EgoWeapon(String id, String name, EgoRarity rarity) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.rank = 1;
        this.exp = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public EgoRarity getRarity() { return rarity; }
    public int getRank() { return rank; }
    public int getExp() { return exp; }

    public void setName(String name) { this.name = name; }
    public void setRarity(EgoRarity rarity) { this.rarity = rarity; }

    public void addRankSkill(int rank, String skillKey) {
        rankSkills.put(rank, skillKey);
    }

    public String getSkillForRank(int r) {
        return rankSkills.get(r);
    }

    public int expToNextRank() {
        return 100 * rank;
    }

    /** Adds XP and returns true if the weapon ranked up. */
    public boolean addExp(int amount) {
        boolean leveled = false;
        exp += amount;
        while (exp >= expToNextRank() && rank < 10) {
            exp -= expToNextRank();
            rank++;
            leveled = true;
        }
        return leveled;
    }

    /** Evolve the weapon: increase rarity and reset rank/exp. */
    public void evolve() {
        this.rarity = this.rarity.next();
        this.rank = 1;
        this.exp = 0;
    }

    /**
     * Create a deep copy of this weapon for assigning to players.
     */
    public EgoWeapon copy() {
        EgoWeapon w = new EgoWeapon(this.id, this.name, this.rarity);
        w.rank = this.rank;
        w.exp = this.exp;
        for (Map.Entry<Integer, String> e : this.rankSkills.entrySet()) {
            w.rankSkills.put(e.getKey(), e.getValue());
        }
        return w;
    }
}
