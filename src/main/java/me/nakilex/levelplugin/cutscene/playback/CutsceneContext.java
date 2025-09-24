package me.nakilex.levelplugin.cutscene.playback;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.Cutscene;
import me.nakilex.levelplugin.cutscene.actor.CutsceneActor;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Per-viewer execution context supplied to frames when they play. */
public final class CutsceneContext {
    private final CutscenePlayback playback;
    private final Player viewer;
    private final boolean primary;

    public CutsceneContext(CutscenePlayback playback, Player viewer, boolean primary) {
        this.playback = playback;
        this.viewer = viewer;
        this.primary = primary;
    }

    public Player getViewer() {
        return viewer;
    }

    public boolean isPrimary() {
        return primary;
    }

    public CutscenePlayback getPlayback() {
        return playback;
    }

    public Cutscene getCutscene() {
        return playback.getCutscene();
    }

    public Main getPlugin() {
        return playback.getPlugin();
    }

    public Optional<CutsceneActor> getActor(String name) {
        return playback.getActor(name);
    }

    public void await(CompletableFuture<?> future) {
        if (future != null) {
            playback.await(future);
        }
    }

    public Collection<Player> getViewers() {
        return playback.getViewers();
    }

    public void playEffectsToAll(EffectSettings effects, Location origin) {
        playback.playEffectsToAll(effects, origin);
    }

    public EffectSettings resolveEffectBundle(String name) {
        return playback.resolveEffectBundle(name);
    }

    public void awaitActor(String name) {
        CompletableFuture<Void> future = playback.getActorFuture(name);
        if (future != null) {
            playback.await(future);
        }
    }
}
