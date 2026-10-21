package faygolover.flperf.mixin.tick_guard;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.TickGuard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/** Тик сущности: замороженная не тикает; исключение — заморозка вместо падения сервера (на клиенте тоже). */
@Mixin(Level.class)
public abstract class LevelEntityGuardMixin {
    @WrapOperation(method = "guardEntityTick", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private <T extends Entity> void flperf$guard(Consumer<T> tick, Object entity, Operation<Void> original) {
        if (TickGuard.isFrozen(entity)) return;
        try {
            original.call(tick, entity);
        } catch (Throwable t) {
            TickGuard.entityFailed((Entity) entity, t);
        }
    }
}
