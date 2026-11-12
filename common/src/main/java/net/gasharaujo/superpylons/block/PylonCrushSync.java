package net.gasharaujo.superpylons.block;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

public final class PylonCrushSync {
    public static final int CHUNK_SIZE = 16384;
    public record Chunk(int part, int total, byte[] bytes) {}

    private PylonCrushSync() {}

    public static void send(MinecraftServer server, Consumer<Chunk> send) {
        byte[] data = PylonPulverizing.exportRecipes(server.getResourceManager()).getBytes(StandardCharsets.UTF_8);
        int total = Math.max(1, (data.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
        for (int part = 0; part < total; part++) {
            int start = part * CHUNK_SIZE;
            send.accept(new Chunk(part, total, Arrays.copyOfRange(data, start, Math.min(data.length, start + CHUNK_SIZE))));
        }
    }
}
