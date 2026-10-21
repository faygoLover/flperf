package faygolover.flperf.feature;

import faygolover.flperf.FlPerf;
import faygolover.flperf.PerfConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Пределы поиска структур (по образцу structureessentials). Время считается на поток, в котором идёт поиск. */
public final class StructureLimits {
    private StructureLimits() {}

    private static final ThreadLocal<long[]> STATE = ThreadLocal.withInitial(() -> new long[3]); // [начало мс, глубина, уже сообщили]
    private static final ThreadLocal<String> NAME = new ThreadLocal<>();
    private static final ThreadLocal<Integer> LOCATE = new ThreadLocal<>();

    private static int cfg(java.util.function.IntSupplier s, int def) {
        try { return s.getAsInt(); } catch (IllegalStateException notLoaded) { return def; }
    }

    public static int radiusCap() {
        Integer locate = LOCATE.get();
        return locate != null ? locate : cfg(PerfConfig.STRUCT_GLOBAL_RADIUS::get, 70);
    }

    public static int mapRadius() { return cfg(PerfConfig.STRUCT_MAP_RADIUS::get, 40); }

    public static int locateRadius() { return cfg(PerfConfig.STRUCT_LOCATE_RADIUS::get, 110); }

    public static void beginLocate() { LOCATE.set(locateRadius()); }

    public static void endLocate() { LOCATE.remove(); }

    public static void start(HolderSet<Structure> structures) {
        long[] s = STATE.get();
        if (s[1]++ == 0) {
            s[0] = System.currentTimeMillis();
            s[2] = 0;
            String name = "?";
            for (Holder<Structure> h : structures) {
                if (h.unwrapKey().isPresent()) { name = h.unwrapKey().get().location().toString(); break; }
            }
            NAME.set(name);
        }
    }

    public static void end() {
        long[] s = STATE.get();
        if (s[1] > 0 && --s[1] == 0) s[0] = 0;
    }

    public static boolean timedOut() {
        long[] s = STATE.get();
        if (s[0] == 0) return false;
        long limit = cfg(PerfConfig.STRUCT_TIMEOUT_S::get, 50) * 1000L + (LOCATE.get() != null ? 30_000L : 0L);
        if (System.currentTimeMillis() - s[0] <= limit) return false;
        if (s[2] == 0) {
            s[2] = 1;
            FlPerf.LOG.info("[flperf] Поиск структуры {} прерван: дольше {} с", NAME.get(), limit / 1000);
        }
        return true;
    }
}
