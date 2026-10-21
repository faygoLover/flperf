package faygolover.flperf.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import faygolover.flperf.FlPerf;
import faygolover.flperf.PerfConfig;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Белый список достижений: id или маска «namespace:path*»; родители нужных достижений сохраняются автоматически. */
public final class Advancements {
    private Advancements() {}

    public static Map<ResourceLocation, JsonElement> filter(Map<ResourceLocation, JsonElement> all) {
        List<? extends String> rules;
        try {
            rules = PerfConfig.ADV_KEEP.get();
        } catch (IllegalStateException notLoaded) {
            rules = PerfConfig.ADV_KEEP.getDefault();
        }
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
