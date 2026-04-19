package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BerserkerTracker {

    private static final Pattern DEVICE_DONE = Pattern.compile(
            "(.+) completed a device \\(\\d+/7\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SEVEN_OF_SEVEN = Pattern.compile(
            "\\(7/7\\)"
    );
    private static final String P3_START = "Who dares trespass into my domain";

    private boolean inP3              = false;
    private boolean inS1              = false;
    private boolean berserkerDone     = false;
    private boolean notDoneSent       = false;
    private boolean doneMsgSent       = false;
    private int     sevenOfSevenCount = 0;

    private static final BerserkerTracker INSTANCE = new BerserkerTracker();
    public static BerserkerTracker get() { return INSTANCE; }
    private BerserkerTracker() {}

    public void onChatMessage(String raw) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.berserkerTracker) return;

        // P3 starts
        if (raw.contains(P3_START)) {
            inP3              = true;
            inS1              = true;
            berserkerDone     = false;
            notDoneSent       = false;
            doneMsgSent       = false;
            sevenOfSevenCount = 0;
            return;
        }

        if (!inP3) return;

        // Device completed — only track in S1
        if (inS1) {
            Matcher mDevice = DEVICE_DONE.matcher(raw);
            if (mDevice.find()) {
                String playerName = mDevice.group(1).trim();
                if (isBerserker(playerName)) {
                    berserkerDone = true;
                    // Send start msg only when berserk completes device
                    if (!doneMsgSent && ModConfig.nonEmpty(cfg.berserkerMsgStart)) {
                        sendPc(cfg.berserkerMsgStart);
                        doneMsgSent = true;
                    }
                }
                return;
            }
        }

        // 7/7 check
        if (SEVEN_OF_SEVEN.matcher(raw).find()) {
            sevenOfSevenCount++;

            if (sevenOfSevenCount == 1) {
                // S1 ended
                inS1 = false;
                // Only send "not done" if berserk never did device
                if (!berserkerDone && !notDoneSent && ModConfig.nonEmpty(cfg.berserkerMsgNotDone)) {
                    sendPc(cfg.berserkerMsgNotDone);
                    notDoneSent = true;
                }
            } else if (sevenOfSevenCount == 2) {
                // S4 started — only send if berserk did NOT do device
                if (!berserkerDone && ModConfig.nonEmpty(cfg.berserkerMsgS4)) {
                    sendPc(cfg.berserkerMsgS4);
                }
                inP3 = false;
            }
        }
    }

    private boolean isBerserker(String playerName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return false;

        Collection<PlayerListEntry> entries = mc.getNetworkHandler().getPlayerList();
        for (PlayerListEntry entry : entries) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;
            String line = displayName.getString();
            if (line.contains("[B]") && line.contains(playerName)) return true;
        }
        return false;
    }

    private void sendPc(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.getNetworkHandler() != null)
            mc.getNetworkHandler().sendChatCommand("pc " + message);
    }
}