package faygolover.flperf.mixin.client.advancements_hide_ui;

import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Уведомлений о достижениях нет (только если advancements_hide_ui). */
@Mixin(ToastComponent.class)
public abstract class ToastComponentMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void flperf$noAdvancementToasts(Toast toast, CallbackInfo ci) {
        if (toast instanceof AdvancementToast) ci.cancel();
    }
}
