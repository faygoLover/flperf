package faygolover.flperf.mixin.sync_loads;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.Chunks;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Слушатели игровых событий (скалк и т.п.) обновляются, только если чанк уже загружен. */
@Mixin(DynamicGameEventListener.class)
public abstract class DynamicGameEventListenerMixin {
    @WrapOperation(method = "ifChunkExists", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelReader;getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private static ChunkAccess flperf$onlyIfLoaded(LevelReader level, int x, int z, ChunkStatus status, boolean load, Operation<ChunkAccess> original) {
        return Chunks.getChunkNow(level, x, z);
    }
}
