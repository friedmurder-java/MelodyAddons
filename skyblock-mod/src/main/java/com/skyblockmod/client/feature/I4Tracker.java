package com.skyblockmod.client.feature;

import com.skyblockmod.client.config.ModConfig;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * I4 Tracker - Tracks when Berserker completes a device in Floor 7/M7 Phase 3 Section 1
 *
 * Detects messages like: "Berserker completed a device (3/7)"
 * Only activates in Phase 3, Section 1
 * Sends customizable party chat message
 */
public class I4Tracker {

    private static final Logger LOGGER = LoggerFactory.getLogger("I4Tracker");
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Regex patterns
    private static final Pattern SECTION_COMPLETE_REGEX = Pattern.compile(".*\(7/7\)$");
    private static final Pattern DEVICE_COMPLETION_REGEX = Pattern.compile(
        "^\[BOSS\] (.+?): completed a device \((\d+)/7\).*",
        Pattern.CASE_INSENSITIVE
    );

    // State tracking
    private int currentSection = 1;
    private String berserkerName = null;
    private final Set<String> detectedBerserkersInRun = new HashSet<>();

    // Singleton
    private static final I4Tracker INSTANCE = new I4Tracker();
    public static I4Tracker get() { return INSTANCE; }
    private I4Tracker() {
        initializeListeners();
    }

    /**
     * Initialize chat and world load listeners
     */
    private void initializeListeners() {
        // Listen for chat messages (section completion and device completion)
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, handler) -> {
            String chatText = message.getString();

            // Check for section completion
            if (SECTION_COMPLETE_REGEX.matcher(chatText).matches()) {
                onSectionComplete(chatText);
            }

            // Check for device completion
            Matcher deviceMatcher = DEVICE_COMPLETION_REGEX.matcher(chatText);
            if (deviceMatcher.find()) {
                onDeviceCompletion(deviceMatcher);
            }
        });

        // Reset on world load
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            onWorldLoad();
        });
    }

    /**
     * Called when a section is completed (7/7 message received)
     */
    private void onSectionComplete(String message) {
        currentSection++;
        LOGGER.info("[I4Tracker] Section completed! Moving to section: {}", currentSection);

        if (mc.player != null) {
            mc.player.sendMessage(
                Text.literal("§b[I4Tracker] §fSection §6" + (currentSection - 1) + " §fcompleted! Now in section §6" + currentSection),
                false
            );
        }
    }

    /**
     * Called when a device is completed
     * Format: [BOSS] PlayerName: completed a device (3/7)
     */
    private void onDeviceCompletion(Matcher matcher) {
        // Only trigger in Phase 3, Section 1
        if (!canActivate()) {
            LOGGER.debug("[I4Tracker] Ignoring device completion - not in Phase 3 Section 1");
            return;
        }

        String playerName = matcher.group(1).trim();
        String deviceCount = matcher.group(2);

        LOGGER.info("[I4Tracker] Device completed by {}: ({}/7)", playerName, deviceCount);

        // Store berserker info
        berserkerName = playerName;
        detectedBerserkersInRun.add(playerName);

        // Send party chat with customizable message
        sendPartyMessage(playerName, deviceCount);

        // Send client message
        if (mc.player != null) {
            mc.player.sendMessage(
                Text.literal("§c[I4] §fBerserker §6" + playerName + " §fcompleted device (§e" + deviceCount + "§f/7)"),
                false
            );
        }
    }

    /**
     * Send customizable party chat message
     */
    private void sendPartyMessage(String playerName, String deviceCount) {
        if (mc.player == null) {
            return;
        }

        ModConfig cfg = ModConfig.get();
        String customMessage = cfg.i4TrackerMessage; // You'll add this to ModConfig

        if (customMessage == null || customMessage.isEmpty()) {
            // Default message if config not set
            customMessage = "I4! Berserker {player} completed device {count}/7";
        }

        // Replace placeholders
        String message = customMessage
            .replace("{player}", playerName)
            .replace("{count}", deviceCount)
            .replace("{section}", String.valueOf(currentSection));

        try {
            mc.player.networkHandler.sendChatCommand("pc " + message);
            LOGGER.info("[I4Tracker] Sent party message: {}", message);
        } catch (Exception e) {
            LOGGER.error("[I4Tracker] Failed to send party message", e);
        }
    }

    /**
     * Called when the player joins a world
     */
    private void onWorldLoad() {
        currentSection = 1;
        berserkerName = null;
        detectedBerserkersInRun.clear();
        LOGGER.info("[I4Tracker] World loaded - resetting section tracker");
    }

    /**
     * Check if we should activate (Phase 3, Section 1 only)
     */
    private boolean canActivate() {
        return currentSection == 1;
    }

    // ========== Getters ========== 

    public int getCurrentSection() {
        return currentSection;
    }

    public String getBerserkerName() {
        return berserkerName;
    }

    public Set<String> getDetectedBerserkersInRun() {
        return new HashSet<>(detectedBerserkersInRun);
    }

    public void setCurrentSection(int section) {
        this.currentSection = section;
        LOGGER.info("[I4Tracker] Section manually set to: {}", section);
    }
}