package faygolover.flperf.feature;

import faygolover.flperf.FlPerf;
import faygolover.flperf.PerfConfig;
import faygolover.flperf.mixin.orphan_tickets.TicketOwnerAccessor;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Брошенные метки чанков. Мод может держать чанк меткой Forge «от сущности» (снаряд, техника); метка сохраняется в
 * {@code data/chunks.dat} и при каждом старте ставится заново. Если сущность пропала, а мод метку не снял (Superb
 * Warfare старых версий оставил 128 таких меток), чанки висят загруженными вечно. Раз в {@code check_seconds}:
 * если все чанки метки загружены вместе с сущностями, а владельца нет ни в одном мире — это промах; после
 * {@code confirm_checks} промахов подряд метка снимается. Ванильный /forceload и метки «от блока» не трогаются.
 */
public final class OrphanTickets {
    private OrphanTickets() {}

    /** владелец (мод + UUID) → сколько проверок подряд его нет */
    private static final Map<String, Integer> MISSES = new HashMap<>();
    private static int tick;

    public static void onServerStarted(MinecraftServer server) {
        MISSES.clear();
        tick = 0;
        for (String line : report(server, 0)) FlPerf.LOG.info("[flperf] Метки чанков: {}", line);
    }

    public static void onServerTick(MinecraftServer server) {
        int period = Math.max(1, cfg(() -> PerfConfig.ORPHAN_CHECK_SECONDS.get(), 60)) * 20;
        if (++tick % period != 0) return;
        int confirm = Math.max(1, cfg(() -> PerfConfig.ORPHAN_CONFIRM.get(), 2));
        List<? extends String> exempt = cfgList();
        Map<String, Integer> seen = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            ForcedChunksSavedData data = level.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
            if (data == null) continue;
            ForgeChunkManager.TicketTracker<UUID> tracker = data.getEntityForcedChunks();
            if (tracker.isEmpty()) continue;
            for (Owner o : owners(tracker).values()) {
                if (exempt.contains(o.mod)) continue;
                if (!allLoaded(level, o)) continue;               // сущности ещё не загрузились — не судим
                if (present(server, o.uuid)) continue;
                String key = o.mod + "/" + o.uuid;
                int misses = MISSES.getOrDefault(key, 0) + 1;
                if (misses < confirm) { seen.put(key, misses); continue; }
                release(level, o);
            }
        }
        MISSES.clear();
        MISSES.putAll(seen);
    }

    private static void release(ServerLevel level, Owner o) {
        List<String> where = new ArrayList<>();
        for (long c : o.chunks) {
            ChunkPos p = new ChunkPos(c);
            ForgeChunkManager.forceChunk(level, o.mod, o.uuid, p.x, p.z, false, false);
            where.add(p.x + "," + p.z);
        }
        for (long c : o.ticking) {
            ChunkPos p = new ChunkPos(c);
            ForgeChunkManager.forceChunk(level, o.mod, o.uuid, p.x, p.z, false, true);
            where.add(p.x + "," + p.z + "(тик)");
        }
        FlPerf.LOG.info("[flperf] Метки чанков: снята брошенная метка мода {} в {} — владельца {} нет; чанки {}",
                o.mod, level.dimension().location(), o.uuid, where);
    }

    /** все чанки владельца загружены вместе с сущностями */
    private static boolean allLoaded(ServerLevel level, Owner o) {
        for (long c : o.chunks) if (!level.areEntitiesLoaded(c)) return false;
        for (long c : o.ticking) if (!level.areEntitiesLoaded(c)) return false;
        return true;
    }

    private static boolean present(MinecraftServer server, UUID uuid) {
        for (ServerLevel l : server.getAllLevels()) if (l.getEntity(uuid) != null) return true;
        return false;
    }

    private static final class Owner {
        final String mod;
        final UUID uuid;
        final LongSet chunks = new LongOpenHashSet();
        final LongSet ticking = new LongOpenHashSet();
        Owner(String mod, UUID uuid) { this.mod = mod; this.uuid = uuid; }
    }

    private static Map<String, Owner> owners(ForgeChunkManager.TicketTracker<UUID> tracker) {
        Map<String, Owner> out = new HashMap<>();
        collect(tracker.getChunks(), out, false);
        collect(tracker.getTickingChunks(), out, true);
        return out;
    }

    private static void collect(Map<ForgeChunkManager.TicketOwner<UUID>, LongSet> map, Map<String, Owner> out, boolean ticking) {
        for (Map.Entry<ForgeChunkManager.TicketOwner<UUID>, LongSet> e : map.entrySet()) {
            TicketOwnerAccessor a = (TicketOwnerAccessor) (Object) e.getKey();
            UUID uuid = (UUID) a.flperf$owner();
            Owner o = out.computeIfAbsent(a.flperf$modId() + "/" + uuid, k -> new Owner(a.flperf$modId(), uuid));
            (ticking ? o.ticking : o.chunks).addAll(e.getValue());
        }
    }

    // ---------------- отчёт для лога и /flperf tickets ----------------

    /** по каждому миру: сколько чанков держит /forceload и метки модов (от блоков и от сущностей); limit > 0 — с координатами */
    public static List<String> report(MinecraftServer server, int limit) {
        List<String> lines = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            ForcedChunksSavedData data = level.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
            if (data == null) continue;
            String dim = level.dimension().location().toString();
            LongSet vanilla = data.getChunks();
            if (!vanilla.isEmpty()) lines.add(dim + ": /forceload — " + vanilla.size() + " чанк(ов)" + coords(vanilla, limit));
            Map<String, LongSet> byMod = new TreeMap<>();
            Map<String, Integer> owners = new TreeMap<>();
            for (Owner o : owners(data.getEntityForcedChunks()).values()) {
                String k = o.mod + " (от сущностей)";
                LongSet s = byMod.computeIfAbsent(k, x -> new LongOpenHashSet());
                s.addAll(o.chunks); s.addAll(o.ticking);
                owners.merge(k, 1, Integer::sum);
            }
            blockTickets(data.getBlockForcedChunks().getChunks(), byMod, owners);
            blockTickets(data.getBlockForcedChunks().getTickingChunks(), byMod, owners);
            byMod.forEach((k, s) -> lines.add(dim + ": " + k + " — " + s.size() + " чанк(ов), меток " + owners.get(k) + coords(s, limit)));
        }
        if (lines.isEmpty()) lines.add("принудительно загруженных чанков нет");
        return lines;
    }

    private static void blockTickets(Map<? extends ForgeChunkManager.TicketOwner<?>, LongSet> map, Map<String, LongSet> byMod, Map<String, Integer> owners) {
        for (Map.Entry<? extends ForgeChunkManager.TicketOwner<?>, LongSet> e : map.entrySet()) {
            String k = ((TicketOwnerAccessor) (Object) e.getKey()).flperf$modId() + " (от блоков)";
            byMod.computeIfAbsent(k, x -> new LongOpenHashSet()).addAll(e.getValue());
            owners.merge(k, 1, Integer::sum);
        }
    }

    private static String coords(LongSet chunks, int limit) {
        if (limit <= 0) return "";
        StringBuilder sb = new StringBuilder(":");
        int n = 0;
        for (long c : chunks) {
            if (n++ == limit) { sb.append(" …"); break; }
            ChunkPos p = new ChunkPos(c);
            sb.append(" [").append(p.getMinBlockX()).append(' ').append(p.getMinBlockZ()).append(']');
        }
        return sb.toString();
    }

    private static int cfg(java.util.function.IntSupplier s, int def) {
        try { return s.getAsInt(); } catch (IllegalStateException e) { return def; }
    }

    private static List<? extends String> cfgList() {
        try { return PerfConfig.ORPHAN_EXEMPT.get(); } catch (IllegalStateException e) { return List.of(); }
    }
}
