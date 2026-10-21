package faygolover.flperf.mixin.sync_loads;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import faygolover.flperf.feature.Chunks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Пчела не проверяет улей в незагруженном чанке (иначе проверка грузит чанк). */
@Mixin(Bee.class)
public abstract class BeeMixin {
    @Shadow @Nullable BlockPos hivePos;

    @ModifyExpressionValue(method = "isHiveValid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Bee;isTooFarAway(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean flperf$onlyIfLoaded(boolean tooFar) {
        return tooFar || !Chunks.hasChunk(((Bee) (Object) this).level(), hivePos);
    }
}
