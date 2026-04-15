package com.skyblockmod.client.rarity;

/**
 * Hypixel Skyblock item rarities with their corresponding glow colors.
 * Colors are in ARGB format (0xAARRGGBB).
 */
public enum SkyblockRarity {

    COMMON     ("COMMON",     0xFFFFFFFF), // White
    UNCOMMON   ("UNCOMMON",   0xFF55FF55), // Green
    RARE       ("RARE",       0xFF5555FF), // Blue
    EPIC       ("EPIC",       0xFFAA00AA), // Purple
    LEGENDARY  ("LEGENDARY",  0xFFFFAA00), // Gold/Orange
    MYTHIC     ("MYTHIC",     0xFFFF55FF), // Pink
    DIVINE     ("DIVINE",     0xFF55FFFF), // Aqua
    SPECIAL    ("SPECIAL",    0xFFFF5555), // Red
    VERY_SPECIAL("VERY SPECIAL", 0xFFFF5555); // Red

    public final String displayName;
    public final int color; // ARGB

    SkyblockRarity(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    /** Extract just the RGB portion (no alpha) for rendering. */
    public int getRGB() {
        return color & 0x00FFFFFF;
    }

    /** Extract the red component (0-1 float). */
    public float getRed() {
        return ((color >> 16) & 0xFF) / 255.0f;
    }

    /** Extract the green component (0-1 float). */
    public float getGreen() {
        return ((color >> 8) & 0xFF) / 255.0f;
    }

    /** Extract the blue component (0-1 float). */
    public float getBlue() {
        return (color & 0xFF) / 255.0f;
    }
}
