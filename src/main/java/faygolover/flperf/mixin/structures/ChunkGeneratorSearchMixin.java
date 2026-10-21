package faygolover.flperf.mixin.structures;

import faygolover.flperf.FlPerf;
import faygolover.flperf.PerfConfig;
import faygolover.flperf.feature.StructureLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import com.mojang.datafixers.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Поиск ближайшей структуры: радиус не больше global_search_radius, время не больше search_timeout_seconds. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorSearchMixin {
    private static final String FIND = "findNearestMapStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/HolderSet;Lnet/minecraft/core/BlockPos;IZ)Lcom/mojang/datafixers/util/Pair;";

    @ModifyVariable(method = FIND, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int flperf$capRadius(int radius) {
        return Math.min(radius, StructureLimits.radiusCap());
    }

    @Inject(method = FIND, at = @At("HEAD"))
    private void flperf$start(ServerLevel level, HolderSet<Structure> structures, BlockPos pos, int radius, boolean skipKnown,
                              CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        StructureLimits.start(structures);
    }

    @Inject(method = FIND, at = @At("RETURN"))
    private void flperf$end(ServerLevel level, HolderSet<Structure> structures, BlockPos pos, int radius, boolean skipKnown,
                            CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        StructureLimits.end();
    }

    /** оба обхода (кольца и сетка) на каждом шаге спрашивают getStructureGeneratingAt — тут и прерываем по времени */
    @Inject(method = "getStructureGeneratingAt", at = @At("HEAD"), cancellable = true)
    private static void flperf$timeout(CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        if (StructureLimits.timedOut()) cir.setReturnValue(null);
    }
}
