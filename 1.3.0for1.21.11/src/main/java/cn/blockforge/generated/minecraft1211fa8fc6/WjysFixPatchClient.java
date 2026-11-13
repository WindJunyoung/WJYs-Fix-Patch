package cn.blockforge.generated.minecraft1211fa8fc6;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端入口点：仅确认配置已加载（Bug2 的客户端 Mixin 会自行查询开关）。
 * 其余修复项运行在服务端逻辑，客户端安装即可生效，服务端安装对所有玩家生效。
 */
public final class WjysFixPatchClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("WJYsFixPatch/Client");

    @Override
    public void onInitializeClient() {
        try {
            BugFixConfig.get();
            LOGGER.info("客户端修复项就绪（MC-302687 GUI丢弃动画拦截已随客户端加载）");
        } catch (Throwable t) {
            LOGGER.warn("客户端初始化日志输出失败（不影响修复）", t);
        }
    }
}
