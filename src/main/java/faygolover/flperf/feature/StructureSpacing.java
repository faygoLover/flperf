package faygolover.flperf.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Занятые структурами клетки — на каждый генератор чанков свои (только в памяти, как в structureessentials). */
public final class StructureSpacing {
    private static final Map<ChunkGenerator, StructureSpacing> BY_GENERATOR = new WeakHashMap<>();
    private static final int XZ_OFFSET = 3_000_000;
    /** Поток ставит структуру по команде ГМа: расстояние не проверять. */
    public static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> false);

    private final Map<Long, String> cells = new ConcurrentHashMap<>();

    public static synchronized StructureSpacing of(ChunkGenerator generator) {
        return BY_GENERATOR.computeIfAbsent(generator, g -> new StructureSpacing());
    }

    private static long cell(BlockPos pos, int distance, int yOffset) {
        return BlockPos.asLong((pos.getX() + XZ_OFFSET * distance) / distance, (pos.getY() + yOffset) / distance, (pos.getZ() + XZ_OFFSET * distance) / distance);
    }

    public String owner(BlockPos pos, int distance, int yOffset) {
        return cells.get(cell(pos, distance, yOffset));
    }

    /** клетки записываются со сдвигом 2000 по высоте — так делает structureessentials, поведение то же */
    public void claim(BlockPos pos, int distance, String name) {
        cells.put(cell(pos, distance, 2000), name);
    }

    public static List<BlockPos> corners(BoundingBox b) {
        return List.of(
                new BlockPos(b.minX(), b.minY(), b.minZ()), new BlockPos(b.minX(), b.minY(), b.maxZ()),
                new BlockPos(b.minX(), b.maxY(), b.minZ()), new BlockPos(b.minX(), b.maxY(), b.maxZ()),
                new BlockPos(b.maxX(), b.minY(), b.minZ()), new BlockPos(b.maxX(), b.minY(), b.maxZ()),
                new BlockPos(b.maxX(), b.maxY(), b.minZ()), new BlockPos(b.maxX(), b.maxY(), b.maxZ()));
    }
}
