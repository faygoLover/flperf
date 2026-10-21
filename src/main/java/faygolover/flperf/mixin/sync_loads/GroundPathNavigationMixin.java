package faygolover.flperf.mixin.sync_loads;

import faygolover.flperf.feature.Chunks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Моб не строит путь к точке в незагруженном чанке. */
@Mixin(GroundPathNavigation.class)
public abstract class GroundPathNavigationMixin extends PathNavigation {
    private GroundPathNavigationMixin(Mob mob, Level level) {
        super(mob, level);
    }

    @Inject(method = "createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
    private void flperf$onlyIfLoaded(BlockPos target, int accuracy, CallbackInfoReturnable<Path> cir) {
        if (!Chunks.hasChunk(this.level, target)) cir.setReturnValue(null);
    }
}
