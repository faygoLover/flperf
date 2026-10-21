package faygolover.flperf.mixin.sync_loads;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.Chunks;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Карта в руках не грузит чанки, которые рисует: незагруженные пропускаются. */
@Mixin(MapItem.class)
public abstract class MapItemMixin {
    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk flperf$onlyIfLoaded(Level level, int x, int z, Operation<LevelChunk> original) {
        return Chunks.getChunkNow(level, x, z) instanceof LevelChunk chunk ? chunk : null;
    }

    @WrapOperation(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;isEmpty()Z"))
    private boolean flperf$nullIsEmpty(LevelChunk chunk, Operation<Boolean> original) {
        return chunk == null || original.call(chunk);
    }
}
