package me.nakilex.levelplugin.player.fishing.minigame;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import me.nakilex.levelplugin.utils.RandomUtil;

/** Registers weighted fishing challenges and chooses one for each bite. */
public final class FishingMinigameRegistry {
    private final Map<FishingMinigameFactory, Double> factories = new LinkedHashMap<>();

    public FishingMinigameRegistry register(FishingMinigameFactory factory, double weight) {
        if (factory != null && weight > 0.0) factories.put(factory, weight);
        return this;
    }

    public boolean isEmpty() { return factories.isEmpty(); }

    public FishingMinigame create(Random random, FishingMinigameContext context) {
        if (factories.isEmpty()) return new ReelWindowFishingMinigame(context);
        return RandomUtil.pickWeighted(random, factories).create(context);
    }
}
