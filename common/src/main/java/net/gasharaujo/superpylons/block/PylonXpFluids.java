package net.gasharaujo.superpylons.block;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

final class PylonXpFluids {
    static final int MILLIBUCKETS_PER_POINT = 20;
    private static final List<String> SOURCE_IDS = List.of(
        "justdirethings:xp_fluid_source", "enderio:xp_juice", "mob_grinding_utils:fluid_xp"
    );

    private PylonXpFluids() {}

    static String id(Fluid fluid) { return BuiltInRegistries.FLUID.getKey(fluid).toString(); }
    static boolean supports(Fluid fluid) { return SOURCE_IDS.contains(id(fluid)); }

    static List<Fluid> available() { return Available.FLUIDS; }

    private static final class Available {
        private static final List<Fluid> FLUIDS = BuiltInRegistries.FLUID.stream().filter(PylonXpFluids::supports).toList();
    }

    static long millibuckets(long points) {
        return points > Long.MAX_VALUE / MILLIBUCKETS_PER_POINT ? Long.MAX_VALUE : points * MILLIBUCKETS_PER_POINT;
    }
}
