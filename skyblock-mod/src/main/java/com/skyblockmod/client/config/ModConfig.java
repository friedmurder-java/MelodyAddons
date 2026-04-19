package com.skyblockmod.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyblockmod.client.feature.HudElement;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {

    public boolean rarityGlow        = true;
    public boolean slayerRngTracker  = true;
    public boolean dungeonRngTracker = true;
    public boolean dungeonRngBlessed = true;
    public boolean cocoonAlert       = true;
    public boolean melodyMessages    = true;
    public boolean fullbright        = false;
    public boolean heightHud         = true;
    public boolean leapAnnounce = false;
    public boolean berserkerTracker  = false;

    public String  berserkerMsgStart  = "";
    public String  berserkerMsgNotDone = "";
    public String  berserkerMsgS4     = "";
    public String leapAnnounceMsg = "Leaped to {player}!";
    public String melodyMsgStart  = "";
    public String melodyMsgClose  = "";
    public List<String> melodyMsgPool = new ArrayList<>();

    public List<ChatTrigger> chatTriggers = new ArrayList<>();

    public Map<String, HudElement> hudElements = new HashMap<>();

    public static class ChatTrigger {
        public String trigger = "";
        public String output  = "";
        public int    ticks   = 60;

        public ChatTrigger() {}
        public ChatTrigger(String trigger, String output, int ticks) {
            this.trigger = trigger;
            this.output  = output;
            this.ticks   = ticks;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("melodyaddons.json");
    private static ModConfig INSTANCE;

    public static ModConfig get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    private static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig cfg = GSON.fromJson(r, ModConfig.class);
                if (cfg != null) {
                    if (cfg.melodyMsgPool  == null) cfg.melodyMsgPool  = new ArrayList<>();
                    if (cfg.chatTriggers   == null) cfg.chatTriggers   = new ArrayList<>();
                    if (cfg.hudElements    == null) cfg.hudElements    = new HashMap<>();
                    return cfg;
                }
            } catch (IOException e) {
                System.err.println("[MelodyAddons] Failed to load config: " + e.getMessage());
            }
        }
        return new ModConfig();
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            System.err.println("[MelodyAddons] Failed to save config: " + e.getMessage());
        }
    }

    public boolean hasMelodyMessages() {
        return nonEmpty(melodyMsgStart) || nonEmpty(melodyMsgClose) || !melodyMsgPool.isEmpty();
    }

    public static boolean nonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public HudElement getHudElement(String id) {
        return hudElements.computeIfAbsent(id, k -> new HudElement(k, 10, 10, 1.0f));
    }
}