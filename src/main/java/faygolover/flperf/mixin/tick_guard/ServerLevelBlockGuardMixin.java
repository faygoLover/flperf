package faygolover.flperf.mixin.tick_guard;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.TickGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Запланированные и случайные тики блоков и жидкостей: сбой — блок замораживается вместо падения сервера. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelBlockGuardMixin {
    private static final String BS_TICK = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V";
    private static final String BS_RANDOM = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V";
    private static final String FS_TICK = "Lnet/minecraft/world/level/material/FluidState;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V";
    private static final String FS_RANDOM = "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V";

    @WrapOperation(method = "tickBlock", at = @At(value = "INVOKE", target = BS_TICK))
    private void flperf$block(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        flperf$guardBlock(state, level, pos, random, original);
    }

    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE", target = BS_RANDOM))
    private void flperf$blockRandom(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        flperf$guardBlock(state, level, pos, random, original);
    }

    @org.spongepowered.asm.mixin.Unique
    private static void flperf$guardBlock(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        if (TickGuard.isFrozenBlock(level, pos)) return;
        try {
            original.call(state, level, pos, random);
        } catch (Throwable t) {
            TickGuard.blockFailed(level, pos, String.valueOf(state.getBlock()), t);
        }
    }

    @WrapOperation(method = "tickFluid", at = @At(value = "INVOKE", target = FS_TICK))
    private void flperf$fluid(FluidState state, Level level, BlockPos pos, Operation<Void> original) {
        if (TickGuard.isFrozenBlock(level, pos)) return;
        try {
            original.call(state, level, pos);
        } catch (Throwable t) {
            TickGuard.blockFailed(level, pos, String.valueOf(state.getType()), t);
        }
    }

    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE", target = FS_RANDOM))
    private void flperf$fluidRandom(FluidState state, Level level, BlockPos pos, RandomSource random, Operation<Void> original) {
        if (TickGuard.isFrozenBlock(level, pos)) return;
        try {
            original.call(state, level, pos, random);
        } catch (Throwable t) {
            TickGuard.blockFailed(level, pos, String.valueOf(state.getType()), t);
        }
    }
}
