package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeapAnnouncer {

    private static final Pattern LEAPED_PATTERN = Pattern.compile(
            "You have teleported to (\\w{1,16})!",
            Pattern.CASE_INSENSITIVE
    );

    private static final LeapAnnouncer INSTANCE = new LeapAnnouncer();
    public static LeapAnnouncer get() { return INSTANCE; }
    private LeapAnnouncer() {}

    public void onChatMessage(String raw) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.leapAnnounce) return;
        if (!ModConfig.nonEmpty(cfg.leapAnnounceMsg)) return;

        Matcher m = LEAPED_PATTERN.matcher(raw);
        if (!m.find()) return;

        String player = m.group(1);
        String msg = cfg.leapAnnounceMsg.replace("(player)", player);
        sendPc(msg);
    }

    private void sendPc(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.getNetworkHandler() != null)
            mc.getNetworkHandler().sendChatCommand("pc " + message);
    }
}