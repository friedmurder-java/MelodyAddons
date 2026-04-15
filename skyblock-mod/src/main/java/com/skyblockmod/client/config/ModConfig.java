package com.skyblockmod.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    public boolean rarityGlow        = true;
    public boolean slayerRngTracker  = true;
    public boolean dungeonRngTracker = true;
    public boolean cocoonAlert       = true;
    public boolean melodyMessages    = true;

    public String melodyMsgStart  = "";
    public String melodyMsgClose  = "";
    public List<String> melodyMsgPool = new ArrayList<>();

    public List<ChatTrigger> chatTriggers = new ArrayList<>();

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
            FabricLoader.getInstance().getConfigDir().resolve("mymod.json");
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
                    return cfg;
                }
            } catch (IOException e) {
                System.err.println("[MyMod] Failed to load config: " + e.getMessage());
            }
        }
        return new ModConfig();
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            System.err.println("[MyMod] Failed to save config: " + e.getMessage());
        }
    }

    public boolean hasMelodyMessages() {
        return nonEmpty(melodyMsgStart) || nonEmpty(melodyMsgClose) || !melodyMsgPool.isEmpty();
    }

    public static boolean nonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}