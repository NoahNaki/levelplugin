package me.nakilex.npc.nms.v1_21_3.skin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.nakilex.npc.core.model.SkinData;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpSkinResolver implements SkinResolver {
    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;
    private final ExecutorService executor;

    public HttpSkinResolver(URI endpoint, Duration timeout) {
        this.endpoint = endpoint;
        this.timeout = timeout;
        this.executor = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder()
                .setNameFormat("npc-skin-%d")
                .setDaemon(true)
                .build());
        this.client = HttpClient.newBuilder()
                .executor(executor)
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public CompletableFuture<SkinData> resolve(String url) {
        String encoded = URLEncoder.encode(url, StandardCharsets.UTF_8);
        URI target = endpoint.resolve("?url=" + encoded);
        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(timeout)
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("Skin resolver returned " + response.statusCode());
                    }
                    String body = response.body();
                    String texture = extractJson(body, "texture");
                    String signature = extractJson(body, "signature");
                    if (texture == null || signature == null) {
                        throw new IllegalStateException("Skin resolver response missing texture data");
                    }
                    return new SkinData(texture, signature);
                });
    }

    private String extractJson(String body, String key) {
        String search = "\"" + key + "\"";
        int index = body.indexOf(search);
        if (index == -1) {
            return null;
        }
        int colon = body.indexOf(':', index);
        int startQuote = body.indexOf('"', colon + 1);
        int endQuote = body.indexOf('"', startQuote + 1);
        if (startQuote == -1 || endQuote == -1) {
            return null;
        }
        return body.substring(startQuote + 1, endQuote);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
