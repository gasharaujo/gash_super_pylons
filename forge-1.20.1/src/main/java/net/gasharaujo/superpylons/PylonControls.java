package net.gasharaujo.superpylons;

import net.gasharaujo.superpylons.block.PylonControlRequest;
import net.gasharaujo.superpylons.block.PylonCrushSync;
import net.gasharaujo.superpylons.client.PylonCrushRecipes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PylonControls {
    private static final String VERSION = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(SuperPylonsMod.MOD_ID, "controls"), () -> VERSION, VERSION::equals, VERSION::equals);

    private PylonControls() {}

    static void register() {
        CHANNEL.messageBuilder(PylonControlRequest.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder(PylonControls::write).decoder(PylonControls::read)
            .consumerMainThread((message, context) -> {
                var sender = context.get().getSender();
                if (sender != null) message.handle(sender);
            }).add();
        CHANNEL.messageBuilder(PylonCrushSync.Chunk.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((message, buffer) -> {
                buffer.writeVarInt(message.part());
                buffer.writeVarInt(message.total());
                buffer.writeByteArray(message.bytes());
            })
            .decoder(buffer -> new PylonCrushSync.Chunk(buffer.readVarInt(), buffer.readVarInt(),
                buffer.readByteArray(PylonCrushSync.CHUNK_SIZE)))
            .consumerMainThread((message, context) ->
                PylonCrushRecipes.receive(message.part(), message.total(), message.bytes())).add();
    }

    static void syncCrush(OnDatapackSyncEvent event) {
        var players = event.getPlayer() == null ? event.getPlayerList().getPlayers() : java.util.List.of(event.getPlayer());
        PylonCrushSync.send(event.getPlayerList().getServer(), data -> {
            for (ServerPlayer player : players) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), data);
        });
    }

    static void tagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            PylonCrushRecipes.tagsUpdated();
        }
    }

    public static void send(PylonControlRequest request) { CHANNEL.sendToServer(request); }

    private static void write(PylonControlRequest message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.menuId());
        buffer.writeVarInt(message.slot());
        buffer.writeItem(message.item());
        buffer.writeUtf(message.tag(), 256);
    }

    private static PylonControlRequest read(FriendlyByteBuf buffer) {
        return new PylonControlRequest(buffer.readVarInt(), buffer.readVarInt(), buffer.readItem(), buffer.readUtf(256));
    }
}
