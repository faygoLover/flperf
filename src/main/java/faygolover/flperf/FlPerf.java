package faygolover.flperf;

import com.mojang.logging.LogUtils;
import faygolover.flperf.feature.AsyncSave;
import faygolover.flperf.feature.WarmChunks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/** faygoLover's Performance: правки производительности и стабильности сборки S5. Что включено — {@link Features}. */
@Mod(FlPerf.MODID)
public final class FlPerf {
    public static final String MODID = "flperf";
    public static final Logger LOG = LogUtils.getLogger();

    public FlPerf() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PerfConfig.SPEC, "flperf-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, PerfClientConfig.SPEC, "flperf-client.toml");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> FlPerfCommands.register(e.getDispatcher()));
        if (Features.on("warm_chunks")) {
            MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> WarmChunks.onServerStarted(e.getServer()));
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
                if (e.phase == TickEvent.Phase.END) WarmChunks.onServerTick(e.getServer());
            });
            MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> WarmChunks.onServerStopping());
        }
        if (Features.on("async_save")) MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> AsyncSave.flush());

        if (FMLEnvironment.dist == Dist.CLIENT) faygolover.flperf.client.ClientEvents.init(modBus);
    }
}
