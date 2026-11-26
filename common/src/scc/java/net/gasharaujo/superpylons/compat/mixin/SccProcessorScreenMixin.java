package net.gasharaujo.superpylons.compat.mixin;

import de.scc.calculator.network.ProcessingCalculatorBlockEntity;
import net.gasharaujo.superpylons.compat.PylonSccCompat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "de.scc.calculator.client.screen.ProcessorConfigScreen", remap = false)
abstract class SccProcessorScreenMixin {
    @Inject(method = "machineStack", at = @At("RETURN"), cancellable = true, require = 0)
    private static void gsp$showUpgradeIcon(ProcessingCalculatorBlockEntity calculator,
                                            CallbackInfoReturnable<ItemStack> result) {
        if (calculator == null || calculator.manualPick()) return;
        var icon = PylonSccCompat.processingIcon(calculator.getLevel(), calculator.machinePos());
        if (!icon.isEmpty()) result.setReturnValue(icon);
    }

    @Inject(method = "machineTitle", at = @At("RETURN"), cancellable = true, require = 0)
    private void gsp$showProcessingUpgrade(ProcessingCalculatorBlockEntity calculator, int width,
                                            CallbackInfoReturnable<String> result) {
        if (calculator == null || calculator.manualPick()) return;
        var label = PylonSccCompat.processingLabel(calculator.getLevel(), calculator.machinePos());
        if (label != null) result.setReturnValue(label.getString());
    }
}
