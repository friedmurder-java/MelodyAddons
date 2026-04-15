package com.skyblockmod.client.mixin;

import com.skyblockmod.client.config.ModConfig;
import com.skyblockmod.client.rarity.RarityResolver;
import com.skyblockmod.client.rarity.SkyblockRarity;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRenderStateMixin {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void onUpdateRenderState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (!ModConfig.get().rarityGlow) return;

        SkyblockRarity rarity = RarityResolver.resolve(entity);
        if (rarity == null) return;

        // outlineColor is an int field on ItemEntityRenderState — set it to the rarity color
        state.outlineColor = rarity.color;
    }
}
