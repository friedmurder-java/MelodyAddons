package com.skyblockmod.client;

import com.skyblockmod.client.config.ModConfig;
import com.skyblockmod.client.feature.DungeonRngTracker;
import com.skyblockmod.client.gui.ChatTriggerScreen;
import com.skyblockmod.client.gui.HudEditorScreen;
import com.skyblockmod.client.gui.ModMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyblockModClient implements ClientModInitializer {

    public static final String MOD_ID = "MelodyAddons";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean openMenuNextTick      = false;
    private static boolean openTriggersNextTick  = false;
    private static boolean openHudEditorNextTick = false;

    private static final String[] FLOORS = {"f1","f2","f3","f4","f5","f6","f7","m1","m2","m3","m4","m5","m6","m7"};

    @Override
    public void onInitializeClient() {
        ModConfig.get();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            java.util.function.Consumer<com.mojang.brigadier.builder.LiteralArgumentBuilder<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>> register = cmd -> {

                var runsCmd = ClientCommandManager.literal("runs");
                var runsTillCmd = ClientCommandManager.literal("runstill");

                for (String floor : FLOORS) {
                    final String floorName = floor;
                    runsCmd.then(ClientCommandManager.literal(floorName).executes(context -> {
                        sendRunsNeeded(floorName);
                        return 1;
                    }));
                    runsTillCmd.then(ClientCommandManager.literal(floorName).executes(context -> {
                        sendRunsNeeded(floorName);
                        return 1;
                    }));
                }

                dispatcher.register(cmd
                        .executes(context -> {
                            openMenuNextTick = true;
                            return 1;
                        })
                        .then(ClientCommandManager.literal("chattriggers").executes(context -> {
                            openTriggersNextTick = true;
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("ct").executes(context -> {
                            openTriggersNextTick = true;
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("hud").executes(context -> {
                            openHudEditorNextTick = true;
                            return 1;
                        }))
                        .then(runsCmd)
                        .then(runsTillCmd)
                );
            };

            register.accept(ClientCommandManager.literal("melodyaddons"));
            register.accept(ClientCommandManager.literal("ma"));
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
            if (openHudEditorNextTick && client.currentScreen == null) {
                openHudEditorNextTick = false;
                client.setScreen(new HudEditorScreen());
            }
        });

        LOGGER.info("[MelodyAddons] Loaded! Type /melodyaddons to open settings.");
    }

    private static void sendRunsNeeded(String floor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long xp        = DungeonRngTracker.get().getCachedXp();
        long maxXp     = DungeonRngTracker.get().getCachedMaxXp();
        String cachedFloor = DungeonRngTracker.get().getCachedFloor();

        if (xp < 0 || maxXp < 0) {
            mc.player.sendMessage(Text.literal("§b[MelodyAddons] §7No RNG Meter data — open §e/rng §7first!"), false);
            return;
        }

        if (xp == 0 && maxXp == 0) {
            mc.player.sendMessage(Text.literal("§b[MelodyAddons] §cNo item selected in RNG Meter — open §e/rng §cand select a drop!"), false);
            return;
        }

        if (cachedFloor != null && !cachedFloor.equalsIgnoreCase(floor)) {
            mc.player.sendMessage(Text.literal(String.format(
                    "§b[MelodyAddons] §cRNG data is for §e%s§c, not §e%s§c — open §e/rng §cto update!",
                    cachedFloor, floor.toUpperCase())), false);
            return;
        }

        double scorePerRun = ModConfig.get().dungeonRngBlessed ? 300 * 1.1 : 300.0;
        long remaining = maxXp - xp;

        if (remaining <= 0) {
            mc.player.sendMessage(Text.literal(
                    "§b[MelodyAddons] §aRNG Meter is already full! Drop guaranteed next run."), false);
            return;
        }

        double runsNeeded = Math.ceil(remaining / scorePerRun);
        double pct = (xp * 100.0) / maxXp;
        String floorDisplay = floor.toUpperCase();

        mc.player.sendMessage(Text.literal(String.format(
                "§b[MelodyAddons] §7%s RNG Meter: §6%s§7/§6%s §7(§a%.1f%%§7) — §eAbout §6%.0f %s runs §eneeded.",
                floorDisplay, fmt(xp), fmt(maxXp), pct, runsNeeded, floorDisplay
        )), false);
    }

    private static String fmt(long n) {
        return String.format("%,d", n);
    }
}