package net.gasharaujo.superpylons.compat;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.gasharaujo.superpylons.block.SuperPylonBlock;
import net.gasharaujo.superpylons.block.SuperPylonBlockEntity;

public final class PylonWthitPlugin implements IWailaClientPlugin, IBlockComponentProvider {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(this, SuperPylonBlock.class);
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlockEntity() instanceof SuperPylonBlockEntity pylon) {
            tooltip.addLine(PylonTooltip.storageLine(pylon));
        }
    }
}
