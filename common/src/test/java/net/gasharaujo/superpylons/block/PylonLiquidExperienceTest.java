package net.gasharaujo.superpylons.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(EphemeralTestServerProvider.class)
final class PylonLiquidExperienceTest {
    @Test
    void fallbackRoundTripsFractionalExperience(MinecraftServer server) {
        var fallback = PylonXpFluidRegistration.FALLBACK.get();
        assertTrue(fallback.defaultFluidState().is(PylonXpFluidRegistration.EXPERIENCE_TAGS.get(0)));
        assertTrue(PylonXpFluids.supports(fallback));
        assertFalse(PylonXpFluids.supports(Fluids.WATER));
        assertFalse(PylonXpFluids.supports(Fluids.FLOWING_WATER));

        var pylon = new SuperPylonBlockEntity(BlockPos.ZERO, SuperPylonsMod.SUPER_PYLON.get().defaultBlockState());
        pylon.loadUpgrades("xp_tank_upgrade");
        pylon.loadSpecialtyAmounts(2, 0);
        assertSame(fallback, pylon.preferredXpFluid());
        assertTrue(PylonXpFluids.canExport(fallback));
        pylon.moveXpFluid(true, fallback, 7);
        assertEquals(47, pylon.xpFluidAmount());
        pylon.moveXpFluid(false, fallback, 28);
        assertEquals(0, pylon.getExperience());
        assertEquals(19, pylon.xpFluidAmount());
    }
}
