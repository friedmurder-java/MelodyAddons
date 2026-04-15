package com.skyblockmod.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

// /oskar is now handled via Fabric client command API in SkyblockModClient
@Mixin(ClientPlayNetworkHandler.class)
public class ChatSendMixin {
}
