package faygolover.flperf.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import faygolover.flperf.FlPerf;
import faygolover.flperf.PerfConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Белый список достижений: id или маска «namespace:path*»; родители нужных достижений сохраняются автоматически.
 * Достижения модов MCreator (пакет net.mcreator.*) остаются все: их код берёт своё достижение по id и сразу его
 * использует — без него падает (Butchery на каждое выданное достижение).
 */
public final class Advancements {
    private Advancements() {}

    private static List<String> mcreator;

    /** Пространства имён с достижениями в jar модов MCreator — правила «ns:*». */
    private static synchronized List<String> mcreatorRules() {
        if (mcreator != null) return mcreator;
        TreeSet<String> ns = new TreeSet<>();
        LoadingModList list = LoadingModList.get();
        if (list != null) for (ModFileInfo info : list.getModFiles()) {
            try {
                var jar = info.getFile().getSecureJar();
                if (jar.getPackages().stream().noneMatch(p -> p.startsWith("net.mcreator."))) continue;
                Path data = jar.getPath("data");
                if (!Files.isDirectory(data)) continue;
                try (Stream<Path> dirs = Files.list(data)) {
                    dirs.filter(d -> Files.isDirectory(d.resolve("advancements")))
                        .forEach(d -> ns.add(d.getFileName().toString().replace("/", "")));
                }
            } catch (Exception ignored) {}
        }
        List<String> out = new ArrayList<>();
        for (String n : ns) out.add(n + ":*");
        mcreator = List.copyOf(out);
        if (!out.isEmpty()) FlPerf.LOG.info("[flperf] Достижения модов MCreator остаются: {}", ns);
        return mcreator;
    }

    public static Map<ResourceLocation, JsonElement> filter(Map<ResourceLocation, JsonElement> all) {
        List<? extends String> rules;
        try {
            rules = PerfConfig.ADV_KEEP.get();
        } catch (IllegalStateException notLoaded) {
            rules = PerfConfig.ADV_KEEP.getDefault();
        }
        List<String> all2 = new ArrayList<>(rules);
        all2.addAll(mcreatorRules());
        rules = all2;
        Map<ResourceLocation, JsonElement> kept = new HashMap<>();
        Deque<ResourceLocation> todo = new ArrayDeque<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : all.entrySet()) {
            if (matches(e.getKey().toString(), rules)) {
                kept.put(e.getKey(), e.getValue());
                todo.add(e.getKey());
            }
        }
        while (!todo.isEmpty()) {
            JsonElement json = all.get(todo.poll());
            if (json instanceof JsonObject o && o.has("parent")) {
                ResourceLocation parent = ResourceLocation.tryParse(o.get("parent").getAsString());
                if (parent != null && all.containsKey(parent) && kept.putIfAbsent(parent, all.get(parent)) == null) todo.add(parent);
            }
        }
        FlPerf.LOG.info("[flperf] Достижения: загружено {} из {} (белый список {})", kept.size(), all.size(), rules);
        return kept;
    }

    private static boolean matches(String id, List<? extends String> rules) {
        for (String r : rules) {
            if (r.endsWith("*") ? id.startsWith(r.substring(0, r.length() - 1)) : id.equals(r)) return true;
        }
        return false;
    }
}
