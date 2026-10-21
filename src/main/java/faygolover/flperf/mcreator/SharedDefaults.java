package faygolover.flperf.mcreator;

/** Один объект «переменные игрока по умолчанию» на класс; вызывается из кода модов MCreator после трансформации. */
public final class SharedDefaults {
    private SharedDefaults() {}

    private static final ClassValue<Object> DEFAULTS = new ClassValue<>() {
        @Override
        protected Object computeValue(Class<?> type) {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("flperf: не создать " + type.getName(), e);
            }
        }
    };

    public static Object of(Class<?> type) {
        return DEFAULTS.get(type);
    }
}
