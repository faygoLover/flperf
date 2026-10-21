package faygolover.flperf.mixin.async_save;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.feature.AsyncSave;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;

/** Данные мира и модов (SavedData): тег собирается на основном потоке как обычно, на диск пишется в фоне. */
@Mixin(SavedData.class)
public abstract class SavedDataMixin {
    @WrapOperation(method = "save(Ljava/io/File;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V"))
    private void flperf$writeLater(CompoundTag tag, File file, Operation<Void> original) {
        AsyncSave.writeAtomically(tag, file);
    }
}
