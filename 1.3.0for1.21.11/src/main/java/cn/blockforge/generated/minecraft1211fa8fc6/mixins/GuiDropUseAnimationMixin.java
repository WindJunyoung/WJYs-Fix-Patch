package cn.blockforge.generated.minecraft1211fa8fc6.mixins;

import cn.blockforge.generated.minecraft1211fa8fc6.BugFix;
import cn.blockforge.generated.minecraft1211fa8fc6.BugFixConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class GuiDropUseAnimationMixin {
    @Shadow
    protected MinecraftClient client;

    @Inject(
            method = "swingHand(Lnet/minecraft/util/Hand;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void wjys$cancelGuiDropSwingHand(Hand hand, CallbackInfo ci) {
        if (!BugFixConfig.isEnabled(BugFix.MC302687)) return;
        if (client.currentScreen != null) {
            ci.cancel();
        }
    }
}
