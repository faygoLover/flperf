package faygolover.flperf.mixin.sync_loads;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasureStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Поиск клада сразу отбрасывает чанки с неподходящим биомом, не читая их с диска. */
@Mixin(StructureCheck.class)
public abstract class StructureCheckMixin {
    @Shadow @Final private ChunkGenerator chunkGenerator;
    @Shadow @Final private RandomState randomState;

    @Inject(method = "checkStart", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/world/level/levelgen/structure/StructureCheck;tryLoadFromStorage(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/structure/Structure;ZJ)Lnet/minecraft/world/level/levelgen/structure/StructureCheckResult;"))
    private void flperf$skipInvalidBiome(ChunkPos chunkPos, Structure structure, boolean skipKnown, CallbackInfoReturnable<StructureCheckResult> cir) {
        if (!(structure instanceof BuriedTreasureStructure)) return;
        BlockPos pos = chunkPos.getMiddleBlockPosition(chunkGenerator.getSeaLevel());
        if (!structure.biomes().contains(chunkGenerator.getBiomeSource().getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2, randomState.sampler()))) {
            cir.setReturnValue(StructureCheckResult.START_NOT_PRESENT);
        }
    }
}
