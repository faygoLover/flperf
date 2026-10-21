package faygolover.flperf.mixin.orphan_tickets;

import net.minecraftforge.common.world.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Чей мод и чей владелец у метки Forge (поля закрыты). */
@Mixin(value = ForgeChunkManager.TicketOwner.class, remap = false)
public interface TicketOwnerAccessor {
    @Accessor("modId")
    String flperf$modId();

    @Accessor("owner")
    Comparable<?> flperf$owner();
}
