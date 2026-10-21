package faygolover.flperf.mixin.advancements;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Скины робота Mekanism «за достижение» (Allay, MekaSuit) открыты всем: их достижения выброшены белым списком,
 * и без правки скины были бы закрыты навсегда. Без Mekanism миксин не применяется.
 */
@Pseudo
@Mixin(targets = "mekanism.api.robit.AdvancementBasedRobitSkin", remap = false)
public abstract class RobitSkinMixin {
    @Inject(method = "isUnlocked", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void flperf$unlocked(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
