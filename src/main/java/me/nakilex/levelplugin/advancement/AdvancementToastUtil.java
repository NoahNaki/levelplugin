package me.nakilex.levelplugin.advancement;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.advancement.model.Advancement;
import me.nakilex.levelplugin.advancement.model.AdvancementDisplay;
import me.nakilex.levelplugin.advancement.model.AdvancementKey;
import me.nakilex.levelplugin.advancement.model.BaseAdvancement;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/** Emits a native Minecraft advancement toast while replacing vanilla toast audio with LevelPlugin's Nexo sound. */
public final class AdvancementToastUtil {
    private static final String ACHIEVEMENT_SOUND = "nexo:ui.achievement";

    private AdvancementToastUtil() {}


    /** Shows an ad-hoc toast while reusing the native temporary-advancement pipeline. */
    public static void showToast(Player player, Material icon, String title, String description,
                                 AdvancementDisplay.FrameType frameType) {
        AdvancementDisplay display = new AdvancementDisplay.Builder(icon)
                .title(title)
                .descriptionLine(description)
                .frameType(frameType)
                .build();
        showToast(player, new BaseAdvancement(new AdvancementKey("levelplugin", "notification"), display, 1, null));
    }

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
                "\"icon\":{\"id\":\"" + materialId + "\"}," +
                "\"title\":\"" + title + "\"," +
                "\"description\":\"" + description + "\"," +
                "\"frame\":\"" + frame + "\"," +
                "\"show_toast\":true,\"announce_to_chat\":false,\"hidden\":true" +
                "}," +
                "\"criteria\":{\"impossible\":{\"trigger\":\"minecraft:impossible\"}}" +
                "}";

        try {
            org.bukkit.advancement.Advancement bukkitAdv = Bukkit.getUnsafe().loadAdvancement(tempKey, json);
            if (bukkitAdv == null) return;

            suppressVanillaToastSounds(player);
            var progress = player.getAdvancementProgress(bukkitAdv);
            Collection<String> criteria = progress.getRemainingCriteria();
            for (String c : criteria) progress.awardCriteria(c);
            suppressVanillaToastSounds(player);
            playAchievementSound(player);

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> suppressVanillaToastSounds(player), 1L);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> Bukkit.getUnsafe().removeAdvancement(tempKey), 40L);
        } catch (Exception ignored) {
            // Never fail command execution due to a toast formatting/runtime issue.
        }
    }

    public static void playAchievementSound(Player player) {
        if (player == null || !canHearAchievementSound(player)) {
            return;
        }
        player.playSound(player.getLocation(), ACHIEVEMENT_SOUND, 1.0f, 1.0f);
    }

    private static boolean canHearAchievementSound(Player player) {
        Main plugin = Main.getInstance();
        if (plugin == null || plugin.getSettingsManager() == null) {
            return true;
        }
        return plugin.getSettingsManager().getSettings(player).isAchievementSoundEffectsEnabled();
    }

    private static void suppressVanillaToastSounds(Player player) {
        player.stopSound(Sound.UI_TOAST_IN);
        player.stopSound(Sound.UI_TOAST_OUT);
        player.stopSound(Sound.UI_TOAST_CHALLENGE_COMPLETE);
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
