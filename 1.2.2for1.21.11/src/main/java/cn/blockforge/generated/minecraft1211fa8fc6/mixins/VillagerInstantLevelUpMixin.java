package cn.blockforge.generated.minecraft1211fa8fc6.mixins;

import cn.blockforge.generated.minecraft1211fa8fc6.BugFix;
import cn.blockforge.generated.minecraft1211fa8fc6.BugFixConfig;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 【Bug5 · MC-310787】移除村民交易升级的强制过渡等待期（原版40刻）。
 */
@Mixin(VillagerEntity.class)
public abstract class VillagerInstantLevelUpMixin {

    @Shadow
    private boolean levelingUp;

    @Shadow
    private int levelUpTimer;

    @Shadow
    private boolean canLevelUp() {
        return false;
    }

    @Shadow
    private void levelUp(ServerWorld world) {
    }

    @Shadow
    private void sendOffersToCustomer() {
    }

    @Inject(
            method = "tick()V",
            at = @At("HEAD"),
            require = 0
    )
    @SuppressWarnings("ConstantConditions")
    private void wjys$skipLevelUpDelay(CallbackInfo ci) {
        if (!BugFixConfig.isEnabled(BugFix.MC310787)) {
            return;
        }
        try {
            VillagerEntity self = (VillagerEntity)(Object)this;
            World world = self.getEntityWorld();

            // 服务端、处于升级等待期、且经验足够时，直接跳过等待执行升级
            if (!(world instanceof ClientWorld) && this.levelingUp && this.canLevelUp()) {
                this.levelUpTimer = 0;
                this.levelUp((ServerWorld) world);
                this.sendOffersToCustomer();
            }
        } catch (Throwable t) {
            BugFixConfig.LOGGER.warn("[MC-310787] 村民即时升级修复注入失败，已回退原版行为", t);
        }
    }
}
