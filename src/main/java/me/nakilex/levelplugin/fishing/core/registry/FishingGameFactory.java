package me.nakilex.levelplugin.fishing.core.registry;

import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.game.FishingGame;
import me.nakilex.levelplugin.fishing.core.game.GameDefinition;

import java.util.function.Consumer;

public interface FishingGameFactory {
    FishingGame create(GameDefinition definition, FishingContext context, Consumer<Boolean> onComplete);
}
