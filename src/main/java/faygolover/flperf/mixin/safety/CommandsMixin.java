package faygolover.flperf.mixin.safety;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Ошибка внутри команды пишется в лог со стеком (ваниль — только в режиме отладки). */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @ModifyExpressionValue(method = "performCommand", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;isDebugEnabled()Z", remap = false))
    private boolean flperf$alwaysLog(boolean debug) {
        return true;
    }
}
