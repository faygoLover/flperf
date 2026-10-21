package faygolover.flperf.mixin.terrablender;

import faygolover.flperf.feature.SurfaceBiomes;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** На время построения поверхности чанка запоминаем регион генерации (контексту нужны соседние чанки). */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    private static final String BUILD = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V";

    @Inject(method = BUILD, at = @At("HEAD"))
    private void flperf$begin(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk, CallbackInfo ci) {
        SurfaceBiomes.REGION.set(region);
    }

    @Inject(method = BUILD, at = @At("TAIL"))
    private void flperf$end(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk, CallbackInfo ci) {
        SurfaceBiomes.REGION.remove();
    }
}
