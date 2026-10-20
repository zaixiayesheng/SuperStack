package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 机械动力的鼓风机按「物品数量 / 16」放大处理时长：原版 64 个一组时是 4 倍，
 * 堆叠上限调到 32768 之后会变成 2048 倍，看起来就像「洗不动」。
 * 这里把放大倍数钳到配置值（默认 4）。
 */
@Mixin(targets = "com.simibubi.create.content.kinetics.fan.processing.FanProcessing", remap = false)
public abstract class CreateFanProcessingMixin {

    @ModifyVariable(
            method = "applyProcessing(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/world/level/Level;Lcom/simibubi/create/content/kinetics/fan/processing/FanProcessingType;)Lcom/simibubi/create/content/kinetics/belt/behaviour/TransportedItemStackHandlerBehaviour$TransportedResult;",
            at = @At("STORE"),
            index = 4,
            remap = false)
    private static int superstack$capTransportedModifier(int modifier) {
        return Math.min(modifier, SuperStackConfig.fanProcessingTimeCap());
    }

    @ModifyVariable(
            method = "decrementProcessingTime",
            at = @At("STORE"),
            index = 6,
            remap = false)
    private static int superstack$capItemEntityModifier(int modifier) {
        return Math.min(modifier, SuperStackConfig.fanProcessingTimeCap());
    }
}
