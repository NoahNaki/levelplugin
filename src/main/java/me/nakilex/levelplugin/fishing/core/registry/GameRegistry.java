package me.nakilex.levelplugin.fishing.core.registry;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.game.FishingGame;
import me.nakilex.levelplugin.fishing.core.game.GameDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GameRegistry {
    private final Map<String, FishingGameFactory> factories = new HashMap<>();

    public void register(String key, FishingGameFactory factory) {
        if (key == null || factory == null) {
            return;
        }
        factories.put(key.toLowerCase(), factory);
    }

    public FishingGame create(GameDefinition definition, FishingContext context, Consumer<Boolean> onComplete) {
        if (definition == null) {
            return null;
        }
        FishingGameFactory factory = factories.get(definition.type().toLowerCase());
        if (factory == null) {
            return null;
        }
        return factory.create(definition, context, onComplete);
    }
}
