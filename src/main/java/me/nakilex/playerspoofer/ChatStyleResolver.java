package me.nakilex.playerspoofer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Tries to make packet-only fake chat resemble the server's normal rank/prefix styling.
 * Fake players cannot enter Paper's real AsyncChatEvent pipeline, so we resolve Vault chat
 * metadata when available and render it through the configurable fake-message format.
 */
final class ChatStyleResolver {
    private final PlayerSpooferPlugin plugin;

    ChatStyleResolver(PlayerSpooferPlugin plugin) {
        this.plugin = plugin;
    }

    String render(FakePlayer bot, String safeMessage) {
        String format = plugin.getConfig().getString(
                "chat.display.format",
                plugin.getConfig().getString("chat.message-format", "{prefix}&f{name}{suffix}&7: &f{message}")
        );
        if (format == null || format.isBlank()) format = "{prefix}&f{name}{suffix}&7: &f{message}";

        String prefix = plugin.getConfig().getString("chat.display.fallback-prefix", "");
        String suffix = plugin.getConfig().getString("chat.display.fallback-suffix", "");
        if (plugin.getConfig().getBoolean("chat.display.use-vault-prefix", true)) {
            String resolvedPrefix = resolveVaultMeta(bot, true);
            String resolvedSuffix = resolveVaultMeta(bot, false);
            if (resolvedPrefix != null && !resolvedPrefix.isBlank()) prefix = resolvedPrefix;
            if (resolvedSuffix != null && !resolvedSuffix.isBlank()) suffix = resolvedSuffix;
        }

        return format
                .replace("{prefix}", prefix == null ? "" : prefix)
                .replace("{suffix}", suffix == null ? "" : suffix)
                .replace("{name}", bot.name())
                .replace("{message}", safeMessage);
    }

    String modeDescription() {
        boolean vault = plugin.getConfig().getBoolean("chat.display.use-vault-prefix", true);
        if (!vault) return "configured format";
        try {
            Class.forName("net.milkbowl.vault.chat.Chat", false, plugin.getClass().getClassLoader());
            return "configured format + Vault prefix/suffix";
        } catch (Throwable ignored) {
            return "configured format (Vault chat API unavailable)";
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String resolveVaultMeta(FakePlayer bot, boolean prefix) {
        try {
            Class<?> chatClass = Class.forName("net.milkbowl.vault.chat.Chat");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration((Class) chatClass);
            if (registration == null || registration.getProvider() == null) return null;
            Object provider = registration.getProvider();
            String methodName = prefix ? "getPlayerPrefix" : "getPlayerSuffix";
            OfflinePlayer offline = Bukkit.getOfflinePlayer(bot.uuid());
            String world = configuredWorld();

            for (Method method : provider.getClass().getMethods()) {
                if (!method.getName().equals(methodName)) continue;
                Class<?>[] params = method.getParameterTypes();
                try {
                    Object result = null;
                    if (params.length == 2 && params[0] == String.class && OfflinePlayer.class.isAssignableFrom(params[1])) {
                        result = method.invoke(provider, world, offline);
                    } else if (params.length == 2 && params[0] == String.class && params[1] == String.class) {
                        result = method.invoke(provider, world, bot.name());
                    } else if (params.length == 1 && OfflinePlayer.class.isAssignableFrom(params[0])) {
                        result = method.invoke(provider, offline);
                    } else if (params.length == 1 && params[0] == String.class) {
                        result = method.invoke(provider, bot.name());
                    }
                    if (result != null) return String.valueOf(result);
                } catch (Throwable ignored) {
                    // Try the next overload/provider signature.
                }
            }
        } catch (Throwable ignored) {
            // Vault is optional for styling; the configured fallback remains valid.
        }
        return null;
    }

    private String configuredWorld() {
        String configured = plugin.getConfig().getString("chat.display.vault-world", "");
        if (configured != null && !configured.isBlank()) return configured;
        World world = Bukkit.getWorlds().stream().findFirst().orElse(null);
        return world == null ? "world" : world.getName();
    }
}
