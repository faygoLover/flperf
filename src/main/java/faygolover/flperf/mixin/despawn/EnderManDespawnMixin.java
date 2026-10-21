package faygolover.flperf.mixin.despawn;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Эндермен с блоком в руках может исчезнуть (блок выпадет — см. MobDespawnMixin), а не живёт вечно. */
@Mixin(EnderMan.class)
public abstract class EnderManDespawnMixin {
    @ModifyReturnValue(method = "requiresCustomPersistence", at = @At("RETURN"))
    private boolean flperf$blockIsNotPersistence(boolean keep) {
        return ((EnderMan) (Object) this).isPassenger();
    }
}
