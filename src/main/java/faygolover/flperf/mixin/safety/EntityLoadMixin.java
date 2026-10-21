package faygolover.flperf.mixin.safety;

import com.llamalad7.mixinextras.sugar.Local;
import faygolover.flperf.FlPerf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Сущность с битыми данными не роняет сервер при загрузке чанка (по образцу Cupboard). */
@Mixin(Entity.class)
public abstract class EntityLoadMixin {
    /** координаты или поворот NaN/бесконечность → ставим на 0 70 0 / поворот 0, вместо исключения «invalid position» */
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setYBodyRot(F)V", shift = At.Shift.AFTER))
    private void flperf$fixInvalid(CompoundTag tag, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!Double.isFinite(self.getX()) || !Double.isFinite(self.getY()) || !Double.isFinite(self.getZ())) {
            FlPerf.LOG.warn("[flperf] Сущность {} с неверными координатами — поставлена на 0 70 0", self.getType());
            self.setPosRaw(0.0, 70.0, 0.0);
        }
        if (!Float.isFinite(self.getYRot()) || !Float.isFinite(self.getXRot())) {
            self.setYRot(0.0F);
            self.setXRot(0.0F);
        }
    }

    /** любая другая ошибка чтения → сущность остаётся недочитанной, вместо падения сервера */
    @Inject(method = "load", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/CrashReport;forThrowable(Ljava/lang/Throwable;Ljava/lang/String;)Lnet/minecraft/CrashReport;"))
    private void flperf$skipBroken(CompoundTag tag, CallbackInfo ci, @Local Throwable error) {
        FlPerf.LOG.warn("[flperf] Не прочитать сущность {} — пропущена вместо падения", ((Entity) (Object) this).getType(), error);
        ci.cancel();
    }
}
