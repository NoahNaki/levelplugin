package me.nakilex.levelplugin.dungeon.reward;

/**
 * Calculates dungeon completion rewards using a weighted formula that takes
 * into account threat, party size, damage taken, clear time, total rooms and
 * the combined combat power of the configured mobs.
 */
public final class DungeonRewardCalculator {

    private DungeonRewardCalculator() {}

    /** Immutable set of metrics describing a dungeon run. */
    public record Context(int threatLevel,
                          int totalRooms,
                          int participants,
                          int completedPlayers,
                          int mobCombatPower,
                          long durationMillis,
                          double totalDamage,
                          double playerDamage) {}

    /** Resulting payout for adventurers and the dungeon master. */
    public record RewardResult(int playerXp,
                               int playerCoins,
                               int playerGems,
                               int masterCoins,
                               int masterGems,
                               double totalMultiplier,
                               double damageFactor,
                               double timeFactor) {}

    public static RewardResult calculate(Context ctx) {
        int threat = Math.max(1, ctx.threatLevel());
        int participants = Math.max(1, ctx.participants());
        int completed = Math.max(1, ctx.completedPlayers());
        int rooms = Math.max(1, ctx.totalRooms());
        double baseThreatWeight = 0.65 + threat * 0.35; // Give threat the highest influence
        double roomFactor = 1.0 + Math.min(0.6, rooms / 18.0);
        double participantFactor = 1.0 + Math.min(0.7, (participants - 1) * 0.12);
        double completionFactor = 1.0 + Math.min(0.6, completed / (double) participants);
        double combatFactor = 1.0 + Math.min(0.75, ctx.mobCombatPower() / 20000.0);

        double totalDamage = Math.max(1.0, ctx.totalDamage());
        double playerDamageShare = ctx.playerDamage() <= 0 ? 0 : ctx.playerDamage() / totalDamage;
        double playerDamagePenalty = Math.min(0.45,
                playerDamageShare * 0.35 + ctx.playerDamage() / 3500.0);
        double avgDamage = ctx.totalDamage() / participants;
        double groupDamagePenalty = Math.min(0.3, avgDamage / 4000.0);
        double damageFactor = clamp(1.0 - playerDamagePenalty - groupDamagePenalty, 0.35, 1.05);

        double minutes = ctx.durationMillis() / 60000.0;
        double timeFactor = minutes <= 10
                ? 1.0 + Math.min(0.3, (10 - minutes) / 25.0)
                : 1.0 - Math.min(0.4, (minutes - 10) / 35.0);

        double multiplier = baseThreatWeight
                * roomFactor
                * participantFactor
                * completionFactor
                * combatFactor
                * damageFactor
                * timeFactor;
        multiplier = clamp(multiplier, 0.4, 6.0);

        int baseXp = 450 + threat * 475;
        int baseCoins = 300 + threat * 240;
        int baseGems = threat <= 1 ? 0 : threat * 7;

        int playerXp = (int) Math.round(baseXp * multiplier);
        int playerCoins = (int) Math.round(baseCoins * multiplier);
        int playerGems = (int) Math.round(baseGems * multiplier * 0.45);
        if (playerGems < 0) playerGems = 0;

        int masterCoins = (int) Math.round((220 + threat * 200)
                * participantFactor
                * completionFactor);
        int masterGems = threat <= 2 ? 0 : (int) Math.round(threat * 4 * completionFactor);

        return new RewardResult(playerXp, playerCoins, playerGems,
                Math.max(0, masterCoins), Math.max(0, masterGems),
                multiplier, damageFactor, timeFactor);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
