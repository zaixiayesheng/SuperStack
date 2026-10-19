package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.20.1 的 {@code Container.getMaxStackSize()} 默认返回 64，而玩家物品栏直接吃这个默认值：
 * 拾取、{@code /give}、把物品塞回背包时都按 64 合并，于是物品上限抬到 32768 之后背包还是每格 64。
 *
 * <p>1.20.1 的 Mixin 版本不支持给接口注入，所以不能直接改 {@code Container} 的默认方法，
 * 只能把 {@code Inventory} 自己那几处 {@code this.getMaxStackSize()} 调用重定向到配置值。
 * 槽位界面那一侧由 {@code SlotMixin} 负责。
 */
@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    @Redirect(
            method = {
                    "hasRemainingSpaceForItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
                    "addResource(ILnet/minecraft/world/item/ItemStack;)I"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getMaxStackSize()I"))
    private int superstack$containerLimit(Container container) {
        return SuperStackConfig.maxStackSize();
    }
}
