package com.lightcraft.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lightcraft.client.LightCraftClient;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {
    private static final String CONFIG_FILENAME = "lightcraft.json";
    private final Path configPath;
    private final Gson gson;
    
    public ConfigManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILENAME);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }
    
    public ModConfig loadConfig() {
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                return gson.fromJson(reader, ModConfig.class);
            } catch (Exception e) { LightCraftClient.LOGGER.error("Error loading config", e); }
        }
        ModConfig config = new ModConfig();
        saveConfig(config);
        return config;
    }
    
    public void saveConfig(ModConfig config) {
        CompletableFuture.runAsync(() -> saveConfigImmediate(config));
    }
    
    public void saveConfigImmediate(ModConfig config) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                gson.toJson(config, writer);
            }
        } catch (Exception e) { LightCraftClient.LOGGER.error("Error saving config", e); }
    }
}
