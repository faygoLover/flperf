package faygolover.flperf.mixin.sync_loads;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.Chunks;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Зомби, ищущий яйца черепах, не грузит чанки. */
@Mixin(RemoveBlockGoal.class)
public abstract class RemoveBlockGoalMixin {
    @WrapOperation(method = "isValidTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelReader;getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"))
    private ChunkAccess flperf$onlyIfLoaded(LevelReader level, int x, int z, ChunkStatus status, boolean load, Operation<ChunkAccess> original) {
        return Chunks.getChunkNow(level, x, z);
    }
}
