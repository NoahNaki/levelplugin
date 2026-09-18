package me.nakilex.playerspoofer;

import java.util.concurrent.CompletableFuture;

interface ChatProvider {
    String id();
    boolean configured();
    String status();
    CompletableFuture<String> generate(ChatPrompt prompt);
}
