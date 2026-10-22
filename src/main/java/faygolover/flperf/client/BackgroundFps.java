package faygolover.flperf.client;

import faygolover.flperf.PerfClientConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

/**
 * Кадры и звук, когда игра не в фокусе (по образцу Dynamic FPS, его значения по умолчанию): мышь над окном — до 60 кадров,
 * окно не в фокусе — 1 кадр в секунду и звук 25%, свёрнуто — кадры не рисуются и звук 0. Игровые тики идут как обычно,
 * пропускается только отрисовка. Состояние окна опрашивается раз в кадр (без колбэков GLFW — не мешаем чужим).
 */
public final class BackgroundFps {
    private BackgroundFps() {}

    private enum State { FOCUSED, HOVERED, UNFOCUSED, MINIMIZED }

    private static State state = State.FOCUSED;
    private static long lastFrame;

    /** вызывается из GameRenderer.render раз в кадр; true — этот кадр не рисовать */
    public static boolean skipFrame() {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        State now;
        if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE) now = State.MINIMIZED;
        else if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE) now = State.FOCUSED;
        else if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_HOVERED) == GLFW.GLFW_TRUE) now = State.HOVERED;
        else now = State.UNFOCUSED;
        if (now != state) {
            state = now;
            applyVolume(mc);
        }
        int target = switch (state) {
            case FOCUSED -> -1;
            case HOVERED -> PerfClientConfig.HOVERED_FPS.get();
            case UNFOCUSED -> PerfClientConfig.UNFOCUSED_FPS.get();
            case MINIMIZED -> 0;
        };
        if (target < 0) return false;
        if (target == 0) return true;
        long t = Util.getMillis();
        if (t - lastFrame < 1000L / target) return true;
        lastFrame = t;
        return false;
    }

    private static void applyVolume(Minecraft mc) {
        double factor = switch (state) {
            case FOCUSED, HOVERED -> 1.0;
            case UNFOCUSED -> PerfClientConfig.UNFOCUSED_VOLUME.get();
            case MINIMIZED -> 0.0;
        };
        float own = mc.options.getSoundSourceVolume(SoundSource.MASTER);
        // Never down to 0 ourselves: at 0 vanilla stops every sound playing (and plays no new ones), so
        // mods' looping sounds were gone for good after the game was minimised. Practically silent instead.
        float volume = (float) Math.max(own * factor, own > 0.0f ? 1.0E-4 : 0.0);
        mc.getSoundManager().updateSourceVolume(SoundSource.MASTER, volume);
    }
}
