package me.nakilex.levelplugin.progression.objectives;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight in-process objective event bus to decouple producers and consumers.
 */
public final class ObjectiveProgressBus {

    private static final ObjectiveProgressBus INSTANCE = new ObjectiveProgressBus();

    private final List<ObjectiveProgressListener> listeners = new CopyOnWriteArrayList<>();

    public static ObjectiveProgressBus getInstance() {
        return INSTANCE;
    }

    private ObjectiveProgressBus() {
    }

    public void subscribe(ObjectiveProgressListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(ObjectiveProgressListener listener) {
        listeners.remove(listener);
    }

    public void publish(ObjectiveProgressEvent event) {
        if (event == null) {
            return;
        }
        for (ObjectiveProgressListener listener : listeners) {
            listener.onProgress(event);
        }
    }
}
