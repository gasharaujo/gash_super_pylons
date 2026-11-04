package net.gasharaujo.superpylons;

import net.gasharaujo.superpylons.block.PylonControlRequest;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PylonControls {
    private static final String VERSION = "1";
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
