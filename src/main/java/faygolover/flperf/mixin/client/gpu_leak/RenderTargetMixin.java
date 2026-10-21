package faygolover.flperf.mixin.client.gpu_leak;

import com.mojang.blaze3d.pipeline.RenderTarget;
import faygolover.flperf.client.GpuLeak;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Буфер кадра, который мод забыл уничтожить, освобождается после сборки мусора (через Cleaner, без finalize). */
@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {
    @Shadow public int frameBufferId;
    @Shadow protected int colorTextureId;
    @Shadow protected int depthBufferId;
    @Unique private int[] flperf$ids;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void flperf$register(boolean useDepth, CallbackInfo ci) {
        int[] ids = new int[3];
        java.util.Arrays.fill(ids, -1);
        flperf$ids = ids;
        GpuLeak.watch(this, ids);
    }

    @Inject(method = "createBuffers", at = @At("TAIL"))
    private void flperf$created(int width, int height, boolean clearError, CallbackInfo ci) {
        flperf$ids[0] = frameBufferId;
        flperf$ids[1] = colorTextureId;
        flperf$ids[2] = depthBufferId;
    }

    @Inject(method = "destroyBuffers", at = @At("TAIL"))
    private void flperf$destroyed(CallbackInfo ci) {
        flperf$ids[0] = flperf$ids[1] = flperf$ids[2] = -1;
    }
}
