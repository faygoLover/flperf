package faygolover.flperf.mixin.sync_loads;

import com.llamalad7.mixinextras.sugar.Local;
import faygolover.flperf.feature.Chunks;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.SleepInBed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Житель не идёт спать в кровать из незагруженного чанка. */
@Mixin(SleepInBed.class)
public abstract class SleepInBedMixin {
    @Inject(method = "checkExtraStartConditions", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;", ordinal = 0))
    private void flperf$onlyIfLoaded(ServerLevel level, LivingEntity owner, CallbackInfoReturnable<Boolean> cir, @Local GlobalPos bed) {
        if (!Chunks.hasChunk(level, bed.pos())) cir.setReturnValue(false);
    }
}
