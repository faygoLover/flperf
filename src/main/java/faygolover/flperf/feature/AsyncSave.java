package faygolover.flperf.feature;

import faygolover.flperf.FlPerf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Один фоновый поток записи: задачи идут по порядку. При остановке сервера {@link #flush()} ждёт, пока всё запишется,
 * иначе процесс мог бы выйти раньше (так было у fastasyncworldsave).
 */
public final class AsyncSave {
    private AsyncSave() {}

    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "flperf-save");
        t.setDaemon(true);
        return t;
    });
    private static final ThreadLocal<Object[]> LEVEL_DAT = new ThreadLocal<>();

    /** записать во временный файл рядом и заменить им старый — обрыв посреди записи не портит файл */
    public static void writeAtomically(CompoundTag tag, File file) {
        WRITER.execute(() -> {
            File temp = new File(file.getParentFile(), file.getName() + ".flperf-tmp");
            try {
                NbtIo.writeCompressed(tag, temp);
                try {
                    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception atomicFailed) {
                    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                FlPerf.LOG.error("[flperf] Не записать {}", file, e);
            }
        });
    }

    public static void deferLevelDat(CompoundTag tag, File temp) {
        LEVEL_DAT.set(new Object[]{tag, temp});
    }

    public static void submitLevelDat(Runnable replace) {
        Object[] pending = LEVEL_DAT.get();
        LEVEL_DAT.remove();
        WRITER.execute(() -> {
            try {
                if (pending != null) NbtIo.writeCompressed((CompoundTag) pending[0], (File) pending[1]);
                replace.run();
            } catch (Exception e) {
                FlPerf.LOG.error("[flperf] Не записать level.dat", e);
            }
        });
    }

    /** дождаться записи всего, что поставлено в очередь (не дольше 2 минут) */
    public static void flush() {
        try {
            WRITER.submit(() -> {}).get(2, TimeUnit.MINUTES);
        } catch (Exception e) {
            FlPerf.LOG.error("[flperf] Запись на диск не закончилась за 2 минуты", e);
        }
    }
}
