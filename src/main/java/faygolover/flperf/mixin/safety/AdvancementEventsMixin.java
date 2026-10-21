package faygolover.flperf.mixin.safety;

import faygolover.flperf.FlPerf;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Достижение уже выдано, событие только сообщает об этом модам. Если чей-то обработчик падает (MCreator-моды берут
 * своё достижение и сразу вызывают у него equals — с выброшенным белым списком достижением это NPE), исключение
 * уходило в выдачу достижения и дальше во вход игрока: сервер выкидывал его с «Invalid player data». Теперь — запись
 * в лог (один раз на место падения), выдача и вход продолжаются.
 */
@Mixin(value = ForgeEventFactory.class, remap = false)
public abstract class AdvancementEventsMixin {
    @Unique private static final Set<String> flperf$reported = ConcurrentHashMap.newKeySet();

    @Redirect(method = {"onAdvancementEarnedEvent", "onAdvancementProgressedEvent"},
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"))
    private static boolean flperf$safePost(IEventBus bus, Event event) {
        try {
            return bus.post(event);
        } catch (RuntimeException e) {
            StackTraceElement[] st = e.getStackTrace();
            String where = st.length > 0 ? st[0].getClassName() + "." + st[0].getMethodName() : "?";
            if (flperf$reported.add(e.getClass().getName() + " " + where))
                FlPerf.LOG.warn("[flperf] Ошибка в обработчике события достижения ({}) — пропущена, игрок не отключается", where, e);
            return false;
        }
    }
}
