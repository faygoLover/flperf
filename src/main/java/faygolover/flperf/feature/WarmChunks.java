package faygolover.flperf.feature;

import faygolover.flperf.FlPerf;
import faygolover.flperf.PerfConfig;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Тёплые чанки. Раз в минуту, пока на сервере есть игроки: вся теплота умножается на 0.5^(1/half_life_minutes),
 * каждый чанк в зоне прорисовки каждого игрока получает +1. Самые тёплые (до max_chunks, не ниже min_heat) держатся
 * меткой уровня 33 — чанк загружен (FULL), но не тикает. При старте сервера держимые чанки ставятся понемногу за тик.
 */
public final class WarmChunks {
    private WarmChunks() {}

    public static final TicketType<ChunkPos> TICKET = TicketType.create("flperf_warm", Comparator.comparingLong(ChunkPos::toLong));
    private static final String DATA = "flperf_warm_chunks";

    private static final Map<ResourceKey<Level>, LongSet> HELD = new HashMap<>();
    private static final Deque<Object[]> PREWARM = new ArrayDeque<>();
    private static int tick;

    /** одна запись в данных верхнего мира на все измерения */
    public static final class Data extends SavedData {
        final Map<ResourceKey<Level>, Long2FloatOpenHashMap> heat = new HashMap<>();

        static Data load(CompoundTag tag) {
            Data d = new Data();
            ListTag dims = tag.getList("dims", Tag.TAG_COMPOUND);
            for (int i = 0; i < dims.size(); i++) {
                CompoundTag t = dims.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(t.getString("dim"));
                if (id == null) continue;
                long[] pos = t.getLongArray("pos");
                int[] val = t.getIntArray("heat");
                Long2FloatOpenHashMap map = new Long2FloatOpenHashMap();
                for (int k = 0; k < Math.min(pos.length, val.length); k++) map.put(pos[k], Float.intBitsToFloat(val[k]));
                d.heat.put(ResourceKey.create(Registries.DIMENSION, id), map);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag dims = new ListTag();
            heat.forEach((dim, map) -> {
                CompoundTag t = new CompoundTag();
                t.putString("dim", dim.location().toString());
                long[] pos = new long[map.size()];
                int[] val = new int[map.size()];
                int k = 0;
                for (Long2FloatMap.Entry e : map.long2FloatEntrySet()) {
                    pos[k] = e.getLongKey();
                    val[k++] = Float.floatToIntBits(e.getFloatValue());
                }
                t.put("pos", new LongArrayTag(pos));
                t.putIntArray("heat", val);
                dims.add(t);
            });
            tag.put("dims", dims);
            return tag;
        }
    }

