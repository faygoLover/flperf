package faygolover.flperf.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;

import java.lang.ref.Cleaner;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Очередь буферов на удаление: заполняется Cleaner-потоком, разбирается в тике клиента на потоке отрисовки. */
public final class GpuLeak {
    private GpuLeak() {}

    private static final Cleaner CLEANER = Cleaner.create();
    private static final ConcurrentLinkedQueue<int[]> QUEUE = new ConcurrentLinkedQueue<>();

    public static void watch(Object target, int[] ids) {
        CLEANER.register(target, () -> {
            if (ids[0] > -1 || ids[1] > -1 || ids[2] > -1) QUEUE.add(ids.clone());
        });
    }

    /** вызывать на потоке отрисовки */
    public static void drain() {
        int n = 0;
        int[] ids;
        while (n++ < 20 && (ids = QUEUE.poll()) != null) {
            GlStateManager._bindTexture(0);
            GlStateManager._glBindFramebuffer(36160, 0);
            if (ids[1] > -1) TextureUtil.releaseTextureId(ids[1]);
            if (ids[2] > -1) TextureUtil.releaseTextureId(ids[2]);
            if (ids[0] > -1) GlStateManager._glDeleteFramebuffers(ids[0]);
        }
    }
}
