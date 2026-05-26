package me.nakilex.levelplugin.advancement;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.advancement.model.Advancement;
import me.nakilex.levelplugin.advancement.model.AdvancementDisplay;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Emits a native Minecraft advancement toast by loading a temporary hidden advancement. */
public final class AdvancementToastUtil {
    private AdvancementToastUtil() {}

    public static void showToast(Player player, Advancement advancement) {
        if (player == null || advancement == null) return;

        AdvancementDisplay display = advancement.display();
        String materialId = toMinecraftMaterial(display == null ? null : display.icon());
        String frame = (display == null || display.frameType() == null)
                ? "task"
                : display.frameType().name().toLowerCase(Locale.ROOT);
        String title = jsonEscape(display == null ? advancement.key().value() : String.valueOf(display.title()));
        String description = jsonEscape(display == null || display.description().isEmpty()
                ? ""
                : String.join(" ", display.description()));

        String unique = "toast_" + advancement.key().namespace() + "_" + advancement.key().value() + "_" + UUID.randomUUID().toString().replace("-", "");
        NamespacedKey tempKey = new NamespacedKey(Main.getInstance(), unique.toLowerCase(Locale.ROOT));

        String json = "{" +
                "\"display\":{" +
                "\"icon\":{\"item\":\"" + materialId + "\"}," +
                "\"title\":\"" + title + "\"," +
                "\"description\":\"" + description + "\"," +
                "\"frame\":\"" + frame + "\"," +
                "\"show_toast\":true,\"announce_to_chat\":false,\"hidden\":true" +
                "}," +
                "\"criteria\":{\"impossible\":{\"trigger\":\"minecraft:impossible\"}}" +
                "}";

        org.bukkit.advancement.Advancement bukkitAdv = Bukkit.getUnsafe().loadAdvancement(tempKey, json);
        if (bukkitAdv == null) return;

        var progress = player.getAdvancementProgress(bukkitAdv);
        Set<String> criteria = progress.getRemainingCriteria();
        for (String c : criteria) progress.awardCriteria(c);

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> Bukkit.getUnsafe().removeAdvancement(tempKey), 40L);
    }

    private static String toMinecraftMaterial(Material material) {
        Material safe = material == null ? Material.PAPER : material;
        String key = safe.getKey().getKey();
        return "minecraft:" + key;
    }

    private static String jsonEscape(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
