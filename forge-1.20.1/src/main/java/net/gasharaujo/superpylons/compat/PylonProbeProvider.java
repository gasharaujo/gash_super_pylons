package net.gasharaujo.superpylons.compat;

import java.util.function.Function;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.InterModComms;

public final class PylonProbeProvider implements IProbeInfoProvider {
    public static void enqueueRegistration() {
        InterModComms.sendTo(SuperPylonsMod.MOD_ID, "theoneprobe", "getTheOneProbe",
            () -> (Function<ITheOneProbe, Void>) probe -> {
                probe.registerProvider(new PylonProbeProvider());
                return null;
            });
    }

    @Override
    public ResourceLocation getID() {
        return new ResourceLocation(SuperPylonsMod.MOD_ID, "storage");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level,
                             BlockState state, IProbeHitData hit) {
        if (state.is(SuperPylonsMod.SUPER_PYLON.get()) &&
            level.getBlockEntity(hit.getPos()) instanceof SuperPylonBlockEntity pylon) {
            PylonTooltip.append(pylon, info::text);
        }
    }
}
