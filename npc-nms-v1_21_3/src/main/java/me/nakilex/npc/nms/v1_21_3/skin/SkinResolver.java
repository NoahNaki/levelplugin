package me.nakilex.npc.nms.v1_21_3.skin;

import me.nakilex.npc.core.model.SkinData;

import java.util.concurrent.CompletableFuture;

public interface SkinResolver {
    CompletableFuture<SkinData> resolve(String url);
}
