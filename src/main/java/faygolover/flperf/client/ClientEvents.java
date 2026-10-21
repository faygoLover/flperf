package faygolover.flperf.client;

import faygolover.flperf.FlPerf;
import faygolover.flperf.Features;
import faygolover.flperf.mixin.client.recipebook.ImageButtonAccessor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Всё клиентское: дальность, видеопамять, кнопки книги рецептов и достижений, встроенный ресурспак атласа. */
public final class ClientEvents {
    private ClientEvents() {}

    private static final ResourceLocation RECIPE_BUTTON = new ResourceLocation("textures/gui/recipe_button.png");

    public static void init(IEventBus modBus) {
        if (Features.on("atlas_pack")) modBus.addListener(ClientEvents::addPacks);
        if (Features.on("distance") || Features.on("gpu_leak")) MinecraftForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        if (Features.on("recipebook") || Features.on("advancements")) MinecraftForge.EVENT_BUS.addListener(ClientEvents::onScreenInit);
        if (Features.on("advancements")) MinecraftForge.EVENT_BUS.addListener(ClientEvents::onScreenOpen);
    }

    private static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (Features.on("distance")) DistanceState.update();
        if (Features.on("gpu_leak")) GpuLeak.drain();
    }

    /** убрать кнопку книги рецептов (инвентарь, верстак, печи) и кнопку «Достижения» в меню паузы */
    private static void onScreenInit(ScreenEvent.Init.Post e) {
        List<GuiEventListener> remove = new ArrayList<>();
        for (GuiEventListener l : e.getListenersList()) {
            if (Features.on("recipebook") && l instanceof ImageButton ib && RECIPE_BUTTON.equals(((ImageButtonAccessor) ib).flperf$texture())) remove.add(l);
            if (Features.on("advancements") && e.getScreen() instanceof PauseScreen && l instanceof Button b && isKey(b.getMessage(), "gui.advancements")) remove.add(l);
        }
        remove.forEach(e::removeListener);
    }

    private static boolean isKey(Component c, String key) {
        return c.getContents() instanceof TranslatableContents tc && tc.getKey().equals(key);
    }

    /** окно достижений не открывается (клавиша L, моды) */
    private static void onScreenOpen(ScreenEvent.Opening e) {
        if (e.getNewScreen() instanceof AdvancementsScreen) e.setCanceled(true);
    }

    private static void addPacks(AddPackFindersEvent e) {
        if (e.getPackType() != PackType.CLIENT_RESOURCES) return;
        Path root = ModList.get().getModFileById(FlPerf.MODID).getFile().findResource("resourcepacks", "flperf_atlas");
        e.addRepositorySource(add -> {
            Pack pack = Pack.readMetaAndCreate("flperf_atlas", Component.literal("S5: атлас блоков без огромных текстур"), true,
                    id -> new PathPackResources(id, root, true), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) add.accept(pack);
        });
    }
}
