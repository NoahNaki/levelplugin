package me.nakilex.playerspoofer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class OpenRouterChatProvider implements ChatProvider {
    private final PlayerSpooferPlugin plugin;
    private final HttpClient http;
    private final boolean enabled;
    private final String endpoint;
    private final String apiKey;
    private final List<String> models;
    private final double temperature;
    private final int maxTokens;
    private final Duration timeout;
    private final String referer;
    private final SlidingWindowRateLimiter limiter;
    private volatile long backoffUntil;
    private volatile String lastError = "never called";

    OpenRouterChatProvider(PlayerSpooferPlugin plugin, HttpClient http) {
        this.plugin = plugin;
        this.http = http;
        this.enabled = plugin.getConfig().getBoolean("chat.openrouter.enabled", true);
        this.endpoint = plugin.getConfig().getString("chat.openrouter.endpoint", "https://openrouter.ai/api/v1/chat/completions");
        this.apiKey = resolveApiKey(plugin, plugin.getConfig().getString("chat.openrouter.api-key", "${OPENROUTER_API_KEY}"));
        List<String> configuredModels = new ArrayList<>(plugin.getConfig().getStringList("chat.openrouter.models"));
        if (configuredModels.isEmpty()) configuredModels.add("openrouter/free");
        this.models = configuredModels.stream().filter(v -> v != null && !v.isBlank()).distinct().toList();
        this.temperature = clamp(plugin.getConfig().getDouble("chat.openrouter.temperature", 0.9D), 0D, 2D);
        this.maxTokens = Math.max(16, plugin.getConfig().getInt("chat.openrouter.max-tokens", 80));
        this.timeout = Duration.ofSeconds(Math.max(3L, plugin.getConfig().getLong("chat.openrouter.timeout-seconds", 12L)));
        this.referer = plugin.getConfig().getString("chat.openrouter.http-referer", "");
        this.limiter = new SlidingWindowRateLimiter(
                plugin.getConfig().getInt("chat.openrouter.max-requests-per-minute", 4),
                plugin.getConfig().getInt("chat.openrouter.max-requests-per-hour", 40),
                plugin.getConfig().getInt("chat.openrouter.max-requests-per-day", 45)
        );
    }

    @Override public String id() { return "openrouter"; }

    @Override
    public boolean configured() {
        return enabled && endpoint != null && !endpoint.isBlank() && apiKey != null && !apiKey.isBlank() && !models.isEmpty();
    }

    @Override
    public String status() {
        if (!enabled) return "disabled";
        if (apiKey == null || apiKey.isBlank()) return "missing API key (OPENROUTER_API_KEY or plugins/PlayerSpoofer/openrouter.key)";
        if (System.currentTimeMillis() < backoffUntil) return "backoff: " + lastError;
        return "ready (models=" + String.join(",", models) + ", used=" + limiter.usedLastMinute() + "/m "
                + limiter.usedLastHour() + "/h " + limiter.usedLastDay() + "/d)";
    }

    @Override
    public CompletableFuture<String> generate(ChatPrompt prompt) {
        if (!configured()) return CompletableFuture.failedFuture(new IllegalStateException("OpenRouter is not configured"));
        if (System.currentTimeMillis() < backoffUntil) return CompletableFuture.failedFuture(new IllegalStateException("OpenRouter is in backoff"));
        if (!limiter.tryAcquire()) return CompletableFuture.failedFuture(new IllegalStateException("OpenRouter rate limit reached"));

        StringBuilder modelJson = new StringBuilder("[");
        for (int i = 0; i < models.size(); i++) {
            if (i > 0) modelJson.append(',');
            modelJson.append(SimpleJson.quote(models.get(i)));
        }
        modelJson.append(']');

        String body = "{" +
                "\"models\":" + modelJson + ',' +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":" + SimpleJson.quote(prompt.system()) + "}," +
                "{\"role\":\"user\",\"content\":" + SimpleJson.quote(prompt.user()) + "}]," +
                "\"temperature\":" + temperature + ',' +
                "\"max_tokens\":" + maxTokens + ',' +
                "\"stream\":false" +
                "}";

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Title", "PlayerSpoofer")
                .header("User-Agent", "PlayerSpoofer/1.6.5")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (referer != null && !referer.isBlank()) builder.header("HTTP-Referer", referer);

        return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        String message = "HTTP " + response.statusCode();
                        markFailure(message);
                        throw new IllegalStateException(message);
                    }
                    String content = SimpleJson.contentAfterMessage(response.body());
                    if (content == null || content.isBlank()) {
                        markFailure("empty response");
                        throw new IllegalStateException("OpenRouter returned no message content");
                    }
                    lastError = "ok";
                    backoffUntil = 0L;
                    return content;
                })
                .exceptionallyCompose(error -> {
                    markFailure(rootMessage(error));
                    return CompletableFuture.failedFuture(error);
                });
    }

    private void markFailure(String error) {
        lastError = error == null ? "request failed" : error;
        long seconds = Math.max(10L, plugin.getConfig().getLong("chat.openrouter.failure-backoff-seconds", 60L));
        backoffUntil = System.currentTimeMillis() + seconds * 1000L;
    }

    private static String resolveApiKey(PlayerSpooferPlugin plugin, String raw) {
        String configured = resolveSecret(raw);
        if (!configured.isBlank()) return configured;

        String fileName = plugin.getConfig().getString("chat.openrouter.api-key-file", "openrouter.key");
        if (fileName == null || fileName.isBlank()) return "";
        try {
            Path base = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
            Path file = base.resolve(fileName).normalize();
            // Keep the convenience key file inside the plugin data directory.
            if (!file.startsWith(base) || !Files.isRegularFile(file)) return "";
            String value = Files.readString(file).trim();
            return value.startsWith("sk-") ? value : value;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String resolveSecret(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.startsWith("${") && value.endsWith("}") && value.length() > 3) {
            String env = System.getenv(value.substring(2, value.length() - 1));
            return env == null ? "" : env.trim();
        }
        return value;
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
