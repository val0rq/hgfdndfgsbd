package com.lightcraft.client.hud;
import com.lightcraft.config.ModConfig;
import com.lightcraft.client.gui.HudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class FpsHud {
    private final ModConfig config;
    private int currentFps = 0;
    private long lastUpdate = 0;
    
    public FpsHud(ModConfig config) { this.config = config; }
    
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate >= config.fpsUpdateInterval) {
            currentFps = MinecraftClient.getInstance().getCurrentFps();
            lastUpdate = now;
        }
    }
    
    public void render(DrawContext context, int w, int h) {
        int color = currentFps >= 60 ? 0xFF55FF55 : (currentFps >= 30 ? 0xFFFFFF55 : 0xFFFF5555);
        HudRenderer.fillSafe(context, config.fpsX - 2, config.fpsY - 2, config.fpsX + 50, config.fpsY + 12, 0x80000000);
        HudRenderer.drawTextSafe(context, MinecraftClient.getInstance().textRenderer, 
            currentFps + " FPS", config.fpsX, config.fpsY, color, true);
    }
}
