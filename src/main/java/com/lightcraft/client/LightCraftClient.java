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
        // Use reflection to register keys to avoid 1.21.1 vs 1.21.11 constructor mismatches
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
                LOGGER.error("Failed to register keybinding helper for " + name, e);
            }
        }
        return null;
    }

    private KeyBinding createKeyBinding(String name, int code, String category) {
        // 1. Try Standard Constructor (1.21.x standard)
        try {
            return new KeyBinding(name, InputUtil.Type.KEYSYM, code, category);
        } catch (Throwable t1) {
            // 2. Try Legacy Constructor
            try {
                return new KeyBinding(name, code, category);
            } catch (Throwable t2) {
                // 3. Mounts of Mayhem / Unknown Version Fallback via Reflection
                LOGGER.warn("Standard KeyBinding failed, attempting reflection for 1.21.11 compatibility...");
                try {
                    for (Constructor<?> c : KeyBinding.class.getConstructors()) {
                        Class<?>[] types = c.getParameterTypes();
                        
                        // Look for (String, int, String)
                        if (types.length == 3 && types[0] == String.class && types[1] == int.class && types[2] == String.class) {
                            return (KeyBinding) c.newInstance(name, code, category);
                        }
                        // Look for (String, InputUtil.Type, int, String)
                        if (types.length == 4 && types[0] == String.class && types[1] == InputUtil.Type.class) {
                            return (KeyBinding) c.newInstance(name, InputUtil.Type.KEYSYM, code, category);
                        }
                    }
                } catch (Throwable t3) {
                    LOGGER.error("CRITICAL: Could not create KeyBinding for " + name, t3);
                }
            }
        }
        return null;
    }
    
    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        
        // Null checks prevent crash if registration failed
        if (toggleHudKey != null && toggleHudKey.wasPressed()) {
            while (toggleHudKey.wasPressed()) { // Clear buffer
                config.hudEnabled = !config.hudEnabled;
                configManager.saveConfig(config);
            }
        }
        
        if (toggleMinimapKey != null && toggleMinimapKey.wasPressed()) {
            while (toggleMinimapKey.wasPressed()) {
                config.minimapEnabled = !config.minimapEnabled;
                configManager.saveConfig(config);
            }
        }
        
        if (openConfigKey != null && openConfigKey.wasPressed()) {
            while (openConfigKey.wasPressed()) {
                client.setScreen(new ConfigScreen(config, configManager, waypointManager, hudRenderer));
            }
        }
        
        if (addWaypointKey != null && addWaypointKey.wasPressed()) {
            while (addWaypointKey.wasPressed()) {
                client.setScreen(new WaypointScreen(config, configManager, waypointManager, client.player.getBlockPos(), true));
            }
        }
        
        hudRenderer.tick();
    }
    
    public static LightCraftClient getInstance() { return instance; }
    public ModConfig getConfig() { return config; }
    public ConfigManager getConfigManager() { return configManager; }
}
