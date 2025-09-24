package me.nakilex.levelplugin.cutscene.actor;

import me.nakilex.levelplugin.cutscene.playback.CutsceneContext;
import org.bukkit.Location;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Runtime representation of an actor participating in a cutscene. Actors can
 * be NPCs, players, holograms, etc. Actions return a {@link CompletableFuture}
 * so frames may synchronise on their completion when necessary.
 */
public interface CutsceneActor {
    String getName();

    void spawn(CutsceneContext context);

    void despawn();

    CompletableFuture<Void> performAction(String action, CutsceneContext context, Map<String, Object> parameters);

    Location getCurrentLocation();

    /**
     * @return true if the actor should persist after the cutscene finishes.
     */
    default boolean isPersistent() {
        return false;
    }
}
