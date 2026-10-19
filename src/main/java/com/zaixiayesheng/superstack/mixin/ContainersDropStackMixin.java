package com.zaixiayesheng.superstack.mixin;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Container drop size.
 *
 * <p>Vanilla scatters broken container contents in random chunks of 10 to 30 items
 * ({@code stack.split(random.nextInt(21) + 10)} in a loop). A 32768 stack therefore turns into about
 * 1600 item entities, which stalls the server and floods the client. Stacks above the split
 * threshold are dropped as a single entity instead; ordinary stacks keep the vanilla scatter.
 */
@Mixin(Containers.class)
public abstract class ContainersDropStackMixin {

    private static final int SUPERSTACK_SPLIT_THRESHOLD = 128;

    @Inject(method = "dropItemStack", at = @At("HEAD"), cancellable = true)
    private static void superstack$dropAsSingleEntity(Level level, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (stack.getCount() <= SUPERSTACK_SPLIT_THRESHOLD) {
            return;
        }
        ItemStack dropped = stack.split(stack.getCount());
        ItemEntity entity = new ItemEntity(level, x, y, z, dropped);
        entity.setDeltaMovement(
                level.random.triangle(0.0, 0.11485000171139836),
                level.random.triangle(0.2, 0.11485000171139836),
                level.random.triangle(0.0, 0.11485000171139836));
        level.addFreshEntity(entity);
        ci.cancel();
    }
}