package com.zaixiayesheng.superstack.mixin.compat;

import com.zaixiayesheng.superstack.SuperStackConfig;
import net.minecraft.world.Container;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Forge item handler wrapper limit.
 *
 * <p>{@code InvWrapper.getSlotLimit} returns {@code inv.getMaxStackSize()} (64 for any vanilla style
 * container). Mods that move items through the Forge item handler capability therefore still clamp
 * at 64 even though the GUI slots accept more. The call is redirected to the configured limit.
 */
@Mixin(InvWrapper.class)
public abstract class InvWrapperMixin {

    @Redirect(
            method = "getSlotLimit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;getMaxStackSize()I"),
            require = 0
    )
    private int superstack$limit(Container container) {
        return SuperStackConfig.compatStackLimit();
    }
}