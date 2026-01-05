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
        
        addDrawableChild(ButtonWidget.builder(Text.of("FPS Display: " + config.fpsEnabled), b -> {
            config.fpsEnabled = !config.fpsEnabled;
            b.setMessage(Text.of("FPS Display: " + config.fpsEnabled));
            manager.saveConfig(config);
        }).dimensions(x, y, 200, 20).build());
        
        y += 25;
        addDrawableChild(ButtonWidget.builder(Text.of("Minimap: " + config.minimapEnabled), b -> {
            config.minimapEnabled = !config.minimapEnabled;
            b.setMessage(Text.of("Minimap: " + config.minimapEnabled));
            manager.saveConfig(config);
        }).dimensions(x, y, 200, 20).build());

        y += 25;
        addDrawableChild(ButtonWidget.builder(Text.of("Edit HUD Layout"), b -> {
            renderer.setEditMode(true);
            close();
        }).dimensions(x, y, 200, 20).build());
        
        y += 25;
        addDrawableChild(ButtonWidget.builder(Text.of("Done"), b -> close()).dimensions(x, y, 200, 20).build());
    }
    
    @Override
    public void close() { MinecraftClient.getInstance().setScreen(null); }
    
    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        renderBackground(c, mx, my, d);
        c.drawCenteredTextWithShadow(textRenderer, title, width/2, 20, 0xFFFFFF);
        super.render(c, mx, my, d);
    }
}
