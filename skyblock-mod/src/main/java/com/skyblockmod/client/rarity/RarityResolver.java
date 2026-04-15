package com.skyblockmod.client.rarity;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;

import java.util.List;

/**
 * Resolves the Hypixel Skyblock rarity of a dropped ItemEntity by inspecting
 * the item's custom name and lore lines for rarity keywords.
 *
 * On Hypixel Skyblock, rarity is shown as the LAST lore line, e.g.:
 *   "§f§lCOMMON"  /  "§a§lUNCOMMON"  /  "§9§lRARE"  etc.
 */
public class RarityResolver {

    /**
     * Returns the SkyblockRarity for the given dropped item entity,
     * or null if it cannot be determined (e.g. vanilla item with no lore).
     */
    public static SkyblockRarity resolve(ItemEntity entity) {
        ItemStack stack = entity.getStack();
        return resolveFromStack(stack);
    }

    /**
     * Resolves rarity directly from an ItemStack.
     */
    public static SkyblockRarity resolveFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;

        // Check lore lines (Hypixel puts rarity as last lore line)
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            List<Text> lines = lore.lines();
            // Iterate in reverse – rarity is usually the last line
            for (int i = lines.size() - 1; i >= 0; i--) {
                String plain = lines.get(i).getString().toUpperCase();
                SkyblockRarity found = matchRarity(plain);
                if (found != null) return found;
            }
        }

        // Fallback: check custom name
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            String name = stack.get(DataComponentTypes.CUSTOM_NAME).getString().toUpperCase();
            return matchRarity(name);
        }

        return null;
    }

    /**
     * Matches a rarity keyword inside a string (stripped of formatting codes).
     */
    private static SkyblockRarity matchRarity(String text) {
        // Strip Minecraft color/format codes (§X)
        String stripped = text.replaceAll("§[0-9A-FK-OR]", "").trim();

        // Check longest names first to avoid partial matches
        if (stripped.contains("VERY SPECIAL")) return SkyblockRarity.VERY_SPECIAL;
        if (stripped.contains("LEGENDARY"))    return SkyblockRarity.LEGENDARY;
        if (stripped.contains("UNCOMMON"))     return SkyblockRarity.UNCOMMON;
        if (stripped.contains("SPECIAL"))      return SkyblockRarity.SPECIAL;
        if (stripped.contains("DIVINE"))       return SkyblockRarity.DIVINE;
        if (stripped.contains("MYTHIC"))       return SkyblockRarity.MYTHIC;
        if (stripped.contains("COMMON"))       return SkyblockRarity.COMMON;
        if (stripped.contains("EPIC"))         return SkyblockRarity.EPIC;
        if (stripped.contains("RARE"))         return SkyblockRarity.RARE;

        return null;
    }
}
