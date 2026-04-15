package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class ChatTriggerManager {

    private static final ChatTriggerManager INSTANCE = new ChatTriggerManager();
    public static ChatTriggerManager get() { return INSTANCE; }
    private ChatTriggerManager() {}

    private final List<ActiveAlert> active = new ArrayList<>();

    private static class ActiveAlert {
        String text;
        int ticksLeft;
        int totalTicks;
        ActiveAlert(String text, int ticks) {
            this.text      = text;
            this.ticksLeft = ticks;
            this.totalTicks = ticks;
        }
    }

    public void tick() {
        active.removeIf(a -> {
            a.ticksLeft--;
            return a.ticksLeft <= 0;
        });
    }

    public void onChatMessage(String raw) {
        ModConfig cfg = ModConfig.get();
        for (ModConfig.ChatTrigger t : cfg.chatTriggers) {
            if (ModConfig.nonEmpty(t.trigger) && raw.toLowerCase().contains(t.trigger.toLowerCase())) {
                if (ModConfig.nonEmpty(t.output)) {
                    active.add(new ActiveAlert(t.output, t.ticks > 0 ? t.ticks : 60));
                }
            }
        }
    }

    public void render(DrawContext ctx, int screenWidth, int screenHeight) {
        if (active.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        if (tr == null) return;

        int baseY = screenHeight / 2 - (active.size() * 12 * 5) / 2;

        for (int i = 0; i < active.size(); i++) {
            ActiveAlert alert = active.get(i);
            float alpha = alert.ticksLeft < 20 ? (alert.ticksLeft / 20.0f) : 1.0f;
            int a = (int)(alpha * 255) & 0xFF;
            int redA   = (a << 24) | (0xFF << 16) | 0x0000;
            int shadow = (a << 24) | 0x3F0000;

            float scale = 5.0f;
            int textW = tr.getWidth(alert.text);
            float x = (screenWidth - textW * scale) / 2f;
            float y = baseY + i * (int)(tr.fontHeight * scale + 8);

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x, y);
            ctx.getMatrices().scale(scale, scale);
            ctx.drawText(tr, alert.text, 1, 1, shadow, false);
            ctx.drawText(tr, alert.text, 0, 0, redA, false);
            ctx.getMatrices().popMatrix();
        }
    }
}