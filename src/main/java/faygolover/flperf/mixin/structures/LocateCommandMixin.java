package faygolover.flperf.mixin.structures;

import faygolover.flperf.feature.StructureLimits;
import net.minecraft.server.commands.LocateCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** /locate: радиус locate_search_radius и запас времени +30 с (команду зовёт админ осознанно). */
@Mixin(LocateCommand.class)
public abstract class LocateCommandMixin {
    @ModifyConstant(method = "locateStructure", constant = @Constant(intValue = 100), require = 0)
    private static int flperf$radius(int vanilla) {
        return StructureLimits.locateRadius();
    }

    @Inject(method = "locateStructure", at = @At("HEAD"))
    private static void flperf$begin(CallbackInfoReturnable<Integer> cir) {
        StructureLimits.beginLocate();
    }

    @Inject(method = "locateStructure", at = @At("RETURN"))
    private static void flperf$end(CallbackInfoReturnable<Integer> cir) {
        StructureLimits.endLocate();
    }
}
