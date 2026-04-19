package com.skyblockmod.client.mixin;

import com.skyblockmod.client.feature.BerserkerTracker;
import com.skyblockmod.client.feature.ChatTriggerManager;
import com.skyblockmod.client.feature.CocoonAlert;
import com.skyblockmod.client.feature.DungeonRngTracker;
import com.skyblockmod.client.feature.LeapAnnouncer;
import com.skyblockmod.client.feature.SlayerRngTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatReceiveMixin {

    @Inject(method = "onGameMessage", at = @At("TAIL"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        Text msg = packet.content();
        if (msg == null) return;
        String plain = msg.getString();
        SlayerRngTracker.get().onChatMessage(plain);
        DungeonRngTracker.get().onChatMessage(plain);
        CocoonAlert.get().onChatMessage(plain);
        ChatTriggerManager.get().onChatMessage(plain);
        LeapAnnouncer.get().onChatMessage(plain);
        BerserkerTracker.get().onChatMessage(plain);
    }
}