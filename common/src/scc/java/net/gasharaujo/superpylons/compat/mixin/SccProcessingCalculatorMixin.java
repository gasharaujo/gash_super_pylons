package net.gasharaujo.superpylons.compat.mixin;

import de.scc.calculator.network.ProcessingCalculatorBlockEntity;
import java.util.Set;
import net.gasharaujo.superpylons.compat.PylonSccCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "de.scc.calculator.network.ProcessingCalculatorBlockEntity", remap = false)
abstract class SccProcessingCalculatorMixin {
    @Inject(method = "categoryLimit", at = @At("HEAD"), cancellable = true, require = 0)
    private void gsp$activeProcessingCategories(CallbackInfoReturnable<Set<String>> result) {
        var calculator = (ProcessingCalculatorBlockEntity) (Object) this;
        var categories = PylonSccCompat.processingCategories(calculator.getLevel(), calculator.machinePos());
        if (categories != null) result.setReturnValue(categories);
    }

    @Inject(method = "runsCategory", at = @At("RETURN"), cancellable = true, require = 0)
    private void gsp$allowSavedCrushOrders(String category, CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValueZ()) return;
        var calculator = (ProcessingCalculatorBlockEntity) (Object) this;
        if (PylonSccCompat.acceptsLegacyCrushCategory(calculator.getLevel(), calculator.machinePos(), category)) {
            result.setReturnValue(true);
        }
    }
}
