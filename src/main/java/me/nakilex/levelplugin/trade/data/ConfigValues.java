package me.nakilex.levelplugin.trade.data;

import me.nakilex.levelplugin.Main;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigValues {
    private final File customConfigFile;
    FileConfiguration cfg;
    public final int TIME_TRADING_REQUEST_SURVIVES;
    public final String LANGUAGE_VERSION;
    public boolean USE_WITHOUT_PERMISSION;
    public boolean ENABLE_TRADE_BY_RIGHTCLICK_PLAYER;
    public boolean REQUIRE_SHIFT_CLICK;
    public boolean ALLOW_BLOCKING;
    public int MAX_DISTANCE_FOR_USING_TRADE_COMMAND;

    // --- PATHS

    final String TIME_REQUEST_SURVIVES_PATH = "time_until_trade_request_gets_invalid";
    final String LANGUAGE_VERSION_PATH = "language_version";
    final String USE_WITHOUT_PERMISSION_PATH = "use_without_permission";
    final String ENABLE_TRADE_BY_RIGHTCLICK_PLAYER_PATH = "enable_trade_by_right_click_player";
    final String REQUIRE_SHIFT_CLICK_PATH = "require_shift_click";
    final String ALLOW_BLOCKING_PATH = "allow_blocking_trade_requests";
    final String MAX_DISTANCE_FOR_USING_TRADE_COMMAND_PATH = "max_distance_for_using_trade_command";

    public ConfigValues(File file) {
        this.customConfigFile = file;
        cfg = Main.getPlugin().getCustomConfig();

        if(!cfg.contains(TIME_REQUEST_SURVIVES_PATH)) {
            cfg.set(TIME_REQUEST_SURVIVES_PATH, 1);
            this.saveCfg();
        }

        if(!cfg.contains((LANGUAGE_VERSION_PATH))) {
            cfg.set(LANGUAGE_VERSION_PATH, "en_us");
            this.saveCfg();
        }

        if(!cfg.contains((USE_WITHOUT_PERMISSION_PATH))) {
            cfg.set(USE_WITHOUT_PERMISSION_PATH, true);
            this.saveCfg();
        }

        if(!cfg.contains((ENABLE_TRADE_BY_RIGHTCLICK_PLAYER_PATH))) {
            cfg.set(ENABLE_TRADE_BY_RIGHTCLICK_PLAYER_PATH, true);
            this.saveCfg();
        }

        if(!cfg.contains((REQUIRE_SHIFT_CLICK_PATH))) {
            cfg.set(REQUIRE_SHIFT_CLICK_PATH, false);
            this.saveCfg();
        }

        if(!cfg.contains((ALLOW_BLOCKING_PATH))) {
            cfg.set(ALLOW_BLOCKING_PATH, true);
            this.saveCfg();
        }

        if(!cfg.contains((MAX_DISTANCE_FOR_USING_TRADE_COMMAND_PATH))) {
            cfg.set(MAX_DISTANCE_FOR_USING_TRADE_COMMAND_PATH, -1);
            this.saveCfg();
        }

        TIME_TRADING_REQUEST_SURVIVES = cfg.getInt(TIME_REQUEST_SURVIVES_PATH);
        LANGUAGE_VERSION = cfg.getString(LANGUAGE_VERSION_PATH);
        USE_WITHOUT_PERMISSION = cfg.getBoolean(USE_WITHOUT_PERMISSION_PATH);
        ENABLE_TRADE_BY_RIGHTCLICK_PLAYER = cfg.getBoolean(ENABLE_TRADE_BY_RIGHTCLICK_PLAYER_PATH);
        REQUIRE_SHIFT_CLICK = cfg.getBoolean(REQUIRE_SHIFT_CLICK_PATH);
        ALLOW_BLOCKING = cfg.getBoolean(ALLOW_BLOCKING_PATH);
        MAX_DISTANCE_FOR_USING_TRADE_COMMAND = cfg.getInt(MAX_DISTANCE_FOR_USING_TRADE_COMMAND_PATH);
    }

    public boolean toggleUseWithoutPermission() {
        boolean useWithoutPermission = true;
        if(!cfg.contains((USE_WITHOUT_PERMISSION_PATH))) {
            cfg.set(USE_WITHOUT_PERMISSION_PATH, useWithoutPermission);
        } else {
            useWithoutPermission = cfg.getBoolean(USE_WITHOUT_PERMISSION_PATH);
            cfg.set(USE_WITHOUT_PERMISSION_PATH, !useWithoutPermission);
        }
        this.saveCfg();
        return useWithoutPermission;
    }

    private void saveCfg() {
        try {
            cfg.save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}