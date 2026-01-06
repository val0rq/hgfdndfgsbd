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
        int y = 40;
        
        // HUD Toggles
        addDrawableChild(ButtonWidget.builder(Text.of("FPS: " + (config.fpsEnabled ? "ON" : "OFF")), b -> {
            config.fpsEnabled = !config.fpsEnabled;
            b.setMessage(Text.of("FPS: " + (config.fpsEnabled ? "ON" : "OFF")));
            manager.saveConfig(config);
        }).dimensions(x, y, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.of("Coords: " + (config.coordsEnabled ? "ON" : "OFF")), b -> {
            config.coordsEnabled = !config.coordsEnabled;
            b.setMessage(Text.of("Coords: " + (config.coordsEnabled ? "ON" : "OFF")));
            manager.saveConfig(config);
        }).dimensions(x + 105, y, 95, 20).build());
        y += 25;
        
        // Minimap Toggle
        addDrawableChild(ButtonWidget.builder(Text.of("Minimap: " + (config.minimapEnabled ? "ON" : "OFF")), b -> {
            config.minimapEnabled = !config.minimapEnabled;
            b.setMessage(Text.of("Minimap: " + (config.minimapEnabled ? "ON" : "OFF")));
            manager.saveConfig(config);
        }).dimensions(x, y, 200, 20).build());
        y += 25;

        // Edit Layout (Opens New Screen)
        addDrawableChild(ButtonWidget.builder(Text.of("Edit HUD Positions..."), b -> {
            client.setScreen(new HudEditorScreen(config, manager, renderer));
        }).dimensions(x, y, 200, 20).build());
        y += 30;

        // Waypoints List
        HudRenderer.drawTextSafe(null, textRenderer, "Waypoints", x, y, 0xFFFFFFFF, true);
        y += 15;
        
        for (int i = 0; i < Math.min(4, config.waypoints.size()); i++) {
            ModConfig.Waypoint wp = config.waypoints.get(i);
            final int idx = i;
            
            addDrawableChild(ButtonWidget.builder(Text.of(wp.name), b -> {
               // Toggle logic
            }).dimensions(x, y, 160, 20).build());
            
            addDrawableChild(ButtonWidget.builder(Text.of("X"), b -> {
                waypointManager.removeWaypoint(idx);
                this.init(); // Refresh
            }).dimensions(x + 165, y, 35, 20).build());
            y += 25;
        }

        // Done
        addDrawableChild(ButtonWidget.builder(Text.of("Done"), b -> close())
            .dimensions(x, height - 30, 200, 20).build());
    }
    
    @Override
    public void close() { MinecraftClient.getInstance().setScreen(null); }
    
    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        HudRenderer.fillSafe(c, 0, 0, width, height, 0xC0000000);
        int titleWidth = textRenderer.getWidth(title);
        HudRenderer.drawTextSafe(c, textRenderer, title.getString(), width/2 - titleWidth/2, 15, 0xFFFFFFFF, true);
        super.render(c, mx, my, d);
    }
}
