package faygolover.flperf.mixin.advancements;

import com.google.gson.JsonElement;
import faygolover.flperf.feature.Advancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

/** Из всех достижений сборки (~6300) загружаются только из белого списка и их родители. */
@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin {
    @ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"), argsOnly = true)
    private Map<ResourceLocation, JsonElement> flperf$whitelist(Map<ResourceLocation, JsonElement> all) {
        return Advancements.filter(all);
    }
}
