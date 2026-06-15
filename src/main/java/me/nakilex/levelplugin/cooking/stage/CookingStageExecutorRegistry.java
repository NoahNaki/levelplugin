package me.nakilex.levelplugin.cooking.stage;

import me.nakilex.levelplugin.cooking.model.CookingStageType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** Registry mapping configured cooking stage types to their runtime executors. */
public class CookingStageExecutorRegistry {
    private final Map<CookingStageType, CookingStageExecutor> executors = new EnumMap<>(CookingStageType.class);

    public CookingStageExecutorRegistry register(CookingStageExecutor executor) {
        if (executor != null) {
            executors.put(executor.type(), executor);
        }
        return this;
    }

    public Optional<CookingStageExecutor> get(CookingStageType type) {
        return Optional.ofNullable(type == null ? null : executors.get(type));
    }
}
