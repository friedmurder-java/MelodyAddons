package com.skyblockmod.client.mixin;

import com.skyblockmod.client.feature.ChatTriggerManager;
import com.skyblockmod.client.feature.CocoonAlert;
import com.skyblockmod.client.feature.DungeonRngTracker;
import com.skyblockmod.client.feature.MelodyTracker;
import com.skyblockmod.client.feature.SlayerRngTracker;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ClientTickMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        SlayerRngTracker.get().tick();
        DungeonRngTracker.get().tick();
        CocoonAlert.get().tick();
        MelodyTracker.get().tick();
        ChatTriggerManager.get().tick();
    }
}