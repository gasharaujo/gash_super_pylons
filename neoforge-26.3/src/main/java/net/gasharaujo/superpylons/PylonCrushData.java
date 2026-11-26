package net.gasharaujo.superpylons;

import net.gasharaujo.superpylons.block.PylonCrushSync;
import net.gasharaujo.superpylons.client.PylonCrushRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record PylonCrushData(PylonCrushSync.Chunk data) implements CustomPacketPayload {
    private static final Type<PylonCrushData> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "crush_recipes"));
    private static final StreamCodec<RegistryFriendlyByteBuf, PylonCrushData> CODEC = StreamCodec.of(
        (buffer, packet) -> {
            buffer.writeVarInt(packet.data().part());
            buffer.writeVarInt(packet.data().total());
            buffer.writeByteArray(packet.data().bytes());
        },
        buffer -> new PylonCrushData(new PylonCrushSync.Chunk(buffer.readVarInt(), buffer.readVarInt(),
            buffer.readByteArray(PylonCrushSync.CHUNK_SIZE))));

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (packet, context) ->
            PylonCrushRecipes.receive(packet.data().part(), packet.data().total(), packet.data().bytes()));
    }

    static void sync(OnDatapackSyncEvent event) {
        var players = event.getPlayer() == null ? event.getPlayerList().getPlayers() : java.util.List.of(event.getPlayer());
        PylonCrushSync.send(event.getPlayerList().getServer(), data -> {
            for (var player : players) PacketDistributor.sendToPlayer(player, new PylonCrushData(data));
        });
    }

    static void tagsUpdated(TagsUpdatedEvent event) {
        if (event instanceof TagsUpdatedEvent.ClientPacketReceived) {
            PylonCrushRecipes.tagsUpdated();
        }
    }

    @Override public Type<PylonCrushData> type() { return TYPE; }
}
