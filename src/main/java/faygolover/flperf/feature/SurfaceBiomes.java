package faygolover.flperf.feature;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.Set;

/** Регион генерации текущего потока (на время buildSurface) и постоянные условия для отсечённых правил. */
public final class SurfaceBiomes {
    private SurfaceBiomes() {}

    public static final ThreadLocal<WorldGenRegion> REGION = new ThreadLocal<>();

    public interface Holder {
        Set<ResourceKey<Biome>> flperf$surroundingBiomes();
    }

    public record Constant(boolean value) implements SurfaceRules.Condition {
        public static final Constant TRUE = new Constant(true);
        public static final Constant FALSE = new Constant(false);

        @Override
        public boolean test() {
            return value;
        }
    }
}
