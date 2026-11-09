package faygolover.flperf.mixin.structures;

import faygolover.flperf.feature.StructureSpacing;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/** {@code /place structure} ставит структуру там, где сказал ГМ: расстояние до соседей проверяется только при генерации. */
@Mixin(PlaceCommand.class)
public abstract class PlaceCommandMixin {
    @Redirect(method = "placeStructure", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/Structure;generate(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/biome/BiomeSource;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;JLnet/minecraft/world/level/ChunkPos;ILnet/minecraft/world/level/LevelHeightAccessor;Ljava/util/function/Predicate;)Lnet/minecraft/world/level/levelgen/structure/StructureStart;"))
    private static StructureStart flperf$anywhere(Structure structure, RegistryAccess access, ChunkGenerator generator, BiomeSource biomes, RandomState random,
                                                  StructureTemplateManager templates, long seed, ChunkPos chunk, int references,
                                                  LevelHeightAccessor height, Predicate<Holder<Biome>> validBiome) {
        StructureSpacing.IGNORE.set(true);
        try {
            return structure.generate(access, generator, biomes, random, templates, seed, chunk, references, height, validBiome);
        } finally {
            StructureSpacing.IGNORE.remove();
        }
    }
}
