package com.skyblockmod.client.gui;

import com.skyblockmod.client.config.ModConfig;
import com.skyblockmod.client.feature.HeightHud;
import com.skyblockmod.client.feature.HudElement;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditorScreen extends Screen {

    private HudElement dragging = null;
    private float dragOffsetX, dragOffsetY;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dark overlay
        ctx.fill(0, 0, width, height, 0x88000000);

        // Render all HUD elements in editor mode
        HeightHud.get().render(ctx);

        // Render selection box around hovered/dragged element
        ModConfig cfg = ModConfig.get();
        HudElement el = cfg.getHudElement("height");
        if (cfg.heightHud) {
            renderSelectionBox(ctx, el, mouseX, mouseY);
        }

        // Instructions
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Drag to move  ·  Scroll to resize  ·  ESC to close"),
                width / 2, height - 20, 0xFFAAAAAA);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderSelectionBox(DrawContext ctx, HudElement el, int mouseX, int mouseY) {
        int mc2 = net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth("Height: 64");
        int boxW = (int)((mc2 + 12) * el.scale);
        int boxH = (int)((9 + 8) * el.scale);

        int px = (int) el.x;
        int py = (int) el.y;

        boolean hovered = mouseX >= px && mouseX <= px + boxW && mouseY >= py && mouseY <= py + boxH;
        int color = (dragging == el || hovered) ? 0xFF4A90D9 : 0x664A90D9;

        ctx.fill(px - 1, py - 1, px + boxW + 1, py, color);
        ctx.fill(px - 1, py + boxH, px + boxW + 1, py + boxH + 1, color);
        ctx.fill(px - 1, py, px, py + boxH, color);
        ctx.fill(px + boxW, py, px + boxW + 1, py + boxH, color);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            ModConfig cfg = ModConfig.get();
            HudElement el = cfg.getHudElement("height");
            if (cfg.heightHud && isOverElement(el, (int) click.x(), (int) click.y())) {
                dragging = el;
                dragOffsetX = (float) click.x() - el.x;
                dragOffsetY = (float) click.y() - el.y;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && dragging != null) {
            dragging = null;
            ModConfig.get().save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            dragging.x = (float) click.x() - dragOffsetX;
            dragging.y = (float) click.y() - dragOffsetY;
            // Clamp to screen
            dragging.x = Math.max(0, Math.min(width - 50, dragging.x));
            dragging.y = Math.max(0, Math.min(height - 20, dragging.y));
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ModConfig cfg = ModConfig.get();
        HudElement el = cfg.getHudElement("height");
        if (cfg.heightHud && isOverElement(el, (int) mouseX, (int) mouseY)) {
            el.scale = Math.max(0.5f, Math.min(4.0f, el.scale + (float) verticalAmount * 0.1f));
            cfg.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private boolean isOverElement(HudElement el, int mx, int my) {
        int textW = net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth("Height: 64");
        int boxW = (int)((textW + 12) * el.scale);
        int boxH = (int)((9 + 8) * el.scale);
        return mx >= el.x && mx <= el.x + boxW && my >= el.y && my <= el.y + boxH;
    }

    @Override
    public boolean shouldPause() { return false; }
}

