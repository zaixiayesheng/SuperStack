package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 机械动力的鼓风机按「物品数量 / 16」放大处理时长：原版 64 个一组时是 4 倍，
 * 堆叠上限调到 32768 之后会变成 2048 倍，看起来就像「洗不动」。
 *
 * <p>这里不直接替换公式，只把参与计算的数量钳到配置的虚拟堆叠数量：
 * 数量小于等于该值时保持原版算法（小堆叠不会被拖慢），超过时按该值计算。
 * 公式仍是原版的 (count - 1) / 16 + 1。
 */
@Mixin(targets = "com.simibubi.create.content.kinetics.fan.processing.FanProcessing", remap = false)
public abstract class CreateFanProcessingMixin {

    /**
     * 传送带、置物台等位置上的物品走这里。
     */
    @Redirect(
            method = "applyProcessing(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I"),
            remap = false)
    private static int superstack$cappedTransportedCount(ItemStack stack) {
        return superstack$cappedCount(stack);
    }

    /**
     * 掉落在地上、被吹起来的物品实体走这里。
     */
    @Redirect(
            method = "decrementProcessingTime",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I"),
            remap = false)
    private static int superstack$cappedItemEntityCount(ItemStack stack) {
        return superstack$cappedCount(stack);
    }

    private static int superstack$cappedCount(ItemStack stack) {
        int count = stack.getCount();
        int virtualCount = SuperStackConfig.fanStackCount();
        return count < virtualCount ? count : virtualCount;
    }
}
