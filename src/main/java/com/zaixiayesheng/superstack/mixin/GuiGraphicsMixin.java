package com.zaixiayesheng.superstack.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zaixiayesheng.superstack.CountFormat;
import com.zaixiayesheng.superstack.SuperStackConfig;
import com.zaixiayesheng.superstack.SuperStackLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * 数量显示。
 *
 * <p>原版是 {@code drawString(font, 数量文字, x + 19 - 2 - 字宽, y + 9, 白, 带阴影)} —— 右对齐、不缩放。
 * 两三位数还塞得下，四位数开始就戳出 16 像素的格子了，所以很多模组干脆缩写成 "1K"，
 * 但那样 1000~1999 全变成 "1K"，看不出具体多少。
 *
 * <p>这里按精妙背包（Sophisticated Backpacks）的路子来：
 *
 * <ul>
 *   <li>数量超过 99 才接管；</li>
 *   <li>文字按当前字体宽度缩放到正好塞进 16 像素（最多缩到 1 倍以下，不放大）；</li>
 *   <li>仍然贴着格子右下角对齐，缩小时补偿一点纵向偏移，看起来和原版同一个位置；</li>
 *   <li>具体显示成 {@code 1,234} / {@code 12.3k} 还是精确数字，由配置决定，
 *       不管哪种都能在提示框里看到精确数量。</li>
 * </ul>
 *
 * <p>这个类每帧都会跑，所以平时一行日志都不打：只有数量显示看起来不对、需要排查时，
 * 打开 DEBUG 等级才会看到首次接管的那个堆叠（格式化结果、缩放比例、落点）。
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    /** 格子宽 16 像素，文字最多占这么宽。 */
    private static final float SLOT_WIDTH = 16.0F;
    /** 原版右对齐的落点：x + 19 - 2。 */
    private static final float RIGHT_OFFSET = 17.0F;

    /** DEBUG 下只报第一次接管，避免每帧刷屏。 */
    @Unique
    private static boolean superstack$countLogged;

    @WrapOperation(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
            )
    )
    private int superstack$drawCount(GuiGraphics graphics, Font font, String text, int x, int y, int color,
                                     boolean dropShadow, Operation<Integer> original,
                                     Font itemFont, ItemStack stack, int itemX, int itemY,
                                     @Nullable String customText) {
        // 精妙系列的界面在数量超过 99 时会自己画缩写数量，再画一遍就叠字了
        if (superstack$isSophisticatedScreen()) {
            return original.call(graphics, font, text, x, y, color, dropShadow);
        }

        int count = stack.getCount();
        if (count <= 99 || customText != null || text == null) {
            return original.call(graphics, font, text, x, y, color, dropShadow);
        }

        String display = SuperStackConfig.countDisplay() == SuperStackConfig.CountDisplay.COMPACT
                ? CountFormat.compact(count)
                : CountFormat.exact(count);

        int width = font.width(display);
        if (width <= 0) {
            // 理论上到不了这里（count > 99 的文字不可能零宽），真到了就别缩放，按原样画
            SuperStackLog.warn("数量文字量不出宽度（数量 " + count + "，文字 \"" + display + "\"），已按原样绘制。");
            return original.call(graphics, font, display, x, y, color, dropShadow);
        }

        float scale = Math.min(1.0F, SLOT_WIDTH / width);
        // 缩小之后字变矮了，往下补一点，让它还是贴着格子底部
        float top = y + font.lineHeight * 0.5F * (1.0F - scale);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (scale < 1.0F) {
            pose.scale(scale, scale, 1.0F);
        }
        int drawX = Math.round((itemX + RIGHT_OFFSET) / scale - width);
        int drawY = Math.round(top / scale);

        if (!superstack$countLogged && SuperStackLog.isDebugEnabled()) {
            superstack$countLogged = true;
            SuperStackLog.debug("数量显示已接管：首次是 " + count + " 个，"
                    + "原版文字 \"" + text + "\" → 显示 \"" + display + "\"，"
                    + "字宽 " + width + "px 缩放到 " + String.format(Locale.ROOT, "%.3f", scale) + " 倍。");
        }

        int result = original.call(graphics, font, display, drawX, drawY, color, dropShadow);
        pose.popPose();
        return result;
    }

    /** 当前界面是不是精妙（背包/存储）的 GUI。按类名判断，没装精妙时永远是 false。 */
    @Unique
    private static boolean superstack$isSophisticatedScreen() {
        var screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return false;
        }
        return screen.getClass().getName().startsWith("net.p3pp3rf1y.sophisticated");
    }
}
