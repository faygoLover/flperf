package faygolover.flperf;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** Числовые настройки: {@code config/flperf-common.toml}. Включение самих правок — {@link Features}. */
public final class PerfConfig {
    private PerfConfig() {}

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue WARM_MAX_CHUNKS;
    public static final ForgeConfigSpec.DoubleValue WARM_HALF_LIFE_MIN;
    public static final ForgeConfigSpec.DoubleValue WARM_MIN_HEAT;
    public static final ForgeConfigSpec.IntValue WARM_PREWARM_PER_TICK;
    public static final ForgeConfigSpec.IntValue WARM_MIN_FREE_HEAP_PERCENT;

    public static final ForgeConfigSpec.IntValue GUARD_NOTIFY_LEVEL;
    public static final ForgeConfigSpec.IntValue GUARD_MAX_SUSPENDED;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ADV_KEEP;

    public static final ForgeConfigSpec.IntValue STRUCT_TIMEOUT_S;
    public static final ForgeConfigSpec.IntValue STRUCT_MAP_RADIUS;
    public static final ForgeConfigSpec.IntValue STRUCT_LOCATE_RADIUS;
    public static final ForgeConfigSpec.IntValue STRUCT_GLOBAL_RADIUS;
    public static final ForgeConfigSpec.IntValue STRUCT_MIN_DISTANCE;

    public static final ForgeConfigSpec.IntValue ORPHAN_CHECK_SECONDS;
    public static final ForgeConfigSpec.IntValue ORPHAN_CONFIRM;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ORPHAN_EXEMPT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Тёплые чанки (warm_chunks): сервер греет чанки, которые отправляет игрокам, и держит самые тёплые загруженными без тиков.").push("warm_chunks");
        WARM_MAX_CHUNKS = b.comment("Сколько чанков держать (во всех мирах вместе).").defineInRange("max_chunks", 400, 0, 10000);
        WARM_HALF_LIFE_MIN = b.comment("За сколько минут ИГРОВОГО времени (пока на сервере есть игроки) теплота падает вдвое.")
                .defineInRange("half_life_minutes", 120.0, 1.0, 100000.0);
        WARM_MIN_HEAT = b.comment("Минимальная теплота, чтобы чанк удерживался (1 = минута, проведённая рядом одним игроком).")
                .defineInRange("min_heat", 10.0, 0.0, 1.0E9);
        WARM_PREWARM_PER_TICK = b.comment("Сколько тёплых чанков загружать за тик при старте сервера.").defineInRange("prewarm_per_tick", 8, 1, 400);
        WARM_MIN_FREE_HEAP_PERCENT = b.comment("Если свободной памяти меньше стольких процентов — отпускать самые холодные чанки.")
                .defineInRange("min_free_heap_percent", 10, 0, 90);
        b.pop();

        b.comment("Защита от сбоев в тике (tick_guard).").push("tick_guard");
        GUARD_NOTIFY_LEVEL = b.comment("С какого уровня прав (op) игроки видят сообщение о замороженной сущности или блоке.")
                .defineInRange("notify_permission_level", 2, 0, 4);
        GUARD_MAX_SUSPENDED = b.comment("Если заморожено больше стольких объектов — в лог предупреждение (что-то серьёзно сломано).")
                .defineInRange("warn_suspended_over", 100, 1, 100000);
        b.pop();

        b.comment("Достижения по белому списку (advancements).").push("advancements");
        ADV_KEEP = b.comment("Какие достижения загружать: id или маска с * на конце. Остальные выбрасываются.",
                        "rpmedicine:book/* — замки записей книги RP Medicine (уровни навыка, раздел ГМа).",
                        "s5:* — вкладка «Департамент» (S5 Achievements).")
                .defineListAllowEmpty("advancements_keep", List.of("rpmedicine:book/*", "s5:*"), o -> o instanceof String);
        b.pop();

        b.comment("Поиск структур и генерация (structures).").push("structures");
        STRUCT_TIMEOUT_S = b.comment("Предел времени одного поиска структуры, секунды.").defineInRange("search_timeout_seconds", 50, 1, 3600);
        STRUCT_MAP_RADIUS = b.comment("Радиус поиска для карт сокровищ и исследователя, чанки.").defineInRange("map_search_radius", 40, 1, 200);
        STRUCT_LOCATE_RADIUS = b.comment("Радиус /locate, чанки.").defineInRange("locate_search_radius", 110, 1, 500);
        STRUCT_GLOBAL_RADIUS = b.comment("Общий предел радиуса поиска (глаз Эндера, дельфины, моды), чанки.").defineInRange("global_search_radius", 70, 1, 500);
        STRUCT_MIN_DISTANCE = b.comment("Новые структуры не ближе стольких блоков к другим (0 — выключено).").defineInRange("min_structure_distance", 32, 0, 512);
        b.pop();

        b.comment("Брошенные метки чанков (orphan_tickets): метка мода «от сущности», чьей сущности больше нет, снимается.").push("orphan_tickets");
        ORPHAN_CHECK_SECONDS = b.comment("Как часто проверять, секунды.").defineInRange("check_seconds", 600, 5, 3600);
        ORPHAN_CONFIRM = b.comment("Сколько проверок подряд владельца не должно быть, чтобы снять метку.").defineInRange("confirm_checks", 2, 1, 100);
        ORPHAN_EXEMPT = b.comment("Моды, чьи метки не трогать (id мода).")
                .defineListAllowEmpty("exempt_mods", List.of(), o -> o instanceof String);
        b.pop();

        SPEC = b.build();
    }
}
