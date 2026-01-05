package com.lightcraft.client.gui;
import com.lightcraft.client.hud.*;
import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class HudRenderer {
    private final ModConfig config;
    private final FpsHud fpsHud;
    private final CoordsHud coordsHud;
    private final MinimapHud minimapHud;
    private boolean editMode = false;
    
    public HudRenderer(ModConfig config, WaypointManager waypointManager) {
        this.config = config;
        this.fpsHud = new FpsHud(config);
        this.coordsHud = new CoordsHud(config);
        this.minimapHud = new MinimapHud(config, waypointManager);
    }
    
    public void render(DrawContext context, float tickDelta) {
        if (!config.hudEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        
        context.getMatrices().push();
        context.getMatrices().scale(config.hudScale, config.hudScale, 1.0f);
        
        int sW = (int)(width / config.hudScale);
        int sH = (int)(height / config.hudScale);
        
        if (config.fpsEnabled) fpsHud.render(context, sW, sH);
        if (config.coordsEnabled) coordsHud.render(context, sW, sH);
        if (config.minimapEnabled) minimapHud.render(context, sW, sH, tickDelta);
        
        if (editMode) {
             context.drawBorder(config.fpsX-2, config.fpsY-2, 60, 16, 0x80FFFFFF);
             context.drawBorder(config.coordsX-2, config.coordsY-2, 150, 40, 0x80FFFFFF);
        }
        
        context.getMatrices().pop();
    }
    
    public void tick() { fpsHud.tick(); }
    public void setEditMode(boolean mode) { this.editMode = mode; }
}
