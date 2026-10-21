package faygolover.flperf.mixin.terrablender;

import faygolover.flperf.feature.SurfaceBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.function.Predicate;

/** Условие «биом X»: если X нет рядом — сразу «ложь»; если рядом только X — сразу «истина». Ветки потом выкидываются. */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$BiomeConditionSource")
public abstract class BiomeConditionSourceMixin {
    @Shadow @Final Predicate<ResourceKey<Biome>> biomeNameTest;

    @Inject(method = "apply(Lnet/minecraft/world/level/levelgen/SurfaceRules$Context;)Lnet/minecraft/world/level/levelgen/SurfaceRules$Condition;",
            at = @At("HEAD"), cancellable = true)
    private void flperf$constant(SurfaceRules.Context context, CallbackInfoReturnable<SurfaceRules.Condition> cir) {
        if (!((Object) context instanceof SurfaceBiomes.Holder holder)) return;
        Set<ResourceKey<Biome>> near = holder.flperf$surroundingBiomes();
        if (near == null) return;
        boolean any = false;
        for (ResourceKey<Biome> b : near) {
            if (biomeNameTest.test(b)) { any = true; break; }
        }
        if (!any) cir.setReturnValue(SurfaceBiomes.Constant.FALSE);
        else if (near.size() == 1) cir.setReturnValue(SurfaceBiomes.Constant.TRUE);
    }
}
