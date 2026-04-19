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

public class DungeonRngTracker {

    private static final Pattern DROP_CHAT_FULL = Pattern.compile(
            "RARE DROP!\\s+(.+?)\\s+\\(([\\d,]+)\\s+RNG Meter\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DROP_CHAT_SHORT = Pattern.compile(
            "RNG METER\\s*-\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LORE_SCORE = Pattern.compile(
            "Dungeon Score:\\s*([\\d,]+)\\s*/\\s*([\\d,]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TEAM_SCORE_CHAT = Pattern.compile(
            "Team Score:\\s*([\\d,]+)\\s*\\(S[+]?\\)",
            Pattern.CASE_INSENSITIVE
    );

    private long cachedXp    = -1;
    private long cachedMaxXp = -1;
    private String cachedItem = null;

    private static final DungeonRngTracker INSTANCE = new DungeonRngTracker();
    public static DungeonRngTracker get() { return INSTANCE; }
    private DungeonRngTracker() {}

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (!title.toLowerCase().contains("catacombs rng meter")) return;

        ScreenHandler handler = screen.getScreenHandler();

        // First pass: look for selected item
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (stack.isEmpty()) continue;

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;

            boolean isSelected = false;
            long xp = -1, maxXp = -1;

            for (Text line : lore.lines()) {
                String plain = line.getString();
                if (plain.contains("SELECTED") || plain.contains("Selected")) isSelected = true;
                Matcher m = LORE_SCORE.matcher(plain);
                if (m.find()) {
                    xp    = parseLong(m.group(1));
                    maxXp = parseLong(m.group(2));
                }
            }

            if (isSelected && xp >= 0) {
                cachedXp    = xp;
                cachedMaxXp = maxXp;
                cachedItem  = stack.getName().getString();
                return;
            }
        }

        // Second pass: fallback to first valid item
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (stack.isEmpty()) continue;

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;

            for (Text line : lore.lines()) {
                String plain = line.getString();
                Matcher m = LORE_SCORE.matcher(plain);
                if (m.find()) {
                    cachedXp    = parseLong(m.group(1));
                    cachedMaxXp = parseLong(m.group(2));
                    cachedItem  = stack.getName().getString();
                    return;
                }
            }
        }
    }

    public void onChatMessage(String raw) {
        if (!ModConfig.get().dungeonRngTracker) return;

        // Detect end-of-run score
        Matcher mScore = TEAM_SCORE_CHAT.matcher(raw);
        if (mScore.find() && cachedXp >= 0) {
            long score = parseLong(mScore.group(1));
            double multiplier = ModConfig.get().dungeonRngBlessed ? 1.1 : 1.0;
            long gained = (long)(score * multiplier);
            cachedXp = cachedXp + gained; // allow over 100%
            sendClientMessage(String.format(
                    "§b[MelodyAddons] §7RNG Meter: §6%s §7/ §6%s §7(§a+%s XP§7)",
                    fmt(cachedXp), fmt(cachedMaxXp), fmt(gained)));
            return;
        }

        Matcher mFull  = DROP_CHAT_FULL.matcher(raw);
        Matcher mShort = DROP_CHAT_SHORT.matcher(raw);

        if (mFull.find())  { handleDrop(mFull.group(1).trim());  return; }
        if (mShort.find()) { handleDrop(mShort.group(1).trim()); }
    }

    private void handleDrop(String itemFromChat) {
        String itemName = (cachedItem != null && !cachedItem.isEmpty()) ? cachedItem : itemFromChat;

        StringBuilder msg = new StringBuilder();
        msg.append("§b[MelodyAddons] §e✦ §6").append(itemName).append(" §edropped! ");

        if (cachedXp >= 0 && cachedMaxXp > 0) {
            double pct = (cachedXp * 100.0) / cachedMaxXp;
            msg.append(String.format("§7You were at §a%.1f%%§7 — ", pct));
            msg.append("§6").append(fmt(cachedXp))
                    .append(" §7/ §6").append(fmt(cachedMaxXp))
                    .append(" §7XP.");
        }

        sendClientMessage(msg.toString());
        // Reset after drop
        cachedXp = 0;
    }

    private void sendClientMessage(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal(text), false);
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.replace(",", "")); }
        catch (NumberFormatException e) { return -1; }
    }

    private static String fmt(long n) { return String.format("%,d", n); }
}