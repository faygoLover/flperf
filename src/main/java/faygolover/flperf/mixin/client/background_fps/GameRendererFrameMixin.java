package faygolover.flperf.mixin.client.background_fps;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import faygolover.flperf.client.BackgroundFps;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Кадр не рисуется, если окно свёрнуто или не в фокусе и до следующего кадра по лимиту ещё рано (тики идут как обычно). */
@Mixin(GameRenderer.class)
public abstract class GameRendererFrameMixin {
    @ModifyExpressionValue(method = "render(FJZ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;noRender:Z", opcode = 180))
    private boolean flperf$skipInBackground(boolean noRender) {
        return noRender || BackgroundFps.skipFrame();
    }
}
