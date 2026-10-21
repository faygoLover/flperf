package faygolover.flperf.mixin.safety;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.ThreadingDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.stream.Stream;

/** Ошибка «доступ из нескольких потоков» показывает стеки всех потоков — видно, какой мод полез не из своего потока. */
@Mixin(ThreadingDetector.class)
public abstract class ThreadingDetectorMixin {
    @WrapOperation(method = "makeThreadingException", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;of([Ljava/lang/Object;)Ljava/util/stream/Stream;"))
    private static Stream<Object> flperf$allThreads(Object[] threads, Operation<Stream<Object>> original) {
        return Thread.getAllStackTraces().keySet().stream().map(t -> (Object) t);
    }
}
