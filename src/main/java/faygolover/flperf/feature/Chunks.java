package faygolover.flperf.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

/** «Чанк уже загружен?» — без загрузки (по образцу ServerCore). Вне основного потока сервера ответ «нет». */
public final class Chunks {
    private Chunks() {}

    @Nullable
    public static ChunkAccess getChunkNow(LevelReader level, int chunkX, int chunkZ) {
        if (level instanceof ServerLevel server) return server.getChunkSource().getChunkNow(chunkX, chunkZ);
        return level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    public static boolean hasChunk(Level level, int chunkX, int chunkZ) {
        return !(level instanceof ServerLevel) || getChunkNow(level, chunkX, chunkZ) != null;
    }

    public static boolean hasChunk(Level level, @Nullable BlockPos pos) {
        return pos == null || hasChunk(level, pos.getX() >> 4, pos.getZ() >> 4);
    }
}
