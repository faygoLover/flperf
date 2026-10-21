package faygolover.flperf.mixin.client.recipebook;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Книга рецептов никогда не открыта (даже если в сохранении стоит «открыта»). */
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
    @ModifyReturnValue(method = "isVisible", at = @At("RETURN"))
    private boolean flperf$hidden(boolean visible) {
        return false;
    }
}
