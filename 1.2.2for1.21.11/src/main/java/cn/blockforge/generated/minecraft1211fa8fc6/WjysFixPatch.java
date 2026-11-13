package cn.blockforge.generated.minecraft1211fa8fc6;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WJY's Fix Patch —— 轻量原版 Bug 修复模组（Minecraft 1.21.1 / Fabric，纯 Mixin）。
 *
 * <p>入口点只负责两件事：加载配置（含冲突自动避让检测）并把结果写进日志。
 * 全部修复逻辑都在独立 Mixin 类中，单点失败互不影响、自动回退原版行为。</p>
 */
public final class WjysFixPatch implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("WJYsFixPatch");

    @Override
    public void onInitialize() {
        try {
            BugFixConfig.logSummary();
            LOGGER.info("WJY's Fix Patch 初始化完成：共 {} 个修复项，详见上方摘要", BugFix.values().length);
        } catch (Throwable t) {
            // 异常兜底：入口点出错也绝不崩溃，Mixin 注入各自还会再查一次配置
            LOGGER.warn("WJY's Fix Patch 初始化日志输出失败（不影响修复）", t);
        }
    }
}
