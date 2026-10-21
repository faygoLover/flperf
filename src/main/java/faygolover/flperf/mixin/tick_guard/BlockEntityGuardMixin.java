package faygolover.flperf.mixin.tick_guard;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.TickGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Тик блок-сущности (машины, мебель модов...): замороженная не тикает; исключение — заморозка вместо падения. */
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BlockEntityGuardMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void flperf$guard(BlockEntityTicker<BlockEntity> ticker, Level level, BlockPos pos, BlockState state, BlockEntity be, Operation<Void> original) {
        if (TickGuard.isFrozen(be)) return;
        try {
            original.call(ticker, level, pos, state, be);
        } catch (Throwable t) {
            TickGuard.blockEntityFailed(be, t);
        }
    }
}
