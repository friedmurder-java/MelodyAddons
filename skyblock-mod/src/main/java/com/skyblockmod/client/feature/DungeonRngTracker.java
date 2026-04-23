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
    private static final Pattern LORE_PROGRESS = Pattern.compile(
            "([\\d,]+(?:\\.\\d+)?(?:[kKmM])?)\\s*/\\s*([\\d,]+(?:\\.\\d+)?(?:[kKmM])?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LORE_FLOOR = Pattern.compile(
            "Catacombs\\s*\\(([MF]\\d+)\\)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TEAM_SCORE_CHAT = Pattern.compile(
            "Team Score:\\s*([\\d,]+)\\s*\\(S[+]?\\)",
            Pattern.CASE_INSENSITIVE
    );

    private long cachedXp;
    private long cachedMaxXp;
    private String cachedItem;
    private String cachedFloor;

    private static final DungeonRngTracker INSTANCE = new DungeonRngTracker();
    public static DungeonRngTracker get() { return INSTANCE; }

    private DungeonRngTracker() {
        ModConfig cfg = ModConfig.get();
        cachedXp    = cfg.rngCachedXp;
        cachedMaxXp = cfg.rngCachedMaxXp;
        cachedItem  = cfg.rngCachedItem;
        cachedFloor = cfg.rngCachedFloor;
    }

    public long getCachedXp()      { return cachedXp; }
    public long getCachedMaxXp()   { return cachedMaxXp; }
    public String getCachedFloor() { return cachedFloor; }

    private void saveToConfig() {
        ModConfig cfg = ModConfig.get();
        cfg.rngCachedXp    = cachedXp;
        cfg.rngCachedMaxXp = cachedMaxXp;
        cfg.rngCachedItem  = cachedItem  != null ? cachedItem  : "";
        cfg.rngCachedFloor = cachedFloor != null ? cachedFloor : "";
        cfg.save();
    }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) return;
        String title = screen.getTitle().getString();
        if (!title.toLowerCase().contains("catacombs rng meter")) return;

        ScreenHandler handler = screen.getScreenHandler();

        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (stack.isEmpty()) continue;

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;

            boolean isSelected = false;
            boolean nextLineIsItem = false;
            long xp = -1, maxXp = -1;
            String floor = null;
            String itemName = null;

            for (Text line : lore.lines()) {
                String plain = line.getString();

                // Floor erkennen
                Matcher mFloor = LORE_FLOOR.matcher(plain);
                if (mFloor.find()) floor = mFloor.group(1).toUpperCase();

                // "Selected Drop" → nächste Zeile ist der Item-Name
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

                // Progress lesen
                Matcher m = LORE_PROGRESS.matcher(plain);
                if (m.find()) {
                    xp    = parseNumber(m.group(1));
                    maxXp = parseNumber(m.group(2));
                }
            }

            if (isSelected && xp >= 0) {
                cachedXp    = xp;
                cachedMaxXp = maxXp;
                cachedItem  = itemName != null ? itemName : stack.getName().getString();
                cachedFloor = floor;
                saveToConfig();
                return;
            }
        }

        // Fallback: erstes Item mit Progress
        for (int i = 0; i < handler.slots.size(); i++) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (stack.isEmpty()) continue;

            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) continue;

            String floor = null;
            for (Text line : lore.lines()) {
                String plain = line.getString();
                Matcher mFloor = LORE_FLOOR.matcher(plain);
                if (mFloor.find()) floor = mFloor.group(1).toUpperCase();

                Matcher m = LORE_PROGRESS.matcher(plain);
                if (m.find()) {
                    cachedXp    = parseNumber(m.group(1));
                    cachedMaxXp = parseNumber(m.group(2));
                    cachedItem  = stack.getName().getString();
                    cachedFloor = floor;
                    saveToConfig();
                    return;
                }
            }
        }

        // Menü offen aber kein Item selected
        cachedXp    = 0;
        cachedMaxXp = 0;
        cachedItem  = null;
        cachedFloor = null;
        saveToConfig();
    }

    public void onChatMessage(String raw) {
        if (!ModConfig.get().dungeonRngTracker) return;

        if (raw.contains("You reset your selected drop for your")) {
            cachedXp    = 0;
            cachedMaxXp = 0;
            cachedItem  = null;
            cachedFloor = null;
            saveToConfig();
            return;
        }

        Matcher mScore = TEAM_SCORE_CHAT.matcher(raw);
        if (mScore.find() && cachedXp >= 0) {
            long score = parseNumber(mScore.group(1));
            double multiplier = ModConfig.get().dungeonRngBlessed ? 1.1 : 1.0;
            long gained = (long)(score * multiplier);
            cachedXp = cachedXp + gained;
            saveToConfig();
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
        cachedXp    = 0;
        cachedMaxXp = 0;
        cachedItem  = null;
        cachedFloor = null;
        saveToConfig();
    }

    private void sendClientMessage(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal(text), false);
    }

    private static String fmt(long n) { return String.format("%,d", n); }

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
}