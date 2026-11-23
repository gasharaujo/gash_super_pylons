package net.gasharaujo.superpylons.block;

import java.util.List;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PylonXpFluidRegistration {
    static final List<TagKey<Fluid>> EXPERIENCE_TAGS = List.of(Tags.Fluids.EXPERIENCE);

    private static final DeferredRegister<FluidType> TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, SuperPylonsMod.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
        DeferredRegister.create(Registries.FLUID, SuperPylonsMod.MOD_ID);

    private static final DeferredHolder<FluidType, FluidType> TYPE = TYPES.register(PylonLiquidExperience.NAME,
        () -> new FluidType(FluidType.Properties.create()));
    public static final DeferredHolder<Fluid, Fluid> FALLBACK = FLUIDS.register(PylonLiquidExperience.NAME,
        () -> new PylonLiquidExperience() {
            @Override public FluidType getFluidType() { return TYPE.get(); }
        });

    private PylonXpFluidRegistration() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
