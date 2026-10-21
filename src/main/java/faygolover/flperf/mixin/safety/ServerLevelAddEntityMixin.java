package faygolover.flperf.mixin.safety;

import faygolover.flperf.FlPerf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Мод добавляет сущность не из потока сервера → она встаёт в очередь и добавляется в потоке сервера (по образцу Cupboard). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelAddEntityMixin {
    @Unique private final ConcurrentLinkedQueue<Entity> flperf$late = new ConcurrentLinkedQueue<>();
    @Unique private static final Set<EntityType<?>> flperf$warned = ConcurrentHashMap.newKeySet();
    @Unique private boolean flperf$adding;

    @Shadow protected abstract boolean addEntity(Entity entity);

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void flperf$offThread(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel self = (ServerLevel) (Object) this;
        if (!self.getServer().isSameThread()) {
            if (flperf$warned.add(entity.getType())) FlPerf.LOG.warn("[flperf] {} добавлен не из потока сервера — отложен", entity.getType(), new Exception("след"));
            flperf$late.add(entity);
            cir.setReturnValue(true);
        } else if (!flperf$adding && !flperf$late.isEmpty()) {
            flperf$adding = true;
            try {
                Entity e;
                while ((e = flperf$late.poll()) != null) addEntity(e);
            } finally {
                flperf$adding = false;
            }
        }
    }
}
