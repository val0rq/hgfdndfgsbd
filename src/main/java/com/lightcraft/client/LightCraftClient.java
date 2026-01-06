package com.lightcraft.client;

import com.lightcraft.client.gui.*;
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
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightCraftClient implements ClientModInitializer {
    public static final String MOD_ID = "lightcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static LightCraftClient instance;
    
    private ModConfig config;
    private ConfigManager configManager;
    private HudRenderer hudRenderer;
    private WaypointManager waypointManager;
    
    private KeyBinding toggleHudKey, toggleMinimapKey, openConfigKey, addWaypointKey;
    private final boolean[] keyStates = new boolean[512];
    private boolean wasDead = false;
    
    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initializing LightCraft...");
        
        try {
            configManager = new ConfigManager();
            config = configManager.loadConfig();
            waypointManager = new WaypointManager(configManager);
            
            // Safe HUD Init
            hudRenderer = new HudRenderer(config, waypointManager);
            
            registerKeybindings();
            
            HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
                try {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player != null && !client.options.hudHidden) {
                        float delta = tickCounter.getTickDelta(false);
                        hudRenderer.render(drawContext, delta);
                    }
                } catch (Exception e) {}
            });
            
            ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        } catch (Exception e) {
            LOGGER.error("CRITICAL: Failed to init LightCraft. Mod disabled.", e);
        }
    }
    
    private void registerKeybindings() {
        toggleHudKey = registerSafe("lightcraft.key.toggle_hud", GLFW.GLFW_KEY_H);
        toggleMinimapKey = registerSafe("lightcraft.key.toggle_minimap", GLFW.GLFW_KEY_M);
        openConfigKey = registerSafe("lightcraft.key.open_config", GLFW.GLFW_KEY_K);
        addWaypointKey = registerSafe("lightcraft.key.add_waypoint", GLFW.GLFW_KEY_B);
    }

    private KeyBinding registerSafe(String name, int code) {
        try { return KeyBindingHelper.registerKeyBinding(new KeyBinding(name, InputUtil.Type.KEYSYM, code, "lightcraft.key.category")); } catch (Throwable t) { return null; }
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        
        if (config.deathWaypoint) {
            if (client.player.isDead()) {
                if (!wasDead) {
                    waypointManager.addWaypoint("Death", client.player.getBlockPos(), 0xFF0000, client.world.getRegistryKey().getValue().toString());
                    wasDead = true;
                }
            } else { wasDead = false; }
        }
        
        long handle = client.getWindow().getHandle();
        if (checkManualKey(handle, GLFW.GLFW_KEY_H)) { config.hudEnabled = !config.hudEnabled; configManager.saveConfig(config); }
        if (checkManualKey(handle, GLFW.GLFW_KEY_M)) { config.minimapEnabled = !config.minimapEnabled; configManager.saveConfig(config); }
        if (checkManualKey(handle, GLFW.GLFW_KEY_K)) client.execute(() -> client.setScreen(new ConfigScreen(config, configManager, waypointManager, hudRenderer)));
        if (checkManualKey(handle, GLFW.GLFW_KEY_B)) client.execute(() -> client.setScreen(new WaypointScreen(config, configManager, waypointManager, client.player.getBlockPos(), true)));
        
        if (hudRenderer != null) hudRenderer.tick();
    }
    
    private boolean checkManualKey(long handle, int key) {
        boolean isDown = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        boolean wasDown = keyStates[key];
        keyStates[key] = isDown;
        return isDown && !wasDown;
    }
    
    public static LightCraftClient getInstance() { return instance; }
    public ModConfig getConfig() { return config; }
    public ConfigManager getConfigManager() { return configManager; }
}
