package me.nakilex.levelplugin.stronghold.util;

import me.nakilex.levelplugin.stronghold.StrongholdTemplateData;
import org.bukkit.World;

import java.util.Locale;

public final class StrongholdWorldUtil {
    private StrongholdWorldUtil() {
    }

    public static boolean isStrongholdWorld(World world) {
        if (world == null || world.getName() == null) {
            return false;
        }
        String worldName = world.getName().toLowerCase(Locale.ROOT);
        return worldName.startsWith(StrongholdTemplateData.generatedWorldPrefix()) || worldName.contains("stronghold");
    }
}
