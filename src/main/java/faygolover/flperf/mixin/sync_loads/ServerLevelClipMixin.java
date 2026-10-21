package faygolover.flperf.mixin.sync_loads;

import faygolover.flperf.feature.Chunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Supplier;

/** Луч (стрелы, взгляд мобов, моды) с концом в незагруженном чанке — промах в конечной точке, без загрузки чанка. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelClipMixin extends Level {
    private ServerLevelClipMixin(WritableLevelData data, ResourceKey<Level> key, RegistryAccess access, Holder<DimensionType> type,
                                 Supplier<ProfilerFiller> profiler, boolean client, boolean debug, long seed, int maxUpdates) {
        super(data, key, access, type, profiler, client, debug, seed, maxUpdates);
    }

    @Override
    public BlockHitResult clip(ClipContext context) {
        Vec3 to = context.getTo();
        if (Chunks.hasChunk(this, Mth.floor(to.x) >> 4, Mth.floor(to.z) >> 4)) return super.clip(context);
        Vec3 d = context.getFrom().subtract(to);
        return BlockHitResult.miss(to, Direction.getNearest(d.x, d.y, d.z), BlockPos.containing(to));
    }
}
