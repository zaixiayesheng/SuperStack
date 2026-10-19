package com.zaixiayesheng.superstack.mixin;

import com.zaixiayesheng.superstack.SuperStackCraftLimiter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 每次点击开始时把合成计数清零，封顶只针对「这一次点击」。 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"))
    private void superstack$resetCraftCounter(int slotId, int button, ClickType clickType, Player player,
                                              CallbackInfo ci) {
        SuperStackCraftLimiter.reset();
    }
}
