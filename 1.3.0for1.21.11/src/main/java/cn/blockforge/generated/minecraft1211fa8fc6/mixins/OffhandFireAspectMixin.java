package cn.blockforge.generated.minecraft1211fa8fc6.mixins;

import cn.blockforge.generated.minecraft1211fa8fc6.BugFix;
import cn.blockforge.generated.minecraft1211fa8fc6.BugFixConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 【Bug4 · MC-306614】修复副手火焰附加长矛冲刺击杀不触发熟食掉落。
 *
 * <p>原版 {@code EnchantmentHelper#onTargetDamaged(ServerWorld, Entity, DamageSource)}
 * 在应用“命中目标”类附魔效果（火焰附加的点燃效果等）时，只取攻击者的
 * {@code getWeaponStack()}（主手武器栈）。副手持有带火焰附加的长矛时，
 * 点燃效果从不生效，掉落表的 on_fire 判据不成立，肉类不会被烹饪。</p>
 *
 * <p>修复：在该方法 TAIL 处补一次判定——若目标尚未被点燃（说明主手没有触发成功）、
 * 攻击者副手持有非空物品，则用与主手完全相同的入口
 * {@code onTargetDamaged(ServerWorld, Entity, DamageSource, ItemStack)} 走一遍副手物品，
 * 使副手与主手的火焰附加判定逻辑一致。</p>
 *
 * <p>潜在冲突：战斗优化、附魔扩展类模组（检测到 combatroll、bettercombat 时自动关闭本项）。
 * 主手已经点燃时本注入直接跳过，不与主手逻辑叠加重复。require=0 + try/catch 兜底。</p>
 */
@Mixin(EnchantmentHelper.class)
public class OffhandFireAspectMixin {

    @Inject(
            method = "onTargetDamaged(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;)V",
            at = @At("TAIL"),
            require = 0
    )
    private static void wjys$alignOffhandFireAspect(ServerWorld world, Entity target, DamageSource source, CallbackInfo ci) {
        if (!BugFixConfig.isEnabled(BugFix.MC306614)) {
            return;
        }
        try {
            if (!(source.getAttacker() instanceof LivingEntity attacker)) {
                return;
            }
            // 主手火焰附加（或其他路径）已经点燃目标时不再重复处理
            if (target instanceof LivingEntity livingTarget && livingTarget.isOnFire()) {
                return;
            }
            ItemStack offHand = attacker.getOffHandStack();
            if (offHand.isEmpty()) {
                return;
            }
            // 与主手完全一致的判定入口，把副手物品也纳入 on_target_damaged 附魔效果
            EnchantmentHelper.onTargetDamaged(world, target, source, offHand);
        } catch (Throwable t) {
            // 异常兜底：静默失效回退原版行为
            BugFixConfig.LOGGER.warn("[MC-306614] 副手火焰附加修复注入失败，已回退原版行为", t);
        }
    }
}
