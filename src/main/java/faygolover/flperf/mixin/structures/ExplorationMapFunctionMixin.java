package faygolover.flperf.mixin.structures;

import faygolover.flperf.feature.StructureLimits;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Карты сокровищ и исследователя ищут структуру не дальше map_search_radius. */
@Mixin(ExplorationMapFunction.class)
public abstract class ExplorationMapFunctionMixin {
    @Shadow @Final @Mutable int searchRadius;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void flperf$capRadius(CallbackInfo ci) {
        this.searchRadius = Math.min(this.searchRadius, StructureLimits.mapRadius());
    }
}
