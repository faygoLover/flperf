package faygolover.flperf.mixin.async_save;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.AsyncSave;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;

/** level.dat: запись во временный файл и замена — одной задачей в фоне, в том же порядке, что и ваниль. */
@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class LevelStorageAccessMixin {
    private static final String SAVE = "saveDataTag(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/storage/WorldData;Lnet/minecraft/nbt/CompoundTag;)V";

    @WrapOperation(method = SAVE, at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V"))
    private void flperf$deferWrite(CompoundTag tag, File temp, Operation<Void> original) {
        AsyncSave.deferLevelDat(tag, temp);
    }

    @WrapOperation(method = SAVE, at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;safeReplaceFile(Ljava/io/File;Ljava/io/File;Ljava/io/File;)V"))
    private void flperf$replaceLater(File target, File temp, File backup, Operation<Void> original) {
        AsyncSave.submitLevelDat(() -> original.call(target, temp, backup));
    }
}
