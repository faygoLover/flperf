package faygolover.flperf.mixin.despawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Подобранный предмет больше не делает моба вечным (ваниль ставит persistenceRequired). Подобранное помечено шансом
 * выпадения 2.0 (ваниль), стартовая экипировка — 0.085, поэтому при исчезновении роняем ровно то, что моб подобрал.
 * По мотивам Let Me Despawn.
 */
@Mixin(Mob.class)
public abstract class MobDespawnMixin {
    @Shadow private boolean persistenceRequired;
    @Unique private boolean flperf$wasPersistent;

    @Shadow protected abstract float getEquipmentDropChance(EquipmentSlot slot);

    @Inject(method = "setItemSlotAndDropWhenKilled", at = @At("HEAD"))
    private void flperf$remember(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        flperf$wasPersistent = persistenceRequired;
    }

    @Inject(method = "setItemSlotAndDropWhenKilled", at = @At("TAIL"))
    private void flperf$restore(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        persistenceRequired = flperf$wasPersistent || ((Mob) (Object) this).hasCustomName();
    }

    /** перед исчезновением по расстоянию — уронить подобранное (и блок эндермена) */
    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;discard()V"))
    private void flperf$dropPicked(Mob self, Operation<Void> original) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = self.getItemBySlot(slot);
            if (!stack.isEmpty() && getEquipmentDropChance(slot) > 1.0F) {
                self.spawnAtLocation(stack);
                self.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        if (self instanceof EnderMan enderman) {
            BlockState carried = enderman.getCarriedBlock();
            if (carried != null) {
                self.spawnAtLocation(carried.getBlock().asItem());
                enderman.setCarriedBlock(null);
            }
        }
        original.call(self);
    }
}
