package cn.blockforge.generated.minecraft1211fa8fc6.mixins;

import cn.blockforge.generated.minecraft1211fa8fc6.BugFix;
import cn.blockforge.generated.minecraft1211fa8fc6.BugFixConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 【Bug1 · MC-219981】修复首领僵尸出生血量不满（保留原版随机血量设计）。
 *
 * <p>原版机制（1.21.1 字节码实测）：{@code ZombieEntity#applyAttributeModifiers(float)}
 * 以「难度 × 5%」的概率掷出首领判定，命中后给 GENERIC_MAX_HEALTH 挂上
 * {@code minecraft:leader_zombie_bonus}——ADD_MULTIPLIED_TOTAL、随机 1~4 的加成值，
 * 生命上限随之随机放大到约 40~100。这就是原版意图：首领僵尸的血量是 20~100 的随机值。
 * 本模组<b>不改动</b>这一随机机制。</p>
 *
 * <p>Mojira 收录的 MC-219981 是它的配套漏洞：上限掷高之后<b>当前血量仍停留在 20</b>，
 * 不随上限重算（{@code finalizeSpawn} 与 {@code convertTo} 两条路径都一样），于是
 * 首领僵尸一出生就挂着 "20/80" 的残血面板，实际战力与普通僵尸无异。</p>
 *
 * <p>修复（与 NeoForge 官方修复 PR #2451 同思路）：在 {@code applyAttributeModifiers}
 * 的 HEAD 快照当前上限与当前血量，TAIL 处若发现 {@code leader_zombie_bonus} 把上限
 * 提高了，就把<b>增加的那一截等量补进当前血量</b>（封顶新上限）。效果：首领僵尸
 * 出生即满血、血量依旧是 20~100 的随机；已有伤的个体（如僵尸村民转化）保留既有
 * 伤害，只是整体上移，不会被误奶满。增援、自然生成、袭击队长、僵尸转化等所有
 * 掷首领的路径都经过该方法，一次覆盖。</p>
 *
 * <p>旧存档中在修复装入前生成的首领不做读档补偿——读档补满会把本来就残血的个体
 * 错误奶满，且无法区分"bug 残血"与"战斗中残血"。修复装入后新生成/转化的首领即为满血。</p>
 *
 * <p>潜在冲突：怪物属性、生成类模组。冲突时可关闭本项或交给自动避让。
 * 注入点使用 {@code @At("HEAD")} + {@code @At("TAIL")}（不覆盖任何方法），require=0 +
 * 全量 try/catch：目标签名不匹配或注入失败时静默回退原版行为；首次实际修正时会写
 * 一条 INFO 日志，方便玩家在 latest.log 里核对修复是否生效。</p>
 */
@Mixin(ZombieEntity.class)
public abstract class ZombieLeaderHealthMixin extends MobEntity {

    @Unique
    private static final Identifier WJYS$LEADER_BONUS = Identifier.ofVanilla("leader_zombie_bonus");

    /** HEAD 快照：调用前的生命上限；-1 表示快照无效（配置关闭或记录失败）。 */
    @Unique
    private double wjys$capturedMaxHealth = -1.0;

    /** HEAD 快照：调用前的当前血量。 */
    @Unique
    private float wjys$capturedHealth = -1.0F;

    /** 首次生效时打一条日志，之后不再重复。 */
    @Unique
    private static boolean wjys$logged = false;

    protected ZombieLeaderHealthMixin(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
    }

    /**
     * 唯一的首领加成投掷点：生成（任意 SpawnReason，含增援 REINFORCEMENT）
     * 与僵尸转化都会经过这里。调用前记录血量快照。
     */
    @Inject(
            method = "applyAttributeModifiers(F)V",
            at = @At("HEAD"),
            require = 0
    )
    private void wjys$captureHealthSnapshot(float difficulty, CallbackInfo ci) {
        wjys$capturedMaxHealth = -1.0;
        if (!BugFixConfig.isEnabled(BugFix.MC219981)) {
            return;
        }
        try {
            wjys$capturedMaxHealth = this.getMaxHealth();
            wjys$capturedHealth = this.getHealth();
        } catch (Throwable t) {
            wjys$capturedMaxHealth = -1.0;
        }
    }

    /**
     * 投掷结束后：若本次首领加成把生命上限提高了，就把当前血量等量上移（封顶新上限）。
     */
    @Inject(
            method = "applyAttributeModifiers(F)V",
            at = @At("TAIL"),
            require = 0
    )
    private void wjys$healToRolledMaxHealth(float difficulty, CallbackInfo ci) {
        if (wjys$capturedMaxHealth < 0.0 || wjys$capturedHealth < 0.0F) {
            return; // 快照无效：配置关闭或 HEAD 注入未执行，保持原版行为
        }
        try {
            ZombieEntity self = (ZombieEntity) (Object) this;
            EntityAttributeInstance maxHealth = self.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            if (maxHealth == null || maxHealth.getModifier(WJYS$LEADER_BONUS) == null) {
                return; // 没掷出首领，什么都不做
            }
            double newMax = self.getMaxHealth();
            double gained = newMax - wjys$capturedMaxHealth;
            if (gained <= 0.0) {
                return; // 上限没有变化（例如转化时未再次命中），保持原版行为
            }
            // MC-219981：上限被首领加成随机放大后，原版不会重算当前血量。
            // 修复：增加多少上限就补多少血量，封顶新上限（保留既有伤害）。
            float healed = (float) Math.min(newMax, wjys$capturedHealth + gained);
            self.setHealth(healed);
            if (!wjys$logged) {
                wjys$logged = true;
                BugFixConfig.LOGGER.info("[MC-219981] 修复已生效：首领僵尸当前血量已随随机上限补齐（保留原版 20~100 随机血量设计），示例 {}/{}",
                        (int) healed, (int) newMax);
            }
        } catch (Throwable t) {
            // 异常兜底：静默失效回退原版，不弹窗、不崩溃
            BugFixConfig.LOGGER.warn("[MC-219981] 首领僵尸血量修复注入失败，已回退原版行为", t);
        }
    }
}
