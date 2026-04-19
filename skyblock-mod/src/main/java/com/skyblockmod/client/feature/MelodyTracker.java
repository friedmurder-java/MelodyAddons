package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MelodyTracker {

    private boolean inTerminal        = false;
    private boolean sentThreeQuarters = false;
    private int lastRow = -1;
    private List<String> selectedMessages = new ArrayList<>();

    private static final MelodyTracker INSTANCE = new MelodyTracker();
    public static MelodyTracker get() { return INSTANCE; }
    private MelodyTracker() {}

    public boolean isInTerminal() {
        if (inTerminal) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return false;
        return screen.getTitle().getString().toLowerCase().contains("click the button on time");
    }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
            if (inTerminal) onClose();
            return;
        }
        String title = screen.getTitle().getString();
        if (!title.toLowerCase().contains("click the button on time")) {
            if (inTerminal) onClose();
            return;
        }
        if (!inTerminal) {
            inTerminal = true;
            lastRow    = -1;
            selectMessages();
            onOpen();
        }
    }

    public void onSlotUpdate(String itemId, int slot) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.melodyMessages || !cfg.hasMelodyMessages()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (!title.toLowerCase().contains("click the button on time")) return;

        if (itemId.equals("minecraft:lime_terracotta")) {
            int row = slot / 9;
            if (row == lastRow) return;
            lastRow = row;

            int index = row - 2;
            if (index >= 0 && index < selectedMessages.size()) {
                String msg = selectedMessages.get(index) + " " + (index + 1) + "/4";
                sendPc(msg);
                if (index == 2) sentThreeQuarters = true;
            }
        }
    }

    private void selectMessages() {
        ModConfig cfg = ModConfig.get();
        List<String> pool = new ArrayList<>(cfg.melodyMsgPool);
        Collections.shuffle(pool);
        selectedMessages = new ArrayList<>(pool.subList(0, Math.min(3, pool.size())));
    }

    private void onOpen() {
        ModConfig cfg = ModConfig.get();
        if (!cfg.melodyMessages) return;
        if (ModConfig.nonEmpty(cfg.melodyMsgStart)) sendPc(cfg.melodyMsgStart);
    }

    private void onClose() {
        if (sentThreeQuarters) {
            ModConfig cfg = ModConfig.get();
            if (cfg.melodyMessages && ModConfig.nonEmpty(cfg.melodyMsgClose))
                sendPc(cfg.melodyMsgClose);
        }
        inTerminal        = false;
        lastRow           = -1;
        sentThreeQuarters = false;
        selectedMessages.clear();
    }

    private void sendPc(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.getNetworkHandler() != null)
            mc.getNetworkHandler().sendChatCommand("pc " + message);
    }
}