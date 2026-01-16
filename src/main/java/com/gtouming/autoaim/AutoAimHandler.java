package com.gtouming.autoaim;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;

/**
 * 自动瞄准处理器
 * 监听玩家单击攻击事件，自动瞄准配置中的实体并攻击
 */
@EventBusSubscriber(modid = Autoaim.MODID, value = Dist.CLIENT)
public class AutoAimHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 是否正在瞄准
    private static boolean isAiming = false;

    /**
     * 监听鼠标点击事件，检测单击攻击
     */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        // 检查是否启用自动瞄准
        if (!Config.enableAutoAim) {
            return;
        }

        // 检查是否是左键点击 (button 0)
        if (event.getAction() != 1 || event.getButton() != 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // 确保玩家存在且在游戏中
        if (player == null || !player.isAlive()) {
            return;
        }

        LOGGER.debug("检测到单击攻击，开始自动瞄准");

        // 查找并瞄准最近的实体
        Entity target = findNearestTarget(player);

        if (target != null) {
            // 瞄准实体
            aimAtEntity(player, target);
            isAiming = true;
            LOGGER.debug("已瞄准实体: {}", target.getType().getDescription());

            // 自动攻击目标
            attackTarget(mc, player, target);
        } else {
            LOGGER.debug("未找到符合条件的实体");
        }
    }

    /**
     * 查找玩家周围最近的符合条件的实体
     * 
     * @param player 玩家
     * @return 最近的符合条件的实体，如果没有则返回null
     */
    private static Entity findNearestTarget(LocalPlayer player) {
        Vec3 playerPos = player.position();
        double searchDistance = Config.searchDistance;

        // 创建搜索范围
        AABB searchBox = new AABB(
            playerPos.x - searchDistance, 
            playerPos.y - searchDistance, 
            playerPos.z - searchDistance,
            playerPos.x + searchDistance, 
            playerPos.y + searchDistance, 
            playerPos.z + searchDistance
        );

        // 获取搜索范围内的所有实体
        List<Entity> entities = player.level().getEntities(player, searchBox, entity -> {
            // 排除玩家自己
            if (entity == player) {
                return false;
            }

            // 只检查活体实体
            if (!(entity instanceof LivingEntity)) {
                return false;
            }

            // 检查实体是否在目标列表中
            return Config.targetEntities.contains(entity.getType());
        });

        // 如果没有找到符合条件的实体，返回null
        if (entities.isEmpty()) {
            return null;
        }

        // 找到距离最近的实体
        return entities.stream()
            .min(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(playerPos)))
            .orElse(null);
    }

    /**
     * 将玩家的准心对准指定实体
     * 
     * @param player 玩家
     * @param target 目标实体
     */
    private static void aimAtEntity(LocalPlayer player, Entity target) {
        Vec3 playerEyePos = player.getEyePosition(1.0F);
        Vec3 targetPos = new Vec3(
            target.getX(),
            target.getY() + target.getEyeHeight() * 0.5, // 瞄准实体中心偏上位置
            target.getZ()
        );

        // 计算从玩家到目标的方向向量
        Vec3 direction = targetPos.subtract(playerEyePos).normalize();

        // 计算需要的偏航角和俯仰角
        float yaw = (float) (Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.asin(direction.y));

        // 设置玩家的旋转角度
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    /**
     * 自动攻击目标实体
     * 
     * @param mc Minecraft实例
     * @param player 玩家
     * @param target 目标实体
     */
    private static void attackTarget(Minecraft mc, LocalPlayer player, Entity target) {
        // 确保目标是一个活体实体
        if (!(target instanceof LivingEntity)) {
            return;
        }

        LivingEntity livingTarget = (LivingEntity) target;

        // 检查玩家是否可以攻击（冷却时间等）
        if (!player.canAttack(livingTarget)) {
            LOGGER.debug("无法攻击目标: {}", target.getType().getDescription());
            return;
        }

        // 执行攻击
        mc.gameMode.attack(player, livingTarget);
        LOGGER.debug("已攻击实体: {}", target.getType().getDescription());
    }
}
