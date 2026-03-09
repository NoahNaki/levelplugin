package me.nakilex.levelplugin.progression.objectives;

@FunctionalInterface
public interface ObjectiveProgressListener {
    void onProgress(ObjectiveProgressEvent event);
}
