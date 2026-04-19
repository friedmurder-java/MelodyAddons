package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class HeightHud {

    private static final HeightHud INSTANCE = new HeightHud();
    public static HeightHud get() { return INSTANCE; }
    private HeightHud() {}

    public void render(DrawContext ctx) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.heightHud) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;

        HudElement el = cfg.getHudElement("height");
        TextRenderer tr = mc.textRenderer;

        int y = (int) mc.player.getY();
        String text = "Height: " + y;

        int textW = tr.getWidth(text);
        int textH = tr.fontHeight;

        float scale = el.scale;
        int px = (int) el.x;
        int py = (int) el.y;

        int padX = 6;
        int padY = 4;
        int boxW = (int)((textW + padX * 2) * scale);
        int boxH = (int)((textH + padY * 2) * scale);

        // Background
        ctx.fill(px, py, px + boxW, py + boxH, 0x55000000);
        // Border hint
        ctx.fill(px, py, px + boxW, py + 1, 0x44FFFFFF);
        ctx.fill(px, py + boxH - 1, px + boxW, py + boxH, 0x44FFFFFF);
        ctx.fill(px, py, px + 1, py + boxH, 0x44FFFFFF);
        ctx.fill(px + boxW - 1, py, px + boxW, py + boxH, 0x44FFFFFF);

        // Text
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(px + padX * scale, py + padY * scale);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTextWithShadow(tr, text, 0, 0, 0xFFFFFFFF);
        ctx.getMatrices().popMatrix();
    }
}