package faygolover.flperf.mixin.tick_guard;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import faygolover.flperf.FlPerf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Тик предмета в инвентаре: сбой пишется в лог (раз на тип предмета), предмет просто не тикает в этот раз. */
@Mixin(Inventory.class)
public abstract class InventoryGuardMixin {
    @Unique private static final Set<Item> flperf$reported = ConcurrentHashMap.newKeySet();

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;onInventoryTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;II)V", remap = false))
    private void flperf$guard(ItemStack stack, Level level, Player player, int slot, int selected, Operation<Void> original) {
        try {
            original.call(stack, level, player, slot, selected);
        } catch (Throwable t) {
            if (flperf$reported.add(stack.getItem())) FlPerf.LOG.error("[flperf] Сбой в тике предмета {} у {} — пропущен", stack.getItem(), player.getName().getString(), t);
        }
    }
}
