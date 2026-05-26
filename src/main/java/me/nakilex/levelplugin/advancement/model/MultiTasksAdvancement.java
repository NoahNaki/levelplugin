package me.nakilex.levelplugin.advancement.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiTasksAdvancement extends BaseAdvancement {
    private final List<TaskAdvancement> tasks = new ArrayList<>();

    public MultiTasksAdvancement(AdvancementKey key, AdvancementDisplay display, int maxProgress, Advancement parent) {
        super(key, display, maxProgress, parent);
    }

    public void addTask(TaskAdvancement task) {
        tasks.add(task);
        int sum = tasks.stream().mapToInt(TaskAdvancement::maxProgress).sum();
        if (sum > maxProgress()) {
            tasks.remove(task);
            throw new IllegalArgumentException("Task max progression sum exceeds parent max progression");
        }
    }

    public List<TaskAdvancement> tasks() { return Collections.unmodifiableList(tasks); }
}
