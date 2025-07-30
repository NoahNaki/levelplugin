package me.nakilex.levelplugin.codex;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Data representing a mob essence modifier. */
public class MobEssence {
    public final int level;
    public final Double hpMult;
    public final Double damageMult;
    public final Double moveMult;
    public final Double attackMult;
    public final Double countMult;

    public MobEssence(int level,
                      Double hpMult,
                      Double damageMult,
                      Double moveMult,
                      Double attackMult,
                      Double countMult) {
        this.level = level;
        this.hpMult = hpMult;
        this.damageMult = damageMult;
        this.moveMult = moveMult;
        this.attackMult = attackMult;
        this.countMult = countMult;
    }

    public Map<String,Object> toMap() {
        Map<String,Object> map = new HashMap<>();
        map.put("level", level);
        if (hpMult != null) map.put("hp", hpMult);
        if (damageMult != null) map.put("damage", damageMult);
        if (moveMult != null) map.put("move", moveMult);
        if (attackMult != null) map.put("attack", attackMult);
        if (countMult != null) map.put("count", countMult);
        return map;
    }

    public static MobEssence fromMap(Map<?,?> map) {
        int lvl = map.containsKey("level") ? ((Number)map.get("level")).intValue() : 1;
        Double hp = map.containsKey("hp") ? ((Number)map.get("hp")).doubleValue() : null;
        Double dmg = map.containsKey("damage") ? ((Number)map.get("damage")).doubleValue() : null;
        Double move = map.containsKey("move") ? ((Number)map.get("move")).doubleValue() : null;
        Double atk = map.containsKey("attack") ? ((Number)map.get("attack")).doubleValue() : null;
        Double cnt = map.containsKey("count") ? ((Number)map.get("count")).doubleValue() : null;
        return new MobEssence(lvl, hp, dmg, move, atk, cnt);
    }

    /** Generate a random essence with simple multipliers. */
    public static MobEssence randomEssence() {
        int lvl = ThreadLocalRandom.current().nextInt(1, 4);
        Double hp = ThreadLocalRandom.current().nextDouble(0.5, 3.0);
        Double dmg = ThreadLocalRandom.current().nextDouble(0.5, 3.0);
        Double move = ThreadLocalRandom.current().nextDouble(0.5, 3.0);
        Double atk = ThreadLocalRandom.current().nextDouble(0.5, 3.0);
        Double cnt = ThreadLocalRandom.current().nextDouble(0.5, 3.0);
        return new MobEssence(lvl, hp, dmg, move, atk, cnt);
    }
}
