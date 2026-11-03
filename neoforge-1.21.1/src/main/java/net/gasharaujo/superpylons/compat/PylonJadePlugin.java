package net.gasharaujo.superpylons.compat;

import net.gasharaujo.superpylons.SuperPylonsMod;
import net.gasharaujo.superpylons.block.SuperPylonBlock;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(SuperPylonsMod.MOD_ID)
public final class PylonJadePlugin implements IWailaPlugin, IBlockComponentProvider {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(this, SuperPylonBlock.class);
    }

    @Override
    public ResourceLocation getUid() {
        return ResourceLocation.fromNamespaceAndPath(SuperPylonsMod.MOD_ID, "storage");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof SuperPylonBlockEntity pylon) {
            tooltip.add(PylonTooltip.storageLine(pylon));
        }
    }
}
