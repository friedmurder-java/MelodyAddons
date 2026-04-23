package com.skyblockmod.client.gui;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ChatTriggerScreen extends Screen {

    private static final int BG_COLOR    = 0xCC0D0D0D;
    private static final int PANEL_COLOR = 0xFF1A1A2E;
    private static final int ACCENT      = 0xFF4A90D9;
    private static final int TEXT_COLOR  = 0xFFEEEEEE;
    private static final int SUBTEXT     = 0xFF888888;
    private static final int FIELD_BG    = 0xFF0A0A1A;
    private static final int FIELD_ON    = 0xFF4A90D9;
    private static final int FIELD_OFF   = 0xFF334466;

    private static final int PANEL_W  = 500;
    private static final int ROW_H    = 50;
    private static final int HEADER_H = 55;
    private static final int FOOTER_H = 36;
    private static final int PADDING  = 14;

    private int panelX, panelY, panelH;
    private int scrollOffset = 0;
    private int maxScroll    = 0;

    private final List<TextFieldWidget> triggerFields = new ArrayList<>();
    private final List<TextFieldWidget> outputFields  = new ArrayList<>();
    private final List<TextFieldWidget> tickFields    = new ArrayList<>();

    public ChatTriggerScreen() {
        super(Text.literal("Chat Triggers"));
    }

    @Override
    protected void init() {
        triggerFields.forEach(this::remove);
        outputFields.forEach(this::remove);
        tickFields.forEach(this::remove);
        triggerFields.clear();
        outputFields.clear();
        tickFields.clear();

        ModConfig cfg = ModConfig.get();
        int count = cfg.chatTriggers.size();
        int contentH = HEADER_H + FOOTER_H + count * ROW_H;

        panelH = Math.min(contentH, height - 20);
        panelX = (width - PANEL_W) / 2;
        panelY = (height - panelH) / 2;
        maxScroll = Math.max(0, contentH - panelH);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        rebuildFields();
    }

    private void rebuildFields() {
        triggerFields.forEach(this::remove);
        outputFields.forEach(this::remove);
        tickFields.forEach(this::remove);
        triggerFields.clear();
        outputFields.clear();
        tickFields.clear();

        ModConfig cfg = ModConfig.get();
        int y = panelY + HEADER_H;

        for (int i = 0; i < cfg.chatTriggers.size(); i++) {
            ModConfig.ChatTrigger t = cfg.chatTriggers.get(i);
            int iy = y - scrollOffset;

            // Trigger field - wider
            TextFieldWidget tf = new TextFieldWidget(textRenderer,
                    panelX + PADDING, iy + (ROW_H - 16) / 2, 155, 16, Text.literal("trigger"));
            tf.setMaxLength(64);
            tf.setText(t.trigger);
            tf.setPlaceholder(Text.literal("§8Trigger word..."));
            tf.setDrawsBackground(true);
            int idx = i;
            tf.setChangedListener(v -> { cfg.chatTriggers.get(idx).trigger = v; cfg.save(); });
            addDrawableChild(tf);
            triggerFields.add(tf);

            // Output field - wider
            TextFieldWidget of = new TextFieldWidget(textRenderer,
                    panelX + PADDING + 165, iy + (ROW_H - 16) / 2, 215, 16, Text.literal("output"));
            of.setMaxLength(64);
            of.setText(t.output);
            of.setPlaceholder(Text.literal("§8Display text..."));
            of.setDrawsBackground(true);
            of.setChangedListener(v -> { cfg.chatTriggers.get(idx).output = v; cfg.save(); });
            addDrawableChild(of);
            outputFields.add(of);

            // Ticks field
            TextFieldWidget tkf = new TextFieldWidget(textRenderer,
                    panelX + PADDING + 390, iy + (ROW_H - 16) / 2, 46, 16, Text.literal("ticks"));
            tkf.setMaxLength(5);
            tkf.setText(String.valueOf(t.ticks));
            tkf.setPlaceholder(Text.literal("§860"));
            tkf.setDrawsBackground(true);
            tkf.setChangedListener(v -> {
                try { cfg.chatTriggers.get(idx).ticks = Integer.parseInt(v); cfg.save(); }
                catch (NumberFormatException ignored) {}
            });
            addDrawableChild(tkf);
            tickFields.add(tkf);

            y += ROW_H;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, BG_COLOR);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, PANEL_COLOR);
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 3, ACCENT);

        ctx.enableScissor(panelX, panelY + 3, panelX + PANEL_W, panelY + panelH - FOOTER_H);

        // Header
        int hy = panelY + 8;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§bChat Triggers"),
                panelX + PANEL_W / 2, hy, TEXT_COLOR);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§8Trigger | Output text | Ticks (duration)"),
                panelX + PANEL_W / 2, hy + 14, SUBTEXT);
        ctx.fill(panelX + PADDING, hy + 26, panelX + PANEL_W - PADDING, hy + 27, 0xFF333355);

        // Column headers
        int chy = panelY + HEADER_H - 14;
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Trigger"), panelX + PADDING, chy, SUBTEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Output"), panelX + PADDING + 165, chy, SUBTEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Ticks"), panelX + PADDING + 390, chy, SUBTEXT);

        // Rows
        ModConfig cfg = ModConfig.get();
        int y = panelY + HEADER_H;
        for (int i = 0; i < cfg.chatTriggers.size(); i++) {
            int sy = y - scrollOffset;
            if (sy + ROW_H > panelY + 3 && sy < panelY + panelH - FOOTER_H) {
                if (i % 2 == 0) ctx.fill(panelX + PADDING, sy, panelX + PANEL_W - PADDING, sy + ROW_H - 2, 0x08FFFFFF);

                // Reposition fields
                if (i < triggerFields.size()) {
                    triggerFields.get(i).setY(sy + (ROW_H - 16) / 2);
                    triggerFields.get(i).visible = true;
                }
                if (i < outputFields.size()) {
                    outputFields.get(i).setY(sy + (ROW_H - 16) / 2);
                    outputFields.get(i).visible = true;
                }
                if (i < tickFields.size()) {
                    tickFields.get(i).setY(sy + (ROW_H - 16) / 2);
                    tickFields.get(i).visible = true;
                }

                // X button — right side, vertically centered
                int bx = panelX + PANEL_W - PADDING - 20;
                int by = sy + (ROW_H - 16) / 2;
                boolean hov = mouseX >= bx && mouseX <= bx + 18 && mouseY >= by && mouseY <= by + 16;
                ctx.fill(bx, by, bx + 18, by + 16, hov ? 0xFFAA3A3A : 0xFF7A2A2A);
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§fx"), bx + 9, by + 4, TEXT_COLOR);
            } else {
                if (i < triggerFields.size()) triggerFields.get(i).visible = false;
                if (i < outputFields.size())  outputFields.get(i).visible  = false;
                if (i < tickFields.size())    tickFields.get(i).visible    = false;
            }
            y += ROW_H;
        }

        ctx.disableScissor();

        // Footer — separate area below scissor
        ctx.fill(panelX, panelY + panelH - FOOTER_H, panelX + PANEL_W, panelY + panelH - FOOTER_H + 1, 0xFF333355);

        // + Add button centered
        int addX = panelX + PANEL_W / 2 - 35;
        int addY = panelY + panelH - FOOTER_H + 8;
        boolean addHov = mouseX >= addX && mouseX <= addX + 70 && mouseY >= addY && mouseY <= addY + 14;
        ctx.fill(addX, addY, addX + 70, addY + 14, addHov ? 0xFF4A9A4A : 0xFF3A7A3A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f+ Add"), addX + 35, addY + 3, TEXT_COLOR);

        // Footer text
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8Scroll  ·  ESC to close"),
                panelX + PANEL_W / 2, panelY + panelH - 12, SUBTEXT);

        if (maxScroll > 0) renderScrollbar(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderScrollbar(DrawContext ctx) {
        int totalH = panelH - FOOTER_H - HEADER_H;
        int barH = Math.max(20, totalH * totalH / (totalH + maxScroll));
        int barY = panelY + HEADER_H + (totalH - barH) * scrollOffset / maxScroll;
        int barX = panelX + PANEL_W - 4;
        ctx.fill(barX, panelY + HEADER_H, barX + 3, panelY + panelH - FOOTER_H, 0xFF222233);
        ctx.fill(barX, barY, barX + 3, barY + barH, 0xFF4A90D9);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        ModConfig cfg = ModConfig.get();
        int y = panelY + HEADER_H;

        // X buttons
        for (int i = 0; i < cfg.chatTriggers.size(); i++) {
            int sy = y - scrollOffset;
            int bx = panelX + PANEL_W - PADDING - 20;
            int by = sy + (ROW_H - 16) / 2;
            if ((int) click.x() >= bx && (int) click.x() <= bx + 18
                    && (int) click.y() >= by && (int) click.y() <= by + 16) {
                cfg.chatTriggers.remove(i);
                cfg.save();
                init();
                return true;
            }
            y += ROW_H;
        }

        // + Add button
        int addX = panelX + PANEL_W / 2 - 35;
        int addY = panelY + panelH - FOOTER_H + 8;
        if ((int) click.x() >= addX && (int) click.x() <= addX + 70
                && (int) click.y() >= addY && (int) click.y() <= addY + 14) {
            cfg.chatTriggers.add(new ModConfig.ChatTrigger("", "", 60));
            cfg.save();
            init();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 12));
        rebuildFields();
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}