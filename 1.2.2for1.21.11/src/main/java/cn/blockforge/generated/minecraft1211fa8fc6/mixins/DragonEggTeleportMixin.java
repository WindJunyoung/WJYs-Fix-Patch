package cn.blockforge.generated.minecraft1211fa8fc6.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(ClientWorld.class)
public class DragonEggTeleportMixin {

    @Inject(method = "syncWorldEvent", at = @At("HEAD"), cancellable = true)
    private void wjys$fixDragonEggParticles(PlayerEntity player, int eventId, BlockPos pos, int data, CallbackInfo ci) {
        // 2003 是龙蛋传送的专属世界事件
        if (eventId == 2003) {
            // 1. 直接取消原版生成，阻断错误粒子的出现
            ci.cancel();

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.particleManager == null) return;

            // 2. 从原版打包的 data 中解包出传送的方向偏移量
            int dx = (byte) (data & 0xFF);
            int dy = (byte) ((data >> 8) & 0xFF);
            int dz = (byte) ((data >> 16) & 0xFF);

            Vec3d start = Vec3d.ofCenter(pos);
            Vec3d offset = new Vec3d(dx, dy, dz);
            double dist = offset.length();

            // 3. 如果有位移，则生成带正确飞行速度的粒子
            if (dist > 0.001) {
                Vec3d dir = offset.normalize();
                // 距离越远，初速度稍微大一点
                double speed = 0.15 + 0.05 * Math.min(dist, 6.0);
                Vec3d velocity = dir.multiply(speed);

                Random random = new Random();
                for (int i = 0; i < 64; i++) {
                    // 在起点位置稍作随机散布，让粒子群看起来更自然
                    double ox = (random.nextDouble() - 0.5) * 0.5;
                    double oy = (random.nextDouble() - 0.5) * 0.5;
                    double oz = (random.nextDouble() - 0.5) * 0.5;

                    // 使用客户端粒子管理器直接生成带有初速度的传送门粒子
                    client.particleManager.addParticle(
                            ParticleTypes.PORTAL,
                            start.x + ox, start.y + oy, start.z + oz,
                            velocity.x, velocity.y, velocity.z
                    );
                }
            }
        }
    }
}