package faygolover.flperf.mixin.terrablender;

import faygolover.flperf.feature.SurfaceBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

/** Контекст правил поверхности знает, какие биомы есть в чанке и в 8 соседних (по образцу TerraBlenderFix). */
@Mixin(SurfaceRules.Context.class)
public abstract class SurfaceContextMixin implements SurfaceBiomes.Holder {
    @Shadow @Final ChunkAccess chunk;
    @Unique private Set<ResourceKey<Biome>> flperf$biomes;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void flperf$collect(CallbackInfo ci) {
        WorldGenRegion region = SurfaceBiomes.REGION.get();
        if (region == null) return;
        Set<ResourceKey<Biome>> set = new HashSet<>();
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (LevelChunkSection section : region.getChunk(cx + x, cz + z).getSections()) {
                    section.getBiomes().getAll(b -> b.unwrapKey().ifPresent(set::add));
                }
            }
        }
        flperf$biomes = set;
    }

    @Override
    public Set<ResourceKey<Biome>> flperf$surroundingBiomes() {
        return flperf$biomes;
    }
}
