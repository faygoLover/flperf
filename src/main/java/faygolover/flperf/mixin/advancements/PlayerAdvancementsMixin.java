package faygolover.flperf.mixin.advancements;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * В файле прогресса игрока остались тысячи выброшенных достижений — ваниль писала бы предупреждение на каждое при входе.
 * Здесь их просто пропускаем (при сохранении ваниль и так пишет только существующие).
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    private void flperf$onlyKnown(Stream<Map.Entry<ResourceLocation, AdvancementProgress>> entries,
                                  Consumer<Map.Entry<ResourceLocation, AdvancementProgress>> action,
                                  Operation<Void> original, @Local(argsOnly = true) ServerAdvancementManager manager) {
        original.call(entries.filter(e -> manager.getAdvancement(e.getKey()) != null), action);
    }
}
