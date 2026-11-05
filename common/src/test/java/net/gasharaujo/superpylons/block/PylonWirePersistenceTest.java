package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
final class PylonWirePersistenceTest {
    @Test
    void incomingReferencesKeepSourceIdentityAcrossReload(MinecraftServer server) {
        var state = SuperPylonsMod.SUPER_PYLON.get().defaultBlockState();
        var pos = new BlockPos(10, 70, 10);
        var pylon = new SuperPylonBlockEntity(pos, state);
        var input = new PylonWire(PylonWire.Type.ITEM, pos.west(32), UUID.randomUUID());
        pylon.loadWireInputs(input.encode());
        var loaded = new SuperPylonBlockEntity(pos, state);
        PylonTestSerialization.load(loaded, pylon.getUpdateTag(server.registryAccess()), server);
        assertEquals(input.encode(), loaded.saveWireInputs());
        assertTrue(loaded.outgoingWires().isEmpty());
    }

    @Test
    void invalidAndDuplicateIncomingReferencesAreIgnored(MinecraftServer server) {
        var pos = new BlockPos(10, 70, 10);
        var pylon = new SuperPylonBlockEntity(pos, SuperPylonsMod.SUPER_PYLON.get().defaultBlockState());
        var valid = new PylonWire(PylonWire.Type.FLUID, pos.above(), UUID.randomUUID());
        var far = new PylonWire(PylonWire.Type.FLUID, pos.east(33), UUID.randomUUID());
        var self = new PylonWire(PylonWire.Type.FLUID, pos, UUID.randomUUID());
        pylon.loadWireInputs(valid.encode() + "," + valid.encode() + "," + far.encode() + "," + self.encode() + ",invalid");
        assertEquals(valid.encode(), pylon.saveWireInputs());
    }
}
