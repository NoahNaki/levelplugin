package me.nakilex.levelplugin.advancement;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.advancement.model.Advancement;
import me.nakilex.levelplugin.advancement.model.AdvancementDisplay;
import me.nakilex.levelplugin.advancement.model.AdvancementKey;
import me.nakilex.levelplugin.advancement.model.BaseAdvancement;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Emits LevelPlugin achievement popups without triggering vanilla Minecraft advancement sounds. */
public final class AdvancementToastUtil {
    private static final String ACHIEVEMENT_SOUND = "nexo:ui.achievement";

    private AdvancementToastUtil() {}


    /** Shows an ad-hoc achievement popup using LevelPlugin's custom sound/visual pipeline. */
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
        String title = display == null ? advancement.key().value() : String.valueOf(display.title());
        String description = display == null || display.description().isEmpty()
                ? ""
                : String.join(" ", display.description());

        playAchievementSound(player);
        showAchievementPopup(player, title, description);
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

    private static void showAchievementPopup(Player player, String title, String description) {
        player.sendTitle(ChatColor.GOLD + "Achievement Unlocked!", ChatColor.YELLOW + title, 10, 60, 20);
        StringBuilder message = new StringBuilder(ChatColor.GOLD + "Achievement Unlocked! "
                + ChatColor.YELLOW + title);
        if (description != null && !description.isBlank()) {
            message.append(ChatColor.GRAY).append(" - ").append(ChatColor.WHITE).append(description);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD, message.toString());
    }
}
