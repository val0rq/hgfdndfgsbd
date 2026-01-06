package com.lightcraft.client.gui;
import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.config.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final ModConfig config;
    private final ConfigManager manager;
    private final WaypointManager waypointManager;
    private final HudRenderer renderer;
    
    public ConfigScreen(ModConfig c, ConfigManager m, WaypointManager wm, HudRenderer hr) {
        super(Text.of("LightCraft Config"));
        this.config = c; this.manager = m; this.waypointManager = wm; this.renderer = hr;
    }
    
    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = 50;
        
        // Toggle Death Waypoints
        addDrawableChild(ButtonWidget.builder(Text.of("Death Waypoints: " + (config.deathWaypoint ? "ON" : "OFF")), b -> {
            config.deathWaypoint = !config.deathWaypoint;
            b.setMessage(Text.of("Death Waypoints: " + (config.deathWaypoint ? "ON" : "OFF")));
            manager.saveConfig(config);
        }).dimensions(x, y, 200, 20).build());
        y += 25;

        // Existing toggles...
        addDrawableChild(ButtonWidget.builder(Text.of("Minimap: " + (config.minimapEnabled ? "ON" : "OFF")), b -> {
            config.minimapEnabled = !config.minimapEnabled;
            b.setMessage(Text.of("Minimap: " + (config.minimapEnabled ? "ON" : "OFF")));
            manager.saveConfig(config);
        }).dimensions(x, y, 200, 20).build());
        y += 25;
        
        addDrawableChild(ButtonWidget.builder(Text.of("Edit HUD Layout"), b -> {
            client.setScreen(new HudEditorScreen(config, manager, renderer));
        }).dimensions(x, y, 200, 20).build());
        y += 25;
        
        addDrawableChild(ButtonWidget.builder(Text.of("Done"), b -> close()).dimensions(x, height - 30, 200, 20).build());
    }
    
    @Override
    public void close() { MinecraftClient.getInstance().setScreen(null); }
    
    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        // BEAUTIFUL GRADIENT BACKGROUND
        HudRenderer.fillSafe(c, 0, 0, width, height, 0xAA000000); 
        
        int titleWidth = textRenderer.getWidth(title);
        HudRenderer.drawTextSafe(c, textRenderer, title.getString(), width/2 - titleWidth/2, 20, 0xFFFFFFFF, true);
        super.render(c, mx, my, d);
    }
}
