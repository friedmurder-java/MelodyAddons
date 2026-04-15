package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.regex.Pattern;

public class CocoonAlert {

    private static final Pattern COCOON_PATTERN = Pattern.compile(
            "cocooned? (your|the) (slayer )?boss",
            Pattern.CASE_INSENSITIVE
    );

    private static final int DISPLAY_TICKS = 60;
    private int ticksLeft = 0;

    private static final CocoonAlert INSTANCE = new CocoonAlert();
    public static CocoonAlert get() { return INSTANCE; }
    private CocoonAlert() {}

    public void tick() {
        if (ticksLeft > 0) ticksLeft--;
    }

    public void onChatMessage(String raw) {
        if (!ModConfig.get().cocoonAlert) return;
        if (COCOON_PATTERN.matcher(raw).find()) {
            ticksLeft = DISPLAY_TICKS;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }

    public void render(DrawContext ctx, int screenWidth, int screenHeight) {
        if (!ModConfig.get().cocoonAlert || ticksLeft <= 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        if (tr == null) return;

        float alpha = ticksLeft < 20 ? (ticksLeft / 20.0f) : 1.0f;
        int a = (int)(alpha * 255) & 0xFF;
        int redA   = (a << 24) | (0xFF << 16) | 0x0000;
        int shadow = (a << 24) | 0x3F0000;

        String line1 = "COCOONED!";
        float scale = 5.0f;
        int textW = tr.getWidth(line1);

        float x = (screenWidth  - textW  * scale) / 2f;
        float y = (screenHeight - tr.fontHeight * scale) / 2f;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(tr, line1, 1, 1, shadow, false);
        ctx.drawText(tr, line1, 0, 0, redA, false);
        ctx.getMatrices().popMatrix();
    }
}