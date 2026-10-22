package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 机械动力：鼓风机按配置的虚拟堆叠数量计算处理时长。
 *
 * <p>Create 的 {@code decrementProcessingTime} 把物品数量换算成倍数
 * {@code (count - 1) / 16 + 1} 再乘以基础时长，堆叠上限一大，处理时间就跟着线性爆炸。
 * 这里直接改写它算好待存的那个倍数，等价于「一个堆叠按最多 N 个来算」。</p>
 *
 * <p>用局部变量注入而不是重定向 {@code ItemStack#getCount}：重定向需要重映射
 * 原版方法名，而这个 mixin 的目标是第三方类（remap = false），refmap 里拿不到映射。</p>
 */
@Mixin(targets = "com.simibubi.create.content.kinetics.fan.processing.FanProcessing", remap = false)
public class CreateFanProcessingMixin {

    @ModifyVariable(method = "decrementProcessingTime", at = @At("STORE"), index = 6, remap = false)
    private static int superstack$virtualModifier(int original, ItemEntity entity) {
        int count = SuperStackConfig.fanCountFor(entity.getItem().getCount());
        return (count - 1) / 16 + 1;
    }
}
