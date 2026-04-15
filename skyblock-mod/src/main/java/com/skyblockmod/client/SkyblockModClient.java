package com.skyblockmod.client;

import com.skyblockmod.client.config.ModConfig;
import com.skyblockmod.client.gui.ChatTriggerScreen;
import com.skyblockmod.client.gui.ModMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyblockModClient implements ClientModInitializer {

    public static final String MOD_ID = "mymod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean openMenuNextTick     = false;
    private static boolean openTriggersNextTick = false;

    @Override
    public void onInitializeClient() {
        ModConfig.get();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("oskar")
                    .executes(context -> {
                        openMenuNextTick = true;
                        return 1;
                    })
                    .then(ClientCommandManager.literal("chattriggers").executes(context -> {
                        openTriggersNextTick = true;
                        return 1;
                    }))
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openMenuNextTick && client.currentScreen == null) {
                openMenuNextTick = false;
                client.setScreen(new ModMenuScreen());
            }
            if (openTriggersNextTick && client.currentScreen == null) {
                openTriggersNextTick = false;
                client.setScreen(new ChatTriggerScreen());
            }
        });

        LOGGER.info("[MyMod] Loaded! Type /oskar to open settings.");
    }
}