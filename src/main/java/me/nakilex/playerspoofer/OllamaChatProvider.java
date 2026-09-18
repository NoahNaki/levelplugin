package me.nakilex.playerspoofer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

final class OllamaChatProvider implements ChatProvider {
    private final PlayerSpooferPlugin plugin;
    private final HttpClient http;
    private final boolean enabled;
    private final String endpoint;
    private final String model;
    private final String apiKey;
    private final double temperature;
    private final int maxTokens;
    private final int maxConcurrent;
    private final Duration timeout;
    private final AtomicInteger inFlight = new AtomicInteger();
    private volatile long backoffUntil;
    private volatile String lastError = "never called";

    OllamaChatProvider(PlayerSpooferPlugin plugin, HttpClient http) {
        this.plugin = plugin;
        this.http = http;
        this.enabled = plugin.getConfig().getBoolean("chat.ollama.enabled", false);
        this.endpoint = plugin.getConfig().getString("chat.ollama.endpoint", "http://127.0.0.1:11434/api/chat");
        this.model = plugin.getConfig().getString("chat.ollama.model", "gemma3:4b");
        this.apiKey = resolveSecret(plugin.getConfig().getString("chat.ollama.api-key", ""));
        this.temperature = clamp(plugin.getConfig().getDouble("chat.ollama.temperature", 0.9D), 0D, 2D);
        this.maxTokens = Math.max(16, plugin.getConfig().getInt("chat.ollama.max-tokens", 80));
        this.maxConcurrent = Math.max(1, plugin.getConfig().getInt("chat.ollama.max-concurrent", 2));
        this.timeout = Duration.ofSeconds(Math.max(3L, plugin.getConfig().getLong("chat.ollama.timeout-seconds", 10L)));
    }

    @Override public String id() { return "ollama"; }

    @Override
    public boolean configured() {
        return enabled && endpoint != null && !endpoint.isBlank() && model != null && !model.isBlank();
    }

    @Override
    public String status() {
        if (!enabled) return "disabled";
        if (System.currentTimeMillis() < backoffUntil) return "backoff: " + lastError;
        return "ready (model=" + model + ", in-flight=" + inFlight.get() + ")";
    }

    @Override
    public CompletableFuture<String> generate(ChatPrompt prompt) {
        if (!configured()) return CompletableFuture.failedFuture(new IllegalStateException("Ollama is not configured"));
        if (System.currentTimeMillis() < backoffUntil) return CompletableFuture.failedFuture(new IllegalStateException("Ollama is in backoff"));
        if (inFlight.incrementAndGet() > maxConcurrent) {
            inFlight.decrementAndGet();
            return CompletableFuture.failedFuture(new IllegalStateException("Ollama concurrency limit reached"));
        }

        String body = "{" +
                "\"model\":" + SimpleJson.quote(model) + ',' +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":" + SimpleJson.quote(prompt.system()) + "}," +
                "{\"role\":\"user\",\"content\":" + SimpleJson.quote(prompt.user()) + "}]," +
                "\"stream\":false," +
                "\"think\":false," +
                "\"options\":{\"temperature\":" + temperature + ",\"num_predict\":" + maxTokens + "}" +
                "}";

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("User-Agent", "PlayerSpoofer/1.6.0")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (apiKey != null && !apiKey.isBlank()) builder.header("Authorization", "Bearer " + apiKey);

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
                        throw new IllegalStateException("Ollama returned no message content");
                    }
                    lastError = "ok";
                    backoffUntil = 0L;
                    return content;
                })
                .whenComplete((unused, error) -> {
                    inFlight.decrementAndGet();
                    if (error != null) markFailure(rootMessage(error));
                });
    }

    private void markFailure(String error) {
        lastError = error == null ? "request failed" : error;
        long seconds = Math.max(5L, plugin.getConfig().getLong("chat.ollama.failure-backoff-seconds", 30L));
        backoffUntil = System.currentTimeMillis() + seconds * 1000L;
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
