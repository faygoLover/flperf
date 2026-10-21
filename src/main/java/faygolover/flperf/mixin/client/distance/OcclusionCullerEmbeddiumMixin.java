package faygolover.flperf.mixin.client.distance;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import faygolover.flperf.client.DistanceState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Embeddium: секция в пределах дальности по его проверке ещё должна попасть в эллипс (выше/ниже и по бокам — короче). */
@Mixin(value = OcclusionCuller.class, remap = false)
public abstract class OcclusionCullerEmbeddiumMixin {
    @ModifyReturnValue(method = "isWithinRenderDistance", at = @At("RETURN"), remap = false)
    private static boolean flperf$ellipse(boolean within, CameraTransform camera, RenderSection section, float maxDistance) {
        if (!within) return false;
        float dx = section.getOriginX() + 8 - camera.intX - camera.fracX;
        float dy = section.getOriginY() + 8 - camera.intY - camera.fracY;
        float dz = section.getOriginZ() + 8 - camera.intZ - camera.fracZ;
        return DistanceState.inside(dx, dy, dz, maxDistance + 8.0F);
    }
}
