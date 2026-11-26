package net.gasharaujo.superpylons.compat.mixin;

import de.scc.calculator.network.ProcessingCalculatorBlockEntity;
import net.gasharaujo.superpylons.compat.PylonSccCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "de.scc.calculator.network.TerminalBlockEntity", remap = false)
abstract class SccTerminalMixin {
    @Redirect(method = "refreshNow", at = @At(value = "INVOKE",
        target = "Lde/scc/calculator/network/ProcessingCalculatorBlockEntity;patternModule()Z"), require = 0)
    private boolean gsp$skipAutomaticLimitsForPatterns(ProcessingCalculatorBlockEntity calculator) {
        return calculator.patternModule() && (calculator.manualPick() ||
            PylonSccCompat.processingCategories(calculator.getLevel(), calculator.machinePos()) == null);
    }
}
