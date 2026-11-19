package net.gasharaujo.superpylons.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

final class PylonXpFluids {
    static final int MILLIBUCKETS_PER_POINT = 20;
    private static final List<String> SOURCE_IDS = List.of(
        "justdirethings:xp_fluid_source", "enderio:xp_juice", "mob_grinding_utils:fluid_xp"
    );

    private PylonXpFluids() {}

    static String id(Fluid fluid) { return BuiltInRegistries.FLUID.getKey(fluid).toString(); }

    static boolean supports(Fluid fluid) {
        if (fluid == Fluids.EMPTY || !fluid.defaultFluidState().isSource()) return false;
        if (fluid == PylonXpFluidRegistration.FALLBACK.get() || SOURCE_IDS.contains(id(fluid))) return true;
        for (TagKey<Fluid> tag : PylonXpFluidRegistration.EXPERIENCE_TAGS) {
            if (fluid.defaultFluidState().is(tag)) return true;
        }
        return false;
    }

    static boolean canExport(Fluid fluid) {
        return supports(fluid) && (fluid != PylonXpFluidRegistration.FALLBACK.get() || externalSources().isEmpty());
    }

    static Fluid preferred(String storedId) {
        List<Fluid> sources = externalSources();
        for (Fluid fluid : sources) {
            if (id(fluid).equals(storedId)) return fluid;
        }
        return sources.isEmpty() ? PylonXpFluidRegistration.FALLBACK.get() : sources.get(0);
    }

    static List<Fluid> externalSources() {
        List<Fluid> sources = new ArrayList<>(Legacy.FLUIDS);
        for (TagKey<Fluid> tag : PylonXpFluidRegistration.EXPERIENCE_TAGS) {
            for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
                Fluid fluid = holder.value();
                if (fluid != Fluids.EMPTY && fluid != PylonXpFluidRegistration.FALLBACK.get() &&
                    fluid.defaultFluidState().isSource() && !sources.contains(fluid)) {
                    sources.add(fluid);
                }
            }
        }
        return sources;
    }

    private static final class Legacy {
        private static final List<Fluid> FLUIDS = BuiltInRegistries.FLUID.stream()
            .filter(fluid -> SOURCE_IDS.contains(id(fluid)) && fluid.defaultFluidState().isSource()).toList();
    }

    static long millibuckets(long points) {
        return points > Long.MAX_VALUE / MILLIBUCKETS_PER_POINT ? Long.MAX_VALUE : points * MILLIBUCKETS_PER_POINT;
    }
}
