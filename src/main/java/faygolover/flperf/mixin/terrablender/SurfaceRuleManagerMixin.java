package faygolover.flperf.mixin.terrablender;

import com.google.common.collect.ImmutableList;
import faygolover.flperf.FlPerf;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import terrablender.api.SurfaceRuleManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TerraBlender: правила модов складываются в одну последовательность (а не дерево «по пространствам имён» с проверкой
 * на каждом шаге), порядок регистрации сохраняется (по образцу TerraBlenderFix).
 */
@Mixin(value = SurfaceRuleManager.class, remap = false)
public abstract class SurfaceRuleManagerMixin {
    @Shadow private static Map<SurfaceRuleManager.RuleCategory, Map<String, SurfaceRules.RuleSource>> surfaceRules;
    @Shadow private static Map<SurfaceRuleManager.RuleCategory, SurfaceRules.RuleSource> defaultSurfaceRules;
    @Shadow private static Map<SurfaceRuleManager.RuleCategory, Map<SurfaceRuleManager.RuleStage, List<com.mojang.datafixers.util.Pair<Integer, SurfaceRules.RuleSource>>>> defaultSurfaceRuleInjections;

    /**
     * @author faygoLover (по TerraBlenderFix)
     * @reason одна плоская последовательность вместо проверки пространства имён на каждом блоке
     */
    @Overwrite
    public static SurfaceRules.RuleSource getNamespacedRules(SurfaceRuleManager.RuleCategory category, SurfaceRules.RuleSource datapackRules) {
        ImmutableList.Builder<SurfaceRules.RuleSource> b = ImmutableList.builder();
        b.addAll(surfaceRules.get(category).values());
        b.add(datapackRules);
        FlPerf.LOG.info("[flperf] Правила поверхности TerraBlender ({}) собраны в одну последовательность", category);
        return SurfaceRules.sequence(b.build().toArray(SurfaceRules.RuleSource[]::new));
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void flperf$orderedMaps(CallbackInfo ci) {
        surfaceRules = new LinkedHashMap<>(surfaceRules);
        defaultSurfaceRules = new LinkedHashMap<>(defaultSurfaceRules);
        defaultSurfaceRuleInjections = new LinkedHashMap<>(defaultSurfaceRuleInjections);
    }
}
