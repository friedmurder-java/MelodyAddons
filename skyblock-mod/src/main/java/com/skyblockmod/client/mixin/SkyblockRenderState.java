package com.skyblockmod.client.mixin;

import com.skyblockmod.client.rarity.SkyblockRarity;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;

import java.util.WeakHashMap;

/**
 * Stores per-render-state rarity data.
 *
 * We use a WeakHashMap so entries are automatically cleaned up when
 * render state objects are garbage collected.
 */
public class SkyblockRenderState {

    private static final WeakHashMap<ItemEntityRenderState, SkyblockRarity> RARITY_MAP = new WeakHashMap<>();

    public static void setRarity(ItemEntityRenderState state, SkyblockRarity rarity) {
        if (rarity != null) {
            RARITY_MAP.put(state, rarity);
        } else {
            RARITY_MAP.remove(state);
        }
    }

    public static SkyblockRarity getRarity(ItemEntityRenderState state) {
        return RARITY_MAP.get(state);
    }
}
