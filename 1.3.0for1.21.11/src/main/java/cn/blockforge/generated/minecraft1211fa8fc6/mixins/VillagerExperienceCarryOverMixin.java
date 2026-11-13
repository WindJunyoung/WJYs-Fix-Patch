package cn.blockforge.generated.minecraft1211fa8fc6.mixins;

import cn.blockforge.generated.minecraft1211fa8fc6.BugFix;
import cn.blockforge.generated.minecraft1211fa8fc6.BugFixConfig;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VillagerEntity.class)
public abstract class VillagerExperienceCarryOverMixin {

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
            at = @At("TAIL"),
            require = 0
    )
    @SuppressWarnings("ConstantConditions")
    private void wjys$carryOverOverflowExperience(CallbackInfo ci) {
        if (!BugFixConfig.isEnabled(BugFix.MC265423)) {
            return;
        }
        try {
            // 强转为目标类，调用公共方法 getEntityWorld() 获取世界实例
            VillagerEntity self = (VillagerEntity)(Object)this;
            World world = self.getEntityWorld();

            if (world instanceof ClientWorld || this.levelingUp || this.levelUpTimer > 0) {
                return;
            }

            ServerWorld serverWorld = (ServerWorld) world;
            int carried = 0;
            while (carried < 5 && this.canLevelUp()) {
                this.levelUp(serverWorld);
                // 强转后调用 addStatusEffect 方法
                self.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0));
                carried++;
            }

            if (carried > 0) {
                this.sendOffersToCustomer();
            }
        } catch (Throwable t) {
            BugFixConfig.LOGGER.warn("[MC-265423] 村民经验溢出结转修复注入失败，已回退原版行为", t);
        }
    }
}
