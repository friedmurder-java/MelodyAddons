package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dungeon RNG Drop Tracker
 *
 * Reads the RNG Meter % directly from the tab list each tick, then the
 * moment a drop chat message fires it snaps the current % and prints:
 *   [MyMod] ✦ Hyperion dropped! You were at 84.7%.
 *
 * No expiry window needed — tab is always up to date.
 */
public class DungeonRngTracker {

    // ── Tab-list patterns ─────────────────────────────────────────────────────

    /** "RNG Meter: 84.7% (Hyperion)"  or  "RNG Meter: 84.7%" */
    private static final Pattern TAB_RNG_FULL = Pattern.compile(
        "RNG Meter[:\\s]+([\\d.]+)%(?:\\s*\\((.+?)\\))?",
        Pattern.CASE_INSENSITIVE
    );

    /** Fallback: "Hyperion   84.7%" */
    private static final Pattern TAB_RNG_ITEM = Pattern.compile(
        "^(.+?)\\s{2,}([\\d.]+)%\\s*$"
    );

    // ── Chat patterns ─────────────────────────────────────────────────────────

    /** "RARE DROP! Hyperion (5,000,000 RNG Meter)" */
    private static final Pattern DROP_CHAT_FULL = Pattern.compile(
        "RARE DROP!\\s+(.+?)\\s+\\([\\d,]+\\s+RNG Meter\\)",
        Pattern.CASE_INSENSITIVE
    );

    /** "RNG METER - Hyperion" */
    private static final Pattern DROP_CHAT_SHORT = Pattern.compile(
        "RNG METER\\s*-\\s*(.+)",
        Pattern.CASE_INSENSITIVE
    );

    // ── State ─────────────────────────────────────────────────────────────────

    private double cachedPct  = -1;
    private String cachedItem = null;

    // Singleton
    private static final DungeonRngTracker INSTANCE = new DungeonRngTracker();
    public static DungeonRngTracker get() { return INSTANCE; }
    private DungeonRngTracker() {}

    // ── Tick ─────────────────────────────────────────────────────────────────

    public void tick() {
        refreshTabCache();
    }

    // ── Chat hook ─────────────────────────────────────────────────────────────

    public void onChatMessage(String raw) {
        if (!ModConfig.get().dungeonRngTracker) return;

        Matcher mFull  = DROP_CHAT_FULL.matcher(raw);
        Matcher mShort = DROP_CHAT_SHORT.matcher(raw);

        if (mFull.find())  { handleDrop(mFull.group(1).trim());  return; }
        if (mShort.find()) { handleDrop(mShort.group(1).trim()); }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void handleDrop(String itemFromChat) {
        refreshTabCache(); // one final refresh at drop moment

        String itemName = (cachedItem != null && !cachedItem.isEmpty())
                ? cachedItem : itemFromChat;

        StringBuilder msg = new StringBuilder();
        msg.append("§b[MyMod] §e✦ §6").append(itemName).append(" §edropped! ");

        if (cachedPct >= 0) {
            msg.append(String.format("§7You were at §a%.1f%%§7.", cachedPct));
        }

        sendClientMessage(msg.toString());
    }

    private void refreshTabCache() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;

        Collection<PlayerListEntry> entries = mc.getNetworkHandler().getPlayerList();
        for (PlayerListEntry entry : entries) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;
            String line = displayName.getString();

            Matcher m1 = TAB_RNG_FULL.matcher(line);
            if (m1.find()) {
                cachedPct  = parseDouble(m1.group(1));
                if (m1.group(2) != null) cachedItem = m1.group(2).trim();
                return;
            }

            Matcher m2 = TAB_RNG_ITEM.matcher(line.trim());
            if (m2.find()) {
                double pct      = parseDouble(m2.group(2));
                String candidate = m2.group(1).trim();
                if (pct > 0 && pct <= 100 && candidate.length() > 2) {
                    cachedPct  = pct;
                    cachedItem = candidate;
                    return;
                }
            }
        }
    }

    private void sendClientMessage(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal(text), false);
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.replace(",", "")); }
        catch (NumberFormatException e) { return -1; }
    }
}
