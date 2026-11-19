package net.gasharaujo.superpylons.block;

import java.util.List;
import java.util.function.Consumer;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PylonXpFluidRegistration {
    static final List<TagKey<Fluid>> EXPERIENCE_TAGS = List.of(
        TagKey.create(Registries.FLUID, new ResourceLocation("c", "experience")),
        TagKey.create(Registries.FLUID, new ResourceLocation("forge", "experience")));

    private static final DeferredRegister<FluidType> TYPES =
        DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, SuperPylonsMod.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
        DeferredRegister.create(ForgeRegistries.FLUIDS, SuperPylonsMod.MOD_ID);

    private static final RegistryObject<FluidType> TYPE = TYPES.register(PylonLiquidExperience.NAME,
        () -> new FluidType(FluidType.Properties.create()) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override public ResourceLocation getStillTexture() {
                        return new ResourceLocation("minecraft", "block/water_still");
                    }
                    @Override public ResourceLocation getFlowingTexture() {
                        return new ResourceLocation("minecraft", "block/water_flow");
                    }
                    @Override public int getTintColor() { return 0xFF81CF2F; }
                });
            }
        });
    static final RegistryObject<Fluid> FALLBACK = FLUIDS.register(PylonLiquidExperience.NAME,
        () -> new PylonLiquidExperience() {
            @Override public FluidType getFluidType() { return TYPE.get(); }
        });

    private PylonXpFluidRegistration() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
    }
}
