package me.nakilex.levelplugin.playerhead;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/** Hosts {@link PlayerModelResourcePack}'s zip over plain HTTP so it can be sent as a second, independent resource pack. */
public final class PlayerModelResourcePackServer {
    private static final String CONTEXT_PATH = "/playermodel-pack.zip";

    private final HttpServer server;
    private final String publicUrl;

    public PlayerModelResourcePackServer(Plugin plugin, PlayerModelResourcePack pack,
                                         String bindHost, int port, String publicAddress) throws IOException {
        server = HttpServer.create(new InetSocketAddress(bindHost, port), 0);
        server.createContext(CONTEXT_PATH, exchange -> {
            byte[] body = pack.zipBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "LevelPlugin-PlayerModelPack");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        this.publicUrl = "http://" + publicAddress + ":" + port + CONTEXT_PATH;
        plugin.getLogger().info("Player-model shader pack hosted at " + publicUrl);
    }

    public String publicUrl() {
        return publicUrl;
    }

    public void stop() {
        server.stop(0);
    }
}
