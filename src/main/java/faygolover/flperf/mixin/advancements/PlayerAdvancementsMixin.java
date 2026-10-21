package faygolover.flperf.mixin.advancements;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * В файле прогресса игрока остались тысячи выброшенных достижений — ваниль писала бы предупреждение на каждое при входе.
 * Здесь их просто пропускаем (при сохранении ваниль и так пишет только существующие).
 * <p>
 * Моды (MCreator: SCPO и др.) берут достижение по id и, не проверяя на null, спрашивают прогресс или выдают его.
 * Выброшенного достижения нет → null → исключение; при входе оно срывало вход («Invalid player data»).
 * Для отсутствующего достижения: прогресс — пустой, выдать/отозвать — ничего не делать.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    private void flperf$onlyKnown(Stream<Map.Entry<ResourceLocation, AdvancementProgress>> entries,
                                  Consumer<Map.Entry<ResourceLocation, AdvancementProgress>> action,
                                  Operation<Void> original, @Local(argsOnly = true) ServerAdvancementManager manager) {
        original.call(entries.filter(e -> manager.getAdvancement(e.getKey()) != null), action);
    }

    @Inject(method = "getOrStartProgress", at = @At("HEAD"), cancellable = true)
    private void flperf$noProgressForMissing(Advancement advancement, CallbackInfoReturnable<AdvancementProgress> cir) {
        if (advancement == null) cir.setReturnValue(new AdvancementProgress());
    }

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void flperf$noAwardForMissing(Advancement advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (advancement == null) cir.setReturnValue(false);
    }

    @Inject(method = "revoke", at = @At("HEAD"), cancellable = true)
    private void flperf$noRevokeForMissing(Advancement advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (advancement == null) cir.setReturnValue(false);
    }
}
