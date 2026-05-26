package me.nakilex.levelplugin.advancement;

import me.nakilex.levelplugin.advancement.model.*;
import me.nakilex.levelplugin.advancement.persistence.InMemoryAdvancementDatabase;
import org.bukkit.Material;

import java.util.Set;

public final class AdvancementModule {
    private static final AdvancementService SERVICE = new AdvancementService(new InMemoryAdvancementDatabase());
    private static boolean initialized;

    private AdvancementModule() {}

    public static AdvancementService service() {
        if (!initialized) {
            initialized = true;
            registerDefaults();
        }
        return SERVICE;
    }

    private static void registerDefaults() {
        AdvancementTab tab = new AdvancementTab("levelplugin");
        RootAdvancement root = new RootAdvancement(new AdvancementKey("levelplugin", "root"),
                new AdvancementDisplay.Builder(Material.BOOK).title("LevelPlugin Journey").descriptionLine("Start your adventure").build(),
                1, "minecraft:textures/block/stone.png");
        BaseAdvancement child = new BaseAdvancement(new AdvancementKey("levelplugin", "first_steps"),
                new AdvancementDisplay.Builder(Material.WOODEN_SWORD).title("First Steps").descriptionLine("Complete your first objective").build(),
                1, root);
        tab.registerAdvancements(root, Set.of(child));
        SERVICE.registerTab(tab);
    }
}
