package faygolover.flperf.mixin.structures;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.FlPerf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Фича (дерево, руда, декор мода), упавшая при расстановке, пропускается с записью в лог — генерация чанка не падает. */
@Mixin(PlacedFeature.class)
public abstract class PlacedFeatureMixin {
    private static final String CALL = "Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;placeWithContext(Lnet/minecraft/world/level/levelgen/placement/PlacementContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z";

    @WrapOperation(method = {"place", "placeWithBiomeCheck"}, at = @At(value = "INVOKE", target = CALL))
    private boolean flperf$guard(PlacedFeature self, PlacementContext context, RandomSource random, BlockPos pos, Operation<Boolean> original) {
        try {
            return original.call(self, context, random, pos);
        } catch (Exception e) {
            String key = context.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getResourceKey(self)
                    .map(k -> k.location().toString()).orElse(String.valueOf(self));
            FlPerf.LOG.warn("[flperf] Фича {} упала при расстановке в {} — пропущена", key, pos, e);
            return false;
        }
    }
}
