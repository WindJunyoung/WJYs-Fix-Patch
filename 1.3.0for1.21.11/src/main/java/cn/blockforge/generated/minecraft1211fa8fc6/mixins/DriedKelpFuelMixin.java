package cn.blockforge.generated.minecraft1211fa8fc6.mixins;

import cn.blockforge.generated.minecraft1211fa8fc6.BugFix;
import cn.blockforge.generated.minecraft1211fa8fc6.BugFixConfig;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 【Bug3 · MC-267023】修复干海带块燃烧时间的 1 刻浮点尾差。
 *
 * <p>原版 {@code AbstractFurnaceBlockEntity#getFuelTime} 对干海带块返回 4001 刻
 * （应为整数 4000 刻，多出的 1 刻是历史浮点换算残留的尾差）。</p>
 *
 * <p>修复：仅在 TAIL 处做数值兜底——返回值恰好等于 4001 且燃料是干海带块时才修正为 4000。
 * 不触碰任何其他燃料、不改动整体平衡；其他模组通过燃料注册表自定义的数值（≠4001）原样保留。
 * 高炉/烟熏炉的 getFuelTime 覆写内部都调用本方法，因此一并生效。</p>
 *
 * <p>潜在冲突：熔炉优化、燃料数值类模组（检测到 lithium、ferritecore 时自动关闭本项）。
 * require=0 + try/catch：注入失败静默回退原版行为。</p>
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class DriedKelpFuelMixin {

    @Inject(
            method = "getFuelTime(Lnet/minecraft/item/ItemStack;)I",
            at = @At("TAIL"),
            cancellable = true,
            require = 0
    )
    private void wjys$fixDriedKelpBurnTime(ItemStack fuel, CallbackInfoReturnable<Integer> cir) {
        if (!BugFixConfig.isEnabled(BugFix.MC267023)) {
            return;
        }
        try {
            // 仅修正 4001 → 4000 的浮点尾差，其余数值（含其他模组自定义值）不动
            if (cir.getReturnValueI() == 4001 && fuel.isOf(Blocks.DRIED_KELP_BLOCK.asItem())) {
                cir.setReturnValue(4000);
            }
        } catch (Throwable t) {
            // 异常兜底：保持原版返回值
            BugFixConfig.LOGGER.warn("[MC-267023] 干海带块燃烧时间修复注入失败，已回退原版行为", t);
        }
    }
}
