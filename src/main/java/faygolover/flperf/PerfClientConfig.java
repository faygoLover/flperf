package faygolover.flperf;

import net.minecraftforge.common.ForgeConfigSpec;

/** Клиентские настройки: {@code config/flperf-client.toml}. */
public final class PerfClientConfig {
    private PerfClientConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue VERTICAL_SCALE;
    public static final ForgeConfigSpec.DoubleValue SIDE_SCALE;
    public static final ForgeConfigSpec.BooleanValue AFFECT_ENTITIES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Дальность прорисовки эллипсом (distance). 1.0 — как обычно; 2.0 по вертикали — секции выше и ниже видно",
                "на половину дальности; 1.1 по бокам — слева и справа на 10% меньше.").push("distance");
        VERTICAL_SCALE = b.defineInRange("vertical_scale", 2.0, 0.5, 10.0);
        SIDE_SCALE = b.defineInRange("side_scale", 1.1, 0.5, 3.0);
        AFFECT_ENTITIES = b.comment("То же для сущностей.").define("affect_entities", true);
        b.pop();
        SPEC = b.build();
    }
}
