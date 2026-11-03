package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class SuperPylonItemRenderer implements SpecialModelRenderer<Float> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final PylonCoreRenderer core = new PylonCoreRenderer();

    @Override
    public void submit(@Nullable Float ticks, PoseStack pose, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outline) {
        BlockModelRenderState frame = new BlockModelRenderState();
        Minecraft.getInstance().getBlockModelResolver().update(
            frame, SuperPylonsMod.SUPER_PYLON.get().defaultBlockState(), DISPLAY_CONTEXT);
        frame.submitMultiLayer(pose, collector, light, overlay, outline);
        core.submit(pose, collector, ticks == null ? 0 : ticks, light, overlay, outline, null);
    }

    @Override
    public Float extractArgument(ItemStack stack) {
        return PylonAnimation.itemTicks(Util.getMillis());
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x <= 1; x++) for (int y = 0; y <= 1; y++)
            for (int z = 0; z <= 1; z++) output.accept(new Vector3f(x, y * 14.0F / 16.0F, z));
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<Float> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SuperPylonItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new SuperPylonItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<Float>> type() { return MAP_CODEC; }
    }
}