    private static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, DATA);
    }

    private static int cfgInt(java.util.function.IntSupplier s, int def) {
        try { return s.getAsInt(); } catch (IllegalStateException e) { return def; }
    }

    private static double cfgDouble(java.util.function.DoubleSupplier s, double def) {
        try { return s.getAsDouble(); } catch (IllegalStateException e) { return def; }
    }

    // ---------------- жизненный цикл ----------------

    public static void onServerStarted(MinecraftServer server) {
        HELD.clear();
        PREWARM.clear();
        tick = 0;
        for (Map.Entry<ResourceKey<Level>, List<Long>> e : hottest(server).entrySet()) {
            for (long pos : e.getValue()) PREWARM.add(new Object[]{e.getKey(), pos});
        }
        if (!PREWARM.isEmpty()) FlPerf.LOG.info("[flperf] Тёплые чанки: прогрев {} чанков", PREWARM.size());
    }

    public static void onServerTick(MinecraftServer server) {
        int perTick = cfgInt(PerfConfig.WARM_PREWARM_PER_TICK::get, 8);
        for (int i = 0; i < perTick && !PREWARM.isEmpty(); i++) {
            Object[] job = PREWARM.poll();
            @SuppressWarnings("unchecked") ResourceKey<Level> dim = (ResourceKey<Level>) job[0];
            hold(server, dim, (long) job[1]);
        }
        if (++tick % 1200 != 0) return;
        if (!server.getPlayerList().getPlayers().isEmpty()) {
            warmUp(server);
            data(server).setDirty();
        }
        apply(server);
    }

    public static void onServerStopping() {
        HELD.clear();
        PREWARM.clear();
    }

    // ---------------- учёт ----------------

    private static void warmUp(MinecraftServer server) {
        Data d = data(server);
        float decay = (float) Math.pow(0.5, 1.0 / cfgDouble(PerfConfig.WARM_HALF_LIFE_MIN::get, 120.0));
        for (Long2FloatOpenHashMap map : d.heat.values()) {
            map.long2FloatEntrySet().removeIf(e -> e.getFloatValue() * decay < 0.05F);
            for (Long2FloatMap.Entry e : map.long2FloatEntrySet()) e.setValue(e.getFloatValue() * decay);
        }
        int view = server.getPlayerList().getViewDistance();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Long2FloatOpenHashMap map = d.heat.computeIfAbsent(p.level().dimension(), k -> new Long2FloatOpenHashMap());
            ChunkPos c = p.chunkPosition();
            for (int x = -view; x <= view; x++) {
                for (int z = -view; z <= view; z++) map.addTo(ChunkPos.asLong(c.x + x, c.z + z), 1.0F);
            }
        }
    }

    /** самые тёплые чанки по всем измерениям: не ниже min_heat, не больше max_chunks */
    private static Map<ResourceKey<Level>, List<Long>> hottest(MinecraftServer server) {
        float min = (float) cfgDouble(PerfConfig.WARM_MIN_HEAT::get, 10.0);
        int max = cfgInt(PerfConfig.WARM_MAX_CHUNKS::get, 400);
        List<Object[]> all = new ArrayList<>();
        data(server).heat.forEach((dim, map) -> {
            for (Long2FloatMap.Entry e : map.long2FloatEntrySet()) if (e.getFloatValue() >= min) all.add(new Object[]{dim, e.getLongKey(), e.getFloatValue()});
        });
        all.sort((a, b) -> Float.compare((float) b[2], (float) a[2]));
        Map<ResourceKey<Level>, List<Long>> out = new HashMap<>();
        for (int i = 0; i < Math.min(max, all.size()); i++) {
            @SuppressWarnings("unchecked") ResourceKey<Level> dim = (ResourceKey<Level>) all.get(i)[0];
            out.computeIfAbsent(dim, k -> new ArrayList<>()).add((long) all.get(i)[1]);
        }
        return out;
    }

    /** привести держимые чанки к списку самых тёплых; при нехватке памяти — держать вдвое меньше */
    private static void apply(MinecraftServer server) {
        Map<ResourceKey<Level>, List<Long>> want = hottest(server);
        Runtime rt = Runtime.getRuntime();
        long free = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        if (free * 100 / rt.maxMemory() < cfgInt(PerfConfig.WARM_MIN_FREE_HEAP_PERCENT::get, 10)) {
            want.replaceAll((dim, list) -> list.subList(0, list.size() / 2));
            FlPerf.LOG.warn("[flperf] Тёплые чанки: мало свободной памяти — держим вдвое меньше");
        }
        for (Map.Entry<ResourceKey<Level>, LongSet> e : new HashMap<>(HELD).entrySet()) {
            List<Long> keep = want.getOrDefault(e.getKey(), List.of());
            LongSet keepSet = new LongOpenHashSet(keep);
            for (long pos : new LongOpenHashSet(e.getValue())) if (!keepSet.contains(pos)) release(server, e.getKey(), pos);
        }
        want.forEach((dim, list) -> list.forEach(pos -> hold(server, dim, pos)));
    }

    private static void hold(MinecraftServer server, ResourceKey<Level> dim, long pos) {
        ServerLevel level = server.getLevel(dim);
        if (level == null) return;
        if (!HELD.computeIfAbsent(dim, k -> new LongOpenHashSet()).add(pos)) return;
        ChunkPos cp = new ChunkPos(pos);
        level.getChunkSource().addRegionTicket(TICKET, cp, 0, cp);
    }

    private static void release(MinecraftServer server, ResourceKey<Level> dim, long pos) {
        ServerLevel level = server.getLevel(dim);
        LongSet set = HELD.get(dim);
        if (set == null || !set.remove(pos) || level == null) return;
        ChunkPos cp = new ChunkPos(pos);
        level.getChunkSource().removeRegionTicket(TICKET, cp, 0, cp);
    }

    // ---------------- для команд ----------------

    public static int heldCount() {
        return HELD.values().stream().mapToInt(LongSet::size).sum();
    }

    public static float heatAt(MinecraftServer server, ResourceKey<Level> dim, ChunkPos pos) {
        Long2FloatOpenHashMap map = data(server).heat.get(dim);
        return map == null ? 0 : map.get(pos.toLong());
    }

    public static boolean isHeld(ResourceKey<Level> dim, ChunkPos pos) {
        LongSet set = HELD.get(dim);
        return set != null && set.contains(pos.toLong());
    }

    public static List<String> top(MinecraftServer server, int n) {
        List<String> out = new ArrayList<>();
        hottest(server).forEach((dim, list) -> {
            for (int i = 0; i < Math.min(n, list.size()); i++) {
                ChunkPos cp = new ChunkPos(list.get(i));
                out.add(dim.location() + " " + cp.getMiddleBlockX() + " " + cp.getMiddleBlockZ() + " — " + String.format("%.1f", heatAt(server, dim, cp)));
            }
        });
        return out;
    }

    public static void clear(MinecraftServer server) {
        for (Map.Entry<ResourceKey<Level>, LongSet> e : new HashMap<>(HELD).entrySet()) {
            for (long pos : new LongOpenHashSet(e.getValue())) release(server, e.getKey(), pos);
        }
        data(server).heat.clear();
        data(server).setDirty();
    }
}
