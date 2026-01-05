package com.lightcraft.client;

import com.lightcraft.client.gui.ConfigScreen;
import com.lightcraft.client.gui.HudRenderer;
import com.lightcraft.client.gui.WaypointScreen;
import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.config.ConfigManager;
import com.lightcraft.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

public class LightCraftClient implements ClientModInitializer {
    public static final String MOD_ID = "lightcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static LightCraftClient instance;
    
    private ModConfig config;
    private ConfigManager configManager;
    private HudRenderer hudRenderer;
    private WaypointManager waypointManager;
    
    private KeyBinding toggleHudKey;
    private KeyBinding toggleMinimapKey;
    private KeyBinding openConfigKey;
    private KeyBinding addWaypointKey;
    
    // Manual Input State
    private final boolean[] keyStates = new boolean[512];
    
    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initializing LightCraft for Mounts of Mayhem...");
        
        configManager = new ConfigManager();
        config = configManager.loadConfig();
        waypointManager = new WaypointManager(configManager);
        hudRenderer = new HudRenderer(config, waypointManager);
        
        registerKeybindings();
        
        // Robust HUD rendering callback
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null && !client.options.hudHidden) {
                    float delta = tickCounter.getTickDelta(false);
                    hudRenderer.render(drawContext, delta);
                }
            } catch (Exception e) {
                // Prevent rendering crashes
            }
        });
        
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }
    
    private void registerKeybindings() {
        // Attempt registration (might fail on 1.21.11, but we have a fallback now)
        toggleHudKey = registerSafe("lightcraft.key.toggle_hud", GLFW.GLFW_KEY_H);
        toggleMinimapKey = registerSafe("lightcraft.key.toggle_minimap", GLFW.GLFW_KEY_M);
        openConfigKey = registerSafe("lightcraft.key.open_config", GLFW.GLFW_KEY_K);
        addWaypointKey = registerSafe("lightcraft.key.add_waypoint", GLFW.GLFW_KEY_B);
    }

    private KeyBinding registerSafe(String name, int code) {
        KeyBinding key = createKeyBinding(name, code, "lightcraft.key.category");
        if (key != null) {
            try {
                return KeyBindingHelper.registerKeyBinding(key);
            } catch (Exception e) {
                LOGGER.error("Failed to register keybinding helper for " + name);
            }
        }
        return null;
    }

    private KeyBinding createKeyBinding(String name, int code, String category) {
        try {
            return new KeyBinding(name, InputUtil.Type.KEYSYM, code, category);
        } catch (Throwable t1) {
            try {
                return new KeyBinding(name, code, category);
            } catch (Throwable t2) {
                // Reflection attempts... but if these fail, we fall back to manual input
                return null;
            }
        }
    }
    
    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        
        // 1. Try Standard KeyBindings (if registration worked)
        if (toggleHudKey != null) {
            while (toggleHudKey.wasPressed()) { toggleHud(); }
            while (toggleMinimapKey.wasPressed()) { toggleMinimap(); }
            while (openConfigKey.wasPressed()) { openConfig(client); }
            while (addWaypointKey.wasPressed()) { addWaypoint(client); }
        } 
        // 2. Fallback: Manual Input (Direct GLFW check)
        else {
            long handle = client.getWindow().getHandle();
            
            if (checkManualKey(handle, GLFW.GLFW_KEY_H)) toggleHud();
            if (checkManualKey(handle, GLFW.GLFW_KEY_M)) toggleMinimap();
            if (checkManualKey(handle, GLFW.GLFW_KEY_K)) openConfig(client);
            if (checkManualKey(handle, GLFW.GLFW_KEY_B)) addWaypoint(client);
        }
        
        hudRenderer.tick();
    }
    
    // --- Manual Input Helper ---
    private boolean checkManualKey(long handle, int key) {
        // Returns true only on the "rising edge" (the moment it is pressed)
        boolean isDown = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        boolean wasDown = keyStates[key];
        keyStates[key] = isDown;
        return isDown && !wasDown;
    }

    // --- Actions ---
    private void toggleHud() {
        config.hudEnabled = !config.hudEnabled;
        configManager.saveConfig(config);
        LOGGER.info("HUD Toggled");
    }
    
    private void toggleMinimap() {
        config.minimapEnabled = !config.minimapEnabled;
        configManager.saveConfig(config);
    }
    
    private void openConfig(MinecraftClient client) {
        // Run on next tick to avoid input conflict
        client.execute(() -> 
            client.setScreen(new ConfigScreen(config, configManager, waypointManager, hudRenderer)));
    }
    
    private void addWaypoint(MinecraftClient client) {
        client.execute(() -> 
            client.setScreen(new WaypointScreen(config, configManager, waypointManager, client.player.getBlockPos(), true)));
    }
    
    public static LightCraftClient getInstance() { return instance; }
    public ModConfig getConfig() { return config; }
    public ConfigManager getConfigManager() { return configManager; }
}
