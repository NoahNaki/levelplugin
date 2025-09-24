package me.nakilex.levelplugin.battlepass;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Mutable state for a player's battle pass progression.
 */
public class BattlePassProgress {
    private final Set<Integer> claimedFree = new HashSet<>();
    private final Set<Integer> claimedPremium = new HashSet<>();
    private int seasonId;
    private int xp;
    private boolean premium;

    public BattlePassProgress(int seasonId) {
        this.seasonId = seasonId;
    }

    public int getSeasonId() {
        return seasonId;
    }

    public void setSeasonId(int seasonId) {
        this.seasonId = seasonId;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public void addXp(int amount, int maxXp) {
        if (amount <= 0) return;
        int capped = maxXp > 0 ? Math.min(maxXp, xp + amount) : xp + amount;
        this.xp = Math.max(0, capped);
    }

    public boolean hasPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public Set<Integer> getClaimedFree() {
        return Collections.unmodifiableSet(claimedFree);
    }

    public Set<Integer> getClaimedPremium() {
        return Collections.unmodifiableSet(claimedPremium);
    }

    public boolean isClaimed(int tier, boolean premiumTrack) {
        return (premiumTrack ? claimedPremium : claimedFree).contains(tier);
    }

    public void markClaimed(int tier, boolean premiumTrack) {
        (premiumTrack ? claimedPremium : claimedFree).add(tier);
    }

    public void clearClaims() {
        claimedFree.clear();
        claimedPremium.clear();
    }

    public int unlockedTiers(int xpPerTier, int totalTiers) {
        if (xpPerTier <= 0) return 0;
        int unlocked = xp / xpPerTier;
        return Math.max(0, Math.min(totalTiers, unlocked));
    }
}
