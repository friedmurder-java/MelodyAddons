package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slayer RNG Drop Tracker.
 *
 * Slayer drops use the two-message system:
 *   1. "RARE DROP! Scythe Blade (750,000 RNG Meter)"
 *   2. "Your RNG Meter was reset. You had 623,400 XP."
 *
 * We pair them with a short expiry window since they always arrive together.
 *
 * Output: [MyMod] ✦ Scythe Blade dropped! You were at 83.1% — 623,400 / 750,000 XP reset.
 */
public class SlayerRngTracker {

    private static final Pattern DROP_FULL = Pattern.compile(
        "RARE DROP!\\s+(.+?)\\s+\\(([\\d,]+)\\s+RNG Meter\\)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DROP_SHORT = Pattern.compile(
        "RNG METER\\s*-\\s*(.+)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern XP_RESET = Pattern.compile(
        "RNG Meter was reset\\.\\s+You had ([\\d,]+) XP",
        Pattern.CASE_INSENSITIVE
    );

    private String pendingItem  = null;
    private long   pendingXpMax = -1;
    private long   pendingXpHad = -1;
    private int    pendingTicks = 0;
    private static final int EXPIRY_TICKS = 40;

    private static final SlayerRngTracker INSTANCE = new SlayerRngTracker();
    public static SlayerRngTracker get() { return INSTANCE; }
    private SlayerRngTracker() {}

    public void tick() {
        if (pendingTicks > 0) {
            pendingTicks--;
            if (pendingTicks == 0) {
                if (pendingItem != null) tryEmit();
                clearPending();
            }
        }
    }

    public void onChatMessage(String raw) {
        if (!ModConfig.get().slayerRngTracker) return;

        Matcher mFull = DROP_FULL.matcher(raw);
        if (mFull.find()) {
            pendingItem   = mFull.group(1).trim();
            pendingXpMax  = parseLong(mFull.group(2));
            pendingXpHad  = -1;
            pendingTicks  = EXPIRY_TICKS;
            tryEmit();
            return;
        }

        Matcher mShort = DROP_SHORT.matcher(raw);
        if (mShort.find()) {
            pendingItem  = mShort.group(1).trim();
            pendingXpMax = -1;
            pendingXpHad = -1;
            pendingTicks = EXPIRY_TICKS;
            return;
        }

        Matcher mReset = XP_RESET.matcher(raw);
        if (mReset.find() && pendingItem != null) {
            pendingXpHad = parseLong(mReset.group(1));
            tryEmit();
        }
    }

    private void tryEmit() {
        if (pendingItem == null || pendingXpHad < 0) return;

        StringBuilder msg = new StringBuilder();
        msg.append("§b[MelodyAddons] §e✦ §6").append(pendingItem).append(" §edropped! ");

        if (pendingXpMax > 0) {
            double pct = (pendingXpHad * 100.0) / pendingXpMax;
            msg.append(String.format("§7You were at §a%.1f%%§7 — ", pct));
            msg.append("§6").append(fmt(pendingXpHad))
               .append(" §7/ §6").append(fmt(pendingXpMax))
               .append(" §7XP reset.");
        } else {
            msg.append("§6").append(fmt(pendingXpHad)).append(" §7XP reset.");
        }

        send(msg.toString());
        clearPending();
    }

    private void clearPending() {
        pendingItem  = null;
        pendingXpMax = -1;
        pendingXpHad = -1;
        pendingTicks = 0;
    }

    private void send(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal(text), false);
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.replace(",", "")); }
        catch (NumberFormatException e) { return -1; }
    }

    private static String fmt(long n) { return String.format("%,d", n); }
}
