package faygolover.flperf.mixin.client.distance;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import faygolover.flperf.client.DistanceState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Дальность сущностей — тем же эллипсом, что и чанки. */
@Mixin(Entity.class)
public abstract class EntityDistanceMixin {
    @WrapOperation(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;shouldRenderAtSqrDistance(D)Z"))
    private boolean flperf$ellipse(Entity self, double distSqr, Operation<Boolean> original,
                                   @Local(argsOnly = true, ordinal = 0) double camX, @Local(argsOnly = true, ordinal = 1) double camY,
                                   @Local(argsOnly = true, ordinal = 2) double camZ) {
        if (!DistanceState.affectEntities()) return original.call(self, distSqr);
        return original.call(self, DistanceState.scaledSqr(self.getX() - camX, self.getY() - camY, self.getZ() - camZ));
    }
}
