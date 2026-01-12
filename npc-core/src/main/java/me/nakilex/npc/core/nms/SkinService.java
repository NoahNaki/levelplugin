package me.nakilex.npc.core.nms;

import me.nakilex.npc.core.model.SkinData;
import me.nakilex.npc.core.model.SkinRef;

import java.util.concurrent.CompletableFuture;

public interface SkinService {
    CompletableFuture<SkinData> resolveSkin(SkinRef ref);
}
