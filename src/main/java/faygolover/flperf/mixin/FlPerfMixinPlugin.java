package faygolover.flperf.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import faygolover.flperf.Features;
import faygolover.flperf.mcreator.MCreatorTransformer;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Правка = пакет {@code mixin.<функция>} или {@code mixin.client.<функция>}; применяется, только если функция включена
 * в flperf-mixins.properties и нужный мод есть в сборке. Здесь же запускается MixinExtras и трансформер MCreator.
 */
public final class FlPerfMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOG = LoggerFactory.getLogger("flperf");
    private static final String ROOT = "faygolover.flperf.mixin.";

    @Override
    public void onLoad(String mixinPackage) {
        MixinExtrasBootstrap.init();
        if (Features.on("mcreator") || Features.on("mcreator_advancements")) {
            try {
                MCreatorTransformer.install(Features.on("mcreator"), Features.on("mcreator_advancements") && Features.on("advancements"));
            } catch (Throwable t) {
                LOG.warn("[flperf] MCreator: трансформер не подключён", t);
            }
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(ROOT)) return false;
        String rest = mixinClassName.substring(ROOT.length());
        if (rest.startsWith("client.")) rest = rest.substring("client.".length());
        int dot = rest.indexOf('.');
        if (dot < 0) return true;
        String feature = rest.substring(0, dot);
        if (!Features.on(feature)) return false;
        if (mixinClassName.contains("Embeddium") && !has("embeddium")) return false;
        if (feature.equals("terrablender") && !has("terrablender")) return false;
        if (feature.equals("advancements_hide_ui") && !Features.on("advancements")) return false;
        return true;
    }

    private static boolean has(String modId) {
        LoadingModList list = LoadingModList.get();
        return list != null && list.getModFileById(modId) != null;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
