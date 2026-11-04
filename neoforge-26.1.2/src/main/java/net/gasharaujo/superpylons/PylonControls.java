package net.gasharaujo.superpylons;

import net.gasharaujo.superpylons.block.PylonControlRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record PylonControls(PylonControlRequest request) implements CustomPacketPayload {
    private static final Type<PylonControls> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "controls"));
    private static final StreamCodec<RegistryFriendlyByteBuf, PylonControls> CODEC = StreamCodec.of(
        (buffer, packet) -> {
            var message = packet.request();
            buffer.writeVarInt(message.menuId());
            buffer.writeVarInt(message.slot());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, message.item());
            buffer.writeUtf(message.tag(), 256);
        },
        buffer -> new PylonControls(new PylonControlRequest(buffer.readVarInt(), buffer.readVarInt(),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readUtf(256))));

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (packet, context) -> packet.request().handle(context.player()));
    }

    public static void send(PylonControlRequest request) { ClientPacketDistributor.sendToServer(new PylonControls(request)); }

    @Override public Type<PylonControls> type() { return TYPE; }
}
