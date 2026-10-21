package faygolover.flperf.mixin.terrablender;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.SurfaceBiomes;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Из последовательности правил выкидываются ветки с условием «ложь» и пустые последовательности. */
@Mixin(value = SurfaceRules.SequenceRuleSource.class, priority = 1101)
public abstract class SequenceRuleSourceMixin {
    @WrapOperation(method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$SurfaceRule;",
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;add(Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList$Builder;", remap = false))
    private ImmutableList.Builder<Object> flperf$prune(ImmutableList.Builder<Object> builder, Object value, Operation<ImmutableList.Builder<Object>> original) {
        if (value instanceof SurfaceRules.TestRule test && test.condition() == SurfaceBiomes.Constant.FALSE) return builder;
        if (value instanceof SurfaceRules.SequenceRule seq && seq.rules().isEmpty()) return builder;
        return original.call(builder, value);
    }
}
