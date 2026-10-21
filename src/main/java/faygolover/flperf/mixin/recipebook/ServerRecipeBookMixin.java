package faygolover.flperf.mixin.recipebook;

import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Книга рецептов выключена: сервер не шлёт игроку открытые рецепты (ни при входе, ни при открытии новых). */
@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {
    @Inject(method = "sendInitialRecipeBook", at = @At("HEAD"), cancellable = true)
    private void flperf$noInitial(ServerPlayer player, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "sendRecipes", at = @At("HEAD"), cancellable = true)
    private void flperf$noUpdates(ClientboundRecipePacket.State state, ServerPlayer player, List<ResourceLocation> recipes, CallbackInfo ci) {
        ci.cancel();
    }
}
