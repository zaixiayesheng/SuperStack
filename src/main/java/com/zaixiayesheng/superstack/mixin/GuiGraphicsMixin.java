package com.zaixiayesheng.superstack.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zaixiayesheng.superstack.CountFormat;
import com.zaixiayesheng.superstack.SuperStackConfig;
import com.zaixiayesheng.superstack.SuperStackLog;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 数量显示。
 *
 * <p>原版把数字右对齐画在格子右下角：{@code drawString(font, 数量, x + 19 - 2 - 字宽, y + 9)}。
 * 数量到四位数就超出 16 像素的格子，所以这里接管：把文字缩到最多 16 像素宽，仍贴右下角。
 *
 * <p>缩放坐标的算法与精妙背包一致：PoseStack 先 scale，再把屏幕坐标除以 scale，
 * 因为缩放会同时作用在坐标上。此前绕某个原点做 translate/scale 的写法在不同界面下会偏，
 * 这里统一按「格子原点 + 原版偏移」计算，和原版位置对齐。
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    /** 格子宽 16 像素，文字最多占这么宽。 */
    private static final float SLOT_WIDTH = 16.0F;
    /** 原版右对齐的落点：x + 19 - 2。 */
    private static final float RIGHT_OFFSET = 17.0F;
    /** 原版基线：y + 6 + 3。 */
    private static final float BASELINE_OFFSET = 9.0F;

    /** DEBUG 下只报第一次接管，避免每帧刷屏。 */
    @Unique
    private static boolean superstack$countLogged;

    @Redirect(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
            )
    )
    private int superstack$drawCount(GuiGraphics graphics, Font font, String text, int x, int y, int color,
                                     boolean dropShadow,
                                     Font itemFont, ItemStack stack, int itemX, int itemY,
                                     @Nullable String customText) {
        // 精妙界面里它会自己传 customText（数量超过 99 时传空串然后自绘缩写），那种交给它。
        // 鼠标拖动/悬停的堆叠没有 customText，仍然由我们缩写。
        int count = stack.getCount();
        if (count <= 99 || customText != null || text == null) {
            return graphics.drawString(font, text, x, y, color, dropShadow);
        }

        String display = SuperStackConfig.countStyle() == SuperStackConfig.CountDisplay.COMPACT
                ? CountFormat.compact(count)
                : CountFormat.exact(count);

        int width = font.width(display);
        if (width <= 0) {
            SuperStackLog.warn("数量文字量不出宽度（数量 " + count + "，文字 \"" + display + "\"），已按原样绘制。");
            return graphics.drawString(font, display, x, y, color, dropShadow);
        }

        float scale = Math.min(1.0F, SLOT_WIDTH / width);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (scale < 1.0F) {
            pose.scale(scale, scale, 1.0F);
        }
        int drawX = Math.round((itemX + RIGHT_OFFSET - width * scale) / scale);
        int drawY = Math.round((itemY + BASELINE_OFFSET + (1.0F / (scale * scale) - 1.0F)) / scale);

        if (!superstack$countLogged && SuperStackLog.isDebugEnabled()) {
            superstack$countLogged = true;
            SuperStackLog.debug("数量显示已接管：首次是 " + count + " 个，"
                    + "原版文字 \"" + text + "\" → 显示 \"" + display + "\"，"
                    + "字宽 " + width + "px 缩放到 " + String.format(java.util.Locale.ROOT, "%.3f", scale) + " 倍，"
                    + "落点 (" + drawX + ", " + drawY + ")。");
        }

        int result = graphics.drawString(font, display, drawX, drawY, color, dropShadow);
        pose.popPose();
        return result;
    }
}