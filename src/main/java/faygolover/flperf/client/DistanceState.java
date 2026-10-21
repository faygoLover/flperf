package faygolover.flperf.client;

import faygolover.flperf.PerfClientConfig;
import net.minecraft.client.Minecraft;

/**
 * Дальность эллипсом: расстояние считается с растяжкой — по вертикали в vertical_scale раз, поперёк взгляда в side_scale раз.
 * Поворот камеры обновляется раз в тик ({@link #update()}), в кадре — только умножения.
 */
public final class DistanceState {
    private DistanceState() {}

    private static float cos = 1, sin = 0, side2 = 1, vert2 = 1;
    private static boolean entities = true;

    public static void update() {
        Minecraft mc = Minecraft.getInstance();
        float yaw = mc.gameRenderer.getMainCamera().getYRot() * ((float) Math.PI / 180F);
        cos = (float) Math.cos(yaw);
        sin = (float) Math.sin(yaw);
        float s = PerfClientConfig.SIDE_SCALE.get().floatValue();
        float v = PerfClientConfig.VERTICAL_SCALE.get().floatValue();
        side2 = s * s;
        vert2 = v * v;
        entities = PerfClientConfig.AFFECT_ENTITIES.get();
    }

    public static boolean affectEntities() { return entities; }

    /** квадрат «растянутого» расстояния */
    public static double scaledSqr(double dx, double dy, double dz) {
        double across = dx * cos + dz * sin;     // поперёк взгляда
        double along = -dx * sin + dz * cos;     // вдоль взгляда
        return across * across * side2 + along * along + dy * dy * vert2;
    }

    public static boolean inside(float dx, float dy, float dz, float radius) {
        return scaledSqr(dx, dy, dz) <= (double) radius * radius;
    }
}
