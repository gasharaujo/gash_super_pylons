package net.gasharaujo.superpylons.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public final class PylonCoreRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        SuperPylonsMod.MOD_ID, "textures/block/core.png");
    private final ModelPart core = PylonCoreModel.create();

    public void render(PoseStack pose, MultiBufferSource buffers, float ticks, int light, int overlay) {
        pose.pushPose();
        PylonAnimation.apply(pose, ticks);
        core.render(pose, buffers.getBuffer(RenderType.entitySolid(TEXTURE)), light, overlay);
        pose.popPose();
    }

    boolean renderFluid(SuperPylonBlockEntity entity, PoseStack pose, MultiBufferSource buffers,
                        float ticks, int light, int overlay) {
        var handler = entity.getFluidHandler();
        if (handler == null) return false;
        var stack = handler.getFluidInTank(0);
        if (stack.isEmpty()) return false;
        var fluid = stack.getFluid();
        var state = fluid.defaultFluidState();
        var extension = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = extension.getStillTexture(stack);
        if (texture == null) texture = extension.getStillTexture(state, entity.getLevel(), entity.getBlockPos());
        if (texture == null) return false;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return false;

        int tint = extension.getTintColor(stack);
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            tint = extension.getTintColor(state, entity.getLevel(), entity.getBlockPos()) | 0xFF000000;
        }
        int fluidLight = LightTexture.pack(Math.max(LightTexture.block(light), fluid.getFluidType().getLightLevel(stack)),
            LightTexture.sky(light));
        boolean translucent = (tint >>> 24) < 255 || ItemBlockRenderTypes.getRenderLayer(state) == RenderType.translucent();
        PylonFluidCore visual = new PylonFluidCore(sprite, tint, fluidLight, translucent);
        RenderType type = visual.translucent() ? RenderType.entityTranslucent(sprite.atlasLocation())
            : RenderType.entityCutout(sprite.atlasLocation());
        pose.pushPose();
        PylonAnimation.apply(pose, ticks);
        PoseStack.Pose matrix = pose.last();
        VertexConsumer buffer = buffers.getBuffer(type);
        visual.draw((corner, normal, u, v) -> buffer.addVertex(matrix, corner.x(), corner.y(), corner.z())
            .setColor(visual.tint()).setUv(u, v).setOverlay(overlay).setLight(visual.light())
            .setNormal(matrix, normal.getStepX(), normal.getStepY(), normal.getStepZ()));
        pose.popPose();
        return true;
    }
}
