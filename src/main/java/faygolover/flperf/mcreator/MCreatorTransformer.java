package faygolover.flperf.mcreator;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Код, который генерирует MCreator, при каждом обращении к переменным игрока делает
 * {@code cap.orElse(new XxxModVariables.PlayerVariables())} — новый объект на каждый вызов, много раз за тик.
 * Трансформер меняет {@code new PlayerVariables()} на общий объект ({@link SharedDefaults}), как MCreator Mem Fix.
 * Моды MCreator ищутся при каждом запуске по классу {@code <пакет>.network.<Имя>ModVariables$PlayerVariables}.
 * <p>
 * Второе (функция mcreator_advancements): процедуры MCreator, которые только выдают свои достижения (их выбрасывает
 * белый список flperf), — подписчики событий в списке {@link #MUTED} сразу возвращаются. Так у SCPO и Butchery нет
 * проверок инвентаря каждый тик и поиска достижения на каждом клике/крафте/убийстве. Список проверен по коду: в этих
 * процедурах нет ничего, кроме выдачи достижения (и монет Butchery за два достижения — награды не нужны).
 */
public final class MCreatorTransformer implements ILaunchPluginService {
    private static final Logger LOG = LoggerFactory.getLogger("flperf");
    private static final EnumSet<Phase> YES = EnumSet.of(Phase.BEFORE);
    private static final EnumSet<Phase> NO = EnumSet.noneOf(Phase.class);
    private static final String DEFAULTS = "faygolover/flperf/mcreator/SharedDefaults";

    /** [корень пакета мода с «/» на конце, внутреннее имя класса PlayerVariables] */
    private final List<String[]> profiles;

    /** Процедуры MCreator, которые только выдают достижения (внутренние имена). */
    static final Set<String> MUTED = Set.of(
            "net/mcreator/butchery/procedures/BreathlessadvancementaddedProcedure",
            "net/mcreator/butchery/procedures/ItsafloateradvancementgrantedProcedure",
            "net/mcreator/butchery/procedures/FirstcarcassadvancementProcedure",
            "net/mcreator/butchery/procedures/GrantbloodandgearsProcedure",
            "net/mcreator/butchery/procedures/GrantbloodbankadvancementProcedure",
            "net/mcreator/butchery/procedures/GrantbutcheradvancementProcedure",
            "net/mcreator/butchery/procedures/GrantcutabovetherestadvancementProcedure",
            "net/mcreator/butchery/procedures/GranteverypartcountsadvancementProcedure",
            "net/mcreator/butchery/procedures/GrantfirstbloodadvancementProcedure",
            "net/mcreator/butchery/procedures/GrantHangingaroundadvancementProcedure",
            "net/mcreator/butchery/procedures/GrantheadsupProcedure",
            "net/mcreator/butchery/procedures/GrantknifetomeatyouProcedure",
            "net/mcreator/butchery/procedures/GrantleatherlatethanneveradvancementProcedure",
            "net/mcreator/butchery/procedures/GrantPlayertickadvancementsProcedure",
            "net/mcreator/butchery/procedures/GrantrackemupadvancementProcedure",
            "net/mcreator/butchery/procedures/GrantrotisserierookieProcedure",
            "net/mcreator/butchery/procedures/GrantseasontoperfectionadvancementProcedure",
            "net/mcreator/butchery/procedures/GrantskininthegameadvancementProcedure",
            "net/mcreator/butchery/procedures/GrantsteptowardsleatheradvancementProcedure",
            "net/mcreator/butchery/procedures/GrantthetrophymakerProcedure",
            "net/scpo/procedures/Attack966Procedure",
            "net/scpo/procedures/CleanSecretionProcedure",
            "net/scpo/procedures/Effected939Procedure",
            "net/scpo/procedures/GiveAdvancementsItemsProcedure",
            "net/scpo/procedures/Kill939Procedure",
            "net/scpo/procedures/OvertimeStartUpProcedure",
            "net/scpo/procedures/Recontain055RightClickProcedure",
            "net/scpo/procedures/SCP1548AchievementGiverProcedure");
    private static final String SUBSCRIBE = "Lnet/minecraftforge/eventbus/api/SubscribeEvent;";

    private final boolean mute;

    private MCreatorTransformer(List<String[]> profiles, boolean mute) { this.profiles = profiles; this.mute = mute; }

    public static void install(boolean variables, boolean mute) throws Throwable {
        List<String[]> profiles = variables ? findProfiles() : List.of();
        if (profiles.isEmpty() && !mute) return;
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        MethodHandles.Lookup lookup = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(implLookup), unsafe.staticFieldOffset(implLookup));
        LaunchPluginHandler handler = (LaunchPluginHandler) lookup.findVarHandle(Launcher.class, "launchPlugins", LaunchPluginHandler.class).get(Launcher.INSTANCE);
        @SuppressWarnings("unchecked")
        Map<String, ILaunchPluginService> plugins = (Map<String, ILaunchPluginService>) lookup.findVarHandle(LaunchPluginHandler.class, "plugins", Map.class).get(handler);
        MCreatorTransformer t = new MCreatorTransformer(profiles, mute);
        plugins.put(t.name(), t);
        LOG.info("[flperf] MCreator: {} мод(ов): {}", profiles.size(), profiles.stream().map(p -> p[1].substring(p[1].lastIndexOf('/') + 1)).toList());
        if (mute) LOG.info("[flperf] MCreator: процедуры, которые только выдают достижения, отключены (до {} шт.)", MUTED.size());
    }

    private static List<String[]> findProfiles() {
        List<String[]> out = new ArrayList<>();
        LoadingModList list = LoadingModList.get();
        if (list == null) return out;
        for (ModFileInfo info : list.getModFiles()) {
            SecureJar jar = info.getFile().getSecureJar();
            for (String pkg : jar.getPackages()) {
                if (!pkg.endsWith(".network")) continue;
                Path dir = jar.getPath(pkg.replace('.', '/'));
                if (!Files.isDirectory(dir)) continue;
                try (Stream<Path> files = Files.list(dir)) {
                    files.map(p -> p.getFileName().toString())
                         .filter(n -> n.endsWith("ModVariables$PlayerVariables.class"))
                         .forEach(n -> {
                             String root = pkg.substring(0, pkg.length() - ".network".length()).replace('.', '/') + "/";
                             out.add(new String[]{root, pkg.replace('.', '/') + "/" + n.substring(0, n.length() - ".class".length())});
                         });
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    @Override public String name() { return "flperf_mcreator"; }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        if (isEmpty) return NO;
        String name = classType.getInternalName();
        if (mute && MUTED.contains(name)) return YES;
        for (String[] p : profiles) if (name.startsWith(p[0])) return YES;
        return NO;
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        boolean modified = false;
        if (mute && MUTED.contains(classNode.name)) {
            for (MethodNode m : classNode.methods) {
                if (!m.desc.endsWith(")V") || m.visibleAnnotations == null) continue;
                boolean subscriber = false;
                for (AnnotationNode a : m.visibleAnnotations) if (SUBSCRIBE.equals(a.desc)) subscriber = true;
                if (!subscriber) continue;
                m.instructions.insert(new InsnNode(Opcodes.RETURN));   // подписчик сразу возвращается
                modified = true;
                LOG.debug("[flperf] MCreator: отключён {}.{}", classNode.name, m.name);
            }
            if (modified) LOG.info("[flperf] MCreator: отключена выдача достижений: {}", classNode.name.substring(classNode.name.lastIndexOf('/') + 1));
        }
        if (profiles.isEmpty()) return modified;
        for (MethodNode m : classNode.methods) {
            InsnList insns = m.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() != Opcodes.NEW || !(insn instanceof TypeInsnNode ti) || !isPlayerVariables(ti.desc)) continue;
                AbstractInsnNode dup = next(insn);
                AbstractInsnNode init = dup == null ? null : next(dup);
                AbstractInsnNode call = init == null ? null : next(init);
                if (dup == null || dup.getOpcode() != Opcodes.DUP) continue;
                if (!(init instanceof MethodInsnNode ctor) || !ctor.name.equals("<init>") || !ctor.desc.equals("()V") || !ctor.owner.equals(ti.desc)) continue;
                if (!(call instanceof MethodInsnNode orElse) || !orElse.name.equals("orElse") || !orElse.desc.equals("(Ljava/lang/Object;)Ljava/lang/Object;")) continue;
                LdcInsnNode cls = new LdcInsnNode(Type.getObjectType(ti.desc));
                insns.insertBefore(insn, cls);
                insns.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, DEFAULTS, "of", "(Ljava/lang/Class;)Ljava/lang/Object;", false));
                insns.remove(dup);
                insns.remove(ctor);
                insns.remove(insn);
                insn = cls;
                modified = true;
            }
        }
        return modified;
    }

    private boolean isPlayerVariables(String internal) {
        for (String[] p : profiles) if (p[1].equals(internal)) return true;
        return false;
    }

    /** следующая инструкция кода (без меток, номеров строк и кадров) */
    private static AbstractInsnNode next(AbstractInsnNode insn) {
        AbstractInsnNode n = insn.getNext();
        while (n != null && n.getOpcode() < 0) n = n.getNext();
        return n;
    }
}
