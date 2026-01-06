package me.nakilex.levelplugin.player.profile;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.listeners.StaticItemListener;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.utils.BetterHudUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class ProfileEntryUtil {

    private ProfileEntryUtil() {
    }

    public static void saveActiveProfile(Player player) {
        if (player == null) {
            return;
        }
        boolean profilesEnabled = Main.getInstance().getCustomConfig()
                .getBoolean("features.profiles", true);
        ProfileManager pm = ProfileManager.getInstance();
        if (profilesEnabled) {
            pm.saveActiveProfile(player);
        } else {
            pm.saveProfile(player, 0);
        }
    }

    public static void clearInventory(Player player) {
        if (player == null) {
            return;
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
    }

    public static void clearActiveSlot(Player player) {
        if (player == null) {
            return;
        }
        boolean profilesEnabled = Main.getInstance().getCustomConfig()
                .getBoolean("features.profiles", true);
        if (profilesEnabled) {
            ProfileManager.getInstance().clearActiveSlot(player.getUniqueId());
        }
    }

    public static void handleProfileEntry(Player player) {
        if (player == null) {
            return;
        }
        boolean profilesEnabled = Main.getInstance().getCustomConfig()
                .getBoolean("features.profiles", true);
        UUID pid = player.getUniqueId();

        if (!profilesEnabled) {
            ProfileManager pm = ProfileManager.getInstance();
            if (pm.getProfile(pid, 0) == null) {
                pm.createProfile(pid, 0, "Profile 1");
            }
            pm.setActiveSlot(pid, 0);
            PlayerConfig cfg = Main.getInstance().getPlayerConfig();
            org.bukkit.Location loc = cfg.getProfileLocation(pid, 0);
            if (loc != null) {
                player.teleport(loc);
            }
            clearInventory(player);
            ItemStack[] contents = cfg.getProfileInventory(pid, 0);
            ItemStack[] armor = cfg.getProfileArmor(pid, 0);
            if (contents.length > 0) {
                player.getInventory().setContents(contents);
            } else {
                StaticItemListener.giveStaticItems(player);
            }
            if (armor.length > 0) {
                player.getInventory().setArmorContents(armor);
            }
            BetterHudUtil.addHud(player);
        } else {
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                if (!player.isOnline()) {
                    return;
                }
                ProfileManager.getInstance().clearActiveSlot(pid);
                ProfileSelectionGUI.startSelection(player);
            }, 30L);
        }
    }
}
