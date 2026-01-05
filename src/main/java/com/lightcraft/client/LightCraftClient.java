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
    
    private KeyBinding toggleHudKey;
    private KeyBinding toggleMinimapKey;
    private KeyBinding openConfigKey;
    private KeyBinding addWaypointKey;
    
    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initializing LightCraft...");
        
        configManager = new ConfigManager();
        config = configManager.loadConfig();
        waypointManager = new WaypointManager(configManager);
        hudRenderer = new HudRenderer(config, waypointManager);
        
        registerKeybindings();
        
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && !client.options.hudHidden) {
                hudRenderer.render(drawContext, tickDelta);
            }
        });
        
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }
    
    private void registerKeybindings() {
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "lightcraft.key.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "lightcraft.key.category"));
        toggleMinimapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "lightcraft.key.toggle_minimap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "lightcraft.key.category"));
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "lightcraft.key.open_config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "lightcraft.key.category"));
        addWaypointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "lightcraft.key.add_waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "lightcraft.key.category"));
    }
    
    private void onClientTick(MinecraftClient client) {
        if (client.player == null) return;
        
        while (toggleHudKey.wasPressed()) {
            config.hudEnabled = !config.hudEnabled;
            configManager.saveConfig(config);
        }
        while (toggleMinimapKey.wasPressed()) {
            config.minimapEnabled = !config.minimapEnabled;
            configManager.saveConfig(config);
        }
        while (openConfigKey.wasPressed()) {
            client.setScreen(new ConfigScreen(config, configManager, waypointManager, hudRenderer));
        }
        while (addWaypointKey.wasPressed()) {
            client.setScreen(new WaypointScreen(config, configManager, waypointManager, client.player.getBlockPos(), true));
        }
        
        hudRenderer.tick();
    }
    
    public static LightCraftClient getInstance() { return instance; }
    public ModConfig getConfig() { return config; }
    public ConfigManager getConfigManager() { return configManager; }
}
