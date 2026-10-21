package faygolover.flperf;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import faygolover.flperf.feature.TickGuard;
import faygolover.flperf.feature.WarmChunks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

/** /flperf warm [here|clear], /flperf frozen [resume|kill <номер>] — для админов (op 2). */
public final class FlPerfCommands {
    private FlPerfCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("flperf").requires(s -> s.hasPermission(2))
                .then(Commands.literal("warm")
                        .executes(FlPerfCommands::warmList)
                        .then(Commands.literal("here").executes(FlPerfCommands::warmHere))
                        .then(Commands.literal("clear").executes(c -> {
                            WarmChunks.clear(c.getSource().getServer());
                            return ok(c, "Учёт тёплых чанков сброшен, удержание снято.");
                        })))
                .then(Commands.literal("frozen")
                        .executes(FlPerfCommands::frozenList)
                        .then(Commands.literal("resume").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> release(c, false))))
                        .then(Commands.literal("kill").then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(c -> release(c, true))))));
    }

    private static int ok(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int warmList(CommandContext<CommandSourceStack> c) {
        List<String> top = WarmChunks.top(c.getSource().getServer(), 10);
        ok(c, "Тёплых чанков держится: " + WarmChunks.heldCount() + ". Самые тёплые:");
        top.forEach(line -> ok(c, "  " + line));
        return top.size();
    }

    private static int warmHere(CommandContext<CommandSourceStack> c) {
        var src = c.getSource();
        ChunkPos pos = new ChunkPos(net.minecraft.core.BlockPos.containing(src.getPosition()));
        float heat = WarmChunks.heatAt(src.getServer(), src.getLevel().dimension(), pos);
        return ok(c, "Чанк " + pos + ": теплота " + String.format("%.1f", heat) + (WarmChunks.isHeld(src.getLevel().dimension(), pos) ? ", держится" : ", не держится"));
    }

    private static int frozenList(CommandContext<CommandSourceStack> c) {
        List<TickGuard.Frozen> all = TickGuard.list();
        ok(c, all.isEmpty() ? "Замороженного нет." : "Заморожено: " + all.size());
        for (TickGuard.Frozen f : all) ok(c, "  #" + f.id() + " " + f.kind() + " " + f.what() + " — " + f.where());
        return all.size();
    }

    private static int release(CommandContext<CommandSourceStack> c, boolean kill) {
        int id = IntegerArgumentType.getInteger(c, "id");
        TickGuard.Frozen f = TickGuard.release(id, kill);
        if (f == null) return ok(c, "Нет замороженного #" + id);
        return ok(c, (kill ? "Удалено: #" : "Разморожено: #") + id + " " + f.kind() + " " + f.what());
    }
}
