package me.nakilex.levelplugin.pathfinding.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-player runtime state tracked by {@link MercenaryDeploymentManager}.
 */
public final class MercenaryDeploymentState {
    private final Map<String, ActiveDeployment> active = new LinkedHashMap<>();
    private final List<CompletedDeployment> completed = new ArrayList<>();

    public Collection<ActiveDeployment> activeDeployments() {
        return Collections.unmodifiableCollection(active.values());
    }

    public List<CompletedDeployment> completedDeployments() {
        return Collections.unmodifiableList(completed);
    }

    boolean isEmpty() {
        return active.isEmpty() && completed.isEmpty();
    }

    void addActive(ActiveDeployment deployment) {
        active.put(deployment.deploymentId(), deployment);
    }

    ActiveDeployment getActive(String deploymentId) {
        return active.get(deploymentId);
    }

    ActiveDeployment removeActive(String deploymentId) {
        return active.remove(deploymentId);
    }

    void addCompleted(CompletedDeployment deployment) {
        completed.add(deployment);
    }

    void clearCompleted(java.util.function.Predicate<CompletedDeployment> predicate) {
        completed.removeIf(predicate);
    }

    /** Active deployment data persisted to disk while the mission runs. */
    public static final class ActiveDeployment {
        private final String deploymentId;
        private final MercenarySpecialization specialization;
        private final long startedAt;
        private final long durationMillis;
        private final double successChance;
        private final double rewardMultiplier;

        ActiveDeployment(String deploymentId,
                         MercenarySpecialization specialization,
                         long startedAt,
                         long durationMillis,
                         double successChance,
                         double rewardMultiplier) {
            this.deploymentId = deploymentId;
            this.specialization = specialization;
            this.startedAt = startedAt;
            this.durationMillis = durationMillis;
            this.successChance = successChance;
            this.rewardMultiplier = rewardMultiplier;
        }

        public String deploymentId() {
            return deploymentId;
        }

        public MercenarySpecialization specialization() {
            return specialization;
        }

        public long startedAt() {
            return startedAt;
        }

        public long durationMillis() {
            return durationMillis;
        }

        public double successChance() {
            return successChance;
        }

        public double rewardMultiplier() {
            return rewardMultiplier;
        }

        public long endsAt() {
            return startedAt + durationMillis;
        }

        public long remaining(long now) {
            return Math.max(0L, endsAt() - now);
        }
    }

    /** Result of a deployment awaiting claim by the player. */
    public static final class CompletedDeployment {
        private final String deploymentId;
        private final MercenarySpecialization specialization;
        private final boolean success;
        private final double rewardMultiplier;
        private final double successChance;
        private final long completedAt;

        CompletedDeployment(String deploymentId,
                             MercenarySpecialization specialization,
                             boolean success,
                             double rewardMultiplier,
                             double successChance,
                             long completedAt) {
            this.deploymentId = deploymentId;
            this.specialization = specialization;
            this.success = success;
            this.rewardMultiplier = rewardMultiplier;
            this.successChance = successChance;
            this.completedAt = completedAt;
        }

        public String deploymentId() {
            return deploymentId;
        }

        public MercenarySpecialization specialization() {
            return specialization;
        }

        public boolean success() {
            return success;
        }

        public double rewardMultiplier() {
            return rewardMultiplier;
        }

        public double successChance() {
            return successChance;
        }

        public long completedAt() {
            return completedAt;
        }
    }
}
