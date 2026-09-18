package me.nakilex.playerspoofer;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

final class ChatProviderService {
    private final PlayerSpooferPlugin plugin;
    private ChatProviderMode mode;
    private OpenRouterChatProvider openRouter;
    private OllamaChatProvider ollama;

    ChatProviderService(PlayerSpooferPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(2L, plugin.getConfig().getLong("chat.http-connect-timeout-seconds", 4L))))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.mode = ChatProviderMode.fromConfig(plugin.getConfig().getString("chat.provider", "HYBRID"));
        this.openRouter = new OpenRouterChatProvider(plugin, http);
        this.ollama = new OllamaChatProvider(plugin, http);
    }

    ChatProviderMode mode() { return mode; }
    String openRouterStatus() { return openRouter.status(); }
    String ollamaStatus() { return ollama.status(); }

    CompletableFuture<String> generate(ChatPrompt prompt) {
        return switch (mode) {
            case TEMPLATE -> CompletableFuture.failedFuture(new IllegalStateException("Template-only mode"));
            case OPENROUTER -> openRouter.generate(prompt);
            case OLLAMA -> ollama.generate(prompt);
            case HYBRID -> firstSuccessful(List.of(ollama, openRouter), prompt, 0);
        };
    }

    private CompletableFuture<String> firstSuccessful(List<? extends ChatProvider> providers, ChatPrompt prompt, int index) {
        if (index >= providers.size()) return CompletableFuture.failedFuture(new IllegalStateException("No configured LLM provider succeeded"));
        ChatProvider provider = providers.get(index);
        if (!provider.configured()) return firstSuccessful(providers, prompt, index + 1);
        return provider.generate(prompt).handle((text, error) -> new Attempt(text, error)).thenCompose(attempt -> {
            if (attempt.error == null && attempt.text != null && !attempt.text.isBlank()) return CompletableFuture.completedFuture(attempt.text);
            return firstSuccessful(providers, prompt, index + 1);
        });
    }

    private record Attempt(String text, Throwable error) {}
}
