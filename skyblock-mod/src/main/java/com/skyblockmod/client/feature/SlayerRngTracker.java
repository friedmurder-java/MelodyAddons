package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlayerRngTracker {

    private static final Pattern DROP_CHAT_FULL = Pattern.compile(
            "RARE DROP!\\s+(.+?)\\s+\\(([\\d,]+)\\s+RNG Meter\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DROP_CHAT_SHORT = Pattern.compile(
            "RNG METER\\s*-\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern XP_RESET = Pattern.compile(
            "RNG Meter was reset\\.\\s+You had ([\\d,]+) XP",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LORE_PROGRESS = Pattern.compile(
            "([\\d,]+(?:\\.\\d+)?(?:[kKmM])?)\\s*/\\s*([\\d,]+(?:\\.\\d+)?(?:[kKmM])?)",
            Pattern.CASE_INSENSITIVE
    );

    // Pending drop state
    private String pendingItem  = null;
    private long   pendingXpMax = -1;
    private long   pendingXpHad = -1;
    private int    pendingTicks = 0;
    private static final int EXPIRY_TICKS = 40;

    // Cached meter state
    private long   cachedXp    = -1;
    private long   cachedMaxXp = -1;
    private String cachedItem  = null;
    private String cachedSlayer = null;

    private static final SlayerRngTracker INSTANCE = new SlayerRngTracker();
    public static SlayerRngTracker get() { return INSTANCE; }

    private SlayerRngTracker() {
        ModConfig cfg = ModConfig.get();
        cachedXp     = cfg.slayerRngCachedXp;
        cachedMaxXp  = cfg.slayerRngCachedMaxXp;
        cachedItem   = cfg.slayerRngCachedItem;
        cachedSlayer = cfg.slayerRngCachedSlayer;
    }

    public long getCachedXp()       { return cachedXp; }
    public long getCachedMaxXp()    { return cachedMaxXp; }
    public String getCachedSlayer() { return cachedSlayer; }

    private void saveToConfig() {
        ModConfig cfg = ModConfig.get();
        cfg.slayerRngCachedXp     = cachedXp;
        cfg.slayerRngCachedMaxXp  = cachedMaxXp;
        cfg.slayerRngCachedItem   = cachedItem   != null ? cachedItem   : "";
        cfg.slayerRngCachedSlayer = cachedSlayer != null ? cachedSlayer : "";
        cfg.save();
    }

    public void tick() {
        // Pending drop expiry
        if (pendingTicks > 0) {
            pendingTicks--;
            if (pendingTicks == 0) {
                if (pendingItem != null) tryEmit();
                clearPending();
            }
        }

        // Read Slayer RNG Meter GUI
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (!title.toLowerCase().contains("slayer rng meter")) return;

        ScreenHandler handler = screen.getScreenHandler();

        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (stack.isEmpty()) continue;

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;

            boolean isSelected = false;
            boolean nextLineIsItem = false;
            long xp = -1, maxXp = -1;
            String slayerName = null;
            String itemName = null;

            java.util.List<Text> lines = lore.lines();
            // Second lore line is the slayer type
            if (lines.size() >= 2) {
                slayerName = lines.get(1).getString().trim();
            }

            for (Text line : lines) {
                String plain = line.getString();

                if (plain.trim().equalsIgnoreCase("Selected Drop")) {
                    isSelected = true;
                    nextLineIsItem = true;
                    continue;
                }
                if (nextLineIsItem) {
                    itemName = plain.trim();
                    nextLineIsItem = false;
                    continue;
                }

                Matcher m = LORE_PROGRESS.matcher(plain);
                if (m.find()) {
                    xp    = parseNumber(m.group(1));
                    maxXp = parseNumber(m.group(2));
                }
            }

            if (isSelected && xp >= 0) {
                cachedXp     = xp;
                cachedMaxXp  = maxXp;
                cachedItem   = itemName != null ? itemName : stack.getName().getString();
                cachedSlayer = slayerName;
                saveToConfig();
                return;
            }
        }

        // Fallback: first item with progress
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (stack.isEmpty()) continue;

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;

            java.util.List<Text> lines = lore.lines();
            String slayerName = lines.size() >= 2 ? lines.get(1).getString().trim() : null;

            for (Text line : lines) {
                String plain = line.getString();
                Matcher m = LORE_PROGRESS.matcher(plain);
                if (m.find()) {
                    cachedXp     = parseNumber(m.group(1));
                    cachedMaxXp  = parseNumber(m.group(2));
                    cachedItem   = stack.getName().getString();
                    cachedSlayer = slayerName;
                    saveToConfig();
                    return;
                }
            }
        }

        // Menü offen aber kein Item selected
        cachedXp     = 0;
        cachedMaxXp  = 0;
        cachedItem   = null;
        cachedSlayer = null;
        saveToConfig();
    }

    public void onChatMessage(String raw) {
        if (!ModConfig.get().slayerRngTracker) return;

        if (raw.contains("You reset your selected drop for your")) {
            cachedXp     = 0;
            cachedMaxXp  = 0;
            cachedItem   = null;
            cachedSlayer = null;
            saveToConfig();
            return;
        }

        Matcher mFull = DROP_CHAT_FULL.matcher(raw);
        if (mFull.find()) {
            pendingItem  = mFull.group(1).trim();
            pendingXpMax = parseNumber(mFull.group(2));
            pendingXpHad = -1;
            pendingTicks = EXPIRY_TICKS;
            tryEmit();
            return;
        }

        Matcher mShort = DROP_CHAT_SHORT.matcher(raw);
        if (mShort.find()) {
            pendingItem  = mShort.group(1).trim();
            pendingXpMax = -1;
            pendingXpHad = -1;
            pendingTicks = EXPIRY_TICKS;
            return;
        }

        Matcher mReset = XP_RESET.matcher(raw);
        if (mReset.find() && pendingItem != null) {
            pendingXpHad = parseNumber(mReset.group(1));
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

        cachedXp    = 0;
        cachedMaxXp = 0;
        cachedItem  = null;
        saveToConfig();
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

    private static long parseNumber(String s) {
        try {
            s = s.replace(",", "").trim();
            if (s.endsWith("M") || s.endsWith("m")) {
                return (long)(Double.parseDouble(s.substring(0, s.length()-1)) * 1_000_000);
            }
            if (s.endsWith("k") || s.endsWith("K")) {
                return (long)(Double.parseDouble(s.substring(0, s.length()-1)) * 1000);
            }
            if (s.contains(".")) {
                return (long) Double.parseDouble(s);
            }
            return Long.parseLong(s);
        } catch (NumberFormatException e) { return -1; }
    }

    private static String fmt(long n) { return String.format("%,d", n); }
}