package faygolover.flperf.mixin.structures;

import faygolover.flperf.PerfConfig;
import faygolover.flperf.feature.StructureSpacing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * Новая структура не ставится ближе min_structure_distance к другой (как в structureessentials, который сейчас в сборке):
 * пространство делится на клетки этого размера, каждая клетка помнит, какая структура её заняла.
 */
@Mixin(Structure.class)
public abstract class StructureMinDistanceMixin {
    @Inject(method = "generate", at = @At("RETURN"), cancellable = true)
    private void flperf$keepApart(RegistryAccess access, ChunkGenerator generator, BiomeSource biomes, RandomState random,
                                  StructureTemplateManager templates, long seed, ChunkPos chunk, int references,
                                  LevelHeightAccessor height, Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> cir) {
        StructureStart start = cir.getReturnValue();
        int distance;
        try { distance = PerfConfig.STRUCT_MIN_DISTANCE.get(); } catch (IllegalStateException e) { distance = 32; }
        if (start == StructureStart.INVALID_START || distance <= 0) return;
        Structure self = (Structure) (Object) this;
        ResourceLocation id = access.registryOrThrow(Registries.STRUCTURE).getKey(self);
        String name = id != null ? id.toString() : "unknown:" + self;
        int yOffset = self.step() == GenerationStep.Decoration.SURFACE_STRUCTURES ? 2000 : 500;
        StructureSpacing spacing = StructureSpacing.of(generator);
        for (StructurePiece piece : start.getPieces()) {
            String other = spacing.owner(piece.getLocatorPosition(), distance, yOffset);
            BoundingBox box = piece.getBoundingBox();
            if (other == null && (box.getXSpan() > 8 || box.getYSpan() > 8 || box.getZSpan() > 8)) {
                for (BlockPos corner : StructureSpacing.corners(box)) {
                    other = spacing.owner(corner, distance, yOffset);
                    if (other != null && !other.equals(name)) break;
                }
            }
            if (other != null && !other.equals(name)) {
                cir.setReturnValue(StructureStart.INVALID_START);
                return;
            }
        }
        for (StructurePiece piece : start.getPieces()) {
            spacing.claim(piece.getLocatorPosition(), distance, name);
            BoundingBox box = piece.getBoundingBox();
            if (box.getXSpan() > 8 || box.getYSpan() > 8 || box.getZSpan() > 8) {
                for (BlockPos corner : StructureSpacing.corners(box)) spacing.claim(corner, distance, name);
            }
        }
    }
}
