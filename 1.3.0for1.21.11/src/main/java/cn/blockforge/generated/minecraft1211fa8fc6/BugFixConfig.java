package cn.blockforge.generated.minecraft1211fa8fc6;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 模组配置 + 冲突自动避让。
 *
 * <p>配置文件：{@code config/wjys-fix-patch.json}（Gson 为 Minecraft 自带库，无额外依赖）。
 * 每个修复项一个独立布尔开关，默认全部开启；另有一个「冲突自动避让」总开关（默认开启）。</p>
 *
 * <p>优先级：手动显式写下的开关 > 自动避让 > 默认开启。
 * 即：玩家在配置里手动写 {@code "autoAvoidConflicts": false} 或写死某一项的 true/false，
 * 就覆盖对应修复项的自动检测结果（满足「玩家可在配置文件中手动强制开启，覆盖自动检测」的要求）。</p>
 *
 * <p>整个加载过程全程 try/catch：文件不存在则生成默认文件；
 * 解析失败则回退「全部开启 + 自动避让开启」的原生默认，只记日志，绝不抛异常。</p>
 */
public final class BugFixConfig {

    public static final String MOD_ID = "wjys_fix_patch";
    /** 统一日志出口：所有 Mixin 的兜底异常都写到这里，不弹窗、不打断游戏。 */
    public static final Logger LOGGER = LoggerFactory.getLogger("WJYsFixPatch");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 冲突自动避让总开关（关闭后所有修复项强制生效）。 */
    private boolean autoAvoidConflicts = true;

    /** 每项修复的最终生效状态（默认全部开启）。 */
    private final Map<BugFix, Boolean> effective = new EnumMap<>(BugFix.class);

    /** 每项修复在配置文件里是否被用户显式写过（显式写过则覆盖自动避让）。 */
    private final Map<BugFix, Boolean> manual = new EnumMap<>(BugFix.class);

    private BugFixConfig() {
        for (BugFix fix : BugFix.values()) {
            this.effective.put(fix, true);
        }
    }

    private static BugFixConfig instance;

    /** 加载并返回全局唯一配置。任何异常都已兜底，不会向外抛出。 */
    public static synchronized BugFixConfig get() {
        if (instance == null) {
            instance = new BugFixConfig();
            instance.load();
            instance.applyConflictAvoidance();
        }
        return instance;
    }

    /** 供 Mixin 查询：该修复项当前是否生效。查询失败视为关闭，保证不崩溃。 */
    public static boolean isEnabled(BugFix fix) {
        try {
            return get().effective.getOrDefault(fix, false);
        } catch (Throwable t) {
            LOGGER.warn("查询修复项 {} 状态失败，按关闭处理", fix.mojiraId(), t);
            return false;
        }
    }

    private void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("wjys-fix-patch.json");
        try {
            if (!Files.exists(path)) {
                writeDefault(path);
                LOGGER.info("已生成默认配置文件：{}", path);
            } else {
                String raw = Files.readString(path, StandardCharsets.UTF_8);
                JsonObject root = GSON.fromJson(raw, JsonObject.class);
                if (root == null) {
                    throw new IllegalStateException("配置文件为空或无法解析");
                }
                if (root.has("autoAvoidConflicts")) {
                    this.autoAvoidConflicts = root.get("autoAvoidConflicts").getAsBoolean();
                }
                JsonObject fixes = root.has("fixes") ? root.getAsJsonObject("fixes") : null;
                if (fixes != null) {
                    for (BugFix fix : BugFix.values()) {
                        if (fixes.has(fix.configKey())) {
                            boolean value = fixes.get(fix.configKey()).getAsBoolean();
                            this.effective.put(fix, value);
                            this.manual.put(fix, value);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // 异常兜底：解析失败回退默认（全部开启 + 自动避让开启），只写日志，不打断游戏
            this.autoAvoidConflicts = true;
            for (BugFix fix : BugFix.values()) {
                this.effective.put(fix, true);
            }
            this.manual.clear();
            LOGGER.warn("读取配置文件失败，回退默认配置（全部修复开启、自动避让开启）：{}", path, t);
        }
    }

    /** 启动时检测已加载模组，命中冲突名单则关闭对应修复项（手动显式设置过的项不受影响）。 */
    private void applyConflictAvoidance() {
        if (!this.autoAvoidConflicts) {
            LOGGER.info("冲突自动避让已被配置关闭，所有修复项强制生效");
            return;
        }
        for (BugFix fix : BugFix.values()) {
            if (this.manual.containsKey(fix)) {
                LOGGER.info("修复项 {}（{}）被手动显式设置为 {}，跳过自动避让", fix.name(), fix.mojiraId(), this.manual.get(fix));
                continue;
            }
            for (String modId : fix.conflictMods()) {
                if (isModLoaded(modId)) {
                    this.effective.put(fix, false);
                    LOGGER.info("检测到冲突模组 [{}]，自动关闭修复项 {}（{}）：{}", modId, fix.name(), fix.mojiraId(), fix.description());
                    break;
                }
            }
        }
    }

    private static boolean isModLoaded(String modId) {
        try {
            String needle = modId.toLowerCase(Locale.ROOT);
            return FabricLoader.getInstance().isModLoaded(modId)
                    || FabricLoader.getInstance().getAllMods().stream()
                    .anyMatch(m -> m.getMetadata().getId().toLowerCase(Locale.ROOT).contains(needle));
        } catch (Throwable t) {
            LOGGER.warn("检测模组 {} 是否加载时出错，按未加载处理", modId, t);
            return false;
        }
    }

    private void writeDefault(Path path) {
        JsonObject root = new JsonObject();
        root.addProperty("autoAvoidConflicts", true);
        JsonObject fixes = new JsonObject();
        for (BugFix fix : BugFix.values()) {
            fixes.addProperty(fix.configKey(), true);
        }
        root.add("fixes", fixes);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            LOGGER.warn("写入默认配置文件失败（不影响运行）：{}", path, t);
        }
    }

    /** 打印一次当前生效摘要，方便玩家在日志里核对。 */
    public static void logSummary() {
        BugFixConfig config = get();
        for (BugFix fix : BugFix.values()) {
            LOGGER.info("修复项 {}（{}）{} —— {}", fix.name(), fix.mojiraId(),
                    Boolean.TRUE.equals(config.effective.get(fix)) ? "已启用" : "已停用", fix.description());
        }
    }
}
