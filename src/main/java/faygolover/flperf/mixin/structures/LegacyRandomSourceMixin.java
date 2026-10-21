package faygolover.flperf.mixin.structures;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Мод, взявший общий рандом из нескольких потоков, ваниль роняет («Accessing LegacyRandomSource from multiple threads»).
 * Здесь — без падения: рандом продолжает работать (как в structureessentials, который сейчас стоит в сборке).
 */
@Mixin(value = LegacyRandomSource.class, priority = 5)
public abstract class LegacyRandomSourceMixin {
    @WrapOperation(method = "next", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/atomic/AtomicLong;compareAndSet(JJ)Z"), require = 0)
    private boolean flperf$noCrash(AtomicLong seed, long expected, long next, Operation<Boolean> original) {
        original.call(seed, expected, next);
        return true;
    }
}
