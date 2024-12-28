package me.nakilex.levelplugin.mob.data;

import me.nakilex.levelplugin.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class CustomMob {

    public static final NamespacedKey MOB_ID_KEY = new NamespacedKey(Main.getInstance(), "mob_id");
    public static final NamespacedKey LEVEL_KEY  = new NamespacedKey(Main.getInstance(), "mob_level");

    public static final NamespacedKey XP_KEY = new NamespacedKey(Main.getInstance(), "mob_xp"); // store xp

    public static final PersistentDataType<String, String> STRING_TAG = PersistentDataType.STRING;
    public static final PersistentDataType<Integer, Integer> INT_TAG = PersistentDataType.INTEGER;
}
