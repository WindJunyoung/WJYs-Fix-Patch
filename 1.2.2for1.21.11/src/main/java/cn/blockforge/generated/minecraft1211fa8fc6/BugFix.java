package cn.blockforge.generated.minecraft1211fa8fc6;

import java.util.List;

/**
 * 本模组收录的原版 Bug 修复项清单。
 *
 * <p>每一项对应 Mojira 的一个编号、一个独立的 Mixin 类、一个独立的配置开关。
 * {@link #conflictMods()} 是「冲突自动避让」名单：启动时检测到其中任何一个模组
 * 被加载，就会自动关闭对应修复项（可以在配置文件里手动写值覆盖，见 {@link BugFixConfig}）。</p>
 */
public enum BugFix {

    /**
     * 【Bug1 · MC-219981】修复首领僵尸出生血量不满（保留原版随机血量设计）。
     * 原版在 applyAttributeModifiers 里以约 难度×5% 的概率把僵尸判定为"首领"，
     * 生命上限随随机修饰符放大到 20~100 内的随机值——这是原版本来的意图，本模组
     * 原样保留。真正的 bug 是上限掷高后当前血量仍停在 20、不随上限重算，首领僵尸
     * 一出生就挂着 "20/80" 的残血面板。修复后：凡命中首领判定（增援、自然生成、
     * 袭击队长、僵尸转化等所有路径）都把当前血量随上限等量补齐、出生即满血；
     * 已有伤的个体保留既有伤害，不会被误奶满。
     * 潜在冲突：怪物属性优化、怪物生成类模组（无统一模组 id，仅保留手动开关）。
     */
    MC219981("mc219981_zombie_leader_health", "MC-219981", "首领僵尸出生血量不满（血量按原版随机20~100）", List.of()),

    /**
     * 【Bug2 · MC-302687】修复打开物品栏/背包 GUI 时按丢弃键 Q 错误触发长矛戳刺（使用）动画。
     * 修复逻辑：GUI 打开状态下拦截物品使用动画触发，仅执行丢弃物品逻辑。
     * 潜在冲突：输入控制优化、战斗动画类模组。
     */
    MC302687("mc302687_gui_drop_use_animation", "MC-302687", "物品栏按Q触发长矛戳刺动画", List.of("combatroll", "bettercombat")),

    /**
     * 【Bug3 · MC-267023】修复干海带块燃烧时间浮点误差：4001 刻 → 4000 刻。
     * 仅修正数值精度（尾差兜底），不改动整体燃料平衡，也不影响其他模组自定义的燃料值。
     * 潜在冲突：燃料数值类模组。性能优化模组（Lithium、FerriteCore 等）不涉及燃料
     * 结算逻辑，不在避让名单内——装了优化模组本修复依然生效。
     */
    MC267023("mc267023_dried_kelp_burn_time", "MC-267023", "干海带块燃烧时间浮点误差", List.of()),

    /**
     * 【Bug4 · MC-306614】修复副手持有带火焰附加附魔的长矛、冲刺攻击击杀生物时
     * 肉类掉落物不会被烹饪的问题（原版 onTargetDamaged 只检查主手武器）。
     * 修复后副手与主手火焰附加判定逻辑一致。
     * 潜在冲突：战斗优化、附魔扩展类模组。
     */
    MC306614("mc306614_offhand_fire_aspect", "MC-306614", "副手火焰附加长矛不触发熟食掉落", List.of("combatroll", "bettercombat")),

    /**
     * 【Bug5 · MC-310787】移除村民交易升级的强制过渡等待期（原版 40 刻）。
     * 获得足够经验后即时完成升级、刷新交易列表；升级粒子与音效保持原版。
     * 潜在冲突：村民行为改写类模组（Better Villagers、Lazier Villagers 等）。
     * 性能优化模组（Lithium、FerriteCore 等）不改村民升级逻辑，已移出避让名单，
     * 装了它们本修复依然生效。
     */
    MC310787("mc310787_villager_level_up_delay", "MC-310787", "村民升级强制等待时间", List.of("bettervillagers", "laziervillagers")),

    /**
     * 【Bug6 · MC-265423】修复村民一次获得足够连升多级的经验时只升 1 级、
     * 超出部分经验被闲置丢失的问题。修复后剩余经验自动结转至下一等级（与玩家经验升级逻辑一致）。
     * 潜在冲突：村民行为改写、交易类模组（性能优化模组不在避让名单内，装了依然生效）。
     */
    MC265423("mc265423_villager_experience_overflow", "MC-265423", "村民升级经验溢出", List.of("bettervillagers", "laziervillagers"));

    private final String configKey;
    private final String mojiraId;
    private final String description;
    private final List<String> conflictMods;

    BugFix(String configKey, String mojiraId, String description, List<String> conflictMods) {
        this.configKey = configKey;
        this.mojiraId = mojiraId;
        this.description = description;
        this.conflictMods = conflictMods;
    }

    public String configKey() {
        return this.configKey;
    }

    public String mojiraId() {
        return this.mojiraId;
    }

    public String description() {
        return this.description;
    }

    public List<String> conflictMods() {
        return this.conflictMods;
    }
}
