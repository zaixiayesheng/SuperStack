package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.20.1 的 {@code Container.getMaxStackSize()} 默认返回 64，而玩家物品栏直接吃这个默认值：
 * 拾取、{@code /give}、把物品塞回背包时都按 64 合并，于是物品上限抬到 32768 之后背包还是每格 64。
 *
 * <p>1.20.1 的 Mixin 版本不支持给接口注入，所以不能直接改 {@code Container} 的默认方法。
 * {@code Inventory} 本身也没有声明这个方法，编译器把 {@code this.getMaxStackSize()} 编成
 * {@code invokevirtual Inventory.getMaxStackSize()I}，因此这里按实际调用点重定向。
 * 槽位界面那一侧由 {@code SlotMixin} 负责。
 */
@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    @Redirect(
            method = {
                    "hasRemainingSpaceForItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
                    "addResource(ILnet/minecraft/world/item/ItemStack;)I"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getMaxStackSize()I"))
    private int superstack$containerLimit(Inventory inventory) {
        return SuperStackConfig.maxStackSize();
    }
}
