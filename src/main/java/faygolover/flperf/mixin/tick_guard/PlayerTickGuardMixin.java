package faygolover.flperf.mixin.tick_guard;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.FlPerf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Сбой в тике игрока — игрок отключается с понятным сообщением, сервер работает дальше. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerTickGuardMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doTick()V"))
    private void flperf$guard(ServerPlayer player, Operation<Void> original) {
        try {
            original.call(player);
        } catch (Throwable t) {
            FlPerf.LOG.error("[flperf] Сбой в тике игрока {} — отключён, сервер работает дальше", player.getName().getString(), t);
            ((ServerGamePacketListenerImpl) (Object) this).disconnect(Component.literal("Ошибка на сервере в тике игрока (" + t + "). Зайдите снова; если повторится — сообщите админу."));
        }
    }
}
