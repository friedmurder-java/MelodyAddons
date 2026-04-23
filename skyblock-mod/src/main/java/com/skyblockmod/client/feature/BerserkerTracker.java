package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BerserkerTracker {

    private static final Pattern DEVICE_DONE = Pattern.compile(
            "(.+) completed a device! \\(\\d+/7\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SEVEN_OF_SEVEN = Pattern.compile(
            "\\(7/7\\)"
    );
    private static final String P3_START   = "Who dares trespass into my domain";
    private static final String RUN_START  = "Starting in 1 second";
    private static final int    SCAN_DELAY = 60;

    private boolean inP3              = false;
    private boolean inS1              = false;
    private boolean berserkerDone     = false;
    private boolean notDoneSent       = false;
    private boolean doneMsgSent       = false;
    private int     sevenOfSevenCount = 0;
    private int     scanCountdown     = -1;

    private final List<String> berserkerNames = new ArrayList<>();
    private String lastActiveBerserker = null;

    private static final BerserkerTracker INSTANCE = new BerserkerTracker();
    public static BerserkerTracker get() { return INSTANCE; }
    private BerserkerTracker() {}

    public void tick() {
        if (scanCountdown > 0) {
            scanCountdown--;
            if (scanCountdown == 0) {
                doScan();
            }
        }
    }

    public void onChatMessage(String raw) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.berserkerTracker) return;

        if (raw.contains(RUN_START)) {
            berserkerNames.clear();
            lastActiveBerserker = null;
            scanCountdown = SCAN_DELAY;
            return;
        }

        if (raw.contains(P3_START)) {
            inP3              = true;
            inS1              = true;
            berserkerDone     = false;
            notDoneSent       = false;
            doneMsgSent       = false;
            sevenOfSevenCount = 0;
            lastActiveBerserker = null;
            return;
        }

        if (!inP3) return;

        Matcher mDevice = DEVICE_DONE.matcher(raw);
        if (mDevice.find()) {
            String playerName = mDevice.group(1).trim();
            if (isKnownBerserker(playerName)) {
                inS1= true;
                berserkerDone = true;
                lastActiveBerserker = playerName;
                if (!doneMsgSent && ModConfig.nonEmpty(cfg.berserkerMsgStart)) {
                    sendFormattedPc(cfg.berserkerMsgStart, playerName);
                    doneMsgSent = true;
                }
            }
            return;
        }

        if (SEVEN_OF_SEVEN.matcher(raw).find()) {
            sevenOfSevenCount++;
            if (sevenOfSevenCount == 1) {
                inS1 = false;
                if (!berserkerDone && !notDoneSent && ModConfig.nonEmpty(cfg.berserkerMsgNotDone)) {
                    sendFormattedPc(cfg.berserkerMsgNotDone, getBestBerserkerName());
                    notDoneSent = true;
                }
            } else if (sevenOfSevenCount == 2) {
                if (!berserkerDone && ModConfig.nonEmpty(cfg.berserkerMsgS4)) {
                    sendFormattedPc(cfg.berserkerMsgS4, getBestBerserkerName());
                }
                inP3 = false;
            }
        }
    }

    private void doScan() {
        berserkerNames.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;

        Collection<PlayerListEntry> entries = mc.getNetworkHandler().getPlayerList();
        for (PlayerListEntry entry : entries) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;
            String line = displayName.getString();
            String stripped = line.replaceAll("§[0-9a-fk-orx]", "");

            if (stripped.contains("Berserk")) {
                String name = extractName(entry);
                if (name != null && !berserkerNames.contains(name)) {
                    berserkerNames.add(name);
                }
            }
        }

        if (mc.player != null) {
            if (berserkerNames.isEmpty()) {
                mc.player.sendMessage(Text.literal("§d[MelodyAddons] §7No berserker found in tab list"), false);
            } else {
                mc.player.sendMessage(Text.literal("§d[MelodyAddons] §fBerserker(s) detected: §6" + String.join(", ", berserkerNames)), false);
            }
        }
    }

    private String extractName(PlayerListEntry entry) {
        // Try profile name first but skip if it looks like an internal entry
        if (entry.getProfile() != null && entry.getProfile().name() != null) {
            String name = entry.getProfile().name();
            if (!name.startsWith("!") && !name.startsWith("#") && name.length() > 2) {
                return name;
            }
        }
        // Fallback: extract from display name
        // Format: "[429] PlayerName ⚔ (Berserk X)"
        Text displayName = entry.getDisplayName();
        if (displayName == null) return null;
        String line = displayName.getString().replaceAll("§[0-9a-fk-orx]", "").trim();
        // Remove rank like [429]
        line = line.replaceAll("^\\[\\d+\\]\\s*", "").trim();
        // Take first word as player name
        String[] parts = line.split("\\s+");
        if (parts.length > 0 && !parts[0].isEmpty()) return parts[0];
        return null;
    }

    private boolean isKnownBerserker(String playerName) {
        for (String name : berserkerNames) {
            if (name.equalsIgnoreCase(playerName)) return true;
        }
        return false;
    }

    private String getBestBerserkerName() {
        if (lastActiveBerserker != null) return lastActiveBerserker;
        if (!berserkerNames.isEmpty()) return berserkerNames.get(0);
        return "Berserker";
    }

    private void sendFormattedPc(String message, String name) {
        String finalMsg = message.replace("(bers)", name);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.getNetworkHandler() != null)
            mc.getNetworkHandler().sendChatCommand("pc " + finalMsg);
    }
}