package faygolover.flperf;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Включение правок: {@code config/flperf-mixins.properties}. Читается до загрузки модов (миксины решаются раньше,
 * чем Forge грузит свои конфиги), поэтому изменения — после перезапуска. Числовые настройки — в flperf-common/client.toml.
 * Здесь нельзя трогать классы Minecraft: файл читает плагин миксинов.
 */
public final class Features {
    private Features() {}

    /** ключ → [по умолчанию, описание] */
    private static final Map<String, String[]> ALL = new LinkedHashMap<>();
    static {
        def("warm_chunks", "Тёплые чанки: где играют, чанки остаются загруженными без тиков и прогреваются при старте сервера");
        def("tick_guard", "Сбой в тике сущности, блок-сущности, блока или предмета не роняет сервер: виновник замораживается, админам — сообщение");
        def("advancements", "Достижения по белому списку (advancements_keep в flperf-common.toml); окно достижений и уведомления выключены; скины робота Mekanism открыты всем");
        def("sync_loads", "Мобы, пчёлы, поиск пути, карты, лучи и сон не грузят чанк синхронно посреди тика");
        def("despawn", "Моб, подобравший предмет, не становится вечным; при исчезновении роняет подобранное (эндермен — блок)");
        def("distance", "Дальность прорисовки эллипсом: выше/ниже и по бокам видно меньше (настройки в flperf-client.toml)");
        def("terrablender", "Быстрая генерация поверхности: правила биомов, которых нет в соседних чанках, отсекаются (нужен TerraBlender)");
        def("safety", "Битая сущность при загрузке не роняет сервер; сущности из чужих потоков ставятся в очередь; ошибки команд в лог; падение чужого обработчика события достижения не выкидывает игрока");
        def("structures", "Поиск структур с пределом радиуса и времени; падения генерации фич ловятся; структуры не ближе min_structure_distance");
        def("recipebook", "Книга рецептов выключена: нет кнопки, сервер не шлёт открытые рецепты");
        def("gpu_leak", "Видеопамять буферов, которые мод забыл удалить, освобождается после сборки мусора");
        def("mcreator", "Моды MCreator не создают новый объект переменных игрока при каждом обращении");
        def("mcreator_advancements", "Процедуры SCPO и Butchery, которые только выдают свои (выброшенные) достижения, отключены: без проверок инвентаря каждый тик (работает вместе с advancements)");
        def("async_save", "level.dat и данные модов пишутся на диск в фоне; при остановке сервера запись дожидается");
        def("background_fps", "Игра не в фокусе: 1 кадр в секунду и звук 25%; мышь над окном — 60 кадров; свёрнута — без кадров и звука (вместо Dynamic FPS)");
        def("orphan_tickets", "Метки чанков модов от сущностей, которых больше нет (снаряды, техника), снимаются — чанки не висят загруженными вечно. /flperf tickets — кто держит чанки");
        def("atlas_pack", "Встроенный ресурспак: огромные текстуры Tobacconist и Superb Warfare уменьшены (атлас блоков вдвое меньше)");
    }

    private static void def(String key, String comment) { ALL.put(key, new String[]{"true", comment}); }

    private static final Properties VALUES = new Properties();
    private static boolean loaded;

    public static synchronized boolean on(String key) {
        if (!loaded) load();
        return Boolean.parseBoolean(VALUES.getProperty(key, "true"));
    }

    private static void load() {
        loaded = true;
        Path file = FMLPaths.CONFIGDIR.get().resolve("flperf-mixins.properties");
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { VALUES.load(r); } catch (IOException ignored) {}
        }
        boolean missing = ALL.keySet().stream().anyMatch(k -> !VALUES.containsKey(k));
        if (!missing) return;
        StringBuilder sb = new StringBuilder("# faygoLover's Performance — включение правок (true/false). Изменения — после перезапуска.\n");
        for (Map.Entry<String, String[]> e : ALL.entrySet()) {
            sb.append("\n# ").append(e.getValue()[1]).append('\n')
              .append(e.getKey()).append('=').append(VALUES.getProperty(e.getKey(), e.getValue()[0])).append('\n');
        }
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) { w.write(sb.toString()); } catch (IOException ignored) {}
    }
}
