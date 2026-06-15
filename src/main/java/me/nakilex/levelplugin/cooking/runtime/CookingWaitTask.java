package me.nakilex.levelplugin.cooking.runtime;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/** Isolated scheduled countdown for WAIT cooking stages. */
public class CookingWaitTask extends BukkitRunnable {
    private final long periodTicks;
    private final BooleanSupplier stillValid;
    private final LongConsumer tickHandler;
    private final Runnable completionHandler;
    private final Runnable invalidHandler;
    private long remainingTicks;

    public CookingWaitTask(long durationTicks,
                           long periodTicks,
                           BooleanSupplier stillValid,
                           LongConsumer tickHandler,
                           Runnable completionHandler,
                           Runnable invalidHandler) {
        this.remainingTicks = Math.max(0L, durationTicks);
        this.periodTicks = Math.max(1L, periodTicks);
        this.stillValid = stillValid;
        this.tickHandler = tickHandler;
        this.completionHandler = completionHandler;
        this.invalidHandler = invalidHandler;
    }

    @Override
    public void run() {
        if (!stillValid.getAsBoolean()) {
            cancel();
            invalidHandler.run();
            return;
        }
        if (remainingTicks <= 0L) {
            cancel();
            completionHandler.run();
            return;
        }
        tickHandler.accept(remainingTicks);
        remainingTicks = Math.max(0L, remainingTicks - periodTicks);
    }
}
