package faygolover.flperf.feature;

import faygolover.flperf.FlPerf;
import faygolover.flperf.PerfConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сбой в тике не роняет сервер: виновник «замораживается» (не тикает), в лог — полный стек, админам — сообщение.
 * Игрок не замораживается никогда — у него только пропускается тик со сбоем.
 * Заморозка только в памяти: после перезапуска объект снова тикает (и снова заморозится, если ошибка осталась).
 * По мотивам Neruina.
 */
public final class TickGuard {
    private TickGuard() {}

    /** что заморожено: сущность / блок-сущность (по объекту) или блок (по позиции) */
    public record Frozen(int id, String kind, String what, String where, Object target) {}

    private static final Map<Object, Frozen> BY_TARGET = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Integer, Frozen> BY_ID = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Set<Long> FROZEN_BLOCKS = ConcurrentHashMap.newKeySet();
    private static int nextId = 1;

    public static boolean isFrozen(Object target) {
        return BY_TARGET.containsKey(target);
    }

    public static boolean isFrozenBlock(Level level, BlockPos pos) {
        return !FROZEN_BLOCKS.isEmpty() && FROZEN_BLOCKS.contains(key(level, pos));
    }

    private static long key(Level level, BlockPos pos) {
        return pos.asLong() * 31 + level.dimension().location().hashCode();
    }

    public static void entityFailed(Entity entity, Throwable error) {
        // Игрока не замораживаем никогда: он встал бы как вкопанный, и ни смерть, ни смена режима не помогали.
        // Только пропускаем этот тик и пишем в лог (не чаще раза в 10 с на игрока).
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            playerFailed(player, error);
            return;
        }
        freeze("сущность", entity.getType().builtInRegistryHolder().key().location().toString(), entity.level(), entity.blockPosition(), entity, error);
    }

    private static final Map<java.util.UUID, Long> PLAYER_ERRORS = new ConcurrentHashMap<>();

    private static void playerFailed(net.minecraft.world.entity.player.Player player, Throwable error) {
        long now = System.currentTimeMillis();
        Long last = PLAYER_ERRORS.get(player.getUUID());
        if (last != null && now - last < 10_000L) return;
        PLAYER_ERRORS.put(player.getUUID(), now);
        String name = player.getName().getString();
        FlPerf.LOG.error("[flperf] Сбой в тике игрока {} ({}) — тик пропущен, игрок не заморожен", name,
                player.level().isClientSide ? "клиент" : "сервер", error);
        if (player.level().isClientSide) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int level = cfg(PerfConfig.GUARD_NOTIFY_LEVEL::get, 2);
        Component msg = Component.literal("[flperf] Сбой в тике игрока " + name + " (тик пропущен, игрок не заморожен). Причина: " + error)
                .withStyle(ChatFormatting.RED);
        server.execute(() -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) if (p.hasPermissions(level)) p.sendSystemMessage(msg);
        });
    }

    public static void blockEntityFailed(BlockEntity be, Throwable error) {
        Level level = be.getLevel();
        String type = String.valueOf(net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType()));
        freeze("блок-сущность", type, level, be.getBlockPos(), be, error);
    }

    public static void blockFailed(Level level, BlockPos pos, String what, Throwable error) {
        long k = key(level, pos);
        if (!FROZEN_BLOCKS.add(k)) return;
        freeze("блок", what, level, pos, k, error);
    }

    private static void freeze(String kind, String what, Level level, BlockPos pos, Object target, Throwable error) {
        if (BY_TARGET.containsKey(target)) return;
        String where = (level == null ? "?" : level.dimension().location()) + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        Frozen f;
        synchronized (BY_ID) {
            f = new Frozen(nextId++, kind, what, where, target);
            BY_ID.put(f.id(), f);
        }
        BY_TARGET.put(target, f);
        FlPerf.LOG.error("[flperf] Сбой в тике: {} {} в {} — заморожен (#{}). Сервер продолжает работу.", kind, what, where, f.id(), error);
        if (BY_ID.size() > cfg(PerfConfig.GUARD_MAX_SUSPENDED::get, 100)) FlPerf.LOG.warn("[flperf] Заморожено уже {} объектов — что-то серьёзно сломано", BY_ID.size());
        notifyAdmins(f, error);
    }

    private static void notifyAdmins(Frozen f, Throwable error) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int level = cfg(PerfConfig.GUARD_NOTIFY_LEVEL::get, 2);
        Component msg = Component.literal("[flperf] Сбой: " + f.kind() + " " + f.what() + " (" + f.where() + ") заморожен #" + f.id()
                + ". /flperf frozen — список, resume/kill " + f.id() + ". Причина: " + error).withStyle(ChatFormatting.RED);
        server.execute(() -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) if (p.hasPermissions(level)) p.sendSystemMessage(msg);
        });
    }

    public static List<Frozen> list() {
        synchronized (BY_ID) { return new ArrayList<>(BY_ID.values()); }
    }

    /** разморозить; для сущности kill=true — удалить её из мира */
    public static Frozen release(int id, boolean kill) {
        Frozen f = BY_ID.remove(id);
        if (f == null) return null;
        BY_TARGET.remove(f.target());
        if (f.target() instanceof Long k) FROZEN_BLOCKS.remove(k);
        if (kill && f.target() instanceof Entity e) e.discard();
        if (kill && f.target() instanceof BlockEntity be && be.getLevel() != null) be.getLevel().removeBlock(be.getBlockPos(), false);
        return f;
    }

    private static int cfg(java.util.function.IntSupplier s, int def) {
        try { return s.getAsInt(); } catch (IllegalStateException e) { return def; }
    }
}
